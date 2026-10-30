package io.karatelabs.ext.coverage;

import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.w3c.dom.Node;

public record AssertionStrengthSummary(int component1, double b, boolean c, boolean d) {
   public static final double a = -1.0;
   private static final io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore f = new io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore(0.5, false);
   private static final io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore g = new io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore(1.0, false);
   private static final io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore h = new io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore(1.0, true);
   private static final Set<String> i = Set.of("equals", "contains", "containsOnly", "containsDeep", "containsOnlyDeep", "within", "jsonSchema", "notContains");
   private static final Set<String> j = Set.of(
      "required",
      "enum",
      "const",
      "pattern",
      "minimum",
      "maximum",
      "exclusiveMinimum",
      "exclusiveMaximum",
      "minLength",
      "maxLength",
      "minItems",
      "maxItems",
      "format"
   );


   public static io.karatelabs.ext.coverage.AssertionStrengthSummary a(List<Map<String, Object>> var0, boolean var1) {
      double var2 = 0.0;
      boolean var4 = false;
      boolean var5 = false;

      for (Map var7 : var0) {
         io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore var8 = a(var7);
         var2 = Math.max(var2, var8.a);
         var4 |= var8.b;
         var5 |= b(var7);
      }

      return new io.karatelabs.ext.coverage.AssertionStrengthSummary(var0.size(), var1 ? var2 : -1.0, var4, var5);
   }

   private static io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore a(Map<String, Object> var0) {
      if (!var0.containsKey("expected")) {
         return f;
      } else {
         String var1 = var0.get("verb") == null ? "" : String.valueOf(var0.get("verb"));
         Object var2 = var0.get("expected");
         if (!a(var1, var2)) {
            return f;
         } else if (a(var2)) {
            return f;
         } else {
            return "jsonSchema".equals(a(var1)) ? c(var2) : a(var2, var1);
         }
      }
   }

   private static boolean a(Object var0) {
      if (var0 instanceof Node) {
         return true;
      } else if (var0 instanceof String var1) {
         String var2 = var1.trim();
         if (StringUtils.isXml(var2) && var2.endsWith(">")) {
            try {
               Xml.toXmlDoc(var2);
               return true;
            } catch (RuntimeException var4) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static String a(String var0) {
      if ("each".equals(var0)) {
         return "equals";
      } else {
         return var0.length() > 4 && var0.startsWith("each") ? Character.toLowerCase(var0.charAt(4)) + var0.substring(5) : var0;
      }
   }

   private static boolean a(String var0, Object var1) {
      String var2 = a(var0);
      return i.contains(var2) ? true : ("containsAny".equals(var2) || "containsAnyDeep".equals(var2)) && b(var1) == 1;
   }

   private static int b(Object var0) {
      if (var0 instanceof Collection var2) {
         return var2.size();
      } else {
         return var0 instanceof Map var1 ? var1.size() : 1;
      }
   }

   private static boolean b(String var0) {
      String var1 = a(var0);
      return "notContains".equals(var1) || var1.startsWith("contains") && !var1.startsWith("containsOnly");
   }

   private static io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore c(Object var0) {
      boolean[] var1 = new boolean[2];
      a(var0, var1);
      return !var1[0] && !var1[1] ? f : new io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore(1.0, var1[1]);
   }

   private static void a(Object var0, boolean[] var1) {
      if (var0 instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var5 = String.valueOf(var4.getKey());
            Object var6 = var4.getValue();
            if ("const".equals(var5)) {
               var1[0] = true;
            } else if (j.contains(var5)) {
               var1[0] |= !d(var6);
            } else if ("properties".equals(var5) && var6 instanceof Map var7) {
               var1[1] |= var7.size() >= 2;
               var7.values().forEach(var1x -> a(var1x, var1));
            } else if ("items".equals(var5)) {
               if (var6 instanceof Collection var9) {
                  var9.forEach(var1x -> a(var1x, var1));
               } else {
                  a(var6, var1);
               }
            } else if (var6 instanceof Collection var8 && ("allOf".equals(var5) || "anyOf".equals(var5) || "oneOf".equals(var5))) {
               var8.forEach(var1x -> a(var1x, var1));
            }
         }
      }
   }

   private static boolean d(Object var0) {
      if (var0 == null) {
         return true;
      } else if (var0 instanceof Collection var3) {
         return var3.isEmpty();
      } else {
         return var0 instanceof Map var2 ? var2.isEmpty() : var0 instanceof String var1 && var1.isBlank();
      }
   }

   private static io.karatelabs.ext.coverage.AssertionStrengthSummary.AssertionScore a(Object var0, String var1) {
      if (var0 instanceof String var5) {
         return c(var5) ? g : f;
      } else if (!(var0 instanceof Map) && !(var0 instanceof Collection)) {
         return g;
      } else {
         boolean var2 = !b(var1);
         if (b(var0) == 0) {
            return var2 ? g : f;
         } else {
            int var3 = f(var0);
            int var4 = "notContains".equals(a(var1)) ? 1 : 2;
            if (var3 >= var4) {
               return h;
            } else {
               return a(var0, var2) ? g : f;
            }
         }
      }
   }

   private static boolean e(Object var0) {
      return !(var0 instanceof String var1) ? true : !var1.startsWith("##") && !var1.startsWith("#ignore") && !var1.startsWith("#notpresent");
   }

   private static int f(Object var0) {
      if (var0 instanceof Map var5) {
         int var6 = 0;

         for (Object var8 : var5.values()) {
            var6 += f(var8);
         }

         return var5.isEmpty() ? 1 : var6;
      } else if (!(var0 instanceof Collection var1)) {
         return e(var0) ? 1 : 0;
      } else {
         int var2 = 0;

         for (Object var4 : var1) {
            var2 += f(var4);
         }

         return var1.isEmpty() ? 1 : var2;
      }
   }

   private static boolean a(Object var0, boolean var1) {
      if (var0 instanceof Map var6) {
         if (var6.isEmpty()) {
            return var1;
         } else {
            for (Object var8 : var6.values()) {
               if (a(var8, var1)) {
                  return true;
               }
            }

            return false;
         }
      } else if (var0 instanceof Collection var5) {
         if (var5.isEmpty()) {
            return var1;
         } else {
            for (Object var4 : var5) {
               if (a(var4, var1)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return var0 instanceof String var2 ? c(var2) : true;
      }
   }

   private static boolean c(String var0) {
      if (!var0.startsWith("#")) {
         return true;
      } else if (var0.startsWith("##") || var0.startsWith("#?")) {
         return false;
      } else if (var0.startsWith("#(") || var0.startsWith("#regex") || var0.startsWith("#uuid")) {
         return true;
      } else if (!var0.startsWith("#[")) {
         return false;
      } else {
         int var1 = var0.indexOf(93);
         return var1 > 2 && !var0.substring(2, var1).isBlank();
      }
   }

   private static boolean b(Map<String, Object> var0) {
      Object var1 = var0.get("actualExpr");
      if (var1 == null) {
         return false;
      } else {
         String var2 = d(String.valueOf(var1));
         if (var2 == null) {
            return false;
         } else {
            LinkedHashSet var3 = new LinkedHashSet();
            if (var0.get("expectedExpr") != null) {
               a(String.valueOf(var0.get("expectedExpr")), (Set<String>)var3);
            }

            a(var0.get("expected"), var3);
            return var3.contains(var2);
         }
      }
   }

   private static String d(String var0) {
      String var1 = var0.trim();
      int var2 = 1;

      while (var2 != 0) {
         for (var2 = 0; var1.startsWith("(") || var1.startsWith("$"); var2 = 1) {
            var1 = var1.substring(1).trim();
         }

         String var3 = e(var1);
         if (var3 != null) {
            var1 = var3;
            var2 = 1;
         }
      }

      var2 = 0;

      while (var2 < var1.length() && (Character.isLetterOrDigit(var1.charAt(var2)) || var1.charAt(var2) == '_')) {
         var2++;
      }

      return var2 > 0 ? var1.substring(0, var2) : null;
   }

   private static String e(String var0) {
      if (!var0.startsWith("get")) {
         return null;
      } else {
         String var1 = var0.substring(3);
         if (var1.startsWith("[")) {
            int var2 = var1.indexOf(93);
            var1 = var2 < 0 ? "" : var1.substring(var2 + 1);
         }

         return !var1.isEmpty() && Character.isWhitespace(var1.charAt(0)) ? var1.trim() : null;
      }
   }

   private static void a(String var0, Set<String> var1) {
      int var2 = 0;

      while (var2 < var0.length()) {
         char var3 = var0.charAt(var2);
         if (var3 == '\'' || var3 == '"') {
            int var4 = var0.indexOf(var3, var2 + 1);
            if (var4 < 0) {
               return;
            }

            b(var0.substring(var2 + 1, var4), var1);
            var2 = var4 + 1;
         } else if (!Character.isLetter(var3) && var3 != '_' && var3 != '$') {
            var2++;
         } else {
            int var8 = var2;

            while (var2 < var0.length() && (Character.isLetterOrDigit(var0.charAt(var2)) || var0.charAt(var2) == '_' || var0.charAt(var2) == '$')) {
               var2++;
            }

            boolean var5 = var8 > 0 && var0.charAt(var8 - 1) == '.';
            int var6 = var2;

            while (var6 < var0.length() && Character.isWhitespace(var0.charAt(var6))) {
               var6++;
            }

            boolean var7 = var6 < var0.length() && var0.charAt(var6) == ':';
            if (!var5 && !var7) {
               var1.add(var0.charAt(var8) == '$' ? var0.substring(var8 + 1, var2) : var0.substring(var8, var2));
            }
         }
      }
   }

   private static void b(String var0, Set<String> var1) {
      int var2 = var0.indexOf("#(");
      int var3 = var0.lastIndexOf(41);
      if (var2 >= 0 && var3 > var2 + 1) {
         a(var0.substring(var2 + 2, var3), var1);
      }
   }

   private static void a(Object var0, Set<String> var1) {
      if (var0 instanceof String var2) {
         b(var2, var1);
      } else if (var0 instanceof Map var3) {
         var3.values().forEach(var1x -> a(var1x, var1));
      } else if (var0 instanceof Collection var4) {
         var4.forEach(var1x -> a(var1x, var1));
      }
   }

   public int a() {
      return this.component1;
   }

   public double b() {
      return this.b;
   }

   public boolean c() {
      return this.c;
   }

   public boolean d() {
      return this.d;
   }

   private record AssertionScore(double a, boolean b) {

   }
}
