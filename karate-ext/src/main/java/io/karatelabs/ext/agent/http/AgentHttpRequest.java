package io.karatelabs.ext.agent.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentHttpRequest {
   private String a;
   private String b;
   private Map<String, List<String>> c;
   private byte[] d;

   public String url() {
      return this.a;
   }

   public void setUrl(String var1) {
      this.a = var1;
   }

   public String method() {
      return this.b;
   }

   public void setMethod(String var1) {
      this.b = var1;
   }

   public Map<String, List<String>> headers() {
      return this.c;
   }

   public void setHeaders(Map<String, List<String>> var1) {
      this.c = var1;
   }

   public byte[] body() {
      return this.d;
   }

   public void setBody(byte[] var1) {
      this.d = var1;
   }

   public String requestUri() {
      return this.a;
   }

   public void addHeader(String var1, String var2) {
      if (this.c == null) {
         this.c = new LinkedHashMap<>();
      }

      this.c.computeIfAbsent(var1, var0 -> new ArrayList<>()).add(var2);
   }

   public String header(String var1) {
      if (this.c == null) {
         return null;
      } else {
         var var2 = this.c.get(var1);
         return var2 != null && !var2.isEmpty() ? (String)var2.get(0) : null;
      }
   }
}
