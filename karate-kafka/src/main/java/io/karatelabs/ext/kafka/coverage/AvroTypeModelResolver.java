package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.TypeModel;
import io.karatelabs.ext.contract.TypeModelResolver;
import org.apache.avro.Schema.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroTypeModelResolver implements TypeModelResolver {
   private static final Logger logger = LoggerFactory.getLogger(AvroTypeModelResolver.class);

   public String format() {
      return "avro";
   }

   public TypeModel resolve(String schemaText) {
      try {
         return new AvroTypeModel(new Parser().parse(schemaText));
      } catch (RuntimeException var3) {
         logger.warn("avro: cannot parse .avsc schema — covered name-only (no field axes): {}", var3.toString());
         return null;
      }
   }
}
