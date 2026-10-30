package io.karatelabs.ext.websocket;

import io.karatelabs.core.Channel;
import io.karatelabs.core.ScenarioRuntime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketChannel implements Channel {
   static final Logger logger = LoggerFactory.getLogger("karate.websocket");
   private final WebsocketExt ext;
   final List<WebsocketConsumer> sessions = new ArrayList<>();

   public WebsocketChannel(WebsocketExt var1) {
      this.ext = var1;
   }

   public WebsocketConsumer init(ScenarioRuntime var1) {
      WebsocketConsumer var2 = new WebsocketConsumer();
      if (this.ext != null) {
         this.ext.seedDefaults(var2);
      }

      this.sessions.add(var2);
      return var2;
   }

   @Override
   public void afterScenario() {
      for (WebsocketConsumer var2 : this.sessions) {
         try {
            var2.stop();
         } catch (Exception var4) {
            logger.warn("{}", var4.getMessage());
         }
      }

      this.sessions.clear();
   }
}
