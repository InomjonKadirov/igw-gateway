package uz.thinkhub.igw.gateway;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uz.thinkhub.igw.starter.tokencache.Token;
import uz.thinkhub.igw.starter.tokencache.TokenCache;

/**
 * Defense-in-depth check: after the JWT security filter has authenticated
 * the request, the user's profile says their requests must come from a
 * specific IP (their {@code user.verification_ip}). If the request comes
 * from a different IP, return 403.
 *
 * <p>The verification IP is looked up from the {@link TokenCache} under
 * the JWT {@code sub} (the user id). The cache key is the user id; the
 * cached value's {@code value()} field carries the IP (the cache is
 * generic; the token-cache-starter is reused as a small key-value
 * store for Phase 0).
 *
 * <p>Behavior:
 * <ul>
 *   <li>Disabled by default ({@code igw.edge.security.ip-check.enabled=false})
 *       in Phase 0 — the cache is empty and would always fail. Enable
 *       it in environments where the cache is populated.</li>
 *   <li>If no {@link Authentication} is present (anonymous / pre-auth),
 *       the filter passes through. The Spring Security chain handles 401.</li>
 *   <li>If the user id is not in the cache, the filter fails OPEN
 *       (logs a warning, passes through). Production should fail closed
 *       once the cache is populated.</li>
 *   <li>If the cached IP and the request's remote address don't match,
 *       returns 403.</li>
 * </ul>
 */
@Component
public class IpCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpCheckFilter.class);

    private final TokenCache tokenCache;
    private final IgwEdgeProperties properties;

    public IpCheckFilter(TokenCache tokenCache, IgwEdgeProperties properties) {
        this.tokenCache = tokenCache;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.getSecurity().getIpCheck().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String userId = currentUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        Optional<Token> cached = tokenCache.get(userId);
        if (cached.isEmpty()) {
            log.debug("No cached IP for user {}; passing through (cache miss)", userId);
            filterChain.doFilter(request, response);
            return;
        }
        String verificationIp = cached.get().value();
        String requestIp = request.getRemoteAddr();
        if (!verificationIp.equals(requestIp)) {
            log.warn("IP mismatch for user {}: cached={}, request={}",
                    userId, verificationIp, requestIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }
}
