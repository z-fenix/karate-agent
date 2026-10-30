package io.karatelabs.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Predicate;
import io.karatelabs.common.DataUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.OsUtils;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.output.LogContext;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class KarateJsUtils {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final Configuration jsonPathConfig = Configuration.defaultConfiguration().addOptions(new Option[]{Option.SUPPRESS_EXCEPTIONS});

   private KarateJsUtils() {
   }

   static JavaInvokable append() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("append() needs at least two arguments");
         } else {
            List<Object> result = new ArrayList<>();
            Object first = args[0];
            if (first instanceof List) {
               result.addAll((List)first);
            } else {
               result.add(first);
            }

            for (int i = 1; i < args.length; i++) {
               Object item = args[i];
               if (item instanceof List) {
                  result.addAll((List)item);
               } else {
                  result.add(item);
               }
            }

            return result;
         }
      };
   }

   static JavaInvokable appendTo() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("appendTo() needs at least two arguments: list and item(s)");
         } else {
            List<Object> list = (List<Object>)args[0];

            for (int i = 1; i < args.length; i++) {
               Object item = args[i];
               if (item instanceof List) {
                  list.addAll((List)item);
               } else {
                  list.add(item);
               }
            }

            return list;
         }
      };
   }

   static JavaInvokable distinct() {
      return args -> {
         if (args.length != 0 && args[0] != null) {
            if (args[0] instanceof List<?> list) {
               LinkedHashSet var3 = new LinkedHashSet(list);
               return new ArrayList(var3);
            } else {
               return new ArrayList();
            }
         } else {
            return new ArrayList();
         }
      };
   }

   static JavaInvokable extract() {
      return args -> {
         if (args.length < 3) {
            throw new RuntimeException("extract() needs three arguments: text, regex, group");
         } else {
            String text = args[0].toString();
            String regex = args[1].toString();
            int group = ((Number)args[2]).intValue();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            return !matcher.find() ? null : matcher.group(group);
         }
      };
   }

   static JavaInvokable extractAll() {
      return args -> {
         if (args.length < 3) {
            throw new RuntimeException("extractAll() needs three arguments: text, regex, group");
         } else {
            String text = args[0].toString();
            String regex = args[1].toString();
            int group = ((Number)args[2]).intValue();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            List<String> list = new ArrayList<>();

            while (matcher.find()) {
               list.add(matcher.group(group));
            }

            return list;
         }
      };
   }

   static JavaInvokable filter() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("filter() needs two arguments: list and function");
         } else {
            List<?> list = (List<?>)args[0];
            JavaCallable fn = (JavaCallable)args[1];
            List<Object> result = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               Object keep = fn.call(null, item, i);
               if (Boolean.TRUE.equals(keep)) {
                  result.add(item);
               }
            }

            return result;
         }
      };
   }

   static JavaInvokable filterKeys() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("filterKeys() needs at least two arguments");
         } else {
            Map<String, Object> source = (Map<String, Object>)args[0];
            Map<String, Object> result = new LinkedHashMap<>();
            if (args[1] instanceof Map<?, ?> rawKeysMap) {
               Map<String, Object> keysMap = (Map<String, Object>) rawKeysMap;
               for (String key : keysMap.keySet()) {
                  if (source.containsKey(key)) {
                     result.put(key, source.get(key));
                  }
               }
            } else if (args[1] instanceof List) {
               for (Object keyO : (List<?>)args[1]) {
                  String keyx = (String) keyO;
                  if (source.containsKey(keyx)) {
                     result.put(keyx, source.get(keyx));
                  }
               }
            } else {
               for (int i = 1; i < args.length; i++) {
                  String keyxx = args[i].toString();
                  if (source.containsKey(keyxx)) {
                     result.put(keyxx, source.get(keyxx));
                  }
               }
            }

            return result;
         }
      };
   }

   static JavaInvokable forEach() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("forEach() needs two arguments: collection and function");
         } else {
            Object collection = args[0];
            JavaCallable fn = (JavaCallable)args[1];
            if (collection instanceof List<?> list) {
               for (int i = 0; i < list.size(); i++) {
                  fn.call(null, list.get(i), i);
               }
            } else if (collection instanceof Map<?, ?> map) {
               int i = 0;

               for (Entry<?, ?> entry : map.entrySet()) {
                  fn.call(null, entry.getKey(), entry.getValue(), i++);
               }
            }

            return null;
         }
      };
   }

   static JavaInvokable fromJson() {
      return args -> {
         if (args.length < 1) {
            throw new RuntimeException("fromJson() needs one argument: a JSON string");
         } else {
            Object arg = args[0];
            return arg instanceof String str ? Json.of(str).value() : arg;
         }
      };
   }

   static JavaInvokable jsonPath() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("jsonPath() needs two arguments: object and path");
         } else {
            Object json = args[0];
            if (json instanceof Node node) {
               json = Xml.toObject(node);
            }

            String path = args[1].toString();
            return JsonPath.using(jsonPathConfig).parse(json).read(path, new Predicate[0]);
         }
      };
   }

   static JavaInvokable keysOf() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("keysOf() needs one argument");
         } else {
            Map<String, Object> map = (Map<String, Object>)args[0];
            return new ArrayList<>(map.keySet());
         }
      };
   }

   static JavaInvokable lowerCase() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("lowerCase() needs one argument");
         } else {
            Object obj = args[0];
            if (obj instanceof String) {
               return ((String)obj).toLowerCase();
            } else if (obj instanceof Map) {
               String json = Json.stringifyStrict(obj).toLowerCase();
               return Json.of(json).value();
            } else if (obj instanceof List) {
               String json = Json.stringifyStrict(obj).toLowerCase();
               return Json.of(json).value();
            } else if (obj instanceof Node) {
               String xml = Xml.toString((Node)obj, false).toLowerCase();
               return Xml.toXmlDoc(xml);
            } else {
               return obj;
            }
         }
      };
   }

   static JavaInvokable map() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("map() needs two arguments: list and function");
         } else {
            List<?> list = (List<?>)args[0];
            JavaCallable fn = (JavaCallable)args[1];
            List<Object> result = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
               result.add(fn.call(null, list.get(i), i));
            }

            return result;
         }
      };
   }

   static JavaInvokable mapWithKey() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("mapWithKey() needs two arguments: list and key name");
         } else {
            Object listArg = args[0];
            if (listArg == null) {
               return new ArrayList();
            } else {
               List<?> list = (List<?>)listArg;
               String key = args[1].toString();
               List<Map<String, Object>> result = new ArrayList<>();

               for (Object item : list) {
                  Map<String, Object> map = new LinkedHashMap<>();
                  map.put(key, item);
                  result.add(map);
               }

               return result;
            }
         }
      };
   }

   static JavaInvokable merge() {
      return args -> {
         Map<String, Object> result = new LinkedHashMap<>();

         for (Object arg : args) {
            if (arg instanceof Map) {
               result.putAll((Map<? extends String, ? extends Object>)arg);
            }
         }

         return result;
      };
   }

   static JavaInvokable pause() {
      return args -> {
         if (args.length != 0 && args[0] != null) {
            long millis = ((Number)args[0]).longValue();

            try {
               Thread.sleep(millis);
            } catch (InterruptedException var4) {
               Thread.currentThread().interrupt();
            }

            return null;
         } else {
            return null;
         }
      };
   }

   static JavaInvokable pretty() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("pretty() needs one argument");
         } else {
            Object obj = args[0];
            if (obj instanceof Map || obj instanceof List) {
               return StringUtils.formatJson(obj);
            } else if (obj instanceof Node) {
               return Xml.toString((Node)obj, true);
            } else {
               return obj != null ? obj.toString() : "null";
            }
         }
      };
   }

   static JavaInvokable range() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("range() needs at least two arguments: start and end");
         } else {
            int start = ((Number)args[0]).intValue();
            int end = ((Number)args[1]).intValue();
            int step = args.length > 2 ? ((Number)args[2]).intValue() : 1;
            List<Integer> result = new ArrayList<>();
            if (step > 0) {
               for (int i = start; i < end; i += step) {
                  result.add(i);
               }
            } else if (step < 0) {
               for (int i = start; i > end; i += step) {
                  result.add(i);
               }
            }

            return result;
         }
      };
   }

   static JavaInvokable prettyXml() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("prettyXml() needs one argument");
         } else {
            Object obj = args[0];
            if (obj instanceof Node) {
               return Xml.toString((Node)obj, true);
            } else if (obj instanceof String) {
               return Xml.toString(Xml.toXmlDoc((String)obj), true);
            } else {
               throw new RuntimeException("prettyXml() argument must be XML node or string");
            }
         }
      };
   }

   static JavaInvokable repeat() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("repeat() needs two arguments: count and function");
         } else {
            int count = ((Number)args[0]).intValue();
            JavaCallable fn = (JavaCallable)args[1];
            List<Object> result = new ArrayList<>();

            for (int i = 0; i < count; i++) {
               result.add(fn.call(null, i));
            }

            return result;
         }
      };
   }

   static JavaInvokable sizeOf() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("sizeOf() needs one argument");
         } else {
            Object obj = args[0];
            if (obj instanceof List) {
               return ((List)obj).size();
            } else if (obj instanceof Map) {
               return ((Map)obj).size();
            } else {
               return obj instanceof String ? ((String)obj).length() : 0;
            }
         }
      };
   }

   static JavaInvokable sort() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("sort() needs at least one argument: the list to sort");
         } else {
            List<?> list = (List<?>)args[0];
            JavaCallable fn = args.length > 1 && args[1] instanceof JavaCallable ? (JavaCallable)args[1] : null;
            int count = list.size();
            List<Object[]> pairs = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
               Object item = list.get(i);
               Object key = fn == null ? item : fn.call(null, item, i);
               pairs.add(new Object[]{key, item});
            }

            pairs.sort((a, b) -> compareKeys(a[0], b[0]));
            List<Object> result = new ArrayList<>(count);

            for (Object[] pair : pairs) {
               result.add(pair[1]);
            }

            return result;
         }
      };
   }

   private static int compareKeys(Object a, Object b) {
      if (a != null && b != null) {
         if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number)a).doubleValue(), ((Number)b).doubleValue());
         } else {
            return a instanceof Comparable && a.getClass().isInstance(b) ? ((Comparable)a).compareTo(b) : String.valueOf(a).compareTo(String.valueOf(b));
         }
      } else {
         return a == b ? 0 : (a == null ? -1 : 1);
      }
   }

   static JavaInvokable toBean() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("toBean() needs two arguments: object and class name");
         } else {
            Object obj = args[0];
            String className = args[1].toString();
            String jsonString = Json.of(obj).toString();
            return Json.fromJson(jsonString, className);
         }
      };
   }

   static JavaInvokable toCsv() {
      return args -> {
         if (args.length == 0 || args[0] == null) {
            return "";
         } else if (!(args[0] instanceof List<?> rawList)) {
            throw new RuntimeException("toCsv() argument must be a list of maps, got: " + args[0].getClass().getName());
         } else {
            List<Map<String, Object>> list = (List<Map<String, Object>>)(Object) rawList;
            return list.isEmpty() ? "" : DataUtils.toCsv(list);
         }
      };
   }

   static JavaInvokable toBytes() {
      return args -> {
         if (args.length < 1) {
            throw new RuntimeException("toBytes() needs one argument: a list of numbers");
         } else {
            Object arg = args[0];
            if (arg instanceof byte[]) {
               return arg;
            } else if (!(arg instanceof List<?> list)) {
               throw new RuntimeException("toBytes() argument must be a list of numbers, got: " + arg.getClass().getName());
            } else {
               byte[] bytes = new byte[list.size()];

               for (int i = 0; i < list.size(); i++) {
                  Object item = list.get(i);
                  if (!(item instanceof Number num)) {
                     throw new RuntimeException("toBytes() list must contain only numbers, got: " + item.getClass().getName() + " at index " + i);
                  }

                  bytes[i] = num.byteValue();
               }

               return bytes;
            }
         }
      };
   }

   static JavaInvokable toJson() {
      return args -> {
         if (args.length < 1) {
            throw new RuntimeException("toJson() needs at least one argument");
         } else {
            Object obj = args[0];
            boolean removeNulls = args.length > 1 && Boolean.TRUE.equals(args[1]);
            Object result = Json.of(obj).value();
            if (removeNulls) {
               removeNullValues(result);
            }

            return result;
         }
      };
   }

   private static void removeNullValues(Object obj) {
      if (obj instanceof Map<?, ?> map) {
         map.entrySet().removeIf(e -> e.getValue() == null);
         map.values().forEach(KarateJsUtils::removeNullValues);
      } else if (obj instanceof List) {
         ((List)obj).forEach(KarateJsUtils::removeNullValues);
      }
   }

   static JavaCallable toStringValue() {
      return (thisRef, args) -> {
         if (args.length != 0 && args[0] != null) {
            Object value = args[0];
            if (value instanceof Map || value instanceof List) {
               return Json.of(value).toString();
            } else if (value instanceof Node) {
               return Xml.toString((Node)value, false);
            } else {
               return value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : value.toString();
            }
         } else {
            return null;
         }
      };
   }

   static JavaInvokable typeOf() {
      return args -> {
         if (args.length != 0 && args[0] != null) {
            Object value = args[0];
            if (value instanceof Boolean) {
               return "boolean";
            } else if (value instanceof Number) {
               return "number";
            } else if (value instanceof String) {
               return "string";
            } else if (value instanceof byte[]) {
               return "bytes";
            } else if (value instanceof List) {
               return "list";
            } else if (value instanceof JavaCallable) {
               return "function";
            } else if (value instanceof Map) {
               return "map";
            } else {
               return value instanceof Node ? "xml" : "object";
            }
         } else {
            return "null";
         }
      };
   }

   static JavaInvokable urlEncode() {
      return args -> args.length != 0 && args[0] != null ? URLEncoder.encode(args[0].toString(), StandardCharsets.UTF_8) : "";
   }

   static JavaInvokable urlDecode() {
      return args -> args.length != 0 && args[0] != null ? URLDecoder.decode(args[0].toString(), StandardCharsets.UTF_8) : "";
   }

   static JavaInvokable uuid() {
      return args -> UUID.randomUUID().toString();
   }

   static JavaInvokable valuesOf() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("valuesOf() needs one argument");
         } else {
            Object obj = args[0];
            if (obj instanceof Map) {
               return new ArrayList(((Map)obj).values());
            } else {
               return obj instanceof List ? new ArrayList((List)obj) : new ArrayList();
            }
         }
      };
   }

   static Map<String, Object> getOsInfo() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("name", System.getProperty("os.name", "unknown"));
      result.put("type", OsUtils.getOsType());
      return result;
   }

   static Object evalXmlPath(Node doc, String path) {
      NodeList nodeList;
      try {
         nodeList = Xml.getNodeListByPath(doc, path);
      } catch (Exception var7) {
         String strValue = Xml.getTextValueByPath(doc, path);
         if (path.startsWith("count")) {
            try {
               return Integer.parseInt(strValue);
            } catch (NumberFormatException var6) {
               return strValue;
            }
         }

         return strValue;
      }

      int count = nodeList.getLength();
      if (count == 0) {
         return null;
      } else if (count == 1) {
         return nodeToValue(nodeList.item(0));
      } else {
         List<Object> list = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            list.add(nodeToValue(nodeList.item(i)));
         }

         return list;
      }
   }

   static Object nodeToValue(Node node) {
      if (node.getNodeType() == 2) {
         return node.getNodeValue();
      } else {
         return Xml.getChildElementCount(node) == 0 ? node.getTextContent() : Xml.toNewDocument(node);
      }
   }

   static JavaInvokable fromString() {
      return args -> {
         if (args.length != 0 && args[0] != null) {
            String text = args[0].toString();
            if (text.isEmpty()) {
               return text;
            } else if (StringUtils.looksLikeJson(text)) {
               try {
                  return Json.of(text).value();
               } catch (Exception var3) {
                  logger.warn("fromString JSON parse failed: {}", var3.getMessage());
                  return text;
               }
            } else if (StringUtils.isXml(text)) {
               try {
                  return Xml.toXmlDoc(text);
               } catch (Exception var4) {
                  logger.warn("fromString XML parse failed: {}", var4.getMessage());
                  return text;
               }
            } else {
               return text;
            }
         } else {
            return null;
         }
      };
   }

   static String detectMimeType(Object obj) {
      if (obj instanceof Map || obj instanceof List) {
         return "application/json";
      } else if (obj instanceof Node) {
         return "application/xml";
      } else {
         return obj instanceof byte[] bytes ? sniffBytesMime(bytes) : "text/plain";
      }
   }

   static String sniffBytesMime(byte[] b) {
      if (b != null && b.length >= 4) {
         int b0 = b[0] & 255;
         int b1 = b[1] & 255;
         int b2 = b[2] & 255;
         int b3 = b[3] & 255;
         if (b0 == 137 && b1 == 80 && b2 == 78 && b3 == 71) {
            return "image/png";
         } else if (b0 == 255 && b1 == 216 && b2 == 255) {
            return "image/jpeg";
         } else if (b0 == 71 && b1 == 73 && b2 == 70 && b3 == 56) {
            return "image/gif";
         } else if (b0 == 66 && b1 == 77) {
            return "image/bmp";
         } else if (b0 == 37 && b1 == 80 && b2 == 68 && b3 == 70) {
            return "application/pdf";
         } else {
            return b.length >= 12
                  && b0 == 82
                  && b1 == 73
                  && b2 == 70
                  && b3 == 70
                  && (b[8] & 255) == 87
                  && (b[9] & 255) == 69
                  && (b[10] & 255) == 66
                  && (b[11] & 255) == 80
               ? "image/webp"
               : "application/octet-stream";
         }
      } else {
         return "application/octet-stream";
      }
   }

   static byte[] convertToBytes(Object obj) {
      if (obj == null) {
         return new byte[0];
      } else if (obj instanceof byte[]) {
         return (byte[])obj;
      } else if (obj instanceof String str) {
         return str.getBytes(StandardCharsets.UTF_8);
      } else if (!(obj instanceof List<?> list)) {
         if (obj instanceof Map) {
            return StringUtils.formatJson(obj).getBytes(StandardCharsets.UTF_8);
         } else {
            return obj instanceof Node ? Xml.toString((Node)obj, true).getBytes(StandardCharsets.UTF_8) : obj.toString().getBytes(StandardCharsets.UTF_8);
         }
      } else if (!list.isEmpty() && list.getFirst() instanceof Number) {
         byte[] bytes = new byte[list.size()];

         for (int i = 0; i < list.size(); i++) {
            bytes[i] = ((Number)list.get(i)).byteValue();
         }

         return bytes;
      } else {
         return StringUtils.formatJson(list).getBytes(StandardCharsets.UTF_8);
      }
   }

   static Object navigateToPath(Object target, String path) {
      String[] parts = path.split("\\.");
      Object current = target;

      for (String part : parts) {
         if (!(current instanceof Map)) {
            return null;
         }

         current = ((Map)current).get(part);
      }

      return current;
   }

   static void setAtPath(Object target, String path, Object value) {
      String[] parts = path.split("\\.");
      Object current = target;

      for (int i = 0; i < parts.length - 1; i++) {
         if (current instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) current;
            Object next = map.get(parts[i]);
            if (next == null) {
               next = new LinkedHashMap();
               map.put(parts[i], next);
            }

            current = next;
         }
      }

      if (current instanceof Map) {
         ((Map)current).put(parts[parts.length - 1], value);
      }
   }

   static int findMatchingParen(String str, int openPos) {
      int depth = 1;

      for (int i = openPos + 1; i < str.length(); i++) {
         char c = str.charAt(i);
         if (c == '(') {
            depth++;
         } else if (c == ')') {
            if (--depth == 0) {
               return i;
            }
         }
      }

      return -1;
   }

   static String valueToString(Object value) {
      if (value == null) {
         return "";
      } else {
         return value instanceof Node ? Xml.toString((Node)value, false) : value.toString();
      }
   }

   static JavaInvokable xmlPath() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("xmlPath() needs two arguments: xml and path");
         } else {
            Object xmlObj = args[0];
            String path = args[1].toString();
            Node doc;
            if (xmlObj instanceof Node docNode) {
               doc = docNode;
            } else {
               if (!(xmlObj instanceof String)) {
                  throw new RuntimeException("xmlPath() first argument must be XML node or string, but was: " + (xmlObj == null ? "null" : xmlObj.getClass()));
               }

               doc = Xml.toXmlDoc((String)xmlObj);
            }

            try {
               return evalXmlPath(doc, path);
            } catch (Exception var5) {
               throw new RuntimeException("xmlPath failed for path: " + path + " - " + var5.getMessage(), var5);
            }
         }
      };
   }

   static JavaInvokable fail() {
      return args -> {
         String message = args.length > 0 && args[0] != null ? args[0].toString() : "karate.fail() called";
         throw new RuntimeException(message);
      };
   }

   static JavaInvokable toJava() {
      return args -> {
         logger.warn("karate.toJava() is deprecated and a no-op in V2 - JavaScript arrays work directly with Java");
         return args.length < 1 ? null : args[0];
      };
   }

   static JavaInvokable stop() {
      return args -> {
         int port = 0;
         if (args.length > 0 && args[0] != null) {
            if (args[0] instanceof Number) {
               port = ((Number)args[0]).intValue();
            } else {
               port = Integer.parseInt(args[0].toString());
            }
         }

         try {
            Boolean var12;
            try (ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
               int actualPort = serverSocket.getLocalPort();
               System.out
                  .println(
                     "*** waiting for socket, type the command below:\ncurl http://localhost:"
                        + actualPort
                        + "\nin a new terminal (or open the URL in a web-browser) to proceed ..."
                  );

               try (Socket clientSocket = serverSocket.accept()) {
                  OutputStream out = clientSocket.getOutputStream();
                  out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK".getBytes());
                  out.flush();
               }

               logger.info("*** exited socket wait successfully");
               var12 = true;
            }

            return var12;
         } catch (Exception var11) {
            throw new RuntimeException("stop() failed: " + var11.getMessage(), var11);
         }
      };
   }

   static JavaInvokable waitForHttp() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("waitForHttp() needs a URL argument");
         } else {
            String url = args[0] + "";
            int timeoutMs = 30000;
            int pollMs = 250;
            if (args.length > 1 && args[1] instanceof Map<?, ?> options) {
               if (options.containsKey("timeout")) {
                  timeoutMs = ((Number)options.get("timeout")).intValue();
               }

               if (options.containsKey("interval")) {
                  pollMs = ((Number)options.get("interval")).intValue();
               }
            }

            long deadline = System.currentTimeMillis() + timeoutMs;
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(pollMs)).build();

            while (System.currentTimeMillis() < deadline) {
               try {
                  HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(pollMs)).GET().build();
                  HttpResponse<Void> response = httpClient.send(request, BodyHandlers.discarding());
                  int status = response.statusCode();
                  if (status >= 200 && status < 400) {
                     return true;
                  }
               } catch (Exception var11) {
               }

               try {
                  Thread.sleep(pollMs);
               } catch (InterruptedException var10) {
                  Thread.currentThread().interrupt();
                  return false;
               }
            }

            throw new RuntimeException("HTTP endpoint not available: " + url);
         }
      };
   }

   static JavaInvokable waitForPort() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("waitForPort() needs host and port arguments");
         } else {
            String host = args[0] + "";
            int port;
            if (args[1] instanceof Number) {
               port = ((Number)args[1]).intValue();
            } else {
               port = Integer.parseInt(args[1].toString());
            }

            int timeoutMs = 30000;
            int pollMs = 250;
            if (args.length > 2 && args[2] instanceof Number) {
               timeoutMs = ((Number)args[2]).intValue();
            }

            if (args.length > 3 && args[3] instanceof Number) {
               pollMs = ((Number)args[3]).intValue();
            }

            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
               try {
                  Boolean t$;
                  try (Socket socket = new Socket(host, port)) {
                     t$ = true;
                  }

                  return t$;
               } catch (Exception var13) {
                  try {
                     Thread.sleep(pollMs);
                  } catch (InterruptedException var10) {
                     Thread.currentThread().interrupt();
                     return false;
                  }
               }
            }

            throw new RuntimeException("Port " + host + ":" + port + " not available within timeout");
         }
      };
   }
}
