package io.karatelabs.ext.mutation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public final class MutantHomeMaterializer {
   private MutantHomeMaterializer() {
   }

   public static Path materialize(Path var0, io.karatelabs.ext.rules.MutationGenerator.Mutant var1, Path var2) {
      String var3 = var0.getFileName().toString();
      Path var4 = var2.resolve(var1.d()).resolve("rulebooks");
      Path var5 = var4.resolve(var3);

      try {
         Files.createDirectories(var5);

         try (var var6 = Files.list(var0)) {
            for (Path var8 : (Iterable<Path>) (Iterable<?>) (var6.filter(var0x -> Files.isRegularFile(var0x)).sorted().toList())) {
               Files.copy(var8, var5.resolve(var8.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            }
         }

         Path var16 = var0.resolve("tables");
         if (Files.isDirectory(var16)) {
            try (var var17 = Files.walk(var16)) {
               for (Path var9 : (Iterable<Path>) (Iterable<?>) (var17.filter(var0x -> Files.isRegularFile(var0x)).sorted().toList())) {
                  Path var10 = var5.resolve(var0.relativize(var9).toString());
                  Files.createDirectories(var10.getParent());
                  Files.copy(var9, var10, StandardCopyOption.REPLACE_EXISTING);
               }
            }
         }

         Files.writeString(var5.resolve("calc.js"), var1.m());
         if (var1.r() != null) {
            Files.writeString(var5.resolve(var1.r().d()), var1.a().a(var1.r().d()));
         }

         return var4;
      } catch (IOException var15) {
         throw new UncheckedIOException("failed to materialize mutant home for " + var1.d(), var15);
      }
   }
}
