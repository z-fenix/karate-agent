package io.karatelabs.driver;

import java.util.Map;

public class InterceptRequest {
   private final String requestId;
   private final String url;
   private final String method;
   private final Map<String, Object> headers;
   private final String postData;
   private final String resourceType;

   public InterceptRequest(String requestId, String url, String method, Map<String, Object> headers, String postData, String resourceType) {
      this.requestId = requestId;
      this.url = url;
      this.method = method;
      this.headers = headers;
      this.postData = postData;
      this.resourceType = resourceType;
   }

   public String getRequestId() {
      return this.requestId;
   }

   public String getUrl() {
      return this.url;
   }

   public String getMethod() {
      return this.method;
   }

   public Map<String, Object> getHeaders() {
      return this.headers;
   }

   public String getHeader(String name) {
      if (this.headers == null) {
         return null;
      } else {
         Object value = this.headers.get(name);
         return value != null ? value.toString() : null;
      }
   }

   public String getPostData() {
      return this.postData;
   }

   public String getResourceType() {
      return this.resourceType;
   }

   public boolean urlContains(String substring) {
      return this.url != null && this.url.contains(substring);
   }

   public boolean urlMatches(String pattern) {
      if (this.url != null && pattern != null) {
         String regex = pattern.replace(".", "\\.").replace("*", ".*");
         return this.url.matches(regex);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "InterceptRequest{method='" + this.method + "', url='" + this.url + "', resourceType='" + this.resourceType + "'}";
   }
}
