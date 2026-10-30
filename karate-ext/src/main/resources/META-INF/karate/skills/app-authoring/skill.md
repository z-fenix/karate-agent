# App authoring — the playground app a business user reads

`Http.app` serves a twin-bound rulebook as screens over its recording mock; every action is an exact call through the twin's binding. You change how the app reads, never what it sends.

| about to… | call |
|---|---|
| relabel fields, values, actions or reads; add help or refusal wording | `Skill.help('app-authoring', 'overlay')` |
| show money, a date, a percent, a state badge; pick the currency | `Skill.help('app-authoring', 'formats')` |
| change a page's layout | `Skill.help('app-authoring', 'shadow-pages')` |
| read the result the way a reviewer sees it | `Skill.help('app-authoring', 'reviewing')` |

## The loop

1. **Check** — `Twin.check(rule).findings.ui` lints `app/ui.js` and `app/*.html` without a boot. A finding is `{id, kind, message}`: `UI_UNKNOWN` — a name the model does not declare, or no such format; `UI_INVALID` — a value of the wrong shape. Any finding fails `ci`.
2. **Run** — `var mock = Http.mock({twin: rule, record: true})`, then `var app = Http.app({twin: rule, target: mock})`; `console: '<console url>'` links each timeline to its record.
3. **Read the page** — `Http.get(app.url + path, {full: true}).body` for `/`, `/worlds/<id>`, `/worlds/<id>/timeline`, `/docs`. A world page needs a world: send the creating request (`/docs` shows it) to the mock.
4. **Edit** — `File.write('app/ui.js', src)`; over HTTP, `POST /api/write?path=app/ui.js` with the file as the body. A served app reads `ui.js` and pages live: write, then refresh — no restart. A broken overlay renders a page naming the key. Go to 1.

## Three levels

1. **Derived** — no file: the model's names spaced (`lossDate` → `Loss date`), refusals as reason codes.
2. **`app/ui.js`** — one literal, `ui = {...}`, keyed by the model's names.
3. **Shadow pages** — `app/<page>.html` replaces that base page.

## The one rule

The overlay names; it never types. An argument's type and domain are the twin's, the route and body the binding's. Every submit is the page's `app.act(...)` through the binding, so no page, overlay or shadow can send a request the model would not. The app writes nothing: confirming and promoting a record happen in the console's play view (`Skill.help('twin-authoring', 'play')`).
