package io.karatelabs.ext.max.console;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.help.ClasspathTextLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConsoleRegistry {
   static final String PATH = "META-INF/karate/console/registry.json";
   private final List<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> roots;
   private final Map<String, ConsoleRegistry.Entry> byId;
   private static volatile ConsoleRegistry instance;

   private ConsoleRegistry(List<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> var1, Map<String, ConsoleRegistry.Entry> var2) {
      this.roots = var1;
      this.byId = var2;
   }

   public static ConsoleRegistry get() {
      ConsoleRegistry var0 = instance;
      if (var0 == null) {
         synchronized (ConsoleRegistry.class) {
            var0 = instance;
            if (var0 == null) {
               instance = var0 = load();
            }
         }
      }

      return var0;
   }

   private static ConsoleRegistry load() {
      ArrayList<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> var0 = new ArrayList();
      LinkedHashMap var1 = new LinkedHashMap();
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/console/registry.json");
      if (var2 != null && Json.of(var2).value() instanceof Map var4) {
         Object var6 = var4.get("pages");
         if (var6 instanceof List) {
            for (Object var7 : (List)var6) {
               if (var7 instanceof Map var8) {
                  io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var9 = build(var8, null, null, var1);
                  if (var9 != null) {
                     var0.add(var9);
                  }
               }
            }
         }
      }

      return new ConsoleRegistry(var0, var1);
   }

   private static io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode build(Map<String, Object> var0, String var1, String var2, Map<String, ConsoleRegistry.Entry> var3) {
      Object var4 = var0.get("id");
      if (var4 != null && !String.valueOf(var4).isBlank()) {
         String var5 = String.valueOf(var4).trim();
         String var6 = var1 == null ? var5 : var1 + "." + var5;
         String var7 = var0.get("route") instanceof String var8 && !var8.isBlank() ? var8.trim() : null;
         String var18 = var7 != null ? var7 : var2;
         String var19 = var0.get("label") instanceof String var10 ? var10 : var5;
         String var20 = var0.get("help") instanceof String var21 && !var21.isBlank() ? var21.trim() : null;
         String var22 = var0.get("note") instanceof String var23 ? var23 : null;
         ArrayList<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> var24 = new ArrayList();
         Object var14 = var0.get("sections");
         if (var14 instanceof List) {
            for (Object var15 : (List)var14) {
               if (var15 instanceof Map var16) {
                  io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var17 = build(var16, var6, var18, var3);
                  if (var17 != null) {
                     var24.add(var17);
                  }
               }
            }
         }

         ConsoleRegistry.Entry var26 = new ConsoleRegistry.Entry(var6, var18, var7, var6, var19, var1, var20, var22, var1 == null, !var24.isEmpty());
         var3.putIfAbsent(var6, var26);
         return new io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode(var26, var24);
      } else {
         return null;
      }
   }

   public List<Map<String, Object>> pages() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var3 : this.roots) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("id", var3.a.id());
         var4.put("route", var3.a.route());
         var4.put("label", var3.a.label());
         if (var3.a.note() != null) {
            var4.put("note", var3.a.note());
         }

         if (var3.a.helpId() != null) {
            var4.put("help", var3.a.helpId());
         }

         var4.put("sections", var3.b.size());
         var1.add(var4);
      }

      return var1;
   }

   public List<Map<String, Object>> sections(String var1) {
      io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var2 = this.find(var1);
      return (List<Map<String, Object>>)(var2 == null ? new ArrayList<>() : this.project(var2.b));
   }

   private List<Map<String, Object>> project(List<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var4 : var1) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("id", var4.a.id());
         var5.put("label", var4.a.label());
         var5.put("anchor", var4.a.anchor());
         if (var4.a.declaredRoute() != null) {
            var5.put("route", var4.a.declaredRoute());
         }

         if (var4.a.helpId() != null) {
            var5.put("help", var4.a.helpId());
         }

         if (var4.a.note() != null) {
            var5.put("note", var4.a.note());
         }

         if (!var4.b.isEmpty()) {
            var5.put("sections", this.project(var4.b));
         }

         var2.add(var5);
      }

      return var2;
   }

   public Map<String, Object> resolve(String var1) {
      ConsoleRegistry.Entry var2 = var1 == null ? null : this.byId.get(var1);
      if (var2 == null) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("id", var2.id());
         var3.put("label", var2.label());
         var3.put("anchor", var2.anchor());
         if (var2.route() != null) {
            var3.put("route", var2.route());
         }

         if (var2.navParent() != null) {
            var3.put("navParent", var2.navParent());
         }

         if (var2.helpId() != null) {
            var3.put("helpId", var2.helpId());
         }

         if (var2.note() != null) {
            var3.put("note", var2.note());
         }

         var3.put("isPage", var2.page());
         return var3;
      }
   }

   public List<String> ids() {
      return new ArrayList<>(this.byId.keySet());
   }

   public List<String> declaredRoutes() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (ConsoleRegistry.Entry var3 : this.byId.values()) {
         if (var3.declaredRoute() != null) {
            var1.add(var3.declaredRoute());
         }
      }

      return var1;
   }

   private io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode find(String var1) {
      return var1 == null ? null : this.find(this.roots, var1);
   }

   private io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode find(List<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> var1, String var2) {
      for (io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var4 : var1) {
         if (var4.a.id().equals(var2)) {
            return var4;
         }

         io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode var5 = this.find(var4.b, var2);
         if (var5 != null) {
            return var5;
         }
      }

      return null;
   }

   public record Entry(String id, String route, String declaredRoute, String anchor, String label, String navParent, String helpId, String note, boolean page, boolean hasSections) {

   }

   private record RegistryNode(ConsoleRegistry.Entry a, List<io.karatelabs.ext.max.console.ConsoleRegistry.RegistryNode> b) {

   }
}
