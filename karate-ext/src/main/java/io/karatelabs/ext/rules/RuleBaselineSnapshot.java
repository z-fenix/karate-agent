package io.karatelabs.ext.rules;

import java.util.Map;

public record RuleBaselineSnapshot(Map<String, ExecutionTraceSnapshotNode> a, Object b, long c, Map<String, String> d, String e) {

   public RuleBaselineSnapshot(Map<String, ExecutionTraceSnapshotNode> a, Object b, long c, Map<String, String> d, String e) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
   }
}
