// kaPrefs — the client half of the per-browser UI-prefs channel (D114, step 27). WRITES go to
// /ui/session (POST {key,value}); READS are SSR-hydrated — the page embeds session.ui server-side
// (a ka:island, #prefs-data) so it comes back pre-arranged with no GET round-trip and no flash.
// Per-key coalescing: a drag that fires many updates persists once it settles. Theme is NOT here —
// it stays in localStorage (the one app-wide exception), resolved in layout.html. See the markup
// skill topics/app-setup.md "Where per-browser UI state belongs".
window.kaPrefs = (function () {
  var timers = {};
  function set(key, value, delay) {
    clearTimeout(timers[key]);
    timers[key] = setTimeout(function () {
      fetch('/ui/session', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key: key, value: value }),
        keepalive: true
      }).catch(function () {});   // best-effort — a lost UI-pref write is never worth a console error
    }, delay == null ? 250 : delay);
  }
  // SSR-hydrated read: the value the server embedded for `key` (or `fallback` before any write).
  function read(key, fallback) {
    var el = document.getElementById('prefs-data');
    if (!el) return fallback;
    try {
      var prefs = JSON.parse(el.textContent) || {};
      return prefs[key] === undefined ? fallback : prefs[key];
    } catch (e) { return fallback; }
  }
  return { set: set, read: read };
})();
