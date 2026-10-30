package io.karatelabs.driver.w3c;

import java.util.HashMap;
import java.util.Map;

public enum W3cBrowserType {
   CHROMEDRIVER("chromedriver", "chrome", "chromedriver", 9515, "--port=%d"),
   GECKODRIVER("geckodriver", "firefox", "geckodriver", 4444, "--port=%d"),
   SAFARIDRIVER("safaridriver", "safari", "safaridriver", 5555, "--port=%d"),
   MSEDGEDRIVER("msedgedriver", "MicrosoftEdge", "msedgedriver", 9515, "--port=%d");

   private static final Map<String, W3cBrowserType> BY_TYPE = new HashMap<>();
   final String karateType;
   final String browserName;
   final String executable;
   final int defaultPort;
   final String portArgFormat;

   private W3cBrowserType(String karateType, String browserName, String executable, int defaultPort, String portArgFormat) {
      this.karateType = karateType;
      this.browserName = browserName;
      this.executable = executable;
      this.defaultPort = defaultPort;
      this.portArgFormat = portArgFormat;
   }

   public static boolean isW3cType(String type) {
      return type != null && BY_TYPE.containsKey(type);
   }

   public static W3cBrowserType fromType(String type) {
      return BY_TYPE.get(type);
   }

   public String getKarateType() {
      return this.karateType;
   }

   public String getBrowserName() {
      return this.browserName;
   }

   public String getExecutable() {
      return this.executable;
   }

   public int getDefaultPort() {
      return this.defaultPort;
   }

   public String formatPortArg(int port) {
      return String.format(this.portArgFormat, port);
   }

   static {
      for (W3cBrowserType bt : values()) {
         BY_TYPE.put(bt.karateType, bt);
      }
   }
}
