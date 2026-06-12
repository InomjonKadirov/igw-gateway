# 0002. Six combined-form Spring Boot starters under `igw-starters/`, one per capability

- Status: Accepted
- Date: 2026-06-13
- Context: Phase 0 (Foundation) of the IGW banking integration gateway

## Context

The Phase 0 kickoff prompt (`docs/dev-prompts/igw-claude-code-prompts.md` §3) calls for six Spring Boot starters:

1. `envelope-starter` — produces the legacy `{success, status, message, path, result}` envelope
2. `audit-starter` — writes the `request` table row with PII/PAN masking; fail-closed
3. `error-mapping-starter` — per-provider error translator SPI
4. `resilience-starter` — per-provider `maxPerRoute` HTTP client, CircuitBreaker, SemaphoreBulkhead
5. `token-cache-starter` — Redis cache-aside for OAuth tokens
6. `i18n-starter` — `Accept-Language` pass-through resolver

The Spring Boot reference doc offers two canonical layouts for starters:

- **Combined form** — one module per starter, named `*-spring-boot-starter`, with the autoconfigure classes and a `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file.
- **Split form** — two modules per starter, `*-spring-boot` (autoconfigure) and `*-spring-boot-starter` (deps only). Recommended only when a starter has flavors or optional features.

## Decision

Adopt the **combined form**: six subprojects, one per starter:

```
igw-starters/
├── envelope-spring-boot-starter/
├── audit-spring-boot-starter/
├── error-mapping-spring-boot-starter/
├── resilience-spring-boot-starter/
├── token-cache-spring-boot-starter/
└── i18n-spring-boot-starter/
```

Each subproject:

- Is a plain Gradle subproject under `igw-starters/`.
- Applies the `spring-boot-library-conventions` convention plugin.
- Pulls `org.springframework.boot:spring-boot-autoconfigure` as `compileOnly` (so `@AutoConfiguration`, `@ConditionalOnClass`, etc. compile) and the autoconfigure / configuration-metadata processors as `annotationProcessor` — already wired by the convention plugin.
- Has no main class and no `bootJar` task.
- Tests use `ApplicationContextRunner` (or its `WebApplicationContextRunner` / `ReactiveWebApplicationContextRunner` variants where appropriate).

Group ID: `uz.thinkhub.igw`. Artifact IDs: `igw-envelope-spring-boot-starter`, `igw-audit-spring-boot-starter`, etc.

## Consequences

- Six `build.gradle.kts` files instead of twelve; less ceremony for Phase 0.
- We can split a starter later if it grows flavors. The split is mechanical: rename `<name>-spring-boot-starter` to `<name>-spring-boot`, create a new `<name>-spring-boot-starter` that depends on it, and update consumers.
- Each starter is independently testable from PR #2 onwards.
- Consumers (e.g. `igw-edge`) depend on each starter individually (`implementation(project(":igw-starters:envelope-spring-boot-starter"))`). The choice of which starters to pull in is per-app.

## Notes / follow-ups

- If `audit-spring-boot-starter` later needs flavors (e.g. `audit-jdbc` vs `audit-kafka`), split per the Spring Boot doc.
- If `token-cache-spring-boot-starter` later needs flavors (e.g. `token-cache-redis` vs `token-cache-caffeine`), split similarly.
