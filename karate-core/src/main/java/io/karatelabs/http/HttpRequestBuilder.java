package io.karatelabs.http;

import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.core.KarateConfig;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.hc.core5.net.URIBuilder;

public class HttpRequestBuilder implements SimpleObject {
   private String url;
   private String method;
   private List<String> paths;
   private Map<String, List<String>> params;
   private Map<String, List<String>> headers;
   private MultiPartBuilder multiPart;
   private Object body;
   private Set<Cookie> cookies;
   private String charset;
   private AuthHandler authHandler;
   private String retryUntil;
   private final HttpClient client;
   private Supplier<KarateConfig> configSupplier;
   private static final List<String> CURL_IGNORED_HEADERS = Arrays.asList("accept-encoding", "connection", "host", "user-agent", "content-length");

   public HttpRequestBuilder(HttpClient client) {
      this.client = client;
   }

   public void reset() {
      this.method = null;
      this.paths = null;
      this.params = null;
      this.headers = null;
      this.multiPart = null;
      this.body = null;
      this.cookies = null;
      this.charset = null;
      this.authHandler = null;
      this.retryUntil = null;
   }

   public HttpRequestBuilder configSupplier(Supplier<KarateConfig> supplier) {
      this.configSupplier = supplier;
      return this;
   }

   public HttpRequestBuilder copy() {
      HttpRequestBuilder hrb = new HttpRequestBuilder(this.client);
      hrb.configSupplier = this.configSupplier;
      hrb.url = this.url;
      hrb.method = this.method;
      hrb.paths = this.paths;
      hrb.params = this.params;
      hrb.headers = this.headers;
      hrb.multiPart = this.multiPart;
      hrb.body = this.body;
      hrb.cookies = this.cookies;
      hrb.charset = this.charset;
      hrb.authHandler = this.authHandler;
      hrb.retryUntil = this.retryUntil;
      return hrb;
   }

   public void restoreFrom(HttpRequestBuilder source) {
      this.url = source.url;
      this.method = source.method;
      this.paths = source.paths;
      this.params = source.params;
      this.headers = source.headers;
      this.multiPart = source.multiPart;
      this.body = source.body;
      this.cookies = source.cookies;
      this.charset = source.charset;
      this.authHandler = source.authHandler;
      this.retryUntil = source.retryUntil;
   }

   public HttpRequest build() {
      this.buildInternal();
      HttpRequest request = new HttpRequest();
      request.setMethod(this.method);
      request.setUrl(this.getUri());
      if (this.multiPart != null) {
         request.setBodyDisplay(this.multiPart.getBodyForDisplay());
      }

      if (this.body != null) {
         request.setBody(Json.toBytes(this.body));
      }

      request.setHeaders(this.headers);
      if (this.params != null) {
         request.setParams(this.params);
      }

      return request;
   }

   public HttpResponse invoke() {
      HttpRequest request = this.build();
      if (this.client == null) {
         throw new RuntimeException("http client not set");
      } else {
         this.reset();
         HttpResponse response = this.client.invoke(request);
         response.setRequest(request);
         if (this.configSupplier != null) {
            KarateConfig config = this.configSupplier.get();
            if (config != null) {
               response.setStrictParsing(config.isStrictResponseParsing());
            }
         }

         return response;
      }
   }

   public HttpResponse invoke(String method) {
      this.method = method;
      return this.invoke();
   }

   public HttpResponse invoke(String method, Object body) {
      this.method = method;
      this.body = body;
      return this.invoke();
   }

   public HttpRequestBuilder charset(String charset) {
      this.charset = charset;
      return this;
   }

   public HttpRequestBuilder url(String value) {
      this.url = value;
      return this;
   }

   public String getUrl() {
      return this.url;
   }

   public HttpRequestBuilder method(String method) {
      this.method = method;
      return this;
   }

   public HttpRequestBuilder paths(String... paths) {
      for (String path : paths) {
         this.path(path);
      }

      return this;
   }

   public HttpRequestBuilder path(String path) {
      if (path == null) {
         return this;
      } else {
         if (this.paths == null) {
            this.paths = new ArrayList<>();
         }

         this.paths.add(path);
         return this;
      }
   }

   public List<String> getPaths() {
      return this.paths;
   }

   public Object getBody() {
      return this.body;
   }

   public HttpRequestBuilder body(Object body) {
      this.body = body;
      return this;
   }

   public HttpRequestBuilder bodyJson(String json) {
      this.body = Json.of(json).value();
      return this;
   }

   public HttpClient getClient() {
      return this.client;
   }

   public HttpRequestBuilder forkNewBuilder() {
      return new HttpRequestBuilder(this.client);
   }

   public Map<String, String> getHeaders() {
      if (this.headers == null) {
         return new LinkedHashMap<>(0);
      } else {
         Map<String, String> map = new LinkedHashMap<>(this.headers.size());
         this.headers.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
               Object value = v.getFirst();
               if (value != null) {
                  map.put(k, value.toString());
               }
            }
         });
         return map;
      }
   }

   public List<String> getHeaderValues(String name) {
      return StringUtils.getIgnoreKeyCase(this.headers, name);
   }

   public String getHeader(String name) {
      List<String> list = this.getHeaderValues(name);
      return list != null && !list.isEmpty() ? list.getFirst() : null;
   }

   public String getContentType() {
      return this.getHeader(HttpUtils.Header.CONTENT_TYPE.key);
   }

   public HttpRequestBuilder removeHeader(String name) {
      if (this.headers != null) {
         StringUtils.removeIgnoreKeyCase(this.headers, name);
      }

      return this;
   }

   public HttpRequestBuilder header(String name, String... values) {
      return this.header(name, Arrays.asList(values));
   }

   public HttpRequestBuilder header(String name, List<String> values) {
      if (this.headers == null) {
         this.headers = new LinkedHashMap<>();
      }

      for (String key : this.headers.keySet()) {
         if (key.equalsIgnoreCase(name)) {
            name = key;
            break;
         }
      }

      this.headers.put(name, values);
      return this;
   }

   public HttpRequestBuilder header(String name, String value) {
      return this.header(name, Collections.singletonList(value));
   }

   public HttpRequestBuilder headers(Map<String, Object> map) {
      map.forEach((k, v) -> {
         if (v instanceof List) {
            this.header(k, (List<String>)v);
         } else if (v != null) {
            this.header(k, v.toString());
         }
      });
      return this;
   }

   public HttpRequestBuilder contentType(String contentType) {
      if (contentType != null) {
         this.header(HttpUtils.Header.CONTENT_TYPE.key, contentType);
      }

      return this;
   }

   public List<String> getParam(String name) {
      return this.params != null && name != null ? this.params.get(name) : null;
   }

   public HttpRequestBuilder param(String name, String... values) {
      return this.param(name, Arrays.asList(values));
   }

   public HttpRequestBuilder param(String name, List<String> values) {
      if (this.params == null) {
         this.params = new LinkedHashMap<>();
      }

      List<String> notNullValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
      if (!notNullValues.isEmpty()) {
         List<String> existing = this.params.get(name);
         if (existing == null) {
            this.params.put(name, new ArrayList<>(notNullValues));
         } else {
            existing.addAll(notNullValues);
         }
      }

      return this;
   }

   public HttpRequestBuilder params(Map<String, List<String>> params) {
      this.params = params;
      return this;
   }

   public HttpRequestBuilder cookies(Collection<Map<String, Object>> cookies) {
      for (Map<String, Object> map : cookies) {
         this.cookie(map);
      }

      return this;
   }

   public HttpRequestBuilder cookie(Map<String, Object> map) {
      return this.cookie(Cookies.fromMap(map));
   }

   public HttpRequestBuilder cookie(Cookie cookie) {
      if (this.cookies == null) {
         this.cookies = new HashSet<>();
      }

      this.cookies.add(cookie);
      return this;
   }

   public HttpRequestBuilder cookie(String name, String value) {
      return this.cookie(new DefaultCookie(name, value));
   }

   public HttpRequestBuilder formField(String name, Object value) {
      if (this.multiPart == null) {
         this.multiPart = new MultiPartBuilder(false, this.charset);
      }

      this.multiPart.part(name, value);
      return this;
   }

   public HttpRequestBuilder multiPartJson(String json) {
      return this.multiPart(Json.of(json).value());
   }

   public HttpRequestBuilder multiPart(Map<String, Object> map) {
      if (this.multiPart == null) {
         this.multiPart = new MultiPartBuilder(true, this.charset);
      }

      this.multiPart.part(map);
      return this;
   }

   public HttpRequestBuilder auth(AuthHandler authHandler) {
      this.authHandler = authHandler;
      return this;
   }

   public HttpRequestBuilder retryUntil(String condition) {
      this.retryUntil = condition;
      return this;
   }

   public String getRetryUntil() {
      return this.retryUntil;
   }

   public boolean isRetry() {
      return this.retryUntil != null;
   }

   public String getUri() {
      try {
         URIBuilder builder;
         if (this.url == null) {
            builder = new URIBuilder();
         } else {
            builder = new URIBuilder(this.url);
         }

         if (this.params != null) {
            this.params.forEach((key, values) -> values.forEach(value -> builder.addParameter(key, value)));
         }

         if (this.paths != null) {
            List<String> segments = new ArrayList<>();

            for (String item : builder.getPathSegments()) {
               if (!item.isEmpty()) {
                  segments.add(item);
               }
            }

            Iterator<String> pathIterator = this.paths.iterator();

            while (pathIterator.hasNext()) {
               String itemx = pathIterator.next();
               if (!pathIterator.hasNext() && "/".equals(itemx)) {
                  segments.add("");
               } else {
                  segments.addAll(StringUtils.split(itemx, '/', true));
               }
            }

            builder.setPathSegments(segments);
         }

         URI uri = builder.build();
         return uri.toASCIIString();
      } catch (URISyntaxException var5) {
         throw new RuntimeException(var5);
      }
   }

   private void buildInternal() {
      if (this.url == null) {
         throw new RuntimeException("incomplete http request, 'url' not set");
      } else {
         if (this.method == null) {
            if (this.multiPart != null && this.multiPart.isMultipart()) {
               this.method = "POST";
            } else {
               this.method = "GET";
            }
         }

         this.method = this.method.toUpperCase();
         if ("GET".equals(this.method) && this.multiPart != null) {
            Map<String, Object> parts = this.multiPart.getFormFields();
            if (parts != null) {
               parts.forEach((k, v) -> this.param(k, (String)v));
            }

            this.multiPart = null;
         }

         if (this.multiPart != null && this.body == null) {
            this.body = this.multiPart.build();
            String userContentType = this.getHeader(HttpUtils.Header.CONTENT_TYPE.key);
            if (userContentType != null) {
               String boundary = this.multiPart.getBoundary();
               if (boundary != null) {
                  this.contentType(userContentType + "; boundary=" + boundary);
               }
            } else {
               this.contentType(this.multiPart.getContentTypeHeader());
            }
         }

         if (this.cookies != null && !this.cookies.isEmpty()) {
            List<String> cookieValues = new ArrayList<>(this.cookies.size());

            for (Cookie c : this.cookies) {
               String cookieValue = ClientCookieEncoder.LAX.encode(c);
               cookieValues.add(cookieValue);
            }

            this.header(HttpUtils.Header.COOKIE.key, StringUtils.join(cookieValues, "; "));
         }

         if (this.body != null && this.multiPart == null) {
            String contentType = this.getContentType();
            if (contentType == null) {
               ResourceType rt = ResourceType.fromObject(this.body);
               if (rt != null) {
                  contentType = rt.contentType;
               }
            }

            Charset cs = contentType == null ? null : HttpUtils.parseContentTypeCharset(contentType);
            if (cs == null && this.charset != null) {
               contentType = StringUtils.trimToNull(contentType);
               if (contentType != null) {
                  contentType = contentType + "; charset=" + this.charset;
               }
            }

            this.contentType(contentType);
         }

         if (this.authHandler != null) {
            this.authHandler.apply(this);
         }
      }
   }

   private static String urlEncode(String value) {
      if (value == null) {
         return "";
      } else {
         try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
         } catch (Exception var2) {
            return value;
         }
      }
   }

   public String toCurlCommand() {
      return this.toCurlCommand("sh");
   }

   public String toCurlCommand(String platform) {
      this.buildInternal();
      return this.buildCurlCommand(false, platform);
   }

   public String toCurlCommandPreview() {
      return this.toCurlCommandPreview("sh");
   }

   public String toCurlCommandPreview(String platform) {
      return this.buildCurlCommand(true, platform);
   }

   private String buildCurlCommand(boolean preview, String platform) {
      if (platform == null) {
         platform = "sh";
      }

      String effectivePlatform = platform;
      String lineContinuation = StringUtils.getLineContinuation(effectivePlatform);
      StringBuilder sb = new StringBuilder();
      sb.append("curl");
      String curlMethod = this.method;
      if (curlMethod == null) {
         if (this.multiPart != null && this.multiPart.isMultipart()) {
            curlMethod = "POST";
         } else {
            curlMethod = "GET";
         }
      }

      sb.append(" -X ").append(curlMethod.toUpperCase());
      String url = this.getUri();
      if (!StringUtils.isBlank(url)) {
         sb.append(lineContinuation).append("  ").append(StringUtils.shellEscapeForPlatform(url, effectivePlatform));
      }

      String authArgument;
      if (this.authHandler != null) {
         if (preview) {
            authArgument = this.authHandler.toCurlPreview(effectivePlatform);
            if (authArgument != null) {
               sb.append(lineContinuation).append("  ").append(authArgument);
            } else if (!(this.authHandler instanceof ClientCredentialsAuthHandler)) {
               this.authHandler.apply(this);
            }
         } else {
            authArgument = this.authHandler.toCurlArgument(effectivePlatform);
            if (authArgument != null) {
               sb.append(lineContinuation).append("  ").append(authArgument);
            }
         }
      } else {
         authArgument = null;
      }

      if (this.headers != null && !this.headers.isEmpty()) {
         this.headers.forEach((namex, valuesx) -> {
            boolean skipAuth = authArgument != null && "authorization".equals(namex.toLowerCase());
            if (!skipAuth && !CURL_IGNORED_HEADERS.contains(namex.toLowerCase()) && valuesx != null && !valuesx.isEmpty()) {
               valuesx.forEach(valuex -> {
                  if (valuex != null) {
                     sb.append(lineContinuation).append("  ");
                     sb.append("-H ").append(StringUtils.shellEscapeForPlatform(namex + ": " + valuex, effectivePlatform));
                  }
               });
            }
         });
      }

      if (preview && this.authHandler != null && authArgument == null && this.authHandler instanceof ClientCredentialsAuthHandler) {
         sb.append(lineContinuation).append("  ");
         sb.append("-H ").append(StringUtils.shellEscapeForPlatform("Authorization: Bearer <your-oauth2-access-token>", effectivePlatform));
      }

      if (this.multiPart != null) {
         String multiPartCommand = this.multiPart.toCurlCommand(effectivePlatform);
         if (!multiPartCommand.isEmpty()) {
            sb.append(lineContinuation).append("  ");
            String shLineCont = StringUtils.getLineContinuation("sh");
            sb.append(multiPartCommand.replace(shLineCont, lineContinuation + "  "));
         }
      } else if (this.body != null) {
         String contentType = this.getContentType();
         sb.append(lineContinuation).append("  ");
         if (this.body instanceof byte[]) {
            sb.append("-d ").append(StringUtils.shellEscapeForPlatform("[binary data]", effectivePlatform));
         } else {
            String bodyStr = this.body instanceof String s ? s : Json.stringifyStrict(this.body);
            sb.append("-d ").append(StringUtils.shellEscapeForPlatform(bodyStr, effectivePlatform));
         }
      } else if (this.params != null && !this.params.isEmpty() && !"GET".equals(curlMethod)) {
         String contentType = this.getContentType();
         boolean isUrlEncoded = contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded");
         sb.append(lineContinuation).append("  ");
         Iterator<Entry<String, List<String>>> paramsIterator = this.params.entrySet().iterator();

         while (paramsIterator.hasNext()) {
            Entry<String, List<String>> entry = paramsIterator.next();
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
               Iterator<String> valuesIterator = values.iterator();

               while (valuesIterator.hasNext()) {
                  String value = valuesIterator.next();
                  if (value != null) {
                     if (isUrlEncoded) {
                        sb.append("--data-urlencode ").append(StringUtils.shellEscapeForPlatform(name + "=" + value, effectivePlatform));
                     } else {
                        sb.append("-d ").append(StringUtils.shellEscapeForPlatform(urlEncode(name) + "=" + urlEncode(value), effectivePlatform));
                     }

                     if (valuesIterator.hasNext() || paramsIterator.hasNext()) {
                        sb.append(lineContinuation).append("  ");
                     }
                  }
               }
            }
         }
      }

      return sb.toString();
   }

   private JavaInvokable method() {
      return args -> {
         if (args.length > 1) {
            this.body = args[1];
         }

         if (args.length > 0) {
            this.method = args[0] + "";
         }

         return this.invoke();
      };
   }

   private JavaInvokable header() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("header() needs two arguments");
         } else {
            this.header(args[0] + "", args[1] + "");
            return this;
         }
      };
   }

   private JavaInvokable param() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("param() needs two arguments");
         } else {
            this.param(args[0] + "", args[1] + "");
            return this;
         }
      };
   }

   private JavaInvokable params() {
      return args -> {
         if (args.length != 0 && args[0] instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
               if (v instanceof List) {
                  for (Object item : (List)v) {
                     if (item != null) {
                        this.param(k + "", item + "");
                     }
                  }
               } else if (v != null) {
                  this.param(k + "", v + "");
               }
            });
            return this;
         } else {
            throw new RuntimeException("params() needs a map argument");
         }
      };
   }

   private JavaInvokable headers() {
      return args -> {
         if (args.length != 0 && args[0] instanceof Map<?, ?> map) {
            this.headers((Map<String, Object>)map);
            return this;
         } else {
            throw new RuntimeException("headers() needs a map argument");
         }
      };
   }

   private JavaInvokable path() {
      return args -> {
         for (Object arg : args) {
            if (arg != null) {
               this.path(arg + "");
            }
         }

         return this;
      };
   }

   private JavaInvokable body() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("body() needs at least one argument");
         } else {
            this.body(args[0]);
            return this;
         }
      };
   }

   @Override
   public Object jsGet(String key) {
      switch (key) {
         case "get":
         case "post":
         case "put":
         case "delete":
         case "head":
         case "options":
         case "trace":
         case "connect":
         case "patch":
            return (JavaInvokable)args -> args.length > 0 ? this.invoke(key, args[0]) : this.invoke(key);
         case "method":
            return this.method();
         case "header":
            return this.header();
         case "param":
            return this.param();
         case "params":
            return this.params();
         case "headers":
            return this.headers();
         case "path":
            return this.path();
         case "body":
            return this.body();
         default:
            return null;
      }
   }
}
