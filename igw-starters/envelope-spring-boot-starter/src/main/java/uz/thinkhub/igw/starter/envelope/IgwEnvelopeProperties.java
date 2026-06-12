package uz.thinkhub.igw.starter.envelope;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.envelope")
public class IgwEnvelopeProperties {

    /**
     * Whether to wrap controller responses in the legacy envelope.
     * Default: true.
     */
    private boolean wrapSuccess = true;

    /**
     * Request header to use for the envelope's "path" field when present.
     * If absent, the request URI path is used.
     * Default: X-Request-Path.
     */
    private String pathHeaderName = "X-Request-Path";

    public boolean isWrapSuccess() {
        return wrapSuccess;
    }

    public void setWrapSuccess(boolean wrapSuccess) {
        this.wrapSuccess = wrapSuccess;
    }

    public String getPathHeaderName() {
        return pathHeaderName;
    }

    public void setPathHeaderName(String pathHeaderName) {
        this.pathHeaderName = pathHeaderName;
    }
}
