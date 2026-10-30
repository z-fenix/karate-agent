## API

bot.go(url) — navigate
bot.look(opts?) — get page elements → {elements: [{role, name, locator, actions}], hint, scroll}
  look({full: true}) also returns `content`: list of data containers (tables, grids) with row counts and headers
  look() defaults to ON-SCREEN elements only. For a tall form, a popup/modal, or a wizard step whose lower fields are below the fold, use look({viewport: false}) to get ALL fields at once, or look({scope: '<css>'}) to focus one container (also look({role: 'input'}) to filter). The hint tells you when fields are hidden.
bot.act(locator, action, value?) — perform action → {hint, navigated?, text?, validation?}
bot.scroll(direction?) — scroll page ('down' default, 'up', 'left', 'right'). For scrollable containers: act(locator, 'scroll', 'down')
bot.text(selector?) — structured page text (tables as markdown, key-value pairs, headings). Auto-skips nav chrome. Use to read data shown in look()'s `content` field
  READING VALUES: look() lists things you can ACT on, so a block that only DISPLAYS values (a totals strip, a quote panel, a validation dock) has no element entry — do NOT conclude it isn't there, and never `bot.eval` its outerHTML. Every `headings` entry carries the `locator` of the region it introduces, and `values: true` marks a region that holds content but nothing actionable: `bot.text('<that locator>')` reads it (or `bot.table(...)` if it is tabular).
bot.table(selector?) — extract a TABLE as structured rows → {columns, rowCount, rows:[{header: value}], hint}. Numeric cells are parsed to real numbers. PREFER this over text() for any tabular data: aggregate directly (rows.filter(r=>r.Category==='Travel').length, rows.reduce((s,r)=>s+r.Amount,0)) instead of splitting markdown — and check your count against rowCount. No selector = the largest visible table
bot.screenshot(title?) — take screenshot → {captured, title, filename, url}. Title appears in the report. In the autonomous loop the image is fed back as vision input automatically; over a live/MCP connection use the returned `url` to view it (MCP also returns the image inline). Options instead of a title: `bot.screenshot({fullPage: true})` captures the WHOLE scrollable page (everything below the fold, one image), `bot.screenshot({locators: true})` adds element coordinates for vision-to-DOM mapping — the two cannot be combined (locator boxes are viewport-relative), take them as two calls.
bot.capture('logs') — browser console + uncaught JS exceptions. Returns `{console: [{type, text, stack, source, …}], network, kjs, bot}`. Use ONLY when a click "did nothing" / page seems blocked — `look()` will hint when this is worth doing. Real apps emit lots of harmless `console-warn` / `console-log` entries; the BLOCKING errors are entries with `type: 'exception'` or `type: 'rejection'`. Read with this EXACT 2-line block:
```js
var logs = bot.capture('logs')
logs
```
The bare `logs` line is what RETURNS the value — `var x = …` alone returns `undefined`. Scan `logs.console` for an entry with `type: 'exception'`; its `stack` names the failing function. On a `ReferenceError` the result's `hint` carries the exact patch — follow it verbatim: an EMPTY page-scope stub FIRST (`bot.eval("window.fnName = function(){}")` — the assignment must sit INSIDE the bot.eval quotes; bare JS in your block has no `window`), redo the action, verify, and reimplement the body only if the retry still fails. Do NOT instrument `console.error` via bot.eval — it duplicates this API and wastes iterations.
bot.wait(ms | 'idle' | 'url' | 'user' | locator) — act() and go() ALREADY auto-wait for the page/postback to settle (Guidewire's full-page reloads included), so an explicit wait is the EXCEPTION, not a per-step habit. Do NOT follow every act() with a wait() — it usually just adds a timeout for no benefit. Reach for wait() only for genuinely async UI that act() can't see settle (a premium/total that fills in later, an SPA region that loads after the click) — and wait on the NEXT thing you'll act on (a locator) or a title you ACTUALLY SAW, never a guessed token: wait('title','X') with an X you haven't observed just times out (the failure you'd otherwise blame on the page). When you do need a navigation wait on a URL-stable app (Guidewire — every page is PolicyCenter.do), the page TITLE is the signal: wait('title', 'pattern') / 'title:pattern' (or 'url:pattern' where the URL changes), using the exact title look() showed you.
bot.eval(js) — run JS in page; returns the last expression OR a top-level `return` (a scan loop like `for(...){ if(...) return x } return null` is fine). Your introspection tool when look() can't surface something: list ids/`<select>` option values/`aria-required` fields/a field's `placeholder`. Concatenate strings — no backtick template literals.
Robot.step(name, [status], [note]) — log a milestone on the run's outcome report ('pass'|'fail' status
  + optional note). Call after verifying each meaningful business step.
Robot.success(result) — task done, report what you confirmed (becomes the report's summary)
Robot.failure(reason) — task failed, report what went wrong (becomes the report's summary)
(`Robot.step` works live too — it streams a milestone onto this run's report. `Robot.success`/`Robot.failure`
are AUTONOMOUS-LOOP only: when driving live over /api/eval, end with `bot.destroy()` and report your result
to the user — and use a labeled `match(...)` / `Rule.execute(...).verify(...)` for a pass/fail report row.)

Actions: click, input, select, check, uncheck, hover, clear, key, scroll, upload
File upload: `act('<file-input locator>', 'upload', 'checks/<file>')` attaches a file (a list uploads several). Target the `<input type=file>` ITSELF — enterprise apps (Guidewire, Salesforce) hide it (`display:none`, 0×0) behind a styled "Add Files" / "Browse" button, and that is FINE: upload bypasses the visibility checks and sets the file via the browser directly (firing the app's change event), so you do NOT click the styled button or face an OS file dialog. The path is project-relative, the same `checks/` folder you `read()` from. If look() doesn't show the input, `bot.eval` for `input[type=file]` to get its locator (id/name).
Checkboxes/radios: look() lists them with `actions: ['check','uncheck']` and a `checked` field. Use act(loc, 'check') / act(loc, 'uncheck') to set the DESIRED state — idempotent (no-op if already there) and it fires the app's change event. Do NOT set `.checked` via eval — the app's framework won't register it (e.g. a Guidewire vehicle stays "no driver assigned").
Hover-revealed actions: some apps hide a row's action icons until the row is hovered (e.g. a Guidewire n2record / interview icon next to a contact). If look() doesn't list an action you expect on a row, DON'T fall into repeated DOM searches — `act('<row locator>', 'hover')` first, then `look()` again: the revealed icon now has a real `locator` you can act on. (`bot.eval` DOM-scraping is a last resort; it can't click for you and the icon may not exist until hover.)
Collapsed menus + state flags: advanced/less-common actions hide behind menus and disclosure carets — search-an-account-by-number, advanced search, new-X, settings. look() flags the TRIGGER that owns a collapsed menu: `{name:'Account', menu:{closed:true, items:5}}` — the hidden items are NOT listed until it opens. Open it with `look({expand: '<trigger locator>'})`: this performs the right reveal (click or hover, whichever that widget needs), leaves the menu OPEN, and lists the items — each carrying `revealedBy:{owner, via}`, which is also the reveal step you author in a check (`<via>` the owner, then click the item). If the action you need isn't on the page, expand the relevant trigger before concluding it is missing. Other state flags look() emits only when true: `occluded:'<locator>'` names an overlay covering the control — dismiss it first; `disabled:true` may enable after the page settles or a prior field commits — re-look after your next action; `ambient:true` marks passive text that merely SHARES its text with a real target (a label, not a control) — never act on it.

Self-healing acts: if you act on a target behind a closed menu, act() auto-reveals it as a safety net and the response carries `autoRevealed:'<via> <owner>'` — put that reveal step in your check rather than leaning on the net. If a locator has drifted (an id or text that moved), act() heals to the closest live match and returns `healed:{from,to}`, and the NEXT look() repeats it (`healed:[…]`) so a multi-act block cannot lose it — adopt `to` from then on; if nothing is close enough (or the best match pins a different test-attribute identity) it refuses with a decorated error instead of clicking the wrong thing, so a failure is honest, never a false green. Inside `Check.run` there is no auto-heal: a check runs strict — drift fails the check, and the error names the fix.


Stuck? Take stock first: `bot.study()` opens a take-stock phase — explore the page, then submit a dossier (`bot.study({drawerSelector, nameNoiseTokens, caretSelectors, menuSelectors, prose, helpers})`) whose machine fields make look()/act() better for the rest of the session automatically. When the page is a known enterprise app, study() hands you that app's pattern pack (menu carets, field-name rules, the error drawer, date/picker choreographies) already applied — adopt it instead of re-deriving. Reach for study() when a page is dense, its names are ambiguous, or the same act keeps missing.
No URL navigation — explore the UI: many enterprise apps are NOT URL-addressable (Guidewire is one URL, PolicyCenter.do; the URL never changes between screens). You CANNOT reach a screen by editing the URL/hash/query (`bot.go(url + '#accountSearch')`, `?page=...` etc. do nothing — do not invent them). The ONLY way to a screen is through the UI: look() to discover what's actually on the page (including the carets above), act() to operate it, screenshot to confirm visually. When you don't know how to reach something, EXPLORE — open a caret, read look() — never guess a locator, a menu item, or a `wait('title:...')` token you haven't seen resolve.
Keys for `act(loc, 'key', '{X}')`: {Enter}, {Tab}, {Escape}, {Backspace}, {Delete}, {Space}, {ArrowUp}, {ArrowDown}, {ArrowLeft}, {ArrowRight}, {PageUp}, {PageDown}, {Home}, {End}. Either `'{Escape}'` or `'Escape'` works; an unknown `{Foo}` returns INVALID_KEY (does NOT type the literal).
Locators: copy from look() response. CSS `[name="x"]`, wildcard `{button}Submit`, indexed `{button:2}Add`. Use `{^tag}Prefix` for starts-with — e.g. `{^a}Suspended` matches "Suspended", "Suspended 1", "Suspended 2" so badge counts don't break the locator.
Troubleshooting: if multi-line code fails, call look() to see current state, then retry one action at a time. If act() reports "occluded by …", close the overlay first (Escape, click outside, or its close button) — retries can't bypass pointer-events occlusion.
New tabs: when a click opens `target="_blank"` / `window.open()`, focus auto-switches to the new tab and the act() result includes `{newTab: {url, title}}`. Call look() — it describes the new tab. Do NOT re-click the original button. For multi-tab: `page('window', 'switchTab', titleOrUrlSubstr)`.
Async eval: `bot.eval()` automatically awaits returned Promises (5s cap). For longer waits, use the window-global pattern: `bot.eval("window.__r=null; fetch(...).then(r=>r.text()).then(t=>window.__r=t); 'started'")` then poll with `bot.eval("window.__r")`.

## Persistence

The run holds ONE JS engine in memory. Every `var`, `function`, and assignment from one block survives into the next block — same scope, same heap, same browser. Use this to build helpers ONCE and call them many times instead of repeating long extraction code.

```js
// iter 1 — define a helper from what look() shows
function getStat(label) {
  return bot.eval('var els=document.querySelectorAll("li,tr,dt,dd,span");for(var i=0;i<els.length;i++){var t=els[i].textContent.trim();if(t.indexOf(' + JSON.stringify(label) + ')===0)return t;}return null');
}
var price = getStat('Previous Close');
```

```js
// iter 2 — getStat is still defined, no need to redeclare
var exDiv = getStat('Ex-Dividend Date');
var yield_ = getStat('Forward Dividend');
```

Call the helper per value; never re-run `look({full:true})` or `text({full:true})` to re-extract.

## Example

One step per turn — observe first, THEN act on the locators you saw. Do not guess locators ahead.

```js
// turn 1 — OBSERVE. I don't know the field locators yet, so just go + look.
bot.go('https://example.com')
bot.look()
```
The look() result lists each field's real `locator`. Only NOW, in the next turn, act on them — and
you may combine the same-page fields into one block since you have already seen all three:
```js
// turn 2 — act on the locators look() returned (copied exactly), then stop to observe the result.
bot.act('[name="email"]', 'input', 'test@example.com')
bot.act('[name="password"]', 'input', secret('APP_PASSWORD'))
bot.act('{button}Sign In', 'click')
```
```js
// turn 3 — the page changed, so look() again before deciding what to confirm.
bot.look()
// Robot.success('Signed in successfully')  ← once the result confirms it
```
