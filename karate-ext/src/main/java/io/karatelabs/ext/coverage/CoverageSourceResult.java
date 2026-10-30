package io.karatelabs.ext.coverage;

import java.util.List;

public record CoverageSourceResult(CoverageSource e, CoverageRunEvidence f, List<CoveredItemDetail> g, List<CoverageItem> h, List<Action> i, CoverageSummary j) {

   public CoverageSourceResult(CoverageSource e, CoverageRunEvidence f, List<CoveredItemDetail> g, List<CoverageItem> h, List<Action> i, CoverageSummary j) {
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
   }

   public double coveragePercentage() {
      return this.j != null ? this.j.f() : 0.0;
   }

   public boolean hasWarnings() {
      return this.j != null && this.j.warningCount() > 0;
   }

   public boolean hasActions() {
      return this.i != null && !this.i.isEmpty();
   }

   public int evidenceActionCount() {
      int var1 = this.g != null ? this.g.stream().mapToInt(io.karatelabs.ext.coverage.CoveredItemDetail::actionCount).sum() : 0;
      int var2 = this.i != null ? this.i.size() : 0;
      return var1 + var2;
   }

   public CoverageSource e() {
      return this.e;
   }

   public CoverageRunEvidence f() {
      return this.f;
   }

   public List<CoveredItemDetail> g() {
      return this.g;
   }

   public List<CoverageItem> h() {
      return this.h;
   }

   public List<Action> i() {
      return this.i;
   }

   public CoverageSummary j() {
      return this.j;
   }
}
