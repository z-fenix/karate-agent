package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public final class ProofArtifactParser {
   public static final String a = "proof:";
   public static final String b = "proof.json";
   public static final String c = "proof";
   private static final AtomicLong e = new AtomicLong();
   private static final Pattern f = Pattern.compile("[0-9a-f]{64}");
   public static final RunArtifactParser d = new RunArtifactParser() {
      @Override
      public String a() {
         return "proof:";
      }

      @Override
      public String b() {
         return "proof";
      }

      @Override
      public TestExecution a(Path var1) {
         return io.karatelabs.ext.coverage.collect.ProofArtifactParser.c(var1);
      }

      @Override
      public io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(String var1, ExecutionInventory var2) {
         return io.karatelabs.ext.coverage.collect.ProofArtifactParser.a(var1, var2);
      }

      @Override
      public boolean c() {
         return true;
      }
   };

   private ProofArtifactParser() {
   }

   public static String a(String var0, String var1) {
      return "proof:" + var0 + "#" + var1;
   }

   public static String[] a(String var0) {
      if (var0 != null && var0.startsWith("proof:")) {
         String var1 = var0.substring("proof:".length());
         int var2 = var1.lastIndexOf(35);
         return var2 > 0 && var2 != var1.length() - 1 ? new String[]{var1.substring(0, var2), var1.substring(var2 + 1)} : null;
      } else {
         return null;
      }
   }

   static io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(String var0, ExecutionInventory var1) {
      String[] var2 = a(var0);
      if (var2 == null) {
         return null;
      } else {
         Path var3 = var1.b();
         if (var3 == null) {
            return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
         } else {
            Path var4 = a(var3, var2[0]);
            Path var5 = a(var3, var2[1]);
            return var4 != null && var5 != null && Files.isRegularFile(var4) && Files.isRegularFile(var5) ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED;
         }
      }
   }

   public static Map<String, Object> a(Path var0, Path var1, Map<String, Object> var2, Map<String, Object> var3) {
      String var4 = a(String.valueOf(var3.get("spec")), String.valueOf(var3.get("cfg")));
      long var5 = e.updateAndGet(var0x -> Math.max(System.currentTimeMillis(), var0x + 1L));
      String var7 = "proof-" + UUID.randomUUID().toString().substring(0, 8);
      Path var8 = var0.resolve(var7);
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("runId", var7);
      var9.put("execution", var7 + "#1");
      var9.put("key", var4);
      var9.put("startedMs", var5);
      var9.put("root", var8.toAbsolutePath().normalize().relativize(var1.toAbsolutePath().normalize()).toString().replace('\\', '/'));
      LinkedHashMap var10 = new LinkedHashMap();
      var10.put("ingest", var9);
      var10.put("envelope", var2);
      var10.put("mapping", var3);
      LinkedHashMap var11 = new LinkedHashMap();
      var11.put("runId", var7);
      var11.put("protocol", "evidence/1");
      var11.put("mode", "proof");
      var11.put("type", "proof");
      var11.put("task", "Rule.prove " + var3.get("spec"));
      var11.put("status", "completed");
      var11.put("startedMs", var5);
      var11.put("finishedMs", System.currentTimeMillis());
      var11.put("evidenceFinalized", true);

      try {
         Files.createDirectories(var8);
         Files.writeString(var8.resolve("proof.json"), Json.stringifyStrict(var10), StandardCharsets.UTF_8);
         Files.writeString(var8.resolve("meta.json"), Json.stringifyStrict(var11), StandardCharsets.UTF_8);
      } catch (IOException var13) {
         throw new UncheckedIOException(var13);
      }

      LinkedHashMap var12 = new LinkedHashMap(var9);
      var12.remove("root");
      return var12;
   }

   public static Map<String, Object> a(Path var0) {
      Path var1 = var0.resolve("proof.json");
      if (!Files.isRegularFile(var1)) {
         return null;
      } else {
         try {
            return Json.of(Files.readString(var1, StandardCharsets.UTF_8)).value() instanceof Map var3
                  && var3.get("ingest") instanceof Map
                  && var3.get("envelope") instanceof Map
                  && var3.get("mapping") instanceof Map
               ? var3
               : null;
         } catch (RuntimeException | IOException var4) {
            return null;
         }
      }
   }

   public static List<Map<String, Object>> b(Path var0) {
      var var1 = a(var0);
      return var1 == null ? null : a(var0, var1);
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static List<Map<String, Object>> a(Path var0, Map<String, Object> var1) {
      Map var2 = (Map)var1.get("ingest");
      Map var3 = (Map)var1.get("envelope");
      Path var4 = var0.toAbsolutePath().resolve(String.valueOf(var2.getOrDefault("root", "../.."))).normalize();
      ArrayList var5 = new ArrayList();
      ArrayList var6 = new ArrayList();
      String var7 = a(var3.get("inputs"), (Map<?, ?>)var1.get("mapping"));
      if (var7 != null) {
         var5.add(var7);
      } else {
         Map var8 = (Map)var3.get("inputs");
         ArrayList var9 = new ArrayList((List)var8.get("spec"));
         var9.add(var8.get("cfg"));
         var9.add(var8.get("mapping"));

         for (Object var11 : var9) {
            a(var4, var11, var5, var6);
         }
      }

      ArrayList var19 = new ArrayList();

      for (Object var22 : var3.get("obligations") instanceof List ? (List)var3.get("obligations") : List.of()) {
         if (var22 instanceof Map var24) {
            ArrayList var12 = new ArrayList(var5);
            ArrayList var13 = new ArrayList(var6);
            LinkedHashMap var14 = new LinkedHashMap();
            var14.put("name", String.valueOf(var24.get("name")));
            String var15 = String.valueOf(var24.get("outcome"));
            if (var24.get("binding") instanceof Map var16) {
               var14.put("criterion", String.valueOf(var16.get("criterion")));
               a(var4, var16, var12, var13);
            }
            String var25 = switch (var15) {
               case "pass", "counterexample" -> var15;
               default -> "notchecked";
            };
            if (!var12.isEmpty()) {
               var25 = "unbindable";
               var14.put("why", var12);
            } else if (!var13.isEmpty()) {
               var25 = "stale";
               var14.put("why", var13);
            }

            var14.put("outcome", var25);
            var14.put("tlc", var15);
            var19.add(var14);
         }
      }

      return var19;
   }

   public static TestExecution c(Path var0) {
      var var1 = a(var0);
      if (var1 == null) {
         return null;
      } else {
         Map var2 = (Map)var1.get("ingest");
         Map var3 = (Map)var1.get("envelope");
         Map var4 = (Map)var1.get("mapping");
         String var5 = String.valueOf(var2.get("key"));
         String var6 = String.valueOf(var4.get("spec"));
         Long var7 = var2.get("startedMs") instanceof Number var8 ? var8.longValue() : null;
         ArrayList var19 = new ArrayList();
         ArrayList var20 = new ArrayList();
         boolean var10 = false;
         boolean var11 = true;

         for (Map var13 : (Iterable<Map>) (Iterable<?>) (a(var0, var1))) {
            String var14 = String.valueOf(var13.get("outcome"));

            io.karatelabs.ext.coverage.graph.TestNode.TestStatus var15 = switch (var14) {
               case "pass" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
               case "counterexample" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
               default -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
            };
            var10 |= var15 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
            var11 &= var15 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
            LinkedHashSet var16 = new LinkedHashSet();
            if (var13.get("criterion") != null) {
               var16.add("req=" + var13.get("criterion"));
            }

            var16.add("evidence=model");
            var16.add("proof=" + var14);
            String var26 = String.valueOf(var13.get("name"));
            var19.add(new io.karatelabs.ext.coverage.graph.TestNode(var5 + "::" + var26, var26, var6, 0, var16, var15));
            if (var13.get("why") != null) {
               LinkedHashMap var18 = new LinkedHashMap();
               var18.put("kind", var14);
               var18.put("obligation", var26);
               var18.put("why", var13.get("why"));
               var20.add(var18);
            }
         }

         LinkedHashMap var21 = new LinkedHashMap();
         var21.put("lane", "proof");
         if (var3.get("tool") instanceof Map var22) {
            var21.put("tool", String.valueOf(var22.get("version")));
         }

         var21.put("completion", String.valueOf(var3.get("completion")));
         String var23 = var0.getFileName().toString();
         io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var25 = var10 ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.FAILED : (var11 && !var19.isEmpty() ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED : io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.SKIPPED);
         return new TestExecution(
            String.valueOf(var2.get("execution")),
            var5,
            null,
            var23,
            var7,
            var7,
            var7,
            io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL,
            var25,
            false,
            null,
            var5,
            "Rule.prove " + var6,
            var6,
            0,
            Set.of(),
            "proof",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            var20,
            var21,
            var19
         );
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static void a(Path var0, Map<String, Object> var1, List<String> var2, List<String> var3) {
      String var4 = String.valueOf(var1.get("criterion"));
      String var5 = String.valueOf(var1.get("requirements"));
      Path var6 = a(var0, var5);
      String var7 = null;
      if (var6 != null && Files.isRegularFile(var6)) {
         var7 = b(var6, var4);
         if (var7 == null) {
            var2.add(var4);
         } else if (!a(var7.getBytes(StandardCharsets.UTF_8)).equals(var1.get("criterionSha256"))) {
            var3.add(var4);
         }
      } else {
         var2.add(var5);
      }

      for (Object var12 : var1.get("content") instanceof List ? (List)var1.get("content") : List.of()) {
         a(var0, var12, var2, var3);
      }
   }

   public static String a(Object var0, Map<?, ?> var1) {
      if (var0 instanceof Map var2) {
         if (!(
            var2.get("spec") instanceof List var3
               && var3.stream().allMatch(ProofArtifactParser::a)
               && !var3.stream().noneMatch(var1x -> ((Map)var1x).get("path").equals(var1.get("spec")))
         )) {
            return "inputs.spec";
         } else if (a(var2.get("cfg")) && ((Map)var2.get("cfg")).get("path").equals(var1.get("cfg"))) {
            return a(var2.get("mapping")) ? null : "inputs.mapping";
         } else {
            return "inputs.cfg";
         }
      } else {
         return "inputs";
      }
   }

   private static boolean a(Object var0) {
      return var0 instanceof Map var2 && var2.get("path") instanceof String && var2.get("sha256") instanceof String var1 && f.matcher(var1).matches();
   }

   private static void a(Path var0, Object var1, List<String> var2, List<String> var3) {
      if (var1 instanceof Map var4 && var4.get("path") != null) {
         String var5 = String.valueOf(var4.get("path"));
         Path var6 = a(var0, var5);
         if (var6 != null && Files.isRegularFile(var6)) {
            try {
               if (!a(Files.readAllBytes(var6)).equals(var4.get("sha256"))) {
                  var3.add(var5);
               }
            } catch (IOException var8) {
               var2.add(var5);
            }
         } else {
            var2.add(var5);
         }
      } else {
         var2.add(String.valueOf(var1));
      }
   }

   private static String b(Path var0, String var1) {
      int var2 = var1.indexOf(47);
      if (var2 <= 0) {
         return null;
      } else {
         try {
            io.karatelabs.ext.requirements.model.RequirementNode var3 = io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(Files.readString(var0, StandardCharsets.UTF_8))
               .a(var1.substring(0, var2));
            if (var3 == null) {
               return null;
            } else {
               String var4 = var1.substring(var2 + 1);

               for (io.karatelabs.ext.requirements.AcceptanceCriterion var6 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (io.karatelabs.ext.requirements.AcceptanceCriterion.a(var3.m()))) {
                  if (var6.a().equals(var4)) {
                     return var6.b();
                  }
               }

               return null;
            }
         } catch (RuntimeException | IOException var7) {
            return null;
         }
      }
   }

   public static Path a(Path var0, String var1) {
      Path var2 = var0.resolve(var1).normalize();
      return var2.startsWith(var0.normalize()) ? var2 : null;
   }

   private static String a(byte[] var0) {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(var0));
      } catch (NoSuchAlgorithmException var2) {
         throw new IllegalStateException(var2);
      }
   }
}
