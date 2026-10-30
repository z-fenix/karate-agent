package io.karatelabs.avro;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroDeserializer extends KafkaAvroDeserializer {
   static final Logger logger = LoggerFactory.getLogger(io.karatelabs.avro.AvroDeserializer.class);

   public Object deserialize(String topic, Headers headers, byte[] bytes) {
      try {
         return super.deserialize(topic, headers, bytes);
      } catch (Exception var5) {
         logger.debug("unable to deserialize avro, falling back to json");
         return bytes;
      }
   }
}
