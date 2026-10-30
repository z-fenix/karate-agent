package io.karatelabs.ext.kafka;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KafkaProducerHandle extends JsDispatch {
   private final io.karatelabs.ext.kafka.KarateKafkaProducer producer;
   private volatile String topic;

   KafkaProducerHandle(io.karatelabs.ext.kafka.KarateKafkaProducer producer) {
      this.producer = producer;
      this.lazy("topic", () -> this.topic);
      this.fn("send", a -> {
         this.send(opts(JsApi.arg(a, 0)));
         return null;
      });
      this.fn("close", a -> {
         producer.close();
         return null;
      });
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   private void send(Map<String, Object> o) {
      if (o != null && o.get("topic") != null) {
         this.topic = o.get("topic").toString();
         this.producer.putMember("topic", this.topic);
         if (o.get("schema") != null) {
            this.producer.putMember("schema", o.get("schema"));
         }

         if (o.get("key") != null) {
            this.producer.putMember("key", o.get("key"));
         }

         this.producer.putMember("value", o.get("value"));
         if (o.get("headers") instanceof Map) {
            this.producer.putMember("headers", o.get("headers"));
         }

         this.producer.send();
      } else {
         throw new RuntimeException("Kafka producer.send needs at least a { topic: '...', value: ... }");
      }
   }

   private static Map<String, Object> opts(Object arg) {
      return (Map<String, Object>)(arg instanceof Map<?, ?> m ? m : null);
   }

   public Collection<String> jsKeys() {
      return List.of("topic");
   }

   public String jsKind() {
      return "kafka-producer";
   }

   private static Object help() {
      return JsApi.markdown(
         "# kafka producer (kind: kafka-producer)\n- `p.send({ topic, schema?, key?, value, headers? })` — produce one record; with `schema`\n  (a registered name) the value is Avro/Protobuf-serialized, else sent as raw JSON bytes.\n- `p.close()` — flush + close (surfaces a flush failure rather than swallowing it).\n"
      );
   }
}
