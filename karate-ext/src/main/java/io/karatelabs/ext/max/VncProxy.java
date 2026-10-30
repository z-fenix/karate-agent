package io.karatelabs.ext.max;

import io.karatelabs.common.ResourceType;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.WsClient;
import io.karatelabs.http.WsClientOptions;
import io.karatelabs.http.WsConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VncProxy {
   private static final Logger logger = LoggerFactory.getLogger(VncProxy.class);
   public static final String STATIC_PREFIX = "/novnc/";
   public static final String WS_PATH = "/novnc/websockify";
   private static final AtomicLong TUNNELS = new AtomicLong();
   private final Path root;
   private final String backendUrl;

   public VncProxy(Path var1, String var2) {
      this.root = var1.toAbsolutePath().normalize();
      this.backendUrl = var2;
   }

   public HttpResponse serveStatic(HttpRequest var1) {
      String var2 = var1.getPath().substring("/novnc/".length());
      if (var2.isEmpty()) {
         var2 = "vnc.html";
      }

      Path var3;
      try {
         var3 = MaxPaths.within(this.root, this.root.resolve(var2));
      } catch (SecurityException var7) {
         return HttpResponse.notFound("no novnc asset: " + var1.getPath());
      }

      if (!Files.isRegularFile(var3)) {
         return HttpResponse.notFound("no novnc asset: " + var1.getPath());
      } else {
         try {
            ResourceType var4 = ResourceType.fromFileExtension(var3.getFileName().toString());
            HttpResponse var5 = HttpResponse.ok();
            var5.setBody(Files.readAllBytes(var3), var4 != null ? var4 : ResourceType.BINARY);
            return var5;
         } catch (Exception var6) {
            logger.debug("novnc asset read failed for {}: {}", var1.getPath(), var6.getMessage());
            return HttpResponse.notFound("no novnc asset: " + var1.getPath());
         }
      }
   }

   public void tunnel(HttpRequest var1, WsConnection var2) {
      long var3 = TUNNELS.incrementAndGet();
      ExecutorService var5 = Executors.newSingleThreadExecutor(var2x -> {
         Thread var3x = new Thread(var2x, "vnc-tunnel-" + var3);
         var3x.setDaemon(true);
         return var3x;
      });

      WsClient var6;
      try {
         var6 = WsClient.connect(
            WsClientOptions.builder(this.backendUrl)
               .connectTimeout(Duration.ofSeconds(5L))
               .maxPayloadSize(16777216)
               .callbackExecutor(var5)
               .onMessage(var1x -> {
                  if (var1x.isBinary()) {
                     var2.sendBytes(var1x.getBytes());
                  } else if (var1x.isText()) {
                     var2.send(var1x.getText());
                  }
               })
               .onClose(() -> {
                  var2.close();
                  var5.shutdown();
               })
               .build()
         );
      } catch (Exception var8) {
         logger.warn("vnc tunnel {}: backend unreachable at {}: {}", new Object[]{var3, this.backendUrl, var8.getMessage()});
         var2.close(1011, "vnc backend unavailable");
         var5.shutdown();
         return;
      }

      var2.onBinary(var6::send);
      var2.onMessage(var6::send);
      var2.onClose(var6::close);
      logger.debug("vnc tunnel {} open: {} -> {}", new Object[]{var3, var1.getPath(), this.backendUrl});
   }
}
