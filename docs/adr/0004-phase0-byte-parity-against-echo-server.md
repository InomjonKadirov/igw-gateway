# 0004. Phase 0 byte-parity is verified against `local/echo-server`, not real legacy PHP

- Status: Accepted
- Date: 2026-06-13
- Context: Phase 0 (Foundation) of the IGW banking integration gateway

## Context

The Phase 0 success criteria (`docs/dev-prompts/igw-claude-code-prompts.md` §3) include:

- `igw-edge` passes a load smoke test: 1000 RPS transparent proxy, p99 added latency < 5ms, zero response byte diffs vs direct legacy access
- The golden harness demonstrates: identical replay → green; injected 1-byte envelope change → red with a readable diff

These criteria require a "real legacy PHP" target. None is available in the repo:

- No `legacy/console/service/v1/{PROVIDER}/**` directory.
- No `contracts/` (recovered OpenAPI specs).
- No `golden-tests/recordings/` (recorded traffic).
- No `local/dc.yml` standing up PHP behind a load balancer.

The architecture doc's own gotcha (`docs/dev-prompts/igw-claude-code-prompts.md` §2) says:

> "If you are uncertain about legacy PHP behavior (envelope edge cases, error formats), list your assumptions explicitly in the PR description instead of guessing silently."

Byte parity against real PHP is a Phase 1+ concern (per-adapter). In Phase 0, the goal is to build the *infrastructure* (starters, edge, golden harness, smoke harness) such that byte parity can be measured once recordings and contracts exist.

## Decision

For Phase 0, byte parity is verified against a **fake PHP echo server**, not real legacy PHP.

`local/echo-server/` is a small Spring Boot service that:

- Listens on `localhost:8081` (configurable).
- Accepts any method and path.
- Returns a JSON body that mirrors the request: `{ "method": ..., "path": ..., "headers": { ... }, "body": ... }`.
- Sets status 200 and `Content-Type: application/json`.

We chose to echo the **parsed JSON** of the request (not the raw bytes) so JSON content negotiation works against the envelope advice.

Phase 0 byte-parity smoke is structured as:

1. `curl -i http://localhost:8081/echo` (direct echo-server access) → record the bytes as `baseline.bin`.
2. `curl -i http://localhost:8080/echo -H '...'` (through `igw-edge` with the echo-server as upstream) → record the bytes as `proxied.bin`.
3. `diff baseline.bin proxied.bin` → must be empty (modulo headers we don't care about, e.g. `Date`, `X-Correlation-Id`).

Phase 0 golden-tests:

- One hand-crafted recording at `golden-tests/src/main/resources/recordings/_smoke/01-untouched-passthrough.json`.
- Replay against `local/echo-server` → green.
- Manually mutate 1 byte in the recording → red with a readable diff.

The k6 smoke (`local/k6/smoke.js`) runs 1000 RPS for 30s through `igw-edge` against the echo-server and asserts `p99 < 5ms`.

## Assumptions explicitly logged

| Assumption | Why we made it | Where it bites |
| --- | --- | --- |
| Legacy envelope shape: `{success, status, message, path, result}` | The only shape documented | Envelope advice must match byte-for-byte; if real legacy uses different field order or names, we'll need to revisit |
| Legacy error body: provider bytes passed through unchanged | The architecture doc mentions "per-provider error mapping" but doesn't define the byte format | Error-mapping-starter ships no-op default; per-provider translators come in Phase 1+ |
| Audit `request` schema (id, correlation_id, user_id, method, path, status, latency_ms, masked_body, created_at) | Invented; not in the doc | When real audit table schema is known, we'll add a Flyway/Liquibase migration in Phase 1+ |
| Echo-server echoes **parsed** JSON, not raw bytes | So JSON content negotiation works | If the real legacy requires raw byte echo, we'll need a different test fixture |
| p99 < 5ms on the local echo-server (artificially easy) | The real legacy is much slower; the threshold is a sanity floor | `local/k6/README.md` notes that the real-legacy threshold will be re-baselined in Phase 1+ |
| No real Keycloak in Phase 0 | A Keycloak stand would inflate Phase 0 | JWT validation tests use a mock JWKS server; a real Keycloak in `local/dc.yml` is Phase 1+ |
| `Accept-Language` locales: `en, ru, uzl, uzc` | The only locales in the doc | If real legacy serves a wider set, we add to `igw.i18n.supported-locales` |

## Consequences

- Phase 0 can complete without real legacy. We get the *infrastructure* in place and proven.
- Phase 1+ per-adapter PRs (template in `docs/dev-prompts/igw-claude-code-prompts.md` §4) will replace the echo-server with real providers and replace the hand-crafted recording with real recorded traffic.
- The byte-parity assertion is *mechanism-validated*, not contract-validated. We prove that the proxy can be byte-transparent; we don't yet prove the envelope is correct.

## Notes / follow-ups

- `local/echo-server/` should be tagged as a dev-only artifact (in a future `local/` cleanup, or just by convention).
- When `contracts/` is populated in Phase 1+, the golden harness will use the OpenAPI schemas to validate response bodies structurally, not just by byte diff.
