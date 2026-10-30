package io.karatelabs.http;

import io.karatelabs.common.KarateLifecycle;
import io.karatelabs.common.Stoppable;
import io.karatelabs.common.ThreadUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer implements Stoppable {
   static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
   public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
   public static final int DEFAULT_MAX_HEADER_SIZE = 8192;
   public static final int DEFAULT_MAX_CONTENT_LENGTH = 1048576;
   private final EventLoopGroup bossGroup;
   private final EventLoopGroup workerGroup;
   private final Channel channel;
   private final int port;
   private final SslContext sslContext;
   final Function<HttpRequest, HttpResponse> handler;
   final SseHandler sseHandler;
   final WsHandler wsHandler;
   final ExecutorService handlers = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("http-handler-", 0L).factory());
   private final long stopTimeoutMillis;

   private static List<HttpServer> active() {
      return KarateLifecycle.running().stream().filter(HttpServer.class::isInstance).map(HttpServer.class::cast).toList();
   }

   public static void shutdownAll() {
      List<HttpServer> servers = active();
      if (!servers.isEmpty()) {
         logger.info("shutting down {} active server(s)", servers.size());

         for (HttpServer server : servers) {
            server.stopAsync();
         }
      }
   }

   public static HttpServer start(int port, Function<HttpRequest, HttpResponse> handler) {
      return builder().port(port).handler(handler).start();
   }

   public static HttpServer start(int port, Function<HttpRequest, HttpResponse> handler, SseHandler sseHandler) {
      return builder().port(port).handler(handler).sseHandler(sseHandler).start();
   }

   public static HttpServer start(int port, SslContext sslContext, Function<HttpRequest, HttpResponse> handler) {
      return builder().port(port).sslContext(sslContext).handler(handler).start();
   }

   public static HttpServer start(int port, SslContext sslContext, Function<HttpRequest, HttpResponse> handler, SseHandler sseHandler) {
      return builder().port(port).sslContext(sslContext).handler(handler).sseHandler(sseHandler).start();
   }

   public static HttpServer start(int port, Function<HttpRequest, HttpResponse> handler, SseHandler sseHandler, WsHandler wsHandler) {
      return builder().port(port).handler(handler).sseHandler(sseHandler).wsHandler(wsHandler).start();
   }

   public static HttpServer start(int port, SslContext sslContext, Function<HttpRequest, HttpResponse> handler, SseHandler sseHandler, WsHandler wsHandler) {
      return builder().port(port).sslContext(sslContext).handler(handler).sseHandler(sseHandler).wsHandler(wsHandler).start();
   }

   public static HttpServer start(String host, int port, Function<HttpRequest, HttpResponse> handler, SseHandler sseHandler, WsHandler wsHandler) {
      return builder().host(host).port(port).handler(handler).sseHandler(sseHandler).wsHandler(wsHandler).start();
   }

   public static HttpServer.Builder builder() {
      return new HttpServer.Builder();
   }

   private static int positive(String name, int bytes) {
      if (bytes <= 0) {
         throw new IllegalArgumentException(name + " must be a positive number of bytes, was: " + bytes);
      } else {
         return bytes;
      }
   }

   public boolean isSsl() {
      return this.sslContext != null;
   }

   public int getPort() {
      return this.port;
   }

   public InetSocketAddress getLocalAddress() {
      return (InetSocketAddress)this.channel.localAddress();
   }

   public void waitSync() {
      try {
         this.channel.closeFuture().sync();
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   @Override
   public String lifecycleName() {
      return "http-server:" + this.port;
   }

   @Override
   public String lifecycleKind() {
      return "http-server";
   }

   @Override
   public void stop() {
      this.stopAndWait();
   }

   public boolean stopAndWait() {
      this.stopAsync();

      try {
         this.channel.closeFuture().sync();
         this.bossGroup.terminationFuture().sync();
         this.workerGroup.terminationFuture().sync();
         if (!this.handlers.awaitTermination(this.stopTimeoutMillis, TimeUnit.MILLISECONDS)) {
            this.handlers.shutdownNow();
            if (!this.handlers.awaitTermination(this.stopTimeoutMillis, TimeUnit.MILLISECONDS)) {
               logger.warn("stop: a request handler on port {} is still running after being interrupted", this.port);
               return false;
            }
         }
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
         throw new RuntimeException("Interrupted while shutting down HTTP server", var2);
      }

      logger.debug("stop: shutdown complete");
      return true;
   }

   public void stopAsync() {
      KarateLifecycle.unregister(this);
      logger.debug("stop: shutting down");
      this.channel.close();
      this.bossGroup.shutdownGracefully(0L, this.stopTimeoutMillis, TimeUnit.MILLISECONDS);
      this.workerGroup.shutdownGracefully(0L, this.stopTimeoutMillis, TimeUnit.MILLISECONDS);
      this.handlers.shutdown();
   }

   private HttpServer(HttpServer.Builder b) {
      String host = b.host;
      int requestedPort = b.port;
      this.handler = b.handler;
      this.sseHandler = b.sseHandler;
      this.wsHandler = b.wsHandler;
      this.sslContext = b.sslContext;
      this.stopTimeoutMillis = b.stopTimeoutMillis;
      final HttpDecoderConfig decoderConfig = new HttpDecoderConfig().setMaxInitialLineLength(b.maxInitialLineLength).setMaxHeaderSize(b.maxHeaderSize);
      final int maxContentLength = b.maxContentLength;
      this.bossGroup = new MultiThreadIoEventLoopGroup(1, ThreadUtils.daemonFactory("http-boss-"), NioIoHandler.newFactory());
      this.workerGroup = new MultiThreadIoEventLoopGroup(b.workerThreads, ThreadUtils.daemonFactory("http-worker-"), NioIoHandler.newFactory());
      final CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
         .allowNullOrigin()
         .allowedRequestHeaders(HttpUtils.Header.keys())
         .allowedRequestMethods(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD})
         .build();

      try {
         ServerBootstrap bootstrap = new ServerBootstrap();
         ((ServerBootstrap)bootstrap.group(this.bossGroup, this.workerGroup).channel(NioServerSocketChannel.class))
            .childHandler(new ChannelInitializer<Channel>() {
               protected void initChannel(Channel c) {
                  ChannelPipeline p = c.pipeline();
                  if (HttpServer.this.sslContext != null) {
                     p.addLast(new ChannelHandler[]{HttpServer.this.sslContext.newHandler(c.alloc())});
                  }

                  p.addLast(new ChannelHandler[]{new HttpServerCodec(decoderConfig)});
                  p.addLast(new ChannelHandler[]{new DecoderFailureHandler()});
                  p.addLast(new ChannelHandler[]{new HttpObjectAggregator(maxContentLength)});
                  p.addLast(new ChannelHandler[]{new CorsHandler(corsConfig)});
                  p.addLast(new ChannelHandler[]{new HttpServerHandler(HttpServer.this)});
               }
            });
         InetSocketAddress bindAddress = host != null && !host.isBlank() && !"0.0.0.0".equals(host)
            ? new InetSocketAddress(host, requestedPort)
            : new InetSocketAddress(requestedPort);
         this.channel = bootstrap.bind(bindAddress).sync().channel();
         InetSocketAddress isa = (InetSocketAddress)this.channel.localAddress();
         this.port = isa.getPort();
         KarateLifecycle.register(this);
         String protocol = this.sslContext != null ? "https" : "http";
         logger.debug("{} server started on {}:{}", new Object[]{protocol, isa.getHostString(), this.port});
      } catch (Exception var11) {
         throw new RuntimeException(var11);
      }
   }

   public static class Builder {
      private String host;
      private int port;
      private SslContext sslContext;
      private Function<HttpRequest, HttpResponse> handler;
      private SseHandler sseHandler;
      private WsHandler wsHandler;
      private int maxInitialLineLength = 4096;
      private int maxHeaderSize = 8192;
      private int maxContentLength = 1048576;
      private int workerThreads;
      private long stopTimeoutMillis = 15000L;

      private Builder() {
      }

      public HttpServer.Builder workerThreads(int threads) {
         this.workerThreads = threads;
         return this;
      }

      public HttpServer.Builder stopTimeout(Duration timeout) {
         this.stopTimeoutMillis = timeout.toMillis();
         return this;
      }

      public HttpServer.Builder host(String host) {
         this.host = host;
         return this;
      }

      public HttpServer.Builder port(int port) {
         this.port = port;
         return this;
      }

      public HttpServer.Builder sslContext(SslContext sslContext) {
         this.sslContext = sslContext;
         return this;
      }

      public HttpServer.Builder handler(Function<HttpRequest, HttpResponse> handler) {
         this.handler = handler;
         return this;
      }

      public HttpServer.Builder sseHandler(SseHandler sseHandler) {
         this.sseHandler = sseHandler;
         return this;
      }

      public HttpServer.Builder wsHandler(WsHandler wsHandler) {
         this.wsHandler = wsHandler;
         return this;
      }

      public HttpServer.Builder maxInitialLineLength(int bytes) {
         this.maxInitialLineLength = HttpServer.positive("maxInitialLineLength", bytes);
         return this;
      }

      public HttpServer.Builder maxHeaderSize(int bytes) {
         this.maxHeaderSize = HttpServer.positive("maxHeaderSize", bytes);
         return this;
      }

      public HttpServer.Builder maxContentLength(int bytes) {
         this.maxContentLength = HttpServer.positive("maxContentLength", bytes);
         return this;
      }

      public HttpServer start() {
         return new HttpServer(this);
      }
   }
}
