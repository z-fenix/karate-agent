package io.karatelabs.ext.openapi;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecBinding extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(SpecBinding.class);
   private final List<Supplier<List<String>>> sources = new CopyOnWriteArrayList<>();
   private final Map<String, OpenApiDocument> cache = new ConcurrentHashMap<>();

   public SpecBinding(Supplier<List<String>> var1) {
      this.sources.add(var1);
      this.fn("schema", var1x -> this.schema(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public SpecBinding addSource(Supplier<List<String>> var1) {
      this.sources.add(var1);
      return this;
   }

   public List<String> paths() {
      return this.resolvedPaths();
   }

   private List<String> resolvedPaths() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Supplier var3 : this.sources) {
         for (String var5 : (Iterable<String>) (Iterable<?>) ((List)var3.get())) {
            if (var5 != null && !var1.contains(var5)) {
               var1.add(var5);
            }
         }
      }

      return var1;
   }

   private Object schema(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         var var4 = this.resolvedPaths();
         if (var4.isEmpty()) {
            throw new RuntimeException(
               "no API spec is configured — set cov.openapi in karate-boot.js (or boot.ext('openapi').path) so spec.schema() knows the contract"
            );
         } else {
            String var5 = var2 == null ? null : String.valueOf(var2 instanceof Number var6 ? var6.intValue() : var2);
            ArrayList var13 = new ArrayList();
            ArrayList var7 = new ArrayList();

            for (String var9 : (Iterable<String>) (Iterable<?>) (var4)) {
               OpenApiDocument var10 = this.cache.computeIfAbsent(var9, var0 -> new OpenApiDocument(Resource.path(var0)));
               OpenApiSchemaNode var11 = OpenApiDocumentQueries.a(var10, var3);
               if (var11 != null) {
                  return template(var3, var11);
               }

               if (OpenApiDocumentQueries.b(var10, var3) != null) {
                  OpenApiSchemaNode var12 = OpenApiDocumentQueries.a(var10, var3, var5);
                  if (var12 == null) {
                     throw new RuntimeException(
                        "operation '" + var3 + "' declares no response schema" + (var5 != null ? " for status " + var5 : "") + " — nothing to lock"
                     );
                  }

                  return template(var3, var12);
               }

               var13.addAll(OpenApiDocumentQueries.b(var10));
               var7.addAll(OpenApiDocumentQueries.c(var10));
            }

            throw new RuntimeException(
               "'" + var3 + "' matches no component or operation in the configured spec(s) — components: " + var13 + "; operations: " + var7
            );
         }
      } else {
         throw new RuntimeException(
            "spec.schema(ref[, status]) needs a component name or an operationId, e.g. spec.schema('User') or spec.schema('createUser', 400)"
         );
      }
   }

   private static Object template(String var0, OpenApiSchemaNode var1) {
      var var2 = io.karatelabs.ext.openapi.SchemaMatchConverter.unsupportedMarkerDiagnostics(var1);
      if (!var2.isEmpty()) {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var2)) {
            var3.add(var5.get("path") + " " + var5.get("keyword"));
         }

         logger.warn(
            "spec.schema('{}'): the # template cannot carry {} — use match.jsonSchema(actual, schema) for real JSON-Schema semantics (Openapi.matchSchema returns the full `lossy` list)",
            var0,
            var3
         );
      }

      return io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplate(var1);
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# spec — the configured API contract, as match templates\n\n`spec.schema(ref[, status])` → the Karate **match template** for a spec schema, so one\nline locks a response to the contract's *shape* (required fields `#typed`, optional\n`##typed`, `format: uuid` → `#uuid`, enums → a value-set predicate):\n\n- `spec.schema('User')` — a component (`components/schemas/User`).\n- `spec.schema('createUser')` — that operation's **success** response schema.\n- `spec.schema('createUser', 400)` — a declared **error** shape (negative paths welcome).\n\n```gherkin\n* def CreateUserResp = spec.schema('User')\nThen status 201\nAnd match response == CreateUserResp\n```\n\nThe template pins the shape per-spec — a value edit never breaks it; a contract drift\ndoes. Related: `match.schema(example)` infers the same template from a real payload\n(no spec needed); `Openapi.matchSchema(specPath, ref)` is this verb's JS/MCP twin."
      );
   }
}
