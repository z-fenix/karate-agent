package io.karatelabs.ext.coverage;

import java.util.List;

public class SourceCoverageAnalyzer {
   private final CoverageSource a;
   private final io.karatelabs.ext.coverage.graph.TraceGraph b;

   public SourceCoverageAnalyzer(CoverageSource var1, CoverageRunEvidence var2) {
      this.a = var1;
      this.b = io.karatelabs.ext.coverage.graph.TraceGraph.a(List.of(var1), var2);
   }

   public CoverageSourceResult analyze() {
      return this.b.a(this.a);
   }

   public io.karatelabs.ext.coverage.graph.TraceGraph traceGraph() {
      return this.b;
   }
}
