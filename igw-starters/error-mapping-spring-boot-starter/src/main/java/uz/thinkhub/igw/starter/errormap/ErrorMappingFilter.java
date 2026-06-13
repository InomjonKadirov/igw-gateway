package uz.thinkhub.igw.starter.errormap;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Looks up the {@code X-Provider} request header and, when the upstream
 * response is 4xx/5xx, asks the {@link ProviderErrorTranslatorRegistry}
 * for the matching translator and applies it.
 *
 * <p>Implemented as a servlet {@code Filter} (extending Spring's
 * {@link OncePerRequestFilter}) per the architecture's "no WebFlux, no
 * reactive types in our code" rule. The plan's "WebFilter" terminology
 * refers to the same intent: a request-stage filter that inspects the
 * response after the chain runs.
 *
 * <p>Phase 0 implementation: the filter invokes the translator (when the
 * conditions are met) with an empty body. Per-provider translators come in
 * Phase 1+; the no-op default just leaves the upstream body unchanged.
 * Body buffering / replacement is wired up in PR #8 (igw-edge gateway
 * integration) using a servlet response wrapper.
 */
@Component
public class ErrorMappingFilter extends OncePerRequestFilter {

    public static final String PROVIDER_HEADER = "X-Provider";

    private final ProviderErrorTranslatorRegistry registry;

    public ErrorMappingFilter(ProviderErrorTranslatorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provider = request.getHeader(PROVIDER_HEADER);
        if (provider == null) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(request, response);
        applyIfError(request, response, provider);
    }

    private void applyIfError(HttpServletRequest request,
                              HttpServletResponse response,
                              String provider) {
        int status = response.getStatus();
        if (status < 400) {
            return;
        }

        Optional<ProviderErrorTranslator> translator = registry.get(provider);
        if (translator.isEmpty()) {
            return;
        }

        // Phase 0: in the real WebMVC gateway we'd buffer the upstream body via
        // ContentCachingResponseWrapper, then write the translated bytes back.
        // For now, invoke the translator with an empty body — the gateway
        // integration in PR #8 will plug in the actual body capture.
        MediaType contentType = parseContentType(response.getContentType());
        HttpStatus resolved = HttpStatus.resolve(status);
        translator.get().translate(
                resolved != null ? resolved.value() : status,
                new byte[0],
                contentType);
    }

    private static MediaType parseContentType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (Exception e) {
            return null;
        }
    }
}
