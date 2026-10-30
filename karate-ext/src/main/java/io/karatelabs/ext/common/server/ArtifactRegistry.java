package io.karatelabs.ext.common.server;

import io.karatelabs.ext.common.MaxPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ArtifactRegistry {
   private static final Logger b = LoggerFactory.getLogger(ArtifactRegistry.class);
   private final Map<String, Path> c = new LinkedHashMap<>();
   public static final String a = ".local.";

   public ArtifactRegistry a(String var1, Path var2) {
      if (var2 != null) {
         this.c.put(var1, var2.toAbsolutePath().normalize());
      }

      return this;
   }

   public Map<String, String> a() {
      LinkedHashMap var1 = new LinkedHashMap();
      this.c.forEach((var1x, var2) -> var1.put(var1x, var2.toString()));
      return var1;
   }

   public Path a(String var1, String var2) {
      Path var3 = this.c.get(var1);
      if (var3 != null && var2 != null) {
         Path var4;
         try {
            var4 = MaxPaths.within(var3, var3.resolve(var2));
         } catch (SecurityException var6) {
            b.debug("artifact path traversal refused: {} / {}", var1, var2);
            return null;
         }

         if (a(var4)) {
            b.debug("local-only artifact refused from serving: {} / {}", var1, var2);
            return null;
         } else {
            return Files.isRegularFile(var4) ? var4 : null;
         }
      } else {
         return null;
      }
   }

   public static boolean a(Path var0) {
      Path var1 = var0 == null ? null : var0.getFileName();
      return var1 != null && var1.toString().contains(".local.");
   }

   public List<Map<String, Object>> b(String var1, String var2) {
      Path var3 = this.c.get(var1);
      ArrayList var4 = new ArrayList();
      if (var3 != null && Files.isDirectory(var3)) {
         try (var var5 = Files.walk(var3)) {
            var5.filter(var0 -> Files.isRegularFile(var0)).filter(var0 -> !a(var0)).map(var3::relativize).map(Path::toString).sorted().forEach(var3x -> {
               LinkedHashMap var4x = new LinkedHashMap();
               var4x.put("file", var3x);
               var4x.put("url", var2 + "/" + var1 + "/" + var3x);
               var4.add(var4x);
            });
         } catch (IOException var10) {
            b.debug("artifact list failed for {}: {}", var1, var10.getMessage());
         }

         return var4;
      } else {
         return var4;
      }
   }
}
