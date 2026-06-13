package uz.thinkhub.igw.starter.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(IgwI18nProperties.class)
public class I18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AcceptLanguageResolver acceptLanguageResolver(IgwI18nProperties properties) {
        return new AcceptLanguageResolver(properties);
    }
}
