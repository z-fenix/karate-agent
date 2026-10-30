package io.karatelabs.match;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.js.Context;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class Value implements SimpleObject, AutoCloseable {
   private static final Logger logger = LoggerFactory.getLogger(Value.class);
   public static final long MEMORY_THRESHOLD = 10485760L;
   final Value.Type type;
   final BiConsumer<Context, Result> onResult;
   private final Object value;
   private LargeValueStore largeStore;
   private final long memoryThreshold;
   private Context context;

   Value(Object value) {
      this(value, null, null);
   }

   Value(Object value, Context context, BiConsumer<Context, Result> onResult) {
      this(value, context, onResult, true, 10485760L);
   }

   Value(Object value, Context context, BiConsumer<Context, Result> onResult, boolean checkLargeCollection) {
      this(value, context, onResult, checkLargeCollection, 10485760L);
   }

   Value(Object value, Context context, BiConsumer<Context, Result> onResult, boolean checkLargeCollection, long memoryThreshold) {
      this.context = context;
      this.memoryThreshold = memoryThreshold;
      if (value instanceof Set<?> set) {
         value = new ArrayList(set);
      } else if (value != null && value.getClass().isArray()) {
         int length = Array.getLength(value);
         List<Object> list = new ArrayList<>(length);

         for (int i = 0; i < length; i++) {
            list.add(Array.get(value, i));
         }

         value = list;
      }

      this.value = value;
      this.onResult = onResult;
      if (value == null) {
         this.type = Value.Type.NULL;
      } else if (value instanceof Node) {
         this.type = Value.Type.XML;
      } else if (value instanceof List) {
         this.type = Value.Type.LIST;
         if (checkLargeCollection) {
            this.checkAndCreateLargeStore((List<?>)value);
         }
      } else if (value instanceof Map) {
         this.type = Value.Type.MAP;
      } else if (value instanceof String) {
         this.type = Value.Type.STRING;
      } else if (Number.class.isAssignableFrom(value.getClass())) {
         this.type = Value.Type.NUMBER;
      } else if (Boolean.class.equals(value.getClass())) {
         this.type = Value.Type.BOOLEAN;
      } else if (value instanceof byte[]) {
         this.type = Value.Type.BYTES;
      } else {
         this.type = Value.Type.OTHER;
      }
   }

   private void checkAndCreateLargeStore(List<?> list) {
      long estimatedSize = DiskBackedList.estimateCollectionSize(list);
      if (estimatedSize > this.memoryThreshold) {
         try {
            this.largeStore = DiskBackedList.create(list);
            logger.debug("created disk-backed store for collection of estimated size {} bytes", estimatedSize);
         } catch (IOException var5) {
            logger.warn("failed to create disk-backed store, keeping in memory: {}", var5.getMessage());
         }
      }
   }

   public boolean isBoolean() {
      return this.type == Value.Type.BOOLEAN;
   }

   public boolean isNumber() {
      return this.type == Value.Type.NUMBER;
   }

   public boolean isString() {
      return this.type == Value.Type.STRING;
   }

   public boolean isNull() {
      return this.type == Value.Type.NULL;
   }

   public boolean isMap() {
      return this.type == Value.Type.MAP;
   }

   public boolean isList() {
      return this.type == Value.Type.LIST;
   }

   public boolean isXml() {
      return this.type == Value.Type.XML;
   }

   boolean isNotPresent() {
      return "#notpresent".equals(this.value);
   }

   boolean isArrayObjectOrReference() {
      String temp = this.value.toString();
      return temp.startsWith("#[")
         || temp.startsWith("##[")
         || temp.startsWith("#(")
         || temp.startsWith("##(")
         || "#array".equals(temp)
         || "##array".equals(temp)
         || "#object".equals(temp)
         || "##object".equals(temp);
   }

   boolean isMapOrListOrXml() {
      return switch (this.type) {
         case LIST, MAP, XML -> true;
         default -> false;
      };
   }

   public boolean isLargeCollection() {
      return this.largeStore != null;
   }

   public LargeValueStore getLargeStore() {
      return this.largeStore;
   }

   public int getListSize() {
      if (this.type != Value.Type.LIST) {
         return -1;
      } else {
         return this.largeStore != null ? this.largeStore.size() : ((List)this.value).size();
      }
   }

   public Iterator<Object> listIterator() {
      if (this.type != Value.Type.LIST) {
         return null;
      } else {
         return this.largeStore != null ? this.largeStore.iterator() : ((List)this.value).iterator();
      }
   }

   public Object getListElement(int index) {
      if (this.type != Value.Type.LIST) {
         throw new IllegalStateException("not a list");
      } else {
         return this.largeStore != null ? this.largeStore.get(index) : ((List)this.value).get(index);
      }
   }

   @Override
   public void close() {
      if (this.largeStore != null) {
         this.largeStore.close();
         this.largeStore = null;
      }
   }

   public <T> T getValue() {
      return (T)this.value;
   }

   String getWithinSingleQuotesIfString() {
      return this.type == Value.Type.STRING ? "'" + this.value + "'" : this.getAsString();
   }

   public String getAsString() {
      switch (this.type) {
         case LIST:
         case MAP:
            return Json.stringifyStrict(this.value);
         case XML:
            return Xml.toString(this.getValue());
         default:
            return this.value + "";
      }
   }

   String getAsXmlString() {
      if (this.type == Value.Type.MAP) {
         Node node = Xml.fromMap(this.getValue());
         return Xml.toString(node);
      } else {
         return this.getAsString();
      }
   }

   Value getSortedLike(Value other) {
      if (this.isMap() && other.isMap()) {
         Map<String, Object> reference = other.getValue();
         Map<String, Object> source = this.getValue();
         Set<String> remainder = new LinkedHashSet<>(source.keySet());
         Map<String, Object> result = new LinkedHashMap<>(source.size());
         reference.keySet().forEach(keyx -> {
            if (source.containsKey(keyx)) {
               result.put(keyx, source.get(keyx));
               remainder.remove(keyx);
            }
         });

         for (String key : remainder) {
            result.put(key, source.get(key));
         }

         return new Value(result, other.context, other.onResult);
      } else {
         return this;
      }
   }

   @Override
   public String toString() {
      return "[type: " + this.type + ", value: " + this.value + "]";
   }

   public Result is(Match.Type matchType, Object expected) {
      return this.is(null, matchType, expected);
   }

   public Result is(Engine engine, Match.Type matchType, Object expected) {
      return this.is(engine, null, matchType, expected);
   }

   public Result is(Engine engine, MarkerPolicy policy, Match.Type matchType, Object expected) {
      Value expectedValue = new Value(parseIfJsonOrXmlString(expected), this.context, this.onResult);

      Result result;
      try {
         Operation op = new Operation(engine, policy, matchType, this, expectedValue, false);
         op.execute();
         result = op.getResult();
      } catch (Throwable var10) {
         try {
            expectedValue.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      expectedValue.close();
      if (this.onResult != null) {
         this.onResult.accept(this.context, result);
      }

      return result;
   }

   public static Object parseIfJsonOrXmlString(Object o) {
      if (o instanceof String s) {
         if (s.isEmpty()) {
            return o;
         }

         if (StringUtils.looksLikeJson(s)) {
            return Json.of(s).value();
         }

         if (StringUtils.isXml(s)) {
            return Xml.toXmlDoc(s);
         }

         if (s.charAt(0) == '\\') {
            return s.substring(1);
         }
      }

      return o;
   }

   public Result _equals(Object expected) {
      return this.is(Match.Type.EQUALS, expected);
   }

   public Result contains(Object expected) {
      return this.is(Match.Type.CONTAINS, expected);
   }

   public Result containsDeep(Object expected) {
      return this.is(Match.Type.CONTAINS_DEEP, expected);
   }

   public Result containsOnly(Object expected) {
      return this.is(Match.Type.CONTAINS_ONLY, expected);
   }

   public Result containsOnlyDeep(Object expected) {
      return this.is(Match.Type.CONTAINS_ONLY_DEEP, expected);
   }

   public Result containsAny(Object expected) {
      return this.is(Match.Type.CONTAINS_ANY, expected);
   }

   public Result notEquals(Object expected) {
      return this.is(Match.Type.NOT_EQUALS, expected);
   }

   public Result notContains(Object expected) {
      return this.is(Match.Type.NOT_CONTAINS, expected);
   }

   public Result eachEquals(Object expected) {
      return this.is(Match.Type.EACH_EQUALS, expected);
   }

   public Result eachNotEquals(Object expected) {
      return this.is(Match.Type.EACH_NOT_EQUALS, expected);
   }

   public Result eachContains(Object expected) {
      return this.is(Match.Type.EACH_CONTAINS, expected);
   }

   public Result eachNotContains(Object expected) {
      return this.is(Match.Type.EACH_NOT_CONTAINS, expected);
   }

   public Result eachContainsDeep(Object expected) {
      return this.is(Match.Type.EACH_CONTAINS_DEEP, expected);
   }

   public Result eachContainsOnly(Object expected) {
      return this.is(Match.Type.EACH_CONTAINS_ONLY, expected);
   }

   public Result eachContainsAny(Object expected) {
      return this.is(Match.Type.EACH_CONTAINS_ANY, expected);
   }

   public Result within(Object expected) {
      return this.is(Match.Type.WITHIN, expected);
   }

   public Result notWithin(Object expected) {
      return this.is(Match.Type.NOT_WITHIN, expected);
   }

   JavaCallable call(Match.Type matchType) {
      return (context, args) -> {
         this.context = context;
         Result result = this.is(matchType, args[0]);
         return result.toMap();
      };
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "equals" -> this.call(Match.Type.EQUALS);
         case "contains" -> this.call(Match.Type.CONTAINS);
         case "containsDeep" -> this.call(Match.Type.CONTAINS_DEEP);
         case "containsOnly" -> this.call(Match.Type.CONTAINS_ONLY);
         case "containsOnlyDeep" -> this.call(Match.Type.CONTAINS_ONLY_DEEP);
         case "containsAny" -> this.call(Match.Type.CONTAINS_ANY);
         case "notEquals" -> this.call(Match.Type.NOT_EQUALS);
         case "eachEquals" -> this.call(Match.Type.EACH_EQUALS);
         case "notContains" -> this.call(Match.Type.NOT_CONTAINS);
         case "eachNotEquals" -> this.call(Match.Type.EACH_NOT_EQUALS);
         case "eachContains" -> this.call(Match.Type.EACH_CONTAINS);
         case "eachNotContains" -> this.call(Match.Type.EACH_NOT_CONTAINS);
         case "eachContainsDeep" -> this.call(Match.Type.EACH_CONTAINS_DEEP);
         case "eachContainsOnly" -> this.call(Match.Type.EACH_CONTAINS_ONLY);
         case "eachContainsAny" -> this.call(Match.Type.EACH_CONTAINS_ANY);
         case "within" -> this.call(Match.Type.WITHIN);
         case "notWithin" -> this.call(Match.Type.NOT_WITHIN);
         default -> throw new RuntimeException("no such match api: " + name);
      };
   }

   public static enum Type {
      NULL,
      BOOLEAN,
      NUMBER,
      STRING,
      BYTES,
      LIST,
      MAP,
      XML,
      OTHER;
   }
}
