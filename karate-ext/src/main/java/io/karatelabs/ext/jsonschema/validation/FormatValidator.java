package io.karatelabs.ext.jsonschema.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormatValidator {
   private static final Set<String> JSON_SCHEMA_CORE_FORMATS = immutableSortedSet("date-time", "email", "hostname", "ipv4", "ipv6", "uri");
   private static final Set<String> DRAFT07_AND_ASYNC_API_FORMATS = immutableSortedSet(
      "date-time",
      "date",
      "time",
      "email",
      "idn-email",
      "hostname",
      "idn-hostname",
      "ipv4",
      "ipv6",
      "uri",
      "uri-reference",
      "iri",
      "iri-reference",
      "uri-template",
      "json-pointer",
      "relative-json-pointer",
      "regex"
   );
   private static final Set<String> MODERN_JSON_SCHEMA_FORMATS = a(DRAFT07_AND_ASYNC_API_FORMATS, immutableSortedSet(new String[]{"duration", "uuid"}));
   private static final Set<String> OPEN_API_30_FORMATS = immutableSortedSet("int32", "int64", "float", "double", "byte", "binary", "date", "date-time", "password");
   private static final Set<String> OPEN_API_NUMERIC_FORMATS = immutableSortedSet("int32", "int64", "float", "double", "password");
   private static final Set<String> OPEN_API_30_SUPPORTED_FORMATS = a(JSON_SCHEMA_CORE_FORMATS, OPEN_API_30_FORMATS);
   private static final Set<String> OPEN_API_31_SUPPORTED_FORMATS = a(MODERN_JSON_SCHEMA_FORMATS, OPEN_API_NUMERIC_FORMATS);
   private static final BigDecimal FLOAT_MAX_VALUE = new BigDecimal(String.valueOf(Float.MAX_VALUE));
   private static final BigDecimal DOUBLE_MAX_VALUE = new BigDecimal(String.valueOf(Double.MAX_VALUE));
   private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
   private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})(?:\\.\\d+)?(?:[Zz]|([+-])(\\d{2}):(\\d{2}))");
   private static final String DURATION_DATE_PART = "(?:\\d+Y(?:\\d+M(?:\\d+D)?)?|\\d+M(?:\\d+D)?|\\d+D)";
   private static final String DURATION_TIME_PART = "T(?:\\d+H(?:\\d+M(?:\\d+S)?)?|\\d+M(?:\\d+S)?|\\d+S)";
   private static final Pattern DURATION_PATTERN = Pattern.compile(
      "P(?:\\d+W|(?:\\d+Y(?:\\d+M(?:\\d+D)?)?|\\d+M(?:\\d+D)?|\\d+D)(?:T(?:\\d+H(?:\\d+M(?:\\d+S)?)?|\\d+M(?:\\d+S)?|\\d+S))?|T(?:\\d+H(?:\\d+M(?:\\d+S)?)?|\\d+M(?:\\d+S)?|\\d+S))"
   );
   private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
   private static final Pattern HOSTNAME_LABEL_PATTERN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");
   private static final Pattern IPV4_OCTET_PATTERN = Pattern.compile("0|[1-9][0-9]{0,2}");
   private static final Pattern IPV6_HEXTET_PATTERN = Pattern.compile("[0-9A-Fa-f]{1,4}");
   private static final String EMAIL_ATOM_CHARACTERS = "A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~";
   private static final Pattern EMAIL_LOCAL_PART_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~]+");
   private static final Pattern IDN_EMAIL_LOCAL_PART_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~\\x{80}-\\x{10FFFF}]+");
   private static final Pattern URI_TEMPLATE_EXPRESSION_PATTERN = Pattern.compile("[+#./;?&=,!@|]?(?:[A-Za-z0-9_%.]+(?::[1-9][0-9]{0,3}|\\*)?)(?:,[A-Za-z0-9_%.]+(?::[1-9][0-9]{0,3}|\\*)?)*");
   private static final Pattern RELATIVE_JSON_POINTER_PATTERN = Pattern.compile("(0|[1-9][0-9]*)(#|(/.*)?)");

   private FormatValidator() {
   }

   public static Set<String> supportedFormats(SchemaDialect var0) {
      return switch (var0) {
         case DRAFT_07, ASYNCAPI_2 -> DRAFT07_AND_ASYNC_API_FORMATS;
         case V2019_09, V2020_12 -> MODERN_JSON_SCHEMA_FORMATS;
         case OAS_3_1 -> OPEN_API_31_SUPPORTED_FORMATS;
         case OAS_3_0, SWAGGER_2_0 -> OPEN_API_30_SUPPORTED_FORMATS;
      };
   }

   public static boolean isValid(String var0, Object var1) {
      return formatError(var0, var1) == null;
   }

   static String formatError(String var0, Object var1) {
      if (var1 instanceof String var3) {
         return stringFormatError(var0, var3);
      } else {
         return var1 instanceof Number var2 ? numericFormatError(var0, var2) : null;
      }
   }

   private static String stringFormatError(String var0, String var1) {
      return switch (var0) {
         case "date" -> isDate(var1) ? null : "expected an RFC 3339 full-date, e.g. 2026-08-03";
         case "time" -> isTime(var1) ? null : "expected an RFC 3339 full-time incl. an offset, e.g. 08:30:06Z";
         case "date-time" -> isDateTime(var1) ? null : "expected an RFC 3339 date-time, e.g. 2026-08-03T08:30:06Z";
         case "duration" -> isDuration(var1) ? null : "expected an RFC 3339 duration, e.g. P4DT12H30M5S";
         case "email" -> isEmail(var1, false) ? null : "expected an RFC 5321 mailbox, e.g. joe@example.com";
         case "idn-email" -> isEmail(var1, true) ? null : "expected an RFC 6531 mailbox, e.g. δοκιμή@example.com";
         case "hostname" -> isHostname(var1) ? null : "expected an RFC 1123 hostname, e.g. www.example.com";
         case "ipv4" -> isIpv4(var1) ? null : "expected a dotted-quad IPv4 address, e.g. 192.168.0.1";
         case "ipv6" -> isIpv6(var1) ? null : "expected an RFC 4291 IPv6 address, e.g. ::1";
         case "uri" -> io.karatelabs.ext.jsonschema.validation.UriValidator.isAbsoluteUri(var1, false) ? null : "expected an absolute RFC 3986 URI (a scheme is required)";
         case "uri-reference" -> io.karatelabs.ext.jsonschema.validation.UriValidator.isUriReference(var1, false) ? null : "expected an RFC 3986 URI-reference";
         case "iri" -> io.karatelabs.ext.jsonschema.validation.UriValidator.isAbsoluteUri(var1, true) ? null : "expected an absolute RFC 3987 IRI (a scheme is required)";
         case "iri-reference" -> io.karatelabs.ext.jsonschema.validation.UriValidator.isUriReference(var1, true) ? null : "expected an RFC 3987 IRI-reference";
         case "uuid" -> UUID_PATTERN.matcher(var1).matches() ? null : "expected an RFC 4122 UUID, e.g. 2eb8aa08-aa98-11ea-b4aa-73b441d16380";
         case "uri-template" -> isUriTemplate(var1) ? null : "expected an RFC 6570 URI-template, e.g. /users/{id}";
         case "json-pointer" -> isJsonPointer(var1) ? null : "expected an RFC 6901 JSON pointer, e.g. /a/0/b";
         case "relative-json-pointer" -> isRelativeJsonPointer(var1) ? null : "expected an RFC 6901 relative JSON pointer, e.g. 1/b";
         case "regex" -> isEcmaRegex(var1) ? null : "expected a valid ECMA-262 regular expression";
         case "byte" -> isBase64(var1) ? null : "expected base64-encoded content (OAS 3.0 'byte')";
         default -> null;
      };
   }

   private static String numericFormatError(String var0, Number var1) {
      BigDecimal var2 = io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var1);
      if (var2 == null) {
         return null;
      } else {
         return switch (var0) {
            case "int32" -> isIntegral(var2) && isWithinInclusiveRange(var2, -2147483648L, 2147483647L) ? null : "expected a signed 32-bit integer";
            case "int64" -> isIntegral(var2) && isWithinInclusiveRange(var2, Long.MIN_VALUE, Long.MAX_VALUE) ? null : "expected a signed 64-bit integer";
            case "float" -> var2.abs().compareTo(FLOAT_MAX_VALUE) <= 0 ? null : "expected a value representable as a 32-bit float";
            case "double" -> var2.abs().compareTo(DOUBLE_MAX_VALUE) <= 0 ? null : "expected a value representable as a 64-bit double";
            default -> null;
         };
      }
   }

   private static boolean isIntegral(BigDecimal var0) {
      return var0.stripTrailingZeros().scale() <= 0;
   }

   private static boolean isWithinInclusiveRange(BigDecimal var0, long var1, long var3) {
      return var0.compareTo(BigDecimal.valueOf(var1)) >= 0 && var0.compareTo(BigDecimal.valueOf(var3)) <= 0;
   }

   static boolean isDate(String var0) {
      Matcher var1 = DATE_PATTERN.matcher(var0);
      if (!var1.matches()) {
         return false;
      } else {
         int var2 = Integer.parseInt(var1.group(1));
         int var3 = Integer.parseInt(var1.group(2));
         int var4 = Integer.parseInt(var1.group(3));
         return var3 >= 1 && var3 <= 12 && var4 >= 1 && var4 <= daysInMonth(var2, var3);
      }
   }

   private static int daysInMonth(int var0, int var1) {
      return switch (var1) {
         case 2 -> (var0 % 4 != 0 || var0 % 100 == 0) && var0 % 400 != 0 ? 28 : 29;
         default -> 31;
         case 4, 6, 9, 11 -> 30;
      };
   }

   static boolean isTime(String var0) {
      Matcher var1 = TIME_PATTERN.matcher(var0);
      if (!var1.matches()) {
         return false;
      } else {
         int var2 = Integer.parseInt(var1.group(1));
         int var3 = Integer.parseInt(var1.group(2));
         int var4 = Integer.parseInt(var1.group(3));
         if (var2 <= 23 && var3 <= 59 && var4 <= 60) {
            int var5 = 0;
            if (var1.group(4) != null) {
               int var6 = Integer.parseInt(var1.group(5));
               int var7 = Integer.parseInt(var1.group(6));
               if (var6 > 23 || var7 > 59) {
                  return false;
               }

               var5 = (var6 * 60 + var7) * ("-".equals(var1.group(4)) ? -1 : 1);
            }

            return var4 != 60 || Math.floorMod(var2 * 60 + var3 - var5, 1440) == 1439;
         } else {
            return false;
         }
      }
   }

   static boolean isDateTime(String var0) {
      int var1 = var0.indexOf(84) < 0 ? var0.indexOf(116) : var0.indexOf(84);
      return var1 > 0 && isDate(var0.substring(0, var1)) && isTime(var0.substring(var1 + 1));
   }

   static boolean isDuration(String var0) {
      return DURATION_PATTERN.matcher(var0).matches();
   }

   static boolean isHostname(String var0) {
      if (!var0.isEmpty() && var0.length() <= 253) {
         for (String var4 : var0.split("\\.", -1)) {
            if (!HOSTNAME_LABEL_PATTERN.matcher(var4).matches()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static boolean isIpv4(String var0) {
      String[] var1 = var0.split("\\.", -1);
      if (var1.length != 4) {
         return false;
      } else {
         for (String var5 : var1) {
            if (!IPV4_OCTET_PATTERN.matcher(var5).matches() || Integer.parseInt(var5) > 255) {
               return false;
            }
         }

         return true;
      }
   }

   static boolean isIpv6(String var0) {
      String var1 = var0;
      String var2 = null;
      int var3 = var0.indexOf("::");
      if (var3 >= 0) {
         if (var0.indexOf("::", var3 + 1) >= 0) {
            return false;
         }

         var1 = var0.substring(0, var3);
         var2 = var0.substring(var3 + 2);
      }

      var var4 = splitIpv6(var1);
      List var5 = var2 == null ? List.of() : splitIpv6(var2);
      if (var4 != null && var5 != null) {
         var var6 = new ArrayList<>(var4);
         var6.addAll(var5);
         int var7 = var6.size();
         List var8 = var2 == null ? var4 : var5;
         if (!var8.isEmpty() && ((String)var6.get(var7 - 1)).indexOf(46) >= 0) {
            if (!isIpv4((String)var6.get(var7 - 1))) {
               return false;
            }

            var6.remove(var7 - 1);
            var7++;
         }

         for (String var10 : (Iterable<String>) (Iterable<?>) (var6)) {
            if (!IPV6_HEXTET_PATTERN.matcher(var10).matches()) {
               return false;
            }
         }

         return var3 >= 0 ? var7 <= 7 : var7 == 8;
      } else {
         return false;
      }
   }

   private static List<String> splitIpv6(String var0) {
      if (var0.isEmpty()) {
         return List.of();
      } else {
         var var1 = List.of(var0.split(":", -1));

         for (String var3 : (Iterable<String>) (Iterable<?>) (var1)) {
            if (var3.isEmpty()) {
               return null;
            }
         }

         return var1;
      }
   }

   static boolean isEmail(String var0, boolean var1) {
      int var2 = findLastUnquotedAt(var0);
      return var2 > 0 && var2 != var0.length() - 1 ? isEmailLocalPart(var0.substring(0, var2), var1) && isEmailDomain(var0.substring(var2 + 1), var1) : false;
   }

   private static int findLastUnquotedAt(String var0) {
      boolean var1 = false;
      int var2 = -1;

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\' && var1) {
            var3++;
         } else if (var4 == '"') {
            var1 = !var1;
         } else if (var4 == '@' && !var1) {
            var2 = var3;
         }
      }

      return var2;
   }

   private static boolean isEmailLocalPart(String var0, boolean var1) {
      if (var0.startsWith("\"")) {
         return isQuotedEmailLocalPart(var0);
      } else {
         Pattern var2 = var1 ? IDN_EMAIL_LOCAL_PART_PATTERN : EMAIL_LOCAL_PART_PATTERN;

         for (String var6 : var0.split("\\.", -1)) {
            if (!var2.matcher(var6).matches()) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isQuotedEmailLocalPart(String var0) {
      if (var0.length() >= 2 && var0.indexOf(10) < 0) {
         for (int var1 = 1; var1 < var0.length(); var1++) {
            char var2 = var0.charAt(var1);
            if (var2 == '\\') {
               var1++;
            } else if (var2 == '"') {
               return var1 == var0.length() - 1;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean isEmailDomain(String var0, boolean var1) {
      if (var0.startsWith("[")) {
         if (!var0.endsWith("]")) {
            return false;
         } else {
            String var8 = var0.substring(1, var0.length() - 1);
            return var8.startsWith("IPv6:") ? isIpv6(var8.substring(5)) : isIpv4(var8);
         }
      } else if (!var1) {
         return isHostname(var0);
      } else {
         for (String var5 : var0.split("\\.", -1)) {
            if (var5.isEmpty() || var5.startsWith("-") || var5.endsWith("-")) {
               return false;
            }

            for (int var6 = 0; var6 < var5.length(); var6++) {
               char var7 = var5.charAt(var6);
               if (var7 < 128 && !Character.isLetterOrDigit(var7) && var7 != '-') {
                  return false;
               }
            }
         }

         return true;
      }
   }

   static boolean isJsonPointer(String var0) {
      if (var0.isEmpty()) {
         return true;
      } else if (var0.charAt(0) != '/') {
         return false;
      } else {
         for (int var1 = 0; var1 < var0.length(); var1++) {
            if (var0.charAt(var1) == '~' && (var1 + 1 >= var0.length() || var0.charAt(var1 + 1) != '0' && var0.charAt(var1 + 1) != '1')) {
               return false;
            }
         }

         return true;
      }
   }

   static boolean isRelativeJsonPointer(String var0) {
      Matcher var1 = RELATIVE_JSON_POINTER_PATTERN.matcher(var0);
      return var1.matches() && (var1.group(3) == null || isJsonPointer(var1.group(3)));
   }

   static boolean isUriTemplate(String var0) {
      StringBuilder var1 = new StringBuilder();
      int var2 = 0;

      while (var2 < var0.length()) {
         char var3 = var0.charAt(var2);
         if (var3 == '}') {
            return false;
         }

         if (var3 == '{') {
            int var4 = var0.indexOf(125, var2);
            if (var4 < 0 || !URI_TEMPLATE_EXPRESSION_PATTERN.matcher(var0.substring(var2 + 1, var4)).matches()) {
               return false;
            }

            var2 = var4 + 1;
         } else {
            var1.append(var3);
            var2++;
         }
      }

      return io.karatelabs.ext.jsonschema.validation.UriValidator.isUriReference(var1.toString(), true);
   }

   static boolean isEcmaRegex(String var0) {
      return io.karatelabs.ext.jsonschema.validation.EcmaRegex.isValidEcmaPattern(var0);
   }

   static boolean isBase64(String var0) {
      try {
         Base64.getDecoder().decode(var0);
         return true;
      } catch (IllegalArgumentException var2) {
         return false;
      }
   }

   private static Set<String> immutableSortedSet(String... var0) {
      return Collections.unmodifiableSet(new TreeSet<>(List.of(var0)));
   }

   private static Set<String> a(Set<String> var0, Set<String> var1) {
      var var2 = new TreeSet<>(var0);
      var2.addAll(var1);
      return Collections.unmodifiableSet(var2);
   }
}
