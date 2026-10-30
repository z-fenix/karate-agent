package io.karatelabs.http;

import io.karatelabs.js.SimpleObject;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public class CsrfProtection {
   public static final String SESSION_KEY = "_csrf_token";
   public static final String FORM_FIELD_NAME = "_csrf";
   public static final String HEADER_NAME = "X-CSRF-Token";
   public static final String HTMX_HEADER_NAME = "HX-CSRF-Token";
   private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final int TOKEN_LENGTH = 32;

   public static String generateToken() {
      byte[] bytes = new byte[32];
      RANDOM.nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }

   public static String getOrCreateToken(Session session) {
      if (session != null && !session.isTemporary()) {
         String token = (String)session.getMember("_csrf_token");
         if (token == null) {
            token = generateToken();
            session.putMember("_csrf_token", token);
         }

         return token;
      } else {
         return null;
      }
   }

   public static boolean validate(HttpRequest request, Session session) {
      if (session != null && !session.isTemporary()) {
         String expectedToken = (String)session.getMember("_csrf_token");
         if (expectedToken == null) {
            return false;
         } else {
            String providedToken = getTokenFromRequest(request);
            return providedToken == null ? false : constantTimeEquals(expectedToken, providedToken);
         }
      } else {
         return false;
      }
   }

   public static String getTokenFromRequest(HttpRequest request) {
      String token = request.getHeader("X-CSRF-Token");
      if (token != null && !token.isEmpty()) {
         return token;
      } else {
         token = request.getHeader("HX-CSRF-Token");
         if (token != null && !token.isEmpty()) {
            return token;
         } else {
            token = request.getParam("_csrf");
            return token != null && !token.isEmpty() ? token : null;
         }
      }
   }

   public static boolean requiresValidation(String method) {
      return method != null && PROTECTED_METHODS.contains(method.toUpperCase());
   }

   public static CsrfProtection.CsrfToken createTemplateToken(Session session) {
      String token = getOrCreateToken(session);
      return token == null ? null : new CsrfProtection.CsrfToken(token);
   }

   private static boolean constantTimeEquals(String a, String b) {
      if (a != null && b != null) {
         if (a.length() != b.length()) {
            return false;
         } else {
            int result = 0;

            for (int i = 0; i < a.length(); i++) {
               result |= a.charAt(i) ^ b.charAt(i);
            }

            return result == 0;
         }
      } else {
         return false;
      }
   }

   public static class CsrfToken implements SimpleObject {
      private final String token;

      public CsrfToken(String token) {
         this.token = token;
      }

      public String getToken() {
         return this.token;
      }

      public String getHeaderName() {
         return "X-CSRF-Token";
      }

      public String getFieldName() {
         return "_csrf";
      }

      @Override
      public Object jsGet(String key) {
         return switch (key) {
            case "token" -> this.token;
            case "headerName" -> "X-CSRF-Token";
            case "fieldName" -> "_csrf";
            default -> null;
         };
      }

      @Override
      public Map<String, Object> toMap() {
         return Map.of("token", this.token != null ? this.token : "", "headerName", "X-CSRF-Token", "fieldName", "_csrf");
      }
   }
}
