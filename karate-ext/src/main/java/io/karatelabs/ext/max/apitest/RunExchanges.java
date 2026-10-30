package io.karatelabs.ext.max.apitest;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.KarateEventLogParser;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Step;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RunExchanges {
   static final int BODY_CAP = 65536;
   private static final int SCAN = 50;
   private static final Pattern MASK = Pattern.compile("‹(?:masked|secret:[^›]*)›");

   private RunExchanges() {
   }

   public static Path newest(Path var0, String var1) {
      if (!Files.isDirectory(var0)) {
         return null;
      } else {
         List var2;
         try (var var3 = Files.list(var0)) {
            var2 = var3.<Path>map(var0x -> var0x.resolve("karate-json/karate-events.jsonl"))
               .filter(var0x -> Files.isRegularFile(var0x))
               .sorted(Comparator.comparingLong(RunExchanges::modified).reversed())
               .limit(50L)
               .toList();
         } catch (IOException var8) {
            return null;
         }

         for (Path var4 : (Iterable<Path>) (Iterable<?>) (var2)) {
            if (io.karatelabs.ext.common.KarateEventLogParser.a(var4).b().stream().anyMatch(var1x -> sameFeature(var1x.a(), var1))) {
               return var4.getParent().getParent();
            }
         }

         return null;
      }
   }

   public static List<Map<String, Object>> read(Path var0, String var1, FeatureSource var2, Cards.FeatureView var3) {
      ArrayList var4 = new ArrayList();
      if (var2.feature() == null) {
         return var4;
      } else {
         var var5 = var2.feature().getSections();

         for (io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var7 : io.karatelabs.ext.common.KarateEventLogParser.a(var0).b()) {
            if (sameFeature(var7.a(), var1)) {
               int var8 = sectionOf(var5, var7.e());
               if (var8 >= 0 && var8 < var3.flows().size()) {
                  Cards.Flow var9 = var3.flows().get(var8);
                  LinkedHashMap var10 = new LinkedHashMap();
                  var10.put("index", var8);
                  var10.put("name", var7.d());
                  if (var9.outline() && var7.j() >= 0 && ((FeatureSection)var5.get(var8)).isOutline()) {
                     var10.put("row", OutlineRows.examplesRow(((FeatureSection)var5.get(var8)).getScenarioOutline(), var7.e(), var7.j()));
                  }

                  var10.put("status", var7.g());
                  if (var7.h() != null) {
                     var10.put("error", var7.h());
                  }

                  var var11 = var7.i().stream().filter(io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult::j).toList();
                  var var12 = var7.i().stream().filter(var0x -> !var0x.j()).toList();
                  if (changed(
                     var2, (FeatureSection)var5.get(var8), var10.get("row") instanceof Integer var13 ? var13 : -1, var11, var12, "passed".equals(var7.g())
                  )) {
                     var10.put("changed", true);
                  }

                  if (var3.background() != null && !var11.isEmpty()) {
                     var10.put("background", recorded(var3.background().cards(), var11));
                  }

                  var var15 = recorded(var9.cards(), var12);
                  var10.put("cards", var15);
                  ArrayList var16 = new ArrayList();
                  if ("passed".equals(var7.g()) && var9.checks().notChecked()) {
                     var16.add("notasserted");
                  }

                  if ("passed".equals(var7.g()) && WeakeningDiff.marked((FeatureSection)var5.get(var8))) {
                     var16.add("weakened");
                  }

                  if (mockOnly(var10)) {
                     var16.add("mockOnly");
                  }

                  if (!var16.isEmpty()) {
                     var10.put("qualifiers", var16);
                  }

                  var4.add(var10);
               }
            }
         }

         return var4;
      }
   }

   private static boolean changed(FeatureSource var0, FeatureSection var1, int var2, List<io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult> var3, List<io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult> var4, boolean var5) {
      List var6 = var0.feature().getBackground() != null && var0.feature().getBackground().getSteps() != null
         ? var0.feature().getBackground().getSteps()
         : List.of();
      boolean var8 = true;
      List var7;
      if (var1.isOutline()) {
         OutlineRows.Bound var9 = OutlineRows.bind(var1.getScenarioOutline(), var2);
         var8 = var9 != null;
         var7 = var9 != null ? var9.scenario().getSteps() : var1.getScenarioOutline().getSteps();
      } else {
         var7 = var1.getScenario().getSteps();
      }

      return differs(var6, var3, true, var5) || differs(var7, var4, var8, var5);
   }

   private static boolean differs(List<Step> var0, List<io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult> var1, boolean var2, boolean var3) {
      if (var1.size() <= var0.size() && (!var3 || var1.size() == var0.size())) {
         for (int var4 = 0; var4 < var1.size(); var4++) {
            io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var5 = (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult)var1.get(var4);
            Step var6 = (Step)var0.get(var4);
            if (!Objects.equals(strip(var6.getKeyword()), strip(var5.d()))) {
               return true;
            }

            if (var2
               && (
                  !same(strip(var6.getText()), strip(var5.e()))
                     || !same(var6.getDocString(), var5.f())
                     || !sameRows(var6.getTable() == null ? null : var6.getTable().getRows(), var5.g())
               )) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean same(String var0, String var1) {
      if (Objects.equals(var0, var1)) {
         return true;
      } else if (var0 != null && var1 != null && MASK.matcher(var1).find()) {
         StringBuilder var2 = new StringBuilder();
         Matcher var3 = MASK.matcher(var1);

         int var4;
         for (var4 = 0; var3.find(); var4 = var3.end()) {
            var2.append(Pattern.quote(var1.substring(var4, var3.start()))).append("(?s:.+?)");
         }

         return Pattern.matches(var2.append(Pattern.quote(var1.substring(var4))).toString(), var0);
      } else {
         return false;
      }
   }

   private static boolean sameRows(List<List<String>> var0, List<List<String>> var1) {
      if (var0 != null && var1 != null && var0.size() == var1.size()) {
         for (int var2 = 0; var2 < var0.size(); var2++) {
            List var3 = (List)var0.get(var2);
            List var4 = (List)var1.get(var2);
            if (var3.size() != var4.size()) {
               return false;
            }

            for (int var5 = 0; var5 < var3.size(); var5++) {
               if (!same((String)var3.get(var5), (String)var4.get(var5))) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return Objects.equals(var0, var1);
      }
   }

   private static String strip(String var0) {
      return var0 != null && !var0.isBlank() ? var0.strip() : null;
   }

   private static List<Map<String, Object>> recorded(List<Cards.Card> var0, List<io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult> var1) {
      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var4 : var1) {
         var2.add(new io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome(var4.h(), var4.i(), var4.k()));
      }

      return cards(var0, var2);
   }

   static List<Map<String, Object>> cards(List<Cards.Card> var0, List<io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome> var1) {
      ArrayList var2 = new ArrayList();
      LinkedHashMap var3 = null;

      for (Cards.Card var5 : var0) {
         if (var5.kind() == Cards.Kind.DATA_TABLE) {
            var3 = null;
         } else {
            int var6 = var5.source().firstStep();
            int var7 = var5.source().lastStep();
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("step", var6);
            var8.put("end", var7);
            String var9 = null;
            String var10 = null;
            ArrayList var11 = new ArrayList();

            for (int var12 = var6; var12 <= var7 && var12 < var1.size(); var12++) {
               io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome var13 = (io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome)var1.get(var12);
               if (var13 != null) {
                  var9 = worse(var9, var13.a());
                  if (var10 == null && var13.b() != null) {
                     var10 = var13.b();
                  }

                  for (Map var15 : var13.c()) {
                     var11.add(exchange(var15));
                  }
               }
            }

            var8.put("status", var9 == null ? "skipped" : var9);
            if (var10 != null) {
               var8.put("error", var10);
            }

            if (!var11.isEmpty()) {
               var8.put("exchanges", var11);
            }

            if (var5.kind() == Cards.Kind.CALL && var5.detail() instanceof Cards.Call var16 && var16.method() != null) {
               var3 = var8;
               var8.put("verdict", var8.get("status"));
            } else if ((var5.kind() == Cards.Kind.CHECK || var5.kind() == Cards.Kind.REMEMBER) && var3 != null) {
               var3.put("verdict", worse((String)var3.get("verdict"), (String)var8.get("status")));
            } else {
               var3 = null;
            }

            var2.add(var8);
         }
      }

      return var2;
   }

   private static Map<String, Object> exchange(Map<String, Object> var0) {
      Map var1 = var0.get("request") instanceof Map var2 ? var2 : Map.of();
      Map var6 = var0.get("response") instanceof Map var7 ? var7 : Map.of();
      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("method", var1.get("method") == null ? null : String.valueOf(var1.get("method")).toUpperCase());
      var8.put("url", var1.get("url"));
      var8.put("status", var6.get("status"));
      if (var6.get("responseTime") instanceof Number var9) {
         var8.put("ms", var9.longValue());
      }

      if (mockServed(var6)) {
         var8.put("mock", true);
      }

      var8.put("request", side(var1));
      var8.put("response", side(var6));
      return var8;
   }

   private static Map<String, Object> side(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("headers", var0.get("headers") == null ? Map.of() : var0.get("headers"));
      Object var2 = var0.get("body");
      if (var2 != null) {
         String var3 = var2 instanceof String var4 ? var4 : Json.stringifyStrict(var2);
         if (var3.length() > 65536) {
            var1.put("body", var3.substring(0, 65536));
            var1.put("truncated", var3.length());
         } else {
            var1.put("body", var2);
         }
      }

      return var1;
   }

   private static boolean mockServed(Map<String, Object> var0) {
      if (var0.get("headers") instanceof Map var1) {
         for (Entry var3 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if ("Karate-Mock".equalsIgnoreCase(String.valueOf(var3.getKey()))) {
               Object var4 = var3.getValue();
               Object var5 = var4 instanceof List var6 ? (var6.isEmpty() ? null : var6.get(0)) : var4;
               return "true".equalsIgnoreCase(String.valueOf(var5));
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean mockOnly(Map<String, Object> var0) {
      boolean var1 = false;

      for (String var3 : List.of("background", "cards")) {
         Object var5 = var0.get(var3);
         if (var5 instanceof List) {
            for (Object var6 : (List)var5) {
               Object var8 = ((Map)var6).get("exchanges");
               if (var8 instanceof List) {
                  for (Object var9 : (List)var8) {
                     if (!Boolean.TRUE.equals(((Map)var9).get("mock"))) {
                        return false;
                     }

                     var1 = true;
                  }
               }
            }
         }
      }

      return var1;
   }

   private static String worse(String var0, String var1) {
      return rank(var1) > rank(var0) ? var1 : var0;
   }

   private static int rank(String var0) {
      if (var0 == null) {
         return -1;
      } else {
         return switch (var0) {
            case "failed" -> 3;
            case "skipped" -> 1;
            case "passed" -> 0;
            default -> 2;
         };
      }
   }

   private static int sectionOf(List<FeatureSection> var0, int var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         if (((FeatureSection)var0.get(var3)).getLine() <= var1) {
            var2 = var3;
         }
      }

      return var2;
   }

   static boolean sameFeature(String var0, String var1) {
      String var2 = var0.replace('\\', '/');
      if (var2.startsWith("classpath:")) {
         var2 = var2.substring("classpath:".length());
      }

      int var3 = var2.indexOf(".feature:");
      if (var3 >= 0) {
         var2 = var2.substring(0, var3 + ".feature".length());
      }

      var2 = var2.replaceFirst("^/+", "");
      return var2.equals(var1) || var2.endsWith("/" + var1);
   }

   private static long modified(Path var0) {
      try {
         return Files.getLastModifiedTime(var0).toMillis();
      } catch (IOException var2) {
         return 0L;
      }
   }
}
