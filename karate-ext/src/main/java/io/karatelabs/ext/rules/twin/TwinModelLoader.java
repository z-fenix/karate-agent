package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;

public final class TwinModelLoader {
   private static final String a = "(function(fn, w, a) { var r = fn(w, a); return r === undefined ? null : typeof r; })";

   private TwinModelLoader() {
   }

   public static TwinModel a(String var0) {
      TwinBuilder var1 = new TwinBuilder();
      Engine var2 = new Engine();
      var2.put("t", var1);

      try {
         var2.eval(var0);
      } catch (RuntimeException var5) {
         TwinException var4 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var5, TwinException.class);
         if (var4 != null) {
            throw var4;
         }

         throw new TwinException(400, "PARSE_FAILED", JsApi.jsErrorMessage(var5));
      }

      return var1.build((JavaCallable)var2.eval("(function(fn, w, a) { var r = fn(w, a); return r === undefined ? null : typeof r; })"));
   }
}
