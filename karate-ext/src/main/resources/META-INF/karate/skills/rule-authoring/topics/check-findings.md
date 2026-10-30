# Reading `Rule.check` — the five axes, and the findings that are not defects

> Pull this when `Rule.check` reports anything you are about to act on. It names the three arm findings
> that are NOT a rules bug, what `robustness` discloses, and the two benign-dead-arm cases that cost a
> cold run a rewrite it did not need. Back to the loop: `Skill.help('rule-authoring')`.

`check.verdict`'s detail is
split by the FIX on the three rule-arm axes (*reachable* =
some input INSIDE the declared domain can drive the arm · *used* = an in-domain saved scenario exercises
it — an out-of-domain row earns neither, it is disclosed under `robustness.savedRows` ·
*tested* = reserved, a live-system run). **The verdict that gates "clean" is these five axes:**
- **`notused`** — reachable but no saved scenario exercises it → **add case data** (the fix is data).
- **`notreachable`** — no scenario reaches it even with the generator → **fix the rules** (a dead branch like
  a `> MAX` cap that can never trip; `gaps` is the legacy alias). **The finding names the input `axis` it
  turns on**, or carries `attribution:'notderivable'` when the comparison reached no input field — then the
  fix is a `calc.levels` hint or a proposed row, not a branch rewrite. `Rule.ranges(name)` gives that axis
  its values and boundaries, which tells you whether the arm is dead or merely unwitnessed. Three cases are NOT a rules bug.
  (i) An arm a **closed** axis excludes — `g.enum('state',['CA'])` with a `state != 'CA'` arm. Widen
  the declared domain, or accept the arm as dead inside it. (ii) On a **free-text** axis, an arm the
  engine could not supply a value for: it supplies one (the `other` class — `Rule.ranges` marks it `complement:true`) only for a direct
  `== === != !==` test against a string. `in` / `not in` / `.includes()`, `!= null`, and a compare
  against a non-string get no witness — rewrite the test as `!=` if you want it covered. (iii) An arm
  only an OUT-OF-DOMAIN probe reaches — a defensive guard doing its job; `robustness.coverageOnly`
  names it, and it is dead for every legal input rather than a missing scenario. Check
  `failures` too: a witness that made the calc throw leaves the arm unreached for that reason (an entry
  marked `domain:'out'` is a robustness probe — it never forfeits the COMPLETE claim).
- **`deadBranches`** (a site stuck on ONE arm), **`notproduced`** (a `calc.outcome` value no input
  yields), and **`properties`** (the PBT pair: a violated `calc.always` with a minimal IN-DOMAIN
  counterexample — always an input that can occur; a `calc.sometimes` situation no in-domain
  input reaches). A surviving counterexample has **two readings** — the
  rules are wrong, or the property/scenario expectation is wrong (a spec defect): fix the reviewed
  artifact, re-run the full deck, and **keep the counterexample as a saved scenario** so the defect
  class stays guarded.

**`rejects`** is reported alongside them — the negative scenarios (`Skill.help('rule-authoring','negative-scenarios')`), graded against
`schema.js` alone. Any entry with `pass:false` is a finding: `ACCEPTED` means the schema took a row it
was told to refuse, `NOTCHECKED` means the row proves nothing. **`accepts`** is its other half — the
saved rows the shape must admit; a green roster beside a `NOTCHECKED` `accepts` is a shape that
refuses everything.

**`robustness`** is reported too, and is likewise informational — findings, never verdicts. Present
only when a declared bound was crossed: `{rows, failures, properties[], savedRows[], coverageOnly[],
unreachableBoundaries[]}`. Read `savedRows` (a stored row the declared domain forbids — fix the row or
widen the generator) and `unreachableBoundaries` (a threshold no legal input can cross) as authoring
signals; `coverageOnly` arms are dead for every legal input, not scenario gaps.

**`notderivable` is reported too but is INFORMATIONAL — it does NOT make `check` unclean.** Any rulebook
that branches on a derived aggregate populates it; don't chase it to empty (see the `calc.levels` note).
"Clean" = **no *genuine* findings** on the five axes above — every `always` holds, every `sometimes` reached.
That is why `verdict.status` distinguishes `REVIEW` from `FINDINGS` instead of calling every dead arm a
defect: `CLEAN` is a fact, `REVIEW` is a question addressed to you.

> **Not every `notreachable`/`deadBranches` entry is a defect** — **defensive-default arms are benign and
> expected.** A `const items = input.items || []` or `lookup.regionTax[region] || 0` fallback is a branch
> whose `|| <default>` arm never fires *because the data always supplies the value* — `check` honestly
> reports it as a dead arm, but it's correct code, not a rule bug. The reference `order-pricing` ships
> **three** such entries (two `||`-fallback arms + the one deliberate "Discount cap") and is still the
> "good" example — so the gate is "no *unexpected* dead arm", not a literally-empty list. Read each entry's
> `source`: a `x || default` fallback or a documented cap is fine; a real business rule reported dead is
> the bug to fix. **Compare against `Rule.check('order-pricing')`** when unsure what benign noise looks like.

> **A reachable business arm can be wrongly flagged dead when you branch on a *precomputed boolean*.** If
> you route on `if (scoreBorderline || dtiBorderline)` (booleans you derived earlier), the coverage
> minimizer may never select the exact input combination that exercises the *other* arm, so a perfectly
> reachable outcome shows up as `notreachable`/`deadBranches`. **Fix: branch on the raw input field**
> (`if (creditScore <= 669)`) instead of the derived boolean, and pin the boundary with
> `calc.levels('creditScore', [669, 670])` — then the explorer can attribute it and the false positive
> clears. Confirm an arm is genuinely reachable with `Rule.explore({suite:'boundary'})` (it shows the rows
> the minimized suite dropped) before you start rewriting rules.

