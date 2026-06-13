package uz.thinkhub.igw.starter.tokencache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachingTokenProviderTest {

    private TokenCache cache;
    private TokenRefresher refresher;
    private CachingTokenProvider provider;

    @BeforeEach
    void setUp() {
        cache = mock(TokenCache.class);
        refresher = mock(TokenRefresher.class);
        provider = new CachingTokenProvider(cache, refresher, new IgwTokenCacheProperties());
    }

    @Test
    void cacheMissTriggersRefresherAndStoresResult() {
        Token fresh = new Token("token-1", Instant.now().plus(Duration.ofMinutes(30)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.empty());
        when(refresher.refresh("iabs")).thenReturn(fresh);

        Token result = provider.getToken("iabs");

        assertThat(result).isEqualTo(fresh);
        verify(refresher, times(1)).refresh("iabs");
        // TTL is computed from token.expiresAt() - now(), so we use any() to
        // tolerate sub-millisecond clock drift.
        verify(cache, times(1)).put(eq("iabs"), eq(fresh), any(Duration.class));
    }

    @Test
    void cacheHitReturnsCachedTokenWithoutRefresher() {
        Token cached = new Token("token-cached", Instant.now().plus(Duration.ofMinutes(30)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.of(cached));

        Token result = provider.getToken("iabs");

        assertThat(result).isEqualTo(cached);
        verify(refresher, never()).refresh(anyString());
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void nearExpiryTriggersProactiveRefresh() {
        // Token expires in 30 seconds, well within the 60s refreshWindow.
        Token nearExpiry = new Token("token-near", Instant.now().plus(Duration.ofSeconds(30)));
        Token fresh = new Token("token-fresh", Instant.now().plus(Duration.ofMinutes(30)));

        when(cache.get("iabs")).thenReturn(java.util.Optional.of(nearExpiry));
        when(refresher.refresh("iabs")).thenReturn(fresh);

        Token result = provider.getToken("iabs");

        assertThat(result).isEqualTo(fresh);
        verify(refresher, times(1)).refresh("iabs");
    }

    @Test
    void exactlyOnRefreshWindowBoundaryIsTreatedAsNearExpiry() {
        // Token expires in exactly 60s. The check is "expiresAt is NOT after now+60s",
        // so exactly 60s is treated as near-expiry.
        Token atBoundary = new Token("token-boundary", Instant.now().plus(Duration.ofSeconds(60)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.of(atBoundary));
        when(refresher.refresh("iabs")).thenReturn(
                new Token("fresh", Instant.now().plus(Duration.ofMinutes(30))));

        provider.getToken("iabs");

        verify(refresher, times(1)).refresh("iabs");
    }

    @Test
    void wellBeyondRefreshWindowIsCached() {
        // Token expires in 30 minutes; refreshWindow is 60s. Should be served from cache.
        Token cached = new Token("token-long", Instant.now().plus(Duration.ofMinutes(30)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.of(cached));

        Token result = provider.getToken("iabs");

        assertThat(result).isEqualTo(cached);
        verify(refresher, never()).refresh(anyString());
    }

    @Test
    void alreadyExpiredTokenInCacheTriggersRefresh() {
        // Cache returned an already-expired token. Should be refreshed.
        Token expired = new Token("token-expired", Instant.now().minus(Duration.ofMinutes(1)));
        Token fresh = new Token("token-fresh", Instant.now().plus(Duration.ofMinutes(30)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.of(expired));
        when(refresher.refresh("iabs")).thenReturn(fresh);

        Token result = provider.getToken("iabs");

        assertThat(result).isEqualTo(fresh);
        verify(refresher, times(1)).refresh("iabs");
    }

    @Test
    void customRefreshWindowIsRespected() {
        IgwTokenCacheProperties props = new IgwTokenCacheProperties();
        props.setRefreshWindow(Duration.ofMinutes(5));
        CachingTokenProvider longWindow = new CachingTokenProvider(cache, refresher, props);

        // Token expires in 2 minutes, within the 5-min window. Should refresh.
        Token nearExpiry = new Token("token-near", Instant.now().plus(Duration.ofMinutes(2)));
        Token fresh = new Token("fresh", Instant.now().plus(Duration.ofMinutes(30)));
        when(cache.get("iabs")).thenReturn(java.util.Optional.of(nearExpiry));
        when(refresher.refresh("iabs")).thenReturn(fresh);

        longWindow.getToken("iabs");

        verify(refresher, times(1)).refresh("iabs");
    }

    @Test
    void defaultTtlIsUsedForAlreadyExpiredRefreshedToken() {
        // Refresher returns a token that's already expired (provider bug).
        // CachingTokenProvider should fall back to the default TTL rather than
        // throwing or storing a zero-TTL entry.
        AtomicInteger putCount = new AtomicInteger();
        AtomicReference<Duration> storedTtl = new AtomicReference<>();
        cache = new TokenCache() {
            @Override
            public java.util.Optional<Token> get(String key) {
                return java.util.Optional.empty();
            }
            @Override
            public void put(String key, Token token, Duration ttl) {
                putCount.incrementAndGet();
                storedTtl.set(ttl);
            }
            @Override
            public void invalidate(String key) {}
        };
        refresher = mock(TokenRefresher.class);
        provider = new CachingTokenProvider(cache, refresher, new IgwTokenCacheProperties());

        Token expiredRefreshed = new Token("buggy-token", Instant.now().minus(Duration.ofSeconds(1)));
        when(refresher.refresh("iabs")).thenReturn(expiredRefreshed);

        provider.getToken("iabs");

        assertThat(putCount.get()).isEqualTo(1);
        assertThat(storedTtl.get()).isEqualTo(Duration.ofMinutes(30));  // defaultTtl
    }
}
