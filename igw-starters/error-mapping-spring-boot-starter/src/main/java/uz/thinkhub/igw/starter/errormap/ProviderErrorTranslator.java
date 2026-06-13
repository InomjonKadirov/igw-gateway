package uz.thinkhub.igw.starter.errormap;

import org.springframework.http.MediaType;

/**
 * Translates a provider's raw error response bytes into the legacy envelope
 * shape that igw-edge returns to clients.
 *
 * <p>Per-provider implementations are registered into the
 * {@link ProviderErrorTranslatorRegistry} keyed by provider name (the value
 * of the {@code X-Provider} request header).
 *
 * <p>Implementations must be safe to call concurrently.
 */
public interface ProviderErrorTranslator {

    /**
     * Translate the provider's error response.
     *
     * @param status      the upstream HTTP status code (4xx or 5xx)
     * @param providerBody the raw response body bytes from the provider
     * @param contentType the response Content-Type, or {@code null} if unknown
     * @return the translated response body bytes (the body to return to the client)
     */
    byte[] translate(int status, byte[] providerBody, MediaType contentType);
}
