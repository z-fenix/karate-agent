package io.karatelabs.ext.asyncapi.jsapi;

import io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.KafkaAction;
import io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader;
import io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry;
import io.karatelabs.ext.scaffold.ContractScaffold;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

public class Asyncapi extends JsDispatch {
   private Path root;
   private Path classpathRoot;
   private boolean noProject;

   public Asyncapi() {
      this.fn("scaffold", var1 -> this.scaffold(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("reverse", var1 -> this.reverse(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public Asyncapi withRoot(Path var1) {
      return this.withRoot(var1, var1);
   }

   public Asyncapi withRoot(Path var1, Path var2) {
      this.root = var1;
      this.classpathRoot = var2;
      return this;
   }

   public Asyncapi withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   Object reverse(Object var1, Object var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Asyncapi.reverse");
      } else {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("asyncapi.reverse", "openapi");
         if (var1 instanceof String var3 && !var3.isBlank()) {
            Map<java.lang.String, Object> var4 = var2 instanceof Map var5 ? var5 : Map.of();
            Path var16 = this.resolveJsonl(var3);
            if (var16 == null) {
               return JsApi.missing(
                  "Asyncapi.reverse could not find a karate-events.jsonl under '"
                     + var3
                     + "' — pass the .jsonl file or the run dir holding karate-json/karate-events.jsonl (run the kafka features with -f karate:jsonl — auto-enabled when boot.ext('coverage') is active — which records the kafka-match events the reverse reads)"
               );
            } else {
               ArrayList<io.karatelabs.ext.coverage.KafkaAction> var6 = new ArrayList();

               try {
                  for (Action var8 : new JsonlRunEvidenceReader(List.of(var16)).a()) {
                     if (var8 instanceof KafkaAction var9) {
                        var6.add(var9);
                     }
                  }
               } catch (IOException var15) {
                  return JsApi.missing("Asyncapi.reverse could not read '" + var16 + "': " + var15.getMessage());
               }

               if (var6.isEmpty()) {
                  return JsApi.missing(
                     "Asyncapi.reverse found no kafka traffic in '"
                        + var16
                        + "' — the doc is derived from observed produce/consume events; run the kafka features with the event stream on first (-f karate:jsonl, auto-enabled when boot.ext('coverage') is active)"
                  );
               } else {
                  var var17 = io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.a(var6, str(var4.get("title")), str(var4.get("version")), str(var4.get("host")));
                  String var18 = yaml(var17);
                  LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
                  var19.put("doc", var18);
                  int var10 = io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.a(var17);
                  var19.put("channels", var10);
                  var19.put("operations", io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.b(var17));
                  String var11 = var4.get("out") instanceof String var12 && !var12.isBlank() ? var12 : null;
                  if (var11 != null) {
                     Path var20 = MaxPaths.resolvePath(this.root, var11);

                     try {
                        Path var21 = var20.toAbsolutePath().getParent();
                        if (var21 != null) {
                           Files.createDirectories(var21);
                        }

                        Files.writeString(var20, var18);
                     } catch (IOException var14) {
                        return JsApi.missing("Asyncapi.reverse could not write '" + var11 + "': " + var14.getMessage());
                     }

                     var19.put("out", var11);
                     var19.put("written", true);
                     var19.put(
                        "hint",
                        "wrote an AsyncAPI doc to "
                           + var11
                           + " ("
                           + var10
                           + " channel(s)). Review the inferred payload schemas, then drive it forward — Asyncapi.scaffold('"
                           + var11
                           + "') or point cov.asyncapi at it."
                     );
                  } else {
                     var19.put(
                        "hint",
                        "`doc` is an AsyncAPI v3 YAML synthesized from observed kafka traffic ("
                           + var10
                           + " channel(s)). Pass {out:'asyncapi.yml'} to write it, then scaffold it forward or set cov.asyncapi."
                     );
                  }

                  return var19;
               }
            }
         } else {
            return JsApi.missing(
               "Asyncapi.reverse(source, {title?, version?, host?, out?}) requires a path to a karate-events.jsonl or its run dir, e.g. Asyncapi.reverse('target/karate-reports', {out:'asyncapi.yml'})"
            );
         }
      }
   }

   private Path resolveJsonl(String var1) {
      Path var2 = MaxPaths.resolvePath(this.root, var1);
      if (Files.isRegularFile(var2)) {
         return var2;
      } else {
         Path var3 = var2.resolve("karate-json").resolve("karate-events.jsonl");
         if (Files.isRegularFile(var3)) {
            return var3;
         } else {
            Path var4 = var2.resolve("karate-events.jsonl");
            return Files.isRegularFile(var4) ? var4 : null;
         }
      }
   }

   private static String str(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static String yaml(Object var0) {
      DumperOptions var1 = new DumperOptions();
      var1.setDefaultFlowStyle(FlowStyle.BLOCK);
      var1.setIndent(2);
      var1.setPrettyFlow(true);
      var1.setSplitLines(false);
      return new Yaml(var1).dump(var0);
   }

   Object scaffold(Object var1, Object var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Asyncapi.scaffold");
      } else {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("asyncapi.scaffold", "openapi");
         if (var1 instanceof String var3 && !var3.isBlank()) {
            Map<java.lang.String, Object> var4 = var2 instanceof Map var5 ? var5 : Map.of();
            String var17 = MaxPaths.resolveRef(this.root, var3);

            Contract var6;
            try {
               var6 = io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.a(Path.of(var17));
            } catch (RuntimeException var16) {
               return JsApi.missing("Asyncapi.scaffold could not parse '" + var3 + "': " + var16.getMessage());
            }

            ScaffoldEmitter var7 = io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry.b(var6.protocol());
            if (var7 == null) {
               return JsApi.missing(
                  "Asyncapi.scaffold has no scaffold emitter for protocol "
                     + var6.protocol()
                     + " — the "
                     + var6.protocol().name().toLowerCase()
                     + " leaf is not on the classpath (AsyncAPI scaffold needs the async jar; the lean agent jar carries OpenAPI only)"
               );
            } else {
               String var8 = baseName(var3);
               String var9 = featureName(var4.get("feature"), var6.title());
               String var10 = "checks/" + var9;
               String var11 = var4.get("out") instanceof String var12 && !var12.isBlank() ? var12 : null;
               ScaffoldContext.Builder var18 = ScaffoldContext.builder(this.root).outDir(var11).overwrite(var4.get("overwrite")).copyArtifact(var17, var8);

               for (String var14 : schemaFiles(var6)) {
                  Path var15 = Path.of(var17).toAbsolutePath().getParent().resolve(var14);
                  var18.copyArtifact(var15.toString(), baseName(var14));
               }

               ScaffoldContext var20 = var18.var("docRef", var8)
                  .var("featurePath", var10)
                  .writtenHint(
                     var11 == null
                        ? null
                        : "wrote a runnable project (`out` is the resolved project dir, `written` lists the files) — start the broker, then `karate run "
                           + var10
                           + "` from there (boot.ext('coverage') auto-enables the event stream the asyncapi coverage join reads). Replace the smoke round-trips with intent-bearing checks."
                  )
                  .filesHint(
                     "write each entry of `files` to its named path (pass {out:'dir'} to have scaffold write them + copy the doc in), start the broker, then `karate run "
                        + var10
                        + "` (boot.ext('coverage') auto-enables the event stream the asyncapi coverage join reads)"
                  )
                  .build();
               return new ContractScaffold().scaffold(var6, var7, var20);
            }
         } else {
            return JsApi.missing(
               "Asyncapi.scaffold(doc, {out?, feature?}) requires an AsyncAPI doc path String, e.g. Asyncapi.scaffold('asyncapi.yml', {out:'work/kafka'})"
            );
         }
      }
   }

   private static String featureName(Object var0, String var1) {
      String var2 = var0 instanceof String var3 && !var3.isBlank() ? var3 : ScaffoldContext.slug(var1);
      if (var2.isBlank()) {
         var2 = "starter";
      }

      return var2.endsWith(".feature") ? var2 : var2 + ".feature";
   }

   private static List<String> schemaFiles(Contract var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (Operation var3 : var0.operations()) {
         var var4 = new ArrayList<>(var3.inputs());
         var4.addAll(var3.outputs());

         for (Message var6 : (Iterable<Message>) (Iterable<?>) (var4)) {
            if (var6.ext().get("schemaFile") instanceof String var7 && !var7.isBlank()) {
               var1.add(var7);
            }
         }
      }

      return new ArrayList<>(var1);
   }

   private static String baseName(String var0) {
      String var1 = var0;
      int var2 = var0.indexOf(63);
      if (var2 >= 0) {
         var1 = var0.substring(0, var2);
      }

      int var3 = Math.max(var1.lastIndexOf(47), var1.lastIndexOf(92));
      return var3 >= 0 ? var1.substring(var3 + 1) : var1;
   }

   private static Object help() {
      return JsApi.markdown(
         "# Asyncapi — author from an AsyncAPI (event) doc: scaffold a runnable project\n\nThe event-side sibling of `Openapi`. Turns an AsyncAPI v2/v3 doc into a runnable coverage project\n(the channel×direction universe + the JSON-Schema payload field axes). Async-jar only — it needs\nthe protocol leaf (Kafka today) for the step emission.\n\n## Asyncapi.scaffold(doc, { out?, feature? }) -> a runnable project\n```\nAsyncapi.scaffold('asyncapi.yml', { out:'work/kafka' })\n// -> writes karate-boot.js (boot kafka ext + cov.asyncapi) + checks/<name>.feature\n//    (per channel/op: produce a sampled JSON message + consume + assert — a broker\n//    round-trip, no mock), and copies the doc in. Omit `out` for the `files` map only.\n```\nThen start the broker and `karate run checks/<name>.feature` (boot.ext('coverage') auto-enables\nthe event stream the asyncapi coverage join reads). JSON payloads need no schema\nregistry; the sampled message + its field axes (enum/bool/…) come straight from the doc.\n\n## Asyncapi.reverse(source, { title?, version?, host?, out? }) -> a doc from observed traffic\n```\nAsyncapi.reverse('target/karate-reports', { out:'asyncapi.yml' })\n// -> reads the recorded kafka-match events, groups by topic, INFERS each produced payload's\n//    schema (no registry), and emits an AsyncAPI v3 doc. `source` is a karate-events.jsonl or\n//    the run dir holding karate-json/karate-events.jsonl. Omit `out` for the YAML in `doc`.\n```\nThe reverse of `scaffold` — turn what your tests actually produced into a documented contract, then\ndrive it forward (`Asyncapi.scaffold` / `cov.asyncapi`). Max-resident (reads the event stream).\n"
      );
   }
}
