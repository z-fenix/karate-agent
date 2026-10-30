package io.karatelabs.ext.postman;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostmanCollectionTreeMapper {
   public static final int a = 200;
   static final String b = "a folder's and the collection's settings are read-only in the editor";
   private static final Pattern c = Pattern.compile("(/item/\\d+)*");
   private static final Pattern d = Pattern.compile("(?<=/):([A-Za-z_][A-Za-z0-9_]*)");

   private PostmanCollectionTreeMapper() {
   }

   public static PostmanCollectionNode a(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var0) {
      return new PostmanCollectionNode(null, Json.of(var0.h()));
   }

   public static PostmanCollectionNode a(PostmanCollectionNode var0, String var1) {
      if (var1 != null && c.matcher(var1).matches()) {
         PostmanCollectionNode var2 = var0;

         for (String var6 : var1.split("/")) {
            if (!var6.isEmpty() && !"item".equals(var6)) {
               int var7 = Integer.parseInt(var6);
               if (var7 >= var2.d.size()) {
                  return null;
               }

               var2 = var2.d.get(var7);
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   public static String a(PostmanCollectionNode var0) {
      StringBuilder var1 = new StringBuilder();

      for (PostmanCollectionNode var2 = var0; var2.c != null; var2 = var2.c) {
         var1.insert(0, "/item/" + var2.c.d.indexOf(var2));
      }

      return var1.toString();
   }

   public static List<Map<String, Object>> a(PostmanCollectionNode var0, boolean var1, io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex var2) {
      int var3 = Integer.MAX_VALUE;
      if (!var1) {
         var3 = 1;

         while (var3 < 64 && a(var0, var3 + 1) <= 200 && a(var0, var3 + 1) > a(var0, var3)) {
            var3++;
         }
      }

      ArrayList var4 = new ArrayList();

      for (PostmanCollectionNode var6 : var0.d) {
         var4.add(a(var6, var3, var1, var2));
      }

      return var4;
   }

   private static int a(PostmanCollectionNode var0, int var1) {
      if (var1 == 0) {
         return 0;
      } else {
         int var2 = 0;

         for (PostmanCollectionNode var4 : var0.d) {
            var2 += 1 + a(var4, var1 - 1);
         }

         return var2;
      }
   }

   private static Map<String, Object> a(PostmanCollectionNode var0, int var1, boolean var2, io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("item", a(var0));
      boolean var5 = b(var0);
      var4.put("kind", var5 ? "request" : "folder");
      var4.put("name", var0.d());
      if (var5) {
         var4.put("method", g(var0));
         var4.put("url", e(var0));
      }

      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("prerequest", b(var0, "prerequest").size());
      var6.put("test", b(var0, "test").size());
      var4.put("events", var6);
      if (var2) {
         var4.put("auth", d(var0));
         var4.put("description", f(var0));
      }

      if (var3 != null) {
         String[] var7 = a(var0, var3);
         var4.put("status", a(List.of(var7)));
         var4.put("byRow", List.of(var7));
      }

      if (!var5) {
         if (var1 <= 1 && !var0.d.isEmpty()) {
            int[] var11 = new int[2];
            a(var0, var11);
            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("folders", var11[0]);
            var12.put("requests", var11[1]);
            var4.put("count", var12);
         } else {
            ArrayList var10 = new ArrayList();

            for (PostmanCollectionNode var9 : var0.d) {
               var10.add(a(var9, var1 - 1, var2, var3));
            }

            var4.put("children", var10);
         }
      }

      return var4;
   }

   private static void a(PostmanCollectionNode var0, int[] var1) {
      for (PostmanCollectionNode var3 : var0.d) {
         var1[b(var3) ? 1 : 0]++;
         a(var3, var1);
      }
   }

   public static String[] a(PostmanCollectionNode var0, io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex var1) {
      String[] var2 = new String[var1.a()];
      if (b(var0)) {
         String[] var8 = var1.b().get(a(var0));

         for (int var10 = 0; var10 < var2.length; var10++) {
            var2[var10] = var8 != null && var8[var10] != null ? var8[var10] : "notrun";
         }

         return var2;
      } else {
         ArrayList var3 = new ArrayList();

         for (PostmanCollectionNode var5 : var0.d) {
            var3.add(a(var5, var1));
         }

         for (int var9 = 0; var9 < var2.length; var9++) {
            ArrayList var11 = new ArrayList();

            for (String[] var7 : (Iterable<String[]>) (Iterable<?>) (var3)) {
               var11.add(var7[var9]);
            }

            var2[var9] = a(var11);
         }

         return var2;
      }
   }

   public static String a(List<String> var0) {
      if (var0.stream().anyMatch(var0x -> "failed".equals(var0x) || "error".equals(var0x))) {
         return "failed";
      } else if (var0.contains("notchecked")) {
         return "notchecked";
      } else {
         return var0.contains("passed") ? "passed" : "notrun";
      }
   }

   public static Map<String, Object> a(PostmanCollectionNode var0, Function<String, String> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("item", a(var0));
      boolean var3 = b(var0);
      var2.put("kind", var0.a() ? "collection" : (var3 ? "request" : "folder"));
      Function var4 = var3
         ? var1
         : var1x -> var1.apply(null) != null ? (String)var1.apply(null) : "a folder's and the collection's settings are read-only in the editor";
      var2.put("name", a(var0.d(), (String)var4.apply(null)));
      var2.put("description", a(f(var0), (String)var4.apply(null)));
      if (var3) {
         boolean var5 = h(var0) != null;
         var2.put("method", a(g(var0), (String)var1.apply(null)));
         var2.put("url", b(var0, var1));
         var2.put(
            "header",
            a(
               var5 ? List.of() : var0.p(),
               !var5 && var0.b.asMap().get("request") instanceof Map var6 && var6.get("header") instanceof String ? "string" : "list",
               (String)var1.apply(null)
            )
         );
         var2.put("body", c(var0, var1));
      }

      var2.put("auth", d(var0, var4));
      if (var0.a()) {
         var2.put("variables", a(var0.G(), "list", (String)var4.apply(null)));
      }

      LinkedHashMap var10 = new LinkedHashMap();
      var10.put("prerequest", a(var0, "prerequest", var1));
      var10.put("test", a(var0, "test", var1));
      var2.put("events", var10);
      var var11 = var0.b.asMap();
      Object var12 = var11.get("protocolProfileBehavior");
      if (var12 != null) {
         var2.put("protocolProfileBehavior", b(var12, "protocolProfileBehavior is preserved as written"));
      }

      if (var11.get("response") instanceof List var8 && !var8.isEmpty()) {
         var2.put("responses", b(var8.size(), "saved response examples are preserved as written"));
      }

      return var2;
   }

   private static Map<String, Object> a(Object var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("value", var0);
      return a((Map<String, Object>)var2, var1);
   }

   private static Map<String, Object> b(Object var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("value", var0);
      return a((Map<String, Object>)var2, var1);
   }

   private static Map<String, Object> a(Map<String, Object> var0, String var1) {
      var0.put("editable", var1 == null);
      if (var1 != null) {
         var0.put("reason", var1);
      }

      return var0;
   }

   private static Map<String, Object> a(List<Map<String, Object>> var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("form", var1);
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var0.size(); var5++) {
         LinkedHashMap var6 = new LinkedHashMap();
         var6.put("row", var5);
         var6.putAll((Map)var0.get(var5));
         var6.putIfAbsent("disabled", false);
         var4.add(var6);
      }

      var3.put("rows", var4);
      return a((Map<String, Object>)var3, var2);
   }

   private static Map<String, Object> b(PostmanCollectionNode var0, Function<String, String> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      Object var3 = h(var0) != null ? h(var0) : i(var0).get("url");
      var2.put("raw", e(var0));
      var2.put("form", var3 instanceof String ? "string" : "object");
      Map var4 = var3 instanceof Map var5 ? var5 : Map.of();

      for (String var6 : List.of("protocol", "host", "port", "path", "hash", "description")) {
         if (var4.get(var6) != null) {
            var2.put(var6, var4.get(var6));
         }
      }

      var2.put("query", a(var4.get("query") instanceof List var8 ? var8 : List.of(), "list", (String)var1.apply(null)).get("rows"));
      var2.put("variable", c(var0));
      return a((Map<String, Object>)var2, (String)var1.apply(null));
   }

   private static List<Map<String, Object>> c(PostmanCollectionNode var0) {
      List var1 = i(var0).get("url") instanceof Map var3 && var3.get("variable") instanceof List var2 ? var2 : List.of();
      ArrayList var9 = new ArrayList();
      String var10 = e(var0);
      Matcher var12 = d.matcher(var10 == null ? "" : var10);
      ArrayList var5 = new ArrayList();

      while (var12.find()) {
         var5.add(var12.group(1));
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         LinkedHashMap var7 = new LinkedHashMap();
         var7.put("row", var6);
         var7.putAll((Map)var1.get(var6));
         var9.add(var7);
         var5.remove(String.valueOf(((Map)var1.get(var6)).get("key")));
      }

      for (String var14 : (Iterable<String>) (Iterable<?>) (var5)) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("row", null);
         var8.put("key", var14);
         var8.put("value", "");
         var9.add(var8);
      }

      return var9;
   }

   private static Map<String, Object> c(PostmanCollectionNode var0, Function<String, String> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (i(var0).get("body") instanceof Map var4) {
         var4.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), var2x));
      }

      return a((Map<String, Object>)var2, (String)var1.apply(null));
   }

   private static Map<String, Object> d(PostmanCollectionNode var0, Function<String, String> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var var3 = d(var0);
      var2.put("own", var3);
      PostmanCollectionNode var4 = PmRuntime.resolveAuth(var0);
      LinkedHashMap var5 = new LinkedHashMap();
      if (var4 == null) {
         var5.put("type", "noauth".equals(var3 == null ? null : var3.get("type")) ? "noauth" : "none");
      } else {
         var5.put("type", var4.q());
         var5.put("from", var4 == var0 ? "this item" : (var4.a() ? "collection" : var4.d()));
         var5.put("item", a(var4));
         if (!PmRuntime.AUTH_TYPES.contains(var4.q())) {
            var5.put("notsupported", true);
         }
      }

      var2.put("resolved", var5);
      String var6 = (String)var1.apply(null);
      return a((Map<String, Object>)var2, var6 != null ? var6 : io.karatelabs.ext.postman.editor.PostmanCollectionEditor.c(var3));
   }

   private static Map<String, Object> d(PostmanCollectionNode var0) {
      return (b(var0) ? i(var0).get("auth") : var0.b.asMap().get("auth")) instanceof Map var2 ? var2 : null;
   }

   private static List<Map<String, Object>> b(PostmanCollectionNode var0, String var1) {
      ArrayList var2 = new ArrayList();
      List var3 = var0.b.get("event", null);
      if (var3 != null) {
         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var3)) {
            if (var1.equals(var5.get("listen"))) {
               var2.add(var5);
            }
         }
      }

      return var2;
   }

   private static List<Map<String, Object>> a(PostmanCollectionNode var0, String var1, Function<String, String> var2) {
      ArrayList var3 = new ArrayList();
      int var4 = 0;

      for (Map var6 : b(var0, var1)) {
         LinkedHashMap var7 = new LinkedHashMap();
         var7.put("n", var4++);
         Object var8 = var6.get("script");
         String var9 = (String)var2.apply(null);
         if (var8 instanceof Map var10) {
            var7.put("exec", var10.get("exec"));
            var7.put("type", var10.get("type"));
         } else {
            var7.put("exec", var8);
            if (var9 == null) {
               var9 = "a script given by reference is preserved as written";
            }
         }

         var3.add(a((Map<String, Object>)var7, var9));
      }

      return var3;
   }

   private static String e(PostmanCollectionNode var0) {
      if (h(var0) != null) {
         return h(var0);
      } else {
         Object var1 = i(var0).get("url");
         if (var1 instanceof String var5) {
            return var5;
         } else {
            return var1 instanceof Map var2 && var2.get("raw") instanceof String var3 ? var3 : var0.J();
         }
      }
   }

   private static String f(PostmanCollectionNode var0) {
      Object var1 = var0.a() ? null : var0.b.asMap().get("description");
      if (var1 instanceof Map var2) {
         return var2.get("content") == null ? null : String.valueOf(var2.get("content"));
      } else {
         return var1 != null ? String.valueOf(var1) : var0.f();
      }
   }

   static boolean b(PostmanCollectionNode var0) {
      return var0.b.asMap().get("request") != null;
   }

   private static String g(PostmanCollectionNode var0) {
      return h(var0) != null ? "GET" : (String)i(var0).get("method");
   }

   private static String h(PostmanCollectionNode var0) {
      return var0.b.asMap().get("request") instanceof String var1 ? var1 : null;
   }

   private static Map<String, Object> i(PostmanCollectionNode var0) {
      return var0.b.asMap().get("request") instanceof Map var1 ? var1 : Map.of();
   }

   public record RunStatusIndex(int a, Map<String, String[]> b) {


      public static io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex a(List<Map<String, Object>> var0) {
         int var1 = 0;

         for (Map var3 : var0) {
            var1 = Math.max(var1, ((Number)var3.get("row")).intValue() + 1);
         }

         int var7 = var1;
         LinkedHashMap<String, String[]> var8 = new LinkedHashMap<>();

         for (Map var5 : var0) {
            String[] var6 = var8.computeIfAbsent((String)var5.get("item"), var1x -> new String[var7]);
            var6[((Number)var5.get("row")).intValue()] = (String)var5.get("status");
         }

         return new io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex(var7, var8);
      }
   }
}
