package io.karatelabs.ext.max.jsapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProjectEnvironmentScanner {
   private static final Pattern a = Pattern.compile("karate-config-([A-Za-z0-9._-]{1,64})\\.js");
   private static final Pattern b = Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*karate\\.env\\b");
   private static final String c = "['\"]([A-Za-z0-9._-]{1,64})['\"]";

   private ProjectEnvironmentScanner() {
   }

   static List<String> a(List<Path> var0, String var1) {
      TreeSet<java.lang.String> var2 = new TreeSet();

      for (Path var4 : var0) {
         try (DirectoryStream var5 = Files.newDirectoryStream(var4, "karate-config-*.js")) {
            for (Path var7 : (Iterable<Path>) (Iterable<?>) (var5)) {
               Matcher var8 = a.matcher(var7.getFileName().toString());
               if (var8.matches()) {
                  var2.add(var8.group(1));
               }
            }
         } catch (IOException var12) {
         }

         Path var13 = var4.resolve("karate-config.js");
         if (Files.isRegularFile(var13)) {
            try {
               var2.addAll(a(Files.readString(var13, StandardCharsets.UTF_8)));
            } catch (IOException var9) {
            }
         }
      }

      if (var1 != null) {
         var2.add(var1);
      }

      return new ArrayList<>(var2);
   }

   static List<String> a(String var0) {
      var var1 = new ArrayList<>(List.of("karate\\.env"));
      Matcher var2 = b.matcher(var0);

      while (var2.find()) {
         var1.add(Pattern.quote(var2.group(1)));
      }

      ArrayList<java.lang.String> var3 = new ArrayList();

      for (String var5 : (Iterable<String>) (Iterable<?>) (var1)) {
         String var6 = "(?<![\\w$.])" + var5 + "(?![\\w$])";
         a(var0, Pattern.compile(var6 + "\\s*[!=]==?\\s*['\"]([A-Za-z0-9._-]{1,64})['\"]"), var3);
         a(var0, Pattern.compile("['\"]([A-Za-z0-9._-]{1,64})['\"]\\s*[!=]==?\\s*" + var6), var3);
         Matcher var7 = Pattern.compile("switch\\s*\\(\\s*" + var6 + "\\s*\\)\\s*\\{").matcher(var0);

         while (var7.find()) {
            int var8 = a(var0, var7.end() - 1);
            a(var0.substring(var7.end(), var8), Pattern.compile("\\bcase\\s+['\"]([A-Za-z0-9._-]{1,64})['\"]\\s*:"), var3);
         }
      }

      return var3;
   }

   private static void a(String var0, Pattern var1, List<String> var2) {
      Matcher var3 = var1.matcher(var0);

      while (var3.find()) {
         var2.add(var3.group(1));
      }
   }

   private static int a(String var0, int var1) {
      int var2 = 0;

      for (int var3 = var1; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '{') {
            var2++;
         } else if (var4 == '}') {
            if (--var2 == 0) {
               return var3;
            }
         }
      }

      return var0.length();
   }

   static String a() {
      return ProjectConfig.ambientEnv();
   }
}
