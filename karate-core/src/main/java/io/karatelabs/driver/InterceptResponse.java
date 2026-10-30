package io.karatelabs.driver;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minidev.json.JSONValue;

public class InterceptResponse {
   private final int status;
   private final Map<String, Object> headers;
   private final byte[] body;

   private InterceptResponse(int status, Map<String, Object> headers, byte[] body) {
      this.status = status;
      this.headers = headers;
      this.body = body;
   }

   public static InterceptResponse of(int status, Map<String, Object> headers, byte[] body) {
      return new InterceptResponse(status, headers, body);
   }

   public static InterceptResponse of(int status, byte[] body) {
      return new InterceptResponse(status, new LinkedHashMap<>(), body);
   }

   public static InterceptResponse of(int status, String body) {
      return new InterceptResponse(status, new LinkedHashMap<>(), body.getBytes(StandardCharsets.UTF_8));
   }

   public static InterceptResponse json(int status, Object body) {
      Map<String, Object> headers = new LinkedHashMap<>();
      headers.put("Content-Type", "application/json");
      String json = JSONValue.toJSONString(body);
      return new InterceptResponse(status, headers, json.getBytes(StandardCharsets.UTF_8));
   }

   public static InterceptResponse json(Object body) {
      return json(200, body);
   }

   public static InterceptResponse html(int status, String html) {
      Map<String, Object> headers = new LinkedHashMap<>();
      headers.put("Content-Type", "text/html");
      return new InterceptResponse(status, headers, html.getBytes(StandardCharsets.UTF_8));
   }

   public static InterceptResponse html(String html) {
      return html(200, html);
   }

   public static InterceptResponse text(int status, String text) {
      Map<String, Object> headers = new LinkedHashMap<>();
      headers.put("Content-Type", "text/plain");
      return new InterceptResponse(status, headers, text.getBytes(StandardCharsets.UTF_8));
   }

   public static InterceptResponse text(String text) {
      return text(200, text);
   }

   public static InterceptResponse status(int status) {
      return new InterceptResponse(status, new LinkedHashMap<>(), new byte[0]);
   }

   public static InterceptResponse ok() {
      return status(200);
   }

   public static InterceptResponse notFound() {
      return status(404);
   }

   public static InterceptResponse serverError() {
      return status(500);
   }

   public static InterceptResponse redirect(String location) {
      Map<String, Object> headers = new LinkedHashMap<>();
      headers.put("Location", location);
      return new InterceptResponse(302, headers, new byte[0]);
   }

   public InterceptResponse header(String name, String value) {
      this.headers.put(name, value);
      return this;
   }

   public int getStatus() {
      return this.status;
   }

   public Map<String, Object> getHeaders() {
      return this.headers;
   }

   public byte[] getBody() {
      return this.body;
   }

   public String getBodyBase64() {
      return this.body != null && this.body.length != 0 ? Base64.getEncoder().encodeToString(this.body) : "";
   }

   public String getBodyString() {
      return this.body != null && this.body.length != 0 ? new String(this.body, StandardCharsets.UTF_8) : "";
   }
}
