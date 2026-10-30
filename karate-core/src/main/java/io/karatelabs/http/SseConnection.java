package io.karatelabs.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseConnection {
   private static final Logger logger = LoggerFactory.getLogger(SseConnection.class);
   private final ChannelHandlerContext ctx;
   private volatile boolean open;
   private volatile boolean headersSent;

   SseConnection(ChannelHandlerContext ctx) {
      this.ctx = ctx;
      this.open = true;
   }

   public void open() {
      this.ensureHeaders();
   }

   private void ensureHeaders() {
      if (!this.headersSent) {
         this.headersSent = true;
         DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
         response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
         response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
         response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
         response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
         response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
         this.ctx.writeAndFlush(response);
      }
   }

   public void reject(int status, String message) {
      if (this.headersSent) {
         this.close();
      } else {
         this.headersSent = true;
         this.open = false;
         byte[] body = (message == null ? "" : message).getBytes(StandardCharsets.UTF_8);
         FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status), Unpooled.copiedBuffer(body));
         response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
         response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
         this.ctx.writeAndFlush(response).addListener(future -> this.ctx.close());
      }
   }

   public void send(String event, String data) {
      if (this.open && this.ctx.channel().isActive()) {
         this.ensureHeaders();
         StringBuilder sb = new StringBuilder();
         if (event != null) {
            sb.append("event: ").append(event).append('\n');
         }

         if (data != null) {
            for (String line : data.split("\n", -1)) {
               sb.append("data: ").append(line).append('\n');
            }
         }

         sb.append('\n');
         this.ctx.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer(sb.toString(), StandardCharsets.UTF_8)));
      } else {
         this.open = false;
      }
   }

   public void send(String data) {
      this.send(null, data);
   }

   public void sendComment(String comment) {
      if (this.open && this.ctx.channel().isActive()) {
         this.ensureHeaders();
         String frame = ": " + comment + "\n\n";
         this.ctx.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer(frame, StandardCharsets.UTF_8)));
      } else {
         this.open = false;
      }
   }

   public void close() {
      if (this.open) {
         this.open = false;
         this.ensureHeaders();
         this.ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(future -> this.ctx.close());
      }
   }

   public boolean isOpen() {
      return this.open && this.ctx.channel().isActive();
   }

   public void onDisconnect(Runnable callback) {
      this.ctx.channel().closeFuture().addListener(future -> {
         this.open = false;
         callback.run();
      });
   }
}
