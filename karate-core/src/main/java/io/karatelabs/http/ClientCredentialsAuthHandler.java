package io.karatelabs.http;

import io.karatelabs.common.Json;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientCredentialsAuthHandler implements AuthHandler {
   private static final Logger logger = LoggerFactory.getLogger(ClientCredentialsAuthHandler.class);
   private final Map<String, Object> config;
   private Map<String, Object> token;

   public ClientCredentialsAuthHandler(Map<String, Object> config) {
      this.config = config;
   }

   static Map<String, Object> requestToken(HttpRequestBuilder builder, Map<String, Object> config, String mainRequestUrl) {
      String tokenUrl = (String)config.get("url");
      if (tokenUrl != null && !tokenUrl.startsWith("http://") && !tokenUrl.startsWith("https://") && mainRequestUrl != null) {
         try {
            URI mainUri = new URI(mainRequestUrl);
            String baseUrl = mainUri.getScheme() + "://" + mainUri.getAuthority();
            if (!tokenUrl.startsWith("/")) {
               tokenUrl = "/" + tokenUrl;
            }

            tokenUrl = baseUrl + tokenUrl;
         } catch (URISyntaxException var7) {
            logger.warn("failed to parse main request URL for relative token URL resolution: {}", var7.getMessage());
         }
      }

      builder.url(tokenUrl);
      builder.formField("grant_type", "client_credentials");
      builder.formField("client_id", config.get("client_id"));
      builder.formField("client_secret", config.get("client_secret"));
      if (config.containsKey("scope")) {
         builder.formField("scope", config.get("scope"));
      }

      builder.header("Accept", "application/json");

      try {
         HttpResponse response = builder.invoke("post");
         return Json.of(response.getBodyString()).asMap();
      } catch (Exception var6) {
         logger.error("client credentials auth request failed: {}", var6.getMessage());
         throw new RuntimeException("client credentials auth request failed: " + var6.getMessage(), var6);
      }
   }

   @Override
   public void apply(HttpRequestBuilder builder) {
      if (this.token == null) {
         this.token = requestToken(builder.forkNewBuilder(), this.config, builder.getUrl());
      }

      builder.header("Authorization", "Bearer " + this.token.get("access_token"));
   }

   @Override
   public String getType() {
      return "oauth2";
   }

   @Override
   public String toCurlPreview(String platform) {
      return null;
   }

   public String getAuthorizationHeaderPreview() {
      return "Bearer <your-oauth2-access-token>";
   }
}
