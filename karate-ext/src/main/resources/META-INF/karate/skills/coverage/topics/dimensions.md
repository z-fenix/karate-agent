# Coverage dimensions — variation depth + required combinations

> **What it adds:** path+method coverage answers *"was the endpoint called?"* — binary. **Dimensions**
> answer *"was it exercised across the input variations that matter, and the combinations that matter?"* —
> a graded %, with the **rulebook as the adequacy oracle**. This is the "out-model the fuzzer" surface:
> the rules already know which inputs change the outcome, so they define what *should* have been tested.

## The two halves (keep distinct)

- **Input-space dimension** = a rulebook's input **axis + its value universe** — the enum members,
  numeric boundaries, array cardinality the calc distinguishes. Projected by `Rule.ranges` (the
  `rule-authoring` skill builds these). "Which inputs vary and what values matter."
- **Coverage dimension** = the **observed coordinate on a hit** (`attrs.dim`, e.g.
  `{customerTier:'gold', region:'EU', response:201}`) — "how deeply was this op exercised."

They connect: the rulebook **supplies the universe** the observed coordinates are graded against. The
hit records the point; the rule is the oracle.

## Setup — a `dimensions.js` file, wired with `cov.dimensions`

In `karate-boot.js`, point coverage at a dimensions file:

```js
const cov = boot.ext('coverage');
cov.openapi = 'openapi.yaml';       // path+method coverage (the items)
cov.rules = 'rulebooks';         // the rulebooks (the axis universes)
cov.dimensions = 'config/dimensions.js';   // the dimension handlers (this file)
```

`dimensions.js` evaluates to an object literal mapping a **path pattern** → a handler. Two forms:

```js
({
  // (a) bare function — open axes, observed values only (no universe to grade against)
  '/payments': function(request, response) {
    return { paymentMethod: request.body.method, response: response.status };
  },

  // (a') the THIRD argument `scenario` — derive an OFF-WIRE coordinate centrally (AOP), no per-scenario
  //      @dim tags: scenario.tag('provider') reads a @provider=stripe tag; .has('x'), .name, .feature, .tags
  '/checkout': function(request, response, scenario) {
    return { provider: scenario.tag('provider'), response: response.status };
  },

  // (b) object form — bind the path's items to a rulebook + declare a required combination
  '/orders': {
    rulebook: 'order-pricing',                                  // its Rule.ranges axes = the closed universe
    extract: function(request, response) {                      // the coordinate emitter (plain JS over the
      return { customerTier: request.body.customerTier,         //   stored request/response maps — no rehydration)
               region:       request.body.region,
               response:     response.status };
    },
    cross: ['customerTier', 'region']                           // the cross/point required combination
  }
})
```

- **`extract`** runs at **report-time over the stored `http-exchange`** — so it is **retroactive**
  (add/edit an axis and re-derive over the same run, no re-execution). `request`/`response` are the plain
  JSON-shaped maps (`request.body.x`, `response.status`).
- **The third argument `scenario`** is the executing scenario (the AOP join-point's "who"): `scenario.tag('k')`
  → the value of a `@k=v` tag (else null), `scenario.has('smoke')`, `scenario.name`, `scenario.feature`,
  `scenario.tags`. Use it to derive an **off-wire** coordinate **centrally** — one rule reads `@provider=stripe`
  off every `/checkout` scenario — instead of scattering `@dim=provider:…` on each. (For raw traffic with no
  scenario, `scenario.tags` is empty, so null-guard what you read.) This is the scalable alternative to
  per-scenario `@dim` tags whenever the off-wire value follows a tag/name convention.
- **The key is a path pattern, matched like the OpenAPI spec paths** (templated segments allowed —
  `/orders/{id}`). It is **path-only, not method-scoped**: one handler fires for *every* method on that
  path. When a path has several operations (e.g. `GET` + `POST /orders`), branch inside `extract` on
  `request.method` (return different coordinates, or `{}` to skip a method).
- **`rulebook`** makes each enum/boolean/number axis of that rulebook a **closed** axis with a graded
  universe: an **enum** axis grades against its member set, a **boolean** axis against `{true, false}` (a
  2-value universe — so testing only `rush:false` reads `0.5`), a **numeric** axis by **boundary-value
  partition** (`< k`/`= k`/…), not raw value.
- **`cross`** names the axes whose **combinations** are required — the rule-as-oracle projection.

### Or skip `dimensions.js` — declare a coordinate on the `Scenario` with `@dim=axis:value`

A second, **rules-free and extractor-free** path: tag a scenario with `@dim=axis:value` and that coordinate
is merged onto every hit the scenario produces — no `dimensions.js`, no rulebook needed.

```gherkin
@dim=paymentMethod:card @dim=region:US
Scenario: a US card payment
  Given path '/payments'
  ...
```

- One `axis:value` per tag (the value is everything after the **first** `:`, so `@dim=response:201`
  works); declare several axes with several tags. Kept as a **string** (matched by `String.valueOf`).
- It **composes** with `dimensions.js`: if both set the same axis, the `extract` value (read off the actual
  exchange) wins; tag-only axes are preserved. Use tags for a coordinate that isn't in the request/response
  (a semantic label like `@dim=path:happy`), `dimensions.js` for values you can read off the wire.
- **Scale tip:** if the off-wire value follows a *convention* (a `@provider=stripe` category tag, a naming
  pattern), prefer a **central** `dimensions.js` extractor reading the `scenario` third arg
  (`scenario.tag('provider')`) over a per-scenario `@dim=provider:…` — one rule covers all matching traffic,
  no scatter. Reach for `@dim` for one-off coordinates or a quick start before you write the extractor.
- Like any coordinate, a tag value is **graded only if the axis has a universe** (a `dimensions.js` `rulebook`
  binding or the spec `response` axis); otherwise it is an open axis — enumerated, not graded.

## Read it — `Coverage.dimensions([itemId])`

The `itemId` is the **namespaced** item id exactly as `Coverage.gaps()` / `Coverage.items()` emit it —
`grpc:HelloService/Hello`, `openapi:createOrder`, `kafka:orders` (`source:operationId`). No arg = every
item. A **bare** operation id (`HelloService/Hello`) is accepted when it resolves to exactly one item; an
unknown or ambiguous id returns a did-you-mean over the known ids — it never returns a silent empty result
(so a wrong id can't masquerade as "no dimensions"). Discover ids from `Coverage.gaps()`'s `id` field.

Each item yields a **marginal** grade per axis + (if a `cross` is declared) a **cross** block:

```jsonc
{ "id": "openapi:createOrder", "points": 3,
  "axes": {
    "customerTier": { "coveredProportion": 0.75, "seen": ["standard","gold","platinum"], "gaps": ["silver"] },
    "region":       { "coveredProportion": 1.0,  "seen": ["US","EU","APAC"], "gaps": [] }
  },
  "cross": { "axes": ["customerTier","region"], "required": 12, "covered": 3,
             "coveredProportion": 0.25, "pruned": 0,
             "gaps": [ {"customerTier":"silver","region":"US"}, … ] } }   // the untested combinations
```

- **`coveredProportion`** ∈ [0,1] per axis (`-1` = open/not gradeable). `Report.generate()` renders these
  as bars + the cross gap-cells in the coverage report's per-item drill-down.
- **`cross.coveredProportion`** is the headline "Rule Coverage over the dimension surface" — *region 100%,
  tiers 75%, but only 3 of 12 combinations*. The `gaps` are a ready-made worklist.

## Generate the deck — `Coverage.coveringArray([itemId], [strength])`

The `cross.gaps` tell you *what is untested*; the covering array tells you the **minimal set of cells to
run next** to reach a strength target. For each `cross`-bound item it returns a strength-`t` covering array
(default `t=2`, **pairwise** — the NIST interaction rule: ≤6-way catches ~all faults):

```js
Coverage.coveringArray('openapi:createOrder')          // pairwise (t=2) by default
Coverage.coveringArray('openapi:createOrder', 3)       // t=3 (3-way); t≥|axes| = the full cross-product
// → { items: [ { id, strength: 2, axes: ["customerTier","region","rush"],
//               rows: [ {customerTier:"silver",region:"US",rush:true}, … ],  // the cells to test next
//               size: 12, targets: 26, seedCovered: 3, residual: 0, exhaustive: false } ] }
```

- **`rows`** is the deck — each row a concrete test cell. `size` rows reach strength-`t` adequacy where the
  exhaustive cross-product would need many more (pairwise over 4×3×2 = **12** rows, not 24).
- **Constraint- and seed-aware:** an infeasible cell (a `cross.pruned` combination) is **never proposed**,
  and a combination your traffic already exercised is **not re-targeted** — the deck fills only the gap
  (`seedCovered` = how many target tuples the observed coordinates already met).
- **Deterministic** — same graph → same deck (a greedy over the feasible cross-product, no randomness), so
  it is safe to diff/commit as a test plan.
- **Strength follows authored criticality** (*one axis, more leverage*). Declare `criticality` on the
  cross binding (`{ '/orders': { rulebook, extract, cross, criticality: 'high' } }`) and the default policy
  picks `t` by it: **high ⇒ 3-way, else pairwise**. The *same* `@criticality` that drives governance posture
  (lint strictness, coverage SLO) now also drives combinatorial strength — you set it once. Override per call:
  a number (`coveringArray(id, 3)` — same `t` for all) or a policy object (`coveringArray(id, {high:3, default:2})`).

## The workflow (forward-engineer an API test-adequacy target)

1. **Author the rulebook** for the endpoint's input shape + logic (`rule-authoring` skill) — its axes
   become the universe.
2. **Bind** the path to the rulebook in `dimensions.js`, with an `extract` that pulls the axes you care about
   off the request/response.
3. **Declare a `cross`** for the combinations that must be tested together.
4. **Run or replay** traffic (tests, recorded prod, an autonomous run — any stored traffic populates it).
5. **Read `Coverage.dimensions()`**, close the `gaps` (marginal + cross) with more cases, re-derive — or
   ask `Coverage.coveringArray()` for the minimal pairwise deck of cells to run next.

## Gotchas

- **The extractor must emit every axis you want graded.** A rule-bound axis the `extract` never returns
  shows `coveredProportion: 0` with all values missing — that is the **adequacy signal** ("the rules say
  `quantity` matters and your tests never varied it"), *not* a bug. Add it to `extract` (and your test
  data) to close it.
- **`cross` over an open axis is ungradeable** (`coveredProportion: -1`, the axis listed under `open`) —
  cross axes must be rule-bound or the spec `response` axis.
- **The cross-product is feasibility-pruned** — when the `cross` axes are enum/boolean and rule-bound, the
  rulebook's `calc.js` is run over each cell and the cells the rules **reject** are dropped from `required`
  (`cross.pruned` counts them), so the % is not diluted by impossible combinations. Express an impossible
  combination as a plain `throw` at the top of `execute` (e.g.
  `if (input.tier === 'silver' && input.region !== 'US') throw 'silver is US-only';`). **Only a deliberate
  `throw '<reason>'` / `throw new Error(...)` prunes** — a built-in fault (`TypeError`/`ReferenceError`/a
  timeout) is treated as a *calc bug*, kept in `required`, and logged, so a defect can't masquerade as
  "impossible" (the `rule-authoring` skill has the full contract). A numeric or open cross axis is **not**
  pruned (representative-witness synthesis is a follow-on) — still choose `cross` axes whose combinations
  are meaningful.
- **Dimensions are optional + additive** — no `dimensions.js` means you still get path+method + requirement
  coverage. It is purely the depth layer.
- **Suppress a noisy axis with `ignore`** — a contract-derived axis the suite never exercises (an optional field,
  a debug flag) reads as a permanent gap. Drop it with a standalone `dimensions.js` entry — no rulebook/extract:
  `({ '/orders': { ignore: ['note'] } })`. The key is an OpenAPI **path** pattern; for a **non-path protocol**
  (gRPC/Kafka items carry no URL path) key it on the item id — bare (`'HelloService/Hello'`) or namespaced
  (`'grpc:HelloService/Hello'`). The axis vanishes from the grade entirely (universe + observations).

## References

- `io.karatelabs.ext.coverage.dim.*` (`DimensionHandlers` / `DimensionReplay` / `DimensionUniverse`),
  `Coverage.dimensions()` (`Coverage.help()`).
- The input-space side: `Skill.help('rule-authoring')` (`Rule.ranges`).
