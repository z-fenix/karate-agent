package io.karatelabs.ext.kafka;

import io.karatelabs.ext.common.run.JsNamespaceProvider;
import java.nio.file.Path;

public class KafkaNamespaceProvider implements JsNamespaceProvider {
   public String name() {
      return "Kafka";
   }

   public Object namespace() {
      return new io.karatelabs.ext.kafka.Kafka();
   }

   public Object namespace(Path root) {
      return new io.karatelabs.ext.kafka.Kafka().withRoot(root);
   }

   public String skill() {
      return "kafka";
   }
}
