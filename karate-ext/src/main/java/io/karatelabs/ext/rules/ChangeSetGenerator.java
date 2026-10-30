package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

final class ChangeSetGenerator {
   static final String a = "evidence/changes";
   private static final Pattern b = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

   private ChangeSetGenerator() {
   }

   static List<Map<String, Object>> a(io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var0, io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var1) {
      ImportMapping var2 = var0.c();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      LinkedHashSet var5 = new LinkedHashSet();
      var0.e().keySet().forEach(var1x -> var5.add(var1x == null ? "" : var1x));
      var1.e().keySet().forEach(var1x -> var5.add(var1x == null ? "" : var1x));

      for (String var7 : (Iterable<String>) (Iterable<?>) (var5)) {
         String var8 = var7.isEmpty() ? null : var7;
         var var9 = a(var0.e().get(var8));
         var var10 = a(var1.e().get(var8));

         for (io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var12 : (Iterable<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>) (Iterable<?>) (var9.values())) {
            io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var13 = (io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow)var10.get(var12.b());
            if (var13 == null) {
               var3.add(a(var2.q, var8, var12.b(), "drop", a(var2, var12), null));
            } else {
               LinkedHashMap var14 = new LinkedHashMap();
               LinkedHashMap var15 = new LinkedHashMap();

               for (Entry var17 : var2.w.entrySet()) {
                  Object var18 = io.karatelabs.ext.rules.RulebookImportEngine.c((String)var17.getValue(), var12.c().get(var17.getKey()));
                  Object var19 = io.karatelabs.ext.rules.RulebookImportEngine.c((String)var17.getValue(), var13.c().get(var17.getKey()));
                  if (!io.karatelabs.ext.rules.BidirectionalValueMapping.a(var18).equals(io.karatelabs.ext.rules.BidirectionalValueMapping.a(var19))) {
                     var14.put((String)var17.getKey(), var18);
                     var15.put((String)var17.getKey(), var19);
                  }
               }

               if (!var14.isEmpty()) {
                  var3.add(a(var2.q, var8, var12.b(), "change", var14, var15));
               }
            }
         }

         for (io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var21 : (Iterable<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>) (Iterable<?>) (var10.values())) {
            if (!var9.containsKey(var21.b())) {
               var4.add(a(var2.q, var8, var21.b(), "add", null, a(var2, var21)));
            }
         }
      }

      var3.addAll(var4);
      return var3;
   }

   private static Map<List<String>, io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow> a(List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 != null) {
         for (io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var3 : var0) {
            var1.put(var3.b(), var3);
         }
      }

      return var1;
   }

   private static Map<String, Object> a(ImportMapping var0, io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (Entry var4 : var0.w.entrySet()) {
         var2.put((String)var4.getKey(), io.karatelabs.ext.rules.RulebookImportEngine.c((String)var4.getValue(), var1.c().get(var4.getKey())));
      }

      return var2;
   }

   private static Map<String, Object> a(String var0, String var1, List<String> var2, String var3, Map<String, Object> var4, Map<String, Object> var5) {
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("table", var0);
      if (var1 != null) {
         var6.put("jurisdiction", var1);
      }

      var6.put("key", var2);
      var6.put("op", var3);
      if (var4 != null) {
         var6.put("before", var4);
      }

      if (var5 != null) {
         var6.put("after", var5);
      }

      return var6;
   }

   static Map<String, Object> a(Object var0) {
      if (!(var0 instanceof Map var1)) {
         throw new RuleApiException(400, "INVALID_INPUT", "produces declares {edition, effective, renewalEffective?, jurisdictions, lines}");
      } else {
         LinkedHashMap var2 = new LinkedHashMap();
         if (var1.get("edition") instanceof String var3 && var3.matches("[A-Za-z0-9_.-]+")) {
            var2.put("edition", var3);

            for (String var5 : List.of("effective", "renewalEffective")) {
               Object var6 = var1.get(var5);
               if (var6 != null || !var5.equals("renewalEffective")) {
                  if (!(var6 instanceof String var7) || !b.matcher(var7).matches()) {
                     throw new RuleApiException(400, "INVALID_INPUT", "produces." + var5 + " is an ISO date (YYYY-MM-DD)");
                  }

                  var2.put(var5, var7);
               }
            }

            for (String var13 : List.of("jurisdictions", "lines")) {
               if (!(var1.get(var13) instanceof List var14 && !var14.isEmpty())) {
                  throw new RuleApiException(400, "INVALID_INPUT", "produces." + var13 + " is a non-empty list, declared — never inferred");
               }

               ArrayList var16 = new ArrayList();
               Iterator var8 = var14.iterator();

               while (var8.hasNext()) {
                  if (!(var8.next() instanceof String var10) || var10.isEmpty()) {
                     throw new RuleApiException(400, "INVALID_INPUT", "produces." + var13 + " is a list of names");
                  }

                  var16.add(var10);
               }

               var2.put(var13, var16);
            }

            return var2;
         } else {
            throw new RuleApiException(400, "INVALID_INPUT", "produces.edition is a name of letters, digits, '_', '.' or '-'");
         }
      }
   }

   static Map<String, Object> a(Path var0, ImportMapping var1, List<Map<String, Object>> var2, Map<String, Object> var3, boolean var4) {
      String var5 = var3.get("edition") + "-" + io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2)).substring(0, 12);
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("id", var5);
      var6.put("ai", true);
      var6.put("provenance", io.karatelabs.ext.rules.RulebookImportEngine.a(var1, var1.p, (String)null));
      var6.put("produces", var3);
      var6.put("algorithm", var4);
      var6.put("rows", var2);
      Path var7 = var0.resolve("evidence/changes").resolve(var5 + ".json");

      try {
         if (Files.isRegularFile(var7)) {
            Map var8 = (Map)Json.parseStrict(Files.readString(var7, StandardCharsets.UTF_8), true);
            ArrayList var9 = new ArrayList();
            Map var10 = var8.get("produces") instanceof Map var11 ? var11 : Map.of();

            for (String var15 : List.of("effective", "renewalEffective", "jurisdictions", "lines")) {
               if (!Objects.equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var10.get(var15)), io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.get(var15)))) {
                  var9.add(var15);
               }
            }

            if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var8.get("provenance")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var6.get("provenance")))) {
               var9.add("provenance");
            }

            if (!var9.isEmpty()) {
               throw new RuleApiException(
                  409, "CHANGESET_ID_COLLISION", "change set " + var5 + " exists with different " + var9 + " — a change set is immutable once written"
               );
            }

            var8.put("path", "evidence/changes/" + var5 + ".json");
            var8.put("existing", true);
            return var8;
         }

         Files.createDirectories(var7.getParent());
         Files.writeString(var7, io.karatelabs.ext.rules.RulebookImportEngine.a(var6, Set.of("rows")), StandardCharsets.UTF_8);
      } catch (IOException var13) {
         throw new UncheckedIOException(var13);
      }

      var6.put("path", "evidence/changes/" + var5 + ".json");
      return var6;
   }

   static Map<String, Object> a(Path var0, Object var1) {
      Map var2;
      if (var1 instanceof Map var3) {
         var2 = var3;
      } else {
         if (!(var1 instanceof String var4) || !var4.matches("[A-Za-z0-9_.-]+")) {
            throw new RuleApiException(400, "INVALID_INPUT", "changeSet is an id under evidence/changes/ or the change set object");
         }

         Path var5 = var0.resolve("evidence/changes").resolve(var4 + ".json");
         if (!Files.isRegularFile(var5)) {
            throw new RuleApiException(404, "CHANGESET_NOT_FOUND", "no change set '" + var4 + "' under evidence/changes/");
         }

         try {
            var2 = (Map)Json.parseStrict(Files.readString(var5, StandardCharsets.UTF_8), true);
         } catch (IOException var15) {
            throw new UncheckedIOException(var15);
         }
      }

      String var16 = String.valueOf(var2.get("id"));
      if (!(var2.get("rows") instanceof List var17)) {
         throw new RuleApiException(400, "CHANGESET_INVALID", "change set " + var16 + ": rows is a list of {table, jurisdiction?, key, op, before?, after?}");
      } else {
         int var19 = 0;

         for (Object var7 : var17) {
            String var8 = "change set " + var16 + " row " + ++var19;
            if (var7 instanceof Map var9
               && var9.get("table") instanceof String
               && var9.get("key") instanceof List
               && var9.get("op") instanceof String var10
               && List.of("change", "add", "drop").contains(var10)) {
               boolean var20 = var9.get("before") instanceof Map var12 && !var12.isEmpty();
               boolean var21 = var9.get("after") instanceof Map var22 && !var22.isEmpty();
               if (var10.equals("change") ? var20 && var21 : (var10.equals("add") ? var21 && var9.get("before") == null : var20 && var9.get("after") == null)) {
                  if (var10.equals("change")
                     && io.karatelabs.ext.rules.twin.CanonicalJson.c(a((Map<String, Object>)var9.get("before")))
                        .equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(a((Map<String, Object>)var9.get("after"))))) {
                     throw new RuleApiException(
                        400,
                        "CHANGESET_NOOP_ROW",
                        var8
                           + " ("
                           + var9.get("table")
                           + " "
                           + String.join("/", ((List)var9.get("key")).stream().map(String::valueOf).toList())
                           + "): before equals after — a change row changes a value"
                     );
                  }
                  continue;
               }

               throw new RuleApiException(400, "CHANGESET_INVALID", var8 + ": a change carries before and after, an add after, a drop before");
            }

            throw new RuleApiException(400, "CHANGESET_INVALID", var8 + ": {table, key, op: change|add|drop} is required");
         }

         return var2;
      }
   }

   private static Map<String, String> a(Map<String, Object> var0) {
      TreeMap var1 = new TreeMap();
      var0.forEach((var1x, var2) -> var1.put(var1x, io.karatelabs.ext.rules.BidirectionalValueMapping.a(var2)));
      return var1;
   }

   static List<Map<String, Object>> a(RuleTableCatalog var0, RuleTableCatalog var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      var var4 = new LinkedHashSet<>(var0.b().keySet());
      var4.addAll(var1.b().keySet());

      for (String var6 : (Iterable<String>) (Iterable<?>) (var4)) {
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var7 = var0.b().get(var6);
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var8 = var1.b().get(var6);
         var var9 = a(var0, var7);
         var var10 = a(var1, var8);
         var var11 = new LinkedHashSet<>(var9.keySet());
         var11.addAll(var10.keySet());
         LinkedHashSet var12 = new LinkedHashSet(var7 == null ? Set.of() : var7.c().keySet());
         var12.addAll(var8 == null ? Set.of() : var8.c().keySet());

         for (String var14 : (Iterable<String>) (Iterable<?>) (var11)) {
            String var15 = var14.isEmpty() ? null : var14;
            var var16 = var9.getOrDefault(var14, Map.of());
            var var17 = var10.getOrDefault(var14, Map.of());

            for (Entry var19 : (Iterable<Entry>) (Iterable<?>) (var16.entrySet())) {
               Map var20 = (Map)var17.get(var19.getKey());
               if (var20 == null) {
                  var2.add(a(var6, var15, (List<String>)var19.getKey(), "drop", a(var12, (Map<String, Object>)var19.getValue()), null));
               } else {
                  LinkedHashMap var21 = new LinkedHashMap();
                  LinkedHashMap var22 = new LinkedHashMap();

                  for (String var24 : (Iterable<String>) (Iterable<?>) (var12)) {
                     Object var25 = ((Map)var19.getValue()).get(var24);
                     Object var26 = var20.get(var24);
                     if (!io.karatelabs.ext.rules.BidirectionalValueMapping.a(var25).equals(io.karatelabs.ext.rules.BidirectionalValueMapping.a(var26))) {
                        var21.put(var24, var25);
                        var22.put(var24, var26);
                     }
                  }

                  if (!var21.isEmpty()) {
                     var2.add(a(var6, var15, (List<String>)var19.getKey(), "change", var21, var22));
                  }
               }
            }

            for (Entry var28 : (Iterable<Entry>) (Iterable<?>) (var17.entrySet())) {
               if (!var16.containsKey(var28.getKey())) {
                  var3.add(a(var6, var15, (List<String>)var28.getKey(), "add", null, a(var12, (Map<String, Object>)var28.getValue())));
               }
            }
         }
      }

      var2.addAll(var3);
      return var2;
   }

   static Map<String, Map<List<String>, Map<String, Object>>> a(RuleTableCatalog var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (var1 != null) {
         for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var4 : var1.g()) {
            var2.put(var4.c() == null ? "" : var4.c(), var0.a(var1, var4.a()));
         }
      }

      return var2;
   }

   private static Map<String, Object> a(Set<String> var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (String var4 : var0) {
         if (var1.containsKey(var4)) {
            var2.put(var4, var1.get(var4));
         }
      }

      return var2;
   }

   static Map<String, Object> a(Map<String, Object> var0, Set<String> var1, String var2) {
      ArrayList var3 = new ArrayList();

      for (Object var5 : (List)var0.get("rows")) {
         LinkedHashMap var6 = new LinkedHashMap((Map)var5);
         String var7 = (String)var6.get("table");
         int var8 = var7.indexOf(46);
         String var9 = var8 > 0 && var1.contains(var7.substring(0, var8)) ? var7.substring(var8 + 1) : var7;
         var6.put("table", var2.replace('/', '.') + "." + var9);
         var3.add(var6);
      }

      var var10 = new LinkedHashMap<>(var0);
      var10.put("rows", var3);
      return var10;
   }

   static Map<String, Object> a(Map<String, Object> var0, RuleTableCatalog var1) {
      LinkedHashSet var2 = new LinkedHashSet();
      ArrayList var3 = new ArrayList();
      Map var5 = (Map)var0.get("rows");
      if (!(var5 instanceof List)) {
         throw new RuleApiException(400, "INVALID_INPUT", "a change set carries rows");
      } else {
         for (Object var6 : (List)var5) {
            Map var7 = (Map)var6;
            String var8 = String.valueOf(var7.get("table"));
            String var9 = var7.get("jurisdiction") == null ? null : String.valueOf(var7.get("jurisdiction"));
            ArrayList var10 = new ArrayList();

            for (Object var12 : (List)var7.get("key")) {
               var10.add(String.valueOf(var12));
            }

            Map var21 = (Map)("drop".equals(var7.get("op")) ? var7.get("before") : var7.get("after"));
            io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var22 = var1.b().get(var8);

            for (String var14 : (Iterable<String>) (Iterable<?>) (var21.keySet())) {
               io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var15 = var1.a(var8, var9, var10, var14);
               if (var15 != null) {
                  var2.add(var15.a());
               } else {
                  boolean var16 = var22 != null ? var22.c().size() == 1 : !"change".equals(var7.get("op")) && var21.size() == 1;
                  boolean var17 = var22 != null ? var22.d() != null : var9 != null;
                  LinkedHashMap var18 = new LinkedHashMap();
                  var18.put("address", new io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference(var8, null, var9, var10, var14, null, var16, var17).a());
                  var18.put("op", var7.get("op"));
                  var3.add(var18);
               }
            }
         }

         var5 = new LinkedHashMap();
         var5.put("changeSet", var0.get("id"));
         var5.put("cells", new ArrayList(var2));
         var5.put("absent", var3);
         return var5;
      }
   }
}
