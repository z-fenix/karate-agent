/*
 * /ui/help — the Help tab's data channel (D124, D98). Runs in a FRESH per-request engine with the
 * template globals in scope (`help` — the Help JsDispatch instance), so a page render never queues
 * behind a bot op on the shared run engine's evalLock. Same verbs an LLM composes over
 * /api/eval (P3) — a thin route shim. The core api-handler channel strips the /ui prefix:
 * request.path here is e.g. /help/page.
 *
 * Routes:
 *   GET /help/pages           → help.pages()    (the page list — datasheet, rules)
 *   GET /help/topics          → help.topics()   (the full help-id index)
 *   GET /help/page?id=         → help.page(id)    ({id, title, markdown})
 *   GET /help/get?id=          → help.get(id)     (resolve a help-id → page markdown + anchor + url)
 */
function send(out) {
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.get && request.pathMatches('/help/pages')) {
  send(help.pages());
} else if (request.get && request.pathMatches('/help/topics')) {
  send(help.topics());
} else if (request.get && request.pathMatches('/help/page')) {
  send(help.page(request.param('id')));
} else if (request.get && request.pathMatches('/help/get')) {
  send(help.get(request.param('id')));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
