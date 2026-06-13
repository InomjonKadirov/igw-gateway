package uz.thinkhub.igw.starter.tokencache;

/**
 * Strategy the consuming service implements to fetch a fresh token from the
 * upstream provider when the cache is missing or near expiry.
 *
 * <p>Implementations must be safe to call concurrently. They should throw
 * a runtime exception on failure; the {@link CachingTokenProvider} will
 * propagate the exception to its caller (caller decides retry policy).
 */
@FunctionalInterface
public interface TokenRefresher {

    /**
     * Fetch a fresh token for the given key.
     *
     * @param key the cache key (typically a provider name or a logical token id)
     * @return a fresh, non-expired token
     */
    Token refresh(String key);
}
