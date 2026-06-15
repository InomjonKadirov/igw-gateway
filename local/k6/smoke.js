// k6 smoke for igw-edge Phase 0.
//
// What this proves:
//   - igw-edge is up and proxying to local/echo-server under sustained load.
//   - The proxy does not error out at the HTTP layer (status, connection).
//
// What this does NOT prove:
//   - The 5ms p99 added-latency target from the kickoff prompt. 20000 RPS on a
//     single host with a Spring Cloud Gateway MVC fronting a Tomcat echo
//     server is a *saturation probe*, not a regression gate. The threshold
//     here is deliberately loose (p99<1000ms) so the run is informative on
//     saturation behaviour, not a pass/fail signal. Byte-parity is the real
//     acceptance criterion and lives in byte-diff.sh.
//
// Override the target with TARGET_URL=http://host:port k6 run smoke.js.
//
// Outputs: JSON summary to ../reports/summary.json (relative to the script
// directory).

import http from "k6/http";
import { check, fail } from "k6";
import { Trend, Rate } from "k6/metrics";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.3/index.js";

const TARGET = __ENV.TARGET_URL || "http://localhost:8080/echo";
const SCENARIO_DURATION = __ENV.DURATION || "30s";
const TARGET_RPS = Number(__ENV.RPS || 20000);

// Custom trend so the p99 latency in the summary is unambiguous even when
// http_req_duration is heavily affected by cold-start spikes.
const gatewayLatency = new Trend("gateway_latency_ms", true);
const gatewayErrors = new Rate("gateway_errors");

export const options = {
    scenarios: {
        smoke: {
            executor: "constant-arrival-rate",
            // 20000 iterations per second, each spawning a single VU slot.
            // Pre-allocate VUs so the rate governor isn't throttled by the
            // VU-ramp; the value is an upper bound, k6 will reuse slots.
            rate: TARGET_RPS,
            timeUnit: "1s",
            duration: SCENARIO_DURATION,
            preAllocatedVUs: 200,
            maxVUs: 1000,
        },
    },
    // Loose by design — see header comment. We care that the proxy is up and
    // not returning 5xx, not that a single-host stress test meets the
    // 5ms p99 production target.
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(99)<1000"],
        gateway_errors: ["rate<0.01"],
    },
    summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

export default function () {
    const res = http.get(TARGET, {
        // k6 sets User-Agent by default; let the gateway see a typical browser
        // header so any future per-UA filters have something to look at.
        headers: { Accept: "application/json" },
        tags: { name: "echo_proxy" },
    });

    gatewayLatency.add(res.timings.duration);
    gatewayErrors.add(res.status >= 500);

    const ok = check(res, {
        "status is 200": (r) => r.status === 200,
        "body has method": (r) => {
            try {
                return JSON.parse(r.body).method === "GET";
            } catch (_) {
                return false;
            }
        },
    });

    if (!ok) {
        fail("unexpected response from " + TARGET + " status=" + res.status);
    }
}

export function handleSummary(data) {
    return {
        "../reports/summary.json": JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: "  ", enableColors: true }),
    };
}
