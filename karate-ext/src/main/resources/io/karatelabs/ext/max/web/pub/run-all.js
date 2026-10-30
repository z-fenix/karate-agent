/*
 * RunAll — the ONE sequential run-all machinery the rulebook page's case-table uses (D111d).
 * A client-side loop (v1 — no SSE run-all endpoint on the /ui channel): per-item status
 * callbacks drive the page's Alpine state, results are RETAINED per item (selecting a row
 * shows ITS output), and a stop request breaks cleanly between items.
 *
 * "Failed" semantics are the CALLER's (the isFailed hook): a rule case fails when its calc
 * errors, or (when an expected output is present) on the oracle-vs-actual verdict.
 */
(function () {
  window.RunAll = {
    /**
     * @param items   array to run, in order
     * @param runOne  async (item, index) → result (an {error} envelope is a failure)
     * @param hooks   {onStart(item,i), onDone(item,i,result,ok), shouldStop(), isFailed(result,item,i)}
     * @returns {completed, failed, stopped, durationMs}
     */
    sequential: async function (items, runOne, hooks) {
      hooks = hooks || {};
      var started = Date.now();
      var completed = 0, failed = 0, stopped = false;
      for (var i = 0; i < items.length; i++) {
        if (hooks.shouldStop && hooks.shouldStop()) { stopped = true; break; }
        if (hooks.onStart) hooks.onStart(items[i], i);
        var result;
        try {
          result = await runOne(items[i], i);
        } catch (e) {
          result = { error: String((e && e.message) || e) };
        }
        var ok = !(result && result.error);
        if (ok && hooks.isFailed && hooks.isFailed(result, items[i], i)) ok = false;
        if (ok) completed++; else failed++;
        if (hooks.onDone) hooks.onDone(items[i], i, result, ok);
      }
      return { completed: completed, failed: failed, stopped: stopped, durationMs: Date.now() - started };
    }
  };
})();
