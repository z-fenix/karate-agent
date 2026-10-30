package io.karatelabs.js;

import java.math.BigDecimal;
import java.math.BigInteger;

class JsBigIntConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsBigIntConstructor() {
      this.name = "BigInt";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("asIntN", new JsBuiltinMethod("asIntN", 2, this::asIntN), (byte)13);
      this.defineOwn("asUintN", new JsBuiltinMethod("asUintN", 2, this::asUintN), (byte)13);
      this.defineOwn("prototype", JsBigIntPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      if (args.length == 0) {
         throw JsErrorException.typeError("Cannot convert undefined to a BigInt");
      } else {
         return toBigInt(args[0], (CoreContext)context);
      }
   }

   static BigInteger toBigInt(Object value, CoreContext context) {
      if (value instanceof ObjectLike) {
         value = Terms.toPrimitive(value, "number", context);
         if (context != null && context.isError()) {
            return BigInteger.ZERO;
         }
      } else if (value instanceof JsValue jv) {
         value = jv.getJavaValue();
      }

      return primitiveToBigInt(value);
   }

   static BigInteger toBigInt(Object value) {
      return toBigInt(value, null);
   }

   private static BigInteger primitiveToBigInt(Object value) {
      if (value instanceof BigInteger bi) {
         return bi;
      } else if (value instanceof Boolean b) {
         return b ? BigInteger.ONE : BigInteger.ZERO;
      } else if (value instanceof Number n) {
         double d = n.doubleValue();
         if (!Double.isFinite(d)) {
            throw JsErrorException.rangeError("Cannot convert non-finite number to BigInt");
         } else if (d != Math.floor(d)) {
            throw JsErrorException.rangeError("Cannot convert non-integer number to BigInt");
         } else {
            return new BigDecimal(d).toBigInteger();
         }
      } else if (value instanceof String s) {
         String trimmed = s.trim();
         if (trimmed.isEmpty()) {
            return BigInteger.ZERO;
         } else {
            try {
               if (trimmed.length() > 2 && trimmed.charAt(0) == '0') {
                  char c = trimmed.charAt(1);
                  if (c == 'x' || c == 'X') {
                     return new BigInteger(trimmed.substring(2), 16);
                  }

                  if (c == 'o' || c == 'O') {
                     return new BigInteger(trimmed.substring(2), 8);
                  }

                  if (c == 'b' || c == 'B') {
                     return new BigInteger(trimmed.substring(2), 2);
                  }
               }

               return new BigInteger(trimmed);
            } catch (NumberFormatException var4) {
               throw JsErrorException.syntaxError("Cannot convert " + s + " to a BigInt");
            }
         }
      } else if (value != null && value != Terms.UNDEFINED) {
         throw JsErrorException.typeError("Cannot convert object to a BigInt");
      } else {
         throw JsErrorException.typeError("Cannot convert " + (value == null ? "null" : "undefined") + " to a BigInt");
      }
   }

   private Object asIntN(Context context, Object[] args) {
      int bits = toIndex(args.length > 0 ? args[0] : Terms.UNDEFINED, (CoreContext)context);
      BigInteger bi = toBigInt(args.length > 1 ? args[1] : Terms.UNDEFINED, (CoreContext)context);
      if (bits == 0) {
         return BigInteger.ZERO;
      } else {
         BigInteger mod = BigInteger.ONE.shiftLeft(bits);
         BigInteger r = bi.mod(mod);
         BigInteger half = BigInteger.ONE.shiftLeft(bits - 1);
         if (r.compareTo(half) >= 0) {
            r = r.subtract(mod);
         }

         return r;
      }
   }

   private Object asUintN(Context context, Object[] args) {
      int bits = toIndex(args.length > 0 ? args[0] : Terms.UNDEFINED, (CoreContext)context);
      BigInteger bi = toBigInt(args.length > 1 ? args[1] : Terms.UNDEFINED, (CoreContext)context);
      if (bits == 0) {
         return BigInteger.ZERO;
      } else {
         BigInteger mod = BigInteger.ONE.shiftLeft(bits);
         return bi.mod(mod);
      }
   }

   private static int toIndex(Object value, CoreContext context) {
      if (value instanceof ObjectLike) {
         value = Terms.toPrimitive(value, "number", context);
         if (context != null && context.isError()) {
            return 0;
         }
      }

      Number n = Terms.objectToNumber(value);
      double d = n.doubleValue();
      if (!Double.isNaN(d) && d != 0.0) {
         long truncated = (long)d;
         if (truncated < 0L) {
            throw JsErrorException.rangeError("ToIndex: value must be non-negative");
         } else {
            return (int)truncated;
         }
      } else {
         return 0;
      }
   }
}
