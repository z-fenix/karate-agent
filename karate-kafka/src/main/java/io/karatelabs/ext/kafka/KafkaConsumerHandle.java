package io.karatelabs.ext.kafka;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KafkaConsumerHandle extends JsDispatch {
   private final io.karatelabs.ext.kafka.KarateKafkaConsumer consumer;
   private volatile String topic;
   private volatile Number count;

   KafkaConsumerHandle(io.karatelabs.ext.kafka.KarateKafkaConsumer consumer) {
      this.consumer = consumer;
      this.lazy("topic", () -> this.topic);
      this.lazy("count", () -> this.count);
      this.fn("start", a -> {
         this.start(opts(JsApi.arg(a, 0)));
         return null;
      });
      this.fn("collect", a -> consumer.collect());
      this.fn("pop", a -> consumer.pop());
      this.fn("close", a -> {
         consumer.close();
         return null;
      });
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   private void start(Map<String, Object> o) {
      if (o != null && o.get("topic") != null) {
         this.topic = o.get("topic").toString();
         this.consumer.putMember("topic", this.topic);
         if (o.get("count") instanceof Number n) {
            this.count = n;
            this.consumer.putMember("count", n);
         }

         if (o.containsKey("filter")) {
            this.consumer.putMember("filter", o.get("filter"));
         }

         if (o.get("schema") != null) {
            this.consumer.putMember("schema", o.get("schema"));
         }

         if (o.get("timeout") instanceof Number t) {
            this.consumer.putMember("timeout", t);
         }

         if (o.get("headers") instanceof Map) {
            this.consumer.putMember("headers", o.get("headers"));
         }

         this.consumer.start();
      } else {
         throw new RuntimeException("Kafka consumer.start needs at least a { topic: '...' }");
      }
   }

   private static Map<String, Object> opts(Object arg) {
      return (Map<String, Object>)(arg instanceof Map<?, ?> m ? m : null);
   }

   public Collection<String> jsKeys() {
      return List.of("topic", "count");
   }

   public String jsKind() {
      return "kafka-consumer";
   }

   private static Object help() {
      return JsApi.markdown(
         "# kafka consumer (kind: kafka-consumer)\n- `c.start({ topic, count?, filter?, schema?, timeout?, headers? })` — subscribe (non-blocking);\n  `count` = how many messages collect() waits for, `filter` = a `x => boolean` predicate,\n  `schema` = a registered schema name for the value, `timeout` = ms bound on collect().\n- `c.collect()` -> `[{ offset, key, headers, value }]` (blocking, timeout-bounded; returns\n  partials on timeout). `c.pop()` -> the first message. `c.close()`.\n"
      );
   }
}
