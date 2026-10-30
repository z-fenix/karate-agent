package io.karatelabs.ext.coverage.collect;

import java.nio.file.Path;
import java.util.List;

public interface RunArtifactParser {
   String a();

   String b();

   TestExecution a(Path var1);

   io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(String var1, ExecutionInventory var2);

   default boolean c() {
      return false;
   }

   static List<RunArtifactParser> d() {
      return List.of(io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.d, io.karatelabs.ext.coverage.collect.ProofArtifactParser.d);
   }

   static RunArtifactParser a(String var0) {
      if (var0 != null) {
         for (RunArtifactParser var2 : d()) {
            if (var0.startsWith(var2.a())) {
               return var2;
            }
         }
      }

      return null;
   }
}
