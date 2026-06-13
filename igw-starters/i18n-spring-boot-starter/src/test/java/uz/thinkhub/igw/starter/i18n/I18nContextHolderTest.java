package uz.thinkhub.igw.starter.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class I18nContextHolderTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsLocaleFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Locale expected = Locale.forLanguageTag("ru");
        request.setAttribute("igw.locale", expected);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(I18nContextHolder.currentLocale()).isEqualTo(expected);
    }

    @Test
    void usesCustomAttributeName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Locale expected = Locale.forLanguageTag("uzc");
        request.setAttribute("custom.attr", expected);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(I18nContextHolder.currentLocale("custom.attr")).isEqualTo(expected);
        assertThat(I18nContextHolder.currentLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void fallsBackToEnglishWhenNoRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
        assertThat(I18nContextHolder.currentLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void fallsBackToEnglishWhenAttributeMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(I18nContextHolder.currentLocale()).isEqualTo(Locale.ENGLISH);
    }
}
