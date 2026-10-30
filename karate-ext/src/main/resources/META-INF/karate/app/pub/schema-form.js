/*
 * SchemaForm — pure helpers for a schema-driven, recursive, EDITABLE form.
 * Ported from markup-playground app/pub/schema-form.js (first-party). One copy serves the console's
 * rulebook inputs pane (D86) and the playground app's action forms (D315); the console aliases
 * /pub/schema-form.js here.
 *
 * Render an editable form from a karate-js schema literal you don't know at author time.
 * Pure functions only — the page (Alpine) owns the state and calls flatten() to get the flat
 * row list its template renders.
 *
 * Schema markers (a karate-js literal):
 *   '#string' '#number' '#integer' '#boolean' '#date'   → primitives
 *   '#int[0,100]' '#number[,25]'                        → a number carrying its bounds (min / max)
 *   ['a','b','c'] [10, 40]   (literals, no '#')         → enum (a <select>); one member is a const
 *   { '#enum': [ ...any values ] }                       → enum of exactly these members, whatever they are
 *   [ elementSchema ]        (one element, itself a schema) → array-of-T
 *   { key: subSchema, … }    (a plain map)               → nested object
 *
 * Row kinds emitted by flatten(): 'object' | 'array' | 'field'. An array item is just the
 * element schema walked one level deeper with `removable: true`. `collapsed` is a plain
 * object keyed by path-string.
 */
(function (global) {
  'use strict';

  function isMarked(s) {
    return s != null && typeof s === 'object' && !Array.isArray(s) && Object.keys(s).length === 1
      && Array.isArray(s['#enum']);
  }
  function isEnum(s) {
    return isMarked(s) || Array.isArray(s) && s.length > 0 && s.every(function (e) {
      return (typeof e === 'string' && e.charAt(0) !== '#') || typeof e === 'number' || typeof e === 'boolean';
    });
  }
  function enumOptions(s) { return isMarked(s) ? s['#enum'] : s; }
  function isArrayOf(s) { return Array.isArray(s) && !isEnum(s); }
  function isObject(s)  { return s != null && typeof s === 'object' && !Array.isArray(s) && !isMarked(s); }

  // An option as a person reads it: text as-is, any other value as its JSON.
  function optionText(v) { return typeof v === 'string' ? v : JSON.stringify(v); }
  function sameValue(a, b) { return JSON.stringify(a) === JSON.stringify(b); }

  // '#int[lo,hi]' / '#number[lo,hi]' (either side may be open, '##' optional) → {type, min, max}.
  function range(marker) {
    var m = typeof marker === 'string' && /^##?(int|number)\[\s*(-?[\d.]*)\s*,\s*(-?[\d.]*)\s*\]$/.exec(marker);
    if (!m) return null;
    return { type: m[1] === 'int' ? 'integer' : 'number',
             min: m[2] === '' ? null : Number(m[2]), max: m[3] === '' ? null : Number(m[3]) };
  }

  // '#number' → 'number', etc. Unknown markers fall back to 'string'.
  function primType(marker) {
    var r = range(marker);
    if (r) return r.type;
    switch (marker) {
      case '#number':  return 'number';
      case '#integer': return 'integer';
      case '#boolean': return 'boolean';
      case '#date':    return 'date';
      default:         return 'string';
    }
  }

  // Path-walk read; undefined if any segment is missing.
  function getValue(obj, path) {
    var cur = obj;
    for (var i = 0; i < path.length; i++) {
      if (cur == null) return undefined;
      cur = cur[path[i]];
    }
    return cur;
  }

  // Path-walk write; creates intermediate objects/arrays as needed (a numeric
  // next-segment implies an array).
  function setValue(obj, path, val) {
    var cur = obj;
    for (var i = 0; i < path.length - 1; i++) {
      if (cur[path[i]] == null) cur[path[i]] = (typeof path[i + 1] === 'number') ? [] : {};
      cur = cur[path[i]];
    }
    cur[path[path.length - 1]] = val;
  }

  // A blank value matching a schema — used when adding an array item.
  function defaultFromSchema(s) {
    if (isEnum(s))    return enumOptions(s)[0];
    if (isArrayOf(s)) return [];
    if (isObject(s)) {
      var o = {};
      Object.keys(s).forEach(function (k) { o[k] = defaultFromSchema(s[k]); });
      return o;
    }
    switch (s) {
      case '#number': case '#integer': return 0;
      case '#boolean': return false;
      default:         return '';
    }
  }

  // Flatten schema (+ the live value, for array lengths) into the visible rows.
  function flatten(schema, value, collapsed) {
    var rows = [];
    collapsed = collapsed || {};
    function key(path) { return path.join('.'); }

    function walk(s, path, depth, label, removable) {
      if (isObject(s)) {
        rows.push({ kind: 'object', path: path, depth: depth, label: label,
                    size: Object.keys(s).length, removable: !!removable });
        if (!collapsed[key(path)]) {
          Object.keys(s).forEach(function (k) { walk(s[k], path.concat(k), depth + 1, k, false); });
        }
      } else if (isArrayOf(s)) {
        var arr = getValue(value, path) || [];
        rows.push({ kind: 'array', path: path, depth: depth, label: label, size: arr.length, elem: s[0] });
        if (!collapsed[key(path)]) {
          for (var i = 0; i < arr.length; i++) walk(s[0], path.concat(i), depth + 1, '#' + i, true);
        }
      } else {
        var r = range(s);
        rows.push({ kind: 'field', path: path, depth: depth, label: label,
                    type: isEnum(s) ? 'enum' : primType(s),
                    enumOpts: isEnum(s) ? enumOptions(s) : null, removable: !!removable,
                    min: r ? r.min : null, max: r ? r.max : null });
      }
    }
    Object.keys(schema).forEach(function (k) { walk(schema[k], [k], 0, k, false); });
    return rows;
  }

  global.SchemaForm = {
    flatten: flatten, getValue: getValue, setValue: setValue,
    defaultFromSchema: defaultFromSchema, primType: primType, range: range,
    isEnum: isEnum, isArrayOf: isArrayOf, isObject: isObject,
    optionText: optionText, sameValue: sameValue,
  };
})(window);
