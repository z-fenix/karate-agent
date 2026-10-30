package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathTemplateMatcher {
   private static final Pattern a = Pattern.compile("\\{([^}]+)}");

   public static io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch a(String var0, String var1) {
      var0 = c(var0);
      var1 = c(var1);
      if (var0.equals(var1)) {
         return new io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch(var1, Collections.emptyMap(), Integer.MAX_VALUE);
      } else {
         String[] var2 = d(var0);
         String[] var3 = d(var1);
         if (var2.length != var3.length) {
            return null;
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var4 = new LinkedHashMap();
            int var5 = 0;

            for (int var6 = 0; var6 < var2.length; var6++) {
               String var7 = var2[var6];
               String var8 = var3[var6];
               if (a(var8)) {
                  String var9 = var8.substring(1, var8.length() - 1);
                  var4.put(var9, var7);
               } else {
                  if (!var7.equals(var8)) {
                     return null;
                  }

                  var5++;
               }
            }

            return new io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch(var1, Collections.unmodifiableMap(var4), var5);
         }
      }
   }

   public static String a(String var0, List<String> var1) {
      io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var2 = b(var0, var1);
      return var2 != null ? var2.a() : null;
   }

   public static io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch b(String var0, List<String> var1) {
      io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var2 = null;

      for (String var4 : var1) {
         io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var5 = a(var0, var4);
         if (var5 != null && (var2 == null || var5.c() > var2.c())) {
            var2 = var5;
         }
      }

      return var2;
   }

   public static boolean b(String var0, String var1) {
      String[] var2 = d(c(var0));
      String[] var3 = d(c(var1));
      if (var2.length != var3.length) {
         return false;
      } else {
         int var4 = 0;
         int var5 = 0;
         boolean var6 = false;

         for (int var7 = 0; var7 < var2.length; var7++) {
            boolean var8 = a(var2[var7]);
            boolean var9 = a(var3[var7]);
            if (!var8 && !var9 && !var2[var7].equals(var3[var7])) {
               return false;
            }

            if (!var8) {
               var4++;
            }

            if (!var9) {
               var5++;
            }

            if (!var2[var7].equals(var3[var7])) {
               var6 = true;
            }
         }

         return var6 && var4 == var5;
      }
   }

   public static boolean a(String var0) {
      return var0.startsWith("{") && var0.endsWith("}");
   }

   public static List<String> b(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      Matcher var2 = a.matcher(var0);

      while (var2.find()) {
         var1.add(var2.group(1));
      }

      return var1;
   }

   static String c(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         if (var0.length() > 1 && var0.endsWith("/")) {
            var0 = var0.substring(0, var0.length() - 1);
         }

         if (!var0.startsWith("/")) {
            var0 = "/" + var0;
         }

         return var0;
      } else {
         return "/";
      }
   }

   static String[] d(String var0) {
      return var0 != null && !var0.isEmpty() && !var0.equals("/")
         ? Arrays.stream(var0.split("/")).filter(var0x -> !var0x.isEmpty()).toArray(String[]::new)
         : new String[0];
   }

   public record PathTemplateMatch(String a, Map<String, String> b, int c) {

   }
}
