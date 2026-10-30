package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.KarateEventLogParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RunEvidenceInspector {
   private static final Logger a = LoggerFactory.getLogger(RunEvidenceInspector.class);
   private static final int b = 500;
   private static final int c = 360;

   private RunEvidenceInspector() {
   }

   static List<Map<String, Object>> a(Path var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      a(var0, var1x -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var2.put("name", var1x.b());
         var2.put("passed", var1x.c());
         if (var1x.g() >= 0L) {
            var2.put("durationMs", var1x.g());
         }

         if (var1x.d()) {
            var2.put("skipped", true);
         }

         if (var1x.e() != null) {
            var2.put("error", c(var1x.e()));
         }

         var1.add(var2);
         return var1.size() < 500;
      });
      return var1;
   }

   static List<Map<String, Object>> a(Path var0, List<Map<String, Object>> var1) {
      if (var1 != null && !var1.isEmpty()) {
         ArrayList<io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate> var2 = new ArrayList();
         LinkedHashMap<java.lang.String, io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate> var3 = new LinkedHashMap();

         for (Map var5 : var1) {
            io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate var6 = new io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate(String.valueOf(var5.get("path")), String.valueOf(var5.get("kind")));
            var2.add(var6);
            var3.putIfAbsent(a(var6.a), var6);
         }

         a(var0, var1x -> {
            if (var1x.f() == 0) {
               String var2x = var1x.a() != null && !var1x.a().isBlank() ? var1x.a() : var1x.b();
               io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate var3x = (io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate)var3.get(a(var2x));
               if (var3x != null) {
                  var3x.a(var1x);
               }
            }

            return true;
         });
         return var2.stream().map(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ResourceOutcomeAggregate::a).toList();
      } else {
         return List.of();
      }
   }

   static String a(String var0) {
      String var1 = var0.replace('\\', '/');
      if (var1.startsWith("classpath:")) {
         var1 = var1.substring("classpath:".length());
      }

      int var2 = var1.indexOf(".feature:");
      return var2 < 0 ? var1 : var1.substring(0, var2 + ".feature".length());
   }

   static io.karatelabs.ext.max.jsapi.RunEvidenceInspector.CheckEvidenceSummary b(Path var0) {
      io.karatelabs.ext.max.jsapi.RunEvidenceInspector.EvidenceOutcomeCollector var1 = new io.karatelabs.ext.max.jsapi.RunEvidenceInspector.EvidenceOutcomeCollector();
      Path var2 = var0.resolve("transcript.jsonl");
      if (Files.isRegularFile(var2)) {
         a(
            var2,
            null,
            var1x -> {
               Object var2x = var1x.get("type");
               if ("assertion".equals(var2x) && var1x.get("label") instanceof String var3 && !var3.isBlank()) {
                  var1.a(var3, var1x.get("actual"), Boolean.TRUE.equals(var1x.get("pass")));
               } else if ("step".equals(var2x)
                  && var1x.get("name") instanceof String var4
                  && ("pass".equals(var1x.get("status")) || "fail".equals(var1x.get("status")))) {
                  var1.a(var4, null, "pass".equals(var1x.get("status")));
               }
            }
         );
      } else {
         a(var0.resolve("karate-json/karate-events.jsonl"), "\"FEATURE_EXIT\"", var1x -> {
            if ("FEATURE_EXIT".equals(var1x.get("type")) && var1x.get("data") instanceof Map var2x && var2x.get("scenarioResults") instanceof List var3) {
               for (Object var5 : var3) {
                  if (var5 instanceof Map var6) {
                     Object var8 = var6.get("stepResults");
                     if (var8 instanceof List) {
                        for (Object var9 : (List)var8) {
                           if (var9 instanceof Map var10) {
                              a(var10, var1);
                           }
                        }
                     }
                  }
               }
            }
         });
      }

      return new io.karatelabs.ext.max.jsapi.RunEvidenceInspector.CheckEvidenceSummary(var1.a, var1.c, var1.d);
   }

   private static void a(Map<?, ?> var0, io.karatelabs.ext.max.jsapi.RunEvidenceInspector.EvidenceOutcomeCollector var1) {
      Map<java.lang.String, Object> var2 = var0.get("step") instanceof Map var3 ? var3 : Map.of();
      boolean var12 = false;
      Object var5 = var0.get("embeds");
      if (var5 instanceof List) {
         for (Object var6 : (List)var5) {
            if (var6 instanceof Map var7 && "evidence".equals(var7.get("name"))) {
               Map<java.lang.String, Object> var8 = a(var7);
               if (var8 != null && "assertion".equals(var8.get("kind"))) {
                  var12 = true;
                  String var9 = var8.get("label") instanceof String var10 && !var10.isBlank() ? var10 : ("step".equals(var8.get("source")) ? b(var2) : null);
                  if (var9 != null) {
                     var1.b(var9, var8.get("actual"), Boolean.TRUE.equals(var8.get("pass")));
                  }
               }
            }
         }
      }

      Object var14 = var0.get("result") instanceof Map var16 ? var16.get("status") : null;
      if (!var12 && ("match".equals(var2.get("keyword")) || "assert".equals(var2.get("keyword"))) && ("passed".equals(var14) || "failed".equals(var14))) {
         var1.b(b(var2), null, "passed".equals(var14));
      }
   }

   private static Map<?, ?> a(Map<String, Object> var0) {
      try {
         String var1 = new String(Base64.getDecoder().decode(io.karatelabs.ext.common.KarateEventLogParser.a(var0)), StandardCharsets.UTF_8);
         return Json.of(var1).value() instanceof Map var2 ? var2 : null;
      } catch (RuntimeException var4) {
         return null;
      }
   }

   private static String b(Map<?, ?> var0) {
      String var1 = var0.get("keyword") instanceof String var2 ? var2 : "";
      String var5 = var0.get("text") instanceof String var6 ? var6.trim() : "";
      return !var1.isEmpty() && !var5.startsWith(var1 + " ") ? var1 + " " + var5 : var5;
   }

   private static Object a(Object var0) {
      String var1;
      try {
         var1 = Json.stringifyStrict(var0);
      } catch (RuntimeException var3) {
         var1 = String.valueOf(var0);
      }

      return var1.length() > 360 ? var1.substring(0, 360) + " …(truncated)" : var0;
   }

   private static void a(Path var0, String var1, Consumer<Map<String, Object>> var2) {
      if (Files.isRegularFile(var0)) {
         try (var var3 = Files.lines(var0)) {
            for (String var5 : (Iterable<String>) (var3::iterator)) {
               if (!var5.isBlank() && (var1 == null || var5.contains(var1))) {
                  try {
                     if (Json.of(var5).value() instanceof Map var6) {
                        var2.accept(var6);
                     }
                  } catch (RuntimeException var9) {
                  }
               }
            }
         } catch (Exception var11) {
            a.debug("could not read checks for {}: {}", var0, var11.getMessage());
         }
      }
   }

   private static void a(Path var0, Predicate<io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ScenarioExitEvidence> var1) {
      Path var2 = var0.resolve("karate-json/karate-events.jsonl");
      if (Files.isRegularFile(var2)) {
         try (var var3 = Files.lines(var2)) {
            for (String var5 : (Iterable<String>) (var3::iterator)) {
               if (!var5.isBlank() && var5.contains("\"SCENARIO_EXIT\"")) {
                  try {
                     if (Json.of(var5).value() instanceof Map var6 && "SCENARIO_EXIT".equals(var6.get("type")) && var6.get("data") instanceof Map var7) {
                        io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ScenarioExitEvidence var16 = new io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ScenarioExitEvidence(
                           var7.get("feature") == null ? null : String.valueOf(var7.get("feature")),
                           var7.get("name") == null ? null : String.valueOf(var7.get("name")),
                           Boolean.TRUE.equals(var7.get("passed")),
                           Boolean.TRUE.equals(var7.get("skipped")),
                           c(var7),
                           var7.get("callDepth") instanceof Number var9 ? var9.intValue() : 0,
                           var7.get("durationMillis") instanceof Number var17 ? var17.longValue() : -1L
                        );
                        if (!var1.test(var16)) {
                           return;
                        }
                     }
                  } catch (RuntimeException var12) {
                  }
               }
            }
         } catch (Exception var14) {
            a.debug("could not read verdicts for {}: {}", var0, var14.getMessage());
         }
      }
   }

   private static String c(Map<?, ?> var0) {
      Object var1 = var0.get("errorReason") != null ? var0.get("errorReason") : var0.get("error");
      return var1 == null ? null : String.valueOf(var1);
   }

   private static String c(String var0) {
      String var1 = b(var0);
      return Runner.withBootHint(var1.length() > 360 ? var1.substring(0, 360) + " …(truncated)" : var1);
   }

   static String b(String var0) {
      if (!var0.contains("js failed:")) {
         return var0;
      } else {
         String[] var1 = var0.split("\n", -1);
         int var2 = -1;

         for (int var3 = 0; var3 < var1.length; var3++) {
            if (var1[var3].trim().startsWith("Error:")) {
               var2 = var3;
            }
         }

         if (var2 < 0) {
            return var0;
         } else {
            StringBuilder var5 = new StringBuilder(var1[var2].trim().substring("Error:".length()).trim());

            for (int var4 = var2 + 1; var4 < var1.length && !var1[var4].trim().startsWith("=========="); var4++) {
               var5.append('\n').append(var1[var4]);
            }

            for (int var6 = var2 - 1; var6 >= 0 && !var1[var6].trim().startsWith("=========="); var6--) {
               if (var1[var6].trim().startsWith("Code:")) {
                  var5.append('\n').append(var1[var6]);
                  break;
               }
            }

            return var5.toString();
         }
      }
   }

   record CheckEvidenceSummary(List<Map<String, Object>> a, int b, int c) {

   }

   private record ScenarioExitEvidence(String a, String b, boolean c, boolean d, String e, int f, long g) {

   }

   private static final class ResourceOutcomeAggregate {
      private final String a;
      private final String b;
      private int c;
      private boolean d;
      private boolean e = true;
      private String f;
      private long g;
      private boolean h;

      ResourceOutcomeAggregate(String var1, String var2) {
         this.a = var1;
         this.b = var2;
      }

      void a(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.ScenarioExitEvidence var1) {
         this.c++;
         if (var1.g() >= 0L) {
            this.g = this.g + var1.g();
            this.h = true;
         }

         if (!var1.d()) {
            this.e = false;
         }

         if (!var1.c()) {
            this.d = true;
            if (this.f == null && var1.e() != null) {
               this.f = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.c(var1.e());
            }
         }
      }

      Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("path", this.a);
         var1.put("kind", this.b);
         if (this.c == 0) {
            var1.put("notRun", true);
            var1.put("scenarios", 0);
            return var1;
         } else {
            var1.put("passed", !this.d);
            var1.put("scenarios", this.c);
            if (this.h) {
               var1.put("durationMs", this.g);
            }

            if (this.e) {
               var1.put("skipped", true);
            }

            if (this.f != null) {
               var1.put("error", this.f);
            }

            return var1;
         }
      }
   }

   private static final class EvidenceOutcomeCollector {
      private final List<Map<String, Object>> a = new ArrayList<>();
      private final Set<String> b = new HashSet<>();
      private int c;
      private int d;

      void a(String var1, Object var2, boolean var3) {
         if (this.b.add(var1 + "\u0000" + var3)) {
            this.b(var1, var2, var3);
         }
      }

      void b(String var1, Object var2, boolean var3) {
         if (var3) {
            this.c++;
         } else {
            this.d++;
         }

         if (this.a.size() < 500) {
            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            var4.put("label", var1);
            if (var2 != null) {
               var4.put("actual", io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var2));
            }

            var4.put("pass", var3);
            this.a.add(var4);
         }
      }
   }
}
