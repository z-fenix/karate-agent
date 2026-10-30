package io.karatelabs.ext.rules;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleArmCoverageCatalog {
   private RuleArmCoverageCatalog() {
   }

   public static List<Map<String, Object>> a(Path var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         RulebookRepository var2 = new RulebookRepository(var0);

         for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var4 : var2.b()) {
            String var5 = var4.a();

            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var6;
            try {
               var6 = var2.b(var5);
            } catch (RuntimeException var12) {
               continue;
            }

            Map var7;
            try {
               var7 = RuleAnalysisEngine.c(var6);
            } catch (RuntimeException var11) {
               continue;
            }

            List var8 = var7.get("arms") instanceof List var9 ? var9 : List.of();
            LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
            var13.put("rule", var5);
            var13.put("used", var7.get("used"));
            var13.put("reachable", var7.get("reachable"));
            var13.put("total", var7.get("total"));
            var13.put("notclaimed", var7.get("notclaimed"));
            var13.put("arms", var8);
            var1.add(var13);
         }

         return var1;
      }
   }
}
