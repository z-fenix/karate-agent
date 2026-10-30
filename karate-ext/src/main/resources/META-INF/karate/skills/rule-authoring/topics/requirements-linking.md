# `calc.req` — linking a decision to a requirement criterion

> Pull this when a decision in `calc.js` realizes an acceptance criterion, or when `Rule.cover` reports
> `req:{COVERED:0}` despite many `calc.req` calls. Back to the loop: `Skill.help('rule-authoring')`.

### `calc.req('<reqId>/<acId>')` — link a decision to a requirement criterion
The call goes **inside the `if`/`case` arm it describes**. An arm with authored behaviour and no call
reads `notclaimed` in `Rule.check` (REVIEW) — claim it or delete it; a body-less arm (an else-less `if`)
needs none, and a ternary carries none, so a decision you want claimed is an `if`. Each reached criterion
emits an **OBSERVED** hit on `req:<reqId>/<acId>` (the branch-level counterpart to a scenario
`@req=ORD-001/1` tag). `Rule.run` surfaces the criteria a run reached as **`reqs`** (the live preview);
the criterion goes **COVERED** in the persisted RTM only when a **run** emits the hit (a `karate run` /
a data-driven feature — `/api/eval` is a no-op, so the matrix reflects runs, not console calls). This
closes the requirements→rules→coverage loop: author the requirement markdown
(**`Requirement.write(name, md)`**), link the criterion here, and the run proves it.
```js
calc.label('Approval routing');
if (total > 50000) { calc.req('ORD-014/1'); calc.outcome('needs-approval'); }
else               { calc.req('ORD-014/2'); calc.outcome('auto-approved'); }
```
**Author the requirement first** if it doesn't exist yet — `Requirement.write('<name>', md)` writes
`requirements/<name>.md`, and the criteria then exist to be linked + covered. The format, in full:

```markdown
## PL-001: Tax code resolution
@criticality=high
The system shall resolve the operating tax code before computing tax.

**Acceptance:**
- WHEN the code is unrecognised THE SYSTEM SHALL apply the emergency code
- WHEN the code is `NT` THE SYSTEM SHALL deduct no tax
```

Heading `ID: Title`, one optional `@key=value` line, prose, then an `**Acceptance:**` bullet list —
each bullet is a criterion, numbered by position, so those two are `PL-001/1` and `PL-001/2`. **A
requirement with no `**Acceptance:**` block has no criteria**, and `calc.req('PL-001/1')` against it
lands nothing. `Skill.help('requirements')` covers parents, `@real=` and the
rest.

> **The `<acId>` is the criterion's 1-based position in document order** — no dot, no `ac` prefix. (A
> *dotted* id like `1.5` is only for **inserting** a criterion between `1` and `2` without renumbering
> — see the `requirements` skill.) **An `<acId>` matching no real criterion silently lands nothing**
> (the #1 trap): no warning today, and `Rule.cover` reports `req:{COVERED:0}` despite many `calc.req`
> calls. If coverage doesn't light up, **confirm the ids with `Requirement.get('<reqId>')`**
> (its `criteria[]` lists them).

