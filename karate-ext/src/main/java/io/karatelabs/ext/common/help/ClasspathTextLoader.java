package io.karatelabs.ext.common.help;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClasspathTextLoader {
   private static final Pattern a = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

   private ClasspathTextLoader() {
   }

   public static String a(String var0) {
      ClassLoader var1 = ClasspathTextLoader.class.getClassLoader();

      try {
         String var4;
         try (InputStream var2 = var1.getResourceAsStream(var0)) {
            if (var2 == null) {
               return null;
            }

            byte[] var3 = var2.readAllBytes();
            var4 = io.karatelabs.ext.common.help.SealedHelpCodec.a(var3) ? io.karatelabs.ext.common.help.SealedHelpCodec.b(var3) : new String(var3, StandardCharsets.UTF_8);
         }

         return var4;
      } catch (Exception var7) {
         return null;
      }
   }

   public static String b(String var0) {
      if (var0 == null) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if (var3 >= 'a' && var3 <= 'z' || var3 >= 'A' && var3 <= 'Z' || var3 >= '0' && var3 <= '9' || var3 == '-' || var3 == '_') {
               var1.append(var3);
            }
         }

         return var1.toString();
      }
   }

   public static String a(String var0, Map<String, Object> var1) {
      return a(var0, var1, false);
   }

   public static String b(String var0, Map<String, Object> var1) {
      return a(var0, var1, true);
   }

   private static String a(String var0, Map<String, Object> var1, boolean var2) {
      if (var0 == null) {
         return null;
      } else {
         Map<java.lang.String, Object> var3 = var1 != null ? var1 : Map.of();
         ArrayList<java.lang.String> var4 = var2 ? new ArrayList() : null;
         Matcher var5 = a.matcher(var0);
         StringBuilder var6 = new StringBuilder();

         while (var5.find()) {
            String var7 = var5.group(1);
            if (var3.containsKey(var7) && var3.get(var7) != null) {
               var5.appendReplacement(var6, Matcher.quoteReplacement(String.valueOf(var3.get(var7))));
            } else {
               if (var2) {
                  var4.add(var7);
               }

               var5.appendReplacement(var6, Matcher.quoteReplacement(var5.group(0)));
            }
         }

         var5.appendTail(var6);
         if (var2 && !var4.isEmpty()) {
            throw new IllegalArgumentException("unsubstituted placeholders: " + var4);
         } else {
            return var6.toString();
         }
      }
   }
}
