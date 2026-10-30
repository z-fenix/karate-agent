package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.contract.Address;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Outcome;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class GenericContractAddressMatcher implements AddressMatcher {
   private final String a;

   GenericContractAddressMatcher(Contract var1) {
      this.a = io.karatelabs.ext.coverage.collect.ContractCoverageSource.a(var1.protocol());
   }

   @Override
   public List<CoverageItem> buildItems(Contract var1) {
      ArrayList<io.karatelabs.ext.coverage.CoverageItem> var2 = new ArrayList();

      for (Operation var4 : var1.operations()) {
         Address var5 = var4.address();
         String var6 = var5.qualifier() == null ? var5.primary() : var5.primary() + " " + var5.qualifier();
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("interaction", var4.interaction().name());
         var7.put("addressKind", var5.kind().name());
         var7.put("primary", var5.primary());
         var7.put("qualifier", var5.qualifier());
         var7.put("responses", var4.outcomes().stream().map(Outcome::code).toList());
         var2.add(new CoverageItem(var4.id(), var6, new LinkedHashSet<>(var4.tags()), null, var7));
      }

      return var2;
   }

   @Override
   public Map<String, Object> buildMetadata(Contract var1, List<CoverageItem> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("specType", var1.ext().get("specType"));
      var3.put("title", var1.title());
      var3.put("version", var1.version());
      LinkedHashSet var4 = new LinkedHashSet();

      for (CoverageItem var6 : var2) {
         var4.addAll(var6.tags());
      }

      var3.put("tags", new ArrayList(var4));
      return var3;
   }

   @Override
   public boolean accepts(Action var1) {
      return var1 instanceof io.karatelabs.ext.coverage.EmitAction var2 && this.a.equals(var2.a());
   }

   @Override
   public CoverageItem match(Action var1, List<CoverageItem> var2) {
      if (var1 instanceof io.karatelabs.ext.coverage.EmitAction var3) {
         for (CoverageItem var5 : var2) {
            if (var5.id().equals(var3.b())) {
               return var5;
            }
         }
      }

      return null;
   }

   @Override
   public List<Warning> warnings(Action var1, CoverageItem var2, List<CoverageItem> var3) {
      return List.of();
   }

   @Override
   public MatchKind matchConfidence(Action var1, CoverageItem var2, List<CoverageItem> var3) {
      return MatchKind.EXACT;
   }

   @Override
   public List<Warning> loadWarnings(List<CoverageItem> var1) {
      return List.of();
   }

   @Override
   public Map<String, Object> aggregate(List<Action> var1, CoverageItem var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("callCount", var1.size());
      var3.put("avgDurationMs", 0L);
      var3.put("queryParamsUsed", new ArrayList());
      return var3;
   }
}
