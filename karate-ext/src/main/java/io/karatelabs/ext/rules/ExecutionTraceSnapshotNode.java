package io.karatelabs.ext.rules;

import java.util.List;

public record ExecutionTraceSnapshotNode(String a, int b, int c, boolean d, String e, List<ExecutionTraceSnapshotNode> f) {

   public ExecutionTraceSnapshotNode(String a, int b, int c, boolean d, String e, List<ExecutionTraceSnapshotNode> f) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
   }
}
