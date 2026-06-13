package uz.thinkhub.igw.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void readsCorrelationIdFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "client-supplied-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(CorrelationIdFilter.ATTR_NAME))
                .isEqualTo("client-supplied-id");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo("client-supplied-id");
    }

    @Test
    void generatesUuidWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Object generated = request.getAttribute(CorrelationIdFilter.ATTR_NAME);
        assertThat(generated).isNotNull();
        assertThat(generated.toString()).hasSize(36);  // UUID
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(generated);
    }

    @Test
    void putsCorrelationIdInMdcAndClearsItAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "mdc-test-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture the MDC value mid-filter via a FilterChain that reads MDC.
        MDC.clear();
        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("mdc-test-id");
        });
        // After the filter returns, MDC should be cleared.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesNewIdWhenHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Object generated = request.getAttribute(CorrelationIdFilter.ATTR_NAME);
        assertThat(generated).isNotNull();
        assertThat(generated.toString()).isNotEqualTo("   ");
    }
}
