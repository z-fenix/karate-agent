# Karate Rule Authoring — Skill

> **What it is:** forward-engineer a **rulebook** — the decision logic behind a business requirement —
> from prose, then let the engine prove it. You write `calc.js` (and optionally `generator.js`); the
> engine generates the *smallest* set of test data that covers every rule and hands you back a decision
> table, Rule-Coverage %, gaps, dead branches, never-produced outcomes, and property violations. You read
> that feedback and iterate. **The rebuilt graph is the oracle — there is no human judge**.
> Optimize for that feedback loop.
> **Local-first:** a rulebook is a folder under the rules home; reports are self-contained `file://` HTML.
> **Drive it over `karate serve` → `POST /api/eval` (raw JS) or MCP** — `Rule.help()` prints the surface.
> **Or hand the whole loop to the product:** `Harness.codify(requirement, {book: '<name>'})` runs this
> skill's loop autonomously and ends COMPLETED only when the rulebook on disk clears a bar you set
> first (it still stops at its iteration/budget ceiling like any run) — the
> verdict is computed by the engine, not reported by the model. `Harness.help()` has the bar's shape.

## What this skill is for

Turn a business requirement ("premium = base-rate × age-factor, −10% multi-car; orders over $50k need
approval") into a **rulebook** the engine can exercise exhaustively and report on — then close every gap
the report surfaces until `Rule.check` is clean. Link each decision back to its requirement criterion with
`calc.req` so the run lands **requirement coverage** in the RTM (the branch-level counterpart to a
scenario `@req=` tag — see the `coverage` and `requirements` skills).

**This skill is paginated — the trunk is the whole loop; four deep-dives are one call away.** Pull the
one your next step needs, at the moment you need it:

| about to… | call |
|---|---|
| link a decision to a requirement criterion | `Skill.help('rule-authoring', 'requirements-linking')` |
| declare a guarantee or a situation | `Skill.help('rule-authoring', 'properties')` |
| write a `throw`, a reject row, or a pinned `_expect` | `Skill.help('rule-authoring', 'negative-scenarios')` |
| act on anything `Rule.check` reported | `Skill.help('rule-authoring', 'check-findings')` |

## The mental model

A **rulebook** is a folder under the rules home holding up to three files:

- **`schema.js`** — the *shape contract*. A single `schema = {...}` object: field names →
  `'#string'`/`'#number'`/`'#boolean'`, nested objects. It is a karate match-schema **plus three
  conventions of our own — the same three `Rule.ranges` reads, so one declaration buys enforcement and
  analysis at once**: a **list of literals is an ENUM** whose members are the whole domain
  (`state: ['CA','NY']` accepts `'CA'`, refuses `'ZZ'`, and membership is strict — `'1'` is not `1`);
  **`[ {...} ]` is an element TEMPLATE** ("every element looks like this", never "exactly one" —
  cardinality belongs to `g.array`); and **`'#date'` is ours**, an ISO `YYYY-MM-DD`. An empty list is
  refused at load. A numeric RANGE has no shape form — it belongs in `generator.js`
  (`g.int('age',16,90)`).
  **A field is required by default and undeclared keys fail** — the match is strict both ways. Make one
  optional with karate's **`##` prefix**: `notes: '##string'` accepts a row that omits it (or carries
  null) and still type-checks the value when it IS present — `'##date'` works the same way. Optional
  never means unchecked. The **enum form has no optional spelling yet**: a list of literals is always
  required.
  **Business fields only — never declare an engine key** (`_label`, `_id`, `_expect`, anything starting
  with `_`). They are dropped from both sides of the match, so declaring one checks nothing; the rulebook
  refuses to load if you do. `_label` still works as a row field and needs no declaration.
- **`generator.js`** (optional) — the *constrained input domain*. A single `function generate(g){...}`
  declaring every input dimension and its valid domain. This is what the explorer sweeps — and the
  membership test every verdict is scoped to. With no
  `generator.js` the engine falls back to sniffing the schema (one default array element, can't vary — so
  prefer a generator whenever arrays or pinned boundaries matter).
- **`calc.js`** — the *oracle*. A single `const execute = function(calc){...}` that reads `calc.input`,
  computes, writes `calc.output`, and narrates with `calc.log`.

**Determinism law (non-negotiable).** Reports must be byte-stable across runs. NO `Date.now()`, NO
`Math.random()`, NO `new Date()`, no wall-clock, no environment, no I/O. Rate against the input data only.
A rule that depends on "today" must take the date as an input field instead. **Date *math* must be
timezone-safe, not just wall-clock-free:** counting spans by raw millisecond diffs
(`(endMs - startMs) / msPerDay`) drops a day when a daylight-saving shift falls inside the span, so the
same input prorates differently across timezones. See "Common mistakes" for the fix.

## Writing `calc.js`

```js
// editable rate table FIRST, as a top-level const named `lookup`
const lookup = {
    baseRate: { basic: 0.02, standard: 0.035, premium: 0.05 },
    ageBands: [
        { maxAge: 25, factor: 1.6 },
        { maxAge: 40, factor: 1.0 },
        { maxAge: 65, factor: 0.9 },
        { maxAge: 200, factor: 1.2 }
    ],
    multiCarDiscount: 0.1
};

const execute = function (calc) {
    const input = calc.input;
    const rate = lookup.baseRate[input.coverage];
    calc.log('base rate for ' + input.coverage + ' coverage: ' + rate);

    let factor = 1, banded = false;
    lookup.ageBands.forEach(band => {
        if (!banded && input.driverAge <= band.maxAge) { factor = band.factor; banded = true; }
    });
    calc.log('age factor for driver age ' + input.driverAge + ': ' + factor);

    let premium = input.vehicle.value * rate * factor;
    if (input.multiCar) {
        premium = premium * (1 - lookup.multiCarDiscount);
        calc.log('multi-car discount applied: -' + (lookup.multiCarDiscount * 100) + '%');
    }

    calc.output = {                                  // write ONE object
        premium: calc.round(premium),                // round money: NEVER Math.round(x*100)/100
        coverage: input.coverage
    };
};
```

Rules:

- Put the **editable rate table in a top-level `const lookup = {...}`**. The console (and
  `Rule.lookups.update` / `Rule.impact`) AST-patch this const in place; helper functions and `execute`
  survive untouched. Keep rates as data here, not inlined in the logic.
- **Narrate every meaningful step with `calc.log(msg)`** — these become the audit tree and the diagram
  bubbles. Log the *value that drove* the decision, not just "applied discount".
- **Mark the business sections with `calc.log('# Section name')`** — a `#`-prefixed log line is a
  section heading: it becomes a navigation-outline entry in the diagram/combo view AND a visual
  section break in the flowchart. A rulebook of any size should carry a handful (e.g. `'# Base rate'`,
  `'# Discounts'`, `'# Total & routing'`); without them (or loops / large decisions) the outline is empty.
- **`calc.output` is a write** (`calc.output = {...}`), assigned once near the end. The first numeric leaf
  becomes the default tracked metric for `sweep`/`impact`, so put the headline number first.
- **Round money with `calc.round(x)`** — half away from zero, 2dp by default (`calc.round(x, dp)` for
  others). **Do not use `Math.round(x*100)/100`**: it is IEEE-754 arithmetic, so `1.005*100` is
  `100.49999999999999` and the penny is lost — and it is *intermittent* (`15.025` and `2.675` round
  correctly), so spot-checking two values reads as proof. Use `.toFixed(2)` only inside log strings,
  never for stored numbers.
- **Gate decisions on raw `input.x` comparisons, not derived copies.** The explorer attributes a boundary
  by *observing* a comparison that references an input path — a branch on a precomputed boolean or a
  derived aggregate can't be attributed, so a perfectly reachable arm may read `notreachable` (the
  false-positive note under `Rule.check` below). Branch on the input field itself; reach for `calc.levels`
  only when a *single* input drives a derived boundary.
- **Write floors and caps branch-free.** A minimum-premium rule written as
  `if (premium < lookup.minPremium) premium = lookup.minPremium` reads as a dead branch whenever the
  current rates can't actually go below the floor — an honest `notreachable` you then chase. The
  sanctioned idiom is a clamp plus a guarded invariant:
  `premium = Math.max(premium, lookup.minPremium);` and
  `calc.always('premium is at least the minimum', premium >= lookup.minPremium, {req: '…'})` —
  no dead arm, the invariant carries the promise into the RTM, and the clamp still enforces the floor if
  a later rate change makes it reachable. (Same shape for caps: `Math.min` + an invariant.)
- **Keep ONE stable output shape across every path.** On a declined/referred path emit the *same fields*
  with `null` where no value exists (`premium: null`) plus a `reason` string — never a differently-shaped
  object per path. Checks and the decision table compare like with like; the categorical routing itself
  belongs in `calc.outcome`, not in shape variance.

## The explainability hints — and WHEN to use each

All five are optional, cheap (like `log`), collected during a normal run, and read by the analysis engine.
They are how you make the black box legible to `Rule.check` and the decision table.

### `calc.label(text)` — name the business rule, right before a decision
Call it on the line *immediately before* a branch/decision. The nearest following branch site inherits the
label; it becomes the decision-table **column header** and the text in `Rule.check` findings. Label every
distinct business rule.
```js
calc.label('Volume discount');
lookup.volumeTiers.forEach(function (tier) {
    if (volumeDiscount === 0 && totalQty >= tier.minQty) volumeDiscount = tier.discount;
});
```

### `calc.req('<reqId>/<acId>')` — link a decision to a requirement criterion
The call goes **inside the `if`/`case` arm it describes**. An arm with authored behaviour and no call
reads `notclaimed` in `Rule.check` (REVIEW) — claim it or delete it; a body-less arm (an else-less `if`)
needs none, and a ternary carries none, so a decision you want claimed is an `if`. The criterion
goes **COVERED** only when a **run** emits the hit (`/api/eval` is a no-op — `Rule.cover` persists it).
```js
calc.label('Approval routing');
if (total > 50000) { calc.req('ORD-014/1'); calc.outcome('needs-approval'); }
else               { calc.req('ORD-014/2'); calc.outcome('auto-approved'); }
```
**The `<acId>` is the criterion's 1-based position** (`PL-001/1`) — one that names no real criterion
**lands nothing, with no warning**; confirm with `Requirement.get('<reqId>')`, and author the
requirement first if absent: **`Requirement.write('<name>', md)`**.

→ **`Skill.help('rule-authoring', 'requirements-linking')`** — the requirement markdown format in full,
the dotted-id insert rule, and that #1 trap end to end.

### `calc.outcome(value)` — the categorical result
The decision/disposition of the run (`'approved'` / `'declined'` / `'needs-approval'` / `'auto-approved'`),
distinct from the numeric `calc.output`. Last write wins. **Always set an outcome for any rulebook that
makes a decision** — it powers the decision table's outcome column and `check`'s *never-produced-outcome*
detection (an outcome you wrote in code but no input can reach is a bug it surfaces). **Call it with a
string literal at each decision arm** (`calc.outcome('declined')`), never through a variable — the
never-produced universe is statically scanned from `calc.outcome('…')` literals, so a variable-fed
outcome silently opts out of `notproduced` detection.

### `calc.always` / `calc.sometimes` / `calc.levels` — guarantees, situations, pinned boundaries

- **`calc.always(name, condition[, {req}])`** — a safety property that must hold for EVERY input in the
  declared domain. `Rule.check` grades it `COMPLETE` (proven over a closed domain) / `VIOLATED` (handing
  you the minimal IN-DOMAIN counterexample) / `HOLDS` (held across the deck — sampled, not proven) /
  `NOTCHECKED`. Declare **2–4 real ones** — domain truths, not tautologies. The optional
  `{req: '<reqId>/<acId>'}` **guards an acceptance criterion**, so a violated one reads FAILING in the
  matrix and blocks `Requirement.readiness()`.
- **A domain too big to walk is proven by certificate**: `Rule.check(name, {certify: true})` walks it
  once and stores the result, so every later `check`/`explore` grades `COMPLETE` off
  `universe.certified`. An in-domain authored `throw` is a refused region the claim subtracts
  (`domainRestricted: true`); any other runtime error still forfeits it.
- **`calc.sometimes(name, condition)`** — a situation some input must reach at least once. `REACHED`
  carries the minimal witness; `NOTREACHED` is a sampled gap; `NOTREACHABLE` (closed domain walked
  whole) is proven impossible — a guarded one blocks readiness.
- **`calc.levels(inputPath, [values])`** — pin the values that straddle a boundary living behind a
  *derived* computation, and only when a **single** input drives it.

```js
calc.always('discount never exceeds subtotal', discount <= subtotal);
calc.always('total at least the discounted amount', total >= discounted, {req: 'ORD-001/2'});
calc.sometimes('volume and tier discounts stack', volumeDiscount > 0 && tierDiscount > 0);
```

**Name each with a string literal, not an expression** (properties are pre-cataloged from the source, so
a computed name silently vanishes), and declare `always` properties **unconditionally** — one inside a
branch arm executes only on the rows that reach it and stays `HOLDS`.

→ **`Skill.help('rule-authoring', 'properties')`** — read it BEFORE you declare a guarantee: what
`COMPLETE` requires and what silently forfeits it, what counts as a branch site (a property condition
adds them), `calc.levels`' scope limit, and why `notderivable` is informational rather than a failure.

### Refusing input — `throw`, reject rows, and pinned expectations

**Keep the three rejections apart** — they have different fixes:

| meaning | how it is written | what it says |
|---|---|---|
| **schema-rejected** | a `scenarios.json` row with `_expect: 'schema-reject'` | the input is malformed — the gate must refuse it |
| **precondition-excluded** | a bare `throw '<reason>'` in `calc.js` | valid input, but the business cannot sell it |
| **business-denied** | an ordinary row whose `calc.outcome` is a denial | valid input, and the answer is "no" |

A reject row lives in the **same `scenarios.json`**; `calc.js` never runs it, and
`Rule.check(name).rejects` grades it `REFUSED` / `ACCEPTED` / `NOTCHECKED`. Its other half is
`accepts` — the saved rows the shape must *admit*. The **other** `_expect` — an *object*,
`{"output": {…}}` / `{"outcome": v}` — is the stored expectation `Rule.check(name).expectations` grades;
**derive that figure from the requirement**, never from what `Rule.execute` just returned (a copied one
agrees by construction and guards nothing).

→ **`Skill.help('rule-authoring', 'negative-scenarios')`** — read it BEFORE you write any of the three:
the feasibility contract a bare `throw` buys, why a built-in error subtype is read as a calc bug instead
of a domain rejection, why you must read the `why` on a refusal, and the mock-is-a-weaker-witness caveat.

## Writing `generator.js`

Declare the constrained universe with one `function generate(g){...}`. Every dimension you declare is a
dimension the explorer sweeps and `Rule.ranges` projects. Combinators (aliases in parens are equivalent):

| Call | Meaning |
|---|---|
| `g.enum(name, [...])` *(`g.pick`)* | sweep every member |
| `g.int(name, lo, hi[, [boundaries]])` | **whole numbers** over `[lo,hi]` — a fractional bound is refused. The bound is a MEMBERSHIP test, not a generation hint: rows outside it still run as robustness probes but never decide a verdict; outer endpoints tested, inner boundaries learned — **or pin them** with the optional 4th arg |
| `g.number(name, lo, hi[, [boundaries]])` *(`g.num`)* | **real-valued** over `[lo,hi]` — same membership rule. **This is the money axis: use it for any amount carrying pennies.** `g.int` is NOT an alias of it — declare `amount` with `g.int` and a row of `100.05` is out of domain, so it runs as a robustness probe and decides nothing (`Rule.check`'s `robustness.savedRows` is where it goes) |
| `g.bool(name)` *(`g.boolean`)* | false / true |
| `g.const(name, value)` *(`g.fixed`)* | fixed — present in every row, NOT a dimension |
| `g.string(name[, [samples]])` *(`g.text`)* | enumerates samples (defaults to one placeholder) |
| `g.object(name, v => {...})` *(`g.group`)* | nested object; declare its fields on `v` |
| `g.array(name, minCard, maxCard, el => {...})` *(`g.list`)* | an array: **cardinality `min..max` is itself a dimension** + per-element variation declared on `el` |

```js
function generate(g) {
    g.enum('customerTier', ['standard', 'silver', 'gold', 'platinum']);
    g.enum('region', ['US', 'EU', 'APAC']);
    g.bool('rush');
    g.array('items', 0, 3, function (el) {
        el.enum('category', ['hardware', 'software', 'service']);
        el.int('qty', 1, 100, [10, 50]);     // volume-tier boundaries pinned
        el.int('unitPrice', 10, 5000);
    });
}
```

Guidance:
- **Declare every input dimension.** A field you forget is a field the explorer can't vary.
- **Constrain numeric domains** to the valid business range (`g.int('driverAge', 16, 90)`, never `-1..∞`).
  The bound is a claim about what can occur, not a hint about what to explore: a saved row outside it is
  disclosed under `robustness.savedRows` and **stops deciding verdicts**. So narrowing an axis does not
  move that evidence onto the saved rows — it silences them, and the rows left outside say the rulebook
  now contradicts its own scenarios. An over-broad domain bloats the suite without adding coverage.
- **An incoherent declaration is refused at parse** — `lo > hi`, a fractional or non-finite `g.int` bound,
  a negative/non-integral/inverted `g.array` cardinality, an explicit boundary outside the declared
  bounds, or the same axis declared twice. A bound is a membership claim the whole verdict scopes to, so a
  typo'd one would silently switch the rulebook's verdict off — the engine fails it closed instead.
- **Pin known boundaries** with the 4th arg whenever a rate table or rule changes behavior at a specific
  value (`[25, 40, 65]`, `[10, 50]`). These are the values that flip an outcome.
- **Model arrays with `g.array`** so cardinality *and* per-element variation are real, swept dimensions —
  the clean answer to nested arrays. Include `0` in the range if "empty cart / no items" is a valid case.
- **v1 limitation:** element variation is **homogeneous** — a chosen element value applies to every element
  of the array (enough to fire each per-element branch arm; full per-element independence is a later step).
  Don't write rules that require two *different* elements in the same array to both hit distinct arms.

## The forward-engineering LOOP

Drive these JSAPI verbs over curl/MCP (`Rule.help()` prints the surface). The loop:

1. **Write** `calc.js` (+ `generator.js`) from the requirement and persist them:
   - **New rulebook from scratch** → `Rule.create(name, {schema, calc, generator?, scenarios?})` — creates
     the folder with all the files in one call (everything parse-validated before any write). This is the
     only verb that *creates*; the per-file writes below refuse to. Its undo is
     **`Rule.delete(name)`** — removes the whole book, irreversibly; use it on a probe or a throwaway.
   - **Existing rulebook** → `Rule.calc.update(rule, calcJs)`, `Rule.schema.update(rule, schemaJs)`, and
     **`Rule.generator.update(rule, generatorJs)`** (add an input domain to a rulebook that shipped
     without one). All parse-validated; `{ifMatch:<mtimes sha>}` guards against a stale edit → 409.
     A `generator.js` write also **runs** `generate(g)`, so an incoherent declaration
     (`g.int('amount', 0, 10000, [12000])` — a boundary outside its own domain) is refused with
     `GENERATOR_INVALID` and nothing lands on disk.

   Seed the rows that pin the cases the requirement names by example. **Pass an ARRAY to save them in
   one call** — `Rule.scenario.create(rule, [row, row, …])` → `{ids, mtime}`, one write, all-or-nothing
   (a bad row saves none). One row per call costs a turn per row.

   **A row is flat, and `_`-prefixed keys are the engine's.** Everything WITHOUT the prefix is business
   input and is sent to a system under test verbatim (`Rule.execute(...).input`); everything WITH it is
   metadata the engine owns and never puts on the wire. So a business field may safely be called `id`,
   `label` or `tags` — the prefix is what keeps the two apart:
   `{_id:'bulk-order', _label:'bulk order — discount applies', quantity:12, …}`
   - **`_label`** — the display name across the console and reports. Give every scenario one: without it the
     row reads `(unnamed)` and is indistinguishable from its siblings. Name the *case* (what it exercises),
     not the raw values — the equivalence class or boundary it pins ("at the 10-unit threshold", "below
     threshold").
   - **`_id`** — the stable key (minted as `sc-<6hex>` if you omit it). It is also the
     `rules:<rulebook>/<id>` coverage-item name, so a meaningful one is worth authoring and must not be
     renamed casually — `@cov=` references point at it.
2. **`Rule.ranges(name)`** → which input values matter: per-dimension equivalence classes + boundaries,
   plus **`notderivable`** (boundaries behind derived values — informational, see below; cover them with
   scenarios, and `calc.levels` only when one input drives the boundary). A free-text axis the calc tests
   with `==` `===` `!=` `!==` also carries an **`other`** class (`complement:true`) standing for every
   value the rulebook never names — its sample is a witness of that class, not a value your data must
   contain, and not necessarily a row in the deck. **`schema.js` decides whether an axis is free
   text, not `generator.js`:** a `'#string'` field keeps the class when sampled with
   `g.enum('code', ['BR','NT'])`. A local copy is fine (`var code = input.code` then
   `code === 'BR'`) provided that name is **assigned once** — reassign it and the comparison is
   attributed to nothing, so the axis reports empty `gates` and lands in `notderivable` instead.
3. **`Rule.check(name)`** → the health verdict. **`check.verdict` is the rollup** —
   `{status:'CLEAN'|'REVIEW'|'FINDINGS', findings, review, reasons[]}` — so branch on that rather than
   re-deciding by hand which of the arrays below count. The line between them is **declared vs
   inferred**: `FINDINGS` = something YOU asserted is unmet (a guarantee, a `sometimes`, a negative
   scenario, a declared outcome) — a broken promise; `REVIEW` = the arm axes, which the engine inferred
   from the code's shape and you never claimed anything about — a `|| default` fallback arm is dead
   because the code is correct, while a business rule reported dead is a bug. That adjudication is
   yours, which is why it is not `FINDINGS`. The detail,
   The **five axes that gate "clean"**: **`notused`** (reachable, but no saved scenario exercises it →
   add case data) · **`notreachable`** (nothing reaches it even with the generator → fix the rules; the
   finding names the input `axis` it turns on, or carries `attribution:'notderivable'`) ·
   **`deadBranches`** (a site stuck on ONE arm) · **`notproduced`** (a `calc.outcome` value no input
   yields) · **`properties`** (a violated `calc.always` with its minimal in-domain counterexample, or a
   `calc.sometimes` nothing reaches). Reported alongside but **never** deciding the verdict:
   `rejects`/`accepts`, `robustness`, `notderivable`.

   → **`Skill.help('rule-authoring', 'check-findings')`** — read it BEFORE you act on any finding: the
   three `notreachable` cases that are not a rules bug, what each `robustness` key discloses, and the two
   benign-dead-arm patterns that cost a cold run a rewrite it did not need.

   The verb tells you which lever to pull: add scenarios for `notused`, fix rules / widen the generator for
   `notreachable` — then re-run.
4. **`Rule.table(name)`** → the decision-table projection: rows grouped by outcome-key, columns kept only
   where they VARY, plus the coverage verdict (Rule-Coverage %). Read it to confirm the rules read the way
   the business intended. `{output:'markdown'}` renders it.
5. **`Rule.report(name)`** → render the self-contained `file://` HTML Analysis report (Coverage table ·
   Value Ranges · Rule Check · What-If). Honors `karate-theme`.
6. **`Rule.cover([name])`** → **close the requirements→rules→coverage loop, run-free**. Runs every saved
   scenario through the oracle and writes a unified `coverage-data.json` with two sources: **req** (the
   `calc.req` criteria the rules realize) and **rules** (each scenario). Both land **COVERED** — running the
   calc genuinely realizes them — carrying **`oracleOnly`**, the honest disclosure that nothing outside the
   rulebook checked them. Then `Requirement.matrix()` / `Coverage.summary()` / `Report.generate()` show them
   with a `rules only` chip (indigo), never an unqualified green; a `@req=`-tagged test, or a
   `Rule.execute(...).verify(ok)` stamp, clears it. Omit `name` to cover all rulebooks. This
   is the serve-native way to make the RTM reflect the rules — the interactive `Rule.run` emit is a no-op
   over `/api/eval`, so `Rule.cover` is what persists the coverage.
7. **`Rule.bless(name)`** → once the book reads right, **freeze it as the drift reference — before the
   next edit, never after**: `drift` compares against the baseline on disk, so one captured after a
   change has nothing to compare and the change itself silently becomes the reference. It writes
   `.baseline/verification.json`: every saved scenario's output, outcome and branch path, every invariant's
   status, every reject row's verdict. Bless again — deliberately — each time you accept a behaviour change.
   Then **`Rule.drift(name)`** is the guard on every later edit: it re-runs everything and reports
   `invariantRegressions` (a guarantee that held is now VIOLATED, or stopped being evaluated) ·
   `outputDrift` (a saved scenario's output changed **with no edit to that scenario**, word-diffed) ·
   `outcomeDrift` (its `calc.outcome` or branch path moved) · `rejectDrift` (a negative scenario's verdict
   moved — `REFUSED`→`ACCEPTED` means the shape was loosened). `changed` is true only for those four, so
   gate CI on it; your own edits ride `added`/`removed`/`edited`/`invariantImprovements` and never set it.
   This is the failure the suite cannot catch, because the suite asks the rules what to expect: **the tests
   stay green and the meaning moves.** Read a finding as either reading — the rules are wrong, or the
   expectation is — fix the reviewed artifact, then re-bless.

> **The rulebook is then the oracle for the tests, too.** Once these rules exist, a `.feature` or `.js`
> check must never pin a premium/total/decision the calc computes — it asserts the live system against
> `Rule.execute(name, row)` and stamps `check.verify(ok, note)` (which is what clears `oracleOnly` and attaches
> the criteria). Pull `Skill.help('api')` → "the RULE is the oracle" for the worked pattern.

Supporting verbs: **`Rule.execute(name, row)`** runs the calc against an ad-hoc row (the raw oracle call →
`{output, outcome, audit, durationMs}` — `outcome` present when the rules set one via `calc.outcome`); **`Rule.run(name, scenarioId[, opts])`** runs a *stored* row + emits coverage (the `calc.req`
hits land here); **`Rule.sweep(name, {dim, output})`** is single-factor sensitivity with outcome-flip marks
(omit `dim` for the tornado); **`Rule.impact(name, {path, set|mul|add|pct})`** perturbs a `lookup` value and
re-runs the saved scenarios (the actuary rate-change view) — `path` walks into `Rule.load(name).lookups`
one dot-separated step at a time, and since a rate table is usually an ARRAY, an index is a step of its own:
`Rule.impact('auto-premium', {path:'ageBands.0.factor', pct:7})` (`ageBands[0].factor` is accepted as the
same path). Use `sweep`/`impact` to sanity-check that a rate change moves the book the way you expect.

Iterate 2→3 until `check` is clean — **read `check.verdict`**, which rolls this up for you:
`FINDINGS` (a defect the engine is sure of — a violated guarantee, an accepted reject row, an outcome
nothing produces), `REVIEW` (arm findings only you can adjudicate — the benign-dead-arm note below is
exactly this case), `CLEAN`. Its `reasons[]` names each one. `notderivable` and `robustness` count toward
nothing. Then check the `table` reads correctly.

## Common mistakes to avoid

- **Wall-clock / randomness** — any `Date`/`Math.random` breaks byte-stability. Take time as an input field.
- **Timezone-fragile date math** (see the determinism law) — don't count spans from raw millisecond diffs;
  count whole calendar days on a UTC baseline from each date's local Y/M/D:
  ```js
  const dayNumber = d => Math.floor(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / 86400000);
  const days = dayNumber(endDate) - dayNumber(startDate);   // DST-immune, timezone-independent
  ```
- **Unreachable branches** — `if (x > MAX)` when `MAX` is the exact maximum `x` can reach never fires;
  `Rule.check` reports it as a dead branch. Use `>=` at the true boundary, or remove the rule.
- **Forgetting `calc.outcome`** for a decision rulebook → no outcome column, no never-produced-outcome
  detection.
- **Forgetting `calc.req`** → the run exercises the rules but no coverage lands in the RTM,
  the loop doesn't close, and `Rule.check` reads REVIEW (`notclaimed`).
- **Over-broad numeric domains** in the generator → suite bloat with no extra coverage. Constrain to the
  business range and pin the boundaries that matter.
- **Treating `notderivable` as a failure to drive to empty** — it's informational; cover derived boundaries
  with scenarios. Reach for `calc.levels` only when a *single* input drives the boundary.
- **Inlining rates** instead of a top `const lookup` → not editable via the console / `impact`.
- **Multiple `calc.output` writes or no write** → write exactly one object, headline number first.

## Reference rulebooks

Call **`Rule.list()`** to see the rulebooks the server already has, then read their files via the console.
The seeded worked examples:

- **`auto-premium`** — simple, schema-only (no generator), lookup-table boundaries, money rounding.
- **`order-pricing`** — generator + `g.array` of line items, `calc.label`/`calc.outcome`/`calc.always`/
  `calc.sometimes`, `calc.req` blocks, and the deliberately-unreachable "Discount cap" rule (the worked
  `check` finding — including its never-reached `sometimes` reporting NOT CHECKED).

## Cross-skill links

- `Skill.help('requirements')` — author the requirement markdown whose criteria
  `calc.req` links back to; the readiness scorecard.
- `Skill.help('coverage')` — the unified graph the `calc.req` hits land in; how
  requirement coverage reads. Your `Rule.ranges` axes also become the **API-coverage dimension universe**
  (`Skill.help('coverage', 'dimensions')`) — the rulebook as the adequacy
  oracle over an endpoint's input variation + required combinations.

## References

- `io.karatelabs.ext.rules.*` — `Rule` (the JSAPI front door, with a `help()` verb), `RuleRunner` / `Calc` /
  `CalcCore` (the oracle + the `calc.*` hints), `RulePathExplorer` / `RuleAnalysis` (ranges + check),
  `RuleReport` / `RuleTable` (the decision table + report).
- `io.karatelabs.ext.coverage.*` — `CoverageEmit` (`req` hits) + `RequirementsCoverageSource` (the join).
