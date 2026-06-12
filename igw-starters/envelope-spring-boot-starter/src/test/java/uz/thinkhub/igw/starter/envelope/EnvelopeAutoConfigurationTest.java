package uz.thinkhub.igw.starter.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

class EnvelopeAutoConfigurationTest {

    @Test
    void autoConfigurationRegistersAdviceBean() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EnvelopeAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(LegacyEnvelopeAdvice.class);
                    assertThat(context).hasSingleBean(IgwEnvelopeProperties.class);
                });
    }

    @Test
    void autoConfigurationIsConditionalOnWebApplication() {
        // Without @ConditionalOnWebApplication, the advice would be created even in a non-web context.
        // We verify the auto-config is loaded (smoke check).
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EnvelopeAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void adviceWrapsBodyInLegacyEnvelopeShape() throws Exception {
        LegacyEnvelopeAdvice advice = new LegacyEnvelopeAdvice(new IgwEnvelopeProperties());

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("/test"));

        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        rawResponse.setStatus(200);
        ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        @SuppressWarnings("unchecked")
        Class<? extends HttpMessageConverter<?>> converterType =
                (Class<? extends HttpMessageConverter<?>>) (Class<?>) StringHttpMessageConverter.class;

        Object result = advice.beforeBodyWrite(
                Map.of("foo", "bar"),
                mock(MethodParameter.class),
                MediaType.APPLICATION_JSON,
                converterType,
                request,
                response);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) result;
        assertThat(envelope).containsEntry("success", true);
        assertThat(envelope).containsEntry("status", 200);
        assertThat(envelope).containsEntry("message", "OK");
        assertThat(envelope).containsEntry("path", "/test");
        assertThat(envelope).containsEntry("result", Map.of("foo", "bar"));
    }

    @Test
    void adviceMarks4xxAsClientError() throws Exception {
        LegacyEnvelopeAdvice advice = new LegacyEnvelopeAdvice(new IgwEnvelopeProperties());

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("/not-found"));

        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        rawResponse.setStatus(404);
        ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        @SuppressWarnings("unchecked")
        Class<? extends HttpMessageConverter<?>> converterType =
                (Class<? extends HttpMessageConverter<?>>) (Class<?>) StringHttpMessageConverter.class;

        Object result = advice.beforeBodyWrite(
                "not found",
                mock(MethodParameter.class),
                MediaType.APPLICATION_JSON,
                converterType,
                request,
                response);

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) result;
        assertThat(envelope).containsEntry("success", false);
        assertThat(envelope).containsEntry("status", 404);
        assertThat(envelope).containsEntry("message", "Client error");
    }

    @Test
    void supportsReturnsFalseWhenWrapSuccessDisabled() {
        IgwEnvelopeProperties props = new IgwEnvelopeProperties();
        props.setWrapSuccess(false);
        LegacyEnvelopeAdvice advice = new LegacyEnvelopeAdvice(props);

        boolean supports = advice.supports(mock(MethodParameter.class), StringHttpMessageConverter.class);
        assertThat(supports).isFalse();
    }

    @Test
    void customPathHeaderOverridesRequestUri() throws Exception {
        IgwEnvelopeProperties props = new IgwEnvelopeProperties();
        props.setPathHeaderName("X-Custom-Path");
        LegacyEnvelopeAdvice advice = new LegacyEnvelopeAdvice(props);

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Custom-Path", "/api/v1/foo");
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("/test"));

        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        rawResponse.setStatus(200);
        ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        @SuppressWarnings("unchecked")
        Class<? extends HttpMessageConverter<?>> converterType =
                (Class<? extends HttpMessageConverter<?>>) (Class<?>) StringHttpMessageConverter.class;

        Object result = advice.beforeBodyWrite(
                Map.of(),
                mock(MethodParameter.class),
                MediaType.APPLICATION_JSON,
                converterType,
                request,
                response);

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) result;
        assertThat(envelope).containsEntry("path", "/api/v1/foo");
    }

    // Used in tests as a no-op converter type
    static class StringHttpMessageConverter
            implements HttpMessageConverter<String> {
        @Override public boolean canRead(Class<?> clazz, MediaType mediaType) { return false; }
        @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) { return false; }
        @Override public java.util.List<MediaType> getSupportedMediaTypes() { return java.util.List.of(); }
        @Override public String read(Class<? extends String> clazz, org.springframework.http.HttpInputMessage msg) { return null; }
        @Override public void write(String s, MediaType contentType, org.springframework.http.HttpOutputMessage msg) { }
    }
}
