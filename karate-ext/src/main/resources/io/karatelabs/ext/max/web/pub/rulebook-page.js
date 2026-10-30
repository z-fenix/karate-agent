/*
 * rulebook-page.js — the Alpine factory for /rulebooks/{name} (the rulebook-sim + diff editor).
 * Extracted verbatim from rulebook.html (D114) to keep the SSR template lean and
 * move page logic to /pub (the schema-form / json-table / run-all / pane-search pattern). Loaded
 * via <script src>; reads its SSR snapshot from the #rulebook-bootstrap JSON island and uses the
 * global helpers JsonTable / SchemaForm / RunAll / PaneSearch + window.cmEditor at runtime.
 */
/**
 * The breadcrumb-row actions (a tiny standalone Alpine component, scoped to the breadcrumb which sits
 * ABOVE the main rulebookPage() x-data root). Two actions, both generating the standalone Rule Analysis
 * report and opening it in a new tab: "Analysis report ↗" (Coverage · Value Ranges · Rule Check ·
 * What-If) and "Self-grade ↗" (the mutation self-grade first, landing on the report's Mutation tab).
 * The blank tab opens SYNCHRONOUSLY on click (keeps the user gesture so popup blockers allow it), then
 * is pointed at the generated artifact URL once the /ui/ route returns. The browser focuses that tab at
 * once, so the progress line is written INTO it — a "Grading…" label left on this page is never seen.
 * The report honors the page's karate-theme (same localStorage key).
 */
function rulebookActions() {
  var boot = JSON.parse(document.getElementById('rulebook-bootstrap').textContent || '{}');
  var holdTab = function (tab, title, line) {
    if (!tab) return;
    var dark = false;
    try { dark = localStorage.getItem('karate-theme') === 'dark'; } catch (e) { /* storage blocked */ }
    var d = tab.document;
    d.open();
    d.write('<!doctype html><title>' + title + '</title><body style="margin:0;height:100vh;display:flex;'
      + 'align-items:center;justify-content:center;font:14px system-ui,sans-serif;'
      + (dark ? 'background:#0f172a;color:#cbd5e1' : 'background:#f8fafc;color:#475569') + '">'
      + '<div style="text-align:center"><div style="font-size:16px;font-weight:600;margin-bottom:6px">'
      + title + '</div><div>' + line + '</div></div></body>');
    d.close();
  };
  return {
    rbName: boot.name,
    reportBusy: false,
    reportError: '',
    gradeBusy: false,
    gradeError: '',
    // the latest premium-match ingest per sheet (D328(g)), SSR'd; empty when none was ingested
    match: boot.match || [],
    /** "pc match: 31 match · 2 mismatch · 1 ±0.01 · 3 not rated" off the newest ingest — '' when none */
    matchLine: function () {
      var m = this.match[0];
      if (!m) return '';
      var c = m.counts || {}, fig = (m.mapping || {}).figure || {};
      var parts = [['match', ''], ['mismatch', ''], ['match (±x)', '±' + fig.tolerance],
        ['rated but expected refused', ''], ['not rated', ''], ['unknown case', '']]
        .filter(function (k) { return c[k[0]]; })
        .map(function (k) { return c[k[0]] + ' ' + (k[1] || k[0]); });
      var who = (m.producer && m.producer.name) || (m.mapping || {}).name || 'carrier';
      return who + ' match: ' + (parts.join(' · ') || 'nothing rated')
        + ((m.disclosures || {}).mockOnly ? ' (mock)' : '')
        + (this.match.length > 1 ? ' · +' + (this.match.length - 1) + ' other sheet' + (this.match.length > 2 ? 's' : '') : '');
    },
    openReport: async function (tab) {
      if (this.reportBusy) return;
      this.reportError = '';
      var win = window.open('about:blank', '_blank');
      holdTab(win, 'Generating the analysis report', 'Rule Analysis — ' + this.rbName + ' opens here in a moment.');
      this.reportBusy = true;
      try {
        var res = await fetch('/ui/rulebooks/' + encodeURIComponent(this.rbName) + '/report');
        var j = await res.json();
        if (j && j.error) {
          if (win) win.close();
          this.reportError = 'Report failed: ' + (j.error || j.code || 'unknown');
        } else if (win) {
          win.location = j.url + (tab ? '#analysis=' + tab : '');
        } else {
          window.open(j.url + (tab ? '#analysis=' + tab : ''), '_blank');   // popup blocked → best-effort second attempt
        }
      } catch (e) {
        if (win) win.close();
        this.reportError = 'Report failed: ' + e;
      } finally {
        this.reportBusy = false;
      }
    },
    selfGrade: async function () {
      if (this.gradeBusy) return;
      this.gradeError = '';
      var tab = window.open('about:blank', '_blank');
      holdTab(tab, 'Grading ' + this.rbName + '…',
        'Seeding defects into a copy of the rulebook and checking what its own oracles notice. '
        + 'The Mutation tab opens here when done.');
      this.gradeBusy = true;
      try {
        var res = await fetch('/ui/rulebooks/' + encodeURIComponent(this.rbName) + '/mutate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: '{}'
        });
        var j = await res.json();
        if (j && j.error) {
          if (tab) tab.close();
          this.gradeError = 'Self-grade failed: ' + (j.error || j.code || 'unknown');
        } else if (tab) {
          tab.location = j.url;
        } else {
          window.open(j.url, '_blank');           // popup blocked → best-effort second attempt
        }
      } catch (e) {
        if (tab) tab.close();
        this.gradeError = 'Self-grade failed: ' + e;
      } finally {
        this.gradeBusy = false;
      }
    }
  };
}

/** A record's confirmation draft: the current confirmation when there is one, else nothing checked and
 *  each step's values as the record has them. */
function playDraft(r) {
  var c = r.confirmation && r.confirmation.state === 'current' ? r.confirmation : null;
  var expect = (c && c.expect) || {}, byAt = {}, d = { steps: {}, 'final': {}, req: {} };
  (expect.steps || []).forEach(function (e) { byAt[e.at] = e; });
  r.steps.forEach(function (s) {
    var e = byAt[s.at];
    d.steps[s.at] = { on: !!e, outcome: e ? e.outcome : s.disposition,
      stage: (e ? e.stage : s.stage) || '', reason: (e ? e.reason : s.reason) || '' };
  });
  Object.keys((r.replay && r.replay['final']) || {}).forEach(function (k) {
    d['final'][k] = !!(expect['final'] && k in expect['final']);
  });
  (r.candidates || []).forEach(function (cand) { d.req[cand.id] = !!(c && c.req.indexOf(cand.id) !== -1); });
  return d;
}

/**
 * The house route-failure envelope (D98) — a top-level STRING `error` beside code/status, mirrored
 * onto a non-2xx. A 2xx payload whose `error` is an OBJECT is data, not a failed request: the Twin
 * pane's section carries the refused walk (the D272 box) that way, beside the rows it still has.
 */
function isRouteFailure(json) {
  return !!(json && typeof json.error === 'string');
}

function rulebookPage() {
  // ── Bootstrap ──
  var boot = JSON.parse(document.getElementById('rulebook-bootstrap').textContent || '{}');
  var rbName = boot.name;

  // LAYOUT persists per-browser via kaPrefs (the D114 session-prefs substrate) — rb.* keys,
  // SSR-hydrated off the #prefs-data island so the page comes back pre-arranged with no GET/flash.
  // SELECTION (selectedId) + the unsaved-draft cache (editsBySc) stay IN-MEMORY (per-tab identity a
  // cookie-shared session would fight; large nested drafts don't belong in a prefs store). NO
  // localStorage (theme is the one app-wide exception, in layout.html). See console.md "Console v4".

  // the /ui/ page-data channel (D98) — JSON in, JSON out; domain failures are {error, code, status}
  async function ui(method, path, body) {
    var res = await fetch('/ui' + path, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    });
    var json = await res.json();
    if (json && json.error) json.__status = res.status;
    return json;
  }

  return {
    // ── State ──
    rbName: rbName,
    rb: boot.rule,                                       // re-fetched after mutations
    tableRowsLoaded: false,                              // file tables arrive as summaries until asked for
    // v4 altitude split (D114): LEFT Scenarios | CENTER Input|Result (scenario-scoped, flex-grow) |
    // RIGHT Reference panel (rulebook-scoped, DOCKABLE). The center grows; left + ref are fixed-%.
    // Layout persists per-browser via kaPrefs (rb.* keys), hydrated in init() off the #prefs-data island.
    leftPct: 20,
    refPct: 30,                                          // Reference panel width when docked
    centerSplit: 55,                                     // Input column % of the center Input|Result pair
    refSlot: 'lookup',                                   // lookup | code | diagram | scenario-coverage | check | twin
    refState: 'docked',                                  // docked (a column) | collapsed (a thin rail)
    coverageTransposed: true,                            // ⤢ Scenario Coverage orientation (fields=rows, scenarios = cols)
    draggingLeft: false,
    draggingRef: false,
    // the CENTER Result column stacks Audit over Output — cols keeps just that pair now (the
    // center-column inputs/lookups/diagram/source toggles are gone, replaced by the Reference slot)
    cols: { audit: true, output: true },
    outputSplit: 32,                                     // Output % of the Result column (Audit takes the rest)
    search: '',
    selectedId: null,                                    // set to the first scenario in init()
    renamingId: null,
    renameDraft: '',
    deletingId: null,
    // editsBySc[id] = {edit: <deep clone minus the _-prefixed keys>, editLabel} — per-scenario draft cache,
    // IN-MEMORY only (a reload drops unsaved edits; save-then-run is the model)
    editsBySc: {},
    inputCollapsed: {},
    lookupsDraft: {},
    lookupCollapsed: {},
    saving: false,
    saveMessage: '',
    saveError: false,
    // runResults[id] = the full run result (output + audit + diff + trace) — RETAINED per
    // scenario (D111d); selecting a row shows ITS result. NOT persisted — reload is clean.
    runResults: {},
    runningIds: {},
    dirtySource: false,
    // the Rule.check triage SSRs in via the island (the Health slot) — recomputed on page reload
    check: boot.check || null,
    // Rule.cells' summary (D326), SSR'd for a book with file tables only
    cells: boot.cells || null,
    // the Twin pane (§17.4). NOT SSR'd: the payload is two walks, so it loads lazily on first open.
    twinData: null,
    twinLoading: false,
    twinRuns: {},                    // sequenceId → its Twin.run envelope (in-memory, like runResults)
    twinRunning: {},
    // the play records slot (playground.md §6 (1)) — the confirmation drafts are in-memory until confirmed
    playData: null,
    playOpen: {},
    playDrafts: {},                  // recordId → {steps: {at: {on, outcome, stage, reason}}, final: {key: on}, req: {id: on}}
    playSaving: {},
    playMissing: null,               // a ?play=<id> the quarantine does not hold
    playSelected: null,
    // the diagram SSRs in via the island (D111e) — sha-keyed by mtimes.calc
    diagramSvg: (boot.diagram && boot.diagram.svg) || '',
    diagramMermaid: (boot.diagram && boot.diagram.mermaid) || '',
    diagramError: boot.diagram && boot.diagram.error
        ? (boot.diagram.code || '') + ': ' + boot.diagram.error : '',
    diagramSha: boot.rule && boot.rule.mtimes ? boot.rule.mtimes.calc : null,
    // the explore report depends on calc.js AND tables/ — keyed on the rulebook digest (D325(a))
    exploreDigest: boot.rule && boot.rule.digest ? boot.rule.digest.sha256 : null,
    // D111-b collapse + outline: big containers SSR folded; expandSet/expandAll refetch
    // server-rendered states (in-memory only — a reload returns to the folded default)
    diagramOutline: (boot.diagram && boot.diagram.outline) || [],
    diagramCollapsed: (boot.diagram && boot.diagram.collapsed) || [],
    diagramCollapsibleAll: (boot.diagram && boot.diagram.collapsibleAll) || [],
    expandSet: [],
    expandAll: false,
    compact: true,                   // D114 business view (compact flowchart) — default ON
    outlineOpen: false,
    mermaidCopied: false,
    exploreReport: null,
    exploreLoading: false,
    explorePanel: false,
    lookupPopover: null,
    _hoverEl: null,
    runAllRunning: false,
    runAllStop: false,
    runAllSummary: null,
    runAllDoneCount: 0,
    actionMenuId: null,
    // D113-a: per-container audit collapse (client state, default expanded), the diagram
    // full-screen drawer, and the per-pane search boxes (plain literals — PaneSearch supplies
    // only static helpers, so load order with the bottom <script src> never bites)
    auditCollapsed: {},
    diagramFull: false,
    // D114 Test Data Design (the 'table' center view + its full-screen drawer)
    tableData: null,
    tableSuite: 'coverage',          // coverage | pairwise | boundary
    tableLoading: false,
    tableHideScenarios: false,
    onlyExisting: true,              // "Existing" strategy — DEFAULT: show only saved scenarios (no
                                     // auto-generated candidates until a strategy is picked)
    tableFull: false,
    // expanded Scenario Coverage: a clicked scenario → its audit+output in the right split (Rule.execute);
    // the workbench split is always present (empty-hint when nothing selected) + draggable
    selectedScenarioIdx: null,
    scenarioRun: null,
    scenarioRunLoading: false,
    scenarioAuditCollapsed: {},
    scenarioWbPct: 30,                   // workbench width % of the ⤢ Scenario Coverage drawer (draggable)
    scenarioCols: { audit: true, output: true },   // workbench reuses the Result audit⇄output split pattern
    scenarioOutputSplit: 35,             // Output % of the workbench (Audit takes the rest)
    fieldSel: {},                    // matrix field-picker: path→false hides that input column (absent = show)
    fieldMenuOpen: false,
    showDecisions: true,             // matrix: show the decision (branch-condition) rows/cols
    matrixColW: 200,                 // the matrix sticky first column width (px) — drag-resizable, fixed (no jank)
    scenarioAuditSearch: { open: false, q: '' },   // workbench audit search (fullscreen Scenario Coverage)
    // click-a-node steps popover (business-view; the merged bubble's <title> lists its inlined steps)
    stepsPopover: null,
    // ⛶ Combo (expanded diagram view): column on/off + draggable widths (diagram is the grow column)
    comboCols: { outline: true, diagram: true, code: true },
    comboOutlinePct: 20,
    comboCodePct: 38,
    // "save as new scenario" inline prompt (when the input draft is dirty)
    savingAsNew: false,
    newScenarioName: '',
    inputsSearch: { open: false, q: '', matches: [], idx: 0 },
    lookupsSearch: { open: false, q: '', matches: [], idx: 0 },
    auditSearch: { open: false, q: '', matches: [], idx: 0 },

    // ── Init (Alpine auto-invokes init() — NO x-init on the element, the double-run gotcha) ──
    init() {
      var self = this;
      // hydrate the persisted layout (kaPrefs.read off the SSR #prefs-data island — D114).
      // Numbers parse-with-fallback; booleans/strings read straight (a stored `false` must survive).
      var numPref = function (k, d) { var v = parseFloat(kaPrefs.read(k, d)); return isNaN(v) ? d : v; };
      this.leftPct = numPref('rb.leftPct', this.leftPct);
      this.refPct = numPref('rb.ref.width', this.refPct);
      this.centerSplit = numPref('rb.centerSplit', this.centerSplit);
      this.outputSplit = numPref('rb.resultSplit', this.outputSplit);
      this.refSlot = kaPrefs.read('rb.ref.slot', this.refSlot);
      if (this.refSlot === 'outline') this.refSlot = 'lookup';   // the outline tab was retired (lives in the Combo)
      this.refState = kaPrefs.read('rb.ref.state', this.refState);
      this.compact = kaPrefs.read('rb.compact', this.compact);
      this.coverageTransposed = kaPrefs.read('rb.coverageTransposed', this.coverageTransposed);
      // D115 deep-link — a URL param addresses a specific Reference slot + scenario (the ?file=
      // editor precedent). A param WINS for this load over the kaPrefs-persisted default, so View.navigate
      // can hand the human a fully-addressed view: /rulebooks/<name>?ref=diagram&scenario=<id>. Set refSlot
      // directly (not setRefSlot — no persist; the init tail below fires the slot's lazy data loads).
      var params = new URLSearchParams(location.search);
      var qRef = params.get('ref');
      if (qRef && ['lookup', 'code', 'diagram', 'scenario-coverage', 'check', 'twin'].indexOf(qRef) !== -1) {
        this.refSlot = qRef;
      }
      // selection drives the edit draft + the hot-path overlay (no persistence — in-memory, D114)
      this.$watch('selectedId', function (v) {
        self.loadEdit();
        self.saveMessage = '';
        // the hot-path overlay follows the selection (its retained trace)
        self.$nextTick(function () { self.decorateDiagram(); });
      });
      var qScenario = params.get('scenario');
      if (qScenario && this.scenarioById(qScenario)) {
        this.selectedId = qScenario;                       // deep-link wins over the first-scenario default
      } else if (!this.selectedId || !this.scenarioById(this.selectedId)) {
        if (this.rb.scenarios && this.rb.scenarios.length > 0) {
          this.selectedId = this.rb.scenarios[0]._id;
        }
      } else {
        this.loadEdit();
      }
      this.loadLookupsDraft();

      // CodeMirror bridge — the vendored module dispatches cm-dirty on user edits and cm-ready
      // when mounted (the checks.html pattern); push the initial calc buffer when ready
      window.addEventListener('cm-dirty', function () { self.dirtySource = true; });
      window.addEventListener('cm-ready', function () { self.pushSourceToEditor(); });
      // The LLM points INSIDE this page via View.highlight(id, {node|line}); layout.html re-dispatches
      // the typed sub-target here (read-side navigation only — P27/P23, never authoring). {node} → the
      // diagram slot + the existing jump/flash plumbing; {line} → the source slot + CodeMirror gotoLine.
      window.addEventListener('ka-view-target', function (e) {
        var d = e.detail || {};
        if (d.node) { self.focusNode(String(d.node)); return; }
        if (d.line != null) { self.focusLine(parseInt(d.line, 10)); }
      });
      if (window.cmEditor) this.pushSourceToEditor();
      // the SSR diagram renders compact (the default); if the user last chose "All steps", refetch
      if (!this.compact) this.$nextTick(function () { self.refetchDiagram(); });
      this.$nextTick(function () { self.decorateDiagram(); });
      // a persisted Reference slot may need lazy data / a CM remeasure (it starts hidden)
      if (this.refSlot === 'scenario-coverage') this.loadTable();
      if (this.refSlot === 'twin') this.loadTwin();
      this.$nextTick(function () {
        if (self.refSlot === 'code' && window.cmEditor && window.cmEditor.refresh) window.cmEditor.refresh();
      });
    },

    pushSourceToEditor: function () {
      if (!window.cmEditor) return;
      window.cmEditor.setDoc(this.rb.calc || '');
      this.dirtySource = false;
    },

    /** switch the Reference panel's single visible slot (lookup|code|diagram|scenario-coverage|check|twin).
        Carries the three behaviors setCenterView used to: the unsaved-calc guard when LEAVING code,
        decorateDiagram on $nextTick when ENTERING diagram, the lazy data load for scenario-coverage/twin.
        Slots toggle by display (x-show) so the hosts (SVG, CodeMirror) persist — never x-if. */
    setRefSlot: function (next) {
      var self = this;
      if (this.refSlot === 'code' && this.dirtySource && next !== 'code') {
        this.saveMessage = 'calc.js has unsaved edits — your buffer is kept';
        this.saveError = false;
        setTimeout(function () { if (self.saveMessage.indexOf('unsaved edits') !== -1) self.saveMessage = ''; }, 4000);
      }
      // selecting a slot on a collapsed rail re-docks the panel
      if (this.refState === 'collapsed') this.setRefState('docked');
      this.refSlot = next;
      kaPrefs.set('rb.ref.slot', next);
      if (next === 'diagram') {
        this.$nextTick(function () { self.decorateDiagram(); });
      } else if (next === 'code') {
        this.$nextTick(function () { if (window.cmEditor && window.cmEditor.refresh) window.cmEditor.refresh(); });
      } else if (next === 'scenario-coverage' && !this.tableData) {
        this.loadTable();
      } else if (next === 'twin' && !this.twinData) {
        this.loadTwin();
      }
    },
    /** dock ⇄ collapse-to-rail the Reference panel (persisted) */
    setRefState: function (s) {
      this.refState = s;
      kaPrefs.set('rb.ref.state', s);
    },
    toggleRefState: function () { this.setRefState(this.refState === 'docked' ? 'collapsed' : 'docked'); },

    // ── Twin pane (§17.4 — the behaviour model: the walk, its findings, the sequence rail) ──
    loadTwin: async function () {
      this.twinLoading = true;
      var j = await ui('GET', '/rulebooks/' + this.rbName + '/twin');
      this.twinLoading = false;
      if (isRouteFailure(j)) { this.twinData = null; return this.handleError(j); }
      this.twinData = j;      // a nested `error` rides on — the pane discloses it above the rail
      if (j.twin) this.loadPlays();
    },
    twinExplore: function () { return (this.twinData && this.twinData.explore) || null; },
    twinCheck: function () { return (this.twinData && this.twinData.check) || null; },
    twinCi: function () { var c = this.twinCheck(); return (c && c.ci) || null; },
    twinFindings: function () { var c = this.twinCheck(); return (c && c.findings) || {}; },
    twinFidelity: function () { var e = this.twinExplore(); return (e && e.fidelity) || null; },
    twinCeilings: function () { var e = this.twinExplore(); return (e && e.ceilings) || null; },
    twinUsage: function () { var c = this.twinCeilings(); return (c && c.usage) || null; },
    twinStates: function () { var e = this.twinExplore(); return (e && e.states) || null; },
    twinTransitions: function () { var e = this.twinExplore(); return (e && e.transitions) || null; },
    twinStats: function () { var e = this.twinExplore(); return (e && e.stats) || null; },
    twinCounterexamples: function () { var e = this.twinExplore(); return (e && e.counterexamples) || []; },
    twinSequences: function () { return (this.twinData && this.twinData.sequences) || []; },
    /** the record's refusal vocabulary, harvested at read (§17.5) — absent until one was observed */
    twinWireRefusals: function () { return (this.twinData && this.twinData.wireRefusals) || []; },
    /** status · the body's own error and message · why the model refused — the raw body only when the
     *  shape names neither, so a reader never has to parse JSON to read a refusal */
    twinWireRefusalText: function (r) {
      var b = r.body;
      var o = b && typeof b === 'object' && !Array.isArray(b) ? b : null;
      var err = o ? (o.error != null ? o.error : o.code) : null;
      var msg = o ? (o.message != null ? o.message : o.detail) : null;
      return r.status + ' · ' + (err != null ? String(err) : (b === undefined ? '–' : JSON.stringify(b)))
          + (msg != null ? ' · ' + String(msg) : '')
          + ' · model refused: ' + ((r.reasons || []).join(', ') || '–');
    },
    /** the live record's reading of one saved sequence (§17.6, D319) — null reads "not run live". The
     *  wording is duplicated verbatim in static/rule-analysis-report.js (liveView). */
    twinLive: function (id) {
      var rows = (this.twinData && this.twinData.live && this.twinData.live.rows) || [];
      var r = rows.filter(function (x) { return x.sequenceId === id; })[0];
      if (!r) return null;
      var d = r.disposition, s = r.shrink, fresh = {
        STALE: 'the rules or the model changed after the run',
        UNREPRODUCED: 'the recorded run could not be replayed step for step',
        CORRUPT: 'the recorded run does not match its own record',
        VERSION: 'the run was recorded in an earlier format',
        ungraded: 'the walk did not finish, so the run was not graded' };
      var out = { freshness: r.freshness === 'current' ? '' : r.freshness + ' — ' + (fresh[r.freshness] || 'not current'),
        head: '', reason: '', mismatch: '', shrink: '', steps: [] };
      if (!d) return out;
      var mm = d.mismatch || {};
      out.head = d.kind + (d.code ? ' ' + d.code : '')
          + (d.at == null ? '' : mm.kind === 'readBackExplainsNoCandidate' ? ' · at the closing read-back' : ' · at step ' + (d.at + 1))
          + ' · against ' + (r.against === 'live' ? 'a live service' : 'a mock')
          + (r.sutRevision ? ' · revision ' + r.sutRevision : '');
      out.reason = d.reason || '';
      if (d.mismatch) {
        out.mismatch = mm.kind === 'refusalReasonMismatch'
            ? 'the service refused — ' + mm.observed + '; the model refuses for ' + (mm.model || []).join(', ')
            : 'the service ' + this.twinMismatchService({ mismatch: mm }) + '; ' + this.twinMismatchModel({ mismatch: mm });
      }
      if (!s) {
        out.shrink = d.kind === 'FAIL' ? 'not shrunk' : '';
      } else if (s.status === 'DAMAGED') {
        out.shrink = 'shrink DAMAGED — the stored result does not match its own hash, so it is not shown';
      } else {
        var parent = { FOREIGN_PARENT: ' · FOREIGN_PARENT — it minimised another run',
          STALE_PARENT: ' · STALE_PARENT — its run is not current' }[s.status] || '';
        out.shrink = (s.verified === 'CONFIRMED'
            ? 'shrunk ' + s.from + ' → ' + s.to + ' steps · CONFIRMED'
            : 'shrink unconfirmed (UNSTABLE) — attempted ' + s.to + ' steps, not a result')
            + (s.budgetExhausted ? ' · budget exhausted' : '') + parent;
        out.steps = s.steps || s.attempted || [];
      }
      return out;
    },
    /** the pane takes no plan (D319): it says so, beside the hash a report under a plan would differ by */
    twinPlanText: function () {
      var e = this.twinExplore(), sha = e && e.plan && e.plan.planSha;
      return sha ? 'walked under the empty plan · plan ' + String(sha).slice(0, 12) : '';
    },
    twinOpenFields: function () { var f = this.twinFidelity(); return (f && f.undeclaredOpenFields) || []; },
    twinUsageText: function () {
      var u = this.twinUsage();
      return u ? (u.nodesVisited + ' nodes · ' + u.runsExecuted + ' runs · ' + u.wallSeconds + 's · depth '
          + u.maxDepthReached) : '';
    },
    /** plain labels, and the gap where the gap is what reads (D254): `observedFalse` short of
     *  `exercised` means the walk never reached a world where that guard was false */
    twinFidelityText: function () {
      var f = this.twinFidelity();
      if (!f) return '';
      return 'guards ' + f.guards.exercised + ' exercised / ' + (f.guards.exercised - f.guards.observedFalse)
          + ' never seen false / ' + f.guards.notChecked + ' threw · invariants ' + f.invariants.ran + ' run / '
          + f.invariants.failed + ' failed · steps ' + f.steps.oracle + ' checked against the rules / '
          + f.steps.bare + ' not checked · clock ' + f.clock;
    },
    twinStatsText: function () {
      var s = this.twinStats();
      return s ? (s.nodes + ' worlds visited · ' + s.edges + ' moves taken · ' + s.refusals + ' refusals · '
          + s.errors + ' errors') : '';
    },
    /** the file versions this replay was read against — labelled, and never needed to read the pane */
    twinProvenanceText: function () {
      var p = this.twinData && this.twinData.provenance;
      if (!p) return '';
      var labels = { twin: 'model version', calc: 'rules version', schema: 'input-shape version',
        generator: 'generator version' };
      return Object.keys(p).map(function (k) {
        return (labels[k] || k) + ' ' + String(p[k]).slice(0, 8);
      }).join(' · ');
    },
    twinOriginLabel: function (kind) {
      var labels = { explore: 'promoted from a walk', authored: 'written by hand', live: 'from a live run' };
      return labels[kind] || kind || 'written by hand';
    },
    /** the verdict sentence: what the model covered, then what the live replay found. The strings below
     *  are duplicated verbatim from static/rule-analysis-report.js — the two renderers cannot share
     *  code, so they share wording: one finding must not meet a reader in two vocabularies. */
    twinHeadline: function () {
      var t = this.twinData || {}, ck = this.twinCheck() || {}, ci = ck.ci || {};
      var f = ck.findings || {}, ev = ck.evidence || {};
      if (t.error) return 'The walk did not finish — the reading below is partial.';
      if (ci.verdict === 'INVALID') return 'The walk did not report everything it must, so nothing here can be graded.';
      var req = (f.requiredNotreached || []).length, drift = (f.sequenceDrift || []).length,
          mism = (f.liveMismatch || []).length, liveOf = (ev.live || {}).of || 0;
      if (!req && !drift && !mism) {
        return liveOf ? 'Healthy — the walk covers the model and every live replay agrees.'
            : 'Healthy — the walk covers the model; nothing has been replayed against a running service yet.';
      }
      var c = this.twinCount;
      var parts = [req === 0 ? 'The model reaches every required row'
          : c(req, 'required row') + ' the walk never reached'];
      if (drift) parts.push(c(drift, 'blessed sequence') + (drift === 1 ? ' has moved' : ' have moved'));
      if (mism) parts.push(this.twinLaneCounts(f.liveMismatch).join(' · ') + (mism === 1 ? ' stands' : ' stand') + ' against the service');
      var s = parts.join(' — ') + '.';
      return s.charAt(0).toUpperCase() + s.slice(1);
    },
    /** liveMismatch carries a pinned sequence's FAIL (no lane) beside the engine-derived lanes (§17.13) */
    twinLaneCounts: function (rows) {
      var c = this.twinCount, noun = { live: 'live disagreement', witness: 'witness refutation', orders: 'failing order' };
      return ['live', 'witness', 'orders'].map(function (lane) {
        var n = rows.filter(function (m) { return (m.lane || 'live') === lane; }).length;
        return n ? c(n, noun[lane]) : null;
      }).filter(Boolean);
    },
    twinCount: function (n, one) { return (n === 1 ? 'one' : n) + ' ' + one + (n === 1 ? '' : 's'); },
    /** one plain sentence per `incomplete` kind (§17.4/§17.5) — a disclosure is never a defect (P29) */
    twinDisclosureText: function (d) {
      var c = this.twinCount;
      switch (d.kind) {
        case 'ceiling':
          return 'The walk stopped at a limit (' + d.hit + '), so it did not try every order the model allows.';
        case 'frontier':
          return d.frontier + ' orders were still unexplored when the walk stopped.';
        case 'guardNotChecked':
          return 'The rule deciding whether ' + d.command + ' is allowed threw an error, so it was never graded.';
        case 'undeclaredOpenField':
          return 'The field ' + d.field + ' took ' + c(d.distinct, 'value') + ' the model does not declare.';
        case 'neverBlessed':
          return 'Nothing has been blessed yet, so there is no baseline to compare this run against.';
        case 'twinEdited':
          return 'The model was edited after its last bless — review the edit and bless again.';
        case 'adjudicatedModel':
          return 'Sequence ' + d.sequenceId + ': the live disagreement was reviewed and blamed on the model, '
              + 'so it does not fail the build.';
        case 'liveUngraded':
          return 'Sequence ' + d.sequenceId + ': ' + this.twinUngradedReason(d) + '.';
        case 'derivedUngraded':
          return (d.id == null ? 'The ' + d.lane + ' record' : this.twinMismatchWho(d)) + ': ' + this.twinDerivedReason(d) + '.';
        default:
          return String(d.kind);
      }
    },
    /** the reasons Twin.check folds a live record under (§17.5) — the served live topic is their source */
    twinUngradedReason: function (d) {
      switch (d.reason) {
        case 'VERSION': return 'the recorded run uses an earlier record format, so it was not counted — re-run the live replay';
        case 'STALE': return 'the rules or the model changed after the run, so it was not counted';
        case 'UNREPRODUCED': return 'the recorded run could not be replayed step for step, so it was not counted';
        case 'CORRUPT': return 'the recorded run does not match its own record, so it was not counted';
        case 'UNRESOLVED': return 'the replay left more than one possible state, so it decided nothing';
        case 'NOTCHECKED': return 'the model errored during the replay, so it proves nothing';
        case 'REFUSED': return 'the replay was refused before it could finish' + (d.code ? ' (' + d.code + ')' : '');
        case 'INVALID': return 'the service interaction could not be graded — the starting state was not reproduced, or a lost response had no declared meaning';
        default: return 'the replay ended ' + d.reason + ', so it was not counted';
      }
    },
    /** the reasons Twin.check leaves a witness or orders row ungraded (§17.13) */
    twinDerivedReason: function (d) {
      switch (d.reason) {
        case 'VERSION': return 'it was recorded in an earlier format, so it was not counted — re-run the lane';
        case 'STALE': return 'the rules, the model or the plan changed after the run, so it was not counted';
        case 'UNCONFIRMED': return 'the contradiction did not repeat on its confirming replay, so it was not counted';
        case 'NOREVISION': return 'the run or this check names no service revision, so it was not counted';
        case 'REVISION': return 'the run was taken against another service revision, so it was not counted';
        default: return this.twinUngradedReason(d);
      }
    },
    /** the raw fields stay available, compactly and second — never the primary rendering */
    twinDisclosureDetail: function (d) {
      var parts = [];
      Object.keys(d).forEach(function (k) { if (k !== 'kind') parts.push(k + ' ' + JSON.stringify(d[k])); });
      return d.kind + (parts.length ? ' · ' + parts.join(' · ') : '');
    },
    /** a live disagreement read as columns: where, what the service did, what the model allows */
    twinMismatchStep: function (m) {
      return (m.mismatch || {}).kind === 'readBackExplainsNoCandidate'
          ? 'the closing read-back' : 'step ' + ((m.at || 0) + 1);
    },
    twinMismatchService: function (m) {
      var mm = m.mismatch || {};
      if (mm.kind === 'readBackExplainsNoCandidate') {
        return 'read back a state the model does not allow'
            + (mm.missing == null ? '' : ' (' + JSON.stringify(mm.missing) + ')');
      }
      if (mm.kind === 'refusalReasonMismatch') return 'refused the step — ' + mm.observed;
      return (mm.outcome === 'refused' ? 'refused the step'
          : mm.outcome === 'applied' ? 'applied the step' : 'answered ' + mm.outcome)
          + (mm.reason == null ? '' : ' — ' + mm.reason);
    },
    twinMismatchModel: function (m) {
      var mm = m.mismatch || {};
      if (mm.kind === 'refusalReasonMismatch') return 'refuses for ' + (mm.model || []).join(', ');
      return mm.kind === 'readBackExplainsNoCandidate'
          ? 'no state the model still allowed survives that read-back'
          : 'no state the model still allowed explains that answer';
    },
    /** who disagreed: a pinned sequence, a witness obligation (its key) or a failing order (its id) */
    twinMismatchWho: function (m) {
      return m.lane === 'witness' ? 'Witness obligation ' + m.id
          : m.lane === 'orders' ? 'Order ' + m.id : 'Sequence ' + m.sequenceId;
    },
    twinMismatchAgainst: function (m) { return m.against === 'live' ? 'a live service' : 'a mock'; },
    twinExpectText: function (run) {
      var e = run && run.expect;
      if (!e) return '';
      return e.checked ? ('expect checked · ' + (e.mismatches || []).length + ' mismatches') : 'expect not checked';
    },
    twinMismatches: function (run) { return (run && run.expect && run.expect.mismatches) || []; },
    /** the replay's two disclosures (§17.1) — never refusals, so they read as lines, not errors */
    twinDisclosures: function (run) {
      var out = [];
      if (run && run.provenance && run.provenance.differs) out.push('provenance differs');
      var o = run && run.origin;
      if (o && o.checked === false) out.push('origin not checked · ' + (o.reason || ''));
      else if (o && o.differs) out.push('origin differs' + (o.found === false ? ' · counterexample not found' : ''));
      return out;
    },
    twinVerdictCls: function (v) {
      if (v === 'PASS') return 'text-emerald-600 dark:text-emerald-400 border-emerald-200 dark:border-emerald-900/50 bg-emerald-50/60 dark:bg-emerald-950/20';
      if (v === 'FAIL') return 'text-rose-600 dark:text-rose-400 border-rose-200 dark:border-rose-900/50 bg-rose-50/60 dark:bg-rose-950/20';
      return 'text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-900/50 bg-amber-50/60 dark:bg-amber-950/20';
    },
    /** the four grades side by side — never summed, never a percentage across them (D270 / D194) */
    twinEvidence: function () {
      var ev = (this.twinCheck() || {}).evidence || {};
      return ['model', 'oracle', 'mock', 'live'].map(function (g) {
        var p = ev[g] || {};
        return { grade: g, count: p.count || 0, of: p.of || 0, note: g === 'oracle' ? 'per step' : '' };
      });
    },
    /** the exact promote call, shown as text — promotion is a write verb with a walk's cost (§17.3) */
    twinPromoteCall: function (cx) {
      return "Rule.sequence.create('" + this.rbName + "', {promote: {plan: {}, counterexample: '" + cx.id + "'}})";
    },
    // ── Play records (playground.md §6 (1) — confirm a quarantined record, pick its criteria) ──
    loadPlays: async function () {
      var j = await ui('GET', '/rulebooks/' + this.rbName + '/play');
      if (isRouteFailure(j)) { this.playData = null; return this.handleError(j); }
      var drafts = {};
      (j.records || []).forEach(function (r) { if (!r.error) drafts[r.id] = playDraft(r); });
      this.playDrafts = drafts;
      this.playData = j;
      var q = typeof location === 'undefined' ? null : /[?&]play=([^&]*)/.exec(location.search);
      this.playSelect(q ? decodeURIComponent(q[1]) : null);
    },
    /** ?play=<id> (playground.md §5 — the app's timeline link): expand and focus that record once, or
     *  say it is missing. */
    playSelect: function (id) {
      if (!id || this.playSelected === id) return;
      this.playSelected = id;
      var found = this.playRecords().some(function (r) { return r.id === id; });
      if (!found) { this.playMissing = id; return; }
      var open = Object.assign({}, this.playOpen);
      open[id] = true;
      this.playOpen = open;
      this.$nextTick(function () {
        var el = document.getElementById('play-record-' + id);
        if (el) { el.scrollIntoView({ block: 'center' }); el.focus(); }
      });
    },
    playRecords: function () { return (this.playData && this.playData.records) || []; },
    playToggle: function (id) {
      var next = Object.assign({}, this.playOpen);
      next[id] = !next[id];
      this.playOpen = next;
    },
    playReplayStep: function (r, at) {
      return (r.replay && r.replay.steps && r.replay.steps[at]) || null;
    },
    /** a replay runs from t.init, so its world carries the model's own identity, never the one the
     *  mock allocated (playground.md §2 law 4) — named beside the step whenever the two differ */
    playIdentityNote: function (r, at) {
      var s = this.playReplayStep(r, at), f = r.identityField;
      if (!s || !f || !s.world || s.world[f] == null || String(s.world[f]) === String(r.identity)) return '';
      return 'model ' + s.world[f] + ' ≙ wire ' + r.identity;
    },
    playFinal: function (r) { return (r.replay && r.replay['final']) || {}; },
    playConfirmText: function (r) {
      var c = r.confirmation;
      if (!c) return 'not confirmed';
      return c.state === 'current' ? 'confirmed · ' + (c.req.length ? c.req.join(', ') : 'unlinked')
          : 'confirmation invalid — ' + c.message;
    },
    /** the draft as `Expect`'s one shape — sparse: only the checked steps and final keys */
    playExpect: function (r) {
      var d = this.playDrafts[r.id], out = {}, steps = [], fin = {}, world = this.playFinal(r);
      r.steps.forEach(function (s) {
        var e = d.steps[s.at];
        if (!e.on) return;
        var row = { at: s.at, outcome: e.outcome };
        if (e.outcome !== 'applied' && e.stage) row.stage = e.stage;
        if (e.outcome !== 'applied' && e.reason) row.reason = e.reason;
        steps.push(row);
      });
      Object.keys(d['final']).forEach(function (k) { if (d['final'][k]) fin[k] = world[k]; });
      if (steps.length) out.steps = steps;
      if (Object.keys(fin).length) out['final'] = fin;
      return out;
    },
    playPicked: function (r) {
      var req = this.playDrafts[r.id].req;
      return Object.keys(req).filter(function (k) { return req[k]; });
    },
    playPromoteCall: function (r) {
      return "Rule.sequence.create('" + this.rbName + "', {promote: {play: '" + r.id + "'}})";
    },
    playConfirm: async function (r) {
      if (this.playSaving[r.id]) return;
      var start = Object.assign({}, this.playSaving);
      start[r.id] = true;
      this.playSaving = start;
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/play/' + r.id + '/confirm',
          { expect: this.playExpect(r), req: this.playPicked(r) });
      var end = Object.assign({}, this.playSaving);
      delete end[r.id];
      this.playSaving = end;
      if (j && j.error) return this.handleError(j);
      this.loadPlays();
    },
    twinRunSequence: async function (id) {
      if (this.twinRunning[id]) return;
      var start = Object.assign({}, this.twinRunning);
      start[id] = true;
      this.twinRunning = start;
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/twin', { sequenceId: id });
      var end = Object.assign({}, this.twinRunning);
      delete end[id];
      this.twinRunning = end;
      if (j && j.error) return this.handleError(j);
      var next = Object.assign({}, this.twinRuns);
      next[id] = j;
      this.twinRuns = next;
    },

    // ── Test Data Design matrix (D114 — Rule.table over /ui; the friendly decision table) ──
    loadTable: async function () {
      this.tableLoading = true;
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/table', { suite: this.tableSuite });
      this.tableLoading = false;
      if (j && j.error) { this.tableData = null; return this.handleError(j); }
      this.tableData = j;
    },
    setTableSuite: function (s) {
      this.onlyExisting = false;          // a generation strategy → show saved + candidates again
      if (this.tableSuite === s) return;
      this.tableSuite = s;
      this.loadTable();
    },
    /** "Existing" — show ONLY the saved scenarios (no generated candidates). Client-only filter over
        the loaded tableData (coverage % still reflects what the saved scenarios cover). */
    showExisting: function () {
      this.onlyExisting = true;
      if (!this.tableData) this.loadTable();
    },
    /** "27/58 cells exercised · 1 masked · 30 unread" — '' for a book without file-table cells */
    cellsLine: function () {
      var c = this.cells;
      if (!c || c.error || !c.of) return '';
      var parts = [c.exercised + '/' + c.of + ' cells exercised'];
      if (c.masked) parts.push(c.masked + ' masked');
      if (c.notChecked) parts.push(c.notChecked + ' not checked');
      if (c.unread) parts.push(c.unread + ' unread');
      return parts.join(' · ');
    },
    tableVerdict: function () {
      var c = this.tableData && this.tableData.coverage;
      if (!c) return '';
      var gaps = (this.tableData.notcovered || []).length;
      return c.covered + ' of ' + c.total + ' rule outcomes covered' + (gaps ? ' · ' + gaps + ' never fire' : '');
    },
    /** disambiguate display labels: a base label shared by >1 item gets the qualified form (so the
        four distinct `…liability.limit` / `…medicalPayments.limit` paths don't all read as "limit"). */
    _disambiguate: function (items, keyField, base, qualify) {
      var counts = {};
      items.forEach(function (it) { var b = base(it); counts[b] = (counts[b] || 0) + 1; });
      var map = {};
      items.forEach(function (it) { var b = base(it); map[it[keyField]] = counts[b] > 1 ? qualify(it) : b; });
      return map;
    },
    /** input-column labels, last-segment but parent-qualified on collision (path-aware) */
    inputLabelMap: function (cols) {
      return this._disambiguate(cols, 'path', function (c) { return c.label; }, function (c) {
        var p = (c.path || '').split('.');
        return p.length > 1 ? p.slice(-2).join('.') : c.label;
      });
    },
    /** decision-column labels, <name>:L<line> on collision (the same condition fires at N sites) */
    decisionLabelMap: function (cols) {
      return this._disambiguate(cols, 'id', function (c) { return c.label; }, function (c) {
        return c.label + ':L' + c.line;
      });
    },
    /** the disambiguated (parent-qualified on collision) label for one input path — used by the
        Fields ▾ picker so it reads the same as the matrix (liability.limit, not four bare "limit"s) */
    inputColLabel: function (path) {
      var cols = (this.tableData && this.tableData.columns && this.tableData.columns.inputs) || [];
      return this.inputLabelMap(cols)[path] || path;
    },
    /** the input columns the matrix shows — filtered by the Fields ▾ picker (absent = shown) */
    visibleInputCols: function () {
      var sel = this.fieldSel;
      var cols = (this.tableData && this.tableData.columns && this.tableData.columns.inputs) || [];
      return cols.filter(function (c) { return sel[c.path] !== false; });
    },
    /** the explorer fills non-varied / unspecified string dims with the placeholder "x" (for both
        generated candidates AND saved scenarios projected onto the schema). It's never real data, so
        render it as a muted "any" (any value works) rather than a literal "x" that reads as garbage. */
    matrixCell: function (r, c) {
      var v = r.inputs[c.path];
      if (v === 'x') {
        return '<span class="text-slate-300 dark:text-slate-500 italic" title="not specified — any value">any</span>';
      }
      return window.JsonTable.cell(v, c.label);
    },
    // ── Fields ▾ picker (matrix) ──
    fieldToggle: function (path) {
      var next = Object.assign({}, this.fieldSel);
      next[path] = next[path] === false ? true : false;
      this.fieldSel = next;
    },
    fieldShown: function (path) { return this.fieldSel[path] !== false; },
    fieldsAll: function () { this.fieldSel = {}; },
    fieldsNone: function () {
      var next = {};
      ((this.tableData && this.tableData.columns && this.tableData.columns.inputs) || []).forEach(function (c) { next[c.path] = false; });
      this.fieldSel = next;
    },
    /** show only fields whose value actually VARIES across the visible scenarios (kills the null noise) */
    fieldsOnlyVarying: function () {
      var rows = this.visibleRows();
      var next = {};
      ((this.tableData && this.tableData.columns && this.tableData.columns.inputs) || []).forEach(function (c) {
        var seen = null, varies = false;
        for (var i = 0; i < rows.length; i++) {
          var v = JSON.stringify(rows[i].inputs[c.path]);
          if (i === 0) seen = v; else if (v !== seen) { varies = true; break; }
        }
        next[c.path] = varies;
      });
      this.fieldSel = next;
    },
    /** rows for display: optionally hide the stored scenarios to focus on new candidates */
    visibleRows: function () {
      var t = this.tableData;
      if (!t || !t.rows) return [];
      var out = [];
      for (var i = 0; i < t.rows.length; i++) {
        var r = t.rows[i];
        if (this.onlyExisting && r.provenance !== 'scenario') continue;   // "Existing" — saved only
        if (this.tableHideScenarios && r.provenance === 'scenario') continue;
        r._idx = i;                                    // map a promote click back to the source row
        out.push(r);
      }
      return out;
    },
    /** the matrix as an HTML string (x-html). Scenarios-as-ROWS (the non-transposed orientation). Outputs
        are NOT shown inline (they widen the grid) — clicking a row's Scenario cell (data-scenario) selects it
        and the workbench split renders its audit+output. Promote buttons carry data-promote. Both route
        through matrixClick (event delegation). */
    renderMatrix: function () {
      var t = this.tableData;
      if (!t || t.error || !t.columns) return '';
      var E = window.JsonTable.esc;
      var self = this;
      var cols = t.columns;
      var inputCols = this.visibleInputCols();
      var decCols = this.showDecisions ? cols.decisions : [];
      var inLbl = this.inputLabelMap(cols.inputs);
      var decLbl = this.decisionLabelMap(cols.decisions);
      var SHORT = function (s) { return s && s.length > 22 ? s.slice(0, 22) + '…' : s; };
      var rows = this.visibleRows();
      var sel = this.selectedScenarioIdx;
      var TH = 'px-2 py-1 border-b border-slate-200 dark:border-slate-700 font-semibold whitespace-nowrap';
      var TD = 'px-2 py-1 border-b border-slate-100 dark:border-slate-800 align-top';
      var STICKY = 'sticky left-0 z-10 bg-white dark:bg-slate-800 overflow-hidden';
      var FW = ' style="width:var(--mcw);min-width:var(--mcw);max-width:var(--mcw)"';
      var GRIP = '<span data-colresize class="absolute top-0 right-0 h-full w-1.5 cursor-col-resize hover:bg-indigo-300 dark:hover:bg-indigo-600" title="Drag to resize"></span>';
      var h = '<table class="text-xs border-collapse w-max">';
      h += '<thead><tr class="text-left">';
      h += '<th class="' + STICKY + ' ' + TH + ' text-slate-600 dark:text-slate-300 relative"' + FW + '>Scenario' + GRIP + '</th>';
      inputCols.forEach(function (c) {
        h += '<th class="' + TH + ' text-slate-500 dark:text-slate-400" title="' + E(c.path) + '">' + E(inLbl[c.path]) + '</th>';
      });
      decCols.forEach(function (c) {
        h += '<th class="' + TH + ' text-indigo-600 dark:text-indigo-400" title="' + E(decLbl[c.id]) + ' · ' + E(c.label) + '">'
          + E(SHORT(decLbl[c.id])) + '</th>';
      });
      h += '<th class="' + TH + '"></th></tr></thead><tbody>';
      rows.forEach(function (r) {
        var on = sel === r._idx;
        h += '<tr class="' + (on ? 'bg-indigo-50 dark:bg-indigo-950/40' : 'hover:bg-slate-50/70 dark:hover:bg-slate-900/40') + '">';
        var badge = r.provenance === 'scenario'
          ? '<span class="text-indigo-500" title="stored scenario">●</span>'
          : '<span class="text-slate-400" title="generated candidate">✦</span>';
        var flag = '';
        if (r.provenance === 'generated') {
          flag = r.addsCoverage
            ? ' <span class="text-emerald-600 dark:text-emerald-400">＋ new path</span>'
            : ' <span class="text-slate-400 dark:text-slate-500">no new coverage</span>';
        }
        var err = r.error ? ' <span class="text-rose-500" title="' + E(r.error) + '">⚠</span>' : '';
        h += '<td data-scenario="' + r._idx + '" class="' + STICKY + ' ' + TD + ' whitespace-nowrap cursor-pointer ' + (on ? 'bg-indigo-50 dark:bg-indigo-950/40' : 'bg-white dark:bg-slate-800') + '"' + FW + ' title="Show this scenario\'s audit + output">' + badge + ' '
          + '<span class="text-slate-700 dark:text-slate-200">' + E(r.name) + '</span>' + err + flag + GRIP + '</td>';
        inputCols.forEach(function (c) { h += '<td class="' + TD + '">' + self.matrixCell(r, c) + '</td>'; });
        decCols.forEach(function (c) {
          var g = self.decGlyph(r.decisions[c.id]);
          h += '<td class="' + TD + ' text-center"><span class="' + g.cls + '">' + g.text + '</span></td>';
        });
        var promote = (r.provenance === 'generated' && r.addsCoverage && !r.error)
          ? '<button data-promote="' + r._idx + '" class="px-1.5 py-0.5 rounded border border-indigo-200 dark:border-indigo-800 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950 whitespace-nowrap" title="Save this candidate as a scenario">＋ scenario</button>'
          : '';
        h += '<td class="' + TD + '">' + promote + '</td></tr>';
      });
      return h + '</tbody></table>';
    },
    /** a decision outcome cell (T, F, both, or not-reached) → a coloured Y/N/Y·N/– glyph */
    decGlyph: function (cell) {
      switch (cell) {
        case 'T': return { text: 'Y', cls: 'text-emerald-600 dark:text-emerald-400 font-medium' };
        case 'F': return { text: 'N', cls: 'text-slate-500 dark:text-slate-400' };
        case '*': return { text: 'Y·N', cls: 'text-amber-600 dark:text-amber-400' };
        default: return { text: '–', cls: 'text-slate-300 dark:text-slate-600' };
      }
    },
    matrixClick: function (e) {
      if (e.target.closest('[data-colresize]')) return;   // a grip click is a resize, not a select
      var b = e.target.closest('[data-promote]');
      if (b) {
        var i = parseInt(b.getAttribute('data-promote'), 10);
        var r = this.tableData && this.tableData.rows[i];
        if (r) this.promoteRow(r);
        return;
      }
      // a Scenario cell/header (data-scenario) selects the scenario → its audit+output in the workbench split
      var c = e.target.closest('[data-scenario]');
      if (c) this.selectScenario(parseInt(c.getAttribute('data-scenario'), 10));
    },
    /** promote a generated candidate into a real (editable, renameable) scenario (the human ＋save —
        does NOT mark @ai; the LLM path is Rule.scenario.create over MCP, D81/P25) */
    promoteRow: async function (r) {
      var input = JSON.parse(JSON.stringify(r.input || {}));
      input._label = r.name;
      await this.postScenario(input);    // refreshes rb (which reloads the matrix) + selects it
    },
    /** "→ Input" (the mini Scenario-Coverage slot, docked) — load a candidate's full input into the
        SELECTED scenario's edit draft (the SchemaForm-bound state Run flushes) so you can try it →
        Run → read Result with zero context switch. Distinct from promoteRow (which mints a scenario). */
    tryCandidate: function (r) {
      var s = this.selectedScenario;
      if (!s) {
        // nothing selected (fresh land / after Clear) → there's no draft to load into, so materialize
        // the candidate as a new scenario (named by its friendly name) + select it, ready to edit/Run.
        var row = JSON.parse(JSON.stringify(r.input || {}));
        delete row._id; row._label = r.name;
        this.postScenario(row);   // creates + selects
        this.saveMessage = 'created “' + (r.name || 'candidate') + '” — edit or ▶ Run it';
        this.saveError = false;
        var sf = this;
        setTimeout(function () { if (sf.saveMessage.indexOf('created ') === 0) sf.saveMessage = ''; }, 3500);
        return;
      }
      var input = JSON.parse(JSON.stringify(r.input || {}));
      delete input._id; delete input._label;             // the draft.edit excludes the engine-owned keys
      this._writeDraft(s._id, function (d) { d.edit = input; });
      this.saveMessage = 'loaded “' + (r.name || 'candidate') + '” into ' + (s._label || s._id) + ' — ▶ Run to try it';
      this.saveError = false;
      var self = this;
      setTimeout(function () { if (self.saveMessage.indexOf('loaded ') === 0) self.saveMessage = ''; }, 3500);
    },
    /** the mini-slot provenance badge (● saved · ◐ᴬᴵ saved-but-@ai-pending · ✦ generated) */
    rowBadge: function (r) {
      if (r.provenance === 'generated') return { glyph: '✦', cls: 'text-slate-400', title: 'generated candidate' };
      if (r.ai) return { glyph: '◐ᴬᴵ', cls: 'text-amber-500', title: 'saved — AI-authored, review pending' };
      return { glyph: '●', cls: 'text-indigo-500', title: 'saved scenario' };
    },
    openTableFull: function () { this.tableFull = true; if (!this.tableData) this.loadTable(); },
    closeTableFull: function () { this.tableFull = false; this.selectedScenarioIdx = null; this.scenarioRun = null; },
    /** click a scenario (a column when transposed, a row otherwise) → run it (Rule.execute, ad-hoc input
        — works for saved AND generated rows) and show its audit+output in the workbench split. */
    selectScenario: async function (idx) {
      var r = this.tableData && this.tableData.rows[idx];
      if (!r) return;
      this.selectedScenarioIdx = idx;
      this.scenarioAuditCollapsed = {};
      this.scenarioRunLoading = true;
      this.scenarioRun = null;
      // run with the friendly NAME as the label — a generated candidate's input._label is the explorer
      // placeholder "x", so the audit's leading calc.log(policy._label) would read "x" otherwise.
      var input = Object.assign({}, r.input, { _label: r.name });
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/execute', { input: input });
      this.scenarioRunLoading = false;
      this.scenarioRun = (j && j.error) ? { error: j.error, code: j.code } : j;
    },
    get selectedScenarioRow() {
      return (this.selectedScenarioIdx != null && this.tableData) ? this.tableData.rows[this.selectedScenarioIdx] : null;
    },
    closeScenarioWorkbench: function () { this.selectedScenarioIdx = null; this.scenarioRun = null; },
    /** Save the current (dirty) input draft as a NEW named scenario — distinct from "Save Inputs"
        (which overwrites the selected scenario). Inline name prompt; defaults to the current label. */
    startSaveAsNew: function () {
      var s = this.selectedScenario;
      this.newScenarioName = ((s && (this.editLabel || s._label)) || 'New scenario') + ' (copy)';
      this.savingAsNew = true;
    },
    cancelSaveAsNew: function () { this.savingAsNew = false; this.newScenarioName = ''; },
    confirmSaveAsNew: async function () {
      var name = (this.newScenarioName || '').trim();
      if (!name) return;
      var row = JSON.parse(JSON.stringify(this.edit || {}));
      delete row._id; delete row._label;
      row._label = name;
      this.savingAsNew = false;
      this.newScenarioName = '';
      await this.postScenario(row);   // creates + selects the new scenario (its draft becomes clean)
    },
    toggleCoverageTransposed: function () {
      this.coverageTransposed = !this.coverageTransposed;
      kaPrefs.set('rb.coverageTransposed', this.coverageTransposed);
    },
    /** the ⤢ Scenario Coverage grid — orientation per coverageTransposed (default transposed: the BA's
        native decision-table shape, fields=rows + scenarios = cols). Same tableData, a render flag only. */
    renderCoverageGrid: function () {
      return this.coverageTransposed ? this.renderMatrixT() : this.renderMatrix();
    },
    /** export the matrix to CSV — WYSIWYG (the visible scenarios + Fields-picker inputs + decisions when
        on) + the key outputs; scenarios as rows. Downloads <rulebook>-coverage.csv. */
    exportCsv: function () {
      var t = this.tableData;
      if (!t || t.error || !t.columns) return;
      var self = this;
      var rows = this.visibleRows();
      var inputCols = this.visibleInputCols();
      var decCols = this.showDecisions ? t.columns.decisions : [];
      var outCols = t.columns.outputs || [];
      var inLbl = this.inputLabelMap(t.columns.inputs);
      var decLbl = this.decisionLabelMap(t.columns.decisions);
      var cell = function (v) {
        var s = v == null ? '' : (typeof v === 'object' ? JSON.stringify(v) : String(v));
        return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
      };
      var header = ['Scenario', 'Type']
        .concat(inputCols.map(function (c) { return inLbl[c.path]; }),
                decCols.map(function (c) { return decLbl[c.id]; }),
                outCols.map(function (k) { return 'out: ' + k; }));
      var lines = [header.map(cell).join(',')];
      rows.forEach(function (r) {
        var line = [r.name, r.provenance];
        inputCols.forEach(function (c) { line.push(r.inputs[c.path]); });
        decCols.forEach(function (c) { line.push(self.decGlyph(r.decisions[c.id]).text); });
        outCols.forEach(function (k) { line.push(r.outputs ? r.outputs[k] : null); });
        lines.push(line.map(cell).join(','));
      });
      // lead with a UTF-8 BOM so Excel autodetects the encoding (the "–" / "·" glyphs otherwise
      // mis-decode as MacRoman → ",Äì"). Other tools ignore the BOM.
      var blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url; a.download = this.rbName + '-coverage.csv';
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);
    },
    /** TRANSPOSED matrix (D114 v4 default): rows = the field spine (Inputs → Decisions),
        columns = scenarios. Sticky first column + header row; a never-firing decision row tints rose.
        Outputs are NOT shown inline (they widen every column) — clicking a scenario header (data-scenario)
        selects it and the workbench split renders its audit+output. A trailing actions row carries the
        ＋ promote buttons (data-promote). Both route through matrixClick. */
    renderMatrixT: function () {
      var t = this.tableData;
      if (!t || t.error || !t.columns) return '';
      var E = window.JsonTable.esc;
      var self = this;
      var cols = t.columns;
      var inputCols = this.visibleInputCols();
      var decCols = this.showDecisions ? cols.decisions : [];
      var inLbl = this.inputLabelMap(cols.inputs);
      var decLbl = this.decisionLabelMap(cols.decisions);
      var rows = this.visibleRows();
      var sel = this.selectedScenarioIdx;
      var TH = 'px-2 py-1 border-b border-slate-200 dark:border-slate-700 font-semibold whitespace-nowrap';
      var TD = 'px-2 py-1 border-b border-slate-100 dark:border-slate-800 align-top';
      var STICKY = 'sticky left-0 z-10 bg-white dark:bg-slate-800 overflow-hidden';
      var FW = ' style="width:var(--mcw);min-width:var(--mcw);max-width:var(--mcw)"';
      var GRIP = '<span data-colresize class="absolute top-0 right-0 h-full w-1.5 cursor-col-resize hover:bg-indigo-300 dark:hover:bg-indigo-600" title="Drag to resize"></span>';
      var SHORT = function (s) { return s && s.length > 22 ? s.slice(0, 22) + '…' : s; };
      var colCls = function (r) { return sel === r._idx ? ' bg-indigo-50 dark:bg-indigo-950/40' : ''; };
      var h = '<table class="text-xs border-collapse w-max">';
      // header row = the scenario ids (clickable → select the scenario for the workbench)
      h += '<thead><tr class="text-left">';
      h += '<th class="' + STICKY + ' ' + TH + ' text-slate-600 dark:text-slate-300 relative"' + FW + '>Field' + GRIP + '</th>';
      rows.forEach(function (r) {
        var b = self.rowBadge(r);
        var err = r.error ? ' <span class="text-rose-500" title="' + E(r.error) + '">⚠</span>' : '';
        h += '<th data-scenario="' + r._idx + '" class="' + TH + ' text-slate-700 dark:text-slate-200 cursor-pointer' + colCls(r) + '" title="' + E(r.name) + ' — click for its audit + output"><span class="' + b.cls + '">'
          + b.glyph + '</span> ' + E(SHORT(r.name)) + err + '</th>';
      });
      h += '</tr></thead><tbody>';
      // INPUT rows
      inputCols.forEach(function (c) {
        h += '<tr class="hover:bg-slate-50/70 dark:hover:bg-slate-900/40">';
        h += '<td class="' + STICKY + ' ' + TD + ' text-slate-500 dark:text-slate-400 whitespace-nowrap"' + FW + ' title="' + E(c.path) + '">' + E(inLbl[c.path]) + GRIP + '</td>';
        rows.forEach(function (r) { h += '<td class="' + TD + colCls(r) + '">' + self.matrixCell(r, c) + '</td>'; });
        h += '</tr>';
      });
      // DECISION rows — a row never fired by any scenario tints rose (the diagram coverage vocabulary)
      decCols.forEach(function (c) {
        var everFires = rows.some(function (r) { var d = r.decisions[c.id]; return d === 'T' || d === 'F' || d === '*'; });
        h += '<tr class="' + (everFires ? '' : 'bg-rose-50/40 dark:bg-rose-950/20') + '">';
        h += '<td class="' + STICKY + ' ' + TD + ' text-indigo-600 dark:text-indigo-400 whitespace-nowrap"' + FW + ' title="' + E(decLbl[c.id]) + ' · ' + E(c.label) + '">' + E(SHORT(decLbl[c.id])) + GRIP + '</td>';
        rows.forEach(function (r) {
          var g = self.decGlyph(r.decisions[c.id]);
          h += '<td class="' + TD + ' text-center' + colCls(r) + '"><span class="' + g.cls + '">' + g.text + '</span></td>';
        });
        h += '</tr>';
      });
      // actions row — ＋ promote for path-adding generated candidates
      var anyPromotable = rows.some(function (r) { return r.provenance === 'generated' && r.addsCoverage && !r.error; });
      if (anyPromotable) {
        h += '<tr><td class="' + STICKY + ' ' + TD + ' text-slate-400 dark:text-slate-500"' + FW + '>' + GRIP + '</td>';
        rows.forEach(function (r) {
          var promote = (r.provenance === 'generated' && r.addsCoverage && !r.error)
            ? '<button data-promote="' + r._idx + '" class="px-1.5 py-0.5 rounded border border-indigo-200 dark:border-indigo-800 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950 whitespace-nowrap" title="Save this candidate as a scenario">＋</button>'
            : '';
          h += '<td class="' + TD + ' text-center' + colCls(r) + '">' + promote + '</td>';
        });
        h += '</tr>';
      }
      return h + '</tbody></table>';
    },

    /** Result-column show/hide (Audit ⇄ Output) — the pair keeps ≥1 visible */
    toggleCol: function (col) {
      var partner = col === 'audit' ? 'output' : 'audit';
      if (this.cols[col] && !this.cols[partner]) return;   // never hide both
      var next = Object.assign({}, this.cols);
      next[col] = !next[col];
      this.cols = next;
    },
    /** the CENTER Result column's two stacked surfaces (Audit over Output) — split when both
        visible, full height when alone (D113-a). 'audit' takes the remainder, 'output' the fixed % */
    resultRowStyle: function (which) {
      if (!this.cols[which]) return 'display:none';
      var other = which === 'audit' ? 'output' : 'audit';
      if (!this.cols[other]) return 'flex: 1 1 100%';
      return which === 'audit'
          ? 'flex: 1 1 ' + (100 - this.outputSplit) + '%'
          : 'flex: 0 0 ' + this.outputSplit + '%';
    },

    // ── Diagram (SSR'd + sha-keyed — D111e; refreshRule refetches when calc.js changed) ──
    syncDiagram: async function () {
      var digest = this.rb.digest && this.rb.digest.sha256;
      if (digest !== this.exploreDigest) {
        this.exploreDigest = digest;
        this.exploreReport = null;
        this.explorePanel = false;
      }
      var sha = this.rb.mtimes && this.rb.mtimes.calc;
      if (sha === this.diagramSha && (this.diagramSvg || this.diagramError)) return;
      this.diagramSha = sha;
      // a fresh calc resets the collapse state (ids may have shifted)
      this.expandSet = [];
      this.expandAll = false;
      // POST (not the GET light path) so the current compact/All-steps choice is honored
      await this.refetchDiagram();
    },
    applyDiagram: function (j) {
      if (j && j.error) {
        this.diagramError = (j.code || '') + ': ' + j.error;
        this.diagramSvg = '';
        this.diagramMermaid = '';
        this.diagramOutline = [];
        this.diagramCollapsed = [];
        this.diagramCollapsibleAll = [];
      } else {
        this.diagramError = '';
        this.diagramSvg = j.svg || '';
        this.diagramMermaid = j.mermaid || '';
        this.diagramOutline = j.outline || [];
        this.diagramCollapsed = j.collapsed || [];
        this.diagramCollapsibleAll = j.collapsibleAll || [];
      }
      var self = this;
      this.$nextTick(function () { self.decorateDiagram(); });
    },
    /** refetch the server-rendered diagram with the current expand + compact state (D111-b/D114) */
    refetchDiagram: async function () {
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/diagram',
          { expand: this.expandAll ? 'all' : this.expandSet, compact: this.compact });
      this.applyDiagram(j);
    },
    /** D114 — flip between the compact "Business" flowchart and the "All steps" box-per-statement
        view; the level changes the layout, so the expand state resets to the folded default */
    toggleCompact: function () {
      this.compact = !this.compact;
      kaPrefs.set('rb.compact', this.compact);
      this.expandSet = [];
      this.expandAll = false;
      this.refetchDiagram();
    },
    expandNode: function (id) {
      if (this.expandSet.indexOf(id) === -1) this.expandSet.push(id);
      this.refetchDiagram();
    },
    /** collapse one expanded container back to its folded summary (D114 collapse-back widget).
        from "expand all" we drop to an explicit set of everything-else (needs collapsibleAll) */
    collapseNode: function (id) {
      if (this.expandAll) {
        this.expandAll = false;
        this.expandSet = this.diagramCollapsibleAll.filter(function (x) { return x !== id; });
      } else {
        this.expandSet = this.expandSet.filter(function (x) { return x !== id; });
      }
      this.refetchDiagram();
    },
    toggleExpandAll: function () {
      // '⊞ Expand all' when anything is folded; '⊟ Collapse' resets to the folded default
      var anythingFolded = this.diagramCollapsed.length > 0;
      this.expandAll = anythingFolded;
      this.expandSet = [];
      this.refetchDiagram();
    },
    /** outline depth = the parent-chain length (indents the jump list) */
    outlineDepth: function (entry) {
      var byId = {};
      for (var i = 0; i < this.diagramOutline.length; i++) byId[this.diagramOutline[i].id] = this.diagramOutline[i];
      var depth = 0;
      var p = entry.parent;
      while (p && byId[p] && depth < 8) { depth++; p = byId[p].parent; }
      return depth;
    },
    /** outline click: expand the folded ancestor chain (and the target container itself),
        then scroll to the node and flash it */
    jumpTo: async function (entry) {
      var byId = {};
      for (var i = 0; i < this.diagramOutline.length; i++) byId[this.diagramOutline[i].id] = this.diagramOutline[i];
      var need = [];
      if (entry.collapsible && this.diagramCollapsed.indexOf(entry.id) !== -1) need.push(entry.id);
      var p = entry.parent;
      while (p) {
        if (this.diagramCollapsed.indexOf(p) !== -1) need.push(p);
        p = byId[p] ? byId[p].parent : null;
      }
      if (need.length > 0) {
        for (var n = 0; n < need.length; n++) {
          if (this.expandSet.indexOf(need[n]) === -1) this.expandSet.push(need[n]);
        }
        await this.refetchDiagram();
      }
      var self = this;
      this.$nextTick(function () { self.flashNode(entry.id); });
    },
    /** scroll to a diagram node (site id L<line>#<idx>) + flash it — the shared kd-flash plumbing the
        outline jump (jumpTo) and the View.highlight({node}) sub-target both use. ids carry '#' (L26#0),
        so getElementById, never querySelector. */
    flashNode: function (siteId) {
      var el = document.getElementById('kd-' + siteId) || document.getElementById('kd-' + siteId + '.c');
      if (!el) return;
      el.scrollIntoView({ block: 'center' });
      el.classList.add('kd-flash');
      setTimeout(function () { el.classList.remove('kd-flash'); }, 1600);
    },
    /** View.highlight('rules.diagram', {node}): reveal the diagram slot (re-dock a collapsed rail),
        then flash the node once it's laid out. Self-sufficient — no prior ?ref=diagram needed. */
    focusNode: function (siteId) {
      var self = this;
      if (this.refState === 'collapsed') this.setRefState('docked');
      if (this.refSlot !== 'diagram') this.setRefSlot('diagram');
      this.$nextTick(function () { self.flashNode(siteId); });
    },
    /** View.highlight('rules.reference.code', {line}): reveal the source slot, then gotoLine. The code
        slot starts hidden (x-show); CodeMirror's scroll measures its host, so we must let the reveal PAINT
        (double rAF), remeasure, THEN scroll — otherwise it measures a hidden editor and the scroll no-ops. */
    focusLine: function (line) {
      if (!line || line < 1) return;
      if (this.refState === 'collapsed') this.setRefState('docked');
      if (this.refSlot !== 'code') this.setRefSlot('code');
      var go = function () {
        if (!window.cmEditor) return;
        if (window.cmEditor.refresh) window.cmEditor.refresh();
        if (window.cmEditor.gotoLine) window.cmEditor.gotoLine(line);
      };
      this.$nextTick(function () {
        requestAnimationFrame(function () { requestAnimationFrame(go); });
      });
    },
    /** click a node → expand a folded container, else highlight its source line BESIDE the
        diagram (D111a — no tab switch) */
    diagramClick: function (e) {
      // the corner ＋/− widgets are the only fold triggers (D114) — node body click is reserved
      var collapser = e.target.closest('[data-collapse]');
      if (collapser) {
        this.collapseNode(collapser.getAttribute('data-collapse'));
        return;
      }
      var folded = e.target.closest('[data-expand]');
      if (folded) {
        this.expandNode(folded.getAttribute('data-expand'));
        return;
      }
      var g = e.target.closest('[data-line]');
      if (!g) { this.stepsPopover = null; return; }
      var line = parseInt(g.getAttribute('data-line'), 10);
      // in the ⛶ Combo, diagram + code are side by side → jump to source (no scroll cost).
      if (this.diagramFull) {
        if (!line) return;
        if (window.cmEditor && window.cmEditor.gotoLine) window.cmEditor.gotoLine(line);
        return;
      }
      // docked slot mode (business view): no code beside, so a click POPS the node's inlined steps.
      // the SVG <title> already carries them — a merged bubble lists every member ("L12  const …"),
      // a leaf carries its own label. No re-fetch, no "All steps" relayout (D114 v4 follow-up).
      var node = g.closest('.kd-node') || g;
      var titleEl = node.querySelector('title');
      var raw = titleEl ? (titleEl.textContent || '') : '';
      var lines = raw.split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
      if (lines.length === 0) { this.stepsPopover = null; return; }
      this.stepsPopover = { lines: lines, line: line || null, x: e.clientX + 12, y: e.clientY + 8 };
    },
    closeStepsPopover: function () { this.stepsPopover = null; },
    /** hover a lookup node → its rate table as a popover (JsonTable) */
    diagramHover: function (e) {
      var g = e.target.closest('.kd-lookup');
      if (g === this._hoverEl) {
        if (this.lookupPopover) { this.lookupPopover.x = e.clientX + 14; this.lookupPopover.y = e.clientY + 10; }
        return;
      }
      this._hoverEl = g;
      if (!g) { this.lookupPopover = null; return; }
      var m = /lookup\.([A-Za-z_$][\w$]*)/.exec(g.textContent || '');
      var key = m && m[1];
      var val = key && this.rb.lookups ? this.rb.lookups[key] : null;
      if (val == null) { this.lookupPopover = null; return; }
      this.lookupPopover = { key: key, html: JsonTable.render(val), x: e.clientX + 14, y: e.clientY + 10 };
    },
    copyMermaid: async function () {
      if (!this.diagramMermaid) return;
      try { await navigator.clipboard.writeText(this.diagramMermaid); } catch (err) { return; }
      this.mermaidCopied = true;
      var self = this;
      setTimeout(function () { self.mermaidCopied = false; }, 1500);
    },

    /** decorate the rendered SVG in place: explore coverage chips + the selected scenario's
        hot path — site ids are shared (`L<line>#<idx>` ⇄ `#kd-<id>`), no mapping layer */
    decorateDiagram: function () {
      // one host, wherever it currently lives (it MOVES into the drawer when full-screen, D113-a)
      var host = this.$refs.diagramHost;
      if (!host) return;
      var per = (this.exploreReport && this.exploreReport.coverage && this.exploreReport.coverage.perSite) || null;
      var run = this.currentRun;
      var hot = (run && run.trace && run.trace.sites) || null;
      var nodes = host.querySelectorAll('.kd-node');
      for (var i = 0; i < nodes.length; i++) {
        var el = nodes[i];
        el.classList.remove('kd-cov-full', 'kd-cov-part', 'kd-cov-none', 'kd-hot');
        var id = el.getAttribute('id') || '';
        if (id.indexOf('kd-') !== 0) continue;
        var siteId = id.substring(3);
        if (per && Object.prototype.hasOwnProperty.call(per, siteId)) {
          var n = (per[siteId] || []).length;
          el.classList.add(n >= 2 ? 'kd-cov-full' : (n === 1 ? 'kd-cov-part' : 'kd-cov-none'));
        }
        if (hot && hot[siteId]) el.classList.add('kd-hot');
      }
    },

    // ── Explore (Rule.explore — D110/D111: coverage + suggested scenarios) ──
    runExplore: async function () {
      this.exploreLoading = true;
      var j = await ui('GET', '/rulebooks/' + this.rbName + '/explore');
      this.exploreLoading = false;
      if (j && j.error) { this.handleError(j); return; }
      this.exploreReport = j;
      this.explorePanel = (j.suite || []).length > 0;
      var self = this;
      this.$nextTick(function () { self.decorateDiagram(); });
    },
    clearExplore: function () {
      this.exploreReport = null;
      this.explorePanel = false;
      this.decorateDiagram();
    },
    exploreCoveragePct: function () {
      var c = this.exploreReport && this.exploreReport.coverage;
      return c ? c.percent : 0;
    },
    exploreChipText: function () {
      var c = this.exploreReport && this.exploreReport.coverage;
      return c ? c.percent + '% (' + c.covered + '/' + c.total + ')' : '';
    },
    addSuggested: function (suiteRow, i) {
      var row = JSON.parse(JSON.stringify(suiteRow.input || {}));
      row._label = 'explored ' + (i + 1);
      // a cell-suite row (D326): the oracle's output becomes the row's stored expectation, its cells the label
      if (suiteRow.target && suiteRow.target.length) row._label += ' · cells ' + suiteRow.target.join(', ');
      if (suiteRow.expected) row._expect = { output: JSON.parse(JSON.stringify(suiteRow.expected)) };
      this.postScenario(row);
    },

    // ── Scenarios ──
    get selectedScenario() { return this.scenarioById(this.selectedId); },
    /** the selected scenario's RETAINED run result (D111d) */
    get currentRun() { return this.selectedId ? this.runResults[this.selectedId] : null; },
    scenarioById: function (id) {
      if (!id || !this.rb.scenarios) return null;
      for (var i = 0; i < this.rb.scenarios.length; i++) {
        if (this.rb.scenarios[i]._id === id) return this.rb.scenarios[i];
      }
      return null;
    },
    filteredScenarios: function () {
      var list = this.rb.scenarios || [];
      var q = (this.search || '').trim().toLowerCase();
      if (!q) return list;
      return list.filter(function (s) {
        return (s._label || '').toLowerCase().indexOf(q) !== -1
            || s._id.toLowerCase().indexOf(q) !== -1;
      });
    },
    select: function (id) {
      this.selectedId = id;
      this.deletingId = null;
      this.actionMenuId = null;
    },
    startRename: function (s) {
      this.renamingId = s._id;
      this.renameDraft = s._label || '';
    },
    cancelRename: function () { this.renamingId = null; this.renameDraft = ''; },
    commitRename: function (s) {
      if (this.renamingId !== s._id) return;
      var next = (this.renameDraft || '').trim();
      var prev = s._label || '';
      this.renamingId = null;
      if (next === prev) return;
      this.patchScenario(s._id, { _label: next });
    },
    addScenario: function () {
      var defaults = SchemaForm.defaultFromSchema(this.rb.schema || {});
      defaults._label = 'New scenario';
      this.postScenario(defaults);
    },
    duplicate: function (s) {
      var copy = JSON.parse(JSON.stringify(s));
      delete copy._id;
      copy._label = (s._label || '') + ' (copy)';
      this.postScenario(copy);
    },
    confirmDelete: function (id) { this.deletingId = id; },
    doDelete: async function (id) {
      var out = await ui('POST', '/rulebooks/' + this.rbName + '/scenarios/' + id + '/delete',
        { ifMatch: this.rb.mtimes && this.rb.mtimes.scenarios });
      this.deletingId = null;
      if (out && out.error) return this.handleError(out);
      var next = Object.assign({}, this.editsBySc);
      delete next[id];
      this.editsBySc = next;
      var nr = Object.assign({}, this.runResults);
      delete nr[id];
      this.runResults = nr;
      if (this.selectedId === id) this.selectedId = null;
      await this.refreshRule();
    },

    // ── Mutation helpers (all over /ui/, all ifMatch-guarded — D86) ──
    postScenario: async function (row) {
      var out = await ui('POST', '/rulebooks/' + this.rbName + '/scenarios',
        { row: row, ifMatch: this.rb.mtimes && this.rb.mtimes.scenarios });
      if (out && out.error) return this.handleError(out);
      await this.refreshRule();
      if (out && out.id) this.selectedId = out.id;
    },
    patchScenario: async function (id, patch) {
      var out = await ui('POST', '/rulebooks/' + this.rbName + '/scenarios/' + id,
        { patch: patch, ifMatch: this.rb.mtimes && this.rb.mtimes.scenarios });
      if (out && out.error) return this.handleError(out);
      // the on-disk row shifted under any cached draft — drop it so selection reloads fresh
      var next = Object.assign({}, this.editsBySc);
      delete next[id];
      this.editsBySc = next;
      await this.refreshRule();
    },
    revertScenario: function () {
      var s = this.selectedScenario;
      if (!s) return;
      var next = Object.assign({}, this.editsBySc);
      delete next[s._id];
      this.editsBySc = next;
      this.loadEdit();
    },
    /** Save every dirty scenario draft — sequential Rule.scenario.update calls, each chaining the
        returned mtime as the next ifMatch (no batch PUT on the D86 surface; a 409 stops the run). */
    saveInputs: async function () {
      this.saving = true;
      this.saveMessage = '';
      var ifMatch = this.rb.mtimes && this.rb.mtimes.scenarios;
      var list = this.rb.scenarios || [];
      for (var i = 0; i < list.length; i++) {
        var s = list[i];
        if (!this.isScenarioDirty(s)) continue;
        var draft = this.editsBySc[s._id];
        var patch = JSON.parse(JSON.stringify(draft.edit));
        patch._label = draft.editLabel;
        var out = await ui('POST', '/rulebooks/' + this.rbName + '/scenarios/' + s._id,
          { patch: patch, ifMatch: ifMatch });
        if (out && out.error) { this.saving = false; return this.handleError(out); }
        if (out && out.mtime) ifMatch = out.mtime;
      }
      this.saving = false;
      this.editsBySc = {};
      this.saveMessage = 'saved';
      this.saveError = false;
      await this.refreshRule();
      var self = this;
      setTimeout(function () { self.saveMessage = ''; }, 2500);
    },
    saveCalc: async function () {
      if (!window.cmEditor) return;
      this.saving = true;
      this.saveMessage = '';
      var out = await ui('POST', '/rulebooks/' + this.rbName + '/calc',
        { calc: window.cmEditor.getDoc(), ifMatch: this.rb.mtimes && this.rb.mtimes.calc });
      this.saving = false;
      if (out && out.error) return this.handleError(out);
      this.dirtySource = false;
      this.saveMessage = 'calc.js saved';
      this.saveError = false;
      await this.refreshRule();
      this.pushSourceToEditor();
      // auto-run so the audit reflects the new buffer (the veriquant Cmd-S save+re-run cycle)
      if (this.selectedScenario) await this.runScenario();
      var self = this;
      setTimeout(function () { if (self.saveMessage === 'calc.js saved') self.saveMessage = ''; }, 2500);
    },
    /** the file tables load as summaries (D325(q)); their rows are an explicit read */
    loadTableRows: async function () {
      this.tableRowsLoaded = true;
      await this.refreshRule();
    },
    /** one update per home the draft changed (D325(b)): the inline lookup under the calc sha, each
        tables/ file under its own sha — a layered row's file comes from rb.tableHomes */
    lookupsUpdates: function () {
      var orig = this.rb.lookups || {};
      var draft = this.lookupsDraft || {};
      var tables = this.rb.tables || [];
      var homesOf = this.rb.tableHomes || {};
      var same = function (a, b) { return JSON.stringify(a) === JSON.stringify(b); };
      var tops = {};
      tables.forEach(function (t) { tops[t.key.split('.')[0]] = true; });
      var homes = {};
      var home = function (id, token) {
        if (!homes[id]) homes[id] = { lookups: {}, ifMatch: token };
        return homes[id].lookups;
      };
      var put = function (root, path, val) {
        var cur = root;
        for (var i = 0; i < path.length - 1; i++) cur = cur[path[i]] = cur[path[i]] || {};
        cur[path[path.length - 1]] = val;
      };
      var self = this;
      var walk = function (val, before, path) {
        if (same(val, before)) return;
        if (val !== null && typeof val === 'object' && !Array.isArray(val) && before && typeof before === 'object') {
          Object.keys(val).forEach(function (k) { walk(val[k], before[k], path.concat([k])); });
          return;
        }
        var dotted = path.join('.');
        var t = null;
        tables.forEach(function (x) {
          if (dotted.indexOf(x.key + '.') === 0 && (!t || x.key.length > t.key.length)) t = x;
        });
        if (!t) return;
        var file = t.layers
          ? homesOf[path.slice(0, t.key.split('.').length + 1 + t.keys.length).join('.')]
          : t.files[0].path;
        if (file) put(home(file, self.rb.tableShas && self.rb.tableShas[file]), path, val);
      };
      Object.keys(draft).forEach(function (k) {
        if (same(draft[k], orig[k])) return;
        if (!tops[k]) home('inline', self.rb.mtimes && self.rb.mtimes.calc)[k] = draft[k];
        else walk(draft[k], orig[k], [k]);
      });
      return Object.keys(homes).map(function (id) { return homes[id]; });
    },
    saveLookups: async function () {
      this.saving = true;
      this.saveMessage = '';
      var updates = this.lookupsUpdates();
      var out = null;
      for (var i = 0; i < updates.length; i++) {
        out = await ui('POST', '/rulebooks/' + this.rbName + '/lookups', updates[i]);
        if (out && out.error) break;
      }
      this.saving = false;
      if (out && out.error) return this.handleError(out);
      this.saveMessage = 'lookups saved';
      this.saveError = false;
      await this.refreshRule();
      this.loadLookupsDraft();
      this.pushSourceToEditor();   // the calc buffer changed under the hood (the AST splice)
      var self = this;
      setTimeout(function () { self.saveMessage = ''; }, 2500);
    },
    revertLookups: function () { this.loadLookupsDraft(); },
    revertSource: function () { this.pushSourceToEditor(); },

    /** the dirty surfaces, for the toolbar's per-surface Save/Revert buttons */
    dirtySurfaces: function () {
      var self = this;
      var out = [];
      if (this.anyDirty) out.push({ key: 'inputs', label: 'Inputs',
        save: function () { self.saveInputs(); }, revert: function () { self.revertScenario(); } });
      if (this.dirtyLookups) out.push({ key: 'lookups', label: 'Lookups',
        save: function () { self.saveLookups(); }, revert: function () { self.revertLookups(); } });
      if (this.dirtySource) out.push({ key: 'source', label: 'Source',
        save: function () { self.saveCalc(); }, revert: function () { self.revertSource(); } });
      return out;
    },

    /** v1 is save-then-run: flush every dirty surface (ifMatch makes save cheap+safe), then run.
        Every run is traced ({trace:true} — the hot-path overlay) and diffed; the server
        auto-advances the per-rulebook baseline. The result is RETAINED per scenario. */
    runScenario: async function () {
      var s = this.selectedScenario;
      if (!s) return;
      if (this.dirty || this.anyDirty) await this.saveInputs();
      if (this.dirtyLookups) await this.saveLookups();
      if (this.dirtySource) { await this.saveCalc(); return; }   // saveCalc re-runs itself
      await this.runOne(s._id, this.editLabel || s._label || s._id);
    },
    /** the one run call both single-Run and Run-All share */
    runOne: async function (id, label) {
      var start = Object.assign({}, this.runningIds);
      start[id] = true;
      this.runningIds = start;
      var j = await ui('POST', '/rulebooks/' + this.rbName + '/run', { scenarioId: id, trace: true });
      var end = Object.assign({}, this.runningIds);
      delete end[id];
      this.runningIds = end;
      j.__forScenarioId = id;
      j.__forScenarioLabel = label;
      j.__runTs = Date.now();
      var next = Object.assign({}, this.runResults);
      next[id] = j;
      this.runResults = next;
      var self = this;
      this.$nextTick(function () { self.decorateDiagram(); });
      return j;
    },

    headlineOf: function (output) {
      if (output == null) return '';
      if (typeof output !== 'object') return String(output);
      // first numeric top-level field is the scan headline (total, premium, …)
      var keys = Object.keys(output);
      for (var i = 0; i < keys.length; i++) {
        if (typeof output[keys[i]] === 'number') return keys[i] + ': ' + output[keys[i]];
      }
      return '';
    },
    dotClass: function (id) {
      if (this.runningIds[id]) return 'bg-indigo-500 animate-pulse';
      var r = this.runResults[id];
      if (!r) return 'bg-slate-200 dark:bg-slate-600';
      return r.error ? 'bg-rose-500' : 'bg-emerald-500';
    },
    dotTitle: function (id) {
      if (this.runningIds[id]) return 'Running…';
      var r = this.runResults[id];
      if (!r) return 'Not yet run';
      // a rules-row failure = the CALC ERRORED (scenarios carry no expectations)
      if (r.error) return 'Calc error: ' + (r.code || r.error);
      return 'Ran clean ' + (r.durationMs != null ? r.durationMs + 'ms' : '');
    },
    resultLineFor: function (id) {
      if (this.runningIds[id]) return '…';
      var r = this.runResults[id];
      if (!r) return '';
      if (r.error) return r.code || r.error;
      return this.headlineOf(r.output);
    },
    /** ▶ All — the shared RunAll machinery (pub/run-all.js); results retained per row (D111d) */
    runAll: async function () {
      if (this.runAllRunning) return;
      var self = this;
      this.runAllRunning = true;
      this.runAllStop = false;
      this.runResults = {};
      this.runAllSummary = null;
      this.runAllDoneCount = 0;
      var summary = await RunAll.sequential(this.rb.scenarios || [], function (s) {
        return self.runOne(s._id, s._label || s._id);
      }, {
        shouldStop: function () { return self.runAllStop; },
        onDone: function () { self.runAllDoneCount++; }
      });
      this.runAllSummary = summary;
      this.runAllRunning = false;
    },
    stopRunAll: function () { this.runAllStop = true; },
    clearCurrent: function () {
      if (!this.selectedId) return;
      var next = Object.assign({}, this.runResults);
      delete next[this.selectedId];
      this.runResults = next;
      this.decorateDiagram();
    },
    clearAll: function () {
      this.runResults = {};
      this.runAllSummary = null;
      this.runAllDoneCount = 0;
      this.selectedId = null;       // also deselect (convenience) — the Input form returns to "pick a scenario"
      this.decorateDiagram();
    },

    // ── Lookups (shape-driven — D111b) ──
    loadLookupsDraft: function () {
      this.lookupsDraft = JSON.parse(JSON.stringify(this.rb.lookups || {}));
    },
    get dirtyLookups() {
      return JSON.stringify(this.lookupsDraft) !== JSON.stringify(this.rb.lookups || {});
    },
    /** classify each top-level lookup: uniform-object-array → 'grid' (excel-like, fixed columns),
        flat scalar map → 'kv', anything else → 'tree' (the recursive fallback) */
    lookupShapes: function () {
      var draft = this.lookupsDraft || {};
      var out = [];
      var isPlain = function (v) { return v !== null && typeof v === 'object' && !Array.isArray(v); };
      Object.keys(draft).forEach(function (key) {
        var v = draft[key];
        if (Array.isArray(v) && v.length > 0 && v.every(isPlain)) {
          out.push({ key: key, kind: 'grid', cols: JsonTable.gridColumns(v) });
        } else if (isPlain(v) && Object.keys(v).every(function (k) { return v[k] === null || typeof v[k] !== 'object'; })) {
          out.push({ key: key, kind: 'kv' });
        } else {
          out.push({ key: key, kind: 'tree' });
        }
      });
      return out;
    },
    isLookupCollapsed: function (pathArr) { return !!this.lookupCollapsed[pathArr.join('/')]; },
    toggleLookupCollapse: function (pathArr) {
      var k = pathArr.join('/');
      var next = Object.assign({}, this.lookupCollapsed);
      if (next[k]) delete next[k]; else next[k] = true;
      this.lookupCollapsed = next;
    },
    /** ⊞/⊟ bulk fold the Lookups pane (D113-a) — folds every top-level lookup SECTION down to its
        key header (the obvious behavior; the bodies are mostly flat grids/kv, so collapsing the
        section is what reads as "collapse all"). Each section's collapse key is its bare top-level
        name (a single path segment — never collides with tree sub-node keys `key/child`). */
    expandAllLookups: function () { this.lookupCollapsed = {}; },
    collapseAllLookups: function () {
      var next = {};
      this.lookupShapes().forEach(function (ls) { next[ls.key] = true; });
      this.lookupCollapsed = next;
    },
    /** is a nested lookup value a renderable embedded grid / kv map? (D113-a recursive classifier) */
    lookupNodeShape: function (val) {
      var isPlain = function (v) { return v !== null && typeof v === 'object' && !Array.isArray(v); };
      if (Array.isArray(val) && val.length > 0 && val.every(isPlain)) return 'grid';
      if (isPlain(val) && Object.keys(val).length > 0
          && Object.keys(val).every(function (k) { return val[k] === null || typeof val[k] !== 'object'; })) return 'kv';
      return null;
    },
    getLookupValue: function (pathArr) {
      var cur = this.lookupsDraft;
      for (var i = 0; i < pathArr.length; i++) {
        if (cur == null) return null;
        cur = cur[pathArr[i]];
      }
      return cur;
    },
    setLookupValue: function (pathArr, val) {
      var next = JSON.parse(JSON.stringify(this.lookupsDraft));
      var cur = next;
      for (var i = 0; i < pathArr.length - 1; i++) cur = cur[pathArr[i]];
      cur[pathArr[pathArr.length - 1]] = val;
      this.lookupsDraft = next;
    },
    addLookupArrayItem: function (pathArr) {
      var next = JSON.parse(JSON.stringify(this.lookupsDraft));
      var cur = next;
      for (var i = 0; i < pathArr.length; i++) cur = cur[pathArr[i]];
      if (!Array.isArray(cur)) return;
      var tmpl;
      if (cur.length > 0 && cur[0] !== null && typeof cur[0] === 'object' && !Array.isArray(cur[0])) {
        tmpl = {};
        Object.keys(cur[0]).forEach(function (k) {
          var v = cur[0][k];
          tmpl[k] = typeof v === 'number' ? 0 : (typeof v === 'boolean' ? false : '');
        });
      } else {
        tmpl = 0;
      }
      cur.push(tmpl);
      this.lookupsDraft = next;
    },
    removeLookupArrayItem: function (pathArr) {
      var next = JSON.parse(JSON.stringify(this.lookupsDraft));
      var idx = pathArr[pathArr.length - 1];
      var cur = next;
      for (var i = 0; i < pathArr.length - 1; i++) cur = cur[pathArr[i]];
      if (Array.isArray(cur)) cur.splice(idx, 1);
      this.lookupsDraft = next;
    },
    /** the tree fallback, scoped to one top-level lookup key */
    flattenLookupsFor: function (rootKey) {
      var out = [];
      var self = this;
      var walk = function (val, pathArr, depth, labelOverride) {
        var label = labelOverride != null ? labelOverride : pathArr[pathArr.length - 1];
        if (val === null || val === undefined || typeof val !== 'object') {
          var t = typeof val === 'number' ? 'number'
                : typeof val === 'boolean' ? 'boolean'
                : 'string';
          out.push({ path: pathArr, depth: depth, kind: 'primitive', label: label, type: t });
          return;
        }
        // a file table not yet loaded is its summary (D325(q)) — shown, never editable
        if (!Array.isArray(val) && val._table) {
          out.push({ path: pathArr, depth: depth, kind: 'table-summary', label: label, table: val._table });
          return;
        }
        // D113-a: a nested uniform-object-array / flat scalar map embeds as a grid in place,
        // not as more tree rows (the fleetSize.ranges scenario). The write-back path is unchanged —
        // getLookupValue/setLookupValue/add/removeLookupArrayItem already take arbitrary paths.
        var embed = self.lookupNodeShape(val);
        if (embed === 'grid') {
          out.push({ path: pathArr, depth: depth, kind: 'grid-embed', label: label,
                     cols: JsonTable.gridColumns(val), size: val.length });
          return;
        }
        if (embed === 'kv') {
          out.push({ path: pathArr, depth: depth, kind: 'kv-embed', label: label,
                     keys: Object.keys(val) });
          return;
        }
        if (Array.isArray(val)) {
          out.push({ path: pathArr, depth: depth, kind: 'array', label: label, size: val.length });
          if (self.isLookupCollapsed(pathArr)) return;
          for (var i = 0; i < val.length; i++) {
            var itemPath = pathArr.concat([i]);
            var item = val[i];
            if (item !== null && typeof item === 'object') {
              out.push({ path: itemPath, depth: depth + 1, kind: 'array-item', label: '[' + i + ']' });
              if (!self.isLookupCollapsed(itemPath)) {
                if (Array.isArray(item)) {
                  walk(item, itemPath, depth + 2, '[' + i + ']');
                } else {
                  Object.keys(item).forEach(function (k) {
                    walk(item[k], itemPath.concat([k]), depth + 2, k);
                  });
                }
              }
            } else {
              walk(item, itemPath, depth + 1, '[' + i + ']');
            }
          }
          return;
        }
        var keys = Object.keys(val);
        out.push({ path: pathArr, depth: depth, kind: 'object', label: label, size: keys.length });
        if (self.isLookupCollapsed(pathArr)) return;
        keys.forEach(function (k) {
          walk(val[k], pathArr.concat([k]), depth + 1, k);
        });
      };
      var root = (this.lookupsDraft || {})[rootKey];
      if (root !== null && typeof root === 'object' && !Array.isArray(root) && !root._table) {
        // skip the redundant root row — the section header already names the key
        Object.keys(root).forEach(function (k) {
          walk(root[k], [rootKey, k], 0, k);
        });
      } else {
        walk(root, [rootKey], 0, rootKey);
      }
      return out;
    },

    // ── Run-result rendering ──
    get lastRunTsFmt() {
      var r = this.currentRun;
      if (!r || !r.__runTs) return '';
      var d = new Date(r.__runTs);
      var pad = function (n) { return n < 10 ? '0' + n : '' + n; };
      return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    },
    // ── Audit display tree (D113-a) — collapsible containers + chain-dedup + diff rollup ──
    /** Build a normalized DISPLAY tree from the raw audit nodes:
        · empty anonymous containers flatten their children up (pure structural noise)
        · a single-child container CHAIN merges into one row, titles joined ' · ' (consecutive
          duplicates deduped — the "Iteration: N" three-deep scenario)
        · a generic container title ("Iteration: 3") is PROMOTED by appending the first
          descriptive leaf ("Iteration: 3 — Vehicle 3 Physical Damage"); that leaf is then
          hidden as a separate row (__promoted) */
    _genericAuditLabel: function (t) { return /^\s*(iteration|item|row|loop)\s*:?\s*\d*\s*$/i.test(t || ''); },
    _displayChildren: function (rawList) {
      var self = this;
      var res = [];
      (rawList || []).forEach(function (raw) {
        var hasText = raw.text && raw.text.length > 0;
        if (raw.container && !hasText) {
          res = res.concat(self._displayChildren(raw.children || []));   // splice anon scope up
          return;
        }
        res.push(self._toDisplayNode(raw));
      });
      return res;
    },
    _toDisplayNode: function (raw) {
      if (!raw.container) {
        return { key: raw.key, kind: (raw.heading > 0 ? 'heading' : 'leaf'), heading: raw.heading || 0,
                 title: raw.text || '', node: raw, children: [] };
      }
      var titleParts = [raw.text || ''];
      var kids = this._displayChildren(raw.children || []);
      var innerKey = raw.key;
      while (kids.length === 1 && kids[0].kind === 'container') {           // unwrap chain
        innerKey = kids[0].key;
        if (kids[0].title) titleParts.push(kids[0].title);
        kids = kids[0].children;
      }
      var parts = [];
      titleParts.forEach(function (p) { if (p && parts[parts.length - 1] !== p) parts.push(p); });
      var title = parts.join(' · ');
      if (this._genericAuditLabel(title)) {                                // promote first leaf
        for (var i = 0; i < kids.length; i++) {
          if (kids[i].kind !== 'container' && kids[i].title) {
            title = title + ' — ' + kids[i].title;
            kids[i].__promoted = true;
            break;
          }
        }
      }
      return { key: innerKey, kind: 'container', heading: 0, title: title, node: raw, children: kids };
    },
    /** the diff rollup for a (possibly collapsed) container subtree — counts added/removed/changed
        among ALL descendants (ignores collapse), client-side */
    _subtreeDiff: function (dn) {
      var a = 0, r = 0, c = 0;
      var walk = function (list) {
        (list || []).forEach(function (d) {
          if (d.kind !== 'container') {
            if (d.node.status === 'added') a++;
            else if (d.node.status === 'removed') r++;
            else if (d.node.status === 'changed') c++;
          }
          if (d.children && d.children.length) walk(d.children);
        });
      };
      walk(dn.children);
      return { a: a, r: r, c: c, any: (a + r + c) > 0 };
    },
    get auditDisplay() { return this.auditDisplayFor(this.currentRun); },
    /** the normalized display tree for ANY run (currentRun in the Result column, scenarioRun in the
        Scenario Coverage workbench — D114 v4 follow-up) */
    auditDisplayFor: function (run) {
      if (!run || run.error || !run.audit) return [];
      return this._displayChildren(run.audit);
    },
    /** flatten a run's display tree to rows, honoring a per-run collapse map; builds the parent map
        (for ancestor auto-expand) as a side effect on this._auditParents (Result column only) */
    auditRowsFor: function (run, collapseMap) {
      var self = this;
      var trackParents = (run === this.currentRun);
      if (trackParents) this._auditParents = {};
      var out = [];
      var walk = function (nodes, depth, parentKey) {
        (nodes || []).forEach(function (dn) {
          if (dn.__promoted) return;
          if (trackParents) self._auditParents[dn.key] = parentKey;
          var collapsible = dn.kind === 'container' && dn.children.length > 0;
          var collapsed = collapsible && !!collapseMap[dn.key];
          var rollup = (collapsible && collapsed) ? self._subtreeDiff(dn) : null;
          out.push({ key: dn.key, depth: depth, kind: dn.kind, heading: dn.heading,
                     title: dn.title, node: dn.node, collapsible: collapsible,
                     collapsed: collapsed, rollup: rollup });
          if (collapsible && !collapsed) walk(dn.children, depth + 1, dn.key);
        });
      };
      walk(this.auditDisplayFor(run), 0, null);
      return out;
    },
    auditRows: function () { return this.auditRowsFor(this.currentRun, this.auditCollapsed); },
    /** the Scenario-Coverage workbench audit rows (its own collapse map; no diff-nav/parent tracking) */
    scenarioAuditRows: function () { return this.auditRowsFor(this.scenarioRun, this.scenarioAuditCollapsed); },
    toggleScenarioAuditCollapse: function (key) {
      var next = Object.assign({}, this.scenarioAuditCollapsed);
      if (next[key]) delete next[key]; else next[key] = true;
      this.scenarioAuditCollapsed = next;
    },
    /** every collapsible-container key in a display tree (for expand-all/collapse-all) */
    _auditContainerKeys: function (displayNodes) {
      var keys = [];
      var walk = function (nodes) {
        (nodes || []).forEach(function (dn) {
          if (dn.__promoted) return;
          if (dn.kind === 'container' && dn.children.length > 0) keys.push(dn.key);
          if (dn.children && dn.children.length) walk(dn.children);
        });
      };
      walk(displayNodes);
      return keys;
    },
    // main Result audit ⊟/⊞ (parity with Inputs/Lookups — the planned A-item)
    expandAllAudit: function () { this.auditCollapsed = {}; },
    collapseAllAudit: function () {
      var next = {};
      this._auditContainerKeys(this.auditDisplayFor(this.currentRun)).forEach(function (k) { next[k] = true; });
      this.auditCollapsed = next;
    },
    // workbench audit ⊟/⊞
    expandAllScenarioAudit: function () { this.scenarioAuditCollapsed = {}; },
    collapseAllScenarioAudit: function () {
      var next = {};
      this._auditContainerKeys(this.auditDisplayFor(this.scenarioRun)).forEach(function (k) { next[k] = true; });
      this.scenarioAuditCollapsed = next;
    },
    /** the workbench's Audit⇄Output split — same shape as the main Result column (resultRowStyle),
        its own scenarioCols + scenarioOutputSplit. Keeps ≥1 visible. */
    scenarioResultRowStyle: function (which) {
      if (!this.scenarioCols[which]) return 'display:none';
      var other = which === 'audit' ? 'output' : 'audit';
      if (!this.scenarioCols[other]) return 'flex: 1 1 100%';
      return which === 'audit'
          ? 'flex: 1 1 ' + (100 - this.scenarioOutputSplit) + '%'
          : 'flex: 0 0 ' + this.scenarioOutputSplit + '%';
    },
    toggleScenarioCol: function (col) {
      var partner = col === 'audit' ? 'output' : 'audit';
      if (this.scenarioCols[col] && !this.scenarioCols[partner]) return;
      var next = Object.assign({}, this.scenarioCols);
      next[col] = !next[col];
      this.scenarioCols = next;
    },
    startDragScenarioOutput: function (e) {
      var self = this;
      var host = this.$refs.scenarioResultBody;
      if (!host) return;
      var rect = host.getBoundingClientRect();
      var move = function (ev) {
        self.scenarioOutputSplit = Math.max(12, Math.min(((rect.bottom - ev.clientY) / rect.height) * 100, 80));
      };
      var up = function () {
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },
    toggleAuditCollapse: function (key) {
      var next = Object.assign({}, this.auditCollapsed);
      if (next[key]) delete next[key]; else next[key] = true;
      this.auditCollapsed = next;
    },
    /** expand the ancestor chain of an audit node so a hidden diff/search hit becomes visible */
    auditExpandAncestors: function (key) {
      // rebuild the parent map over the FULL tree (collapse-independent)
      var parents = {};
      var walk = function (nodes, parentKey) {
        (nodes || []).forEach(function (dn) {
          if (dn.__promoted) return;
          parents[dn.key] = parentKey;
          if (dn.children && dn.children.length) walk(dn.children, dn.key);
        });
      };
      walk(this.auditDisplay, null);
      var next = Object.assign({}, this.auditCollapsed);
      var p = parents[key];
      while (p) { delete next[p]; p = parents[p]; }
      this.auditCollapsed = next;
    },
    /** ordered keys of every diff leaf in the full tree (collapse-independent) — drives ↑/↓ nav */
    _auditDiffKeys: function () {
      var keys = [];
      var walk = function (nodes) {
        (nodes || []).forEach(function (dn) {
          if (dn.__promoted) return;
          if (dn.kind !== 'container' && (dn.node.status === 'added'
              || dn.node.status === 'removed' || dn.node.status === 'changed')) keys.push(dn.key);
          if (dn.children && dn.children.length) walk(dn.children);
        });
      };
      walk(this.auditDisplay);
      return keys;
    },
    auditDiffCount: function () { return this._auditDiffKeys().length; },
    auditDiffNav: function (dir) {
      var keys = this._auditDiffKeys();
      if (!keys.length) return;
      var cur = this._auditDiffCursor;
      var i = (typeof cur === 'number') ? cur : -1;
      i = (i + dir + keys.length) % keys.length;
      this._auditDiffCursor = i;
      this.auditExpandAncestors(keys[i]);
      this.scrollFlashAudit(keys[i]);
    },
    _auditDiffCursor: -1,
    scrollFlashAudit: function (key) {
      var self = this;
      this.$nextTick(function () {
        // audit row ids carry the node key (contains ':') — getElementById, never querySelector
        var el = document.getElementById('audit-' + key);
        if (!el) return;
        el.scrollIntoView({ block: 'center' });
        el.classList.add('ring-2', 'ring-indigo-400');
        setTimeout(function () { el.classList.remove('ring-2', 'ring-indigo-400'); }, 1200);
      });
    },
    /** Tier-1 token tinting (D113-a) — tokenize the RAW text once (never regex over inserted
        markup, the digit-in-classname collision): a leading "Label:" dims, numbers/currency/
        percent brighten, ×→= go quiet. Returns escaped HTML for x-html. */
    tintAudit: function (text) {
      var esc = function (s) {
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      };
      var raw = (text == null) ? '' : String(text);
      var labelHtml = '', rest = raw;
      var lm = /^([^:<>{}\n]{1,40}?:)(\s*)/.exec(raw);
      if (lm) {
        labelHtml = '<span class="text-slate-400 dark:text-slate-500">' + esc(lm[1]) + '</span>' + lm[2];
        rest = raw.slice(lm[0].length);
      }
      var re = /(\$[\d,]+(?:\.\d+)?|\d+(?:\.\d+)?%|\d+(?:\.\d+)?|[×→=])/g;
      var out = '', last = 0, m;
      while ((m = re.exec(rest))) {
        out += esc(rest.slice(last, m.index));
        var tok = m[0];
        out += /[×→=]/.test(tok)
            ? '<span class="text-slate-300 dark:text-slate-600">' + esc(tok) + '</span>'
            : '<span class="text-indigo-600 dark:text-indigo-300 font-medium">' + esc(tok) + '</span>';
        last = m.index + tok.length;
      }
      out += esc(rest.slice(last));
      return labelHtml + out;
    },
    rowStatusClass: function (node) {
      switch (node.status) {
        case 'added':   return 'border-emerald-400 dark:border-emerald-600 bg-emerald-50/40 dark:bg-emerald-950/30';
        case 'removed': return 'border-rose-400 dark:border-rose-600 bg-rose-50/40 dark:bg-rose-950/30';
        case 'changed': return 'border-amber-400 dark:border-amber-600 bg-amber-50/40 dark:bg-amber-950/30';
        default:        return 'border-transparent';
      }
    },

    // ── Scenario draft cache (per-scenario, survives row switches + reloads) ──
    loadEdit: function () {
      var s = this.selectedScenario;
      if (!s) return;
      if (!this.editsBySc[s._id]) {
        var clone = JSON.parse(JSON.stringify(s));
        delete clone._id;
        delete clone._label;
        var wrap = {};
        wrap[s._id] = { edit: clone, editLabel: s._label || '' };
        this.editsBySc = Object.assign({}, this.editsBySc, wrap);
      }
    },
    get currentDraft() {
      var s = this.selectedScenario;
      if (!s) return { edit: {}, editLabel: '' };
      return this.editsBySc[s._id] || { edit: {}, editLabel: '' };
    },
    get edit() { return this.currentDraft.edit; },
    get editLabel() { return this.currentDraft.editLabel; },
    set editLabel(v) {
      var s = this.selectedScenario; if (!s) return;
      this._writeDraft(s._id, function (d) { d.editLabel = v; });
    },
    _writeDraft: function (id, mutator) {
      var cur = this.editsBySc[id];
      var next = cur
        ? { edit: JSON.parse(JSON.stringify(cur.edit)), editLabel: cur.editLabel }
        : { edit: {}, editLabel: '' };
      mutator(next);
      var wrap = {}; wrap[id] = next;
      this.editsBySc = Object.assign({}, this.editsBySc, wrap);
    },
    // SchemaForm (pub/schema-form.js) drives the recursive inputs widget
    getEditValue: function (pathArr) { return SchemaForm.getValue(this.edit, pathArr); },
    setEditValue: function (pathArr, val) {
      var s = this.selectedScenario; if (!s) return;
      this._writeDraft(s._id, function (d) { SchemaForm.setValue(d.edit, pathArr, val); });
    },
    isInputCollapsed: function (pathArr) { return !!this.inputCollapsed[pathArr.join('.')]; },
    toggleInputCollapse: function (pathArr) {
      var k = pathArr.join('.');
      var next = Object.assign({}, this.inputCollapsed);
      if (next[k]) delete next[k]; else next[k] = true;
      this.inputCollapsed = next;
    },
    /** ⊞/⊟ bulk fold the Inputs form (D113-a) — collapse-all walks the schema/edit with an empty
        collapse map so every object/array path is reachable; expand-all clears the map */
    expandAllInputs: function () { this.inputCollapsed = {}; },
    collapseAllInputs: function () {
      var rows = SchemaForm.flatten(this.rb.schema || {}, this.edit || {}, {});
      var next = {};
      rows.forEach(function (r) {
        if (r.kind === 'object' || r.kind === 'array') next[r.path.join('.')] = true;
      });
      this.inputCollapsed = next;
    },
    addInputArrayItem: function (pathArr, elemSchema) {
      var s = this.selectedScenario; if (!s) return;
      this._writeDraft(s._id, function (d) {
        var cur = SchemaForm.getValue(d.edit, pathArr);
        if (cur == null) { cur = []; SchemaForm.setValue(d.edit, pathArr, cur); }
        if (Array.isArray(cur)) cur.push(SchemaForm.defaultFromSchema(elemSchema));
      });
    },
    removeInputArrayItem: function (pathArr) {
      var s = this.selectedScenario; if (!s) return;
      this._writeDraft(s._id, function (d) {
        var idx = pathArr[pathArr.length - 1];
        var cur = SchemaForm.getValue(d.edit, pathArr.slice(0, -1));
        if (Array.isArray(cur)) cur.splice(idx, 1);
      });
    },
    flatFormFields: function () {
      // `label`/`id` are scenario identity (edited in the tinted header), not rating inputs — drop
      // the stray top-level rows so the schema-driven form shows only the calc's actual inputs.
      return SchemaForm.flatten(this.rb.schema || {}, this.edit || {}, this.inputCollapsed)
          .filter(function (r) { return !(r.path.length === 1 && r.path[0].charAt(0) === '_'); });
    },
    isScenarioDirty: function (s) {
      if (!s) return false;
      var draft = this.editsBySc[s._id];
      if (!draft) return false;
      var baseline = JSON.parse(JSON.stringify(s));
      delete baseline._id;
      delete baseline._label;
      if (JSON.stringify(baseline) !== JSON.stringify(draft.edit)) return true;
      if ((draft.editLabel || '') !== (s._label || '')) return true;
      return false;
    },
    get dirty() { return this.isScenarioDirty(this.selectedScenario); },
    get anyDirty() {
      var list = (this.rb.scenarios || []);
      for (var i = 0; i < list.length; i++) {
        if (this.isScenarioDirty(list[i])) return true;
      }
      return false;
    },

    // ── Per-pane search (D113-a — pub/pane-search.js supplies the matcher; highlight-don't-hide,
    //    auto-expand ancestors, count + ↑/↓ cycle) ──
    paneState: function (which) {
      return which === 'inputs' ? this.inputsSearch
           : which === 'lookups' ? this.lookupsSearch : this.auditSearch;
    },
    /** the searchable {key, text} items for a pane (whole model, incl. hidden/collapsed rows) */
    _paneItems: function (which) {
      var items = [];
      if (which === 'inputs') {
        SchemaForm.flatten(this.rb.schema || {}, this.edit || {}, {}).forEach(function (r) {
          var v = SchemaForm.getValue(this.edit || {}, r.path);
          items.push({ key: r.path.join('.'), text: r.label + ' ' + (v == null ? '' : v) });
        }, this);
      } else if (which === 'lookups') {
        var self = this;
        this.lookupShapes().forEach(function (ls) {
          items.push({ key: ls.key, text: ls.key });
          if (ls.kind === 'grid' || ls.kind === 'kv') {
            items.push({ key: ls.key, text: JSON.stringify(self.lookupsDraft[ls.key]) });
          } else {
            self.flattenLookupsFor(ls.key).forEach(function (r) {
              var v = self.getLookupValue(r.path);
              items.push({ key: r.path.join('/'), text: r.label + ' ' + (v == null || typeof v === 'object' ? '' : v) });
            });
          }
        });
      } else {
        var add = function (nodes) {
          (nodes || []).forEach(function (dn) {
            if (dn.__promoted) return;
            items.push({ key: dn.key, text: dn.title });
            if (dn.children && dn.children.length) add(dn.children);
          });
        };
        add(this.auditDisplay);
      }
      return items;
    },
    paneSearchRun: function (which) {
      var st = this.paneState(which);
      var q = (st.q || '').trim();
      if (!q) { st.matches = []; st.idx = 0; return; }
      var keys = [];
      this._paneItems(which).forEach(function (it) {
        if (PaneSearch.match(it.text, q) && keys.indexOf(it.key) === -1) keys.push(it.key);
      });
      st.matches = keys;
      if (st.idx >= keys.length) st.idx = 0;
      if (keys.length) this._paneReveal(which, keys[st.idx]);
    },
    paneSearchStep: function (which, dir) {
      var st = this.paneState(which);
      if (!st.matches.length) return;
      st.idx = (st.idx + dir + st.matches.length) % st.matches.length;
      this._paneReveal(which, st.matches[st.idx]);
    },
    paneSearchToggle: function (which) {
      var st = this.paneState(which);
      st.open = !st.open;
      if (!st.open) { st.q = ''; st.matches = []; st.idx = 0; }
    },
    isPaneMatch: function (which, key) {
      var st = this.paneState(which);
      return st.open && st.q && st.matches.indexOf(key) !== -1;
    },
    isPaneCurrentMatch: function (which, key) {
      var st = this.paneState(which);
      return st.open && st.matches.length > 0 && st.matches[st.idx] === key;
    },
    _paneReveal: function (which, key) {
      var self = this;
      if (which === 'audit') {
        this.auditExpandAncestors(key);
        this.scrollFlashAudit(key);
        return;
      }
      var sep = which === 'inputs' ? '.' : '/';
      var map = which === 'inputs' ? this.inputCollapsed : this.lookupCollapsed;
      var parts = key.split(sep);
      var next = Object.assign({}, map);
      delete next[key];   // expand the matched node/section itself (lookups sections key on their bare name)
      for (var i = 1; i < parts.length; i++) delete next[parts.slice(0, i).join(sep)];
      if (which === 'inputs') this.inputCollapsed = next; else this.lookupCollapsed = next;
      this.$nextTick(function () {
        var el = document.getElementById((which === 'inputs' ? 'input-' : 'lookup-') + key);
        if (!el) return;
        el.scrollIntoView({ block: 'center' });
        el.classList.add('ring-2', 'ring-indigo-400');
        setTimeout(function () { el.classList.remove('ring-2', 'ring-indigo-400'); }, 1200);
      });
    },

    // ── The ⛶ Combo full-screen drawer (D114 v4: outline | diagram | code; D113-a drawer pattern) ──
    // Physically MOVE the single rendered hosts into the drawer (and back on close) rather than
    // duplicate them — duplicate element ids (SVG sites carry L<line>#<idx>; #cm-host is id-keyed by
    // the CM module) would break getElementById + the single CodeMirror view. One host each = one
    // set of ids/handlers/decoration, riding along untouched. (state stays `diagramFull`.)
    openDiagramFull: function () {
      this.diagramFull = true;
      var self = this;
      this.$nextTick(function () {
        var host = self.$refs.diagramHost, slot = self.$refs.diagramFullSlot;
        if (host && slot && host.parentNode !== slot) {
          self._diagramHome = host.parentNode;
          self._diagramHomeNext = host.nextSibling;
          slot.appendChild(host);
        }
        var cm = document.getElementById('cm-host'), cslot = self.$refs.comboCodeSlot;
        if (cm && cslot && cm.parentNode !== cslot) {
          self._cmHome = cm.parentNode;
          self._cmHomeNext = cm.nextSibling;
          cslot.appendChild(cm);
          if (window.cmEditor && window.cmEditor.refresh) window.cmEditor.refresh();
        }
        self.decorateDiagram();
      });
    },
    closeDiagramFull: function () {
      var host = this.$refs.diagramHost;
      if (host && this._diagramHome) {
        this._diagramHome.insertBefore(host, this._diagramHomeNext || null);
      }
      var cm = document.getElementById('cm-host');
      if (cm && this._cmHome) {
        this._cmHome.insertBefore(cm, this._cmHomeNext || null);
      }
      this.diagramFull = false;
      var self = this;
      this.$nextTick(function () {
        self.decorateDiagram();
        if (window.cmEditor && window.cmEditor.refresh) window.cmEditor.refresh();
      });
    },

    // ── Splitters ──
    startDragLeft: function (e) {
      var self = this;
      this.draggingLeft = true;
      var rect = this.$refs.splitRoot.getBoundingClientRect();
      var move = function (ev) {
        var pct = ((ev.clientX - rect.left) / rect.width) * 100;
        var refW = self.refState === 'collapsed' ? 4 : self.refPct;   // rail ≈ a few %
        pct = Math.max(12, Math.min(pct, 100 - refW - 12));
        self.leftPct = pct;
      };
      var up = function () {
        self.draggingLeft = false;
        kaPrefs.set('rb.leftPct', self.leftPct);
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },
    /** drag the Reference panel's left edge → refPct (inert when the panel is collapsed) */
    startDragRef: function (e) {
      if (this.refState === 'collapsed') return;
      var self = this;
      this.draggingRef = true;
      var rect = this.$refs.splitRoot.getBoundingClientRect();
      var move = function (ev) {
        var pct = ((rect.right - ev.clientX) / rect.width) * 100;
        pct = Math.max(14, Math.min(pct, 100 - self.leftPct - 18));
        self.refPct = pct;
      };
      var up = function () {
        self.draggingRef = false;
        kaPrefs.set('rb.ref.width', self.refPct);
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },
    /** the two surviving inner splitters (D114 v4): 'center' drags the vertical Input|Result
        divider in the center (→ centerSplit); 'output' drags the horizontal Audit|Output divider
        in the Result column (→ outputSplit). Both persist on mouse-up. */
    startDragPair: function (e, which) {
      var self = this;
      var host = this.$refs[which === 'output' ? 'resultBody' : 'centerBody'];
      if (!host) return;
      var rect = host.getBoundingClientRect();
      var move = function (ev) {
        if (which === 'output') {
          var p = ((rect.bottom - ev.clientY) / rect.height) * 100;
          self.outputSplit = Math.max(12, Math.min(p, 75));
        } else {
          var c = ((ev.clientX - rect.left) / rect.width) * 100;
          self.centerSplit = Math.max(20, Math.min(c, 80));
        }
      };
      var up = function () {
        kaPrefs.set(which === 'output' ? 'rb.resultSplit' : 'rb.centerSplit',
            which === 'output' ? self.outputSplit : self.centerSplit);
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },

    // ── ⛶ Combo column layout (toggles + draggable splitters; diagram is the grow column) ──
    comboToggle: function (col) {
      var visible = this.comboCols.outline + this.comboCols.diagram + this.comboCols.code;
      if (this.comboCols[col] && visible <= 1) return;   // keep ≥1 column
      var next = Object.assign({}, this.comboCols);
      next[col] = !next[col];
      this.comboCols = next;
    },
    comboColStyle: function (col) {
      if (!this.comboCols[col]) return 'display:none';
      // diagram is the grow column when visible; otherwise the first remaining visible column grows
      var growCol = this.comboCols.diagram ? 'diagram' : (this.comboCols.outline ? 'outline' : 'code');
      if (col === growCol) return 'flex:1 1 0; min-width:0';
      var pct = col === 'outline' ? this.comboOutlinePct : this.comboCodePct;
      return 'flex:0 0 ' + pct + '%; min-width:0';
    },
    /** drag the matrix sticky first column width (data-colresize grip in its header). Updates the
        --mcw CSS var on the scroll host live (no re-render churn), commits to state on mouse-up. The
        fixed width also stops the column from auto-resizing/janking as cells are added/unhidden. */
    matrixColDrag: function (e) {
      if (!e.target.closest('[data-colresize]')) return;
      e.preventDefault();
      var self = this;
      var host = e.currentTarget;             // the scroll container carrying --mcw
      var startX = e.clientX, startW = this.matrixColW, w = startW;
      var move = function (ev) {
        w = Math.max(90, Math.min(startW + (ev.clientX - startX), 520));
        host.style.setProperty('--mcw', w + 'px');
      };
      var up = function () {
        self.matrixColW = w;
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },
    // workbench audit search (fullscreen Scenario Coverage) — highlight matches; expand all so a hit in a
    // collapsed subtree is visible. Lighter than the main pane-search (no diff/cycle), reuses the matcher.
    scenarioAuditSearchToggle: function () {
      this.scenarioAuditSearch.open = !this.scenarioAuditSearch.open;
      if (!this.scenarioAuditSearch.open) this.scenarioAuditSearch.q = '';
    },
    scenarioAuditSearchRun: function () {
      if ((this.scenarioAuditSearch.q || '').trim()) this.scenarioAuditCollapsed = {};   // expand all so matches show
    },
    scenarioAuditMatch: function (row) {
      var q = this.scenarioAuditSearch.open && (this.scenarioAuditSearch.q || '').trim();
      return q && window.PaneSearch.match(row.title || '', q);
    },
    /** drag the ⤢ Scenario Coverage workbench split (the grid grows, the workbench is the fixed %) */
    startDragScenarioWb: function (e) {
      var self = this;
      var host = this.$refs.scenarioGridBody;
      if (!host) return;
      var rect = host.getBoundingClientRect();
      var move = function (ev) {
        self.scenarioWbPct = Math.max(18, Math.min(((rect.right - ev.clientX) / rect.width) * 100, 65));
      };
      var up = function () {
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },
    startDragCombo: function (e, which) {
      var self = this;
      var host = this.$refs.comboBody;
      if (!host) return;
      var rect = host.getBoundingClientRect();
      var move = function (ev) {
        if (which === 'outline') {
          self.comboOutlinePct = Math.max(12, Math.min(((ev.clientX - rect.left) / rect.width) * 100, 60));
        } else {
          self.comboCodePct = Math.max(15, Math.min(((rect.right - ev.clientX) / rect.width) * 100, 70));
        }
      };
      var up = function () {
        document.removeEventListener('mousemove', move);
        document.removeEventListener('mouseup', up);
      };
      document.addEventListener('mousemove', move);
      document.addEventListener('mouseup', up);
    },

    // ── Refresh + error handling ──
    refreshRule: async function () {
      var j = await ui('GET', '/rulebooks/' + this.rbName + (this.tableRowsLoaded ? '?tables=full' : ''));
      if (j && j.error) return this.handleError(j);
      this.rb = j;
      this.loadEdit();
      this.loadLookupsDraft();
      await this.syncDiagram();   // sha-keyed (D111e): refetches only when calc.js changed
      // the matrix is derived from calc + scenarios — invalidate it (reload if visible: the
      // scenario-coverage mini slot shows tableData too, so reload when either it or the ⤢ expand is up)
      if (this.tableData) {
        if (this.refSlot === 'scenario-coverage' || this.tableFull) this.loadTable(); else this.tableData = null;
      }
    },
    handleError: function (out) {
      if (out.__status === 409 || out.status === 409) {
        this.saveMessage = '409: file changed on disk — reloaded';
        this.saveError = true;
        this.refreshRule();
      } else {
        this.saveMessage = (out.code || out.__status || '') + ': ' + (out.error || 'error');
        this.saveError = true;
      }
    }
  };
}
