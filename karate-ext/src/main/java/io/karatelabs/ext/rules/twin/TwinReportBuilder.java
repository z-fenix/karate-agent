package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class TwinReportBuilder {
   private TwinReportBuilder() {
   }

   public static Map<String, Object> a(String var0, TwinWalkResult var1, TwinWalkPlan var2, TwinModel var3, Map<String, Object> var4) {
      return a(var0, var1, var2, var3, var4, Map.of());
   }

   public static Map<String, Object> a(String var0, TwinWalkResult var1, TwinWalkPlan var2, TwinModel var3, Map<String, Object> var4, Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var5) {
      var var6 = var1.a();
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("rule", var0);
      var7.put("plan", var2.b());
      if (var4 != null) {
         var7.put("required", var4);
      }

      var7.put("states", a(var1, var3));
      var7.put("transitions", b(var1, var3));
      var7.put("census", var6.get("census"));
      var7.put("ceilings", var6.get("ceilings"));
      var7.put("stats", a(var1, var5));
      var7.put("counterexamples", var6.get("counterexamples"));
      var7.put("fidelity", c(var1, var3));
      return var7;
   }

   private static Map<String, Object> a(TwinWalkResult var0, TwinModel var1) {
      LinkedHashSet var2 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var4 : var0.e()) {
         var2.addAll(var4.e());
      }

      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var6 : var1.b()) {
         (var2.contains(var6.a()) ? var7 : var8).add(var6.a());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("reached", var7);
      var9.put("notReached", var8);
      return var9;
   }

   private static Map<String, Object> b(TwinWalkResult var0, TwinModel var1) {
      LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();
      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var6 : var0.f()) {
         var3.add(var6.b());
         if (var6.d() != null) {
            for (String var8 : a(var0.e().get(var6.a()))) {
               for (String var10 : a(var0.e().get(var6.d()))) {
                  LinkedHashMap<java.lang.String, java.lang.String> var11 = new LinkedHashMap();
                  var11.put("from", var8);
                  var11.put("command", var6.b());
                  var11.put("to", var10);
                  if (var2.add(io.karatelabs.ext.rules.twin.CanonicalJson.c(var11))) {
                     var4.add(var11);
                  }
               }
            }
         }
      }

      ArrayList<java.lang.String> var12 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var15 : var1.c()) {
         if (!var3.contains(var15.a())) {
            var12.add(var15.a());
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("observed", var4);
      var14.put("notObserved", var12);
      return var14;
   }

   private static List<String> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var0) {
      return var0.e().isEmpty() ? List.of("(unnamed)") : var0.e();
   }

   private static Map<String, Object> a(TwinWalkResult var0, Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("nodes", var0.e().size());
      var2.put("edges", var0.f().size());
      var2.put("refusals", var0.g().size());
      var2.put("errors", var0.h().size());
      if (!var1.isEmpty()) {
         LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var3 = new LinkedHashMap();

         for (Entry var5 : var1.entrySet()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("hash", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var5.getValue()).a());
            var6.put("calls", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var5.getValue()).b());
            var6.put("threw", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var5.getValue()).c());
            var6.put("millis", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var5.getValue()).d());
            var3.put((String)var5.getKey(), var6);
         }

         var2.put("rulebooks", var3);
      }

      return var2;
   }

   private static Map<String, Object> c(TwinWalkResult var0, TwinModel var1) {
      var var2 = b(var0.k(), "guards");
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 : var1.c()) {
         var var8 = b(var2, var7.a());
         var3 += a(var8, "observedTrue") + a(var8, "observedFalse") > 0 ? 1 : 0;
         var4 += a(var8, "observedFalse") > 0 ? 1 : 0;
         var5 += a(var8, "notChecked") > 0 ? 1 : 0;
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var14 = new LinkedHashMap();
      var14.put("exercised", var3);
      var14.put("observedFalse", var4);
      var14.put("notChecked", var5);
      int var15 = 0;
      int var16 = 0;

      for (Object var10 : b(var0.k(), "invariants").values()) {
         var15 += a(a(var10), "ran");
         var16 += a(a(var10), "failed");
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var17 = new LinkedHashMap();
      var17.put("ran", var15);
      var17.put("failed", var16);
      int var18 = 0;
      int var11 = 0;

      for (Object var13 : b(var0.k(), "commands").values()) {
         var18 += a(a(var13), "oracle");
         var11 += a(a(var13), "bare");
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var19 = new LinkedHashMap();
      var19.put("oracle", var18);
      var19.put("bare", var11);
      LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
      var20.put("guards", var14);
      var20.put("invariants", var17);
      var20.put("steps", var19);
      var20.put("clock", b(var0.l(), "clock").get("declared") == null ? "frozen" : "declared");
      var20.put("undeclaredOpenFields", var0.l().get("undeclaredOpenFields"));
      return var20;
   }

   private static int a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var2 ? var2.intValue() : 0;
   }

   private static Map<String, Object> b(Map<String, Object> var0, String var1) {
      return a(var0.get(var1));
   }

   private static Map<String, Object> a(Object var0) {
      return var0 == null ? Map.of() : (Map)var0;
   }
}
