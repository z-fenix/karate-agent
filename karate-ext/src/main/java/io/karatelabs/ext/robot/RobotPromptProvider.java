package io.karatelabs.ext.robot;

public final class RobotPromptProvider {
   private static final io.karatelabs.ext.common.help.PromptTemplateRepository a = io.karatelabs.ext.common.help.PromptTemplateRepository.a("robot");

   private RobotPromptProvider() {
   }

   public static String a() {
      return a.a();
   }

   public static String b() {
      return a.a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind.AUTONOMOUS);
   }

   public static String c() {
      return a.a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind.LIVE);
   }
}
