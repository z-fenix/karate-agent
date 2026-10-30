package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.rules.imports.ImportReader;
import io.karatelabs.ext.rules.imports.xlsx.XlsxImportReader;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RuleTableCatalog {
   static final String a = "tables";
   static final String b = ".meta.json";
   private static final String d = ".xlsx";
   private static final Set<String> e = Set.of("number", "integer", "string", "boolean");
   private static final Pattern f = Pattern.compile("[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?");
   public static final RuleTableCatalog c = new RuleTableCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), false, io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.a);
   private final Map<String, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition> g;
   private final Map<String, String> h;
   private final Map<String, Object> i;
   private final Map<String, Object> j;
   private final Map<String, byte[]> k;
   private final Map<String, List<Map<String, Object>>> l;
   private final Map<String, io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable> m;
   private final boolean n;
   private final io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope o;
   private final Map<String, Map<List<String>, Map<String, Object>>> p = new ConcurrentHashMap<>();
   private static final Map<Path, SoftReference<RuleTableCatalog>> q = Collections.synchronizedMap(new LinkedHashMap<Path, SoftReference<RuleTableCatalog>>(16, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Path, SoftReference<RuleTableCatalog>> var1) {
         return this.size() > 32;
      }
   });

   private RuleTableCatalog(
      Map<String, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition> var1,
      Map<String, String> var2,
      Map<String, Object> var3,
      Map<String, Object> var4,
      Map<String, byte[]> var5,
      Map<String, List<Map<String, Object>>> var6,
      Map<String, io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable> var7,
      boolean var8,
      io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope var9
   ) {
      this.g = var1;
      this.h = var2;
      this.i = var3;
      this.j = var4;
      this.k = var5;
      this.l = var6;
      this.m = var7;
      this.n = var8;
      this.o = var9;
   }

   public boolean a() {
      return this.g.isEmpty();
   }

   public Map<String, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition> b() {
      return this.g;
   }

   public Map<String, String> c() {
      return this.h;
   }

   public Map<String, Object> d() {
      return this.i;
   }

   public Map<String, Object> e() {
      return this.j;
   }

   public Map<String, Object> a(Map<String, Object> var1, Set<String> var2) {
      for (String var4 : this.g.keySet()) {
         for (String var6 : var2) {
            if (var4.equals(var6) || var4.startsWith(var6 + ".")) {
               throw f(var4, "the inline key '" + var6 + "'");
            }
         }
      }

      var var7 = new LinkedHashMap<>(var1);
      var7.putAll(this.i);
      return var7;
   }

   public String a(String var1) {
      byte[] var2 = this.k.get(var1);
      return var2 == null ? null : new String(var2, StandardCharsets.UTF_8);
   }

   RuleTableCatalog b(String var1) {
      String var2 = this.o.a() + "/" + var1 + "/";
      TreeMap<java.lang.String, byte[]> var3 = new TreeMap();
      this.k.forEach((var2x, var3x) -> {
         if (var2x.startsWith(var2)) {
            var3.put(var2x, var3x);
         }
      });
      RuleTableCatalog var4 = var3.isEmpty() ? c : a(var3, false, null, new io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope(this.o.a() + "/" + var1, this.o.b()));
      return var4.a() ? null : var4;
   }

   public RuleTableCatalog a(String var1, String var2) {
      if (!this.k.containsKey(var1)) {
         throw b(var1, "no such table file");
      } else {
         e(var1);
         var var3 = new TreeMap<>(this.k);
         var3.put(var1, var2.getBytes(StandardCharsets.UTF_8));
         return a(var3, false, this, this.o);
      }
   }

   private static void e(String var0) {
      if (var0.endsWith(".xlsx")) {
         throw b(var0, "an .xlsx table is read-only — edit the workbook, or Rule.import it to a CSV table");
      }
   }

   public RuleTableCatalog a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, Object var2) {
      return this.a(var1.d(), this.a(var1.d(), Map.of(var1.f(), Map.of(var1.g(), var2))));
   }

   public void b(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, Object var2) {
      String var3 = this.g.get(var1.c()).c().get(var1.g());
      a(var1.d(), 0, var1.g(), var1.d().endsWith(".csv") ? a(var2) : var2, var3);
   }

   public RuleTableCatalog c(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, Object var2) {
      var var3 = new TreeMap<>(this.k);
      var3.put(var1.d(), this.a(var1.d(), Map.of(var1.f(), Map.of(var1.g(), var2))).getBytes(StandardCharsets.UTF_8));
      return a(var3, true, null, this.o);
   }

   private static String a(String var0, boolean var1) {
      return var1 && "integer".equals(var0) ? "number" : var0;
   }

   public io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition c(String var1) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var2 = null;

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var4 : this.g.values()) {
         if ((var1.equals(var4.a()) || var1.startsWith(var4.a() + ".")) && (var2 == null || var4.a().length() > var2.a().length())) {
            var2 = var4;
         }
      }

      return var2;
   }

   public List<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> f() {
      ArrayList<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 : this.g.values()) {
         if (!var3.b().isEmpty()) {
            for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var5 : var3.g()) {
               for (Map var7 : this.l.get(var5.a())) {
                  for (String var9 : var3.c().keySet()) {
                     var1.add(a(var3, var5, var7, var9));
                  }
               }
            }
         }
      }

      return var1;
   }

   private static io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var0, io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var1, Map<String, Object> var2, String var3) {
      ArrayList<java.lang.String> var4 = new ArrayList();

      for (String var6 : var0.b()) {
         var4.add((String)var2.get(var6));
      }

      return new io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference(var0.a(), var1.a(), var1.c(), List.copyOf(var4), var3, var2.get(var3), var0.c().size() == 1, var0.d() != null);
   }

   public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference d(String var1) {
      String var4 = null;
      boolean var5 = var1.contains("/");
      String var2;
      ArrayList var3;
      if (var5) {
         var var6 = List.of(var1.split("/", -1));
         var2 = (String)var6.get(0);
         var3 = new ArrayList(var6.subList(1, var6.size()));
         int var7 = var2.indexOf(64);
         if (var7 >= 0) {
            var4 = var2.substring(var7 + 1);
            var2 = var2.substring(0, var7);
         }
      } else {
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var10 = this.c(var1);
         if (var10 == null || var1.equals(var10.a())) {
            return null;
         }

         var2 = var10.a();
         var3 = new ArrayList<>(List.of(var1.substring(var10.a().length() + 1).split("\\.", -1)));
      }

      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var11 = this.g.get(var2);
      if (var11 != null && !var11.b().isEmpty()) {
         boolean var12 = var11.d() != null;
         if (var12 && !var5) {
            if (var3.isEmpty()) {
               return null;
            }

            var4 = (String)var3.remove(0);
         } else if (!var12 && (var5 || var4 != null)) {
            return null;
         }

         if (var3.size() != var11.b().size() + (var11.c().size() == 1 ? 0 : 1)) {
            return null;
         } else {
            List var8 = var3.subList(0, var11.b().size());
            String var9 = var11.c().size() == 1 ? var11.c().keySet().iterator().next() : (String)var3.get(var3.size() - 1);
            return this.a(var11, var4, var8, var9);
         }
      } else {
         return null;
      }
   }

   public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1, String var2, List<String> var3, String var4) {
      if (var1.c().containsKey(var4) && var3.size() == var1.b().size()) {
         io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var5 = var1.g().get(0);
         if (var1.d() != null && var2 != null) {
            String var6 = var1.h().get(var2 + "/" + String.join("/", var3));
            if (var6 == null) {
               return null;
            }

            var5 = var1.g().stream().filter(var1x -> var1x.a().equals(var6)).findFirst().orElseThrow();
         }

         var var7 = this.b(var1, var5.a()).get(var3);
         return var7 == null ? null : a(var1, var5, var7, var4);
      } else {
         return null;
      }
   }

   public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a(String var1, String var2, List<String> var3, String var4) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var5 = this.g.get(var1);
      if (var5 != null && var5.c().containsKey(var4) && var3.size() == var5.b().size() && !var5.b().isEmpty()) {
         for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var7 : var5.g()) {
            if (Objects.equals(var7.c(), var2)) {
               var var8 = this.b(var5, var7.a()).get(var3);
               return var8 == null ? null : a(var5, var7, var8, var4);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public List<String> a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var2 = this.g.get(var1.c());
      if (var2.d() == null) {
         return Collections.singletonList(null);
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (String var5 : var2.e()) {
            if (var1.d().equals(var2.h().get(var5 + "/" + String.join("/", var1.f())))) {
               var3.add(var5);
            }
         }

         return var3;
      }
   }

   Map<List<String>, Map<String, Object>> a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1, String var2) {
      LinkedHashMap<java.util.List<java.lang.String>, java.util.Map<java.lang.String,java.lang.Object>> var3 = new LinkedHashMap();

      for (Map var5 : this.l.get(var2)) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (String var8 : var1.b()) {
            var6.add((String)var5.get(var8));
         }

         var3.put(List.copyOf(var6), var5);
      }

      return var3;
   }

   private Map<List<String>, Map<String, Object>> b(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1, String var2) {
      return this.p.computeIfAbsent(var2, var2x -> {
         HashMap<List<String>, Map<String, Object>> var3 = new HashMap<>();

         for (Map var5 : this.l.get(var2x)) {
            ArrayList<java.lang.String> var6 = new ArrayList();

            for (String var8 : var1.b()) {
               var6.add((String)var5.get(var8));
            }

            var3.putIfAbsent(var6, var5);
         }

         return var3;
      });
   }

   public List<String> b(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var2 = this.g.get(var1.c());
      String var3 = String.join(".", var1.f()) + (var1.i() ? "" : "." + var1.g());
      if (var2.d() == null) {
         return List.of(var2.a() + "." + var3);
      } else {
         ArrayList<java.lang.String> var4 = new ArrayList();

         for (String var6 : var2.e()) {
            if (var1.d().equals(var2.h().get(var6 + "/" + String.join("/", var1.f())))) {
               var4.add(var2.a() + "." + var6 + "." + var3);
            }
         }

         return var4;
      }
   }

   public Map<String, Object> a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("key", var1.a());
      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var5 : var1.g()) {
         var3.add(String.valueOf(var5.d().get("role")));
      }

      var2.put("role", var3.size() == 1 ? var3.iterator().next() : List.copyOf(var3));
      var2.put("rows", var1.i());
      var2.put("keys", var1.b());
      var2.put("columns", var1.c());
      if (var1.d() != null) {
         var2.put("layers", var1.d());
         var2.put("jurisdictions", var1.e());
      }

      if (var1.f()) {
         var2.put("ai", true);
      }

      ArrayList<java.util.LinkedHashMap> var8 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var6 : var1.g()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("path", var6.a());
         var7.put("sha256", var6.b());
         var7.put("rows", this.l.get(var6.a()).size());
         if (var6.c() != null) {
            var7.put("jurisdiction", var6.c());
         }

         var8.add(var7);
      }

      var2.put("files", var8);
      return var2;
   }

   public Map<String, Object> a(Map<String, Object> var1) {
      var var2 = new LinkedHashMap<>(var1);
      LinkedHashMap var3 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var5 : this.g.values()) {
         a(var3, var5.a(), Map.of("_table", this.a(var5)));
      }

      var2.putAll(var3);
      return var2;
   }

   public Map<String, Object> a(String var1, int var2, int var3, int var4) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var5 = this.g.get(var1);
      if (var5 == null) {
         throw new RuleApiException(404, "TABLE_NOT_FOUND", "no file table '" + var1 + "' (tables: " + this.g.keySet() + ")");
      } else {
         int var6 = Math.max(0, Math.min(var3 <= 0 ? var4 : var3, var4));
         ArrayList<java.util.LinkedHashMap> var7 = new ArrayList();

         for (io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry var9 : var5.g()) {
            for (Map var11 : this.l.get(var9.a())) {
               LinkedHashMap<java.lang.String, java.lang.String> var12 = new LinkedHashMap(var11);
               var12.put("_file", var9.a());
               var7.add(var12);
            }
         }

         int var13 = Math.max(0, Math.min(var2, var7.size()));
         int var14 = Math.min(var7.size(), var13 + var6);
         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
         var15.put("key", var1);
         var15.put("offset", var13);
         var15.put("limit", var6);
         var15.put("total", var7.size());
         var15.put("rows", var7.subList(var13, var14));
         var15.put("truncated", var14 < var7.size());
         if (var14 < var7.size()) {
            var15.put("next", var14);
         }

         return var15;
      }
   }

   public Map<String, Object> a(Consumer<String> var1) {
      return this.b(var1x -> var1.accept(var1x.a().a()));
   }

   public Map<String, Object> b(Consumer<io.karatelabs.ext.rules.RuleTableCatalog.TableCellLookup> var1) {
      return new io.karatelabs.ext.rules.RuleTableCatalog.TableLookupView(this.j, "", null, null, List.of(), var1, null, null);
   }

   public Map<String, Object> d(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, Object var2) {
      return new io.karatelabs.ext.rules.RuleTableCatalog.TableLookupView(this.j, "", null, null, List.of(), var0 -> {}, var1.a(), var2);
   }

   public String a(String var1, Map<List<String>, Map<String, Object>> var2) {
      String var3 = this.a(var1);
      if (var3 == null) {
         throw b(var1, "no such table file");
      } else {
         e(var1);
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var4 = this.g
            .values()
            .stream()
            .filter(var1x -> var1x.g().stream().anyMatch(var1xx -> var1xx.a().equals(var1)))
            .findFirst()
            .orElseThrow(() -> b(var1, "a sidecar is not a table file"));
         ArrayList<int[]> var5 = new ArrayList();
         ArrayList<java.lang.String> var6 = new ArrayList();
         var var7 = new LinkedHashMap<>(var2);
         if (var1.endsWith(".csv")) {
            var var17 = e(var1, var3);
            ArrayList<java.lang.String> var19 = new ArrayList();

            for (int[] var25 : (Iterable<int[]>) (Iterable<?>) ((List)var17.get(0))) {
               var19.add(f(var3.substring(var25[0], var25[1])));
            }

            for (int var22 = 1; var22 < var17.size(); var22++) {
               List var26 = (List)var17.get(var22);
               ArrayList<java.lang.String> var28 = new ArrayList();

               for (String var33 : var4.b()) {
                  int[] var36 = (int[])var26.get(var19.indexOf(var33));
                  var28.add(f(var3.substring(var36[0], var36[1])));
               }

               Map var31 = (Map)var7.remove(var28);
               if (var31 != null) {
                  for (Entry var37 : (Iterable<Entry>) (Iterable<?>) (var31.entrySet())) {
                     int var38 = var19.indexOf(var37.getKey());
                     if (var38 < 0 || var4.b().contains(var37.getKey())) {
                        throw b(var1, "no value column '" + (String)var37.getKey() + "'");
                     }

                     var5.add((int[])var26.get(var38));
                     var6.add(b(var37.getValue()));
                  }
               }
            }
         } else {
            io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var8 = io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode.a(var1, var3);
            io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var9 = var8.c == null ? null : var8.c.get("rows");
            if (var9 == null || var9.d == null) {
               throw b(var1, "'rows' is a list of row objects");
            }

            for (io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var11 : var9.d) {
               ArrayList<java.lang.Object> var12 = new ArrayList();

               for (String var14 : var4.b()) {
                  io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var15 = var11.c == null ? null : var11.c.get(var14);
                  var12.add(var15 == null ? null : var15.a());
               }

               Map var29 = (Map)var7.remove(var12);
               if (var29 != null) {
                  for (Entry var35 : (Iterable<Entry>) (Iterable<?>) (var29.entrySet())) {
                     io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var16 = var11.c.get(var35.getKey());
                     if (var16 == null || var4.b().contains(var35.getKey())) {
                        throw b(var1, "no value column '" + (String)var35.getKey() + "'");
                     }

                     var5.add(new int[]{var16.a, var16.b});
                     var6.add(c(var35.getValue()));
                  }
               }
            }
         }

         if (!var7.isEmpty()) {
            throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", var1 + " holds no row " + var7.keySet().iterator().next());
         } else {
            StringBuilder var18 = new StringBuilder(var3);
            ArrayList<java.lang.Integer> var20 = new ArrayList();

            for (int var23 = 0; var23 < var5.size(); var23++) {
               var20.add(var23);
            }

            var20.sort((var1x, var2x) -> ((int[])var5.get(var2x))[0] - ((int[])var5.get(var1x))[0]);

            for (int var27 : (Iterable<java.lang.Integer>) (Iterable<?>) (var20)) {
               var18.replace(((int[])var5.get(var27))[0], ((int[])var5.get(var27))[1], (String)var6.get(var27));
            }

            return var18.toString();
         }
      }
   }

   private static String a(Object var0) {
      return var0 instanceof Number var1 ? Terms.numberToString(Terms.narrow(var1.doubleValue())) : String.valueOf(var0);
   }

   private static String b(Object var0) {
      String var1 = a(var0);
      return !var1.contains(",") && !var1.contains("\"") && !var1.contains("\n") && !var1.contains("\r") ? var1 : "\"" + var1.replace("\"", "\"\"") + "\"";
   }

   private static String c(Object var0) {
      return var0 instanceof String var1 ? Json.stringifyStrict(List.of(var1)).replaceAll("^\\[|\\]$", "") : a(var0);
   }

   private static String f(String var0) {
      return var0.startsWith("\"") && var0.endsWith("\"") && var0.length() >= 2 ? var0.substring(1, var0.length() - 1).replace("\"\"", "\"") : var0;
   }

   public static RuleTableCatalog a(Path var0) {
      Path var1 = var0.resolve("tables");
      if (!Files.isDirectory(var1)) {
         return c;
      } else {
         TreeMap<java.lang.String, byte[]> var2 = new TreeMap();

         try (var var3 = Files.walk(var1)) {
            for (Path var5 : (Iterable<Path>) (var3::iterator)) {
               if (Files.isRegularFile(var5) && !var5.getFileName().toString().startsWith(".")) {
                  var2.put("tables/" + var1.relativize(var5).toString().replace('\\', '/'), Files.readAllBytes(var5));
               }
            }
         } catch (IOException var9) {
            throw new UncheckedIOException(var9);
         }

         Path var10 = var1.toAbsolutePath().normalize();
         SoftReference var11 = q.get(var10);
         RuleTableCatalog var12 = var11 == null ? null : (RuleTableCatalog)var11.get();
         if (var12 != null && a(var12.k, var2)) {
            return var12;
         } else {
            RuleTableCatalog var6 = a(var2, false, var12, io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.a);
            q.put(var10, new SoftReference<>(var6));
            return var6;
         }
      }
   }

   private static boolean a(Map<String, byte[]> var0, Map<String, byte[]> var1) {
      if (!var0.keySet().equals(var1.keySet())) {
         return false;
      } else {
         for (Entry var3 : var0.entrySet()) {
            if (!Arrays.equals((byte[])var3.getValue(), (byte[])var1.get(var3.getKey()))) {
               return false;
            }
         }

         return true;
      }
   }

   static RuleTableCatalog b(Map<String, byte[]> var0) {
      return a(var0, false, null, io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.a);
   }

   static RuleTableCatalog a(Map<String, byte[]> var0, io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope var1) {
      return a(var0, false, null, var1);
   }

   private static RuleTableCatalog a(Map<String, byte[]> var0, boolean var1, RuleTableCatalog var2, io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope var3) {
      var var4 = new TreeMap<>(var0);
      LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.ArrayList> var6 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.String> var7 = new LinkedHashMap();

      for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         String var10 = (String)var9.getKey();
         var5.put(var10, io.karatelabs.ext.common.Sha256.a((byte[])var9.getValue()));
         String var11 = new String((byte[])var9.getValue(), StandardCharsets.UTF_8);
         if (var10.endsWith(".meta.json")) {
            var7.put(var10.substring(0, var10.length() - ".meta.json".length()), var11);
         } else {
            if (!var10.endsWith(".csv") && !var10.endsWith(".json") && !var10.endsWith(".xlsx")) {
               throw b(var10, "only .csv, .json and .xlsx tables are read");
            }

            io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var12 = io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile.a(var3.a(), var10, var10.endsWith(".xlsx") ? a(var10, (byte[])var9.getValue()) : var11);
            var6.computeIfAbsent(var12.c, var0x -> new ArrayList<>()).add(var12);
         }
      }

      for (String var21 : (Iterable<String>) (Iterable<?>) (var7.keySet())) {
         if (!var4.containsKey(var21)) {
            throw b(var21 + ".meta.json", "a sidecar with no table file");
         }

         if (var21.endsWith(".json")) {
            throw b(var21 + ".meta.json", "a JSON table carries its own metadata");
         }
      }

      var var20 = new ArrayList<>(var6.keySet());

      for (int var22 = 0; var22 < var20.size(); var22++) {
         for (int var24 = var22 + 1; var24 < var20.size(); var24++) {
            if (((String)var20.get(var24)).startsWith((String)var20.get(var22) + ".")) {
               throw f((String)var20.get(var24), "the file key '" + (String)var20.get(var22) + "'");
            }
         }
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition> var23 = new LinkedHashMap();
      LinkedHashMap var25 = new LinkedHashMap();
      LinkedHashMap var26 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.List<java.util.Map<java.lang.String,java.lang.Object>>> var27 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable> var13 = new LinkedHashMap();

      for (Entry var15 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
         io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable var16 = var2 == null ? null : var2.a((String)var15.getKey(), (List<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile>)var15.getValue(), var5, var1);
         if (var16 == null) {
            var16 = a((String)var15.getKey(), (List<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile>)var15.getValue(), var7, var5, var1, var27, var3.b());
         } else {
            for (io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var18 : (Iterable<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile>) (Iterable<?>) ((List)var15.getValue())) {
               var27.put(var18.b, var2.l.get(var18.b));
            }
         }

         var13.put((String)var15.getKey(), var16);
         var23.put((String)var15.getKey(), var16.a);
         a(var25, (String)var15.getKey(), var16.b);
         a(var26, (String)var15.getKey(), var16.c);
      }

      return new RuleTableCatalog(
         Collections.unmodifiableMap(var23),
         Collections.unmodifiableMap(var5),
         (Map<String, Object>)d(var25),
         c((Map<String, Object>)var26),
         Collections.unmodifiableMap(var4),
         Collections.unmodifiableMap(var27),
         var13,
         var1,
         var3
      );
   }

   private io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable a(String var1, List<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile> var2, Map<String, String> var3, boolean var4) {
      io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable var5 = this.m.get(var1);
      if (var5 != null && this.n == var4 && var5.a.g().size() == var2.size()) {
         for (io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var7 : var2) {
            String var8 = var7.b + ".meta.json";
            if (!((String)var3.get(var7.b)).equals(this.h.get(var7.b)) || !Objects.equals(var3.get(var8), this.h.get(var8))) {
               return null;
            }
         }

         return var5;
      } else {
         return null;
      }
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         var1.put((String)var3.getKey(), var3.getValue() instanceof LinkedHashMap var4 ? c((Map<String, Object>)var4) : var3.getValue());
      }

      return new io.karatelabs.ext.rules.RuleTableCatalog.ReadOnlyTableMap(var1, null);
   }

   private static String a(String var0, byte[] var1) {
      Main.requireProduct("rules", "rules");

      ImportReader.Sheet var2;
      try {
         var2 = XlsxImportReader.sheet(var1, null);
      } catch (ImportReader.Refused var11) {
         throw b(var0, var11.getMessage());
      } catch (IOException var12) {
         throw new UncheckedIOException(var12);
      }

      StringBuilder var3 = new StringBuilder();
      int var4 = -1;

      for (ImportReader.Row var6 : var2.rows()) {
         ArrayList<java.lang.String> var7 = new ArrayList();

         for (ImportReader.Cell var9 : var6.cells()) {
            var7.add(XlsxImportReader.text(var9));
         }

         while (!var7.isEmpty() && ((String)var7.get(var7.size() - 1)).isBlank() && (var4 < 0 || var7.size() > var4)) {
            var7.remove(var7.size() - 1);
         }

         if (!var7.stream().allMatch(String::isBlank)) {
            if (var4 < 0) {
               var4 = var7.size();
            }

            for (int var13 = 0; var13 < var7.size(); var13++) {
               String var14 = (String)var7.get(var13);
               boolean var10 = var14.contains(",") || var14.contains("\"") || var14.contains("\n") || var14.contains("\r");
               var3.append(var13 == 0 ? "" : ",").append(var10 ? "\"" + var14.replace("\"", "\"\"") + "\"" : var14);
            }

            var3.append('\n');
         }
      }

      return var3.toString();
   }

   private static io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable a(
      String var0,
      List<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile> var1,
      Map<String, String> var2,
      Map<String, String> var3,
      boolean var4,
      Map<String, List<Map<String, Object>>> var5,
      Set<String> var6
   ) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var7 = null;
      ArrayList<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile> var8 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var10 : var1) {
         if (var10.d == null) {
            if (var7 != null) {
               throw f(var0, "the file key '" + var0 + "' (" + var7.b + ")");
            }

            var7 = var10;
         } else {
            var8.add(var10);
         }
      }

      if (var7 == null) {
         throw b(((io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile)var8.get(0)).b, "an override without a countrywide '" + var0 + "' file");
      } else {
         io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var28 = a(var7, var2, null, var6);
         if (var28.c == null && !var8.isEmpty()) {
            throw b(((io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile)var8.get(0)).b, "table '" + var0 + "' declares no layers");
         } else {
            ArrayList<io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry> var29 = new ArrayList();
            var29.add(new io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry(var7.b, (String)var3.get(var7.b), null, var28.e, var28.f));
            boolean var11 = var28.f;
            var var12 = a(var7, var28, var4);
            var5.put(var7.b, var12);
            Map<String, String> var15 = Map.of();
            Object var13;
            Object var14;
            if (var28.c == null) {
               var13 = a(var12, var28);
               var14 = a(var13, null);
            } else {
               LinkedHashMap<java.lang.String, java.util.List> var16 = new LinkedHashMap();
               LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile> var17 = new LinkedHashMap();

               for (io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var19 : (Iterable<io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile>) (Iterable<?>) (var8)) {
                  if (!var28.d.contains(var19.d)) {
                     throw b(var19.b, "jurisdiction '" + var19.d + "' is not one table '" + var0 + "' lists");
                  }

                  if (var17.put(var19.d, var19) != null) {
                     throw f(var0 + "@" + var19.d, "the file key '" + var0 + "@" + var19.d + "'");
                  }

                  io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var20 = a(var19, var2, var28, var6);
                  if (!var20.a.equals(var28.a) || !var20.b.equals(var28.b)) {
                     throw b(var19.b, "an override's keys and columns are the countrywide file's");
                  }

                  if (var20.c != null || var20.d != null) {
                     throw b(var19.b, "layers are declared on the countrywide file");
                  }

                  Object var21 = var20.e.get("jurisdiction");
                  if (var21 != null && !var19.d.equals(var21)) {
                     throw b(var19.b, "provenance names jurisdiction '" + var21 + "'");
                  }

                  var11 |= var20.f;
                  var29.add(new io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry(var19.b, (String)var3.get(var19.b), var19.d, var20.e, var20.f));
                  var16.put(var19.d, a(var19, var20, var4));
                  var5.put(var19.b, (List)var16.get(var19.d));
               }

               LinkedHashMap<java.lang.String, java.lang.String> var31 = new LinkedHashMap();
               LinkedHashMap<java.lang.String, java.lang.Object> var32 = new LinkedHashMap();
               LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();

               for (String var22 : var28.d) {
                  LinkedHashMap<String, Map<String, Object>> var23 = new LinkedHashMap<>();

                  for (Map var25 : (Iterable<Map>) (Iterable<?>) (var12)) {
                     String var26 = a(var25, var28.a);
                     var23.put(var26, var25);
                     var31.put(var22 + "/" + var26, var7.b);
                  }

                  io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var35 = (io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile)var17.get(var22);

                  for (Map var38 : (Iterable<Map>) (Iterable<?>) (var16.getOrDefault(var22, List.of()))) {
                     String var27 = a(var38, var28.a);
                     var23.put(var27, var38);
                     var31.put(var22 + "/" + var27, var35.b);
                  }

                  Object var37 = a(new ArrayList<>(var23.values()), var28);
                  var32.put(var22, var37);
                  var33.put(var22, a(var37, "table '" + var0 + "@" + var22 + "'"));
               }

               var13 = Collections.unmodifiableMap(var32);
               var14 = new io.karatelabs.ext.rules.RuleTableCatalog.ReadOnlyTableMap(var33, "table '" + var0 + "'");
               var15 = Collections.unmodifiableMap(var31);
            }

            io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var30 = new io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition(var0, var28.a, var28.b, var28.c, var28.d, var11, List.copyOf(var29), var15, var12.size());
            return new io.karatelabs.ext.rules.RuleTableCatalog.CompiledTable(var30, var13, var14);
         }
      }
   }

   private static io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration a(io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var0, Map<String, String> var1, io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var2, Set<String> var3) {
      Map var4;
      String var5;
      if (var0.a()) {
         var5 = var0.b + ".meta.json";
         String var6 = (String)var1.get(var0.b);
         if (var6 == null) {
            throw b(var0.b, (var0.b.endsWith(".xlsx") ? "an .xlsx" : "a CSV") + " table needs its .meta.json sidecar");
         }

         var4 = a(var5, c(var5, var6), "the sidecar");
      } else {
         var5 = var0.b;
         var4 = a(var5, c(var5, var0.e), "a JSON table");
      }

      ArrayList<java.lang.String> var21 = new ArrayList();
      Object var7 = var4.get("keys");
      if (var7 == null && var2 != null) {
         var21.addAll(var2.a);
      } else if (var7 != null) {
         if (!(var7 instanceof List)) {
            throw b(var5, "'keys' is a list of column names");
         }

         for (Object var10 : (List)var7) {
            if (!(var10 instanceof String var11 && !var11.isEmpty())) {
               throw b(var5, "'keys' is a list of column names");
            }

            var21.add(var11);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.String> var22 = new LinkedHashMap();
      if (var4.get("columns") == null && var2 != null) {
         var22.putAll(var2.b);
      } else {
         if (!(var4.get("columns") instanceof Map var23) || var23.isEmpty()) {
            throw b(var5, "'columns' declares each value column's type");
         }

         for (Entry var29 : (Iterable<Entry>) (Iterable<?>) (var23.entrySet())) {
            String var12 = String.valueOf(var29.getKey());
            if (var21.contains(var12)) {
               throw b(var5, "column '" + var12 + "' is a key and a value column");
            }

            if (!(var29.getValue() instanceof String var13 && e.contains(var13))) {
               throw b(var5, "column '" + var12 + "' has no type in " + e);
            }

            var22.put(var12, var13);
         }
      }

      String var24 = null;
      List var27 = null;
      if (var4.get("layers") != null) {
         if (!"jurisdiction".equals(var4.get("layers"))) {
            throw b(var5, "'layers' is 'jurisdiction'");
         }

         var24 = "jurisdiction";
         if (!(var4.get("jurisdictions") instanceof List var30) || var30.isEmpty()) {
            throw b(var5, "a layered table lists its 'jurisdictions'");
         }

         ArrayList<java.lang.String> var28 = new ArrayList();

         for (Object var35 : var30) {
            if (!(var35 instanceof String var37 && !var37.isEmpty() && !var28.contains(var37))) {
               throw b(var5, "'jurisdictions' is a list of distinct names");
            }

            var28.add(var37);
         }

         var27 = List.copyOf(var28);
      } else if (var4.get("jurisdictions") != null) {
         throw b(var5, "'jurisdictions' needs layers: 'jurisdiction'");
      }

      if (!(var4.get("provenance") instanceof Map var31)) {
         throw b(var5, "'provenance' is required");
      } else if (var31.get("role") instanceof String var36 && var3.contains(var36)) {
         boolean var39 = Boolean.TRUE.equals(var4.get("ai"));
         ArrayList<Map<String, Object>> var15 = null;
         if (!var0.a()) {
            if (!(var4.get("rows") instanceof List var16)) {
               throw b(var5, "'rows' is a list of row objects");
            }

            var15 = new ArrayList();
            int var40 = 0;

            for (Object var19 : var16) {
               var40++;
               if (!(var19 instanceof Map var20)) {
                  throw b(var5, "row " + var40 + " is not an object");
               }

               var15.add(var20);
            }
         }

         return new io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration(
            List.copyOf(var21), Collections.unmodifiableMap(var22), var24, var27, Collections.unmodifiableMap(new LinkedHashMap<>(var31)), var39, var15
         );
      } else {
         throw b(var5, "provenance.role is one of " + new TreeSet(var3) + (var3.contains("under-test") ? "" : " (under-test is an evidence input)"));
      }
   }

   private static Object c(String var0, String var1) {
      try {
         return Json.parseStrict(var1, true);
      } catch (RuntimeException var3) {
         throw b(var0, "not JSON: " + var3.getMessage());
      }
   }

   private static Map<String, Object> a(String var0, Object var1, String var2) {
      if (var1 instanceof Map var3) {
         return var3;
      } else {
         throw b(var0, var2 + " is a JSON object");
      }
   }

   private static List<Map<String, Object>> a(io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var1, boolean var2) {
      var var3 = b(var0, var1, var2);
      HashMap<java.util.ArrayList, java.lang.Integer> var4 = new HashMap();

      for (int var5 = 0; var5 < var3.size(); var5++) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (String var8 : var1.a) {
            var6.add((String)((Map)var3.get(var5)).get(var8));
         }

         Integer var9 = var4.putIfAbsent(var6, var5 + 1);
         if (var9 != null && !var1.a.isEmpty()) {
            throw new RuleApiException(
               400,
               "TABLE_DUPLICATE_KEY",
               var0.b
                  + ": row "
                  + (var5 + 1)
                  + " column '"
                  + var1.a.get(var1.a.size() - 1)
                  + "': duplicate key "
                  + String.join("/", var6)
                  + " (also row "
                  + var9
                  + ")"
            );
         }
      }

      return var3;
   }

   private static List<Map<String, Object>> b(io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var1, boolean var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      if (var0.a()) {
         var var4 = d(var0.b, var0.e);
         if (var4.isEmpty()) {
            throw b(var0.b, "no header row");
         }

         List var5 = (List)var4.get(0);

         for (String var7 : (Iterable<String>) (Iterable<?>) (var5)) {
            if (!var1.a.contains(var7) && !var1.b.containsKey(var7)) {
               throw b(var0.b, "column '" + var7 + "' is undeclared");
            }
         }

         for (String var17 : var1.a) {
            if (!var5.contains(var17)) {
               throw b(var0.b, "key column '" + var17 + "' is missing");
            }
         }

         for (String var18 : var1.b.keySet()) {
            if (!var5.contains(var18)) {
               throw b(var0.b, "column '" + var18 + "' is missing");
            }
         }

         for (int var15 = 1; var15 < var4.size(); var15++) {
            List var19 = (List)var4.get(var15);
            if (var19.size() != var5.size()) {
               throw b(var0.b, "row " + var15 + " has " + var19.size() + " cells, the header " + var5.size());
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();

            for (int var9 = 0; var9 < var5.size(); var9++) {
               String var10 = (String)var5.get(var9);
               var8.put(var10, a(var0.b, var15, var10, var19.get(var9), var1.a.contains(var10) ? null : a(var1.b.get(var10), var2)));
            }

            var3.add(var8);
         }
      } else {
         int var11 = 0;

         for (Map var16 : var1.g) {
            var11++;

            for (String var22 : (Iterable<String>) (Iterable<?>) (var16.keySet())) {
               if (!var1.a.contains(var22) && !var1.b.containsKey(var22)) {
                  throw b(var0.b, "row " + var11 + " column '" + var22 + "' is undeclared");
               }
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();

            for (String var25 : var1.a) {
               var21.put(var25, a(var0.b, var11, var25, var16.get(var25), null));
            }

            for (Entry var26 : var1.b.entrySet()) {
               var21.put((String)var26.getKey(), a(var0.b, var11, (String)var26.getKey(), var16.get(var26.getKey()), a((String)var26.getValue(), var2)));
            }

            var3.add(var21);
         }
      }

      return var3;
   }

   private static Object a(String var0, int var1, String var2, Object var3, String var4) {
      if (var3 == null || var3 instanceof String var5 && var5.isEmpty()) {
         throw b(var0, "row " + var1 + " column '" + var2 + "' is empty");
      } else if (var4 != null) {
         switch (var4) {
            case "string":
               if (var3 instanceof String var13) {
                  return var13;
               }
               break;
            case "boolean":
               if (var3 instanceof Boolean var12) {
                  return var12;
               }

               if ("true".equals(var3) || "false".equals(var3)) {
                  return Boolean.valueOf((String)var3);
               }
               break;
            case "number":
            case "integer":
               Number var7 = null;
               if (var3 instanceof Number var8) {
                  var7 = Terms.narrow(var8.doubleValue());
               } else if (var3 instanceof String var9 && f.matcher(var9).matches()) {
                  var7 = Terms.narrow(Double.parseDouble(var9));
               }

               if (var7 != null && (var4.equals("number") || var7 instanceof Integer || var7 instanceof Long)) {
                  return var7;
               }
         }

         throw b(var0, "row " + var1 + " column '" + var2 + "' is not a " + var4 + ": " + var3);
      } else if (var3 instanceof String var11) {
         return var11;
      } else if (var3 instanceof Number var10) {
         return Terms.numberToString(Terms.narrow(var10.doubleValue()));
      } else {
         throw b(var0, "row " + var1 + " key column '" + var2 + "' is not a string");
      }
   }

   private static List<List<String>> d(String var0, String var1) {
      ArrayList<java.util.List<java.lang.String>> var2 = new ArrayList();
      ArrayList<java.lang.String> var3 = new ArrayList();
      StringBuilder var4 = new StringBuilder();
      boolean var5 = false;
      boolean var6 = false;
      int var7 = var1.startsWith("\ufeff") ? 1 : 0;

      for (int var8 = 1; var7 < var1.length(); var7++) {
         char var9 = var1.charAt(var7);
         if (var5) {
            if (var9 == '"') {
               if (var7 + 1 < var1.length() && var1.charAt(var7 + 1) == '"') {
                  var4.append('"');
                  var7++;
               } else {
                  var5 = false;
               }
            } else {
               var4.append(var9);
            }
         } else if (var9 == '"') {
            if (var4.length() > 0) {
               throw b(var0, "line " + var8 + ": a quote inside an unquoted cell");
            }

            var5 = true;
            var6 = true;
         } else if (var9 == ',') {
            var3.add(var4.toString());
            var4.setLength(0);
            var6 = true;
         } else if (var9 != '\n' && var9 != '\r') {
            var4.append(var9);
         } else {
            if (var9 == '\r' && var7 + 1 < var1.length() && var1.charAt(var7 + 1) == '\n') {
               var7++;
            }

            if (var6 || var4.length() > 0) {
               var3.add(var4.toString());
               var2.add(var3);
               var3 = new ArrayList();
               var4.setLength(0);
               var6 = false;
            }

            var8++;
         }
      }

      if (var5) {
         throw b(var0, "an unclosed quote");
      } else {
         if (var6 || var4.length() > 0) {
            var3.add(var4.toString());
            var2.add(var3);
         }

         return var2;
      }
   }

   private static List<List<int[]>> e(String var0, String var1) {
      ArrayList<java.util.List<int[]>> var2 = new ArrayList();
      ArrayList<int[]> var3 = new ArrayList();
      int var4 = var1.startsWith("\ufeff") ? 1 : 0;
      int var5 = var4;
      boolean var6 = false;

      boolean var7;
      for (var7 = false; var4 < var1.length(); var4++) {
         char var8 = var1.charAt(var4);
         if (var6) {
            if (var8 == '"') {
               if (var4 + 1 < var1.length() && var1.charAt(var4 + 1) == '"') {
                  var4++;
               } else {
                  var6 = false;
               }
            }
         } else if (var8 == '"') {
            var6 = true;
            var7 = true;
         } else if (var8 == ',') {
            var3.add(new int[]{var5, var4});
            var5 = var4 + 1;
            var7 = true;
         } else if (var8 == '\n' || var8 == '\r') {
            if (var7 || var4 > var5) {
               var3.add(new int[]{var5, var4});
               var2.add(var3);
               var3 = new ArrayList();
            }

            if (var8 == '\r' && var4 + 1 < var1.length() && var1.charAt(var4 + 1) == '\n') {
               var4++;
            }

            var5 = var4 + 1;
            var7 = false;
         }
      }

      if (var6) {
         throw b(var0, "an unclosed quote");
      } else {
         if (var7 || var4 > var5) {
            var3.add(new int[]{var5, var4});
            var2.add(var3);
         }

         return var2;
      }
   }

   private static Object a(List<Map<String, Object>> var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var1) {
      if (var1.a.isEmpty()) {
         ArrayList<java.lang.Object> var9 = new ArrayList();

         for (Map var11 : var0) {
            var9.add(a(var11, var1));
         }

         return Collections.unmodifiableList(var9);
      } else {
         LinkedHashMap var2 = new LinkedHashMap();

         for (Map var4 : var0) {
            Object var5 = var2;

            for (int var6 = 0; var6 < var1.a.size() - 1; var6++) {
               String var7 = (String)var4.get(var1.a.get(var6));
               Object var8 = ((java.util.Map)var5).get(var7);
               if (var8 == null) {
                  var8 = new LinkedHashMap();
                  ((java.util.Map)var5).put(var7, var8);
               }

               var5 = (Map)var8;
            }

            ((java.util.Map)var5).put((String)var4.get(var1.a.get(var1.a.size() - 1)), a(var4, var1));
         }

         return d(var2);
      }
   }

   private static Object a(Map<String, Object> var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDeclaration var1) {
      if (var1.b.size() == 1) {
         return var0.get(var1.b.keySet().iterator().next());
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (String var4 : var1.b.keySet()) {
            var2.put(var4, var0.get(var4));
         }

         return Collections.unmodifiableMap(var2);
      }
   }

   private static String a(Map<String, Object> var0, List<String> var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var1) {
         if (var2.length() > 0) {
            var2.append('/');
         }

         var2.append(var0.get(var4));
      }

      return var2.toString();
   }

   private static Object d(Object var0) {
      if (!(var0 instanceof Map var1)) {
         return var0;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            var2.put((String)var4.getKey(), d(var4.getValue()));
         }

         return Collections.unmodifiableMap(var2);
      }
   }

   private static Object a(Object var0, String var1) {
      if (var0 instanceof Map var6) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
            var7.put((String)var9.getKey(), a(var9.getValue(), var1));
         }

         return new io.karatelabs.ext.rules.RuleTableCatalog.ReadOnlyTableMap(var7, var1);
      } else if (!(var0 instanceof List var2)) {
         return var0;
      } else {
         ArrayList<java.lang.Object> var3 = new ArrayList();

         for (Object var5 : var2) {
            var3.add(a(var5, var1));
         }

         return Collections.unmodifiableList(var3);
      }
   }

   private static void a(Map<String, Object> var0, String var1, Object var2) {
      String[] var3 = var1.split("\\.");
      var var4 = var0;

      for (int var5 = 0; var5 < var3.length - 1; var5++) {
         var4 = (Map<String, Object>)var4.computeIfAbsent(var3[var5], var0x -> new LinkedHashMap());
      }

      var4.put(var3[var3.length - 1], var2);
   }

   static RuleApiException b(String var0, String var1) {
      return new RuleApiException(400, "TABLE_INVALID", var0 + ": " + var1);
   }

   private static RuleApiException f(String var0, String var1) {
      return new RuleApiException(400, "LOOKUP_KEY_COLLISION", "table key '" + var0 + "' collides with " + var1);
   }

   public record TableCellReference(String c, String d, String e, List<String> f, String g, Object h, boolean i, boolean j) {

      public TableCellReference(String c, String d, String e, List<String> f, String g, Object h, boolean i, boolean j) {
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
      }

      public String a() {
         return !this.j
            ? this.c + "." + String.join(".", this.f) + (this.i ? "" : "." + this.g)
            : this.c + (this.e == null ? "" : "@" + this.e) + "/" + String.join("/", this.f) + (this.i ? "" : "/" + this.g);
      }

      public Map<String, Object> b() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("file", this.d);
         var1.put("row", String.join("/", this.f));
         var1.put("column", this.g);
         return var1;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public List<String> f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public Object h() {
         return this.h;
      }

      public boolean i() {
         return this.i;
      }

      public boolean j() {
         return this.j;
      }
   }

   record TableSetScope(String component1, Set<String> b) {
      static final io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope a = new io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope("tables", Set.of("baseline", "reference"));

      TableSetScope(String component1, Set<String> b) {
         this.component1 = component1;
         this.b = b;
      }

      static io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope a(String var0) {
         return new io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope("evidence/" + var0, Set.of("reference", "under-test"));
      }

      static io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope b(String var0) {
         if (!var0.startsWith("evidence/reference") && !var0.startsWith("evidence/under-test")) {
            throw new RuleApiException(400, "INVALID_INPUT", "an evidence table set lives under evidence/reference/ or evidence/under-test/, not '" + var0 + "'");
         } else {
            return new io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope(var0, Set.of("reference", "under-test"));
         }
      }

      public String a() {
         return this.component1;
      }

      public Set<String> b() {
         return this.b;
      }
   }

   private static final class JsonSpanNode {
      int a;
      int b;
      Map<String, io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode> c;
      List<io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode> d;
      String e;

      String a() {
         if (this.e == null) {
            return null;
         } else if (this.e.startsWith("\"")) {
            return String.valueOf(((List)Json.parseStrict("[" + this.e + "]", true)).get(0));
         } else {
            return RuleTableCatalog.f.matcher(this.e).matches() ? Terms.numberToString(Terms.narrow(Double.parseDouble(this.e))) : this.e;
         }
      }

      static io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode a(String var0, String var1) {
         int[] var2 = new int[]{0};
         return a(var0, var1, var2);
      }

      private static void a(String var0, int[] var1) {
         while (var1[0] < var0.length() && Character.isWhitespace(var0.charAt(var1[0]))) {
            var1[0]++;
         }
      }

      private static io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode a(String var0, String var1, int[] var2) {
         a(var1, var2);
         if (var2[0] >= var1.length()) {
            throw RuleTableCatalog.b(var0, "unexpected end of JSON");
         } else {
            io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var3 = new io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode();
            var3.a = var2[0];
            char var4 = var1.charAt(var2[0]);
            if (var4 == '{') {
               var3.c = new LinkedHashMap<>();
               var2[0]++;
               a(var1, var2);
               if (var1.charAt(var2[0]) == '}') {
                  var2[0]++;
               } else {
                  do {
                     io.karatelabs.ext.rules.RuleTableCatalog.JsonSpanNode var5 = a(var0, var1, var2);
                     a(var1, var2);
                     var2[0]++;
                     String var6 = String.valueOf(((List)Json.parseStrict("[" + var5.e + "]", true)).get(0));
                     var3.c.put(var6, a(var0, var1, var2));
                     a(var1, var2);
                  } while (var1.charAt(var2[0]++) != '}');
               }
            } else if (var4 == '[') {
               var3.d = new ArrayList<>();
               var2[0]++;
               a(var1, var2);
               if (var1.charAt(var2[0]) == ']') {
                  var2[0]++;
               } else {
                  do {
                     var3.d.add(a(var0, var1, var2));
                     a(var1, var2);
                  } while (var1.charAt(var2[0]++) != ']');
               }
            } else if (var4 == '"') {
               int var7 = var2[0] + 1;

               while (var1.charAt(var7) != '"') {
                  var7 += var1.charAt(var7) == '\\' ? 2 : 1;
               }

               var2[0] = var7 + 1;
               var3.e = var1.substring(var3.a, var2[0]);
            } else {
               int var8 = var2[0];

               while (var8 < var1.length() && ",}] \t\r\n".indexOf(var1.charAt(var8)) < 0) {
                  var8++;
               }

               var2[0] = var8;
               var3.e = var1.substring(var3.a, var8);
            }

            var3.b = var2[0];
            return var3;
         }
      }
   }

   private record CompiledTable(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition a, Object b, Object c) {

      private CompiledTable(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition a, Object b, Object c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record TableDeclaration(List<String> a, Map<String, String> b, String c, List<String> d, Map<String, Object> e, boolean f, List<Map<String, Object>> g) {

      private TableDeclaration(List<String> a, Map<String, String> b, String c, List<String> d, Map<String, Object> e, boolean f, List<Map<String, Object>> g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   public record TableCellLookup(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a, String b) {

      public TableCellLookup(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   private final class TableLookupView extends AbstractMap<String, Object> {
      private final Map<String, Object> b;
      private final String c;
      private final io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition d;
      private final String e;
      private final List<String> f;
      private final Consumer<io.karatelabs.ext.rules.RuleTableCatalog.TableCellLookup> g;
      private final String h;
      private final Object i;

      TableLookupView(Map<String, Object> var2, String var3, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var4, String var5, List<String> var6, Consumer<io.karatelabs.ext.rules.RuleTableCatalog.TableCellLookup> var7, String var8, Object var9) {
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
         this.f = var6;
         this.g = var7;
         this.h = var8;
         this.i = var9;
      }

      private io.karatelabs.ext.rules.RuleTableCatalog.TableLookupView a(Map<String, Object> var1, String var2, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3, String var4, List<String> var5) {
         return RuleTableCatalog.this.new TableLookupView(var1, var2, var3, var4, var5, this.g, this.h, this.i);
      }

      @Override
      public Object get(Object var1) {
         Object var2 = this.b.get(var1);
         String var3 = String.valueOf(var1);
         if (var2 == null) {
            return null;
         } else if (this.d == null) {
            String var8 = this.c.isEmpty() ? var3 : this.c + "." + var3;
            io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var9 = RuleTableCatalog.this.g.get(var8);
            return var2 instanceof Map var6 ? this.a(var6, var8, var9, null, List.of()) : var2;
         } else if (this.d.b().isEmpty()) {
            return var2;
         } else if (this.d.d() != null && this.e == null) {
            return var2 instanceof Map var7 ? this.a(var7, this.c, this.d, var3, List.of()) : var2;
         } else if (this.f.size() < this.d.b().size()) {
            var var4 = new ArrayList<>(this.f);
            var4.add(var3);
            if (var4.size() == this.d.b().size() && this.d.c().size() == 1) {
               return this.a(var4, this.d.c().keySet().iterator().next(), var2);
            } else {
               return var2 instanceof Map var5 ? this.a(var5, this.c, this.d, this.e, List.copyOf(var4)) : var2;
            }
         } else {
            return this.d.c().containsKey(var3) ? this.a(this.f, var3, var2) : var2;
         }
      }

      private Object a(List<String> var1, String var2, Object var3) {
         io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var4 = RuleTableCatalog.this.a(this.d, this.e, var1, var2);
         if (var4 != null) {
            this.g.accept(new io.karatelabs.ext.rules.RuleTableCatalog.TableCellLookup(var4, this.e));
            if (this.h != null && this.h.equals(var4.a())) {
               return this.i;
            }
         }

         return var3;
      }

      @Override
      public boolean containsKey(Object var1) {
         return this.b.containsKey(var1);
      }

      @Override
      public int size() {
         return this.b.size();
      }

      @Override
      public Set<Entry<String, Object>> entrySet() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

         for (String var3 : this.b.keySet()) {
            Object var4 = this.get(var3);
            if (this.d != null && var4 instanceof io.karatelabs.ext.rules.RuleTableCatalog.TableLookupView var5) {
               var5.entrySet();
            }

            var1.put(var3, var4);
         }

         return Collections.unmodifiableMap(var1).entrySet();
      }

      public Object a(String var1, Object var2) {
         return this.b.put(var1, var2);
      }

      @Override
      public Object remove(Object var1) {
         return this.b.remove(var1);
      }
   }

   private record TableSourceFile(String b, String c, String d, String e) {

      private TableSourceFile(String b, String c, String d, String e) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }

      static io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile a(String var0, String var1, String var2) {
         if (!var1.startsWith(var0 + "/")) {
            throw RuleTableCatalog.b(var1, "not under " + var0 + "/");
         } else {
            String var3 = var1.substring(var0.length() + 1);
            String var4 = var3.substring(0, var3.lastIndexOf(46));
            String var5 = null;
            int var6 = var4.lastIndexOf(64);
            if (var6 >= 0) {
               var5 = var4.substring(var6 + 1);
               var4 = var4.substring(0, var6);
               if (var5.isEmpty() || var5.contains("/")) {
                  throw RuleTableCatalog.b(var1, "a jurisdiction suffix must follow the file name");
               }
            }

            for (String var10 : var4.split("/", -1)) {
               if (var10.isEmpty() || var10.contains(".") || var10.contains("@")) {
                  throw RuleTableCatalog.b(var1, "a key segment is a name without '.' or '@'");
               }
            }

            return new io.karatelabs.ext.rules.RuleTableCatalog.TableSourceFile(var1, var4.replace('/', '.'), var5, var2);
         }
      }

      boolean a() {
         return this.b.endsWith(".csv") || this.b.endsWith(".xlsx");
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }
   }

   public record TableDefinition(String a, List<String> b, Map<String, String> c, String d, List<String> e, boolean f, List<io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry> g, Map<String, String> h, int i) {

      public TableDefinition(String a, List<String> b, Map<String, String> c, String d, List<String> e, boolean f, List<io.karatelabs.ext.rules.RuleTableCatalog.TableFileEntry> g, Map<String, String> h, int i) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
      }
   }

   public record TableFileEntry(String a, String b, String c, Map<String, Object> d, boolean e) {

      public TableFileEntry(String a, String b, String c, Map<String, Object> d, boolean e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   static final class ReadOnlyTableMap extends AbstractMap<String, Object> {
      private final Map<String, Object> a;
      private final String b;

      ReadOnlyTableMap(Map<String, Object> var1, String var2) {
         this.a = var1;
         this.b = var2;
      }

      @Override
      public Object get(Object var1) {
         return this.a.get(var1);
      }

      @Override
      public boolean containsKey(Object var1) {
         if (this.a.containsKey(var1)) {
            return true;
         } else if (this.b != null) {
            throw new RuleApiException(422, "LOOKUP_MISS", this.b + " has no key '" + var1 + "'");
         } else {
            return false;
         }
      }

      @Override
      public int size() {
         return this.a.size();
      }

      @Override
      public Set<Entry<String, Object>> entrySet() {
         return Collections.unmodifiableMap(this.a).entrySet();
      }

      public Object a(String var1, Object var2) {
         throw a(var1);
      }

      @Override
      public Object remove(Object var1) {
         throw a(var1);
      }

      @Override
      public void clear() {
         throw a(null);
      }

      private static RuleApiException a(Object var0) {
         return new RuleApiException(422, "LOOKUP_READ_ONLY", "a table is read-only" + (var0 == null ? "" : ": cannot write '" + var0 + "'"));
      }
   }
}
