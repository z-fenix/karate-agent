package io.karatelabs.ext.agent.http;

import io.karatelabs.common.Json;
import java.nio.charset.StandardCharsets;

public class AgentHttpRequestBuilder {
   private final AgentHttpRequest a = new AgentHttpRequest();
   private Object b;

   public AgentHttpRequestBuilder url(String var1) {
      this.a.setUrl(var1);
      return this;
   }

   public AgentHttpRequestBuilder method(String var1) {
      this.a.setMethod(var1);
      return this;
   }

   public AgentHttpRequestBuilder header(String var1, String var2) {
      this.a.addHeader(var1, var2);
      return this;
   }

   public AgentHttpRequestBuilder contentType(String var1) {
      return this.header("Content-Type", var1);
   }

   public AgentHttpRequestBuilder authorization(String var1) {
      return this.header("Authorization", var1);
   }

   public AgentHttpRequestBuilder bearerToken(String var1) {
      return this.authorization("Bearer " + var1);
   }

   public AgentHttpRequestBuilder body(Object var1) {
      this.b = var1;
      return this;
   }

   public AgentHttpRequestBuilder json(String var1) {
      this.a.setBody(var1.getBytes(StandardCharsets.UTF_8));
      return this.contentType("application/json");
   }

   public AgentHttpRequest build() {
      if (this.a.method() == null) {
         this.a.setMethod("GET");
      }

      if (this.b != null) {
         String var1 = Json.stringifyStrict(this.b);
         this.a.setBody(var1.getBytes(StandardCharsets.UTF_8));
         if (this.a.header("Content-Type") == null) {
            this.a.addHeader("Content-Type", "application/json");
         }
      }

      return this.a;
   }

   public static AgentHttpRequestBuilder post(String var0) {
      return new AgentHttpRequestBuilder().method("POST").url(var0);
   }

   public static AgentHttpRequestBuilder get(String var0) {
      return new AgentHttpRequestBuilder().method("GET").url(var0);
   }
}
