package io.karatelabs.common;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.reader.JsonWriterI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class Json {
   private static final Logger logger = LoggerFactory.getLogger(Json.class);
   private final DocumentContext doc;
   private final boolean array;
   private final boolean object;
   private final String prefix;
   static final Pattern JS_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*$");
   private static final JSONStyle JSON_STYLE = new JSONStyle(2);

   private String prefix(String path) {
      return path.charAt(0) == '$' ? path : this.prefix + path;
   }

   public static Json object() {
      return new Json(JsonPath.parse("{}"));
   }

   public static Json array() {
      return new Json(JsonPath.parse("[]"));
   }

   public static Json of(Object any) {
      if (any == null) {
         throw new IllegalArgumentException("input must not be null");
      } else if (any instanceof String s) {
         if (s.isBlank()) {
            throw new IllegalArgumentException("input string must not be empty or blank");
         } else {
            return new Json(JsonPath.parse(parseLenient(s)));
         }
      } else if (!(any instanceof List) && !(any instanceof Map)) {
         String json = JSONValue.toJSONString(any);
         return new Json(JsonPath.parse(json));
      } else {
         return new Json(JsonPath.parse(any));
      }
   }

   public static Object parseLenient(String json) {
      if (json != null && !json.isBlank()) {
         try {
            Object result = JSONValue.parseKeepingOrder(json);
            if (!isMapOrList(result)) {
               throw new RuntimeException("invalid json: not a JSON object or array");
            } else {
               return result;
            }
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
            throw new RuntimeException("invalid json: " + var3.getMessage(), var3);
         }
      } else {
         throw new RuntimeException("invalid json: input is null or blank");
      }
   }

   public static boolean isMapOrList(Object o) {
      return o instanceof Map || o instanceof List;
   }

   public static Object parseStrict(String json) {
      return parseStrict(json, false);
   }

   public static Object parseStrict(String json, boolean keepOrder) {
      if (json != null && !json.isBlank()) {
         try {
            JSONParser parser = new JSONParser(4752);
            return keepOrder ? parser.parse(json, JSONValue.defaultReader.DEFAULT_ORDERED) : parser.parse(json);
         } catch (Exception var3) {
            throw new RuntimeException("invalid json: " + var3.getMessage(), var3);
         }
      } else {
         throw new RuntimeException("invalid json: input is null or blank");
      }
   }

   private Json(DocumentContext doc) {
      this.doc = doc;
      this.array = doc.json() instanceof List;
      this.object = doc.json() instanceof Map;
      this.prefix = this.array ? "$" : "$.";
   }

   public Json getJson(String path) {
      return of(this.get(path, Object.class));
   }

   public <T> List<T> getAll(String prefix, List<String> paths) {
      List<T> res = new ArrayList<>();

      for (String path : paths) {
         res.add((T)this.doc.read(this.prefix(prefix + path), new Predicate[0]));
      }

      return res;
   }

   public <T> List<T> getAll(List<String> paths) {
      return this.getAll("", paths);
   }

   public <T> T get(String path) {
      return (T)this.doc.read(this.prefix(path), new Predicate[0]);
   }

   public <T> T get(String path, T defaultValue) {
      return this.<T>getOptional(path).orElse(defaultValue);
   }

   public <T> Optional<T> getOptional(String path) {
      try {
         return Optional.of(this.get(path));
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   public <T> T getFirst(String path) {
      List<T> list = this.get(path);
      return list != null && !list.isEmpty() ? list.getFirst() : null;
   }

   public <T> T getAs(String path, Class<T> clazz) {
      return (T)this.doc.read(this.prefix(path), clazz, new Predicate[0]);
   }

   @Override
   public String toString() {
      return !this.array && !this.object ? (String)this.doc.json() : this.doc.jsonString();
   }

   public boolean isJson() {
      return this.array || this.object;
   }

   public boolean isArray() {
      return this.array;
   }

   public boolean isObject() {
      return this.object;
   }

   public <T> T value() {
      return (T)this.doc.read("$", new Predicate[0]);
   }

   public List<Object> asList() {
      return this.value();
   }

   public Map<String, Object> asMap() {
      return this.value();
   }

   public Json set(String path, Object value) {
      this.setInternal(path, value);
      return this;
   }

   public Json set(String path, String value) {
      if (value == null || value.isEmpty()) {
         this.setInternal(path, value);
      } else if (StringUtils.looksLikeJson(value)) {
         this.setInternal(path, of(value).value());
      } else if (value.charAt(0) == '\\') {
         this.setInternal(path, value.substring(1));
      } else {
         this.setInternal(path, value);
      }

      return this;
   }

   public Json setAsString(String path, String value) {
      this.setInternal(path, value);
      return this;
   }

   public Json remove(String path) {
      if (this.pathExists(path)) {
         this.doc.delete(this.prefix(path), new Predicate[0]);
      }

      return this;
   }

   private boolean isArrayPath(String s) {
      return s.endsWith("]") && !s.endsWith("']");
   }

   private String arrayKey(String s) {
      int pos = s.lastIndexOf(91);
      return s.substring(0, pos);
   }

   private int arrayIndex(String s) {
      int leftPos = s.lastIndexOf(91);
      if (leftPos == -1) {
         return -1;
      } else {
         int rightPos = s.indexOf(93, leftPos);
         String num = s.substring(leftPos + 1, rightPos);
         if (num.isEmpty()) {
            return -1;
         } else {
            try {
               return Integer.parseInt(num);
            } catch (NumberFormatException var6) {
               return -1;
            }
         }
      }
   }

   private void setInternal(String path, Object o) {
      path = this.prefix(path);
      if ("$".equals(path)) {
         throw new RuntimeException("cannot replace root path $");
      } else {
         boolean forArray = this.isArrayPath(path);
         if (!this.pathExists(path)) {
            this.createPath(path, forArray);
         }

         Pair<String> pair = toParentAndLeaf(path);
         if (forArray) {
            int index = this.arrayIndex(pair.right);
            if (index == -1) {
               this.doc.add(this.arrayKey(path), o, new Predicate[0]);
            } else {
               this.doc.set(path, o, new Predicate[0]);
            }
         } else {
            this.doc.put(pair.left, pair.right, o, new Predicate[0]);
         }
      }
   }

   public boolean pathExists(String path) {
      if (path.endsWith("[]")) {
         path = path.substring(0, path.length() - 2);
      }

      try {
         Object temp = this.doc.read(path, new Predicate[0]);
         return temp != null;
      } catch (PathNotFoundException var3) {
         return false;
      }
   }

   private void createPath(String path, boolean array) {
      if (this.isArrayPath(path)) {
         String parentPath = this.arrayKey(path);
         if (!this.pathExists(parentPath)) {
            this.createPath(parentPath, true);
         }

         List<Object> list = this.get(parentPath);
         if (list == null) {
            list = new ArrayList<>();
            this.set(parentPath, list);
         }

         int index = this.arrayIndex(path);
         if (list.size() <= index) {
            for (int i = list.size(); i <= index; i++) {
               list.add(null);
            }
         }
      } else {
         Pair<String> pair = toParentAndLeaf(path);
         if (!this.pathExists(pair.left)) {
            this.createPath(pair.left, false);
         }

         if (this.isArrayPath(pair.left)) {
            if (this.isArrayPath(pair.right)) {
               this.doc.set(pair.left, new ArrayList(), new Predicate[0]);
            } else {
               if (!this.pathExists(pair.left)) {
                  this.doc.set(pair.left, new LinkedHashMap(), new Predicate[0]);
               }

               this.doc.put(pair.left, pair.right, array ? new ArrayList() : new LinkedHashMap(), new Predicate[0]);
            }
         } else {
            this.doc.put(pair.left, pair.right, array ? new ArrayList() : new LinkedHashMap(), new Predicate[0]);
         }
      }
   }

   public static Pair<String> toParentAndLeaf(String path) {
      int pos = path.lastIndexOf(46);
      int temp = path.lastIndexOf("['");
      if (temp != -1 && temp > pos) {
         pos = temp - 1;
      }

      String right = path.substring(pos + 1);
      if (right.startsWith("[")) {
         pos++;
      }

      String left = path.substring(0, pos == -1 ? 0 : pos);
      if (right.length() >= 4 && right.charAt(0) == '[') {
         char q = right.charAt(1);
         if (q == '\'' || q == '"') {
            int end = right.indexOf(q, 2);
            if (end > 1 && end + 1 < right.length() && right.charAt(end + 1) == ']') {
               right = right.substring(2, end) + right.substring(end + 2);
            }
         }
      }

      return Pair.of(left, right);
   }

   public static boolean isValidKey(String key) {
      return JS_IDENTIFIER_PATTERN.matcher(key).matches();
   }

   public static String escape(String raw) {
      return JSONValue.escape(raw, JSONStyle.LT_COMPRESS);
   }

   public static Json merge(Object... objects) {
      if (objects.length == 0) {
         return object();
      } else {
         Json json = of(objects[0]);
         if (objects.length == 1) {
            return json;
         } else {
            for (int i = 1; i < objects.length; i++) {
               json.merge(objects[i]);
            }

            return json;
         }
      }
   }

   public Json merge(Object other) {
      Json o = of(other);
      if (this.isObject() && o.isObject()) {
         Map<String, Object> map = this.asMap();
         map.putAll(o.asMap());
      } else if (this.isArray() && o.isArray()) {
         List<Object> list = this.asList();
         list.addAll(o.asList());
      }

      return this;
   }

   public String toStringPretty() {
      return this.toString(true, false, false);
   }

   public String toString(boolean pretty, boolean lenient, boolean sort) {
      if (this.isJson()) {
         return StringUtils.formatJson(this.value(), pretty, lenient, sort);
      } else {
         Object value = this.value();
         return value == null ? "" : this.value().toString();
      }
   }

   public static Object copy(Object o, boolean deep, boolean dropCircularReferences) {
      if (!deep) {
         if (o instanceof List<?> list) {
            return new ArrayList(list);
         } else {
            return o instanceof Map<?, ?> map ? new LinkedHashMap<>(map) : o;
         }
      } else {
         Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
         return copyDeep(o, seen, dropCircularReferences);
      }
   }

   private static Object copyDeep(Object o, Set<Object> seen, boolean dropCircularReferences) {
      if (o instanceof List<?> list) {
         if (!seen.add(o)) {
            return dropCircularReferences ? "#" + o.getClass().getName() : o;
         } else {
            int count = list.size();
            List<Object> listCopy = new ArrayList<>(count);

            for (Object value : list) {
               listCopy.add(copyDeep(value, seen, dropCircularReferences));
            }

            return listCopy;
         }
      } else if (o instanceof Map) {
         if (seen.add(o)) {
            Map<String, Object> map = (Map<String, Object>)o;
            Map<String, Object> mapCopy = new LinkedHashMap<>(map.size());
            map.forEach((k, v) -> mapCopy.put(k, copyDeep(v, seen, dropCircularReferences)));
            return mapCopy;
         } else {
            return dropCircularReferences ? "#" + o.getClass().getName() : o;
         }
      } else if (!(o instanceof Object[] arr)) {
         return o;
      } else if (!seen.add(o)) {
         return dropCircularReferences ? "#" + o.getClass().getName() : o;
      } else {
         Object[] arrCopy = new Object[arr.length];

         for (int i = 0; i < arr.length; i++) {
            arrCopy[i] = copyDeep(arr[i], seen, dropCircularReferences);
         }

         return arrCopy;
      }
   }

   public static byte[] toBytes(Object o) {
      if (o == null) {
         return null;
      } else if (o instanceof Map || o instanceof List) {
         return FileUtils.toBytes(JSONValue.toJSONString(breakCyclesIfAny(o)));
      } else if (o instanceof Node) {
         return FileUtils.toBytes(Xml.toString((Node)o));
      } else {
         return o instanceof byte[] ? (byte[])o : FileUtils.toBytes(o.toString());
      }
   }

   public static String stringifyStrict(Object o) {
      if (!(o instanceof Map) && !(o instanceof List)) {
         return o == null ? "" : o.toString();
      } else {
         return JSONValue.toJSONString(breakCyclesIfAny(o), JSON_STYLE);
      }
   }

   private static Object breakCyclesIfAny(Object o) {
      Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
      return hasCycle(o, seen) ? copy(o, true, true) : o;
   }

   private static boolean hasCycle(Object o, Set<Object> seen) {
      if (o instanceof Map<?, ?> map) {
         if (!seen.add(o)) {
            return true;
         }

         for (Object v : map.values()) {
            if (hasCycle(v, seen)) {
               return true;
            }
         }

         seen.remove(o);
      } else if (o instanceof List<?> list) {
         if (!seen.add(o)) {
            return true;
         }

         for (Object vx : list) {
            if (hasCycle(vx, seen)) {
               return true;
            }
         }

         seen.remove(o);
      } else if (o instanceof Object[] arr) {
         if (!seen.add(o)) {
            return true;
         }

         for (Object vxx : arr) {
            if (hasCycle(vxx, seen)) {
               return true;
            }
         }

         seen.remove(o);
      }

      return false;
   }

   public static Object fromJson(String json, String className) {
      try {
         Class<?> clazz = Class.forName(className);
         return JSONValue.parse(json, clazz);
      } catch (Exception var3) {
         throw new RuntimeException("Failed to deserialize JSON to " + className + ": " + var3.getMessage(), var3);
      }
   }

   static {
      JSONValue.defaultWriter.registerWriterInterfaceFirst(Node.class, new JsonWriterI<Node>() {
         public <E extends Node> void writeJSONString(E node, Appendable out, JSONStyle style) throws IOException {
            JSONValue.writeJSONString(Xml.toString(node), out, style);
         }
      });
      JSONValue.registerWriter(byte[].class, new JsonWriterI<Object>() {
         public <E> void writeJSONString(E value, Appendable out, JSONStyle style) throws IOException {
            byte[] bytes = (byte[])value;
            out.append('[');

            for (int i = 0; i < bytes.length; i++) {
               if (i > 0) {
                  out.append(',');
               }

               out.append(Integer.toString(bytes[i]));
            }

            out.append(']');
         }
      });
      JSONValue.registerWriter(char[].class, new JsonWriterI<Object>() {
         public <E> void writeJSONString(E value, Appendable out, JSONStyle style) throws IOException {
            List<String> strings = new ArrayList<>();

            for (char c : (char[])value) {
               strings.add(String.valueOf(c));
            }

            JSONValue.writeJSONString(strings, out, style);
         }
      });
   }
}
