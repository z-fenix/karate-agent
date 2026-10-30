package io.karatelabs.ext.max.apitest;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.StepResult;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.ext.common.KarateEventLogParser;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.Step;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Failing {
   static final int ACTUAL_MAX = 2000;

   private Failing() {
   }

   public static Map<String, Object> of(SuiteResult var0) {
      for (FeatureResult var2 : var0.getFeatureResults()) {
         if (var2.getFailedCount() != 0) {
            for (ScenarioResult var4 : var2.getScenarioResults()) {
               if (var4.isFailed()) {
                  String var5 = var4.getScenario() == null ? null : var4.getScenario().getName();
                  int var6 = OutlineRows.rowOf(var4.getScenario());
                  int var7 = var4.getScenario() != null && var4.getScenario().getSection() != null ? var4.getScenario().getSection().getIndex() : -1;

                  for (StepResult var10 : var4.getStepResults()) {
                     Step var11 = var10.getStep();
                     if (var10.getStatus() == StepResult.Status.FAILED && var11 != null && !var11.isBackground()) {
                        String var12 = var11.getKeyword() != null && !var11.getKeyword().isBlank() ? var11.getKeyword().trim() + " " : "";
                        return record(var11.getIndex(), var11.getLine(), var6, var7, var5, var12 + var11.getText(), var10.getErrorMessage());
                     }
                  }

                  return record(null, var4.getFailedStepLine(), var6, var7, var5, var4.getFailedStepText(), var4.getFailureMessage());
               }
            }
         }
      }

      return null;
   }

   public static Map<String, Object> of(Path var0, String var1) {
      for (io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var3 : io.karatelabs.ext.common.KarateEventLogParser.a(var0).b()) {
         if ("failed".equals(var3.g()) && var3.a().replace('\\', '/').endsWith(var1)) {
            int var4 = var3.j() >= 0 ? var3.j() : -1;
            int var5 = 0;

            for (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var7 : var3.i()) {
               if (!var7.j()) {
                  if ("failed".equals(var7.h())) {
                     return record(var5, null, var4, var3.k(), var3.d(), var7.a(), var7.i());
                  }

                  var5++;
               }
            }

            return record(null, null, var4, var3.k(), var3.d(), null, var3.h());
         }
      }

      return null;
   }

   public static boolean sameStep(Map<String, Object> var0, Map<String, Object> var1) {
      if (var0 != null && var1 != null) {
         Integer var2 = index(var0.get("step"));
         return var2 != null && var2.equals(index(var1.get("step"))) && sameScenario(var0, var1);
      } else {
         return false;
      }
   }

   public static boolean sameScenario(Map<String, Object> var0, Map<String, Object> var1) {
      if (var0 != null && var1 != null) {
         if (!Objects.equals(index(var0.get("row")), index(var1.get("row")))) {
            return false;
         } else {
            Integer var2 = index(var0.get("section"));
            return var2 != null ? var2.equals(index(var1.get("section"))) : var0.get("scenario") instanceof String var3 && var3.equals(var1.get("scenario"));
         }
      } else {
         return false;
      }
   }

   public static List<Map<String, Object>> passed(FeatureResult var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (ScenarioResult var3 : var0.getScenarioResults()) {
         if (var3.isPassed() && var3.getScenario() != null) {
            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            int var5 = OutlineRows.rowOf(var3.getScenario());
            if (var5 >= 0) {
               var4.put("row", var5);
            }

            if (var3.getScenario().getSection() != null) {
               var4.put("section", var3.getScenario().getSection().getIndex());
            }

            var4.put("scenario", var3.getScenario().getName() == null ? "" : var3.getScenario().getName());
            var1.add(var4);
         }
      }

      return var1;
   }

   public static Map<String, Object> identity(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (String var3 : List.of("step", "row", "section", "scenario")) {
         if (var0 != null && var0.get(var3) != null) {
            var1.put(var3, var0.get(var3));
         }
      }

      return var1;
   }

   public static Map<String, Object> identified(byte[] var0, Map<String, Object> var1) {
      if (!unidentified(var1)) {
         return var1;
      } else {
         Feature var2 = var0 == null ? null : FeatureSource.of(var0).feature();
         return var2 == null ? var1 : identified(var2, var1);
      }
   }

   private static boolean unidentified(Map<String, Object> var0) {
      return var0 != null && index(var0.get("section")) == null && !(var0.get("scenario") instanceof String);
   }

   private static Map<String, Object> identified(Feature var0, Map<String, Object> var1) {
      if (unidentified(var1) && var0.getSections().size() == 1) {
         var var2 = new LinkedHashMap<>(var1);
         var2.put("section", 0);
         return var2;
      } else {
         return var1;
      }
   }

   public static int lineOf(byte[] var0, Map<String, Object> var1) {
      Feature var2 = var1 != null && var0 != null ? FeatureSource.of(var0).feature() : null;
      if (var2 == null) {
         return -1;
      } else {
         var1 = identified(var2, var1);
         Integer var3 = index(var1.get("row"));
         Integer var4 = index(var1.get("section"));
         String var5 = var1.get("scenario") instanceof String var6 ? var6 : null;
         var var11 = var2.getSections();
         if (var4 == null) {
            if (var5 == null) {
               return -1;
            } else {
               for (FeatureSection var8 : (Iterable<FeatureSection>) (Iterable<?>) (var11)) {
                  int var9 = lineOf(var8, var3, var5);
                  if (var9 > 0) {
                     return var9;
                  }
               }

               return -1;
            }
         } else {
            return var4 >= 0 && var4 < var11.size() ? lineOf((FeatureSection)var11.get(var4), var3, null) : -1;
         }
      }
   }

   private static int lineOf(FeatureSection var0, Integer var1, String var2) {
      if (!var0.isOutline()) {
         return var1 == null && var0.getScenario() != null && (var2 == null || var2.equals(var0.getScenario().getName())) ? var0.getScenario().getLine() : -1;
      } else if (var1 == null) {
         return -1;
      } else {
         ScenarioOutline var3 = var0.getScenarioOutline();
         OutlineRows.Bound var4 = OutlineRows.bind(var3, var1);
         if (var4 != null && (var2 == null || var2.equals(var4.scenario().getName()))) {
            int var5 = 0;

            for (ExamplesTable var7 : var3.getExamplesTables()) {
               int var8 = OutlineRows.rows(var7);
               if (var1 < var5 + var8) {
                  return var7.getTable().getLineNumberForRow(var1 - var5 + 1);
               }

               var5 += var8;
            }

            return -1;
         } else {
            return -1;
         }
      }
   }

   private static Integer index(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : null;
   }

   private static Map<String, Object> record(Integer var0, Integer var1, int var2, int var3, String var4, String var5, String var6) {
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      if (var0 != null) {
         var7.put("step", var0);
      }

      if (var1 != null && var1 >= 0) {
         var7.put("line", var1);
      }

      if (var2 >= 0) {
         var7.put("row", var2);
      }

      if (var3 >= 0) {
         var7.put("section", var3);
      }

      var7.put("scenario", var4 == null ? "" : var4);
      var7.put("expected", var5 == null ? "" : var5);
      String var8 = reason(var6);
      var7.put("actual", var8.length() > 2000 ? var8.substring(0, 2000) + "…" : var8);
      return var7;
   }

   private static String reason(String var0) {
      if (var0 == null) {
         return "";
      } else {
         int var1 = var0.startsWith("#") ? var0.indexOf(10) : -1;
         return var1 > 0 ? var0.substring(var1 + 1) : var0;
      }
   }
}
