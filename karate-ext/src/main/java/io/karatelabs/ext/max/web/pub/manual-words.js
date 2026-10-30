// Manual mode — the words its screens put on what the server answers, in one copy: a run's mark, a recorded step,
// a time, a name. The Manual templates evaluate this file server-side (<script ka:scope="global" src>) and the
// layout loads it in the browser for manual.js and manual-runs.js, so a run reads the same in the ledger, on its
// page, on Home and in its live log. It words what the server answered, never grades it (D358).
var kaWords = (function () {

  function plural(n, one, many) { return n + ' ' + (n == 1 ? one : many); }

  function firstLine(t, max) {
    var s = String(t == null ? '' : t).trim();
    var nl = s.indexOf('\n');
    if (nl >= 0) s = s.substring(0, nl).trim();
    return s.length > max ? s.substring(0, max - 1) + '…' : s;
  }

  function short(t, max) {
    var s = String(t == null ? '' : t).replace(/\s+/g, ' ').trim();
    return s.length > max ? s.substring(0, max - 1) + '…' : s;
  }

  // short, but never cut inside an embedded #(…): the cut moves back to before it
  function clip(t, max) {
    var s = String(t == null ? '' : t).replace(/\s+/g, ' ').trim();
    if (s.length <= max) return s;
    var cut = s.substring(0, max - 1), open = cut.lastIndexOf('#(');
    return (open >= 0 && cut.indexOf(')', open) < 0 ? cut.substring(0, open).replace(/\s+$/, '') : cut) + '…';
  }

  function quote(v) { var m = v.match(/^'(.*)'$/); return m ? '“' + m[1] + '”' : v; }
  // a match failure's first diff (karate's "match failed: <TYPE>" block): what came back and what was expected
  function matchDiff(lines) {
    for (var i = 0; i + 3 < lines.length; i++) {
      if (/^match failed/.test(lines[i].trim()) && /\|/.test(lines[i + 1])) {
        return { got: quote(lines[i + 2].trim()), want: quote(lines[i + 3].trim()) };
      }
    }
    return null;
  }

  // why a test failed, in one line: the check's own "Error:" line (a labeled check says its label there),
  // never the wrapper lines around it ("js failed:", "Check '…' failed:") nor a leading "path:line"
  function why(t, max) {
    var lines = String(t == null ? '' : t).split('\n');
    var head = /^\S*:\d+$/.test(lines[0].trim()) ? 1 : 0;
    var st = statusFailure(t);
    if (st) return 'Expected status ' + httpStatus(st.want) + ', got ' + httpStatus(st.got);
    var diff = matchDiff(lines);
    if (diff) return firstLine('Got ' + diff.got + ', expected ' + diff.want, max);
    for (var i = head; i < lines.length; i++) {
      var l = lines[i].trim();
      if (l.indexOf('Error:') == 0) l = l.substring(6).trim();
      else if (i > head) continue;
      l = l.replace(/^Check '[^']*' failed:\s*/, '');
      if (!l || l.indexOf('js failed:') >= 0) continue;
      var cut = l.indexOf(' — match failed');
      return firstLine(cut > 0 ? l.substring(0, cut) : l, max);
    }
    return firstLine(lines.slice(head).join('\n'), max);
  }

  // integer math: a number's text form follows the JVM locale in karate-js (a comma in some)
  function tenths(ms) { var t = Math.round(ms / 100); return Math.floor(t / 10) + '.' + (t % 10); }
  function pad2(n) { return n < 10 ? '0' + n : String(n); }

  function duration(ms) {
    if (ms == null || ms < 0) return null;
    if (ms < 10000) return tenths(ms) + ' s';
    var s = Math.round(ms / 1000);
    if (s < 60) return s + ' s';
    var m = Math.floor(s / 60);
    if (m < 60) return m + ' min ' + pad2(s % 60) + ' s';
    return Math.floor(m / 60) + ' h ' + pad2(m % 60) + ' min';
  }

  // a test's usual time, compact as its summary line says it: <0.1s, 2.1s, 75 min 00 s
  function compactDuration(ms) {
    if (ms == null) return null;
    var t = Math.round(ms / 100);
    if (t == 0) return '<0.1s';
    if (t < 600) return tenths(ms) + 's';
    var s = Math.round(ms / 1000);
    return Math.floor(s / 60) + ' min ' + pad2(s % 60) + ' s';
  }

  // how long ago, as the Runs ledger says it
  function age(ms) {
    if (!ms) return null;
    var s = Math.max(0, Math.floor((Date.now() - ms) / 1000));
    if (s < 60) return 'just now';
    var m = Math.floor(s / 60);
    if (m < 60) return m + ' min ago';
    var h = Math.floor(m / 60);
    if (h < 24) return h + ' h ago';
    var d = Math.floor(h / 24);
    return d == 1 ? 'yesterday' : d + ' days ago';
  }

  // how long ago, as the Tests screens say it
  function ageInWords(ms) {
    if (!ms || ms <= 0) return null;
    var s = Math.max(0, Math.round((Date.now() - ms) / 1000));
    if (s < 60) return 'just now';
    var m = Math.floor(s / 60);
    if (m < 60) return m + ' min ago';
    var h = Math.floor(m / 60);
    if (h < 24) return h + (h == 1 ? ' hour ago' : ' hours ago');
    var d = Math.floor(h / 24);
    if (d == 1) return 'yesterday';
    if (d < 365) return d + ' days ago';
    return 'over a year ago';
  }

  function fileOf(p) { p = String(p || ''); return p.substring(p.lastIndexOf('/') + 1); }

  // a criterion or requirement id from its stamp: no @, no req=
  function stampId(s) {
    var t = String(s == null ? '' : s).trim();
    if (t.indexOf('@') == 0) t = t.substring(1);
    if (t.indexOf('req=') == 0) t = t.substring(4);
    return t;
  }

  // a test's file name as typed: no checks/ or .js, any run of other characters one '-'
  function slug(s) {
    return String(s == null ? '' : s).trim().replace(/\.js$/, '').replace(/^\/*(checks\/)?/, '')
        .replace(/[^A-Za-z0-9_./-]+/g, '-').replace(/^[-.\/]+|[-.\/]+$/g, '');
  }

  // the stage strip a test being created walks through; `at` is the current stage (4 = all four done)
  function stageStrip(at) {
    var names = ['Set up', 'Record', 'Write', 'Verify'];
    var out = [];
    for (var i = 0; i < names.length; i++) {
      out.push({ n: i + 1, label: names[i], current: i == at ? 'step' : null,
                 cls: 'm-stage' + (i < at ? ' m-stage--done' : (i == at ? ' m-stage--now' : '')) });
    }
    return out;
  }

  // a requirement's or a criterion's mark, one per state, never merged: covered (green dot) · failing (red dot) ·
  // never tested and high risk (red ring) · never tested or not run (grey ring)
  function coverageMark(status, criticality) {
    if (status == 'COVERED') return 'm-dot--passed';
    if (status == 'FAILING') return 'm-dot--failed';
    if (status == 'NOTCOVERED' && criticality == 'high') return 'm-dot--critical';
    return 'm-dot--notrun';
  }

  // what an AI run did is its record's (`changed`, stamped at settle): it can declare an authoring run finished
  // without writing its file, or finish a change without touching it
  function wroteNothing(s) {
    return String(s.kind || '') == 'author' && String(s.status || '') == 'completed' && s.changed === false;
  }
  function unchanged(s) {
    return String(s.kind || '') == 'revise' && String(s.status || '') == 'completed' && s.changed === false;
  }

  // a run's state mark — one of five, never merged: passed (green dot) · failed (red dot) · ended before an answer
  // (grey dot — stopped, hit its limit, interrupted, blocked) · nothing tested (grey ring — ran nothing, parsed
  // only, a recording or a live session, an AI run that left its test as it was) · in flight (purple ring)
  function runMark(s, live) {
    var st = String(s.status || '');
    var kind = s.kind ? String(s.kind) : '';
    if (live) return { dot: 'm-dot--inflight', label: 'in flight', note: null };
    if (wroteNothing(s)) return { dot: 'm-dot--notrun', label: 'nothing written', note: 'the AI stopped without writing it' };
    if (unchanged(s)) return { dot: 'm-dot--notrun', label: 'nothing changed', note: 'the AI finished without changing it' };
    if (st == 'completed') {
      if (s.dryRun) return { dot: 'm-dot--notrun', label: 'not run', note: 'parsed only, nothing ran' };
      if (kind == 'live' && !s.check) return { dot: 'm-dot--notrun', label: 'not tested', note: null };
      return { dot: 'm-dot--passed', label: 'passed', note: null };
    }
    if (st == 'failed' || st == 'error') {
      return { dot: 'm-dot--failed', label: 'failed', note: s.errorShort ? firstLine(s.errorShort, 90) : null };
    }
    if (st == 'stopped') return { dot: 'm-dot--stopped', label: 'stopped', note: 'stopped before it finished' };
    if (st == 'terminated') return { dot: 'm-dot--stopped', label: 'stopped', note: 'hit its limit' };
    if (st == 'interrupted') return { dot: 'm-dot--stopped', label: 'stopped', note: 'interrupted — the console restarted' };
    if (st == 'blocked') return { dot: 'm-dot--stopped', label: 'stopped', note: 'blocked — it could not go on' };
    if (st == 'empty') return { dot: 'm-dot--notrun', label: 'not run', note: 'nothing ran' };
    return { dot: 'm-dot--notrun', label: 'not run', note: 'no record of how it ended' };
  }

  // an address as a person reads it: no scheme, query or fragment, and no lone trailing '/'
  function host(url) {
    var s = String(url == null ? '' : url).replace(/^[a-z][a-z0-9+.-]*:\/\//i, '').replace(/[?#].*$/, '');
    return s.indexOf('/') == s.length - 1 ? s.substring(0, s.length - 1) : s;
  }

  // a locator in the tester's words: `{button}Sign in` is the button "Sign in"; anything else stays as written
  function shown(loc) {
    var s = String(loc == null ? '' : loc).trim();
    var m = s.match(/^\{[^}]*\}\s*(.+)$/);
    return '“' + short(m ? m[1] : s, 60) + '”';
  }

  // one recorded step said as what the tester did — the element by the name a person reads on it (the recorder's
  // `name`), else its locator; what was typed is never said
  function step(d) {
    var a = String(d.action || 'action');
    var name = d.name ? String(d.name).trim() : '';
    var loc = name ? '“' + short(name, 60) + '”' : d.locator ? shown(d.locator) : 'the page';
    if (a == 'nav' || a == 'navigate' || a == 'goto') return 'Opened ' + short(host(d.url || d.value || ''), 90);
    if (a == 'type' || a == 'input' || a == 'fill') return 'Typed into ' + loc;
    if (a == 'select') return 'Chose ' + (d.value != null ? '“' + short(d.value, 40) + '” in ' : 'an option in ') + loc;
    if (a == 'check' || a == 'uncheck') return (a == 'check' ? 'Ticked ' : 'Unticked ') + loc;
    if (a == 'upload') return 'Uploaded a file to ' + loc;
    if (a == 'press' || a == 'key') return 'Pressed ' + short(d.value || d.key || 'a key', 30);
    if (a == 'hover') return 'Pointed at ' + loc;
    if (a == 'submit') return 'Submitted ' + (name || d.locator ? loc : 'a form');
    if (a == 'scroll') return 'Scrolled the page';
    return 'Clicked ' + loc;
  }

  var REASON = { 200: 'OK', 201: 'Created', 202: 'Accepted', 204: 'No Content', 301: 'Moved', 302: 'Found',
    304: 'Not Modified', 400: 'Bad Request', 401: 'Unauthorized', 402: 'Payment Required', 403: 'Forbidden',
    404: 'Not Found', 405: 'Method Not Allowed', 409: 'Conflict', 415: 'Unsupported Media Type',
    422: 'Unprocessable', 429: 'Too Many Requests', 500: 'Server Error', 502: 'Bad Gateway', 503: 'Unavailable',
    504: 'Gateway Timeout' };
  function httpStatus(code) { return code == null ? '' : REASON[code] ? code + ' ' + REASON[code] : String(code); }

  var OPERATOR = { '==': 'is', '!=': 'is not', 'contains': 'includes', '!contains': 'does not include',
    'contains only': 'includes only', 'contains any': 'includes any of', 'contains deep': 'includes',
    'contains only deep': 'includes only' };
  var MARKER = { '#string': 'any text', '#number': 'a number', '#boolean': 'true or false', '#array': 'a list',
    '#object': 'an object', '#notnull': 'present', '#present': 'present', '#null': 'empty', '#notpresent': 'absent',
    '#uuid': 'an id', '#ignore': 'anything', '#regex': 'a pattern' };
  // what `match`'s left side names, as a tester reads it: `response.total` is "total", `response` "the response"
  function field(actual) {
    var s = String(actual == null ? '' : actual).trim();
    if (s == 'response' || s == '$') return 'the response';
    var m = s.match(/^(?:response|\$)[.]?(.+)$/);
    return m ? m[1] : s;
  }
  // one Check card in words: "Status is 200 OK", "total is a number", "every item's id is present"
  function check(c) {
    if (c.status != null) return 'Status is ' + httpStatus(c.status);
    var p = checkParts(c);
    return p.owner + ' ' + (OPERATOR[p.op] || p.op) + ' ' + p.said;
  }
  function checkParts(c) {
    var expected = String(c.expected == null ? '' : c.expected).trim();
    var quoted = expected.match(/^'(.*)'$|^"(.*)"$/);
    var bare = quoted ? (quoted[1] != null ? quoted[1] : quoted[2]) : expected;
    var said = MARKER[bare] || MARKER[bare.split(/\s/)[0]] || (quoted ? '“' + bare + '”' : expected);
    var what = field(c.actual);
    var owner = what == 'the response' ? 'The response' : /^[A-Za-z_$][\w$.\[\]]*$/.test(what) && /^(?:response|\$)/.test(String(c.actual).trim())
        ? 'The response\'s ' + what : what;
    return { owner: c.each ? 'Every item of ' + owner.replace(/^The /, 'the ') : owner, op: c.operator, said: said };
  }

  // a failed step in words: a match failure says what came back and what was expected, anything else why() says
  function failure(t, max) { return why(t, max); }

  // a passing match or status step in words, as a Check card says it: "The response's status is “paid”" — null for
  // any other step
  function checkWords(stepText) {
    var s = String(stepText == null ? '' : stepText).trim().replace(/^(Given|When|Then|And|But|\*)\s+/, '');
    var st = s.match(/^status\s+(\d+)$/);
    if (st) return 'Status is ' + httpStatus(Number(st[1]));
    var m = s.match(/^match\s+(each\s+)?(.+?)\s+(contains only deep|contains only|contains any|contains deep|!contains|contains|==|!=)\s+(.+)$/);
    if (!m) return null;
    var line = check({ each: !!m[1], actual: m[2], operator: m[3], expected: m[4] });
    return line.charAt(0).toUpperCase() + line.substring(1);
  }

  // what a failed check expected and what came back, in a sentence: "Expected the response's status to be
  // “paid”, got “pending”." — null for a step that is not a match or status check
  var EXPECT = { '==': 'to be', '!=': 'not to be', 'contains': 'to include', '!contains': 'not to include',
    'contains only': 'to include only', 'contains any': 'to include any of', 'contains deep': 'to include',
    'contains only deep': 'to include only' };
  function expectation(stepText, actual) {
    var s = String(stepText == null ? '' : stepText).trim().replace(/^(Given|When|Then|And|But|\*)\s+/, '');
    var got = gotOf(actual);
    var st = s.match(/^status\s+(\d+)$/);
    if (st) return 'Expected status ' + httpStatus(Number(st[1])) + (got ? ', got ' + got : '') + '.';
    var m = s.match(/^match\s+(each\s+)?(.+?)\s+(contains only deep|contains only|contains any|contains deep|!contains|contains|==|!=)\s+(.+)$/);
    if (!m) return null;
    var p = checkParts({ each: !!m[1], actual: m[2], operator: m[3], expected: m[4] });
    return 'Expected ' + p.owner.replace(/^The /, 'the ') + ' ' + EXPECT[p.op] + ' ' + p.said + (got ? ', got ' + got : '') + '.';
  }
  // a `url` expression as a tester reads it: a literal as written, a variable as {name}, the leading variable
  // (the project's base address) dropped — `demoUrl + '/orders/' + orderId` is "/orders/{orderId}"
  function address(url) {
    var bits = String(url == null ? '' : url).trim().match(/'[^']*'|"[^"]*"|[^+'"]+/g) || [];
    var out = '', first = true;
    for (var i = 0; i < bits.length; i++) {
      var b = bits[i].trim();
      if (!b) continue;
      if (/^['"]/.test(b)) out += b.substring(1, b.length - 1);
      else if (!(first && /^[A-Za-z_$][\w$.]*$/.test(b))) out += '{' + b + '}';
      first = false;
    }
    return out;
  }

  // karate's status failure ("expected status: 200, actual: 401", or its HTTP log's "status code was: 401,
  // expected: 200") as the two codes
  function statusFailure(t) {
    var s = String(t == null ? '' : t), m = s.match(/expected status: (\d{3}), actual: (\d{3})/);
    if (m) return { want: Number(m[1]), got: Number(m[2]) };
    m = s.match(/status code was: (\d{3}), expected: (\d{3})/);
    return m ? { want: Number(m[2]), got: Number(m[1]) } : null;
  }

  // the value a failure says came back: the code of a status failure, the actual side of a match diff, else the text
  function gotOf(actual) {
    if (actual == null || actual === '') return null;
    var st = statusFailure(actual);
    if (st) return httpStatus(st.got);
    var diff = matchDiff(String(actual).split('\n'));
    if (diff) return diff.got;
    var s = short(actual, 120);
    return /^['"“\d\[{]/.test(s) || /^(true|false|null)$/.test(s) ? quote(s) : '“' + s + '”';
  }

  function isList(v) { return v != null && v.length != null && typeof v != 'string'; }

  // the top-level keys of an object literal's text: `{ Accept: 'json', 'X-Id': id }` is Accept, X-Id
  function topKeys(s) {
    var keys = [], depth = 0, q = null, expect = false;
    for (var i = 0; i < s.length; i++) {
      var ch = s.charAt(i);
      if (q) { if (ch == '\\') i++; else if (ch == q) q = null; continue; }
      if (expect) {
        expect = false;
        var m = s.substring(i).match(/^\s*(?:'([^']*)'|"([^"]*)"|([A-Za-z0-9_$-]+))\s*:/);
        if (m) { keys.push(m[1] != null ? m[1] : m[2] != null ? m[2] : m[3]); i += m[0].length - 1; continue; }
      }
      if (ch == '\'' || ch == '"') q = ch;
      else if (ch == '{' || ch == '[' || ch == '(') { depth++; expect = depth == 1 && ch == '{'; }
      else if (ch == '}' || ch == ']' || ch == ')') depth--;
      else if (ch == ',' && depth == 1) expect = true;
    }
    return keys;
  }
  // a Call card's headers or query (`headers`, `params`) as one plain line, the code it reads from folded under it:
  // `Authorization = 'Bearer ' + token` names Authorization, an object literal its keys, any other expression nothing
  function inputs(label, list) {
    if (!isList(list) || !list.length) return null;
    var names = [], worked = false;
    for (var i = 0; i < list.length; i++) {
      var s = String(list[i]).trim(), one = s.match(/^([A-Za-z0-9_-]+)\s*=/);
      var keys = one ? [one[1]] : /^\{[\s\S]*\}$/.test(s) ? topKeys(s) : [];
      if (keys.length) names = names.concat(keys); else worked = true;
    }
    return { text: label + ': ' + (names.length ? names.join(', ') + (worked ? ', and more set when it runs' : '') : 'set when it runs'),
      code: list.join('\n') };
  }
  // a Remember card in words: `token = response.token` remembers token, `json = response` the response as json
  function remembers(variable, expression) {
    var v = String(variable), f = String(expression == null ? '' : expression).trim().match(/^(?:response|\$)\.?([\w$.]*)$/);
    if (!f) return 'Remembers ' + v;
    if (!f[1]) return 'Remembers the response as ' + v;
    var last = f[1].split('.').pop().toLowerCase();
    return v.toLowerCase().slice(-last.length) == last ? 'Remembers ' + v : 'Remembers the response’s ' + f[1] + ' as ' + v;
  }
  // a variable's name as words: appUrl is "site address", orderId "order id", auth_token "auth token"
  var SPOKEN = { url: 'address', baseurl: 'site address', appurl: 'site address' };
  function spoken(name) {
    var n = String(name);
    if (SPOKEN[n.toLowerCase()]) return SPOKEN[n.toLowerCase()];
    return n.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2').replace(/[_$]+/g, ' ').trim()
        .split(/\s+/).map(function (w) { return /^[A-Z0-9]+$/.test(w) && w.length > 1 ? w : w.toLowerCase(); }).join(' ')
        .replace(/\burl$/, 'address');
  }
  // an advanced step in words, where the page can tell what it does; its code shows folded under it
  function stepWords(text) {
    var s = String(text == null ? '' : text).trim().replace(/^(Given|When|Then|And|But|\*)\s+/, ''), m;
    if (/^configure\s+headers\b/.test(s)) return 'Sends these headers with every call after it';
    if ((m = s.match(/^configure\s+(\w+)/))) return 'Sets ' + m[1] + ' for the calls after it';
    if ((m = s.match(/^(?:def\s+\w+\s*=\s*)?(callonce|call)\s+read\(\s*['"]([^'"]+)['"]/))) return 'Runs ' + fileOf(m[2]) + (m[1] == 'callonce' ? ' once, for every flow' : '');
    if ((m = s.match(/^def\s+(\w+)/))) return 'Remembers the ' + spoken(m[1]);
    if (/^print\b/.test(s)) return 'Writes a line to the run log';
    if (/^assert\b/.test(s)) return 'Checks a condition';
    return 'A step written as code';
  }

  function bodyText(body) {
    if (body == null) return null;
    var t = typeof body == 'string' ? body : JSON.stringify(body, null, 2);
    return t.length > 20000 ? t.substring(0, 20000) + '\n…' : t;
  }
  function headerRows(h) {
    var out = [];
    if (!h) return out;
    for (var name in h) {
      var v = h[name];
      out.push({ name: String(name), value: isList(v) ? v.join(', ') : String(v) });
    }
    return out;
  }
  // one http exchange (ApiTest.exchanges, ApiTest.send) as the API test page shows it
  function exchange(x) {
    var req = x.request || {}, res = x.response || {};
    return { method: String(x.method || ''), url: String(x.url || ''), status: httpStatus(x.status == null ? null : x.status),
      ms: x.ms != null ? compactDuration(x.ms) : null, mock: x.mock ? true : null,
      sentBody: bodyText(req.body), gotBody: bodyText(res.body), sentHeaders: headerRows(req.headers), gotHeaders: headerRows(res.headers),
      cut: res.truncated ? 'Showing the first part of a ' + Math.round(res.truncated / 1024) + ' KB response.' : null };
  }

  // the channels a flow opens that this console cannot (ApiTest.read `unsupported`), as a tester names them
  var PROTOCOL = { grpc: 'gRPC', kafka: 'Kafka', websocket: 'WebSocket', mcp: 'MCP' };
  // a reply's console routes as links named by their page (/manual/coverage → Coverage); a name the reply wrote
  // beside its route (“Coverage (/manual/coverage)”) becomes the link, so it is not said twice
  var PAGES = [[/^\/manual\/runs\/[^\/]+$/, 'Run report'], [/^\/manual\/runs$/, 'Runs'], [/^\/manual\/coverage$/, 'Coverage'],
    [/^\/manual\/requirements$/, 'Requirements'], [/^\/manual\/tests$/, 'Tests'], [/^\/manual\/tests\/api$/, 'API test'],
    [/^\/manual$/, 'Home']];
  function pageName(route) {
    var path = route.split('?')[0];
    for (var i = 0; i < PAGES.length; i++) if (PAGES[i][0].test(path)) return PAGES[i][1];
    return null;
  }
  // a recording's sift (Traffic.sift / migrate's `sift`) in the tester's words: the kept calls, each titled by the
  // action it followed, the left-out ones grouped by reason, and what the capture missed when it is incomplete
  var LEFT = { lost: ['call lost while recording (not checked)', 'calls lost while recording (not checked)'],
    'not-http': ['non-web call', 'non-web calls'], console: ['call to this console', 'calls to this console'],
    preflight: ['browser permission check', 'browser permission checks'],
    'other-site': ['call to another site', 'calls to other sites'],
    'static': ['page file (picture, style, script)', 'page files (pictures, styles, scripts)'],
    'page-load': ['page you opened', 'pages you opened'],
    stream: ['live stream (not supported)', 'live streams (not supported)'],
    upload: ['file upload (not supported)', 'file uploads (not supported)'],
    polling: ['repeated in the background', 'repeated in the background'],
    duplicate: ['repeat of the same call', 'repeats of the same call'],
    'no-response': ['never answered', 'never answered'], moved: ['left out by choice', 'left out by choice'] };
  var LOST = { queue: 'dropped while the recorder was busy', 'over-limit': 'over the recording’s limit', drain: 'still loading at Stop' };
  // a call's method as what it does, for a tester who does not read HTTP; the method stays the row's tooltip
  var VERB = { GET: 'Reads', HEAD: 'Reads', POST: 'Sends', PUT: 'Changes', PATCH: 'Changes', DELETE: 'Deletes' };
  function bareHost(h) { return String(h || '').replace(/^[a-z][a-z0-9+.-]*:\/\//i, '').replace(/[/:].*$/, ''); }
  function traffic(sift) {
    var s = sift || {}, titles = {}, app = bareHost(s.app);
    (s.actions || []).forEach(function (a) {
      var t = step({ action: a.kind, name: a.name, locator: a.locator, url: a.url, value: a.kind == 'select' ? a.value : null });
      // a form's name is its labels run together: not a title
      titles[a.seq] = a.kind == 'submit' ? 'after you submitted the form' : 'after you ' + t.charAt(0).toLowerCase() + t.substring(1);
    });
    function moved(m) {
      if (!m) return null;
      return m.by == 'tester' ? 'you asked for this' : 'the assistant’s choice' + (m.reason ? ': ' + m.reason : '');
    }
    function detail(r) {
      var d = r.detail;
      if (!d) return '';
      if (r.reason == 'lost') return LOST[d] || 'not recorded';
      if (d.indexOf('same company domain') == 0) return 'on the same domain as the app';
      return d == 'in-flight at Stop' ? 'still loading at Stop' : 'failed: ' + d;
    }
    // the call in the project's API description it matches (Traffic.sift's operation), when there is one
    function operation(o) {
      if (!o) return null;
      if (o.id) return 'matches ' + o.id;
      return o.ambiguous ? 'matches more than one call in the API description (' + (o.candidates || []).join(', ') + ')' : 'not in the API description';
    }
    function row(r) {
      // a path segment that differs on every run (an id) reads as "(id)"
      var where = r.path == null ? 'a call that was not recorded' : (r.host && bareHost(r.host) != app ? r.host : '')
        + String(r.path).replace(/\{\}/g, '(id)') + (r.query ? '?' + short(r.query, 60) : '');
      var after = r.after == 0 ? 'when the page opened' : titles[r.after] || '';
      var method = String(r.method || '').toUpperCase();
      return { id: r.id, ids: [r.id], method: r.method || '', verb: VERB[method] || method, where: where,
        status: r.status == null ? '' : httpStatus(r.status),
        after: after, title: after ? after.charAt(0).toUpperCase() + after.substring(1) : where,
        site: r.detail && String(r.detail).indexOf('same company domain') == 0 ? bareHost(r.host) : null,
        mine: r.moved && r.moved.by == 'tester' ? true : null, lost: r.reason == 'lost' ? true : null,
        op: operation(r.operation),
        note: moved(r.moved) || (r.consumedBy ? 'a later call uses what it returned' : r.label || detail(r)), times: 1 };
    }
    var groups = [], at = {};
    (s.left || []).forEach(function (r) {
      var code = r.reason || 'moved', g = at[code];
      if (!g) { g = at[code] = { code: code, n: 0, rows: [], names: [] }; groups.push(g); }
      g.n++;
      if (r.label && g.names.indexOf(r.label) < 0) g.names.push(r.label);
      var same = r.group ? g.rows.filter(function (x) { return x.group == r.group; })[0] : null;
      var x = row(r);
      if (same) {
        same.times++;
        same.ids.push(r.id);
        if (same.after != x.after) same.after = '';
        return;
      }
      x.group = r.group || null;
      g.rows.push(x);
    });
    var order = Object.keys((s.counts || {}).byReason || {});
    groups.sort(function (a, b) { return order.indexOf(a.code) - order.indexOf(b.code); });
    // a site on the app's own domain is the likeliest to be part of the app: its rows, and their group, come first
    groups.forEach(function (g) {
      g.site = g.rows.some(function (x) { return x.site; }) ? true : null;
      g.rows.sort(function (a, b) { return (b.site ? 1 : 0) - (a.site ? 1 : 0); });
    });
    groups.sort(function (a, b) { return (b.site ? 1 : 0) - (a.site ? 1 : 0); });
    groups.forEach(function (g) {
      var w = LEFT[g.code];
      g.label = w ? plural(g.n, w[0], w[1]) : g.n + ' ' + ((s.reasons || {})[g.code] || g.code);
      if (g.names.length) g.label += ' (' + g.names.slice(0, 3).join(', ') + (g.names.length > 3 ? ' and ' + (g.names.length - 3) + ' more' : '') + ')';
    });
    var kept = (s.kept || []).map(row);
    return { kept: kept, left: (s.left || []).length, groups: groups, gaps: captureGaps(s.capture),
      keptLine: 'Kept ' + plural(kept.length, 'call', 'calls'), leftLine: 'Left out ' + (s.left || []).length };
  }
  // a proposed check the links card left out ({text, after}: Api.proposeLinks' skipped), said as the outcome
  function notChecked(s) {
    var text = s && s.text != null ? s.text : s;
    var said = checkWords(text);
    var after = (s && s.after ? [].concat(s.after) : []).map(function (a) { return String(a).charAt(0).toLowerCase() + String(a).substring(1); });
    var what = said ? said.charAt(0).toLowerCase() + said.substring(1) : stepWords(text).toLowerCase();
    return 'Not checked, as no criterion names it: ' + (after.length ? after.join(' and ') + ', ' : '') + what + '.';
  }
  // a karate-config change the chat holds (ApiToolkit's config card), said by what it holds rather than its name
  function setting(c) {
    var what = c.removed ? 'Removes the setting ' + c.name + ', which every test reads'
      : c.name == 'auth' ? 'Saves how every test signs in'
      : c.address ? 'Saves the address ' + c.address + ' as a setting every test reads'
      : c.secret ? 'Saves a secret every test reads, kept in the secret store'
      : 'Changes the setting ' + c.name + ', which every test reads';
    return what + '. It is saved only if the test passes.';
  }
  // the side chat's working line: the weightiest allowlisted call in a block the assistant runs (the later one on a
  // tie), in fixed words; the block's comments and strings are dropped first, so no text of its own (a comment, a
  // host, an id) can reach the line
  var WRITES = ['createFeature', 'createLoginFeature', 'createScenario', 'insertStep', 'replaceStep', 'deleteStep',
    'insertComment', 'replaceComment', 'deleteComment', 'insertRow', 'replaceRow', 'deleteRow', 'land'];
  var STEPS = { 'Traffic.read': [1, 'Reading your recording'], 'Traffic.sift': [2, 'Sorting the calls'],
    'Traffic.migrate': [5, 'Writing the test'], 'Postman.migrate': [5, 'Writing the test'], 'ApiTest.config': [3, 'Changing a setting'],
    'ApiTest.run': [6, 'Running the test'], 'ApiTest.exchanges': [2, 'Checking what the site answered'],
    'ApiTest.read': [1, 'Reading the test'], 'ApiTest.diff': [2, 'Reading the test'], 'ApiTest.markBug': [4, 'Noting the bug'],
    'Api.proposeLinks': [4, 'Linking requirements'], 'Http': [2, 'Checking what the site answered'] };
  WRITES.forEach(function (v) { STEPS['ApiTest.' + v] = [4, 'Writing the test']; });
  function chatStep(js) {
    var code = String(js == null ? '' : js).replace(/\/\/[^\n]*|\/\*[\s\S]*?\*\/|'(?:\\.|[^'\\\n])*'|"(?:\\.|[^"\\\n])*"|`(?:\\.|[^`\\])*`/g,
        function (t) { return t.charAt(0) == '/' ? ' ' : '0'; });
    var re = /\b(Traffic|Postman|ApiTest|Http|Api)\s*\.\s*([A-Za-z]+)\s*\(\s*(\))?/g, m, best = null;
    while ((m = re.exec(code)) !== null) {
      var s = m[1] == 'Http' ? STEPS.Http : m[1] + '.' + m[2] == 'ApiTest.config' && m[3] ? [1, 'Reading the settings'] : STEPS[m[1] + '.' + m[2]];
      if (s && (!best || s[0] >= best[0])) best = s;
    }
    return best ? best[1] : 'Working…';
  }
  function captureGaps(c) {
    if (!c || c.complete) return [];
    var out = [], lost = c.lost || {}, gone = (lost.queue || 0) + (lost.overLimit || 0) + (lost.drain || 0) + (lost.mask || 0);
    var un = c.uncaptured || {}, dropped = (c.dropped || {}).other || 0, partial = c.headersPartial || 0;
    if (c.inFlightAtStop) out.push(plural(c.inFlightAtStop, 'call was', 'calls were') + ' still loading at Stop — not checked');
    if (gone) out.push(plural(gone, 'call was', 'calls were') + ' not recorded — not checked');
    if (dropped + partial) out.push(plural(dropped + partial, 'call is', 'calls are') + ' missing part of what was sent or came back');
    if ((c.bodies || {}).unavailable) out.push(plural(c.bodies.unavailable, 'answer', 'answers') + ' could not be read');
    if ((un.tabs || []).length) out.push('Calls in another tab were not recorded: ' + un.tabs.map(host).join(', '));
    if ((un.frames || []).length) out.push('Calls inside an embedded page were not recorded: ' + un.frames.map(host).join(', '));
    if ((un.serviceWorkers || []).length) out.push('Calls the site made in the background were not recorded: ' + un.serviceWorkers.map(host).join(', '));
    if (c.ended && c.ended != 'stop') out.push('The recording ended before you pressed Stop');
    if ((c.errors || []).length) out.push('Stop did not finish cleanly, so the last steps may be missing');
    if (c.attribution == 'uncertain') out.push('The computer’s clock changed while recording, so the click each call followed may be wrong');
    return out;
  }

  function pageLinks(text) {
    var s = String(text == null ? '' : text), out = [], m;
    while ((m = s.match(/\/manual(\/[A-Za-z0-9._~%\/?=&-]*)?/)) != null) {
      var at = m.index, route = m[0].replace(/[.,;:!?\/]+$/, '') || '/manual';
      var name = at == 0 || /[\s(“"']/.test(s.charAt(at - 1)) ? pageName(route) : null;
      if (!name) {
        out.push({ text: s.substring(0, at + m[0].length) });
        s = s.substring(at + m[0].length);
        continue;
      }
      var before = s.substring(0, at), after = s.substring(at + route.length), label = name;
      var said = before.match(new RegExp('(^|[^A-Za-z])(' + name + ')( page)?( at)?\\s*(\\()?\\s*$', 'i'));
      // a label the reply put before the route ("report: /manual/runs/…") is the link's: the page's name replaces it
      var tag = said ? null : before.match(/(^|[^A-Za-z])([A-Za-z]+(?: [A-Za-z]+)?):\s*$/);
      if (said) {
        label = said[2];
        before = before.substring(0, said.index + said[1].length);
        if (said[5] && after.charAt(0) == ')') after = after.substring(1);
      } else if (tag && (' ' + name + ' ').toLowerCase().indexOf(' ' + tag[2].toLowerCase() + ' ') >= 0) {
        before = before.substring(0, tag.index + tag[1].length);
      }
      if (before) out.push({ text: before });
      out.push({ href: route, label: label });
      s = after;
    }
    if (s) out.push({ text: s });
    return out;
  }

  function cannotRun(types) {
    if (!isList(types) || !types.length) return null;
    var names = [];
    for (var i = 0; i < types.length; i++) names.push(PROTOCOL[types[i]] || String(types[i]));
    return 'This flow uses ' + names.join(' and ') + ', which this console can’t run.';
  }

  // the live session (ApiTest.session) in one sentence and the one action it allows: Try this call again on a
  // warm session, Start over otherwise. `calls` are the page's calls [{key, n, title, step, shared?}] (a shared one
  // runs in the Background); `opts` {outline, row, computed} is the flow's Outline and the row shown.
  var STALE = [[/env/, 'the environment was switched'], [/karate-config/, 'the project settings changed'],
    [/Background/, 'the steps that run before each flow changed'], [/Examples/, 'the data table changed'],
    [/cursor/, 'a call above the next one changed'], [/parse|cannot be read/, 'the test can no longer be read'],
    [/gone/, 'this flow was removed']];
  function liveSession(st, calls, opts) {
    st = st || {};
    calls = isList(calls) ? calls : [];
    opts = opts && typeof opts == 'object' ? opts : { outline: !!opts };
    var outline = !!opts.outline;
    var next = null, stopped = null, byKey = {};
    for (var i = 0; i < calls.length; i++) {
      byKey[calls[i].key] = calls[i];
      if (calls[i].shared) continue;
      if (!next && st.cursor != null && calls[i].step >= st.cursor) next = calls[i];
      if (st.stoppedAt != null && calls[i].step <= st.stoppedAt) stopped = calls[i];
    }
    var replays = isList(st.replays) ? st.replays : [];
    var sends = [];
    for (var r = 0; r < replays.length; r++) {
      var c = byKey[(replays[r].background ? 'b' : 's') + replays[r].step];
      sends.push((c ? c.n + ' ' : '') + String(replays[r].title));
    }
    var out = { state: String(st.state || 'none'), dot: 'notrun', text: null, start: true, warm: null, next: null,
      sends: sends.length ? 'Start over sends: ' + sends.join(', ') + '.' : null };
    if (opts.computed) {
      out.text = 'This flow’s data rows are worked out when it runs, so its calls can’t be tried one at a time.';
      out.start = false; out.sends = null;
    } else if (out.state == 'warm' && st.stoppedAt != null) {
      // a Start over that stopped at a call that did not pass (or a check after it): the session stays warm
      out.dot = 'failed'; out.warm = true;
      out.text = 'Started over, but ' + (stopped ? 'call ' + stopped.n + ' ' + stopped.title : 'a call') + ' did not pass.'
          + ' Values up to it are kept; try any call again on its own.';
    } else if (out.state == 'warm') {
      out.dot = 'passed'; out.start = false; out.sends = null; out.warm = true;
      out.next = next ? next.key : null;
      out.text = next ? 'Ready — next call: ' + next.n + ' ' + next.title + '. Try any call again on its own.'
          : 'Ready — the run’s values are kept. Try any call again on its own.';
    } else if (out.state == 'busy') {
      out.dot = 'inflight'; out.start = false; out.sends = null;
      out.text = 'Busy — a call is being sent. Try again in a moment.';
    } else if (out.state == 'stale') {
      var reason = String(st.reason || ''), said = 'the test changed';
      for (var k = 0; k < STALE.length; k++) { if (STALE[k][0].test(reason)) { said = STALE[k][1]; break; } }
      out.dot = 'stopped';
      out.text = 'Out of date — ' + said + ' since the last run. Start over to try calls again.';
    } else if (st.code == 'MULTI_SCENARIO') {
      out.text = outline ? 'This flow ran once per data row. Start over to try its calls on '
          + (opts.row != null ? 'row ' + opts.row + '.' : 'one row.')
          : 'Run only this flow to try calls one at a time.';
      out.start = !!outline;
      if (!outline) out.sends = null;
    } else if (st.code == 'NOT_HTTP') {
      out.text = 'This flow drives a browser or a live channel, so its calls can’t be tried one at a time.';
      out.start = false; out.sends = null;
    } else if (st.code == 'CAPACITY') {
      out.text = 'The console is keeping as many flows ready as it can. Start over in a few minutes.';
    } else {
      out.text = 'Nothing has run yet. Run the test, or press Start over, to try calls one at a time.';
    }
    return out;
  }
  // a refused Send or Start over (its `code`), in words
  function sessionRefusal(code, error) {
    if (code == 'STALE_SESSION') return 'This flow changed since it ran. Start over to try calls again.';
    if (code == 'NO_SESSION') return 'Nothing is ready to try yet. Run the test, or start over.';
    if (code == 'BUSY') return 'Another call is being sent. Try again in a moment.';
    if (code == 'CAPACITY') return 'The console is keeping as many flows ready as it can. Try again in a few minutes.';
    if (code == 'NOT_HTTP') return 'This flow drives a browser or a live channel, so its calls can’t be tried one at a time.';
    if (code == 'DYNAMIC_EXAMPLES') return 'This flow’s data rows are worked out when it runs, so its calls can’t be tried one at a time.';
    return String(error || 'That did not work.');
  }

  // the tests that declare a link to a stamp id (Runner.tests() rows' `req`), a criterion also through its whole
  // requirement: what a never-run linked test is, before any run puts it on the graph
  function linkedTests(tests) {
    var by = {};
    for (var i = 0; tests && i < tests.length; i++) {
      var ids = tests[i].req || [];
      for (var j = 0; j < ids.length; j++) {
        var id = String(ids[j]);
        (by[id] = by[id] || []).push(String(tests[i].path));
      }
    }
    return function (id) {
      id = String(id);
      return by[id] || by[id.split('/')[0]] || [];
    };
  }

  // a Postman request that was not sent, in one line (Postman.results' error and transport); the exception text is
  // a developer's, shown apart
  function pmUnsent(error, transport) {
    var t = transport || {}, h = t.host ? String(t.host) : 'the server';
    if (t.kind == 'unreachable') return 'Couldn’t reach ' + h + ' — nothing answered.';
    if (t.kind == 'timeout') return h + ' didn’t answer in time.';
    if (t.kind == 'dns') return 'Couldn’t find ' + h + ' — no server has that name.';
    if (t.kind == 'tls') return 'Couldn’t make a secure connection to ' + h + '.';
    var s = String(error == null ? '' : error), unset = s.match(/\{\{([^}]+)\}\}/);
    if (unset) return 'It could not be sent: {{' + unset[1] + '}} has no value. An earlier request in this row usually sets it, so check that one first.';
    return 'It could not be sent: ' + failure(s.replace(/^([a-z]+\.)+[A-Za-z]*(Exception|Error):\s*/, ''), 200);
  }

  // what to do about an address nothing answered at: where its {{variable}} gets its value (Postman.env's `from`)
  function pmReach(transport, from) {
    var t = transport || {}, v = t.variable ? String(t.variable) : null;
    var start = t.kind == 'unreachable' || t.kind == 'timeout' ? 'Check that the app at ' + (t.host || 'that address') + ' is running. ' : '';
    if (!v) return start + 'The address is written in the request itself; change it under Address.';
    if (from == 'config') return start + v + ' comes from this project’s settings (karate-config); change ' + v + ' there to point it elsewhere.';
    if (from == 'collection') return start + v + ' comes from the collection; set ' + v + ' in this project’s settings (karate-config) to point it elsewhere.';
    if (from == 'data') return start + v + ' comes from the data file; change it there to point it elsewhere.';
    return start + v + ' has no value; set ' + v + ' in this project’s settings (karate-config).';
  }

  // how the calls sign in (Runner.auth's scheme, identity, login), in words: never a file or a scheme's name
  var SIGNS_IN = { bearer: 'Signs in with a token', basic: 'Signs in with a username and password', apikey: 'Signs in with an API key',
    oauth2: 'Signs in as an app' };
  function signIn(a) {
    if (!a || a.error || !SIGNS_IN[a.scheme]) return 'No sign-in';
    if (a.login) return 'Signs in through a sign-in test';
    if (a.scheme == 'oauth2' && a.identity) return 'Signs in as the app ' + a.identity;
    return SIGNS_IN[a.scheme] + (a.identity ? ', as ' + a.identity : '');
  }

  // the auth preset form (api-workspace §6): env's preset as ApiTest.config's block holds it (its own entry, else the
  // every-env one), each credential as the secret it is kept under and whether a value is there (Secret.list) — never
  // a value. `chip` is signIn's words, for a sign-in the project's settings set by hand.
  function authPreset(config, secrets, env, files, chip) {
    var entries = config && !config.error && config.entries && config.entries.length != null ? config.entries : [];
    var own = null, all = null;
    for (var i = 0; i < entries.length; i++) {
      var e = entries[i];
      if (e.name != 'auth' || !e.auth) continue;
      if (env && e.env == env) own = e.auth; else if (!e.env) all = e.auth;
    }
    var a = own || all || {}, held = {};
    for (var j = 0; secrets && secrets.length != null && j < secrets.length; j++) held[secrets[j].name] = secrets[j].stored || secrets[j].env;
    var refs = {}, missing = [];
    ['token', 'password', 'key', 'clientSecret'].forEach(function (f) {
      var v = a[f];
      refs[f] = v && v.secret ? { name: String(v.secret), stored: held[v.secret] ? true : null } : null;
      if (refs[f] && !refs[f].stored) missing.push(refs[f].name);
    });
    var KINDS = [{ id: 'none', label: 'No sign-in' }, { id: 'bearer', label: 'Bearer token' },
      { id: 'login', label: 'Bearer token from a sign-in test' }, { id: 'basic', label: 'Username and password' },
      { id: 'apikey', label: 'API key' }, { id: 'oauth2', label: 'OAuth 2 client credentials' }];
    var kind = a.scheme == 'bearer' && a.login ? 'login' : a.scheme ? String(a.scheme) : 'none', label = kind;
    for (var k = 0; k < KINDS.length; k++) { if (KINDS[k].id == kind) label = KINDS[k].label; }
    var logins = [];
    for (var f2 = 0; files && files.length != null && f2 < files.length; f2++) {
      if (/(^|\/)[^/]*(auth|login|sign-?in)[^/]*(\/|$)/i.test(String(files[f2]))) logins.push(String(files[f2]));
    }
    var hand = kind == 'none' && chip && chip != 'No sign-in' ? chip : null;
    return {
      env: env || null, version: config && config.version ? String(config.version) : null,
      why: config && config.error ? 'The sign-in can’t be changed here: ' + String(config.error).replace(/ — .*$/, '') + '.' : null,
      kind: kind, kinds: KINDS, from: own ? 'env' : all ? 'all' : null,
      summary: hand ? hand + ', set up by your team' : label + (all && !own && env ? ', set for every environment' : ''),
      username: a.username || '', name: a.query || a.header || '', where: a.query ? 'query' : 'header',
      tokenUrl: a.tokenUrl || '', clientId: a.clientId || '', scope: a.scope || '', login: a.login || '', logins: logins,
      refs: refs, missing: missing.length ? 'No value is stored for ' + missing.join(', ') + '. Type it and save.' : null
    };
  }

  return {
    authPreset: authPreset, signIn: signIn,
    pmUnsent: pmUnsent, pmReach: pmReach,
    httpStatus: httpStatus, check: check, checkWords: checkWords, inputs: inputs, remembers: remembers, stepWords: stepWords, failure: failure, expectation: expectation, address: address, linkedTests: linkedTests,
    plural: plural, firstLine: firstLine, short: short, clip: clip, why: why, duration: duration,
    compactDuration: compactDuration, age: age,
    ageInWords: ageInWords, fileOf: fileOf, stampId: stampId, slug: slug, stageStrip: stageStrip,
    coverageMark: coverageMark, wroteNothing: wroteNothing, unchanged: unchanged, runMark: runMark,
    host: host, shown: shown, step: step, exchange: exchange, liveSession: liveSession, sessionRefusal: sessionRefusal,
    cannotRun: cannotRun, pageLinks: pageLinks, traffic: traffic, notChecked: notChecked, setting: setting, chatStep: chatStep
  };
})();
