
package io.karatelabs.core;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;

import io.karatelabs.common.Resource;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import io.netty.handler.ssl.SslContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.slf4j.Logger;

public class MockServer implements SimpleObject {
   private static final Logger logger = LogContext.MOCK_LOGGER;
   private final HttpServer httpServer;
   private final MockHandler handler;
   private final int port;

   private MockServer(HttpServer httpServer, MockHandler handler, int port) {
      this.httpServer = httpServer;
      this.handler = handler;
      this.port = port;
   }

   public static MockServer.Builder feature(String path) {
      return new MockServer.Builder().feature(path);
   }

   public static MockServer.Builder feature(Feature feature) {
      return new MockServer.Builder().feature(feature);
   }

   public static MockServer.Builder feature(Resource resource) {
      return new MockServer.Builder().feature(Feature.read(resource));
   }

   public static MockServer.Builder featureString(String featureContent) {
      return new MockServer.Builder().featureString(featureContent);
   }

   public int getPort() {
      return this.port;
   }

   public boolean isSsl() {
      return this.httpServer.isSsl();
   }

   public String getUrl() {
      String protocol = this.isSsl() ? "https" : "http";
      return protocol + "://localhost:" + this.port;
   }

   public void stopAndWait() {
      logger.info("stopping mock server on port {}", this.port);
      if (this.httpServer.stopAndWait()) {
         this.handler.releaseHttpClients();
      } else {
         logger.warn("mock server on port {} stopped with a request still running: its clients are not released", this.port);
      }
   }

   public void stopAsync() {
      logger.info("stopping mock server async on port {}", this.port);
      this.httpServer.stopAsync();
   }

   public void waitSync() {
      this.httpServer.waitSync();
      this.handler.releaseHttpClients();
   }

   public Object getVariable(String name) {
      return this.handler.getVariableSnapshot(name);
   }

   public MockHandler getHandler() {
      return this.handler;
   }

   @Override
   public Object jsGet(String key) {
      return switch (key) {
         case "port" -> this.port;
         case "url" -> this.getUrl();
         case "ssl" -> this.isSsl();
         case "stop" -> (JavaCallable) (ctx, args) -> {
            this.stopAsync();
            return null;
         };
         case "get" -> (JavaInvokable) args -> {
            if (args.length == 0) {
               throw new RuntimeException("get() needs at least one argument: the mock variable name");
            } else {
               Object value = this.getVariable(args[0] + "");
               return value == null && args.length > 1 ? args[1] : value;
            }
         };
         default -> null;
      };
   }

   public static class Builder {
      private final List<Feature> features = new ArrayList<>();
      private int port = 0;
      private Map<String, Object> args;
      private String pathPrefix;
      private boolean ssl;
      private String certPath;
      private String keyPath;
      private SslContext customSslContext;
      private boolean watch;
      private boolean javaBridgeEnabled;
      private boolean requestExpressionsEnabled;
      private final HttpServer.Builder serverBuilder = HttpServer.builder();

      private Builder() {
      }

      public MockServer.Builder feature(String path) {
         this.features.add(Feature.read(path));
         return this;
      }

      public MockServer.Builder feature(Feature feature) {
         this.features.add(feature);
         return this;
      }

      public MockServer.Builder featureString(String featureContent) {
         this.features.add(Feature.read(Resource.text(featureContent)));
         return this;
      }

      public MockServer.Builder port(int port) {
         this.port = port;
         return this;
      }

      public MockServer.Builder arg(Map<String, Object> args) {
         this.args = args;
         return this;
      }

      public MockServer.Builder pathPrefix(String prefix) {
         this.pathPrefix = prefix;
         return this;
      }

      public MockServer.Builder ssl(boolean ssl) {
         this.ssl = ssl;
         return this;
      }

      public MockServer.Builder certPath(String certPath) {
         this.certPath = certPath;
         return this;
      }

      public MockServer.Builder keyPath(String keyPath) {
         this.keyPath = keyPath;
         return this;
      }

      public MockServer.Builder sslContext(SslContext sslContext) {
         this.customSslContext = sslContext;
         this.ssl = true;
         return this;
      }

      public MockServer.Builder watch(boolean watch) {
         this.watch = watch;
         return this;
      }

      public MockServer.Builder javaBridgeEnabled(boolean javaBridgeEnabled) {
         this.javaBridgeEnabled = javaBridgeEnabled;
         return this;
      }

      public MockServer.Builder requestExpressionsEnabled(boolean requestExpressionsEnabled) {
         this.requestExpressionsEnabled = requestExpressionsEnabled;
         return this;
      }

      public MockServer.Builder maxInitialLineLength(int bytes) {
         this.serverBuilder.maxInitialLineLength(bytes);
         return this;
      }

      public MockServer.Builder maxHeaderSize(int bytes) {
         this.serverBuilder.maxHeaderSize(bytes);
         return this;
      }

      public MockServer.Builder maxContentLength(int bytes) {
         this.serverBuilder.maxContentLength(bytes);
         return this;
      }

      public MockServer.Builder stopTimeout(Duration timeout) {
         this.serverBuilder.stopTimeout(timeout);
         return this;
      }

      public MockServer start() {
         if (this.features.isEmpty()) {
            throw new RuntimeException("at least one feature file is required");
         } else {
            Function<HttpRequest, HttpResponse> requestHandler;
            MockHandler handler;
            if (this.watch) {
               MockServer.ReloadingHandler reloadingHandler = new MockServer.ReloadingHandler(
                  this.features, this.args, this.pathPrefix, this.javaBridgeEnabled, this.requestExpressionsEnabled
               );
               handler = reloadingHandler.getHandler();
               requestHandler = reloadingHandler;
               MockServer.logger.info("watch mode enabled - features will be reloaded when modified");
            } else {
               handler = new MockHandler(this.features, this.args, this.pathPrefix, this.javaBridgeEnabled, this.requestExpressionsEnabled);
               requestHandler = handler;
            }

            SslContext sslContext = null;
            if (this.ssl) {
               if (this.customSslContext != null) {
                  sslContext = this.customSslContext;
               } else if (this.certPath != null && this.keyPath != null) {
                  sslContext = SslUtils.createNettySslContext(this.certPath, this.keyPath);
               } else {
                  sslContext = SslUtils.generateNettySslContext();
               }
            }

            HttpServer httpServer = this.serverBuilder.port(this.port).sslContext(sslContext).handler(requestHandler).start();
            int actualPort = httpServer.getPort();
            String protocol = this.ssl ? "https" : "http";
            MockServer.logger.info("mock server started on {}://localhost:{}", protocol, actualPort);
            return new MockServer(httpServer, handler, actualPort);
         }
      }
   }

   private static class ReloadingHandler implements Function<HttpRequest, HttpResponse> {
      private final Map<String, Object> args;
      private final String pathPrefix;
      private final boolean javaBridgeEnabled;
      private final boolean requestExpressionsEnabled;
      private final Map<Resource, Long> watchedFiles = new LinkedHashMap<>();
      private MockHandler handler;

      ReloadingHandler(List<Feature> features, Map<String, Object> args, String pathPrefix, boolean javaBridgeEnabled, boolean requestExpressionsEnabled) {
         this.args = args;
         this.pathPrefix = pathPrefix;
         this.javaBridgeEnabled = javaBridgeEnabled;
         this.requestExpressionsEnabled = requestExpressionsEnabled;

         for (Feature feature : features) {
            Resource resource = feature.getResource();
            if (resource.isLocalFile()) {
               this.watchedFiles.put(resource, resource.getLastModified());
               MockServer.logger.debug("watching file: {} (modified: {})", resource.getRelativePath(), resource.getLastModified());
            } else {
               MockServer.logger
                  .warn("watch mode: cannot watch non-file resource: {} (classpath resources in JARs are not watchable)", resource.getRelativePath());
            }
         }

         if (this.watchedFiles.isEmpty()) {
            MockServer.logger.warn("watch mode enabled but no watchable files found - features may be from classpath JARs");
         }

         this.handler = new MockHandler(features, args, pathPrefix, javaBridgeEnabled, requestExpressionsEnabled);
      }

      MockHandler getHandler() {
         return this.handler;
      }

      public HttpResponse apply(HttpRequest request) {
         boolean needsReload = false;

         for (Entry<Resource, Long> entry : this.watchedFiles.entrySet()) {
            Resource resource = entry.getKey();
            long lastKnown = entry.getValue();
            long current = resource.getLastModified();
            if (current > lastKnown) {
               MockServer.logger.info("file modified, reloading: {} (was: {}, now: {})", new Object[]{resource.getRelativePath(), lastKnown, current});
               needsReload = true;
               break;
            }
         }

         if (needsReload) {
            List<Feature> reloadedFeatures = new ArrayList<>();
            Map<Resource, Long> newWatchedFiles = new LinkedHashMap<>();

            for (Resource oldResource : this.watchedFiles.keySet()) {
               Resource freshResource = Resource.from(oldResource.getPath());
               Feature feature = Feature.read(freshResource);
               reloadedFeatures.add(feature);
               newWatchedFiles.put(freshResource, freshResource.getLastModified());
            }

            this.watchedFiles.clear();
            this.watchedFiles.putAll(newWatchedFiles);
            this.handler = new MockHandler(reloadedFeatures, this.args, this.pathPrefix, this.javaBridgeEnabled, this.requestExpressionsEnabled);
            MockServer.logger.info("reloaded {} feature(s)", reloadedFeatures.size());
         }

         return this.handler.apply(request);
      }
   }
}
