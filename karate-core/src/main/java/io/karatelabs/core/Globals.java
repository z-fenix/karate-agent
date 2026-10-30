package io.karatelabs.core;

import java.io.InputStream;
import java.util.Properties;

public final class Globals {
   public static final String KARATE_VERSION = loadVersion();

   private static String loadVersion() {
      try (InputStream is = Globals.class.getResourceAsStream("/karate-meta.properties")) {
         if (is != null) {
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("karate.version", "(unknown)");
         } else {
            return "(unknown)";
         }
      } catch (Exception var5) {
         return "(unknown)";
      }
   }

   private Globals() {
   }
}
