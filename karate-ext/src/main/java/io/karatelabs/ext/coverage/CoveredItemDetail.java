package io.karatelabs.ext.coverage;

import java.util.List;
import java.util.Map;

public record CoveredItemDetail(CoverageItem e, List<Action> f, Map<String, Object> g) {


   public int actionCount() {
      return this.f != null ? this.f.size() : 0;
   }

   public Long averageDurationMs() {
      return (Long)this.g.get("avgDurationMs");
   }

   public Map<Integer, Integer> statusCodeCounts() {
      return (Map<Integer, Integer>)this.g.get("statusCodes");
   }

   public Map<String, Integer> pathParameterCardinality() {
      return (Map<String, Integer>)this.g.get("pathParamCardinality");
   }

   public CoverageItem e() {
      return this.e;
   }

   public List<Action> f() {
      return this.f;
   }

   public Map<String, Object> g() {
      return this.g;
   }
}
