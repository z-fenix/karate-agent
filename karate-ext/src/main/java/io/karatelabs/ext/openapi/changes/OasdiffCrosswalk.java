package io.karatelabs.ext.openapi.changes;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class OasdiffCrosswalk {
   public static final String a = "io/karatelabs/ext/openapi/diff/crosswalk.json";

   private OasdiffCrosswalk() {
   }

   public static byte[] a() {
      return (byte[])io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkData.a.clone();
   }

   public static Map<String, Object> b() {
      return io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkData.b;
   }

   public static Map<String, Map<String, Object>> c() {
      return (Map<String, Map<String, Object>>)io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkData.b.get("checks");
   }

   private static byte[] d() {
      try {
         byte[] var1;
         try (InputStream var0 = OasdiffCrosswalk.class.getClassLoader().getResourceAsStream("io/karatelabs/ext/openapi/diff/crosswalk.json")) {
            if (var0 == null) {
               throw new IllegalStateException("missing io/karatelabs/ext/openapi/diff/crosswalk.json");
            }

            var1 = var0.readAllBytes();
         }

         return var1;
      } catch (IOException var5) {
         throw new UncheckedIOException(var5);
      }
   }

   private static List<io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkRule> e() {
      ArrayList<io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkRule> var0 = new ArrayList();
      c()
         .forEach(
            (var1, var2) -> {
               if ("detected".equals(var2.get("disposition"))) {
                  var0.add(
                     new io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkRule(
                        var1,
                        Set.copyOf((List)var2.get("kinds")),
                        (String)var2.get("direction"),
                        a((String)var2.get("facet")),
                        (String)var2.get("site"),
                        var1.contains("-all-of-")
                     )
                  );
               }
            }
         );
      return var0;
   }

   private static String a(String var0) {
      if (var0 == null) {
         return null;
      } else {
         return switch (var0) {
            case "exclusiveMinimum" -> "minimum";
            case "exclusiveMaximum" -> "maximum";
            default -> var0;
         };
      }
   }

   static List<String> a(String var0, String var1, String var2, String var3, String var4, String var5) {
      String var6 = a(var3, var4);
      String var10000;
      if (var0.startsWith("COMPOSITION_BRANCH_")) {
         switch (var6) {
            case "parameter":
               var10000 = "parameterProperty";
               break;
            case "requestBody":
               var10000 = "requestProperty";
               break;
            case "responseBody":
               var10000 = "responseProperty";
               break;
            default:
               var10000 = var6;
         }
      } else {
         var10000 = var6;
      }

      String var7 = var10000;
      boolean var8 = var4 != null && var4.contains("/allOf/") || var5 != null && var5.contains("/allOf/");
      TreeSet<java.lang.String> var12 = new TreeSet();

      for (io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkRule var11 : io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkData.c) {
         if (var11.b.contains(var0)
            && (var11.c == null || var11.c.equals(var1))
            && (var11.d != null ? var11.d.equals(var2) : var2 == null || !var0.startsWith("CONSTRAINT_") || "format".equals(var2) || var11.f)
            && (var11.f ? var8 && c(var11.e).equals(c(var6)) : var11.e.equals(var6) || var11.e.equals(var7))) {
            var12.add(var11.a);
         }
      }

      return var12.isEmpty() ? null : Collections.unmodifiableList(new ArrayList<>(var12));
   }

   static String a(String var0, String var1) {
      if ("security".equals(var0)) {
         return "security";
      } else if (var1 != null && !var1.isEmpty()) {
         String[] var2 = var1.substring(1).split("/", -1);
         if (var2.length >= 4 && (var2[0].equals("paths") || var2[0].equals("webhooks"))) {
            String var3 = "/" + String.join("/", List.of(var2).subList(3, var2.length));
            String var4 = var2[3];

            return switch (var4) {
               case "parameters" -> b(var3) ? "parameterProperty" : "parameter";
               case "requestBody" -> b(var3) ? "requestProperty" : "requestBody";
               case "responses" -> var2.length > 5 && var2[5].equals("headers") ? "responseHeader" : (b(var3) ? "responseProperty" : "responseBody");
               case "security" -> "security";
               default -> "operation";
            };
         } else {
            return "operation";
         }
      } else {
         return "operation";
      }
   }

   private static boolean b(String var0) {
      int var1 = var0.indexOf("/schema");
      return var1 >= 0 && var0.indexOf("/properties/", var1) >= 0;
   }

   private static String c(String var0) {
      return var0.startsWith("request")
         ? "request"
         : (var0.startsWith("parameter") ? "parameter" : (var0.equals("responseHeader") ? var0 : (var0.startsWith("response") ? "response" : var0)));
   }

   private static final class CrosswalkData {
      static final byte[] a = io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.d();
      static final Map<String, Object> b = (Map<String, Object>)Json.parseStrict(new String(a, StandardCharsets.UTF_8), true);
      static final List<io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.CrosswalkRule> c = io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.e();
   }

   private record CrosswalkRule(String a, Set<String> b, String c, String d, String e, boolean f) {
   }
}
