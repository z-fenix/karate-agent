package io.karatelabs.core;

import io.karatelabs.common.Resource;
import io.karatelabs.js.Engine;
import io.karatelabs.js.ExternalBridge;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BootLoader {
   private static final Logger logger = LoggerFactory.getLogger(BootLoader.class);
   public static final String BOOT_FILE_NAME = "karate-boot.js";

   private BootLoader() {
   }

   public static BootBinding evalIfPresent(Suite suite, Path bootstrapWorkingDir, String env) {
      Resource resource = locate(bootstrapWorkingDir);
      if (resource == null) {
         return null;
      } else {
         logger.info("{} processed", "karate-boot.js");
         BootBinding boot = new BootBinding(
            suite, suite == null ? bootstrapWorkingDir : suite.getRoot(), env, suite == null ? e -> {} : suite::registerExtListener
         );
         Engine engine = new Engine();
         engine.setExternalBridge(new ExternalBridge() {});
         engine.putRootBinding("boot", boot);

         try {
            engine.eval(resource);
            return boot;
         } catch (Exception var7) {
            throw new RuntimeException("karate-boot.js evaluation failed: " + var7.getMessage(), var7);
         }
      }
   }

   public static BootBinding bootOnly(Path workingDir, String env) {
      if (workingDir == null) {
         return null;
      } else {
         Runner.Builder builder = Runner.builder().configDir(workingDir.toString()).workingDir(workingDir);
         if (env != null && !env.isBlank()) {
            builder.karateEnv(env);
         }

         return builder.buildSuite().getBootBinding();
      }
   }

   private static Resource locate(Path bootstrapWorkingDir) {
      if (bootstrapWorkingDir != null) {
         Path bootAtRoot = bootstrapWorkingDir.resolve("karate-boot.js");
         if (Files.exists(bootAtRoot)) {
            return Resource.from(bootAtRoot, bootstrapWorkingDir);
         }
      }

      try {
         Resource r = Resource.path("classpath:karate-boot.js");
         if (r.exists()) {
            return r;
         }
      } catch (Exception var2) {
      }

      return null;
   }
}
