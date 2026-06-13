package uz.thinkhub.igw.starter.audit;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Replaces the value of any field whose name matches {@link IgwAuditProperties}'s
 * {@code maskingFields} list with the literal {@code "***"}.
 *
 * <p>Implemented as a regex over the JSON body string. Robust enough for the
 * default masking list ({@code pan}, {@code card}, {@code cardNumber},
 * {@code pan16}, {@code cvv}, {@code passport}, {@code pin}, {@code phone},
 * {@code email}); a more sophisticated implementation can use a Jackson
 * {@code JsonSerializer} or a streaming masker.
 *
 * <p>Architecture rule: raw card numbers must never reach the database.
 * This class enforces that for the audit {@code request} table.
 */
@Component
public class PiiMaskingConverter {

    static final String MASK = "\"***\"";

    private final IgwAuditProperties properties;

    public PiiMaskingConverter(IgwAuditProperties properties) {
        this.properties = properties;
    }

    /**
     * Mask all configured PII field values in {@code body} with
     * {@value #MASK}. Returns {@code body} unchanged if {@code body} is
     * null or empty.
     */
    public String maskJsonBody(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        String result = body;
        for (String fieldName : properties.getMaskingFields()) {
            // Match "fieldName": "value" (allowing optional whitespace) and
            // replace just the value with "***". The field name itself is
            // preserved so the audit row still tells you WHICH field was
            // redacted, just not its content.
            String pattern = "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*)\"[^\"]*\"";
            result = result.replaceAll(pattern, "$1" + MASK);
        }
        return result;
    }
}
