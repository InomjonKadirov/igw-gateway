package uz.thinkhub.igw.golden;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of a single replay: the actual response from the target, the
 * expected response from the recording, and a list of {@link Diff} entries
 * when they don't match byte-for-byte.
 */
public final class ParityReport {

    private final Recording recording;
    private final Recording.RecordedResponse actual;
    private final List<Diff> diffs;

    public ParityReport(Recording recording, Recording.RecordedResponse actual, List<Diff> diffs) {
        this.recording = Objects.requireNonNull(recording);
        this.actual = actual;
        this.diffs = new ArrayList<>(diffs);
    }

    public boolean isClean() {
        return diffs.isEmpty();
    }

    public Recording getRecording() {
        return recording;
    }

    public Recording.RecordedResponse getActual() {
        return actual;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    /**
     * One observed mismatch between the expected and the actual response.
     * Either status-code, header-name/-value, or body-bytes.
     */
    public record Diff(String field, String expected, String actual) {

        @Override
        public String toString() {
            return "Diff{" + field + ": expected=" + quote(expected) + " actual=" + quote(actual) + "}";
        }

        private static String quote(String s) {
            if (s == null) return "<null>";
            if (s.length() > 80) return "\"" + s.substring(0, 80) + "...\" (len=" + s.length() + ")";
            return "\"" + s + "\"";
        }
    }
}
