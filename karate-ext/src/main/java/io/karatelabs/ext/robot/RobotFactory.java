package io.karatelabs.ext.robot;

import io.karatelabs.driver.Driver;
import io.karatelabs.driver.PageLoadStrategy;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.driver.cdp.CdpDriverOptions;
import io.karatelabs.driver.cdp.CdpLauncher;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobotFactory implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(RobotFactory.class);
   private static final AtomicInteger INSTANCE_SEQ = new AtomicInteger(0);
   private static final Map<Integer, Bot> AGENTS = new ConcurrentHashMap<>();
   private final Consumer<String> logCallback;
   private final Set<Bot> owned = ConcurrentHashMap.newKeySet();
   private final Object lifecycle = new Object();
   private boolean closed;
   private Path defaultScreenshotsDir;
   private Consumer<Bot> onCreate;
   private Supplier<String> createGuard;
   private Supplier<Bot> activeBot;
   private Consumer<Bot> onDestroy;
   private Consumer<Bot> onBeforeClose;
   private io.karatelabs.ext.robot.RobotFactory.StepListener onStep;

   public RobotFactory() {
      this(null);
   }

   public RobotFactory onCreate(Consumer<Bot> var1) {
      this.onCreate = var1;
      return this;
   }

   public RobotFactory onCreateGuard(Supplier<String> var1) {
      this.createGuard = var1;
      return this;
   }

   public RobotFactory onActiveBot(Supplier<Bot> var1) {
      this.activeBot = var1;
      return this;
   }

   public RobotFactory onDestroy(Consumer<Bot> var1) {
      this.onDestroy = var1;
      return this;
   }

   public RobotFactory onBeforeClose(Consumer<Bot> var1) {
      this.onBeforeClose = var1;
      return this;
   }

   public RobotFactory onStep(io.karatelabs.ext.robot.RobotFactory.StepListener var1) {
      this.onStep = var1;
      return this;
   }

   public RobotFactory(Consumer<String> var1) {
      this.logCallback = var1;
   }

   public void setDefaultScreenshotsDir(Path var1) {
      this.defaultScreenshotsDir = var1;
   }

   @Override
   public Object jsGet(String var1) {
      if ("create".equals(var1)) {
         return (io.karatelabs.js.JavaInvokable) this::create;
      } else if ("help".equals(var1)) {
         return (io.karatelabs.js.JavaInvokable) RobotFactory::helpStatic;
      } else if ("step".equals(var1) && this.onStep != null) {
         return (JavaInvokable)var1x -> {
            String var2x = var1x.length > 0 && var1x[0] != null ? var1x[0].toString() : "Step";
            String var3 = var1x.length > 1 && var1x[1] != null ? var1x[1].toString() : null;
            String var4 = var1x.length > 2 && var1x[2] != null ? var1x[2].toString() : null;
            this.onStep.step(var2x, var3, var4);
            return "Step recorded: " + var2x;
         };
      } else if (("success".equals(var1) || "failure".equals(var1)) && this.onStep != null) {
         boolean var2 = "success".equals(var1);
         return (JavaInvokable)var3 -> {
            String var4 = var3.length > 0 && var3[0] != null ? var3[0].toString() : (var2 ? "success" : "failure");
            this.onStep.step(var4, var2 ? "pass" : "fail", null);
            return "Recorded as a "
               + (var2 ? "pass" : "fail")
               + " milestone — Robot."
               + var1
               + " is the loop's run-summary; live, the report is built from your steps/matches and you end with bot.destroy(). Message: "
               + var4;
         };
      } else {
         return null;
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return this.onStep != null ? List.of("create", "step", "success", "failure", "help") : List.of("create", "help");
   }

   public static Object helpStatic(Object... var0) {
      return io.karatelabs.ext.common.help.HelpTopicProvider.a("robot").a(var0);
   }

   private Bot create(Object... var1) {
      boolean var2 = false;
      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = true;
      boolean var6 = true;
      String var7 = null;
      String var8 = null;
      if (var1.length > 0 && var1[0] instanceof Map var9) {
         var2 = Boolean.TRUE.equals(var9.get("headless"));
         var3 = Boolean.TRUE.equals(var9.get("record"));
         var4 = Boolean.TRUE.equals(var9.get("fresh"));
         var5 = !Boolean.FALSE.equals(var9.get("reset"));
         var6 = !Boolean.FALSE.equals(var9.get("reuse"));
         if (var9.containsKey("userDataDir")) {
            var7 = String.valueOf(var9.get("userDataDir"));
         }

         if (var9.containsKey("cdpUrl")) {
            var8 = String.valueOf(var9.get("cdpUrl"));
         }
      }

      if (this.createGuard != null) {
         String var16 = this.createGuard.get();
         if (var16 != null) {
            if (var6 && this.activeBot != null) {
               Bot var18 = this.activeBot.get();
               if (var18 != null && var18.isAlive()) {
                  var18.borrow();
                  this.log("Robot.create() reused the active bot #" + var18.getId() + " (borrowed; pass {reuse:false} to force a new one)");
                  return var18;
               }
            }

            throw new IllegalStateException(var16);
         }
      }

      int var17 = INSTANCE_SEQ.incrementAndGet();
      this.log("Starting agent #" + var17 + (var2 ? " (headless)" : "") + (var3 ? " (recording)" : ""));
      String var10 = resolveAttachUrl(var8);
      Driver var11 = initializeDriver(var17, var7, var2, var10, var5, this.logCallback);
      if (var4) {
         var11.clearCookies();

         try {
            var11.script("localStorage.clear(); sessionStorage.clear()");
         } catch (Exception var15) {
            logger.debug("bot #{} storage clear skipped (about:blank): {}", var17, var15.getMessage());
         }

         this.log("bot #" + var17 + " fresh start (cookies + storage cleared)");
      }

      Bot var12 = new Bot(var17, var11, this.logCallback, var3);
      var12.setAttached(var10 != null);
      if (this.defaultScreenshotsDir != null) {
         var12.getAgentDriver().setScreenshotsDir(this.defaultScreenshotsDir);
      }

      this.register(var12);
      if (this.onDestroy != null) {
         Consumer var13 = this.onDestroy;
         var12.setOnDestroy(() -> {
            try {
               var13.accept(var12);
            } catch (Exception var4x) {
               logger.debug("onDestroy hook failed for bot #{}: {}", var17, var4x.getMessage());
            }
         });
      }

      if (this.onBeforeClose != null) {
         Consumer var19 = this.onBeforeClose;
         var12.setOnBeforeClose(() -> var19.accept(var12));
      }

      if (this.onCreate != null) {
         try {
            this.onCreate.accept(var12);
         } catch (Exception var14) {
            logger.debug("onCreate hook failed for bot #{}: {}", var17, var14.getMessage());
         }
      }

      return var12;
   }

   public static Bot createDirect(boolean var0, boolean var1, Path var2, Consumer<String> var3) {
      int var4 = INSTANCE_SEQ.incrementAndGet();
      log(var3, "Starting agent #" + var4 + (var0 ? " (headless)" : "") + (var1 ? " (recording)" : ""));
      String var5 = resolveAttachUrl(null);
      Driver var6 = initializeDriver(var4, null, var0, var5, true, var3);
      Bot var7 = new Bot(var4, var6, var3, var1);
      var7.setAttached(var5 != null);
      if (var2 != null) {
         var7.getAgentDriver().setScreenshotsDir(var2.resolve("screenshots"));
      }

      AGENTS.put(var4, var7);
      return var7;
   }

   private static String resolveAttachUrl(String var0) {
      String var1 = var0 != null && !var0.isEmpty() ? var0 : System.getenv("KARATE_CDP_URL");
      return var1 != null && !var1.isEmpty() ? var1 : null;
   }

   private static Driver initializeDriver(int var0, String var1, boolean var2, String var3, boolean var4, Consumer<String> var5) {
      if (var1 == null) {
         var1 = System.getProperty("java.io.tmpdir") + "/karate-agent-" + var0 + "-" + System.currentTimeMillis();
      }

      if (var3 == null) {
         createChromePreferences(var1);
      }

      CdpDriverOptions.Builder var6 = CdpDriverOptions.builder().userDataDir(var1).pageLoadStrategy(PageLoadStrategy.DOMCONTENT);
      if (var2) {
         var6.headless(true);
      }

      CdpDriver var7;
      if (var3 != null) {
         log(var5, "bot #" + var0 + " connecting to existing browser at " + var3);
         String var8;
         if (!var3.startsWith("ws://") && !var3.startsWith("wss://")) {
            URI var9 = URI.create(var3);
            var8 = CdpLauncher.getWebSocketUrl(var9.getHost(), var9.getPort());
         } else {
            var8 = var3;
         }

         if (var8 == null) {
            throw new RuntimeException("No browser page target found at " + var3 + ". Ensure Chrome is running with --remote-debugging-port");
         }

         var7 = CdpDriver.connect(var8, var6.build());
         if (var4) {
            collapseToSingleTab(var7, var0, pageTargetIdFromWsUrl(var8));

            try {
               var7.clearCookies();
            } catch (Exception var12) {
               logger.debug("bot #{} attach cookie-clear skipped: {}", var0, var12.getMessage());
            }

            try {
               var7.activate();
            } catch (Exception var11) {
               logger.debug("bot #{} bringToFront on attach skipped: {}", var0, var11.getMessage());
            }

            var7.navigate("about:blank");
            log(var5, "bot #" + var0 + " connected to existing browser (state reset: single foreground tab, cookies cleared, about:blank)");
         } else {
            log(var5, "bot #" + var0 + " connected to existing browser (reset:false — state kept)");
         }
      } else {
         var7 = CdpDriver.start(var6.build());
         log(var5, "bot #" + var0 + " browser started");
      }

      waitForBrowserReady(var7, var0);

      try {
         var7.maximize();
         log(var5, "bot #" + var0 + " browser maximized");
      } catch (Exception var10) {
         logger.warn("bot #{} failed to maximize: {}", var0, var10.getMessage());
      }

      return var7;
   }

   private static void collapseToSingleTab(Driver var0, int var1, String var2) {
      try {
         var var3 = var0.getPages();
         if (var3 == null || var3.size() <= 1) {
            return;
         }

         String var4 = var2 != null && var3.contains(var2) ? var2 : (String)var3.get(0);
         int var5 = 0;

         for (String var7 : (Iterable<String>) (Iterable<?>) (var3)) {
            if (!var7.equals(var4)) {
               try {
                  var0.switchPageById(var7);
                  var0.close();
                  var5++;
               } catch (Exception var10) {
                  logger.debug("bot #{} could not close stale tab {}: {}", new Object[]{var1, var7, var10.getMessage()});
               }
            }
         }

         try {
            var0.switchPageById(var4);
         } catch (Exception var9) {
            logger.debug("bot #{} could not re-select the surviving tab {}: {}", new Object[]{var1, var4, var9.getMessage()});
         }

         if (var5 > 0) {
            logger.info("bot #{} closed {} stale tab(s) on attach — single foreground tab", var1, var5);
         }
      } catch (Exception var11) {
         logger.debug("bot #{} tab-collapse skipped: {}", var1, var11.getMessage());
      }
   }

   private static String pageTargetIdFromWsUrl(String var0) {
      if (var0 == null) {
         return null;
      } else {
         int var1 = var0.indexOf("/devtools/page/");
         if (var1 < 0) {
            return null;
         } else {
            String var2 = var0.substring(var1 + "/devtools/page/".length());
            return var2.isBlank() ? null : var2;
         }
      }
   }

   private static void log(Consumer<String> var0, String var1) {
      logger.info(var1);
      if (var0 != null) {
         var0.accept(var1);
      }
   }

   private static void waitForBrowserReady(Driver var0, int var1) {
      byte var2 = 10;
      byte var3 = 100;

      for (int var4 = 0; var4 < var2; var4++) {
         try {
            Object var5 = var0.script("1 + 1");
            if (Integer.valueOf(2).equals(var5)) {
               logger.debug("bot #{} CDP ready after {} attempts", var1, var4 + 1);
               Thread.sleep(100L);
               return;
            }
         } catch (Exception var7) {
            logger.debug("bot #{} browser not ready (attempt {}): {}", new Object[]{var1, var4 + 1, var7.getMessage()});
         }

         try {
            Thread.sleep(var3);
         } catch (InterruptedException var6) {
            Thread.currentThread().interrupt();
            return;
         }
      }

      logger.warn("bot #{} browser readiness check timed out after {}ms", var1, var2 * var3);
   }

   public static byte[] screenshot() {
      for (Bot var1 : AGENTS.values()) {
         if (!var1.isStopped()) {
            return var1.screenshotBytes();
         }
      }

      return null;
   }

   public void stopOwned() {
      synchronized (this.lifecycle) {
         this.closed = true;

         for (Bot var3 : this.owned) {
            stop(var3);
            AGENTS.remove(var3.getId());
         }

         this.owned.clear();
      }
   }

   private static void stop(Bot var0) {
      if (!var0.isStopped()) {
         try {
            var0.stopInternal();
         } catch (Exception var2) {
            logger.warn("Error stopping agent #{}: {}", var0.getId(), var2.getMessage());
         }
      }
   }

   public static void stopAll() {
      logger.info("Stopping all agents ({} active)", AGENTS.size());

      for (Bot var1 : AGENTS.values()) {
         if (!var1.isStopped()) {
            try {
               var1.stopInternal();
            } catch (Exception var3) {
               logger.warn("Error stopping agent #{}: {}", var1.getId(), var3.getMessage());
            }
         }
      }

      AGENTS.clear();
   }

   void register(Bot var1) {
      synchronized (this.lifecycle) {
         if (this.closed) {
            stop(var1);
         } else {
            this.owned.removeIf(Bot::isStopped);
            AGENTS.put(var1.getId(), var1);
            this.owned.add(var1);
         }
      }
   }

   static void unregister(int var0) {
      AGENTS.remove(var0);
   }

   public static int getActiveCount() {
      return (int)AGENTS.values().stream().filter(var0 -> !var0.isStopped()).count();
   }

   public static int getLastId() {
      return INSTANCE_SEQ.get();
   }

   public static void stopAgent(int var0) {
      Bot var1 = AGENTS.get(var0);
      if (var1 != null && !var1.isStopped()) {
         var1.stopInternal();
         AGENTS.remove(var0);
      }
   }

   public static void cleanupOrphans(Map<String, Object> var0) {
      if (!AGENTS.isEmpty()) {
         HashSet var1 = new HashSet();
         collectAgents(var0.values(), var1);

         for (Bot var3 : new ArrayList<>(AGENTS.values())) {
            if (!var3.isStopped() && !var1.contains(var3)) {
               logger.info("Cleaning up orphaned agent #{}", var3.getId());
               var3.stopInternal();
               AGENTS.remove(var3.getId());
            }
         }
      }
   }

   private static void collectAgents(Collection<?> var0, Set<Bot> var1) {
      for (Object var3 : var0) {
         if (var3 instanceof Bot var4) {
            var1.add(var4);
         } else if (var3 instanceof Map var5) {
            collectAgents(var5.values(), var1);
         } else if (var3 instanceof Collection var6) {
            collectAgents(var6, var1);
         }
      }
   }

   private static void createChromePreferences(String var0) {
      try {
         Path var1 = Path.of(var0, "Default");
         Files.createDirectories(var1);
         String var2 = "{\n    \"profile\": {\n        \"password_manager_leak_detection\": false\n    },\n    \"credentials_enable_service\": false,\n    \"credentials_enable_autosignin\": false\n}\n";
         Files.writeString(var1.resolve("Preferences"), var2);
      } catch (IOException var3) {
         logger.warn("Failed to create Chrome preferences: {}", var3.getMessage());
      }
   }

   private void log(String var1) {
      log(this.logCallback, var1);
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (!AGENTS.isEmpty()) {
            logger.info("Shutdown hook: closing {} browser(s)", AGENTS.size());
            stopAll();
         }
      }, "agent-shutdown-hook"));
   }

   public interface StepListener {
      void step(String var1, String var2, String var3);
   }
}
