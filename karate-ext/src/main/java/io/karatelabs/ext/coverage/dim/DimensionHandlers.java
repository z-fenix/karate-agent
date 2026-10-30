package io.karatelabs.ext.coverage.dim;

import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DimensionHandlers {
   private static final Logger a = LoggerFactory.getLogger(DimensionHandlers.class);
   private final Map<String, JavaCallable> b = new LinkedHashMap<>();
   private final Map<String, String> c = new LinkedHashMap<>();
   private final Map<String, List<String>> d = new LinkedHashMap<>();
   private final Map<String, String> e = new LinkedHashMap<>();
   private final Map<String, List<String>> f = new LinkedHashMap<>();
   private final List<String> g;
   private List<String> h = io.karatelabs.ext.openapi.ServerBasePathResolver.a;

   public DimensionHandlers a(List<String> var1) {
      this.h = var1 != null && !var1.isEmpty() ? var1 : io.karatelabs.ext.openapi.ServerBasePathResolver.a;
      return this;
   }

   public DimensionHandlers(String var1) {
      if (new Engine().eval(var1) instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            String var6 = String.valueOf(var5.getKey());
            Object var7 = var5.getValue();
            if (var7 instanceof JavaCallable var8) {
               this.b.put(var6, var8);
            } else if (var7 instanceof Map var9) {
               boolean var10 = false;
               if (var9.get("extract") instanceof JavaCallable var11) {
                  this.b.put(var6, var11);
                  var10 = true;
               }

               Object var15 = var9.get("rulebook");
               if (var15 != null) {
                  this.c.put(var6, String.valueOf(var15));
                  var10 = true;
               }

               if (var9.get("cross") instanceof List var16) {
                  var var18 = b(var16);
                  if (!var18.isEmpty()) {
                     this.d.put(var6, var18);
                     var10 = true;
                  }
               }

               Object var17 = var9.get("criticality");
               if (var17 != null && !String.valueOf(var17).isBlank()) {
                  this.e.put(var6, String.valueOf(var17).trim());
                  var10 = true;
               }

               if (var9.get("ignore") instanceof List var19) {
                  var var20 = b(var19);
                  if (!var20.isEmpty()) {
                     this.f.put(var6, var20);
                     var10 = true;
                  }
               }

               if (!var10) {
                  a.warn("dimensions.js entry '{}' has none of extract/rulebook/cross/criticality/ignore — ignored", var6);
               }
            } else {
               a.warn("dimensions.js entry '{}' is neither a function nor a binding object — ignored", var6);
            }
         }

         this.g = new ArrayList<>(this.b.keySet());
      } else {
         throw new IllegalArgumentException(
            "dimensions.js must evaluate to an object literal mapping path-pattern -> function (or {rulebook, extract}); wrap it in parentheses: ({ '/path': function(request, response){ return {...}; } })"
         );
      }
   }

   public boolean a() {
      return this.b.isEmpty();
   }

   public Map<String, String> b() {
      return this.c;
   }

   public Map<String, List<String>> c() {
      return this.d;
   }

   public Map<String, String> d() {
      return this.e;
   }

   public Map<String, List<String>> e() {
      return this.f;
   }

   private static List<String> b(List<?> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Object var3 : var0) {
         if (var3 != null) {
            var1.add(String.valueOf(var3));
         }
      }

      return var1;
   }

   public Map<String, Object> a(HttpAction var1) {
      if (var1 != null && var1.k() != null && !this.b.isEmpty()) {
         io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var2 = null;

         for (String var4 : io.karatelabs.ext.openapi.ServerBasePathResolver.a(var1.g(), this.h)) {
            var2 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var4, this.g);
            if (var2 != null) {
               break;
            }
         }

         if (var2 == null) {
            return null;
         } else {
            JavaCallable var12 = this.b.get(var2.a());
            if (var12 == null) {
               return null;
            } else {
               Object var13 = var1.k().get("request");
               Object var5 = var1.k().get("response");

               try {
                  if (var12.call(null, var13, var5, new DimensionHandlers.ScenarioView(var1)) instanceof Map var7) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();

                     for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
                        if (var10.getValue() != null) {
                           var8.put(String.valueOf(var10.getKey()), var10.getValue());
                        }
                     }

                     return var8.isEmpty() ? null : var8;
                  }
               } catch (Exception var11) {
                  a.debug("dimension handler '{}' threw on path '{}': {}", new Object[]{var2.a(), var1.g(), var11.toString()});
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   static final class ScenarioView implements SimpleObject {
      private final HttpAction ha;

      ScenarioView(HttpAction var1) {
         this.ha = var1;
      }

      @Override
      public Object jsGet(String var1) {
         return switch (var1) {
            case "name" -> this.ha.scenarioName();
            case "line" -> this.ha.scenarioLine();
            case "feature" -> this.ha.featurePath();
            case "tags" -> new ArrayList(this.tags());
            case "tag" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.tagValue(arg(var2));
            case "has" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.tags().contains(arg(var2));
            default -> null;
         };
      }

      private Set<String> tags() {
         return this.ha.tags() == null ? Set.of() : this.ha.tags();
      }

      private String tagValue(String var1) {
         if (var1 == null) {
            return null;
         } else {
            String var2 = var1 + "=";

            for (String var4 : this.tags()) {
               if (var4.startsWith(var2)) {
                  return var4.substring(var2.length());
               }
            }

            return null;
         }
      }

      private static String arg(Object[] var0) {
         return var0 != null && var0.length > 0 && var0[0] != null ? String.valueOf(var0[0]) : null;
      }
   }
}
