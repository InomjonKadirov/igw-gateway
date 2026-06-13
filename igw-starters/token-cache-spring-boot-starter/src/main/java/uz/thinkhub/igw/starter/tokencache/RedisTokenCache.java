package uz.thinkhub.igw.starter.tokencache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed {@link TokenCache}.
 *
 * <p>Each token is stored as a single Redis string of the form
 * {@code <value>|<expiresAt-iso8601>} under the key
 * {@code <igw.token-cache.key-prefix><key>}. The TTL is set to the value
 * passed to {@link #put}, so Redis itself evicts expired entries.
 *
 * <p>This class is thread-safe — {@link StringRedisTemplate} is.
 */
public class RedisTokenCache implements TokenCache {

    private final StringRedisTemplate redis;
    private final IgwTokenCacheProperties properties;

    public RedisTokenCache(StringRedisTemplate redis, IgwTokenCacheProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public Optional<Token> get(String key) {
        String stored = redis.opsForValue().get(fullKey(key));
        if (stored == null) {
            return Optional.empty();
        }
        int sep = stored.lastIndexOf('|');
        if (sep <= 0 || sep == stored.length() - 1) {
            return Optional.empty();
        }
        String value = stored.substring(0, sep);
        try {
            Instant expiresAt = Instant.parse(stored.substring(sep + 1));
            return Optional.of(new Token(value, expiresAt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Token token, Duration ttl) {
        String stored = token.value() + "|" + token.expiresAt().toString();
        redis.opsForValue().set(fullKey(key), stored, ttl);
    }

    @Override
    public void invalidate(String key) {
        redis.delete(fullKey(key));
    }

    private String fullKey(String key) {
        return properties.getKeyPrefix() + key;
    }
}
