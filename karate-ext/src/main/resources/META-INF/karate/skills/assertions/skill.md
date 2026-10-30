# Karate Assertions — Skill

> **Status:** Foundational skill referenced by every other authoring skill.
> **Stack:** Karate's built-in `match` operator + fuzzy markers + `karate.match()` / `karate.expect()` JS API. Pure karate-core; no addon.
> **Prerequisites:** `Skill.help('getting-started')` for project setup.
> **Companion docs:** karate-docs `/assertions/match-keyword` and `/assertions/fuzzy-matching`.

## What this skill is for

Pick the **right assertion shape** for the value being checked. Karate's matcher is the most under-leveraged feature in the framework — used well it gives concise, schema-style, future-proof assertions; used badly it gives brittle, over-pinned tests that break on every server-side change.

This skill teaches an LLM to:

1. Pick between `match ==`, `match contains`, and `match each`.
2. Use the fuzzy markers idiomatically (`#string`, `#number`, `#uuid`, `#regex`, `#? <js>`, `#[size]`, `##(opt)`).
3. Write **schema-style** matches that pin contract shape without pinning incidental values.
4. Switch between `match` (step keyword) and `karate.match()` (JS API) when needed.
5. Reach for `karate.expect()` (Chai-style) when an assertion needs to read like an English sentence.
6. Enable soft assertions via `continueOnStepFailure` when collecting multiple failures matters more than fail-fast.

## Mental model

A Karate match is a **shape comparison**, not an equality comparison. The shape can be:

- **Exact** (`==`) — every field, every value, every type, no extras.
- **Subset** (`contains`) — every named field present with the expected value; extras allowed.
- **Per-element** (`each`) — same comparison applied to every array element.

Fuzzy markers (`#string`, `#? _ > 0`) escape literal-value comparison for fields whose **shape** matters but whose **value** is incidental. The matcher walks the structure recursively, applying these rules at every level.

```
   actual               expected             outcome
   ──────               ──────────           ─────────
   { a: 1, b: 2 }   ==  { a: 1, b: 2 }       pass
   { a: 1, b: 2 }   ==  { a: 1 }              fail (extra: b)
   { a: 1, b: 2 }   contains { a: 1 }        pass (b ignored)
   { a: '#number' } applied to actual.a = 1  pass (type match)
```

## Quick start

```feature
Feature: assertion shapes

Scenario: equality vs containment vs per-element
  * def response = { id: 'a1', name: 'Alice', score: 42, tags: ['admin', 'user'] }

  # Exact — every field must match
  * match response == { id: 'a1', name: 'Alice', score: 42, tags: ['admin', 'user'] }

  # Subset — pin what matters, ignore the rest
  * match response contains { id: 'a1', name: 'Alice' }

  # Fuzzy shape — type-only assertion for volatile fields
  * match response == { id: '#string', name: '#string', score: '#number', tags: '#array' }

  # Custom predicate — value must satisfy a JS expression
  * match response.score == '#? _ > 0'

  # Per-element — every tag is a non-empty string
  * match each response.tags == '#? _.length > 0'

  # Containment + array operators
  * match response.tags contains 'admin'
  * match response.tags contains only ['admin', 'user']
  * match response.tags contains any ['admin', 'guest']
```

## The matcher cheat-sheet

| Goal | Operator | Example |
|---|---|---|
| Exact equality (every field, no extras) | `match a == b` | `match response == { id: 42, name: 'Alice' }` |
| Subset (named fields match, extras OK) | `match a contains b` | `match response contains { id: 42 }` |
| Subset, recurse into nested objects | `match a contains deep b` | `match response contains deep { meta: { v: 2 } }` |
| Per-element (array) | `match each a == ...` | `match each response == { active: true }` |
| Per-element, contains | `match each a contains ...` | `match each response contains { type: 'user' }` |
| Negation | `match a != b` | `match response.status != 'failed'` |
| Array has these elements | `match a contains [b1, b2]` | `match response.ids contains [1, 2]` |
| Array has at least one of | `match a contains any [b1, b2]` | `match response.tags contains any ['admin', 'staff']` |
| Array has only these elements | `match a contains only [b1, b2]` | `match response.tags contains only ['admin']` |
| JSON-path extraction (all elements) | `match a[*].field == [...]` | `match cats[*].name == ['Billie', 'Bob']` |
| JSON-path recursive descent | `match a..field == [...]` | `match cat..name == ['Billie', 'Bob', 'Wild']` (every `name` at any depth) |

## Fuzzy markers — exhaustive

| Marker | Meaning |
|---|---|
| `#ignore` | Field exists with any value (skip comparison) |
| `#null` | Explicit null |
| `#notnull` | Not null (key may or may not be present) |
| `#present` | Key exists (any value, including null) |
| `#notpresent` | Key absent |
| `#string` | Any string |
| `#number` | Any number (int or float) |
| `#boolean` | Any boolean |
| `#array` | Any JSON array |
| `#object` | Any JSON object |
| `#uuid` | UUID string |
| `#email` | Email-shaped string |
| `#date` | Date string (ISO-8601 date) |
| `#time` | Time string |
| `#datetime` | ISO-8601 datetime string |
| `#regex <pattern>` | Matches regex (or `#/pattern/` shorthand) |
| `#? <js-expr>` | Custom predicate; `_` binds to the value |
| `#[size]` | Array length. Forms: `#[3]` (exact), `#[_ >= 3]` (predicate on length using `_`). **There is no `+` shorthand** — `#[3+]` / `#[N+]` does not parse; use `#[_ >= 3]`. |
| `#[size] <matcher>` | Length + per-element shape: `#[2] #number` |
| `##(expr)` | **Optional** — null or absent passes; otherwise evaluate `expr` |
| `#(expr)` | **Embedded expression** — evaluates and substitutes |

The `_` placeholder inside `#? <js>` is the value being matched at that node. Inside `match each`, `_$` references the parent (the array element's containing object).

## Topic index

| Topic | When to read |
|---|---|
| `Skill.help('assertions', 'match')` | Deep semantics of `==` vs `contains` vs `each`; nested matching; XML matching. |
| `Skill.help('assertions', 'fuzzy')` | Every fuzzy marker with one realistic example each; choosing predicates. |
| `Skill.help('assertions', 'schema-tools')` | **Generate** an assertion from a real payload (`match.schema`), validate against a JSON-Schema in one shot (`match.jsonSchema`), convert payload↔JSON-Schema↔match-schema. |

## Standing rules

1. **Default to `contains` for response objects with stable-vs-volatile fields.** Pin only what matters; let the rest float.
2. **Use `#? _ > 0` to assert positive numbers** rather than pinning to a server-computed value.
3. **Use `#uuid` (or `#regex`) to assert generated IDs** and rebind the actual value: `match response.id == '#uuid'` then `* def newId = response.id`.
4. **For dates/timestamps, prefer the typed marker** (`#datetime`) over `#string`. Catches "server returned an int instead of an ISO string" sooner.
5. **Schema-style match the happy path; literal-value match only the boundary scenario.** A test that asserts both shape and exact value duplicates intent.
6. **One match per assertion clause.** Don't AND-up two unrelated shape checks into one giant blob — the failure diff stays readable.
7. **Use `match each` for array-of-objects, not a JS loop.** It's faster, gives a per-element diff, and reads cleaner.
8. **For optional fields, use `##(expr)`.** Null or absent is fine; if present it must match `expr`.
9. **Anchor match labels with a `# comment` line** when a step's failure message would otherwise be ambiguous.
10. **When `match` ergonomics fight you, switch to `karate.match()` in a JS block.** It returns `{ pass, message }` and you can branch on it. Real pattern:
    ```feature
    * def r = karate.match(actual, expected)
    * if (!r.pass) karate.fail(r.message)
    ```
11. **Function literals are `def name = function() { ... }`.** Useful for reusable assertions inside a feature: `* def isPositive = function(n) { return n > 0 }` then `* match response.score == '#? isPositive(_)'`.
12. **Don't hand-type a shape you can generate.** Capture one real response and run `match.schema(response)` to get the marker shape, then tune it — faster and less error-prone than guessing markers (see `Skill.help('assertions', 'schema-tools')`). If your contract is already a JSON-Schema, assert it directly with `match.jsonSchema(actual, schema)`.

## Anti-patterns

1. **Anti-pattern:** `match response == { id: 42 }` when the server generated the id. Pins to mock data and breaks the moment a real run uses a different id.
2. **Anti-pattern:** asserting on `Date.now()` literals. Use `#datetime` or `#? new Date(_).getTime() > baseline`.
3. **Anti-pattern:** chained `match` steps that re-pin the same field three times. One `match contains { … }` captures it once.
4. **Anti-pattern:** `match a == b` followed by `match a contains c`. The first already says everything the second does.
5. **Anti-pattern:** asserting a list's exact order when the API doesn't specify order. Use `contains only`.
6. **Anti-pattern:** assuming `match each` over an empty array passes. By default it **fails** (`matchEachEmptyAllowed` defaults to `false`); set `= true` only if empty is legitimately valid. To assert non-emptiness explicitly, pin the length: `'#[_ >= 1]'`. (Note: `'#[1+]'` reads naturally but does **not** parse — only the `'#[_ <op> N]'` predicate form works.)
7. **Anti-pattern:** copy-pasting a 50-line literal JSON when only one field matters. Use `contains` + `#ignore` for the noise.

## Cross-skill links

- `Skill.help('api')` — REST API tests; assertions drive response validation.
- `Skill.help('grpc')` — gRPC tests use the same matcher on decoded messages.
- `Skill.help('kafka')` — Kafka messages decoded to JSON; same matcher applies.
- `Skill.help('review')` — review heuristics cite over-pinning, generated-id-pinning, and order-dependence.
