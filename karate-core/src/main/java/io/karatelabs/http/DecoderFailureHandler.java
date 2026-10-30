package io.karatelabs.http;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http.TooLongHttpLineException;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DecoderFailureHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(DecoderFailureHandler.class);
   private boolean rejected;

   public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (this.rejected) {
         ReferenceCountUtil.release(msg);
      } else if (msg instanceof HttpObject httpObject && httpObject.decoderResult().isFailure()) {
         this.rejected = true;
         Throwable cause = httpObject.decoderResult().cause();
         ReferenceCountUtil.release(msg);
         HttpResponseStatus status = cause instanceof TooLongHttpHeaderException
            ? HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
            : (cause instanceof TooLongHttpLineException ? HttpResponseStatus.REQUEST_URI_TOO_LONG : HttpResponseStatus.BAD_REQUEST);
         logger.warn("bad request: {}", cause.getMessage());
         ctx.writeAndFlush(HttpServerHandler.error(status, status.reasonPhrase())).addListener(ChannelFutureListener.CLOSE);
      } else {
         ctx.fireChannelRead(msg);
      }
   }
}
