package io.karatelabs.ext.robot;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RobotPatternPackLoader {
   private static final Logger a = LoggerFactory.getLogger(RobotPatternPackLoader.class);
   private static final String b = "META-INF/karate/robot/packs/";
   private static volatile List<Map<String, Object>> c;
   private static volatile String d;

   private RobotPatternPackLoader() {
   }

   public static String a() {
      c();
      return d;
   }

   public static List<Map<String, Object>> b() {
      c();
      return c;
   }

   public static String a(String var0) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/robot/packs/" + io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0) + "/study.md");
   }

   public static String b(String var0) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/robot/packs/" + io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0) + "/helpers.js");
   }

   private static void c() {
      if (c == null) {
         synchronized (RobotPatternPackLoader.class) {
            if (c == null) {
               ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
               String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/robot/packs/index.json");
               if (var2 != null) {
                  try {
                     if (Json.of(var2).value() instanceof Map var3) {
                        Object var12 = var3.get("packs");
                        if (var12 instanceof List) {
                           for (Object var6 : (List)var12) {
                              var var7 = c(String.valueOf(var6));
                              if (var7 != null) {
                                 var1.add(var7);
                              }
                           }
                        }
                     }
                  } catch (Exception var9) {
                     a.warn("pattern-pack index unreadable — running pure-generic: {}", var9.getMessage());
                  }
               }

               LinkedHashMap<java.lang.String, java.util.ArrayList> var11 = new LinkedHashMap();
               var11.put("packs", var1);
               d = Json.of(var11).toString();
               c = var1;
               a.debug("loaded {} robot pattern pack(s)", var1.size());
            }
         }
      }
   }

   private static Map<String, Object> c(String var0) {
      String var1 = io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0);
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/robot/packs/" + var1 + "/pack.json");
      if (var2 == null) {
         a.warn("pattern pack '{}' has no pack.json — skipped", var0);
         return null;
      } else {
         try {
            if (Json.of(var2).value() instanceof Map var3) {
               LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
               var6.put("id", var3.get("id") != null ? var3.get("id") : var1);
               a(var6, var3, "app");
               a(var6, var3, "versionRange");
               a(var6, var3, "label");
               var6.put("fingerprint", a(var3.get("fingerprint")));
               var6.put("registry", a(var3.get("registry")));
               return var6;
            } else {
               return null;
            }
         } catch (Exception var5) {
            a.warn("pattern pack '{}' unreadable — skipped: {}", var0, var5.getMessage());
            return null;
         }
      }
   }

   private static void a(Map<String, Object> var0, Map<?, ?> var1, String var2) {
      Object var3 = var1.get(var2);
      if (var3 != null) {
         var0.put(var2, var3);
      }
   }

   private static Object a(Object var0) {
      if (var0 instanceof Map var1) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            String var5 = String.valueOf(var4.getKey());
            if (!var5.startsWith("_")) {
               var2.put(var5, var4.getValue());
            }
         }

         return var2;
      } else {
         return var0;
      }
   }
}
