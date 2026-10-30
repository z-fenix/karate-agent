package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.run.MaxWorkspace;
import io.karatelabs.ext.robot.MatchBinding;
import io.karatelabs.js.Engine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class CheckToolkit implements AgentExtension {
   private final Path root;
   private AgentContext ctx;
   private String aiNote;
   private File.Watcher watcher;
   private Check.Verdict verdict;
   private TriageGate triage;
   private ConsoleState console = new ConsoleState(null);

   public CheckToolkit(Path var1) {
      this.root = var1;
   }

   public CheckToolkit withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   public CheckToolkit withAiNote(String var1) {
      this.aiNote = var1;
      return this;
   }

   public CheckToolkit withWatcher(File.Watcher var1) {
      this.watcher = var1;
      return this;
   }

   public CheckToolkit withVerdict(Check.Verdict var1) {
      this.verdict = var1;
      return this;
   }

   public CheckToolkit withTriage(TriageGate var1) {
      this.triage = var1;
      return this;
   }

   @Override
   public void init(AgentContext var1) {
      this.ctx = var1;
   }

   @Override
   public Map<String, Object> bindings() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      File var2 = File.at(this.root).withAiNote(this.aiNote).withWatcher(this.watcher).withConsole(this.console).withTriage(this.triage);
      var1.put("File", var2);
      Engine var3 = this.ctx != null && this.ctx.toolExecutor() instanceof io.karatelabs.ext.agent.runtime.JavaScriptExecutor var4 ? var4.b() : null;
      Check var6 = var3 == null ? null : Check.at(MaxWorkspace.checksDir(this.root), var3).withFile(var2).onVerdict(this.verdict).withConsole(this.console);
      var1.put("match", new MatchBinding().onAssertion(var2x -> {
         io.karatelabs.ext.common.EvidenceReporter.b(var2x);
         if (this.ctx != null) {
            this.ctx.assertion(var2x);
         }

         if (var6 != null) {
            var6.asserted();
         }
      }));
      if (var6 != null) {
         var1.put("Check", var6);
         var2.withEngine(var3);
      }

      return var1;
   }

   @Override
   public String promptFragment() {
      Path var1 = MaxWorkspace.checksDir(this.root);
      if (!Files.isDirectory(var1)) {
         return "";
      } else {
         var var2 = listJs(var1);
         var var3 = listMd(var1);
         if (var2.isEmpty() && var3.isEmpty()) {
            return "";
         } else {
            StringBuilder var4 = new StringBuilder("## Checks — deterministic macros + knowledge notes\n\n");
            if (!var2.isEmpty()) {
               var4.append("Reusable, validated browser macros. PREFER replaying one over re-deriving its steps:\n");
               var4.append("`Check.run('<name>', arg?)` runs it on your `bot` (throws a decorated error on failure).\n");

               for (Map var6 : (Iterable<Map>) (Iterable<?>) (var2)) {
                  var4.append("- `").append(var6.get("name")).append('`');
                  Object var7 = var6.get("description");
                  if (var7 != null && !String.valueOf(var7).isBlank()) {
                     var4.append(" — ").append(var7);
                  }

                  if (var6.get("params") instanceof List var9 && !var9.isEmpty()) {
                     var4.append(" (params: ").append(String.join(", ", var9.stream().map(String::valueOf).toList())).append(')');
                  }

                  var4.append('\n');
               }
            }

            if (!var3.isEmpty()) {
               var4.append("\nKnowledge notes (user-authored advice — credentials/accounts to use, app quirks). ");
               var4.append("Read with `File.read('checks/<name>')` BEFORE driving the app they describe:\n");

               for (String var11 : (Iterable<String>) (Iterable<?>) (var3)) {
                  var4.append("- `").append(var11).append("`\n");
               }
            }

            return var4.toString();
         }
      }
   }

   private static List<Map<String, Object>> listJs(Path var0) {
      return Check.at(var0, null).list(null) instanceof List var2 ? var2 : List.of();
   }

   private static List<String> listMd(Path var0) {
      try {
         List var2;
         try (var var1 = Files.walk(var0)) {
            var2 = var1.filter(var0x -> Files.isRegularFile(var0x) && var0x.toString().endsWith(".md"))
               .sorted()
               .map(var1x -> "checks/" + var0.relativize(var1x).toString().replace('\\', '/'))
               .toList();
         }

         return var2;
      } catch (Exception var6) {
         return List.of();
      }
   }
}
