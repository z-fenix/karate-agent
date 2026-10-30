package io.karatelabs.avro;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.header.Headers;

public class AvroSerializer extends KafkaAvroSerializer {
   public byte[] serialize(String topic, Headers headers, Object record) {
      return record instanceof byte[] ? (byte[])record : super.serialize(topic, headers, record);
   }
}
