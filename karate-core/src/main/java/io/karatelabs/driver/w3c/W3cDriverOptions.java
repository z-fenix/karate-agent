package io.karatelabs.driver.w3c;

import io.karatelabs.driver.DriverOptions;
import io.karatelabs.driver.PageLoadStrategy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class W3cDriverOptions implements DriverOptions {
   private final W3cBrowserType browserType;
   private int timeout;
   private final int retryCount;
   private final int retryInterval;
   private final boolean headless;
   private final boolean screenshotOnFailure;
   private final boolean highlight;
   private final int highlightDuration;
   private final PageLoadStrategy pageLoadStrategy;
   private final String scope;
   private final boolean stop;
   private final String webDriverUrl;
   private final Map<String, Object> webDriverSession;
   private final Map<String, Object> capabilities;
   private final boolean start;
   private final String executable;
   private final int port;
   private final List<String> addOptions;
   private volatile String preSubmitHash;

   private W3cDriverOptions(Map<String, Object> map) {
      String type = (String)map.getOrDefault("type", "chromedriver");
      this.browserType = W3cBrowserType.fromType(type);
      if (this.browserType == null) {
         throw new IllegalArgumentException("Unknown W3C driver type: " + type);
      } else {
         this.timeout = toInt(map.get("timeout"), 30000);
         this.retryCount = toInt(map.get("retryCount"), 3);
         this.retryInterval = toInt(map.get("retryInterval"), 500);
         this.headless = toBool(map.get("headless"), false);
         this.screenshotOnFailure = toBool(map.get("screenshotOnFailure"), true);
         this.highlight = toBool(map.get("highlight"), false);
         this.highlightDuration = toInt(map.get("highlightDuration"), 3000);
         this.pageLoadStrategy = PageLoadStrategy.DOMCONTENT_AND_FRAMES;
         this.scope = (String)map.getOrDefault("scope", "scenario");
         this.stop = toBool(map.get("stop"), true);
         this.webDriverUrl = (String)map.get("webDriverUrl");
         this.webDriverSession = (Map<String, Object>)map.get("webDriverSession");
         this.capabilities = (Map<String, Object>)map.get("capabilities");
         this.start = toBool(map.get("start"), true);
         this.executable = (String)map.get("executable");
         this.port = toInt(map.get("port"), this.browserType.getDefaultPort());
         Object opts = map.get("addOptions");
         if (opts instanceof List) {
            this.addOptions = (List<String>)opts;
         } else {
            this.addOptions = new ArrayList<>();
         }
      }
   }

   public static W3cDriverOptions fromMap(Map<String, Object> map) {
      return new W3cDriverOptions(map);
   }

   public Map<String, Object> buildSessionPayload() {
      if (this.webDriverSession != null) {
         return new HashMap<>(this.webDriverSession);
      } else {
         Map<String, Object> session = new HashMap<>();
         Map<String, Object> caps = new HashMap<>();
         session.put("capabilities", caps);
         Map<String, Object> alwaysMatch = new HashMap<>();
         alwaysMatch.put("browserName", this.browserType.getBrowserName());
         if (this.capabilities != null) {
            alwaysMatch.putAll(this.capabilities);
         }

         caps.put("alwaysMatch", alwaysMatch);
         return session;
      }
   }

   @Override
   public int getTimeout() {
      return this.timeout;
   }

   @Override
   public void setTimeout(int millis) {
      this.timeout = millis;
   }

   @Override
   public Duration getTimeoutDuration() {
      return Duration.ofMillis(this.timeout);
   }

   @Override
   public int getRetryCount() {
      return this.retryCount;
   }

   @Override
   public int getRetryInterval() {
      return this.retryInterval;
   }

   @Override
   public boolean isHeadless() {
      return this.headless;
   }

   @Override
   public boolean isScreenshotOnFailure() {
      return this.screenshotOnFailure;
   }

   @Override
   public boolean isHighlight() {
      return this.highlight;
   }

   @Override
   public int getHighlightDuration() {
      return this.highlightDuration;
   }

   @Override
   public PageLoadStrategy getPageLoadStrategy() {
      return this.pageLoadStrategy;
   }

   @Override
   public String getPreSubmitHash() {
      return this.preSubmitHash;
   }

   @Override
   public void setPreSubmitHash(String hash) {
      this.preSubmitHash = hash;
   }

   public W3cBrowserType getBrowserType() {
      return this.browserType;
   }

   public String getWebDriverUrl() {
      return this.webDriverUrl;
   }

   public boolean isStart() {
      return this.start;
   }

   public String getExecutable() {
      return this.executable != null && !this.executable.isEmpty() ? this.executable : this.browserType.getExecutable();
   }

   public int getPort() {
      return this.port;
   }

   public List<String> getAddOptions() {
      return this.addOptions;
   }

   public String getScope() {
      return this.scope;
   }

   @Override
   public boolean isStop() {
      return this.stop;
   }

   public boolean isRemote() {
      return this.webDriverUrl != null && !this.webDriverUrl.isEmpty() || !this.start;
   }

   private static int toInt(Object value, int defaultValue) {
      if (value == null) {
         return defaultValue;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else {
         try {
            return Integer.parseInt(value.toString());
         } catch (NumberFormatException var3) {
            return defaultValue;
         }
      }
   }

   private static boolean toBool(Object value, boolean defaultValue) {
      if (value == null) {
         return defaultValue;
      } else {
         return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(value.toString());
      }
   }
}
