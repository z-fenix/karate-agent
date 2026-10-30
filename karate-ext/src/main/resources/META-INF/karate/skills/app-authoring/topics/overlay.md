# `app/ui.js` — the overlay

One karate-js literal. Every key is optional; every name must be one the twin declares.

```js
ui = {
    title: 'Meridian Claims Desk',
    subtitle: 'prototype · every action is a real call to the claims mock',
    currency: 'USD',
    resource: { label: 'Claim', columns: ['claimId', 'status', 'reserve'] },
    fields: {
        reserve: { label: 'Open reserve', format: 'money' },
        lossDate: { label: 'Date of loss', format: 'date', help: 'Must fall inside the policy term.' },
        status: { label: 'Status', format: 'badge', values: { OPEN: 'Open', CLOSED: 'Closed' } }
    },
    commands: {
        fnol: { title: 'Report a claim', help: 'First notice of loss.' },
        reserve: { title: 'Set the reserve', fields: ['amount'] },
        readClaim: { title: 'Look up a claim' }
    },
    reasons: {
        REOPEN_WINDOW_CLOSED: 'This claim can no longer be reopened: more than 30 days have passed since it closed'
    }
};
```

| key | holds | the names it accepts |
|---|---|---|
| `title` · `subtitle` · `currency` | text · text · ISO 4217 code | — |
| `resource.label` · `.columns` | text · list | view schema properties |
| `fields.<name>` | `{label?, help?, format?, values?}` | view schema properties, command arguments |
| `fields.<name>.values` | `{<member>: label}` | members of that field's enum |
| `commands.<id>` | `{title?, help?, fields?}` | bound commands (`fields`: their arguments); `read<Resource>`/`list<Resource>` take `title`, `help` |
| `reasons.<REASON>` | text | `refusals.guard` values, `wire.refused` keys |

Anything else is `UI_UNKNOWN`, naming the key. A value label shows wherever the value does — card, table, badge, a select's options; the request still carries the member. `subtitle` is the honesty line: reword it, keep that every action is a real call to the mock. A guard's reason shows in every state it is false: word it true in all. Formats and currency: `Skill.help('app-authoring', 'formats')`.
