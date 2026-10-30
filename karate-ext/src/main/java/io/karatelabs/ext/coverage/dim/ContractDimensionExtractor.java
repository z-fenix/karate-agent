package io.karatelabs.ext.coverage.dim;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.TypeModel;
import io.karatelabs.ext.coverage.graph.TraceGraph;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ContractDimensionExtractor {
   private static final Set<String> a = Set.of("enum", "boolean", "number", "cardinality", "string", "date");
   private static final Set<String> b = Set.of("format-invalid", "pattern-invalid");

   private ContractDimensionExtractor() {
   }

   public static Map<String, Map<String, Object>> a(List<io.karatelabs.ext.coverage.CoverageSource> var0, TraceGraph var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();

      for (io.karatelabs.ext.coverage.CoverageSource var4 : var0) {
         if (var4 instanceof io.karatelabs.ext.coverage.collect.ContractCoverageSource var5 && !"openapi".equals(var5.b())) {
            String var6 = var5.f();
            Contract var7 = var5.h();

            for (Operation var9 : var7.operations()) {
               var var10 = a(var9);
               if (!var10.isEmpty()) {
                  String var11 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var6, var9.id());
                  if (var1.i().containsKey(var11)) {
                     var2.put(var11, var10);
                  }
               }
            }
         }
      }

      return var2;
   }

   public static void b(List<io.karatelabs.ext.coverage.CoverageSource> var0, TraceGraph var1) {
      var var2 = a(var0, var1);
      if (!var2.isEmpty()) {
         var1.a(var1x -> {
            if (var1x.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var1x.d() != null) {
               Map var2x = (Map)var2.get(var1x.a());
               if (var2x != null && !var2x.isEmpty()) {
                  if (var1x.d().dimensionInput() instanceof Map var4) {
                     var var5 = a(var1x.h());
                     LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

                     for (String var8 : (Iterable<String>) (Iterable<?>) (var2x.keySet())) {
                        if (!var5.contains(var8)) {
                           Object var9 = a(var4, var8);
                           if (var9 != null) {
                              var6.put(var8, var9);
                           }
                        }
                     }

                     return var6.isEmpty() ? var1x : var1x.b(var6);
                  } else {
                     return var1x;
                  }
               } else {
                  return var1x;
               }
            } else {
               return var1x;
            }
         });
      }
   }

   private static Map<String, Object> a(Operation var0) {
      TypeModel var1 = b(var0);
      if (var1 == null) {
         return Map.of();
      } else {
         Map var2;
         try {
            var2 = var1.partitions(null);
         } catch (RuntimeException var14) {
            return Map.of();
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var6 = (String)var5.getKey();
            if (!var6.contains("[]") && var5.getValue() instanceof Map var7) {
               String var15 = String.valueOf(var7.get("kind"));
               if (a.contains(var15) && var7.get("classes") instanceof List var9 && !var9.isEmpty()) {
                  ArrayList<java.lang.Object> var16 = new ArrayList();

                  for (Object var12 : var9) {
                     if (!b.contains(String.valueOf(var12))) {
                        var16.add(var12);
                     }
                  }

                  if (!var16.isEmpty()) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
                     var17.put("universe", var16);
                     var17.put("type", var15);
                     var17.put("source", var7.get("source"));
                     if (("number".equals(var15) || "cardinality".equals(var15)) && var7.get("boundaries") instanceof List var18) {
                        var17.put("boundaries", new ArrayList(var18));
                     }

                     if ("string".equals(var15)) {
                        a(var7, var17, "minLength");
                        a(var7, var17, "maxLength");
                     }

                     var3.put(var6, var17);
                  }
               }
            }
         }

         return var3;
      }
   }

   private static TypeModel b(Operation var0) {
      for (Message var2 : var0.inputs()) {
         if ((var2.role() == Message.Role.BODY || var2.role() == Message.Role.VALUE) && var2.type() != null) {
            return var2.type().model();
         }
      }

      return null;
   }

   private static Object a(Map<?, ?> var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         if (!(var2 instanceof Map var7)) {
            return null;
         }

         var2 = b(var7, var6);
         if (var2 == null) {
            return null;
         }
      }

      return var2;
   }

   private static Object b(Map<?, ?> var0, String var1) {
      if (var0.containsKey(var1)) {
         return var0.get(var1);
      } else {
         String var2 = a(var1);

         for (Entry var4 : var0.entrySet()) {
            if (var4.getKey() instanceof String var5 && a(var5).equals(var2)) {
               return var4.getValue();
            }
         }

         return null;
      }
   }

   private static String a(String var0) {
      return var0.replace("_", "").toLowerCase();
   }

   private static Set<?> a(Map<String, Object> var0) {
      return var0 != null && var0.get("dim") instanceof Map var1 ? var1.keySet() : Set.of();
   }

   private static void a(Map<?, ?> var0, Map<String, Object> var1, String var2) {
      if (var0.get(var2) != null) {
         var1.put(var2, var0.get(var2));
      }
   }
}
