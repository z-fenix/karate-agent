/*
 * /ui/session — the per-browser UI-prefs channel (D114, step 27). Console pages persist SMALL bits
 * of per-browser working state (split sizes, goal history, view toggles) into the cookie-keyed
 * markup `session` so they survive a reload but RESET on a server bounce — the right mental model
 * for working state (browser localStorage outlived restarts and confused; theme is the one app-wide
 * exception, kept in localStorage in layout.html). See the markup skill topics/app-setup.md
 * "Where per-browser UI state belongs".
 *
 * Lazy by design: the GET reads defensively (no session yet → {}); the session is only created on
 * the FIRST write, so a browser that never saves a pref never gets a cookie. All prefs live under
 * one `session.ui` map (keeps the session root free for a future auth/user identity).
 *
 * Routes (the /ui prefix is stripped):
 *   GET  /session              → { prefs: {...} }   the whole ui-prefs map (or {} before first write)
 *   POST /session  {key,value} → { ok: true }       upsert one pref; creates the session on first write
 */
function send(out) {
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

if (request.get && request.pathMatches('/session')) {
  send({ prefs: (session && session.ui) || {} });
} else if (request.post && request.pathMatches('/session')) {
  var body = request.bodyJson || {};
  if (!body.key) {
    response.status = 400;
    send({ error: 'key required', code: 'BAD_REQUEST', status: 400 });
  } else {
    if (!session) context.init();   // opt this browser in (sets the cookie) on the first write
    var ui = session.ui || {};      // reassign the whole map so the store persists the mutation
    ui[body.key] = body.value;
    session.ui = ui;
    send({ ok: true });
  }
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
