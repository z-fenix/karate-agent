/*
 * help-affordance.js (D124) — context-sensitive Help from an OFFLINE (file://) report.
 *
 * The shared behaviour behind every report's "?" affordance. A report can't reliably link to the
 * served console (different origin, or no server at all when opened off disk), so a "?" copies its
 * stable help-id slug to the clipboard; the user pastes it into the console Help tab's lookup box to
 * open /help#<id>. This is the ONE implementation reused across the static report surfaces
 * (coverage-report, traceability-report, rule-analysis-report, …) — add it once per page with
 * `<script src="res/help-affordance.js"></script>` (path relative to that page) and drop in a
 * `<button class="k-help-q" onclick="kaCopyHelp('rule.execute')">?</button>` wherever a concept needs help.
 *
 * Self-contained: it injects its own styles + toast, so it does NOT depend on which stylesheet a
 * given report loads. Theme-agnostic colours (work in the report's light/dark via data-theme).
 */
(function () {
  if (window.kaCopyHelp) {
    return; // idempotent — safe if two report bundles include it
  }

  var STYLE =
    '.k-help-q{display:inline-flex;align-items:center;justify-content:center;width:16px;height:16px;' +
    'border-radius:50%;font-size:11px;font-weight:700;line-height:1;cursor:pointer;padding:0;' +
    'background:#e0e7ff;color:#3730a3;border:1px solid transparent;vertical-align:middle;margin-left:6px}' +
    '.k-help-q:hover{border-color:#6366f1}' +
    '[data-theme="dark"] .k-help-q{background:#312e81;color:#c7d2fe}' +
    '.k-help-toast{position:fixed;bottom:18px;left:50%;transform:translateX(-50%);background:#1e293b;' +
    'color:#f1f5f9;font-size:13px;padding:8px 14px;border-radius:8px;box-shadow:0 4px 16px rgba(0,0,0,.25);' +
    'z-index:9999;max-width:90vw}';

  function ensureStyle() {
    if (document.getElementById('k-help-affordance-style')) {
      return;
    }
    var s = document.createElement('style');
    s.id = 'k-help-affordance-style';
    s.textContent = STYLE;
    document.head.appendChild(s);
  }

  function toast(msg) {
    var t = document.getElementById('k-help-toast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'k-help-toast';
      t.className = 'k-help-toast';
      document.body.appendChild(t);
    }
    t.textContent = msg;
    t.hidden = false;
    clearTimeout(t._t);
    t._t = setTimeout(function () { t.hidden = true; }, 3500);
  }

  function fallback(id, done) {
    try {
      var ta = document.createElement('textarea');
      ta.value = id;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.focus();
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      done();
    } catch (e) {
      window.prompt('Copy this help-id and paste it into the console Help tab:', id);
    }
  }

  window.kaCopyHelp = function (id) {
    var done = function () { toast('Copied help-id "' + id + '" — paste it into the Help tab, or into your AI chat (it can look it up).'); };
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(id).then(done, function () { fallback(id, done); });
      } else {
        fallback(id, done);
      }
    } catch (e) {
      fallback(id, done);
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', ensureStyle);
  } else {
    ensureStyle();
  }
})();
