package uz.thinkhub.igw.starter.audit;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC-backed {@link AuditService} writing to the {@code request} table.
 *
 * <p>The table is created and migrated by the consuming service. This
 * starter does <em>not</em> own the schema.
 */
public class JdbcAuditService implements AuditService {

    static final String INSERT_SQL = """
            INSERT INTO request
                (correlation_id, user_id, method, path, status, latency_ms, masked_body, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            """;

    static final String UPDATE_SQL = """
            UPDATE request
            SET status = ?, latency_ms = ?, masked_body = ?
            WHERE correlation_id = ?
            """;

    private final JdbcTemplate jdbc;

    public JdbcAuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordStart(AuditEvent event) {
        jdbc.update(INSERT_SQL,
                event.correlationId(),
                event.userId(),
                event.method(),
                event.path(),
                event.status(),
                event.latencyMs(),
                event.maskedBody());
    }

    @Override
    public void recordComplete(String correlationId, int status, int latencyMs, String maskedBody) {
        jdbc.update(UPDATE_SQL, status, latencyMs, maskedBody, correlationId);
    }
}
