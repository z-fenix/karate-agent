package io.karatelabs.ext.rules;

import io.karatelabs.ext.diff.DiffSeverity;
import io.karatelabs.ext.diff.DiffReport;
import io.karatelabs.ext.diff.DiffFinding;
import io.karatelabs.ext.diff.UnevaluatedDiff;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DiffSeverityTest {
   @Test
   public void diffFindingRetainsItsSeverityAndKind() {
      var finding = new DiffFinding("taxonomy", "removed", DiffSeverity.BREAKING);
      assertEquals(DiffSeverity.BREAKING, finding.severity());
      assertEquals(finding.identifier(), finding.toMap().get("id"));
      assertEquals("removed", finding.toMap().get("kind"));
      assertEquals("BREAKING", finding.toMap().get("level"));
   }

   @Test
   public void diffReportRetainsTaxonomyAndResult() {
      var model = new DiffReport("taxonomy", "analyzer", "1");
      assertEquals("taxonomy", model.taxonomy());
      var report = model.toMap();
      assertEquals("taxonomy", report.get("taxonomy"));
      assertEquals(Boolean.TRUE, report.get("ok"));
   }

   @Test
   public void unevaluatedDiffRetainsReportFields() {
      var item = new UnevaluatedDiff("unknown", "schema", "current", "/x", "detail");
      assertEquals("unknown", item.toMap().get("reason"));
      assertEquals("schema", item.toMap().get("construct"));
      assertEquals("current", item.toMap().get("side"));
   }

   @Test
   public void severityBucketsKeepTheirReportNames() {
      assertEquals("breaking", DiffSeverity.BREAKING.bucket());
      assertEquals("warnings", DiffSeverity.WARNING.bucket());
      assertEquals("compatible", DiffSeverity.COMPATIBLE.bucket());
   }
}
