# Karate Requirements — Skill

> **Status:** Core traceability + the deterministic governance core (`lint`, `readiness`) are **built**; the tier-3 semantic **review** is *this skill's* job (it is judgment — never a method that calls a model).
> **Stack:** `Requirement.*` JS namespace over `/api/eval` + MCP (`io.karatelabs.ext.requirements`). Markdown is the source of truth; it lowers to normalized `karate-requirements/v1` and joins the unified coverage graph via `@req=` tags.
> **Prerequisites:** `Skill.help('coverage')` (the graph the `req` source joins), `Skill.help('assertions')`.

## What this skill is for

Author business requirements **in git as near-standard markdown**, join them to tests + coverage, and answer the questions no ALM answers: **what's covered, what's failing, what's missing, and is it safe to ship?** — deterministically where possible, with LLM judgment only for the genuinely-semantic residue.

This skill teaches an LLM to:

- **author** requirements in the locked markdown form (heading `ID: Title`, one `@key=value` tag line, EARS acceptance) — and optionally the `@real=` edge to the implementing artifacts — see `Skill.help('requirements', 'authoring')`;
- **read** the traceability matrix + gaps over the `Requirement.*` JSAPI;
- **govern** with the deterministic tools (`lint`, `readiness`) and **then** apply the tier-3 semantic **review** the tools deliberately leave to you — see `Skill.help('requirements', 'semantic-review')`;
- **compose** these into a "tidy these requirements" loop.

## Mental model

```
   requirements/*.md            normalized              unified graph
   (the SoT, in git)            karate-requirements/v1   (one join)
   ─────────────────────        ────────────────────     ───────────────────────
   ## ORD-001: Place order  ──► id/type/title/parent ──► req CoverageItem
   @criticality=high            status/priority          ▲
   The system shall …           criticality              │ @req=ORD-001 (DECLARED edge)
   **Acceptance:**              acceptance[]             scenario test node
   - WHEN … THE SYSTEM SHALL …  constraints[]            (pass/fail → derived status)
```

The **authored** status (`approved`/`draft`) is kept distinct from the **derived** coverage status (`COVERED`/`FAILING`/`NOTRUN`/`NOTCOVERED`). Coverage is **declared** by the `@req=` tag — it lives in the feature text, so "which requirements have a test at all?" is answerable **without executing anything**: `Runner.suite(path, {dryRun:true})` (CLI: `karate run -D`) parses the suite, lands the declared edges and builds the graph with no step run. A requirement whose test exists but has not been run reads **NOTRUN**; one nothing claims reads **NOTCOVERED**. See `Skill.help('coverage')` §"Coverage without executing".

> **⚠️ Tag at the *criterion* level for any requirement that has acceptance criteria.** A decomposed requirement's status is the **AND-rollup of its acceptance criteria**: it goes COVERED only when each criterion is covered. So tag the scenario that verifies a criterion with **`@req=ID/N`** (e.g. `@req=ORD-001/1`), one per criterion the scenario covers (a scenario may carry several). A **bare `@req=ORD-001` does *not* tick off any individual criterion** — so a requirement with untested criteria stays **NOTCOVERED even when a passing, visibly-linked test points at it** (the single most common authoring trap). Use a bare `@req=ID` *only* for a requirement that has no `**Acceptance:**` block.

## The JSAPI surface (every method is deterministic, one-way)

Call these as a roleplaying LLM does, over `/api/eval`:

| Call | Returns |
|---|---|
| `Requirement.matrix()` | every requirement × derived coverage status + covering tests |
| `Requirement.gaps()` | requirements with no passing test + untested acceptance criteria + hollow `@req=` links, **plus the `@real=` trust report**: `incidental` (credited via `@real=` with no `@req=` anchor), `drift` (declared but no real artifact exercised), `orphans` (artifacts exercised that realise no requirement) |
| `Requirement.get('ORD-001')` | one requirement — gist + authored + derived status + tests |
| `Requirement.load([path])` | (re)materialize the normalized `karate-requirements/v1` doc from the markdown SoT |
| `Requirement.lint()` | **structural + lexical** findings (no LLM, no test run) — the air-gapped "requirements health, no AI" report |
| `Requirement.readiness()` | authored `criticality` × derived coverage → per-leaf **risk** → the **confidence-to-ship** verdict + blockers |
| `Requirement.posture()` | authored `criticality` → governance **posture** (`shared`/`attest`/`airgap`, via `cov.criticalityPolicy`) + the **grounded / incidental / hollow** eval-independence score |
| `Requirement.write(name, md[, {ifMatch}])` | **author** (create or overwrite) `requirements/<name>.md` from `karate-requirements/v1` markdown — parse-validated (≥1 requirement), `{ifMatch}` staleness guard → `{path, count, ids}`. The one write verb (still deterministic, no model): it lets an LLM author the requirement doc over HTTP instead of only by git edit. |

Every method takes an optional `{output:'markdown'|'both'}` — `'both'` adds a rendered scorecard alongside the data. Example:

```js
Requirement.lint({ output: 'both' }).markdown        // the health scorecard
Requirement.readiness().blockers                     // the HIGH-risk requirements gating release
```

## Deterministic tools vs. your judgment

The split is the whole point — it is what makes the report trustworthy:

- **`lint()` and `readiness()` never call a model.** They are pure functions over git files, golden-testable, CI-gateable, free. A binary check can't be gamed by its author — that is the primary reward-hacking defense.
- **Semantic review is *yours*.** "Is this acceptance line *actually* convertible to a binary eval, or just grammatically-EARS prose?" is judgment. You do it — guided by `Skill.help('requirements', 'semantic-review')` — and emit findings in the *same shape* `lint()` does, so they compose.

Never invent a `Requirement.review()` that calls an LLM. Review is a skill (this one); lint is a tool.

## Strengthening declared coverage with `@real=` (optional, high-leverage)

Requirement coverage is **author-declared** via `@req=`, so under-declaration is invisible: a test that really exercises ORD-001 but omits the tag leaves it looking NOTCOVERED. The **`@real=` edge** closes that gap. It is a **requirement-side** tag (authored in the markdown, *not* on scenarios — it sits beside `@criticality`) naming the **real artifact(s)** that realise the requirement — an OpenAPI op, gRPC method, or a future code symbol:

```markdown
### ORD-001: Place a valid order
@criticality=high @real=openapi:placeOrder,openapi:reserveInventory
```

Read `@real=` as **"realized by"**; the terse, code-like tag signals a concrete machine-checkable link, distinct from the free-form requirement prose. It is **singular and one-to-many** — repeat the tag or comma-separate (space-free) for several artifacts; the value lands as a JSON array (`real: [...]`). Targets are **namespaced cross-source ids** (`openapi:placeOrder`, `grpc:Orders/Place`, `java:com.foo.Order`). For scale, author the whole requirement→artifact matrix centrally in `karate-boot.js` via **`cov.real`** (a function, or a data map that drives it — same data-or-behavior pattern as `cov.criticalityPolicy`).

Once `@real=` edges exist, an **observed** hit on a real artifact (real traffic — the monetization wedge) strengthens the picture, graded by **provenance** (`Requirement.matrix()` / `gaps()` / `posture()`):

| Signal | Meaning | Your action |
|---|---|---|
| **grounded** | `@req=`-declared **and** the real artifact was observed | nothing — the good case |
| **incidental** | a real artifact observed with **no `@req=` anchor** → promoted out of NOTCOVERED (`coveredVia: real`) **but flagged** | add a `@req=<id>` test to anchor intent (grade *what was wanted*, not just *what the code does*) |
| **drift** | `@req=`-declared but its real artifacts went **unexercised** | the declared test isn't actually hitting the realising op — fix the test or the `@real=` mapping |
| **orphan** | an artifact exercised that realises **no** requirement | author a requirement (or `@real=`) for it, or accept it as out-of-scope |

This is **optional enrichment** — projects that author no `@real=` edges see none of this (no orphan noise). But guiding an LLM to author `@real=` opens up rich **impact-analysis and traceability** reporting (blast-radius "which requirements does op X realise", honest-but-untagged coverage made *visible and credited*). Everything is deterministic — no model decides provenance.

## The "tidy these requirements" loop

The deterministic tool and your judgment compose at the harness layer:

```
1. Requirement.lint()              ── free, deterministic: fix every error + warning first
2. apply `Skill.help('requirements', 'semantic-review')` ── judge the residue lint can't (binary-eval, smuggled-how, …)
3. Requirement.gaps()              ── act on the @real= trust report: tag incidental reqs with @req=,
                                      fix drift, triage orphans (the self-check loop)
4. edit the markdown SoT           ── propose concrete diffs; the human commits (git is the memory)
5. Requirement.lint() again        ── confirm green; repeat until clean
6. Requirement.readiness()         ── is it safe to ship? which requirements block?
```

Lint-green is wirable as a harness `ExitCondition`, so this loop can run autonomously and stop when the requirements are clean.

## Quick start

```
Lint @requirements/ with the Karate requirements skill, fix what you can, then tell me if we're safe to ship.
```

The LLM runs `Requirement.lint()`, fixes structural/lexical findings, applies the semantic-review checklist, re-lints, then runs `Requirement.readiness()` and reports the verdict + blockers.

**Tests exist, requirements exist, nothing joins them?** That is `Skill.help('link')` — propose the `@req=`
claim per scenario, write it with `File.tag`, grade it with `Requirement.delta`. No new tests.

## Topics

- `Skill.help('requirements', 'authoring')` — the locked markdown conventions (`ID:` headings, the `@tag` line, EARS acceptance, `**Constraints:**`).
- `Skill.help('requirements', 'semantic-review')` — the tier-3 review the deterministic linter can't do (binary-eval convertibility, smuggled-how, the two-implementations test).
