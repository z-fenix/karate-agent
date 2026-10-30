package io.karatelabs.ext.grpc;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GrpcSession extends JsDispatch {
   private final io.karatelabs.ext.grpc.GrpcConsumer template;
   private final List<Map<String, Object>> exchanges = Collections.synchronizedList(new ArrayList<>());
   private volatile Map<String, String> lastMetadataResponse;
   private volatile String lastStatus;
   private volatile Map<String, Object> lastStatusDetails;

   GrpcSession(io.karatelabs.ext.grpc.GrpcConsumer template) {
      this.template = template;
      this.val("host", template.host);
      this.val("port", template.port);
      this.val("service", template.service);
      this.val("proto", template.proto);
      this.val("kind", "grpc");
      this.lazy("exchanges", () -> new ArrayList<>(this.exchanges));
      this.lazy("metadataResponse", () -> this.lastMetadataResponse);
      this.lazy("status", () -> this.lastStatus);
      this.lazy("statusDetails", () -> this.lastStatusDetails);
      io.karatelabs.ext.grpc.GrpcCatalog catalog = template.catalog();
      this.fn("methods", a -> catalog.methods(template.service));
      this.fn("describe", a -> catalog.describe(JsApi.str(a, 0)));
      this.fn("call", a -> this.call(JsApi.str(a, 0), JsApi.arg(a, 1), opts(JsApi.arg(a, 2))));
      this.fn("close", a -> null);
      this.fn("help", a -> JsApi.helpTopic(help(), a));
   }

   private Object call(String method, Object msg, Map<String, Object> opts) {
      io.karatelabs.ext.grpc.GrpcConsumer c = this.template.copyConfig();

      Object var5;
      try {
         var5 = c.call(method, msg, opts);
      } finally {
         this.lastMetadataResponse = c.metadataResponse;
         this.lastStatus = c.lastStatus != null ? c.lastStatus : "OK";
         this.lastStatusDetails = c.lastStatusDetails;
         this.exchanges.addAll(c.exchanges);
         c.stop();
      }

      return var5;
   }

   private static Map<String, Object> opts(Object arg) {
      return (Map<String, Object>)(arg instanceof Map<?, ?> m ? m : null);
   }

   public Collection<String> jsKeys() {
      return List.of("host", "port", "service", "proto", "kind", "exchanges", "metadataResponse", "status", "statusDetails");
   }

   public String jsKind() {
      return "grpc";
   }

   private static Object help() {
      return JsApi.markdown(
         "# grpc session — a live gRPC client (kind: grpc)\n\nYou hold a live handle `g` (from `Grpc.connect(...)`). One verb drives every RPC; the\nstreaming mode is read from the proto (or server reflection), so there is nothing to set.\n\n## g.methods() — the catalog (call this first to stop guessing)\nReturns every rpc on the service (read from the proto, or off the server via reflection):\n`[{ method, kind, requestType, responseType, requestStreaming, responseStreaming }]`\nwhere `kind` is `unary` | `server-streaming` | `client-streaming` | `bidi`.\n\n## g.describe(type) — the field-level shape of a message\n`g.describe('HelloRequest')` -> `{ message, fields:[{ name, type, number, repeated?, values? }] }`.\n`type` is a message name from `g.methods()` (`requestType`/`responseType`); message/enum fields\nread as `message:Info` / `enum:Color` (describe those next), maps as `map<k,v>`.\n\n## g.call(method, message, opts?)\n- **unary** → returns the response object\n- **server-streaming** → returns the collected list (blocking, timeout-bounded)\n- **client-streaming** → pass an array of messages → returns the single response\n- **bidi** → pass an array of messages → returns the collected list\n```\ng.call('Hello', { name: 'John' })           // unary\ng.call('LotsOfReplies', { name: 'John' })    // server-stream → [ … ]\ng.call('BidiHello', [a, b, c])               // bidi → [ … ]\ng.call('Hello', { name: 'J' }, { metadata: { authorization: 'x' } })\n```\n\n## error path — a non-OK call THROWS; the typed details stay on the handle\nA failed RPC throws (the message names the coarse code). Catch it, then read the structured\ngRPC error off the handle — `g.status` is the code, `g.statusDetails` the decoded\n`google.rpc.Status` (`{ code, message, details:[…] }`, e.g. a `BadRequest`/`ErrorInfo`):\n```\ntry { g.call('Hello', { name: '' }) } catch (e) { }   // INVALID_ARGUMENT\ng.status         // 'INVALID_ARGUMENT'\ng.statusDetails  // { code: 3, message: 'name is required', details: [ {'@type':'…BadRequest', …} ] }\n```\n\n## read-only state\n- `g.status` — the gRPC status of the last call (`OK` / `INVALID_ARGUMENT` / …; null before the first)\n- `g.statusDetails` — the last call's rich-error model (`google.rpc.Status`), or null if none\n- `g.exchanges` — captured calls `[{method,request,responses,status,durationMs}]`\n- `g.metadataResponse` — response headers/trailers of the last call\n- `g.close()` — shut the session down\n"
      );
   }
}
