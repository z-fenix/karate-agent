# Suite Structure for Coverage-Friendly Karate Tests

> **When to consult this:** authoring a new suite, or refactoring an existing one that produces noisy / sparse coverage. Cross-link from `Skill.help('api')` (authoring) and `Skill.help('coverage')` (where the structure surfaces, via `Coverage.*`/`Requirement.*`).
> **The thesis:** **a well-organized karate suite is its own requirements model.** Folder → feature → scenario IS the tree. Tags are cross-cutting metadata. With these conventions the coverage graph builds clean zero-config; without them coverage data is still produced but reads as noise.

## The five layers

```
folder layout            ─►  domain rollup ("api / payments / underwriting / ui / ...")
feature file             ─►  capability / surface ("Users API", "Login SSO")
feature description      ─►  requirement statement / scope (1-3 lines under Feature:)
scenario name            ─►  test-case identity (becomes the cross-run slug)
scenario description     ─►  requirement statement / acceptance criterion
tags                     ─►  slicing (team, release, smoke/regression, REQ-link, id-override)
```

Each layer maps to one element of the rendered tree. Get the layers right and the tree renders cleanly without any tooling.

## Folder layout — the structural rollup

**Group by domain, not by test type.** The folder closest to the root should answer "what part of the product is this?", not "what kind of test is this?". Karate-agent's tree renders the folder path verbatim; deeply-nested folders compress into the breadcrumb naturally.

```
✅ Good                              ❌ Avoid
src/test/resources/                  src/test/resources/
├── api/                             ├── smoke/
│   ├── users/                       │   ├── users.feature
│   │   ├── users.feature            │   └── orders.feature
│   │   └── auth/                    ├── regression/
│   │       └── tokens.feature       │   ├── users-pagination.feature
│   └── orders/                      │   └── ...
│       └── orders.feature           └── ui/                          ← only one product slice mixed in
├── ui/
│   └── login/
│       └── login.feature
└── kafka/
    └── events.feature
```

**Why:** coverage rolls up by folder. With "smoke / regression / nightly" at the top, every view fragments into the same three buckets. With "api / ui / kafka / payments / underwriting" the rollup mirrors the product map — engineers and PMs both read it.

Test-type slicing belongs on **tags**, not folders.

## Feature files — capability per file

One feature = one **product surface** or one **logical capability**:

```feature
@api @area=users
Feature: Users API
Create, read, update, and delete user accounts.
Covers the public REST surface; admin-only endpoints live in admin-users.feature.
```

Rules:

1. **One concept per file.** "Users API" is a file; "Users API + Orders API" in one file isn't.
2. **A description on every feature.** 1–3 lines under `Feature:`. This is what the coverage graph's feature node renders as the requirement statement. Empty description ≠ empty meaning — it just means the LLM can't help triage gaps.
3. **Feature-level tags should describe the surface, not the test.** `@api`, `@area=users`, `@team=platform` — yes. `@smoke`, `@regression` — those live on individual scenarios.

## Scenarios — the test-case identity

The **scenario name** is the cross-run identity (slug). Treat it like a public API:

```feature
✅ Good — stable, behavioural, unique within feature
Scenario: create user returns 201 with id
Scenario: list users paginates at 50 per page by default
Scenario: delete user requires admin privileges

❌ Avoid — fragile or ambiguous
Scenario: test 1
Scenario: it works
Scenario: should pass
Scenario: scenario        ← unnamed; slug falls back to ::L<line> (line-dependent)
```

Why each matters:

| Pattern | Slug impact |
|---|---|
| Renaming `Scenario: foo` → `Scenario: bar` | Different slug; cross-run history breaks. Acceptable for major behaviour shifts, awful for typo fixes. |
| Reordering scenarios in a feature | No slug change (slugs come from name, not position). Safe. |
| Adding a new scenario between existing ones | No slug change for existing scenarios. Safe. |
| Two scenarios with the same name in one feature | Both share a slug → listener appends `::L<line>` to disambiguate. **Don't rely on this; rename for clarity.** |
| Unnamed scenarios (`Scenario:` then steps) | Slug = `<feature>::L<line>`. Renumbering on edits = history breaks. **Always name your scenarios.** |

### External-system linkage (Xray / Jira / TestRail / homegrown TCM)

When a scenario is also tracked elsewhere, add a tag that names the system explicitly:

```feature
@xrayKey=POL-204 @req=PROD-USER-CREATE
Scenario: create user returns 201 with id
```

The tag flows through as a normal coverage tag without becoming the cross-run slug. The slug stays the scenario's stable identity (`__id` override, else `<feature-path>:<scenario-name>`), which is stable under reordering and overridable under rename via `* def __id`.

**Avoid the generic `@id=KEY` form.** Earlier drafts of this skill suggested it as a way to override the slug; it's been removed because `@id` is a generic tag many teams already use for their own purposes, and silently co-opting it would have coupled their tag conventions to our internal data model.

## Scenario descriptions — the requirement statement

The text between the scenario name and the first `Given` / `When` step is the scenario's description. The coverage graph surfaces it as the requirement statement:

```feature
Scenario: list users paginates at 50 per page by default
GET /users without a 'limit' query parameter returns the first 50 records
ordered by created_at DESC. Subsequent pages use the 'next' link in the
response envelope.
  Given url baseUrl + '/users'
  When method get
  Then status 200
  ...
```

**Rules:**

1. **One scenario, one acceptance criterion.** If the description has more than ~3 sentences, the scenario is probably testing multiple things.
2. **State the behaviour, not the implementation.** "returns 201 with id" — yes. "calls UserRepository.save" — no.
3. **Empty description = degraded coverage view.** The cell still renders; the requirement statement just isn't there to brief a triaging LLM.

## Tags — cross-cutting metadata

Tags are the slice dimension. Common patterns:

| Tag family | Examples | Purpose |
|---|---|---|
| External link | `@req=PROD-USER-CREATE`, `@xrayKey=ABC-42`, `@testRail=qm-1031` | Bind to external TCM / requirement system. The tag flows through as a coverage tag; it does **not** override the cross-run slug. |
| Selection | `@smoke`, `@regression`, `@nightly` | What CI lane runs this. |
| Audience | `@team:payments`, `@team:underwriting`, `@owner=alice` | Who owns / triages. |
| Surface | `@api`, `@grpc`, `@ui`, `@kafka` | Which channel / transport. |
| Behaviour | `@happy-path`, `@error-path`, `@boundary`, `@security` | Test character. |
| Lifecycle | `@deprecated`, `@flaky`, `@skip-on-mac` | Operational state. |
| Opt-out | `@noreq` | Excludes from the requirements view (helpers, fixtures). |

### Tag discipline rules

1. **Use `key=value` form for any tag that carries a payload.** `@req=POL-204` round-trips faithfully through the coverage atom and the connector layer; `@req POL-204` (space-separated) does not. `@req=` can appear multiple times on one scenario (a scenario can cover several requirements).
3. **Don't combinatorially explode.** `@smoke @regression @nightly @release-2026q3 @release-2026q4` on one scenario is a smell — usually the team is using tags as filing cabinets. Prefer 2–3 *purposeful* tags over 6+ stale ones.
4. **Tag at the right level.** If every scenario in a feature has `@api`, put it on the feature instead — `getTagsEffective()` merges feature + scenario tags before emission.
5. **`@noreq` is the explicit opt-out.** Background scenarios, fixture-only scenarios, scenarios that exist purely to set up state — tag them `@noreq` so they don't dilute the requirements count.

### Anti-patterns

| Anti-pattern | What to do instead |
|---|---|
| `@WIP`, `@TODO`, `@DRAFT` long-lived in mainline | Either delete the scenario or convert to a proper `@skip`/`@noreq` opt-out with a comment. |
| Tag stuffing (`@user @users @userAPI @user-api`) | Pick one canonical form and stick to it. |
| Folder + tag duplication (folder `payments/`, every scenario also `@payments`) | Drop the redundant tag. Folder is already the rollup. |
| `@smoke` as a per-team flavour (`@smoke-payments`, `@smoke-billing`) | Use a separate axis: `@smoke` for selection + `@team:payments` for ownership. |

## Scenario Outlines — make Examples coverage-friendly

```feature
Scenario Outline: lookup <key> resolves to <value>
A static dictionary mapping; one row per known key.
  Given path '/lookup/<key>'
  When method get
  Then status 200
  And match response == { value: '<value>' }

  Examples:
    | key   | value     |
    | alpha | 1         |
    | beta  | 2         |
    | gamma | 3         |
```

For each example row, the slug is `<outline-slug>:<exampleIndex>`. The coverage graph shows the outline as a parent node + N children:

```
└── outline   lookup <key> resolves to <value>     (3 examples)
    ├── passed  lookup alpha resolves to 1
    ├── passed  lookup beta resolves to 2
    └── failed  lookup gamma resolves to 3
```

Rules:

1. **Don't reorder Examples rows.** The example index is part of the slug; reorder → per-example history breaks. (a future version will hash example data; until then, treat the table order as load-bearing.)
2. **Don't use Examples for unrelated test cases.** If the rows aren't variations of the same scenario, split into separate scenarios.
3. **Dynamic outlines** (Examples driven by a JS expression) trade per-example history stability for data-driven flexibility. Use them when the data lives elsewhere (DB, API); accept that adding a row at the source shifts indices.

## Anti-patterns at the suite level

1. **Anti-pattern: "test-pyramid" folders** (`unit/`, `integration/`, `e2e/`). Karate tests are end-to-end by definition; the folder taxonomy should be domain-driven, not test-pyramid-driven.
2. **Anti-pattern: one giant feature file** containing every scenario for a service. Split per surface / capability — the coverage tab becomes useless when all 200 scenarios live under one feature node.
3. **Anti-pattern: copy-paste scenarios.** If two scenarios share 90% of their steps, factor the common setup into a `Background:` block. Coverage atoms emit per-scenario; deduplicated step trees don't change that, but they do make the suite readable.
4. **Anti-pattern: hidden requirements in `karate-config.js`.** Configuration belongs there; the requirements live in `.feature` files. If a scenario's behaviour can only be understood by reading the config, you've moved the requirement out of the spec.

## A 60-second self-check before merging a feature file

1. Does the **folder path** describe the product slice (not the test type)?
2. Does the **feature description** explain what surface this file covers (1–3 lines)?
3. Does **every scenario name** describe behaviour, not just "test N"?
4. Does **every scenario description** state an acceptance criterion?
5. Are the **tags purposeful** (no stuffing, no stale `@WIP`)?
6. Does any scenario tracked externally carry a **system-specific tag** (`@xrayKey=`, `@jiraIssue=`, etc.) — never the generic `@id=`?
7. Did you mark **helpers / fixtures** with `@noreq`?

A pass on all seven → the coverage report will read like the spec without any further work.

## Cross-reference

- `Skill.help('coverage')` — the engine + data shape.
- `Skill.help('api')` — REST authoring conventions; this topic is its structural counterpart.
- `Skill.help('review')` — review checklists for suite structure (use this as the LLM-driven gate).
