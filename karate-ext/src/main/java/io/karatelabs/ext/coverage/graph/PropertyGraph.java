package io.karatelabs.ext.coverage.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class PropertyGraph {
   private final LinkedHashMap<String, PropertyNode> a = new LinkedHashMap<>();
   private final List<io.karatelabs.ext.coverage.graph.PropertyEdge> b = new ArrayList<>();

   public void a(PropertyNode var1) {
      this.a.put(var1.a(), var1);
   }

   public void a(io.karatelabs.ext.coverage.graph.PropertyEdge var1) {
      this.b.add(var1);
   }

   public PropertyNode a(String var1) {
      return this.a.get(var1);
   }

   public Collection<PropertyNode> a() {
      return this.a.values();
   }

   public List<String> b() {
      return new ArrayList<>(this.a.keySet());
   }

   public List<io.karatelabs.ext.coverage.graph.PropertyEdge> c() {
      return List.copyOf(this.b);
   }

   public List<io.karatelabs.ext.coverage.graph.PropertyEdge> a(String var1, String var2, Set<String> var3) {
      ArrayList<io.karatelabs.ext.coverage.graph.PropertyEdge> var4 = new ArrayList();

      for (io.karatelabs.ext.coverage.graph.PropertyEdge var6 : this.b) {
         if ((var1 == null || var1.equals(var6.a())) && (var2 == null || var2.equals(var6.b())) && (var3 == null || var3.contains(var6.c()))) {
            var4.add(var6);
         }
      }

      return var4;
   }

   public List<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor> a(String var1, Set<String> var2, io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var3) {
      ArrayList<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor> var4 = new ArrayList();

      for (io.karatelabs.ext.coverage.graph.PropertyEdge var6 : this.b) {
         if (var2 == null || var2.contains(var6.c())) {
            if ((var3 == io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.OUT || var3 == io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.BOTH) && var6.a().equals(var1)) {
               var4.add(new io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor(var6.b(), var6.c(), io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.OUT, var6));
            }

            if ((var3 == io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.IN || var3 == io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.BOTH) && var6.b().equals(var1)) {
               var4.add(new io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor(var6.a(), var6.c(), io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.IN, var6));
            }
         }
      }

      return var4;
   }

   public List<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth> a(String var1, Set<String> var2, io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var3, int var4) {
      ArrayList<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth> var5 = new ArrayList();
      if (var1 == null) {
         return var5;
      } else {
         HashSet<java.lang.String> var6 = new HashSet();
         var6.add(var1);
         ArrayDeque<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth> var7 = new ArrayDeque();
         var7.add(new io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth(var1, 0));

         while (!var7.isEmpty()) {
            io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth var8 = (io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth)var7.poll();
            if (var4 <= 0 || var8.b() < var4) {
               for (io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor var10 : this.a(var8.a(), var2, var3)) {
                  if (var6.add(var10.a())) {
                     io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth var11 = new io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth(var10.a(), var8.b() + 1);
                     var5.add(var11);
                     var7.add(var11);
                  }
               }
            }
         }

         return var5;
      }
   }

   public List<String> a(String var1, String var2, Set<String> var3, io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var4) {
      if (var1 != null && var2 != null) {
         if (var1.equals(var2)) {
            return List.of(var1);
         } else {
            HashMap<java.lang.String, java.lang.String> var5 = new HashMap();
            HashSet<java.lang.String> var6 = new HashSet();
            var6.add(var1);
            ArrayDeque<java.lang.String> var7 = new ArrayDeque();
            var7.add(var1);

            while (!var7.isEmpty()) {
               String var8 = (String)var7.poll();

               for (io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor var10 : this.a(var8, var3, var4)) {
                  if (var6.add(var10.a())) {
                     var5.put(var10.a(), var8);
                     if (var10.a().equals(var2)) {
                        return a(var5, var1, var2);
                     }

                     var7.add(var10.a());
                  }
               }
            }

            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private static List<String> a(Map<String, String> var0, String var1, String var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();
      String var4 = var2;

      while (var4 != null) {
         var3.add(var4);
         if (var4.equals(var1)) {
            break;
         }

         var4 = (String)var0.get(var4);
      }

      ArrayList<java.lang.String> var6 = new ArrayList(var3.size());

      for (int var5 = var3.size() - 1; var5 >= 0; var5--) {
         var6.add((String)var3.get(var5));
      }

      return var6;
   }

   public static PropertyGraph a(Map<String, Object> var0) {
      PropertyGraph var1 = new PropertyGraph();

      for (Map var3 : a(var0, "items")) {
         String var4 = String.valueOf(var3.get("id"));
         String var5 = var3.get("source") == null ? null : String.valueOf(var3.get("source"));
         LinkedHashMap<java.lang.String, Object> var6 = new LinkedHashMap(var3);
         var6.remove("id");
         var6.remove("source");
         var1.a(new PropertyNode(var4, "item", var5, var6));
      }

      for (Map var16 : a(var0, "tests")) {
         String var19 = String.valueOf(var16.get("id"));
         LinkedHashMap<java.lang.String, Object> var22 = new LinkedHashMap(var16);
         var22.remove("id");
         var1.a(new PropertyNode(var19, "test", "test", var22));
      }

      for (Map var17 : a(var0, "hits")) {
         String var20 = String.valueOf(var17.get("item"));
         String var23 = String.valueOf(var17.get("test"));
         LinkedHashMap<java.lang.String, java.lang.Object> var25 = new LinkedHashMap();
         Object var7 = var17.get("kind");
         var25.put("mode", var7 == null ? "observed" : String.valueOf(var7).toLowerCase(Locale.ROOT));

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var17.entrySet())) {
            if (!"item".equals(var9.getKey()) && !"test".equals(var9.getKey()) && !"kind".equals(var9.getKey())) {
               if ("attrs".equals(var9.getKey()) && var9.getValue() instanceof Map var10) {
                  for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
                     var25.put(String.valueOf(var12.getKey()), var12.getValue());
                  }
               } else {
                  var25.put((String)var9.getKey(), var9.getValue());
               }
            }
         }

         var1.a(new io.karatelabs.ext.coverage.graph.PropertyEdge(var23, var20, "hit", var25));
      }

      for (Map var18 : a(var0, "items")) {
         String var21 = String.valueOf(var18.get("id"));
         String var24 = var18.get("source") == null ? null : String.valueOf(var18.get("source"));
         Object var26 = var18.get("parent");
         if (var26 != null) {
            var1.a(io.karatelabs.ext.coverage.graph.PropertyEdge.a(var21, a(var24, var26), "parent"));
         }

         Object var30 = var18.get("ref");
         if (var30 instanceof List) {
            for (Object var36 : (List)var30) {
               if (var36 != null) {
                  var1.a(io.karatelabs.ext.coverage.graph.PropertyEdge.a(var21, a(var24, var36), "ref"));
               }
            }
         }

         var30 = var18.get("decomposes");
         if (var30 instanceof List) {
            for (Object var37 : (List)var30) {
               if (var37 != null) {
                  var1.a(io.karatelabs.ext.coverage.graph.PropertyEdge.a(var21, a(var24, var37), "decomposes"));
               }
            }
         }

         var30 = var18.get("real");
         if (var30 instanceof List) {
            for (Object var38 : (List)var30) {
               if (var38 != null) {
                  var1.a(io.karatelabs.ext.coverage.graph.PropertyEdge.a(var21, String.valueOf(var38), "real"));
               }
            }
         }
      }

      return var1;
   }

   static String a(String var0, Object var1) {
      String var2 = String.valueOf(var1);
      return var2.indexOf(58) <= 0 && var0 != null ? var0 + ":" + var2 : var2;
   }

   private static List<Map<String, Object>> a(Map<String, Object> var0, String var1) {
      return var0 != null && var0.get(var1) instanceof List var2 ? var2 : List.of();
   }

   public record PropertyNeighbor(String a, String b, io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection c, io.karatelabs.ext.coverage.graph.PropertyEdge d) {

   }

   public static enum TraversalDirection {
      OUT,
      IN,
      BOTH;

      public static io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection parse(String var0) {
         if (var0 == null) {
            return BOTH;
         } else {
            String var1 = var0.toLowerCase(Locale.ROOT);

            return switch (var1) {
               case "out" -> OUT;
               case "in" -> IN;
               default -> BOTH;
            };
         }
      }
   }

   public record PropertyDepth(String a, int b) {

   }
}
