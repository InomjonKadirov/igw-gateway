package uz.thinkhub.igw.gateway;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Weighted canary routing. For each incoming request, with probability
 * {@code igw.edge.canary.weight-new / 100} (and only when
 * {@code new-uri} is non-empty), this filter sets {@code X-Canary: new}
 * on the request via a {@link HttpServletRequestWrapper}. Spring Cloud
 * Gateway's route predicates pick the canary upstream for those requests
 * and the legacy upstream for the rest.
 *
 * <p>Phase 0 default: {@code weight-new=0} → no canary requests.
 * Setting {@code weight-new=100} routes 100% of traffic to the new
 * implementation (used as a kill switch / full cutover).
 *
 * <p>Order: registered at filter order 50 (after correlation-id, after
 * security, after IP and rate-limit). That way the rate-limit budget is
 * spent against the same user key regardless of where the request ends
 * up.
 */
@Component
public class CanaryRoutingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CanaryRoutingFilter.class);

    public static final String CANARY_HEADER = "X-Canary";
    public static final String CANARY_HEADER_VALUE = "new";

    private final IgwEdgeProperties properties;
    private final Random random;

    @Autowired
    public CanaryRoutingFilter(IgwEdgeProperties properties) {
        this(properties, new SecureRandom());
    }

    /** Test seam: deterministic {@link Random}. */
    CanaryRoutingFilter(IgwEdgeProperties properties, Random random) {
        this.properties = properties;
        this.random = random;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        int weight = properties.getCanary().getWeightNew();
        String newUri = properties.getCanary().getNewUri();

        if (weight <= 0 || newUri == null || newUri.isBlank()) {
            // 0% canary, or the canary upstream isn't configured yet.
            filterChain.doFilter(request, response);
            return;
        }
        if (weight >= 100) {
            // 100% canary — kill switch / full cutover.
            filterChain.doFilter(canaryRequest(request), response);
            return;
        }
        if (random.nextInt(100) < weight) {
            filterChain.doFilter(canaryRequest(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Returns a request wrapper that exposes {@code X-Canary: new} to the
     * downstream chain (the gateway's predicates see this header).
     */
    private static HttpServletRequest canaryRequest(HttpServletRequest original) {
        return new HttpServletRequestWrapper(original) {
            @Override
            public String getHeader(String name) {
                if (CANARY_HEADER.equalsIgnoreCase(name)) {
                    return CANARY_HEADER_VALUE;
                }
                return super.getHeader(name);
            }
        };
    }
}
