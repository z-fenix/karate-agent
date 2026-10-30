package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.Terms;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CanonicalJson {
   private static final long a = Double.doubleToLongBits(-0.0);

   private CanonicalJson() {
   }

   public static String a(String var0) {
      return io.karatelabs.ext.common.Sha256.a(var0);
   }

   static boolean a(Object var0) {
      return var0 == Terms.UNDEFINED;
   }

   public static void b(Object var0) {
      a(var0, "$", new ArrayList<>());
   }

   private static void a(Object var0, String var1, List<Object> var2) {
      if (var0 != null && !a(var0)) {
         if (!(var0 instanceof String) && !(var0 instanceof Boolean)) {
            String var3 = e(var0);
            if (var3 != null) {
               throw new SnapshotException(var3, var1);
            } else if (var0 instanceof Number var9) {
               double var11 = var9.doubleValue();
               if (!Double.isNaN(var11) && !Double.isInfinite(var11)) {
                  if (Double.doubleToLongBits(var11) == a) {
                     throw new SnapshotException("negativeZero", var1);
                  }
               } else {
                  throw new SnapshotException("nonFinite", var1);
               }
            } else if (a(var2, var0) >= 0) {
               throw new SnapshotException("cycle", var1);
            } else {
               var2.add(var0);
               if (var0 instanceof List var4) {
                  for (int var5 = 0; var5 < var4.size(); var5++) {
                     a(var4.get(var5), var1 + "[" + var5 + "]", var2);
                  }
               } else {
                  for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (((Map)var0).entrySet())) {
                     if (!(var6.getKey() instanceof String var7)) {
                        throw new SnapshotException("hostValue", var1);
                     }

                     if ("$ct".equals(var7)) {
                        throw new SnapshotException("reservedKey", var1 + ".$ct");
                     }

                     a(var6.getValue(), var1 + "." + var7, var2);
                  }
               }

               var2.remove(var2.size() - 1);
            }
         }
      }
   }

   public static String c(Object var0) {
      StringBuilder var1 = new StringBuilder();
      return !a(var0, "$", new ArrayList<>(), var1) ? "null" : var1.toString();
   }

   private static boolean a(Object var0, String var1, List<Object> var2, StringBuilder var3) {
      if (a(var0)) {
         return false;
      } else if (var0 == null) {
         var3.append("null");
         return true;
      } else if (var0 instanceof String var13) {
         a(var13, var3);
         return true;
      } else if (var0 instanceof Boolean var12) {
         var3.append(var12 ? "true" : "false");
         return true;
      } else {
         String var4 = e(var0);
         if (var4 != null) {
            throw new SnapshotException(var4, var1);
         } else if (var0 instanceof Number var14) {
            var3.append(a(var14));
            return true;
         } else if (a(var2, var0) >= 0) {
            throw new SnapshotException("cycle", var1);
         } else {
            var2.add(var0);
            if (var0 instanceof List var5) {
               var3.append('[');

               for (int var6 = 0; var6 < var5.size(); var6++) {
                  if (var6 > 0) {
                     var3.append(',');
                  }

                  if (!a(var5.get(var6), var1 + "[" + var6 + "]", var2, var3)) {
                     var3.append("null");
                  }
               }

               var3.append(']');
            } else {
               Map<java.lang.String, Object> var15 = (Map)var0;
               ArrayList<java.lang.String> var7 = new ArrayList(var15.size());

               for (Object var9 : var15.keySet()) {
                  if (!(var9 instanceof String var10)) {
                     throw new SnapshotException("hostValue", var1);
                  }

                  if ("$ct".equals(var10)) {
                     throw new SnapshotException("reservedKey", var1 + ".$ct");
                  }

                  var7.add(var10);
               }

               var7.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
               var3.append('{');
               boolean var16 = true;
               StringBuilder var17 = new StringBuilder();

               for (String var11 : (Iterable<String>) (Iterable<?>) (var7)) {
                  var17.setLength(0);
                  if (a(var15.get(var11), var1 + "." + var11, var2, var17)) {
                     if (!var16) {
                        var3.append(',');
                     }

                     var16 = false;
                     a(var11, var3);
                     var3.append(':').append((CharSequence)var17);
                  }
               }

               var3.append('}');
            }

            var2.remove(var2.size() - 1);
            return true;
         }
      }
   }

   private static String a(Number var0) {
      double var1 = var0.doubleValue();
      if (Double.isNaN(var1)) {
         return "{\"$ct\":\"NaN\"}";
      } else if (Double.isInfinite(var1)) {
         return var1 > 0.0 ? "{\"$ct\":\"+Inf\"}" : "{\"$ct\":\"-Inf\"}";
      } else {
         return Double.doubleToLongBits(var1) == a ? "{\"$ct\":\"-0\"}" : Terms.numberToString(var0);
      }
   }

   public static Object d(Object var0) {
      if (var0 instanceof List var1 && !f(var0)) {
         ArrayList<java.lang.Object> var6 = new ArrayList(var1.size());

         for (Object var8 : var1) {
            var6.add(a(var8) ? null : d(var8));
         }

         return var6;
      } else if (var0 instanceof Map var5 && !f(var0)) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            if (!a(var4.getValue())) {
               var2.put(String.valueOf(var4.getKey()), d(var4.getValue()));
            }
         }

         return var2;
      } else {
         return var0;
      }
   }

   private static String e(Object var0) {
      if (var0 instanceof JavaCallable) {
         return "function";
      } else if (Terms.isSymbol(var0)) {
         return "symbol";
      } else if (var0 instanceof BigInteger) {
         return "bigint";
      } else if (var0 instanceof BigDecimal) {
         return "hostValue";
      } else if (var0 instanceof Number) {
         return null;
      } else if (var0 instanceof List) {
         return f(var0) ? "hostValue" : null;
      } else if (var0 instanceof Map) {
         return f(var0) ? "hostValue" : null;
      } else {
         return "hostValue";
      }
   }

   private static boolean f(Object var0) {
      return var0 instanceof ObjectLike && !Terms.isPlainObject(var0) && !Terms.isPlainArray(var0);
   }

   private static int a(List<Object> var0, Object var1) {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (var0.get(var2) == var1) {
            return var2;
         }
      }

      return -1;
   }

   public static int a(String var0, String var1) {
      int var2 = 0;
      int var3 = 0;

      while (var2 < var0.length() && var3 < var1.length()) {
         int var4 = var0.codePointAt(var2);
         int var5 = var1.codePointAt(var3);
         if (var4 != var5) {
            return Integer.compare(var4, var5);
         }

         var2 += Character.charCount(var4);
         var3 += Character.charCount(var5);
      }

      return Integer.compare(var0.length() - var2, var1.length() - var3);
   }

   private static void a(String var0, StringBuilder var1) {
      var1.append('"');

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         switch (var3) {
            case '\b':
               var1.append("\\b");
               break;
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\f':
               var1.append("\\f");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '"':
               var1.append("\\\"");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            default:
               if (var3 >= ' ' && !a(var0, var2)) {
                  var1.append(var3);
               } else {
                  var1.append(String.format("\\u%04x", Integer.valueOf(var3)));
               }
         }
      }

      var1.append('"');
   }

   private static boolean a(String var0, int var1) {
      char var2 = var0.charAt(var1);
      if (Character.isHighSurrogate(var2)) {
         return var1 + 1 >= var0.length() || !Character.isLowSurrogate(var0.charAt(var1 + 1));
      } else {
         return !Character.isLowSurrogate(var2) ? false : var1 == 0 || !Character.isHighSurrogate(var0.charAt(var1 - 1));
      }
   }
}
