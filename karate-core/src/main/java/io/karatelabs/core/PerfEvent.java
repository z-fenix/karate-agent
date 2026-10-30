package io.karatelabs.core;

public class PerfEvent {
   private final String name;
   private final long startTime;
   private final long endTime;
   private final int statusCode;
   private boolean failed;
   private String message;

   public PerfEvent(long startTime, long endTime, String name, int statusCode) {
      this.name = name;
      this.startTime = startTime;
      this.endTime = endTime;
      this.statusCode = statusCode;
   }

   public String getName() {
      return this.name;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getEndTime() {
      return this.endTime;
   }

   public int getStatusCode() {
      return this.statusCode;
   }

   public boolean isFailed() {
      return this.failed;
   }

   public void setFailed(boolean failed) {
      this.failed = failed;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   @Override
   public String toString() {
      return "PerfEvent{name='"
         + this.name
         + "', startTime="
         + this.startTime
         + ", endTime="
         + this.endTime
         + ", statusCode="
         + this.statusCode
         + ", failed="
         + this.failed
         + ", message='"
         + this.message
         + "'}";
   }
}
