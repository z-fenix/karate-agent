package io.karatelabs.ext.pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record EndpointAliasSet(List<io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias> d) {
   public static final io.karatelabs.ext.pair.EndpointAliasSet EMPTY = new io.karatelabs.ext.pair.EndpointAliasSet(List.of());


   public static io.karatelabs.ext.pair.EndpointAliasSet parse(Object var0) {
      if (var0 == null) {
         return EMPTY;
      } else {
         List var1 = var0 instanceof List var2 ? var2 : List.of(var0);
         ArrayList<io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias> var14 = new ArrayList();

         for (Object var4 : var1) {
            if (!(var4 instanceof Map var5)) {
               throw new PairException("`aliases` takes objects of {hosts:[…], reason, owner} — got: " + var4);
            }

            ArrayList<java.lang.String> var6 = new ArrayList();
            Object var7 = var5.get("hosts") != null ? var5.get("hosts") : var5.get("origins");
            if (var7 instanceof List) {
               for (Object var10 : (List)var7) {
                  if (var10 != null && !String.valueOf(var10).isBlank()) {
                     var6.add(String.valueOf(var10).trim());
                  }
               }
            }

            if (var6.size() < 2) {
               throw new PairException(
                  "an alias needs at least TWO spellings to declare equal, e.g. {hosts:['host.docker.internal','localhost'], reason:'…', owner:'…'} — got: "
                     + var4
               );
            }

            String var15 = stringOrNull(var5.get("reason"));
            String var16 = stringOrNull(var5.get("owner"));
            if (var15 == null || var16 == null) {
               throw new PairException(
                  "the alias "
                     + var6
                     + " needs a `reason` and an `owner`: declaring two addresses equal is a WAIVER (an ignored infrastructure difference), and a waiver nobody signed is an unattributed exception in a governance artifact."
               );
            }

            long var17 = var6.stream().filter(io.karatelabs.ext.pair.EndpointAliasSet::isOrigin).count();
            if (var17 != 0L && var17 != var6.size()) {
               throw new PairException(
                  "the alias "
                     + var6
                     + " mixes bare hosts with full origins, which does not say which port the origin corresponds to. Declare either all bare hosts (the same host at any port) or all full origins (scheme://host:port exactly)."
               );
            }

            for (String var13 : (Iterable<String>) (Iterable<?>) (var6)) {
               if (isOrigin(var13) && io.karatelabs.ext.pair.EndpointIdentityResolver.normalizeOrigin(var13) == null) {
                  throw new PairException(
                     "the alias member '"
                        + var13
                        + "' has a scheme but is not a usable origin — write it as scheme://host[:port] (e.g. 'https://gw.example.com'), or as a bare host to match that host at any port. A member that can never match would leave the refusal it was written to remove, and read as the harness ignoring your declaration."
                  );
               }
            }

            var14.add(new io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias(List.copyOf(var6), var15, var16));
         }

         return new io.karatelabs.ext.pair.EndpointAliasSet(List.copyOf(var14));
      }
   }

   public boolean isEmpty() {
      return this.d.isEmpty();
   }

   String canonicalize(String var1, String var2, String var3, int var4) {
      for (io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias var6 : this.d) {
         for (String var8 : var6.a()) {
            if (isOrigin(var8) && normalizeAlias(var8).equals(var1)) {
               return canonicalAliasEndpoint(var6, var2, var4);
            }
         }
      }

      for (io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias var10 : this.d) {
         for (String var12 : var10.a()) {
            if (!isOrigin(var12) && normalizeAlias(var12).equals(var3)) {
               return canonicalAliasEndpoint(var10, var2, var4);
            }
         }
      }

      return var1;
   }

   private static String canonicalAliasEndpoint(io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias var0, String var1, int var2) {
      String var3 = var0.a().get(0);
      return isOrigin(var3) ? normalizeAlias(var3) : var1 + "://" + normalizeAlias(var3) + ":" + var2;
   }

   static boolean isOrigin(String var0) {
      return var0.contains("://");
   }

   static String normalizeAlias(String var0) {
      if (isOrigin(var0)) {
         String var1 = io.karatelabs.ext.pair.EndpointIdentityResolver.normalizeOrigin(var0);
         return var1 != null ? var1 : var0.toLowerCase(Locale.ROOT);
      } else {
         return io.karatelabs.ext.pair.EndpointIdentityResolver.normalizeHost(var0.toLowerCase(Locale.ROOT));
      }
   }

   public List<Map<String, Object>> aliasesAsMaps() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias var3 : this.d) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("hosts", var3.a());
         var4.put("reason", var3.b());
         var4.put("owner", var3.c());
         var1.add(var4);
      }

      return var1;
   }

   Set<String> memberHosts() {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias var3 : this.d) {
         var1.addAll(var3.a());
      }

      return var1;
   }

   private static String stringOrNull(Object var0) {
      return var0 != null && !String.valueOf(var0).isBlank() ? String.valueOf(var0).trim() : null;
   }

   public List<io.karatelabs.ext.pair.EndpointAliasSet.EndpointAlias> d() {
      return this.d;
   }

   public record EndpointAlias(List<String> a, String b, String c) {

   }
}
