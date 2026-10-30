package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

final class ImportMapping {
   static final String a = ".import.json";
   static final Set<String> b = Set.of("number", "integer", "string", "boolean");
   static final Set<String> c = Set.of("points", "fraction", "auto");
   static final Set<String> d = Set.of("number", "fraction", "plus-one");
   static final Set<String> e = Set.of("date", "mdy", "datetime");
   static final Set<String> f = Set.of("baseline", "reference", "under-test");
   static final List<String> g = List.of(
      "code", "edition", "line", "jurisdiction", "status", "effective", "renewalEffective", "expiration", "uwCompany", "offering", "companionOf", "nameDate"
   );
   static final Set<String> h = Set.of("effective", "renewalEffective", "expiration", "nameDate");
   final String i;
   final String j;
   final boolean k;
   final String l;
   final int m;
   final Integer n;
   final String o;
   final String p;
   final String q;
   final String r;
   final io.karatelabs.ext.rules.ImportMapping.LayerMapping s;
   final String t;
   final Map<String, String> u = new LinkedHashMap<>();
   final List<String> v;
   final Map<String, String> w = new LinkedHashMap<>();
   final Map<String, io.karatelabs.ext.rules.ImportMapping.UnitConversion> x = new LinkedHashMap<>();
   final Map<String, Integer> y = new LinkedHashMap<>();
   final Map<String, Integer> z = new LinkedHashMap<>();
   final Map<String, String> A = new LinkedHashMap<>();
   final Map<String, List<String>> B;
   final BidirectionalValueMapping C;
   final boolean D;
   final Map<String, Object> E;

   private ImportMapping(String var1, String var2, Map<String, Object> var3) {
      this.i = var1;
      this.j = var2;
      this.k = Boolean.TRUE.equals(var3.get("ai"));
      var var4 = b(var3.get("source"), "source");
      this.l = var4.get("sheet") == null ? null : c(var4.get("sheet"), "source.sheet");
      this.m = a(var4.get("headerRow"), "source.headerRow");
      this.n = var4.get("endRow") == null ? null : a(var4.get("endRow"), "source.endRow");
      if (this.n != null && this.n <= this.m) {
         throw a("source.endRow is below the header row");
      } else {
         var var5 = b(var3.get("target"), "target");
         this.o = c(var5.get("kind"), "target.kind");
         if (!this.o.equals("table") && !this.o.equals("ratebook")) {
            throw a("target.kind is 'table' or 'ratebook'");
         } else {
            for (Entry var7 : b(var3.get("fields"), "fields").entrySet()) {
               String var8 = c(var7.getValue(), "fields." + (String)var7.getKey());
               if (this.u.containsValue(var8)) {
                  throw new RuleApiException(400, "MAPPING_FIELD_COLLISION", "fields: two headers name the column '" + var8 + "'");
               }

               this.u.put((String)var7.getKey(), var8);
            }

            if (this.u.isEmpty()) {
               throw a("fields maps each source header to a column");
            } else {
               for (Entry var15 : b(var3.get("columns"), "columns").entrySet()) {
                  if (!(var15.getValue() instanceof String var30) || !b.contains(var30)) {
                     throw a("columns." + (String)var15.getKey() + " is one of " + new TreeSet<String>(b));
                  }

                  this.w.put((String)var15.getKey(), var30);
               }

               ArrayList<java.lang.String> var14 = new ArrayList();
               if (var3.get("keys") != null) {
                  Object var31 = var3.get("keys");
                  if (!(var31 instanceof List)) {
                     throw a("keys is a list of column names");
                  }

                  for (Object var48 : (List)var31) {
                     var14.add(c(var48, "keys[]"));
                  }
               }

               this.v = List.copyOf(var14);
               if (this.o.equals("table")) {
                  this.p = c(var5.get("role"), "target.role");
                  if (!f.contains(this.p)) {
                     throw a("target.role is one of " + new TreeSet<String>(f));
                  }

                  this.q = c(var5.get("table"), "target.table");

                  for (String var10 : this.q.split("\\.", -1)) {
                     if (!var10.matches("[A-Za-z0-9_-]+")) {
                        throw a("target.table '" + this.q + "' is a dotted key of names");
                     }
                  }

                  this.r = null;
                  if (var5.get("layers") != null && var5.get("jurisdiction") != null) {
                     throw a("target takes layers (one source split) or jurisdiction (one source per jurisdiction), not both");
                  }

                  this.s = var5.get("layers") == null ? null : this.a(b(var5.get("layers"), "target.layers"));
                  this.t = var5.get("jurisdiction") == null ? null : c(var5.get("jurisdiction"), "target.jurisdiction");

                  for (String var34 : this.v) {
                     if (!this.u.containsValue(var34)) {
                        throw a("key '" + var34 + "' is no column fields names");
                     }

                     if (this.w.containsKey(var34)) {
                        throw a("column '" + var34 + "' is a key and a value column");
                     }
                  }

                  for (String var35 : this.u.values()) {
                     if (!this.v.contains(var35) && !this.w.containsKey(var35)) {
                        throw a("column '" + var35 + "' has no declared type (columns) and is no key");
                     }
                  }

                  for (String var36 : this.w.keySet()) {
                     if (!this.u.containsValue(var36)) {
                        throw a("columns." + var36 + " is no column fields names");
                     }
                  }

                  if (this.w.isEmpty()) {
                     throw a("columns declares each value column's type");
                  }
               } else {
                  this.p = null;
                  this.q = null;
                  this.s = null;
                  this.t = null;
                  this.r = c(var5.get("name"), "target.name");
                  if (!this.r.matches("[A-Za-z0-9_-]+") || this.r.equals("rule")) {
                     throw a("target.name '" + this.r + "' — letters, digits, '_' or '-', and not 'rule'");
                  }

                  if (!this.v.isEmpty()) {
                     throw a("a ratebook's identity is code + edition + line — it takes no keys");
                  }

                  for (String var37 : this.u.values()) {
                     if (!g.contains(var37)) {
                        throw a("column '" + var37 + "' is no record field " + g);
                     }

                     if (!"string".equals(this.w.get(var37))) {
                        throw a("columns." + var37 + " is 'string' — every record field is text");
                     }
                  }

                  for (String var38 : g.subList(0, 6)) {
                     if (!this.u.containsValue(var38)) {
                        throw a("fields maps no header to the record field '" + var38 + "'");
                     }
                  }
               }

               for (Entry var39 : b(var3.get("units"), "units").entrySet()) {
                  String var50 = this.a((String)var39.getKey(), "units");
                  if (!var50.equals("number") && !var50.equals("integer")) {
                     throw a("units." + (String)var39.getKey() + " names a " + var50 + " column");
                  }

                  var var55 = b(var39.getValue(), "units." + (String)var39.getKey());
                  String var11 = c(var55.get("source"), "units." + (String)var39.getKey() + ".source");
                  String var12 = c(var55.get("target"), "units." + (String)var39.getKey() + ".target");
                  if (!c.contains(var11)) {
                     throw a("units." + (String)var39.getKey() + ".source is one of " + new TreeSet<String>(c));
                  }

                  if (!d.contains(var12)) {
                     throw a("units." + (String)var39.getKey() + ".target is one of " + new TreeSet<String>(d));
                  }

                  this.x.put((String)var39.getKey(), new io.karatelabs.ext.rules.ImportMapping.UnitConversion(var11, var12));
               }

               for (Entry var40 : b(var3.get("places"), "places").entrySet()) {
                  String var51 = this.a((String)var40.getKey(), "places");
                  int var56 = a(var40.getValue(), "places." + (String)var40.getKey(), 0, 12);
                  if (var51.equals("integer") && var56 != 0) {
                     throw a("places." + (String)var40.getKey() + " is 0 for an integer column");
                  }

                  if (!var51.equals("number") && !var51.equals("integer")) {
                     throw a("places." + (String)var40.getKey() + " names a " + var51 + " column");
                  }

                  this.y.put((String)var40.getKey(), var56);
               }

               for (Entry var41 : this.w.entrySet()) {
                  if (((String)var41.getValue()).equals("integer")) {
                     this.y.putIfAbsent((String)var41.getKey(), 0);
                  } else if (((String)var41.getValue()).equals("number") && !this.y.containsKey(var41.getKey())) {
                     throw a("places." + (String)var41.getKey() + " declares the decimals a number column is written with");
                  }
               }

               for (Entry var42 : b(var3.get("pad"), "pad").entrySet()) {
                  if (!this.v.contains(var42.getKey())) {
                     throw a("pad." + (String)var42.getKey() + " names no key column");
                  }

                  this.z.put((String)var42.getKey(), a(var42.getValue(), "pad." + (String)var42.getKey(), 1, 32));
               }

               for (Entry var43 : b(var3.get("transforms"), "transforms").entrySet()) {
                  if (!"string".equals(this.w.get(var43.getKey())) && !this.v.contains(var43.getKey())) {
                     throw a("transforms." + (String)var43.getKey() + " names no string or key column");
                  }

                  String var52 = c(var43.getValue(), "transforms." + (String)var43.getKey());
                  if (!e.contains(var52)) {
                     throw a("transforms." + (String)var43.getKey() + " is one of " + new TreeSet<String>(e));
                  }

                  this.A.put((String)var43.getKey(), var52);
               }

               if (this.o.equals("ratebook")) {
                  for (String var44 : this.u.values()) {
                     if (h.contains(var44) && !this.A.containsKey(var44)) {
                        throw a("transforms." + var44 + " declares how the date reads ('date' ISO as read, 'mdy' MM/DD/YYYY)");
                     }
                  }
               }

               var var29 = b(var3.get("values"), "values");

               for (String var53 : (Iterable<String>) (Iterable<?>) (var29.keySet())) {
                  if (!this.v.contains(var53) && !"string".equals(this.w.get(var53))) {
                     throw a("values." + var53 + " names no key or string column");
                  }
               }

               this.C = new BidirectionalValueMapping(var29, UnaryOperator.identity());
               this.B = io.karatelabs.ext.rules.BidirectionalValueMapping.a(b(var3.get("format"), "format"));

               for (String var54 : this.B.keySet()) {
                  String var57 = this.a(var54, "format");
                  if (!var57.equals("number") && !var57.equals("integer")) {
                     throw a("format." + var54 + " names a " + var57 + " column");
                  }
               }

               Object var47 = var3.get("formulas");
               if (var47 != null && !"cached".equals(var47)) {
                  throw a("formulas is 'cached' (take a formula cell's cached value) or absent (a formula refuses)");
               } else {
                  this.D = var47 != null;
                  this.E = b(var3.get("provenance"), "provenance");
                  if (this.E.isEmpty()) {
                     throw a("provenance is required");
                  } else if (this.p != null && this.E.get("role") != null && !this.p.equals(this.E.get("role"))) {
                     throw a("provenance.role '" + this.E.get("role") + "' is not target.role '" + this.p + "'");
                  }
               }
            }
         }
      }
   }

   static ImportMapping a(Path var0, String var1) {
      if (!var1.matches("[A-Za-z0-9_-]+")) {
         throw a("name '" + var1 + "' — letters, digits, '_' or '-' only");
      } else {
         Path var2 = var0.resolve("evidence/mappings").resolve(var1 + ".import.json");
         if (!Files.isRegularFile(var2)) {
            throw new RuleApiException(
               404, "MAPPING_NOT_FOUND", "no import mapping '" + var1 + "' at " + var0.getFileName() + "/evidence/mappings/" + var1 + ".import.json"
            );
         } else {
            byte[] var3;
            try {
               var3 = Files.readAllBytes(var2);
            } catch (IOException var7) {
               throw new UncheckedIOException(var7);
            }

            Object var4;
            try {
               var4 = Json.parseStrict(new String(var3, StandardCharsets.UTF_8), true);
            } catch (RuntimeException var6) {
               throw new RuleApiException(400, "PARSE_FAILED", "import mapping '" + var1 + "': " + var6.getMessage());
            }

            return new ImportMapping(var1, io.karatelabs.ext.common.Sha256.a(var3), b(var4, "mapping"));
         }
      }
   }

   static ImportMapping a(Path var0, Object var1) {
      if (var1 instanceof String var3) {
         return a(var0, var3);
      } else if (var1 instanceof Map var2) {
         return new ImportMapping(null, io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2)), var2);
      } else {
         throw a("mapping is a name under evidence/mappings/ (<name>.import.json) or an inline block");
      }
   }

   void a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1) {
      if (var1 != null && !var1.b().equals(this.v)) {
         throw new RuleApiException(400, "IMPORT_KEYS_MISMATCH", "keys " + this.v + " are not table '" + this.q + "''s keys " + var1.b());
      }
   }

   Map<String, Object> a() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (this.i != null) {
         var1.put("name", this.i);
      }

      var1.put("sha", this.j);
      if (this.k) {
         var1.put("ai", true);
      }

      return var1;
   }

   private String a(String var1, String var2) {
      String var3 = this.w.get(var1);
      if (var3 == null) {
         throw a(var2 + "." + var1 + " names no value column");
      } else {
         return var3;
      }
   }

   private io.karatelabs.ext.rules.ImportMapping.LayerMapping a(Map<String, Object> var1) {
      String var2 = c(var1.get("column"), "target.layers.column");
      String var3 = c(var1.get("countrywide"), "target.layers.countrywide");
      if (var1.get("jurisdictions") instanceof List var4 && !var4.isEmpty()) {
         ArrayList<java.lang.String> var9 = new ArrayList();

         for (Object var7 : var4) {
            String var8 = c(var7, "target.layers.jurisdictions[]");
            if (var9.contains(var8) || var8.equals(var3)) {
               throw a("target.layers.jurisdictions is a list of distinct names, the countrywide value not among them");
            }

            var9.add(var8);
         }

         if (this.u.containsKey(var2)) {
            throw a("target.layers.column '" + var2 + "' is a mapped header — the jurisdiction is not a table column");
         } else {
            return new io.karatelabs.ext.rules.ImportMapping.LayerMapping(var2, var3, List.copyOf(var9));
         }
      } else {
         throw a("target.layers.jurisdictions lists the jurisdictions the table layers");
      }
   }

   private static int a(Object var0, String var1) {
      return a(var0, var1, 1, Integer.MAX_VALUE);
   }

   private static int a(Object var0, String var1, int var2, int var3) {
      if (var0 instanceof Number var4 && var4.doubleValue() == Math.rint(var4.doubleValue()) && var4.intValue() >= var2 && var4.intValue() <= var3) {
         return var4.intValue();
      } else {
         throw a(var1 + " is a whole number " + var2 + (var3 == Integer.MAX_VALUE ? " or more" : ".." + var3) + ", not " + var0);
      }
   }

   private static Map<String, Object> b(Object var0, String var1) {
      if (var0 == null) {
         return Map.of();
      } else if (!(var0 instanceof Map)) {
         throw a(var1 + " is an object");
      } else {
         return (Map<String, Object>)var0;
      }
   }

   private static String c(Object var0, String var1) {
      if (var0 instanceof String var2 && !var2.isEmpty()) {
         return var2;
      } else {
         throw a(var1 + " is a non-empty string");
      }
   }

   static RuleApiException a(String var0) {
      return new RuleApiException(400, "MAPPING_INVALID", "import mapping: " + var0);
   }

   record LayerMapping(String a, String b, List<String> c) {

      LayerMapping(String a, String b, List<String> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   record UnitConversion(String a, String b) {

      UnitConversion(String a, String b) {
         this.a = a;
         this.b = b;
      }
   }
}
