package io.karatelabs.ext.max.apitest;

import io.karatelabs.gherkin.FeatureSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeakeningDiff {
   private static final Pattern MARKER = Pattern.compile("#(\\?|\\w+)");

   private WeakeningDiff() {
   }

   public static boolean marked(FeatureSection var0) {
      List<io.karatelabs.gherkin.Tag> var1 = var0.isOutline() ? var0.getScenarioOutline().getTags() : var0.getScenario().getTags();
      return var1 != null
         && var1.stream()
            .anyMatch(
               var0x -> "ai".equals(var0x.getName())
                  && var0x.getValues() != null
                  && var0x.getValues().stream().anyMatch(var0xx -> "weakened".equals(var0xx.strip()))
            );
   }

   public static List<WeakeningDiff.Flag> diff(FeatureSource var0, FeatureSource var1) {
      ArrayList<io.karatelabs.ext.max.apitest.WeakeningDiff.Flag> var2 = new ArrayList();
      if (var0.feature() == null) {
         return var2;
      } else {
         io.karatelabs.ext.max.apitest.FeatureSnapshot var3 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
         if (var1.feature() != null) {
            io.karatelabs.ext.max.apitest.FeatureSnapshot var10 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var1);
            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var11 = io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a;
            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var6 = io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a;
            if (var3.d() != null) {
               io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var7 = new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(false, List.of(), null, null, List.of(), List.of());
               io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var8 = var10.d() == null ? var7 : var10.d();
               compare(var2, null, "Background", var3.d(), var8, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a);
               var11 = envs(var3.d().f(), io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a)[var3.d().f().size()];
               var6 = envs(var8.f(), io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a)[var8.f().size()];
            } else if (var10.d() != null) {
               var6 = envs(var10.d().f(), io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment.a)[var10.d().f().size()];
            }

            for (int var12 = 0; var12 < var3.e().size(); var12++) {
               io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var13 = var3.e().get(var12);
               if (var12 >= var10.e().size()) {
                  dropAll(var2, var12, var13.d(), var13, "the scenario is gone");
               } else {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var9 = var10.e().get(var12);
                  compare(var2, var12, var9.d() == null ? var13.d() : var9.d(), var13, var9, var11, var6);
               }
            }

            return var2;
         } else {
            String var4 = "the new version does not parse (" + var1.notEditable() + ")";
            if (var3.d() != null) {
               dropAll(var2, null, "Background", var3.d(), var4);
            }

            for (int var5 = 0; var5 < var3.e().size(); var5++) {
               dropAll(var2, var5, var3.e().get(var5).d(), var3.e().get(var5), var4);
            }

            return var2;
         }
      }
   }

   private static void dropAll(List<WeakeningDiff.Flag> var0, Integer var1, String var2, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var3, String var4) {
      for (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var6 : var3.f()) {
         switch (io.karatelabs.ext.max.apitest.StepClassifier.a(var6)) {
            case CHECK_MATCH:
               var0.add(new WeakeningDiff.Flag(WeakeningDiff.Kind.MATCH_DROPPED, var1, var2, var6.b(), null, var6.b() + " cannot be shown to hold: " + var4));
               break;
            case CHECK_STATUS:
               var0.add(new WeakeningDiff.Flag(WeakeningDiff.Kind.STATUS_DROPPED, var1, var2, var6.b(), null, var6.b() + " cannot be shown to hold: " + var4));
               break;
            case ADVANCED:
               if (io.karatelabs.ext.max.apitest.StepClassifier.b(var6) != io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.NONE) {
                  var0.add(
                     new WeakeningDiff.Flag(
                        WeakeningDiff.Kind.ADVANCED_STEP_TOUCHED, var1, var2, var6.b(), null, "a step whose assertion meaning is unknown is gone: " + var4
                     )
                  );
               }
         }
      }

      for (io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot var9 : var3.g()) {
         for (int var7 = 1; var7 < var9.c().size(); var7++) {
            var0.add(
               new WeakeningDiff.Flag(
                  WeakeningDiff.Kind.EXAMPLES_ROW_REMOVED,
                  var1,
                  var2,
                  row(var9.c().get(var7)),
                  null,
                  "Examples row " + row(var9.c().get(var7)) + " is gone: " + var4
               )
            );
         }
      }
   }

   private static void compare(
      List<WeakeningDiff.Flag> var0,
      Integer var1,
      String var2,
      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var3,
      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var4,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var5,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var6
   ) {
      var var7 = var3.f();
      var var8 = var4.f();
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var9 = envs(var7, var5);
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var10 = envs(var8, var6);
      int[] var11 = align(var7, var8);
      int[] var12 = ordinals(var7);
      int[] var13 = ordinals(var8);
      int[] var14 = new int[var8.size()];
      Arrays.fill(var14, -1);
      LinkedHashSet<java.lang.Integer> var15 = new LinkedHashSet();

      for (int var16 = 0; var16 < var11.length; var16++) {
         if (var11[var16] >= 0) {
            var14[var11[var16]] = var16;
         }
      }

      for (int var23 = 0; var23 < var8.size(); var23++) {
         if (var14[var23] < 0) {
            var15.add(var23);
         }
      }

      for (int var24 = 0; var24 < var7.size(); var24++) {
         if (var11[var24] < 0) {
            io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var17 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var7.get(var24);
            int var18 = var12[var24];
            switch (io.karatelabs.ext.max.apitest.StepClassifier.a(var17)) {
               case CHECK_MATCH:
                  io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var29 = io.karatelabs.ext.max.apitest.StepClassifier.d(var17);
                  Integer var31 = take(var15, var4x -> {
                     io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var5x = io.karatelabs.ext.max.apitest.StepClassifier.d((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var4x));
                     return var13[var4x] == var18 && var5x != null && var5x.a() == var29.a() && var5x.b().equals(var29.b());
                  });
                  if (var31 == null) {
                     var0.add(new WeakeningDiff.Flag(WeakeningDiff.Kind.MATCH_DROPPED, var1, var2, var17.b(), null, "the check '" + var17.b() + "' is gone"));
                  } else {
                     var14[var31] = var24;
                     io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var21 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var31);
                     String var22 = loosening(var29, io.karatelabs.ext.max.apitest.StepClassifier.d(var21));
                     var0.add(
                        var22 != null
                           ? new WeakeningDiff.Flag(
                              WeakeningDiff.Kind.MATCH_LOOSENED, var1, var2, var17.b(), var21.b(), "the check on " + var29.b() + " was loosened: " + var22
                           )
                           : new WeakeningDiff.Flag(
                              WeakeningDiff.Kind.MATCH_DROPPED,
                              var1,
                              var2,
                              var17.b(),
                              var21.b(),
                              "the check '" + var17.b() + "' was replaced by '" + var21.b() + "' — the original expectation no longer holds"
                           )
                     );
                  }
                  break;
               case CHECK_STATUS:
                  Integer var28 = take(
                     var15,
                     var3x -> var13[var3x] == var18
                        && io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var3x))
                           == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CHECK_STATUS
                  );
                  io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var30 = var28 == null ? null : (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var28);
                  if (var30 == null || !Objects.equals(var30.e(), var17.e())) {
                     var0.add(
                        new WeakeningDiff.Flag(
                           WeakeningDiff.Kind.STATUS_DROPPED,
                           var1,
                           var2,
                           var17.b(),
                           var30 == null ? null : var30.b(),
                           var30 == null ? "the check '" + var17.b() + "' is gone" : "'" + var17.b() + "' became '" + var30.b() + "'"
                        )
                     );
                  }
                  break;
               case ADVANCED:
                  if (io.karatelabs.ext.max.apitest.StepClassifier.b(var17) != io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.NONE) {
                     Integer var19 = take(
                        var15,
                        var2x -> io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var2x))
                              == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED
                           && Objects.equals(((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var2x)).d(), var17.d())
                     );
                     io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var20 = var19 == null ? null : (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var19);
                     var0.add(
                        new WeakeningDiff.Flag(
                           WeakeningDiff.Kind.ADVANCED_STEP_TOUCHED,
                           var1,
                           var2,
                           var17.b(),
                           var20 == null ? null : var20.b(),
                           "a step whose assertion meaning is unknown was " + (var20 == null ? "removed" : "changed") + ": " + var17.b()
                        )
                     );
                  }
            }
         }
      }

      for (int var26 : (Iterable<java.lang.Integer>) (Iterable<?>) (var15)) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var27 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(var26);
         if (io.karatelabs.ext.max.apitest.StepClassifier.a(var27) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED
            && io.karatelabs.ext.max.apitest.StepClassifier.b(var27) == io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.UNKNOWN) {
            var0.add(
               new WeakeningDiff.Flag(
                  WeakeningDiff.Kind.ADVANCED_STEP_TOUCHED, var1, var2, null, var27.b(), "a step whose assertion meaning is unknown was added: " + var27.b()
               )
            );
         }
      }

      dependencies(var0, var1, var2, var8, var14, var9, var10);
      requestInputs(var0, var1, var2, var7, var8, var9, var10);
      examplesRows(var0, var1, var2, var3, var4);
   }

   private static io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] envs(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var1) {
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var2 = new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[var0.size() + 1];
      var2[0] = var1;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         var2[var3 + 1] = var2[var3].a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var3));
      }

      return var2;
   }

   private static Integer take(Set<Integer> var0, IntPredicate var1) {
      for (Integer var3 : var0) {
         if (var1.test(var3)) {
            var0.remove(var3);
            return var3;
         }
      }

      return null;
   }

   private static int[] ordinals(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0) {
      int[] var1 = new int[var0.size()];
      int var2 = 0;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         var1[var3] = var2;
         if (io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var3)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD) {
            var2++;
         }
      }

      return var1;
   }

   static String loosening(io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var0, io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var1) {
      int var2 = rank(var0.c());
      int var3 = rank(var1.c());
      if (var2 >= 0 && var3 >= 0 && var3 < var2) {
         return "'" + var0.c() + "' became '" + var1.c() + "'";
      } else {
         int var4 = markers(var0.d());
         int var5 = markers(var1.d());
         if (var5 <= var4) {
            return null;
         } else {
            return var4 == 0 && isMarker(var1.d())
               ? "the value " + var0.d() + " became the wildcard " + var1.d()
               : "an equality became a shape (" + (var5 - var4) + " more '#' marker" + (var5 - var4 == 1 ? "" : "s") + ")";
         }
      }
   }

   private static int rank(String var0) {
      return switch (var0) {
         case "==" -> 3;
         case "contains only", "contains only deep" -> 2;
         case "contains", "contains deep" -> 1;
         case "contains any", "contains any deep" -> 0;
         default -> -1;
      };
   }

   private static int markers(String var0) {
      Matcher var1 = MARKER.matcher(var0);
      int var2 = 0;

      while (var1.find()) {
         var2++;
      }

      return var2;
   }

   private static boolean isMarker(String var0) {
      String var1 = var0.strip();
      if (var1.length() >= 2 && (var1.charAt(0) == '\'' || var1.charAt(0) == '"') && var1.charAt(var1.length() - 1) == var1.charAt(0)) {
         var1 = var1.substring(1, var1.length() - 1);
      }

      return var1.startsWith("#");
   }

   private static void dependencies(
      List<WeakeningDiff.Flag> var0,
      Integer var1,
      String var2,
      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var3,
      int[] var4,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var5,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var6
   ) {
      for (int var7 = 0; var7 < var3.size(); var7++) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var8 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var3.get(var7);
         io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var9 = io.karatelabs.ext.max.apitest.StepClassifier.d(var8);
         if (var9 != null) {
            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var10 = var5[var4[var7] < 0 ? var5.length - 1 : var4[var7]];
            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var11 = var6[var7];

            for (String var13 : changedProducers(io.karatelabs.ext.max.apitest.StepClassifier.b(var9.d()), var10, var11)) {
               var0.add(
                  new WeakeningDiff.Flag(
                     WeakeningDiff.Kind.EXPECTED_DEPENDENCY_CHANGED,
                     var1,
                     var2,
                     words(var10.a().get(var13)),
                     words(var11.a().get(var13)),
                     "the expected value of '" + var8.b() + "' reads " + var13 + ", whose definition " + changeWord(var10.a().get(var13), var11.a().get(var13))
                  )
               );
            }
         }
      }
   }

   private static void requestInputs(
      List<WeakeningDiff.Flag> var0,
      Integer var1,
      String var2,
      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var3,
      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var4,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var5,
      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var6
   ) {
      var var7 = calls(var3, var5);
      var var8 = calls(var4, var6);

      for (int var9 = 0; var9 < Math.min(var7.size(), var8.size()); var9++) {
         io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall var10 = (io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall)var7.get(var9);
         io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall var11 = (io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall)var8.get(var9);
         if (var10.c() || var11.c()) {
            if (!var10.b().equals(var11.b())) {
               int var12 = 0;

               while (var12 < var10.b().size() && var12 < var11.b().size() && var10.b().get(var12).equals(var11.b().get(var12))) {
                  var12++;
               }

               io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var13 = var12 < var10.b().size() ? var10.b().get(var12) : null;
               io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var14 = var12 < var11.b().size() ? var11.b().get(var12) : null;
               var0.add(
                  new WeakeningDiff.Flag(
                     WeakeningDiff.Kind.REQUEST_INPUT_CHANGED,
                     var1,
                     var2,
                     var13 == null ? null : var13.b(),
                     var14 == null ? null : var14.b(),
                     "a request input of checked call " + (var9 + 1) + " changed"
                  )
               );
            }

            LinkedHashSet var17 = new LinkedHashSet();

            for (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var20 : var11.b()) {
               var17.addAll(io.karatelabs.ext.max.apitest.StepClassifier.g(var20));
            }

            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var19 = var5[var10.a()];
            io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var21 = var6[var11.a()];

            for (String var16 : (Iterable<String>) (Iterable<?>) (changedProducers(var17, var19, var21))) {
               var0.add(
                  new WeakeningDiff.Flag(
                     WeakeningDiff.Kind.REQUEST_INPUT_CHANGED,
                     var1,
                     var2,
                     words(var19.a().get(var16)),
                     words(var21.a().get(var16)),
                     "a request input of checked call "
                        + (var9 + 1)
                        + " reads "
                        + var16
                        + ", whose definition "
                        + changeWord(var19.a().get(var16), var21.a().get(var16))
                  )
               );
            }
         }
      }
   }

   private static List<io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall> calls(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment[] var1) {
      ArrayList<io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall> var2 = new ArrayList();

      for (int var3 = 0; var3 < var0.size(); var3++) {
         if (io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var3)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD) {
            boolean var4 = false;

            for (int var5 = var3 + 1; var5 < var0.size(); var5++) {
               io.karatelabs.ext.max.apitest.StepClassifier.StepKind var6 = io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var5));
               if (var6 == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD) {
                  break;
               }

               if (var6 == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CHECK_MATCH || var6 == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CHECK_STATUS) {
                  var4 = true;
                  break;
               }
            }

            var2.add(new io.karatelabs.ext.max.apitest.WeakeningDiff.CheckedCall(var3, var1[var3].b((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var3)), var4));
         }
      }

      return var2;
   }

   private static void examplesRows(
      List<WeakeningDiff.Flag> var0, Integer var1, String var2, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var3, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var4
   ) {
      for (int var5 = 0; var5 < var3.g().size(); var5++) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot var6 = var3.g().get(var5);
         ArrayList var7 = new ArrayList();
         if (var5 < var4.g().size()) {
            var var8 = var4.g().get(var5).c();
            var7.addAll(var8.subList(Math.min(1, var8.size()), var8.size()));
         }

         for (int var10 = 1; var10 < var6.c().size(); var10++) {
            var var9 = var6.c().get(var10);
            if (!var7.remove(var9)) {
               var0.add(
                  new WeakeningDiff.Flag(
                     WeakeningDiff.Kind.EXAMPLES_ROW_REMOVED, var1, var2, row(var9), null, "Examples row " + row(var9) + " is gone from block " + var5
                  )
               );
            }
         }
      }
   }

   private static List<String> changedProducers(Set<String> var0, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var1, io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment var2) {
      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();
      var var4 = new ArrayList<>(var0);

      while (!var4.isEmpty()) {
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (String var7 : (Iterable<String>) (Iterable<?>) (var4)) {
            if (var3.add(var7)) {
               for (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var9 : concat(var1.a().get(var7), var2.a().get(var7))) {
                  for (String var11 : io.karatelabs.ext.max.apitest.StepClassifier.g(var9)) {
                     if (!var11.equals(var7)) {
                        var5.add(var11);
                     }
                  }
               }
            }
         }

         var4 = var5;
      }

      ArrayList<java.lang.String> var12 = new ArrayList();

      for (String var14 : (Iterable<String>) (Iterable<?>) (var3)) {
         var var15 = var1.a().get(var14);
         var var16 = var2.a().get(var14);
         if ((var16 != null || var15 != null) && !Objects.equals(var16, var15)) {
            var12.add(var14);
         }
      }

      return var12;
   }

   private static List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> concat(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var1) {
      ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var2 = new ArrayList();
      if (var0 != null) {
         var2.addAll(var0);
      }

      if (var1 != null) {
         var2.addAll(var1);
      }

      return var2;
   }

   private static String words(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0) {
      if (var0 != null && !var0.isEmpty()) {
         ArrayList<java.lang.String> var1 = new ArrayList();

         for (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var3 : var0) {
            var1.add(var3.b());
         }

         return String.join("; ", var1);
      } else {
         return null;
      }
   }

   private static String changeWord(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var1) {
      return var1 == null ? "is gone" : (var0 == null ? "is new" : "changed");
   }

   private static int[] align(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var1) {
      int var2 = var0.size();
      int var3 = var1.size();
      int[][] var4 = new int[var2 + 1][var3 + 1];

      for (int var5 = var2 - 1; var5 >= 0; var5--) {
         for (int var6 = var3 - 1; var6 >= 0; var6--) {
            var4[var5][var6] = ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var5)).a().equals(((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var1.get(var6)).a())
               ? var4[var5 + 1][var6 + 1] + 1
               : Math.max(var4[var5 + 1][var6], var4[var5][var6 + 1]);
         }
      }

      int[] var8 = new int[var2];
      Arrays.fill(var8, -1);
      int var9 = 0;
      int var7 = 0;

      while (var9 < var2 && var7 < var3) {
         if (((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var9)).a().equals(((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var1.get(var7)).a())) {
            var8[var9++] = var7++;
         } else if (var4[var9 + 1][var7] >= var4[var9][var7 + 1]) {
            var9++;
         } else {
            var7++;
         }
      }

      return var8;
   }

   private static String row(List<String> var0) {
      return "| " + String.join(" | ", var0) + " |";
   }

   public record Flag(WeakeningDiff.Kind kind, Integer scenarioIndex, String scenario, String before, String after, String message) {


      public Map<String, Object> toJson() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("kind", this.kind.name().toLowerCase());
         if (this.scenarioIndex != null) {
            var1.put("scenario", this.scenarioIndex);
         }

         var1.put("scenarioName", this.scenario);
         if (this.before != null) {
            var1.put("before", this.before);
         }

         if (this.after != null) {
            var1.put("after", this.after);
         }

         var1.put("message", this.message);
         return var1;
      }
   }

   public static enum Kind {
      MATCH_DROPPED,
      MATCH_LOOSENED,
      STATUS_DROPPED,
      EXPECTED_DEPENDENCY_CHANGED,
      REQUEST_INPUT_CHANGED,
      EXAMPLES_ROW_REMOVED,
      ADVANCED_STEP_TOUCHED;
   }

   private record CheckedCall(int a, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> b, boolean c) {

   }

   private record CallEnvironment(Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>> component1, io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot b, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> c) {
      static final io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment a = new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment(Map.of(), null, List.of());


      io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment a(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var1) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var2 = var1.a();
         Object var3 = this.component1;
         String var4 = io.karatelabs.ext.max.apitest.StepClassifier.c(var2);
         if (var4 != null) {
            var3 = new LinkedHashMap<>(this.component1);
            ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var5 = new ArrayList(((java.util.Collection)(((java.util.Map)var3).getOrDefault(var4, List.of()))));
            var5.add(var2);
            ((java.util.Map)var3).put(var4, var5);
         }

         switch (io.karatelabs.ext.max.apitest.StepClassifier.a(var2)) {
            case CALL_INPUT:
               if ("url".equals(var2.d())) {
                  return new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment((Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>>)var3, var2, this.c);
               }

               var var6 = new ArrayList<>(this.c);
               var6.add(var2);
               return new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment((Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>>)var3, this.b, var6);
            case CALL_METHOD:
               return new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment((Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>>)var3, this.b, List.of());
            default:
               return var3 == this.component1 ? this : new io.karatelabs.ext.max.apitest.WeakeningDiff.CallEnvironment((Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>>)var3, this.b, this.c);
         }
      }

      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> b(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var1) {
         ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var2 = new ArrayList();
         if (this.b != null) {
            var2.add(this.b);
         }

         var2.addAll(this.c);
         var2.add(var1.a());
         return var2;
      }

      public Map<String, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>> a() {
         return this.component1;
      }

      public io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot b() {
         return this.b;
      }

      public List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> c() {
         return this.c;
      }
   }
}
