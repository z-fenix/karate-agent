package io.karatelabs.ext.kafka;

import com.google.protobuf.Descriptors.Descriptor;
import io.karatelabs.common.Json;
import io.karatelabs.ext.contract.WireCodec;
import io.karatelabs.protobuf.ProtobufUtils;

public class ProtoCodec implements WireCodec {
   private final Descriptor descriptor;

   public ProtoCodec(Descriptor descriptor) {
      this.descriptor = descriptor;
   }

   public Object encode(Object value) {
      return ProtobufUtils.fromJson(this.descriptor, Json.of(value).toString());
   }

   public Object decode(Object wire) {
      return Json.of(ProtobufUtils.toJson(this.descriptor, (byte[])wire)).value();
   }
}
