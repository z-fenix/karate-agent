# Scaffold from OpenAPI — Skill

> **What it is:** turn an OpenAPI spec **alone** — no hand-written project — into a runnable Karate
> project: a stateful mock, tests that exercise it, derived requirement stubs, and a 3-source coverage
> report. A **composite** workflow — it names the primitives below and you drive them; the
> deterministic tools do the work, **a human PR review is the oracle**.
> **Why it's review-gated:** this is *authoring*, not the autonomous loop. The derived requirements are
> deliberately **hollow** (they restate the API's behavior, not the business intent) — they need a human
> to ground them before anyone trusts the green.

## When to use this

- You have an OpenAPI/Swagger spec and want a working Karate test project fast.
- You want a **stateful** mock of an API (CRUD that actually holds state) to develop against before the
  backend exists — and the *same* spec to score coverage of your tests.
- You want a first-draft **requirements set** reverse-engineered from a spec, to review and ground.

## The primitives this composes (drill into each with `help()`)

| Primitive | Role |
|---|---|
| `Openapi.scaffold(spec, {out})` | **the one-shot** — writes the whole runnable harness (boot, config, start-mock, starter feature, `requirements/derived.md`) from the spec alone. Start here; reach for the primitives below only to customize — `Openapi.help()` |
| `Openapi.summary/operations/resources(spec)` | **orient** in the spec — kind, ops, detected CRUD resources — `Openapi.help()` |
| `Openapi.subset(spec, {resources\|paths\|tags})` | **carve a self-contained slice** ($ref closure) of a big spec into a focused fixture |
| `Http.mock(spec, opts?)` | stand up a **stateful** mock from the spec (CRUD by convention) — `Http.help()` |
| `Http.get/post/…(url)` | **explore** the running mock (or any live API) to confirm its behavior before authoring tests — `Http.help()` (the `api` skill's explore→author loop) |
| `boot.ext('http')` (karate-boot.js) | light the `Http` global **in-run** so `karate-config.js` starts the mock itself |
| `Openapi.deriveRequirements(spec)` | reverse-engineer **hollow** requirement stubs — `Openapi.help()` (topic below) |
| `cov.openapi` (karate-boot.js) | register the spec as the **openapi coverage source** — `Coverage.help()` |
| `Requirement.matrix()` / `gaps()` / `readiness()` | the requirement side of the report — `Requirement.help()` |
| `Report.generate()` | the standalone `file://` coverage scorecard — `Report.help()` |

You do **not** need `Runner.*` or any engine work — core `karate run` (or `Runner.*` in Java)
drives the real run against the mock. The only net-new is *authoring*.

## The flow

**Fast path — one call writes steps 1–4:** `Openapi.scaffold('api.yaml', {out:'.'})` emits the whole
harness (`karate-boot.js` + `karate-config.js` + `start-mock.js` + a starter `checks/<name>.feature` +
`requirements/derived.md`) and copies the spec in. An existing file refuses the whole write, named in the
error; `{overwrite:true}` replaces it. Run it, then jump to step 5 (run) and step 6 (report).
The numbered steps below are what `scaffold` produces — follow them by hand only to customize.

**Honor the project layout.** Authored artifacts have a home — a `.feature`/`.js`/`.md` runnable
goes under `checks/`, a requirement under `requirements/`, a rulebook under `rulebooks/`. `scaffold` already
writes there; when you author *more* files, put them in the same dirs so the console's runs view and
`Check.list` surface them. `Work.dirs()` returns the resolved roots for the active project (a future
`karate-pom.json` will remap them for Maven/Gradle parity).

```
   OpenAPI spec
        │
        ├─(0)─ Openapi.summary/resources(spec) ... orient; for a big spec, Openapi.subset(...) a slice
        │
        ├─(1)─ create the project skeleton (karate-boot.js, karate-config.js)
        │
        ├─(2)─ Http.mock(spec) ............ a stateful backend (CRUD by convention)
        │        wired in-run via boot.ext('http') + karate.callSingle in karate-config.js
        │
        ├─(3)─ author *.feature tests ............ exercise create→read→update→delete against the mock
        │        cov.openapi = '<spec>' in karate-boot.js → the openapi coverage source
        │
        ├─(4)─ Openapi.deriveRequirements(spec) .. hollow requirement stubs → requirements/derived.md
        │        @source=derived, @real=openapi:<opId>, EARS acceptance, @criticality BLANK
        │
        ├─(5)─ run: karate run <feature> ......... real HTTP traffic → observed coverage
        │        (boot.ext('coverage') auto-enables the karate-events.jsonl stream it computes from)
        │
        └─(6)─ the 3-source report ............... Report.generate() + Requirement.matrix()
                 openapi (operations exercised) + req (stubs) + rules (if any)
```

### 0 — orient, and slice a large spec
Enterprise specs are huge (ThousandEyes is 30k lines / 280 ops). Don't read the YAML — ask the spec.
A spec path is a **path reference** anchored at the project root: use `/api/openapi.yaml` (or the
bare `api.yaml` for a spec at the root). `classpath:` also works — it asks the Java classloader first and
falls back to the project — but the `/`-form is shorter and identical in both worlds:

```js
Openapi.summary('api.yaml');     // specType, baseUrl, op count, detected resources + CRUD-by-action
Openapi.resources('api.yaml');   // the REST resources + which op plays each CRUD verb
Openapi.operations('api.yaml');  // the full per-operation inventory
```

For a focused demo/test, carve a **self-contained** slice (the transitive `$ref` closure is resolved for
you — schemas, params, responses, security schemes — across OpenAPI 3 **and** Swagger 2):

```js
var out = Openapi.subset('api.yaml', { resources: ['users','roles'] }); // or {paths:[...]}/{tags:[...]}
// write out.spec to api.yaml — it parses standalone; feed it to Http.mock / cov.openapi / deriveRequirements
```

This is how the bundled `thousandeyes/api.yaml` demo fixture was made — a 6-path slice of the real 1MB
ThousandEyes contract, refs closed over, generated by `Openapi.subset` itself.

### 1 — the skeleton
A folder with `karate-boot.js` and `karate-config.js`. The boot file activates the exts:

```js
// karate-boot.js
boot.ext('http');                 // Http global, in-run (Option B)
const cov = boot.ext('coverage');
cov.openapi = 'api.yaml';            // the openapi coverage source (workdir-relative)
```

### 2 + 3 — the stateful mock, started in-run
`Http.mock` gives CRUD-by-convention for free (POST creates → GET returns it → PUT/PATCH update →
DELETE removes), so a collection/item path pair (`/pets` + `/pets/{id}`) needs **zero handler
code**. `validate: true` answers 400 to a body that breaks the request schema; a created entity gets the
response schema's missing required fields (first enum value, else a typed zero). Start it once per suite
from `karate-config.js`:

```js
// start-mock.js  (at the project root; called once via callSingle)
function() { var mock = Http.mock('api.yaml', { validate: true }); return { url: mock.url }; }

// karate-config.js
function fn() {
  // root-anchored '/start-mock.js' (the paths.md web-root model: a leading '/' resolves from the project
  // root, not the feature's dir) — required because the starter feature lives under checks/.
  var started = karate.callSingle('/start-mock.js');
  return { baseUrl: started.url };
}
```

`karate.*` is the **feature runtime's** API — it does not exist in the console. To stand the same mock up
from `/api/eval` (to probe it before writing the feature), run the very same file with
`var started = File.call('/start-mock.js')` — karate call semantics, so the file's function is invoked and
you get `started.url` back. What a handler script sees (`request`/`response`/`session`) is `Http.help('mock')`.

The mock is auto-stopped at suite end (the `mock` ext tracks it). Author features that drive **stateful
flows** against `baseUrl` — the create→read sequence is what makes the run *grounded*, not synthetic.
The mock knows only the spec: a computed value (an order total) or a rule the spec doesn't state (the
customer must exist) is a Rule (`Http.mock(spec, {rules:{opId:'rulebook'}})`) or a handler, and a test of
it proves the mock, not a backend. That one Rulebook is also the `Rule.execute` oracle (the keystone; see
the `rule-authoring` skill).

### 4 — derived requirement stubs (review-gated)
```js
var out = Openapi.deriveRequirements('api.yaml');
// write out.requirements to requirements/derived.md, then GROUND each stub by hand
```
The stubs are honestly **hollow** — see the `Skill.help('scaffold-from-openapi', 'reverse-engineer-requirements')`
topic for what they contain and how to ground them. **Do not ship them as-is**; that defeats the
reward-hacking gate (a green report over requirements that merely restate the spec proves nothing).

### 5 + 6 — run and report
Run the suite. **Working over MCP / `/api/eval` (a served project)? The server runs it for you — this is
the default:** `Runner.suite('checks/<name>.feature')` — coverage output (JSONL) is **always on**, the
call blocks and returns `{passed, failed, total, reportUrl, verdicts}` inline, and it works identically
whether the engine is a container or a local JAR (no shell, no PATH, nothing to install) — `Runner.help()`
has the options. Only reach for a **separate process** when you're not on a served project (or CI):
`java -jar karate-agent-<v>.jar run <feature>` — prefer invoking **the exact JAR you were
handed** (no PATH assumption, no version skew vs the serving engine); teams that installed the `karate`
CLI launcher can equivalently use `karate run <feature>`. **Never** download a
standalone `karate-<version>.jar` from GitHub (e.g. `karate-1.4.1.jar`) — that is the old `com.intuit.karate` 1.x
engine: it produces no coverage and hangs on Java 25. For the process forms, `boot.ext('coverage')`
auto-enables the enriched `karate-events.jsonl` stream coverage is computed from (passing
`-f karate:jsonl` explicitly is harmless and equivalent). The real traffic against the mock becomes **observed** coverage of the openapi source; the derived stubs' `@real=openapi:<opId>` edges let those
hits strengthen the requirements (graded by provenance — `incidental` until you add `@req=` tags and
ground the stub). Then:

```js
Report.generate();        // → the file:// coverage report page
Requirement.matrix();     // the per-requirement status
Requirement.readiness();  // the confidence-to-ship verdict (hollow stubs → low confidence, by design)
```

**Close the gaps before calling it done.** `Coverage.gaps()` returns two worklists per source —
`notcovered` (operations never exercised) and `happyOnly` (exercised only on the success path; each row
carries the spec's `declaredUntested` error codes) — and `Requirement.gaps()` the uncovered criteria.
Author tests and re-run until **all** of them are empty; a green suite with `happyOnly` entries has
proven no error behavior.

## The scaffolded starter feature is provisional — replace it, don't grow it

The starter `.feature` `Openapi.scaffold` emits is tagged **`@scaffold`** and is **bootstrap smoke, not a
deliverable**. Its calls count as **normal openapi coverage** (the operations *are* exercised — the
`@scaffold` tag has no special coverage semantics). But because it is **untagged** (no `@req=`), it can
only ever yield **`incidental`** provenance on a requirement wired to those operations via `@real=` —
never `exercised` (that needs a `@req=` anchor), so it **cannot make anything look ready**. Your real
tests are a **separate lineage**: `@req=ID/criterion`-tagged, intent-bearing, `@ai`-marked. The recommended
sequence:

1. **scaffold** → prove the loop (green smoke + 3-source report);
2. **ground** the derived requirement stubs (real intent, `@criticality`, EARS; drop `@source=derived`);
3. **author** `@req=ID/criterion` intent-bearing tests (stateful flows), marked `@ai`;
4. **retire** the `@scaffold` starter feature as your real tests come to exercise the same operations
   (or keep a deliberate thin contract-smoke). `@scaffold` marks it disposable — unlike `@ai`, which a
   human must approve before removal.

Don't bolt `@req=` tags onto the starter's hollow scenarios to "turn them green" — a passing scenario with
no meaningful assertion is exactly the hollow-green trap. Author real tests, then delete the scaffold.

**Tag the criterion, not the requirement, when a requirement has acceptance criteria.** A bare `@req=ORD-001`
on a requirement that decomposes into criteria declares the edge (provenance shows `exercised`) but does
**not** satisfy any criterion — the requirement is *defined by* its criteria, so it stays `NOTCOVERED` until
each criterion has a covering test. This is deliberate (a wildcard "covers-all" would re-open the
reward-hacking hole). So tag the exact criterion the scenario verifies: `@req=ORD-001/1`, `@req=ORD-001/2`
(multiple criteria in one test → `@req=ORD-001/1,ORD-001/2`). If you see the confusing "I tagged it but
it's still NOTCOVERED", run `Requirement.gaps()` — its **`bareParent`** bucket names the requirement and the
exact criterion-level tags to add. Use a bare `@req=ID` only for requirements that have **no** acceptance
criteria (then it covers the requirement directly).

## What "done" looks like (and what it deliberately is not)

- **Done:** a runnable project from a spec alone — stateful mock + tests reaching the operations + a
  3-source report. The *plumbing* is proven.
- **NOT done by scaffolding alone:** trustworthy requirements. The derived stubs are placeholders; the
  report will correctly show them as ungrounded. Grounding them (real intent, `@criticality`, real
  acceptance, dropping `@source=derived`) is the human's job — that is the point.

## Topics

- `Skill.help('scaffold-from-openapi', 'reverse-engineer-requirements')` — what
  `Openapi.deriveRequirements` produces, why the stubs are hollow, and the grounding checklist. (Also the
  home for reverse-engineering requirements from an existing run's coverage — run/coverage → stubs.)

## Related skills

- **`api`** — author the `.feature` tests (the explore→author loop with `Http.*`, schema-shape-locking `match`).
- **`requirements`** — authoring requirements properly (the grounding target) + lint/readiness/review.
- **`migrate-to-karate`** — the sibling front door for *existing* test assets (Postman, rest-assured, …).
