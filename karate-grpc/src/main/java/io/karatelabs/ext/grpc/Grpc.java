package io.karatelabs.ext.grpc;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.grpc.coverage.GrpcContractLoader;
import io.karatelabs.ext.grpc.coverage.GrpcScaffoldEmitter;
import io.karatelabs.ext.scaffold.ContractScaffold;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Grpc extends JsDispatch {
   private static final long DEFAULT_TIMEOUT_MS = 30000L;
   private Path root;

   public Grpc() {
      this.fn("connect", a -> this.connect(JsApi.arg(a, 0)));
      this.fn("diff", a -> this.diff(JsApi.arg(a, 0)));
      this.fn("scaffold", a -> this.scaffold(JsApi.arg(a, 0)));
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   public io.karatelabs.ext.grpc.Grpc withRoot(Path root) {
      this.root = root;
      return this;
   }

   private String ref(String s) {
      return s == null ? null : MaxPaths.resolveRef(this.root, s);
   }

   private List<String> refs(List<String> in) {
      if (in == null) {
         return null;
      } else {
         List<String> out = new ArrayList<>(in.size());

         for (String s : in) {
            out.add(this.ref(s));
         }

         return out;
      }
   }

   Object scaffold(Object optsArg) {
      Main.requireProduct("grpc.scaffold", new String[]{"grpc"});
      if (!(optsArg instanceof Map<?, ?> opts)) {
         return JsApi.missing(
            "Grpc.scaffold(opts) requires an object, e.g. { proto:'proto/hello.proto', out?:'work/grpc', feature?:'hello', protoRoots?:['.'] }"
         );
      } else {
         Object protoArg = opts.get("proto");
         if (protoArg != null && !protoArg.toString().isBlank()) {
            String proto = this.ref(protoArg.toString());

            Contract contract;
            try {
               contract = new GrpcContractLoader().load(List.of(Path.of(proto)), null);
            } catch (RuntimeException var13) {
               return JsApi.missing("Grpc.scaffold could not parse proto '" + proto + "': " + var13.getMessage());
            }

            String protoRef = baseName(proto);
            String featureName = featureName(opts.get("feature"), contract.title());
            String featurePath = "checks/" + featureName;
            String dir = opts.get("out") instanceof String s && !s.isBlank() ? s : null;
            ScaffoldContext ctx = ScaffoldContext.builder(this.root)
               .outDir(dir)
               .overwrite(opts.get("overwrite"))
               .copyArtifact(proto, protoRef)
               .var("protoRef", protoRef)
               .var("featurePath", featurePath)
               .writtenHint(
                  dir == null
                     ? null
                     : "wrote a runnable gRPC project (`out` is the resolved project dir, `written` lists the files) — start the server, then `karate run -f karate:jsonl "
                        + featurePath
                        + "` from there (the `-f karate:jsonl` is REQUIRED — the grpc coverage join reads that event stream). Replace the smoke scenarios with intent-bearing checks that assert the responses."
               )
               .filesHint(
                  "write each entry of `files` to its named path (pass {out:'dir'} to have scaffold write them + copy the proto in), start the server, then `karate run -f karate:jsonl "
                     + featurePath
                     + "` (the `-f karate:jsonl` is required — the grpc coverage join reads that event stream)"
               )
               .build();
            return new ContractScaffold().scaffold(contract, new GrpcScaffoldEmitter(), ctx);
         } else {
            return JsApi.missing("Grpc.scaffold needs `proto` (the path to a .proto — the service×method universe)");
         }
      }
   }

   private static String featureName(Object featureOpt, String title) {
      String base = featureOpt instanceof String s && !s.isBlank() ? s : ScaffoldContext.slug(title);
      if (base.isBlank()) {
         base = "starter";
      }

      return base.endsWith(".feature") ? base : base + ".feature";
   }

   private static String baseName(String path) {
      String p = path;
      int q = path.indexOf(63);
      if (q >= 0) {
         p = path.substring(0, q);
      }

      int slash = Math.max(p.lastIndexOf(47), p.lastIndexOf(92));
      return slash >= 0 ? p.substring(slash + 1) : p;
   }

   Object diff(Object optsArg) {
      if (optsArg instanceof Map<?, ?> opts) {
         Object oldPath = opts.get("old");
         Object newPath = opts.get("new");
         if (oldPath != null && newPath != null) {
            List<String> protoRoots = this.refs((List<String>)(opts.get("protoRoots") instanceof List<?> pr ? pr : null));
            ProtoFileElement oldFile = io.karatelabs.ext.grpc.ProtobufUtils.toSchema(this.ref(oldPath.toString()), protoRoots).rawSchema();
            ProtoFileElement newFile = io.karatelabs.ext.grpc.ProtobufUtils.toSchema(this.ref(newPath.toString()), protoRoots).rawSchema();
            return io.karatelabs.ext.grpc.GrpcDiff.report(io.karatelabs.ext.grpc.GrpcDiff.diff(oldFile, newFile));
         } else {
            return JsApi.missing("Grpc.diff needs `old` and `new` (paths to the two .proto versions)");
         }
      } else {
         return JsApi.missing("Grpc.diff(opts) requires an object, e.g. { old:'proto/hello-v1.proto', new:'proto/hello-v2.proto', protoRoots?:['.'] }");
      }
   }

   Object connect(Object optsArg) {
      if (optsArg instanceof Map<?, ?> opts) {
         io.karatelabs.ext.grpc.GrpcConsumer c = new io.karatelabs.ext.grpc.GrpcConsumer(new LinkedHashMap<>());
         if (opts.get("host") != null) {
            c.host = opts.get("host").toString();
         }

         if (opts.get("port") instanceof Number p) {
            c.port = p.intValue();
         }

         if (opts.get("proto") != null) {
            c.proto = this.ref(opts.get("proto").toString());
         }

         if (opts.get("service") != null) {
            c.service = opts.get("service").toString();
         }

         if (opts.get("protoRoots") instanceof List<?> pr) {
            c.protoRoots = this.refs((List<String>)pr);
         }

         if (opts.get("metadata") instanceof Map<?, ?> md) {
            c.metadataRequest = (Map<String, String>)md;
         }

         if (opts.get("tls") instanceof Map<?, ?> tls) {
            if (tls.get("trustCert") != null) {
               c.trustCert = this.ref(tls.get("trustCert").toString());
            }

            if (tls.get("clientCert") != null) {
               c.clientCert = this.ref(tls.get("clientCert").toString());
            }

            if (tls.get("clientKey") != null) {
               c.clientKey = this.ref(tls.get("clientKey").toString());
            }
         }

         c.timeout = opts.get("timeout") instanceof Number t ? t.longValue() : 30000L;
         return c.host != null && c.port != 0 && c.service != null
            ? new io.karatelabs.ext.grpc.GrpcSession(c)
            : JsApi.missing("Grpc.connect needs host, port and service (proto is optional — omit it to discover the service via server reflection)");
      } else {
         return JsApi.missing(
            "Grpc.connect(opts) requires an object, e.g. { host:'localhost', port:50051, service:'HelloService', proto?:'proto/hello.proto', protoRoots?:['.'], tls?:{trustCert,clientCert,clientKey}, metadata?:{...}, timeout?:30000 }  (omit proto to discover via server reflection)"
         );
      }
   }

   private static Object help() {
      return JsApi.markdown(
         "# Grpc — probe a live gRPC service (kind: grpc)\n\nConnect to a real service, drive its RPCs to learn the shapes, then crystallize a check.\n\n## Grpc.connect(opts) -> a live `g` handle\n```\nvar g = Grpc.connect({\n  host: 'localhost', port: 50051,\n  service: 'HelloService',\n  proto: 'proto/hello.proto', protoRoots: ['.'],  // OPTIONAL — omit to use server reflection\n  tls: { trustCert: '...', clientCert: '...', clientKey: '...' },  // optional mTLS\n  metadata: { authorization: 'secret' },                          // optional\n  timeout: 30000 })                                               // optional collect bound\n```\nWith no `proto`, the service's shapes are discovered off the live server via\n`grpc.reflection.v1.ServerReflection` — `g.methods()/describe()/call()` work the same. If the\nserver has reflection disabled, the error says so; pass `proto:` to use a local `.proto` instead.\n\n## discover the catalog, then call (one verb, dispatched on the proto's method type)\n```\ng.methods()                                  // [{ method, kind, requestType, responseType }] — no guessing\ng.call('Hello', { name: 'John' })            // unary  -> { message: 'hello John' }\ng.call('LotsOfReplies', { name: 'John' })    // server-stream -> [ … ]\ng.call('BidiHello', [a, b, c])               // bidi   -> [ … ]\n```\n`g.exchanges` (captured calls), `g.metadataResponse` (last trailers), `g.close()`.\n\n## Grpc.diff({ old, new, protoRoots? }) -> breaking-change report\nCompare two `.proto` versions (a maintenance/CI check — no server needed):\n```\nGrpc.diff({ old:'proto/hello-v1.proto', new:'proto/hello-v2.proto' })\n// -> { ok:false, summary:{breaking,warnings,compatible},\n//      breaking:[{ kind:'FIELD_REMOVED', path:'HelloRequest.name', detail:'…' }, …],\n//      warnings:[…], compatible:[…] }\n```\nMatched by wire identity (fields/enum-values by number, methods/messages by name). `ok` is\ntrue iff there are no breaking changes — gate a release on it.\n\n## Grpc.scaffold({ proto, out?, feature? }) -> a runnable gRPC project\nCold-start authoring — a `.proto` becomes a runnable coverage project (no server needed to\ngenerate; the generated feature runs against a LIVE server):\n```\nGrpc.scaffold({ proto:'proto/hello.proto', out:'work/grpc' })\n// -> writes karate-boot.js (boot grpc ext + cov.grpc) + checks/<name>.feature\n//    (one scenario per RPC, sampled request), and copies the proto in. No mock —\n//    start the server, then `karate run -f karate:jsonl checks/<name>.feature`.\n```\nOmit `out` to get the `files` map back without writing. There is no gRPC mock yet — the\nfeature drives a live server.\n\nThe same project drives a durable suite too — author a `.feature`/`.js` over\n`karate.channel('grpc')` and run it with `Runner.run(...)`. The live `g.call(...)` handle\nabove takes streaming as an array arg; the feature-level channel DSL is different — for\nclient-streaming and bidi in a `.feature` (`session.stream` / `count` / `flush()` /\n`collect()`), see `Skill.help('grpc')`.\n"
      );
   }
}
