package io.karatelabs.ext.robot;

import io.karatelabs.common.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RobotResourceLoader {
   public static String a(String var0) {
      try {
         String var2;
         try (InputStream var1 = RobotResourceLoader.class.getResourceAsStream(var0)) {
            if (var1 == null) {
               throw new IOException("app resource not found: " + var0);
            }

            var2 = FileUtils.toString(var1);
         }

         return var2;
      } catch (Exception var6) {
         throw new RuntimeException(var6);
      }
   }

   public static URL b(String var0) {
      return RobotResourceLoader.class.getResource(var0);
   }

   public static InputStream c(String var0) {
      return RobotResourceLoader.class.getResourceAsStream(var0);
   }
}
