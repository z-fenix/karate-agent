/*
 * /ui/apitest — the API test page's data channel (D98: a fresh per-request engine). `apiTest` is the template
 * global (the ApiTest namespace); each route is one verb. Running a test stays on /api/eval (Runner.run, with
 * {session} to leave the flow warm).
 *
 * Routes (the /ui prefix is stripped):
 *   GET /apitest/read?test=<path>                          → ApiTest.read(path)
 *   GET /apitest/exchanges?test=<path>[&runId=][&scenario=] → ApiTest.exchanges(path, {runId, scenario});
 *                                                            404 NOT_FOUND while the run has no result yet
 *   GET /apitest/run?runId=<id>                            → {runId, status} of Harness.get(runId): a run that
 *                                                            ended with no result for the file still settles
 *   GET /apitest/session?test=<path>&scenario=<n>[&row=][&step=][&session=]
 *                                                          → ApiTest.session(path, {scenario, row, step, session})
 *   POST /apitest/send    {test, scenario, step, row?, session?}  → ApiTest.send(path, opts)
 *   POST /apitest/restart {test, scenario, step?, row?, session?} → ApiTest.restart(path, opts)
 *   POST /apitest/close   {test, scenario, row?, session?}        → ApiTest.close(path, opts)
 *   GET  /apitest/triage?test=<path>                       → ApiTest.triage(path) (the gate, api-workspace §5)
 *   POST /apitest/answer  {test, answer, ticket?, candidate, base} | {test, answer: 'discard', doc}
 *                                                          → ApiTest.answer(path, opts)
 *   POST /apitest/recheck {test, candidate}                → ApiTest.recheck(path, opts)
 *   GET  /apitest/change?test=<path>                       → ApiTest.change(path)
 *   POST /apitest/undo    {test, after}                    → ApiTest.undo(path, opts)
 *   POST /apitest/chat    {message, test?, chat?, sent?, from?, req?} → Harness.api(message, {test, chat, sent, from})
 *                                                            (the side chat, §5; sent: the page's latest Send, {call,
 *                                                            status, body}; from: a recording's run id — its calls panel's
 *                                                            moves and sites (/ui/traffic) and the criteria New test chose
 *                                                            (req) go with it; the chat is held as that recording's by run
 *                                                            id, and a repeated first message answers that chat
 *                                                            (Harness.chat, reused: true))
 *   GET  /apitest/chat?chat=<id>[&test=<path>]             → Harness.chat({chat, test}); 404 when the server holds none
 *   POST /apitest/link    {test, scenario, tags, ifMatch}  → ApiTest.link(path, opts) (the tester's ticked criteria)
 *   GET  /apitest/config                                   → ApiTest.config() (the generated block's entries)
 *   POST /apitest/config  {auth, env?, ifMatch} | {remove: 'auth', env?, ifMatch}
 *                                                          → ApiTest.config(opts): the auth preset form (api-workspace §6);
 *                                                            a credential's {value} goes to the secret store
 *   A refusal ({error, code, status}) answers with its status: NO_SESSION / STALE_SESSION / BUSY 409, CAPACITY 503.
 *   A failed replay is an answer (status 'failed' beside its cards), not a refusal: it carries no code.
 */
function send(out) {
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

function sessionOpts(src) {
  var opts = {};
  if (src.scenario != null && src.scenario !== '') opts.scenario = Number(src.scenario);
  if (src.row != null && src.row !== '') opts.row = Number(src.row);
  if (src.step != null && src.step !== '') opts.step = Number(src.step);
  if (src.session) opts.session = String(src.session);
  return opts;
}

if (request.get && request.pathMatches('/apitest/read')) {
  send(apiTest.read(request.param('test')));
} else if (request.get && request.pathMatches('/apitest/exchanges')) {
  var opts = {};
  if (request.param('runId')) opts.runId = request.param('runId');
  if (request.param('scenario') != null && request.param('scenario') !== '') opts.scenario = request.paramInt('scenario');
  send(apiTest.exchanges(request.param('test'), opts));
} else if (request.get && request.pathMatches('/apitest/run')) {
  var meta = harness.get(String(request.param('runId') || ''));
  send(meta && meta.status ? { runId: request.param('runId'), status: meta.status } : { error: 'no run ' + request.param('runId'), code: 'NOT_FOUND', status: 404 });
} else if (request.get && request.pathMatches('/apitest/session')) {
  send(apiTest.session(request.param('test'), sessionOpts({
    scenario: request.param('scenario'), row: request.param('row'), step: request.param('step'), session: request.param('session')
  })));
} else if (request.post && request.pathMatches('/apitest/send')) {
  var body = request.bodyJson || {};
  send(apiTest.send(body.test, sessionOpts(body)));
} else if (request.post && request.pathMatches('/apitest/restart')) {
  var body = request.bodyJson || {};
  send(apiTest.restart(body.test, sessionOpts(body)));
} else if (request.post && request.pathMatches('/apitest/close')) {
  var body = request.bodyJson || {};
  send(apiTest.close(body.test, sessionOpts(body)));
} else if (request.get && request.pathMatches('/apitest/triage')) {
  send(apiTest.triage(request.param('test')));
} else if (request.post && request.pathMatches('/apitest/answer')) {
  var body = request.bodyJson || {};
  var opts = { answer: body.answer };
  if (body.ticket) opts.ticket = String(body.ticket);
  if (body.doc) opts.doc = String(body.doc);
  if (body.candidate !== undefined) opts.candidate = body.candidate;
  if (body.base !== undefined) opts.base = body.base;
  send(apiTest.answer(body.test, opts));
} else if (request.post && request.pathMatches('/apitest/recheck')) {
  var body = request.bodyJson || {};
  send(apiTest.recheck(body.test, { candidate: body.candidate }));
} else if (request.get && request.pathMatches('/apitest/change')) {
  send(apiTest.change(request.param('test')));
} else if (request.post && request.pathMatches('/apitest/undo')) {
  var body = request.bodyJson || {};
  send(apiTest.undo(body.test, { after: body.after }));
} else if (request.post && request.pathMatches('/apitest/chat')) {
  var body = request.bodyJson || {};
  var opts = {};
  if (body.test) opts.test = String(body.test);
  if (body.chat) opts.chat = String(body.chat);
  if (body.sent && body.sent.call) {
    opts.sent = { call: String(body.sent.call), status: String(body.sent.status), body: body.sent.body == null ? null : String(body.sent.body) };
  }
  // a recording's calls panel: the calls this browser confirmed there (/ui/traffic) ride the first message, and the
  // chat it starts is that recording's, by run id (session.ui.traffic[<run>].chat): a repeated first message — a
  // reload, a second tab — gets that chat back instead of a second one
  var from = body.from ? String(body.from) : null;
  var picked = from && session && session.ui && session.ui.traffic ? session.ui.traffic[from] : null;
  var out = null;
  if (picked && picked.chat) {
    var held = harness.chat({ chat: String(picked.chat) });
    if (held && !held.error) { held.reused = true; out = held; }
  }
  if (from && !out) {
    var reqs = [];
    for (var ri = 0; body.req && ri < body.req.length; ri++) reqs.push(String(body.req[ri]));
    opts.from = { source: from, moves: picked && picked.moves ? picked.moves : [], hosts: picked && picked.hosts ? picked.hosts : [], reqs: reqs };
  }
  if (!out) out = harness.api(body.message, opts);
  if (from && out && out.chat && !out.error && !out.reused) {
    if (!session) context.init();
    var ui = session.ui || {};
    var traffic0 = ui.traffic || {};
    var mine = traffic0[from] || {};
    traffic0[from] = { moves: mine.moves || [], hosts: mine.hosts || [], chat: out.chat };
    ui.traffic = traffic0;
    // the recording's API test is a result of its own beside the UI test's (session.ui.manualRecording.results)
    var intent = ui.manualRecording && String(ui.manualRecording.runId) == from ? ui.manualRecording : { runId: from };
    var results = intent.results || {};
    results.api = { chat: out.chat, test: null, status: 'writing' };
    intent.results = results;
    ui.manualRecording = intent;
    session.ui = ui;
  }
  send(out);
} else if (request.get && request.pathMatches('/apitest/chat')) {
  var opts = {};
  if (request.param('chat')) opts.chat = String(request.param('chat'));
  if (request.param('test')) opts.test = String(request.param('test'));
  send(harness.chat(opts));
} else if (request.post && request.pathMatches('/apitest/link')) {
  var body = request.bodyJson || {};
  send(apiTest.link(body.test, { scenario: Number(body.scenario || 0), tags: body.tags, ifMatch: body.ifMatch }));
} else if (request.get && request.pathMatches('/apitest/config')) {
  send(apiTest.config());
} else if (request.post && request.pathMatches('/apitest/config')) {
  var body = request.bodyJson || {};
  var opts = body.remove === 'auth' ? { remove: 'auth' } : { auth: body.auth };
  if (body.env) opts.env = String(body.env);
  if (body.ifMatch) opts.ifMatch = String(body.ifMatch);
  send(apiTest.config(opts));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
