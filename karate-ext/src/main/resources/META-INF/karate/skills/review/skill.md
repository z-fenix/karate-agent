# Karate Review — Skill

> **Status:** The skill-driven entry point for the review track. A productized `karate review` CLI, a dashboard "Suite Health" page, and a PR-bot are planned follow-ons; the LLM-driven surface ships now, with this skill.
> **Stack:** Operates on existing `.feature` files. No new infrastructure — Claude Code + this skill is enough.
> **Prerequisites:** `Skill.help('assertions')`, `Skill.help('api')` (or whichever skill covers the target feature's surface).

## What this skill is for

An LLM walks an existing Karate feature file (or a directory of them) and produces a **structured review report**: strengths, gaps, anti-patterns, suggested edits — cross-linked to the appropriate skill topics. Optionally applies low-risk fixes in-place.

The skill is the entry surface for the broader review track. The same heuristics (codified in `Skill.help('review', 'feature-file-review')`) will drive the planned CLI, dashboard page, and PR-bot. The skill ships first because it needs zero new infrastructure.

## Mental model

```
   .feature file
        ↓
   walk top-to-bottom against the checklist (7 sections)
        ↓
   for each finding:
     ├─ severity (info / warn / error)
     ├─ topic-link (cross-skill or cross-topic)
     ├─ line number
     └─ suggested fix (concrete diff)
        ↓
   structured report
```

The output is a **report**, not a refactor. The developer chooses what to accept. If asked, the LLM applies the diff afterward.

## Quick start

From Claude Code:

```
Review @path/to/feature.feature using the karate-review skill.
```

The LLM produces:

```markdown
## Review — path/to/feature.feature

**Verdict:** 2 errors, 3 warnings, 1 info. Strong overall — clear Background, good tag hygiene. Two scenarios over-pin generated IDs; one mixes two assertions.

### Strengths
- Clean `Background:` — no per-scenario URL repetition.
- Tags scoped to single concerns (`@smoke`, `@regression`, `@req=POL-001`).
- All scenarios have an `@req=…` link or `@noreq` opt-out.

### Findings

| Sev | Line | Finding | Topic |
|---|---|---|---|
| error | 18 | Pins `id: 42` — server-generated. Use `'#uuid'` + rebind. | `Skill.help('assertions', 'fuzzy')` §format markers |
| error | 24 | Two unrelated assertions in one scenario (status code + body content). Split. | rule #4 |
| warn | 12 | `Thread.sleep(2000)` in JS step. Use `retry until`. | checklist #6 (flakiness) |
| warn | 7 | Background fetches token via REST call — slow + flaky. Move to `karate.callSingle`. | `Skill.help('api', 'auth')` |
| warn | 33 | Tag `@SMOKE` should be lowercase per convention. | tag hygiene |
| info | 41 | Consider `match contains` instead of `==` — response has volatile timestamp. | `Skill.help('assertions', 'match')` |

### Suggested edits

(unified diff)

\`\`\`diff
@@ line 18 @@
-  And match response.id == 42
+  And match response.id == '#uuid'
+  * def newId = response.id
\`\`\`
```

## The 7-section checklist

Walk the file top-to-bottom in this order. The checklist itself lives at `Skill.help('review', 'feature-file-review')`.

### 1. Structure

- [ ] Feature description reads as a requirement statement (it becomes a TCM record).
- [ ] `Background:` sets shared state without leaking per-scenario data.
- [ ] Each `Scenario:` covers exactly one outcome.
- [ ] No commented-out code (git remembers).
- [ ] No 50-line literal JSON inlined (move to a separate `.json` file + `read('…')`).

### 2. Tags

- [ ] `@smoke` / `@regression` for filtering (where appropriate).
- [ ] `@req=<id>` for traceability (where the scenario maps to a requirement).
- [ ] `@noreq` for scenarios intentionally outside the requirements view.
- [ ] `@report=false` for secret-handling scenarios.
- [ ] `@parallel=false` for scenarios with order coupling or shared mutable state (rare; usually a smell).
- [ ] Lowercase, kebab-case tag names. `@team:payments`, not `@Team_Payments`.

### 3. Assertions

- [ ] **Not a thin assertion.** A status alone, `match response == '#string'`, a lone presence/type marker, or a
      `contains 'substring'` on a whole body are smoke — they pass against a broken response. Assert the **shape**:
      `match response == { id: '#string', title: 'buy milk', complete: false }` (fuzzy for server-assigned, literal
      for business). Under-asserting is as much a finding as over-pinning — see `Skill.help('review', 'assertion-strength')`.
- [ ] No literal IDs that the server generates. Use `'#uuid'` / `'#number'` + capture-and-rebind.
- [ ] No time-dependent literals. Use `'#datetime'` / regex / fuzzy predicate.
- [ ] One outcome per match where readability is at risk.
- [ ] Schema-style match where the contract matters more than the exact values.
- [ ] `match each` over a possibly-empty array: empty **fails by default** (`matchEachEmptyAllowed`); only set `= true` if empty is valid, and pin a length (`'#[_ >= 1]'`) to assert non-emptiness explicitly.

### 4. Coverage

- [ ] Happy path present.
- [ ] At least one explicit error path (4xx / 5xx for REST; non-OK status for gRPC; failure event for Kafka),
      **asserting the error body contract**, not just the status. Where a coverage source is registered,
      check it instead of eyeballing: `Coverage.gaps()` → `happyOnly` lists every operation never driven
      through an error outcome, with its declared-but-untested codes.
- [ ] Auth-failure path where the endpoint requires auth.
- [ ] Edge cases for boundary inputs (empty array, max length, null optional fields).
- [ ] **Stateful flows are chained, not faked** — for a resource API, at least one scenario does the real
      lifecycle (create → capture the id → read back with `#(id)` → update → delete → 404) in ONE scenario,
      not independent single-shot calls that never prove persistence.
- [ ] **Variations are data-driven** (`Scenario Outline:` + `Examples:`), not copy-pasted near-identical scenarios.

### 5. Reuse

- [ ] No copy-pasted setup that should be `call read('common-setup.feature')`.
- [ ] No per-scenario auth dance that should be `karate.callSingle('auth.feature')`.
- [ ] No magic numbers without a `* def` binding.

### 6. Flakiness

- [ ] No `Thread.sleep` (use `retry until`).
- [ ] No order-dependent state across scenarios.
- [ ] External dependencies acknowledged (mocked, or scenario tagged for environments that have them).
- [ ] No assertion on `responseTime` without a documented baseline.

### 7. Style

- [ ] Scenarios independent (can run alone, can run in parallel).
- [ ] Consistent naming (snake-vs-kebab-vs-camel — pick one).
- [ ] No raw URLs (use `* url baseUrl` + `path`).
- [ ] Background does only Background things (setup + config); no assertions, no business logic.

## When the scenario is correct but the run still fails — re-shelve

If the 7-section checklist passes (the scenario is sound) but the run is red, the fault is downstream — the backend, the spec, or the data doesn't implement what the scenario asserts. Re-shelve the scenario so the dashboard cycles back to green *without throwing away the authoring work*:

```feature
@todo @req=CUST-006
Scenario: update an existing customer's email (placeholder — pending author)
  * karate.abort()
  # Drafted against PUT /customers/{id} — backend pending:
  #   Given path 'customers', 'c-1'
  #   And request { name: 'Alice Singh', email: 'alice.new@example.com' }
  #   When method put
  #   Then status 200
  #   And match response == { id: 'c-1', name: 'Alice Singh', email: 'alice.new@example.com' }
```

Two reflexes:

1. **Restore the `@todo` tag.** Pulls the scenario back into `Coverage.gaps()` as `declared-not-implemented`, where it belongs while the requirement is unmet. `karate.abort()` makes the runner skip cleanly (not fail).
2. **Preserve the draft as a comment block.** The body that *would* satisfy the requirement stays inline so the next author has a head-start. Strip the comments when the backend lands and the scenario goes live.

Use this outcome — don't propose backend code changes — when the review skill is invoked from a Troubleshoot prompt. Backend implementation is a different decision (different stakeholder, different change-control). The review skill's scope is the feature file.

## Topic index

| Topic | When to read |
|---|---|
| `Skill.help('review', 'assertion-strength')` | Over-pinning vs under-asserting; the `contains` vs `==` decision; the thin-assertion anti-pattern. |

## Checklists

- `Skill.help('review', 'feature-file-review')` — the 7-section checklist as a single page.

## Standing rules (for the *reviewing* LLM)

1. **Lead with strengths.** A review that only lists faults trains the author against the skill. Always section 1 = "Strengths."
2. **Suggest, don't impose.** Provide the diff; let the developer accept or reject.
3. **Cross-link to the catalog.** Each finding should point at the canonical topic file the developer can read for depth.
4. **Don't invent requirements.** Review checks for `@req=…` traceability where it should exist; it does not fabricate requirement IDs.
5. **Severity calibration:**
   - **error** = will produce wrong test results or production-bug-masking flake (over-pinning generated ID; `Thread.sleep`).
   - **warn** = brittle or hard-to-maintain but currently correct (long inline JSON; missing 401 coverage).
   - **info** = stylistic; "we usually do it this way."
6. **One review per file.** Don't bundle across files unless explicitly asked. Each file gets its own report.
7. **Quote line numbers, not just "in the second scenario."** The developer (and the LLM applying fixes) needs exact locations.
8. **For ambiguous findings, ask before flagging.** "I'd expect a 401 scenario here — is that intentionally out of scope, or missing?"

## Anti-patterns (for the *reviewing* LLM)

1. **Anti-pattern:** flagging every minor style choice as a finding. Aim for the top ~10 highest-value findings; let the rest go.
2. **Anti-pattern:** offering a fix that introduces a new dependency. If the fix is "use `karate-mock`," check if the project already has it before suggesting.
3. **Anti-pattern:** rewriting the file. The skill produces a *report*. Refactor only on explicit request.
4. **Anti-pattern:** scoring or grading. Reviews surface findings; they don't assign letter grades.
5. **Anti-pattern:** moralizing. "You should always…" → "Use `…` because rule #2 in `Skill.help('api')`."

## Cross-skill links

- `Skill.help('api')` — most review findings cross-reference rules in the api skill.
- `Skill.help('assertions')` — assertion-strength findings link to fuzzy markers and `match` semantics.
- `Skill.help('grpc')` — gRPC-specific anti-patterns (forgot `flush()`, mismatched `count`).
- `Skill.help('kafka')` — Kafka-specific anti-patterns (produce-before-consume.start).
