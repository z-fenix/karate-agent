# Sequences — `sequences.json`, promotion, replay

The sixth rulebook file, optional. A versioned document: a copied file says what was replayed against which model.

```jsonc
{ "version": 1,
  "provenance": { "twin": "<sha>", "calc": "<sha>", "schema": "<sha>", "generator": "<sha>" },
  "sequences": [
    { "_id": "seq-bind-after-expiry",
      "_label": "bind refused once the quote expired",
      "_from":  { "kind": "explore", "walk": "<sha>", "plan": { }, "planSha": "<sha>" },
      "steps":  [ { "command": "submit", "args": { "day": 0 } },
                  { "command": "rate" },
                  { "command": "bind", "args": { "day": 40, "term": 12 } } ],
      "_expect": { "steps": [ { "at": 2, "outcome": "refused", "stage": "apply" } ],
                   "final": { "status": "quoted" } } } ] }
```

**Commands only.** `Twin.run` recomputes every world from the root; the clock value rides in `steps[].args`, never outside the command.

## Writing rows

```js
Rule.sequence.create('policy', {steps: [{command: 'rate', args: {drivers: 2}}], _label: '…'})
Rule.sequence.create('policy', [row, row], {ifMatch: '<mtime>'})   // a row or an array
Rule.sequence.update('policy', 'seq-…', {_label: '…', _expect: {…}})
Rule.sequence.delete('policy', 'seq-…')
```

Every row is validated against the declared commands before any write: `steps` an array of rows — **possibly empty**, which pins the root world alone — each `command` declared, each `args` canonical JSON, `_expect` well-formed, `_id` (`seq-…`, minted when absent) unique. Otherwise `SEQUENCE_INVALID`, naming the row and index, and nothing is written. A write needs a `twin.js` (`TWIN_NOT_FOUND`); an unknown id is `SEQUENCE_NOT_FOUND`.

**`_from` is engine-written.** A caller-supplied `_from` on `create`, or a patch touching `_id`/`_from`, is `SEQUENCE_INVALID`.

## Promotion — the engine verifies it

```js
Rule.sequence.create('policy', {promote: {plan: {…}, counterexample: '<id>'}, _id: '…', _label: '…'})
```

**Order of operations.** Pin the required rows' witnesses by promotion (`promote: {required: {id, rows, sha}}`, where `id` names ONE ROW inside the `rows` supplied here — never the manifest), then `Twin.live`; author a step list only for an order no row demands.

This **re-runs the walk** under `plan` — a write with a walk's cost, since a hash cannot prove the counterexample is still there. It writes only when that walk ends `ceilings.hit: 'exhausted'`; otherwise `PROMOTION_INCOMPLETE`, carrying the ceiling and frontier (a truncated frontier is not absence). With the frontier exhausted and no counterexample carrying that `id`, `COUNTEREXAMPLE_NOTFOUND`. On success the `steps` come from the walk and `_from` carries the engine's own hashes.

`promote: {play: id}` pins a person-confirmed mock session instead: `Skill.help('twin-authoring', 'play')`.

## The two provenances

| where | means |
|---|---|
| document `provenance` | the **replay context** — the rulebook's `mtimes` at the document's last write. Refreshed by every `Rule.sequence.*` write. Claims nothing about where a row came from. |
| row `_from` | the **origin**, immutable once written: `explore` (walk hash, the plan itself and its hash) · `authored` · `live` · `play` |

`Twin.run` returns both, as disclosures, never refusals — it always replays the **current** model; recovering recorded bytes is git's job.

- `provenance{file?, current, differs}` — `differs` is a boolean over the two maps beside it.
- `origin` is the row's `_from` plus, for an `explore` origin, `differs`: the engine re-runs `_from.plan` and compares the resulting walk hash with `_from.walk`, naming both when they differ. An `authored` origin has no `differs`.

## Replay is linear and total

```js
Twin.run('policy', 'seq-bind-after-expiry')                        // by id
Twin.run('policy', [{command: 'rate', args: {drivers: 2}}], {trace: true})   // inline
```

Every step is validated before the first runs: an unreplayable sequence is a refusal, never a half-run. A refused or errored step leaves the world where it was and the replay continues; it reports `postcondition: notEvaluated` and the invariants of the world it left. A clock argument that goes backwards surfaces as `outcome: refused, stage: clock` — the guard never ran.

Inline steps have no id, no origin and no expectation: `sequenceId` and `origin` are absent, `expect` is `{checked: false, mismatches: []}`, and `sequences.json` is not consulted.

## `_expect` — the subset law

Optional and sparse. `steps[]` carries a per-step `outcome` (`applied | refused | error`, exact) with optional `stage`/`reason` and an optional `world` **subset**; `final` is the same subset over the final world. `world` and `final` name **world fields** — a `t.state` id matches nothing there. `at` is a zero-based index, unique and in range, else `SEQUENCE_INVALID`. An index with no entry asserts nothing about it.

- an object matches when every expected key is present and matches recursively;
- an array matches only at **exact length, positionally**;
- a scalar and `null` match by equality;
- an **absent key is distinct from a present `null`**.

A mismatch names its `field` as a dotted path from the entry (`outcome` · `world.quote.status` · `final.items[1].sku`), carries `at` only for a `steps[]` entry, and omits `actual` when the key was absent.

A row without `_expect` asserts only that the model still accepts it. A promoted counterexample carries the observed results as **origin**, not as an expectation — write an `_expect` from the requirement, where it can disagree with the model.

## Drift

`Rule.bless` extends the verification baseline with `twin: <sha>` and `sequences[{sequenceId, inputSha, dispositions[], finalSha}]` — the per-step outcome vector and the canonical final-world hash — only when a `twin.js` exists. `Rule.drift` then reports `sequenceDrift[{sequenceId, at, prev, now}]` wherever a disposition or the final hash moved with no edit to the row. `sequenceDrift` fails CI: a revised twin that is right, or one that regressed — decide which.
