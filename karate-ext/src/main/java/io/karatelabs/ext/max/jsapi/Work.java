package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.demo.DemoTemplate;
import io.karatelabs.ext.max.run.MaxWorkspace;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Work extends JsDispatch {
   private final Supplier<MaxWorkspace> active;
   private final Function<String, Object> useFn;
   private final Supplier<Path> reportDir;
   private final Supplier<List<Path>> requirementsHomes;
   private final Supplier<Path> rulesHome;
   private volatile ConsoleState console = new ConsoleState(null);

   public Work withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   public Work(Supplier<MaxWorkspace> var1, Function<String, Object> var2, Supplier<Path> var3, Supplier<List<Path>> var4, Supplier<Path> var5) {
      this.active = var1;
      this.useFn = var2;
      this.reportDir = var3;
      this.requirementsHomes = var4;
      this.rulesHome = var5;
      this.fn("current", var1x -> this.current());
      this.fn("dirs", var1x -> this.dirs());
      this.fn("list", var1x -> ((MaxWorkspace)var1.get()).projects());
      this.fn("browse", var1x -> this.browse(JsApi.str(var1x, 0)));
      this.fn("use", var1x -> this.use(JsApi.str(var1x, 0)));
      this.fn("create", var1x -> this.create(JsApi.str(var1x, 0)));
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
      this.fn("seed", var1x -> this.seed(JsApi.str(var1x, 0)));
      this.fn("boot", var1x -> this.boot(JsApi.arg(var1x, 0)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   Map<String, Object> current() {
      var var1 = new LinkedHashMap<>(this.active.get().current());
      if (var1.get("dirs") instanceof Map) {
         var1.put("dirs", this.dirs());
      }

      var1.put("env", this.console.env());
      return var1;
   }

   Map<String, Object> dirs() {
      var var1 = new LinkedHashMap<>(this.active.get().dirs());
      var var2 = this.requirementsHomes.get();
      if (var2 != null && !var2.isEmpty()) {
         var1.put("requirements", ((Path)var2.get(0)).toAbsolutePath().normalize().toString());
      }

      var1.put("reports", this.reportDir.get().toString());
      return var1;
   }

   Object seed(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1.trim() : "demo";
      if (!var2.matches("[a-z0-9-]+")) {
         return JsApi.missing("Work.seed(name?) — name is a simple slug; the shipped template is 'demo'");
      } else {
         Path var3 = this.active.get().ceiling().resolve(var2);

         try {
            DemoTemplate.materialize(var2, var3);
         } catch (RuntimeException var5) {
            return JsApi.missing("Work.seed('" + var2 + "') failed: " + var5.getMessage());
         }

         return this.useFn.apply(var3.toString());
      }
   }

   public Object boot(Object var1) {
      MaxWorkspace var2 = this.active.get();
      if (var2.noProject()) {
         return JsApi.noProjectSelected("Work.boot");
      } else {
         Path var3 = var2.root();
         Path var4 = var3.resolve("karate-boot.js");
         if (Files.exists(var4)) {
            return bootExists();
         } else {
            String var5 = Runner.anyDeveloperTest(var3);
            if (var5 != null) {
               return refused(
                  "this project already holds tests (" + var5 + ") — the exts they run with are its developer's to declare in karate-boot.js", "HAS_TESTS"
               );
            } else {
               LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
               var6.put("path", "karate-boot.js");
               var6.put("text", this.bootText(var3));
               if (var1 instanceof Map var7 && Boolean.TRUE.equals(var7.get("dryRun"))) {
                  var6.put("dryRun", true);
                  return var6;
               } else {
                  try {
                     landNew(var4, (String)var6.get("text"));
                  } catch (FileAlreadyExistsException var8) {
                     return bootExists();
                  } catch (IOException var9) {
                     return JsApi.missing("Work.boot() failed: " + var9.getMessage());
                  }

                  var6.put("status", "written");
                  return var6;
               }
            }
         }
      }
   }

   private static void landNew(Path var0, String var1) throws IOException {
      Path var2 = var0.resolveSibling("." + var0.getFileName() + "." + System.nanoTime() + ".tmp");

      try {
         Files.writeString(var2, var1);

         try {
            Files.createLink(var0, var2);
         } catch (FileSystemException | UnsupportedOperationException var8) {
            if (var8 instanceof FileAlreadyExistsException var4) {
               throw var4;
            }

            Files.move(var2, var0);
         }
      } finally {
         Files.deleteIfExists(var2);
      }
   }

   private static Map<String, Object> bootExists() {
      return refused(
         "this project already has a karate-boot.js — it is the project's own; read or edit it with File.read('karate-boot.js') / File.write", "EXISTS"
      );
   }

   private String bootText(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      var var3 = this.dirs();
      var var4 = this.requirementsHomes.get();
      ArrayList<java.lang.String> var5 = new ArrayList();

      for (Path var7 : (Iterable<Path>) (Iterable<?>) (var4 != null && !var4.isEmpty() ? var4 : List.of(Path.of(String.valueOf(var3.get("requirements")))))) {
         var5.add(quoted(var2, var7));
      }

      Path var8 = this.rulesHome.get();
      if (var8 == null) {
         var8 = Path.of(String.valueOf(var3.get("rulebooks")));
      }

      return String.join(
         "\n",
         "// Written by Work.boot() so this project's tests run: rules binds Check/match (a recorded test",
         "// runs as a suite), robot lets a test drive a browser (booting it opens none), coverage joins",
         "// the requirements so a passing test turns its criteria green.",
         "const rules = boot.ext('rules');",
         "rules.home = " + quoted(var2, var8) + ";",
         "boot.ext('robot');",
         "const cov = boot.ext('coverage');",
         "cov.requirements = " + (var5.size() == 1 ? (String)var5.get(0) : "[" + String.join(", ", var5) + "]") + ";",
         ""
      );
   }

   private static String quoted(Path var0, Path var1) {
      String var2 = var0.relativize(var1.toAbsolutePath().normalize()).toString().replace('\\', '/');
      return "'" + (var2.isEmpty() ? "/" : var2).replace("'", "\\'") + "'";
   }

   private static Map<String, Object> refused(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("error", "Work.boot: " + var0);
      var2.put("code", var1);
      var2.put("status", 409);
      return var2;
   }

   Object create(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.trim();
         if (!var2.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*")) {
            return JsApi.missing("Work.create(name) — use a simple folder name (start alphanumeric; letters, digits, '.', '_', '-'), no path separators");
         } else {
            MaxWorkspace var3 = this.active.get();
            Path var4 = var3.ceiling().resolve(var2).normalize();
            if (!var3.within(var4)) {
               return JsApi.missing("Work.create: '" + var2 + "' escapes the /work ceiling");
            } else if (Files.exists(var4)) {
               return JsApi.missing("Work.create: '" + var2 + "' already exists — pick it from the list or choose another name");
            } else {
               try {
                  Files.createDirectories(var4);
               } catch (IOException var6) {
                  return JsApi.missing("Work.create('" + var2 + "') failed: " + var6.getMessage());
               }

               return this.useFn.apply(var4.toString());
            }
         }
      } else {
         return JsApi.missing("Work.create(name) requires a folder name to create under the /work ceiling");
      }
   }

   Object delete(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.trim();
         if (!var2.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*")) {
            return JsApi.missing("Work.delete(name) — a simple folder name (no path separators)");
         } else {
            MaxWorkspace var3 = this.active.get();
            Path var4 = var3.ceiling().resolve(var2).normalize();
            if (!var3.within(var4) || var4.equals(var3.ceiling())) {
               return JsApi.missing("Work.delete: '" + var2 + "' is not a project under the /work ceiling");
            } else if (!Files.isDirectory(var4)) {
               return JsApi.missing("Work.delete: no project folder '" + var2 + "'");
            } else if (var4.equals(var3.root().normalize())) {
               return JsApi.missing("Work.delete: '" + var2 + "' is the active project — switch to another project first (Work.use), then delete it");
            } else {
               try (var var5 = Files.walk(var4)) {
                  var5.sorted(Comparator.reverseOrder()).forEach(var0 -> {
                     try {
                        Files.delete(var0);
                     } catch (IOException var2x) {
                     }
                  });
               } catch (IOException var10) {
                  return JsApi.missing("Work.delete('" + var2 + "') failed: " + var10.getMessage());
               }

               if (Files.exists(var4)) {
                  return JsApi.missing("Work.delete('" + var2 + "') incomplete — some files could not be removed");
               } else {
                  LinkedHashMap<java.lang.String, java.lang.String> var11 = new LinkedHashMap();
                  var11.put("status", "deleted");
                  var11.put("name", var2);
                  return var11;
               }
            }
         }
      } else {
         return JsApi.missing("Work.delete(name) requires the project folder name to delete");
      }
   }

   Object use(String var1) {
      return var1 != null && !var1.isBlank()
         ? this.useFn.apply(var1)
         : JsApi.missing(
            "Work.use(name) requires the project to re-anchor to — a bare name from Work.list() (e.g. 'order-pricing') or a path under the /work ceiling"
         );
   }

   Object browse(String var1) {
      try {
         return this.active.get().browse(var1);
      } catch (RuntimeException var3) {
         return JsApi.missing("Work.browse failed: " + var3.getMessage());
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Work — the project the console is pointed at\n\nOne container is a single tenant (one active project at a time). `Work.*` lists the\nproject library under the `/work` ceiling and (soon) re-anchors the whole server to a\ndifferent one — the nav project-picker calls these same verbs.\n\n- `Work.current()` → the active anchor: `{root, name, configDir, ceiling, markers, project, dirs, env}`\n  — `dirs` is where each artifact kind lives: `{requirements, checks, rulebooks, reports}`;\n  `env` is the `karate.env` runs execute under: the ambient `-Dkarate.env` / `KARATE_ENV`, else null;\n  `Runner.config(env)` reloads the live engine only.\n- `Work.dirs()` → just that content-roots map. **Honor it when authoring a new file** — a `.feature`\n  goes under `checks`, a requirement under `requirements`, a rulebook under `rulebooks`. (A\n  future `karate-pom.json` remaps these per project, for Maven/Gradle parity.)\n- `Work.list()` → the projects under the ceiling: `[{name, path, markers, project, current}]`.\n  A folder is a project if it carries any marker (`rulebooks`/`requirements`/`checks`/`openapi.yaml`/\n  `.karate`/`karate-pom.json`); a bare folder is still anchorable.\n- `Work.browse(dir?)` → one level of child folders under `dir` (ceiling-relative; default the\n  ceiling). Lazy — it never recurses the disk and never escapes the ceiling.\n- `Work.use(name)` → re-anchor the whole server to that project (`{root, status:'switched'}`);\n  takes a bare name from `Work.list()` (e.g. `'order-pricing'`, resolved against the ceiling)\n  or a full path. Every open console tab reloads onto it. Refused while a run is active, or if\n  the folder does not exist or is outside the ceiling (a `{error}` envelope).\n- `Work.create(name)` → create a **new empty folder** under the ceiling and re-anchor onto it\n  (the picker's \"New folder\" button). A bare project you then fill in (record→check, requirements,\n  rules); the authoring surfaces create `checks/`/`requirements/`/`rulebooks/` lazily on first\n  write. Refuses a name that already exists or escapes the ceiling.\n- `Work.delete(name)` → **recursively delete** a project folder under the ceiling (the picker\n  tile's Delete). Refuses the active project (switch away first), the ceiling, or a name outside\n  it. To download a project's contents first, the tile's **Zip** link hits `GET /api/work-zip/<name>`.\n- `Work.seed([name])` → materialize the jar-baked flagship **`demo`** project under the ceiling\n  (requirements + rules + openapi + checks, with the right `.gitignore`) and re-anchor onto it\n  — a fresh user lands on something real. The \"Create demo\" button calls this.\n- `Work.boot([{dryRun?}])` → write `karate-boot.js` for a project that has none and holds no test\n  a person owns (an unreviewed `// @ai: written` check is not one): rules, robot and coverage over the server's rules and requirements homes, so a recorded\n  `.js` test runs as a suite and its criteria turn green. `{path, text, status:'written'}`;\n  `{dryRun:true}` writes nothing. Refused 409 `EXISTS` / `HAS_TESTS`: that project's boot file is\n  its developer's.\n\nThe ceiling is the `/work` mount in-container (or the cwd's parent for the CLI) — the picker\nboundary. Re-anchoring stays within it."
      );
   }
}
