package io.karatelabs.ext.requirements.jsapi;

import io.karatelabs.ext.coverage.graph.TraceGraph;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class RequirementDeltaAnalyzer {
   private RequirementDeltaAnalyzer() {
   }

   static Map<String, Object> a(Map<String, Object> var0, Map<String, Object> var1, Set<String> var2, boolean var3) {
      LinkedHashSet var4 = new LinkedHashSet();
      LinkedHashSet var5 = new LinkedHashSet();
      Map var6 = a(var0, true, var2, var4, null);
      Map var7 = a(var1, true, var2, var4, null);
      Map var8 = a(var0, false, var2, var5, var6);
      Map var9 = a(var1, false, var2, var5, var7);
      if (var2 != null) {
         a(var0, var2, var4, var5);
         a(var1, var2, var4, var5);
      }

      io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.DeltaSummaryFlags var10 = new io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.DeltaSummaryFlags();
      LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
      var11.put("criteria", a(var6, var7, var10, var4, var3));
      var11.put("requirements", a(var8, var9, var10, var5, var3));
      LinkedHashMap<java.lang.String, java.util.Map> var12 = new LinkedHashMap();
      Map var13 = a(var8);
      Map var14 = a(var9);
      var12.put("from", var13);
      var12.put("to", var14);
      var12.put("net", a(var13, var14));
      var11.put("counts", var12);
      var11.put("unchanged", !var10.d);
      var11.put("improved", (var10.a || var10.b) && !var10.c);
      return var11;
   }

   private static Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> a(
      Map<String, Object> var0, boolean var1, Set<String> var2, Set<String> var3, Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var4
   ) {
      io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex var5 = io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.a(var0);
      LinkedHashMap<java.lang.String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var6 = new LinkedHashMap();

      for (Map var8 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if ("req".equals(var8.get("source"))) {
            boolean var9 = "acceptance-criterion".equals(var8.get("kind"));
            if (var9 == var1) {
               String var10 = String.valueOf(var8.get("id"));
               String var11 = var10.startsWith("req:") ? var10.substring(4) : var10;
               if (var2 != null && !var2.contains(var10)) {
                  var3.add(var11);
               } else {
                  var var12 = a(var11, var4);
                  String var13 = var12.isEmpty() ? String.valueOf(var8.get("status")) : a(var12);
                  boolean var14 = "COVERED".equals(var13);
                  boolean var15 = var5.d(var10);
                  var6.put(
                     var11,
                     new io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState(
                        var13,
                        var14 && (var12.isEmpty() ? var15 && var5.e(var10) : var12.stream().anyMatch(io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState::b)),
                        var14 && (var12.isEmpty() ? var15 && var5.f(var10) : var12.stream().anyMatch(io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState::c)),
                        Boolean.TRUE.equals(var8.get("mockOnly")),
                        var5.a(var10),
                        var5.b(var10),
                        var5.c(var10)
                     )
                  );
               }
            }
         }
      }

      return var6;
   }

   private static void a(Map<String, Object> var0, Set<String> var1, Set<String> var2, Set<String> var3) {
      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         a(String.valueOf(var5.get("item")), var1, var2, var3);
      }

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "unmatched")) {
         if ("req".equals(var7.get("source")) && var7.get("key") != null) {
            a("req:" + var7.get("key"), var1, var2, var3);
         }
      }
   }

   private static void a(String var0, Set<String> var1, Set<String> var2, Set<String> var3) {
      if (var0.startsWith("req:") && !var1.contains(var0)) {
         String var4 = var0.substring(4);
         (var4.contains("/") ? var2 : var3).add(var4);
      }
   }

   private static List<io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> a(String var0, Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var1) {
      if (var1 == null) {
         return List.of();
      } else {
         ArrayList<io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var2 = new ArrayList();
         String var3 = var0 + "/";

         for (Entry var5 : var1.entrySet()) {
            if (((String)var5.getKey()).startsWith(var3)) {
               var2.add((io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState)var5.getValue());
            }
         }

         return var2;
      }
   }

   private static String a(List<io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var0) {
      boolean var1 = false;
      boolean var2 = false;
      boolean var3 = false;

      for (io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState var5 : var0) {
         var1 |= "FAILING".equals(var5.a());
         var2 |= "NOTCOVERED".equals(var5.a());
         var3 |= "NOTRUN".equals(var5.a());
      }

      return var1 ? "FAILING" : (var2 ? "NOTCOVERED" : (var3 ? "NOTRUN" : "COVERED"));
   }

   private static Map<String, Object> a(
      Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var0,
      Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var1,
      io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.DeltaSummaryFlags var2,
      Set<String> var3,
      boolean var4
   ) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();
      ArrayList<java.lang.String> var6 = new ArrayList();
      ArrayList<java.lang.String> var7 = new ArrayList();
      ArrayList<java.lang.String> var8 = new ArrayList();
      ArrayList<java.lang.String> var9 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var10 = new ArrayList();
      ArrayList<java.lang.String> var11 = new ArrayList();
      LinkedHashSet<java.lang.String> var12 = new LinkedHashSet();
      ArrayList<java.lang.String> var13 = new ArrayList();
      ArrayList<java.lang.String> var14 = new ArrayList();

      for (Entry var16 : var0.entrySet()) {
         String var17 = (String)var16.getKey();
         io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState var18 = (io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState)var16.getValue();
         io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState var19 = (io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState)var1.get(var17);
         if (var19 == null) {
            var14.add(var17);
         } else {
            if (!var18.e() && var19.e()) {
               var6.add(var17);
               var2.a |= var4;
            }

            if (var18.e() && !var19.e()) {
               var7.add(var17);
               var2.c |= var4;
            }

            if (!var18.a().equals(var19.a())) {
               var5.add(a(var17, var18.a(), var19.a()));
               if ("FAILING".equals(var19.a())) {
                  var12.add(var17);
               }

               if ("COVERED".equals(var18.a()) && "NOTRUN".equals(var19.a())) {
                  var12.add(var17);
               }

               if (a(var18.a(), var19.a(), var4)) {
                  var2.a = true;
               }
            }

            if (var18.b() && !var19.b() && var19.g()) {
               var8.add(var17);
               var2.b = true;
            }

            if (var18.c() && !var19.c() && var19.g()) {
               var9.add(var17);
               var2.b = true;
            }

            if (var18.d() != var19.d()) {
               var10.add(a(var17, var18.d(), Boolean.valueOf(var19.d())));
            }

            if (var18.f() && !var19.f() || var18.g() && !var19.g()) {
               var11.add(var17);
               var12.add(var17);
            }
         }
      }

      for (String var22 : var1.keySet()) {
         if (!var0.containsKey(var22)) {
            var13.add(var22);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
      var21.put("moved", var5);
      var21.put("declared", var6);
      var21.put("undeclared", var7);
      LinkedHashMap<java.lang.String, java.util.ArrayList> var23 = new LinkedHashMap();
      var23.put("oracleOnly", var8);
      var23.put("refusalOnly", var9);
      var21.put("corroborated", var23);
      var21.put("mockOnlyChanged", var10);
      var21.put("lost", var11);
      var21.put("regressed", new ArrayList(var12));
      var21.put("added", var13);
      var21.put("removed", var14);
      var21.put("retired", new ArrayList(var3));
      var2.c = var2.c | !var12.isEmpty();
      var2.d = var2.d
         | (
            !var5.isEmpty()
               || !var6.isEmpty()
               || !var7.isEmpty()
               || !var8.isEmpty()
               || !var9.isEmpty()
               || !var10.isEmpty()
               || !var11.isEmpty()
               || !var13.isEmpty()
               || !var14.isEmpty()
         );
      return var21;
   }

   private static Map<String, Object> a(String var0, Object var1, Object var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("id", var0);
      var3.put("from", var1);
      var3.put("to", var2);
      return var3;
   }

   private static boolean a(String var0, String var1, boolean var2) {
      return "COVERED".equals(var1) ? true : var2 && "NOTRUN".equals(var1) && "NOTCOVERED".equals(var0);
   }

   private static Map<String, Object> a(Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (String var3 : List.of("covered", "failing", "notrun", "notcovered")) {
         var1.put(var3, 0);
      }

      for (io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.EvidenceState var6 : var0.values()) {
         String var4 = var6.a().toLowerCase();
         if (var1.containsKey(var4)) {
            var1.put(var4, (Integer)var1.get(var4) + 1);
         }
      }

      return var1;
   }

   private static Map<String, Object> a(Map<String, Object> var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : var0.entrySet()) {
         var2.put((String)var4.getKey(), (Integer)var1.get(var4.getKey()) - (Integer)var4.getValue());
      }

      return var2;
   }

   private static final class TestEvidenceIndex {
      private final Map<String, io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.ItemEvidenceState> a = new LinkedHashMap<>();
      private final Set<String> b = new LinkedHashSet<>();
      private final Set<String> c = new LinkedHashSet<>();
      private final Set<String> d = new LinkedHashSet<>();

      static io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex a(Map<String, Object> var0) {
         LinkedHashMap<java.lang.String, java.util.Map> var1 = new LinkedHashMap();

         for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
            var1.put(String.valueOf(var3.get("id")), var3);
         }

         io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex var11 = new io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex();

         for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
            Map var5 = (Map)var1.get(String.valueOf(var4.get("test")));
            String var6 = String.valueOf(var4.get("item"));
            boolean var7 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var4.get("evidence") instanceof Map var8 ? var8 : null, b(var5));
            boolean var13 = b(var5).contains("evidence=refusal");
            boolean var14 = b(var5).contains("evidence=model");
            if (!var14) {
               io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.ItemEvidenceState var10 = var11.a.computeIfAbsent(var6, var0x -> new io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.ItemEvidenceState());
               var10.a = true;
               var10.b |= var7;
               var10.c |= !var13;
            }

            if (var7 && "DECLARED".equals(String.valueOf(var4.get("kind")))) {
               var11.b.add(var6);
            }

            if (a(var4, var5)) {
               var11.c.add(var6);
               if (var7 && !var13) {
                  var11.d.add(var6);
               }
            }
         }

         return var11;
      }

      boolean a(String var1) {
         return a(this.b, var1);
      }

      boolean b(String var1) {
         return a(this.c, var1);
      }

      boolean c(String var1) {
         return a(this.d, var1);
      }

      boolean d(String var1) {
         return this.a.containsKey(var1);
      }

      boolean e(String var1) {
         io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.ItemEvidenceState var2 = this.a.get(var1);
         return var2 == null || !var2.b;
      }

      boolean f(String var1) {
         io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.TestEvidenceIndex.ItemEvidenceState var2 = this.a.get(var1);
         return var2 == null || !var2.c;
      }

      private static boolean a(Set<String> var0, String var1) {
         if (var0.contains(var1)) {
            return true;
         } else {
            String var2 = var1 + "/";

            for (String var4 : var0) {
               if (var4.startsWith(var2)) {
                  return true;
               }
            }

            return false;
         }
      }

      private static boolean a(Map<String, Object> var0, Map<String, Object> var1) {
         if (var0.get("evidence") instanceof Map var3) {
            String var4 = String.valueOf(var3.get("status"));
            if ("pass".equals(var4) || "fail".equals(var4)) {
               return true;
            }
         }

         String var5 = var1 == null ? "PASSED" : String.valueOf(var1.get("status"));
         return "PASSED".equals(var5) || "FAILED".equals(var5);
      }

      private static List<?> b(Map<String, Object> var0) {
         return var0 != null && var0.get("tags") instanceof List var1 ? var1 : List.of();
      }

      private static final class ItemEvidenceState {
         boolean a;
         boolean b;
         boolean c;
      }
   }

   private static final class DeltaSummaryFlags {
      boolean a;
      boolean b;
      boolean c;
      boolean d;
   }

   private record EvidenceState(String a, boolean b, boolean c, boolean d, boolean e, boolean f, boolean g) {
   }
}
