# Twin authoring — the behaviour model (`twin.js`) the engine walks

Write the state machine behind a rulebook as pure JS. The engine walks it under frozen ceilings and reports what it reached, what it never reached, where an invariant broke, and how much a rulebook decided. A walk is evidence about the declaration, bounded by its ceilings; it never proves the system.

| about to… | call |
|---|---|
| write or fix an exploration plan | `Skill.help('twin-authoring', 'plan')` |
| supply the rows a run must reach | `Skill.help('twin-authoring', 'required-rows')` |
| save, promote or replay a sequence | `Skill.help('twin-authoring', 'sequences')` |
| act on anything `Twin.check` reported | `Skill.help('twin-authoring', 'findings')` |
| replay a sequence against a running system | `Skill.help('twin-authoring', 'live')` |
| bind a twin to the wire and project its contract | `Skill.help('twin-authoring', 'binding')` |
| ask what a world can do next, or pin a mock session | `Skill.help('twin-authoring', 'play')` |

The concepts — the state-only guard, the `undefined` rail, the coverage boundary, the aliasing disclosure — are `Twin.help()`.

## The file

`twin.js` is the fifth file of a rulebook, beside `schema.js` · `generator.js` · `calc.js` · `scenarios.json`. It inherits the rulebook's lifecycle whole: the `mtimes` hash, the drift baseline, `Rule.bless`.

```js
t.init(w => { w.status = 'draft'; w.premium = 0; w.day = 0; });

t.state('draft',  w => w.status === 'draft');
t.state('bound',  w => w.status === 'bound');

t.clock('day');

t.command('rate', {
  when:  w => w.status === 'draft',
  args:  a => { a.int('drivers', 1, 3, [1, 2, 3]); a.enum('day', [0, 40]); },
  apply: (w, a) => {
    const r = t.calc({ drivers: a.drivers, day: a.day });
    w.premium = r.output.premium; w.status = 'quoted';
  },
  then:  (before, after) => after.premium > 0,
  req:   'POL-003'
});

t.command('bind', {
  when:  w => w.status === 'quoted',
  args:  [{ term: 6 }, { term: 12 }],
  apply: (w, a) => {
    if (a.term > 6 && w.premium > 250) t.reject('term too long for this premium');
    w.status = 'bound'; w.term = a.term;
  },
  request:  (w, a) => ({ method: 'POST', path: '/quote/bind', body: { term: a.term } }),
  observe:  r => r.status === 200 ? { kind: 'applied' } : { kind: 'refused', reason: r.body.reason },
  refusals: { guard: 'not_quoted' }
});

t.readBack({                                        // live-only, and it must reproduce the root
  request:  w => ({ method: 'GET', path: '/quote' }),
  observe:  r => ({ status: r.body.status }),
  required: ['status']
});

t.always('premium never negative', w => w.premium >= 0);
```

| call | rule |
|---|---|
| `t.init(w => …)` | roots one declared fixture world — canonical JSON, no functions or handles |
| `t.state(id, w => …)` | a predicate, and a **reporting projection** — never the walk's identity |
| `t.command(id, {when, scopeGuard?, args?, apply, then?, req?, idempotent?, request?, observe?, refusals?, captures?, capture?, indefinite?})` | one transition; with no `args` it runs once per world with `{}`; the last five are live-only |
| `t.reset(w => request)` | live-only — the request that puts the system at the root; it requires `t.readBack`, and declaring neither assumes the root |
| `t.readBack({request, observe, required: [field…], oracle?: [field…]})` | live-only — `w => request` and `r => partialWorld`; an omitted `required` field reproduces nothing; `oracle` names the `required` fields a rulebook computes, which is what `Twin.mutate` splits raw kills from order ones on |
| `t.always(name, w => …)` | an invariant, run after every step in every state |
| `t.clock(field)` | declares the clock; its value rides in `args`, and every step runs advance → guard → apply |
| `t.reject('<reason>')` | inside `apply` only — an argument-dependent refusal |
| `t.calc(input, opts?)` | inside `apply` only — runs the rulebook's `calc.js` on `input` (the rulebook's own input; `Rule.load(name).schema` is its shape) and returns `{outcome, output}`; a rulebook refusal **throws** here, where `Rule.execute` returns `{error, code, status}` as data; `opts.rule` names another rulebook in the same home |

- `when` is **state-only**: it never sees `args`; with no `when` a command is enabled everywhere. A `when` that disables a command records a stage-`guard` refusal, which no `rejection` row matches — raise a refusal a required row must reach inside `apply`, with `t.reject`.
- `apply` mutates the draft and returns exactly `undefined`; `null` or a trailing assignment is refused at stage `returnValue`.
- `t.calc`'s result enters the world as data (`w.premium = r.output.premium`), and time enters the rulebook as an `input` field — the facade passes no clock. A step that called it at least once with no call throwing is graded `oracle`; a caught throw leaves the step `bare`, an uncaught one errors it and the walk's error entry carries the rulebook's `code`. Outside `apply` — in `when`, `then`, `t.always` and at load — the call is `CALC_OUTSIDE_APPLY`.
- `then(before, after, a)` is that transition's postcondition; a failure is a counterexample.
- `args` declares the axes: the statement form (`a.enum(name, [v, …])` · `a.int(name, lo, hi, levels)` · `a.bool(name)` · `a.const(name, v)`) or a literal array of rows. A row reaches `apply` as a plain canonical-JSON copy, safe to store in the world (`w.sub = a`); rows are **indivisible**, ordered, and heterogeneous keys never become axes.
- `a.int`'s `levels` are required and are what the walk enumerates — a non-empty list of integers inside `[lo, hi]`. Only `lo == hi` implies `[lo]`: the engine never samples a universe for you.
- `example: {name: value}` is the argument values a playground form prefills (provenance `example`); each must lie in its statement-form domain, else `TWIN_INVALID` at load. The walk never reads it.
- The live members — `request` · `observe` · `refusals` · `captures` with `capture` · `indefinite` — and `t.reset`/`t.readBack` are read only by `Twin.live`; a walk invokes none of them. `request` without `observe`, `captures` without `capture`, and `t.reset` without `t.readBack` are `TWIN_INVALID` at load. `captures` lists **world paths** — `quoteId`, `quote.id`, `items[*].id`; a malformed one is `TWIN_INVALID` too. Name the leaf the service invents, not the collection holding it: `Twin.witness` masks exactly the leaves a path addresses, so `['items']` declares every field under `items` invented and collapses those edges into one obligation. Their signatures and the protocol: `Skill.help('twin-authoring', 'live')`.

## The loop

```js
Rule.twin.update('policy', twinJs)                 // twinJs is the source as a JS string — a template
                                                   // literal holding no backtick and no ${ of its own;
                                                   // TWIN_INVALID refuses, and nothing is written
Twin.explore('policy')                             // the walk
```

Read the summary in this order and act:

| reading | do |
|---|---|
| `ceilings.hit != 'exhausted'` | nothing about reachability is claimable — cut the argument domains, or add a plan |
| `states.notReached` / `transitions.notObserved` | a guard is unsatisfiable from the root, or the path needs more depth — fix the twin, or raise that pair's depth in the plan |
| `counterexamples[]` | an invariant or postcondition failed — the model is wrong, or the rule is |
| `fidelity.undeclaredOpenFields` | `Twin.help('aliasing')` — two moves, pick one |

Then pin what matters and take the verdict:

```js
Rule.sequence.create('policy', {promote: {plan: {}, counterexample: '<id>'}, _label: '…'})
Twin.check('policy', {required: {rows: […], sha: '<sha>'}})   // {evidence, findings, ci}
Rule.bless('policy')                                          // once ci.pass is true
Twin.run('policy', 'seq-…')                                   // replay one row under the current model
Twin.next('policy', 'seq-…')                                  // the commands its final world enables
```

`Rule.bless` writes the baseline the next `Twin.check` reads: `twin`, `sequences[]`, `twinSha`. `Twin.check` never blesses.

To check the model against a system you did not generate, replay the pinned sequences against it:

```js
Twin.live('policy', {baseUrl: 'http://localhost:8080', against: 'mock'})  // then against: 'live'
Twin.adjudicate('policy', '<evidence>', {disposition: 'system'})          // whose defect a FAIL is
Twin.allows('policy', {sequenceId: 'seq-…', log: row.log})                // grade a recorded log, no system
Twin.mutate('policy', {handlers: '/mock/handlers.js'})                    // how much of a mock's guards
                                                                         // the deck catches — Twin.help('mutate')
```

Each sequence gets one disposition in `.baseline/live.json`, and the next `Twin.check` re-folds it into `evidence.mock`/`evidence.live`, `liveMismatch` and `incomplete {kind:'liveUngraded'}`. The declarations a run needs, the six dispositions, the three adjudications and grading a recorded log: `Skill.help('twin-authoring', 'live')`.

## The plan, in ten lines

```jsonc
{ "depth":  { "default": 2, "endorse→cancel": 5 },
  "levels": { "rate.drivers": [1, 3] },
  "focus":  ["POL-007"] }
```

`depth` bounds the walk, per pair `'<from>→<to>'` or by `default` — with no plan the walk runs at `default` 12 (the frozen `maxDepth`), every declared level, and no `focus`. `levels` **replaces** the declared levels of one `'<command>.<argument>'` path; every value must be a member of the declared domain — an integer inside `a.int`'s bounds, a member of `a.enum`'s list — and the bounds themselves never move. `focus` names supplied `required` row ids or `req:` ids. The plan is data: `Twin.explore`/`Twin.check` execute it deterministically and save it beside the result, so the run is byte-reproducible without the model that proposed it. Every cause of `PLAN_INVALID` is `Skill.help('twin-authoring', 'plan')`.

## `_expect`, in six lines

```jsonc
"_expect": { "steps": [ { "at": 2, "outcome": "refused", "stage": "apply" } ],
             "final": { "status": "quoted" } }
```

Optional and sparse: `at` is a zero-based step index, `outcome ∈ applied | refused | error` is exact, `world` and `final` are **subsets**. An object matches when every expected key is present and matches recursively; an array matches only at exact length, positionally; an absent key is distinct from a present `null`. An index with no entry asserts nothing. Full rules: `Skill.help('twin-authoring', 'sequences')`.

## Honesty rules

- `ceilings.hit != 'exhausted'` is a **disclosure you must carry**, not a footnote to drop. Nothing was proven unreachable.
- A green `Twin.check` carrying `incomplete {kind:'neverBlessed'}` is not a baseline — `sequenceDrift` is `[]` because nothing has been blessed, not because nothing drifted.
- Never hand-edit `_from`, and never mint it: it is engine-written, and a `create` carrying one is `SEQUENCE_INVALID`.
- Never write an `_expect` by copying what the model just returned — an expectation is worth having exactly where it can disagree with the model. Derive it from the requirement.
- `PASS` proves the declaration held, not that the system conforms. Report `evidence.oracle` as the `{count, of}` the walk produced: applied steps whose `apply` reached the rulebook with no call throwing, over every applied step.
- Grades never blend into one percentage. Report a number with its grade or not at all.
