# Fuzzy Markers — Choosing the Right One

> **Skill:** `Skill.help('assertions')`. **Related topics:** `Skill.help('assertions', 'match')`.

## TL;DR

Fuzzy markers escape literal-value comparison. They assert **shape** without pinning **value**. Pick the most specific marker that still passes for valid data.

```
#string   < #uuid       (uuid is a stricter string)
#number   < #? _ > 0    (positive number)
#object   < { id: '#number' }     (object with shape)
#array    < #[_ >= 1] / #[3] / #[_ < 10]    (sized array)
```

## When to reach for fuzzy markers

| Field type | Marker |
|---|---|
| Generated id | `#uuid` or `#regex '/^[a-zA-Z0-9-]+$/'` |
| Server-side timestamp | `#datetime` |
| User-supplied email | `#email` |
| Computed total | `#? _ > 0` |
| Optional field | `##(<expr>)` |
| Anything (just must exist) | `#present` or `#notnull` |
| Anything (must be absent) | `#notpresent` |
| Array of unknown length | `#array` |
| Array with at least 1 element | `#[_ >= 1]` |
| Array with exactly N elements | `#[N]` |
| Array with M..N elements | `#[_ >= M && _ <= N]` |

## Pattern(s)

### Type markers — `#string`, `#number`, `#boolean`, `#array`, `#object`

```feature
* def response = { id: 42, name: 'Alice', active: true, tags: ['admin'], meta: { v: 2 } }
* match response == {
    id: '#number',
    name: '#string',
    active: '#boolean',
    tags: '#array',
    meta: '#object'
  }
```

### Presence / absence — `#null`, `#notnull`, `#present`, `#notpresent`

```feature
# 'optional' may or may not be present; if present, must be non-null
* match response contains { optional: '##(#notnull)' }

# 'deprecated' must be absent
* match response contains { deprecated: '#notpresent' }

# 'sometimes' must be present with any value (including null)
* match response contains { sometimes: '#present' }
```

### Format markers — `#uuid`, `#email`, `#date`, `#time`, `#datetime`

```feature
* def response = {
    id: '6c5e6f4a-1f3a-4e7d-9a8b-2b3c4d5e6f7a',
    email: 'alice@example.com',
    createdAt: '2026-05-18T12:34:56Z',
    birthday: '1990-01-15',
    bedtime: '22:30:00'
  }
* match response == {
    id: '#uuid',
    email: '#email',
    createdAt: '#datetime',
    birthday: '#date',
    bedtime: '#time'
  }
```

### Regex — `#regex <pattern>` or `#/pattern/`

```feature
* match response.slug == '#regex [a-z0-9-]+'
* match response.sku  == '#/^SKU-[0-9]{6}$/'
```

Both forms accept standard Java regex. The `#/...../` shorthand is closer to JS literal regex and avoids backslash-escaping in YAML/JSON contexts.

### Custom predicate — `#? <js-expr>`

The `_` is the value being matched. JS-engine context applies, so `karate.*` is reachable.

```feature
* match response.score == '#? _ > 0 && _ < 100'
* match response.tags == '#? _.length >= 1 && _.length <= 5'
* match response.createdAt == '#? new Date(_).getTime() > new Date("2020-01-01").getTime()'
* match response.userId == '#? _ == karate.get("currentUserId")'
```

### Array length — `#[size]`

```feature
* match response.items == '#[]'        # any array (incl. empty)
* match response.items == '#[3]'       # exactly 3
* match response.items == '#[_ >= 1]'      # 1 or more
* match response.items == '#[_ < 10]'  # predicate on length
```

### Array length + per-element shape — `#[size] <matcher>`

```feature
* match response.scores == '#[3] #number'
* match response.users == '#[_ >= 1] #object'
* match response.events == '#[_ >= 1] { type: \"#string\", timestamp: \"#datetime\" }'
```

Note: the per-element matcher is space-separated from the length pin.

### Optional — `##(<expr>)`

A field that may or may not be present. If absent or null, the check passes. If present and non-null, `<expr>` must match.

```feature
# 'metadata' is optional; if present, must be an object with v=2
* match response contains { metadata: '##({ v: 2 })' }

# 'nickname' is optional; if present, must be a string
* match response contains { nickname: '##(#string)' }

# 'count' is optional; if present, must satisfy a predicate
* match response contains { count: '##(#? _ > 0)' }
```

The expression inside `##(...)` follows the normal marker grammar — `#string`, `#number`, `#? <js>`, or even a literal value like `##({ v: 2 })`. Always prefix the inner marker with `#`.

### Embedded expression — `#(expr)`

Different from the `#` markers above: `#(expr)` **substitutes** the result of evaluating `expr` into the expected value. Used to splice values in:

```feature
* def expectedId = karate.uuid()
* def request = { id: '#(expectedId)', name: 'Alice' }
* match response == { id: '#(expectedId)', name: 'Alice' }
```

Distinguish:
- `'#string'` → marker (asserts type)
- `'#(varName)'` → substitution (asserts equality to the value)
- `'#? _ == varName'` → predicate (asserts equality via JS)

The substitution form is shortest when you have a variable in scope.

## Edge cases / gotchas

1. **`#? _ > 0` returns truthy/falsy.** If your JS evaluates to anything other than a boolean, the matcher coerces. `'#? _ != ""'` works; `'#? _'` works for "truthy."
2. **`##(...)` only handles null and absent.** It does *not* handle "present but undefined" — JS undefined coerces to null in JSON anyway.
3. **`#regex` does not anchor by default.** Use `^...$` if you want full-string match.
4. **`#[N]` is for arrays.** `#string` is for strings. Don't mix.
5. **Predicates inside `#?` run on every match step.** Avoid expensive computations — read other variables, don't call out to a service.
6. **Custom marker registration** (advanced): if your project has a recurring shape (e.g. ISO country codes), you can register a marker via `karate-config.js`. See karate-docs `/assertions/fuzzy-matching` §Custom Validators.

## Anti-patterns

1. **Anti-pattern:** `#ignore` everywhere. If you ignored a field, ask why you matched the surrounding object at all.
2. **Anti-pattern:** `#regex` when `#uuid` / `#email` / `#datetime` would do. Typed markers are self-documenting.
3. **Anti-pattern:** `#? typeof _ === 'string'`. Use `#string`.
4. **Anti-pattern:** Pinning to time-of-test computed values. `#? _ > new Date("2020-01-01").getTime()` is fine; `#? _ > Date.now() - 1000` introduces flake.

## References

- Source: `karate-core/src/main/java/io/karatelabs/match/Operation.java` (markers parsed in `parseExpected`)
- karate-docs: `/assertions/fuzzy-matching`
