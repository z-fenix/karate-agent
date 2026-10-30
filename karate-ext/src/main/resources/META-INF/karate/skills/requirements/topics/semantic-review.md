# Requirements — semantic review (tier 3)

> The third lint tier. `Requirement.lint()` covers tiers 1–2 **deterministically** (structural integrity + a lexical smell dictionary, no model). This tier is **judgment** — it ships as *this skill*, run by you, the driving LLM, **never** as a method that calls a model. Run `Requirement.lint()` first; apply these checks to what survives.

## Why this is a skill, not a tool

A deterministic check can be golden-tested and **cannot be gamed by the author who wrote it** — that is the primary reward-hacking defense, and it is why the binary tiers live in code. But "is this requirement *actually* verifiable, or does it just *look* like a spec?" needs reading comprehension. That residue is yours. Emit your findings in the **same shape** `lint()` uses so they compose into one report:

```json
{ "id": "ORD-004", "tier": "semantic", "rule": "not-binary-evaluable",
  "severity": "warn", "message": "...", "suggestion": "..." }
```

Use `severity` `error` (the requirement cannot be verified as written), `warn` (a real ambiguity), `info` (a nudge). Always include a concrete `suggestion` — the rewrite, not just the complaint.

## The five checks

### 1. Binary-eval convertibility  (`rule: not-binary-evaluable`)
Could a test assert **pass/fail** on this acceptance line *without a human judging it*? Words like *fast, robust, user-friendly, intuitive, scalable, reasonable, appropriate* have no binary. Demand a measurable threshold or an observable outcome.

- ❌ "The system shall respond quickly."
- ✅ "WHEN a price is requested THE SYSTEM SHALL respond within 200 ms at p95."
- Suggestion pattern: replace the adjective with the number/observable that would make a test green-or-red.

### 2. Smuggled-how  (`rule: smuggled-how`)
The lexical tier catches a fixed token dictionary (`Redis`, `gRPC`, `microservice`). You catch the **paraphrased** how — an implementation choice dressed as a goal. A requirement states the **outcome**; *how* belongs in the design.

- ❌ "The system shall store the session in an in-memory cache keyed by token."
- ✅ "The system shall keep a user signed in across requests until they log out or the session expires."
- Ask: would this requirement still be true if the team chose a totally different mechanism? If naming the mechanism is load-bearing, it's smuggled-how.

### 3. The two-implementations test  (`rule: underspecified`)
Could two competent engineers build **materially different** systems that *both* satisfy this requirement, where a user would notice the difference? Then it is underspecified — pin the missing decision.

- ❌ "The system shall handle invalid input gracefully." (one returns 400, another silently drops it — both "graceful")
- ✅ "IF a required field is missing THEN THE SYSTEM SHALL return 400 with a body naming the missing field."

### 4. Goal, not task  (`rule: task-not-goal`)
Is this a durable **business capability** or a transient implementation **task**? Tasks ("add a Submit button", "create the orders table") belong in an issue tracker, not the requirements catalog — they go stale and can't be meaningfully "covered".

- ❌ "Add a cancel button to the order page."
- ✅ "The system shall let a customer cancel an order while it is still pending."

### 5. Constraint measurability  (`rule: unmeasurable-constraint`)
Each `**Constraints:**` (NFR) line should name a **measurable threshold** and, ideally, the **kind** of test that proves it (`@kind=perf`, `@kind=security`). A constraint with no number is a wish.

- ❌ "- Must be performant @kind=perf"
- ✅ "- p95 latency under 200 ms at 500 rps @kind=perf @check=perf/price-slo.feature"

## How to run it

1. `Requirement.lint({output:'both'})` — fix every structural error + lexical warning first; don't spend judgment on what the tool already caught.
2. For each **leaf** requirement, walk the five checks above against its `body` + `acceptance` (and `constraints`). Use `Requirement.get(id)` to pull the full gist.
3. Propose concrete edits to the markdown SoT (the human reviews + commits in git, same flow as code). Prefer rewriting the acceptance line into a testable EARS obligation.
4. Re-run `Requirement.lint()` to confirm you didn't regress structure, then `Requirement.readiness()` for the ship verdict.

## What NOT to do

- Don't re-flag what `lint()` already reported (duplicate ids, missing acceptance, EARS grammar, the impl-token dictionary) — start from its output.
- Don't rewrite a requirement's **intent** — fix its *expression*. If the intent itself is unclear, ask the human; never guess and overwrite.
- Don't gate the build on semantic findings the way lint gates on structural ones — semantic review is advisory; the deterministic tiers are the CI gate.
