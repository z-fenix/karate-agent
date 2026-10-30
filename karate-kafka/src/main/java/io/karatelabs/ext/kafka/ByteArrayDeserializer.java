package io.karatelabs.ext.kafka;

import org.apache.kafka.common.serialization.Deserializer;

public class ByteArrayDeserializer implements Deserializer<byte[]> {
   public byte[] deserialize(String topic, byte[] data) {
      return data;
   }
}
