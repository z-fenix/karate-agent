package io.karatelabs.driver.cdp;

import io.karatelabs.driver.DriverOptions;
import io.karatelabs.driver.PageLoadStrategy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CdpDriverOptions implements DriverOptions {
   private int timeout;
   private final int retryCount;
   private final int retryInterval;
   private final boolean headless;
   private final String host;
   private final int port;
   private final String executable;
   private final String userDataDir;
   private final String userAgent;
   private final boolean screenshotOnFailure;
   private final boolean highlight;
   private final int highlightDuration;
   private final PageLoadStrategy pageLoadStrategy;
   private final List<String> addOptions;
   private final String webSocketUrl;
   private final String scope;
   private final boolean stop;
   private volatile String preSubmitHash;

   private CdpDriverOptions(CdpDriverOptions.Builder builder) {
      this.timeout = builder.timeout;
      this.retryCount = builder.retryCount;
      this.retryInterval = builder.retryInterval;
      this.headless = builder.headless;
      this.host = builder.host;
      this.port = builder.port;
      this.executable = builder.executable;
      this.userDataDir = builder.userDataDir;
      this.userAgent = builder.userAgent;
      this.screenshotOnFailure = builder.screenshotOnFailure;
      this.highlight = builder.highlight;
      this.highlightDuration = builder.highlightDuration;
      this.pageLoadStrategy = builder.pageLoadStrategy;
      this.addOptions = builder.addOptions != null ? List.copyOf(builder.addOptions) : List.of();
      this.webSocketUrl = builder.webSocketUrl;
      this.scope = builder.scope;
      this.stop = builder.stop;
   }

   public static CdpDriverOptions.Builder builder() {
      return new CdpDriverOptions.Builder();
   }

   public static CdpDriverOptions fromMap(Map<String, Object> map) {
      CdpDriverOptions.Builder builder = builder();
      if (map == null) {
         return builder.build();
      } else {
         if (map.containsKey("timeout")) {
            builder.timeout(toInt(map.get("timeout")));
         }

         if (map.containsKey("retryCount")) {
            builder.retryCount(toInt(map.get("retryCount")));
         }

         if (map.containsKey("retryInterval")) {
            builder.retryInterval(toInt(map.get("retryInterval")));
         }

         if (map.containsKey("headless")) {
            builder.headless(toBoolean(map.get("headless")));
         }

         if (map.containsKey("host")) {
            builder.host((String)map.get("host"));
         }

         if (map.containsKey("port")) {
            builder.port(toInt(map.get("port")));
         }

         if (map.containsKey("executable")) {
            builder.executable((String)map.get("executable"));
         }

         if (map.containsKey("userDataDir")) {
            builder.userDataDir((String)map.get("userDataDir"));
         }

         if (map.containsKey("userAgent")) {
            builder.userAgent((String)map.get("userAgent"));
         }

         if (map.containsKey("screenshotOnFailure")) {
            builder.screenshotOnFailure(toBoolean(map.get("screenshotOnFailure")));
         }

         if (map.containsKey("highlight")) {
            builder.highlight(toBoolean(map.get("highlight")));
         }

         if (map.containsKey("highlightDuration")) {
            builder.highlightDuration(toInt(map.get("highlightDuration")));
         }

         if (map.containsKey("pageLoadStrategy")) {
            Object strategy = map.get("pageLoadStrategy");
            if (strategy instanceof PageLoadStrategy) {
               builder.pageLoadStrategy((PageLoadStrategy)strategy);
            } else if (strategy instanceof String) {
               builder.pageLoadStrategy(PageLoadStrategy.valueOf(((String)strategy).toUpperCase()));
            }
         }

         if (map.containsKey("addOptions")) {
            builder.addOptions((List<String>)map.get("addOptions"));
         }

         if (map.containsKey("webSocketUrl")) {
            builder.webSocketUrl((String)map.get("webSocketUrl"));
         }

         if (map.containsKey("scope")) {
            builder.scope((String)map.get("scope"));
         }

         if (map.containsKey("stop")) {
            builder.stop(toBoolean(map.get("stop")));
         }

         return builder.build();
      }
   }

   private static int toInt(Object value) {
      return value instanceof Number ? ((Number)value).intValue() : Integer.parseInt(value.toString());
   }

   private static boolean toBoolean(Object value) {
      return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(value.toString());
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
   public int getRetryCount() {
      return this.retryCount;
   }

   @Override
   public int getRetryInterval() {
      return this.retryInterval;
   }

   @Override
   public boolean isHeadless() {
      return this.headless ? true : "true".equalsIgnoreCase(System.getenv("KARATE_DRIVER_HEADLESS"));
   }

   public String getHost() {
      return this.host;
   }

   public int getPort() {
      return this.port;
   }

   public String getExecutable() {
      return this.executable != null && !this.executable.isEmpty() ? this.executable : System.getenv("KARATE_CHROME_EXECUTABLE");
   }

   public String getUserDataDir() {
      return this.userDataDir;
   }

   public String getUserAgent() {
      return this.userAgent;
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

   public List<String> getAddOptions() {
      String envArgs = System.getenv("KARATE_CHROME_ARGS");
      if (envArgs != null && !envArgs.isEmpty()) {
         List<String> merged = new ArrayList<>(this.addOptions);

         for (String arg : envArgs.split("\\s+")) {
            if (!arg.isEmpty()) {
               merged.add(arg);
            }
         }

         return merged;
      } else {
         return this.addOptions;
      }
   }

   public String getWebSocketUrl() {
      return this.webSocketUrl;
   }

   public String getScope() {
      return this.scope;
   }

   public boolean isCallerScope() {
      return "caller".equals(this.scope);
   }

   @Override
   public boolean isStop() {
      return this.stop;
   }

   @Override
   public Duration getTimeoutDuration() {
      return Duration.ofMillis(this.timeout);
   }

   public static class Builder {
      private int timeout = 30000;
      private int retryCount = 3;
      private int retryInterval = 500;
      private boolean headless = false;
      private String host = "localhost";
      private int port = 0;
      private String executable;
      private String userDataDir;
      private String userAgent;
      private boolean screenshotOnFailure = true;
      private boolean highlight = false;
      private int highlightDuration = 3000;
      private PageLoadStrategy pageLoadStrategy = PageLoadStrategy.DOMCONTENT_AND_FRAMES;
      private List<String> addOptions;
      private String webSocketUrl;
      private String scope = "scenario";
      private boolean stop = true;

      public CdpDriverOptions.Builder timeout(int millis) {
         this.timeout = millis;
         return this;
      }

      public CdpDriverOptions.Builder timeout(Duration duration) {
         this.timeout = (int)duration.toMillis();
         return this;
      }

      public CdpDriverOptions.Builder retryCount(int count) {
         this.retryCount = count;
         return this;
      }

      public CdpDriverOptions.Builder retryInterval(int millis) {
         this.retryInterval = millis;
         return this;
      }

      public CdpDriverOptions.Builder headless(boolean headless) {
         this.headless = headless;
         return this;
      }

      public CdpDriverOptions.Builder host(String host) {
         this.host = host;
         return this;
      }

      public CdpDriverOptions.Builder port(int port) {
         this.port = port;
         return this;
      }

      public CdpDriverOptions.Builder executable(String path) {
         this.executable = path;
         return this;
      }

      public CdpDriverOptions.Builder userDataDir(String path) {
         this.userDataDir = path;
         return this;
      }

      public CdpDriverOptions.Builder userAgent(String userAgent) {
         this.userAgent = userAgent;
         return this;
      }

      public CdpDriverOptions.Builder screenshotOnFailure(boolean enabled) {
         this.screenshotOnFailure = enabled;
         return this;
      }

      public CdpDriverOptions.Builder highlight(boolean enabled) {
         this.highlight = enabled;
         return this;
      }

      public CdpDriverOptions.Builder highlightDuration(int millis) {
         this.highlightDuration = millis;
         return this;
      }

      public CdpDriverOptions.Builder pageLoadStrategy(PageLoadStrategy strategy) {
         this.pageLoadStrategy = strategy;
         return this;
      }

      public CdpDriverOptions.Builder addOptions(List<String> options) {
         this.addOptions = options != null ? new ArrayList<>(options) : null;
         return this;
      }

      public CdpDriverOptions.Builder addOption(String option) {
         if (this.addOptions == null) {
            this.addOptions = new ArrayList<>();
         }

         this.addOptions.add(option);
         return this;
      }

      public CdpDriverOptions.Builder webSocketUrl(String url) {
         this.webSocketUrl = url;
         return this;
      }

      public CdpDriverOptions.Builder scope(String scope) {
         this.scope = scope != null ? scope : "scenario";
         return this;
      }

      public CdpDriverOptions.Builder stop(boolean stop) {
         this.stop = stop;
         return this;
      }

      public CdpDriverOptions build() {
         if (this.userDataDir == null) {
            this.userDataDir = Path.of("target", "chrome-temp-" + UUID.randomUUID().toString().substring(0, 8)).toAbsolutePath().toString();
         }

         return new CdpDriverOptions(this);
      }
   }
}
