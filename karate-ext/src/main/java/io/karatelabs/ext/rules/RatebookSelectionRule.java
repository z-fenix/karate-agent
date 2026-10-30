package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RatebookSelectionRule {
   static final String a = "evidence/ratebooks";
   static final String b = "rule.json";
   static final String c = "changes";
   private final String d;
   private final boolean e;
   private final String f;
   private final boolean g;
   private final String h;
   private final Set<String> i;
   private final Set<String> j;
   private final Pattern k;
   private final String l;
   private final String m;
   private final String n;
   private final List<String> o;
   private final List<String> p;
   private final Map<String, String> q = new LinkedHashMap<>();
   private final Map<String, String> r = new LinkedHashMap<>();

   private RatebookSelectionRule(String var1, Map<String, Object> var2) {
      this.d = var1;
      this.e = Boolean.TRUE.equals(var2.get("ai"));
      this.f = d(var2.get("dateKey"), "dateKey");
      if (var2.get("renewal") instanceof Boolean var3) {
         this.g = var3;
         this.h = d(var2.get("combine"), "combine");
         if (!this.h.equals("layer") && !this.h.equals("replace")) {
            throw d("combine is 'layer' or 'replace'");
         } else {
            var var14 = b(var2.get("statuses"), "statuses");
            this.i = new LinkedHashSet<>(c(var14.get("selectable"), "statuses.selectable"));
            this.j = new LinkedHashSet<>(c(var14.get("other"), "statuses.other"));

            for (String var6 : this.i) {
               if (this.j.contains(var6)) {
                  throw d("status '" + var6 + "' is both selectable and other");
               }
            }

            String var15 = d(var2.get("editionId"), "editionId");

            try {
               this.k = Pattern.compile(var15);
            } catch (PatternSyntaxException var13) {
               throw d("editionId is not a regular expression: " + var13.getDescription());
            }

            var var16 = this.k.namedGroups().keySet();
            if (var16.contains("base") && var16.contains("version")) {
               this.l = d(var2.get("filed"), "filed");
               if (!this.l.equals("changes") && !this.l.matches("[A-Za-z0-9_-]+")) {
                  throw d("filed is 'changes' or the name of a filed-records file under evidence/ratebooks/");
               } else {
                  this.m = var2.get("from") == null ? null : a(var2.get("from"), "from");
                  this.n = var2.get("to") == null ? null : a(var2.get("to"), "to");
                  if (this.m != null && this.n != null && this.m.compareTo(this.n) > 0) {
                     throw d("from is on or before to");
                  } else {
                     var var7 = b(var2.get("scope"), "scope");
                     this.o = c(var7.get("jurisdictions"), "scope.jurisdictions");
                     this.p = c(var7.get("lines"), "scope.lines");
                     if (!this.o.isEmpty() && !this.p.isEmpty()) {
                        if (var2.get("editionIds") instanceof Map var8) {
                           for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
                              String var11 = d(var10.getValue(), "editionIds." + var10.getKey());
                              String var12 = this.r.put(var11, String.valueOf(var10.getKey()));
                              if (var12 != null) {
                                 throw d("editionIds: '" + var12 + "' and '" + var10.getKey() + "' both map to '" + var11 + "'");
                              }

                              this.q.put(String.valueOf(var10.getKey()), var11);
                           }
                        } else {
                           throw d("editionIds is an object: our edition id → the carrier's");
                        }
                     } else {
                        throw d("scope declares at least one jurisdiction and one line");
                     }
                  }
               }
            } else {
               throw d("editionId declares the named groups 'base' and 'version' (and optionally 'companion')");
            }
         }
      } else {
         throw d("renewal is true (renewalEffective governs a renewal) or false");
      }
   }

   public static RatebookSelectionRule a(Path var0) {
      Path var1 = var0.resolve("evidence/ratebooks").resolve("rule.json");
      return Files.isRegularFile(var1) ? a(var1, "rule.json") : null;
   }

   public static RatebookSelectionRule a(Path var0, Object var1) {
      if (var1 == null) {
         RatebookSelectionRule var5 = a(var0);
         if (var5 == null) {
            throw new RuleApiException(
               404,
               "RULE_UNDECLARED",
               "no selection rule at "
                  + var0.getFileName()
                  + "/evidence/ratebooks/rule.json — declare dateKey, renewal, combine, statuses, editionId, filed, scope and editionIds; nothing defaults"
            );
         } else {
            return var5;
         }
      } else if (var1 instanceof String var4) {
         if (!var4.matches("[A-Za-z0-9_-]+")) {
            throw d("rule name '" + var4 + "' — letters, digits, '_' or '-' only");
         } else {
            Path var3 = var0.resolve("evidence/ratebooks").resolve(var4 + ".json");
            if (!Files.isRegularFile(var3)) {
               throw new RuleApiException(404, "RULE_UNDECLARED", "no selection rule '" + var4 + "' at " + var0.getFileName() + "/evidence/ratebooks/" + var4 + ".json");
            } else {
               return a(var3, var4);
            }
         }
      } else if (var1 instanceof Map var2) {
         return new RatebookSelectionRule(io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2)), var2);
      } else {
         throw d("rule is a name under evidence/ratebooks/ or an inline rule object");
      }
   }

   private static RatebookSelectionRule a(Path var0, String var1) {
      byte[] var2;
      try {
         var2 = Files.readAllBytes(var0);
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }

      Object var3;
      try {
         var3 = Json.parseStrict(new String(var2, StandardCharsets.UTF_8), true);
      } catch (RuntimeException var5) {
         throw new RuleApiException(400, "PARSE_FAILED", "selection rule '" + var1 + "': " + var5.getMessage());
      }

      return new RatebookSelectionRule(io.karatelabs.ext.common.Sha256.a(var2), b(var3, "rule"));
   }

   public io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution a(String var1) {
      String var2 = this.q.getOrDefault(var1, var1);
      Matcher var3 = this.k.matcher(var2);
      if (!var3.matches()) {
         return new io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution(var1, var1, var1, null, null, false, false);
      } else {
         String var4 = var3.group("base");
         if (var4 == null) {
            return new io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution(var1, var1, var1, null, null, false, false);
         } else {
            String var5 = var3.group("version");
            String var6 = this.k.namedGroups().containsKey("companion") ? var3.group("companion") : null;
            String var7 = var6 == null ? var4 : var4 + var6;
            return new io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution(var1, this.r.getOrDefault(var7, var7), this.r.getOrDefault(var4, var4), var5, var6, true, this.r.containsKey(var7));
         }
      }
   }

   public String a() {
      return this.d;
   }

   public boolean b() {
      return this.e;
   }

   public String c() {
      return this.f;
   }

   public boolean d() {
      return this.g;
   }

   public boolean e() {
      return this.h.equals("layer");
   }

   public Boolean b(String var1) {
      if (this.i.contains(var1)) {
         return true;
      } else {
         return this.j.contains(var1) ? false : null;
      }
   }

   public String f() {
      return this.l;
   }

   public String g() {
      return this.m;
   }

   public String h() {
      return this.n;
   }

   public List<String> i() {
      return this.o;
   }

   public List<String> j() {
      return this.p;
   }

   public Map<String, Object> k() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("sha", this.d);
      if (this.e) {
         var1.put("ai", true);
      }

      return var1;
   }

   private static String a(Object var0, String var1) {
      String var2 = d(var0, var1);
      if (c(var2) == null) {
         throw d(var1 + " is an ISO date (YYYY-MM-DD), not '" + var2 + "'");
      } else {
         return var2;
      }
   }

   static String c(String var0) {
      try {
         return LocalDate.parse(var0).toString();
      } catch (DateTimeParseException var2) {
         return null;
      }
   }

   private static Map<String, Object> b(Object var0, String var1) {
      if (!(var0 instanceof Map)) {
         throw d(var1 + " is an object");
      } else {
         return (Map<String, Object>)var0;
      }
   }

   private static List<String> c(Object var0, String var1) {
      if (!(var0 instanceof List var2)) {
         throw d(var1 + " is a list of strings");
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (Object var5 : var2) {
            var3.add(d(var5, var1 + "[]"));
         }

         return List.copyOf(var3);
      }
   }

   private static String d(Object var0, String var1) {
      if (var0 instanceof String var2 && !var2.isEmpty()) {
         return var2;
      } else {
         throw d(var1 + " is a non-empty string");
      }
   }

   private static RuleApiException d(String var0) {
      return new RuleApiException(400, "RULE_INVALID", "selection rule: " + var0);
   }

   public record EditionResolution(String b, String c, String d, String e, String f, boolean g, boolean h) {

      public EditionResolution(String b, String c, String d, String e, String f, boolean g, boolean h) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }

      boolean a() {
         return this.f != null;
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

      public String f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }

      public boolean h() {
         return this.h;
      }
   }
}
