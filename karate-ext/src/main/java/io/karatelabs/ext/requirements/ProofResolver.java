package io.karatelabs.ext.requirements;

import io.karatelabs.js.ObjectLike;
import java.util.List;
import java.util.Map;

public final class ProofResolver {
   private ProofResolver() {
   }

   public static List<String> a(Object var0, Map<String, Object> var1) {
      if (var0 != null && var1 != null) {
         io.karatelabs.ext.common.JsCallback var2 = io.karatelabs.ext.common.JsCallback.a(var0);
         if (var2 != null) {
            return a(var2.a(var1));
         } else if (!(var0 instanceof ObjectLike) && !(var0 instanceof Map)) {
            return List.of();
         } else {
            io.karatelabs.ext.common.JsCallback var3 = io.karatelabs.ext.common.JsCallback.b(var0, "resolve");
            return var3 != null ? a(var3.a(var1)) : a(io.karatelabs.ext.common.JsCallback.a(var0, String.valueOf(var1.get("id"))));
         }
      } else {
         return List.of();
      }
   }

   static List<String> a(Object var0) {
      return io.karatelabs.ext.common.JsCallback.b(var0);
   }
}
