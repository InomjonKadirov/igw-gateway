package uz.thinkhub.igw.golden;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Replays a {@link Recording} against a target URI and produces a
 * {@link ParityReport} comparing the actual response to the expected one.
 *
 * <p>Comparison rules (Phase 0, intentionally simple):
 * <ul>
 *   <li>Status codes must match exactly.</li>
 *   <li>Every expected header (name, value) must be present in the actual
 *       response. Extra headers in the actual response are allowed
 *       (e.g. {@code Date} added by the server).</li>
 *   <li>Bodies must be byte-equal (UTF-8 decoded as Strings).</li>
 * </ul>
 *
 * <p>Not thread-safe; each thread should hold its own harness.
 */
public class GoldenHarness {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public Recording loadRecording(java.nio.file.Path path) throws IOException {
        try (var in = java.nio.file.Files.newInputStream(path)) {
            return objectMapper.readValue(in, Recording.class);
        }
    }

    public ParityReport replay(Recording recording, URI target) throws IOException, InterruptedException {
        Objects.requireNonNull(recording, "recording");
        Objects.requireNonNull(target, "target");

        // Merge path + recording's path; if the recording path is absolute
        // (starts with /), use it as-is; otherwise join with target.
        String path = recording.request().path();
        URI requestUri = path.startsWith("/")
                ? target.resolve(path)
                : target;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(requestUri)
                .timeout(Duration.ofSeconds(5))
                .method(
                        recording.request().method() == null ? "GET" : recording.request().method(),
                        recording.request().body() == null
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofString(recording.request().body(), StandardCharsets.UTF_8));
        if (recording.request().headers() != null) {
            recording.request().headers().forEach((k, v) -> {
                if (v != null) builder.header(k, v);
            });
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Recording.RecordedResponse actual = new Recording.RecordedResponse(
                response.statusCode(),
                toMap(response.headers().map()),
                response.body());

        return compare(recording, actual);
    }

    private static Map<String, String> toMap(Map<String, java.util.List<String>> headers) {
        // HttpResponse#headers().map() lower-cases header names per the JDK spec,
        // so we key the result map by lower-case too. Callers that need the
        // original casing (e.g. the "header[Content-Type]" diff label) should
        // look it up via a separate case-preserved map.
        Map<String, String> out = new HashMap<>();
        headers.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                out.put(k.toLowerCase(java.util.Locale.ROOT), v.get(0));
            }
        });
        return out;
    }

    static ParityReport compare(Recording recording, Recording.RecordedResponse actual) {
        Recording.RecordedResponse expected = recording.response();
        List<ParityReport.Diff> diffs = new ArrayList<>();

        if (actual.status() != expected.status()) {
            diffs.add(new ParityReport.Diff("status",
                    String.valueOf(expected.status()),
                    String.valueOf(actual.status())));
        }
        if (expected.headers() != null) {
            for (Map.Entry<String, String> e : expected.headers().entrySet()) {
                String expectedValue = e.getValue();
                // HttpResponse.headers().map() lower-cases keys, so look up
                // case-insensitively. Preserve the recording's original case
                // in the diff label for human-readability.
                String actualValue = actual.headers() == null
                        ? null
                        : actual.headers().get(e.getKey().toLowerCase(java.util.Locale.ROOT));
                if (actualValue == null || !actualValue.equals(expectedValue)) {
                    diffs.add(new ParityReport.Diff("header[" + e.getKey() + "]",
                            expectedValue, actualValue));
                }
            }
        }
        String expectedBody = expected.body() == null ? "" : expected.body();
        String actualBody = actual.body() == null ? "" : actual.body();
        if (!expectedBody.equals(actualBody)) {
            diffs.add(new ParityReport.Diff("body", expectedBody, actualBody));
        }
        return new ParityReport(recording, actual, diffs);
    }
}
