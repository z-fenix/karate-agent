package io.karatelabs.ext.kafka;

import io.karatelabs.avro.AvroSerializer;
import io.karatelabs.common.Json;
import io.karatelabs.ext.common.CoverageEmit;
import io.karatelabs.ext.contract.WireCodec;
import io.karatelabs.js.SimpleObject;
import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarateKafkaProducer implements SimpleObject, Closeable {
   static final Logger logger = LoggerFactory.getLogger("karate.kafka");
   private static final String TOPIC = "topic";
   private static final String SCHEMA = "schema";
   private static final String HEADER = "header";
   private static final String HEADERS = "headers";
   private static final String SEND = "send";
   private static final String KEY = "key";
   private static final String VALUE = "value";
   private static final String CLOSE = "close";
   private static final long SEND_ACK_TIMEOUT_MS = 30000L;
   private final KafkaProducer<String, GenericRecord> kafka;
   private final Properties producerProps;
   private final Map<String, WireCodec> codecs;
   private boolean preflightDone;
   String key;
   Object value;
   String topic;
   String schemaName;
   private Map<String, String> headers = new LinkedHashMap<>();

   public KarateKafkaProducer(Map<String, Object> config, Map<String, WireCodec> codecs) {
      this.codecs = codecs;
      Properties props = new Properties();
      props.put("bootstrap.servers", "127.0.0.1:29092");
      props.put("key.serializer", StringSerializer.class.getName());
      props.put("enable.idempotence", "true");
      props.put("acks", "all");
      props.put("max.in.flight.requests.per.connection", "5");
      props.put("compression.type", "snappy");
      props.put("linger.ms", "20");
      props.put("batch.size", Integer.toString(32768));
      if (config != null) {
         props.putAll(config);
      }

      if (config != null && config.containsKey("schema.registry.url")) {
         props.put("value.serializer", AvroSerializer.class.getName());
      } else {
         props.put("value.serializer", io.karatelabs.ext.kafka.ByteArraySerializer.class.getName());
      }

      this.kafka = new KafkaProducer(props);
      this.producerProps = props;
   }

   private void preflight() {
      if (!this.preflightDone) {
         try {
            List<PartitionInfo> partitions = this.kafka.partitionsFor(this.topic);
            if (partitions != null && !partitions.isEmpty()) {
               this.preflightDone = true;
               logger.debug("preflight ok, partitions for topic '{}': {}", this.topic, partitions.size());
            } else {
               throw new RuntimeException("topic '" + this.topic + "' has no partitions or does not exist");
            }
         } catch (Exception var3) {
            String diag = io.karatelabs.ext.kafka.KafkaDiagnostics.connectFailure(this.producerProps, "fetch metadata for topic '" + this.topic + "'", var3);
            logger.error(diag);
            throw new RuntimeException(diag, var3);
         }
      }
   }

   public void send(ProducerRecord<String, GenericRecord> record) {
      this.preflight();
      this.kafka.send(record);
   }

   public void send() {
      this.preflight();
      String json = Json.of(this.value).toString();
      Object body;
      if (this.schemaName != null) {
         WireCodec codec = this.codecs.get(this.schemaName);
         if (codec == null) {
            throw new RuntimeException("no schema/codec registered: " + this.schemaName);
         }

         if (this.producerProps.containsKey("schema.registry.url") && codec instanceof io.karatelabs.ext.kafka.AvroCodec avro) {
            body = avro.toRecord(this.value);
         } else {
            body = codec.encode(this.value);
         }
      } else {
         body = io.karatelabs.ext.kafka.JsonCodec.INSTANCE.encode(this.value);
      }

      RecordHeaders rh = new RecordHeaders();
      if (this.headers != null) {
         this.headers.forEach((k, v) -> {
            if (v != null) {
               rh.add(k, v.getBytes());
            }
         });
      }

      ProducerRecord pr = new ProducerRecord(this.topic, null, this.key, body, rh);
      logger.debug(">> kafka topic: {}, key: {}, schema: {}", new Object[]{this.topic, this.key, this.schemaName});
      logger.debug(">> body: {}", json);
      long start = System.currentTimeMillis();
      String status = "ack";
      RuntimeException failure = null;

      try {
         this.kafka.send(pr).get(30000L, TimeUnit.MILLISECONDS);
      } catch (Exception var11) {
         status = "error";
         Throwable cause = (Throwable)(var11.getCause() != null ? var11.getCause() : var11);
         failure = new RuntimeException(
            io.karatelabs.ext.kafka.KafkaDiagnostics.connectFailure(this.producerProps, "produce to topic '" + this.topic + "'", cause), cause
         );
         logger.error(failure.getMessage());
      }

      this.emitCoverageMatch("publish", status, System.currentTimeMillis() - start, asMap(json));
      if (failure != null) {
         throw failure;
      }
   }

   private void emitCoverageMatch(String direction, String status, long durationMs, Object payload) {
      if (this.topic != null) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("topic", this.topic);
         if (this.schemaName != null) {
            m.put("schema", this.schemaName);
         }

         m.put("direction", direction);
         m.put("status", status);
         m.put("durationMs", durationMs);
         if (payload != null) {
            m.put("payload", payload);
         }

         CoverageEmit.match("kafka-match", m);
      }
   }

   private static Object asMap(String json) {
      try {
         Object v = Json.of(json).value();
         return v instanceof Map ? v : null;
      } catch (RuntimeException var2) {
         return null;
      }
   }

   @Override
   public void close() {
      logger.debug("closing producer");
      Exception flushFailure = null;

      try {
         this.kafka.flush();
      } catch (Exception var4) {
         String diag = io.karatelabs.ext.kafka.KafkaDiagnostics.connectFailure(this.producerProps, "flush buffered messages on producer close", var4);
         logger.error(diag);
         flushFailure = new RuntimeException(diag, var4);
      }

      this.kafka.close();
      if (flushFailure != null) {
         throw (RuntimeException)flushFailure;
      }
   }

   public Collection<String> jsKeys() {
      return List.of("topic", "schema", "header", "headers", "send", "key", "value", "close");
   }

   public Object jsGet(String name) {
      return switch (name) {
         case "header" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.header(args[0].toString(), args[1].toString());
            return null;
         };
         case "send" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.send();
            return null;
         };
         case "close" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.close();
            return null;
         };
         default -> null;
      };
   }

   public void putMember(String name, Object value) {
      switch (name) {
         case "key":
            this.key = value != null ? value.toString() : null;
            break;
         case "value":
            this.value = value;
            break;
         case "topic":
            this.topic = value != null ? value.toString() : null;
            break;
         case "schema":
            this.schemaName = value != null ? value.toString() : null;
            break;
         case "headers":
            if (value == null) {
               this.headers.clear();
            } else if (value instanceof Map) {
               this.headers = (Map<String, String>)value;
            }
            break;
         default:
            logger.warn("put not supported on producer object: {} - {}", name, value);
      }
   }

   public void header(String key, String value) {
      if (value == null) {
         this.headers.remove(key);
      } else {
         this.headers.put(key, value);
      }
   }
}
