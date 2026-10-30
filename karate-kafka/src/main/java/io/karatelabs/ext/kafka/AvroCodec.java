package io.karatelabs.ext.kafka;

import io.karatelabs.avro.AvroUtils;
import io.karatelabs.common.Json;
import io.karatelabs.ext.contract.WireCodec;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

public class AvroCodec implements WireCodec {
   private final Schema schema;

   public AvroCodec(Schema schema) {
      this.schema = schema;
   }

   public Schema schema() {
      return this.schema;
   }

   public GenericRecord toRecord(Object value) {
      return AvroUtils.fromJson(this.schema, Json.of(value).toString());
   }

   public Object encode(Object value) {
      return AvroUtils.toBytes(this.toRecord(value));
   }

   public Object decode(Object wire) {
      return Json.of(AvroUtils.toJson(this.schema, (byte[])wire)).value();
   }
}
