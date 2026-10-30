package io.karatelabs.ext.kafka;

import io.karatelabs.avro.AvroUtils;
import io.karatelabs.core.Channel;
import io.karatelabs.core.FeatureRuntime;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.contract.WireCodec;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.protobuf.ProtobufUtils;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaChannel implements Channel, SimpleObject {
   static final Logger logger = LoggerFactory.getLogger("karate.kafka");
   final Map<String, Object> config;
   private static final String CONSUMER = "consumer";
   private static final String PRODUCER = "producer";
   private static final String REGISTER = "register";
   final Map<String, WireCodec> codecs = new ConcurrentHashMap<>();
   private Path root;
   private Path classpathRoot;
   final List<Closeable> closeables = new ArrayList<>();

   public KafkaChannel(Map<String, Object> config) {
      this.config = config;
   }

   public Object init(ScenarioRuntime runtime) {
      FeatureRuntime fr = runtime == null ? null : runtime.getFeatureRuntime();
      Suite suite = fr == null ? null : fr.getSuite();
      if (suite != null) {
         this.root = suite.getRoot();
         this.classpathRoot = suite.getClasspathRoot();
      }

      return this;
   }

   private String ref(String s) {
      return s != null && this.root != null ? MaxPaths.resolveResource(this.root, this.classpathRoot, s).getPath().toString() : s;
   }

   public void afterScenario() {
      for (int i = this.closeables.size() - 1; i >= 0; i--) {
         try {
            this.closeables.get(i).close();
         } catch (Exception var3) {
            logger.error("error closing kafka resource: {}", var3.getMessage());
         }
      }

      this.closeables.clear();
      this.codecs.clear();
   }

   public void register(Map<String, Object> data) {
      String name = (String)data.get("name");
      if (data.get("codec") instanceof WireCodec codec) {
         this.codecs.put(name, codec);
      } else {
         String path = (String)data.get("path");
         String message = (String)data.get("message");
         List<String> roots = (List<String>)data.get("roots");
         if (path == null) {
            throw new RuntimeException("register needs a 'path' (*.avsc/*.proto) or a 'codec' (a WireCodec)");
         } else {
            if (path.endsWith(".proto")) {
               List<String> resolvedRoots = null;
               if (roots != null) {
                  resolvedRoots = new ArrayList<>(roots.size());

                  for (String r : roots) {
                     resolvedRoots.add(this.ref(r));
                  }
               }

               this.codecs.put(name, new io.karatelabs.ext.kafka.ProtoCodec(ProtobufUtils.toDescriptor(this.ref(path), message, resolvedRoots)));
            } else {
               if (!path.endsWith(".avsc")) {
                  throw new RuntimeException("only '*.avsc' or '*.proto' schemas supported");
               }

               this.codecs.put(name, new io.karatelabs.ext.kafka.AvroCodec(AvroUtils.toSchema(this.ref(path))));
            }
         }
      }
   }

   public io.karatelabs.ext.kafka.KarateKafkaConsumer consumer() {
      io.karatelabs.ext.kafka.KarateKafkaConsumer consumer = new io.karatelabs.ext.kafka.KarateKafkaConsumer(this.config, this.codecs);
      this.closeables.add(consumer);
      return consumer;
   }

   public io.karatelabs.ext.kafka.KarateKafkaProducer producer() {
      io.karatelabs.ext.kafka.KarateKafkaProducer producer = new io.karatelabs.ext.kafka.KarateKafkaProducer(this.config, this.codecs);
      this.closeables.add(producer);
      return producer;
   }

   public Collection<String> jsKeys() {
      return List.of("consumer", "producer", "register");
   }

   public Object jsGet(String name) {
      return switch (name) {
         case "consumer" -> (io.karatelabs.js.JavaInvokable) args -> this.consumer();
         case "producer" -> (io.karatelabs.js.JavaInvokable) args -> this.producer();
         case "register" -> (io.karatelabs.js.JavaInvokable) args -> {
            if (args.length > 0 && args[0] instanceof Map) {
               this.register((Map<String, Object>)args[0]);
            }

            return null;
         };
         default -> null;
      };
   }

   public void putMember(String name, Object value) {
      if ("register".equals(name) && value instanceof Map) {
         this.register((Map<String, Object>)value);
      } else {
         logger.warn("put not supported on channel object: {} - {}", name, value);
      }
   }
}
