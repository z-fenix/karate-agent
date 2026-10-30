# Guarantees and situations — `calc.always`, `calc.sometimes`, `calc.levels`

> Pull this before you declare a guarantee, when `Rule.check` reports a `properties` finding, or when a
> book reads `HOLDS` and you wanted `COMPLETE`. Back to the loop: `Skill.help('rule-authoring')`.

### `calc.always(name, condition[, {req}])` — safety properties that must hold for ALL inputs in the declared domain
Property-based testing (the invariant polarity). The engine evaluates the condition on every generated +
stored input, but only rows INSIDE the declared domain decide the verdict — so a counterexample is
always an input the rulebook itself says can occur, surfaced as the **minimal failing input** (shrunk).
Evaluations on out-of-domain probes ride along as `robustnessChecks`, and what they found lands under
`check`'s `robustness` block — findings, never verdicts. Declare **2–4 real always-properties** — domain
truths, not tautologies.

**When a counterexample is an input your business cannot actually produce** — an incoherent combination
of fields, say — declare that where a reader can see it: `throw '<reason>'` at the top of `execute`
(`Skill.help('rule-authoring','negative-scenarios')`) says the combination is impossible, takes it out of every verdict, and carries the why for the
human reviewing the rulebook. Narrowing `generator.js` instead removes the same row and states nothing.
```js
calc.always('total is never negative', total >= 0);
calc.always('discount never exceeds subtotal', discount <= subtotal);
calc.always('total at least the discounted amount', total >= discounted, {req: 'ORD-001/2'});
```
The optional third arg `{req: '<reqId>/<acId>'}` **guards an acceptance criterion** with the invariant —
the release-gate join: `Rule.cover` projects the verdict onto that criterion, so a VIOLATED guarded
invariant reads FAILING in the requirements matrix and **blocks `Requirement.readiness()`** with the
counterexample attached, while a held one adds positive (`oracleOnly`) evidence. Guard the invariants
that state what a requirement promises. Rules (enforced, `Rule.cover` refuses otherwise): the `{req}` id
must be a **string literal** naming a real criterion, and property **names must be unique** per rulebook.
`Rule.check` grades each always-property `COMPLETE` (**proven for this input domain** — every axis of
the rulebook is closed: an enum, a boolean, or a bounded `g.int`; no arrays, no free text, `notderivable`
empty — and check walked the ENTIRE raw-value product, size-guarded at 50k rows, `{maxProduct}`
overrides; **over that cap, `Rule.check(name, {certify: true})` proves it by certificate instead** —
one stored walk of the declared domain, read by every later check while the `calc.js`/`schema.js`
hashes and the evaluator version match, reported under `universe.certified` with the per-property `certified {trueRows,
falseRows, unknownRows, executedRows}`, the feasible branch signatures under `signatures`, and a typed
`code` where the abstraction cannot state the book) / `VIOLATED` / `HOLDS` (held across the deck's in-domain rows — sampled, not proven) /
`NOTCHECKED` (`checks: 0` — never executed, or evaluated only outside the declared domain, its
`robustnessChecks` disclosing them). Every always-property is ALSO hunted at the corners of its declared
box — counted apart as `searched`, plus `searchViolations` where it broke there — so a `VIOLATED` beside
`violations: 0` is a real in-domain counterexample the coverage deck never generated; read it as any
other. **That hunt is capped**: `check.search` reports `{rows, corners, maxSearch, truncated}`, and
`truncated:true` means a held property was sampled over `rows` of `corners` and no further — raise it
with `Rule.check(name, {maxSearch: n})` before you read `HOLDS` as strong. The `universe` block names
every cause that keeps a book at HOLDS (an
open axis, an array, `notderivable`, `overcap` with the count, an IN-DOMAIN throwing row — a guard
tripping on an impossible input costs nothing — or `walkmembership`), and `Rule.ranges` marks each axis
`exhaustive:true+size` or `cause`. Want the proof rung? Keep the book's decision core finite, its
comparisons directly on `input.*` paths — a derived-value comparison lands in `notderivable` and
honestly forfeits COMPLETE for the whole book — and declare properties **unconditionally** (at the
function's end, not inside a branch arm): a property inside an arm executes on only the rows that
reach it and stays HOLDS, its `walkChecks` disclosing the gap.

### `calc.sometimes(name, condition)` — situations the explored space must reach at least once
The reachability polarity: an interesting **situation** — richer than a line or a single branch arm —
that some input must make true (`sometimes` = "this genuinely happens in the domain"). `status`:
`REACHED` (carries the **minimal witness input**) / `NOTREACHED` (no explored in-domain input made it
true — sampled; add data, or check the rules) / `NOTREACHABLE` (the closed domain was walked whole and
none did — proven impossible; fix the rules or the property, never the data) / `NOTCHECKED`. Use it for
cross-cutting
conditions no single decision arm captures:
```js
calc.sometimes('volume and tier discounts stack', volumeDiscount > 0 && tierDiscount > 0);
calc.sometimes('a claim is partially paid after the deductible', paid > 0 && paid < billed);
```
`calc.sometimes` takes the same optional `{req}` arg — a **reached** situation adds positive evidence on
the criterion it guards; `NOTREACHABLE` reads FAILING and blocks `Requirement.readiness()`
(`notreachable[]` on the blocker); `NOTREACHED` stays an honest gap, never a verdict.

**Name with a string literal, not an expression** — properties are statically pre-cataloged from the
source, so one on a never-reached arm still reports (as NOT CHECKED, `checks: 0`) instead of silently
vanishing; a computed name defeats that.

> **What counts as a branch site:** every `if` / ternary / `switch` case / `&&` / `||` / `??` in
> `calc.js` — *wherever it appears*, including inside a `calc.always`/`calc.sometimes` condition or a
> function argument. So a property condition written `a && b` adds a site whose both outcomes want
> covering, and a `x || default` inside one shows up in the universe like any other fallback (the
> benign-dead-arm note under `Rule.check` applies). Keep property conditions simple; don't over-provision
> scenarios just to feed short-circuits inside property expressions.

### `calc.levels(inputPath, [values])` — pin a boundary that ONE input drives through a derivation
When a boundary lives **behind a derived computation** the black-box explorer can't attribute (e.g. you
branch on `subtotal = qty * unitPrice`, not on a raw input field), that boundary lands in `ranges`
**`notderivable`**. When **a single input** drives that derived value, declare the exact values that
straddle the boundary so coverage becomes deterministic:
```js
// the rule trips at a derived total of 50000, driven by one input — pin the unitPrice values around it
calc.levels('items[0].unitPrice', [600, 625, 650]);
```
**Scope & limit (important):** `calc.levels` only helps when ONE input crosses the boundary. If the derived
value is a **product/aggregate of several inputs** (e.g. `premium = base × typeFactor × coverageFactor`,
where no single input's value straddles the threshold), there is no input to pin — `calc.levels` **silently
has no effect** there. In that case **don't fight `notderivable`** — see the next note — just add explicit
scenarios that land on each side of the boundary.

> **`notderivable` is INFORMATIONAL, not a health failure.** It is the honest list of boundaries the
> black-box explorer couldn't attribute to a raw input — *any* rulebook that branches on a derived
> aggregate will populate it (the reference `order-pricing` ships a large `notderivable` over its
> `total > 50000` band and is still the "good" example). It does **not** make `check` fail. The health
> verdict is governed only by **`notused` / `notreachable` / `deadBranches` / `notproduced` /
> `properties`**. Close a derived boundary by **covering it with scenarios** (and `calc.levels` *only* when
> one input drives it); never chase `notderivable` to empty.

