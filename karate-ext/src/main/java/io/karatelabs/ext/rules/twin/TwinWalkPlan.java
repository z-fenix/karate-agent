package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public record TwinWalkPlan(Map<String, Integer> c, Map<String, List<Object>> d, List<String> component3, String component4) {
   private static final char e = '→';
   private static final String f = "default";

   public TwinWalkPlan(Map<String, Integer> c, Map<String, List<Object>> d, List<String> component3, String component4) {
      this.c = c;
      this.d = d;
      this.component3 = component3;
      this.component4 = component4;
   }

   public static Object a(Object var0, Path var1) {
      if (var0 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         return null;
      } else if (var0 instanceof String var2) {
         String var3 = "'" + var2 + "' must be a relative path to a JSON file inside the project";
         String var4 = "'" + var2 + "' is not a readable JSON plan";

         Path var5;
         try {
            Path var6 = Path.of(var2);
            if (var2.isBlank() || var6.isAbsolute()) {
               throw TwinException.a("plan", var3);
            }

            Path var7 = var1.toRealPath();
            var5 = var7.resolve(var6).toRealPath();
            if (!var5.startsWith(var7)) {
               throw TwinException.a("plan", var3 + " '" + var7 + "'");
            }
         } catch (InvalidPathException var9) {
            throw TwinException.a("plan", var3 + " (" + var9.getMessage() + ")");
         } catch (IOException var10) {
            throw TwinException.a("plan", var4 + " (" + var10 + ")");
         }

         try {
            return Json.parseStrict(Files.readString(var5, StandardCharsets.UTF_8), true);
         } catch (RuntimeException | IOException var8) {
            throw TwinException.a("plan", var4 + " (" + var8.getMessage() + ")");
         }
      } else {
         return var0;
      }
   }

   public static TwinWalkPlan a(Object var0, TwinModel var1, TwinExecutionLimits var2, Set<String> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      LinkedHashMap var5 = new LinkedHashMap();
      ArrayList var6 = new ArrayList();
      if (var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         if (!(var0 instanceof Map var7)) {
            throw TwinException.a("plan", "must be an object {depth, levels, focus}");
         }

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            String var10 = String.valueOf(var9.getKey());
            switch (var10) {
               case "depth":
                  a(var9.getValue(), var1, var2, var4);
                  break;
               case "levels":
                  a(var9.getValue(), var1, var5);
                  break;
               case "focus":
                  a(var9.getValue(), var1, var3, var6);
                  break;
               default:
                  throw TwinException.a(var10, "unknown key — a plan is {depth, levels, focus}");
            }
         }
      }

      LinkedHashMap var13 = new LinkedHashMap();
      var13.put("default", var4.getOrDefault("default", var2.a()));
      ArrayList<String> var14 = new ArrayList(var4.keySet());
      var14.remove("default");
      var14.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (String var17 : (Iterable<String>) (Iterable<?>) (var14)) {
         var13.put(var17, (Integer)var4.get(var17));
      }

      LinkedHashMap var16 = new LinkedHashMap();
      ArrayList<String> var18 = new ArrayList(var5.keySet());
      var18.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (String var20 : (Iterable<String>) (Iterable<?>) (var18)) {
         var16.put(var20, (List)var5.get(var20));
      }

      TwinWalkPlan var19 = new TwinWalkPlan(Collections.unmodifiableMap(var13), Collections.unmodifiableMap(var16), List.copyOf(var6), null);
      return new TwinWalkPlan(var19.c(), var19.d(), var19.e(), io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var19.a())));
   }

   public Map<String, Object> a() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("depth", new LinkedHashMap<>(this.c));
      var1.put("levels", new LinkedHashMap<>(this.d));
      var1.put("focus", List.copyOf(this.component3));
      return var1;
   }

   public Map<String, Object> b() {
      var var1 = this.a();
      var1.put("planSha", this.component4);
      return var1;
   }

   private static void a(Object var0, TwinModel var1, TwinExecutionLimits var2, Map<String, Integer> var3) {
      if (!(var0 instanceof Map var4)) {
         throw TwinException.a("depth", "must be an object of caps keyed 'default' or '<a>→<b>'");
      } else {
         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var7 = String.valueOf(var6.getKey());
            String var8 = "depth." + var7;
            if (!"default".equals(var7)) {
               int var9 = var7.indexOf(8594);
               if (var9 < 0 || var7.indexOf(8594, var9 + 1) >= 0) {
                  throw TwinException.a(var8, "must be 'default' or one pair '<a>→<b>'");
               }

               if (!var1.f().containsKey(var7.substring(0, var9)) || !var1.f().containsKey(var7.substring(var9 + 1))) {
                  throw TwinException.a(var8, "names a command the twin does not declare");
               }
            }

            var3.put(var7, a(var8, var6.getValue(), var2));
         }
      }
   }

   private static int a(String var0, Object var1, TwinExecutionLimits var2) {
      if (var1 instanceof Number var3 && Double.isFinite(var3.doubleValue()) && var3.doubleValue() == Math.rint(var3.doubleValue())) {
         long var4 = var3.longValue();
         if (var4 < 0L) {
            throw TwinException.a(var0, "must not be negative");
         } else if (var4 > var2.a()) {
            throw TwinException.a(var0, "must not exceed the frozen maxDepth " + var2.a() + " — a plan only tightens a ceiling");
         } else {
            return (int)var4;
         }
      } else {
         throw TwinException.a(var0, "must be a whole number of steps");
      }
   }

   private static void a(Object var0, TwinModel var1, Map<String, List<Object>> var2) {
      if (!(var0 instanceof Map var3)) {
         throw TwinException.a("levels", "must be an object keyed '<command>.<argument>'");
      } else {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            String var6 = String.valueOf(var5.getKey());
            String var7 = "levels." + var6;
            int var8 = var6.indexOf(46);
            if (var8 <= 0 || var8 == var6.length() - 1 || var6.indexOf(46, var8 + 1) >= 0) {
               throw TwinException.a(var7, "must name '<command>.<argument>'");
            }

            io.karatelabs.ext.rules.twin.TwinModel.CommandModel var9 = var1.f().get(var6.substring(0, var8));
            if (var9 == null) {
               throw TwinException.a(var7, "names a command the twin does not declare");
            }

            if (var9.g() == null) {
               throw TwinException.a(var7, "names a literal-form command, which declares no axes");
            }

            String var10 = var6.substring(var8 + 1);
            io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var11 = null;

            for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var13 : var9.g()) {
               if (var13.a().equals(var10)) {
                  var11 = var13;
               }
            }

            if (var11 == null) {
               throw TwinException.a(var7, "names an argument the command does not declare");
            }

            if ("const".equals(var11.b())) {
               throw TwinException.a(var7, "names a const, which is one value and not an axis");
            }

            var2.put(var6, a(var7, var11, var5.getValue()));
         }
      }
   }

   private static List<Object> a(String var0, io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var1, Object var2) {
      if (var2 instanceof List var3 && !var3.isEmpty()) {
         ArrayList var4 = new ArrayList(var3.size());

         for (Object var6 : var3) {
            try {
               io.karatelabs.ext.rules.twin.CanonicalJson.b(var6);
            } catch (SnapshotException var8) {
               throw TwinException.a(var0, var8.getMessage());
            }

            if (!TwinModel.a(var1, var6)) {
               throw TwinException.a(var0, io.karatelabs.ext.rules.twin.CanonicalJson.c(var6) + " is not in the declared domain of '" + var1.a() + "'");
            }

            var4.add(io.karatelabs.ext.rules.twin.CanonicalJson.d(var6));
         }

         return Collections.unmodifiableList(var4);
      } else {
         throw TwinException.a(var0, "must name at least one level");
      }
   }

   private static void a(Object var0, TwinModel var1, Set<String> var2, List<String> var3) {
      if (!(var0 instanceof List var4)) {
         throw TwinException.a("focus", "must be an array of requirement ids");
      } else {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            String var6 = "focus[" + var5 + "]";
            if (!(var4.get(var5) instanceof String var7)) {
               throw TwinException.a(var6, "must be a requirement id");
            }

            boolean var11 = var2 != null && var2.contains(var7);

            for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var10 : var1.c()) {
               var11 |= var7.equals(var10.j());
            }

            if (!var11) {
               throw TwinException.a(var6, "matches neither a supplied required row nor a req: in the twin");
            }

            if (!var3.contains(var7)) {
               var3.add(var7);
            }
         }
      }
   }

   public Map<String, Integer> c() {
      return this.c;
   }

   public Map<String, List<Object>> d() {
      return this.d;
   }

   public List<String> e() {
      return this.component3;
   }

   public String f() {
      return this.component4;
   }
}
