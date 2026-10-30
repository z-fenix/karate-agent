package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Json;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class TwinHttpRouter implements Function<HttpRequest, HttpResponse> {
   public static final String a = "play";
   private static final String b = "application/json";
   private static final String c = "args_invalid";
   private static final String d = "not_found";
   private static final String e = "method_not_allowed";
   private static final String f = "WIRE_UNDECLARED";
   private static final String g = "RESPONSE_INVALID";
   private static final String h = "STEP_ERROR";
   private static final Object i = new Object();
   private static final Object j = new Object();
   private final TwinModel k;
   private final io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel l;
   private final String m;
   private final RulebookRuntime n;
   private final Path o;
   private final boolean p;
   private final Map<String, Object> q;
   private final Map<String, Object> r;
   private final String s;
   private final List<io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet> t;
   private final Map<String, io.karatelabs.ext.jsonschema.validation.CompiledSchema> u = new LinkedHashMap<>();
   private final String v;
   private final boolean w;
   private final LongSupplier x;
   private final Map<String, Object> y = new LinkedHashMap<>();
   private final Map<String, io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession> z = new LinkedHashMap<>();
   private final Map<String, Integer> A = new LinkedHashMap<>();
   private Object B;
   private Map<String, Object> C;

   public TwinHttpRouter(TwinModel var1, String var2, io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var3, Map<String, String> var4, Map<String, Object> var5, Path var6, boolean var7, RulebookRuntime var8) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, new SecureRandom()::nextLong);
   }

   TwinHttpRouter(TwinModel var1, String var2, io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var3, Map<String, String> var4, Map<String, Object> var5, Path var6, boolean var7, RulebookRuntime var8, LongSupplier var9) {
      this.x = var9;
      this.k = var1;
      this.l = var1.k();
      this.m = var2;
      this.n = var8;
      this.o = var6;
      this.p = var7;
      this.q = new LinkedHashMap<>(var5);
      this.s = var3.c();
      LinkedHashMap var10 = new LinkedHashMap();
      var10.put("twin", var4.get("twin"));
      var10.put("schema", var4.get("schema"));
      this.r = var10;
      this.v = "list" + this.l.h();
      this.w = a(this.l);
      this.t = this.b(var3.b());
   }

   static TwinHttpRouter a(TwinModel var0, String var1, io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var2, Map<String, String> var3, Map<String, Object> var4, RulebookRuntime var5, Object var6) {
      TwinHttpRouter var7 = new TwinHttpRouter(var0, var1, var2, var3, var4, null, true, var5, () -> 0L);
      var7.B = var6;
      return var7;
   }

   Map<String, Object> a(Map<String, Object> var1) {
      HttpRequest var2 = new HttpRequest();
      var2.setMethod(String.valueOf(var1.get("method")));
      var2.setPath(String.valueOf(var1.get("path")));
      if (var1.containsKey("body")) {
         var2.setBody(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get("body")).getBytes(StandardCharsets.UTF_8));
      }

      synchronized (this) {
         this.C = null;
         this.b(var2);
         return this.C;
      }
   }

   public static String a(TwinModel var0) {
      if (var0.j() != null) {
         return "t.reset";
      } else {
         return var0.i() != null ? "t.readBack" : null;
      }
   }

   private List<io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet> b(Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 : this.k.c()) {
         if (var4.q() != null) {
            var2.put(var4.q().a(), var4);
         }
      }

      ArrayList var13 = new ArrayList();
      Map var14 = (Map)var1.get("paths");

      for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var14.entrySet())) {
         LinkedHashMap var7 = new LinkedHashMap();

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (((Map)var6.getValue()).entrySet())) {
            Map var10 = (Map)var9.getValue();
            String var11 = String.valueOf(var10.get("operationId"));
            var7.put(((String)var9.getKey()).toUpperCase(), new io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute(var11, (io.karatelabs.ext.rules.twin.TwinModel.CommandModel)var2.get(var11)));
            this.a(var11, (Map<String, Object>)var10.get("responses"), var1);
         }

         ArrayList var15 = new ArrayList();

         for (String var12 : ((String)var6.getKey()).split("/", -1)) {
            var15.add(io.karatelabs.ext.rules.twin.TwinSourceCompiler.b(var12));
         }

         var13.add(new io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet(List.copyOf(var15), var7));
      }

      return List.copyOf(var13);
   }

   private void a(String var1, Map<String, Object> var2, Map<String, Object> var3) {
      for (Entry var5 : var2.entrySet()) {
         if (((Map)var5.getValue()).get("content") instanceof Map var7 && var7.get("application/json") instanceof Map var8) {
            this.u.put(var1 + " " + (String)var5.getKey(), new io.karatelabs.ext.jsonschema.validation.SchemaRegistry(io.karatelabs.ext.jsonschema.validation.SchemaDialect.V2020_12).register("", var3).compile(var8.get("schema")));
         }
      }
   }

   @Override
   public HttpResponse apply(HttpRequest request) {
      return this.a(request);
   }

   public HttpResponse a(HttpRequest var1) {
      synchronized (this) {
         return this.b(var1);
      }
   }

   private HttpResponse b(HttpRequest var1) {
      io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRouteMatch var2 = this.a(c(var1));
      if (var2 == null) {
         return a(404, this.d("not_found"));
      } else {
         io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute var3 = var2.a().b().get(var1.getMethod().toUpperCase());
         if (var3 == null) {
            return a(405, this.d("method_not_allowed"));
         } else {
            return var3.b() == null ? this.a(var3, var2.b()) : this.a(var1, var3, var2.b());
         }
      }
   }

   private static List<String> c(HttpRequest var0) {
      String var1 = var0.getPathRaw();
      int var2 = var1.length();

      for (char var6 : new char[]{'?', '#'}) {
         int var7 = var1.indexOf(var6);
         if (var7 >= 0 && var7 < var2) {
            var2 = var7;
         }
      }

      return List.of(var1.substring(0, var2).split("/", -1));
   }

   private static String b(String var0) {
      if (var0.indexOf(37) < 0) {
         return var0;
      } else {
         ByteArrayOutputStream var1 = new ByteArrayOutputStream(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            int var4 = var3 == '%' && var2 + 2 < var0.length() ? a(var0, var2 + 1) : -1;
            if (var4 >= 0) {
               var1.write(var4);
               var2 += 2;
            } else {
               var1.writeBytes(String.valueOf(var3).getBytes(StandardCharsets.UTF_8));
            }
         }

         return var1.toString(StandardCharsets.UTF_8);
      }
   }

   private static int a(String var0, int var1) {
      int var2 = Character.digit(var0.charAt(var1), 16);
      int var3 = Character.digit(var0.charAt(var1 + 1), 16);
      return var2 >= 0 && var3 >= 0 ? var2 * 16 + var3 : -1;
   }

   private io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRouteMatch a(List<String> var1) {
      io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRouteMatch var2 = null;
      int var3 = Integer.MAX_VALUE;

      for (io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet var5 : this.t) {
         if (var5.a().size() == var1.size()) {
            int var6 = 0;

            for (List var8 : var5.a()) {
               for (io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment var10 : (Iterable<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment>) (Iterable<?>) (var8)) {
                  var6 += var10.b() ? 1 : 0;
               }
            }

            if (var6 < var3) {
               var var11 = this.a(var5, var1);
               if (var11 != null) {
                  var2 = new io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRouteMatch(var5, var11);
                  var3 = var6;
               }
            }
         }
      }

      return var2;
   }

   private Map<String, String> a(io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet var1, List<String> var2) {
      ArrayList var3 = new ArrayList(var2.size());

      for (int var4 = 0; var4 < var2.size(); var4++) {
         var var5 = a(var1.a().get(var4), (String)var2.get(var4));
         if (var5.isEmpty()) {
            return null;
         }

         var3.add(var5);
      }

      ArrayList var6 = new ArrayList(1);
      Map var7 = this.a(var3, 0, new LinkedHashMap<>(), var6);
      return var7 != null ? var7 : (Map)var6.get(0);
   }

   private Map<String, String> a(List<List<Map<String, String>>> var1, int var2, Map<String, String> var3, List<Map<String, String>> var4) {
      if (var2 == var1.size()) {
         var var9 = new LinkedHashMap<>(var3);
         if (var4.isEmpty()) {
            var4.add(var9);
         }

         return this.c(var9) ? var9 : null;
      } else {
         for (Map var6 : (Iterable<Map>) (Iterable<?>) ((List)var1.get(var2))) {
            var var7 = new LinkedHashMap<>(var3);
            var7.putAll(var6);
            var var8 = this.a(var1, var2 + 1, var7, var4);
            if (var8 != null) {
               return var8;
            }
         }

         return null;
      }
   }

   private static List<Map<String, String>> a(List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> var0, String var1) {
      ArrayList var2 = new ArrayList(1);
      a(var0, var1, 0, 0, new LinkedHashMap<>(), true, var2);
      String var3 = b(var1);
      if (var2.isEmpty() && !var3.equals(var1)) {
         a(var0, var3, 0, 0, new LinkedHashMap<>(), false, var2);
      }

      return var2;
   }

   private static void a(
      List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment> var0, String var1, int var2, int var3, Map<String, String> var4, boolean var5, List<Map<String, String>> var6
   ) {
      if (var2 == var0.size()) {
         if (var3 == var1.length()) {
            var6.add(var4);
         }
      } else {
         io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment var7 = (io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment)var0.get(var2);
         if (!var7.b()) {
            if (var1.startsWith(var7.a(), var3)) {
               a(var0, var1, var2 + 1, var3 + var7.a().length(), var4, var5, var6);
            }
         } else {
            io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment var8 = var2 + 1 < var0.size() ? (io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment)var0.get(var2 + 1) : null;

            for (int var9 = var8 == null ? var1.length() : var1.indexOf(var8.a(), var3 + 1);
               var9 > var3;
               var9 = var8 == null ? -1 : var1.indexOf(var8.a(), var9 + 1)
            ) {
               String var10 = var1.substring(var3, var9);
               var var11 = new LinkedHashMap<>(var4);
               var11.put(var7.a(), var5 ? b(var10) : var10);
               a(var0, var1, var2 + 1, var9, var11, var5, var6);
            }
         }
      }
   }

   private boolean c(Map<String, String> var1) {
      Object var2 = this.y.get(var1.get(this.l.c()));
      return var2 != null && this.a(var2, var1);
   }

   private HttpResponse a(io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute var1, Map<String, String> var2) {
      if (var1.a().equals(this.v)) {
         ArrayList var7 = new ArrayList(this.y.size());

         for (Object var5 : this.y.values()) {
            Object var6 = this.a(var5);
            if (var6 == null) {
               return a(500, this.c((String)null));
            }

            var7.add(var6);
         }

         return this.a(var1.a(), 200, var7, null);
      } else {
         Object var3 = this.y.get(var2.get(this.l.c()));
         if (var3 != null && this.a(var3, var2)) {
            Object var4 = this.a(var3);
            return var4 == null ? a(500, this.c((String)null)) : this.a(var1.a(), 200, var4, null);
         } else {
            return this.a(var1.a(), 404, this.d("not_found"), null);
         }
      }
   }

   private boolean a(Object var1, Map<String, String> var2) {
      var var3 = d(var1);

      for (Entry var5 : var2.entrySet()) {
         if (!((String)var5.getKey()).equals(this.l.c()) && !((String)var5.getValue()).equals(c(var3.get(var5.getKey())))) {
            return false;
         }
      }

      return true;
   }

   private HttpResponse a(HttpRequest var1, io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute var2, Map<String, String> var3) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 = var2.b();
      Object var5 = d(var1);

      Map var6;
      try {
         var6 = a(var4, var5);
      } catch (io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException var13) {
         return this.a(var2.a(), 400, this.b(var13.a), null);
      }

      io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var7 = var4.q();
      Object var8;
      String var9;
      if (var7.d()) {
         var8 = io.karatelabs.ext.rules.twin.CanonicalJson.d(this.k.l());
         var9 = null;
      } else {
         var9 = (String)var3.get(this.l.c());
         var8 = this.y.get(var9);
         if (var8 == null || !this.a(var8, var3)) {
            return this.a(var2.a(), 404, this.d("not_found"), null);
         }
      }

      var var10 = d(var4.k().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var8), io.karatelabs.ext.rules.twin.CanonicalJson.d(var6)));
      io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var11 = CommandExecutor.a(this.k, var8, var4, var6, this.n);
      if ("error".equals(var11.b())) {
         var var12 = this.d("STEP_ERROR");
         var12.put("command", var4.a());
         var12.put("stage", var11.c());
         if (var11.f() != null) {
            var12.put("code", var11.f());
         }

         var12.put("message", var11.e());
         return this.a(var9, var4, var6, var8, var8, var10, a(500, var12), "error", var11);
      } else {
         return "refused".equals(var11.b()) ? this.a(var9, var4, var6, var8, var10, var11, var2.a()) : this.a(var9, var4, var6, var8, var10, var11, var2, var7);
      }
   }

   private HttpResponse a(String var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3, Object var4, Map<String, Object> var5, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var6, String var7) {
      String var8 = "guard".equals(var6.c()) ? var2.o().get("guard") : ("apply".equals(var6.c()) ? var6.d() : null);
      List var9 = var8 == null ? null : var2.q().e().b().get(var8);
      if (var9 != null && !var9.isEmpty()) {
         io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var12 = (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal)var9.get(0);
         HttpResponse var11 = this.a(var7, var12.a(), this.d(var12.b()), var2.a());
         return this.a(var1, var2, var3, var4, var4, var5, var11, "refused", var6);
      } else {
         var var10 = this.d("WIRE_UNDECLARED");
         var10.put("command", var2.a());
         var10.put("reason", var8 == null ? var6.d() : var8);
         return this.a(var1, var2, var3, var4, var4, var5, a(500, var10), "refused", var6);
      }
   }

   private HttpResponse a(
      String var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3, Object var4, Map<String, Object> var5, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var6, io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute var7, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var8
   ) {
      Object var9 = var6.j();
      String var10 = var1;
      if (var8.d()) {
         Object var11 = this.a(var2);
         d(var9).put(this.l.c(), var11);
         var10 = c(var11);
      }

      Object var13 = this.a(var9);
      Object var12 = var13 == null
         ? List.of(Map.of("field", "resource.view", "reason", "the view has no canonical form"))
         : this.a(var7.a(), io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var8), var13);
      if (var12 != null) {
         return this.a(var1, var2, var3, var4, var4, var5, a(500, this.a(var2.a(), var12)), "error", var6);
      } else {
         this.y.put(var10, var9);
         return this.a(var10, var2, var3, var4, var9, var5, a(io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var8), var13), "applied", var6);
      }
   }

   private Object a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1) {
      int var2 = this.A.merge(var1.a(), 1, Integer::sum);
      if (this.B != null) {
         Object var6 = this.B;
         this.B = null;
         return var6;
      } else {
         Object var3 = this.k.l().get(this.l.c());
         if (var3 instanceof Number) {
            return (long)var2;
         } else if (var3 instanceof String var4 && !var4.isEmpty()) {
            int var5 = var4.length();

            while (var5 > 0 && Character.isDigit(var4.charAt(var5 - 1))) {
               var5--;
            }

            return var4.substring(0, var5) + var2;
         } else {
            return this.w ? (long)var2 : String.valueOf(var2);
         }
      }
   }

   private static boolean a(io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var0) {
      Object var7 = ((var0.e() instanceof Map var2 ? var2.get("properties") : null) instanceof Map var6 ? var6.get(var0.c()) : null) instanceof Map var4
         ? var4.get("type")
         : null;
      return "integer".equals(var7) || "number".equals(var7);
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, Object var1) {
      var var2 = var0.g();
      if (var2 == null) {
         if (var1 != i) {
            throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException("body", "'" + var0.a() + "' declares no args and takes no body");
         } else {
            return Map.of();
         }
      } else if (var1 == j) {
         throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException("body", "must be a JSON object");
      } else {
         Map var3 = var1 == i ? Map.of() : (var1 instanceof Map var4 ? d(var4) : null);
         if (var3 == null) {
            throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException("body", "must be a JSON object");
         } else {
            LinkedHashSet var12 = new LinkedHashSet();

            for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var6 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis>) (Iterable<?>) (var2)) {
               var12.add(var6.a());
            }

            for (String var15 : (Iterable<String>) (Iterable<?>) (var3.keySet())) {
               if (!var12.contains(var15)) {
                  throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException(var15, "is not a declared argument of '" + var0.a() + "'");
               }
            }

            LinkedHashMap var14 = new LinkedHashMap();

            for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var7 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis>) (Iterable<?>) (var2)) {
               if (!var3.containsKey(var7.a())) {
                  throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException(var7.a(), "is required");
               }

               Object var8 = var3.get(var7.a());

               boolean var9;
               try {
                  var9 = TwinModel.a(var7, var8);
               } catch (RuntimeException var11) {
                  var9 = false;
               }

               if (!var9) {
                  throw new io.karatelabs.ext.rules.twin.TwinHttpRouter.RequestValidationException(var7.a(), "is outside its declared domain");
               }

               var14.put(var7.a(), var8);
            }

            return var14;
         }
      }
   }

   private static Object d(HttpRequest var0) {
      byte[] var1 = var0.getBody();
      if (var1 != null && var1.length != 0) {
         String var2 = var0.getBodyString();
         if (var2 == null) {
            return j;
         } else {
            try {
               return Json.parseStrict("[" + var2 + "]", true) instanceof List var4 && var4.size() == 1 ? var4.get(0) : j;
            } catch (RuntimeException var5) {
               return j;
            }
         }
      } else {
         return i;
      }
   }

   private HttpResponse a(String var1, int var2, Object var3, String var4) {
      Object var5 = this.a(var1, var2, var3);
      return var5 == null ? a(var2, var3) : a(500, this.a(var4, var5));
   }

   private Object a(String var1, int var2, Object var3) {
      io.karatelabs.ext.jsonschema.validation.CompiledSchema var4 = this.u.get(var1 + " " + var2);
      if (var4 == null) {
         return null;
      } else {
         io.karatelabs.ext.jsonschema.validation.ValidationResult var5 = var4.validate(var3);
         return var5.valid ? null : var5.toMap().get("errors");
      }
   }

   private Map<String, Object> a(String var1, Object var2) {
      var var3 = this.d("RESPONSE_INVALID");
      if (var1 != null) {
         var3.put("command", var1);
      }

      var3.put("errors", var2);
      return var3;
   }

   private Object a(Object var1) {
      try {
         Object var2 = this.l.d().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1));
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var2);
         return io.karatelabs.ext.rules.twin.CanonicalJson.d(var2);
      } catch (RuntimeException var3) {
         return null;
      }
   }

   private Map<String, Object> c(String var1) {
      return this.a(var1, List.of(Map.of("field", "resource.view", "reason", "the view has no canonical form")));
   }

   private Map<String, Object> d(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put(this.l.f(), var1);
      return var2;
   }

   private Map<String, Object> b(List<Map<String, Object>> var1) {
      var var2 = this.d("args_invalid");
      var2.put("errors", var1);
      return var2;
   }

   private static HttpResponse a(int var0, Object var1) {
      return HttpResponse.json(var0, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
   }

   private HttpResponse a(
      String var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3, Object var4, Object var5, Map<String, Object> var6, HttpResponse var7, String var8, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var9
   ) {
      if (this.p && var1 != null) {
         io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession var10 = this.z.get(var1);
         if (var10 == null) {
            var10 = this.b(d(var5).get(this.l.c()));
            this.z.put(var1, var10);
         }

         LinkedHashMap var11 = new LinkedHashMap();
         var11.put("command", var2.a());
         var11.put("args", var3);
         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("status", var7.getStatus());
         var12.put("body", a(var7));
         var11.put("exchange", Map.of("request", var6, "response", var12));
         var11.put("disposition", var8);
         if (var9.c() != null) {
            var11.put("stage", var9.c());
         }

         if (var9.d() != null) {
            var11.put("reason", var9.d());
         }

         var11.put("worldHashBefore", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var4)));
         var11.put("worldHashAfter", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var5)));
         var10.d.add(var11);
         this.C = var11;
         if (var10.c != null) {
            this.a(var10);
         }

         return var7;
      } else {
         return var7;
      }
   }

   private io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession b(Object var1) {
      if (this.o == null) {
         return new io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession(null, var1, null);
      } else {
         Path var2 = this.o.resolve("target").resolve("draft").resolve("play");

         try {
            Files.createDirectories(var2);
         } catch (IOException var6) {
            throw new UncheckedIOException(var6);
         }

         int var3 = 0;

         while (var3 < 100) {
            String var4 = String.format("play-%016x", this.x.getAsLong());

            try {
               Files.createDirectory(var2.resolve(var4));
               return new io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession(var4, var1, var2.resolve(var4).resolve("record.json"));
            } catch (FileAlreadyExistsException var7) {
               var3++;
            } catch (IOException var8) {
               throw new UncheckedIOException(var8);
            }
         }

         throw new IllegalStateException("could not mint a unique record id after 100 attempts");
      }
   }

   private void a(io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("id", var1.a);
      var2.put("rulebook", this.m);
      var2.put("identity", var1.b);
      var var3 = new LinkedHashMap<>(this.r);
      var var4 = new LinkedHashMap<>(this.q);
      this.n.a().forEach((var1x, var2x) -> var4.put(var1x, var2x.a()));
      var3.put("rulebooks", var4);
      var3.put("projection", this.s);
      var2.put("manifest", var3);
      var2.put("steps", var1.d);
      var2.put("digest", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2)));

      try {
         Files.writeString(var1.c, io.karatelabs.ext.rules.twin.CanonicalJson.c(var2), StandardCharsets.UTF_8);
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   private static Object a(HttpResponse var0) {
      String var1 = var0.getBodyString();
      if (var1 != null && !var1.isEmpty()) {
         return Json.parseStrict("[" + var1 + "]", true) instanceof List var3 && var3.size() == 1 ? var3.get(0) : var1;
      } else {
         return null;
      }
   }

   public void a() {
      synchronized (this) {
         this.y.clear();
         this.z.clear();
         this.A.clear();
      }
   }

   public String b() {
      return this.m;
   }

   public boolean c() {
      return this.p && this.o != null;
   }

   public Path d() {
      return this.o;
   }

   public Object a(String var1) {
      synchronized (this) {
         if (!"records".equals(var1)) {
            if ("worlds".equals(var1)) {
               LinkedHashMap var9 = new LinkedHashMap();
               this.y.forEach((var1x, var2) -> var9.put(var1x, io.karatelabs.ext.rules.twin.CanonicalJson.d(var2)));
               return var9;
            } else {
               return null;
            }
         } else {
            ArrayList var3 = new ArrayList(this.z.size());

            for (io.karatelabs.ext.rules.twin.TwinHttpRouter.PlaySession var5 : this.z.values()) {
               LinkedHashMap var6 = new LinkedHashMap();
               var6.put("id", var5.a);
               var6.put("identity", var5.b);
               var6.put("path", var5.c.toString());
               var3.add(var6);
            }

            return var3;
         }
      }
   }

   private static String c(Object var0) {
      return var0 instanceof String var1 ? var1 : io.karatelabs.ext.rules.twin.CanonicalJson.c(var0);
   }

   private static Map<String, Object> d(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static final class RequestValidationException extends RuntimeException {
      private final transient List<Map<String, Object>> a;

      RequestValidationException(String var1, String var2) {
         super(var2, null, false, false);
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("field", var1);
         var3.put("reason", var2);
         this.a = List.of(var3);
      }
   }

   private record OperationRouteMatch(io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet a, Map<String, String> b) {

      private OperationRouteMatch(io.karatelabs.ext.rules.twin.TwinHttpRouter.PathRouteSet a, Map<String, String> b) {
         this.a = a;
         this.b = b;
      }
   }

   private record OperationRoute(String a, io.karatelabs.ext.rules.twin.TwinModel.CommandModel b) {

      private OperationRoute(String a, io.karatelabs.ext.rules.twin.TwinModel.CommandModel b) {
         this.a = a;
         this.b = b;
      }
   }

   private static final class PlaySession {
      private final String a;
      private final Object b;
      private final Path c;
      private final List<Map<String, Object>> d = new ArrayList<>();

      private PlaySession(String var1, Object var2, Path var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
      }
   }

   private record PathRouteSet(List<List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment>> a, Map<String, io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute> b) {

      private PathRouteSet(List<List<io.karatelabs.ext.rules.twin.TwinSourceCompiler.PathTemplateSegment>> a, Map<String, io.karatelabs.ext.rules.twin.TwinHttpRouter.OperationRoute> b) {
         this.a = a;
         this.b = b;
      }
   }
}
