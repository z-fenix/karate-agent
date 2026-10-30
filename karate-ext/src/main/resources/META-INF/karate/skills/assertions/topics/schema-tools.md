# Schema Tools — generate assertions from data, validate against JSON-Schema

> **Skill:** `Skill.help('assertions')`. **Related topics:** `Skill.help('assertions', 'fuzzy')`, `Skill.help('assertions', 'match')`.

The `match.*` namespace carries **schema tools** — the inverse of asserting. Instead of hand-typing fuzzy
markers, hand it a real payload (or a JSON-Schema) and it writes the shape for you; or validate a value against
a JSON-Schema in one shot. Pure karate-core; free with any product. Three forms, three representations:

- **payload** — a concrete JSON value (a real response / message).
- **JSON-Schema** — the standard, what an OpenAPI body or AsyncAPI message embeds.
- **Karate match-schema** — the `#`-marker shape the `match` operator speaks (`#string`, `##optional`, `#[] …`).

## TL;DR — pick the verb

| You have… | You want… | Verb |
|---|---|---|
| a real payload | a reusable Karate assertion | `match.schema(example)` |
| a real payload (or several) | a JSON-Schema | `match.toJsonSchema(example[, more…])` |
| a JSON-Schema (e.g. OpenAPI body) | a Karate assertion to read/edit | `match.fromJsonSchema(schema)` |
| a value **and** a JSON-Schema | a one-shot conformance assertion | `match.jsonSchema(actual, schema, labelOrOpts?)` |
| a value **and** a JSON-Schema | a verdict to branch on, no throw | `match.testJsonSchema(actual, schema)` |

## Generate an assertion from a real payload — `match.schema`

The highest-leverage move: capture one real response, get a future-proof **shape** assertion (types pinned,
volatile values free). No more guessing markers.

```feature
Scenario: shape-assert a response without hand-typing markers
  * def response = { id: '7f1c…-uuid', name: 'Alice', score: 42, tags: ['admin'], addr: { city: 'SF' } }
  * def shape = match.schema(response)
  # shape -> { id: '#uuid', name: '#string', score: '#number', tags: '#[] #string', addr: { city: '#string' } }
  * match response == shape
```

Pass **several samples** and fields that aren't always present surface as `##` (optional), nulls as nullable:

```feature
* def shape = match.schema(sampleA, sampleB, sampleC)   # nick missing in one → { nick: '##string', … }
```

`#uuid`/`date-time`/`email`/`uri` formats are auto-detected from the values. Then **tighten by hand** where a
type-only marker is too loose (`#number` → `#? _ > 0`) — the generator gives you the skeleton, not the last word.

## Validate against a JSON-Schema in one shot — `match.jsonSchema`

For teams who keep their contract as JSON-Schema (an OpenAPI body, an AsyncAPI message). **Real JSON-Schema
semantics** — `oneOf` / `anyOf` / `if`-`then`-`else` / `patternProperties` / `const` / `additionalProperties: false`
/ `unevaluatedProperties` all mean what the spec says. Throws on failure and emits an evidence row, exactly like
`match.equals`; each failure names the keyword, where in the payload, where in the schema, and — for a combinator —
the *nearest* branch and why it missed.

```feature
* def schema = read('order-schema.json')
* match.jsonSchema(response, schema, 'response conforms to the order contract')
```

Options (the 3rd arg is a label **or** an options object):

```feature
# `format` is an ANNOTATION by default (the spec's rule) — opt in to assert it:
* match.jsonSchema(response, schema, { label: 'order', assertFormat: true })
# a schema that declares no `$schema` can be told which dialect it is:
* match.jsonSchema(response, schema, { dialect: 'oas-3.0' })
# …and the non-throwing form, when you need to branch:
* def result = match.testJsonSchema(response, schema)   # { valid, errors[] }
```

> `assertFormat` asserts the format names that dialect defines — `date-time`, `date`, `uuid`, `email`, `ipv4`,
> `uri`, … for JSON-Schema, plus `int32`/`int64`/`byte` for an OpenAPI dialect. A custom format name (`account-id`)
> stays an annotation. Prefer `match.jsonSchema` when you want JSON-Schema to *mean* JSON-Schema; use
> `match.fromJsonSchema(schema)` + `match.containsDeep` when you deliberately want the lenient, karate-flavoured path.

## Convert between the two — `fromJsonSchema` / `toJsonSchema`

```feature
# JSON-Schema → a Karate assertion you can read and tune:
* def shape = match.fromJsonSchema(openapiBodySchema)   # { id: '#uuid', status: "#? ['NEW','SHIPPED'].indexOf(_) != -1" }
* match response == shape

# payload(s) → a JSON-Schema (e.g. to seed a spec or document an event):
* def jsonSchema = match.toJsonSchema(response)
```

## When to reach for which

- **Authoring a new test against a live/mock endpoint** → `match.schema(response)`, then tune. Fastest path to a
  clean shape assertion.
- **You already own a JSON-Schema contract** → `match.jsonSchema(actual, schema)` to enforce it directly, or
  `match.fromJsonSchema` to turn it into editable markers.
- **You need a JSON-Schema artifact** (seed a spec, document an observed event) → `match.toJsonSchema(samples…)`.

## Anti-patterns

1. **Shipping the generated shape unread.** `match.schema` is a *starting point* — review it; tighten `#number` to a
   predicate where the value has meaning, drop fields you don't care about with `contains`.
2. **Using `match.jsonSchema` when you only need a couple of fields.** A one-line `match contains { id: '#uuid' }` is
   clearer than dragging in a whole schema.
3. **Re-deriving the shape every run.** Generate once at authoring time, paste the literal into the feature. The
   verbs are for *writing* tests, not a runtime indirection.
