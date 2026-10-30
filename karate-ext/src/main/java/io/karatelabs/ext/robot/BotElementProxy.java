package io.karatelabs.ext.robot;

import io.karatelabs.driver.Locators;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BotElementProxy implements SimpleObject {
   private final String locator;
   private final BotDriver agentDriver;

   public BotElementProxy(String var1, BotDriver var2) {
      this.locator = var1;
      this.agentDriver = var2;
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "locator" -> this.locator;
         case "eval" -> (io.karatelabs.js.JavaInvokable) this::evalElement;
         case "parents" -> (io.karatelabs.js.JavaInvokable) this::getParents;
         case "parent" -> (io.karatelabs.js.JavaInvokable) var1x -> this.findRelative("parentElement");
         case "children" -> (io.karatelabs.js.JavaInvokable) var1x -> this.findRelativeAll("children");
         case "nextSibling" -> (io.karatelabs.js.JavaInvokable) var1x -> this.findRelative("nextElementSibling");
         case "previousSibling" -> (io.karatelabs.js.JavaInvokable) var1x -> this.findRelative("previousElementSibling");
         default -> null;
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("locator", "eval", "parents", "parent", "children", "nextSibling", "previousSibling");
   }

   private Object evalElement(Object... var1) {
      if (var1.length < 1) {
         return this.errorResult("eval requires function argument");
      } else {
         String var2 = Locators.toFunction(var1[0]);
         String[] var3 = BotDriver.parseFrameLocator(this.locator);
         String var4 = var3[0];
         String var5 = var3[1];

         try {
            if (var4 != null) {
               this.agentDriver.getDriver().switchFrame(var4);
            }

            Object var6;
            try {
               var6 = this.agentDriver.getDriver().script(var5, var2);
            } finally {
               if (var4 != null) {
                  try {
                     this.agentDriver.getDriver().switchFrame(null);
                  } catch (Exception var14) {
                  }
               }
            }

            return var6;
         } catch (Exception var16) {
            return this.errorResult("eval failed: " + var16.getMessage());
         }
      }
   }

   private Object getParents(Object... var1) {
      int var2 = 5;
      if (var1.length > 0 && var1[0] instanceof Number) {
         var2 = ((Number)var1[0]).intValue();
      }

      String[] var3 = BotDriver.parseFrameLocator(this.locator);
      String var4 = var3[0];
      String var5 = var3[1];

      try {
         if (var4 != null) {
            this.agentDriver.getDriver().switchFrame(var4);
         }

         Object var7;
         try {
            this.agentDriver.injectAgentLookJs();
            String var6 = var5.replace("\\", "\\\\").replace("'", "\\'");
            var7 = this.agentDriver.getDriver().script("window.__kjs.look.getParents('" + var6 + "', " + var2 + ")");
         } finally {
            if (var4 != null) {
               try {
                  this.agentDriver.getDriver().switchFrame(null);
               } catch (Exception var15) {
               }
            }
         }

         return var7;
      } catch (Exception var17) {
         return this.errorResult("parents failed: " + var17.getMessage());
      }
   }

   private Object findRelative(String var1) {
      try {
         Boolean var2 = (Boolean)this.agentDriver.getDriver().script(this.locator, "_." + var1 + " !== null");
         if (!Boolean.TRUE.equals(var2)) {
            return null;
         } else {
            String var3 = this.locator + " > " + var1;
            return new BotElementProxy(var3, this.agentDriver);
         }
      } catch (Exception var4) {
         return null;
      }
   }

   private Object findRelativeAll(String var1) {
      try {
         String var2 = this.locator + " > *";
         Number var3 = (Number)this.agentDriver.getDriver().script("document.querySelectorAll('" + var2.replace("'", "\\'") + "').length");
         int var4 = var3 != null ? var3.intValue() : 0;
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (int var6 = 0; var6 < var4; var6++) {
            var5.add(var2 + ":nth-child(" + (var6 + 1) + ")");
         }

         return new BotElementArrayProxy(var5, this.agentDriver);
      } catch (Exception var7) {
         return new BotElementArrayProxy(List.of(), this.agentDriver);
      }
   }

   private Map<String, Object> errorResult(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("error", var1);
      return var2;
   }

   public String getLocator() {
      return this.locator;
   }
}
