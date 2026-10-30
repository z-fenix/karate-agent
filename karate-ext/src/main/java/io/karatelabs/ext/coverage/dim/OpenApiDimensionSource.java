package io.karatelabs.ext.coverage.dim;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.ext.coverage.graph.TraceGraph;
import io.karatelabs.ext.openapi.OpenApiSchemaNode;
import io.karatelabs.ext.openapi.OpenApiDocument;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenApiDimensionSource {
   private static final Logger a = LoggerFactory.getLogger(OpenApiDimensionSource.class);
   private static final Set<String> b = Set.of("enum", "boolean", "number", "cardinality", "string", "date");
   private static final Set<String> c = Set.of("format-invalid", "pattern-invalid");
   private static final String d = "absent";
   private static final String e = "null";
   private final Map<String, Map<String, Object>> f = new LinkedHashMap<>();
   private final Map<String, Map<String, Object>> g = new LinkedHashMap<>();
   private static final Object h = new Object();
   private static final Object i = new Object();

   private OpenApiDimensionSource() {
   }

   public static OpenApiDimensionSource a(List<Resource> var0) {
      OpenApiDimensionSource var1 = new OpenApiDimensionSource();

      for (Resource var3 : var0) {
         try {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog var4 = io.karatelabs.ext.openapi.OpenApiOperationCatalog.fromDocument(new OpenApiDocument(var3));

            for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var6 : var4.operations()) {
               OpenApiSchemaNode var7 = var6.f();
               if (var7 != null) {
                  var var8 = a(var7);
                  if (!var8.isEmpty()) {
                     if (var6.a() != null) {
                        var1.f.put(var6.a(), var8);
                     }

                     var1.g.put(a(var6.b(), var6.c()), var8);
                  }
               }
            }
         } catch (RuntimeException var9) {
            a.warn("spec dimensions: could not load '{}' — skipping: {}", var3, var9.toString());
         }
      }

      return var1;
   }

   public boolean a() {
      return this.f.isEmpty() && this.g.isEmpty();
   }

   private static Map<String, Object> a(OpenApiSchemaNode var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      Map var2;
      try {
         var2 = var0.partitions(null);
      } catch (RuntimeException var13) {
         return var1;
      }

      for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         String var5 = (String)var4.getKey();
         if (!var5.contains("[]") && var4.getValue() instanceof Map var6) {
            String var14 = String.valueOf(var6.get("kind"));
            if (b.contains(var14) && var6.get("classes") instanceof List var8 && !var8.isEmpty()) {
               ArrayList<java.lang.Object> var15 = new ArrayList();

               for (Object var11 : var8) {
                  if (!c.contains(String.valueOf(var11))) {
                     var15.add(var11);
                  }
               }

               if (!var15.isEmpty()) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
                  var16.put("universe", var15);
                  var16.put("type", var14);
                  var16.put("source", "spec");
                  if (("number".equals(var14) || "cardinality".equals(var14)) && var6.get("boundaries") instanceof List var17) {
                     var16.put("boundaries", new ArrayList(var17));
                  }

                  if ("string".equals(var14)) {
                     a(var6, var16, "minLength");
                     a(var6, var16, "maxLength");
                  }

                  var1.put(var5, var16);
               }
            }
         }
      }

      return var1;
   }

   public Map<String, Map<String, Object>> a(TraceGraph var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();
      if (this.a()) {
         return var2;
      } else {
         for (Entry var4 : var1.i().entrySet()) {
            var var5 = this.a((CoverageItem)var4.getValue());
            if (var5 != null && !var5.isEmpty()) {
               var2.put((String)var4.getKey(), c(var5));
            }
         }

         return var2;
      }
   }

   public void b(TraceGraph var1) {
      if (!this.a()) {
         var1.a(var2 -> {
            if (var2.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var2.d() instanceof HttpAction var3 && var3.k() != null) {
               CoverageItem var16 = var1.i().get(var2.a());
               if (var16 == null) {
                  return var2;
               } else {
                  var var5 = this.a(var16);
                  if (var5 == null || var5.isEmpty()) {
                     return var2;
                  } else if (a(var3) instanceof Map var7) {
                     var var8 = a(var2.h());
                     LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();

                     for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
                        String var12 = (String)var11.getKey();
                        if (!var8.contains(var12)) {
                           Map<java.lang.String, Object> var13 = var11.getValue() instanceof Map var14 ? b(var14) : Map.of();
                           Object var17 = a(var7, var12);
                           if (var17 == h) {
                              if (b(var13, "absent")) {
                                 var9.put(var12, "absent");
                              }
                           } else if (var17 == i) {
                              if (b(var13, "null")) {
                                 var9.put(var12, "null");
                              }
                           } else if ("date".equals(var13.get("type"))) {
                              String var18 = a(String.valueOf(var17), var3.timestamp());
                              if (var18 != null) {
                                 var9.put(var12, var18);
                              }
                           } else {
                              var9.put(var12, var17);
                           }
                        }
                     }

                     return var9.isEmpty() ? var2 : var2.b(var9);
                  } else {
                     return var2;
                  }
               }
            } else {
               return var2;
            }
         });
      }
   }

   public static Map<String, Map<String, Object>> a(Map<String, Map<String, Object>> var0, Map<String, Map<String, Object>> var1) {
      if (var0.isEmpty()) {
         return var1;
      } else if (var1.isEmpty()) {
         return var0;
      } else {
         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();
         var var3 = new LinkedHashSet<>(var0.keySet());
         var3.addAll(var1.keySet());

         for (String var5 : (Iterable<String>) (Iterable<?>) (var3)) {
            var var6 = new LinkedHashMap<>(var0.getOrDefault(var5, Map.of()));
            var6.putAll(var1.getOrDefault(var5, Map.of()));
            var2.put(var5, var6);
         }

         return var2;
      }
   }

   private Map<String, Object> a(CoverageItem var1) {
      if (var1.getMethod() == null) {
         return null;
      } else {
         Map var2 = var1.id() == null ? null : this.f.get(var1.id());
         return var2 != null ? var2 : this.g.get(a(var1.getMethod(), var1.getPath()));
      }
   }

   private static Object a(HttpAction var0) {
      return var0.k().get("request") instanceof Map var2 ? var2.get("body") : null;
   }

   private static Set<?> a(Map<String, Object> var0) {
      return var0 != null && var0.get("dim") instanceof Map var1 ? var1.keySet() : Set.of();
   }

   private static Object a(Map<?, ?> var0, String var1) {
      Object var2 = var0;
      String[] var3 = var1.split("\\.");

      for (int var4 = 0; var4 < var3.length; var4++) {
         if (!(var2 instanceof Map var5) || !var5.containsKey(var3[var4])) {
            return h;
         }

         var2 = var5.get(var3[var4]);
         if (var2 == null) {
            return var4 == var3.length - 1 ? i : h;
         }
      }

      return var2;
   }

   private static boolean b(Map<String, Object> var0, String var1) {
      return var0.get("universe") instanceof List var2 && var2.contains(var1);
   }

   private static Map<String, Object> b(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         var1.put((String)var3.getKey(), var3.getValue() instanceof Map var4 ? new LinkedHashMap(var4) : var3.getValue());
      }

      return var1;
   }

   static String a(String var0, Instant var1) {
      if (var0 != null && var0.length() >= 10 && var1 != null) {
         LocalDate var2;
         try {
            var2 = LocalDate.parse(var0.substring(0, 10));
         } catch (DateTimeParseException var4) {
            return null;
         }

         LocalDate var3 = var1.atZone(ZoneOffset.UTC).toLocalDate();
         if (var2.getMonthValue() == 2 && var2.getDayOfMonth() == 29) {
            return "leap-day";
         } else if (var2.getDayOfMonth() == 1 || var2.getDayOfMonth() == var2.lengthOfMonth()) {
            return "period-boundary";
         } else if (var2.isAfter(var3.plusYears(50L))) {
            return "far-future";
         } else if (var2.isBefore(var3)) {
            return "past";
         } else {
            return var2.isEqual(var3) ? "today" : "future";
         }
      } else {
         return null;
      }
   }

   private static void a(Map<?, ?> var0, Map<String, Object> var1, String var2) {
      if (var0.get(var2) != null) {
         var1.put(var2, var0.get(var2));
      }
   }

   private static String a(String var0, String var1) {
      return (var0 == null ? "" : var0.toUpperCase()) + " " + var1;
   }
}
