package io.karatelabs.ext.robot;

import io.karatelabs.common.Json;
import io.karatelabs.driver.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ElementRevealController {
   private static final Logger b = LoggerFactory.getLogger(ElementRevealController.class);
   private static final int c = 8;
   static final String a = "closest candidates below the similarity floor";
   private final BotDriver d;
   private final Driver e;
   private String f;
   private String g;

   ElementRevealController(BotDriver var1, Driver var2) {
      this.d = var1;
      this.e = var2;
   }

   void a() {
      this.f = null;
   }

   String b() {
      return this.f;
   }

   String a(String var1, Object var2) {
      this.g = null;

      try {
         if (var2 != null && this.a(var2)) {
            return var1;
         } else if (this.e.script("window.__kjs.model.revealPlanFor(" + BotDriver.jsString(var1) + ")") instanceof Map var4 && var4.get("owner") != null) {
            this.a(String.valueOf(var4.get("via")), String.valueOf(var4.get("owner")));
            return var1;
         } else {
            this.b(var1);
            return var1;
         }
      } catch (Exception var5) {
         b.debug("revealIfNeeded failed for '{}': {}", var1, var5.getMessage());
         return var1;
      }
   }

   private boolean b(String var1) {
      Object var2;
      try {
         var2 = this.e.script("window.__kjs.model.revealCandidates(" + BotDriver.jsString(var1) + ")");
      } catch (Exception var17) {
         return false;
      }

      if (var2 instanceof List var3 && !var3.isEmpty()) {
         String var4 = this.e.getUrl();
         ArrayList<java.lang.String> var5 = new ArrayList();
         int var6 = 0;

         for (Object var8 : var3) {
            if (var6 >= 8) {
               break;
            }

            if (var8 instanceof Map var9 && var9.get("owner") != null) {
               String var10 = String.valueOf(var9.get("owner"));

               for (String var12 : this.a(String.valueOf(var9.get("via")), Boolean.TRUE.equals(var9.get("mayClick")))) {
                  if (var6 >= 8) {
                     break;
                  }

                  var6++;
                  var5.add(var12 + " " + var10);

                  try {
                     this.d.performAction(var10, var12, null);
                     this.d.waitForStable();
                  } catch (Exception var18) {
                     continue;
                  }

                  if (!this.e.getUrl().equals(var4)) {
                     this.g = "active reveal aborted — probing '" + var12 + " " + var10 + "' navigated the page away";
                     return false;
                  }

                  Object var13 = null;

                  try {
                     var13 = this.e.script("window.__kjs.model.isRevealed(" + BotDriver.jsString(var1) + ")");
                  } catch (Exception var16) {
                  }

                  if (Boolean.TRUE.equals(var13)) {
                     String var14 = var12 + " " + var10;
                     this.f = this.f == null ? var14 : this.f + " → " + var14;
                     this.g = null;
                     return true;
                  }

                  try {
                     this.e.keys().press("\ue00c");
                  } catch (Exception var15) {
                  }
               }
            }
         }

         this.g = "active reveal found no owner for a hidden target (tried: " + String.join("; ", var5) + ")";
         return false;
      } else {
         return false;
      }
   }

   private List<String> a(String var1, boolean var2) {
      if (!var2) {
         return List.of("hover");
      } else {
         return "hover".equals(var1) ? List.of("hover", "click") : List.of("click", "hover");
      }
   }

   private boolean a(Object var1) {
      if (this.e.script("window.__kjs.model.verifyRevealPlan(" + Json.of(var1) + ")") instanceof Map var3 && Boolean.TRUE.equals(var3.get("ok"))) {
         Object var5 = var3.get("reveal");
         if (var5 instanceof List) {
            for (Object var6 : (List)var5) {
               if (var6 instanceof Map var7 && var7.get("owner") != null) {
                  this.a(String.valueOf(var7.get("via")), String.valueOf(var7.get("owner")));
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void a(String var1, String var2) {
      if (!"hover".equals(var1) && !"click".equals(var1)) {
         var1 = "click";
      }

      b.debug("revealIfNeeded: revealing via {} on owner '{}'", var1, var2);
      this.d.performAction(var2, var1, null);
      this.d.waitForStable();
      String var3 = var1 + " " + var2;
      this.f = this.f == null ? var3 : this.f + " → " + var3;
   }

   Map<String, Object> a(String var1) {
      try {
         Object var2 = this.e.script("window.__kjs.model.heal(" + BotDriver.jsString(var1) + ")");
         return var2 instanceof Map ? (Map)var2 : null;
      } catch (Exception var3) {
         b.debug("heal failed for '{}': {}", var1, var3.getMessage());
         return null;
      }
   }

   String a(String var1, Map<String, Object> var2) {
      StringBuilder var3 = new StringBuilder("element not found and no confident match: '").append(var1).append("'");
      if (var2.get("candidates") instanceof List var5 && !var5.isEmpty()) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (Object var8 : var5) {
            if (var8 instanceof Map var9) {
               String var10 = Boolean.TRUE.equals(var9.get("blocked")) ? ", blocked: different identity" : "";
               var6.add("'" + var9.get("locator") + "' (" + var9.get("name") + ", ~" + var9.get("score") + var10 + ")");
            }
         }

         var3.append(" — ").append("closest candidates below the similarity floor").append(": ").append(String.join(", ", var6));
      }

      if (var2.get("reason") != null) {
         var3.append(" — ").append(var2.get("reason"));
      }

      var3.append(". Call look() to see the live elements — the target may have moved or been removed.");
      if (this.g != null) {
         var3.append(" (").append(this.g).append(".)");
      }

      return var3.toString();
   }
}
