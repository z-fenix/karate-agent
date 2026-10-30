package io.karatelabs.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
   public static final File WORKING_DIR = new File("").getAbsoluteFile();
   public static final String KARATE_OUTPUT_DIR = "karate.output.dir";

   private FileUtils() {
   }

   public static String getBuildDir() {
      String explicit = System.getProperty("karate.output.dir");
      if (explicit != null && !explicit.isEmpty()) {
         return explicit;
      } else {
         File gradleFile = new File(WORKING_DIR, "build.gradle");
         File gradleKtsFile = new File(WORKING_DIR, "build.gradle.kts");
         File settingsGradle = new File(WORKING_DIR, "settings.gradle");
         File settingsGradleKts = new File(WORKING_DIR, "settings.gradle.kts");
         boolean hasGradleFiles = gradleFile.exists() || gradleKtsFile.exists() || settingsGradle.exists() || settingsGradleKts.exists();
         if (hasGradleFiles) {
            return "build";
         } else {
            File pomFile = new File(WORKING_DIR, "pom.xml");
            if (pomFile.exists()) {
               return "target";
            } else {
               File buildDir = new File(WORKING_DIR, "build");
               File targetDir = new File(WORKING_DIR, "target");
               if (buildDir.isDirectory() && !targetDir.isDirectory()) {
                  return "build";
               } else if (targetDir.isDirectory() && !buildDir.isDirectory()) {
                  return "target";
               } else {
                  String command = System.getProperty("sun.java.command", "");
                  if (command.contains("org.gradle.")) {
                     return "build";
                  } else {
                     return System.getProperty("org.gradle.test.worker") != null ? "build" : "target";
                  }
               }
            }
         }
      }
   }

   public static String toString(File file) {
      try {
         return Files.readString(file.toPath(), StandardCharsets.UTF_8);
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   public static String toString(URL url) {
      return toString(toBytes(url));
   }

   public static byte[] toBytes(URL url) {
      try {
         byte[] var3;
         try (InputStream is = url.openStream()) {
            ByteArrayOutputStream os = toByteStream(is);
            var3 = os.toByteArray();
         }

         return var3;
      } catch (Exception var6) {
         throw new RuntimeException(var6);
      }
   }

   public static String toString(InputStream is) {
      try {
         return toByteStream(is).toString(StandardCharsets.UTF_8);
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   public static byte[] toBytes(File file) {
      try {
         return Files.readAllBytes(file.toPath());
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   public static byte[] toBytes(InputStream is) {
      return toByteStream(is).toByteArray();
   }

   public static ByteArrayOutputStream toByteStream(InputStream is) {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];

      try {
         int length;
         while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
         }

         return result;
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }
   }

   public static String toString(byte[] bytes) {
      return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
   }

   public static byte[] toBytes(String string) {
      return string == null ? null : string.getBytes(StandardCharsets.UTF_8);
   }

   public static void copy(File src, File dest) {
      try {
         Files.copy(src.toPath(), dest.toPath());
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   public static void writeToFile(File file, byte[] data) {
      try {
         Path path = file.toPath();
         Path parent = path.getParent();
         if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
         }

         Files.write(path, data);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public static void writeToFile(File file, String data) {
      writeToFile(file, data.getBytes(StandardCharsets.UTF_8));
   }

   public static InputStream toInputStream(String text) {
      return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
   }
}
