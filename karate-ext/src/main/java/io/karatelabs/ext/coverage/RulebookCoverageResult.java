package io.karatelabs.ext.coverage;

import java.util.Map;

public record RulebookCoverageResult(String c, io.karatelabs.ext.coverage.collect.TestExecution d, Map<String, Object> e, Map<String, Object> f) {


   public boolean hasError() {
      return this.f != null;
   }

   public String errorCode() {
      return this.f == null ? null : String.valueOf(this.f.get("code"));
   }

   public String c() {
      return this.c;
   }

   public io.karatelabs.ext.coverage.collect.TestExecution d() {
      return this.d;
   }

   public Map<String, Object> e() {
      return this.e;
   }

   public Map<String, Object> f() {
      return this.f;
   }

   public interface RulebookCoverageProducer {
      Map<String, RulebookCoverageResult> a(io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var1);
   }
}
