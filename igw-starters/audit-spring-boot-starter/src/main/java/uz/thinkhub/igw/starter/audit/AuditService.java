package uz.thinkhub.igw.starter.audit;

/**
 * Persists audit events for each gateway call.
 *
 * <p>Two-phase API to support fail-closed semantics:
 * <ul>
 *   <li>{@link #recordStart(AuditEvent)} — called from the interceptor's
 *       {@code preHandle}. If this throws and the interceptor is in
 *       {@code fail-closed} mode, the request is rejected with HTTP 503.</li>
 *   <li>{@link #recordComplete(String, int, int, String)} — best-effort
 *       update from {@code afterCompletion} with the final status, latency,
 *       and PII-masked request body. Failures here are logged and swallowed;
 *       the row from {@code recordStart} is still present.</li>
 * </ul>
 *
 * <p>Implementations must be safe to call concurrently.
 */
public interface AuditService {

    /**
     * Insert the initial audit row. {@link AuditEvent#status()},
     * {@link AuditEvent#latencyMs()}, and {@link AuditEvent#maskedBody()}
     * may be placeholder values — they are updated by
     * {@link #recordComplete(String, int, int, String)}.
     */
    void recordStart(AuditEvent event);

    /**
     * Update the existing row (matched by {@code correlationId}) with the
     * final status, latency, and PII-masked request body. Best-effort.
     */
    void recordComplete(String correlationId, int status, int latencyMs, String maskedBody);
}
