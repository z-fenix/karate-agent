package io.karatelabs.ext.coverage;

import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface CoverageSource {
   List<CoverageItem> a();

   String b();

   Map<String, Object> c();

   default String d() {
      Object var1 = this.c().get("title");
      return var1 != null ? var1.toString() : "Unknown";
   }

   default String e() {
      Object var1 = this.c().get("version");
      return var1 != null ? var1.toString() : null;
   }

   default String f() {
      return this.b();
   }

   default boolean a(Action var1) {
      return var1 instanceof EmitAction var2 && this.b().equals(var2.a());
   }

   default CoverageItem b(Action var1) {
      if (var1 instanceof EmitAction var2) {
         for (CoverageItem var4 : this.a()) {
            if (var4.id().equals(var2.b())) {
               return var4;
            }
         }
      }

      return null;
   }

   default List<Warning> a(Action var1, CoverageItem var2) {
      return List.of();
   }

   default MatchKind b(Action var1, CoverageItem var2) {
      return MatchKind.EXACT;
   }

   default List<Warning> g() {
      return List.of();
   }

   default Map<String, Object> a(List<Action> var1, CoverageItem var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("callCount", var1.size());
      var3.put("avgDurationMs", 0L);
      var3.put("queryParamsUsed", new ArrayList());
      return var3;
   }
}
