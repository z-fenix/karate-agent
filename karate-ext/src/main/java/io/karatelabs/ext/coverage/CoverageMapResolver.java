package io.karatelabs.ext.coverage;

import io.karatelabs.js.ObjectLike;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoverageMapResolver {
   private CoverageMapResolver() {
   }

   public static List<String> a(Object var0, Map<String, Object> var1) {
      if (var0 != null && var1 != null) {
         io.karatelabs.ext.common.JsCallback var2 = io.karatelabs.ext.common.JsCallback.a(var0);
         if (var2 != null) {
            return io.karatelabs.ext.common.JsCallback.b(var2.a(var1));
         } else if (!(var0 instanceof ObjectLike) && !(var0 instanceof Map)) {
            return List.of();
         } else {
            io.karatelabs.ext.common.JsCallback var3 = io.karatelabs.ext.common.JsCallback.b(var0, "resolve");
            return var3 != null
               ? io.karatelabs.ext.common.JsCallback.b(var3.a(var1))
               : io.karatelabs.ext.common.JsCallback.b(io.karatelabs.ext.common.JsCallback.a(var0, String.valueOf(var1.get("slug"))));
         }
      } else {
         return List.of();
      }
   }

   public static Map<String, Object> a(TestExecutionSummary var0, String var1) {
      List var2 = var0.c() == null ? List.of() : List.copyOf(var0.c());
      LinkedHashMap<java.lang.String, java.util.ArrayList> var3 = new LinkedHashMap();

      for (String var5 : (Iterable<String>) (Iterable<?>) (var2)) {
         int var6 = var5.indexOf(61);
         String var7 = var6 <= 0 ? var5 : var5.substring(0, var6).trim();
         List<java.lang.String> var8 = var3.computeIfAbsent(var7, var0x -> new ArrayList());
         if (var6 > 0) {
            for (String var12 : var5.substring(var6 + 1).split(",")) {
               String var13 = var12.trim();
               if (!var13.isEmpty()) {
                  var8.add(var13);
               }
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("name", var0.a());
      var14.put("feature", var0.d());
      var14.put("slug", var1);
      var14.put("tags", var2);
      var14.put("tagValues", var3);
      return var14;
   }
}
