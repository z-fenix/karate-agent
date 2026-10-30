package io.karatelabs.ext.robot;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.openapi.JsonSchemaInferrer;
import io.karatelabs.js.Context;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchBinding extends JsDispatch implements JavaCallable {
   private io.karatelabs.ext.robot.MatchBinding.AssertionListener sink;

   public MatchBinding() {
      this.fn("equals", var1 -> this.assertMatch(Match.Type.EQUALS, var1));
      this.fn("notEquals", var1 -> this.assertMatch(Match.Type.NOT_EQUALS, var1));
      this.fn("contains", var1 -> this.assertMatch(Match.Type.CONTAINS, var1));
      this.fn("notContains", var1 -> this.assertMatch(Match.Type.NOT_CONTAINS, var1));
      this.fn("containsDeep", var1 -> this.assertMatch(Match.Type.CONTAINS_DEEP, var1));
      this.fn("containsOnly", var1 -> this.assertMatch(Match.Type.CONTAINS_ONLY, var1));
      this.fn("containsAny", var1 -> this.assertMatch(Match.Type.CONTAINS_ANY, var1));
      this.fn("each", var1 -> this.assertMatch(Match.Type.EACH_EQUALS, var1));
      this.fn("eachContains", var1 -> this.assertMatch(Match.Type.EACH_CONTAINS, var1));
      this.fn("within", var1 -> this.assertMatch(Match.Type.WITHIN, var1));
      this.fn("notWithin", var1 -> this.assertMatch(Match.Type.NOT_WITHIN, var1));
      this.fn("test", var0 -> test(var0));
      this.fn("jsonSchema", var1 -> this.assertJsonSchema(var1));
      this.fn("testJsonSchema", var0 -> testJsonSchema(var0));
      this.fn("schema", var0 -> matchSchemaFrom(var0));
      this.fn("toJsonSchema", var0 -> jsonSchemaFrom(var0));
      this.fn("fromJsonSchema", var0 -> io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplateFromMap(asMap(var0, "fromJsonSchema")));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   private Object assertJsonSchema(Object[] var1) {
      if (var1 != null && var1.length >= 2) {
         String var2 = label(var1);
         io.karatelabs.ext.jsonschema.validation.ValidationResult var3 = validateJsonSchema(var1);
         this.emit("jsonSchema", var2, var1[0], var1[1], var3.valid, var3.message());
         if (var3.valid) {
            LinkedHashMap<java.lang.String, java.lang.Boolean> var4 = new LinkedHashMap();
            var4.put("pass", true);
            var4.put("message", null);
            return var4;
         } else {
            throw new RuntimeException((var2 != null && !var2.isBlank() ? var2 + " — " : "") + "json-schema match failed:\n" + var3.message());
         }
      } else {
         throw new RuntimeException("match.jsonSchema(actual, schema[, label]) needs an actual and a JSON-Schema");
      }
   }

   private static Object testJsonSchema(Object[] var0) {
      if (var0 != null && var0.length >= 2) {
         return validateJsonSchema(var0).toMap();
      } else {
         throw new RuntimeException("match.testJsonSchema(actual, schema[, opts]) needs an actual and a JSON-Schema");
      }
   }

   private static io.karatelabs.ext.jsonschema.validation.ValidationResult validateJsonSchema(Object[] var0) {
      if (!(var0[1] instanceof Map)) {
         throw new RuntimeException("match.jsonSchema(actual, schema): schema must be a JSON-Schema object");
      } else {
         io.karatelabs.ext.jsonschema.validation.SchemaDialect var1 = null;
         boolean var2 = false;
         io.karatelabs.ext.jsonschema.validation.ValidationDirection var3 = io.karatelabs.ext.jsonschema.validation.ValidationDirection.NONE;
         if (var0.length > 2 && var0[2] instanceof Map var4) {
            if (var4.get("dialect") != null) {
               String var6 = String.valueOf(var4.get("dialect"));
               var1 = io.karatelabs.ext.jsonschema.validation.SchemaDialect.fromId(var6);
               if (var1 == null) {
                  throw new RuntimeException("unknown dialect '" + var6 + "' — one of: draft-07, 2019-09, 2020-12, oas-3.0, oas-3.1, swagger-2.0, asyncapi-2");
               }
            }

            var2 = Boolean.TRUE.equals(var4.get("assertFormat"));
            if (var4.get("direction") != null) {
               var3 = io.karatelabs.ext.jsonschema.validation.ValidationDirection.of(String.valueOf(var4.get("direction")));
            }
         }

         return io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(var0[1], var1).withFormatAssertion(var2).withDirection(var3).validate(var0[0]);
      }
   }

   private static String label(Object[] var0) {
      if (var0.length >= 3 && var0[2] != null) {
         if (var0[2] instanceof Map var1) {
            return var1.get("label") == null ? null : String.valueOf(var1.get("label")).trim();
         } else {
            return String.valueOf(var0[2]).trim();
         }
      } else {
         return null;
      }
   }

   private static Object matchSchemaFrom(Object[] var0) {
      return io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplateFromMap(jsonSchemaFrom(var0));
   }

   private static Map<String, Object> jsonSchemaFrom(Object[] var0) {
      if (var0 != null && var0.length != 0) {
         return io.karatelabs.ext.openapi.JsonSchemaInferrer.a((List<Object>)(new ArrayList<>(Arrays.asList(var0))));
      } else {
         throw new RuntimeException("match.toJsonSchema(example[, more…]) needs at least one example value");
      }
   }

   private static Map<String, Object> asMap(Object[] var0, String var1) {
      if (var0 != null && var0.length != 0 && var0[0] instanceof Map) {
         return (Map<String, Object>)var0[0];
      } else {
         throw new RuntimeException("match." + var1 + "(jsonSchema) needs a JSON-Schema object");
      }
   }

   private void emit(String var1, String var2, Object var3, Object var4, boolean var5, String var6) {
      if (this.sink != null) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("kind", "assertion");
         if (var2 != null && !var2.isBlank()) {
            var7.put("label", var2);
         }

         var7.put("verb", var1);
         var7.put("actual", var3);
         var7.put("expected", var4);
         var7.put("pass", var5);
         var7.put("message", var6);
         this.sink.assertion(var7);
      }
   }

   public MatchBinding onAssertion(io.karatelabs.ext.robot.MatchBinding.AssertionListener var1) {
      this.sink = var1;
      return this;
   }

   @Override
   public Object call(Context var1, Object... var2) {
      return this.assertMatch(Match.Type.EQUALS, var2);
   }

   private Object assertMatch(Match.Type var1, Object[] var2) {
      if (var2 != null && var2.length >= 2) {
         String var3 = var2.length > 2 && var2[2] != null ? String.valueOf(var2[2]).trim() : null;
         Result var4 = Match.execute(new Engine(), var1, var2[0], var2[1]);
         if (this.sink != null) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("kind", "assertion");
            if (var3 != null && !var3.isBlank()) {
               var5.put("label", var3);
            }

            var5.put("verb", verb(var1));
            var5.put("actual", var2[0]);
            var5.put("expected", var2[1]);
            var5.put("pass", var4.pass);
            var5.put("message", var4.message);
            this.sink.assertion(var5);
         }

         if (var4.pass) {
            return resultMap(var4);
         } else {
            throw new RuntimeException((var3 != null && !var3.isBlank() ? var3 + " — " : "") + "match failed: " + var4.message);
         }
      } else {
         throw new RuntimeException("match." + verb(var1) + "(actual, expected[, label]) needs at least an actual and an expected");
      }
   }

   private static Object test(Object[] var0) {
      if (var0 != null && var0.length >= 2) {
         return resultMap(Match.execute(new Engine(), Match.Type.EQUALS, var0[0], var0[1]));
      } else {
         throw new RuntimeException("match.test(actual, expected) needs an actual and an expected");
      }
   }

   private static Map<String, Object> resultMap(Result var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("pass", var0.pass);
      var1.put("message", var0.message);
      return var1;
   }

   public static String verb(Match.Type var0) {
      return switch (var0) {
         case NOT_EQUALS -> "notEquals";
         case CONTAINS -> "contains";
         case NOT_CONTAINS -> "notContains";
         case CONTAINS_DEEP -> "containsDeep";
         case CONTAINS_ONLY -> "containsOnly";
         case CONTAINS_ANY -> "containsAny";
         case CONTAINS_ONLY_DEEP -> "containsOnlyDeep";
         case CONTAINS_ANY_DEEP -> "containsAnyDeep";
         case EACH_EQUALS -> "each";
         case EACH_NOT_EQUALS -> "eachNotEquals";
         case EACH_CONTAINS -> "eachContains";
         case EACH_NOT_CONTAINS -> "eachNotContains";
         case EACH_CONTAINS_ONLY -> "eachContainsOnly";
         case EACH_CONTAINS_ANY -> "eachContainsAny";
         case EACH_CONTAINS_DEEP -> "eachContainsDeep";
         case WITHIN -> "within";
         case NOT_WITHIN -> "notWithin";
         default -> "equals";
      };
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# match — assert actual vs expected (throws on failure)\n\n`match` is both callable and a namespace. The optional last arg is a **label** (the\nassertion's intent) — it leads the failure message and, during a run, becomes a pass/fail\nrow on the report.\n\n- `match(actual, expected, label?)` — shorthand for `equals`.\n- `match.equals(actual, expected, label?)` — deep equals.\n- `match.contains` · `match.containsDeep` · `match.containsOnly` · `match.containsAny`\n- `match.each(actual, expected, label?)` — every element matches `expected`.\n- `match.eachContains` · `match.notEquals` · `match.notContains` · `match.within` · `match.notWithin`\n- `match.test(actual, expected)` → `{pass, message}` — a **non-throwing** predicate (branch/retry).\n\n`expected` accepts karate's fuzzy markers: `#string`, `#number`, `#boolean`, `#uuid`,\n`#regex ...`, `#[]` (array), `#(expr)` (embedded JS), `##` (optional), etc.\n\n## Validate against a JSON-Schema (one shot)\n\nReal JSON-Schema semantics — `oneOf` / `anyOf` / `if`-`then`-`else` / `patternProperties` /\n`const` / `additionalProperties:false` all mean what the spec says.\n\n- `match.jsonSchema(actual, schema, label?)` — assert `actual` conforms. Throws like `equals`.\n- `match.testJsonSchema(actual, schema)` → `{valid, errors[]}` — **non-throwing**, for branching.\n  Each error names the failing `keyword`, `instancePath`, `schemaPath`, expected-vs-actual, and\n  for a combinator the **nearest** failing branch plus its own nested causes.\n- Dialect is taken from the schema's own `$schema`, else `2020-12`. Override only for a schema\n  that declares none: `match.jsonSchema(actual, schema, {label, dialect: 'oas-3.0'})` — one of\n  `draft-07` · `2019-09` · `2020-12` · `oas-3.0` · `oas-3.1` · `swagger-2.0` · `asyncapi-2`.\n- A `$ref` to another document resolves only against schemas already registered — **never fetched**.\n- `format` is an **annotation** by default (the spec's rule). Assert it with\n  `match.jsonSchema(actual, schema, {assertFormat: true})` — that asserts the format names the\n  dialect itself defines (`date-time`, `uuid`, `ipv4`, … and OAS's `int32`/`int64`/`byte`);\n  a custom format name stays an annotation.\n- Under `oas-3.0`, `required` is **direction-scoped** by `readOnly`/`writeOnly`. Say which way\n  the payload travels — `{direction: 'request'}` or `{direction: 'response'}` — and a `required`\n  property marked `readOnly` stops being required on a request (`writeOnly`, on a response).\n  Default is neither, which grades `required` as written.\n\n## Write the assertion for me (schema authoring)\n\nThe inverse of asserting — turn real data (or a JSON-Schema) INTO a match-schema, so you pin the\n*shape* without hand-typing markers. Pure data, no throw.\n\n- `match.schema(example[, more…])` → a Karate match-schema (`#`-marker shape) from example payload(s).\n  Pass several samples and optional/nullable fields surface as `##`.\n- `match.toJsonSchema(example[, more…])` → a proper JSON-Schema from example payload(s).\n- `match.fromJsonSchema(jsonSchema)` → a Karate match-schema from a JSON-Schema (e.g. an OpenAPI body).\n  This is the **lenient** path, named for what it is: run it with `match.containsDeep`. Use\n  `match.jsonSchema` when you want JSON-Schema to *mean* JSON-Schema.\n\n```js\nmatch(quote.total, oracle.total, 'quote total = rules oracle')\nmatch.jsonSchema(response, openapiBodySchema, 'response conforms to the contract')\n// capture a real response once, get a future-proof shape assertion:\n* def shape = match.schema(response)      // -> { id: '#uuid', name: '#string', tags: '#[] #string' }\n* match response == shape\nif (!match.test(page.title, 'Checkout').pass) bot.go('/checkout')   // branch, no throw\n```"
      );
   }

   @FunctionalInterface
   public interface AssertionListener {
      void assertion(Map<String, Object> var1);
   }
}
