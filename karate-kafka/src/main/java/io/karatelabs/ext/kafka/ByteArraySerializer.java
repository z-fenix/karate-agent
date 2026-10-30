package io.karatelabs.ext.kafka;

import org.apache.kafka.common.serialization.Serializer;

public class ByteArraySerializer implements Serializer<byte[]> {
   public byte[] serialize(String topic, byte[] data) {
      return data;
   }
}
