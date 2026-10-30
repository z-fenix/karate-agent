package io.karatelabs.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsConnection {
   private static final Logger logger = LoggerFactory.getLogger(WsConnection.class);
   private final ChannelHandlerContext ctx;
   private volatile boolean open;
   private volatile Consumer<String> messageHandler;
   private volatile Consumer<byte[]> binaryHandler;

   WsConnection(ChannelHandlerContext ctx) {
      this.ctx = ctx;
      this.open = true;
   }

   public void send(String text) {
      if (this.isOpen()) {
         this.ctx.writeAndFlush(new TextWebSocketFrame(text));
      }
   }

   public void sendBytes(byte[] data) {
      if (this.isOpen()) {
         this.ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
      }
   }

   public void onMessage(Consumer<String> handler) {
      this.messageHandler = handler;
   }

   public void onBinary(Consumer<byte[]> handler) {
      this.binaryHandler = handler;
   }

   public void onClose(Runnable callback) {
      this.ctx.channel().closeFuture().addListener(future -> {
         this.open = false;
         callback.run();
      });
   }

   public void close() {
      this.close(1000, null);
   }

   public void close(int code, String reason) {
      if (this.open) {
         this.open = false;
         this.ctx.writeAndFlush(new CloseWebSocketFrame(code, reason)).addListener(future -> this.ctx.close());
      }
   }

   public boolean isOpen() {
      return this.open && this.ctx.channel().isActive();
   }

   void handleText(String text) {
      Consumer<String> handler = this.messageHandler;
      if (handler != null) {
         try {
            handler.accept(text);
         } catch (Exception var4) {
            logger.error("ws message handler error: {}", var4.getMessage());
         }
      }
   }

   void handleBinary(byte[] data) {
      Consumer<byte[]> handler = this.binaryHandler;
      if (handler != null) {
         try {
            handler.accept(data);
         } catch (Exception var4) {
            logger.error("ws binary handler error: {}", var4.getMessage());
         }
      }
   }

   void handleClose() {
      this.open = false;
   }
}
