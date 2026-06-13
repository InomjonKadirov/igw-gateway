package uz.thinkhub.igw.starter.audit;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igw.audit")
public class IgwAuditProperties {

    /**
     * If {@code true}, a failure to write the audit row in {@code preHandle}
     * causes the gateway to reject the request with HTTP 503. This is the
     * compliance-safe default.
     * <p>If {@code false}, the row write is best-effort: failures are logged
     * but the request proceeds. Set this to {@code false} only in dev/test
     * environments where DB availability is unreliable.
     * Default: {@code true}.
     */
    private boolean failClosed = true;

    /**
     * Field names whose values must be replaced with {@code "***"} in the
     * {@code request.masked_body} column. Matches are case-insensitive against
     * the JSON key.
     * Default: card-related and contact-info fields.
     */
    private List<String> maskingFields = List.of(
            "pan",
            "card",
            "cardNumber",
            "pan16",
            "cvv",
            "passport",
            "pin",
            "phone",
            "email"
    );

    public boolean isFailClosed() {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed) {
        this.failClosed = failClosed;
    }

    public List<String> getMaskingFields() {
        return maskingFields;
    }

    public void setMaskingFields(List<String> maskingFields) {
        this.maskingFields = maskingFields;
    }
}
