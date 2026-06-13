package uz.thinkhub.igw.starter.resilience;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.resilience")
public class IgwResilienceProperties {

    /**
     * Apache HttpClient connect timeout in milliseconds. Default: 1000.
     */
    private int defaultConnectTimeoutMs = 1000;

    /**
     * Apache HttpClient read/response timeout in milliseconds. Default: 5000.
     */
    private int defaultReadTimeoutMs = 5000;

    /**
     * Default max connections per route (host:port) on the Apache HttpClient
     * pool. Per-provider overrides live in {@link #providers}.
     * Default: 5.
     */
    private int defaultMaxPerRoute = 5;

    /**
     * Per-provider connection pool overrides. Map key is a logical provider
     * name (e.g. {@code iabs}, {@code humo}); the value carries the
     * provider-specific max-per-route.
     */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    public int getDefaultConnectTimeoutMs() {
        return defaultConnectTimeoutMs;
    }

    public void setDefaultConnectTimeoutMs(int defaultConnectTimeoutMs) {
        this.defaultConnectTimeoutMs = defaultConnectTimeoutMs;
    }

    public int getDefaultReadTimeoutMs() {
        return defaultReadTimeoutMs;
    }

    public void setDefaultReadTimeoutMs(int defaultReadTimeoutMs) {
        this.defaultReadTimeoutMs = defaultReadTimeoutMs;
    }

    public int getDefaultMaxPerRoute() {
        return defaultMaxPerRoute;
    }

    public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    public static class ProviderConfig {
        /**
         * Max connections per route for this provider. Default: 5.
         */
        private int maxPerRoute = 5;

        public int getMaxPerRoute() {
            return maxPerRoute;
        }

        public void setMaxPerRoute(int maxPerRoute) {
            this.maxPerRoute = maxPerRoute;
        }
    }
}
