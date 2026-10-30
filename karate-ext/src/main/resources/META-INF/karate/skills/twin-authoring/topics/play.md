# Play — from a mock session to a pinned row

`Http.mock({twin: rule, record: true})` serves the model; every world it creates keeps a play record, listed in the instance's `records`.

| about to… | call |
|---|---|
| list what a world can do next | `Twin.next(rule, {record: 'play-…'})` — or a sequence id, or inline steps |
| see why an enabled command was refused | append the attempt to the steps and `Twin.run(rule, steps)` |
| pin a session as evidence | `Rule.sequence.create(rule, {promote: {play: 'play-…'}})` |
| act on a contradicted expectation | `Twin.check(rule)` → `findings.expectMismatch` |

The fields, the refusal order and the promoted row's shape: `Twin.help('play')`.

- `enabled` is the guard only. `apply` may still refuse, the wire is not consulted, and a model-only command is listed like any other.
- To ask about a later day, append the clock step to the history. Nothing advances the clock for you.
- You cannot confirm a record: a person confirms it in the console's play view, and no argument of the promote verb carries a confirmation. On `PLAY_UNCONFIRMED`, ask for the confirmation; do not retry.
- Playing a world further after its confirmation unconfirms the record.
- Supply only `promote`, `_id` and `_label` on the promote row. `steps`, `_expect`, `_req`, `_from` and `_ai` are the engine's.
- A promoted row is `_ai: true` and review-pending until a human commit clears `_ai`.
- `expectMismatch` fails `ci`. Decide from the requirement whether the model or the expectation is wrong, and change that one — never the expectation just to match what the model returns.
