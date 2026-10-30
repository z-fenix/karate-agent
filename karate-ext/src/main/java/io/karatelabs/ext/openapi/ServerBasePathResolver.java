package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerBasePathResolver {
   public static final List<String> a = List.of("");
   private static final Pattern b = Pattern.compile("\\{([^}/]+)}");

   private ServerBasePathResolver() {
   }

   public static String a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         if (!var1.startsWith("/")) {
            var1 = "/" + var1;
         }

         while (var1.length() > 1 && var1.endsWith("/")) {
            var1 = var1.substring(0, var1.length() - 1);
         }

         return "/".equals(var1) ? "" : var1;
      } else {
         return "";
      }
   }

   public static List<String> a(List<String> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (String var3 : var0) {
         var1.add(a(var3));
      }

      return var1.isEmpty() ? a : List.copyOf(var1);
   }

   public static List<String> a(Map<String, Object> var0, List<String> var1) {
      if (var0 == null) {
         return a;
      } else {
         LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();
         if (var0.containsKey("openapi")) {
            Object var5 = var0.get("servers");
            if (var5 instanceof List) {
               for (Object var6 : (List)var5) {
                  if (var6 instanceof Map var7 && var7.get("url") instanceof String var8) {
                     String var12 = a(var8, (Map<String, Object>)var7.get("variables"), var1);
                     if (var12 != null) {
                        var2.add(a(b(var12)));
                     }
                  }
               }
            }

            b(var0, var1);
         } else if (var0.get("basePath") instanceof String var3) {
            var2.add(a(var3));
         }

         return var2.isEmpty() ? a : List.copyOf(var2);
      }
   }

   private static String a(String var0, Map<String, Object> var1, List<String> var2) {
      Matcher var3 = b.matcher(var0);
      StringBuilder var4 = new StringBuilder();

      while (var3.find()) {
         String var5 = var3.group(1);
         Object var7 = (var1 == null ? null : var1.get(var5)) instanceof Map var8 ? var8.get("default") : null;
         if (var7 == null) {
            if (var2 != null) {
               var2.add("server '" + var0 + "' declares variable '" + var5 + "' with no default — the server is ignored");
            }

            return null;
         }

         var3.appendReplacement(var4, Matcher.quoteReplacement(String.valueOf(var7)));
      }

      var3.appendTail(var4);
      return var4.toString();
   }

   private static String b(String var0) {
      int var1 = var0.indexOf("://");
      if (var1 < 0) {
         return var0;
      } else {
         int var2 = var0.indexOf(47, var1 + 3);
         return var2 < 0 ? "" : var0.substring(var2);
      }
   }

   private static void b(Map<String, Object> var0, List<String> var1) {
      if (var1 != null && var0.get("paths") instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (var4.getValue() instanceof Map var5) {
               if (var5.containsKey("servers")) {
                  var1.add("path '" + var4.getKey() + "' declares its own servers — not evaluated");
               }

               for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
                  if (var7.getValue() instanceof Map var8 && var8.containsKey("servers")) {
                     var1.add("operation '" + var7.getKey() + " " + var4.getKey() + "' declares its own servers — not evaluated");
                  }
               }
            }
         }
      }
   }

   public static List<String> a(String var0, List<String> var1) {
      if (var0 == null) {
         return List.of();
      } else {
         List var2 = var1 != null && !var1.isEmpty() ? var1 : a;
         ArrayList<java.lang.String> var3 = new ArrayList(var2.size());

         for (String var5 : (Iterable<String>) (Iterable<?>) (var2)) {
            String var6;
            if (var5.isEmpty()) {
               var6 = var0;
            } else {
               if (!var0.startsWith(var5) || var0.length() != var5.length() && var0.charAt(var5.length()) != '/') {
                  continue;
               }

               var6 = var0.substring(var5.length());
               if (var6.isEmpty()) {
                  var6 = "/";
               }
            }

            if (!var3.contains(var6)) {
               var3.add(var6);
            }
         }

         return var3;
      }
   }
}
