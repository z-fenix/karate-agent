package io.karatelabs.js;

public class EngineException extends RuntimeException {
   private final String jsErrorName;
   private final String jsMessage;
   private final boolean authoredThrow;
   private final Object thrownValue;
   private final int throwLine;

   public EngineException(String message, Throwable cause) {
      this(message, cause, null, null);
   }

   public EngineException(String message, Throwable cause, String jsErrorName) {
      this(message, cause, jsErrorName, null);
   }

   public EngineException(String message, Throwable cause, String jsErrorName, String jsMessage) {
      this(message, cause, jsErrorName, jsMessage, false, null, 0);
   }

   public EngineException(String message, Throwable cause, String jsErrorName, String jsMessage, boolean authoredThrow, Object thrownValue, int throwLine) {
      super(message, cause);
      this.jsErrorName = jsErrorName;
      this.jsMessage = jsMessage;
      this.authoredThrow = authoredThrow;
      this.thrownValue = thrownValue;
      this.throwLine = throwLine;
   }

   public String getJsErrorName() {
      return this.jsErrorName;
   }

   public String getJsMessage() {
      return this.jsMessage;
   }

   public boolean isAuthoredThrow() {
      return this.authoredThrow;
   }

   public Object getThrownValue() {
      return this.thrownValue;
   }

   public int getThrowLine() {
      return this.throwLine;
   }
}
