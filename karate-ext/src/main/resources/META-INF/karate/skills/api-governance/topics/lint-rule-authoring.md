# Writing your own lint rules

> The bundled rulepack is generic API hygiene. **Your style guide is yours to write** — and the rules worth
> having are the ones nobody could express before: the ones that reach past the document into the coverage
> graph. This topic is the authoring reference; `Openapi.help()` is the live one, and always wins.

## A rule is data with a code predicate

```js
var rules = {
  'operation-description-required': {
    given: '$.paths.*.*',                 // WHAT to look at
    givenType: 'jsonpath-plus',           // in which selector dialect
    severity: 'warn',                     // error | warn | info
    dimension: 'documentation',           // WHICH scorecard axis it grades  (default: design)
    then: function (node, ctx) {          // the predicate — reads like a `match`
      if (node.operationId && !node.description) {
        return 'operation "' + node.operationId + '" has no description';
      }
    }
  }
};
Openapi.lint('openapi.yaml', { rules: rules });
```

**The whole key list** (an unknown key is rejected with a did-you-mean, never silently ignored):
`id` · `given` · `givenType` · `severity` · `message` · `description` · `documentationUrl` · `formats` ·
`dimension` · `cap` · `params` · `anchor` · `resolved` · `provenance` · `origin` · `waiver` · `then`.
Only `given` and `then` are required.

### `given` — the selector, in one of two dialects

| `givenType` | is |
|---|---|
| `jsonpath-plus` | the full JSONPath-Plus dialect — `$.paths.*.*`, `~` for keys, `^` for the parent, `@property`, filter expressions |
| `js` | a plain JS expression with the document bound as `$`, returning the nodes to grade. `Object.keys($.paths)` **is** the keys selector — reach for this when the selection is easier to *compute* than to express |

**A string `given` MUST declare its `givenType` — there is no default and it is never sniffed**, because
`$.paths` parses as both dialects and means different node sets in each. Omitting it is a loud loader
error, not a guess.

### `then` — the predicate

`(node, ctx) => …`, and the return value is the verdict:

- **return nothing** → this node passes. (The common case; don't return `true`.)
- **return a string** → one finding with that message.
- **return `{message, path}`** → a finding pointed at a different node than the one selected.
- **return a list of either** → several findings about one subject.

`ctx` carries `{path, pointer, key, parent, document, params, rule, severity, format, graph}` — where
**`ctx.graph` is the boolean a graph-reaching rule cannot live without** (see below) and `ctx.format` is the
document's detected format.

### The three fields that quietly change the outcome

1. **`dimension` defaults to `design`.** A documentation rule you forget to label grades the *design* axis
   and moves the wrong number. Label every rule.
2. **`anchor: 'key'`** puts the reported position on the key rather than the value — which is what you want
   for a casing or unsupported-keyword rule. The default is `value`.
3. **`resolved` defaults to `true`** — a rule grades the **`$ref`-resolved** view unless you say otherwise,
   which is right when the rule is about the *effective* schema. Declare **`resolved: false`** when the rule
   is about how the document is *written* (`$ref` hygiene, siblings, unreferenced components, key casing at
   the site) — on the resolved view those subjects have already been substituted away.

A **`cap`** (a score in `[0,1]`) makes the rule a *gating* one: an unwaived finding holds its whole
dimension at or below that score however good everything else is, and a bound cap is also a gate — so use
it for the handful of things that genuinely stop a release, not for house preferences.

## The helper library — reach for `fn` before writing the logic

Thirteen pure predicates over their arguments and the bound document.

**They share `then`'s return convention: `null` means PASS, a message string (or a list of findings) means
FAIL.** So the idiom is `return fn.casing(x, {type:'camel'})` — **return it directly, never negate it**.
`if (!fn.casing(...))` is inverted and will fire on everything that passes. Every helper that *takes*
options takes an **object**, never a bare string (the four value-only ones take none), and throws a loud
`LintException` if a required option is missing.

**On a null/absent input they do not all agree, so guard rather than assume.** The four *string/shape*
helpers — `pattern` `casing` `length` `enumeration` — pass on null, because absence is `fn.defined`'s
question and not theirs. But `fn.xor` and `fn.or` **fail** on a null or non-object node (nothing is
present, so "at least one of …" is unsatisfied), and `fn.schema` **validates** null against your schema
rather than skipping it. If the node may legitimately be absent, say so yourself: `if (!node) return;`.

| helper | signature | fails when |
|---|---|---|
| `fn.truthy(v)` · `fn.falsy(v)` | value only | the value is falsy / truthy |
| `fn.defined(v)` · `fn.undefined(v)` | value only | the value is absent / present |
| `fn.pattern(v, {match?, notMatch?})` | at least one of the two (ECMA regex source) | `match` does not find, or `notMatch` does |
| `fn.casing(v, {type, disallowDigits?, separator?})` | `type` is one of `flat` `camel` `pascal` `kebab` `cobol` `snake` `macro`; `separator: {char, allowLeading?}` is the `x-` prefix tail | the value is not in that case |
| `fn.length(v, {min?, max?})` | at least one bound; grades a number by value, a list by size, an object by property count, anything else by string length | out of bounds |
| `fn.enumeration(v, {values})` | a non-empty list | the value is not one of them |
| `fn.alphabetical(v, {keyedBy?})` | a list (optionally keyed by a member) or an object's keys | reports **each** break in sort order, not just the first |
| `fn.schema(v, {schema, dialect?, assertFormat?})` | a JSON Schema | the value does not validate — findings carry `keyword`/`instancePath`/`expected`/`actual` |
| `fn.xor(node, {properties})` · `fn.or(node, {properties})` | property names on the selected object | not exactly one present / none present |
| `fn.unreferencedReusableObject(node, {reusableObjectsLocation})` | e.g. `'#/components/schemas'` | a member of that map is referenced by nothing |

`fn.schema` is the shipped JSON Schema validator's non-throwing form — not a second implementation.

Probing one directly is the fastest way to confirm a shape — the helpers are bound in the rule sandbox, so
do it from inside a throwaway rule's `then`, or just read the exception: a wrong option shape says exactly
what it wanted (`fn.casing needs {type} — one of flat/camel/pascal/…`).

## The sandbox — what a rule may touch

Rules run in an **enumerated** sandbox, and the result's `capabilities` block states the terms so an auditor
can read what the rules were allowed to do. Granted: the document (a private copy — a rule cannot rewrite
what the *next* rule is graded against), the read-only `Coverage` graph, `fn`, and a write-only `console`.
Denied by construction: filesystem, network, process, Java interop.

The `Coverage` grant is verb-by-verb: `help` `summary` `sources` `items` `status` `gaps` `tests` `hits`
`dimensions` `coveringArray`. **`Coverage.path` is excluded by name** — it hands back a filesystem path, and
a sandbox that denies the filesystem must not leak one through a read verb.

## The graph-reaching rules — the whole point

These are the questions no document check can ask. The mechanism is simply that `Coverage` is in scope —
**and the first line of the predicate is always the same guard**, for a reason that is not optional:

```js
var rules = {
  'operation-claimed-by-a-requirement': {
    given: '$.paths.*.*',
    givenType: 'jsonpath-plus',
    severity: 'error',
    dimension: 'provenness',
    then: function (node, ctx) {
      if (!ctx.graph) return;              // NO GRAPH — ask nothing. See below; never omit this.
      if (!node.operationId) return;
      var s = Coverage.status('openapi:' + node.operationId);
      if (!s || !s.requirements || s.requirements.length === 0) {
        return 'operation "' + node.operationId + '" is claimed by no requirement';
      }
    }
  }
};
```

**Why `ctx.graph` and not a `Coverage` check.** With no graph on disk, *every* `Coverage` verb answers an
error map — and with a graph, an unknown id answers the same way. So a predicate guarding only on the
response cannot tell *"there was nothing to ask"* from *"I asked, and the graph has never heard of this
operation"* — and the second is exactly the orphan the rule exists to catch. `ctx.graph` is the one fact
that distinguishes them, which is why every bundled graph-reaching rule opens with it.

**Omit the guard and the rule does the opposite of nothing.** On a graphless project the lookup fails for
every operation, so the rule raises a finding on *all* of them. At `severity: 'error'` that opens the
`unwaived-errors` gate and drives the whole verdict to `NOT_READY` — manufactured out of an absent graph.
The engine will still mark the rule inert (so it does not *grade*), but its findings are already in
`findings[]` and counted in `summary.errors`. Inert must mean *silent*, and only your guard makes it so.

`Coverage.status('openapi:<operationId>')` returns
`{id, status, callCount, tests, requirements, oracleOnly, mockOnly}` — which is enough to ask *claimed?*
(`requirements`), *exercised?* (`callCount`/`status`), and *did anything but our own mock answer?*
(`mockOnly`). Those three are exactly what the bundled `operation-claimed-by-requirement`,
`operation-exercised` and `operation-not-mock-only` rules ask — read them as the reference shape.

### The trap: zero findings can mean *inert*, not *clean*

A graph-reaching rule over a project with **no coverage graph** raises nothing — and so does one over a
project where everything is fine. They are opposite outcomes with an identical finding count. Before you
report a pass, check the disclosure:

```js
var l = Openapi.lint('openapi.yaml');
l.graph;   // { available, graphReachingRules: [...], note? }
```

`available:false` with a `note` means those rules asked nothing. Fix it by writing the graph —
**`Report.aggregate()`**, or any settled console run, which folds into it. See the parent skill, step 3.

The same trap bites while *developing* a rule: a condition that happens to be vacuously false today
(`mockOnly` on a graph where nothing has ever been called) looks exactly like a rule that isn't wired up.
Prove the rule fires by first pointing it at a condition you know is true right now, then tighten it.

## Declare them once, in `karate-boot.js`

`opts.rules` is for a one-off check. The project's law belongs in the file it already boots from, beside
`cov.criticalityPolicy` and `cov.real`:

```js
// karate-boot.js
const cov = boot.ext('coverage');
cov.openapi = 'openapi.yaml';
cov.lintRules = {
  'operation-description-required': { /* … */ }
};
```

Every surface that lints — console, MCP, curl, CI — then honours them without being handed anything.
**Precedence is nearest-scope-wins by id:** bundled pack → `cov.lintRules` → `opts.rules`.

If that file cannot be evaluated, the result carries a `project` block saying so, and it **gates the grade**
rather than reading clean — because we cannot know which of your rules did not run. A malformed rule in it
is a rulebook error, not a finding.

## Retuning or standing down a bundled rule

There is no "disable" flag, and deliberately so — you **redeclare the id**, which is a visible act. What
each form does to the number is different, so pick on the effect you actually want:

```js
// RETUNE — same rule, different threshold. Still grades, at its own severity.
Openapi.lint('openapi.yaml', { rules: { 'schema-string-bounded': { /* …, params: {max: 4096} */ } } });

// DOWN-WEIGHT — severity IS the rule's weight (error 3 · warn 2 · info 1), so redeclaring at `info`
// keeps it reported and keeps it grading, at a third of an error's pull. It does not switch it off.
Openapi.lint('openapi.yaml', { rules: { 'tags-alphabetical': { given: '$.tags',
    givenType: 'jsonpath-plus', severity: 'info',
    then: function (node) { return fn.alphabetical(node, { keyedBy: 'name' }); } } } });

// GENUINELY OFF — give it a selector that matches nothing. A rule that selected zero nodes has no
// proportion to take, so it is counted inert: it neither credits nor penalises the dimension.
Openapi.lint('openapi.yaml', { rules: { 'tags-alphabetical': { given: '$.__off__',
    givenType: 'jsonpath-plus', then: function () {} } } });

// drop the bundled pack entirely — useful while developing YOUR rules, so you see only your findings
Openapi.lint('openapi.yaml', { rulepack: false, rules: rules });
```

**Do not "turn a rule off" with an empty `then` over a real selector.** It never fires, so the rule passes
over every node it selected and **credits** the dimension — you have not silenced it, you have given
yourself free marks. Off means *selecting nothing*; the engine's own definition of inert.

## The authoring loop

1. Write the rule and run it with `{rulepack: false}` so the output is only yours.
2. Confirm it fires — right count, right operations, and the `line`/`col` land on the construct you meant
   (check `position.exact`; a finding reached through a `$ref` reports `exact:false` and says so).
3. Confirm it *stops* firing against a fixed document. A rule that never passes is not a rule.
4. Label the `dimension`, and decide whether it deserves a `cap`.
5. Move it into `cov.lintRules` and re-grade — the number it moves should be the one you expected.
