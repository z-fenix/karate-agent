package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class ConsoleState implements io.karatelabs.ext.common.run.RunnerEnvironmentPolicy {
   public static final ConsoleState HEADLESS = new ConsoleState(null);
   private volatile io.karatelabs.ext.common.secrets.SecretStore store;
   private String env;
   private int switches;
   private volatile UnaryOperator<String> mask;

   public ConsoleState(io.karatelabs.ext.common.secrets.SecretStore var1) {
      this.store = var1;
   }

   public io.karatelabs.ext.common.secrets.SecretStore store() {
      return this.store;
   }

   public ConsoleState useStore(io.karatelabs.ext.common.secrets.SecretStore var1) {
      if (this == HEADLESS) {
         throw new IllegalStateException("the headless state has no store");
      } else {
         this.store = var1;
         return this;
      }
   }

   public synchronized String env() {
      return this.env != null ? this.env : ProjectConfig.ambientEnv();
   }

   @Override
   public String env(String var1) {
      return var1 != null && !var1.isBlank() ? var1.trim() : this.env();
   }

   synchronized ConsoleState.Snapshot switchEnv(String var1) {
      this.env = var1;
      this.switches++;
      return this.snapshot();
   }

   synchronized boolean switched() {
      return this.env != null;
   }

   public synchronized int switches() {
      return this.switches;
   }

   public synchronized ConsoleState.Snapshot snapshot() {
      return new ConsoleState.Snapshot(this.env(), this.switches);
   }

   public Map<String, String> overlay() {
      return this.store == null ? null : this.store.e();
   }

   public ConsoleState withMask(UnaryOperator<String> var1) {
      if (this == HEADLESS) {
         throw new IllegalStateException("the headless state masks nothing");
      } else {
         this.mask = var1;
         return this;
      }
   }

   @Override
   public UnaryOperator<io.karatelabs.core.Runner.Builder> kickoff(String var1) {
      return this.at(var1)::apply;
   }

   public ConsoleState.Kickoff at(String var1) {
      ConsoleState.Snapshot var2 = this.snapshot();
      var var3 = this.overlay();
      return new ConsoleState.Kickoff(
         var1 != null && !var1.isBlank() ? var1.trim() : var2.env(), var2.switches(), var3 == null ? null : Map.copyOf(var3), this.mask
      );
   }

   public io.karatelabs.core.Runner.Builder apply(io.karatelabs.core.Runner.Builder var1, String var2) {
      return this.kickoff(var2).apply(var1);
   }

   public record Kickoff(String env, int switches, Map<String, String> overlay, UnaryOperator<String> mask) {


      public io.karatelabs.core.Runner.Builder apply(io.karatelabs.core.Runner.Builder var1) {
         if (this.env != null) {
            var1.karateEnv(this.env);
         }

         if (this.mask instanceof SecretRedactor var2) {
            var1.listener(io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.a(var1x -> var2.observeTextForSecrets(Json.stringifyStrict(var1x))));
         }

         return var1.systemEnv(this.overlay).outputMask(this.mask);
      }

      public ConsoleState.Snapshot snapshot() {
         return new ConsoleState.Snapshot(this.env, this.switches);
      }
   }

   public record Snapshot(String env, int switches) {

   }
}
