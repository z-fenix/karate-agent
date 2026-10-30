package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.js.Engine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;

final class UiProjectionBuilder {
   static final String a = "UI_UNKNOWN";
   static final String b = "UI_INVALID";
   static final List<String> c = List.of("money", "date", "percent", "badge");
   static final String d = "prototype · every action is a real call to the mock";
   static final List<String> e = List.of(
      "layout.html", "index.html", "detail.html", "timeline.html", "docs.html", "components/action.html", "components/result.html", "components/exchange.html"
   );

   private UiProjectionBuilder() {
   }

   static Map<String, Object> a(TwinModel var0, String var1, String var2) {
      ArrayList var3 = new ArrayList();
      Map<java.lang.String, java.lang.Object> var4 = a(var0, var1, var2, var3);
      if (!var3.isEmpty()) {
         Map<java.lang.String, Object> var5 = (Map)var3.get(0);
         throw new io.karatelabs.ext.rules.RuleApiException(400, String.valueOf(var5.get("kind")), "app/ui.js: " + var5.get("id") + " — " + var5.get("message"))
            .a(Map.of("key", var5.get("id")));
      } else {
         return var4;
      }
   }

   static List<Map<String, Object>> a(TwinModel var0, String var1, Path var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      if (var0.k() != null && var2 != null && Files.isDirectory(var2)) {
         Resource var4 = new AppResourceResolver(var2).resolve("ui.js", null);
         if (var4 != null) {
            a(var0, var1, var4.getText(), var3);
         }

         Path var5;
         List var6;
         try {
            var5 = var2.toRealPath();

            try (var var7 = Files.walk(var5)) {
               var6 = var7.filter(var0x -> var0x.getFileName().toString().endsWith(".html")).sorted().toList();
            }
         } catch (IOException var12) {
            throw new UncheckedIOException(var12);
         }

         for (Path var8 : (Iterable<Path>) (Iterable<?>) (var6)) {
            String var9 = var5.relativize(var8).toString().replace('\\', '/');
            if (!io.karatelabs.ext.rules.twin.PlayRecordStore.a(var5, var8)) {
               a(var3, "app/" + var9, "UI_INVALID", "is a symbolic link — the app never serves it");
            } else if (!e.contains(var9)) {
               a(var3, "app/" + var9, "UI_UNKNOWN", "names no base page — one of " + e);
            }
         }

         return var3;
      } else {
         return var3;
      }
   }

   private static Map<String, Object> a(TwinModel var0, String var1, String var2, List<Map<String, Object>> var3) {
      var var4 = a(var0, var1);
      if (var2 != null) {
         var var5 = a(var2, var3);
         if (var5 != null) {
            a(var4, var5, a(var0), a(var0.k()), var3);
         }
      }

      return var4;
   }

   static List<String> a(io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var0) {
      return var0.g() ? List.of("read" + var0.h(), "list" + var0.h()) : List.of("read" + var0.h());
   }

   private static Map<String, Set<String>> a(TwinModel var0) {
      LinkedHashMap<java.lang.String, java.util.Set<java.lang.String>> var1 = new LinkedHashMap();
      Map var2 = var0.k().e() instanceof Map var4 && var4.get("properties") instanceof Map var3 ? var3 : Map.of();
      var2.forEach((var1x, var2x) -> {
         if (var2x instanceof Map var3x && (var3x.get("enum") instanceof List || var3x.containsKey("const"))) {
            List var4x = var3x.get("enum") instanceof List var5 ? var5 : Collections.singletonList(var3x.get("const"));
            var4x.forEach(var2xx -> var1.computeIfAbsent(String.valueOf(var1x), var0xxx -> new LinkedHashSet()).add(a(var2xx)));
         }
      });

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var8 : var0.c()) {
         for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var6 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis>) (Iterable<?>) (var8.q() != null && var8.g() != null ? var8.g() : List.of())) {
            if ("enum".equals(var6.b()) || "const".equals(var6.b())) {
               var6.c().forEach(var2x -> var1.computeIfAbsent(var6.a(), var0xx -> new LinkedHashSet()).add(a(var2x)));
            }
         }
      }

      return var1;
   }

   static String a(Object var0) {
      return var0 instanceof String var1 ? var1 : io.karatelabs.ext.rules.twin.CanonicalJson.c(var0);
   }

   private static Map<String, Object> a(TwinModel var0, String var1) {
      io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var2 = var0.k();
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("title", a(var1));
      var3.put("subtitle", "prototype · every action is a real call to the mock");
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("name", var2.h());
      var4.put("label", var2.h());
      var4.put("identity", var2.c());
      var4.put("columns", new ArrayList<>(b(var2)));
      var3.put("resource", var4);
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var5 = new LinkedHashMap();
      Map<java.lang.String, Object> var6 = var2.e() instanceof Map var8 && var8.get("properties") instanceof Map var7 ? var7 : Map.of();

      for (String var17 : b(var2)) {
         var var20 = c(var17);
         if (var6.get(var17) instanceof Map var10) {
            if (var10.get("title") instanceof String var25) {
               var20.put("label", var25);
            }

            if (var10.get("description") instanceof String var26) {
               var20.put("help", var26);
            }
         }

         var5.put(var17, var20);
      }

      LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var16 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.String> var18 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var23 : var0.c()) {
         if (var23.q() != null) {
            ArrayList<java.lang.String> var27 = new ArrayList();

            for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var13 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis>) (Iterable<?>) (var23.g() == null ? List.of() : var23.g())) {
               var27.add(var13.a());
               var5.putIfAbsent(var13.a(), c(var13.a()));
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var31 = new LinkedHashMap();
            var31.put("title", a(var23.a()));
            var31.put("fields", var27);
            var16.put(var23.a(), var31);

            for (String var14 : a(var23)) {
               var18.putIfAbsent(var14, var14);
            }
         }
      }

      for (String var24 : a(var2)) {
         LinkedHashMap<java.lang.String, java.lang.String> var28 = new LinkedHashMap();
         var28.put("title", a(var24));
         var16.put(var24, var28);
      }

      var3.put("fields", var5);
      var3.put("commands", var16);
      var3.put("reasons", var18);
      return var3;
   }

   static Set<String> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0) {
      var var1 = new LinkedHashSet<>(var0.o().values());
      var1.addAll(var0.q().e().b().keySet());
      return var1;
   }

   private static Set<String> b(io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var0) {
      return (var0.e() instanceof Map var2 ? var2.get("properties") : null) instanceof Map var4 ? var4.keySet() : Set.of();
   }

   private static Map<String, Object> c(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("label", a(var0));
      return var1;
   }

   static String a(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 4);

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '_' || var3 == '-') {
            var1.append(' ');
         } else if (Character.isUpperCase(var3) && var2 > 0 && !Character.isUpperCase(var0.charAt(var2 - 1))) {
            var1.append(' ').append(Character.toLowerCase(var3));
         } else {
            var1.append(var3);
         }
      }

      return var1.isEmpty() ? var0 : Character.toUpperCase(var1.charAt(0)) + var1.substring(1);
   }

   private static Map<String, Object> a(String var0, List<Map<String, Object>> var1) {
      Object var2;
      try {
         Engine var3 = new Engine();
         var3.eval(var0);
         var2 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var3.get("ui"));
      } catch (RuntimeException var4) {
         a(var1, "ui", "UI_INVALID", "app/ui.js did not evaluate: " + JsApi.jsErrorMessage(var4));
         return null;
      }

      if (var2 instanceof Map var5) {
         return b(var5);
      } else {
         a(var1, "ui", "UI_INVALID", "app/ui.js must assign one literal: ui = { ... }");
         return null;
      }
   }

   private static void a(Map<String, Object> var0, Map<String, Object> var1, Map<String, Set<String>> var2, List<String> var3, List<Map<String, Object>> var4) {
      a(var1, "ui", List.<String>of("title", "subtitle", "currency", "resource", "fields", "commands", "reasons"), var4);

      for (String var6 : List.of("title", "subtitle")) {
         String var7 = a(var1.get(var6), "ui." + var6, var4);
         if (var7 != null) {
            var0.put(var6, var7);
         }
      }

      String var21 = a(var1.get("currency"), "ui.currency", var4);
      if (var21 != null && b(var21) == null) {
         a(var4, "ui.currency", "UI_INVALID", "'" + var21 + "' is not an ISO 4217 currency code, such as 'USD'");
      } else if (var21 != null) {
         var0.put("currency", var21);
      }

      var var22 = b(var0.get("resource"));
      var var23 = b(var1.get("resource"), "ui.resource", var4);
      if (var23 != null) {
         a(var23, "ui.resource", List.<String>of("label", "columns"), var4);
         String var8 = a(var23.get("label"), "ui.resource.label", var4);
         if (var8 != null) {
            var22.put("label", var8);
         }

         var var9 = a(var23.get("columns"), "ui.resource.columns", c(var22.get("columns")), var4);
         if (var9 != null) {
            var22.put("columns", var9);
         }
      }

      var var24 = b(var0.get("fields"));
      var var25 = b(var1.get("fields"), "ui.fields", var4);

      for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var25 == null ? List.of() : var25.entrySet())) {
         String var12 = "ui.fields." + (String)var11.getKey();
         var var13 = b(var11.getValue(), var12, var4);
         if (!var24.containsKey(var11.getKey())) {
            a(var4, var12, "UI_UNKNOWN", "the model declares no field '" + (String)var11.getKey() + "' — one of " + var24.keySet());
         } else if (var13 != null) {
            a(var13, var12, List.<String>of("label", "help", "format", "values"), var4);
            var var14 = b(var24.get(var11.getKey()));
            var var15 = b(var13.get("values"), var12 + ".values", var4);
            if (var15 != null && !var2.containsKey(var11.getKey())) {
               a(
                  var4,
                  var12 + ".values",
                  "UI_INVALID",
                  "'" + (String)var11.getKey() + "' has no enumerated domain — values labels the members of an enum field"
               );
            } else if (var15 != null) {
               LinkedHashMap<java.lang.String, java.lang.String> var16 = new LinkedHashMap();

               for (Entry var18 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
                  if (!((Set)var2.get(var11.getKey())).contains(var18.getKey())) {
                     a(
                        var4,
                        var12 + ".values." + (String)var18.getKey(),
                        "UI_UNKNOWN",
                        "'" + (String)var18.getKey() + "' is not a member of '" + (String)var11.getKey() + "' — one of " + var2.get(var11.getKey())
                     );
                  } else {
                     String var19 = a(var18.getValue(), var12 + ".values." + (String)var18.getKey(), var4);
                     if (var19 != null) {
                        var16.put((String)var18.getKey(), var19);
                     }
                  }
               }

               var14.put("values", var16);
            }

            for (String var40 : List.of("label", "help")) {
               String var44 = a(var13.get(var40), var12 + "." + var40, var4);
               if (var44 != null) {
                  var14.put(var40, var44);
               }
            }

            String var37 = a(var13.get("format"), var12 + ".format", var4);
            if (var37 != null && !c.contains(var37)) {
               a(var4, var12 + ".format", "UI_UNKNOWN", "'" + var37 + "' is not a format — one of " + c);
            } else if (var37 != null) {
               var14.put("format", var37);
            }
         }
      }

      var var26 = b(var0.get("commands"));
      var var27 = b(var1.get("commands"), "ui.commands", var4);

      for (Entry var30 : (Iterable<Entry>) (Iterable<?>) (var27 == null ? List.of() : var27.entrySet())) {
         String var32 = "ui.commands." + (String)var30.getKey();
         var var34 = b(var30.getValue(), var32, var4);
         if (!var26.containsKey(var30.getKey())) {
            a(var4, var32, "UI_UNKNOWN", "'" + (String)var30.getKey() + "' is neither a bound command nor a derived read — one of " + var26.keySet());
         } else if (var34 != null) {
            a(var34, var32, var3.contains(var30.getKey()) ? List.<String>of("title", "help") : List.<String>of("title", "help", "fields"), var4);
            var var38 = b(var26.get(var30.getKey()));

            for (String var45 : List.of("title", "help")) {
               String var47 = a(var34.get(var45), var32 + "." + var45, var4);
               if (var47 != null) {
                  var38.put(var45, var47);
               }
            }

            if (!var3.contains(var30.getKey())) {
               var var42 = c(var38.get("fields"));
               List<java.lang.Object> var46 = a(var34.get("fields"), var32 + ".fields", var42, var4);
               if (var46 != null) {
                  for (Object var20 : var42) {
                     if (!var46.contains(var20)) {
                        var46.add(var20);
                     }
                  }

                  var38.put("fields", var46);
               }
            }
         }
      }

      var var29 = b(var0.get("reasons"));
      var var31 = b(var1.get("reasons"), "ui.reasons", var4);

      for (Entry var35 : (Iterable<Entry>) (Iterable<?>) (var31 == null ? List.of() : var31.entrySet())) {
         String var39 = "ui.reasons." + (String)var35.getKey();
         if (!var29.containsKey(var35.getKey())) {
            a(var4, var39, "UI_UNKNOWN", "the model declares no refusal reason '" + (String)var35.getKey() + "' — one of " + var29.keySet());
         } else {
            String var43 = a(var35.getValue(), var39, var4);
            if (var43 != null) {
               var29.put((String)var35.getKey(), var43);
            }
         }
      }
   }

   static String b(String var0) {
      try {
         return Currency.getInstance(var0).getSymbol(Locale.US);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   private static void a(Map<String, Object> var0, String var1, List<String> var2, List<Map<String, Object>> var3) {
      for (String var5 : var0.keySet()) {
         if (!var2.contains(var5)) {
            a(var3, var1 + "." + var5, "UI_UNKNOWN", "'" + var5 + "' is not an overlay key here — one of " + var2);
         }
      }
   }

   private static List<Object> a(Object var0, String var1, List<Object> var2, List<Map<String, Object>> var3) {
      if (var0 == null) {
         return null;
      } else if (var0 instanceof List var4 && !var4.stream().anyMatch(var0x -> !(var0x instanceof String))) {
         ArrayList<java.lang.Object> var5 = new ArrayList();

         for (Object var7 : var4) {
            if (var2.contains(var7)) {
               var5.add(var7);
            } else {
               a(var3, var1 + "." + var7, "UI_UNKNOWN", "'" + var7 + "' is not one of " + var2);
            }
         }

         return var5;
      } else {
         a(var3, var1, "UI_INVALID", "must be a list of names");
         return null;
      }
   }

   private static String a(Object var0, String var1, List<Map<String, Object>> var2) {
      if (var0 != null && !(var0 instanceof String)) {
         a(var2, var1, "UI_INVALID", "must be text");
         return null;
      } else {
         return (String)var0;
      }
   }

   private static Map<String, Object> b(Object var0, String var1, List<Map<String, Object>> var2) {
      if (var0 != null && !(var0 instanceof Map)) {
         a(var2, var1, "UI_INVALID", "must be an object");
         return null;
      } else {
         return var0 == null ? null : b(var0);
      }
   }

   private static void a(List<Map<String, Object>> var0, String var1, String var2, String var3) {
      LinkedHashMap<String, Object> var4 = new LinkedHashMap();
      var4.put("id", var1);
      var4.put("kind", var2);
      var4.put("status", "finding");
      var4.put("message", var3);
      var0.add(var4);
   }

   private static Map<String, Object> b(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static List<Object> c(Object var0) {
      return (List<Object>)var0;
   }
}
