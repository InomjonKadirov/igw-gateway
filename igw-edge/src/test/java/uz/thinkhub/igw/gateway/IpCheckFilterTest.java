package uz.thinkhub.igw.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.thinkhub.igw.starter.tokencache.Token;
import uz.thinkhub.igw.starter.tokencache.TokenCache;

class IpCheckFilterTest {

    private final TokenCache tokenCache = mock(TokenCache.class);
    private final IgwEdgeProperties properties = new IgwEdgeProperties();
    private final IpCheckFilter filter = new IpCheckFilter(tokenCache, properties);

    @BeforeEach
    void setUp() {
        properties.getSecurity().getIpCheck().setEnabled(true);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughWhenDisabled() throws Exception {
        properties.getSecurity().getIpCheck().setEnabled(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Even if the auth context has a user and the cache has a different IP,
        // the filter passes through when disabled.
        authenticateAs("user-1");
        when(tokenCache.get("user-1")).thenReturn(
                Optional.of(new Token("9.9.9.9", Instant.now().plusSeconds(60))));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void passesThroughWhenAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // No authentication; security filter chain will reject with 401 elsewhere.
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void passesThroughWhenCacheMiss() throws Exception {
        authenticateAs("user-1");
        when(tokenCache.get("user-1")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        // Phase 0: cache miss fails open.
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsRequestWhenCachedIpMatches() throws Exception {
        authenticateAs("user-1");
        when(tokenCache.get("user-1")).thenReturn(
                Optional.of(new Token("1.2.3.4", Instant.now().plusSeconds(60))));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequestWhenCachedIpMismatches() throws Exception {
        authenticateAs("user-1");
        when(tokenCache.get("user-1")).thenReturn(
                Optional.of(new Token("1.2.3.4", Instant.now().plusSeconds(60))));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");  // different from cached
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    private static void authenticateAs(String userId) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId, "n/a", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
