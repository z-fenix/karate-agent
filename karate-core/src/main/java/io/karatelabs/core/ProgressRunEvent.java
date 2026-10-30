package io.karatelabs.core;

import java.util.LinkedHashMap;
import java.util.Map;

public record ProgressRunEvent(Suite suite, int completed, int total, long timeStamp) implements RunEvent {
   public static ProgressRunEvent of(Suite suite, int completed, int total) {
      return new ProgressRunEvent(suite, completed, total, System.currentTimeMillis());
   }

   @Override
   public RunEventType getType() {
      return RunEventType.PROGRESS;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("completed", this.completed);
      map.put("total", this.total);
      if (this.total > 0) {
         map.put("percent", this.completed * 100 / this.total);
      }

      return map;
   }
}
