package io.karatelabs.js;

import io.karatelabs.common.StringUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface SimpleObject extends ObjectLike {
   Logger logger = LoggerFactory.getLogger(SimpleObject.class);
   String TO_STRING = "toString";

   @Override
   default void putMember(String name, Object value) {
      logger.warn("putMember() not implemented for: {} - {}", name, this.getClass().getName());
   }

   @Override
   default void removeMember(String name) {
      logger.warn("removeMember() not implemented for: {} - {}", name, this.getClass().getName());
   }

   @Override
   default Map<String, Object> toMap() {
      return toMap(this.jsKeys(), this);
   }

   default Collection<String> jsKeys() {
      logger.warn("jsKeys() not implemented for: {}", this.getClass().getName());
      return Collections.emptyList();
   }

   @Override
   default Object getMember(String name) {
      return "toString".equals(name) ? this.jsToString() : this.jsGet(name);
   }

   Object jsGet(String var1);

   default JavaCallable jsToString() {
      try {
         if (this.jsGet("toString") instanceof JavaCallable jsc) {
            return jsc;
         }
      } catch (Exception var3) {
      }

      return (context, args) -> toString(this.toMap());
   }

   static String toString(Map<String, Object> map) {
      return StringUtils.formatJson(map, false, false, false);
   }

   static Map<String, Object> toMap(Collection<String> keys, SimpleObject so) {
      Map<String, Object> map = new LinkedHashMap<>();

      for (String key : keys) {
         map.put(key, so.jsGet(key));
      }

      return map;
   }
}
