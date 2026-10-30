package io.karatelabs.ext.common.run;

import io.karatelabs.js.Engine;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface EngineRuntimeProfile {
   String name();

   default void stop() {
   }

   List<NamespaceBinding> namespaces(Engine var1);

   default String a() {
      return "serve";
   }

   default Map<String, Path> artifactRoots() {
      return Map.of();
   }

   default Map<String, Object> templateGlobals() {
      return Map.of();
   }

   default void onEvalStart(String var1) {
   }

   default void onEval(String var1, String var2) {
   }

   default void apply(Engine var1) {
      var var2 = this.namespaces(var1);

      for (NamespaceBinding var4 : (Iterable<NamespaceBinding>) (Iterable<?>) (var2)) {
         var1.putRootBinding(var4.a(), var4.b());
         if (var4.d() != null) {
            var4.d().accept(var1);
         }
      }

      var1.putRootBinding("help", io.karatelabs.ext.common.run.NamespaceHelpBuilder.a(var2));
   }

   default String reanchorBlockedReason() {
      return null;
   }

   default String a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind var1) {
      io.karatelabs.ext.common.help.PromptTemplateRepository var2 = io.karatelabs.ext.common.help.PromptTemplateRepository.a(this.a());
      return var2.a(var1) + "\n\n" + var2.a();
   }
}
