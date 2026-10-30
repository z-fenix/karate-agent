/* Mutation report renderer (mutation-testing SPEC §4/§6) — vanilla JS over the
 * window.KARATE_MUTATION_DATA island. One page, two lanes off the same shape: the suite-graded
 * kill check (independentScore + rawKillRate) and the headless self-run (selfRunScore). Score
 * tiles + the §6 framing rail on the face, the worklist as the centerpiece, the full mutant
 * table below. No framework, no network, no CDN — opens off disk. */
(function () {
    'use strict';
    var DATA = window.KARATE_MUTATION_DATA || {};
    var SELF = DATA.kind === 'self-run';

    function h(tag, attrs, children) {
        var e = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (k) {
                if (k === 'class') e.className = attrs[k];
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
    function pct(v) { return v == null ? '–' : (Math.round(v * 1000) / 10) + '%'; }

    function tile(val, label, note, hero) {
        return h('div', { class: 'km-tile' + (hero ? ' km-tile--hero' : '') }, [
            h('div', { class: 'km-val', text: val }),
            h('div', { class: 'km-lbl', text: label }),
            note ? h('div', { class: 'km-note', text: note }) : null
        ]);
    }
    function statusChip(status) {
        return h('span', { class: 'km-status km-' + status, text: status });
    }

    function render() {
        var app = document.getElementById('app');
        var wrap = h('div', { class: 'km-wrap' });
        var counts = DATA.counts || {};
        var worklist = DATA.worklist || [];
        var mutants = DATA.mutants || [];

        var TAMPER = DATA.kind === 'tamper';
        wrap.appendChild(h('div', { class: 'km-head' }, [
            h('h1', {}, ['Mutation Report ', h('span', { class: 'km-rule', text: DATA.rulebook || DATA.subject || '' })]),
            h('span', { class: 'km-lane', text: SELF ? 'self-run — the rulebook’s own oracles'
                : TAMPER ? 'response tamper — ' + (DATA.seam || '') + (DATA.rung ? ' (rung ' + DATA.rung + ')' : '')
                : 'suite-graded' })
        ]));
        wrap.appendChild(h('div', { class: 'km-sub' }, [
            SELF ? 'Would the rulebook’s OWN oracle book (stored _expect expectations + calc.always/sometimes properties) notice a seeded defect? '
                 : TAMPER ? 'Would this suite’s assertions notice the live API’s responses being perturbed in flight? '
                 : 'Would this suite’s assertions notice a seeded defect in the rules? ',
            'Close the worklist below, re-run, watch the score climb.'
        ]));

        // ---- score tiles ----
        var tiles = h('div', { class: 'km-tiles' });
        if (SELF) {
            tiles.appendChild(tile(pct(DATA.selfRunScore), 'Self-run score',
                'noticed ÷ checked mutants (' + DATA.noticed + ' / ' + DATA.denominator + ')', true));
            tiles.appendChild(tile(String(counts.SURVIVED || 0), 'Survived',
                'defects nothing in the book would notice'));
            tiles.appendChild(tile(String(counts.SCREENED || 0), 'Screened out',
                'indistinguishable on the probe universe'));
        } else {
            // with a relational kill, each rate shows its capture-reuse reading beside it (§4)
            var cr = DATA.relations ? DATA.relations.captureReuseReading : null;
            tiles.appendChild(tile(pct(DATA.independentScore), 'Independent score',
                'independent kills ÷ checked mutants (' + DATA.independentlyKilled + ' / ' + DATA.denominator + ')'
                + (cr ? ' · capture-reuse reading ' + pct(cr.independentScore) + ' (' + cr.independentlyKilled + ')' : ''), true));
            tiles.appendChild(tile(pct(DATA.rawKillRate), 'Raw kill rate',
                'every kill, echoes and recomputation included (' + DATA.rawKilled + ' / ' + DATA.denominator + ')'
                + (cr ? ' · capture-reuse reading ' + pct(cr.rawKillRate) + ' (' + cr.rawKilled + ')' : '')));
            tiles.appendChild(tile(String(counts.SURVIVED || 0), 'Survived',
                'defects no assertion noticed'));
            tiles.appendChild(tile(String((counts.NOTCOVERED || 0)), 'Not covered',
                'sites no request reached — coverage gaps, outside the score'));
        }
        tiles.appendChild(tile(String(worklist.length), 'Worklist',
            'the next-step artifact — every checked mutant not ' + (SELF ? 'noticed' : 'independently killed')));
        wrap.appendChild(tiles);

        // ---- the §6 framing rail (always on the face) ----
        wrap.appendChild(h('div', { class: 'km-rail' }, [
            h('strong', {}, ['What this number means: ']),
            String(DATA.framing || '')
        ]));

        if (DATA.relations) {
            wrap.appendChild(h('div', { class: 'km-rail' }, [
                h('strong', {}, ['Relations: ']), String(DATA.relations.disclosure || '')
            ]));
        }

        // ---- the D233 rung disclosures (every face says what the run could NOT see) ----
        (DATA.disclosures || []).forEach(function (d) {
            wrap.appendChild(h('div', { class: 'km-rail' }, [
                h('strong', {}, ['Disclosure: ']), String(d)
            ]));
        });

        // ---- status chip strip ----
        var chips = h('div', { class: 'km-chips' });
        ['KILLED', 'SURVIVED', 'NOTCOVERED', 'SCREENED', 'TIMEOUT', 'INVALID'].forEach(function (s) {
            if (counts[s] != null) {
                chips.appendChild(h('span', { class: 'km-chip km-' + s, text: s + ' ' + counts[s] }));
            }
        });
        wrap.appendChild(chips);

        // ---- the worklist (centerpiece) ----
        var wl = h('div', { class: 'km-section' });
        wl.appendChild(h('h2', {}, ['Worklist — what nothing would notice']));
        wl.appendChild(h('p', { class: 'km-hint', text: SELF
            ? 'Each row carries a ready-made counterexample: store it as a scenario with an _expect, or declare the property that forbids it.'
            : 'Strengthen these assertions (frozen literals / shape locks on the computed outputs), then re-run.' }));
        if (worklist.length === 0) {
            wl.appendChild(h('div', { class: 'km-empty', text: 'Empty — every checked mutant is ' + (SELF ? 'noticed by the book.' : 'independently killed.') }));
        } else {
            var t = h('table', { class: 'km-table' });
            t.appendChild(h('thead', {}, [h('tr', {}, [
                h('th', {}, ['Mutant']), h('th', {}, ['Site']), h('th', {}, ['Requirement']),
                h('th', {}, ['Finding']), h('th', {}, ['Status'])
            ])]));
            var tb = h('tbody');
            worklist.forEach(function (w) {
                var finding = h('td', {}, [String(w.finding || '')]);
                if (w.fix) finding.appendChild(h('div', { class: 'km-fix', text: w.fix }));
                if (w.hint) finding.appendChild(h('div', { class: 'km-fix', text: w.hint }));
                if (w.counterexample) {
                    finding.appendChild(h('div', { class: 'km-cx', text: JSON.stringify(w.counterexample.input)
                        + '\n' + String(w.counterexample.divergence || '') }));
                }
                tb.appendChild(h('tr', {}, [
                    h('td', { class: 'km-mono', text: w.id || '' }),
                    h('td', { class: 'km-mono', text: w.site || '' }),
                    h('td', {}, (w.reqs && w.reqs.length)
                        ? w.reqs.map(function (r) { return h('span', { class: 'km-req', text: r }); })
                        : ['–']),
                    finding,
                    h('td', {}, [statusChip(String(w.status || 'SURVIVED'))])
                ]));
            });
            t.appendChild(tb);
            wl.appendChild(h('div', { class: 'km-tablewrap' }, [t]));
        }
        wrap.appendChild(wl);

        // ---- the full mutant table (all six statuses) ----
        var mt = h('div', { class: 'km-section' });
        mt.appendChild(h('h2', {}, ['Every mutant, every verdict']));
        mt.appendChild(h('p', { class: 'km-hint', text:
            'KILLED · SURVIVED · NOTCOVERED (excluded from the score) · SCREENED (indistinguishable on the enumerated deck — never claimed equivalent) · TIMEOUT · INVALID.' }));
        var t2 = h('table', { class: 'km-table' });
        t2.appendChild(h('thead', {}, [h('tr', {}, [
            h('th', {}, ['Mutant']), h('th', {}, ['Change']), h('th', {}, ['Status']),
            h('th', {}, [SELF ? 'Noticed by' : 'Killed by']), h('th', {}, ['Detail'])
        ])]));
        var tb2 = h('tbody');
        mutants.forEach(function (m) {
            var by = '';
            if (SELF && m.oracles) {
                by = m.oracles.map(function (o) { return o.kind + ': ' + o.name; }).join(' · ');
            } else if (m.killClass) {
                by = m.killClass + (m.relation ? ' · relational (' + (m.relation.axes || []).join(', ') + ')' : '')
                    + (m.killedBy ? ' — ' + m.killedBy : '');
            }
            tb2.appendChild(h('tr', {}, [
                h('td', { class: 'km-mono', text: m.id || '' }),
                h('td', { class: 'km-mono', text: (m.original != null ? m.original + ' → ' + m.mutated : '') }),
                h('td', {}, [statusChip(String(m.status || ''))]),
                h('td', { class: 'km-mono', text: by }),
                h('td', {}, [String(m.statusReason || '')])
            ]));
        });
        t2.appendChild(tb2);
        mt.appendChild(h('div', { class: 'km-tablewrap' }, [t2]));
        wrap.appendChild(mt);

        wrap.appendChild(h('div', { class: 'km-foot', text:
            'karate-max · Mutation Report · deterministic, offline · '
            + (SELF ? 'self-run and suite-graded scores are never comparable'
                    : 'grades assertion strength against the rulebook — transfer = rulebook fidelity') }));
        app.appendChild(wrap);
    }

    render();
})();
