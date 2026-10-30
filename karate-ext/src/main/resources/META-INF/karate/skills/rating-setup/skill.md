# Rating set-up — a rate book from the carrier's sources

Build the rulebook yourself, now, through eval calls: perform each step with its call, and reply only when the export is done — never with a plan.

`File.*` paths start at the project root (`rulebooks/<book>/sources/…`); `Rule.*` paths at the book (`sources/…`).

1. Call `Skill.help('rating-setup', 'setup')` and do every step: skeleton, import mappings, baseline `tables/`.
2. Call `Skill.help('rating-setup', 'revision')` and do every step: each circular or deviation as a change set, edition records, `rule.json`.
3. Call `Skill.help('rating-setup', 'algorithm')` and do every step: `calc.js` from the rules-manual page.
4. Export (below). `Rule.help()` has every verb's arguments.

## The review rail

- Mark every draft `ai`: `"ai": true` in each import mapping and `rule.json`; `const ai = true;` as the first line of `calc.js`. `Rule.import` and `Rule.changeSet` carry it on. Never remove a marker; a human approves.
- Never draft or edit the carrier mapping `pc.json`. It lives at `rulebooks/<book>/evidence/mappings/pc.json`; if it is elsewhere, `File.copy` it there unchanged.
- Register every judgement (a source clause with more than one reading) as one `assumptions.md` entry: list every reading, choose one.

## assumptions.md

Exactly these four fields per entry:

```markdown
## A-1
- source clause: <source>, <clause id as it prints it>
- affected artifact: <the file the reading shapes>
- readings considered: A — <one reading>; B — <another>
- reading chosen: A
```

Write the whole file each time: `File.write('rulebooks/<book>/assumptions.md', text)`.

## Export

The last positive and last all-refusal sheets you export are the submission; export any probe before them.

1. Export the positive sheet in one eval: the whole-book cell suite; each transaction at every edition boundary (`dates`: each edition's effective and renewal effective date and the day before, none before the first edition); per two-reading `assumptions.md` entry, a row where the readings price apart (`extra`):
   `var s = Rule.explore(book, {suite: 'cells', target: 'all'}); var rows = s.suite.map(r => r.input).concat(extra); dates.forEach(d => ['new', 'renewal'].forEach(t => rows.push(Object.assign({}, rows[0], {transaction: t, effectiveDate: d})))); var x = Rule.export(book, {rows: rows, mapping: 'pc'}); ({cells: s.cells, sheet: x.sheet})`.
   Require `cells.notfound` `0`; register an accepted `unreachable` cell's reason. Drop a row `EXPORT_DUPLICATE_ID` names.
2. Save the negative cases with `Rule.scenario.create(book, rows)`, each with an `_id`: an invalid input with `_expect: 'schema-reject'`; a valid input the calc refuses (a date before the first edition) as a plain row. Then `Rule.export(book, {ids: [...], mapping: 'pc'})`.
3. Given a results file, call `Rule.match(book, {results: 'evidence/results/<file>.json'})`. On a `mismatch`, fix the artifact its diverging step names, register the judgement, export again.
