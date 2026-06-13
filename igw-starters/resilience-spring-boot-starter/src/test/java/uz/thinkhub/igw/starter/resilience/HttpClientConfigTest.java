package uz.thinkhub.igw.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpClientConfigTest {

    private PoolingHttpClientConnectionManager manager;

    @AfterEach
    void tearDown() throws Exception {
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    void connectionManagerHasDefaultMaxPerRoute() throws Exception {
        IgwResilienceProperties props = new IgwResilienceProperties();
        manager = HttpClientConfig.createConnectionManager(props);

        // getMaxPerRoute(HttpRoute) returns the per-route limit. For an unregistered
        // route, the manager returns its default value.
        assertThat(manager.getMaxPerRoute(route("unknown.example.com"))).isEqualTo(5);
    }

    @Test
    void connectionManagerAppliesPerProviderMaxPerRoute() throws Exception {
        IgwResilienceProperties props = new IgwResilienceProperties();
        IgwResilienceProperties.ProviderConfig iabsConfig = new IgwResilienceProperties.ProviderConfig();
        iabsConfig.setMaxPerRoute(20);
        IgwResilienceProperties.ProviderConfig humoConfig = new IgwResilienceProperties.ProviderConfig();
        humoConfig.setMaxPerRoute(15);
        props.getProviders().put("iabs", iabsConfig);
        props.getProviders().put("humo", humoConfig);

        manager = HttpClientConfig.createConnectionManager(props);

        assertThat(manager.getMaxPerRoute(route("iabs.example.com"))).isEqualTo(20);
        assertThat(manager.getMaxPerRoute(route("humo.example.com"))).isEqualTo(15);
        // An unregistered route falls back to the default.
        assertThat(manager.getMaxPerRoute(route("unknown.example.com"))).isEqualTo(5);
    }

    @Test
    void customDefaultMaxPerRouteIsRespected() throws Exception {
        IgwResilienceProperties props = new IgwResilienceProperties();
        props.setDefaultMaxPerRoute(10);
        manager = HttpClientConfig.createConnectionManager(props);

        assertThat(manager.getMaxPerRoute(route("unknown.example.com"))).isEqualTo(10);
    }

    @Test
    void httpClientBuildsWithConfiguredTimeouts() throws Exception {
        IgwResilienceProperties props = new IgwResilienceProperties();
        props.setDefaultConnectTimeoutMs(2000);
        props.setDefaultReadTimeoutMs(10000);
        manager = HttpClientConfig.createConnectionManager(props);

        HttpClient client = HttpClientConfig.createHttpClient(props, manager);
        assertThat(client).isNotNull();
    }

    private static HttpRoute route(String host) {
        return new HttpRoute(new HttpHost(host, 80));
    }
}
