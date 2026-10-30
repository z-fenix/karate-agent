package io.karatelabs.ext.postman;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PostmanAuthConverter {
   private PostmanAuthConverter() {
   }

   public static boolean a(PostmanCollectionNode var0) {
      String var1 = var0.v();
      return var1 == null || "inherit".equals(var1);
   }

   public static Map<String, Object> a(Object var0) {
      if (var0 instanceof Map var1 && var1.get("scheme") != null) {
         String var2 = String.valueOf(var1.get("scheme")).toLowerCase(Locale.ROOT);
         ArrayList var3 = new ArrayList();
         switch (var2) {
            case "bearer":
               if (!a(var3, "token", var1.get("token"))) {
                  return null;
               }
               break;
            case "basic":
               if (!a(var3, "username", var1.get("username")) || !a(var3, "password", var1.get("password"))) {
                  return null;
               }
               break;
            case "apikey":
               Object var6 = var1.get("query");
               if (!a(var3, "key", var6 != null ? var6 : var1.get("header")) || !a(var3, "value", var1.get("key"))) {
                  return null;
               }

               a(var3, "in", var6 != null ? "query" : "header");
               break;
            case "oauth2":
               if (!a(var3, "accessTokenUrl", var1.get("tokenUrl"))
                  || !a(var3, "clientId", var1.get("clientId"))
                  || !a(var3, "clientSecret", var1.get("clientSecret"))) {
                  return null;
               }

               a(var3, "scope", var1.get("scope"));
               a(var3, "grant_type", "client_credentials");
               break;
            default:
               return null;
         }

         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("type", var2);
         var4.put(var2, var3);
         return var4;
      } else {
         return null;
      }
   }

   public static boolean a(PostmanCollectionNode var0, Map<String, Object> var1) {
      Map var2 = var1 != null && a(var0) ? a(var1.get("auth")) : null;
      if (var2 == null) {
         return false;
      } else {
         var0.b.set("auth", var2);
         return true;
      }
   }

   private static boolean a(List<Map<String, Object>> var0, String var1, Object var2) {
      if (var2 instanceof String var3 && !var3.isEmpty()) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("key", var1);
         var4.put("value", var3);
         var4.put("type", "string");
         var0.add(var4);
         return true;
      } else {
         return false;
      }
   }
}
