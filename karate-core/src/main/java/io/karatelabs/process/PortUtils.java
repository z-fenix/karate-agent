package io.karatelabs.process;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PortUtils {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");

   private PortUtils() {
   }

   public static boolean waitForPort(String host, int port, int attempts, int intervalMs, BooleanSupplier stillAlive) {
      for (int i = 0; i < attempts; i++) {
         if (stillAlive != null && !stillAlive.getAsBoolean()) {
            logger.warn("process died while waiting for port {}:{}", host, port);
            return false;
         }

         try {
            boolean var7;
            try (Socket socket = new Socket()) {
               socket.connect(new InetSocketAddress(host, port), 1000);
               logger.debug("port {}:{} is available after {} attempts", new Object[]{host, port, i + 1});
               var7 = true;
            }

            return var7;
         } catch (Exception var11) {
            logger.trace("port {}:{} not yet available (attempt {})", new Object[]{host, port, i + 1});
            sleep(intervalMs);
         }
      }

      logger.warn("port {}:{} not available after {} attempts", new Object[]{host, port, attempts});
      return false;
   }

   public static boolean waitForHttp(String url, int attempts, int intervalMs, BooleanSupplier stillAlive) {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();

      for (int i = 0; i < attempts; i++) {
         if (stillAlive != null && !stillAlive.getAsBoolean()) {
            logger.warn("process died while waiting for HTTP {}", url);
            return false;
         }

         try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
               logger.debug("HTTP {} is available after {} attempts", url, i + 1);
               return true;
            }

            logger.trace("HTTP {} returned {} (attempt {})", new Object[]{url, response.statusCode(), i + 1});
         } catch (Exception var8) {
            logger.trace("HTTP {} not available (attempt {}): {}", new Object[]{url, i + 1, var8.getMessage()});
         }

         sleep(intervalMs);
      }

      logger.warn("HTTP {} not available after {} attempts", url, attempts);
      return false;
   }

   public static int findFreePort() {
      try {
         int var1;
         try (ServerSocket socket = new ServerSocket(0)) {
            var1 = socket.getLocalPort();
         }

         return var1;
      } catch (Exception var5) {
         throw new RuntimeException("failed to find free port", var5);
      }
   }

   private static void sleep(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }
   }
}
