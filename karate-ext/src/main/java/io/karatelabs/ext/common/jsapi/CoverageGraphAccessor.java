package io.karatelabs.ext.common.jsapi;

import io.karatelabs.common.Json;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageGraphAccessor {
   private static final Logger d = LoggerFactory.getLogger(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.class);
   public static final String a = "ext/coverage/coverage-data.json";
   public static final String b = "evidence=model";
   public static final String c = "proof=";

   private CoverageGraphAccessor() {
   }

   public static io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult a(Path var0) {
      if (var0 != null && Files.exists(var0)) {
         try {
            if (Json.of(Files.readString(var0)).value() instanceof Map var5) {
               return new io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.OK, var5, null);
            } else {
               String var3 = "the graph root is not an object";
               d.warn("unreadable coverage data at {}: {}", var0, var3);
               return new io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.UNREADABLE, null, var3);
            }
         } catch (Exception var4) {
            String var2 = var4.getMessage() == null ? var4.getClass().getSimpleName() : var4.getMessage();
            d.warn("unreadable coverage data at {}: {}", var0, var2);
            return new io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.UNREADABLE, null, var2);
         }
      } else {
         return new io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.MISSING, null, null);
      }
   }

   public static List<Map<String, Object>> a(Map<String, Object> var0, String var1) {
      return var0 != null && var0.get(var1) instanceof List var2 ? var2 : List.of();
   }

   public static String a(Map<String, Object> var0, String... var1) {
      for (String var5 : var1) {
         Object var6 = var0.get(var5);
         if (var6 != null) {
            if (!(var6 instanceof List)) {
               return "`" + var5 + "` is not a list";
            }

            for (Object var9 : (List)var6) {
               if (!(var9 instanceof Map)) {
                  return "`" + var5 + "` holds a " + (var9 == null ? "null" : var9.getClass().getSimpleName()) + ", not an object";
               }
            }
         }
      }

      return null;
   }

   public static List<?> a(Map<String, Object> var0) {
      return "req".equals(var0.get("source")) && !"acceptance-criterion".equals(var0.get("kind")) && var0.get("real") instanceof List var1 ? var1 : List.of();
   }

   public static List<Map<String, Object>> b(Map<String, Object> var0, String var1) {
      for (Map var3 : a(var0, "items")) {
         if (var1.equals(var3.get("id"))) {
            LinkedHashSet<java.lang.String> var4 = new LinkedHashSet();
            Object var6 = var3.get("tests");
            if (var6 instanceof List) {
               for (Object var7 : (List)var6) {
                  var4.add(String.valueOf(var7));
               }
            }

            return a(var0, "tests").stream().filter(var1x -> var4.contains(String.valueOf(var1x.get("id")))).toList();
         }
      }

      return null;
   }

   public static Map<String, String> b(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();

      for (Map var3 : a(var0, "tests")) {
         Object var4 = var3.get("id");
         Object var5 = var3.get("name");
         if (var4 != null && var5 != null) {
            Object var6 = var3.get("feature");
            if (var6 != null && String.valueOf(var6).endsWith(".feature")) {
               String var7 = Path.of(String.valueOf(var6)).getFileName().toString();
               var1.put(String.valueOf(var4), var7 + ":" + var5);
            } else {
               var1.put(String.valueOf(var4), String.valueOf(var4));
            }
         }
      }

      return var1;
   }

   public static List<Map<String, Object>> a(Map<String, Object> var0, Object var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var1 instanceof List var3 && !var3.isEmpty()) {
         LinkedHashMap<java.lang.String, java.util.Map> var4 = new LinkedHashMap();
         LinkedHashMap<String, List<Map<String, Object>>> var5 = new LinkedHashMap();

         for (Map var7 : a(var0, "executions")) {
            var4.put(String.valueOf(var7.get("id")), var7);
            var5.computeIfAbsent(String.valueOf(var7.get("test")), var0x -> new ArrayList<>()).add(var7);
         }

         LinkedHashMap<java.lang.String, java.util.Map> var18 = new LinkedHashMap();

         for (Map var8 : a(var0, "tests")) {
            var18.put(String.valueOf(var8.get("id")), var8);
         }

         for (Object var21 : var3) {
            String var9 = String.valueOf(var21);
            Map<java.lang.String, Object> var10 = (Map)var18.get(var9);
            if (var10 != null && var10.get("execution") != null) {
               String var11 = String.valueOf(var10.get("execution"));
               LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
               var12.put("test", var9);
               var12.put("execution", var11);
               Map<java.lang.String, Object> var13 = (Map)var4.get(var11);
               if (var13 != null) {
                  var12.put("run", var13.get("runId"));
               }

               ArrayList<java.util.LinkedHashMap> var14 = new ArrayList();

               for (Map var16 : (Iterable<Map>) (Iterable<?>) (var5.getOrDefault(var9, List.of()))) {
                  if (var11.equals(var16.get("supersededBy")) && "failed".equals(var16.get("outcome"))) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
                     var17.put("execution", var16.get("id"));
                     var17.put("run", var16.get("runId"));
                     var14.add(var17);
                  }
               }

               if (!var14.isEmpty()) {
                  var12.put("previouslyFailed", var14);
               }

               for (String var23 : List.of("pending", "retired", "unknown")) {
                  if (Boolean.TRUE.equals(var10.get(var23))) {
                     var12.put(var23, true);
                  }
               }

               var2.add(var12);
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   public static List<String> b(Map<String, Object> var0, Object var1) {
      return c(b(var0), var1);
   }

   public static List<String> c(Map<String, String> var0, Object var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      if (var1 instanceof List) {
         for (Object var5 : (List)var1) {
            String var6 = String.valueOf(var5);
            var2.add(var0.getOrDefault(var6, var6));
         }
      }

      return var2;
   }

   public record CoverageGraphLoadResult(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus b, Map<String, Object> c, String d) {


      public boolean a() {
         return this.b == io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.OK;
      }

      public io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus b() {
         return this.b;
      }

      public Map<String, Object> c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }
   }

   public static enum CoverageGraphStatus {
      OK,
      MISSING,
      UNREADABLE;
   }
}
