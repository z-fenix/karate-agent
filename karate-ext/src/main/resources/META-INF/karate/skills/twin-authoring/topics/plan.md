# The exploration plan

The plan is **data the engine executes**, not a script the model drives. It is applied deterministically and saved beside the result, so an AI-guided run replays byte-for-byte without the model that proposed it.

```jsonc
{ "depth":  { "default": 2, "endorse→cancel": 5 },
  "levels": { "rate.drivers": [1, 3], "rate.day": [0, 40] },
  "focus":  ["POL-007", "POL-003"] }
```

Pass the same plan to every verb that walks — `explore` and `check` echo it under `plan`, `witness` under `selection.planSha`. A witness run given another plan walks another population, so its denominators are not the walk's:

```js
Twin.explore('policy', {plan: {depth: {default: 3}}})
Twin.check('policy',   {plan: {levels: {'rate.drivers': [1, 3]}}})
Twin.witness('policy', {baseUrl: url, against: 'mock', plan: {depth: {default: 3}}})
```

## The three keys

| key | shape | meaning |
|---|---|---|
| `depth` | `{default: <int>, '<from>→<to>': <int>}` | how deep the walk goes; a pair key raises it for that command-to-command edge only |
| `levels` | `{'<command>.<argument>': [v, …]}` | **replaces** the declared levels of that one argument path for this run |
| `focus` | `[<reqId>, …]` | ids validated against the supplied `required` row ids ∪ the `req:` ids in the twin; it narrows the findings, never the walk |

Omit any key. An empty plan (`{}`) is the declared model at its declared domains.

`levels` is the one lever that changes what the walk enumerates. Its law is membership: every replacement value must already be in the declared domain — an integer inside `a.int(name, lo, hi, levels)`'s bounds, a member of `a.enum(name, [v, …])`'s list, canonical JSON. **The bounds never move.** A plan can narrow or re-sample a domain; it can never widen one. Widening is an edit to `twin.js`.

## `PLAN_INVALID` — every cause

The refusal names the offending field. Fix the plan, not the twin, for all of these:

- a `levels` path naming no builder-form axis — a literal-form command has no axes, and a `const` has no levels;
- a level that is empty, out of the declared domain, or not canonical JSON;
- a `depth` key naming an unknown command, or a malformed `'<from>→<to>'` pair;
- a `depth` value that is not an integer, or is negative;
- a `depth` above the frozen ceiling — a plan only tightens;
- a `focus` id matching neither a supplied `required` row nor a `req:` in the twin;
- an unknown top-level key.

Nothing runs when the plan is refused — a `PLAN_INVALID` result carries no walk.

## Using it

- **Reachability stalled** (`states.notReached` non-empty, `ceilings.hit: 'exhausted'`): the path is longer than `depth`. Raise the specific pair, not `default` — `default` multiplies the whole frontier.
- **Ceiling tripped**: cut the enumeration. Replace the widest argument's levels with its endpoints, re-run, and confirm `ceilings.hit: 'exhausted'` before reading any "not reached".
- **An aliasing disclosure**: narrow the argument that writes the open field — `Twin.help('aliasing')`.
- **A promotion**: the plan is stored in the row's `_from`, in full and by hash. A counterexample is not re-derivable once the model is fixed, so the plan that found it is the ledger. `Rule.sequence.create` re-runs it and writes only when that walk ends `exhausted`.
