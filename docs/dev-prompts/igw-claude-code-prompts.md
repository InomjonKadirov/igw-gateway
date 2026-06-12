# IGW Development with Claude Code (Fable 5) — Prompt Package

Состав: (1) настройка сессии, (2) `CLAUDE.md` в корень репозитория, (3) kickoff-промпт этапа 0, (4) повторяемый промпт-шаблон переноса адаптера — конвейер для 512 endpoints. Построено по рекомендациям Anthropic: контекст и мотивация вместо приказов, XML-теги для структуры, явные критерии успеха, plan mode перед кодом, TDD как acceptance-фильтр.

---

## 1. Session setup

```bash
claude --model claude-fable-5
```

- Начинать каждую крупную задачу в **Plan Mode** (Shift+Tab) — Fable 5 строит план изменений до правок; ревьюишь план, потом исполнение.
- `/clear` между несвязанными задачами — деградация качества при заполненном контексте реальна; один адаптер = одна сессия.
- Для массового переноса (этап 2+) — headless-режим `claude -p "<prompt>"` в CI поверх шаблона из §4.

---

## 2. `CLAUDE.md` (положить в корень монорепо IGW)

```markdown
# IGW — Integration Gateway

Banking integration gateway replacing a PHP/Yii2 monolith via strangler-fig migration.
Routes client REST calls to ~25 external providers (core banking IABS, card networks
HUMO/SVGATE, payment providers, government APIs). Almost stateless: business state
lives in external systems; locally only audit (PostgreSQL), users (Keycloak), caches (Redis).

## Stack
- JDK 25, Spring Boot 4.x, Spring Cloud Gateway MVC (edge), Gradle (Kotlin DSL)
- Spring Modulith 2.x — module boundaries are ENFORCED by tests; never add
  cross-module imports
- Resilience4j (CircuitBreaker + SemaphoreBulkhead per provider), Bucket4j (rate limit)
- Apache CXF for SOAP (HUMO only), openapi-generator for REST clients
- Testing: JUnit 5, Testcontainers, WireMock, ArchUnit

## Repository layout
- igw-edge/          — Spring Cloud Gateway MVC: JWT, per-user IP check, rate limit,
                       correlation-id, canary routing PHP<->Java. THIN: no business
                       logic, no request-body buffering. Never add either.
- igw-hub/           — modular monolith; one Gradle module per provider
                       (paynet, insurance, katm-myid, gov-tax, payment-providers, scoring-erp)
- igw-core-banking/  — IABS adapter; modules per subdomain (references, accounts,
                       customers, deposits, transactions, currency, loans)
- igw-card/          — HUMO + SVGATE + gateway-card orchestration (PCI scope)
- igw-starters/      — shared starters: envelope, audit-masking, error-mapping,
                       token-cache, i18n, resilience-defaults
- contracts/         — recovered OpenAPI specs (source of truth per endpoint)
- golden-tests/      — record/replay parity harness against legacy PHP responses

## Architecture rules (violations = PR rejected)
1. Module = hexagonal: port interfaces named by intent (SendPayment), never by vendor.
   DTO mapping and error translation live ONLY inside the adapter (anti-corruption layer).
2. Response envelope {success, status, message, path, result} and per-provider error
   mapping must be BYTE-COMPATIBLE with legacy PHP. Never "improve" a contract;
   improvements go to a versioned v2 only.
3. Money operations: NEVER auto-retry a timed-out POST. Pattern: status-check-then-retry
   via the provider's check/status method. Reads may retry with backoff+jitter.
4. Timeouts on the HTTP client (connect/read), NOT Resilience4j TimeLimiter
   (it cannot interrupt synchronous calls on virtual threads).
5. Audit: every gateway call writes to the `request` table via audit-starter with
   PII/PAN field masking. Raw card numbers must never reach the database.
6. Localization: IABS/gateway-card endpoints honor Accept-Language (en, ru, uzl, uzc) —
   pass-through to providers; golden tests run per locale.
7. Config is typed @ConfigurationProperties, validated at startup. Secrets only from Vault.

## Commands
- ./gradlew build                 — full build + ArchUnit + Modulith verify
- ./gradlew :igw-hub:test         — single service tests
- ./gradlew goldenTest -Pmodule=X — parity run for module X against recorded PHP traffic
- docker compose -f local/dc.yml up — local stand: Postgres, Redis, Keycloak, WireMock

## Definition of Done for any endpoint
green golden test (byte parity incl. error cases and locales) + negative tests
(timeout, 5xx, connection reset via Toxiproxy) + metrics tagged module=<name> +
masked audit verified + runbook section updated.

## Gotchas
- Legacy PHP `publishToQueue()` is synchronous despite the name; there is no queue.
- Some legacy OpenAPI descriptions have broken encoding — recover contracts from
  forms/actions code, not from openapi.json.
- Providers HUMO/SVGATE/IABS encode "card blocked" differently ("3" / BLOCKED / C_BLK);
  canonical enums live in each module's domain model.
- Apache HttpClient default pool is 5 connections per route — always configure
  maxPerRoute per provider in the resilience starter.
```

---

## 3. Kickoff prompt — этап 0 (Foundation)

```text
You are the lead engineer implementing Phase 0 of the IGW banking integration
gateway. Read CLAUDE.md first — it defines the architecture rules.

<context>
We are replacing a PHP/Yii2 gateway (95 controllers, 512 endpoints, ~25 external
providers) using strangler-fig: igw-edge will sit in front of legacy PHP from day
one, transparently proxying 100% of traffic, then switch routes to Java services
gradually with weighted canary. Contract fidelity is the #1 project risk: partner
banks depend on byte-exact response envelopes and error bodies. The golden-test
harness (record/replay against production traffic) is our main safety mechanism —
it must exist before any endpoint is migrated.
</context>

<task>
Build the Phase 0 foundation in this order:
1. igw-starters: envelope-starter (ResponseBodyAdvice producing the legacy
   {success,status,message,path,result} envelope), audit-starter (writes request
   table rows with PII/PAN masking; fail-closed if DB unavailable),
   error-mapping-starter (per-provider error translator SPI),
   resilience-starter (named CircuitBreaker + SemaphoreBulkhead per provider,
   HTTP-client timeouts, maxPerRoute pool config), token-cache-starter (Redis),
   i18n-starter (Accept-Language pass-through for en/ru/uzl/uzc).
2. igw-edge: JWT validation against Keycloak JWKS (cached), per-user IP check
   from user.verification_ip, Bucket4j rate limit keyed by user identity,
   correlation-id filter, weighted canary routing with config-refresh rollback.
   Transparent-proxy mode: 100% to legacy upstream, zero response mutation.
3. golden-tests harness: replay recorded request/response pairs against any
   target (PHP or Java), assert byte parity on body and status, diff report
   on mismatch, locale matrix support.
</task>

<constraints>
- JDK 25 virtual threads everywhere; no WebFlux, no reactive types in our code.
- igw-edge must not buffer request bodies and must add < 5ms p99 overhead.
- Every starter gets its own integration test using Testcontainers.
- Follow CLAUDE.md architecture rules strictly; if a rule seems wrong for a
  specific case, stop and ask rather than deviating silently.
</constraints>

<workflow>
Work in plan mode first: present the module layout, key classes, and test
strategy for my approval before writing code. Then implement one starter at a
time: write the failing test first, implement, run ./gradlew build, commit with
a conventional-commit message. Do not start the next starter until the previous
one is green. If you are uncertain about legacy PHP behavior (envelope edge
cases, error formats), list your assumptions explicitly in the PR description
instead of guessing silently.
</workflow>

<success_criteria>
- ./gradlew build green including ArchUnit and Spring Modulith verification
- igw-edge passes a load smoke test: 1000 RPS transparent proxy, p99 added
  latency < 5ms, zero response byte diffs vs direct legacy access
- golden harness demonstrates: identical replay → green; injected 1-byte
  envelope change → red with a readable diff
</success_criteria>
```

---

## 4. Повторяемый промпт-шаблон: перенос одного адаптера

Подставляются 4 переменные: `{PROVIDER}`, `{MODULE}`, `{ENDPOINTS}`, `{SPEC_PATH}`. Это конвейер этапов 1–4 и рычага У2.

```text
Migrate the {PROVIDER} adapter from legacy PHP to {MODULE}. Read CLAUDE.md first.

<inputs>
- OpenAPI spec: {SPEC_PATH} (source of truth; recovered from PHP forms)
- Endpoints to migrate: {ENDPOINTS}
- Legacy PHP reference: legacy/console/service/v1/{PROVIDER}/** — use it to
  replicate error mapping and token handling exactly, not as code to translate
  literally
- Recorded traffic for golden tests: golden-tests/recordings/{PROVIDER}/
</inputs>

<workflow>
1. Scaffold the module from the cookiecutter template (do not hand-create files).
2. Generate the HTTP client from the spec (openapi-generator, restclient library);
   generated code is not committed — wire the gradle task.
3. Write the port interface (named by intent) and the ACL mapper: provider DTOs
   and error bodies → canonical module model. Replicate legacy error pass-through
   byte-exactly: when PHP forwarded the provider's error body as-is, we forward
   it as-is.
4. Tests before logic: WireMock stubs from recorded traffic, golden parity tests
   (all locales if the provider is localized), negative tests via Toxiproxy
   (timeout mid-POST, connection reset, 5xx, slow response 30s).
5. For money-moving endpoints implement status-check-then-retry; prove it with a
   test that kills the connection mid-POST and asserts no duplicate submission.
6. Run ./gradlew :{MODULE}:build and goldenTest -Pmodule={MODULE}. Iterate until
   green. Commit per endpoint group, conventional commits.
</workflow>

<constraints>
- Do not modify shared starters or other modules; if a starter lacks something,
  stop and describe the gap instead of working around it locally.
- Do not invent error messages or "fix" legacy typos in contract strings —
  byte parity wins over aesthetics until PHP is decommissioned.
- Resilience config per CLAUDE.md defaults; justify any per-provider override
  in the PR description with a number (provider's measured latency profile).
</constraints>

<success_criteria>
All golden tests green; negative tests green; Modulith/ArchUnit verification
green; module metrics visible with tag module={MODULE}; DoD checklist in the PR
description with every item checked or explicitly N/A with a reason.
</success_criteria>
```

---

## 5. Почему промпты построены так (рекомендации Anthropic)

| Приём | Где применён |
|---|---|
| Контекст и мотивация, не только приказы — модель обобщает правило, понимая «зачем» | `<context>` объясняет strangler-fig и почему byte-parity = риск №1 |
| XML-теги для разделения блоков | `<context>/<task>/<constraints>/<workflow>/<success_criteria>` |
| Явные критерии успеха, проверяемые автоматически | success_criteria = команды и измеримые пороги |
| Plan mode / «explore → plan → code → commit» | `<workflow>` требует план до кода |
| TDD как acceptance-фильтр агентского кода | тесты пишутся до логики; golden = приёмка |
| «Stop and ask» вместо тихих допущений | constraints в обоих промптах |
| CLAUDE.md как постоянная память + gotchas | §2, включая ловушки легаси (publishToQueue, кодировки) |
| Контекст-гигиена: /clear, одна задача = одна сессия, headless для конвейера | §1 |
| Цели и ограничения вместо пошаговой инструкции (не «railroad») | workflow задаёт порядок и инварианты, но не диктует структуру классов |

Sources: [Claude Code best practices](https://code.claude.com/docs/en/best-practices), [Prompting best practices](https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/claude-prompting-best-practices), [Effective context engineering for AI agents](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents), [How Anthropic teams use Claude Code](https://www-cdn.anthropic.com/58284b19e702b49db9302d5b6f135ad8871e7658.pdf)
