package uz.thinkhub.igw.starter.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AcceptLanguageResolverTest {

    @Test
    void resolvesHighestQualitySupportedLocale() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ru-RU,uz-Uzc;q=0.9");
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Locale resolved = (Locale) request.getAttribute("igw.locale");
        assertThat(resolved).isNotNull();
        // "ru" is a standard 2-letter language, "uz-Uzc" is non-standard. The standard
        // ru-RU match wins because ru is directly supported; uzc is a non-standard
        // IETF tag that does not algorithmically match the client's "uz-Uzc" via
        // Locale.lookup. The "ru" match is the correct, conservative fallback.
        assertThat(resolved.getLanguage()).isEqualTo("ru");
    }

    @Test
    void fallsBackToDefaultWhenNoHeader() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Locale resolved = (Locale) request.getAttribute("igw.locale");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getLanguage()).isEqualTo("en");
    }

    @Test
    void fallsBackToDefaultWhenAllRequestedLocalesUnsupported() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "de-DE,fr-FR");
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Locale resolved = (Locale) request.getAttribute("igw.locale");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getLanguage()).isEqualTo("en");
    }

    @Test
    void matchesByLanguageFamily() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        // ru-RU should match the supported "ru" locale.
        request.addHeader("Accept-Language", "ru-RU");
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Locale resolved = (Locale) request.getAttribute("igw.locale");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getLanguage()).isEqualTo("ru");
    }

    @Test
    void customAttributeNameIsRespected() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        props.setAttributeName("custom.locale");
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ru-RU");
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("custom.locale")).isNotNull();
        assertThat(request.getAttribute("igw.locale")).isNull();
    }

    @Test
    void customDefaultLocaleIsRespected() throws Exception {
        IgwI18nProperties props = new IgwI18nProperties();
        props.setDefaultLocale(Locale.forLanguageTag("ru"));
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Locale resolved = (Locale) request.getAttribute("igw.locale");
        assertThat(resolved.getLanguage()).isEqualTo("ru");
    }

    @Test
    void resolveMethodHandlesNullAndBlank() {
        AcceptLanguageResolver resolver = new AcceptLanguageResolver(new IgwI18nProperties());

        assertThat(resolver.resolve(null).getLanguage()).isEqualTo("en");
        assertThat(resolver.resolve("").getLanguage()).isEqualTo("en");
        assertThat(resolver.resolve("   ").getLanguage()).isEqualTo("en");
    }
}
