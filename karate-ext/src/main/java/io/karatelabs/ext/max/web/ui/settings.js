/*
 * /ui/settings — the Settings page's data channel (D98: a FRESH per-request engine, so a preflight
 * that waits on a provider round-trip never queues behind a bot op on the run engine's evalLock).
 * `harness` is the template global.
 *
 * Routes (the /ui prefix is stripped):
 *   POST /settings/ping {provider?, model?}  → Harness.ping's map verbatim: {ok:true, provider, model,
 *                                              latencyMs} or {ok:false, provider, model, status?,
 *                                              error, hint?} — a provider failure is a result, never
 *                                              a throw, so this is always a 200 the page branches on.
 */
function send(out) {
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.post && request.pathMatches('/settings/ping')) {
  var body = request.bodyJson || {};
  var opts = {};
  if (body.provider) { opts.provider = body.provider; }
  if (body.model) { opts.model = body.model; }
  send(harness.ping(opts));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
