/*
 * The API test page (manual/tests/api.html) — its Alpine factory. The page is server-rendered; this picks the
 * call whose exchange shows, turns a JSON body into the read-only grid (pub/json-table.js), switches the env, and runs the test:
 * Runner.run with {session} over /api/eval (run-modal.js kaKickSuite, D98), staying on the page until the run has a result
 * for this file, then reloading on it (?run=<runId>). Try this call again and Start over post /ui/apitest/{send,restart}
 * and show the cards the verb returns over the run's; the strip is the session state (ApiTest.session) in words.
 * The side chat (api-workspace §5, §8): a message posts /ui/apitest/chat (Harness.api) and its turn streams on the chat's
 * SSE topic; the chat id stays in the tab's sessionStorage so the page's own reloads reattach; the question, Undo and the
 * links post /ui/apitest/{answer,recheck,undo,link}, a draft /ui/requirements/write. Opened from a recording's calls
 * panel (?from=<run id>), a new test's first message is sent once, carrying the recording (/ui/apitest/chat {from}).
 */
function kaApiTest() {
  var el = document.getElementById('apiTest-data');
  var s = (el && JSON.parse(el.textContent)) || {};
  var pe = document.getElementById('chatPane-data');
  var pane = (pe && JSON.parse(pe.textContent)) || {};
  var tri = pane.triage || {};
  function stored(key, value) {
    try {
      if (value === undefined) return sessionStorage.getItem(key);
      if (value === null) sessionStorage.removeItem(key); else sessionStorage.setItem(key, value);
    } catch (e) { return null; }
  }
  function message(e) { return String(e && e.message ? e.message : e).replace(/^Error: /, ''); }
  // a refusal that names a tool call is the console's own words, never the tester's: the page says what did not happen
  var TOOL_CALL = /\b(Api|ApiTest|Http|Postman|Requirement|File|Harness|Runner|Secret)\.[a-z][A-Za-z]*\b/;
  function plain(error, fallback) { return TOOL_CALL.test(String(error)) ? fallback : String(error); }
  return {
    s: s, sel: s.selected || null, side: 'got', running: false, switching: false, err: '',
    sess: s.session || { state: 'none' }, tried: {}, sends: 0, refusal: {}, busy: null, sessErr: '', stoppedAt: null,
    chatOpen: !!pane.open, turns: [], draft: '', waiting: false, chatErr: '', notice: '', reconnecting: false, unread: false,
    hasQuestion: tri.state === 'open' || tri.state === 'lost', ticket: '', answering: false, askErr: '', undoErr: '',
    ticks: {}, saved: {}, linked: {}, chatId: null, maxN: 0, src: null, lastSent: null, landed: false,

    init() { this.chatStart(); },

    // ---- the side chat ----
    chatKey(test) {
      if (!test && !pane.test && pane.from) return 'ka.apichat.from.' + pane.from.source;
      return 'ka.apichat.' + (test || pane.test || 'new');
    },
    // on the run it names, else on the test's newest: a run param an earlier run left would show its verdict
    reloadOpen(test, run) {
      var q = new URLSearchParams(location.search);
      if (test) { q.set('test', test); q.delete('from'); }
      if (run) q.set('run', run); else q.delete('run');
      q.set('chat', 'open');
      location.replace(location.pathname + '?' + q.toString());
    },
    async chatStart() {
      var id = stored(this.chatKey()) || (pane.from && pane.from.chat) || null;
      if (!id && !pane.test && pane.from && pane.from.ok && pane.model) { this.ask(pane.from.message, pane.from.source); return; }
      if (!id && !pane.test) return;
      var q = new URLSearchParams();
      if (id) q.set('chat', id);
      if (pane.test) q.set('test', pane.test);
      var res = await fetch('/ui/apitest/chat?' + q.toString()).catch(function () { return null; });
      var out = res ? await res.json().catch(function () { return null; }) : null;
      if (res && res.ok && out && out.chat) {
        this.load(out);
        this.listen();
      } else if (id) {
        this.notice = 'The chat restarted; your test is as last saved.';
        stored(this.chatKey(), null);
      }
    },
    load(out) {
      this.chatId = out.chat;
      stored(this.chatKey(), out.chat);
      this.maxN = out.events || 0;
      this.turns = (out.turns || []).map(function (t) {
        return { turn: t.turn, kind: t.kind, message: t.message, status: t.status, reply: t.reply || null,
          error: t.error || null, cards: t.cards || [], step: null };
      });
      this.waiting = !!out.busy;
      this.scroll();
    },
    listen() {
      var self = this;
      if (this.src || !this.chatId || !window.karate) return;
      this.src = window.karate.events(function (name, raw) {
        if (name !== 'chat' && name !== 'tool_request') return;
        try {
          var ev = JSON.parse(raw);
          if (name === 'chat') self.onEvent(ev); else self.onStep(ev);
        } catch (e) { /* a malformed event is skipped */ }
      }, { run: this.chatId, onOpen: function () { self.reconnecting = false; }, onError: function () { self.reconnecting = true; } });
    },
    onEvent(ev) {
      if (ev.chat !== this.chatId || ev.n <= this.maxN) return;
      this.maxN = ev.n;
      var t = this.turns.filter(function (x) { return x.turn === ev.turn; })[0];
      if (!t) {
        t = { turn: ev.turn, kind: 'message', message: '', status: 'running', reply: null, error: null, cards: [], step: null };
        this.turns.push(t);
      }
      if (ev.kind === 'message') {
        t.message = ev.text; t.kind = ev.from; t.status = 'running'; this.waiting = true;
      } else if (ev.kind === 'reply') {
        t.status = ev.status; t.reply = ev.text || null;
        t.error = ev.text ? null : ev.error || 'The assistant stopped without an answer. Ask again.';
        this.waiting = false;
        if (!this.chatOpen) this.unread = true;
        this.afterTurn(t);
      } else {
        if (ev.saved) this.landed = true;
        // a card that supersedes one shown earlier (a recording's calls made again): the old one goes, from any turn;
        // an event with no kind only removes
        if (ev.replaces != null) this.turns.forEach(function (x) { x.cards = x.cards.filter(function (c) { return c.n !== ev.replaces; }); });
        if (ev.kind) t.cards.push(ev);
      }
      this.scroll();
    },
    // a block the assistant runs, named by its step under the running turn; its model text is never shown
    onStep(ev) {
      var t = this.turns.filter(function (x) { return x.status === 'running'; }).pop();
      if (t) t.step = kaWords.chatStep(ev.js);
    },
    afterTurn(t) {
      var created = t.cards.filter(function (c) { return c.kind === 'run' && c.created && c.saved; })[0];
      if (created) {
        var self = this;
        // a turn that saved again after creating the test (its outcome checks) ends on that later run
        var last = t.cards.filter(function (c) { return c.kind === 'run' && c.saved && c.path === created.path; }).pop() || created;
        stored(this.chatKey(created.path), this.chatId);
        this.stampReq(created).then(function () { self.reloadOpen(created.path, last.runId); });
        return;
      }
      // a red put to the tester: its question is answered on the page of its path, which the reload renders; a
      // red the chat fixes opens only a new test's page
      var red = t.cards.filter(function (c) { return c.kind === 'run' && c.status === 'red' && (!c.fix || c.path !== pane.test); })[0];
      if (red) {
        stored(this.chatKey(red.path), this.chatId);
        this.reloadOpen(red.path, red.runId);
        return;
      }
      var saved = t.cards.filter(function (c) { return c.saved; })[0];
      if (t.kind === 'answer' || saved) this.reloadOpen(null, saved && saved.runId);
    },
    // the criteria Coverage's New API test named (?req=) are linked on the test the chat created; a recording's test
    // is linked only through the chat's proposal and the tester's tick (its criteria ride the first message)
    async stampReq(created) {
      if (pane.from || !pane.req || !pane.req.length) return;
      try {
        var res = await fetch('/ui/apitest/read?test=' + encodeURIComponent(created.path));
        var read = res.ok ? await res.json() : null;
        if (!read || !read.version) return;
        var out = await this.post('/ui/apitest/link', { test: created.path, scenario: created.scenario || 0,
          tags: pane.req.map(function (id) { return '@req=' + id; }), ifMatch: read.version });
        if (out.error) this.notice = 'The test was saved, but it could not be linked to ' + pane.req.join(', ') + '.';
      } catch (e) { this.notice = 'The test was saved, but it could not be linked: ' + message(e); }
    },
    scroll() {
      var log = this.$refs && this.$refs.log;
      if (log) this.$nextTick(function () { log.scrollTop = log.scrollHeight; });
    },
    async ask(text, from) {
      var said = (text || this.draft || '').trim();
      if (!said || this.waiting) return;
      this.chatErr = '';
      try {
        var body = { message: said };
        if (from) { body.from = from; if (pane.from && pane.from.req) body.req = pane.from.req; }
        if (pane.test) body.test = pane.test;
        if (this.chatId) body.chat = this.chatId;
        if (this.lastSent) body.sent = this.lastSent;
        var out = await this.post('/ui/apitest/chat', body);
        if (out.error) { this.chatErr = plain(out.error, 'The message was not sent. Reload the page, then send it again.'); return; }
        if (out.restarted) this.notice = out.restarted;
        // the recording's chat already exists (a reload, another tab): it is shown, not asked again
        if (out.reused) { this.load(out); this.listen(); return; }
        if (this.chatId !== out.chat) {
          if (this.src) { this.src.close(); this.src = null; }
          this.chatId = out.chat;
          this.maxN = 0;
          stored(this.chatKey(), out.chat);
        }
        if (!text) this.draft = '';
        this.waiting = true;
        this.listen();
      } catch (e) { this.chatErr = message(e); }
    },
    chip(text) { this.ask(text); },
    explain() {
      var self = this;
      var c = (s.calls || []).filter(function (x) { return x.key === self.sel; })[0];
      // a Try again answer is in no run the chat can read, so the message carries it
      var ex = c && this.tried[c.key] && this.tried[c.key].ex;
      var got = ex ? ' Trying it again just now answered ' + ex.status + (ex.gotBody ? ': ' + String(ex.gotBody).slice(0, 1500) : '.') : '';
      this.ask(c ? 'Explain the response of call ' + c.n + ', ' + c.title + ', in plain words.' + got : 'Explain the last response in plain words.');
    },
    runWords(c) {
      if (c.unchanged) {
        var said = c.status === 'green' ? '' : kaWords.expectation((c.failing || {}).expected, (c.failing || {}).actual);
        return c.status === 'green' ? 'Ran the saved test again: it passed.' : 'Ran the saved test again: it did not pass' + (said ? ': ' + said : '.');
      }
      if (c.status === 'green') {
        var weak = c.weakening && c.weakening.length ? ' It weakens a check: see the change above the cards.' : '';
        var settings = c.config === true ? ' The settings change was saved with it.' : c.config === false ? ' The settings change was not saved.' : '';
        return (c.saved ? (c.created ? 'Passed, and saved as a new test.' : 'Passed, and saved.') : 'Passed, but it could not be saved.') + settings + weak;
      }
      var f = c.failing || {};
      var plain = kaWords.expectation(f.expected, f.actual);
      return 'Did not pass, so nothing was saved' + (c.config === false ? ', the settings change included' : '')
        + (plain ? ': ' + plain : f.expected ? ': ' + kaWords.failure(f.expected, 140) : '.');
    },
    tick(n, tag, on) {
      var now = (this.ticks[n] || []).filter(function (x) { return x !== tag; });
      if (on) now.push(tag);
      this.ticks = Object.assign({}, this.ticks, { [n]: now });
    },
    // a proposed link the flow already carries (chatPane.linked) reads ticked and linked, after a reload too
    carries(c, it) {
      var held = (pane.linked || {})[c.scenario] || [];
      return held.indexOf(String(it.tag).replace(/^@req=/, '')) >= 0;
    },
    linkLabel(c) {
      var self = this;
      var held = (c.items || []).some(function (it) { return self.carries(c, it); });
      return this.linked[c.n] || held && !(this.ticks[c.n] || []).length ? 'Linked' : 'Link the ticked';
    },
    async link(c) {
      this.chatErr = '';
      try {
        // the chat's own save since the page loaded moved the version: link on the test as it saved it
        var version = pane.version;
        if (this.landed || !version) {
          var res = await fetch('/ui/apitest/read?test=' + encodeURIComponent(c.path));
          var read = res.ok ? await res.json() : null;
          if (read && read.version) version = read.version;
        }
        var out = await this.post('/ui/apitest/link', { test: c.path, scenario: c.scenario, tags: this.ticks[c.n] || [], ifMatch: version });
        if (out.error) {
          this.chatErr = out.code === 'STALE' ? 'This test changed since the page loaded. Reload, then link again.'
            : plain(out.error, 'The ticked criteria were not linked. Reload the page, then link them again.');
          return;
        }
        this.linked = Object.assign({}, this.linked, { [c.n]: true });
        this.reloadOpen();
      } catch (e) { this.chatErr = message(e); }
    },
    // a draft is saved create-only (/ui/requirements/import refuses an existing file and a taken id): a name
    // another file holds moves on to the next free one, and the card says where it went
    async saveDraft(c) {
      this.chatErr = '';
      var base = String(c.file).replace(/\.md$/, '');
      try {
        for (var k = 1; k < 10; k++) {
          var name = k == 1 ? base + '.md' : base + '-' + k + '.md';
          var res = await fetch('/ui/requirements/import?name=' + encodeURIComponent(name), {
            method: 'POST', headers: { 'Content-Type': 'text/plain; charset=utf-8' }, body: c.markdown });
          var out = await res.json().catch(function () { return null; });
          if (!out) throw new Error('The console did not answer (' + res.status + ').');
          if (out.code === 'EXISTS') continue;
          if (out.code === 'DUPLICATE_ID') {
            this.chatErr = 'Not saved: another requirements file already uses ' + (out.ids || []).join(', ')
              + '. Ask the assistant to draft them with new ids.';
            return;
          }
          if (out.error) { this.chatErr = plain(out.error, 'The requirements were not saved. Save them from the requirements editor.'); return; }
          this.saved = Object.assign({}, this.saved, { [c.n]: k == 1 ? 'Saved as ' + out.file
            : 'Saved as ' + out.file + ': requirements/' + base + '.md already holds other requirements and was left as it is.' });
          return;
        }
        this.chatErr = 'Not saved: every name like ' + base + '.md is taken. Save them from the requirements editor.';
      } catch (e) { this.chatErr = message(e); }
    },
    // a draft is saved once a requirements file holds every id it drafts: the card says so after a reload too
    savedAs(c) {
      if (this.saved[c.n]) return this.saved[c.n];
      var held = pane.held || {}, ids = this.draftItems(c.markdown).map(function (r) { return r.id; });
      var file = ids.length ? held[ids[0]] : null;
      return file && ids.every(function (id) { return held[id] === file; }) ? 'Saved in ' + file : '';
    },
    // a turn's call cards: more than a few read as one line that opens to them all
    callsOf(t) { return (t.cards || []).filter(function (c) { return c.kind === 'call'; }); },
    // and a run card that repeats the one just before it (same test, verdict and failure) reads as one with a count
    shownCards(t) {
      var self = this, many = this.callsOf(t).length > 3, out = [], prev = null;
      (t.cards || []).forEach(function (c) {
        if (prev && c.kind === 'run' && prev.kind === 'run' && c.path === prev.path && c.status === prev.status
            && JSON.stringify(c.failing || null) === JSON.stringify(prev.failing || null) && self.runWords(c) === self.runWords(prev)) {
          var head = out[out.length - 1];
          out[out.length - 1] = Object.assign({}, head, { times: (head.times || 1) + 1 });
        } else if (!many || c.kind !== 'call') out.push(c);
        prev = c;
      });
      return out;
    },
    // the requirements and criteria a draft card holds, read off its markdown
    draftItems(md) {
      var out = [], at = null;
      String(md || '').split('\n').forEach(function (line) {
        var h = line.match(/^#{2,4}\s+([A-Za-z0-9._-]+):\s*(.+)$/);
        var k = line.match(/^\s*-\s*[\d.]+\s*:\s*(.+)$/);
        if (h) { at = { id: h[1], title: h[2].trim(), criteria: [] }; out.push(at); }
        else if (k && at) at.criteria.push(k[1].trim());
      });
      return out;
    },
    async answer(choice) {
      if (this.answering) return;
      this.answering = true;
      this.askErr = '';
      try {
        var body = { test: pane.test, answer: choice };
        if (choice === 'discard' && tri.state === 'lost') body.doc = tri.doc;
        else { body.candidate = tri.candidate; body.base = tri.base; }
        if (choice === 'bug' && this.ticket.trim()) body.ticket = this.ticket.trim();
        var out = await this.post('/ui/apitest/answer', body);
        if (out.error) {
          this.askErr = out.code === 'BUSY' ? 'The assistant is still busy. Answer again in a moment.'
            : plain(out.error, 'The answer was not taken. Reload the page, then answer again.');
          return;
        }
        this.reloadOpen();
      } catch (e) { this.askErr = message(e); } finally { this.answering = false; }
    },
    async recheck() {
      this.askErr = '';
      try {
        var out = await this.post('/ui/apitest/recheck', { test: pane.test, candidate: tri.candidate });
        if (out.error) { this.askErr = plain(out.error, 'It was not checked again. Reload the page, then try again.'); return; }
        this.reloadOpen();
      } catch (e) { this.askErr = message(e); }
    },
    async undo(btn) {
      if (this.busy) return;
      this.busy = 'undo';
      this.undoErr = '';
      try {
        var out = await this.post('/ui/apitest/undo', { test: pane.test, after: btn.dataset.mUndo });
        if (out.error) { this.undoErr = plain(out.error, 'Nothing was undone. Reload the page, then try again.'); return; }
        if (out.deleted) { location.replace('/manual/tests/api?chat=open'); return; }
        location.reload();
      } catch (e) { this.undoErr = message(e); } finally { this.busy = null; }
    },
    openChat() {
      this.chatOpen = true;
      this.unread = false;
      var q = new URLSearchParams(location.search);
      q.set('chat', 'open');
      history.replaceState(null, '', location.pathname + '?' + q.toString());
      this.scroll();
    },
    closeChat() {
      this.chatOpen = false;
      var q = new URLSearchParams(location.search);
      q.delete('chat');
      history.replaceState(null, '', location.pathname + (q.toString() ? '?' + q.toString() : ''));
    },

    pick(key) { this.sel = key; },

    live() {
      if (this.running) return { dot: 'inflight', text: 'Running — you can try calls one at a time when it finishes.' };
      var st = this.stoppedAt == null ? this.sess : Object.assign({}, this.sess, { stoppedAt: this.stoppedAt });
      return kaWords.liveSession(st, s.calls, s.live);
    },
    ready() { return !this.running && this.sess.state === 'warm'; },
    dotClass(tone) {
      var out = {};
      ['passed', 'failed', 'notrun', 'inflight', 'stopped'].forEach(function (t) { out['m-dot--' + t] = t === (tone || 'notrun'); });
      return out;
    },
    triedCheck(key, el) {
      var at = el.closest('[data-m-check]');
      return (this.tried[key] && at && this.tried[key].checks[at.dataset.mCheck]) || {};
    },

    body(extra) {
      var b = { test: s.path, scenario: s.scenario, session: s.sessionId };
      if (s.row != null) b.row = s.row;
      return Object.assign(b, extra || {});
    },
    async post(route, body) {
      var res = await fetch(route, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      var out = await res.json().catch(function () { return null; });
      if (!out) throw new Error('The console did not answer (' + res.status + ').');
      return out;
    },
    async refresh() {
      var q = new URLSearchParams({ test: s.path, scenario: s.scenario, session: s.sessionId });
      if (s.row != null) q.set('row', s.row);
      var res = await fetch('/ui/apitest/session?' + q.toString()).catch(function () { return null; });
      var out = res && res.ok ? await res.json().catch(function () { return null; }) : null;
      if (out && out.state) this.sess = out;
    },

    // the cards a Send or Start over returned (the exchanges shape): each call's verdict and last exchange, and
    // the outcome of every check after it, keyed as the page keys its own calls
    apply(cards) {
      var calls = {}, tried = Object.assign({}, this.tried), at = null, seq = ++this.sends;
      (s.calls || []).forEach(function (c) { if (!c.shared) calls[c.step] = c.key; });
      (cards || []).forEach(function (cd) {
        var mark = function (st) { return st === 'passed' ? 'passed' : st === 'failed' ? 'failed' : 'notrun'; };
        var err = cd.error ? kaWords.failure(cd.error, 220) : null;
        if (calls[cd.step]) {
          var verdict = cd.verdict || cd.status;
          var xs = cd.exchanges || [];
          at = { word: verdict === 'passed' ? 'passed' : verdict === 'failed' ? 'did not pass' : 'not sent',
            dot: mark(verdict), ex: xs.length ? kaWords.exchange(xs[xs.length - 1]) : null, lone: err, checks: {}, seq: seq };
          tried[calls[cd.step]] = at;
        } else if (at) {
          at.checks[cd.step] = { dot: mark(cd.status), error: err };
          if (err) at.lone = null;
        }
      });
      this.tried = tried;
    },

    async tryCall(card) {
      var key = card.dataset.mCall;
      if (this.busy) return;
      this.busy = key;
      this.refusal = Object.assign({}, this.refusal, { [key]: '' });
      try {
        var out = await this.post('/ui/apitest/send', this.body({ step: Number(card.dataset.step) }));
        if (out.error) {
          this.refusal = Object.assign({}, this.refusal, { [key]: kaWords.sessionRefusal(out.code, out.error) });
          await this.refresh();
          return;
        }
        this.apply(out.cards);
        // the chat reads no Send: each message carries the latest one (api-workspace §5)
        var c = (s.calls || []).filter(function (x) { return x.key === key; })[0], ex = this.tried[key] && this.tried[key].ex;
        this.lastSent = c && ex ? { call: 'call ' + c.n + ', ' + c.title, status: ex.status,
          body: ex.gotBody ? String(ex.gotBody).slice(0, 600) : null } : null;
        this.stoppedAt = null;
        this.sess = Object.assign({}, this.sess, { state: out.state, cursor: out.cursor, version: out.version, env: out.env });
        this.sel = key;
      } catch (e) {
        this.refusal = Object.assign({}, this.refusal, { [key]: message(e) });
      } finally { this.busy = null; }
    },

    async startOver() {
      if (this.busy) return;
      this.busy = 'restart';
      this.sessErr = '';
      this.refusal = {};
      this.stoppedAt = null;
      try {
        var out = await this.post('/ui/apitest/restart', this.body());
        if (out.error && !out.state) {
          this.sessErr = kaWords.sessionRefusal(out.code, out.error);
        } else {
          this.tried = {};
          this.apply(out.cards);
          if (out.status === 'failed') {
            var red = (out.cards || []).filter(function (cd) { return (cd.verdict || cd.status) === 'failed'; })[0];
            this.stoppedAt = red ? red.step : -1;
            this.sessErr = kaWords.failure(out.error || 'see the call', 220);
          }
        }
        await this.refresh();
      } catch (e) { this.sessErr = message(e); } finally { this.busy = null; }
    },

    // Runner.env over /ui/runner/env ('' is the default, sent as null); the next run and the sign-in chip use it,
    // so the page reloads on it
    async switchEnv(name) {
      if (this.switching) return;
      this.err = '';
      this.switching = true;
      try {
        var res = await fetch('/ui/runner/env', { method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: name || null }) });
        var out = await res.json().catch(function () { return null; });
        if (!res.ok || !out || out.error) throw new Error((out && out.error) || 'The environment did not change.');
        location.reload();
      } catch (e) { this.err = message(e); this.switching = false; }
    },

    // a JSON body reads as the grid; anything else stays the text the server rendered
    grid(box) {
      var pre = box.querySelector('pre');
      if (!pre || !window.JsonTable) return;
      var value;
      try { value = JSON.parse(pre.textContent); } catch (e) { return; }
      if (value === null || typeof value !== 'object') return;
      box.innerHTML = window.JsonTable.render(value, { literal: true });
    },

    async run(btn) {
      if (this.running) return;
      this.err = '';
      this.running = true;
      var target = btn.getAttribute('data-m-api-run');
      try {
        var out = await kaKickSuite(target, { async: true, session: s.sessionId });
        if (!out || !out.runId) throw new Error((out && out.error) || 'The run did not start.');
        this.follow(out.runId);
      } catch (e) { this.err = message(e); this.running = false; }
    },

    // the run's end lands as `complete` on its SSE topic (an `error` there is one step failing, and the run goes
    // on); the poll covers a missed event: the run's result for this file, or the run settled without one
    follow(runId) {
      var self = this, done = false, tries = 0, src = null;
      function land() {
        if (done) return;
        done = true;
        if (src) src.close();
        var q = new URLSearchParams(location.search);
        q.set('run', runId);
        location.replace(location.pathname + '?' + q.toString());
      }
      async function ready() {
        var url = '/ui/apitest/exchanges?test=' + encodeURIComponent(s.path) + '&runId=' + encodeURIComponent(runId);
        var res = await fetch(url).catch(function () { return null; });
        if (res && res.ok) return true;
        var meta = await fetch('/ui/apitest/run?runId=' + encodeURIComponent(runId)).catch(function () { return null; });
        var out = meta && meta.ok ? await meta.json().catch(function () { return null; }) : null;
        return !!(out && out.status && out.status !== 'running' && out.status !== 'started');
      }
      src = window.karate.events(function (name) {
        if (name === 'complete') land();
      }, { run: runId });
      (async function poll() {
        while (!done && tries++ < 300) {
          await new Promise(function (r) { setTimeout(r, 2000); });
          if (!done && await ready()) land();
        }
        if (!done) { self.running = false; self.err = 'The run is taking long. Open it on Runs to watch it.'; }
      })();
    }
  };
}
