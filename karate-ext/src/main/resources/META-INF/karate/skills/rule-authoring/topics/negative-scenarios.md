# Refusing input — `throw`, reject rows, and pinned expectations

> Pull this before you write a `throw`, a `_expect: 'schema-reject'` row, or a stored `_expect` figure —
> and when reading `Rule.check`'s `rejects` / `accepts` / `expectations`. Back to the loop:
> `Skill.help('rule-authoring')`.

### Rejecting an impossible / invalid input — `throw`

When a combination of inputs **cannot occur** in the business (a rule of the domain forbids it, not just a
bad value), reject it with a plain `throw` at the top of `execute`, before pricing. This is also how you
answer a counterexample that is not a real input: it scopes every guarantee, visibly and with a reason,
where a narrowed `generator.js` scopes them silently.

```javascript
const execute = function (calc) {
    const input = calc.input;
    // a domain precondition — the silver tier is only sold in the US, so any other region is impossible
    if (input.customerTier === 'silver' && input.region !== 'US') {
        throw 'silver tier is sold only in the US';      // a deliberate rejection
    }
    // … normal pricing …
};
```

**Why it matters — the feasibility contract.** A deliberate `throw` is how a rulebook declares an
**infeasible** input, and the coverage cross-projection uses it as an oracle: it runs `calc.js` over each
required `customerTier × region` combination and **drops the cells you `throw` on** from the denominator, so
the "Rule Coverage over combinations" % measures only combinations that can actually happen (it shows
`N infeasible pruned`). See the `coverage` skill's `dimensions` topic.

**Intent vs. accident — use the right signal:**
- A **deliberate rejection** is a *bare* `throw 'reason'` or `throw new Error('reason')`. Only these mark a
  cell infeasible (and surface as `RUN_FAILED` to `Rule.execute`).
- A **built-in error subtype** — `throw new TypeError(...)`, a `ReferenceError` from a typo'd variable, a
  division/`null` fault, a timeout — is read as a **calc bug**, *not* a domain rejection: it is **not**
  pruned (it would mask the defect) and is logged. So don't reach for `throw new TypeError()` to express a
  business precondition — use a plain `throw '<reason>'`. Keep the reason human-readable; it is the "why"
  for the pruned cell.

### Proving bad input is refused — the negative scenario

A `throw` says a combination **cannot occur in the business**. A **negative scenario** says something
different: this input is **malformed**, and `schema.js` must refuse it at the gate. Write it as an ordinary
row with one extra key:

```json
{
  "_id": "qty-not-a-number",
  "_label": "a non-numeric quantity must be refused",
  "_expect": "schema-reject",
  "customerTier": "gold",
  "items": [ { "sku": "HW-1", "qty": "five", "unitPrice": 200 } ]
}
```

- It lives in the **same `scenarios.json`** — one artifact, no second file.
- It is **not a scenario**: `calc.js` never runs it, `Rule.run` refuses it, and `Rule.load(name)` returns
  it under `rejects`, not `scenarios`.
- **Add `_req` to name the criteria it answers** — one criterion id or an array (`"_req": "ORD-001/2"`).
  `Rule.cover` then projects a `REFUSED` row onto each: the criterion reads COVERED with `refusalOnly`
  (`ACCEPTED` → FAILING, `NOTCHECKED` → NOTRUN). `_req` is legal only here — on an ordinary row
  `calc.req` is the linkage — and a row without it is counted under `Rule.cover(...).rejects.unlinked`.
- `Rule.check(name).rejects` grades it: `[{_id, _label, status, pass, why}]`, with three statuses —
  **`REFUSED`** (the claim holds; `why` names what the shape objected to), **`ACCEPTED`** (the failure —
  the shape is too loose, tighten `schema.js`), and **`NOTCHECKED`** (the row carries no business input,
  so its mismatch is arithmetic, not evidence).
- **Read the `why`.** A passing verdict proves that *this exact row* is refused — not that the gate
  refuses bad input in general. A row can be refused for a reason you did not intend: leave out a required
  field and it is refused for *that*, while the malformed value you meant to pin is never reached. Write
  one row per defect, otherwise valid.
- **`Rule.check(name).accepts` is the other half of the gate** — the ordinary saved rows the shape must
  *admit*: `{status, checked, incomplete, refused[]}`, reading `ACCEPTED` / `REFUSED` (a saved row its own
  shape rejects — that row or the shape is wrong) / `NOTCHECKED` (no eligible row; a row omitting a
  declared key is counted under `incomplete` and skipped). **A green roster beside a `NOTCHECKED`
  `accepts` is a shape that refuses everything**, and every claim in it holds for that reason alone.
- **Top-level** engine keys (anything starting with `_`) are ignored on both sides of the match. A
  `_`-prefixed key nested inside an object or array element is NOT — it would refuse the row on its own.
- A **typo'd marker fails the load**, naming the row. `'schema-reject'` is the only string value.
- `_expect` is also used with an **object** value (`{"output": {…}}` / `{"outcome": v}`) — that is the
  stored expectation the mutation self-run grades. Same key, told apart by type: both answer "what should
  happen if I submit this row?"
  **`Rule.check(name).expectations` grades it** → `{status, checked, failed[]}`, `MATCHED` /
  `MISMATCHED` (the rules contradict the figure — one of the two is wrong) / `NOTCHECKED` (nothing
  pinned; optional, never a defect). A mismatch is a finding; it does not reach `readiness()`.
  It is the only **per-row output** oracle — a figure meant to come from the requirement rather than
  from `calc.js`, so the row can be red the day you write it (a `{req}`-linked `calc.always` anchors
  externally too, at the level of a rule). So pin the rows where a competent implementer could
  reasonably get a different number — a sign change, a band boundary; elsewhere it is duplication you
  maintain by hand.
  **Derive that figure from the requirement** — by hand, or from the customer's answer key. One copied
  from what `Rule.execute` just returned pins the calc to itself: it agrees by construction and
  survives every rewrite of the logic it was meant to guard. Can't derive it independently? Say so and
  leave the row without an `_expect`.

**Keep the three rejections apart** — they have different fixes:

| meaning | how it is written | what it says |
|---|---|---|
| **schema-rejected** | `_expect: 'schema-reject'` | the input is malformed — the gate must refuse it |
| **precondition-excluded** | a bare `throw` in `calc.js` | valid input, but the business cannot sell it |
| **business-denied** | an ordinary row whose `calc.outcome` is a denial | valid input, and the answer is "no" |

A live SUT may additionally assert the declared 4xx. **A rule-backed mock is a weaker witness than it
looks:** `Http.mock` gates a request only with `validate:true` (off by default), and it gates against the
**OpenAPI operation's request schema — not this rulebook's `schema.js`**. The two can disagree: if
`schema.js` types `qty` as `#number` but the OpenAPI spec leaves it untyped, `Rule.check` reads REFUSED
while the mock answers 200. Assert the 4xx against a real SUT, or make the OpenAPI schema say the same
thing first.

