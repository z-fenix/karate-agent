package io.karatelabs.output;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.js.JavaCallable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;

public class LogMask {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final Set<String> KNOWN_KEYS = Set.of("defaults", "headers", "jsonPaths", "patterns", "replacement", "enableForUri");
   private static final String DEFAULT_REPLACEMENT = "***";
   public static final List<String> DEFAULT_HEADERS = List.of("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization");
   public static final LogMask DEFAULT = new LogMask(Set.of(), withDefaults(Set.of()), List.of(), List.of(), "***", null);
   private final Set<String> maskedHeadersLower;
   private final Set<String> reportHeadersLower;
   private final List<String> jsonPaths;
   private final List<LogMask.PatternRule> patternRules;
   private final String replacement;
   private final JavaCallable enableForUri;

   private LogMask(
      Set<String> maskedHeadersLower,
      Set<String> reportHeadersLower,
      List<String> jsonPaths,
      List<LogMask.PatternRule> patternRules,
      String replacement,
      JavaCallable enableForUri
   ) {
      this.maskedHeadersLower = maskedHeadersLower;
      this.reportHeadersLower = reportHeadersLower;
      this.jsonPaths = jsonPaths;
      this.patternRules = patternRules;
      this.replacement = replacement;
      this.enableForUri = enableForUri;
   }

   private static Set<String> withDefaults(Set<String> headersLower) {
      Set<String> all = new LinkedHashSet<>(headersLower);
      DEFAULT_HEADERS.forEach(h -> all.add(h.toLowerCase()));
      return all;
   }

   public static LogMask fromMap(Map<String, Object> map) {
      if (map != null && !map.isEmpty()) {
         for (String key : map.keySet()) {
            if (!KNOWN_KEYS.contains(key)) {
               logger.warn("configure logging.mask: unknown key '{}' (known: {})", key, KNOWN_KEYS);
            }
         }

         String replacement = map.get("replacement") instanceof String s ? s : "***";
         Set<String> headers = new LinkedHashSet<>();
         List<LogMask.PatternRule> patternRules = (List<LogMask.PatternRule>)map.get("headers");
         if (patternRules instanceof List) {
            for (Object item : patternRules) {
               if (item != null) {
                  headers.add(item.toString().toLowerCase());
               }
            }
         }

         List<String> jsonPaths = new ArrayList<>();
         JavaCallable enableForUri = (JavaCallable)map.get("jsonPaths");
         if (enableForUri instanceof List) {
            for (Object itemx : (List)enableForUri) {
               if (itemx != null) {
                  jsonPaths.add(itemx.toString());
               }
            }
         }

         patternRules = new ArrayList<>();
         if (map.get("patterns") instanceof List<?> list) {
            int idx = 0;

            for (Object itemxx : list) {
               if (itemxx instanceof Map<?, ?> pm) {
                  Object regex = pm.get("regex");
                  if (regex == null) {
                     logger.warn("configure logging.mask.patterns[{}] missing 'regex' — skipped", idx);
                     idx++;
                     continue;
                  }

                  Object rep = pm.get("replacement");
                  String repStr = rep == null ? replacement : rep.toString();

                  try {
                     patternRules.add(new LogMask.PatternRule(Pattern.compile(regex.toString()), repStr));
                  } catch (PatternSyntaxException var14) {
                     logger.warn("configure logging.mask.patterns[{}] invalid regex '{}': {} — skipped", new Object[]{idx, regex, var14.getDescription()});
                  }
               }

               idx++;
            }
         }

         enableForUri = map.get("enableForUri") instanceof JavaCallable c ? c : null;
         boolean defaults = !Boolean.FALSE.equals(map.get("defaults"));
         if (!defaults && headers.isEmpty() && jsonPaths.isEmpty() && patternRules.isEmpty()) {
            logger.warn("configure logging.mask: no usable rules — set at least one of headers / jsonPaths / patterns. mask is OFF.");
            return null;
         } else {
            return new LogMask(headers, defaults ? withDefaults(headers) : headers, jsonPaths, patternRules, replacement, enableForUri);
         }
      } else {
         return DEFAULT;
      }
   }

   public String describe() {
      int h = this.maskedHeadersLower.size();
      int r = this.reportHeadersLower.size() - h;
      int j = this.jsonPaths.size();
      int p = this.patternRules.size();
      return h
         + (h == 1 ? " header" : " headers")
         + (r == 0 ? ", " : " (+" + r + " in report), ")
         + j
         + (j == 1 ? " jsonPath, " : " jsonPaths, ")
         + p
         + (p == 1 ? " pattern" : " patterns");
   }

   public boolean enabledForUri(String uri) {
      if (this.enableForUri == null) {
         return true;
      } else {
         try {
            Object result = this.enableForUri.call(null, uri);
            if (result instanceof Boolean b) {
               return b;
            } else if (result == null) {
               return false;
            } else if (result instanceof String s) {
               return !s.isEmpty();
            } else {
               return result instanceof Number n ? n.doubleValue() != 0.0 : true;
            }
         } catch (Exception var4) {
            return true;
         }
      }
   }

   public boolean isHeaderMasked(String headerName) {
      return headerName != null && this.reportHeadersLower.contains(headerName.toLowerCase());
   }

   public String maskHeader(String name, String value) {
      return this.isHeaderMasked(name) ? this.replacement : this.applyPatterns(value);
   }

   public String maskConsoleHeader(String name, String value) {
      return name != null && this.maskedHeadersLower.contains(name.toLowerCase()) ? this.replacement : this.applyPatterns(value);
   }

   public boolean hasReportOnlyHeaders() {
      return this.reportHeadersLower.size() > this.maskedHeadersLower.size();
   }

   public String maskBody(String body) {
      if (body != null && !body.isEmpty()) {
         String result = body;
         if (!this.jsonPaths.isEmpty() && looksLikeJson(body)) {
            try {
               Object parsed = Json.parseLenient(result);
               boolean changed = false;

               for (String path : this.jsonPaths) {
                  changed |= applyJsonPath(parsed, path, this.replacement);
               }

               if (changed) {
                  result = StringUtils.formatJson(parsed);
               }
            } catch (Exception var7) {
            }
         }

         return this.applyPatterns(result);
      } else {
         return body;
      }
   }

   private String applyPatterns(String text) {
      if (text != null && !this.patternRules.isEmpty()) {
         String result = text;

         for (LogMask.PatternRule rule : this.patternRules) {
            result = rule.pattern.matcher(result).replaceAll(rule.replacement);
         }

         return result;
      } else {
         return text;
      }
   }

   private static boolean applyJsonPath(Object root, String path, String replacement) {
      if (path != null && path.length() >= 2 && path.charAt(0) == '$') {
         String rest = path.substring(1);
         if (rest.startsWith("..")) {
            String key = rest.substring(2);
            return replaceKeyRecursive(root, key, replacement);
         } else {
            return rest.startsWith(".") ? descendAndReplace(root, rest.substring(1), replacement) : false;
         }
      } else {
         return false;
      }
   }

   private static boolean descendAndReplace(Object node, String dottedKeys, String replacement) {
      if (!dottedKeys.isEmpty() && node != null) {
         int dot = dottedKeys.indexOf(46);
         String head = dot == -1 ? dottedKeys : dottedKeys.substring(0, dot);
         String tail = dot == -1 ? "" : dottedKeys.substring(dot + 1);
         if (node instanceof Map<?, ?> map) {
            if (!map.containsKey(head)) {
               return false;
            } else if (tail.isEmpty()) {
               Map<String, Object> mutable = (Map<String, Object>) map;
               mutable.put(head, replacement);
               return true;
            } else {
               return descendAndReplace(map.get(head), tail, replacement);
            }
         } else if (!(node instanceof List<?> list)) {
            return false;
         } else {
            boolean changed = false;

            for (Object item : list) {
               changed |= descendAndReplace(item, dottedKeys, replacement);
            }

            return changed;
         }
      } else {
         return false;
      }
   }

   private static boolean replaceKeyRecursive(Object node, String key, String replacement) {
      if (node instanceof Map<?, ?> map) {
         Map<String, Object> mutable = (Map<String, Object>)map;
         boolean changed = false;

         for (Object keyObj : new HashSet<>(map.keySet())) {
            String k = (String) keyObj;
            if (k.equals(key)) {
               mutable.put(k, replacement);
               changed = true;
            } else {
               changed |= replaceKeyRecursive(mutable.get(k), key, replacement);
            }
         }

         return changed;
      } else if (!(node instanceof List<?> list)) {
         return false;
      } else {
         boolean changed = false;

         for (Object item : list) {
            changed |= replaceKeyRecursive(item, key, replacement);
         }

         return changed;
      }
   }

   private static boolean looksLikeJson(String text) {
      if (text == null) {
         return false;
      } else {
         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
               return c == '{' || c == '[';
            }
         }

         return false;
      }
   }

   private static final class PatternRule {
      final Pattern pattern;
      final String replacement;

      PatternRule(Pattern pattern, String replacement) {
         this.pattern = pattern;
         this.replacement = replacement;
      }
   }
}
