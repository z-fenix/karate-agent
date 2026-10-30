package io.karatelabs.ext.requirements.kiro;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KiroTaskParser {
   private static final Pattern a = Pattern.compile("^\\s*[-*]\\s*\\[([ xX])]\\s*(?:(\\d+(?:\\.\\d+)*)\\.?\\s+)?(.*)$");
   private static final Pattern b = Pattern.compile("_Requirements:\\s*([^_]*)_");

   private KiroTaskParser() {
   }

   public static List<io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask> a(String var0) {
      ArrayList<io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask> var1 = new ArrayList();
      if (var0 != null && !var0.isBlank()) {
         String var2 = null;
         String var3 = null;
         boolean var4 = false;
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (String var9 : var0.split("\n", -1)) {
            Matcher var10 = a.matcher(var9.stripTrailing());
            Matcher var11 = b.matcher(var9);
            if (var11.find()) {
               for (String var15 : var11.group(1).split(",")) {
                  String var16 = var15.trim();
                  if (!var16.isEmpty()) {
                     var5.add(var16);
                  }
               }
            } else if (var10.matches()) {
               if (var3 != null) {
                  var1.add(new io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask(var2, var3, var4, List.copyOf(var5)));
               }

               var4 = !" ".equals(var10.group(1));
               var2 = var10.group(2);
               var3 = var10.group(3).trim();
               var5 = new ArrayList();
            }
         }

         if (var3 != null) {
            var1.add(new io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask(var2, var3, var4, List.copyOf(var5)));
         }

         return var1;
      } else {
         return var1;
      }
   }

   static String b(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 < 0 ? var0 : var0.substring(0, var1);
   }

   static String c(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 < 0 ? null : var0.substring(var1 + 1);
   }

   public record KiroTask(String a, String b, boolean c, List<String> d) {
   }
}
