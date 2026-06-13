package uz.thinkhub.igw.starter.errormap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ErrorMappingAutoConfigurationTest {

    @Test
    void registersRegistryAndFilterBeans() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorMappingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ProviderErrorTranslatorRegistry.class);
                    assertThat(context).hasSingleBean(ErrorMappingFilter.class);
                });
    }

    @Test
    void registryStartsEmpty() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorMappingAutoConfiguration.class))
                .run(context -> {
                    ProviderErrorTranslatorRegistry registry =
                            context.getBean(ProviderErrorTranslatorRegistry.class);
                    assertThat(registry.size()).isZero();
                });
    }
}
