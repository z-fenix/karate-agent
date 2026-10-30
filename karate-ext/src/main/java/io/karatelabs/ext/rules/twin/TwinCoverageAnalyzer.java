package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class TwinCoverageAnalyzer {
   static final String a = "(captured)";
   static final String b = "transitions";
   static final String c = "pairs";
   static final String d = "refusals";
   static final List<String> e = List.of("transitions", "pairs", "refusals");
   static final String f = "witnessed";
   static final String g = "compatible";
   static final String h = "missed";
   static final String i = "refuted";
   static final String j = "blocked";
   private static final List<String> k = List.of("witnessed", "compatible", "missed", "refuted", "blocked");

   private TwinCoverageAnalyzer() {
   }

   static Set<String> a(TwinModel var0) {
      TreeSet<java.lang.String> var1 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : var0.c()) {
         var1.addAll(var3.n());
      }

      return var1;
   }

   static String a(Object var0, Set<String> var1) {
      Object var2 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var0);

      for (String var4 : var1) {
         io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var2, io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var4), 0, "(captured)");
      }

      return io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);
   }

   static String a(String var0, Set<String> var1) {
      return var1.isEmpty() ? var0 : a(Json.parseStrict(var0, true), var1);
   }

   static io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport a(TwinModel var0, TwinWalkResult var1, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var2, List<Map<String, Object>> var3, Set<String> var4) {
      var var5 = a(var0);
      ArrayList var6 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var7 = new LinkedHashMap();
      int var8 = 0;
      ArrayList<Map<String, Object>> var9 = new ArrayList<>();
      LinkedHashMap<java.lang.String, java.util.ArrayList> var10 = new LinkedHashMap();
      LinkedHashMap<java.lang.Object, java.util.Collection> var11 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> var12 = new LinkedHashMap();
      int var13 = 0;

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var15 : var1.f()) {
         if (var15.d() == null) {
            var8++;
         } else {
            var13++;
            Object var16 = var1.e().get(var15.a()).c();
            Object var17 = var1.e().get(var15.d()).c();
            String var18 = a(var16, var5);
            String var19 = a(var17, var5);
            String var20 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var15.c());
            String var21 = var18 + "|" + var15.b() + "|" + var20 + "|" + var19;
            var10.computeIfAbsent(var21, var0x -> new ArrayList<>()).add(a(var1, var15));
            var11.computeIfAbsent(var21, var0x -> new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a)).addAll(a(var5, var16, var17));
            if (!var12.containsKey(var21)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
               var22.put("fromKey", var18);
               var22.put("command", var15.b());
               var22.put("args", io.karatelabs.ext.rules.twin.CanonicalJson.d(var15.c()));
               var22.put("toKey", var19);
               var var23 = var2.a(var15.a());
               var23.add(TwinWalker.a(var15.b(), var15.c()));
               var12.put(var21, new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation("transitions", var21, var22, var23, List.of(new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition(var18, var19)), null));
            }
         }
      }

      int var26 = var13 - var12.size();

      for (Entry var29 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
         if (((List)var29.getValue()).size() >= 2) {
            LinkedHashMap<java.lang.String, java.lang.Object> var31 = new LinkedHashMap();
            var31.put("key", var29.getKey());
            var31.put("members", var29.getValue());
            var31.put("maskedFields", new ArrayList((Collection)var11.get(var29.getKey())));
            var9.add(var31);
         }
      }

      if (var4.contains("transitions")) {
         var6.addAll(a(var12.values()));
         var7.put("transitions", var12.size());
      }

      TransitionPairIndex var28 = io.karatelabs.ext.rules.twin.TransitionPairIndex.a(var1);
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey> var30 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var34 : var28.b()) {
         var30.putIfAbsent(a(var34, var5), var34);
      }

      LinkedHashSet<java.lang.String> var33 = new LinkedHashSet();

      for (Map var37 : var3) {
         String var40 = a(io.karatelabs.ext.rules.twin.TransitionPairIndex.a(var37), var5);
         if (var30.containsKey(var40)) {
            var33.add(var40);
         }
      }

      ArrayList<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> var36 = new ArrayList();

      for (Entry var41 : (Iterable<Entry>) (Iterable<?>) (var30.entrySet())) {
         if (!var33.contains(var41.getKey())) {
            io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var43 = (io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey)var41.getValue();
            String var45 = a(var43.a(), var5);
            String var47 = a(var43.c(), var5);
            LinkedHashMap<String, Object> var24 = new LinkedHashMap();
            var24.put("fromKey", var45);
            var24.put("command1", var43.b());
            var24.put("midKey", var47);
            var24.put("command2", var43.d());
            var36.add(
               new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation("pairs", (String)var41.getKey(), var24, var28.a(var2, var43), List.of(new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition(var45, var47), new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition(var47, null)), null)
            );
         }
      }

      if (var4.contains("pairs")) {
         var6.addAll(a(var36));
         var7.put("pairs", var36.size());
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> var39 = new LinkedHashMap();
      LinkedHashMap<String, Map<String, Object>> var42 = new LinkedHashMap<>();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var46 : var1.g()) {
         String var48 = var46.b() + "|" + var46.d() + "|" + var46.e();
         if (!var39.containsKey(var48) && !var42.containsKey(var48)) {
            LinkedHashMap<String, Object> var49 = new LinkedHashMap();
            var49.put("command", var46.b());
            var49.put("stage", var46.d());
            var49.put("reason", var46.e());
            if (a(var0, var46)) {
               var42.put(var48, var49);
            } else {
               var var25 = var2.a(var46.a());
               var25.add(TwinWalker.a(var46.b(), var46.c()));
               var39.put(var48, new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation("refusals", var48, var49, var25, List.of(), b(var0, var46)));
            }
         }
      }

      if (var4.contains("refusals")) {
         var6.addAll(a(var39.values()));
         var7.put("refusals", var39.size());
      }

      return new io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport(var6, var8, var33.size(), var26, var9, new ArrayList<>(var42.values()), var7);
   }

   private static boolean a(TwinModel var0, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var1) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2 = var0.f().get(var1.b());
      return "guard".equals(var1.d()) && var2 != null && var2.c();
   }

   private static String b(TwinModel var0, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var1) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2 = var0.f().get(var1.b());
      if ("guard".equals(var1.d())) {
         return var2 == null ? null : var2.o().get("guard");
      } else {
         return "apply".equals(var1.d()) ? var1.e() : null;
      }
   }

   private static String a(io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var0, Set<String> var1) {
      return a(var0.a(), var1) + "|" + var0.b() + "|" + a(var0.c(), var1) + "|" + var0.d();
   }

   private static Map<String, Object> a(TwinWalkResult var0, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("fromKey", var0.e().get(var1.a()).b());
      var2.put("command", var1.b());
      var2.put("args", io.karatelabs.ext.rules.twin.CanonicalJson.d(var1.c()));
      var2.put("toKey", var1.e());
      return var2;
   }

   static Set<String> a(Set<String> var0, Object var1, Object var2) {
      TreeSet<java.lang.String> var3 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (String var5 : var0) {
         var var6 = io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var5);
         if (io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var1, var6, 0, null) || io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var2, var6, 0, null)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static List<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> a(Collection<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> var0) {
      var var1 = new ArrayList<>(var0);
      var1.sort((var0x, var1x) -> io.karatelabs.ext.rules.twin.CanonicalJson.a(var0x.b(), var1x.b()));
      return var1;
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var1, Set<String> var2) {
      var var3 = var1.a();
      String var4 = String.valueOf(var3.get("kind"));
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      if ("FAIL".equals(var4)) {
         var5.put("credit", "refuted");
         var5.put("at", var3.get("at"));
         var5.put("mismatch", var3.get("mismatch"));
         return var5;
      } else if (!"PASS".equals(var4)) {
         var5.put("credit", "blocked");
         var5.put("reason", var4);
         if (var3.get("code") != null) {
            var5.put("code", var3.get("code"));
         }

         return var5;
      } else {
         int var6 = var0.d().size() - 1;
         if ("refusals".equals(var0.a())) {
            var5.put("credit", a(a(var1.b(), var6), var0.f(), String.valueOf(var0.c().get("stage"))) ? "witnessed" : "missed");
            return var5;
         } else {
            ArrayList<java.lang.String> var7 = new ArrayList();
            int var8 = var6 - var0.e().size() + 1;

            for (io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition var10 : var0.e()) {
               var7.add(a(a(var1.b(), var8++), var10, var2));
            }

            var5.put("credit", var7.contains("missed") ? "missed" : (var7.contains("compatible") ? "compatible" : "witnessed"));
            return var5;
         }
      }
   }

   private static String a(Map<String, Object> var0, io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition var1, Set<String> var2) {
      var var3 = a(var0, "profileBefore");
      var var4 = a(var0, "profileAfter");
      if (var3.size() == 1 && var4.size() == 1) {
         boolean var5 = var1.a().equals(a((String)var3.get(0), var2)) && (var1.b() == null || var1.b().equals(a((String)var4.get(0), var2)));
         return var5 ? "witnessed" : "missed";
      } else {
         return !var3.isEmpty() && !var4.isEmpty() ? "compatible" : "missed";
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static boolean a(Map<String, Object> var0, String var1, String var2) {
      if (var0 != null && "refused".equals(var0.get("outcome")) && var1 != null) {
         for (Object var8 : var0.get("candidates") instanceof List<?> values ? values : List.of()) {
            if (var8 instanceof Map var9
               && Boolean.TRUE.equals(var9.get("consistent"))
               && var9.get("model") instanceof Map var6
               && "refused".equals(var6.get("kind"))
               && var2.equals(var6.get("stage"))
               && var1.equals(var6.get("reason"))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static Map<String, Object> a(List<Map<String, Object>> var0, int var1) {
      for (Map var3 : var0) {
         if ("command".equals(var3.get("kind")) && var3.get("at") instanceof Number var4 && var4.intValue() == var1) {
            return var3;
         }
      }

      return null;
   }

   private static List<String> a(Map<String, Object> var0, String var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      if (var0 != null) {
         Object var4 = var0.get(var1);
         if (var4 instanceof List) {
            for (Object var5 : (List)var4) {
               var2.add(String.valueOf(var5));
            }
         }
      }

      return var2;
   }

   static Map<String, Object> a(List<Map<String, Object>> var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (String var4 : k) {
         var2.put(var4, 0);
      }

      for (Map var7 : var0) {
         if (var1.equals(var7.get("kind"))) {
            String var5 = String.valueOf(var7.get("credit"));
            var2.put(var5, (Integer)var2.getOrDefault(var5, 0) + 1);
         }
      }

      return var2;
   }

   static List<Map<String, Object>> a(List<Map<String, Object>> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : var0) {
         if ("refusals".equals(var3.get("kind"))) {
            Map var4 = (Map)var3.get("obligation");
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap(var4);
            var5.put("credit", var3.get("credit"));
            var1.add(var5);
         }
      }

      return var1;
   }

   record CoverageTransition(String a, String b) {

      CoverageTransition(String a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   record CoverageObligation(String a, String b, Map<String, Object> c, List<Map<String, Object>> d, List<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition> e, String f) {

      CoverageObligation(String a, String b, Map<String, Object> c, List<Map<String, Object>> d, List<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageTransition> e, String f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   record CoverageReport(List<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> a, int b, int c, int d, List<Map<String, Object>> e, List<Map<String, Object>> f, Map<String, Integer> g) {

      CoverageReport(List<io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation> a, int b, int c, int d, List<Map<String, Object>> e, List<Map<String, Object>> f, Map<String, Integer> g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }
}
