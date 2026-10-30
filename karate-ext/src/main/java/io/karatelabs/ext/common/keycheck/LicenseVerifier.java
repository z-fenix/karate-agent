package io.karatelabs.ext.common.keycheck;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LicenseVerifier {
   private static final Logger c = LoggerFactory.getLogger(LicenseVerifier.class);
   private static final String d = "0834d471dc6a8c03143bab975952f8f102ca814ffadca03d7ba5b0484e27058d";
   static final String a = ".karate/karate.lic";
   static final int b = 2;

   public static Map<String, Object> a() {
      return a(System.getenv("KARATE_LICENSE_PATH"), System.getenv("KARATE_LICENSE_TEXT"));
   }

   static Map<String, Object> a(String var0, String var1) {
      return a(var0, var1, new File("").getAbsoluteFile());
   }

   static Map<String, Object> a(String var0, String var1, File var2) {
      try {
         String var3;
         if (var0 != null && !var0.isBlank()) {
            File var6 = new File(var0);
            if (!var6.exists()) {
               throw new RuntimeException("karate license not found at KARATE_LICENSE_PATH: " + var6.getAbsolutePath());
            }

            var3 = Files.readString(var6.toPath(), StandardCharsets.UTF_8);
         } else if (var1 != null && !var1.isBlank()) {
            var3 = var1;
         } else {
            File var4 = a(var2);
            if (var4 == null) {
               throw new RuntimeException(
                  "karate license not found at: "
                     + new File(var2, ".karate/karate.lic").getPath()
                     + " (nor in up to 2 parent dirs, or set KARATE_LICENSE_PATH / KARATE_LICENSE_TEXT)"
               );
            }

            var3 = Files.readString(var4.toPath(), StandardCharsets.UTF_8);
         }

         return a(var3);
      } catch (Exception var5) {
         c.error("karate license invalid - {}", var5.getMessage());
         if (var5 instanceof RuntimeException) {
            throw (RuntimeException)var5;
         } else {
            throw new RuntimeException(var5);
         }
      }
   }

   static File a(File var0) {
      File var1 = var0 == null ? new File("").getAbsoluteFile() : var0;

      for (int var2 = 0; var1 != null && var2 <= 2; var2++) {
         File var3 = new File(var1, ".karate/karate.lic");
         if (var3.exists()) {
            return var3;
         }

         var1 = var1.getParentFile();
      }

      return null;
   }

   protected static Map<String, Object> a(String var0) {
      var0 = var0.replaceAll("(-----BEGIN LICENSE FILE-----|\\s|-----END LICENSE FILE-----)", "");
      byte[] var1 = a("license payload", var0.getBytes(StandardCharsets.UTF_8));
      var var2 = b(new String(var1, StandardCharsets.UTF_8));
      String var3 = a(var2, "enc");
      String var4 = a(var2, "sig");
      String var5 = a(var2, "ver");
      if (!var5.equals("1")) {
         throw new RuntimeException("license check - unexpected version: " + var5);
      } else {
         byte[] var6 = var3.getBytes(StandardCharsets.UTF_8);
         byte[] var7 = a("license sig", var4.getBytes(StandardCharsets.UTF_8));
         if (!a(HexFormat.of().parseHex("0834d471dc6a8c03143bab975952f8f102ca814ffadca03d7ba5b0484e27058d"), var6, var7)) {
            throw new RuntimeException("license check - signature verification failed");
         } else {
            byte[] var8 = a("license enc", var6);
            var var9 = b(new String(var8, StandardCharsets.UTF_8));
            String var10 = a(var9, "issued");
            String var11 = a(var9, "expiry");
            Instant var12 = Instant.now();

            Instant var13;
            Instant var14;
            try {
               var13 = Instant.parse(var10);
               var14 = Instant.parse(var11);
            } catch (DateTimeParseException var18) {
               throw new RuntimeException("license check - invalid issued/expiry timestamp");
            }

            long var15 = Duration.between(var12, var14).toDays();
            c.debug("karate license issued: {}, now: {}, expiry: {}, days-left: {}", new Object[]{var10, var12, var11, var15});
            if (!var13.isAfter(var12) && !var14.isBefore(var12)) {
               Object var17 = var9.get("user");
               if (var17 instanceof Map) {
                  ((Map)var17).put("daysLeft", var15);
               }

               return var9;
            } else {
               throw new RuntimeException("license check - expired");
            }
         }
      }
   }

   private static String a(Map<String, Object> var0, String var1) {
      if (var0 != null && var0.get(var1) instanceof String var2 && !var2.isBlank()) {
         return var2;
      } else {
         throw new RuntimeException("license check - missing or invalid '" + var1 + "' field");
      }
   }

   private static byte[] a(String var0, byte[] var1) {
      try {
         return Base64.getDecoder().decode(var1);
      } catch (IllegalArgumentException var3) {
         throw new RuntimeException("license check - malformed base64 (" + var0 + ")");
      }
   }

   private static Map<String, Object> b(String var0) {
      Object var1 = JSONValue.parse(var0);
      if (!(var1 instanceof Map)) {
         throw new RuntimeException("license check - unparseable license payload");
      } else {
         return (Map<String, Object>)var1;
      }
   }

   private static boolean a(byte[] var0, byte[] var1, byte[] var2) {
      if (var0.length != 32) {
         throw new IllegalArgumentException("ed25519 public key must be 32 bytes");
      } else {
         byte[] var3 = new byte[44];
         var3[0] = 48;
         var3[1] = 42;
         var3[2] = 48;
         var3[3] = 5;
         var3[4] = 6;
         var3[5] = 3;
         var3[6] = 43;
         var3[7] = 101;
         var3[8] = 112;
         var3[9] = 3;
         var3[10] = 33;
         var3[11] = 0;
         System.arraycopy(var0, 0, var3, 12, 32);

         try {
            PublicKey var4 = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(var3));
            Signature var5 = Signature.getInstance("Ed25519");
            var5.initVerify(var4);
            var5.update(var1);
            return var5.verify(var2);
         } catch (Exception var6) {
            throw new RuntimeException(var6);
         }
      }
   }
}
