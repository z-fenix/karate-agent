package io.karatelabs.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
   private static final Logger logger = LoggerFactory.getLogger(WsServerHandler.class);
   private final WsConnection connection;

   WsServerHandler(WsConnection connection) {
      this.connection = connection;
   }

   protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
      if (frame instanceof TextWebSocketFrame text) {
         this.connection.handleText(text.text());
      } else if (frame instanceof BinaryWebSocketFrame binary) {
         byte[] bytes = new byte[binary.content().readableBytes()];
         binary.content().readBytes(bytes);
         this.connection.handleBinary(bytes);
      } else if (frame instanceof PingWebSocketFrame ping) {
         ctx.writeAndFlush(new PongWebSocketFrame(ping.content().retain()));
      } else if (frame instanceof CloseWebSocketFrame) {
         this.connection.handleClose();
         ctx.close();
      }
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      logger.error("ws error: {}", cause.getMessage());
      this.connection.handleClose();
      ctx.close();
   }
}
