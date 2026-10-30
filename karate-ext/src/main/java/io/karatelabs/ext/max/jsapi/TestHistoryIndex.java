package io.karatelabs.ext.max.jsapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class TestHistoryIndex {
   private TestHistoryIndex() {
   }

   static Map<String, io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestHistorySummary> a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestHistorySummary> var1 = new LinkedHashMap();
      b(var0).forEach((var1x, var2) -> {
         io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate var3 = null;
         int var4 = 0;
         ArrayList<java.lang.Long> var5 = new ArrayList();

         for (io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate var7 : var2.values()) {
            if (!var7.b) {
               var4++;
            }

            if (var7.e) {
               var5.add(var7.d);
            }

            if (var3 == null || var7.c > var3.c || var7.c == var3.c && var7.a.compareTo(var3.a) > 0) {
               var3 = var7;
            }
         }

         var5.sort(null);
         int var8 = var5.size();
         Long var9 = var8 == 0 ? null : var8 % 2 == 1 ? (Long)var5.get(var8 / 2) : ((Long)var5.get(var8 / 2 - 1) + (Long)var5.get(var8 / 2)) / 2L;
         var1.put(var1x, new io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestHistorySummary(var2.size(), var4, var3.b ? "failed" : "passed", var3.c, var3.a, var9));
      });
      return var1;
   }

   static Map<String, Map<String, io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate>> b(Map<String, Object> var0) {
      if (var0 == null) {
         return Map.of();
      } else {
         HashMap<java.lang.String, java.lang.String> var1 = new HashMap();

         for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
            String var4 = c(var3);
            if (var4 != null) {
               var1.put(String.valueOf(var3.get("id")), var4);
            }
         }

         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate>> var13 = new LinkedHashMap();

         for (Map var15 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "executions")) {
            String var5 = String.valueOf(var15.get("outcome"));
            if (!"running".equals(var15.get("lifecycle")) && ("passed".equals(var5) || "failed".equals(var5))) {
               String var6 = (String)var1.get(String.valueOf(var15.get("test")));
               if (var6 != null) {
                  String var7 = String.valueOf(var15.get("runId"));
                  io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate var8 = var13.computeIfAbsent(var6, var0x -> new LinkedHashMap<>()).computeIfAbsent(var7, io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate::new);
                  var8.b = var8.b | "failed".equals(var5);
                  Long var9 = a(var15.get("startedAt"));
                  Long var10 = a(var15.get("endedAt"));
                  long var11 = var10 != null ? var10 : (var9 != null ? var9 : 0L);
                  var8.c = Math.max(var8.c, var11);
                  if (var9 != null && var10 != null && var10 >= var9) {
                     var8.d = var8.d + (var10 - var9);
                     var8.e = true;
                  }
               }
            }
         }

         return var13;
      }
   }

   static String c(Map<String, Object> var0) {
      String var1 = String.valueOf(var0.get("id"));
      if (var1.startsWith("check:")) {
         return var1.substring("check:".length());
      } else {
         return var0.get("feature") instanceof String var2 && io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var2).endsWith(".feature") ? io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var2) : null;
      }
   }

   private static Long a(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : null;
   }

   static final class TestRunAggregate {
      final String a;
      boolean b;
      long c;
      long d;
      boolean e;

      TestRunAggregate(String var1) {
         this.a = var1;
      }
   }

   record TestHistorySummary(int b, int c, String d, long e, String f, Long g) {


      Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("runs", this.b);
         var1.put("passed", this.c);
         var1.put("lastStatus", this.d);
         var1.put("lastRunAt", this.e);
         var1.put("lastRunId", this.f);
         if (this.g != null) {
            var1.put("typicalMs", this.g);
         }

         return var1;
      }

      public int b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public long e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public Long g() {
         return this.g;
      }
   }
}
