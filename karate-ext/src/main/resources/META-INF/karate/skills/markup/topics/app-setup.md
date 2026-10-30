# karate-markup — App Setup, Routing, Sessions

> One-time Java-side setup for a karate-markup app. After this, day-to-day work happens in HTML templates and `ka:scope` blocks. This is the deep-dive reference for **wiring the server** — routing, global variables, session stores, auth.
> **See also:** `Skill.help('markup')` (templating + gotchas + principles) · `Skill.help('markup', 'catalog')` (pattern catalog) · `Skill.help('markup', 'tailwind')` (the CSS path).
>
> **The playground app (`Http.app`) is wired already** — relabel it with `app/ui.js` or shadow a page in `app/`: `Skill.help('app-authoring')`.
>
> **Reference wiring lives in this repo:** karate-max's own console is a karate-markup app — `MaxServer.buildMarkupHandler()` (`io.karatelabs.ext.max.MaxServer`) is the real, in-production `ServerConfig` build, and is the canonical answer to "how do I wire sessions / routing / `/api` vs `/ui`?" See [§Reference wiring](#reference-wiring--the-karate-max-console) below.

---

## Table of contents

- [Setup](#setup)
  - [Minimal app](#minimal-app)
  - [Automatic URL routing](#automatic-url-routing)
  - [Directory structure](#directory-structure)
  - [Dev mode (hot reload)](#dev-mode-hot-reload)
  - [Global variables (`SimpleObject`)](#global-variables-simpleobject)
  - [`ServerConfig` options](#serverconfig-options)
  - [API endpoints (JS handlers)](#api-endpoints-js-handlers)
  - [Request filter (centralized middleware)](#request-filter-centralized-middleware)
  - [Authentication & protected pages](#authentication--protected-pages)
  - [Reference wiring — the karate-max console](#reference-wiring--the-karate-max-console)
  - [Complete examples](#complete-examples)
- [Routing](#routing)
- [Sessions](#sessions)
  - [Where per-browser UI state belongs (the rule)](#where-per-browser-ui-state-belongs-the-rule)

---

# Setup

## Minimal app

```java
import io.karatelabs.common.Resource;
import io.karatelabs.http.HttpServer;
import io.karatelabs.http.ServerConfig;
import io.karatelabs.http.ServerRequestHandler;
import io.karatelabs.markup.ResourceResolver;

String resourceRoot = "classpath:app";
ResourceResolver resolver = (path, caller) -> Resource.path(resourceRoot + "/" + path);

ServerConfig config = new ServerConfig(resourceRoot);
ServerRequestHandler handler = new ServerRequestHandler(config, resolver);
HttpServer server = HttpServer.start(8080, handler);
```

That's it. Put an `index.html` in `src/main/resources/app/` and visit `http://localhost:8080/`.

## Automatic URL routing

`ServerRequestHandler` maps URLs to HTML templates automatically:

| URL | Template |
|-----|----------|
| `/` | `index.html` |
| `/login` | `login.html` |
| `/admin/users` | `admin/users.html` |
| `/sessions/abc` | `sessions/abc.html` (or handle with `request.pathMatches()` — see [Routing](#routing)) |

No route table, no annotations, no configuration. Add a `.html` file, and the URL works.

## Directory structure

```
app/
  index.html              <- / (root page)
  login.html              <- /login
  dashboard.html          <- /dashboard
  components/
    badge.html            <- reusable fragments (not routable as pages)
    modal.html
  pub/
    app.css               <- /pub/app.css (static file)
    logo.png              <- /pub/logo.png
  api/
    todos.js              <- /api/todos (JS API handler)
```

**Pages** at root and in subdirectories — each is a routable URL.
**`components/`** holds reusable fragments referenced via `th:insert` / `th:replace` — not served as pages.
**`pub/`** holds static files (CSS, images, fonts) — served as-is, no template processing. Referenced in HTML as `/pub/app.css`.
**`api/`** holds server-side JS handlers for JSON endpoints (see [API endpoints](#api-endpoints-js-handlers)).

## Dev mode (hot reload)

In dev mode, templates are loaded from the filesystem instead of the classpath, so changes are picked up immediately without restarting:

```java
boolean devMode = true;
String resourceRoot = devMode
        ? "src/main/resources/app"    // filesystem — hot reload
        : "classpath:app";            // classpath — cached in production

ServerConfig config = new ServerConfig(resourceRoot)
        .devMode(devMode);
```

The filesystem path is relative to the working directory. For a Maven project run from the project root, `src/main/resources/app` is the standard location.

## Global variables (`SimpleObject`)

Make Java objects available in all templates by passing them as `globalVariables`. They become top-level variables in template expressions and `ka:scope` blocks.

**Define a utils class:**
```java
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;

public class MyUtils implements SimpleObject {

    @Override
    public Object jsGet(String key) {
        return switch (key) {
            case "getItems" -> (JavaCallable) (ctx, args) -> fetchItems();
            case "saveItem" -> (JavaCallable) (ctx, args) -> saveItem(args[0]);
            case "appName" -> "My App";
            default -> null;
        };
    }

    private List<Map<String, Object>> fetchItems() { ... }
    private Object saveItem(Object item) { ... }
}
```

> <a id="simpleobject-jskeys"></a>**`jsKeys()` for serialization.** When an object is also serialized whole (over MCP / `toMap`), override `jsKeys()` to return the data-only keys so the snapshot omits verb members (the verbs stay reachable via `jsGet`). In karate-max this is the `JsDispatch` substrate's job (see the jsapi spec); a hand-rolled `SimpleObject` follows the same contract.

**Register it:**
```java
MyUtils utils = new MyUtils();
ServerConfig config = new ServerConfig(resourceRoot)
        .globalVariables(Map.of("utils", utils, "devMode", devMode));
```

**Use in templates:**
```html
<script ka:scope="global">
  _.items = utils.getItems();
</script>
<h1 th:text="utils.appName">App</h1>
```

`JavaCallable` is `io.karatelabs.js.JavaCallable` — a functional interface with `Object call(Context ctx, Object... args)`. The `ctx` parameter is the JS engine context (rarely needed). Methods that return data should return `Map`, `List`, or primitives — these are natively understood by the JS expression engine. Static properties (like `appName` above) can be returned directly without `JavaCallable`.

**Exceptions are JS-catchable.** Any `RuntimeException` thrown from a `JavaCallable` surfaces as a standard JS `Error` on the script side. Prefer throwing a domain-specific `RuntimeException` with a clear message over returning error-shaped maps — the template or API handler can use ordinary `try/catch`:

```java
// in MyUtils
case "decodeLicenseFile" -> (JavaCallable) (ctx, args) -> {
    return license.decode((String) args[0]);   // throws RuntimeException("signature verification failed")
};
```

```javascript
// in /api/renew.js
try {
  var data = utils.decodeLicenseFile(request.body);
  response.body = JSON.stringify(data);
} catch (e) {
  response.body = 'error:' + e.message;        // "error:signature verification failed"
}
```

Inside `catch`, `e` is an `Error` with `e.message` (original exception message), `e.name === 'Error'`, and `'' + e === 'Error: <message>'`. Extending `RuntimeException` with a specific subclass is fine — the JS side always sees a plain `Error` with the message preserved.

Two exceptions that intentionally bypass JS `catch`: (1) `FlowControlSignal` subclasses, used by `context.redirect()` and `context.switch()` — these are control flow, not errors; (2) any `RuntimeException` outside a `try` block bubbles up with the usual `expression: <code> - <msg>` framing before reaching the 500 handler, so uncaught failures still produce helpful logs.

## `ServerConfig` options

| Method | Default | Purpose |
|--------|---------|---------|
| `devMode(boolean)` | `false` | Filesystem templates + no-cache headers |
| `csrfEnabled(boolean)` | `true` | CSRF token validation on POST/PUT/DELETE |
| `securityHeadersEnabled(boolean)` | `true` | CSP, X-Frame-Options, Referrer-Policy |
| `sessionStore(SessionStore)` | none | Enable sessions (see [Sessions](#sessions)) |
| `sessionExpirySeconds(int)` | `600` | Session TTL |
| `globalVariables(Map)` | none | Objects available in all templates |
| `staticPrefix(String)` | `"/pub/"` | URL prefix for static files |
| `apiPrefix(String)` | `"/api/"` | URL prefix for JS API handlers |
| `contextPath(String)` | `""` | Base path prefix (e.g., `/app` if behind a reverse proxy) |
| `requestFilter(BiFunction)` | none | Per-request filter with session access (see [Request filter](#request-filter-centralized-middleware)) |
| `templateRoute(String, String)` | none | Map URL pattern to template: `.templateRoute("/sessions/{id}", "session.html")` |
| `jsRoute(String, String)` | none | Map URL pattern to a JS handler outside the apiPrefix (core 2.1.3+): `.jsRoute("/team/export", "team/export.js")` — see [§jsRoute](#js-handlers-outside-the-apiprefix--jsroute) |
| `shellTemplate(String)` | none | Wrap full-page navigations in a layout shell (see `Skill.help('markup')` §Layout shell) |
| `rawPaths(String...)` | none | Paths that ship their own `<html>` and must NOT be shell-wrapped |
| `errorTemplate404(String)` | none | Custom 404 page template name |
| `errorTemplate500(String)` | none | Custom 500 page template name |

Disable CSRF and security headers for internal tools or benchmark apps that don't have user sessions:

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .csrfEnabled(false)
        .securityHeadersEnabled(false);
```

## API endpoints (JS handlers)

Files in `api/` are executed as server-side JS (not rendered as HTML). The URL maps to the file:

| URL | Handler File |
|-----|-------------|
| `/api/todos` | `api/todos.js` |
| `/api/todos/abc` | `api/todos.js` (path becomes `/todos/abc`) |
| `/api/users/list` | `api/users.js` (path becomes `/users/list`) |

```javascript
// api/todos.js
if (request.get) {
    response.body = utils.getItems();          // a Map/List body auto-serializes as JSON
} else if (request.post) {
    var item = request.bodyJson;               // parsed object whatever the content-type
    utils.saveItem(item);
    response.body = { success: true };
}
```

The same `request`, `response`, `context`, `session`, and global variables are available in API handlers as in templates.

**Reading the body:** use `request.bodyJson` for JSON payloads — `request.body` is *content-type-converted* (a JSON `Content-Type` arrives as an already-parsed Map/List, so `context.fromJson(request.body)` fails on exactly the well-behaved clients). `bodyJson` coerces in every case — already-parsed → as-is, string → lenient parse, unparsable/blank/binary → `null`. Raw access: `request.bodyString` / `request.bodyBytes`.

**Writing the response:** assigning a Map/List to `response.body` serializes it as JSON and sets `Content-Type: application/json` automatically. Assigning a *string* sets `text/plain` — so don't pre-serialize with `context.toJson(...)` unless you also set the header yourself. Assign the object, not the string. **Order matters when you do set headers yourself:** the body assignment stamps its Content-Type, so set `response.header('Content-Type', ...)` *after* `response.body = ...`, never before — a header set first is silently overwritten (classic symptom: a CSV download arrives as `text/plain`).

**Errors:** an exception thrown by a handler returns a 500 and is **always logged server-side with the failing method + path + stack trace** (`ServerRequestCycle` logger). With `devMode(true)` the exception message is additionally included in the 500 response body; in production the body stays opaque (`Internal Server Error`) so internals don't leak.

API handlers that don't need a user session (webhooks, public endpoints) work automatically — if no `sessionStore` is configured, `session` is `null` in all handlers. If sessions are enabled, API handlers share the same session mechanism as templates.

> **Two API channels — `/api/*` (machine) vs a second prefix for page-data.** karate-max splits these: `/api/*` is the machine-only programmatic surface, and the SSR console runs its page-data verbs under a **separate `apiPrefix("/ui/")`** in a *fresh per-request engine* so pure data reads never queue behind a long-running op on the shared session engine. If your app has both an external API and rich page-data needs, the same split applies. See [§Reference wiring](#reference-wiring--the-karate-max-console).

### JS handlers outside the apiPrefix — jsRoute()

*(core 2.1.3+)* `jsRoute(pathPattern, jsFile)` maps an individual URL pattern to a JS handler file, so a JS endpoint can live at **any** path — not only under the apiPrefix. It mirrors `templateRoute()`: checked in registration order, original request path preserved so the handler can use `request.pathMatches()` to extract parameters.

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .jsRoute("/team/export", "team/export.js")
        .jsRoute("/reports/{id}/data", "reports/data.js");
```

**Request resolution order:** apiPrefix match → `jsRoute` patterns → `templateRoute` patterns → template file → 404.

**When to reach for it — the auth-gate case.** A `requestFilter` typically gates a whole page prefix ("everything under `/team/*` requires the team-admin role"). A download endpoint for those pages wants to live *inside* that prefix — `jsRoute("/team/export", "team/export.js")` — so the existing gate covers it with no second rule. Before jsRoute the endpoint had to park under `/api/` and the filter needed a duplicated gate clause there; that convention is exactly the drift risk jsRoute removes. (This was the motivating production case: studio's team CSV export.)

**The CSV-download shape** (see the playground's `/data-table/export` → `data-table-export.js` for the live exemplar wired to a data table's sort + search state):

```javascript
// team/export.js — routed from /team/export
function esc(v) {                               // per-cell escaping — BOTH halves matter:
    if (v == null) return '';
    var s = String(v);
    // 1. formula-injection guard: a cell starting with = + - @ (or tab/CR)
    //    executes in Excel/Sheets — CSV quoting alone does NOT neutralize it
    if (/^[\s]*[=+\-@\t\r]/.test(s)) s = "'" + s;
    // 2. standard CSV quoting for commas / quotes / newlines
    return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}
var rows = buildCsvRows(esc);                   // however the app assembles them
// body FIRST — a string body assignment stamps Content-Type: text/plain,
// which would overwrite a header set before it
response.body = rows.join('\r\n') + '\r\n';
response.header('Content-Type', 'text/csv');
response.header('Content-Disposition', 'attachment; filename="export.csv"');
```

Any cell that can carry user-entered text (names, group labels, emails) is a
formula-injection vector without guard #1 — treat it as mandatory in every
CSV endpoint, not an option.

`Content-Disposition: attachment` makes the browser download without navigating, so a plain `<a href="/team/export">` works from any page — no HTMX involvement, and no `ka:get` (an HTMX swap would try to inject the CSV into the DOM).

Prefer `jsRoute` for *individual* endpoints inside page prefixes; keep bulk machine APIs under the apiPrefix. A prefix-wide JS rule over a page prefix would shadow its HTML pages — which is why this is a per-path route, not a second apiPrefix.

## Request filter (centralized middleware)

`requestFilter` is a `BiFunction<HttpRequest, ServerMarkupContext, HttpResponse>` that runs **after** the session is loaded but **before** CSRF validation and template/API processing. Return `null` to continue, or an `HttpResponse` to short-circuit.

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .sessionStore(sessionStore)
        .requestFilter((request, context) -> {
            String path = request.getPath();

            // Public API endpoints — skip session persistence
            if (path.startsWith("/api/webhook") || path.startsWith("/api/health")) {
                context.setSession(Session.TEMPORARY);
                return null; // continue processing
            }

            // Protected routes — require authenticated session
            if (path.startsWith("/admin")) {
                if (context.getSession()?.getMember("user") == null) {
                    return HttpResponse.text(403, "Forbidden"); // short-circuit
                }
            }

            return null; // continue for all other routes
        });
```

### What the filter can do

| Action | How | Use Case |
|--------|-----|----------|
| Block a request | Return an `HttpResponse` (401, 403, etc.) | Auth validation, rate limiting |
| Set temporary session | `context.setSession(Session.TEMPORARY)` | Webhooks, public APIs — no cookie, no persistence |
| Inspect session | `context.getSession()?.getMember("user")` | Role-based access control |
| Continue normally | Return `null` | Everything else |

### `Session.TEMPORARY`

When you call `context.setSession(Session.TEMPORARY)`, the request is processed normally but:
- No session cookie is set in the response
- Session data is not persisted to the store
- CSRF validation is skipped (temporary sessions have no token to validate)

This is the pattern for endpoints that receive external calls (webhooks, health checks, public APIs) where a persistent session is meaningless.

### Filter execution order

```
Request → Static file check → Session loaded from cookie → requestFilter → CSRF check → Template/API processing
```

The filter runs with the full context available (session loaded, flash messages ready) so it can make decisions based on user identity.

## Authentication & protected pages

Authentication is enforced at the **template level**, not with middleware or route configuration. Each protected page checks the session at the top of its `ka:scope` block. (Centralize it at the `requestFilter` layer once the guards repeat — see `Skill.help('markup')` §"Centralized Auth with `requestFilter`".)

### Auth guard (protected pages)

Add this at the top of every page that requires login:

```html
<!-- index.html, dashboard.html, etc. -->
<script ka:scope="global">
  if (!session?.user) {
    context.redirect('/signin');
  }
</script>
```

The `?.` (optional chaining) operator safely handles `session` being `null`. The redirect exits template rendering immediately — nothing below it executes.

### Public pages (no auth check)

Pages like `signin.html` simply don't include the auth check. They are accessible to anyone:

```html
<!-- signin.html — public page -->
<script ka:scope="global">
  // Handle signin logic (OAuth callback, form login, etc.)
  if (request.post) {
    var email = request.param('email');
    var user = utils.authenticate(email, request.param('password'));
    if (user) {
      context.init();          // create session
      session.user = user;     // store user
      context.redirect('/');   // send to protected area
    } else {
      _.error = 'Invalid credentials';
    }
  }

  // Already signed in? Redirect away from signin page
  if (session?.user) {
    context.redirect('/');
  }
</script>
```

Key points:
- `context.init()` creates the session and sets the cookie — call it **before** writing to `session`
- The `?.` operator safely handles `session` being `null` — no need for `session && session.user`
- `context.flash` survives one redirect — use it for error/success messages after signin

### Signout

```html
<!-- signout.html -->
<script ka:scope="global">
  if (session) {
    context.close();              // destroy session, clear cookie
  }
  context.redirect('/signin');
</script>
```

### Admin/role protection

For pages that need a specific role, check after the basic auth guard:

```html
<script ka:scope="global">
  if (!session?.user) {
    context.redirect('/signin');
  }
  if (!session.user.admin) {
    response.status = 403;
    _.error = 'Admin access required';
  }
</script>

<div th:if="error" class="<your-danger-alert-class>" th:text="error">Error</div>
<div th:unless="error">
  <!-- admin-only content -->
</div>
```

### Dev-mode auto-signin

For development, add a shortcut that bypasses OAuth:

```html
<!-- In signin.html ka:scope block -->
if (devMode && request.param('dev')) {
  var email = request.param('dev');
  context.init();
  session.user = { email: email, name: email, admin: true };
  context.redirect('/');
}
```

This lets you sign in via URL: `http://localhost:8080/signin?dev=admin@test.com`. Only works when `devMode` is a global variable set to `true`.

### Page type summary

| Page Type | Session Store | Auth Check | Example |
|-----------|--------------|------------|---------|
| Public page | Optional | None | `signin.html`, landing page |
| Protected page | Required | `if (!session?.user) context.redirect('/signin')` | `index.html`, dashboard |
| Admin page | Required | Auth check + role check | `admin.html` |
| API endpoint (authenticated) | Required | Check `session.user` in JS handler | `api/teams.js` |
| API endpoint (public) | Optional | None | `api/webhook.js` |
| Internal tool (no auth) | Not configured | None — no sessionStore means no sessions | Benchmark apps, dev tools |

## Reference wiring — the karate-max console

The karate-max console (the `karate serve` SSR webapp — switchboard, `/runs`, rulebooks, reports) **is** a karate-markup app. Its `ServerConfig` build is the canonical, in-production reference — `io.karatelabs.ext.max.MaxServer#buildMarkupHandler`:

```java
// io.karatelabs.ext.max.MaxServer (abridged — the real build)
ServerConfig cfg = new ServerConfig(WEB_CP_ROOT)            // classpath:io/karatelabs/ext/max/web
        .shellTemplate("layout.html")                       // the shared chrome shell
        .csrfEnabled(false)                                 // local single-user tool; CSP would block the CDN + noVNC iframe
        .securityHeadersEnabled(false)
        .sessionStore(new io.karatelabs.http.InMemorySessionStore())   // per-browser UI state
        .sessionExpirySeconds(8 * 60 * 60)                  // 8h idle — a console working session, not a login
        .apiPrefix("/ui/")                                  // page-data verbs in a FRESH per-request engine
        .templateRoute("/runs/{id}", "run.html")
        .templateRoute("/rulebooks/{name}", "rulebook.html")
        .rawPaths("/benchmark.html")
        .globalVariables(templateGlobals);                  // the JSAPI namespaces — harness.sessions(), Rule.*, …
ResourceResolver resolver = (p, caller) -> Resource.path(WEB_CP_ROOT + "/" + p);
return new ServerRequestHandler(cfg, resolver);
```

Things to copy from this build:

- **`sessionStore(new InMemorySessionStore())`** enables a cookie-keyed `session` object on every page. It's **core's** `io.karatelabs.http.InMemorySessionStore` — a `ConcurrentHashMap`, so it **resets on a server bounce**. That is the *right* mental model for console/UI state (see the rule below). It stays inert until a page calls `context.init()` (`session` is `null` otherwise) — so adding the store changes no existing behavior; you opt a page in by initializing the session.
- **`shellTemplate("layout.html")` + `templateRoute(...)`** — the bookmarkable-URL shell plus parameterized routes (`/sessions/{id}` → `session.html`, read back with `request.pathMatches()`).
- **`apiPrefix("/ui/")`** — moves page-data handlers off the default `/api/*`. In karate-max `/api/*` is reserved as the **machine-only** programmatic surface; `/ui/*` runs in a fresh per-request engine so a pure data read never queues behind a long-running op on the shared engine. Most apps don't need this split, but if yours has both an external API and rich page-data, this is the pattern.
- **CSRF off** is deliberate here (container = single tenant); for a multi-user app, leave the `csrfEnabled(true)` default and wire the CSRF meta + `app.js` header injection (see `Skill.help('markup')` §"HTMX error surface").

> **Name collision, worth flagging:** this `session` is the **HTTP** session (`io.karatelabs.http`, the markup cookie session). It is unrelated to `io.karatelabs.ext.max.jsapi.SessionStore`, which is the Harness *run/bot* session (filesystem-backed). Same word, two layers.

## Complete examples

### Minimal (no auth)

For internal tools, benchmarks, or public sites with no login:

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .devMode(devMode)
        .csrfEnabled(false)
        .globalVariables(Map.of("utils", utils));

ServerRequestHandler handler = new ServerRequestHandler(config, resolver);
HttpServer.start(8080, handler);
```

### With authentication

For apps that need user login and sessions:

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .devMode(devMode)
        .sessionStore(new InMemorySessionStore())  // ConcurrentHashMap; resets on restart
        .sessionExpirySeconds(30 * 60)             // 30 minutes
        .globalVariables(Map.of("utils", utils, "devMode", devMode));

ServerRequestHandler handler = new ServerRequestHandler(config, resolver);
HttpServer.start(8080, handler);
```

Then create at minimum two templates:

**`signin.html`** (public — no auth check):
```html
<script ka:scope="global">
  if (request.post) {
    var user = utils.authenticate(request.param('email'), request.param('password'));
    if (user) {
      context.init();
      session.user = user;
      context.redirect('/');
    } else {
      _.error = 'Invalid credentials';
    }
  }
  if (session?.user) {
    context.redirect('/');
  }
</script>
<!-- signin form here -->
```

**`index.html`** (protected — redirects if not logged in):
```html
<script ka:scope="global">
  if (!session?.user) {
    context.redirect('/signin');
  }
</script>
<body>
  <h1 th:text="'Welcome, ' + session.user.name">Welcome</h1>
</body>
```

No defensive `th:if="session?.user"` is needed on `<body>` — `context.redirect()` short-circuits template rendering, so the body below the guard only runs when `session.user` is non-null. See `Skill.help('markup')` §Flow Control for the full contract.

---

# Routing

> Full page per route. HTMX for in-page interactions only. Deep-linkable URLs.

## Path-based routing

Use `templateRoute()` to map URL patterns with path parameters to templates. When a URL like `/sessions/abc123` doesn't match a template file, the route patterns are checked in order. The original request path is preserved so the template can use `request.pathMatches()` to extract parameters.

```java
ServerConfig config = new ServerConfig(resourceRoot)
        .templateRoute("/sessions/{id}", "session.html")
        .templateRoute("/sessions/{id}/report", "report.html")
        .templateRoute("/flows/{path}", "flows.html");
```

**Resolution order:** apiPrefix match → [`jsRoute` patterns](#js-handlers-outside-the-apiprefix--jsroute) → `templateRoute` patterns (in registration order) → template file → 404.

Register more specific routes first (`/sessions/{id}/report` before `/sessions/{id}`).

In each template, use `request.pathMatches()` to extract path parameters:

```html
<!-- index.html -->
<script ka:scope="global">
  if (request.pathMatches('/sessions/{id}')) {
    _.view = 'session';
    _.sessionId = request.pathParams.id;
  } else if (request.pathMatches('/flows/{path}')) {
    _.view = 'flows';
    _.flowPath = request.pathParams.path;
  } else {
    _.view = 'dashboard';
  }
</script>

<div th:if="view == 'session'" th:insert="session"></div>
<div th:if="view == 'flows'" th:insert="flows"></div>
<div th:unless="view == 'session' || view == 'flows'" th:insert="dashboard"></div>
```

> <a id="request-no-pathparam-method"></a>`request.pathMatches(pattern)` populates `request.pathParams` as a JS object keyed by the pattern's named segments. Skipping the match call leaves `pathParams` empty (or stale from a prior match). For a single template handling several routes, chain matches with `if (request.pathMatches('/users/{id}')) { ... } else if (request.pathMatches('/users')) { ... }`.

## Query-parameter routing (simpler alternative)

For apps without clean URLs, use query params:

```html
<script ka:scope="global">
  _.view = request.param('view') || 'dashboard';
  _.id = request.param('id');
</script>
```

## Session-based routing

`session.route` tracks which page the user is on. This avoids exposing route names in the URL but requires an active session:

```html
<script ka:scope="global">
  if (!session?.user) {
    context.redirect('/signin');
  }
  _.route = session.route || 'dashboard';
</script>

<div th:if="route == 'admin'" th:insert="admin"></div>
<div th:unless="route == 'admin'" th:insert="dashboard"></div>
```

Route changes happen server-side: `session.route = 'admin'` in a `ka:scope` block handling a POST or link click.

## Navigation links

Use `ka:get` for navigating between views within the app. Use plain `<a href>` for full page loads.

```html
<!-- HTMX navigation by template name + ka:vals (params become a query string) -->
<a ka:get="admin-teams" hx-target="#main-content" ka:vals="teamId:team.teamId">
  Edit Team
</a>

<!-- HTMX navigation with a dynamic URL — ${} interpolates server vars inside ka:get -->
<a ka:get="/admin/users?email=${r.email}" hx-target="#main-content" hx-push-url="true">
  View
</a>

<!-- Full page navigation — browser handles it -->
<a th:attr="href: '/sessions/' + session.id">View Session</a>
```

Prefer the `${}` form over `th:attr="'hx-get': ..."` when building URLs from server values — `ka:get` already evaluates JS expressions and gives you the context-path prepend + `"this"` resolution for free.

## SPA-feel with `hx-push-url`

Add `hx-push-url="true"` to update the browser URL bar when HTMX swaps content. This makes the back button work:

```html
<a ka:get="session" hx-target="#main-content" hx-push-url="true"
   ka:vals="id:session.id">
  View Session
</a>
```

The server must handle the pushed URL as a full page load (for direct navigation and browser refresh). This means the `request.pathMatches()` routing in `index.html` must cover every URL that `hx-push-url` produces.

## Pattern summary

| Approach | When to Use | Example |
|----------|-------------|---------|
| `request.pathMatches()` | Clean URLs, public-facing apps | `/sessions/{id}` |
| Query params | Internal tools, simple apps | `/?view=session&id=abc` |
| `session.route` | Apps with auth, where URL doesn't matter | admin panel |
| `hx-push-url` | SPA-feel without full page reload | HTMX nav within app |

---

# Sessions

> Sessions persist user state across requests. Initialize with `context.init()`, access via the `session` object, store in memory (default) or a custom backend.

## Session lifecycle

### Creating a session

Call `context.init()` to create a new session and set the session cookie:

```html
<script ka:scope="global">
  if (request.post) {
    var email = request.param('email');
    var user = db.authenticate(email, request.param('password'));
    if (user) {
      context.init();              // creates session, sets cookie
      session.user = user;         // store user in session
      session.route = 'dashboard'; // set initial route
      context.redirect('/');
    } else {
      _.error = 'Invalid credentials';
    }
  }
</script>
```

### Reading session state

The `session` object is `null` until `context.init()` is called. Always check:

```html
<script ka:scope="global">
  if (!session?.user) {
    context.redirect('/signin');
  }
</script>

<h1 th:text="'Welcome, ' + session.user.name">Welcome</h1>
```

### Updating session state

Assign properties directly — changes are persisted automatically:

```html
<script ka:scope="global">
  if (request.post) {
    session.route = request.param('route');
    session.lastAction = new Date().toISOString();
  }
</script>
```

### Ending a session

Call `context.close()` to destroy the session and clear the cookie:

```html
<script ka:scope="global">
  if (request.param('action') == 'logout') {
    context.close();
    context.redirect('/signin');
  }
</script>
```

## Where per-browser UI state belongs (the rule)

**Per-browser UI state goes in `session.*`, NOT browser `localStorage`.**

When a console/app page needs to remember per-browser state across reloads — a split-pane size, the selected row, a draft-edit cache — put it in the cookie-keyed server `session`, not `localStorage`. The reasons:

- **`session.*` resets on a server bounce; `localStorage` survives one.** A `ConcurrentHashMap`-backed `InMemorySessionStore` is the *right* mental model for working state — restart the server, the UI is clean. `localStorage` outliving restarts caused real confusion (stale state that looked like a bug).
- **It's server-readable.** State in `session.*` is available to `ka:scope` during render, so the page can come back pre-arranged instead of flashing a default and then re-arranging in client JS.
- **It's cookie-scoped per browser**, which is exactly the granularity "this browser's view of the app" wants.

The standing exceptions stay in `localStorage` — state that legitimately *should* survive a server restart, or that applies before any session exists. In the karate-max console these are the **app-wide theme** (dark/light, resolved in the layout shell's pre-paint script — see `tailwind.md` §Dark mode) and the **goal-textarea prompt history** (recent prompts the user wants to keep across bounces). Keep this list short and deliberate — reach for `localStorage` only when cross-restart persistence is the actual intent, not as a default.

To opt a page in: call `context.init()` once (e.g. on first visit or sign-in), then read/write `session.<key>` freely. Until a page initializes the session, `session` is `null` and nothing is stored — so adding `sessionStore(...)` to the config is non-breaking.

**The shipped reference pattern (karate-max console).** The console persists UI prefs through a tiny, reusable two-part substrate worth copying:

- **A lazy write endpoint** under the page-data prefix — `web/ui/session.js` (`GET /ui/session` → `{prefs}`, `POST /ui/session {key,value}`). The GET reads defensively (`(session && session.ui) || {}` — no init on read), and only the POST calls `context.init()` on the *first* write, so a browser that never saves a pref never gets a cookie. All prefs live under one `session.ui` map (keeps the session root free for a future auth identity); the write reassigns the whole map (`var ui = session.ui || {}; ui[key]=value; session.ui = ui;`) so the store persists the mutation.
- **SSR hydration, not a client GET.** The page emits `session.ui` server-side as a JSON island (`_.prefs = { split: ... }` + `<div ka:island="prefs"/>` → `#prefs-data`) so it comes back *pre-arranged* on first paint — no fetch, no flash. A small client helper (`kaPrefs.read(key, fallback)` off `#prefs-data`, `kaPrefs.set(key, value)` → debounced `POST /ui/session`) is the only client surface; reads never round-trip.

A note on multi-tab: the session cookie is shared across a browser's tabs, so it fits *preferences* (a split size, a recent-prompt list) but not per-tab identity like "the file open in THIS tab" — leave that in-memory or in the URL.

## Session stores

### In-memory (default once enabled)

`InMemorySessionStore` keeps sessions in a `ConcurrentHashMap`. Suitable for development, single-instance deployments, and local single-user tools (the karate-max console uses it). Sessions are lost on server restart — which, for UI state, is a feature (see the rule above).

### Custom session store

Implement the `SessionStore` interface for a persistent or distributed backend (file, Redis, DynamoDB, …):

```java
public interface SessionStore {
    Session create(int expirySeconds);  // construct + persist a fresh session
    Session get(String id);              // null if missing or expired
    void save(Session session);          // upsert; bump `updated` to now (epoch sec)
    void delete(String id);
}
```

Time fields on `Session` (`created`, `updated`, `expires`) are **epoch seconds (UTC)** — use `Instant.now().getEpochSecond()`, not `System.currentTimeMillis()`. Every built-in store (`InMemorySessionStore`, `FileSessionStore`) follows this contract.

> <a id="ddb-ttl-seconds"></a>**If you back sessions with DynamoDB:** its `TimeToLiveSpecification` interprets the TTL attribute as **Unix epoch SECONDS**. A millisecond value silently no-ops (DDB reads `1.7 × 10¹²` as ~year 58,200 AD and never sweeps the row — the table accumulates forever). Write `Session.getExpires()` (already seconds) straight to the TTL attribute, and lock the contract with a test asserting `session.getExpires() < 1e11` (10-digit = seconds; 13-digit = ms = broken).

## Session vs context

| Object | Scope | Persists | Use For |
|--------|-------|----------|---------|
| `session` | Per-user, across requests | Yes (cookie-based) | User identity, preferences, per-browser UI/route state |
| `context` | Per-request | No | Request handling, redirects, flash messages, utilities |
| `context.flash` | One request after redirect | Auto-cleared | Success/error messages after form submission |

## Storing complex objects

Session values are serialized to JSON. Store plain objects, arrays, and primitives:

```html
<script ka:scope="global">
  session.recentSearches = session.recentSearches || [];
  if (request.param('q')) {
    session.recentSearches.unshift(request.param('q'));
    if (session.recentSearches.length > 10) {
      session.recentSearches = session.recentSearches.slice(0, 10);
    }
  }
</script>
```

Avoid storing large objects (file contents, images) in sessions — use a separate store and keep only references in the session.

## Sessions without authentication

Sessions can be used without user login — for wizard progress, per-browser UI state, or anonymous preferences:

```html
<script ka:scope="global">
  if (!session) {
    context.init(); // anonymous session
  }
  session.theme = request.param('theme') || session.theme || 'light';
</script>
```
