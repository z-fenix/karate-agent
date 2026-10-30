/*
 * /ui/traffic — the calls panel's data channel (traffic §6; D98: a fresh per-request engine). `traffic` is the
 * template global (the Traffic namespace). The sift is a pure function of the recording and its options, so the
 * tester's choices are kept here, per browser, under session.ui.traffic[<source>] = {moves, hosts, chat}, and every
 * answer is Traffic.sift(source, {moves, hosts}) with all of them.
 *
 * Routes (the /ui prefix is stripped):
 *   GET  /traffic/sift?source=<run id>           → Traffic.sift(source, this browser's choices)
 *   POST /traffic/move {source, id, to}          → the tester moves one call to 'kept' or 'left' (a move that
 *                                                  reverses the tester's own earlier move removes it), then the sift
 *   POST /traffic/host {source, host}            → the tester keeps a site (a same-company-domain host), then the sift
 *   A refusal ({error, code}) stores nothing and answers 400 (404 for an id the recording does not hold).
 */
function send(out) {
  if (out && out.error) {
    response.status = out.status || (out.code == 'NOT_FOUND' ? 404 : 400);
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

function list(v) {
  var out = [];
  for (var i = 0; v && i < v.length; i++) out.push(v[i]);
  return out;
}

function choices(source) {
  var all = session && session.ui && session.ui.traffic ? session.ui.traffic : {};
  var c = all[source] || {};
  return { moves: list(c.moves), hosts: list(c.hosts) };
}

function opts(c) {
  var o = {};
  if (c.moves.length) o.moves = c.moves;
  if (c.hosts.length) o.hosts = c.hosts;
  return o;
}

function keep(source, c) {
  if (!session) context.init();
  var ui = session.ui || {};
  var all = ui.traffic || {};
  var was = all[source] || {};
  all[source] = { moves: c.moves, hosts: c.hosts, chat: was.chat || null };   // the chat made from it stays its own
  ui.traffic = all;
  session.ui = ui;   // reassign the whole map so the store persists the mutation
}

function find(rows, id) {
  for (var i = 0; rows && i < rows.length; i++) if (String(rows[i].id) == id) return rows[i];
  return null;
}

// one change to the choices, answered with the sift over all of them; a refused sift stores nothing
function change(source, edit) {
  var c = choices(source);
  var before = traffic.sift(source, opts(c));
  if (before.error) return before;
  var next = edit(c, before);
  if (next.error) return next;
  var after = traffic.sift(source, opts(next));
  if (!after.error) keep(source, next);
  return after;
}

if (request.get && request.pathMatches('/traffic/sift')) {
  var source = String(request.param('source') || '');
  send(traffic.sift(source, opts(choices(source))));
} else if (request.post && request.pathMatches('/traffic/move')) {
  var body = request.bodyJson || {};
  var id = String(body.id || '');
  var to = String(body.to || '');
  if (to != 'kept' && to != 'left') {
    send({ error: 'to is kept or left', code: 'BAD_REQUEST' });
  } else {
    send(change(String(body.source || ''), function (c, sift) {
      var kept = find(sift.kept, id), left = find(sift.left, id);
      var row = kept || left;
      if (!row) return { error: 'This recording has no call ' + id + '.', code: 'NOT_FOUND' };
      var moves = [];
      for (var i = 0; i < c.moves.length; i++) if (String(c.moves[i].id) != id) moves.push(c.moves[i]);
      var there = (to == 'kept' && kept) || (to == 'left' && left);
      if (there) return c;
      // moving back a call the tester moved puts it where the sift had it; any other row takes a move
      if (!(row.moved && row.moved.by == 'tester')) moves.push({ id: id, to: to, by: 'tester' });
      return { moves: moves, hosts: c.hosts };
    }));
  }
} else if (request.post && request.pathMatches('/traffic/host')) {
  var body = request.bodyJson || {};
  var host = String(body.host || '').trim();
  send(change(String(body.source || ''), function (c) {
    if (!host) return { error: 'host is the site to keep', code: 'BAD_REQUEST' };
    if (c.hosts.indexOf(host) < 0) c.hosts.push(host);
    return c;
  }));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
