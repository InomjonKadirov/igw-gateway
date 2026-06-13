package uz.thinkhub.igw.starter.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.i18n")
public class IgwI18nProperties {

    /**
     * Locale to use when the client does not send {@code Accept-Language}
     * or when the requested locale is not in {@link #supportedLocales}.
     * Default: {@code en}.
     */
    private Locale defaultLocale = Locale.forLanguageTag("en");

    /**
     * Locales the gateway is willing to serve. The resolver matches the
     * client's {@code Accept-Language} against this list using the standard
     * HTTP language-range algorithm.
     * Default: {@code en, ru, uzl, uzc}.
     */
    private List<Locale> supportedLocales = List.of(
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("ru"),
            Locale.forLanguageTag("uzl"),
            Locale.forLanguageTag("uzc")
    );

    /**
     * Request attribute name under which the resolved {@link Locale} is
     * exposed. Downstream code reads it via {@link I18nContextHolder}.
     * Default: {@code igw.locale}.
     */
    private String attributeName = "igw.locale";

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(List<Locale> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
}
