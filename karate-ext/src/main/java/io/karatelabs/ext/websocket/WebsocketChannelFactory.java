package io.karatelabs.ext.websocket;

import io.karatelabs.core.Channel;
import io.karatelabs.core.ChannelFactory;
import io.karatelabs.core.ScenarioRuntime;
import java.util.Map;

public class WebsocketChannelFactory implements ChannelFactory {
   private static volatile boolean gated;
   private final WebsocketExt ext;

   public WebsocketChannelFactory() {
      this(null);
   }

   public WebsocketChannelFactory(WebsocketExt var1) {
      this.ext = var1;
   }

   @Override
   public Channel create(ScenarioRuntime var1, Map<String, Object> var2) {
      if (!gated) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("websocket", "openapi");
         gated = true;
      }

      return new WebsocketChannel(this.ext);
   }
}
