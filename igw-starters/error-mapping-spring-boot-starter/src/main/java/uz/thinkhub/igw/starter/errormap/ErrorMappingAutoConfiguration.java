package uz.thinkhub.igw.starter.errormap;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication
public class ErrorMappingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProviderErrorTranslatorRegistry providerErrorTranslatorRegistry() {
        return new ProviderErrorTranslatorRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorMappingFilter errorMappingFilter(ProviderErrorTranslatorRegistry registry) {
        return new ErrorMappingFilter(registry);
    }
}
