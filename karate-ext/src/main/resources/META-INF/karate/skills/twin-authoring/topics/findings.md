# Findings — reading `Twin.check`

```js
Twin.check('policy', {plan: {…}, required: {rows: […], sha?: '<sha>'}})
// {rule, plan, required?, ceilings, evidence, transitionPairs, findings, ci{pass, reasons}}
```

`Twin.check` re-runs the deterministic walk, never a prior result, and never blesses.

## `evidence` — four aggregates, four denominators

| grade | means |
|---|---|
| `model` | the walk reached it — no calc, no system |
| `oracle` | **per step**: this step's `apply` called the rulebook at least once and no call threw |
| `mock` | `Twin.live(rule, {baseUrl, against:'mock'})` — pinned sequences replayed against a mock the twin did not generate |
| `live` | the same call with `against:'live'` — observed against an independent system |

They never blend: a number is reported with its grade or not at all. `apply` reaches the rulebook through `t.calc(input, opts?)`; a step that never called it or caught a throw is `bare`. Both count **applied** steps only: a refused or erroring step is in neither denominator. `mock` and `live` are each `{count: sequences whose latest run claimed that grade and read PASS, of: pinned sequences}`: `{0, 0}` before any pin is no sequences, not no gap; a pinned sequence with no run is ungraded and still counted. The declarations and dispositions a run needs: `Skill.help('twin-authoring', 'live')`.

## `transitionPairs`

`{covered, of}`: `of` is every pair of transitions composable over the walk's landed edges — a floor under a tripped ceiling or a depth-pruned edge; `covered` is how many the pinned deck's model replay traverses, two consecutively **applied** steps at a time (a refused or errored step breaks the chain).

## `findings` — separate lists, not one failure list

| list | what it is | on it |
|---|---|---|
| `requiredNotreached[]` | a `req:` command never fired, or a supplied row went unobserved; `undeclared:<what>:<name>` — a name this twin does not declare | check `ceilings.hit` first: not `exhausted` means the walk stopped early — re-run narrower before calling the row unreachable. Else fix the twin, or the row. |
| `sequenceDrift[]` | `{sequenceId, at, prev, now}` — a disposition or the final world hash moved with no edit to the row | decide which reading it is: the twin was revised and is right (re-bless), or it regressed (fix it). |
| `liveMismatch[]` | `{sequenceId, at, against, evidence, mismatch}` — a `Twin.live` sequence that read `FAIL` | record the disposition before touching anything: editing the twin or the expectation to make a mismatch go away is a change, not a record. `Twin.adjudicate(rule, evidence, {disposition, note?})` with `system` leaves it here, failing CI until the system changes; with `model` it becomes `incomplete {kind:'adjudicatedModel'}`. |
| `expectMismatch[]` | `{id, kind?, mismatches}` — a row's `_expect` the current model contradicts, any origin | the model or the expectation is wrong: decide from the requirement, fix one. `Skill.help('twin-authoring', 'play')` |
| `transitionPairGaps[]` | `{fromKey, command1, midKey, command2, candidate{steps}}` — a composable pair no pinned sequence traverses | a worklist, never in the CI union: pin what is worth pinning with `Rule.sequence.create(rule, {steps: row.candidate.steps})`. |
| `incomplete[]` | `{kind, …}` — **disclosure, never a defect** | carry it in whatever you report; none of these fails CI on its own. |

## `incomplete` — every kind

| kind | means | the move |
|---|---|---|
| `ceiling` | a frozen ceiling stopped expansion | cut the argument domains or lower `depth`, re-run for `exhausted` — no "not reached" reading is valid until then |
| `frontier` | worlds were left unexpanded | same; `ceilings.frontier` is how many |
| `guardNotChecked` | a guard threw, or could not read a world — `NOTCHECKED`, never "false" | fix the guard: it must be total over the worlds the walk reaches |
| `undeclaredOpenField` | a world field the state predicates do not distinguish, kept apart by its value alone | `Twin.help('aliasing')` — narrow the argument that writes it, or take it out of the world |
| `neverBlessed` | no baseline exists, so `sequenceDrift` is `[]` because nothing was compared | `Rule.bless(rule)` once the run is green — until then a green `check` is not a baseline |
| `adjudicatedModel` | a `liveMismatch` adjudicated `model` — the twin's fault | the twin owes an edit; CI passes on that row meanwhile |
| `transitionPairFloor` | `{depthPruned}` — `transitionPairs.of` under-states the universe | re-run for `exhausted`, or read the ratio as a floor |
| `sequencePairOutsideWalk` | `{sequenceId, at, fromKey, command1, midKey, command2}` — a pinned sequence traversed a pair the walk never proved possible | it used arguments no planned row has: widen the command's `args`, or leave it ungraded |
| `liveUngraded` | `{sequenceId, reason, code?}` — a live run that graded nothing: `VERSION` (the record is at an earlier `version`), `STALE` (its `twinSha`/`inputSha` is not current), `CORRUPT` (the record does not re-fold), or the disposition `UNRESOLVED`/`NOTCHECKED`/`REFUSED`/`INVALID` | re-run `Twin.live` after a retired, stale or corrupt record; otherwise fix what the `reason` names — the sequence is evidence of nothing until it reads `PASS` or `FAIL` |

## `ci`

`ci.pass` is true **iff** `ci.verdict == 'PASS'`. `INVALID` — one of the walk's fixed disclosures missing or contradicting the walk — takes precedence: an ungradeable run, `pass: false` even with every finding list empty. Fix the run before reading any finding. After that check, `FAIL` iff `requiredNotreached ∪ sequenceDrift ∪ liveMismatch ∪ expectMismatch` is non-empty. `ci.reasons[]` names each. `transitionPairGaps` never joins it.

A tripped ceiling with every required row reached is a **green run with a disclosure**. So `ci.pass: true` is never the whole report: state `ceilings.hit` and the `incomplete` kinds beside it.

Report the run as: the verdict, `ceilings.hit`, the `incomplete` kinds, and each `evidence` figure with its grade. `Rule.report(rule)` renders it for a human.
