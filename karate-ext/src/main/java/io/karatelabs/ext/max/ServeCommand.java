package io.karatelabs.ext.max;

import io.karatelabs.ext.max.run.MaxProfile;
import io.karatelabs.ext.max.run.MaxWorkspace;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
   name = "serve",
   mixinStandardHelpOptions = true,
   description = {"Start the karate-max curl + MCP server (one port: /api/eval and /api/mcp)"}
)
public class ServeCommand implements Callable<Integer> {
   private static final Logger logger = LoggerFactory.getLogger(ServeCommand.class);
   static final int DEFAULT_PORT = 4444;
   static final String DEFAULT_BIND = "localhost";
   static final String DEFAULT_REPORT_DIR = "target/karate-reports";
   static final String DEFAULT_RULES_DIR = "rulebooks";
   static final String DEFAULT_REQUIREMENTS_DIR = "requirements";
   @Option(
      names = {"-p", "--port"},
      description = {"Port to listen on (default: ${DEFAULT-VALUE})"}
   )
   int port = 4444;
   @Option(
      names = {"-b", "--bind"},
      description = {"Bind address (default: ${DEFAULT-VALUE})"}
   )
   String bind = "localhost";
   @Parameters(
      index = "0",
      arity = "0..1",
      paramLabel = "PROJECT",
      description = {"The project folder to serve (the developer convenience). Always the active project; its PARENT becomes the picker ceiling, so the console can flip to sibling projects at runtime, no restart (D186/D187). Defaults to the current dir. To serve a whole folder OF projects instead, use --work (or --select to name the child) — it is not guessed from on-disk markers. Ignored when --work is set. Must exist."}
   )
   String project;
   @Option(
      names = {"-w", "--work"},
      description = {"Fix the picker ceiling explicitly (no parent derivation) — the container path. The active project is --select, else the first child, else the 'no project' screen when empty. Also $KARATE_SERVE_WORK (the image sets /work). Mount a folder OF projects here."}
   )
   String workDir;
   @Option(
      names = {"--select"},
      description = {"Which project sub-folder under the ceiling to anchor on start (a child name); else $KARATE_SERVE_SELECT, else the first child, else the 'no project' screen (D186)."}
   )
   String select;
   @Option(
      names = {"-r", "--report-dir"},
      description = {"Karate report dir for the Coverage namespace (default: ${DEFAULT-VALUE})"}
   )
   String reportDir = "target/karate-reports";
   @Option(
      names = {"--rules-dir"},
      description = {"Rulebook home dir for the Rule/Schema namespaces (default: ${DEFAULT-VALUE})"}
   )
   String rulesDir = "rulebooks";
   @Option(
      names = {"--requirements-dir"},
      split = ",",
      description = {"Requirements home dir(s) for the Requirement namespace — comma-separated for several, all merging into the one req source. Overrides the project: unset, the home comes from its karate-boot.js (req.home / cov.requirements), else requirements/"}
   )
   List<String> requirementsDirs = List.of();

   public Integer call() {
      io.karatelabs.ext.common.keycheck.Main.requireLicensed("serve");
      logger.info(io.karatelabs.ext.common.keycheck.Main.licenseSummary());
      String var1 = firstNonBlank(this.workDir, System.getenv("KARATE_SERVE_WORK"));
      String var2 = firstNonBlank(this.select, System.getenv("KARATE_SERVE_SELECT"));
      String var3 = var1 == null && this.project == null ? "." : this.project;
      int var4 = this.port == 4444 ? envInt("KARATE_SERVE_PORT", 4444) : this.port;
      String var5 = !"localhost".equals(this.bind) ? this.bind : firstNonBlank(System.getenv("KARATE_SERVE_BIND"), "localhost");

      MaxWorkspace var6;
      try {
         var6 = resolveWorkspace(var3, var1, var2);
      } catch (IllegalArgumentException var15) {
         logger.error("karate serve: {}", var15.getMessage());
         return 2;
      }

      Path var7 = var6.root();
      Path var8 = under(var7, this.reportDir);
      Path var9 = under(var7, this.rulesDir);
      var var10 = homesUnder(var7, this.requirementsDirs);
      MaxServer var11 = new MaxServer();
      MaxProfile var12 = new MaxProfile(var6, var8, var9, var10, var11.getSseHub());
      var11.applyProfile(var12);
      String var13 = System.getenv("KARATE_NOVNC_ROOT");
      if (var13 != null && !var13.isBlank()) {
         String var14 = System.getenv().getOrDefault("KARATE_VNC_WS_URL", "ws://localhost:6080/websockify");
         var11.vnc(Path.of(var13), var14);
         logger.info("noVNC on the serve port: /novnc/ (static from {}, tunnel to {})", var13, var14);
      }

      var11.start(var5, var4);
      if ("0.0.0.0".equals(var5)) {
         logger.warn(
            "karate serve bound to ALL interfaces (--bind 0.0.0.0) — the /api/eval surface is engine-eval; expose it only on a trusted network (or publish to 127.0.0.1 with Docker -p)"
         );
      }

      logger.info(
         "karate serve ready — POST http://{}:{}/api/eval and /api/mcp ({} under ceiling {}; coverage from {}, rules from {}, requirements from {})",
         new Object[]{
            var5,
            var11.getPort(),
            var6.noProject() ? "NO project selected (empty ceiling — Create demo / New folder)" : "project " + var7,
            var6.ceiling(),
            var8,
            var9,
            var12.requirementsHomes()
         }
      );
      Runtime.getRuntime().addShutdownHook(new Thread(var11::stop, "karate-max-shutdown"));
      var11.waitSync();
      return 0;
   }

   private static Path under(Path var0, String var1) {
      Path var2 = Path.of(var1);
      return var2.isAbsolute() ? var2.normalize() : var0.resolve(var2).normalize();
   }

   static List<Path> homesUnder(Path var0, List<String> var1) {
      ArrayList<java.nio.file.Path> var2 = new ArrayList();

      for (String var4 : (Iterable<String>) (Iterable<?>) (var1 == null ? List.of() : var1)) {
         if (var4 != null && !var4.isBlank()) {
            var2.add(under(var0, var4.trim()));
         }
      }

      return List.copyOf(var2);
   }

   static MaxWorkspace resolveWorkspace(String var0, String var1, String var2) {
      boolean var3 = var2 != null && !var2.isBlank();
      if (var1 != null && !var1.isBlank()) {
         Path var6 = requireDir(var1, "work folder (--work)");
         Path var5;
         if (var3) {
            var5 = requireChild(var6, var2, "--select");
         } else if (var0 != null && !var0.isBlank()) {
            var5 = var6.resolve(var0).normalize();
            if (!var5.startsWith(var6) || !Files.isDirectory(var5)) {
               throw new IllegalArgumentException("project '" + var0 + "' is not a folder under the --work ceiling " + var6);
            }
         } else {
            var5 = MaxWorkspace.of(var6, var6).firstProject();
         }

         return MaxWorkspace.of(var6, var5);
      } else {
         Path var4 = requireDir(var0, "project folder");
         return var3 ? MaxWorkspace.of(var4, requireChild(var4, var2, "--select")) : MaxWorkspace.at(var4);
      }
   }

   private static Path requireDir(String var0, String var1) {
      Path var2 = Path.of(var0).toAbsolutePath().normalize();
      if (!Files.isDirectory(var2)) {
         throw new IllegalArgumentException(var1 + " does not exist: " + var2);
      } else {
         return var2;
      }
   }

   private static Path requireChild(Path var0, String var1, String var2) {
      Path var3 = var0.resolve(var1).normalize();
      if (!Files.isDirectory(var3)) {
         throw new IllegalArgumentException(var2 + " '" + var1 + "' is not a project folder under " + var0 + " (no directory " + var3 + ")");
      } else {
         return var3;
      }
   }

   private static String firstNonBlank(String... var0) {
      for (String var4 : var0) {
         if (var4 != null && !var4.isBlank()) {
            return var4;
         }
      }

      return null;
   }

   private static int envInt(String var0, int var1) {
      String var2 = System.getenv(var0);
      if (var2 != null && !var2.isBlank()) {
         try {
            return Integer.parseInt(var2.trim());
         } catch (NumberFormatException var4) {
            return var1;
         }
      } else {
         return var1;
      }
   }
}
