package io.karatelabs.ext.requirements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequirementsRepository {
   private static final Logger b = LoggerFactory.getLogger(RequirementsRepository.class);
   public static final String a = "not-a-requirements-doc";
   private final List<Path> c;
   private final Path d;
   private io.karatelabs.ext.requirements.model.RequirementsBundle e;
   private final Map<String, String> f = new LinkedHashMap<>();
   private final Map<io.karatelabs.ext.requirements.model.RequirementNode, String> g = new IdentityHashMap<>();
   private final Map<String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> h = new LinkedHashMap<>();

   public RequirementsRepository(Path var1) {
      this(var1 == null ? List.of() : List.of(var1));
   }

   public RequirementsRepository(List<Path> var1) {
      this(var1, null);
   }

   public RequirementsRepository(List<Path> var1, Path var2) {
      this.c = var1 == null ? List.of() : List.copyOf(var1);
      this.d = var2;
   }

   public io.karatelabs.ext.requirements.model.RequirementsBundle a() {
      if (this.e != null) {
         return this.e;
      } else {
         ArrayList var1 = new ArrayList();
         ArrayList<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> var2 = new ArrayList();

         for (Path var4 : this.c) {
            if (var4 != null && Files.isDirectory(var4)) {
               for (Path var6 : c(var4)) {
                  try {
                     String var7 = Files.readString(var6, StandardCharsets.UTF_8);
                     String var8 = a(this.d, var4, var6);
                     if (!io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.b(var6)) {
                        List var9;
                        if (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.a(var6)) {
                           io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroSpecParseResult var10 = io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.a(
                              var7, a(var6), io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.c(var6), var8
                           );
                           var9 = var10.b();
                           this.h.putAll(var10.c());
                           var2.addAll(var10.d());
                        } else {
                           if (!io.karatelabs.ext.requirements.RequirementsMarkdownParser.b(var7)) {
                              var2.add(
                                 new io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic(
                                    "not-a-requirements-doc",
                                    var8,
                                    "parsed no requirements — the file shows none of the three authoring conventions (an `ID: Title` heading, an Acceptance/Constraints block, or an `@key=value` tag line), so its headings are prose"
                                 )
                              );
                              continue;
                           }

                           var9 = io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(var7).d();
                        }

                        for (io.karatelabs.ext.requirements.model.RequirementNode var11 : (Iterable<io.karatelabs.ext.requirements.model.RequirementNode>) (Iterable<?>) (var9)) {
                           this.f.putIfAbsent(var11.b(), var8);
                           this.g.put(var11, var8);
                        }

                        var1.addAll(var9);
                     }
                  } catch (RuntimeException | IOException var12) {
                     b.warn("requirements: skipping {} — {}", var6, var12.getMessage());
                  }
               }
            }
         }

         this.e = io.karatelabs.ext.requirements.model.RequirementsBundle.a(var1, var2);
         return this.e;
      }
   }

   public Map<String, String> b() {
      this.a();
      return this.f;
   }

   public String a(io.karatelabs.ext.requirements.model.RequirementNode var1) {
      this.a();
      return this.g.get(var1);
   }

   public Map<String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> c() {
      this.a();
      return this.h;
   }

   private static String a(Path var0) {
      Path var1 = var0.resolveSibling("tasks.md");
      if (!Files.isRegularFile(var1)) {
         return null;
      } else {
         try {
            return Files.readString(var1, StandardCharsets.UTF_8);
         } catch (IOException var3) {
            b.warn("requirements: cannot read {} — {}", var1, var3.getMessage());
            return null;
         }
      }
   }

   public static String a(Path var0, Path var1, Path var2) {
      return a(var0 != null ? var0 : b(var1), var2);
   }

   private static Path b(Path var0) {
      return var0.toAbsolutePath().normalize().getParent();
   }

   private static String a(Path var0, Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();

      try {
         if (var0 != null) {
            return var0.relativize(var2).toString().replace('\\', '/');
         }
      } catch (IllegalArgumentException var4) {
      }

      return var2.getFileName().toString();
   }

   public List<io.karatelabs.ext.requirements.model.RequirementNode> d() {
      return this.a().d();
   }

   public io.karatelabs.ext.requirements.model.RequirementNode a(String var1) {
      return this.a().a(var1);
   }

   private static List<Path> c(Path var0) {
      try {
         List var2;
         try (var var1 = Files.walk(var0)) {
            var2 = var1.filter(var0x -> Files.isRegularFile(var0x))
               .filter(var0x -> var0x.getFileName().toString().toLowerCase().endsWith(".md"))
               .sorted(Comparator.comparing(Path::toString))
               .toList();
         }

         return var2;
      } catch (IOException var6) {
         b.warn("requirements: cannot walk {} — {}", var0, var6.getMessage());
         return List.of();
      }
   }
}
