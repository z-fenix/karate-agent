# Requirements — authoring conventions

> The **locked** markdown form the one-shot parser lowers into normalized
> `karate-requirements/v1`. Near-standard markdown with exactly three conventions. The parser is
> **best-effort** — a malformed line degrades, it never throws — but author the locked form so the
> normalized output (and the ALM integration point) stays clean.

## 1. A heading `ID: Title` is a requirement

```markdown
# Orders — Requirements      ← `#` is the DOCUMENT title, not a requirement

## COMMERCE: Commerce        ← `##`+ are requirements; heading depth = the parent hierarchy
@type=epic

### ORD: Orders
@type=feature

#### ORD-001: Place a valid order
```

- The token before the first `:` is the **stable id** (`ORD-001`) — no internal whitespace, or it's treated as prose. The rest is the title.
- **Heading depth is the hierarchy:** a deeper heading's `parent` is the nearest shallower one. Cross-file or override parents with `@parent=<id>`.
- Ids are the graph keys and the `@req=` join target — keep them **stable**; renaming an id orphans its tags.

## 2. One `@key=value` tag line, directly under the heading

Same grammar as scenario tags. Well-known keys are promoted to fields; everything else is categorization.

```markdown
#### ORD-001: Place a valid order
@status=approved @priority=p1 @criticality=high @owner=orders-team
```

| Key | Meaning |
|---|---|
| `@type` | the level — open vocabulary (`epic`/`feature`/`story`/`req`) |
| `@status` | **authored** workflow status (`approved`/`draft`/…) — *not* coverage status |
| `@priority` | value/urgency |
| `@criticality` | **impact** (`high`/`medium`/`low`) — drives `risk` + release-readiness (≠ priority) |
| `@parent` | explicit parent id (overrides heading nesting) |
| `@ref` | soft cross-pointers to other requirements — singular, repeatable / comma-separated → `ref[]` |
| `@real` | the implementing artifact(s) — "realized by" an OpenAPI op / gRPC method / code symbol; singular, repeatable / comma-separated → `real[]` (see below) |
| anything else | lands in `tags{}` (absorbs ALM custom-field sprawl) |

The linkage tags (`@ref`, `@real`, and the scenario-side `@req`/`@cov`) are **singular and one-to-many**: repeat the tag or comma-separate (space-free) — both accumulate into a JSON array. No plural tag forms.

**Author `@criticality` on leaves** — it is the input to `Requirement.readiness()`. Unauthored defaults to `medium`.

### `@real=` — link the requirement to its implementing artifact (optional, high-leverage)

```markdown
#### ORD-001: Place a valid order
@criticality=high @real=openapi:placeOrder,openapi:reserveInventory
```

`@real=` (read it as **"realized by"**) names the concrete, machine-checkable artifact(s) that implement the requirement — namespaced cross-source ids (`openapi:placeOrder`, `grpc:Orders/Place`, a future `java:com.foo.Order`). It lets an **observed** hit on that artifact strengthen the requirement's coverage (graded by provenance — grounded / incidental / drift; see the skill's "Strengthening declared coverage" section) and powers impact-analysis / orphan / drift reporting via `Requirement.gaps()`. Optional — omit it and nothing changes. At scale, author the matrix centrally in `karate-boot.js` with `cov.real` (a function, or a data map that drives it).

## 3. `**Acceptance:**` (EARS) and optional `**Constraints:**` (NFRs)

```markdown
#### ORD-001: Place a valid order
@status=approved @priority=p1 @criticality=high
The system shall accept an order for a known customer and return a generated id.

**Acceptance:**
- WHEN the cart has >= 1 line for a known customer THE SYSTEM SHALL return 201 with an order id

**Constraints:**
- p95 latency under 200 ms at 500 rps @kind=perf @check=perf/orders-slo.feature
```

- **Criterion ids** — each acceptance line is an individually-coverable criterion, referenced qualified by
  its requirement: `@req=ID/1`, `@req=ID/2`, … (and a rulebook's `calc.req('ID/1')` lights it). **The id is
  just the line's 1-based position** — a plain acceptance list gives `1, 2, 3, …` in document order, no
  prefix needed. The id after the `/` must match an existing criterion (confirm with `Requirement.get('ID')`
  → `criteria[]`); a dangling `/<acId>` silently covers nothing. An id never contains `/` — that's the
  reserved requirement↔criterion separator. **Only if you later need to *insert* a criterion** without
  renumbering existing `@req=` refs, give that one line an explicit dotted id (`- 1.5: WHEN …` slots between
  `1` and `2`); otherwise leave them as plain positions.
- **Acceptance** = one EARS line per list item — bulleted (`-`) or numbered (`1.`) — stored verbatim; the block may also be opened by a heading whose text is the label (`#### Acceptance Criteria`), which is what generated spec-driven-development docs emit. EARS templates:
  - Ubiquitous — `THE SYSTEM SHALL <response>`
  - Event — `WHEN <trigger> THE SYSTEM SHALL <response>`
  - Unwanted — `IF <condition> THEN THE SYSTEM SHALL <response>`
  - State — `WHILE <state> THE SYSTEM SHALL <response>`
  - Optional — `WHERE <feature> THE SYSTEM SHALL <response>`
  - Every line needs a **`shall`/`must`** obligation — `lint()` flags lines that lack one (`acceptance-not-ears`).
- **Body** prose between the tag line and the first block is the requirement statement (RFC-2119 strength words encouraged).
- **Constraints** are NFRs: free `text`, optional `@kind=` (open string) and `@check=` (a governance-oracle pointer).

## Joining tests to requirements

On a scenario, tag `@req=<id>`:

```gherkin
@req=ORD-001
Scenario: place an order for alice
```

Bare ids resolve to the default `req` namespace (git-markdown native, zero re-tagging). ALM / LLM-native front-ends use the namespaced form `@req=xray:STORE-106`. The tag becomes a **DECLARED** edge; a passing covering scenario lights the requirement `COVERED`.

## What `lint()` enforces deterministically

Author to stay clean: a title on every requirement, unique ids, resolvable non-cyclic parents, an EARS acceptance block on every leaf, no implementation tokens or conjunctions in the goal. Run `Requirement.lint()` and fix its findings before committing.
