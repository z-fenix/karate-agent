# Revision — a circular or a deviation as a change set and an edition

Perform each step now, once per revising source: the circular, then the deviation filing.

1. **Before and after files.** Copy both under `rulebooks/<book>/sources/`. When the source lists only the changed rows, write the after file yourself — the base rate book with those rows changed, added or dropped — and register it.

2. **Write a mapping for the revision** like the base one — `target.table` the base table it revises — with `provenance` naming the revising source: `{"source": "filing", "role": "baseline", "ref": "<circular id>", "effective": "<its date>", "retrieved": "<YYYY-MM-DD>"}` and `"ai": true`.

3. **Build the change set.** Call `Rule.changeSet(book, {before: 'sources/<base>', after: 'sources/<revised>', mapping: '<name>', produces: {edition, effective, renewalEffective, jurisdictions, lines}})`.
   - `produces` is what the source states: its edition id as printed (never composed), effective date, renewal effective date when given, jurisdictions and lines. Register an open one.
   - Pass `algorithm: true` when the revision adds a factor or a step.
   - Check `counts` (change · add · drop) against the source's own count of changed rows.

4. **Read its cells.** Call `Rule.changeSetCells(book, '<change set id>')` → `cells` and `absent`. An `absent` address is a row the revision adds or drops, or a key spelled differently from the base table — fix the mapping for the latter.

5. **Make the book rate it.** Import the after file through a copy of that mapping whose `target.table` is the edition's own key, and select it in `calc.js` by the date input. A deviation is a table under its own key that `calc.js` reads on top of the base factor; register how it combines with the base.

6. **Import the edition records** — every edition, current and revised. Write a mapping with `"target": {"kind": "ratebook", "name": "<name>"}`, `fields` mapping headers to `code` · `edition` · `line` · `jurisdiction` · `status` · `effective` (and `renewalEffective`, `expiration`, `companionOf`, `nameDate` when present), every column `"string"`, each date column in `transforms` (`"date"` ISO, `"mdy"` MM/DD/YYYY), no `keys`. Call `Rule.import(book, {mapping: '<name>', source: 'sources/<records file>'})` → `evidence/ratebooks/<name>.json`. Add one record per change set's `produces` (the imported records' `code`, `line`, `status`) and rewrite the file with `File.write`, keeping `provenance` and `"ai": true`.

7. **Write the selection rule**: `File.write('rulebooks/<book>/evidence/ratebooks/rule.json', json)`. Nothing defaults; register each field a source leaves open:
   ```json
   {
     "ai": true,
     "dateKey": "<pc.json date input>",
     "renewal": true,
     "combine": "layer",
     "statuses": {"selectable": ["<records status that rates>"], "other": ["<each other status>"]},
     "editionId": "<regex over the sources' edition ids: groups base, version, companion>",
     "filed": "filed",
     "scope": {"jurisdictions": ["<each state the sources name>"], "lines": ["<each line they name>"]},
     "editionIds": {"<produces edition>": "<the records' edition>"}
   }
   ```
   `filed` names `evidence/ratebooks/<filed>.json`, in the records' shape: every edition the sources file. `"changes"` reads `ok` only when every edition comes from a change set, never beside a rate book already in force.

8. **Check.** Call `Rule.editions(book, {records: '<name>'})` and require `ok: true`, every window's `finding` `ok`. Fix a finding in the artifact it names, never by narrowing `scope` or `from`/`to`. A window in `notEvaluated` names its cause.

Then call `Skill.help('rating-setup', 'algorithm')`.
