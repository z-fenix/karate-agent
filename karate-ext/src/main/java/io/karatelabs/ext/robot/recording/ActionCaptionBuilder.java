package io.karatelabs.ext.robot.recording;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ActionCaptionBuilder {
   private static final String a = "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";
   private static final String b = "[^\\n\\r\\u2028\\u2029]";
   private static final Pattern c = Pattern.compile("[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+");
   private static final Pattern d = Pattern.compile(
      "^[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+|[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+$"
   );
   private static final Pattern e = Pattern.compile("^[a-z][a-z0-9+.-]*://", 2);
   private static final Pattern f = Pattern.compile("[?#][^\\n\\r\\u2028\\u2029]*$");
   private static final Pattern g = Pattern.compile(
      "^\\{[^}]*\\}[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]*([^\\n\\r\\u2028\\u2029]+)\\z"
   );

   private ActionCaptionBuilder() {
   }

   static String a(Map<String, Object> var0, boolean var1) {
      if (var1) {
         return "Sign in";
      } else if (var0 == null) {
         return "When the page opened";
      } else {
         String var2 = a(var0);
         return "After you " + Character.toLowerCase(var2.charAt(0)) + var2.substring(1);
      }
   }

   static String b(Map<String, Object> var0, boolean var1) {
      if (var1) {
         return "Sign in";
      } else if (var0 == null) {
         return "Page opened";
      } else {
         String var2 = b(var0.get("kind")) ? c(var0.get("kind")) : "";
         String var3 = b(var0.get("name")) ? b(c(var0.get("name"))) : "";
         if (var3.isEmpty() || !var2.equals("click") && !var2.isEmpty()) {
            return var2.equals("submit") ? "Submitted the form" : a(var0);
         } else {
            return "“" + a(var3, 40) + "”";
         }
      }
   }

   static String a(List<String> var0) {
      StringBuilder var1 = new StringBuilder();
      int var2 = 0;

      for (String var4 : var0) {
         String var5 = var2 != 0 && !var4.startsWith("“") ? Character.toLowerCase(var4.charAt(0)) + var4.substring(1) : var4;
         String var6 = var2 == 0 ? var5 : var1 + ", " + var5;
         if (var2 == 6 || var2 > 0 && (var6 + ", …").length() > 80) {
            break;
         }

         var1.setLength(0);
         var1.append(var6);
         var2++;
      }

      return var2 < var0.size() ? var1 + ", …" : var1.toString();
   }

   static String a(Map<String, Object> var0) {
      String var1 = b(var0.get("kind")) ? c(var0.get("kind")) : "action";
      String var2 = b(var0.get("name")) ? b(c(var0.get("name"))) : "";
      String var3 = !var2.isEmpty() ? "“" + a(var2, 60) + "”" : (b(var0.get("locator")) ? a(var0.get("locator")) : "the page");
      Object var4 = var0.get("value");

      return switch (var1) {
         case "nav", "navigate", "goto" -> "Opened " + a(a(c(b(var0.get("url")) ? var0.get("url") : (b(var4) ? var4 : ""))), 90);
         case "type", "input", "fill" -> "Typed into " + var3;
         case "select" -> "Chose " + (var4 != null ? "“" + a(c(var4), 40) + "” in " : "an option in ") + var3;
         case "check" -> "Ticked " + var3;
         case "uncheck" -> "Unticked " + var3;
         case "upload" -> "Uploaded a file to " + var3;
         case "press", "key" -> "Pressed " + a(c(b(var4) ? var4 : (b(var0.get("key")) ? var0.get("key") : "a key")), 30);
         case "hover" -> "Pointed at " + var3;
         case "submit" -> "Submitted " + (var2.isEmpty() && !b(var0.get("locator")) ? "a form" : var3);
         case "scroll" -> "Scrolled the page";
         default -> "Clicked " + var3;
      };
   }

   private static String a(String var0) {
      String var1 = f.matcher(e.matcher(var0).replaceFirst("")).replaceFirst("");
      return var1.indexOf(47) == var1.length() - 1 ? var1.substring(0, Math.max(0, var1.length() - 1)) : var1;
   }

   private static String a(Object var0) {
      String var1 = b(c(var0));
      Matcher var2 = g.matcher(var1);
      return "“" + a(var2.find() ? var2.group(1) : var1, 60) + "”";
   }

   private static String a(String var0, int var1) {
      String var2 = b(c.matcher(var0).replaceAll(" "));
      return var2.length() > var1 ? var2.substring(0, var1 - 1) + "…" : var2;
   }

   private static String b(String var0) {
      return d.matcher(var0).replaceAll("");
   }

   private static boolean b(Object var0) {
      if (var0 == null || var0 instanceof Boolean var1 && !var1) {
         return false;
      } else if (var0 instanceof Number var5) {
         double var2 = var5.doubleValue();
         return var2 != 0.0 && !Double.isNaN(var2);
      } else {
         return !(var0 instanceof String var4 && var4.isEmpty());
      }
   }

   private static String c(Object var0) {
      return var0 instanceof Number var1 && var1.doubleValue() == Math.rint(var1.doubleValue()) && !Double.isInfinite(var1.doubleValue())
         ? String.valueOf(var1.longValue())
         : String.valueOf(var0);
   }
}
