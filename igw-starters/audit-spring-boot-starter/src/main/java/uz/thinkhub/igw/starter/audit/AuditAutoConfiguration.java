package uz.thinkhub.igw.starter.audit;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(IgwAuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditService jdbcAuditService(JdbcTemplate jdbc) {
        return new JdbcAuditService(jdbc);
    }

    @Bean
    @ConditionalOnMissingBean
    public PiiMaskingConverter piiMaskingConverter(IgwAuditProperties properties) {
        return new PiiMaskingConverter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FailClosedAuditInterceptor failClosedAuditInterceptor(AuditService auditService,
                                                                IgwAuditProperties properties) {
        return new FailClosedAuditInterceptor(auditService, properties);
    }

    /**
     * Registers the {@link FailClosedAuditInterceptor} with Spring MVC's
     * interceptor chain. The bean is a {@link WebMvcConfigurer} that
     * Spring picks up automatically and applies.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebMvcConfigurer auditInterceptorConfigurer(FailClosedAuditInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
