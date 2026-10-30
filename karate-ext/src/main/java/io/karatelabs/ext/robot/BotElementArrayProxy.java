package io.karatelabs.ext.robot;

import io.karatelabs.driver.Locators;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BotElementArrayProxy implements SimpleObject {
   private final List<String> locators;
   private final BotDriver agentDriver;

   public BotElementArrayProxy(List<String> var1, BotDriver var2) {
      this.locators = (List<String>)(var1 != null ? new ArrayList<>(var1) : List.of());
      this.agentDriver = var2;
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "locators" -> new ArrayList(this.locators);
         case "length" -> this.locators.size();
         case "eval" -> (io.karatelabs.js.JavaInvokable) this::evalAll;
         case "first" -> (io.karatelabs.js.JavaInvokable) var1x -> this.first();
         case "last" -> (io.karatelabs.js.JavaInvokable) var1x -> this.last();
         case "at" -> (io.karatelabs.js.JavaInvokable) this::at;
         default -> null;
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("locators", "length", "eval", "first", "last", "at");
   }

   private Object evalAll(Object... var1) {
      if (var1.length < 1) {
         return this.errorResult("eval requires function argument");
      } else if (this.locators.isEmpty()) {
         return List.of();
      } else {
         String var2 = Locators.toFunction(var1[0]);

         try {
            StringBuilder var3 = new StringBuilder();
            var3.append("(function() { var els = [");

            for (int var4 = 0; var4 < this.locators.size(); var4++) {
               if (var4 > 0) {
                  var3.append(",");
               }

               var3.append("document.querySelector('").append(this.escapeJs(this.locators.get(var4))).append("')");
            }

            var3.append("]; var fn = ").append(var2).append("; return els.map(fn); })()");
            return this.agentDriver.getDriver().script(var3.toString());
         } catch (Exception var5) {
            return this.errorResult("eval failed: " + var5.getMessage());
         }
      }
   }

   private Object first() {
      return this.locators.isEmpty() ? null : new BotElementProxy(this.locators.get(0), this.agentDriver);
   }

   private Object last() {
      return this.locators.isEmpty() ? null : new BotElementProxy(this.locators.get(this.locators.size() - 1), this.agentDriver);
   }

   private Object at(Object... var1) {
      if (var1.length < 1) {
         return this.errorResult("at requires index argument");
      } else {
         int var2 = ((Number)var1[0]).intValue();
         return var2 >= 0 && var2 < this.locators.size() ? new BotElementProxy(this.locators.get(var2), this.agentDriver) : null;
      }
   }

   private String escapeJs(String var1) {
      return var1.replace("\\", "\\\\").replace("'", "\\'");
   }

   private Map<String, Object> errorResult(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("error", var1);
      return var2;
   }

   public List<String> getLocators() {
      return new ArrayList<>(this.locators);
   }
}
