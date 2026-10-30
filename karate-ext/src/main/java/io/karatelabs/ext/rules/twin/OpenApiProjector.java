package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

public final class OpenApiProjector {
   public static final String a = "openapi.yaml";
   private static final String b = "application/json";
   private static final String c = "args_invalid";
   private static final String d = "not_found";
   private final TwinModel e;
   private final String f;
   private final io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel g;
   private final List<Map<String, Object>> h = new ArrayList<>();
   private final List<Map<String, Object>> i = new ArrayList<>();

   private OpenApiProjector(TwinModel var1, String var2) {
      this.e = var1;
      this.f = var2;
      this.g = var1.k();
   }

   public static io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection a(TwinModel var0, String var1) {
      if (var0.k() == null) {
         throw TwinException.a("BINDING_MISSING", null, "resource", "the twin declares no transport binding — declare t.resource and a route per command");
      } else {
         return new OpenApiProjector(var0, var1).a();
      }
   }

   private io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection a() {
      var var1 = this.b();
      if (!(var1.get("properties") instanceof Map var2 && var2.containsKey(this.g.c()))) {
         this.i
            .add(
               a(
                  "IDENTITY_UNPROJECTED",
                  "resource.schema.properties",
                  "the view schema does not declare the identity '" + this.g.c() + "', so no response can answer which " + this.g.h() + " it is about"
               )
            );
      }

      LinkedHashMap var6 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 : this.e.c()) {
         io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var5 = var4.q();
         if (var5 == null) {
            this.h.add(a(var4.a(), var4.k() != null ? "AUTHORED_TRANSPORT" : "UNBOUND"));
         } else if (var4.h()) {
            this.h.add(a(var4.a(), "ARGS_LITERAL"));
         } else {
            a(var6, var5.c(), var5.b().toLowerCase(), this.a(var4, var5));
         }
      }

      this.a(var6);

      for (String var10 : io.karatelabs.ext.rules.twin.TwinSourceCompiler.a) {
         LinkedHashMap<String, Object> var12 = new LinkedHashMap();
         var12.put("kind", "fence");
         var12.put("name", var10);
         this.h.add(var12);
      }

      if (!this.i.isEmpty()) {
         return new io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection(false, null, null, null, List.copyOf(this.h), List.copyOf(this.i));
      } else {
         LinkedHashMap<java.lang.String, java.lang.String> var9 = new LinkedHashMap();
         var9.put("title", this.f);
         var9.put("version", "1.0.0");
         var9.put("description", "Projected from the twin.js of rulebook '" + this.f + "'.");
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("openapi", "3.1.0");
         var11.put("info", var9);
         var11.put("paths", var6);
         var11.put("components", Map.of("schemas", Map.of(this.g.h(), var1)));
         var var13 = b(var11);
         return new io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection(
            true, var13, "sha256:" + io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.common.CanonicalJson.a(var13)), c(var13), List.copyOf(this.h), List.copyOf(this.i)
         );
      }
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("operationId", var2.a());
      var3.put("summary", var1.a());
      var3.put("description", b(var1, var2));
      var3.put("tags", List.of(this.g.h()));
      if (var1.j() != null) {
         var3.put("x-karate-req", var1.j());
      }

      var var4 = this.a(var2.c());
      if (!var4.isEmpty()) {
         var3.put("parameters", var4);
      }

      boolean var5 = var1.g() != null && !var1.g().isEmpty();
      if (var5) {
         var3.put("requestBody", this.a(var1));
      }

      var3.put("responses", this.a(var1, var2, var5));
      return var3;
   }

   private static String b(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var1) {
      StringBuilder var2 = new StringBuilder("The twin command '").append(var0.a()).append("'");
      if (var0.b() != null) {
         var2.append(", guarded by its declared precondition");
      }

      var2.append('.');
      if (!var1.e().b().isEmpty()) {
         var2.append(" Refuses: ").append(String.join(", ", new TreeSet<>(var1.e().b().keySet()))).append('.');
      }

      return var2.toString();
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var3 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var5 : var1.g()) {
         var2.add(var5.a());
         var3.put(var5.a(), a(var5));
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("type", "object");
      var7.put("additionalProperties", false);
      var7.put("required", var2);
      var7.put("properties", var3);
      LinkedHashMap<java.lang.String, java.util.LinkedHashMap<java.lang.String,java.lang.Object>> var8 = new LinkedHashMap();
      var8.put("schema", var7);
      if (!var1.f().isEmpty()) {
         var var6 = new LinkedHashMap<>(var1.f().get(0));
         if (var1.r() != null) {
            var6.putAll(var1.r());
         }

         var8.put("example", var6);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("required", true);
      var9.put("content", Map.of("application/json", var8));
      return var9;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      String var2 = var0.b();
      switch (var2) {
         case "int":
            var1.put("type", "integer");
            var1.put("minimum", var0.d());
            var1.put("maximum", var0.e());
            break;
         case "bool":
            var1.put("type", "boolean");
            break;
         case "const":
            var1.put("const", var0.c().get(0));
            break;
         default:
            String var4 = a((Collection<?>)var0.c());
            if (var4 != null) {
               var1.put("type", var4);
            }

            var1.put("enum", var0.c());
      }

      return var1;
   }

   private List<Map<String, Object>> a(String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (String var4 : io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var1)) {
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
         var5.put("type", "string");
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("name", var4);
         var6.put("in", "path");
         var6.put("required", true);
         var6.put("schema", var5);
         var2.add(var6);
      }

      return var2;
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding var2, boolean var3) {
      TreeMap<java.lang.Integer, java.util.TreeSet> var4 = new TreeMap();
      TreeMap<java.lang.Object, java.util.Set<java.lang.String>> var5 = new TreeMap();

      for (Entry var7 : var2.e().b().entrySet()) {
         for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var9 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var7.getValue())) {
            var4.computeIfAbsent(var9.a(), var0 -> new TreeSet<>()).add(var9.b());
            var5.computeIfAbsent(var9.a(), var0 -> new TreeSet<>()).add((String)var7.getKey());
         }
      }

      int var10 = io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var2);
      if (var3 && var10 != 400) {
         var4.computeIfAbsent(400, var0 -> new TreeSet<>()).add("args_invalid");
         var5.computeIfAbsent(400, var0 -> new TreeSet<>()).add("the body is outside the declared argument domain");
      }

      if (var10 != 404 && io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var2.c()).contains(this.g.c())) {
         var4.computeIfAbsent(404, var0 -> new TreeSet<>()).add("not_found");
         var5.computeIfAbsent(404, var0 -> new TreeSet<>()).add("no " + this.g.h() + " with that " + this.g.c());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
      var11.put(String.valueOf(var10), this.b("Applied: the " + this.g.h() + " view after " + var1.a() + "."));

      for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         var11.put(String.valueOf(var13.getKey()), this.a((Set<String>)var13.getValue(), (Set<String>)var5.get(var13.getKey())));
      }

      return var11;
   }

   private Map<String, Object> b(String var1) {
      return a(var1, Map.of("$ref", "#/components/schemas/" + this.g.h()));
   }

   private Map<String, Object> a(Set<String> var1, Set<String> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("type", "string");
      var3.put("enum", new ArrayList(var1));
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("type", "object");
      var4.put("required", List.of(this.g.f()));
      var4.put("properties", Map.of(this.g.f(), var3));
      return a("Refused: " + String.join(", ", var2) + ".", var4);
   }

   private static Map<String, Object> a(String var0, Object var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("description", var0);
      var2.put("content", Map.of("application/json", Map.of("schema", var1)));
      return var2;
   }

   private void a(Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("operationId", "read" + this.g.h());
      var2.put("summary", "read " + this.g.h());
      var2.put("description", "The view of one " + this.g.h() + ".");
      var2.put("tags", List.of(this.g.h()));
      var2.put("parameters", this.a(this.g.a()));
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var3 = new LinkedHashMap();
      var3.put("200", this.b("The " + this.g.h() + " view."));
      var3.put("404", this.a(new TreeSet<>(List.of("not_found")), new TreeSet<>(List.of("no " + this.g.h() + " with that " + this.g.c()))));
      var2.put("responses", var3);
      a(var1, this.g.a(), "get", var2);
      if (this.g.g()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("type", "array");
         var4.put("items", Map.of("$ref", "#/components/schemas/" + this.g.h()));
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("operationId", "list" + this.g.h());
         var5.put("summary", "list " + this.g.h());
         var5.put("description", "Every " + this.g.h() + " the instance holds.");
         var5.put("tags", List.of(this.g.h()));
         var5.put("responses", Map.of("200", a("The " + this.g.h() + " views.", var4)));
         a(var1, this.g.b(), "get", var5);
      }
   }

   private static void a(Map<String, Object> var0, String var1, String var2, Object var3) {
      ((java.util.Map)(var0.computeIfAbsent(var1, var0x -> new LinkedHashMap<>()))).put(var2, var3);
   }

   private Map<String, Object> b() {
      return (Map<String, Object>)a(this.g.e());
   }

   private static Object a(Object var0) {
      if (var0 instanceof List var7) {
         return var7.stream().map(OpenApiProjector::a).toList();
      } else if (!(var0 instanceof Map var1)) {
         return var0;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            String var5 = String.valueOf(var4.getKey());
            boolean var6 = var5.equals("enum") || var5.equals("const") || var5.equals("default") || var5.equals("example") || var5.equals("examples");
            var2.put(var5, var6 ? var4.getValue() : a(var4.getValue()));
         }

         if (var2.get("enum") instanceof List var8 && !var2.containsKey("type")) {
            String var10 = a((Collection<?>)var8);
            if (var10 != null) {
               var2.put("type", var10);
            }
         }

         return var2;
      }
   }

   private static Map<String, Object> a(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("kind", "command");
      var2.put("command", var0);
      var2.put("reason", var1);
      return var2;
   }

   private static Map<String, Object> a(String var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("code", var0);
      var3.put("field", var1);
      var3.put("reason", var2);
      return var3;
   }

   private static String a(Collection<?> var0) {
      String var1 = null;

      for (Object var3 : var0) {
         String var4 = var3 instanceof String
            ? "string"
            : (
               var3 instanceof Boolean
                  ? "boolean"
                  : (
                     var3 instanceof Number var5 && var5.doubleValue() == Math.rint(var5.doubleValue())
                        ? "integer"
                        : (var3 instanceof Number ? "number" : null)
                  )
            );
         if (var4 == null || var1 != null && !var1.equals(var4)) {
            return null;
         }

         var1 = var4;
      }

      return var1;
   }

   private static Map<String, Object> b(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (String var3 : (Iterable<String>) (Iterable<?>) (new TreeSet(var0.keySet()))) {
         var1.put(var3, b(var0.get(var3)));
      }

      return var1;
   }

   private static Object b(Object var0) {
      if (var0 instanceof Map var5) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var5.forEach((var1x, var2x) -> var6.put(String.valueOf(var1x), var2x));
         return b(var6);
      } else if (!(var0 instanceof List var1)) {
         return var0;
      } else {
         ArrayList<java.lang.Object> var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            var2.add(b(var4));
         }

         return var2;
      }
   }

   private static String c(Object var0) {
      DumperOptions var1 = new DumperOptions();
      var1.setDefaultFlowStyle(FlowStyle.BLOCK);
      var1.setIndent(2);
      var1.setPrettyFlow(true);
      var1.setSplitLines(false);
      return new Yaml(var1).dump(var0);
   }

   public record OpenApiProjection(boolean a, Map<String, Object> b, String c, String d, List<Map<String, Object>> e, List<Map<String, Object>> f) {

      public OpenApiProjection(boolean a, Map<String, Object> b, String c, String d, List<Map<String, Object>> e, List<Map<String, Object>> f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }
}
