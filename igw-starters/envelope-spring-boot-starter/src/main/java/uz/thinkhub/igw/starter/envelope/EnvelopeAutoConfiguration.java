package uz.thinkhub.igw.starter.envelope;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(IgwEnvelopeProperties.class)
public class EnvelopeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LegacyEnvelopeAdvice legacyEnvelopeAdvice(IgwEnvelopeProperties properties) {
        return new LegacyEnvelopeAdvice(properties);
    }
}
