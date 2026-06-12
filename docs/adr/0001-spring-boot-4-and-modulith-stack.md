# 0001. Adopt Spring Boot 4.1 + Spring Cloud 2025.1.2 + Spring Modulith 2.x as the IGW stack

- Status: Accepted
- Date: 2026-06-13
- Context: Phase 0 (Foundation) of the IGW banking integration gateway

## Context

IGW replaces a PHP/Yii2 monolith that fronts ~25 external providers (core banking IABS, card networks HUMO/SVGATE, payment providers, government APIs). The system is almost stateless — business state lives in external systems; locally only audit (PostgreSQL), users (Keycloak), and caches (Redis).

The architecture doc (`docs/dev-prompts/igw-claude-code-prompts.md` §2) mandates:

- JDK 25 + Spring Boot 4.x
- Spring Cloud Gateway MVC (the server-webmvc variant; no WebFlux in our code)
- Spring Modulith 2.x — module boundaries enforced by tests
- Apache HttpClient for provider HTTP (per-provider `maxPerRoute`)
- JUnit 5, Testcontainers, WireMock, ArchUnit
- Maven Central for the BOM; no other repos

The user has already pinned the existing scaffold to:

- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- Java 25 (Gradle toolchain)
- Gradle 9.5.1 (Kotlin DSL)

## Decision

Adopt the pinned stack as the IGW Phase 0 baseline:

| Concern | Pinned version |
| --- | --- |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.2 |
| Spring Modulith | 2.1.0 |
| Java | 25 (Gradle toolchain) |
| Gradle | 9.5.1 |
| Testcontainers | 1.20.4 |
| WireMock | 3.10.0 |
| ArchUnit | 1.3.0 |
| JUnit 5 | 5.11.3 |
| AssertJ | 3.26.3 |
| Mockito | 5.14.0 |
| Resilience4j | 2.2.0 |
| Bucket4j | 8.10.1 |
| Nimbus JOSE | 9.40 |
| Apache HttpClient 5 | 5.4 |

Spring Boot manages the actual versions of all `spring-*` libraries. Testcontainers and WireMock are pinned explicitly in `gradle/libs.versions.toml`.

## Consequences

- Spring Cloud 2025.1.2 is the new release train aligned with Spring Boot 4.x. We will track the train in lockstep — minor version bumps happen together.
- Spring Modulith 2.1.0 has a documented compatibility matrix with Spring Boot; we confirm the matrix in PR #1 by running `./gradlew build` (it includes a `modulith-verify` test scaffolded but trivially passing in Phase 0).
- Java 25 introduces virtual-thread-friendly APIs we will use in our filter code (e.g. `Thread.ofVirtual()` in `CorrelationIdFilter`). No `synchronized` blocks on hot paths.
- ArchUnit is added to every subproject's test classpath via `java-conventions` so architecture rules can be enforced incrementally.

## Notes / follow-ups

- Modulith 2.1.x + Spring Boot 4.1.0: confirmed in PR #1 build. If incompatible, pin Modulith to whatever matches and add a TODO.
- The `@ResponseBodyAdvice` envelope advice uses the Spring Web servlet stack; we will confirm in PR #2 that no WebFlux types leak into our code.
