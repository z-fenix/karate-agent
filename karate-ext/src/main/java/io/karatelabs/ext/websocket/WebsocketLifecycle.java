package io.karatelabs.ext.websocket;

public interface WebsocketLifecycle {
   default void onStart(WebsocketConsumer var1) {
   }

   default void onStop(WebsocketConsumer var1) {
   }

   default void onMessage(WebsocketConsumer var1, Object var2) {
      var1.receive(var2);
   }
}
