package uz.thinkhub.igw.starter.tokencache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cache-aside token provider. Wraps a {@link TokenRefresher} with read-through
 * semantics:
 *
 * <ol>
 *   <li>Look up the cache. If a non-near-expiry token is present, return it.</li>
 *   <li>Otherwise, call {@link TokenRefresher#refresh}, store the result in
 *       the cache with a TTL based on the token's expiry, and return it.</li>
 * </ol>
 *
 * <p>Proactive refresh: if a cached token's {@code expiresAt} is within
 * {@code igw.token-cache.refresh-window} of "now," it's treated as
 * near-expiry and refreshed on the next access. This avoids serving a
 * token that's about to expire.
 */
public class CachingTokenProvider {

    private final TokenCache cache;
    private final TokenRefresher refresher;
    private final IgwTokenCacheProperties properties;

    public CachingTokenProvider(TokenCache cache,
                               TokenRefresher refresher,
                               IgwTokenCacheProperties properties) {
        this.cache = cache;
        this.refresher = refresher;
        this.properties = properties;
    }

    /**
     * Return a token for the given key, using the cache and refreshing
     * via {@link TokenRefresher} when needed.
     */
    public Token getToken(String key) {
        Optional<Token> cached = cache.get(key);
        if (cached.isPresent() && !isNearExpiry(cached.get())) {
            return cached.get();
        }
        Token fresh = refresher.refresh(key);
        cache.put(key, fresh, computeTtl(fresh));
        return fresh;
    }

    private boolean isNearExpiry(Token token) {
        Instant now = Instant.now();
        Instant threshold = now.plus(properties.getRefreshWindow());
        return !token.expiresAt().isAfter(threshold);
    }

    private Duration computeTtl(Token token) {
        Duration untilExpiry = Duration.between(Instant.now(), token.expiresAt());
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            // Provider returned an already-expired or no-expiry token; fall
            // back to the configured default TTL.
            return properties.getDefaultTtl();
        }
        return untilExpiry;
    }
}
