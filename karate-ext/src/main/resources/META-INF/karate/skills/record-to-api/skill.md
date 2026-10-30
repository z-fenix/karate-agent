# Karate — a recording's calls → a verified API test

Drive the console by POSTing JavaScript as the request body to `<console>/api/eval` (or the MCP `eval` tool); one engine keeps your variables across calls. Each namespace's `help()` lists its options: `Traffic.help()`, `ApiTest.help('land')`.

## The route

1. `Traffic.read('<runId>')` — the recorded calls, masked as written: a token reads `‹masked:k1›`, a typed password `‹secret:NAME›`.
2. `Traffic.sift('<runId>')` → `kept`, `left` (each with its `reason`). Move a call with `{moves: [{id, to: 'kept'|'left', by: 'chat', reason}]}`, and pass every move on each later call.
3. `var m = Traffic.migrate('<runId>', {moves})` → `{to, needs, reads, chains, proposed, next, feature}`: the candidate, written nowhere. Keep `m.feature` in a variable; edit it as a string.
4. Do each `ApiTest.config(…)` that `m.next` names. `m.reads` are set already; set each `m.needs` entry: `ApiTest.config({name, value, ifMatch})`, a credential `{name, secret: 'NAME', ifMatch}` with a name `Secret.list()` holds (else ask the operator for it). `ApiTest.config()` → `version`; each call answers the next.
5. Before landing, put each `proposed` row's `text` you keep into its `scenario` before step `at` (0-based in the scenario). A `chains` row with `guessed` and a `from`: `* def` the value after that call and read it where the row's `to` points; with no `from`, keep the literal only if it is the same every run.
6. `var r = ApiTest.land(m.to, src)` — runs `src` at `m.to`; only a green run writes it, `@ai`-marked. A red writes nothing and asks no one: `ApiTest.exchanges(m.to, {runId: r.runId})` shows each call's request and response; change `src` and land again. A file that exists already needs `{ifMatch: version}` (`ApiTest.read(m.to).version`); `File.read(m.to)` is its text.
7. Link criteria from `Requirement.list()` once their outcomes are asserted: `ApiTest.link(m.to, {scenario, tags: ['@req=ID/1'], ifMatch: r.version})` — a tag-only change, which needs no run.

## The rules

{{rules}}
- Keep the candidate source off disk until `ApiTest.land`: `File.write` and the `ApiTest` edit and create verbs write a feature at once, unrun.
- Never put a left-out call back by hand: re-migrate with a move.
- `Http.*` probes answer real responses, tokens included: never copy one into the source.
