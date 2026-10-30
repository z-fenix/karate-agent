package io.karatelabs.ext.robot;

import io.karatelabs.ext.agent.runtime.ToolResult;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamePageStallHook implements io.karatelabs.ext.agent.runtime.AgentHook {
   static final int a = 5;
   private static final Pattern b = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
   private String c;
   private int d;

   @Override
   public Optional<String> afterTool(io.karatelabs.ext.agent.runtime.AgentContext var1, String var2, ToolResult var3, String var4) {
      String var5 = b(var4);
      if (var5 == null) {
         return Optional.empty();
      } else {
         if (var5.equals(this.c)) {
            if (c(var2)) {
               this.d++;
               if (this.d >= 5) {
                  String var6 = "STUCK ON PAGE. "
                     + this.d
                     + " iterations on "
                     + var5
                     + " with no act() or Robot.success/failure. Either: (a) pick a locator from your last look() and act() on it, (b) define a helper function once and reuse it across the data you need, or (c) call Robot.failure('what blocked progress, which locator/value missing') to exit cleanly.";
                  var1.hint("stuck-on-page", var6);
                  return Optional.of(var6);
               }
            } else {
               this.d = 0;
            }
         } else {
            this.c = var5;
            this.d = c(var2) ? 1 : 0;
         }

         return Optional.empty();
      }
   }

   static String b(String var0) {
      if (var0 == null) {
         return null;
      } else {
         Matcher var1 = b.matcher(var0);
         return var1.find() ? var1.group(1) : null;
      }
   }

   static boolean c(String var0) {
      if (var0 == null) {
         return true;
      } else {
         String var1 = var0.replaceAll("(?m)//[^\n]*", "");
         return !var1.contains("act(")
            && !var1.contains("Robot.success")
            && !var1.contains("Robot.failure")
            && !var1.contains("Robot.step")
            && !var1.contains(".go(");
      }
   }
}
