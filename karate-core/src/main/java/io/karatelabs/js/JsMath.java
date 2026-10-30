package io.karatelabs.js;

import java.util.function.BiFunction;
import java.util.function.Function;

class JsMath extends JsObject {
   private static final byte CONSTANT_ATTRS = 8;
   private static final byte METHOD_ATTRS = 13;

   JsMath() {
      this.defineOwn("E", Math.E, (byte)8);
      this.defineOwn("LN10", Math.log(10.0), (byte)8);
      this.defineOwn("LN2", Math.log(2.0), (byte)8);
      this.defineOwn("LOG2E", 1.0 / Math.log(2.0), (byte)8);
      this.defineOwn("LOG10E", 1.0 / Math.log(10.0), (byte)8);
      this.defineOwn("PI", Math.PI, (byte)8);
      this.defineOwn("SQRT1_2", Math.sqrt(0.5), (byte)8);
      this.defineOwn("SQRT2", Math.sqrt(2.0), (byte)8);
      this.installMethod("abs", 1, math(Math::abs));
      this.installMethod("acos", 1, math(Math::acos));
      this.installMethod("acosh", 1, math(x -> {
         if (x < 1.0) {
            throw JsErrorException.rangeError("value must be >= 1");
         } else {
            return Math.log(x + Math.sqrt(x * x - 1.0));
         }
      }));
      this.installMethod("asin", 1, math(Math::asin));
      this.installMethod("asinh", 1, math(x -> !Double.isNaN(x) && x != 0.0 && !Double.isInfinite(x) ? Math.log(x + Math.sqrt(x * x + 1.0)) : x));
      this.installMethod("atan", 1, math(Math::atan));
      this.installMethod("atan2", 2, math(Math::atan2));
      this.installMethod("atanh", 1, math(x -> {
         if (Double.isNaN(x)) {
            return Double.NaN;
         } else if (x > 1.0 || x < -1.0) {
            return Double.NaN;
         } else if (x == 1.0) {
            return Double.POSITIVE_INFINITY;
         } else if (x == -1.0) {
            return Double.NEGATIVE_INFINITY;
         } else {
            return x == 0.0 ? x : 0.5 * Math.log((1.0 + x) / (1.0 - x));
         }
      }));
      this.installMethod("cbrt", 1, math(Math::cbrt));
      this.installMethod("ceil", 1, math(Math::ceil));
      this.installMethod("clz32", 1, (JsInvokable)args -> {
         Number x = Terms.objectToNumber(args.length == 0 ? Terms.UNDEFINED : args[0]);
         double d = x.doubleValue();
         if (!Double.isNaN(d) && !Double.isInfinite(d) && d != 0.0) {
            double t = d < 0.0 ? Math.ceil(d) : Math.floor(d);
            double mod = t - Math.floor(t / 4.2949673E9F) * 4.2949673E9F;
            int n = (int)((long)mod);
            return Integer.numberOfLeadingZeros(n);
         } else {
            return 32;
         }
      });
      this.installMethod("cos", 1, math(Math::cos));
      this.installMethod("cosh", 1, math(Math::cosh));
      this.installMethod("exp", 1, math(Math::exp));
      this.installMethod("expm1", 1, math(Math::expm1));
      this.installMethod("floor", 1, math(Math::floor));
      this.installMethod("fround", 1, (JsInvokable)args -> {
         Number x = Terms.objectToNumber(args[0]);
         float y = (float)x.doubleValue();
         return (double)y;
      });
      this.installMethod("hypot", 2, (context, args) -> {
         CoreContext cc = (CoreContext)context;
         double[] coerced = new double[args.length];

         for (int i = 0; i < args.length; i++) {
            Number n = Terms.toNumberCoerce(args[i], cc);
            if (cc != null && cc.isError()) {
               return Terms.UNDEFINED;
            }

            coerced[i] = n.doubleValue();
         }

         boolean hasNaN = false;

         for (double d : coerced) {
            if (Double.isInfinite(d)) {
               return Double.POSITIVE_INFINITY;
            }

            if (Double.isNaN(d)) {
               hasNaN = true;
            }
         }

         if (hasNaN) {
            return Double.NaN;
         } else if (coerced.length == 0) {
            return 0;
         } else if (coerced.length == 1) {
            return Terms.narrow(Math.abs(coerced[0]));
         } else {
            double r = Math.hypot(coerced[0], coerced[1]);

            for (int i = 2; i < coerced.length; i++) {
               r = Math.hypot(r, coerced[i]);
            }

            return Terms.narrow(r);
         }
      });
      this.installMethod("imul", 2, (JsInvokable)args -> {
         Number x = Terms.objectToNumber(args[0]);
         Number y = Terms.objectToNumber(args[1]);
         return x.intValue() * y.intValue();
      });
      this.installMethod("log", 1, math(Math::log));
      this.installMethod("log10", 1, math(Math::log10));
      this.installMethod("log1p", 1, math(Math::log1p));
      this.installMethod("log2", 1, math(x -> Math.log(x) / Math.log(2.0)));
      this.installMethod("max", 2, (context, args) -> {
         CoreContext cc = (CoreContext)context;
         if (args.length == 0) {
            return Double.NEGATIVE_INFINITY;
         } else {
            double[] coerced = new double[args.length];

            for (int i = 0; i < args.length; i++) {
               Number n = Terms.toNumberCoerce(args[i], cc);
               if (cc != null && cc.isError()) {
                  return Terms.UNDEFINED;
               }

               coerced[i] = n.doubleValue();
            }

            double r = Double.NEGATIVE_INFINITY;

            for (double d : coerced) {
               if (Double.isNaN(d)) {
                  return Double.NaN;
               }

               r = Math.max(r, d);
            }

            return Terms.narrow(r);
         }
      });
      this.installMethod("min", 2, (context, args) -> {
         CoreContext cc = (CoreContext)context;
         if (args.length == 0) {
            return Double.POSITIVE_INFINITY;
         } else {
            double[] coerced = new double[args.length];

            for (int i = 0; i < args.length; i++) {
               Number n = Terms.toNumberCoerce(args[i], cc);
               if (cc != null && cc.isError()) {
                  return Terms.UNDEFINED;
               }

               coerced[i] = n.doubleValue();
            }

            double r = Double.POSITIVE_INFINITY;

            for (double d : coerced) {
               if (Double.isNaN(d)) {
                  return Double.NaN;
               }

               r = Math.min(r, d);
            }

            return Terms.narrow(r);
         }
      });
      this.installMethod("pow", 2, math(Math::pow));
      this.installMethod("random", 0, (JsInvokable)args -> Math.random());
      this.installMethod("round", 1, math(JsScalars::round));
      this.installMethod("sign", 1, math(x -> {
         if (Double.isNaN(x)) {
            return Double.NaN;
         } else {
            return x == 0.0 ? x : x > 0.0 ? 1.0 : -1.0;
         }
      }));
      this.installMethod("sin", 1, math(Math::sin));
      this.installMethod("sinh", 1, math(Math::sinh));
      this.installMethod("sqrt", 1, math(Math::sqrt));
      this.installMethod("tan", 1, math(Math::tan));
      this.installMethod("tanh", 1, math(Math::tanh));
      this.installMethod("trunc", 1, math(x -> x > 0.0 ? Math.floor(x) : Math.ceil(x)));
   }

   private void installMethod(String name, int length, JsCallable delegate) {
      this.defineOwn(name, new JsBuiltinMethod(name, length, delegate), (byte)13);
   }

   private static JsInvokable math(Function<Double, Double> fn) {
      return args -> {
         try {
            Number x = Terms.objectToNumber(args[0]);
            Double y = fn.apply(x.doubleValue());
            return Terms.narrow(y);
         } catch (Exception var4) {
            return Double.NaN;
         }
      };
   }

   private static JsInvokable math(BiFunction<Double, Double, Double> fn) {
      return args -> {
         try {
            Number x = Terms.objectToNumber(args[0]);
            Number y = Terms.objectToNumber(args[1]);
            Double r = fn.apply(x.doubleValue(), y.doubleValue());
            return Terms.narrow(r);
         } catch (Exception var5) {
            return Double.NaN;
         }
      };
   }
}
