# k6 smoke harness

Stress probe for `igw-edge` against `local/echo-server`. Two scripts, one
Gradle task.

## What it proves

- `igw-edge` is up, can route to a Spring Boot upstream, and survives 30s of
  sustained 20 000 RPS without erroring at the HTTP layer
  (`http_req_failed: rate<0.01`).
- `igw-edge` is **byte-transparent**: a `GET /echo` issued directly to the
  echo server returns the same JSON document as the same request routed
  through the proxy (modulo the echoed request headers — see below).

## What it does **not** prove

- The 5 ms p99 added-latency target from `docs/dev-prompts/.../§3`. 20 000
  RPS on a single host with a Spring Cloud Gateway MVC in front of a
  Tomcat echo server is a **saturation probe**, not a regression gate. The
  k6 `http_req_duration` threshold is deliberately loose (`p(99)<1000`) so
  the run is informative on saturation behaviour, not a pass/fail signal.
  The real latency gate will be re-baselined in Phase 1+ when we run
  against real upstream providers (per ADR-0004).
- Per-endpoint matrix coverage (GET + POST + locale fan-out). That's a
  follow-up PR.

## Files

```
local/k6/
├── smoke.js        # k6 script (20000 RPS, 30s, single GET /echo)
├── byte-diff.sh    # snapshot direct vs proxied body and diff
└── README.md       # this file
```

Reports land in `local/k6/reports/` (gitignored):
- `summary.json` — k6's structured summary (thresholds, trend stats)
- `baseline.bin` / `proxied.bin` — raw response bodies from the byte-diff

## Running

### One-shot via Gradle (recommended)

```bash
./gradlew k6Smoke
```

This task:
1. Verifies `k6` is on PATH (`brew install k6`); fails fast with a clear
   hint if not.
2. Starts `:igw-edge:bootRun` and `:local:echo-server:bootRun` as
   background javaexecs on ports 8080 and 8081.
3. Polls both endpoints until they respond 200.
4. Runs `k6 run local/k6/smoke.js` (configurable via `TARGET_URL`,
   `DURATION`, `RPS` env vars).
5. Runs `local/k6/byte-diff.sh` — fails the build on any body diff.
6. Tears down the two bootRuns on exit (success or failure).

Skip the k6 step (useful for CI without k6 installed) but keep the
byte-diff:

```bash
./gradlew k6Smoke -Pk6.skip=true
```

### Standalone (you brought up the stack yourself)

```bash
k6 run local/k6/smoke.js
ECHO_URL=http://localhost:8081/echo EDGE_URL=http://localhost:8080/echo \
  local/k6/byte-diff.sh
```

### Docker compose service

```bash
docker compose -f local/dc.yml up k6
```

The compose service runs `k6 run /scripts/smoke.js` against the same stack
once the other services are healthy.

## Tuning

| env var       | default                     | meaning                          |
|---------------|-----------------------------|----------------------------------|
| `TARGET_URL`  | `http://localhost:8080/echo`| Where igw-edge serves the echo.  |
| `DURATION`    | `30s`                       | k6 scenario duration.            |
| `RPS`         | `20000`                     | Constant arrival rate.           |
| `ECHO_URL`    | `http://localhost:8081/echo`| Used by byte-diff.sh (direct).   |
| `EDGE_URL`    | `http://localhost:8080/echo`| Used by byte-diff.sh (proxied).  |

Lower `RPS` to 1000 if you want a less destructive probe that has any
chance of meeting the 5 ms p99 floor on a single host.

## Byte-parity caveat: echoed request headers

The echo controller returns a JSON document that *includes the request
headers it received*. Those will differ between the two calls in
byte-diff.sh because:

- The direct call's `User-Agent` is `curl/...`, the proxied call's is
  `curl/...` plus whatever `Via` / `X-Forwarded-*` headers the gateway
  adds.
- Spring Cloud Gateway adds its own set of headers (`X-Correlation-Id`,
  sometimes `X-Forwarded-Host`, `X-Forwarded-Proto`).

To keep the diff focused on the **proxy's effect on the payload shape**
rather than on header echo fidelity, byte-diff.sh strips the `.headers`
field from both bodies before comparing. The remaining document
(`method`, `path`, `query`, `body`) is what the bank side actually
consumes; if those are stable, the proxy is transparent.

When we swap the echo server for real legacy PHP in Phase 1+, the
recordings won't echo request headers and the diff will be a true body
equality.
