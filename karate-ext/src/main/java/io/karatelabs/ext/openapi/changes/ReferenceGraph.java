package io.karatelabs.ext.openapi.changes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

final class ReferenceGraph {
   private static final String a = "securitySchemes";
   private static final Set<String> b = Set.of("example", "default", "enum", "const");
   private static final Set<String> c = Set.of(
      "properties", "patternProperties", "$defs", "definitions", "dependentSchemas", "responses", "content", "headers", "encoding", "callbacks", "links"
   );
   private final Map<String, Map<String, Set<String>>> d = Map.of("baseline", new HashMap<>(), "current", new HashMap<>());
   private final Map<String, Map<String, Set<String>>> e = Map.of("baseline", new HashMap<>(), "current", new HashMap<>());
   private final Map<String, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext> f = new LinkedHashMap<>();
   private final Set<String> g = new HashSet<>();
   private final Map<String, io.karatelabs.ext.openapi.changes.ReferenceGraph.ComponentReferenceImpact> h = new LinkedHashMap<>();

   void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, String var3, Object var4) {
      this.f.putIfAbsent(var2.a(), var2);
      this.a(var1, var2, var3);
      HashSet<java.lang.String> var5 = new HashSet();
      ArrayDeque var6 = new ArrayDeque();
      a(var6, var4, false);

      while (!var6.isEmpty()) {
         Object[] var7 = (Object[])var6.pop();
         boolean var8 = (Boolean)var7[1];
         ArrayList<java.lang.String> var9 = new ArrayList();
         if (var7[0] instanceof List var10) {
            var10.forEach(var1x -> a(var6, var1x, false));
         } else if (var7[0] instanceof Map var11 && var8) {
            var11.values().forEach(var1x -> a(var6, var1x, false));
         } else if (var7[0] instanceof Map var12) {
            if (var12.get("$ref") instanceof String var22) {
               if (var22.startsWith("#")) {
                  var9.add(var22.substring(1));
               }

               if (var1.e) {
                  var12 = Map.of();
               }
            }

            if (var12.get("discriminator") instanceof Map var23 && var23.get("mapping") instanceof Map var25) {
               for (Object var16 : var25.values()) {
                  String var17 = String.valueOf(var16);
                  if (var17.startsWith("#")) {
                     var9.add(var17.substring(1));
                  } else if (!var17.contains("/")) {
                     var9.add("/components/schemas/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var17));
                  }
               }
            }

            for (Entry var26 : (Iterable<Entry>) (Iterable<?>) (var12.entrySet())) {
               String var29 = String.valueOf(var26.getKey());
               if (var29.equals("examples")) {
                  if (var26.getValue() instanceof Map var30) {
                     var30.values().forEach(var4x -> this.a(var1, var2, var4x, var5));
                  }
               } else if (!b.contains(var29) && !var29.startsWith("x-")) {
                  a(var6, var26.getValue(), c.contains(var29));
               }
            }
         }

         for (String var19 : (Iterable<String>) (Iterable<?>) (var9)) {
            if (var5.add(var19)) {
               this.a(var1, var2, var19);
               if (var19.startsWith("/components/examples/")) {
                  this.a(var1, var2, var1.a(var19), var5);
               } else {
                  a(var6, var1.a(var19), false);
               }
            }
         }
      }
   }

   void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, Object var3) {
      HashSet<java.lang.String> var4 = new HashSet();

      while (var3 instanceof Map) {
         Map<java.lang.String, Object> var5 = (Map)var3;
         if (!(var5.get("$ref") instanceof String var6) || !var6.startsWith("#") || !var4.add(var6.substring(1))) {
            break;
         }

         String var8 = var6.substring(1);
         this.e.get(var1.a).computeIfAbsent(var8, var0 -> new LinkedHashSet<>()).add(var2.a());
         this.a(var8);
         var3 = var1.a(var8);
      }
   }

   void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, Object var3, Set<String> var4) {
      while (var3 instanceof Map var5 && var5.get("$ref") instanceof String var6 && var6.startsWith("#") && var4.add(var6.substring(1))) {
         this.a(var1, var2, var6.substring(1));
         var3 = var1.a(var6.substring(1));
      }
   }

   private static void a(Deque<Object[]> var0, Object var1, boolean var2) {
      if (var1 instanceof Map || var1 instanceof List) {
         var0.push(new Object[]{var1, var2});
      }
   }

   private void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, String var3) {
      this.d.get(var1.a).computeIfAbsent(var3, var0 -> new LinkedHashSet<>()).add(var2.a());
      this.a(var3);
   }

   private void a(String var1) {
      String[] var2 = var1.split("/", 5);
      if (var2.length >= 4 && var2[1].equals("components")) {
         this.g.add("/components/" + var2[2] + "/" + var2[3]);
      }
   }

   void a(DiffSession var1) {
      LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();

      for (DiffDocument var4 : List.of(var1.b, var1.c)) {
         if (var4.d.get("components") instanceof Map var5) {
            var5.forEach(
               (var1x, var2x) -> {
                  String var3 = String.valueOf(var1x);
                  if (!var3.equals("securitySchemes") && !var3.startsWith("x-") && var2x instanceof Map var4x) {
                     var4x.keySet()
                        .forEach(
                           var2xx -> var2.add(
                              "/components/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var3) + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(String.valueOf(var2xx))
                           )
                        );
                  }
               }
            );
         }
      }

      for (String var9 : (Iterable<String>) (Iterable<?>) (var2)) {
         Object var10 = var1.b.a(var9);
         Object var11 = var1.c.a(var9);
         if ((var10 == null || var11 == null || !io.karatelabs.ext.common.CanonicalJson.a(var10).equals(io.karatelabs.ext.common.CanonicalJson.a(var11))) && !this.b(var9)) {
            io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var7 = var1.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPONENT_CHANGED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, null).a("component", var9).b(var9).c("unreferenced");
            if (var10 != null) {
               var7.b(var9, var9);
            }

            if (var11 != null) {
               var7.c(var9, var9);
            }

            var7.a();
         }
      }
   }

   private boolean b(String var1) {
      if (this.g.contains(var1)) {
         return true;
      } else {
         for (Map var3 : this.d.values()) {
            for (int var4 = var1.lastIndexOf(47); var4 > 0; var4 = var1.lastIndexOf(47, var4 - 1)) {
               if (var3.containsKey(var1.substring(0, var4))) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   void a(io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var1, String var2) {
      String var3 = var1.l != null ? var1.l : var1.m;
      if (var3 != null && var3.startsWith("/components/")) {
         io.karatelabs.ext.openapi.changes.ReferenceGraph.ComponentReferenceImpact var4 = this.h.computeIfAbsent(var3, var0 -> new io.karatelabs.ext.openapi.changes.ReferenceGraph.ComponentReferenceImpact(var0, new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>()));
         var4.b().add(var2);
         var4.c().addAll(this.a("baseline", var1.l));
         var4.d().addAll(this.a("current", var1.m));
      }
   }

   private Set<String> a(String var1, String var2) {
      HashSet<java.lang.String> var3 = new HashSet();
      if (var2 == null) {
         return var3;
      } else {
         var3.addAll(this.e.get(var1).getOrDefault(var2, Set.of()));
         var var4 = this.d.get(var1);

         for (int var5 = var2.length(); var5 > 0; var5 = var2.lastIndexOf(47, var5 - 1)) {
            var3.addAll(var4.getOrDefault(var2.substring(0, var5), Set.of()));
         }

         return var3;
      }
   }

   List<Map<String, Object>> a(CoverageJoinIndex var1) {
      if (this.h.isEmpty()) {
         return null;
      } else {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

         for (io.karatelabs.ext.openapi.changes.ReferenceGraph.ComponentReferenceImpact var4 : this.h.values()) {
            ArrayList<java.util.LinkedHashMap> var5 = new ArrayList();
            LinkedHashMap var6 = new LinkedHashMap();
            LinkedHashMap var7 = new LinkedHashMap();

            for (io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var9 : this.f.values()) {
               boolean var10 = var4.c().contains(var9.a());
               boolean var11 = var4.d().contains(var9.a());
               if (var10 || var11) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
                  var12.put("key", var9.a());
                  var12.put("side", var10 && var11 ? "both" : (var10 ? "baseline" : "current"));
                  var5.add(var12);
                  var var13 = var1.a((Map<?, ?>)var9.b().get("graphIds"));
                  a(var6, var13.get("requirements"));
                  a(var7, var13.get("tests"));
               }
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
            var14.put("authored", var4.a());
            var14.put("findings", new ArrayList<>(var4.b()));
            var14.put("operations", var5);
            var14.put("requirements", new ArrayList(var6.values()));
            var14.put("tests", new ArrayList(var7.values()));
            var2.add(var14);
         }

         return var2;
      }
   }

   private static void a(Map<String, Map<String, Object>> var0, Object var1) {
      if (var1 instanceof List) {
         for (Object var4 : (List)var1) {
            Map<java.lang.String, Object> var5 = (Map)var4;
            String var6 = String.valueOf(var5.get("id"));
            Map<java.lang.String, java.lang.String> var7 = (Map)var0.get(var6);
            if (var7 == null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
               var5.forEach((var1x, var2) -> var8.put(String.valueOf(var1x), var2));
               var0.put(var6, var8);
            } else if (!Objects.equals(var7.get("side"), var5.get("side"))) {
               var7.put("side", "both");
            }
         }
      }
   }

   private record ComponentReferenceImpact(String a, Set<String> b, Set<String> c, Set<String> d) {
   }
}
