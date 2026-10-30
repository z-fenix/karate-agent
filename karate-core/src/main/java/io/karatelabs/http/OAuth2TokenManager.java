package io.karatelabs.http;

import io.karatelabs.common.Json;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2TokenManager {
   private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenManager.class);
   private final Map<String, Object> config;
   private OAuth2Token currentToken;

   public OAuth2TokenManager(Map<String, Object> config) {
      this.config = config;
   }

   public OAuth2Token getValidToken() {
      return this.currentToken != null && !this.currentToken.isExpired() ? this.currentToken : null;
   }

   public void storeToken(OAuth2Token token) {
      this.currentToken = token;
      logger.debug("Token stored, expires in {} seconds", token.getExpiresInSeconds());
   }

   public OAuth2Token refreshToken(HttpRequestBuilder builder) {
      if (this.currentToken != null && this.currentToken.hasRefreshToken()) {
         logger.debug("Refreshing OAuth token...");
         String tokenUrl = (String)this.config.get("url");
         builder.url(tokenUrl);
         builder.formField("grant_type", "refresh_token");
         builder.formField("refresh_token", this.currentToken.getRefreshToken());
         builder.formField("client_id", this.config.get("client_id"));
         if (this.config.containsKey("client_secret")) {
            builder.formField("client_secret", this.config.get("client_secret"));
         }

         builder.header("Accept", "application/json");

         try {
            HttpResponse response = builder.invoke("post");
            String body = response.getBodyString();
            int status = response.getStatus();
            if (status >= 200 && status < 300) {
               Json json;
               try {
                  json = Json.of(body);
               } catch (Exception var9) {
                  String errorMessage = "Token refresh failed: server returned invalid JSON response";
                  logger.error(errorMessage);
                  this.currentToken = null;
                  throw new OAuth2Exception(errorMessage);
               }

               if (!json.isObject()) {
                  String errorMessage = "Token refresh failed: expected JSON object but received " + (json.isArray() ? "array" : "primitive value");
                  logger.error(errorMessage);
                  this.currentToken = null;
                  throw new OAuth2Exception(errorMessage);
               } else {
                  Map<String, Object> data = json.asMap();
                  OAuth2Token newToken = OAuth2Token.fromMap(data);
                  this.storeToken(newToken);
                  logger.debug("Token refreshed successfully");
                  return newToken;
               }
            } else {
               String errorMessage = this.parseOAuthError(body, status);
               logger.error("Token refresh failed: {}", errorMessage);
               this.currentToken = null;
               throw new OAuth2Exception(errorMessage);
            }
         } catch (OAuth2Exception var10) {
            throw var10;
         } catch (Exception var11) {
            String errorMessage = "Token refresh failed: " + var11.getMessage();
            logger.error(errorMessage);
            this.currentToken = null;
            throw new OAuth2Exception(errorMessage, var11);
         }
      } else {
         logger.warn("Cannot refresh: no refresh token available");
         return null;
      }
   }

   public void clearToken() {
      this.currentToken = null;
      logger.debug("Token cleared");
   }

   private String parseOAuthError(String body, int status) {
      if (body != null && !body.isBlank()) {
         try {
            Json json = Json.of(body);
            if (json.isObject()) {
               Map<String, Object> data = json.asMap();
               String error = (String)data.get("error");
               String errorDescription = (String)data.get("error_description");
               if (error != null) {
                  if (errorDescription != null) {
                     return "Token refresh failed: " + error + " - " + errorDescription;
                  }

                  return "Token refresh failed: " + error;
               }
            }
         } catch (Exception var7) {
         }

         String preview = body.length() > 200 ? body.substring(0, 200) + "..." : body;
         return "Token refresh failed: server returned HTTP " + status + ": " + preview;
      } else {
         return "Token refresh failed: server returned HTTP " + status + " with empty response";
      }
   }
}
