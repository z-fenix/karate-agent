package io.karatelabs.ext.rules.twin;

import java.util.List;
import java.util.Map;

public record ReplayTrace(List<io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep> a, Object b) {

   public ReplayTrace(List<io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep> a, Object b) {
      this.a = a;
      this.b = b;
   }

   public record ReplayStep(String a, Map<String, Object> b, String c, String d, String e, Map<String, Object> f, String g, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> h, Object i) {

      public ReplayStep(String a, Map<String, Object> b, String c, String d, String e, Map<String, Object> f, String g, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> h, Object i) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
      }
   }
}
