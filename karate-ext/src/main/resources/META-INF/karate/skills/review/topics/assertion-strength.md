# Assertion strength — over-pinning vs under-asserting

A passing test is only as good as what it asserts. Two failure modes, opposite directions:

- **Under-asserting** (the common one for generated tests): the call is made, the status is checked, and
  little else. It passes against a **broken** response — a silent false-green.
- **Over-pinning**: asserting volatile, server-assigned values (a generated id, a timestamp) as literals,
  so the test breaks on every run for no real reason.

The target is in between: **lock the contract shape**, fuzzy for what the server assigns, literal for what
carries business meaning.

## Thin assertions to flag (under-asserting)

These are smoke, not tests — each passes against a wrong or empty body:

```gherkin
Then status 200                          # status only — nothing about the body
And match response == '#string'          # "it's a string" — not WHICH string
And match response contains 'DOCTYPE'    # a substring of a whole document
And match response.id == '#present'      # a field exists — not its value or the rest of the shape
And match each response == '#object'     # every element is merely "an object" — no field shapes locked
```

Rewrite each to assert the actual shape:

```gherkin
And match response == { id: '#string', title: 'buy milk', complete: false }   # the whole contract
And match response == { items: '#[_ >= 1]', total: '#number' }                     # non-empty + typed
```

## The strength ladder (strongest first)

1. **`match ==` on the full shape** — the highest-value API assertion. Fuzzy marker for server-assigned
   fields (`id: '#string'`/`#uuid`, `createdAt: '#datetime'`), **literal** for business fields
   (`status: 'BOUND'`, `total: 165.6`). Catches missing, renamed, extra, and wrong-typed fields at once.
2. **`match contains { … }`** — assert the fields you care about when extra fields are legitimately allowed
   to vary. Weaker than `==` (it ignores unlisted fields) — use deliberately, not as a default.
3. **Presence/type markers** (`'#string'`, `'#present'`, `'#notnull'`) — a floor, only for a field whose
   value genuinely can't be pinned. Not a whole-body assertion.
4. **Status only** — necessary, never sufficient (except a bare `DELETE → 204` inside a chain that then
   reads back a 404).

## Anti-echo — don't assert the bug

If the expected value is copied from the *same run's* observed response, the test asserts whatever the
system did, including a bug ("assert the bug"). Ground the expected value in an independent oracle — the
spec, the business rules (`Rule.execute`), or a value you computed — not `response.x` echoed back.

## Judge / review stance

Reviews **surface** weak assertions; they don't score or block (that's the roadmap `assertedProportion`
grade). Flag under-asserting as firmly as over-pinning: *"this scenario runs green but asserts only a
status — it would pass against a broken response; lock the shape with `match response == { … }`."*

See also: the `assertions` skill (the full matcher + fuzzy-marker reference) and `api` §"Lock the response
to the contract shape".
