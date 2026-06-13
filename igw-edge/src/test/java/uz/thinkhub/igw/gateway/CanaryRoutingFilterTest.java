package uz.thinkhub.igw.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CanaryRoutingFilterTest {

    private IgwEdgeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IgwEdgeProperties();
    }

    @Test
    void zeroWeightNeverRoutesToCanary() throws Exception {
        properties.getCanary().setWeightNew(0);
        properties.getCanary().setNewUri("http://localhost:8082");
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, stubRandom(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedHeader).isNull();
    }

    @Test
    void emptyNewUriFailsClosedEvenIfWeightIsHigh() throws Exception {
        // Guard: if new-uri is empty, never route to canary (would 502).
        properties.getCanary().setWeightNew(100);
        properties.getCanary().setNewUri("");
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, stubRandom(50));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedHeader).isNull();
    }

    @Test
    void fullWeightAlwaysRoutesToCanary() throws Exception {
        properties.getCanary().setWeightNew(100);
        properties.getCanary().setNewUri("http://localhost:8082");
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, stubRandom(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedHeader).isEqualTo("new");
    }

    @Test
    void weightFiftyProducesBothOutcomesOverManyCalls() throws Exception {
        properties.getCanary().setWeightNew(50);
        properties.getCanary().setNewUri("http://localhost:8082");
        // Alternating 49 (below 50 → canary) and 50 (>= 50 → legacy) — exact 50/50 split.
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, stubRandom(49, 50));

        int canaryCount = 0;
        int legacyCount = 0;
        for (int i = 0; i < 200; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
            filter.doFilter(request, response, chain);
            if ("new".equals(chain.capturedHeader)) {
                canaryCount++;
            } else {
                legacyCount++;
            }
        }
        assertThat(canaryCount).isEqualTo(100);
        assertThat(legacyCount).isEqualTo(100);
    }

    @Test
    void weightTwentyProducesApproximatelyTwentyPercentCanary() throws Exception {
        properties.getCanary().setWeightNew(20);
        properties.getCanary().setNewUri("http://localhost:8082");
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, new Random(42));

        int canaryCount = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
            filter.doFilter(request, response, chain);
            if ("new".equals(chain.capturedHeader)) {
                canaryCount++;
            }
        }
        // Statistical assertion with reasonable tolerance.
        assertThat(canaryCount).isBetween(150, 250);
    }

    @Test
    void preExistingXCanaryHeaderIsReplaced() throws Exception {
        properties.getCanary().setWeightNew(100);
        properties.getCanary().setNewUri("http://localhost:8082");
        CanaryRoutingFilter filter = new CanaryRoutingFilter(properties, stubRandom(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CanaryRoutingFilter.CANARY_HEADER, "user-set-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicCanaryHeaderCapture chain = new AtomicCanaryHeaderCapture();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedHeader).isEqualTo("new");
    }

    /** Returns a {@link Random} that yields the given ints in order. */
    private static Random stubRandom(int... values) {
        List<Integer> ints = new ArrayList<>();
        for (int v : values) ints.add(v);
        return new Random() {
            private int idx = 0;
            @Override
            public int nextInt(int bound) {
                int v = ints.get(idx % ints.size());
                idx++;
                return v;
            }
        };
    }

    /** Captures the {@code X-Canary} header that the wrapped request exposes. */
    private static class AtomicCanaryHeaderCapture extends MockFilterChain {
        String capturedHeader;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws java.io.IOException, jakarta.servlet.ServletException {
            jakarta.servlet.http.HttpServletRequest http = (jakarta.servlet.http.HttpServletRequest) request;
            this.capturedHeader = http.getHeader(CanaryRoutingFilter.CANARY_HEADER);
            super.doFilter(request, response);
        }
    }
}
