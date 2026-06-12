# Domain docs

This repo uses a **single-context** layout. Domain language and architectural decisions live in two places:

- **`CONTEXT.md`** (repo root) — the project's domain language: key terms, the bounded contexts in play, the "ubiquitous language" that the codebase and skills should share.
- **`docs/adr/`** (repo root) — Architecture Decision Records. One file per decision, named `NNNN-short-slug.md`.

## Consumer rules for skills

Skills that need to learn the project's domain follow these rules in order:

1. **Read `CONTEXT.md` first.** Treat its terms as authoritative. If code or comments disagree with `CONTEXT.md`, `CONTEXT.md` wins until the doc is updated.
2. **Read recent ADRs** in `docs/adr/` (sorted by number, descending). ADRs explain *why* a past decision was made; do not re-litigate them in new work — extend or supersede with a new ADR.
3. **If `CONTEXT.md` is missing**, fall back to `QWEN.md`, then `README.md`, then `HELP.md`. The skill should warn the user that the fallback is in use and recommend creating `CONTEXT.md`.

## When to update

- Update `CONTEXT.md` whenever a new term is introduced that other modules will need to share.
- Add a new ADR to `docs/adr/` whenever a non-obvious architectural choice is made (e.g. choosing WebMVC over WebFlux, picking a specific routing strategy, deciding on a service discovery approach). Use the format:
  ```
  docs/adr/NNNN-short-slug.md
  ```
  where `NNNN` is a zero-padded sequence number.

## Out of scope

This layout assumes one logical context per repo. If this repo later grows into a monorepo with multiple bounded contexts (e.g. `gateway/` and `admin-ui/`), migrate to a multi-context layout: a `CONTEXT-MAP.md` at the root pointing to per-context `CONTEXT.md` files, and per-context `docs/adr/` subdirectories.
