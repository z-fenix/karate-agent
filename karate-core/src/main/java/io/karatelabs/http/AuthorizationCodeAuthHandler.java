package io.karatelabs.http;

import io.karatelabs.common.Json;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationCodeAuthHandler implements AuthHandler {
   private static final Logger logger = LoggerFactory.getLogger(AuthorizationCodeAuthHandler.class);
   private final Map<String, Object> config;
   private final OAuth2BrowserCallback browserCallback;
   private final OAuth2TokenManager tokenManager;

   public AuthorizationCodeAuthHandler(Map<String, Object> config, OAuth2BrowserCallback browserCallback) {
      this.config = config;
      this.browserCallback = browserCallback;
      this.tokenManager = new OAuth2TokenManager(config);
   }

   @Override
   public void apply(HttpRequestBuilder builder) {
      OAuth2Token token = this.tokenManager.getValidToken();
      if (token == null || token.isExpired()) {
         if (token != null && token.hasRefreshToken()) {
            logger.debug("Token expired, attempting refresh...");

            try {
               token = this.tokenManager.refreshToken(builder.forkNewBuilder());
            } catch (OAuth2Exception var4) {
               logger.warn("Token refresh failed, performing full authorization flow");
               token = null;
            }
         }

         if (token == null) {
            logger.debug("No valid token, starting authorization flow...");
            token = this.performAuthorizationFlow(builder);
         }
      }

      builder.header("Authorization", "Bearer " + token.getAccessToken());
   }

   private OAuth2Token performAuthorizationFlow(HttpRequestBuilder builder) {
      LocalCallbackServer callbackServer = null;

      OAuth2Token var10;
      try {
         logger.debug("Generating PKCE...");
         PkceGenerator pkce = PkceGenerator.create();
         logger.debug("Starting local callback server...");
         callbackServer = new LocalCallbackServer();
         String redirectUri = this.startCallbackServer(callbackServer);
         String authUrl = this.buildAuthorizationUrl(pkce, redirectUri);
         logger.debug("Authorization URL: {}", authUrl);
         logger.debug("Opening browser for user authorization...");
         CompletableFuture<String> browserFuture = this.browserCallback.openBrowserForAuthorization(authUrl, redirectUri);
         String browserResult = browserFuture.get(30L, TimeUnit.SECONDS);
         String code;
         if ("USE_CALLBACK_SERVER".equals(browserResult)) {
            logger.debug("Browser opened, waiting for OAuth callback...");
            code = callbackServer.getCodeFuture().get(5L, TimeUnit.MINUTES);
         } else {
            code = browserResult;
         }

         logger.debug("Authorization code received");
         OAuth2Token token = this.exchangeCodeForToken(builder.forkNewBuilder(), code, pkce.getVerifier(), redirectUri);
         this.tokenManager.storeToken(token);
         var10 = token;
      } catch (Exception var14) {
         logger.error("Authorization flow failed: {}", var14.getMessage());
         throw new OAuth2Exception("Authorization flow failed: " + var14.getMessage(), var14);
      } finally {
         if (callbackServer != null) {
            callbackServer.stop();
         }
      }

      return var10;
   }

   private String buildAuthorizationUrl(PkceGenerator pkce, String redirectUri) {
      String authzEndpoint = (String)this.config.get("authorizationUrl");
      if (authzEndpoint == null) {
         throw new OAuth2Exception("Missing 'authorizationUrl' in OAuth config");
      } else {
         String clientId = (String)this.config.get("client_id");
         if (clientId == null) {
            throw new OAuth2Exception("Missing 'client_id' in OAuth config");
         } else {
            String scope = (String)this.config.getOrDefault("scope", "");
            StringBuilder url = new StringBuilder(authzEndpoint);
            url.append(authzEndpoint.contains("?") ? "&" : "?");
            url.append("response_type=code");
            url.append("&client_id=").append(this.urlEncode(clientId));
            url.append("&redirect_uri=").append(this.urlEncode(redirectUri));
            url.append("&code_challenge=").append(this.urlEncode(pkce.getChallenge()));
            url.append("&code_challenge_method=").append(pkce.getMethod());
            if (!scope.isEmpty()) {
               url.append("&scope=").append(this.urlEncode(scope));
            }

            String state = this.generateState();
            url.append("&state=").append(this.urlEncode(state));
            return url.toString();
         }
      }
   }

   private OAuth2Token exchangeCodeForToken(HttpRequestBuilder builder, String code, String codeVerifier, String redirectUri) {
      String tokenUrl = (String)this.config.get("url");
      if (tokenUrl == null) {
         throw new OAuth2Exception("Missing 'url' (token endpoint) in OAuth config");
      } else {
         logger.debug("Exchanging authorization code for token...");
         builder.url(tokenUrl);
         builder.formField("grant_type", "authorization_code");
         builder.formField("code", code);
         builder.formField("redirect_uri", redirectUri);
         builder.formField("client_id", this.config.get("client_id"));
         builder.formField("code_verifier", codeVerifier);
         if (this.config.containsKey("client_secret")) {
            builder.formField("client_secret", this.config.get("client_secret"));
         }

         builder.header("Accept", "application/json");

         try {
            HttpResponse response = builder.invoke("post");
            String bodyString = response.getBodyString();

            Json json;
            try {
               json = Json.of(bodyString);
            } catch (Exception var12) {
               throw new OAuth2Exception("Token endpoint returned invalid response: " + this.truncate(bodyString));
            }

            if (!json.isObject()) {
               throw new OAuth2Exception("Token endpoint returned unexpected response: " + this.truncate(bodyString));
            } else {
               Map<String, Object> data = json.asMap();
               if (data.containsKey("error")) {
                  String error = String.valueOf(data.get("error"));
                  String desc = data.containsKey("error_description") ? String.valueOf(data.get("error_description")) : null;
                  throw new OAuth2Exception("Token request failed: " + error + (desc != null ? " - " + desc : ""));
               } else {
                  logger.debug("Token exchange successful");
                  return OAuth2Token.fromMap(data);
               }
            }
         } catch (OAuth2Exception var13) {
            logger.error("Token exchange failed: {}", var13.getMessage());
            throw new OAuth2Exception("Token exchange failed: " + var13.getMessage(), var13);
         } catch (Exception var14) {
            logger.error("Token exchange failed: {}", var14.getMessage());
            throw new OAuth2Exception("Token exchange failed: " + var14.getMessage(), var14);
         }
      }
   }

   private String startCallbackServer(LocalCallbackServer server) {
      int[] ports = this.getConfiguredPorts();
      Exception lastException = null;

      for (int port : ports) {
         try {
            String redirectUri = server.start(port);
            logger.debug("Callback server started on port {}", port);
            return redirectUri;
         } catch (Exception var9) {
            logger.warn("Port {} in use, trying next port", port);
            lastException = var9;
         }
      }

      throw new OAuth2Exception(
         "Could not start callback server on any configured port: "
            + Arrays.toString(ports)
            + ". Please ensure at least one port is available, or configure different ports using 'callbackPort' or 'callbackPorts'. These ports must be registered as redirect URIs with your OAuth provider.",
         lastException
      );
   }

   private int[] getConfiguredPorts() {
      Object callbackPort = this.config.get("callbackPort");
      if (callbackPort != null) {
         return new int[]{this.parsePort(callbackPort)};
      } else {
         Object callbackPorts = this.config.get("callbackPorts");
         return callbackPorts != null ? this.parsePortList(callbackPorts) : new int[]{8080, 8888, 9090, 3000};
      }
   }

   private int parsePort(Object portValue) {
      if (portValue instanceof Integer) {
         return (Integer)portValue;
      } else if (portValue instanceof String) {
         try {
            return Integer.parseInt((String)portValue);
         } catch (NumberFormatException var3) {
            throw new OAuth2Exception("Invalid callbackPort: " + portValue);
         }
      } else {
         throw new OAuth2Exception("Invalid callbackPort type: " + portValue.getClass());
      }
   }

   private int[] parsePortList(Object portsValue) {
      if (!(portsValue instanceof String)) {
         throw new OAuth2Exception("Invalid callbackPorts type: " + portsValue.getClass());
      } else {
         String[] parts = ((String)portsValue).split(",");
         int[] ports = new int[parts.length];

         for (int i = 0; i < parts.length; i++) {
            try {
               ports[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException var6) {
               throw new OAuth2Exception("Invalid port in callbackPorts: " + parts[i]);
            }
         }

         return ports;
      }
   }

   private String generateState() {
      return UUID.randomUUID().toString();
   }

   private String urlEncode(String value) {
      try {
         return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
      } catch (UnsupportedEncodingException var3) {
         throw new OAuth2Exception("URL encoding failed", var3);
      }
   }

   private String truncate(String value) {
      if (value == null) {
         return "(empty)";
      } else {
         return value.length() <= 100 ? value : value.substring(0, 100) + "...";
      }
   }

   @Override
   public String getType() {
      return "oauth2";
   }

   @Override
   public String toCurlPreview(String platform) {
      return null;
   }
}
