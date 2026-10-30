# Feature-File Review Checklist

Walk top-to-bottom. Surface findings as { severity, topic-link, line, suggested fix }.

## 1. Structure

- [ ] Feature description is a clear requirement statement (it becomes a TCM record).
- [ ] Background sets shared state without leaking per-scenario data.
- [ ] Each Scenario covers exactly one outcome.
- [ ] No commented-out code (delete it; git remembers).

## 2. Tags

- [ ] `@smoke` / `@regression` for filtering (where appropriate).
- [ ] `@req=<id>` for traceability (where the scenario maps to a requirement).
- [ ] `@noreq` for opt-out (where the scenario is intentionally not requirements-mapped).
- [ ] `@report=false` for secret-handling scenarios.

## 3. Assertions

- [ ] **Not a thin assertion.** A status code alone, `match response == '#string'` (asserts only "it's a
      string"), a lone presence/type marker, or `contains 'some-substring'` on a whole body are **smoke, not a
      test** — they pass against a broken response. Assert the response **shape**: `match response == { id:
      '#string', title: 'buy milk', complete: false }` (fuzzy marker for server-assigned fields, literal for
      business fields). Under-asserting is as much a finding as over-pinning.
- [ ] No literal IDs that the server generates (use `#uuid` / `#number` + capture-and-rebind).
- [ ] No time-dependent literals (use `#string` / regex / fuzzy predicate).
- [ ] One outcome per match where readability is at risk.
- [ ] Schema-style match where the contract matters more than the exact values.

## 4. Coverage

- [ ] Happy path present.
- [ ] At least one explicit error path (4xx / 5xx) **asserting the error body contract**, not just the status.
- [ ] Auth-failure path where the endpoint requires auth.
- [ ] Edge cases for boundary inputs.
- [ ] **Stateful flows are chained, not faked.** For a resource API, at least one scenario exercises a real
      lifecycle — create → capture the id (`* def id = response.id`) → read it back (`match … == { id: '#(id)',
      … }`) → update → delete → 404 — in **one** scenario, rather than independent single-shot calls that never
      prove persistence.
- [ ] **Variations are data-driven** (`Scenario Outline:` + `Examples:`), not copy-pasted near-identical scenarios.

## 5. Reuse

- [ ] No copy-pasted setup that should be `call read('common-setup.feature')`.
- [ ] No per-scenario auth dance that should be `karate.callSingle('auth.feature')`.
- [ ] No magic numbers without a `* def` binding.
- [ ] No legacy env/sysprop idioms — flag `karate.properties['x']`, `java.lang.System.getenv(...)`, `java.lang.System.getProperty(...)`, and `karate.sysenv('X') || 'default'`. Rewrite to `karate.sysenv(name, [default])` / `karate.sysprop(name, [default])`. See `Skill.help('migration')` §Legacy idioms for the full mapping.

## 6. Flakiness

- [ ] No `Thread.sleep` (use `retry until`).
- [ ] No order-dependent state across scenarios.
- [ ] External dependencies acknowledged (mocked, or scenario tagged for environments that have them).

## 7. Style

- [ ] Scenarios independent (can run alone, can run in parallel).
- [ ] Consistent naming (snake-vs-kebab-vs-camel — pick one).
- [ ] No raw URLs (use `* url baseUrl` + `path`).
