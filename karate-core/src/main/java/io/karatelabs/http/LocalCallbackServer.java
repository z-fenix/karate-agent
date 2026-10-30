package io.karatelabs.http;

import io.karatelabs.common.ResourceType;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCallbackServer {
   private static final Logger logger = LoggerFactory.getLogger(LocalCallbackServer.class);
   private HttpServer server;
   private CompletableFuture<String> codeFuture;
   private int port;

   public String start() {
      return this.start(0);
   }

   public String start(int preferredPort) {
      this.codeFuture = new CompletableFuture<>();
      this.server = HttpServer.start(preferredPort, this::handleRequest);
      this.port = this.server.getPort();
      logger.debug("OAuth callback server started on port {}", this.port);
      return "http://127.0.0.1:" + this.port + "/callback";
   }

   private HttpResponse handleRequest(HttpRequest request) {
      if (!"GET".equalsIgnoreCase(request.getMethod())) {
         return HttpResponse.text(405, "Method Not Allowed");
      } else if (!"/callback".equals(request.getPath())) {
         return HttpResponse.text(404, "Not Found");
      } else {
         String code = request.getParam("code");
         String error = request.getParam("error");
         String state = request.getParam("state");
         logger.debug("Received OAuth callback: code={}, error={}, state={}", new Object[]{code != null ? "present" : "null", error, state});
         HttpResponse response = new HttpResponse();
         if (code != null) {
            this.codeFuture.complete(code);
            this.sendSuccessPage(response);
         } else {
            String errorDesc = request.getParam("error_description");
            if (errorDesc == null) {
               errorDesc = "Unknown error";
            }

            this.codeFuture.completeExceptionally(new OAuth2Exception(error + ": " + errorDesc));
            this.sendErrorPage(response, error, errorDesc);
         }

         return response;
      }
   }

   private void sendSuccessPage(HttpResponse response) {
      String html = "<!DOCTYPE html>\n<html>\n<head>\n    <title>Authorization Successful</title>\n    <style>\n        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }\n        h1 { color: #4caf50; }\n        p { color: #666; }\n    </style>\n</head>\n<body>\n    <h1>✓ Authorization Successful</h1>\n    <p>You can close this window and return to the application.</p>\n</body>\n</html>\n";
      response.setStatus(200);
      response.setBody(html, ResourceType.HTML);
   }

   private void sendErrorPage(HttpResponse response, String error, String description) {
      String html = String.format(
         "<!DOCTYPE html>\n<html>\n<head>\n    <title>Authorization Failed</title>\n    <style>\n        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }\n        h1 { color: #f44336; }\n        p { color: #666; }\n        code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; }\n    </style>\n</head>\n<body>\n    <h1>✗ Authorization Failed</h1>\n    <p><strong>Error:</strong> <code>%s</code></p>\n    <p>%s</p>\n</body>\n</html>\n",
         error,
         description
      );
      response.setStatus(400);
      response.setBody(html, ResourceType.HTML);
   }

   public CompletableFuture<String> getCodeFuture() {
      return this.codeFuture;
   }

   public int getPort() {
      return this.port;
   }

   public void stop() {
      this.stopAndWait();
   }

   public void stopAsync() {
      if (this.server != null) {
         logger.debug("Stopping OAuth callback server on port {}", this.port);
         this.server.stopAsync();
      }
   }

   public void stopAndWait() {
      if (this.server != null) {
         logger.debug("Stopping OAuth callback server on port {}", this.port);
         this.server.stopAndWait();
      }
   }
}
