package io.karatelabs.ext.common;

import io.karatelabs.common.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MaxPaths {
   public static final String REQUIREMENTS = "requirements";
   public static final String CHECKS = "checks";
   public static final String RULEBOOKS = "rulebooks";

   private MaxPaths() {
   }

   public static Path requirementsDir(Path var0) {
      return var0.resolve("requirements");
   }

   public static Path checksDir(Path var0) {
      return var0.resolve("checks");
   }

   public static Path rulebooksDir(Path var0) {
      return var0.resolve("rulebooks");
   }

   public static boolean isFeature(String var0) {
      return var0 != null && var0.trim().toLowerCase().endsWith(".feature");
   }

   public static Path eventsFile(Path var0) {
      return var0.resolve("karate-json").resolve("karate-events.jsonl");
   }

   public static String resolveRef(Path var0, String var1) {
      if (var1 == null || var1.isBlank()) {
         return var1;
      } else if (var1.startsWith("file:") || var1.startsWith("classpath:")) {
         return var1;
      } else if (var1.startsWith("this:")) {
         throw new IllegalArgumentException(
            "'" + var1 + "' — 'this:' is feature-relative and is only valid inside a feature; use a leading '/' to anchor the project root"
         );
      } else if (var0 == null) {
         return var1;
      } else {
         return !var1.startsWith("/") && Path.of(var1).isAbsolute() ? var1 : var0.resolve(stripLeadingSlashes(var1)).normalize().toString();
      }
   }

   public static Path resolvePath(Path var0, String var1) {
      if (var1 != null && var1.startsWith("classpath:")) {
         throw new IllegalArgumentException(
            "'"
               + var1
               + "' — a 'classpath:' reference has no file-system path (it may live inside a jar). Use a leading '/' to anchor the project root, or 'file:' for a host path."
         );
      } else {
         String var2 = resolveRef(var0, var1);
         return Path.of(var2 != null && var2.startsWith("file:") ? Resource.removePrefix(var2) : var2);
      }
   }

   public static Resource resolveResource(Path var0, String var1) {
      return resolveResource(var0, var0, var1);
   }

   public static Resource resolveResource(Path var0, Path var1, String var2) {
      return var2 != null && var2.startsWith("classpath:") ? Resource.path(var2, var0, var1) : Resource.path(resolveRef(var0, var2));
   }

   public static Path resolveLaunch(Path var0, String var1) {
      if (var1 == null) {
         return var0;
      } else {
         Path var2 = Path.of(var1);
         return (!var2.isAbsolute() && var0 != null ? var0.resolve(var2) : var2).normalize();
      }
   }

   public static Path lockKey(Path var0) {
      Path var1 = var0.toAbsolutePath().normalize();
      return keyFrom(var1, var1.getParent());
   }

   public static Path dirLockKey(Path var0) {
      Path var1 = var0.toAbsolutePath().normalize();
      return keyFrom(var1, var1);
   }

   private static Path keyFrom(Path var0, Path var1) {
      Path var2 = var1;

      while (var2 != null && !Files.isDirectory(var2)) {
         var2 = var2.getParent();
      }

      if (var2 == null) {
         return var0;
      } else {
         try {
            return var2.toRealPath().resolve(var2.relativize(var0));
         } catch (IOException var4) {
            return var0;
         }
      }
   }

   public static Path within(Path var0, Path var1) {
      if (var0 != null && var1 != null) {
         Path var2 = var0.toAbsolutePath().normalize();
         Path var3 = var1.toAbsolutePath().normalize();
         if (!var3.startsWith(var2)) {
            throw new SecurityException("path traversal denied: " + var1);
         } else {
            return var3;
         }
      } else {
         throw new IllegalArgumentException("within requires a ceiling and a candidate");
      }
   }

   public static Path resolveSandboxed(Path var0, String var1) {
      if (var0 == null) {
         throw new IllegalArgumentException("resolveSandboxed requires a root");
      } else {
         String var2 = var1 == null ? "" : var1;
         if (!var2.startsWith("file:") && !var2.startsWith("classpath:")) {
            Path var3 = within(var0, var0.resolve(stripLeadingSlashes(var2)));
            Path var4 = lockKey(var3);
            if (Files.isSymbolicLink(var3)) {
               try {
                  var4 = var3.toRealPath();
               } catch (IOException var6) {
               }
            }

            if (!var4.startsWith(dirLockKey(var0))) {
               throw new SecurityException("'" + var1 + "' leads outside the project through a symbolic link");
            } else {
               return var3;
            }
         } else {
            throw new SecurityException("'" + var1 + "' is not reachable here — this surface is sandboxed to the project; use a project-relative path");
         }
      }
   }

   public static String stripLeadingSlashes(String var0) {
      return Resource.stripLeadingSlashes(var0);
   }
}
