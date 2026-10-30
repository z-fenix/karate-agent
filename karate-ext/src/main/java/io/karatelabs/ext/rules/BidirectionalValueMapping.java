package io.karatelabs.ext.rules;

import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

final class BidirectionalValueMapping {
   private final UnaryOperator<String> a;
   private final Map<String, Map<String, String>> b = new LinkedHashMap<>();
   private final Map<String, Map<String, String>> c = new LinkedHashMap<>();

   BidirectionalValueMapping(Map<String, Object> var1, UnaryOperator<String> var2) {
      this.a = var2;

      for (Entry var4 : var1.entrySet()) {
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
         LinkedHashMap<java.lang.String, java.lang.String> var6 = new LinkedHashMap();
         if (!((var4.getValue() == null ? Map.of() : var4.getValue()) instanceof Map var8)) {
            throw new RuleApiException(400, "MAPPING_INVALID", "mapping: values." + (String)var4.getKey() + " is an object");
         }

         for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
            String var11 = String.valueOf(var10.getKey());
            String var12 = a(var10.getValue());
            String var13 = var6.put(var12, var11);
            if (var13 != null) {
               throw new RuleApiException(
                  400, "MAPPING_NOT_INJECTIVE", "values." + (String)var4.getKey() + ": '" + var13 + "' and '" + var11 + "' both map to '" + var12 + "'"
               );
            }

            var5.put(var11, var12);
         }

         String var14 = (String)var2.apply((String)var4.getKey());
         this.b.put(var14, var5);
         this.c.put(var14, var6);
      }
   }

   Set<String> a() {
      return this.b.keySet();
   }

   boolean a(String var1) {
      return this.b.containsKey(this.a.apply(var1));
   }

   Collection<String> b(String var1) {
      return this.b.get(var1).keySet();
   }

   Object a(String var1, Object var2) {
      var var3 = this.b.get(this.a.apply(var1));
      if (var3 == null) {
         return var2;
      } else {
         String var4 = (String)var3.get(a(var2));
         if (var4 == null) {
            throw this.a(var1, var2, "ours", var3.keySet());
         } else {
            return var4;
         }
      }
   }

   String b(String var1, Object var2) {
      var var3 = this.c.get(this.a.apply(var1));
      if (var3 == null) {
         return a(var2);
      } else {
         String var4 = (String)var3.get(a(var2));
         if (var4 == null) {
            throw this.a(var1, var2, "the carrier's", var3.keySet());
         } else {
            return var4;
         }
      }
   }

   private RuleApiException a(String var1, Object var2, String var3, Collection<String> var4) {
      return new RuleApiException(400, "MAPPING_VALUE_UNMAPPED", "values." + this.a.apply(var1) + ": " + var3 + " value '" + a(var2) + "' is outside the map " + var4);
   }

   static Map<String, List<String>> a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         if (!((var3.getValue() == null ? Map.of() : var3.getValue()) instanceof Map var5)) {
            throw new RuleApiException(400, "MAPPING_INVALID", "mapping: format." + (String)var3.getKey() + " is an object");
         }

         if (!(var5.get("strip") instanceof List var6 && !var6.isEmpty())) {
            throw new RuleApiException(
               400, "MAPPING_FORMAT", "format." + (String)var3.getKey() + ".strip is a non-empty list of the separators or currency symbols to remove"
            );
         }

         ArrayList<java.lang.String> var11 = new ArrayList();

         for (Object var9 : var6) {
            if (var9 instanceof String var10 && !var10.isEmpty()) {
               if (!io.karatelabs.ext.rules.EvidenceMapping.b.matcher(var10).lookingAt() && !var10.equals("-") && !var10.equals(".")) {
                  var11.add(var10);
                  continue;
               }

               throw new RuleApiException(
                  400,
                  "MAPPING_FORMAT",
                  "format." + (String)var3.getKey() + ".strip cannot strip '" + var10 + "' — a digit, sign or decimal point is part of the figure"
               );
            }

            throw new RuleApiException(400, "MAPPING_INVALID", "mapping: format." + (String)var3.getKey() + ".strip[] is a non-empty string");
         }

         var1.put((String)var3.getKey(), List.copyOf(var11));
      }

      return var1;
   }

   static String a(String var0, List<String> var1) {
      String var2 = var0.strip();

      for (String var4 : var1) {
         var2 = var2.replace(var4, "");
      }

      return var2;
   }

   static String a(Object var0) {
      if (var0 == null) {
         return "null";
      } else if (var0 instanceof String var3) {
         return var3;
      } else if (var0 instanceof Number var2) {
         return Terms.numberToString(var2);
      } else {
         return var0 instanceof Boolean var1 ? var1.toString() : io.karatelabs.ext.rules.twin.CanonicalJson.c(var0);
      }
   }
}
