package io.karatelabs.ext.robot;

import io.karatelabs.ext.agent.runtime.ToolResult;
import java.util.Optional;

public class StopLookingHook implements io.karatelabs.ext.agent.runtime.AgentHook {
   private int a;

   @Override
   public Optional<String> afterTool(io.karatelabs.ext.agent.runtime.AgentContext var1, String var2, ToolResult var3, String var4) {
      if (var4 != null && var4.contains("No changes since last look()")) {
         this.a++;
         if (this.a >= 2) {
            String var5 = "STOP LOOKING. "
               + this.a
               + " consecutive look()s returned no page changes. The page is static. Pick a locator from your previous full look() and act() on it, scroll to reveal new fields, or call Robot.success()/Robot.failure() if stuck.";
            var1.hint("stop-looking", var5);
            return Optional.of(var5);
         }
      } else {
         this.a = 0;
      }

      return Optional.empty();
   }
}
