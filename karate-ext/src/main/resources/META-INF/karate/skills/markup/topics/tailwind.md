# CSS_TAILWIND — Tailwind path for Karate Markup

> Companion to `Skill.help('markup')`. Read that first for engine, Alpine, HTMX, sessions, routing, forms, etc. This file covers Tailwind-specific patterns: CDN, design tokens, components, class names, gotchas.
>
> For the Bootstrap path, see CSS_BOOTSTRAP.md.

The patterns here are extracted from the **Karate Sensei UI prototype** (snapshot at `/Users/peter/dev/zcode/sensei-preview/`, live at https://sensei-preview.netlify.app/). Treat the prototype as the canonical visual reference; this file codifies the recipes that recur across its 9 pages.

## Setup

Tailwind has two setup paths. Default to **production** for any shipped app — the CDN exists only for one-off prototypes.

### Production (recommended)

Pre-compile a tree-shaken stylesheet with the **Tailwind standalone CLI** (no Node toolchain — single binary download) and serve it as a static asset. Working reference: [karatelabs/studio](https://github.com/karatelabs/studio) — `conf/tailwind.config.js`, `conf/tailwind.src.css`, `conf/tailwind.sh` (the build script — auto-downloads + caches the CLI binary, supports `--watch` for dev iteration).

Layout:

```
conf/
  tailwind.config.js   ← design tokens + content glob (see §"Design tokens" below)
  tailwind.src.css     ← three lines: @tailwind base; @tailwind components; @tailwind utilities;
  tailwind.sh          ← build script
src/main/resources/app/pub/
  tailwind.css         ← generated, committed (~30-50 KB minified)
```

Build command (from the script): `tailwindcss -c conf/tailwind.config.js -i conf/tailwind.src.css -o src/main/resources/app/pub/tailwind.css --minify`. Wire it into your deploy script before `mvn package` so a stale bundle can never ship.

`<head>` block:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<link href="/pub/tailwind.css" rel="stylesheet" ka:nocache="true"/>
```

`ka:nocache="true"` rewrites the URL to `/pub/tailwind.css?ts=<lastModifiedMillis>` at render time so a fresh build (which updates the file's mtime) automatically busts the browser cache for users. See `Skill.help('markup')` §"HTMX error surface (required)" → layout.html for the full pattern across CSS + JS.

### Prototype / playground (CDN)

For a single-page demo with no build step (e.g. the `markup/playground/tailwind/` reference app in this repo):

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<script src="https://cdn.tailwindcss.com"></script>
```

The CDN ships JIT mode and reads any `tailwind.config = {...}` block that appears in the page **before any element using extended classes**. Adds ~50ms of CSS work per cold page (browser tree-shaking) and logs `cdn.tailwindcss.com should not be used in production` to the console — fine for prototypes, never for shipped apps.

## Design tokens (`tailwind.config`)

The same JS object below lives in either `conf/tailwind.config.js` (production — wrapped in `module.exports = {...}`) or an inline `<script>tailwind.config = {...}</script>` block in your `<head>` immediately after the CDN script (prototype). Production also adds a `content` array to scan templates for tree-shaking:

```html
<script>
  tailwind.config = {
    darkMode: 'class',                       // see §Dark mode below
    theme: {
      extend: {
        fontFamily: {
          sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
          mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
        },
        colors: {
          ink: {
            50:  '#F8FAFC', 100: '#F1F5F9', 200: '#E2E8F0', 300: '#CBD5E1',
            400: '#94A3B8', 500: '#64748B', 600: '#475569', 700: '#334155',
            800: '#1E293B', 900: '#0F172A', 950: '#020617',
          },
          brand: {
            50:  '#EEF2FF', 100: '#E0E7FF', 200: '#C7D2FE', 300: '#A5B4FC',
            400: '#818CF8', 500: '#6366F1', 600: '#4F46E5', 700: '#4338CA',
            800: '#3730A3', 900: '#312E81', 950: '#1E1B4B',
          },
          coral: { 300: '#FDA29B', 400: '#F97066', 500: '#F04438' },
        },
        boxShadow: {
          'card':   '0 1px 2px 0 rgb(0 0 0 / 0.04), 0 1px 3px 0 rgb(0 0 0 / 0.06)',
          'drawer': '-8px 0 24px -4px rgb(0 0 0 / 0.10), -2px 0 8px -2px rgb(0 0 0 / 0.06)',
        },
      },
    },
  };
</script>
<style>
  html, body { font-family: 'Inter', ui-sans-serif, system-ui, sans-serif; }
  .font-mono { font-family: 'JetBrains Mono', ui-monospace, monospace; }
  ::-webkit-scrollbar { width: 8px; height: 8px; }
  ::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 4px; }
  ::-webkit-scrollbar-track { background: transparent; }
  [x-cloak] { display: none !important; }
</style>
```

**Production form (`conf/tailwind.config.js`)** — same theme object, wrapped as a CommonJS module with a `content` array so the CLI knows which files to scan:

```js
const path = require('path');

module.exports = {
  darkMode: 'class',
  content: [
    // __dirname-relative so the glob is correct regardless of caller cwd.
    path.resolve(__dirname, '../src/main/resources/app/**/*.html'),
  ],
  theme: { extend: { /* ...same fontFamily / colors / boxShadow as above... */ } },
};
```

Move the `<style>` rules (Inter, scrollbar, `[x-cloak]`) into `pub/app.css` instead of inlining them in every page — production has a single static stylesheet anyway, no reason to repeat.

**Color usage at a glance:** `ink` is the slate ramp (text, borders, neutral surfaces — `ink-50` for body bg, `ink-900` for primary text, `ink-950` for the rail). `brand` is indigo (primary actions, active states — `brand-700` is the workhorse). `coral` is for warnings / destructive / renewal-due states.

## Dark mode

The shell supports light, dark, and system-follow themes. Tailwind's `darkMode: 'class'` strategy means dark variants apply only when `<html>` has the `dark` class. We resolve which class to set before paint to avoid a flash of light theme on dark systems.

**Contract:**

| `localStorage.theme` | Behavior |
|---|---|
| `'dark'` | Force dark, regardless of system |
| `'light'` | Force light, regardless of system |
| absent | Follow system (`prefers-color-scheme`); live-tracks system changes |

**FOUC-avoiding head script** — paste this **before** the Tailwind CDN script (it must run before any element renders so `<html>` already has `class="dark"` when stylesheets evaluate):

```html
<script>
  // Resolve theme before paint. Sets `dark` class on <html> when appropriate.
  // Pair with the cycleTheme() / applyTheme() helpers in pub/app.js.
  (function () {
    var stored = localStorage.getItem('theme');
    var systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    var dark = stored === 'dark' || (stored !== 'light' && systemDark);
    if (dark) document.documentElement.classList.add('dark');
  })();
</script>
```

**Toggle helpers in `pub/app.js`:**

```js
// Cycle: light → dark → system → light. Persist override in localStorage.
function cycleTheme() {
  var stored = localStorage.getItem('theme');
  if (stored === 'light')      localStorage.setItem('theme', 'dark');
  else if (stored === 'dark')  localStorage.removeItem('theme');     // → system
  else                         localStorage.setItem('theme', 'light');
  applyTheme();
}

function applyTheme() {
  var stored = localStorage.getItem('theme');
  var systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  var dark = stored === 'dark' || (stored !== 'light' && systemDark);
  document.documentElement.classList.toggle('dark', dark);
  // Reflect current mode on the toggle button so [data-mode] CSS picks the right icon.
  var btn = document.getElementById('theme-toggle');
  if (btn) btn.dataset.mode = stored || 'system';
}

// When in 'system' mode, mirror system flips live (no page reload needed).
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
  if (!localStorage.getItem('theme')) applyTheme();
});

document.addEventListener('DOMContentLoaded', applyTheme);
```

**Toggle button in the topbar** — three icons, one visible at a time based on `data-mode`. Use sibling `:not()` selectors so the button's HTML stays declarative:

```html
<button id="theme-toggle" type="button" data-mode="system" onclick="cycleTheme()"
        title="Theme: click to cycle Light / Dark / System"
        class="w-9 h-9 rounded-lg hover:bg-ink-100 dark:hover:bg-ink-800 text-ink-600 dark:text-ink-300 flex items-center justify-center">
  <!-- Sun (shown when mode='light') -->
  <svg data-icon="light" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.25m6.364.386-1.591 1.591M21 12h-2.25m-.386 6.364-1.591-1.591M12 18.75V21m-4.773-4.227-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z"/></svg>
  <!-- Moon (shown when mode='dark') -->
  <svg data-icon="dark" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.72 9.72 0 0 1 18 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 0 0 3 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 0 0 9.002-5.998Z"/></svg>
  <!-- Monitor (shown when mode='system') -->
  <svg data-icon="system" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 0 1-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0 1 15 18.257V17.25m6-12V15a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 15V5.25m18 0A2.25 2.25 0 0 0 18.75 3H5.25A2.25 2.25 0 0 0 3 5.25m18 0V12a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 12V5.25"/></svg>
</button>
```

```css
/* In pub/app.css — show only the icon matching the current mode */
#theme-toggle [data-icon] { display: none; }
#theme-toggle[data-mode='light']  [data-icon='light']  { display: block; }
#theme-toggle[data-mode='dark']   [data-icon='dark']   { display: block; }
#theme-toggle[data-mode='system'] [data-icon='system'] { display: block; }
```

**Dark variant table for the canonical surfaces.** Apply alongside the light class as `light dark:dark`:

| Element | Light | Dark variant |
|---|---|---|
| Body bg | `bg-ink-50` | `dark:bg-ink-950` |
| Card bg | `bg-white` | `dark:bg-ink-900` |
| Topbar bg | `bg-white` | `dark:bg-ink-900` |
| Rail bg | `bg-ink-950` | (unchanged — rail is dark in both) |
| Border subtle | `border-ink-100` | `dark:border-ink-800` |
| Border emphatic | `border-ink-200` | `dark:border-ink-700` |
| Text primary | `text-ink-900` | `dark:text-ink-100` |
| Text secondary | `text-ink-700` | `dark:text-ink-300` |
| Text muted | `text-ink-500` | `dark:text-ink-400` |
| Active rail item | `bg-brand-700 text-white` | (unchanged) |
| Hover row | `hover:bg-ink-50` | `dark:hover:bg-ink-800/50` |
| Input bg | `bg-white` | `dark:bg-ink-900` |
| Input border | `border-ink-200` | `dark:border-ink-700` |
| Pill `<primary>` | `bg-brand-50 text-brand-700` | `dark:bg-brand-950 dark:text-brand-300` |
| Pill `<success>` | `bg-emerald-50 text-emerald-700` | `dark:bg-emerald-950 dark:text-emerald-300` |
| Pill `<warning>` | `bg-amber-50 text-amber-700` | `dark:bg-amber-950 dark:text-amber-300` |
| Pill `<danger>` | `bg-coral-400/10 text-coral-500` | (unchanged — coral reads on both) |

**Gotcha — FOUC requires the inline `<script>` to run before any stylesheet evaluates.** The pre-render `<script>` MUST sit before `<link href="/pub/tailwind.css">` (production) or the `cdn.tailwindcss.com` script (prototype) AND before any other external stylesheet. If it runs after the parser has reached `<body>`, browsers may have already painted the light shell, then re-paint dark — visible flash. Place it as the first `<script>` in `<head>`, after `<meta>` tags only.

**Gotcha — JIT can't see dark variants in dynamic class strings.** Same rule as T1: write `dark:bg-ink-900` as a literal in the source, not constructed at runtime. Server-side `th:class` ternaries that emit dark variants must include both branches as complete literal strings (e.g. `th:class="active ? 'bg-brand-700 text-white' : 'text-ink-700 dark:text-ink-300'"`).

## Concrete class substitution table

Wherever `Skill.help('markup')` says `<your-X-class>`, swap in:

| Placeholder | Tailwind classes |
|---|---|
| `<success>` (badge / pill) | `bg-emerald-50 text-emerald-700` |
| `<primary>` (badge / pill) | `bg-brand-50 text-brand-700` |
| `<danger>` (badge / pill) | `bg-coral-400/10 text-coral-500` |
| `<warning>` (badge / pill) | `bg-amber-50 text-amber-700` |
| `<neutral>` / `<secondary>` (badge / pill) | `bg-ink-100 text-ink-700` |
| `<your-success-alert-class>` | `flex items-start gap-3 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-emerald-800` |
| `<your-danger-alert-class>` | `flex items-start gap-3 rounded-lg border border-coral-300/60 bg-coral-400/10 px-4 py-3 text-coral-500` |
| `<your-active-btn-class>` | `inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-700 hover:bg-brand-800 rounded-lg shadow-sm` |
| `<your-inactive-btn-class>` | `inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-ink-700 bg-white border border-ink-200 rounded-lg hover:bg-ink-50` |
| `<your-input-class>` | `block w-full px-3 py-2 text-sm border border-ink-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500` |
| `<your-spinner-class>` | `animate-spin h-4 w-4 border-2 border-ink-300 border-t-brand-700 rounded-full` |
| `<your-success-icon-class>` | inline `<svg class="w-4 h-4 text-emerald-500">…</svg>` (Heroicons) |
| `<your-danger-icon-class>` | inline `<svg class="w-4 h-4 text-coral-500">…</svg>` |
| Hide-class for HTMX-error-toast JS | `hidden` |

In the HTMX-error toast handler (from `Skill.help('markup')` §HTMX error surface), `host.classList.remove('hidden')` / `add('hidden')` are the show/hide calls.

## Layout shell — vertical icon rail + sticky topbar

Adapted from prototype `index.html`. This is the canonical shell for any Sensei-style enterprise admin app.

```html
<body class="bg-ink-50 text-ink-900 antialiased">
<div class="flex min-h-screen">

  <!-- ============== RAIL (left, dark, sticky) ============== -->
  <!-- Dark variant: rail one shade lighter than body. Body dark = ink-950, so
       rail dark = ink-900 keeps the rail visible as a distinct surface (matches
       the topbar's elevation). Without dark:bg-ink-900 the rail merges into the
       body and the only differentiator is the faint border — the UI element
       effectively disappears in dark mode. -->
  <aside class="w-16 shrink-0 bg-ink-950 dark:bg-ink-900 border-r border-ink-800 flex flex-col items-center py-4 text-ink-400 sticky top-0 h-screen">
    <a class="rail-item w-12 h-12 flex items-center justify-center mb-2 hover:scale-105 transition" href="/" title="App brand">
      <img src="/karate-logo.svg" alt="Brand" class="w-10 h-10"/>
      <span class="rail-tip"><strong>Karate Sensei</strong> · enterprise admin</span>
    </a>
    <div class="text-[10px] font-semibold tracking-[0.2em] uppercase text-ink-400 mb-4">SENSEI</div>
    <nav class="flex flex-col gap-1 items-center">
      <!-- Active item -->
      <a href="/" class="rail-item w-10 h-10 rounded-lg bg-brand-700 text-white flex items-center justify-center" aria-current="page">
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12 12 2.25 21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25"/></svg>
        <span class="rail-tip">Dashboard</span>
      </a>
      <!-- Inactive item -->
      <a href="/users" class="rail-item w-10 h-10 rounded-lg hover:bg-ink-800 hover:text-white flex items-center justify-center transition">
        <svg class="w-5 h-5">…</svg>
        <span class="rail-tip">Users</span>
      </a>
    </nav>
    <a href="/help" class="rail-item w-10 h-10 rounded-lg hover:bg-ink-800 hover:text-white flex items-center justify-center transition mt-auto">
      <svg class="w-5 h-5">…</svg>
      <span class="rail-tip">Help</span>
    </a>
  </aside>

  <!-- ============== MAIN COLUMN ============== -->
  <div class="flex-1 flex flex-col min-w-0">
    <header class="h-16 bg-white border-b border-ink-200 px-6 flex items-center gap-6 sticky top-0 z-30">
      <!-- Breadcrumb (left) -->
      <nav class="flex items-center gap-2 text-sm min-w-0">
        <a class="text-ink-500 hover:text-ink-700" href="/">Sensei</a>
        <svg class="w-4 h-4 text-ink-300">…</svg>
        <span class="text-ink-900 font-medium" th:text="team.name">Acme Corp</span>
      </nav>
      <!-- ⌘K palette trigger (center) -->
      <div class="flex-1 flex justify-center">
        <button @click="$dispatch('open-palette')" class="w-full max-w-md flex items-center gap-3 px-3 py-2 rounded-lg bg-ink-100 hover:bg-ink-200 text-ink-500 text-sm transition">
          <svg class="w-4 h-4">🔍</svg>
          <span class="flex-1 text-left">Search users, groups, licenses…</span>
          <kbd class="px-1.5 py-0.5 text-[11px] font-mono bg-white border border-ink-200 rounded text-ink-600">⌘K</kbd>
        </button>
      </div>
      <!-- Identity chip (right) -->
      <div class="flex items-center gap-3">
        <span class="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-brand-50 text-brand-700 text-xs font-medium">
          <svg class="w-3 h-3">★</svg>Enterprise
        </span>
        <button class="relative w-9 h-9 rounded-lg hover:bg-ink-100 text-ink-600 flex items-center justify-center">
          <svg class="w-5 h-5">🔔</svg>
          <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-coral-400"></span>
        </button>
        <button class="flex items-center gap-2 pl-2 pr-3 py-1 rounded-lg hover:bg-ink-100">
          <span class="w-8 h-8 rounded-full bg-brand-700 text-white text-sm font-semibold flex items-center justify-center" th:text="initials">PT</span>
          <div class="text-left hidden md:block leading-tight">
            <div class="text-sm font-medium text-ink-900" th:text="session.user.displayName">Peter Thomas</div>
            <div class="text-[11px] text-ink-500" th:text="roleLabel">Team admin</div>
          </div>
        </button>
      </div>
    </header>
    <main class="flex-1 px-8 py-8 max-w-[1400px] w-full mx-auto" id="main-content" th:utext="content">
      <!-- shell-wrapped content from Skill.help('markup') §Layout shell -->
    </main>
  </div>
</div>
</body>
```

**Rail tooltip CSS** (drop into `pub/app.css`):

```css
.rail-item { position: relative; }
.rail-item .rail-tip {
  position: absolute; left: 100%; top: 50%; transform: translateY(-50%);
  margin-left: 12px; padding: 6px 10px; background: #0F172A; color: #fff;
  font-size: 12px; border-radius: 6px; white-space: nowrap;
  opacity: 0; pointer-events: none; transition: opacity 120ms ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 50;
}
.rail-item:hover .rail-tip { opacity: 1; }
```

**Active rail item:** `bg-brand-700 text-white` + `aria-current="page"`. Render the active class server-side via `th:class` based on the current path.

## Reusable visual components

### KPI tile

```html
<div class="bg-white rounded-xl shadow-card p-5 border border-ink-100">
  <div class="text-xs font-medium text-ink-500 uppercase tracking-wide">Total users</div>
  <div class="mt-2 text-3xl font-semibold text-ink-900 tabular-nums" th:text="totalUsers">419</div>
  <div class="mt-2 text-xs text-emerald-600 font-medium inline-flex items-center gap-1">
    <svg class="w-3 h-3">↑</svg> +12 this month
  </div>
</div>
```

For the **alert variant** (e.g. "Pending approvals: 4"), add a coral ring + pulse:

```html
<div class="bg-white rounded-xl shadow-card p-5 border-2 border-coral-300/60">
  <div class="text-xs font-semibold text-coral-500 uppercase tracking-wide inline-flex items-center gap-1.5">
    Pending <span class="w-1.5 h-1.5 rounded-full bg-coral-400 animate-pulse"></span>
  </div>
  <div class="mt-2 text-3xl font-semibold text-coral-500 tabular-nums" th:text="pendingCount">4</div>
</div>
```

### Status pill

```html
<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-brand-50 text-brand-700 text-xs font-medium">
  <svg class="w-3 h-3">✓</svg> Active
</span>
```

### Progress bar (with color ramp by percentage)

```html
<div class="space-y-1">
  <div class="flex justify-between text-sm">
    <span class="font-medium text-ink-900">PRO</span>
    <span class="text-ink-500"><span th:text="used">72</span> / <span th:text="quota">100</span></span>
  </div>
  <div class="h-2 bg-ink-100 rounded-full overflow-hidden">
    <div class="h-full rounded-full"
         th:class="pct >= 100 ? 'bg-coral-500' : pct >= 80 ? 'bg-amber-500' : 'bg-brand-500'"
         th:style="'width: ' + pct + '%'"></div>
  </div>
</div>
```

### Selectable table row

```html
<tr class="hover:bg-ink-50/60 cursor-pointer transition"
    :class="{'bg-brand-50 shadow-[inset_3px_0_0_#4338CA]': selectedId === user.id}"
    @click="select(user.id)">
  <td class="px-4 py-3" @click.stop>
    <input type="checkbox" x-model="user.checked"/>
  </td>
  <td class="px-4 py-3 text-sm font-medium text-ink-900" th:text="user.email">alice@acme.com</td>
  <td class="px-4 py-3 text-sm text-ink-500" th:text="user.lastSeen">2 days ago</td>
</tr>
```

The `@click.stop` on the checkbox cell prevents the row-select handler from firing. The `:class` ternary applies the selected-state classes; the `box-shadow: inset 3px 0 0` is the prototype's left-border accent.

### Renewal banner (coral, gradient)

```html
<div class="mb-6 rounded-xl border-2 border-coral-400/60 bg-gradient-to-r from-coral-400/10 via-coral-300/5 to-transparent p-5 flex items-center gap-4 shadow-sm"
     th:if="daysUntilExpiry &lt; 30">
  <div class="w-11 h-11 rounded-lg bg-coral-400 text-white flex items-center justify-center shrink-0 shadow-md">
    <svg class="w-5 h-5">⚠</svg>
  </div>
  <div class="flex-1 min-w-0">
    <h2 class="text-base font-semibold text-ink-900">
      Your plan renews in <span class="text-coral-500 font-bold" th:text="daysUntilExpiry">14</span> days
    </h2>
    <p class="text-sm text-ink-600 mt-0.5">
      Renew now to avoid service interruption — your <span th:text="userCount">419</span> users will lose IDE access on expiry.
    </p>
  </div>
  <a th:href="'mailto:sales@karatelabs.io?subject=Renewal — ' + team.name"
     class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-semibold text-white bg-coral-500 hover:bg-coral-400 rounded-lg shadow-sm">
    Renew now <svg class="w-4 h-4">→</svg>
  </a>
</div>
```

## Alpine pattern recipes (canonical)

These patterns recur across the prototype. They are framework-agnostic in spirit but the visual recipes here are Tailwind-specific.

### A. Page-level Alpine factory

Reference implementation: `markup/playground/tailwind/src/main/resources/app/users.html` ([github](https://github.com/karatelabs/karate-skills)). One factory closure (`usersPage()`) owns the table, the slide-over drawer, the confirm modal, AND the undo toast — actions on the drawer call `openConfirm` + `showUndoToast` directly because they're sibling methods on the same Alpine component.

For pages with non-trivial state (table + drawer + modal + filters), define one factory function and bind it at the body or outer container:

```html
<!-- The shell renders content fragments inside <main>, so the factory binds
     to the fragment's outer div (not <body>, which is owned by the layout). -->
<div x-data="usersPage()" x-cloak class="relative">
  …table, drawer, confirm modal, toast all live inside this div…

  <!-- JSON island for hydration (Gotcha #14). Always emitted; init() reads + parses.
       Server sets _.usersJson = JSON.stringify(_.users); the th:utext substitutes
       the raw JSON between the <script> tags. The [] fallback shows if the
       template engine doesn't substitute (helps surface server-side bugs). -->
  <script type="application/json" id="users-data" th:utext="usersJson">[]</script>

  <script>
    function usersPage() {
      return {
        // ---- state ----
        users: [],            // hydrated by init() from the JSON island above
        selectedId: null,
        statusFilter: 'all',
        search: '',

        // ---- §E confirm-modal state (sibling, not nested) ----
        confirm: { open: false, /* … */ onConfirm: null },

        // ---- §F undo-toast state ----
        undoToast: { visible: false, /* … */ onUndo: null, onCommit: null },
        _undoTimer: null,

        // ---- init ----
        init() {
          const node = document.getElementById('users-data');
          if (node && node.textContent) {
            try { this.users = JSON.parse(node.textContent); }
            catch (e) { console.error('users-data hydration failed', e); this.users = []; }
          }
        },

        // ---- computed ----
        get selectedUser() { return this.users.find(u => u.id === this.selectedId); },
        get visibleUsers() {
          const q = (this.search || '').toLowerCase().trim();
          return this.users.filter(u =>
            (this.statusFilter === 'all' || u.status === this.statusFilter) &&
            (!q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
          );
        },

        // ---- actions ----
        select(id) { this.selectedId = (this.selectedId === id) ? null : id; },

        // Drawer action that chains §E confirm → §F toast.
        confirmRevoke(u) {
          this.openConfirm({
            intensity: 'danger',
            title: 'Revoke ' + u.license + ' from ' + u.name + '?',
            confirmText: u.email,    // type-to-confirm gate
            confirmCta: 'Revoke',
            onConfirm: () => {
              const prev = u.license;
              u.license = null;       // optimistic mutation on the same object Alpine reactivity owns
              this.showUndoToast({
                message: prev + ' revoked from ' + u.name,
                onUndo:   () => { u.license = prev; },
                onCommit: () => fetch('/api/team/users/' + u.id + '/revoke', {method: 'POST'}),
              });
            },
          });
        },

        // … openConfirm / canConfirm / submitConfirm     (§E)
        // … showUndoToast / undo / commit / _fireCommit  (§F)
      };
    }
  </script>
</div>
```

Why the patterns:
- **One factory, one closure, one source of state.** The drawer can call `openConfirm` and `showUndoToast` directly because they're sibling methods on the same Alpine component. No event-bus, no `$dispatch`. If you split state across multiple `x-data` blocks (drawer, modal, toast as separate scopes), you lose the easy chaining and have to wire `$dispatch` between them.
- **Why bind to the fragment's outer div, not `<body>`.** When the layout shell wraps content fragments via `th:utext="content"`, the fragment owns its own outermost element. Putting `x-data` there lets the fragment be self-contained and survive route changes (each route swap rebuilds the Alpine component). Putting `x-data` on `<body>` requires the body to live in the fragment, which conflicts with the shell ownership.
- **JSON-island hydration over inline `x-data` literals.** For >5–10 rows, the JSON island wins on three axes: (1) the data shape is server-controlled (no maintenance of two parallel literals); (2) larger payloads don't bloat the template attribute; (3) survives Thymeleaf's `th:if`-on-`<script>` quirk (Gotcha #14 in `Skill.help('markup')`). The fallback `[]` shows if substitution fails — helps surface server-side bugs.
- **Optimistic mutation of the row object (`u.license = null`).** Alpine's reactivity owns the same object, so the table re-renders immediately. Undo restores by re-assigning the captured `prev`. Avoid wrapping in `Object.assign` or replacing the whole `users` array — direct property mutation is faster and simpler.
- **For form data, `ka:data` is still the preferred path** — JSON-island is for read-only / sometimes-mutated arrays; `ka:data` is for forms that round-trip through `request.paramJson`. See §Forms.

### B. Modal overlay (centered, backdrop, escape-to-close)

```html
<div x-show="modalOpen" x-cloak
     class="fixed inset-0 z-50 bg-ink-950/40 flex items-center justify-center p-4"
     x-transition:enter="transition-opacity ease-out duration-200"
     x-transition:enter-start="opacity-0"
     x-transition:enter-end="opacity-100"
     x-transition:leave="transition-opacity ease-in duration-150"
     x-transition:leave-start="opacity-100"
     x-transition:leave-end="opacity-0"
     @click.self="modalOpen = false"
     @keydown.escape.window="modalOpen = false">
  <div x-show="modalOpen" x-cloak
       x-transition:enter="transition-transform ease-out duration-200"
       x-transition:enter-start="scale-95"
       x-transition:enter-end="scale-100"
       x-transition:leave="transition-transform ease-in duration-150"
       x-transition:leave-start="scale-100"
       x-transition:leave-end="scale-95"
       class="bg-white dark:bg-ink-900 rounded-2xl shadow-2xl max-w-lg w-full">
    <div class="flex items-start justify-between p-5 border-b border-ink-100 dark:border-ink-800">
      <h3 class="text-lg font-semibold text-ink-900 dark:text-ink-100">Modal title</h3>
      <button @click="modalOpen = false" class="text-ink-400 hover:text-ink-700 p-1 rounded hover:bg-ink-100 dark:hover:bg-ink-800">
        <svg class="w-5 h-5">×</svg>
      </button>
    </div>
    <div class="p-5">…body…</div>
    <div class="flex justify-end gap-2 p-4 border-t border-ink-100 dark:border-ink-800">
      <button @click="modalOpen = false" class="px-3 py-2 text-sm font-medium text-ink-700 dark:text-ink-200 bg-white dark:bg-ink-900 border border-ink-200 dark:border-ink-700 rounded-lg hover:bg-ink-50 dark:hover:bg-ink-800">Cancel</button>
      <button @click="save(); modalOpen = false" class="px-3 py-2 text-sm font-semibold text-white bg-brand-700 hover:bg-brand-800 rounded-lg">Save</button>
    </div>
  </div>
</div>
```

**Why `@click.self`:** clicking the backdrop closes; clicking the modal card does not (event bubbles, but `.self` filters to the literal target). **Why `@keydown.escape.window`:** Escape works regardless of focus.

**Why outer transitions opacity only and inner transitions transform only — split-property rule.** When two nested `x-show` elements both animate opacity, multiplicative blending (card_opacity × overlay_opacity) produces a perceptually faster fade on the card than either curve alone, and any duration mismatch between outer and inner becomes a visible flicker on close. The split-property recipe above keeps each layer doing one thing: backdrop fades opacity, card scales transform. The card's effective opacity tracks the backdrop's because it's nested inside it — no double-animation, no compounding. **Also:** prefer the longhand `x-transition:enter` / `:leave` form (with `transition-opacity` / `transition-transform` Tailwind classes) over Alpine's shorthand `x-transition.opacity`. The shorthand uses Alpine's default duration which can drift from your inner element's explicit `duration-150` — visible as a flicker frame at the leave boundary. Longhand makes durations and easings symmetric across nested transitions. See Tailwind gotcha T8 below.

### C. Slide-over drawer (right side, full-height detail panel)

The user-detail panel pattern from `users.html`:

```html
<!-- Drawer -->
<div x-show="selectedId" x-cloak
     class="fixed top-0 right-0 h-screen w-[480px] bg-white shadow-drawer z-40 overflow-y-auto"
     x-transition:enter="transition transform ease-out duration-200"
     x-transition:enter-start="translate-x-full"
     x-transition:enter-end="translate-x-0"
     x-transition:leave="transition transform ease-in duration-150"
     x-transition:leave-start="translate-x-0"
     x-transition:leave-end="translate-x-full"
     @keydown.escape.window="selectedId = null">
  <template x-if="selectedUser">
    <div class="p-5">
      <div class="flex justify-between items-start">
        <div>
          <h3 class="text-lg font-semibold" x-text="selectedUser.email"></h3>
          <p class="text-sm text-ink-500" x-text="selectedUser.role"></p>
        </div>
        <button @click="selectedId = null" class="text-ink-400 hover:text-ink-700 p-1 rounded hover:bg-ink-100">
          <svg class="w-5 h-5">×</svg>
        </button>
      </div>
      <!-- detail content -->
    </div>
  </template>
</div>

<!-- Backdrop (separate element so the drawer slides without the backdrop animating) -->
<div x-show="selectedId" x-cloak
     class="fixed inset-0 bg-ink-950/30 z-30"
     x-transition:enter="transition opacity ease-out duration-200"
     x-transition:enter-start="opacity-0"
     x-transition:enter-end="opacity-100"
     x-transition:leave="transition opacity ease-in duration-150"
     x-transition:leave-start="opacity-100"
     x-transition:leave-end="opacity-0"
     @click="selectedId = null"></div>
```

The backdrop is a separate element with its own opacity transition so the drawer can slide while the backdrop fades. `shadow-drawer` is the custom token defined above.

### D. Dropdown menu (action menu / overflow menu)

```html
<div class="relative" x-data="{open: false}">
  <button @click="open = !open" @click.outside="open = false"
          class="inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium text-ink-700 bg-white border border-ink-200 rounded-lg hover:bg-ink-50">
    Actions
    <svg class="w-4 h-4" :class="{'rotate-180': open}">▾</svg>
  </button>
  <div x-show="open" x-cloak
       x-transition:enter="transition ease-out duration-100"
       x-transition:enter-start="opacity-0 scale-95"
       x-transition:enter-end="opacity-100 scale-100"
       class="absolute right-0 mt-2 w-56 bg-white border border-ink-200 rounded-lg shadow-lg py-1 z-20">
    <a href="#" class="block px-4 py-2 text-sm text-ink-700 hover:bg-ink-50">Import CSV</a>
    <a href="#" class="block px-4 py-2 text-sm text-ink-700 hover:bg-ink-50">Export CSV</a>
    <a href="#" class="block px-4 py-2 text-sm text-coral-500 hover:bg-coral-50">Delete selected</a>
  </div>
</div>
```

`@click.outside` is Alpine-native and replaces the manual `document.addEventListener('click', ...)` pattern. Combine with `@click.self="open = false"` on the menu items if you want clicking an item to also close the dropdown.

### E. Confirm modal with intensity (G16 pattern from prototype)

For destructive or high-impact actions, the prototype uses a graded confirm modal — `intensity` controls visual treatment (soft = neutral header + brand CTA; danger = coral header + coral CTA; nuclear = danger + type-to-confirm gate). One modal element per page, opened by an `openConfirm({...})` factory; every action button passes its own copy + `onConfirm` closure.

Reference implementation: `markup/playground/tailwind/src/main/resources/app/confirm.html` ([github](https://github.com/karatelabs/karate-skills)).

```html
<!-- Single modal, opened by openConfirm({...}) from any button.
     Split-property transitions (backdrop fades opacity; card scales transform)
     match §B and avoid the compound-opacity flicker (see gotcha T8). -->
<div x-show="confirm.open" x-cloak
     class="fixed inset-0 z-50 bg-ink-950/50 flex items-center justify-center p-4"
     x-transition:enter="transition-opacity ease-out duration-200"
     x-transition:enter-start="opacity-0"
     x-transition:enter-end="opacity-100"
     x-transition:leave="transition-opacity ease-in duration-150"
     x-transition:leave-start="opacity-100"
     x-transition:leave-end="opacity-0"
     @click.self="confirm.open = false"
     @keydown.escape.window="confirm.open = false">
  <div x-show="confirm.open" x-cloak
       x-transition:enter="transition-transform ease-out duration-200"
       x-transition:enter-start="scale-95"
       x-transition:enter-end="scale-100"
       x-transition:leave="transition-transform ease-in duration-150"
       x-transition:leave-start="scale-100"
       x-transition:leave-end="scale-95"
       class="bg-white dark:bg-ink-900 rounded-2xl shadow-2xl max-w-md w-full overflow-hidden"
       @keydown.enter="if (canConfirm()) submitConfirm()">
    <div class="p-5 border-b border-ink-100 dark:border-ink-800"
         :class="confirm.intensity === 'danger' ? 'bg-coral-50 dark:bg-coral-400/10' : ''">
      <h3 class="text-lg font-semibold text-ink-900 dark:text-ink-100" x-text="confirm.title"></h3>
      <p class="text-sm text-ink-600 dark:text-ink-400 mt-1" x-text="confirm.subtitle"></p>
    </div>
    <div class="p-5 space-y-4">
      <ul class="space-y-1 text-sm text-ink-700 dark:text-ink-300">
        <template x-for="line in confirm.impact" :key="line">
          <li class="flex items-start gap-2">
            <span class="text-ink-400 dark:text-ink-500 mt-px">•</span>
            <span x-text="line"></span>
          </li>
        </template>
      </ul>
      <!-- Type-to-confirm field for nuclear actions. x-show toggles based on
           whether confirmText is supplied — soft / danger callers omit it. -->
      <div x-show="confirm.confirmText">
        <label class="text-xs text-ink-500 dark:text-ink-400">
          Type <span class="font-mono font-semibold text-ink-900 dark:text-ink-100" x-text="confirm.confirmText"></span> to confirm
        </label>
        <input x-model="confirm.typed"
               x-ref="typeConfirmInput"
               type="text" autocomplete="off" spellcheck="false"
               class="block w-full mt-1 px-3 py-2 text-sm bg-white dark:bg-ink-950 text-ink-900 dark:text-ink-100 border border-ink-200 dark:border-ink-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-coral-500 focus:border-coral-500"/>
      </div>
    </div>
    <div class="flex justify-end gap-2 p-4 border-t border-ink-100 dark:border-ink-800 bg-ink-50 dark:bg-ink-950/50">
      <button @click="confirm.open = false"
              class="px-3 py-2 text-sm font-medium text-ink-700 dark:text-ink-200 bg-white dark:bg-ink-900 border border-ink-200 dark:border-ink-700 rounded-lg hover:bg-ink-50 dark:hover:bg-ink-800">Cancel</button>
      <button @click="submitConfirm()"
              :disabled="!canConfirm()"
              :class="confirm.intensity === 'danger' ? 'bg-coral-500 hover:bg-coral-400' : 'bg-brand-700 hover:bg-brand-800'"
              class="px-3 py-2 text-sm font-semibold text-white rounded-lg shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
              x-text="confirm.confirmCta || 'Confirm'"></button>
    </div>
  </div>
</div>
```

In the page factory (see §A):

```js
confirm: {
  open: false, intensity: 'soft', title: '', subtitle: '', impact: [],
  confirmText: '', typed: '', confirmCta: 'Confirm', onConfirm: null,
},
openConfirm(cfg) {
  // Defaults-then-override merge prevents stale state (e.g. previous confirmText)
  // from leaking between invocations.
  Object.assign(this.confirm, {
    open: true, intensity: 'soft', title: '', subtitle: '', impact: [],
    confirmText: '', typed: '', confirmCta: 'Confirm', onConfirm: null,
  }, cfg);
  if (this.confirm.confirmText) {
    this.$nextTick(() => {
      const el = this.$refs.typeConfirmInput;
      if (el) el.focus();
    });
  }
},
canConfirm() {
  if (!this.confirm.open) return false;
  if (this.confirm.confirmText && this.confirm.typed !== this.confirm.confirmText) return false;
  return true;
},
submitConfirm() {
  if (!this.canConfirm()) return;
  if (this.confirm.onConfirm) this.confirm.onConfirm();
  this.confirm.open = false;
}
```

Trigger from any row action:

```html
<button @click="openConfirm({
  intensity: 'danger',
  title: 'Suspend ' + user.email + '?',
  subtitle: 'They will lose IDE access immediately.',
  impact: ['License revoked on next renew', 'Audit row USER_SUSPENDED written'],
  confirmCta: 'Suspend',
  onConfirm: () => suspend(user.id)
})" class="text-sm text-coral-500 hover:underline">Suspend</button>
```

Why the helpers (`canConfirm`, `submitConfirm`) and not inline `@click`:
- The Enter-key shortcut needs the same gating logic the button uses; extracting `canConfirm()` keeps the two in lockstep.
- `submitConfirm()` is the single place that fires the closure and closes the modal. If you ever add post-confirm behaviour (analytics, optimistic UI, undo-toast hand-off), there's one call site.
- The disabled-state CSS (`disabled:opacity-50 disabled:cursor-not-allowed`) tracks `:disabled="!canConfirm()"` instead of inline expressions — easier to read at the call site.

### F. Undo toast with countdown (G16 companion)

After a destructive action, show a temporary toast with an Undo button. The 30-second countdown gives the user a real grace period before the action commits server-side.

Reference implementation: `markup/playground/tailwind/src/main/resources/app/toast.html` ([github](https://github.com/karatelabs/karate-skills)).

**Two reversibility models** — the factory shape is identical, only the closures differ:

| Model | onConfirm fires | onCommit fires | onUndo fires | When to use |
|---|---|---|---|---|
| **Deferred-commit** | nothing yet (optimistic UI only) | timer expires or close clicked → POST to server | undo clicked → restore client-state, never POSTs | Default. True undoability without compensating-action plumbing. |
| **Immediate-commit** | POST commits to server | nothing (commit already happened) | POST a reversal (compensating action) | When the action must be visible to other systems instantly, or the action is hard to "buffer" client-side. |

Pick deferred-commit unless something forces immediate-commit. The recipe below assumes deferred.

```html
<!-- Always-dark toast surface (Gmail / GitHub style) — high-contrast in both themes. -->
<div x-show="undoToast.visible" x-cloak
     class="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-ink-900 text-white rounded-lg shadow-lg flex flex-col min-w-[360px] max-w-[480px] overflow-hidden"
     x-transition:enter="transition ease-out duration-200"
     x-transition:enter-start="opacity-0 translate-y-4"
     x-transition:enter-end="opacity-100 translate-y-0"
     x-transition:leave="transition ease-in duration-150"
     x-transition:leave-start="opacity-100 translate-y-0"
     x-transition:leave-end="opacity-0 translate-y-4">
  <div class="flex items-center gap-4 px-4 py-3">
    <span class="flex-1 text-sm" x-text="undoToast.message"></span>
    <button @click="undo()" class="text-sm font-semibold text-brand-300 hover:text-brand-200">Undo</button>
    <span class="text-xs text-ink-400 font-mono tabular-nums" x-text="undoToast.secondsLeft + 's'"></span>
    <button @click="commit()" class="text-ink-400 hover:text-white p-1 -mr-1 rounded" aria-label="Dismiss">
      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/></svg>
    </button>
  </div>
  <!-- Progress bar shrinks left-to-right as the timer counts down. -->
  <div class="h-0.5 bg-ink-800">
    <div class="h-full bg-brand-400 transition-[width] duration-1000 ease-linear"
         :style="`width: ${(undoToast.secondsLeft / undoToast.total) * 100}%`"></div>
  </div>
</div>
```

In the page factory (see §A):

```js
undoToast: {
  visible: false, message: '', secondsLeft: 0, total: 30,
  onUndo: null, onCommit: null,
},
_undoTimer: null,

showUndoToast({ message, onUndo = null, onCommit = null, seconds = 30 }) {
  // If a previous toast is in flight, fire its onCommit first — the user
  // implicitly accepted it by triggering a new action. Without this,
  // the previous action's commit would be silently orphaned.
  if (this.undoToast.visible) this._fireCommit();
  this.undoToast.message = message;
  this.undoToast.total = seconds;
  this.undoToast.secondsLeft = seconds;
  this.undoToast.onUndo = onUndo;
  this.undoToast.onCommit = onCommit;
  this.undoToast.visible = true;
  if (this._undoTimer) clearInterval(this._undoTimer);
  this._undoTimer = setInterval(() => {
    if (this.undoToast.secondsLeft > 0) this.undoToast.secondsLeft--;
    else this._fireCommit();
  }, 1000);
},
undo() {
  if (this.undoToast.onUndo) this.undoToast.onUndo();
  this._dismiss();
},
commit() {
  // Manual close = accept (timer fast-forward).
  this._fireCommit();
},
_fireCommit() {
  if (this.undoToast.onCommit) this.undoToast.onCommit();
  this._dismiss();
},
_dismiss() {
  this.undoToast.visible = false;
  this.undoToast.onUndo = null;
  this.undoToast.onCommit = null;
  if (this._undoTimer) { clearInterval(this._undoTimer); this._undoTimer = null; }
}
```

Trigger from any destructive action — note the optimistic local update happens before `showUndoToast`, and `onUndo` is responsible for restoring client state:

```js
archive(email) {
  const previous = [...this.inbox];
  this.inbox = this.inbox.filter(e => e.id !== email.id);  // optimistic
  this.showUndoToast({
    message: 'Archived "' + email.subject + '"',
    seconds: 30,
    onUndo:   () => { this.inbox = previous; },
    onCommit: () => fetch('/api/archive/' + email.id, { method: 'POST' }),
  });
}
```

**Chains naturally with §E confirm modal.** For high-impact actions, gate the trigger with a confirm modal first, then offer last-mile reversibility via the toast — `onConfirm` becomes the entry point that calls `showUndoToast`. Use a shorter `seconds` (e.g. 10) for actions that already passed a confirm gate; full 30s for one-click actions.

**Why fire `_fireCommit` on re-trigger:** if a user archives email A, then archives email B before A's toast expires, the previous A toast vanishes when B's toast replaces it. Without firing A's commit, A's optimistic-deletion would persist on the client but never reach the server — the row reappears on next page load. Firing A's `onCommit` immediately preserves the user's intent.

**Edge case: page navigation during the grace window.** This recipe doesn't handle `beforeunload` — if the user closes the tab during a deferred commit, the action is lost. For studio-grade flows where this matters, add a `window.addEventListener('beforeunload', () => this._fireCommit())` in `init()`. For the optimistic-UI common case (Gmail-style archive), accepting that risk is fine; the row reappearing on next load is recoverable.

### G. Tree expand/collapse (group hierarchy)

Reference implementation: `markup/playground/tailwind/src/main/resources/app/tree.html` ([github](https://github.com/karatelabs/karate-skills)). Demonstrates 4-level hierarchy + chevron toggle + cost-centre amber badge + member-count badge + selected-node detail card + expand-all / collapse-all helpers, dark-mode safe.

```html
<ul class="flex-1 overflow-y-auto py-2">
  <template x-for="node in flatTree" :key="node.id">
    <li>
      <div class="flex items-center gap-1.5 px-3 py-2 cursor-pointer text-sm hover:bg-ink-50 dark:hover:bg-ink-800/60"
           :style="`padding-left: ${0.75 + node.depth * 1}rem`"
           :class="selectedId === node.id ? 'bg-brand-50 dark:bg-brand-950 text-brand-900 dark:text-brand-100 font-medium' : 'text-ink-700 dark:text-ink-200'"
           @click="select(node.id)">
        <!-- Chevron — @click.stop is mandatory so toggle doesn't also fire row select -->
        <button @click.stop="toggleExpand(node.id)"
                :class="{'invisible': !node.hasChildren}"
                class="w-4 h-4 shrink-0 text-ink-400 dark:text-ink-500 hover:text-ink-700 dark:hover:text-ink-200">
          <svg class="w-3 h-3 transition-transform" :class="{'rotate-90': expanded[node.id]}"
               fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 0 1 .02-1.06L11.168 10 7.23 6.29a.75.75 0 1 1 1.04-1.08l4.5 4.25a.75.75 0 0 1 0 1.08l-4.5 4.25a.75.75 0 0 1-1.06-.02Z" clip-rule="evenodd"/>
          </svg>
        </button>
        <!-- Folder vs leaf icon. Heroicons outline; size matches chevron. -->
        <svg x-show="node.hasChildren" class="w-4 h-4 shrink-0 text-ink-400 dark:text-ink-500">…folder…</svg>
        <svg x-show="!node.hasChildren" class="w-4 h-4 shrink-0 text-ink-400 dark:text-ink-500">…leaf…</svg>
        <span class="flex-1 truncate flex items-center gap-1.5 min-w-0">
          <template x-if="node.costCentre">
            <span class="text-[10px] font-mono font-semibold text-amber-700 dark:text-amber-300 bg-amber-50 dark:bg-amber-400/10 px-1 py-0.5 rounded shrink-0" x-text="node.costCentre"></span>
          </template>
          <span class="truncate" x-text="node.name"></span>
        </span>
        <span class="text-[11px] text-ink-400 dark:text-ink-500 font-mono tabular-nums" x-text="node.members"></span>
      </div>
    </li>
  </template>
</ul>
```

In the page factory (see §A):

```js
tree: { id: 'root', name: 'Acme Corp', depth: 0, members: 247, children: [/* … */] },
expanded: { root: true, engineering: true },
selectedId: 'root',

get flatTree() {
  // Walk depth-first; only emit nodes whose ancestors are all expanded.
  // Decorate with hasChildren so the chevron can hide on leaves.
  // Spread to avoid mutating the source tree.
  const out = [];
  const walk = (node) => {
    out.push({ ...node, hasChildren: node.children.length > 0 });
    if (this.expanded[node.id]) for (const c of node.children) walk(c);
  };
  walk(this.tree);
  return out;
},
select(id) { this.selectedId = id; },
toggleExpand(id) { this.expanded[id] = !this.expanded[id]; },
expandAll() {
  const walk = n => { if (n.children.length) { this.expanded[n.id] = true; n.children.forEach(walk); } };
  walk(this.tree);
},
collapseAll() {
  // Keep root expanded — collapsing it yields a one-row tree which is jarring.
  this.expanded = { root: true };
}
```

Why the patterns:
- **Indent via `:style="padding-left: …"`** not `pl-{n}` utilities — JIT mode can't generate dynamic class names from runtime expressions (gotcha T1). The inline style is the safe path for arbitrary depth.
- **`@click.stop` on the chevron** is mandatory — without it, clicking the chevron also fires the row's `@click="select(node.id)"` via bubbling. Toggle vs select are two distinct intents.
- **Cost-centre badge** uses the slash-opacity dark variant (`dark:bg-amber-400/10`) for a subtle tinted surface that doesn't blow out on the dark canvas. Avoid `dark:bg-amber-50` here — it's a near-white in dark mode and clashes with the rest of the panel.
- **`tabular-nums`** on the right-aligned member-count badge so digits don't shift width when the tree re-renders.
- **`flatTree` getter** spreads each node — never mutates the source `tree`. Decorations like `hasChildren` and `expanded` (if you choose to inject them) live on the returned shallow copy.

### Tree-picker popover (form input replacing a flat `<select>`)

Use this when a form needs to pick *one* node from a hierarchy and a flat `<select>` with `— ` indent prefixes is too clumsy for the actual depth — typically anything past a single level of nesting. Pairs naturally with §G's tree but trades the always-visible panel for a popover anchored to a button trigger, so it drops into a row cell or a modal field without dominating the layout.

Reference implementation in studio: `src/main/resources/app/team/users.html` (`groupPicker()` factory + `#group-tree-data` JSON island; used in both the row "change group" cell and the add-user modal).

```html
<!-- Anywhere a form input would normally go. The wrapping element listens
     for `group-pick` to react to selection (submit a form, update model,
     etc.) — the picker itself is decoupled from what happens after pick. -->
<div @group-pick.stop="form.groupId = $event.detail.groupId">
  <div x-data="treePicker()" x-init="seed($el)"
       data-initial-id="root" data-initial-name="root"
       class="relative inline-block w-full">
    <button type="button" @click="toggle()"
            class="inline-flex items-center justify-between w-full px-3 py-1.5 text-sm border border-ink-200 dark:border-ink-700 rounded-lg bg-white dark:bg-ink-900 text-ink-900 dark:text-ink-100 hover:bg-ink-50 dark:hover:bg-ink-800 focus:outline-none focus:ring-2 focus:ring-brand-500">
      <span class="truncate" x-text="currentName"></span>
      <svg class="w-4 h-4 text-ink-400" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.168l3.71-3.938a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd"/>
      </svg>
    </button>
    <div x-show="isOpen" x-cloak
         @click.outside="close()"
         @keydown.escape.window="close()"
         class="absolute z-50 top-full left-0 mt-1 w-full max-h-60 overflow-y-auto bg-white dark:bg-ink-900 border border-ink-200 dark:border-ink-700 rounded-lg shadow-lg p-1">
      <template x-for="node in treeNodes" :key="node.id">
        <div :style="`padding-left: ${0.5 + node.depth * 0.875}rem`"
             :class="currentId === node.id ? 'bg-brand-50 dark:bg-brand-950/40 text-brand-700 dark:text-brand-200 font-medium' : 'hover:bg-ink-50 dark:hover:bg-ink-800/40 text-ink-700 dark:text-ink-200'"
             class="px-2 py-1.5 rounded cursor-pointer flex items-center gap-1.5 text-sm"
             @click="pick(node)">
          <!-- folder/leaf icon — same SVG pair as §G -->
          <span class="truncate" x-text="node.name"></span>
        </div>
      </template>
    </div>
  </div>
</div>

<!-- Hydration island lives once per page; many pickers can share it. -->
<script type="application/json" id="tree-picker-data">[
  {"id":"root","name":"Acme","depth":0,"parentId":null,"hasChildren":true},
  {"id":"eng","name":"Engineering","depth":1,"parentId":"root","hasChildren":true},
  {"id":"backend","name":"Backend","depth":2,"parentId":"eng","hasChildren":false}
]</script>
```

The Alpine factory is small and reusable across rows + modals on the same page:

```js
function treePicker() {
  return {
    isOpen: false,
    currentId: 'root',
    currentName: 'root',
    treeNodes: [],
    init() {
      const island = document.getElementById('tree-picker-data');
      if (island && island.textContent) {
        try { this.treeNodes = JSON.parse(island.textContent); }
        catch (e) { this.treeNodes = []; }
      }
    },
    seed(el) {
      // Caller's data-* attributes seed initial state without forcing every
      // row to inline a unique x-data expression. data-auto-open="true" is
      // useful when the picker is itself revealed by an outer toggle and
      // the user shouldn't have to click twice (once to reveal, once to open).
      if (el && el.dataset) {
        if (el.dataset.initialId)   this.currentId   = el.dataset.initialId;
        if (el.dataset.initialName) this.currentName = el.dataset.initialName;
        if (el.dataset.autoOpen === 'true') this.$nextTick(() => { this.isOpen = true; });
      }
    },
    toggle() { this.isOpen = !this.isOpen; },
    open()   { this.isOpen = true; },
    close()  { this.isOpen = false; },
    pick(node) {
      this.currentId = node.id;
      this.currentName = node.name;
      this.close();
      // Bubbles — the parent that wraps this picker decides what to do.
      this.$dispatch('group-pick', { groupId: node.id, groupName: node.name });
    }
  };
}
```

Two parent-side wirings worth knowing — they're the reason this pattern exists:

**Submit-on-pick (row inline edit).** Wrap the picker in the form; the outer listener fills the hidden input and calls `requestSubmit()` so the user gets one-click selection without a separate "go" button.

```html
<form ka:post="/team/users" hx-target="#main-content"
      @group-pick.stop="$el.querySelector('[name=newGroupId]').value = $event.detail.groupId; $el.requestSubmit()">
  <input type="hidden" name="action"      value="moveUserToGroup"/>
  <input type="hidden" name="userId"      th:value="u.userId"/>
  <input type="hidden" name="newGroupId"  th:value="u.groupId"/>
  <div x-data="treePicker()" x-init="seed($el)"
       th:attr="'data-initial-id': u.groupId, 'data-initial-name': u.groupName, 'data-auto-open': 'true'"
       class="relative inline-block">
    <!-- trigger + popover as above -->
  </div>
</form>
```

**Update-model-on-pick (modal field).** Skip the form submit — the picker just updates an Alpine model property the parent owns:

```html
<div @group-pick.stop="form.groupId = $event.detail.groupId">
  <div x-data="treePicker()" x-init="seed($el)" data-initial-id="root" data-initial-name="root">
    <!-- trigger + popover as above -->
  </div>
</div>
```

Why the patterns:
- **Event-out, not prop-in.** The picker exposes its selection through `$dispatch('group-pick', ...)` rather than letting the parent reach into the picker's `currentId`. Alpine has no real props — passing reactive parent state into a child `x-data` only captures the value once at init time. Event-out keeps the picker closed-over its own state and the parent owns the reaction.
- **`seed($el)` from `data-*`** lets the same factory power dozens of row pickers without each one needing a unique `x-data="treePicker({initialId: u.groupId, initialName: u.groupName})"` literal — Thymeleaf-friendly because `th:attr` plugs naturally into `data-*`. Also keeps the JS-vs-template seam at the dataset boundary, which is how IDEs and linters expect Alpine factories to look.
- **`data-auto-open="true"`** is the difference between "click change → click trigger → click node" (three clicks) and "click change → click node" (two). The former is what you get if the picker is wrapped in an outer `x-show="picking"` toggle and has no auto-open hook; the seeded auto-open makes the popover appear in the same gesture that revealed the row form.
- **`@click.outside` + `@keydown.escape.window` together.** Outside-click handles the mouse path; the global Esc handles the keyboard path. Both are mandatory — losing either makes the popover feel sticky on whichever input the user prefers.
- **Hydration via JSON island, not server-rendered options.** Same reason §A uses islands: keeps the template free of template-engine loops over user data, lets multiple pickers on one page share a single index, and survives HTMX swaps that would otherwise wipe per-fragment server state. The island is built once per full-page load from `getGroupHierarchy(teamId)` flattened depth-first.
- **Indent via `:style="padding-left: …"`** — same JIT-mode reason as §G (gotcha T1). `pl-{n}` utilities can't take runtime expressions.
- **`z-50` on the popover, `relative` on the picker root.** The popover is positioned `absolute top-full` against the picker root; `z-50` keeps it above modal content (which is `z-40`/`z-50` itself) and table row hover backgrounds. If the picker lives inside a modal, double-check the popover's `z-` is not below the modal's body — `z-50` matches Tailwind's modal layer, so popovers inside modals work without a custom `z-` class.
- **Fits inside §B modal scope.** The popover's `absolute` positioning is relative to the picker root, so it floats out of the modal's content box without clipping. The modal's `overflow` should not be `hidden` on the body wrapper, or popovers will be cropped — modal panels in this skill use the default `overflow: visible`.

### H. Audit JSON drawer (click-to-expand row)

Reference implementation: `markup/playground/tailwind/src/main/resources/app/audit.html` ([github](https://github.com/karatelabs/karate-skills)). Demonstrates click-to-expand row + pretty-printed JSON + 5-line client-side syntax highlighter + action-category color map + filter chips, dark-mode safe.

```html
<!-- Header row + grid columns: 180px timestamp / 180px action / flex actor / 24px chevron -->
<div class="bg-white dark:bg-ink-900 rounded-xl shadow-card border border-ink-100 dark:border-ink-800 overflow-hidden">
  <div class="px-5 py-2.5 border-b border-ink-100 dark:border-ink-800 bg-ink-50 dark:bg-ink-950/50 text-[11px] font-semibold uppercase tracking-wide text-ink-500 dark:text-ink-400 grid grid-cols-[180px_180px_1fr_24px] gap-4">
    <div>Timestamp</div><div>Action</div><div>Actor</div><div></div>
  </div>
  <ul class="divide-y divide-ink-100 dark:divide-ink-800">
    <template x-for="ev in visibleEvents" :key="ev.id">
      <li>
        <div class="px-5 py-3 grid grid-cols-[180px_180px_1fr_24px] gap-4 items-center cursor-pointer hover:bg-ink-50/50 dark:hover:bg-ink-800/40"
             @click="expanded[ev.id] = !expanded[ev.id]">
          <div class="text-xs font-mono tabular-nums">
            <div class="text-ink-700 dark:text-ink-200" x-text="ev.timestamp"></div>
            <div class="text-ink-400 dark:text-ink-500" x-text="relativeTime(ev.epoch)"></div>
          </div>
          <div>
            <span class="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-semibold"
                  :class="categoryClasses(ev.action)"
                  x-text="ev.action"></span>
          </div>
          <div class="text-sm text-ink-700 dark:text-ink-300 truncate" x-text="ev.actor"></div>
          <svg class="w-4 h-4 text-ink-400 dark:text-ink-500 transition-transform shrink-0"
               :class="{'rotate-90': expanded[ev.id]}"
               fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 0 1 .02-1.06L11.168 10 7.23 6.29a.75.75 0 1 1 1.04-1.08l4.5 4.25a.75.75 0 0 1 0 1.08l-4.5 4.25a.75.75 0 0 1-1.06-.02Z" clip-rule="evenodd"/>
          </svg>
        </div>
        <div x-show="expanded[ev.id]" x-cloak
             x-transition:enter="transition ease-out duration-150"
             x-transition:enter-start="opacity-0"
             x-transition:enter-end="opacity-100"
             class="px-5 pb-4 pl-[208px]">
          <pre class="text-xs font-mono bg-ink-950 dark:bg-black text-ink-100 p-4 rounded-lg overflow-x-auto json"
               x-html="highlight(JSON.stringify(ev.details, null, 2))"></pre>
        </div>
      </li>
    </template>
  </ul>
</div>
```

Add to a `<style>` block (scoped to `pre.json` so it doesn't leak):

```css
pre.json .k { color: #93C5FD; }   /* key — sky-300 */
pre.json .s { color: #86EFAC; }   /* string — green-300 */
pre.json .n { color: #FCD34D; }   /* number — amber-300 */
pre.json .b { color: #F0ABFC; }   /* boolean / null — fuchsia-300 */
```

In the page factory:

```js
events: [/* … */],         // hydrated from server (or hardcoded for demo)
expanded: {},
filter: 'all',

get visibleEvents() {
  if (this.filter === 'all') return this.events;
  return this.events.filter(e => this.category(e.action) === this.filter);
},

category(action) {
  if (action.startsWith('USER_'))    return 'user';
  if (action.startsWith('GROUP_'))   return 'group';
  if (action.startsWith('QUOTA_') || action.startsWith('LICENSE_')) return 'quota';
  if (action.startsWith('SYSTEM_'))  return 'system';
  return 'user';
},
categoryClasses(action) {
  return ({
    user:   'bg-brand-50 dark:bg-brand-950 text-brand-700 dark:text-brand-300',
    group:  'bg-amber-50 dark:bg-amber-400/10 text-amber-700 dark:text-amber-300',
    quota:  'bg-coral-50 dark:bg-coral-400/10 text-coral-500',
    system: 'bg-ink-100 dark:bg-ink-800 text-ink-700 dark:text-ink-200',
  })[this.category(action)];
},

// Lightweight 5-line JSON syntax highlighter. Order matters: escape HTML first
// so attacker-controlled detail payloads can't inject markup; then run the
// regex over the escaped string and wrap each token in a styled span.
highlight(json) {
  const escaped = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return escaped.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let cls = 'n';
      if (/^"/.test(match)) cls = /:$/.test(match) ? 'k' : 's';
      else if (/true|false|null/.test(match)) cls = 'b';
      return '<span class="' + cls + '">' + match + '</span>';
    }
  );
}
```

Why the patterns:
- **`x-html` safety:** `x-html` writes raw HTML into the DOM. The highlighter MUST escape `&`, `<`, `>` before tokenizing, otherwise an attacker-controlled detail payload (e.g. `{ "subject": "<script>…</script>" }`) injects markup. The escape-first / tokenize-second order is non-negotiable.
- **Client-side highlight vs server-side:** the recipe used to suggest a server-side highlighter that emits `<span>` markup with `.k` / `.s` / `.n` classes. Client-side is simpler — keeps the server response a plain `Map<String,Object>`, no template-string concatenation server-side, no risk of double-escaping. Cost is one ~5-line JS function.
- **Action-category color map** scans by prefix, not exact action name — keeps the map open to new actions without code changes. Five new `USER_*` actions inherit the brand colour automatically.
- **Grid layout `grid-cols-[180px_180px_1fr_24px]`** keeps timestamp + action columns fixed-width across rows so the action badges align vertically (visual scanning support). Drawer's `pl-[208px]` aligns to the actor column for visual continuity.
- **Drawer transition is opacity-only.** Safe here because the drawer is a sibling of the row (not nested inside another `x-show`). The compound-opacity rule (gotcha T8) only bites on nested `x-show` chains.
- **Filter chips** are inline pills; one Alpine boolean (`filter`) drives both the chip's active styling and the `visibleEvents` getter — no separate "is selected" state per chip.

### I. ⌘K command palette

Reference implementation: `markup/playground/tailwind/src/main/resources/app/layout.html` (the palette is globally mounted in the layout shell so every page can trigger it; `palette.html` is the explainer page) ([github](https://github.com/karatelabs/karate-skills)).

**Global mount strategy.** Place the palette in the layout shell, not in any page fragment. Three triggers fire it:
1. `Cmd+K` / `Ctrl+K` keystrokes (window-level listener).
2. Topbar Search button (or any button in any page) dispatching `new CustomEvent('open-palette')`.
3. Any page's Alpine code calling `window.dispatchEvent(new CustomEvent('open-palette'))`.

The custom-event bus decouples page-level triggers from the palette's Alpine scope — they live in the layout, the buttons live anywhere.

```html
<!-- Drop just before </body> in layout.html -->
<div x-data="paletteApp()" x-cloak
     @keydown.window.meta.k.prevent="openIt()"
     @keydown.window.ctrl.k.prevent="openIt()"
     @open-palette.window="openIt()">
  <div x-show="isOpen" x-cloak
       class="fixed inset-0 z-50 bg-ink-950/50 flex items-start justify-center pt-24 px-4"
       x-transition:enter="transition-opacity ease-out duration-150"
       x-transition:enter-start="opacity-0"
       x-transition:enter-end="opacity-100"
       @click.self="close()"
       @keydown.escape.window="close()">
    <div x-show="isOpen" x-cloak
         x-transition:enter="transition-transform ease-out duration-150"
         x-transition:enter-start="scale-95"
         x-transition:enter-end="scale-100"
         class="bg-white dark:bg-ink-900 rounded-xl shadow-2xl max-w-xl w-full overflow-hidden">
      <div class="flex items-center gap-3 px-4 py-3 border-b border-ink-100 dark:border-ink-800">
        <svg class="w-5 h-5 text-ink-400 dark:text-ink-500" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"/></svg>
        <input x-model="query" x-ref="paletteInput"
               @keydown.arrow-down.prevent="activeIdx = Math.min(activeIdx + 1, filtered.length - 1)"
               @keydown.arrow-up.prevent="activeIdx = Math.max(activeIdx - 1, 0)"
               @keydown.enter.prevent="activate()"
               type="text" autocomplete="off" spellcheck="false"
               placeholder="Search users, groups, licenses…"
               class="flex-1 text-sm bg-transparent focus:outline-none text-ink-900 dark:text-ink-100"/>
        <kbd class="px-1.5 py-0.5 text-[11px] font-mono bg-ink-100 dark:bg-ink-800 rounded text-ink-600 dark:text-ink-300">esc</kbd>
      </div>
      <div x-show="filtered.length > 0" x-cloak class="max-h-96 overflow-y-auto py-1">
        <template x-for="(item, idx) in filtered" :key="item.id">
          <a :href="item.url"
             @mouseenter="activeIdx = idx"
             @click="close()"
             class="flex items-center gap-3 px-4 py-2 cursor-pointer transition"
             :class="idx === activeIdx ? 'bg-brand-50 dark:bg-brand-950' : 'hover:bg-ink-50 dark:hover:bg-ink-800/50'">
            <span class="text-[10px] font-semibold uppercase tracking-wide w-16" :class="typeClass(item.type)" x-text="item.type"></span>
            <span class="flex-1 min-w-0">
              <span class="block text-sm font-medium text-ink-900 dark:text-ink-100 truncate" x-text="item.label"></span>
              <span class="block text-xs text-ink-500 dark:text-ink-400 truncate" x-text="item.hint"></span>
            </span>
            <span x-show="idx === activeIdx" class="text-[10px] text-ink-500 dark:text-ink-400 font-mono">↵</span>
          </a>
        </template>
      </div>
      <div x-show="filtered.length === 0" x-cloak class="px-4 py-8 text-center text-sm text-ink-500 dark:text-ink-400 italic">No matches.</div>
      <div class="px-4 py-2 border-t border-ink-100 dark:border-ink-800 bg-ink-50 dark:bg-ink-950/50 flex items-center justify-between text-[10px] text-ink-500 dark:text-ink-400">
        <span><span class="font-mono">↑↓</span> navigate · <span class="font-mono">↵</span> select · <span class="font-mono">esc</span> close</span>
        <span x-text="filtered.length + ' / ' + index.length + ' results'"></span>
      </div>
    </div>
  </div>
</div>
```

Factory:

```js
function paletteApp() {
  return {
    isOpen: false,
    query: '',
    activeIdx: 0,
    index: [
      { id: 'u-alice', type: 'User',  label: 'Alice Bauer',  hint: 'alice@acme.com · Engineering / Backend', url: '/users' },
      { id: 'g-eng',   type: 'Group', label: 'Engineering',  hint: 'CC 50000 · 145 members',                 url: '/tree' },
      // … hardcoded for demo; production hydrates from a JSON island per Gotcha #14
    ],

    get filtered() {
      const q = (this.query || '').toLowerCase().trim();
      if (!q) return this.index.slice(0, 10);
      const matches = this.index.filter(i =>
        i.label.toLowerCase().includes(q) ||
        i.hint.toLowerCase().includes(q) ||
        i.type.toLowerCase().includes(q)
      );
      matches.sort((a, b) => this._score(b, q) - this._score(a, q));
      return matches.slice(0, 10);
    },
    _score(item, q) {
      const l = item.label.toLowerCase();
      if (l === q)         return 100;   // exact match
      if (l.startsWith(q)) return 50;    // prefix
      if (l.includes(q))   return 25;    // substring
      return 10;                         // matched on hint or type only
    },

    typeClass(t) {
      return ({
        User:    'text-brand-700 dark:text-brand-300',
        Group:   'text-amber-700 dark:text-amber-300',
        License: 'text-violet-700 dark:text-violet-300',
        Action:  'text-ink-500 dark:text-ink-400',
      })[t] || 'text-ink-500 dark:text-ink-400';
    },

    init() {
      this.$watch('query', () => { this.activeIdx = 0; });
      this.$watch('isOpen', (v) => {
        if (v) this.$nextTick(() => { if (this.$refs.paletteInput) this.$refs.paletteInput.focus(); });
      });
    },

    openIt() { this.isOpen = true; },
    close()  { this.isOpen = false; this.query = ''; this.activeIdx = 0; },

    activate() {
      const item = this.filtered[this.activeIdx];
      if (!item) return;
      this.close();
      if (item.url) window.location.href = item.url;
    },
  };
}
```

Why the patterns:
- **Custom-event bus** (`@open-palette.window`) decouples the palette from the rest of the page. Any button anywhere — across page route changes, fragment swaps, what have you — can trigger the palette without sharing Alpine scope.
- **State naming `isOpen` / `openIt()`** instead of `open` / `open()` — the latter has a method that shadows the boolean state, confusing to read. Renaming makes intent obvious.
- **Ranking: exact > prefix > substring > hint/type-only** matches VSCode / Linear / Cursor convention. Bumping the score thresholds is cheap if a future workload demands fuzzy matching (fzy / fzf). Don't ship fuzzy until the substring approach starts surfacing irrelevant results.
- **`@mouseenter` keeps mouse + keyboard agreed** on `activeIdx`. Without it, the keyboard-highlighted row and the mouse-hovered row diverge and users see two competing focus states.
- **Auto-focus** via `$watch('isOpen')` + `$nextTick` — the input must exist in the DOM before `.focus()` fires; one tick after `x-show` flips true is enough.
- **In-memory index, capped at ~500/surface.** For larger sets, hydrate from a JSON island (Gotcha #14) per page or build a server-side search endpoint. The factory contract stays the same; only the source of `index` changes.

## Forms (Tailwind recipe)

No `.form-control` equivalent — build via utilities:

```html
<form ka:post="this" hx-target="#main-content" class="space-y-4 max-w-md">
  <div>
    <label class="block text-sm font-medium text-ink-700 mb-1">Email</label>
    <input type="email" name="email"
           class="block w-full px-3 py-2 text-sm border border-ink-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500"/>
  </div>
  <div>
    <label class="block text-sm font-medium text-ink-700 mb-1">Role</label>
    <select name="role"
            class="block w-full px-3 py-2 text-sm border border-ink-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500">
      <option value="user">User</option>
      <option value="admin">Admin</option>
    </select>
  </div>
  <div class="flex items-center gap-2">
    <input type="checkbox" id="notify" name="notify"
           class="w-4 h-4 text-brand-700 border-ink-300 rounded focus:ring-brand-500"/>
    <label for="notify" class="text-sm text-ink-700">Send notifications</label>
  </div>
  <button type="submit"
          class="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-700 hover:bg-brand-800 rounded-lg shadow-sm">
    Save
  </button>
</form>
```

**Form binding via `ka:data`** works identically to Bootstrap — see `forms.md §Alpine Form Binding`. Just substitute the active/inactive button classes:

```html
<form ka:data="form:initialForm" ka:post="this" hx-target="#main-content">
  <button type="button" x-on:click="form.role='user'"
          x-bind:class="form.role=='user'
            ? 'inline-flex items-center px-3 py-1.5 text-sm font-semibold text-white bg-brand-700 rounded-lg'
            : 'inline-flex items-center px-3 py-1.5 text-sm font-medium text-ink-700 bg-white border border-ink-200 rounded-lg hover:bg-ink-50'">
    User
  </button>
  <!-- …same for Admin -->
</form>
```

### Validation re-render with preserved input (canonical Tailwind pattern)

Reference implementation: `markup/playground/tailwind/src/main/resources/app/form.html` ([github](https://github.com/karatelabs/karate-skills)). Demonstrates a realistic edit-team form with five validation rules, error preservation, flash success, and a "last server-received POST" panel that shows the round-trip end-to-end.

```html
<script ka:scope="global">
  _.errors = [];
  _.flashSuccess = null;
  if (request.post) {
    var posted = request.paramJson('form');
    if (!posted.name || posted.name.length < 2) _.errors.push('Team name must be at least 2 characters.');
    if (!posted.expiryDate || new Date(posted.expiryDate) <= new Date()) _.errors.push('Expiry date must be in the future.');
    // …more rules
    if (_.errors.length === 0) _.flashSuccess = 'Saved.';
    _.initialForm = posted;   // ALWAYS preserve user input
  }
</script>
<script ka:scope="local">
  if (!_.initialForm) _.initialForm = { name: '', expiryDate: '', /* …defaults */ };
</script>

<!-- Flash success: emerald banner -->
<div th:if="flashSuccess" class="rounded-lg border border-emerald-200 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950 px-4 py-3">
  <p class="text-sm text-emerald-800 dark:text-emerald-200">
    <span class="font-semibold">Saved.</span> <span th:text="flashSuccess"></span>
  </p>
</div>

<!-- Validation errors: coral panel -->
<div th:if="errors && errors.length > 0" class="rounded-lg border border-coral-300 dark:border-coral-400/40 bg-coral-50 dark:bg-coral-400/10 px-4 py-3">
  <p class="text-sm font-semibold text-coral-500 mb-1">Couldn't save — please fix:</p>
  <ul class="text-sm text-coral-500 list-disc pl-5 space-y-0.5">
    <li th:each="err: errors" th:text="err"></li>
  </ul>
</div>

<form ka:data="form:initialForm" ka:post="this" hx-target="#main-content">
  <input x-model="form.name" type="text" required class="block w-full px-3 py-2 …"/>
  <input x-model.number="form.quotas[tier]" type="number" min="0" class="…"/>
  <!-- … -->
  <button type="submit" class="px-3 py-2 text-sm font-semibold text-white bg-brand-700 hover:bg-brand-800 rounded-lg">Save</button>
</form>
```

Why the patterns:
- **Always set `_.initialForm = posted` on POST**, even when validation fails — never blank-out the form on errors. The user fixes the flagged fields and resubmits with the rest intact.
- **`x-model.number`** coerces input strings to numbers so server-side comparisons (`v < 0`) don't need `parseInt`. The `.number` modifier is Alpine-native.
- **Radio-pickers as click-able label cards:** wrap a hidden `<input type="radio" class="sr-only">` in a `<label>` styled as a card; `x-bind:class` toggles the active styling based on `form.licenseMode`. Keeps semantics correct (real radios for mutually-exclusive choice + keyboard nav) while letting Tailwind class-toggle the visual.
- **Flash + errors panels** use the established colour vocabulary: emerald for success, coral for validation, brand for info. All have dark-mode variants.
- **CSRF:** add `<meta th:if="context.csrf" name="csrf-token" th:content="context.csrf.token"/>` in the layout `<head>`; `pub/app.js`'s `htmx:configRequest` listener reads it and adds the `X-CSRF-Token` header automatically. Without the meta tag, POSTs return 403 once `csrfEnabled=true` (the default).

## HTMX patterns under Tailwind

The HTMX mechanics (partial swaps, `ka:get`/`ka:post`, error toasts) are CSS-agnostic. The Tailwind-specific differences:

- **Swap target visual hint**: wrap the swap target in a relative-positioned div with the loading indicator absolutely positioned. The `.htmx-request` class is automatically added by HTMX during in-flight requests:
  ```html
  <div id="results" class="relative">
    <div class="htmx-indicator absolute inset-0 bg-white/60 flex items-center justify-center">
      <div class="animate-spin h-6 w-6 border-2 border-ink-300 border-t-brand-700 rounded-full"></div>
    </div>
    <!-- swapped content -->
  </div>
  ```
  with CSS:
  ```css
  .htmx-indicator { display: none; }
  .htmx-request .htmx-indicator { display: flex; }
  ```

- **Error toast (top-right)**:
  ```html
  <div id="htmx-error-toast"
       class="hidden fixed top-4 right-4 z-50 max-w-sm bg-coral-500 text-white px-4 py-3 rounded-lg shadow-lg"
       role="alert"></div>
  ```
  In the JS handler from `Skill.help('markup')` §HTMX error surface, use `host.classList.remove('hidden')` / `add('hidden')`.

## Icons

The prototype uses inline SVGs (Heroicons-style). No icon font dependency. Per-icon paths are short enough to inline; for high-frequency icons, define a fragment:

```html
<!-- components/icons.html -->
<svg th:fragment="check" class="w-4 h-4 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5"/>
</svg>
<svg th:fragment="x" class="w-4 h-4 text-coral-500" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/>
</svg>
```

Reference Heroicons (https://heroicons.com/) for the SVG path data. The prototype uses 24x24 viewBox + `stroke-width="1.8"` for outline icons.

## Tailwind-specific gotchas

### T1. JIT mode + dynamic class names

The Tailwind CDN ships JIT compilation. **Dynamic class names where the class string is constructed at runtime won't resolve** because JIT scans the source for static class names:

```html
<!-- WRONG — JIT can't see "bg-coral-500" as a literal -->
<div :class="`bg-${color}-500`">…</div>

<!-- RIGHT — full class names appear as literals -->
<div :class="color === 'coral' ? 'bg-coral-500' : 'bg-brand-500'">…</div>
```

Same rule for `th:class` server-side: emit complete literal class strings, not template-string fragments.

### T2. No `!important` overrides — Alpine `x-show` works naturally

Tailwind's utility classes do NOT use `!important`. So Alpine's `x-show` (which sets inline `display: none`) is reliable — no need for the `:class` workaround that Bootstrap requires. (See Gotcha #10 in `Skill.help('markup')` and B1 in `CSS_BOOTSTRAP.md` for the Bootstrap version of this.)

### T3. `hidden` class shadowing `[hidden]` attribute

Tailwind's `hidden` utility is `display: none`. Alpine doesn't add a `hidden` attribute; it sets inline `display: none`. They co-exist fine, but if you have an element with `class="hidden"` and try to show it via JS, you must `.classList.remove('hidden')` — setting `style.display = 'block'` won't override the `hidden` class because both have the same specificity and the class wins via document order.

### T4. CDN is for prototypes only

The `cdn.tailwindcss.com` script ships the entire Tailwind ruleset (~3 MB JS), JIT-compiles in the browser on every page load (~50ms of CSS work per cold page), and logs `cdn.tailwindcss.com should not be used in production` to the console. Use it only for one-page demos / playgrounds.

For anything shipped, use the standalone CLI per §Setup → Production. The compiled bundle is typically 30-50 KB minified, cached forever via your asset-versioning strategy, and frees you from the cascade ordering problem covered in T7.

### T5. Custom font loading

Inter and JetBrains Mono load via Google Fonts. The `<link rel="preconnect">` lines reduce font-fetch latency by 50–100ms — keep them. Without preconnect, the first paint shows the system fallback for ~200ms before swapping (FOUT). For zero-flash, self-host the fonts and reference via `@font-face` in `pub/app.css`.

### T6. `[x-cloak]` is mandatory

Always include `[x-cloak] { display: none !important; }` in your CSS (already in the recommended `<style>` block above) and add `x-cloak` to any element with initially-false `x-show`. Without it, elements briefly render visible during Alpine's defer-load window.

### T7. Cascade ordering: where to put `pub/app.css` relative to Tailwind

How you load Tailwind decides whether you need `!important` on your `pub/app.css` overrides.

**Production path (static stylesheet) — load `pub/app.css` AFTER `pub/tailwind.css`.** Both are static `<link>` tags at known positions in your `<head>`, so the cascade is yours to control. At equal specificity, later-in-cascade wins:

```html
<link href="/pub/tailwind.css" rel="stylesheet"/>   <!-- defines .inline-block, .hidden, etc. -->
<link href="/pub/app.css" rel="stylesheet"/>        <!-- your overrides win at equal specificity -->
```

With this order, `.ka-indicator { display: none }` in `pub/app.css` correctly hides spinners that also carry Tailwind utilities like `inline-block`. No `!important` needed.

**Prototype path (CDN) — `!important` is required.** The CDN script processes the page on first parse and injects its compiled rules into a `<style>` tag at the **end of `<head>`**, AFTER any `<link href="/pub/app.css">` you wrote earlier. Same specificity, later in cascade — Tailwind always wins:

```css
/* pub/app.css */
.ka-indicator { display: none; }              /* (0,1,0) */
```
```css
/* CDN-injected at end of <head> — same specificity, later */
.inline-block { display: inline-block; }      /* (0,1,0) */
```

```html
<span class="ka-indicator inline-block w-3 h-3 ... animate-spin"></span>
```
→ spinner is **always visible** under the CDN. Symptom: every page-action button has a constantly spinning loader. (Inverse of the Bootstrap `d-none !important` problem in `Skill.help('markup')` Gotcha #10.)

**Fixes (CDN path only):**

1. **Don't put `inline-block` (or `block` / `flex` / similar display utilities) on a `ka-indicator` element.** The `.htmx-request .ka-indicator { display: inline-block }` rule applies the right display when the request is in flight; the default state needs to be `none`.

   ```html
   <!-- WRONG — `inline-block` overrides the default-hidden rule -->
   <span class="ka-indicator inline-block w-3 h-3 border-2 ... animate-spin"></span>

   <!-- RIGHT — let pub/app.css control display via the htmx-request class -->
   <span class="ka-indicator w-3 h-3 border-2 ... animate-spin"></span>
   ```

2. **Mark both rules `!important` in `pub/app.css`** as a backstop:

   ```css
   .ka-indicator                  { display: none         !important; }
   .htmx-request .ka-indicator    { display: inline-block !important; }
   .htmx-request .ka-indicator-hide { display: none       !important; }
   ```

**Verification:** open DevTools on a page with action buttons and confirm every `.ka-indicator` element has `display: none` in computed styles. Click a button that posts via HTMX — during the request the indicator should switch to `display: inline-block`. After response, back to `display: none`.

**Migration note:** if you're converting from CDN to production, you can drop the `!important` flags from your `pub/app.css` once the cascade order (`tailwind.css` → `app.css`) is in place. They're harmless to leave but no longer load-bearing.

### T8. Don't compound opacity across nested `x-show` transitions

When a modal/drawer has a backdrop wrapper and a card child both with `x-show` and both animating opacity, the card's effective opacity is `card * backdrop` — multiplicative blending makes the card fade visibly faster than either timing curve alone, and any tiny duration mismatch between the two transitions surfaces as a flicker frame on close. Symptoms: the card appears to "snap" out a beat before the backdrop finishes fading, or a one-frame brightness pop at the end of the leave transition.

**Rule:** in a nested-modal pattern, **outer transitions opacity only** (backdrop fade), **inner transitions transform only** (card scale). The card visually fades because it's inside the fading overlay; you don't need to also animate its opacity. Use Tailwind's `transition-opacity` and `transition-transform` longhand utilities (not the bare `transition` class — that transitions everything) so each element animates exactly one property:

```html
<!-- Outer: opacity only -->
x-transition:leave="transition-opacity ease-in duration-150"
x-transition:leave-start="opacity-100"
x-transition:leave-end="opacity-0"

<!-- Inner: transform only -->
x-transition:leave="transition-transform ease-in duration-150"
x-transition:leave-start="scale-100"
x-transition:leave-end="scale-95"
```

The recipe in §B "Modal overlay" follows this rule. Same advice applies to right-side drawers (§C — already split: backdrop is a sibling element, drawer slides via transform).

**Sub-rule:** prefer longhand `x-transition:enter` / `:leave` over Alpine's shorthand `x-transition.opacity`. The shorthand applies Alpine's default duration which can subtly drift from your inner element's explicit `duration-150`; longhand pins both durations symmetrically.

### T9. Active-state utility collisions — put the base class on a wrapper, not the toggleable item

When a toggle/tab/chip set switches between active and inactive styling by overriding the same property, two utility classes for the same property end up in the merged class list — same specificity, source-order wins. Tailwind's CDN doesn't guarantee a stable order between `bg-white` and `bg-ink-50`; the active state silently fails to show.

```html
<!-- WRONG — `bg-ink-50` (base) and `bg-white` (active) both apply.
     Source order in the JIT-emitted stylesheet decides which wins, and
     it's usually NOT the one you want. Active state appears identical
     to inactive. -->
<a class="px-3 py-1 text-xs font-medium rounded-md border bg-ink-50 dark:bg-ink-950"
   th:classappend="active ? 'bg-white dark:bg-ink-900 text-ink-900 shadow-sm' : 'text-ink-600 hover:text-ink-900'">JSON config</a>
```

**Fix:** put the wrapper background on a parent element; only the active item gets a foreground (e.g. `bg-white`). No collision because base and active set background on different elements:

```html
<!-- RIGHT — segmented control. Wrapper has the gray bg; only active pill has white bg. -->
<div class="inline-flex items-center gap-1 p-1 rounded-lg bg-ink-100 dark:bg-ink-800">
  <a th:classappend="active ? 'bg-white dark:bg-ink-900 text-ink-900 shadow-sm' : 'text-ink-600 hover:text-ink-900'"
     class="px-3 py-1 text-xs font-medium rounded-md transition cursor-pointer">JSON config</a>
  <a ...>Markdown rewrite</a>
</div>
```

Same shape used by `components/filter-chip.html` (segmented status filter), the diff page's pair toggle, and the playground tabs strip. **Heuristic:** if you find yourself toggling `bg-X` vs `bg-Y` on the same element, the structure is wrong — lift one bg to a wrapper.

**Other collision pairs to watch for:**
- `text-ink-700` vs `text-ink-100` — won't fail silently the same way (specificity is equal but visible if either one wins), but the design intent gets lost when source-order picks the inactive color.
- `border-ink-200` vs `border-brand-500` — same.
- `shadow-sm` vs nothing — works because nothing means no rule, no collision.

The general rule: **when active and inactive set the same CSS property, pull the inactive value out of the toggleable element.** Either move the base to a wrapper (segmented control), or omit the base entirely so only the active rule emits a value (chip/badge variant — base has no `bg-*`).
