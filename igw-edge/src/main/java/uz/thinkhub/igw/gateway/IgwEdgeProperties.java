package uz.thinkhub.igw.gateway;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.edge")
public class IgwEdgeProperties {

    private String upstream = "http://localhost:8081";
    private String routeId = "echo-server";
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Canary canary = new Canary();
    private Security security = new Security();
    private RateLimit rateLimit = new RateLimit();

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

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Canary {
        private int weightNew = 0;
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

    public static class Security {
        private IpCheck ipCheck = new IpCheck();

        public IpCheck getIpCheck() {
            return ipCheck;
        }

        public void setIpCheck(IpCheck ipCheck) {
            this.ipCheck = ipCheck;
        }
    }

    public static class IpCheck {
        /**
         * Phase 0 default: false. The cache is empty, so enabling the
         * check would always 403. Enable once the cache is populated
         * (typically by the user-profile sync job in a follow-up).
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class RateLimit {
        /** Refill rate, tokens per second. Default 100. */
        private int rps = 100;
        /** Maximum burst size. Default 200. */
        private int burst = 200;

        public int getRps() {
            return rps;
        }

        public void setRps(int rps) {
            this.rps = rps;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }
    }
}
