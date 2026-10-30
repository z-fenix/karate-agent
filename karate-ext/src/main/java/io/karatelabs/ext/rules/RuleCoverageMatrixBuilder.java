package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

public final class RuleCoverageMatrixBuilder {
   private RuleCoverageMatrixBuilder() {
   }

   static Map<String, Object> a(String var0, Object var1, List<Map<String, Object>> var2, List<Map<String, Object>> var3, RuleEvaluator var4) {
      return a(var0, Map.of(), var1, var2, var3, var4, null, Map.of());
   }

   static Map<String, Object> a(
      String var0,
      Map<String, Object> var1,
      Object var2,
      List<Map<String, Object>> var3,
      List<Map<String, Object>> var4,
      RuleEvaluator var5,
      List<String> var6,
      Map<Integer, String> var7
   ) {
      var var8 = RuleInputSpaceExplorer.b(var0);
      var var9 = new ArrayList<>(var8.values());
      ArrayList<String> var10 = new ArrayList<>(var9);
      var10.sort(RuleCoverageMatrixBuilder::a);
      RuleFlowGraph var11 = io.karatelabs.ext.rules.RuleFlowGraph.a(var0);
      var var12 = var11.c().stream().filter(var0x -> "decision".equals(var0x.b())).toList();
      if (var6 == null) {
         var6 = new ArrayList();
         a(var2, "", (List<String>)var6);
      }

      ArrayList<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow> var13 = new ArrayList();
      LinkedHashSet var14 = new LinkedHashSet();

      for (Map var16 : (Iterable<Map>) (Iterable<?>) (var3 == null ? List.of() : var3)) {
         String var17 = b(var16.get("_id"));
         String var18 = b(var16.get("_label"));
         var13.add(a(var0, var1, var16, "scenario", var17, var18 != null ? var18 : (var17 != null ? var17 : "scenario"), var8, var10, var5, var14));
      }

      for (Map var27 : (Iterable<Map>) (Iterable<?>) (var4 == null ? List.of() : var4)) {
         if (var27.get("input") instanceof Map var31) {
            var13.add(a(var0, var1, var31, "generated", null, null, var8, var10, var5, var14));
         }
      }

      var var28 = var6.stream().filter(var0x -> !RulebookRepository.a(var0x)).filter(var1x -> a(var13, var1xx -> b(var1xx.d, var1x))).toList();
      var var30 = var12.stream().filter(var1x -> a(var13, var1xx -> a(var1xx.e.get(var1x.a())))).toList();
      boolean var32 = var13.stream().anyMatch(var0x -> var0x.h);
      ArrayList<java.lang.String> var19 = new ArrayList();
      LinkedHashSet<java.lang.String> var20 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var22 : (Iterable<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow>) (Iterable<?>) (var13)) {
         if (var22.f instanceof Map var23) {
            for (Object var25 : var23.keySet()) {
               if (var20.add(String.valueOf(var25))) {
                  var19.add(String.valueOf(var25));
               }
            }
         }
      }

      for (io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var34 : (Iterable<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow>) (Iterable<?>) (var13)) {
         if (var34.a == null) {
            var34.a = a(var34, var28);
         }
      }

      return a(var13, var28, var30, var19, var10, var14.size(), var7 == null ? Map.of() : var7, var32);
   }

   private static io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow a(
      String var0,
      Map<String, Object> var1,
      Map<String, Object> var2,
      String var3,
      String var4,
      String var5,
      Map<Integer, String> var6,
      List<String> var7,
      RuleEvaluator var8,
      Set<String> var9
   ) {
      io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var10 = new io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow(var3, var4, var5, var2);
      BranchOutcomeCollector var11 = new BranchOutcomeCollector();

      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var12 = var8.a(var0, var1, var2, var11);
         var10.e = var11.a(var6);
         var10.f = var12.b();
         var10.g = var12.f();
         var10.h = var12.g();
         var10.i = a(var7, var10.e);
         var10.k = var9.add(var10.i);
      } catch (RuntimeException var13) {
         var10.j = var13.getMessage() == null ? var13.getClass().getSimpleName() : var13.getMessage();
      }

      return var10;
   }

   public static String a(List<String> var0, Map<String, Set<Boolean>> var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var0) {
         var2.append(var4).append('=').append(a((Set<Boolean>)var1.get(var4))).append(';');
      }

      return var2.toString();
   }

   static Map<String, Object> a(Map<String, Object> var0, io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var1) {
      if (var1 != null && var1.a()) {
         if (var1.b()) {
            return a(var1);
         } else {
            LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();

            for (Map var4 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("rows"))) {
               if (var4.get("outcomeKey") instanceof String var5) {
                  var2.add(RuleInputSpaceCertificateBuilder.a(var5));
               }
            }

            return a(var2, (List<Map<String, Object>>)var1.c().get("signatures"));
         }
      } else {
         return null;
      }
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var0) {
      var var1 = var0.c();
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("signatures", var1.get("signatures") instanceof List var3 ? var3.size() : 0);
      var2.put("rows", var1.get("accepted"));
      var2.put("unresolvedRows", var1.get("unresolved"));
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("certified", false);
      var5.put("partial", true);
      var5.put("floor", var2);
      return var5;
   }

   static Map<String, Object> a(Set<String> var0, List<Map<String, Object>> var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      int var3 = 0;

      for (Map var5 : var1) {
         if (var0.contains(String.valueOf(var5.get("key")))) {
            var3++;
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("key", var5.get("key"));
            var6.put("witness", var5.get("witness"));
            var2.add(var6);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("certified", true);
      var7.put("covered", var3);
      var7.put("of", var1.size());
      var7.put("notcovered", var2);
      return var7;
   }

   static String a(Set<Boolean> var0) {
      if (var0 != null && !var0.isEmpty()) {
         if (var0.size() > 1) {
            return "*";
         } else {
            return var0.contains(Boolean.TRUE) ? "T" : "F";
         }
      } else {
         return "-";
      }
   }

   private static Map<String, Object> a(
      List<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow> var0, List<String> var1, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> var2, List<String> var3, List<String> var4, int var5, Map<Integer, String> var6, boolean var7
   ) {
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var9 = new ArrayList();

      for (String var11 : var1) {
         LinkedHashMap<java.lang.String, java.lang.String> var12 = new LinkedHashMap();
         var12.put("path", var11);
         var12.put("label", b(var11));
         var9.add(var12);
      }

      var8.put("inputs", var9);
      ArrayList<java.util.LinkedHashMap> var21 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var24 : var2) {
         LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
         var13.put("id", var24.a());
         var13.put("line", var24.d());
         String var14 = a(var6, var24.d());
         var13.put("label", var14 != null ? var14 : var24.c());
         var21.add(var13);
      }

      var8.put("decisions", var21);
      var8.put("outputs", var3);
      var8.put("hasOutcome", var7);
      ArrayList<java.util.LinkedHashMap> var23 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var27 : var0) {
         LinkedHashMap<java.lang.String, java.lang.Object> var29 = new LinkedHashMap();
         var29.put("name", var27.a);
         var29.put("provenance", var27.b);
         if (var27.c != null) {
            var29.put("scenarioId", var27.c);
         }

         var29.put("addsCoverage", var27.k);
         if (var27.i != null) {
            var29.put("outcomeKey", var27.i);
         }

         if (var27.j != null) {
            var29.put("error", var27.j);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();

         for (String var17 : var1) {
            var15.put(var17, b(var27.d, var17));
         }

         var29.put("inputs", var15);
         var29.put("input", var27.d);
         LinkedHashMap<java.lang.String, java.lang.String> var31 = new LinkedHashMap();

         for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var18 : var2) {
            var31.put(var18.a(), a(var27.e.get(var18.a())));
         }

         var29.put("decisions", var31);
         LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();
         if (var27.f instanceof Map var34) {
            for (String var20 : var3) {
               var33.put(var20, var34.get(var20));
            }
         }

         var29.put("outputs", var33);
         if (var7) {
            var29.put("outcome", var27.g);
         }

         var23.add(var29);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var26 = new LinkedHashMap();
      var26.put("columns", var8);
      var26.put("rows", var23);
      var26.put("siteUniverse", var4);
      LinkedHashMap<java.lang.String, java.lang.Object> var28 = new LinkedHashMap();
      long var30 = var0.stream().filter(var0x -> "scenario".equals(var0x.b)).count();
      var28.put("rows", var0.size());
      var28.put("scenarios", var30);
      var28.put("generated", var0.size() - var30);
      var28.put("distinctPaths", var5);
      var26.put("stats", var28);
      return var26;
   }

   static String a(Map<String, Object> var0) {
      Map<java.lang.String, java.util.List> var1 = (Map)var0.get("columns");
      List<Map<String, Object>> var2 = (List)var1.get("inputs");
      List<Map<String, Object>> var3 = (List)var1.get("decisions");
      List<String> var4 = (List)var1.get("outputs");
      List<Map<String, Object>> var5 = (List)var0.get("rows");
      boolean var6 = Boolean.TRUE.equals(var1.get("hasOutcome"));
      ArrayList<java.lang.String> var7 = new ArrayList();
      var7.add("Scenario");
      var2.forEach(var1x -> var7.add(d(String.valueOf(var1x.get("label")))));
      var3.forEach(var1x -> var7.add(d(a(String.valueOf(var1x.get("label")), 28))));
      if (var6) {
         var7.add("Outcome");
      }

      var4.forEach(var1x -> var7.add(d(var1x)));
      StringBuilder var8 = new StringBuilder();
      var8.append("| ").append(String.join(" | ", var7)).append(" |\n");
      var8.append("|").append(" --- |".repeat(var7.size())).append('\n');

      for (Map var10 : (Iterable<Map>) (Iterable<?>) (var5)) {
         if ("scenario".equals(var10.get("provenance")) || Boolean.TRUE.equals(var10.get("addsCoverage"))) {
            ArrayList<String> var11 = new ArrayList<>();
            String var12 = d(String.valueOf(var10.get("name")));
            var11.add(var10.get("error") != null ? var12 + " ⚠️" : var12);
            Map var13 = (Map)var10.get("inputs");
            var2.forEach(var2x -> var11.add(d(a(var13.get(var2x.get("path"))))));
            Map var14 = (Map)var10.get("decisions");
            var3.forEach(var2x -> var11.add(a(String.valueOf(var14.get(var2x.get("id"))))));
            if (var6) {
               var11.add(d(a(var10.get("outcome"))));
            }

            Map<java.lang.Object, Object> var15 = (Map)var10.get("outputs");
            var4.forEach(var2x -> var11.add(d(a(var15.get(var2x)))));
            var8.append("| ").append(String.join(" | ", var11)).append(" |\n");
         }
      }

      return var8.toString();
   }

   private static String a(String var0) {
      return switch (var0) {
         case "T" -> "Y";
         case "F" -> "N";
         case "*" -> "Y/N";
         default -> "–";
      };
   }

   private static void a(Object var0, String var1, List<String> var2) {
      if (var0 instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            String var6 = var1.isEmpty() ? String.valueOf(var5.getKey()) : var1 + "." + var5.getKey();
            Object var7 = var5.getValue();
            if (var7 instanceof Map) {
               a(var7, var6, var2);
            } else if (var7 instanceof List var8) {
               if (var8.stream().noneMatch(var0x -> var0x instanceof Map || var0x instanceof List)) {
                  var2.add(var6);
               }
            } else {
               var2.add(var6);
            }
         }
      }
   }

   static Object a(Map<String, Object> var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         if (!(var2 instanceof Map var7)) {
            return null;
         }

         var2 = var7.get(var6);
      }

      return var2;
   }

   static Object b(Map<String, Object> var0, String var1) {
      if (var1.endsWith("#")) {
         return a(var0, var1.substring(0, var1.length() - 1)) instanceof List var9 ? var9.size() : null;
      } else {
         int var2 = var1.indexOf("[]");
         if (var2 >= 0) {
            String var3 = var1.substring(0, var2);
            String var4 = var1.substring(var2 + 2);
            if (var4.startsWith(".")) {
               var4 = var4.substring(1);
            }

            if (a(var0, var3) instanceof List var6 && !var6.isEmpty() && var6.get(0) instanceof Map) {
               Map var7 = (Map)var6.get(0);
               return a(var7, var4);
            } else {
               return null;
            }
         } else {
            return a(var0, var1);
         }
      }
   }

   private static String b(String var0) {
      if (var0.endsWith("#")) {
         return var0.substring(0, var0.length() - 1) + " (count)";
      } else {
         return var0.contains("[]") ? var0 : c(var0);
      }
   }

   private static String a(Map<Integer, String> var0, int var1) {
      if (var0 != null && !var0.isEmpty()) {
         int var2 = -1;

         for (Integer var4 : var0.keySet()) {
            if (var4 <= var1 && var4 > var2) {
               var2 = var4;
            }
         }

         return var2 < 0 ? null : (String)var0.get(var2);
      } else {
         return null;
      }
   }

   private static boolean a(List<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow> var0, Function<io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow, Object> var1) {
      if (var0.size() < 2) {
         return false;
      } else {
         Object var2 = var1.apply((io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow)var0.getFirst());

         for (io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var4 : var0) {
            if (!Objects.equals(var2, var1.apply(var4))) {
               return true;
            }
         }

         return false;
      }
   }

   private static String a(io.karatelabs.ext.rules.RuleCoverageMatrixBuilder.CoverageRow var0, List<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : var1) {
         Object var5 = a(var0.d, var4);
         if (var5 != null && var2.size() < 3) {
            var2.add(c(var4) + " " + a(var5));
         }
      }

      return var2.isEmpty() ? "baseline" : String.join(" · ", var2);
   }

   private static String c(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 < 0 ? var0 : var0.substring(var1 + 1);
   }

   static String a(Object var0) {
      if (var0 == null) {
         return "–";
      } else {
         return !(var0 instanceof Map) && !(var0 instanceof List) ? String.valueOf(var0) : Json.stringifyStrict(var0);
      }
   }

   private static String b(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static String d(String var0) {
      return var0 == null ? "" : var0.replace("|", "\\|").replace("\n", " ");
   }

   private static String a(String var0, int var1) {
      return var0 != null && var0.length() > var1 ? var0.substring(0, var1) + "…" : var0;
   }

   static int a(String var0, String var1) {
      long[] var2 = e(var0);
      long[] var3 = e(var1);
      int var4 = Long.compare(var2[0], var3[0]);
      return var4 != 0 ? var4 : Long.compare(var2[1], var3[1]);
   }

   private static long[] e(String var0) {
      try {
         int var1 = var0.indexOf(35);
         long var2 = Long.parseLong(var0.substring(1, var1));
         long var4 = Long.parseLong(var0.substring(var1 + 1));
         return new long[]{var2, var4};
      } catch (RuntimeException var6) {
         return new long[]{Long.MAX_VALUE, Long.MAX_VALUE};
      }
   }

   private static final class CoverageRow {
      String a;
      final String b;
      final String c;
      final Map<String, Object> d;
      Map<String, Set<Boolean>> e = Map.of();
      Object f;
      Object g;
      boolean h;
      String i;
      String j;
      boolean k;

      CoverageRow(String var1, String var2, String var3, Map<String, Object> var4) {
         this.b = var1;
         this.c = var2;
         this.a = var3;
         this.d = var4;
      }
   }
}
