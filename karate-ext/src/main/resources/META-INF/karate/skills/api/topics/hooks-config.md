# `karate-config.js` Hooks & Tag-Driven Setup

> **Skill:** `Skill.help('api')`. **Related topics:** `Skill.help('api', 'auth')`, `Skill.help('api', 'reuse-call')`.

## TL;DR

`karate-config.js` runs **once per feature** and returns a map that becomes top-level variables. But it can also:

1. **Register hooks** (`beforeScenario` / `afterScenario` / `afterFeature` / `onStepFailure`) that fire across all scenarios.
2. **Branch on tags** via `karate.tags`, gating setup to specific test groups.
3. **Set caches and global configures** (`callSingleCache`, `logging`, `ssl`, `retry`).
4. **Share state** suite-wide via `karate.set` / `karate.get` (or via `karate.callSingle`).

This file is more than a config map — it's the runtime customization entry point.

## When to reach for this

| Need | Pattern |
|---|---|
| Suite-wide auth token, refreshed on expiry | `karate.callSingle` + `beforeScenario` hook |
| Screenshot on UI test failure | `onStepFailure` hook |
| Different headers for `@admin` vs `@user` scenarios | `karate.tags` branch in config |
| Set log redaction for the whole suite | `configure logging = { mask: ... }` in config |
| Allow `match each` over a legitimately-empty array (strict-fail is the default) | `configure matchEachEmptyAllowed = true` in config |
| Disk-cache an expensive `callSingle` across runs | `karate.configure('callSingleCache', { minutes: N, dir: '...' })` |

## Pattern: full-featured `karate-config.js`

```javascript
function fn() {
  var env = karate.env || 'dev';
  karate.log('karate-config: env =', env);

  // ---- 1. Base config map (returned at the end) ----
  var config = {
    dev:  { baseUrl: karate.sysenv('DEV_BASE_URL',  'http://localhost:8080') },
    qa:   { baseUrl: karate.sysenv('QA_BASE_URL',   'https://qa.example.com') },
    prod: { baseUrl: karate.sysenv('PROD_BASE_URL', 'https://api.example.com') }
  }[env];

  if (!config) karate.fail('unknown env: ' + env);

  config.clientId     = karate.sysprop('oauth.client_id');
  config.clientSecret = karate.sysenv('OAUTH_CLIENT_SECRET');

  // ---- 2. Disk-cached suite-wide auth ----
  karate.configure('callSingleCache', { minutes: 30, dir: 'target/karate-cache' });
  var auth = karate.callSingle('/auth.feature', config);
  config.token = auth.access_token;

  // ---- 3. Global log masking ----
  karate.configure('logging', {
    mask: {
      headers: ['Authorization', 'Cookie', 'X-Api-Key'],
      jsonPaths: ['$.password', '$..token'],
      replacement: '***'
    }
  });

  // ---- 4. Tag-driven overrides ----
  var tags = karate.tags || [];

  if (tags.indexOf('admin') >= 0) {
    karate.log('admin scenario — using elevated token');
    var adminAuth = karate.callSingle('/admin-auth.feature');
    config.token = adminAuth.access_token;
  }

  if (tags.indexOf('readonly') >= 0) {
    karate.configure('headers', { 'X-Read-Only': 'true' });
  }

  // ---- 5. Hooks ----
  karate.configure('beforeScenario', function() {
    var now = Date.now();
    if (karate.get('tokenExpiresAt', 0) < now + 60000) {
      var fresh = karate.call('/auth.feature', { baseUrl: config.baseUrl });
      karate.set('token', fresh.access_token);
      karate.set('tokenExpiresAt', now + (fresh.expires_in * 1000));
    }
  });

  karate.configure('afterScenario', function() {
    var info = karate.info;
    if (info.errorMessage) {
      karate.log('scenario failed:', info.scenarioName, '-', info.errorMessage);
    }
  });

  karate.configure('onStepFailure', function() {
    // For driver-based UI tests: take a screenshot on any failure
    var driver = karate.get('driver');
    if (driver) {
      karate.embed(driver.screenshot(), 'image/png', 'failure');
    }
  });

  return config;
}
```

## Pattern: tag-guarded hook registration

Hooks fire on every scenario unless you guard them. Most projects need *some* hooks for *some* tags only:

```javascript
var tags = karate.tags || [];

// Only install screenshot-on-failure for @ui scenarios
if (tags.indexOf('ui') >= 0) {
  karate.configure('onStepFailure', function() {
    var driver = karate.get('driver');
    if (driver) karate.embed(driver.screenshot(), 'image/png');
  });
}

// Only install the audit-log hook for @audit scenarios
if (tags.indexOf('audit') >= 0) {
  karate.configure('afterScenario', function() {
    karate.write({ scenario: karate.info.scenarioName, ts: Date.now() }, 'audit-log.jsonl');
  });
}
```

The tag check happens at config-load time (per-feature), so each feature picks up only the hooks relevant to its tags.

## Pattern: env-specific cache strategies

```javascript
if (env === 'ci') {
  // CI: short cache; tokens may differ across pipeline stages
  karate.configure('callSingleCache', { minutes: 5, dir: 'target/karate-cache' });
} else if (env === 'dev') {
  // Dev: long cache; one token per developer per workday
  karate.configure('callSingleCache', { minutes: 480, dir: '/tmp/karate-cache' });
} else {
  // Prod / qa: in-memory only — no disk leak
  karate.configure('callSingleCache', { minutes: 30 });
}
```

## Available hooks

| Hook | Fires | `this` / args | Common use |
|---|---|---|---|
| `beforeScenario` | Before each scenario starts. | None (read `karate.info`, `karate.tags`). | Token refresh, DB seed, test-data reset. |
| `afterScenario` | After each scenario ends. | None (read `karate.info.errorMessage`). | Cleanup, custom logging. |
| `afterFeature` | After a feature ends. | None. | Feature-level summaries. |
| `onStepFailure` | On any step failure (before scenario ends). | None. | Screenshot, dump request/response, abort suite. |
| `afterScenarioOutline` | After a `Scenario Outline:` finishes all rows. | None. | Outline-level aggregation. |

All hooks installed via `karate.configure('hookName', fn)` in `karate-config.js` apply to **every scenario** in the suite, unless tag-guarded as above.

## Edge cases / gotchas

1. **`karate.tags` is the feature-level tag union.** It includes scenario tags too — but only for the scenario currently being prepared. If you're in `karate-config.js` at feature-load time, `karate.tags` might lag behind. Check `karate.info.scenarioTags` inside a hook for current-scenario specificity.
2. **`karate.configure(...)` overrides earlier `karate.configure(...)`** for the same key. Order matters; tag-guarded overrides must come after the base config.
3. **Hooks installed in `karate-config.js` are global**; hooks installed in a feature's `Background` are feature-scoped. Pick the scope deliberately.
4. **`callSingleCache` config must be set *before* the `karate.callSingle` call**, otherwise the first call won't be cached.
5. **`karate.set` / `karate.get` are thread-safe** but eventually consistent across parallel features. For per-scenario state, use `* def` instead.
6. **`onStepFailure` can swallow the failure** if it doesn't return a truthy value. Don't `return false` unless you know what you're doing.

## Anti-patterns

1. **Anti-pattern:** putting business logic in `karate-config.js`. It's setup; tests live in features.
2. **Anti-pattern:** synchronous slow code in a hook. A `beforeScenario` that calls a slow service multiplies the run time by N.
3. **Anti-pattern:** registering a hook without a tag guard, then having the hook only matter for some scenarios. Guard with `tags.indexOf('foo') >= 0`.
4. **Anti-pattern:** mutating `config` after the return. Once returned, the map is frozen-ish. Pre-compute everything inside `fn()`.
5. **Anti-pattern:** using `karate.set('config', ...)` to share the config map. Return it; that's the contract.

## References

- karate-docs `/core-syntax/configuration`, `/advanced/hooks`
- Source: `karate-core/src/main/java/io/karatelabs/core/KarateConfig.java`
