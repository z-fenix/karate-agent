package io.karatelabs.ext.coverage;

import java.util.List;
import java.util.Map;

public interface CoverageRunEvidence {
   List<Action> a();

   default List<TestExecutionSummary> b() {
      return List.of();
   }

   String c();

   Map<String, Object> d();

   default List<String> e() {
      return (List<String>)this.d().get("files");
   }
}
