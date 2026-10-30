package io.karatelabs.driver.cdp;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

public class CdpMessage {
   private final CdpClient client;
   private final int id;
   private final String method;
   private Map<String, Object> params;
   private Duration timeout;
   private String sessionId;
   private static final JSONStyle JSON_STYLE = new JSONStyle(2);

   public CdpMessage(CdpClient client, int id, String method) {
      this.client = client;
      this.id = id;
      this.method = method;
   }

   public CdpMessage param(String key, Object value) {
      if (this.params == null) {
         this.params = new LinkedHashMap<>();
      }

      this.params.put(key, value);
      return this;
   }

   public CdpMessage params(Map<String, Object> params) {
      if (params != null) {
         if (this.params == null) {
            this.params = new LinkedHashMap<>(params);
         } else {
            this.params.putAll(params);
         }
      }

      return this;
   }

   public CdpMessage timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
   }

   public CdpMessage timeout(long millis) {
      this.timeout = Duration.ofMillis(millis);
      return this;
   }

   public CdpMessage sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
   }

   public CdpResponse send() {
      return this.client.send(this);
   }

   public CompletableFuture<CdpResponse> sendAsync() {
      return this.client.sendAsync(this);
   }

   public void sendWithoutWaiting() {
      this.client.sendWithoutWaiting(this);
   }

   public int getId() {
      return this.id;
   }

   public String getMethod() {
      return this.method;
   }

   public Map<String, Object> getParams() {
      return this.params;
   }

   public Duration getTimeout() {
      return this.timeout;
   }

   public String getSessionId() {
      return this.sessionId;
   }

   public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", this.id);
      map.put("method", this.method);
      if (this.params != null && !this.params.isEmpty()) {
         map.put("params", this.params);
      }

      if (this.sessionId != null) {
         map.put("sessionId", this.sessionId);
      }

      return map;
   }

   public String toJson() {
      return JSONValue.toJSONString(this.toMap(), JSON_STYLE);
   }

   @Override
   public String toString() {
      return "CdpMessage[" + this.id + ": " + this.method + "]";
   }
}
