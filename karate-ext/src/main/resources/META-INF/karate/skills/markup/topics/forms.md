# Forms

> `ka:data` for complex form state with Alpine binding. Plain `<form>` with `ka:post` for simple submissions. Server reads via `request.param()` or `request.paramJson()`.

> **CSS classes** in the examples below (e.g. `form-control`, `btn btn-primary`) are Bootstrap. For Tailwind equivalents see `Skill.help('markup', 'tailwind')`. The mechanics — `ka:post`, `ka:data`, `request.paramJson`, etc. — are the same regardless.

## Simple Form (No Alpine)

For forms with a few fields, use standard HTML form elements with `ka:post`:

```html
<form ka:post="this" hx-target="#main-content">
  <input type="hidden" name="action" value="create"/>
  <div>
    <label>Team Name</label>
    <input name="name" required/>
  </div>
  <button type="submit">
    <span class="ka-indicator-hide">Create</span>
    <span class="ka-indicator <your-spinner-class>"></span>
  </button>
</form>
```

**Server reads:** `request.param('name')` and `request.param('action')`.

## Submit on Enter (single input, no `<form>`)

A bare `<input ka:post="...">` without a surrounding `<form>` does **not** submit on Enter by default — HTMX's default trigger for `<input>` elements is `change`, which fires on blur (and only coincidentally on Enter in some browsers). A "type then press Enter" UX (todo-list add field, single-field search) needs an explicit trigger.

Two idiomatic options:

**Option A — wrap in a `<form>` (preferred).** The browser's native form submission fires on Enter, and HTMX intercepts it. Also keeps progressive enhancement: a user with JS disabled can still hit Enter to submit.

```html
<form ka:post="main" hx-target="#main" hx-swap="outerHTML">
  <input type="hidden" name="action" value="add"/>
  <input class="<your-input-class>" name="title" placeholder="What needs to be done?" autofocus/>
</form>
```

**Option B — `hx-trigger` with a key filter (minimal, no wrapper).** Use when wrapping in a form is awkward (e.g. the input already lives inside another form, or you explicitly don't want a submit button).

```html
<input class="<your-input-class>" name="title" placeholder="What needs to be done?"
       ka:post="main" ka:vals="action:'add'"
       hx-trigger="keyup[key=='Enter']"/>
```

**The `[...]` is a JS filter expression** evaluated by HTMX (via `eval`, so `htmx.config.allowEval` must stay on — the default). `key=='Enter'` is case-sensitive; `'enter'` silently fails.

### Gotcha: `keyup-enter` is NOT valid HTMX syntax

Hyphen-suffix shortcuts like `hx-trigger="keyup-enter"` or `"keyup-escape"` are **not** a thing in HTMX 2.x — they look plausible but compile to "listen for a custom event named `keyup-enter`," which nothing ever fires. The symptom: input accepts keystrokes but Enter does nothing, no network request, no error. Always use the bracket-filter form:

```html
<!-- WRONG — looks like it should work, silently never fires -->
<input hx-trigger="keyup-enter" ka:post="save"/>
<input hx-trigger="blur,keyup-enter,keyup-escape" ka:post="update"/>

<!-- RIGHT -->
<input hx-trigger="keyup[key=='Enter']" ka:post="save"/>
<input hx-trigger="blur,keyup[key=='Enter'],keyup[key=='Escape']" ka:post="update"/>
```

## Alpine Form Binding (`ka:data`)

For complex forms with dynamic fields, toggles, or client-side validation, use `ka:data` to seed Alpine state from the server:

```html
<script ka:scope="local">
  // Prepare initial form data — server-side
  _.initialForm = { email: '', role: 'user', products: [], notify: true };
  // Or load existing data for edit:
  // _.initialForm = db.getTeam(request.param('teamId'));
</script>

<form ka:data="form:initialForm" ka:post="manage-team" hx-target="#main-content">
  <div>
    <label>Email</label>
    <input x-model="form.email" type="email"/>
  </div>

  <div>
    <label>Role</label>
    <div>
      <button type="button" x-on:click="form.role='user'"
              x-bind:class="form.role=='user' ? '<your-active-btn-class>' : '<your-inactive-btn-class>'">User</button>
      <button type="button" x-on:click="form.role='admin'"
              x-bind:class="form.role=='admin' ? '<your-active-btn-class>' : '<your-inactive-btn-class>'">Admin</button>
    </div>
  </div>

  <div>
    <input x-model="form.notify" type="checkbox" id="notify"/>
    <label for="notify">Send notifications</label>
  </div>

  <button type="submit">Save</button>
</form>
```

### What `ka:data` Does Automatically

1. Adds `x-data='{ form: {"email":"","role":"user",...} }'` (single-quoted for safe JSON; an apostrophe, `&` or `<` in a value is entity-escaped and the browser decodes it back — pass raw data, never pre-escape)
2. On `<form>` elements: injects a hidden input `<input type="hidden" name="form" x-bind:value="JSON.stringify(form)"/>`

> **⚠️ Nested-scope gotcha:** because step 1 puts `x-data` ON the form element, `ka:data` opens a **new Alpine scope** with its own `form` object. Inputs inside the form bind to this *inner* scope. If you wrap the form in an outer `x-data` scope (a modal, dropdown, page-level factory) and try to populate `form` from there (e.g. from a row-click payload), the inputs stay empty — they're reading the inner empty scope, not the outer hydrated one. Use `ka:data` for **user-typed** forms; for **payload-driven edit modals**, drop `ka:data` and mirror the outer scope's `form` through one `<input type="hidden" name="form" :value="JSON.stringify(form)"/>`. Full pattern + detection steps: `Skill.help('markup')` §Gotchas #20.

### Server Reads the Form Back

```html
<script ka:scope="global">
  if (request.post) {
    var form = request.paramJson('form');
    // form is a JS object: { email: "user@test.com", role: "admin", ... }
    db.saveTeam(form);
    context.flash.success = 'Team saved';
    context.redirect('/teams');
  }
</script>
```

## Action Parameters in Forms

**Gotcha:** `ka:vals` on a submit button inside an HTMX form is silently dropped. Use hidden `<input>` instead.

```html
<!-- WRONG — ka:vals on button inside form is ignored -->
<form ka:post="this" hx-target="#main-content">
  <button type="submit" ka:vals="action:'save'">Save</button>
</form>

<!-- CORRECT — hidden input for action -->
<form ka:post="this" hx-target="#main-content">
  <input type="hidden" name="action" value="save"/>
  <button type="submit">Save</button>
</form>
```

For standalone buttons (not inside a form), `ka:vals` works fine:

```html
<button ka:post="admin-teams" ka:vals="action:'delete',teamId:team.teamId"
        hx-target="#main-content" hx-confirm="Delete this team?">Delete</button>
```

## Flash Messages

Use `context.flash` to pass one-time messages across redirects:

```html
<!-- In the POST handler -->
<script ka:scope="global">
  if (request.post) {
    db.save(request.paramJson('form'));
    context.flash.success = 'Changes saved';
    context.redirect('/teams');
  }
</script>

<!-- In the target page — flash is auto-cleared after first read -->
<div th:if="flash && flash.success" class="alert alert-success alert-dismissible">
  <span th:text="flash.success">Message</span>
  <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>

<div th:if="flash && flash.error" class="alert alert-danger alert-dismissible">
  <span th:text="flash.error">Error</span>
  <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
```

## Validation

Combine HTML5 validation with server-side checks:

```html
<script ka:scope="global">
  if (request.post) {
    var form = request.paramJson('form');
    _.errors = [];
    if (!form.email || form.email.indexOf('@') < 0) {
      _.errors.push('Valid email required');
    }
    if (_.errors.length == 0) {
      db.save(form);
      context.flash.success = 'Saved';
      context.redirect('/teams');
    }
    // If errors, fall through — re-render page with errors and form data
    _.initialForm = form; // preserve user input
  }
</script>

<div th:if="errors && errors.length > 0" class="alert alert-danger">
  <ul class="mb-0">
    <li th:each="err: errors" th:text="err">Error</li>
  </ul>
</div>

<form ka:data="form:initialForm" ka:post="this" hx-target="#main-content">
  <input x-model="form.email" type="email" class="<your-input-class>" required/>
  <button type="submit">Save</button>
</form>
```

## CSRF Protection

When `ServerConfig.csrfEnabled` is true (default), karate-core auto-generates CSRF tokens.

**Setup (once in layout):**
```html
<meta name="csrf-token" th:content="context.csrf.token"/>
<script>
  document.addEventListener('htmx:configRequest', function(e) {
    var token = document.querySelector('meta[name="csrf-token"]');
    if (token) e.detail.headers['X-CSRF-Token'] = token.content;
  });
</script>
```

This intercepts every HTMX request and adds the CSRF header automatically. No per-form tokens needed.

**Disable for API-only servers:** `ServerConfig.csrfEnabled(false)` — appropriate for internal tools without user sessions.

## Multi-Step Forms (Wizard)

Use HTMX to swap between steps without full page reloads. Store progress in `session`:

```html
<script ka:scope="global">
  _.step = session.wizardStep || 1;
  if (request.post) {
    var action = request.param('action');
    if (action == 'next') {
      session.wizardData = Object.assign(session.wizardData || {}, request.paramJson('form'));
      session.wizardStep = _.step + 1;
      _.step = session.wizardStep;
    }
  }
</script>

<div th:if="step == 1" th:insert="wizard-step1"></div>
<div th:if="step == 2" th:insert="wizard-step2"></div>
<div th:if="step == 3" th:insert="wizard-complete"></div>
```
