package io.karatelabs.ext.postman;

import io.karatelabs.common.StringUtils;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import java.util.Collection;
import java.util.List;

public class PmResponse implements SimpleObject {
   final PmRuntime runtime;
   final HttpResponse response;
   final SimpleObject responseHeaders;
   final SimpleObject responseTo;
   final SimpleObject responseToBe;
   final SimpleObject responseToHave;
   final SimpleObject responseTimeAssertions;

   PmResponse(PmRuntime var1, HttpResponse var2) {
      this.runtime = var1;
      this.response = var2;
      this.responseTimeAssertions = PmRuntime.object("pm.response.to.have.responseTime", var2x -> {
         return switch (var2x) {
            case "below" -> (io.karatelabs.js.JavaCallable) (var2xx, var3) -> {
               Number var4 = (Number)var3[0];
               long var5 = var2.getResponseTime();
               Result var7;
               if (var5 < var4.longValue()) {
                  var7 = Result.PASS;
               } else {
                  var7 = Result.fail("actual: " + var5 + "ms, expected: below " + var4 + "ms");
               }

               var1.onMatch.accept(var2xx, var7);
               return null;
            };
            case "above" -> (io.karatelabs.js.JavaCallable) (var2xx, var3) -> {
               Number var4 = (Number)var3[0];
               long var5 = var2.getResponseTime();
               Result var7;
               if (var5 > var4.longValue()) {
                  var7 = Result.PASS;
               } else {
                  var7 = Result.fail("actual: " + var5 + "ms, expected: above " + var4 + "ms");
               }

               var1.onMatch.accept(var2xx, var7);
               return null;
            };
            case "within" -> (io.karatelabs.js.JavaCallable) (var2xx, var3) -> {
               Number var4 = (Number)var3[0];
               Number var5 = (Number)var3[1];
               long var6 = var2.getResponseTime();
               Result var8;
               if (var6 >= var4.longValue() && var6 <= var5.longValue()) {
                  var8 = Result.PASS;
               } else {
                  var8 = Result.fail("actual: " + var6 + "ms, expected: within " + var4 + "ms to " + var5 + "ms");
               }

               var1.onMatch.accept(var2xx, var8);
               return null;
            };
            default -> throw new UnsupportedPostmanFeatureException("pm.response.to.have.responseTime." + var2x);
         };
      });
      this.responseToHave = PmRuntime.object("pm.response.to.have", var3 -> {
         return switch (var3) {
            case "status" -> (io.karatelabs.js.JavaCallable) (var2xx, var3x) -> {
               Match.evaluate(var2.getStatus(), var2xx, var1.onMatch)._equals(var3x[0]);
               return null;
            };
            case "header" -> (io.karatelabs.js.JavaCallable) (var2xx, var3x) -> {
               String var4 = var3x[0] + "";
               String var5 = var3x[1] + "";
               var var6 = var2.getHeaderValues(var4);
               Match.evaluate(var6, var2xx, var1.onMatch).contains(var5);
               return null;
            };
            case "body" -> (io.karatelabs.js.JavaCallable) (var2xx, var3x) -> {
               String var4 = var2.getBodyString();
               Match.evaluate(var4, var2xx, var1.onMatch)._equals(var3x[0]);
               return null;
            };
            case "jsonBody" -> (io.karatelabs.js.JavaCallable) (var2xx, var3x) -> {
               Object var4 = var2.getBodyConverted();
               Match.evaluate(var4, var2xx, var1.onMatch)._equals(var3x[0]);
               return null;
            };
            case "responseTime" -> this.responseTimeAssertions;
            default -> throw new UnsupportedPostmanFeatureException("pm.response.to.have." + var3);
         };
      });
      this.responseToBe = PmRuntime.object("pm.response.to.be", var2x -> this.matchStatus(var2.getStatus(), var2x));
      this.responseTo = PmRuntime.object("pm.response.to", var1x -> {
         return switch (var1x) {
            case "have" -> this.responseToHave;
            case "be" -> this.responseToBe;
            default -> throw new UnsupportedPostmanFeatureException("pm.response.to." + var1x);
         };
      });
      this.responseHeaders = PmRuntime.object("pm.response.headers", var1x -> {
         return switch (var1x) {
            case "get" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> var2.getHeader(var2x[0] + "");
            case "has" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> var2.getHeader(var2x[0] + "") != null;
            case "toObject" -> (io.karatelabs.js.JavaCallable) (var1xx, var2x) -> StringUtils.simplify(var2.getHeaders(), false);
            default -> throw new UnsupportedPostmanFeatureException("pm.response.headers." + var1x);
         };
      });
   }

   private Void matchStatus(int var1, String var2) {
      String var3 = switch (var2) {
         case "ok", "success" -> var1 >= 200 && var1 < 300 ? null : "actual: " + var1 + ", expected: 2XX";
         case "info" -> var1 >= 200 ? "actual: " + var1 + ", expected: 1XX" : null;
         case "redirection" -> var1 >= 300 && var1 < 400 ? null : "actual: " + var1 + ", expected: 3XX";
         case "clientError" -> var1 >= 400 && var1 < 500 ? null : "actual: " + var1 + ", expected: 4XX";
         case "serverError" -> var1 < 500 ? "actual: " + var1 + ", expected: 5XX" : null;
         case "error" -> var1 < 400 ? "actual: " + var1 + ", expected: 4XX or 5XX" : null;
         case "accepted" -> var1 != 202 ? "actual: " + var1 + ", expected: 202" : null;
         case "badRequest" -> var1 != 400 ? "actual: " + var1 + ", expected: 400" : null;
         default -> throw new UnsupportedPostmanFeatureException("pm.response.to.be." + var2);
      };
      Result var4 = var3 == null ? Result.PASS : Result.fail(var3);
      this.runtime.onMatch.accept(this.runtime.karate.engine.getRootContext(), var4);
      return null;
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "code" -> this.response.getStatus();
         case "status" -> this.response.getStatusText();
         case "responseTime" -> this.response.getResponseTime();
         case "responseSize" -> this.response.getContentLength();
         case "headers" -> this.responseHeaders;
         case "to" -> this.responseTo;
         case "text" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.response.getBodyString();
         case "json" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.response.getBodyConverted();
         default -> throw new UnsupportedPostmanFeatureException("pm.response." + var1);
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("code", "status", "responseTime", "responseSize");
   }

   @Override
   public void putMember(String var1, Object var2) {
      throw new UnsupportedPostmanFeatureException("pm.response." + var1 + " =");
   }
}
