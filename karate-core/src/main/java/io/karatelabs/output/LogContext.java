package io.karatelabs.output;

import io.karatelabs.core.StepResult;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogContext {
   private static final ThreadLocal<LogContext> CURRENT = new ThreadLocal<>();
   public static final Logger RUNTIME_LOGGER = LoggerFactory.getLogger("karate.runtime");
   public static final Logger HTTP_LOGGER = LoggerFactory.getLogger("karate.http");
   public static final Logger MOCK_LOGGER = LoggerFactory.getLogger("karate.mock");
   public static final Logger SERVER_LOGGER = LoggerFactory.getLogger("karate.server");
   public static final Logger SCENARIO_LOGGER = LoggerFactory.getLogger("karate.scenario");
   public static final Logger CONSOLE_LOGGER = LoggerFactory.getLogger("karate.console");
   private static LogLevel threshold = LogLevel.INFO;
   private static final String[] RUNTIME_LOGGERS = new String[]{
      "karate", "karate.runtime", "karate.http", "karate.mock", "karate.server", "karate.scenario", "karate.console", "io.karatelabs"
   };
   private final StringBuilder buffer = new StringBuilder();
   private List<StepResult.Embed> embeds;
   private List<StepResult> pendingSteps;
   private LogMask mask = LogMask.DEFAULT;
   private boolean pretty = true;
   private boolean capture = true;
   private static final ThreadLocal<LogContext.Snapshot> PENDING = new ThreadLocal<>();

   public static LogContext get() {
      LogContext ctx = CURRENT.get();
      if (ctx == null) {
         ctx = new LogContext();
         CURRENT.set(ctx);
      }

      return ctx;
   }

   public static LogContext peekCurrent() {
      return CURRENT.get();
   }

   public static void set(LogContext ctx) {
      CURRENT.set(ctx);
   }

   public static void clear() {
      CURRENT.remove();
   }

   public static LogContext.LogWriter with(Logger logger) {
      return new LogContext.LogWriter(logger);
   }

   public static void setLogLevel(LogLevel level) {
      threshold = level;
   }

   public static LogLevel getLogLevel() {
      return threshold;
   }

   public static boolean setRuntimeLogLevel(String level) {
      if (level != null && !level.isEmpty()) {
         String normalized = level.equalsIgnoreCase("none") ? "off" : level;

         try {
            Object ctx = logbackContext();
            if (ctx == null) {
               RUNTIME_LOGGER.debug("Runtime log level not supported: not using Logback");
               return false;
            } else {
               Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
               Object levelValue = levelClass.getMethod("toLevel", String.class).invoke(null, normalized.toUpperCase());
               capturePendingLevels();

               for (String name : RUNTIME_LOGGERS) {
                  setLevelOn(ctx, levelClass, name, levelValue);
               }

               RUNTIME_LOGGER.debug("Set runtime log level to: {}", level);
               return true;
            }
         } catch (Exception var9) {
            RUNTIME_LOGGER.debug("Failed to set runtime log level: {}", var9.getMessage());
            return false;
         }
      } else {
         return false;
      }
   }

   private static Object logbackContext() {
      Object factory = LoggerFactory.getILoggerFactory();
      return !factory.getClass().getName().equals("ch.qos.logback.classic.LoggerContext") ? null : factory;
   }

   private static void setLevelOn(Object ctx, Class<?> levelClass, String name, Object levelValue) throws Exception {
      Object logger = ctx.getClass().getMethod("getLogger", String.class).invoke(ctx, name);
      logger.getClass().getMethod("setLevel", levelClass).invoke(logger, levelValue);
   }

   private static Map<String, String> captureRuntimeLevels() {
      try {
         Object ctx = logbackContext();
         if (ctx == null) {
            return null;
         } else {
            Map<String, String> levels = new LinkedHashMap<>();

            for (String name : RUNTIME_LOGGERS) {
               Object logger = ctx.getClass().getMethod("getLogger", String.class).invoke(ctx, name);
               Object level = logger.getClass().getMethod("getLevel").invoke(logger);
               levels.put(name, level == null ? null : level.toString().toLowerCase());
            }

            return levels;
         }
      } catch (Exception var8) {
         return null;
      }
   }

   private static void restoreRuntimeLevels(Map<String, String> levels) {
      try {
         Object ctx = logbackContext();
         if (ctx == null) {
            return;
         }

         Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
         Method toLevel = levelClass.getMethod("toLevel", String.class);

         for (Entry<String, String> e : levels.entrySet()) {
            Object levelValue = e.getValue() == null ? null : toLevel.invoke(null, e.getValue().toUpperCase());
            setLevelOn(ctx, levelClass, e.getKey(), levelValue);
         }
      } catch (Exception var7) {
         RUNTIME_LOGGER.debug("Failed to restore runtime log levels: {}", var7.getMessage());
      }
   }

   private static void capturePendingLevels() {
      LogContext.Snapshot s = PENDING.get();
      if (s != null && !s.captured) {
         for (Map<String, String> levels = captureRuntimeLevels(); s != null && !s.captured; s = s.outer) {
            s.captured = true;
            s.consoleLevels = levels;
         }
      }
   }

   public static LogContext.Snapshot snapshot() {
      LogContext ctx = get();
      LogContext.Snapshot s = new LogContext.Snapshot(getLogLevel(), ctx.mask, ctx.pretty, PENDING.get());
      PENDING.set(s);
      return s;
   }

   public LogMask getMask() {
      return this.mask;
   }

   public void setMask(LogMask mask) {
      this.mask = mask;
   }

   public boolean isPretty() {
      return this.pretty;
   }

   public void setPretty(boolean pretty) {
      this.pretty = pretty;
   }

   public boolean isCapture() {
      return this.capture;
   }

   public void setCapture(boolean capture) {
      this.capture = capture;
   }

   public boolean isCapturing(LogLevel level) {
      return this.capture && level.isEnabled(threshold);
   }

   public void log(LogLevel level, String message) {
      if (this.capture && level.isEnabled(threshold)) {
         this.buffer.append(message).append('\n');
      }
   }

   public void log(LogLevel level, String format, Object... args) {
      if (this.capture && level.isEnabled(threshold)) {
         String message = format(format, args);
         this.buffer.append(message).append('\n');
      }
   }

   public void log(Object message) {
      this.log(LogLevel.INFO, String.valueOf(message));
   }

   public void log(String format, Object... args) {
      this.log(LogLevel.INFO, format, args);
   }

   public void embed(byte[] data, String mimeType, String name) {
      if (this.embeds == null) {
         this.embeds = new ArrayList<>();
      }

      this.embeds.add(new StepResult.Embed(data, mimeType, name));
   }

   public void embed(byte[] data, String mimeType) {
      this.embed(data, mimeType, null);
   }

   public void embed(StepResult.Embed embed) {
      if (this.embeds == null) {
         this.embeds = new ArrayList<>();
      }

      this.embeds.add(embed);
   }

   public List<StepResult.Embed> collectEmbeds() {
      List<StepResult.Embed> result = this.embeds;
      this.embeds = null;
      return result;
   }

   public StepResult step(String text, boolean passed, List<StepResult.Embed> embeds) {
      StepResult sr = StepResult.synthetic(text, passed ? StepResult.Status.PASSED : StepResult.Status.FAILED, System.currentTimeMillis(), null);
      if (embeds != null) {
         for (StepResult.Embed e : embeds) {
            sr.addEmbed(e);
         }
      }

      return this.step(sr);
   }

   public StepResult step(String text, boolean passed) {
      return this.step(text, passed, null);
   }

   public StepResult step(StepResult sr) {
      if (this.pendingSteps == null) {
         this.pendingSteps = new ArrayList<>();
      }

      this.pendingSteps.add(sr);
      return sr;
   }

   public List<StepResult> collectPendingSteps() {
      List<StepResult> result = this.pendingSteps;
      this.pendingSteps = null;
      return result;
   }

   public String collect() {
      String result = this.buffer.toString();
      this.buffer.setLength(0);
      return result;
   }

   public void appendCaptured(String content) {
      if (this.capture && content != null && !content.isEmpty()) {
         this.buffer.append(content);
      }
   }

   public String peek() {
      return this.buffer.toString();
   }

   static String format(String format, Object... args) {
      if (args != null && args.length != 0) {
         StringBuilder sb = new StringBuilder();
         int argIndex = 0;
         int i = 0;

         while (i < format.length()) {
            if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
               if (argIndex < args.length) {
                  sb.append(args[argIndex++]);
               } else {
                  sb.append("{}");
               }

               i += 2;
            } else {
               sb.append(format.charAt(i));
               i++;
            }
         }

         return sb.toString();
      } else {
         return format;
      }
   }

   public static class LogWriter {
      private final Logger logger;

      LogWriter(Logger logger) {
         this.logger = logger;
      }

      public void trace(String message) {
         this.log(LogLevel.TRACE, message);
         this.logger.trace(message);
      }

      public void debug(String message) {
         this.log(LogLevel.DEBUG, message);
         this.logger.debug(message);
      }

      public void info(String message) {
         this.log(LogLevel.INFO, message);
         this.logger.info(message);
      }

      public void warn(String message) {
         this.log(LogLevel.WARN, message);
         this.logger.warn(message);
      }

      public void error(String message) {
         this.log(LogLevel.ERROR, message);
         this.logger.error(message);
      }

      public boolean isDebugEnabled() {
         return this.logger.isDebugEnabled();
      }

      public boolean isTraceEnabled() {
         return this.logger.isTraceEnabled();
      }

      private void log(LogLevel level, String message) {
         LogContext ctx = LogContext.get();
         if (ctx.capture && level.isEnabled(LogContext.threshold)) {
            ctx.buffer.append(message).append('\n');
         }
      }
   }

   public static final class Snapshot {
      private final LogLevel reportLevel;
      private final LogMask mask;
      private final boolean pretty;
      private final LogContext.Snapshot outer;
      private boolean captured;
      private Map<String, String> consoleLevels;

      private Snapshot(LogLevel reportLevel, LogMask mask, boolean pretty, LogContext.Snapshot outer) {
         this.reportLevel = reportLevel;
         this.mask = mask;
         this.pretty = pretty;
         this.outer = outer;
      }

      public void restore() {
         LogContext.setLogLevel(this.reportLevel);
         if (this.consoleLevels != null) {
            LogContext.restoreRuntimeLevels(this.consoleLevels);
         }

         if (LogContext.PENDING.get() == this) {
            if (this.outer == null) {
               LogContext.PENDING.remove();
            } else {
               LogContext.PENDING.set(this.outer);
            }
         }

         LogContext ctx = LogContext.get();
         ctx.setMask(this.mask);
         ctx.setPretty(this.pretty);
      }
   }
}
