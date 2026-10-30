/*
 * karate-max standalone coverage report — the Alpine.js component doing the reactive
 * heavy lifting (filter by source/status/text, expandable drill-down). Reads the karate-trace/v1
 * graph from window.KARATE_COVERAGE_DATA (inlined as a <script> data tag, so it works off file://,
 * loaded from disk and never over the network). Vanilla helpers where they read clearer.
 */
document.addEventListener('alpine:init', function () {
  Alpine.data('coverageReport', function () {
    return {
      data: window.KARATE_COVERAGE_DATA || {},
      q: '',
      sourceFilter: '',
      statusFilter: '',
      expanded: {},

      // Per-scenario `rules` rows are removed from the report (the arm — not the scenario — is the
      // coverable unit; see the Rule Coverage section below). Hide the whole `rules` SOURCE from the
      // top strip too, so no stray per-scenario tile lingers. Its graph data stays intact for Rule.cover.
      get sources() { return (this.data.sources || []).filter(function (s) { return (s.namespace || s.type) !== 'rules'; }); },
      get allItems() { return (this.data.items || []).filter(function (i) { return i.kind !== 'rule-scenario' && i.source !== 'rules'; }); },
      get hits() { return this.data.hits || []; },
      get karateSummary() { return this.data.karateSummary || ''; },

      // Rule Coverage (MODEL §Glossary/§6) — the decision ARM is the coverable item (the analog of an API
      // operation): per rulebook, every arm with its status (used · notused · notreached) + scenario count.
      // The % is over the FULL arm universe (total), so a dead/unreached arm honestly caps coverage below
      // 100% (fix = remove the dead branch). Its own section, never conflated with the requirement RTM rows.
      get ruleCoverage() { return this.data.ruleCoverage || []; },
      // %-used over ALL of a rulebook's arms (the full universe, incl. the unreachable ones).
      rulePct: function (r) { return r && r.total ? Math.round((r.used || 0) * 100 / r.total) : 0; },
      ruleBarClass: function (r) { var p = this.rulePct(r); return p >= 100 ? 'k-ok' : (p > 0 ? 'k-warn' : 'k-no'); },
      // arms ordered for display: the gaps first (NOT USED, then NOT REACHED), then USED — the eye lands on
      // what's missing; within a status, by source line, true-arm before false-arm.
      ruleArms: function (r) {
        var rank = { notused: 0, notreached: 1, used: 2 };
        return (r.arms || []).slice().sort(function (a, b) {
          var d = (rank[a.status] === undefined ? 9 : rank[a.status]) - (rank[b.status] === undefined ? 9 : rank[b.status]);
          if (d !== 0) return d;
          if ((a.line || 0) !== (b.line || 0)) return (a.line || 0) - (b.line || 0);
          return (a.outcome === b.outcome) ? 0 : (a.outcome ? -1 : 1);
        });
      },
      armBadge: function (s) { return s === 'used' ? 'CHECKED' : s === 'notused' ? 'NOT CHECKED' : 'UNREACHABLE'; },
      armClass: function (s) { return s === 'used' ? 'k-arm-used' : s === 'notused' ? 'k-arm-notused' : 'k-arm-notreached'; },
      // how many arms carry no calc.label — the report doubles as a "where a calc.label would read clearer" guide.
      ruleUnlabeled: function (r) { return (r.arms || []).filter(function (a) { return !a.label; }).length; },

      // ── Confidence-to-ship banner (Part 1) ────────────────────────────────────────────────────────
      // The CANONICAL release-readiness verdict, stamped into the data global by the Java bake
      // (Requirement.readinessFor → RequirementReadiness) — the SAME verdict the traceability report
      // shows. Never re-derived here (the req items lack `criticality`); null (no requirements) ⇒ no banner.
      get readiness() { return this.data.readiness || null; },
      // whether a sibling Traceability report exists to link to (stamped by the Java bake when the project
      // has requirements, so both reports are part of the same deliverable) — gates the cross-link + nav tab.
      get hasTraceability() { return !!this.data.hasTraceability; },
      // the relative href to the sibling Traceability page — context-dependent (the in-run page nests under
      // ext/coverage/pages/, the standalone sits flat), so the Java bake stamps the right path per surface;
      // this keeps the report BODY identical across both page shells (one renderer, D-unify).
      get traceabilityHref() { return this.data.traceabilityHref || '../../traceability/pages/traceability.html'; },
      get readyState() { var r = this.readiness; return r ? (r.state || (r.ready ? 'READY' : 'NOT_READY')) : ''; },
      // a plain-language headline for the auditor (not the raw READY/CONDITIONAL/NOT_READY token)
      readyWord: function () {
        return { READY: 'Ready to ship', CONDITIONAL: 'Ship with caution', NOT_READY: 'Not ready to ship' }[this.readyState] || '';
      },
      readyClass: function () {
        return { READY: 'k-sc-ready', CONDITIONAL: 'k-sc-conditional', NOT_READY: 'k-sc-block' }[this.readyState] || '';
      },
      // the canonical one-line reason (kept verbatim — single source with the RTM verdict)
      readyVerdict: function () { return (this.readiness && this.readiness.verdict) || ''; },
      // covered, but only the rulebook vouches (§2c) — surfaced beside the split, never folded into green
      get oracleOnlyCount() {
        return ((this.readiness && this.readiness.requirements) || [])
          .filter(function (r) { return (r.oracleOnly || r.refusalOnly || r.modelOnly) && r.coverage === 'COVERED'; }).length;
      },
      // the narrowest of those (D307): the only evidence is the shape refusing what the criterion named.
      // A subset of the count above — never its own segment, or the green would be counted twice.
      get refusalOnlyCount() {
        return ((this.readiness && this.readiness.requirements) || [])
          .filter(function (r) { return r.refusalOnly && r.coverage === 'COVERED'; }).length;
      },
      // covered, but every graded passing test asserts nothing (D311) — its own hollow-green flavour,
      // split out of the green beside the rules-only one so the chips can never sum past the total
      get notassertedCount() {
        return ((this.readiness && this.readiness.requirements) || [])
          .filter(function (r) { return r.notasserted && !r.oracleOnly && !r.refusalOnly && !r.modelOnly && r.coverage === 'COVERED'; }).length;
      },
      // the assertion-strength census riding the result (D311); absent ⇒ the line is not rendered
      get assertionStrength() {
        return this.data.assertionStrength
          || (this.readiness && this.readiness.assertionStrength)
          || (this.data.runEvidence && this.data.runEvidence.assertionStrength) || null;
      },
      strengthLine: function () {
        var a = this.assertionStrength;
        return a ? (a.graded || 0) + ' graded · ' + (a.ungraded || 0) + ' ungraded · ' + (a.notasserted || 0) + ' notasserted' : '';
      },
      // the honest status split behind the verdict — covered / failing / not-tested, never blended
      readyStatusCounts: function () {
        var c = { COVERED: 0, FAILING: 0, NOTRUN: 0, NOTCOVERED: 0 };
        ((this.readiness && this.readiness.requirements) || []).forEach(function (r) {
          if (c[r.coverage] !== undefined) c[r.coverage]++;
        });
        return c;
      },

      // ── Coverage scorecard (Part 2) ───────────────────────────────────────────────────────────────
      // Every coverage axis as its own card with a STATUS-SPLIT count (never one blended %), ordered by
      // what an auditor deciding to ship weighs most. Three distinct axes (MODEL §Glossary): requirement
      // coverage · Rule Coverage · protocol coverage shown at its three §2h depths (actions / values /
      // combinations). A card renders only when its axis has data. Segment class → colour: ok green,
      // bad red (failing), none grey (untested / missing / not-reached). `good`/`total` drive the headline %.
      // operations = real items that aren't requirements (an openapi op, grpc method, kafka topic, …)
      get operationItems() {
        return this.listItems.filter(function (i) { return i.kind !== 'req'; });
      },
      statusSplit: function (items) {
        var c = { COVERED: 0, FAILING: 0, NOTRUN: 0, NOTCOVERED: 0 };
        items.forEach(function (i) { if (c[i.status] !== undefined) c[i.status]++; });
        return c;
      },
      // Σ a per-source rollup field ({covered,total} or {covered,required}) across all non-rules sources
      sumSource: function (key, num, den) {
        var s = 0, t = 0;
        (this.sources || []).forEach(function (src) {
          var b = src[key];
          if (b) { s += (b[num] || 0); t += (b[den] || 0); }
        });
        return { covered: s, total: t };
      },
      // how many input axes have NO gradeable universe (open — a field with no declared allowed-values)
      get openAxisCount() {
        var n = 0;
        this.dimensions.forEach(function (d) {
          if (!d.axes) return;
          Object.keys(d.axes).forEach(function (k) {
            if (k !== 'response' && d.axes[k] && d.axes[k].closed === false) n++;
          });
        });
        return n;
      },
      // the ordered scorecard cards (only those with data)
      axisCards: function () {
        var cards = [];
        var mk = function (key, label, ask, help, total, segs) {
          var good = segs.length ? segs[0].n : 0;
          return { key: key, label: label, ask: ask, help: help, total: total,
                   pct: total ? Math.round(good * 100 / total) : 0, segments: segs };
        };
        // 1) Requirements — the auditor's first question. Count LEAF requirements (the canonical readiness
        // rows), so the card agrees with the ship banner (a parent epic is not itself a coverable leaf).
        var reqRows = (this.readiness && this.readiness.requirements) || null;
        var rc = reqRows
          ? reqRows.reduce(function (a, r) { if (a[r.coverage] !== undefined) a[r.coverage]++; return a; },
              { COVERED: 0, FAILING: 0, NOTRUN: 0, NOTCOVERED: 0 })
          : this.statusSplit(this.listItems.filter(function (i) { return i.kind === 'req'; }));
        // covered, but only the rulebook vouches — split out of the green so the card can't read
        // greener than the evidence (the readiness row carries the flag)
        // only ever split out of the GREEN segment, so the segments can never sum past the total
        var reqOracleOnly = reqRows
          ? this.oracleOnlyCount
          : this.listItems.filter(function (i) { return i.kind === 'req' && (i.oracleOnly || i.refusalOnly || i.modelOnly) && i.status === 'COVERED'; }).length;
        var reqRefusalOnly = reqRows
          ? this.refusalOnlyCount
          : this.listItems.filter(function (i) { return i.kind === 'req' && i.refusalOnly && i.status === 'COVERED'; }).length;
        var reqNotasserted = reqRows
          ? this.notassertedCount
          : this.listItems.filter(function (i) { return i.kind === 'req' && i.notasserted && !i.oracleOnly && !i.refusalOnly && !i.modelOnly && i.status === 'COVERED'; }).length;
        var reqTotal = reqRows ? reqRows.length : this.listItems.filter(function (i) { return i.kind === 'req'; }).length;
        if (reqTotal) {
          cards.push(mk('req', 'Requirements', "requirements we've actually tested", 'model.coverage.axis.req',
            reqTotal, [
              { cls: 'k-seg-ok', n: Math.max(0, rc.COVERED - reqOracleOnly - reqNotasserted), title: 'covered (tested & passed)' },
              { cls: 'k-seg-sim', n: reqOracleOnly, title: 'the rules realize it, but nothing outside the rulebook checked it'
                  + (reqRefusalOnly ? ' — ' + reqRefusalOnly + ' of them only because the shape refuses what it must' : '') },
              { cls: 'k-seg-sim', n: reqNotasserted, title: 'covered by tests that assert nothing — a lock on a value or shape clears it' },
              { cls: 'k-seg-bad', n: rc.FAILING, title: 'a test is failing' },
              { cls: 'k-seg-none', n: rc.NOTRUN + rc.NOTCOVERED, title: 'not tested yet' }
            ]));
        }
        // 2) Rule branches — the yes/no paths of the business rules
        var rcov = this.ruleCoverage;
        if (rcov.length) {
          var used = 0, tot = 0, reach = 0;
          rcov.forEach(function (r) { used += (r.used || 0); tot += (r.total || 0); reach += (r.reachable || 0); });
          cards.push(mk('rules', 'Rule branches', "yes/no paths in our rules we've checked", 'model.coverage.axis.rules',
            tot, [
              { cls: 'k-seg-ok', n: used, title: 'checked by a saved scenario' },
              { cls: 'k-seg-none', n: reach - used, title: 'reachable but no scenario checks it — add data' },
              { cls: 'k-seg-bad', n: tot - reach, title: 'never reached — dead branch or unbuildable input' }
            ]));
        }
        // 3) Actions tested — the things the system does (protocol operations)
        var ops = this.operationItems;
        if (ops.length) {
          var oc = this.statusSplit(ops);
          cards.push(mk('actions', 'Actions tested', "system actions we've exercised", 'model.coverage.axis.actions',
            ops.length, [
              { cls: 'k-seg-ok', n: oc.COVERED, title: 'exercised & passed' },
              { cls: 'k-seg-bad', n: oc.FAILING, title: 'a test is failing' },
              { cls: 'k-seg-none', n: oc.NOTRUN + oc.NOTCOVERED, title: 'not exercised' }
            ]));
        }
        // 4) Field values — meaningfully-different inputs we tried (marginal input adequacy, §2h)
        var inp = this.sumSource('inputs', 'covered', 'total');
        var openAx = this.openAxisCount;
        if (inp.total || openAx) {
          cards.push(mk('inputs', 'Field values', 'meaningfully-different input values we tried', 'model.coverage.axis.inputs',
            inp.total, [
              { cls: 'k-seg-ok', n: inp.covered, title: 'value tried' },
              { cls: 'k-seg-none', n: inp.total - inp.covered, title: 'value never tried' }
            ]));
          cards[cards.length - 1].note = openAx ? (openAx + (openAx === 1 ? ' field' : ' fields') + " can't be scored yet") : '';
        }
        // 5) Risky combinations — value combinations we tried together (cross coverage, §2h)
        var cmb = this.sumSource('combos', 'covered', 'required');
        if (cmb.total) {
          cards.push(mk('combos', 'Risky combinations', 'risky value combinations we tried together', 'model.coverage.axis.combos',
            cmb.total, [
              { cls: 'k-seg-ok', n: cmb.covered, title: 'combination tried' },
              { cls: 'k-seg-none', n: cmb.total - cmb.covered, title: 'combination not tried' }
            ]));
        }
        // 6) Error paths — the sad-path grade (D228b): of the actions exercised at all, which ever
        // produced a failure outcome? A happy-only item is "covered" by every other measure while its
        // declared error behavior has never once been witnessed.
        var ep = this.sumSource('errorPaths', 'covered', 'total');
        if (ep.total) {
          cards.push(mk('errors', 'Error paths', 'actions whose failure modes we exercised', 'model.coverage.axis.errors',
            ep.total, [
              { cls: 'k-seg-ok', n: ep.covered, title: 'produced at least one error outcome' },
              { cls: 'k-seg-none', n: ep.total - ep.covered, title: 'happy-path only — no failure mode ever produced' }
            ]));
        }
        return cards;
      },
      // ── Error paths section (D228b) — the response-side sibling of Input Coverage: per source, each
      // EXERCISED item's outcome (statuses produced · declared codes never produced), plus the
      // "what to test next" worklist. Render-only over the per-item outcome the graph already carries.
      errorPathCoverage: function () {
        var self = this;
        var bySource = {};
        (this.data.items || []).forEach(function (it) {
          if (!it.outcome) return;   // never exercised — no sad-path to grade until the happy path runs
          var box = bySource[it.source] || (bySource[it.source] = {
            source: it.source, total: 0, tested: 0, rows: [], worklist: [] });
          box.total++;
          if (it.outcome.tested) box.tested++;
          var never = it.outcome.declaredUntested || [];
          // the items table's own naming: `METHOD /path` for http, the bare name for grpc/kafka
          var label = (it.method && !self.protoBadge(it) ? it.method + ' ' : '') + self.label(it);
          box.rows.push({ id: it.id, label: label, tested: !!it.outcome.tested,
                          seen: self.observedStatuses(it), errors: it.outcome.errors || [], never: never });
          never.forEach(function (c) { box.worklist.push({ item: label, ask: c }); });
          // no declared error universe to name (gRPC, or a spec with no 4xx/5xx) — the ask is generic
          if (!it.outcome.tested && !never.length) box.worklist.push({ item: label, ask: 'any error' });
        });
        return Object.keys(bySource).map(function (k) {
          var box = bySource[k];
          // findings first: rows with a never-produced code, then happy-only, then fully error-tested
          box.rows.sort(function (a, b) {
            var ra = a.never.length ? 0 : (a.tested ? 2 : 1);
            var rb = b.never.length ? 0 : (b.tested ? 2 : 1);
            return ra - rb;
          });
          return box;
        });
      },
      // ── error-path (outcome) row helpers (D228b) ──────────────────────────────────────────────────
      // every status the wire actually produced for this item, across protocols
      observedStatuses: function (it) {
        var out = [];
        ['statusCodes', 'grpcStatuses', 'kafkaStatuses'].forEach(function (k) {
          if (it[k]) Object.keys(it[k]).forEach(function (s) { out.push(String(s)); });
        });
        return out;
      },
      // declared response codes no run ever produced — the honest spec-side gap
      neverProduced: function (it) { return (it.outcome && it.outcome.declaredUntested) || []; },
      // exercised, but never on an error path
      happyOnly: function (it) { return !!(it.outcome && it.outcome.tested === false); },
      segWidth: function (seg, total) { return total ? (seg.n * 100 / total) + '%' : '0%'; },

      // ── Input Coverage drill-down (Part 3) ────────────────────────────────────────────────────────
      // Each input value is a coverable item (the marginal analog of the Rule Coverage arm view): per
      // tested area, the value universe of each field with tried ✓ / missed ✗ (+ the value that closes a
      // miss), open fields greyed with a "no allowed-values" nudge, and a per-area "what to test next"
      // worklist. Derived in JS from the baked `dimensions` projection (rendered, never re-graded).
      get itemSourceMap() {
        var m = {};
        (this.data.items || []).forEach(function (i) { m[i.id] = i.source; });
        return m;
      },
      // value that closes a missed class: numeric class label → its concrete `sample` (age = 17); enum
      // classes already ARE the value (its label == the value), so fall back to the label.
      closingValue: function (field, missed) {
        return (field.examples && field.examples[missed] != null) ? field.examples[missed] : missed;
      },
      inputCoverage: function () {
        var srcOf = this.itemSourceMap, self = this;
        var bySource = {};   // src -> { field -> {field, universe:{}, covered:{}, examples:{}, kind, source, closed} }
        this.dimensions.forEach(function (d) {
          if (!d.axes) return;
          var src = srcOf[d.id] || String(d.id).split(':')[0];
          var grp = bySource[src] || (bySource[src] = {});
          Object.keys(d.axes).forEach(function (field) {
            if (field === 'response') return;   // documented status codes are not an input field
            var a = d.axes[field];
            var f = grp[field] || (grp[field] = { field: field, universe: {}, covered: {}, examples: {}, kind: a.kind || null, source: a.source || null, closed: false });
            if (!f.kind && a.kind) f.kind = a.kind;
            if (!f.source && a.source) f.source = a.source;
            if (a.closed) {
              f.closed = true;
              var gap = {}; (a.gaps || []).forEach(function (g) { gap[String(g)] = true; });
              (a.universe || []).forEach(function (u) {
                var k = String(u);
                f.universe[k] = u;
                if (!gap[k]) f.covered[k] = u;
              });
              if (a.examples) Object.keys(a.examples).forEach(function (k) { f.examples[k] = a.examples[k]; });
            }
          });
        });
        var out = [];
        Object.keys(bySource).forEach(function (src) {
          var fields = [], tried = 0, total = 0, openFields = [];
          Object.keys(bySource[src]).forEach(function (name) {
            var f = bySource[src][name];
            if (!f.closed || !Object.keys(f.universe).length) { openFields.push(name); return; }
            var triedVals = [], missedVals = [];
            Object.keys(f.universe).forEach(function (k) {
              if (f.covered[k] !== undefined) triedVals.push(f.universe[k]);
              else missedVals.push({ label: f.universe[k], value: self.closingValue(f, k) });
            });
            tried += triedVals.length; total += Object.keys(f.universe).length;
            fields.push({ field: name, kind: f.kind, source: f.source, tag: self.axisTag(f),
                          tried: triedVals, missed: missedVals });
          });
          if (!fields.length && !openFields.length) return;
          var worklist = [];
          fields.forEach(function (f) { f.missed.forEach(function (m) { worklist.push({ field: f.field, value: m.value }); }); });
          out.push({ source: src, fields: fields, openFields: openFields,
                     tried: tried, total: total, missed: total - tried, worklist: worklist });
        });
        return out;
      },

      // A1 (D168): a requirement id (`req:ORD-001`) → an external-tracker click-through, when a provider
      // is configured (ADO Story / Jira / git heading). `links` is a resolved { authority: urlTemplate }
      // map keyed on the requirement namespace, inlined into the coverage data global (same offline-safe
      // shape as the RTM). Empty / no matching authority → '' → the id renders as plain text. Mirrors the
      // RTM's reqHref so both artifacts link the same way (hyperlinks-to-source-of-truth everywhere).
      get trackerLinks() { return this.data.links || {}; },
      // bare-id → the requirement item (for the git provider's per-requirement sourceFile + heading anchor)
      get reqItemById() {
        var m = {};
        (this.data.items || []).forEach(function (i) {
          if (i.kind === 'req') m[String(i.id).replace(/^req:/, '')] = i;
        });
        return m;
      },
      reqHref: function (id) {
        if (!id) return '';
        var s = String(id);
        var colon = s.indexOf(':');
        var authority = colon < 0 ? 'req' : s.substring(0, colon);
        var tmpl = this.trackerLinks[authority];
        if (!tmpl) return '';
        var local = colon < 0 ? s : s.substring(colon + 1);
        var localId = local.split('/')[0];                 // drop any /criterion suffix
        var url = tmpl.split('{id}').join(encodeURIComponent(localId));
        // the git provider (E8/D168) needs the item's stamped source file + heading anchor (the id alone
        // can't fill {file}/{anchor}); no sourceFile ⇒ plain text (fail-soft). Mirrors the RTM's reqHref.
        if (url.indexOf('{file}') >= 0 || url.indexOf('{anchor}') >= 0) {
          var it = this.reqItemById[localId];
          if (!it || !it.sourceFile) return '';
          var file = String(it.sourceFile).split('/').map(encodeURIComponent).join('/');
          url = url.split('{file}').join(file).split('{anchor}').join(it.anchor || '');
        }
        return url;
      },

      // diagnostics (carried in the same graph): calls that matched no spec op, and spec-load lint
      get unmatched() { return this.data.unmatched || []; },
      get warnings() {
        var out = [];
        (this.sources || []).forEach(function (s) {
          (s.warnings || []).forEach(function (w) {
            out.push({ source: s.namespace || s.type, type: w.type, endpoint: w.endpoint, message: w.message });
          });
        });
        return out;
      },

      // the main list: real items only — drop acceptance-criterion sub-items (they show nested under
      // their requirement) AND non-leaf requirement `container`s (an epic/feature that only groups child
      // requirements — its status is a deferred rollup, never its own NOTCOVERED, MODEL §2c/§4b, D75).
      // `container` is stamped by CoverageData.itemsJson (the same leaf rule leafRows/sourceSummaryJson
      // apply), so the item table agrees with the readiness scorecard, Coverage.gaps and the RTM.
      get listItems() {
        return this.allItems.filter(function (i) {
          return i.kind !== 'acceptance-criterion' && !i.container;
        });
      },
      get visibleTotal() { return this.listItems.length; },
      get rows() {
        var q = this.q.trim().toLowerCase();
        var src = this.sourceFilter, st = this.statusFilter;
        return this.listItems.filter(function (i) {
          if (src && i.source !== src) return false;
          if (st && i.status !== st) return false;
          if (q) {
            var hay = (i.id + ' ' + (i.name || '') + ' ' + (i.path || '') + ' ' + (i.method || '')).toLowerCase();
            if (hay.indexOf(q) < 0) return false;
          }
          return true;
        });
      },

      // (the per-source operation/combos/inputs sub-stats moved wholesale to the scorecard + the Input
      // Coverage section — the demoted source strip is retired, D181; its getters went with it.)
      toggle: function (id) { this.expanded[id] = !this.expanded[id]; },
      isOpen: function (id) { return !!this.expanded[id]; },

      // drill-down sources
      criteria: function (item) {
        var prefix = item.id + '/';
        return this.allItems.filter(function (i) {
          return i.kind === 'acceptance-criterion' && i.id.indexOf(prefix) === 0;
        });
      },
      hitsFor: function (id) {
        return this.hits.filter(function (h) { return h.item === id; });
      },

      // §2h coverage dimensions (D118): the GRADED projection baked in by Report.generate (marginal
      // coveredProportion per axis + the cross/point block) — rendered, never re-graded here.
      get dimensions() { return this.data.dimensions || []; },
      dimsFor: function (id) {
        return this.dimensions.find(function (d) { return d.id === id; }) || null;
      },
      // the per-axis marginal rows of a dimension item, as a render-friendly list
      axisList: function (d) {
        if (!d || !d.axes) return [];
        return Object.keys(d.axes).map(function (k) {
          var a = d.axes[k];
          return { name: k, closed: !!a.closed, coveredProportion: a.coveredProportion,
                   seen: a.seen || [], gaps: a.gaps || [], kind: a.kind || null, source: a.source || null };
        });
      },
      // §2h: the muted `kind · provenance` tag beside an axis name — the kind (cardinality/string/enum/…)
      // and where its universe came from (the OpenAPI spec vs a rulebook). Auto-derived; render-only.
      axisTag: function (ax) {
        if (!ax || !ax.kind) return '';
        var src = ax.source === 'spec' ? 'from spec'
                : (ax.source && ax.source.indexOf('rule:') === 0) ? 'from rules' : ax.source;
        return src ? ax.kind + ' · ' + src : ax.kind;
      },
      crossOf: function (id) {
        var d = this.dimsFor(id);
        return d && d.cross ? d.cross : null;
      },
      // §2h/D67 covering-array deck (the "which cells to test next" companion to the cross gaps),
      // baked by Report.generate at the criticality-selected strength — rendered read-only.
      get covering() { return this.data.covering || []; },
      deckFor: function (id) {
        return this.covering.find(function (c) { return c.id === id; }) || null;
      },
      // a strength t → its combinatorial name (t=1 marginal, t=2 pairwise, else N-way)
      strengthLabel: function (t) {
        return t === 1 ? 'marginal' : (t === 2 ? 'pairwise' : (t + '-way'));
      },
      // the at-a-glance DIMS-column rollup (computed in Java — Coverage.dimensions): {kind, coveredProportion, gaps}
      dimRollupFor: function (id) {
        var d = this.dimsFor(id);
        return d && d.rollup ? d.rollup : null;
      },
      dimCellTip: function (id) {
        var r = this.dimRollupFor(id);
        if (!r) return '';
        if (r.coveredProportion < 0) return 'dimensions bound but no closed axis observed';
        var what = r.kind === 'cross' ? 'required combinations' : 'input dimensions';
        return what + ': ' + this.pctOf(r.coveredProportion) + '% covered'
          + (r.gaps > 0 ? ' · ' + r.gaps + ' gap' + (r.gaps === 1 ? '' : 's') : ' · complete');
      },
      // a percent (0..100) from a coveredProportion; -1 (open/ungradeable) renders as 0-width
      pctOf: function (p) { return Math.round((p < 0 ? 0 : p) * 100); },
      barClass: function (p) { return p >= 1 ? 'k-ok' : (p > 0 ? 'k-warn' : 'k-no'); },
      // a cross cell {coverage:'LIABILITY', state:'FL', priorClaims:true} → "LIABILITY · FL · priorClaims"
      // (a bare true/false reads as a trap for a business reader — render the boolean as the field name).
      cellText: function (cell) {
        return Object.keys(cell).map(function (k) {
          var v = cell[k];
          if (v === true) return k;
          if (v === false) return 'no ' + k;
          return v;
        }).join(' · ');
      },

      // the source family for a row (openapi/grpc/kafka/rules/req) — an item carries its source
      // *namespace*, so map it back to the declared type; keys the badge on protocol, not a renamed ns.
      srcType: function (it) {
        var ns = it.source, list = this.sources || [];
        for (var i = 0; i < list.length; i++) {
          if (list[i].namespace === ns) return list[i].type;
        }
        return ns;
      },
      // protocol-kind badge for the API-operation sources whose operation name IS already the row label
      // (grpc `RatingService/Rate`, a kafka topic) — the badge adds the protocol, not a redundant echo.
      // openapi is exempt: its GET/POST verb is NOT in the label (just the path), so it keeps the verb badge.
      protoBadge: function (it) {
        var t = this.srcType(it);
        if (t === 'grpc') return 'GRPC';
        if (t === 'kafka') return 'KAFKA';
        if (t === 'mcp') return 'MCP';
        return '';
      },
      // labels
      label: function (it) {
        if (it.kind === 'req') return it.name || '';  // id shown in the k-reqid badge alongside
        if (it.method) return (it.path || it.name || it.id);
        // rules rows: the rulebook name shows in the k-rulebook badge, so drop its redundant
        // `<rulebook>: ` prefix from the row label (RulesCoverageSource mints `rule + ": " + label`).
        if (it.rulebook && it.name) {
          var pre = it.rulebook + ': ';
          return it.name.indexOf(pre) === 0 ? it.name.slice(pre.length) : it.name;
        }
        return it.name || it.id;
      },
      hitLabel: function (h) {
        if (h.method && h.path) return h.method + ' ' + h.path + (h.status ? ' → ' + h.status : '');
        if (h.service) return h.service + '/' + h.method + (h.status ? ' → ' + h.status : '');
        if (h.key) return h.key;
        return h.kind;
      },
      // the same display label the traceability page shows: the node's name; the id stays the key
      testName: function (slug) {
        if (!slug) return '';
        var n = (this.testsById[slug] || {}).name;
        if (n) return n;
        var i = String(slug).lastIndexOf(':');
        return i >= 0 ? String(slug).slice(i + 1) : String(slug);
      },
      get testsById() {
        var m = {};
        (this.data.tests || []).forEach(function (t) { m[t.id] = t; });
        return m;
      },
      // the distinct tests behind an item's hits, its acceptance criteria's rolled in (the table shows the
      // requirement, the hits land on the criteria) — each expands into its execution history
      testsOf: function (item) {
        var self = this, seen = {}, out = [];
        var ids = [item.id].concat(this.criteria(item).map(function (c) { return c.id; }));
        ids.forEach(function (id) {
          self.hitsFor(id).forEach(function (h) { if (!seen[h.test]) { seen[h.test] = true; out.push(h.test); } });
        });
        return out;
      },
      testStatus: function (slug) { return (this.testsById[slug] || {}).status || ''; },
      // a test's own outcome on the item-status maps the pills share
      outcomeStatus: function (s) { return { PASSED: 'COVERED', FAILED: 'FAILING', SKIPPED: 'NOTRUN' }[s] || ''; },
      // ---- execution history: every execution of a test's key in scope, latest first; the selected one is
      // the row's verdict and every other one says why it is not (the selection's own reasons) ----
      histOpen: {},
      histToggle: function (id) { this.histOpen[id] = !this.histOpen[id]; },
      histIsOpen: function (id) { return !!this.histOpen[id]; },
      historyOf: function (slug) {
        return (this.data.executions || []).filter(function (e) { return e.test === slug; })
          .sort(function (a, b) { return (b.startedAt || 0) - (a.startedAt || 0) || String(b.id).localeCompare(String(a.id)); });
      },
      // the viewer's local time, zone named — a run at 05:20 UTC reads as the hour the viewer lived it
      execTime: function (e) {
        if (!e.startedAt) return '';
        var d = new Date(e.startedAt), p = function (n) { return (n < 10 ? '0' : '') + n; }, zone = '';
        try {
          zone = new Intl.DateTimeFormat(undefined, { timeZoneName: 'short' }).formatToParts(d)
            .filter(function (x) { return x.type === 'timeZoneName'; }).map(function (x) { return x.value; })[0] || '';
        } catch (err) { zone = ''; }
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':'
          + p(d.getMinutes()) + ':' + p(d.getSeconds()) + (zone ? ' ' + zone : '');
      },
      execStatus: function (e) { return { passed: 'PASSED', failed: 'FAILED' }[e.outcome] || 'SKIPPED'; },
      execGrade: function (e) {
        var p = e.assertedProportion;
        return (p === undefined || p === null || p < 0) ? '' : 'asserted ' + p;
      },
      execProv: function (e) {
        var p = e.provenance || {};
        return ['build', 'env', 'origin'].filter(function (k) { return p[k]; }).map(function (k) { return k + ' ' + p[k]; });
      },
      // the run's report, served by the console's artifact route; off file:// the run id renders as text
      get runsBase() {
        if (this.data.runsBase) return this.data.runsBase;
        return location.protocol.indexOf('http') === 0 ? '/api/artifacts/runs/' : '';
      },
      execReportHref: function (e) {
        var r = e.artifacts && e.artifacts.report;
        return r && this.runsBase ? this.runsBase + r : '';
      },
      hasFinding: function (e, kind) { return (e.findings || []).some(function (f) { return f.kind === kind; }); },
      execWhy: function (e) {
        if (e.selected) return 'current';
        if (e.retired) return 'retired';
        if (e.lifecycle === 'running') return this.hasFinding(e, 'unfinalizedRun') ? 'unfinalized' : 'pending';
        if (e.supersededBy) return 'superseded';
        return 'not selected';
      },
      execWhyClass: function (e) { return { current: 'k-ok', pending: 'k-warn', unfinalized: 'k-warn' }[this.execWhy(e)] || 'k-tag'; },
      get provisional() { return !!this.data.provisional; },
      // D311: the grade is a fact about the test. Absent = ungraded (the run proved no capture), never 0;
      // negative = not green, so not applicable — both render nothing.
      assertedOf: function (slug) {
        var p = (this.testsById[slug] || {}).assertedProportion;
        return (p === undefined || p === null || p < 0) ? '' : 'asserted ' + p;
      },
      statusClass: function (s) {
        return { COVERED: 'k-ok', FAILING: 'k-no', NOTRUN: 'k-warn', NOTCOVERED: 'k-no' }[s] || '';
      },
      statusIcon: function (s) {
        return { COVERED: '✓', FAILING: '✗', NOTRUN: '~', NOTCOVERED: '—' }[s] || '?';
      }
    };
  });
});
