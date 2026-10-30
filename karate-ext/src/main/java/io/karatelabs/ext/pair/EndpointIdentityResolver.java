package io.karatelabs.ext.pair;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EndpointIdentityResolver {
   private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]");
   private static final Pattern URL_ORIGIN_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*)://(?:[^@/?#]*@)?(\\[[^\\]]+\\]|[^/:?#]+)(?::(\\d+))?");

   private EndpointIdentityResolver() {
   }

   static String a(PairRunEvidence var0, String var1, String var2) {
      return a(var0, var1, var2, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static String a(PairRunEvidence var0, String var1, String var2, EndpointAliasSet var3) {
      return a(var0, var1, var2, var3, null);
   }

   static String a(PairRunEvidence var0, String var1, String var2, EndpointAliasSet var3, String var4) {
      var var5 = c(var0);
      if (!var5.isEmpty() && !b(var5, var1, var3)) {
         boolean var6 = a(var5, var1, var3);
         boolean var7 = var4 != null && !c(var1, var4) && b(var5, var4, var3);
         return "the "
            + var0.b()
            + " leg never addressed the target it was handed ("
            + var1
            + "): of "
            + var5.size()
            + " recorded HTTP exchange(s), not one went to that host and port — they went to "
            + b(var5)
            + ". The suite is therefore NOT target-swappable, and this run measured something other than the pair: a suite that keeps its own base URL (or falls back to auto-starting its own mock) runs BOTH legs against the same place, agrees with itself, and would mint a perfect claim over a provider that was never contacted. Make the target a variable the harness sets — read it from karate.properties['"
            + var2
            + "'] — or, if your config already reads a property of its own, pass that name as targetProp:'<yourProperty>'."
            + (
               var6
                  ? " And do NOT reach for `aliases` here: what answered this leg was A KARATE MOCK — it disclosed itself on the wire — so it is not your target under a second name, it is a stand-in. Declaring the two addresses equivalent would sign a waiver saying our mock is the provider, which is this failure with a signature on it rather than a fix for it. (`aliases` is for one target reached under a second name — a container's host.docker.internal, a gateway, a DNS alias.) The fix here is `targetProp` above: the suite is falling back to a mock of its own because it never read the property."
                  : (
                     var7
                        ? " And do NOT reach for `aliases` here: this leg addressed the OTHER leg's target in this same pair ("
                           + var4
                           + "). Nothing here says what answered — no response disclosed itself — but the two addresses are the two SIDES of the comparison, so declaring them equivalent would sign a waiver saying the pair compared one system with itself. (`aliases` is for one target reached under a second name — a container's host.docker.internal, a gateway, a DNS alias.) The fix here is `targetProp` above."
                        : " If those addresses ARE the target under another name — a container reaching the host as host.docker.internal, a gateway or tunnel in front of the provider, a DNS alias for an IP literal — declare the equivalence instead of renaming anything: aliases:[{hosts:['"
                           + a(var5)
                           + "','"
                           + var1
                           + "'], reason:'…', owner:'…'}]. That is a signed waiver on the record, not a bar stood down: the leg must still have addressed its target, and this says which addresses are it."
                  )
            )
            + (var3.isEmpty() ? "" : " (Declared already: " + var3.memberHosts() + " — none of them matched, so they are not the difference here.)");
      } else {
         return null;
      }
   }

   private static boolean a(List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var0, String var1, EndpointAliasSet var2) {
      String var3 = a(var1, var2);

      for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var5 : var0) {
         if (var3 == null || !var3.equals(a(var5.d(), var2))) {
            Object var6 = var5.g() == null ? null : var5.g().get("karate-mock");
            if (var6 != null && !"false".equalsIgnoreCase(String.valueOf(var6))) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean c(String var0, String var1) {
      String var2 = d(var0);
      String var3 = d(var1);
      return var2 != null && var3 != null ? var2.equals(var3) : c(var0) != null && c(var0).equals(c(var1));
   }

   static String a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, String var1, String var2) {
      return a(var0, var1, var2, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static String a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, String var1, String var2, EndpointAliasSet var3) {
      var var4 = var0.m();
      return !var4.isEmpty() && !b(var4, var1, var3)
         ? "the "
            + var2
            + " leg's run of this scenario made "
            + var4.size()
            + " HTTP exchange(s) and none reached the target it was handed ("
            + var1
            + ") — it addressed "
            + b(var4)
            + " instead. A scenario that did not answer to the pair's target was never comparable, whatever verdict it reported. If that address IS the target under another name, declare it — aliases:[{hosts:[…], reason, owner}] — rather than rewriting the suite."
         : null;
   }

   static String validateTarget(String var0, String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.trim();
         return var2.matches("(?i)^[a-z][a-z0-9+.-]*://.*")
            ? null
            : "`"
               + var0
               + "` = '"
               + var1
               + "' is not a usable target: it needs an ABSOLUTE base URL with a scheme (http://host:port or https://host), because that is what a leg's exchanges are checked against. Without one nothing can address it, every call fails inside the HTTP client, and those failures would read as divergences — a fidelity finding manufactured out of a wiring mistake. Did you mean 'http://"
               + var2
               + "'?";
      } else {
         return null;
      }
   }

   static String b(String var0, String var1) {
      return validateDistinctTargets(var0, var1, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static String validateDistinctTargets(String var0, String var1, EndpointAliasSet var2) {
      String var3 = a(var0, var2);
      String var4 = var3 != null && var3.equals(a(var1, var2)) ? var3 : (c(var0) != null && c(var0).equals(c(var1)) ? var0.trim() : null);
      return var4 == null
         ? null
         : "the provider and the mock are the same target ("
            + var4
            + "), so this pair would compare one system against itself and agree about everything. A pair needs two targets: `provider` is the real provider's base URL, and the mock leg's target is the mock — stood up from `spec` when you omit `mock`, or whatever you named there.";
   }

   private static String c(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim().toLowerCase();

         while (var1.endsWith("/")) {
            var1 = var1.substring(0, var1.length() - 1);
         }

         return var1.isEmpty() ? null : var1;
      } else {
         return null;
      }
   }

   static Map<String, Object> a(PairRunEvidence var0) {
      return a(var0, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static Map<String, Object> a(PairRunEvidence var0, EndpointAliasSet var1) {
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;

      for (io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var7 : var0.e()) {
         if (!var7.m().isEmpty()) {
            var2++;
            if (b(var7.m(), var0.c(), var1)) {
               var3++;
            }

            var4 += var7.m().size();

            for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var9 : var7.m()) {
               if (b(List.of(var9), var0.c(), var1)) {
                  var5++;
               }
            }
         }
      }

      LinkedHashMap var10 = new LinkedHashMap();
      var10.put("reachedTarget", var3);
      var10.put("ofScenariosMakingCalls", var2);
      var10.put("exchangesOnTarget", var5);
      var10.put("ofExchanges", var4);
      boolean var11 = a(var0.c(), var1) != null;
      var10.put(
         "note",
         var2 == 0
            ? "NOT CHECKED — this leg recorded no HTTP exchange at all, so there was nothing to judge adherence by. Either the suite makes no calls, or the run's exchange stream is missing; read this as unverified rather than as agreement."
            : (
               !var11
                  ? "NOT CHECKED — this leg's target ("
                     + var0.c()
                     + ") has no host and port we could parse out of it, so no exchange could be compared against it and every scenario counted as adherent by default. The counts above are what the run recorded, NOT a verified match. Hand this leg a plain absolute base URL (http://host:port) to have the question actually asked."
                  : (
                     var3 == var2
                        ? "every scenario that made a call addressed this leg's own target — checked off the exchanges the run recorded, not assumed. A suite that kept a base URL of its own would run both legs against the same place and agree with itself."
                           + (
                              var5 == var4
                                 ? " Every one of the " + var4 + " exchanges went there."
                                 : " BUT ONLY "
                                    + var5
                                    + " of "
                                    + var4
                                    + " exchanges did: the other "
                                    + (var4 - var5)
                                    + " went somewhere else, and whatever they compared, they compared against the same place on both legs. Adherence is judged per SCENARIO, so this does not change the verdict — read it before quoting the claim."
                           )
                        : var2
                           - var3
                           + " scenario(s) made calls but never reached this leg's target, so they were never comparable and are `notmeasured` (cause `targetNotAddressed`)."
                  )
            )
      );
      return var10;
   }

   static boolean b(PairRunEvidence var0) {
      return b(var0, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static boolean b(PairRunEvidence var0, EndpointAliasSet var1) {
      String var2 = a(var0.c(), var1);

      for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var4 : c(var0)) {
         if (var2 == null || var2.equals(a(var4.d(), var1))) {
            Object var5 = var4.g() == null ? null : var4.g().get("karate-mock");
            if (var5 != null && !"false".equalsIgnoreCase(String.valueOf(var5))) {
               return true;
            }
         }
      }

      return false;
   }

   static List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> c(PairRunEvidence var0) {
      return var0.e().stream().flatMap(var0x -> var0x.m().stream()).toList();
   }

   private static boolean b(List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var0, String var1, EndpointAliasSet var2) {
      String var3 = a(var1, var2);
      if (var3 == null) {
         return true;
      } else {
         for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var5 : var0) {
            if (var3.equals(a(var5.d(), var2))) {
               return true;
            }
         }

         return false;
      }
   }

   private static String a(List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var0) {
      for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var2 : var0) {
         String var3 = d(var2.d());
         if (var3 != null) {
            return var3;
         }
      }

      return "<the address it used>";
   }

   private static String b(List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var3 : var0) {
         String var4 = d(var3.d());
         var1.add(var4 == null ? var3.d() : var4);
      }

      return String.join(", ", var1);
   }

   private static String d(String var0) {
      return a(var0, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static String normalizeOrigin(String var0) {
      return a(var0, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   static String normalizeHost(String var0) {
      return var0 != null && LOCAL_HOSTS.contains(var0) ? "localhost" : var0;
   }

   private static String a(String var0, EndpointAliasSet var1) {
      if (var0 != null && !var0.isBlank()) {
         String var2 = null;
         String var3 = null;
         int var4 = -1;

         try {
            URI var5 = URI.create(var0.trim());
            var2 = var5.getScheme();
            var3 = var5.getHost();
            var4 = var5.getPort();
         } catch (RuntimeException var9) {
         }

         if (var3 == null) {
            Matcher var10 = URL_ORIGIN_PATTERN.matcher(var0.trim());
            if (!var10.find()) {
               return null;
            }

            var2 = var10.group(1);
            var3 = var10.group(2);
            var4 = var10.group(3) == null ? -1 : Integer.parseInt(var10.group(3));
         }

         if (var2 != null && !var3.isBlank()) {
            int var11 = var4 != -1 ? var4 : ("https".equalsIgnoreCase(var2) ? 443 : 80);
            String var6 = var3.toLowerCase();
            String var7 = normalizeHost(var6);
            String var8 = var2.toLowerCase() + "://" + var7 + ":" + var11;
            return var1.isEmpty() ? var8 : var1.canonicalize(var8, var2.toLowerCase(), var7, var11);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
