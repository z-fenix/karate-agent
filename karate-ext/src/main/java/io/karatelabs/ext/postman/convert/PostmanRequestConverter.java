package io.karatelabs.ext.postman.convert;

import io.karatelabs.common.StringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

final class PostmanRequestConverter {
   private static final Pattern a = Pattern.compile("^\\* (?:url|path|params?|headers?|cookies?|form fields?|request) .*");
   private final BiConsumer<String, String> b;
   private String c;

   PostmanRequestConverter(BiConsumer<String, String> var1) {
      this.b = var1;
   }

   void a() {
      this.c = null;
   }

   List<String> a(io.karatelabs.ext.postman.PostmanCollectionNode var1) {
      String var2 = a(var1.d());
      PostmanVariableTranslator var3 = new PostmanVariableTranslator(false);
      ArrayList var4 = new ArrayList();
      var var5 = this.a(var1, "prerequest", var2);
      ArrayList var6 = new ArrayList();
      io.karatelabs.ext.postman.convert.PostmanRequestConverter.RequestUrlParts var7 = io.karatelabs.ext.postman.convert.PostmanRequestConverter.RequestUrlParts.a(var1);
      var6.add("Given url " + var3.d(var7.a()));
      ArrayList var8 = new ArrayList();

      for (String var10 : var7.b()) {
         if (var10.startsWith(":") && var10.length() > 1) {
            String var11 = var10.substring(1);
            String var12 = var7.d().get(var11);
            if (var12 != null && !var12.isEmpty()) {
               var8.add(var3.d(var12));
            } else {
               var4.add("the path variable :" + var11 + " has no value in the collection");
               var8.add("karate.get(" + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var11) + ")");
            }
         } else {
            var8.add(var3.d(var10));
         }
      }

      if (!var8.isEmpty()) {
         var6.add("And path " + String.join(", ", var8));
      }

      for (String[] var18 : var7.c()) {
         var6.add("And param " + var18[0] + " = " + var3.d(var18[1]));
      }

      String var17 = var1.z();
      String var19 = "raw".equals(var17) && var1.A() != null && !var1.A().isBlank() ? var3.e(var1.A()) : null;

      for (Map var22 : var1.p()) {
         if (!Boolean.TRUE.equals(var22.get("disabled")) && var22.get("key") != null) {
            String var13 = String.valueOf(var22.get("key")).strip();
            String var14 = var22.get("value") == null ? "" : String.valueOf(var22.get("value"));
            if (!"content-type".equalsIgnoreCase(var13) || !a(var14, var17, var19)) {
               var6.add("And header " + var13 + " = " + var3.d(var14));
            }
         }
      }

      String var21 = a(var1, var3, var6, var4);
      a(var1, var17, var19, var3, var6, var4);
      String var23 = var1.g();
      var6.add("When method " + (var23 != null && !var23.isBlank() ? var23.strip().toLowerCase() : "get"));

      for (String var26 : var3.b) {
         var4.add("the dynamic variable {{" + var26 + "}}");
      }

      ArrayList var25 = new ArrayList();

      for (String var15 : (Iterable<String>) (Iterable<?>) (var4)) {
         var25.add("# not converted: " + var15);
         this.b.accept(var2, var15);
      }

      if (var21 == null ? this.c != null : !var21.equals(this.c)) {
         var25.add(var21 == null ? "* configure auth = null" : var21);
         this.c = var21;
      }

      if (!var5.isEmpty()) {
         var25.add("# " + var2 + ": before the request");
         var25.addAll(var5);
      }

      a(var25, var2);
      var25.addAll(var6);
      var25.addAll(this.a(var1, "test", var2));
      return var25;
   }

   private static void a(List<String> var0, String var1) {
      int var2 = var0.size();

      for (int var3 = var0.size() - 1; var3 >= 0 && (((String)var0.get(var3)).startsWith("#") || a.matcher((CharSequence)var0.get(var3)).matches()); var3--) {
         if (!((String)var0.get(var3)).startsWith("#")) {
            var2 = var3;
         }
      }

      var0.add(var2, "# " + var1);
      if (var2 > 0) {
         var0.add(var2, "");
      }
   }

   static List<String> a(io.karatelabs.ext.postman.PostmanCollectionNode var0, String var1) {
      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.postman.PostmanCollectionNode var3 = var0; var3 != null; var3 = var3.c) {
         var2.add(0, var3);
      }

      ArrayList var6 = new ArrayList();

      for (io.karatelabs.ext.postman.PostmanCollectionNode var5 : (Iterable<io.karatelabs.ext.postman.PostmanCollectionNode>) (Iterable<?>) (var2)) {
         var6.addAll(var5.r(var1));
      }

      return var6;
   }

   private List<String> a(io.karatelabs.ext.postman.PostmanCollectionNode var1, String var2, String var3) {
      ArrayList var4 = new ArrayList();
      HashSet var5 = new HashSet();

      for (Map var7 : var1.p()) {
         if (!Boolean.TRUE.equals(var7.get("disabled")) && var7.get("key") != null) {
            var5.add(String.valueOf(var7.get("key")).strip().toLowerCase());
         }
      }

      PostmanScriptConverter var9 = new PostmanScriptConverter(var2x -> this.b.accept(var3, var2x), var1.d() == null ? "" : var1.d(), var5);

      for (String var8 : a(var1, var2)) {
         var4.addAll(var9.a(var8));
      }

      return var4;
   }

   static Map<?, ?> b(io.karatelabs.ext.postman.PostmanCollectionNode var0) {
      for (io.karatelabs.ext.postman.PostmanCollectionNode var1 = var0; var1 != null; var1 = var1.c) {
         if ((var1.b() ? var1.b.get("request.auth", null) : var1.b.get("auth", null)) instanceof Map var3) {
            Object var4 = var3.get("type");
            if (var4 != null && !"inherit".equals(var4)) {
               return "noauth".equals(var4) ? null : var3;
            }
         }
      }

      return null;
   }

   static Map<String, String> a(Map<?, ?> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      Object var2 = var0.get(String.valueOf(var0.get("type")));
      if (var2 instanceof List) {
         for (Object var6 : (List)var2) {
            if (var6 instanceof Map var7 && var7.get("key") != null) {
               var1.put(String.valueOf(var7.get("key")), var7.get("value") == null ? null : String.valueOf(var7.get("value")));
            }
         }
      } else if (var2 instanceof Map var4) {
         var4.forEach((var1x, var2x) -> var1.put(String.valueOf(var1x), var2x == null ? null : String.valueOf(var2x)));
      }

      return var1;
   }

   private static String a(io.karatelabs.ext.postman.PostmanCollectionNode var0, PostmanVariableTranslator var1, List<String> var2, List<String> var3) {
      var var4 = b(var0);
      if (var4 == null) {
         return null;
      } else {
         String var5 = String.valueOf(var4.get("type"));
         var var6 = a(var4);
         switch (var5) {
            case "bearer":
               var2.add("And header Authorization = " + var1.d("Bearer " + b((String)var6.get("token"))));
               return null;
            case "apikey":
               String var15 = (String)var6.get("key");
               if (var15 != null && !var15.isBlank() && !var15.contains("{{")) {
                  boolean var16 = "query".equals(var6.get("in"));
                  var2.add((var16 ? "And param " : "And header ") + var15.strip() + " = " + var1.d(b((String)var6.get("value"))));
                  return null;
               }

               var3.add("apikey auth with no literal key name");
               return null;
            case "basic":
               return "* configure auth = { type: 'basic', username: "
                  + var1.d(b((String)var6.get("username")))
                  + ", password: "
                  + var1.d(b((String)var6.get("password")))
                  + " }";
            case "ntlm":
               return "* configure auth = { type: 'ntlm', username: "
                  + var1.d(b((String)var6.get("username")))
                  + ", password: "
                  + var1.d(b((String)var6.get("password")))
                  + (var6.get("domain") == null ? "" : ", domain: " + var1.d((String)var6.get("domain")))
                  + (var6.get("workstation") == null ? "" : ", workstation: " + var1.d((String)var6.get("workstation")))
                  + " }";
            case "oauth2":
               String var9 = (String)var6.get("grant_type");
               if (var9 != null && !"client_credentials".equals(var9)) {
                  var3.add("OAuth2 " + var9 + " auth: it needs a browser sign-in");
                  return null;
               } else {
                  StringBuilder var10 = new StringBuilder("* configure auth = { type: 'oauth2', grantType: 'client_credentials'");

                  for (String[] var14 : new String[][]{
                     {"accessTokenUrl", "accessTokenUrl"}, {"clientId", "clientId"}, {"clientSecret", "clientSecret"}, {"scope", "scope"}
                  }) {
                     if (var6.get(var14[0]) != null) {
                        var10.append(", ").append(var14[1]).append(": ").append(var1.d((String)var6.get(var14[0])));
                     }
                  }

                  return var10.append(" }").toString();
               }
            default:
               var3.add(var5 + " auth");
               return null;
         }
      }
   }

   private static String b(String var0) {
      return var0 == null ? "" : var0;
   }

   private static boolean a(String var0, String var1, String var2) {
      String var3 = var0.toLowerCase();
      return var2 != null && var3.startsWith("application/json")
         || "urlencoded".equals(var1) && var3.startsWith("application/x-www-form-urlencoded")
         || "formdata".equals(var1) && var3.startsWith("multipart/form-data");
   }

   private static void a(io.karatelabs.ext.postman.PostmanCollectionNode var0, String var1, String var2, PostmanVariableTranslator var3, List<String> var4, List<String> var5) {
      if (var1 != null) {
         switch (var1) {
            case "raw":
               String var14 = var0.A();
               if (var14 == null || var14.isBlank()) {
                  return;
               }

               var4.add("And request " + (var2 != null ? var2 : var3.d(var14)));
               break;
            case "urlencoded":
               for (Map var16 : var0.B()) {
                  if (!Boolean.TRUE.equals(var16.get("disabled")) && var16.get("key") != null) {
                     var4.add("And form field " + var16.get("key") + " = " + var3.d(var16.get("value") == null ? "" : String.valueOf(var16.get("value"))));
                  }
               }
               break;
            case "formdata":
               for (Map var15 : var0.C()) {
                  if (!Boolean.TRUE.equals(var15.get("disabled")) && var15.get("key") != null) {
                     if ("file".equals(var15.get("type"))) {
                        var5.add("the file upload " + var15.get("key") + " = " + var15.get("src"));
                     } else {
                        var4.add(
                           "And multipart field " + var15.get("key") + " = " + var3.d(var15.get("value") == null ? "" : String.valueOf(var15.get("value")))
                        );
                     }
                  }
               }
               break;
            case "file":
            case "binary":
               var5.add("the file body " + var0.D());
               break;
            case "graphql":
               String var8 = var0.E();
               if (var8 == null) {
                  return;
               }

               String var9 = var0.F();
               String var10 = "{\"query\":" + StringUtils.formatJsonString(var8) + ",\"variables\":" + (var9 != null && !var9.isBlank() ? var9 : "{}") + "}";
               String var11 = var3.e(var10);
               if (var11 == null) {
                  var5.add("the GraphQL variables " + var9.replace('\n', ' '));
               } else {
                  var4.add("And request " + var11);
               }
               break;
            default:
               var5.add("the " + var1 + " body");
         }
      }
   }

   static String a(String var0) {
      return var0 == null ? "unnamed" : var0.replaceAll("\\s+", " ").strip();
   }

   record RequestUrlParts(String a, List<String> b, List<String[]> c, Map<String, String> d) {

      static io.karatelabs.ext.postman.convert.PostmanRequestConverter.RequestUrlParts a(io.karatelabs.ext.postman.PostmanCollectionNode var0) {
         Object var1 = var0.b.get("request.url", null);
         Map var2 = var1 instanceof Map var3 ? var3 : Map.of();
         String var19 = var1 instanceof String var5 ? var5 : (var2.get("raw") instanceof String var4 ? var4 : "");
         int var20 = var19.indexOf(35);
         if (var20 >= 0 && !var19.startsWith("{{", var20 - 1)) {
            var19 = var19.substring(0, var20);
         }

         int var21 = var19.indexOf(63);
         String var22 = var21 < 0 ? null : var19.substring(var21 + 1);
         String var7 = var21 < 0 ? var19 : var19.substring(0, var21);
         ArrayList var9 = new ArrayList();
         List var10 = var2.get("path") != null ? var0.l() : null;
         List var11 = var2.get("host") != null ? var0.j() : null;
         String var8;
         if (var11 != null && !var11.isEmpty()) {
            String var12 = var2.get("protocol") instanceof String var13 ? var13 + "://" : "";
            String var26 = var2.get("port") == null ? "" : ":" + var2.get("port");
            var8 = var12 + String.join(".", var11) + var26;
         } else {
            var8 = a(var7);
         }

         if (var10 != null) {
            for (String var27 : (Iterable<String>) (Iterable<?>) (var10)) {
               if (var27 != null && !var27.isEmpty()) {
                  var9.add(var27);
               }
            }
         } else {
            String var24 = var7.substring(Math.min(var7.length(), a(var7).length()));

            for (String var16 : var24.split("/")) {
               if (!var16.isEmpty()) {
                  var9.add(var16);
               }
            }
         }

         ArrayList var25 = new ArrayList();
         Object var32 = var2.get("query");
         if (var32 instanceof List) {
            for (Object var36 : (List)var32) {
               if (var36 instanceof Map var40 && var40.get("key") != null && !Boolean.TRUE.equals(var40.get("disabled"))) {
                  var25.add(new String[]{String.valueOf(var40.get("key")), var40.get("value") == null ? "" : String.valueOf(var40.get("value"))});
               }
            }
         } else if (var22 != null) {
            for (String var17 : var22.split("&")) {
               if (!var17.isEmpty()) {
                  int var18 = var17.indexOf(61);
                  var25.add(var18 < 0 ? new String[]{var17, ""} : new String[]{var17.substring(0, var18), var17.substring(var18 + 1)});
               }
            }
         }

         LinkedHashMap var30 = new LinkedHashMap();
         Object var38 = var2.get("variable");
         if (var38 instanceof List) {
            for (Object var42 : (List)var38) {
               if (var42 instanceof Map var43 && var43.get("key") != null) {
                  var30.put(String.valueOf(var43.get("key")), var43.get("value") == null ? null : String.valueOf(var43.get("value")));
               }
            }
         }

         return new io.karatelabs.ext.postman.convert.PostmanRequestConverter.RequestUrlParts(var8, var9, var25, var30);
      }

      static String a(String var0) {
         int var1 = 0;
         if (var0.startsWith("{{")) {
            int var2 = var0.indexOf("}}");
            var1 = var2 < 0 ? var0.length() : var2 + 2;
         } else if (var0.contains("://")) {
            var1 = var0.indexOf("://") + 3;
         }

         int var3 = var0.indexOf(47, var1);
         return var3 < 0 ? var0 : var0.substring(0, var3);
      }
   }
}
