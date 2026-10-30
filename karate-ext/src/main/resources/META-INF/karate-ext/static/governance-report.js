/*
 * karate-max governance report — the Alpine.js component behind ext/governance/pages/governance.html.
 * Reads window.KARATE_GOVERNANCE, which is VERBATIM what Openapi.grade returned (inlined as a <script>
 * data tag, so the page works off file:// and off a static site).
 *
 * The one rule this file obeys, and the reason it is thin: IT DERIVES NO CLAIM (D227f). The verdict word,
 * its sentence, the chips, every dimension score, the cap, the index and the maturity level come
 * pre-computed from the verb that owns them. What is here is presentation only: selecting, filtering,
 * escaping, and the percentage rounding the markdown renderer already owns.
 */
document.addEventListener('alpine:init', function () {
  Alpine.data('governanceReport', function () {
    return {
      grade: window.KARATE_GOVERNANCE || {},
      method: false,
      glossaryOpen: false,
      filter: '',
      // the severities in the order a reader triages them — the engine's own vocabulary, and the page
      // never invents a fourth or hides one that came back zero
      severities: ['error', 'warn', 'info'],

      get headline() { return this.grade.headline || {}; },
      get lint() { return this.grade.lint || {}; },
      get summary() { return this.lint.summary || {}; },
      get graph() { return this.lint.graph || {}; },
      get level() { return this.grade.level || {}; },
      get weights() { return this.grade.weights || {}; },
      get waived() { return this.grade.waived || {}; },
      get rehearsal() { return this.grade.rehearsal || {}; },
      get judged() { return this.grade.judged || {}; },
      get scoring() { return this.grade.scoring || {}; },
      get findings() { return this.lint.findings || []; },
      get waivers() { return this.lint.waivers || []; },

      get subtitle() {
        if (this.grade.absent) return 'The deterministic grade of this project\'s API document.';
        return (this.grade.spec || 'this API document')
          + ' graded against ' + (this.summary.rules || 0) + ' rules (deterministic evaluation).';
      },
      get stateClass() {
        return { clean: 'k-sc-ready', waived: 'k-sc-conditional', issues: 'k-sc-conditional',
          failing: 'k-sc-block', blocked: 'k-sc-block' }[this.headline.state] || 'k-sc-conditional';
      },
      // the verdict is the VERB's, read not re-derived — the page only spells it for a human
      get verdictWord() { return String(this.grade.verdict || '—').replace('_', ' '); },
      get verdictClass() {
        return { READY: 'k-v-ok', CONDITIONAL: 'k-v-warn', NOT_READY: 'k-v-no' }[this.grade.verdict] || '';
      },

      // the dimensions as a list, in the payload's own order
      get dimensions() {
        var d = this.grade.dimensions || {};
        return Object.keys(d).map(function (k) {
          var row = d[k] || {};
          return {
            key: k, label: row.label || k, measured: row.measured === true, score: row.score,
            weight: row.weight, effectiveWeight: row.effectiveWeight, rules: row.rules,
            gradedRules: row.gradedRules, ungradedRules: row.ungradedRules, cappedAt: row.cappedAt,
            cappedBy: row.cappedBy, uncappedScore: row.uncappedScore, note: row.note,
            // the disclosures the verb attaches to two specific rows: `contract` explains that its score
            // IS a paired run's rung normalised, and `provenness` discloses what its number does NOT
            // carry. Both were minted and neither was rendered.
            carries: row.carries
          };
        });
      },

      // READ, never tallied. LintEngine excludes a waived finding from its severity counts, so counting
      // rows here produced a number the verb disagreed with — a red `2 error` tile under a
      // "CLEAN, ON WAIVERS" headline. The net counts are already in the payload (D227f).
      get counts() {
        var s = this.summary;
        return { error: s.errors || 0, warn: s.warnings || 0, info: s.infos || 0 };
      },
      get shown() {
        var f = this.filter;
        return this.findings.filter(function (x) { return !f || x.severity === f; });
      },

      severityTile: function (s) { return { error: 'k-no', warn: 'k-warn', info: '' }[s] || ''; },
      severityClass: function (s, waived) {
        if (waived) return 'k-tagpill';
        return { error: 'k-no', warn: 'k-warn', info: 'k-link' }[s] || 'k-tagpill';
      },
      severityHelp: function (s) {
        return {
          error: 'error — it blocks the ship verdict until it is fixed or waived, and it weighs the '
            + 'most against its dimension (weight 3)',
          warn: 'warn — it does not block the verdict. It lowers its dimension (weight 2)',
          info: 'info — the mildest finding. It does not block the verdict, and it still lowers its '
            + 'dimension (weight 1)'
        }[s] || '';
      },
      /** The line, with the marker a rule graded against the $ref-resolved view earns: its position is the
       *  reference site, not the definition, and a reader is owed that rather than a subtly wrong number. */
      where: function (f) {
        if (f.line == null) return '';
        var exact = !f.position || f.position.exact !== false;
        return f.line + ':' + (f.col == null ? '' : f.col) + (exact ? '' : ' (≈)');
      },
      scoreClass: function (v) {
        if (typeof v !== 'number') return 'k-tagpill';
        return v >= 0.9 ? 'k-ok' : v >= 0.7 ? 'k-warn' : 'k-no';
      },

      /** A proportion as a percentage — floored except at a true 1.0, the arithmetic the markdown owns. */
      pct: function (value) {
        if (typeof value !== 'number') return 'not measured';
        return (value >= 1 ? 100 : Math.min(99, Math.floor(value * 100))) + '%';
      },
      pctOf: function (value) { return typeof value === 'number' ? Math.round(value * 100) : 0; },

      // ---- the glossary ----
      // PAGE-OWNED DATA: it defines the words the grade uses, it computes nothing from the payload, and it
      // is static so the page still explains itself off a published CI artifact with no server to ask.
      openGlossary: function (key) {
        this.glossaryOpen = true;
        this.$nextTick(function () {
          var el = document.getElementById('gloss-' + key);
          if (el) {
            el.scrollIntoView({ block: 'start' });
            el.classList.add('k-gloss-hi');
            setTimeout(function () { el.classList.remove('k-gloss-hi'); }, 1400);
          }
        });
      },

      glossary: [
        {
          key: 'verdict', title: 'The ship verdict',
          intro: 'Decided by gates, never by the index. A gate is a fact that settles the verdict on its '
            + 'own and is never folded into a number, so the number stays something you can re-derive.',
          terms: [
            { t: 'READY', cls: 'k-ok', d: 'No gate is open, nothing stands — no finding AND no waiver — and every dimension had evidence to grade. An artifact that passes only because something was waived is not READY.' },
            { t: 'CONDITIONAL', cls: 'k-warn', d: 'No gate is open, but a finding stands, a waiver was applied, or a dimension had nothing to measure. READY needs every dimension measured, so missing evidence holds the verdict here however clean the document is.' },
            { t: 'NOT READY', cls: 'k-no', d: 'A gate is open. There are five: an unwaived error, a refused waiver, a bound cap, a rule that failed to evaluate, and a project policy file that would not run.' },
            { t: 'gate', d: 'A fact that decides the verdict by itself and never enters the index. A rule that could not run is one, because a grade computed around it is a grade over an unknown. A bound cap is one too — so a warn-severity rule that caps still yields NOT READY.' }
          ]
        },
        {
          key: 'index', title: 'The index',
          intro: 'One number over the dimensions that had evidence — the weighted HARMONIC mean, after '
            + 'each cap has bound its dimension. Harmonic on purpose: one measured dimension at zero takes '
            + 'the index to zero whatever the weights say. Order: score, cap, mean, gate, ladder.',
          terms: [
            { t: 'score', d: 'Per dimension: the proportion of the subjects a rule actually looked at that passed — failing operations over operations, not findings counted up. A large tidy API therefore does not grade below a small sloppy one.' },
            { t: 'cap', d: 'A rule saying "however good everything else is, this dimension is not above X". A cap binds before the mean sees the score, and the uncapped score is shown beside it.' },
            { t: 'weight', d: 'Policy, not a product constant. The declared weight and the effective weight are both shown; the effective one is renormalised over the measured dimensions only, and the renormalisation is disclosed rather than silent.' },
            { t: 'not measured', cls: 'k-tagpill', d: 'No rule graded that dimension against this document. It is never zero and never quietly dropped: zero would read as a score you earned.' }
          ]
        },
        {
          key: 'level', title: 'The maturity ladder',
          intro: 'A named level, climbed in order. Each is cumulative AND evidence-gated: it is awarded '
            + 'only when every level below it holds and the dimension that defines it was measured. An '
            + 'uninstrumented project tops out at Governed and is told why.',
          terms: [
            { t: 'Ungoverned', cls: 'k-no', d: 'A gate is open, or documentation scores below 70%.' },
            { t: 'Described', cls: 'k-warn', d: 'Documentation is at or above 70% — the document describes itself.' },
            { t: 'Governed', cls: 'k-warn', d: 'It also passes the style and security floor.' },
            { t: 'Traced', cls: 'k-link', d: 'Its operations are also claimed by requirements.' },
            { t: 'Exercised', cls: 'k-link', d: 'A run also exercised them, and not only against our own mock.' },
            { t: 'Proven', cls: 'k-ok', d: 'A paired run also shows the mock can stand in for the provider.' }
          ]
        },
        {
          key: 'dimensions', title: 'The five dimensions',
          terms: [
            { t: 'design', d: 'The style-guide ground: naming, path shape, operation ids, declared parameters, response shapes.' },
            { t: 'security', d: 'Security HYGIENE over the document — is auth declared at all, does a key ride in a query string, are collections bounded. It is not a runtime posture grade, and reading it as one would be a mistake.' },
            { t: 'documentation', d: 'Whether the document describes itself: owners, summaries, descriptions, examples that validate against their own schema.' },
            { t: 'provenness', d: 'Whether anything actually ran against these operations. It reads `not measured` without a coverage graph, rather than scoring full marks for never asking.' },
            { t: 'contract', d: 'The depth of contract testing: the rung a recorded paired run earns, normalised. Only a pair minted against THIS document counts.' }
          ]
        },
        {
          key: 'severity', title: 'Finding severities',
          terms: [
            { t: 'error', cls: 'k-no', d: 'Blocks the ship verdict until it is fixed or waived — an unwaived error is a gate. It also weighs the most against its dimension score (weight 3).' },
            { t: 'warn', cls: 'k-warn', d: 'Does not block the verdict. It lowers the dimension it belongs to (weight 2).' },
            { t: 'info', cls: 'k-link', d: 'The mildest finding — an observation rather than a defect. It does not block the verdict, and it still lowers its dimension (weight 1). It is not free: three of them weigh about what one error does.' },
            { t: 'points at', d: 'The JSON pointer the finding lands on, with the line and column in the source. A rule whose selector cannot pin a position says nothing rather than guessing: a wrong line is worse than none.' }
          ]
        },
        {
          key: 'waiver', title: 'Waivers',
          intro: 'A waiver is a signed exception, not a fix. It is reported, never folded into a count: an '
            + 'API that passes with nine waivers is not the same artifact as one that passes clean.',
          terms: [
            { t: 'applied', cls: 'k-warn', d: 'The waiver held, and the findings it covers are marked waived rather than removed.' },
            { t: 'refused', cls: 'k-no', d: 'The waiver was rejected — it is incomplete, or it names a rule that does not exist. It grants nothing, so the rule it names still applies.' },
            { t: 'owner', d: 'Who signed it. A waiver with no owner is refused, because an exception nobody owns is one nobody will revisit.' }
          ]
        }
      ]
    };
  });
});
