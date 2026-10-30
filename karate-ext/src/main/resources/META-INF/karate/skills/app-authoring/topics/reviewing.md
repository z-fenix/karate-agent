# Reviewing — read the page the person reads

Fetch the rendered HTML (`Http.get(app.url + path, {full: true})`) and read its markers:

| marker | says |
|---|---|
| `data-command="reopen" data-enabled="false"` + `data-reason` | the action and whether `Twin.next` enables it, with the reason's wording |
| `data-badge="unavailable"` | the status's code; the pill reads *Not available* |
| `data-expects="reopen_window_closed"` | enabled, but the model expects `apply` to refuse it — still sendable |
| `data-result data-outcome="refused"` + `data-refusal` | the last action's result: `applied` · `refused` · `error` · `invalid` |
| `data-changes` · `data-change="reserve"` | the fields that action changed, before → after |
| `data-format="money"` · `data-value="OPEN"` | a field shown with its format · the member behind a value label |
| `data-exchange-json` · `data-curl` | the exchange as recorded, and the curl that replays it |
| `data-record` · `data-play-link` | the world's play record id, and its console link |
| `data-ui-error="UI_UNKNOWN"` | the overlay is broken; the page names the key |

To act as the person does, post the page's own form — or drive it with `Robot`: open `app.url`, fill the action's fields, press its button, read the result. Every action lands in the world's record; a reviewer confirms that record in the console, never in the app.
