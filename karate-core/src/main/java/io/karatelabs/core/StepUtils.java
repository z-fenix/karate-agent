package io.karatelabs.core;

import io.karatelabs.common.Json;
import io.karatelabs.common.Xml;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsValue;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class StepUtils {
   private StepUtils() {
   }

   public static StepUtils.ParsedFeaturePath parseFeaturePath(String rawPath) {
      if (rawPath.startsWith("@")) {
         return new StepUtils.ParsedFeaturePath(null, rawPath, true, null);
      } else {
         int tagPos = rawPath.indexOf(".feature@");
         if (tagPos != -1) {
            String path = rawPath.substring(0, tagPos + 8);
            String tag = "@" + rawPath.substring(tagPos + 9);
            return new StepUtils.ParsedFeaturePath(path, tag, false, null);
         } else {
            int linePos = rawPath.indexOf(".feature:");
            if (linePos != -1) {
               String path = rawPath.substring(0, linePos + 8);
               String remainder = rawPath.substring(linePos + 9);
               if (remainder.matches("\\d+(:\\d+)*")) {
                  Set<Integer> lines = new LinkedHashSet<>();

                  for (String part : remainder.split(":")) {
                     lines.add(Integer.parseInt(part));
                  }

                  return new StepUtils.ParsedFeaturePath(path, null, false, lines);
               }
            }

            return new StepUtils.ParsedFeaturePath(rawPath, null, false, null);
         }
      }
   }

   public static boolean isValidVariableName(String name) {
      return name != null && !name.isEmpty() && name.matches("[a-zA-Z_][a-zA-Z0-9_]*");
   }

   public static int findReadCloseParen(String text) {
      if (!text.startsWith("read(")) {
         return -1;
      } else {
         int parenDepth = 0;
         boolean inSingleQuote = false;
         boolean inDoubleQuote = false;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
               inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
               inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote) {
               if (c == '(') {
                  parenDepth++;
               } else if (c == ')') {
                  if (--parenDepth == 0) {
                     return i;
                  }
               }
            }
         }

         return -1;
      }
   }

   public static StepUtils.CallParts splitCallExpr(String text) {
      int splitAfter;
      if (text.startsWith("read(")) {
         int closeParen = findReadCloseParen(text);
         if (closeParen < 0) {
            return new StepUtils.CallParts(text, null);
         }

         splitAfter = closeParen;
      } else {
         int space = text.indexOf(32);
         if (space < 0) {
            return new StepUtils.CallParts(text, null);
         }

         splitAfter = space - 1;
      }

      String callable = text.substring(0, splitAfter + 1).trim();
      String arg = text.substring(splitAfter + 1).trim();
      return new StepUtils.CallParts(callable, arg.isEmpty() ? null : arg);
   }

   public static boolean isSingleQuotedLiteral(String text) {
      if (text.length() < 2) {
         return false;
      } else {
         char quote = text.charAt(0);
         return quote != '\'' && quote != '"' ? false : text.indexOf(quote, 1) == text.length() - 1;
      }
   }

   public static boolean isLiteralFeatureRead(String text) {
      if (!text.startsWith("read(")) {
         return false;
      } else {
         int openParen = text.indexOf(40);
         if (openParen == -1) {
            return false;
         } else {
            int nextChar = openParen + 1;
            if (nextChar >= text.length()) {
               return false;
            } else {
               char c = text.charAt(nextChar);
               if (c != '\'' && c != '"') {
                  return false;
               } else {
                  int end = text.indexOf(c, nextChar + 1);
                  if (end == -1) {
                     return false;
                  } else {
                     String path = text.substring(nextChar + 1, end);
                     return path.endsWith(".feature");
                  }
               }
            }
         }
      }
   }

   public static Map<String, Object> calleeDelta(Map<String, Object> callerVars, Map<String, Object> calleeVars) {
      return calleeDelta(callerVars, calleeVars, Set.of());
   }

   public static Map<String, Object> calleeDelta(Map<String, Object> callerVars, Map<String, Object> calleeVars, Set<String> assignedNames) {
      Map<String, Object> delta = new LinkedHashMap<>();

      for (Entry<String, Object> entry : calleeVars.entrySet()) {
         String key = entry.getKey();
         Object value = entry.getValue();
         if (!callerVars.containsKey(key) || callerVars.get(key) != value || assignedNames.contains(key)) {
            delta.put(key, value);
         }
      }

      return delta;
   }

   public static Object deepCopy(Object value) {
      return deepCopy(value, null);
   }

   private static Object deepCopy(Object value, StepUtils.Copies seen) {
      if (value == null) {
         return null;
      } else if (value instanceof JavaCallable) {
         return value;
      } else {
         if (value instanceof JsValue jv && (value instanceof Map || value instanceof List)) {
            value = jv.getJavaValue();
            if (value == null) {
               return null;
            }
         }

         if (value instanceof Date date) {
            Object existing = alreadyCopied(seen, date);
            if (existing != null) {
               return existing;
            } else {
               Date copy = new Date(date.getTime());
               if (seen != null) {
                  seen.put(date, copy);
               }

               return copy;
            }
         } else if (value instanceof Node) {
            Object existing = alreadyCopied(seen, value);
            if (existing != null) {
               return existing;
            } else {
               Object copy;
               if (value instanceof Element element && !(value instanceof Document)) {
                  copy = Xml.toNewDocument(element).getDocumentElement();
               } else {
                  copy = ((Node)value).cloneNode(true);
               }

               if (seen != null) {
                  seen.put(value, copy);
               }

               return copy;
            }
         } else if (value.getClass().isArray()) {
            Class<?> componentType = value.getClass().getComponentType();
            if (componentType.isPrimitive()) {
               Object existing = alreadyCopied(seen, value);
               if (existing != null) {
                  return existing;
               } else {
                  int length = Array.getLength(value);
                  Object copyx = Array.newInstance(componentType, length);
                  System.arraycopy(value, 0, copyx, 0, length);
                  if (seen != null) {
                     seen.put(value, copyx);
                  }

                  return copyx;
               }
            } else {
               Object[] arr = (Object[])value;
               Object[] existing = (Object[])alreadyCopied(seen, arr);
               if (existing != null) {
                  return existing;
               } else {
                  Object[] copyx = new Object[arr.length];
                  seen = remember(seen, arr, copyx);
                  boolean typePreserved = componentType != Object.class;

                  for (int i = 0; i < arr.length; i++) {
                     copyx[i] = deepCopy(arr[i], seen);
                     if (copyx[i] != null && !componentType.isInstance(copyx[i])) {
                        typePreserved = false;
                     }
                  }

                  if (typePreserved && !seen.wasShared(arr)) {
                     Object[] typed = (Object[])Array.newInstance(componentType, arr.length);
                     System.arraycopy(copyx, 0, typed, 0, arr.length);
                     seen.put(arr, typed);
                     return typed;
                  } else {
                     return copyx;
                  }
               }
            }
         } else if (value instanceof Map) {
            Object existing = alreadyCopied(seen, value);
            if (existing != null) {
               return existing;
            } else {
               Map<String, Object> copyx = new LinkedHashMap<>();
               seen = remember(seen, value, copyx);

               for (Entry<String, Object> entry : ((Map<String, Object>)value).entrySet()) {
                  copyx.put(entry.getKey(), deepCopy(entry.getValue(), seen));
               }

               return copyx;
            }
         } else if (value instanceof List) {
            Object existing = alreadyCopied(seen, value);
            if (existing != null) {
               return existing;
            } else {
               List<Object> copyx = new ArrayList<>();
               seen = remember(seen, value, copyx);

               for (Object item : (List)value) {
                  copyx.add(deepCopy(item, seen));
               }

               return copyx;
            }
         } else if (!(value instanceof Set)) {
            return value;
         } else {
            Object existing = alreadyCopied(seen, value);
            if (existing != null) {
               return existing;
            } else {
               Set<Object> copyx = new LinkedHashSet<>();
               seen = remember(seen, value, copyx);

               for (Object item : (Set)value) {
                  copyx.add(deepCopy(item, seen));
               }

               return copyx;
            }
         }
      }
   }

   private static Object alreadyCopied(StepUtils.Copies seen, Object value) {
      return seen == null ? null : seen.get(value);
   }

   private static StepUtils.Copies remember(StepUtils.Copies seen, Object value, Object copy) {
      if (seen == null) {
         seen = new StepUtils.Copies();
      }

      seen.put(value, copy);
      return seen;
   }

   public static Object getOrCreatePath(Map<String, Object> target, String path) {
      String[] parts = path.split("\\.");
      Map<String, Object> current = target;

      for (String part : parts) {
         Object next = current.get(part);
         if (next == null) {
            next = new LinkedHashMap();
            current.put(part, next);
         }

         if (!(next instanceof Map)) {
            return next;
         }

         current = (Map<String, Object>) next;
      }

      return current;
   }

   public static void setValueAtPath(Object target, String path, Object value) {
      if (target instanceof Map) {
         Map<String, Object> map = (Map<String, Object>) target;
         if (path.contains("[")) {
            int bracketIdx = path.indexOf(91);
            String key = path.substring(0, bracketIdx);
            int closeIdx = path.indexOf(93);
            int arrayIdx = Integer.parseInt(path.substring(bracketIdx + 1, closeIdx));
            String remainder = closeIdx + 1 < path.length() ? path.substring(closeIdx + 1) : "";
            Object arr = map.get(key);
            if (arr == null) {
               arr = new ArrayList();
               map.put(key, arr);
            }

            if (arr instanceof List) {
               List<Object> list = (List<Object>) arr;
               while (list.size() <= arrayIdx) {
                  list.add(remainder.isEmpty() ? null : new LinkedHashMap());
               }

               if (remainder.isEmpty()) {
                  list.set(arrayIdx, value);
               } else {
                  String nextPath = remainder.startsWith(".") ? remainder.substring(1) : remainder;
                  setValueAtPath(list.get(arrayIdx), nextPath, value);
               }
            }
         } else if (path.contains(".")) {
            int dotIdx = path.indexOf(46);
            String keyx = path.substring(0, dotIdx);
            String remainderx = path.substring(dotIdx + 1);
            Object next = map.get(keyx);
            if (next == null) {
               next = new LinkedHashMap();
               map.put(keyx, next);
            }

            setValueAtPath(next, remainderx, value);
         } else {
            map.put(path, value);
         }
      }
   }

   public static void removeAtPath(Map<String, Object> map, String path) {
      if (path.startsWith("[")) {
         int closeIdx = path.indexOf(93);
         if (closeIdx > 0) {
            String key = path.substring(1, closeIdx);
            if (key.startsWith("'") && key.endsWith("'") || key.startsWith("\"") && key.endsWith("\"")) {
               key = key.substring(1, key.length() - 1);
            }

            String remainder = closeIdx + 1 < path.length() ? path.substring(closeIdx + 1) : "";
            if (remainder.isEmpty()) {
               map.remove(key);
            } else {
               Object nested = map.get(key);
               if (nested instanceof Map) {
                  String nextPath = remainder.startsWith(".") ? remainder.substring(1) : remainder;
                  removeAtPath((Map<String, Object>)nested, nextPath);
               }
            }
         }
      } else if (path.contains(".")) {
         int dotIdx = path.indexOf(46);
         String keyx = path.substring(0, dotIdx);
         String remainder = path.substring(dotIdx + 1);
         Object nested = map.get(keyx);
         if (nested instanceof Map) {
            removeAtPath((Map<String, Object>)nested, remainder);
         }
      } else {
         map.remove(path);
      }
   }

   public static int findAssignmentOperator(String text) {
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '=' && i > 0) {
            char prev = text.charAt(i - 1);
            if (prev != '=' && prev != '!' && prev != '<' && prev != '>' && (i + 1 >= text.length() || text.charAt(i + 1) != '=')) {
               return i;
            }
         }
      }

      return -1;
   }

   public static boolean hasPunctuation(String s) {
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == '.' || c == '(' || c == ')' || c == '[' || c == ']' || c == '\'' || c == '"') {
            return true;
         }
      }

      return false;
   }

   public static String stringify(Object value) {
      if (value == null) {
         return "null";
      } else if (value instanceof String) {
         return (String)value;
      } else if (value instanceof Map || value instanceof List) {
         return Json.stringifyStrict(value);
      } else {
         return value instanceof Node ? Xml.toString((Node)value, false) : value.toString();
      }
   }

   public static boolean containsJsonPathWildcard(String s) {
      return s.contains("[*]") || s.contains("[?") || s.contains("..");
   }

   public static boolean isPlainIdentifier(String s) {
      if (s == null) {
         return false;
      } else {
         s = s.trim();
         int n = s.length();
         if (n == 0) {
            return false;
         } else {
            char c0 = s.charAt(0);
            if (!Character.isLetter(c0) && c0 != '_' && c0 != '$') {
               return false;
            } else {
               for (int i = 1; i < n; i++) {
                  char c = s.charAt(i);
                  if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                     return false;
                  }
               }

               return true;
            }
         }
      }
   }

   public static boolean isPureJsonPath(String lhs) {
      if (lhs == null) {
         return false;
      } else {
         String s = lhs.trim();
         int n = s.length();
         if (n == 0) {
            return false;
         } else {
            char c0 = s.charAt(0);
            if (!Character.isLetter(c0) && c0 != '_' && c0 != '$') {
               return false;
            } else {
               int i;
               for (i = 1; i < n; i++) {
                  char c = s.charAt(i);
                  if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                     break;
                  }
               }

               boolean hasSegment = false;

               while (i < n) {
                  char c = s.charAt(i);
                  if (c != '.') {
                     if (c != '[') {
                        return false;
                     }

                     hasSegment = true;
                     if (++i >= n) {
                        return false;
                     }

                     char inner = s.charAt(i);
                     if (inner != '\'' && inner != '"') {
                        if (inner == '*') {
                           i++;
                        } else if (inner == '?') {
                           int depth = 1;
                           i++;

                           for (; i < n && depth > 0; i++) {
                              char cc = s.charAt(i);
                              if (cc == '[') {
                                 depth++;
                              } else if (cc == ']') {
                                 if (--depth == 0) {
                                    break;
                                 }
                              }
                           }

                           if (i >= n) {
                              return false;
                           }
                        } else {
                           if (inner != '-' && !Character.isDigit(inner)) {
                              return false;
                           }

                           if (inner == '-') {
                              i++;
                           }

                           while (i < n && Character.isDigit(s.charAt(i))) {
                              i++;
                           }

                           if (i < n && s.charAt(i) == ':') {
                              i++;
                              if (i < n && s.charAt(i) == '-') {
                                 i++;
                              }

                              while (i < n && Character.isDigit(s.charAt(i))) {
                                 i++;
                              }
                           }
                        }
                     } else {
                        char quote = inner;
                        i++;

                        for (; i < n && s.charAt(i) != quote; i++) {
                           if (s.charAt(i) == '\\' && i + 1 < n) {
                              i++;
                           }
                        }

                        if (i >= n) {
                           return false;
                        }

                        i++;
                     }

                     if (i >= n || s.charAt(i) != ']') {
                        return false;
                     }

                     i++;
                  } else {
                     hasSegment = true;
                     i++;
                     if (i < n && s.charAt(i) == '.') {
                        if (++i >= n) {
                           return false;
                        }

                        char next = s.charAt(i);
                        if (next == '[') {
                           continue;
                        }

                        if (!Character.isLetter(next) && next != '_' && next != '$') {
                           return false;
                        }
                     }

                     if (i >= n) {
                        return false;
                     }

                     char id = s.charAt(i);
                     if (!Character.isLetter(id) && id != '_' && id != '$') {
                        return false;
                     }

                     i++;

                     while (i < n) {
                        char cc = s.charAt(i);
                        if (!Character.isLetterOrDigit(cc) && cc != '_' && cc != '$' && cc != '-') {
                           break;
                        }

                        i++;
                     }
                  }
               }

               return hasSegment;
            }
         }
      }
   }

   public static StepUtils.VarAndPath splitVarAndJsonPath(String lhs) {
      lhs = lhs.trim();
      int i = 0;
      int n = lhs.length();
      if (n == 0) {
         return new StepUtils.VarAndPath("", "$");
      } else {
         char first = lhs.charAt(0);
         if (!Character.isLetter(first) && first != '_' && first != '$') {
            return new StepUtils.VarAndPath("", "$");
         } else {
            for (i = 1; i < n; i++) {
               char c = lhs.charAt(i);
               if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                  break;
               }
            }

            String var = lhs.substring(0, i);
            String rest = lhs.substring(i).trim();
            String path;
            if (rest.isEmpty()) {
               path = "$";
            } else if (rest.charAt(0) == '$') {
               path = rest;
            } else if (rest.charAt(0) != '.' && rest.charAt(0) != '[') {
               path = "$." + rest;
            } else {
               path = "$" + rest;
            }

            return new StepUtils.VarAndPath(var, path);
         }
      }
   }

   public record CallParts(String callable, String arg) {
   }

   private static final class Copies {
      private final IdentityHashMap<Object, Object> copies = new IdentityHashMap<>();
      private final IdentityHashMap<Object, Boolean> shared = new IdentityHashMap<>();

      Object get(Object original) {
         Object copy = this.copies.get(original);
         if (copy != null) {
            this.shared.put(original, Boolean.TRUE);
         }

         return copy;
      }

      void put(Object original, Object copy) {
         this.copies.put(original, copy);
      }

      boolean wasShared(Object original) {
         return this.shared.containsKey(original);
      }
   }

   public record ParsedFeaturePath(String path, String tagSelector, boolean sameFile, Set<Integer> lineFilters) {
   }

   public static final class VarAndPath {
      public final String var;
      public final String path;

      VarAndPath(String var, String path) {
         this.var = var;
         this.path = path;
      }
   }
}
