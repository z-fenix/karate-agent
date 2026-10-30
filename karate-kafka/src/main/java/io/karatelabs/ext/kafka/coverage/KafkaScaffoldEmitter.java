package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Interaction;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KafkaScaffoldEmitter implements ScaffoldEmitter {
   private static final int SCENARIO_CAP = 12;

   public Map<String, String> emit(Contract contract, ScaffoldContext ctx) {
      String docRef = ctx.var("docRef", "asyncapi.yml");
      String featurePath = ctx.var("featurePath", "checks/" + ScaffoldContext.slug(contract.title()) + ".feature");
      Map<String, String> files = new LinkedHashMap<>();
      files.put("karate-boot.js", bootJs(docRef));
      files.put(featurePath, featureFile(contract, ctx, docRef));
      return files;
   }

   private static String bootJs(String docRef) {
      return "// Scaffolded by AsyncApi.scaffold from "
         + docRef
         + " — a runnable Kafka coverage project. The\n// AsyncAPI doc IS the channel×direction universe (the event-side analog of an OpenAPI spec).\n// Boot the kafka ext + set the broker once here (central config) so the feature names only the rest.\nvar kafka = boot.ext('kafka');\nkafka.bootstrap = boot.sysprop('kafka.bootstrap', 'localhost:9092');\n// JSON payloads need no schemaRegistry. Avro payloads round-trip registryless too — the feature\n// channel.register({name, path:'<schema>.avsc'})s the local schema (raw Avro binary). Set\n// kafka.schemaRegistry only for Confluent wire-format (magic-byte) interop with other producers.\n\n// Coverage: the AsyncAPI doc declares the topic×direction universe + the JSON-Schema payload field\n// axes. The run's kafka-match traffic joins by topic/direction, so the Coverage tab shows which\n// channels were exercised + grades the payload field dimensions, with untested channels as honest\n// gaps. Needs the enriched JSONL stream — run with `-f karate:jsonl` (the served console sets it).\nvar cov = boot.ext('coverage');\ncov.asyncapi = '"
         + docRef
         + "';\n";
   }

   private static String featureFile(Contract contract, ScaffoldContext ctx, String docRef) {
      String title = contract.title() == null ? "Kafka" : contract.title();
      StringBuilder sb = new StringBuilder();
      sb.append("@scaffold\n");
      sb.append("Feature: ").append(title).append(" — starter (scaffolded)\n\n");
      sb.append("  # Scaffolded by AsyncApi.scaffold from ").append(docRef).append(". Each scenario round-trips a\n");
      sb.append("  # sampled JSON message against the broker (produce + consume + assert) — observed kafka traffic\n");
      sb.append("  # the asyncapi coverage source scores; channels not exercised stay the honest gaps, and the\n");
      sb.append("  # payload field axes (enum/bool/…) grade from what you produce. No mock — start the broker first.\n");
      sb.append("  # Replace these smoke round-trips with intent-bearing @req=ID/criterion checks. @scaffold = disposable.\n\n");
      sb.append("  Background:\n");
      sb.append("    * def channel = karate.channel('kafka')\n");
      int emitted = 0;
      int skipped = 0;

      for (Operation op : contract.operations()) {
         String scenario = scenario(op, ctx);
         if (scenario == null) {
            skipped++;
         } else if (emitted >= 12) {
            skipped++;
         } else {
            sb.append(scenario);
            emitted++;
         }
      }

      if (skipped > 0) {
         sb.append("\n  # … ").append(skipped).append(" channel/operation(s) not shown (capped, or a payload with no\n");
         sb.append("  # sample-able JSON schema) — add scenarios for them to close the gaps.\n");
      }

      return sb.toString();
   }

   private static String scenario(Operation op, ScaffoldContext ctx) {
      String topic = op.address().primary();
      String direction = op.address().qualifier();
      boolean publish = op.interaction() == Interaction.PUBLISH || op.interaction() == Interaction.REQUEST_RESPONSE || "publish".equals(direction);
      List<Message> values = publish ? op.inputs() : op.outputs();
      if (values.isEmpty()) {
         values = publish ? op.outputs() : op.inputs();
      }

      if (values.isEmpty()) {
         return null;
      } else {
         Message value = values.getFirst();
         String sample = ctx.sample(value);
         if (sample == null) {
            return null;
         } else {
            String schemaFile = "avro".equals(value.ext().get("format")) ? str(value.ext().get("schemaFile")) : null;
            String codecName = schemaFile == null ? null : registerName(schemaFile);
            StringBuilder sb = new StringBuilder();
            sb.append("\n  Scenario: ").append(topic).append(' ').append(direction).append('\n');
            if (codecName != null) {
               sb.append("    * channel.register({ name: '").append(codecName).append("', path: '").append(baseName(schemaFile)).append("' })\n");
            }

            sb.append("    * def consumer = channel.consumer()\n");
            sb.append("    * consumer.topic = '").append(topic).append("'\n");
            if (codecName != null) {
               sb.append("    * consumer.schema = '").append(codecName).append("'\n");
            }

            sb.append("    * consumer.count = 1\n");
            sb.append("    * consumer.start()\n");
            sb.append("    * def producer = channel.producer()\n");
            sb.append("    * producer.topic = '").append(topic).append("'\n");
            if (codecName != null) {
               sb.append("    * producer.schema = '").append(codecName).append("'\n");
            }

            sb.append("    * producer.value = ").append(sample).append('\n');
            sb.append("    * producer.send()\n");
            sb.append("    * def rec = consumer.pop()\n");
            sb.append("    * match rec.value == ").append(sample).append('\n');
            return sb.toString();
         }
      }
   }

   private static String registerName(String schemaFile) {
      String base = baseName(schemaFile);
      int dot = base.lastIndexOf(46);
      return dot > 0 ? base.substring(0, dot) : base;
   }

   private static String baseName(String path) {
      int slash = Math.max(path.lastIndexOf(47), path.lastIndexOf(92));
      return slash >= 0 ? path.substring(slash + 1) : path;
   }

   private static String str(Object o) {
      return o == null ? null : String.valueOf(o);
   }
}
