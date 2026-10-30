package io.karatelabs.ext.openapi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class WitnessedPathGraphRenderer {
   private static final int a = 200;
   private static final int b = 40;
   private static final int c = 40;
   private static final int d = 64;
   private static final int e = 40;
   private static final int f = 18;
   private static final int g = 22;
   private static final int h = 26;
   private static final int i = 12;
   private static final int j = 28;
   private static final double k = 6.3;
   private final StringBuilder l = new StringBuilder();
   private int m;

   private WitnessedPathGraphRenderer() {
   }

   public static String a(List<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode> var0, List<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge> var1) {
      return new WitnessedPathGraphRenderer().b(var0, var1);
   }

   private String b(List<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode> var1, List<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge> var2) {
      LinkedHashMap<String, io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode> var3 = new LinkedHashMap<>();

      for (io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode var5 : var1) {
         if (var3.putIfAbsent(var5.a(), var5) != null) {
            throw new IllegalArgumentException("duplicate node: " + var5.a());
         }
      }

      var var27 = new ArrayList<>(var2);
      var27.sort(Comparator.comparing(io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge::a).thenComparing(io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge::b));

      for (int var28 = 0; var28 < var27.size(); var28++) {
         io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge var6 = (io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge)var27.get(var28);
         if (var28 > 0 && var6.a().equals(((io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge)var27.get(var28 - 1)).a()) && var6.b().equals(((io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge)var27.get(var28 - 1)).b())) {
            throw new IllegalArgumentException("duplicate edge: " + var6.a() + " -> " + var6.b());
         }

         var3.putIfAbsent(var6.a(), new io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode(var6.a(), var6.a()));
         var3.putIfAbsent(var6.b(), new io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode(var6.b(), var6.b()));
      }

      if (var3.isEmpty()) {
         this.l
            .append(
               "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"kd-svg\" viewBox=\"0 0 240 60\" width=\"240\" height=\"60\">\n<text class=\"kd-text kd-text-quiet\" x=\"120\" y=\"34\" text-anchor=\"middle\">no witnessed paths</text>\n</svg>\n"
            );
         return this.l.toString();
      } else {
         Map<String, Integer> var29 = a(var3, var27);
         int var30 = var29.values().stream().max(Integer::compare).orElse(0) + 1;
         ArrayList<java.util.ArrayList> var7 = new ArrayList();

         for (int var8 = 0; var8 < var30; var8++) {
            var7.add(new ArrayList());
         }

         var3.keySet().forEach(var2x -> ((List)var7.get((Integer)var29.get(var2x))).add(var2x));
         int var31 = 0;

         for (List var10 : (Iterable<List>) (Iterable<?>) (var7)) {
            var31 = Math.max(var31, var10.size() * 240 - 40);
         }

         LinkedHashMap<java.lang.String, int[]> var32 = new LinkedHashMap();

         for (int var33 = 0; var33 < var30; var33++) {
            List var11 = (List)var7.get(var33);
            int var12 = 40 + (var31 - (var11.size() * 240 - 40)) / 2;

            for (String var14 : (Iterable<String>) (Iterable<?>) (var11)) {
               var32.put(var14, new int[]{var12, 40 + var33 * 104});
               var12 += 240;
            }
         }

         this.m = 40 + var31;

         for (Entry var36 : (Iterable<Entry>) (Iterable<?>) (var32.entrySet())) {
            this.a((io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode)var3.get(var36.getKey()), ((int[])var36.getValue())[0], ((int[])var36.getValue())[1]);
         }

         LinkedHashMap<java.lang.String, java.lang.Integer> var35 = new LinkedHashMap();
         LinkedHashMap<java.lang.String, java.lang.Integer> var37 = new LinkedHashMap();

         for (io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge var40 : (Iterable<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge>) (Iterable<?>) (var27)) {
            if ((Integer)var29.get(var40.b()) > (Integer)var29.get(var40.a())) {
               var35.merge(var40.a(), 1, Integer::sum);
               var37.merge(var40.b(), 1, Integer::sum);
            }
         }

         LinkedHashMap var39 = new LinkedHashMap();
         LinkedHashMap var41 = new LinkedHashMap();
         int var42 = 0;
         int var15 = 0;

         for (io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge var17 : (Iterable<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge>) (Iterable<?>) (var27)) {
            int[] var18 = (int[])var32.get(var17.a());
            int[] var19 = (int[])var32.get(var17.b());
            int var20 = (Integer)var29.get(var17.a());
            int var21 = (Integer)var29.get(var17.b());
            if (var17.a().equals(var17.b())) {
               int var22 = var18[0] + 200 - 30;
               int var23 = var18[1] + 12;
               int var24 = var18[0] + 200;
               this.a(
                  var17,
                  "M " + var22 + " " + var18[1] + " C " + var22 + " " + (var18[1] - 26) + " " + (var24 + 26) + " " + var23 + " " + var24 + " " + var23,
                  var24 + 8,
                  var18[1] - 6
               );
            } else if (var21 > var20) {
               int var45 = var18[0] + 100 + a(var39, var35, var17.a());
               int var48 = var19[0] + 100 + a(var41, var37, var17.b());
               int var51 = var18[1] + 40;
               int var25 = var19[1];
               int var26 = (var51 + var25) / 2;
               this.a(
                  var17,
                  "M " + var45 + " " + var51 + " C " + var45 + " " + var26 + " " + var48 + " " + var26 + " " + var48 + " " + var25,
                  (var45 + var48) / 2 + 6,
                  var26 - 4
               );
            } else if (var21 == var20) {
               int var46 = var18[0] + 100 - 20;
               int var49 = var19[0] + 100 + 20;
               int var52 = var18[1] + 40;
               int var54 = var52 + 22 + 6 * (var15++ % 4);
               this.a(
                  var17,
                  "M " + var46 + " " + var52 + " C " + var46 + " " + var54 + " " + var49 + " " + var54 + " " + var49 + " " + var52,
                  (var46 + var49) / 2,
                  var54
               );
            } else {
               int var47 = var18[1] + 20 + 6;
               int var50 = var19[1] + 20 - 6;
               int var53 = var18[0] + 200;
               int var55 = var19[0] + 200;
               int var56 = Math.max(var53, var55) + 36 + 18 * var42++;
               this.a(
                  var17,
                  "M " + var53 + " " + var47 + " C " + var56 + " " + var47 + " " + var56 + " " + var50 + " " + var55 + " " + var50,
                  Math.max(var53, var55) + (var56 - Math.max(var53, var55)) * 3 / 4 + 4,
                  (var47 + var50) / 2
               );
            }
         }

         int var43 = this.m + 40;
         int var44 = 80 + var30 * 104 - 64;
         return "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"kd-svg\" viewBox=\"0 0 "
            + var43
            + " "
            + var44
            + "\" width=\""
            + var43
            + "\" height=\""
            + var44
            + "\">\n<defs><marker id=\"kd-arr\" viewBox=\"0 0 8 8\" refX=\"7\" refY=\"4\" markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\"><path d=\"M 0 0 L 8 4 L 0 8 z\" class=\"kd-arrow\"/></marker></defs>\n"
            + this.l
            + "</svg>\n";
      }
   }

   private static Map<String, Integer> a(Map<String, io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode> var0, List<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge> var1) {
      LinkedHashMap<String, List<String>> var2 = new LinkedHashMap<>();
      LinkedHashMap<java.lang.String, java.lang.Integer> var3 = new LinkedHashMap();
      ArrayList<java.lang.String> var4 = new ArrayList();

      for (io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge var6 : var1) {
         if (!var6.a().equals(var6.b())) {
            var2.computeIfAbsent(var6.a(), var0x -> new ArrayList<>()).add(var6.b());
            var3.merge(var6.b(), 1, Integer::sum);
         }
      }

      for (String var14 : var0.keySet()) {
         boolean var7 = var1.stream().anyMatch(var1x -> var1x.a().equals(var14) || var1x.b().equals(var14));
         if (var7) {
            var4.add(var14);
         }
      }

      LinkedHashMap<String, Integer> var13 = new LinkedHashMap();
      ArrayDeque<java.lang.String> var15 = new ArrayDeque();

      for (String var8 : (Iterable<String>) (Iterable<?>) (var4)) {
         if (!var3.containsKey(var8)) {
            var13.put(var8, 0);
            var15.add(var8);
         }
      }

      int var17 = -1;

      while (true) {
         while (var15.isEmpty()) {
            String var18 = var4.stream().filter(var1x -> !var13.containsKey(var1x)).findFirst().orElse(null);
            if (var18 == null) {
               for (String var9 : var0.keySet()) {
                  var13.putIfAbsent(var9, var17 + 1);
               }

               return var13;
            }

            var13.put(var18, 0);
            var15.add(var18);
         }

         String var20 = (String)var15.poll();
         int var21 = (Integer)var13.get(var20);
         var17 = Math.max(var17, var21);

         for (String var11 : (Iterable<String>) (Iterable<?>) (var2.getOrDefault(var20, List.of()))) {
            if (var13.putIfAbsent(var11, var21 + 1) == null) {
               var15.add(var11);
            }
         }
      }
   }

   private static int a(Map<String, Integer> var0, Map<String, Integer> var1, String var2) {
      int var3 = (Integer)var1.get(var2);
      int var4 = var0.merge(var2, 1, Integer::sum) - 1;
      return Math.max(-66, Math.min(66, (2 * var4 - (var3 - 1)) * 12 / 2));
   }

   private void a(io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode var1, int var2, int var3) {
      int var4 = var2 + 100;
      this.l.append("<g id=\"kd-").append(a(var1.a())).append("\" class=\"kd-node\" data-op=\"").append(a(var1.a())).append("\">");
      this.l
         .append("<rect class=\"kd-shape\" x=\"")
         .append(var2)
         .append("\" y=\"")
         .append(var3)
         .append("\" width=\"")
         .append(200)
         .append("\" height=\"")
         .append(40)
         .append("\" rx=\"4\"/>");
      if (var1.b().equals(var1.a())) {
         this.a("kd-text", var4, var3 + 20 + 4, var1.b());
      } else {
         this.a("kd-text", var4, var3 + 17, var1.b());
         this.a("kd-text kd-text-quiet", var4, var3 + 32, var1.a());
      }

      this.l.append("<title>").append(a(var1.b().equals(var1.a()) ? var1.a() : var1.b() + "\n" + var1.a())).append("</title>");
      this.l.append("</g>\n");
   }

   private void a(io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge var1, String var2, int var3, int var4) {
      this.l
         .append("<path class=\"kd-edge\" marker-end=\"url(#kd-arr)\" data-from=\"")
         .append(a(var1.a()))
         .append("\" data-to=\"")
         .append(a(var1.b()))
         .append("\" data-sources=\"")
         .append(a(String.join(" ", var1.c())))
         .append("\" d=\"")
         .append(var2)
         .append("\">");
      this.l.append("<title>").append(a(var1.a() + " → " + var1.b() + "\n" + String.join("\n", var1.c()))).append("</title></path>\n");
      String var5 = String.join(", ", var1.c());
      if (!var5.isEmpty()) {
         if (var5.length() > 28) {
            var5 = var5.substring(0, 27) + "…";
         }

         this.l.append("<text class=\"kd-edge-label\" x=\"").append(var3).append("\" y=\"").append(var4).append("\">").append(a(var5)).append("</text>\n");
         this.m = Math.max(this.m, var3 + (int)(var5.length() * 6.3));
      }

      this.m = Math.max(this.m, var3);
   }

   private void a(String var1, int var2, int var3, String var4) {
      byte var5 = 29;
      String var6 = var4.length() > var5 ? var4.substring(0, var5 - 1) + "…" : var4;
      this.l
         .append("<text class=\"")
         .append(var1)
         .append("\" x=\"")
         .append(var2)
         .append("\" y=\"")
         .append(var3)
         .append("\" text-anchor=\"middle\">")
         .append(a(var6))
         .append("</text>");
   }

   private static String a(String var0) {
      return var0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
   }

   public record WitnessedPathEdge(String a, String b, List<String> c) {

   }

   public record WitnessedPathNode(String a, String b) {

   }
}
