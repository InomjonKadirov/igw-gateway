package uz.thinkhub.igw.starter.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that records an audit row for every gateway call.
 *
 * <p>{@code preHandle}:
 * <ul>
 *   <li>Reads {@code correlation-id} and {@code user-id} from request
 *       attributes (set by upstream filters; the correlation-id filter
 *       and the JWT filter are added by the gateway in PR #8+).</li>
 *   <li>Records the start of the request via
 *       {@link AuditService#recordStart(AuditEvent)}.</li>
 *   <li>If {@code fail-closed} and the write fails, the request is
 *       rejected with HTTP 503 and the chain is short-circuited.</li>
 * </ul>
 *
 * <p>{@code afterCompletion}: best-effort update with the final status,
 * latency, and PII-masked request body. Failures are logged and swallowed.
 *
 * <p>Architecture rule: audit must never silently drop a row when
 * {@code fail-closed} is enabled.
 */
@Component
public class FailClosedAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FailClosedAuditInterceptor.class);

    public static final String START_TIME_ATTR = "igw.audit.start-time";

    private final AuditService auditService;
    private final IgwAuditProperties properties;

    public FailClosedAuditInterceptor(AuditService auditService, IgwAuditProperties properties) {
        this.auditService = auditService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        String correlationId = (String) request.getAttribute("igw.correlation-id");
        String userId = (String) request.getAttribute("igw.user-id");
        AuditEvent startEvent = AuditEvent.forStart(
                correlationId, userId, request.getMethod(), request.getRequestURI());

        try {
            auditService.recordStart(startEvent);
        } catch (Exception e) {
            if (properties.isFailClosed()) {
                log.error("Audit row write failed (fail-closed). Rejecting request. correlationId={}",
                        correlationId, e);
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                return false;
            }
            log.warn("Audit row write failed (fail-open). Continuing. correlationId={}", correlationId, e);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        int latencyMs = startTime != null ? (int) (System.currentTimeMillis() - startTime) : -1;
        String correlationId = (String) request.getAttribute("igw.correlation-id");

        try {
            auditService.recordComplete(correlationId, response.getStatus(), latencyMs, "");
        } catch (Exception e) {
            log.warn("Audit row update failed. correlationId={}", correlationId, e);
        }
    }
}
