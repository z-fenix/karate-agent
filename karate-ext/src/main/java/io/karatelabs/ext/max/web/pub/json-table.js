/*
 * JsonTable — the generic json→table renderer (D111c, friendlied in D114): the veriquant
 * per-rulebook calc.html fragment pattern generalized into ONE algorithm, shared across pages
 * (rulebook Output pane, scenario expected/actual, the D114 test-data matrix, future RTM evidence
 * rails). Pure function → HTML string (Alpine x-html), everything escaped; the Tailwind classes
 * here are scanned by etc/web-assets/build.sh (pub/*.js rides the content globs).
 *
 *   - array of uniform objects → striped fixed-column grid (columns = the union of keys)
 *   - plain object            → key/value rows; complex values recurse inline
 *   - scalar / null           → an inline mono span
 *   render(v, {literal: true}) keeps keys and values as written (an API body), skipping the friendly pass.
 *
 * D114 "friendly" pass (the veriquant json-grid aesthetic): humanized keys (camelCase → Title
 * Case, raw key on hover), type-aware cells (right-aligned numerics with thousands separators,
 * currency on monetary keys, muted nulls, Yes/No booleans), and LIGHTER nested chrome — a
 * borderless label/value CSS-grid for nested objects instead of heavy bordered nested tables.
 */
(function () {
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function isPlainObject(v) {
    return v !== null && typeof v === 'object' && !Array.isArray(v);
  }

  /** every element a plain object → renderable as a fixed-column grid */
  function isGrid(v) {
    return Array.isArray(v) && v.length > 0 && v.every(isPlainObject);
  }

  function gridColumns(arr) {
    var seen = {}, cols = [];
    arr.forEach(function (row) {
      Object.keys(row).forEach(function (k) {
        if (!seen[k]) { seen[k] = true; cols.push(k); }
      });
    });
    return cols;
  }

  // ── friendly helpers (D114) ──

  /** camelCase / snake_case / kebab-case → "Title Case"; ALLCAPS + short tokens kept verbatim */
  function humanize(key) {
    if (!key) return '';
    if (literal) return String(key);
    if (/^[A-Z0-9_]+$/.test(key) && key.length <= 4) return key; // VIN, ID, NAIC — leave acronyms
    var words = String(key)
      .replace(/[_-]+/g, ' ')
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
      .trim();
    return words.replace(/\b\w/g, function (c) { return c.toUpperCase(); });
  }

  /** a monetary key (insurance domain) → render the number as currency. Deliberately excludes
   *  rate/factor (those are multipliers, not money) — see D114 spec. */
  function isMoneyKey(key) {
    return !!key && /(^|[a-z])(premium|cost|price|amount|total|fee|deductible|charge|surcharge|subtotal|payment|balance)s?$/i.test(key);
  }

  function formatNumber(n, key) {
    if (!isFinite(n)) return String(n);
    if (isMoneyKey(key)) {
      return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    if (Number.isInteger(n)) return n.toLocaleString('en-US');
    return n.toLocaleString('en-US', { maximumFractionDigits: 6 });
  }

  function isNumeric(v) { return typeof v === 'number' && isFinite(v); }

  // {literal: true}: keys and values exactly as on the wire (an API body), not the friendly reading
  var literal = false;

  function scalarHtml(v, key) {
    if (v === null || v === undefined) {
      return '<span class="font-mono text-slate-400 dark:text-slate-500">null</span>';
    }
    if (isNumeric(v)) {
      return '<span class="font-mono tabular-nums text-slate-900 dark:text-slate-100">' + esc(literal ? String(v) : formatNumber(v, key)) + '</span>';
    }
    if (typeof v === 'boolean' && literal) {
      return '<span class="font-mono text-slate-900 dark:text-slate-100">' + v + '</span>';
    }
    if (typeof v === 'boolean') {
      return v
        ? '<span class="font-medium text-emerald-700 dark:text-emerald-400">Yes</span>'
        : '<span class="font-medium text-slate-400 dark:text-slate-500">No</span>';
    }
    return '<span class="font-mono text-slate-900 dark:text-slate-100">' + esc(String(v)) + '</span>';
  }

  // ── chrome ──

  var TABLE = 'w-full text-xs';
  var WRAP = 'rounded border border-slate-200 dark:border-slate-700 overflow-hidden my-1';
  var TH = 'px-2 py-1 text-left font-semibold text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-900/60';
  var TD = 'px-2 py-1 align-top border-t border-slate-100 dark:border-slate-700/60';
  var TR_STRIPE = 'odd:bg-white even:bg-slate-50/70 dark:odd:bg-slate-800 dark:even:bg-slate-900/40';

  /** array of uniform objects → a striped grid. Columns whose every value is numeric are
   *  right-aligned (the "ledger" read); headers humanized with the raw key on hover. */
  function gridHtml(arr, depth) {
    var cols = gridColumns(arr);
    var numericCol = {};
    cols.forEach(function (c) {
      numericCol[c] = arr.every(function (row) { return !(c in row) || row[c] == null || isNumeric(row[c]); })
        && arr.some(function (row) { return isNumeric(row[c]); });
    });
    var h = '<div class="' + WRAP + '"><table class="' + TABLE + '"><thead><tr>';
    cols.forEach(function (c) {
      h += '<th class="' + TH + (numericCol[c] ? ' text-right' : '') + '" title="' + esc(c) + '">' + esc(humanize(c)) + '</th>';
    });
    h += '</tr></thead><tbody>';
    arr.forEach(function (row) {
      h += '<tr class="' + TR_STRIPE + '">';
      cols.forEach(function (c) {
        h += '<td class="' + TD + (numericCol[c] ? ' text-right tabular-nums' : '') + '">'
          + (c in row ? cellHtml(row[c], c, depth + 1) : '') + '</td>';
      });
      h += '</tr>';
    });
    return h + '</tbody></table></div>';
  }

  function kvHtml(obj, depth) {
    var keys = Object.keys(obj);
    if (depth > 0) {
      // lighter nested chrome (D114): a borderless label/value CSS grid, not another bordered table.
      // grid-template-columns is an inline style (this generated HTML isn't Tailwind-scanned, so an
      // arbitrary grid-cols-[…] utility wouldn't be emitted) — the rest stays utility classes.
      var g = '<div class="my-0.5 pl-2 border-l border-slate-200 dark:border-slate-700/60"'
        + ' style="display:grid;grid-template-columns:auto 1fr;column-gap:0.75rem;row-gap:0.125rem">';
      keys.forEach(function (k) {
        g += '<div class="font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap" title="' + esc(k) + '">' + esc(humanize(k)) + '</div>'
          + '<div class="min-w-0">' + cellHtml(obj[k], k, depth + 1) + '</div>';
      });
      return g + '</div>';
    }
    var h = '<div class="' + WRAP + '"><table class="' + TABLE + '"><tbody>';
    keys.forEach(function (k) {
      h += '<tr class="' + TR_STRIPE + '">'
        + '<td class="' + TD + ' font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap w-px" title="' + esc(k) + '">' + esc(humanize(k)) + '</td>'
        + '<td class="' + TD + '">' + cellHtml(obj[k], k, depth + 1) + '</td></tr>';
    });
    return h + '</tbody></table></div>';
  }

  /** non-uniform array → an indexed key/value table */
  function listHtml(arr, depth) {
    var h = '<div class="' + WRAP + '"><table class="' + TABLE + '"><tbody>';
    arr.forEach(function (v, i) {
      h += '<tr class="' + TR_STRIPE + '">'
        + '<td class="' + TD + ' font-medium text-slate-400 dark:text-slate-500 whitespace-nowrap w-px">[' + i + ']</td>'
        + '<td class="' + TD + '">' + cellHtml(v, null, depth + 1) + '</td></tr>';
    });
    return h + '</tbody></table></div>';
  }

  /** one value at a given nesting depth, with its key for type-aware scalar formatting */
  function cellHtml(v, key, depth) {
    if (isGrid(v)) return gridHtml(v, depth);
    if (Array.isArray(v)) return v.length === 0 ? scalarHtml('[]') : listHtml(v, depth);
    if (isPlainObject(v)) return Object.keys(v).length === 0 ? scalarHtml('{}') : kvHtml(v, depth);
    return scalarHtml(v, key);
  }

  window.JsonTable = {
    esc: esc,
    isGrid: isGrid,
    gridColumns: gridColumns,
    humanize: humanize,
    render: function (v, opts) {
      literal = !!(opts && opts.literal);
      try { return cellHtml(v, null, 0); } finally { literal = false; }
    },
    // a single value → HTML with key-aware scalar formatting (currency/number/bool/null) and a
    // light nested grid for objects — the D114 matrix cell renderer (shares this module's logic)
    cell: function (v, key) {
      if (v !== null && typeof v === 'object') return cellHtml(v, key, 1);
      return scalarHtml(v, key);
    }
  };
})();
