/*
 * karate-max paired-run report — the Alpine.js component behind ext/contract/pages/contract.html.
 * Reads window.KARATE_PAIR, which is VERBATIM what Contract.read returned (inlined as a <script> data
 * tag, so the page works off file:// and off a static site).
 *
 * The one rule this file obeys, and the reason it is thin: IT DERIVES NO CLAIM (D227f). Freshness, the
 * standing rung, the two halves of the proportion and every count come pre-computed from the verb that
 * owns them — a second implementation of what a pair means would eventually disagree with the first, and
 * the disagreement would surface as a claim nobody can attest. What is here is presentation only:
 * selecting, escaping, and the percentage rounding (floored, exactly as PairReport.pct does it, so the
 * page and the markdown never print two different numbers for one value).
 */
document.addEventListener('alpine:init', function () {
  Alpine.data('contractReport', function () {
    return {
      pair: window.KARATE_PAIR || {},
      expanded: {},
      filter: '',
      method: false,
      glossaryOpen: false,
      // the six outcome classes, in the order Divergence.CLASSES declares them (the vocabulary is the
      // engine's — this page never invents a seventh, and never quietly drops one that came back zero)
      classes: ['agreed', 'staleMock', 'mockBug', 'notassertedDivergence', 'agreedFailure', 'notmeasured'],

      // THE HEADLINE IS THE VERB'S. state, word, sentence and chips are minted by PairDigest and read
      // here — a verdict computed in the browser would be a second implementation of what a pair means
      // (D227f), and the markdown reader renders the identical block.
      get headline() { return this.pair.headline || {}; },
      get stateClass() {
        return { matched: 'k-sc-ready', diverged: 'k-sc-block', notmeasured: 'k-sc-conditional',
          withdrawn: 'k-sc-conditional' }[this.headline.state] || 'k-sc-conditional';
      },
      get subtitle() {
        var s = this.scope || {};
        if (this.pair.absent) return 'One suite, run against the mock this project ships and against the real provider.';
        if (!s.suite) return 'One suite ran twice: against the mock this project ships, and against the real provider.';
        return s.suite + ' ran against the mock this project ships, and against ' + (s.provider || 'the provider')
          + (s.providerEnv ? ' (' + s.providerEnv + ')' : '') + '.';
      },
      // the one caveat the wire cannot close — a line on the Evidence card, with the full disclosure in
      // Method and evidence rather than as the first paragraph a reader meets
      get identityCaveat() { return !this.pair.rehearsal && !!this.pair.rehearsalCheck; },

      get scope() { return this.pair.scope || {}; },
      get claim() { return this.pair.claim || {}; },
      get proportion() { return this.claim.measuredProportion || {}; },
      get coverage() { return this.claim.claimCoverage || {}; },
      get measured() { return this.claim.measured === true; },
      get specMismatch() { return this.claim.specMismatch || {}; },
      get noSpec() { return this.claim.noSpec || null; },
      get freshness() { return this.pair.freshness || {}; },
      get refresh() { return this.pair.refresh || {}; },
      get warnings() { return this.pair.warnings || {}; },
      get ignored() { return this.pair.ignored || {}; },
      get excluded() { return this.pair.excluded || {}; },
      get coverageGraph() { return this.pair.coverageGraph || {}; },
      get outcomes() { return this.pair.outcomes || {}; },
      get notMeasured() { return this.pair.notMeasured || []; },
      get karateSummary() { return this.pair.karateSummary || ''; },
      get rehearsal() { return !!this.pair.rehearsal; },
      get rehearsalNote() {
        var r = this.pair.rehearsal;
        return typeof r === 'string' ? r : (r && r.note)
          || 'the provider is itself a karate mock. Two instances of one mock agree by construction, so '
          + 'this run exercises the mechanism and is not evidence about a real provider';
      },

      // the LADDER, as the payload holds it. The page is written from the ARTIFACT (Contract.report
      // renders the full reading, not the summary), so every operation carries the bars it was asked.
      // THE RUNG THIS PAGE SHOWS IS THE STANDING ONE (`pair.rung`) — derived at read time, and WITHDRAWN
      // on a decayed pair with the prose that described it as in force stripped off. `claim.rungs.aggregate`
      // is the MINT-TIME rung and is only a fallback for a payload carrying no standing projection at all;
      // reaching for it on a decayed pair would put the withdrawn claim back in the one slot a reader
      // quotes, which is the defect the verb was fixed for and the renderer is the obvious place to
      // re-introduce it.
      get standing() { return this.pair.rung || this.aggregate; },
      get withdrawn() { return !!(this.pair.rung && this.pair.rung.withdrawnBy); },
      get rungs() { return (this.claim.rungs) || {}; },
      get aggregate() { return this.rungs.aggregate || {}; },
      get items() { return Array.isArray(this.rungs.items) ? this.rungs.items : []; },
      rungName: function (n) {
        return ['', 'shape-conformant', 'verified-against-mock', 'verified-against-provider',
          'proven-substitutable'][n] || '';
      },
      // …and a rung of null is NOT MEASURED, which takes no failure tint: "no verdict" is not "bad", the
      // same rule the outcome tiles obey.
      rungClass: function (rung) {
        if (rung === null || rung === undefined) return 'k-tagpill';
        return rung >= 4 ? 'k-ok' : rung >= 3 ? 'k-link' : rung >= 2 ? 'k-warn' : 'k-no';
      },
      toggle: function (id) { this.expanded[id] = !this.expanded[id]; },

      get freshClass() {
        return { fresh: 'k-fresh-ok', stale: 'k-fresh-no', expired: 'k-fresh-no',
          unbindable: 'k-fresh-no', undatable: 'k-fresh-no' }[this.freshness.status] || 'k-fresh-warn';
      },

      // the divergence set, with the compared values on every difference (the artifact's own rows).
      get divergences() { return this.pair.divergences || []; },
      get shown() {
        var f = this.filter;
        return this.divergences.filter(function (d) { return !f || d.outcome === f; });
      },

      // a divergence row's PILL — every class gets a colour, including the neutral one
      outcomeClass: function (c) {
        return { agreed: 'k-ok', agreedFailure: 'k-warn', notmeasured: 'k-tagpill',
          staleMock: 'k-no', mockBug: 'k-no', notassertedDivergence: 'k-warn' }[c] || 'k-tagpill';
      },
      // a COUNT tile's tint — deliberately not the pill map: `notmeasured` takes no tint at all, because a
      // filled tile reads as a selected one, and this strip is also the filter
      outcomeTile: function (c) {
        return { agreed: 'k-ok', agreedFailure: 'k-warn',
          staleMock: 'k-no', mockBug: 'k-no', notassertedDivergence: 'k-warn' }[c] || '';
      },
      // THE LABELS TRANSLATE THE MODEL, they do not rename it: the payload keys stay `staleMock`,
      // `notassertedDivergence`, `notexercised`, and a reader who wants them has them in the artifact and
      // in the tile's own tooltip. An identifier shown as a label makes a reader learn a taxonomy before
      // they learn anything.
      label: function (c) {
        return { agreed: 'matched', staleMock: 'mock is out of date', mockBug: 'mock is wrong',
          notassertedDivergence: 'difference the suite does not check',
          agreedFailure: 'both legs failed', notmeasured: 'not compared' }[c] || c;
      },
      // which classes are actually IN `divergences` — the only ones a filter over it can select. The other
      // three are counted from the same source but live elsewhere, so their tiles do not pretend to filter.
      filterable: function (c) {
        return c === 'staleMock' || c === 'mockBug' || c === 'notassertedDivergence';
      },
      // the tooltip keeps the MODEL's identifier beside the reader's label, so translating the surface
      // never costs a reader the vocabulary the artifact and the skill use
      outcomeHelp: function (c) {
        return c + ' — ' + {
          agreed: 'identical on both legs, at the verdict layer AND the response layer',
          staleMock: 'the mock passed and the provider failed. The mock is behind the provider',
          mockBug: 'the provider passed and the mock failed',
          notassertedDivergence: 'both legs passed and the responses differ. The suite does not assert '
            + 'the field that differs',
          agreedFailure: 'both legs failed. That is agreement about behaviour, not a finding about the mock',
          notmeasured: 'no verdict about behaviour was produced. Causes: an environment failure, a '
            + 'skipped leg, a failed precondition probe, a read over ambient state, or a divergence that '
            + 'did not reproduce'
        }[c] || '';
      },

      /**
       * A proportion as a percentage — ROUNDED DOWN except at a true 1.0, the same arithmetic
       * PairReport.pct does. Ordinary rounding would print "100%" for 199 divergence-free operations out
       * of 200, which is the one number this page may not overstate.
       */
      pct: function (value) {
        if (typeof value !== 'number') return 'not measured';
        return (value >= 1 ? 100 : Math.min(99, Math.floor(value * 100))) + '%';
      },

      // ---- the glossary ----
      // PAGE-OWNED DATA, and deliberately: it defines the words the artifact uses, it does not compute a
      // number from the payload, so D227f is not in play. It is static so the page still explains itself
      // off file:// with no server to ask — which is how most readers meet it (a published CI report).
      openGlossary: function (key) {
        this.glossaryOpen = true;
        var self = this;
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
          key: 'setup', title: 'What is being compared',
          intro: 'One suite runs twice in one session — once against your mock, once against the real '
            + 'provider — and the run reports every place the two answers differed.',
          terms: [
            { t: 'provider', d: 'The real service. A URL you hand in: a deployed environment, a staging box, a local build.' },
            { t: 'mock', d: 'The stand-in your project ships to consuming teams. Name it, or the harness synthesizes one from your spec — which agrees with spec-shaped assertions by construction, and is a different subject from the mock you ship. A pair minted without a spec must name its mock: there is no document to synthesize one from.' },
            { t: 'leg', d: 'One of the two runs: the mock leg and the provider leg. Same suite, same scenario order, one session.' },
            { t: 'paired run', d: 'The two legs together, plus the difference set they produce. One pair id is stamped at the top and both legs carry it.' },
            { t: 'operation', d: 'One endpoint in your contract — the unit everything here is counted in (GET /policies/{id}, and so on).' }
          ]
        },
        {
          key: 'numbers', title: 'The two numbers',
          intro: 'Neither means anything alone. The report always shows them together.',
          terms: [
            { t: 'declared', d: 'Every operation your contract declares. The denominator of scope.' },
            { t: 'exercised', d: 'An operation this run actually sent at least one request to.' },
            { t: 'admissible', d: 'An exercised operation that produced a verdict. The denominator of the proportion.' },
            { t: 'divergence-free', d: 'An admissible operation where the two legs did not differ.' },
            { t: 'not tested', d: 'A declared operation this run never exercised. It is not measured, and never substitutable.' }
          ]
        },
        {
          key: 'outcomes', title: 'What each scenario came back as',
          intro: 'Every scenario lands in exactly one class. Three of them are findings.',
          terms: [
            { t: 'matched', cls: 'k-ok', d: 'Identical on both legs, at the verdict layer and at the response layer. (agreed)' },
            { t: 'mock is out of date', cls: 'k-no', d: 'The mock passed and the provider failed. The mock is behind the provider. (staleMock)' },
            { t: 'mock is wrong', cls: 'k-no', d: 'The provider passed and the mock failed. (mockBug)' },
            { t: 'difference the suite does not check', cls: 'k-warn', d: 'Both legs passed and the responses still differ. A finding about your suite: it does not assert the field that differs. (notassertedDivergence)' },
            { t: 'both legs failed', cls: 'k-warn', d: 'Both legs failed. Agreement about behaviour, not a finding about the mock. (agreedFailure)' },
            { t: 'not compared', cls: 'k-tagpill', d: 'No verdict was produced: an environment failure, a skipped leg, an unequal starting state, a read over ambient data, or a difference that did not reproduce. It is not zero and it is not agreement. (notMeasured)' }
          ]
        },
        {
          key: 'rung', title: 'The rung — what the evidence lets you say',
          intro: 'Four cumulative rungs. Each holds only if every rung below it holds, and each is a '
            + 'counted statement with the evidence for every bar under it. The whole-artifact rung is the '
            + 'floor of the per-operation rungs, and it is capped below the top while any declared '
            + 'operation is not tested.',
          terms: [
            { t: '1 shape-conformant', cls: 'k-no', d: 'Your contract declares this operation. Nothing is claimed about its behaviour.' },
            { t: '2 verified-against-mock', cls: 'k-warn', d: 'A suite exercised it against the mock, and the mock leg passed. This is what an everyday per-commit run already earns.' },
            { t: '3 verified-against-provider', cls: 'k-link', d: 'The provider leg exercised it, sent its requests to the target it was given, and passed. It does not by itself say a real provider answered — a rehearsal pair reaches this level too, and is stopped here. What answered is the rehearsal bar.' },
            { t: '4 proven-substitutable', cls: 'k-ok', d: 'The two legs were compared and did not differ, at both layers, under conditions strict enough for the claim. This is the claim consumers care about: the mock can stand where the provider stood.' },
            { t: 'bar', d: 'One counted condition a rung rests on. A bar whose only evidence is that nothing refused it is not evidence: 0 of 0 reads not checked, never passed.' }
          ]
        },
        {
          key: 'waiver', title: 'Waivers and disclosures',
          terms: [
            { t: 'ignore rule', d: 'A declared waiver for an expected difference — surrogate ids, timestamps. It carries a reason and an owner, it re-classifies rather than hides (both values are kept), and it is marked stale when it covered nothing. Nothing is ignored by default.' },
            { t: 'data profile', d: 'seeded — the suite creates everything it needs through the API, so the mock starts empty by construction; the only profile that reaches rung 4. fixture — both legs are pre-loaded from one declared file, whose digest becomes a freshness binding. ambient — whatever data the two targets happen to hold, where a data difference reads as a behaviour difference.' },
            { t: 'rehearsal', cls: 'k-warn', d: 'A karate mock answered the provider leg. Two instances of one mock agree by construction, so the run exercises the mechanism and is not evidence about a real provider.' },
            { t: 'draft', cls: 'k-warn', d: 'Minted with {draft:true} — exploratory, not project evidence, whatever directory it sits in.' }
          ]
        },
        {
          key: 'freshness', title: 'Freshness — evidence decays',
          intro: 'The pair is a committed file and is never rewritten, so freshness is calculated when '
            + 'you read it. The same bytes can read fresh today and expired next month. Anything other '
            + 'than fresh withdraws the rung rather than lowering it.',
          terms: [
            { t: 'fresh', cls: 'k-ok', d: 'Every binding still matches, and the evidence is inside its maximum age.' },
            { t: 'stale', cls: 'k-no', d: 'A binding changed. The bindings are the suite, the mock, the spec, the data profile, any declared fixture, and the rulebooks — editing a fixture decays a pair exactly as editing the spec does. This evidence is about a different subject than the one you have now.' },
            { t: 'expired', cls: 'k-no', d: 'Nothing visible changed, but the evidence is older than its maximum age.' },
            { t: 'unbindable', cls: 'k-no', d: 'The pair records no binding digests, so nothing can check what it is evidence about.' },
            { t: 'undatable', cls: 'k-no', d: 'Its execution time is unreadable, so its age can never be established.' }
          ]
        }
      ],

      /** A compared value, escaped — never injected, and an absent one says so rather than reading blank. */
      cell: function (value) {
        if (value === null || value === undefined) return '<span class="k-muted">absent</span>';
        // an object or array arrives VERBATIM for short values (Divergence.elide only stringifies the
        // long ones), and String() turned both sides of a real difference into "[object Object]"
        var raw = (value !== null && typeof value === 'object') ? JSON.stringify(value) : String(value);
        var s = raw.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return '<code>' + s + '</code>';
      }
    };
  });
});
