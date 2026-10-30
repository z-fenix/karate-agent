package io.karatelabs.driver.cdp;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import java.util.Map;

public class CdpResponse {
   private final int id;
   private final Map<String, Object> result;
   private final Map<String, Object> error;
   private final Map<String, Object> raw;

   public CdpResponse(Map<String, Object> raw) {
      this.raw = raw;
      this.id = raw.containsKey("id") ? ((Number)raw.get("id")).intValue() : -1;
      this.result = (Map<String, Object>)raw.get("result");
      this.error = (Map<String, Object>)raw.get("error");
   }

   public int getId() {
      return this.id;
   }

   public boolean isError() {
      return this.error != null;
   }

   public Map<String, Object> getError() {
      return this.error;
   }

   public String getErrorMessage() {
      if (this.error == null) {
         return null;
      } else {
         Object message = this.error.get("message");
         return message != null ? message.toString() : null;
      }
   }

   public Integer getErrorCode() {
      if (this.error == null) {
         return null;
      } else {
         Object code = this.error.get("code");
         return code != null ? ((Number)code).intValue() : null;
      }
   }

   public Map<String, Object> getResult() {
      return this.result;
   }

   public Map<String, Object> getRaw() {
      return this.raw;
   }

   public <T> T getResult(String path) {
      return this.result == null ? null : this.get(this.result, path);
   }

   public String getResultAsString(String path) {
      Object value = this.getResult(path);
      return value != null ? value.toString() : null;
   }

   public Integer getResultAsInt(String path) {
      Object value = this.getResult(path);
      if (value == null) {
         return null;
      } else {
         return value instanceof Number ? ((Number)value).intValue() : Integer.parseInt(value.toString());
      }
   }

   public Boolean getResultAsBoolean(String path) {
      Object value = this.getResult(path);
      if (value == null) {
         return null;
      } else {
         return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(value.toString());
      }
   }

   public <T> T get(String path) {
      return this.get(this.raw, path);
   }

   private <T> T get(Map<String, Object> map, String path) {
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
      return this.isError() ? "CdpResponse[" + this.id + " ERROR: " + this.getErrorMessage() + "]" : "CdpResponse[" + this.id + "]";
   }
}
