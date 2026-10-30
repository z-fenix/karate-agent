package io.karatelabs.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsClientHandler extends SimpleChannelInboundHandler<Object> {
   private static final Logger logger = LoggerFactory.getLogger(WsClientHandler.class);
   protected final WsClient client;
   private final WebSocketClientHandshaker handshaker;
   private ChannelPromise handshakeFuture;

   public WsClientHandler(WsClient client, WebSocketClientHandshaker handshaker) {
      this.client = client;
      this.handshaker = handshaker;
   }

   public ChannelPromise getHandshakeFuture() {
      return this.handshakeFuture;
   }

   public void handlerAdded(ChannelHandlerContext ctx) {
      this.handshakeFuture = ctx.newPromise();
   }

   public void channelActive(ChannelHandlerContext ctx) {
      this.handshaker.handshake(ctx.channel());
   }

   public void channelInactive(ChannelHandlerContext ctx) {
      logger.debug("websocket disconnected: {}", this.client.getUri());
      this.client.handleDisconnect();
   }

   protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (!this.handshaker.isHandshakeComplete()) {
         try {
            this.handshaker.finishHandshake(ctx.channel(), (FullHttpResponse)msg);
            logger.debug("websocket handshake complete: {}", this.client.getUri());
            this.handshakeFuture.setSuccess();
         } catch (WebSocketHandshakeException var9) {
            logger.error("websocket handshake failed: {}", var9.getMessage());
            this.handshakeFuture.setFailure(var9);
         }
      } else if (msg instanceof FullHttpResponse response) {
         throw new IllegalStateException(
            "unexpected FullHttpResponse (status=" + response.status() + ", content=" + response.content().toString(StandardCharsets.UTF_8) + ")"
         );
      } else {
         WebSocketFrame frame = (WebSocketFrame)msg;
         if (frame instanceof TextWebSocketFrame textFrame) {
            this.onTextFrame(ctx, textFrame);
         } else if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            this.onBinaryFrame(ctx, binaryFrame);
         } else if (frame instanceof PingWebSocketFrame pingFrame) {
            this.onPingFrame(ctx, pingFrame);
         } else if (frame instanceof PongWebSocketFrame pongFrame) {
            this.onPongFrame(ctx, pongFrame);
         } else if (frame instanceof CloseWebSocketFrame closeFrame) {
            this.onCloseFrame(ctx, closeFrame);
         }
      }
   }

   protected void onTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
      String text = frame.text();
      logger.trace("received text frame: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
      this.client.handleFrame(WsFrame.text(text));
   }

   protected void onBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
      byte[] bytes = new byte[frame.content().readableBytes()];
      frame.content().readBytes(bytes);
      logger.trace("received binary frame: {} bytes", bytes.length);
      this.client.handleFrame(WsFrame.binary(bytes));
   }

   protected void onPingFrame(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
      logger.trace("received ping, sending pong");
      ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
   }

   protected void onPongFrame(ChannelHandlerContext ctx, PongWebSocketFrame frame) {
      logger.trace("received pong");
      this.client.handleFrame(WsFrame.pong());
   }

   protected void onCloseFrame(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
      int code = frame.statusCode();
      String reason = frame.reasonText();
      logger.debug("received close frame: {} {}", code, reason);
      this.client.handleFrame(WsFrame.close(code, reason));
      ctx.close();
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      logger.error("websocket error: {}", cause.getMessage());
      if (!this.handshakeFuture.isDone()) {
         this.handshakeFuture.setFailure(cause);
      }

      this.client.handleError(cause);
      ctx.close();
   }
}
