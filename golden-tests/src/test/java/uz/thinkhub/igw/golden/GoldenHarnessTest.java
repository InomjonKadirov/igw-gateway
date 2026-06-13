package uz.thinkhub.igw.golden;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoldenHarnessTest {

    private WireMockServer wm;
    private GoldenHarness harness;
    private URI target;

    @BeforeAll
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        target = URI.create("http://localhost:" + wm.port());
        harness = new GoldenHarness();
    }

    @AfterAll
    void tearDown() {
        wm.stop();
    }

    @Test
    void replayMatchesByteForByte() throws Exception {
        // The recording is the "control" (expected) response.
        Recording recording = harness.loadRecording(
                Path.of("src/main/resources/recordings/_smoke/01.json"));

        // The mock returns exactly what the recording expects.
        wm.stubFor(get(urlEqualTo("/test/path"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(recording.response().body())));

        ParityReport report = harness.replay(recording, target);

        assertThat(report.isClean())
                .as("Expected no diffs but got: %s", report.getDiffs())
                .isTrue();
    }

    @Test
    void replayDetectsOneByteBodyMutation() throws Exception {
        Recording recording = harness.loadRecording(
                Path.of("src/main/resources/recordings/_smoke/01.json"));

        // Mutate the upstream: drop the trailing '}' so the body length differs.
        String mutatedBody = recording.response().body().substring(0,
                recording.response().body().length() - 1);
        wm.stubFor(get(urlEqualTo("/test/path"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mutatedBody)));

        ParityReport report = harness.replay(recording, target);

        assertThat(report.isClean()).isFalse();
        assertThat(report.getDiffs())
                .extracting(ParityReport.Diff::field)
                .contains("body");
    }

    @Test
    void replayDetectsStatusCodeMismatch() throws Exception {
        Recording recording = harness.loadRecording(
                Path.of("src/main/resources/recordings/_smoke/01.json"));

        wm.stubFor(get(urlEqualTo("/test/path"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(recording.response().body())));

        ParityReport report = harness.replay(recording, target);

        assertThat(report.isClean()).isFalse();
        assertThat(report.getDiffs())
                .extracting(ParityReport.Diff::field)
                .contains("status");
    }

    @Test
    void replayForwardsAcceptLanguageHeader() throws Exception {
        // Reuse the smoke recording; vary Accept-Language per call to verify the
        // gateway (in a real run) would route through the i18n-starter.
        Recording recording = harness.loadRecording(
                Path.of("src/main/resources/recordings/_smoke/01.json"));

        // WireMock echoes the Accept-Language it received so we can assert
        // the harness forwards the header correctly.
        for (String lang : new String[]{"en", "ru", "uzl", "uzc"}) {
            wm.stubFor(get(urlEqualTo("/test/path"))
                    .withHeader("Accept-Language", new com.github.tomakehurst.wiremock.matching.EqualToPattern(lang))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(recording.response().body())));

            // Build a per-language copy of the recording.
            Recording perLang = new Recording(
                    recording.id(),
                    new Recording.RecordedRequest(
                            recording.request().method(),
                            recording.request().path(),
                            new java.util.HashMap<>(recording.request().headers()) {{
                                put("Accept-Language", lang);
                            }},
                            recording.request().body()),
                    recording.response());

            ParityReport report = harness.replay(perLang, target);
            assertThat(report.isClean())
                    .as("Locale %s should match", lang)
                    .isTrue();
        }
    }
}
