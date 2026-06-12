# 0003. Use a `build-logic` included build to share build logic across subprojects

- Status: Accepted
- Date: 2026-06-13
- Context: Phase 0 (Foundation) of the IGW banking integration gateway

## Context

Phase 0 ships 9 Gradle subprojects (1 service + 6 starters + 1 test harness + 1 local helper). Every subproject needs:

- Java 25 toolchain
- JUnit 5 + AssertJ + Mockito
- ArchUnit
- Test logging conventions

Starters additionally need:

- `io.spring.dependency-management` with the Spring Boot BOM
- Autoconfigure + configuration-metadata annotation processors

Apps (`igw-edge`, `local/echo-server`) additionally need:

- `org.springframework.boot` plugin
- Spring Cloud BOM

Without sharing, each `build.gradle.kts` repeats ~30 lines of identical config.

The Gradle docs offer three patterns for sharing build logic:

- `buildSrc/` — implicit included build; works, but its settings are not visible
- `build-logic` included build — explicit `includeBuild("build-logic")` in `pluginManagement`
- Precompiled script plugins inside `buildSrc/` or `build-logic/conventions/`

The 4th pattern (per-subproject duplication) was rejected in grilling.

## Decision

Adopt the **`build-logic` included build** pattern, Kotlin DSL, with four precompiled convention plugins:

| Plugin | What it sets |
| --- | --- |
| `java-conventions` | Java 25 toolchain, JUnit 5, AssertJ, Mockito, ArchUnit, `useJUnitPlatform()` |
| `spring-boot-library-conventions` | `java` + `io.spring.dependency-management`, Spring Boot BOM, autoconfigure + configuration-metadata processors, `compileOnly` Spring Boot autoconfigure |
| `spring-boot-app-conventions` | `java` + `org.springframework.boot` + `io.spring.dependency-management`, Spring Boot BOM, Spring Cloud BOM, runtime starter bundle |
| `test-conventions` | `io.spring.dependency-management`, Testcontainers BOM, Testcontainers + WireMock test deps |

Subprojects apply the relevant plugin(s):

- The 6 starters: `id("spring-boot-library-conventions")`
- `igw-edge`, `local/echo-server`: `id("spring-boot-app-conventions")` (+ `id("test-conventions")` for tests)
- `golden-tests`: `id("java-conventions")` + `id("test-conventions")`

Version catalog: `gradle/libs.versions.toml` is the auto-imported `libs` extension. Convention plugins access it as a regular `libs` reference (Gradle 8.5+ auto-imports catalogs into all build scripts, included builds, and convention plugins).

`pluginManagement` in root `settings.gradle.kts` wires the included build:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories { gradlePluginPortal(); mavenCentral() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}
```

## Consequences

- Each subproject's `build.gradle.kts` is 5–15 lines instead of 30–50.
- A change to a convention (e.g. bumping the Java toolchain) is one edit in `build-logic/`, not nine.
- Convention plugins are versioned with the repo, not separately published.
- `RepositoriesMode.FAIL_ON_PROJECT_REPOS` prevents subprojects from declaring their own repositories — a long-term guard against repo drift.

## Notes / follow-ups

- The `test-conventions` plugin applies `io.spring.dependency-management` for the Testcontainers BOM. If we ever combine it with `spring-boot-library-conventions` on the same subproject, the BOM contributions may collide. We handle that by giving `test-conventions` its own `dependencyManagement` block and accepting the override (the Testcontainers BOM doesn't overlap with Spring Boot deps).
- If a convention plugin needs to be shared with a sibling repo, we can promote `build-logic` to a standalone published artifact. Not needed for Phase 0.
