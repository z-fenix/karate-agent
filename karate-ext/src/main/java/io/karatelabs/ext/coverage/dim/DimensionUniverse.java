package io.karatelabs.ext.coverage.dim;

import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.graph.TraceGraph;
import io.karatelabs.ext.openapi.PathTemplateMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DimensionUniverse {
   private static final Logger a = LoggerFactory.getLogger(DimensionUniverse.class);

   private DimensionUniverse() {
   }

   public static Map<String, Object> a(DimensionOracle var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      Map var3;
      try {
         var3 = var0.ranges(var1, null);
      } catch (RuntimeException var19) {
         a.warn("dimension universe: Rule.ranges('{}') failed — skipping: {}", var1, var19.toString());
         return var2;
      }

      if (var3 != null && var3.get("error") == null && var3.get("dimensions") instanceof List var4) {
         String var20 = "rule:" + var1;

         for (Object var7 : var4) {
            if (var7 instanceof Map var8) {
               String var9 = String.valueOf(var8.get("type"));
               if ("enum".equals(var9) || "boolean".equals(var9) || "number".equals(var9)) {
                  if ("enum".equals(var9) && "freetext".equals(String.valueOf(var8.get("cause")))) {
                     a.debug("dimension universe: '{}' axis '{}' is free text sampled by g.enum — not a closed universe, skipping", var1, var8.get("path"));
                  } else {
                     String var10 = String.valueOf(var8.get("path"));
                     if (var8.get("classes") instanceof List var11) {
                        boolean var21 = "number".equals(var9);
                        ArrayList var13 = new ArrayList();
                        ArrayList var14 = new ArrayList();
                        LinkedHashMap var15 = new LinkedHashMap();

                        for (Object var17 : var11) {
                           if (var17 instanceof Map var18) {
                              if (var21) {
                                 var13.add(var18.get("label"));
                                 if (var18.get("label") != null && var18.get("sample") != null) {
                                    var15.put(String.valueOf(var18.get("label")), var18.get("sample"));
                                 }

                                 if (Boolean.TRUE.equals(var18.get("boundary")) && var18.get("sample") != null) {
                                    var14.add(var18.get("sample"));
                                 }
                              } else if (var18.containsKey("sample")) {
                                 var13.add(var18.get("sample"));
                              }
                           }
                        }

                        if (!var13.isEmpty() && (!var21 || !var14.isEmpty())) {
                           LinkedHashMap var22 = new LinkedHashMap();
                           var22.put("universe", var13);
                           var22.put("type", var9);
                           var22.put("source", var20);
                           if (var21) {
                              var22.put("boundaries", var14);
                              if (!var15.isEmpty()) {
                                 var22.put("examples", var15);
                              }
                           }

                           var2.put(var10, var22);
                        }
                     }
                  }
               }
            }
         }

         return var2;
      } else {
         a.warn("dimension universe: no dims for rulebook '{}' — skipping", var1);
         return var2;
      }
   }

   public static Map<String, List<String>> a(TraceGraph var0, DimensionHandlers var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var var3 = var1.c();
      if (var3.isEmpty()) {
         return var2;
      } else {
         var var4 = new ArrayList<>(var3.keySet());

         for (Entry var6 : var0.i().entrySet()) {
            String var7 = ((CoverageItem)var6.getValue()).getPath();
            if (var7 != null) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var8 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var7, var4);
               if (var8 != null) {
                  var2.put((String)var6.getKey(), new ArrayList((Collection)var3.get(var8.a())));
               }
            }
         }

         return var2;
      }
   }

   public static Map<String, List<String>> b(TraceGraph var0, DimensionHandlers var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var var3 = var1.e();
      if (var3.isEmpty()) {
         return var2;
      } else {
         var var4 = new ArrayList<>(var3.keySet());

         for (Entry var6 : var0.i().entrySet()) {
            String var7 = (String)var6.getKey();
            String var8 = ((CoverageItem)var6.getValue()).getPath();
            List var9 = null;
            if (var8 != null) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var10 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var8, var4);
               if (var10 != null) {
                  var9 = (List)var3.get(var10.a());
               }
            }

            if (var9 == null) {
               String var13 = var7.contains(":") ? var7.substring(var7.indexOf(58) + 1) : var7;

               for (String var12 : (Iterable<String>) (Iterable<?>) (var4)) {
                  if (var12.equals(var7) || var12.equals(var13)) {
                     var9 = (List)var3.get(var12);
                     break;
                  }
               }
            }

            if (var9 != null) {
               var2.put(var7, new ArrayList(var9));
            }
         }

         return var2;
      }
   }

   public static Map<String, String> c(TraceGraph var0, DimensionHandlers var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var var3 = var1.d();
      if (var3.isEmpty()) {
         return var2;
      } else {
         var var4 = new ArrayList<>(var3.keySet());

         for (Entry var6 : var0.i().entrySet()) {
            String var7 = ((CoverageItem)var6.getValue()).getPath();
            if (var7 != null) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var8 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var7, var4);
               if (var8 != null) {
                  var2.put((String)var6.getKey(), (String)var3.get(var8.a()));
               }
            }
         }

         return var2;
      }
   }

   public static Map<String, List<Map<String, Object>>> a(TraceGraph var0, DimensionHandlers var1, DimensionOracle var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var var4 = var1.c();
      var var5 = var1.b();
      if (!var4.isEmpty() && !var5.isEmpty() && var2 != null) {
         var var6 = new ArrayList<>(var4.keySet());
         var var7 = new ArrayList<>(var5.keySet());
         LinkedHashMap<String, Map<String, Object>> var8 = new LinkedHashMap<>();

         for (Entry var10 : var0.i().entrySet()) {
            String var11 = ((CoverageItem)var10.getValue()).getPath();
            if (var11 != null) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var12 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var11, var6);
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var13 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var11, var7);
               if (var12 != null && var13 != null) {
                  List var14 = (List)var4.get(var12.a());
                  String var15 = (String)var5.get(var13.a());
                  Map var16 = var8.computeIfAbsent(var15, var1x -> a(var2, var1x));
                  ArrayList var17 = new ArrayList();
                  boolean var18 = true;

                  for (String var20 : (Iterable<String>) (Iterable<?>) (var14)) {
                     if (!(var16.get(var20) instanceof Map var21)
                        || "number".equals(var21.get("type"))
                        || !(var21.get("universe") instanceof List var22)
                        || var22.isEmpty()) {
                        var18 = false;
                        break;
                     }

                     var17.add(new ArrayList(var22));
                  }

                  if (var18) {
                     ArrayList var24 = new ArrayList();

                     for (Map var26 : (Iterable<Map>) (Iterable<?>) (a(var14, var17))) {
                        if (!var2.feasible(var15, var26)) {
                           var24.add(var26);
                        }
                     }

                     if (!var24.isEmpty()) {
                        var3.put((String)var10.getKey(), var24);
                     }
                  }
               }
            }
         }

         return var3;
      } else {
         return var3;
      }
   }

   private static List<Map<String, Object>> a(List<String> var0, List<List<Object>> var1) {
      ArrayList var2 = new ArrayList();
      var2.add(new LinkedHashMap());

      for (int var3 = 0; var3 < var0.size(); var3++) {
         String var4 = (String)var0.get(var3);
         ArrayList var5 = new ArrayList();

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (var2)) {
            for (Object var9 : (List)var1.get(var3)) {
               LinkedHashMap var10 = new LinkedHashMap(var7);
               var10.put(var4, var9);
               var5.add(var10);
            }
         }

         var2 = var5;
      }

      return var2;
   }

   public static Map<String, Map<String, Object>> b(TraceGraph var0, DimensionHandlers var1, DimensionOracle var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var var4 = var1.b();
      if (!var4.isEmpty() && var2 != null) {
         var var5 = new ArrayList<>(var4.keySet());
         LinkedHashMap<String, Map<String, Object>> var6 = new LinkedHashMap<>();

         for (Entry var8 : var0.i().entrySet()) {
            String var9 = ((CoverageItem)var8.getValue()).getPath();
            if (var9 != null) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var10 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var9, var5);
               if (var10 != null) {
                  String var11 = (String)var4.get(var10.a());
                  Map var12 = var6.computeIfAbsent(var11, var1x -> a(var2, var1x));
                  if (!var12.isEmpty()) {
                     var3.put((String)var8.getKey(), new LinkedHashMap(var12));
                  }
               }
            }
         }

         return var3;
      } else {
         return var3;
      }
   }
}
