package uz.thinkhub.igw.starter.tokencache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisTokenCacheTest {

    /**
     * Verifies the cache delegates correctly to {@link StringRedisTemplate} and
     * applies the configured key prefix and TTL. Uses a mocked
     * {@link ValueOperations} so the test doesn't require a running Redis.
     *
     * <p>An integration test using Testcontainers (the original PR #6 plan)
     * lives in {@code RedisTokenCacheIntegrationTest}, which is disabled
     * when Docker is unavailable.
     */
    @Test
    void delegatesToRedisTemplateWithPrefixedKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("igw:token:iabs")).thenReturn("the-secret|" + Instant.now().plus(Duration.ofMinutes(5)));

        RedisTokenCache cache = new RedisTokenCache(redis, new IgwTokenCacheProperties());

        Optional<Token> loaded = cache.get("iabs");

        assertThat(loaded).isPresent();
        assertThat(loaded.get().value()).isEqualTo("the-secret");
    }

    @Test
    void usesCustomKeyPrefix() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        IgwTokenCacheProperties props = new IgwTokenCacheProperties();
        props.setKeyPrefix("custom:prefix:");
        RedisTokenCache cache = new RedisTokenCache(redis, props);

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        cache.put("k", new Token("v", expiresAt), Duration.ofMinutes(5));

        verify(ops).set(eq("custom:prefix:k"), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getReturnsEmptyWhenValueIsNull() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        RedisTokenCache cache = new RedisTokenCache(redis, new IgwTokenCacheProperties());

        assertThat(cache.get("iabs")).isEmpty();
    }

    @Test
    void getReturnsEmptyWhenValueIsMalformed() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("igw:token:iabs")).thenReturn("garbage-no-pipe");

        RedisTokenCache cache = new RedisTokenCache(redis, new IgwTokenCacheProperties());

        assertThat(cache.get("iabs")).isEmpty();
    }

    @Test
    void invalidateDelegatesToRedisDelete() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        IgwTokenCacheProperties props = new IgwTokenCacheProperties();
        props.setKeyPrefix("custom:");
        RedisTokenCache cache = new RedisTokenCache(redis, props);

        cache.invalidate("iabs");

        verify(redis).delete("custom:iabs");
        verify(redis, never()).opsForValue();
    }
}
