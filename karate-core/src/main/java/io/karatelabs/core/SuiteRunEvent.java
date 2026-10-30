package io.karatelabs.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SuiteRunEvent(RunEventType type, Suite source, SuiteResult result, long timeStamp) implements RunEvent {
   public static SuiteRunEvent enter(Suite suite) {
      return new SuiteRunEvent(RunEventType.SUITE_ENTER, suite, null, System.currentTimeMillis());
   }

   public static SuiteRunEvent exit(Suite suite, SuiteResult result) {
      return new SuiteRunEvent(RunEventType.SUITE_EXIT, suite, result, System.currentTimeMillis());
   }

   @Override
   public RunEventType getType() {
      return this.type;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      if (this.type == RunEventType.SUITE_EXIT && this.result != null) {
         map.put("summary", this.result.summaryJson());
         return map;
      } else {
         if (this.source != null) {
            map.put("env", this.source.env);
            map.put("threads", this.source.threadCount);
            BootBinding boot = this.source.getBootBinding();
            if (boot != null) {
               List<Map<String, Object>> manifests = boot.manifests();
               if (!manifests.isEmpty()) {
                  map.put("exts", manifests);
               }
            }
         }

         return map;
      }
   }
}
