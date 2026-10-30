package io.karatelabs.core;

import io.karatelabs.gherkin.Step;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import java.util.LinkedHashMap;
import java.util.Map;

public record MatchRunEvent(
   RunEventType type,
   Match.Type matchType,
   String actualExpr,
   String expectedExpr,
   Object actual,
   Object expected,
   Result result,
   ScenarioRuntime scenarioRuntime,
   long timeStamp
) implements RunEvent {
   public static MatchRunEvent exit(
      Match.Type matchType, String actualExpr, String expectedExpr, Object actual, Object expected, Result result, ScenarioRuntime sr
   ) {
      return new MatchRunEvent(RunEventType.MATCH_EXIT, matchType, actualExpr, expectedExpr, actual, expected, result, sr, System.currentTimeMillis());
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
      map.put("matchType", this.matchType == null ? null : this.matchType.name());
      map.put("actualExpr", this.actualExpr);
      map.put("expectedExpr", this.expectedExpr);
      if (this.result != null) {
         map.put("pass", this.result.pass);
         map.put("message", this.result.message);
      }

      return map;
   }

   public Step getCurrentStep() {
      return this.scenarioRuntime != null ? this.scenarioRuntime.getCurrentStep() : null;
   }
}
