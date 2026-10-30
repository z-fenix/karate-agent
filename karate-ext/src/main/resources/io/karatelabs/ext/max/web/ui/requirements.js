/*
 * /ui/requirements — the Requirements tab's data channel (D98 migration off /api/eval). Fresh
 * per-request engine; `file`/`requirement`/`requirementsHomes` are the template globals. The
 * Explore ▶ kickoff stays a plain /runs?goal= redirect (no eval) and run ops stay on /api/eval.
 *
 * Every route works over the CONFIGURED requirements home(s) (D214d) — never a literal
 * `requirements/`, which showed an empty tab and forked a second tree for a project whose home is
 * elsewhere (.kiro/specs). The write is `requirement.write`, the verb the LLM calls (P23).
 *
 * Routes (the /ui prefix is stripped):
 *   GET  /requirements                       → [{name, home}] — name is home-relative
 *   GET  /requirements/read?name=&home=      → {content}
 *   POST /requirements/write   {name, content, home, ifMatch?, overwrite?, dryRun?} → {ok, file, mtime} (or the
 *                                              Requirement.write envelope); an existing file is refused (409
 *                                              EXISTS) unless ifMatch or overwrite:true; name is home-relative (the name
 *                                              requirement.list() gives reads too), file as list() names it;
 *                                              {dryRun:true} writes nothing and answers what the console reads
 *                                              from the content
 *   GET  /requirements/parse?name=&home=     → requirement.load('<home>/<name>') — the parsed cards, each
 *                                              with the server's criteria[] {id, text, stamp} and stamp
 *   POST /requirements/import?name=&home=[&overwrite=true]   raw markdown body → {ok, file, count, ids,
 *                                              mtime}; an existing file is refused (409 EXISTS) unless
 *                                              overwrite=true
 *   GET  /requirements/export[?run=<id>]     → the matrix as a CSV attachment (requirement.matrix, csv)
 *   POST /requirements/draft   {content, name?, home?, provider?, model?} → harness.draft(content, {provider,
 *                                              model, taken}): the AI's rewrite of the draft, written nowhere;
 *                                              taken = the ids the other requirements files hold
 */
function send(out) {
  // a domain failure carrying an explicit status mirrors onto the HTTP response — the checks/rulebooks contract
  if (out && out.code && out.status) {
    response.status = out.status;
  }
  response.body = out;
  response.headers['Content-Type'] = 'application/json';
}

var homes = requirementsHomes && requirementsHomes.length ? requirementsHomes : ['requirements'];

// the home a request names, defaulting to the first; an unconfigured value is refused rather than
// resolved — the param reaches the filesystem
function homeOf(want) {
  if (!want) {
    return homes[0];
  }
  for (var i = 0; i < homes.length; i++) {
    if (homes[i] === want) {
      return homes[i];
    }
  }
  return null;
}

// the name reaches the filesystem, concatenated onto the home or resolved by `requirement.write`, so it
// must be a relative markdown path and nothing else — a `..` segment walks out of the home even though
// the sandbox stops an escape from the project root
function badName(name) {
  if (!name || name.indexOf(':') >= 0 || name.indexOf('\\') >= 0) {
    return true;
  }
  var segments = String(name).split('/');
  for (var i = 0; i < segments.length; i++) {
    var segment = segments[i];
    if (!segment || segment === '.' || segment === '..') {
      return true;
    }
  }
  var last = segments[segments.length - 1];
  var dot = last.lastIndexOf('.');
  // `.md`, or no extension at all — `requirement.write` appends the one it writes
  return dot >= 0 && last.substring(dot) !== '.md';
}

function invalidName(want) {
  response.status = 400;
  send({ error: "invalid requirements file name '" + want + "' — expected a relative .md path inside a requirements home",
    code: 'INVALID_NAME', status: 400 });
}

function unknownHome(want) {
  var known = '';
  for (var i = 0; i < homes.length; i++) {
    known += (i ? ', ' : '') + homes[i];
  }
  send({ error: "unknown requirements home '" + want + "' — configured homes are " + known,
    code: 'NOT_FOUND', status: 404 });
}

if (request.get && request.pathMatches('/requirements')) {
  var list = [];
  for (var h = 0; h < homes.length; h++) {
    var home = homes[h];
    var mds = file.list(home + '/**/*.md');
    for (var i = 0; mds && i < mds.length; i++) {
      list.push({ name: mds[i].substring(home.length + 1), home: home });
    }
  }
  list.sort(function (a, b) { return a.name < b.name ? -1 : 1; });
  send(list);
} else if (request.get && request.pathMatches('/requirements/read')) {
  var readHome = homeOf(request.param('home'));
  if (!readHome) {
    unknownHome(request.param('home'));
  } else if (badName(request.param('name'))) {
    invalidName(request.param('name'));
  } else {
    var content = file.read(readHome + '/' + request.param('name'));
    send(content && content.error ? content : { content: content });
  }
} else if (request.post && request.pathMatches('/requirements/write')) {
  var body = request.bodyJson || {};
  if (badName(body.name)) {
    // `requirement.write` refuses an escape too; the channel takes only a relative .md path
    invalidName(body.name);
  } else {
    // `requirement.write` names its home explicitly and hands back the file it actually wrote
    var wopts = { home: body.home || homes[0] };
    if (body.ifMatch) wopts.ifMatch = String(body.ifMatch);
    if (body.dryRun === true) wopts.dryRun = true;
    // replacing a file takes the version it was read at, or an explicit {overwrite:true}: a save that holds
    // neither creates a file and never lands on one that exists
    else if (!body.ifMatch && body.overwrite !== true) wopts.overwrite = false;
    var result = requirement.write(body.name, body.content, wopts);
    send(result && result.error ? result : wopts.dryRun ? result : { ok: true, file: result.file, mtime: result.mtime });
  }
} else if (request.get && request.pathMatches('/requirements/parse')) {
  var parseHome = homeOf(request.param('home'));
  if (!parseHome) {
    unknownHome(request.param('home'));
  } else if (badName(request.param('name'))) {
    invalidName(request.param('name'));
  } else {
    // a root-relative reference, so a name inside a nested home resolves there and not by convention
    var parsed = parseHome + '/' + request.param('name');
    var loaded = requirement.load(parsed);
    if (!loaded.error) {
      // criterion ids and @req= tokens are the parser's (Requirement.list), joined by (file, id) — an id
      // another file also declares must not lend this card its criteria; never re-derived here
      var parsedFile = parsed.replace(/^\/+/, '');
      parsedFile = /\.md$/.test(parsedFile) ? parsedFile : parsedFile + '.md';
      var byId = {};
      var index = requirement.list();
      for (var li = 0; li < index.length; li++) {
        if (index[li].file === parsedFile) {
          byId[index[li].id] = index[li];
        }
      }
      for (var ri = 0; ri < loaded.requirements.length; ri++) {
        var card = loaded.requirements[ri];
        var indexed = byId[card.id] || {};
        card.criteria = indexed.criteria || [];
        if (indexed.stamp) {
          card.stamp = indexed.stamp;
        }
      }
    }
    send(loaded);
  }
} else if (request.post && request.pathMatches('/requirements/import')) {
  var importHome = homeOf(request.param('home'));
  var importName = request.param('name');
  var markdown = request.bodyString;
  if (!importHome) {
    unknownHome(request.param('home'));
  } else if (badName(importName)) {
    invalidName(importName);
  } else if (!markdown || !String(markdown).trim()) {
    send({ error: 'import requires the markdown file as the request body', code: 'BAD_REQUEST', status: 400 });
  } else {
    var overwrite = request.param('overwrite') === 'true';
    // a new file never takes an id another file declares: the id is what a test's @req= names
    var taken = [];
    var present = file.exists(importHome + '/' + (/\.md$/.test(importName) ? importName : importName + '.md'));
    if (!overwrite && !present) {
      var read = requirement.write(importName, markdown, { home: importHome, dryRun: true });
      var found = read && read.findings ? read.findings : [];
      for (var fi = 0; fi < found.length; fi++) {
        if (found[fi].rule === 'duplicate-id' && taken.indexOf(String(found[fi].id)) < 0) taken.push(String(found[fi].id));
      }
    }
    if (taken.length) {
      send({ error: 'another requirements file already declares ' + taken.join(', ') + ' — give these requirements new ids',
        code: 'DUPLICATE_ID', status: 409, ids: taken });
    } else {
      // the one write path (P23): parse-validated by the verb, which refuses an existing file
      var imported = requirement.write(importName, markdown, { home: importHome, overwrite: overwrite });
      send(imported.error ? imported : { ok: true, file: imported.file, count: imported.count,
        ids: imported.ids, mtime: imported.mtime });
    }
  }
} else if (request.post && request.pathMatches('/requirements/draft')) {
  var draftBody = request.bodyJson || {};
  // the file being drafted keeps its own ids; every other file's are taken
  var own = draftBody.name && !badName(draftBody.name)
    ? String(draftBody.home || homes[0]) + '/' + String(draftBody.name).replace(/\.md$/, '') + '.md' : null;
  var taken = [];
  var known = requirement.list();
  for (var ki = 0; known && ki < known.length; ki++) {
    if (known[ki].file !== own && taken.indexOf(String(known[ki].id)) < 0) taken.push(String(known[ki].id));
  }
  var draftOpts = { taken: taken };
  if (draftBody.provider) draftOpts.provider = String(draftBody.provider);
  if (draftBody.model) draftOpts.model = String(draftBody.model);
  send(harness.draft(draftBody.content, draftOpts));
} else if (request.get && request.pathMatches('/requirements/export')) {
  var run = request.param('run') || null;
  // a run id is one folder under runs/ — the param reaches the filesystem
  var csv = run && (run.indexOf('/') >= 0 || run.indexOf('\\') >= 0 || run === '..')
    ? { error: "invalid run id '" + run + "'", code: 'BAD_REQUEST', status: 400 }
    : requirement.matrix({ output: 'csv', run: run });
  if (csv.error) {
    send(csv);
  } else {
    // the BOM is what makes a spreadsheet read UTF-8; the verb's own text stays BOM-free for a machine reader
    response.body = '\uFEFF' + csv.csv;
    response.headers['Content-Type'] = 'text/csv; charset=utf-8';
    response.headers['Content-Disposition'] = 'attachment; filename="traceability-matrix.csv"';
  }
} else {
  response.status = 404;
  send({ error: 'no route: ' + request.method + ' /ui' + request.path, code: 'NO_ROUTE', status: 404 });
}
