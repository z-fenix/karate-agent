package io.karatelabs.core;

import io.karatelabs.gherkin.Step;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public record HttpRunEvent(RunEventType type, HttpRequest request, HttpResponse response, ScenarioRuntime scenarioRuntime, long timeStamp) implements RunEvent {
   public static HttpRunEvent enter(HttpRequest request, ScenarioRuntime sr) {
      return new HttpRunEvent(RunEventType.HTTP_ENTER, request, null, sr, System.currentTimeMillis());
   }

   public static HttpRunEvent exit(HttpRequest request, HttpResponse response, ScenarioRuntime sr) {
      return new HttpRunEvent(RunEventType.HTTP_EXIT, request, response, sr, System.currentTimeMillis());
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
      if (this.request != null) {
         map.put("method", this.request.getMethod());
         map.put("url", this.request.getUrlAndPath());
      }

      if (this.type == RunEventType.HTTP_EXIT && this.response != null) {
         map.put("status", this.response.getStatus());
         map.put("responseTime", this.response.getResponseTime());
      }

      return map;
   }

   public Step getCurrentStep() {
      return this.scenarioRuntime != null ? this.scenarioRuntime.getCurrentStep() : null;
   }
}
