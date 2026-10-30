package io.karatelabs.ext.kafka;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Kafka extends JsDispatch {
   private Path root;

   public Kafka() {
      this.fn("connect", a -> this.connect(JsApi.arg(a, 0)));
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   public io.karatelabs.ext.kafka.Kafka withRoot(Path root) {
      this.root = root;
      return this;
   }

   String ref(String s) {
      return s == null ? null : MaxPaths.resolveRef(this.root, s);
   }

   Object connect(Object optsArg) {
      if (optsArg instanceof Map<?, ?> opts) {
         io.karatelabs.ext.kafka.KafkaExt ext = new io.karatelabs.ext.kafka.KafkaExt();
         ext.putMember("bootstrap", opts.get("bootstrap"));
         if (opts.get("schemaRegistry") != null) {
            ext.putMember("schemaRegistry", opts.get("schemaRegistry"));
         }

         if (opts.get("ssl") instanceof Map<?, ?> ssl) {
            ext.putMember("ssl", this.resolveSsl((Map<String, Object>)ssl));
         }

         if (opts.get("props") instanceof Map) {
            ext.putMember("props", opts.get("props"));
         }

         if (opts.get("bootstrap") == null) {
            return JsApi.missing("Kafka.connect needs a bootstrap server, e.g. { bootstrap:'127.0.0.1:29092' }");
         } else {
            io.karatelabs.ext.kafka.KafkaChannel channel = new io.karatelabs.ext.kafka.KafkaChannel(ext.toClientConfig(null, null));
            return new io.karatelabs.ext.kafka.KafkaSession(
               channel, opts.get("bootstrap").toString(), opts.get("schemaRegistry") != null ? opts.get("schemaRegistry").toString() : null, this
            );
         }
      } else {
         return JsApi.missing(
            "Kafka.connect(opts) requires an object, e.g. { bootstrap:'127.0.0.1:29092', schemaRegistry?:'http://localhost:8081', ssl?:{...}, props?:{...} }"
         );
      }
   }

   private Map<String, Object> resolveSsl(Map<String, Object> ssl) {
      Map<String, Object> out = new LinkedHashMap<>(ssl);
      if (ssl.get("truststore") instanceof String t) {
         out.put("truststore", this.ref(t));
      }

      if (ssl.get("keystore") instanceof String k) {
         out.put("keystore", this.ref(k));
      }

      return out;
   }

   private static Object help() {
      return JsApi.markdown(
         "# Kafka — probe a live broker (kind: kafka)\n\nConnect, register a schema, produce + consume to learn the shapes, then crystallize a check.\n\n## Kafka.connect(opts) -> a live `k` handle\n```\nvar k = Kafka.connect({ bootstrap: '127.0.0.1:29092',\n                        schemaRegistry: 'http://localhost:8081' })  // schemaRegistry optional\nk.register({ name: 'hello', path: 'hello.avsc' })                   // or a .proto\n```\n\n## consume + produce (start the consumer first so it doesn't miss the message)\n```\nvar c = k.consumer()\nc.start({ topic: 'test', count: 2, filter: x => x.key != 'zero' })  // non-blocking subscribe\n\nvar p = k.producer()\np.send({ topic: 'test', schema: 'hello', key: 'first',\n         value: { message: 'hi' }, headers: { foo: 'bar' } })\n\nc.collect()   // blocking drain w/ timeout -> [{ offset, key, headers, value }]\nk.close()\n```\nWithout `schema`, the value is sent/read as raw JSON bytes. `c` is kind `kafka-consumer`,\n`p` is kind `kafka-producer`.\n\nThe same project drives a durable suite too — author a `.feature`/`.js` over\n`karate.channel('kafka')` and run it with `Runner.run(...)`.\n"
      );
   }
}
