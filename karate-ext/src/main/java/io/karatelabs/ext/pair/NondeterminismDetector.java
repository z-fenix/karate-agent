package io.karatelabs.ext.pair;

import io.karatelabs.js.Engine;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class NondeterminismDetector {
   public static final String IGNORE_PROFILE_NAME = "nondeterminism";
   private final List<io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule> ignoreRules;
   private final List<Map<String, Object>> rejectedRules;
   private final Object rawConfig;
   private final Map<String, Integer> matchCounts = new LinkedHashMap<>();

   private NondeterminismDetector(List<io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule> var1, List<Map<String, Object>> var2, Object var3) {
      this.ignoreRules = var1;
      this.rejectedRules = var2;
      this.rawConfig = var3;

      for (io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule var5 : var1) {
         this.matchCounts.put(var5.b(), 0);
      }
   }

   public NondeterminismDetector copy() {
      return new NondeterminismDetector(this.ignoreRules, this.rejectedRules, this.rawConfig);
   }

   public static NondeterminismDetector empty() {
      return new NondeterminismDetector(List.of(), List.of(), null);
   }

   public Object rawConfig() {
      return this.rawConfig;
   }

   public static NondeterminismDetector fromConfig(Object var0) {
      if (var0 == null) {
         return empty();
      } else {
         ArrayList var1 = new ArrayList();
         if (var0 instanceof List var2) {
            var1.addAll(var2);
         } else {
            var1.add(var0);
         }

         ArrayList var10 = new ArrayList();
         ArrayList var3 = new ArrayList();

         for (Object var5 : var1) {
            if (var5 instanceof String var6) {
               if (!"nondeterminism".equals(var6.trim())) {
                  throw new PairException(
                     "unknown ignore profile '"
                        + var6
                        + "' — the only bundled one is 'nondeterminism'. Nothing is ignored by default (D223c): declare your own rules as {id, key|scope, marker, reason, owner} objects."
                  );
               }

               var10.addAll(bundledRules());
            } else {
               try {
                  var10.add(io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule.ruleFromMap(var5));
               } catch (PairException var9) {
                  LinkedHashMap var7 = new LinkedHashMap();
                  var7.put("spec", var5 instanceof Map var8 ? new LinkedHashMap(var8) : String.valueOf(var5));
                  var7.put("refused", var9.getMessage());
                  var3.add(var7);
               }
            }
         }

         return new NondeterminismDetector(var10, var3, var0);
      }
   }

   private static List<io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule> bundledRules() {
      String var0 = "karate-labs (bundled 'nondeterminism' profile)";
      return List.of(
         new io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule("generated-id", null, "id", "#string", "a server-generated identifier differs between two targets by construction", var0),
         new io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule("created-timestamp", null, "createdAt", "#string", "a creation timestamp is written by whichever target served the call", var0),
         new io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule("updated-timestamp", null, "updatedAt", "#string", "an update timestamp is written by whichever target served the call", var0),
         new io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule("correlation-id", null, "requestId", "#string", "a per-request correlation id is unique to the request that produced it", var0)
      );
   }

   public io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule matchRule(String var1, Object var2, Object var3) {
      for (io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule var5 : this.ignoreRules) {
         if (var5.a(var1) && matchesMarker(var5.e(), var2) && matchesMarker(var5.e(), var3)) {
            this.matchCounts.merge(var5.b(), 1, Integer::sum);
            return var5;
         }
      }

      return null;
   }

   private static boolean matchesMarker(String var0, Object var1) {
      try {
         Result var2 = Match.execute(new Engine(), Match.Type.EQUALS, var1, var0);
         return var2.pass;
      } catch (RuntimeException var3) {
         return false;
      }
   }

   public boolean isEmpty() {
      return this.ignoreRules.isEmpty() && this.rejectedRules.isEmpty();
   }

   public List<Map<String, Object>> rejectedRules() {
      return this.rejectedRules;
   }

   public Map<String, Object> summary() {
      int var1 = 0;
      int var2 = 0;
      ArrayList var3 = new ArrayList();

      for (io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule var5 : this.ignoreRules) {
         int var6 = this.matchCounts.getOrDefault(var5.b(), 0);
         var1 += var6;
         var var7 = var5.a();
         var7.put("matched", var6);
         if (var6 == 0) {
            var7.put("stale", true);
            var2++;
         }

         var3.add(var7);
      }

      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("count", var1);
      var8.put("stale", var2);
      var8.put("rules", var3);
      if (!this.rejectedRules.isEmpty()) {
         var8.put("rejected", this.rejectedRules);
      }

      var8.put(
         "note",
         this.ignoreRules.isEmpty()
            ? "No rule waived a difference. Nothing is ignored by default, so the run compared every difference below on its merits."
            : this.ignoreRules.size()
               + " rule"
               + (this.ignoreRules.size() == 1 ? "" : "s")
               + " re-classified "
               + var1
               + " difference"
               + (var1 == 1 ? "" : "s")
               + " as `ignored`. The rules did not suppress them: each difference is kept with both values and the rule that covered it, on its own scenario row: `scenarios[].ignored[]` in the artifact (Contract.read(file, {full:true})). A scenario whose differences were all waived has the outcome `agreed`, and it is not in `divergences`. Each ignore rule is a waiver, and it carries a reason and an owner"
               + (
                  var2 > 0
                     ? ". "
                        + var2
                        + " of the rules waived nothing in this pair (`stale:true`). A stale rule waives a difference that no longer occurs. Delete it."
                     : "."
               )
      );
      return var8;
   }

   public record IgnoreRule(String b, String c, String d, String e, String f, String g) {
      static final List<String> RULE_KEYS = List.of("id", "scope", "key", "marker", "reason", "owner");


      public static io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule ruleFromMap(Object var0) {
         if (var0 instanceof Map var1) {
            LinkedHashMap var2 = new LinkedHashMap();

            for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
               String var5 = String.valueOf(var4.getKey());
               if (!RULE_KEYS.contains(var5)) {
                  var var6 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var5, RULE_KEYS);
                  throw new PairException(
                     "ignore rule: unknown key '"
                        + var5
                        + "'"
                        + (var6.isEmpty() ? "" : " — did you mean " + String.join(" / ", var6) + "?")
                        + " (known: "
                        + String.join(", ", RULE_KEYS)
                        + ")"
                  );
               }

               var2.put(var5, var4.getValue());
            }

            String var10 = b(var2.get("id"));
            String var11 = b(var2.get("scope"));
            String var12 = b(var2.get("key"));
            String var13 = b(var2.get("marker"));
            String var7 = b(var2.get("reason"));
            String var8 = b(var2.get("owner"));
            ArrayList var9 = new ArrayList();
            if (var10 == null) {
               var9.add("id");
            }

            if (var11 == null && var12 == null) {
               var9.add("scope or key");
            }

            if (var13 == null) {
               var9.add("marker");
            }

            if (var7 == null) {
               var9.add("reason");
            }

            if (var8 == null) {
               var9.add("owner");
            }

            if (!var9.isEmpty()) {
               throw new PairException(
                  "an ignore rule needs "
                     + String.join(", ", var9)
                     + " — it is a WAIVER, and an anonymous exception is not an approval record. A whole rule is {id, key|scope, marker, reason, owner}, e.g. {id:'server-minted-id', key:'id', marker:'#string', reason:'each target mints its own', owner:'@api-team'}. `key` is a field name at any depth (the portable form) or `scope` is a JSON Pointer relative to the exchange (/response/body/createdAt); `marker` is a karate match template — #string / #uuid / #number / #regex … — and BOTH values must match it for the difference to be excused, which is what stops a waiver from hiding a real change of shape."
               );
            } else if (var11 != null && !var11.startsWith("/")) {
               throw new PairException(
                  "ignore rule '" + var10 + "': `scope` is a JSON Pointer into the normalised exchange and must start with '/', e.g. /response/body/createdAt"
               );
            } else if (!var13.startsWith("#")) {
               throw new PairException("ignore rule '" + var10 + "': `marker` is a karate match template and must start with '#', e.g. #uuid / #string / #regex \\\\d+");
            } else {
               return new io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule(var10, var11, var12, var13, var7, var8);
            }
         } else {
            throw new PairException("an ignore rule must be an object of " + String.join("/", RULE_KEYS) + ", got " + (var0 == null ? "null" : var0.getClass().getSimpleName()));
         }
      }

      public boolean a(String var1) {
         if (this.d != null) {
            int var2 = var1.lastIndexOf(47);
            return this.d.equals(var2 < 0 ? var1 : var1.substring(var2 + 1));
         } else {
            return var1.equals(this.c) || var1.startsWith(this.c.endsWith("/") ? this.c : this.c + "/");
         }
      }

      public Map<String, Object> a() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("id", this.b);
         if (this.c != null) {
            var1.put("scope", this.c);
         }

         if (this.d != null) {
            var1.put("key", this.d);
         }

         var1.put("marker", this.e);
         var1.put("reason", this.f);
         var1.put("owner", this.g);
         return var1;
      }

      private static String b(Object var0) {
         String var1 = var0 == null ? null : String.valueOf(var0);
         return var1 != null && !var1.isBlank() ? var1 : null;
      }
   }
}
