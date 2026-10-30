package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.CoverageProjectBuilder;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.ext.rules.Schema;
import io.karatelabs.ext.rules.twin.Twin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class RulesToolkit implements AgentExtension {
   private final Path root;
   private final Path rulesHome;
   private final Path reportDir;
   private final List<Path> requirementsHomes;
   private boolean readOnly;
   private Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> projectBuilder;
   private Rule rule;
   private Twin twin;

   public RulesToolkit(Path var1, Path var2, Path var3, List<Path> var4) {
      this.root = var1;
      this.rulesHome = var2;
      this.reportDir = var3;
      this.requirementsHomes = var4 == null ? List.of() : List.copyOf(var4);
   }

   public RulesToolkit withProjectBuilder(Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> var1) {
      this.projectBuilder = var1;
      return this;
   }

   public RulesToolkit readOnly() {
      this.readOnly = true;
      return this;
   }

   @Override
   public void init(AgentContext var1) {
      this.rule = Rule.atRulesHome(this.rulesHome)
         .withRoot(this.root)
         .withReportDir(this.reportDir)
         .withRequirementsHomes(this.requirementsHomes)
         .withProjectBuilder(this.projectBuilder)
         .onStep(var1::step);
   }

   @Override
   public Map<String, Object> bindings() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (this.rule == null) {
         this.rule = Rule.atRulesHome(this.rulesHome)
            .withRoot(this.root)
            .withReportDir(this.reportDir)
            .withRequirementsHomes(this.requirementsHomes)
            .withProjectBuilder(this.projectBuilder);
      }

      if (this.readOnly) {
         var1.put("Rule", new RulesToolkit.ReadOnlyRule(this.rule));
         return var1;
      } else {
         if (this.twin == null) {
            this.twin = Twin.atRulesHome(this.rulesHome).withRoot(this.root).withMocks(io.karatelabs.ext.http.HttpMockFactory.a);
         }

         var1.put("Rule", this.rule);
         var1.put("Twin", this.twin);
         var1.put("Schema", Schema.atRulesHome(this.rulesHome));
         var1.put(
            "Requirement",
            Requirement.atReportDir(this.reportDir)
               .withRequirementsHomes(this.requirementsHomes)
               .withProjectRoot(this.root)
               .withRunsDir(this.root.resolve("runs"))
         );
         var1.put("Skill", new Skill());
         return var1;
      }
   }

   @Override
   public String promptFragment() {
      if (this.readOnly) {
         StringBuilder var5 = new StringBuilder("## Rules — the acceptance oracle (read-only)\n\n");
         var5.append(
            "`Rule.list()`, `Rule.load(name)` (the rulebook's files), `Rule.execute(name, row)` / `Rule.run(name, scenarioId)` (re-run its calc.js on a row), `Rule.audit(name, row)` (the step-by-step audit tree), `Rule.check(name)`. `Rule.help()` describes them. The authoring verbs are not bound.\n"
         );
         var var6 = this.bookNames();
         if (!var6.isEmpty()) {
            var5.append("\nRulebooks here:\n");

            for (String var8 : (Iterable<String>) (Iterable<?>) (var6)) {
               var5.append("- `").append(var8).append("`\n");
            }
         }

         return var5.toString();
      } else {
         StringBuilder var1 = new StringBuilder("## Rules — the acceptance substrate\n\n");
         var1.append(
            "`Rule` (author + verify a rulebook), `Schema` (its input shape), `Requirement` (the requirements graph a `{req}`-guarded property joins to), `Skill` (the shipped guides).\n"
         );
         var1.append(
            "**Read the surface before authoring — it is the source of truth for the conventions, not this prompt:** `Skill.help('rule-authoring')`, then `Rule.help()` and `Schema.help()`. Every namespace self-describes; call `help()` rather than guessing a verb, and a sub-namespace (`Rule.scenario`, `Rule.lookups`, …) is listed by its parent's help.\n"
         );
         var var2 = this.bookNames();
         if (var2.isEmpty()) {
            var1.append("\nThis project has no rulebooks yet — `Rule.list()` is empty.\n");
         } else {
            var1.append("\nRulebooks already here (`Rule.list()`):\n");

            for (String var4 : (Iterable<String>) (Iterable<?>) (var2)) {
               var1.append("- `").append(var4).append("`\n");
            }
         }

         return var1.toString();
      }
   }

   private List<String> bookNames() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      try {
         for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var3 : new RulebookRepository(this.rulesHome).b()) {
            var1.add(var3.a());
         }

         return var1;
      } catch (Exception var4) {
         return List.of();
      }
   }

   @Override
   public void close() {
      if (this.rule != null) {
         this.rule.close();
      }

      if (this.twin != null) {
         this.twin.close();
      }
   }

   static final class ReadOnlyRule extends JsDispatch {
      static final List<String> VERBS = List.of("list", "load", "execute", "run", "audit", "check", "help");

      ReadOnlyRule(Rule var1) {
         for (String var3 : VERBS) {
            this.val(var3, var1.jsGet(var3));
         }
      }
   }
}
