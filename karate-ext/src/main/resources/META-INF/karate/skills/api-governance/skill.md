# API governance — Skill

> **What it is:** grade an OpenAPI/Swagger document against a style guide *and against what actually ran*,
> and get back a scored, gated scorecard you could hand an auditor. `Openapi.lint` is the findings;
> `Openapi.grade` is the same lint scored into five dimensions, a maturity level and a ship verdict.
> **Deterministic end to end** — no LLM, no network, no run required for the document half, so
> the number is one you can re-derive and attest.
> **What makes it more than a linter:** rules that reach past the document into the project's coverage
> graph — *is this operation claimed by a requirement, was it ever exercised, did anything other than our
> own mock ever answer it?* No document check can ask those, and they are the ones worth having.

## When to use this

Match this skill to a task worded any of these ways — they are all this lane:

- *"is our API spec up to standard?"* · *"can I get a governance grade?"* · *"lint the spec"*
- *"enforce our API style guide"* · *"we have a Spectral ruleset"* · *"API governance"* · *"conformance"*
- *"is this API ready to ship?"* · *"score the spec"* · *"what is blocking release?"*

**Not** this skill: `scaffold-from-openapi` (spec → a runnable project), `review` (feature-file quality),
`requirements` (governing requirements rather than the spec).

## The primitives this composes

| Primitive | Role |
|---|---|
| `Openapi.lint(spec[, opts])` | the bundled rulepack over the document → `{findings, waivers, graph, rules, summary, capabilities, ruleset}` (`ruleset` = `{digest, total, bundled, project, opts}`, the policy that produced the verdict); a finding that **has a pointer** carries `line`/`col` + a `position` span, and one the engine cannot honestly pin carries none rather than a plausible wrong one — `Openapi.help()` |
| `Openapi.grade(spec[, opts])` | the same lint **graded** → `{verdict, index, level, dimensions, notMeasured, gates, waived, weights, judged, scoring, lint}` — `Openapi.help()` |
| `opts.rules` / `cov.lintRules` | **your own rules** — one-off on the call, or declared once in `karate-boot.js` so every surface honours them — topic below |
| `opts.waivers` / `# karate-lint-disable-next-line` | the two waiver homes: project-level, and comment-anchored at the point of violation |
| `Contract.pair({suite, spec, provider})` | the **paired run**: one functional suite against the real provider AND against the mock, in one session, one order → a measured **divergence set** written as a dated file you commit. The only thing that measures the `contract` dimension — `Contract.help()` |
| `Contract.read([file][, {asOf}])` | a stored pair, with its **freshness derived at read time** (`fresh` / `stale` — a binding changed / `expired` — it is simply old / `unbindable` · `undatable` — it cannot be checked or dated at all). The file is never rewritten. With no file it reads the pair minted **most recently** — by the pair's own `mintedAt`, not by file name — and `selected` says so when others are on file |
| `Contract.report([file][, {asOf, out}])` | that same reading as an **HTML page** in the report dir (beside Coverage and Traceability), for a reader who is not at a prompt. A projection of `read` — it re-derives nothing |
| `Report.aggregate()` | writes the **coverage graph** the graph-reaching rules read — without one they are inert; on the console every settled run also folds into it |
| `Coverage.status(id)` / `Coverage.gaps()` | what the graph knows about one operation (`openapi:<operationId>`) |
| `Runner.suite('checks')` · `Harness.startLive()`/`stop()` | the two ways to put real evidence in the graph |

## The flow

```
   OpenAPI spec
        │
        ├─(1)─ Openapi.grade(spec) ......... the scorecard, cold. Read `verdict`, `index`, `level`,
        │        `gates`, and `notMeasured` FIRST — the gates decide the verdict, not the number
        │
        ├─(2)─ read every `not measured` note ... each names the call that moves it, or says plainly
        │        that nothing does. Act on it; don't skip past it to the score
        │
        ├─(3)─ Report.aggregate() ........... write the graph, so the graph-reaching rules stop being
        │        inert. Then get real evidence in (suite or live run) — each settled run folds in
        │
        ├─(4)─ author your own rules ........ the house style guide + the upgrade rules that reach the
        │        graph → topic `lint-rule-authoring`
        │
        ├─(5)─ waive what you accept ........ with a reason and an owner, in git — never silenced
        │
        ├─(6)─ fix, re-grade, watch it move . the loop a customer actually buys
        │
        └─(7)─ Contract.pair({...}) ......... the ONE thing that measures `contract` — and the only route
                 to the top rung. Needs a real provider to point at; release-time, not per-commit
```

### 1 — grade it cold

```js
var g = Openapi.grade('openapi.yaml');
({ verdict: g.verdict, index: g.index, level: g.level, gates: g.gates, notMeasured: g.notMeasured });
```

Read it in this order, because they answer different questions:

- **`gates`** — deterministic blockers. A gate *decides the verdict* and is never folded into the index, so
  a good-looking number with an open gate is still `NOT_READY`. This is the first thing to report.
- **`level`** — the rung (**Ungoverned → Described → Governed → Traced → Exercised → Proven**) plus
  `level.blockedBy`, the one sentence saying what is holding it here, and `level.ladder`, every rung with
  what it asks for — answer *"what would it take to reach the top?"* from that rather than by climbing one
  rung and re-grading. A rung is only awarded where the dimension behind it was *measured*, so an unmeasured
  dimension stops the climb rather than being waved through.
- **`dimensions`** — `design` · `security` · `documentation` · `provenness` · `contract`. A `cappedAt` means
  one critical finding is holding the dimension down however good the rest is; `cappedBy` names it.
- **`index`** — last. It is a weighted harmonic mean over **what was measured**, renormalised, with
  `weights.note` saying so.
- **`g.lint.findings`** — the actual worklist, each with a `line`/`col` to jump to.

`{output:'both'}` renders the same thing as markdown for a human.

### 2 — `not measured` is a worklist item, not a footnote

A dimension with no evidence reads **`not measured`** — never zero, never quietly dropped. Each one carries
a `note`. **Act on the note**; the whole point of the scorecard is that it refuses to score what it did not
look at, so leaving one unmeasured caps the ladder.

- **`provenness`** unmeasured means *no coverage graph was readable* — see step 3.
- **`contract`** unmeasured means one of two things, and the payload distinguishes them: *no paired mock-vs-provider run has been recorded for this artifact*, or one has and its rung is **withdrawn** because the spec, suite or mock moved since (the row then carries the pair, its freshness and what changed). It
  is never inferred from the document. The call that moves it is
  **`Contract.pair({suite, spec, provider})`** — it runs your functional suite against the real provider and
  against the mock in one session and writes the divergence set as a dated file. **It needs a provider you
  can point at**; with none available, say so plainly and report the dimension as unmeasurable rather than
  hunting for another verb (the nearest evidence without a provider is the `operation-not-mock-only` rule,
  which tells you where only our own mock ever answered). Until a pair exists the dimension is renormalised
  out of the index rather than counted against you, and it holds the top rung (*Proven*) out of reach.
  **Read a pair as TWO numbers** — the divergence-free proportion means nothing without the coverage of the
  claim beside it, and an unexercised operation is `not measured`, never `substitutable`.

### 3 — give the graph-reaching rules something to read

**Do this before grading.** The graph-reaching rules read the
project coverage graph; with none they are inert, and `provenness` stays unmeasured. Write it:

```js
Report.aggregate();               // even with NO runs this renders the DECLARED universe (every
                                  // operation as a gap) — which measures `provenness` honestly at its floor
```

Then put real evidence in. On the console each settled run folds into the graph — re-grade straight after:

```js
Runner.suite('checks');           // a suite run — the normal path

// or, exploring from this console: probes only land in the graph inside a LIVE RUN
Harness.startLive();
var mock = Http.mock('openapi.yaml');
Http.configure({ baseUrl: mock.url, spec: 'openapi.yaml' });
Http.get('/policies');            // a bare probe OUTSIDE the bracket is not evidence
Harness.stop();                   // the stop folds the probes into the graph
Coverage.status('openapi:listPolicies');   // → COVERED, callCount 1, mockOnly:true
```

`mockOnly:true` is the honest read of that last one: our own mock answered, so `operation-not-mock-only`
still fires. That is the design, not a bug — evidence is *recorded*, never inferred.

### 4 — author the house rules

The bundled pack is generic hygiene plus the graph-reaching three. Your style guide is yours to write:
`{rules: {...}}` for a one-off, `cov.lintRules` in `karate-boot.js` to make it the project's law.
→ topic `Skill.help('api-governance', 'lint-rule-authoring')`.

### 5 — waive what you accept, visibly

Two homes, and **a waiver never silences**: the finding is still reported, marked `waived`, with the reason
and owner in the register — and the report refuses to call an API *clean* when it passes with exceptions.

```js
Openapi.lint('openapi.yaml', { waivers: { rule: 'info-contact', scope: '/info',
    reason: 'tracked in POL-42, owner assigning next sprint', owner: '@ptrthomas' } });
```

```yaml
servers:
  # karate-lint-disable-next-line server-https — local dev only, prod is https (owner: @ptrthomas)
  - url: http://localhost:8080
```

**Not every finding can be waived inline.** The comment binds by *line adjacency*, so it needs a construct
to sit above — a **whole-document** finding (`pointer: ""`, e.g. `security-scheme-declared`, which is about
what the document never says) has none. A comment on the first line binds to the first *key*, not the root,
and the register reports `applied: 0` with a note. Waive those through `opts.waivers` with an empty `scope`.
Check `finding.position`/`pointer` before reaching for the inline form.

`source:'inline'` is the comment-anchored one (bound to that exact pointer, nothing beneath it);
`source:'project'` is the project-level one (a rule id + a JSON-Pointer scope prefix) — the project's waiver
register, handed in as `opts.waivers`. A waiver missing its reason or owner is rejected and said so. Reason and owner are required because `git blame` is only an approval record if a
name is attached.

**A waiver DOES move the score, by exactly as much as fixing would** — a waived finding is not counted as a
failing subject, because that is what accepting an exception *means*. So the index alone cannot tell an
excused artifact from a clean one, and you must not report it as if it could. What tells them apart travels
with the grade: **`grade.waived.count`** (and its note), `lint.summary.clean` — false while any waiver
stands — and the signed register in `lint.waivers`, every entry carrying its reason and owner. **Report the
waiver count whenever you report the index.**

### 6 — close the loop

Edit the spec, re-grade, and report the delta — the finding count for that rule, the dimension score, the
index, and whether the verdict or rung moved. One fix in one dimension usually should *not* flip the
verdict; say so rather than implying it did.

### 7 — measure `contract`: mint a pair

This is the only evidence there is for the `contract` dimension and the top rung — and `Openapi.grade`
reads it, so mint the pair and then re-grade to show the dimension and the level move. It needs a
**real provider you can point at** — if there is none, stop here and report the dimension as unmeasurable.

```js
Contract.pair({
  suite:    'checks/contract',        // one ORDINARY functional suite — the shared artifact both legs answer to
  spec:     'openapi.yaml',
  provider: 'https://staging.example.com',
  mock:     'mock/api.feature',       // THE MOCK YOU SHIP. Omit it and you grade a synthesized one instead
  profile:  'seeded',                 // the suite creates what it needs through the API (the default)
  ignore:   'nondeterminism'          // opt IN to the bundled starter waivers; nothing is ignored by default
});
```

The suite needs exactly two contract-shaped things: **the target as a variable**
(`karate.properties['contract.target']` → `* url target`) and **`@destructive` on anything that must not
run against a real provider** — those are held out of *both* legs and named in the denominator.

**Most real suites already read a property of their own** — `baseUrl` in `karate-config.js` is the common
one. Do NOT rewrite the suite to match the example above: pass **`targetProp:'baseUrl'`** and the harness
hands each leg its target through the property the suite already reads. (Check first: `File.read` the
suite's `Background` and its `karate-config.js`, and use the name you find there.)

**Name the `mock`, and check what it resolved to.** `mock` takes the URL of a mock already standing, the
path to your project's own karate mock feature, or `Http.mock` options overlaying the spec. **Omit it and
the pair synthesizes one from `spec`** — which is a legitimate default and a *different subject* from the
mock your CI runs, and one that tends to agree with spec-shaped assertions by construction. `bindings.inputs.mock`
reads `auto:<spec>` when it was synthesized; read it before you report the claim.

**No `spec`?** The pair still runs — internal APIs often have no document, and the comparison never needed
one. Operations are named by method and path, response headers are not compared, and the evidence binds no
contract document (the claim block carries `noSpec` and the sentence says so). Two consequences: `mock` is
**required** (nothing to synthesize one from), and `Openapi.grade` will not read the pair — the `contract`
dimension needs a pair bound to the document being graded.

**When you run one, and what reads it afterwards.** A pair is **release-time, not per-commit** — it needs a
provider, runs the suite twice in one pinned order, and holds destructive scenarios out of both legs. The
per-commit job is the everyday mock leg (an ordinary `Runner.suite`), which is a regression check on the
mock and is *not* half of a pair. While you are still wiring it up, pass **`{draft:true}`** — same run, but
it lands under `target/` so it is not project evidence and nothing commits it.

Then there are two ways the result gets used, and they want different things:

- **Gate, in the same session** — `Contract.pair(...)` hands back the summary (the claim sentence, the
  rung, the counts, one line per finding), so a release job asserts on it and fails the build there and
  then. Nothing has to be read back for this; add `{full:true}` if the gate needs the compared values.
- **Everyone who did not run it** — that is what the committed file is for. `Contract.read(file)` derives
  whether it is still believable (a summary; `{full:true}` for the artifact), and `git blame` at the
  release commit is the audit record, and `Openapi.grade` reads it — the `contract` dimension scores the
  rung it earned. Re-running is often not an
  option later: staging may need credentials, be rate-limited, or be gone.
- **A reader who is not at a prompt** — `Contract.report([file])` writes the same reading as an HTML page
  (`ext/contract/pages/contract.html`) into the report dir, beside Coverage and Traceability, so publishing
  the report dir publishes the divergence set. It renders what `Contract.read` returns and re-derives
  nothing, so the page and the verb cannot disagree; freshness is derived at read time, so the page is a
  snapshot at the instant it was rendered and wants re-rendering after every mint.

Reading the result:

- **`outcomes`** — `agreed` · `staleMock` (mock passes, provider fails) · `mockBug` · **`notassertedDivergence`**
  (both legs passed and the responses differ — a finding owned by *your suite*, whose assertions are too
  loose to see it) · `agreedFailure` (both red — agreement, not a fidelity finding) · **`notmeasured`**.
- **`notmeasured` is decided before anything is compared** — an environment failure, a failed precondition
  probe, an ambient read. Never report one as a divergence.
- **`claim` is two numbers**, always quoted together. Never say "97% substitutable".
- **`ignored`** is a waiver register: reasons, owners, and a **`stale`** mark on a rule that covered nothing.
  A rule is scoped by `key` (a field name at any depth — the portable form) or by `scope`, a JSON Pointer
  **relative to the exchange** (`/response/body/createdAt`), never against the `/exchanges/<i>/…` path the
  finding is reported at.
- **`claim.rungs`** — the graded ladder: each operation gets a rung (1 `shape-conformant` → 2
  `verified-against-mock` → 3 `verified-against-provider` → 4 `proven-substitutable`), and the artifact
  holds the **floor** of them, capped below 4 while any declared operation is unexercised. **Quote the rung
  WITH its bars**, never alone: every bar carries the count that shows it was asked, and `0 of 0` means NOT
  CHECKED. Rung 4 needs more than "no differences" — the `seeded` profile, a probe that *observed* the two
  legs starting equivalent, a provider that is not one of our mocks, and **the mock your project ships**
  (omit `mock` and you grade a synthesized one, which caps at 3). Standing a bar down is a waiver:
  `bars: {dataProfile|mockIdentity|probeVerified: {reason, owner}}`; `adherence` and `rehearsal` cannot be
  stood down at all.
- **The rung moves the grade.** `Openapi.grade`'s `contract` dimension reads the recorded pair and scores it
  as the rung ÷ 4, and the maturity level `Proven` is awarded at rung 4 and nowhere else. Re-grade after
  minting and report the delta.
- Commit the evidence file. `Contract.read(file, {asOf})` derives its freshness later — `stale` names the
  binding that changed, `expired` means we simply no longer believe an unobserved provider has not moved, and
  `unbindable`/`undatable` mean the file records nothing to check against or cannot be dated at all. All four
  withdraw the rung rather than lowering it.
  A decayed pair holds **no rung at all** rather than a smaller one, and the summary `Contract.read`
  returns by default says so without your paying for the whole artifact.

## Rules before you report

1. **No graph, no graph-reaching rules.** `Report.aggregate()` writes the declared universe; each settled
   console run then folds its evidence in. (Step 3.)
2. **Zero findings from a graph-reaching rule can mean *inert*, not *clean*.** Check `lint.graph.available`
   and `lint.graph.note` before reporting a pass — with no graph those rules asked nothing.
3. **A bare console `Http.get` is not coverage.** Bracket probes in `Harness.startLive()`/`Harness.stop()`;
   read out anything you want to keep before you stop.
4. **`contract` moves only from a paired run — `Contract.pair`.** No amount of linting, running or
   aggregating touches it, and a mock-only suite run is a regression check on the mock, not half of a pair.
   If there is no provider to point at, report the dimension as unmeasurable and say why.
5. **A waiver moves the index exactly as much as a fix does.** Never report an index without
   `grade.waived.count` beside it. (Step 5.)
6. **The score is not the headline — the gate is.** Report `gates` and `level.blockedBy` first.
7. **Before quoting a pair, read `legs.*.target`, the divergences and `rehearsal`.** An all-`agreed` pair
   from a suite you did not wire to the target, or a provider leg answered by a karate mock (`rehearsal`),
   compared nothing real. A suite that reads its own base-URL property needs `targetProp:'<prop>'`. When a
   leg's address genuinely IS the target under another name (`host.docker.internal`, a gateway), declare
   `aliases:[{hosts:[…], reason, owner}]`; never alias the mock leg's own address — the fix there is
   `targetProp`.
8. **Between pairs against the same mock, call `mock.restart()`** (re-runs `Background`, same port, the handle
   stays valid). `reset()` clears only the call log and store; read its `{clean, survives, restart}` answer.
   A provider you did not start, only you can restart.
9. **`refresh.call` is only as good as the scope the pair recorded.** It re-mints from `scope`; anything the
   original call passed that `scope` does not carry cannot come back. Check the call it hands you names the
   same `mock` and `targetProp` you used before you run it — and read `refresh.caution` if it is there: a
   provider recorded as `localhost:<high port>` was a stand-in from somebody's console session, so pasting
   the call re-mints against a target that no longer exists (which reads as an environment failure, not as
   decay). Refreshing a draft keeps `draft:true`.
10. **A pair does not feed the project coverage graph** — `coverageGraph` in the payload says so. It runs
   the suite twice and holds `@destructive` out, so its traffic is deliberately not folded in. Read
   `Coverage.status` / the RTM after an ordinary `Runner.run(...)`, which folds in; a `NOTCOVERED` right
   after minting a pair is that, not a bug.

## What "done" looks like

- The verdict and the rung, with the **one sentence** saying what is holding it there.
- Every `not measured` either measured, or explained as unmeasurable — never silently skipped.
- Findings triaged into *fixed* / *waived with a reason and an owner* / *open*, and the re-grade delta shown.
- The index reported **with `grade.waived.count`** — an excused artifact and a clean one score the same.

## Topics

- `Skill.help('api-governance', 'lint-rule-authoring')` — writing your own rules: the shape, the
  selector dialects, the helper library, the sandbox's terms, how to retune or stand down a bundled rule,
  and how to write the graph-reaching rules that are the whole point.

## Related skills

- **`coverage`** — the graph these rules read: sources, atoms, gaps.
- **`requirements`** — the other half of the join `operation-claimed-by-requirement` asks about.
- **`scaffold-from-openapi`** — spec → a runnable project (the *authoring* front door, not the grading one).
- **`rule-authoring`** — a different thing despite the name: forward-engineering a business **rulebook**
  (the calc/oracle engine), not a lint rule.
