package io.karatelabs.output;

public enum LogLevel {
   TRACE(0),
   DEBUG(1),
   INFO(2),
   WARN(3),
   ERROR(4);

   public final int value;

   private LogLevel(int value) {
      this.value = value;
   }

   public boolean isEnabled(LogLevel threshold) {
      return this.value >= threshold.value;
   }
}
