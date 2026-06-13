package uz.thinkhub.igw.starter.i18n;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads the {@code Accept-Language} request header, picks the best match
 * against {@link IgwI18nProperties#supportedLocales}, and exposes the
 * resolved {@link Locale} as a request attribute (default name
 * {@code igw.locale}).
 *
 * <p>Servlet {@code Filter} (not WebFlux) per the architecture's
 * "no WebFlux, no reactive types in our code" rule.
 *
 * <p>The matching algorithm uses {@link Locale.LanguageRange#parse} and
 * {@link Locale#lookup(List, List)}, which honor quality values
 * ({@code q=0.9}) and language family matching ({@code ru-RU} matches
 * a supported {@code ru}).
 */
@Component
public class AcceptLanguageResolver extends OncePerRequestFilter {

    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    private final IgwI18nProperties properties;

    public AcceptLanguageResolver(IgwI18nProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Locale resolved = resolve(request.getHeader(ACCEPT_LANGUAGE_HEADER));
        request.setAttribute(properties.getAttributeName(), resolved);
        filterChain.doFilter(request, response);
    }

    Locale resolve(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null || acceptLanguageHeader.isBlank()) {
            return properties.getDefaultLocale();
        }
        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguageHeader);
        Locale matched = Locale.lookup(ranges, properties.getSupportedLocales());
        return matched != null ? matched : properties.getDefaultLocale();
    }
}
