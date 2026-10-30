package io.karatelabs.core;

public class KarateException extends RuntimeException {
   private final String rawMessage;

   private KarateException(String rawMessage, String decoratedMessage) {
      super(decoratedMessage);
      this.rawMessage = rawMessage;
   }

   public String getRawMessage() {
      return this.rawMessage;
   }

   public static KarateException forStep(Throwable error, String featurePath, int line, String stepText) {
      if (error instanceof KarateException ke) {
         return ke;
      } else {
         String message = error.getMessage();
         if (message == null) {
            message = error.toString();
         }

         String location = featurePath + ":" + line;
         KarateException wrapper = new KarateException(message, message + "\n" + location);
         StackTraceElement featureFrame = new StackTraceElement("<feature>", stepText == null ? "" : stepText, featurePath, line);
         StackTraceElement[] original = error.getStackTrace();
         StackTraceElement[] merged = new StackTraceElement[original.length + 1];
         merged[0] = featureFrame;
         System.arraycopy(original, 0, merged, 1, original.length);
         wrapper.setStackTrace(merged);
         return wrapper;
      }
   }
}
