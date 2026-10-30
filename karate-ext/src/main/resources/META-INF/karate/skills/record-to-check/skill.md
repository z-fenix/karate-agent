# Karate — record a UI flow → author a check

> **Prerequisites:** `Skill.help('getting-started')`, `Skill.help('assertions')`. For the `bot.*` surface call `Robot.help()`; for the recording lifecycle call `Recording.help()`.

## What this skill is for

A human demonstrates a UI flow **once** (driving the real browser through the noVNC pane); you turn that demonstration into a **proper, maintainable karate check** — adding waits, parameterizing inputs, looping with a Scenario Outline, switching environments in `karate-config.js`, and tying the run to requirements/rules/coverage in `karate-boot.js`.

**A ready-to-run JS is NOT auto-generated, and that is deliberate.** The recording is *material* — locator-anchored, noise-filtered, replay-grade — but only you can decide what is incidental (the exact account that happened to be open) vs essential (the steps that prove the goal), where a wait belongs, and what to parameterize. You author the artifact; you verify it by running it. This is the deliberate stance: write the programmatic equivalent, run it once, iterate if it fails — no heavyweight run→script transpiler.

The network calls a recording made, sorted into those an API test needs and those it does not: `Traffic.sift(runId)`; a verified API test of them: `Skill.help('record-to-api')`.

## The loop

```
   Recording.start('pa-submission')   ← human drives the browser; every interaction is captured
     ↓                                  (semantic, locator-anchored human_action events)
   Recording.note('this is the offering tier — pick Gold')   ← optional: label a step / state intent
     ↓
   Recording.stop()
     ↓
   Recording.get()                ← read the recording back as authorable MATERIAL (this skill)
     ↓                                  { startUrl, steps:[bot.*-ready], notes:[...], captured, replayable }
   author checks/<name>.js  (or a .feature Scenario Outline)   ← YOU write the proper artifact
     ↓
   Check.run('<name>.js')  /  run the feature   ← verify; iterate until green
```

## Author in stages — drive live, then codify

A live `bot` is already open on the SUT — treat it as a REPL. Every `Check.run` re-drives the flow from the start, so grow the check **one verified stage at a time** (a stage is a logical segment: `login + reach the start`, `add driver`, `quote`). For each stage:

1. **Drive its recorded steps on the live `bot`** — `bot.act('<loc>', '<action>', '<v>')`, locator and action copied verbatim (Step 1) — and confirm each landed: read the return value and `bot.look()` for the next locator. A step that errors means its locator drifted: try an `| alt:` candidate or re-anchor from `bot.look({full: true})`.
2. **Append only the steps you saw work**, parameterized, in the order you proved them: `File.write('checks/<name>.js', <js>)`. Never re-derive the file from memory — that reintroduces the bugs you just fixed. Keep the unreached tail as `// TODO: next stage` comments so the file parses and runs to a known-good point.
3. **`Check.run('<name>.js')` to green before you extend.** A failure throws with the line number, the failing source line, the message and the live page state. On red, drive that one step by hand to see what the page does, fix that one line, re-run — never blind-rewrite and re-drive.

**When the live app differs from the recording** (state, data, build), `bot.look({full: true})` and steer to the goal: skip steps you are already past, handle a surprise page then rejoin, and target a changed control by its most stable signal (id, accessible name, visible text). The recording is one path on one build.

**The file layout is yours.** `checks/<name>.js` is the entry check that must end green. For a long flow, write each stage as its own file (`checks/<name>/login.js`, `.../quote.js`) and compose them from the entry check with `Check.run('<name>/login.js', __arg)` — nested checks share the live `bot`, and a failure names one small file. `File.write` creates parent dirs. Scratch files are fine; only the entry check must end green.

## Step 1 — read the recording

`Recording.get([runId])` returns the recording's **replayable core** — never a DOM dump, so it stays small:

```js
Recording.get('4edb8b8a')
// => {
//   startUrl: 'https://gw.example/.../NewSubmission.do',
//   steps: [
//     'nav    https://gw.example/.../Account.do',
//     'click  {div}Account                      | alt: #account-tab , [aria-label="Account"]',
//     'type   input[name="policyNumber"]  = "P-449"',
//     'select #offering  = "Gold"',
//     'click  {button}Next',
//     ...
//   ],
//   notes:  ['this dropdown is the offering tier — pick Gold'],
//   captured: 41, replayable: 27   // 14 noise events (scroll/drag/hover/focus) were dropped
// }
```

- **`steps`** are `bot.*`-ready: the `locator` is exactly what `bot.act()` consumes (copy it **verbatim** — never reconstruct `{button}Text:N`). Use the **primary** locator (before `| alt:`); a stable `#id`/`[name]` is the durable choice (resolves while hidden, survives re-renders). `| alt:` lists fallback candidates **for when the primary drifts** — not a stylistic preference: don't downgrade a working `#id` to a `{div}Text` wildcard (it can collide with an ambient same-text label elsewhere on the page).
- **`notes`** are the human's annotations — your goal + assertion hints.
- **`captured` vs `replayable`** tells you how much demonstration noise was filtered.

### Map each step to a `bot.*` call

| recorded action | call | notes |
|---|---|---|
| `nav <url>` | `bot.go(url)` | the starting context — usually pin it (see Step 3) |
| `click <loc>` | `bot.act(loc, 'click')` | |
| `type <loc> = "v"` | `bot.act(loc, 'input', 'v')` | parameterize `v` |
| `select <loc> = "v"` | `bot.act(loc, 'select', 'v')` | |
| `check`/`uncheck <loc>` | `bot.act(loc, 'check')` / `bot.act(loc, 'uncheck')` | set the DESIRED state, never a toggle — idempotent, and it fires the app's change event. A recorded `click` re-drives into the WRONG state on the second run |
| `hover <loc>` | `bot.act(loc, 'hover')` | **keep it** — opens a menu/submenu before the next click (the bot auto-pauses for the submenu to render) |
| `key (Enter)` | `bot.act(loc, 'key', '{Enter}')` | the verb is `key`; there is no `press` |
| `submit` | usually a `click` on the submit control | |
| `scroll` / `drag` | **drop** (already filtered) | re-add only for sliders/canvas/reordering |
| `click` on a file `<input type=file>` **or** a styled "Add Files"/"Browse"/"Choose File" button | `bot.act('<the input>', 'upload', 'checks/<file>')` | **NEVER replay it as a `click`** — see "File uploads" below |

### File uploads — replace the recorded click with the `upload` verb (NEVER click the chooser)

A file pick is the one recorded interaction you must **not** copy verbatim. The recording captured a `click` on the `<input type=file>` (or on the styled **Add Files / Browse** button in front of it), but **replaying either click opens the native OS file-chooser dialog — which is outside the page, un-scriptable, and HANGS the run** (the loop gets stuck waiting on a dialog it can't see). Two things the recording can NOT give you, by design:

1. **Which file the human picked** — the OS dialog is outside the browser, so the filename was never captured. **The fixture must already be in `checks/`** (the human uploads it there, or you're told its name). If it isn't, **ask the human for the file** — don't invent a path.
2. **The scriptable path** — instead of the click, drive the hidden input directly with the `upload` action:

```js
// recorded:  click #Doc-...-FileInput--file   (and/or  click {div}Add Files)
// author as: target the <input type=file> ITSELF and use 'upload' (CDP setFileInputFiles — no OS dialog)
bot.act('#Doc-...-FileInput--file', 'upload', 'checks/contract.pdf');
// if look() doesn't surface the input, find its id/name:
bot.eval("(document.querySelector('input[type=file]')||{}).id")
```

`upload` bypasses the visibility/occlusion checks and sets the file straight on the input (firing the app's `change` event), so a `display:none`/0×0 enterprise input is fine and **no dialog ever opens**. Enterprise upload widgets then require an explicit **commit** (an "Update"/"Save"/"OK" button) and often mandatory metadata (a document **Status**/**Type** select on the staged row) — drive those after the `upload`, then verify the new row landed. See [`Robot.help('forms')`] for the full upload contract.

## Step 2 — waits are the EXCEPTION, not the norm

`bot.act()` (and `bot.go()`) have a **built-in stability wait** — on postback-heavy apps (Guidewire's per-step full-page reloads, classic server-rendered wizards) they handle the reload with **no explicit wait needed**. So do **not** reflexively follow every `bot.act()` with a `bot.wait('title:…')`: most of the time it adds a timeout for nothing, and a **guessed** title token (`wait('title:Submission')` when the page is actually titled "Offerings") just times out — a self-inflicted failure you'll waste a turn blaming on the page. Add an explicit wait **only** for genuinely async UI that `act()` can't see settle, and wait on a **real** signal — the next locator, or a title you **actually saw** in `look()`:

```js
bot.act('{div}Quote', 'click');
bot.wait('#quote-premium');          // an SPA region that fills in asynchronously
// assert with the canonical form (NOT match(x).contains(y) — see Step 6):
match.contains(bot.text('#quote-premium'), '$', 'premium shown');
```

Do **not** sprinkle `bot.sleep()` — prefer `bot.wait(locator)` / waiting on observable state, and only when `act()`'s built-in wait genuinely isn't enough.

## Step 3 — make the start deterministic (the #1 source of flaky re-runs)

A recording starts from *whatever state the human was already in*. Two distinct traps — handle both:

**(a) The SUT holds the session — a re-run does NOT start at login.** This is the default for enterprise apps (Guidewire, Salesforce, SAP): `bot.go(startUrl)` lands wherever the last run left off (mid-wizard), not the login page — so a check that blindly types into login fields fails on the 2nd run. Make login **idempotent**: probe for the field, log in only if it's there.

```js
bot.go(startUrl);
if (bot.find('[name="...-username"]', { wait: false })) {
  bot.act('[name="...-username"]', 'input', user);
  bot.act('[name="...-password"]', 'input', pass);
  bot.act('{div}Log In', 'click');
}   // else: already authenticated — skip
```

**(b) An ambient entity — pin it only if the flow actually opens one.** If the recording navigates into *whichever* account/policy was open (`click {div}Account` → an open record), make it explicit (search by a stable key). But don't force this where it doesn't apply: a top-nav menu like `{div}Account → {div}New Submission` opens a *generic* picker (the entity is chosen later) — there is nothing to pin there. Read what the step actually does before "pinning" it.

> **The recording can't teach you this navigation — the human was already on the entity, so they never searched for it. You must DISCOVER the pin path live, by UI exploration, not by guessing.** This is the #1 way the authoring fails: the model invents a search screen, a `{div}Account Search` menu item, a `wait('title:Account File')`, or a fake URL (`bot.go(url + '#accountSearch')`) — none of which exist. Don't. The search-by-number control usually hides behind a **collapsed top-nav menu**: `bot.look()` flags its trigger as `{name:'Account', menu:{closed:true, items:N}}`. Open it with `bot.look({expand:'<trigger locator>'})` — that performs whichever reveal the widget needs and lists the items — read the *real* field it reveals, drive it, and see the *real* title it lands on, then codify exactly that. (On live GW: expanding the Account trigger reveals an inline `[name="...AccountNumberSearchItem"]` field + its search button, and the account opens on title `Account Summary: <name>` — but verify on YOUR build; copy what look() shows, never these strings.)

```js
// ILLUSTRATIVE shape only — copy the locators bot.look() shows you on YOUR build, not these:
bot.act('<account-number search field — found by expanding the Account trigger>', 'input', accountNumber);
bot.act('<its search/go button>', 'click');
bot.wait('title:Account Summary');   // ← the REAL title you saw it land on, not a guessed one
```

If the goal note doesn't name the starting context/entity, **ask the human** — it's an input, not something to infer.

> **List rows below the fold:** `bot.look()` returns only the visible viewport by default, so a row further down a long list-view is silently absent. Use `bot.look({ viewport: false })` (or find the row by its accessible `name`) — don't trust a position-indexed id like `…LV-8-…`.

## Step 4 — parameterize + loop (Scenario Outline)

Hardcoded values (`P-449`, `Gold`, a VIN) belong in **data**, not the steps. For a single flow, read each from `__arg` with its own default — `var user = __arg.user || 'su'` — never `__arg || {…}`: a check run with no arg gets an empty, truthy `__arg`, so object defaults never apply. For many rows, drive a **gherkin Scenario Outline** over a data file — no JS-side loop primitive:

```gherkin
Feature: PA submission quote

  Background:
    * def data = read('submissions.json')

  Scenario Outline: quote a submission
    * def premium = Check.run('pa-submission.js', __row).value
    * match premium == '#number'
    Examples:
      | data |
```

`pa-submission.js` is the recorded flow as a self-oracling leaf; the Outline feeds it rows.

## Step 5 — environments + ties (config + boot)

> **A `.js` check is plain JS — there is NO `karate` global.** Inside a check run via `Check.run('x.js', arg)` the only bindings are `bot`, `Robot`, `Check`, `File`, `match`, and `read(path)` for files; inputs arrive as `__arg`. Do **not** reach for `karate.env` / `karate.get` / `karate.config` — they are `undefined` and throw `karate is not defined`. The `karate` bridge below exists only when the check runs inside a **`.feature`** (where `karate-config.js` is loaded). For a standalone check, take env/host/creds as `__arg` defaults (Step 4).

- **`karate-config.js`** — environment switching (applies when the check runs via a **`.feature`**). Put base URLs / credentials / the GW host per env here, keyed off `karate.env`; never hardcode a host in the check.

  ```js
  function fn() {
    var env = karate.env || 'dev';
    var config = { baseUrl: 'http://localhost:8180', user: 'su', password: 'gw' };
    if (env == 'sandbox') config.baseUrl = 'https://gw-sandbox.example';
    return config;
  }
  ```

- **`karate-boot.js`** — wire the exts + the traceability ties once, for the whole suite:

  ```js
  boot.ext('robot');   // binds Robot/bot for UI checks (like boot.ext('rules') binds Rule/match/Check)
  boot.ext('rules');   // Rule/match/Check + calc.req requirement links
  boot.ext('coverage');
  ```

  Tie a check to a **requirement** with an `@req=` tag (and `@cov=` for coverage) so the run attributes into the RTM — on the `.feature` Scenario, or via the run's `req` anchor. Rule oracles via `Rule.execute(...).verify(...)` auto-attach their `@req=`/rule links.

## Step 6 — make the report explainable, then verify

Karate's report shows **steps**, and you control what they say:

- A **`.feature`** scenario — every Gherkin step is a native report step. Prefer this when you want the run to read step-by-step.
- A **`.js`** check — JS statements are **not** auto-logged line-by-line (a per-statement step event is a *future* enhancement). Explainability comes from explicit calls that land as **real report step rows**:
  - `Robot.step('Verified premium = $1,240', 'pass')` — a milestone row (status `pass`/`fail`, optional 3rd note arg). **It is `Robot.step`, not `bot.step`** — milestones hang off `Robot`; only visuals hang off `bot`.
  - `match.contains(actual, expected, 'label')` — an assertion row with actual/expected/verdict. Use the `match.equals/contains/each(actual, expected, label)` form; there is **no** `match(x).contains(y)` chain.
  - `bot.screenshot('Quote summary — premium $1,240')` — a labeled visual step.

  When a `.js` check runs (standalone or called from a feature step) these inline into the scenario as native steps — so 3–8 well-chosen `Robot.step`/`match`/`screenshot` calls make the report tell the business story.

**Verify by running it** — the full flow from a CLEAN browser with no arg, the way it is replayed: `Check.run('<name>.js', null, {reset:true})`. `{reset:true}` wipes cookies/storage BEFORE the run, so it exercises the true cold-start path (entry URL → idempotent login → …), not the mid-flow state your live drive left behind. Use plain `Check.run` only to troubleshoot one stage from where you are. The check is done when it is green from a reset state and the report reads the way a stakeholder needs.

## Step 7 — live authoring only: release the session

This step is for authoring over the live console; an autonomous run ends with `Robot.success`/`Robot.failure` instead. Keep the live browser **alive while you iterate** — `Robot.create({ reset: false })` reuse keeps you logged in between `Check.run`s. **Once the check is green and you're handing it back, release the session** — otherwise it dangles (the run stays `live`, and a still-armed recording can bleed its state into the next run):

```js
Recording.stop();   // if a recording is still armed (Recording.status().recording)
bot.destroy();      // closes the browser and settles the live run; Robot.create() starts a fresh one
```

Do this only at the **end** of the authoring session, not between iterations.
