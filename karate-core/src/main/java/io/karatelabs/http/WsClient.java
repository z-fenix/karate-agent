package io.karatelabs.http;

import io.karatelabs.common.KarateLifecycle;
import io.karatelabs.common.Stoppable;
import io.karatelabs.common.ThreadUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsClient implements Stoppable {
   private static final Logger logger = LoggerFactory.getLogger(WsClient.class);
   private static final ExecutorService CALLBACK_EXECUTOR = Executors.newCachedThreadPool(ThreadUtils.daemonFactory("ws-callback-"));
   private final WsClientOptions options;
   private final ExecutorService callbackExecutor;
   private final Executor callbacks;
   private final List<Consumer<WsFrame>> messageListeners = new CopyOnWriteArrayList<>();
   private final List<Runnable> closeListeners = new CopyOnWriteArrayList<>();
   private final List<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
   private EventLoopGroup group;
   private Channel channel;
   private volatile boolean open;
   private String negotiatedSubprotocol;
   private ScheduledFuture<?> pingTask;

   private static List<WsClient> active() {
      return KarateLifecycle.running().stream().filter(WsClient.class::isInstance).map(WsClient.class::cast).toList();
   }

   public static void closeAll() {
      List<WsClient> clients = active();
      if (!clients.isEmpty()) {
         logger.info("closing {} active websocket client(s)", clients.size());

         for (WsClient client : clients) {
            client.closeNow();
         }
      }
   }

   public static void shutdownCallbackExecutor() {
      CALLBACK_EXECUTOR.shutdown();
   }

   public static WsClient connect(String uri) {
      return connect(WsClientOptions.builder(uri).build());
   }

   public static WsClient connect(URI uri) {
      return connect(WsClientOptions.builder(uri).build());
   }

   public static WsClient connect(WsClientOptions options) {
      WsClient client = new WsClient(options);
      client.doConnect();
      return client;
   }

   private WsClient(WsClientOptions options) {
      this.options = options;
      this.callbackExecutor = options.getCallbackExecutor() != null ? options.getCallbackExecutor() : CALLBACK_EXECUTOR;
      this.callbacks = new WsClient.SerialExecutor(this.callbackExecutor);
      if (options.getMessageListener() != null) {
         this.messageListeners.add(options.getMessageListener());
      }

      if (options.getCloseListener() != null) {
         this.closeListeners.add(options.getCloseListener());
      }

      if (options.getErrorListener() != null) {
         this.errorListeners.add(options.getErrorListener());
      }
   }

   private void doConnect() {
      URI uri = this.options.getUri();
      String scheme = uri.getScheme();
      final String host = this.options.getHost();
      final int port = this.options.getPort();
      SslContext sslContext = null;
      if (this.options.isSsl()) {
         sslContext = this.options.getSslContext();
         if (sslContext == null && this.options.isTrustAllCerts()) {
            try {
               sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException var16) {
               throw new WsException(WsException.Type.CONNECT_FAILED, "SSL context creation failed", var16);
            }
         }
      }

      HttpHeaders headers = new DefaultHttpHeaders();

      for (Entry<String, String> entry : this.options.getHeaders().entrySet()) {
         headers.add(entry.getKey(), entry.getValue());
      }

      WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
         uri, WebSocketVersion.V13, this.options.getSubProtocol(), this.options.isCompression(), headers, this.options.getMaxPayloadSize()
      );
      final WsClientHandler handler = new WsClientHandler(this, handshaker);
      final SslContext finalSslContext = sslContext;
      this.group = new MultiThreadIoEventLoopGroup(1, ThreadUtils.daemonFactory("ws-client-"), NioIoHandler.newFactory());

      try {
         Bootstrap bootstrap = new Bootstrap();
         ((Bootstrap)((Bootstrap)bootstrap.group(this.group)).channel(NioSocketChannel.class)).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) {
               ChannelPipeline p = ch.pipeline();
               if (finalSslContext != null) {
                  p.addLast(new ChannelHandler[]{finalSslContext.newHandler(ch.alloc(), host, port)});
               }

               p.addLast(new ChannelHandler[]{new HttpClientCodec()});
               p.addLast(new ChannelHandler[]{new HttpObjectAggregator(WsClient.this.options.getMaxPayloadSize())});
               if (WsClient.this.options.isCompression()) {
                  p.addLast(new ChannelHandler[]{new WebSocketClientCompressionHandler(0)});
               }

               p.addLast(new ChannelHandler[]{handler});
            }
         });
         this.channel = bootstrap.connect(host, port).sync().channel();
         handler.getHandshakeFuture().await(this.options.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS);
         if (!handler.getHandshakeFuture().isSuccess()) {
            Throwable cause = handler.getHandshakeFuture().cause();
            throw new WsException(WsException.Type.CONNECT_FAILED, "websocket handshake failed: " + (cause != null ? cause.getMessage() : "timeout"), cause);
         } else {
            this.open = true;
            KarateLifecycle.register(this);
            logger.debug("websocket connected: {}", uri);
            if (this.options.getPingInterval() != null) {
               long intervalMillis = this.options.getPingInterval().toMillis();
               this.pingTask = this.channel.eventLoop().scheduleAtFixedRate(this::ping, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
            }
         }
      } catch (WsException var13) {
         throw var13;
      } catch (InterruptedException var14) {
         Thread.currentThread().interrupt();
         throw new WsException(WsException.Type.CONNECT_FAILED, "connection interrupted", var14);
      } catch (Exception var15) {
         throw new WsException(WsException.Type.CONNECT_FAILED, "connection failed: " + var15.getMessage(), var15);
      }
   }

   public boolean isOpen() {
      return this.open && this.channel != null && this.channel.isActive();
   }

   public URI getUri() {
      return this.options.getUri();
   }

   public String getNegotiatedSubprotocol() {
      return this.negotiatedSubprotocol;
   }

   public void send(String text) {
      this.send(WsFrame.text(text));
   }

   public void send(byte[] binary) {
      this.send(WsFrame.binary(binary));
   }

   public void send(WsFrame frame) {
      if (!this.isOpen()) {
         throw new WsException(WsException.Type.CONNECTION_CLOSED, "websocket is not open");
      } else {
         WebSocketFrame wsFrame = this.toNettyFrame(frame);
         this.channel.writeAndFlush(wsFrame).addListener((ChannelFutureListener)future -> {
            if (!future.isSuccess()) {
               logger.error("send failed: {}", future.cause().getMessage());
            }
         });
      }
   }

   public CompletableFuture<Void> sendAsync(WsFrame frame) {
      if (!this.isOpen()) {
         return CompletableFuture.failedFuture(new WsException(WsException.Type.CONNECTION_CLOSED, "websocket is not open"));
      } else {
         CompletableFuture<Void> future = new CompletableFuture<>();
         WebSocketFrame wsFrame = this.toNettyFrame(frame);
         this.channel.writeAndFlush(wsFrame).addListener((ChannelFutureListener)channelFuture -> {
            if (channelFuture.isSuccess()) {
               future.complete(null);
            } else {
               future.completeExceptionally(new WsException(WsException.Type.SEND_FAILED, "send failed", channelFuture.cause()));
            }
         });
         return future;
      }
   }

   private WebSocketFrame toNettyFrame(WsFrame frame) {
      return (WebSocketFrame)(switch (frame.getType()) {
         case TEXT -> new TextWebSocketFrame(frame.getText());
         case BINARY -> new BinaryWebSocketFrame(Unpooled.wrappedBuffer(frame.getBytes()));
         case PING -> new PingWebSocketFrame();
         case PONG -> new PongWebSocketFrame();
         case CLOSE -> new CloseWebSocketFrame(frame.getCloseCode(), frame.getCloseReason());
      });
   }

   public void onMessage(Consumer<WsFrame> listener) {
      this.messageListeners.add(listener);
   }

   public void onClose(Runnable listener) {
      this.closeListeners.add(listener);
   }

   public void onError(Consumer<Throwable> listener) {
      this.errorListeners.add(listener);
   }

   @Override
   public String lifecycleName() {
      return "ws-client:" + this.options.getUri();
   }

   @Override
   public String lifecycleKind() {
      return "ws-client";
   }

   @Override
   public void stop() {
      this.close();
   }

   @Override
   public void close() {
      if (this.channel != null && this.channel.isOpen()) {
         this.channel.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
      }

      this.cleanup();
   }

   public void closeNow() {
      if (this.channel != null) {
         this.channel.close();
      }

      this.cleanup();
   }

   public void waitSync() {
      if (this.channel != null) {
         try {
            this.channel.closeFuture().sync();
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }
      }
   }

   private void cleanup() {
      KarateLifecycle.unregister(this);
      this.open = false;
      if (this.pingTask != null) {
         this.pingTask.cancel(false);
         this.pingTask = null;
      }

      if (this.group != null) {
         this.group.shutdownGracefully();
      }
   }

   public void ping() {
      if (this.isOpen()) {
         this.channel.writeAndFlush(new PingWebSocketFrame());
      }
   }

   void handleFrame(WsFrame frame) {
      for (Consumer<WsFrame> listener : this.messageListeners) {
         this.dispatch(() -> {
            try {
               listener.accept(frame);
            } catch (Exception var3x) {
               logger.error("message listener error: {}", var3x.getMessage());
            }
         });
      }
   }

   void handleDisconnect() {
      this.open = false;

      for (Runnable listener : this.closeListeners) {
         this.dispatch(() -> {
            try {
               listener.run();
            } catch (Exception var2x) {
               logger.error("close listener error: {}", var2x.getMessage());
            }
         });
      }

      this.cleanup();
   }

   void handleError(Throwable cause) {
      for (Consumer<Throwable> listener : this.errorListeners) {
         this.dispatch(() -> {
            try {
               listener.accept(cause);
            } catch (Exception var3x) {
               logger.error("error listener error: {}", var3x.getMessage());
            }
         });
      }
   }

   private void dispatch(Runnable task) {
      try {
         this.callbacks.execute(task);
      } catch (RejectedExecutionException var3) {
         logger.debug("callback dropped, executor already shut down");
      }
   }

   private static final class SerialExecutor implements Executor {
      private final Executor delegate;
      private final Queue<Runnable> queue = new ArrayDeque<>();
      private boolean draining;

      private SerialExecutor(Executor delegate) {
         this.delegate = delegate;
      }

      @Override
      public void execute(Runnable task) {
         synchronized (this) {
            this.queue.add(task);
            if (this.draining) {
               return;
            }

            this.draining = true;
         }

         try {
            this.drain();
         } catch (RuntimeException var6) {
            synchronized (this) {
               this.draining = false;
               this.queue.clear();
            }

            throw var6;
         }
      }

      private void drain() {
         this.delegate.execute(() -> {
            while (true) {
               Runnable next;
               synchronized (this) {
                  next = this.queue.poll();
                  if (next == null) {
                     this.draining = false;
                     return;
                  }
               }

               try {
                  next.run();
               } catch (Throwable var4) {
                  WsClient.logger.error("websocket callback error: {}", var4.getMessage());
               }
            }
         });
      }
   }
}
