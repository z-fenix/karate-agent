package io.karatelabs.ext.agent.runtime;

import java.util.Optional;

public final class DeadlineExitCondition implements ExitCondition {
   private final long a;
   private final long b;

   public DeadlineExitCondition(long var1) {
      if (var1 <= 0L) {
         throw new IllegalArgumentException("deadlineMs must be positive: " + var1);
      } else {
         this.a = var1;
         this.b = System.currentTimeMillis();
      }
   }

   @Override
   public Optional<AgentOutcome> check(AgentRunState var1) {
      long var2 = System.currentTimeMillis() - this.b;
      return var2 >= this.a ? Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.terminated("deadline exceeded: " + var2 + "ms elapsed >= " + this.a + "ms")) : Optional.empty();
   }
}
