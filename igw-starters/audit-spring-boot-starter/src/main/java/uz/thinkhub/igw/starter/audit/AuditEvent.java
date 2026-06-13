package uz.thinkhub.igw.starter.audit;

/**
 * The audit row written to the {@code request} table for each gateway call.
 *
 * <p>The schema (created by the consuming service, not by this starter):
 * <pre>
 *   CREATE TABLE request (
 *     id              BIGSERIAL PRIMARY KEY,
 *     correlation_id  VARCHAR(64),
 *     user_id         VARCHAR(64),
 *     method          VARCHAR(10),
 *     path            VARCHAR(1024),
 *     status          INT,
 *     latency_ms      INT,
 *     masked_body     TEXT,
 *     created_at      TIMESTAMP NOT NULL DEFAULT NOW()
 *   );
 * </pre>
 */
public record AuditEvent(
        String correlationId,
        String userId,
        String method,
        String path,
        int status,
        int latencyMs,
        String maskedBody
) {
    /**
     * Construct an initial event for the {@code preHandle} write. Status and
     * latency are placeholders (use {@code -1}) — the actual values are
     * written by {@link AuditService#recordComplete} in
     * {@code afterCompletion}.
     */
    public static AuditEvent forStart(String correlationId, String userId, String method, String path) {
        return new AuditEvent(correlationId, userId, method, path, -1, -1, "");
    }
}
