package io.karatelabs.ext.rules;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class JsLiteralWriter {
   private static final int a = 2;

   private JsLiteralWriter() {
   }

   public static String a(Object var0) {
      return a(var0, 0);
   }

   public static String a(Object var0, int var1) {
      StringBuilder var2 = new StringBuilder();
      a(var2, var0, var1);
      return var2.toString();
   }

   private static void a(StringBuilder var0, Object var1, int var2) {
      if (var1 == null) {
         var0.append("null");
      } else if (var1 instanceof JsFunction || var1 instanceof JavaCallable) {
         throw new IllegalArgumentException("functions are not JS-literal data");
      } else if (var1 instanceof Boolean var7) {
         var0.append(var7);
      } else if (var1 instanceof Number var6) {
         var0.append(a(var6));
      } else if (var1 instanceof CharSequence var5) {
         a(var0, var5.toString());
      } else if (var1 instanceof Map var4) {
         a(var0, var4, var2);
      } else if (var1 instanceof List var3) {
         a(var0, var3, var2);
      } else {
         throw new IllegalArgumentException("not a JS-literal value: " + var1.getClass().getName());
      }
   }

   static String a(Number var0) {
      if (!(var0 instanceof Integer) && !(var0 instanceof Long) && !(var0 instanceof Short) && !(var0 instanceof Byte)) {
         double var1 = var0.doubleValue();
         if (Double.isNaN(var1)) {
            throw new IllegalArgumentException("NaN cannot be a JS literal");
         } else if (Double.isInfinite(var1)) {
            throw new IllegalArgumentException("Infinity cannot be a JS literal");
         } else if (var1 == Math.floor(var1) && Math.abs(var1) < 1.0E16) {
            return Long.toString((long)var1);
         } else {
            double var3 = Math.abs(var1);
            return var3 >= 1.0E-6 && var3 < 1.0E21 ? new BigDecimal(Double.toString(var1)).stripTrailingZeros().toPlainString() : Double.toString(var1);
         }
      } else {
         return var0.toString();
      }
   }

   private static void a(StringBuilder var0, String var1) {
      var0.append('\'');

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
            case '\'':
               var0.append("\\'");
               break;
            case '\\':
               var0.append("\\\\");
               break;
            default:
               var0.append(var3);
         }
      }

      var0.append('\'');
   }

   private static void a(StringBuilder var0, List<?> var1, int var2) {
      if (var1.isEmpty()) {
         var0.append("[]");
      } else {
         int var3 = var2 + 2;
         var0.append('[');

         for (Object var5 : var1) {
            var0.append('\n');
            a(var0, var3);
            a(var0, var5, var3);
            var0.append(',');
         }

         var0.append('\n');
         a(var0, var2);
         var0.append(']');
      }
   }

   private static void a(StringBuilder var0, Map<?, ?> var1, int var2) {
      if (var1.isEmpty()) {
         var0.append("{}");
      } else {
         int var3 = var2 + 2;
         var0.append('{');

         for (Entry var5 : var1.entrySet()) {
            var0.append('\n');
            a(var0, var3);
            a(var0, var5.getKey().toString());
            var0.append(": ");
            a(var0, var5.getValue(), var3);
            var0.append(',');
         }

         var0.append('\n');
         a(var0, var2);
         var0.append('}');
      }
   }

   private static void a(StringBuilder var0, int var1) {
      for (int var2 = 0; var2 < var1; var2++) {
         var0.append(' ');
      }
   }
}
