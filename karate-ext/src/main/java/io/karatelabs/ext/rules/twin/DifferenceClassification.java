package io.karatelabs.ext.rules.twin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

record DifferenceClassification(String component1, boolean component2) {
   static final String a = "ORDER";
   static final String b = "RAW";

   DifferenceClassification(String component1, boolean component2) {
      this.component1 = component1;
      this.component2 = component2;
   }

   static DifferenceClassification a(Map<String, Object> var0, Collection<String> var1) {
      boolean var2 = false;
      boolean var3 = false;
      if (var0 != null) {
         Object var5 = var0.get("differences");
         if (var5 instanceof List) {
            for (Object var6 : (List)var5) {
               if (var6 instanceof Map var7) {
                  boolean var8 = var1.contains(String.valueOf(var7.get("field")));
                  var3 |= var8;
                  var2 |= !var8;
               }
            }
         }
      }

      return new DifferenceClassification(var3 && !var2 ? "RAW" : "ORDER", var3 && var2);
   }

   public String a() {
      return this.component1;
   }

   public boolean b() {
      return this.component2;
   }
}
