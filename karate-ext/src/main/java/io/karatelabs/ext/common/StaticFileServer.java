package io.karatelabs.ext.common;

import io.karatelabs.common.ResourceType;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StaticFileServer {
   private StaticFileServer() {
   }

   public static HttpServer a(Path var0, int var1) {
      Path var2 = var0.toAbsolutePath().normalize();
      return HttpServer.start(var1, var1x -> a(var2, var1x));
   }

   static HttpResponse a(Path var0, HttpRequest var1) {
      String var2 = var1.getPath();
      if (var2 == null || var2.isEmpty() || var2.equals("/")) {
         var2 = "/index.html";
      }

      int var3 = var2.indexOf(63);
      if (var3 >= 0) {
         var2 = var2.substring(0, var3);
      }

      HttpResponse var4 = new HttpResponse();

      Path var5;
      try {
         var5 = MaxPaths.resolveSandboxed(var0, var2);
      } catch (SecurityException var8) {
         var4.setStatus(404);
         var4.setBody("not found: " + var2, ResourceType.TEXT);
         return var4;
      }

      if (!Files.isRegularFile(var5)) {
         var4.setStatus(404);
         var4.setBody("not found: " + var2, ResourceType.TEXT);
         return var4;
      } else {
         try {
            ResourceType var6 = ResourceType.fromFileExtension(var5.getFileName().toString());
            var4.setStatus(200);
            var4.setBody(Files.readAllBytes(var5), var6 != null ? var6 : ResourceType.BINARY);
         } catch (Exception var7) {
            var4.setStatus(500);
            var4.setBody("error: " + var7.getMessage(), ResourceType.TEXT);
         }

         return var4;
      }
   }
}
