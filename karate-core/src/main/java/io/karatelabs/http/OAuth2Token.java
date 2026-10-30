package io.karatelabs.http;

import java.util.Map;

public class OAuth2Token {
   private final String accessToken;
   private final String tokenType;
   private final String refreshToken;
   private final long expiresAt;
   private final String scope;

   public OAuth2Token(String accessToken, String tokenType, String refreshToken, long expiresAt, String scope) {
      this.accessToken = accessToken;
      this.tokenType = tokenType;
      this.refreshToken = refreshToken;
      this.expiresAt = expiresAt;
      this.scope = scope;
   }

   public static OAuth2Token fromMap(Map<String, Object> data) {
      String accessToken = (String)data.get("access_token");
      String tokenType = (String)data.getOrDefault("token_type", "Bearer");
      String refreshToken = (String)data.get("refresh_token");
      Object expiresInObj = data.get("expires_in");
      int expiresIn = 3600;
      if (expiresInObj instanceof Integer) {
         expiresIn = (Integer)expiresInObj;
      } else if (expiresInObj instanceof Long) {
         expiresIn = ((Long)expiresInObj).intValue();
      }

      String scope = (String)data.get("scope");
      long expiresAt = System.currentTimeMillis() + expiresIn * 1000L;
      return new OAuth2Token(accessToken, tokenType, refreshToken, expiresAt, scope);
   }

   public boolean isExpired() {
      return System.currentTimeMillis() >= this.expiresAt - 60000L;
   }

   public boolean hasRefreshToken() {
      return this.refreshToken != null && !this.refreshToken.isEmpty();
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getTokenType() {
      return this.tokenType;
   }

   public String getRefreshToken() {
      return this.refreshToken;
   }

   public long getExpiresAt() {
      return this.expiresAt;
   }

   public String getScope() {
      return this.scope;
   }

   public long getExpiresInSeconds() {
      long remaining = (this.expiresAt - System.currentTimeMillis()) / 1000L;
      return Math.max(0L, remaining);
   }
}
