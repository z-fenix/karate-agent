// Manual mode — the client half of the Manual chrome (loaded by layout.html on /manual pages only).
//
// kaManual.refresh()  re-renders the current page server-side and swaps every element carrying
//                     data-m-live (matched by its data-ka-section id) with the fresh copy — the rail's
//                     counts and verdict, and any screen section that opts in; a page whose only live
//                     section is the rail re-renders the rail alone (/manual/rail); a section holding a
//                     focused field or typed text is left as it is. Pages stay SSR (D88):
//                     no client-side re-derivation of a number or a verdict.
// kaManual.onEvent()  the layout's control channel forwards every global SSE event here; a run starting,
//                     settling, or folding into the project graph (`coverage`) triggers one debounced refresh.
// kaManual.choose()   the mode picker: remember Manual/Dev for this browser (POST /ui/session, D114), then go.

// The entrance plays once per page load (manual.css "motion"): this script runs in <head>, so the class is
// on before first paint, and it comes off once the page has arrived — a live refresh never replays it.
// The stage strip buds in when the flow is entered or the page is reloaded: coming from another page that
// showed it, it stays put and its black bubble travels to the next step (m-strip-carry).
(function () {
  var root = document.documentElement;
  root.classList.add('m-arrive');
  var nav = (performance.getEntriesByType && performance.getEntriesByType('navigation')[0]) || {};
  try { if (nav.type !== 'reload' && sessionStorage.getItem('m-strip')) root.classList.add('m-strip-carry'); } catch (e) {}
  // Home's first-case guide, hidden in this browser (manual/index.html kaGuide.hide), stays hidden from first paint
  try { if (localStorage.getItem('ka-manual-guide') === 'hidden') root.classList.add('m-guide-off'); } catch (e) {}
  // a project switch re-anchors the console: the tab that asked lands on Home, every other tab says once where it
  // went (kaManual.onReanchor, below)
  try {
    if (sessionStorage.getItem('m-project-land')) {
      sessionStorage.removeItem('m-project-land');
      if (location.pathname !== '/manual') location.replace('/manual');
    }
    var moved = sessionStorage.getItem('m-project-moved');
    if (moved) { sessionStorage.removeItem('m-project-moved'); window.kaProjectMoved = moved; }
  } catch (e) {}
  addEventListener('pagehide', function () {
    try {
      if (document.querySelector('[data-m-stages]')) sessionStorage.setItem('m-strip', '1');
      else sessionStorage.removeItem('m-strip');
    } catch (e) {}
  });
  function arrived() { setTimeout(function () { root.classList.remove('m-arrive'); }, 1800); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', arrived);
  else arrived();
})();

window.kaManual = (function () {
  var REFRESH_ON = ['coverage', 'run_start', 'complete'];
  var timer = null;

  function syncThemeGlyph() {
    var b = document.getElementById('ka-theme-btn');
    if (b) b.textContent = document.documentElement.getAttribute('data-theme') === 'dark' ? '☀' : '☾';
  }

  function text(el) { return el ? el.textContent.replace(/\s+/g, ' ').trim() : ''; }

  // a refresh that moved something says so once (manual.css "motion"): on the rail, the verdict and each
  // count that changed; on a screen, the live section whose text changed
  function markChanged(was, now) {
    if (now.classList.contains('m-rail')) {
      var verdict = now.querySelector('.m-verdict');
      if (verdict && text(verdict) !== text(was.querySelector('.m-verdict'))) verdict.classList.add('m-changed');
      now.querySelectorAll('[data-m-count]').forEach(function (c) {
        var old = was.querySelector('[data-m-count=' + JSON.stringify(c.getAttribute('data-m-count')) + ']');
        if (old && text(old) !== text(c)) c.classList.add('m-changed');
      });
    } else if (text(was) !== text(now)) {
      now.classList.add('m-changed');
    }
  }

  // a section the user is typing into keeps its copy: a field in it has focus, or a text field differs from what
  // it rendered with — until that field is emptied or its page saves it
  var TEXTLESS = /^(checkbox|radio|file|hidden|button|submit|reset|image|range|color)$/;
  function typing(el) {
    var a = document.activeElement;
    if (a && el.contains(a) && /^(INPUT|TEXTAREA|SELECT)$/.test(a.tagName)) return true;
    var fields = el.querySelectorAll('input, textarea');
    for (var i = 0; i < fields.length; i++) {
      var f = fields[i];
      if (!TEXTLESS.test(f.type || '') && f.value !== f.defaultValue) return true;
    }
    return false;
  }

  function swap(html) {
    var doc = new DOMParser().parseFromString(html, 'text/html');
    document.querySelectorAll('[data-m-live]').forEach(function (el) {
      var id = el.getAttribute('data-ka-section');
      if (!id || typing(el)) return;
      var fresh = doc.querySelector('[data-ka-section=' + JSON.stringify(id) + ']');
      if (!fresh) return;
      var node = document.importNode(fresh, true);
      markChanged(el, node);
      el.replaceWith(node);
    });
    syncThemeGlyph();
    if (window.kaView) window.kaView.syncPinUi();
    document.dispatchEvent(new CustomEvent('m-swap'));   // a page re-reads what it keeps on the swapped sections
  }

  // what a refresh renders: the page, unless the rail is all it keeps live
  function source() {
    return document.querySelector('[data-m-live]:not(.m-rail)') ? location.pathname + location.search
      : '/manual/rail?from=' + encodeURIComponent(location.pathname);
  }

  function refresh() {
    clearTimeout(timer);
    timer = setTimeout(function () {
      fetch(source(), { headers: { Accept: 'text/html' }, cache: 'no-store' })
        .then(function (r) { return r.ok ? r.text() : null; })
        .then(function (html) { if (html) swap(html); })
        .catch(function () {});   // a missed refresh is repaired by the next event or a reload
    }, 300);
  }

  function onEvent(name) {
    if (REFRESH_ON.indexOf(name) >= 0) refresh();
  }

  // a file saved from another page sends no event: a page shown again from the back cache, or a tab come back
  // to, re-reads what it shows
  addEventListener('pageshow', function (e) { if (e.persisted) refresh(); });
  document.addEventListener('visibilitychange', function () { if (document.visibilityState === 'visible') refresh(); });

  function choose(event, view, href) {
    if (event) event.preventDefault();
    fetch('/ui/session', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ key: 'consoleView', value: view })
    }).catch(function () {}).then(function () { location.assign(href); });
    return false;
  }

  return { refresh: refresh, onEvent: onEvent, choose: choose };
})();

// New test — record a flow in the live browser, then let the AI turn the recording into a test. Kickoffs
// ride /api/eval (window.karate); the intent (file name, criteria, model) is per-browser working state on
// /ui/session under `manualRecording` (D114), so the run page can finish what the form started.
//
// kaManual.startRecording(intent)   open the live browser on intent.url, arm the recorder, land on the run.
//                                   intent: {name, url, req:[criterion ids], overwrite?, provider?, model?,
//                                   maxIterations?, video?} — overwrite: replace the test that has this name
// kaManual.stopRecording(runId)     end the capture and settle the live run (the browser is free again) —
//                                   the Dev run page's Stop recording + Stop (Recording.stop, Harness.stop).
// kaManual.turnIntoTest(runId, o?)  Harness.author on a settled recording (uses AI), with the provider·model
//                                   the Dev Convert dialog preselects; lands on the authoring run.
//                                   `o` overrides the saved intent ({choice: 'both'}: the API test follows).
//                                   A taken name rejects with code 'EXISTS' unless {overwrite:true}.
//                                   The intent keeps one result per kind of test:
//                                   results: {ui: {authorRunId, check, status}, api: {chat, test, status}}
// kaManual.linkTest(check, ids)     stamp the criteria on the written test's // @req: header, in one
//                                   /ui/checks/link call.
// kaManual.linkAuthored(meta)       linkTest for an authoring run that settled completed ({kind, status, check,
//                                   req} — req is the saved intent's, since the run itself carries none).
// kaManual.recordingIntent(runId?)  the saved intent (for that recording), or null.
(function (M) {
  var KEY = 'manualRecording';

  var slug = kaWords.slug;   // the name New test makes, the same server-side (pub/manual-words.js)
  function ids(v) {
    var list = Array.isArray(v) ? v : String(v == null ? '' : v).split(',');
    return list.map(function (s) { return String(s).trim().replace(/^@/, '').replace(/^req=/, ''); })
      .filter(Boolean);
  }
  // an envelope that refuses ({error}, no runId) becomes a rejection carrying the backend's sentence and code
  function refused(out) {
    if (out && out.error && !out.runId) throw Object.assign(new Error(out.error), { code: out.code || null });
    return out;
  }
  function save(value) {
    return fetch('/ui/session', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ key: KEY, value: value })
    }).catch(function () {});
  }
  function recordingIntent(runId) {
    return fetch('/ui/session', { cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (j) {
        var v = j && j.prefs ? j.prefs[KEY] : null;
        return v && (!runId || v.runId === runId || uiResult(v).authorRunId === runId) ? v : null;
      })
      .catch(function () { return null; });
  }
  function uiResult(v) { return (v && v.results && v.results.ui) || {}; }
  function land(runId) { location.assign('/manual/runs/' + encodeURIComponent(runId)); }

  // the model that writes the test when the intent names none — the Dev Convert dialog's preselected combo:
  // the default provider's default model, else the first usable one (a keyless author call would pick
  // Anthropic even when only Ollama is set up)
  function pickModel() {
    return window.karate.value('Harness.providers()').then(function (llm) {
      var ps = (llm && llm.providers) || [];
      var p = ps.filter(function (x) { return x['default']; })[0] || ps[0];
      if (!p) return {};
      var models = (p.models || []).map(function (m) { return m.id; });
      return { provider: p.name, model: models.indexOf(llm.defaultModel) >= 0 ? llm.defaultModel : (models[0] || null) };
    }).catch(function () { return {}; });
  }

  function startRecording(intent) {
    intent = intent || {};
    var name = slug(intent.name) || 'recorded-flow';
    var url = String(intent.url || '').trim();
    if (url && !/^[a-zA-Z][\w+.-]*:/.test(url)) url = 'https://' + url;   // a bare host is a web address
    var goal = 'New test: checks/' + name + '.js' + (url ? ' on ' + url : '');
    // `var bot` binds the live bot in the serve engine, as the console's live kickoff does; the recording
    // starts before the first navigation, so the page's own load and its first calls are captured
    var js = 'var bot = Robot.create(); (function () {'
      + ' var r = Recording.start(' + JSON.stringify(name) + ');'
      + ' if (r && r.error) return r;'
      + (url ? ' bot.go(' + JSON.stringify(url) + ');' : '')
      + ' Harness.stampLiveGoal(' + JSON.stringify(goal) + ');'
      + ' return { runId: r.runId || Harness.runs().active };'
      + ' })()';
    return window.karate.value(js).then(refused).then(function (out) {
      if (!out || !out.runId) throw new Error('The live browser did not start a run.');
      return save({
        runId: out.runId, name: name, url: url, req: ids(intent.req), overwrite: intent.overwrite === true || null,
        provider: intent.provider || null, model: intent.model || null,
        maxIterations: Number(intent.maxIterations) > 0 ? Number(intent.maxIterations) : null,
        video: intent.video !== false
      }).then(function () { land(out.runId); return out; });
    });
  }

  function stopRecording(runId) {
    var id = JSON.stringify(runId || null);
    // an alert the app left open blocks the recorder's teardown until it closes: dismiss it first
    // (bot.page('dialog', …) throws NO_DIALOG when there is none); once capture is off, the page is
    // blanked, so the run that writes the test starts from nothing rather than where the recording ended
    // a Stop that failed (a fence, the capture) is the call's rejection once the run has settled; a capture whose
    // attribution is uncertain is answered for the page to say
    var js = '(function () { try { bot.page(\'dialog\', \'dismiss\'); } catch (e) {}'
      + ' var s = Recording.stop();'
      + ' try { bot.go(\'about:blank\'); } catch (e) {}'
      + ' var live = Harness.runs().active;'
      + ' var h = live && (' + id + ' === null || live == ' + id + ') ? Harness.stop() : null;'
      + ' return { runId: live || ' + id + ', actions: s ? s.actions : 0, status: h && h.status ? h.status : null,'
      + ' error: s && s.error || null, attribution: s && s.attribution || null }; })()';
    return window.karate.value(js).then(function (out) {
      if (out && out.error) throw new Error(out.error);
      return out;
    });
  }

  function turnIntoTest(runId, overrides) {
    if (!runId) return Promise.reject(new Error('Which recording? A run id is needed.'));
    return recordingIntent(runId).then(function (saved) {
      var intent = Object.assign({}, saved && saved.runId === runId ? saved : {}, overrides || {});
      var name = slug(intent.name) || 'recorded-flow';
      var req = ids(intent.req);
      // no `req` on the authoring run: its own verdict is not a test of the criteria (a failed attempt
      // would paint them red); the written test carries them once it lands (linkAuthored → @req header)
      var opts = { async: true, name: name };
      // replacing a test is asked for one name: New test's choice holds only for the name it was made for
      var chose = saved && saved.runId === runId && saved.overwrite === true && slug(saved.name) === name;
      if (chose || (overrides && overrides.overwrite === true)) opts.overwrite = true;
      if (intent.provider) opts.provider = intent.provider;
      if (intent.model) opts.model = intent.model;
      if (Number(intent.maxIterations) > 0) opts.maxIterations = Number(intent.maxIterations);
      if (intent.video === false) opts.record = false;
      var chosen = intent.provider ? Promise.resolve({}) : pickModel().then(function (m) {
        if (m.provider) opts.provider = m.provider;
        if (m.model) opts.model = m.model;
      });
      return chosen
        .then(function () {
          return window.karate.value('Harness.author(' + JSON.stringify(runId) + ', ' + JSON.stringify(opts) + ')');
        })
        .then(refused)
        .then(function (out) {
          // the saved choice to replace is what this call sent, for the name it sent; the API result, if any, stays
          var results = Object.assign({}, intent.results || {},
            { ui: { authorRunId: out.runId, check: name + '.js', status: 'running' } });
          return save(Object.assign({}, intent, { runId: runId, name: name, req: req, results: results,
            choice: intent.choice || null, overwrite: opts.overwrite === true || null }))
            .then(function () { land(out.runId); return out; });
        });
    });
  }

  // one write for every id: the server links the whole list under the check's lock, or none of it
  function linkTest(check, stamps) {
    var name = String(check || '').replace(/^\/+/, '');
    var list = ids(stamps);
    if (!name || !list.length) return Promise.resolve(null);
    if (name.indexOf('checks/') !== 0) name = 'checks/' + name;
    return fetch('/ui/checks/link', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name, stamp: list })
    }).then(function (r) { return r.json(); }).catch(function (e) { return { error: String(e) }; });
  }

  function linkAuthored(meta) {
    if (!meta || meta.kind !== 'author' || meta.status !== 'completed' || !meta.check || !meta.req) {
      return Promise.resolve(null);
    }
    return linkTest(meta.check, meta.req);
  }

  Object.assign(M, {
    startRecording: startRecording,
    stopRecording: stopRecording,
    turnIntoTest: turnIntoTest,
    linkTest: linkTest,
    linkAuthored: linkAuthored,
    recordingIntent: recordingIntent
  });
})(window.kaManual);

// The guide — the rail's "Show the guide": Home's first-case guide on the project you are on, back on screen
// even when it was hidden or is done. It creates and switches nothing: the guide's steps write into this project.
//
// kaManual.guide()   open Home at the guide
(function (M) {
  var SHOW = 'm-guide-show';

  // a card hidden with × keeps its leave animation, which would play again and hold it at opacity 0
  function settle() {
    document.querySelectorAll('[data-m-guide]').forEach(function (c) { c.classList.remove('m-guide--leaving'); });
  }
  function guide() {
    try { localStorage.removeItem('ka-manual-guide'); sessionStorage.setItem(SHOW, '1'); } catch (e) {}
    settle();
    document.documentElement.classList.remove('m-guide-off');
    if (location.pathname === '/manual') reveal(); else location.assign('/manual');
  }

  // Home asked to show it: the done card too, which a browser otherwise sees once, when it finishes the case;
  // a live refresh re-renders the card, so it stays shown for the rest of the page's life
  var shown = false;
  function reveal() {
    var card = document.querySelector('[data-m-guide]');
    try {
      if (!card || !sessionStorage.getItem(SHOW)) return;
      sessionStorage.removeItem(SHOW);
    } catch (e) { return; }
    shown = true;
    settle();
    card.hidden = false;
    card.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }
  document.addEventListener('DOMContentLoaded', reveal);
  document.addEventListener('m-swap', function () {
    var card = shown && document.querySelector('[data-m-guide]');
    if (card) card.hidden = false;
  });

  M.guide = guide;
})(window.kaManual);

// The project — the rail names the project every screen reads; Change opens the chooser layout.html renders
// from Work.list(). A pick is Work.use over /api/eval, a deliberate state change like the Dev /work page's:
// the server re-anchors and publishes `reanchor`. The tab that asked lands on Home; every other Manual tab
// leaves a page about one item of the old project for its list, and says once that the project changed. A tab
// that refuses to unload (unsaved text) stays, says it shows the old project, and can no longer save into the
// new one. A run in flight holds the switch, and the chooser says so before you pick.
//
// kaManual.project()          open the chooser
// kaManual.useProject(btn)    switch to the project the button names (data-m-project-use)
// kaManual.onReanchor(raw)    the layout's `reanchor` handler on a Manual page
(function (M) {
  var LAND = 'm-project-land', MOVED = 'm-project-moved';
  // the switch's answer and its reanchor event both take the asking tab Home: the first one goes, a second
  // navigation would cancel it mid-transition
  var leaving = false;
  function home(go) { if (!leaving) { leaving = true; go('/manual'); } }

  function chooser() { return document.getElementById('m-project'); }
  function items(d, disabled) {
    d.querySelectorAll('[data-m-project-use]').forEach(function (b) {
      b.disabled = disabled && b.getAttribute('aria-current') !== 'true';
    });
  }
  function say(d, text) {
    var err = d.querySelector('[data-m-project-err]');
    err.textContent = text || '';
    err.hidden = !text;
  }
  function plain(e) {
    var msg = String(e && e.message ? e.message : e).replace(/^Error: /, '');
    return /while a run is active/.test(msg)
      ? 'A run is using the console. Change the project once it has ended, or stop it from its page.'
      : msg;
  }
  function nameOf(root) { var r = String(root || '').replace(/\/+$/, ''); return r.substring(r.lastIndexOf('/') + 1); }

  // a run in flight holds the switch: the live rail's own line says so, and the chooser repeats it with its link
  function project() {
    var d = chooser();
    if (!d) return;
    var live = document.querySelector('.m-rail .m-rail-inflight');
    var busy = d.querySelector('[data-m-project-busy]');
    if (busy) {
      busy.hidden = !live;
      if (live) busy.querySelector('[data-m-project-watch]').setAttribute('href', live.getAttribute('href'));
    }
    items(d, !!live);
    say(d, '');
    d.showModal();
    var here = d.querySelector('[data-m-project-use][aria-current="true"]') || d.querySelector('[data-m-project-use]');
    if (here) here.focus();
  }

  // the flag goes down before the switch: the reanchor event may reload this tab before the call returns
  function useProject(btn) {
    var d = chooser();
    if (btn.getAttribute('aria-current') === 'true') { d.close(); return; }
    items(d, true);
    say(d, '');
    try { sessionStorage.setItem(LAND, '1'); } catch (e) {}
    window.karate.value('Work.use(' + JSON.stringify(btn.getAttribute('data-m-project-use')) + ')').then(function (out) {
      if (out && out.error) throw new Error(out.error);
      home(function (u) { location.assign(u); });
    }).catch(function (e) {
      if (e instanceof TypeError) return;   // the switch's own reload cancelled the call: the tab is leaving
      try { sessionStorage.removeItem(LAND); } catch (x) {}
      items(d, false);
      say(d, plain(e));
    });
  }

  // a page about one item of the old project would say it may have been deleted: its list says where you are
  function listOf(path) {
    if (path.indexOf('/manual/runs/') === 0) return '/manual/runs';
    if (path === '/manual/campaign') return '/manual/campaigns';
    if (path === '/manual/tests/edit') return '/manual/tests';
    if (path === '/manual/requirements/edit') return '/manual/coverage';
    return null;
  }
  function onReanchor(raw) {
    var to = '';
    try { to = nameOf(JSON.parse(raw).root); } catch (e) {}
    var asked = false;
    try { asked = !!sessionStorage.getItem(LAND); } catch (e) {}
    if (asked) { home(function (u) { location.replace(u); }); return; }
    try { sessionStorage.setItem(MOVED, to); } catch (e) {}
    var list = listOf(location.pathname);
    if (list) location.replace(list); else location.reload();
    // still here a moment later: the page kept its unsaved text, and it is not the console's project any more
    setTimeout(function () {
      try { sessionStorage.removeItem(MOVED); } catch (e) {}
      stale(to);
    }, 1500);
  }

  function notice(text, cls) {
    var host = document.querySelector('.m-content');
    if (!host) return;
    var el = document.createElement('p');
    el.className = 'm-nudge ' + cls;
    el.setAttribute('role', cls === 'm-stale' ? 'alert' : 'status');
    el.textContent = text;
    host.insertBefore(el, host.firstChild);
  }
  function stale(to) {
    if (document.querySelector('.m-stale')) return;
    var was = (document.querySelector('.m-rail-project-name') || {}).textContent || 'the old project';
    notice('The console switched to ' + (to || 'another project') + ' in another tab. This page still shows ' + was
      + ': copy what you need, then reload. Saving here is off.', 'm-stale');
    document.documentElement.classList.add('m-project-stale');
    // a page's own bindings re-enable its Save, and Cmd/Ctrl+S skips the button: both stop here, before the page
    function stop(e) { e.preventDefault(); e.stopImmediatePropagation(); }
    window.addEventListener('click', function (e) {
      if (e.target.closest && e.target.closest('[data-m-save], [data-m-draft], form button[type=submit]')) stop(e);
    }, true);
    window.addEventListener('keydown', function (e) {
      if ((e.metaKey || e.ctrlKey) && (e.key === 's' || e.key === 'S')) stop(e);
    }, true);
    window.addEventListener('submit', stop, true);
  }
  document.addEventListener('DOMContentLoaded', function () {
    if (window.kaProjectMoved) notice('The console is now on project ' + window.kaProjectMoved + ': it was changed in another tab.', 'm-moved');
    var d = chooser();
    // a live refresh replaces the rail, so the button that opened the chooser is gone when it closes
    if (d) d.addEventListener('close', function () {
      var b = document.querySelector('[data-m-project]');
      if (b && b.focus) b.focus();
    });
  });

  document.addEventListener('click', function (e) {
    var d = chooser();
    if (d && e.target === d) d.close();   // a click on the backdrop
  });

  M.project = project;
  M.useProject = useProject;
  M.onReanchor = onReanchor;
})(window.kaManual);

// The beacon's rings (manual.css m-beacon) grow the same 12px on every side: the scale per axis comes from the
// button's own size — measured on load, after a live refresh, and when the window is resized.
(function () {
  function size() {
    document.querySelectorAll('.m-beacon').forEach(function (b) {
      var w = b.offsetWidth, h = b.offsetHeight;
      if (!w || !h) return;
      b.style.setProperty('--m-beacon-sx', ((w + 24) / w).toFixed(3));
      b.style.setProperty('--m-beacon-sy', ((h + 24) / h).toFixed(3));
    });
  }
  document.addEventListener('DOMContentLoaded', size);
  document.addEventListener('m-swap', size);
  addEventListener('resize', size);
})();
