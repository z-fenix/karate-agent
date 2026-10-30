package io.karatelabs.js;

import io.karatelabs.common.Xml;
import io.karatelabs.parser.Token;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;

public class Terms {
   public static final JsUndefined UNDEFINED = JsUndefined.INSTANCE;
   static final Number NEGATIVE_ZERO = -0.0;
   static final int LESS_UNDEFINED = -1;

   private Terms() {
   }

   private static boolean isBigIntOp(Number lhs, Number rhs) {
      return lhs instanceof BigInteger || rhs instanceof BigInteger;
   }

   private static void requireBothBigInt(Number lhs, Number rhs, String opName) {
      if (!(lhs instanceof BigInteger) || !(rhs instanceof BigInteger)) {
         throw JsErrorException.typeError("Cannot mix BigInt and other types, use explicit conversions (" + opName + ")");
      }
   }

   static Number parseInt(String str, int radix) {
      if (str == null) {
         return Double.NaN;
      } else {
         str = str.trim();
         if (str.isEmpty()) {
            return Double.NaN;
         } else {
            boolean negative = false;
            int index = 0;
            if (str.charAt(0) == '-') {
               negative = true;
               index++;
            } else if (str.charAt(0) == '+') {
               index++;
            }

            if (radix == 0 || radix >= 2 && radix <= 36) {
               if ((radix == 0 || radix == 16) && radixPrefix(str, index) == 16) {
                  radix = 16;
                  index += 2;
               } else if (radix == 0) {
                  radix = 10;
               }

               long result = 0L;

               boolean foundDigit;
               for (foundDigit = false; index < str.length(); index++) {
                  char ch = str.charAt(index);
                  int digit;
                  if (ch >= '0' && ch <= '9') {
                     digit = ch - '0';
                  } else if (ch >= 'a' && ch <= 'z') {
                     digit = ch - 'a' + 10;
                  } else {
                     if (ch < 'A' || ch > 'Z') {
                        break;
                     }

                     digit = ch - 'A' + 10;
                  }

                  if (digit >= radix) {
                     break;
                  }

                  result = result * radix + digit;
                  foundDigit = true;
               }

               if (!foundDigit) {
                  return Double.NaN;
               } else {
                  double value = negative ? -result : result;
                  return narrow(value);
               }
            } else {
               return Double.NaN;
            }
         }
      }
   }

   static Number parseFloat(String str, boolean asInt) {
      if (str == null) {
         return Double.NaN;
      } else {
         str = str.trim();
         if (str.isEmpty()) {
            return Double.NaN;
         } else {
            int index = 0;
            if (str.charAt(index) == '-' || str.charAt(index) == '+') {
               index++;
            }

            if (!asInt && str.startsWith("Infinity", index)) {
               return str.charAt(0) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            } else {
               if (asInt) {
                  Number radix = fromRadixPrefix(str);
                  if (radix != null) {
                     return narrow(radix.doubleValue());
                  }
               }

               boolean foundDigit = false;
               boolean seenDot = false;

               while (index < str.length()) {
                  char ch = str.charAt(index);
                  if (ch == '.' && !asInt && !seenDot) {
                     seenDot = true;
                     index++;
                  } else {
                     if (ch < '0' || ch > '9') {
                        break;
                     }

                     foundDigit = true;
                     index++;
                  }
               }

               if (!foundDigit) {
                  return Double.NaN;
               } else {
                  if (!asInt && index < str.length() && (str.charAt(index) == 'e' || str.charAt(index) == 'E')) {
                     int p = index + 1;
                     if (p < str.length() && (str.charAt(p) == '+' || str.charAt(p) == '-')) {
                        p++;
                     }

                     int digitsStart = p;

                     while (p < str.length() && str.charAt(p) >= '0' && str.charAt(p) <= '9') {
                        p++;
                     }

                     if (p > digitsStart) {
                        index = p;
                     }
                  }

                  return narrow(Double.parseDouble(str.substring(0, index)));
               }
            }
         }
      }
   }

   static Number objectToNumber(Object o) {
      if (o instanceof JsValue jv) {
         o = jv.getJsValue();
      }
      return (Number)(switch (o) {
         case null -> 0;
         case Number n -> n;
         case Boolean b -> b ? 1 : 0;
         case Date d -> d.getTime();
         case String s -> stringToNumber(s);
         default -> Double.NaN;
      });
   }

   static Number toNumberCoerce(Object o, CoreContext context) {
      Object prim = toPrimitive(o, "number", context);
      return (Number)(context != null && context.isError() ? Double.NaN : objectToNumber(prim));
   }

   public static Number toNumber(String text) {
      if (text.isEmpty()) {
         return 0;
      } else {
         try {
            return narrow(Double.parseDouble(text));
         } catch (Exception var3) {
            Number radix = fromRadixPrefix(text);
            return (Number)(radix == null ? Double.NaN : narrow(radix.doubleValue()));
         }
      }
   }

   public static Number stringToNumber(String text) {
      text = stripJsWhiteSpace(text);
      if (text.isEmpty()) {
         return 0;
      } else {
         return (Number)(text.indexOf(95) >= 0 ? Double.NaN : toNumber(text));
      }
   }

   static Number literalToNumber(String text) {
      if (text.indexOf(95) >= 0) {
         text = text.replace("_", "");
      }

      return toNumber(text);
   }

   private static String stripJsWhiteSpace(String s) {
      int len = s.length();
      int start = 0;

      while (start < len && isJsWhiteSpace(s.charAt(start))) {
         start++;
      }

      int end = len;

      while (end > start && isJsWhiteSpace(s.charAt(end - 1))) {
         end--;
      }

      return start == 0 && end == len ? s : s.substring(start, end);
   }

   private static boolean isJsWhiteSpace(char c) {
      return Character.isWhitespace(c) || c == 160 || c == '\ufeff';
   }

   public static Object literalValue(Token token) {
      Object cached = token.getCachedLiteral();
      if (cached != null) {
         return cached;
      } else {
         Object value = computeLiteralValue(token);
         if (value != null) {
            token.cacheLiteral(value);
         }

         return value;
      }
   }

   private static Object computeLiteralValue(Token token) {
      return switch (token.type) {
         case S_STRING, D_STRING -> {
            String text = token.getText();
            yield text.substring(1, text.length() - 1);
         }
         case NUMBER -> literalToNumber(token.getText());
         case BIGINT -> toBigInt(token.getText());
         case TRUE -> true;
         case FALSE -> false;
         default -> null;
      };
   }

   private static BigInteger toBigInt(String text) {
      String s = text.substring(0, text.length() - 1);
      if (s.indexOf(95) >= 0) {
         s = s.replace("_", "");
      }

      int radix = radixPrefix(s);
      return radix == 0 ? new BigInteger(s) : new BigInteger(s.substring(2), radix);
   }

   static int radixPrefix(String s, int from) {
      if (s.length() > from + 1 && s.charAt(from) == '0') {
         char p = s.charAt(from + 1);
         if (p == 'x' || p == 'X') {
            return 16;
         }

         if (p == 'b' || p == 'B') {
            return 2;
         }

         if (p == 'o' || p == 'O') {
            return 8;
         }
      }

      return 0;
   }

   private static int radixPrefix(String s) {
      return radixPrefix(s, 0);
   }

   static long toUint32(Object value) {
      double d = objectToNumber(value).doubleValue();
      if (!Double.isNaN(d) && !Double.isInfinite(d)) {
         double truncated = d < 0.0 ? Math.ceil(d) : Math.floor(d);
         return (long)(truncated % 4.2949673E9F) & 4294967295L;
      } else {
         return 0L;
      }
   }

   static Number fromRadixPrefix(String text) {
      int radix = radixPrefix(text);
      if (radix == 0) {
         return null;
      } else {
         try {
            return narrow(Long.parseLong(text.substring(2), radix));
         } catch (NumberFormatException var3) {
            return Double.NaN;
         }
      }
   }

   public static boolean sameValue(Object lhs, Object rhs) {
      if (lhs instanceof Number ln && rhs instanceof Number rn && !(lhs instanceof BigInteger) && !(rhs instanceof BigInteger)) {
         double a = ln.doubleValue();
         double b = rn.doubleValue();
         if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
         } else {
            return a == 0.0 && b == 0.0 ? Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b) : a == b;
         }
      } else {
         return eq(lhs, rhs, true);
      }
   }

   static boolean eq(Object lhs, Object rhs, boolean strict) {
      if (lhs == null) {
         return rhs == null || !strict && rhs == UNDEFINED;
      } else if (lhs == UNDEFINED) {
         return rhs == UNDEFINED || !strict && rhs == null;
      } else if (rhs == null || rhs == UNDEFINED) {
         return false;
      } else if (lhs == rhs) {
         return true;
      } else if (lhs instanceof JsPrimitive || !(lhs instanceof List) && !(lhs instanceof Map)) {
         if (lhs.equals(rhs)) {
            return true;
         } else if (strict) {
            if (lhs instanceof BigInteger || rhs instanceof BigInteger) {
               return false;
            } else {
               return lhs instanceof Number && rhs instanceof Number ? ((Number)lhs).doubleValue() == ((Number)rhs).doubleValue() : false;
            }
         } else {
            if (lhs instanceof JsPrimitive jp) {
               lhs = jp.getJavaValue();
            }

            if (rhs instanceof JsPrimitive jp) {
               rhs = jp.getJavaValue();
            }

            if (lhs.equals(rhs)) {
               return true;
            } else if (lhs instanceof BigInteger || rhs instanceof BigInteger) {
               return bigIntLooseEq(lhs, rhs);
            } else {
               return !(lhs instanceof Number) && !(rhs instanceof Number) ? false : objectToNumber(lhs).doubleValue() == objectToNumber(rhs).doubleValue();
            }
         }
      } else {
         return false;
      }
   }

   private static boolean bigIntLooseEq(Object lhs, Object rhs) {
      BigInteger bi;
      Object other;
      if (lhs instanceof BigInteger b) {
         bi = b;
         other = rhs;
      } else {
         bi = (BigInteger)rhs;
         other = lhs;
      }

      if (!(other instanceof String s)) {
         if (other instanceof Number n) {
            double d = n.doubleValue();
            if (!Double.isFinite(d)) {
               return false;
            } else {
               return d != Math.floor(d) ? false : bi.equals(new BigDecimal(d).toBigInteger());
            }
         } else {
            return other instanceof Boolean b ? bi.equals(b ? BigInteger.ONE : BigInteger.ZERO) : false;
         }
      } else {
         BigInteger parsed = stringToBigInt(s);
         return parsed != null && bi.equals(parsed);
      }
   }

   private static BigInteger stringToBigInt(String s) {
      String trimmed = stripJsWhiteSpace(s);
      if (trimmed.isEmpty()) {
         return BigInteger.ZERO;
      } else {
         int radix = radixPrefix(trimmed);

         try {
            return radix == 0 ? new BigInteger(trimmed) : new BigInteger(trimmed.substring(2), radix);
         } catch (NumberFormatException var4) {
            return null;
         }
      }
   }

   private static boolean isNotANumber(Object o) {
      return o instanceof Double d && d.isNaN();
   }

   static boolean strictEq(Object lhs, Object rhs) {
      return !isNotANumber(lhs) && !isNotANumber(rhs) ? eq(lhs, rhs, true) : false;
   }

   static boolean looseEq(Object lhs, Object rhs, CoreContext context) {
      if (!(lhs instanceof JsSymbol) && !(rhs instanceof JsSymbol)) {
         if (lhs instanceof ObjectLike) {
            if (rhs instanceof ObjectLike) {
               return lhs == rhs;
            }

            if (rhs == null || rhs == UNDEFINED) {
               return false;
            }

            lhs = toPrimitive(lhs, "default", context);
            if (context != null && context.isError()) {
               return false;
            }
         } else if (rhs instanceof ObjectLike) {
            if (lhs == null || lhs == UNDEFINED) {
               return false;
            }

            rhs = toPrimitive(rhs, "default", context);
            if (context != null && context.isError()) {
               return false;
            }
         }

         if (!isNotANumber(lhs) && !isNotANumber(rhs)) {
            if (lhs instanceof Boolean b) {
               lhs = b ? 1 : 0;
            }

            if (rhs instanceof Boolean b) {
               rhs = b ? 1 : 0;
            }

            return eq(lhs, rhs, false);
         } else {
            return false;
         }
      } else {
         return lhs == rhs;
      }
   }

   static int isLessThan(Object lhs, Object rhs, boolean leftFirst, CoreContext context) {
      if (lhs instanceof Number ln && rhs instanceof Number rn && !isBigIntOp(ln, rn)) {
         double a = ln.doubleValue();
         double b = rn.doubleValue();
         if (!Double.isNaN(a) && !Double.isNaN(b)) {
            return a < b ? 1 : 0;
         } else {
            return -1;
         }
      } else {
         if (lhs instanceof ObjectLike || rhs instanceof ObjectLike) {
            if (leftFirst) {
               lhs = toPrimitive(lhs, "number", context);
               if (context != null && context.isError()) {
                  return -1;
               }

               rhs = toPrimitive(rhs, "number", context);
            } else {
               rhs = toPrimitive(rhs, "number", context);
               if (context != null && context.isError()) {
                  return -1;
               }

               lhs = toPrimitive(lhs, "number", context);
            }

            if (context != null && context.isError()) {
               return -1;
            }
         }

         if (lhs instanceof String ls && rhs instanceof String rs) {
            return ls.compareTo(rs) < 0 ? 1 : 0;
         } else if (!(lhs instanceof BigInteger) && !(rhs instanceof BigInteger)) {
            double a = objectToNumber(lhs).doubleValue();
            double b = objectToNumber(rhs).doubleValue();
            if (!Double.isNaN(a) && !Double.isNaN(b)) {
               return a < b ? 1 : 0;
            } else {
               return -1;
            }
         } else {
            int c = bigIntCompare(
               !(lhs instanceof BigInteger) && !(lhs instanceof String) ? objectToNumber(lhs) : lhs,
               !(rhs instanceof BigInteger) && !(rhs instanceof String) ? objectToNumber(rhs) : rhs
            );
            return c == Integer.MIN_VALUE ? -1 : (c < 0 ? 1 : 0);
         }
      }
   }

   private static int bigIntCompare(Object lhs, Object rhs) {
      if (lhs instanceof BigInteger && rhs instanceof BigInteger) {
         return ((BigInteger)lhs).compareTo((BigInteger)rhs);
      } else {
         BigInteger bi;
         Object other;
         boolean swapped;
         if (lhs instanceof BigInteger b) {
            bi = b;
            other = rhs;
            swapped = false;
         } else {
            bi = (BigInteger)rhs;
            other = lhs;
            swapped = true;
         }

         if (other instanceof String s) {
            BigInteger parsed = stringToBigInt(s);
            if (parsed == null) {
               return Integer.MIN_VALUE;
            } else {
               int c = bi.compareTo(parsed);
               return swapped ? -c : c;
            }
         } else if (other instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d)) {
               return Integer.MIN_VALUE;
            } else if (d == Double.POSITIVE_INFINITY) {
               return swapped ? 1 : -1;
            } else if (d == Double.NEGATIVE_INFINITY) {
               return swapped ? -1 : 1;
            } else {
               int c = new BigDecimal(d).compareTo(new BigDecimal(bi));
               c = -c;
               return swapped ? -c : c;
            }
         } else {
            return Integer.MIN_VALUE;
         }
      }
   }

   private static Number toNumericOperand(Object o, CoreContext context) {
      if (o instanceof ObjectLike) {
         Object prim = toPrimitive(o, "number", context);
         return (Number)(context != null && context.isError() ? Double.NaN : objectToNumber(prim));
      } else {
         return objectToNumber(o);
      }
   }

   static Object bitAnd(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "&");
            return narrowBigInt(((BigInteger)lhs).and((BigInteger)rhs));
         } else {
            return lhs.intValue() & rhs.intValue();
         }
      }
   }

   static Object bitOr(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "|");
            return narrowBigInt(((BigInteger)lhs).or((BigInteger)rhs));
         } else {
            return lhs.intValue() | rhs.intValue();
         }
      }
   }

   static Object bitXor(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "^");
            return narrowBigInt(((BigInteger)lhs).xor((BigInteger)rhs));
         } else {
            return lhs.intValue() ^ rhs.intValue();
         }
      }
   }

   static Object bitShiftRight(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, ">>");
            return narrowBigInt(((BigInteger)lhs).shiftRight(((BigInteger)rhs).intValueExact()));
         } else {
            return lhs.intValue() >> rhs.intValue();
         }
      }
   }

   static Object bitShiftLeft(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "<<");
            return narrowBigInt(((BigInteger)lhs).shiftLeft(((BigInteger)rhs).intValueExact()));
         } else {
            return lhs.intValue() << rhs.intValue();
         }
      }
   }

   static Object bitShiftRightUnsigned(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            throw JsErrorException.typeError("BigInts have no unsigned right shift, use >> instead");
         } else {
            return narrow((lhs.intValue() & 4294967295L) >>> rhs.intValue());
         }
      }
   }

   static Object bitNot(Object value, CoreContext context) {
      Number number = toNumericOperand(value, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         return number instanceof BigInteger bi ? narrowBigInt(bi.not()) : ~number.intValue();
      }
   }

   static Object mul(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "*");
            return narrowBigInt(((BigInteger)lhs).multiply((BigInteger)rhs));
         } else {
            double result = lhs.doubleValue() * rhs.doubleValue();
            return narrow(result);
         }
      }
   }

   static Object div(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "/");
            BigInteger r = (BigInteger)rhs;
            if (r.signum() == 0) {
               throw JsErrorException.rangeError("Division by zero");
            } else {
               return narrowBigInt(((BigInteger)lhs).divide(r));
            }
         } else {
            double result = lhs.doubleValue() / rhs.doubleValue();
            return narrow(result);
         }
      }
   }

   static Object min(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "-");
            return narrowBigInt(((BigInteger)lhs).subtract((BigInteger)rhs));
         } else {
            double result = lhs.doubleValue() - rhs.doubleValue();
            return narrow(result);
         }
      }
   }

   static Object mod(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "%");
            BigInteger r = (BigInteger)rhs;
            if (r.signum() == 0) {
               throw JsErrorException.rangeError("Division by zero");
            } else {
               return narrowBigInt(((BigInteger)lhs).remainder(r));
            }
         } else {
            double result = lhs.doubleValue() % rhs.doubleValue();
            return narrow(result);
         }
      }
   }

   static Object exp(Object lhsObject, Object rhsObject, CoreContext context) {
      Number lhs = toNumericOperand(lhsObject, context);
      if (context != null && context.isError()) {
         return UNDEFINED;
      } else {
         Number rhs = toNumericOperand(rhsObject, context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         } else if (isBigIntOp(lhs, rhs)) {
            requireBothBigInt(lhs, rhs, "**");
            BigInteger r = (BigInteger)rhs;
            if (r.signum() < 0) {
               throw JsErrorException.rangeError("Exponent must be non-negative");
            } else {
               return narrowBigInt(((BigInteger)lhs).pow(r.intValueExact()));
            }
         } else {
            double result = Math.pow(lhs.doubleValue(), rhs.doubleValue());
            return narrow(result);
         }
      }
   }

   static Object add(Object lhs, Object rhs, CoreContext context) {
      if (lhs instanceof ObjectLike) {
         lhs = toPrimitive(lhs, "default", context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         }
      }

      if (rhs instanceof ObjectLike) {
         rhs = toPrimitive(rhs, "default", context);
         if (context != null && context.isError()) {
            return UNDEFINED;
         }
      }

      if (lhs instanceof String || rhs instanceof String) {
         return concatOperand(lhs) + concatOperand(rhs);
      } else if (!(lhs instanceof BigInteger) && !(rhs instanceof BigInteger)) {
         Number lhsNum = objectToNumber(lhs);
         Number rhsNum = objectToNumber(rhs);
         double result = lhsNum.doubleValue() + rhsNum.doubleValue();
         return narrow(result);
      } else if (lhs instanceof BigInteger && rhs instanceof BigInteger) {
         return narrowBigInt(((BigInteger)lhs).add((BigInteger)rhs));
      } else {
         throw JsErrorException.typeError("Cannot mix BigInt and other types, use explicit conversions (+)");
      }
   }

   private static String concatOperand(Object o) {
      if (o instanceof String s) {
         return s;
      } else {
         return o instanceof Number n ? numberToString(n) : String.valueOf(o);
      }
   }

   static BigInteger narrowBigInt(BigInteger value) {
      return value;
   }

   static Object incDecStep(Object operand) {
      return operand instanceof BigInteger ? BigInteger.ONE : 1;
   }

   public static Number narrow(double d) {
      long l = (long)d;
      if (l != d) {
         return d;
      } else if (l == 0L && Double.doubleToRawLongBits(d) != 0L) {
         return d;
      } else {
         return (Number)(l >= -2147483648L && l <= 2147483647L ? (int)l : l);
      }
   }

   static JsValue toJsValue(Object o) {
      if (o == null) {
         return null;
      } else {
         return (JsValue)(switch (o) {
            case String s -> new JsString(s);
            case BigInteger bi -> new JsBigInt(bi);
            case Number n -> new JsNumber(n);
            case Boolean b -> new JsBoolean(b);
            case Date d -> new JsDate(d);
            case Instant i -> new JsDate(i);
            case LocalDateTime ldt -> new JsDate(ldt);
            case LocalDate ld -> new JsDate(ld);
            case ZonedDateTime zdt -> new JsDate(zdt);
            case byte[] bytes -> new JsUint8Array(bytes);
            default -> null;
         });
      }
   }

   static JsArray toJsArray(Object o) {
      if (o != null && o.getClass().isArray() && !(o instanceof byte[])) {
         int length = Array.getLength(o);
         List<Object> list = new ArrayList<>(length);

         for (int i = 0; i < length; i++) {
            list.add(Array.get(o, i));
         }

         return new JsArray(list);
      } else {
         return null;
      }
   }

   static ObjectLike toObjectLike(Object o) {
      if (o instanceof ObjectLike ol) {
         return ol;
      } else if (o instanceof List<?> list) {
         return new JsArray((List<Object>)list);
      } else {
         JsArray jsArray = toJsArray(o);
         if (jsArray != null) {
            return jsArray;
         } else if (o instanceof Node node && Xml.toObject(node) instanceof Map<?, ?> map) {
            return new JsObject((Map<String, Object>)map);
         } else {
            return toJsValue(o) instanceof ObjectLike ol ? ol : null;
         }
      }
   }

   static Iterable<KeyValue> toIterable(Object o) {
      return toIterable(o, null);
   }

   static Iterable<KeyValue> toIterable(Object o, CoreContext ctx) {
      if (o instanceof JsArray jsArray) {
         return jsArray.jsEntries(ctx);
      } else if (o instanceof JsObject jsObject) {
         return jsObject.jsEntries(ctx);
      } else if (o instanceof List) {
         return new JsArray((List<Object>)o).jsEntries(ctx);
      } else {
         JsArray jsArray = toJsArray(o);
         if (jsArray != null) {
            return jsArray.jsEntries(ctx);
         } else if (o instanceof Map) {
            return new JsObject((Map<String, Object>)o).jsEntries(ctx);
         } else if (o instanceof String) {
            return new JsString((String)o).jsEntries(ctx);
         } else {
            return o instanceof ObjectLike obj ? new JsObject(obj.toMap()).jsEntries(ctx) : new JsObject().jsEntries(ctx);
         }
      }
   }

   static void copyDataProperties(Map<String, Object> target, Object source, Set<String> excluded, CoreContext ctx) {
      if (source != null && source != UNDEFINED && (ctx == null || !ctx.isError())) {
         for (KeyValue kv : toIterable(source, ctx)) {
            if (ctx != null && ctx.isError()) {
               return;
            }

            if (excluded == null || !excluded.contains(kv.key())) {
               target.put(kv.key(), kv.value());
            }
         }

         if (source instanceof JsObject from && target instanceof JsObject to) {
            for (JsSymbol sym : from.ownEnumerableSymbols()) {
               if (ctx != null && ctx.isError()) {
                  return;
               }

               Object v = from.getSymbol(sym, ctx);
               if (ctx != null && ctx.isError()) {
                  return;
               }

               to.defineOwnSymbol(sym, v, (byte)7);
            }
         }
      }
   }

   static Iterable<KeyValue> forInIterable(Object o, CoreContext ctx) {
      if (o == null || o == UNDEFINED) {
         return Collections.emptyList();
      }

      return () -> {
         List<KeyValue> out = new ArrayList<>();
         Set<String> seen = new HashSet<>();
         int yieldCount = 0;

         for (KeyValue kv : toIterable(o, ctx)) {
            if (seen.add(kv.key())) {
               out.add(new KeyValue(o, yieldCount++, kv.key(), kv.value()));
            }
         }

         ObjectLike ol;
         for (ObjectLike cur = o instanceof ObjectLike olObj ? olObj.getPrototype() : null; cur != null; cur = cur.getPrototype()) {
            for (String key : enumerableOwnKeysAtLevel(cur)) {
               if (seen.add(key)) {
                  out.add(new KeyValue(o, yieldCount++, key, null));
               }
            }
         }

         return out.iterator();
      };
   }

   private static Iterable<String> enumerableOwnKeysAtLevel(ObjectLike ol) {
      if (ol instanceof JsArray ja) {
         Set<String> out = new LinkedHashSet<>();

         for (KeyValue kv : ja.jsEntries(null)) {
            out.add(kv.key());
         }

         return out;
      } else if (!(ol instanceof JsObject jo)) {
         if (ol instanceof Prototype p) {
            Set<String> out = new LinkedHashSet<>();

            for (String name : p.toMap().keySet()) {
               if ((p.getOwnAttrs(name) & 2) != 0) {
                  out.add(name);
               }
            }

            return out;
         } else {
            return Collections.emptyList();
         }
      } else {
         Set<String> out = new LinkedHashSet<>();

         for (KeyValue kv : jo.jsEntries(null)) {
            out.add(kv.key());
         }

         return out;
      }
   }

   public static boolean isTruthy(Object value) {
      if (value == null || value.equals(UNDEFINED) || value.equals(Double.NaN)) {
         return false;
      } else if (value instanceof JsPrimitive) {
         return true;
      } else {
         if (value instanceof JsValue jv) {
            value = jv.getJavaValue();
         }
         return switch (value) {
            case Boolean b -> b;
            case Number number -> number.doubleValue() != 0.0;
            case String s -> !s.isEmpty();
            default -> true;
         };
      }
   }

   static boolean isPrimitive(Object value) {
      if (value instanceof String || value instanceof Number || value instanceof Boolean) {
         return true;
      } else {
         return value == null ? true : value == UNDEFINED;
      }
   }

   public static String typeOf(Object value) {
      if (value instanceof String) {
         return "string";
      } else if (value instanceof JsSymbol) {
         return "symbol";
      } else if (value instanceof JsInvokable) {
         return "function";
      } else if (value instanceof JsObject jo && jo.isJsFunction()) {
         return "function";
      } else if (value instanceof JsCallable && !(value instanceof ObjectLike)) {
         return "function";
      } else if (value instanceof JsPrimitive) {
         return "object";
      } else if (value instanceof BigInteger) {
         return "bigint";
      } else if (value instanceof Number) {
         return "number";
      } else if (value instanceof Boolean) {
         return "boolean";
      } else {
         return value == UNDEFINED ? "undefined" : "object";
      }
   }

   public static boolean isPlainObject(Object value) {
      return value != null && value.getClass() == JsObject.class;
   }

   public static boolean isPlainArray(Object value) {
      return value != null && value.getClass() == JsArray.class;
   }

   public static boolean isSymbol(Object value) {
      return value instanceof JsSymbol;
   }

   static boolean instanceOf(Object lhs, Object rhs) {
      if (rhs instanceof JsArray && lhs instanceof JsArray) {
         return true;
      } else if (rhs instanceof JsCallable && !(rhs instanceof JsFunction) && lhs != null && lhs.getClass().equals(rhs.getClass())) {
         return true;
      } else if (lhs instanceof JsValue && rhs instanceof JsValue) {
         return lhs.getClass().equals(rhs.getClass());
      } else {
         if (lhs instanceof ObjectLike objectLhs && rhs instanceof ObjectLike objectRhs) {
            Object target = objectRhs.getMember("prototype");

            for (ObjectLike current = objectLhs.getPrototype(); current != null; current = current.getPrototype()) {
               if (current == target) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   static boolean in(Object lhs, Object rhs) {
      if (rhs instanceof ObjectLike obj) {
         JsSymbol sym = JsSymbol.keyedBy(lhs);
         if (sym != null) {
            for (ObjectLike walk = obj; walk != null; walk = walk.getPrototype()) {
               if (walk instanceof JsObject jo && jo.hasSymbol(sym)) {
                  return true;
               }
            }

            return false;
         } else {
            String key = toPropertyKey(lhs);

            for (ObjectLike current = obj; current != null; current = current.getPrototype()) {
               if (current.isOwnProperty(key)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         throw JsErrorException.typeError("Cannot use 'in' operator to search for '" + lhs + "' in " + rhs);
      }
   }

   static Object toPrimitive(Object value, String hint, CoreContext context) {
      if (value == null || value == UNDEFINED) {
         return value;
      } else if (value instanceof JsSymbol) {
         throw JsErrorException.typeError("Cannot convert a Symbol value to a string");
      } else if (value instanceof JsPrimitive jp) {
         return jp.getJavaValue();
      } else if (!(value instanceof BigInteger) && !isPrimitive(value)) {
         ObjectLike ol = value instanceof ObjectLike ? (ObjectLike)value : toObjectLike(value);
         if (ol != null && context != null) {
            Object exotic = ol.getMember("@@toPrimitive");
            if (exotic == null || exotic == UNDEFINED) {
               return ordinaryToPrimitive(ol, hint, context);
            } else if (exotic instanceof JsCallable jsc) {
               CoreContext callCtx = new CoreContext(context, null, null);
               callCtx.thisObject = ol;
               String hintArg = hint == null ? "default" : hint;
               Object r = jsc.call(callCtx, new Object[]{hintArg});
               if (callCtx.isError()) {
                  context.updateFrom(callCtx);
                  return UNDEFINED;
               } else if (r != null && r != UNDEFINED && !isPrimitive(r) && !(r instanceof BigInteger)) {
                  throw JsErrorException.typeError("Cannot convert object to primitive value");
               } else {
                  return r;
               }
            } else {
               throw JsErrorException.typeError("Symbol.toPrimitive method is not callable");
            }
         } else {
            return value;
         }
      } else {
         return value;
      }
   }

   static Object ordinaryToPrimitive(ObjectLike ol, String hint, CoreContext context) {
      String[] order = "string".equals(hint) ? new String[]{"toString", "valueOf"} : new String[]{"valueOf", "toString"};

      for (String methodName : order) {
         if (ol.getMember(methodName) instanceof JsCallable jsc) {
            CoreContext callCtx = new CoreContext(context, null, null);
            callCtx.thisObject = ol;
            Object r = jsc.call(callCtx, new Object[0]);
            if (callCtx.isError()) {
               context.updateFrom(callCtx);
               return UNDEFINED;
            }

            if (r == null || r == UNDEFINED || isPrimitive(r) || r instanceof BigInteger) {
               return r;
            }
         }
      }

      throw JsErrorException.typeError("Cannot convert object to primitive value");
   }

   public static String toPropertyKey(Object o) {
      return toPropertyKey(o, null);
   }

   public static String toPropertyKey(Object o, CoreContext ctx) {
      if (o == null) {
         return "null";
      } else if (o instanceof String s) {
         return s;
      } else if (o instanceof JsSymbol sym) {
         return sym.toString();
      } else if (o == UNDEFINED) {
         return "undefined";
      } else if (o instanceof JsString js) {
         return js.toString();
      } else if (o instanceof Number n) {
         return numberToString(n);
      } else if (o instanceof Boolean) {
         return o.toString();
      } else {
         if (ctx != null) {
            Object prim = toPrimitive(o, "string", ctx);
            if (ctx.isError()) {
               return "";
            }

            if (prim != o) {
               return toPropertyKey(prim, ctx);
            }
         }

         return o.toString();
      }
   }

   public static String numberToString(Number n) {
      if (n instanceof Integer i) {
         return Integer.toString(i);
      } else if (n instanceof Long l) {
         return Long.toString(l);
      } else if (n instanceof BigInteger bi) {
         return bi.toString();
      } else {
         double d = n.doubleValue();
         if (Double.isNaN(d)) {
            return "NaN";
         } else if (d == 0.0) {
            return "0";
         } else if (Double.isInfinite(d)) {
            return d > 0.0 ? "Infinity" : "-Infinity";
         } else {
            double abs = Math.abs(d);
            if (!(abs < 1.0E21) || !(abs >= 1.0E-6)) {
               String s = Double.toString(d);
               int eIdx = s.indexOf(69);
               if (eIdx < 0) {
                  return s;
               } else {
                  String mantissa = s.substring(0, eIdx);
                  String exp = s.substring(eIdx + 1);
                  if (mantissa.endsWith(".0")) {
                     mantissa = mantissa.substring(0, mantissa.length() - 2);
                  }

                  if (exp.charAt(0) != '+' && exp.charAt(0) != '-') {
                     exp = "+" + exp;
                  }

                  return mantissa + "e" + exp;
               }
            } else if (d == Math.floor(d)) {
               long l = (long)d;
               return l == d ? Long.toString(l) : BigDecimal.valueOf(d).toBigInteger().toString();
            } else {
               return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            }
         }
      }
   }

   public static void requireObjectCoercible(Object value, String methodName) {
      if (value == null || value == UNDEFINED) {
         throw JsErrorException.typeError(methodName + " called on null or undefined");
      }
   }

   public static String toStringCoerce(Object o, CoreContext context) {
      if (o instanceof String s) {
         return s;
      } else if (o instanceof JsSymbol) {
         throw JsErrorException.typeError("Cannot convert a Symbol value to a string");
      } else if (o instanceof Number n) {
         return numberToString(n);
      } else if (o == null) {
         return "null";
      } else if (o == UNDEFINED) {
         return "undefined";
      } else if (!isPrimitive(o) && !(o instanceof JsValue)) {
         ObjectLike ol = o instanceof ObjectLike ? (ObjectLike)o : toObjectLike(o);
         if (ol != null) {
            if (context != null && ol.getMember("toString") instanceof JsCallable jsc) {
               CoreContext callCtx = new CoreContext(context, null, null);
               callCtx.thisObject = ol;
               Object r = jsc.call(callCtx, new Object[0]);
               if (callCtx.isError()) {
                  context.updateFrom(callCtx);
                  return "";
               }

               if (r instanceof String s) {
                  return s;
               }

               if (r != null && r != UNDEFINED && !(r instanceof ObjectLike)) {
                  return r.toString();
               }
            }

            return "[object Object]";
         } else {
            return o.toString();
         }
      } else {
         return o.toString();
      }
   }
}
