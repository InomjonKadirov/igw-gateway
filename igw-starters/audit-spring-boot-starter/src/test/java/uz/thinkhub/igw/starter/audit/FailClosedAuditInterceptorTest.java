package uz.thinkhub.igw.starter.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class FailClosedAuditInterceptorTest {

    private AuditService auditService;
    private IgwAuditProperties properties;
    private FailClosedAuditInterceptor interceptor;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        properties = new IgwAuditProperties();
        interceptor = new FailClosedAuditInterceptor(auditService, properties);
    }

    @Test
    void preHandleRecordsStartAndContinues() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        org.springframework.mock.web.MockHttpServletRequest realRequest =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/pay");
        realRequest.setAttribute("igw.correlation-id", "corr-1");
        realRequest.setAttribute("igw.user-id", "user-1");

        boolean proceed = interceptor.preHandle(realRequest, response, new Object());

        assertThat(proceed).isTrue();
        verify(auditService).recordStart(any(AuditEvent.class));
    }

    @Test
    void preHandleRejectsWith503WhenFailClosedAndAuditThrows() throws Exception {
        properties.setFailClosed(true);
        interceptor = new FailClosedAuditInterceptor(auditService, properties);
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(auditService).recordStart(any(AuditEvent.class));

        org.springframework.mock.web.MockHttpServletRequest realRequest =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/pay");
        org.springframework.mock.web.MockHttpServletResponse realResponse =
                new org.springframework.mock.web.MockHttpServletResponse();
        realRequest.setAttribute("igw.correlation-id", "corr-1");

        boolean proceed = interceptor.preHandle(realRequest, realResponse, new Object());

        assertThat(proceed).isFalse();
        assertThat(realResponse.getStatus()).isEqualTo(503);
    }

    @Test
    void preHandleContinuesWhenFailOpenAndAuditThrows() throws Exception {
        properties.setFailClosed(false);
        interceptor = new FailClosedAuditInterceptor(auditService, properties);
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(auditService).recordStart(any(AuditEvent.class));

        org.springframework.mock.web.MockHttpServletRequest realRequest =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/pay");
        org.springframework.mock.web.MockHttpServletResponse realResponse =
                new org.springframework.mock.web.MockHttpServletResponse();
        realRequest.setAttribute("igw.correlation-id", "corr-1");

        boolean proceed = interceptor.preHandle(realRequest, realResponse, new Object());

        assertThat(proceed).isTrue();
        assertThat(realResponse.getStatus()).isEqualTo(200);  // unchanged
    }

    @Test
    void afterCompletionRecordsFinalStatus() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest realRequest =
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/test");
        realRequest.setAttribute("igw.correlation-id", "corr-1");
        realRequest.setAttribute(FailClosedAuditInterceptor.START_TIME_ATTR,
                System.currentTimeMillis() - 50);
        org.springframework.mock.web.MockHttpServletResponse realResponse =
                new org.springframework.mock.web.MockHttpServletResponse();
        realResponse.setStatus(200);

        interceptor.afterCompletion(realRequest, realResponse, new Object(), null);

        verify(auditService).recordComplete(eq("corr-1"), eq(200), anyInt(), eq(""));
    }

    @Test
    void afterCompletionSwallowsExceptions() throws Exception {
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(auditService).recordComplete(anyString(), anyInt(), anyInt(), anyString());

        org.springframework.mock.web.MockHttpServletRequest realRequest =
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/test");
        realRequest.setAttribute("igw.correlation-id", "corr-1");
        org.springframework.mock.web.MockHttpServletResponse realResponse =
                new org.springframework.mock.web.MockHttpServletResponse();

        // Should not throw
        interceptor.afterCompletion(realRequest, realResponse, new Object(), null);
    }
}
