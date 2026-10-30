package io.karatelabs.ext.common;

import java.io.InputStream;
import java.util.Properties;

public final class ExtMeta {
   private ExtMeta() {
   }

   public static String version(String var0) {
      return property(var0, "version");
   }

   public static String property(String var0, String var1) {
      try (InputStream var2 = ExtMeta.class.getResourceAsStream("/" + var0)) {
         if (var2 != null) {
            Properties var3 = new Properties();
            var3.load(var2);
            return var3.getProperty(var1, "(unknown)");
         } else {
            return "(unknown)";
         }
      } catch (Exception var7) {
         return "(unknown)";
      }
   }
}
