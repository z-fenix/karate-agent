package io.karatelabs.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
   static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
   final HttpServer server;
   private volatile CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

   HttpServerHandler(HttpServer server) {
      this.server = server;
   }

   protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
      HttpRequest request;
      try {
         request = toRequest(req, this.server.isSsl());
      } catch (Exception var8) {
         logger.warn("bad request '{}': {}", req.uri(), var8.getMessage());
         FullHttpResponse res = error(HttpResponseStatus.BAD_REQUEST, "cannot parse the request URI: " + var8.getMessage());
         this.answerInTurn(ctx, done -> ctx.writeAndFlush(res).addListener(done));
         return;
      }

      if (this.server.wsHandler != null && isWsUpgrade(req)) {
         try {
            this.handleWsUpgrade(ctx, req, request);
         } catch (Exception var6) {
            String message = var6.getMessage();
            logger.error("ws upgrade error: {}", message);
            ctx.writeAndFlush(error(message));
         }
      } else if (this.server.sseHandler != null && isSseRequest(req)) {
         try {
            SseConnection connection = new SseConnection(ctx);
            this.server.sseHandler.accept(request, connection);
         } catch (Exception var7) {
            String message = var7.getMessage();
            logger.error("sse handler error: {}", message);
            ctx.writeAndFlush(error(message));
         }
      } else {
         this.answerInTurn(ctx, done -> this.handle(ctx, request, done));
      }
   }

   private void answerInTurn(ChannelHandlerContext ctx, Consumer<ChannelFutureListener> answer) {
      ctx.channel().config().setAutoRead(false);
      CompletableFuture<Void> answered = new CompletableFuture<>();
      CompletableFuture<Void> previous = this.tail;
      this.tail = answered;
      previous.whenCompleteAsync((v, t) -> answer.accept(f -> answered.complete(null)), this.server.handlers);
      answered.whenComplete((v, t) -> {
         if (this.tail == answered) {
            ctx.channel().config().setAutoRead(true);
         }
      });
   }

   private void handle(ChannelHandlerContext ctx, HttpRequest request, ChannelFutureListener done) {
      try {
         HttpResponse response = this.server.handler.apply(request);
         int delay = response.getDelay();
         if (response.isResetConnection()) {
            Runnable reset = () -> {
               ctx.channel().config().setOption(ChannelOption.SO_LINGER, 0);
               ctx.close().addListener(done);
            };
            if (delay > 0) {
               ctx.executor().schedule(reset, delay, TimeUnit.MILLISECONDS);
            } else {
               reset.run();
            }

            return;
         }

         FullHttpResponse res = toResponse(response);
         if (delay > 0) {
            ctx.executor().schedule(() -> ctx.writeAndFlush(res).addListener(done), delay, TimeUnit.MILLISECONDS);
         } else {
            ctx.writeAndFlush(res).addListener(done);
         }
      } catch (Exception var7) {
         String message = var7.getMessage();
         logger.error("http server error: {}", message);
         ctx.writeAndFlush(error(message)).addListener(done);
      } catch (Throwable var8) {
         logger.error("http server error: {}", var8.toString());
         ctx.writeAndFlush(error(var8.getMessage())).addListener(done).addListener(ChannelFutureListener.CLOSE);
      }
   }

   static boolean isSseRequest(FullHttpRequest req) {
      if (!HttpMethod.GET.equals(req.method())) {
         return false;
      } else {
         String accept = req.headers().get("Accept");
         return accept != null && accept.contains("text/event-stream");
      }
   }

   static HttpRequest toRequest(FullHttpRequest req) {
      return toRequest(req, false);
   }

   static HttpRequest toRequest(FullHttpRequest req, boolean ssl) {
      HttpRequest request = new HttpRequest();
      request.setUrl(req.uri());
      request.setMethod(req.method().name());
      HttpHeaders hh = req.headers();
      Map<String, List<String>> headers = new HashMap<>(hh.size());
      hh.forEach(entry -> {
         List<String> list = headers.computeIfAbsent((String)entry.getKey(), k -> new ArrayList<>());
         list.add((String)entry.getValue());
      });
      request.setHeaders(headers);
      String host = hh.get("Host");
      if (host != null) {
         String proto = hh.get("X-Forwarded-Proto");
         if (proto == null) {
            proto = ssl ? "https" : "http";
         }

         request.setUrlBase(proto + "://" + host);
      }

      ByteBuf buf = req.content();
      int len = buf.readableBytes();
      if (len > 0) {
         byte[] bytes = new byte[len];
         buf.readBytes(bytes);
         request.setBody(bytes);
      }

      return request;
   }

   static FullHttpResponse toResponse(HttpResponse response) {
      String statusText = response.getStatusText();
      HttpResponseStatus status = statusText == null
         ? HttpResponseStatus.valueOf(response.getStatus())
         : HttpResponseStatus.valueOf(response.getStatus(), statusText);
      FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
      Map<String, List<String>> headers = response.getHeaders();
      if (headers != null) {
         headers.forEach((k, list) -> {
            for (String v : list) {
               res.headers().add(k, v);
            }
         });
      }

      byte[] bytes = response.getBodyBytes();
      if (bytes != null) {
         ByteBuf content = Unpooled.copiedBuffer(bytes);
         res.content().writeBytes(content);
      }

      res.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes == null ? 0 : bytes.length);
      return res;
   }

   static FullHttpResponse error(String message) {
      return error(HttpResponseStatus.INTERNAL_SERVER_ERROR, message);
   }

   static FullHttpResponse error(HttpResponseStatus status, String message) {
      FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
      res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
      String text = message == null ? String.valueOf(status.reasonPhrase()) : message;
      ByteBuf content = Unpooled.copiedBuffer(text.getBytes(StandardCharsets.UTF_8));
      res.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
      res.content().writeBytes(content);
      return res;
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      logger.error("http server pipeline error: {}", cause.toString());
      if (ctx.channel().isActive()) {
         ctx.writeAndFlush(error(cause.getMessage())).addListener(ChannelFutureListener.CLOSE);
      } else {
         ctx.close();
      }
   }

   static boolean isWsUpgrade(FullHttpRequest req) {
      String upgrade = req.headers().get(HttpHeaderNames.UPGRADE);
      return upgrade != null && "websocket".equalsIgnoreCase(upgrade);
   }

   private void handleWsUpgrade(ChannelHandlerContext ctx, FullHttpRequest req, HttpRequest request) {
      String wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + req.uri();
      WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(wsUrl, null, true, 1048576);
      WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
      if (handshaker == null) {
         WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
      } else {
         WsConnection connection = new WsConnection(ctx);
         handshaker.handshake(ctx.channel(), req).addListener(future -> {
            if (future.isSuccess()) {
               ChannelPipeline p = ctx.pipeline();
               p.addLast(new ChannelHandler[]{new WsServerHandler(connection)});
               this.server.wsHandler.accept(request, connection);
            } else {
               logger.error("ws handshake failed: {}", future.cause().getMessage());
               ctx.close();
            }
         });
      }
   }
}
