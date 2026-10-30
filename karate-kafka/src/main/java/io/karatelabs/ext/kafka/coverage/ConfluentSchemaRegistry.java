package io.karatelabs.ext.kafka.coverage;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.karatelabs.ext.contract.SchemaRegistry;
import io.karatelabs.ext.contract.TypeModel;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluentSchemaRegistry implements SchemaRegistry {
   private static final Logger logger = LoggerFactory.getLogger(ConfluentSchemaRegistry.class);
   private final SchemaRegistryClient client;

   public ConfluentSchemaRegistry(SchemaRegistryClient client) {
      this.client = client;
   }

   public static ConfluentSchemaRegistry of(String url) {
      return new ConfluentSchemaRegistry(new CachedSchemaRegistryClient(url, 256));
   }

   public List<String> subjects() {
      try {
         return new ArrayList<>(this.client.getAllSubjects());
      } catch (Exception var2) {
         throw new RuntimeException("kafka schema-registry: cannot list subjects — " + var2.getMessage(), var2);
      }
   }

   public TypeModel resolve(String subject, int version) {
      try {
         SchemaMetadata meta = version == -1 ? this.client.getLatestSchemaMetadata(subject) : this.client.getSchemaMetadata(subject, version);
         String type = meta.getSchemaType();
         if (type != null && !"AVRO".equalsIgnoreCase(type)) {
            logger.debug("kafka schema-registry: subject {} is {} — name-only (no field axes yet)", subject, type);
            return null;
         } else {
            return new AvroTypeModel(new Parser().parse(meta.getSchema()));
         }
      } catch (Exception var5) {
         if (isSubjectNotFound(var5)) {
            logger.debug("kafka schema-registry: no subject {} — covered name-only (no field axes)", subject);
         } else {
            logger.warn("kafka schema-registry: cannot resolve subject {} (v{}) — {}", new Object[]{subject, version, var5.toString()});
         }

         return null;
      }
   }

   private static boolean isSubjectNotFound(Throwable e) {
      return e instanceof RestClientException rce && rce.getStatus() == 404;
   }
}
