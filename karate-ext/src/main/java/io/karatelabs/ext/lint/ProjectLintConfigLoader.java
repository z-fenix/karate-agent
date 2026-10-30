package io.karatelabs.ext.lint;

import io.karatelabs.core.BootBinding;
import io.karatelabs.core.BootLoader;
import io.karatelabs.core.Ext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectLintConfigLoader {
   private static final Logger logger = LoggerFactory.getLogger(ProjectLintConfigLoader.class);
   private static final Map<String, io.karatelabs.ext.lint.ProjectLintConfigLoader.CachedProjectLintConfig> configCache = new ConcurrentHashMap<>();

   private ProjectLintConfigLoader() {
   }

   public static io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig load(Path var0) {
      if (var0 == null) {
         return io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig.EMPTY_CONFIG;
      } else {
         Path var1 = var0.resolve("karate-boot.js");
         return !Files.exists(var1)
            ? io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig.EMPTY_CONFIG
            : configCache.compute(var0.toAbsolutePath().normalize().toString(), (var2, var3) -> (io.karatelabs.ext.lint.ProjectLintConfigLoader.CachedProjectLintConfig)(var3 != null && var3.isFresh(var1) ? var3 : loadAndCache(var0, var1))).a;
      }
   }

   private static io.karatelabs.ext.lint.ProjectLintConfigLoader.CachedProjectLintConfig loadAndCache(Path var0, Path var1) {
      io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig var2;
      try {
         BootBinding var3 = BootLoader.bootOnly(var0, null);
         LintConfigProvider var4 = var3 == null ? null : findConfigProvider(var3);
         var2 = new io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig(var4 == null ? null : var4.getLintRules(), null, var4 == null ? null : var4.getOpenapi());
      } catch (RuntimeException var6) {
         logger.warn("karate-boot.js at {} could not be evaluated, so any cov.lintRules it declares did not run: {}", var0, var6.getMessage());
         var2 = new io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig(null, var6.getMessage());
      }

      try {
         return new io.karatelabs.ext.lint.ProjectLintConfigLoader.CachedProjectLintConfig(var2, Files.getLastModifiedTime(var1).toMillis(), Files.size(var1));
      } catch (IOException var5) {
         return new io.karatelabs.ext.lint.ProjectLintConfigLoader.CachedProjectLintConfig(var2, -1L, -1L);
      }
   }

   private static LintConfigProvider findConfigProvider(BootBinding var0) {
      for (Ext var2 : var0.getExts()) {
         if (var2 instanceof LintConfigProvider var3) {
            return var3;
         }
      }

      return null;
   }

   public record ProjectLintConfig(Object b, String c, Object d) {
      static final io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig EMPTY_CONFIG = new io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig(null, null, null);

      public ProjectLintConfig(Object var1, String var2) {
         this(var1, var2, null);
      }


      public Map<String, Object> unreadableConfigReport() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("bootFileReadable", false);
         var1.put(
            "note",
            "the project's karate-boot.js could not be evaluated, so any rules it declares in `cov.lintRules` did NOT run — this grade is over the bundled pack alone: "
               + this.c
         );
         return var1;
      }
   }

   private record CachedProjectLintConfig(io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig a, long b, long c) {


      boolean isFresh(Path var1) {
         try {
            return this.b == Files.getLastModifiedTime(var1).toMillis() && this.c == Files.size(var1);
         } catch (IOException var3) {
            return false;
         }
      }
   }
}
