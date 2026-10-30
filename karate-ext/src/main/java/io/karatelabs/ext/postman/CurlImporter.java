package io.karatelabs.ext.postman;

import io.karatelabs.common.DataUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.http.HttpRequestBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurlImporter {
   private static final Logger a = LoggerFactory.getLogger(CurlImporter.class);

   private static String d(String var0) {
      String var1 = var0.replaceAll("\\^\\s*[\\r\\n]+\\s*", " ");
      return var1.replaceAll("\\^(.)", "$1");
   }

   public static HttpRequestBuilder a(String var0) {
      var0 = d(var0);
      CurlRequestOptions var1 = io.karatelabs.ext.postman.CurlRequestOptions.a(StringUtils.tokenizeCliCommand(var0));
      return b(var1);
   }

   public static PostmanCollectionNode b(String var0) {
      var0 = d(var0);
      String[] var1 = StringUtils.tokenizeCliCommand(var0);
      if (a.isTraceEnabled()) {
         a.trace("Tokenized curl command into {} tokens:", var1.length);

         for (int var2 = 0; var2 < var1.length; var2++) {
            a.trace("  [{}]: {}", var2, var1[var2]);
         }
      }

      CurlRequestOptions var4 = io.karatelabs.ext.postman.CurlRequestOptions.a(var1);
      return a(var4);
   }

   public static PostmanCollectionNode a(CurlRequestOptions var0) {
      HashMap var1 = new HashMap();
      var1.put("name", "Imported from cURL");
      var1.put("request", new HashMap());
      PostmanCollectionNode var2 = new PostmanCollectionNode(null, Json.of(var1));
      if (var0.a != null && !var0.a.isEmpty()) {
         var2.u(var0.a.getFirst());
      }

      String var3 = var0.c;
      boolean var4 = false;
      if (var0.g != null) {
         var0.e = var0.g;
         var4 = true;
      }

      if (var0.e != null && var3 == null) {
         var3 = "POST";
      }

      if (var0.f != null && var3 == null) {
         var3 = "POST";
      }

      if (var0.h != null && var3 == null) {
         var3 = "POST";
      }

      if (var3 == null) {
         var3 = "GET";
      }

      var2.d(var3);
      if (var0.d != null) {
         for (String var6 : var0.d) {
            int var7 = var6.indexOf(58);
            String var8;
            String var9;
            if (var7 == -1) {
               var8 = var6;
               var9 = null;
            } else {
               var8 = var6.substring(0, var7).trim();
               var9 = var6.substring(var7 + 1).trim();
            }

            var2.a(var8, var9);
         }
      }

      if (var0.i != null) {
         int var16 = var0.i.indexOf(58);
         String var19 = var0.i;
         String var22 = "";
         if (var16 != -1) {
            var19 = var0.i.substring(0, var16);
            var22 = var0.i.substring(var16 + 1);
         }

         var2.j("basic");
         ArrayList var27 = new ArrayList();
         HashMap var32 = new HashMap();
         var32.put("key", "username");
         var32.put("value", var19);
         var27.add(var32);
         HashMap var10 = new HashMap();
         var10.put("key", "password");
         var10.put("value", var22);
         var27.add(var10);
         var2.f(var27);
      }

      if (var0.j != null) {
         var2.a("User-Agent", var0.j);
      }

      if (var0.k != null) {
         var2.a("Cookie", var0.k);
      }

      if (var0.l != null) {
         var2.a("Referer", var0.l);
      }

      if (var0.h != null) {
         var2.l("formdata");
         ArrayList var17 = new ArrayList();

         for (String var23 : var0.h) {
            int var28 = var23.indexOf(61);
            if (var28 != -1) {
               String var33 = var23.substring(0, var28);
               String var36 = var23.substring(var28 + 1);
               HashMap var11 = new HashMap();
               var11.put("key", var33);
               var11.put("value", var36);
               var11.put("type", "text");
               var17.add(var11);
            }
         }

         var2.m(var17);
      } else if (var0.f != null) {
         var2.l("raw");
         var2.m(var0.f);
      } else if (var0.e != null) {
         boolean var18 = false;
         String var21 = null;

         for (String var29 : var0.e) {
            if (StringUtils.looksLikeJson(var29)) {
               var18 = true;
               var21 = var29;
               break;
            }
         }

         if (var18) {
            var2.l("raw");
            var2.m(var21);
         } else if (var4) {
            var2.l("urlencoded");
            ArrayList var25 = new ArrayList();

            for (String var34 : var0.e) {
               int var37 = var34.indexOf(61);
               if (var37 != -1) {
                  String var39 = var34.substring(0, var37);
                  String var12 = "";

                  try {
                     var12 = var34.substring(var37 + 1);
                  } catch (Exception var15) {
                     a.warn("failed to extract key: {} - {}", var39, var15.getMessage());
                  }

                  HashMap var13 = new HashMap();
                  var13.put("key", var39);
                  var13.put("value", var12);
                  var25.add(var13);
               }
            }

            var2.l(var25);
         } else {
            var2.l("raw");
            StringBuilder var26 = new StringBuilder();

            for (String var35 : var0.e) {
               int var38 = var35.indexOf(61);
               if (var38 != -1) {
                  if (!var26.isEmpty()) {
                     var26.append('&');
                  }

                  String var40 = var35.substring(0, var38);
                  String var41 = "";

                  try {
                     var41 = var35.substring(var38 + 1);
                  } catch (Exception var14) {
                     a.warn("failed to extract key: {} - {}", var40, var14.getMessage());
                  }

                  var26.append(var40).append('=').append(var41);
               } else {
                  var26.append(var35);
               }
            }

            var2.m(var26.toString());
         }
      }

      return var2;
   }

   public static HttpRequestBuilder b(CurlRequestOptions var0) {
      HttpRequestBuilder var1 = new HttpRequestBuilder(null);
      boolean var2 = false;
      if (var0.g != null) {
         var0.e = var0.g;
         var2 = true;
      }

      if (var0.e != null) {
         if (var0.c == null) {
            var0.c = "POST";
         }

         StringBuilder var3 = new StringBuilder();

         for (String var5 : var0.e) {
            if (StringUtils.looksLikeJson(var5)) {
               var1.bodyJson(var5);
            } else {
               int var6 = var5.indexOf(61);
               if (var6 != -1) {
                  String var7 = var5.substring(0, var6);
                  String var8 = "";

                  try {
                     var8 = var5.substring(var6 + 1);
                  } catch (Exception var10) {
                     a.warn("failed to extract key: {} - {}", var7, var10.getMessage());
                  }

                  if (var2) {
                     var1.formField(var7, var8);
                  } else {
                     if (!var3.isEmpty()) {
                        var3.append('&');
                     }

                     var3.append(var7).append('=').append(var8);
                  }
               } else {
                  var1.body(var5);
               }
            }
         }

         if (!var3.isEmpty()) {
            var1.body(var3.toString());
         }
      }

      if (var0.f != null) {
         if (var0.c == null) {
            var0.c = "POST";
         }

         var1.body(var0.f);
      }

      if (var0.d != null) {
         for (String var12 : var0.d) {
            int var13 = var12.indexOf(58);
            String var14;
            String var15;
            if (var13 == -1) {
               var14 = var12;
               var15 = null;
            } else {
               var14 = var12.substring(0, var13).trim();
               var15 = var12.substring(var13 + 1).trim();
            }

            var1.header(var14, var15);
         }
      }

      if (var0.a != null && !var0.a.isEmpty()) {
         var1.url(var0.a.getFirst());
      }

      var1.method(var0.c);
      return var1;
   }

   public static List<Map<String, Object>> c(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         Resource var1 = Resource.path(var0);
         String var2 = var0.toLowerCase();
         if (var2.endsWith(".csv")) {
            return DataUtils.fromCsv(var1.getText());
         } else if (var2.endsWith(".json")) {
            String var3 = var1.getText();
            if (var3 != null && !var3.trim().isEmpty()) {
               return (List<Map<String, Object>>)Json.parseLenient(var3);
            } else {
               a.warn("JSON file is empty: {}", var0);
               return null;
            }
         } else {
            a.warn("Unsupported file type: {}", var0);
            return null;
         }
      } else {
         return null;
      }
   }
}
