package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;

class CalcVerbs extends JsDispatch {
   CalcVerbs(Rule var1) {
      this.fn("update", var1x -> var1.calcUpdate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
   }
}
