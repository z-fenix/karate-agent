/*
 * /ui/apidocs — the API docs page's data channel (D192d, openapi.md §The API docs surface). Fresh
 * per-request engine; `file` + `openapi` are the template globals — spec parsing is a filesystem
 * read, never an /api/eval. Routes (the /ui prefix is stripped):
 *   GET /apidocs/specs           → [{path}] — candidate API spec files (sniffed for openapi/swagger)
 *   GET /apidocs/model?spec=<p>  → Openapi.docs(spec) — the page-shaped documentation model; for
 *                                  rulebooks/<name>/openapi.yaml, Openapi.docs(spec, {twin: <name>})
 */
function send(out) {
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

// candidate spec discovery: shallow well-known locations, content-sniffed — a project's spec is
// conventionally at the root or under api/ (the scaffold writes it at the root, paths.md anchors)
function findSpecs() {
  var globs = ['*.yaml', '*.yml', '*.json',
    'api/*.yaml', 'api/*.yml', 'api/*.json',
    'spec/*.yaml', 'spec/*.yml', 'specs/*.yaml', 'specs/*.yml', 'openapi/*.yaml', 'openapi/*.yml',
    'rulebooks/*/openapi.yaml'];
  var seen = {};
  var out = [];
  for (var g = 0; g < globs.length; g++) {
    var files = file.list(globs[g]);
    for (var i = 0; i < files.length; i++) {
      var p = files[i];
      if (seen[p]) continue;
      seen[p] = true;
      // obvious non-specs — never worth a read
      if (p === 'package.json' || p === 'karate-pom.json' || p === 'karate-agent.json') continue;
      var text = file.read(p);
      if (typeof text !== 'string') continue;
      var head = text.substring(0, 2000);
      if (head.indexOf('openapi') >= 0 || head.indexOf('swagger') >= 0) {
        out.push({ path: p });
      }
    }
  }
  return out;
}

if (request.get && request.pathMatches('/apidocs/specs')) {
  send(findSpecs());
} else if (request.get && request.pathMatches('/apidocs/model')) {
  // the twin joins by location only (playground.md §7), never by the spec's info text
  var spec = request.param('spec');
  var book = /^rulebooks\/([^\/]+)\/openapi\.yaml$/.exec(spec || '');
  send(book ? openapi.docs(spec, { twin: book[1] }) : openapi.docs(spec));
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
