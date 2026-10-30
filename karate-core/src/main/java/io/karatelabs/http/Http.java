package io.karatelabs.http;

import io.karatelabs.common.Json;
import io.karatelabs.core.KarateConfig;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http {
   private static final Logger logger = LoggerFactory.getLogger(Http.class);
   public final String urlBase;
   private final HttpClient client;
   private final HttpRequestBuilder builder;
   private final KarateConfig config = new KarateConfig();
   private boolean quiet;

   public static Http to(String url) {
      return new Http(url);
   }

   private Http(String urlBase) {
      this.urlBase = urlBase;
      this.client = new DefaultHttpClientFactory().create();
      this.builder = new HttpRequestBuilder(this.client).configSupplier(() -> this.config);
      this.builder.url(urlBase);
   }

   public Http url(String url) {
      this.builder.url(url);
      return this;
   }

   public Http param(String key, String... values) {
      this.builder.param(key, values);
      return this;
   }

   public Http path(String... paths) {
      this.builder.paths(paths);
      return this;
   }

   public Http header(String name, String value) {
      this.builder.header(name, value);
      return this;
   }

   public Http body(Object body) {
      this.builder.body(body);
      return this;
   }

   public Http contentType(String contentType) {
      this.builder.contentType(contentType);
      return this;
   }

   public Http configure(String key, Object value) {
      this.config.configure(key, value);
      this.client.apply(this.config);
      return this;
   }

   public Http configure(Map<String, Object> map) {
      map.forEach(this.config::configure);
      this.client.apply(this.config);
      return this;
   }

   public Http quiet() {
      this.quiet = true;
      return this;
   }

   public HttpResponse method(String method, Object body) {
      if (body != null) {
         this.builder.body(body instanceof Json ? ((Json)body).value() : body);
      }

      this.builder.method(method);
      HttpResponse response = this.builder.invoke();
      if (response.getStatus() >= 400) {
         if (this.quiet) {
            logger.debug("http response code: {}, response: {}, url: {}", new Object[]{response.getStatus(), response.getBodyString(), this.builder.getUri()});
         } else {
            logger.warn("http response code: {}, response: {}, url: {}", new Object[]{response.getStatus(), response.getBodyString(), this.builder.getUri()});
         }
      }

      return response;
   }

   public HttpResponse method(String method) {
      return this.method(method, null);
   }

   public HttpResponse methodJson(String method, String body) {
      return this.method(method, Json.of(body));
   }

   public HttpResponse get() {
      return this.method("GET");
   }

   public HttpResponse post(Object body) {
      return this.method("POST", body);
   }

   public HttpResponse postJson(String body) {
      return this.post(Json.of(body).value());
   }

   public HttpResponse put(Object body) {
      return this.method("PUT", body);
   }

   public HttpResponse putJson(String body) {
      return this.put(Json.of(body).value());
   }

   public HttpResponse patch(Object body) {
      return this.method("PATCH", body);
   }

   public HttpResponse patchJson(String body) {
      return this.patch(Json.of(body).value());
   }

   public HttpResponse delete() {
      return this.method("DELETE");
   }

   public HttpResponse delete(Object body) {
      return this.method("DELETE", body);
   }
}
