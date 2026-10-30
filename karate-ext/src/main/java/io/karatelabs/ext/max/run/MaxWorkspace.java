package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.MaxPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class MaxWorkspace {
   public static final List<String> MARKERS = List.of("karate-pom.json", "rulebooks", "requirements", "checks", ".karate", "openapi.yaml");
   private static final int BROWSE_CAP = 500;
   private final Path ceiling;
   private final Path root;
   public static final String REQUIREMENTS = "requirements";
   public static final String CHECKS = "checks";
   public static final String RULEBOOKS = "rulebooks";

   private MaxWorkspace(Path var1, Path var2) {
      this.ceiling = var1.toAbsolutePath().normalize();
      this.root = var2.toAbsolutePath().normalize();
   }

   public static MaxWorkspace of(Path var0, Path var1) {
      return new MaxWorkspace(var0, var1);
   }

   public static MaxWorkspace at(Path var0) {
      Path var1 = var0.toAbsolutePath().normalize();
      Path var2 = var1.getParent();
      return new MaxWorkspace(var2 != null ? var2 : var1, var1);
   }

   public Path ceiling() {
      return this.ceiling;
   }

   public Path root() {
      return this.root;
   }

   public boolean noProject() {
      return this.root.equals(this.ceiling);
   }

   public Path configDir() {
      return this.root;
   }

   public Path requirementsDir() {
      return this.root.resolve("requirements");
   }

   public Path checksDir() {
      return this.root.resolve("checks");
   }

   public Path rulebooksDir() {
      return this.root.resolve("rulebooks");
   }

   public static Path requirementsDir(Path var0) {
      return MaxPaths.requirementsDir(var0);
   }

   public static Path checksDir(Path var0) {
      return MaxPaths.checksDir(var0);
   }

   public static Path rulebooksDir(Path var0) {
      return MaxPaths.rulebooksDir(var0);
   }

   public Map<String, Object> dirs() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("requirements", this.requirementsDir().toString());
      var1.put("checks", this.checksDir().toString());
      var1.put("rulebooks", this.rulebooksDir().toString());
      return var1;
   }

   public MaxWorkspace reanchor(Path var1) {
      Path var2 = var1.isAbsolute() ? var1.normalize() : this.ceiling.resolve(var1).normalize();
      if (!this.within(var2)) {
         throw new IllegalArgumentException(
            "'"
               + var2
               + "' is outside the mounted work ceiling "
               + this.ceiling
               + " — a container only sees paths under its /work mount, so it cannot anchor or run tests here. To work on this path, restart with it (or a parent folder holding several projects) mounted as /work: docker run -v \""
               + var2
               + "\":/work …  (running the JAR/CLI instead? pass that directory to `serve`.) Do NOT run the tests outside the product with a separately downloaded karate jar — that loses coverage, requirements and rules."
         );
      } else {
         return new MaxWorkspace(this.ceiling, var2);
      }
   }

   public boolean within(Path var1) {
      return var1.toAbsolutePath().normalize().startsWith(this.ceiling);
   }

   public Map<String, Object> current() {
      var var1 = this.describe(this.root);
      var1.put("root", this.root.toString());
      var1.put("configDir", this.configDir().toString());
      var1.put("ceiling", this.ceiling.toString());
      var1.put("dirs", this.dirs());
      var1.put("current", true);
      var1.put("noProject", this.noProject());
      return var1;
   }

   public List<Map<String, Object>> projects() {
      return this.childDirs(this.ceiling);
   }

   public Path firstProject() {
      Path var1 = null;

      for (Map var3 : this.childDirs(this.ceiling)) {
         Path var4 = Path.of((String)var3.get("path"));
         if (var1 == null) {
            var1 = var4;
         }

         if (Boolean.TRUE.equals(var3.get("project"))) {
            return var4;
         }
      }

      return var1 != null ? var1 : this.ceiling;
   }

   public Map<String, Object> browse(String var1) {
      Path var2 = var1 != null && !var1.isBlank() ? this.ceiling.resolve(var1).normalize() : this.ceiling;
      if (!dir(var2) && var1 != null && !var1.isBlank()) {
         Path var3 = Path.of(var1).toAbsolutePath().normalize();
         if (dir(var3)) {
            var2 = var3;
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      if (this.within(var2) && dir(var2)) {
         var5.put("ceiling", this.ceiling.toString());
         var5.put("path", var2.toString());
         Path var4 = var2.getParent();
         var5.put("parent", var4 != null && this.within(var4) && !var2.equals(this.ceiling) ? var4.toString() : null);
         var5.put("dirs", this.childDirs(var2));
         return var5;
      } else {
         var5.put("ceiling", this.ceiling.toString());
         var5.put("path", this.ceiling.toString());
         var5.put("dirs", List.of());
         var5.put("error", "outside the workspace or not a directory: " + var2);
         return var5;
      }
   }

   private List<Map<String, Object>> childDirs(Path var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (!dir(var1)) {
         return var2;
      } else {
         try (var var3 = Files.list(var1)) {
            var3.filter(MaxWorkspace::dir)
               .filter(var0 -> !var0.getFileName().toString().startsWith("."))
               .filter(var0 -> !"target".equals(var0.getFileName().toString()))
               .sorted()
               .limit(500L)
               .forEach(var2x -> var2.add(this.describe(var2x)));
         } catch (IOException var8) {
         }

         return var2;
      }
   }

   private Map<String, Object> describe(Path var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("name", var1.getFileName() != null ? var1.getFileName().toString() : var1.toString());
      var2.put("path", var1.toString());
      var var3 = markersOf(var1);
      var2.put("markers", var3);
      var2.put("project", !var3.isEmpty());
      if (var1.equals(this.root)) {
         var2.put("current", true);
      }

      return var2;
   }

   public static List<String> markersOf(Path var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var3 : MARKERS) {
         if (Files.exists(var0.resolve(var3))) {
            var1.add(var3);
         }
      }

      return var1;
   }

   private static boolean dir(Path var0) {
      return var0 != null && Files.isDirectory(var0);
   }
}
