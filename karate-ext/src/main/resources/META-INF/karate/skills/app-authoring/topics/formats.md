# `fields.<name>.format` — display only

A format changes how a value is shown, never the value: nothing is rounded, converted or defaulted, and the folded exchange under every result still shows the bytes sent. It formats what the app displays — a card, a table, what changed — never a form's input box, which takes the plain value.

| format | a value renders as |
|---|---|
| `money` | digits grouped by thousands, the decimals as sent — `2000` → `2,000`, `1234.5` → `1,234.5`; with `currency: 'USD'`, `$2,000` |
| `percent` | the value as sent with `%` — `15` → `15%` (never multiplied) |
| `date` | the text as sent, in a `<time datetime>` element |
| `badge` | the text as sent, as a pill — for a state field |

Any other `format` is `UI_UNKNOWN`. A field with no format shows its value as text, a boolean as `Yes` / `No`; a missing value shows `—`.

**`currency`** is the business's, not a guess: read it from the requirements or the source-of-truth document (`File.read`) before setting it. Not an ISO 4217 code is `UI_INVALID`.
