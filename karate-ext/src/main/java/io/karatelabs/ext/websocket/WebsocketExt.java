package io.karatelabs.ext.websocket;

import io.karatelabs.core.Ext;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.contract.WireCodec;
import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(WebsocketExt.class);
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   private static final String HEADERS = "headers";
   private static final String CODEC = "codec";
   Map<String, Object> headers;
   WireCodec codec;

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("websocket", "openapi");
      var1.registerChannelFactory("websocket", new WebsocketChannelFactory(this));
      logger.info("websocket ext booted (channel factory registered)");
   }

   void seedDefaults(WebsocketConsumer var1) {
      if (var1.headers == null && this.headers != null) {
         var1.headers = new LinkedHashMap<>(this.headers);
      }

      if (var1.codec == null && this.codec != null) {
         var1.codec = this.codec;
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("headers", "codec");
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "headers" -> this.headers;
         case "codec" -> this.codec;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "headers":
            if (var2 instanceof Map) {
               this.headers = (Map<String, Object>)var2;
            }
            break;
         case "codec":
            this.codec = (WireCodec)var2;
            break;
         default:
            logger.warn("websocket ext: unknown boot config key: {} - {}", var1, var2);
      }
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", VERSION);
      return var1;
   }
}
