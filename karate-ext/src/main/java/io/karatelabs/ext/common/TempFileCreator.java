package io.karatelabs.ext.common;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public final class TempFileCreator {
   private TempFileCreator() {
   }

   public static Path a(Path var0, String var1, String var2) throws IOException {
      while (true) {
         Path var3 = var0.resolve(var1 + Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + var2);

         try {
            return Files.createFile(var3);
         } catch (FileAlreadyExistsException var5) {
         }
      }
   }
}
