package io.karatelabs.ext.sourcedoc;

import java.math.BigInteger;
import java.util.regex.Pattern;

final class YamlScalarParser {
   private static final Pattern INTEGER_PATTERN = Pattern.compile("[-+]?[0-9]+");
   private static final Pattern OCTAL_PATTERN = Pattern.compile("0o[0-7]+");
   private static final Pattern HEX_PATTERN = Pattern.compile("0x[0-9a-fA-F]+");
   private static final Pattern DECIMAL_PATTERN = Pattern.compile("[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?");
   private static final Pattern INFINITY_PATTERN = Pattern.compile("[-+]?\\.(inf|Inf|INF)");
   private static final Pattern NAN_PATTERN = Pattern.compile("\\.(nan|NaN|NAN)");

   private YamlScalarParser() {
   }

   static Object parse(String var0) {
      if (!var0.isEmpty() && !"~".equals(var0) && !"null".equals(var0) && !"Null".equals(var0) && !"NULL".equals(var0)) {
         switch (var0) {
            case "true":
            case "True":
            case "TRUE":
               return Boolean.TRUE;
            case "false":
            case "False":
            case "FALSE":
               return Boolean.FALSE;
            default:
               char var1 = var0.charAt(0);
               boolean var3 = var1 == '-' || var1 == '+' || var1 == '.' || var1 >= '0' && var1 <= '9';
               if (!var3) {
                  return var0;
               } else if (INTEGER_PATTERN.matcher(var0).matches()) {
                  return parseInteger(var0, 10);
               } else if (OCTAL_PATTERN.matcher(var0).matches()) {
                  return parseInteger(var0.substring(2), 8);
               } else if (HEX_PATTERN.matcher(var0).matches()) {
                  return parseInteger(var0.substring(2), 16);
               } else if (DECIMAL_PATTERN.matcher(var0).matches()) {
                  return Double.parseDouble(var0);
               } else if (INFINITY_PATTERN.matcher(var0).matches()) {
                  return var0.charAt(0) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
               } else {
                  return NAN_PATTERN.matcher(var0).matches() ? Double.NaN : var0;
               }
         }
      } else {
         return null;
      }
   }

   private static Object parseInteger(String var0, int var1) {
      try {
         return Integer.valueOf(var0, var1);
      } catch (NumberFormatException var5) {
         try {
            return Long.valueOf(var0, var1);
         } catch (NumberFormatException var4) {
            return new BigInteger(var0, var1);
         }
      }
   }
}
