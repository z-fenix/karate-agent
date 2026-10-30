package io.karatelabs.ext.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.List;
import java.util.Map;

public interface AddressMatcher {
   List<CoverageItem> buildItems(Contract var1);

   Map<String, Object> buildMetadata(Contract var1, List<CoverageItem> var2);

   boolean accepts(Action var1);

   CoverageItem match(Action var1, List<CoverageItem> var2);

   List<Warning> warnings(Action var1, CoverageItem var2, List<CoverageItem> var3);

   MatchKind matchConfidence(Action var1, CoverageItem var2, List<CoverageItem> var3);

   List<Warning> loadWarnings(List<CoverageItem> var1);

   Map<String, Object> aggregate(List<Action> var1, CoverageItem var2);
}
