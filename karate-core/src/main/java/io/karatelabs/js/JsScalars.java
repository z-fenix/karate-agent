package io.karatelabs.js;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class JsScalars {
   private JsScalars() {
   }

   public static double round(double x) {
      if (Double.isNaN(x) || Double.isInfinite(x) || x == 0.0) {
         return x;
      } else if (x == Math.floor(x)) {
         return x;
      } else if (x > 0.0 && x < 0.5) {
         return 0.0;
      } else {
         return x < 0.0 && x >= -0.5 ? -0.0 : Math.floor(x + 0.5);
      }
   }

   public static String toFixed(double value, int digits) {
      if (digits < 0 || digits > 100) {
         throw JsErrorException.rangeError("toFixed() digits argument must be between 0 and 100");
      } else if (Double.isNaN(value)) {
         return "NaN";
      } else if (value == Double.POSITIVE_INFINITY) {
         return "Infinity";
      } else if (value == Double.NEGATIVE_INFINITY) {
         return "-Infinity";
      } else if (Math.abs(value) >= 1.0E21) {
         return Terms.numberToString(value);
      } else {
         BigDecimal bd = BigDecimal.valueOf(value);
         bd = bd.setScale(digits, RoundingMode.HALF_UP);
         StringBuilder pattern = new StringBuilder("0");
         if (digits > 0) {
            pattern.append(".");
            pattern.append("0".repeat(digits));
         }

         DecimalFormat df = new DecimalFormat(pattern.toString());
         return df.format(bd.doubleValue());
      }
   }
}
