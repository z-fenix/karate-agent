# karate-markup — Pattern Catalog

> **Audience:** an LLM (or human) building UI in karate-markup. This catalog is the map: which patterns exist, what each is for, and where the live working code lives. The actual *how* lives in `Skill.help('markup')` (rules, gotchas, principles) and the playground source at `markup/playground/tailwind/src/main/resources/app/`.
> **Why minimal snippets:** the playground is the canonical reference — every pattern has a working page. The snippets here capture each pattern's *shape*; for the full implementation, read the listed files.
> **Cross-refs:** gotcha citations use slugs (e.g. `#cdn-script-order`). See `Skill.help('markup')` §Gotchas.

---

## How to use this catalog

1. **Pick a category** matching your problem (Layout / Inputs / Display / Overlays / Realtime / Editors).
2. **Find the pattern** — match its **Problem** statement against your need.
3. **Read the Files** list — that's the working code. The snippet here is illustrative, not a substitute.
4. **Check Gotchas** — every pattern carries the gotchas the playground hit while building it.

The catalog is **stable-slug**: pattern IDs (`data-table`, `splitter`, `mega-menu-hover`) don't change. Categories may grow.

---

## Catalog entry template

Every entry follows this shape. Fill the fields concretely; absent ones (no Java wiring, no related components) become `—`.

```markdown
### <pattern-slug>
**Title:** Human Readable Name  ·  **Category:** Layout / Inputs / Display / Overlays / Realtime / Editors  ·  **Status:** stable / experimental / deprecated

**Problem.** One paragraph describing the user/dev problem this solves. What does the alternative-without-it look like?

**When to reach for it.**
- Bullet list of fit conditions.

**When NOT to.**
- Bullet list of misuse cases or cheaper alternatives.

**Live demo:** http://localhost:8080/<slug>

**Files (read these for the full picture):**
- `app/<slug>.html` — template
- `app/api/<slug>-*.js` — handler(s) (if any)
- `App.java#<MarkerOrStore>` — Java wiring (if any)
- `app/components/<x>.html` — reused components (if any)

**Minimal representative snippet:**
~~~html
<!-- shape that captures the *core* of the pattern, not the full file -->
~~~

**Gotchas.** See `Skill.help('markup')`: #slug-1, #slug-2.
**Related.** other-pattern-slug, another-slug.
```

---

## Categories

- [Foundations](#foundations) — Java-side setup + routing + sessions
- [Layout](#layout) — page-level structure
- [Inputs](#inputs) — user-driven data entry
- [Display](#display) — read-mostly content
- [Overlays](#overlays) — floating / popped UI
- [Realtime](#realtime) — server-pushed UI
- [Editors](#editors) — code-shaped editing

---

## Foundations

### app-setup
**Title:** App setup, routing, sessions  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** Before any UI pattern is useful, the karate-markup app needs a `ServerConfig` + `HttpServer` boot, a routing strategy (path / query / session-based), optional session-store wiring (in-memory, DynamoDB), and an auth approach (template-level guard via `context.redirect`). Without a clear reference, every new app reinvents these and gets the security headers, CSRF defaults, and session-TTL mechanics wrong.

**When to reach for it.**
- Starting a new karate-markup app.
- Adding sessions or auth to an existing one.
- Wiring DynamoDB-backed session storage and hitting the TTL = epoch SECONDS gotcha.
- Choosing between path-based (`/sessions/{id}`), query-based (`/?view=session`), or session-based (`session.route`) routing.

**When NOT to.**
- Day-to-day template work — see `Skill.help('markup')` quick-reference sections (`requestFilter`, layout shell, directory grouping).

**Live demo / exemplar:**
- **Working code:** `markup/playground/tailwind/src/main/java/playground/App.java` — reads as the canonical minimal setup.
- **Running app:** http://localhost:8080/ (every page renders inside the configured shell).

**Files (read these for the full picture):**
- `app-setup.md` — deep-dive (Setup, Routing, Sessions sections).
- `markup/playground/tailwind/src/main/java/playground/App.java` — the live exemplar: `ServerConfig`, `globalVariables`, route registrations, `SimpleObject` stores, `SseHandler`, `rawPaths(...)` opt-outs.
- `Skill.help('markup')` §`requestFilter` and §Layout shell + bookmarkable URLs — quick-reference subset of SETUP.

**Minimal representative snippet:**
```java
ServerConfig config = new ServerConfig(resourceRoot)
        .devMode(true)
        .globalVariables(Map.of("utils", new MyUtils(), "devMode", true))
        .templateRoute("/sessions/{id}", "session.html");

ServerRequestHandler handler = new ServerRequestHandler(config, resolver);
HttpServer.start(8080, handler);
```

**Gotchas.** See `Skill.help('markup')`: #ddb-ttl-seconds, #context-redirect-short-circuits, #simpleobject-jskeys.
**Related.** client-stack, layout-shell, modal-as-url (uses `rawPaths`).

---

### client-stack
**Title:** Client stack — shell `<head>` + `<body>` assembly  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** A working app shell pulls together seven moving pieces in the right order: theme pre-paint script, CSRF meta, CSS framework CDN + design tokens, htmx + app.js + Alpine in load-bearing order, chrome (rail/topbar/breadcrumb), the `#main-content` slot, and global mounts (palette / toast / modal stack). Get any one wrong and the failure is silent and confusing — htmx POSTs 403 with no obvious cause (missing CSRF meta), Alpine magics go undefined and the entire `x-data` cascades to nulls (script order wrong), dark mode flashes light on every load (no pre-paint). This entry is the index that ties them together; companion to `app-setup` (server foundations ↔ client foundations).

**When to reach for it.**
- Setting up the shell template for a new karate-markup app.
- Diagnosing "everything looks fine but X silently doesn't work" in the shell (CSRF, theme, Alpine magic, htmx error toasts).
- Auditing whether an existing shell has all seven pieces.

**When NOT to.**
- Day-to-day template work — the shell is a one-time setup. Once it's right, content pages don't touch it.
- Page-specific chrome (per-page sidebar widgets, page-local modals) — those live in the page, not the shell.

**Live exemplar:** `markup/playground/tailwind/src/main/resources/app/layout.html` — the seven pieces wired in order with comments at each step.

**Files (read these for the full picture):**
- `Skill.help('markup')` §"Layout shell + bookmarkable URLs" → "Shell composition checklist" — the canonical index of the seven pieces with cross-links to depth.
- `Skill.help('markup')` §"CDN Block" — the exact 3 script tags + Alpine-microtask ordering rationale.
- `Skill.help('markup')` §"HTMX error surface" — `pub/app.js` body (CSRF header injection + error toasts).
- `tailwind.md` §Setup, §Dark mode, §"Layout shell — vertical icon rail + sticky topbar" — Tailwind specifics: design tokens, theme pre-paint script, the visual shell HTML.
- `markup/playground/tailwind/src/main/resources/app/layout.html` + `pub/app.js` — the live build.

**Minimal representative snippet:** intentionally not provided — the shell is too long for a useful snippet and reading the live `layout.html` end-to-end is the right entry point. The "Shell composition checklist" in skill.md is the index; this entry exists so it's discoverable from the catalog rather than buried in prose.

**Gotchas.** See `Skill.help('markup')`: #cdn-script-order, #asset-paths-must-be-absolute, #hx-request-shell-bypass, #fragment-replace-host-disappears.
**Related.** app-setup, layout-shell, auth-signin (CSRF meta wiring), post-redirect-get (HX-Redirect path).

---

### auth-signin
**Title:** Sign-in form (CSRF round-trip)  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** Every enterprise app needs a public sign-in form that creates a session, hands the user a CSRF token, and round-trips invalid input without losing the typed email. Without a documented pattern, devs reinvent the cookie-issue / CSRF-skip-on-anonymous / never-round-trip-the-password trio per app, and either over-protect signin (CSRF-block the very POST that would create a session) or under-protect it (no token rotation post-signin).

**When to reach for it.**
- Any app that has authenticated users.
- You need a working reference for `context.init()` + `session.user = …` + cross-redirect flash.
- Verifying CSRF end-to-end: meta tag → htmx header → server validation.

**When NOT to.**
- Apps with zero auth (internal benchmarks, public dashboards). Skip `sessionStore`, skip CSRF, skip this whole pattern.
- OAuth-only apps (no password form). The signin redirects to the IdP — see app-setup.md §"Authentication pattern (OAuth)" instead; the post-callback session-write is the same as here.

**Live demo:** http://localhost:8080/signin (anonymous: form + dev shortcuts; signed-in: bounces to `/account`).

**Files (read these for the full picture):**
- `app/signin.html` — public template, ka:scope global handler, `ka:data="form:initialForm"` with email round-trip.
- `App.java#AuthStore` — tiny in-memory user directory, `auth.signin(email, password)`, `auth.demoAdmin` / `auth.demoUser` constants.
- `App.java#start` — `.sessionStore(new InMemorySessionStore())` line that unlocks `session.*` and `context.csrf`.
- `app/layout.html` — line `<meta th:if="context.csrf" name="csrf-token" th:content="context.csrf.token"/>`.
- `app/pub/app.js` — `htmx:configRequest` listener that mirrors the meta token into `X-CSRF-Token`.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  if (session?.user) context.redirect('/account');     // already signed in
  if (request.post) {
    var posted = request.paramJson('form') || {};
    var user = auth.signin(posted.email, posted.password);
    if (user) {
      context.init();                                   // create session + cookie
      session.user = user;
      context.flash.success = 'Welcome, ' + user.name + '.';
      context.redirect('/account');                     // short-circuits render
    }
    _.errorMsg = 'No account matches that email and password.';
    _.initialForm = { email: posted.email, password: '' };  // round-trip email only
  }
</script>
<form ka:data="form:initialForm" ka:post="this" hx-target="#main-content">
  <input x-model="form.email"    type="email" autocomplete="username"/>
  <input x-model="form.password" type="password" autocomplete="current-password"/>
  <button type="submit">Sign in</button>
</form>
```

**Why CSRF doesn't block the signin POST.** `validateCsrf` skips when `session == null || session.isTemporary()` (see `karate-core/io.karatelabs.http.ServerRequestHandler#validateCsrf`). The signin POST runs against a temporary session, gets through, calls `context.init()` to mint a real one. Subsequent state-changing POSTs require the token.

**Gotchas.** See `Skill.help('markup')`: #context-redirect-short-circuits, #session-no-store-opaque, #scope-bare-vs-underscore.
**Related.** auth-session-guard, form-binding, app-setup.

---

### auth-session-guard
**Title:** Session guard, role gate, signout  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** Once a sign-in pattern exists, every protected page needs the same three-line shape: redirect anonymous visitors, check role for sensitive surfaces, expose a POST-only signout. Authorization failures should render in the layout shell (so the user sees what they tried) instead of bait-and-switch redirecting. Hand-rolling this per page leaks subtle differences — some pages forget the guard, some redirect non-admins, some signout on GET (CSRF-vulnerable). One canonical reference + a layout chip kills the variance.

**When to reach for it.**
- Any page that requires a signed-in user.
- Any page that requires a role/permission beyond "signed in."
- Apps that need a visible "who am I, sign me out" affordance in the chrome.

**When NOT to.**
- Business-level authorization ("user X owns resource Y"). That belongs in the handler, not this guard — the guard is for authentication + coarse role gating only.
- Per-action permissions inside an admin panel. Use this guard to gate the *page*; layer per-row checks on top.

**Live demo:**
- http://localhost:8080/account — protected page (redirects to `/signin` if anonymous), session inspector card, signout button.
- http://localhost:8080/admin — role-gated; non-admins see a 403 panel rendered in-shell.

**Files (read these for the full picture):**
- `app/account.html` — auth guard, `session.user` display, session/CSRF inspector, POST signout form.
- `app/admin.html` — auth guard + `session.user.admin` check, sets `response.status = 403` and renders the deny panel in the same shell.
- `app/signout.html` — POST-only handler: `context.close()` + `context.flash.success` + `context.redirect('/signin')`. GET renders a confirm card.
- `app/layout.html` — topbar auth chip: signed-in name + admin pill + signout form, OR a "Sign in" link for anonymous.

**Minimal representative snippet:**
```html
<!-- Protected page -->
<script ka:scope="global">
  if (!session?.user) context.redirect('/signin');         // auth guard
  _.user = session.user;
</script>

<!-- Role-gated page -->
<script ka:scope="global">
  if (!session?.user) context.redirect('/signin');
  if (!session.user.admin) {
    response.status = 403;
    _.denied = true;                                       // template branches on this
  }
</script>

<!-- POST signout (don't do GET — CSRF-vulnerable) -->
<form ka:post="/signout" hx-target="#main-content">
  <button type="submit">Sign out</button>
</form>
```

**Why render-and-403 instead of redirecting non-admins.** A redirect on authorization failure is a bait-and-switch: the user lands somewhere unrelated and has no idea why. Rendering the deny panel in-shell with a `403` status preserves both the URL (they can bookmark and retry as admin) and the UX (clear "you don't have access" message with a sign-out option).

**Gotchas.** See `Skill.help('markup')`: #context-redirect-short-circuits, #scope-bare-vs-underscore, #session-no-store-opaque.
**Related.** auth-signin, app-setup, flash component.

---

### post-redirect-get
**Title:** Post-Redirect-Get (htmx + native form)  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** Any state-mutating POST should redirect to a safe GET so a browser refresh doesn't re-submit and the back button doesn't re-trigger the action. With htmx in the picture, a bare `302` is a trap: the XHR layer silently follows the redirect and only swaps the configured `hx-target`, leaving the document `<head>` (and anything else outside the swap) on the previous page. karate-core promotes `context.redirect()` to `HX-Redirect: <path>` (status 200) when `HX-Request: true` is present, so htmx does a full window navigation — same end result as the native-form path.

**When to reach for it.**
- Any handler that mutates server state and then needs to render a fresh page (form save, "approve" button, "delete" confirmation that returns the user to a list view).
- Verifying the `context.redirect()` round-trip works under htmx — the `HX-Redirect` promotion is the part most apps don't realize they're getting for free.

**When NOT to.**
- Read-only handlers — no mutation, no need for the redirect.
- Endpoints that should swap *only* a fragment (modal-as-URL, partial table refresh). Those want `hx-target` + a fragment response, not a redirect.

**Live demo:** http://localhost:8080/post-redirect-get — anonymous, two buttons (`ka:post` and native `<form method="post">`) hit the same handler; both end with a full page nav.

**Files (read these for the full picture):**
- `app/post-redirect-get.html` — template, two forms, observable counters.
- `App.java#PrgStore` — atomic counter pair + last-source / last-time. Exposed as the `prg` global.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  if (request.post) {
    prg.bump(request.param('source'));
    context.redirect('/post-redirect-get');   // PRG: redirect after mutate
  }
</script>

<!-- htmx variant: HX-Redirect (200), htmx does window.location -->
<form ka:post="/post-redirect-get">
  <input type="hidden" name="source" value="htmx"/>
  <button type="submit">Bump (htmx)</button>
</form>

<!-- native variant: 302 + Location, browser follows -->
<form method="post" action="/post-redirect-get">
  <input type="hidden" name="source" value="form"/>
  <button type="submit">Bump (form)</button>
</form>
```

**The htmx-vs-native trade-off.** For non-auth flows, `ka:post` is fine — the `HX-Redirect` promotion preserves correctness. For auth-state changes (sign-in, sign-out, session reset), prefer native `<form method="post">`: the page boundary is exactly what you want when CSRF tokens, session cookies, and the layout shell all need to re-render with the new identity. See `auth-signin` and `auth-session-guard`. Native forms also need a hidden `<input name="_csrf" th:value="context.csrf.token"/>` since they can't piggyback on the htmx `X-CSRF-Token` header injected by `pub/app.js`.

**Gotchas.** See `Skill.help('markup')`: #context-redirect-short-circuits.
**Related.** auth-signin, auth-session-guard, app-setup, htmx-push-url.

---

### htmx-push-url
**Title:** Bookmarkable HTMX navigation (`hx-push-url` + back button)  ·  **Category:** Foundations  ·  **Status:** stable

**Problem.** htmx swaps a fragment into the configured `hx-target` and leaves the browser URL bar alone — so the back button skips the swap, refresh loses the state, and the URL can't be shared. `hx-push-url="true"` updates the URL bar on swap. Two contracts must hold simultaneously: the server has to handle every URL `hx-push-url` produces as a full-page request (for direct nav / refresh / bookmark), AND the htmx swap path has to return bare content for the same URL. The layout shell does both already (HX-Request bypasses shell automatically); the page handler routes off `request.param(...)` (or `request.pathMatches(...)`) so the same URL renders the same view either way.

**When to reach for it.**
- Composite pages where users navigate between multiple states they'd want to bookmark, share, or return to via the back button: tabs, wizards, lookups, modal-as-url, search-result drilldowns, the queued HTTP runner.
- Any page where refresh has to land the user on what they were just looking at.

**When NOT to.**
- Atomic-demo pages that have a single view. Adding push-url for its own sake means the server must now route every demo URL — pointless overhead.
- In-page partial refreshes that don't represent a navigable state (e.g. a "load more" pagination append where the URL bar accurately stays on the list page).

**Live demo:** http://localhost:8080/htmx-push-url — three view links with `hx-push-url="true"` plus one comparison link without it. DevTools Network shows the swap path (`HX-Request: true`, bare-content response) vs the full-page path (no header, shell-wrapped response).

**Files (read these for the full picture):**
- `app/htmx-push-url.html` — the page; reads `?view=` and renders one of three views.
- `Skill.help('markup')` §"Layout shell + bookmarkable URLs" — how the shell handles HX-Request bypass + rawPaths.
- `app-setup.md` §"SPA-feel with `hx-push-url`" — the recipe with the route-coverage caveat.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  _.view = request.param('view') || 'overview';   // GET handler routes by query
</script>

<!-- Render the view from the server param so direct nav / refresh / bookmark work -->
<div th:if="view == 'overview'">…</div>
<div th:if="view == 'details'">…</div>

<!-- Swap + push URL: htmx updates the address bar, back button replays history -->
<a ka:get="/htmx-push-url?view=details" hx-target="#main-content" hx-push-url="true">
  Details
</a>
```

**Edge case (Gotcha `#hx-push-url-multi-param`).** If the page exposes filters / search / pagination as separate query params, an `hx-push-url="true"` swap must re-emit ALL preserved params in the request URL — otherwise the URL bar drops everything except the one you just changed and the back button replays a stale state.

**Gotchas.** See `Skill.help('markup')`: #hx-push-url-multi-param, #hx-request-shell-bypass.
**Related.** layout-shell, modal-as-url, post-redirect-get.

---

## Layout

### splitter
**Title:** Resizable splitter (H + V, persisted)  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** IDE-class layouts need user-resizable panes that remember their sizes across reloads. Without a primitive, every layout reinvents drag-handle math, double-click reset, bound clamping, and `localStorage` persistence per pane.

**When to reach for it.**
- Building any 2- or 3-pane workspace (HTTP runner, file browser + editor + preview, audit drawer + main).
- Users need to control their viewport for sustained work, not glance.
- Sizes must survive page reload.

**When NOT to.**
- Fixed-aspect dashboards. CSS Grid is simpler.
- Mobile / narrow viewports. Resizable handles aren't useful at <768px.

**Live demo:** http://localhost:8080/splitter

**Files:**
- `app/splitter.html` — template + Alpine factory
- `App.java#splitter route` — straight `templateRoute("/splitter")`
- — *no API, no components — pure client*

**Minimal representative snippet:**
```html
<div x-data="splitterShell()" class="flex">
  <div :style="`width: ${leftW}px`">left</div>
  <div class="w-1 cursor-col-resize bg-ink-200 hover:bg-brand-400"
       @mousedown="startDrag('left', $event)"
       @dblclick="resetLeft()"></div>
  <div class="flex-1">right</div>
</div>
<script>
function splitterShell() {
  return {
    leftW: parseInt(localStorage.getItem('splitter:demo:left') || '320', 10),
    startDrag(which, ev) { /* listen on window mousemove/up; clamp; persist */ },
    resetLeft() { this.leftW = 320; localStorage.removeItem('splitter:demo:left'); },
  };
}
</script>
```

**Gotchas.** See `Skill.help('markup')`: #alpine-text-select-during-drag, #localstorage-key-collisions.
**Related.** tabs, layout-shell.

---

### tabs
**Title:** Tabbed multi-doc workspace (open / close / reorder, dirty marker, URL hash)  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** Multi-doc editing surfaces (HTTP runner, IDE, log dashboards) need tab strips with open/close, dirty markers, drag-reorder, and bookmarkable active tab. Hand-rolling each is repetitive; the URL-hash + popstate plumbing is the part developers skip.

**When to reach for it.**
- Multiple parallel docs/views the user toggles between.
- Active tab must survive reload + back/forward navigation.
- Dirty state (unsaved changes) must be visible per tab.

**When NOT to.**
- Single-doc workspaces — use a header instead.
- Steps in a wizard — use the `stepper` pattern (linear, not parallel).

**Live demo:** http://localhost:8080/tabs

**Files:**
- `app/tabs.html` — template + Alpine factory + URL-hash + localStorage persistence
- `App.java#tabs route` — straight templateRoute

**Minimal representative snippet:**
```html
<div x-data="tabStrip()" x-cloak>
  <div class="flex border-b">
    <template x-for="t in openTabs" :key="t.id">
      <button @click="activate(t.id)"
              :class="t.id === activeId ? 'border-brand-500' : 'border-transparent'"
              class="px-3 py-2 border-b-2">
        <span x-text="t.label"></span>
        <span x-show="t.dirty" class="text-coral-500">●</span>
        <span @click.stop="close(t.id)" class="ml-2 opacity-60 hover:opacity-100">×</span>
      </button>
    </template>
  </div>
  <div x-show="active" class="p-4" x-html="active?.body"></div>
</div>
```

**Gotchas.** See `Skill.help('markup')`: #urlhash-popstate-init, #alpine-x-cloak-flicker.
**Related.** splitter, stepper.

---

### breadcrumb
**Title:** Breadcrumb trail  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** Pages need a "you are here" trail showing the navigation context (e.g. `Playground › Layout › Splitter`). Hard-coding it per page is repetitive; deriving it from a server-built `crumbs` array centralizes the convention.

**When to reach for it.**
- Hierarchical page trees (>2 levels deep).
- Pages reachable from multiple entry points (the trail tells the user how they got here).

**When NOT to.**
- Top-level dashboards / single-page UIs — the page title is enough.

**Live demo:** http://localhost:8080/ (used in the layout shell topbar)

**Files:**
- `app/components/breadcrumb.html` — fragment driven by a `crumbs` server-side variable
- `app/layout.html` — derives `crumbs` from `request.path` against the categories table; renders the fragment in the topbar

**Minimal representative snippet:**
```html
<!-- in layout shell topbar: server prepares `crumbs = [{label, href}]` -->
<nav class="flex items-center gap-2 text-sm">
  <template th:each="c, iter : ${crumbs}">
    <a th:if="!iter.last" th:href="c.href" th:text="c.label" class="text-ink-500 hover:text-ink-700"></a>
    <span th:if="!iter.last" class="text-ink-300">›</span>
    <span th:if="iter.last" th:text="c.label" class="text-ink-700"></span>
  </template>
</nav>
```

**Where the data comes from — important.** The `crumbs` array is built **inside the shell** by default, NOT inside the content page. `_` is per-template-render scope; anything content writes to `_.crumbs` lives and dies inside the content render and never reaches the shell (only `content`, `contentTemplate`, `session`, and `context.flash` cross the boundary — see `Skill.help('markup')` §"Layout shell + bookmarkable URLs"). The playground shell derives its trail from `request.path` against the existing `categories` table:

| Path shape | Resulting trail |
|---|---|
| `/` | `Playground` (1 level, current page) |
| `/catalog/<slug>` | `Playground › <Category>` (2 levels — middle "Catalog" deliberately omitted; it can't link anywhere meaningful) |
| `categories[].patterns[].href` match | `Playground › <Category› › <Pattern>` (3 levels, category clickable) |
| `*.md` | `Playground › Docs › <basename>` |
| anything else | `Playground › <contentTemplate>` (fallback) |

**Per-page override.** When derivation is wrong (custom labels, deep-nested page outside `categories`, one-off shape), a content page opts in via `context.setGlobal('crumbs', [...])`. The shell reads `context.getGlobal('crumbs')` first and falls back to the derivation. See `Skill.help('markup')` §`context.setGlobal/getGlobal` for the cross-render contract.

**Gotchas.** See `Skill.help('markup')`: #fragment-th-with, #shell-derives-crumbs.
**Related.** layout-shell, client-stack, mega-menu-hover.

---

### layout-shell
**Title:** Layout shell (rail + topbar + main + global mounts)  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** Every page in an app shares chrome: rail, topbar, breadcrumb, theme toggle, palette, error toast. Without a single `layout.html` that wraps content, each page reimplements chrome and drifts.

**When to reach for it.**
- Any multi-page app. (The reference: `markup/playground/tailwind/src/main/resources/app/layout.html`.)

**When NOT to.**
- Marketing / landing pages — they have their own chrome.
- htmx fragment endpoints (modal-as-url, etc.) which deliberately bypass the shell via `HX-Request` + `rawPaths(...)`.

**Live demo:** http://localhost:8080/ (every page renders inside it)

**Files:**
- `app/layout.html` — the shell
- `App.java#shellTemplate("layout.html")` — registers the wrapper
- `App.java#rawPaths(...)` — opt-out routes that bypass it

**Minimal representative snippet:**
```html
<!DOCTYPE html>
<html>
<head th:fragment="head"><!-- meta, fonts, Tailwind, theme-resolve script, htmx, app.js, alpine --></head>
<body>
  <div class="flex min-h-screen">
    <aside class="w-16"><!-- rail --></aside>
    <div class="flex-1 flex flex-col">
      <header class="h-16"><!-- breadcrumb, palette button, theme --></header>
      <main id="main-content" th:utext="content">content</main>
    </div>
  </div>
  <!-- global mounts: palette, error toast, etc. -->
</body>
</html>
```

**Gotchas.** See `Skill.help('markup')`: #cdn-script-order, #fragment-replace-host-disappears.
**Related.** breadcrumb, rail, mega-menu-hover, mega-menu-click-pinned.

---

### rail
**Title:** Vertical rail (narrow icon nav with hover flyouts)  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** App shells need a primary nav that's always visible without dominating the viewport. A 64px-wide left rail with category icons is the IDE convention (VSCode, IntelliJ, Linear). Hover flyouts then expose the patterns within each category without a click cost.

**When to reach for it.**
- Multi-page apps with >6 top-level sections.
- IDE-class shells where the main viewport is the work surface.
- Apps that want zero state to manage (CSS-only flyouts).

**When NOT to.**
- Marketing / landing pages — use a top-bar nav.
- Tiny apps with <4 sections — a top-bar suffices.
- Touch-first apps — pair the rail with a bottom-tab equivalent.

**Live demo:** http://localhost:8080/ (the left rail with 6 category icons + hover flyouts).

**Files:**
- `app/layout.html` — the rail markup + flyout panels.
- `app/components/mega-menu.html :: hover` — used by each rail item's flyout panel.
- `app/components/tooltip.html :: styles` — used for the small icon tooltips on the home / theme buttons.

**Minimal representative snippet:**
```html
<aside class="w-16 bg-ink-950 sticky top-0 h-screen flex flex-col">
  <a href="/" class="ka-tip" data-tip="Home"><span>🏠</span></a>
  <th:block th:each="cat : categories">
    <div class="relative group">
      <a th:href="'/catalog/' + cat.slug" class="w-10 h-10 …">
        <span th:text="cat.icon"></span>
      </a>
      <div class="absolute left-full ml-3 w-72 opacity-0 invisible
                  group-hover:opacity-100 group-hover:visible …">
        <a th:each="p : cat.patterns" th:href="p.href" th:text="p.label"></a>
      </div>
    </div>
  </th:block>
</aside>
```

**Gotchas.** See `Skill.help('markup')`: #asset-paths-must-be-absolute (icon paths in shell must be `/pub/...`).
**Related.** layout-shell, mega-menu-hover, tooltip, breadcrumb.

### side-panel
**Title:** Side panel (drawer family)  ·  **Category:** Layout  ·  **Status:** stable

**Problem.** Modals demand focus; sometimes the right UX is "show context, keep the page visible." Without a canonical pattern, every contextual detail view (selected row → side panel, quick action → bottom sheet) reinvents the slide-in transition, the backdrop, the focus management. And devs often nest the backdrop inside the panel and end up with compounded opacities.

**When to reach for it.**
- "Select a row → see its detail on the side" pattern.
- Quick-action menus that don't deserve a full screen modal (mobile especially).
- Triage flows where the user moves through many items and reads context for each.

**When NOT to.**
- Required decisions (delete confirm, save before navigate) — use `modal`.
- Server-state-heavy detail panels — use `modal-as-url` so the panel body lives at a URL.

**Live demo:** http://localhost:8080/side-panel

**Files:**
- `app/side-panel.html` — right drawer (click a row → context view) + bottom sheet (action picker). Two Alpine state flags drive the open/close; row data flows in via `data-*` attributes read on click.

**Minimal representative snippet:**
```html
<!-- Backdrop (sibling, not nested) -->
<div x-show="rightOpen" x-cloak class="fixed inset-0 bg-ink-950/30 z-30"
     x-transition:enter="transition-opacity ease-out duration-200"
     x-transition:enter-start="opacity-0" x-transition:enter-end="opacity-100"
     @click="rightOpen = false"></div>

<!-- Panel -->
<aside x-show="rightOpen" x-cloak
       class="fixed top-0 right-0 h-screen w-[420px] max-w-full bg-white dark:bg-ink-900 z-40"
       x-transition:enter="transition-transform ease-out duration-200"
       x-transition:enter-start="translate-x-full" x-transition:enter-end="translate-x-0"
       @keydown.escape.window="rightOpen = false">
  <!-- header / body / footer -->
</aside>
```

**Two siblings, not nested.** Backdrop + panel get their own transitions (opacity vs transform). Nesting them stacks opacities and produces a muddy panel over a muddy backdrop. Keep them flat in the DOM.

**Modal vs side panel.** Modal = focus-demanding ("decide this before continuing"). Side panel = focus-offering ("here's context — keep working"). Convert flowing-context flows (read-mostly detail, multi-row triage) to side panels.

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (both elements need x-cloak so they don't flash open before mount).
**Related.** modal, modal-as-url, dropdown-menu, popover.

---

## Inputs

### form-binding *(stub — retrofit of /form)*
### combobox
**Title:** Combobox (HTMX typeahead)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Forms with a single-select against a large list (assign user, pick a tag, choose an org) need typeahead — the user types a few characters, sees matching rows, picks one. Without a documented pattern, devs reach for client-side JSON APIs + custom rendering, ending up with two separate row-templates (one in the search dropdown, one in the row-list). Server-rendered HTML responses with HTMX collapse this to one template.

**When to reach for it.**
- Single-select against >20 options where scrolling a `<select>` is impractical.
- The result rows benefit from rich rendering (avatars, badges, secondary lines).
- The backing dataset is server-side (DB query, store API) — not a static client-side list.

**When NOT to.**
- Static <20-option list — a `<select>` is fine.
- Multi-select with token chips — use `tag-input`.
- The pattern is "search to navigate" not "search to pick" — use `palette` (⌘K).

**Live demo:** http://localhost:8080/combobox

**Files:**
- `app/combobox.html` — input + results panel + Alpine host that listens for selection events.
- `app/api/combobox-search.js` — handler: reads `?q=…`, calls `dt.query()`, returns HTML rows.
- `App.java#DataTableStore` — backing store (shared with `data-table`).

**Minimal representative snippet:**
```html
<input name="q" type="search"
       hx-get="/api/combobox-search"
       hx-trigger="keyup changed delay:200ms"
       hx-target="#combobox-results"
       hx-swap="innerHTML"/>

<ul id="combobox-results"
    @combobox-pick.window="pick($event.detail)"></ul>
```

```js
// /api/combobox-search.js
var q = request.param('q') || '';
if (q.length < 1) { response.body = ''; }
else {
  var page = dt.query('name', 'asc', 1, 8, q);
  response.body = renderRows(page.rows);  // server-rendered HTML
}
response.headers['Content-Type'] = 'text/html';
```

**Why server-rendered HTML, not JSON:** the search dropdown row markup matches what users see elsewhere (status badge tones, group line). Returning HTML keeps the row template in one place — the API handler. Pure JSON forces every consumer to re-implement the row.

**Gotchas.** See `Skill.help('markup')`: #htmx-double-colon (use `hx-on-htmx-…`), #expressions-only-in-th-and-ka.
**Related.** data-table (shares the store), tag-input, palette.
### tag-input
**Title:** Tag input (token field)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Many forms need a free-form list of strings — tags, recipients, allowlist entries, label filters. Without a canonical pattern, devs use a comma-separated text field (no validation, no chips, hard to delete one entry) or roll their own widget per page (chip rendering, key handling, dedupe, all reinvented).

**When to reach for it.**
- Free-form list of short strings the user types in (tags, keywords, recipients).
- The number of entries is open-ended — not a closed enum (use `combobox` for picking from a list).
- Visual chip feedback matters — confirms each entry committed.

**When NOT to.**
- Picking from a known list — use `combobox` (server-side typeahead) instead.
- Single-value input — a plain `<input>` is enough.
- Long-text capture — a `<textarea>` is the right semantic.

**Live demo:** http://localhost:8080/tag-input

**Files:**
- `app/tag-input.html` — two variants: standalone Alpine widget + form round-trip via `ka:data`.
- — *no API handler — the standalone form just echoes back `request.paramJson('form')` to demonstrate the wire shape.*

**Minimal representative snippet:**
```html
<div x-data="tagInput(['design', 'inputs'])" @click="$refs.input.focus()"
     class="flex flex-wrap items-center gap-1.5 px-2 py-1.5 border rounded-md cursor-text">
  <template x-for="(t, i) in tags" :key="t + ':' + i">
    <span class="inline-flex items-center gap-1 pl-2.5 pr-1 rounded-full bg-brand-50 text-brand-700">
      <span x-text="t"></span>
      <button type="button" @click.stop="remove(i)">×</button>
    </span>
  </template>
  <input x-ref="input" x-model="draft"
         @keydown.enter.prevent="commit()"
         @keydown.backspace="onBackspace()"
         @blur="commit()" placeholder="Add a tag…"/>
</div>

<script>
  function tagInput(initial) {
    return {
      tags: initial || [],
      draft: '',
      commit() {
        var v = (this.draft || '').trim();
        if (v && this.tags.indexOf(v) < 0) this.tags.push(v);
        this.draft = '';
      },
      remove(i) { this.tags.splice(i, 1); },
      onBackspace() { if (this.draft === '' && this.tags.length) this.tags.pop(); }
    };
  }
</script>
```

**Form integration.** Inside `<form ka:data="form:initialForm">`, bind `form.tags` from the outer scope (don't open a nested `ka:data` — Gotcha #ka-data-nested-scope). The hidden mirror input that `ka:data` injects serializes the whole `form` JSON; server reads via `request.paramJson('form')`.

**Gotchas.** See `Skill.help('markup')`: #ka-data-nested-scope (outer-scope binding for embedded widgets), #alpine-x-cloak-flicker (chip render shouldn't flash before Alpine mounts).
**Related.** combobox (typeahead for closed lists), form-binding (ka:data round-trip), data-mirror (payload-driven forms).

### inline-edit
**Title:** Inline edit (click cell → swap → save)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Editing a row by opening a full modal is heavyweight when the user only wants to fix one field. Inline edit shrinks the round-trip to a single cell — click the value, edit it in place, hit Enter to save. Without a documented pattern, every page reinvents the read↔edit↔read swap cycle, gets focus management wrong, and ends up with state drift between Alpine and the server.

**When to reach for it.**
- Dense tables where users frequently fix one field (email typo, role bump, status flip).
- The edit form has zero or one field; multi-field edits belong in a modal (see `modal-as-url`).
- The server can validate atomically — there's no cross-field dependency at save time.

**When NOT to.**
- Multi-field edits requiring validation across fields → `modal-as-url`.
- Optimistic-first UX (instant local update, async retry) — htmx swap is server-confirmed by design.
- Bulk edit ("change role on all selected") → use a separate bulk action with selection state.

**Live demo:** http://localhost:8080/inline-edit

**Files:**
- `app/inline-edit.html` — table renders read cells inline; each `<td>` carries `hx-get` to fetch the edit form on click.
- `app/api/inline-edit-cell.js` — one endpoint, three modes (`?mode=read`, `?mode=edit`, POST). Always returns a complete `<td id="cell-…">` so the htmx `outerHTML` swap preserves wiring across the cycle.
- `App.java#MockStore` — backing store (shared with `/modal-as-url`).

**Minimal representative snippet:**
```html
<!-- Read cell — click swaps itself with the edit form. -->
<td th:id="'cell-' + u.userId + '-email'"
    th:attr="'hx-get': '/api/inline-edit-cell?userId=' + u.userId + '&field=email&mode=edit'"
    hx-trigger="click" hx-target="this" hx-swap="outerHTML"
    th:text="u.email"></td>
```

```js
// /api/inline-edit-cell.js — single handler, three modes
var mode = (request.param('mode') || 'read').toLowerCase();
if (request.post) { mock.update(...); mode = 'read'; }
response.body = mode === 'edit' ? renderEditCell(u, field) : renderReadCell(u, field);
```

**Cancel = the same endpoint, `mode=read`.** The × button and the Escape keypress both hit `GET /api/inline-edit-cell?...&mode=read` with the cell as the target. No client-side "remember the previous value" state.

**Gotchas.** See `Skill.help('markup')`: #expressions-only-in-th-and-ka (use `th:attr` for the dynamic `hx-get` URL), #vals-on-submit-dropped (the edit form uses hidden inputs, not `ka:vals`).
**Related.** modal-as-url (multi-field edits), data-table, combobox.

### file-upload
**Title:** File upload (drag-drop + multipart + progress)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** File upload is a four-problem composite: dropzone UX (drag-over visual + click-to-pick), multi-file selection, per-file progress feedback, and multipart wire format. Without a canonical pattern, every page reinvents one of these and gets another wrong — most commonly skipping per-file progress because htmx's standard form submit doesn't surface `XMLHttpRequest.upload.onprogress` cleanly.

**When to reach for it.**
- Any UI where users contribute files (avatars, attachments, bulk-import payloads).
- Multi-file workflows where the user wants per-row feedback.
- Server can persist incrementally — each file is a complete record on arrival.

**When NOT to.**
- Single file, no progress needed (e.g. CSV pre-paste for `bulk-import-wizard`) — a plain `<input type="file">` inside a form is enough.
- Direct-to-S3 uploads via presigned URL — different wire shape; the client posts to S3, not to your server.

**Live demo:** http://localhost:8080/file-upload

**Files:**
- `app/file-upload.html` — dropzone (Alpine) + XHR per file with progress + recent-uploads panel that swaps in the server's HTML on each successful upload.
- `app/api/upload.js` — reads `request.files('upload')`, records metadata in `UploadStore`, returns rendered "recent" list as HTML.
- `App.java#UploadStore` — bounded ring of the last 10 uploads (metadata only; bytes are not retained).

**Minimal representative snippet:**
```html
<div x-data="uploadDemo()"
     @dragover.prevent="dropActive = true"
     @drop.prevent="dropActive = false; onDrop($event)"
     @click="$refs.input.click()">
  <input x-ref="input" type="file" multiple class="sr-only"
         @change="onPick($event.target.files)"/>
</div>

<script>
  function uploadDemo() {
    return {
      items: [], dropActive: false,
      onPick(fl) { Array.from(fl).forEach(f => this.start(f)); },
      onDrop(ev) { Array.from(ev.dataTransfer.files).forEach(f => this.start(f)); },
      start(file) {
        const item = { id: ++this._seq, name: file.name, size: file.size, progress: 0, status: 'uploading' };
        this.items.unshift(item);
        const fd = new FormData(); fd.append('upload', file, file.name);
        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/upload', true);
        const token = document.querySelector('meta[name="csrf-token"]');
        if (token) xhr.setRequestHeader('X-CSRF-Token', token.content);
        xhr.upload.onprogress = e => { if (e.lengthComputable) item.progress = Math.round(e.loaded / e.total * 100); };
        xhr.onload  = () => { item.status = xhr.status < 300 ? 'done' : 'error'; };
        xhr.send(fd);
      }
    };
  }
</script>
```

**XHR over htmx for real progress.** htmx's standard form submit doesn't surface `upload.onprogress` in a tracked way. Drive uploads from a small Alpine factory with raw XHR — the page still uses htmx for everything else.

**One POST per file.** Sending each file as its own POST gives row-per-progress-bar, lets one failure not block the others, and matches the visual model. The trade-off is server-side: handle each request idempotently.

**Don't keep bytes in heap.** Stream `request.file().value` straight to disk / S3 / blob store. Holding bytes in heap longer than the request OOMs the JVM under concurrency.

**Gotchas.** See `Skill.help('markup')`: #vals-on-submit-dropped (any hidden fields use `<input name="…">`, not `ka:vals`).
**Related.** bulk-import-wizard (composes file upload + parse + preview), form-binding.
### date-picker
**Title:** Date picker (native + custom)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Date inputs come in three shapes and devs reach for the wrong one regularly: native `<input type="date">` (best default, hard to theme), a heavyweight library like flatpickr (overkill for a calendar grid), or a custom widget rolled per page (calendar math reinvented poorly). A canonical pattern that documents when to use which — and ships a minimal custom widget for the cases native can't cover — short-circuits the design decision.

**When to reach for it.**
- Native `<input type="date">` — first pick for every form. Free locale + keyboard + mobile picker.
- Custom Alpine widget — only when native doesn't fit: range selection, in-row dense-table picker, calendar styled to match a custom design system end-to-end.

**When NOT to.**
- Need datetime, not just date — use `<input type="datetime-local">` or a full library (flatpickr) and document why.
- Free-form natural-language date entry ("next Tuesday") — that's a parser problem, not a picker problem.

**Live demo:** http://localhost:8080/date-picker

**Files:**
- `app/date-picker.html` — three variants on one page: native, custom single, custom range. Inline Alpine factories (`datePickerSingle`, `datePickerRange`) using only built-in `Date`.

**Minimal representative snippet:**
```html
<div x-data="datePickerSingle('2026-06-15')" x-init="init()"
     @click.outside="open = false" @keydown.escape.window="open = false"
     class="relative">
  <button @click="open = !open" :aria-expanded="open" class="…">
    <span x-text="value || '— choose a date —'"></span>
  </button>
  <input type="hidden" name="pickedDate" :value="value"/>
  <div x-show="open" x-cloak class="absolute mt-2 …">
    <!-- month header + grid + Today/Clear -->
    <div class="grid grid-cols-7 gap-0.5">
      <template x-for="row in weeks" :key="row[0].iso">
        <template x-for="cell in row" :key="cell.iso">
          <button @click="pick(cell.iso)"
                  :class="cell.isSelected ? 'bg-brand-600 text-white' : '…'"
                  x-text="cell.day"></button>
        </template>
      </template>
    </div>
  </div>
</div>
```

**ISO round-trip.** Always serialize the chosen date as `YYYY-MM-DD` — never the browser-locale display string — so the server has one parse path. The hidden mirror input handles this transparently.

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (the popover must not flash open before mount).
**Related.** form-binding (typical embedding case), stepper (date fields in multi-step forms).

### stepper
**Title:** Stepper (multi-step form, URL-per-step)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Sign-up flows, onboarding, license activation — anything that takes more than ~5 fields — splits naturally into steps. Without a canonical pattern, devs reach for client-side state (`x-data` carrying a `step` integer + accumulated values) and lose every URL-shareable property: the back button skips the whole flow, refresh resets, no one can paste a half-completed link.

**When to reach for it.**
- Multi-step forms where each step has 2–5 fields and a clear name.
- Users may want to bail mid-flow and resume (or share progress with a colleague).
- The data is non-sensitive — small enough to live in the query string.

**When NOT to.**
- Sensitive flows (KYC, payment details, passwords) — use session-backed state instead.
- Two-step forms with one decision — a single page with `th:if` for the second view is simpler.
- "Wizard" UX where each step's options depend on a previous step's heavy server compute — render the whole wizard server-side and skip the URL plumbing.

**Live demo:** http://localhost:8080/stepper

**Files:**
- `app/stepper.html` — three steps + done screen on one template; each step's "Next" registers a `context.actions['next-N']` handler that validates + redirects to the next step carrying state in the URL.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  _.state = { name: request.param('name') || '', email: request.param('email') || '' };

  function buildUrl(step) {
    var parts = ['step=' + step];
    if (_.state.name)  parts.push('name='  + encodeURIComponent(_.state.name));
    if (_.state.email) parts.push('email=' + encodeURIComponent(_.state.email));
    return '/stepper?' + parts.join('&');
  }

  context.actions['next-1'] = function () {
    _.state.name  = (request.param('name')  || '').trim();
    _.state.email = (request.param('email') || '').trim();
    if (_.state.name && _.state.email) context.redirect(buildUrl(2));
  };
</script>

<form method="post" action="/stepper">
  <input type="hidden" name="action" value="next-1"/>
  <input name="name"  th:value="state.name"/>
  <input name="email" th:value="state.email"/>
  <button type="submit">Next →</button>
</form>
```

**Make completed steps clickable.** The step indicator dots are links once a step is done — users fix a typo without losing later selections. Forward steps stay inert (`pointer-events: none`) so the indicator doesn't lie.

**Gotchas.** See `Skill.help('markup')`: #vals-on-submit-dropped (use hidden inputs for the action name, not `ka:vals`).
**Related.** form-binding (single-page form variant), bulk-import-wizard (data-shaped stepper).

### bulk-import-wizard
**Title:** Bulk import wizard (paste → review → import)  ·  **Category:** Inputs  ·  **Status:** stable

**Problem.** Bulk imports from a paste or CSV are a recurring need: invite teammates, add allowlist entries, seed config. The naive UX (paste into a textarea, hope it parses) silently drops bad rows. The over-engineered one (full inline-edit table with per-cell validation) is bigger than the problem. The sweet spot: paste → server parses + validates → user reviews with per-row badges + checkboxes → confirm.

**When to reach for it.**
- One-shot bulk operations (invite N users, allowlist N emails, import N config rows).
- The user is willing to fix a bad paste in the source — they don't expect to edit cells in the wizard.
- Validation is row-local (regex check, schema check) — no cross-row constraints beyond duplicate detection.

**When NOT to.**
- Multi-author / multi-session imports — use a job queue + status UI instead.
- Imports that require cell-level edits (typo fixes inline) — see `inline-edit` and `data-table` patterns.
- Tiny lists (<5 items) — a plain form with N rows is simpler.

**Live demo:** http://localhost:8080/bulk-import-wizard

**Files:**
- `app/bulk-import-wizard.html` — three steps + done screen on one template. Step 1: paste textarea. Step 2: parsed rows with per-row valid/duplicate/invalid badges and checkboxes. Step 3: import summary.
- Session-backed state (`session.bulkImportBody`) — body lives in session because URL would be too small for real paste sizes; cursor (step number) lives in the URL.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  context.actions['next-1'] = function () {
    var body = request.param('body') || '';
    context.init();                   // mint session if needed
    session.bulkImportBody = body;    // stash the paste
    context.redirect('/bulk-import-wizard?step=2');
  };

  // Step 2 reads from session — note: NO context.init() here, calling it
  // mid-flow rotates the session and orphans our writes.
  if (_.step === 2) {
    _.rows = parseAndValidate(session?.bulkImportBody || '');
  }

  context.actions['import'] = function () {
    var includeIdx = request.paramValues('include') || [];
    var rows = parseAndValidate(session?.bulkImportBody || '');
    var imported = rows.filter(r => includeIdx.indexOf(String(r.idx)) >= 0 && r.status === 'valid');
    session.bulkImportImported = imported.length;
    context.redirect('/bulk-import-wizard?step=3&done=1');
  };
</script>
```

**Session for the payload, URL for the cursor.** Step number is shareable (`?step=2` link works for a colleague reviewing your paste). Raw payload is per-user and potentially big — session is the right channel.

**Don't `context.init()` on every render.** Calling it unconditionally on a render that already has a valid session cookie rotates the session id and orphans your earlier writes. Call init() only inside action handlers that are about to write fresh state, and only when the data being written is new (the session.X read should be guarded `session?.X` so you can read without init).

**Composes other patterns.** Step indicator = `stepper`. Per-row badge = `status-badge`. Nothing-pasted state = `empty-state`. The wizard is the gravitational centre — it's a composite, not a primitive.

**Gotchas.** See `Skill.help('markup')`: #vals-on-submit-dropped (use hidden inputs for the action name), #shell-derives-crumbs.
**Related.** stepper (mechanism), status-badge (per-row tone), empty-state, file-upload (CSV variant feeds in here).

---

## Display

### data-table
**Title:** Data table (sort, paginate, multi-select, sticky header)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Admin / dev-tool screens are dominated by tables. Without a canonical pattern, every table reinvents sortable headers, page navigation, multi-select with select-all-on-page, and sticky headers — and gets the URL-state and bookmarkability wrong. Worse, naïve implementations load the whole dataset and slice in the template, which doesn't scale and isn't how a real backend works.

**When to reach for it.**
- Lists of >20 items where users need to sort, paginate, or search.
- Bulk operations (select multiple, then act).
- Pages whose state must be shareable (`?sort=email&dir=desc&page=2&q=alice`).

**When NOT to.**
- Hierarchical data — use `tree-view` or `nested-lookups`.
- <10 items with no sort — a plain `<ul>` is enough.

**Live demo:** http://localhost:8080/data-table

**Files (read these for the full picture):**
- `app/data-table.html` — template: reads `request.param('sort'/'dir'/'page'/'q')`, makes ONE call to the store, renders header + rows + pagination.
- `playground/App.java#DataTableStore` — the canonical server-side query contract: `dt.query(sort, dir, page, pageSize, q)` returns `{rows, total, pageCount, page, pageSize, sort, dir}` per call. The full dataset never crosses the JS bridge.
- — *no separate handler — sort/page navigation is plain `<a href>` full-page nav so URLs are shareable and Alpine state for selections survives via localStorage.*

**Minimal representative snippet:**
```html
<script ka:scope="global">
  var sortKey = (request.param('sort') || 'name').toLowerCase();
  var sortDir = (request.param('dir') == 'desc') ? 'desc' : 'asc';
  var page    = parseInt(request.param('page') || '1', 10);
  var q       = (request.param('q') || '').trim();
  _.result    = dt.query(sortKey, sortDir, page, 10, q);  // ← one call returns one page
</script>

<!-- Sort header -->
<a th:href="'/data-table?sort=' + col.key + '&dir=' + (result.sort == col.key && result.dir == 'asc' ? 'desc' : 'asc')">
  <span th:text="col.label"></span>
  <span th:if="result.sort == col.key" th:text="result.dir == 'asc' ? '▲' : '▼'"></span>
</a>

<!-- Pagination -->
<a th:each="n : Array.from({length: result.pageCount}, function(_, i){return i + 1;})"
   th:href="'/data-table?page=' + n" th:text="n"></a>
```

```java
// playground/App.java
public Map<String, Object> queryInternal(String sort, String dir, int page, int pageSize, String q) {
    // filter → sort → paginate → return { rows, total, pageCount, page, pageSize, sort, dir }
}
```

**Gotchas.** See `Skill.help('markup')`: #data-attr-quoted-key (must use `th:attr="'data-id': u.id"`, not bare `data-id` — JS-object hyphen), #scope-bare-vs-underscore (within a `ka:scope` script body, use `var x` for locals; `_.x` is for export to template attrs).
**Related.** pagination, empty-state, status-badge, inline-edit, tree-view.

---

### tree-view *(stub — retrofit of /tree)*
### nested-lookups *(stub — retrofit of /lookups)*
### json-tree *(stub — retrofit of /json-tree)*
### diff *(stub — retrofit of /diff)*
### code-view *(stub — retrofit of /code-view)*
### kpi-card
**Title:** KPI card (sparkline + trend)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Dashboard tiles drift into chart-library territory the moment you want a trend visualisation. A four-segment sparkline does not warrant a 60-KB charting dep — and a chart library brings dark-mode theming, canvas/SVG rendering, and tooltip positioning concerns the dashboard never asked for. The shape of a KPI tile is small enough to render entirely server-side as static SVG.

**When to reach for it.**
- Dashboard tile: label + current value + recent trend.
- The trend matters for direction (up/down) and shape (volatile/stable), not exact values — a sparkline reads shape, not values.
- The series is short (5–100 points). Above ~200 a sparkline becomes noise; reach for a real chart.

**When NOT to.**
- Interactive charts with hover tooltips, axis labels, zoom — use a charting library and own the asset weight.
- Real-time updates more often than ~once per second — re-rendering server-side every tick is wasteful; switch to a canvas-based primitive.

**Live demo:** http://localhost:8080/kpi-card (toggle `?period=7d|30d|90d`)

**Files:**
- `app/kpi-card.html` — four tiles in a grid, period switcher, server-side sparkline math (polyline points in a 100×30 viewBox).

**Minimal representative snippet:**
```html
<script ka:scope="global">
  function pointsFor(series) {
    var min = Math.min.apply(null, series);
    var max = Math.max.apply(null, series);
    var range = Math.max(0.0001, max - min);
    return series.map(function (v, i) {
      var x = (i / Math.max(1, series.length - 1)) * 100;
      var y = 28 - ((v - min) / range) * 26;
      return x.toFixed(2) + ',' + y.toFixed(2);
    }).join(' ');
  }
  _.points = pointsFor(_.series);
</script>

<svg viewBox="0 0 100 30" preserveAspectRatio="none" class="w-full h-10">
  <polyline fill="none" stroke="#6366F1" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round"
            th:attr="'points': points"/>
</svg>
```

**Invert "down-is-good" metrics at the data layer.** Churn, error rate, p95 latency — mark them `invert: true` server-side; the trend chip then renders emerald on a drop. Don't push direction-of-good into the template's tone cascade.

**Gotchas.** See `Skill.help('markup')`: #expressions-only-in-th-and-ka (must `th:attr` to set `points`, not bare `points="…"`).
**Related.** status-badge (semantic tones reused on the trend chip), data-table, empty-state.
### status-badge
**Title:** Status badge (semantic colour family)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Status pills are everywhere (active / suspended / trial / failed / pending) and each one ad-hoc-picks a colour. Without a documented semantic mapping, "trial" is amber on one page and yellow on another; "pending" is grey here and brand-blue there. Users learn the meaning by colour faster than by reading — inconsistency breaks that.

**When to reach for it.**
- Any inline status indicator: row column, KPI sub-line, drawer header.
- A toast or alert needs the same semantic colour as its severity.

**When NOT to.**
- Long-form messaging (use a banner or alert panel).
- Hierarchy/priority badges (use a separate scale — don't reuse semantic palette).

**Live demo:** http://localhost:8080/status-badge

**Files:**
- `app/status-badge.html` — the 5 tones × 2 variants × 4 sizes matrix, with the canonical colour mapping inline.
- — *atomic CSS — no fragment, no Alpine. Embed inline at the call site.*

**Semantic mapping:**
```
success → emerald   (active, ok, online, completed)
warning → amber     (trial, pending review, degraded)
error   → coral     (failed, suspended, offline)
info    → brand     (notice, queued, beta)
pending → ink       (invited, draft, idle, processing)
```

**Minimal representative snippet:**
```html
<!-- Pill -->
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-medium
             bg-emerald-50 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300">
  active
</span>

<!-- Pill + dot -->
<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-medium
             bg-emerald-50 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300">
  <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> active
</span>
```

**Gotchas.** See `Skill.help('markup')`: #th-if-empty-string-truthy (an empty status string still renders the pill chrome).
**Related.** notification-stack (uses same tones), kpi-card.
### timeline
**Title:** Timeline (activity feed, bucketed)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Activity feeds are nominally simple — a list of events — but get awkward when timestamps span hours, days, and weeks. A flat list of absolute timestamps reads as noise; "3 hours ago / 2 days ago / last week" is what users actually parse. Without a canonical pattern, each feed reinvents bucketing logic and ends up with inconsistent thresholds across the app.

**When to reach for it.**
- Activity feed sorted recent-first across a non-trivial time span.
- Events carry a category that benefits from visual tagging (user / deploy / comment / alert).
- The feed is read-only; interactivity belongs in a separate detail view.

**When NOT to.**
- Auditable log surfaces where the absolute timestamp matters per row — use a table with sortable timestamp column.
- Real-time stream where every line lands within a single bucket — use `log-viewer`.

**Live demo:** http://localhost:8080/timeline (filter via `?cat=user|deploy|comment|alert`)

**Files:**
- `app/timeline.html` — server-rendered list with relative-time bucketing (Today / Yesterday / Last 7 days / Older) and URL-driven category filter.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  function bucketFor(m) {
    if (m < 24 * 60)       return 'Today';
    if (m < 2 * 24 * 60)   return 'Yesterday';
    if (m < 7 * 24 * 60)   return 'Last 7 days';
    return 'Older';
  }
  function relLabel(m) {
    if (m < 60)            return m + ' min ago';
    if (m < 24 * 60)       return Math.round(m / 60) + ' h ago';
    if (m < 14 * 24 * 60)  return Math.round(m / (24 * 60)) + ' d ago';
    return Math.round(m / (7 * 24 * 60)) + ' w ago';
  }
  _.groups = bucketAndGroup(events, _.cat);
</script>

<div th:each="g : groups">
  <h2 th:text="g.bucket">Today</h2>
  <ul>
    <li th:each="e : g.items">
      <span th:text="e.actor"></span>
      <span th:text="e.verb"></span>
      <span th:text="e.obj"></span>
      <span th:title="e.absLbl" th:text="e.relLbl">3 h ago</span>
    </li>
  </ul>
</div>
```

**Bucket relatively, keep absolute one hover away.** "3 days ago" answers "is this recent?" — the question users actually ask. The exact timestamp goes in the `title` attribute for the rare case precision matters. Don't show both in the row; the noise erodes the bucket's value.

**Filter on the URL, not on Alpine.** Server-side filtering means the URL is shareable and refresh-safe. For very long feeds, page through buckets server-side (combine with `pagination`).

**Gotchas.** See `Skill.help('markup')`: #expressions-only-in-th-and-ka (use `th:attr` for the dynamic SVG `d` path).
**Related.** log-viewer (real-time variant), pagination (when the feed grows), status-badge.
### empty-state
**Title:** Empty / loading / error states  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Every data surface has three sibling states: still-loading (skeleton), loaded-but-empty (zero-state), and load-failed (error). Without a canonical triad, each page invents its own loading spinner, drops zero-state entirely, or shows a useless "Something went wrong" with no diagnostics or retry.

**When to reach for it.**
- Any page that fetches or renders user-scoped data.
- The data may be empty after a successful load (new account, all-completed list, etc.).
- Errors must communicate cause + retry path.

**When NOT to.**
- Static pages with no data dependency.
- Brief flashes (<200ms) where a skeleton's pulse is more distracting than the actual load.

**Live demo:** http://localhost:8080/empty-state (toggle `?state=live|loading|empty|error`)

**Files:**
- `app/empty-state.html` — the three states inline, gated by `?state=`. Pure CSS — no Alpine, no JS.
- — *no handler, no Java wiring; consumers read their own loading/empty/error flags and pick a `th:if` branch.*

**Minimal representative snippet:**
```html
<!-- Skeleton -->
<ul class="divide-y animate-pulse">
  <li th:each="i : [1, 2, 3]" class="py-3 flex gap-3">
    <div class="w-8 h-8 rounded-full bg-ink-200 dark:bg-ink-800"></div>
    <div class="flex-1 space-y-1.5">
      <div class="h-3 w-1/3 rounded bg-ink-200 dark:bg-ink-800"></div>
      <div class="h-2.5 w-1/2 rounded bg-ink-100 dark:bg-ink-800/60"></div>
    </div>
  </li>
</ul>

<!-- Zero-state -->
<div class="p-12 text-center">
  <h3 class="text-lg font-semibold">No team members yet</h3>
  <p class="mt-1 text-sm text-ink-600">Invite teammates to collaborate.</p>
  <button class="mt-5 px-4 py-2 bg-brand-600 text-white rounded-md">Invite a teammate</button>
</div>

<!-- Error -->
<div class="p-8 border border-coral-300/40 rounded-xl">
  <h3 class="font-semibold">Couldn't load team members</h3>
  <p class="text-sm text-ink-600">Server returned 500. <a href="?retry">Retry</a></p>
</div>
```

**Decision rule.** State is a function of `(loadingP, errorP, dataIsEmpty)`. Don't conflate them: a 200-OK-with-empty array is NOT the same as still-loading.

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (skeleton must not flash before mount).
**Related.** data-table, log-viewer, timeline.

### carousel
**Title:** Image carousel (strip + lightbox)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** A "browse a sequence of images" surface is everywhere — agent run screenshots, report galleries, audit evidence, file-upload previews — and each page reinvents the same plumbing: horizontal strip with lazy thumbs, click-to-lightbox, arrow-key nav, escape-to-close. Without a canonical primitive the keyboard contract drifts (some pages bind to the strip, some to the lightbox; Home/End is hit-or-miss), and the lightbox chrome re-implements the modal backdrop / escape / click-outside dance every time.

**When to reach for it.**
- A sequence of images users skim before drilling into one (screenshots, gallery, file previews).
- The set is bounded — tens to low hundreds. Above that, paginate the strip server-side.
- Captions and a small corner overlay (iteration number, timestamp) are useful but not the focus.

**When NOT to.**
- Single hero image — wrap in a plain `<img>`; the lightbox adds noise.
- Pan/zoom-heavy image viewing (image-annotator territory) — out of scope here.
- Auto-advancing slideshow / hero rotators — this primitive is user-driven only.

**Live demo:** http://localhost:8080/carousel

**Files:**
- `app/carousel.html` — JSON-island items, Alpine factory drives both surfaces, lightbox chrome inline.
- — *atomic; no API handler, no Java wiring. Consumers feed items through the JSON island.*

**Minimal representative snippet:**
```html
<script type="application/json" id="carousel-data" th:utext="JSON.stringify(items)">[]</script>

<div x-data="carousel()"
     @keydown.window.arrow-left.prevent="if (open) prev()"
     @keydown.window.arrow-right.prevent="if (open) next()"
     @keydown.window.home.prevent="if (open) first()"
     @keydown.window.end.prevent="if (open) last()">

  <!-- Strip -->
  <div class="flex gap-2 overflow-x-auto">
    <template x-for="(item, idx) in items" :key="idx">
      <button @click="openAt(idx)" class="relative shrink-0">
        <img :src="item.src" loading="lazy" class="h-28 w-44 object-cover"/>
        <span x-show="item.meta" x-text="item.meta" class="absolute top-1 left-1 ..."></span>
      </button>
    </template>
  </div>

  <!-- Lightbox: outer fades opacity, inner scales — same chrome as the `modal` primitive. -->
  <div x-show="open" x-cloak class="fixed inset-0 z-50 bg-ink-950/85 flex items-center justify-center"
       @click.self="close()" @keydown.escape.window="close()">
    <img :src="current && current.src"/>
  </div>
</div>
```

**One factory, two surfaces.** Strip click and lightbox keys share the same `active` integer. Split into two factories and you'll drift the moment a new key binding or thumb highlight lands.

**Bind keys to the window, gate on `open`.** Arrow keys are useless when the strip is closed and would fight with native scroll. `@keydown.window` handlers no-op unless the lightbox is open, so the page's keyboard surface stays clean.

**Items shape (passed via JSON island):** `[{ src, caption?, meta? }]`. `meta` is the small-text corner overlay (iteration number, timestamp) — optional, hidden when absent.

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (lightbox must be `x-cloak`), #alpine-double-quotes-break-thymeleaf (factory in a separate `<script>` block, not inline), #cdn-script-order.
**Related.** modal (lightbox chrome shape), agent-transcript (apex consumer — screenshot strip), file-upload, notification-stack.

### pagination
**Title:** Pagination (numbered + load-more)  ·  **Category:** Display  ·  **Status:** stable

**Problem.** Lists of >20 items need a pagination contract. Without a canonical pattern, every page invents page-number windowing (where do the ellipses go?), Prev/Next disabled states, and load-more semantics — and gets URL-shareability wrong (back button skips the whole feed, refresh resets to page 1). The mental model is one decision (numbered vs load-more) and one slot-array helper that both variants share.

**When to reach for it.**
- Numbered — random-access lists. Users jump to page N, share the URL with a colleague who needs the same slice.
- Load-more — serially-consumed lists (feed, comments, recent activity). Users scan forward; reaching back is rare.
- Either way: URL is the source of truth (`?page=N` or `?loaded=K`).

**When NOT to.**
- <20 items — render them all.
- Infinite scroll with scroll-position-as-state — that's a different (worse) UX contract; only use it when the page IS the feed (Twitter, search results).

**Live demo:** http://localhost:8080/pagination

**Files:**
- `app/pagination.html` — both variants on one page, server-side slot windowing helper, fake 73-item dataset.

**Minimal representative snippet:**
```html
<script ka:scope="global">
  var page = parseInt(request.param('page') || '1', 10);
  var pageCount = Math.ceil(total / pageSize);

  // Window: first + last + ±2 around active, ellipsis where gaps span >1.
  _.slots = (function () {
    var out = [], seen = {};
    function add(n) { if (!seen[n]) { out.push(n); seen[n] = true; } }
    add(1);
    for (var d = -2; d <= 2; d++) {
      var n = page + d;
      if (n > 1 && n < pageCount) add(n);
    }
    if (pageCount > 1) add(pageCount);
    out.sort((a, b) => a - b);
    var f = [];
    for (var i = 0; i < out.length; i++) {
      if (i > 0 && out[i] - out[i - 1] > 1) f.push('…');
      f.push(out[i]);
    }
    return f;
  })();
</script>

<th:block th:each="slot : slots">
  <span th:if="slot == '…'" th:text="slot">…</span>
  <a th:if="slot != '…' && slot != page" th:href="'?page=' + slot" th:text="slot">1</a>
  <span th:if="slot != '…' && slot == page" aria-current="page" th:text="slot">1</span>
</th:block>
```

**Server is the source of truth.** Don't compute slots in Alpine — state resets on every htmx swap and the window flickers back to page-1 shape. Compute server-side, render the `<a>` trail, let URL nav do the rest.

**Gotchas.** See `Skill.help('markup')`: #scope-bare-vs-underscore (use `_.slots = …` then bare `slots` in `th:each`).
**Related.** data-table (adopts this pagination), empty-state, log-viewer.

---

## Overlays

### modal-as-url
**Title:** Modal-as-URL (htmx-driven endpoint)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** Inline modals carry server values via `data-*` round-trips and re-implement open/close logic per page. The cleaner mental model: a modal is just a URL — fetched on demand, returns bare HTML, swapped into the modal slot. State arrives via URL params, no `ka:data-mirror` needed for the modal body, and the chrome is intentionally duplicated per endpoint (per Server-Render Composition principle).

**When to reach for it.**
- Modals whose contents depend on server state (Edit a row, Approve an action).
- Workflows where the modal might need its own validation/error path on submit.
- Any case where deep-linking the modal would be useful (`?openEdit=u-alice`).

**When NOT to.**
- Tiny confirms (yes/no, no payload) — use a plain modal with `severity="danger"`.
- Modal whose state is purely client-side (theme picker, etc.).

**Live demo:** http://localhost:8080/modal-as-url

**Files:**
- `app/modal-as-url.html` — list page with row actions that fetch modal endpoints
- `app/modal-as-url-edit.html` — bare modal body for the Edit endpoint
- `app/modal-as-url-approve.html` — bare modal body for Approve (with stale-row guard)
- `app/modal-as-url-delete.html` — bare modal body for Delete
- `App.java#MockStore` — `SimpleObject` exposing the mock data + mutation methods
- `App.java#rawPaths(...)` — opts the modal endpoints out of the layout shell

**Minimal representative snippet:**
```html
<!-- list page: row's Edit button fetches the modal endpoint -->
<button hx-get="/modal-as-url-edit?id=u-alice"
        hx-target="#modal-slot"
        hx-swap="innerHTML"
        class="text-brand-700">Edit</button>
<div id="modal-slot"></div>

<!-- modal-as-url-edit.html: bare body, NO layout shell -->
<div class="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/50"
     @click.self="document.getElementById('modal-slot').innerHTML = ''"
     @keydown.escape.window="document.getElementById('modal-slot').innerHTML = ''">
  <form hx-post="/modal-as-url-edit"
        hx-target="#main-content"
        class="bg-white p-6 rounded-xl">
    <input type="hidden" name="id" th:value="user.id"/>
    <!-- ...form fields driven by `user.*` server data... -->
    <button type="submit">Save</button>
  </form>
</div>
```

**Gotchas.** See `Skill.help('markup')`: #hx-request-shell-bypass, #rawpaths-opt-in, #simpleobject-jskeys.
**Related.** modal, drawer, palette.

---

### modal *(stub — retrofit of /modal)*
### drawer *(stub)*
### dropdown-menu
**Title:** Dropdown menu (action menu, row kebab, split button)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** Action menus appear everywhere (header buttons, table-row "more" kebabs, primary-CTA + alternates). Without a documented Alpine shape, each one invents its own click-outside / escape / aria-expanded plumbing and they drift apart. Standardising on `{ open: false }` + `@click.outside` + `@keydown.escape.window` gives a 3-line contract every dropdown shares.

**When to reach for it.**
- Header / row / inline buttons that fan out to 3-7 short actions.
- One-line items (label + optional icon + optional kbd hint).

**When NOT to.**
- Multi-section grouped menus → `mega-menu-click-pinned`.
- Rich content beyond labels (forms, descriptions) → `popover`.
- Quick disambiguation between hover-revealed columns → `mega-menu-hover`.

**Live demo:** http://localhost:8080/dropdown-menu

**Files:**
- `app/dropdown-menu.html` — three variants on one page (action menu, row kebab, split button).
- — *no fragment / no Java; atomic enough to inline.*

**The 3-line contract:**
```html
<div x-data="{ open: false }"
     @click.outside="open = false"
     @keydown.escape.window="open = false"
     class="relative">
  <button @click="open = !open" :aria-expanded="open">…</button>
  <div x-show="open" x-cloak class="absolute …">
    <button @click="open = false">Action 1</button>
    <button @click="open = false">Action 2</button>
  </div>
</div>
```

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (panel must be `x-cloak` to avoid flash on first render).
**Related.** mega-menu-click-pinned, popover, palette.
### mega-menu-hover
**Title:** Mega-menu (hover, pure CSS)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** Multi-column flyout menus that reveal on hover are a staple of marketing sites and developer tool rails. Naïve implementations need Alpine state for open/close — but for hover-driven flyouts that's overkill. The CSS `group` + `group-hover:visible` combo gives the same result with zero JS.

**When to reach for it.**
- The rail's category flyouts (used in `app/layout.html`).
- Marketing-site product / resources mega-menus (mirrors `karatelabs-site/src/_includes/partials/navbar.njk`).
- Quick scan-and-jump browsing where users don't want to click to dismiss.

**When NOT to.**
- Touch-first apps — hover doesn't fire on tap (the component's CSS includes `@media (hover: none)` to hide on touch).
- Menus where users want to dwell and read — use `mega-menu-click-pinned` so the menu doesn't close on mouse-out.
- Menus deeper than two-column grids — flyouts get hard to scan.

**Live demo:** http://localhost:8080/ (rail's category icons — hover any of them).

**Files:**
- `app/components/mega-menu.html :: hover(menu)` — fragment.
- `app/layout.html` — uses it for the rail's category flyouts.

**Minimal representative snippet:**
```html
<div class="relative group">
  <button>Product ▾</button>
  <div class="absolute top-full mt-2 w-[720px] opacity-0 invisible
              group-hover:opacity-100 group-hover:visible
              transition-all duration-200">
    <div class="grid grid-cols-3 gap-6 p-6">
      <div th:each="section : menu.sections">
        <p class="text-[10px] uppercase">…</p>
        <a th:each="item : section.items" th:href="item.href">
          <p th:text="item.label"></p>
          <p th:text="item.hint"></p>
        </a>
      </div>
    </div>
  </div>
</div>
```

**Gotchas.** See `Skill.help('markup')`: (none specific — pure CSS, no Alpine).
**Related.** mega-menu-click-pinned (the click counterpart), dropdown-menu, popover.

---

### mega-menu-click-pinned
**Title:** Mega-menu (click-pinned, Alpine)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** When users want to *read* a menu carefully — comparing options, scanning descriptions — hover-only menus close on incidental mouse-out. Click-to-pin keeps the menu open until the user clicks outside or presses escape. This is the right pattern for top-bar Catalog / Docs menus where each item warrants reading.

**When to reach for it.**
- Top-bar menus on apps with rich content per row.
- Multi-section menus where one click-out keeps the menu visible across all of them.
- Accessibility-sensitive surfaces — click-driven works on touch and via keyboard nav, hover doesn't.

**When NOT to.**
- Quick scan-and-jump (e.g. rail icons) — `mega-menu-hover` is faster.
- Single-column simple menus — `dropdown-menu` is lighter.

**Live demo:** http://localhost:8080/ (top-bar Catalog and Docs menus).

**Files:**
- `app/components/mega-menu.html :: clickPinned(menu)` — fragment.
- `app/layout.html` — uses it for the topbar's Catalog and Docs menus.

**Minimal representative snippet:**
```html
<div x-data="{ open: false }"
     @click.outside="open = false"
     @keydown.escape.window="open = false"
     class="relative">
  <button @click="open = !open" :aria-expanded="open">Catalog ▾</button>
  <div x-show="open" x-cloak class="absolute top-full mt-2 w-[640px] grid grid-cols-3 gap-6 p-6 …">
    …
  </div>
</div>
```

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker, #alpine-double-quotes-break-thymeleaf.
**Related.** mega-menu-hover (the hover counterpart), dropdown-menu, palette.
### palette *(stub — retrofit of /palette)*
### tooltip
**Title:** Tooltip (pure CSS, drop-in)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** Icon-only buttons need labels visible on hover/focus for accessibility and discoverability. Hand-rolling per-icon tooltips drifts on positioning, dark-mode contrast, and touch-device behaviour. A single component with `data-tip="…"` + four placement classes covers every common case.

**When to reach for it.**
- Icon buttons (rail items, theme toggle, copy-to-clipboard).
- Any element whose meaning isn't obvious from its visual.
- Small label content (≤2 words ideal, ≤80 chars max).

**When NOT to.**
- Rich content (links, multi-line text, forms) — use `popover`.
- Tap-targets on touch devices — the CSS hides tooltips at `(hover: none)` automatically.

**Live demo:** http://localhost:8080/ (rail icons all have tooltips).

**Files:**
- `app/components/tooltip.html :: styles` — CSS-only fragment dropped into the layout shell.
- `app/layout.html` — includes the styles + uses `.ka-tip` on rail items.

**API:**
```html
<a class="ka-tip" data-tip="Search">           <!-- right placement (default) -->
<button class="ka-tip ka-tip-top" data-tip="Save">
<span class="ka-tip ka-tip-bottom" data-tip="…">
<a class="ka-tip ka-tip-left" data-tip="…">
```

**Gotchas.** See `Skill.help('markup')`: (none — pure CSS, automatic touch-suppression via `@media (hover: none)`).
**Related.** popover, dropdown-menu, status-badge.
### popover
**Title:** Popover (click-driven rich overlay)  ·  **Category:** Overlays  ·  **Status:** stable

**Problem.** Inline help, mini-forms (rename, quick-edit, add-tag), or quick "what is this" explanations need an overlay richer than a tooltip but lighter than a modal. Without a documented pattern, devs reach for full modals (too heavy, breaks flow) or invent ad-hoc CSS poppers (drift on positioning, focus trap, escape handling).

**When to reach for it.**
- Inline help next to a label (heading + paragraph + secondary action).
- Mini-forms attached to a row (rename, quick-edit, add label).
- "Show details" overlays with multiple paragraphs.

**When NOT to.**
- Label-only hint → `tooltip` (lighter, hover-driven, no Alpine).
- List of one-line actions → `dropdown-menu`.
- Multi-step or destructive flow → `modal`.

**Live demo:** http://localhost:8080/popover

**Files:**
- `app/popover.html` — two variants on one page (info popover, form popover with rename mini-form).
- — *no fragment / no Java; same 3-line Alpine shape as dropdown-menu, just with rich content.*

**Minimal representative snippet:**
```html
<div x-data="{ open: false }"
     @click.outside="open = false"
     @keydown.escape.window="open = false"
     class="relative">
  <button @click="open = !open" :aria-expanded="open">?</button>
  <div x-show="open" x-cloak class="absolute mt-2 w-72 …">
    <h3>What's a run prefix?</h3>
    <p>A short string prepended to every test run's name…</p>
    <a href="/app-setup.md">Learn more →</a>
  </div>
</div>
```

**Decision rule.** Tooltip = hover, label-only. Popover = click, rich content. Dropdown-menu = click, list of one-liners. Mega-menu = multi-section. Pick by *what goes inside*, not by visual size.

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker, #alpine-double-quotes-break-thymeleaf.
**Related.** tooltip, dropdown-menu, modal.
---

## Realtime

> Roster: **live-sse-rest** (the `$live` primitive), **log-viewer**, **notification-stack**, **agent-transcript** (stub), **presence** (stub), **progress-stream** (stub). The category groups *every server-pushed UI*; pick by what's being pushed: one stream of arbitrary events (`$live`), an append-only log (`log-viewer`), a turn-by-turn agent run (`agent-transcript`), transient toasts (`notification-stack`), who-is-active (`presence`), or a phased job lifecycle (`progress-stream`).

### live-sse-rest *(stub — retrofit of /live)*

`$live` Alpine magic for SSE-with-REST-history reconciliation. See `Skill.help('markup')` §Live SSE+REST Reconciliation. Working demo at `/live`.

### log-viewer
**Title:** Log viewer (live SSE + filter + follow-tail)  ·  **Category:** Realtime  ·  **Status:** stable

**Problem.** Streaming logs into a browser is a four-problem composite: open SSE, render rows in a fixed-height viewport, filter (level + free-text) without remounting the stream, and follow-tail with a graceful pause when the user scrolls up. Without a canonical wiring, each page reinvents the buffer/dedup/scroll plumbing — and gets the seam between REST history and live SSE wrong.

**When to reach for it.**
- Real-time log surface (deploy progress, agent run output, audit feed).
- The stream is append-only and ordered — events have an `id` or a `ts+source` dedup key.
- Users want both context (history) and live (current).

**When NOT to.**
- Bursty data that isn't really logs — use `notification-stack` for transient toasts.
- Logs over hours where users mostly search-and-pin specific lines — page through them server-side instead.

**Live demo:** http://localhost:8080/log-viewer

**Files:**
- `app/log-viewer.html` — `$live` stream + level-filter chips + free-text filter + follow-tail toggle. Pure client-side filtering for instant flips.
- `app/api/logs-history.js` — REST seed returning `JSON.stringify(logs.messages)`.
- `App.java#LogStore` — backing store: daemon thread emits a synthetic log line every ~700–2000ms, SSE subscribers receive every line, REST history exposes the ring buffer.
- App.java `sseHandler` routes `/api/logs/sse` to `LOGS.subscribe(conn)`.

**Minimal representative snippet:**
```html
<div x-data="{
       lines: $live('/api/logs/sse', { initial: '/api/logs-history', dedup: 'id' }),
       level: 'all', search: '', follow: true,
       get visible() {
         const q = this.search.toLowerCase().trim();
         return this.lines.filter(l =>
           (this.level === 'all' || l.level === this.level) &&
           (!q || l.source.toLowerCase().includes(q) || l.message.toLowerCase().includes(q))
         );
       }
     }"
     x-init="$watch('lines.length', () => { if (follow) $nextTick(() => $refs.scroller.scrollTop = $refs.scroller.scrollHeight) })">
  <template x-for="l in visible" :key="l.id">…</template>
</div>
```

**Filter on the client, dedup on the server.** Level + search filters are local — flips should feel immediate. Dedup (`dedup: 'id'`) lives server-side because the seam between REST history and the first SSE events is the only place duplicates can arise.

**Follow-tail = scroll on every new line.** The toggle is an explicit checkbox here; a polished variant detects "user scrolled up" and auto-pauses, then re-enables when they return to the bottom. The pending-count badge tells them how far behind they are.

**Gotchas.** See `Skill.help('markup')`: #cdn-script-order (`$live` magic requires app.js BEFORE Alpine).
**Related.** live-sse-rest (the `$live` magic), notification-stack, empty-state, agent-transcript.

### notification-stack
**Title:** Notification stack (toast queue)  ·  **Category:** Realtime  ·  **Status:** stable

**Problem.** Long-running flows produce multiple feedback events (saved, then synced, then notified). A single-toast pattern overwrites the previous message; users miss them. A queue lets several stack up, each with its own severity, auto-dismiss timer, and manual close. The hardest part is keeping the API clean across the three lanes (server, htmx, Alpine, vanilla JS) — solved by exposing a `'notify'` custom event.

**When to reach for it.**
- Any app with multi-step flows where each step produces feedback.
- Server-side handlers (POST → flash) that should also emit a transient toast — combine with `ka:dispatch`.
- When users may dismiss faster than the auto-timeout (sticky errors, in-progress jobs).

**When NOT to.**
- Single-shot success/error after a form submit — use a flash message in the page header instead. Toasts are for transient events that don't bind to a region.
- Modal-style "are you sure?" — use `modal` with severity instead.

**Live demo:** http://localhost:8080/notification-stack

**Files:**
- `app/notification-stack.html` — host element with the Alpine factory + a triggers panel demonstrating the event-driven API.
- — *no fragment / no Java wiring; consumers fire `dispatchEvent(new CustomEvent('notify', {detail: {...}}))` from anywhere.*

**API:**
```js
window.dispatchEvent(new CustomEvent('notify', { detail: {
  message:   'Saved successfully.',
  severity:  'success',  // success | warning | error | info | pending
  sticky:    false,      // true = stay until manually closed
  timeoutMs: 4000        // auto-dismiss delay (default 4s)
}}));
```

**Three lanes:**
```html
<!-- Vanilla JS -->
<button onclick="window.dispatchEvent(new CustomEvent('notify', {detail: {message: 'Saved.', severity: 'success'}}))">Save</button>

<!-- Server-dispatched via ka:dispatch — triggers the event after htmx swap -->
<button ka:dispatch="notify" ka:vals="message:'Saved.', severity:'success'">Save</button>

<!-- Alpine -->
<button @click="$dispatch('notify', {message: 'Saved.', severity: 'success'})">Save</button>
```

**Gotchas.** See `Skill.help('markup')`: #alpine-x-cloak-flicker (host element must be `x-cloak` to avoid flash on first mount).
**Related.** status-badge (shares the tone family), modal, palette, agent-transcript.

### agent-transcript *(stub)*

**Title:** Agent transcript (LLM session view, live + autonomous) · **Category:** Realtime · **Status:** stub

A composite for an LLM-driven agent session — same UI handles two modes:
- **Autonomous** — internal LLM loop produces JS → tool runner executes → result feeds back. Many turns until terminal.
- **Live** — human / external program sends one JS expression per turn via a proxy. No LLM inference step.

**Key invariants** (from the design doc):
- Generic event envelope `{kind, status, payload, ts, parentId?, severity?}` + per-kind renderer registry. Forward-compatible — unknown `kind` falls back to a generic event tile.
- Three named in-flight states surfaced as phase pills: `request-in-flight` → `response-in-flight` → `tool-call-in-flight` → terminal (`done` / `error` / `warn`). No token streaming in v1.
- Live = degenerate turn (no LLM phase). Same card chrome as autonomous; mode-specific bits live in the shell, not the primitive.
- Sub-events (`screenshot`, `console`, `hint`) carry `parentId` and render as peer subsections of the turn (flat-per-turn default; causal tree available as a debug toggle).
- Severity (`info` / `warn` / `error`) is **server-emitted** — single source of truth.
- Sticky **Task** header + sticky **Result/Controls** footer at viewport edges. Body scrolls between.
- Footer is state-driven — `Pause/Stop` while running, multi-line compose + `Resume` while paused, result + `ZIP/Report/Delete` while stopped.
- Manual scroll. Explicit Follow-tail toggle + Expand-all / Collapse-all in the header. Never disrupt the user's reading position when new turns arrive.

**Composes:**
- `splitter` — column resize (transcript ↔ browser); screenshot strip ↔ VNC vertical split.
- `carousel` — screenshot thumbnail strip + click-to-lightbox.
- `live-sse-rest` — `$live('/api/sessions/{id}/sse', { initial: '/api/sessions/{id}/events' })`.
- `log-viewer` primitives — filter chips + follow-tail + level tones inside per-turn Console subsection.
- `status-badge` — severity tones (info / warn / error).
- `modal` — lightbox overlay.

**Apex consumer:** karate-agent.

**Gotchas (when implemented).** See `Skill.help('markup')`: #cdn-script-order ($live magic requires app.js before Alpine), #shell-derives-crumbs.
**Related.** live-sse-rest, log-viewer, notification-stack, splitter, carousel, status-badge.

### presence *(stub — future)*

A tiny realtime primitive: list / dot indicator of currently-active users on a shared resource. Consumes `$live` for join/leave events; renders an avatar stack + activity hint. Useful in collaborative editors, support chats, deploy dashboards. **No implementation yet.**

### progress-stream *(stub — future)*

A composite for long-running jobs that emit phased status (queued → running → finalising → done). Consumes `$live` for phase-change events; renders a phase-bar + a per-phase log block. Replaces ad-hoc "loading…" surfaces for any job whose backend can emit progress. **No implementation yet.**

---

## Editors

### codemirror-editor *(stub — retrofit of /editor)*

CodeMirror 6 from esm.sh, JS API completions, round-trip via `ka:data-mirror`.

### karate-language *(stub — same /editor page)*

`StreamLanguage` Karate/Gherkin tokeniser + step autocomplete. Demonstrates a custom-language adapter without writing a Lezer grammar.

### json-editor
**Title:** JSON editor (CM6 + live validation)  ·  **Category:** Editors  ·  **Status:** stable

**Problem.** Plain `<textarea>` for JSON is hostile: no indenting, no bracket matching, no syntax cues, no validity feedback. The full `/editor` page with language switching is overkill when JSON is the only language. A focused JSON editor with live parse validation + format/minify is the right size.

**When to reach for it.**
- The user edits a JSON payload (config, webhook body, raw policy).
- The payload is small-to-medium (~100 KB) — bigger and you want streaming editors / virtual scroll.
- Validity matters at edit time — users want to know "is this parseable?" before submitting.

**When NOT to.**
- Read-only JSON view — use `json-tree`.
- General-purpose editor (multiple languages, custom completions) — use `codemirror-editor`.
- Very large JSON payloads — render the tree by default with an "edit raw" toggle to swap in the editor on demand.

**Live demo:** http://localhost:8080/json-editor

**Files:**
- `app/json-editor.html` — CM6 with `lang-json`, linter (driving validity badge + gutter markers), Format / Minify / Save buttons. No backing store — refresh resets to the default doc.

**Minimal representative snippet:**
```html
<script type="module">
  import { EditorView, basicSetup } from "codemirror";
  import { json }                   from "@codemirror/lang-json";
  import { linter, lintGutter }     from "@codemirror/lint";

  function jsonLinter(view) {
    const text = view.state.doc.toString();
    try { JSON.parse(text); return []; }
    catch (e) {
      const pos = (/position (\d+)/.exec(e.message) || [, 0])[1];
      return [{ from: +pos, to: +pos + 1, severity: 'error', message: e.message }];
    }
  }

  new EditorView({
    state: EditorState.create({
      doc: initialDoc,
      extensions: [basicSetup, json(), linter(jsonLinter), lintGutter()]
    }),
    parent: host
  });
</script>
```

**Validate on the client, then again on the server.** Live validity feedback is for UX; `JSON.parse(request.param('body'))` on POST is the contract. Never trust a client "valid" flag.

**Format / minify operate on parseable docs only.** The buttons disable when invalid (read `JSON.parse`, write `JSON.stringify`). This is the right behaviour: format-on-invalid would silently swallow part of the document.

**Gotchas.** See `Skill.help('markup')`: #cdn-script-order (module scripts using esm.sh imports must come AFTER the importmap, but the importmap can sit anywhere before them).
**Related.** codemirror-editor (full version), json-tree (read-only sibling), form-binding (typical embedding).

Lightweight editable variant of `json-tree`. Type-aware editing, validation, save-on-blur.

---

## Status

| Item | State | Notes |
|---|---|---|
| Catalog scaffold + 3 anchor entries | ✅ | This file |
| Layout/breadcrumb/layout-shell | ✅ | Anchored; full entries pending |
| Stubbed patterns | ⏳ | Stubs above; fill as each lands |
| Retrofits | ⏳ | Existing pages get their entries when retrofit lands |

