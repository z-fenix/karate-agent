package io.karatelabs.http;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.ObjectLike;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class HttpResponse implements ObjectLike {
   private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);
   private int status = 200;
   private String statusText;
   private Map<String, List<String>> headers;
   private byte[] body;
   private ResourceType resourceType;
   private long startTime;
   private long responseTime;
   private int contentLength;
   private HttpRequest request;
   private int delay;
   private boolean resetConnection;
   private boolean strictParsing;
   private Object bodyConverted;
   private byte[] bodyConvertedBytes;

   public ResourceType getResourceType() {
      if (this.resourceType == null) {
         String contentType = this.getContentType();
         if (contentType != null) {
            this.resourceType = ResourceType.fromContentType(contentType);
         }
      }

      return this.resourceType;
   }

   public String getContentType() {
      return this.getHeader(HttpUtils.Header.CONTENT_TYPE.key);
   }

   public String getHeader(String name) {
      List<String> values = this.getHeaderValues(name);
      return values != null && !values.isEmpty() ? values.getLast() : null;
   }

   public List<String> getHeaderValues(String name) {
      return StringUtils.getIgnoreKeyCase(this.headers, name);
   }

   public int getStatus() {
      return this.status;
   }

   public void setStatus(int status) {
      this.status = status;
   }

   public void setStatus(int status, String statusText) {
      this.status = status;
      this.statusText = statusText;
   }

   public void setContentLength(int contentLength) {
      this.contentLength = contentLength;
   }

   public int getContentLength() {
      return this.contentLength;
   }

   public String getStatusText() {
      return this.statusText;
   }

   public void setStatusText(String statusText) {
      this.statusText = statusText;
   }

   public Map<String, List<String>> getHeaders() {
      return this.headers;
   }

   public void setContentType(String contentType) {
      this.setHeader(HttpUtils.Header.CONTENT_TYPE.key, contentType);
   }

   public void setHeader(String name, List<String> values) {
      if (this.headers == null) {
         this.headers = new HashMap<>();
      } else {
         this.removeHeader(name);
      }

      this.headers.put(name, values);
   }

   public HttpRequest getRequest() {
      return this.request;
   }

   public void setRequest(HttpRequest request) {
      this.request = request;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void setHeader(String name, String... values) {
      this.setHeader(name, Arrays.asList(values));
   }

   public void removeHeader(String name) {
      if (this.headers != null) {
         this.headers.entrySet().removeIf(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name));
      }
   }

   public void setHeaders(Map<String, ?> map) {
      if (map != null) {
         map.forEach((k, v) -> {
            if (v instanceof List) {
               this.setHeader(k, (List<String>)v);
            } else if (v != null) {
               this.setHeader(k, v.toString());
            }
         });
      }
   }

   public Object getBody() {
      this.syncBody();
      return this.body;
   }

   public byte[] getBodyBytes() {
      this.syncBody();
      return Json.toBytes(this.body);
   }

   public String getBodyString() {
      this.syncBody();
      return FileUtils.toString(Json.toBytes(this.body));
   }

   private void syncBody() {
      if (this.bodyConverted != null) {
         byte[] now = Json.toBytes(this.bodyConverted);
         if (!Arrays.equals(now, this.bodyConvertedBytes)) {
            this.body = now;
            this.bodyConvertedBytes = now;
         }
      }
   }

   private void invalidateBodyConverted() {
      this.bodyConverted = null;
      this.bodyConvertedBytes = null;
   }

   public void setBody(byte[] body, ResourceType type) {
      this.body = body;
      this.resourceType = type;
      this.invalidateBodyConverted();
      if (type != null) {
         this.setContentType(applyCharset(type));
      }
   }

   private static String applyCharset(ResourceType type) {
      return switch (type) {
         case TEXT, HTML, XML, JSON, JS, CSS, CSV, YAML, TOML, MARKDOWN -> type.contentType + "; charset=UTF-8";
         default -> type.contentType;
      };
   }

   public void setBody(String body, ResourceType type) {
      this.setBody(FileUtils.toBytes(body), type);
   }

   public Object getBodyConverted() {
      ResourceType rt = this.getResourceType();
      if (rt != null && rt.isBinary()) {
         return this.body;
      } else if (this.bodyConverted != null) {
         return this.bodyConverted;
      } else {
         Object converted = this.strictParsing ? HttpUtils.fromBytesDeclared(this.body, rt) : HttpUtils.fromBytes(this.body, false, rt);
         if (converted instanceof Map || converted instanceof List || converted instanceof Node) {
            this.bodyConverted = converted;
            this.bodyConvertedBytes = Json.toBytes(converted);
         }

         return converted;
      }
   }

   public boolean isStrictParsing() {
      return this.strictParsing;
   }

   public void setStrictParsing(boolean strictParsing) {
      this.strictParsing = strictParsing;
      this.invalidateBodyConverted();
   }

   public long getResponseTime() {
      return this.responseTime;
   }

   public void setResponseTime(long responseTime) {
      this.responseTime = responseTime;
   }

   public void setResourceType(ResourceType resourceType) {
      this.resourceType = resourceType;
      this.invalidateBodyConverted();
   }

   public int getDelay() {
      return this.delay;
   }

   public void setDelay(int delay) {
      this.delay = delay;
   }

   public boolean isResetConnection() {
      return this.resetConnection;
   }

   public void setResetConnection(boolean resetConnection) {
      this.resetConnection = resetConnection;
   }

   public Map<String, Map<String, Object>> getCookies() {
      List<String> setCookieHeaders = this.getHeaderValues(HttpUtils.Header.SET_COOKIE.key);
      if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
         Map<String, Map<String, Object>> cookies = new LinkedHashMap<>();

         for (String header : setCookieHeaders) {
            Cookie cookie = ClientCookieDecoder.LAX.decode(header);
            if (cookie != null) {
               cookies.put(cookie.name(), Cookies.toMap(cookie));
            }
         }

         return cookies;
      } else {
         return Collections.emptyMap();
      }
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("status", this.status);
      map.put("headers", this.headers);
      map.put("body", this.getBodyConverted());
      map.put("responseTime", this.responseTime);
      map.put("contentLength", this.contentLength);
      return map;
   }

   public static HttpResponse skipped(HttpRequest request) {
      HttpResponse response = new HttpResponse();
      response.setRequest(request);
      response.setStatus(0);
      response.setHeaders(Collections.emptyMap());
      return response;
   }

   public boolean isSkipped() {
      return this.status == 0;
   }

   private JavaInvokable header() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("missing argument for header()");
         } else {
            String name = args[0] + "";
            if (args.length == 1) {
               return this.getHeader(name);
            } else {
               this.putHeaderValue(name, args[1]);
               return null;
            }
         }
      };
   }

   private JavaInvokable headerValues() {
      return args -> {
         if (args.length > 0) {
            return this.getHeaderValues(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for headerValues()");
         }
      };
   }

   void putHeaderValue(String name, Object value) {
      if (value == null) {
         this.removeHeader(name);
      } else if (value instanceof List<?> list) {
         List<String> strList = new ArrayList<>(list.size());

         for (Object o : list) {
            strList.add(o == null ? null : o.toString());
         }

         this.setHeader(name, strList);
      } else {
         this.setHeader(name, value.toString());
      }
   }

   private ObjectLike headersView() {
      return new ObjectLike() {
         @Override
         public Object getMember(String name) {
            return HttpResponse.this.getHeaderValues(name);
         }

         @Override
         public void putMember(String name, Object value) {
            HttpResponse.this.putHeaderValue(name, value);
         }

         @Override
         public void removeMember(String name) {
            HttpResponse.this.removeHeader(name);
         }

         @Override
         public Map<String, Object> toMap() {
            return (Map<String, Object>)(HttpResponse.this.headers == null ? Collections.emptyMap() : new LinkedHashMap<>(HttpResponse.this.headers));
         }
      };
   }

   @Override
   public Object getMember(String key) {
      return switch (key) {
         case "status" -> this.status;
         case "statusText" -> this.statusText;
         case "startTime" -> this.startTime;
         case "responseTime" -> this.responseTime;
         case "headers" -> this.headersView();
         case "header" -> this.header();
         case "headerValues" -> this.headerValues();
         case "body" -> this.getBodyConverted();
         case "bodyString" -> this.getBodyString();
         case "bodyBytes" -> this.getBodyBytes();
         case "request" -> this.request;
         default -> null;
      };
   }

   @Override
   public void putMember(String key, Object value) {
      switch (key) {
         case "body":
            applyJsBody(this, value);
            break;
         case "status":
            this.status = ((Number)value).intValue();
            break;
         case "headers":
            this.setHeaders((Map<String, ?>)value);
            break;
         default:
            logger.warn("put - unexpected key: {}", key);
      }
   }

   public void setBodyDynamic(Object value) {
      applyJsBody(this, value);
   }

   private static void applyJsBody(HttpResponse r, Object value) {
      if (value == null) {
         r.setBody((byte[])null, null);
      } else if (value instanceof byte[] bytes) {
         r.setBody(bytes, null);
      } else if (value instanceof String s) {
         r.setBody(s, ResourceType.TEXT);
      } else if (value instanceof Node xml) {
         r.setBody(Xml.toString(xml), ResourceType.XML);
      } else {
         if (!(value instanceof Map) && !(value instanceof List)) {
            throw new RuntimeException("unsupported response body type: " + value);
         }

         r.setBody(FileUtils.toBytes(JSONValue.toJSONString(value)), ResourceType.JSON);
      }
   }

   public static HttpResponse ok() {
      HttpResponse r = new HttpResponse();
      r.setStatus(200);
      return r;
   }

   public static HttpResponse ok(Object body) {
      HttpResponse r = ok();
      applyJsBody(r, body);
      return r;
   }

   public static HttpResponse text(String body) {
      return text(200, body);
   }

   public static HttpResponse text(int status, String body) {
      HttpResponse r = new HttpResponse();
      r.setStatus(status);
      r.setBody(body, ResourceType.TEXT);
      return r;
   }

   public static HttpResponse json(Object body) {
      return json(200, body);
   }

   public static HttpResponse json(int status, Object body) {
      HttpResponse r = new HttpResponse();
      r.setStatus(status);
      if (body == null) {
         r.setBody(new byte[0], ResourceType.JSON);
      } else if (body instanceof String s) {
         r.setBody(s, ResourceType.JSON);
      } else if (body instanceof byte[] bytes) {
         r.setBody(bytes, ResourceType.JSON);
      } else {
         r.setBody(Json.toBytes(body), ResourceType.JSON);
      }

      return r;
   }

   public static HttpResponse html(String body) {
      HttpResponse r = new HttpResponse();
      r.setStatus(200);
      r.setBody(body, ResourceType.HTML);
      return r;
   }

   public static HttpResponse xml(String body) {
      HttpResponse r = new HttpResponse();
      r.setStatus(200);
      r.setBody(body, ResourceType.XML);
      return r;
   }

   public static HttpResponse binary(byte[] body) {
      HttpResponse r = new HttpResponse();
      r.setStatus(200);
      r.setBody(body, ResourceType.BINARY);
      return r;
   }

   public static HttpResponse noContent() {
      HttpResponse r = new HttpResponse();
      r.setStatus(204);
      return r;
   }

   public static HttpResponse redirect(String location) {
      return redirect(302, location);
   }

   public static HttpResponse redirect(int status, String location) {
      HttpResponse r = new HttpResponse();
      r.setStatus(status);
      r.setHeader(HttpUtils.Header.LOCATION.key, location);
      return r;
   }

   public static HttpResponse error(int status, String message) {
      HttpResponse r = new HttpResponse();
      r.setStatus(status);
      r.setBody(Json.toBytes(Map.of("error", message == null ? "" : message)), ResourceType.JSON);
      return r;
   }

   public static HttpResponse badRequest(String message) {
      return error(400, message);
   }

   public static HttpResponse notFound(String message) {
      return error(404, message);
   }

   @Override
   public void removeMember(String name) {
      logger.warn("remove() not implemented");
   }
}
