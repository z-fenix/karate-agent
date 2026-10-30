package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.jsapi.Harness;
import java.util.ArrayList;
import java.util.List;

class RecordingsPane extends JsDispatch {
   RecordingsPane(Harness var1) {
      this.fn("of", var1x -> {
         ArrayList<java.lang.String> var2 = new ArrayList();
         Object var4 = JsApi.arg(var1x, 0);
         if (var4 instanceof List) {
            for (Object var5 : (List)var4) {
               var2.add(String.valueOf(var5));
            }
         }

         return var1.recorded(var2);
      });
      this.fn("steps", var1x -> var1.recordedSteps(JsApi.str(var1x, 0)));
   }
}
