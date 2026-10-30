// Manual mode — the client half of the Runs ledger and the run page (loaded by manual/runs.html and
// manual/run.html only). Pages are SSR (D88): everything a page shows at load is rendered server-side.
// This file adds only what needs the browser:
//
//   kaManualRuns.runAll / again              a run kickoff — Runner.run over /api/eval (the kickoff
//                                            primitive kaKickSuite from run-modal.js), then open the run
//   kaManualRuns.stop                        Stop — Runner.stop(id) for a set, Harness.stop() for the one
//                                            browser-holding run (a loop or a live run), as run.html does;
//                                            a recording ends through kaManual.stopRecording (manual.js)
//   kaManualRuns.convert                     Turn it into a test — kaManual.turnIntoTest (Harness.author);
//                                            data-m-choice="both" saves that the API test follows
//   kaManualRuns.moveCall / keepSite         the calls panel: POST /ui/traffic/{move,host} (Traffic.sift with
//                                            the tester's choices), then the panel re-renders in place
//   kaManualRuns.finishWritten               finish a test the AI just wrote: stamp the criteria New test
//                                            asked for (kaManual.linkAuthored), then re-render once
//   kaManualRuns.deleteTest                  delete a written test file (POST /ui/checks/delete, D98)
//   kaManualRuns.watch                       the in-flight view: the run's own SSE topic feeds a
//                                            plain-language action log, the elapsed time, the attempt
//                                            count and the AI figures; the run settling reloads the page
//                                            into its finished shape
window.kaManualRuns = (function () {

  // a message lands beside the button that caused it (its block's data-m-msg), else under the header
  function say(msg, near) {
    var box = near && near.closest ? near.closest('.m-run-convert, .m-run-foot, .m-calls') : null;
    var el = (box && box.querySelector('[data-m-msg]')) || document.querySelector('[data-m-error]');
    if (el) el.textContent = msg || '';
  }

  function busy(btn, label) {
    if (!btn) return function () {};
    var was = btn.textContent;
    btn.disabled = true;
    btn.textContent = label;
    return function () { btn.disabled = false; btn.textContent = was; };
  }

  // `verify`: the replay is the Verify stage of a test just written, so its page carries the stage strip
  function open(runId, verify) {
    location.href = '/manual/runs/' + encodeURIComponent(runId) + (verify ? '?verify=1' : '');
  }

  // one kickoff path for every "run it" button: a set of paths, async, then open the new run
  function start(selector, opts, btn) {
    say('', btn);
    var restore = busy(btn, 'Starting…');
    return kaKickSuite(selector, Object.assign({ async: true }, opts || {}))
      .then(function (out) {
        if (!out || !out.runId) throw new Error((out && out.error) || 'the run did not start');
        open(out.runId, btn && btn.hasAttribute('data-m-verify'));
      })
      .catch(function (e) { restore(); say(String(e && e.message || e), btn); });
  }

  // "Run the tests I have" — the Tests list as one set; the video bracket is on, as the console modal's is
  function runAll(btn) {
    var selector = JSON.parse(btn.getAttribute('data-m-selector') || '[]');
    if (!selector.length) return;
    start(selector, { video: true }, btn);
  }

  // Run again: a set replays its recorded selection with the options it ran under (D281 c); a single
  // test — a written, repaired or replayed one — runs that file
  function again(btn) {
    var test = btn.getAttribute('data-m-test');
    if (test) { start([test], { video: true }, btn); return; }
    var selector = JSON.parse(btn.getAttribute('data-m-selector') || '[]');
    var opts = JSON.parse(btn.getAttribute('data-m-options') || '{}');
    if (selector.length) start(selector, opts, btn);
  }

  // Turn it into a test: the name typed beside the button overrides the one New test saved. A name a test
  // already has is refused (EXISTS) and replaced only once the person says so; Try again carries
  // data-m-overwrite, since it writes the same test again
  function convert(btn) {
    var scope = btn.closest('.m-run-convert, .m-run-foot') || document;
    var input = scope.querySelector('input[data-m-name]');
    var name = input ? input.value.trim() : (btn.getAttribute('data-m-name') || '');
    if (!name) { say('Name the test first.', btn); if (input) input.focus(); return; }
    say('', btn);
    var restore = busy(btn, 'Starting…');
    var run = btn.getAttribute('data-m-run');
    var overwrite = btn.getAttribute('data-m-overwrite') === 'true';
    var o = { name: name };
    if (overwrite) o.overwrite = true;
    if (btn.getAttribute('data-m-choice')) o.choice = btn.getAttribute('data-m-choice');
    kaManual.turnIntoTest(run, o)
      .catch(function (e) {
        if (e && e.code === 'EXISTS' && confirm('A test named ' + name + ' already exists. Replace it with the one '
            + 'written from this recording?')) {
          return kaManual.turnIntoTest(run, Object.assign({}, o, { overwrite: true }));
        }
        if (e && e.code === 'EXISTS') {
          restore(); say('A test with this name already exists. Type another name.', btn); if (input) input.focus();
          return;
        }
        throw e;
      })
      .catch(function (e) { restore(); say(String(e && e.message || e), btn); });
  }

  // once per run and tab: a link the header does not come to show must not reload the page again and again
  function linkWritten(el) {
    if (!el || !window.kaManual || !kaManual.linkAuthored) return Promise.resolve(false);
    var meta = JSON.parse(el.getAttribute('data-m-meta') || 'null');
    var key = 'm-run-linked-' + (meta && meta.check) + '-' + (meta && meta.req);
    try { if (sessionStorage.getItem(key)) return Promise.resolve(false); sessionStorage.setItem(key, '1'); }
    catch (e) { return Promise.resolve(false); }
    return kaManual.linkAuthored(meta).then(function (out) {
      return !!(out && !out.error && out.changed);
    });
  }

  function finishWritten() {
    var linkEl = document.querySelector('[data-m-link-written]');
    if (!linkEl) return;
    linkWritten(linkEl).then(function (changed) { if (changed) location.reload(); });
  }

  // the calls panel: each control is one /ui/traffic call; its answer is the sift the page renders, so the panel
  // re-renders from the server (kaManual.refresh swaps the data-m-live section)
  function traffic(btn, route, body) {
    var panel = btn.closest('[data-m-source]');
    if (!panel) return;
    say('', btn);
    var restore = busy(btn, 'Saving…');
    fetch('/ui/traffic/' + route, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(Object.assign({ source: panel.getAttribute('data-m-source') }, body))
    }).then(function (r) { return r.json(); })
      .then(function (out) {
        if (!out || out.error) throw new Error(out && out.error ? out.error : 'Nothing changed. Reload the page, then try again.');
        kaManual.refresh();
      })
      .catch(function (e) { restore(); say(String(e && e.message || e), btn); });
  }
  function moveCall(btn) { traffic(btn, 'move', { id: btn.getAttribute('data-m-id'), to: btn.getAttribute('data-m-move') }); }
  function keepSite(btn) { traffic(btn, 'host', { host: btn.getAttribute('data-m-host') }); }

  // Left out stays open across the panel's re-render when the tester opened it
  var LEFT_OPEN = 'm-calls-left-open';
  if (typeof document != 'undefined' && document.addEventListener) document.addEventListener('toggle', function (e) {
    if (!e.target.matches || !e.target.matches('[data-m-left-out]')) return;
    try { if (e.target.open) sessionStorage.setItem(LEFT_OPEN, '1'); else sessionStorage.removeItem(LEFT_OPEN); } catch (x) {}
  }, true);
  function reopen() {
    var d = document.querySelector('[data-m-left-out]');
    try { if (d && sessionStorage.getItem(LEFT_OPEN)) d.open = true; } catch (x) {}
  }
  if (typeof document != 'undefined' && document.addEventListener) {
    document.addEventListener('DOMContentLoaded', reopen);
    document.addEventListener('m-swap', reopen);
  }

  function stop(btn) {
    var id = btn.getAttribute('data-m-run');
    var via = btn.getAttribute('data-m-stop');
    say('', btn);
    var restore = busy(btn, 'Stopping…');
    var call = via === 'suite' ? karate.value('Runner.stop(' + JSON.stringify(id) + ')')
      : via === 'record' ? kaManual.stopRecording(id)
      : karate.value('Harness.stop()');
    Promise.resolve(call)
      .then(function (out) {
        if (out && out.attribution === 'uncertain') {
          say('The browser\'s clock changed while recording: a call may be credited to the wrong action.', btn);
        }
        setTimeout(function () { settleReload(id, true); }, 4000);
      })
      .catch(function (e) { restore(); say(String(e && e.message || e), btn); });
  }

  function deleteTest(btn) {
    var name = btn.getAttribute('data-m-test');
    if (!name || !confirm('Delete ' + name + '? The file is removed from the project.')) return;
    say('', btn);
    var restore = busy(btn, 'Deleting…');
    fetch('/ui/checks/delete', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name: name })
    }).then(function (r) { return r.json(); })
      .then(function (out) {
        if (out && out.error) throw new Error(out.error);
        location.reload();
      })
      .catch(function (e) { restore(); say(String(e && e.message || e), btn); });
  }

  // a settled run re-renders in its finished shape — once per run and page load, so a replayed
  // `complete` (SSE backfill) can never loop the reload
  function settleReload(runId, force) {
    var key = 'm-run-settled-' + runId;
    try {
      if (!force && sessionStorage.getItem(key)) return false;
      sessionStorage.setItem(key, '1');
    } catch (e) { /* storage off — a reload still happens once per page */ }
    location.reload();
    return true;
  }

  // ---- the in-flight view -----------------------------------------------------------------------

  function clock(ms) {
    var s = Math.max(0, Math.floor(ms / 1000));
    var m = Math.floor(s / 60);
    return m + ':' + (s % 60 < 10 ? '0' : '') + (s % 60);
  }

  function elapsed(ms) {
    var s = Math.max(0, Math.floor(ms / 1000));
    if (s < 60) return s + 's';
    var m = Math.floor(s / 60);
    if (m < 60) return m + 'm ' + (s % 60 < 10 ? '0' : '') + (s % 60) + 's';
    return Math.floor(m / 60) + 'h ' + (m % 60 < 10 ? '0' : '') + (m % 60) + 'm';
  }

  function thousands(n) {
    return String(Math.round(n || 0)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }

  // the first line of prose in a model reply — code fences and what they hold are skipped, so a reply
  // that is only a code block has none (it headlined as "js")
  function firstLine(t, max) {
    var lines = String(t == null ? '' : t).split('\n');
    var inCode = false;
    for (var i = 0; i < lines.length; i++) {
      var l = lines[i].trim();
      if (/^```/.test(l)) { inCode = !inCode; continue; }
      if (inCode) continue;
      var s = l.replace(/^[#>*_`-]+\s*/, '').trim();
      if (s) return s.length > max ? s.substring(0, max - 1) + '…' : s;
    }
    return '';
  }

  // the first string literal after `at` in a line of JS — a locator, a url, a file name
  function literal(js, at) {
    var m = js.slice(at).match(/(['"`])((?:\\.|(?!\1).)*)\1/);
    return m ? m[2] : null;
  }

  // the words the run page and the ledger share (pub/manual-words.js)
  var short = kaWords.short, shown = kaWords.shown, host = kaWords.host;

  // one line of JS the agent (or a live caller) ran, said as the action it performs
  function plainAction(js) {
    var src = String(js || '').trim();
    var lead = src.match(/^\/\/\s*(.+)/);
    if (lead && lead[1].trim()) return short(lead[1], 140);   // the agent narrates in a leading comment
    var i;
    if ((i = src.search(/\bbot\.(go|navigate)\s*\(/)) >= 0) {
      var url = literal(src, i);
      return url ? 'Opened ' + short(host(url), 90) : 'Opened a page';
    }
    if ((i = src.search(/\bbot\.act\s*\(/)) >= 0) {
      var loc = literal(src, i);
      var verb = (src.slice(i).match(/,\s*['"](\w+)['"]/) || [])[1] || 'click';
      var what = loc ? shown(loc) : 'an element';
      if (/^(type|input|fill|enter)$/i.test(verb)) return 'Typed into ' + what;
      if (/^(select|choose)$/i.test(verb)) return 'Chose an option in ' + what;
      if (/^upload$/i.test(verb)) return 'Uploaded a file to ' + what;
      if (/^(hover)$/i.test(verb)) return 'Pointed at ' + what;
      return 'Clicked ' + what;
    }
    if ((i = src.search(/\bbot\.(click|press)\s*\(/)) >= 0) {
      var target = literal(src, i);
      return 'Clicked ' + (target ? shown(target) : 'an element');
    }
    if (/\bbot\.(type|input|fill)\s*\(/.test(src)) return 'Typed into the page';
    if (/\bbot\.screenshot\s*\(/.test(src)) return 'Took a screenshot';
    if (/\bbot\.(look|snapshot|text|find|read|html|title|url|exists|count)\s*\(/.test(src)) return 'Looked at the page';
    if (/\bbot\.wait/.test(src)) return 'Waited for the page';
    if (/\bbot\.reset\s*\(/.test(src)) return 'Reset the browser';
    if ((i = src.search(/\bCheck\.run\s*\(/)) >= 0) {
      var c = literal(src, i);
      return 'Ran ' + (c ? (c.indexOf('checks/') === 0 ? c : 'checks/' + c) : 'a test');
    }
    if ((i = src.search(/\bFile\.write\s*\(/)) >= 0) return 'Wrote ' + (literal(src, i) || 'a file');
    if ((i = src.search(/\bFile\.read\s*\(/)) >= 0) return 'Read ' + (literal(src, i) || 'a file');
    if (/\bRobot\.success\s*\(/.test(src)) return 'Said the work is done';
    if (/\bRobot\.(fail|blocked)\s*\(/.test(src)) return 'Reported a problem';
    if (/\bRobot\.create\s*\(/.test(src)) return 'Opened the browser';
    if (/\bRecording\.get\s*\(/.test(src)) return 'Read the recording';
    if (/\bRequirement\./.test(src)) return 'Read the requirements';
    if (/\bRule\./.test(src)) return 'Consulted the rules';
    if (/\bmatch\s*\(/.test(src)) return 'Checked a value';
    return 'Ran a step';
  }

  function opensApp(js) { return /\bbot\.(go|navigate)\s*\(/.test(String(js || '')); }

  // one captured human action (the recorder's human_action), said as the run page says a kept step; null for what
  // the log leaves out (a hover, a scroll, the click a label forwards to its own input, opening a list the choice
  // is taken from)
  function plainHuman(d) {
    var a = String(d.action || 'action');
    if (a === 'hover' || a === 'scroll' || a === 'focus' || a === 'blur') return null;
    if (a === 'click' && (d.tag === 'label' || d.tag === 'select')) return null;
    return kaWords.step(d);
  }

  // a tool result that is an error text or an error envelope
  function looksFailed(r) {
    if (r == null) return false;
    var s = String(r).trim();
    if (/^(Error|TypeError|ReferenceError|SyntaxError|RangeError|Exception)\b/.test(s)) return true;
    if (s.charAt(0) === '{') {
      try {
        var o = JSON.parse(s);
        return !!o && (o.ok === false || o.command === 'error' || (typeof o.error === 'string' && !!o.code));
      } catch (e) { return false; }
    }
    return false;
  }

  var DOTS = { done: 'm-dot--passed', failed: 'm-dot--failed', now: 'm-dot--inflight', note: '' };

  function watch(cfg) {
    var root = document.querySelector('[data-m-watch]');
    if (!root || !cfg || !cfg.runId) return;
    var logEl = root.querySelector('[data-m-log]');
    var emptyEl = root.querySelector('[data-m-log-empty]');
    var nowEl = root.querySelector('[data-m-now]');
    var elapsedEl = root.querySelector('[data-m-elapsed]');
    var iterEl = root.querySelector('[data-m-iter]');
    var doneEl = root.querySelector('[data-m-done]');
    var tokensEl = root.querySelector('[data-m-tokens]');
    var thumbsEl = root.querySelector('[data-m-thumbs]');
    var settledEl = root.querySelector('[data-m-settled]');
    var started = cfg.startedMs || Date.now();
    var state = { iter: 0, tokens: cfg.tokens || 0, model: cfg.model || '', done: 0, lastSeq: -1,
                  settled: false, shots: {} };
    var pending = [];      // rows still in flight (purple ring), oldest first
    var scenarios = {};    // suite: scenario key → its row

    function atBottom() { return logEl.scrollHeight - logEl.scrollTop - logEl.clientHeight < 40; }

    function row(text, kind, ts, title) {
      var follow = atBottom();
      if (emptyEl) { emptyEl.remove(); emptyEl = null; }
      var el = document.createElement('div');
      el.className = 'm-row m-row--step';
      var dot = document.createElement('span');
      var main = document.createElement('span');
      main.className = 'm-row-title';
      main.textContent = text;
      if (title) main.title = title;
      var at = document.createElement('span');
      at.className = 'm-run-log-at';
      at.textContent = kind === 'now' ? 'now' : clock((ts || Date.now()) - started);
      el.appendChild(dot); el.appendChild(main); el.appendChild(at);
      var r = { el: el, dot: dot, main: main, at: at, ts: ts || Date.now() };
      mark(r, kind);
      logEl.appendChild(el);
      if (follow) logEl.scrollTop = logEl.scrollHeight;
      return r;
    }

    function mark(r, kind) {
      // a step leaving `now` settles into its colour with one pop (manual.css "motion")
      var settles = r.kind === 'now' && kind !== 'now' && !!DOTS[kind];
      r.kind = kind;
      r.dot.className = 'm-dot' + (DOTS[kind] ? ' ' + DOTS[kind] : '') + (settles ? ' m-dot--settle' : '');
      r.el.classList.toggle('m-run-log-now', kind === 'now');
      if (kind === 'now') { pending.push(r); } else {
        var i = pending.indexOf(r);
        if (i >= 0) pending.splice(i, 1);
        if (r.at.textContent === 'now') r.at.textContent = clock(r.ts - started);   // when it started
      }
    }

    // a new step settles the one before it — the agent moved on, so it ran
    function begin(text, ts, title) {
      unthink();
      pending.slice().forEach(function (p) { mark(p, 'done'); });
      return row(text, 'now', ts, title);
    }

    // while the model works out its next step nothing else happens on screen (a local model can take a
    // minute): one "Thinking" row with the turn's elapsed seconds and, once text streams in, its latest
    // line — the same thinking/text_chunk stream the Dev run page shows in full
    var thinking = null;
    function think(iteration, ts) {
      if (thinking) return;
      var r = row('Thinking about the next step' + (iteration ? ' (attempt ' + iteration + ')' : '') + '…', 'now', ts);
      pending.splice(pending.indexOf(r), 1);    // not a step: nothing settles it but the step it leads to
      var sub = document.createElement('span');
      sub.className = 'm-row-sub m-run-think';
      r.main.parentNode.insertBefore(sub, r.main.nextSibling);
      var main = document.createElement('span');
      main.className = 'm-row-main';
      r.el.insertBefore(main, r.main);
      main.appendChild(r.main); main.appendChild(sub);
      thinking = { row: r, sub: sub, text: '', since: ts || Date.now() };
      thinking.tick = setInterval(function () {
        r.at.textContent = Math.max(0, Math.round((Date.now() - thinking.since) / 1000)) + 's';
      }, 1000);
    }
    function heard(chunk) {
      if (!thinking || !chunk) return;
      thinking.text = (thinking.text + chunk).slice(-2000);
      var lines = thinking.text.split(/\n+/).map(function (l) { return l.replace(/<\/?think>/g, '').trim(); })
        .filter(Boolean);
      var last = lines.length ? lines[lines.length - 1] : '';
      thinking.sub.textContent = last.length > 140 ? '…' + last.slice(-139) : last;
    }
    function unthink() {
      if (!thinking) return;
      clearInterval(thinking.tick);
      thinking.row.el.remove();
      thinking = null;
    }

    function now(text) { if (nowEl && text) { nowEl.textContent = text; nowEl.hidden = false; } }

    // the stage strip is rendered at Write for an authoring run; its first test run is where Verify begins
    function stageAt(at) {
      var stages = document.querySelectorAll('[data-m-stages] .m-stage');
      function apply() {
        stages.forEach(function (li, i) {
          li.classList.toggle('m-stage--done', i < at);
          li.classList.toggle('m-stage--now', i === at);
          if (i === at) li.setAttribute('aria-current', 'step'); else li.removeAttribute('aria-current');
        });
      }
      // the black bubble travels to the new step as a view transition (manual.css "motion")
      var moves = stages[at] && !stages[at].classList.contains('m-stage--now');
      var calm = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (moves && !calm && document.startViewTransition && document.visibilityState === 'visible') {
        document.startViewTransition(apply);
      } else {
        apply();
      }
    }

    function figures() {
      if (iterEl) iterEl.textContent = String(state.iter);
      if (doneEl) doneEl.textContent = String(state.done);
      if (tokensEl) tokensEl.textContent = thousands(state.tokens) + ' tokens' + (state.model ? ' · ' + state.model : '');
    }

    function shot(d) {
      var file = d.filename || d.file;
      if (!thumbsEl || !file || state.shots[file]) return;
      state.shots[file] = true;
      var url = '/api/artifacts/runs/' + encodeURIComponent(cfg.runId) + '/screenshots/' + encodeURIComponent(file);
      var a = document.createElement('a');
      a.className = 'm-run-thumb';
      a.href = url; a.target = '_blank'; a.rel = 'noopener';
      a.title = d.title || 'screenshot';
      var img = document.createElement('img');
      img.src = url; img.alt = d.title || 'screenshot'; img.loading = 'lazy';
      a.appendChild(img);
      thumbsEl.appendChild(a);
      while (thumbsEl.children.length > 3) thumbsEl.removeChild(thumbsEl.firstChild);
      thumbsEl.hidden = false;
    }

    var FINISHED = { completed: 'Finished', failed: 'Finished with a failure', stopped: 'Stopped',
                     terminated: 'Stopped at its limit', blocked: 'Stopped — it could not go on',
                     empty: 'Finished — nothing ran', error: 'Finished with an error' };

    function complete(d) {
      if (state.settled) return;
      state.settled = true;
      unthink();
      pending.slice().forEach(function (p) { mark(p, 'done'); });
      var st = d.status || 'completed';
      row(FINISHED[st] || 'Finished', st === 'completed' ? 'done' : st === 'failed' || st === 'error' ? 'failed' : 'note', d.ts);
      clearInterval(tick);
      if (settledEl) settledEl.hidden = false;
      setTimeout(function () { settleReload(cfg.runId, false); }, 1200);
    }

    var suite = cfg.kind === 'suite';
    var ROUTER = {
      iteration_start: function (d) {
        state.iter = Math.max(state.iter, d.iteration || 0); figures();
        if (!suite) think(d.iteration, d.ts);
      },
      thinking: function (d) { heard(d.text || d.chunk); },
      text_chunk: function (d) { heard(d.chunk); },
      llm_response: function (d) {
        unthink();
        state.tokens += (d.inputTokens || 0) + (d.outputTokens || 0) + (d.cacheReadTokens || 0) + (d.cacheCreationTokens || 0);
        if (d.model) state.model = d.model;
        figures();
        var said = firstLine(d.text, 160);
        if (said) now(said);
      },
      tool_request: function (d) {
        if (cfg.kind === 'author' && /\bCheck\.run\s*\(/.test(d.js || '')) stageAt(3);
        begin(plainAction(d.js), d.ts, d.js ? short(d.js, 400) : null);
      },
      tool_result: function (d) {
        var r = pending[pending.length - 1];
        if (r) mark(r, looksFailed(d.result) ? 'failed' : 'done');
      },
      step: function (d) {
        if (suite) {
          // a set's failing step is told on its test's row, not as a line of Gherkin
          if (d.status === 'failed') state.failedStep = short(d.text || '', 80);
          return;
        }
        var text = (d.name || 'A step') + (d.note ? ' — ' + d.note : '');
        row(short(text, 200), d.status === 'fail' ? 'failed' : 'done', d.ts);
      },
      assertion: function (d) {
        if (!d.label) return;
        row(short(d.label, 200), d.pass === false ? 'failed' : 'done', d.ts, d.message || null);
      },
      screenshot: function (d) {
        shot(d);
        row('Took a screenshot' + (d.title ? ': ' + short(d.title, 100) : ''), 'done', d.ts);
      },
      error: function (d) {
        if (suite) return;   // a set's failure is its test's red row (the error rides the row's title)
        row(short(d.hint || firstLine(d.message || d.error, 160) || 'Something went wrong', 200), 'failed', d.ts,
          d.message ? short(d.message, 600) : null);
      },
      healed: function (d) { row('The page had changed — found the element another way', 'done', d.ts); },
      paused: function () { now('Paused.'); },
      // while recording, an eval is the console's own control call (Stop, Turn it into a test), not a step —
      // all but the kickoff that opens the app
      eval_start: function (d) {
        if (cfg.recording && !opensApp(d.request)) return;
        begin(plainAction(d.request), d.ts, d.request ? short(d.request, 400) : null);
      },
      eval: function (d) {
        if (cfg.recording && !opensApp(d.request)) return;
        var failed = d.response && d.response.command === 'error';
        var r = pending[pending.length - 1];
        if (r) { mark(r, failed ? 'failed' : 'done'); } else {
          row(plainAction(d.request), failed ? 'failed' : 'done', d.ts);
        }
      },
      human_action: function (d) {
        var text = plainHuman(d);
        if (!text) return;
        // ticking a box is one step, not the click that did it
        var same = state.lastHuman && state.lastHuman.locator === d.locator && state.lastHuman.row.el.isConnected;
        if (same && (d.action === 'check' || d.action === 'uncheck') && state.lastHuman.action === 'click') {
          state.lastHuman.row.main.textContent = text;
          state.lastHuman.action = d.action;
          return;
        }
        if (same && state.lastHuman.text === text) return;   // the same step twice is one step
        state.lastHuman = { locator: d.locator, action: d.action, text: text, row: row(text, 'done', d.ts) };
      },
      annotation: function (d) { if (d.text) row('Note: ' + short(d.text, 160), 'note', d.ts); },
      browser_reset: function (d) { row('Reset the browser', 'done', d.ts); },
      scenario_enter: function (d) {
        var key = d.slug || ((d.feature || '') + '::' + (d.name || ''));
        state.failedStep = null;
        scenarios[key] = begin(short(d.name || 'A test', 160), d.ts);
        now('Now: ' + short(d.name || 'a test', 120));
      },
      scenario_exit: function (d) {
        var key = d.slug || ((d.feature || '') + '::' + (d.name || ''));
        var r = scenarios[key] || row(short(d.name || 'A test', 160), 'now', d.ts);
        var took = d.durationMillis ? ' — ' + (Math.round(d.durationMillis / 100) / 10) + ' s' : '';
        r.main.textContent = short(d.name || 'A test', 160)
          + (d.passed ? ' — passed' : ' — failed' + (state.failedStep ? ' at “' + state.failedStep + '”' : '')) + took;
        state.failedStep = null;
        if (!d.passed && d.error) r.main.title = short(d.errorReason || d.error, 600);
        mark(r, d.passed ? 'done' : 'failed');
      },
      // a set's member is one feature — a wrapped test file is a one-scenario feature — so a member is
      // done when its feature exits
      feature_exit: function (d) {
        if (d.callDepth) return;
        state.done++;
        figures();
      },
      complete: complete
    };

    function onEvent(name, raw) {
      var d;
      try { d = JSON.parse(raw); } catch (e) { return; }
      if (!d || typeof d !== 'object') return;
      if (d.seq != null) { if (d.seq <= state.lastSeq) return; state.lastSeq = d.seq; }
      var h = ROUTER[d.type || name];
      if (h) h(d);
    }

    var tick = setInterval(function () {
      if (elapsedEl) elapsedEl.textContent = elapsed(Date.now() - started);
    }, 1000);
    if (elapsedEl) elapsedEl.textContent = elapsed(Date.now() - started);
    figures();

    var src = karate.events(onEvent, { run: cfg.runId, since: -1 });
    window.addEventListener('pagehide', function () { try { src.close(); } catch (e) {} clearInterval(tick); });

    // the live browser view: noVNC is served same-origin under /novnc (the container ships it); a
    // console without it gets a plain sentence instead of a 404 inside the frame
    var frame = root.querySelector('iframe[data-m-vnc]');
    if (frame) {
      // noVNC's own control bar sits over the app's left edge and eats clicks there; the console needs none
      // of its controls (same origin, so the frame's document is reachable)
      frame.addEventListener('load', function () {
        try {
          var d = frame.contentDocument;
          var st = d.createElement('style');
          st.textContent = '#noVNC_control_bar_anchor { display: none !important; }';
          d.head.appendChild(st);
        } catch (e) { /* a cross-origin or torn-down frame keeps noVNC's default chrome */ }
      });
      fetch('/novnc/vnc.html', { cache: 'no-store' })
        .then(function (r) { if (!r.ok) throw new Error(); frame.src = frame.getAttribute('data-m-vnc'); })
        .catch(function () {
          frame.remove();
          var off = root.querySelector('[data-m-vnc-off]');
          if (off) off.hidden = false;
        });
    }
  }

  return {
    runAll: runAll, again: again, convert: convert, finishWritten: finishWritten, stop: stop, moveCall: moveCall, keepSite: keepSite,
    deleteTest: deleteTest, watch: watch,
    // exposed for the page's own tests of the plain-language projection
    plainAction: plainAction, plainHuman: plainHuman
  };
})();
