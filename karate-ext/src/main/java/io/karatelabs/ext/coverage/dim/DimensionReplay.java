package io.karatelabs.ext.coverage.dim;

import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.ext.coverage.graph.TraceGraph;
import java.util.Map;

public final class DimensionReplay {
   private DimensionReplay() {
   }

   public static void a(TraceGraph var0, DimensionHandlers var1) {
      if (var1 != null && !var1.a()) {
         var0.a(var1x -> {
            if (var1x.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var1x.d() instanceof HttpAction var2) {
               var var4 = var1.a(var2);
               if (var4 != null && !var4.isEmpty()) {
                  return var1x.b(var4);
               }
            }

            return var1x;
         });
      }
   }
}
