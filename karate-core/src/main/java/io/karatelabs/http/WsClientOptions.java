package io.karatelabs.http;

import io.netty.handler.ssl.SslContext;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class WsClientOptions {
   private final URI uri;
   private final Map<String, String> headers;
   private final String subProtocol;
   private final boolean compression;
   private final int maxPayloadSize;
   private final Duration connectTimeout;
   private final Duration pingInterval;
   private final boolean trustAllCerts;
   private final SslContext sslContext;
   private final ExecutorService callbackExecutor;
   private final Consumer<WsFrame> messageListener;
   private final Runnable closeListener;
   private final Consumer<Throwable> errorListener;

   private WsClientOptions(WsClientOptions.Builder builder) {
      this.uri = builder.uri;
      this.headers = builder.headers != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers)) : Collections.emptyMap();
      this.subProtocol = builder.subProtocol;
      this.compression = builder.compression;
      this.maxPayloadSize = builder.maxPayloadSize;
      this.connectTimeout = builder.connectTimeout;
      this.pingInterval = builder.pingInterval;
      this.trustAllCerts = builder.trustAllCerts;
      this.sslContext = builder.sslContext;
      this.callbackExecutor = builder.callbackExecutor;
      this.messageListener = builder.messageListener;
      this.closeListener = builder.closeListener;
      this.errorListener = builder.errorListener;
   }

   public static WsClientOptions.Builder builder(String uri) {
      return new WsClientOptions.Builder(URI.create(uri));
   }

   public static WsClientOptions.Builder builder(URI uri) {
      return new WsClientOptions.Builder(uri);
   }

   public URI getUri() {
      return this.uri;
   }

   public String getHost() {
      return this.uri.getHost();
   }

   public int getPort() {
      int port = this.uri.getPort();
      if (port != -1) {
         return port;
      } else {
         return this.isSsl() ? 443 : 80;
      }
   }

   public boolean isSsl() {
      String scheme = this.uri.getScheme();
      return "wss".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
   }

   public Map<String, String> getHeaders() {
      return this.headers;
   }

   public String getSubProtocol() {
      return this.subProtocol;
   }

   public boolean isCompression() {
      return this.compression;
   }

   public int getMaxPayloadSize() {
      return this.maxPayloadSize;
   }

   public Duration getConnectTimeout() {
      return this.connectTimeout;
   }

   public Duration getPingInterval() {
      return this.pingInterval;
   }

   public boolean isTrustAllCerts() {
      return this.trustAllCerts;
   }

   public SslContext getSslContext() {
      return this.sslContext;
   }

   public ExecutorService getCallbackExecutor() {
      return this.callbackExecutor;
   }

   public Consumer<WsFrame> getMessageListener() {
      return this.messageListener;
   }

   public Runnable getCloseListener() {
      return this.closeListener;
   }

   public Consumer<Throwable> getErrorListener() {
      return this.errorListener;
   }

   public static class Builder {
      private final URI uri;
      private Map<String, String> headers;
      private String subProtocol;
      private boolean compression = false;
      private int maxPayloadSize = 1048576;
      private Duration connectTimeout = Duration.ofSeconds(30L);
      private Duration pingInterval = Duration.ofSeconds(30L);
      private boolean trustAllCerts = true;
      private SslContext sslContext;
      private ExecutorService callbackExecutor;
      private Consumer<WsFrame> messageListener;
      private Runnable closeListener;
      private Consumer<Throwable> errorListener;

      private Builder(URI uri) {
         if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
         } else {
            this.uri = uri;
         }
      }

      public WsClientOptions.Builder headers(Map<String, String> headers) {
         this.headers = headers;
         return this;
      }

      public WsClientOptions.Builder header(String name, String value) {
         if (this.headers == null) {
            this.headers = new LinkedHashMap<>();
         }

         this.headers.put(name, value);
         return this;
      }

      public WsClientOptions.Builder subProtocol(String subProtocol) {
         this.subProtocol = subProtocol;
         return this;
      }

      public WsClientOptions.Builder compression(boolean enabled) {
         this.compression = enabled;
         return this;
      }

      public WsClientOptions.Builder maxPayloadSize(int size) {
         this.maxPayloadSize = size;
         return this;
      }

      public WsClientOptions.Builder connectTimeout(Duration timeout) {
         this.connectTimeout = timeout;
         return this;
      }

      public WsClientOptions.Builder pingInterval(Duration interval) {
         this.pingInterval = interval;
         return this;
      }

      public WsClientOptions.Builder disablePing() {
         this.pingInterval = null;
         return this;
      }

      public WsClientOptions.Builder sslContext(SslContext context) {
         this.sslContext = context;
         return this;
      }

      public WsClientOptions.Builder trustAllCerts(boolean trust) {
         this.trustAllCerts = trust;
         return this;
      }

      public WsClientOptions.Builder callbackExecutor(ExecutorService executor) {
         this.callbackExecutor = executor;
         return this;
      }

      public WsClientOptions.Builder onMessage(Consumer<WsFrame> listener) {
         this.messageListener = listener;
         return this;
      }

      public WsClientOptions.Builder onClose(Runnable listener) {
         this.closeListener = listener;
         return this;
      }

      public WsClientOptions.Builder onError(Consumer<Throwable> listener) {
         this.errorListener = listener;
         return this;
      }

      public WsClientOptions build() {
         return new WsClientOptions(this);
      }
   }
}
