package io.karatelabs.ext.rules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class NumericBoundsParser {
   private static final BigDecimal a = new BigDecimal("9007199254740992");

   private NumericBoundsParser() {
   }

   public static Double a(Object var0, boolean var1) {
      if (var0 == null) {
         return null;
      } else {
         Double var2 = a(var0);
         if (var2 == null) {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notNumber", var0);
         } else if (!var2.isNaN() && !var2.isInfinite()) {
            if (var1 && var2 != Math.rint(var2)) {
               throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notWhole", var2);
            } else {
               return var2;
            }
         } else {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notFinite", var2);
         }
      }
   }

   public static int a(Object var0, int var1) {
      if (var0 == null) {
         return var1;
      } else {
         Double var2 = a(var0);
         if (var2 != null && !var2.isNaN() && !var2.isInfinite()) {
            if (var2 != Math.rint(var2)) {
               throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notWhole", var2);
            } else if (var2 < 0.0) {
               throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("negative", var2);
            } else {
               return (int)var2.doubleValue();
            }
         } else {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notFinite", var0);
         }
      }
   }

   public static List<Object> a(List<Object> var0) {
      if (var0 != null && !var0.isEmpty()) {
         return new ArrayList<>(var0);
      } else {
         throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("empty", null);
      }
   }

   private static Double a(Object var0) {
      return var0 instanceof Number var1 ? var1.doubleValue() : null;
   }

   public static io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds a(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0.startsWith("##") ? var0.substring(1) : var0;
         boolean var2 = var1.startsWith("#int[");
         if (!var2 && !var1.startsWith("#number[")) {
            return null;
         } else {
            int var3 = var1.indexOf(91);
            if (!var1.endsWith("]")) {
               throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("malformed", var0);
            } else {
               String[] var4 = var1.substring(var3 + 1, var1.length() - 1).split(",", -1);
               if (var4.length != 2) {
                  throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("malformed", var0);
               } else {
                  Double var5 = a(var0, var4[0], var2);
                  Double var6 = a(var0, var4[1], var2);
                  if (var5 == null && var6 == null && !var2) {
                     throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("empty", var0);
                  } else if (var5 != null && var6 != null && var5 > var6) {
                     throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("loAboveHi", var0);
                  } else {
                     return new io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds(var5, var6, var2);
                  }
               }
            }
         }
      }
   }

   private static Double a(String var0, String var1, boolean var2) {
      String var3 = var1.trim();
      if (var3.isEmpty()) {
         return null;
      } else if (var2) {
         BigDecimal var9;
         try {
            var9 = new BigDecimal(var3);
         } catch (NumberFormatException var7) {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notNumber", var0);
         }

         if (var9.stripTrailingZeros().scale() > 0) {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notWhole", var0);
         } else if (var9.abs().compareTo(a) > 0) {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notExact", var0);
         } else {
            return var9.doubleValue();
         }
      } else {
         double var4;
         try {
            var4 = Double.parseDouble(var3);
         } catch (NumberFormatException var8) {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notNumber", var0);
         }

         if (!Double.isNaN(var4) && !Double.isInfinite(var4)) {
            return var4;
         } else {
            throw new io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException("notFinite", var0);
         }
      }
   }

   public static final class NumericBoundsException extends RuntimeException {
      public final String a;
      public final Object b;

      NumericBoundsException(String var1, Object var2) {
         super(var1);
         this.a = var1;
         this.b = var2;
      }
   }

   public record NumericBounds(Double a, Double b, boolean c) {

      public NumericBounds(Double a, Double b, boolean c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
}
