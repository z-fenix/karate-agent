package io.karatelabs.ext.max.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DemoTemplate {
   private static final String GITIGNORE = "# machine-generated — never committed (D128): runs/ is the durable evidence store,\n# target/ holds regenerable reports. Commit the authored artifacts (requirements/,\n# rulebooks/, checks/, openapi.yaml, configs).\nruns/\ntarget/\n";

   private DemoTemplate() {
   }

   public static void materialize(String var0, Path var1) {
      String var2 = "META-INF/karate/templates/" + var0 + "/";

      for (String var4 : manifest(var2)) {
         byte[] var5 = readResource(var2 + var4);
         if (var5 == null) {
            throw new IllegalStateException("template file missing from the jar: " + var2 + var4);
         }

         Path var6 = var1.resolve(var4);

         try {
            Files.createDirectories(var6.getParent());
            Files.write(var6, var5);
         } catch (IOException var9) {
            throw new UncheckedIOException(var9);
         }
      }

      try {
         Files.createDirectories(var1);
         Files.writeString(
            var1.resolve(".gitignore"),
            "# machine-generated — never committed (D128): runs/ is the durable evidence store,\n# target/ holds regenerable reports. Commit the authored artifacts (requirements/,\n# rulebooks/, checks/, openapi.yaml, configs).\nruns/\ntarget/\n"
         );
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      }
   }

   private static List<String> manifest(String var0) {
      byte[] var1 = readResource(var0 + "manifest.txt");
      if (var1 == null) {
         throw new IllegalArgumentException("no such project template baked in the jar: " + var0);
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (String var6 : new String(var1, StandardCharsets.UTF_8).split("\n")) {
            String var7 = var6.trim();
            if (!var7.isEmpty() && !var7.startsWith("#")) {
               var2.add(var7);
            }
         }

         return var2;
      }
   }

   private static byte[] readResource(String var0) {
      ClassLoader var1 = DemoTemplate.class.getClassLoader();

      try {
         byte[] var3;
         try (InputStream var2 = var1.getResourceAsStream(var0)) {
            var3 = var2 == null ? null : var2.readAllBytes();
         }

         return var3;
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }
}
