/*
 * The shared RUN-KICKOFF modal (components/run-modal.html) — the one reusable kickoff surface for the
 * Runs + Checks pages (D132/D58). Open from anywhere via kaRunModalOpen({path?, check?, goal?,
 * req?, again?}); the split-button "run now, straight to live view" path is kaRunNow({path|check}).
 *
 * The lanes, picked by what's chosen: a SET — any mix of .feature and checks/*.js chips, or a folder /
 * "." — is the headless SUITE (Runner.run, D281); a *.karate.js is the LAUNCH orchestrator; a typed goal
 * (empty picker) and/or a .md is the autonomous LOOP (the free-form exploratory run).
 *
 * The kickoff primitives are standalone (below) so both the modal factory and the no-UI kaRunNow path
 * share one implementation. Loaded by each host page (not globally) via the fragment's <script> include;
 * it is a classic script so runModal() exists when the deferred Alpine boots.
 */

// ---- the standalone kickoff primitives (shared by the modal + the run-now split button) -------------

/** A headless SUITE: Runner.run takes a single path, a folder, a LIST of paths (the mixed .feature +
 *  checks/*.js chip set), or '.' ("Run ALL"). Async → mints runs/<id> and returns {runId, status:'started'}.
 *
 *  A REFUSAL (the D48 lease held by another set, an unusable selector) comes back as a `{error, code}`
 *  envelope on the RESULT channel, not as a thrown error — so raise it here, once, and every caller's
 *  existing catch surfaces it instead of navigating to `/runs/undefined`. */
async function kaKickSuite(pathArg, opts) {
  const out = await karate.value('Runner.run(' + JSON.stringify(pathArg) + ', ' + JSON.stringify(opts || {}) + ')');
  if (out && out.error && !out.runId) throw new Error(out.error);
  return out;
}

/** The autonomous LOOP — a free-form goal (+ optional .md hints / provider·model). */
async function kaKickLoop(task, opts) {
  return await karate.value('Harness.explore(' + JSON.stringify(task) + ', ' + JSON.stringify(opts || {}) + ')');
}

/** The HEAL lane (D163, "Run with AI") — run-or-heal an existing .js check: run it, and on failure let
 *  the LLM re-drive the flow and repair the source to green. Async → mints runs/<id> like a loop run. */
async function kaKickHeal(name, opts) {
  return await karate.value('Harness.heal(' + JSON.stringify(name) + ', ' + JSON.stringify(opts || {}) + ')');
}

/** The deterministic CHECK lane: ensure a LIVE `bot`, register the live run, then fire a PLAIN
 *  `Check.run(name)` with a keepalive fetch (survives the navigation). Returns the run id to navigate to.
 *
 *  NO Harness wrapper. `Check.run` is deterministic — the kickoff must not inject `try/catch` +
 *  `Harness.stop` into the eval: settling the run MID-eval ran the run-var teardown, which wiped the
 *  catch's own `e` so `throw e` raised "e is not defined" — a Harness-manufactured error masking the
 *  check's REAL failure. Settlement now happens SERVER-SIDE at the /api/eval boundary (MaxProfile.onEval),
 *  after the eval fully unwinds: success → 'completed', a thrown check error → 'failed', the real error on
 *  the transcript. Recording (video) is opt-in — a deterministic check does not record by default. */
/** The LAUNCH lane: a `*.karate.js` ORCHESTRATOR — headless, and it must NEVER create the bot (the
 *  script composes the Runner + Report verbs itself; a browser is the check/loop lanes' concern, and most
 *  orchestrators are pure API/rules suites). No pre-registered live run either: the orchestrator's own
 *  Runner.run calls mint first-class runs/<id> entries as they execute — a wrapper live-run would just
 *  dangle "running" forever (no bot lifecycle and no Check.run settle boundary ever settles it). Fire
 *  the eval with a keepalive fetch (it survives the navigation), then poll briefly for the FIRST run the
 *  orchestrator mints and return its id so the caller lands on the actual run (the streaming suite view),
 *  not the list. Returns null if nothing appears in time (an orchestrator that errors before its first
 *  Runner.run, or one that runs none) — the caller falls back to /runs. */
async function kaEnsureLaunch(name) {
  const before = await karate.value('Harness.runs()');
  const head = (before.runs && before.runs[0] && before.runs[0].runId) || '';
  const body = 'Runner.launch(' + JSON.stringify(name) + ')';
  fetch('/api/eval', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body, keepalive: true });
  for (let i = 0; i < 15; i++) {
    await new Promise((r) => setTimeout(r, 300));
    const now = await karate.value('Harness.runs()');
    const top = now.runs && now.runs[0];
    if (top && top.runId !== head) return top.runId;   // newest-first — the orchestrator's first run
  }
  return null;
}

async function kaEnsureLiveCheck(name, opts) {
  opts = opts || {};
  // `typeof bot` alone is a trap: a destroyed bot (an LLM run ended with bot.destroy()) is still an
  // object whose every verb returns CONNECTION_LOST. The wire snapshot carries `alive` — ask for that.
  const env = await karate.eval('typeof bot === "object" ? bot : null');
  if (!(env.payload && env.payload.alive)) await karate.value('var bot = Robot.create()');
  // register the run BEFORE firing so we can navigate to it; Check.run stamps THIS run (onRun) and the
  // server settles it at the eval boundary (onEval) — the deterministic eval itself stays Harness-free.
  let sid = await karate.value('Harness.runs().active');
  if (!sid) sid = (await karate.value('Harness.startLive()')).runId;
  const body = (opts.record ? 'Harness.recordLive(); ' : '')
             + 'Check.run(' + JSON.stringify(name) + ')';
  fetch('/api/eval', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body, keepalive: true });
  return sid;
}

// ---- the global openers ------------------------------------------------------------------------------

/** Normalize a project-root `path` handoff: anything that does not name a LEAF (.feature/.js/.md, and
 *  so .karate.js too) is a FOLDER and gets the trailing slash. The explorer appends one, a `?path=checks`
 *  query hand-off does not — normalizing at the seam means every lane test downstream reads one form. */
function kaNormalizePath(p) {
  p = (p || '').trim();
  if (!p || p === '.' || p.endsWith('/')) return p;
  return /\.(feature|js|md)$/.test(p) ? p : p + '/';
}

/** Open the kickoff modal, optionally preselected. detail: {path?(project-root .feature/folder),
 *  check?(checks-relative .js/.md), goal?, req?}. */
function kaRunModalOpen(detail) {
  window.dispatchEvent(new CustomEvent('ka-run-modal-open', { detail: detail || {} }));
}

/** RUN AGAIN (D281 c) — open the kickoff modal PRE-POPULATED from a settled suite run's record: the
 *  recorded selector becomes the chip set (or the single folder selector), the effective options fill the
 *  form. The user submits from there, so editing first is just the run-a-varied-set flow. */
function kaRunAgainOpen(selector, options) {
  kaRunModalOpen({ again: { selector: [].concat(selector || []), options: options || {} } });
}

/** The split-button primary: run immediately with defaults and jump to the live view — no UI. A
 *  determinate .js check or .feature/folder runs now; anything ambiguous opens the modal to choose. */
async function kaRunNow(detail) {
  detail = detail || {};
  if (detail.path) detail.path = kaNormalizePath(detail.path);
  try {
    if (detail.check && detail.check.endsWith('.karate.js')) {
      // an orchestrator is NOT a check — launch it headless, no bot (see kaEnsureLaunch)
      const sid = await kaEnsureLaunch(detail.check);
      location.href = sid ? '/runs/' + encodeURIComponent(sid) : '/runs';
      return;
    }
    if (detail.check && detail.check.endsWith('.js')) {
      // a deterministic check does not record by default (it's not a Harness/AI run) — just drive + report
      const sid = await kaEnsureLiveCheck(detail.check, {});
      location.href = '/runs/' + encodeURIComponent(sid);
      return;
    }
    if (detail.path && (detail.path.endsWith('.feature') || detail.path.endsWith('/'))) {
      const out = await kaKickSuite(detail.path, { async: true });
      location.href = '/runs/' + encodeURIComponent(out.runId);
      return;
    }
    kaRunModalOpen(detail);   // .md / indeterminate — let the user choose in the modal
  } catch (e) { alert(e); }
}

// ---- the Alpine factory -----------------------------------------------------------------------------

function runModal() {
  // the LLM selector — flat provider · model combos off the per-provider shortlists (SSR island);
  // BOTH provider and model are passed to Harness.explore (a provider pick alone silently ran the default).
  const llmEl = document.getElementById('llm-data');
  const llm = (llmEl && JSON.parse(llmEl.textContent)) || { providers: [], defaultModel: '' };
  const combos = [];
  llm.providers.forEach((p) => {
    const models = p.models && p.models.length ? p.models : [{ id: '', label: '' }];
    models.forEach((m) => combos.push({
      // `model` (the wire value) stays the id/alias — provider routing depends on it; the LABEL shows the
      // EXACT version (`resolved`) so the user sees which sonnet/gemma/opus actually runs, not just "sonnet".
      provider: p.name, model: m.id,
      label: m.label
        ? p.name + ' · ' + m.label + (m.resolved && m.resolved !== m.label ? ' (' + m.resolved + ')' : '')
        : p.name,
      def: !!p.default && (m.id === llm.defaultModel || !m.id)
    }));
  });
  // the file picker's candidates (.feature suites + .js checks + .md notes), SSR-embedded
  const pickEl = document.getElementById('pickables-data');
  const pickables = pickEl ? (JSON.parse(pickEl.textContent) || []) : [];
  // the in-flight run snapshot (a string island embeds as RAW text) — single-user gate (D48)
  const activeEl = document.getElementById('activeRun-data');
  const active = activeEl ? activeEl.textContent.trim() : '';

  return {
    open: false,
    // selected[] = the ordered SET: .feature paths + checks/*.js members, project-root relative (D281);
    // file = a single-leaf pick (a folder selector, a .md goal, a *.karate.js orchestrator);
    // query = the typeahead text. A set member and a single-leaf pick never coexist.
    selected: [], file: '', query: '', fileOpen: false,
    goal: '', req: '',
    activeRun: active,
    starting: false, err: '', copied: false,
    record: true, video: true, combos, combo: Math.max(0, combos.findIndex((c) => c.def)),
    watchLive: true, runWithAi: false,   // runWithAi = the Harness.heal repair mode for a single check (D163)
    maxTurns: 50,   // the autonomous loop's iteration ceiling → Harness.explore maxIterations (default 50)
    tags: '', env: '', threads: 1, formal: false,
    pickables,
    history: JSON.parse(localStorage.getItem('ka-goal-history') || '[]'), histOpen: false,

    // --- open / close ---
    show(d) {
      d = d || {};
      if (d.goal != null) this.goal = d.goal;
      if (d.req != null) this.req = d.req;
      if (d.path) this.applyPath(d.path);                          // project-root .feature/folder
      if (d.check) this.applyCheck(d.check);                       // checks-relative .js/.md/.karate.js
      if (d.again) this.applyAgain(d.again.selector, d.again.options);   // a recorded suite run (D281 c)
      this.err = '';
      this.open = true;
    },
    close() { this.open = false; this.starting = false; this.fileOpen = false; },
    applyPath(p) {
      p = kaNormalizePath(p);                                        // a folder hand-off carries the slash
      if (!p) return;
      if (p.endsWith('.feature')) this.addMember(p);
      else { this.file = p; this.selected = []; }                  // a folder IS the set — one selector
    },
    applyCheck(c) {
      c = (c || '').trim();
      if (!c) return;
      // a plain .js check is a set member (D281); a .md goal / a .karate.js orchestrator is single-leaf
      if (c.endsWith('.js') && !c.endsWith('.karate.js')) this.addMember('checks/' + c.replace(/^checks\//, ''));
      else { this.file = c; this.selected = []; }
    },
    // RUN AGAIN (D281 c) — the recorded selector + effective options fill the form; submit is the normal
    // one. `.feature`/`.js` entries are the ordered chip set; anything else (a folder, '.') is the
    // single-leaf selector, which re-resolves at kickoff.
    applyAgain(selector, options) {
      const sel = [].concat(selector || []).map(String);
      const rec = options || {};
      this.selected = []; this.file = '';
      if (sel.length === 1 && !/\.(feature|js)$/.test(sel[0])) this.file = sel[0];
      else sel.forEach((p) => this.addMember(p));
      this.env = rec.env || '';
      // core ANDs a tag LIST (varargs) but reads a lone string as one expression — >1 needs the enhanced
      // form (Tags.fromKarateOptionsTags) to keep the AND the recorded run had
      const tags = [].concat(rec.tags || []).map(String);
      this.tags = tags.length > 1
        ? tags.map((t) => (t.startsWith('~') ? "not('" + t.slice(1) + "')" : "anyOf('" + t + "')")).join(' && ')
        : (tags[0] || '');
      this.video = !!rec.video;
      if (rec.threads > 1 && !this.hasCheck()) this.threads = rec.threads;
    },

    // --- the picker ---
    addMember(name) {
      if (!this.selected.includes(name)) this.selected.push(name);
      this.file = '';                                              // a set and a single-leaf pick never mix
    },
    toggleMember(name) {
      const i = this.selected.indexOf(name);
      if (i >= 0) this.selected.splice(i, 1); else this.addMember(name);
    },
    pick(p) {
      if (p.type === 'feature' || p.type === 'js') { this.toggleMember(p.name); }
      else { this.file = p.name; this.selected = []; }             // a .md / a .karate.js is single-leaf
      this.query = ''; this.fileOpen = false;
    },
    get filteredFiles() {
      const q = (this.query || '').trim().toLowerCase();
      return this.pickables.filter((p) => p.name.toLowerCase().includes(q)).slice(0, 12);
    },

    // --- the lane, by what's chosen ---
    kind() {
      if (this.selected.length) return 'suite';        // the set: features + checks, in any mix (D281)
      const f = this.file.trim();
      if (f.endsWith('.feature') || f.endsWith('/') || f === '.') return 'suite';
      if (f.endsWith('.karate.js')) return 'launch';   // an orchestrator — headless, never the bot
      return 'loop';
    },
    canRun() {
      const k = this.kind();
      if (k === 'suite') return this.selected.length > 0 || this.file.trim() !== '';
      if (k === 'launch') return this.file.trim() !== '';
      return this.goal.trim() !== '' || this.file.trim() !== '';   // loop: a goal or a .md
    },
    // the path arg fed to Runner.run: the ordered member list, or a single folder selector
    suitePathArg() {
      return this.selected.length ? this.selected.slice() : this.file.trim();
    },
    // a check member drives a bot on the container display, so the engine forces threads=1 (D281)
    hasCheck() { return this.selected.some((p) => p.endsWith('.js')); },
    // a display-capable SELECTOR: a check member, or a folder / '.' (it re-resolves at kickoff, so it
    // may bring checks in) — those can be watched and recorded; a feature-only chip set is headless,
    // with nothing to film. Read off the path arg, so "Run ALL" ('.') decides like any folder does.
    displaySelector(pathArg) {
      return [].concat(pathArg || []).some((p) => {
        p = String(p);
        return p.endsWith('.js') || p.endsWith('/') || p === '.';
      });
    },
    canVideo() { return this.kind() === 'suite' && this.displaySelector(this.suitePathArg()); },
    // the heal lane's subject (D163): a set of exactly one check — "Run with AI" re-drives + repairs it
    soloCheck() {
      return this.selected.length === 1 && this.selected[0].endsWith('.js') ? this.selected[0] : '';
    },

    // --- prompt history (a sanctioned localStorage exception, with theme) ---
    saveHistory() {
      const p = this.goal.trim();
      if (!p) return;
      this.history = [p].concat(this.history.filter((h) => h !== p)).slice(0, 8);
      localStorage.setItem('ka-goal-history', JSON.stringify(this.history));
    },
    dropHistory(h) {
      this.history = this.history.filter((x) => x !== h);
      localStorage.setItem('ka-goal-history', JSON.stringify(this.history));
      if (!this.history.length) this.histOpen = false;
    },

    // the live-run connect prompt — deliberately MINIMAL (one curl + follow it); everything else lives
    // in the served focused set (/api/prompts/browser), not the clipboard
    async copyLlmPrompt() {
      const text = 'Run `curl -s ' + location.origin + '/api/prompts/browser` and follow those instructions.';
      await kaCopy(text);
      this.copied = true;
      setTimeout(() => { this.copied = false; }, 1800);
    },

    // --- kickoff ---
    async runRun() {
      if (!this.canRun() || this.starting) return;
      const k = this.kind();
      const solo = this.soloCheck();
      // a set runs headless-or-leased through the suite lane; "Run with AI" on a single check is the
      // one deliberate exception — the heal engine re-drives the flow on the live bot (D163)
      if (k === 'suite' && !(solo && this.runWithAi)) { await this.runSuite(this.suitePathArg()); return; }
      // the live-run lanes (heal/loop/launch) are single-user gated (D48) — one live run at a time
      if (this.activeRun) {
        this.err = 'a run is already in flight (' + this.activeRun + ') — finish or stop it first (single-user)';
        return;
      }
      this.starting = true; this.err = '';
      try {
        if (k === 'launch') {
          // a *.karate.js orchestrator — headless, no bot, no record (there is no browser to record)
          await this.runLaunch(this.file.trim());
          return;
        }
        if (solo) {
          await this.runHeal(solo);
          return;
        }
        // the loop (free-form exploratory run): a typed goal + an optional .md as hints
        const opts = { robot: true, async: true, record: this.record };
        if (this.req.trim()) opts.req = this.req.trim();
        const sel = this.combos[this.combo];
        if (sel) { opts.provider = sel.provider; if (sel.model) opts.model = sel.model; }
        if (this.maxTurns > 0) opts.maxIterations = Number(this.maxTurns);
        let task = this.goal.trim();
        const md = this.file.trim();
        if (md) { if (task) opts.hints = md; else task = md; }
        const out = await kaKickLoop(task, opts);
        this.saveHistory();
        this.land(out.runId);
      } catch (e) { this.err = String(e); this.starting = false; }
    },
    async runSuite(pathArg) {
      if (this.starting) return;
      this.starting = true; this.err = '';
      const opts = { async: true };
      if (this.tags.trim()) opts.tags = this.tags.trim();
      if (this.env.trim()) opts.env = this.env.trim();
      if (this.threads > 1 && !this.hasCheck()) opts.threads = this.threads;
      if (this.formal) opts.formal = true;
      if (this.req.trim()) opts.req = this.req.trim();
      if (this.video && this.displaySelector(pathArg)) opts.video = true;   // one display timeline for the whole set (D281)
      try {
        const out = await kaKickSuite(pathArg, opts);
        this.land(out.runId);
      } catch (e) { this.err = String(e); this.starting = false; }
    },
    // Run ALL under the project root ('.') — every .feature plus every checks/*.js the engine discovers;
    // honors the suite tags/env/threads/formal fields, and '.' is display-capable like any folder, so it
    // records unless the video toggle was unchecked
    async runAll() {
      this.selected = []; this.file = '';
      await this.runSuite('.');
    },
    // a *.karate.js orchestrator — Runner.launch, headless (see kaEnsureLaunch); lands on the FIRST
    // run the orchestrator mints (falls back to the /runs list if none appears in time)
    async runLaunch(name) {
      try {
        const sid = await kaEnsureLaunch(name);
        if (sid) { this.land(sid); return; }
        this.close();
        if (location.pathname === '/runs') location.reload(); else location.href = '/runs';
      } catch (e) { this.err = String(e); this.starting = false; }
    },
    // "Run with AI" → Harness.heal: run the check, and on drift let the LLM repair it to green (D163)
    async runHeal(name) {
      try {
        const out = await kaKickHeal(name, { async: true, record: this.record });
        this.land(out.runId);
      } catch (e) { this.err = String(e); this.starting = false; }
    },
    // watch-live → navigate to the streaming run; else close (on /runs reload to show it in-flight; a
    // completion toast settles it from anywhere)
    land(runId) {
      this.close();
      if (this.watchLive) { location.href = '/runs/' + encodeURIComponent(runId); return; }
      if (location.pathname === '/runs') location.reload();
    }
  };
}
