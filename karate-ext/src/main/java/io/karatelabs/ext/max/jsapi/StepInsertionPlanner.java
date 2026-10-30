package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.max.apitest.Cards;
import io.karatelabs.ext.max.apitest.FeatureSource;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Step;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StepInsertionPlanner {
   private final List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestedStepInsertion> a = new ArrayList<>();

   StepInsertionPlanner(byte[] var1, List<Map<String, Object>> var2) {
      var var3 = b(var1);

      for (Map var5 : var2) {
         int var6 = var5.get("scenario") instanceof Number var7 ? var7.intValue() : 0;
         int var15 = var5.get("at") instanceof Number var16 ? var16.intValue() : 0;
         String var17 = a(String.valueOf(var5.get("text")));
         List var18 = var6 < var3.size() ? a((FeatureSection)var3.get(var6)) : List.of();
         List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock> var10 = b(var18);
         io.karatelabs.ext.max.jsapi.StepInsertionPlanner.StepAnchor var11 = null;
         if (var15 >= 1 && var15 <= var18.size()) {
            for (int var12 = var10.size() - 1; var12 >= 0 && var11 == null; var12--) {
               io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock var13 = (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock)var10.get(var12);
               if (var13.d < var15) {
                  List var14 = var10.stream().filter(var1x -> var1x.a.equals(var13.a)).toList();
                  var11 = new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.StepAnchor(var13.a, var14.indexOf(var13), var14.size(), var13.b);
               }
            }
         }

         this.a.add(new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestedStepInsertion(var5, var6, var11, var17));
      }
   }

   List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> a(byte[] var1) {
      var var2 = b(var1);
      ArrayList<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var3 = new ArrayList();

      for (int var4 = 0; var4 < this.a.size(); var4++) {
         io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestedStepInsertion var5 = this.a.get(var4);
         List var6 = var5.b < var2.size() ? a((FeatureSection)var2.get(var5.b)) : List.of();
         io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock var7 = a(b(var6), var5.c);
         if (var7 == null) {
            var3.add(
               new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion(
                  var5.a, var5.b, -1, String.valueOf(var5.a.get("text")), var5.c == null ? null : var5.c.a(), var5.c == null ? null : var5.c.e()
               )
            );
         } else if (a(var6, var7, var5.d) < 0) {
            int var8 = var7.e;

            for (int var9 = var4 + 1; var9 < this.a.size(); var9++) {
               io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestedStepInsertion var10 = this.a.get(var9);
               int var11 = var10.b == var5.b && Objects.equals(var10.c, var5.c) ? a(var6, var7, var10.d) : -1;
               var8 = var11 >= 0 ? Math.min(var8, var11) : var8;
            }

            var3.add(new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion(var5.a, var5.b, var8, String.valueOf(var5.a.get("text")), var5.c.a(), var5.c.e()));
         }
      }

      ArrayList<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var12 = new ArrayList(var3);
      Collections.reverse(var12);
      var12.sort(Comparator.comparingInt(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion::b).thenComparing(var0 -> -var0.c()));
      return var12;
   }

   static List<String> a(String var0, List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion var4 : var1) {
         if (var4.c() >= 0) {
            var2.add(
               "ApiTest.insertStep('"
                  + var0
                  + "', {scenario: "
                  + var4.b()
                  + ", at: "
                  + var4.c()
                  + ", text: "
                  + StringUtils.formatJsonString(var4.d())
                  + ", ifMatch})"
            );
         }
      }

      for (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion var6 : var1) {
         if (var6.c() < 0) {
            var2.add(
               (var6.e() == null ? "its call" : var6.e())
                  + " is not found once in the test: place "
                  + StringUtils.formatJsonString(var6.d())
                  + " after that call's method step yourself, or skip it"
            );
         }
      }

      return var2;
   }

   static List<Map<String, Object>> a(List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion var3 : var0) {
         var var4 = new LinkedHashMap<>(var3.a());
         var4.put("at", var3.c());
         var1.add(var4);
      }

      return var1;
   }

   private static io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock a(List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock> var0, io.karatelabs.ext.max.jsapi.StepInsertionPlanner.StepAnchor var1) {
      if (var1 == null) {
         return null;
      } else {
         var var2 = var0.stream().filter(var1x -> var1x.a.equals(var1.b)).toList();
         if (var2.size() == var1.d) {
            return (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock)var2.get(var1.c);
         } else {
            var var3 = var2.stream().filter(var1x -> var1.e != null && var1.e.equals(var1x.b)).toList();
            return var3.size() == 1 ? (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock)var3.get(0) : null;
         }
      }
   }

   private static int a(List<Step> var0, io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock var1, String var2) {
      for (int var3 = var1.d + 1; var3 < var1.e; var3++) {
         if (var2.equals(b((Step)var0.get(var3)))) {
            return var3;
         }
      }

      return -1;
   }

   private static List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock> b(List<Step> var0) {
      ArrayList<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock> var1 = new ArrayList();
      int var2 = -1;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         String var4 = ((Step)var0.get(var3)).getKeyword();
         if (var2 < 0 && Cards.callInput(var4)) {
            var2 = var3;
         }

         if ("method".equals(var4)) {
            int var5 = var2 < 0 ? var3 : var2;
            if (!var1.isEmpty()) {
               io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock var6 = (io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock)var1.remove(var1.size() - 1);
               var1.add(new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock(var6.a, var6.b, var6.c, var6.d, var5));
            }

            var1.add(new io.karatelabs.ext.max.jsapi.StepInsertionPlanner.RequestStepBlock(a(var0, var5, var3), c((Step)var0.get(var5)), var5, var3, var0.size()));
            var2 = -1;
         }
      }

      return var1;
   }

   private static String a(List<Step> var0, int var1, int var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();
      String var4 = "";

      for (int var5 = var1; var5 < var2; var5++) {
         Step var6 = (Step)var0.get(var5);
         if ("path".equals(var6.getKeyword())) {
            var3.add(a(var6));
         } else if ("url".equals(var6.getKeyword())) {
            var4 = a(var6);
         }
      }

      return a((Step)var0.get(var2)).toLowerCase() + " " + (var3.isEmpty() ? var4 : String.join(", ", var3));
   }

   private static String a(Step var0) {
      return var0.getText() == null ? "" : var0.getText().strip();
   }

   private static List<FeatureSection> b(byte[] var0) {
      try {
         return var0 == null ? List.of() : FeatureSource.of(var0).feature().getSections();
      } catch (RuntimeException var2) {
         return List.of();
      }
   }

   private static List<Step> a(FeatureSection var0) {
      List var1 = var0.isOutline() ? var0.getScenarioOutline().getSteps() : var0.getScenario().getSteps();
      return var1 == null ? List.of() : var1;
   }

   private static String b(Step var0) {
      return ((var0.getKeyword() == null ? "" : var0.getKeyword() + " ") + var0.getText()).strip();
   }

   private static String a(String var0) {
      return var0.strip().replaceFirst("^(Given|When|Then|And|But|\\*)\\s+", "");
   }

   private static String c(Step var0) {
      var var1 = var0.getComments();
      return var1 != null && !var1.isEmpty() ? ((String)var1.get(var1.size() - 1)).replaceFirst("^\\s*#\\s*", "").strip() : null;
   }

   private record RequestedStepInsertion(Map<String, Object> a, int b, io.karatelabs.ext.max.jsapi.StepInsertionPlanner.StepAnchor c, String d) {

   }

   private record RequestStepBlock(String a, String b, int c, int d, int e) {

   }

   private record StepAnchor(String b, int c, int d, String e) {


      String a() {
         String var1 = this.b.substring(0, this.b.indexOf(32)).toUpperCase() + this.b.substring(this.b.indexOf(32));
         return this.d > 1 ? var1 + " (" + (this.c + 1) + " of " + this.d + " so written)" : var1;
      }

      public String b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }
   }

   record PlannedStepInsertion(Map<String, Object> a, int b, int c, String d, String e, String f) {

   }
}
