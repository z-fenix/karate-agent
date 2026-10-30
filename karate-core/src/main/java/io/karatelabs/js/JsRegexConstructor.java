package io.karatelabs.js;

class JsRegexConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsRegexConstructor() {
      this.name = "RegExp";
      this.length = 2;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("escape", new JsBuiltinMethod("escape", 1, (ctx, args) -> this.escape(args)), (byte)13);
      this.defineOwn("prototype", JsRegexPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object... args) {
      if (args.length == 0) {
         return new JsRegex();
      } else {
         String pattern = args[0].toString();
         String flags = args.length > 1 ? args[1].toString() : "";
         return new JsRegex(pattern, flags);
      }
   }

   private Object escape(Object[] args) {
      if (!((args.length > 0 ? args[0] : Terms.UNDEFINED) instanceof String str)) {
         throw JsErrorException.typeError("RegExp.escape called on non-string");
      } else {
         StringBuilder out = new StringBuilder(str.length() + 8);
         int i = 0;

         while (i < str.length()) {
            int cp = str.codePointAt(i);
            if (out.length() == 0 && isAsciiDigitOrLetter(cp)) {
               out.append("\\x").append(toHexLowerPad(cp, 2));
            } else {
               encodeForRegExpEscape(out, cp);
            }

            i += Character.charCount(cp);
         }

         return out.toString();
      }
   }

   private static boolean isAsciiDigitOrLetter(int cp) {
      return cp >= 48 && cp <= 57 || cp >= 65 && cp <= 90 || cp >= 97 && cp <= 122;
   }

   private static void encodeForRegExpEscape(StringBuilder out, int cp) {
      if (isSyntaxCharacterOrSolidus(cp)) {
         out.append('\\').appendCodePoint(cp);
      } else {
         char controlEscape = controlEscapeLetter(cp);
         if (controlEscape != 0) {
            out.append('\\').append(controlEscape);
         } else if (!isOtherPunctuator(cp) && !isWhiteSpace(cp) && !isLineTerminator(cp) && !isLoneSurrogate(cp)) {
            out.appendCodePoint(cp);
         } else {
            if (cp <= 255) {
               out.append("\\x").append(toHexLowerPad(cp, 2));
            } else if (cp <= 65535) {
               out.append("\\u").append(toHexLowerPad(cp, 4));
            } else {
               int high = 55296 + (cp - 65536 >> 10);
               int low = 56320 + (cp - 65536 & 1023);
               out.append("\\u").append(toHexLowerPad(high, 4));
               out.append("\\u").append(toHexLowerPad(low, 4));
            }
         }
      }
   }

   private static boolean isSyntaxCharacterOrSolidus(int cp) {
      return switch (cp) {
         case 36, 40, 41, 42, 43, 46, 47, 63, 91, 92, 93, 94, 123, 124, 125 -> true;
         default -> false;
      };
   }

   private static char controlEscapeLetter(int cp) {
      return switch (cp) {
         case 9 -> 't';
         case 10 -> 'n';
         case 11 -> 'v';
         case 12 -> 'f';
         case 13 -> 'r';
         default -> '\u0000';
      };
   }

   private static boolean isOtherPunctuator(int cp) {
      return switch (cp) {
         case 33, 34, 35, 37, 38, 39, 44, 45, 58, 59, 60, 61, 62, 64, 96, 126 -> true;
         default -> false;
      };
   }

   private static boolean isWhiteSpace(int cp) {
      if (cp == 32 || cp == 160 || cp == 65279) {
         return true;
      } else if (cp == 5760) {
         return true;
      } else {
         return cp >= 8192 && cp <= 8202 ? true : cp == 8239 || cp == 8287 || cp == 12288;
      }
   }

   private static boolean isLineTerminator(int cp) {
      return cp == 8232 || cp == 8233;
   }

   private static boolean isLoneSurrogate(int cp) {
      return cp >= 55296 && cp <= 57343;
   }

   private static String toHexLowerPad(int value, int width) {
      String hex = Integer.toHexString(value);
      return hex.length() >= width ? hex : "0".repeat(width - hex.length()) + hex;
   }
}
