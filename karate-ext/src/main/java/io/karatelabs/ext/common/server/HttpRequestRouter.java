package io.karatelabs.ext.common.server;

import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class HttpRequestRouter {
   private final List<io.karatelabs.ext.common.server.HttpRequestRouter.HttpRoute> a = new ArrayList<>();

   public HttpRequestRouter a(String var1, String var2, Function<HttpRequest, HttpResponse> var3) {
      this.a.add(new io.karatelabs.ext.common.server.HttpRequestRouter.HttpRoute(var1, var2, false, var3));
      return this;
   }

   public HttpRequestRouter b(String var1, String var2, Function<HttpRequest, HttpResponse> var3) {
      this.a.add(new io.karatelabs.ext.common.server.HttpRequestRouter.HttpRoute(var1, var2, true, var3));
      return this;
   }

   public HttpRequestRouter a(String var1, int var2) {
      this.a.add(new io.karatelabs.ext.common.server.HttpRequestRouter.HttpRoute(null, var1, true, var1x -> {
         if (var2 == 404) {
            return HttpResponse.notFound("no route: " + var1x.getMethod() + " " + var1x.getPath());
         } else {
            HttpResponse var2x = new HttpResponse();
            var2x.setStatus(var2);
            return var2x;
         }
      }));
      return this;
   }

   public Optional<HttpResponse> a(HttpRequest var1) {
      String var2 = var1.getMethod();
      String var3 = var1.getPath();

      for (io.karatelabs.ext.common.server.HttpRequestRouter.HttpRoute var5 : this.a) {
         if (var5.a(var2, var3)) {
            return Optional.of(var5.d().apply(var1));
         }
      }

      return Optional.empty();
   }

   private record HttpRoute(String a, String b, boolean c, Function<HttpRequest, HttpResponse> d) {


      boolean a(String var1, String var2) {
         if (this.a != null && !this.a.equals(var1)) {
            return false;
         } else {
            return this.c ? var2.startsWith(this.b) : var2.equals(this.b);
         }
      }
   }
}
