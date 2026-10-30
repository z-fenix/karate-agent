package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class EvidenceSheetExporter {
   static final String a = "evidence/sheets";
   static final String b = "evaluator 5";
   static final String c = "karate-max " + RulesExt.VERSION;

   private EvidenceSheetExporter() {
   }

   public static io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, EvidenceMapping var2) {
      ArrayList var3 = new ArrayList();

      for (Map var5 : var0.f()) {
         var3.add(io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate.a(var5));
      }

      return a(var0, var1, var3, var2, null);
   }

   public static io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, List<io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate> var2, EvidenceMapping var3, Map<String, Object> var4) {
      if (var3 != null) {
         var3.a(var0);
      }

      ArrayList var5 = new ArrayList();
      ArrayList var6 = new ArrayList();
      LinkedHashSet var7 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate var9 : var2) {
         if (!var7.add(var9.a())) {
            throw new RuleApiException(400, "EXPORT_DUPLICATE_ID", "row id '" + var9.a() + "' appears twice — a sheet is an inventory by id");
         }

         Map var10 = var9.e() ? RulebookRepository.d(var9.c()) : var9.c();
         LinkedHashMap var11 = new LinkedHashMap();
         var11.put("id", var9.a());
         if (var9.b() != null) {
            var11.put("label", var9.b());
         }

         var11.put("provenance", var9.e() ? "scenario" : "generated");
         var11.put("input", var10);
         if (var9.d() != null) {
            var11.put("expected", Map.of("refusal", var9.d()));
         } else {
            a(var0, var1, var9.c(), var11);
            if (var9.f() != null) {
               for (String var13 : List.of("target", "probes")) {
                  if (var9.f().get(var13) != null) {
                     var11.put(var13, var9.f().get(var13));
                  }
               }
            }
         }

         var5.add(var11);
         LinkedHashMap var18 = new LinkedHashMap();
         var18.put("id", var9.a());
         var18.put("input", var10);
         if (var9.d() != null) {
            var18.put("refusal", var9.d());
         }

         var6.add(var18);
      }

      var var14 = a(var0, var3 == null ? null : var3.b());
      String var15 = a(var6, var14);
      LinkedHashMap var16 = new LinkedHashMap();
      var16.put("book", var0.d());
      var16.put("digest", var0.n().a());
      var16.put("engine", c);
      var16.put("figures", var0.o());
      var16.put("generator", var0.i().get("generator"));
      if (var3 != null) {
         var16.put("mapping", var3.j());
         var var17 = var3.a(var0.e());
         if (!var17.isEmpty()) {
            var16.put("lint", var17);
         }
      }

      var16.put("sheet", Map.of("digest", var15));
      var16.put("binding", var14);
      if (var4 != null) {
         var16.put("cells", var4);
      }

      var16.put("rows", var5);
      return new io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet(var15, var16);
   }

   private static void a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Map<String, Object> var2, Map<String, Object> var3) {
      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var4 = var1.a(var0.g(), var0.m().e(), var2, null);
         if (var4.g()) {
            var3.put("outcome", var4.f());
         }

         LinkedHashMap var5 = new LinkedHashMap();
         if (var4.b() instanceof Map var6) {
            for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
               var5.put(String.valueOf(var8.getKey()), var8.getValue());
            }
         }

         var3.put("outputs", var5);
         var3.put("reqs", var4.h() == null ? List.of() : new ArrayList<>(var4.h()));
         var3.put("expected", var4.b());
      } catch (RuntimeException var9) {
         var3.put("error", var9.getMessage() == null ? var9.getClass().getSimpleName() : var9.getMessage());
         var3.put("outputs", Map.of());
         var3.put("reqs", List.of());
      }
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("rulebook", var0.n().a());
      var2.put("schema", var0.i().get("schema"));
      if (var1 != null) {
         var2.put("mapping", var1);
      }

      var2.put("engine", "evaluator 5");
      return var2;
   }

   static String a(List<Map<String, Object>> var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("rows", var0);
      var2.put("binding", var1);
      return io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2));
   }

   public static Path a(Path var0, io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet var1) {
      Path var2 = var0.resolve("evidence/sheets").resolve(var1.b() + ".json");

      try {
         Files.createDirectories(var2.getParent());
         a(var2, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.c()) + "\n");
         return var2;
      } catch (IOException var4) {
         throw new UncheckedIOException(var4);
      }
   }

   static void a(Path var0, String var1) throws IOException {
      if (Files.exists(var0)) {
         Files.setLastModifiedTime(var0, FileTime.from(Instant.now()));
      } else {
         Files.writeString(var0, var1, StandardCharsets.UTF_8);
      }
   }

   public static Map<String, Object> b(Path var0, String var1) {
      if (var1 != null && var1.matches("[0-9a-f]{64}")) {
         Path var2 = var0.resolve("evidence/sheets").resolve(var1 + ".json");
         if (!Files.isRegularFile(var2)) {
            return null;
         } else {
            try {
               return (Map<String, Object>)Json.parseStrict(Files.readString(var2, StandardCharsets.UTF_8), true);
            } catch (IOException var4) {
               throw new UncheckedIOException(var4);
            }
         }
      } else {
         return null;
      }
   }

   static Map<String, Integer> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.j() != null) {
         for (io.karatelabs.ext.rules.Generator.GeneratorDimension var3 : Generator.a(var0.j()).a()) {
            if ("cardinality".equals(var3.b())) {
               var1.put(io.karatelabs.ext.rules.FlattenedCsvCodec.b(var3.f()), ((Number)var3.c().get(1)).intValue());
            }
         }
      }

      return var1;
   }

   public static String a(io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet var0, EvidenceMapping var1, Map<String, Integer> var2, int var3) {
      var var4 = var0.a();
      if (var4.size() > var3) {
         throw new RuleApiException(
            400,
            "EXPORT_ROW_CAP",
            var4.size()
               + " rows is over the CSV row cap "
               + var3
               + " (karate.rules.tableRowsMax); nothing was written — export without format:'csv' for the JSON sheet"
         );
      } else {
         ArrayList var5 = new ArrayList();
         LinkedHashMap var6 = new LinkedHashMap();

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var4)) {
            Map var9 = io.karatelabs.ext.rules.FlattenedCsvCodec.a(var8);
            LinkedHashMap var10 = new LinkedHashMap();

            for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
               String var13 = a((String)var12.getKey());
               String var14 = (String)var12.getKey();
               Object var15 = var12.getValue();
               if (var13 != null) {
                  if (var13.endsWith("#")) {
                     String var16 = io.karatelabs.ext.rules.FlattenedCsvCodec.b(var13.substring(0, var13.length() - 1));
                     if (!var2.containsKey(var16)) {
                        throw new RuleApiException(
                           400,
                           "EXPORT_NOT_FLAT",
                           "input."
                              + var16
                              + " is an array with no declared bound (a generator cardinality) — the CSV cannot flatten it; nothing was written — export without format:'csv' for the JSON sheet"
                        );
                     }
                  } else if (var1 != null) {
                     String var18 = var1.a(var13);
                     var14 = var18 != null ? var18 : var14;
                     var15 = var1.a(var13, var15);
                  }
               }

               String var19 = (String)var6.putIfAbsent(var14, (String)var12.getKey());
               if (var19 != null && !var19.equals(var12.getKey())) {
                  throw a(var14, var19, (String)var12.getKey());
               }

               var10.put(var14, var15);
            }

            var5.add(var10);
         }

         List var17 = io.karatelabs.ext.rules.FlattenedCsvCodec.b(var5);
         if (var1 != null) {
            if (var6.containsKey(var1.e())) {
               throw a(var1.e(), (String)var6.get(var1.e()), "figure.field");
            }

            var17.add(var1.e());
         }

         return io.karatelabs.ext.rules.FlattenedCsvCodec.a(var17, var5);
      }
   }

   private static RuleApiException a(String var0, String var1, String var2) {
      return new RuleApiException(400, "EXPORT_COLUMN_COLLISION", "'" + var1 + "' and '" + var2 + "' both flatten to the column '" + var0 + "'");
   }

   private static String a(String var0) {
      if (var0.equals("input")) {
         return null;
      } else if (var0.startsWith("input.")) {
         return var0.substring("input.".length());
      } else {
         return var0.startsWith("input[") ? var0.substring("input".length()) : null;
      }
   }

   public static List<Map<String, Object>> a(String var0, EvidenceMapping var1) {
      ArrayList var2 = new ArrayList();

      for (Map var4 : io.karatelabs.ext.rules.FlattenedCsvCodec.c(var0)) {
         LinkedHashMap var5 = new LinkedHashMap();

         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var8 = (String)var7.getKey();
            Object var9 = var7.getValue();
            if (var1 != null) {
               if (var8.equals(var1.e())) {
                  continue;
               }

               String var10 = var1.b(var8);
               if (var10 != null) {
                  var8 = var10.startsWith("[") ? "input" + var10 : "input." + var10;
               }

               String var11 = a(var8);
               if (var11 != null && !var11.endsWith("#") && var1.c(var11)) {
                  var9 = var1.b(var11, var9);
               }
            }

            var5.put(var8, var9);
         }

         var2.add(io.karatelabs.ext.rules.FlattenedCsvCodec.b(var5));
      }

      return var2;
   }

   public record SheetCandidate(String a, String b, Map<String, Object> c, String d, boolean e, Map<String, Object> f) {

      public SheetCandidate(String a, String b, Map<String, Object> c, String d, boolean e, Map<String, Object> f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      public static io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate a(Map<String, Object> var0) {
         Object var1 = var0.get("_label");
         return new io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate(
            String.valueOf(var0.get("_id")), var1 == null ? null : String.valueOf(var1), var0, RulebookRepository.a(var0) ? "schema-reject" : null, true, null
         );
      }

      public static io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate a(Map<String, Object> var0, Map<String, Object> var1) {
         return new io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate("gen-" + io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var0)).substring(0, 12), null, var0, null, false, var1);
      }
   }

   public record EvidenceSheet(String b, Map<String, Object> c) {

      public EvidenceSheet(String b, Map<String, Object> c) {
         this.b = b;
         this.c = c;
      }

      public List<Map<String, Object>> a() {
         return (List<Map<String, Object>>)this.c.get("rows");
      }

      public String b() {
         return this.b;
      }

      public Map<String, Object> c() {
         return this.c;
      }
   }
}
