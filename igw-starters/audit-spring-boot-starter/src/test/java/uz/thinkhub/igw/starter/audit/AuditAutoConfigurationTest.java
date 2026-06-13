package uz.thinkhub.igw.starter.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditAutoConfigurationTest {

    @Test
    void registersAuditServiceAndJdbcTemplateWhenDataSourceIsPresent() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .withUserConfiguration(TestDataSourceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JdbcTemplate.class);
                    assertThat(context).hasSingleBean(AuditService.class);
                    assertThat(context.getBean(AuditService.class)).isInstanceOf(JdbcAuditService.class);
                    assertThat(context).hasSingleBean(PiiMaskingConverter.class);
                    assertThat(context).hasSingleBean(FailClosedAuditInterceptor.class);
                });
    }

    @Test
    void doesNotRegisterWhenDataSourceIsMissing() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditService.class);
                    assertThat(context).doesNotHaveBean(FailClosedAuditInterceptor.class);
                });
    }

    @Test
    void respectsConditionalOnMissingBeanForAuditService() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .withUserConfiguration(TestDataSourceConfig.class, CustomAuditServiceConfig.class)
                .run(context -> {
                    // The custom AuditService bean is used; the auto-config's
                    // JdbcAuditService is NOT created.
                    assertThat(context.getBean(AuditService.class)).isSameAs(CustomAuditServiceConfig.BEAN);
                });
    }

    @Configuration
    static class TestDataSourceConfig {
        @Bean
        public DataSource dataSource() {
            // Mock DataSource; the auto-config only needs a bean to satisfy
            // @ConditionalOnBean(DataSource.class). No actual DB is touched.
            return mock(DataSource.class);
        }
    }

    @Configuration
    static class CustomAuditServiceConfig {
        static final AuditService BEAN = new AuditService() {
            @Override
            public void recordStart(AuditEvent event) {}
            @Override
            public void recordComplete(String correlationId, int status, int latencyMs, String maskedBody) {}
        };

        @Bean
        public AuditService customAuditService() {
            return BEAN;
        }
    }
}
