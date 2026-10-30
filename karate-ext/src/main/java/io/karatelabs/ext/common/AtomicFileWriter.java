package io.karatelabs.ext.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFileWriter {
   private AtomicFileWriter() {
   }

   public static void a(Path var0, String var1) throws IOException {
      a(var0, var1.getBytes(StandardCharsets.UTF_8));
   }

   public static void a(Path var0, byte[] var1) throws IOException {
      Path var2 = var0.resolveSibling("." + var0.getFileName() + "." + System.nanoTime() + ".tmp");

      try {
         Files.write(var2, var1);

         try {
            Files.move(var2, var0, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var7) {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(var2);
      }
   }
}
