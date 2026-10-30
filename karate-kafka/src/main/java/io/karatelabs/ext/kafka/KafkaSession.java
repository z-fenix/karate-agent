package io.karatelabs.ext.kafka;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KafkaSession extends JsDispatch {
   private final io.karatelabs.ext.kafka.KafkaChannel channel;

   KafkaSession(io.karatelabs.ext.kafka.KafkaChannel channel, String bootstrap, String schemaRegistry, io.karatelabs.ext.kafka.Kafka kafka) {
      this.channel = channel;
      this.val("bootstrap", bootstrap);
      this.val("schemaRegistry", schemaRegistry);
      this.fn("register", a -> {
         if (JsApi.arg(a, 0) instanceof Map<?, ?> m) {
            channel.register(resolvePaths((Map<String, Object>)m, kafka));
         }

         return null;
      });
      this.fn("consumer", a -> new io.karatelabs.ext.kafka.KafkaConsumerHandle(channel.consumer()));
      this.fn("producer", a -> new io.karatelabs.ext.kafka.KafkaProducerHandle(channel.producer()));
      this.fn("close", a -> {
         channel.afterScenario();
         return null;
      });
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   private static Map<String, Object> resolvePaths(Map<String, Object> data, io.karatelabs.ext.kafka.Kafka kafka) {
      Map<String, Object> out = new LinkedHashMap<>(data);
      if (data.get("path") instanceof String p) {
         out.put("path", kafka.ref(p));
      }

      if (data.get("roots") instanceof List<?> rs) {
         List<Object> resolved = new ArrayList<>(rs.size());

         for (Object r : rs) {
            resolved.add(r instanceof String s ? kafka.ref(s) : r);
         }

         out.put("roots", resolved);
      }

      return out;
   }

   public Collection<String> jsKeys() {
      return List.of("bootstrap", "schemaRegistry");
   }

   public String jsKind() {
      return "kafka";
   }

   private static Object help() {
      return JsApi.markdown(
         "# kafka session — a live broker connection (kind: kafka)\n\n- `k.register({ name, path })` — register a schema (`.avsc` or `.proto`); `path` resolves\n  classpath:/file. Optional `message` (proto message name), `roots` (proto import roots).\n- `k.consumer()` -> a `kafka-consumer` handle — `c.start({topic,count?,filter?,schema?,timeout?})`\n  then `c.collect()` / `c.pop()`.\n- `k.producer()` -> a `kafka-producer` handle — `p.send({topic,schema?,key?,value,headers?})`.\n- `k.close()` — tear the session (and its consumers/producers) down.\n"
      );
   }
}
