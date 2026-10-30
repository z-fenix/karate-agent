package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class LiveProjection {
   private final Map<String, List<Map<String, Object>>> a;
   private final String b;

   private LiveProjection(Map<String, List<Map<String, Object>>> var1) {
      this.a = var1;
      this.b = io.karatelabs.ext.rules.twin.CanonicalJson.c(var1);
   }

   public static LiveProjection a(Map<String, Object> var0) {
      if (!((var0 == null ? null : var0.get("sequences")) instanceof List var2)) {
         throw new IllegalArgumentException("not a Twin.live result: " + var0);
      } else {
         LinkedHashMap<String, List<Map<String, Object>>> var3 = new LinkedHashMap<>();

         for (Object var5 : var2) {
            Map<java.lang.String, Object> var6 = (Map)var5;
            String var7 = String.valueOf(var6.get("sequenceId"));
            if (!(var6.get("log") instanceof List var8)) {
               throw new IllegalArgumentException("sequence '" + var7 + "' carries no log — a projection needs Twin.live's detail:'log'");
            }

            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var13 = new ArrayList();

            for (Object var11 : var8) {
               var var12 = b((Map<String, Object>)var11);
               if (var12 != null) {
                  var13.add(var12);
               }
            }

            var3.put(var7, var13);
         }

         return new LiveProjection(var3);
      }
   }

   private static Map<String, Object> b(Map<String, Object> var0) {
      if (var0.get("observed") instanceof Map var1) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("request", a((Map<String, Object>)var0.get("request"), "method", "path", "body"));
         if (var1.get("response") instanceof Map var3) {
            var5.put("response", a(var3, "status", "body"));
         } else {
            var5.put("transportFailure", true);
         }

         return var5;
      } else {
         return null;
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0, String... var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (String var6 : var1) {
         if (var0 != null && var0.containsKey(var6)) {
            var2.put(var6, var0.get(var6));
         }
      }

      return var2;
   }

   public Map<String, List<Map<String, Object>>> a() {
      return this.a;
   }

   public List<Map<String, Object>> a(String var1) {
      return this.a.get(var1);
   }

   public String b() {
      return this.b;
   }

   public String c() {
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(this.b);
   }

   public boolean a(LiveProjection var1) {
      return var1 != null && this.b.equals(var1.b);
   }

   public boolean a(LiveProjection var1, String var2) {
      return var1 == null ? false : io.karatelabs.ext.rules.twin.CanonicalJson.c(this.a.get(var2)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.a.get(var2)));
   }

   public List<String> b(LiveProjection var1, String var2) {
      var var3 = this.a.get(var2);
      List var4 = var1 == null ? null : var1.a.get(var2);
      if (var3 != null && var4 != null && var3.size() == var4.size()) {
         TreeSet<java.lang.String> var5 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

         for (int var6 = 0; var6 < var3.size(); var6++) {
            Map<java.lang.String, java.util.Map> var7 = (Map)var3.get(var6);
            Map<java.lang.String, java.util.Map> var8 = (Map)var4.get(var6);
            if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var7.get("request")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var8.get("request"))) || !io.karatelabs.ext.rules.twin.CanonicalJson.c(var7.get("transportFailure")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var8.get("transportFailure")))) {
               return null;
            }

            Map<java.lang.String, Object> var9 = (Map)var7.get("response");
            Map<java.lang.String, Object> var10 = (Map)var8.get("response");
            if (var9 != null && var10 != null) {
               if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var9.get("status")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var10.get("status")))) {
                  return null;
               }

               var var11 = a(var9.get("body"));
               var var12 = a(var10.get("body"));
               TreeSet var13 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
               var13.addAll(var11.keySet());
               var13.addAll(var12.keySet());

               for (String var15 : (Iterable<String>) (Iterable<?>) (var13)) {
                  if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var11.get(var15)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var12.get(var15)))) {
                     var5.add(var15);
                  }
               }
            }
         }

         return new ArrayList<>(var5);
      } else {
         return null;
      }
   }

   private static Map<String, String> a(Object var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();
      a(var0, "body", var1);
      return var1;
   }

   private static void a(Object var0, String var1, Map<String, String> var2) {
      if (var0 instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            a(var5.getValue(), io.karatelabs.ext.rules.twin.ObservationReconciler.a(var1, String.valueOf(var5.getKey())), var2);
         }
      } else {
         var2.put(var1, io.karatelabs.ext.rules.twin.CanonicalJson.c(var0));
      }
   }

   public List<String> b(LiveProjection var1) {
      TreeSet var2 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      var2.addAll(this.a.keySet());
      var2.addAll(var1.a.keySet());
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (String var5 : (Iterable<String>) (Iterable<?>) (var2)) {
         if (!this.a(var1, var5)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   @Override
   public boolean equals(Object var1) {
      return var1 instanceof LiveProjection var2 && this.b.equals(var2.b);
   }

   @Override
   public int hashCode() {
      return this.b.hashCode();
   }

   @Override
   public String toString() {
      return this.a.size() + " sequences, sha256 " + this.c();
   }
}
