package io.karatelabs.ext.postman.convert;

import io.karatelabs.common.StringUtils;
import io.karatelabs.js.JsonParser;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PostmanVariableTranslator {
   static final Pattern a = Pattern.compile("\\{\\{([^{}]+)}}");
   private static final Pattern c = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
   private static final Map<String, String> d = Map.ofEntries(
      Map.entry("$guid", "karate.uuid()"),
      Map.entry("$randomUUID", "karate.uuid()"),
      Map.entry("$timestamp", "karate.faker.timestamp()"),
      Map.entry("$isoTimestamp", "karate.faker.isoTimestamp()"),
      Map.entry("$randomFirstName", "karate.faker.firstName()"),
      Map.entry("$randomLastName", "karate.faker.lastName()"),
      Map.entry("$randomFullName", "karate.faker.fullName()"),
      Map.entry("$randomEmail", "karate.faker.email()"),
      Map.entry("$randomUserName", "karate.faker.userName()"),
      Map.entry("$randomPhoneNumber", "karate.faker.phoneNumber()"),
      Map.entry("$randomCity", "karate.faker.city()"),
      Map.entry("$randomCountry", "karate.faker.country()"),
      Map.entry("$randomStreetAddress", "karate.faker.streetAddress()"),
      Map.entry("$randomInt", "karate.faker.randomInt()"),
      Map.entry("$randomBoolean", "karate.faker.randomBoolean()"),
      Map.entry("$randomColor", "karate.faker.hexColor()"),
      Map.entry("$randomCompanyName", "karate.faker.companyName()"),
      Map.entry("$randomJobTitle", "karate.faker.jobTitle()")
   );
   final Set<String> b = new LinkedHashSet<>();
   private final boolean e;

   PostmanVariableTranslator(boolean var1) {
      this.e = var1;
   }

   static Set<String> a(String var0, Set<String> var1) {
      if (var0 != null) {
         Matcher var2 = a.matcher(var0);

         while (var2.find()) {
            String var3 = var2.group(1).trim();
            if (!var3.startsWith("$")) {
               var1.add(var3);
            }
         }
      }

      return var1;
   }

   static boolean a(String var0) {
      return c.matcher(var0).matches();
   }

   static String b(String var0) {
      StringBuilder var1 = new StringBuilder("'");

      for (char var5 : var0.toCharArray()) {
         switch (var5) {
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '\'':
               var1.append("\\'");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            default:
               var1.append(var5);
         }
      }

      return var1.append('\'').toString();
   }

   String c(String var1) {
      String var2 = var1.trim();
      if (var2.startsWith("$")) {
         String var3 = d.get(var2);
         if (var3 != null) {
            return var3;
         } else {
            this.b.add(var2);
            return "''";
         }
      } else {
         return a(var2) && !this.e ? var2 : "karate.get(" + b(var2) + ")";
      }
   }

   String d(String var1) {
      if (var1 == null) {
         return "''";
      } else {
         Matcher var2 = a.matcher(var1);
         StringBuilder var3 = new StringBuilder();

         int var4;
         for (var4 = 0; var2.find(); var4 = var2.end()) {
            if (var2.start() > var4) {
               a(var3, b(var1.substring(var4, var2.start())));
            }

            a(var3, this.c(var2.group(1)));
         }

         if (var4 < var1.length() || var3.isEmpty()) {
            a(var3, b(var1.substring(var4)));
         }

         return var3.toString();
      }
   }

   private static void a(StringBuilder var0, String var1) {
      if (!var0.isEmpty()) {
         var0.append(" + ");
      }

      var0.append(var1);
   }

   String e(String var1) {
      StringBuilder var2 = new StringBuilder();
      int var3 = 0;
      int var4 = var1.length();

      while (var3 < var4) {
         char var5 = var1.charAt(var3);
         if (var5 == '"') {
            int var13 = a(var1, var3);
            if (var13 < 0) {
               return null;
            }

            String var7 = var1.substring(var3, var13 + 1);
            String var8 = var1.substring(var3 + 1, var13);
            if (a.matcher(var8).find()) {
               String var9;
               try {
                  var9 = (String)JsonParser.parse(var7);
               } catch (RuntimeException var12) {
                  return null;
               }

               var2.append(StringUtils.formatJsonString("#(" + this.d(var9) + ")"));
            } else {
               var2.append(var7);
            }

            var3 = var13 + 1;
         } else if (var5 == '{' && var1.startsWith("{{", var3)) {
            int var6 = var1.indexOf("}}", var3);
            if (var6 < 0) {
               return null;
            }

            var2.append(StringUtils.formatJsonString("#(" + this.c(var1.substring(var3 + 2, var6)) + ")"));
            var3 = var6 + 2;
         } else {
            var2.append(var5);
            var3++;
         }
      }

      try {
         return StringUtils.formatJson(JsonParser.parse(var2.toString()), false, false, false);
      } catch (RuntimeException var11) {
         return null;
      }
   }

   private static int a(String var0, int var1) {
      for (int var2 = var1 + 1; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '\\') {
            var2++;
         } else if (var3 == '"') {
            return var2;
         }
      }

      return -1;
   }
}
