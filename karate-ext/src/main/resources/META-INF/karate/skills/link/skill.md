# Karate Link — Skill

> **What it is:** you have tests that run and requirements in some form, and **nothing joining them**. This
> skill produces a real traceability matrix and a readiness verdict **without writing a single test**.
> **How it works:** you propose which existing scenario claims which acceptance criterion; `File.tag` writes
> the claim onto the feature file; the graph grades it. No verb matches scenarios to criteria — that
> proposal is your work, and it is the only part that is.
> **Prerequisites:** `Skill.help('requirements')` (the `@req=` form),
> `Skill.help('coverage')` (the status ladder, the dry run).

## What this skill is for

Turning an untagged estate into the RTM. Nothing is rewritten, nothing is migrated, no test is added — the
join is the deliverable.

## Mental model

```
   requirements/*.md                 existing .feature files
     criterion ORD-001/2               Scenario: rejects an expired card
            │                                    │
            └────────── your proposal ───────────┘
                          │  File.tag(path, scenario, {add: ['@req=ORD-001/2']})
                          ▼
     Runner.suite(path, {dryRun: true})   the CLAIM lands    NOTCOVERED → NOTRUN
     Runner.suite(path)                   the EVIDENCE lands NOTRUN → COVERED | FAILING
                          │
                          ▼
     Requirement.delta({from, to})        did the join improve?  →  repeat, or stop
```

## The loop

```js
Work.use('<project>')                                 // 1. anchor on the project
File.list('**/*.feature')                             // 2. the estate — it decides <scope>
Runner.suite(<scope>, { dryRun: true })               // 3. the BASELINE pair — keep both runIds:
Runner.suite(<scope>)                                 //    a delta only ever compares like with like
Requirement.list()                                    // 4. every criterion + its exact @req= stamp token
File.read('<feature>')                                // 5. read the scenarios and their assertions
File.tag('<feature>', '<scenario>', { add: ['@req=ORD-001/2'] })   // 6. write the claim
Runner.suite(<scope>, { dryRun: true })               // 7. the claims land — no SUT, no traffic
Requirement.delta({ from: {run:'<prev dry>'}, to: {run:'<this dry>'} })   // 8. measures: 'claims'
Runner.suite(<scope>)                                 // 9. the real run
Requirement.delta({ from: {run:'<prev run>'}, to: {run:'<this run>'} })   // 10. measures: 'evidence'
Requirement.readiness()                               // 11. once improved is false
```

**Repeat 5–10 one pass at a time.** Each delta's `from` is the **previous pass's** run of the same kind, so
`improved: false` means that pass added nothing and the loop is done. Keep the baseline pair from step 3 —
the closing report is `Requirement.matrix({run: '<baseline>'})` against `Requirement.matrix({run: '<final>'})`.

`<scope>` is the estate being linked, never a fixed folder: pick it from step 2's paths and read the
features. `Runner.suite` takes one path **or a list of paths** (`Runner.suite(['orders.feature','checks/'])`).
Both sides of a delta must run the identical scope — a different scope is a different graph, not a change.

Step 4 gives the token — copy `criteria[].stamp` verbatim, never compose a tag by hand.

Step 6 needs no prior `File.read` — it reads, splices and writes the file in one call. It targets one
scenario by its **exact name or its line number**; an ambiguous name is refused naming
every line it matched. `add` / `remove` are exact tag tokens (a value inside a comma-joined token is not
addressable — remove the token, then add the one you want). An already-present add comes back as
`unchanged`, an absent remove as `absent`, a tag the feature already carries as `inherited` — read those:
they mean your proposal did not land. **A tag invalidates the path's read record**, so `File.write` on that
file is refused until a fresh `File.read`.

Steps 8 and 10 take **two explicit run scopes** — `{run: '<runId>'}` (from `Runner.suite(...).runId`) or
`{reportDir: '<dir>'}`. There is no default and no "previous run"; only you know which two runs are
comparable. Both sides must be the same kind — both dry or both executed — or the call is refused.

## What a `@req=` tag claims

A `@req=ID/cid` tag says **this scenario's assertions check that criterion's behaviour** — the scenario
would fail if the criterion were violated. A scenario that only reaches the endpoint the criterion lives
behind, without checking what it does, is *reach*, not a claim: it gets `@cov=openapi:<operationId>`, never
`@req=` — read the id from `Openapi.operations('<spec>').operations[].operationId`. One criterion per tag token; a scenario that genuinely checks several carries several tokens
(`@req=ORD-001/1 @req=ORD-001/2`). A bare `@req=ORD-001` ticks off **no** criterion — use it only for a
requirement with no acceptance block.

## Reading the delta

| Key | Reads |
|---|---|
| `measures` | `'claims'` (dry vs dry) or `'evidence'` (executed vs executed) |
| `moved[]` | `{id, from, to}` on the status ladder |
| `declared[]` / `undeclared[]` | a claim on `to` and not on `from`, and its mirror (always `regressed`) — a claim is progress only under `measures: 'claims'` |
| `corroborated` | `{oracleOnly[], refusalOnly[]}` — a qualifier cleared **while the criterion still holds evidence a test ran**: something independent now checks what the rulebook or a refusal was vouching for |
| `mockOnlyChanged[]` | `{id, from, to}` — descriptive, never progress: a cleared `mockOnly` says nothing about what answered |
| `lost[]` | evidence gone since `from` — a verdict, or the outside test behind a cleared qualifier; never a clearance |
| `regressed[]` | any move to FAILING, any lost claim or evidence |
| `added[]` / `removed[]` | nodes present on one side only |
| `counts` / `net` | per-side totals and the difference — read under the project's oracle projection (`oracle: 'project'`), which `Requirement.matrix({run})` does not carry, so the two disagree on `covered` |
| `improved` | something moved up the ladder **or was corroborated**, and nothing regressed |
| `unchanged` | `true` — the two graphs are identical; that pass changed nothing |

`improved` counts corroboration because linking's principal success often moves no status: an estate whose
criteria are already COVERED by their rulebook reads COVERED before and after, and what your tags changed
is that `oracleOnly` cleared.

Both graphs are read under the **current** requirement files (`requirementsAsOf: 'current'`), so a delta
across a requirements edit is not a measurement of your tags.

## Standing rules

1. **Never `{feature: true}` unless every scenario in the file exercises the criterion.** It tags the
   feature's own tag lines and every scenario inherits — the fan-out placement, never the default.
2. **NOTRUN means nobody ran it.** Run the suite; do not re-tag.
3. **FAILING means inspect the failure.** Remove or move a tag **only** when the mapping is demonstrably
   wrong. A correctly tagged test that fails has found a defect — report it and **keep the claim**.
4. **Requirements are not edited in this loop.** Authoring, splitting and rewording criteria is
   `Skill.help('requirements')` — do it before or after, never between two deltas.
5. **Stop when the delta stops improving**, and report **both** matrices — baseline and final — then
   `Requirement.readiness()`.
6. **Say what linking measured.** It grades *whether* a test claims a criterion, not *how well* it checks
   it: a green test that asserts nothing reads COVERED with `notasserted`, which a `match` locking a value
   or a shape clears. Report "covered", disclose the rest, never imply "verified".

## Anti-patterns

1. **Feature-level tags to move the numbers** — one scenario in the file that doesn't exercise the
   criterion makes every inherited claim false.
2. **Tagging reach as a claim** — `@req=` on a scenario that calls the endpoint and asserts a status code.
   That is `@cov=`.
3. **Writing a matcher script** — name similarity is not intent. Propose one scenario at a time, from its
   assertions.
4. **One delta over a mixed pair** — a dry side against an executed side is refused, and a delta spanning a
   requirements edit measures the edit.

## Cross-skill links

- `Skill.help('coverage')` — the status ladder, `oracleOnly`/`mockOnly`, the dry run.
- `Skill.help('requirements')` — authoring criteria, `lint`, `readiness`.
- `Skill.help('review')` — is this scenario's assertion strong enough to claim anything?
