# Reverse-engineer requirements (the hollow-stub deriver)

> Turn a spec — or an existing run's coverage — into **draft requirement stubs** you then ground.
> Deterministic, authoring-only (`Openapi.deriveRequirements`): it never calls an LLM and writes
> nothing; you review and write the output.

## What `Openapi.deriveRequirements(spec)` produces

One **hollow** requirement stub per operation, grouped into features (by CRUD resource, else the first
tag, else the first path segment), as `karate-requirements/v1` authoring markdown. Example, from a
`/products` CRUD spec:

```markdown
# Products API — derived requirements

> Reverse-engineered from the OpenAPI spec by `scaffold-from-openapi`. These stubs are
> **hollow** — they restate the API's behavior, not the business intent. Review each: ...

## PRODUCTS: Products
@type=feature

### PRODUCTS-002: Create a product
@source=derived @real=openapi:createProduct
_Derived from `POST /products` → 201._

**Acceptance:**
- 1: WHEN a client sends `POST /products` with a valid request body THE SYSTEM SHALL respond 201
```

Returns `{ requirements: '<markdown>', count: <n>, hint: ... }`. Write `requirements` to
`requirements/derived.md`.

## Why the stubs are hollow (the reward-hacking gate)

Derived-from-spec requirements **restate behavior, not intent** — "POST /products returns 201" is what
the API *does*, not *why the business needs it*. If you let such stubs count as real coverage, a green
report proves nothing (you've graded the spec against itself). So every stub is **visibly incomplete**:

| Marker | Meaning |
|---|---|
| `@source=derived` | the hollow flag — the report/skill surface it for promotion; **remove it once grounded** |
| `@criticality` (absent) | impact is **yours to assign** — the deriver never guesses how bad a failure is |
| `@real=openapi:<opId>` | wired to the operation, so an observed hit strengthens it — but graded `incidental` (credited *and flagged*) until you add a real `@req=` intent anchor |
| title + prose | a mechanical restatement — rewrite as the actual requirement |
| acceptance | one happy-path EARS line — add the negative/edge criteria the spec implies |

## The grounding checklist (per stub)

1. **Rewrite the title + prose** as real intent ("Create a product" → "A merchant can list a new product
   for sale").
2. **Assign `@criticality`** (`low`/`medium`/`high`) — this drives risk, posture, and readiness.
3. **Refine acceptance** — add the empty-body 400, the auth 401/403, the not-found 404 the spec declares;
   each becomes a `- N: WHEN … THE SYSTEM SHALL …` line (tag tests `@req=ID/N` to cover them).
4. **Add `@req=` coverage** from the tests that verify it (the *intent* anchor; promotes `incidental`
   → `exercised`).
5. **Drop `@source=derived`** once the stub is real. (Until then, `Requirement.gaps()` / the report keep
   flagging it — that is the system working as designed.)

## The other direction — from an existing run

The same idea applies when you already have a run: read `Coverage.gaps()` for operations with traffic but
no requirement, and author stubs for them (wired with `@real=` to the exercised artifact). This closes the
"under-declaration is invisible" gap — the work is real, it just has no intent anchor yet.

## Related

- `Openapi.help()` — the verb reference. `Requirement.help()` + the **`requirements`** skill — how to
  author the grounded result (lint, readiness, the tier-3 semantic review).
