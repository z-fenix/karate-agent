package io.karatelabs.http;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Pair;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.SimpleObject;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequest implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);
   private String method;
   private String urlAndPath;
   private String urlBase;
   private String path;
   private String pathOriginal;
   private Map<String, List<String>> params;
   private Map<String, List<String>> headers;
   private byte[] body;
   private String bodyDisplay;
   private ResourceType resourceType;
   private Map<String, String> pathParams;
   private String pathPattern;
   private Map<String, List<Map<String, Object>>> multiParts;
   private Map<String, Object> cacheStore;
   private transient HttpClient httpClient;

   public void setHttpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
   }

   public void setUrl(String url) {
      this.urlAndPath = url;
      Pair<String> pair = HttpUtils.parseUriIntoUrlBaseAndPath(url);
      this.urlBase = pair.left;
      QueryStringDecoder qsd = new QueryStringDecoder(pair.right);
      this.setPath(qsd.path());
      this.setParams(qsd.parameters());
   }

   public String getUrlAndPath() {
      return this.urlAndPath != null ? this.urlAndPath : (this.urlBase != null ? this.urlBase : "") + this.path;
   }

   public void setPath(String path) {
      if (path == null || path.isEmpty()) {
         path = "/";
      }

      if (path.charAt(0) != '/') {
         path = "/" + path;
      }

      this.path = path;
      if (this.pathOriginal == null) {
         this.pathOriginal = path;
      }
   }

   public String getUrlBase() {
      return this.urlBase;
   }

   public void setUrlBase(String urlBase) {
      this.urlBase = urlBase;
   }

   public void setParams(Map<String, List<String>> params) {
      this.params = params;
   }

   public Map<String, List<String>> getParams() {
      return this.params;
   }

   public String getPath() {
      return this.path;
   }

   public String getPathRaw() {
      if (this.urlBase != null && this.urlAndPath != null) {
         return this.urlAndPath.charAt(0) == '/' ? this.urlAndPath : this.urlAndPath.substring(this.urlBase.length());
      } else {
         return this.path;
      }
   }

   public boolean pathMatches(String pattern) {
      Map<String, String> temp = HttpUtils.parseUriPattern(pattern, this.path);
      if (temp == null) {
         return false;
      } else {
         this.pathParams = temp;
         this.pathPattern = pattern;
         return true;
      }
   }

   public Map<String, String> getPathParams() {
      return this.pathParams == null ? Collections.emptyMap() : this.pathParams;
   }

   public void putHeader(String name, String... values) {
      this.putHeader(name, Arrays.asList(values));
   }

   public void putHeader(String name, List<String> values) {
      if (this.headers == null) {
         this.headers = new HashMap<>();
      }

      for (String key : this.headers.keySet()) {
         if (key.equalsIgnoreCase(name)) {
            name = key;
            break;
         }
      }

      this.headers.put(name, values);
   }

   public String getMethod() {
      return this.method;
   }

   public void setMethod(String method) {
      this.method = method.toUpperCase();
   }

   public Map<String, List<String>> getHeaders() {
      return this.headers;
   }

   public void setHeaders(Map<String, List<String>> headers) {
      this.headers = headers;
   }

   public byte[] getBody() {
      return this.body;
   }

   public String getBodyString() {
      return FileUtils.toString(this.body);
   }

   public Object getBodyJson() {
      Object converted = this.getBodyConverted();
      if (!(converted instanceof Map) && !(converted instanceof List)) {
         if (converted instanceof String s && !s.isBlank()) {
            try {
               return Json.parseLenient(s);
            } catch (RuntimeException var4) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return converted;
      }
   }

   public void setBody(byte[] body) {
      this.body = body;
   }

   public String getBodyDisplay() {
      return this.bodyDisplay;
   }

   public void setBodyDisplay(String bodyDisplay) {
      this.bodyDisplay = bodyDisplay;
   }

   public List<String> getHeaderValues(String name) {
      return StringUtils.getIgnoreKeyCase(this.headers, name);
   }

   public List<String> getParamValues(String name) {
      return this.params == null ? null : this.params.get(name);
   }

   public String getParam(String name) {
      List<String> values = this.getParamValues(name);
      return values != null && !values.isEmpty() ? values.getFirst() : null;
   }

   public void removeHeader(String name) {
      if (this.headers != null) {
         for (String key : this.headers.keySet()) {
            if (key.equalsIgnoreCase(name)) {
               name = key;
               break;
            }
         }

         this.headers.remove(name);
      }
   }

   public String getHeader(String name) {
      List<String> values = this.getHeaderValues(name);
      return values != null && !values.isEmpty() ? values.getLast() : null;
   }

   public String getContentType() {
      return this.getHeader(HttpUtils.Header.CONTENT_TYPE.key);
   }

   public Map<String, Map<String, String>> getCookies() {
      List<String> cookieHeaders = this.getHeaderValues("Cookie");
      if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
         Map<String, Map<String, String>> cookies = new LinkedHashMap<>();

         for (String cookieHeader : cookieHeaders) {
            for (String part : cookieHeader.split(";")) {
               String trimmed = part.trim();
               int eqPos = trimmed.indexOf(61);
               if (eqPos > 0) {
                  String name = trimmed.substring(0, eqPos).trim();
                  String value = trimmed.substring(eqPos + 1).trim();
                  Map<String, String> cookie = new LinkedHashMap<>();
                  cookie.put("name", name);
                  cookie.put("value", value);
                  cookies.put(name, cookie);
               }
            }
         }

         return cookies;
      } else {
         return Collections.emptyMap();
      }
   }

   public void setContentType(String contentType) {
      this.putHeader(HttpUtils.Header.CONTENT_TYPE.key, contentType);
   }

   public ResourceType getResourceType() {
      if (this.resourceType == null) {
         String contentType = this.getContentType();
         if (contentType != null) {
            this.resourceType = ResourceType.fromContentType(contentType);
         }
      }

      return this.resourceType;
   }

   public boolean isAjax() {
      return this.getHeader("HX-Request") != null;
   }

   public boolean isMultiPart() {
      return this.multiParts != null;
   }

   public Map<String, List<Map<String, Object>>> getMultiParts() {
      return this.multiParts;
   }

   public Map<String, Object> getMultiPart(String name) {
      if (this.multiParts == null) {
         return null;
      } else {
         List<Map<String, Object>> parts = this.multiParts.get(name);
         return parts != null && !parts.isEmpty() ? parts.get(0) : null;
      }
   }

   public Map<String, Object> getFile(String name) {
      return this.getMultiPart(name);
   }

   public List<Map<String, Object>> getFiles(String name) {
      if (this.multiParts == null) {
         return Collections.emptyList();
      } else {
         List<Map<String, Object>> parts = this.multiParts.get(name);
         return parts != null ? parts : Collections.emptyList();
      }
   }

   public void processBody() {
      if (this.body != null) {
         String contentType = this.getContentType();
         if (contentType != null) {
            boolean multipart;
            if (contentType.startsWith("multipart")) {
               multipart = true;
               this.multiParts = new HashMap<>();
            } else {
               if (!contentType.contains("form-urlencoded")) {
                  return;
               }

               multipart = false;
            }

            logger.trace("decoding content-type: {}", contentType);
            this.params = this.params != null && !this.params.isEmpty() ? new HashMap<>(this.params) : new HashMap<>();
            DefaultFullHttpRequest nettyRequest = new DefaultFullHttpRequest(
               HttpVersion.HTTP_1_1, HttpMethod.valueOf(this.method), this.path, Unpooled.wrappedBuffer(this.body)
            );
            nettyRequest.headers().add("Content-Type", contentType);
            InterfaceHttpPostRequestDecoder decoder = (InterfaceHttpPostRequestDecoder)(multipart
               ? new HttpPostMultipartRequestDecoder(nettyRequest)
               : new HttpPostStandardRequestDecoder(nettyRequest));

            try {
               for (InterfaceHttpData part : decoder.getBodyHttpDatas()) {
                  String name = part.getName();
                  if (multipart && part instanceof FileUpload fup) {
                     String filename = fup.getFilename();
                     if (filename != null && !filename.isEmpty()) {
                        List<Map<String, Object>> list = this.multiParts.computeIfAbsent(name, k -> new ArrayList<>());
                        Map<String, Object> map = new HashMap<>();
                        list.add(map);
                        map.put("name", name);
                        map.put("filename", filename);
                        Charset charset = fup.getCharset();
                        if (charset != null) {
                           map.put("charset", charset.name());
                        }

                        map.put("contentType", fup.getContentType());
                        byte[] bytes = fup.get();
                        map.put("value", bytes);
                        map.put("bytes", bytes);
                        map.put("size", bytes != null ? bytes.length : 0);
                        String transferEncoding = fup.getContentTransferEncoding();
                        if (transferEncoding != null) {
                           map.put("transferEncoding", transferEncoding);
                        }
                     } else {
                        List<String> listx = this.params.computeIfAbsent(name, k -> new ArrayList<>());
                        listx.add(fup.getString(fup.getCharset()));
                     }
                  } else {
                     Attribute attribute = (Attribute)part;
                     List<String> listx = this.params.computeIfAbsent(name, k -> new ArrayList<>());
                     listx.add(attribute.getValue());
                  }
               }
            } catch (Exception var18) {
               throw new RuntimeException(var18);
            } finally {
               decoder.destroy();
            }
         }
      }
   }

   public Object getBodyConverted() {
      ResourceType rt = this.getResourceType();
      return rt != null && rt.isBinary() ? this.body : HttpUtils.fromBytes(this.body, false, rt);
   }

   public Map<String, Object> toBlockData() {
      Map<String, Object> request = new HashMap<>();
      request.put("method", this.method);
      request.put("url", this.urlBase);
      if (this.path != null) {
         request.put("path", this.path);
      }

      List<Map<String, Object>> paramsList = new ArrayList<>();
      request.put("params", paramsList);
      if (this.params != null) {
         for (String name : this.params.keySet()) {
            List<String> values = this.params.get(name);
            if (values != null && !values.isEmpty()) {
               Map<String, Object> map = new HashMap<>();
               paramsList.add(map);
               map.put("name", name);
               map.put("value", String.join(",", values));
            }
         }
      }

      List<Map<String, Object>> headersList = new ArrayList<>();
      request.put("headers", headersList);
      if (this.headers != null) {
         for (String namex : this.headers.keySet()) {
            List<String> values = this.headers.get(namex);
            if (values != null && !values.isEmpty()) {
               Map<String, Object> map = new HashMap<>();
               headersList.add(map);
               map.put("name", namex);
               map.put("value", String.join(",", values));
            }
         }
      }

      if (this.body != null) {
         request.put("body", this.getBodyConverted());
      }

      Map<String, Object> data = new HashMap<>();
      data.put("request", request);
      return data;
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("url", this.urlAndPath);
      map.put("urlBase", this.urlBase);
      map.put("path", this.path);
      map.put("pathRaw", this.getPathRaw());
      map.put("method", this.method);
      map.put("headers", this.headers);
      map.put("params", this.params);
      map.put("body", this.getBodyConverted());
      return map;
   }

   @Override
   public String toString() {
      return this.method + " " + this.urlAndPath;
   }

   public HttpRequestBuilder toHttpRequestBuilder(HttpClient client) {
      HttpRequestBuilder builder = new HttpRequestBuilder(client);
      builder.method(this.method);
      if (this.urlBase != null) {
         builder.url(this.urlBase);
      }

      if (this.path != null) {
         builder.path(this.path);
      }

      if (this.params != null) {
         builder.params(this.params);
      }

      if (this.headers != null) {
         this.headers.forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
               builder.header(name, (List<String>)values);
            }
         });
      }

      if (this.body != null) {
         builder.body(this.getBodyConverted());
      }

      return builder;
   }

   private JavaInvokable param() {
      return args -> {
         if (args.length > 0) {
            return this.getParam(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for param()");
         }
      };
   }

   private JavaInvokable paramValues() {
      return args -> {
         if (args.length > 0) {
            return this.getParamValues(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for paramValues()");
         }
      };
   }

   private JavaInvokable paramInt() {
      return args -> {
         if (args.length > 0) {
            String val = this.getParam(args[0] + "");
            return val == null ? null : Integer.parseInt(val);
         } else {
            throw new RuntimeException("missing argument for paramInt()");
         }
      };
   }

   private JavaInvokable paramJson() {
      return args -> {
         if (args.length > 0) {
            String val = this.getParam(args[0] + "");
            if (val == null) {
               return null;
            } else {
               try {
                  return Json.of(val).value();
               } catch (Exception var4) {
                  return val;
               }
            }
         } else {
            throw new RuntimeException("missing argument for paramJson()");
         }
      };
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

   void putHeaderValue(String name, Object value) {
      if (value == null) {
         this.removeHeader(name);
      } else if (value instanceof List<?> list) {
         List<String> strList = new ArrayList<>(list.size());

         for (Object o : list) {
            strList.add(o == null ? null : o.toString());
         }

         this.putHeader(name, strList);
      } else {
         this.putHeader(name, value.toString());
      }
   }

   private ObjectLike headersView() {
      return new ObjectLike() {
         @Override
         public Object getMember(String key) {
            return HttpRequest.this.getHeaderValues(key);
         }

         @Override
         public void putMember(String key, Object value) {
            HttpRequest.this.putHeaderValue(key, value);
         }

         @Override
         public void removeMember(String key) {
            HttpRequest.this.removeHeader(key);
         }

         @Override
         public Map<String, Object> toMap() {
            return (Map<String, Object>)(HttpRequest.this.headers == null ? Collections.emptyMap() : new LinkedHashMap<>(HttpRequest.this.headers));
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

   private JavaInvokable pathMatches() {
      return args -> {
         if (args.length > 0) {
            return this.pathMatches(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for pathMatches()");
         }
      };
   }

   private JavaInvokable multiPart() {
      return args -> {
         if (args.length > 0) {
            return this.getMultiPart(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for multiPart()");
         }
      };
   }

   private JavaInvokable file() {
      return args -> {
         if (args.length > 0) {
            return this.getFile(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for file()");
         }
      };
   }

   private JavaInvokable files() {
      return args -> {
         if (args.length > 0) {
            return this.getFiles(args[0] + "");
         } else {
            throw new RuntimeException("missing argument for files()");
         }
      };
   }

   public HttpResponse proceed(String targetUrl) {
      if (targetUrl == null) {
         String host = this.getHeader("Host");
         if (host == null) {
            throw new RuntimeException("proceed() needs a target URL or Host header in request");
         }

         targetUrl = "http://" + host;
      }

      HttpClient client = this.httpClient != null ? this.httpClient : new DefaultHttpClientFactory().create();
      HttpRequestBuilder builder = new HttpRequestBuilder(client);
      builder.url(targetUrl);
      builder.path(this.path);
      builder.method(this.method);
      if (this.headers != null) {
         this.headers.forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            if (!lowerName.equals("content-length") && !lowerName.equals("host") && !lowerName.equals("transfer-encoding")) {
               builder.header(name, (List<String>)values);
            }
         });
      }

      Object converted = this.getBodyConverted();
      if (converted != null) {
         builder.body(converted);
      }

      return builder.invoke();
   }

   private JavaInvokable proceed() {
      return args -> {
         String targetUrl = args.length > 0 && args[0] != null ? args[0].toString() : null;
         return this.proceed(targetUrl);
      };
   }

   private JavaCallable cache() {
      return (context, args) -> {
         if (args.length < 2) {
            throw new RuntimeException("cache() requires (key, fn)");
         } else {
            String key = args[0] + "";
            if (this.cacheStore != null && this.cacheStore.containsKey(key)) {
               return this.cacheStore.get(key);
            } else if (args[1] instanceof JavaCallable fn) {
               Object var6 = fn.call(context);
               if (this.cacheStore == null) {
                  this.cacheStore = new HashMap<>();
               }

               this.cacheStore.put(key, var6);
               return var6;
            } else {
               throw new RuntimeException("cache() second argument must be a function: " + key);
            }
         }
      };
   }

   @Override
   public Object jsGet(String key) {
      switch (key) {
         case "method":
            return this.method;
         case "body":
            return this.getBodyConverted();
         case "bodyString":
            return this.getBodyString();
         case "bodyJson":
            return this.getBodyJson();
         case "bodyBytes":
            return this.body;
         case "url":
            return this.urlAndPath;
         case "urlBase":
            return this.urlBase;
         case "path":
            return this.path;
         case "pathRaw":
            return this.getPathRaw();
         case "params":
            return StringUtils.simplify(this.params, true);
         case "param":
            return this.param();
         case "paramInt":
            return this.paramInt();
         case "paramJson":
            return this.paramJson();
         case "paramValues":
            return this.paramValues();
         case "headers":
            return this.headersView();
         case "header":
            return this.header();
         case "headerValues":
            return this.headerValues();
         case "cookies":
            return this.getCookies();
         case "pathMatches":
            return this.pathMatches();
         case "pathParams":
            return this.getPathParams();
         case "multiPart":
            return this.multiPart();
         case "multiParts":
            return this.multiParts;
         case "file":
            return this.file();
         case "files":
            return this.files();
         case "proceed":
            return this.proceed();
         case "cache":
            return this.cache();
         case "get":
         case "post":
         case "put":
         case "delete":
         case "patch":
         case "head":
         case "options":
         case "trace":
            return this.method.toLowerCase().equals(key);
         case "ajax":
            return this.isAjax();
         default:
            logger.warn("get - unexpected key: {}", key);
            return null;
      }
   }
}
