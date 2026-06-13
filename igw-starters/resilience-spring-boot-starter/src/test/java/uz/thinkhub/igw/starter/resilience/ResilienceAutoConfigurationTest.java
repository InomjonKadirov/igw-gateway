package uz.thinkhub.igw.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ResilienceAutoConfigurationTest {

    @Test
    void registersConnectionManagerAndHttpClient() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(PoolingHttpClientConnectionManager.class);
                    assertThat(context).hasSingleBean(HttpClient.class);
                });
    }

    @Test
    void resilience4jCircuitBreakerAnnotationIsOnClasspath() {
        // Verify the resilience4j-spring-boot3 starter is on the test runtime classpath
        // (transitively via this starter's implementation deps). Per-provider
        // CircuitBreaker / Bulkhead instances are configured by the consuming service
        // in application.yaml under resilience4j.circuitbreaker.instances.<name>.* and
        // resilience4j.bulkhead.instances.<name>.* — this starter does not register them.
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(ClassLoader.getSystemClassLoader()
                            .getResource(
                                    "io/github/resilience4j/circuitbreaker/annotation/CircuitBreaker.class"))
                            .isNotNull();
                });
    }

    @Test
    void perProviderConfigIsApplied() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "igw.resilience.providers.iabs.max-per-route=20",
                        "igw.resilience.providers.humo.max-per-route=15")
                .run(context -> {
                    IgwResilienceProperties props = context.getBean(IgwResilienceProperties.class);
                    assertThat(props.getProviders()).containsKeys("iabs", "humo");
                    assertThat(props.getProviders().get("iabs").getMaxPerRoute()).isEqualTo(20);
                    assertThat(props.getProviders().get("humo").getMaxPerRoute()).isEqualTo(15);
                });
    }
}
