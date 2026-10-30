package io.karatelabs.js;

public class EngineTimeoutException extends EngineInterruptedException {
   public EngineTimeoutException(String message) {
      super(message);
   }
}
