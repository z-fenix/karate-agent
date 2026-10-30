package io.karatelabs.ext.mock;

import io.karatelabs.ext.common.JsCallback;
import io.karatelabs.ext.jsonschema.validation.ValidationDirection;
import io.karatelabs.ext.jsonschema.validation.ValidationException;
import io.karatelabs.ext.jsonschema.validation.ValidationResult;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import io.karatelabs.ext.openapi.OpenApiSchemaNode;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.RuleEvaluator;
import io.karatelabs.ext.rules.RuleEvaluationCapture;
import io.karatelabs.ext.rules.MutationTraceCollector;
import io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.Session;
import io.karatelabs.js.Engine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiMockHandler implements Function<HttpRequest, HttpResponse> {
   private static final Logger logger = LoggerFactory.getLogger(OpenApiMockHandler.class);
   private final Set<String> ungatable = ConcurrentHashMap.newKeySet();
   private final OpenApiOperationCatalog model;
   private final Map<String, Object> rules;
   private final Map<String, Object> handlers;
   private final RulebookRepository ruleManager;
   private final RuleEvaluator ruleRunner;
   private final Session session;
   private final boolean crud;
   private final boolean validate;
   private final ResourceResolver resourceResolver;
   private OpenApiMockHandler.RuleTraceSink traceSink;
   private String mutantId;
   private final AtomicInteger ruleRequestIndex = new AtomicInteger();

   public OpenApiMockHandler withRuleTrace(OpenApiMockHandler.RuleTraceSink var1) {
      this.traceSink = var1;
      return this;
   }

   public OpenApiMockHandler withMutantId(String var1) {
      this.mutantId = var1;
      return this;
   }

   public OpenApiMockHandler(OpenApiOperationCatalog var1, Map<String, ?> var2, Map<String, ?> var3, RulebookRepository var4, RuleEvaluator var5, Session var6, boolean var7, boolean var8, ResourceResolver var9) {
      this.model = var1;
      this.rules = canonicalize(var2);
      this.handlers = canonicalize(var3);
      this.ruleManager = var4;
      this.ruleRunner = var5;
      this.session = var6;
      this.crud = var7;
      this.validate = var8;
      this.resourceResolver = var9;
   }

   public HttpResponse apply(HttpRequest var1) {
      io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var2 = this.model.findOperation(var1.getMethod(), var1.getUrlAndPath());
      if (var2 == null) {
         return HttpResponse.json(404, Map.of("error", "no matching operation", "method", var1.getMethod(), "path", var1.getPath()));
      } else {
         var1.processBody();
         ReentrantLock var3 = JsRequestHandler.lockOf(this.session);
         var3.lock();

         try {
            if (this.validate && var2.f() != null) {
               String var4 = var2.a() == null ? var2.b() + " " + var2.c() : var2.a();
               ValidationResult var5 = null;

               try {
                  var5 = var2.f().validateWithDirection(var1.getBodyConverted(), io.karatelabs.ext.jsonschema.validation.ValidationDirection.REQUEST);
               } catch (ValidationException var19) {
                  if (this.ungatable.add(var4)) {
                     logger.warn("mock: validate=true cannot gate '{}' — {}", var4, var19.getMessage());
                  }
               }

               if (var5 != null && !var5.valid) {
                  return HttpResponse.json(
                     400, Map.of("error", "request does not conform to the spec schema", "operation", var4, "detail", (String)var5.toMap().get("errors"))
                  );
               }
            }

            io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var21 = null;
            if (this.crud) {
               if (this.resourceResolver != null) {
                  var21 = this.resourceResolver.resolve(var2);
               }

               if (var21 == null) {
                  var21 = this.model.resourceOperationFor(var2);
               }
            }

            HttpResponse var22 = var21 != null ? this.crudResponse(var2, var21, var1) : sampleResponse(var2);
            Object var6 = lookup(this.handlers, var2);
            if (var6 != null) {
               this.runHandler(var6, var1, var22);
               if (this.mutantId != null) {
                  var22.setHeader("Karate-Mutant", this.mutantId);
               }
            }

            Object var7 = lookup(this.rules, var2);
            if (var7 != null) {
               Object var24 = var1.getBodyConverted();
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var9 = this.ruleManager.b(String.valueOf(var7));
               String var10 = var9.g();
               var var11 = var9.m().e();
               HttpResponse var12;
               if (this.traceSink != null) {
                  String var13 = var2.a() != null ? var2.a() : var2.b() + " " + var2.c();
                  int var14 = this.ruleRequestIndex.getAndIncrement();
                  io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation var15 = RuleEvaluationCapture.a(this.ruleRunner, var10, var11, var24);
                  this.traceSink.onRuleServed(var13, var14, var15.b(), var15.c());
                  var12 = var15.c() ? HttpResponse.json(500, Map.of("error", "rule execution failed")) : HttpResponse.json(var2.d(), var15.a().b());
               } else {
                  io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var25 = this.ruleRunner.a(var10, var11, var24, null);
                  var12 = HttpResponse.json(var2.d(), var25.b());
               }

               if (this.mutantId != null) {
                  var12.setHeader("Karate-Mutant", this.mutantId);
               }

               return var12;
            } else {
               return var22;
            }
         } finally {
            var3.unlock();
         }
      }
   }

   private void runHandler(Object var1, HttpRequest var2, HttpResponse var3) {
      JsCallback var4 = io.karatelabs.ext.common.JsCallback.a(var1);
      if (var4 != null) {
         var4.a(var2, var3, this.session);
      } else {
         if (var1 instanceof String var5 && !var5.isBlank()) {
            Engine var6 = new Engine();
            var6.put("request", var2);
            var6.put("response", var3);
            var6.put("session", this.session);
            var6.eval(var5);
         }
      }
   }

   private static Map<String, Object> canonicalize(Map<String, ?> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 != null) {
         var0.forEach((var1x, var2) -> {
            if (var1x != null && var2 != null) {
               var1.put(canonicalKey(var1x), var2);
            }
         });
      }

      return var1;
   }

   static String canonicalKey(String var0) {
      int var1 = var0.indexOf(32);
      if (var1 > 0) {
         String var2 = var0.substring(0, var1).trim();
         String var3 = var0.substring(var1 + 1).trim();
         if (var3.startsWith("/")) {
            return var2.toUpperCase() + " " + var3;
         }
      }

      return var0;
   }

   static List<String> opKeys(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      ArrayList var1 = new ArrayList();
      if (var0.a() != null) {
         var1.add(var0.a());
      }

      var1.add(var0.b().toUpperCase() + " " + var0.c());
      return var1;
   }

   private static Object lookup(Map<String, Object> var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      for (String var3 : opKeys(var1)) {
         if (var0.containsKey(var3)) {
            return var0.get(var3);
         }
      }

      return null;
   }

   public static List<Map<String, Object>> orphanWarnings(OpenApiOperationCatalog var0, Map<String, ?> var1, String var2) {
      HashSet var3 = new HashSet();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var5 : var0.operations()) {
         var3.addAll(opKeys(var5));
      }

      ArrayList var8 = new ArrayList();
      if (var1 != null) {
         for (String var6 : var1.keySet()) {
            if (var6 != null && !var3.contains(canonicalKey(var6))) {
               LinkedHashMap var7 = new LinkedHashMap();
               var7.put("type", "orphan_overlay");
               var7.put("kind", var2);
               var7.put("key", var6);
               var7.put("message", var2 + " '" + var6 + "' matches no operation in the spec");
               var8.add(var7);
            }
         }
      }

      return var8;
   }

   private static HttpResponse sampleResponse(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      Object var1 = var0.e();
      return HttpResponse.json(var0.d(), var1 == null ? Map.of() : var1);
   }

   private HttpResponse crudResponse(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var2, HttpRequest var3) {
      var var4 = this.collection(var2.a().a());
      switch (var2.b()) {
         case LIST:
            return HttpResponse.json(var1.d(), listBody(var1, new ArrayList<>(var4.values())));
         case CREATE:
            var var13 = asMap(var3.getBodyConverted());
            Object var15 = var13.get("id");
            String var16 = var15 != null ? String.valueOf(var15) : this.nextId(var2.a().a());
            var13.put("id", var15 != null ? var15 : typedId(var1, var16));
            fillRequired(var1, var13);
            var4.put(var16, var13);
            return HttpResponse.json(var1.d(), var13);
         case READ:
            Object var12 = var4.get(idOf(var3));
            return var12 == null ? notFound(var3) : HttpResponse.json(var1.d(), var12);
         case UPDATE:
            String var11 = idOf(var3);
            if (!var4.containsKey(var11)) {
               return notFound(var3);
            }

            var var14 = asMap(var3.getBodyConverted());
            var14.put("id", typedId(var1, var11));
            fillRequired(var1, var14);
            var4.put(var11, var14);
            return HttpResponse.json(var1.d(), var14);
         case PATCH:
            String var10 = idOf(var3);
            if (!(var4.get(var10) instanceof Map var7)) {
               return notFound(var3);
            }

            if (var3.getBodyConverted() instanceof Map var9) {
               var7.putAll(var9);
            }

            var7.put("id", typedId(var1, var10));
            return HttpResponse.json(var1.d(), var7);
         case DELETE:
            String var5 = idOf(var3);
            if (var4.remove(var5) == null) {
               return notFound(var3);
            }

            return HttpResponse.json(var1.d(), Map.of());
         default:
            return sampleResponse(var1);
      }
   }

   private static void fillRequired(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0, Map<String, Object> var1) {
      if (var0.e() instanceof Map var2) {
         for (String var4 : var0.j()) {
            if (!var1.containsKey(var4) && var2.get(var4) != null) {
               var1.put(var4, var2.get(var4));
            }
         }

         for (OpenApiSchemaNode var10 : objectParts(var0.k())) {
            for (OpenApiSchemaNode var6 : var10.children) {
               String var7 = var6.name();
               if (var7 != null && !"id".equals(var7) && var1.get(var7) != null && var2.get(var7) != null && !conforms(var6, var1.get(var7))) {
                  var1.put(var7, var2.get(var7));
               }
            }
         }
      }
   }

   private static Object typedId(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0, String var1) {
      String var2 = idType(var0.k());
      if (var2 == null) {
         var2 = idType(var0.f());
      }

      if ("integer".equals(var2) || "number".equals(var2)) {
         try {
            return Long.parseLong(var1);
         } catch (NumberFormatException var4) {
         }
      }

      return var1;
   }

   private static String idType(OpenApiSchemaNode var0) {
      for (OpenApiSchemaNode var2 : objectParts(var0)) {
         for (OpenApiSchemaNode var4 : var2.children) {
            if ("id".equals(var4.name())) {
               return var4.type();
            }
         }
      }

      return null;
   }

   private static List<OpenApiSchemaNode> objectParts(OpenApiSchemaNode var0) {
      List<OpenApiSchemaNode> var1 = var0 == null ? List.of() : ("allOf".equals(var0.type()) ? var0.children : List.of(var0));
      return var1.stream().filter(var0x -> "object".equals(var0x.type())).toList();
   }

   private static boolean conforms(OpenApiSchemaNode var0, Object var1) {
      try {
         return var0.validateWithDirection(var1, io.karatelabs.ext.jsonschema.validation.ValidationDirection.RESPONSE).valid;
      } catch (ValidationException var3) {
         return true;
      }
   }

   private static Object listBody(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0, List<Object> var1) {
      if (var0.e() instanceof Map var2 && !var2.isEmpty()) {
         LinkedHashMap var7 = new LinkedHashMap();
         boolean var4 = false;

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (!var4 && var6.getValue() instanceof List) {
               var7.put(String.valueOf(var6.getKey()), var1);
               var4 = true;
            } else {
               var7.put(String.valueOf(var6.getKey()), var6.getValue());
            }
         }

         if (var4) {
            return var7;
         }
      }

      return var1;
   }

   private Map<String, Object> collection(String var1) {
      Object var2 = this.session.getData().get(var1);
      if (!(var2 instanceof Map)) {
         var2 = new LinkedHashMap();
         this.session.getData().put(var1, var2);
      }

      return (Map<String, Object>)var2;
   }

   private String nextId(String var1) {
      String var2 = "__seq_" + var1;
      int var4 = (this.session.getData().get(var2) instanceof Number var5 ? var5.intValue() : 0) + 1;
      this.session.getData().put(var2, var4);
      return String.valueOf(var4);
   }

   private static String idOf(HttpRequest var0) {
      String var1 = var0.getPath();
      int var2 = var1.indexOf(63);
      if (var2 >= 0) {
         var1 = var1.substring(0, var2);
      }

      if (var1.length() > 1 && var1.endsWith("/")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      int var3 = var1.lastIndexOf(47);
      return var3 >= 0 ? var1.substring(var3 + 1) : var1;
   }

   private static Map<String, Object> asMap(Object var0) {
      return var0 instanceof Map var1 ? new LinkedHashMap<>(var1) : new LinkedHashMap<>();
   }

   private static HttpResponse notFound(HttpRequest var0) {
      return HttpResponse.json(404, Map.of("error", "not found", "path", var0.getPath()));
   }

   public interface RuleTraceSink {
      void onRuleServed(String var1, int var2, io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot var3, boolean var4);
   }
}
