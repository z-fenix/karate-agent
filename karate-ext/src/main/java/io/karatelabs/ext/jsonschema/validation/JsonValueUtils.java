package io.karatelabs.ext.jsonschema.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class JsonValueUtils {
   private JsonValueUtils() {
   }

   static String jsonTypeName(Object var0) {
      if (var0 == null) {
         return "null";
      } else if (var0 instanceof Boolean) {
         return "boolean";
      } else if (var0 instanceof Map) {
         return "object";
      } else if (var0 instanceof List) {
         return "array";
      } else {
         return var0 instanceof Number ? "number" : "string";
      }
   }

   static boolean isNumber(Object var0) {
      return var0 instanceof Number;
   }

   static boolean isInteger(Object var0) {
      if (!(var0 instanceof Number)) {
         return false;
      } else {
         BigDecimal var1 = toBigDecimal(var0);
         return var1 != null && var1.stripTrailingZeros().scale() <= 0;
      }
   }

   static BigDecimal toBigDecimal(Object var0) {
      if (var0 instanceof BigDecimal var5) {
         return var5;
      } else if (var0 instanceof BigInteger var4) {
         return new BigDecimal(var4);
      } else if (var0 instanceof Double || var0 instanceof Float) {
         double var3 = ((Number)var0).doubleValue();
         return !Double.isNaN(var3) && !Double.isInfinite(var3) ? BigDecimal.valueOf(var3) : null;
      } else {
         return var0 instanceof Number var1 ? BigDecimal.valueOf(var1.longValue()) : null;
      }
   }

   static boolean jsonEquals(Object var0, Object var1) {
      if (var0 != null && var1 != null) {
         if (var0 instanceof Number && var1 instanceof Number) {
            BigDecimal var7 = toBigDecimal(var0);
            BigDecimal var9 = toBigDecimal(var1);
            return var7 != null && var9 != null && var7.compareTo(var9) == 0;
         } else if (!(var0 instanceof Boolean) && !(var1 instanceof Boolean)) {
            if (var0 instanceof Map var2 && var1 instanceof Map var8) {
               if (var2.size() != var8.size()) {
                  return false;
               } else {
                  for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
                     if (!var8.containsKey(var5.getKey()) || !jsonEquals(var5.getValue(), var8.get(var5.getKey()))) {
                        return false;
                     }
                  }

                  return true;
               }
            } else if (!(var0 instanceof List var6 && var1 instanceof List var3)) {
               return !(var0 instanceof Map) && !(var1 instanceof Map) && !(var0 instanceof List) && !(var1 instanceof List) ? var0.equals(var1) : false;
            } else if (var6.size() != var3.size()) {
               return false;
            } else {
               for (int var4 = 0; var4 < var6.size(); var4++) {
                  if (!jsonEquals(var6.get(var4), var3.get(var4))) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return var0.equals(var1);
         }
      } else {
         return var0 == null && var1 == null;
      }
   }

   static int codePointLength(String var0) {
      return var0.codePointCount(0, var0.length());
   }

   static Object describeValue(Object var0) {
      if (var0 instanceof Map var2) {
         return "{" + var2.size() + " keys}";
      } else {
         return var0 instanceof List var1 ? "[" + var1.size() + " items]" : var0;
      }
   }
}
