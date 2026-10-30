package io.karatelabs.ext.common;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class CanonicalJson {
   private CanonicalJson() {
   }

   public static String a(Object var0) {
      StringBuilder var1 = new StringBuilder();
      a(var1, var0);
      return var1.toString();
   }

   private static void a(StringBuilder var0, Object var1) {
      switch (var1) {
         case null:
            var0.append("null");
            break;
         case Map var4:
            TreeMap<java.lang.String, java.lang.Object> var9 = new TreeMap();
            var4.forEach((var1x, var2) -> var9.put(String.valueOf(var1x), var2));
            var0.append('{');
            boolean var11 = true;

            for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
               if (!var11) {
                  var0.append(',');
               }

               var11 = false;
               a(var0, (String)var14.getKey());
               var0.append(':');
               a(var0, var14.getValue());
            }

            var0.append('}');
            break;
         case Collection var5:
            var0.append('[');
            boolean var10 = true;

            for (Object var8 : var5) {
               if (!var10) {
                  var0.append(',');
               }

               var10 = false;
               a(var0, var8);
            }

            var0.append(']');
            break;
         case Boolean var6:
            var0.append(var6.toString());
            break;
         case Number var7:
            var0.append(a(var7));
            break;
         default:
            a(var0, String.valueOf(var1));
      }
   }

   private static String a(Number var0) {
      try {
         return new BigDecimal(var0.toString()).stripTrailingZeros().toPlainString();
      } catch (NumberFormatException var2) {
         return var0.toString();
      }
   }

   private static void a(StringBuilder var0, String var1) {
      var0.append('"');

      for (int var2 = 0; var2 < var1.length(); var2++) {
         char var3 = var1.charAt(var2);
         switch (var3) {
            case '\t':
               var0.append("\\t");
               break;
            case '\n':
               var0.append("\\n");
               break;
            case '\r':
               var0.append("\\r");
               break;
            case '"':
               var0.append("\\\"");
               break;
            case '\\':
               var0.append("\\\\");
               break;
            default:
               if (var3 < ' ') {
                  var0.append(String.format("\\u%04x", Integer.valueOf(var3)));
               } else {
                  var0.append(var3);
               }
         }
      }

      var0.append('"');
   }
}
