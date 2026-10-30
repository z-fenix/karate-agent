package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;

class LookupsVerbs extends JsDispatch {
   LookupsVerbs(Rule var1) {
      this.fn("update", var1x -> var1.lookupsUpdate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("rows", var1x -> var1.lookupsRows(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("cell", var1x -> var1.lookupsCell(JsApi.str(var1x, 0), JsApi.str(var1x, 1)));
   }
}
