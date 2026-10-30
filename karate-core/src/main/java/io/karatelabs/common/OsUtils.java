package io.karatelabs.common;

public class OsUtils {
   public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
   public static final String USER_HOME = System.getProperty("user.home");

   private OsUtils() {
   }

   public static boolean isWindows() {
      return OS_NAME.contains("win");
   }

   public static boolean isMac() {
      return OS_NAME.contains("mac");
   }

   public static boolean isWindowsOrMac() {
      return isWindows() || isMac();
   }

   public static boolean isLinux() {
      return OS_NAME.contains("nix") || OS_NAME.contains("nux");
   }

   public static String getOsType() {
      if (isWindows()) {
         return "windows";
      } else if (isMac()) {
         return "macosx";
      } else {
         return isLinux() ? "linux" : "unknown";
      }
   }
}
