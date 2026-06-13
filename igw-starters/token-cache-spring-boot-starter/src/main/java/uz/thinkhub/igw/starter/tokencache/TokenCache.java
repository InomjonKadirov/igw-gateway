package uz.thinkhub.igw.starter.tokencache;

import java.time.Duration;
import java.util.Optional;

/**
 * A key/value cache for {@link Token}s.
 *
 * <p>Implementations must be safe to call concurrently. They are typically
 * backed by Redis (see {@link RedisTokenCache}) but may be replaced by the
 * consuming service for tests or alternative stores.
 */
public interface TokenCache {

    /**
     * Look up a token by key.
     *
     * @return the cached token, or {@link Optional#empty()} if the key is
     *         not present (or the entry has expired and was evicted)
     */
    Optional<Token> get(String key);

    /**
     * Store a token under {@code key} with the given TTL. If a token is
     * already present, it is overwritten.
     */
    void put(String key, Token token, Duration ttl);

    /**
     * Remove the entry for {@code key}. No-op if the key is not present.
     */
    void invalidate(String key);
}
