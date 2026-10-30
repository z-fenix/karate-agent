package io.karatelabs.ext.agent.runtime;

public record AgentOutcome(AgentOutcome.Kind kind, Object value, String message) {
   public static AgentOutcome success(Object var0) {
      return new AgentOutcome(AgentOutcome.Kind.SUCCESS, var0, null);
   }

   public static AgentOutcome failure(String var0) {
      return new AgentOutcome(AgentOutcome.Kind.FAILURE, null, var0);
   }

   public static AgentOutcome stuck(String var0) {
      return new AgentOutcome(AgentOutcome.Kind.STUCK, null, var0);
   }

   public static AgentOutcome terminated(String var0) {
      return new AgentOutcome(AgentOutcome.Kind.TERMINATED, null, var0);
   }

   public static AgentOutcome blocked(Object var0, String var1) {
      return new AgentOutcome(AgentOutcome.Kind.BLOCKED, var0, var1);
   }

   public boolean isSuccess() {
      return this.kind == AgentOutcome.Kind.SUCCESS;
   }

   public AgentRunState.Status status() {
      return this.kind.status();
   }

   public static enum Kind {
      SUCCESS(AgentRunState.Status.COMPLETED),
      FAILURE(AgentRunState.Status.FAILED),
      STUCK(AgentRunState.Status.STUCK),
      TERMINATED(AgentRunState.Status.TERMINATED),
      BLOCKED(AgentRunState.Status.BLOCKED);

      private final AgentRunState.Status status;

      private Kind(AgentRunState.Status var3) {
         this.status = var3;
      }

      public AgentRunState.Status status() {
         return this.status;
      }
   }
}
