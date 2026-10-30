package io.karatelabs.ext.openapi;

import java.util.List;
import java.util.Map;

public class OpenApiOperationMatcher {
   private final List<Map<String, Object>> a;
   private final List<String> b;

   public OpenApiOperationMatcher(List<Map<String, Object>> var1) {
      this(var1, io.karatelabs.ext.openapi.ServerBasePathResolver.a);
   }

   public OpenApiOperationMatcher(List<Map<String, Object>> var1, List<String> var2) {
      this.a = var1;
      this.b = var2 == null ? io.karatelabs.ext.openapi.ServerBasePathResolver.a : var2;
   }

   public Map<String, Object> a(String var1, String var2) {
      if (var1 != null && var2 != null && this.a != null) {
         for (String var4 : io.karatelabs.ext.openapi.ServerBasePathResolver.a(a(var2), this.b)) {
            var var5 = this.c(var1, var4);
            if (var5 != null) {
               return var5;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Map<String, Object> c(String var1, String var2) {
      Map<java.lang.String, java.lang.Object> var3 = null;
      int var4 = -1;

      for (Map var6 : this.a) {
         String var7 = (String)var6.get("method");
         if (var7 != null && var7.equalsIgnoreCase(var1)) {
            String var8 = (String)var6.get("path");
            if (var8 != null) {
               int var9 = b(var2, var8);
               if (var9 > var4) {
                  var3 = var6;
                  var4 = var9;
               }
            }
         }
      }

      return var3;
   }

   static int b(String var0, String var1) {
      String[] var2 = b(var0);
      String[] var3 = b(var1);
      if (var2.length != var3.length) {
         return -1;
      } else {
         int var4 = 0;

         for (int var5 = 0; var5 < var2.length; var5++) {
            String var6 = var3[var5];
            if (var6.length() >= 2 && var6.charAt(0) == '{' && var6.charAt(var6.length() - 1) == '}') {
               if (var2[var5].isEmpty()) {
                  return -1;
               }
            } else {
               if (!var6.equals(var2[var5])) {
                  return -1;
               }

               var4++;
            }
         }

         return var4;
      }
   }

   static String a(String var0) {
      int var1 = var0.indexOf(63);
      if (var1 >= 0) {
         var0 = var0.substring(0, var1);
      }

      int var2 = var0.indexOf("://");
      if (var2 >= 0) {
         int var3 = var0.indexOf(47, var2 + 3);
         var0 = var3 < 0 ? "/" : var0.substring(var3);
      }

      if (var0.length() > 1 && var0.endsWith("/")) {
         var0 = var0.substring(0, var0.length() - 1);
      }

      return var0;
   }

   private static String[] b(String var0) {
      if (!var0.isEmpty() && !"/".equals(var0)) {
         String var1 = var0.startsWith("/") ? var0.substring(1) : var0;
         return var1.split("/", -1);
      } else {
         return new String[]{""};
      }
   }
}
