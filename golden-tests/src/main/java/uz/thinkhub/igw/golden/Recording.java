package uz.thinkhub.igw.golden;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A recorded request/response pair loaded from JSON on disk.
 *
 * <p>Wire format (JSON):
 * <pre>
 * {
 *   "id": "01",
 *   "request": { "method": "GET", "path": "/test", "headers": {...}, "body": null },
 *   "response": { "status": 200, "headers": {...}, "body": "..." }
 * }
 * </pre>
 *
 * <p>The body is a String (UTF-8). For binary recordings, the body is base64-encoded
 * (not used in Phase 0).
 */
public record Recording(
        @JsonProperty("id") String id,
        @JsonProperty("request") RecordedRequest request,
        @JsonProperty("response") RecordedResponse response
) {
    public record RecordedRequest(
            @JsonProperty("method") String method,
            @JsonProperty("path") String path,
            @JsonProperty("headers") java.util.Map<String, String> headers,
            @JsonProperty("body") String body
    ) {}

    public record RecordedResponse(
            @JsonProperty("status") int status,
            @JsonProperty("headers") java.util.Map<String, String> headers,
            @JsonProperty("body") String body
    ) {}
}
