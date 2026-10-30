/*
 * /ui/rulebooks — the rulebook pages' data channel (D98). Runs in a FRESH per-request engine with
 * the template globals in scope (`rule` — the Rule JsDispatch instance), so a page action never
 * queues behind a bot op on the shared run engine's evalLock. Same verbs an LLM composes over
 * /api/eval (P3) — this file is a thin route shim, zero logic. The core api-handler channel strips
 * the /ui prefix: request.path here is e.g. /rulebooks/<name>/run.
 *
 * Routes (v1 is save-then-run — no run-with-unsaved-buffer Overrides):
 *   GET  /rulebooks                              → Rule.list()
 *   GET  /rulebooks/{name}?tables=full           → Rule.load(name, {tables})   (incl. lookups + mtimes;
 *                                                  file tables as summaries unless tables=full)
 *   GET  /rulebooks/{name}/diagram               → Rule.diagram(name)  ({svg, mermaid, outline,
 *                                                  collapsed, collapsibleAll} — the server-rendered
 *                                                  flowchart of the SAVED calc.js, big containers folded)
 *   POST /rulebooks/{name}/diagram               {expand?, compact?}  → Rule.diagram(name, opts) —
 *                                                  expand state (container ids | 'all') (D111-b) +
 *                                                  the business-view compact toggle (D114; default ON)
 *   GET  /rulebooks/{name}/explore?suite=         → Rule.explore(name, {suite})  (runtime branch
 *                                                  coverage + the suggest-scenarios suite — D111;
 *                                                  suite = coverage|pairwise|boundary — D114)
 *   POST /rulebooks/{name}/table                  {suite?}  → Rule.table(name, {suite}) — the
 *                                                  decision-table projection (D114)
 *   GET  /rulebooks/{name}/report                  → Rule.report(name) into the artifact `reports`
 *                                                  root → {url} (the "Analysis report ↗" button opens it)
 *   POST /rulebooks/{name}/mutate                  → Rule.mutate(name) (the self-grade) then
 *                                                  Rule.report(name) → {url#analysis=mutation, counts,
 *                                                  denominator, rawKillRate, independentScore, worklist}
 *                                                  (the "Self-grade ↗" button opens the Mutation tab)
 *   POST /rulebooks/{name}/execute               {input}  → Rule.execute(name, input) — ad-hoc run
 *                                                  for the Scenario Coverage workbench → {output, audit}
 *   POST /rulebooks/{name}/run                   {scenarioId, trace?}        → Rule.run(…, {diff:true,
 *                                                  trace}) — trace lights the diagram hot path
 *   POST /rulebooks/{name}/scenarios             {row, ifMatch}              → Rule.scenario.create
 *   POST /rulebooks/{name}/scenarios/{id}        {patch, ifMatch}            → Rule.scenario.update
 *   POST /rulebooks/{name}/scenarios/{id}/delete {ifMatch}                   → Rule.scenario.delete
 *   POST /rulebooks/{name}/calc                  {calc, ifMatch}             → Rule.calc.update
 *   POST /rulebooks/{name}/lookups               {lookups, ifMatch}          → Rule.lookups.update
 *   GET  /rulebooks/{name}/twin                   → the Twin pane payload (§17.4): {rule, twin:false}
 *                                                  when the rulebook ships no twin.js, else the explore
 *                                                  summary + the check envelope + the sequence rows (a
 *                                                  refused walk rides as `error` beside them)
 *   POST /rulebooks/{name}/twin                  {sequenceId}                → Twin.run — replay one
 *                                                  saved sequence under the CURRENT model
 *   GET  /rulebooks/{name}/play                   → the play records slot (playground.md §6 (1)): the
 *                                                  quarantine's records for this rulebook, each with its
 *                                                  Twin.run worlds, its confirmation and its criterion
 *                                                  candidates
 *   POST /rulebooks/{name}/play/{id}/confirm      {expect, req}  → writes the record's confirm.json —
 *                                                  the only writer of a confirmation (§6 (1))
 *
 * A domain failure is DATA ({error, code, status} — e.g. a stale ifMatch → status 409): the
 * handler mirrors that status onto the HTTP response so the page's fetch layer can branch on it.
 */
function send(out) {
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

// request.bodyJson — the explicit coercing accessor (parsed object whatever the content-type)
var body = (request.post && request.bodyJson) || {};

if (request.get && request.pathMatches('/rulebooks')) {
  send(rule.list());
} else if (request.post && request.pathMatches('/rulebooks/{name}/run')) {
  send(rule.run(request.pathParams.name, body.scenarioId, { diff: true, trace: !!body.trace }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/execute')) {
  // ad-hoc input run (the Scenario Coverage workbench — any row, saved or generated) → {output, audit}.
  // Rule.execute returns a LIVE RuleResult (a JsDispatch handle whose verify() verb is run-only) — a live
  // handle can't be assigned to response.body (HttpResponse can't serialize it). The page only needs the
  // DATA, so project the result's data members to a plain object. Keys per RuleResult (Rule.java): output
  // / outcome (only when calc.outcome was set) / audit / reqs / durationMs / truncated. A domain failure is
  // a plain {error,...} Map — pass it through.
  var rr = rule.execute(request.pathParams.name, body.input);
  send((rr && rr.error) ? rr : {
    output: rr.output, outcome: rr.outcome, audit: rr.audit, reqs: rr.reqs,
    durationMs: rr.durationMs, truncated: rr.truncated
  });
} else if (request.post && request.pathMatches('/rulebooks/{name}/scenarios')) {
  send(rule.scenario.create(request.pathParams.name, body.row, { ifMatch: body.ifMatch }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/scenarios/{id}/delete')) {
  send(rule.scenario['delete'](request.pathParams.name, request.pathParams.id, { ifMatch: body.ifMatch }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/scenarios/{id}')) {
  send(rule.scenario.update(request.pathParams.name, request.pathParams.id, body.patch, { ifMatch: body.ifMatch }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/calc')) {
  send(rule.calc.update(request.pathParams.name, body.calc, { ifMatch: body.ifMatch }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/lookups')) {
  send(rule.lookups.update(request.pathParams.name, body.lookups, { ifMatch: body.ifMatch }));
} else if (request.get && request.pathMatches('/rulebooks/{name}/diagram')) {
  send(rule.diagram(request.pathParams.name));
} else if (request.post && request.pathMatches('/rulebooks/{name}/diagram')) {
  send(rule.diagram(request.pathParams.name, { expand: body.expand, compact: body.compact }));
} else if (request.get && request.pathMatches('/rulebooks/{name}/twin')) {
  send(twin.section(request.pathParams.name));
} else if (request.post && request.pathMatches('/rulebooks/{name}/twin')) {
  send(twin.run(request.pathParams.name, body.sequenceId));
} else if (request.get && request.pathMatches('/rulebooks/{name}/play')) {
  send(twin.plays(request.pathParams.name));
} else if (request.post && request.pathMatches('/rulebooks/{name}/play/{id}/confirm')) {
  send(twin.confirm(request.pathParams.name, request.pathParams.id, body.expect, body.req));
} else if (request.get && request.pathMatches('/rulebooks/{name}/explore')) {
  send(rule.explore(request.pathParams.name, { suite: request.param('suite') }));
} else if (request.post && request.pathMatches('/rulebooks/{name}/table')) {
  send(rule.table(request.pathParams.name, { suite: body.suite }));
} else if (request.get && request.pathMatches('/rulebooks/{name}/report')) {
  // generate the standalone Rule Analysis report (Coverage · Value Ranges · Rule Check · What-If)
  // into the artifact `reports` root (rule.report defaults `out` there), then hand the page a
  // ready-to-open artifact URL — the "Analysis report ↗" button window.opens it (D119/D120).
  var rep = rule.report(request.pathParams.name);
  if (rep && rep.error) {
    send(rep);
  } else {
    var fileName = String(rep.file).split('/').pop();
    send({ url: '/api/artifacts/reports/' + encodeURIComponent(fileName), file: rep.file });
  }
} else if (request.post && request.pathMatches('/rulebooks/{name}/mutate')) {
  // Rule.mutate with no suite leaves the self-run report at the very path rule.report reads as the
  // Mutation tab's precondition, so grading and rendering on ONE engine is what lights that tab.
  var grade = rule.mutate(request.pathParams.name);
  if (grade && grade.error) {
    send(grade);
  } else {
    var graded = rule.report(request.pathParams.name);
    if (graded && graded.error) {
      send(graded);
    } else {
      send({
        url: '/api/artifacts/reports/' + encodeURIComponent(String(graded.file).split('/').pop())
            + '#analysis=mutation',
        file: graded.file,
        counts: grade.counts,
        denominator: grade.denominator,
        rawKillRate: grade.rawKillRate,
        independentScore: grade.independentScore,
        worklist: (grade.worklist || []).length
      });
    }
  }
} else if (request.get && request.pathMatches('/rulebooks/{name}')) {
  send(rule.load(request.pathParams.name, request.param('tables') ? { tables: request.param('tables') } : null));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
