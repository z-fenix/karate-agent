# Components

> Fragments are components. `th:insert` with `th:with` parameters. No JS component framework needed.

## Directory Convention

```
app/
  index.html              <- layout shell
  header.html             <- nav bar
  dashboard.html          <- page
  session.html            <- page
  components/
    badge.html            <- reusable fragment
    modal.html            <- reusable fragment
    pagination.html       <- reusable fragment
  pub/
    app.css               <- custom styles
```

Pages live at the root level. Reusable fragments live in `components/`. Static assets (CSS, images) live in `pub/`.

## Defining a Component

Use `th:fragment` to define a named fragment. Parameters come in as template variables via `th:with`:

```html
<!-- components/badge.html -->
<!-- Concrete state classes are CSS-framework-specific. See CSS_BOOTSTRAP.md / tailwind.md. -->
<span th:fragment="badge"
      th:class="'badge ' + (state == 'ready' ? '<success>' :
                             state == 'running' ? '<primary>' :
                             state == 'error' ? '<danger>' :
                             state == 'stopped' ? '<warning>' : '<secondary>')"
      th:text="state">state</span>
```

## Using a Component

```html
<!-- Insert: keeps the host element, inserts fragment inside — th:with on same element works -->
<span th:insert="components/badge :: badge" th:with="state: session.state"></span>

<!-- Replace: host element is removed, so th:with must be on a PARENT element -->
<span th:with="state: session.state"><span th:replace="components/badge :: badge"></span></span>
```

**`th:insert` vs `th:replace`:**
- `th:insert` — fragment is inserted *inside* the host element (host element remains). `th:with` works on the same element.
- `th:replace` — host element is *replaced* by the fragment. `th:with` on the same element is lost (the element is removed before variables take effect). Put `th:with` on a parent element instead.

Use `th:replace` when you want the fragment's root element to be the final element. Use `th:insert` when you want to wrap the fragment in a container.

## Multiple Parameters

Pass multiple parameters with comma-separated `th:with`:

```html
<span th:replace="components/badge :: badge"
      th:with="state: session.state, size: 'sm', tooltip: session.error"></span>
```

The fragment can use all parameters.

## Multiple Fragments Per File

A component file can define multiple fragments:

```html
<!-- components/icons.html — concrete icon classes are CSS-framework-specific (Bootstrap Icons,
     inline SVGs, or other). See CSS_BOOTSTRAP.md / tailwind.md. -->
<i th:fragment="check" class="<your-success-icon-class>"></i>
<i th:fragment="error" class="<your-danger-icon-class>"></i>
<i th:fragment="loading" class="<your-spinner-class>"></i>

<!-- Usage -->
<span th:insert="components/icons :: check"></span>
<span th:insert="components/icons :: error"></span>
```

## Whole-File Fragment (No `th:fragment` Name)

If a file has no `th:fragment` attribute, the entire file is the fragment. Reference it without `::`:

```html
<!-- components/footer.html (no th:fragment defined) -->
<footer class="text-center">
  <small>Built with Karate Markup</small>
</footer>

<!-- Usage — just the file name, no :: -->
<div th:insert="components/footer"></div>
```

## Component with Server-Side Logic

Components can include `ka:scope` for data preparation:

```html
<!-- components/user-list.html -->
<div th:fragment="list">
  <script ka:scope="local">
    _.users = db.getUsers(teamId); // teamId passed via th:with
  </script>
  <table th:if="users.length > 0">
    <tr th:each="user: users">
      <td th:text="user.name">Name</td>
      <td th:text="user.email">Email</td>
    </tr>
  </table>
  <p th:unless="users.length > 0">No users.</p>
</div>

<!-- Usage -->
<div th:insert="components/user-list :: list" th:with="teamId: team.id"></div>
```

## Components Inside `th:each`

Components work naturally inside loops:

```html
<table>
  <tr th:each="session: sessions">
    <td th:text="session.name">Name</td>
    <td>
      <span th:with="state: session.state"><span th:replace="components/badge :: badge"></span></span>
    </td>
    <td>
      <button ka:post="this" ka:vals="action:'delete',id:session.id"
              hx-target="#main-content" hx-confirm="Delete?">
        Delete
      </button>
    </td>
  </tr>
</table>
```

## Head Component (CDN Block)

Extract the CDN block into a reusable head fragment to ensure consistency. Concrete CSS framework CDN lines are in CSS_BOOTSTRAP.md / `Skill.help('markup', 'tailwind')`.

```html
<!-- components/head.html -->
<head th:fragment="head">
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <!-- CSS framework CDN goes here — see CSS_*.md -->
  <script src="https://unpkg.com/htmx.org@2.0.4"></script>
  <script defer src="https://unpkg.com/alpinejs@3.14.3/dist/cdn.min.js"></script>
  <link href="/pub/app.css" rel="stylesheet"/>
</head>

<!-- Usage in index.html -->
<html>
<head th:replace="~{components/head :: head}"></head>
<body>...</body>
</html>
```

Note absolute `/pub/app.css` (Gotcha #16 in `Skill.help('markup')`).

## Pagination Component

Server-side pagination with HTMX navigation. The server prepares page data in `ka:scope`, renders the current page with `th:each`, and the pagination nav uses `ka:get` with page params.

```html
<!-- components/pagination.html -->
<nav th:fragment="pagination" th:if="totalPages > 1" aria-label="pagination">
  <ul>
    <li th:class="page == 1 ? 'disabled' : ''">
      <a ka:get="this" hx-target="#main-content"
         ka:vals="'page':page - 1, 'sort':sort, 'dir':dir">Previous</a>
    </li>
    <li th:each="p: pages" th:class="p == page ? 'active' : ''">
      <a ka:get="this" hx-target="#main-content"
         ka:vals="'page':p, 'sort':sort, 'dir':dir" th:text="p">1</a>
    </li>
    <li th:class="page == totalPages ? 'disabled' : ''">
      <a ka:get="this" hx-target="#main-content"
         ka:vals="'page':page + 1, 'sort':sort, 'dir':dir">Next</a>
    </li>
  </ul>
</nav>
```

The `ka:vals` on each page link passes current sort/direction so pagination preserves the user's sort choice. CSS-framework-specific class polish (active/disabled visual states) lives in your `CSS_*.md`.

**Server-side data prep:**
```html
<script ka:scope="global">
  var pageSize = 20;
  _.page = parseInt(request.param('page')) || 1;
  _.sort = request.param('sort') || 'created';
  _.dir = request.param('dir') || 'desc';
  var all = utils.getSessions(_.sort, _.dir);
  _.totalPages = Math.ceil(all.length / pageSize);
  _.pages = [];
  for (var i = 1; i <= _.totalPages; i++) _.pages.push(i);
  _.sessions = all.slice((_.page - 1) * pageSize, _.page * pageSize);
</script>

<table>
  <tr th:each="session: sessions">...</tr>
</table>
<div th:replace="components/pagination :: pagination"
     th:with="page: page, totalPages: totalPages, pages: pages, sort: sort, dir: dir"></div>
```

## Visual components — Modal, Carousel, Popover/Tooltip

These are CSS-framework-coupled — both the markup and the JS-init pattern depend on what your framework provides. See:

- CSS_BOOTSTRAP.md — Bootstrap 5 modal (with `bootstrap.Modal` API), carousel, popover/tooltip recipes, plus the `htmx:afterSwap` re-init pattern for components inside swap targets.
- `Skill.help('markup', 'tailwind')` — Alpine `x-show` + `x-transition` modal pattern (no separate framework JS), Alpine-driven carousel, hover-popover via Alpine `x-data="{ open: false }"`. Tailwind has no JS component runtime — overlays are just `fixed inset-0 z-50` divs gated on Alpine state.
