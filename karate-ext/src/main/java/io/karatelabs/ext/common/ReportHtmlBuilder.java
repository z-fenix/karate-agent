package io.karatelabs.ext.common;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.parser.JsLexer;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class ReportHtmlBuilder {
   public static final String a = "io/karatelabs/ext/max/web/pub/tailwind.css";
   private static final String b = "(function () {\n  try {\n    var t = localStorage.getItem('karate-theme');\n    if (!t) t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';\n    document.documentElement.setAttribute('data-theme', t);\n    window.addEventListener('storage', function (e) {\n      if (e.key === 'karate-theme' && e.newValue) {\n        document.documentElement.setAttribute('data-theme', e.newValue);\n      }\n    });\n  } catch (e) {\n    document.documentElement.setAttribute('data-theme', 'light');\n  }\n})();";

   private ReportHtmlBuilder() {
   }

   public static String a(String var0, List<String> var1, String var2, Map<String, Object> var3, List<String> var4) {
      StringBuilder var5 = new StringBuilder(
         "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>"
            + b(var0)
            + "</title>\n<script>\n(function () {\n  try {\n    var t = localStorage.getItem('karate-theme');\n    if (!t) t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';\n    document.documentElement.setAttribute('data-theme', t);\n    window.addEventListener('storage', function (e) {\n      if (e.key === 'karate-theme' && e.newValue) {\n        document.documentElement.setAttribute('data-theme', e.newValue);\n      }\n    });\n  } catch (e) {\n    document.documentElement.setAttribute('data-theme', 'light');\n  }\n})();\n</script>\n<style>\n"
      );

      for (String var7 : var1) {
         var5.append(a(var7, a(var7))).append('\n');
      }

      var5.append("</style>\n</head>\n<body>\n<div id=\"app\"></div>\n<script>window.")
         .append(var2)
         .append(" = ")
         .append(c(Json.stringifyStrict(var3)))
         .append(";</script>\n");

      for (String var9 : var4) {
         var5.append("<script>\n").append(c(a(var9, a(var9)))).append("\n</script>\n");
      }

      return var5.append("</body>\n</html>\n").toString();
   }

   public static String a(String var0) {
      try {
         String var2;
         try (InputStream var1 = ReportHtmlBuilder.class.getClassLoader().getResourceAsStream(var0)) {
            if (var1 == null) {
               throw new IllegalStateException("missing bundled resource: " + var0);
            }

            var2 = new String(var1.readAllBytes(), StandardCharsets.UTF_8);
         }

         return var2;
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   static String a(String var0, String var1) {
      if (var0.endsWith(".css")) {
         return e(var1);
      } else {
         return var0.endsWith(".html") ? f(var1) : d(var1);
      }
   }

   private static String d(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());
      int var2 = 0;
      JsLexer var3 = new JsLexer(Resource.text(var0));

      for (Token var4 = var3.nextToken(); var4.type != TokenType.EOF; var4 = var3.nextToken()) {
         if (var4.type == TokenType.L_COMMENT || var4.type == TokenType.B_COMMENT || var4.type == TokenType.G_COMMENT) {
            String var5 = var0.substring(var4.pos, var4.pos + var4.length);
            var1.append(var0, var2, var4.pos).append(var5.indexOf(10) < 0 ? " " : "\n");
            var2 = var4.pos + var4.length;
         }
      }

      return var1.append(var0, var2, var0.length()).toString();
   }

   private static String e(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());
      char var2 = 0;
      int var3 = 0;

      while (var3 < var0.length()) {
         char var4 = var0.charAt(var3);
         if (var2 != 0) {
            var1.append(var4);
            if (var4 == '\\' && var3 + 1 < var0.length()) {
               var1.append(var0.charAt(var3 + 1));
               var3 += 2;
            } else {
               if (var4 == var2) {
                  var2 = 0;
               }

               var3++;
            }
         } else if (var4 != '"' && var4 != '\'') {
            if (var4 == '/' && var3 + 1 < var0.length() && var0.charAt(var3 + 1) == '*') {
               int var5 = var0.indexOf("*/", var3 + 2);
               var5 = var5 < 0 ? var0.length() : var5 + 2;
               var1.append(var0.startsWith("/*!", var3) ? var0.substring(var3, var5) : " ");
               var3 = var5;
            } else {
               var1.append(var4);
               var3++;
            }
         } else {
            var2 = var4;
            var1.append(var4);
            var3++;
         }
      }

      return var1.toString();
   }

   private static String f(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());
      int var2 = 0;

      while (var2 < var0.length()) {
         String var3 = a(var0, var2, "<script") ? "script" : (a(var0, var2, "<style") ? "style" : null);
         if (var3 != null && a(var0, var2 + 1 + var3.length())) {
            int var7 = var0.indexOf(62, var2);
            if (var7 < 0) {
               break;
            }

            int var5 = var7 + 1;
            int var6 = a(var0, "</" + var3, var5);
            var6 = var6 < 0 ? var0.length() : var6;
            var1.append(var0, var2, var5).append(a(var3.equals("style") ? ".css" : ".js", var0.substring(var5, var6)));
            var2 = var6;
         } else if (var0.startsWith("<!--", var2)) {
            int var4 = var0.indexOf("-->", var2 + 4);
            var2 = var4 < 0 ? var0.length() : var4 + 3;
         } else {
            var1.append(var0.charAt(var2));
            var2++;
         }
      }

      return var1.toString();
   }

   private static boolean a(String var0, int var1) {
      return var1 >= var0.length() || Character.isWhitespace(var0.charAt(var1)) || var0.charAt(var1) == '>' || var0.charAt(var1) == '/';
   }

   private static boolean a(String var0, int var1, String var2) {
      return var0.regionMatches(true, var1, var2, 0, var2.length());
   }

   private static int a(String var0, String var1, int var2) {
      for (int var3 = var2; var3 + var1.length() <= var0.length(); var3++) {
         if (var0.regionMatches(true, var3, var1, 0, var1.length())) {
            return var3;
         }
      }

      return -1;
   }

   public static String b(String var0) {
      return var0 == null ? "" : var0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   public static String c(String var0) {
      return var0 == null ? "" : var0.replace("</script", "<\\/script");
   }
}
