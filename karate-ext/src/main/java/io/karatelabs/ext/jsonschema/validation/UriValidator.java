package io.karatelabs.ext.jsonschema.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UriValidator {
   private static final Pattern URI_REFERENCE_PATTERN = Pattern.compile("(?s)(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?");
   private static final Pattern SCHEME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+\\-.]*");
   private static final Pattern PORT_PATTERN = Pattern.compile("[0-9]*");
   private static final Pattern IP_FUTURE_PATTERN = Pattern.compile("[vV][0-9A-Fa-f]+\\.[A-Za-z0-9\\-._~!$&'()*+,;=:]+");
   private static final String SUB_DELIMITERS = "!$&'()*+,;=";

   private UriValidator() {
   }

   static boolean isAbsoluteUri(String var0, boolean var1) {
      Matcher var2 = URI_REFERENCE_PATTERN.matcher(var0);
      return var2.matches() && var2.group(1) != null && validateUriParts(var2, var1);
   }

   static boolean isUriReference(String var0, boolean var1) {
      Matcher var2 = URI_REFERENCE_PATTERN.matcher(var0);
      return var2.matches() && validateUriParts(var2, var1);
   }

   private static boolean validateUriParts(Matcher var0, boolean var1) {
      String var2 = var0.group(1);
      String var3 = var0.group(2);
      String var4 = var0.group(3) == null ? "" : var0.group(3);
      String var5 = var0.group(4);
      String var6 = var0.group(5);
      if (var2 != null && !SCHEME_PATTERN.matcher(var2).matches()) {
         return false;
      } else if (var3 != null && !isValidAuthority(var3, var1)) {
         return false;
      } else if (!containsOnlyAllowedUriCharacters(var4, ":@/", var1)) {
         return false;
      } else if (var3 != null ? var4.isEmpty() || var4.startsWith("/") : !var4.startsWith("//")) {
         if (var2 == null && var3 == null && !var4.startsWith("/")) {
            int var7 = var4.indexOf(47);
            if ((var7 < 0 ? var4 : var4.substring(0, var7)).indexOf(58) >= 0) {
               return false;
            }
         }

         return (var5 == null || containsOnlyAllowedUriCharacters(var5, ":@/?", var1)) && (var6 == null || containsOnlyAllowedUriCharacters(var6, ":@/?", var1));
      } else {
         return false;
      }
   }

   private static boolean isValidAuthority(String var0, boolean var1) {
      String var2 = var0;
      int var3 = var0.lastIndexOf(64);
      if (var3 >= 0) {
         if (!containsOnlyAllowedUriCharacters(var0.substring(0, var3), ":", var1)) {
            return false;
         }

         var2 = var0.substring(var3 + 1);
      }

      if (var2.startsWith("[")) {
         int var6 = var2.indexOf(93);
         if (var6 < 0) {
            return false;
         } else {
            String var7 = var2.substring(1, var6);
            return !io.karatelabs.ext.jsonschema.validation.FormatValidator.isIpv6(var7) && !IP_FUTURE_PATTERN.matcher(var7).matches() ? false : isValidPortSuffix(var2.substring(var6 + 1));
         }
      } else {
         int var4 = var2.lastIndexOf(58);
         String var5 = var4 < 0 ? var2 : var2.substring(0, var4);
         return containsOnlyAllowedUriCharacters(var5, "", var1) && (var4 < 0 || isValidPortSuffix(var2.substring(var4)));
      }
   }

   private static boolean isValidPortSuffix(String var0) {
      return var0.isEmpty() || var0.charAt(0) == ':' && PORT_PATTERN.matcher(var0.substring(1)).matches();
   }

   private static boolean containsOnlyAllowedUriCharacters(String var0, String var1, boolean var2) {
      int var3 = 0;

      while (var3 < var0.length()) {
         char var4 = var0.charAt(var3);
         if (var4 == '%') {
            if (var3 + 2 >= var0.length() || !isHexDigit(var0.charAt(var3 + 1)) || !isHexDigit(var0.charAt(var3 + 2))) {
               return false;
            }

            var3 += 3;
         } else {
            int var5 = var0.codePointAt(var3);
            if (!isUnreservedCodePoint(var5) && "!$&'()*+,;=".indexOf(var5) < 0 && var1.indexOf(var5) < 0 && (!var2 || !isAllowedUnicodeCodePoint(var5))) {
               return false;
            }

            var3 += Character.charCount(var5);
         }
      }

      return true;
   }

   private static boolean isHexDigit(char var0) {
      return var0 >= '0' && var0 <= '9' || var0 >= 'a' && var0 <= 'f' || var0 >= 'A' && var0 <= 'F';
   }

   private static boolean isUnreservedCodePoint(int var0) {
      return var0 >= 65 && var0 <= 90 || var0 >= 97 && var0 <= 122 || var0 >= 48 && var0 <= 57 || var0 == 45 || var0 == 46 || var0 == 95 || var0 == 126;
   }

   private static boolean isAllowedUnicodeCodePoint(int var0) {
      if (var0 < 160) {
         return false;
      } else if (var0 > 55295 && (var0 < 57344 || var0 > 64975) && (var0 < 65008 || var0 > 65519)) {
         if (var0 >= 65536 && var0 <= 1114109) {
            int var1 = var0 & 65535;
            return var1 <= 65533;
         } else {
            return false;
         }
      } else {
         return true;
      }
   }
}
