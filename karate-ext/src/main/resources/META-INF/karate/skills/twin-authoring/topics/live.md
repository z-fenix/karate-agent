# Live — replaying a pinned sequence against a running system

Pin the required rows' witnesses by promotion (`promote: {required}`) before `Twin.live`; author a step list only for an order no row demands.

## 1. Declare the protocol on the twin

```js
t.readBack({ // must reproduce the root, or the run is INVALID
  request: w => ({ method: 'GET', path: w.id ? '/quotes/' + w.id : '/quotes' }),
  observe: r => r.status === 200 ? { status: r.body.status, policyNumber: r.body.policyNumber }
                 : { status: 'draft' }, // no quote yet — the root's partial world
  required: ['status'],
  entity: 'body.id',              // any exchange body carrying this value is the same resource
  volatile: ['body.updatedAt'],   // declared noise, reported beside the findings, never among them
  arrays: { 'body.items': 'id' }  // flatten by identity; every other array is one atomic value
});

t.reset(w => ({ method: 'POST', path: '/reset' })); // optional; response not classified

const reasons = { NOT_QUOTED: 'not quoted yet', // wire code → the model's own reason
                  QUOTE_EXPIRED: 'quote expired' };

t.command('bind', {
  when: w => w.status === 'quoted',
  apply: w => { if (w.expired) t.reject('quote expired'); w.status = 'bound'; },
  then: (before, after) => after.status === 'bound',
  request: w => ({ method: 'POST', path: '/quotes/' + w.id + '/bind' }),
  observe: r => r.status === 200 ? { kind: 'applied' }
                 : r.status === 409 ? { kind: 'refused', reason: reasons[r.body.code] }
                 : { kind: 'unknown' },
  refusals: { guard: reasons.NOT_QUOTED },
  captures: ['policyNumber'],
  capture: (w, r) => { w.policyNumber = r.body.policyNumber; },
  indefinite: { transportFailure: ['applied', 'unchanged'] }
});
```

`reason` is the **model's** string — a `t.reject` reason or `refusals.guard`; a model refusal explains a live one only on that reason, and a `refused` with no string `reason` reads `unknown`, explaining no candidate.

Take the wire codes and the observation window off the run just made, never by probing: `summary.wireRefusals[{command, status, body, reasons[]}]` and `summary.observation` — wire paths counted `read`/`unread`, model fields `observed[]`, one `findings[]` row per unobserved body path that moved within one resource.

Each is `TWIN_INVALID` at load: `request` requires `observe`, `captures` requires `capture` and holds world paths (`items[*].id` — name the leaf), `t.reset` requires `t.readBack`. An omitted `required` field reproduces nothing; no `t.readBack` assumes the system at the root. The **canonical request** is `{method, path, body?}` only — upper-case ASCII `method`, relative `path` starting `/`, `body` canonical JSON or absent — sent to `baseUrl + path` as UTF-8 `application/json`, no other header.

`indefinite` lists what a transport failure admits; one on a command declaring none grades and accuses nothing.

## 2. Run it — mock first, then the system

```js
Twin.live('policy', {baseUrl: 'http://localhost:8097', against: 'mock'})
```

`against ∈ mock | live` is required — your claim: the engine cannot tell a mock from staging. Each sequence runs from reset; one disposition never stops the next. A command with no `request` is `LIVE_UNDECLARED {command}`. Options and envelope: `Twin.help('live')`.

`Twin.allows('policy', {sequenceId, log})` grades a recorded log at the row's ceiling, no system; a malformed log is `TRACE_INVALID`.

## 3. Read the disposition

`PASS` counts in `evidence.<against>` · `FAIL` none survived (`refusalReasonMismatch`), or the read-back contradicted every survivor — adjudicate it, `disposition.evidence` names the entry · `UNRESOLVED` a non-singleton profile or an unresolved capture — declare a state, or capture the field · `NOTCHECKED` every survivor stood only on a model error — fix the twin or the rulebook that step called · `REFUSED` `AMBIGUOUS_REQUEST` or `PROFILE_OVERCAP` — tighten the guard or read-back · `INVALID` root unreproduced, a transport failure with no `indefinite`, a non-2xx reset (`RESET_REFUSED`) or `RESET_HOOK_FAILED` — fix the reset or read-back, or declare `indefinite`.

A `then` or `t.always` failing on a live successor lands in the entry's `properties`, moving no disposition.

## 4. Adjudicate a `FAIL`

```js
Twin.adjudicate('policy', '<evidence>', {disposition: 'system', note: 'bind returns 500 on retry'})
```

`evidence` is an entry key of the current `.baseline/live.json`, else `ADJUDICATION_UNKNOWN`. `system` leaves the `liveMismatch` failing CI until the system changes · `model` moves it to `incomplete {kind:'adjudicatedModel'}` and the twin owes an edit · `open` records that nobody decided. One record per key (a second is `ADJUDICATION_EXISTS`), append-only: reversing one is a hand edit.

## 5. Re-run `Twin.check`

```js
Twin.check('policy') // {evidence{model, oracle, mock, live}, findings, ci}
```

`check` re-folds each log against the current twin and derives every grade itself, never from a stored disposition. `evidence.mock`/`evidence.live` are each `{count, of: pinned sequences}`; a sequence with no run is ungraded, undisclosed and counted. A `FAIL` is `liveMismatch` and fails CI; `findings.unobservedVariation` never joins the union. `incomplete {kind:'liveUngraded', sequenceId, reason}` carries the ungradeable disposition, or one of:

`VERSION` an earlier `version`, so every pinned sequence reads as no run · `STALE` the twin, the row or a rulebook moved since the run — both re-run `Twin.live` · `UNREPRODUCED` the re-fold did not finish, or did not repeat the run's rulebook calls — make the fold terminate, or pin what its rulebooks do · `CORRUPT` a later `version`, a run off its recorded shape, a `runSha` that does not hash the run, or a repeated fold that disagrees — hand-edited, restore it from git and re-run.

`Rule.report('policy', {plan, sutRevision})` renders each pinned sequence's run and shrink under that plan — pass what `Twin.witness`/`Twin.orders` ran with.

`Rule.bless`/`Rule.drift` ignore the live record: a live run is evidence, not baseline.
