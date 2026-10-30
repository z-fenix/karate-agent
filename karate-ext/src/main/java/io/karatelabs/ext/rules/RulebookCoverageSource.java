package io.karatelabs.ext.rules;

import io.karatelabs.ext.coverage.CoverageItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RulebookCoverageSource implements io.karatelabs.ext.coverage.CoverageSource {
   private final Path a;
   private final RulebookRepository b;
   private List<CoverageItem> c;

   public RulebookCoverageSource(Path var1) {
      this.a = var1;
      this.b = var1 == null ? null : new RulebookRepository(var1);
   }

   @Override
   public List<CoverageItem> a() {
      if (this.c != null) {
         return this.c;
      } else {
         ArrayList var1 = new ArrayList();
         if (this.b != null) {
            var var2 = this.b.b().iterator();

            label77:
            while (true) {
               String var4;
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5;
               while (true) {
                  if (!var2.hasNext()) {
                     break label77;
                  }

                  io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var3 = (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary)var2.next();
                  var4 = var3.a();

                  try {
                     var5 = this.b.b(var4);
                     break;
                  } catch (RuntimeException var15) {
                  }
               }

               for (Map var7 : var5.f()) {
                  if (var7.get("_id") instanceof String var9) {
                     String var10 = var7.get("_label") instanceof String var11 ? var11 : var9;
                     LinkedHashMap var22 = new LinkedHashMap();
                     var22.put("kind", RulebookRepository.a(var7) ? "rule-reject" : "rule-scenario");
                     var22.put("rule", var4);
                     var22.put("scenarioId", var9);
                     var1.add(new CoverageItem(var4 + "/" + var9, var4 + ": " + var10, Set.of(), null, var22));
                  }
               }

               if (var5.l() != null) {
                  Object var17 = var5.l().get("sequences");
                  if (var17 instanceof List) {
                     for (Object var19 : (List)var17) {
                        if (var19 instanceof Map var20 && var20.get("_id") instanceof String var21 && RulebookRepository.c(var20)) {
                           String var24 = var20.get("_label") instanceof String var13 ? var13 : var21;
                           LinkedHashMap var25 = new LinkedHashMap();
                           var25.put("kind", "rule-sequence");
                           var25.put("rule", var4);
                           var25.put("sequenceId", var21);
                           var1.add(new CoverageItem(var4 + "/" + var21, var4 + ": " + var24, Set.of(), null, var25));
                        }
                     }
                  }
               }
            }
         }

         this.c = var1;
         return var1;
      }
   }

   @Override
   public String b() {
      return "rules";
   }

   @Override
   public Map<String, Object> c() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("title", "Rules");
      if (this.a != null) {
         var1.put("home", this.a.toString());
      }

      return var1;
   }
}
