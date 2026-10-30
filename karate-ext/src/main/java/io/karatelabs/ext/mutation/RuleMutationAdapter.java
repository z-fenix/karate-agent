package io.karatelabs.ext.mutation;

import io.karatelabs.core.Runner;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.ext.rules.MutationProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class RuleMutationAdapter implements MutationProvider {
   private static final RuleMutationAdapter INSTANCE = new RuleMutationAdapter();

   public static void install() {
      Rule.withMutations(INSTANCE);
   }

   public Map<String, Object> runSelfMutation(Path var1, String var2, Path var3, Object var4) {
      return io.karatelabs.ext.mutation.RulebookSelfMutationRunner.runWithOptions(var1, var2, var3, var4);
   }

   public Map<String, Object> runSuiteMutation(
      Path var1,
      String var2,
      String var3,
      String var4,
      List<String> var5,
      Path var6,
      Map<String, String> var7,
      String var8,
      Object var9,
      Supplier<UnaryOperator<Runner.Builder>> var10
   ) {
      io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions var11 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions(var1, var2, var3, var4, var5, var6, var7, Set.of(), 120000L);
      if (var8 != null) {
         var11 = var11.withHandlerOverlay(var8);
      }

      return io.karatelabs.ext.mutation.SuiteMutationRunner.run(var11.withMockArg(var9).withRunnerConfig((UnaryOperator<Runner.Builder>)var10.get()));
   }

   private RuleMutationAdapter() {
   }
}
