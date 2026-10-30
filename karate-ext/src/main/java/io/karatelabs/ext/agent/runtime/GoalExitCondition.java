package io.karatelabs.ext.agent.runtime;

import io.karatelabs.common.Json;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import java.util.LinkedHashMap;
import java.util.Optional;

public final class GoalExitCondition implements ExitCondition {
   private final String a;
   private final String b;

   public GoalExitCondition(String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         try {
            Engine.parse(var2);
         } catch (Exception var4) {
            throw new IllegalArgumentException("goal expr does not parse: " + var4.getMessage());
         }

         this.a = var1 != null && !var1.isBlank() ? var1 : "goal";
         this.b = var2;
      } else {
         throw new IllegalArgumentException(
            "goal expr is required — a JS predicate over `result` (the last tool step's structured value), e.g. \"result && result.failed === 0\""
         );
      }
   }

   public String a() {
      return this.a;
   }

   @Override
   public Optional<AgentOutcome> check(AgentRunState var1) {
      return Optional.empty();
   }

   @Override
   public Optional<AgentOutcome> a(AgentRunState var1, ToolStep var2) {
      if (var2 != null && var2.result() != null && var2.result().value() != null) {
         Object var3;
         try {
            var3 = Json.of(Json.stringifyStrict(var2.result().value())).value();
         } catch (Exception var7) {
            return Optional.of(
               io.karatelabs.ext.agent.runtime.AgentOutcome.terminated(
                  "goal '"
                     + this.a
                     + "' could not read the step result (not wire-serializable: "
                     + var7.getMessage()
                     + ") — the pre-registered goal is never silently skipped; make the exit verb return plain data"
               )
            );
         }

         Object var4;
         try {
            Engine var5 = new Engine();
            var5.put("result", var3);
            var4 = var5.eval(this.b);
         } catch (Exception var6) {
            return Optional.of(
               io.karatelabs.ext.agent.runtime.AgentOutcome.terminated(
                  "goal '"
                     + this.a
                     + "' failed to evaluate: "
                     + var6.getMessage()
                     + " — fix the goal expr and re-run (a pre-registered goal that cannot evaluate is never silently skipped)"
               )
            );
         }

         if (!Terms.isTruthy(var4)) {
            return Optional.empty();
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            var8.put("goal", this.a);
            var8.put("iteration", var1.iterations());
            var8.put("result", var3);
            return Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.success(var8));
         }
      } else {
         return Optional.empty();
      }
   }
}
