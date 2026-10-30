package io.karatelabs.ext.common.run;

import io.karatelabs.core.Runner;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface RunnerEnvironmentPolicy {
   RunnerEnvironmentPolicy a = new RunnerEnvironmentPolicy() {
      @Override
      public String env(String var1) {
         return var1 != null && !var1.isBlank() ? var1.trim() : io.karatelabs.ext.common.run.RunnerEnvironmentPolicy.a();
      }

      @Override
      public UnaryOperator<Runner.Builder> kickoff(String var1) {
         String var2 = this.env(var1);
         return var1x -> {
            if (var2 != null) {
               var1x.karateEnv(var2);
            }

            return var1x.systemEnv(null).outputMask(null);
         };
      }
   };

   String env(String var1);

   UnaryOperator<Runner.Builder> kickoff(String var1);

   static String a() {
      return a(System::getProperty, System::getenv);
   }

   static String a(Function<String, String> var0, Function<String, String> var1) {
      String var2 = a((String)var0.apply("karate.env"));
      return var2 != null ? var2 : a((String)var1.apply("KARATE_ENV"));
   }

   private static String a(String var0) {
      return var0 != null && !var0.isBlank() ? var0.trim() : null;
   }
}
