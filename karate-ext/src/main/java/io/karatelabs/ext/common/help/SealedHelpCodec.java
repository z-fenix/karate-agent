package io.karatelabs.ext.common.help;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class SealedHelpCodec {
   private static final byte[] a = new byte[]{75, 72, 83, 1};
   private static final byte[] b = "karate-help-licensed-content".getBytes(StandardCharsets.US_ASCII);
   private static final Pattern c = Pattern.compile("<!--.*?-->\\n?", 32);

   private SealedHelpCodec() {
   }

   public static void a(String[] var0) throws IOException {
      Path var1 = Path.of(var0[0]);

      List var2;
      try (var var3 = Files.walk(var1)) {
         var2 = var3.filter(var0x -> var0x.toString().endsWith(".md")).toList();
      }

      int var10 = 0;

      for (Path var5 : (Iterable<Path>) (Iterable<?>) (var2)) {
         byte[] var6 = Files.readAllBytes(var5);
         if (!a(var6)) {
            String var7 = c.matcher(new String(var6, StandardCharsets.UTF_8)).replaceAll("");
            Files.write(var5, a(var7));
            var10++;
         }
      }

      System.out.println("sealed " + var10 + " help files under " + var1);
   }

   static byte[] a(String var0) {
      Deflater var1 = new Deflater(9);
      var1.setInput(var0.getBytes(StandardCharsets.UTF_8));
      var1.finish();
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      var2.writeBytes(a);
      byte[] var3 = new byte[8192];

      while (!var1.finished()) {
         int var4 = var1.deflate(var3);
         var2.write(var3, 0, var4);
      }

      var1.end();
      byte[] var5 = var2.toByteArray();
      c(var5);
      return var5;
   }

   static boolean a(byte[] var0) {
      if (var0.length < a.length) {
         return false;
      } else {
         for (int var1 = 0; var1 < a.length; var1++) {
            if (var0[var1] != a[var1]) {
               return false;
            }
         }

         return true;
      }
   }

   static String b(byte[] var0) {
      byte[] var1 = (byte[])var0.clone();
      c(var1);
      Inflater var2 = new Inflater();
      var2.setInput(var1, a.length, var1.length - a.length);
      ByteArrayOutputStream var3 = new ByteArrayOutputStream();
      byte[] var4 = new byte[8192];

      try {
         while (!var2.finished()) {
            int var5 = var2.inflate(var4);
            if (var5 == 0 && var2.needsInput()) {
               break;
            }

            var3.write(var4, 0, var5);
         }
      } catch (DataFormatException var9) {
         throw new UncheckedIOException(new IOException("corrupt sealed help resource", var9));
      } finally {
         var2.end();
      }

      return var3.toString(StandardCharsets.UTF_8);
   }

   private static void c(byte[] var0) {
      for (int var1 = a.length; var1 < var0.length; var1++) {
         var0[var1] ^= b[var1 % b.length];
      }
   }
}
