package uz.thinkhub.igw.starter.envelope;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class LegacyEnvelopeAdvice implements ResponseBodyAdvice<Object> {

    private final IgwEnvelopeProperties properties;

    public LegacyEnvelopeAdvice(IgwEnvelopeProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.isWrapSuccess();
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        int status = currentStatus(response);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("success", isSuccess(status));
        envelope.put("status", status);
        envelope.put("message", defaultMessage(status));
        envelope.put("path", resolvePath(request));
        envelope.put("result", body);
        return envelope;
    }

    private static int currentStatus(ServerHttpResponse response) {
        // Spring 7.x removed the getStatusCode() getter from ServerHttpResponse.
        // We read the status from the underlying servlet response.
        if (response instanceof ServletServerHttpResponse servletResponse) {
            HttpServletResponse raw = servletResponse.getServletResponse();
            int status = raw.getStatus();
            // getStatus() can return 0 before the response is committed; fall back to 200.
            return status == 0 ? 200 : status;
        }
        return 200;
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status) {
        if (status >= 200 && status < 300) {
            return "OK";
        }
        if (status >= 400 && status < 500) {
            return "Client error";
        }
        if (status >= 500 && status < 600) {
            return "Server error";
        }
        return "Error";
    }

    private String resolvePath(ServerHttpRequest request) {
        String headerPath = request.getHeaders().getFirst(properties.getPathHeaderName());
        if (headerPath != null && !headerPath.isBlank()) {
            return headerPath;
        }
        URI uri = request.getURI();
        return uri.getPath() != null ? uri.getPath() : uri.toString();
    }
}
