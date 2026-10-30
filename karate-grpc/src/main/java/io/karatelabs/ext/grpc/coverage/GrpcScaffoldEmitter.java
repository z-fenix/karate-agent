package io.karatelabs.ext.grpc.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.GrpcBinding;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;
import java.util.LinkedHashMap;
import java.util.Map;

public class GrpcScaffoldEmitter implements ScaffoldEmitter {
   private static final int SCENARIO_CAP = 12;

   public Map<String, String> emit(Contract contract, ScaffoldContext ctx) {
      String protoRef = ctx.var("protoRef", "service.proto");
      String featurePath = ctx.var("featurePath", "checks/" + ScaffoldContext.slug(contract.title()) + ".feature");
      Map<String, String> files = new LinkedHashMap<>();
      files.put("karate-boot.js", bootJs(protoRef));
      files.put(featurePath, featureFile(contract, ctx, protoRef));
      return files;
   }

   private static String bootJs(String protoRef) {
      return "// Scaffolded by Grpc.scaffold from "
         + protoRef
         + " — a runnable gRPC coverage project. The\n// .proto IS the service×method universe (the gRPC analog of an OpenAPI spec). Boot the grpc\n// ext + set the suite-wide endpoint once here (central config) so the feature names only the rest.\nvar grpc = boot.ext('grpc');\ngrpc.host = boot.sysprop('grpc.host', 'localhost');\ngrpc.port = boot.sysprop('grpc.port', '50051');\ngrpc.protoRoots = ['.'];\n\n// Coverage: point cov.grpc at the .proto (a file, a list, or the proto/ DIRECTORY — every *.proto\n// in it is enumerated). The run's grpc-match traffic joins by service/method, so the Coverage tab\n// shows which methods were exercised + the OK/error histogram, with untested methods as honest\n// gaps. Needs the enriched JSONL stream — run with `-f karate:jsonl` (the served console sets it).\nvar cov = boot.ext('coverage');\ncov.grpc = '"
         + protoRef
         + "';\n";
   }

   private static String featureFile(Contract contract, ScaffoldContext ctx, String protoRef) {
      String title = contract.title() == null ? "gRPC" : contract.title();
      StringBuilder sb = new StringBuilder();
      sb.append("@scaffold\n");
      sb.append("Feature: ").append(title).append(" — starter (scaffolded)\n\n");
      sb.append("  # Scaffolded by Grpc.scaffold from ").append(protoRef).append(". These calls become observed\n");
      sb.append("  # gRPC traffic the grpc coverage source scores; methods not called here stay the honest gaps\n");
      sb.append("  # (Coverage.gaps()). Runs against a LIVE server (no mock — start it first). Replace these\n");
      sb.append("  # smoke calls with intent-bearing @req=ID/criterion checks that assert the responses. The\n");
      sb.append("  # @scaffold tag marks it disposable.\n\n");
      sb.append("  Background:\n");
      sb.append("    * def session = karate.channel('grpc')\n");
      sb.append("    * session.proto = '").append(protoRef).append("'\n");
      int emitted = 0;

      for (Operation op : contract.operations()) {
         if (emitted >= 12) {
            sb.append("\n  # … ").append(contract.operations().size() - 12).append(" more method(s) not shown — add scenarios for them to close the gaps.\n");
            break;
         }

         sb.append(scenario(op, ctx));
         emitted++;
      }

      return sb.toString();
   }

   private static String scenario(Operation op, ScaffoldContext ctx) {
      GrpcBinding binding = op.ext().get("binding") instanceof GrpcBinding b ? b : null;
      String service = binding != null ? binding.service() : serviceFromId(op.id());
      String method = binding != null ? binding.method() : methodFromId(op.id());
      boolean clientStream = binding != null && binding.clientStream();
      boolean serverStream = binding != null && binding.serverStream();
      Message input = op.inputs().isEmpty() ? null : (Message)op.inputs().getFirst();
      String sample = ctx.sample(input);
      String arg = sample == null ? "{}" : sample;
      if (clientStream) {
         arg = "[" + arg + "]";
      }

      StringBuilder sb = new StringBuilder();
      sb.append("\n  Scenario: ").append(method).append('\n');
      sb.append("    * session.service = '").append(service).append("'\n");
      sb.append("    * session.method = '").append(method).append("'\n");
      if (serverStream) {
         sb.append("    # server-streaming — set session.count if the server expects a reply count\n");
      }

      sb.append("    * session.send(").append(arg).append(")\n");
      if (serverStream) {
         sb.append("    * def responses = session.collect()\n");
      } else {
         sb.append("    * def response = session.pop()\n");
      }

      return sb.toString();
   }

   private static String serviceFromId(String id) {
      int slash = id.indexOf(47);
      return slash >= 0 ? id.substring(0, slash) : id;
   }

   private static String methodFromId(String id) {
      int slash = id.indexOf(47);
      return slash >= 0 ? id.substring(slash + 1) : id;
   }
}
