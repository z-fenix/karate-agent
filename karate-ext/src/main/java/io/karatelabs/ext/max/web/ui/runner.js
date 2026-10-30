/*
 * /ui/runner — the app-under-test read and the env switch for pages (D98: a FRESH per-request engine, never the
 * run engine's evalLock). `runner` is the template global (RunnerPane).
 *
 * Routes (the /ui prefix is stripped):
 *   GET /runner/target?env=<e> → runner.target(e): {env, var, url, reachable, status?, latencyMs?, error?,
 *                                hint?} — an unreachable app is a result (200); an env that is not a plain
 *                                name is a 400 envelope.
 *   GET  /runner/envs          → runner.envs(): [{name, current}]
 *   POST /runner/env {name}    → Runner.env(name): {env, previous}; name null is the default; an undeclared or
 *                                pinned env is refused
 *   GET  /runner/auth?env=<e>  → runner.auth(e): {env, scheme, identity?, label}
 */
function send(out) {
  if (out && out.error && out.code) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.get && request.pathMatches('/runner/target')) {
  send(runner.target(request.param('env')));
} else if (request.get && request.pathMatches('/runner/envs')) {
  send(runner.envs());
} else if (request.post && request.pathMatches('/runner/env')) {
  send(runner.env((request.bodyJson || {}).name));
} else if (request.get && request.pathMatches('/runner/auth')) {
  send(runner.auth(request.param('env')));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
