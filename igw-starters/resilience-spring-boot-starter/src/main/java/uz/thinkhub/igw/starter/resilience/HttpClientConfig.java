package uz.thinkhub.igw.starter.resilience;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;

/**
 * Builds the Apache HttpClient 5 {@link PoolingHttpClientConnectionManager}
 * and the {@link HttpClient} used by igw-edge for upstream calls.
 *
 * <p>The connection manager applies {@link IgwResilienceProperties#getDefaultMaxPerRoute()}
 * as the pool default and {@link IgwResilienceProperties.ProviderConfig#getMaxPerRoute()}
 * for each configured provider. The route key is derived from the provider
 * name ({@code <provider>.example.com} as a placeholder); real per-route
 * limits are wired in PR #8 (igw-edge) when each provider's upstream host
 * is known.
 *
 * <p>Resilience4j's CircuitBreaker and SemaphoreBulkhead are configured
 * separately via the {@code resilience4j.circuitbreaker.instances.<name>.*} and
 * {@code resilience4j.bulkhead.instances.<name>.*} properties — the
 * {@code resilience4j-spring-boot3} starter (transitively on the classpath)
 * handles their registration. This starter does <em>not</em> register
 * Resilience4j instances itself; per-provider instances are declared by
 * the consuming service in {@code application.yaml}.
 */
public final class HttpClientConfig {

    private HttpClientConfig() {
    }

    public static PoolingHttpClientConnectionManager createConnectionManager(IgwResilienceProperties props) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(props.getDefaultConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .setSocketTimeout(props.getDefaultReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

        PoolingHttpClientConnectionManager manager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnPerRoute(props.getDefaultMaxPerRoute())
                .setMaxConnTotal(Integer.MAX_VALUE)  // unbounded total
                .build();

        // Per-provider max-per-route is recorded on the manager keyed by a
        // placeholder HttpHost. Real wire-level routing maps the logical
        // provider name to a real upstream host:port in PR #8.
        for (Map.Entry<String, IgwResilienceProperties.ProviderConfig> entry : props.getProviders().entrySet()) {
            HttpHost host = new HttpHost(entry.getKey() + ".example.com", 80);
            manager.setMaxPerRoute(new HttpRoute(host), entry.getValue().getMaxPerRoute());
        }
        return manager;
    }

    public static HttpClient createHttpClient(IgwResilienceProperties props,
                                              PoolingHttpClientConnectionManager connectionManager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(props.getDefaultConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(props.getDefaultReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
