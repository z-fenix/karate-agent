# `match` Operator — Deep Semantics

> **Skill:** `Skill.help('assertions')`. **Related topics:** `Skill.help('assertions', 'fuzzy')`.

## TL;DR

- `match a == b` — every field in both sides matches, no extras.
- `match a contains b` — every field named in `b` is present in `a` and matches. Extras in `a` are fine. (Not recursive into nested objects by default.)
- `match a contains deep b` — same as `contains` but recursive into nested objects.
- `match each a == b` — array `a`; the same shape check applied to every element.
- `match a != b` — negation.

## When to reach for each

| Situation | Operator |
|---|---|
| Asserting a constructor's output where every field is mine | `==` |
| Asserting a server response where it owns most of the shape | `contains` |
| Asserting nested config where intermediate levels add noise | `contains deep` |
| Asserting "every user in the list has role `staff`" | `each ... contains` |
| Asserting "the status isn't 'failed'" | `!=` |

## Pattern(s)

### Equality match — strictest

```feature
* def actual = { id: 1, name: 'Alice', score: 42 }
* match actual == { id: 1, name: 'Alice', score: 42 }    # passes
* match actual == { id: 1, name: 'Alice' }                # fails — `score` is extra
* match actual == { id: 1, name: 'Alice', score: '#number' }  # passes — type match
```

### Containment match — pin only what matters

```feature
* def actual = { id: 1, name: 'Alice', score: 42, _internal: 'noise' }
* match actual contains { id: 1, name: 'Alice' }          # passes; score + _internal ignored
* match actual contains { score: '#? _ > 0' }              # passes; predicate on score
```

### `contains deep` — recursive

```feature
* def actual = { user: { id: 1, profile: { name: 'Alice', age: 30 } } }
* match actual contains { user: { id: 1 } }                # FAILS — `user` requires exact equality without `deep`
* match actual contains deep { user: { id: 1 } }           # passes — recurses
* match actual contains deep { user: { profile: { name: 'Alice' } } }   # passes
```

The default `contains` is **shallow** for nested objects — the nested value must match exactly. `contains deep` makes containment recursive at every level. Use `deep` whenever the expected RHS describes a *partial* nested shape.

### Per-element match

```feature
* def users = [{ id: 1, role: 'admin' }, { id: 2, role: 'user' }, { id: 3, role: 'user' }]

# Every element has a numeric id
* match each users == { id: '#number', role: '#string' }

# Every element's role is one of the allowed values
* match each users == { id: '#number', role: '#? _ == "admin" || _ == "user"' }

# At least one element has role admin
* def admins = users.filter(u => u.role == 'admin')
* match admins == '#[_ >= 1]'
```

### Negation

```feature
* match response.status != 'failed'
* match response.errors != []
* match response != null
```

### JSON-Path extraction inside match

Karate's matcher accepts JSON-Path-flavored expressions directly on the LHS — extracting and comparing in one step.

```feature
* def cat = {
    name: 'Billie',
    kittens: [
      { id: 23, name: 'Bob' },
      { id: 42, name: 'Wild' }
    ]
  }

# Pull all elements with [*]
* match cat.kittens[*].id == [23, 42]
* match cat.kittens[*].name == ['Bob', 'Wild']

# Recursive descent with ..  (every `name` at any depth)
* match cat..name == ['Billie', 'Bob', 'Wild']
```

This is far cleaner than a JS map + match. Use it for any "every X under Y" assertion.

### Array indexing in match

```feature
* match response[0] contains { status: 'first' }
* match response[1].errors == '#[0]'           # 2nd element's errors array is empty
* match response[-1].timestamp == '#datetime'  # last element (negative index)
```

### XML match

```feature
* def xml = <root><user id="1"><name>Alice</name></user></root>
* match xml //user/@id == '1'                   # xpath attribute
* match xml //user/name == 'Alice'              # xpath text
* match xml // == <root><user id="1"><name>Alice</name></user></root>  # exact
```

## Edge cases / gotchas

1. **`contains` is shallow.** A common bug: `match { a: { b: 1 } } contains { a: { b: 1 } }` passes, but `match { a: { b: 1, c: 2 } } contains { a: { b: 1 } }` **fails** — the nested `a` must match exactly without `deep`.
2. **Array `contains` doesn't preserve order.** `match [1, 2, 3] contains [3, 1]` passes. Use `==` if order matters.
3. **`match each` over an empty array FAILS by default** (`matchEachEmptyAllowed` defaults to `false`) — an unexpectedly-empty collection is caught for you, no length pin needed. Set `configure matchEachEmptyAllowed = true` only when an empty array is legitimately valid. To assert non-emptiness *explicitly*, still pin the length: `match users == '#[_ >= 1]'`.
4. **`match a == b` ignores key ordering** in objects but not in arrays.
5. **JS expressions in expected values** are evaluated once per match. Don't put a side-effecting expression on the RHS (e.g. `Date.now()` will fix the timestamp at evaluation time, not match time — usually fine, but be aware).
6. **`#ignore` only works inside an expected structure.** `match response == '#ignore'` does NOT compile to "skip the whole match." It's a per-field marker.
7. **Match failure messages diff structurally.** A failure inside `contains` reports only the missing/different keys; a failure inside `==` reports extras AND missing fields. This matters when you want a tight failure signal: `==` gives more info but more false positives.

## Anti-patterns

1. **Anti-pattern:** `match response == { id: '#ignore', name: 'Alice', score: '#ignore' }`. Use `contains { name: 'Alice' }` instead.
2. **Anti-pattern:** `match response contains { x: { ... nested ... } }` when you wanted recursive subset. Use `contains deep`.
3. **Anti-pattern:** assuming `match each users` *passes* on an empty array. By default it **fails** (`matchEachEmptyAllowed` defaults to `false`) — good when empty is a bug; set `matchEachEmptyAllowed = true` if empty is legitimately valid.

## References

- Source: `karate-core/src/main/java/io/karatelabs/match/Operation.java`
- karate-docs: `/assertions/match-keyword`
