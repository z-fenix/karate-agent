package io.karatelabs.ext.robot;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VideoRecorder {
   private static final Logger a = LoggerFactory.getLogger(VideoRecorder.class);

   private VideoRecorder() {
   }

   public static Process a(Path var0) {
      try {
         Files.createDirectories(var0.getParent());
         String var1 = System.getenv().getOrDefault("DISPLAY", ":1");
         String var2 = System.getenv().getOrDefault("KARATE_WIDTH", "1440");
         String var3 = System.getenv().getOrDefault("KARATE_HEIGHT", "900");
         Process var4 = new ProcessBuilder(
               "ffmpeg",
               "-y",
               "-f",
               "x11grab",
               "-r",
               "8",
               "-s",
               var2 + "x" + var3,
               "-i",
               var1,
               "-vcodec",
               "libx264",
               "-pix_fmt",
               "yuv420p",
               "-preset",
               "fast",
               "-g",
               "16",
               "-movflags",
               "frag_keyframe+empty_moov",
               var0.toString()
            )
            .redirectErrorStream(true)
            .redirectOutput(Redirect.DISCARD)
            .start();
         Runtime.getRuntime().addShutdownHook(new Thread(() -> a(var4), "video-shutdown-" + var4.pid()));
         a.info("video recording started: {}", var0);
         return var4;
      } catch (Exception var5) {
         a.warn("video recording failed to start: {}", var5.getMessage());
         return null;
      }
   }

   public static void a(Process var0) {
      if (var0 != null && var0.isAlive()) {
         var0.destroy();

         try {
            var0.waitFor(5L, TimeUnit.SECONDS);
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }

         if (var0.isAlive()) {
            var0.destroyForcibly();
         }

         a.info("video recording stopped");
      }
   }
}
