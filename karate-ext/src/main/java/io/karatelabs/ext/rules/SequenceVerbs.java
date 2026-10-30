package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;

class SequenceVerbs extends JsDispatch {
   SequenceVerbs(Rule var1) {
      this.fn("create", var1x -> var1.sequenceCreate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("update", var1x -> var1.sequenceUpdate(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2), JsApi.arg(var1x, 3)));
      this.fn("delete", var1x -> var1.sequenceDelete(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
   }
}
