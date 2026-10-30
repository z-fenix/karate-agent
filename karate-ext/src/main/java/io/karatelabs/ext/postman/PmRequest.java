package io.karatelabs.ext.postman;

import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PmRequest implements SimpleObject {
   final PmRuntime runtime;
   final PostmanCollectionNode item;
   final SimpleObject requestUrl;
   final SimpleObject requestHeaders;
   final SimpleObject requestBody;

   public PmRequest(PmRuntime var1, PostmanCollectionNode var2) {
      this.runtime = var1;
      this.item = var2;
      this.requestUrl = PmRuntime.object("pm.request.url", var2x -> {
         return switch (var2x) {
            case "protocol" -> var2.m();
            case "host" -> var2.j();
            case "port" -> var2.k();
            case "path" -> var2.l();
            case "query" -> var2.n();
            case "raw" -> var2.h();
            case "variable" -> var2.o();
            case "toString" -> (io.karatelabs.js.JavaCallable) (var2xx, var3) -> var2.a(var1._variables);
            default -> throw new UnsupportedPostmanFeatureException("pm.request.url." + var2x);
         };
      });
      this.requestHeaders = PmRuntime.object("pm.request.headers", var1x -> {
         return switch (var1x) {
            case "add" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> {
               if (var2x[0] instanceof Map var3) {
                  String var4 = (String)var3.get("key");
                  Object var5 = var3.get("value");
                  var2.a(var4, var5 + "");
               }

               return null;
            };
            case "get" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> var2.g(var2x[0] + "");
            case "has" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> var2.h(var2x[0] + "");
            case "remove" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> {
               var2.i(var2x[0] + "");
               return null;
            };
            case "upsert" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> {
               var2.b(var2x[0] + "", var2x[1] + "");
               return null;
            };
            case "toObject" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> var2.p();
            default -> throw new UnsupportedPostmanFeatureException("pm.request.headers." + var1x);
         };
      });
      this.requestBody = PmRuntime.object("pm.request.body", var1x -> {
         return switch (var1x) {
            case "mode" -> var2.z();
            case "raw" -> var2.A();
            case "urlencoded" -> var2.B();
            case "formdata" -> var2.C();
            default -> throw new UnsupportedPostmanFeatureException("pm.request.body." + var1x);
         };
      });
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "url" -> this.requestUrl;
         case "method" -> this.item.g();
         case "headers" -> this.requestHeaders;
         case "body" -> this.requestBody;
         default -> throw new UnsupportedPostmanFeatureException("pm.request." + var1);
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("method", "url");
   }

   @Override
   public void putMember(String var1, Object var2) {
      throw new UnsupportedPostmanFeatureException("pm.request." + var1 + " =");
   }
}
