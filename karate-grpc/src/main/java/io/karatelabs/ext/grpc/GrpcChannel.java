package io.karatelabs.ext.grpc;

import io.karatelabs.core.Channel;
import io.karatelabs.core.FeatureRuntime;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcChannel implements Channel {
   static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private final Map<String, Object> config;
   private final io.karatelabs.ext.grpc.GrpcExt ext;
   final List<io.karatelabs.ext.grpc.GrpcConsumer> consumers = new ArrayList<>();

   public GrpcChannel(Map<String, Object> config, io.karatelabs.ext.grpc.GrpcExt ext) {
      this.config = config;
      this.ext = ext;
   }

   public io.karatelabs.ext.grpc.GrpcConsumer init(ScenarioRuntime scenarioRuntime) {
      io.karatelabs.ext.grpc.GrpcConsumer consumer = new io.karatelabs.ext.grpc.GrpcConsumer(this.config);
      FeatureRuntime fr = scenarioRuntime == null ? null : scenarioRuntime.getFeatureRuntime();
      Suite suite = fr == null ? null : fr.getSuite();
      if (suite != null) {
         consumer.root = suite.getRoot();
         consumer.classpathRoot = suite.getClasspathRoot();
      }

      if (this.ext != null) {
         this.ext.seedDefaults(consumer);
      }

      this.consumers.add(consumer);
      return consumer;
   }

   public void afterScenario() {
      for (io.karatelabs.ext.grpc.GrpcConsumer consumer : this.consumers) {
         try {
            consumer.stop();
         } catch (Exception var4) {
            logger.warn("{}", var4.getMessage());
         }
      }

      this.consumers.clear();
   }
}
