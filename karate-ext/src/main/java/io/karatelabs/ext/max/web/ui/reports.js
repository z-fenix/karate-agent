/*
 * /ui/reports — the Reports tab's generate action (D98 migration off /api/eval: a pure
 * graph-rebuild + render, no live-run state). Fresh per-request engine; `report` is the
 * template global. Composes the same two verbs an LLM uses (P3): aggregate (project | one
 * run | a picked set) then traceability.
 *
 * Route (the /ui prefix is stripped):
 *   POST /reports/generate  {runs: [id…]}  → {items, hits, attested, runs, verdict}
 */
function send(out) {
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.post && request.pathMatches('/reports/generate')) {
  var body = request.bodyJson || {};
  var picked = body.runs || [];
  var agg = picked.length
    ? report.aggregate({ run: picked.length === 1 ? picked[0] : picked })
    : report.aggregate();
  if (agg && agg.error) {
    send(agg);
  } else {
    var rtm = agg.rtmDir ? report.traceability({ reportDir: agg.rtmDir }) : report.traceability();
    if (rtm && rtm.error) {
      send(rtm);
    } else {
      send({ items: agg.items, hits: agg.hits, attestedHits: agg.attestedHits,
             attestedRuns: agg.attestedRuns, verdict: rtm.verdict || '' });
    }
  }
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
