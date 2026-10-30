package io.karatelabs.js;

public interface ContextListener {
   default void onEvent(Event event) {
   }

   default ExitResult onError(Event event, Exception e) {
      return null;
   }

   default void onBind(BindEvent event) {
   }
}
