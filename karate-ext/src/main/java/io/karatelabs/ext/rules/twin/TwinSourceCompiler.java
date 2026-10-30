package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.Context;
import io.karatelabs.js.JavaCallable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class TwinSourceCompiler {
   static final List<String> a = List.of("queryParameters", "headers", "auth", "pagination", "contentNegotiation", "multipart", "streaming");
   private static final Set<String> b = Set.of("GET", "PUT", "POST", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE");
   private static final List<String> c = List.of("path", "identity", "view", "schema", "errorCode", "list", "name");
   private static final List<String> d = List.of("applied", "refused");
   private static final List<String> e = List.of("status", "code");
   private static final List<String> f = List.of("$ref", "$defs", "$id", "$anchor", "$dynamicRef", "$dynamicAnchor");
   private static final char[] g = "0123456789ABCDEF".toCharArray();

   private TwinSourceCompiler() {
   }

   static io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel a(Object var0) {
      var var1 = TwinBuilder.object(var0);
      if (var1 == null) {
         throw TwinException.a(null, "resource", "must be an object");
      } else {
         for (String var3 : (Iterable<String>) (Iterable<?>) (var1.keySet())) {
            if (!c.contains(var3)) {
               throw TwinException.a(null, "resource", "'" + var3 + "' is unknown — the keys are " + c);
            }
         }

         String var12 = c(var1.get("path"));
         if (var12 != null && var12.startsWith("/") && !var12.endsWith("/") && !var12.contains("{")) {
            String var13 = c(var1.get("identity"));
            if (var13 == null) {
               throw TwinException.a(null, "resource.identity", "must name the world field the item path addresses");
            } else {
               JavaCallable var4 = TwinBuilder.function(var1.get("view"), null, "resource.view");
               Object var5 = b(var1.get("schema"));
               Object var6 = var1.get("errorCode");
               String var7 = var6 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var6) ? c(var6) : "error";
               if (var7 == null) {
                  throw TwinException.a(null, "resource.errorCode", "must be a body field name");
               } else {
                  Object var8 = var1.get("list");
                  boolean var9 = d(null, "resource.list", var8);
                  Object var10 = var1.get("name");
                  String var11 = var10 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var10) ? c(var10) : c(var12);
                  if (var11 != null && !var11.isEmpty()) {
                     return new io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel(var12, var13, var4, var5, var7, var9, var11);
                  } else {
                     throw TwinException.a(null, "resource.name", "must be the schema component name");
                  }
               }
            }
         } else {
            throw TwinException.a(null, "resource.path", "must be an absolute collection path with no {param}, such as '/claims'");
         }
      }
   }

   private static Object b(Object var0) {
      if (var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         Object var1;
         try {
            io.karatelabs.ext.rules.twin.CanonicalJson.b(var0);
            var1 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var0);
         } catch (SnapshotException var4) {
            throw TwinException.a(null, "resource.schema", var4.getMessage());
         }

         if (!(var1 instanceof Map)) {
            throw TwinException.a(null, "resource.schema", "must be a JSON Schema object");
         } else {
            a(var1, "");

            try {
               io.karatelabs.ext.jsonschema.validation.CompiledSchema var2 = io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(var1);
               var2.validate(new LinkedHashMap());
               return var1;
            } catch (io.karatelabs.ext.jsonschema.validation.ValidationException var3) {
               throw TwinException.a("SCHEMA_UNSUPPORTED", null, "resource.schema", var3.getMessage());
            }
         }
      } else {
         throw TwinException.a(null, "resource.schema", "is required — the view's JSON Schema");
      }
   }

   private static void a(Object var0, String var1) {
      if (var0 instanceof Map var2) {
         for (Object var4 : var2.keySet()) {
            String var5 = String.valueOf(var4);
            if (f.contains(var5)) {
               throw b(a(var1, var5), "the view schema is self-contained; no references");
            }

            if (!io.karatelabs.ext.jsonschema.validation.SchemaKeywords.isSupportedKeyword(var5)) {
               throw b(a(var1, var5), "the validator does not read '" + var5 + "'");
            }
         }

         for (String var11 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SINGLE_SCHEMA_KEYWORDS) {
            a(var2.get(var11), a(var1, var11));
         }

         for (String var12 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SCHEMA_ARRAY_KEYWORDS) {
            b(var2.get(var12), a(var1, var12));
         }

         for (String var13 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SCHEMA_MAP_KEYWORDS) {
            if (var2.get(var13) instanceof Map var15) {
               var15.forEach((var2x, var3) -> a(var3, a(var1, var13) + "." + var2x));
            }
         }

         Object var10 = var2.get("items");
         if (var10 instanceof List) {
            b(var10, a(var1, "items"));
         } else {
            a(var10, a(var1, "items"));
         }

         if (var2.get("dependencies") instanceof Map var14) {
            var14.forEach((var1x, var2x) -> a(var2x, a(var1, "dependencies") + "." + var1x));
         }
      }
   }

   private static void b(Object var0, String var1) {
      if (var0 instanceof List var2) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            a(var2.get(var3), var1 + "[" + var3 + "]");
         }
      }
   }

   private static String a(String var0, String var1) {
      return var0.isEmpty() ? var1 : var0 + "." + var1;
   }

   private static TwinException b(String var0, String var1) {
      return TwinException.a("SCHEMA_UNSUPPORTED", null, "resource.schema", var1 + " (at '" + var0 + "')");
   }

   static io.karatelabs.ext.rules.twin.TwinModel.OperationBinding a(String var0, Object var1, JavaCallable var2) {
      Object var3 = TwinBuilder.member(var1, "route");
      Object var4 = TwinBuilder.member(var1, "operation");
      Object var5 = TwinBuilder.member(var1, "wire");
      Object var6 = TwinBuilder.member(var1, "creates");
      boolean var7 = var3 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var3);
      if (!var7) {
         for (String var19 : List.of("operation", "wire", "creates")) {
            Object var20 = TwinBuilder.member(var1, var19);
            if (var20 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var20)) {
               throw TwinException.a(var0, var19, "requires route — '" + var19 + "' declares how a bound command travels, and nothing is bound without a route");
            }
         }

         return null;
      } else if (var2 != null) {
         throw TwinException.a(var0, "route", "is exclusive with request — an authored request is the live-only escape hatch, and nothing derives a route from it");
      } else {
         String var8 = c(var3);
         if (var8 == null) {
            throw TwinException.a(var0, "route", "must be '<METHOD> /template'");
         } else {
            int var9 = var8.indexOf(32);
            if (var9 < 0) {
               throw TwinException.a("ROUTE_UNRESOLVED", var0, "route", "must be '<METHOD> /template', got '" + var8 + "'");
            } else {
               String var10 = var8.substring(0, var9);
               String var11 = var8.substring(var9 + 1).trim();
               if (!b.contains(var10)) {
                  throw TwinException.a("ROUTE_UNRESOLVED", var0, "route", "'" + var10 + "' is not an HTTP method — one of " + new TreeSet<String>(b));
               } else if (var11.startsWith("/") && !var11.contains(" ")) {
                  c(var0, var11);
                  String var12 = var4 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var4) ? c(var4) : var0;
                  if (var12 != null && !var12.isEmpty()) {
                     io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var13 = new io.karatelabs.ext.rules.twin.TwinModel.OperationBinding(var12, var10, var11, d(var0, "creates", var6), a(var0, var5));

                     for (Entry var15 : var13.e().b().entrySet()) {
                        for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var17 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var15.getValue())) {
                           if (var17.a() == a(var13)) {
                              throw TwinException.a(
                                 "WIRE_AMBIGUOUS",
                                 var0,
                                 "wire.refused",
                                 "'" + (String)var15.getKey() + "' answers " + var17.a() + ", which is the applied status"
                              );
                           }
                        }
                     }

                     return var13;
                  } else {
                     throw TwinException.a(var0, "operation", "must be the operationId this command joins on");
                  }
               } else {
                  throw TwinException.a("ROUTE_UNRESOLVED", var0, "route", "'" + var11 + "' is not an absolute path template");
               }
            }
         }
      }
   }

   private static void c(String var0, String var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < var1.length(); var3++) {
         char var4 = var1.charAt(var3);
         if (var4 == '{') {
            if (var2 >= 0) {
               throw d(var0, var1);
            }

            if (var3 > 0 && var1.charAt(var3 - 1) == '}') {
               throw a(var0, var1, var3);
            }

            var2 = var3;
         } else if (var4 == '}') {
            if (var2 < 0 || var3 == var2 + 1) {
               throw d(var0, var1);
            }

            var2 = -1;
         }
      }

      if (var2 >= 0) {
         throw d(var0, var1);
      }
   }

   private static TwinException d(String var0, String var1) {
      return TwinException.a(var0, "route", "'" + var1 + "' carries an unbalanced or empty placeholder — each is one '{name}'");
   }

   private static TwinException a(String var0, String var1, int var2) {
      int var3 = var1.lastIndexOf(47, var2) + 1;
      int var4 = var1.indexOf(47, var2);
      String var5 = var1.substring(var3, var4 < 0 ? var1.length() : var4);
      return TwinException.a(var0, "route", "'" + var5 + "' adjoins two placeholders — a literal must separate them, or neither value has an end");
   }

   static int a(io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var0) {
      return var0.e().a() != null ? var0.e().a() : (var0.d() ? 201 : 200);
   }

   private static io.karatelabs.ext.rules.twin.TwinModel.WireModel a(String var0, Object var1) {
      if (var1 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
         var var2 = TwinBuilder.object(var1);
         if (var2 == null) {
            throw TwinException.a(var0, "wire", "must be an object { applied?, refused? }");
         } else {
            for (String var4 : (Iterable<String>) (Iterable<?>) (var2.keySet())) {
               if (!d.contains(var4)) {
                  throw TwinException.a(var0, "wire", "'" + var4 + "' is unknown — the keys are " + d);
               }
            }

            Object var14 = var2.get("applied");
            Integer var15 = var14 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var14) ? b(var0, "wire.applied", var14) : null;
            LinkedHashMap<java.lang.String, java.util.List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>> var5 = new LinkedHashMap();
            Object var6 = var2.get("refused");
            if (var6 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var6)) {
               var var7 = TwinBuilder.object(var6);
               if (var7 == null) {
                  throw TwinException.a(var0, "wire.refused", "must be an object of {status, code} keyed by reason");
               }

               for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
                  var5.put((String)var9.getKey(), a(var0, (String)var9.getKey(), var9.getValue()));
               }
            }

            LinkedHashMap<java.lang.String, java.lang.String> var16 = new LinkedHashMap();

            for (Entry var18 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
               for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var11 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var18.getValue())) {
                  String var12 = var11.a() + " " + var11.b();
                  String var13 = (String)var16.put(var12, (String)var18.getKey());
                  if (var13 != null) {
                     throw TwinException.a(
                        "WIRE_AMBIGUOUS",
                        var0,
                        "wire.refused",
                        "'" + var13 + "' and '" + (String)var18.getKey() + "' share the shape " + var12 + " — the derived observer could not tell them apart"
                     );
                  }
               }
            }

            return new io.karatelabs.ext.rules.twin.TwinModel.WireModel(var15, Collections.unmodifiableMap(var5));
         }
      } else {
         return new io.karatelabs.ext.rules.twin.TwinModel.WireModel(null, Map.of());
      }
   }

   private static List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal> a(String var0, String var1, Object var2) {
      String var3 = "wire.refused." + var1;
      if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         List<Object> var4 = var2 instanceof List var5 ? new ArrayList(var5) : List.of(var2);
         if (var4.isEmpty()) {
            throw TwinException.a(var0, var3, "must name at least one {status, code}");
         } else {
            LinkedHashSet<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal> var11 = new LinkedHashSet();

            for (Object var7 : (Iterable<Object>)(Iterable<?>)(var4)) {
               var var8 = TwinBuilder.object(var7);
               if (var8 == null) {
                  throw TwinException.a(var0, var3, "must be {status, code} or a list of them");
               }

               for (String var10 : (Iterable<String>) (Iterable<?>) (var8.keySet())) {
                  if (!e.contains(var10)) {
                     throw TwinException.a(var0, var3, "'" + var10 + "' is unknown — the keys are " + e);
                  }
               }

               String var12 = c(var8.get("code"));
               if (var12 == null) {
                  throw TwinException.a(var0, var3, "code must be a non-empty string");
               }

               var11.add(new io.karatelabs.ext.rules.twin.TwinModel.WireRefusal(b(var0, var3, var8.get("status")), var12));
            }

            return List.copyOf(var11);
         }
      } else {
         throw TwinException.a(var0, var3, "must be {status, code} or a list of them");
      }
   }

   private static int b(String var0, String var1, Object var2) {
      if (var2 instanceof Number var3 && var3.doubleValue() == Math.rint(var3.doubleValue()) && var3.intValue() >= 100 && var3.intValue() <= 599) {
         return var3.intValue();
      } else {
         throw TwinException.a(var0, var1, "status must be an HTTP status between 100 and 599, got " + var2);
      }
   }

   static void a(TwinModel var0) {
      ArrayList<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : var0.c()) {
         if (var3.q() != null) {
            var1.add(var3);
         }
      }

      io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var14 = var0.k();
      if (var1.isEmpty()) {
         if (var14 != null) {
            throw TwinException.a(null, "resource", "no command is bound — declare a route on the commands this resource exposes, or drop the resource");
         }
      } else if (var14 == null) {
         throw TwinException.a(((io.karatelabs.ext.rules.twin.TwinModel.CommandModel)var1.get(0)).a(), "route", "requires t.resource — a route without a resource addresses nothing");
      } else {
         var var15 = new LinkedHashSet<>(var0.l().keySet());
         var15.add(var14.c());
         LinkedHashMap<java.lang.String, java.lang.String> var4 = new LinkedHashMap();
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
         var4.put("GET " + var14.a(), "the derived item read");
         var5.put("read" + var14.h(), "the derived item read");
         if (var14.g()) {
            var4.put("GET " + var14.b(), "the derived collection read");
            var5.put("list" + var14.h(), "the derived collection read");
         }

         int var6 = 0;
         String var7 = null;

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var9 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.CommandModel>) (Iterable<?>) (var1)) {
            io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var10 = var9.q();
            a(var9.a(), var10, var14, var15);
            String var11 = var10.b() + " " + var10.c();
            String var12 = var4.put(var11, var9.a());
            if (var12 != null) {
               throw TwinException.a("ROUTE_DUPLICATE", var9.a(), "route", "'" + var11 + "' is already claimed by " + var12);
            }

            String var13 = var5.put(var10.a(), var9.a());
            if (var13 != null) {
               throw TwinException.a("OPERATION_DUPLICATE", var9.a(), "operation", "'" + var10.a() + "' is already claimed by " + var13);
            }

            if (var10.d()) {
               var6++;
               var7 = var7 == null ? var9.a() : var7;
               if (var6 > 1) {
                  throw TwinException.a(var9.a(), "creates", "declared twice, naming both " + var7 + " and " + var9.a() + " — one aggregate, one creating command");
               }
            }

            b(var9, var14);
         }

         if (var6 == 0) {
            throw TwinException.a(null, "creates", "none declared — one bound command allocates the identity " + var14.c());
         }
      }
   }

   private static void a(String var0, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var1, io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var2, Set<String> var3) {
      for (String var5 : a(var1.c())) {
         if (!var3.contains(var5)) {
            throw TwinException.a("ROUTE_UNRESOLVED", var0, "route", "'{" + var5 + "}' is not a field of the world — the init world declares " + var3);
         }
      }

      String var6 = var2.a();
      boolean var7 = var1.c().equals(var6) || var1.c().startsWith(var6 + "/") || var1.d() && var1.c().equals(var2.b());
      if (!var7) {
         throw TwinException.a(
            "ROUTE_UNRESOLVED", var0, "route", "'" + var1.c() + "' is neither " + var6 + " nor a sub-path of it" + (var1.d() ? " nor " + var2.b() : "")
         );
      }
   }

   private static void b(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var1) {
      if (var0.l() != null) {
         for (Entry var3 : var0.q().e().b().entrySet()) {
            for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var5 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var3.getValue())) {
               LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
               var6.put("status", var5.a());
               var6.put("body", new LinkedHashMap<>(Map.of(var1.f(), var5.b())));

               Object var7;
               try {
                  var7 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var0.l().call(null, var6));
               } catch (RuntimeException var10) {
                  throw TwinException.a("OBSERVE_DISAGREES", var0.a(), "observe", "threw on the derived response for '" + (String)var3.getKey() + "': " + io.karatelabs.ext.rules.twin.ThrowableUtils.a(var10));
               }

               Object var8 = var7 instanceof Map var9 ? var9.get("reason") : null;
               if (!((String)var3.getKey()).equals(var8)) {
                  throw TwinException.a("OBSERVE_DISAGREES", var0.a(), "observe", "reads the derived response for '" + (String)var3.getKey() + "' as '" + var8 + "'");
               }
            }
         }
      }
   }

   static List<Map<String, Object>> a(TwinModel var0, TwinWalkResult var1, Path var2, String var3) {
      if (var0.k() == null) {
         return List.of();
      } else {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();
         LinkedHashSet<java.lang.String> var5 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var7 : var1.g()) {
            io.karatelabs.ext.rules.twin.TwinModel.CommandModel var8 = var0.f().get(var7.b());
            if (var8 != null && var8.q() != null) {
               boolean var9 = "guard".equals(var7.d());
               if (!var9 || !var8.c()) {
                  String var10 = var9 ? var8.o().get("guard") : var7.e();
                  if (var10 == null || !var8.q().e().b().containsKey(var10)) {
                     var var11 = e(var8.a() + ":" + (var10 == null ? var7.d() : var10), "wireUndeclared");
                     var11.put("command", var8.a());
                     var11.put("stage", var7.d());
                     if (var10 != null) {
                        var11.put("reason", var10);
                     }

                     if (var5.add(String.valueOf(var11.get("id")))) {
                        var4.add(var11);
                     }
                  }
               }
            }
         }

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var13 : var1.h()) {
            if ("IDENTITY_REASSIGNED".equals(var13.f())) {
               var var14 = e(var13.b(), "identityReassigned");
               var14.put("command", var13.b());
               var14.put("message", var13.e());
               if (var5.add("identity:" + var13.b())) {
                  var4.add(var14);
               }
            }
         }

         a(var0, var2, var3, var4);
         return var4;
      }
   }

   private static void a(TwinModel var0, Path var1, String var2, List<Map<String, Object>> var3) {
      Path var4 = var1 == null ? null : var1.resolve("openapi.yaml");
      if (var4 != null && Files.isRegularFile(var4)) {
         String var5;
         try {
            io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var6 = io.karatelabs.ext.rules.twin.OpenApiProjector.a(var0, var2);
            if (var6.a() && var6.d().equals(Files.readString(var4, StandardCharsets.UTF_8))) {
               return;
            }

            var5 = var6.a() ? "the file is not what Rule.project writes" : "the projection refuses, so nothing could have written this file";
         } catch (RuntimeException | IOException var7) {
            var5 = "the projection could not be computed: " + io.karatelabs.ext.rules.twin.ThrowableUtils.a(var7);
         }

         var var8 = e("openapi.yaml", "projectionDrift");
         var8.put("file", var4.toString());
         var8.put("reason", var5);
         var3.add(var8);
      }
   }

   private static Map<String, Object> e(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("id", var0);
      var2.put("kind", var1);
      var2.put("status", "finding");
      return var2;
   }

   static List<String> a(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      int var2 = -1;

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '{') {
            var2 = var3;
         } else if (var4 == '}' && var2 >= 0) {
            var1.add(var0.substring(var2 + 1, var3));
            var2 = -1;
         }
      }

      return var1;
   }

   static Map<String, Object> a(JavaCallable var0) {
      try {
         LinkedHashMap var1 = new LinkedHashMap();
         var0.call(null, var1);
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var1);
         return (Map<String, Object>)io.karatelabs.ext.rules.twin.CanonicalJson.d(var1);
      } catch (TwinException var2) {
         throw var2;
      } catch (RuntimeException var3) {
         throw TwinException.a(null, "init", "must build a canonical world before a route can resolve against it: " + io.karatelabs.ext.rules.twin.ThrowableUtils.a(var3));
      }
   }

   static io.karatelabs.ext.rules.twin.TwinModel.CommandModel a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var1) {
      io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var2 = var0.q();
      if (var2 == null) {
         return var0;
      } else {
         var var3 = var0.n();
         Object var4 = var0.m();
         if (var2.d()) {
            var4 = new io.karatelabs.ext.rules.twin.TwinSourceCompiler.ResourceIdentityCaptureCallable(var0.m(), var1.c());
            if (!var3.contains(var1.c())) {
               ArrayList<java.lang.String> var5 = new ArrayList(var3);
               var5.add(var1.c());
               var3 = List.copyOf(var5);
            }
         }

         return new io.karatelabs.ext.rules.twin.TwinModel.CommandModel(
            var0.a(),
            var0.b(),
            var0.c(),
            var0.d(),
            var0.e(),
            var0.f(),
            var0.g(),
            var0.h(),
            var0.i(),
            var0.j(),
            new io.karatelabs.ext.rules.twin.TwinSourceCompiler.HttpRequestCallable(var0.a(), var2.b(), b(var2.c()), var0.g() != null || var0.h()),
            (JavaCallable)(var0.l() != null ? var0.l() : new TwinSourceCompiler.WireOutcomeObserver(a(var2), var2.e().b(), var1.f())),
            (JavaCallable)var4,
            var3,
            var0.o(),
            var0.p(),
            var2,
            var0.r()
         );
      }
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var0, Object var1) {
      String var2 = var1 == null ? var0.b() : var0.a();
      return a((Map<?, ?>)new io.karatelabs.ext.rules.twin.TwinSourceCompiler.HttpRequestCallable("read" + var0.h(), "GET", b(var2), false).call(null, var1));
   }

   static List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> b(String var0) {
      ArrayList<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> var1 = new ArrayList();
      int var2 = 0;

      while (var2 < var0.length()) {
         int var3 = var0.indexOf(123, var2);
         if (var3 < 0) {
            var1.add(new io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment(var0.substring(var2), false));
            break;
         }

         if (var3 > var2) {
            var1.add(new io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment(var0.substring(var2, var3), false));
         }

         int var4 = var0.indexOf(125, var3);
         var1.add(new io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment(var0.substring(var3 + 1, var4), true));
         var2 = var4 + 1;
      }

      return List.copyOf(var1);
   }

   private static String c(String var0, String var1, Object var2) {
      String var3 = var2 instanceof String var4 ? var4 : io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);

      ByteBuffer var9;
      try {
         var9 = StandardCharsets.UTF_8
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(var3));
      } catch (CharacterCodingException var8) {
         throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("command '" + var0 + "' cannot encode the path parameter '" + var1 + "': the world's value is not text the wire can carry");
      }

      StringBuilder var5 = new StringBuilder(var3.length());

      while (var9.hasRemaining()) {
         byte var6 = var9.get();
         char var7 = (char)(var6 & 255);
         if ((var7 < 'a' || var7 > 'z') && (var7 < 'A' || var7 > 'Z') && (var7 < '0' || var7 > '9') && var7 != '-' && var7 != '.' && var7 != '_' && var7 != '~'
            )
          {
            var5.append('%').append(g[var6 >> 4 & 15]).append(g[var6 & 15]);
         } else {
            var5.append(var7);
         }
      }

      return var5.toString();
   }

   private static Map<String, Object> a(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static boolean d(String var0, String var1, Object var2) {
      if (var2 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         return false;
      } else if (var2 instanceof Boolean var3) {
         return var3;
      } else {
         throw TwinException.a(var0, var1, "must be true or false");
      }
   }

   private static String c(Object var0) {
      return var0 instanceof String var1 && !var1.isEmpty() ? var1 : null;
   }

   private static String c(String var0) {
      String var1 = var0.substring(var0.lastIndexOf(47) + 1);
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1.split("[-_]")) {
         if (!var6.isEmpty()) {
            var2.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }
      }

      return var2.toString();
   }

   private record WireOutcomeObserver(int a, Map<String, List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>> b, String c) implements JavaCallable {

      private WireOutcomeObserver(int a, Map<String, List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>> b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }

      @Override
      public Object call(Context var1, Object... var2) {
         Map<java.lang.String, Object> var3 = var2.length > 0 && var2[0] instanceof Map var4 ? io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var4) : Map.of();
         int var10 = var3.get("status") instanceof Number var11 ? var11.intValue() : -1;
         if (var10 == this.a) {
            return Map.of("kind", "applied");
         } else {
            Object var12 = var3.get("body") instanceof Map var13 ? var13.get(this.c) : null;

            for (Entry var15 : this.b.entrySet()) {
               for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var9 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var15.getValue())) {
                  if (var9.a() == var10 && var9.b().equals(var12)) {
                     return Map.of("kind", "refused", "reason", (String)var15.getKey());
                  }
               }
            }

            return Map.of("kind", "unknown");
         }
      }
   }

   private record ResourceIdentityCaptureCallable(JavaCallable a, String b) implements JavaCallable {

      private ResourceIdentityCaptureCallable(JavaCallable a, String b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public Object call(Context var1, Object... var2) {
         if (this.a != null) {
            this.a.call(var1, var2);
         }

         if ((var2.length > 1 && var2[1] instanceof Map var4 ? var4.get("body") : null) instanceof Map var6 && var6.containsKey(this.b)) {
            io.karatelabs.ext.rules.twin.TwinSourceCompiler.a((Map<?, ?>)var2[0]).put(this.b, io.karatelabs.ext.rules.twin.CanonicalJson.d(var6.get(this.b)));
            return null;
         } else {
            throw new IllegalStateException("the response body carries no '" + this.b + "'");
         }
      }
   }

   private record HttpRequestCallable(String a, String b, List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> c, boolean d) implements JavaCallable {

      private HttpRequestCallable(String a, String b, List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> c, boolean d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }

      @Override
      public Object call(Context var1, Object... var2) {
         Map<java.lang.String, Object> var3 = var2.length > 0 && var2[0] instanceof Map var4 ? io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var4) : Map.of();
         StringBuilder var7 = new StringBuilder();

         for (io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment var6 : this.c) {
            var7.append(var6.b() ? io.karatelabs.ext.rules.twin.TwinSourceCompiler.c(this.a, var6.a(), var3.get(var6.a())) : var6.a());
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
         var9.put("method", this.b);
         var9.put("path", var7.toString());
         if (this.d && var2.length > 1 && var2[1] != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2[1])) {
            var9.put("body", var2[1]);
         }

         return var9;
      }
   }

   record PathTemplateSegment(String a, boolean b) {

      PathTemplateSegment(String a, boolean b) {
         this.a = a;
         this.b = b;
      }
   }
}
