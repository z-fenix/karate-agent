/*
 * /ui/checks — the Checks explorer's data channel (D98: these pure data verbs ride a fresh per-request
 * engine, never the run engine's evalLock, so they can't queue behind a long bot op). `file` is the
 * template global; run/kickoff ops (Harness.*, the live-bot Check.run) stay on /api/eval — they target
 * the live run and belong on the transcript.
 *
 * The Checks page is now a single UNIFIED project tree (VS Code-style): every path is PROJECT-ROOT
 * relative (no checks/ vs root split, no `root` flag). `checks/` is just the recommended folder; the
 * tree lazy-loads one folder level at a time via File.ls — it never walks the whole project.
 *
 * Routes (the /ui prefix is stripped):
 *   GET  /checks/ls?dir=<rel>     → one directory level: [{name,dir,size,modified}] (folders first),
 *                                   minus dotfiles and noisy build dirs (DENY). dir defaults to root.
 *   GET  /checks/read?name=<n>    → {content} (or the File error envelope) — any text file
 *   POST /checks/write   {name, content}  → the File.write result ({error} on a disk conflict)
 *   POST /checks/delete  {name}           → {ok:true} (or the File error envelope; recurses a folder)
 *   POST /checks/move    {from, to}       → {ok:true} — the one rename/move verb (binary-safe)
 *   POST /checks/mkdir   {name}           → {ok:true} — create a real (persisted) folder
 *   GET  /checks/meta?name=<n>    → {description, params} from a file's header (the properties popover);
 *                                   a checks/*.js answers what Check.info reads: + {title, req, ai}
 *   POST /checks/link    {name, stamp}   → Check.link — add an id (or a list) to the check's // @req: header
 *   POST /checks/annotate {name, title?, description?, req?, ifMatch?} → Check.annotate — the header in one
 *                                   write; a stale ifMatch answers 409 STALE_MTIME
 *   GET  /checks/glob?pattern=<g> → File.list(pattern) — the glob-lens file lists (D193: Req/Rules/Feat)
 *   POST /checks/upload?name=<subpath>   raw octet-stream body → {ok:true, written} — a BINARY artifact
 *                                   (upload-fixture PDF/PNG, data blob). The body IS the file
 *                                   (request.bodyBytes — no multipart parser in core), so the target
 *                                   rides ?name=. Project-root relative (drop a fixture under checks/ by
 *                                   passing name=checks/<file>).
 */
function send(out) {
  // a domain failure carrying an explicit status (e.g. a BAD_REQUEST → 400) mirrors onto the HTTP
  // response so the page fetch layer can branch on it — same contract as the rulebooks /ui handler
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

// folder names a project tree should never surface — build output, VCS/IDE metadata, deps. Filtered
// per-level in /ls (dotfiles are dropped too) so the explorer stays focused on authored artifacts.
var DENY = { 'node_modules': 1, 'target': 1, 'build': 1, 'dist': 1, '.git': 1, '.idea': 1, '.vscode': 1 };

// a .js under checks/ is a check — its header is read by Check itself, not re-parsed here
function isCheck(name) {
  var n = String(name || '').replace(/^\/+/, '');
  return typeof check !== 'undefined' && n.indexOf('checks/') === 0 && /\.js$/.test(n);
}

if (request.get && request.pathMatches('/checks/ls')) {
  var dir = request.param('dir') || '';
  var entries = file.ls(dir);
  if (entries && entries.error) { send(entries); }
  else {
    var visible = [];
    for (var i = 0; i < entries.length; i++) {
      var e = entries[i];
      if (e.name.charAt(0) === '.') continue;        // dotfiles/dotdirs — noise
      if (e.dir && DENY[e.name]) continue;           // build output / VCS / IDE dirs
      visible.push(e);
    }
    send(visible);
  }
} else if (request.get && request.pathMatches('/checks/read')) {
  var content = file.read(request.param('name'));
  send(content && content.error ? content : { content: content });
} else if (request.post && request.pathMatches('/checks/write')) {
  var body = request.bodyJson || {};
  var result = file.write(body.name, body.content);
  send(result && result.error ? result : { ok: true });
} else if (request.post && request.pathMatches('/checks/delete')) {
  var db = request.bodyJson || {};
  var dr = file.delete(db.name);
  send(dr && dr.error ? dr : { ok: true });
} else if (request.post && request.pathMatches('/checks/move')) {
  var mb = request.bodyJson || {};
  if (!mb.from || !mb.to) {
    send({ error: 'move requires {from, to}', code: 'BAD_REQUEST', status: 400 });
  } else {
    var mr = file.move(mb.from, mb.to);
    send(mr && mr.error ? mr : { ok: true });
  }
} else if (request.post && request.pathMatches('/checks/mkdir')) {
  var kb = request.bodyJson || {};
  if (!kb.name) {
    send({ error: 'mkdir requires {name}', code: 'BAD_REQUEST', status: 400 });
  } else {
    var kr = file.mkdir(kb.name);
    send(kr && kr.error ? kr : { ok: true });
  }
} else if (request.get && request.pathMatches('/checks/meta')) {
  // properties popover — a checks/*.js reads through Check.info, so the page and the JSAPI agree on what
  // the header says; any other text file falls back to a scan of its leading lines
  var mname = request.param('name');
  var info = isCheck(mname) ? check.info(mname) : null;
  var src = info && !info.error ? null : file.read(mname);
  if (info && !info.error) {
    send({ description: info.description, params: info.params.join(', '), title: info.title, req: info.req, ai: info.ai });
  } else if (src && src.error) { send(src); }
  else {
    var meta = { description: '', params: '' };
    var lines = String(src).split('\n');
    for (var li = 0; li < lines.length && li < 40; li++) {
      var dm = lines[li].match(/^\s*(?:\/\/|#)?\s*@description:\s*(.*)$/);
      if (dm) { meta.description = dm[1].trim(); }
      var pm = lines[li].match(/^\s*(?:\/\/|#)?\s*@params:\s*(.*)$/);
      if (pm) { meta.params = pm[1].trim(); }
    }
    send(meta);
  }
} else if (request.post && request.pathMatches('/checks/link')) {
  var lb = request.bodyJson || {};
  if (!lb.name || !lb.stamp) {
    send({ error: 'link requires {name, stamp}', code: 'BAD_REQUEST', status: 400 });
  } else if (typeof check === 'undefined') {
    send({ error: 'no Check surface on this server', code: 'NOT_FOUND', status: 404 });
  } else {
    send(check.link(lb.name, lb.stamp));
  }
} else if (request.post && request.pathMatches('/checks/annotate')) {
  var ab = request.bodyJson || {};
  var fields = {};
  ['title', 'description', 'req'].forEach(function (k) { if (ab[k] !== undefined) fields[k] = ab[k]; });
  if (!ab.name) {
    send({ error: 'annotate requires {name, title?, description?, req?}', code: 'BAD_REQUEST', status: 400 });
  } else if (typeof check === 'undefined') {
    send({ error: 'no Check surface on this server', code: 'NOT_FOUND', status: 404 });
  } else {
    send(check.annotate(ab.name, fields, ab.ifMatch ? { ifMatch: ab.ifMatch } : {}));
  }
} else if (request.get && request.pathMatches('/checks/glob')) {
  // the glob-lens file lists (D193 — e.g. Req = requirements/**/*.md, Feat = **/*.feature): a flat,
  // sandboxed File.list over the project (same power the /api/eval surface already grants). The generated
  // trees stay pruned (a lens showing target/'s copy of a feature is noise), but a tree view wants the
  // whole list rather than the read-it-yourself cap, so it asks for a limit no real project reaches.
  send(file.list(request.param('pattern') || '', { limit: 5000 }));
} else if (request.post && request.pathMatches('/checks/upload')) {
  var uname = request.param('name');
  if (!uname) {
    send({ error: 'upload requires ?name=<path under the project>', code: 'BAD_REQUEST', status: 400 });
  } else {
    // binary ingress: the body IS the file (request.bodyBytes). Project-root relative.
    var ur = file.writeBytes(uname, request.bodyBytes);
    send(ur && ur.error ? ur : { ok: true, written: ur });
  }
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
