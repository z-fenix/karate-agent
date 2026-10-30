package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JsonPathTokenMatcher {
   static final String a = "[*]";

   private JsonPathTokenMatcher() {
   }

   static List<String> a(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var5 : var0.split("\\.", -1)) {
         int var6 = var5.indexOf(91);
         String var7 = var6 < 0 ? var5 : var5.substring(0, var6);
         if (var7.isEmpty() || var7.indexOf(93) >= 0) {
            return null;
         }

         var1.add(var7);

         while (var6 >= 0 && var6 < var5.length()) {
            if (!var5.startsWith("[*]", var6)) {
               return null;
            }

            var1.add("[*]");
            var6 += "[*]".length();
         }
      }

      return var1;
   }

   static List<String> a(List<String> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var3 : var0) {
         var var4 = a(var3);
         String var5 = var4 == null ? var3 : (String)var4.get(0);
         if (!var1.contains(var5)) {
            var1.add(var5);
         }
      }

      return var1;
   }

   static boolean a(Object var0, List<String> var1, int var2, String var3) {
      String var4 = (String)var1.get(var2);
      boolean var5 = var2 == var1.size() - 1;
      if ("[*]".equals(var4)) {
         if (var0 instanceof List var10) {
            List var7 = var10;
            boolean var8 = false;

            for (int var9 = 0; var9 < var7.size(); var9++) {
               if (!var5) {
                  var8 |= a(var7.get(var9), var1, var2 + 1, var3);
               } else {
                  var8 = true;
                  if (var3 != null) {
                     var7.set(var9, var3);
                  }
               }
            }

            return var8;
         } else {
            return false;
         }
      } else if (!(var0 instanceof Map var6 && var6.containsKey(var4))) {
         return false;
      } else if (!var5) {
         return a(var6.get(var4), var1, var2 + 1, var3);
      } else {
         if (var3 != null) {
            var6.put(var4, var3);
         }

         return true;
      }
   }
}
