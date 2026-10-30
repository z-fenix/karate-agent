package io.karatelabs.ext.postman;

import io.karatelabs.common.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class PostmanResourceLoader {
   public static String a(String var0) {
      try {
         String var2;
         try (InputStream var1 = PostmanResourceLoader.class.getResourceAsStream(var0)) {
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
      return PostmanResourceLoader.class.getResource(var0);
   }

   public static String c(String var0) {
      return b(var0).toExternalForm();
   }

   public static InputStream d(String var0) {
      return PostmanResourceLoader.class.getResourceAsStream(var0);
   }
}
