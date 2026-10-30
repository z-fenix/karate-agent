package io.karatelabs.ext.robot;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class RobotHarnessFactory {
   private RobotHarnessFactory() {
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a() {
      return a(null);
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a(Path var0) {
      return a(var0, null);
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a(Path var0, UnaryOperator<String> var1) {
      return a(var0, var1, null);
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a(Path var0, UnaryOperator<String> var1, AtomicBoolean var2) {
      return a(var0, var1, var2, null);
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a(Path var0, UnaryOperator<String> var1, AtomicBoolean var2, Supplier<String> var3) {
      return a(var0, var1, var2, var3, null);
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder a(Path var0, UnaryOperator<String> var1, AtomicBoolean var2, Supplier<String> var3, Function<String, String> var4) {
      RobotAgentExtension var5 = new RobotAgentExtension().a(var4);
      if (var0 != null) {
         var5.a(var0);
      }

      if (var1 != null) {
         var5.a(var1);
      }

      if (var2 != null) {
         var5.a(var2);
      }

      var5.a(var3);
      return io.karatelabs.ext.agent.harness.AgentHarness.builder().systemPrompt(io.karatelabs.ext.robot.RobotPromptProvider.b()).historyWindow(4).addExtension(var5);
   }
}
