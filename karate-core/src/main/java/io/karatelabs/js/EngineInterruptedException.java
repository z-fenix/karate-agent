package io.karatelabs.js;

public class EngineInterruptedException extends EngineException {
   public EngineInterruptedException() {
      super("interrupted", null);
   }

   protected EngineInterruptedException(String message) {
      super(message, null);
   }
}
