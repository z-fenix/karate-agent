package io.karatelabs.common;

import io.karatelabs.js.JsFunction;
import io.karatelabs.js.JsValue;
import io.karatelabs.js.JsonParser;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.Terms;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {
   public static final String EMPTY = "";
   private static final Pattern CLI_ARG = Pattern.compile("'([^']*)'[^\\S]|\"([^\"]*)\"[^\\S]|(\\S+)");
   static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   static final int CHAR_LENGTH = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".length();
   static SecureRandom RANDOM = new SecureRandom();
   private static final Pattern JS_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*$");

   private StringUtils() {
   }

   public static boolean looksLikeJson(String s) {
      if (s != null && !s.isEmpty()) {
         if (s.charAt(0) == ' ') {
            s = s.trim();
            if (s.isEmpty()) {
               return false;
            }
         }

         return s.charAt(0) == '{' || s.charAt(0) == '[';
      } else {
         return false;
      }
   }

   public static boolean isXml(String s) {
      if (s != null && !s.isEmpty()) {
         if (s.charAt(0) == ' ') {
            s = s.trim();
            if (s.isEmpty()) {
               return false;
            }
         }

         return s.charAt(0) == '<';
      } else {
         return false;
      }
   }

   public static String truncate(String s, int length, boolean addDots) {
      if (s == null) {
         return "";
      } else if (s.length() > length) {
         return addDots ? s.substring(0, length) + " ..." : s.substring(0, length);
      } else {
         return s;
      }
   }

   public static String trimToEmpty(String s) {
      return s == null ? "" : s.trim();
   }

   public static String trimToNull(String s) {
      if (s == null) {
         return null;
      } else {
         String temp = trimToEmpty(s);
         return "".equals(temp) ? null : temp;
      }
   }

   public static String repeat(char c, int count) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < count; i++) {
         sb.append(c);
      }

      return sb.toString();
   }

   public static String join(Object[] a, char delimiter) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < a.length; i++) {
         sb.append(a[i]);
         if (i != a.length - 1) {
            sb.append(delimiter);
         }
      }

      return sb.toString();
   }

   public static String join(Collection<String> c, String delimiter) {
      if (c == null) {
         return null;
      } else {
         StringBuilder sb = new StringBuilder();
         Iterator<String> iterator = c.iterator();

         while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
               sb.append(delimiter);
            }
         }

         return sb.toString();
      }
   }

   public static List<String> split(String s, char delimiter, boolean skipBackSlash) {
      int pos = s.indexOf(delimiter);
      if (pos == -1) {
         return Collections.singletonList(s);
      } else {
         List<String> list = new ArrayList<>();
         int startPos = 0;
         int searchPos = 0;

         while (pos != -1) {
            if (skipBackSlash && pos > 0 && s.charAt(pos - 1) == '\\') {
               s = s.substring(0, pos - 1) + s.substring(pos);
               searchPos = pos;
            } else {
               String temp = s.substring(startPos, pos);
               if (!"".equals(temp)) {
                  list.add(temp);
               }

               startPos = pos + 1;
               searchPos = startPos;
            }

            pos = s.indexOf(delimiter, searchPos);
         }

         if (startPos != s.length()) {
            String temp = s.substring(startPos);
            list.add(temp);
         }

         return list;
      }
   }

   public static boolean isBlank(String s) {
      return trimToNull(s) == null;
   }

   public static String toIdString(String name) {
      return name == null ? "" : name.replaceAll("[\\s_\\\\/:<>\"\\|\\?\\*]", "-").toLowerCase();
   }

   public static Pair<String> splitByFirstLineFeed(String text) {
      String left = "";
      String right = "";
      if (text != null) {
         int pos = text.indexOf(10);
         if (pos != -1) {
            left = text.substring(0, pos).trim();
            right = text.substring(pos).trim();
         } else {
            left = text.trim();
         }
      }

      return Pair.of(left, right);
   }

   public static List<String> toStringLines(String text) {
      return new BufferedReader(new StringReader(text)).lines().collect(Collectors.toList());
   }

   public static int countLineFeeds(String text) {
      int count = 0;

      for (char c : text.toCharArray()) {
         if (c == '\n') {
            count++;
         }
      }

      return count;
   }

   public static int wrappedLinesEstimate(String text, int colWidth) {
      List<String> lines = toStringLines(text);
      int estimate = 0;

      for (String s : lines) {
         int wrapEstimate = (int)Math.ceil((double)s.length() / colWidth);
         if (wrapEstimate == 0) {
            estimate++;
         } else {
            estimate += wrapEstimate;
         }
      }

      return estimate;
   }

   public static boolean containsIgnoreCase(List<String> list, String str) {
      for (String i : list) {
         if (i.equalsIgnoreCase(str)) {
            return true;
         }
      }

      return false;
   }

   public static <T> T getIgnoreKeyCase(Map<String, T> map, String name) {
      if (map != null && name != null) {
         for (Entry<String, T> entry : map.entrySet()) {
            String key = entry.getKey();
            if (name.equalsIgnoreCase(key)) {
               return entry.getValue();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void removeIgnoreKeyCase(Map<String, ?> map, String name) {
      if (map != null && name != null) {
         for (String key : map.keySet()) {
            if (name.equalsIgnoreCase(key)) {
               map.remove(key);
               return;
            }
         }
      }
   }

   public static Map<String, Object> simplify(Map<String, List<String>> map, boolean always) {
      if (map == null) {
         return Collections.emptyMap();
      } else {
         Map<String, Object> result = new LinkedHashMap<>(map.size());

         for (Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
               if (values.size() > 1) {
                  if (always) {
                     result.put(key, join(values, ","));
                  } else {
                     result.put(key, values);
                  }
               } else {
                  Object value = values.getFirst();
                  if (value != null) {
                     result.put(key, value + "");
                  }
               }
            }
         }

         return result;
      }
   }

   public static String throwableToString(Throwable t) {
      try {
         String var3;
         try (
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
         ) {
            t.printStackTrace(pw);
            var3 = sw.toString();
         }

         return var3;
      } catch (IOException var9) {
         return null;
      }
   }

   public static String[] tokenizeCliCommand(String command) {
      List<String> args = new ArrayList<>();
      Matcher m = CLI_ARG.matcher(command + " ");

      while (m.find()) {
         if (m.group(1) != null) {
            args.add(m.group(1));
         } else if (m.group(2) != null) {
            args.add(m.group(2));
         } else {
            args.add(m.group(3));
         }
      }

      return args.toArray(new String[0]);
   }

   public static String shellEscape(String value) {
      return OsUtils.isWindows() ? shellEscapeWindows(value) : shellEscapeUnix(value);
   }

   public static String shellEscapeUnix(String value) {
      return value == null ? "''" : "'" + value.replace("'", "'\\''") + "'";
   }

   public static String shellEscapeWindows(String value) {
      if (value == null) {
         return "\"\"";
      } else {
         String escaped = value.replace("\"", "\"\"")
            .replace("^", "^^")
            .replace("&", "^&")
            .replace("|", "^|")
            .replace("<", "^<")
            .replace(">", "^>")
            .replace("%", "%%");
         return "\"" + escaped + "\"";
      }
   }

   public static String shellEscapePowerShell(String value) {
      return value == null ? "''" : "'" + value.replace("'", "''") + "'";
   }

   public static String shellEscapeForPlatform(String value, String platform) {
      if (platform == null) {
         platform = "sh";
      }
      return switch (platform) {
         case "cmd" -> shellEscapeWindows(value);
         case "ps" -> shellEscapePowerShell(value);
         default -> shellEscapeUnix(value);
      };
   }

   public static String getLineContinuation(String platform) {
      if (platform == null) {
         platform = "sh";
      }
      return switch (platform) {
         case "cmd" -> " ^\n";
         case "ps" -> " `\n";
         default -> " \\\n";
      };
   }

   public static String buildShellCommand(String command, String... args) {
      StringBuilder sb = new StringBuilder();
      sb.append(command);

      for (String arg : args) {
         sb.append(' ');
         sb.append(shellEscape(arg));
      }

      return sb.toString();
   }

   public static String buildShellCommand(String command, List<String> args) {
      StringBuilder sb = new StringBuilder();
      sb.append(command);

      for (String arg : args) {
         sb.append(' ');
         sb.append(shellEscape(arg));
      }

      return sb.toString();
   }

   public static String randomAlphaNumeric(int len) {
      StringBuilder sb = new StringBuilder(len);

      for (int i = 0; i < len; i++) {
         sb.append("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(RANDOM.nextInt(CHAR_LENGTH)));
      }

      return sb.toString();
   }

   public static String formatFileSize(int bytes) {
      if (bytes < 1024) {
         return bytes + " bytes";
      } else {
         return bytes < 1048576 ? String.format("%.1f KB", bytes / 1024.0) : String.format("%.1f MB", bytes / 1048576.0);
      }
   }

   public static String formatJson(Object o) {
      return formatJson(o, true, false, false);
   }

   public static String formatJson(Object o, boolean pretty, boolean lenient, boolean sort) {
      return formatJson(o, pretty, lenient, sort, "  ");
   }

   public static String formatJson(Object o, boolean pretty, boolean lenient, boolean sort, String indent) {
      if (o instanceof String ostring) {
         if (!looksLikeJson(ostring)) {
            return ostring;
         }

         o = JsonParser.parse(ostring);
      }

      if (!(o instanceof List) && !(o instanceof Map) && (o == null || !o.getClass().isArray())) {
         return o instanceof Number n ? Terms.numberToString(n) : o + "";
      } else {
         StringBuilder sb = new StringBuilder();
         Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
         formatRecurse(o, pretty, lenient, sort, indent, sb, 0, visited);
         return sb.toString();
      }
   }

   private static void formatRecurse(Object o, boolean pretty, boolean lenient, boolean sort, String indent, StringBuilder sb, int depth, Set<Object> visited) {
      if (o == null) {
         sb.append("null");
      } else if (o instanceof JsFunction) {
         sb.append("null");
      } else if (o instanceof JsValue jv) {
         formatRecurse(jv.getJavaValue(), pretty, lenient, sort, indent, sb, depth, visited);
      } else if (o.getClass().isArray()) {
         if (visited.contains(o)) {
            sb.append("\"[Circular]\"");
            return;
         }

         visited.add(o);

         try {
            int length = Array.getLength(o);
            sb.append('[');
            if (pretty) {
               sb.append('\n');
            }

            for (int i = 0; i < length; i++) {
               if (pretty) {
                  pad(sb, depth + 1, indent);
               }

               formatRecurse(Array.get(o, i), pretty, lenient, sort, indent, sb, depth + 1, visited);
               if (i < length - 1) {
                  sb.append(',');
               }

               if (pretty) {
                  sb.append('\n');
               }
            }

            if (pretty) {
               pad(sb, depth, indent);
            }

            sb.append(']');
         } finally {
            visited.remove(o);
         }
      } else if (o instanceof List<?> list) {
         if (visited.contains(o)) {
            sb.append("\"[Circular]\"");
            return;
         }

         visited.add(o);

         try {
            sb.append('[');
            if (pretty) {
               sb.append('\n');
            }

            Iterator<?> iterator = list.iterator();

            while (iterator.hasNext()) {
               Object child = iterator.next();
               if (pretty) {
                  pad(sb, depth + 1, indent);
               }

               formatRecurse(child, pretty, lenient, sort, indent, sb, depth + 1, visited);
               if (iterator.hasNext()) {
                  sb.append(',');
               }

               if (pretty) {
                  sb.append('\n');
               }
            }

            if (pretty) {
               pad(sb, depth, indent);
            }

            sb.append(']');
         } finally {
            visited.remove(o);
         }
      } else if (o instanceof ObjectLike ol) {
         if (visited.contains(o)) {
            sb.append("\"[Circular]\"");
            return;
         }

         visited.add(o);

         try {
            Map<Object, Object> map = new LinkedHashMap<>(ol.toMap());
            if (sort) {
               map = new TreeMap<>(map);
            }

            List<Entry<Object, Object>> entries = new ArrayList<>();

            for (Entry<Object, Object> entry : map.entrySet()) {
               Object value = entry.getValue();
               if (value != Terms.UNDEFINED && !(value instanceof JsFunction)) {
                  entries.add(entry);
               }
            }

            formatMap(entries, pretty, lenient, sort, indent, sb, depth, visited);
         } finally {
            visited.remove(o);
         }
      } else if (o instanceof Map) {
         if (visited.contains(o)) {
            sb.append("\"[Circular]\"");
            return;
         }

         visited.add(o);

         try {
            Map<Object, Object> map = (Map<Object, Object>)o;
            if (sort) {
               map = new TreeMap<>(map);
            }

            List<Entry<Object, Object>> entries = new ArrayList<>();

            for (Entry<Object, Object> entryx : map.entrySet()) {
               Object value = entryx.getValue();
               if (value != Terms.UNDEFINED && !(value instanceof JsFunction)) {
                  entries.add(entryx);
               }
            }

            formatMap(entries, pretty, lenient, sort, indent, sb, depth, visited);
         } finally {
            visited.remove(o);
         }
      } else if (o instanceof Date date) {
         Instant instant = date.toInstant();
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
         String isoDate = formatter.format(instant);
         sb.append('"').append(isoDate).append('"');
      } else if (o instanceof Number n) {
         sb.append(Terms.numberToString(n));
      } else if (o instanceof Boolean) {
         sb.append(o);
      } else {
         String value = o.toString();
         if (lenient) {
            sb.append('\'').append(escapeJsonValue(value)).append('\'');
         } else {
            sb.append('"').append(escapeJsonValue(value)).append('"');
         }
      }
   }

   private static void formatMap(
      List<Entry<Object, Object>> entries, boolean pretty, boolean lenient, boolean sort, String indent, StringBuilder sb, int depth, Set<Object> visited
   ) {
      sb.append('{');
      if (pretty) {
         sb.append('\n');
      }

      Iterator<Entry<Object, Object>> iterator = entries.iterator();

      while (iterator.hasNext()) {
         Entry<Object, Object> entry = iterator.next();
         Object key = entry.getKey();
         if (pretty) {
            pad(sb, depth + 1, indent);
         }

         if (lenient) {
            String escaped = escapeJsonValue(key == null ? null : key.toString());
            if (escaped != null && isValidJsonKey(escaped)) {
               sb.append(key);
            } else {
               sb.append('\'').append(escaped).append('\'');
            }
         } else {
            sb.append('"').append(escapeJsonValue(key == null ? null : key.toString())).append('"');
         }

         sb.append(':');
         if (pretty) {
            sb.append(' ');
         }

         formatRecurse(entry.getValue(), pretty, lenient, sort, indent, sb, depth + 1, visited);
         if (iterator.hasNext()) {
            sb.append(',');
         }

         if (pretty) {
            sb.append('\n');
         }
      }

      if (pretty) {
         pad(sb, depth, indent);
      }

      sb.append('}');
   }

   private static void pad(StringBuilder sb, int depth, String indent) {
      sb.append(indent.repeat(depth));
   }

   private static boolean isValidJsonKey(String key) {
      return JS_IDENTIFIER_PATTERN.matcher(key).matches();
   }

   public static String formatJsonString(String raw) {
      return "\"" + escapeJsonValue(raw) + "\"";
   }

   private static String escapeJsonValue(String raw) {
      if (raw == null) {
         return null;
      } else {
         int len = raw.length();

         int i;
         for (i = 0; i < len; i++) {
            char c = raw.charAt(i);
            if (c < ' ' || c == '"' || c == '\\' || c >= 127 && c <= 159 || c == 8232 || c == 8233) {
               break;
            }
         }

         if (i == len) {
            return raw;
         } else {
            StringBuilder sb = new StringBuilder(len + 16);
            sb.append(raw, 0, i);

            for (; i < len; i++) {
               char c = raw.charAt(i);
               switch (c) {
                  case '\b':
                     sb.append("\\b");
                     break;
                  case '\t':
                     sb.append("\\t");
                     break;
                  case '\n':
                     sb.append("\\n");
                     break;
                  case '\f':
                     sb.append("\\f");
                     break;
                  case '\r':
                     sb.append("\\r");
                     break;
                  case '"':
                     sb.append("\\\"");
                     break;
                  case '\\':
                     sb.append("\\\\");
                     break;
                  default:
                     if (c >= ' ' && (c < 127 || c > 159) && c != 8232 && c != 8233) {
                        sb.append(c);
                     } else {
                        sb.append(String.format("\\u%04X", Integer.valueOf(c)));
                     }
               }
            }

            return sb.toString();
         }
      }
   }
}
