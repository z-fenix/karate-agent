# Karate Coverage — Skill

> **What it is:** test coverage as a **unified traceability graph** (`karate-trace/v1`) — one in-memory
> graph joining requirements, OpenAPI operations, rules, and the tests that exercise them, with a
> derived **status ladder** per item. **Local-first:** the graph lives in
> `<reportDir>/ext/coverage/coverage-data.json` — **no database, no server-side aggregator, no dashboard.**
> It is a pure function of its inputs (`karate-events.jsonl` + the git requirement files), so
> it rebuilds on demand — a report is always derived from a real run, never authored by hand.
> **Prerequisites:** `Skill.help('api')`, `Skill.help('assertions')`.

## What this skill is for

Compute and interpret **coverage** for a Karate suite — several **sources**, one graph:

1. **Requirement coverage** — requirements authored in git markdown (`requirements/*.md`); a scenario
   covers one via a `@req=ORD-001` tag (or `@req=ORD-001/1` for an acceptance criterion).
2. **API coverage** — which OpenAPI operations does the suite hit? (`@cov=openapi:placeOrder`, or
   matched automatically from observed HTTP traffic, with `exact`/`templated`/`ambiguous` confidence).
3. **Rule / custom coverage** — any source plugged in over the `CoverageSource` SPI.

Cross-source joins work because **it is one graph**: a requirement, the operation that realizes it, and
the test that exercises both are connected nodes.

## The status ladder

Every coverage item derives exactly one status:

| Status | Meaning |
|---|---|
| `COVERED` | exercised by a passing test (observed hit and/or a `@req=`/`@cov=` declared edge) |
| `FAILING` | exercised, but the covering test failed |
| `NOTRUN` | linked to a test that was skipped |
| `NOTCOVERED` | no covering test at all — the gap |

A `COVERED` item can also carry **`mockOnly`** — every observed exercise of it was answered by a **karate
mock**, which says so on the wire (a `Karate-Mock` header on everything it fabricates). The traffic was
real; the responder was a stand-in you started. It is **not** a lesser status and not a risk grade — a mock
is a legitimate stand-in — it exists so a green can say *what answered it*, which no address could tell you
(a real service on `localhost` is ordinary). Read it in one direction only: **true is certain, absent means
unknown** — WireMock, a stubbed `baseUrl` or a fake staging service answer exactly like the real system. So
never report an unflagged item as "verified against the real system".

A `COVERED` item can additionally carry **`oracleOnly`** — the rules realize it (a `calc.req` arm ran),
but nothing *outside* the rulebook checked it, so it is the rulebook vouching for itself. It is cleared
by a `@req=` test, a wire hit (HTTP/gRPC/Kafka) from a covering scenario, or a `Rule.execute(...).verify(ok)`
stamp. Read it as "green, but nobody independent looked" — see the `rules` help page and the `api`
skill's "the RULE is the oracle".

## How you query it (over `karate serve` → `POST /api/eval`, raw JS; or MCP)

Each namespace has a `help()` verb — **call it first if unsure**: `Coverage.help()`, `Requirement.help()`,
`Graph.help()`.

- **`Coverage.*`** — the source/item view: `Coverage.summary()` (per-source totals + a `byStatus`
  breakdown), `Coverage.gaps()` (NOTCOVERED items + `happyOnly` — items exercised only on the success
  path, never an error outcome; each row carries the spec's `declaredUntested` codes — + unmatched
  calls), `Coverage.status('openapi:placeOrder')` (one item — status, call count, covering tests, the
  `requirements` that claim it, the `oracleOnly`/`mockOnly` disclosures, and — once exercised — the
  observed status histogram + the error-path `outcome` incl. `declaredUntested`),
  `Coverage.items([source])`, `Coverage.hits([itemId])` (with match `confidence`).
- **`Requirement.*`** — the requirement view + governance: `Requirement.matrix()` (per-requirement
  status + covering tests + acceptance criteria), `Requirement.gaps()`, **`Requirement.readiness()`**
  (the confidence-to-ship verdict), `Requirement.lint()`, `Requirement.posture()`.
- **`Graph.*`** — raw traversal when the views aren't enough: `Graph.neighbors()`, `Graph.traverse()`, `Graph.path()`.
- **`Report.generate()`** — a `file://`-openable standalone scorecard straight from the graph.
- **`Report.gate()`** — the same verdicts as the pull-request check payload (conclusion + line annotations).

> **Gotcha:** `Coverage.summary()`’s per-source `percentage` is derived from item **status** (COVERED /
> total). Requirement coverage is best read from **`Requirement.matrix()`/`readiness()`** — they add the
> criticality/risk/readiness layer `Coverage` doesn't.

### The post-run loop — run, THEN read

On the console every settled run — suite, check, live session, loop — folds into the project graph, so
you read straight after it:

```js
Runner.run('checks/quote-api.feature')   // 1. run (Runner.suite(...) for a folder) — its settle folds it in
Requirement.matrix()                     // 2. read: matrix / readiness / Coverage.summary / gaps
```

A plain `karate run` on the CLI writes the graph itself as the suite exits (the coverage ext is booted).
`Report.aggregate()` is for a run-free rebuild (the declared universe before any run) or a source
`karate-boot.js` does not declare. If a view returns "no graph yet", nothing has run — run, or aggregate.

> **Declare the WHOLE universe in `karate-boot.js`** (`cov.rules = 'rulebooks'`, `cov.openapi =
> 'openapi.yaml'`): every settle, `Report.aggregate()` and `Rule.cover()` rebuilds `coverage-data.json` from
> all runs plus those sources. Sources passed to `Report.aggregate({rules, spec, requirements, events})` are
> kept for every later rebuild where `karate-boot.js` declares none of that kind, until an aggregate naming
> sources replaces them (`{spec:[]}` drops them); a kept one that no longer resolves is listed in
> `selection.droppedSources`.

## Coverage without executing — the dry run

**"Do we have a test for this?" does not need a run.** A *declared* edge (`@req=`, `@cov=`) lives in the
feature text, so it is knowable statically — and a **dry run** is how you harvest it:

```js
Runner.suite('checks/', { dryRun: true })   // JS / MCP / console
Requirement.matrix({ run: '<runId>' })      // the DECLARED RTM — no SUT, no traffic, no mock
```
```bash
karate run -D -o target/rtm-dryrun checks/                       # the same thing on the CLI
java -jar karate-agent-<v>.jar run -D -o target/rtm-dryrun checks/   # ... or straight off the jar
```

Every scenario is discovered and its tags read; **no step, hook or `karate-config.js` executes**. You still
get the JSONL, the report and the full coverage graph — built from the declared edges alone, with **zero
observed hits**. It is cheap enough to run on every PR, and it is the honest answer to *"can we get a
coverage report without running the suite?"*.

**Read it as an inventory, not a verdict** — the status vocabulary carries the whole meaning:

| Reads | Means | The fix |
|---|---|---|
| `NOTRUN` | a test **declares** this item; nobody ran it | run the suite for real |
| `NOTCOVERED` | **nothing in the suite claims it** — the authoring gap | write (or tag) a test |
| `COVERED` | *impossible from a dry run* | — |

That last row is the point: core stamps a dry-run step **`skipped`, never `passed`**, so a parse-only
pass can never manufacture a green. The headline coverage **`percentage` will read 0** — by design, not a
bug. What you read is the NOTRUN-vs-NOTCOVERED split, and the result carries `skipped` + `dryRun:true`
because a skipped scenario counts toward `passed` (nothing failed — nothing ran).

Four things to know before you rely on it:

1. **It measures your tags, not your tests.** A test that verifies ORD-001 without the `@req=ORD-001/1` tag
   is invisible to it. Tag discipline *is* the measurement — or bind in bulk from `karate-boot.js`:
   `cov.map = s => REQ_TO_OPS[s.tagValues.req[0]]` derives each scenario's edges (a function returning
   an id, an array or null; or a data map keyed by scenario slug), merged with the inline tags.
2. **API/contract coverage needs `@cov=`.** An OpenAPI/gRPC operation is normally matched from *observed*
   traffic, which a dry run has none of — so operations read NOTCOVERED unless scenarios carry
   `@cov=openapi:<operationId>`. Add that tag and the declared operation coverage is answerable statically
   too (the "annotate the test with the operation it exercises" pattern).
3. **No tag filter.** `tags`/`-t` excludes non-matching scenarios from the run entirely, so their declared
   edges never land — a *complete* declared RTM needs a dry run with **no** tag filter.
4. **`@setup` scenarios really execute** (they are exempt from the skip), so their traffic is real.

Use a separate report dir (`-o` / `{output:…}`): a run replaces the report dir it writes, so a dry run
pointed at your normal one displaces the real graph with an all-NOTRUN one.

*The dry run answers "does a test exist that claims this?" A real run answers "does the system actually do
it?" Neither is line coverage — pair with JaCoCo if that is the question.*

## Mental model

```
   karate run
   ──────────────────────────────────────────
   each scenario emits a FEATURE_EXIT event with
     slug + tags (@req=/@cov=) + embeds (http/grpc/emit)
                       │
                       ▼   TraceGraph.build  (a pure function)
   coverage-data.json  =  karate-trace/v1 graph
     ├─ items[]   (nodes: requirement / openapi-op / criterion …, each with derived status)
     ├─ tests[]   (nodes: executed scenarios, keyed by stable slug)
     ├─ hits[]    (edges: observed + declared, with match confidence)
     └─ sources[] (per-source summary)
                       │
                       ▼   read live, re-parsed on every call
   Coverage.* / Requirement.* / Graph.*  (JS/MCP)   ·   Report.generate()  (file:// HTML)
```

A well-organized suite produces a clean graph **with no config** — the folder/feature/scenario tree is
the structural rollup, tags are the cross-cutting slice. See `Skill.help('coverage', 'suite-structure')`.

## Standing rules

1. **Coverage is a *gap-finder*, not a *quality-meter*.** 100% coverage means every item was exercised;
   it says nothing about assertion strength.
2. **An observed hit proves a call was made and matched — never that the real system answered it.** If you
   stand up a mock and drive it, that is genuine coverage of the *contract*, and it is reported as such:
   karate's own mocks disclose themselves (`mockOnly`), so say "exercised against a mock" when you see it.
   For anything else — WireMock, a stubbed base URL, a fake staging service — the graph cannot tell, so
   don't claim it either way.
3. **Pair requirement coverage with API coverage.** Requirement coverage catches "we forgot behavior Y";
   API coverage catches "we forgot endpoint X."
4. **Name scenarios for stability.** Cross-run identity is the stable slug (`__id` override, else the
   scenario name); a clear, durable name survives refactors better than any cross-reference.
5. **Don't gate CI on coverage % alone.** Gate on "no new gaps in this PR" — a delta beats an absolute threshold.
6. **`FAILING ≠ NOTCOVERED`.** A failing test still *exercises* the item; treat it as a distinct bucket
   (`Coverage.summary().byStatus`), not as a not-covered gap.
7. **"Done" means BOTH `gaps()` lists are empty** — `notcovered` (never exercised) *and* `happyOnly`
   (exercised, but never through an error outcome — each row lists the spec's declared-but-untested
   codes). A suite that is green with `happyOnly` entries has tested no failure behavior at all.

## Working in passes (large specs, multi-session)

The graph lives on disk and every verb re-reads it, so coverage state survives sessions — **don't persist
what the engine re-derives**. A fresh session re-orients in two calls: `Openapi.operations(spec)` for the
inventory, `Coverage.gaps()` for what's left. That makes per-slice passes the natural way to cover a large
spec: scope each pass to one resource/tag, end it with a run, and let `gaps()` be the worklist handed to
the next pass — "done" stays deterministic across any number of sessions, or even different agents per
pass. The one thing worth writing to a *file* between passes is what no verb can re-derive: business
intent — and the durable home for that is the requirements layer, not ad-hoc notes.

## Anti-patterns

1. **Chasing 100%** — some items are intentionally unexercised (health-check noise, deprecated ops).
2. **Counting a ran-but-didn’t-assert scenario as covered** — under-asserting masquerading as coverage.
3. **One templated scenario per OpenAPI operation** — hits the endpoint without validating the contract.
4. **Reaching for a database/dashboard** — the report is the product; the graph is a local file.

## Topic index

| Topic | Purpose |
|---|---|
| `Skill.help('coverage', 'suite-structure')` | Folder layout, scenario naming, tag discipline — the structural reference before authoring or refactoring a coverage-friendly suite. |
| `Skill.help('coverage', 'dimensions')` | **Coverage dimensions** — measure input-variation depth + required combinations, graded by a rulebook (the "out-model the fuzzer" API-adequacy surface): `dimensions.js` / `cov.dimensions` / `Coverage.dimensions()`. |

## Cross-skill links

- `Skill.help('api')` — the REST authoring this measures.
- `Skill.help('requirements')` — authoring requirements + the readiness scorecard.
- `Skill.help('link')` — you have tests and requirements and nothing joining them: tag the existing suite
  and grade the join, no new tests.
- `Skill.help('grpc')` — gRPC contributes via its own coverage source (descriptor-grade request-field dimensions).
- `Skill.help('kafka')` — `cov.kafka` topic×direction, plus Avro **field-dimension** coverage via the registry form (`{ schemaRegistry }`).
- `Skill.help('review')` — review surfaces missing error-path scenarios via coverage gaps.

## References

- `io.karatelabs.ext.coverage.*` (the engine + `CoverageSource` SPI), `io.karatelabs.ext.coverage.graph.*`
  (`TraceGraph` / `PropertyGraph` / `MatchKind`).
- The JSAPI front doors: `Coverage` / `Requirement` / `Graph` / `Report` (each with a `help()` verb).
