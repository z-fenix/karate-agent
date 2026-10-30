# Shadow pages — `app/<page>.html`

A file in the project's `app/` replaces the base page of the same path. Write one only for a layout the overlay cannot express. A page naming no base page is a `UI_UNKNOWN` finding.

| page | route | reads |
|---|---|---|
| `layout.html` | the shell around every page | `app.ui` |
| `index.html` | `/` — the table and the creating action | `app.worlds()` · `app.creates()` |
| `detail.html` | `/worlds/{id}` — a card and the action shell | `app.world(id)` |
| `timeline.html` | `/worlds/{id}/timeline` | `app.timeline(id)` |
| `docs.html` | `/docs` — each operation with a try-it | `app.docs()` |
| `components/action.html` | one action form | a form from the above |
| `components/result.html` | a result card, and `:: value` for one field | `app.act(...)`'s answer |
| `components/exchange.html` | the folded exchange with curl | a result's `exchange` |

Where the base's fixed words live: the header and its subtitle — `layout.html`; an action's status (*Available*, *Not available*, *Refusal expected*) — `components/action.html`; *Done* / *Refused* and what changed — `components/result.html`. Titles, labels and wording belong in `ui.js`; the exchange's `POST … → 201` line and a refusal's code are the proof, so keep them.

Start from the base page (`META-INF/karate/app/<page>` in the jar) and keep its `ka:scope` script: a POST calls `app.act(world, command, request.paramJson('form'))` and redirects. That call is the only send. The markup stack is `Skill.help('markup')`.

A page sees `app`, and the markup runtime's `request`, `response`, `context` and `session`. Nothing else: no console global, no `Java`, no read outside `app/` and the base — `context.read` and every include resolve inside those two roots, and a symbolic link inside `app/` is never followed. A page that writes a response header is refused `APP_HEADER_REFUSED`.
