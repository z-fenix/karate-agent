package io.karatelabs.ext.contract;

import java.util.Map;

public record KafkaBinding(String topic, KafkaBinding.Direction direction, String keySubject, String valueSubject, String strategy, Integer partitions, Integer replicas, String groupId, String clientId, String schemaRegistryUrl, String schemaRegistryVendor, String schemaIdLocation, String schemaIdPayloadEncoding, Map<String, Object> topicConfiguration) {


   public static KafkaBinding of(String var0, KafkaBinding.Direction var1, String var2, String var3) {
      return new KafkaBinding(var0, var1, var3, var2, null, null, null, null, null, null, null, null, null, Map.of());
   }

   public static enum Direction {
      PUBLISH,
      SUBSCRIBE;
   }
}
