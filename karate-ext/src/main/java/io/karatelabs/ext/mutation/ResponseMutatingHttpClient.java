package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.core.KarateConfig;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class ResponseMutatingHttpClient implements HttpClient {
   private final HttpClient delegate;
   private final ResponseMutationSpec mutationSpec;
   private final io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher routeMatcher;
   private final io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationListener appliedListener;
   private int matchOrdinal;

   public ResponseMutatingHttpClient(HttpClient var1, ResponseMutationSpec var2, io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher var3, io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationListener var4) {
      this.delegate = var1;
      this.mutationSpec = var2;
      this.routeMatcher = var3;
      this.appliedListener = var4;
   }

   public HttpClient delegate() {
      return this.delegate;
   }

   @Override
   public HttpResponse invoke(HttpRequest var1) {
      HttpResponse var2 = this.delegate.invoke(var1);
      if (this.mutationSpec != null && var2 != null && !var2.isSkipped()) {
         String var3 = this.routeMatcher.of(var1.getMethod(), var1.getUrlAndPath());
         if (!this.mutationSpec.f().equals(var3)) {
            return var2;
         } else {
            int var4 = this.matchOrdinal++;
            if (this.mutationSpec.n() >= 0 && this.mutationSpec.n() != var4) {
               return var2;
            } else {
               if (applyMutation(var2, this.mutationSpec)) {
                  this.appliedListener.applied(new io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent(this.mutationSpec.d(), var1.getMethod(), io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(var1.getUrlAndPath()), var4));
               }

               return var2;
            }
         }
      } else {
         return var2;
      }
   }

   static boolean applyMutation(HttpResponse var0, ResponseMutationSpec var1) {
      String var2 = var1.e();
      switch (var2) {
         case "STATUS_SWAP":
         case "DECLARED_STATUS":
            if (var0.getStatus() == var1.h() && var0.getStatus() != var1.i()) {
               var0.setStatus(var1.i());
               return true;
            }

            return false;
         case "CONTENT_TYPE_TAMPER":
            byte[] var11 = var0.getBodyBytes();
            if (var11 != null && var11.length != 0 && !"text/plain".equals(var0.getContentType())) {
               var0.setContentType("text/plain");
               var0.setResourceType(ResourceType.TEXT);
               return true;
            }

            return false;
         case "MALFORMED_BODY":
            Object var10 = jsonResponseBody(var0);
            if (var10 == null) {
               return false;
            } else {
               byte[] var12 = var0.getBodyBytes();
               if (var12.length < 2) {
                  return false;
               }

               byte[] var13 = new byte[Math.max(1, var12.length / 2)];
               System.arraycopy(var12, 0, var13, 0, var13.length);
               var0.setBody(var13, null);
               return true;
            }
         default:
            if (!(jsonResponseBody(var0) instanceof Map var5)) {
               return false;
            } else if (!var5.containsKey(var1.j())) {
               return false;
            } else {
               Object var6 = var5.get(var1.j());
               String var7 = var1.e();
               switch (var7) {
                  case "FIELD_DROP":
                     var5.remove(var1.j());
                     break;
                  case "FIELD_NULL":
                     if (var6 == null) {
                        return false;
                     }

                     var5.put(var1.j(), null);
                     break;
                  case "TYPE_FLIP":
                     if (!(var6 instanceof Number) && !(var6 instanceof Boolean)) {
                        if (!(var6 instanceof String)) {
                           return false;
                        }

                        var5.put(var1.j(), 0);
                     } else {
                        var5.put(var1.j(), io.karatelabs.ext.mutation.ResponseMutationFactory.previewValue(var6));
                     }
                     break;
                  case "VALUE_OFF_BY_ONE":
                     if (!(var6 instanceof Number var14)) {
                        return false;
                     }

                     var5.put(var1.j(), !(var14 instanceof Integer) && !(var14 instanceof Long) ? var14.doubleValue() + 1.0 : var14.longValue() + 1L);
                     break;
                  case "CARDINALITY_EMPTY":
                     if (!(var6 instanceof List var9) || var9.isEmpty()) {
                        return false;
                     }

                     var5.put(var1.j(), List.of());
                     break;
                  case "ENUM_SUBSTITUTION":
                     if (var6 == null || var1.m() == null || var1.m().equals(var6)) {
                        return false;
                     }

                     var5.put(var1.j(), var1.m());
                     break;
                  default:
                     return false;
               }

               var0.setBody(Json.stringifyStrict(var5).getBytes(StandardCharsets.UTF_8), null);
               return true;
            }
      }
   }

   private static Object jsonResponseBody(HttpResponse var0) {
      String var1 = var0.getContentType();
      if (var1 != null && var1.toLowerCase().contains("json")) {
         byte[] var2 = var0.getBodyBytes();
         if (var2 != null && var2.length != 0) {
            try {
               return Json.parseStrict(new String(var2, StandardCharsets.UTF_8), true);
            } catch (RuntimeException var4) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public void apply(KarateConfig var1) {
      this.delegate.apply(var1);
   }

   @Override
   public void abort() {
      this.delegate.abort();
   }

   @Override
   public void close() throws IOException {
      this.delegate.close();
   }

   public record AppliedMutationEvent(String a, String b, String c, int d) {
   }

   @FunctionalInterface
   public interface RouteMatcher {
      String of(String var1, String var2);
   }

   @FunctionalInterface
   public interface AppliedMutationListener {
      void applied(io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent var1);
   }
}
