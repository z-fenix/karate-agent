/*
 * /ui/postman — the Postman page's data channel (D98: a fresh per-request engine). `postman` is the template global
 * (the Postman namespace); each route is one verb (postman SPEC §4). Running stays on /api/eval (Runner.run).
 *
 * Routes (the /ui prefix is stripped):
 *   GET  /postman/read?file=<path>[&runId=][&item=]      → Postman.read(file, {runId, item})
 *   GET  /postman/item?file=<path>&item=<pointer>        → Postman.item(file, {item})
 *   POST /postman/edit {file, ifMatch, edits, dryRun?}   → Postman.edit(file, {ifMatch, edits, dryRun})
 *   POST /postman/edit {file, ifMatch, undo}             → Postman.edit(file, {ifMatch, undo})
 *   GET  /postman/run?runId=<id>                         → {runId, status, total, passes} of Harness.get(runId), and
 *                                                          done: how many (row, request) results Postman.results has
 *   GET  /postman/results?runId=<id>[&item=][&row=]      → Postman.results(runId, {item, row, full: true})
 *   GET  /postman/env?file=<path>[&env=][&data=]         → Postman.env(file, {env, data})
 *   POST /postman/env {file, from, env, ifMatch}         → Postman.env(file, {from, env, ifMatch})
 *   POST /postman/migrate {file, item?, to?, data?}      → Postman.migrate(file, {item, to, data}): the feature, written nowhere
 *   GET  /postman/data?file=<path>[&item=]              → Postman.data(file, {item}): a new data file's columns and path
 *   POST /postman/data {file, item?, to?, text}         → Postman.data(file, {item, to, text}): the file, written create-only
 *   A refusal ({error, code, status}) answers with its status.
 */
function send(out) {
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

function opt(o, key, value) {
  if (value != null && value !== '') o[key] = value;
  return o;
}

if (request.get && request.pathMatches('/postman/read')) {
  var o = opt({}, 'runId', request.param('runId'));
  send(postman.read(request.param('file'), opt(o, 'item', request.param('item'))));
} else if (request.get && request.pathMatches('/postman/item')) {
  send(postman.item(request.param('file'), { item: request.param('item') || '' }));
} else if (request.post && request.pathMatches('/postman/edit')) {
  var body = request.bodyJson || {};
  var opts = body.undo != null ? { ifMatch: body.ifMatch, undo: body.undo } : { ifMatch: body.ifMatch, edits: body.edits };
  if (body.dryRun) opts.dryRun = true;
  send(postman.edit(body.file, opts));
} else if (request.get && request.pathMatches('/postman/run')) {
  var id = String(request.param('runId') || '');
  var meta = harness.get(id);
  if (meta && !meta.error && meta.status) {
    var so = postman.results(id, {});
    send({ runId: id, status: meta.status, done: so && so.length != null && !so.error ? so.length : 0,
      total: meta.total || null, passes: meta.passes || null });
  } else {
    send({ error: 'no run ' + id, code: 'NOT_FOUND', status: 404 });
  }
} else if (request.get && request.pathMatches('/postman/results')) {
  var r = opt({ full: true }, 'item', request.param('item'));
  if (request.param('row') != null && request.param('row') !== '') r.row = request.paramInt('row');
  send(postman.results(request.param('runId'), r));
} else if (request.get && request.pathMatches('/postman/env')) {
  send(postman.env(request.param('file'), opt(opt({}, 'env', request.param('env')), 'data', request.param('data'))));
} else if (request.post && request.pathMatches('/postman/env')) {
  var b = request.bodyJson || {};
  send(postman.env(b.file, opt({ from: b.from, ifMatch: b.ifMatch }, 'env', b.env)));
} else if (request.post && request.pathMatches('/postman/migrate')) {
  var m = request.bodyJson || {};
  send(postman.migrate(m.file, opt(opt(opt({}, 'item', m.item), 'to', m.to), 'data', m.data)));
} else if (request.get && request.pathMatches('/postman/data')) {
  send(postman.data(request.param('file'), opt({}, 'item', request.param('item'))));
} else if (request.post && request.pathMatches('/postman/data')) {
  var d = request.bodyJson || {};
  send(postman.data(d.file, opt(opt({ text: d.text }, 'item', d.item), 'to', d.to)));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
