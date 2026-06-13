package uz.thinkhub.igw.gateway;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-user token-bucket rate limit keyed by the JWT {@code sub}.
 *
 * <p>Config (defaults): {@code igw.edge.rate-limit.rps=100},
 * {@code igw.edge.rate-limit.burst=200}. The bucket is held in memory
 * (a {@link ConcurrentMap}); for multi-replica deployments, a distributed
 * bucket (Redis-backed) is needed — flagged for a follow-up.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Anonymous requests pass through (the Spring Security chain returns
 *       401 first).</li>
 *   <li>Authenticated users get a bucket keyed by user id; on overflow,
 *       the filter returns 429 Too Many Requests.</li>
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final IgwEdgeProperties properties;

    public RateLimitFilter(IgwEdgeProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = currentUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(userId, k -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", "1");
        }
    }

    private Bucket newBucket() {
        int rps = properties.getRateLimit().getRps();
        int burst = properties.getRateLimit().getBurst();
        Bandwidth limit = Bandwidth.builder()
                .capacity(burst)
                .refillIntervally(rps, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }
}
