package io.karatelabs.ext.robot;

import io.karatelabs.ext.agent.runtime.ToolResult;
import java.util.Optional;

public class LocatorFailureHook implements io.karatelabs.ext.agent.runtime.AgentHook {
   private String a;
   private int b;

   @Override
   public Optional<String> afterTool(io.karatelabs.ext.agent.runtime.AgentContext var1, String var2, ToolResult var3, String var4) {
      if (var4 == null) {
         return Optional.empty();
      } else {
         boolean var5 = var4.startsWith("Error:");
         if (!var5) {
            this.a = null;
            this.b = 0;
            return Optional.empty();
         } else {
            StringBuilder var6 = new StringBuilder();
            String var7 = b(var4);
            if (var7 != null && var7.equals(this.a)) {
               this.b++;
               if (this.b >= 2) {
                  String var8 = "STUCK: '"
                     + var7
                     + "' has failed "
                     + (this.b + 1)
                     + " times. The locator is WRONG — bare text like 'Transactions' won't work. You MUST call look({full: true}) and copy the EXACT locator string (e.g., '{div}Transactions' or '#some-id') from the response.";
                  var1.hint("stuck-locator", var8);
                  a(var6, var8);
               }
            } else {
               this.a = var7;
               this.b = 0;
            }

            if (var4.contains("CDP timeout") || var4.contains("Runtime.evaluate") || var4.contains("ConnectionClosedException")) {
               String var9 = "Page is heavy or still loading. Try: bot.wait('idle'); look(); or define a helper that targets a small selector instead of dumping the full page, e.g. function getStat(label) { return bot.eval('document.querySelector(' + JSON.stringify(label) + ')?.textContent'); }";
               var1.hint("cdp-timeout", var9);
               a(var6, var9);
            }

            return var6.length() > 0 ? Optional.of(var6.toString()) : Optional.empty();
         }
      }
   }

   private static void a(StringBuilder var0, String var1) {
      if (var0.length() > 0) {
         var0.append("\n⚠ ");
      }

      var0.append(var1);
   }

   static String b(String var0) {
      if (var0 != null && !var0.contains("\nparser state: ")) {
         for (int var1 = var0.indexOf("act('"); var1 >= 0; var1 = var0.indexOf("act('", var1 + 1)) {
            int var2 = var1 + 5;
            int var3 = var0.indexOf("'", var2);
            if (var3 < 0) {
               return null;
            }

            String var4 = var0.substring(var2, var3);
            if (!var4.startsWith("<") || !var4.endsWith(">")) {
               return var4;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
