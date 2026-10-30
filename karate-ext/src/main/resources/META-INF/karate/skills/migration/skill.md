# Karate Migration — Skill

> **Status:** Translate test artifacts from other frameworks into Karate `.feature` files.
> **Stack:** Postman goes through the converter, `Postman.migrate(collection, {item})`. Every other source is translated by you, the LLM, from the patterns below.
> **Prerequisites:** `Skill.help('api')`, `Skill.help('assertions')`, `Skill.help('getting-started')`.

## What this skill is for

A team has 5 years of tests in REST-assured / Postman / SoapUI / Insomnia / curl scripts / HAR captures. The skill teaches an LLM to **translate them into idiomatic Karate** — not transliterate (line-by-line, brittle, ugly) but **idiomatize** (idiomatic, leveraging Background, configure, call, match).

The 9 source frameworks (in priority order):

1. **Postman collections** — the converter, `Postman.migrate`. The only source with one.
2. **curl scripts / HAR files** — LLM-driven: read the command or capture, write the steps.
3. **REST-assured** (Java) — pattern-driven translation.
4. **SoapUI** — XML projects.
5. **Katalon** — Groovy DSL.
6. **ReadyAPI** — commercial SoapUI.
7. **Parasoft SOAtest** — future.
8. **Insomnia** — JSON export.
9. **Playwright (API mode)** — TypeScript.

## Mental model

```
   source artifact (curl / HAR / Postman JSON / Java test class)
     │
     ▼ parse
   intermediate representation
     ├─ requests: { method, url, headers, body, … }
     ├─ assertions (where the source has them — Postman tests, REST-assured chains)
     └─ environment vars
     │
     ▼ translate
   .feature file(s)
     ├─ Background: shared url, configure headers, auth
     ├─ Scenarios: one per source-request (or grouped by endpoint)
     └─ Standing rules from `Skill.help('api')` applied (no Thread.sleep, fuzzy markers, ...)
```

The IR pivot avoids N×M direct translators. Source-specific parsers all produce the IR; one Karate-emitter handles all.

## Postman — the converter, then a run

Postman is the one source with a converter. Migrate one folder at a time:

1. `Postman.read(collection)` — find the folder's `item` pointer.
2. `Postman.migrate(collection, {item, to?, data?})` → `{to, feature, notConverted, needs}`. It writes nothing. `to` defaults to `checks/<folder>.feature`; `data` (a `.csv`/`.json`) makes a flow that reads its columns a Scenario Outline.
3. In the API test chat, `Postman.migrate` holds the feature as the copy and `ApiTest.run(to)` saves it only when it passes. Elsewhere, write `feature` with `File.write(to, feature)` and run it.
4. Read `notConverted`: each line the feature keeps as `# not converted: <line>` (an `if`, a loop, `pm.sendRequest`, a file upload, an auth type with no Karate form). Translate those by hand.
5. `needs` names variables only karate-config can supply. Import a Postman environment with `Postman.env(collection, {from, env, ifMatch})`; never hand-write a second config.
6. Then propose `@req` links for the criteria the flow proves.

What it translates: `Postman.help('migrate')`. In short, requests that hand a variable on become one scenario; a collection variable is a Background `def` that karate-config overrides; `pm.test` is a `# title` over its `match` / `assert` steps; `pm.*.set` is `def`.

## curl and HAR — LLM-driven

There is no importer. Read the command or the capture and write the steps:

```feature
Scenario: create a user
  Given url baseUrl
  And path 'users'
  And header Authorization = 'Bearer ' + token
  And request { name: 'Alice' }
  When method post
  Then status 201
```

1. Put the host in `baseUrl` (karate-config) and the token behind a config-driven value.
2. Add a `match` on what the response must hold.
3. A HAR holds dozens of requests: group them by user flow, one `Scenario:` each; drop analytics beacons, prefetches and asset loads; replace generated ids with variables.
4. Tag `@req=…` when the requirement is known.

## REST-assured translation (pattern-driven)

REST-assured tests are Java code. A typical example:

```java
given()
  .baseUri("https://api.example.com")
  .header("Authorization", "Bearer abc")
  .contentType("application/json")
  .body("{\"name\":\"Alice\"}")
.when()
  .post("/users")
.then()
  .statusCode(201)
  .body("id", notNullValue())
  .body("name", equalTo("Alice"));
```

Translates to:

```feature
Background:
  * url 'https://api.example.com'
  * configure headers = { Authorization: 'Bearer abc', 'Content-Type': 'application/json' }

Scenario: create user
  Given path 'users'
  And request { name: 'Alice' }
  When method post
  Then status 201
  And match response == { id: '#present', name: 'Alice' }
```

### REST-assured → Karate mapping

| REST-assured | Karate |
|---|---|
| `given().baseUri(...)` | `* url ...` (in Background). |
| `given().header(k, v)` | `* configure headers = { k: v }` (Background) or `* header k = v` (per request). |
| `given().contentType(...)` | implicit when request body is a JSON / XML object literal. |
| `given().body(...)` | `* request ...`. |
| `.when().post(path)` | `Given path 'path'` + `When method post`. |
| `.then().statusCode(n)` | `Then status n`. |
| `.then().body(jp, matcher)` | `And match response.<jp> == <matcher>` (jp converted from JsonPath form to dot form). |
| Hamcrest `equalTo(...)` | literal. |
| `notNullValue()` | `'#notnull'`. |
| `containsString("x")` | `'#regex .*x.*'` or `'#? _.indexOf("x") >= 0'`. |
| `hasSize(N)` | `'#[N]'`. |
| `.extract().path("...")` then assertion | `* def x = response.<path>` then `* match x == ...`. |

## SoapUI / ReadyAPI

SOAP envelopes + XPath assertions. Karate supports XML matching natively:

```feature
* def envelope =
"""
<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:pol='http://example.com/policy'>
  <soapenv:Body>
    <pol:QuoteRequest><pol:age>35</pol:age></pol:QuoteRequest>
  </soapenv:Body>
</soapenv:Envelope>
"""

Given url 'https://soap.example.com/policy'
And request envelope
And soap action 'urn:Quote'
When method post
Then status 200
And match //quote/price == '#number'
```

SoapUI's "test step" hierarchy maps to `Background:` + `Scenario:`; XML asserts use Karate's xpath-aware `match`.

## Legacy `karate-config.js` idioms → modern API

When migrating an older Karate suite (or post-processing converter output), rewrite these:

| Legacy idiom | Modern API |
|---|---|
| `karate.properties['foo']` | `karate.sysprop('foo')` |
| `karate.properties['foo'] \|\| 'default'` | `karate.sysprop('foo', 'default')` |
| `java.lang.System.getenv('FOO')` | `karate.sysenv('FOO')` |
| `karate.sysenv('FOO') \|\| 'default'` | `karate.sysenv('FOO', 'default')` |
| `java.lang.System.getProperty('foo')` | `karate.sysprop('foo')` |

Inside `karate-boot.js`, the same rewrites apply with the `boot.` prefix (`boot.sysenv`, `boot.sysprop`). Both APIs treat unset and empty-string identically — empty falls through to the default.

## Standing rules

1. **Always post-process converter output.** `Postman.migrate` produces a *runnable* but rarely *idiomatic* feature file. Apply rules 1–13 from `Skill.help('api')`.
2. **Extract baseUrl + auth into `karate-config.js`.** Importers leave them inline; idiomatic Karate hoists them.
3. **Strip cookies that the new test will get fresh.** Imported sessions often carry stale session cookies; let Karate's HTTP client manage them.
4. **Replace literal IDs with fuzzy markers + rebinding.** Imported tests pin to whatever IDs were in the source artifact; idiomatic Karate pins shapes.
5. **Group by use case, not by URL.** Five HAR requests to `/users` that represent a real CRUD flow become one Scenario with five steps, not five Scenarios.
6. **Drop noise.** HAR captures every analytics beacon; Postman collections accumulate cruft. Be ruthless.
7. **Translate assertions, don't rebuild them.** A REST-assured test with three asserts becomes a Karate scenario with three matches — not one mega-match and not zero.
8. **Tag the imported tests with `@imported-from-<source>`.** Lets the team filter and gradually idiomatize.

## Anti-patterns

1. **Anti-pattern:** committing raw converter output. The whole point is *idiomatic* Karate; literal translation leaves brittle, ugly tests.
2. **Anti-pattern:** translating Hamcrest matchers one-for-one without considering fuzzy markers. `notNullValue()` → `'#notnull'`, not `* assert response.x != null`.
3. **Anti-pattern:** preserving Postman environment-variable indirection (`{{baseUrl}}`) verbatim. Use `baseUrl` in karate-config.js.
4. **Anti-pattern:** importing a 500-request HAR as one Scenario. Split.
5. **Anti-pattern:** keeping the source artifact and the imported feature in sync indefinitely. Migrate once, then drop the source — the feature file is now the truth.

## Cross-skill links

- `Skill.help('api')` — the idiomatic target.
- `Skill.help('assertions')` — for fuzzy-marker translation.
- `Skill.help('review')` — run the review skill on every imported file before considering it "done."
- `Skill.help('coverage')` — measure the gap between the imported suite and the OpenAPI spec.

## References

- `Postman.help('migrate')` — the converter's translation table and what it leaves not converted.
