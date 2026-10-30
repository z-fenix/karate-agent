package io.karatelabs.ext.agent.runtime;

public final class ControlSignal extends RuntimeException {
   private final io.karatelabs.ext.agent.runtime.ControlSignal.Kind kind;
   private final transient AgentOutcome outcome;

   private ControlSignal(io.karatelabs.ext.agent.runtime.ControlSignal.Kind var1, AgentOutcome var2, String var3) {
      super(var3);
      this.kind = var1;
      this.outcome = var2;
   }

   public io.karatelabs.ext.agent.runtime.ControlSignal.Kind kind() {
      return this.kind;
   }

   public AgentOutcome outcome() {
      return this.outcome;
   }

   public String message() {
      return this.getMessage();
   }

   public static ControlSignal stop(Object var0) {
      return stop(io.karatelabs.ext.agent.runtime.AgentOutcome.success(var0));
   }

   public static ControlSignal stop(String var0) {
      return stop(io.karatelabs.ext.agent.runtime.AgentOutcome.failure(var0));
   }

   public static ControlSignal stop(AgentOutcome var0) {
      if (var0 == null) {
         throw new IllegalArgumentException("ControlSignal.stop requires an Outcome");
      } else {
         return new ControlSignal(io.karatelabs.ext.agent.runtime.ControlSignal.Kind.STOP, var0, var0.message());
      }
   }

   public static ControlSignal suspend(String var0) {
      return new ControlSignal(io.karatelabs.ext.agent.runtime.ControlSignal.Kind.SUSPEND, null, var0);
   }

   public static ControlSignal hint(String var0) {
      return new ControlSignal(io.karatelabs.ext.agent.runtime.ControlSignal.Kind.HINT, null, var0);
   }

   public static ControlSignal replan(String var0) {
      return new ControlSignal(io.karatelabs.ext.agent.runtime.ControlSignal.Kind.REPLAN, null, var0);
   }

   public static ControlSignal a(Throwable var0) {
      Throwable var1 = var0;

      for (int var2 = 0; var1 != null && var2 < 32; var2++) {
         if (var1 instanceof ControlSignal var3) {
            return var3;
         }

         if (var1.getCause() == var1) {
            break;
         }

         var1 = var1.getCause();
      }

      return null;
   }

   @Override
   public synchronized Throwable fillInStackTrace() {
      return this;
   }

   public static enum Kind {
      STOP,
      SUSPEND,
      HINT,
      REPLAN;
   }
}
