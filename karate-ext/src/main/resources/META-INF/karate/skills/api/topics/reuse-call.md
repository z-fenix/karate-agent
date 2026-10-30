# Reuse — `call`, `callonce`, `karate.callSingle`

Factor a shared sequence (log in, seed a fixture, a common setup) into its own feature and call it, instead
of repeating it. Three variants by *how often it runs*.

## `call` — run every time, pass args in, get variables out

A called feature receives the caller's variables plus any arg you pass, and returns **all its variables** as
an object.

```gherkin
# called.feature — creates a todo, returns it
@ignore                          # @ignore keeps it out of direct suite runs; it only runs when called
Feature:
  Scenario:
    Given url apiUrl + '/api/todos'
    And request { title: title, complete: false }
    When method post
    Then status 201

# caller.feature
* def created = call read('called.feature') { title: 'from-caller' }
* def id = created.response.id       # the callee's variables (response, etc.) come back
```

Call a **single tagged scenario** of a multi-scenario feature with `@name=`:

```gherkin
* def result = call read('helpers.feature@name=createUser') { name: 'Bob' }
```

Data-driven call — pass a JSON **array** and the feature runs once per element:

```gherkin
* def results = call read('called.feature') [{ title: 'a' }, { title: 'b' }]
```

## `callonce` — run once per feature, cache the result

Same syntax, but the result is memoized for the whole feature (all scenarios share it). Use for
per-feature setup that must not repeat across scenarios (e.g. one fixture).

```gherkin
* def fixture = callonce read('seed.feature')
```

## `karate.callSingle` — run once per whole SUITE (auth is the classic case)

Fetch an expensive shared value (an auth token) **once** across the entire run — even under parallel
execution — and cache it. Put it in `karate-config.js` so every feature gets it:

```javascript
// karate-config.js
function fn() {
  var auth = karate.callSingle('/auth.feature');   // runs ONCE for the whole suite
  return { authToken: auth.token };
}
```

```gherkin
# any feature — reuse the cached token
Given url baseUrl
And path 'todos'
And header Authorization = 'Bearer ' + authToken
When method get
Then status 200
```

## Which one

| need | use |
|---|---|
| a helper on each call, args in / values out | `call` |
| setup that runs once per feature | `callonce` |
| one expensive value shared suite-wide (a token) | `karate.callSingle` (in `karate-config.js`) |

Keep called features `@ignore`d so they don't run as standalone scenarios; a called feature returns its
variables, so name the ones you want back.
