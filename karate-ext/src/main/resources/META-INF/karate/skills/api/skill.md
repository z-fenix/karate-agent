# Karate API — REST Skill

> **Stack:** karate-core; runs over JDK 21+; no addon required for REST.
> **Prerequisites:** `Skill.help('getting-started')` for project setup; `Skill.help('assertions')` for `match` semantics.
> **Companion docs:** karate-docs `/http-requests/*`, `/http-responses/*`, `/reusability/calling-features`.

## What this skill is for

Author **end-to-end REST API tests** in Karate. The skill teaches an LLM to:

1. Read an OpenAPI spec, curl trace, HAR file, or English description → emit `.feature` files with one scenario per use case.
2. Pick the right step keyword: `path`, `url`, `param`, `header`, `request`, `method`, `multipart file`, `form field`, etc.
3. Structure shared setup with `Background:` + `configure headers` + `karate-config.js`.
4. Reuse with `call`, `callonce`, `karate.callSingle`.
5. Authenticate: bearer / OAuth2 / basic / mTLS.
6. Drive **data-driven** tests with `Scenario Outline:` and external `Examples:`.
7. Poll for async outcomes with `retry until` and the `karate.signal` / `karate.listen` pair.
8. Tag for filtering (`@smoke`, `@regression`) and for the feature-files-as-TCM model (`@req=…`, `@noreq`).

## Mental model

A Karate scenario is a **stateful sequence of HTTP calls**. The state lives in variables (`* def x = …`) and in two implicit fields:

- `request` / `response` — body of the most recent call.
- `responseStatus` / `responseHeaders` / `responseCookies` / `responseTime` — metadata.

You build a request piece-by-piece (`path`, `param`, `header`, `request`), fire it (`method <verb>`), then assert the response with `match`. The next step in the scenario starts from where the previous left off — `path` and `param` get reset between requests, but `url` and `configure`-set headers persist.

```
   Background:    (runs before every scenario)
     ├─ * url baseUrl
     └─ * configure headers = { ... }

   Scenario:
     ├─ Given path 'users', '42'    ← build request
     ├─ And request {name: 'Alice'}
     ├─ When method post            ← fire request; populates `response`
     ├─ Then status 201             ← assert metadata
     ├─ And match response == ...   ← assert body
     └─ * def newId = response.id   ← bind for next step
```

## Quick start

```feature
@policy
Feature: Policy quotes

Background:
  * url baseUrl
  * configure headers = { Authorization: 'Bearer ' + token }

@req=POL-001
Scenario: quote endpoint returns a price for a valid driver
  Given path 'quote'
  And request { age: 35, postcode: 'SW1A 1AA' }
  When method post
  Then status 200
  And match response == { price: '#number', currency: 'GBP', expiresAt: '#datetime' }
  * def quoteId = response.id

@req=POL-002
Scenario: quote endpoint rejects under-age driver
  Given path 'quote'
  And request { age: 16, postcode: 'SW1A 1AA' }
  When method post
  Then status 400
  And match response contains { error: 'age_below_minimum' }
```

Project-level config (`karate-config.js`) provides `baseUrl` and `token` per env:

```javascript
function fn() {
  var env = karate.env || 'dev';
  return {
    dev:  { baseUrl: 'http://localhost:8080', token: 'dev-token' },
    prod: {
      baseUrl: karate.sysenv('API_BASE_URL', 'https://api.example.com'),
      token:   karate.sysprop('prod.token')  // -Dprod.token=…
    }
  }[env];
}
```

`karate.sysenv(name, [default])` reads OS env vars; `karate.sysprop(name, [default])` reads `-D` JVM system properties. Optional second arg = fallback when unset or empty.

## Explore an unfamiliar API first, then write the test (`Http.*`)

When you **don't** have a trustworthy spec — a mock you just stood up, or an under-documented real /
third-party backend — **explore the live surface first**, infer its rules and stateful behavior, then
crystallize what you learn into a `.feature`. The `Http.*` namespace is the probe (prefer it over raw
`curl` for anything iterative; a one-off `curl` is fine):

- **token-frugal** — a large list comes back as a `bodyShape` (`{array:200, of:{id:'string',…}}`) + a
  small `sample`, not 200 objects flooding your context (`{full:true}` for the whole body);
- **stateful** — cookies/session auth persist across calls automatically (`Http.cookies()` to inspect);
- **header injection** — `Http.configure({headers: function(){ return {Authorization:'Bearer '+token} }})`
  re-applies a refreshed token to every call;
- **transparent** — every response carries `request:{method,url,headers}` so you can verify exactly what
  was sent (the injected cookie/auth) — `curl -v`, but structured and composable (`r.body.id`).

The loop (the same whether the target is your mock or a real API — this is the reverse-engineering front door):

```js
Http.configure({ baseUrl: mockUrl });
Http.get('/users');                                     // (1) list — learn the resource + field shape
Http.post('/users', { name: 'Bob' });                   // (2) probe a rule → 400 {error:'email is required'}
Http.post('/users', { name: 'Bob', email: 'b@x.com' }); // (3) happy path → 201, learn server defaults (role,id)
Http.get('/users/2');                                   // (4) read-back → proves it's stateful, not canned
```

From those four calls you infer: *email is required* (a business rule), *the server assigns `id` and
defaults `role:'member'`/`active:true`*, and *state persists*. Now write the `.feature` that locks each
finding (below). This is the explorer the future security/OWASP harness loop reuses — same surface.

## "Don't invent" means don't guess the contract — it does **not** mean don't write

A careful brief often says *"do not invent endpoints, payloads, ids, or fixtures."* That is right about the
**contract** — never guess a URL, a field name, a status code. It is **wrong** if read as *"don't create
data / only GET."* Supplying a realistic request body to a `POST` and asserting what comes back is
**grounding, not invention** — it is how you learn the real shape and prove the write path. The strongest
API test *must* write: a `create → capture the id → read it back → update → delete → 404` chain is the
**opposite** of guessing — every value after the first call is one the server actually returned. A suite
that only reads (health/version/GET smoke) because "don't invent" was read as "don't write" is the
**bare-minimum** failure mode — green, but it never exercises the API. Rule of thumb: ground the **inputs**
(realistic payloads, ids you captured, values the spec defines); never fabricate the **contract**.

## Lock the response to the contract shape (best practice)

Assert the **whole response shape** with fuzzy markers, not a handful of fields — a schema-shape lock
catches missing/renamed/extra fields a partial assert misses, while staying robust to server-assigned
values:

```feature
Scenario: create persists and returns the contract shape
  Given path 'users'
  And request { name: 'Bob', email: 'bob@example.com' }
  When method post
  Then status 201
  # lock every field: fuzzy markers for server-assigned values, literals for business-meaningful defaults
  And match response == { id: '#string', name: 'Bob', email: 'bob@example.com', role: 'member', active: '#boolean' }
  * def id = response.id

Scenario: the email-required rule
  Given path 'users'
  And request { name: 'Bob' }
  When method post
  Then status 400
  And match response contains { error: '#string' }
```

Rules of thumb:
- **fuzzy** (`#string`/`#number`/`#boolean`/`#uuid`/`#present`) for server-assigned/opaque values; never
  hard-code a generated id (`match response.id == '#uuid'`, then `* def id = response.id`);
- **literal** for values that carry business meaning (a default `role: 'member'`, an exact error contract);
- `##marker` for an **optional** field (may be absent); **`match each response.items == { … }`** to assert
  every array element's shape (`'#[3] #number'` pins a LENGTH, optionally with a space-separated
  per-element matcher — it is not the shape form);
- `#(id)` to assert **cross-call identity** on a read-back (the same id you captured persisted).
  A step's `{…}`/`[…]` literal is JSON, not JS: a bare `x.id` in it is the string `'x.id'` — write
  `'#(x.id)'`, or `({ id: x.id })` for JS.

**Derive the lock from the spec when you have one — it's one line.** When the project has a spec in play
(`cov.openapi` in karate-boot.js, or `boot.ext('openapi').path`), the **`spec` global** emits the match
template straight from the contract:

```feature
* def UserShape = spec.schema('User')          # a component — { id:'#uuid', name:'#string', role:'##string' }
And match response == UserShape
* def BadShape = spec.schema('createUser', 400)  # an operation's DECLARED error shape — negative paths too
```

Required fields come back `#typed`, optional `##typed`, `format: uuid` → `#uuid`, enums → a value-set
predicate. The JS/MCP twin is `Openapi.matchSchema(specPath, ref[, status])`; with **no spec**, infer the
same template from a real payload with `match.schema(response)`. Either way the test asserts the
response *conforms to the contract*, the highest-value assertion an API test makes.

## When the project has a rulebook, the RULE is the oracle — never pin the number

A shape-lock proves the response *fits the contract*. It says nothing about whether the **business value**
is right. The moment you assert a computed business output — a premium, total, discount, tax, APR,
decision — ask **where the expected value comes from**. If the project has a rulebook (a `rulebooks/`
home, `cov.rules` in `karate-boot.js`, `Rule.list()` returns something), the answer is **the rulebook**,
and a hardcoded literal is a bug in the test:

```gherkin
# ✗ WRONG — 81.99 is a copy of what the rules already compute. It is not an independent oracle:
#   it must be re-pinned on every rate change, and it can never catch system-vs-rule drift.
And match response.total == 81.99

# ✓ RIGHT — assert the SYSTEM against the RULE. Nothing to maintain: a rate change never requires EDITING
#   this row, and it goes red only when the system and the business rules genuinely disagree.
* def check = Rule.execute('pricing', order)
And match response.total == check.output.total
And match response.outcome == check.outcome
* check.verify(true, 'live /quote matches the pricing rulebook')
```

The canonical shape is a **data-driven acceptance feature over the rulebook's own saved scenarios** —
every row is a business case someone signed off on, and the suite grows by adding a row, not a test:

```gherkin
Scenario Outline: <_id> — <_label>
  # the ORACLE: run the saved row through the rulebook. This also fires each calc.req arm the row
  # reaches, so the requirement criteria light up in the RTM with no tagging.
  * def check = Rule.execute('rating', __row)
  # the SYSTEM: a real call
  Given path 'quotes'
  And request { state: '#(__row.state)', coverage: '#(__row.coverage)', driverAge: '#(__row.driverAge)', priorClaims: '#(__row.priorClaims)' }
  When method post
  Then status 201
  And match response contains check.output       # no golden number, anywhere
  # the stamp: records that something OUTSIDE the rulebook agreed with it
  * check.verify(true, 'live /quotes matches the rulebook')

  Examples:
    # `.scenarios` — NEVER read('../rulebooks/rating/scenarios.json'): the same file also holds the
    # negative rows (`_expect:'schema-reject'`), and a raw read would drive malformed input at the
    # SUT expecting 201. Those rows ride `Rule.load(...).rejects` and assert the refusal instead.
    | Rule.load('rating').scenarios |
```

**`calc.output` IS the assertion surface** — `match response contains check.output` asserts exactly the
fields the rules model, and silently says nothing about the rest (an empty `calc.output` would make the
line an unconditional pass). So when the system returns a business field the rulebook doesn't compute,
either model it in the rulebook — the right fix — or assert it explicitly as the *rule it stands for*
(`match response.policyClass == 'SUBSTANDARD'` for a prior-claims driver), named in a comment as the
deliberate exception. What you must not do is leave it unasserted and believe the one-liner covered it.

**To drive ONE saved case** (a named business scenario rather than the whole file), read the rulebook's
scenarios and pick by id — there is no single-row verb, `Rule.load` is the reader:

```gherkin
* def saved = Rule.load('pricing').scenarios
* def rowById = function(id){ return karate.filter(saved, function(s){ return s._id == id })[0] }
* def order = rowById('gold-single-gadget')
```

Keep the business data in `scenarios.json` and name the row from the test — a check that re-types the
values owns a second copy of the business data that will drift from the one the rulebook prices.
(A row is flat: `_`-prefixed keys are the engine's, everything else is business input. On a
schema-strict protocol like gRPC send `check.input` — the row minus that metadata — since proto JSON
rejects unknown fields where a REST body tolerates them.)

Why `verify` matters for traceability: `Rule.execute` alone lights the criteria as **`oracleOnly`** — the
rulebook vouching for itself. `check.verify(ok, note)` is what records that a real system was compared
against the oracle, clearing the disclosure and auto-attaching `@req=`/`@cov=` with no tag typed by hand
(see the `rules` help page and the `requirements` skill). It is also an **assertion** — a false verdict
fails the scenario like a failed `match`, so a check can never be green while the system disagrees with
the rulebook.

**Then close the loop:** author → **`Runner.run('checks/<file>.feature')`** →
**`Requirement.matrix()`** / **`Requirement.readiness()`** — on the console the run's settle folds it into
the graph those read (the `coverage` skill has the loop, and which sources `karate-boot.js` must declare).

**The one legitimate hardcoded set** is a small, deliberately-curated **business-acceptance** file that
verifies the *rulebook itself* — a handful of human-signed-off values (`Rule.execute(name, row).output.apr == 0.06`),
kept separate from the suite and never per-scenario. That set checks the oracle; it is not an oracle.

**When there is no rulebook**, assert the requirement **directionally** instead of pinning a number the
implementation happens to produce — e.g. *a rush order costs more than the same order without rush*,
*a loaded driver pays more than a clean one*. A directional assertion encodes the business rule; a pinned
total encodes today's arithmetic.

## Worked example: a CRUD chain in one scenario

The pattern that separates a functional test from a smoke test — create, then reuse the server-assigned id
to read it back, update it, and delete it, proving each step against real state. One scenario, a stateful
sequence; the shape-lock uses a fuzzy marker for the id and literals for the business fields.

```gherkin
Scenario: create a todo, read it back, update it, delete it, confirm it is gone
  Given path 'todos'
  And request { title: 'buy milk', complete: false }
  When method post
  Then status 201
  And match response == { id: '#string', title: 'buy milk', complete: false }
  * def id = response.id                        # capture the server id

  Given path 'todos', id                        # reuse it
  When method get
  Then status 200
  And match response == { id: '#(id)', title: 'buy milk', complete: false }   # cross-call identity

  Given path 'todos', id
  And request { title: 'buy oat milk', complete: true }
  When method put
  Then status 200
  And match response == { id: '#(id)', title: 'buy oat milk', complete: true }

  Given path 'todos', id
  When method delete
  Then status 204                               # a 204 has NO body — assert status only (response is null; `== ''` fails NULL:STRING)
  Given path 'todos', id
  When method get
  Then status 404                               # a real 404 proves the delete took
```

A runnable version (against a stateful mock, plus a 400 validation negative) is the `todo-api` example kit's
`checks/todos-crud.feature`. Keep scenarios **independent** (anti-pattern 6): a full lifecycle belongs in
**one** scenario, not split across scenarios that depend on run order.

## Topic index

| Topic | When to read |
|---|---|
| `Skill.help('api', 'error-paths')` | Asserting 4xx/5xx flows; the "every endpoint needs a 4xx scenario" rule. |
| `Skill.help('api', 'data-driven')` | `Scenario Outline:`, `Examples:`, CSV, dynamic outline from JSON. |
| `Skill.help('api', 'reuse-call')` | `call`, `callonce`, `karate.callSingle`; when each is right. |
| `Skill.help('api', 'auth')` | Bearer / OAuth2 / basic / API-key / mTLS / refresh flows. |
| `Skill.help('api', 'hooks-config')` | `karate-config.js` hooks, tag-driven setup, `callSingleCache` patterns. |

## Step keyword reference

| Keyword | Purpose | Example |
|---|---|---|
| `url <str>` | Set base URL; persists across requests in the scenario. | `* url 'https://api.example.com'` |
| `path <segment>...` | Append path segments (encoded). | `Given path 'users', userId` |
| `param <key> = <val>` | Add **one** query-string param (resets each request). | `* param page = 2` |
| `params <map>` | Bulk-set query params (plural = many in one go). | `* params { page: 2, size: 50, q: 'alice' }` |
| `header <key> = <val>` | Per-request header. | `* header X-Trace-Id = traceId` |
| `headers <map>` | Bulk-set headers for this request. | `* headers { 'X-A': '1', 'X-B': '2' }` |
| `cookie <key> = <val>` | Per-request cookie. | `* cookie sid = 'abc'` |
| `request <value>` | Set request body. | `* request { name: 'Alice' }` |
| `method <verb>` | Fire the request. | `When method post` |
| `status <code>` | Assert response status. | `Then status 200` |
| `multipart file <name> = …` | Attach a file. | `* multipart file image = { read: 'cat.jpg' }` |
| `multipart field <name> = …` | Add a form field. | `* multipart field caption = 'hi'` |
| `form field <name> = …` | URL-encoded form field. | `* form field user = 'alice'` |
| `soap action <str>` | SOAPAction header + body wrap. | `* soap action 'urn:GetPolicy'` |
| `retry until <expr>` | Re-fire the previous request until `expr` is truthy. | `* retry until response.status == 'DONE'` |
| `configure <key> = <val>` | Set global config. | `* configure headers = { ... }` |
| `def <name> = <val>` | Bind a variable. | `* def newId = response.id` |
| `match <a> <op> <b>` | Assert response/value shape. | `And match response == { ... }` |
| `assert <js-expr>` | Boolean assertion. | `* assert response.length > 0` |
| `print <vals>` | Log to scenario output. | `* print 'response:', response` |
| `eval <js>` | Run JS without binding. | `* eval karate.log(response)` |
| `call <feature> <arg>` | Reuse another scenario. | `* call read('auth.feature')` |
| `callonce <feature> <arg>` | Reuse, cache per feature. | `* def t = callonce read('auth.feature')` |

## Standing rules

1. **Default the suite to assert behavior, not implementation.** Match on contract shape, not on incidental fields.
2. **Never hard-code generated IDs.** Capture: `match response.id == '#uuid'` then `* def newId = response.id`.
3. **Tag for filtering, tag for traceability.** `@smoke` / `@regression` for selection; `@req=…` for the feature-files-as-TCM model. `@noreq` opts out of the requirements view. **For a requirement that has acceptance criteria, tag per criterion — `@req=ORD-001/1` — not bare `@req=ORD-001`:** the requirement is the AND-rollup of its criteria, so a bare tag won't cover any individual criterion and the requirement stays NOTCOVERED even with a passing linked test (see the `requirements` skill).
4. **One scenario, one outcome.** If a scenario asserts two unrelated things, split it.
5. **`@report=false`** on scenarios that handle secrets — suppresses step detail in JSONL/HTML output.
6. **Put shared setup in `Background:`, not in the first scenario.** Background runs before every scenario; the order-coupling trap dies.
7. **Set `baseUrl` and auth in `karate-config.js`, not in each feature.** Env switching becomes a `-Dkarate.env=…` flag, not a code change.
8. **For one-time setup that's expensive (DB seed, token fetch), use `karate.callSingle`** in `karate-config.js`. Caches across parallel threads and across runs (with `configure callSingleCache`).
9. **For per-feature setup that depends on the test data, use `callonce`** in the Background. Caches per feature, not across features.
10. **For per-scenario reuse, use `call`** with the arg map. No cache; each call is fresh.
11. **`Scenario Outline:` for parametric tests, not for repeating slightly-different scenarios.** If your outline has only 2 rows, just write two scenarios.
12. **Assert error-path scenarios explicitly.** Every endpoint should have at least one 4xx scenario.
    Where a coverage source is registered this is *measured*, not aspirational: `Coverage.gaps()` returns
    a `happyOnly` worklist — operations exercised only on the success path, each row carrying the spec's
    declared-but-untested error codes (see the `coverage` skill).
13. **Use `retry until` not `Thread.sleep`.** Polling for async outcomes is a first-class pattern.
14. **Never pin a computed business value the project's rules already compute.** Assert the system against
    `Rule.execute(...)` (or assert the requirement directionally). See "the RULE is the oracle" above.

## Anti-patterns

1. **Anti-pattern:** `Thread.sleep(N)` (via `karate.exec` or worse, Java interop). Use `retry until` with a max-tries cap.
2. **Anti-pattern:** Re-asserting the same field in three scenarios. Move to a shared `call`-able feature.
3. **Anti-pattern:** Hard-coding `baseUrl` literally. Always via `karate-config.js`.
4. **Anti-pattern:** Asserting on `responseTime` without rationale. Performance assertions belong in a `Perf.*` track, not in a smoke test.
5. **Anti-pattern:** Catching auth failures with `try/catch`-shaped JS. Either pin the 401 in a dedicated scenario, or fix the auth chain.
6. **Anti-pattern:** Scenario A depends on Scenario B having run first. Scenarios must be **independent and parallelizable**. If shared state is real, capture it in `karate.callSingle`.
7. **Anti-pattern:** A golden number copied from the rules (`match response.total == 81.99`) presented as
   an independent oracle. It re-breaks on every legitimate rate change and can never detect system-vs-rule
   drift — the one thing it looks like it is checking. Use `Rule.execute`; see the section above.
8. **Anti-pattern:** Background that calls a network — slow + flaky-on-environment. `karate.callSingle` for the network call; `Background:` for setup that's free.

## Advanced surface (training-data-sparse — call out explicitly)

These are the karate-core features an LLM is unlikely to know from base training. Reach for them when applicable.

- **`karate.callSingle(path, arg)`** — execute once per *Suite*, cache result globally. Real pattern in `karate-config.js`:
  ```javascript
  karate.configure('callSingleCache', { minutes: 30, dir: 'target/karate-cache' });
  var auth = karate.callSingle('/auth.feature', { baseUrl: config.baseUrl });
  config.token = auth.token;
  ```
  Cache is in-memory + on-disk; survives across runs within the configured window. For expensive auth tokens, DB seeds, env discovery.
- **`karate.tags` introspection in `karate-config.js`** — gate hooks/auth/config by tags. Pattern:
  ```javascript
  var tags = karate.tags || [];
  if (tags.indexOf('admin') >= 0) {
    karate.configure('headers', { Authorization: 'Bearer ' + adminToken });
  }
  ```
- **`configure beforeScenario` / `afterScenario`** — per-scenario hooks. Use for token refresh, DB cleanup, screenshot-on-failure. Wire in `karate-config.js` or in a feature's `Background`.
- **`karate.match()` programmatic** — `match` but as a JS expression, returns `{ pass, message }`. Use when you need to *branch* on the result (instead of failing the step):
  ```feature
  * def r = karate.match(response, expected)
  * if (!r.pass) karate.log('warn', 'soft fail:', r.message)
  ```
- **`def <name> = function() { ... }`** — inline JS function. Reusable inside the scenario or feature:
  ```feature
  * def add = function(a, b) { return a + b }
  * match add(2, 3) == 5
  ```
- **`Java.type('com.example.Helper')`** — call Java static methods directly from JS:
  ```feature
  * def helper = Java.type('app.Helper')
  * def hash = helper.sha256('secret')
  * def base64 = Java.type('java.util.Base64').getEncoder().encodeToString('user:pass'.getBytes('UTF-8'))
  ```
- **`karate.setup()` / `karate.setupOnce()`** — programmatically execute a `@setup`-tagged scenario; useful for dynamic Outline data:
  ```feature
  Scenario Outline: parameterized
    * match name == '#string'
    Examples: | karate.setup().data |
  ```
  The `@setup` scenario builds a `data` array; the Outline iterates it.
- **`karate.signal(value)` / `karate.listen(timeoutMs)`** — cross-scenario async signal. Feature A `karate.signal({ok:true})`; Feature B `karate.listen(5000)` waits up to 5s. Used for fan-out tests.
- **`karate.fork(command)`** — spawn an async process; returns a handle with `.start()`, `.waitSync()`, `.stdOut`, `.exitCode`. Useful for starting a sidecar service from a Background.
- **`karate.faker.firstName() / email() / randomInt(min, max) / creditCardNumber()`** — built-in test-data generator.
- **`karate.read(path)`** — auto-detects JSON, CSV, YAML, XML, JS, feature files, binary. Supports `@tag` selectors: `karate.read('common.feature@auth')` calls only the scenario with `@auth`.
- **`karate.embed(data, mimeType, name)`** — embed binary / text in the HTML report. Useful for screenshots, downloaded PDFs.
- **`karate.set('name', value)` / `karate.get('name')`** — cross-scenario state. When set in `karate-config.js` via `karate.callSingle`, the value is suite-wide. Useful for sharing tokens, IDs, computed config.
- **`configure logging = { mask: ... }`** — redact secrets from logs (Authorization header, PII fields) without `@report=false`.
- **`configure retry = { count: 5, interval: 1000 }`** — set per-feature retry defaults for `retry until`.
- **`configure headers` function-form** — for dynamic headers (e.g. refreshed token):
  ```feature
  * configure headers = function() { return { Authorization: 'Bearer ' + karate.get('token') } }
  ```
  Function re-runs per request, so a `karate.set('token', newToken)` from a hook is picked up automatically.
- **`configure matchEachEmptyAllowed = true`** — allow `match each` over an empty array to pass. It **fails by default** (an unexpectedly-empty collection is caught for you); set `= true` in `karate-config.js` only when an empty array is legitimately valid.
- **Soft assertions** — `configure continueOnStepFailure = true` collects multiple failures per scenario; the scenario fails at the end.

## Cross-skill links

- `Skill.help('assertions')` — the `match` operator and fuzzy markers used in every assertion above.
- `Skill.help('grpc')` — gRPC tests reuse `Background`, `Scenario`, `match` ergonomics; the request build differs.
- `Skill.help('kafka')` — event-based tests pair with REST when the system has both.
- `Skill.help('coverage')` — OpenAPI coverage joins feature files to spec operations.
- `Skill.help('review')` — review heuristics check rules 1–13 above.
- `Skill.help('mock')` — Karate's mock server uses the same `.feature` syntax in reverse.
