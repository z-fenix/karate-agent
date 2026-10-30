package io.karatelabs.ext.robot.recording;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class JavaScriptIdentifierSanitizer {
   private static final Set<String> a = Set.of(
      "break",
      "case",
      "catch",
      "class",
      "const",
      "continue",
      "debugger",
      "default",
      "delete",
      "do",
      "else",
      "enum",
      "export",
      "extends",
      "false",
      "finally",
      "for",
      "function",
      "if",
      "import",
      "in",
      "instanceof",
      "new",
      "null",
      "return",
      "super",
      "switch",
      "this",
      "throw",
      "true",
      "try",
      "typeof",
      "var",
      "void",
      "while",
      "with",
      "yield",
      "let",
      "static",
      "implements",
      "interface",
      "package",
      "private",
      "protected",
      "public",
      "await",
      "undefined",
      "karate",
      "response",
      "responseStatus",
      "responseHeaders",
      "responseCookies",
      "responseTime",
      "request",
      "read",
      "call",
      "auth"
   );
   private final Set<String> b = new HashSet<>();

   String a(String var1) {
      String var2 = c(var1);
      String var3 = var2;

      for (int var4 = 2; a.contains(var3) || this.b.contains(var3); var4++) {
         var3 = var2 + var4;
      }

      this.b.add(var3);
      return var3;
   }

   boolean b(String var1) {
      return !a.contains(var1) && this.b.add(var1);
   }

   static String c(String var0) {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = true;

      for (String var6 : var0.split("[^A-Za-z0-9]+")) {
         if (!var6.isEmpty()) {
            boolean var7 = var6.equals(var6.toUpperCase(Locale.ROOT));
            if (var2) {
               var1.append(var7 ? var6.toLowerCase(Locale.ROOT) : Character.toLowerCase(var6.charAt(0)) + var6.substring(1));
               var2 = false;
            } else {
               String var8 = var7 ? var6.substring(1).toLowerCase(Locale.ROOT) : var6.substring(1);
               var1.append(Character.toUpperCase(var6.charAt(0))).append(var8);
            }
         }
      }

      if (var1.isEmpty()) {
         var1.append("value");
      }

      return Character.isDigit(var1.charAt(0)) ? "_" + var1 : var1.toString();
   }

   static String d(String var0) {
      if (var0.endsWith("ies") && var0.length() > 3) {
         return var0.substring(0, var0.length() - 3) + "y";
      } else if (!var0.endsWith("ses") && !var0.endsWith("xes") && !var0.endsWith("zes") && !var0.endsWith("ches") && !var0.endsWith("shes")) {
         return var0.endsWith("s") && !var0.endsWith("ss") && var0.length() > 1 ? var0.substring(0, var0.length() - 1) : var0;
      } else {
         return var0.substring(0, var0.length() - 2);
      }
   }
}
