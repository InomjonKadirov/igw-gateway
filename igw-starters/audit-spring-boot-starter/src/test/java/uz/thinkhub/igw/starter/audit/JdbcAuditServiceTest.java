package uz.thinkhub.igw.starter.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAuditServiceTest {

    private JdbcTemplate jdbc;
    private JdbcAuditService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new JdbcAuditService(jdbc);
    }

    @Test
    void recordStartIssuesInsertWithAllFields() {
        AuditEvent event = new AuditEvent(
                "corr-1", "user-1", "POST", "/api/v1/pay",
                -1, -1, "");

        service.recordStart(event);

        verify(jdbc).update(
                eq(JdbcAuditService.INSERT_SQL),
                eq("corr-1"), eq("user-1"), eq("POST"), eq("/api/v1/pay"),
                eq(-1), eq(-1), eq(""));
    }

    @Test
    void recordCompleteIssuesUpdateOnCorrelationId() {
        service.recordComplete("corr-1", 200, 42, "");

        verify(jdbc).update(
                eq(JdbcAuditService.UPDATE_SQL),
                eq(200), eq(42), eq(""), eq("corr-1"));
    }

    @Test
    void recordStartPropagatesJdbcFailures() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("db down"));

        AuditEvent event = AuditEvent.forStart("corr-1", "user-1", "GET", "/test");

        try {
            service.recordStart(event);
            org.assertj.core.api.Assertions.fail("expected DataAccessResourceFailureException");
        } catch (org.springframework.dao.DataAccessResourceFailureException e) {
            // expected
        }
    }
}
