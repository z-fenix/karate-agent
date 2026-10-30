# Authentication — Bearer / OAuth2 / Basic / API-Key / mTLS

> **Skill:** `Skill.help('api')`. **Related topics:** `Skill.help('api', 'reuse-call')`.

## TL;DR

| Auth scheme | How to wire |
|---|---|
| **Bearer token** | `configure headers = { Authorization: 'Bearer ' + token }` in Background, or in `karate-config.js`. |
| **OAuth2 (client credentials)** | `karate.callSingle('/oauth.feature')` in `karate-config.js`; cache the token. |
| **OAuth2 (auth code / refresh)** | Same — token-fetch feature, then `karate.set('token', result.access_token)`. |
| **Basic auth** | `* def credentials = base64('user:pass')` then `Authorization: 'Basic ' + credentials`. |
| **API key (header)** | `configure headers = { 'X-API-Key': apiKey }`. |
| **API key (query)** | `* param api_key = apiKey` per request, or pre-compute `baseUrl`. |
| **mTLS** | `configure ssl = { keyStore: '...', keyStorePassword: '...', keyStoreType: 'PKCS12' }`. |

## Pattern(s)

### Bearer token (most common)

`karate-config.js`:

```javascript
function fn() {
  var env = karate.env || 'dev';
  var config = {
    baseUrl:      karate.sysenv('API_BASE_URL',
                    env === 'prod' ? 'https://api.example.com' : 'http://localhost:8080'),
    oauthClient:  karate.sysprop('oauth.client_id'),
    oauthSecret:  karate.sysenv('OAUTH_CLIENT_SECRET')
  };

  // Suite-wide auth, cached on disk for 30 minutes
  karate.configure('callSingleCache', { minutes: 30, dir: 'target/karate-cache' });
  var auth = karate.callSingle('/auth.feature', config);
  config.token = auth.access_token;

  return config;
}
```

Real pattern: declare `callSingleCache` *before* the `callSingle` call so the first invocation populates the disk cache. Subsequent runs within the window reuse it — drops a 1–2s OAuth round-trip on every test run.

Feature file:

```feature
Feature: My tests

Background:
  * url baseUrl
  * configure headers = { Authorization: 'Bearer ' + token }

Scenario: protected endpoint
  Given path 'me'
  When method get
  Then status 200
```

`auth.feature` (the one-time fetch):

```feature
Feature: Fetch OAuth2 token (client_credentials)

Scenario:
  Given url 'https://auth.example.com'
  And path 'oauth', 'token'
  And form field grant_type = 'client_credentials'
  And form field client_id = karate.sysprop('oauth.client_id')
  And form field client_secret = karate.sysenv('OAUTH_CLIENT_SECRET')
  When method post
  Then status 200
  * def token = response.access_token
  * def expiresIn = response.expires_in
```

The result is cached for the whole Suite by `karate.callSingle`. Pair with `configure callSingleCache = { minutes: 30, dir: 'target/karate-cache' }` for disk caching across runs.

### Basic auth

```feature
* def credentials = java.util.Base64.getEncoder().encodeToString('user:pass'.getBytes('UTF-8'))
* configure headers = { Authorization: 'Basic #(credentials)' }
```

Or in `karate-config.js`:

```javascript
var creds = java.util.Base64.getEncoder().encodeToString(java.lang.String('user:pass').getBytes('UTF-8'));
return { authHeader: 'Basic ' + creds };
```

### API key

In header:

```feature
* configure headers = { 'X-API-Key': apiKey }
```

In query string (less ideal but sometimes required):

```feature
Scenario: list users
  Given path 'users'
  And param api_key = apiKey
  When method get
```

### mTLS / client certificates

```feature
* configure ssl = {
    keyStore: '/certs/client.p12',
    keyStorePassword: 'changeit',
    keyStoreType: 'PKCS12',
    trustStore: '/certs/ca.jks',
    trustStorePassword: 'changeit'
  }
```

Set globally in `karate-config.js` for the whole suite, or per-feature in `Background`.

### Per-request override

Auth set in Background applies to every request. To override for one scenario:

```feature
Scenario: anonymous endpoint
  Given path 'public', 'status'
  And header Authorization = ''       # blank to drop the Background auth
  When method get
```

Or programmatically:

```feature
* configure headers = null              # drops headers entirely (rare; usually you want override per-call)
```

### Refresh on expiry

If your token expires mid-suite, you have three patterns. Real shapes from production suites:

**Pattern 1 — fetch fresh per feature**: drop `karate.callSingle`, use `callonce` in each feature's Background. Fresh token per feature, no expiry issue.

```feature
Background:
  * def authResult = callonce read('/auth.feature')
  * url baseUrl
  * configure headers = { Authorization: 'Bearer ' + authResult.token }
```

**Pattern 2 — `beforeScenario` hook**: refresh in `karate-config.js`, runs before every scenario. Token shared via `karate.set` / `karate.get`.

```javascript
// karate-config.js
config.tokenExpiresAt = 0;

karate.configure('beforeScenario', function() {
  if (karate.get('tokenExpiresAt', 0) < Date.now() + 60000) {  // 1-minute buffer
    var fresh = karate.call('/auth.feature');
    karate.set('token', fresh.access_token);
    karate.set('tokenExpiresAt', Date.now() + (fresh.expires_in * 1000));
  }
});
```

**Pattern 3 — dynamic `configure headers` function**: the header value re-resolves on every request, so a refreshed `karate.set('token', ...)` is automatically picked up. No explicit hook needed:

```feature
Background:
  * url baseUrl
  * configure headers = function() { return { Authorization: 'Bearer ' + karate.get('token') } }
```

Combine pattern 2 (refresh) with pattern 3 (dynamic header) for the cleanest expiry handling: the hook refreshes the token in `karate.set`; the dynamic header function reads it on every call.

## Edge cases / gotchas

1. **`configure headers` in Background sets headers as a function call.** If you write `configure headers = { ... fixedToken ... }`, the value is captured at Background time. Use the function form `configure headers = read('headers.js')` if the token changes across scenarios.
2. **Path-encoded credentials are NOT auto-encoded.** `path 'user:pass@host'` keeps the `:` and `@` literally. Use `url 'https://user:pass@host'` for Basic-in-URL (deprecated, but exists).
3. **mTLS + proxy interaction**: when running through karate-agent's session proxy, mTLS may not survive. Test with direct connection first.
4. **OAuth refresh-token rotation**: if your IdP rotates the refresh token on each use, the cached token must be invalidated. Use Pattern 1 above.
5. **`karate.callSingle` and parallel threads**: the result is shared across threads — thread-safe by design. But the auth call itself runs on one thread; ensure the auth feature is thread-safe (no scribbling on shared state).
6. **`Authorization` header on redirects**: Karate follows redirects by default (`configure followRedirects = true`). The Authorization header is re-sent — make sure that's safe for cross-domain redirects.

## Anti-patterns

1. **Anti-pattern:** putting a literal token in the feature file. Read it in `karate-config.js` via `karate.sysenv('TOKEN')` or `karate.sysprop('token')`.
2. **Anti-pattern:** fetching the token in every scenario. Use `karate.callSingle` (suite) or `callonce` (feature).
3. **Anti-pattern:** scattering auth setup across 12 features. Centralize in `karate-config.js` + one `auth.feature`.
4. **Anti-pattern:** committing real tokens to git. Read from env vars or `-D` system properties; never inline.
5. **Anti-pattern:** using Basic auth in production tests when the prod system has moved to OAuth. Test what runs in prod.

## References

- karate-docs: `/http-requests/headers-auth`, `/advanced/hooks`
- Source: `karate-core/src/main/java/io/karatelabs/core/KarateConfig.java` (callSingleCache, ssl config)
