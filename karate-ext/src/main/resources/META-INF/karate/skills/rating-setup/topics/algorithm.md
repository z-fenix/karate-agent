# Algorithm — `calc.js` from the rules-manual page

The rules-manual page is the only source of the algorithm. Draft `calc.js` from it now, step by step, in the page's order:

```js
const ai = true;
const lookup = {
    editions: [{id: '<edition id as its source prints it>', effective: '<YYYY-MM-DD>', renewalEffective: '<YYYY-MM-DD>'}],
    carrier: {<name>: <number the page states>}
};
const execute = function (calc) {
    const p = calc.input;
    const from = e => p.transaction === 'renewal' ? e.renewalEffective : e.effective;
    const edition = lookup.editions.filter(e => from(e) <= p.effectiveDate).pop();
    if (!edition) { throw 'no edition in force on ' + p.effectiveDate; }
    const base = lookup.<table key as Rule.import wrote it>[p.<key input>];
    calc.log('Base: ' + base, {step: '<pc.json step>', amount: base});
    const premium = Math.round(base * lookup.carrier.<name>);
    calc.log('Premium: ' + premium, {step: 'premium', amount: premium});
    calc.output = {edition: edition.id, premium: premium};
};
```

- **Tables.** `tables/<scope>/<table>.csv|xlsx` is `lookup.<scope>.<table>`, indexed by its key columns in order (`[<key 1>][<key 2>]`), a layered table by jurisdiction first. Never read a file or repeat a table's number.
- **Inline numbers.** A number the page states that no rate book holds, and the editions from the edition records, go in the inline `lookup` under keys no table uses, never in the arithmetic.
- **Steps.** Call `calc.log(text, {step, amount})` once per worksheet step, `step` named from the carrier mapping's `steps` list — the premium match names the first step that diverges.
- **Editions.** Select by the date input under the reading `rule.json` records: the latest edition whose effective date (renewal effective date for a renewal) is on or before it. `throw` for a date before the first edition, never a zero premium.
- **Rounding.** Round where and how the page rounds; register a step it leaves unrounded or ambiguous.
- **Determinism.** No `Date`, no `Math.random`, no I/O.

Write it: call `Rule.calc.update(book, src)`; it refuses a parse error with its line. Keep `const ai = true;` first.

Then check it:
1. Save each worked example on the page as a scenario: `Rule.scenario.create(book, [{_id, _label, _expect: {output: {premium: <the page's figure>}}, ...inputs}])`. Take the figure from the page, never from `Rule.execute`.
2. Call `Rule.check(book)` and read `verdict`: fix a `FINDINGS` defect, decide a `REVIEW` arm.
3. Call `Rule.audit(book, '<scenario id>', {output: 'markdown'})` and compare its steps with the page.
4. Rerun the set-up topic's verify step: every table cell `exercised`, or `unreachable` with its reason.

Then export, as `Skill.help('rating-setup')` § Export states.
