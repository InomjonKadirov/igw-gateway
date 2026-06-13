package uz.thinkhub.igw.starter.errormap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ErrorMappingFilterTest {

    @Test
    void invokesTranslatorWhenProviderHeaderAnd4xxStatus() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<Integer> seenStatus = new AtomicReference<>();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            callCount.incrementAndGet();
            seenStatus.set(status);
            return body;
        };

        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("iabs", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("X-Provider", "iabs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(seenStatus.get()).isEqualTo(404);
    }

    @Test
    void invokesTranslatorWhenProviderHeaderAnd5xxStatus() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            callCount.incrementAndGet();
            return body;
        };
        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("iabs", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("X-Provider", "iabs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void doesNotInvokeTranslatorWhenProviderHeaderMissing() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            callCount.incrementAndGet();
            return body;
        };
        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("iabs", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(callCount.get()).isZero();
    }

    @Test
    void doesNotInvokeTranslatorWhenResponseIs2xx() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            callCount.incrementAndGet();
            return body;
        };
        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("iabs", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("X-Provider", "iabs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.OK.value());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(callCount.get()).isZero();
    }

    @Test
    void doesNotInvokeTranslatorWhenNoTranslatorRegisteredForProvider() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            callCount.incrementAndGet();
            return body;
        };
        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("different-provider", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("X-Provider", "iabs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(callCount.get()).isZero();
    }

    @Test
    void passesContentTypeToTranslator() throws Exception {
        AtomicReference<MediaType> seenContentType = new AtomicReference<>();
        ProviderErrorTranslator translator = (status, body, contentType) -> {
            seenContentType.set(contentType);
            return body;
        };
        ProviderErrorTranslatorRegistry registry = new ProviderErrorTranslatorRegistry();
        registry.register("iabs", translator);
        ErrorMappingFilter filter = new ErrorMappingFilter(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("X-Provider", "iabs");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(seenContentType.get()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
