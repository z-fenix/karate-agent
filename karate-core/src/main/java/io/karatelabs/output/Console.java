package io.karatelabs.output;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public final class Console {
   private static final Logger logger = LogContext.CONSOLE_LOGGER;
   private static final Pattern ANSI_PATTERN = Pattern.compile("\u001b\\[[;\\d]*m");
   public static final char BODY_OPEN = '\u0000';
   public static final char BODY_LANG_END = '\u0001';
   public static final char BODY_CLOSE = '\u0002';
   private static final Pattern SENTINEL_PATTERN = Pattern.compile("\u0000[^\u0001]*\u0001|\u0002");
   public static final String RESET = "\u001b[0m";
   public static final String BOLD = "\u001b[1m";
   public static final String RED = "\u001b[31m";
   public static final String GREEN = "\u001b[32m";
   public static final String YELLOW = "\u001b[33m";
   public static final String BLUE = "\u001b[34m";
   public static final String MAGENTA = "\u001b[35m";
   public static final String CYAN = "\u001b[36m";
   public static final String WHITE = "\u001b[37m";
   public static final String BRIGHT_RED = "\u001b[91m";
   public static final String BRIGHT_GREEN = "\u001b[92m";
   public static final String BRIGHT_YELLOW = "\u001b[93m";
   public static final String BRIGHT_CYAN = "\u001b[96m";
   public static final String DIM = "\u001b[2m";
   public static final String GREY = "\u001b[90m";
   private static boolean colorsEnabled = detectColorSupport();
   private static PrintStream out = System.out;

   private Console() {
   }

   public static String stripAnsi(String text) {
      if (text == null) {
         return null;
      } else {
         String noAnsi = ANSI_PATTERN.matcher(text).replaceAll("");
         return SENTINEL_PATTERN.matcher(noAnsi).replaceAll("");
      }
   }

   public static String stripSentinels(String text) {
      return text == null ? null : SENTINEL_PATTERN.matcher(text).replaceAll("");
   }

   public static List<Map<String, String>> splitLog(String text) {
      List<Map<String, String>> segments = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         int i = 0;
         int len = text.length();

         while (i < len) {
            int open = text.indexOf(0, i);
            if (open < 0) {
               addTextSegment(segments, text.substring(i));
               break;
            }

            int langEnd = text.indexOf(1, open + 1);
            int close = langEnd < 0 ? -1 : text.indexOf(2, langEnd + 1);
            if (langEnd < 0 || close < 0) {
               addTextSegment(segments, text.substring(i));
               break;
            }

            addTextSegment(segments, text.substring(i, open));
            Map<String, String> seg = new LinkedHashMap<>();
            seg.put("lang", text.substring(open + 1, langEnd));
            seg.put("code", ANSI_PATTERN.matcher(text.substring(langEnd + 1, close)).replaceAll(""));
            segments.add(seg);
            i = close + 1;
         }

         return segments;
      } else {
         return segments;
      }
   }

   private static void addTextSegment(List<Map<String, String>> segments, String raw) {
      if (!raw.isEmpty()) {
         String stripped = ANSI_PATTERN.matcher(raw).replaceAll("");
         if (!stripped.isEmpty()) {
            Map<String, String> seg = new LinkedHashMap<>();
            seg.put("text", stripped);
            segments.add(seg);
         }
      }
   }

   private static boolean detectColorSupport() {
      String term = System.getenv("TERM");
      String colorterm = System.getenv("COLORTERM");
      String forceColor = System.getenv("FORCE_COLOR");
      String noColor = System.getenv("NO_COLOR");
      if (noColor != null) {
         return false;
      } else if (forceColor != null && !forceColor.equals("0")) {
         return true;
      } else if (term == null || !term.contains("color") && !term.contains("xterm") && !term.contains("256")) {
         if (colorterm != null) {
            return true;
         } else {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
               String wtSession = System.getenv("WT_SESSION");
               return wtSession != null;
            } else {
               return System.console() != null;
            }
         }
      } else {
         return true;
      }
   }

   public static void setColorsEnabled(boolean enabled) {
      colorsEnabled = enabled;
   }

   public static boolean isColorsEnabled() {
      return colorsEnabled;
   }

   public static void setOutput(PrintStream output) {
      out = output;
   }

   public static String color(String text, String... codes) {
      if (colorsEnabled && codes.length != 0) {
         StringBuilder sb = new StringBuilder();

         for (String code : codes) {
            sb.append(code);
         }

         sb.append(text);
         sb.append("\u001b[0m");
         return sb.toString();
      } else {
         return text;
      }
   }

   public static String bold(String text) {
      return color(text, "\u001b[1m");
   }

   public static String red(String text) {
      return color(text, "\u001b[31m");
   }

   public static String green(String text) {
      return color(text, "\u001b[32m");
   }

   public static String yellow(String text) {
      return color(text, "\u001b[33m");
   }

   public static String cyan(String text) {
      return color(text, "\u001b[36m");
   }

   public static String brightRed(String text) {
      return color(text, "\u001b[91m");
   }

   public static String brightGreen(String text) {
      return color(text, "\u001b[92m");
   }

   public static String brightYellow(String text) {
      return color(text, "\u001b[93m");
   }

   public static String grey(String text) {
      return color(text, "\u001b[90m");
   }

   public static String dim(String text) {
      return color(text, "\u001b[2m");
   }

   public static String pass(String text) {
      return color(text, "\u001b[92m");
   }

   public static String fail(String text) {
      return color(text, "\u001b[91m", "\u001b[1m");
   }

   public static String warn(String text) {
      return color(text, "\u001b[93m");
   }

   public static String info(String text) {
      return color(text, "\u001b[36m");
   }

   public static String label(String text) {
      return color(text, "\u001b[37m", "\u001b[1m");
   }

   public static void println(String text) {
      out.println(text);
      if (logger.isTraceEnabled()) {
         logger.trace(stripAnsi(text));
      }
   }

   public static void println() {
      out.println();
   }

   public static void print(String text) {
      out.print(text);
      if (logger.isTraceEnabled()) {
         logger.trace(stripAnsi(text));
      }
   }

   public static String line(int length) {
      return "=".repeat(length);
   }

   public static String line() {
      return line(60);
   }
}
