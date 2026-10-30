package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;

class ScenarioVerbs extends JsDispatch {
   ScenarioVerbs(Rule var1) {
      this.fn("create", var1x -> var1.scenarioCreate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("update", var1x -> var1.scenarioUpdate(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2), JsApi.arg(var1x, 3)));
      this.fn("delete", var1x -> var1.scenarioDelete(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
   }
}
