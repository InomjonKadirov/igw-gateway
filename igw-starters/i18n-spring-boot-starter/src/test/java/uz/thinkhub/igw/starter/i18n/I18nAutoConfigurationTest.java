package uz.thinkhub.igw.starter.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class I18nAutoConfigurationTest {

    @Test
    void registersResolverAndProperties() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(I18nAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AcceptLanguageResolver.class);
                    assertThat(context).hasSingleBean(IgwI18nProperties.class);
                });
    }

    @Test
    void defaultSupportedLocalesAreEnRuUzlUzc() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(I18nAutoConfiguration.class))
                .run(context -> {
                    IgwI18nProperties props = context.getBean(IgwI18nProperties.class);
                    assertThat(props.getSupportedLocales())
                            .extracting(Locale::toLanguageTag)
                            .containsExactly("en", "ru", "uzl", "uzc");
                    assertThat(props.getDefaultLocale().toLanguageTag()).isEqualTo("en");
                    assertThat(props.getAttributeName()).isEqualTo("igw.locale");
                });
    }
}
