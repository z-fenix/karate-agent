package io.karatelabs.ext.websocket;

import io.karatelabs.http.WsClientOptions;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import java.net.ConnectException;
import javax.net.ssl.SSLException;

final class WebsocketConnectionDiagnostics {
   private WebsocketConnectionDiagnostics() {
   }

   static String a(WsClientOptions var0, Throwable var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append("websocket connect failed");
      if (var1 != null && var1.getMessage() != null) {
         var2.append(": ").append(var1.getMessage());
      }

      var2.append("\n  uri = ").append(var0.getUri());
      var2.append("\n  host:port = ").append(var0.getHost()).append(':').append(var0.getPort());
      var2.append("\n  ssl = ").append(var0.isSsl());
      if (var0.getSubProtocol() != null) {
         var2.append("\n  subProtocol = ").append(var0.getSubProtocol());
      }

      if (var0.getHeaders() != null && !var0.getHeaders().isEmpty()) {
         var2.append("\n  headers (keys) = ").append(var0.getHeaders().keySet());
      }

      Throwable var3 = a(var1);
      if (a(var1, WebSocketHandshakeException.class)) {
         var2.append("\nhint: TCP connected but the server did not return HTTP 101 Switching Protocols.");
         var2.append("\n  - 4xx usually means wrong path, wrong subProtocol, or missing/invalid auth headers");
         var2.append("\n  - check the URI path and any required headers (Authorization, etc.)");
      } else if (!(var3 instanceof ConnectException)
         && (var1 == null || var1.getMessage() == null || !var1.getMessage().toLowerCase().contains("connection refused"))) {
         if (a(var1, SSLException.class)) {
            var2.append("\nhint: TLS handshake failed.");
            var2.append("\n  - if the server is plain ws:// (no TLS), use ws:// not wss://");
            var2.append("\n  - re-run with -Djavax.net.debug=ssl,handshake for the exact TLS failure reason");
         }
      } else {
         var2.append("\nhint: TCP connection refused — nothing is listening on ").append(var0.getHost()).append(':').append(var0.getPort());
         var2.append("\n  - check the URI scheme (ws:// vs wss://) and port");
         var2.append("\n  - confirm the server is running and reachable");
      }

      return var2.toString();
   }

   private static boolean a(Throwable var0, Class<? extends Throwable> var1) {
      for (Throwable var2 = var0; var2 != null; var2 = var2.getCause()) {
         if (var1.isInstance(var2)) {
            return true;
         }
      }

      return false;
   }

   private static Throwable a(Throwable var0) {
      Throwable var1 = var0;

      while (var1 != null && var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1;
   }
}
