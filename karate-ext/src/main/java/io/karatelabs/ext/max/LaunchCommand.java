package io.karatelabs.ext.max;

import io.karatelabs.ext.max.jsapi.Runner;
import io.karatelabs.ext.max.run.MaxProfile;
import io.karatelabs.js.Engine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
   name = "launch",
   mixinStandardHelpOptions = true,
   description = {"Run a *.karate.js orchestrator (scriptable suites — Runner.*/Harness.* kickoff)"}
)
public class LaunchCommand implements Callable<Integer> {
   private static final Logger logger = LoggerFactory.getLogger(LaunchCommand.class);
   @Parameters(
      index = "0",
      arity = "1",
      description = {"the orchestrator script to run (any JS file; the convention is *.karate.js)"}
   )
   String script;
   @Option(
      names = {"--root"},
      description = {"Project root — the anchor for suite/spec/path references (default: current dir)"}
   )
   String root = ".";
   @Option(
      names = {"-r", "--report-dir"},
      description = {"Karate report dir for Runner/Coverage/Report (default: ${DEFAULT-VALUE})"}
   )
   String reportDir = "target/karate-reports";
   @Option(
      names = {"--rules-dir"},
      description = {"Rules home dir for the Rule/Schema/Mock namespaces (default: ${DEFAULT-VALUE})"}
   )
   String rulesDir = "rulebooks";
   @Option(
      names = {"--requirements-dir"},
      split = ",",
      description = {"Requirements home dir(s) for the Requirement namespace — comma-separated for several, all merging into the one req source. Overrides the project: unset, the home comes from its karate-boot.js (req.home / cov.requirements), else requirements/"}
   )
   List<String> requirementsDirs = List.of();

   public Integer call() {
      io.karatelabs.ext.common.keycheck.Main.requireLicensed("launch");
      logger.info(io.karatelabs.ext.common.keycheck.Main.licenseSummary());
      return this.run();
   }

   int run() {
      Path var1 = Path.of(this.script).toAbsolutePath().normalize();
      if (!Files.isRegularFile(var1)) {
         logger.error("launch: script not found: {}", var1);
         return 1;
      } else {
         Path var2 = Path.of(this.root).toAbsolutePath().normalize();
         Engine var3 = new Engine();
         Path var4 = under(var2, this.reportDir);
         new MaxProfile(var2, var4, under(var2, this.rulesDir), ServeCommand.homesUnder(var2, this.requirementsDirs), null)
            .withSuiteOutputDir(var4)
            .apply(var3);
         Runner var5 = (Runner)var3.getRootBindings().get("Runner");
         Runner.Launched var6 = var5.launchScope(() -> var3.eval(Files.readString(var1)));
         if (var6.error() != null) {
            logger.error("launch: {} failed: {}", var1.getFileName(), var6.error().getMessage());
         } else {
            logger.info("launch: {} completed, exit {}", var1.getFileName(), var6.exitCode());
         }

         return var6.exitCode();
      }
   }

   private static Path under(Path var0, String var1) {
      Path var2 = Path.of(var1);
      return var2.isAbsolute() ? var2.normalize() : var0.resolve(var2).normalize();
   }
}
