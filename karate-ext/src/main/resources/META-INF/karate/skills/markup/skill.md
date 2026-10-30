# Karate Markup — UI Skill

> **CSS framework:** This skill covers Karate's template engine, Alpine binding, and HTMX integration. The CSS framework is pluggable. Pick your path:
> - **Tailwind** → see `Skill.help('markup', 'tailwind')` — the shipped, in-repo path (the karate-max console uses it).
> - **Bootstrap 5** (or any other) → no topic ships in-repo; substitute your framework's concrete classes wherever an example uses a `<your-…-class>` placeholder. References to `CSS_BOOTSTRAP.md` below mark the spots where a Bootstrap-specific note would go.
>
> Examples below use generic CSS classes (`alert`, `badge`, `btn`) where the choice doesn't matter to the framework mechanics. Where a class string in an example is meaningful (e.g. an Alpine `x-bind:class` that toggles between two visual states), the example uses `<your-active-class>` / `<your-inactive-class>` placeholders — substitute the concrete classes from your chosen CSS framework.

> **Stack:** Alpine.js 3 + HTMX 2 + your CSS framework
> **Engine:** Thymeleaf with JavaScript expressions (not OGNL/SpEL). Runs on JVM via karate-core.
> **Philosophy:** HTMX done right — server returns HTML, not JSON. No build step, no bundler, no hydration.

## CDN Block (copy into `<head>`)

```html
<script src="https://unpkg.com/htmx.org@2.0.4"></script>
<!-- App-level scripts that register Alpine.magic / Alpine.directive / Alpine.plugin
     MUST load BEFORE alpinejs (see note below). ka:nocache="true" rewrites the URL
     to /pub/app.js?ts=<lastModifiedMillis> so edits show up without a hard-refresh
     in dev AND deploys bust the browser cache for users. -->
<script src="/pub/app.js" ka:nocache="true" defer></script>
<script defer src="https://unpkg.com/alpinejs@3.14.3/dist/cdn.min.js"></script>
```

Add the CDN lines for your CSS framework (the Tailwind block is in `Skill.help('markup', 'tailwind')`) above these.

**Script order matters for `Alpine.magic` / `Alpine.directive` / `Alpine.plugin`.** The Alpine 3 CDN starts via `queueMicrotask(Alpine.start)` immediately after its script executes — *not* on `DOMContentLoaded`. Any `defer` script loaded AFTER the Alpine CDN runs too late: by the time it executes, Alpine has already drained the start microtask, dispatched `alpine:init`, walked the DOM, and processed every `x-data` — so a missing magic surfaces as `Alpine Expression Error: $foo is not defined` and the entire component scope fails to mount (cascade of `events is not defined`, `text is not defined`, etc., for everything declared inside that `x-data`). Load registration scripts BEFORE the Alpine CDN line; their `alpine:init` listeners will then be live in time. Templates that don't register any magic don't need to care about ordering.

## Server-Side JS (`ka:scope`)

```html
<script ka:scope="global">
  // Runs on server (JVM). Variables set on _ are available in template expressions.
  _.items = db.getItems();
  if (request.post) {
    var name = request.param('name');
    db.save({ name: name });
    context.flash.success = 'Saved';
    context.redirect('/items');
  }
</script>
<h1 th:text="'Hello, ' + session?.user?.name">Placeholder</h1>
<div th:each="item: items">
  <span th:text="item.name">Name</span>
</div>
```

**`ka:scope="local"`** — runs per iteration inside `th:each`. Variables isolated to that loop cycle:

```html
<div th:each="item, iter: items">
  <script ka:scope="local">
    _.total = item.price * (1 + taxRate);
    _.statusClass = item.status == 'ready' ? '<your-success-class>' : '<your-danger-class>';
  </script>
  <span th:text="iter.count">1</span>. <span th:text="item.name">Name</span>: $<span th:text="total">0</span>
  <span th:class="'badge ' + statusClass" th:text="item.status">status</span>
</div>
```

(Replace `<your-success-class>` / `<your-danger-class>` with the concrete state classes from your `CSS_*.md`.)

**When to reach for `ka:scope="local"` (decision rule):**

Inline the expression in `th:text` / `th:if` / `th:class` by default. Reach for `ka:scope="local"` only when **at least one** of these holds:

1. **Two or more derived values are reused** within the same row — recomputing them in every attribute is noisier than a 2-line script. Example: `team/users.html` row computes `statusKey` (drives label + badge color + pill class) and `idle` (drives the dim icon + tooltip), used 4 times in the row markup.
2. **The expression is awkward in attribute syntax** — multi-step null-coalescing, regex test, date math, calls into `utils.*` that take >1 argument, or anything that reads better on its own line.
3. **A value depends on another locally-derived value** — e.g. `idle` depends on a parsed `lastSeenDays`. Without a local script you'd compute `lastSeenDays` twice.

**Don't reach for it when:**

- The value is used exactly once (`<span th:text="item.name">` is fine — no `ka:scope` needed).
- A simple `||` default would do (`th:text="item.label || 'Unnamed'"`).
- The derived value belongs at the page level (lift it to `ka:scope="global"` and pre-compute once).

**Cost reminder:** `ka:scope="local"` re-evaluates the script on every iteration. For a 200-row table that's 200 JS evals — fine for a handful of trivial assignments, less fine if you're calling `db.*` or `utils.get*` per row. If you find yourself doing per-row DB reads, batch at the global scope and key into the result locally instead.

## Template Attributes (Cheat Sheet)

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `th:text` | Set text content | `th:text="item.name"` |
| `th:if` / `th:unless` | Conditional render | `th:if="user.admin"` |
| `th:each` | Loop | `th:each="item: items"` |
| `th:attr` | Set any attribute (see Gotcha #18) | `th:attr="'data-id': item.id"` |
| `th:class` | Set class | `th:class="item.active ? 'active' : ''"` |
| `th:classappend` | Append to class | `th:classappend="item.active ? 'active' : ''"` |
| `th:insert` | Insert fragment | `th:insert="components/badge"` |
| `th:replace` | Replace with fragment | `th:replace="header"` |
| `th:with` | Pass params to fragment | `th:with="state: item.state, size: 'sm'"`. Also: shorthand `th:with="state, size"` forwards named vars; spread `th:with="...ctx"` forwards every property of `ctx`; combine + override with `th:with="...ctx, size: 'lg'"` |
| `th:fragment` | Define a named fragment | `th:fragment="badge"` |

**Expression language is JavaScript.** Use `items.length` not `items.size()`. Empty string `""` is truthy. Note: `th:with` uses `:` not `=` for assignment (JS syntax, not standard Thymeleaf).

**Iteration variables:** Always use the explicit `iter` naming: `th:each="item, iter: items"`. This provides `iter.index` (0-based), `iter.count` (1-based), `iter.first`, `iter.last`, `iter.even`, `iter.odd`. Without explicit naming, Thymeleaf defaults to `itemStat` (variable name + "Stat") which is verbose — always use `iter` for consistency.

## HTMX Integration (`ka:` attributes)

```html
<!-- GET request, swap result into #results -->
<button ka:get="search" hx-target="#results" hx-swap="innerHTML">Search</button>

<!-- POST with values -->
<button ka:post="admin-teams" ka:vals="action:'delete',teamId:team.teamId"
        hx-target="#main-content" hx-confirm="Delete this team?">Delete</button>

<!-- Form submission -->
<form ka:post="this" hx-target="#main-content">
  <input name="email" type="email"/>
  <input type="hidden" name="action" value="save"/>
  <button type="submit">Save</button>
</form>
```

**Principle: use `ka:` only when it adds value (preprocessing, expression evaluation, URL resolution). Use `hx-*` directly for everything else.**

| `ka:` attribute | Why `ka:` | Notes |
|-----------------|-----------|-------|
| `ka:get`, `ka:post`, `ka:put`, `ka:patch`, `ka:delete` | Resolves template name to URL + prepends contextPath | `"search"` → `/search`, `"this"` → current page URL, `"admin-teams"` → `/admin-teams` |
| `ka:vals` | Evaluates JS expression → JSON + single-quote wrapping; an apostrophe, `&` or `<` in a value is entity-escaped (the browser decodes it back) — pass raw data, never pre-escape | See syntax note below |
| `ka:data` | Alpine binding + hidden input injection | See Alpine.js section |
| `ka:dispatch` | Declarative custom-event dispatch on click; reads co-located `ka:vals` for the event detail | See "Declarative Custom-Event Dispatch" section |

For all other HTMX attributes, use `hx-*` directly — no `ka:` wrapper needed:

```html
<button ka:get="search" hx-target="#results" hx-swap="innerHTML">Search</button>
<button ka:post="items" ka:vals="action:'delete',id:item.id"
        hx-target="#main-content" hx-confirm="Delete this item?">Delete</button>
```

Common `hx-*` attributes: `hx-target`, `hx-swap`, `hx-trigger`, `hx-confirm`, `hx-push-url`, `hx-select`, `hx-indicator`, `hx-boost`, `hx-include`, `hx-ext`, `hx-disabled-elt`. See [HTMX docs](https://htmx.org/reference/) for the full list.

**`ka:vals` syntax note:** The value is evaluated as a JS expression returning an object. Write `ka:vals="action:'delete',id:item.id"` — the engine implicitly wraps it as `{action:'delete',id:item.id}`. Do NOT add explicit braces.

### HTMX error surface

HTMX by default only swaps response content on 2xx. On 4xx/5xx it fires `htmx:responseError`, on network failures `htmx:sendError`, on timeouts `htmx:timeout` — and does nothing visible with any of them. Users click, nothing happens, no feedback. Do not ship an HTMX app without a global handler for these events.

The canonical fix lives in the layout shell so every HTMX navigation benefits. Keep the handlers in an external `pub/app.js` alongside `pub/app.css` (inline scripts block strict CSP policies and grow the shell over time):

**`pub/app.js`**
```js
document.addEventListener('htmx:configRequest', function (e) {
  var token = document.querySelector('meta[name="csrf-token"]');
  if (token) e.detail.headers['X-CSRF-Token'] = token.content;
});

function showHtmxError(msg) {
  var host = document.getElementById('htmx-error-toast');
  if (!host) return;
  host.textContent = msg;
  host.classList.remove('hidden');     // class to show/hide is framework-specific —
  clearTimeout(host._hideTimer);       //   see CSS_*.md for the concrete class to use
  host._hideTimer = setTimeout(function () { host.classList.add('hidden'); }, 6000);
}

document.addEventListener('htmx:responseError', function (e) {
  var status = e.detail.xhr.status;
  var path = e.detail.requestConfig && e.detail.requestConfig.path;
  showHtmxError('Request failed (' + status + ')' + (path ? ' — ' + path : ''));
});
document.addEventListener('htmx:sendError', function () {
  showHtmxError('Network error — check your connection and try again.');
});
document.addEventListener('htmx:timeout', function () {
  showHtmxError('Request timed out — please try again.');
});
```

**`layout.html`** — load with `defer` so listeners register before DOMContentLoaded, and `ka:nocache="true"` on every static asset (CSS, JS) you serve from `/pub/` so local-dev edits show up immediately AND deploys bust the browser cache for users:

```html
<head>
  ...
  <link href="/pub/tailwind.css" rel="stylesheet" ka:nocache="true"/>
  <link href="/pub/app.css" rel="stylesheet" ka:nocache="true"/>
  <meta th:if="context.csrf" name="csrf-token" th:content="context.csrf.token"/>
  <script src="/pub/app.js" ka:nocache="true" defer></script>
</head>
<body>
  <!-- See CSS_*.md for the concrete toast container classes (color, position, hide-class) -->
  <div id="htmx-error-toast" role="alert"></div>
  ...
</body>
```

`ka:nocache="true"` works on both `<script src>` and `<link href>` (and any element with a URL attribute). The processor reads the file's lastModified timestamp via the resource resolver and rewrites the URL to `<original>?ts=<lastModifiedMillis>`. Same path each render until the file changes, then a new `?ts=` flips and browsers re-fetch. The `ka:nocache` attribute itself is scrubbed from the rendered HTML so it never leaks into the browser.

**Why this matters:** Silent failures are indistinguishable from "nothing happened" — users retry, blame the click, or think the feature is broken. The toast gives a non-intrusive signal that something went wrong and a hint at what. Pair with server-side logging so ops has the matching stack trace. This is *required hygiene* for any HTMX-driven UI, not an optional polish pass.

Dev verification: open browser devtools console on any page and run `htmx.ajax('GET', '/nonexistent', {target: '#main-content'})` — the toast should appear.

## Declarative Custom-Event Dispatch (`ka:dispatch`)

When a click should fire a `CustomEvent` (open a modal, signal the palette, broadcast a UI state change), `ka:dispatch="event-name"` emits the equivalent `onclick` handler. The (optional) co-located `ka:vals` becomes the event's `detail`.

```html
<button ka:dispatch="open-edit-user"
        ka:vals="userId:u.userId,role:u.role,products:(u.karateProductIds || [])">
  Edit
</button>
```

Renders to:

```html
<button onclick='window.dispatchEvent(new CustomEvent("open-edit-user",
        {detail: {"userId":"u-42","role":"admin","products":[...]},
         bubbles: true, composed: true}))'>Edit</button>
```

The CustomEvent fires with `bubbles: true, composed: true` so listeners attached at `window` or `document` (the typical "open modal anywhere" pattern) receive it.

**Why this exists:** before this directive the same pattern needed a hand-written `onclick="window.dispatchEvent(new CustomEvent(...))"` plus `data-*` attributes for every server value the JS needed (Gotcha #18 — server values must transit `data-*` to be readable in client JS). With `ka:dispatch` the values are evaluated server-side and inlined directly into the emitted JS — no `data-*` round-trip. Vue's `@click="$emit(...)"` and HTMX's `hx-trigger` are the rough mainstream parallels.

**Trigger:** click by default. Embed an `@<event>` suffix in the value to bind to a different DOM/htmx event — emitted as `hx-on:<event>` (htmx must be loaded on the page). Whitespace around `@` is optional.

```html
<!-- htmx event after a swap completes -->
<div ka:dispatch="users-refreshed @ htmx:afterSwap"></div>

<!-- plain DOM event -->
<select ka:dispatch="role-changed @ change"></select>
```

**Listener side (typical):**

```js
window.addEventListener('open-edit-user', function (e) {
  Alpine.store('editUserModal', { open: true, ...e.detail });
});
```

## Alpine.js Binding (`ka:data`)

Deep dive: `Skill.help('markup', 'alpine')`; form state and submission: `Skill.help('markup', 'forms')`.

```html
<script ka:scope="local">
  _.initialForm = { email: '', role: 'user', products: [] };
</script>
<form ka:data="form:initialForm" ka:post="manage-team" hx-target="#main-content">
  <input x-model="form.email" type="email"/>
  <!-- See CSS_*.md for the concrete active/inactive button classes -->
  <button x-on:click="form.role='admin'"
          x-bind:class="form.role=='admin' ? '<your-active-btn-class>' : '<your-inactive-btn-class>'">Admin</button>
  <button type="submit">Save</button>
</form>
```

**What `ka:data="form:initialForm"` does automatically:**
1. Adds `x-data='{ form: {"email":"","role":"user","products":[]} }'` (single-quoted for safe JSON embedding)
2. On `<form>`: injects `<input type="hidden" name="form" x-bind:value="JSON.stringify(form)"/>`

**Server reads it back:** `var form = request.paramJson('form');`

## Fragment Components

Deep dive: `Skill.help('markup', 'components')`.

**Directory convention:** pages at root, reusable fragments in `components/`, static assets in `pub/`.

```
app/
  index.html          ← / (layout shell)
  header.html         ← nav bar (included via th:replace)
  dashboard.html      ← /dashboard
  session.html        ← /session
  components/
    badge.html        ← reusable fragment (th:insert, not a routable URL)
    modal.html        ← reusable fragment
  pub/
    app.css           ← /pub/app.css (served as-is, no template processing)
```

URLs map to templates automatically: `/login` → `login.html`, `/admin/users` → `admin/users.html`. Static files under `pub/` are served raw. See App Setup (`Skill.help('markup', 'app-setup')`) for server configuration.

**Define a component:** the fragment shape is framework-agnostic; the class strings come from your `CSS_*.md`.

```html
<!-- components/badge.html — see CSS_*.md for the concrete state classes -->
<span th:fragment="badge"
      th:class="'badge ' + (state == 'ready' ? '<success>' : state == 'error' ? '<danger>' : '<neutral>')"
      th:text="state">state</span>
```

**Use it with parameters (`th:insert` keeps host element, `th:replace` replaces it):**
```html
<!-- th:insert + th:with on the same element — works -->
<span th:insert="components/badge :: badge" th:with="state: session.state"></span>
<!-- th:replace + th:with — wrap th:with on a PARENT element (see Gotcha #9) -->
<span th:with="state: session.state"><span th:replace="~{components/badge :: badge}"></span></span>
```

## Loading Indicators

```css
/* In app.css */
.ka-indicator { display: none; }
.htmx-request .ka-indicator { display: inline-block; }
.htmx-request .ka-indicator-hide { display: none; }
```

```html
<button ka:post="save" hx-target="#result">
  <span class="ka-indicator-hide">Save</span>
  <!-- Spinner element class is CSS-framework-specific — see CSS_*.md for the concrete spinner -->
  <span class="ka-indicator <your-spinner-class>"></span>
</button>
```

## Dev Mode — Hot Reload + Fragment Trace

`ServerConfig.devMode(true)` enables hot template reloading (every request re-parses templates so HTML/JS edits show up without a server restart). Switch on at server start:

```java
ServerConfig serverConfig = new ServerConfig(resourceRoot)
    .devMode(true)
    .devTrace(true)   // optional — adds fragment-trace comments
    ...
```

When devMode is on, you'll see a startup banner: `karate dev-mode enabled — templates hot-reload on every request`. Template errors that fall through to the fallback 500 response also include the formatted block (resource, line/col, source context, root cause) in the response body — same content as the server log. With devMode off, the response body is opaque (`Internal Server Error`) so template paths and source lines don't leak.

### `devTrace` — fragment composition trace

When `devTrace(true)` is also enabled, every `th:insert` resolution is wrapped with HTML comments in the rendered output:

```html
<!-- ka:fragment-begin (insert) components/modal :: shell depth=0 with={title:'Edit User',size:'lg',form: form} -->
<div class="fixed inset-0 ...">...resolved fragment content...</div>
<!-- ka:fragment-end (insert) components/modal :: shell depth=0 -->
```

`Cmd-F "ka:fragment"` in view source surfaces every fragment that rendered, with the call-site `th:with` payload visible — the analog to React DevTools' props inspector.

**Activation:**
- Programmatic: `ServerConfig.devTrace(true)`
- Env var: `KARATE_DEV_TRACE=true mvn ...` (read once when `ServerConfig` is constructed; programmatic call overrides)

**Production safety:** `devTrace` is silently a no-op unless `devMode=true` as well. Without that gate the trace would leak fragment names + scoped-vars to end users. Banner: `karate dev-trace enabled — fragment boundaries emitted as HTML comments (never enable in production)`.

**Three boundaries are traced:**
1. **`th:insert`** — host preserved, comments inside body wrapping the fragment.
2. **`th:replace`** — host gone, comments wrap the replacement fragment directly.
3. **Shell wrap** — when `ServerConfig.shellTemplate(...)` is set, comments mark the page-content slot inside the shell. Kind shows as `(shell)`, expression shows the page template name. Every full-page nav has these.

The first two are implemented by injecting a synthetic `<ka-trace>` element around the resolved fragment IModel inside the structure-handler pipeline; a post-processor middleware suppresses the wrapper tags while emitting the comments. Shell trace is a simple string-wrap of the content HTML before it's interpolated into the shell's `th:utext="content"` slot.

**Performance:** zero overhead when `devTrace=false` — the post-processor middleware isn't even registered with Thymeleaf in that case. When on, dev-only cost is one synthetic element per fragment + an O(1) tag-name comparison per element in the middleware.

## Built-in Objects (Server Mode)

| Object | Key Properties |
|--------|---------------|
| `request` | `.method`, `.get`, `.post`, `.ajax`, `.param(name)`, `.paramValues(name)`, `.paramJson(name)`, `.paramInt(name)`, `.body`, `.path`, `.pathMatches(pattern)`, `.header(name)`, `.headerValues(name)`, `.cookies` (map: `cookies['name'].value`), `.file(name)` (first multipart upload as `{name, filename, contentType, value, bytes, size}`), `.files(name)` (list of all uploads with that name), `.cache(key, fn)` (per-request memoize, see below) |
| `response` | `.status`, `.body`, `.headers` |
| `session` | Any key/value (null until `context.init()`) |
| `context` | `.init()`, `.close()`, `.redirect(path)`, `.switch(template)`, `.flash`, `.actions` (POST-handler registry, see below), `.toJson()`, `.fromJson()`, `.uuid()`, `.template`, `.get(name, default?)` / `.set(name, value)` (per-render scope; see below), `.getGlobal(name, default?)` / `.setGlobal(name, value)` (per-request engine globals; see below) |
| *global variables* | Any name registered via `ServerConfig.globalVariables(Map.of("utils", myUtils))` — available as `utils` in expressions and `ka:scope` |

Global variables are how Java logic is exposed to templates. See App Setup (`Skill.help('markup', 'app-setup')`) for `SimpleObject` and `JavaCallable` patterns.

### `context.get(name, default?)` — optional fragment params

Fragments often read names that the caller may or may not have passed via `th:with`. Bare-name reads of unbound names throw `ReferenceError` (gotcha #11 design); `context.get` is the safe accessor:

```html
<!-- Inside a fragment that takes optional `spacing` and `target` params -->
<form th:fragment="formButton"
      th:with="spacing: context.get('spacing', 'mb-4'),
               target:  context.get('target',  '#main-content')"
      th:attr="'hx-target': target"
      th:classappend="spacing">
  ...
</form>
```

**Contract:**
- `context.get('name')` — returns the bound non-null value if found in the underscore map (`_`) or the wrapped Thymeleaf scope (which carries every `th:with`-bound name, every global, every level-flushed entry); returns `null` on miss.
- `context.get('name', default)` — same lookup, returns `default` when the value is unbound or null.
- Lookup is cheap; no need to memoize.
- Use this in fragments for **optional params**. Use bare names (which throw on miss) for **required params** — the strict-throw catches missing args at the call site.

The same lookup is reachable as `_.name` (returns null on miss, no default arg). Use `context.get` when you want to declare the default inline; use `_.name` when you don't have a default or want to preserve an explicit `_.foo = null`.

### `context.set(name, value)` — per-render writer

Symmetric write side for `context.get`. Stores `value` in the per-render `_` underscore namespace — equivalent to writing `_.<name> = value` from JS, just reachable via the same `context.<verb>(name, …)` shape as the rest of the context API.

```html
<script ka:scope="local">
  context.set('theme', 'dark');
  // identical effect:  _.theme = 'dark'
</script>
<span th:text="context.get('theme')"></span>
```

**Lifetime:** dies when the current template render ends. Same scope as `_`, same constraints — content's `context.set('foo', …)` is NOT visible to the shell render. For the cross-render case, see `context.setGlobal` below.

### `context.setGlobal(name, value)` / `context.getGlobal(name, default?)` — per-request engine globals

The escape hatch for handing a value from a content render up to the shell render within the same request — fills the gap that used to make `_.crumbs = […]` a silent no-op (Gotcha #26). Routes to the per-request JS engine bindings, so the value lives as a script-level global for the rest of the request.

```html
<!--/* content page (signin.html) */-->
<script ka:scope="local">
  context.setGlobal('pageTitle', 'Sign in');
</script>

<!--/* shell (layout.html) */-->
<title th:text="context.getGlobal('pageTitle', 'Playground · ' + (contentTemplate ? contentTemplate : 'home'))"></title>
```

**Contract:**
- `context.setGlobal('name', value)` — `engine.put(name, value)`. Visible as a bare JS name in any subsequent eval (local or global) within this request, AND as `context.getGlobal('name')`.
- `context.getGlobal('name')` — returns the engine binding's value if set (non-null), else `null`.
- `context.getGlobal('name', default)` — same, returns `default` when unset.
- Per-request lifetime; never crosses requests.

**When to reach for it.** Almost never. The two real use cases are:
1. **Page-controlled shell metadata** (titles, meta description, body class) where the shell's derived default is wrong for one specific page.
2. **Content overriding shell-derived breadcrumbs** when the route shape isn't in `categories`.

For everything else, prefer the existing channels — `_.foo = …` for per-render state, `session` for per-user persistence, `context.flash` for redirect messages, shell derivation from `request.path` for nav/crumb state. `setGlobal` is the explicit escape hatch; using it for things that fit the standard channels just adds non-local coupling between content and shell.

**What about `request.cache` or `session`?** `request.cache(key, fn)` is lazy-compute memoize, not a write/read bag — using it for write/read pairs requires a dummy fn at the read site and is order-fragile (first call's fn wins, even a `() => null` from an early shell partial would lock out the content's later set). `session.foo` has the wrong lifetime (cross-request) — set "Sign in" on `/signin`, navigate elsewhere, the next page sees the stale value. `setGlobal` is purpose-built for the eager-write/read-later, request-scoped case.

### `request.cache(key, fn)` — per-request memoize

Server-side JS in templates evaluates per fragment, so a layout shell and a page can both call the same expensive function (DB query, palette index build) within one HTTP request. `request.cache` deduplicates within a request:

```html
<script ka:scope="global">
  // First fragment to ask computes; later fragments get the cached value.
  var team = request.cache('team', () => utils.getTeamWithUsers());
</script>
```

**Contract:**
- Always two args: a string `key` and a thunk `fn`.
- First call with a key invokes `fn`, stores the result on the request, returns it.
- Subsequent calls with the same key return the stored value — `fn` is not invoked again.
- A cached `null` / `undefined` is still a cache hit.
- The store dies with the request — never crosses request boundaries.

**Use it for:** pure read-only computations a render needs in multiple places (palette indexes, "current user" walkers, DB queries the shell + page both use). **Not for:** anything mutating, anything keyed by external state you didn't put in the key, or anything you want to survive a redirect (use `context.flash` for that — different lifetime).

## Flow Control: `context.redirect` and `context.switch`

Both `context.redirect(path)` and `context.switch(template)` **short-circuit** the current evaluation — statements after the call do not run, and the rest of the template does not render. Guard clauses can be written naturally, no defensive `th:if` required:

```html
<script ka:scope="global">
  // Auth guard — nothing below runs if there's no user, and the body below
  // does not render, so session.user.name is always safe to dereference.
  if (!session?.user) {
    context.redirect('/signin');
  }
</script>
<p>Welcome, <span th:text="session.user.name">name</span>!</p>
```

**`redirect` vs `switch`:**
- **`context.redirect(path)`** — sends a 302 with `Location: path`. Full HTTP round-trip: URL changes, browser history entry, flash messages survive to the next request.
- **`context.switch(template)`** — renders `template` instead of the current one. Same request, same URL, different HTML — ideal for "POST to mutate, then render the list view" inside HTMX swaps, where a 302 would break the swap flow.

```html
<!-- admin-team-create.html — POST creates a team, then switches to the list template -->
<script ka:scope="global">
  if (request.post) {
    utils.createTeam(request.param('name'));
    context.switch('admin-teams');   // render updated list HTML; no redirect
  }
</script>
<form ka:post="this" hx-target="#main-content">
  <input name="name"/>
  <button type="submit">Create</button>
</form>
```

Both methods work identically from `ka:scope="global"` and from API handlers under `/api/`. The short-circuit is clean — no error logs, no 500 responses — so handlers stay compact.

## Centralized Auth with `requestFilter`

Per-template `if (!session.user) context.redirect('/signin')` guards are easy to forget and even easier to mis-apply. Centralize at the `ServerConfig.requestFilter` layer — it runs after session load and short-circuits the request when it returns non-null:

```java
new ServerConfig(...)
    .sessionStore(...)
    .requestFilter((req, ctx) -> {
        String path = req.getPath();
        if (isPublic(path)) return null;                    // /signin, /signout, /api/health, webhooks…
        var session = ctx.getSession();
        Map<String,Object> user = session == null ? null
                : (Map<String,Object>) session.getData().get("user");
        if (user == null) {
            return path.startsWith("/api/") ? json(401, "unauthorized") : redirect("/signin");
        }
        if (path.equals("/admin") || path.startsWith("/admin/")) {
            if (!Boolean.TRUE.equals(user.get("admin"))) {
                return path.startsWith("/api/") ? json(403, "forbidden") : redirect("/");
            }
        }
        return null;
    });
```

**Rules in order:** public allowlist → authenticated by default → path-prefix admin gate. Templates then drop their opening auth guards — `session.user` is guaranteed set by the time a template's JS runs. Business-level authorization ("user X can only see their own team") stays in the handler; the filter is for authentication and role gating, not fine-grained resource ownership.

## Layout shell + bookmarkable URLs

Fragment pages are elegant for HTMX swaps, but broken for direct navigation: typing `/admin/teams` in the address bar or reloading returns a bare `<div>` with no CSS-framework chrome, no nav, no `<head>`. Fix this in one config call — the server wraps full-page navigations with a shell template and sends HTMX requests through bare:

```java
new ServerConfig(resourceRoot)
    .shellTemplate("layout.html")            // wraps non-HTMX requests
    .rawPaths("/signin", "/signout")         // pages that ship their own <html>
```

**How the router decides** (inside `ServerRequestCycle.handleTemplate`):
1. Resolve URL → content template as usual (templateRoutes, file lookup, directory-index fallback).
2. Render the content template (its `ka:scope` runs; flash/session/redirect/switch commit).
3. If content called `context.redirect()` → return 302, shell never runs.
4. If `HX-Request: true` OR path is in `rawPaths` OR shell is not configured → return the bare content.
5. Else bind `content` (rendered HTML) + `contentTemplate` (resolved path) into vars and render the shell.

**The shell template:**
```html
<!DOCTYPE html>
<html>
<head>
  <title th:text="'MyApp — ' + contentTemplate">MyApp</title>
  <!-- CDN block, CSRF meta, etc. — all the chrome. See "Shell composition checklist" below. -->
</head>
<body>
  <div th:replace="header"></div>
  <!-- Flash classes are CSS-framework-specific — see CSS_*.md -->
  <div th:if="context.flash.success" class="<your-success-alert-class>" th:text="context.flash.success"></div>
  <div id="main-content" th:utext="content">content</div>
</body>
</html>
```

**Shell composition checklist.** A working shell `<head>` + `<body>` typically assembles seven pieces. Most are documented elsewhere in this skill or in the `CSS_*.md` siblings — this list is the index that ties them together so a fresh build doesn't omit one and silently break (e.g. a missing CSRF meta makes htmx POSTs 403 once a session exists; a missing app.js means no `X-CSRF-Token` header is injected; a missing theme pre-paint causes a flash of light theme before dark resolves).

| # | Where | Piece | Read |
|---|---|---|---|
| 1 | `<head>` (very early, inline) | Theme pre-paint script — resolves dark/light from `localStorage` + `prefers-color-scheme` and toggles `html.dark` *before* CSS paints, killing the flash | `Skill.help('markup', 'tailwind')` §Dark mode (or your CSS framework's equivalent) |
| 2 | `<head>` (conditional) | CSRF meta — `<meta th:if="context.csrf" name="csrf-token" th:content="context.csrf.token"/>` — read by app.js to inject `X-CSRF-Token` on every htmx request. Skipped when no session (no token to leak) | §"HTMX error surface" + the auth-signin / auth-session-guard catalog entries |
| 3 | `<head>` | CSS framework CDN + design-token config (Tailwind inline `tailwind.config = { ... }` or Bootstrap link tag) | `Skill.help('markup', 'tailwind')` §Setup / `CSS_BOOTSTRAP.md` §Setup |
| 4 | `<head>` (in this exact order) | htmx → app.js (`ka:nocache="true" defer`) → Alpine. Loading app.js *before* Alpine is load-bearing for any `Alpine.magic` / `Alpine.directive` registration | §"CDN Block" above + Gotcha `#cdn-script-order` |
| 5 | `<body>` | Chrome — rail, topbar, breadcrumb, theme toggle, auth chip. Framework-specific markup | `Skill.help('markup', 'tailwind')` §"Layout shell" / playground `app/layout.html` |
| 6 | `<body>` | `<main id="main-content" th:utext="content">content</main>` — the slot. `id` is the canonical `hx-target` for in-page swaps; `th:utext` (not `th:text`) prevents HTML-escaping the rendered fragment | §"Layout shell + bookmarkable URLs" (this section), slot variable rules above |
| 7 | end of `<body>` | Global mounts — palette (⌘K), error toast host, modal stack root, notification stack root. Mounted once in the shell so every page can dispatch to them | playground `app/layout.html` + the `palette` / `notification-stack` catalog entries |

**`pub/app.js` content** is the other half of #2 and #4 — it registers `htmx:configRequest` (CSRF header injection), `htmx:responseError` / `htmx:sendError` / `htmx:timeout` (toast surfacing), the theme `cycleTheme()` helper, and any `Alpine.magic` you add (e.g. `$live` in the playground). See §"HTMX error surface" for the canonical body.

**Live exemplar:** `markup/playground/tailwind/src/main/resources/app/layout.html` is the reference build of all seven pieces, in order, with comments at each step.

**Content pages become pure fragments** — no `<html>`, no `<head>`, no nav, no flash blocks. Just the `<section>` or `<div>` that is the page. Works identically in both modes: reloaded `/admin/teams` → shell + fragment; `ka:get="/admin/teams"` HTMX swap → bare fragment.

**Render order matters.** Content renders *before* the shell. This means:
- Side effects in content (`context.flash.success = ...`, `session.x = y`, `context.redirect()`, `context.switch()`) commit before the shell evaluates — so the shell can read them naturally.
- The shell sees the *final* session state, including anything the content just created via `context.init()`.
- `context.switch('other-page')` in content makes `other-page` the new content; the shell still wraps it.

**`_` does NOT cross the boundary.** A page's `<script ka:scope="global">` `_.foo = ...` lives only inside that page's render — when the shell evaluates its own `<script ka:scope="global">` it sees a fresh `_`, not the content's. Concretely: a page that writes `_.crumbs = [...]` and expects the shell to render it will hit a silent no-op (Gotcha [#shell-derives-crumbs](#shell-derives-crumbs)). Anything the shell needs from the content has to flow through one of the four crossing channels listed above, or via the explicit `context.setGlobal('name', value)` / `context.getGlobal('name')` escape hatch (see §`context.setGlobal/getGlobal`) — purpose-built for this exact case.

**Slot variable names** inside the shell:
- `content` — rendered content HTML (use `th:utext`, since it's already rendered HTML).
- `contentTemplate` — resolved content template path, e.g. `admin/teams.html`. Useful for `<title>`, breadcrumbs, body class.
- `request.path` — the request URL, also available inside the shell. Combined with a server-built `categories` table this is enough to derive crumbs / nav state without depending on the content page (see the playground `app/layout.html` for the reference pattern).

**`rawPaths` use cases:**
- Pre-session pages that ship bespoke branding (signin, signout).
- Marketing landing pages with a different visual shell.
- Health probes, webhooks (though these usually live under `/api/*` which isn't shell-wrapped anyway).

Match is exact on the request path (not prefix). Use request paths, not template names.

**What's NOT shell-wrapped (automatically):**
- `/api/*` routes (handled by `.js` API handlers).
- `/pub/*` static files.
- Error responses (404 / 500) — they go through their own template path.
- HTMX requests (identified by `HX-Request: true`).

**Placement of visit counters, auth guards, flash messages:** favor the shell for cross-cutting chrome (nav, flash), and the content for page-specific logic (auth guard that redirects, page-specific data prep). Because the content runs first, the shell sees the result either way.

## Grouping pages under a directory prefix

`/foo` maps to `foo.html` by default. If `foo.html` doesn't exist but `foo/index.html` does, karate-core falls back to the index variant automatically (standard web-server convention). So moving `admin.html`, `admin-teams.html`, `admin-team-edit.html` into `admin/index.html`, `admin/teams.html`, `admin/team-edit.html` works transparently: URLs become `/admin`, `/admin/teams`, `/admin/team-edit` and no `templateRoute` is needed.

**Use absolute paths in `ka:get` / `ka:post` once you nest.** HTMX resolves relative URLs against the current document path, which breaks when navigating across subdirectories (`ka:get="team-edit"` from `/admin/teams` resolves to `/admin/team-edit` but is easy to get wrong in deeper hierarchies). `ka:get="/admin/team-edit"` is unambiguous regardless of which page it's on.

## Custom Error Pages (404 / 500)

Configure friendly error templates via `ServerConfig`:

```java
new ServerConfig(resourceRoot)
    .errorTemplate404("errors/404.html")   // rendered for unmatched paths
    .errorTemplate500("errors/500.html")   // rendered when a handler/template throws
    ...
```

If not set, karate-core falls back to a plain-text response (`Internal Server Error: ...` or `Not Found: ...`). With a template set, the response is `text/html; charset=utf-8` and the template receives:

| Template | Extra vars | Plus |
|---|---|---|
| 500 | `error` (exception message — the outer one, e.g. Thymeleaf's `TemplateProcessingException`) | all globals, `request`, `response`, `context`, `session` |
| 404 | `path` (the unmatched URL path) | all globals, `request`, `response`, `context`, `session` |

Globals like `devMode`, `utils`, or any app-specific registry are available — use them to gate dev-only details:

```html
<!-- errors/500.html -->
<h1>Something went wrong</h1>
<p>We've logged the error.</p>
<div th:if="devMode">
  <pre th:text="error">error</pre>
</div>
```

**Security:** don't render `error` to end users in production — exception messages can leak internal paths, SQL, or stack-level details. Gate with `devMode` (or your own flag) and rely on server logs for real diagnostics.

**Failure-of-the-failure handling:** if the error template itself throws, karate-core silently falls through to the plain-text response — so don't rely on heavy logic (DB calls, session mutation) inside error templates.

## Gotchas

(CSS-framework-specific gotchas live in the corresponding `CSS_*.md` file. The list below is framework-agnostic.)

> If you hit a rough edge below in real work, report it rather than silently adopting the workaround.

### Index

Each gotcha has a stable slug anchor (e.g. `#th-attr-quoted-key`); cross-references use slugs so they survive renumbering. Grouped by theme below; the gotcha bodies follow in chronological-discovery order.

#### By theme

**Templating & fragments** —
[`#th-attr-quoted-key`](#th-attr-quoted-key) ·
[`#th-if-empty-string-truthy`](#th-if-empty-string-truthy) ·
[`#java-methods-not-on-js-strings`](#java-methods-not-on-js-strings) ·
[`#th-with-colon-syntax`](#th-with-colon-syntax) ·
[`#fragment-inline-page-walk-fails`](#fragment-inline-page-walk-fails) ·
[`#thymeleaf-opaque-parse-errors`](#thymeleaf-opaque-parse-errors) ·
[`#th-include-deprecated`](#th-include-deprecated) ·
[`#th-if-on-script-ignored`](#th-if-on-script-ignored) ·
[`#th-fragment-no-params`](#th-fragment-no-params) ·
[`#html-comments-ship`](#html-comments-ship)

**Server-side scripting (`ka:scope`)** —
[`#scope-bare-vs-underscore`](#scope-bare-vs-underscore) ·
[`#ternary-colon-confuses-parser`](#ternary-colon-confuses-parser) ·
[`#response-status-headers`](#response-status-headers) ·
[`#java-exceptions-js-catchable`](#java-exceptions-js-catchable) ·
[`#expressions-only-in-th-and-ka`](#expressions-only-in-th-and-ka)

**HTMX integration** —
[`#vals-on-submit-dropped`](#vals-on-submit-dropped) ·
[`#htmx-double-colon`](#htmx-double-colon) ·
[`#dont-th-attr-the-ka-attrs`](#dont-th-attr-the-ka-attrs)

**Alpine integration** —
[`#alpine-x-show-css-conflict`](#alpine-x-show-css-conflict) ·
[`#alpine-double-quotes-break-thymeleaf`](#alpine-double-quotes-break-thymeleaf) ·
[`#alpine-x-cloak-flicker`](#alpine-x-cloak-flicker) ·
[`#ka-data-nested-scope`](#ka-data-nested-scope)

**Layout, shell & cross-page state** —
[`#asset-paths-must-be-absolute`](#asset-paths-must-be-absolute) ·
[`#localstorage-poison-undefined`](#localstorage-poison-undefined) ·
[`#shell-derives-crumbs`](#shell-derives-crumbs)

**Setup & dev mode** —
[`#dev-mode-required`](#dev-mode-required) ·
[`#request-no-pathparam-method`](#request-no-pathparam-method) ·
[`#session-no-store-opaque`](#session-no-store-opaque) ·
[`#encode-uri-component`](#encode-uri-component) ·
[`#th-with-unset-resolves-null`](#th-with-unset-resolves-null)

#### Cross-references (orphan slugs)

Slugs cited from CATALOG.md / playground that point to other parts of this doc rather than to a standalone gotcha:

| Slug | Points to |
|---|---|
| `#cdn-script-order` | §CDN Block — Alpine 3 microtask + script ordering (defer-loading magic must come BEFORE Alpine CDN) |
| `#fragment-th-with` | (under [`#th-include-deprecated`](#th-include-deprecated)) `th:insert + th:with` is the right shape for fragment params |
| `#fragment-replace-host-disappears` | (under [`#th-include-deprecated`](#th-include-deprecated)) `th:replace` removes the host before the fragment evaluates |
| `#data-mirror-typing` | (under [`#ka-data-nested-scope`](#ka-data-nested-scope)) `ka:data-mirror` for payload-driven modals |
| `#hx-request-shell-bypass` | §Layout shell — `HX-Request: true` skips the shell automatically |
| `#rawpaths-opt-in` | §Layout shell — `ServerConfig.rawPaths(...)` for endpoints that always ship bare HTML |
| `#simpleobject-jskeys` | `Skill.help('markup', 'app-setup')` §Global variables — `SimpleObject.jsKeys()` + `jsGet()` |
| `#ddb-ttl-seconds` | `Skill.help('markup', 'app-setup')` §Sessions — DynamoDB TTL is epoch SECONDS, not millis |
| `#context-redirect-short-circuits` | §Flow Control — `context.redirect()` exits template rendering |
| `#urlhash-popstate-init` | (Pattern detail, /tabs) URL hash restore + popstate sync |
| `#localstorage-key-collisions` | (Pattern detail) namespaced keys when multiple Alpine factories persist to the same origin |
| `#alpine-text-select-during-drag` | (Pattern detail, /splitter) `user-select: none` + cursor classes during drag |
| `#hx-push-url-multi-param` | (Pattern detail) re-emit all preserved params in the URL builder |
| `#hx-target-id-stability` | (Pattern detail) `hx-target` IDs must survive the swap |

1. **<a id="vals-on-submit-dropped"></a>`ka:vals` on submit button inside HTMX form is silently dropped.** HTMX does not include `hx-vals` from buttons when the form has `hx-post`. Use a hidden `<input name="action" value="...">` instead. `ka:vals` on standalone elements (links, buttons with their own `ka:post`) works fine.

2. **<a id="htmx-double-colon"></a>`::` in HTMX event attributes conflicts with Thymeleaf.** Use `hx-on-htmx-after-request` (hyphen syntax) instead of `hx-on::after-request`.

3. **<a id="th-attr-quoted-key"></a>Attribute names with hyphens or colons need to be quoted in `th:attr`.** Hyphens are parsed as subtraction; colons confuse the JS-expression parser. Quote both:
    ```html
    <!-- WRONG — both fail -->
    <a th:attr="data-item-id: item.id, ka:get: baseUrl, hx-target: target"></a>
    <!-- RIGHT — quote every key with hyphen, colon, or any non-identifier char -->
    <a th:attr="'data-item-id': item.id, 'ka:get': baseUrl, 'hx-target': target"></a>
    ```
    Plain identifiers (`title`, `name`, `value`) don't need quoting. The rule of thumb: if the attribute name isn't a legal JS identifier, quote it. Same rule applies to `ka:with`, `hx-vals`, and `ka:dispatch` — anywhere karate-markup parses a value as an object literal.

    **Also: always use the colon form, never Thymeleaf's `=` form.** `th:attr="value=item.id"` / `th:attr="'hx-get'=item.linkUrl"` parsed by accident on karate-core ≤ 2.0.x (the lax parser tolerated `{key = expr}` object literals). As of **2.1.3.RC1** the ECMA-compliant parser rejects it — `invalid shorthand initializer: only allowed in destructuring pattern` — at template parse time (a 500 on first render). Write `th:attr="value: item.id"` / `th:attr="'hx-get': item.linkUrl"`. (Found migrating studio to 2.1.3.RC1 — eight `=`-form sites, all rendering 500.)

    Karate-core surfaces an actionable hint when this happens — the failure looks like:
    ```
    Error: SyntaxError parsing object-literal expression (...) — attribute keys
    `data-item-id`, `hx-target`, `ka:get` contain hyphens or colons and must be
    quoted. Try: 'data-item-id': item.id, 'hx-target': target, 'ka:get': baseUrl
    ```

4. **<a id="th-if-empty-string-truthy"></a>`th:if` truthiness deviates from JS for empty string.** Empty string `""` is **truthy** in template expressions, unlike JS where `if("")` is falsy. `null` and `undefined` are both falsy as expected. **Real-world failure mode:** a template like `<span th:if="planName" th:text="planName">` will render an empty pill when `planName` is `""` — visible as an empty bordered/coloured chrome element with no text inside.

    **Recommended idiom — drive falsiness from the JS side, not the template:**

    ```js
    // BAD — emits empty pill when team has no planName
    _.planName = '';
    if (currentTeam) _.planName = currentTeam.planName || '';

    // GOOD — null is reliably falsy in th:if
    _.planName = null;
    if (currentTeam && currentTeam.planName) _.planName = currentTeam.planName;
    ```

    `<span th:if="planName">` then behaves correctly. Avoid `th:if="planName.length > 0"` — it works but throws if `planName` is ever `null`/`undefined` (which is the whole reason you assigned a default). Avoid `th:if="planName != ''"` for the same reason — verbose and still needs a default.

    **Why not just leave the variable unset?** Because referencing an unset variable in `th:if` is a `ReferenceError` at template-render time. You must assign *something*, and `null` is the right something for an "absent" string-valued attribute.

5. **<a id="dev-mode-required"></a>Hot reload requires `devMode: true`.** Templates are cached in production. Set `ServerConfig.devMode(true)` or `KARATE_DEV_MODE=true` for development.

6. **<a id="thymeleaf-opaque-parse-errors"></a>Thymeleaf parse errors are opaque — no line numbers.** If a template fails to parse, use binary search: comment out half the template, render, narrow down. This is a Thymeleaf limitation.

7. **<a id="ternary-colon-confuses-parser"></a>Colons in ternary expressions near `th:` attributes can confuse the parser.** Split complex ternaries into `th:if`/`th:unless` pairs instead of inline ternaries with `:`.

    **Related — Elvis operator (`?:`) is NOT supported in karate-js.** Despite Thymeleaf-style template syntax familiarity, the expression engine is JS-spec-strict (karate-js targets Test262 conformance). The Elvis operator is not part of the JavaScript spec — it's a Groovy/Kotlin/PHP extension. Writing `contentTemplate ?: 'home'` produces `expected: [EXPR]` at parse time. **Use one of these JS-spec alternatives instead:**

    | Goal | Use | Behavior |
    |---|---|---|
    | Fallback when value is `null` or `undefined` only | `value ?? 'fallback'` (nullish coalescing, ES2020) | `'' ?? 'x'` → `''`; `null ?? 'x'` → `'x'` |
    | Fallback when value is any falsy (`null`, `undefined`, `''`, `0`, `false`) | `value \|\| 'fallback'` | `'' \|\| 'x'` → `'x'`; `0 \|\| 'x'` → `'x'` |
    | Standard ternary | `cond ? truthy : falsy` | both branches required |

    Empirical confirmation: `karate-js/Parser.java:585-589` strictly requires the truthy branch between `?` and `:`. No Elvis branch exists in the grammar; not a planned extension (deviating from the JS spec is a non-goal — the broader karate-js work is to *increase* Test262 conformance, not add non-standard sugar). Verified at karate-js 2.0.7.RC1.

8. **<a id="response-status-headers"></a>Setting response status/headers in `ka:scope` and `app/api/*.js`.** Use `response.status = 404` and either of these two equivalent forms for headers:

    | Form | Notes |
    |---|---|
    | `response.header('HX-Redirect', '/login')` | Recommended. 1-arg `response.header(name)` reads. 2-arg `response.header(name, value)` writes. `null` value removes the header. `List` value sets all values. |
    | `response.headers['HX-Redirect'] = '/login'` | Also works. Bracket-set coerces String → `[String]`, accepts List, removes on null. Reads still return the underlying `List<String>` (unchanged from earlier). |

    The same `request.header(name, value)` 2-arg setter is available on the request side for filter-style header injection.

9. **<a id="th-include-deprecated"></a>`th:include` is deprecated.** Use `th:insert` (keeps host element) or `th:replace` (replaces host element). **`th:with` scoping:** `th:insert + th:with` works on the same element. `th:replace + th:with` does NOT — `th:replace` removes the host element, losing scoped variables BEFORE the fragment evaluates, so the fragment sees `ReferenceError: <param> is not defined`. Put `th:with` on a parent element OR (preferred for component invocations) just use `th:insert`:

    ```html
    <!-- WRONG — fragment errors with ReferenceError on `spacing` -->
    <th:block th:replace="~{components/flash :: stack}" th:with="spacing: 'mt-3'"></th:block>
    <!-- RIGHT — th:insert preserves the th:with scope into the fragment -->
    <th:block th:insert="~{components/flash :: stack}" th:with="spacing: 'mt-3'"></th:block>
    ```

    The wrapping `<th:block>` is itself invisible (Thymeleaf strips it from output), so `th:insert` doesn't add any extra element to the rendered HTML. **Real-world bite from refactoring studio's modals into components:** every call site needs to be `th:insert` once you start passing parameters.

10. **<a id="alpine-x-show-css-conflict"></a>CSS-framework-specific class conflicts with Alpine `x-show`.** Some CSS frameworks ship utility classes that use `!important` (e.g. Bootstrap's `d-flex`, `d-none`). These override Alpine's inline `display: none` from `x-show="false"`. **Fix:** Use `:class` binding instead of `x-show`. See CSS_BOOTSTRAP.md for the concrete pattern. Tailwind's utility classes do not use `!important`, so this gotcha doesn't apply there.

11. **<a id="scope-bare-vs-underscore"></a>The `_` underscore namespace — design and discipline.** `_` is the script-local state object you write into from `ka:scope` (`_.foo = bar`), and it is **dual-lookup on read**: `_.foo` first checks the underscore map, then falls through to the wrapped Thymeleaf scope (which carries every `th:with`-bound name, every global, and every level-flushed value). Concretely:

    - **Writes always go to `_`.** `_.foo = 'bar'` mutates the underscore map. The map is flushed into the Thymeleaf scope at every level-increase, which is how a sibling element's `<span th:text="foo">` (bare name) sees the value.
    - **Reads via `_.foo` work everywhere.** Inside the writing script body, in the next script's body, in template attributes (`th:text="_.foo"`), inside fragments — all paths read through the same dual-lookup. Returns the bound value, or `null` when truly unset. **Never throws.**
    - **Reads via bare `foo` are strict.** Outside a `_.` prefix, an unbound name throws `ReferenceError` with a hint pointing at either `_.foo` or `th:with` at the call site (depending on whether `_` has it). This is intentional — it prevents JS-global-scope pollution and flags genuine mistakes (typos, missing fragment params) loudly.
    - **Within the same script body, after `_.foo = bar`, the bare name `foo` is NOT auto-bound.** That's the namespace discipline — you wrote into `_`, you read from `_`. Use `var foo = ...` for true intra-script intermediates only the script reads.

    **Why this design:** the `_` namespace exists specifically to keep template-mutated state out of the JS global scope. Auto-binding bare names from `_` would defeat the purpose; treating bare names as strict refs catches real bugs the lenient-fallback days used to silently paper over.

12. **<a id="encode-uri-component"></a>`encodeURIComponent`, `encodeURI`, `decodeURIComponent` and `decodeURI` work in `ka:scope`** as in the browser — use them for query strings (`context.redirect('/admin/users?email=' + encodeURIComponent(form.email))`).

13. **<a id="alpine-double-quotes-break-thymeleaf"></a>Double quotes inside `x-init`/`x-data` break Thymeleaf parsing.** Inline JS inside double-quoted Alpine attributes cannot contain double quotes — Thymeleaf's HTML parser sees them as closing the attribute. Use escaped single quotes (`\'`) for HTML inside JS strings within Alpine attributes: `x-init="el.innerHTML = '<i class=\'bi bi-check\'></i>'"`. For complex JS, move to a `<script>` block or external function.

14. **<a id="th-if-on-script-ignored"></a>`th:if` on `<script>` tags is ignored.** `<script th:if="condition">` renders unconditionally — the template engine doesn't process conditionals on script elements. **Workaround:** render the data into a hidden `<script type="application/json">` element, then check the condition in client-side JS. See §Cross-Page Data Passing for an example. The hand-rolled `<script type="application/json" id="x-data" th:utext="JSON.stringify(x)">null</script>` boilerplate is replaced by the **`ka:island` directive** — `<div ka:island="x"/>` emits the equivalent script tag with the value JSON-stringified server-side; the host element is replaced.

15. **<a id="alpine-x-cloak-flicker"></a>Alpine `x-show` with initially-false state needs `x-cloak`.** Elements with `x-show="false"` may flash visible before Alpine initializes (especially with `defer` loading). Add `x-cloak` to the element and `[x-cloak] { display: none !important }` to your CSS. CSS-framework-specific overlay/modal recipes live in your `CSS_*.md`.

16. **<a id="asset-paths-must-be-absolute"></a>Asset paths in the shell template must be absolute (`/pub/...`), not relative (`pub/...`).**
NOTE: This rule includes `<link rel="icon" href="/pub/favicon.ico" sizes="any"/>` — drop a `favicon.ico` (or PNG/SVG) into `src/main/resources/app/pub/` and reference it absolutely from the shell `<head>`. Any page that ships its own `<html>` (signin/signout/error-500 in `rawPaths`) needs the same `<link>` repeated since they bypass the shell. Without this, browsers fall back to a 404 GET for `/favicon.ico` on every cold visit (visible in DevTools Network), and the tab shows the default globe/blank icon.

 Browsers resolve relative URLs against the **current** page path, so `<link href="pub/app.css">` on the shell breaks on nested routes: a page at `/admin/users` fetches `/admin/pub/app.css` → 404. Any page at depth > 0 loses all shell-level CSS and JS. The failure mode is deceptive because CSS-framework CDN URLs are absolute (so the page visually renders) — but:
    - `.ka-indicator { display: none }` from app.css never applies → every HTMX spinner on the page appears permanently "on", making buttons look stuck-spinning long after the request completed. Easy to chase as a network / server bug when it's actually a 404 on the CSS.
    - `htmx:configRequest` listener from app.js doesn't register → CSRF header not injected → POSTs fail with 403.
    - `htmx:responseError` toast handler not registered → silent HTMX failures.

    **Fix:** always write `/pub/app.css`, `/pub/app.js`, etc. in the shell and in any page that ships its own `<html>` (signin, signout, error templates). Component fragments inserted via `th:insert` inherit the absolute paths from the shell, so they don't need to repeat them.

    ```html
    <!-- WRONG — breaks on /admin/users, /manage-team, and every other nested URL -->
    <link href="pub/app.css" rel="stylesheet"/>
    <script src="pub/app.js" defer></script>

    <!-- RIGHT -->
    <link href="/pub/app.css" rel="stylesheet"/>
    <script src="/pub/app.js" defer></script>
    ```

    Detection: open DevTools → Network on any nested URL — a 404 on a `pub/*` asset is this bug. Console shows `GET .../pub/... net::ERR_ABORTED 404`.

17. **<a id="java-exceptions-js-catchable"></a>Java host-function exceptions are JS-catchable**. When a `JavaCallable` / `SimpleObject` method or a `Java.type(...)` call throws a Java `RuntimeException`, server-side JS can intercept it with a standard `try { ... } catch (e) { ... }`. Inside `catch`, `e` is a JS `Error` with `e.message` (the original exception's message), `e.name` (`"Error"`), and `'' + e` yields `"Error: <message>"`. Uncaught exceptions still bubble up with the helpful `expression: <code> - <message>` framing, so nothing changes for pages that don't guard. Use this to map Java exceptions to user-facing responses — e.g.:

    ```javascript
    // api/renew.js
    try {
      var license = utils.decodeLicenseFile(request.body);
      response.body = JSON.stringify(license);
    } catch (e) {
      response.body = 'error:' + e.message;   // → "error:signature verification failed"
    }
    ```

    Two escape hatches that are NOT caught: (a) `FlowControlSignal` exceptions (`context.redirect`, `context.switch`) are intentional control flow and pass straight through; (b) errors inside a `try` body that originate from the engine itself (e.g. `a is not a function`) still surface with the usual "js failed:" framing — they become JS-catchable too, so take care that overly broad `catch (e)` blocks don't swallow genuine bugs. Prefer narrow `try` scopes around the single Java call you expect may fail.

18. **<a id="expressions-only-in-th-and-ka"></a>Server-side expressions only work inside `th:*` and `ka:*` attributes.** Expressions like `item.id` or `s.name` are NOT evaluated in plain HTML attributes (`onclick`, `@click`, `x-init`, `data-*`, etc.) — they render as literal text. **Fix:** Use `th:attr` to pass server values into `data-*` attributes, then read them in client-side JS via `dataset`:
```html
<!-- WRONG — s.id renders literally as the string "s.id", not the value -->
<button @click="fetch('/api/items/' + s.id)">Delete</button>

<!-- RIGHT — th:attr resolves the expression, client JS reads the data attribute -->
<td th:attr="'data-id': item.id">
  <button @click.stop="fetch('/api/items/' + $el.closest('td').dataset.id, {method:'DELETE'})">
    Delete
  </button>
</td>
```
This is the bridge between server-side (Karate template engine) and client-side (Alpine/vanilla JS). The `th:attr` attribute is the only way to inject server-computed values into elements that client-side JS will read. For `ka:vals`, this is handled automatically — the expression is evaluated server-side and emitted as `hx-vals='{"id":"abc123"}'`.

19. **<a id="dont-th-attr-the-ka-attrs"></a>Don't use `th:attr` to set `ka:get` / `ka:post` / `ka:vals` / `ka:data` dynamically — they're already dynamic.** `HxMethodProcessor` evaluates JS inside `ka:get` / `ka:post` / etc. natively, so a double-dynamic pattern (`th:attr` setting a `ka:*` attribute) is redundant and harder to read. Either interpolate with `${...}` inside the `ka:get` value, or use a whole-value `${expression}`:

    ```html
    <!-- WRONG — doubly dynamic, and bypasses karate's context-path + "this" resolution -->
    <a th:attr="'hx-get': '/admin/users?email=' + r.email" hx-target="#main-content">View</a>

    <!-- RIGHT — interpolate inside ka:get -->
    <a ka:get="/admin/users?email=${r.email}" hx-target="#main-content">View</a>

    <!-- ALSO RIGHT — whole-value JS expression -->
    <a ka:get="${'/admin/users?email=' + r.email}" hx-target="#main-content">View</a>
    ```

    The same rule applies to `ka:vals` (expression already) and `ka:data` (Alpine binding). Reach for `th:attr` only when you need to set an ordinary HTML attribute (`data-*`, `title`, `href` for non-HTMX nav) that has no `ka:*` counterpart.

20. **<a id="ka-data-nested-scope"></a>`ka:data="form:initialForm"` opens a NESTED Alpine scope that shadows the outer one — fine for user-typed forms, broken for payload-driven edit modals.** The directive expands to `x-data='{ form: {...} }'` ON the form element (see `markup-forms.md` §"What `ka:data` Does Automatically"). That creates a fresh Alpine scope where `form` is initialised from the server-side `_.initialForm` snapshot. Inputs inside the form (`<input x-model="form.email">`) bind to *that inner* `form`, not any `form` in an enclosing modal/page scope.

    **Two patterns work; mixing them silently fails.**

    | Pattern | Source of truth | Use `ka:data`? |
    |---|---|---|
    | **User-typed form (Add modal)** — empty initial state, user types into inputs, post serializes inner `form` | inner Alpine scope (populated by user) | YES — `ka:data="form:initialForm"` is exactly the recipe |
    | **Payload-driven form (Edit modal)** — outer modal scope holds the row's data, modal `openIt(payload)` populates `form` from outside | outer Alpine scope (populated by JS) | NO — drop `ka:data`, use a single hidden input that reactively serializes |

    **Failure mode (verified via karate-agent):** outer scope holds `{ email: 'alice@acme.com', role: 'admin', products: [...] }`. Inputs render empty. Hitting submit posts an empty `form`. Reason: inner scope has its own empty `{ email: '', role: 'user', products: [] }` initialised at form-element render time; outer scope is unreachable from inputs that walk *up* and stop at the first matching `form` binding (the inner one).

    **Fix for the payload-driven case** — drop `ka:data` and use the **`ka:data-mirror` directive (karate-core 2.0.9.RC1+)**. It expands at template-render time to a hidden input + reactive Alpine bind:

    ```html
    <!-- WRONG — nested empty `form` scope shadows the modal's hydrated `form` -->
    <div x-data="{ form:{}, openIt(p){ this.form = p } }" @open-edit.window="openIt($event.detail)">
      <form ka:data="form:form" ka:post="this">
        <input x-model="form.email"/>           <!-- binds to INNER empty form, stays blank -->
      </form>
    </div>

    <!-- RIGHT — single directive; inputs bind to the modal's outer `form`;
         processor emits the hidden mirror input + the reactive bind. -->
    <div x-data="{ form:{}, openIt(p){ this.form = p } }" @open-edit.window="openIt($event.detail)">
      <form ka:post="this">
        <input ka:data-mirror="form"/>          <!-- ← outer-scope mirror, the magic -->
        <input x-model="form.email"/>           <!-- binds to OUTER hydrated form -->
      </form>
    </div>
    ```

    `ka:data-mirror="<expr>"` rewrites the host element to `<input type="hidden" name="<expr>" :value="JSON.stringify(<expr>)"/>`. Sub-paths work too (`ka:data-mirror="form.contact"`). Server reads `request.paramJson('form')` exactly as it would for `ka:data` — wire-compatible. Pre-directive hand-typed equivalent (still works on older karate-core): `<input type="hidden" name="form" :value="JSON.stringify(form)"/>`.

    **Mental model:** `ka:data` ships server state INTO Alpine; `ka:data-mirror` ships Alpine state BACK to the server. Use `ka:data` for user-typed forms (Add modal — empty initial state); use `ka:data-mirror` for payload-driven forms (Edit modal — outer scope hydrates `form` from a row click).

    **When to skip both `ka:data` and `ka:data-mirror`** — if the modal would benefit from server-side conditional logic (stale-row guards, server-state-driven select options) or you want each modal to be its own bookmarkable URL, use the **Modal-as-URL pattern** instead. The modal endpoint reads state fresh from `request.param('userId')`, so neither `ka:data` nor `ka:data-mirror` is needed — the URL is the state channel.

    **Why does the user-typed Add pattern work with `ka:data` despite the same nested scope?** Because the outer scope's `form` is empty too, so the visual result matches the inner empty scope. User typing populates the inner scope; submit serializes the inner scope. There's no asymmetry to expose. The asymmetry only bites when you populate `form` from outside (a row-click payload, an `x-init` flash restore that targets the wrong scope, etc.).

    **Detection:** symptoms of "outer state correct, inputs blank" → walk the input's `_x_dataStack` in DevTools. If the closest stack frame has `form` separate from the parent's `form`, you've nested-scope-shadowed.

21. **<a id="th-fragment-no-params"></a>Param lists in `th:fragment` signatures are NOT supported — use plain `th:fragment="name"` + `th:with` at the call site.** karate-markup deliberately doesn't support the `th:fragment="chip(label, count)"` declared-signature shape. Use the convention below instead — combined with unset-as-null evaluation, it gives the same ergonomics with simpler engine semantics:

    ```html
    <!--/* WRONG — declared signature; karate-core throws a friendly error */-->
    <a th:fragment="chip(label, count)">...</a>

    <!--/* RIGHT — no signature; caller passes via th:with, fragment reads names directly */-->
    <a th:fragment="chip" href="#"
       th:attr="'ka:get': baseUrl,
                'hx-target': target == null ? '#main-content' : target"
       th:classappend="accent == 'amber' ? '<amber-active>' : '<default-active>'">
      <span th:text="label">Label</span>
      <span th:if="count != null" th:text="count">0</span>
    </a>
    ```

    The fragment reads `label`, `count`, `target`, `accent` directly. Optional ones default to `null` (unset names resolve to null — no typeof guard). Callers pass values via `th:with`:

    ```html
    <th:block th:insert="~{components/chip :: chip}" th:with="
      label: 'Active', count: counts.active, baseUrl: '/team/users'"></th:block>
    ```

    If you do accidentally write `th:fragment="chip(...)"`, karate-core 2.0.8.RC1+ throws a karate-flavoured error pointing back at this rule rather than the underlying Thymeleaf strict-matching message.

22. **<a id="th-with-unset-resolves-null"></a>An unset name in a template expression resolves to `null`** — no `typeof` guard needed for optional fragment params, but a typo (`th:text="userNme"`) renders empty instead of throwing.

23. **<a id="html-comments-ship"></a>Plain HTML comments `<!-- ... -->` ship to the browser — use Thymeleaf parser-level comments `<!--/* ... */-->` for template-only docs.** Plain HTML comments are deliberately preserved by Thymeleaf (some are semantic — historical IE conditionals, attribution markers, etc.). The bite: tests asserting on `body(resp).contains("...")` will match strings present *only inside* an HTML comment, so a docstring like `<!-- expects id="foo" -->` can fail an unrelated test elsewhere that scans for the literal `id="foo"`.

    **Use the right comment shape for the job:**

    | Syntax | Behavior | Use for |
    |---|---|---|
    | `<!-- ... -->` | Passes through to rendered HTML | Comments you intentionally ship to the browser (rare) |
    | `<!--/* ... */-->` | **Stripped at parse time — never rendered** | Template-only documentation, doc-strings, structural notes |
    | `<!--/*/ ... /*/-->` | Markers stripped, **inner content surfaces during render** | Static-prototype stub data — content is hidden in a static-HTML preview but processed normally by the engine |

    The middle row is the answer for nearly every template comment you'd want to write. Default to `<!--/* ... */-->` for any docstring; keep plain `<!-- ... -->` only when you have a specific reason for the comment to reach the browser. Verified in karate-core via `MarkupTest#testCommentHandling`.

24. **<a id="session-no-store-opaque"></a>With no `ServerConfig.sessionStore(...)`, any `session.*` read throws `session is unavailable`** (the proxy is truthy, so `if (session)` still enters). Configure a store and call `context.init()` before touching `session`; otherwise keep per-process state in a `SimpleObject` global from Java, and per-request memo in `request.cache(key, fn)`.

25. **<a id="localstorage-poison-undefined"></a>`JSON.parse(localStorage.getItem(key) || '[]')` blows up when the stored value is the literal string `"undefined"`.** The `||` fallback only catches `null` (key missing). If a previous version of the app, an interrupted save, or a browser extension wrote the string `"undefined"` (or `"null"`) into the slot, `JSON.parse("undefined")` throws `SyntaxError: "undefined" is not valid JSON`. Inside an Alpine factory's constructor this is catastrophic: the throw aborts the `x-data` initialiser, so every property the template references (`tabs.length`, `activeTab`, `pickerOpen`, …) cascades into "is not defined" errors and the page renders as broken markup with no obvious cause.

    **Always parse defensively** — treat `"undefined"`, `"null"`, parse failure, AND wrong-type results as the empty default:

    ```js
    function safeParseArray(key) {
      var raw = localStorage.getItem(key);
      if (!raw || raw === 'undefined' || raw === 'null') return [];
      try { var v = JSON.parse(raw); return Array.isArray(v) ? v : []; }
      catch (e) { return []; }
    }
    function safeParseObject(key, dflt) {
      var raw = localStorage.getItem(key);
      if (!raw || raw === 'undefined' || raw === 'null') return dflt;
      try { var v = JSON.parse(raw); return (v && typeof v === 'object' && !Array.isArray(v)) ? v : dflt; }
      catch (e) { return dflt; }
    }
    ```

    Used in `app/tabs.html` and `app/data-table.html`. Mirror this any time an Alpine factory hydrates from `localStorage` — same pattern for `sessionStorage`. Treat it as the canonical parse helper for client-side persistence. `setItem(key, undefined)` stores the string `"undefined"`, so the poison value is easy to write.

26. **<a id="shell-derives-crumbs"></a>Breadcrumbs are derived in the shell; a page overrides them with `context.setGlobal('crumbs', [...])`.** `_` never crosses content→shell, so `_.crumbs` is ignored; the shell reads `context.getGlobal('crumbs')` first, then derives from `request.path` against `categories`. Same for other content-specific shell metadata (`pageTitle`).

27. **<a id="java-methods-not-on-js-strings"></a>Java String methods don't work inside `th:` / `ka:` expressions.** Server-side template expressions evaluate in the Karate JS engine — not Java/SpEL/OGNL — so a Java `String` returned from a `SimpleObject` or a `Map.get(...)` arrives as a JS string with only the ES `String.prototype` surface. Calling `.isEmpty()`, `.equalsIgnoreCase(...)`, `.startsWith(prefix, offset)`, or any other Java-only String method blows up at render time. **Real-world failure mode:**
    ```html
    <!-- WRONG — TypeError: r . env . isEmpty is not a function -->
    <td th:text="r.env != null && !r.env.isEmpty() ? r.env : '—'"></td>
    ```
    ```
    Error: TypeError: r . env . isEmpty is not a function
    ```
    **Fix.** Use ES idioms — truthiness (`s ? s : '—'`), length (`s && s.length > 0`), or explicit equality (`s !== ''`). The first form also handles `null`/`undefined` naturally:
    ```html
    <!-- RIGHT — JS truthiness covers null, undefined, and "" in one shot -->
    <td th:text="r.env ? r.env : '—'"></td>
    ```
    Same trap with other Java-only methods: `equalsIgnoreCase` → `s.toLowerCase() === t.toLowerCase()`, `Collections.isEmpty(list)` → `!list || list.length === 0`, `Optional.orElse(x)` → `value ?? x` (or `value != null ? value : x`). The rule of thumb: if it's not on MDN's `String.prototype` / `Array.prototype` page, it's not going to exist here either. Reach for the Java-side wrapper (a `SimpleObject` exposing a pre-formatted property) when the logic actually wants Java semantics — e.g. case-insensitive collation on a non-ASCII alphabet — rather than fighting the boundary in the template.

28. **<a id="th-with-colon-syntax"></a>`th:with` uses `key: value`, not `key=value`.** karate-markup's `th:with` evaluates as an object literal — same parser as `ka:vals`, `ka:dispatch`, `th:attr`. Use **colons** to separate keys from values, and commas between entries. Classic Thymeleaf's `key=value` form will look like it parses but won't bind the names.
    ```html
    <!-- WRONG — parses as expression, bindings never resolve -->
    <li th:insert="~{components/x::card}" th:with="title=heading, body=lede"></li>

    <!-- RIGHT — object-literal shape -->
    <li th:insert="~{components/x::card}" th:with="title: heading, body: lede"></li>
    ```
    Same trap with multi-value `ka:dispatch` payloads, `hx-vals` server-side values, and any other directive that expects "key/value bag." The mnemonic: if you'd type curly braces around it in JS, the keys take colons.

29. **<a id="fragment-inline-page-walk-fails"></a>Inline `th:fragment` in the same file as the page that uses it fails the page-walk evaluation.** Defining a fragment in the same file as its caller seems convenient, but karate-markup processes the page top-to-bottom. When the engine reaches the fragment body, **the caller's `th:with` bindings haven't been applied** — references to fragment-scope variables hit `ReferenceError`. The canonical home for a fragment is a sibling file under `<surface>/components/` (matches the playground convention).
    ```html
    <!-- WRONG — error: 'node' is not defined when the engine walks past the fragment -->
    <div ...>
      <ul>
        <li th:each="root : tree" th:insert="~{::treeNode}" th:with="node: root"></li>
      </ul>
    </div>
    <li th:fragment="treeNode">
      <span th:text="node.name"></span>      <!-- fails: page-walk hits this with node unset -->
    </li>

    <!-- RIGHT — fragment in its own file, page references it by path -->
    <!-- page.html: -->
    <li th:each="root : tree"
        th:insert="~{runs/components/tree-node::treeNode}"
        th:with="node: root, depth: 0"></li>

    <!-- runs/components/tree-node.html: -->
    <li th:fragment="treeNode" ...>
      <span th:text="node.name"></span>      <!-- only renders via th:insert; node is bound -->
    </li>
    ```
    The error message itself is helpful — it surfaces the unbound name and points at the fragment-param fix. The fact that *moving the fragment to its own file* is the practical resolution isn't in the message; this gotcha covers that.
    *Escape hatch.* If you absolutely need an inline fragment, guard every fragment-body reference with `context.get('node')` so the page-walk sees `null` instead of throwing. The friction usually pushes you to the separate-file path anyway.

30. **<a id="request-no-pathparam-method"></a>`request.pathParam(name)` doesn't exist — call `request.pathMatches(...)` first, then read `request.pathParams.<name>`.** Routes registered via `templateRoute("/runs/{id}", "runs/detail.html")` carry their bindings on `request.pathParams` only after the template's `ka:scope` calls `request.pathMatches('/runs/{id}')`. There's no `request.pathParam(...)` accessor; an attempt errors with `TypeError: request.pathParam is not a function`.
    ```html
    <!-- WRONG — TypeError: request.pathParam is not a function -->
    <script ka:scope="global">
      var runId = request.pathParam('id');
    </script>

    <!-- RIGHT — match the route, then read the map -->
    <script ka:scope="global">
      request.pathMatches('/runs/{id}');
      var runId = request.pathParams.id;
    </script>
    ```
    `request.pathMatches(pattern)` populates `request.pathParams` as a JS object keyed by the pattern's named segments. Skipping the match call leaves `pathParams` empty (or stale from a prior match). For multi-route templates (e.g. a single landing page handling both `/users` and `/users/{id}`), chain matches with `if (request.pathMatches('/users/{id}')) { ... } else if (request.pathMatches('/users')) { ... }`. See `Skill.help('markup', 'app-setup')` §Routing for the longer write-up.

## Component-Composition Idioms

Real-world experience extracting components from studio (modal chrome, group-picker, filter-chip, flash-stack, row-action) surfaced a small idiom set worth standardising.

**Fragment + th:with convention** — karate-markup uses plain `th:fragment="name"` (no param list) and passes values via `th:with`. The fragment reads names directly; unset names resolve to `null` — no typeof guards needed.

```html
<!--/* components/filter-chip.html */-->
<a th:fragment="chip" href="#"
   th:attr="'ka:get': baseUrl,
            'hx-target': target == null ? '#main-content' : target"
   th:classappend="accent == 'amber' ? '<amber-active>' : '<default-active>'">
  <span th:text="label">Label</span>
  <span th:if="count != null" th:text="count">0</span>
</a>
```

Document the contract — required vs optional params, types, semantics — in the file's leading docstring (see component file conventions below). The fragment body is the runtime contract; the docstring is the human-readable one. If a caller forgets a required param, the rendered output for that field is empty — not an error, but visible during development.

**Why no declared params?** See Gotcha #21. The declared-signature path in Thymeleaf has strict matching that doesn't compose with optional params; the karate convention sidesteps it cleanly.

**Caller pattern** — every component invocation uses `th:insert` (not `th:replace`) so `th:with` propagates into the fragment scope:

```html
<th:block th:insert="~{components/filter-chip :: chip}" th:with="
  key: 'active', label: 'Active', count: counts.active,
  current: filter, baseUrl: '/team/users'"></th:block>
```

The wrapping `<th:block>` is invisible to the rendered DOM. Caller verbosity stays linear in the param count, which is honest — you're calling a function with arguments.

**Forwarding parent-scope vars to a child** — `th:with` evaluates as a JS object literal, so both ES6 shorthand and spread work:

```html
<!-- Shorthand — pick named vars from the enclosing scope -->
<th:block th:insert="~{components/filter-chip :: chip}" th:with="key, count, baseUrl"></th:block>

<!-- Spread — forward every property of an object as a local var -->
<th:block th:insert="~{components/modal :: shell}" th:with="...modalCtx"></th:block>

<!-- Spread + override — last write wins, same as JS -->
<th:block th:insert="~{components/modal :: shell}" th:with="...modalCtx, size: 'lg'"></th:block>
```

**Component file conventions** — emerged from the studio refactor and are worth following for consistency:

- `components/<name>.html` per component. Multiple `th:fragment` blocks per file are fine when they're tightly related (see `components/modal.html` exporting `backdrop`, `header`, `footer` chrome pieces).
- A leading `<!--/* */-->` docstring at the top of the file describing: purpose, all parameters (required vs optional), an end-to-end usage example, gotchas. Use the parser-level comment shape so the docstring doesn't leak to the browser (see Gotcha #23). Future maintainers (and AI assistants) read this before the code.
- Use plain `th:fragment="name"` (no param list — see Gotcha #21). Callers pass values via `th:with`. The leading file docstring carries the param contract; unset names resolve to `null`.
- For SVG icons, follow the established `components/icons.html` convention (no params; `cls` read from scope; passes through `th:class`).

**Fragments are for atoms, not for body composition.** Reach for `th:fragment`+`th:insert` when the call site has nothing structural to add: icons, layout shell head/foot, narrow widgets like a status pill or filter chip. Don't reach for fragments to compose page or modal bodies — that's the job of server-rendered partials (Modal-as-URL, htmx-fetched chunks). karate-markup deliberately doesn't ship named slots or fragment-expression body composition; the cost is some boilerplate when chrome repeats across a few endpoints, the gain is each template reading top-to-bottom with no special composition syntax. See the **Modal-as-URL** section below and the **Server-Render Composition** principle.

## Principles

1. **Server is source of truth.** Alpine `x-data` is ephemeral — re-initialized from server state on every HTMX swap. Don't persist client state; round-trip it.

2. **Fragments are components.** `th:insert` with `th:with` parameters. No JS component framework needed.

3. **Three lanes.** HTMX = server round-trips. Alpine = client-side reactivity. Vanilla JS = complex widget islands (CodeMirror, video, canvas).

4. **`ka:scope` is the controller.** Server-side JS handles data prep, POST logic, redirects, session state. Replaces backend controllers and frontend data fetching.

5. **`ka:vals` for discrete values, `ka:data` for complex state.** Both auto-handle JSON quote escaping via single-quoted attributes.

6. **Server-Render Composition.** Compose page and modal bodies by rendering server-side partials (URL endpoints, htmx-fetched chunks), not by composing template fragments at include time. `th:fragment`+`th:insert` is for atomic stable pieces (icons, shell head/foot, narrow widgets). Conditional body content uses `th:if` / `th:switch` / `th:each` directly. No named slots; no fragment-expression body composition. See §Modal-as-URL for the canonical pattern.

## JS Island Patterns

**Simple widget (Alpine wrapper):**
```html
<div x-data="{ open: false }">
  <button @click="open = !open">Toggle</button>
  <div x-show="open" x-transition>Content</div>
</div>
```

**Complex widget (vanilla JS + custom events):**
```html
<div id="editor-container"></div>
<script>
  // Initialize CodeMirror, video player, etc.
  var editor = initEditor(document.getElementById('editor-container'));
  editor.on('save', function(content) {
    // Signal HTMX to submit
    htmx.trigger('#editor-container', 'editor-save', { content: content });
  });
</script>
```

Use Alpine when state is simple and benefits from reactivity. Use vanilla JS when the widget has its own lifecycle and internal state management.

## Cross-Page Data Passing

Pass structured data between pages using a POST form + `request.paramValues()` on the target. Better than query strings for arrays or when you don't want params in the URL.

**Source page (vanilla JS):**
```html
<button @click="navigateWithData()">Go</button>
<script>
  function navigateWithData() {
    var form = document.createElement('form');
    form.method = 'POST';
    form.action = '/target-page';
    ['item1', 'item2', 'item3'].forEach(function(val) {
      var input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'items';
      input.value = val;
      form.appendChild(input);
    });
    document.body.appendChild(form);
    form.submit();
  }
</script>
```

**Target page (`ka:scope`):**
```html
<script ka:scope="global">
  var items = request.paramValues('items');  // List<String> or null
  _.items = items || [];
  _.hasItems = _.items.length > 0;
</script>
<div th:if="hasItems">
  <!-- Use items to seed Alpine state, pre-fill forms, auto-open modals, etc. -->
</div>
```

**Key methods:** `request.param(name)` returns first value (String). `request.paramValues(name)` returns all values (List). `request.paramJson(name)` parses a single param as JSON (for `ka:data` round-trips). `request.paramInt(name)` returns int with default 0.

## POST Action Dispatch

A page handles multiple POST actions by registering handlers on `context.actions`. The engine routes `request.param('action')` → `context.actions[name](form)` automatically and wraps each call in try/catch (uncaught throws land in `context.flash.error`).

**Page handler (recommended):**
```html
<script ka:scope="global">
  context.actions.delete = function () {
    db.delete(request.param('id'));
    context.flash.success = 'Deleted';
    context.redirect('/items');
  };

  context.actions.save = function (form) {
    // form is auto-decoded from request.paramJson('form')
    db.upsert(form);
    context.flash.success = 'Saved.';
  };

  context.actions.prefill = function () {
    // Cross-page: another page POSTed here to seed state
    _.prefillData = request.paramValues('items') || [];
  };
</script>
```

The handler signature is `(form)` where `form = request.paramJson('form')`, auto-decoded by the engine (`null` if absent). For non-form actions, declare no parameter and read `request.param(...)` via closure.

**Dot vs bracket notation:** `context.actions` is a plain Map, so both `context.actions.foo = fn` (dot) and `context.actions['foo'] = fn` (bracket) work. **Prefer dot notation** for normal action names — it reads more cleanly and matches how `context.flash.success` is written. Use bracket only when the action name isn't a valid JS identifier (hyphens, numeric prefixes, etc.).

**Why use the registry:** zero boilerplate. No `if (request.post)`, no `switch`/`case`/`break`, no per-handler `try/catch`. Adding a new action is one named entry. The engine guarantees at-most-once dispatch per request even if multiple `ka:scope="global"` blocks register actions.

**Dispatch timing — eager-on-register:** the matching handler fires the moment its registration assignment runs (`context.actions.editUser = function(...) { ... }`), not at end-of-script. This lets state reads later in the same script observe post-mutation data:

```js
context.actions.editUser = function (form) {
  utils.updateTeamUser(form);  // ← fires NOW if action=editUser
};

// any reads below this point see post-mutation DB state
_.team = utils.getTeamWithUsers(teamId, 'email');  // fresh
```

Practical convention: **register handlers near the top of the script, do state reads after**. The original switch pattern had this ordering naturally; the registry preserves it.

**Custom error handling:** when a handler needs more than the default `flash.error = e.message` (e.g. preserving form data on error so a modal re-renders populated, or rewriting the error message), keep an explicit `try/catch`:

```js
context.actions.addUser = function (form) {
  try {
    var result = utils.addUserToTeam(form);
    if (result.quotaError) { _.quotaError = ...; _.form = form; }
    else { context.flash.success = 'Added ' + form.email; }
  } catch (e) {
    context.flash.error = e.message || e;
    _.form = form;  // preserve form so the modal stays populated
  }
};
```

**Legacy switch pattern (still works):** pages with `if (request.post) { switch (action) { case 'X': ... } }` continue to function. The dispatch hook is opt-in — pages without an `actions` block skip it. Mix-and-match is allowed but rarely useful; prefer one style per page.

**Same-page trigger (HTMX, no form):**
```html
<button ka:post="items" ka:vals="action:'delete',id:item.id" hx-target="#main" hx-confirm="Delete?">Delete</button>
```

**Same-page trigger (inside form — use hidden input, not `ka:vals`):**
```html
<form ka:post="this" hx-target="#main">
  <input type="hidden" name="action" value="save"/>
  <input name="email" type="email"/>
  <button type="submit">Save</button>
</form>
```

**Cross-page trigger (vanilla JS form POST):**
```js
var form = document.createElement('form');
form.method = 'POST';
form.action = '/target-page';
form.innerHTML = '<input type="hidden" name="action" value="prefill"/>';
items.forEach(function(v) {
  form.innerHTML += '<input type="hidden" name="items" value="' + v + '"/>';
});
document.body.appendChild(form);
form.submit();
```

## JSON-island hydration (`ka:island`)

```html
<script ka:scope="local">_.users = utils.listUsers();</script>
<div ka:island="users"/>
```

Replaces the host element with `<script type="application/json" id="users-data">{json}</script>`. Client reads via `JSON.parse(document.getElementById('users-data').textContent)`.

- Default id: `<expr>-data`. Override: `ka:island="users:custom-id"`.
- Null values render as `null` (valid JSON). Pre-set `_.foo = _.foo || []` for an empty-array default.
- The expression part must not contain `:` — pre-compute on `_` for ternaries.

## Modal-as-URL

Each modal body is its own URL endpoint. Row button uses htmx to fetch it; response is bare modal HTML that swaps into a target slot. Dismissal removes the modal from the DOM. URL carries state — no `ka:data-mirror`, no Alpine outer-scope plumbing.

```html
<!-- 1. Row button -->
<button th:hx-get="'/users-edit?userId=' + u.userId"
        hx-target="#modal-slot" hx-swap="innerHTML">Edit</button>

<!-- 2. Empty target on the page -->
<div id="modal-slot"></div>
```

```html
<!-- 3. Modal endpoint (users-edit.html) -->
<script ka:scope="local">
  _.user = utils.findUser(request.param('userId'));
</script>
<div th:if="user != null"
     x-data="{ open: true }" x-show="open" x-cloak
     class="fixed inset-0 z-50 ..."
     @click.self="open=false" @keydown.escape.window="open=false">
  <form hx-post="/users" hx-target="#main-content" hx-swap="innerHTML">
    <input type="hidden" name="action" value="editUser"/>
    <input type="hidden" name="userId" th:value="user?.userId"/>
    <input name="email" th:value="user?.email" required/>
    <button type="submit">Save</button>
  </form>
</div>
```

**Server config.**
- `HX-Request` header bypasses the shell automatically (every htmx fetch).
- `ServerConfig.rawPaths("/users-edit", ...)` for direct-URL hits (no `HX-Request`).

**Form submit.** Modal's form `hx-post`s to the list page targeting `#main-content innerHTML`. Server mutates state, re-renders the list (shell skipped, `HX-Request` set), htmx swaps. Modal vanishes with the DOM replacement. Validation errors return updated modal targeting `#modal-slot`.

**Server-side conditionals work freely** since the endpoint reads state fresh on every open:
```html
<script ka:scope="local">
  _.user  = utils.findUser(request.param('userId'));
  _.guard = user != null && user.status != 'pending';
</script>
<div th:if="guard">User no longer pending — refresh.</div>
<form th:if="!guard" ...>...</form>
```

**When NOT to use:**
- Rapid-fire / latency-sensitive UI (instant confirms, hovering popovers) — round-trip is felt.
- Modal body trivially derived from row data on the client → use `ka:data-mirror` (see Gotcha #20 / [`#data-mirror-typing`](#data-mirror-typing)).

**Chrome.** Each modal endpoint duplicates ~15 lines of backdrop/header/footer. Intentional per Principle #6 (Server-Render Composition).

**Reference implementation.** `markup/playground/tailwind/src/main/resources/app/modal-as-url*.html` and the matching `App.MockStore` SimpleObject for shared mock state across requests. Live at `/modal-as-url` in the playground.

## Live SSE+REST Reconciliation (`$live`)

HTMX `sse` and hand-rolled EventSource patterns: `Skill.help('markup', 'sse')`.

`$live` is an Alpine.js magic helper that bundles the SSE-with-REST-history reconciliation pattern into one declarative call. Replaces the hand-rolled "open SSE, fetch history, buffer-during-fetch, dedup the seam, flush, then live" plumbing with one line at the call site.

```html
<!-- Minimal — subscribe to SSE, append every event to a reactive array -->
<div x-data="{ events: $live('/api/events') }">
  <template x-for="ev in events" :key="ev.id"><div x-text="ev.message"/></template>
</div>

<!-- Full reconciliation — REST history backfill + dedup across the seam -->
<div x-data="{ events: $live('/api/sessions/123/sse', {
                  initial: '/api/sessions/123/events',
                  dedup:   'type+timestamp+iteration'
                }) }">
  <template x-for="ev in events" :key="ev.id">…</template>
</div>
```

**API.** `$live(url, options?)`. Returns an Alpine reactive array — push triggers re-render, length-watch is reactive, snapshot with `[...arr]`.

| Option | Type | Default | Effect |
|---|---|---|---|
| `initial` | `string` (URL) | none | REST endpoint fetched on mount; SSE events arriving during the fetch are buffered and flushed (deduped) after history loads |
| `dedup`   | `string` of fields joined with `+` (e.g. `'id'`, `'type+timestamp'`) **or** function `(ev) => key` | none | drops events whose computed key was seen before (within the lifetime of this `$live` call); applies to both REST history and SSE |
| `event`   | `string` | `'message'` | SSE named-event channel; matches `connection.send(event, data)` on the server |

**The reconciliation contract.**
1. SSE opens **first** (synchronous `new EventSource`). Events that arrive between mount and history-load are captured in an internal buffer, never dropped.
2. If `initial` is set, REST history is fetched in parallel. When it resolves: append history rows (recording dedup keys), then flush the buffered SSE events (deduped against history), then the array switches to live-streaming mode.
3. If `initial` is absent, no buffering — every SSE event flows straight into the array.
4. The history endpoint may return a bare array, `{ items: [...] }`, `{ messages: [...] }`, `{ events: [...] }`, or any object with a single array-valued property; the helper unwraps the common shapes.

**Lifecycle / cleanup.** `EventSource` auto-reconnects on transient drops natively — `$live` doesn't override. On permanent close (`readyState === EventSource.CLOSED`) it surfaces via the page's `showHtmxError` toast (or `console.error` if no toast host is wired). The helper registers two global listeners that close any open SSE connections it owns:
- `htmx:beforeSwap` — closes EventSources rooted inside the swap target before the DOM is replaced.
- `beforeunload` — closes everything on full-page nav, refresh, or tab close.

No per-component teardown needed.

**Out of scope (v1, by design).**
- **Scroll-pin / "at bottom" tracking** — UI concern, not data. Use `x-effect` on `events.length` + `$nextTick` + `$refs.scroller.scrollTop = scrollHeight`. The playground's `/live` page is the reference.
- **`Last-Event-ID` resume on reconnect** — needs server cooperation; the browser's native auto-reconnect just re-subscribes and you rely on dedup + history to reconcile.
- **WebSockets** — different protocol, separate helper if/when needed.

**Distribution.** Today `$live` ships as a snippet in the karate-markup-playground's `pub/app.js`. Copy that snippet into your own asset pipeline. A future option — serving it from karate-core's classpath as a built-in `pub/karate.js` so consumers don't need to copy-paste — is deferred until the snippet is proven across more than one consumer.

**Reference implementation.** `markup/playground/tailwind/src/main/resources/app/pub/app.js` (the magic) + `markup/playground/tailwind/src/main/resources/app/live.html` (the chat-room demo) + `App.ChatStore` SimpleObject (broadcast-to-subscribers pubsub primitive). Live at `/live` in the playground.

## See also

- `Skill.help('markup', 'catalog')` — canonical pattern catalog (LLM-first reference). Each entry: problem, when-to-reach-for, source files, minimal snippet.
- `Skill.help('markup', 'app-setup')` — Java-side setup, routing, sessions, auth guards, public vs protected pages.
- `Skill.help('markup', 'tailwind')` — Tailwind tokens, dark mode, layout shell, recipes.
- Playground source under `markup/playground/tailwind/src/main/resources/app/` — every pattern's live, working implementation.

## Standing Directives

**Goal: karate-markup as a first-class UI framework.** The combination of karate-core's template engine + Alpine + HTMX + your CSS framework should let you build enterprise UIs that match what teams ship with React, Vue, Svelte, or HTMX-on-Django/Rails. We are not aiming for "good enough for internal tools" — we are aiming for parity with the popular front-end app frameworks on developer experience, idiom clarity, and pattern coverage. Every UI session is an opportunity to close a gap.

**Improve karate-core / karate-js / the markup engine.** We own them. When implementing any UI task, if the framework is missing a capability, has a rough edge, or could be more elegant — improve it in the framework, not in application code. Examples of past upgrades that came out of this discipline: `context.switch()`, `FlowControlSignal` short-circuit, `ka:data` quote-escape fix, `parseInt(value, radix)` support in KJS, Java-RuntimeException-as-JS-catchable, directory-index fallback for `/foo` → `foo/index.html`, layout-shell rendering order. Each one removed an awkward workaround from app code. **Always add tests for new framework capabilities.** If a karate-core change ships unguarded, the next session that depends on the same behavior may regress it silently.

**Improve this skill doc + the CSS_*.md siblings + the topic files.** When you reference any of these docs and find content that is inaccurate, incomplete, unclear, or missing a pattern you just used successfully — update the `.md` file directly. These are living references, not frozen specs. Flag what you changed so the commit message reflects the improvement. **Improvements compound:** every project that picks up this skill benefits from each session's lessons.

**Cross-cutting checklist when finishing a UI session:**
1. Was there a workaround in app code that should have been a framework change? → file an issue or fix it.
2. Was there a pattern you re-derived from scratch that should have been in this doc? → write it down.
3. Was there a CSS recipe (modal, dropdown, drawer, palette, …) you composed by hand that belongs in `CSS_BOOTSTRAP.md` or `Skill.help('markup', 'tailwind')`? → add it.
4. Was there a gotcha you hit that future-you would benefit from knowing? → add it to the Gotchas section.

The cost of writing it down once is minutes. The cost of every future session re-deriving it is hours, multiplied across every project that uses this skill.
