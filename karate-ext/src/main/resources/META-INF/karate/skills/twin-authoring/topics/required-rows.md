# Required rows — the obligation an outside document carries in

Required rows are an **input, never ambient**. The engine has no opinion about what a run ought to reach; supply the obligation and it reports which rows went unobserved.

```js
Twin.check('policy', {required: {rows: [...], sha: '<sha of the source document>'}})
```

`sha` is optional — it is your claim about the document, echoed in the result and in the saved plan record so a verdict names the manifest it was judged against. Without `required` and without any `req:` on a command, `findings.requiredNotreached` is empty **by definition** — an empty findings list is then a statement about nothing.

A `req:` on a command is the in-model form of the same obligation: the command must fire at least once. Unreached, it reports as `{id, kind: 'req', status: 'notreached'}`, after the supplied rows. Its id must be a registered acceptance criterion — `Requirement.list()` shows them — or `Rule.twin.update` refuses `REQ_NOTFOUND` and writes nothing.

## The row

`{id, kind, prose?, state?, from?, to?, operation?, alternatives?, args?, world?, request?, outcome?, reason?, appliesTo?, predicate?}` — `id` and `kind` always; each kind reads its own subset, and any other key is `REQUIRED_INVALID`. `prose` is the obligation sentence, carried onto the row's witness and its finding.

| kind | fields it reads | observed when |
|---|---|---|
| `state` | `state` | some visited node's `states` carries it |
| `transition` | `from`, `to`, `operation` or `alternatives`, `args?`, `world?` | an edge — into a new or a visited node — runs the operation from a node in `from` to a node in `to` |
| `rejection` | `operation` or `alternatives`, `from?`, `args?`, `world?`, `reason?` | a refusal at stage `apply` names the operation from a node in `from` |
| `assertion` | `appliesTo` (transition row ids), `predicate` | at least one edge those transitions observed **and** every such edge satisfies `predicate(before, after, args)` |
| `invariant` | `predicate` | every visited node satisfies `predicate(world)` — the way to require that something is *never* recorded |

`args` and `world` are **witness subsets** — over the step's arguments and the before-world, by the same subset law as `_expect` — that tell apart rows sharing a `from → to` and an operation. A rejection's `reason` is reported beside the finding and never graded. A `predicate` is a function source string; one that does not compile is `REQUIRED_INVALID`, one that throws or returns a non-boolean is `failed`.

```js
{ id: 'R-1', kind: 'state',      state: 'cancelled' }

{ id: 'R-2', kind: 'transition', from: 'quoted', to: 'bound', operation: 'bind',
             args: { term: 12 } }

{ id: 'R-3', kind: 'rejection',  alternatives: ['bind', 'endorse'], from: 'cancelled',
             reason: 'policy is cancelled' }

{ id: 'R-4', kind: 'assertion',  appliesTo: ['R-2'],
             predicate: '(before, after, args) => after.premium >= before.premium' }

{ id: 'R-5', kind: 'invariant',  predicate: '(w) => w.premium === null || w.premium >= 0' }
```

## Naming the wire instead of the model

`state`/`from`/`to`/`operation` are **this twin's** names. A row written before the twin existed — a frozen manifest, another team's model — states the API instead: `operation` as `METHOD /path/{id}`, `request` as a subset of the body the command's `request` derived, `outcome` as `{kind: 'applied'|'refused', status?, reason?, body?}`. `from`/`to` and a command name are then optional.

```js
{ id: 'R-6', kind: 'rejection', prose: 'A zero-length meeting is refused.',
             operation: 'POST /meeting',
             request: { duration: { from: { time: '09:00:00' }, to: { time: '09:00:00' } } },
             outcome: { kind: 'refused', status: 400 } }
```

`status`/`body` are read **through the command's own `observe`**: you name the status the API documents, the engine translates it into this model's refusal. That is how two rows on one operation are told apart with no model-private subset. `outcome.reason` matches the recorded refusal text directly.

## Undeclared names, and the four traps

A row naming a state, command or operation the twin does not declare is **that row's** `notreached`, carrying `reason: 'undeclared:<what>:<name>'`. Every other row still grades. `REQUIRED_INVALID` means a malformed document — an unknown key, a row with no matchable key at all, an `appliesTo` naming no supplied row.

**`alternatives` is not a convenience.** A refusal a correct implementation may legitimately raise on either of two commands, matched on one command name, scores that implementation as a miss. Name every command the refusal may land on.

**An `assertion` never passes vacuously.** Zero applicable edges is `requiredNotreached`, not a pass. The applicable set includes noop edges and edges into merged worlds — both have concrete `before`/`after` worlds, so both are judged.

**A "never happens" claim is an `invariant`, not a rejection.** A rejection row is observed when a refusal exists; it cannot say that no visited world carries the outcome. `R-5` can.

**Rows that share their matching keys are one row.** With no `args`/`world`/`request`/`outcome` between them, a single step observes them all. Give each the subset that tells it apart, or they discriminate nothing.

## Reading the result

An unobserved row lands in `findings.requiredNotreached`, and that is the **only** finding a tripped ceiling can cause. When it fires, check `ceilings.hit` first:

- `exhausted` — the row is genuinely unreachable in the declared model. Fix the twin, or the row is wrong.
- anything else — the walk stopped early. Narrow the argument domains or raise the depth for the pair on the path (`Skill.help('twin-authoring', 'plan')`), then re-run before concluding anything.

`requiredNotreached` fails CI. Everything in `findings.incomplete` does not — see `Skill.help('twin-authoring', 'findings')`.
