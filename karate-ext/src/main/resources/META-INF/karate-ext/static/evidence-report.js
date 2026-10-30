/*
 * karate-max report ext — client-side renderer for the `evidence` embed (reports.md D135).
 *
 * The unified emit seam (EvidenceEmit, Java) lands a `.js`/UI check's assertion as a JSON embed on the
 * in-flight Gherkin step: { name:"evidence", parts:[{ mime:"application/json", data:<base64-json> }] }.
 * Core defers the renderer until the embed scrolls into view and calls this via
 * KarateReport.registerEmbed (see EXT.md § Embeds). We decode the JSON and draw a compact pass/fail
 * evidence row — the label (the assertion's intent), the verb (the strength ladder), and, on failure,
 * the actual-vs-expected diff + the match message. Until this registers, core's generic JSON fallback
 * shows the raw payload, so the seam degrades gracefully.
 *
 * Styling reuses the report's Tailwind utilities (the summary page is a Tailwind/Alpine app) — no
 * vendored CSS, so this stays a single self-contained script.
 */
(function () {
    'use strict';

    var KarateEvidence = {

        // core hands us {name, parts, meta} + the report api (for _esc); only visible embeds render.
        render: function (embed, api) {
            var payload = this._payload(embed);
            if (!payload) {
                return '';   // nothing decodable — let nothing show rather than a broken card
            }
            return payload.kind === 'step'
                ? this._stepRow(payload, api)
                : this._assertionRow(payload, api);
        },

        // the JSON rides as the (base64) data of the primary application/json part
        _payload: function (embed) {
            var part = (embed.parts || []).find(function (p) {
                return (p.mime || '').indexOf('json') >= 0 && p.data;
            });
            if (!part) {
                return null;
            }
            try {
                return JSON.parse(this._b64utf8(part.data));
            } catch (e) {
                return null;
            }
        },

        // base64 -> UTF-8 string (atob alone mangles multi-byte chars; the payload may carry any text)
        _b64utf8: function (b64) {
            var bin = atob(b64);
            try {
                var bytes = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) {
                    bytes[i] = bin.charCodeAt(i);
                }
                return new TextDecoder('utf-8').decode(bytes);
            } catch (e) {
                return bin;   // no TextDecoder (very old engine) — best effort
            }
        },

        _assertionRow: function (a, api) {
            var esc = api._esc.bind(api);
            var pass = a.pass !== false;
            var tone = pass
                ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-900/20'
                : 'border-rose-500 bg-rose-50 dark:bg-rose-900/20';
            var icon = pass ? '&#10003;' : '&#10007;';
            var iconTone = pass ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400';
            // the `assert` keyword is not a match verb, so it is never badged as one
            var badge = a.verb === 'assert' ? 'assert' : ('match.' + esc(a.verb || 'equals'));
            var title = a.label ? esc(a.label) : (a.actualExpr ? esc(a.actualExpr) : badge);

            var h = '<div class="border-l-4 ' + tone + ' rounded-r pl-3 pr-2 py-1.5 my-1 text-sm">';
            h += '<div class="flex items-center gap-2">';
            h += '<span class="font-bold ' + iconTone + '">' + icon + '</span>';
            h += '<span class="font-medium text-slate-800 dark:text-slate-100">' + title + '</span>';
            h += '<span class="ml-auto text-xs font-mono px-1.5 py-0.5 rounded bg-slate-200 dark:bg-slate-700 '
                + 'text-slate-600 dark:text-slate-300">' + badge + '</span>';
            h += '</div>';

            // pass = show the verified value; fail = show actual vs expected + the engine message.
            // The step path carries no `actual` on a pass (it would bloat every run), so the expected
            // side stands in — and if that too is absent, the expression as written.
            if (pass) {
                h += this._kv('value', a, 'actual', esc, 'mt-1')
                    || this._kv('expected', a, 'expected', esc, 'mt-1')
                    || (a.expectedExpr ? this._line('expected', esc(String(a.expectedExpr)), 'mt-1') : '');
            } else {
                h += this._kv('actual', a, 'actual', esc, 'mt-1');
                h += this._kv('expected', a, 'expected', esc, 'mt-0.5');
                if (a.message) {
                    h += '<div class="mt-1 text-xs text-rose-700 dark:text-rose-300 font-mono whitespace-pre-wrap">'
                        + esc(String(a.message)) + '</div>';
                }
            }
            h += '</div>';
            return h;
        },

        _stepRow: function (s, api) {
            var esc = api._esc.bind(api);
            var ok = s.status == null || s.status === 'pass' || s.status === 'ok';
            var iconTone = ok ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400';
            var h = '<div class="border-l-4 border-slate-300 dark:border-slate-600 rounded-r pl-3 pr-2 py-1 my-1 text-sm '
                + 'bg-slate-50 dark:bg-slate-800/40">';
            h += '<span class="font-bold ' + iconTone + '">' + (ok ? '&#9679;' : '&#10007;') + '</span> ';
            h += '<span class="text-slate-800 dark:text-slate-100">' + esc(String(s.name || 'step')) + '</span>';
            if (s.note) {
                h += '<span class="ml-2 text-xs text-slate-500 dark:text-slate-400">' + esc(String(s.note)) + '</span>';
            }
            h += '</div>';
            return h;
        },

        // one payload field as a labeled line, by PRESENCE of its key: a value too big to carry was
        // omitted with its byte count in a `<key>Truncated` sibling; a key that is simply absent renders
        // nothing at all — never the string "undefined".
        _kv: function (label, a, key, esc, cls) {
            var json;
            if (Object.prototype.hasOwnProperty.call(a, key)) {
                try {
                    json = JSON.stringify(a[key]);
                } catch (e) {
                    json = String(a[key]);
                }
                if (json === undefined) {
                    json = String(a[key]);   // an undefined value serializes to nothing
                }
            } else if (typeof a[key + 'Truncated'] === 'number') {
                json = a[key + 'Truncated'] < 0
                    ? '(omitted)'
                    : '(omitted, ' + a[key + 'Truncated'] + ' bytes)';
            } else {
                return '';
            }
            return this._line(label, esc(json), cls);
        },

        // a labeled value line: <label> <mono body> — scalars inline, long values on a new line
        _line: function (label, body, cls) {
            var multiline = body.length > 60;
            return '<div class="' + cls + ' ' + (multiline ? '' : 'flex gap-2 items-baseline') + '">'
                + '<span class="text-xs uppercase tracking-wide text-slate-400 dark:text-slate-500">' + label + '</span> '
                + '<code class="text-xs text-slate-700 dark:text-slate-200">' + body + '</code></div>';
        }
    };

    window.KarateEvidence = KarateEvidence;

    if (window.KarateReport && typeof window.KarateReport.registerEmbed === 'function') {
        window.KarateReport.registerEmbed('evidence', function (embed, api) {
            return KarateEvidence.render(embed, api);
        });
    }
})();
