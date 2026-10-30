package io.karatelabs.ext.jsonschema.validation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class EcmaRegex {
   private static final String ECMA_WHITESPACE_CLASS = " \\t\\n\\x0B\\f\\r\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff";
   private static final String JVM_ONLY_ESCAPE_CHARACTERS = "QEAZzGhHVRXaeN";
   private static final Map<String, Pattern> compiledPatternCache = new ConcurrentHashMap<>();

   private EcmaRegex() {
   }

   public static Pattern compile(String var0) {
      Pattern var1 = compiledPatternCache.get(var0);
      if (var1 == null) {
         String var2 = translateEcmaPattern(var0);

         try {
            var1 = Pattern.compile(var2);
         } catch (PatternSyntaxException var4) {
            throw new ValidationException("pattern is not translatable to a JVM regex: '" + var0 + "' — " + var4.getMessage());
         }

         compiledPatternCache.put(var0, var1);
      }

      return var1;
   }

   public static boolean find(String var0, String var1) {
      return compile(var0).matcher(var1).find();
   }

   static boolean isValidEcmaPattern(String var0) {
      try {
         Pattern.compile(translateEcmaPattern(var0));
         return true;
      } catch (RuntimeException var2) {
         return false;
      }
   }

   static String translateEcmaPattern(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 16);
      boolean var2 = false;
      int var3 = 0;
      int var4 = var0.length();

      while (var3 < var4) {
         char var5 = var0.charAt(var3);
         if (var5 == '\\') {
            if (var3 + 1 >= var4) {
               throw new ValidationException("pattern ends with a dangling backslash: '" + var0 + "'");
            }

            char var8 = var0.charAt(var3 + 1);
            if (var8 == 's') {
               var1.append(
                  var2
                     ? " \\t\\n\\x0B\\f\\r\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff"
                     : "[ \\t\\n\\x0B\\f\\r\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]"
               );
            } else if (var8 == 'S') {
               if (var2) {
                  throw new ValidationException("pattern uses '\\S' inside a character class, which cannot be translated to ECMA-262 whitespace semantics: '" + var0 + "'");
               }

               var1.append("[^").append(" \\t\\n\\x0B\\f\\r\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff").append("]");
            } else {
               if ("QEAZzGhHVRXaeN".indexOf(var8) >= 0) {
                  throw new ValidationException("pattern uses the JVM-only '\\" + var8 + "' construct, which ECMA-262 does not define: '" + var0 + "'");
               }

               if (var8 == 'v') {
                  var1.append("\\x0B");
               } else {
                  if (var8 == 'c' && var3 + 2 < var4 && Character.isLetter(var0.charAt(var3 + 2))) {
                     var1.append(String.format("\\x{%02x}", var0.charAt(var3 + 2) % ' '));
                     var3 += 3;
                     continue;
                  }

                  if ((var8 == 'p' || var8 == 'P') && var3 + 2 < var4 && var0.charAt(var3 + 2) == '{') {
                     int var7 = var0.indexOf(125, var3 + 3);
                     if (var7 < 0) {
                        throw new ValidationException("pattern has an unterminated \\" + var8 + "{…}: '" + var0 + "'");
                     }

                     var1.append(var5).append(var8).append('{').append(translateUnicodeProperty(var0.substring(var3 + 3, var7))).append('}');
                     var3 = var7 + 1;
                     continue;
                  }

                  var1.append(var5).append(var8);
               }
            }

            var3 += 2;
         } else if (var2) {
            if (var5 == ']') {
               var2 = false;
            }

            var1.append(var5);
            var3++;
         } else {
            switch (var5) {
               case '$':
                  var1.append("\\z");
                  var3++;
                  break;
               case '[':
                  if (var3 + 1 < var4 && var0.charAt(var3 + 1) == ']') {
                     var1.append("(?!)");
                     var3 += 2;
                  } else {
                     if (var3 + 2 < var4 && var0.charAt(var3 + 1) == '^' && var0.charAt(var3 + 2) == ']') {
                        var1.append("[\\s\\S]");
                        var3 += 3;
                        continue;
                     }

                     var2 = true;
                     var1.append(var5);
                     var3++;
                  }
                  break;
               case '^':
                  var1.append("\\A");
                  var3++;
                  break;
               case '{':
                  int var6 = findQuantifierEnd(var0, var3);
                  if (var6 < 0) {
                     var1.append("\\{");
                     var3++;
                  } else {
                     var1.append(var0, var3, var6 + 1);
                     var3 = var6 + 1;
                  }
                  break;
               case '}':
                  var1.append("\\}");
                  var3++;
                  break;
               default:
                  var1.append(var5);
                  var3++;
            }
         }
      }

      if (var2) {
         throw new ValidationException("pattern has an unterminated character class: '" + var0 + "'");
      } else {
         return var1.toString();
      }
   }

   private static String translateUnicodeProperty(String var0) {
      String var1 = var0.trim();
      int var2 = var1.indexOf(61);
      if (var2 > 0) {
         String var3 = var1.substring(0, var2).trim();
         String var4 = var1.substring(var2 + 1).trim();
         if (var3.equals("Script") || var3.equals("sc") || var3.equals("Script_Extensions") || var3.equals("scx")) {
            return "Is" + var4;
         } else {
            return !var3.equals("General_Category") && !var3.equals("gc") ? var1 : normalizeUnicodeCategory(var4);
         }
      } else {
         return normalizeUnicodeCategory(var1);
      }
   }

   private static String normalizeUnicodeCategory(String var0) {
      return switch (var0) {
         case "Letter" -> "L";
         case "Uppercase_Letter" -> "Lu";
         case "Lowercase_Letter" -> "Ll";
         case "Titlecase_Letter" -> "Lt";
         case "Modifier_Letter" -> "Lm";
         case "Other_Letter" -> "Lo";
         case "Mark", "Combining_Mark" -> "M";
         case "Nonspacing_Mark" -> "Mn";
         case "Spacing_Mark" -> "Mc";
         case "Enclosing_Mark" -> "Me";
         case "Number" -> "N";
         case "Decimal_Number", "digit" -> "Nd";
         case "Letter_Number" -> "Nl";
         case "Other_Number" -> "No";
         case "Punctuation", "punct" -> "P";
         case "Symbol" -> "S";
         case "Math_Symbol" -> "Sm";
         case "Currency_Symbol" -> "Sc";
         case "Modifier_Symbol" -> "Sk";
         case "Other_Symbol" -> "So";
         case "Separator" -> "Z";
         case "Space_Separator" -> "Zs";
         case "Line_Separator" -> "Zl";
         case "Paragraph_Separator" -> "Zp";
         case "Other" -> "C";
         case "Control", "cntrl" -> "Cc";
         case "Format" -> "Cf";
         case "Surrogate" -> "Cs";
         case "Private_Use" -> "Co";
         case "Unassigned" -> "Cn";
         case "Connector_Punctuation" -> "Pc";
         case "Dash_Punctuation" -> "Pd";
         case "Open_Punctuation" -> "Ps";
         case "Close_Punctuation" -> "Pe";
         case "Initial_Punctuation" -> "Pi";
         case "Final_Punctuation" -> "Pf";
         case "Other_Punctuation" -> "Po";
         default -> var0;
      };
   }

   private static int findQuantifierEnd(String var0, int var1) {
      int var2 = var1 + 1;

      int var3;
      for (var3 = 0; var2 < var0.length() && Character.isDigit(var0.charAt(var2)); var3++) {
         var2++;
      }

      if (var3 == 0) {
         return -1;
      } else {
         if (var2 < var0.length() && var0.charAt(var2) == ',') {
            var2++;

            while (var2 < var0.length() && Character.isDigit(var0.charAt(var2))) {
               var2++;
            }
         }

         return var2 < var0.length() && var0.charAt(var2) == 125 ? var2 : -1;
      }
   }
}
