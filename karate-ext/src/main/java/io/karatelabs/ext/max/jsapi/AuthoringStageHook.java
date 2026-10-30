package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.ToolResult;
import java.util.Optional;

public class AuthoringStageHook implements AgentHook {
   static final int STAGE_THRESHOLD = 6;
   private int liveStepsSinceCheck;
   private int lastStageNudgeAt = -1;

   @Override
   public Optional<String> afterTool(AgentContext var1, String var2, ToolResult var3, String var4) {
      if (var2 == null) {
         return Optional.empty();
      } else if (var2.contains("Check.run(")) {
         int var7 = this.liveStepsSinceCheck;
         this.liveStepsSinceCheck = 0;
         this.lastStageNudgeAt = -1;
         if (var7 >= 6 && !var2.contains("reset")) {
            String var6 = "You ran the check after driving "
               + var7
               + " live steps, from the current mid-flow state. For a full/end-to-end verify, run it from a CLEAN browser: Check.run('<name>.js', null, {reset:true}) (wipes cookies/storage → the true cold-start path, the way it runs for real). Plain Check.run (no reset) is only for troubleshooting ONE stage from where you already are.";
            var1.hint("author-reset", var6);
            return Optional.of(var6);
         } else {
            return Optional.empty();
         }
      } else {
         if (var2.contains("bot.act(") && !var2.contains("File.write(")) {
            this.liveStepsSinceCheck++;
            if (this.liveStepsSinceCheck >= 6 && (this.lastStageNudgeAt < 0 || this.liveStepsSinceCheck - this.lastStageNudgeAt >= 3)) {
               this.lastStageNudgeAt = this.liveStepsSinceCheck;
               String var5 = "STAGE NOW. You've driven "
                  + this.liveStepsSinceCheck
                  + " live steps without a Check.run. Codify the steps you've PROVEN into checks/<name>.js (copy each locator VERBATIM from what you just drove) and Check.run a prefix to GREEN before driving further. Don't keep driving toward a 20-step monolith: a bug then costs a full re-drive to find, and context older than the last turn is compacted (a trimmed look()'s locators become a count), so a locator you codify late is reconstructed from a degraded breadcrumb — wrong. The FILE is your durable memory (immune to trimming); persistent variables you define are echoed back across trims too.";
               var1.hint("author-stage", var5);
               return Optional.of(var5);
            }
         }

         return Optional.empty();
      }
   }
}
