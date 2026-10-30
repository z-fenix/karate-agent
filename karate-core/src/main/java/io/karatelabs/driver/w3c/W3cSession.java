package io.karatelabs.driver.w3c;

import io.karatelabs.common.Json;
import io.karatelabs.driver.DriverException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class W3cSession {
   private static final Logger logger = LoggerFactory.getLogger(W3cSession.class);
   static final String W3C_ELEMENT_KEY = "element-6066-11e4-a52e-4f735466cecf";
   private final HttpClient httpClient;
   private final String baseUrl;
   private final String sessionId;
   private final Duration timeout;

   private W3cSession(HttpClient httpClient, String baseUrl, String sessionId, Duration timeout) {
      this.httpClient = httpClient;
      this.baseUrl = baseUrl;
      this.sessionId = sessionId;
      this.timeout = timeout;
   }

   public static W3cSession create(String baseUrl, Map<String, Object> sessionPayload, Duration timeout) {
      HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
      if (baseUrl.endsWith("/")) {
         baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }

      String json = Json.of(sessionPayload).toString();
      logger.debug("Creating W3C session at {} with payload: {}", baseUrl, json);
      URI uri = URI.create(baseUrl + "/session");
      Builder requestBuilder = HttpRequest.newBuilder().uri(uri).header("Content-Type", "application/json; charset=utf-8").timeout(timeout);
      String userInfo = uri.getUserInfo();
      if (userInfo != null && !userInfo.isBlank()) {
         String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userInfo.getBytes());
         requestBuilder.header("Authorization", basicAuth);
      }

      HttpRequest request = requestBuilder.POST(BodyPublishers.ofString(json)).build();

      try {
         HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
         if (response.statusCode() != 200) {
            throw new DriverException("WebDriver session create failed with status " + response.statusCode() + ": " + response.body());
         } else {
            Map<String, Object> body = Json.of(response.body()).asMap();
            Map<String, Object> value = (Map<String, Object>)body.get("value");
            String sessionId = (String)value.get("sessionId");
            if (sessionId == null) {
               throw new DriverException("WebDriver session create response missing sessionId: " + response.body());
            } else {
               logger.info("W3C session created: {}", sessionId);
               return new W3cSession(client, baseUrl, sessionId, timeout);
            }
         }
      } catch (InterruptedException | IOException var13) {
         if (var13 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         throw new DriverException("WebDriver session create failed: " + var13.getMessage(), var13);
      }
   }

   public void navigateTo(String url) {
      this.post("url", Map.of("url", url));
   }

   public String getUrl() {
      return (String)getValue(this.get("url"));
   }

   public String getTitle() {
      return (String)getValue(this.get("title"));
   }

   public void back() {
      this.post("back", Map.of());
   }

   public void forward() {
      this.post("forward", Map.of());
   }

   public void refresh() {
      this.post("refresh", Map.of());
   }

   public Object executeScript(String script, Object... args) {
      List<Object> argsList = new ArrayList<>();
      Collections.addAll(argsList, args);
      Map<String, Object> payload = Map.of("script", script, "args", argsList);
      return getValue(this.post("execute/sync", payload));
   }

   public String findElement(String using, String value) {
      Map<String, Object> payload = Map.of("using", using, "value", value);
      Map<String, Object> result = (Map<String, Object>)getValue(this.post("element", payload));
      return extractElementId(result);
   }

   public List<String> findElements(String using, String value) {
      Map<String, Object> payload = Map.of("using", using, "value", value);
      List<Map<String, Object>> results = (List<Map<String, Object>>)getValue(this.post("elements", payload));
      List<String> ids = new ArrayList<>();
      if (results != null) {
         for (Map<String, Object> result : results) {
            ids.add(extractElementId(result));
         }
      }

      return ids;
   }

   public void clickElement(String elementId) {
      this.post("element/" + elementId + "/click", Map.of());
   }

   public void sendKeys(String elementId, String text) {
      this.post("element/" + elementId + "/value", Map.of("text", text));
   }

   public void clearElement(String elementId) {
      this.post("element/" + elementId + "/clear", Map.of());
   }

   public String getElementText(String elementId) {
      return (String)getValue(this.get("element/" + elementId + "/text"));
   }

   public String getElementAttribute(String elementId, String name) {
      return (String)getValue(this.get("element/" + elementId + "/attribute/" + name));
   }

   public String getElementProperty(String elementId, String name) {
      Object val = getValue(this.get("element/" + elementId + "/property/" + name));
      return val != null ? val.toString() : null;
   }

   public String getElementTagName(String elementId) {
      return (String)getValue(this.get("element/" + elementId + "/name"));
   }

   public boolean isElementEnabled(String elementId) {
      Object result = getValue(this.get("element/" + elementId + "/enabled"));
      return Boolean.TRUE.equals(result);
   }

   public byte[] elementScreenshot(String elementId) {
      String base64 = (String)getValue(this.get("element/" + elementId + "/screenshot"));
      return Base64.getDecoder().decode(base64);
   }

   public byte[] screenshot() {
      String base64 = (String)getValue(this.get("screenshot"));
      return Base64.getDecoder().decode(base64);
   }

   public void switchFrame(Object id) {
      if (id == null) {
         this.postRaw("frame", "{\"id\":null}");
      } else {
         this.post("frame", Map.of("id", id));
      }
   }

   public void parentFrame() {
      this.post("frame/parent", Map.of());
   }

   public String getAlertText() {
      return (String)getValue(this.get("alert/text"));
   }

   public void acceptAlert() {
      this.post("alert/accept", Map.of());
   }

   public void dismissAlert() {
      this.post("alert/dismiss", Map.of());
   }

   public void sendAlertText(String text) {
      this.post("alert/text", Map.of("text", text));
   }

   public Map<String, Object> getWindowRect() {
      return (Map<String, Object>)getValue(this.get("window/rect"));
   }

   public void setWindowRect(Map<String, Object> rect) {
      this.post("window/rect", rect);
   }

   public void maximizeWindow() {
      this.post("window/maximize", Map.of());
   }

   public void minimizeWindow() {
      this.post("window/minimize", Map.of());
   }

   public void fullscreenWindow() {
      this.post("window/fullscreen", Map.of());
   }

   public String getWindowHandle() {
      return (String)getValue(this.get("window"));
   }

   public List<String> getWindowHandles() {
      return (List<String>)getValue(this.get("window/handles"));
   }

   public void switchWindow(String handle) {
      this.post("window", Map.of("handle", handle));
   }

   public void closeWindow() {
      this.delete("window");
   }

   public Map<String, Object> getCookie(String name) {
      try {
         return (Map<String, Object>)getValue(this.get("cookie/" + name));
      } catch (DriverException var3) {
         if (var3.getMessage() != null && var3.getMessage().contains("no such cookie")) {
            return null;
         } else {
            throw var3;
         }
      }
   }

   public List<Map<String, Object>> getAllCookies() {
      return (List<Map<String, Object>>)getValue(this.get("cookie"));
   }

   public void addCookie(Map<String, Object> cookie) {
      this.post("cookie", Map.of("cookie", cookie));
   }

   public void deleteCookie(String name) {
      this.delete("cookie/" + name);
   }

   public void deleteAllCookies() {
      this.delete("cookie");
   }

   public void performActions(List<Map<String, Object>> actions) {
      this.post("actions", Map.of("actions", actions));
   }

   public void releaseActions() {
      this.delete("actions");
   }

   public void deleteSession() {
      try {
         this.delete("");
         logger.info("W3C session deleted: {}", this.sessionId);
      } catch (Exception var2) {
         logger.warn("Error deleting W3C session {}: {}", this.sessionId, var2.getMessage());
      }
   }

   public String getSessionId() {
      return this.sessionId;
   }

   private Map<String, Object> get(String path) {
      String url = this.baseUrl + "/session/" + this.sessionId + "/" + path;
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(this.timeout).GET().build();
      return this.execute(request);
   }

   private Map<String, Object> post(String path, Map<String, Object> payload) {
      return this.postRaw(path, Json.of(payload).toString());
   }

   private Map<String, Object> postRaw(String path, String json) {
      String url = this.baseUrl + "/session/" + this.sessionId + "/" + path;
      logger.trace("POST {} : {}", path, json);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(url))
         .header("Content-Type", "application/json; charset=utf-8")
         .timeout(this.timeout)
         .POST(BodyPublishers.ofString(json))
         .build();
      return this.execute(request);
   }

   private Map<String, Object> delete(String path) {
      String url = this.baseUrl + "/session/" + this.sessionId + (path.isEmpty() ? "" : "/" + path);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(this.timeout).DELETE().build();
      return this.execute(request);
   }

   private Map<String, Object> execute(HttpRequest request) {
      try {
         HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
         String body = response.body();
         if (body != null && !body.isEmpty()) {
            Map<String, Object> result = Json.of(body).asMap();
            if (response.statusCode() >= 400) {
               Object value = result.get("value");
               String message = "WebDriver error";
               if (value instanceof Map<?, ?> rawErrorMap) {
                  Map<String, Object> errorMap = (Map<String, Object>) rawErrorMap;
                  message = (String)errorMap.getOrDefault("message", message);
                  String error = (String)errorMap.get("error");
                  if (error != null) {
                     message = error + ": " + message;
                  }
               }

               throw new DriverException(
                  "WebDriver " + request.method() + " " + request.uri().getPath() + " failed (" + response.statusCode() + "): " + message
               );
            } else {
               return result;
            }
         } else {
            HashMap<String, Object> nullResult = new HashMap<>();
            nullResult.put("value", null);
            return nullResult;
         }
      } catch (DriverException var9) {
         throw var9;
      } catch (InterruptedException | IOException var10) {
         if (var10 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         throw new DriverException("WebDriver request failed: " + request.method() + " " + request.uri() + ": " + var10.getMessage(), var10);
      }
   }

   private static Object getValue(Map<String, Object> response) {
      return response.get("value");
   }

   private static String extractElementId(Map<String, Object> elementMap) {
      String id = (String)elementMap.get("element-6066-11e4-a52e-4f735466cecf");
      if (id == null) {
         id = (String)elementMap.get("ELEMENT");
      }

      if (id == null) {
         throw new DriverException("WebDriver response missing element ID: " + elementMap);
      } else {
         return id;
      }
   }

   static boolean isElementReference(Object value) {
      return !(value instanceof Map<?, ?> map) ? false : map.containsKey("element-6066-11e4-a52e-4f735466cecf") || map.containsKey("ELEMENT");
   }

   static String elementIdFrom(Object value) {
      return value instanceof Map ? extractElementId((Map<String, Object>)value) : null;
   }
}
