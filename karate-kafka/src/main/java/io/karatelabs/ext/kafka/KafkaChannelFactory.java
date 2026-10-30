package io.karatelabs.ext.kafka;

import io.karatelabs.core.ChannelFactory;
import io.karatelabs.core.FeatureRuntime;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.keycheck.Main;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaChannelFactory implements ChannelFactory {
   private static volatile boolean gated;
   private final io.karatelabs.ext.kafka.KafkaExt ext;

   public KafkaChannelFactory() {
      this(null);
   }

   public KafkaChannelFactory(io.karatelabs.ext.kafka.KafkaExt ext) {
      this.ext = ext;
   }

   public io.karatelabs.ext.kafka.KafkaChannel create(ScenarioRuntime runtime, Map<String, Object> options) {
      if (!gated) {
         Main.requireProduct("kafka", new String[]{"kafka"});
         gated = true;
      }

      FeatureRuntime fr = runtime == null ? null : runtime.getFeatureRuntime();
      Suite suite = fr == null ? null : fr.getSuite();
      Path root = suite == null ? null : suite.getRoot();
      Path classpathRoot = suite == null ? null : suite.getClasspathRoot();
      Map<String, Object> config = (Map<String, Object>)(this.ext != null ? this.ext.toClientConfig(root, classpathRoot) : new LinkedHashMap<>());
      return new io.karatelabs.ext.kafka.KafkaChannel(config);
   }
}
