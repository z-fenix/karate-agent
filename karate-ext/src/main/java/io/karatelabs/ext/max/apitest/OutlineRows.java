package io.karatelabs.ext.max.apitest;

import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.ScenarioOutline;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class OutlineRows {
   private OutlineRows() {
   }

   public static boolean addressable(ScenarioOutline var0) {
      for (ExamplesTable var2 : var0.getExamplesTables()) {
         if (var2.getTable() == null || var2.getTable().isDynamic()) {
            return false;
         }
      }

      return true;
   }

   static int rows(ExamplesTable var0) {
      return var0.getTable() == null ? 0 : Math.max(0, var0.getTable().getRows().size() - 1);
   }

   static int rowOf(Scenario var0) {
      return var0.isOutlineExample() && var0.getSection() != null && var0.getSection().isOutline()
         ? examplesRow(var0.getSection().getScenarioOutline(), var0.getLine(), var0.getExampleIndex())
         : -1;
   }

   static int examplesRow(ScenarioOutline var0, int var1, int var2) {
      int var3 = 0;

      for (ExamplesTable var5 : var0.getExamplesTables()) {
         if (var5.getLine() == var1) {
            return var3 + var2;
         }

         var3 += rows(var5);
      }

      return var2;
   }

   public static OutlineRows.Bound bind(ScenarioOutline var0, int var1) {
      if (var1 >= 0 && addressable(var0)) {
         int var2 = 0;

         for (ExamplesTable var4 : var0.getExamplesTables()) {
            int var5 = rows(var4);
            if (var1 < var2 + var5) {
               int var6 = var1 - var2;
               Scenario var7 = var0.toScenario(null, var6, var4.getLine(), var4.getTags());
               var var8 = var4.getTable().getExampleData(var6);
               var var9 = var4.getTable().getRawExampleData(var6);
               LinkedHashMap<String, String> var10 = new LinkedHashMap<>();

               for (String var12 : (Iterable<String>) (Iterable<?>) (var8.keySet())) {
                  var10.put(var12, var9.get(var12) == null ? "" : (String)var9.get(var12));
               }

               var7.setExampleData(var8);
               var10.forEach((var1x, var2x) -> {
                  var7.replace("<" + var1x + ">", var2x);
                  var7.replace("${" + var1x + "}", var2x);
               });
               return new OutlineRows.Bound(var7, var10);
            }

            var2 += var5;
         }

         return null;
      } else {
         return null;
      }
   }

   public record Bound(Scenario scenario, Map<String, String> values) {


      public String apply(String var1) {
         if (var1 == null) {
            return null;
         } else {
            for (Entry var3 : this.values.entrySet()) {
               var1 = var1.replace("<" + (String)var3.getKey() + ">", (CharSequence)var3.getValue())
                  .replace("${" + (String)var3.getKey() + "}", (CharSequence)var3.getValue());
            }

            return var1;
         }
      }
   }
}
