package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.ExtMeta;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

final class RuleTableDiffEngine {
   static final String a = "rating.diff/1";
   static final String b = "table-diff.json";
   static final String c = "tables:";
   static final String d = "CELL_AS_DECLARED";
   static final String e = "CELL_DECLARED_MISSING";
   static final String f = "CELL_WRONG_VALUE";
   static final String g = "CELL_UNEXPECTED";
   static final String h = "TABLE_FILE_UNEXPECTED";
   private static final AtomicLong j = new AtomicLong();
   static final String i = "karate-table-diff";

   private RuleTableDiffEngine() {
   }

   static RuleTableCatalog a(RuleTableCatalog var0, String var1) {
      boolean var2 = !var1.isEmpty()
         && Arrays.stream(var1.split("/", -1))
            .allMatch(var0x -> !var0x.isEmpty() && !var0x.equals("..") && !var0x.contains(".") && !var0x.contains("@") && !var0x.contains("\\"));
      RuleTableCatalog var3 = var2 ? var0.b(var1) : null;
      if (var3 == null) {
         throw new RuleApiException(404, "TABLE_BASE_UNKNOWN", "base '" + var1 + "' names no directory of tables under tables/ — one of " + a(var0));
      } else {
         return var3;
      }
   }

   static Set<String> a(RuleTableCatalog var0) {
      TreeSet var1 = new TreeSet();

      for (String var3 : var0.b().keySet()) {
         if (var3.contains(".")) {
            var1.add(var3.substring(0, var3.indexOf(46)));
         }
      }

      return var1;
   }

   private static Map<String, Object> a(Map<String, Object> var0, String var1, RuleTableCatalog var2) {
      String var3 = (String)var0.get("table");
      String var4 = var1.replace('/', '.') + ".";
      if (!var2.b().containsKey(var3) && var3.startsWith(var4)) {
         var var5 = new LinkedHashMap<>(var0);
         var5.put("table", var3.substring(var4.length()));
         return var5;
      } else {
         return var0;
      }
   }

   static Map<String, Object> a(String var0, RuleTableCatalog var1, io.karatelabs.ext.rules.RulebookRepository.DigestReference var2, RuleTableCatalog var3, String var4, io.karatelabs.ext.rules.RulebookRepository.DigestReference var5, List<Map<String, Object>> var6, String var7) {
      var var8 = var6.stream().map(var0x -> String.valueOf(var0x.get("id"))).toList();
      io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator var9 = new io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator(var1);

      for (Map var11 : var6) {
         for (Object var13 : (List)var11.get("rows")) {
            Map var14 = (Map)var13;
            a(var9, var1, String.valueOf(var11.get("id")), var7 == null ? var14 : a(var14, var7, var1), var7);
         }
      }

      ArrayList<Map<String, Object>> var24 = new ArrayList<>();
      ArrayList<Map<String, Object>> var25 = new ArrayList<>();
      var var26 = new ArrayList<>(var9.e);
      ArrayList var27 = new ArrayList();
      LinkedHashMap var28 = new LinkedHashMap();
      int var15 = 0;
      if (var6.isEmpty()) {
         var26.add(a("no-change-set", (Map<String, Object>)null, null));
         var28.put("raw", io.karatelabs.ext.rules.ChangeSetGenerator.a(var1, var3));
      } else {
         var var16 = new LinkedHashSet<>(var1.b().keySet());
         var16.addAll(var3.b().keySet());

         for (String var18 : (Iterable<String>) (Iterable<?>) (var16)) {
            io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var19 = var1.b().get(var18);
            io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var20 = var3.b().get(var18);
            if (var19 == null) {
               for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var22 : var20.g()) {
                  LinkedHashMap var23 = new LinkedHashMap();
                  var23.put("kind", "TABLE_FILE_UNEXPECTED");
                  var23.put("table", var18);
                  var23.put("construct", a("file", var22.a(), null));
                  var23.put("location", Map.of("file", var22.a()));
                  var23.put("expected", null);
                  var23.put("actual", Map.of("rows", var3.a(var20, var22.a()).size()));
                  var24.add(var23);
               }
            } else if (var20 == null && !var9.d.contains(var18)) {
               var26.add(a("table-absent", a("table", var18, null), "evidence"));
            } else if (var20 == null || a(var19, var20, var1, var9, var18, var26)) {
               var15 += a(var18, var19, var20, var1, var3, var9, var4, var24, var25, var27);
            }
         }

         if (var15 == 0) {
            var26.add(a("no-rows", (Map<String, Object>)null, null));
         }
      }

      var28.put("masked", var9.f);
      a(var3, var28);
      LinkedHashMap<String, Integer> var29 = new LinkedHashMap<>();
      var29.put("cells", var15);

      for (String var32 : List.of("CELL_AS_DECLARED", "CELL_DECLARED_MISSING", "CELL_WRONG_VALUE", "CELL_UNEXPECTED", "TABLE_FILE_UNEXPECTED")) {
         var29.put(var32, 0);
      }

      Stream.<Map>concat(var24.stream(), var25.stream()).forEach(var1x -> var29.merge((String)var1x.get("kind"), 1, Integer::sum));
      var29.put("breaking", var24.size());
      var29.put("compatible", var25.size());
      var29.put("notEvaluated", var26.size());
      LinkedHashMap var31 = new LinkedHashMap();
      var31.put("book", var0);
      var31.put("against", var4);
      if (var7 != null) {
         var31.put("base", var7);
      }

      var31.put("changeSets", var8);
      LinkedHashMap var33 = new LinkedHashMap();
      var33.put("book", var2.a());
      var33.put("evidence", var5.a());
      var31.put("digests", var33);
      var31.put("taxonomy", "rating.diff/1");
      var31.put("ok", var24.isEmpty());
      var31.put("checked", var15 > 0);
      var31.put("complete", var26.isEmpty());
      var31.put("counts", var29);
      var31.put("tables", var27);
      var31.put("breaking", var24);
      var31.put("compatible", var25);
      var31.put("notEvaluated", var26);
      var31.put("disclosures", var28);
      return var31;
   }

   static Map<String, Object> a(Map<String, Object> var0, int var1) {
      var var2 = new LinkedHashMap<>(var0);
      LinkedHashMap var3 = new LinkedHashMap();

      for (String var5 : List.of("breaking", "compatible", "notEvaluated")) {
         var2.put(var5, a((List<Map<String, Object>>)var0.get(var5), var1, var5, var3));
      }

      LinkedHashMap var9 = new LinkedHashMap((Map)var0.get("disclosures"));

      for (String var6 : List.of("raw", "masked")) {
         if (var9.get(var6) instanceof List var7) {
            var9.put(var6, a(var7, var1, var6, var3));
         }
      }

      var2.put("disclosures", var9);
      if (!var3.isEmpty()) {
         var2.put("truncated", var3);
      }

      return var2;
   }

   private static void a(io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator var0, RuleTableCatalog var1, String var2, Map<String, Object> var3, String var4) {
      String var5 = (String)var3.get("table");
      String var6 = (String)var3.get("op");
      String var7 = var3.get("jurisdiction") == null ? null : String.valueOf(var3.get("jurisdiction"));
      String var8 = var7 == null ? "" : var7;
      List var9 = ((List)var3.get("key")).stream().map(String::valueOf).toList();
      String var10 = io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator.a(var5, var8, var9);
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var11 = var1.b().get(var5);
      if (var11 == null) {
         var0.e.add(b(var2, var3, "no table '" + var5 + "' in the book" + (var4 == null ? "" : " under base '" + var4 + "'")));
      } else {
         var0.d.add(var5);
         var0.c.add(var10);
         if (var9.size() != var11.b().size()) {
            var0.e.add(b(var2, var3, "the key has " + var9.size() + " cells, table '" + var5 + "' " + var11.b().size()));
         } else if (var7 == null || var11.d() != null && var11.e().contains(var7)) {
            Map var12 = var3.get("before") instanceof Map var13 ? var13 : Map.of();
            Map var23 = var3.get("after") instanceof Map var24 ? var24 : Map.of();
            LinkedHashSet var25 = new LinkedHashSet(var12.keySet());
            var25.addAll(var23.keySet());

            for (String var28 : (Iterable<String>) (Iterable<?>) (var25)) {
               if (!var11.c().containsKey(var28)) {
                  var0.e.add(b(var2, var3, "'" + var28 + "' is not a value column of '" + var5 + "'"));
                  return;
               }

               if (var6.equals("change") && (!var12.containsKey(var28) || !var23.containsKey(var28))) {
                  var0.e.add(b(var2, var3, "before and after name the same columns"));
                  return;
               }
            }

            var var27 = var0.a.get(var5).computeIfAbsent(var8, var0x -> new LinkedHashMap<>());
            Map<String, io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState> var16 = var27.get(var9);
            var0.c.remove(var10);
            switch (var6) {
               case "add":
                  if (var16 != null) {
                     var0.e.add(b(var2, var3, "the row is present"));
                     var0.c.add(var10);
                     return;
                  }

                  LinkedHashMap var30 = new LinkedHashMap();
                  var23.forEach((var2x, var3x) -> var30.put(var2x, new io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState(var3x, var2)));
                  var27.put(var9, var30);
                  var0.b.remove(var10);
                  break;
               case "drop":
                  if (var16 == null) {
                     var0.e.add(b(var2, var3, "the row is absent"));
                     var0.c.add(var10);
                     return;
                  }

                  ArrayList var29 = new ArrayList();

                  for (Entry var32 : (Iterable<Entry>) (Iterable<?>) (var12.entrySet())) {
                     io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState var22 = (io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState)var16.get(var32.getKey());
                     if (var22 == null || !a(var32.getValue(), var22.a())) {
                        var29.add(a(var2, var3, (String)var32.getKey(), var32.getValue(), var22));
                     }
                  }

                  if (!var29.isEmpty()) {
                     var0.e.addAll(var29);
                     var0.c.add(var10);
                     return;
                  }

                  var16.forEach((var3x, var4x) -> a(var0, var3, var3x, var4x, var2));
                  var27.remove(var9);
                  var0.b.put(var10, var2);
                  break;
               default:
                  if (var16 == null) {
                     var0.e.add(b(var2, var3, "the row is absent"));
                     var0.c.add(var10);
                     return;
                  }

                  for (String var20 : (Iterable<String>) (Iterable<?>) (var25)) {
                     io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState var21 = (io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState)var16.get(var20);
                     if (var21 != null && a(var12.get(var20), var21.a())) {
                        a(var0, var3, var20, var21, var2);
                        var16.put(var20, new io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState(var23.get(var20), var2));
                     } else {
                        var0.e.add(a(var2, var3, var20, var12.get(var20), var21));
                        var0.c.add(var10 + "\u0000" + var20);
                     }
                  }
            }

            return;
         } else {
            var0.e.add(b(var2, var3, "table '" + var5 + "' has no jurisdiction '" + var7 + "'"));
         }
      }
   }

   private static void a(io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator var0, Map<String, Object> var1, String var2, io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState var3, String var4) {
      if (var3.b() != null) {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("table", var1.get("table"));
         if (var1.get("jurisdiction") != null) {
            var5.put("jurisdiction", var1.get("jurisdiction"));
         }

         var5.put("key", var1.get("key"));
         var5.put("column", var2);
         var5.put("changeSet", var3.b());
         var5.put("value", var3.a());
         var5.put("maskedBy", var4);
         var0.f.add(var5);
      }
   }

   private static boolean a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1, RuleTableCatalog var2, io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator var3, String var4, List<Map<String, Object>> var5) {
      LinkedHashMap var6 = new LinkedHashMap();
      if (!var0.b().equals(var1.b())) {
         var6.put("keys", b(var0.b(), var1.b()));
      }

      if (!Objects.equals(var0.d(), var1.d()) || !Objects.equals(a(var0.e()), a(var1.e()))) {
         var6.put("layers", b(var0.d() == null ? null : var0.e(), var1.d() == null ? null : var1.e()));
      }

      LinkedHashSet<String> var7 = new LinkedHashSet<>();
      var var8 = var3.a.get(var4);
      io.karatelabs.ext.rules.ChangeSetGenerator.a(var2, var0).forEach((var2x, var3x) -> {
         if (var3x.isEmpty() || !var8.getOrDefault(var2x, Map.of()).isEmpty()) {
            var7.add(var2x);
         }
      });
      var8.forEach((var1x, var2x) -> {
         if (!var2x.isEmpty()) {
            var7.add(var1x);
         }
      });
      LinkedHashSet<String> var9 = new LinkedHashSet<>();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var11 : var1.g()) {
         var9.add(var11.c() == null ? "" : var11.c());
      }

      if (!var7.equals(var9)) {
         var6.put(
            "files",
            b(
               var7.stream().map(var0x -> var0x.isEmpty() ? "countrywide" : var0x).toList(),
               var9.stream().map(var0x -> var0x.isEmpty() ? "countrywide" : var0x).toList()
            )
         );
      }

      if (!var6.isEmpty()) {
         var var15 = a("layout-mismatch", a("table", var4, null), null);
         var15.put("detail", var6);
         var5.add(var15);
         return false;
      } else {
         for (Entry var16 : var0.c().entrySet()) {
            String var12 = var1.c().get(var16.getKey());
            if (var12 != null && !var12.equals(var16.getValue())) {
               var var13 = a("type-mismatch", a("table", var4, null), null);
               var13.put("detail", Map.of("column", (String)var16.getKey(), "book", (String)var16.getValue(), "evidence", var12));
               var5.add(var13);
               return false;
            }
         }

         return true;
      }
   }

   private static int a(
      String var0,
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1,
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var2,
      RuleTableCatalog var3,
      RuleTableCatalog var4,
      io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator var5,
      String var6,
      List<Map<String, Object>> var7,
      List<Map<String, Object>> var8,
      List<Map<String, Object>> var9
   ) {
      var var10 = io.karatelabs.ext.rules.ChangeSetGenerator.a(var3, var1);
      var var11 = io.karatelabs.ext.rules.ChangeSetGenerator.a(var4, var2);
      var var12 = var5.a.get(var0);
      HashMap<String, String> var13 = new HashMap<>();
      if (var2 != null) {
         var2.g().forEach(var1x -> var13.put(var1x.c() == null ? "" : var1x.c(), var1x.a()));
      }

      HashMap<String, String> var14 = new HashMap<>();
      var1.g().forEach(var1x -> var14.put(var1x.c() == null ? "" : var1x.c(), var1x.a()));
      var var15 = new LinkedHashSet<>(var1.c().keySet());
      if (var2 != null) {
         var15.addAll(var2.c().keySet());
      }

      var var16 = new LinkedHashSet<>(var10.keySet());
      var16.addAll(var12.keySet());
      var16.addAll(var11.keySet());
      int var17 = 0;
      int var18 = var7.size() + var8.size();
      int var19 = var8.size();

      for (String var21 : (Iterable<String>) (Iterable<?>) (var16)) {
         var var22 = var10.getOrDefault(var21, Map.of());
         var var23 = var12.getOrDefault(var21, Map.of());
         var var24 = var11.getOrDefault(var21, Map.of());
         String var25 = var13.getOrDefault(
            var21, var14.getOrDefault(var21, var6 + "/" + var0.replace('.', '/') + (var21.isEmpty() ? "" : "@" + var21) + ".csv")
         );
         LinkedHashSet var26 = new LinkedHashSet(var22.keySet());
         var26.addAll(var23.keySet());
         var26.addAll(var24.keySet());

         for (List var28 : (Iterable<List>) (Iterable<?>) (var26)) {
            String var29 = io.karatelabs.ext.rules.RuleTableDiffEngine.TableDiffAccumulator.a(var0, var21, var28);
            if (!var5.c.contains(var29)) {
               Map var30 = (Map)var22.get(var28);
               Map var31 = (Map)var23.get(var28);
               Map var32 = (Map)var24.get(var28);

               for (String var34 : (Iterable<String>) (Iterable<?>) (var15)) {
                  if (!var5.c.contains(var29 + "\u0000" + var34)) {
                     Object var35 = var30 == null ? null : var30.get(var34);
                     io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState var36 = var31 == null ? null : (io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState)var31.get(var34);
                     Object var37 = var36 == null ? null : var36.a();
                     Object var38 = var32 == null ? null : var32.get(var34);
                     var17++;
                     String var39;
                     if (a(var37, var35)) {
                        if (a(var38, var35)) {
                           continue;
                        }

                        var39 = "CELL_UNEXPECTED";
                     } else if (a(var37, var38)) {
                        var39 = "CELL_AS_DECLARED";
                     } else {
                        var39 = var38 != null && var37 != null && !a(var38, var35) ? "CELL_WRONG_VALUE" : "CELL_DECLARED_MISSING";
                     }

                     LinkedHashMap var40 = new LinkedHashMap();
                     var40.put("kind", var39);
                     var40.put("table", var0);
                     var40.put("construct", a("cell", (var21.isEmpty() ? "" : var21 + "/") + String.join("/", var28), var34));
                     LinkedHashMap var41 = new LinkedHashMap();
                     var41.put("file", var25);
                     var41.put("row", String.join("/", var28));
                     var41.put("column", var34);
                     var40.put("location", var41);
                     if (!var39.equals("CELL_UNEXPECTED")) {
                        var40.put("changeSet", var36 != null && var36.b() != null ? var36.b() : var5.b.get(var29));
                     }

                     var40.put("expected", var37);
                     var40.put("actual", var38);
                     (var39.equals("CELL_AS_DECLARED") ? var8 : var7).add(var40);
                  }
               }
            }
         }
      }

      LinkedHashMap var42 = new LinkedHashMap();
      var42.put("table", var0);
      var42.put("cells", var17);
      var42.put("findings", var7.size() + var8.size() - var18);
      var42.put("compatible", var8.size() - var19);
      var9.add(var42);
      return var17;
   }

   static Map<String, Object> a(Map<String, Object> var0) {
      io.karatelabs.ext.diff.DiffReport var1 = new io.karatelabs.ext.diff.DiffReport("rating.diff/1", "karate-table-diff", ExtMeta.version("karate-max-meta.properties"));

      for (String var3 : List.of("breaking", "compatible")) {
         for (Map var5 : (Iterable<Map>) (Iterable<?>) ((List)var0.get(var3))) {
            String var6 = (String)var5.get("kind");
            Map var7 = (Map)var5.get("construct");
            String var8 = (String)var7.get("facet");
            String var9 = (String)var5.get("table");
            String var10 = var9 + " " + var7.get("key") + (var8 == null ? "" : " " + var8);
            String var11 = var6.equals("TABLE_FILE_UNEXPECTED")
               ? "a table file the book lacks"
               : "expected "
                  + var5.get("expected")
                  + (var5.get("changeSet") == null ? "" : " (" + var5.get("changeSet") + ")")
                  + ", actual "
                  + var5.get("actual");
            var1.a(
               var1.a(var6, var6.equals("CELL_AS_DECLARED") ? io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE : io.karatelabs.ext.diff.DiffSeverity.BREAKING)
                  .a(var10)
                  .b(var11)
                  .a("table", var9)
                  .a((String)var7.get("kind"), (String)var7.get("key"), var8)
                  .a((Map<String, Object>)null, (Map<String, Object>)var5.get("location"))
                  .a("changeSet", var5.get("changeSet"))
                  .a("expected", var5.get("expected"))
                  .a("actual", var5.get("actual"))
            );
         }
      }

      for (Map var14 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("notEvaluated"))) {
         LinkedHashMap var16 = new LinkedHashMap(var14);
         String var17 = (String)var16.remove("reason");
         Map var18 = (Map)var16.remove("construct");
         String var19 = (String)var16.remove("side");
         Object var20 = var16.remove("table");
         String var21 = var18 == null
            ? null
            : var18.get("kind") + " " + (var20 == null ? "" : var20 + " ") + var18.get("key") + (var18.get("facet") == null ? "" : " " + var18.get("facet"));
         var1.a(new io.karatelabs.ext.diff.UnevaluatedDiff(var17, var21, var19, null, var16.isEmpty() ? null : var16));
      }

      Map var13 = (Map)var0.get("digests");
      String var15 = (String)var0.get("book") + (var0.get("base") == null ? "" : "/tables/" + var0.get("base"));
      return var1.a(a(var15, (Map<String, Object>)var13.get("book")), a((String)var0.get("against"), (Map<String, Object>)var13.get("evidence")))
         .a(Boolean.TRUE.equals(var0.get("checked")))
         .toMap();
   }

   private static io.karatelabs.ext.diff.DiffReport.DiffArtifact a(String var0, Map<String, Object> var1) {
      return io.karatelabs.ext.diff.DiffReport.DiffArtifact.a(var0, (String)var1.get("sha256"), (String)var1.get("basis"));
   }

   private static boolean a(Object var0, Object var1) {
      return var0 != null && var1 != null ? io.karatelabs.ext.rules.BidirectionalValueMapping.a(var0).equals(io.karatelabs.ext.rules.BidirectionalValueMapping.a(var1)) : var0 == null && var1 == null;
   }

   private static void a(RuleTableCatalog var0, Map<String, Object> var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var5 : var0.b().values()) {
         for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var7 : var5.g()) {
            if (var7.d().get("rows") instanceof Map var8) {
               if (var8.get("skipped") instanceof List var11 && !var11.isEmpty()) {
                  LinkedHashMap var12 = new LinkedHashMap();
                  var12.put("file", var7.a());
                  var12.put("skipped", var11);
                  var2.add(var12);
               }
            } else {
               var3.add(var7.a());
            }
         }
      }

      var1.put("skipped", var2);
      var1.put("unaccounted", var3);
   }

   private static Map<String, Object> a(String var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("kind", var0);
      var3.put("key", var1);
      if (var2 != null) {
         var3.put("facet", var2);
      }

      return var3;
   }

   private static Map<String, Object> a(String var0, Map<String, Object> var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("reason", var0);
      if (var1 != null) {
         var3.put("construct", var1);
      }

      if (var2 != null) {
         var3.put("side", var2);
      }

      return var3;
   }

   private static Map<String, Object> b(String var0, Map<String, Object> var1, String var2) {
      var var3 = a("address-mismatch", a("row", c(var1), null), "expected");
      var3.put("table", var1.get("table"));
      var3.put("changeSet", var0);
      var3.put("op", var1.get("op"));
      var3.put("detail", var2);
      return var3;
   }

   private static Map<String, Object> a(String var0, Map<String, Object> var1, String var2, Object var3, io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState var4) {
      var var5 = a("before-mismatch", a("cell", c(var1), var2), "expected");
      var5.put("table", var1.get("table"));
      var5.put("changeSet", var0);
      var5.put("op", var1.get("op"));
      var5.put("before", var3);
      var5.put("found", var4 == null ? null : var4.a());
      if (var4 != null && var4.b() != null) {
         var5.put("foundFrom", var4.b());
      }

      return var5;
   }

   private static String c(Map<String, Object> var0) {
      String var1 = String.join("/", ((List)var0.get("key")).stream().map(String::valueOf).toList());
      return var0.get("jurisdiction") == null ? var1 : var0.get("jurisdiction") + "/" + var1;
   }

   private static Map<String, Object> b(Object var0, Object var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("expected", var0);
      var2.put("evidence", var1);
      return var2;
   }

   private static Set<String> a(List<String> var0) {
      return var0 == null ? null : Set.copyOf(var0);
   }

   private static List<Map<String, Object>> a(List<Map<String, Object>> var0, int var1, String var2, Map<String, Integer> var3) {
      if (var0.size() <= var1) {
         return var0;
      } else {
         var3.put(var2, var0.size());
         return new ArrayList<>(var0.subList(0, var1));
      }
   }

   static io.karatelabs.ext.rules.RulebookRepository.DigestReference b(Map<String, byte[]> var0) {
      TreeMap var1 = new TreeMap();
      var0.forEach((var1x, var2x) -> var1.put(var1x, io.karatelabs.ext.common.Sha256.a(var2x)));
      if (var1.size() == 1 && ((String)var1.keySet().iterator().next()).endsWith(".json")) {
         return new io.karatelabs.ext.rules.RulebookRepository.DigestReference("file", (String)var1.values().iterator().next());
      } else {
         ArrayList var2 = new ArrayList();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            LinkedHashMap var5 = new LinkedHashMap();
            var5.put("path", (String)var4.getKey());
            var5.put("sha256", (String)var4.getValue());
            var2.add(var5);
         }

         return new io.karatelabs.ext.rules.RulebookRepository.DigestReference("manifest", io.karatelabs.ext.common.Sha256.a(Json.stringifyStrict(var2)));
      }
   }

   static String a(String var0, String var1, Object var2) {
      return "tables:" + var0 + "#" + var1 + (var2 == null ? "" : "#" + var2);
   }

   static Map<String, Object> a(Path var0, Map<String, Object> var1) {
      String var2 = String.valueOf(var1.get("book"));
      long var3 = j.updateAndGet(var0x -> Math.max(System.currentTimeMillis(), var0x + 1L));
      String var5 = "tablediff-" + UUID.randomUUID().toString().substring(0, 8);
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("runId", var5);
      var6.put("key", a(var2, String.valueOf(var1.get("against")), var1.get("base")));
      var6.put("startedMs", var3);
      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("ingest", var6);
      var7.put("result", var1);
      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("runId", var5);
      var8.put("protocol", "evidence/1");
      var8.put("mode", "table-diff");
      var8.put("type", "table-diff");
      var8.put("task", "Rule.tableDiff " + var2);
      var8.put("book", var2);
      var8.put("against", var1.get("against"));
      var8.put("status", "completed");
      var8.put("startedMs", var3);
      var8.put("finishedMs", System.currentTimeMillis());
      var8.put("evidenceFinalized", true);
      Path var9 = var0.resolve(var5);

      try {
         Files.createDirectories(var9);
         Files.writeString(var9.resolve("table-diff.json"), Json.stringifyStrict(var7), StandardCharsets.UTF_8);
         Files.writeString(var9.resolve("meta.json"), Json.stringifyStrict(var8), StandardCharsets.UTF_8);
         return var6;
      } catch (IOException var11) {
         throw new UncheckedIOException(var11);
      }
   }

   static List<Map<String, Object>> a(Path var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (var0 != null && Files.isDirectory(var0)) {
         try (var var3 = Files.list(var0)) {
            for (Path var5 : (Iterable<Path>) (var3.filter(var0x -> Files.isDirectory(var0x)).sorted()::iterator)) {
               Path var6 = var5.resolve("table-diff.json");
               if (Files.isRegularFile(var6)) {
                  Map var7 = (Map)Json.parseStrict(Files.readString(var6, StandardCharsets.UTF_8), true);
                  Map var8 = (Map)var7.get("result");
                  if (var1.equals(var8.get("book"))) {
                     Map var9 = (Map)var7.get("ingest");
                     String var10 = String.valueOf(var9.get("key"));
                     Map var11 = (Map)var2.get(var10);
                     if (var11 == null || d(var9) > d((Map<String, Object>)var11.get("ingest"))) {
                        LinkedHashMap var12 = new LinkedHashMap(var8);
                        var12.put("ingest", var9);
                        var2.put(var10, var12);
                     }
                  }
               }
            }
         } catch (IOException var15) {
            throw new UncheckedIOException(var15);
         }

         ArrayList var16 = new ArrayList(var2.values());
         var16.sort(Comparator.<Map>comparingLong(var0x -> d((Map<String, Object>)var0x.get("ingest"))).reversed());
         return var16;
      } else {
         return List.of();
      }
   }

   private static long d(Map<String, Object> var0) {
      return var0.get("startedMs") instanceof Number var1 ? var1.longValue() : 0L;
   }

   private record DiffCellState(Object a, String b) {

      private DiffCellState(Object a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   private static final class TableDiffAccumulator {
      final Map<String, Map<String, Map<List<String>, Map<String, io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState>>>> a = new LinkedHashMap<>();
      final Map<String, String> b = new HashMap<>();
      final Set<String> c = new HashSet<>();
      final Set<String> d = new LinkedHashSet<>();
      final List<Map<String, Object>> e = new ArrayList<>();
      final List<Map<String, Object>> f = new ArrayList<>();

      TableDiffAccumulator(RuleTableCatalog var1) {
         for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 : var1.b().values()) {
            LinkedHashMap var4 = new LinkedHashMap();

            for (Entry var6 : io.karatelabs.ext.rules.ChangeSetGenerator.a(var1, var3).entrySet()) {
               LinkedHashMap var7 = new LinkedHashMap();

               for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (((Map)var6.getValue()).entrySet())) {
                  LinkedHashMap var10 = new LinkedHashMap();

                  for (String var12 : var3.c().keySet()) {
                     var10.put(var12, new io.karatelabs.ext.rules.RuleTableDiffEngine.DiffCellState(((Map)var9.getValue()).get(var12), null));
                  }

                  var7.put((List)var9.getKey(), var10);
               }

               var4.put((String)var6.getKey(), var7);
            }

            this.a.put(var3.a(), var4);
         }
      }

      static String a(String var0, String var1, List<String> var2) {
         return var0 + "\u0000" + var1 + "\u0000" + String.join("\u0000", var2);
      }
   }
}
