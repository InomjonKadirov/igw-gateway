package uz.thinkhub.igw.starter.i18n;

import java.util.Locale;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Static accessor for the current request's resolved {@link Locale}.
 *
 * <p>Reads from a request attribute set by
 * {@link AcceptLanguageResolver#doFilterInternal}. Falls back to
 * {@link Locale#ENGLISH} when the attribute is missing (e.g. outside a
 * request scope, or when no resolver is registered).
 *
 * <p>Designed for use by service-layer code (e.g. provider adapters that
 * need to localize error messages) without taking the request as a
 * parameter. Backed by Spring's {@link RequestContextHolder}, which
 * uses a {@code ThreadLocal} — safe under virtual threads because each
 * virtual thread has its own thread-local slot.
 */
public final class I18nContextHolder {

    public static final String DEFAULT_ATTRIBUTE_NAME = "igw.locale";

    private I18nContextHolder() {
    }

    /**
     * Look up the {@link Locale} stored under {@value #DEFAULT_ATTRIBUTE_NAME}
     * on the current request, falling back to {@link Locale#ENGLISH} if
     * the attribute is absent or there's no current request.
     */
    public static Locale currentLocale() {
        return currentLocale(DEFAULT_ATTRIBUTE_NAME);
    }

    /**
     * Look up the {@link Locale} stored under the given attribute name
     * on the current request, falling back to {@link Locale#ENGLISH} if
     * the attribute is absent or there's no current request.
     */
    public static Locale currentLocale(String attributeName) {
        ServletRequestAttributes attrs = requestAttributes();
        if (attrs == null) {
            return Locale.ENGLISH;
        }
        Object value = attrs.getRequest().getAttribute(attributeName);
        if (value instanceof Locale locale) {
            return locale;
        }
        return Locale.ENGLISH;
    }

    private static ServletRequestAttributes requestAttributes() {
        try {
            var attrs = RequestContextHolder.currentRequestAttributes();
            return attrs instanceof ServletRequestAttributes servlet ? servlet : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
