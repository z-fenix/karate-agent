package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.rules.twin.Twin;
import java.util.function.Supplier;

class TwinPane extends JsDispatch {
   TwinPane(Twin var1, PlayRecordInspector var2) {
      this.fn("section", var1x -> enveloped(() -> var1.section(JsApi.str(var1x, 0))));
      this.fn("run", var1x -> var1.run(JsApi.str(var1x, 0), JsApi.str(var1x, 1), null));
      this.fn("plays", var1x -> enveloped(() -> var2.a(JsApi.str(var1x, 0))));
      this.fn("confirm", var1x -> enveloped(() -> var2.a(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2), JsApi.arg(var1x, 3))));
   }

   private static Object enveloped(Supplier<Object> var0) {
      try {
         return var0.get();
      } catch (RuntimeException var2) {
         return JsApi.error(var2);
      }
   }
}
