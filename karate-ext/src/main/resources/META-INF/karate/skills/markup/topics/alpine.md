# Alpine.js Integration

> Alpine state is ephemeral. Re-initialized from server-rendered values on every HTMX swap. Don't persist client state — round-trip it.

> **CSS classes** in the examples below are illustrative. Substitute concrete classes from CSS_BOOTSTRAP.md or `Skill.help('markup', 'tailwind')`. For richer patterns (page-level Alpine factory, slide-over drawers, confirm-modal-with-intensity, undo toast, ⌘K palette, tree expand/collapse) see `tailwind.md` §Alpine pattern recipes.

## Seeding Alpine State from Server

Use `ka:data` to initialize Alpine `x-data` from server-side values:

```html
<script ka:scope="local">
  _.teamData = db.getTeam(teamId);
</script>
<div ka:data="team:teamData">
  <h2 x-text="team.name">Team Name</h2>
  <span x-text="team.members.length + ' members'">0 members</span>
</div>
```

`ka:data="team:teamData"` generates `x-data='{ team: {"name":"Acme","members":[...]} }'` with single-quoted attributes for safe JSON embedding; an apostrophe, `&` or `<` in a value is entity-escaped (the browser decodes it back) — pass raw data, never pre-escape.

**On non-form elements:** `ka:data` only sets `x-data`. No hidden input is injected.  
**On `<form>` elements:** `ka:data` also injects a hidden input for round-tripping (see Forms (`Skill.help('markup', 'forms')`)).

## Ephemeral State Model

Every HTMX swap re-renders the server template, which re-initializes `x-data` from current server state. Alpine state within the swapped region is discarded.

```
User clicks button → HTMX POST → Server processes → Server re-renders template
→ New HTML includes fresh x-data from server → Alpine initializes from new values
```

**Components outside the swap target keep their state.** Only the swapped fragment gets re-initialized. This is why `hx-target` scoping matters.

### What This Means in Practice

- Don't store important state only in Alpine — it will be lost on the next swap
- Use `session` (server-side) for state that persists across interactions
- Use Alpine for UI-only state: dropdowns, toggles, tab selection, form input in progress
- When a form submits, the server reads `request.paramJson('form')` — that's the round-trip

## Pure Client-Side Alpine (No `ka:data`)

For UI state that doesn't come from the server, use plain `x-data`:

```html
<!-- Toggle panel -->
<div x-data="{ showAdvanced: false }">
  <button @click="showAdvanced = !showAdvanced">
    <span x-text="showAdvanced ? 'Hide' : 'Show'">Show</span> Advanced
  </button>
  <div x-show="showAdvanced" x-transition class="mt-2">
    <!-- Advanced options -->
  </div>
</div>

<!-- Tab selector -->
<div x-data="{ tab: 'overview' }">
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <a class="nav-link" x-bind:class="tab == 'overview' && 'active'"
         @click.prevent="tab = 'overview'" href="#">Overview</a>
    </li>
    <li class="nav-item">
      <a class="nav-link" x-bind:class="tab == 'details' && 'active'"
         @click.prevent="tab = 'details'" href="#">Details</a>
    </li>
  </ul>
  <div x-show="tab == 'overview'">Overview content</div>
  <div x-show="tab == 'details'">Details content</div>
</div>
```

## Alpine with HTMX Interactions

Alpine state can drive HTMX requests. Use `hx-include` to send Alpine-managed values, or `ka:data` for the full round-trip:

```html
<!-- Search with Alpine-managed input -->
<div x-data="{ query: '' }">
  <input x-model="query" type="text"
         placeholder="Search..."
         hx-get="/search" hx-target="#results" hx-trigger="input changed delay:300ms"
         name="q" x-bind:value="query"/>
  <div id="results"></div>
</div>
```

## Alpine Event Handling

Common Alpine event patterns (CSS classes are illustrative — see your `CSS_*.md` for concrete styling):

```html
<!-- Dropdown with hover -->
<div x-data="{ open: false }" @mouseenter="open = true" @mouseleave="open = false">
  <button>Menu</button>
  <div x-show="open" x-transition>
    <a href="#">Option 1</a>
    <a href="#">Option 2</a>
  </div>
</div>

<!-- Inline confirmation before action (lightweight; for high-impact actions
     use the confirm-modal-with-intensity pattern in tailwind.md §E) -->
<div x-data="{ confirming: false }">
  <button x-show="!confirming" @click="confirming = true">Delete</button>
  <div x-show="confirming">
    <span>Are you sure?</span>
    <button @click="confirming = false">No</button>
    <button ka:post="this" ka:vals="action:'delete',id:item.id" hx-target="#main-content">Yes</button>
  </div>
</div>
```

## Alpine + `th:each`

Alpine components inside loops work naturally. Each iteration gets its own `x-data` scope:

```html
<div th:each="item: items" x-data="{ expanded: false }">
  <div>
    <span th:text="item.name">Name</span>
    <button @click="expanded = !expanded">
      <span x-text="expanded ? '▴' : '▾'"></span>
    </button>
  </div>
  <div x-show="expanded" x-transition>
    <p th:text="item.description">Description</p>
  </div>
</div>
```

## When to Use Alpine vs Vanilla JS

| Use Alpine | Use Vanilla JS |
|------------|----------------|
| Toggles, dropdowns, tabs | CodeMirror, video players |
| Form input binding | Canvas drawing |
| Show/hide with transitions | Complex widget lifecycle |
| Simple reactive lists | Third-party library wrappers |
| State that maps to DOM | State with internal logic |

See JS Island Patterns (`Skill.help('markup')`) in the core skill doc for vanilla JS examples.
