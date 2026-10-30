package io.karatelabs.ext.robot;

import io.karatelabs.ext.agent.runtime.ToolResult;
import java.util.Optional;

public class UnsavedTextOutputHook implements io.karatelabs.ext.agent.runtime.AgentHook {
   static final int a = 500;

   @Override
   public Optional<String> afterTool(io.karatelabs.ext.agent.runtime.AgentContext var1, String var2, ToolResult var3, String var4) {
      if (var2 != null && var4 != null && var4.length() >= 500) {
         String var5 = d(var2);
         if (var5.contains("bot.text(") && !b(var5)) {
            String var6 = "bot.text() returned "
               + var4.length()
               + " chars but you did NOT save it to a variable — the raw text is trimmed next turn, so it will be GONE. Re-call AND parse in the SAME block, storing what you need as vars (they persist), e.g.: `var rows = bot.text({full:true}).split('\\n').filter(r => r.startsWith('|')); var total = rows.reduce((s,r) => s + (parseFloat(r.split('|')[7])||0), 0); var count = rows.length;`";
            var1.hint("text-not-saved", var6);
            return Optional.of(var6);
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   static boolean b(String var0) {
      for (String var4 : var0.split("[\n;]")) {
         int var5 = var4.indexOf("bot.text(");
         if (var5 >= 0 && c(var4.substring(0, var5))) {
            return true;
         }
      }

      return false;
   }

   private static boolean c(String var0) {
      for (int var1 = 0; var1 < var0.length(); var1++) {
         if (var0.charAt(var1) == '=') {
            char var2 = var1 > 0 ? var0.charAt(var1 - 1) : 32;
            char var3 = var1 + 1 < var0.length() ? var0.charAt(var1 + 1) : 32;
            if (var2 != '=' && var2 != '!' && var2 != '<' && var2 != '>' && var3 != '=') {
               return true;
            }
         }
      }

      return false;
   }

   private static String d(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (String var5 : var0.split("\n", -1)) {
         int var6 = var5.indexOf("//");

         while (var6 > 0 && var5.charAt(var6 - 1) == ':') {
            var6 = var5.indexOf("//", var6 + 2);
         }

         var1.append(var6 >= 0 ? var5.substring(0, var6) : var5).append('\n');
      }

      return var1.toString();
   }
}
