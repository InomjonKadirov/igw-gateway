package uz.thinkhub.igw.echo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Echo controller: any method, any path, returns a JSON document describing
 * the request. Used as the legacy upstream target in Phase 0 (the gateway
 * proxies to this; once the real legacy PHP is back online, replace this
 * with a real one in {@code local/echo-server/}).
 */
@RestController
public class EchoController {

    @RequestMapping("/**")
    @ResponseBody
    public Map<String, Object> echo(HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("method", request.getMethod());
        response.put("path", request.getRequestURI());
        response.put("query", request.getQueryString());

        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        response.put("headers", headers);

        return response;
    }
}
