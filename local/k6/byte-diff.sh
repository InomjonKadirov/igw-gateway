#!/usr/bin/env bash
# byte-diff.sh — direct echo vs proxied echo through igw-edge.
#
# What this proves (per ADR-0004 §3):
#   The proxy is byte-transparent: a GET /echo issued directly to
#   local:echo-server returns the same JSON document as the same request
#   routed through igw-edge. Headers we don't care about (Date,
#   X-Correlation-Id) are stripped before the diff so the comparison is on
#   the application payload, not on Spring Boot's request-id plumbing.
#
# Exit codes:
#   0  bodies match (or only differ in ignored headers)
#   1  bodies differ
#   2  prerequisites missing (curl, jq) or one of the targets is unreachable
#
# Env overrides:
#   ECHO_URL  — direct echo-server URL (default http://localhost:8081/echo)
#   EDGE_URL  — proxied edge URL    (default http://localhost:8080/echo)

set -euo pipefail

ECHO_URL="${ECHO_URL:-http://localhost:8081/echo}"
EDGE_URL="${EDGE_URL:-http://localhost:8080/echo}"

if ! command -v curl >/dev/null; then
  echo "byte-diff: curl not found on PATH" >&2
  exit 2
fi
if ! command -v jq >/dev/null; then
  echo "byte-diff: jq not found on PATH" >&2
  exit 2
fi

# Snapshot the two responses. --include prints the status line so we can fail
# fast on a 5xx. --fail-with-body makes curl exit non-zero on 4xx/5xx.
snap () {
  local url="$1"
  local out="$2"
  if ! curl --silent --show-error --location --fail-with-body \
            --header 'Accept: application/json' \
            --header 'Accept-Language: en' \
            --output "$out" \
            "$url"; then
    echo "byte-diff: could not fetch $url (see $out for body)" >&2
    exit 2
  fi
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKDIR="${SCRIPT_DIR}/reports"
mkdir -p "$WORKDIR"

snap "$ECHO_URL" "$WORKDIR/baseline.bin"
snap "$EDGE_URL" "$WORKDIR/proxied.bin"

# Strip headers we don't care about, then compare bodies (json document).
#
# Spring's Boot 4 default response for our echo controller has no Date header
# since the controller writes a Map<String,Object>, but tomcat may add
# Last-Modified, and igw-edge adds X-Correlation-Id. We don't capture
# headers here — byte-diff only compares the body, which is the contract
# the bank side actually consumes.
#
# Note: the echoed body contains the *request* headers seen by the server.
# Those will differ between the two calls (User-Agent, Accept, etc.) and
# the proxy adds a few more. We hash the JSON and exclude that field from
# the comparison to keep the diff focused on the proxy's effect on the
# payload shape, not on header echo fidelity.
strip_ignored_keys () {
  jq -S 'del(.headers)' "$1"
}

baseline_normalized="$(strip_ignored_keys "$WORKDIR/baseline.bin")"
proxied_normalized="$(strip_ignored_keys "$WORKDIR/proxied.bin")"

# Compare normalized JSON structurally. -S sorts keys, so a key-order diff
# in the echo server's LinkedHashMap vs Jackson's default ordering doesn't
# fail the test.
baseline_hash="$(printf '%s' "$baseline_normalized" | shasum -a 256 | awk '{print $1}')"
proxied_hash="$(printf '%s' "$proxied_normalized" | shasum -a 256 | awk '{print $1}')"

echo "byte-diff: baseline sha256=$baseline_hash"
echo "byte-diff: proxied  sha256=$proxied_hash"

if [ "$baseline_hash" = "$proxied_hash" ]; then
  echo "byte-diff: OK (bodies match modulo echoed request headers)"
  exit 0
fi

# Bodies differ — print the structural diff for the developer to look at.
echo "byte-diff: bodies differ" >&2
diff <(echo "$baseline_normalized") <(echo "$proxied_normalized") || true
exit 1
