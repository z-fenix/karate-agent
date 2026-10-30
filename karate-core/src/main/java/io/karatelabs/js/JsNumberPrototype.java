package io.karatelabs.js;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;

class JsNumberPrototype extends Prototype {
   static final JsNumberPrototype INSTANCE = new JsNumberPrototype();

   private JsNumberPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("toFixed", 1, this::toFixed);
      this.install("toPrecision", 1, this::toPrecision);
      this.install("toExponential", 1, this::toExponential);
      this.install("toLocaleString", 0, this::toLocaleString);
      this.install("toString", 1, this::toStringMethod);
      this.install("valueOf", 0, this::valueOf);
   }

   private Object toStringMethod(Context context, Object[] args) {
      Number n = thisNumber(context);
      if (args.length > 0 && args[0] != Terms.UNDEFINED) {
         if (args[0] instanceof BigInteger) {
            throw JsErrorException.typeError("Cannot convert a BigInt to a number");
         }

         int radix = Terms.objectToNumber(args[0]).intValue();
         if (radix < 2 || radix > 36) {
            throw JsErrorException.rangeError("toString() radix must be between 2 and 36");
         }

         if (radix != 10) {
            double d = n.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
               return Long.toString((long)d, radix);
            }

            return Double.toString(d);
         }
      }

      return Terms.numberToString(n);
   }

   private static Number thisNumber(Context context) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsNumber jn) {
         return jn.value;
      } else if (thisObj instanceof Number n) {
         return n;
      } else if (thisObj == INSTANCE) {
         return 0;
      } else {
         throw JsErrorException.typeError("Number.prototype method called on incompatible receiver");
      }
   }

   private static int toIntegerArg(Object arg, Context context) {
      Number n = context instanceof CoreContext cc ? Terms.toNumberCoerce(arg, cc) : Terms.objectToNumber(arg);
      double d = n.doubleValue();
      return Double.isNaN(d) ? 0 : (int)d;
   }

   private Object toFixed(Context context, Object[] args) {
      double d = thisNumber(context).doubleValue();
      if (args.length > 0 && args[0] instanceof BigInteger) {
         throw JsErrorException.typeError("Cannot convert a BigInt to a number");
      } else {
         int digits = 0;
         if (args.length > 0 && args[0] != Terms.UNDEFINED) {
            digits = toIntegerArg(args[0], context);
         }

         return JsScalars.toFixed(d, digits);
      }
   }

   private Object toPrecision(Context context, Object[] args) {
      double d = thisNumber(context).doubleValue();
      if (args.length == 0 || args[0] == Terms.UNDEFINED) {
         return Terms.numberToString(d);
      } else if (args[0] instanceof BigInteger) {
         throw JsErrorException.typeError("Cannot convert a BigInt to a number");
      } else {
         int precision = toIntegerArg(args[0], context);
         if (Double.isNaN(d)) {
            return "NaN";
         } else if (d == Double.POSITIVE_INFINITY) {
            return "Infinity";
         } else if (d == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
         } else if (precision < 1 || precision > 100) {
            throw JsErrorException.rangeError("toPrecision() precision must be between 1 and 100");
         } else if (d != 0.0) {
            BigDecimal bd = new BigDecimal(d);
            bd = bd.round(new MathContext(precision, RoundingMode.HALF_UP));
            String result = bd.toString();
            if (result.contains("E") || result.contains("e")) {
               return result.replace('E', 'e');
            } else {
               return !(Math.abs(d) < 1.0E-6) && !(Math.abs(d) >= Math.pow(10.0, precision)) ? result : formatPrecision(d, precision);
            }
         } else if (precision == 1) {
            return "0";
         } else {
            StringBuilder sb = new StringBuilder(precision + 2);
            sb.append("0.");

            for (int i = 1; i < precision; i++) {
               sb.append('0');
            }

            return sb.toString();
         }
      }
   }

   private Object toExponential(Context context, Object[] args) {
      double d = thisNumber(context).doubleValue();
      if (args.length > 0 && args[0] instanceof BigInteger) {
         throw JsErrorException.typeError("Cannot convert a BigInt to a number");
      } else {
         boolean digitsAbsent = args.length == 0 || args[0] == Terms.UNDEFINED;
         int digits = digitsAbsent ? 0 : toIntegerArg(args[0], context);
         if (Double.isNaN(d)) {
            return "NaN";
         } else if (d == Double.POSITIVE_INFINITY) {
            return "Infinity";
         } else if (d == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
         } else if (d == 0.0) {
            if (!digitsAbsent && digits != 0) {
               StringBuilder sb = new StringBuilder(digits + 5);
               sb.append("0.");

               for (int i = 0; i < digits; i++) {
                  sb.append('0');
               }

               sb.append("e+0");
               return sb.toString();
            } else {
               return "0e+0";
            }
         } else if (digitsAbsent || digits >= 0 && digits <= 100) {
            String formatted;
            if (digitsAbsent) {
               formatted = String.format(Locale.ROOT, "%.15e", d);
               int e = formatted.indexOf(101);
               String mant = formatted.substring(0, e);
               String exp = formatted.substring(e);
               if (mant.contains(".")) {
                  mant = mant.replaceAll("0+$", "");
                  if (mant.endsWith(".")) {
                     mant = mant.substring(0, mant.length() - 1);
                  }
               }

               formatted = mant + exp;
            } else {
               formatted = String.format(Locale.ROOT, "%." + digits + "e", d);
            }

            int e = formatted.indexOf(101);
            String mant = formatted.substring(0, e);
            char sign = formatted.charAt(e + 1);
            String expDigits = formatted.substring(e + 2).replaceFirst("^0+(?!$)", "");
            return mant + "e" + sign + expDigits;
         } else {
            throw JsErrorException.rangeError("toExponential() fractionDigits must be between 0 and 100");
         }
      }
   }

   private static String formatPrecision(double value, int precision) {
      String formatted = String.format(Locale.ROOT, "%." + (precision - 1) + "e", value);
      int e = formatted.indexOf(101);
      if (e < 0) {
         return formatted;
      } else {
         String mant = formatted.substring(0, e);
         char sign = formatted.charAt(e + 1);
         String expDigits = formatted.substring(e + 2).replaceFirst("^0+(?!$)", "");
         return mant + "e" + sign + expDigits;
      }
   }

   private Object toLocaleString(Context context, Object[] args) {
      double d = thisNumber(context).doubleValue();
      DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance();
      int optionsIndex = args.length > 1 ? 1 : 0;
      if (args.length > optionsIndex && args[optionsIndex] instanceof Map<?, ?> options) {
         Object minFractionDigits = options.get("minimumFractionDigits");
         Object maxFractionDigits = options.get("maximumFractionDigits");
         if (minFractionDigits instanceof Number n) {
            df.setMinimumFractionDigits(n.intValue());
         }

         if (maxFractionDigits instanceof Number n) {
            df.setMaximumFractionDigits(n.intValue());
         }
      }

      return df.format(d);
   }

   private Object valueOf(Context context, Object[] args) {
      return thisNumber(context);
   }
}
