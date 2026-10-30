package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.RunUtils;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Step;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecutionInventory {
   private static final Logger d = LoggerFactory.getLogger(io.karatelabs.ext.coverage.collect.ExecutionInventory.class);
   public static final String a = "check:";
   public static final String b = "oracle:";
   public static final io.karatelabs.ext.coverage.collect.ExecutionInventory c = new io.karatelabs.ext.coverage.collect.ExecutionInventory(
      null,
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE,
      false,
      Map.of(),
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE,
      Set.of(),
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE,
      Map.of(),
      null
   );
   private static final Map<io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp, io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition> e = f();
   private static final Map<io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp, Set<String>> f = f();
   private static final Map<Path, Integer> g = new ConcurrentHashMap<>();
   private final Path h;
   private final io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness i;
   private final boolean j;
   private final Map<String, io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition> k;
   private final io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness l;
   private final Set<String> m;
   private final io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness n;
   private final Map<String, Set<String>> o;
   private final Path p;
   private static final Set<String> q = Set.of("target", "runs", "node_modules", ".git");

   private ExecutionInventory(
      Path var1,
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var2,
      boolean var3,
      Map<String, io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition> var4,
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var5,
      Set<String> var6,
      io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var7,
      Map<String, Set<String>> var8,
      Path var9
   ) {
      this.h = var1;
      this.i = var2;
      this.j = var3;
      this.k = var4;
      this.l = var5;
      this.m = var6;
      this.n = var7;
      this.o = var8;
      this.p = var9;
   }

   public static int a(Path var0) {
      return g.getOrDefault(d(var0), 0);
   }

   public static void a() {
      synchronized (e) {
         e.clear();
      }

      synchronized (f) {
         f.clear();
      }

      g.clear();
   }

   public static io.karatelabs.ext.coverage.collect.ExecutionInventory a(Path var0, Path var1) {
      if (var0 != null && Files.isDirectory(var0)) {
         Path var2 = d(var0);
         LinkedHashMap<java.lang.String, io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition> var3 = new LinkedHashMap();
         io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var4 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.COMPLETE;
         boolean[] var5 = new boolean[]{false};

         try {
            for (Path var7 : a(var2, var5, false)) {
               String var8 = var2.relativize(var7).toString().replace('\\', '/');
               io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition var9 = b(var2, var7);
               var3.put(var8, var9);
               if (var9 == null || var9.c() || !var9.b().isEmpty()) {
                  var4 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.PARTIAL;
               }
            }

            if (var5[0]) {
               var4 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.PARTIAL;
            }
         } catch (RuntimeException | IOException var23) {
            d.warn("definition inventory: could not walk {} ({}) — scenarios unavailable", var2, var23.toString());
            var4 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE;
            var5[0] = true;
         }

         LinkedHashSet<java.lang.String> var24 = new LinkedHashSet();
         io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var25 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.COMPLETE;
         Path var26 = var2.resolve("checks");
         if (Files.isDirectory(var26)) {
            try (var var27 = Files.walk(var26, FileVisitOption.FOLLOW_LINKS)) {
               var27.filter(var0x -> Files.isRegularFile(var0x) && var0x.toString().endsWith(".js"))
                  .forEach(var2x -> var24.add(var2.relativize(var2x.toAbsolutePath().normalize()).toString().replace('\\', '/')));
            } catch (UncheckedIOException var21) {
               boolean var10 = var21.getCause() instanceof FileSystemLoopException;
               d.warn("definition inventory: could not walk {} ({}) — checks {}", new Object[]{var26, var21.toString(), var10 ? "partial" : "unavailable"});
               var25 = var10 ? io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.PARTIAL : io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE;
            } catch (RuntimeException | IOException var22) {
               d.warn("definition inventory: could not walk {} ({}) — checks unavailable", var26, var22.toString());
               var25 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE;
            }
         }

         LinkedHashMap<java.lang.String, java.util.Set<java.lang.String>> var28 = new LinkedHashMap();
         io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var29 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.COMPLETE;
         Path var11 = var1 != null ? var1 : var2.resolve("rulebooks");
         if (Files.isDirectory(var11)) {
            try (var var12 = Files.list(var11)) {
               for (Path var14 : (Iterable<Path>) (var12.filter(var0x -> Files.isDirectory(var0x))::iterator)) {
                  var var15 = e(var14.resolve("scenarios.json"));
                  var28.put(var14.getFileName().toString(), var15);
                  if (var15 == null) {
                     var29 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.PARTIAL;
                  }
               }
            } catch (RuntimeException | IOException var19) {
               d.warn("definition inventory: could not list {} ({}) — rulebooks unavailable", var11, var19.toString());
               var29 = io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE;
            }
         }

         return new io.karatelabs.ext.coverage.collect.ExecutionInventory(var2, var4, !var5[0], var3, var25, var24, var29, var28, var11);
      } else {
         return c;
      }
   }

   public static List<String> b(Path var0) {
      Path var1 = d(var0);
      ArrayList<java.lang.String> var2 = new ArrayList();

      try {
         for (Path var4 : a(var1, new boolean[1], false)) {
            var2.add(var1.relativize(var4).toString().replace('\\', '/'));
         }
      } catch (IOException var5) {
         d.warn("definition inventory: could not list the features under {} ({})", var1, var5.toString());
      }

      return var2;
   }

   public static String c(Path var0) {
      Path var1 = d(var0);

      try {
         var var2 = a(var1, new boolean[1], true);
         return var2.isEmpty() ? null : var1.relativize((Path)var2.get(0)).toString().replace('\\', '/');
      } catch (IOException var3) {
         d.warn("definition inventory: could not list the features under {} ({})", var1, var3.toString());
         return null;
      }
   }

   private static Path d(Path var0) {
      try {
         return var0.toRealPath();
      } catch (IOException var2) {
         return var0.toAbsolutePath().normalize();
      }
   }

   private static List<Path> a(final Path var0, final boolean[] var1, final boolean var2) throws IOException {
      final ArrayList<java.nio.file.Path> var3 = new ArrayList();
      Files.walkFileTree(var0, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
         @Override
         public FileVisitResult preVisitDirectory(Path var1x, BasicFileAttributes var2x) {
            String var3x = var1x.getFileName() == null ? "" : var1x.getFileName().toString();
            return !var1x.equals(var0) && io.karatelabs.ext.coverage.collect.ExecutionInventory.q.contains(var3x) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFile(Path var1x, BasicFileAttributes var2x) {
            if (var2x.isRegularFile() && var1x.getFileName().toString().endsWith(".feature")) {
               var3.add(var1x);
               if (var2) {
                  return FileVisitResult.TERMINATE;
               }
            }

            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFileFailed(Path var1x, IOException var2x) {
            var1[0] = var1[0] | var2x instanceof FileSystemLoopException;
            return FileVisitResult.CONTINUE;
         }
      });
      var3.sort(null);
      return var3;
   }

   private static io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition b(Path var0, Path var1) {
      io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp var2;
      try {
         var2 = new io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp(var1, Files.size(var1), Files.getLastModifiedTime(var1).toMillis());
      } catch (IOException var7) {
         return null;
      }

      synchronized (e) {
         if (e.containsKey(var2)) {
            return e.get(var2);
         }
      }

      io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition var9 = c(var0, var1);
      g.merge(d(var1), 1, Integer::sum);
      synchronized (e) {
         e.put(var2, var9);
         return var9;
      }
   }

   private static io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition c(Path var0, Path var1) {
      Feature var2;
      try {
         var2 = Feature.read(Resource.from(var1, var0));
      } catch (RuntimeException var12) {
         d.debug("definition inventory: {} did not parse ({})", var1, var12.toString());
         return null;
      }

      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();
      LinkedHashSet<java.lang.String> var4 = new LinkedHashSet();
      boolean var5 = var2.getBackground() != null && a(var2.getBackground().getSteps());

      for (FeatureSection var7 : var2.getSections()) {
         if (var7.isOutline()) {
            String var8 = RunUtils.outlineSlug(var7.getScenarioOutline(), var2);
            var3.add(var8);
            var var9 = var7.getScenarioOutline().getExamplesTables();

            for (ExamplesTable var11 : (Iterable<ExamplesTable>) (Iterable<?>) (var9 == null ? List.of() : var9)) {
               if (var11.getTable() != null && var11.getTable().isDynamic()) {
                  var4.add(var8);
               }
            }
         } else {
            var3.add(RunUtils.scenarioSlug(var7.getScenario(), var2));
            var5 |= a(var7.getScenario().getSteps());
         }
      }

      return new io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition(var3, var4, var5);
   }

   private static boolean a(List<Step> var0) {
      for (Step var2 : (Iterable<Step>) (Iterable<?>) (var0 == null ? List.of() : var0)) {
         String var3 = var2.getText();
         if (var3 != null && var3.contains("__id")) {
            return true;
         }
      }

      return false;
   }

   private static Set<String> e(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return null;
      } else {
         io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp var1;
         try {
            var1 = new io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp(var0, Files.size(var0), Files.getLastModifiedTime(var0).toMillis());
         } catch (IOException var10) {
            return null;
         }

         synchronized (f) {
            if (f.containsKey(var1)) {
               return f.get(var1);
            }
         }

         LinkedHashSet<java.lang.String> var13 = null;

         try {
            if (Json.parseStrict(Files.readString(var0, StandardCharsets.UTF_8)) instanceof List var4) {
               var13 = new LinkedHashSet();

               for (Object var6 : var4) {
                  if (var6 instanceof Map var7 && var7.get("_id") != null) {
                     var13.add(String.valueOf(var7.get("_id")));
                  }
               }
            }
         } catch (RuntimeException | IOException var11) {
            d.debug("definition inventory: {} did not parse ({})", var0, var11.toString());
         }

         synchronized (f) {
            f.put(var1, var13);
            return var13;
         }
      }
   }

   public io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(TestExecution var1) {
      if (var1.a()) {
         return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT;
      } else {
         String var2 = var1.h();
         if (var2.startsWith("check:")) {
            return a(this.l, this.m.contains(var2.substring("check:".length())));
         } else if (var2.startsWith("oracle:")) {
            return a(this.n, this.o.containsKey(var2.substring("oracle:".length())));
         } else {
            RunArtifactParser var3 = io.karatelabs.ext.coverage.collect.RunArtifactParser.a(var2);
            io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus var4 = var3 == null ? null : var3.a(var2, this);
            return var4 != null ? var4 : this.b(var1);
         }
      }
   }

   Path b() {
      return this.h;
   }

   Path c() {
      return this.p;
   }

   io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness d() {
      return this.n;
   }

   boolean a(String var1) {
      return this.o.containsKey(var1);
   }

   private io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus b(TestExecution var1) {
      String var2 = var1.t();
      if (this.i == io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE
         || var2 == null
         || !var2.endsWith(".feature")
         || Path.of(var2).isAbsolute()
         || "classpath".equals(var1.C().get("origin"))
         || b(var2)) {
         return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
      } else if (!this.k.containsKey(var2)) {
         return this.j ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
      } else {
         io.karatelabs.ext.coverage.collect.ExecutionInventory.FeatureDefinition var3 = this.k.get(var2);
         if (var3 == null) {
            return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
         } else if (!var3.a().contains(var1.h())) {
            return var3.c() ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED;
         } else {
            String var4 = var1.i();
            if (var4 == null) {
               return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT;
            } else {
               Object var5 = var1.C().get("deck");
               if (var5 != null) {
                  String var6 = String.valueOf(var5);
                  String var7 = var4.startsWith(var6 + "/") ? var4.substring(var6.length() + 1) : var4;
                  if (this.n == io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.UNAVAILABLE) {
                     return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
                  } else if (!this.o.containsKey(var6)) {
                     return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED;
                  } else {
                     var var8 = this.o.get(var6);
                     return var8 == null
                        ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN
                        : (var8.contains(var7) ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED);
                  }
               } else {
                  return var3.b().contains(var1.h()) ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT;
               }
            }
         }
      }
   }

   private static boolean b(String var0) {
      for (String var4 : var0.split("/")) {
         if ("..".equals(var4) || q.contains(var4)) {
            return true;
         }
      }

      return false;
   }

   static io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus a(io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var0, boolean var1) {
      if (var1) {
         return io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.PRESENT;
      } else {
         return var0 == io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness.COMPLETE ? io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.RETIRED : io.karatelabs.ext.coverage.collect.ExecutionInventory.DefinitionStatus.UNKNOWN;
      }
   }

   public Map<String, Object> e() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("scenarios", a(this.i, "features", this.k.size()));
      var1.put("checks", a(this.l, "checks", this.m.size()));
      var1.put("rulebooks", a(this.n, "books", this.o.size()));
      if (this.h != null) {
         var1.put("root", this.h.toString());
      }

      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.coverage.collect.ExecutionInventory.InventoryCompleteness var0, String var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("completeness", var0.wire());
      var3.put(var1, var2);
      return var3;
   }

   private static <T> Map<io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp, T> f() {
      return new LinkedHashMap<io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp, T>(64, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<io.karatelabs.ext.coverage.collect.ExecutionInventory.FileStamp, T> var1) {
            return this.size() > 1024;
         }
      };
   }

   public static enum InventoryCompleteness {
      COMPLETE,
      PARTIAL,
      UNAVAILABLE;

      String wire() {
         return this.name().toLowerCase();
      }
   }

   private record FeatureDefinition(Set<String> a, Set<String> b, boolean c) {

   }

   private record FileStamp(Path a, long b, long c) {

   }

   public static enum DefinitionStatus {
      PRESENT,
      RETIRED,
      UNKNOWN;
   }
}
