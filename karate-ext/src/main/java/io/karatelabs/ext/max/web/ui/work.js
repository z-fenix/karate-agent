/*
 * /ui/work — the project picker's read channel (D126). Pure data verbs on a fresh per-request engine;
 * `work` is the template global. The re-anchor itself (Work.use) is a deliberate state change that
 * re-seeds the shared run engine, so it rides /api/eval (like a kickoff), NOT this read channel.
 *
 * Routes (the /ui prefix is stripped):
 *   GET /work            → work.list()   — the projects under the ceiling
 *   GET /work/current    → work.current()
 *   GET /work/browse?dir=<d> → work.browse(d) — one lazy level under <d> (ceiling-relative; default ceiling)
 */
function send(out) {
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.get && request.pathMatches('/work')) {
  send(work.list());
} else if (request.get && request.pathMatches('/work/current')) {
  send(work.current());
} else if (request.get && request.pathMatches('/work/browse')) {
  send(work.browse(request.param('dir')));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
