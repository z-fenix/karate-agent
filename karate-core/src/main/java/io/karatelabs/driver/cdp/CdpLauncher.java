package io.karatelabs.driver.cdp;

import io.karatelabs.common.OsUtils;
import io.karatelabs.output.LogContext;
import io.karatelabs.process.PortUtils;
import io.karatelabs.process.ProcessBuilder;
import io.karatelabs.process.ProcessHandle;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;

public class CdpLauncher {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   public static final String DEFAULT_PATH_MAC = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
   public static final String DEFAULT_PATH_WIN64 = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
   public static final String DEFAULT_PATH_WIN32 = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
   public static final String DEFAULT_PATH_LINUX = "/usr/bin/google-chrome";
   private static final Set<CdpLauncher> ACTIVE = ConcurrentHashMap.newKeySet();
   private final ProcessHandle process;
   private final String host;
   private final int port;
   private String webSocketUrl;
   private final AtomicBoolean closed = new AtomicBoolean(false);

   private CdpLauncher(ProcessHandle process, String host, int port) {
      this.process = process;
      this.host = host;
      this.port = port;
      ACTIVE.add(this);
   }

   public static void closeAll() {
      ACTIVE.forEach(CdpLauncher::close);
   }

   public static CdpLauncher start(CdpDriverOptions options) {
      if (options == null) {
         throw new IllegalArgumentException("options cannot be null");
      } else {
         String executable = resolveExecutable(options.getExecutable());
         int port = options.getPort() > 0 ? options.getPort() : PortUtils.findFreePort();
         String host = options.getHost();
         if (host == null || host.isEmpty()) {
            host = "localhost";
         }

         int timeout = Math.max(options.getTimeout(), 1000);
         List<String> args = buildArgs(executable, port, options);
         logger.debug("launching chrome: {}", args);
         ProcessHandle process = ProcessHandle.start(ProcessBuilder.create().args(args).logToContext(false).build());
         CdpLauncher launcher = new CdpLauncher(process, host, port);
         launcher.webSocketUrl = launcher.waitForWebSocketUrl(timeout);
         if (launcher.webSocketUrl == null) {
            launcher.close();
            throw new RuntimeException("chrome failed to start or no page targets available within timeout (" + timeout + "ms)");
         } else {
            logger.info("chrome started on port {} with WebSocket: {}", port, launcher.webSocketUrl);
            return launcher;
         }
      }
   }

   public static String getWebSocketUrl(String host, int port) {
      if (host == null || host.isEmpty()) {
         host = "localhost";
      }

      if (port > 0 && port <= 65535) {
         return fetchWebSocketUrl(host, port);
      } else {
         throw new IllegalArgumentException("port must be between 1 and 65535");
      }
   }

   private static String resolveExecutable(String configured) {
      if (configured != null && !configured.isEmpty()) {
         if (Files.isExecutable(Path.of(configured))) {
            return configured;
         }

         logger.warn("configured executable not found: {}", configured);
      }

      String defaultPath = getDefaultPath();
      if (defaultPath != null && Files.isExecutable(Path.of(defaultPath))) {
         logger.debug("using default chrome path: {}", defaultPath);
         return defaultPath;
      } else {
         throw new RuntimeException(
            "chrome executable not found. Set 'executable' option, KARATE_CHROME_EXECUTABLE env var, or install Chrome at: " + defaultPath
         );
      }
   }

   private static String getDefaultPath() {
      if (OsUtils.isMac()) {
         return "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
      } else if (OsUtils.isWindows()) {
         return Files.isRegularFile(Path.of("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
               && Files.isReadable(Path.of("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
            ? "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
            : "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
      } else {
         return OsUtils.isLinux() ? "/usr/bin/google-chrome" : null;
      }
   }

   private static List<String> buildArgs(String executable, int port, CdpDriverOptions options) {
      List<String> args = new ArrayList<>();
      args.add(executable);
      args.add("--remote-debugging-port=" + port);
      args.add("--remote-allow-origins=*");
      args.add("--no-first-run");
      args.add("--disable-popup-blocking");
      if (options.getUserDataDir() != null) {
         args.add("--user-data-dir=" + options.getUserDataDir());
      }

      if (options.isHeadless()) {
         args.add("--headless=new");
      }

      if (options.getUserAgent() != null) {
         args.add("--user-agent=" + options.getUserAgent());
      }

      if (options.getAddOptions() != null) {
         args.addAll(options.getAddOptions());
      }

      return args;
   }

   private String waitForWebSocketUrl(int timeoutMs) {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
      long startTime = System.nanoTime();
      long timeoutNanos = timeoutMs * 1000000L;
      String url = "http://" + this.host + ":" + this.port + "/json";
      int intervalMs = 250;
      int attemptCount = 0;

      while (attemptCount == 0 || System.nanoTime() - startTime < timeoutNanos) {
         attemptCount++;
         if (this.process != null && !this.process.isAlive()) {
            int exitCode = this.process.getExitCode();
            logger.warn("chrome process died while waiting for page targets (exit code: {})", exitCode);
            return null;
         }

         try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
               List<Map<String, Object>> targets = (List<Map<String, Object>>)JSONValue.parse(response.body());
               if (targets != null && !targets.isEmpty()) {
                  for (Map<String, Object> target : targets) {
                     String targetUrl = (String)target.get("url");
                     String targetType = (String)target.get("type");
                     if ((targetUrl == null || !targetUrl.startsWith("chrome-")) && "page".equals(targetType)) {
                        String wsUrl = (String)target.get("webSocketDebuggerUrl");
                        if (wsUrl != null) {
                           long elapsedMs = (System.nanoTime() - startTime) / 1000000L;
                           logger.debug("found page target after {} attempts ({}ms)", attemptCount, elapsedMs);
                           return wsUrl;
                        }
                     }
                  }
               }

               logger.trace("/json returned 200 but no page targets yet (attempt {})", attemptCount);
            }
         } catch (Exception var20) {
            logger.trace("/json not available (attempt {}): {}", attemptCount, var20.getMessage());
         }

         if (System.nanoTime() - startTime < timeoutNanos) {
            sleep(intervalMs);
         }
      }

      long elapsedMs = (System.nanoTime() - startTime) / 1000000L;
      logger.warn("no page targets available after {} attempts ({}ms)", attemptCount, elapsedMs);
      return null;
   }

   private static String fetchWebSocketUrl(String host, int port) {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();

      try {
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://" + host + ":" + port + "/json")).timeout(Duration.ofSeconds(5L)).GET().build();
         HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
         if (response.statusCode() == 200) {
            List<Map<String, Object>> targets = (List<Map<String, Object>>)JSONValue.parse(response.body());
            if (targets != null && !targets.isEmpty()) {
               for (Map<String, Object> target : targets) {
                  String targetUrl = (String)target.get("url");
                  String targetType = (String)target.get("type");
                  if ((targetUrl == null || !targetUrl.startsWith("chrome-")) && "page".equals(targetType)) {
                     String wsUrl = (String)target.get("webSocketDebuggerUrl");
                     if (wsUrl != null) {
                        return wsUrl;
                     }
                  }
               }
            }
         }

         request = HttpRequest.newBuilder().uri(URI.create("http://" + host + ":" + port + "/json/version")).timeout(Duration.ofSeconds(5L)).GET().build();
         response = client.send(request, BodyHandlers.ofString());
         if (response.statusCode() == 200) {
            Map<String, Object> version = (Map<String, Object>)JSONValue.parse(response.body());
            if (version != null) {
               return (String)version.get("webSocketDebuggerUrl");
            }
         }

         return null;
      } catch (Exception var11) {
         logger.warn("failed to fetch WebSocket URL: {}", var11.getMessage());
         return null;
      }
   }

   private static void sleep(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }
   }

   public String getWebSocketUrl() {
      return this.webSocketUrl;
   }

   public int getPort() {
      return this.port;
   }

   public String getHost() {
      return this.host;
   }

   public boolean isRunning() {
      return this.process != null && this.process.isAlive();
   }

   public void close() {
      if (this.closed.compareAndSet(false, true)) {
         ACTIVE.remove(this);
         if (this.process != null) {
            logger.debug("closing chrome process");
            this.process.close();
         }
      }
   }

   public void closeAndWait() {
      if (this.closed.compareAndSet(false, true)) {
         ACTIVE.remove(this);
         if (this.process != null) {
            logger.debug("closing chrome process and waiting");
            this.process.close();

            try {
               this.process.waitSync(5000L);
            } catch (Exception var2) {
               logger.warn("timeout waiting for chrome to close, forcing");
               this.process.close(true);
            }
         }
      }
   }

   public boolean isClosed() {
      return this.closed.get();
   }
}
