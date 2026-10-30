/*
 * kaLifecycleView — the docs' lifecycle pane (playground.md §7) grouped for reading, shared by the
 * console's API lens and Rule.report's Twin tab (the app's /docs groups the same way server-side).
 * The pane is Openapi.docs's `lifecycle` verbatim; this only regroups it — per operation its rows under
 * each state they were seen from, per state the outcome counts — and never drops or merges a row.
 */
(function (g) {
  var OUTCOMES = ['applied', 'refused', 'unavailable', 'errored', 'notreached'];

  function has(list, v) { return (list || []).indexOf(v) >= 0; }

  // the fields that differ between rows of one state, as "path value[|value]"
  function valuesText(values) {
    return Object.keys(values || {}).map(function (k) { return k + ' ' + values[k].join('|'); }).join(' · ');
  }

  function operation(op, order) {
    var groups = {};
    (op.groups || []).forEach(function (gr) { groups[gr.state] = gr; });
    var states = order.map(function (s) {
      var rows = (groups[s] || {}).rows || [];
      function values(pred) {
        var row = rows.filter(pred)[0];
        return row ? valuesText(row.values) : '';
      }
      var tuples = (op.tuples || []).filter(function (t) { return has(t.fromStates, s); }).map(function (t) {
        return { t: t, values: values(function (r) { return r.tuple === t.id; }) };
      });
      var refusals = (op.refusals || []).filter(function (r) { return has(r.fromStates, s); }).map(function (r) {
        return { r: r, values: values(function (x) { return !x.tuple && x.stage === r.stage && x.reason === r.reason; }) };
      });
      var errors = (op.errors || []).filter(function (e) { return has(e.fromStates, s); }).map(function (e) {
        return { r: e, values: values(function (x) { return !x.tuple && x.stage === e.stage && x.message === e.message; }) };
      });
      return { state: s, tuples: tuples, refusals: refusals, errors: errors };
    }).filter(function (st) { return st.tuples.length + st.refusals.length + st.errors.length > 0; });
    return {
      opId: op.opId, command: op.command, req: op.req, grade: op.grade, depthPruned: op.depthPruned,
      tuples: (op.tuples || []).length, refusals: (op.refusals || []).length, errors: (op.errors || []).length,
      states: states
    };
  }

  function state(s) {
    var counts = {};
    OUTCOMES.forEach(function (o) { counts[o] = 0; });
    (s.commands || []).forEach(function (c) {
      (c.outcomes || []).forEach(function (o) { counts[o.outcome] = (counts[o.outcome] || 0) + 1; });
    });
    return {
      state: s.state, worlds: s.worlds, noneEnabled: s.noneEnabled, commands: s.commands || [],
      counts: OUTCOMES.filter(function (o) { return counts[o]; }).map(function (o) { return { outcome: o, n: counts[o] }; })
    };
  }

  g.kaLifecycleView = function (pane) {
    if (!pane) return null;
    if (pane.unavailable) return { rule: pane.rule, unavailable: pane.unavailable };
    var order = (pane.states || []).map(function (s) { return s.state; });
    var ops = {};
    (pane.operations || []).forEach(function (op) { ops[op.opId] = operation(op, order); });
    var d = pane.disclosure || {};
    return {
      rule: pane.rule, line: d.line, boundary: d.boundary, drift: d.projectionDrift || {},
      witness: d.witness || {}, ops: ops, opList: (pane.operations || []).map(function (op) { return ops[op.opId]; }),
      states: (pane.states || []).map(state), flow: pane.flow || {},
      modelOnly: pane.modelOnly || [], unmodelled: pane.unmodelled || []
    };
  };
  // the fields of an outcome row a reader needs, in one line
  g.kaLifecycleOutcomeText = function (o) {
    var parts = [];
    if (o.stage) parts.push(o.stage);
    if (o.reason) parts.push(o.reason);
    if (o.message) parts.push(o.message);
    if (o.wire) parts.push(o.wire.status + ' ' + o.wire.code);
    if (o.depthPruned) parts.push(o.depthPruned + ' depth-pruned');
    if (o.worlds) parts.push('worlds ' + o.worlds.join(', '));
    return parts.join(' · ');
  };
  g.kaLifecycleValuesText = valuesText;
})(window);
