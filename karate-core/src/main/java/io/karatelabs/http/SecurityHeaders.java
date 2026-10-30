package io.karatelabs.http;

public class SecurityHeaders {
   public static void apply(HttpResponse response, ServerConfig config) {
      if (config.isSecurityHeadersEnabled()) {
         response.setHeader("X-Content-Type-Options", "nosniff");
         response.setHeader("X-Frame-Options", config.getXFrameOptions());
         response.setHeader("X-XSS-Protection", "1; mode=block");
         String csp = config.getContentSecurityPolicy();
         if (csp != null && !csp.isEmpty()) {
            response.setHeader("Content-Security-Policy", csp);
         }

         String referrerPolicy = config.getReferrerPolicy();
         if (referrerPolicy != null && !referrerPolicy.isEmpty()) {
            response.setHeader("Referrer-Policy", referrerPolicy);
         }

         if (config.isHstsEnabled() && !config.isDevMode()) {
            String hstsValue = "max-age=" + config.getHstsMaxAge();
            if (config.isHstsIncludeSubDomains()) {
               hstsValue = hstsValue + "; includeSubDomains";
            }

            response.setHeader("Strict-Transport-Security", hstsValue);
         }
      }
   }

   public static boolean isHtmlResponse(String contentType) {
      return contentType == null ? false : contentType.startsWith("text/html");
   }
}
