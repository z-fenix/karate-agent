package io.karatelabs.ext.common.jsapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class SuggestionEngine {
   public static final int a = 3;

   private SuggestionEngine() {
   }

   public static List<String> a(String var0, Collection<String> var1) {
      if (var0 != null && !var0.isBlank() && var1 != null && !var1.isEmpty()) {
         String var2 = var0.toLowerCase(Locale.ROOT);
         int var3 = Math.max(1, Math.min(3, var0.length() / 3));
         ArrayList<io.karatelabs.ext.common.jsapi.SuggestionEngine.ScoredSuggestion> var4 = new ArrayList();

         for (String var6 : var1) {
            if (var6 != null) {
               if (var6.equals(var0)) {
                  return List.of();
               }

               int var7 = a(var2, var6.toLowerCase(Locale.ROOT), var3);
               if (var7 >= 0 && var7 <= var3) {
                  var4.add(new io.karatelabs.ext.common.jsapi.SuggestionEngine.ScoredSuggestion(var6, var7));
               }
            }
         }

         var4.sort((var0x, var1x) -> var0x.b != var1x.b ? Integer.compare(var0x.b, var1x.b) : var0x.a.compareTo(var1x.a));
         ArrayList<java.lang.String> var8 = new ArrayList(Math.min(3, var4.size()));

         for (int var9 = 0; var9 < var4.size() && var9 < 3; var9++) {
            var8.add(((io.karatelabs.ext.common.jsapi.SuggestionEngine.ScoredSuggestion)var4.get(var9)).a);
         }

         return var8;
      } else {
         return List.of();
      }
   }

   static int a(String var0, String var1, int var2) {
      int var3 = var0.length();
      int var4 = var1.length();
      if (Math.abs(var3 - var4) > var2) {
         return -1;
      } else {
         int[] var5 = new int[var4 + 1];
         int[] var6 = new int[var4 + 1];
         int var7 = 0;

         while (var7 <= var4) {
            var5[var7] = var7++;
         }

         for (int var12 = 1; var12 <= var3; var12++) {
            var6[0] = var12;
            int var8 = var6[0];
            char var9 = var0.charAt(var12 - 1);

            for (int var10 = 1; var10 <= var4; var10++) {
               int var11 = var9 == var1.charAt(var10 - 1) ? 0 : 1;
               var6[var10] = Math.min(Math.min(var6[var10 - 1] + 1, var5[var10] + 1), var5[var10 - 1] + var11);
               var8 = Math.min(var8, var6[var10]);
            }

            if (var8 > var2) {
               return -1;
            }

            int[] var13 = var5;
            var5 = var6;
            var6 = var13;
         }

         return var5[var4];
      }
   }

   private record ScoredSuggestion(String a, int b) {
   }
}
