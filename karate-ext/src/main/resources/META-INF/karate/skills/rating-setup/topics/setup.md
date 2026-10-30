# Set-up — skeleton, import mappings, baseline tables

Perform each step now, in order.

1. **Read the sources.** Call `File.list('rulebooks/<book>/**')`, then `File.read(path)` on each file, `pc.json` first: its `fields` keys are your input names, its `values` the members of each closed list, its `figure.name` the result figure.

2. **Create the skeleton.** Call `Rule.create(book, {schema, generator, calc, scenarios: []})`:
   - `schema.js`: `schema = {...}`, one field per carrier-mapping input, named exactly as its `fields` keys; a closed list is an enum whose members equal that input's `values` keys; a date is `'#date'`. Then `figures = ['premium'];` naming the mapping's figure.
   - `generator.js`: `function generate(g) {...}` — `g.enum` per list, `g.int(path, lo, hi, [boundaries])` per count, `g.enum` over the date input listing the day before and the day of each edition's effective and renewal dates.
   - `calc.js`: `const ai = true;` then `const execute = function (calc) { calc.output = {premium: 0}; };` — the algorithm stage replaces the body.
   A folder holding only `sources/` or `evidence/` is filled. Call `Rule.load(book)` and confirm no `error`.

3. **Keep sources under the book** — `Rule.import` reads only inside it. For a source outside `rulebooks/<book>/sources/`, call `File.copy('<path>', 'rulebooks/<book>/sources/<file>')`.

4. **Write one import mapping per table** — a CSV, or an `.xlsx` sheet (`source.sheet` names it) — its key columns and one value column, no other: `File.write('rulebooks/<book>/evidence/mappings/<name>.import.json', json)`:
   ```json
   {
     "ai": true,
     "source": {"headerRow": 1},
     "target": {"kind": "table", "role": "baseline", "table": "<scope>.<table>"},
     "fields": {"<key header as printed>": "<key>", "<value header as printed>": "<value>"},
     "keys": ["<key>"],
     "columns": {"<value>": "number"},
     "places": {"<value>": <decimals written, a bare number>},
     "units": {"<value>": {"source": "<points|fraction|auto>", "target": "<fraction|plus-one|number>"}},
     "values": {"<key>": {"<your book's value>": "<its cell text>"}},
     "provenance": {"source": "filing", "role": "baseline", "ref": "<rate book id>", "effective": "<YYYY-MM-DD>", "retrieved": "<YYYY-MM-DD>"}
   }
   ```
   `units`: source `points` (15 = 15%), `fraction` (0.15), `auto` (per cell); target as calc.js reads it, `plus-one` = 1.15; omit when unchanged. Omit `values` for text kept as is. A state column: `target.layers: {column, countrywide, jurisdictions}`. Register each choice a source leaves open (unit, rounding, key spelling) in `assumptions.md`.

5. **Import.** Call `Rule.import(book, {mapping: '<name>', source: 'sources/<file>'})`. An unexpected `rows.skipped` entry is a mapping error. On a refusal, fix the mapping at the row it names and import again. To re-import a table, pass `ifMatch: {'<path>': Rule.load(book).tableShas['<path>']}`.

6. **Verify**, now and after each later stage:
   - `Rule.load(book).tables` — one per table, with its row count.
   - `Rule.cells(book, {target: 'all'})` — per table `exercised` · `masked` · `unread`; a cell stays `unread` until `calc.js` reads it.
   - `Rule.explore(book, {suite: 'cells', target: 'all'}).cells` — require `notfound` `0`.

Then call `Skill.help('rating-setup', 'revision')`.
