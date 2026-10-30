package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.jsapi.Campaign;
import io.karatelabs.ext.max.jsapi.Harness;
import io.karatelabs.ext.max.jsapi.Runner;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import java.util.Map;

class RailPane extends JsDispatch {
   RailPane(Runner var1, Harness var2, Campaign var3) {
      this.fn("tests", var1x -> var1 == null ? 0 : (int)var1.testPaths().stream().filter(RunnerPane::manual).count());
      this.fn("campaigns", var1x -> var3 == null ? 0 : var3.count());
      this.fn("runs", var1x -> var2.count());
      this.fn("reason", var0 -> JsApi.arg(var0, 0) instanceof Map var1x ? Requirement.readinessReason(var1x) : null);
   }
}
