package io.karatelabs.ext.rules;

import io.karatelabs.core.Runner.Builder;
import io.karatelabs.ext.common.jsapi.JsApi;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface MutationProvider {
   MutationProvider a = new MutationProvider() {
      @Override
      public Map<String, Object> runSelfMutation(Path var1, String var2, Path var3, Object var4) {
         return this.a();
      }

      @Override
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
         Supplier<UnaryOperator<Builder>> var10
      ) {
         return this.a();
      }

      private Map<String, Object> a() {
         return JsApi.missing("Rule.mutate is not available here: no mutation engine is bound");
      }
   };

   Map<String, Object> runSelfMutation(Path var1, String var2, Path var3, Object var4);

   Map<String, Object> runSuiteMutation(
      Path var1,
      String var2,
      String var3,
      String var4,
      List<String> var5,
      Path var6,
      Map<String, String> var7,
      String var8,
      Object var9,
      Supplier<UnaryOperator<Builder>> var10
   );
}
