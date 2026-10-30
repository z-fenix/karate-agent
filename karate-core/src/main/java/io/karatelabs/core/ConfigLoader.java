package io.karatelabs.core;

import io.karatelabs.common.Resource;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoader {
   private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
   private static final String SYNTHETIC = "Feature: __config\n  Scenario: __config\n";

   private ConfigLoader() {
   }

   public static Map<String, Object> configOnly(Path workingDir, String env) {
      return configOnly(workingDir, env, null);
   }

   public static Map<String, Object> configOnly(Path workingDir, String env, Map<String, String> systemProperties) {
      return configOnly(workingDir, env, systemProperties, null);
   }

   public static Map<String, Object> configOnly(Path workingDir, String env, Map<String, String> systemProperties, Map<String, String> systemEnv) {
      if (workingDir == null) {
         return new LinkedHashMap<>();
      } else {
         Runner.Builder builder = Runner.builder().configDir(workingDir.toString()).workingDir(workingDir);
         if (env != null && !env.isBlank()) {
            builder.karateEnv(env);
         }

         if (systemProperties != null) {
            systemProperties.forEach(builder::systemProperty);
         }

         builder.systemEnv(systemEnv);
         Suite suite = builder.buildSuite();
         Feature feature = Feature.read(Resource.text("Feature: __config\n  Scenario: __config\n", workingDir));
         FeatureRuntime fr = new FeatureRuntime(suite, feature, null, null, false, null, null);
         Scenario scenario = feature.getSections().get(0).getScenario();
         ScenarioRuntime sr = new ScenarioRuntime(fr, scenario);

         Map var12;
         try {
            Throwable configError = sr.getConfigError();
            if (configError != null) {
               throw configError instanceof RuntimeException re ? re : new RuntimeException(configError);
            }

            Map<String, Object> config = sr.getConfigVars();
            logger.debug("configOnly({}, env={}) -> {} variables", new Object[]{workingDir, env, config.size()});
            var12 = config;
         } finally {
            sr.releaseHttpClient();
         }

         return var12;
      }
   }
}
