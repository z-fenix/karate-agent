package io.karatelabs.ext.robot;

import io.karatelabs.driver.Dialog;
import io.karatelabs.driver.Driver;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(Bot.class);
   private final int id;
   private final Driver driver;
   private final BotDriver agent;
   private final Consumer<String> logCallback;
   private boolean attached;
   private boolean stopped = false;
   private int borrows = 0;
   private Runnable onDestroy;
   private Runnable onBeforeClose;

   public void borrow() {
      this.borrows++;
   }

   void setOnDestroy(Runnable var1) {
      this.onDestroy = var1;
   }

   void setOnBeforeClose(Runnable var1) {
      this.onBeforeClose = var1;
   }

   void setAttached(boolean var1) {
      this.attached = var1;
   }

   public BotDriver getAgentDriver() {
      return this.agent;
   }

   Bot(int var1, Driver var2, Consumer<String> var3, boolean var4) {
      this.id = var1;
      this.driver = var2;
      this.agent = new BotDriver(var2);
      this.logCallback = var3;
   }

   @Override
   public Object jsGet(String var1) {
      if ("destroy".equals(var1)) {
         return (io.karatelabs.js.JavaInvokable) this::destroy;
      } else if ("status".equals(var1)) {
         return (JavaInvokable)var1x -> this.getStatus();
      } else {
         return !this.isAlive() && !"destroy".equals(var1) ? (JavaInvokable)var0 -> {
            LinkedHashMap<java.lang.String, java.lang.String> var1x = new LinkedHashMap();
            var1x.put("error", "browser not running");
            var1x.put("code", "CONNECTION_LOST");
            var1x.put("hint", "Call Robot.create() to start a new browser in the same run — no new container needed.");
            return var1x;
         } : this.agent.jsGet(var1);
      }
   }

   private Map<String, String> getStatus() {
      if (!this.isAlive()) {
         throw new RuntimeException("browser dead");
      } else {
         return Map.of("url", this.getUrl());
      }
   }

   public Object evalDirect(String var1) {
      if (!this.stopped && this.isAlive()) {
         return this.driver.script(var1);
      } else {
         LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
         var2.put("error", "browser dead, call Robot.create()");
         var2.put("code", "CONNECTION_LOST");
         return var2;
      }
   }

   public boolean isAlive() {
      if (this.stopped) {
         return false;
      } else {
         try {
            if (this.driver.getDialog() != null) {
               return true;
            } else {
               this.driver.script("1");
               return true;
            }
         } catch (Exception var2) {
            logger.debug("Browser connection check failed: {}", var2.getMessage());
            return false;
         }
      }
   }

   @Override
   public Collection<String> jsKeys() {
      var var1 = new ArrayList<>(BotDriver.VERBS);
      var1.add("destroy");
      var1.add("status");
      return var1;
   }

   @Override
   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("kind", "bot");
      var1.put("id", this.id);
      boolean var2 = this.isAlive();
      var1.put("alive", var2);
      if (var2) {
         try {
            var1.put("url", this.getUrl());
         } catch (RuntimeException var4) {
         }
      }

      return var1;
   }

   private Map<String, Object> destroy(Object... var1) {
      if (this.borrows > 0) {
         this.borrows--;
         return Map.of("destroyed", false, "borrowed", true, "message", "released a borrowed bot handle; the owner still holds the browser and run");
      } else if (this.stopped) {
         return Map.of("destroyed", true, "message", "already destroyed");
      } else {
         this.stopInternal();
         RobotFactory.unregister(this.id);
         this.fireOnDestroy();
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var2.put("destroyed", true);
         var2.put(
            "hint",
            "Browser closed and the live run (if any) is settled — the run's vars are cleared with it, so anything you declared before this call is gone. Call Robot.create() for a new browser + run."
         );
         return var2;
      }
   }

   public void reap() {
      if (!this.stopped) {
         this.borrows = 0;
         this.stopInternal();
         RobotFactory.unregister(this.id);
         this.fireOnDestroy();
      }
   }

   private void fireOnDestroy() {
      if (this.onDestroy != null) {
         Runnable var1 = this.onDestroy;
         this.onDestroy = null;
         var1.run();
      }
   }

   void stopInternal() {
      if (!this.stopped) {
         this.log("Stopping agent #" + this.id);
         Runnable var1 = this.onBeforeClose;
         if (var1 != null) {
            this.onBeforeClose = null;

            try {
               var1.run();
            } catch (Exception var9) {
               logger.warn("before-close hook failed for bot #{}: {}", this.id, var9.getMessage());
            }
         }

         try {
            try {
               Dialog var2 = this.driver.getDialog();
               if (var2 != null) {
                  logger.debug("Dismissing blocking dialog before shutdown");
                  var2.accept(null);
               }
            } catch (Exception var7) {
               logger.debug("Could not dismiss dialog: {}", var7.getMessage());
            }

            Thread var10 = new Thread(() -> {
               try {
                  this.driver.script("window.onbeforeunload = null");
                  this.driver.script("window.removeEventListener('beforeunload', function(){}, true)");
               } catch (Exception var2x) {
                  logger.debug("Could not clear beforeunload handlers: {}", var2x.getMessage());
               }
            });
            var10.start();

            try {
               var10.join(2000L);
               if (var10.isAlive()) {
                  logger.debug("Cleanup script timed out, proceeding with shutdown");
                  var10.interrupt();
               }
            } catch (InterruptedException var6) {
               logger.debug("Cleanup interrupted");
            }

            if (this.attached) {
               try {
                  this.driver.clearCookies();

                  try {
                     this.driver.script("localStorage.clear(); sessionStorage.clear()");
                  } catch (Exception var4) {
                     logger.debug("storage clear at park skipped for bot #{}: {}", this.id, var4.getMessage());
                  }

                  this.driver.navigate("about:blank");
                  this.log("bot #" + this.id + " parked the attached browser (cookies + storage cleared, about:blank)");
               } catch (Exception var5) {
                  logger.debug("could not park attached browser for bot #{}: {}", this.id, var5.getMessage());
               }
            }

            this.driver.quit();
         } catch (Exception var8) {
            logger.warn("Error closing browser for agent #{}: {}", this.id, var8.getMessage());
         }

         this.stopped = true;
         this.log("bot #" + this.id + " stopped");
      }
   }

   public int getId() {
      return this.id;
   }

   byte[] screenshotBytes() {
      if (this.stopped) {
         return null;
      } else {
         try {
            return this.driver.screenshot();
         } catch (Exception var2) {
            logger.debug("Screenshot failed for agent #{}: {}", this.id, var2.getMessage());
            return null;
         }
      }
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public String getUrl() {
      if (this.stopped) {
         return null;
      } else {
         try {
            return this.driver.getUrl();
         } catch (Exception var2) {
            return null;
         }
      }
   }

   public void go(String var1) {
      if (this.stopped) {
         throw new RuntimeException("bot is stopped");
      } else {
         this.driver.setUrl(var1);
      }
   }

   public void stop() {
      if (!this.stopped) {
         this.stopInternal();
         RobotFactory.unregister(this.id);
         this.fireOnDestroy();
      }
   }

   public Map<String, Object> getInfo() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", this.id);
      var1.put("stopped", this.stopped);
      if (!this.stopped) {
         String var2 = this.getUrl();
         if (var2 != null) {
            var1.put("url", var2);
         }
      }

      return var1;
   }

   private void log(String var1) {
      logger.info(var1);
      if (this.logCallback != null) {
         this.logCallback.accept(var1);
      }
   }
}
