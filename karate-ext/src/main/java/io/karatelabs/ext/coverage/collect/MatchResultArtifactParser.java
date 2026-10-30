package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class MatchResultArtifactParser {
   public static final String a = "results:";
   public static final String b = "match.json";
   public static final String c = "results";
   private static final AtomicLong e = new AtomicLong();
   public static final RunArtifactParser d = new RunArtifactParser() {
      @Override
      public String a() {
         return "results:";
      }

      @Override
      public String b() {
         return "results";
      }

      @Override
      public TestExecution a(Path var1) {
         return io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.b(var1);
      }

      @Override
      public io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(String var1, ExecutionInventory var2) {
         return io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.a(var1, var2);
      }
   };

   private MatchResultArtifactParser() {
   }

   static io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(String var0, ExecutionInventory var1) {
      String[] var2 = a(var0);
      if (var2 == null) {
         return null;
      } else {
         Path var3 = var1.c();
         return io.karatelabs.ext.coverage.collect.ExecutionInventory.a(
            var1.d(), var1.a(var2[0]) && var3 != null && Files.isRegularFile(var3.resolve(var2[0]).resolve("evidence/sheets/" + var2[1] + ".json"))
         );
      }
   }

   public static String a(String var0, String var1) {
      return "results:" + var0 + "#" + var1;
   }

   public static String[] a(String var0) {
      if (var0 != null && var0.startsWith("results:")) {
         String var1 = var0.substring("results:".length());
         int var2 = var1.lastIndexOf(35);
         return var2 <= 0 ? null : new String[]{var1.substring(0, var2), var1.substring(var2 + 1)};
      } else {
         return null;
      }
   }

   public static Map<String, Object> a(Path var0, Map<String, Object> var1, List<String> var2) {
      String var3 = String.valueOf(var1.get("book"));
      String var4 = String.valueOf(((Map)var1.get("sheet")).get("digest"));
      long var5 = e.updateAndGet(var0x -> Math.max(System.currentTimeMillis(), var0x + 1L));
      String var7 = "match-" + UUID.randomUUID().toString().substring(0, 8);
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("runId", var7);
      var8.put("execution", var7 + "#1");
      var8.put("key", a(var3, var4));
      var8.put("startedMs", var5);
      LinkedHashMap<java.lang.String, java.lang.String> var9 = new LinkedHashMap();

      for (Object var11 : (List)var1.get("cases")) {
         Map<java.lang.String, Object> var12 = (Map)var11;
         var9.putIfAbsent(String.valueOf(var12.get("case")), String.valueOf(var12.get("class")));
      }

      ArrayList<java.util.LinkedHashMap> var16 = new ArrayList();

      for (String var19 : var2) {
         LinkedHashMap<java.lang.String, java.lang.String> var13 = new LinkedHashMap();
         var13.put("case", var19);
         var13.put("status", b((String)var9.get(var19)).name());
         var16.add(var13);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
      var18.put("ingest", var8);
      var18.put("subjects", var16);
      var18.put("report", var1);
      LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
      var20.put("runId", var7);
      var20.put("protocol", "evidence/1");
      var20.put("mode", "match");
      var20.put("type", "match");
      var20.put("task", "Rule.match " + var3);
      var20.put("book", var3);
      var20.put("sheet", var4);
      var20.put("status", "completed");
      var20.put("startedMs", var5);
      var20.put("finishedMs", System.currentTimeMillis());
      var20.put("evidenceFinalized", true);
      Path var21 = var0.resolve(var7);

      try {
         Files.createDirectories(var21);
         Files.writeString(var21.resolve("match.json"), Json.stringifyStrict(var18), StandardCharsets.UTF_8);
         Files.writeString(var21.resolve("meta.json"), Json.stringifyStrict(var20), StandardCharsets.UTF_8);
         return var8;
      } catch (IOException var15) {
         throw new UncheckedIOException(var15);
      }
   }

   static io.karatelabs.ext.coverage.graph.TestNode.TestStatus b(String var0) {
      if (var0 == null) {
         return io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
      } else {
         return switch (var0) {
            case "match", "match (±x)" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
            case "mismatch", "rated but expected refused" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
            default -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
         };
      }
   }

   public static Map<String, Object> a(Path var0) {
      Path var1 = var0.resolve("match.json");
      if (!Files.isRegularFile(var1)) {
         return null;
      } else {
         try {
            return Json.of(Files.readString(var1, StandardCharsets.UTF_8)).value() instanceof Map var3
                  && var3.get("ingest") instanceof Map
                  && var3.get("report") instanceof Map
               ? var3
               : null;
         } catch (RuntimeException | IOException var4) {
            return null;
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static TestExecution b(Path var0) {
      var var1 = a(var0);
      if (var1 == null) {
         return null;
      } else {
         Map<java.lang.String, Object> var2 = (Map)var1.get("ingest");
         Map<java.lang.String, java.lang.Object> var3 = (Map)var1.get("report");
         String var4 = String.valueOf(var2.get("key"));
         String var5 = String.valueOf(var3.get("book"));
         Long var6 = var2.get("startedMs") instanceof Number var7 ? var7.longValue() : null;
         LinkedHashMap<java.lang.String, java.lang.String> var15 = new LinkedHashMap();
         Object var9 = var3.get("cases");
         if (var9 instanceof List) {
            for (Object var10 : (List)var9) {
               if (var10 instanceof Map var11 && var11.get("label") != null) {
                  var15.putIfAbsent(String.valueOf(var11.get("case")), String.valueOf(var11.get("label")));
               }
            }
         }

         ArrayList var17 = new ArrayList();
         boolean var19 = false;

         for (Object var23 : (List)var1.get("cases")) {
            Map<java.lang.String, Object> var26 = (Map)var23;
            String var13 = String.valueOf(var26.get("case"));
            io.karatelabs.ext.coverage.graph.TestNode.TestStatus var14 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.valueOf(String.valueOf(var26.get("status")));
            var19 |= var14 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
            var17.add(new io.karatelabs.ext.coverage.graph.TestNode(var4 + "::" + var13, var15.getOrDefault(var13, var13), "rulebook:" + var5, 0, Set.of(), var14));
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
         var21.put("lane", "results");
         if (var3.get("disclosures") instanceof Map var24 && Boolean.TRUE.equals(var24.get("mockOnly"))) {
            var21.put("mockOnly", true);
         }

         String var25 = var0.getFileName().toString();
         return new TestExecution(
            String.valueOf(var2.get("execution")),
            var4,
            null,
            var25,
            var6,
            var6,
            var6,
            io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL,
            var19 ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.FAILED : io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED,
            false,
            null,
            var4,
            "Rule.match " + var5,
            "rulebook:" + var5,
            0,
            Set.of(),
            "results",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            var21,
            var17
         );
      }
   }

   public static List<Map<String, Object>> a(Path var0, String var1) {
      LinkedHashMap<String, Map<String, Object>> var2 = new LinkedHashMap();
      if (var0 != null && Files.isDirectory(var0)) {
         try (var var3 = Files.list(var0)) {
            for (Path var5 : (Iterable<Path>) (var3.filter(var0x -> Files.isDirectory(var0x)).sorted()::iterator)) {
               var var6 = a(var5);
               if (var6 != null && var1.equals(((Map)var6.get("report")).get("book"))) {
                  LinkedHashMap<String, Object> var7 = new LinkedHashMap((Map)var6.get("ingest"));
                  var7.put("runId", var5.getFileName().toString());
                  String var8 = String.valueOf(var7.get("key"));
                  Map<String, Object> var9 = var2.get(var8);
                  if (var9 == null || a(var7) > a((Map<String, Object>)var9.get("ingest"))) {
                     LinkedHashMap<String, Object> var10 = new LinkedHashMap((Map)var6.get("report"));
                     var10.put("ingest", var7);
                     var2.put(var8, var10);
                  }
               }
            }
         } catch (IOException var13) {
            throw new UncheckedIOException(var13);
         }

         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var14 = new ArrayList(var2.values());
         var14.sort(Comparator.<Map>comparingLong(var0x -> a((Map<String, Object>)var0x.get("ingest"))).reversed());
         return var14;
      } else {
         return List.of();
      }
   }

   private static long a(Map<String, Object> var0) {
      return var0.get("startedMs") instanceof Number var1 ? var1.longValue() : 0L;
   }
}
