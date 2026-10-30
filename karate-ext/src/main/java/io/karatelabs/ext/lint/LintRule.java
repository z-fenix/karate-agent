package io.karatelabs.ext.lint;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public record LintRule(String b, String c, SelectorDialect d, FindingSeverity e, String f, String g, String h, List<String> i, GovernanceDimension j, Double k, Map<String, Object> l, io.karatelabs.ext.sourcedoc.SourceDocument.Anchor m, boolean n, io.karatelabs.ext.lint.LintRule.RuleProvenance o, String p, io.karatelabs.ext.lint.LintRule.WaiverPolicy q, String r) {
   static final List<String> KNOWN_RULE_KEYS = List.of(
      "id",
      "given",
      "givenType",
      "severity",
      "message",
      "description",
      "documentationUrl",
      "formats",
      "dimension",
      "cap",
      "params",
      "anchor",
      "resolved",
      "provenance",
      "origin",
      "waiver",
      "then"
   );

   public LintRule(
      String b,
      String c,
      SelectorDialect d,
      FindingSeverity e,
      String f,
      String g,
      String h,
      List<String> i,
      GovernanceDimension j,
      Double k,
      Map<String, Object> l,
      io.karatelabs.ext.sourcedoc.SourceDocument.Anchor m,
      boolean n,
      io.karatelabs.ext.lint.LintRule.RuleProvenance o,
      String p,
      io.karatelabs.ext.lint.LintRule.WaiverPolicy q,
      String r
   ) {
      if (b == null || b.isBlank()) {
         throw new LintException("a lint rule needs an id");
      } else if (c == null || c.isBlank()) {
         throw new LintException("rule '" + b + "': `given` is required (the selector)");
      } else if (r != null && !r.isBlank()) {
         i = i == null ? List.of() : List.copyOf(i);
         l = l == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap(l));
         d = d == null ? io.karatelabs.ext.lint.SelectorDialect.JS : d;
         j = j == null ? io.karatelabs.ext.lint.GovernanceDimension.DESIGN : j;
         m = m == null ? io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.VALUE : m;
         e = e == null ? io.karatelabs.ext.lint.FindingSeverity.WARN : e;
         o = o == null ? io.karatelabs.ext.lint.LintRule.RuleProvenance.AUTHORED : o;
         q = q == null ? io.karatelabs.ext.lint.LintRule.WaiverPolicy.DEFAULT_WAIVER_POLICY : q;
         this.b = b;
         this.c = c;
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
         this.p = p;
         this.q = q;
         this.r = r;
      } else {
         throw new LintException("rule '" + b + "': `then` is required (the predicate)");
      }
   }

   public static LintRule parseRule(String var0, Object var1) {
      if (var1 instanceof Map var2) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var6 = String.valueOf(var5.getKey());
            if (!KNOWN_RULE_KEYS.contains(var6)) {
               throw new LintException(
                  "rule '"
                     + (var0 == null ? var2.get("id") : var0)
                     + "': unknown key '"
                     + var6
                     + "'"
                     + suggestionHint(var6, (Collection<String>)KNOWN_RULE_KEYS)
                     + " (known: "
                     + String.join(", ", KNOWN_RULE_KEYS)
                     + ")"
               );
            }

            var3.put(var6, var5.getValue());
         }

         String var11 = stringValue(var3.get("id"));
         if (var0 != null && var11 != null && !var0.equals(var11)) {
            throw new LintException("rule '" + var0 + "': its spec declares a different id '" + var11 + "' — name it once");
         } else {
            Object var12 = var0 != null ? var0 : var11;
            Object var13 = var3.get("given");
            SelectorDialect var7 = var3.get("givenType") == null ? null : io.karatelabs.ext.lint.SelectorDialect.of(stringValue(var3.get("givenType")));
            String var8;
            if (var13 instanceof String var10) {
               if (var7 == null) {
                  throw new LintException(
                     "rule '"
                        + var12
                        + "': a string `given` must declare `givenType` ("
                        + String.join(" | ", io.karatelabs.ext.lint.SelectorDialect.names())
                        + ") — it is never inferred from the string"
                  );
               }

               var8 = var10;
            } else {
               if (!(var13 instanceof JavaCallable var9)) {
                  if (var13 == null) {
                     throw new LintException("rule '" + var12 + "': `given` is required (the selector)");
                  }

                  throw new LintException("rule '" + var12 + "': `given` must be a selector string or a `$ => …` arrow, got " + var13.getClass().getSimpleName());
               }

               if (var7 == io.karatelabs.ext.lint.SelectorDialect.JSONPATH_PLUS) {
                  throw new LintException(
                     "rule '"
                        + var12
                        + "': `givenType: jsonpath-plus` needs a selector string, not a function — a JSONPath-Plus selector is data the walker evaluates"
                  );
               }

               var7 = io.karatelabs.ext.lint.SelectorDialect.JS;
               var8 = captureGivenSource((String)var12, var9);
            }

            return new LintRule(
               (String)var12,
               var8,
               var7,
               var3.get("severity") == null ? null : io.karatelabs.ext.lint.FindingSeverity.of(stringValue(var3.get("severity"))),
               stringValue(var3.get("message")),
               stringValue(var3.get("description")),
               stringValue(var3.get("documentationUrl")),
               parseStringList((String)var12, "formats", var3.get("formats")),
               var3.get("dimension") == null ? null : io.karatelabs.ext.lint.GovernanceDimension.of(stringValue(var3.get("dimension"))),
               parseCap((String)var12, var3.get("cap")),
               parseParams((String)var12, var3.get("params")),
               parseAnchor((String)var12, var3.get("anchor")),
               parseBoolean((String)var12, "resolved", var3.get("resolved"), true),
               var3.get("provenance") == null ? null : io.karatelabs.ext.lint.LintRule.RuleProvenance.of(stringValue(var3.get("provenance"))),
               stringValue(var3.get("origin")),
               parseWaiverPolicy((String)var12, var3.get("waiver")),
               parseThenSource((String)var12, var3.get("then"))
            );
         }
      } else {
         throw new LintException("rule '" + var0 + "': expected an object of " + String.join("/", KNOWN_RULE_KEYS) + ", got " + (var1 == null ? "null" : var1.getClass().getSimpleName()));
      }
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", this.b);
      var1.put("given", this.c);
      var1.put("givenType", this.d.wire());
      var1.put("severity", this.e.wire());
      if (this.f != null) {
         var1.put("message", this.f);
      }

      if (this.g != null) {
         var1.put("description", this.g);
      }

      if (this.h != null) {
         var1.put("documentationUrl", this.h);
      }

      var1.put("formats", this.i);
      var1.put("dimension", this.j.wire());
      if (this.k != null) {
         var1.put("cap", this.k);
      }

      var1.put("params", this.l);
      var1.put("anchor", this.m.wire());
      var1.put("resolved", this.n);
      var1.put("provenance", this.o.wire());
      if (this.p != null) {
         var1.put("origin", this.p);
      }

      var1.put("waiver", this.q.toMap());
      var1.put("then", this.r);
      return var1;
   }

   private static String captureGivenSource(String var0, JavaCallable var1) {
      String var2 = var1 instanceof JsFunction var3 ? var3.getSource() : null;
      if (var2 != null && !var2.isBlank()) {
         return "(" + var2.trim() + ")($)";
      } else {
         throw new LintException(
            "rule '" + var0 + "': the `given` function carries no source, so it cannot be recorded in the IR — declare `given` as a selector string instead"
         );
      }
   }

   private static String parseThenSource(String var0, Object var1) {
      if (var1 instanceof String var3) {
         return var3;
      } else if (var1 instanceof JsFunction var2 && var2.getSource() != null && !var2.getSource().isBlank()) {
         return var2.getSource().trim();
      } else if (var1 instanceof JavaCallable) {
         throw new LintException(
            "rule '"
               + var0
               + "': the `then` predicate carries no source, so it cannot be recorded in the IR (or compiled in the sandbox) — declare it as JS text"
         );
      } else if (var1 == null) {
         throw new LintException("rule '" + var0 + "': `then` is required (the predicate)");
      } else {
         throw new LintException("rule '" + var0 + "': `then` must be a JS function or its source text, got " + var1.getClass().getSimpleName());
      }
   }

   private static io.karatelabs.ext.sourcedoc.SourceDocument.Anchor parseAnchor(String var0, Object var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            return io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.of(String.valueOf(var1));
         } catch (RuntimeException var3) {
            throw new LintException("rule '" + var0 + "': `anchor` must be 'key' or 'value', got '" + var1 + "'");
         }
      }
   }

   private static io.karatelabs.ext.lint.LintRule.WaiverPolicy parseWaiverPolicy(String var0, Object var1) {
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Boolean var5) {
         return var5 ? io.karatelabs.ext.lint.LintRule.WaiverPolicy.DEFAULT_WAIVER_POLICY : new io.karatelabs.ext.lint.LintRule.WaiverPolicy(false, List.of());
      } else if (var1 instanceof Map var2) {
         boolean var3 = parseBoolean(var0, "waiver.waivable", var2.get("waivable"), true);
         var var4 = parseStringList(var0, "waiver.requires", var2.get("requires"));
         return new io.karatelabs.ext.lint.LintRule.WaiverPolicy(var3, var4 == null ? io.karatelabs.ext.lint.LintRule.WaiverPolicy.DEFAULT_WAIVER_POLICY.c() : var4);
      } else {
         throw new LintException("rule '" + var0 + "': `waiver` must be a boolean or {waivable, requires:[…]}, got " + var1.getClass().getSimpleName());
      }
   }

   private static Map<String, Object> parseParams(String var0, Object var1) {
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Map var2) {
         return var2;
      } else {
         throw new LintException("rule '" + var0 + "': `params` must be an object, got " + var1.getClass().getSimpleName());
      }
   }

   private static Double parseCap(String var0, Object var1) {
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Number var2) {
         double var3 = var2.doubleValue();
         if (!(var3 < 0.0) && !(var3 > 1.0)) {
            return var3;
         } else {
            throw new LintException("rule '" + var0 + "': `cap` must be in [0,1] (a dimension score, not a percentage), got " + var3);
         }
      } else {
         throw new LintException("rule '" + var0 + "': `cap` must be a number in [0,1], got " + var1.getClass().getSimpleName() + " '" + var1 + "'");
      }
   }

   private static boolean parseBoolean(String var0, String var1, Object var2, boolean var3) {
      if (var2 == null) {
         return var3;
      } else if (var2 instanceof Boolean var4) {
         return var4;
      } else {
         throw new LintException("rule '" + var0 + "': `" + var1 + "` must be true or false, got " + var2.getClass().getSimpleName() + " '" + var2 + "'");
      }
   }

   private static List<String> parseStringList(String var0, String var1, Object var2) {
      if (var2 == null) {
         return null;
      } else if (var2 instanceof String var8) {
         return List.of(var8);
      } else if (var2 instanceof List var3) {
         ArrayList<java.lang.String> var4 = new ArrayList(var3.size());

         for (Object var6 : var3) {
            if (!(var6 instanceof String var7)) {
               throw new LintException("rule '" + var0 + "': every `" + var1 + "` entry must be a string, got " + (var6 == null ? "null" : var6.getClass().getSimpleName()));
            }

            var4.add(var7);
         }

         return var4;
      } else {
         throw new LintException("rule '" + var0 + "': `" + var1 + "` must be a string or a list of strings, got " + var2.getClass().getSimpleName());
      }
   }

   private static String stringValue(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static String suggestionHint(String var0, Collection<String> var1) {
      var var2 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var0, var1);
      return var2.isEmpty() ? "" : " — did you mean " + String.join(" / ", var2) + "?";
   }

   public static enum RuleProvenance {
      AUTHORED("authored"),
      CONVERTED("converted");

      private final String wire;

      private RuleProvenance(String var3) {
         this.wire = var3;
      }

      public String wire() {
         return this.wire;
      }

      public static io.karatelabs.ext.lint.LintRule.RuleProvenance of(String var0) {
         for (io.karatelabs.ext.lint.LintRule.RuleProvenance var4 : values()) {
            if (var4.wire.equals(var0)) {
               return var4;
            }
         }

         throw new LintException("unknown provenance: '" + var0 + "' (known: authored, converted)");
      }
   }

   public record WaiverPolicy(boolean b, List<String> c) {
      public static final io.karatelabs.ext.lint.LintRule.WaiverPolicy DEFAULT_WAIVER_POLICY = new io.karatelabs.ext.lint.LintRule.WaiverPolicy(true, List.of("reason", "owner"));


      public Map<String, Object> toMap() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("waivable", this.b);
         var1.put("requires", this.c);
         return var1;
      }
   }
}
