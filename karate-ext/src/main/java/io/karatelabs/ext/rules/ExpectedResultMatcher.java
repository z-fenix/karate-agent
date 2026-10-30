package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.util.Map;
import java.util.Map.Entry;

public final class ExpectedResultMatcher {
   private ExpectedResultMatcher() {
   }

   public static Map<?, ?> a(Map<String, Object> var0) {
      return var0 != null && var0.get("_expect") instanceof Map var1 ? var1 : null;
   }

   public static String a(Map<?, ?> var0, boolean var1, Object var2, Object var3, boolean var4) {
      if (var1) {
         return "the run throws where an expectation is stored";
      } else {
         for (Entry var6 : var0.entrySet()) {
            String var7 = String.valueOf(var6.getKey());
            if ("output".equals(var7)) {
               String var8 = a(var6.getValue(), var2, "output");
               if (var8 != null) {
                  return var8;
               }
            } else {
               if (!"outcome".equals(var7)) {
                  return "_expect key '" + var7 + "' is not part of the envelope — use {\"output\": {…subset}} and/or {\"outcome\": v}";
               }

               Object var9 = var4 ? var3 : null;
               if (!Json.stringifyStrict(var6.getValue()).equals(Json.stringifyStrict(var9))) {
                  return "outcome: expected " + Json.stringifyStrict(var6.getValue()) + " but was " + Json.stringifyStrict(var9);
               }
            }
         }

         return null;
      }
   }

   private static String a(Object var0, Object var1, String var2) {
      if (var0 instanceof Map var3) {
         if (var1 instanceof Map var4) {
            for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
               String var7 = String.valueOf(var6.getKey());
               if (!var4.containsKey(var7)) {
                  return var2 + "." + var7 + ": missing";
               }

               String var8 = a(var6.getValue(), var4.get(var7), var2 + "." + var7);
               if (var8 != null) {
                  return var8;
               }
            }

            return null;
         } else {
            return var2 + ": expected an object but was " + Json.stringifyStrict(var1);
         }
      } else {
         return !Json.stringifyStrict(var0).equals(Json.stringifyStrict(var1))
            ? var2 + ": expected " + Json.stringifyStrict(var0) + " but was " + Json.stringifyStrict(var1)
            : null;
      }
   }
}
