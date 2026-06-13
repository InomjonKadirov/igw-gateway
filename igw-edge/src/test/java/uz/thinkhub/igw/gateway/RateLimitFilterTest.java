package uz.thinkhub.igw.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

    private IgwEdgeProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new IgwEdgeProperties();
        properties.getRateLimit().setRps(1);     // 1 token/second
        properties.getRateLimit().setBurst(3);   // capacity 3
        filter = new RateLimitFilter(properties);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughWhenAnonymous() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void allowsBurstThenRateLimits() throws Exception {
        authenticateAs("user-1");

        // Burst of 3 — first 3 should pass
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }

        // 4th request exceeds the burst (no time for refill)
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    }

    @Test
    void perUserBucketsAreIndependent() throws Exception {
        // user-1 burns their burst
        authenticateAs("user-1");
        for (int i = 0; i < 3; i++) {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), limited, new MockFilterChain());
        assertThat(limited.getStatus()).isEqualTo(429);

        // user-2 has their own bucket
        authenticateAs("user-2");
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), other, new MockFilterChain());
        assertThat(other.getStatus()).isEqualTo(200);
    }

    private static void authenticateAs(String userId) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId, "n/a", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
