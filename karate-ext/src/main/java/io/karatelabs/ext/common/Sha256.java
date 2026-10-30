package io.karatelabs.ext.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class Sha256 {
   private Sha256() {
   }

   public static String a(String var0) {
      return a(var0.getBytes(StandardCharsets.UTF_8));
   }

   public static String a(byte[] var0) {
      return HexFormat.of().formatHex(a().digest(var0));
   }

   public static String a(List<Path> var0) {
      MessageDigest var1 = a();

      for (Path var3 : var0) {
         byte[] var4;
         try {
            var4 = Files.isRegularFile(var3) ? Files.readAllBytes(var3) : null;
         } catch (IOException var6) {
            var4 = null;
         }

         var1.update((var3 + "\n" + (var4 == null ? -1 : var4.length) + "\n").getBytes(StandardCharsets.UTF_8));
         if (var4 != null) {
            var1.update(var4);
         }
      }

      return HexFormat.of().formatHex(var1.digest());
   }

   private static MessageDigest a() {
      try {
         return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException var1) {
         throw new AssertionError("SHA-256 is always available", var1);
      }
   }
}
