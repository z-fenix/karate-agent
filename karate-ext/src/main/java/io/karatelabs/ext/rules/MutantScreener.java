package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MutantScreener {
   private MutantScreener() {
   }

   public static io.karatelabs.ext.rules.MutantScreener.ScreeningDeck a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      ArrayList<Map<String, Object>> var1 = new ArrayList<>();

      for (Map<String, Object> var3 : var0.b()) {
         LinkedHashMap<String, Object> var4 = new LinkedHashMap<>();
         var3.forEach((var1x, var2) -> {
            if (!var1x.startsWith("_")) {
               var4.put(var1x, var2);
            }
         });
         var1.add(var4);
      }

      LinkedHashSet var12 = new LinkedHashSet();
      List var13 = RuleInputSpaceExplorer.b(var0.g(), var0.m().e(), var0.e(), var0.j(), var1);

      for (int var14 = 0; var14 < var13.size(); var14++) {
         if (!((List)var13.get(var14)).isEmpty()) {
            var12.add(var14);
         }
      }

      io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var15 = RuleInputSpaceExplorer.a(var0.g(), var0.m().e(), var0.e(), var0.j(), var0.b(), "pairwise", null, 512, 2, null);
      Object var5 = var15.a().get("suite");
      if (var5 instanceof List) {
         for (Object var8 : (List)var5) {
            if (var8 instanceof Map var9 && var9.get("input") instanceof Map var10) {
               if ("out".equals(var9.get("domain"))) {
                  var12.add(var1.size());
               }

               var1.add(var10);
            }
         }
      }

      return new io.karatelabs.ext.rules.MutantScreener.ScreeningDeck(var1, var12);
   }

   public static List<Map<String, Object>> b(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      return a(var0).a();
   }

   public static List<String> a(String var0, List<Map<String, Object>> var1, RuleEvaluator var2) {
      return a(var0, Map.of(), var1, var2);
   }

   public static List<String> a(String var0, Map<String, Object> var1, List<Map<String, Object>> var2, RuleEvaluator var3) {
      ArrayList var4 = new ArrayList(var2.size());

      for (Map var6 : var2) {
         var4.add(a(var0, var1, var6, var3));
      }

      return var4;
   }

   public static io.karatelabs.ext.rules.MutantScreener.ScreeningResult a(io.karatelabs.ext.rules.MutationGenerator.Mutant var0, List<Map<String, Object>> var1, List<String> var2, RuleEvaluator var3) {
      return a(var0, var1, var2, var3, Set.of());
   }

   public static io.karatelabs.ext.rules.MutantScreener.ScreeningResult a(io.karatelabs.ext.rules.MutationGenerator.Mutant var0, List<Map<String, Object>> var1, List<String> var2, RuleEvaluator var3, Set<Integer> var4) {
      Integer var5 = null;
      String var6 = null;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         String var8 = a(var0.m(), var0.b(), (Map<String, Object>)var1.get(var7), var3);
         if (!var8.equals(var2.get(var7))) {
            String var9 = "row " + var7 + ": " + (String)var2.get(var7) + " -> " + var8;
            if (!var4.contains(var7)) {
               return new io.karatelabs.ext.rules.MutantScreener.ScreeningResult(var0.d(), false, var7 + 1, var7, var9, false);
            }

            if (var5 == null) {
               var5 = var7;
               var6 = var9;
            }
         }
      }

      return var5 == null ? new io.karatelabs.ext.rules.MutantScreener.ScreeningResult(var0.d(), true, var1.size(), null, null, false) : new io.karatelabs.ext.rules.MutantScreener.ScreeningResult(var0.d(), false, var1.size(), var5, var6, true);
   }

   static String a(String var0, Map<String, Object> var1, Map<String, Object> var2, RuleEvaluator var3) {
      LinkedHashMap var4 = new LinkedHashMap();

      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var5 = var3.a(var0, var1, var2, null);
         var4.put("output", var5.b());
         var4.put("outcome", var5.g() ? var5.f() : null);
         var4.put("threw", false);
      } catch (RuntimeException var6) {
         var4.put("output", null);
         var4.put("outcome", null);
         var4.put("threw", true);
      }

      return Json.stringifyStrict(var4);
   }

   public record ScreeningDeck(List<Map<String, Object>> a, Set<Integer> b) {

      public ScreeningDeck(List<Map<String, Object>> a, Set<Integer> b) {
         this.a = a;
         this.b = b;
      }
   }

   public record ScreeningResult(String b, boolean c, int d, Integer e, String f, boolean g) {

      public ScreeningResult(String b, boolean c, int d, Integer e, String f, boolean g) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }

      public Map<String, Object> a() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("mutantId", this.b);
         var1.put("screened", this.c);
         var1.put("rowsCompared", this.d);
         if (this.e != null) {
            var1.put("divergingRow", this.e);
            var1.put("divergence", this.f);
         }

         return var1;
      }

      public String b() {
         return this.b;
      }

      public boolean c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public Integer e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }
   }
}
