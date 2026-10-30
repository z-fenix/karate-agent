/*
 * PaneSearch — the shared "pane search" helper (D113-a). A pane (Inputs, Lookups, Audit on the
 * rulebook page; the RTM/report panes later) keeps its own tiny search state — {open, q, matches,
 * idx} — and the page owns the model walk + the reveal (expand-then-scroll-flash), because only
 * the page knows how its rows nest. This file is deliberately just the MATCHER so load order with
 * the bottom <script src> never bites (the same stance as JsonTable/RunAll — static helpers, no
 * instance handed into the Alpine data object at construction time).
 *
 * Contract (the page wires these):
 *   state    : { open:false, q:'', matches:[], idx:0 }   (a plain literal on the Alpine object)
 *   match    : PaneSearch.match(haystack, query) → boolean   (case-insensitive substring)
 *
 * Highlight-don't-hide: editable panes (Inputs/Lookups grids) must never filter rows out — a
 * hidden-but-edited row invites blind edits. The page highlights matched rows in place and
 * auto-expands whatever (collapsed input groups · folded lookup keys · audit ancestors) hides the
 * current match, then scrolls + flashes it.
 */
(function () {
  function norm(s) { return (s == null ? '' : String(s)).toLowerCase(); }

  window.PaneSearch = {
    /** case-insensitive substring match; an empty/blank query matches nothing (search is opt-in) */
    match: function (haystack, query) {
      var q = norm(query).trim();
      if (!q) return false;
      return norm(haystack).indexOf(q) !== -1;
    }
  };
})();
