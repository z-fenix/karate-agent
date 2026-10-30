/* Rule Analysis report renderer (D119) — vanilla JS over the window.KARATE_RULE_ANALYSIS_DATA island.
 * Four BA-grade views (Coverage decision table · Value Ranges · Rule Check · What-If), plus Cells, Twin,
 * Mutation and Match where the book has the artifact each needs, all rendered straight from the deterministic
 * engine output. No framework, no network, no CDN — opens off disk. */
(function () {
    'use strict';
    var DATA = window.KARATE_RULE_ANALYSIS_DATA || {};

    // ---- tiny helpers ----
    function h(tag, attrs, children) {
        var e = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (k) {
                if (k === 'class') e.className = attrs[k];
                else if (k === 'html') e.innerHTML = attrs[k];
                else if (k === 'text') e.textContent = attrs[k];
                else e.setAttribute(k, attrs[k]);
            });
        }
        (children || []).forEach(function (c) {
            if (c == null) return;
            e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
        });
        return e;
    }
    function esc(s) {
        return String(s == null ? '' : s).replace(/[&<>"]/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
        });
    }
    function fmt(v) {
        if (v == null) return '–';
        if (typeof v === 'boolean') return v ? 'Yes' : 'No';
        if (typeof v === 'number') {
            if (Number.isInteger(v)) return v.toLocaleString('en-US');
            return v.toLocaleString('en-US', { maximumFractionDigits: 2 });
        }
        if (typeof v === 'object') return JSON.stringify(v);
        return String(v);
    }
    function badge(text, kind) { return h('span', { class: 'ka-badge' + (kind ? ' ka-badge--' + kind : ''), text: text }); }

    // ---- shell ----
    function render() {
        var app = document.getElementById('app');
        var wrap = h('div', { class: 'ka-wrap' });
        var cov = (DATA.table && DATA.table.coverage) || {};
        var head = h('div', { class: 'ka-head' }, [
            h('h1', {}, ['Rule Analysis ', h('span', { class: 'ka-rule', text: DATA.rule || '' }),
                // D124 context-sensitive "?" — copies the help-id (kaCopyHelp is inlined from
                // help-affordance.js); offline-safe, paste it into the Help tab or an AI chat.
                h('button', { class: 'k-help-q', type: 'button', onclick: "kaCopyHelp('rule.dimensions')",
                    title: 'What is rule analysis? Copy the help-id — paste it into the Help tab or your AI chat' }, ['?'])]),
            cov.percent != null ? badge('Rule Coverage ' + cov.percent + '%', cov.percent === 100 ? 'ok' : 'warn') : null,
            DATA.generator ? badge('generator.js', 'gen') : null
        ]);
        wrap.appendChild(head);
        wrap.appendChild(h('div', { class: 'ka-sub', html:
            'Deterministic decision-table analysis — no run, no AI. Toolbox: <code>Rule.ranges</code> → ' +
            '<code>Rule.check</code> → <code>Rule.sweep</code> → <code>Rule.impact</code>.' }));

        var views = [
            { id: 'coverage', label: 'Coverage', count: DATA.table && DATA.table.rows ? DATA.table.rows.length : null, build: viewCoverage },
            // D326 — the cells tab exists only for a book with numeric file-table cells
            DATA.cells ? { id: 'cells', label: 'Cells', count: DATA.cells.exercised + '/' + DATA.cells.of, build: viewCells } : null,
            { id: 'ranges', label: 'Value Ranges', count: DATA.ranges && DATA.ranges.dimensions ? DATA.ranges.dimensions.length : null, build: viewRanges },
            { id: 'check', label: 'Rule Check', count: checkFindingCount(), build: viewCheck },
            { id: 'whatif', label: 'What-If', count: DATA.whatif && DATA.whatif.sweeps ? DATA.whatif.sweeps.length : null, build: viewWhatIf }
        ].filter(Boolean);
        // §17.4 — the twin tab exists only for a rulebook that ships a twin.js
        if (DATA.twin && DATA.twin.twin === true) {
            views.push({ id: 'twin', label: 'Twin', count: (DATA.twin.check || {}).ci ? DATA.twin.check.ci.verdict : null, build: viewTwin });
        }
        // the mutation tab exists only where a self-grade was run for this book
        if (DATA.mutation) {
            views.push({ id: 'mutation', label: 'Mutation',
                count: (DATA.mutation.counts || {}).SURVIVED, build: viewMutation });
        }
        // the match tab exists only where a results file was ingested for this book (D328(g))
        if (DATA.match && DATA.match.length) {
            views.push({ id: 'match', label: 'Match', count: DATA.match[0].matched + '/' + DATA.match[0].of, build: viewMatch });
        }
        // the editions tab exists only where rate-book records sit beside the book (D331(g))
        if (DATA.editions && DATA.editions.length) {
            views.push({ id: 'editions', label: 'Editions', count: editionsCount(), build: viewEditions });
        }
        // the tables tab exists only where a table diff was persisted for this book (D333(f))
        if (DATA.tables && DATA.tables.length) {
            views.push({ id: 'tables', label: 'Tables', count: tablesCount(), build: viewTables });
        }
        // deep-link the active tab via #analysis=<id> (the D115 View.open target)
        var hashTab = (location.hash.match(/analysis=([a-z]+)/) || [])[1];
        var initial = views.map(function (v) { return v.id; }).indexOf(hashTab);
        if (initial < 0) initial = 0;
        var tabs = h('div', { class: 'ka-tabs' });
        var panels = h('div', {});
        var activate = function (id) {
            Array.prototype.forEach.call(tabs.children, function (c) { c.classList.toggle('ka-tab--active', c.getAttribute('data-tab') === id); });
            Array.prototype.forEach.call(panels.children, function (c) { c.classList.toggle('ka-view--active', c.id === 'view-' + id); });
        };
        views.forEach(function (v, i) {
            var btn = h('button', { class: 'ka-tab' + (i === initial ? ' ka-tab--active' : ''), 'data-tab': v.id }, [
                v.label, v.count != null ? h('span', { class: 'ka-count', text: '  ' + v.count }) : null
            ]);
            btn.addEventListener('click', function () { location.hash = 'analysis=' + v.id; activate(v.id); });
            tabs.appendChild(btn);
            var view = h('div', { class: 'ka-view' + (i === initial ? ' ka-view--active' : ''), id: 'view-' + v.id });
            try { v.build(view); } catch (e) { view.appendChild(h('div', { class: 'ka-empty', text: 'render error: ' + e })); }
            panels.appendChild(view);
        });
        wrap.appendChild(tabs);
        wrap.appendChild(panels);
        wrap.appendChild(h('div', { class: 'ka-foot', text: 'karate-max · Rule Analysis · deterministic, offline' }));
        app.appendChild(wrap);
        // live deep-link: a fragment-only nav (#analysis=ranges) switches tabs without a reload
        window.addEventListener('hashchange', function () {
            var t = (location.hash.match(/analysis=([a-z]+)/) || [])[1];
            if (t) activate(t);
        });
    }

    function checkFindingCount() {
        var c = DATA.check || {};
        var notReachable = c.notreachable || c.gaps || [];
        return (c.notused || []).length + notReachable.length + (c.deadBranches || []).length
            + (c.notproduced || []).length
            + (c.properties || []).filter(function (i) { return !i.holds; }).length
            // a negative scenario the schema failed to refuse is a finding like any other — leaving it
            // out would let the headline read "Healthy" while a stated claim is red (D236b)
            + (c.rejects || []).filter(function (r) { return !r.pass; }).length
            // and the accept half: a saved row its own shape refuses, or — where the book claims
            // rejection assurance — nothing showing the shape admits anything at all
            + acceptFindingCount(c)
            // a stored expectation the rules contradict — the only PER-ROW output oracle here, a
            // figure meant to come from the requirement rather than from calc.js
            + ((c.expectations || {}).failed || []).length;
    }

    function acceptFindingCount(c) {
        var a = c.accepts || {};
        if (a.status === 'REFUSED') {
            return (a.refused || []).length;
        }
        return a.status === 'NOTCHECKED' && (c.rejects || []).length ? 1 : 0;
    }

    // ---- Coverage (the decision table) ----
    function viewCoverage(root) {
        var t = DATA.table || {};
        var cols = t.columns || {};
        var inputs = cols.inputs || [], decisions = cols.decisions || [], outputs = cols.outputs || [];
        var cov = t.coverage || {};
        root.appendChild(h('div', { class: 'ka-headline', html:
            'Rule Coverage: <span class="ka-pct">' + (cov.covered != null ? cov.covered + ' of ' + cov.total : '') +
            ' decision outcomes (' + (cov.percent != null ? cov.percent + '%' : 'n/a') + ')</span>' }));
        var stats = t.stats || {};
        root.appendChild(h('div', { class: 'ka-note', text:
            (stats.rows || 0) + ' scenarios (' + (stats.scenarios || 0) + ' saved, ' + (stats.generated || 0) +
            ' generated) · ' + (stats.distinctPaths || 0) + ' distinct decision paths' }));

        var head = [h('th', { text: 'Scenario' }), h('th', { text: '' })];
        inputs.forEach(function (c) { head.push(h('th', { text: c.label })); });
        decisions.forEach(function (c) { head.push(h('th', { class: 'ka-num', title: c.id, text: c.label })); });
        outputs.forEach(function (k) { head.push(h('th', { class: 'ka-num', text: k })); });
        if (cols.hasOutcome) head.push(h('th', { text: 'Outcome' }));

        var body = [];
        (t.rows || []).forEach(function (r) {
            var dup = r.provenance !== 'scenario' && !r.addsCoverage;
            var cells = [
                h('td', {}, [r.name + (r.error ? ' ⚠' : ''), dup ? h('span', { class: 'ka-prov ka-prov--generated', text: '  dup', style: 'margin-left:6px' }) : null]),
                h('td', {}, [h('span', { class: 'ka-prov ka-prov--' + r.provenance, text: r.provenance === 'scenario' ? 'saved' : 'gen' })])
            ];
            inputs.forEach(function (c) { cells.push(h('td', { text: fmt((r.inputs || {})[c.path]) })); });
            decisions.forEach(function (c) { cells.push(decisionCell((r.decisions || {})[c.id])); });
            outputs.forEach(function (k) { cells.push(h('td', { class: 'ka-num', text: fmt((r.outputs || {})[k]) })); });
            if (cols.hasOutcome) cells.push(h('td', {}, [r.outcome != null ? badge(fmt(r.outcome), r.outcome === 'needs-approval' || r.outcome === 'denied' ? 'warn' : 'ok') : '–']));
            body.push(h('tr', { class: dup ? 'ka-row-dup' : '' }, cells));
        });
        root.appendChild(h('div', { class: 'ka-tablewrap' }, [
            h('table', { class: 'ka-table' }, [h('thead', {}, [h('tr', {}, head)]), h('tbody', {}, body)])
        ]));
        root.appendChild(h('div', { class: 'ka-legend' }, [
            legendSwatch('var(--ka-yes-bg)', 'Y — true arm'),
            legendSwatch('var(--ka-no-bg)', 'N — false arm'),
            legendSwatch('var(--ka-both-bg)', 'Y/N — both (a loop)'),
            h('span', {}, [h('span', { class: 'ka-swatch', style: 'background:var(--ka-panel);border:1px solid var(--ka-border)' }), ' – not reached'])
        ]));
    }
    // ---- Cells (D326) — which file-table cells the saved scenarios exercise ----
    var CELL_STATUS = { masked: 'warn', notChecked: 'warn', unread: 'bad', unreachable: 'bad' };

    function viewCells(root) {
        var c = DATA.cells || {}, rows = c.rows || {}, excluded = rows.excluded || [];
        root.appendChild(h('div', { class: 'ka-headline', html: 'Cell coverage: <span class="ka-pct">' + c.exercised
            + ' of ' + c.of + ' table cells exercised (' + (c.of ? Math.round(c.exercised * 100 / c.of) : 0) + '%)</span>' }));
        root.appendChild(h('div', { class: 'ka-note', text: (c.judgedOn === 'figures'
            ? 'Judged on the declared figures (' + (c.figures || []).join(', ') + '): a cell counts only when doubling it moves one.'
            : 'Judged on every output field — schema.js declares no figures: a cell counts only when doubling it moves one.')
            + ' Over ' + count(rows.evaluated || 0, 'saved scenario')
            + (excluded.length ? ' · ' + excluded.length + ' excluded (' + excluded.map(function (x) {
                return x.row + ': ' + x.reason; }).join('; ') + ')' : '') + '.' }));
        var tables = c.tables || {};
        root.appendChild(h('div', { class: 'ka-tablewrap' }, [h('table', { class: 'ka-table' }, [
            h('thead', {}, [h('tr', {}, [h('th', { text: 'Table' })].concat(
                ['Cells', 'Exercised', 'Masked', 'Not checked', 'Unread'].map(function (l) {
                    return h('th', { class: 'ka-num', text: l });
                })))]),
            h('tbody', {}, Object.keys(tables).map(function (k) {
                var t = tables[k];
                return h('tr', {}, [h('td', {}, [h('code', { text: k })])].concat(
                    [t.of, t.exercised, t.masked, t.notChecked, t.unread].map(function (n) {
                        return h('td', { class: 'ka-num', text: fmt(n) });
                    })));
            }))])]));
        var open = c.cells || [];
        root.appendChild(h('div', { class: 'ka-headline', text: 'Not exercised' }));
        if (!open.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: 'Every cell is exercised.' }));
            return;
        }
        root.appendChild(h('div', { class: 'ka-note', text: 'masked — read, but the figures did not move under the probe · '
            + 'not checked — the probe could not compare · unread — no saved case reads it · '
            + 'unreachable — proven: no input can read it. Rule.cells(name, {target: \'unexercised\'}) drills in.' }));
        dataTable(root, ['Cell', 'File', 'Status', 'Why'], open.map(function (x) {
            return h('tr', {}, [
                h('td', {}, [h('code', { text: x.address })]),
                h('td', { class: 'ka-src', text: x.file || '' }),
                h('td', {}, [badge(x.status, CELL_STATUS[x.status])]),
                h('td', { class: 'ka-wrap', text: cellWhy(x) })
            ]);
        }));
        if (c.truncated) {
            root.appendChild(h('div', { class: 'ka-note', text: 'Listing capped at ' + open.length
                + ' cells — Rule.cells pages the rest.' }));
        }
    }

    function cellWhy(x) {
        var p = x.probe || {};
        if (x.status === 'unreachable') return 'no input reaches it: ' + x.missing;
        if (x.status === 'unread') return 'no saved case reads it';
        if (x.status === 'masked') {
            return 'row ' + p.row + ' reads it; doubled to ' + mutVal(p.perturbed) + ', ' + Object.keys(p.before || {}).map(function (f) {
                return f + ' ' + mutVal(p.before[f]) + ' → ' + mutVal(p.after[f]);
            }).join(' · ') + (x.jurisdictions ? ' (' + x.jurisdictions.join(', ') + ')' : '');
        }
        return (x.reason || '') + (x.detail || p.detail ? ': ' + (x.detail || p.detail) : '') + (p.row != null ? ' (row ' + p.row + ')' : '');
    }

    function legendSwatch(color, label) {
        return h('span', {}, [h('span', { class: 'ka-swatch', style: 'background:' + color }), ' ' + label]);
    }
    function decisionCell(v) {
        switch (v) {
            case 'T': return h('td', { class: 'ka-cell-yes', text: 'Y' });
            case 'F': return h('td', { class: 'ka-cell-no', text: 'N' });
            case '*': return h('td', { class: 'ka-cell-both', text: 'Y/N' });
            default: return h('td', { class: 'ka-cell-na', text: '–' });
        }
    }

    // ---- Value Ranges ----
    function viewRanges(root) {
        var r = DATA.ranges || {};
        root.appendChild(h('div', { class: 'ka-headline', text: 'Which input values matter — and why' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Equivalence classes + boundary values per input (black-box test design: BVA + equivalence partitioning). ' +
            'Boundary chips are the exact thresholds to target.' }));
        var panel = h('div', { class: 'ka-panel' });
        (r.dimensions || []).forEach(function (d) {
            var chips = (d.classes || []).map(function (c) {
                return h('span', { class: 'ka-chip' + (c.boundary ? ' ka-chip--boundary' : ''), text: c.label });
            });
            panel.appendChild(h('div', { class: 'ka-dim' }, [
                h('div', { class: 'ka-dim-head' }, [
                    h('span', { class: 'ka-dim-path', text: d.path }), h('span', { class: 'ka-type', text: d.type })
                ]),
                h('div', { class: 'ka-chips' }, chips),
                (d.gates && d.gates.length) ? h('div', { class: 'ka-gates', text: 'gates: ' + d.gates.join(' · ') }) : null
            ]));
        });
        root.appendChild(panel);

        // outcomes
        var oc = r.outcomes;
        if (oc && (oc.declared || []).length) {
            root.appendChild(h('div', { class: 'ka-headline', text: 'Outcomes' }));
            root.appendChild(h('div', { class: 'ka-note', html:
                'declared <code>' + (oc.declared || []).map(esc).join('</code> <code>') + '</code>' +
                (oc.observed && oc.observed.length ? ' · observed <code>' + oc.observed.map(esc).join('</code> <code>') + '</code>' : '') +
                (oc.neverProduced && oc.neverProduced.length ? ' · <span style="color:var(--ka-no)">never produced: <code>' + oc.neverProduced.map(esc).join('</code> <code>') + '</code></span>' : '') }));
        }

        // grey areas (notderivable) — the boundary can't be DERIVED because the branch turns on a computed value
        if ((r.notderivable || []).length) {
            root.appendChild(h('div', { class: 'ka-grey' }, [
                h('h3', { text: '⚠ Not derivable — branches gated by a computed value the black box can’t attribute' }),
                h('div', { class: 'ka-note', html:
                    'These boundaries live behind a derived computation, so the analysis can’t derive which input drives them. ' +
                    'Declare the values that matter with a <code>calc.levels(inputPath, […])</code> hint to make them deterministic coverage.' }),
                h('ul', {}, r.notderivable.map(function (x) {
                    return h('li', {}, [h('code', { text: x.source }), ' — observed ', h('code', { text: x.observed }), ' (' + x.site + ')']);
                }))
            ]));
        }
    }

    // ---- Rule Check ----
    function viewCheck(root) {
        var c = DATA.check || {};
        var findings = checkFindingCount();
        root.appendChild(h('div', { class: 'ka-headline', html: findings === 0
            ? 'Healthy — no gaps, no dead branches, all properties hold'
            : findings + ' finding' + (findings === 1 ? '' : 's') + ' to review' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Completeness & health over the input-space partition. Imperative-honest: true DMN overlap/conflict is a ' +
            'declarative-table property (deferred to rule tables).' }));

        var cards = h('div', { class: 'ka-cards' });
        // the two-failure-mode split (D120): notused = the fix is DATA (add case data); notreachable =
        // the fix is RULES (a branch nothing reaches even with the generator). gaps is the legacy alias.
        cards.appendChild(card('Not used — add case data', (c.notused || []).map(function (g) {
            return h('li', {}, [(g.source || g.site || '') + '', h('span', { class: 'ka-src', text:
                'site ' + (g.site || g.siteId) + ' · ' + (g.outcome === true ? 'true' : g.outcome === false ? 'false' : g.outcome) + ' arm reachable but no saved scenario exercises it · line ' + g.line })]);
        }), 'Every reachable decision outcome is exercised by a saved scenario.'));
        cards.appendChild(card('Not reachable — fix the rules', (c.notreachable || c.gaps || []).map(function (g) {
            return h('li', {}, [(g.source || g.site || '') + '', h('span', { class: 'ka-src', text:
                'site ' + (g.site || g.siteId) + ' · ' + (g.outcome === true ? 'true' : g.outcome === false ? 'false' : g.outcome) + ' arm never fires for any in-domain input · line ' + g.line })]);
        }), 'Every decision outcome is reachable by some input.'));
        cards.appendChild(card('Dead branches', (c.deadBranches || []).map(function (d) {
            return h('li', {}, [h('code', { text: trunc(d.source, 50) }), h('span', { class: 'ka-src', text:
                'site ' + d.siteId + ' · only the ' + (d.arm === 'true' || d.arm === true ? 'true' : 'false') + ' arm fires · line ' + d.line })]);
        }), 'No constant or unreachable branches.'));
        cards.appendChild(card('Not produced — outcomes no input yields', (c.notproduced || []).map(function (o) {
            return h('li', {}, [h('code', { text: String(o) })]);
        }), 'Every declared outcome is produced.'));

        // properties (the PBT pair: calc.always safety invariants + calc.sometimes situations, D188)
        // — an always entry speaks the rules-verification §2 vocabulary: COMPLETE (proven over the
        // closed universe, D236a) / HOLDS (sampled) / VIOLATED / NOTCHECKED
        var invs = (c.properties || []).map(function (inv) {
            var sometimes = inv.kind === 'sometimes';
            var product = c.universe && c.universe.product;
            // D241: `checks` counts the DECLARED-DOMAIN population — the rows a verdict may be read
            // off. `robustnessChecks` is the rest, disclosed rather than mixed in: a property with
            // `checks: 0` beside a non-zero one WAS executed, just never on an input that can occur.
            var probes = inv.robustnessChecks ? ' · +' + inv.robustnessChecks + ' robustness probes' : '';
            // D246: the corner hunt is its own population — a violation it found reads VIOLATED with
            // `violations: 0` beside it, so the count must name the population that produced it, and
            // a hunted-and-held property must say it was hunted rather than merely wandered past.
            var hunted = inv.searched ? ' · ' + inv.searched + ' corners hunted' : '';
            var broke = (inv.violations ? inv.violations + ' of ' + (inv.checks || 0) + ' in-domain inputs' : '')
                + (inv.searchViolations ? (inv.violations ? ' · ' : '')
                    + inv.searchViolations + ' of ' + inv.searched + ' hunted corners' : '');
            // D317: the sometimes polarity reads its own status first — a NOTREACHABLE situation can
            // carry `checks: 0` (its arm is dead on the whole universe) and is a proof, not "not checked"
            var status = sometimes && inv.status === 'NOTREACHABLE'
                ? 'NOTREACHABLE — proven: no input of the closed domain produces it · all ' + product + ' possible inputs evaluated'
                : sometimes && inv.status === 'NOTREACHED'
                ? 'NOTREACHED — ' + inv.checks + ' in-domain inputs tried, none produced it (sampled, not proven)'
                : !inv.checks && inv.status !== 'VIOLATED' ? (sometimes ? 'NOTCHECKED — never executed'
                    : inv.robustnessChecks
                        ? 'NOTCHECKED — evaluated only outside the declared domain (' + inv.robustnessChecks + ' probes)'
                        : 'NOTCHECKED — never executed')
                : inv.holds ? (sometimes ? 'REACHED — ' + inv.satisfied + ' of ' + inv.checks + ' in-domain inputs'
                    : inv.status === 'COMPLETE'
                        ? 'COMPLETE — proven for this input domain · all ' + product + ' possible inputs evaluated' + probes
                        : 'HOLDS — ' + (inv.checks || 0) + ' in-domain inputs, 0 counterexamples (sampled, not proven)' + hunted + probes)
                : (sometimes ? 'never reached · ' + inv.checks + ' inputs tried' : 'VIOLATED — ' + broke);
            var li = h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--' + (inv.holds ? 'pass' : 'fail') }, [
                    h('span', { class: 'ka-mark', text: inv.holds ? '✓' : '✗' }),
                    h('span', {}, [(sometimes ? 'sometimes: ' : 'always: ') + inv.name, h('span', { class: 'ka-src', text: status })])
                ])
            ]);
            if (!inv.holds && inv.counterexample) {
                li.appendChild(h('div', { class: 'ka-counter', text: 'counterexample: ' + JSON.stringify(inv.counterexample) }));
                // the two readings (D236c): machine evidence against the SYSTEM or against the SPEC —
                // the triage the oracle-vs-live diff already teaches
                li.appendChild(h('div', { class: 'ka-src', text: 'two readings: the rules are wrong, or the '
                    + 'property/scenario expectation is wrong (a spec defect)'
                    // `0 >= 0` is true, so a corner-only violation (checks 0, violations 0) used to
                    // be diagnosed as an inverted predicate — the deck simply never reached it
                    + (inv.checks > 0 && inv.violations >= inv.checks
                        ? ' — violated on every explored row, often an inverted property' : '')
                    + '; fix the reviewed artifact, re-run, keep the counterexample as a saved scenario' }));
            }
            return li;
        });
        cards.appendChild(card('Properties', invs, 'No calc.always / calc.sometimes properties declared.'));

        // negative scenarios (D236b): rows claiming schema.js must REFUSE them. REFUSED = the claim
        // holds; ACCEPTED = the shape is too loose; NOTCHECKED = the row carries no business input, so
        // its mismatch proves nothing. Each verdict is about THAT row — never "the gate rejects bad
        // input" in general, which is why the reason is shown rather than a bare tick.
        var rejects = (c.rejects || []).map(function (r) {
            return h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--' + (r.pass ? 'pass' : 'fail') }, [
                    h('span', { class: 'ka-mark', text: r.pass ? '✓' : '✗' }),
                    h('span', {}, [(r._label || r._id || ''),
                        h('span', { class: 'ka-src', text: (r.status || '') + ' — ' + (r.why || '') })])
                ])
            ]);
        });
        // the accept half, rendered in the SAME card: read apart, a green roster on a shape that
        // refuses everything looks like proof (D239), so the two verdicts belong side by side
        var acc = c.accepts || {};
        if (acc.status) {
            var accPass = acc.status === 'ACCEPTED';
            rejects.push(h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--' + (accPass ? 'pass' : 'fail') }, [
                    h('span', { class: 'ka-mark', text: accPass ? '✓' : '✗' }),
                    h('span', {}, ['schema.js accepts valid input',
                        h('span', { class: 'ka-src', text: acc.status + ' — ' + (acc.checked || 0)
                            + ' saved row(s) checked, ' + (acc.refused || []).length + ' refused'
                            + (acc.incomplete ? ', ' + acc.incomplete + ' skipped (omit a declared key)' : '') })])
                ])
            ]));
        }
        cards.appendChild(card('Negative scenarios', rejects,
            'No rows declare _expect: \'schema-reject\' — "bad input is refused" is untested here.'));

        // stored expectations: the only rows carrying a figure the rulebook did not produce
        var exp = c.expectations || {};
        var pins = ((exp.failed) || []).map(function (f) {
            return h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--fail' }, [
                    h('span', { class: 'ka-mark', text: '✗' }),
                    h('span', {}, [(f._label || f._id || ''),
                        h('span', { class: 'ka-src', text: f.why || '' })])
                ])
            ]);
        });
        if (exp.status === 'MATCHED') {
            // "every GRADED pin" — MATCHED does not speak for a row that never ran, and a headline
            // saying it would turn an honest API into a false claim
            pins.push(h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--pass' }, [
                    h('span', { class: 'ka-mark', text: '✓' }),
                    h('span', {}, ['every graded stored expectation holds',
                        h('span', { class: 'ka-src', text: exp.checked + ' author-supplied figure(s) graded'
                            + ((exp.notChecked || []).length ? ', ' + exp.notChecked.length + ' not checked' : '') })])
                ])
            ]));
        }
        // a pin that could not be run says nothing either way, and silence would read as a pass
        ((exp.notChecked) || []).forEach(function (n) {
            pins.push(h('li', {}, [
                h('div', { class: 'ka-inv ka-inv--skip' }, [
                    h('span', { class: 'ka-mark', text: '–' }),
                    h('span', {}, [(n._label || n._id || ''),
                        h('span', { class: 'ka-src', text: 'NOT CHECKED — ' + (n.why || '') })])
                ])
            ]));
        });
        cards.appendChild(card('Stored expectations', pins,
            'No row carries an author-supplied _expect figure — every answer here is the one calc.js computed.'));
        // D241: what the rows OUTSIDE the declared domain found. Findings, never verdicts — they are
        // deliberately absent from checkFindingCount, because none of them fails anything: an
        // impossible input does not get a vote, it gets a report.
        var rob = c.robustness || {};
        var outside = [];
        // D295: the per-axis rollup leads — which declared bound this run walked outside of, and
        // from which population — so the detail below reads as instances of a named finding
        (c.outOfDomain || []).forEach(function (a) {
            var declared = a.members ? JSON.stringify(a.members)
                : (a.lo != null || a.hi != null) ? '[' + (a.lo != null ? a.lo : '') + ', ' + (a.hi != null ? a.hi : '') + ']'
                : (a.min != null ? 'cardinality ' + a.min + '..' + a.max : '');
            outside.push(h('li', {}, [a.axis + ' — ' + a.rows + ' row' + (a.rows === 1 ? '' : 's') + ' outside ' + declared,
                h('span', { class: 'ka-src', text: (a.reason || '') + ' · from ' + (a.sources || []).join(', ') })]));
        });
        (rob.savedRows || []).forEach(function (r) {
            var axis = (r.offending && r.offending[0]) || {};
            outside.push(h('li', {}, ['saved row ' + (r.id || '#' + r.index) + ' leaves the domain on '
                + (axis.axis || ''), h('span', { class: 'ka-src', text: (axis.reason || '')
                + ' — fix the row, or widen generator.js' })]));
        });
        (rob.coverageOnly || []).forEach(function (g) {
            outside.push(h('li', {}, ['site ' + g.site + ' · ' + g.outcome + ' arm fires only for an impossible input',
                h('span', { class: 'ka-src', text: 'dead for every legal input · line ' + g.line
                    + ' — a defensive guard, not a missing scenario' })]));
        });
        (rob.unreachableBoundaries || []).forEach(function (u) {
            outside.push(h('li', {}, [u.axis + ' is compared against ' + u.value,
                h('span', { class: 'ka-src', text: 'no legal input can cross it — the branch cannot fire' })]));
        });
        cards.appendChild(card('Outside the declared domain', outside,
            'No declared bound was crossed.'));
        root.appendChild(cards);
    }
    function card(title, items, emptyMsg) {
        var n = items.length;
        return h('div', { class: 'ka-card' }, [
            // Properties and Negative scenarios are ROSTERS, not gap lists — a non-empty one is the
            // healthy state (each entry carries its own pass/fail mark), so it never badges 'warn'
            // Disclosures is P29 — a disclosure is never a defect, so it never badges 'warn' either
            h('h3', {}, [title, badge(String(n), n === 0 ? 'ok'
                : (title === 'Properties' || title === 'Negative scenarios' || title.indexOf('Disclosures') === 0
                    ? null : 'warn'))]),
            n ? h('ul', {}, items) : h('div', { class: 'ka-empty', text: emptyMsg })
        ]);
    }
    function trunc(s, n) { s = String(s || ''); return s.length > n ? s.slice(0, n) + '…' : s; }

    // ---- What-If ----
    function viewWhatIf(root) {
        var w = DATA.whatif || {};
        root.appendChild(h('div', { class: 'ka-headline', html: 'How sensitive is <span class="ka-pct">' + esc(w.output || 'output') + '</span>, and where does it tip' }));
        root.appendChild(h('div', { class: 'ka-note', text: 'Base ' + (w.output || 'output') + ' = ' + fmt(w.base) + '. Single-factor sweeps over each input’s classes; the tornado ranks every input by how far it moves the output.' }));

        // tornado
        if ((w.tornado || []).length) {
            root.appendChild(h('div', { class: 'ka-chart' }, [
                h('h3', { text: 'Tornado — inputs ranked by impact on ' + (w.output || 'output') }),
                h('div', { html: tornadoSvg(w.tornado, w.base) })
            ]));
        }

        // sweep selector + chart
        var sweeps = w.sweeps || [];
        if (sweeps.length) {
            var chart = h('div', { class: 'ka-chart' });
            var sel = h('select', { class: 'ka-select' }, sweeps.map(function (s, i) {
                return h('option', { value: String(i), text: s.dim });
            }));
            var draw = function () {
                var s = sweeps[+sel.value];
                chart.innerHTML = '';
                chart.appendChild(h('h3', { text: s.output + ' as ' + s.dim + ' varies' }));
                chart.appendChild(h('div', { html: lineSvg(s.points, s.output) }));
                if ((s.points || []).some(function (p) { return p.flip; })) {
                    chart.appendChild(h('div', { class: 'ka-flipnote', text: '◆ marks where the decision outcome flips' }));
                }
            };
            sel.addEventListener('change', draw);
            root.appendChild(h('div', { class: 'ka-controls' }, [h('label', { text: 'Sweep input:' }), sel]));
            root.appendChild(chart);
            draw();
        }
    }

    function tornadoSvg(bars, base) {
        var W = 760, rowH = 26, padL = 150, padR = 70, padT = 10, padB = 24;
        var H = padT + bars.length * rowH + padB;
        var lo = Math.min.apply(null, bars.map(function (b) { return num(b.low); }).concat(num(base)));
        var hi = Math.max.apply(null, bars.map(function (b) { return num(b.high); }).concat(num(base)));
        if (lo === hi) { hi = lo + 1; }
        var x = function (v) { return padL + (num(v) - lo) / (hi - lo) * (W - padL - padR); };
        var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="' + W + '" height="' + H + '">';
        // base line
        s += '<line x1="' + x(base) + '" y1="' + padT + '" x2="' + x(base) + '" y2="' + (H - padB) + '" style="stroke:var(--ka-faint)" stroke-dasharray="3 3"/>';
        s += '<text x="' + x(base) + '" y="' + (H - 8) + '" font-size="10" style="fill:var(--ka-muted)" text-anchor="middle">base ' + esc(fmt(base)) + '</text>';
        bars.forEach(function (b, i) {
            var y = padT + i * rowH + 4;
            var x1 = x(b.low), x2 = x(b.high);
            s += '<text x="' + (padL - 8) + '" y="' + (y + 11) + '" font-size="11" style="fill:var(--ka-ink)" text-anchor="end">' + esc(b.dim) + '</text>';
            s += '<rect x="' + Math.min(x1, x2) + '" y="' + y + '" width="' + Math.max(2, Math.abs(x2 - x1)) + '" height="' + (rowH - 10) + '" rx="3" style="fill:var(--ka-bar)" opacity="0.85"/>';
            s += '<text x="' + (W - padR + 6) + '" y="' + (y + 11) + '" font-size="10" style="fill:var(--ka-muted)">' + esc(fmt(b.spread)) + '</text>';
        });
        return s + '</svg>';
    }

    function lineSvg(points, outputKey) {
        points = (points || []).filter(function (p) { return typeof p.output === 'number'; });
        if (points.length === 0) return '<div class="ka-empty">no numeric output to chart</div>';
        var W = 760, H = 240, padL = 60, padR = 20, padT = 16, padB = 56;
        var ys = points.map(function (p) { return p.output; });
        var lo = Math.min.apply(null, ys), hi = Math.max.apply(null, ys);
        if (lo === hi) { hi = lo + 1; lo = lo - 1; }
        var n = points.length;
        var px = function (i) { return padL + (n === 1 ? 0.5 : i / (n - 1)) * (W - padL - padR); };
        var py = function (v) { return padT + (1 - (v - lo) / (hi - lo)) * (H - padT - padB); };
        var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="' + W + '" height="' + H + '">';
        // axes
        s += '<line x1="' + padL + '" y1="' + padT + '" x2="' + padL + '" y2="' + (H - padB) + '" style="stroke:var(--ka-border-strong)"/>';
        s += '<line x1="' + padL + '" y1="' + (H - padB) + '" x2="' + (W - padR) + '" y2="' + (H - padB) + '" style="stroke:var(--ka-border-strong)"/>';
        s += '<text x="' + (padL - 8) + '" y="' + (py(hi) + 4) + '" font-size="10" style="fill:var(--ka-muted)" text-anchor="end">' + esc(fmt(hi)) + '</text>';
        s += '<text x="' + (padL - 8) + '" y="' + (py(lo) + 4) + '" font-size="10" style="fill:var(--ka-muted)" text-anchor="end">' + esc(fmt(lo)) + '</text>';
        // step line
        var dpath = '';
        points.forEach(function (p, i) { dpath += (i === 0 ? 'M' : 'L') + px(i) + ' ' + py(p.output); });
        s += '<path d="' + dpath + '" fill="none" style="stroke:var(--ka-accent)" stroke-width="2"/>';
        points.forEach(function (p, i) {
            if (p.flip) {
                s += '<line x1="' + px(i) + '" y1="' + padT + '" x2="' + px(i) + '" y2="' + (H - padB) + '" style="stroke:var(--ka-flip)" stroke-dasharray="3 3"/>';
            }
            s += '<circle cx="' + px(i) + '" cy="' + py(p.output) + '" r="' + (p.flip ? 5 : 3.5) + '" style="fill:' + (p.flip ? 'var(--ka-flip)' : 'var(--ka-accent)') + '"/>';
            // x label
            s += '<text x="' + px(i) + '" y="' + (H - padB + 16) + '" font-size="10" style="fill:var(--ka-muted)" text-anchor="middle" transform="rotate(20 ' + px(i) + ' ' + (H - padB + 16) + ')">' + esc(trunc(p.class, 12)) + '</text>';
        });
        return s + '</svg>';
    }

    function num(v) { return typeof v === 'number' ? v : (v == null ? 0 : parseFloat(v) || 0); }

    // ---- Twin (§17.4) — DATA.twin is Twin.section's payload verbatim; the console pane renders the
    // same shape, so nothing here may reshape it. Every plain-language string below is duplicated
    // verbatim in the console's rulebook-page.js: the two renderers cannot share code, so they share
    // wording — one finding must not meet a reader in two vocabularies.
    var TWIN_BOUNDARY = 'A walk is evidence about the declaration, bounded by its ceilings; it never proves the system.';
    var TWIN_NOTE = 'The twin is a model of how the system may behave; the walk drives every order the '
        + 'model allows, and a live replay compares that model against the running service.';

    function verdictKind(v) { return v === 'PASS' ? 'ok' : v === 'FAIL' ? 'bad' : 'warn'; }

    function count(n, one) { return (n === 1 ? 'one' : n) + ' ' + one + (n === 1 ? '' : 's'); }

    // the verdict sentence: what the model covered, then what the live replay found
    function twinHeadline(t) {
        var ck = t.check || {}, f = ck.findings || {}, ev = ck.evidence || {}, ci = ck.ci || {};
        if (t.error) return 'The walk did not finish — the reading below is partial.';
        if (ci.verdict === 'INVALID') return 'The walk did not report everything it must, so nothing here can be graded.';
        var req = (f.requiredNotreached || []).length,
            drift = (f.sequenceDrift || []).length,
            mism = (f.liveMismatch || []).length,
            liveOf = (ev.live || {}).of || 0;
        if (!req && !drift && !mism) {
            return liveOf ? 'Healthy — the walk covers the model and every live replay agrees.'
                : 'Healthy — the walk covers the model; nothing has been replayed against a running service yet.';
        }
        var parts = [req === 0 ? 'The model reaches every required row'
            : count(req, 'required row') + ' the walk never reached'];
        if (drift) parts.push(count(drift, 'blessed sequence') + (drift === 1 ? ' has moved' : ' have moved'));
        if (mism) parts.push(laneCounts(f.liveMismatch).join(' · ')
            + (mism === 1 ? ' stands' : ' stand') + ' against the service');
        return capitalize(parts.join(' — ') + '.');
    }
    // liveMismatch carries a pinned sequence's FAIL (no lane) beside the engine-derived lanes (§17.13)
    var LANE_NOUN = { live: 'live disagreement', witness: 'witness refutation', orders: 'failing order' };
    function laneOf(m) { return m.lane || 'live'; }
    function laneCounts(rows) {
        return ['live', 'witness', 'orders'].map(function (lane) {
            var n = rows.filter(function (m) { return laneOf(m) === lane; }).length;
            return n ? count(n, LANE_NOUN[lane]) : null;
        }).filter(Boolean);
    }
    function capitalize(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

    // the plan the section walked (§17.6), its hash cut as the lifecycle pane's disclosure cuts it;
    // the empty plan's canonical depth is the frozen ceiling (Ceilings.DEFAULT.maxDepth)
    var EMPTY_PLAN_DEPTH = 12;
    function twinPlanLine(p) {
        var depth = p.depth || {}, levels = p.levels || {}, focus = p.focus || [];
        var line = 'plan ' + String(p.planSha).substring(0, 12) + ' · ';
        var pairs = Object.keys(depth).filter(function (k) { return k !== 'default'; });
        var paths = Object.keys(levels);
        if (!pairs.length && !paths.length && !focus.length && depth['default'] === EMPTY_PLAN_DEPTH) {
            return line + 'empty plan';
        }
        var parts = ['depth ' + Object.keys(depth).map(function (k) { return k + ' ' + depth[k]; }).join(', ')];
        if (paths.length) {
            parts.push('levels ' + paths.map(function (k) { return k + ': ' + levels[k].map(fmt).join(', '); }).join('; '));
        }
        if (focus.length) parts.push('focus ' + focus.join(', '));
        return line + parts.join(' · ');
    }

    // one plain sentence per `incomplete` kind (§17.4/§17.5) — a disclosure is never a defect (P29),
    // so each one says what this reading does NOT cover, never that something failed
    function disclosureText(d) {
        switch (d.kind) {
            case 'ceiling':
                return 'The walk stopped at a limit (' + fmt(d.hit) + '), so it did not try every order the model allows.';
            case 'frontier':
                return fmt(d.frontier) + ' orders were still unexplored when the walk stopped.';
            case 'guardNotChecked':
                return 'The rule deciding whether ' + fmt(d.command) + ' is allowed threw an error, so it was never graded.';
            case 'undeclaredOpenField':
                return 'The field ' + fmt(d.field) + ' took ' + count(d.distinct, 'value') + ' the model does not declare.';
            case 'neverBlessed':
                return 'Nothing has been blessed yet, so there is no baseline to compare this run against.';
            case 'twinEdited':
                return 'The model was edited after its last bless — review the edit and bless again.';
            case 'adjudicatedModel':
                return 'Sequence ' + fmt(d.sequenceId) + ': the live disagreement was reviewed and blamed on the model, '
                    + 'so it does not fail the build.';
            case 'liveUngraded':
                return 'Sequence ' + fmt(d.sequenceId) + ': ' + ungradedReason(d) + '.';
            case 'derivedUngraded':
                return (d.id == null ? 'The ' + d.lane + ' record' : mismatchWho(d)) + ': ' + derivedReason(d) + '.';
            default:
                return String(d.kind);
        }
    }

    // the reasons Twin.check folds a live record under (§17.5) — the served live topic is their source
    function ungradedReason(d) {
        switch (d.reason) {
            case 'VERSION': return 'the recorded run uses an earlier record format, so it was not counted — re-run the live replay';
            case 'STALE': return 'the rules or the model changed after the run, so it was not counted';
            case 'UNREPRODUCED': return 'the recorded run could not be replayed step for step, so it was not counted';
            case 'CORRUPT': return 'the recorded run does not match its own record, so it was not counted';
            case 'UNRESOLVED': return 'the replay left more than one possible state, so it decided nothing';
            case 'NOTCHECKED': return 'the model errored during the replay, so it proves nothing';
            case 'REFUSED': return 'the replay was refused before it could finish'
                + (d.code ? ' (' + fmt(d.code) + ')' : '');
            case 'INVALID': return 'the service interaction could not be graded — the starting state was not reproduced, or a lost response had no declared meaning';
            default: return 'the replay ended ' + fmt(d.reason) + ', so it was not counted';
        }
    }

    // the reasons Twin.check leaves a witness or orders row ungraded (§17.13)
    function derivedReason(d) {
        switch (d.reason) {
            case 'VERSION': return 'it was recorded in an earlier format, so it was not counted — re-run the lane';
            case 'STALE': return 'the rules, the model or the plan changed after the run, so it was not counted';
            case 'UNCONFIRMED': return 'the contradiction did not repeat on its confirming replay, so it was not counted';
            case 'NOREVISION': return 'the run or this check names no service revision, so it was not counted';
            case 'REVISION': return 'the run was taken against another service revision, so it was not counted';
            default: return ungradedReason(d);
        }
    }

    // the raw fields stay available, compactly and second — never the primary rendering
    function disclosureDetail(d) {
        var fields = Object.keys(d).filter(function (k) { return k !== 'kind'; })
            .map(function (k) { return k + ' ' + fmt(d[k]); }).join(' · ');
        return d.kind + (fields ? ' · ' + fields : '');
    }

    // a live disagreement, read as three plain columns: where, what the service did, what the model allows
    function mismatchStep(m) {
        return (m.mismatch || {}).kind === 'readBackExplainsNoCandidate'
            ? 'the closing read-back' : 'step ' + fmt((m.at || 0) + 1);
    }
    function mismatchService(m) {
        var mm = m.mismatch || {};
        if (mm.kind === 'readBackExplainsNoCandidate') {
            return 'read back a state the model does not allow'
                + (mm.missing == null ? '' : ' (' + fmt(mm.missing) + ')');
        }
        if (mm.kind === 'refusalReasonMismatch') return 'refused the step — ' + fmt(mm.observed);
        return (mm.outcome === 'refused' ? 'refused the step'
            : mm.outcome === 'applied' ? 'applied the step' : 'answered ' + fmt(mm.outcome))
            + (mm.reason == null ? '' : ' — ' + fmt(mm.reason));
    }
    function mismatchModel(m) {
        var mm = m.mismatch || {};
        if (mm.kind === 'refusalReasonMismatch') return 'refuses for ' + (mm.model || []).map(fmt).join(', ');
        return mm.kind === 'readBackExplainsNoCandidate'
            ? 'no state the model still allowed survives that read-back'
            : 'no state the model still allowed explains that answer';
    }
    // who disagreed: a pinned sequence, a witness obligation (its key) or a failing order (its id)
    function mismatchWho(m) {
        var lane = laneOf(m);
        return lane === 'witness' ? 'Witness obligation ' + m.id
            : lane === 'orders' ? 'Order ' + m.id : 'Sequence ' + m.sequenceId;
    }

    var PROVENANCE_LABEL = { twin: 'model version', calc: 'rules version',
        schema: 'input-shape version', generator: 'generator version' };
    var ORIGIN_LABEL = { explore: 'promoted from a walk', authored: 'written by hand', live: 'from a live run' };

    // a step path a business reader can follow: one numbered line per command, arguments flattened to
    // "key: value" pairs — never an inline JSON blob
    function stepList(steps) {
        return h('ol', { class: 'ka-steps' }, (steps || []).map(function (s) {
            var kv = [];
            flatten(s.args, '', kv);
            return h('li', {}, [String(s.command),
                kv.length ? h('span', { text: ' — ' + kv.join(', ') }) : null]);
        }));
    }
    function flatten(v, prefix, out) {
        if (v == null || typeof v !== 'object') {
            if (prefix) out.push(prefix + ': ' + fmt(v));
            return;
        }
        if (Array.isArray(v)) {
            out.push(prefix + ': ' + v.map(fmt).join(' / '));
            return;
        }
        Object.keys(v).forEach(function (k) { flatten(v[k], prefix ? prefix + '.' + k : k, out); });
    }

    function chips(values) {
        return h('div', { class: 'ka-chips' }, (values || []).map(function (v) {
            return h('span', { class: 'ka-chip', text: String(v) });
        }));
    }

    function twinRow(panel, label, note, body) {
        panel.appendChild(h('div', { class: 'ka-dim' }, [
            h('div', { class: 'ka-dim-head' }, [
                h('span', { class: 'ka-dim-path', text: label }),
                note != null ? h('span', { class: 'ka-type', text: String(note) }) : null
            ]),
            body
        ]));
    }

    function dataTable(root, columns, rows, cls) {
        root.appendChild(h('div', { class: 'ka-tablewrap' }, [
            h('table', { class: 'ka-table' + (cls ? ' ' + cls : '') }, [
                h('thead', {}, [h('tr', {}, columns.map(function (c) { return h('th', { text: c }); }))]),
                h('tbody', {}, rows)
            ])
        ]));
    }

    function viewTwin(root) {
        var t = DATA.twin || {};
        var ex = t.explore || {}, ck = t.check || {}, ci = ck.ci || {};
        var ceil = ex.ceilings || {}, usage = ceil.usage || {};
        root.appendChild(h('div', { class: 'ka-head' }, [
            h('h1', { text: 'Twin' }),
            ci.verdict ? badge(ci.verdict, verdictKind(ci.verdict)) : null,
            ceil.hit ? badge('ceilings: ' + ceil.hit, ceil.hit === 'exhausted' ? 'ok' : 'warn') : null
        ]));
        root.appendChild(h('div', { class: 'ka-headline', text: twinHeadline(t) }));
        if (ex.plan) {
            root.appendChild(h('div', { class: 'ka-src', 'data-twin-plan': '', text: twinPlanLine(ex.plan) }));
        }
        root.appendChild(h('div', { class: 'ka-note', text: TWIN_NOTE }));
        if (ceil.hit) {
            root.appendChild(h('div', { class: 'ka-src', text:
                fmt(usage.nodesVisited) + ' nodes visited · ' + fmt(usage.runsExecuted) + ' runs executed · ' +
                fmt(usage.wallSeconds) + 's wall · max depth reached ' + fmt(usage.maxDepthReached) }));
        }
        root.appendChild(h('div', { class: 'ka-note', text: TWIN_BOUNDARY }));
        if (t.error) {
            // the message leads — a bare code tells a business reader nothing (the code stays, muted)
            root.appendChild(h('div', { class: 'ka-grey' }, [
                h('h3', { text: 'The walk did not finish' }),
                h('div', { class: 'ka-note', text: t.error.message }),
                h('div', { class: 'ka-src', text: 'code ' + t.error.code })
            ]));
            twinSequences(root, t);
            twinLifecycle(root, DATA.twinDocs);
            return;
        }
        twinWalk(root, ex);
        twinFindings(root, ck);
        twinWireRefusals(root, t);
        twinSequences(root, t);
        twinLifecycle(root, DATA.twinDocs);
    }

    // ---- the docs' lifecycle pane (playground.md §7) — DATA.twinDocs is the pane Openapi.docs returns,
    // grouped by the shared kaLifecycleView; read-only, in the console lens's and the app's order:
    // disclosure · flow · what next · per operation · the two lists
    function outcomeBadge(o) { return h('span', { class: 'ka-lc-o ka-lc-o--' + o, text: o }); }

    function fold(summary, body, attrs) {
        var d = h('details', attrs || {}, [h('summary', {}, summary)]);
        (body || []).forEach(function (c) { if (c) d.appendChild(c); });
        return d;
    }

    function twinLifecycle(root, pane) {
        var v = window.kaLifecycleView ? window.kaLifecycleView(pane) : null;
        if (!v) {
            return;
        }
        var box = h('div', { class: 'ka-lc', 'data-lc': v.rule });
        root.appendChild(box);
        box.appendChild(h('div', { class: 'ka-headline', text: 'API lifecycle — by sequence and state' }));
        if (v.unavailable) {
            box.appendChild(h('div', { class: 'ka-note', 'data-lc-unavailable': '', text: 'No lifecycle pane for ' + v.rule
                + ' — ' + v.unavailable.code + ': ' + v.unavailable.reason }));
            return;
        }
        box.appendChild(h('div', { class: 'ka-src', 'data-lc-disclosure': '', text: v.line }));
        box.appendChild(h('div', { class: 'ka-note', text: v.boundary }));
        if (v.drift.drift) {
            box.appendChild(h('div', { class: 'ka-flipnote', text: 'projection drift — ' + (v.drift.reason || '') }));
        }

        var f = v.flow;
        box.appendChild(h('div', { class: 'ka-chart', 'data-lc-flow': '' }, [
            h('h3', {}, ['Flow — ' + f.edges.length + ' edges between ' + (f.nodes || []).length + ' operations',
                h('label', { class: 'ka-lc-toggle' }, [h('input', { type: 'checkbox', class: 'kd-labels-on' }), ' edge labels'])]),
            h('div', { class: 'kd-flow', html: f.svg }),
            h('div', { class: 'ka-note', text: f.caption }),
            h('div', { class: 'ka-src', text: 'Derived reads, listed, not drawn: ' + (f.reads || []).join(', ') }),
            fold(['edges (' + f.edges.length + ')'], f.edges.map(function (e) {
                return h('div', { class: 'ka-lc-row', 'data-lc-edge': '' }, [
                    h('code', { text: e.from + ' → ' + e.to }),
                    e.sequence.length ? h('span', { class: 'ka-lc-dim', text: ' sequences: ' + e.sequence.join(', ') }) : null,
                    e.walk.length ? fold([' walk paths: ' + e.walk.length], [h('div', { class: 'ka-src', text: e.walk.join(', ') })],
                        { class: 'ka-lc-inline' }) : null
                ]);
            }), { class: 'ka-lc-fold' })
        ]));

        box.appendChild(h('div', { class: 'ka-headline', text: 'What next, by state' }));
        v.states.forEach(function (st) {
            box.appendChild(fold([h('code', { text: st.state }), h('span', { class: 'ka-lc-dim', text: ' ' + st.worlds + ' worlds ' })]
                .concat(st.counts.map(function (c) { return h('span', { class: 'ka-lc-o ka-lc-o--' + c.outcome, text: c.outcome + ' ' + c.n }); }))
                .concat(st.noneEnabled ? [h('span', { class: 'ka-lc-dim', text: ' no command enabled in this state' })] : []),
                st.commands.map(function (c) {
                    return h('div', { class: 'ka-lc-row ka-lc-cmd' }, [
                        h('code', { text: c.command }),
                        h('span', { class: 'ka-lc-dim', text: c.operation || '—' }),
                        h('span', { class: 'ka-lc-dim', text: c.inputs.length ? 'in: ' + c.inputs.join(', ') : 'no input' }),
                        h('div', {}, c.outcomes.map(function (o) {
                            return h('div', {}, [outcomeBadge(o.outcome), h('span', { class: 'ka-lc-dim', text: ' ' + window.kaLifecycleOutcomeText(o) })]);
                        }))
                    ]);
                }), { class: 'ka-lc-fold', 'data-lc-state': st.state }));
        });

        box.appendChild(h('div', { class: 'ka-headline', text: 'Per operation' }));
        v.opList.forEach(function (op) {
            box.appendChild(fold([h('code', { text: op.opId }), h('span', { class: 'ka-lc-dim', text: ' ' + op.command + ' · '
                + op.tuples + ' tuples · ' + op.refusals + ' refusals · ' + op.errors + ' errors ' }),
                op.req ? h('span', { class: 'ka-chip ka-chip--sm', text: op.req }) : null],
                op.states.map(function (st) {
                    return fold([h('code', { text: 'from ' + st.state }), h('span', { class: 'ka-lc-dim', text: ' ' + st.tuples.length
                        + ' tuples · ' + (st.refusals.length + st.errors.length) + ' refusal/error rows' })],
                        st.tuples.map(function (row) {
                            var t = row.t;
                            return h('div', { class: 'ka-lc-row', 'data-lc-tuple': t.id }, [
                                outcomeBadge('applied'), ' ',
                                h('code', { text: t.argsClass + '  ' + t.fromStates.join('/') + ' → ' + t.toStates.join('/') }), ' ',
                                h('span', { class: 'ka-chip ka-chip--sm', text: t.grade }),
                                t.witness ? h('span', { class: 'ka-chip ka-chip--sm ka-chip--boundary', text: 'witness: ' + t.witness }) : null,
                                h('span', { class: 'ka-lc-dim', text: ' ' + t.id }),
                                h('div', { class: 'ka-lc-dim', text: 'members ' + t.members.map(function (m) { return m.from + '→' + m.to; }).join(', ')
                                    + (t.differing.length ? ' · differing ' + t.differing.join(', ') : '')
                                    + (t.maskedFields.length ? ' · masked ' + t.maskedFields.join(', ') : '')
                                    + (t.sequences.length ? ' · sequences ' + t.sequences.join(', ') : '') }),
                                row.values ? h('div', { class: 'ka-lc-dim', text: row.values }) : null
                            ]);
                        }).concat(st.refusals.concat(st.errors).map(function (row) {
                            var r = row.r;
                            return h('div', { class: 'ka-lc-row' }, [outcomeBadge(r.status),
                                h('span', { class: 'ka-lc-dim', text: ' ' + window.kaLifecycleOutcomeText(r) + ' · ×' + r.count
                                    + ' · from ' + r.fromStates.join('/') }),
                                row.values ? h('div', { class: 'ka-lc-dim', text: row.values }) : null]);
                        })), { class: 'ka-lc-fold ka-lc-inner', 'data-lc-op-state': st.state });
                }), { class: 'ka-lc-fold', 'data-lc-op': op.opId }));
        });

        box.appendChild(h('div', { class: 'ka-cards' }, [
            card('Model-only commands', v.modelOnly.map(function (m) {
                return h('li', {}, [h('code', { text: m.command }), ' — ' + m.reason + (m.operation ? ' (' + m.operation + ')' : '')]);
            }), 'None — every command joins an operation.'),
            card('Behaviour-unmodelled operations', v.unmodelled.map(function (u) {
                return h('li', {}, [h('code', { text: u })]);
            }), 'None — every operation is modelled.')
        ]));
    }

    // the refused exchanges the live record holds, harvested at read (§17.5) — absent until one exists
    function twinWireRefusals(root, t) {
        var rows = t.wireRefusals || [];
        if (!rows.length) {
            return;
        }
        root.appendChild(h('div', { class: 'ka-headline', text: 'What the service refused' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Every refusal the running service returned during the recorded replays, in its own words.' }));
        dataTable(root, ['Command', 'Status', 'Error', 'Message', 'Why the model refused'], rows.map(function (w) {
            var b = w.body;
            var obj = b && typeof b === 'object' && !Array.isArray(b) ? b : null;
            var err = obj ? (obj.error != null ? obj.error : obj.code) : null;
            var msg = obj ? (obj.message != null ? obj.message : obj.detail) : null;
            return h('tr', {}, [
                h('td', {}, [h('code', { text: w.command })]),
                h('td', { class: 'ka-num', text: fmt(w.status) }),
                // the raw body only where the shape names neither an error nor a message
                h('td', {}, [err != null ? h('span', { text: fmt(err) })
                    : h('span', { class: 'ka-src', text: b === undefined ? '–' : JSON.stringify(b) })]),
                h('td', { text: msg != null ? fmt(msg) : '–' }),
                h('td', { text: (w.reasons || []).join(', ') || '–' })
            ]);
        }));
    }

    function twinWalk(root, ex) {
        var st = ex.states || {}, tr = ex.transitions || {}, fi = ex.fidelity || {}, stats = ex.stats || {};
        var guards = fi.guards || {}, invs = fi.invariants || {}, steps = fi.steps || {};
        root.appendChild(h('div', { class: 'ka-headline', text: 'What the walk reached' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'The states and the moves between them the walk got to under this model, and the ones it never did.' }));
        var panel = h('div', { class: 'ka-panel' });
        twinRow(panel, 'states reached', (st.reached || []).length, chips(st.reached));
        twinRow(panel, 'states not reached', (st.notReached || []).length, chips(st.notReached));
        twinRow(panel, 'transitions observed', (tr.observed || []).length, null);
        twinRow(panel, 'transitions not observed', (tr.notObserved || []).length,
            chips((tr.notObserved || []).map(function (e) { return e.from + ' → ' + e.command + ' → ' + e.to; })));
        // plain labels, and the gap rather than the raw counter where the gap is what reads (D254):
        // `observedFalse` short of `exercised` means the walk never reached a world blocking that guard
        twinRow(panel, 'how thoroughly it was checked', null, chips(fidelityChips(guards, invs, steps, fi)));
        twinRow(panel, 'size of the walk', null, chips(['worlds visited ' + fmt(stats.nodes),
            'moves taken ' + fmt(stats.edges), 'refusals ' + fmt(stats.refusals),
            'errors ' + fmt(stats.errors)]));
        root.appendChild(panel);

        var ces = ex.counterexamples || [];
        root.appendChild(h('div', { class: 'ka-headline', text: 'Where the model breaks its own rules' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Each row is an order of commands the walk found that ends with a rule of the model broken.' }));
        if (!ces.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: 'No invariant or postcondition failed on a reached path.' }));
            return;
        }
        dataTable(root, ['What broke', 'Name', 'Command', 'Steps', 'The order that breaks it'], ces.map(function (c) {
            return h('tr', {}, [
                h('td', {}, [badge(c.kind, 'bad')]),
                h('td', { text: c.name || '' }),
                h('td', {}, [h('code', { text: c.command || '' })]),
                h('td', { class: 'ka-num', text: fmt((c.path || []).length) }),
                h('td', {}, [stepList(c.path), h('span', { class: 'ka-src', text: 'node ' + fmt(c.node) })])
            ]);
        }));
    }

    function fidelityChips(guards, invs, steps, fi) {
        var open = (fi.undeclaredOpenFields || []).map(function (f) {
            return f && f.field != null ? String(f.field) : String(f);
        });
        return [
            'guards exercised ' + fmt(guards.exercised),
            'guards never seen false ' + fmt(num(guards.exercised) - num(guards.observedFalse)),
            'guards that threw ' + fmt(guards.notChecked),
            'invariants run ' + fmt(invs.ran),
            'invariants failed ' + fmt(invs.failed),
            'steps checked against the rules ' + fmt(steps.oracle),
            'steps not checked ' + fmt(steps.bare),
            'clock ' + fmt(fi.clock),
            'open fields undeclared: ' + (open.join(', ') || 'none')
        ];
    }

    function twinFindings(root, ck) {
        var ev = ck.evidence || {}, f = ck.findings || {}, ci = ck.ci || {};
        root.appendChild(h('div', { class: 'ka-headline', text: 'Evidence' }));
        // D270 a: four aggregates with four denominators — never summed, never one percentage
        root.appendChild(h('div', { class: 'ka-note', text:
            'Four separate counts, each over its own population — they are never added up into one number.' }));
        root.appendChild(h('div', { class: 'ka-panel' }, [
            evidenceRow('model', ev.model, 'declared states reached'),
            evidenceRow('oracle', ev.oracle, 'per step'),
            evidenceRow('mock', ev.mock, 'sequences replayed against a mock'),
            evidenceRow('live', ev.live, 'sequences observed against an independent system')
        ]));

        root.appendChild(h('div', { class: 'ka-headline', text: 'Findings' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'What the check found against the model, and — separately — what this reading does not cover.' }));
        var cards = h('div', { class: 'ka-cards' });
        cards.appendChild(card('Required rows never reached — widen the model or the walk', (f.requiredNotreached || []).map(function (r) {
            var why = [r.status, r.reason, r.node != null ? 'node ' + fmt(r.node) : null,
                r.edge ? r.edge.from + ' → ' + r.edge.command + ' → ' + r.edge.to : null];
            return h('li', {}, [r.id + ' · ' + r.kind, h('span', { class: 'ka-src', text:
                why.filter(function (x) { return x != null && x !== ''; }).join(' · ') })]);
        }), 'Every required row was observed.'));
        cards.appendChild(card('Blessed sequences that moved — decide which reading is right', (f.sequenceDrift || []).map(function (d) {
            return h('li', {}, ['Sequence ' + fmt(d.sequenceId) + ' answers differently at step '
                + fmt(num(d.at) + 1) + ' — it was ' + fmt(d.prev) + ', it is now ' + fmt(d.now) + '.',
                h('span', { class: 'ka-src', text: 'sequenceDrift · at ' + fmt(d.at) })]);
        }), 'No blessed sequence moved.'));
        cards.appendChild(card('Disclosures — what this reading does not cover', (f.incomplete || []).map(function (i) {
            return h('li', {}, [disclosureText(i), h('span', { class: 'ka-src', text: disclosureDetail(i) })]);
        }), 'Nothing was left uncovered.'));
        root.appendChild(cards);
        twinLiveMismatch(root, f, ev);

        root.appendChild(h('div', { class: 'ka-headline' }, ['CI verdict ', badge(ci.verdict || '–', verdictKind(ci.verdict))]));
        root.appendChild(h('div', { class: 'ka-card' }, [
            (ci.reasons || []).length
                ? h('ul', {}, ci.reasons.map(function (r) { return h('li', { text: r }); }))
                : h('div', { class: 'ka-empty', text: 'No finding — every graded obligation was met.' })
        ]));
    }

    // the live seam, as a table rather than a JSON blob: where it happened, what each side said
    function twinLiveMismatch(root, f, ev) {
        var rows = f.liveMismatch || [];
        root.appendChild(h('div', { class: 'ka-headline', text: 'Where the service and the model disagree' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Each row is one step where the running service did something the model does not allow — decide which '
            + 'side is wrong; a disagreement already adjudicated against the model appears under Disclosures instead.' }));
        if (!rows.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: ((ev.live || {}).of || 0)
                ? 'No live replay disagreed with the model.'
                : 'No live seam yet — nothing was observed against an independent system.' }));
            return;
        }
        dataTable(root, ['Found by', 'Where', 'What the service did', 'What the model allows', 'Replayed against'],
            rows.map(function (m) {
                return h('tr', {}, [
                    h('td', { class: 'ka-who' }, [mismatchWho(m), m.evidence
                        ? h('span', { class: 'ka-src', text: 'evidence ' + trunc(String(m.evidence), 12) }) : null]),
                    h('td', { text: mismatchStep(m) }),
                    h('td', { class: 'ka-said', text: mismatchService(m) }),
                    h('td', { class: 'ka-said', text: mismatchModel(m) }),
                    h('td', { text: m.against === 'live' ? 'a live service' : 'a mock' })
                ]);
            }));
    }

    function evidenceRow(grade, pair, label) {
        pair = pair || {};
        return h('div', { class: 'ka-dim' }, [
            h('div', { class: 'ka-dim-head' }, [
                h('span', { class: 'ka-dim-path', text: grade }),
                h('span', { class: 'ka-type', text: label })
            ]),
            chips([fmt(pair.count) + ' / ' + fmt(pair.of)])
        ]);
    }

    // no replay here — a static report renders the rail, Twin.run is the console's verb
    function twinSequences(root, t) {
        root.appendChild(h('div', { class: 'ka-headline', text: 'Saved sequences' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'The orders of commands kept on file — every check replays each of them against the current model.' }));
        var rows = t.sequences || [];
        if (!rows.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: 'No saved sequence.' }));
            twinProvenance(root, t);
            return;
        }
        var live = (t.live && t.live.rows) || [];
        dataTable(root, ['Id', 'What it covers', 'Where it came from', 'Steps', 'Expected result', 'Live replay'], rows.map(function (r) {
            var kind = (r._from && r._from.kind) || '';
            return h('tr', {}, [
                h('td', {}, [h('code', { text: r._id || '' })]),
                h('td', { class: 'ka-seq-label', text: r._label || '' }),
                h('td', { text: ORIGIN_LABEL[kind] || kind }),
                h('td', {}, [stepList(r.steps)]),
                h('td', { text: r._expect ? 'declared' : '–' }),
                liveCell(liveView(live.filter(function (x) { return x.sequenceId === r._id; })[0]))
            ]);
        }), 'ka-seqs');
        twinProvenance(root, t);
    }

    // the live record's reading of one saved sequence (§17.6, D319) — freshness and disposition are two
    // vocabularies, never merged. The wording is duplicated verbatim in the console's twinLive.
    var LIVE_FRESHNESS = {
        STALE: 'the rules or the model changed after the run',
        UNREPRODUCED: 'the recorded run could not be replayed step for step',
        CORRUPT: 'the recorded run does not match its own record',
        VERSION: 'the run was recorded in an earlier format',
        ungraded: 'the walk did not finish, so the run was not graded' };
    function liveView(r) {
        if (!r) return null;
        var d = r.disposition, s = r.shrink;
        var out = { freshness: r.freshness === 'current' ? '' : r.freshness + ' — ' + (LIVE_FRESHNESS[r.freshness] || 'not current'),
            kind: '', head: '', reason: '', mismatch: '', shrink: '', steps: [] };
        if (!d) return out;
        var mm = d.mismatch || {};
        out.kind = d.kind;
        out.head = d.kind + (d.code ? ' ' + d.code : '')
            + (d.at == null ? '' : mm.kind === 'readBackExplainsNoCandidate' ? ' · at the closing read-back' : ' · at step ' + (d.at + 1))
            + ' · against ' + (r.against === 'live' ? 'a live service' : 'a mock')
            + (r.sutRevision ? ' · revision ' + r.sutRevision : '');
        out.reason = d.reason || '';
        if (d.mismatch) {
            out.mismatch = mm.kind === 'refusalReasonMismatch'
                ? 'the service refused — ' + mm.observed + '; the model refuses for ' + (mm.model || []).join(', ')
                : 'the service ' + mismatchService({ mismatch: mm }) + '; ' + mismatchModel({ mismatch: mm });
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
    }
    function liveCell(v) {
        if (!v) return h('td', { class: 'ka-live', 'data-live': 'none' }, [h('span', { class: 'ka-src', text: 'not run live' })]);
        var kindCls = v.kind === 'PASS' ? 'ok' : v.kind === 'FAIL' ? 'bad' : 'warn';
        return h('td', { class: 'ka-live', 'data-live': v.kind || 'unread' }, [
            v.freshness ? h('div', { class: 'ka-live-fresh', text: v.freshness }) : null,
            v.head ? h('div', {}, [badge(v.kind, kindCls), h('span', { class: 'ka-live-head', text: v.head.slice(v.kind.length).replace(/^ · /, ' ') })]) : null,
            v.mismatch ? h('div', { text: v.mismatch }) : null,
            v.reason ? h('div', { class: 'ka-src', text: v.reason }) : null,
            v.shrink ? h('div', { class: 'ka-src', text: v.shrink }) : null,
            v.steps.length ? stepList(v.steps) : null
        ]);
    }

    // the file versions this replay was read against — labelled, muted, and never needed to read the
    // section above it
    function twinProvenance(root, t) {
        var p = t.provenance;
        if (!p) {
            return;
        }
        root.appendChild(h('div', { class: 'ka-src', text: 'replay context — ' + Object.keys(p).map(function (k) {
            return (PROVENANCE_LABEL[k] || k) + ' ' + trunc(String(p[k]), 8);
        }).join(' · ') }));
    }

    // ---- Mutation (mutation-testing §4 self-run · §5 the report home · §6 the rails) — DATA.mutation
    // is the self-run report verbatim, so nothing here re-derives a number.
    var MUT_LEGEND = 'KILLED — an oracle noticed the defect · SURVIVED — none did (the worklist) · '
        + 'SCREENED — no probe row distinguishes it, never claimed equivalent · '
        + 'NOTCOVERED / TIMEOUT / INVALID — outside both denominators.';
    var MUT_BOUNDS = 'This grades assertion strength, never oracle correctness — and a self-run grades '
        + 'the book’s OWN oracles, so its numbers are never comparable with a suite-graded score.';
    var MUT_READ = 'A pinned premium locks one saved row; a stated rule must hold for every policy. '
        + 'Read both, never add them.';
    var EXCLUDED = ['SCREENED', 'NOTCOVERED', 'INVALID', 'TIMEOUT'];

    // the survivor groups, in the order a team closes them — the group decides the fix, so it is
    // decided from the survivor's own shape (a linked criterion · a lookup value · a bare site)
    var MUT_GROUPS = [
        { id: 'req', label: 'leave a requirement criterion undefended',
            fix: 'pin the counterexample row’s premium, derived by hand from the filed tables, or '
                + 'declare the rule as a calc.always property' },
        // a file-table survivor splits on the Cells block's read trace (D326(f)); an inline one keeps 'cell'
        { id: 'cell-read', label: 'are table cells a saved case reads, undefended',
            fix: 'pin the reading case’s premium, derived by hand from the filed table (a cell the Cells tab '
                + 'marks masked needs a case whose premium it moves), or declare the rule as a calc.always property' },
        { id: 'cell-unread', label: 'are table cells no saved case reads',
            fix: 'save a case that reads each, with a hand-derived premium — the Cells tab lists them' },
        { id: 'cell', label: 'are lookup values no saved case reads',
            fix: 'save one case per value with a hand-derived premium, or declare the rule that builds '
                + 'the lookup as one calc.always property, which defends every value at once' },
        { id: 'site', label: 'are decision sites with no criterion linked',
            fix: 'link the arm with calc.req, then pin or declare' }
    ];

    function pct(v) { return v == null ? '–' : (Math.round(v * 1000) / 10) + '%'; }

    // a rate factor keeps every digit it was authored with — fmt() would round 1.0125 to 1.01
    function mutVal(v) {
        if (typeof v === 'number') return Number.isInteger(v) ? v.toLocaleString('en-US') : String(v);
        return fmt(v);
    }

    // the two rates read side by side, each carrying its own numerator over the ONE denominator —
    // they are never added, averaged or otherwise blended (§4)
    function score(value, count, den, label, note) {
        return h('div', { class: 'ka-score' }, [
            h('div', { class: 'ka-score-val', text: pct(value) }),
            h('div', { class: 'ka-score-lbl', text: label }),
            h('div', { class: 'ka-score-den', text: count + ' of ' + den + ' checked mutants' }),
            h('div', { class: 'ka-score-note', text: note })
        ]);
    }

    function viewMutation(root) {
        var m = DATA.mutation || {}, counts = m.counts || {}, survivors = m.worklist || [];
        root.appendChild(h('div', { class: 'ka-head' }, [
            h('h1', { text: 'Mutation' }),
            badge(m.kind === 'self-run' ? 'self-run — the rulebook’s own oracles' : String(m.kind || ''), 'gen')
        ]));
        root.appendChild(h('div', { class: 'ka-headline', text:
            'Would the rulebook’s own oracle book notice a seeded defect?' }));

        mutationNextSteps(root, survivors, m.denominator);

        root.appendChild(h('div', { class: 'ka-note', text: MUT_READ }));
        root.appendChild(h('div', { class: 'ka-scores' }, [
            score(m.independentScore, m.independentlyKilled, m.denominator, 'Independent mutation score',
                'caught by a stated rule (calc.always / calc.sometimes)'),
            score(m.rawKillRate, m.rawKilled, m.denominator, 'Raw kill rate',
                'caught by anything, a pinned premium included')
        ]));
        // a relational kill moved a figure: its capture-reuse reading beside each rate, the disclosure once
        var cr = m.relations && m.relations.captureReuseReading;
        if (cr) {
            root.appendChild(h('div', { class: 'ka-note', text: 'Capture-reuse reading: independent '
                + pct(cr.independentScore) + ' (' + cr.independentlyKilled + ' of ' + m.denominator + ') · raw '
                + pct(cr.rawKillRate) + ' (' + cr.rawKilled + ' of ' + m.denominator + '). '
                + String(m.relations.disclosure || '') }));
        }
        // a status the report carries no count for cannot arise in this lane (NOTCOVERED: every
        // screening-surviving mutant is reached by a probe row) — no chip, rather than a dash to explain
        root.appendChild(h('div', { class: 'ka-legend' }, EXCLUDED.filter(function (s) {
            return counts[s] != null;
        }).map(function (s) {
            return h('span', {}, [h('span', { class: 'ka-chip', text: s + ' ' + counts[s] })]);
        }).concat([h('span', { text: 'checked = KILLED + SURVIVED; these sit outside both denominators' })])));
        root.appendChild(h('div', { class: 'ka-note', text: MUT_BOUNDS, style: 'margin-top:12px' }));

        mutationSurvivors(root, survivors, m.mutants || []);
        mutationTable(root, m.mutants || []);
        root.appendChild(h('div', { class: 'ka-legend' }, [h('span', { text: MUT_LEGEND })]));
    }

    function mutGroup(w) {
        if ((w.reqs || []).length) return 'req';
        var site = String(w.site || ''), reads = DATA.mutationReads || {}, address = site.slice(7);
        if (site.indexOf('lookup.') !== 0) return 'site';
        return reads[address] === true ? 'cell-read' : reads[address] === false ? 'cell-unread' : 'cell';
    }

    // the survivors as the work they imply, above the scores: the count, then one card per group
    // carrying the one edit that closes it
    function mutationNextSteps(root, rows, den) {
        root.appendChild(h('div', { class: 'ka-headline', text: 'What to do next' }));
        if (!rows.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: 'No survivor — every checked mutant was noticed.' }));
            return;
        }
        root.appendChild(h('div', { class: 'ka-note', text: rows.length + ' of ' + den
            + ' checked defects would go unnoticed.' }));
        var cards = h('div', { class: 'ka-cards' });
        MUT_GROUPS.forEach(function (g) {
            var hits = rows.filter(function (w) { return mutGroup(w) === g.id; });
            if (!hits.length) return;
            var reqs = [];
            hits.forEach(function (w) {
                (w.reqs || []).forEach(function (r) { if (reqs.indexOf(r) < 0) reqs.push(r); });
            });
            cards.appendChild(h('div', { class: 'ka-card' }, [
                h('h3', {}, [badge(String(hits.length), 'bad'), 'survivors ' + g.label]),
                reqs.length ? h('div', { class: 'ka-chips' }, reqs.map(function (r) {
                    return h('span', { class: 'ka-chip ka-chip--sm', text: r });
                })) : null,
                h('div', { class: 'ka-note', style: 'margin:8px 0 0', text: g.fix })
            ]));
        });
        root.appendChild(cards);
    }

    // the seeded defect said in business words — the mutant carries the operator and the numbers,
    // the worklist entry the site
    function mutationPhrase(w, x) {
        var site = w.site || x.site || '', op = w.mutatorName || x.mutatorName || '';
        var at = x.line > 0 ? ', calc.js line ' + x.line : '';
        var ex = x.excerpt || site, from = x.original, to = x.mutated;
        if (site.indexOf('lookup.') === 0) {
            var f = Number(from), t = Number(to);
            var verb = f && t === f * 2 ? 'doubled' : f && t === f / 2 ? 'halved' : 'changed';
            return 'lookup value ' + site.slice(7) + ' ' + verb + ' '
                + mutVal(from) + ' → ' + mutVal(to);
        }
        if (op === 'CONDITIONALS_BOUNDARY') {
            return 'the comparison ' + ex + ' moved (' + from + ' became ' + to + ')' + at;
        }
        if (op === 'NEGATE_CONDITIONALS') {
            return 'the comparison ' + ex + ' inverted (' + from + ' became ' + to + ')' + at;
        }
        if (op === 'REMOVE_CONDITIONALS') {
            return 'the branch ' + ex + ' switched off' + at;
        }
        return ex + (from != null ? ' changed ' + mutVal(from) + ' → ' + mutVal(to) : ' changed') + at;
    }

    // "row N: {json} -> {json}" → only the values that moved, premium (else the last one) last
    function divergenceShift(s) {
        var body = String(s || '').replace(/^row \d+:\s*/, ''), i = body.indexOf(' -> ');
        if (i < 0) return null;
        var a, b;
        try {
            a = JSON.parse(body.slice(0, i));
            b = JSON.parse(body.slice(i + 4));
        } catch (e) {
            return null;
        }
        var ao = a.output || {}, bo = b.output || {}, shifts = [];
        Object.keys(ao).concat(Object.keys(bo)).forEach(function (k) {
            if (shifts.some(function (x) { return x.k === k; })) return;
            if (JSON.stringify(ao[k]) !== JSON.stringify(bo[k])) {
                shifts.push({ k: k, text: k + ' ' + mutVal(ao[k]) + ' → ' + mutVal(bo[k]) });
            }
        });
        if (a.outcome !== b.outcome) {
            shifts.push({ k: 'outcome', text: 'outcome ' + mutVal(a.outcome) + ' → ' + mutVal(b.outcome) });
        }
        if (!shifts.length) return null;
        var pi = shifts.map(function (x) { return x.k; }).indexOf('premium');
        if (pi >= 0) shifts.push(shifts.splice(pi, 1)[0]);
        return shifts.map(function (x) { return x.text; }).join(' · ');
    }

    // each survivor names the acceptance criterion it leaves undefended (the ArmClaims join) and the
    // first probe row on which it diverges — a ready-made scenario candidate
    function mutationSurvivors(root, rows, mutants) {
        var byId = {};
        mutants.forEach(function (x) { byId[x.id] = x; });
        root.appendChild(h('div', { class: 'ka-headline', text: 'Survivors — what nothing in the book would notice' }));
        root.appendChild(h('div', { class: 'ka-note', text:
            'Each row carries the criterion it leaves undefended and the counterexample that proves it: '
            + 'store that row as a scenario with an _expect, or declare the property that forbids it.' }));
        if (!rows.length) {
            root.appendChild(h('div', { class: 'ka-empty', text: 'No survivor — every checked mutant was noticed.' }));
            return;
        }
        dataTable(root, ['What would slip through', 'Criterion left undefended', 'Row that would show it'],
            rows.map(function (w) {
                var ce = w.counterexample || {}, input = ce.input || {}, shift = divergenceShift(ce.divergence);
                return h('tr', {}, [
                    h('td', { class: 'ka-wrap', title: w.id || '', text: mutationPhrase(w, byId[w.id] || {}) }),
                    h('td', {}, (w.reqs || []).length
                        ? (w.reqs || []).map(function (r) { return h('span', { class: 'ka-chip', text: r }); })
                        : [h('span', { class: 'ka-empty', text: 'no calc.req at this site' })]),
                    // a row outside the declared universe decides nothing (D241): shown, never offered as a case
                    h('td', { class: 'ka-wrap' }, [
                        h('div', { class: 'ka-chips' }, Object.keys(input).map(function (k) {
                            return h('span', { class: 'ka-chip ka-chip--sm', text: k + ': ' + mutVal(input[k]) });
                        })),
                        h('span', { class: 'ka-src', text: shift || ce.divergence || '' })
                    ].concat(ce.domain === 'out' ? [
                        h('div', {}, [badge('outside the declared domain — do not store this row', 'warn')]),
                        h('span', { class: 'ka-src', text: w.fix || '' })
                    ] : []))
                ]);
            }));
    }

    function mutationTable(root, mutants) {
        root.appendChild(h('div', { class: 'ka-headline', text: 'Every mutant, every verdict' }));
        root.appendChild(h('div', { class: 'ka-note', text: mutants.length + ' seeded defects over the '
            + 'rulebook’s decision sites and its rate book.' }));
        // no Mutant column: the id is exactly operator@site, and the duplicate is what pushed this
        // table past the window. A mutant noticed by a dozen oracles names the first and counts the
        // rest — the full list rides the cell's title.
        dataTable(root, ['Operator', 'Site', 'Excerpt', 'Change', 'Verdict', 'Kill class', 'Detected by'],
            mutants.map(function (x) {
                var os = (x.oracles || []).map(function (o) { return o.kind + ': ' + o.name; });
                var by = os.length > 1 ? os[0] + ' + ' + (os.length - 1) + ' more' : os.join('');
                return h('tr', {}, [
                    h('td', { text: x.mutatorName || '' }),
                    h('td', { title: x.id || '' }, [h('code', { text: (x.site || '') + (x.line > 0 ? ' · line ' + x.line : '') })]),
                    h('td', { class: 'ka-wrap', title: x.excerpt || '' }, [h('code', { text: trunc(x.excerpt, 44) })]),
                    h('td', {}, [h('code', { text: x.original != null ? x.original + ' → ' + x.mutated : '–' })]),
                    h('td', {}, [badge(String(x.status || ''), x.status === 'KILLED' ? 'ok'
                        : x.status === 'SURVIVED' ? 'bad' : 'warn')]),
                    h('td', { text: x.killClass ? x.killClass + (x.relation
                        ? ' · relational (' + (x.relation.axes || []).join(', ') + ')' : '') : '–' }),
                    h('td', { class: 'ka-wrap', title: os.join(' · '), text: by || (x.statusReason || '–') })
                ]);
            }));
    }

    // ---- Match (rules-analysis §"Rating tables" — the premium match, D328) — DATA.match, latest ingest per sheet ----
    var MATCH_KIND = { 'match': 'ok', 'match (±x)': 'ok', 'mismatch': 'bad', 'rated but expected refused': 'bad',
        'not rated': 'warn', 'unknown case': 'warn' };
    var MATCH_ORDER = ['mismatch', 'rated but expected refused', 'unknown case', 'not rated', 'match (±x)', 'match'];

    function viewMatch(root) {
        DATA.match.forEach(function (m, i) {
            if (i > 0) root.appendChild(h('hr', { style: 'border:0;border-top:1px solid var(--ka-border);margin:26px 0' }));
            matchSheet(root, m);
        });
    }

    function matchSheet(root, m) {
        var ing = m.ingest || {}, map = m.mapping || {}, counts = m.counts || {}, d = m.disclosures || {};
        var fig = map.figure || {};
        root.appendChild(h('div', { class: 'ka-headline', html: m.checked
            ? 'Premium match: <span class="ka-pct">' + m.matched + ' of ' + m.rated + ' rated cases match</span> · '
                + m.of + ' cases on the sheet'
            : 'Not checked — no case on the sheet was rated' }));
        root.appendChild(h('div', { class: 'ka-note', text: 'Sheet ' + String((m.sheet || {}).digest || '').slice(0, 12)
            + ' · mapping ' + (map.name || 'inline') + ' (' + fig.name + ' ↔ ' + fig.field + ', scale ' + fig.scale
            + (fig.tolerance != null ? ', tolerance ±' + fig.tolerance : '') + ')'
            + ' · ingested ' + (ing.startedMs ? new Date(ing.startedMs).toISOString().replace('T', ' ').slice(0, 19) : '?')
            + ' · run ' + (ing.runId || '?') + ' · the latest ingest of this sheet.' }));
        var notes = [];
        if (d.mockOnly) notes.push([badge('mock', 'warn'), ' The results came from a karate mock — a stand-in, not the carrier system.']);
        if (d.mappingUnblessed) notes.push([badge('unblessed', 'warn'), ' The mapping is AI-drafted and not yet blessed.']);
        (d.lint || []).forEach(function (l) { notes.push([badge('lint', 'warn'), ' ' + l]); });
        if ((d.editions || []).length) {
            notes.push([badge('edition', 'warn'), ' ' + count(d.editions.length, 'case') + ' rated on a different edition: '
                + d.editions.map(function (e) { return e.case + ' (ours ' + e.ours + ', theirs ' + e.theirs + ')'; }).join('; ')]);
        }
        notes.forEach(function (n) { root.appendChild(h('div', { class: 'ka-note' }, n)); });

        root.appendChild(h('div', { class: 'ka-chips', style: 'margin-bottom:16px' }, MATCH_ORDER.slice().reverse()
            .filter(function (k) { return counts[k]; }).map(function (k) { return badge(counts[k] + ' ' + k, MATCH_KIND[k]); })));

        var cases = (m.cases || []).slice().sort(function (a, b) {
            return MATCH_ORDER.indexOf(a['class']) - MATCH_ORDER.indexOf(b['class']);
        });
        var max = DATA.rowsMax || cases.length, shown = cases.slice(0, max), trs = [];
        shown.forEach(function (c) {
            trs.push(h('tr', {}, [
                h('td', { title: c.label || '' }, [h('code', { text: c['case'] != null ? c['case'] : 'row ' + c.row })]),
                h('td', {}, [badge(c['class'], MATCH_KIND[c['class']])]),
                h('td', { class: 'ka-wrap', text: [c.reason, c.error].filter(Boolean).join(': ') || '–' }),
                h('td', { class: 'ka-num', text: c.ours != null ? c.ours : '–' }),
                h('td', { class: 'ka-num', text: c.theirs != null ? String(c.theirs) : '–' }),
                h('td', { class: 'ka-num', text: c.difference != null ? c.difference : '–' }),
                h('td', { class: 'ka-wrap', text: (c.reqs || []).join(', ') || '–' })
            ]));
            if (c.diagnosis) trs.push(h('tr', {}, [h('td', { colspan: '7', class: 'ka-wrap' }, matchDiagnosis(c.diagnosis))]));
        });
        dataTable(root, ['Case', 'Class', 'Reason', 'Ours', 'Theirs', 'Difference', 'Criteria'], trs);
        if (shown.length < cases.length) {
            root.appendChild(h('div', { class: 'ka-note', text: 'Showing ' + shown.length + ' of ' + cases.length
                + ' cases — Rule.match(\'' + m.book + '\') returns every one.' }));
        }
        matchLinkage(root, m.cases || []);
    }

    function matchDiagnosis(dg) {
        var out = [], w = dg.worksheet || {}, c = dg.cells || {};
        if (w.status === 'aligned') {
            var divs = w.divergences || [];
            out.push(h('div', {}, [h('strong', { text: 'Worksheet: ' }), w.aligned + ' steps aligned · '
                + (divs.length ? (w.first ? 'first divergence ' + w.first : 'divergences unordered (a carrier-only step cannot be placed)')
                    : 'no step diverges') + ((w.unmapped || []).length ? ' · unmapped: ' + w.unmapped.join(', ') : '')]));
            if (divs.length) {
                out.push(h('ol', { class: 'ka-steps' }, divs.map(function (x) {
                    return h('li', {}, [h('code', { text: x.step }), ' ' + x.kind
                        + (x.side ? ' (' + x.side + ' only)' : '') + (x.reason ? ' (' + x.reason + ')' : '')
                        + (x.ours != null ? ' · ours ' + x.ours : '') + (x.theirs != null ? ' · theirs ' + x.theirs : '')
                        + (x.difference != null ? ' · Δ ' + x.difference : '')]);
                })));
            }
        } else {
            out.push(h('div', {}, [h('strong', { text: 'Worksheet: ' }), w.status === 'unavailable'
                ? 'alignment unavailable — step ' + w.step + ' repeats on the ' + w.side + ' side'
                : 'not available — ' + ({ 'no-structured-steps': 'the book tags no calc.log step',
                    'no-carrier-worksheet': 'the carrier sent no worksheet' }[w.reason] || w.reason)]));
        }
        if (c.status === 'ranked') {
            var cells = c.cells || [];
            out.push(h('div', {}, [h('strong', { text: 'Cells read (a ranking, not a cause): ' })].concat(
                cells.slice(0, 12).map(function (x) {
                    return h('span', { class: 'ka-chip ka-chip--sm' + (x.rank !== 'read' ? ' ka-chip--boundary' : ''),
                        title: x.rank, text: x.address });
                }), cells.length > 12 ? [' +' + (cells.length - 12) + ' more'] : [])));
        } else {
            out.push(h('div', {}, [h('strong', { text: 'Cells: ' }), 'not checked — ' + (c.reason || '')]));
        }
        return out;
    }

    // the requirement linkage, read off each case's reqs — never evidence on the criterion itself
    function matchLinkage(root, cases) {
        var byReq = {};
        cases.forEach(function (c) {
            (c.reqs || []).forEach(function (r) {
                var e = byReq[r] || (byReq[r] = { cases: 0, matched: 0, mismatched: 0 });
                e.cases++;
                if (c['class'] === 'match' || c['class'] === 'match (±x)') e.matched++;
                else if (c['class'] === 'mismatch' || c['class'] === 'rated but expected refused') e.mismatched++;
            });
        });
        var reqs = Object.keys(byReq).sort();
        if (!reqs.length) return;
        root.appendChild(h('div', { class: 'ka-headline', style: 'margin-top:22px', text: 'Requirement linkage' }));
        root.appendChild(h('div', { class: 'ka-note', text: 'The criteria each case’s run reached. A linkage only: '
            + 'the match is not evidence on the criterion, and the traceability matrix does not change.' }));
        dataTable(root, ['Criterion', 'Cases', 'Matched', 'Mismatched', 'Other'], reqs.map(function (r) {
            var e = byReq[r];
            return h('tr', {}, [h('td', {}, [h('code', { text: r })]), h('td', { class: 'ka-num', text: fmt(e.cases) }),
                h('td', { class: 'ka-num', text: fmt(e.matched) }), h('td', { class: 'ka-num', text: fmt(e.mismatched) }),
                h('td', { class: 'ka-num', text: fmt(e.cases - e.matched - e.mismatched) })]);
        }));
    }

    // ---- Editions (rules-analysis §"Rating tables" — the selection check, D331) — DATA.editions, one check per records file ----
    var FINDING_KIND = { 'ok': 'ok', 'competing': 'bad', 'ambiguous': 'bad', 'not-filed': 'bad', 'orphan-companion': 'bad', 'none-selectable': 'warn' };
    var FINDING_ORDER = ['none-selectable', 'competing', 'ambiguous', 'orphan-companion', 'not-filed', 'ok'];

    function editionsCount() {
        var findings = 0, checked = false;
        DATA.editions.forEach(function (e) {
            if (e.error) return;
            checked = checked || e.checked;
            (e.windows || []).forEach(function (w) { if (w.finding !== 'ok') findings++; });
        });
        return checked ? findings : 'not checked';
    }

    function viewEditions(root) {
        DATA.editions.forEach(function (e, i) {
            if (i > 0) root.appendChild(h('hr', { style: 'border:0;border-top:1px solid var(--ka-border);margin:26px 0' }));
            editionsCheck(root, e);
        });
    }

    function editionsCheck(root, e) {
        if (e.error) {
            root.appendChild(h('div', { class: 'ka-headline', text: 'Not checked — ' + e.records + ': ' + e.error }));
            root.appendChild(h('div', { class: 'ka-note', text: e.code === 'RULE_UNDECLARED'
                ? 'Declare the selection rule at evidence/ratebooks/rule.json — nothing defaults.' : 'Refused ' + e.code + '.' }));
            return;
        }
        var counts = e.counts || {}, wins = e.windows || [], ne = e.notEvaluated || [], d = e.disclosures || {}, rule = e.rule || {};
        var findings = wins.filter(function (w) { return w.finding !== 'ok'; }).length;
        root.appendChild(h('div', { class: 'ka-headline', html: e.checked
            ? 'Selection check: <span class="ka-pct">' + (findings ? count(findings, 'finding') : 'every window ok') + '</span> over '
                + count(wins.length, 'window') + (ne.length ? ' · ' + ne.length + ' not evaluated' : '')
            : 'Not checked — no window could be evaluated' }));
        root.appendChild(h('div', { class: 'ka-note', text: 'Records ' + e.records + ' · rule ' + String(rule.sha || '').slice(0, 12)
            + ' · filed from ' + ((d.filed || {}).source || '?') + ' · the winner is the latest selectable edition on or before each window’s start.' }));
        var notes = [];
        if (rule.ai) notes.push([badge('unblessed', 'warn'), ' The selection rule is AI-drafted and not yet blessed.']);
        (d.nameDates || []).forEach(function (n) {
            notes.push([badge('name date', 'warn'), ' ' + n.edition + ' is named for ' + n.nameDate + ' but effective ' + n.effective + '.']);
        });
        if ((d.unmapped || []).length) notes.push([badge('unmapped', 'warn'), ' Ids outside editionIds: ' + d.unmapped.join(', ')]);
        if ((d.outOfScope || []).length) notes.push([badge('out of scope', 'warn'), ' Records outside the declared scope: ' + d.outOfScope.join(', ')]);
        notes.forEach(function (n) { root.appendChild(h('div', { class: 'ka-note' }, n)); });
        root.appendChild(h('div', { class: 'ka-chips', style: 'margin-bottom:16px' }, FINDING_ORDER.slice().reverse()
            .filter(function (k) { return counts[k]; }).map(function (k) { return badge(counts[k] + ' ' + k, FINDING_KIND[k]); })));
        var sorted = wins.slice().sort(function (a, b) { return FINDING_ORDER.indexOf(a.finding) - FINDING_ORDER.indexOf(b.finding); });
        dataTable(root, ['Jurisdiction', 'Line', 'Transaction', 'From', 'To', 'Winner', 'Filed winner', 'Finding'], sorted.map(function (w) {
            return h('tr', {}, [
                h('td', { text: w.jurisdiction }), h('td', { text: w.line }), h('td', { text: w.transaction }),
                h('td', { class: 'ka-num', text: w.from || '–' }), h('td', { class: 'ka-num', text: w.to || 'open' }),
                h('td', { class: 'ka-wrap' }, [editionCell(w.winner)]), h('td', { class: 'ka-wrap' }, [editionCell(w.filedWinner)]),
                h('td', { class: 'ka-wrap' }, [badge(w.finding, FINDING_KIND[w.finding]), w.competing ? ' ' + w.competing.join(', ')
                    : w.tied ? ' ' + w.tied.join(' = ') : w.orphans ? ' ' + w.orphans.join(', ') : ''])
            ]);
        }));
        if (ne.length) {
            root.appendChild(h('div', { class: 'ka-headline', style: 'margin-top:22px', text: 'Not evaluated' }));
            dataTable(root, ['Jurisdiction', 'Line', 'Transaction', 'From', 'To', 'Why'], ne.map(function (w) {
                return h('tr', {}, [
                    h('td', { text: w.jurisdiction }), h('td', { text: w.line }), h('td', { text: w.transaction }),
                    h('td', { class: 'ka-num', text: w.from || '–' }), h('td', { class: 'ka-num', text: w.to || 'open' }),
                    h('td', { class: 'ka-wrap', text: w.reason === 'pre-first' ? 'before the first record boundary'
                        : (w.reasons || []).map(function (r) { return r.reason + ': ' + r.edition + (r.status ? ' (' + r.status + ')' : ''); }).join('; ')
                            || w.reason + (w.changeSets ? ': ' + w.changeSets.join(', ') : '') })
                ]);
            }));
        }
    }

    function editionCell(c) {
        if (!c) return h('span', { text: '–' });
        return h('span', {}, [h('code', { text: c.edition + (c.version ? ' v' + c.version : '') }),
            (c.companions || []).length ? ' + ' + c.companions.join(', ') : '', ' · ' + c.date]);
    }

    // ---- Tables (rules-analysis §"Rating tables" — the table diff, D333) — DATA.tables, the latest diff per evidence set ----
    var CELL_KIND = { CELL_AS_DECLARED: 'ok', CELL_DECLARED_MISSING: 'bad', CELL_WRONG_VALUE: 'bad', CELL_UNEXPECTED: 'bad', TABLE_FILE_UNEXPECTED: 'bad' };
    var CELL_LABEL = { CELL_AS_DECLARED: 'as declared', CELL_DECLARED_MISSING: 'declared but missing', CELL_WRONG_VALUE: 'wrong value',
        CELL_UNEXPECTED: 'unexpected', TABLE_FILE_UNEXPECTED: 'unexpected file' };

    function tablesCount() {
        var breaking = 0, checked = false;
        DATA.tables.forEach(function (t) { checked = checked || t.checked; breaking += (t.counts || {}).breaking || 0; });
        return checked ? breaking : 'not checked';
    }

    function viewTables(root) {
        DATA.tables.forEach(function (t, i) {
            if (i > 0) root.appendChild(h('hr', { style: 'border:0;border-top:1px solid var(--ka-border);margin:26px 0' }));
            tableDiff(root, t);
        });
    }

    function cellValue(v) { return v === null || v === undefined ? '–' : String(v); }

    function tableDiff(root, t) {
        var c = t.counts || {}, ne = t.notEvaluated || [], d = t.disclosures || {}, stale = t.stale || {};
        var findings = (t.breaking || []).concat(t.compatible || []);
        root.appendChild(h('div', { class: 'ka-headline', html: t.checked
            ? 'Table diff: <span class="ka-pct">' + (c.breaking ? count(c.breaking, 'defect') : 'every cell as declared') + '</span> over '
                + count(c.cells, 'cell') + (c.compatible ? ' · ' + c.compatible + ' as declared' : '') + (ne.length ? ' · ' + ne.length + ' not evaluated' : '')
            : 'Not checked — no cell could be compared' }));
        root.appendChild(h('div', { class: 'ka-note', text: t.against + ' against tables/' + (t.base ? t.base + '/' : '') + ' with ' + (t.changeSets.length ? t.changeSets.join(' → ') : 'no change set')
            + ' · book ' + String((t.digests.book || {}).sha256 || '').slice(0, 12) + ' · evidence ' + String((t.digests.evidence || {}).sha256 || '').slice(0, 12) }));
        if (stale.book) root.appendChild(h('div', { class: 'ka-note' }, [badge('stale', 'warn'), ' The book moved since this diff ran.']));
        if (stale.evidence) root.appendChild(h('div', { class: 'ka-note' }, [badge('stale', 'warn'), ' The evidence set moved since this diff ran.']));
        (d.masked || []).forEach(function (m) {
            root.appendChild(h('div', { class: 'ka-note' }, [badge('masked', 'warn'), ' ' + m.table + ' ' + m.key.join('/') + ' ' + m.column
                + ': ' + m.changeSet + ' set ' + cellValue(m.value) + ', masked by ' + m.maskedBy + '.']));
        });
        (d.skipped || []).forEach(function (s) {
            root.appendChild(h('div', { class: 'ka-note' }, [badge('skipped', 'warn'), ' ' + s.file + ': the import skipped '
                + s.skipped.map(function (r) { return 'row ' + r.row + ' (' + r.reason + ')'; }).join(', ') + '.']));
        });
        if ((d.unaccounted || []).length) root.appendChild(h('div', { class: 'ka-note' }, [badge('unaccounted', 'warn'), ' No import accounting: ' + d.unaccounted.join(', ')]));
        if (t.truncated) root.appendChild(h('div', { class: 'ka-note' }, [badge('truncated', 'warn'), ' Lists cut at the row cap; the counts are over every cell.']));
        root.appendChild(h('div', { class: 'ka-chips', style: 'margin-bottom:16px' }, Object.keys(CELL_LABEL)
            .filter(function (k) { return c[k]; }).map(function (k) { return badge(c[k] + ' ' + CELL_LABEL[k], CELL_KIND[k]); })));
        if (findings.length) {
            dataTable(root, ['Table', 'File', 'Row', 'Column', 'Grade', 'Expected', 'Actual', 'Change set'], findings.map(function (f) {
                var loc = f.location || {};
                return h('tr', {}, [
                    h('td', {}, [h('code', { text: f.table })]), h('td', { class: 'ka-wrap', text: loc.file || '' }),
                    h('td', { text: loc.row || '' }), h('td', { text: loc.column || '' }),
                    h('td', {}, [badge(CELL_LABEL[f.kind] || f.kind, CELL_KIND[f.kind])]),
                    h('td', { class: 'ka-num', text: cellValue(f.expected) }), h('td', { class: 'ka-num', text: f.kind === 'TABLE_FILE_UNEXPECTED' ? count(f.actual.rows, 'row') : cellValue(f.actual) }),
                    h('td', { class: 'ka-id', text: f.changeSet || '' })
                ]);
            }));
        }
        if (ne.length) {
            root.appendChild(h('div', { class: 'ka-headline', style: 'margin-top:22px', text: 'Not evaluated' }));
            dataTable(root, ['Reason', 'Where', 'Change set', 'Why'], ne.map(function (n) {
                var where = n.construct ? (n.table ? n.table + ' ' : '') + n.construct.key + (n.construct.facet ? ' ' + n.construct.facet : '') : '';
                var why = n.reason === 'before-mismatch' ? 'before ' + cellValue(n.before) + ', found ' + cellValue(n.found) + (n.foundFrom ? ' (from ' + n.foundFrom + ')' : '')
                    : n.reason === 'no-change-set' ? count((d.raw || []).length, 'raw difference') + ' listed, none declared'
                    : typeof n.detail === 'string' ? n.detail : n.detail ? JSON.stringify(n.detail) : '';
                return h('tr', {}, [h('td', {}, [badge(n.reason, 'warn')]), h('td', { class: 'ka-wrap', text: where }),
                    h('td', { class: 'ka-id', text: n.changeSet || '' }), h('td', { class: 'ka-wrap', text: why })]);
            }));
        }
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', render);
    else render();
})();
