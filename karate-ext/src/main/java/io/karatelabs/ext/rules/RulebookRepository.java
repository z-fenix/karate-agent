package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RulebookRepository {
   private static final Logger n = LoggerFactory.getLogger(RulebookRepository.class);
   static final Pattern a = Pattern.compile("^sc-[0-9a-f]{6}$");
   private static final Pattern o = Pattern.compile("^[A-Za-z0-9._-]+$");
   private static final Pattern p = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");
   private static final Pattern q = Pattern.compile("(?m)^(\\d+):(\\d+)\\s");
   static final Pattern b = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec r = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("calc", "calc.js", true);
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec s = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("schema", "schema.js", true);
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec t = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("scenarios", "scenarios.json", true);
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec u = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("generator", "generator.js", false);
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec v = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("twin", "twin.js", false);
   private static final io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec w = new io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec("sequences", "sequences.json", false);
   private static final List<io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec> x = List.of(r, s, t, u, v, w);
   private static final String y = x.stream().filter(io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec::c).map(io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec::b).collect(Collectors.joining(", ", "{", "}"));
   private static final int z = 1;
   public static final String c = "_";
   public static final String d = "_id";
   public static final String e = "_label";
   public static final String f = "_from";
   public static final String g = "_deck";
   private static final String A = "promote";
   private static final List<String> B = List.of("state", "transition", "rejection");
   public static final String h = "_expect";
   public static final String i = "schema-reject";
   public static final String j = "_req";
   public static final String k = "_ai";
   private static final List<String> C = List.of("_id", "_label", "_expect", "_req", "_from");
   private final Path D;
   private final Random E;
   private Path F = Path.of(".");
   public static final String l = "_table";
   static final String m = "KARATE_RULES_SCHEMA_TIMEOUT_MS";
   private static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper G = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new RuleApiException(
            408, "TIMEOUT", "schema.js exceeded " + var1 + "ms — it declares the input shape and must return promptly; check for a loop that never ends"
         );
      }

      @Override
      public RuntimeException a(Throwable var1) {
         for (Throwable var2 = var1; var2 != null; var2 = var2.getCause()) {
            if (var2 instanceof RuleApiException var3) {
               return var3;
            }
         }

         return (RuntimeException)(var1 instanceof RuntimeException var4 ? var4 : new IllegalStateException(var1));
      }

      @Override
      public RuntimeException a() {
         return new RuleApiException(500, "INTERNAL", "schema.js eval interrupted");
      }
   };

   public static boolean a(String var0) {
      return var0 != null && var0.startsWith("_");
   }

   public static boolean a(Map<String, Object> var0) {
      return var0 != null && "schema-reject".equals(var0.get("_expect"));
   }

   public static List<Map<String, Object>> a(List<Map<String, Object>> var0) {
      return a(var0, false);
   }

   public static List<Map<String, Object>> b(List<Map<String, Object>> var0) {
      return a(var0, true);
   }

   private static List<Map<String, Object>> a(List<Map<String, Object>> var0, boolean var1) {
      if (var0 == null) {
         return var1 ? List.of() : null;
      } else {
         boolean var2 = false;

         for (Map var4 : var0) {
            if (a(var4)) {
               var2 = true;
               break;
            }
         }

         if (!var2) {
            return var1 ? List.of() : var0;
         } else {
            ArrayList var6 = new ArrayList();

            for (Map var5 : var0) {
               if (a(var5) == var1) {
                  var6.add(var5);
               }
            }

            return var6;
         }
      }
   }

   static void c(List<Map<String, Object>> var0) {
      for (int var1 = 0; var1 < var0.size(); var1++) {
         Map var2 = (Map)var0.get(var1);
         Object var3 = var2.get("_id");
         String var4 = "scenario " + (var3 == null ? "at index " + var1 : "'" + var3 + "'");

         for (String var6 : (Iterable<String>) (Iterable<?>) (var2.keySet())) {
            if (a(var6) && !C.contains(var6)) {
               throw new RuleApiException(
                  400,
                  "PARSE_FAILED",
                  var4
                     + " has an unknown engine-owned key '"
                     + var6
                     + "' — a scenario row carries only "
                     + String.join(", ", C)
                     + "; every other name is business input"
               );
            }
         }

         Object var7 = var2.get("_expect");
         if (var7 != null && !(var7 instanceof Map) && !"schema-reject".equals(var7)) {
            throw new RuleApiException(
               400,
               "PARSE_FAILED",
               var4
                  + " has an unknown _expect value '"
                  + var7
                  + "' — the only string form is 'schema-reject' (the schema must refuse this row); an OBJECT _expect is the stored output/outcome expectation"
            );
         }

         a(var2, var4);
      }
   }

   private static void a(Map<String, Object> var0, String var1) {
      if (var0.containsKey("_req")) {
         Object var2 = var0.get("_req");
         if (!a(var0)) {
            throw new RuleApiException(
               400,
               "PARSE_FAILED",
               var1
                  + " carries _req but is not a reject row — _req names the criteria an _expect:'schema-reject' row answers; on an ordinary scenario calc.req('<reqId>/<acId>') inside calc.js is the linkage"
            );
         } else {
            boolean var3 = var2 instanceof String || var2 instanceof List;
            List var4 = var2 instanceof List var5 ? var5 : Collections.singletonList(var2);
            if (var3 && !var4.isEmpty()) {
               Iterator var8 = var4.iterator();

               while (var8.hasNext()) {
                  if (!(var8.next() instanceof String var7) || var7.isBlank()) {
                     throw new RuleApiException(
                        400, "PARSE_FAILED", var1 + " has a blank or non-string _req entry — every id names an acceptance criterion, '<reqId>/<acId>'"
                     );
                  }
               }
            } else {
               throw new RuleApiException(
                  400,
                  "PARSE_FAILED",
                  var1 + " has an unusable _req — it is a criterion id string, or a non-empty array of them, in the '<reqId>/<acId>' vocabulary"
               );
            }
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static List<String> b(Map<String, Object> var0) {
      Object var1 = var0 == null ? null : var0.get("_req");
      if (var1 == null) {
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();

         for (Object var6 : var1 instanceof List<?> values ? values : List.of(var1)) {
            String var5 = String.valueOf(var6);
            if (!var2.contains(var5)) {
               var2.add(var5);
            }
         }

         return var2;
      }
   }

   public static boolean c(Map<String, Object> var0) {
      return !b(var0).isEmpty() && (!Boolean.TRUE.equals(var0.get("_ai")) || var0.get("_from") instanceof Map var1 && var1.containsKey("confirmation"));
   }

   public static Map<String, Object> d(Map<String, Object> var0) {
      if (var0 == null) {
         return null;
      } else {
         var var1 = new LinkedHashMap<>(var0);
         var1.keySet().removeIf(RulebookRepository::a);
         return var1;
      }
   }

   public RulebookRepository(Path var1) {
      this(var1, new Random());
   }

   RulebookRepository(Path var1, Random var2) {
      this.D = var1;
      this.E = var2;
   }

   public RulebookRepository a(Path var1) {
      this.F = var1 == null ? Path.of(".") : var1;
      return this;
   }

   public Path a() {
      return this.F;
   }

   public List<io.karatelabs.ext.rules.RulebookRepository.RulebookSummary> b() {
      if (!Files.isDirectory(this.D)) {
         return List.of();
      } else {
         ArrayList var1 = new ArrayList();

         try {
            try (var var2 = Files.list(this.D)) {
               for (Path var4 : (Iterable<Path>) (var2.sorted()::iterator)) {
                  if (Files.isDirectory(var4)) {
                     String var5 = var4.getFileName().toString();
                     boolean var6 = true;

                     for (io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec var8 : x) {
                        if (var8.c() && !Files.exists(var8.a(var4))) {
                           var6 = false;
                           break;
                        }
                     }

                     if (!var6) {
                        n.warn("rule '{}' missing one of {} — excluding", var5, y);
                     } else {
                        var1.add(new io.karatelabs.ext.rules.RulebookRepository.RulebookSummary(var5, this.f(var4), this.e(var4)));
                     }
                  }
               }
            }

            return var1;
         } catch (IOException var11) {
            throw new UncheckedIOException(var11);
         }
      }
   }

   public io.karatelabs.ext.rules.RulebookRepository.RulebookDocument b(String var1) {
      Path var2 = this.D.resolve(var1);
      String var3 = f(r.a(var2), var1);
      String var4 = f(s.a(var2), var1);
      String var5 = f(t.a(var2), var1);
      io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult var6 = g(var4);
      Object var7 = var6.a();
      io.karatelabs.ext.rules.CellCoverageAnalyzer.a(var6.b(), var3);
      var var8 = this.g(var2, var5);
      LookupTableEditor var10 = new LookupTableEditor(var3);

      Map var9;
      try {
         var9 = var10.b();
      } catch (RuleApiException var14) {
         if (!"LOOKUP_BLOCK_MISSING".equals(var14.b)) {
            throw var14;
         }

         var9 = Map.of();
      }

      RuleTableCatalog var11 = RuleTableCatalog.a(var2);
      var9 = var11.a(var9, var10.c());
      String var12 = a(var2, v, var1);
      var var13 = this.e(var2);
      a(var13, r, var3);
      a(var13, s, var4);
      a(var13, v, var12);
      return new io.karatelabs.ext.rules.RulebookRepository.RulebookDocument(
         var1, var7, var8, var3, var9, var13, a(var2, u, var1), var12, k(a(var2, w, var1)), var11, a((String)var13.get(r.a()), var11.c()), var6.b()
      );
   }

   static io.karatelabs.ext.rules.RulebookRepository.DigestReference a(String var0, Map<String, String> var1) {
      if (var1.isEmpty()) {
         return new io.karatelabs.ext.rules.RulebookRepository.DigestReference("file", var0);
      } else {
         var var2 = new TreeMap<>(var1);
         var2.put(r.b(), var0);
         ArrayList var3 = new ArrayList();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            LinkedHashMap var6 = new LinkedHashMap();
            var6.put("path", (String)var5.getKey());
            var6.put("sha256", (String)var5.getValue());
            var3.add(var6);
         }

         return new io.karatelabs.ext.rules.RulebookRepository.DigestReference("manifest", io.karatelabs.ext.common.Sha256.a(Json.stringifyStrict(var3)));
      }
   }

   private static void a(Map<String, String> var0, io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec var1, String var2) {
      if (var2 != null) {
         var0.put(var1.a(), f(var2));
      }
   }

   public static String a(Object var0) {
      return RuleVerificationService.a(var0);
   }

   public Path c(String var1) {
      j(var1);
      return this.D.resolve(var1);
   }

   public io.karatelabs.ext.rules.RulebookRepository.RulebookSnapshot d(String var1) {
      j(var1);
      Path var2 = this.D.resolve(var1);
      Path var3 = r.a(var2);
      byte[] var4 = e(var3, var1);
      String var5 = io.karatelabs.ext.common.Sha256.a(var4);
      RuleTableCatalog var6 = RuleTableCatalog.a(var2);
      return new io.karatelabs.ext.rules.RulebookRepository.RulebookSnapshot(new String(var4, StandardCharsets.UTF_8), var5, var6, a(var5, var6.c()));
   }

   public Map<String, Object> a(String var1, String var2) {
      return a(this.b(var1), var2);
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1) {
      for (Map var3 : var0.f()) {
         if (var1.equals(var3.get("_id"))) {
            return var3;
         }
      }

      throw new RuleApiException(404, "SCENARIO_NOT_FOUND", "no scenario " + var1 + " in rule " + var0.d());
   }

   public String a(String var1, String var2, String var3) {
      Path var4 = r.a(this.D.resolve(var1));
      f(var4, var1);
      this.c(var4, var3);

      try {
         Engine.parse(var2);
      } catch (RuntimeException var6) {
         throw a(var6);
      }

      d(var4, var2);
      return b(var4);
   }

   private static boolean c(Path var0) {
      return Files.isDirectory(var0) && !Files.exists(var0.resolve("tables")) && x.stream().noneMatch(var1 -> Files.exists(var1.a(var0)));
   }

   public Map<String, Object> a(String var1, String var2, String var3, String var4, String var5) {
      j(var1);
      if (var2 == null || var2.isBlank()) {
         throw new RuleApiException(400, "PARSE_FAILED", "Rule.create requires schema.js text");
      } else if (var3 != null && !var3.isBlank()) {
         Path var6 = this.D.resolve(var1);
         if (Files.exists(var6) && !c(var6)) {
            throw new RuleApiException(
               409,
               "RULE_EXISTS",
               "rulebook '"
                  + var1
                  + "' already exists — to replace part of one use Rule.calc.update / schema.update / generator.update / lookups.update / scenario.create|update|delete"
            );
         } else {
            io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult var7 = g(var2);

            try {
               Engine.parse(var3);
            } catch (RuntimeException var10) {
               throw a(var10);
            }

            io.karatelabs.ext.rules.CellCoverageAnalyzer.a(var7.b(), var3);
            if (var4 != null && !var4.isBlank()) {
               h(var4);
            }

            String var8 = var5 != null && !var5.isBlank() ? var5 : "[]";
            i(var8);

            try {
               Files.createDirectories(var6);
               d(s.a(var6), var2);
               d(r.a(var6), var3);
               d(t.a(var6), var8);
               if (var4 != null && !var4.isBlank()) {
                  d(u.a(var6), var4);
               }
            } catch (IOException var11) {
               throw new UncheckedIOException(var11);
            }

            LinkedHashMap var9 = new LinkedHashMap();
            var9.put("name", var1);
            var9.put("mtimes", this.e(var6));
            return var9;
         }
      } else {
         throw new RuleApiException(400, "PARSE_FAILED", "Rule.create requires calc.js text");
      }
   }

   public Map<String, Object> e(String var1) {
      j(var1);
      Path var2 = this.D.resolve(var1);
      if (!Files.isDirectory(var2)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "no rulebook '" + var1 + "' (try Rule.list())");
      } else {
         long var3;
         try (var var5 = Files.walk(var2)) {
            var var6 = var5.sorted(Comparator.reverseOrder()).toList();
            var3 = var6.stream().filter(var0 -> Files.isRegularFile(var0)).count();

            for (Path var8 : (Iterable<Path>) (Iterable<?>) (var6)) {
               Files.delete(var8);
            }
         } catch (IOException var11) {
            throw new RuleApiException(
               500,
               "DELETE_INCOMPLETE",
               "rulebook '"
                  + var1
                  + "' was only partly removed ("
                  + var11.getMessage()
                  + ") — the folder is no longer a loadable rulebook. Clear the residue with File.delete('"
                  + this.D.getFileName()
                  + "/"
                  + var1
                  + "')"
            );
         }

         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("name", var1);
         var12.put("status", "deleted");
         var12.put("files", var3);
         return var12;
      }
   }

   public String b(String var1, String var2, String var3) {
      Path var4 = s.a(this.D.resolve(var1));
      f(var4, var1);
      this.c(var4, var3);
      io.karatelabs.ext.rules.CellCoverageAnalyzer.a(g(var2).b(), f(r.a(this.D.resolve(var1)), var1));
      d(var4, var2);
      return b(var4);
   }

   public String c(String var1, String var2, String var3) {
      Path var4 = this.D.resolve(var1);
      if (!Files.isDirectory(var4)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "no rulebook '" + var1 + "'");
      } else if (var2 != null && !var2.isBlank()) {
         Path var5 = u.a(var4);
         if (Files.exists(var5)) {
            this.c(var5, var3);
         }

         h(var2);
         d(var5, var2);
         return b(var5);
      } else {
         throw new RuleApiException(400, "PARSE_FAILED", "Rule.generator.update requires generator.js text");
      }
   }

   private static void h(String var0) {
      try {
         Generator.a(var0);
      } catch (RuntimeException var4) {
         for (Object var2 = var4; var2 != null; var2 = ((java.lang.RuntimeException)var2).getCause()) {
            if (var2 instanceof RuleApiException var3) {
               throw var3;
            }
         }

         throw new RuleApiException(400, "PARSE_FAILED", "generator.js parse failed: " + var4.getMessage(), var4);
      }
   }

   private static void i(String var0) {
      Object var1;
      try {
         var1 = Json.parseStrict(var0, true);
      } catch (RuntimeException var7) {
         throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json parse failed: " + var7.getMessage(), var7);
      }

      if (var1 instanceof List var2) {
         ArrayList var3 = new ArrayList();

         for (Object var5 : var2) {
            if (!(var5 instanceof Map var6)) {
               throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json entry is not an object");
            }

            var3.add(var6);
         }

         c(var3);
      } else {
         throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json must be a JSON array");
      }
   }

   private static void j(String var0) {
      if (var0 == null || !p.matcher(var0).matches()) {
         throw new RuleApiException(400, "PARSE_FAILED", "rulebook name '" + var0 + "' is not valid — letters, digits, '_' or '-' only (no spaces, dots, '/', or ':')");
      }
   }

   public String a(String var1, Map<String, Object> var2, String var3) {
      return String.valueOf(this.b(var1, var2, var3).get("mtime"));
   }

   public Map<String, Object> b(String var1, Map<String, Object> var2, String var3) {
      Path var4 = this.D.resolve(var1);
      Path var5 = r.a(var4);
      String var6 = f(var5, var1);
      RuleTableCatalog var7 = RuleTableCatalog.a(var4);
      LinkedHashMap var8 = new LinkedHashMap();
      if (var7.a()) {
         this.c(var5, var3);
         d(var5, new LookupTableEditor(var6).a(var2));
         var8.put("mtime", b(var5));
         var8.put("home", "inline");
         return var8;
      } else {
         LinkedHashSet var9 = new LinkedHashSet();

         for (String var11 : var7.b().keySet()) {
            var9.add(var11.split("\\.")[0]);
         }

         LookupTableEditor var23 = new LookupTableEditor(var6);
         Map var24 = var23.a() ? var23.b() : Map.of();
         LinkedHashMap var12 = new LinkedHashMap();
         boolean var13 = false;
         TreeMap<String, Map<List<String>, Map<String, Object>>> var14 = new TreeMap<>();
         LinkedHashMap var15 = new LinkedHashMap();

         for (Entry var17 : var2.entrySet()) {
            if (!var9.contains(var17.getKey())) {
               var12.put((String)var17.getKey(), var17.getValue());
               var13 |= var24.get(var17.getKey()) == null
                  || var17.getValue() == null
                  || !Objects.equals(Json.stringifyStrict(var17.getValue()), Json.stringifyStrict(var24.get(var17.getKey())));
            } else {
               LinkedHashMap var18 = new LinkedHashMap();
               a((String)var17.getKey(), var17.getValue(), var18);

               for (Entry var20 : (Iterable<Entry>) (Iterable<?>) (var18.entrySet())) {
                  io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var21 = var7.d((String)var20.getKey());
                  if (var21 == null) {
                     throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "lookup path '" + (String)var20.getKey() + "' names no cell of a file table");
                  }

                  if (!a(var21.h(), var20.getValue())) {
                     Object var22 = var15.put(var21.a(), var20.getValue());
                     if (var22 != null && !a(var22, var20.getValue())) {
                        throw new RuleApiException(
                           400,
                           "LOOKUP_CONFLICT",
                           "'" + (String)var20.getKey() + "' and another path set the one cell " + var21.a() + " (" + var21.d() + ") to different values"
                        );
                     }

                     ((java.util.Map)(var14.computeIfAbsent(var21.d(), var0 -> new LinkedHashMap<>())
                        .computeIfAbsent(var21.f(), var0 -> new LinkedHashMap<>())))
                        .put(var21.g(), var20.getValue());
                  }
               }
            }
         }

         ArrayList var25 = new ArrayList();
         if (var13) {
            var25.add("inline lookup (calc.js)");
         }

         var25.addAll(var14.keySet());
         if (var25.size() > 1) {
            throw new RuleApiException(
               400,
               "LOOKUP_MIXED_HOMES",
               "one lookups update addresses one home; this one changes "
                  + String.join(", ", var25)
                  + " — send one update per home, each with its own ifMatch (mtimes.calc for the inline lookup, tableShas[<path>] for a table file)"
            );
         } else if (var14.isEmpty()) {
            this.c(var5, var3);
            if (var13) {
               d(var5, var23.a(var12));
            }

            var8.put("mtime", b(var5));
            var8.put("home", "inline");
            return var8;
         } else {
            String var26 = (String)var14.keySet().iterator().next();
            Path var27 = var4.resolve(var26);
            this.c(var27, var3);
            String var28 = var7.a(var26, (Map<List<String>, Map<String, Object>>)var14.get(var26));
            var7.a(var26, var28);
            d(var27, var28);
            var8.put("mtime", b(var27));
            var8.put("home", var26);
            return var8;
         }
      }
   }

   private static void a(String var0, Object var1, Map<String, Object> var2) {
      if (var1 instanceof Map var3) {
         if (var3.containsKey("_table")) {
            return;
         }

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            a(var0 + "." + var5.getKey(), var5.getValue(), var2);
         }
      } else {
         if (var1 instanceof List) {
            throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "'" + var0 + "': a file table is edited cell by cell");
         }

         var2.put(var0, var1);
      }
   }

   private static boolean a(Object var0, Object var1) {
      return var0 instanceof Number var2 && var1 instanceof Number var3 ? var2.doubleValue() == var3.doubleValue() : Objects.equals(var0, var1);
   }

   public Map<String, Object> c(String var1, Map<String, Object> var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var var5 = this.a(var1, List.of(var2 == null ? Map.of() : var2), var3);
      var4.put("id", ((List)var5.get("ids")).get(0));
      var4.put("mtime", var5.get("mtime"));
      return var4;
   }

   public Map<String, Object> a(String var1, List<Map<String, Object>> var2, String var3) {
      Path var4 = t.a(this.D.resolve(var1));
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5 = this.b(var1);
      this.c(var4, var3);
      var var6 = new ArrayList<>(var5.f());
      ArrayList var7 = new ArrayList();

      for (Map var9 : var2) {
         LinkedHashMap var10;
         var10 = new LinkedHashMap(var9 == null ? Map.of() : var9);
         var10.remove("_deck");
         c(List.of(var10));
         label34:
         if (var10.get("_id") instanceof String var12 && !var12.isEmpty()) {
            if (!o.matcher(var12).matches()) {
               throw new RuleApiException(
                  400, "PARSE_FAILED", "scenario id '" + var12 + "' is not valid — use only letters, digits, '.', '_' or '-' (no spaces, '/', or ':')"
               );
            }

            var var13 = var6.iterator();

            Map var14;
            do {
               if (!var13.hasNext()) {
                  break label34;
               }

               var14 = (Map)var13.next();
            } while (!var12.equals(var14.get("_id")));

            throw new RuleApiException(400, "PARSE_FAILED", "duplicate scenario id '" + var12 + "'");
         } else {
            var10.put("_id", this.f(var6));
         }

         var6.add(var10);
         var7.add(var10.get("_id"));
      }

      d(var4, e(var6));
      LinkedHashMap var15 = new LinkedHashMap();
      var15.put("ids", var7);
      var15.put("mtime", b(var4));
      return var15;
   }

   public Map<String, Object> a(String var1, String var2, Map<String, Object> var3, String var4) {
      Path var5 = t.a(this.D.resolve(var1));
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var6 = this.b(var1);
      this.c(var5, var4);
      ArrayList var7 = new ArrayList(var6.f().size());
      boolean var8 = false;

      for (Map var10 : var6.f()) {
         if (var2.equals(var10.get("_id"))) {
            LinkedHashMap var11 = new LinkedHashMap(var10);
            if (var3 != null) {
               for (Entry var13 : var3.entrySet()) {
                  if (!"_id".equals(var13.getKey()) && !"_deck".equals(var13.getKey())) {
                     var11.put((String)var13.getKey(), var13.getValue());
                  }
               }
            }

            c(List.of(var11));
            var7.add(var11);
            var8 = true;
         } else {
            var7.add(var10);
         }
      }

      if (!var8) {
         throw new RuleApiException(404, "SCENARIO_NOT_FOUND", "no scenario " + var2 + " in rule " + var1);
      } else {
         d(var5, e(var7));
         return Map.of("mtime", b(var5));
      }
   }

   public Map<String, Object> d(String var1, String var2, String var3) {
      Path var4 = t.a(this.D.resolve(var1));
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5 = this.b(var1);
      this.c(var4, var3);
      ArrayList var6 = new ArrayList(var5.f().size());
      boolean var7 = false;

      for (Map var9 : var5.f()) {
         if (var2.equals(var9.get("_id"))) {
            var7 = true;
         } else {
            var6.add(var9);
         }
      }

      if (!var7) {
         throw new RuleApiException(404, "SCENARIO_NOT_FOUND", "no scenario " + var2 + " in rule " + var1);
      } else {
         d(var4, e(var6));
         return Map.of("mtime", b(var4));
      }
   }

   public String e(String var1, String var2, String var3) {
      return this.a(var1, var2, var3, null);
   }

   public String a(String var1, String var2, String var3, Consumer<io.karatelabs.ext.rules.twin.TwinModel> var4) {
      Path var5 = this.D.resolve(var1);
      if (!Files.isDirectory(var5)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "no rulebook '" + var1 + "'");
      } else if (var2 != null && !var2.isBlank()) {
         io.karatelabs.ext.rules.twin.TwinModel var6 = io.karatelabs.ext.rules.twin.TwinModelLoader.a(var2);
         if (var4 != null) {
            var4.accept(var6);
         }

         Path var7 = v.a(var5);
         this.c(var7, var3);
         d(var7, var2);
         return b(var7);
      } else {
         throw new RuleApiException(400, "PARSE_FAILED", "Rule.twin.update requires twin.js text");
      }
   }

   public Map<String, Object> b(String var1, List<Map<String, Object>> var2, String var3) {
      Path var4 = this.D.resolve(var1);
      io.karatelabs.ext.rules.twin.TwinModel var5 = this.a(var4, var1);
      Path var6 = w.a(var4);
      this.c(var6, var3);
      if (var2.size() != 1) {
         for (Map var8 : var2) {
            if (var8 != null && var8.containsKey("promote")) {
               throw sequenceError(null, null, null, "a promote row is created alone — Rule.sequence.create(rule, {promote: {plan?, counterexample | required | play}})");
            }
         }
      }

      var var18 = new ArrayList<>(b(var4, var1));
      ArrayList var19 = new ArrayList();

      for (Map var10 : var2) {
         LinkedHashMap var11 = new LinkedHashMap(var10 == null ? Map.of() : var10);
         boolean var12 = var11.containsKey("promote");
         Map var13 = var12 ? this.a(var11, var5, var1) : Map.of("kind", "authored");
         a(var11, var5.f().keySet(), var12);
         if ("play".equals(var13.get("kind"))) {
            var11.put("_ai", true);
         }

         if (var11.get("_id") instanceof String var15) {
            for (Map var17 : (Iterable<Map>) (Iterable<?>) (var18)) {
               if (var15.equals(var17.get("_id"))) {
                  throw sequenceError(var15, null, null, "duplicate sequence id '" + var15 + "'");
               }
            }
         } else {
            var11.put("_id", b(var18, this.E, "seq"));
         }

         var11.put("_from", var13);
         var18.add(e(var11));
         var19.add(var11.get("_id"));
      }

      this.a(var4, var18);
      LinkedHashMap var20 = new LinkedHashMap();
      var20.put("ids", var19);
      var20.put("mtime", b(var6));
      return var20;
   }

   private Map<String, Object> a(Map<String, Object> var1, io.karatelabs.ext.rules.twin.TwinModel var2, String var3) {
      String var4 = var1.get("_id") instanceof String var5 ? var5 : null;

      for (String var12 : var1.keySet()) {
         if (!"promote".equals(var12) && !"_id".equals(var12) && !"_label".equals(var12)) {
            throw sequenceError(var4, null, null, "a promote row carries only promote, _id and _label — the steps come from the walk, never from '" + var12 + "'");
         }
      }

      f(var1);
      if (var1.remove("promote") instanceof Map var11) {
         for (Object var7 : var11.keySet()) {
            if (!"plan".equals(var7) && !"counterexample".equals(var7) && !"required".equals(var7) && !"play".equals(var7)) {
               throw sequenceError(var4, null, null, "promote carries only plan, counterexample, required and play, not '" + var7 + "'");
            }
         }

         Object var15 = b(var11, "counterexample");
         Object var16 = b(var11, "required");
         Object var8 = b(var11, "play");
         if ((var15 != null ? 1 : 0) + (var16 != null ? 1 : 0) + (var8 != null ? 1 : 0) != 1) {
            throw sequenceError(var4, null, null, "promote takes exactly one of counterexample, required and play — a row comes from one origin");
         } else if (var8 == null) {
            Object var9 = b(var11.get("plan"));
            return var16 != null ? this.b(var1, var2, var3, var4, var9, var16) : this.a(var1, var2, var3, var4, var9, var15);
         } else if (b(var11, "plan") != null) {
            throw sequenceError(var4, null, null, "promote.play takes no plan — nothing walks");
         } else if (var8 instanceof String var17 && !var17.isEmpty()) {
            return io.karatelabs.ext.rules.twin.LiveConformanceRunner.a(this, this.F, var3, var17, var1);
         } else {
            throw sequenceError(var4, null, null, "promote.play must be the id of a record the twin-backed mock wrote (play-<16 hex>)");
         }
      } else {
         throw sequenceError(var4, null, null, "promote must be {plan?, counterexample: '<id>' | required: {id, rows, sha}} or {play: '<record id>'}");
      }
   }

   private static Object b(Object var0) {
      if (var0 instanceof Map var1 && var1.get("focus") instanceof List var2 && !var2.isEmpty()) {
         throw new RuleApiException(400, "PLAN_INVALID", "plan focus: a promotion plan carries no focus — promotion grades the one row it names")
            .a(Map.of("field", "focus"));
      } else {
         return var0;
      }
   }

   private static Object b(Map<?, ?> var0, String var1) {
      Object var2 = var0.get(var1);
      return var2 == Terms.UNDEFINED ? null : var2;
   }

   private Map<String, Object> a(Map<String, Object> var1, io.karatelabs.ext.rules.twin.TwinModel var2, String var3, String var4, Object var5, Object var6) {
      if (var6 instanceof String var7 && !var7.isEmpty()) {
         io.karatelabs.ext.rules.twin.TwinWalkPlan var8 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var5, var2, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, Set.of());
         io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var9 = this.a(var2, var8, var3);

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample var11 : var9.a().m()) {
            if (var11.a().equals(var7)) {
               var1.put("steps", d(var11.g()));
               var1.putIfAbsent("_label", var11.c() == null ? var11.b() + " at " + var11.d() : var11.b() + ": " + var11.c());
               var var12 = a(var9.a(), var8);
               var12.put("counterexample", var7);
               return var12;
            }
         }

         throw new RuleApiException(
               404,
               "COUNTEREXAMPLE_NOTFOUND",
               "this walk found no counterexample " + var7 + " — re-run Twin.explore under the same plan and promote an id it reports"
            )
            .a(Map.of("counterexample", var7));
      } else {
         throw sequenceError(var4, null, null, "promote.counterexample must be the id of a counterexample Twin.explore reported under the same plan");
      }
   }

   private Map<String, Object> b(Map<String, Object> var1, io.karatelabs.ext.rules.twin.TwinModel var2, String var3, String var4, Object var5, Object var6) {
      if (!(var6 instanceof Map var7 && var7.get("id") instanceof String var8 && !var8.isEmpty())) {
         throw sequenceError(var4, null, null, "promote.required must be {id, rows, sha} — the row named inside the document Twin.check grades");
      } else if (!(var7.get("sha") instanceof String)) {
         throw new RuleApiException(400, "REQUIRED_INVALID", "required sha: must be the caller's digest of the rows").a(Map.of("field", "sha"));
      } else {
         LinkedHashMap var16 = new LinkedHashMap();

         for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            if (!"id".equals(var11.getKey())) {
               if (!"rows".equals(var11.getKey()) && !"sha".equals(var11.getKey())) {
                  throw sequenceError(var4, null, null, "promote.required carries only id, rows and sha, not '" + var11.getKey() + "'");
               }

               var16.put(String.valueOf(var11.getKey()), var11.getValue());
            }
         }

         io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier var17 = io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.fromConfig(var16, var2);
         String var18 = var17.kindOf(var8);
         if (var18 == null) {
            throw b(var8, "the document carries no row '" + var8 + "'");
         } else if (!B.contains(var18)) {
            throw new RuleApiException(400, "SEQUENCE_INVALID", "a " + var18 + " row is not promotable — it holds over every applicable observation, never one sequence")
               .a(Map.of("required", var8, "kind", var18));
         } else {
            io.karatelabs.ext.rules.twin.TwinWalkPlan var12 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var5, var2, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, Set.of());
            io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var13 = this.a(var2, var12, var3);
            var var14 = var17.verifyRow(var13.a(), var13.b(), var8);
            if (var14 == null) {
               throw b(var8, "this walk never observed required row " + var8 + " — Twin.check reports it unreached under the same plan");
            } else {
               var1.put("steps", d(var14));
               var1.putIfAbsent("_label", "required: " + var8);
               var var15 = a(var13.a(), var12);
               var15.put("required", var8);
               var15.put("requiredSha", io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.canonicalize(var16.get("rows")));
               return var15;
            }
         }
      }
   }

   private io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution a(io.karatelabs.ext.rules.twin.TwinModel var1, io.karatelabs.ext.rules.twin.TwinWalkPlan var2, String var3) {
      TwinRuleRuntime var5 = new TwinRuleRuntime(this, var3);

      io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var4;
      try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var6 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope(
            "twin-walk", io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_WALK_TIMEOUT_MS", 630000L)
         )) {
         var4 = var6.a(() -> io.karatelabs.ext.rules.twin.TwinWalker.b(var1, var2, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var5), Twin.MAPPER);
      } catch (Throwable var12) {
         try {
            var5.close();
         } catch (Throwable var9) {
            var12.addSuppressed(var9);
         }

         throw var12;
      }

      var5.close();
      io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling var13 = var4.a().j();
      if (!"exhausted".equals(var13.a())) {
         LinkedHashMap var14 = new LinkedHashMap();
         var14.put("hit", var13.a());
         var14.put("alsoTripped", var13.b());
         var14.put("frontier", var13.c());
         throw new RuleApiException(409, "PROMOTION_INCOMPLETE", "the promotion walk tripped " + var13.a() + " — narrow it with promote.plan {depth, levels} and retry")
            .a(var14);
      } else {
         return var4;
      }
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult var0, io.karatelabs.ext.rules.twin.TwinWalkPlan var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("kind", "explore");
      var2.put("walk", var0.c());
      var2.put("plan", var1.a());
      var2.put("planSha", var1.f());
      return var2;
   }

   static List<Map<String, Object>> d(List<Map<String, Object>> var0) {
      ArrayList var1 = new ArrayList(var0.size());

      for (Map var3 : var0) {
         var1.add((Map)io.karatelabs.ext.rules.twin.CanonicalJson.d(var3));
      }

      return var1;
   }

   private static RuleApiException b(String var0, String var1) {
      return new RuleApiException(404, "REQUIRED_NOTFOUND", var1).a(Map.of("required", var0));
   }

   public Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.TwinModel var2) {
      return RuleVerificationService.a(this.D, this, var1, var2);
   }

   public Map<String, Object> b(String var1, String var2, Map<String, Object> var3, String var4) {
      Path var5 = this.D.resolve(var1);
      io.karatelabs.ext.rules.twin.TwinModel var6 = this.a(var5, var1);
      Path var7 = w.a(var5);
      this.c(var7, var4);
      if (var3 != null && var3.containsKey("_id")) {
         throw sequenceError(var2, null, null, "_id is immutable — delete the row and create it anew");
      } else if (var3 != null && var3.containsKey("_from")) {
         throw sequenceError(var2, null, null, "_from is engine-written — omit it");
      } else if (var3 != null && var3.containsKey("_ai")) {
         throw sequenceError(var2, null, null, "_ai is engine-written — only a human commit clears it");
      } else {
         var var8 = new ArrayList<>(b(var5, var1));
         boolean var9 = false;

         for (int var10 = 0; var10 < var8.size(); var10++) {
            if (var2.equals(((Map)var8.get(var10)).get("_id"))) {
               LinkedHashMap var11 = new LinkedHashMap((Map)var8.get(var10));
               if (var3 != null) {
                  var11.putAll(var3);
                  a((Map<String, Object>)var8.get(var10), var11);
               }

               a(var11, var6.f().keySet(), true);
               var8.set(var10, e(var11));
               var9 = true;
            }
         }

         if (!var9) {
            throw new RuleApiException(404, "SEQUENCE_NOT_FOUND", "no sequence " + var2 + " in rule " + var1);
         } else {
            this.a(var5, var8);
            return Map.of("mtime", b(var7));
         }
      }
   }

   private static void a(Map<String, Object> var0, Map<String, Object> var1) {
      if (var0.get("_from") instanceof Map var2 && "play".equals(var2.get("kind")) && var2.containsKey("confirmation")) {
         for (String var4 : List.of("steps", "_expect", "_req")) {
            if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.get(var4)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get(var4)))) {
               LinkedHashMap var5 = new LinkedHashMap(var2);
               var5.remove("confirmation");
               var1.put("_from", var5);
               return;
            }
         }
      }
   }

   public Map<String, Object> f(String var1, String var2, String var3) {
      Path var4 = this.D.resolve(var1);
      this.a(var4, var1);
      Path var5 = w.a(var4);
      this.c(var5, var3);
      ArrayList var6 = new ArrayList();
      boolean var7 = false;

      for (Map var9 : b(var4, var1)) {
         if (var2.equals(var9.get("_id"))) {
            var7 = true;
         } else {
            var6.add(var9);
         }
      }

      if (!var7) {
         throw new RuleApiException(404, "SEQUENCE_NOT_FOUND", "no sequence " + var2 + " in rule " + var1);
      } else {
         this.a(var4, var6);
         return Map.of("mtime", b(var5));
      }
   }

   private io.karatelabs.ext.rules.twin.TwinModel a(Path var1, String var2) {
      if (!Files.isDirectory(var1)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "no rulebook '" + var2 + "'");
      } else {
         Path var3 = v.a(var1);
         if (!Files.isRegularFile(var3)) {
            throw new RuleApiException(404, "TWIN_NOT_FOUND", "rulebook '" + var2 + "' has no twin.js — author one with Rule.twin.update(name, source)");
         } else {
            return io.karatelabs.ext.rules.twin.TwinModelLoader.a(f(var3, var2));
         }
      }
   }

   private static List<Map<String, Object>> b(Path var0, String var1) {
      var var2 = k(a(var0, w, var1));
      if (var2 == null) {
         return List.of();
      } else {
         ArrayList var3 = new ArrayList();

         for (Object var5 : (List)var2.get("sequences")) {
            var3.add(new LinkedHashMap((Map)var5));
         }

         return var3;
      }
   }

   private static Map<String, Object> k(String var0) {
      if (var0 == null) {
         return null;
      } else {
         Object var1;
         try {
            var1 = Json.parseStrict(var0, true);
         } catch (RuntimeException var7) {
            throw new RuleApiException(400, "PARSE_FAILED", "sequences.json parse failed: " + var7.getMessage(), var7);
         }

         if (!(var1 instanceof Map var2)) {
            throw new RuleApiException(400, "PARSE_FAILED", "sequences.json must be a JSON object {version, provenance, sequences}");
         } else {
            Object var3 = var2.get("version");
            if ((var3 instanceof Integer || var3 instanceof Long) && ((Number)var3).longValue() == 1L) {
               Object var5 = var2.get("sequences");
               if (var5 instanceof List) {
                  for (Object var6 : (List)var5) {
                     if (!(var6 instanceof Map)) {
                        throw new RuleApiException(400, "PARSE_FAILED", "sequences.json entry is not an object");
                     }
                  }

                  return var2;
               } else {
                  throw new RuleApiException(400, "PARSE_FAILED", "sequences.json 'sequences' must be a JSON array");
               }
            } else {
               throw new RuleApiException(400, "SEQUENCES_VERSION_UNSUPPORTED", "sequences.json declares version " + var3 + " — this build reads version 1 only");
            }
         }
      }
   }

   private void a(Path var1, List<Map<String, Object>> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("version", 1);
      var3.put("provenance", this.d(var1));
      var3.put("sequences", var2);
      d(w.a(var1), c((Object)var3));
   }

   private Map<String, String> d(Path var1) {
      var var2 = this.e(var1);
      return a(var2, a((String)var2.get(r.a()), RuleTableCatalog.a(var1).c()));
   }

   public static Map<String, String> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      return a(var0.i(), var0.n());
   }

   private static Map<String, String> a(Map<String, String> var0, io.karatelabs.ext.rules.RulebookRepository.DigestReference var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("twin", (String)var0.get("twin"));
      var2.put(r.a(), var1.c());

      for (io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec var4 : List.of(s, u)) {
         String var5 = (String)var0.get(var4.a());
         if (var5 != null) {
            var2.put(var4.a(), var5);
         }
      }

      return var2;
   }

   private static Map<String, Object> e(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (String var3 : List.of("_id", "_label", "_from", "steps", "_expect", "_req", "_ai")) {
         if (var0.containsKey(var3)) {
            var1.put(var3, var0.get(var3));
         }
      }

      var1.putAll(var0);
      return var1;
   }

   private static RuleApiException sequenceError(String var0, Integer var1, String var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      if (var0 != null) {
         var4.put("id", var0);
      }

      if (var1 != null) {
         var4.put("at", var1);
      }

      if (var2 != null) {
         var4.put("command", var2);
      }

      return new RuleApiException(400, "SEQUENCE_INVALID", var3).a(var4);
   }

   private static void f(Map<String, Object> var0) {
      Object var1 = var0.get("_id");
      String var2 = var1 instanceof String var3 ? var3 : null;
      if (var1 == null || var2 != null && b.matcher(var2).matches()) {
         if (var2 != null && a.matcher(var2).matches()) {
            throw sequenceError(var2, null, null, "sequence id '" + var2 + "' has the scenario id form sc-<6 hex> — choose another id");
         } else if (var0.get("_label") != null && !(var0.get("_label") instanceof String)) {
            throw sequenceError(var2, null, null, "_label must be a string");
         }
      } else {
         throw sequenceError(
            var2,
            null,
            null,
            "sequence id '" + var1 + "' is not valid — lowercase letters, digits and '-' only, starting with a letter or digit (64 chars max)"
         );
      }
   }

   private static void a(Map<String, Object> var0, Set<String> var1, boolean var2) {
      f(var0);
      String var3 = var0.get("_id") instanceof String var4 ? var4 : null;

      for (String var16 : var0.keySet()) {
         if (a(var16)) {
            if (!"_from".equals(var16) && !"_ai".equals(var16)) {
               if (!"_id".equals(var16) && !"_label".equals(var16) && !"_expect".equals(var16) && !"_req".equals(var16)) {
                  throw sequenceError(var3, null, null, "unknown engine-owned key '" + var16 + "' — a sequence row carries only _id, _label, _expect and _req");
               }
            } else if (!var2) {
               throw sequenceError(var3, null, null, var16 + " is engine-written — omit it");
            }
         }
      }

      if (var0.containsKey("_req")) {
         Object var14 = var0.get("_req");
         List var17 = var14 instanceof List var6 ? var6 : Collections.singletonList(var14);
         boolean var21 = (var14 instanceof String || var14 instanceof List) && !var17.isEmpty();

         for (Object var8 : var17) {
            var21 &= var8 instanceof String var9 && !var9.isBlank();
         }

         if (!var21) {
            throw sequenceError(var3, null, null, "_req is a criterion id string, or a non-empty array of them, in the '<reqId>/<acId>' vocabulary");
         }
      }

      if (!(var0.get("steps") instanceof List var15)) {
         throw sequenceError(var3, null, null, "steps is required — an array of {command, args?}, possibly empty");
      } else {
         int var19 = 0;

         while (true) {
            if (var19 < var15.size()) {
               if (!(var15.get(var19) instanceof Map var22)) {
                  throw sequenceError(var3, var19, null, "step is not an object");
               }

               if (var22.get("command") instanceof String var25 && !var25.isEmpty()) {
                  if (!var1.contains(var25)) {
                     throw sequenceError(var3, var19, var25, "command '" + var25 + "' is not declared by twin.js (declared: " + String.join(", ", var1) + ")");
                  }

                  Object var28 = var22.get("args");
                  if (var28 != null) {
                     if (!(var28 instanceof Map)) {
                        throw sequenceError(var3, var19, var25, "args must be an object");
                     }

                     try {
                        io.karatelabs.ext.rules.twin.CanonicalJson.b(var28);
                     } catch (io.karatelabs.ext.rules.twin.SnapshotException var11) {
                        RuleApiException var30 = sequenceError(var3, var19, var25, "args have no canonical form: " + var11.a + " at " + var11.b);
                        var30.e().put("kind", var11.a);
                        var30.e().put("path", var11.b);
                        throw var30;
                     }
                  }

                  var19++;
                  continue;
               }

               throw sequenceError(var3, var19, null, "step has no 'command'");
            }

            Object var20 = var0.get("_expect");
            if (var20 == null) {
               return;
            }

            if (!(var20 instanceof Map)) {
               throw sequenceError(var3, null, null, "_expect: must be an object");
            }

            try {
               io.karatelabs.ext.rules.twin.ExpectationEvaluator.a((Map<String, Object>)var20, var15.size());
               return;
            } catch (io.karatelabs.ext.rules.twin.TwinException var12) {
               var var23 = var12.d();
               Integer var26 = var23 != null && var23.get("at") instanceof Integer var29 ? var29 : null;
               throw sequenceError(var3, var26, null, var12.getMessage());
            }
         }
      }
   }

   private void c(Path var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         String var3 = Files.isRegularFile(var1) ? b(var1) : null;
         if (!var2.equals(var3)) {
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("path", var1.toString());
            var4.put("expectedMtime", var2);
            var4.put("currentMtime", var3);

            try {
               var4.put("currentBody", Files.readString(var1, StandardCharsets.UTF_8));
            } catch (IOException var6) {
            }

            throw new RuleApiException(
                  409,
                  "STALE_MTIME",
                  "stale write at " + var1.getFileName() + ": expected sha=" + var2 + ", current=" + (var3 == null ? "(no such file yet)" : var3)
               )
               .a(var4);
         }
      }
   }

   static RuleApiException a(RuntimeException var0) {
      String var1 = var0.getMessage();
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("message", var1);
      if (var1 != null) {
         Matcher var3 = q.matcher(var1);
         if (var3.find()) {
            try {
               var2.put("line", Integer.parseInt(var3.group(1)));
               var2.put("column", Integer.parseInt(var3.group(2)));
            } catch (NumberFormatException var5) {
            }
         }
      }

      return new RuleApiException(400, "PARSE_FAILED", "calc.js parse failed: " + var1, var0).a(var2);
   }

   private static void d(Path var0, String var1) {
      try {
         Files.writeString(var0, var1, StandardCharsets.UTF_8);
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   private static String e(List<Map<String, Object>> var0) {
      return c((Object)var0);
   }

   private static String c(Object var0) {
      return Json.of(var0).toStringPretty() + "\n";
   }

   private static byte[] e(Path var0, String var1) {
      if (!Files.isRegularFile(var0)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "missing " + var0.getFileName() + " for rule '" + var1 + "'");
      } else {
         try {
            return Files.readAllBytes(var0);
         } catch (IOException var3) {
            throw new UncheckedIOException(var3);
         }
      }
   }

   private static String f(Path var0, String var1) {
      if (!Files.isRegularFile(var0)) {
         throw new RuleApiException(404, "RULE_NOT_FOUND", "missing " + var0.getFileName() + " for rule '" + var1 + "'");
      } else {
         try {
            return Files.readString(var0, StandardCharsets.UTF_8);
         } catch (IOException var3) {
            throw new UncheckedIOException(var3);
         }
      }
   }

   private static String a(Path var0, io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec var1, String var2) {
      Path var3 = var1.a(var0);
      return Files.exists(var3) ? f(var3, var2) : null;
   }

   private Map<String, String> e(Path var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.RulebookRepository.RulebookSourceSpec var4 : x) {
         Path var5 = var4.a(var1);
         if (var4.c() || Files.exists(var5)) {
            var2.put(var4.a(), b(var5));
         }
      }

      return var2;
   }

   static String b(Path var0) {
      try {
         MessageDigest var1 = MessageDigest.getInstance("SHA-256");
         return HexFormat.of().formatHex(var1.digest(Files.readAllBytes(var0)));
      } catch (NoSuchAlgorithmException var2) {
         throw new AssertionError("SHA-256 is always available", var2);
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   static String f(String var0) {
      try {
         MessageDigest var1 = MessageDigest.getInstance("SHA-256");
         return HexFormat.of().formatHex(var1.digest(var0.getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException var2) {
         throw new AssertionError("SHA-256 is always available", var2);
      }
   }

   private static Object l(String var0) {
      return g(var0).a();
   }

   static io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult g(String var0) {
      return b(var0, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_SCHEMA_TIMEOUT_MS", 5000L));
   }

   static Object a(String var0, long var1) {
      return b(var0, var1).a();
   }

   static io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult b(String var0, long var1) {
      io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult var4;
      try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var3 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("rule-schema", var1)) {
         var4 = var3.a(() -> m(var0), G);
      }

      return var4;
   }

   private static io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult m(String var0) {
      Engine var1 = new Engine();

      try {
         var1.eval(var0);
      } catch (RuntimeException var3) {
         throw new RuleApiException(400, "PARSE_FAILED", "schema.js eval failed: " + var3.getMessage(), var3);
      }

      Object var2 = var1.get("schema");
      if (var2 == null) {
         throw new RuleApiException(400, "PARSE_FAILED", "schema.js does not define a `schema = {...}`");
      } else {
         e(var2);
         a(var2, "");
         return new io.karatelabs.ext.rules.RulebookRepository.SchemaEvaluationResult(var2, d(var1.get("figures")));
      }
   }

   private static List<String> d(Object var0) {
      if (var0 != null && var0 != Terms.UNDEFINED) {
         if (var0 instanceof List var1 && !var1.isEmpty()) {
            ArrayList var2 = new ArrayList();
            Iterator var3 = var1.iterator();

            while (var3.hasNext()) {
               if (!(var3.next() instanceof String var5) || var5.isBlank() || var2.contains(var5)) {
                  throw new RuleApiException(400, "PARSE_FAILED", "schema.js `figures` is a list of distinct output field names");
               }

               var2.add(var5);
            }

            return List.copyOf(var2);
         } else {
            throw new RuleApiException(400, "PARSE_FAILED", "schema.js `figures` is a list of distinct output field names");
         }
      } else {
         return null;
      }
   }

   private static void a(Object var0, String var1) {
      if (var0 instanceof Map var2) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var6 = var1.isEmpty() ? String.valueOf(var5.getKey()) : var1 + "." + var5.getKey();
            a(var5.getValue(), var6);
         }
      } else if (var0 instanceof List var3) {
         if (var3.isEmpty()) {
            throw new RuleApiException(
               400,
               "PARSE_FAILED",
               "schema.js declares an empty list at '"
                  + var1
                  + "' — a list of literals is an ENUM, so an empty one declares a field no value can satisfy. Name the members (`['CA','NY']`), or use a marker (`'#array'`, `'#string'`) if you meant a free-form list"
            );
         }

         for (Object var8 : var3) {
            a(var8, var1 + "[]");
         }
      }
   }

   private static void e(Object var0) {
      if (var0 instanceof Map var1) {
         ArrayList var2 = new ArrayList();

         for (Object var4 : var1.keySet()) {
            if (var4 instanceof String var5 && a(var5)) {
               var2.add(var5);
            }
         }

         if (!var2.isEmpty()) {
            throw new RuleApiException(
               400,
               "PARSE_FAILED",
               "schema.js declares engine-owned key(s) "
                  + var2
                  + " — the shape describes BUSINESS INPUT only, and `_`-prefixed keys are dropped from both sides of the match, so declaring one checks nothing. Delete the line(s); `_label` still works as a row field (it needs no declaration)"
            );
         }
      }
   }

   private List<Map<String, Object>> g(Path var1, String var2) {
      Object var3;
      try {
         var3 = Json.parseStrict(var2, true);
      } catch (RuntimeException var10) {
         throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json parse failed: " + var10.getMessage(), var10);
      }

      if (var3 instanceof List var4) {
         ArrayList var5 = new ArrayList();

         for (Object var7 : var4) {
            if (!(var7 instanceof Map var8)) {
               throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json entry is not an object");
            }

            var5.add(new LinkedHashMap(var8));
         }

         boolean var11 = a(var5, this.E, "scenario");
         c(var5);
         if (var11) {
            try {
               Files.writeString(t.a(var1), e(var5), StandardCharsets.UTF_8);
            } catch (IOException var9) {
               throw new UncheckedIOException(var9);
            }
         }

         return var5;
      } else {
         throw new RuleApiException(400, "PARSE_FAILED", "scenarios.json must be a JSON array, got " + var3.getClass().getSimpleName());
      }
   }

   static boolean a(List<Map<String, Object>> var0, Random var1, String var2) {
      boolean var3 = false;
      HashSet var4 = new HashSet();

      for (Map var6 : var0) {
         if (var6.get("_id") instanceof String var8 && !var8.isEmpty()) {
            if (!o.matcher(var8).matches()) {
               throw new RuleApiException(
                  400, "PARSE_FAILED", var2 + " id '" + var8 + "' is not valid — use only letters, digits, '.', '_' or '-' (no spaces, '/', or ':')"
               );
            }

            if (!var4.add(var8)) {
               throw new RuleApiException(400, "PARSE_FAILED", "duplicate " + var2 + " id '" + var8 + "'");
            }
         } else {
            String var9 = a(var0, var1);
            var6.put("_id", var9);
            var4.add(var9);
            var3 = true;
         }
      }

      return var3;
   }

   static String a(List<Map<String, Object>> var0, Random var1) {
      return b(var0, var1, "sc");
   }

   static String b(List<Map<String, Object>> var0, Random var1, String var2) {
      for (int var3 = 0; var3 < 100; var3++) {
         String var4 = String.format("%s-%06x", var2, var1.nextInt(16777216));
         boolean var5 = false;

         for (Map var7 : var0) {
            if (var4.equals(var7.get("_id"))) {
               var5 = true;
               break;
            }
         }

         if (!var5) {
            return var4;
         }
      }

      throw new IllegalStateException("could not mint a unique id after 100 attempts");
   }

   private String f(List<Map<String, Object>> var1) {
      return a(var1, this.E);
   }

   private int f(Path var1) {
      try {
         if (Json.parseStrict(Files.readString(t.a(var1), StandardCharsets.UTF_8), true) instanceof List var3) {
            int var4 = 0;

            for (Object var6 : var3) {
               if (!(var6 instanceof Map var7 && var7.get("_expect") instanceof String)) {
                  var4++;
               }
            }

            return var4;
         }
      } catch (RuntimeException | IOException var8) {
      }

      return 0;
   }

   public record RulebookSnapshot(String a, String b, RuleTableCatalog c, io.karatelabs.ext.rules.RulebookRepository.DigestReference d) {

      public RulebookSnapshot(String a, String b, RuleTableCatalog c, io.karatelabs.ext.rules.RulebookRepository.DigestReference d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   public record DigestReference(String b, String c) {

      public DigestReference(String b, String c) {
         this.b = b;
         this.c = c;
      }

      public Map<String, Object> a() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("basis", this.b);
         var1.put("sha256", this.c);
         return var1;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }
   }

   private record RulebookSourceSpec(String a, String b, boolean c) {

      private RulebookSourceSpec(String a, String b, boolean c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }

      Path a(Path var1) {
         return var1.resolve(this.b);
      }
   }

   public record RulebookDocument(String d, Object e, List<Map<String, Object>> f, String g, Map<String, Object> h, Map<String, String> i, String j, String k, Map<String, Object> l, RuleTableCatalog m, io.karatelabs.ext.rules.RulebookRepository.DigestReference n, List<String> o) {

      public RulebookDocument(String d, Object e, List<Map<String, Object>> f, String g, Map<String, Object> h, Map<String, String> i, String j, String k, Map<String, Object> l, RuleTableCatalog m, io.karatelabs.ext.rules.RulebookRepository.DigestReference n, List<String> o) {
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
         this.l = l;
         this.m = m;
         this.n = n;
         this.o = o;
      }

      public Map<String, String> a() {
         return this.m.c();
      }

      public List<Map<String, Object>> b() {
         return RulebookRepository.a(this.f);
      }

      public List<Map<String, Object>> c() {
         return RulebookRepository.b(this.f);
      }

      public String d() {
         return this.d;
      }

      public Object e() {
         return this.e;
      }

      public List<Map<String, Object>> f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public Map<String, Object> h() {
         return this.h;
      }

      public Map<String, String> i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public Map<String, Object> l() {
         return this.l;
      }

      public RuleTableCatalog m() {
         return this.m;
      }

      public io.karatelabs.ext.rules.RulebookRepository.DigestReference n() {
         return this.n;
      }

      public List<String> o() {
         return this.o;
      }
   }

   public record RulebookSummary(String a, int b, Map<String, String> c) {

      public RulebookSummary(String a, int b, Map<String, String> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   record SchemaEvaluationResult(Object a, List<String> b) {

      SchemaEvaluationResult(Object a, List<String> b) {
         this.a = a;
         this.b = b;
      }
   }
}
