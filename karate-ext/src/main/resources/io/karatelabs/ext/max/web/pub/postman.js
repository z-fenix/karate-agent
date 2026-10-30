/*
 * The Postman page (manual/tests/postman.html) — its Alpine factory. The page is server-rendered from the Postman verbs;
 * this keeps the tester's pending edits, compiles them into one Postman.edit (postman SPEC §2's domain forms) and posts it
 * with the version the page was read at over /ui/postman/edit, checking it first as a dry run. A save reloads the page;
 * its paths and undo token stay in sessionStorage so the reloaded page offers Undo (and Redo). Run kicks Runner.run over
 * /api/eval (run-modal.js kaKickSuite, D98) and reloads on the run once /ui/postman/run says it settled. Migrate posts
 * /ui/postman/migrate, then hands the feature to the side chat (/ui/apitest/chat) and opens the API test page on it.
 * A new or uploaded data file posts /ui/postman/data and reloads with it picked (?data=).
 */
function kaPostman() {
  var el = document.getElementById('postman-data');
  var s = (el && JSON.parse(el.textContent)) || {};
  var MEMO = 'ka-postman-saved', ENV_MEMO = 'ka-postman-env', DATA_MEMO = 'ka-postman-data';
  function message(e) { return String(e && e.message ? e.message : e).replace(/^Error: /, ''); }
  function recall(key) { try { return JSON.parse(sessionStorage.getItem(key) || 'null'); } catch (e) { return null; } }
  function keep(key, v) { try { if (v == null) sessionStorage.removeItem(key); else sessionStorage.setItem(key, JSON.stringify(v)); } catch (e) {} }
  // the default value a field was rendered with: what it is compared against to know it changed
  function was(f) {
    if (f.type === 'checkbox') return f.defaultChecked;
    if (f.tagName === 'SELECT') {
      for (var i = 0; i < f.options.length; i++) { if (f.options[i].defaultSelected) return f.options[i].value; }
      return f.options.length ? f.options[0].value : '';
    }
    return f.defaultValue;
  }
  function now(f) { return f.type === 'checkbox' ? f.checked : f.value; }
  var REFUSED = {
    STALE: 'This collection changed since you opened it — reload.',
    UNINTENDED_CHANGE: 'Saving would have changed more than what you edited, so nothing was saved. Reload and try again.',
    INVALID_COLLECTION: 'Saving would make the collection invalid for Postman, so nothing was saved. Check the values you typed.',
    NOT_FOUND: 'This request is no longer in the collection — reload.',
    NO_PROJECT: 'Choose a project first.'
  };
  function refusal(out) {
    if (REFUSED[out.code]) return REFUSED[out.code];
    if (out.code === 'NOT_EDITABLE') return 'Part of this change can’t be made here, so nothing was saved: ' + String(out.error || '') + '.';
    return 'This change could not be saved: ' + String(out.error || 'the console refused it') + '.';
  }
  function clip(t) { var v = String(t).replace(/\s+/g, ' ').trim(); return v.length > 40 ? v.substring(0, 39) + '…' : v === '' ? '(empty)' : v; }
  function parsed(t) { try { var v = JSON.parse(t); return v !== null && typeof v === 'object' ? { v: v } : null; } catch (e) { return null; } }
  // the leaves two JSON values differ at, each as 'name before → after', named by the nearest field name
  function leafDiff(a, b, at, out) {
    if (a && b && typeof a === 'object' && typeof b === 'object' && Array.isArray(a) === Array.isArray(b)) {
      var keys = Object.keys(a).concat(Object.keys(b).filter(function (k) { return !(k in a); }));
      keys.forEach(function (k) { leafDiff(a[k], b[k], Array.isArray(a) ? at : k, out); });
    } else if (JSON.stringify(a) !== JSON.stringify(b)) {
      out.push((at || 'value') + ' ' + (a === undefined ? '(none)' : clip(JSON.stringify(a))) + ' → ' + (b === undefined ? '(none)' : clip(JSON.stringify(b))));
    }
  }
  // the part two texts differ in, widened to the word or call around it: eql(160) and eql(240)
  function span(a, b) {
    var i = 0, j = 0, stop = /[\s.,;:=\/?&]/;
    while (i < a.length && i < b.length && a[i] === b[i]) i++;
    while (j < a.length - i && j < b.length - i && a[a.length - 1 - j] === b[b.length - 1 - j]) j++;
    var s = i, ea = a.length - j, eb = b.length - j;
    while (s > 0 && !stop.test(a[s - 1])) s--;
    while (ea < a.length && !stop.test(a[ea])) { ea++; eb++; }
    return [a.substring(s, ea), b.substring(s, eb)];
  }
  var PART = { raw: 'Body', 'graphql.query': 'GraphQL query', 'graphql.variables': 'GraphQL variables', file: 'File to send' };
  var ROWS = { query: 'Query parameter', variable: 'Path value', header: 'Header', urlencoded: 'Form field', formdata: 'Form field' };

  return {
    s: s, busy: null, running: !!s.running, progress: 'Running…', switching: false, err: '', said: '', saidBad: false, reloadable: false,
    preview: null, confirm: false, envSaid: '', envBad: false, authType: 'inherit', leaving: false, migrateSaid: '', migrateBad: false,
    dataOpen: false, dataSaid: '', dataBad: false,
    pend: {}, drops: {}, added: { query: [], header: [], prerequest: [], test: [] },

    init() {
      var auth = document.querySelector('[data-m-auth]');
      this.authType = auth ? auth.getAttribute('data-m-auth-was') || 'inherit' : 'inherit';
      var env = recall(ENV_MEMO);
      if (env && env.file === s.file) { this.envSaid = env.text; keep(ENV_MEMO, null); }
      var data = recall(DATA_MEMO);
      if (data && data.file === s.file) { this.dataSaid = data.text; keep(DATA_MEMO, null); }
      var self = this;
      window.addEventListener('beforeunload', function (e) { if (self.count() && !self.leaving) { e.preventDefault(); e.returnValue = ''; } });
      if (this.running) this.follow(s.running);
    },

    // ---- pending edits ----

    edit(f) {
      var d = f.dataset, key, entry;
      var v = now(f), same = v === was(f);
      if (d.mF) {
        key = 'f:' + d.mF;
        entry = {}; entry[d.mF] = v;
      } else if (d.mRows) {
        key = 'r:' + d.mRows + ':' + (d.mRow === '' ? 'new:' + d.mKey : d.mRow);
        var cur = this.pend[key] ? this.pend[key].entry : { rows: d.mRows, row: d.mRow === '' ? 'end' : Number(d.mRow), set: {} };
        if (d.mRow === '') cur.set.key = d.mKey;
        var cell = d.mCell === 'disabled' ? 'disabled' : d.mCell;
        var val = d.mCell === 'disabled' ? !v : v;
        if (same) delete cur.set[cell]; else cur.set[cell] = val;
        var left = Object.keys(cur.set).filter(function (k) { return !(d.mRow === '' && k === 'key'); });
        if (left.length) this.pend[key] = { order: 4, entry: cur }; else delete this.pend[key];
        this.pend = Object.assign({}, this.pend);
        return;
      } else if (d.mBodyPart) {
        key = 'body';
        var body = {};
        if (d.mBodyPart === 'graphql.query' || d.mBodyPart === 'graphql.variables') {
          var q = document.querySelector('[data-m-body-part="graphql.query"]'), vs = document.querySelector('[data-m-body-part="graphql.variables"]');
          same = q.value === q.defaultValue && vs.value === vs.defaultValue;
          body.graphql = { query: q.value, variables: vs.value };
        } else if (d.mBodyPart === 'file') {
          body.file = { src: v };
        } else {
          body.raw = v;
        }
        entry = { body: body };
      } else if (d.mAuthPart) {
        key = 'auth';
        var root = document.querySelector('[data-m-auth]');
        var type = root.querySelector('[data-m-auth-part="type"]').value;
        same = true;
        root.querySelectorAll('[data-m-auth-part]').forEach(function (x) { if (now(x) !== was(x)) same = false; });
        if (type === 'inherit') entry = { auth: 'inherit' };
        else {
          var a = { type: type };
          var fields = { bearer: ['token'], basic: ['username', 'password'], apikey: ['key', 'value', 'in'],
            oauth2: ['accessTokenUrl', 'clientId', 'clientSecret', 'scope'] }[type] || [];
          fields.forEach(function (k) { a[k] = root.querySelector('[data-m-auth-part="' + k + '"]').value; });
          if (type === 'oauth2') a.grant_type = 'client_credentials';
          entry = { auth: a };
        }
      } else if (d.mListen) {
        key = 's:' + d.mListen + ':' + d.mN;
        entry = { script: { listen: d.mListen, n: Number(d.mN), exec: v } };
      } else {
        return;
      }
      var order = d.mF ? 1 : d.mBodyPart ? 2 : d.mAuthPart ? 3 : 7;
      if (same) delete this.pend[key]; else this.pend[key] = { order: order, entry: entry };
      this.pend = Object.assign({}, this.pend);
    },

    // a row or script marked for removal; again keeps it
    drop(btn) {
      var at = btn.closest('[data-m-rowkey]');
      var k = at.getAttribute('data-m-rowkey');
      var d = Object.assign({}, this.drops);
      if (d[k]) delete d[k]; else d[k] = true;
      this.drops = d;
    },
    gone(node) {
      var at = node.closest ? node.closest('[data-m-rowkey]') : null;
      return !!(at && this.drops[at.getAttribute('data-m-rowkey')]);
    },

    // one Postman.edit: sets, then removals from the highest index down (so no index moves under another), then adds
    edits() {
      var item = s.item, out = [];
      var pend = Object.keys(this.pend).map(function (k) { return this.pend[k]; }, this)
        .filter(function (p) {
          var e = p.entry;
          if (e.rows != null && typeof e.row === 'number') return !this.drops[e.rows + ':' + e.row];
          if (e.script) return !this.drops['script:' + e.script.listen + ':' + e.script.n];
          return true;
        }, this)
        .sort(function (a, b) { return a.order - b.order; });
      pend.forEach(function (p) { out.push(Object.assign({ item: item }, p.entry)); });
      var drops = Object.keys(this.drops).map(function (k) { return k.split(':'); });
      drops.filter(function (p) { return p[0] !== 'script'; }).sort(function (a, b) { return Number(b[1]) - Number(a[1]); })
        .forEach(function (p) { out.push({ item: item, rows: p[0], row: Number(p[1]), remove: true }); });
      drops.filter(function (p) { return p[0] === 'script'; }).sort(function (a, b) { return Number(b[2]) - Number(a[2]); })
        .forEach(function (p) { out.push({ item: item, script: { listen: p[1], n: Number(p[2]), remove: true } }); });
      var added = this.added;
      ['query', 'header'].forEach(function (kind) {
        added[kind].forEach(function (r) {
          if (String(r.key).trim()) out.push({ item: item, rows: kind, row: 'end', set: { key: String(r.key).trim(), value: r.value } });
        });
      });
      ['prerequest', 'test'].forEach(function (listen) {
        added[listen].forEach(function (a) {
          if (String(a.text).trim()) out.push({ item: item, script: { listen: listen, n: 'end', exec: a.text } });
        });
      });
      return out;
    },
    count() { return this.edits().length; },

    discard() {
      var form = document.querySelector('[data-m-pm-form]');
      if (form) form.reset();
      this.pend = {}; this.drops = {};
      this.added = { query: [], header: [], prerequest: [], test: [] };
      var auth = document.querySelector('[data-m-auth]');
      this.authType = auth ? auth.getAttribute('data-m-auth-was') || 'inherit' : 'inherit';
      this.said = ''; this.preview = null; this.confirm = false;
    },

    // ---- save, check, undo ----

    async post(route, body) {
      var res = await fetch(route, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      var out = await res.json().catch(function () { return null; });
      if (!out) throw new Error('The console did not answer (' + res.status + ').');
      return out;
    },
    refuse(out) {
      this.said = refusal(out);
      this.saidBad = true;
      this.reloadable = out.code === 'STALE' || out.code === 'NOT_FOUND' || out.code === 'UNINTENDED_CHANGE';
    },
    // a changed path in a tester's words, relative to the item it is in; `listen` names a script's event
    words(path, listen) {
      var p = String(path).replace(/^(\/item\/\d+)+(?=\/)/, ''), m;
      var script = listen === 'prerequest' ? 'Script before the request' : listen === 'test' ? 'Tests script' : 'Script';
      if ((m = p.match(/^\/event\/(\d+)\/script\/exec(?:\/(\d+))?/))) return script + (m[2] != null ? ', line ' + (Number(m[2]) + 1) : '');
      if ((m = p.match(/^\/event(?:\/(\d+))?/))) return 'Scripts';
      if ((m = p.match(/^\/request\/url\/(query|variable)\/(\d+)/))) return ROWS[m[1]] + ' ' + (Number(m[2]) + 1);
      if ((m = p.match(/^\/request\/header\/(\d+)/))) return 'Header ' + (Number(m[1]) + 1);
      if ((m = p.match(/^\/request\/header/))) return 'Headers';
      if ((m = p.match(/^\/request\/body\/(urlencoded|formdata)\/(\d+)/))) return 'Form field ' + (Number(m[2]) + 1);
      if (/^\/request\/body\/graphql/.test(p)) return 'GraphQL';
      if (/^\/request\/body/.test(p)) return 'Body';
      if (/^\/request\/url/.test(p) || /^\/request$/.test(p)) return 'Address';
      if (/^\/request\/method/.test(p)) return 'Method';
      if (/^\/request\/auth|^\/auth/.test(p)) return 'Sign-in';
      if (/^\/(info\/)?name/.test(p)) return 'Name';
      if (/^\/(info\/|request\/)?description/.test(p)) return 'Description';
      return 'Another part of the request';
    },
    pathsOf(out) { return out && out.change && out.change.paths ? out.change.paths : []; },
    said_(paths) {
      var seen = {}, out = [], self = this;
      (paths || []).forEach(function (p) { var w = self.words(p); if (!seen[w]) { seen[w] = true; out.push(w); } });
      return out;
    },
    // each change of an edit's answer (change.diff) as '<what>: <before> → <after>'
    changesOf(out) {
      var diff = out && out.change && Array.isArray(out.change.diff) ? out.change.diff : null;
      if (!diff) return this.said_(this.pathsOf(out));
      var self = this, seen = {}, list = [];
      diff.forEach(function (d) {
        var w = self.words(d.path, d.listen) + self.fromTo(d);
        if (!seen[w]) { seen[w] = true; list.push(w); }
      });
      return list;
    },
    fromTo(d) {
      var had = 'before' in d, has = 'after' in d;
      if (/\/disabled$/.test(d.path)) return has && d.after === true ? ': turned off' : ': turned on';
      if (!had) return ': added' + (typeof d.after === 'string' && d.after ? ' ' + clip(d.after) : '');
      if (!has) return ': removed';
      var a = d.before, b = d.after;
      if (typeof a === 'string' && typeof b === 'string') {
        var ja = parsed(a), jb = parsed(b);
        if (ja && jb) {
          var leaves = [];
          leafDiff(ja.v, jb.v, '', leaves);
          if (leaves.length) return ': ' + leaves.slice(0, 3).join('; ') + (leaves.length > 3 ? '; …' : '');
        }
        var cut = span(a, b);
        return ': ' + clip(cut[0]) + ' → ' + clip(cut[1]);
      }
      return ': ' + clip(JSON.stringify(a)) + ' → ' + clip(JSON.stringify(b));
    },
    itemName() {
      var n = s.edit && s.edit.name ? s.edit.name.value : null;
      return s.item ? String(n && n.content != null ? n.content : n || 'this item') : 'the collection';
    },
    upgradeWords(up) {
      var list = Array.isArray(up) ? up : [];
      return list.length ? 'Saving rewrites how ' + list.length + (list.length == 1 ? ' part' : ' parts')
        + ' of this request is stored in the file, so Postman can hold the change. Save again to go ahead.' : '';
    },

    async check() {
      if (this.busy) return;
      this.busy = 'check'; this.said = ''; this.saidBad = false; this.preview = null; this.reloadable = false;
      try {
        var out = await this.post('/ui/postman/edit', { file: s.file, ifMatch: s.version, edits: this.edits(), dryRun: true });
        if (out.error) { this.refuse(out); return; }
        this.preview = { paths: this.pathsOf(out), words: this.changesOf(out) };
        var up = out.change && out.change.upgraded;
        if (up && up.length) { this.said = this.upgradeWords(up); this.confirm = true; }
        else if (!this.preview.paths.length) this.said = 'These edits change nothing in the file.';
      } catch (e) { this.said = message(e); this.saidBad = true; } finally { this.busy = null; }
    },

    // a dry run first, so a save that would rewrite how the request is stored says so before it lands
    async save() {
      if (this.busy) return;
      var edits = this.edits();
      if (!edits.length) return;
      this.busy = 'save'; this.said = ''; this.saidBad = false; this.reloadable = false;
      try {
        if (!this.confirm) {
          var dry = await this.post('/ui/postman/edit', { file: s.file, ifMatch: s.version, edits: edits, dryRun: true });
          if (dry.error) { this.refuse(dry); return; }
          var up = dry.change && dry.change.upgraded;
          if (up && up.length) { this.said = this.upgradeWords(up); this.confirm = true; return; }
        }
        var out = await this.post('/ui/postman/edit', { file: s.file, ifMatch: s.version, edits: edits });
        if (out.error) { this.refuse(out); this.confirm = false; return; }
        keep(MEMO, { file: s.file, version: out.version, undo: out.undo, paths: this.pathsOf(out), changes: this.changesOf(out), did: 'saved', item: s.item, name: this.itemName() });
        this.leaving = true;
        location.reload();
      } catch (e) { this.said = message(e); this.saidBad = true; } finally { if (!this.leaving) this.busy = null; }
    },

    // the last save on this file, while the file is still at the version it left
    saved() {
      var m = recall(MEMO);
      return m && m.file === s.file && m.version === s.version && m.undo ? m : null;
    },
    async undo() {
      var m = this.saved();
      if (!m || this.busy) return;
      this.busy = 'undo'; this.said = ''; this.saidBad = false;
      try {
        var out = await this.post('/ui/postman/edit', { file: s.file, ifMatch: s.version, undo: m.undo });
        if (out.error) { this.refuse(out); return; }
        keep(MEMO, { file: s.file, version: out.version, undo: out.undo, paths: m.paths, changes: m.changes, did: m.did === 'saved' ? 'undone' : 'saved', item: m.item, name: m.name });
        this.leaving = true;
        location.reload();
      } catch (e) { this.said = message(e); this.saidBad = true; } finally { if (!this.leaving) this.busy = null; }
    },

    // ---- run ----

    async run(btn) {
      if (this.running) return;
      if (this.count()) { this.err = 'Save or discard your changes first: a run uses the collection as saved.'; return; }
      this.err = '';
      this.running = true;
      var target = btn.getAttribute('data-m-pm-run');
      var opts = { item: target === '/' ? '' : target, async: true };
      var data = this.$refs.data ? this.$refs.data.value : '';
      if (data) opts.data = data;
      try {
        var out = await kaKickSuite(s.file, opts);
        if (!out || !out.runId) throw new Error((out && out.error) || 'The run did not start.');
        this.follow(out.runId, opts.item);
      } catch (e) { this.err = message(e); this.running = false; }
    },

    // where a run is: the row it is on over a data file, else the request
    progressOf(out) {
      var done = out && out.done > 0 ? out.done : 0, total = out && out.total > 0 ? out.total : 0, passes = out && out.passes > 1 ? out.passes : 0;
      if (!total) return 'Running…';
      if (passes) return 'Running… row ' + Math.min(passes, Math.floor(done / (total / passes)) + 1) + ' of ' + passes;
      return 'Running… request ' + Math.min(total, done + 1) + ' of ' + total;
    },

    // the run settles on its record; the page then reloads on it, with the item it ran selected
    follow(runId, item) {
      var self = this, tries = 0;
      this.progress = 'Running…';
      (async function poll() {
        while (tries++ < 1200) {
          await new Promise(function (r) { setTimeout(r, 500); });
          var res = await fetch('/ui/postman/run?runId=' + encodeURIComponent(runId)).catch(function () { return null; });
          var out = res && res.ok ? await res.json().catch(function () { return null; }) : null;
          if (out && out.status) self.progress = self.progressOf(out);
          if (out && out.status && out.status !== 'running' && out.status !== 'started') {
            var q = new URLSearchParams(location.search);
            q.set('run', runId);
            q.delete('row');
            if (item != null && !q.get('item')) q.set('item', item);
            location.replace(location.pathname + '?' + q.toString());
            return;
          }
        }
        self.running = false;
        self.err = 'The run is taking long. Open it on Runs to watch it.';
      })();
    },

    // ---- data files: written create-only next to the collection, then picked for the next run ----

    saveData() {
      var name = this.$refs.dataName ? this.$refs.dataName.value.trim() : '';
      if (!name) { this.dataBad = true; this.dataSaid = 'Give the data file a name first.'; return; }
      if (!/\.(csv|json)$/i.test(name)) name += '.csv';
      this.writeData(name, this.$refs.dataText ? this.$refs.dataText.value : '');
    },
    async uploadData(input) {
      var f = input.files && input.files[0];
      input.value = '';
      if (!f) return;
      if (!/\.(csv|json)$/i.test(f.name)) { this.dataBad = true; this.dataSaid = 'A data file is a .csv or a .json file; ' + f.name + ' is neither.'; return; }
      this.writeData(f.name, await f.text());
    },
    async writeData(name, text) {
      if (this.busy) return;
      this.busy = 'data'; this.dataSaid = ''; this.dataBad = false;
      try {
        var out = await this.post('/ui/postman/data', { file: s.file, item: s.item, to: (s.dataDir || '') + name, text: text });
        if (out.error) {
          this.dataBad = true;
          this.dataSaid = out.code === 'EXISTS' ? 'A file named ' + name + ' is already there. Pick another name.'
            : out.code === 'BAD_DATA' ? 'Nothing was saved: ' + String(out.error).replace(/^Postman\.data: /, '') + '.'
            : out.code === 'NO_PROJECT' ? 'Choose a project first.' : 'Nothing was saved: ' + String(out.error) + '.';
          return;
        }
        keep(DATA_MEMO, { file: s.file, text: 'Saved ' + out.path + ': ' + out.rows + (out.rows == 1 ? ' row' : ' rows')
          + '. It is picked for the next run.' });
        var q = new URLSearchParams(location.search);
        q.set('data', out.path);
        this.leaving = true;
        location.replace(location.pathname + '?' + q.toString());
      } catch (e) { this.dataBad = true; this.dataSaid = message(e); } finally { if (!this.leaving) this.busy = null; }
    },

    // ---- environments ----

    async switchEnv(name) {
      if (this.switching) return;
      this.err = '';
      this.switching = true;
      try {
        var out = await this.post('/ui/runner/env', { name: name || null });
        if (out.error) throw new Error(out.error);
        location.reload();
      } catch (e) { this.err = message(e); this.switching = false; }
    },

    async importEnv() {
      if (this.busy) return;
      var from = this.$refs.envFrom ? this.$refs.envFrom.value : '';
      var env = this.$refs.envName ? this.$refs.envName.value.trim() : '';
      if (!from) return;
      this.busy = 'env'; this.envSaid = ''; this.envBad = false;
      try {
        var out = await this.post('/ui/postman/env', { file: s.file, from: from, env: env || null, ifMatch: s.configVersion });
        if (out.error) {
          this.envBad = true;
          this.envSaid = out.code === 'STALE' ? 'The project settings changed since you opened this page — reload.'
            : 'Nothing was brought in: ' + String(out.error) + '.';
          return;
        }
        var entries = (Array.isArray(out.entries) ? out.entries : []).filter(function (e) { return (e.env || '') === env; });
        var secrets = entries.filter(function (e) { return e.secret; }).length;
        var skipped = Array.isArray(out.skipped) ? out.skipped.map(function (k) { return k.name; }) : [];
        keep(ENV_MEMO, { file: s.file, text: 'Brought in ' + entries.length + (entries.length == 1 ? ' value' : ' values')
          + (env ? ' for the ' + env + ' environment' : '') + (secrets ? ', ' + secrets + (secrets == 1 ? ' kept as a secret' : ' kept as secrets') : '')
          + (skipped.length ? '. Left out: ' + skipped.join(', ') + ' (not a usable name)' : '') + '.' });
        location.reload();
      } catch (e) { this.envBad = true; this.envSaid = message(e); } finally { this.busy = null; }
    },

    // ---- migrate: the folder's feature (Postman.migrate), handed to the API test page's assistant to run and save ----

    async migrate() {
      if (this.busy || !s.migrate || !s.model) return;
      this.busy = 'migrate'; this.migrateSaid = ''; this.migrateBad = false;
      try {
        var out = await this.post('/ui/postman/migrate', { file: s.file, item: s.item });
        if (out.error) {
          this.migrateBad = true;
          this.migrateSaid = 'Nothing was migrated: ' + String(out.error) + '.';
          return;
        }
        var chat = await this.post('/ui/apitest/chat', { test: out.to, message: 'Migrate ' + s.migrate.what + ' to Karate as '
          + out.to + ': run it, fix it until it passes, then suggest the requirements it proves.' });
        if (chat.error) {
          this.migrateBad = true;
          this.migrateSaid = chat.code === 'BUSY' ? 'The assistant is busy with another test — try again when it is done.' : String(chat.error);
          return;
        }
        // the page of the candidate, where its question shows if its run is put to the tester
        try { sessionStorage.setItem('ka.apichat.' + out.to, chat.chat); } catch (e) {}
        this.leaving = true;
        location.href = '/manual/tests/api?test=' + encodeURIComponent(out.to) + '&chat=open';
      } catch (e) { this.migrateBad = true; this.migrateSaid = message(e); } finally { if (!this.leaving) this.busy = null; }
    }
  };
}
