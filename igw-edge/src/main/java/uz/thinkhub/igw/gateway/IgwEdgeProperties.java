package uz.thinkhub.igw.gateway;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for igw-edge.
 *
 * <p>Phase 0 keys:
 * <ul>
 *   <li>{@code igw.edge.upstream} — legacy PHP upstream URL (the
 *       strangler-fig default target; 100% of traffic goes here until
 *       PR #11 introduces canary routing).</li>
 *   <li>{@code igw.edge.canary.weight-new} — 0..100 weight for the new
 *       Java implementation. {@code 0} = 100% legacy (Phase 0 default).
 *       Wired in PR #11.</li>
 *   <li>{@code igw.edge.canary.new-uri} — base URL of the new Java
 *       implementation. Empty in Phase 0.</li>
 *   <li>{@code igw.edge.route-id} — id of the primary route (the legacy
 *       echo-server / PHP upstream).</li>
 *   <li>{@code igw.edge.connect-timeout} / {@code read-timeout} — HTTP
 *       timeouts for upstream calls. Default 1s / 5s.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "igw.edge")
public class IgwEdgeProperties {

    private String upstream = "http://localhost:8081";
    private String routeId = "echo-server";
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Canary canary = new Canary();

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Canary getCanary() {
        return canary;
    }

    public void setCanary(Canary canary) {
        this.canary = canary;
    }

    public static class Canary {
        /** 0..100 weight for the new implementation. 0 = 100% legacy. */
        private int weightNew = 0;
        /** Base URL of the new implementation. Empty in Phase 0. */
        private String newUri = "";

        public int getWeightNew() {
            return weightNew;
        }

        public void setWeightNew(int weightNew) {
            this.weightNew = weightNew;
        }

        public String getNewUri() {
            return newUri;
        }

        public void setNewUri(String newUri) {
            this.newUri = newUri;
        }
    }
}
