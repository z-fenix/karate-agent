package io.karatelabs.ext.kafka;

import io.karatelabs.common.Json;
import io.karatelabs.ext.contract.WireCodec;
import java.nio.charset.StandardCharsets;

public class JsonCodec implements WireCodec {
   public static final io.karatelabs.ext.kafka.JsonCodec INSTANCE = new io.karatelabs.ext.kafka.JsonCodec();

   public Object encode(Object value) {
      return Json.of(value).toString().getBytes(StandardCharsets.UTF_8);
   }

   public Object decode(Object wire) {
      return Json.of(new String((byte[])wire, StandardCharsets.UTF_8)).value();
   }
}
