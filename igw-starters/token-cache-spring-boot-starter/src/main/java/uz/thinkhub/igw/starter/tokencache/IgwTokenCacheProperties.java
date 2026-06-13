package uz.thinkhub.igw.starter.tokencache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.token-cache")
public class IgwTokenCacheProperties {

    /**
     * If a cached token expires within this window, {@link CachingTokenProvider}
     * proactively refreshes it on the next access rather than serving it.
     * Default: 60 seconds.
     */
    private Duration refreshWindow = Duration.ofSeconds(60);

    /**
     * Prefix applied to all cache keys in the underlying store. The full
     * Redis key is {@code <keyPrefix><key>}.
     * Default: {@code "igw:token:"}.
     */
    private String keyPrefix = "igw:token:";

    /**
     * Fallback TTL applied when a token's expiry is in the past or zero
     * (i.e. the provider gave us an already-expired or no-expiry token).
     * Default: 30 minutes.
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    public Duration getRefreshWindow() {
        return refreshWindow;
    }

    public void setRefreshWindow(Duration refreshWindow) {
        this.refreshWindow = refreshWindow;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }
}
