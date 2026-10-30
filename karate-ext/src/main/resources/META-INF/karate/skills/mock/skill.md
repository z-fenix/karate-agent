# Karate Mock — Skill

> **Status:** Karate's mock server is built into karate-core (no addon needed for REST mocks).
> **Stack:** karate-core; same `.feature` syntax as test authoring, but interpreted server-side.
> **Prerequisites:** `Skill.help('api')`, `Skill.help('assertions')`.
> **Companion docs:** karate-docs `/extensions/test-doubles` (~1100 lines).

## What this skill is for

Spin up a **Karate-native mock HTTP server** from a `.feature` file. The same syntax that drives tests drives mocks — Karate inverts the request/response roles. The skill teaches an LLM to:

1. Write a `mock.feature` file: per-Scenario request-matching + dynamic response construction.
2. Start the mock embedded (in-process for unit tests) or standalone (subprocess for integration tests).
3. Use stateful mocks: capture request data, return it in subsequent responses.
4. Proxy mode: pass-through to the real backend with selective override (rewrite headers, replay traffic).
5. Route by path / method / header / body shape.
6. Validate the mock's request reception in the test that calls it.

## Rule-backed mocks — pick the RIGHT flavor first (karate-max)

If the mock's responses must be computed by a **rulebook** (`Rule.execute` as the pricing/decision
oracle — the "mock shares a brain with the rules" pattern), do NOT reach for the feature flavor below:

- **Ext visibility per flavor (the trap):** a mock **feature** engine and a **raw-JS** mock engine
  never boot project exts — `boot.ext('rules')` in `karate-boot.js` binds `Rule` for `Runner.run`
  features only, NOT inside mock engines. A mock feature that calls `Rule.*` fails no matter what
  you add to `karate-boot.js`.
- **The working pattern:** the **OpenAPI flavor** — `Http.mock('openapi.yaml', {rules: {opId:
  'rulebook'}, handlers: …})`. A `rules:` overlay routes an operation's response through the
  rulebook engine-side; `handlers:` **functions created in the served console session** (e.g.
  loaded via `File.call('/mock/handlers.js')`) close over the console engine where `Rule` IS
  bound — so stateful handlers can price via `Rule.execute` directly.
- Full option reference: `Http.help('mock')`.

The rest of this skill covers the OSS **feature-flavor** mock — right for spec-less stubs and
request-matching doubles, wrong for rule-backed responses driven from the console.

## Mental model

A Karate **mock feature** flips the script:

- The mock's `Background:` sets state shared across all received requests.
- Each `Scenario:` declares a **request matcher** (in `Given … When match` form) and a **response builder** (`Then status … And response …`).
- The mock loops: incoming request → walk scenarios top-to-bottom → first match wins → build the response → return it.
- State persists across requests because Karate's JS engine is shared across mock-scenario invocations.

```
   GET /users/42  ─────► Karate mock server
                         ├─ Scenario: GET /users/{id}  → match? yes
                         ├─ build response { id: pathParams.id, name: 'Mock-' + id }
                         └─ return 200 + body
   ◄───── 200 { id: '42', name: 'Mock-42' }
```

## Quick start

`mock-users.feature`:

```feature
@ignore
Feature: mock users API

Background:
  * configure cors = true
  * def users = { 42: { id: '42', name: 'Alice' }, 43: { id: '43', name: 'Bob' } }

Scenario: pathMatches('/users/{id}') && methodIs('get')
  * def id = pathParams.id
  * def user = users[id]
  * def responseStatus = user ? 200 : 404
  * def response = user || { error: 'not_found' }

Scenario: pathMatches('/users') && methodIs('post')
  * def user = request
  * def newId = karate.uuid()
  * users[newId] = { id: newId, name: user.name }
  * def responseStatus = 201
  * def response = users[newId]

Scenario:
  # Catch-all — return 405 for anything else
  * def responseStatus = 405
  * def response = { error: 'method_not_allowed' }
```

Start the mock:

```java
// Embedded (in-process)
Result result = MockServer.feature("classpath:mock-users.feature")
    .http(8080)
    .build();
// ... run tests against http://localhost:8080 ...
result.stop();
```

Or programmatically from a feature:

```feature
* def mock = karate.start({ mock: '/mock-users.feature', port: 8080 })
* configure baseUrl = 'http://localhost:8080'

# now drive tests against the mock
Given path 'users', '42'
When method get
Then status 200
And match response == { id: '42', name: 'Alice' }

* mock.stop()
```

### From the console (or a paired run) — where there is no feature runtime

`karate.*` is the **feature** runtime, so `karate.start(...)` is not available from a served console/eval
session — and neither is a project starter script that calls it (`File.call('/mock/start.js')` comes back
`karate is not defined`). Serve the same feature with `Http.mock` instead, which hands back the live handle
(`url` · `port` · `calls` · `stop()`):

```js
var mock = Http.mock('mock/api.feature');           // or Http.mock({feature:'mock/api.feature', port:8080})
Http.get(mock.url + '/users/42').body
mock.stop()
```

That is also what `Contract.pair({mock:'mock/api.feature', …})` takes, so a paired run measures **the mock
your project ships** rather than one synthesized from the spec.

**Between phases — `mock.restart()`, not `mock.reset()`.** A mock feature's state is the variables its
`Background` defined, which live in the engine that feature built. `reset()` clears the call log and the
flavor's store and cannot reach them, so the next phase inherits the previous one's rows; `restart()` runs
the `Background` again on the **same port**, so the URL and the handle stay valid. `reset()` answers
`{calls, cleared, survives?, clean, restart?}` — when it says `clean:false`, believe it: an empty call log
is also what a contaminated mock looks like. (`restart()` re-runs the feature parsed at start; to pick up an
edited file, `stop()` and start a new mock.)

## Mock-side JS surface

Inside a mock feature, Karate exposes a different set of bindings:

| Binding | Type | Purpose |
|---|---|---|
| `request` | Object | Decoded request body (auto-parsed JSON / XML / text). |
| `requestHeaders` | Map | Request headers. |
| `requestParams` | Map | Query-string params. |
| `pathParams` | Map | Path-template params (from the `pathMatches` matcher). |
| `requestMethod` | String | `'GET'` / `'POST'` / etc. |
| `requestUrlBase` | String | The URL up to the path. |
| `requestUri` | String | The path + query. |
| `response` | Object | The response body you're constructing. Set via `* def response = ...`. |
| `responseStatus` | int | The response status code. Default 200. |
| `responseHeaders` | Map | The response headers. |
| `responseDelay` | int | Optional artificial delay (ms). |

Matchers (use inside a Scenario's name / `Given` clause):

| Matcher | Purpose |
|---|---|
| `pathMatches('/users/{id}')` | Match path + extract `pathParams`. |
| `methodIs('post')` | Match HTTP method. |
| `paramExists('q')` | Match presence of a query-string param. |
| `headerContains('Content-Type', 'json')` | Match header substring. |
| `bodyContains('"role":"admin"')` | Match raw request body substring. |

You can combine: `pathMatches('/users/{id}') && methodIs('get') && headerContains('Authorization', 'Bearer')`.

## Proxy mode — pass-through with selective override

```feature
Feature: pass-through to real backend

Background:
  * def targetUrl = 'https://api.example.com'

Scenario: pathMatches('/admin/{path}') && methodIs('get')
  # Override: return mock response for admin endpoints
  * def response = { admin: true, mocked: true }

Scenario:
  # Default: forward to real backend
  * def proceed = karate.proceed(targetUrl)
  * def response = proceed.body
  * def responseStatus = proceed.responseStatus
  * def responseHeaders = proceed.responseHeaders
```

`karate.proceed(targetUrl)` forwards the current request to `targetUrl` (preserving path, method, headers, body) and gives you the response to manipulate.

## State across requests

```feature
Background:
  * def orders = {}
  * def nextId = function() { return karate.uuid() }

Scenario: pathMatches('/orders') && methodIs('post')
  * def id = nextId()
  * def order = request
  * order.id = id
  * order.status = 'PENDING'
  * orders[id] = order
  * def responseStatus = 201
  * def response = order

Scenario: pathMatches('/orders/{id}') && methodIs('get')
  * def order = orders[pathParams.id]
  * def responseStatus = order ? 200 : 404
  * def response = order || { error: 'not_found' }

Scenario: pathMatches('/orders/{id}/complete') && methodIs('post')
  * def order = orders[pathParams.id]
  * order.status = 'COMPLETED'
  * def response = order
```

`orders` is a shared map; changes from one request are visible to the next.

## Standing rules

1. **Order matters.** Mock scenarios are walked top-to-bottom; the first match wins. Put specific matchers above catch-alls.
2. **One mock feature per role.** A mock for "auth" is separate from a mock for "users." Compose at startup by starting multiple mocks on different ports.
3. **`Background:` for shared state**, including the data store, helper functions, and configuration. Do not put response logic in Background.
4. **Set `responseStatus` explicitly** — the default 200 is rarely what you want for negative paths.
5. **For dynamic IDs, use `karate.uuid()`.** Don't increment a counter (parallel requests collide).
6. **Validate request shape inside the matcher**, not in the response builder. Use `bodyContains(...)` / `headerContains(...)` to reject malformed.
7. **Tag every mock feature `@ignore`, on the `Feature:` line.** A mock is a *definition*, not a test: its scenario names are request matchers and its steps read `request`, which exists only while serving. Untagged, any suite that sweeps a folder — `Runner.suite('.')`, `karate run .`, a CI glob — discovers it, executes the matcher as a scenario, and fails with `request is not defined`: a red build from a file that was never a test. `@ignore` is the tag the engine actually excludes from suite selection; `@mock` and friends are labels for humans and exclude nothing on their own. It costs the mock nothing — serving walks the feature's scenarios directly and never consults tags — so the mock still starts and matches exactly as before.

   ```feature
   @ignore
   Feature: mock users API
   ```
8. **Use a catch-all scenario at the bottom** that returns 405 / 501 so unmocked endpoints fail loudly.
9. **Reference the mock feature project-root-anchored.** When `karate-config.js` auto-starts a backend — `karate.callSingle('/mock/start.js')`, and inside it `karate.start({ mock: '/mock/api.feature' })` — use the leading-`/` (project-root) form, NOT `../mock/...`. Config-eval (`Runner.config` / the explore harness) has no running feature to anchor a relative path, so `../mock/x` overshoots above the project and fails "Failed to read text from". The `/`-form resolves the same from a feature run and from config-eval. (Getting-started standing rule 7.)

## Anti-patterns

1. **Anti-pattern:** asserting in a mock. A mock builds responses; it doesn't run `match`. Move the assertion to the calling test.
2. **Anti-pattern:** one giant mock feature with 50 scenarios. Split by domain (`mock-users.feature`, `mock-orders.feature`).
3. **Anti-pattern:** sharing the mock JVM with the test JVM accidentally. Run the mock in its own process if your test mutates JVM state (e.g. system properties).
4. **Anti-pattern:** returning canned literal JSON for every request. Use `karate.read('fixtures/users.json')` for reusable fixtures.
5. **Anti-pattern:** proxy mode without a kill-switch. Always have a "blow up if I forgot to set targetUrl" fallback.

## Cross-skill links

- `Skill.help('api')` — tests that drive the mock; same syntax in reverse.
- `Skill.help('assertions')` — assertions in the calling test, not in the mock.
- `Skill.help('coverage')` — mocks contribute to API coverage when paired with OpenAPI specs.
- karate-docs `/extensions/test-doubles` — full reference (1100+ lines).
