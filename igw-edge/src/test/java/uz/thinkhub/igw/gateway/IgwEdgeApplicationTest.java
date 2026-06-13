package uz.thinkhub.igw.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Smoke test that the igw-edge application context starts with the gateway
 * starter on the classpath and the {@code IgwEdgeProperties} are bound.
 *
 * <p>The end-to-end smoke (bootRun + curl through to echo-server) is
 * exercised in the local docker-compose stack in PR #12; this test runs
 * without Docker.
 */
@SpringBootTest(
        classes = IgwEdgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.cloud.gateway.server.webmvc.enabled=true"
)
class IgwEdgeApplicationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void igwEdgePropertiesAreBound() {
        IgwEdgeProperties props = context.getBean(IgwEdgeProperties.class);
        assertThat(props.getUpstream()).isEqualTo("http://localhost:8081");
        assertThat(props.getCanary().getRouteIdLegacy()).isEqualTo("legacy");
        assertThat(props.getCanary().getRouteIdNew()).isEqualTo("new");
        assertThat(props.getCanary().getWeightNew()).isZero();
        assertThat(props.getCanary().getNewUri()).isEmpty();
    }
}
