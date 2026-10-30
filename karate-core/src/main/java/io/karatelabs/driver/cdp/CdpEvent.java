package io.karatelabs.driver.cdp;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import java.util.Map;

public class CdpEvent {
   private final String method;
   private final Map<String, Object> params;
   private final String sessionId;
   private final Map<String, Object> raw;

   public CdpEvent(Map<String, Object> raw) {
      this.raw = raw;
      this.method = (String)raw.get("method");
      this.params = (Map<String, Object>)raw.get("params");
      this.sessionId = (String)raw.get("sessionId");
   }

   public String getMethod() {
      return this.method;
   }

   public Map<String, Object> getParams() {
      return this.params;
   }

   public String getSessionId() {
      return this.sessionId;
   }

   public Map<String, Object> getRaw() {
      return this.raw;
   }

   public <T> T get(String path) {
      return this.params == null ? null : this.getFromMap(this.params, path);
   }

   public String getAsString(String path) {
      Object value = this.get(path);
      return value != null ? value.toString() : null;
   }

   public Integer getAsInt(String path) {
      Object value = this.get(path);
      if (value == null) {
         return null;
      } else {
         return value instanceof Number ? ((Number)value).intValue() : Integer.parseInt(value.toString());
      }
   }

   public Boolean getAsBoolean(String path) {
      Object value = this.get(path);
      if (value == null) {
         return null;
      } else {
         return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(value.toString());
      }
   }

   private <T> T getFromMap(Map<String, Object> map, String path) {
      if (map == null) {
         return null;
      } else if (!path.contains("[") && !path.startsWith("$")) {
         return this.getByDotPath(map, path);
      } else {
         String jsonPath = path.startsWith("$") ? path : "$." + path;

         try {
            return (T)JsonPath.read(map, jsonPath, new Predicate[0]);
         } catch (PathNotFoundException var5) {
            return null;
         }
      }
   }

   private <T> T getByDotPath(Map<String, Object> map, String path) {
      String[] parts = path.split("\\.");
      Object current = map;

      for (String part : parts) {
         if (current == null) {
            return null;
         }

         if (!(current instanceof Map)) {
            return null;
         }

         current = ((Map)current).get(part);
      }

      return (T)current;
   }

   @Override
   public String toString() {
      return "CdpEvent[" + this.method + "]";
   }
}
