package io.karatelabs.http;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceGenerator {
   private final String verifier;
   private final String challenge;
   private final String method;

   private PkceGenerator(String verifier, String challenge, String method) {
      this.verifier = verifier;
      this.challenge = challenge;
      this.method = method;
   }

   public static PkceGenerator create() {
      return create("S256");
   }

   public static PkceGenerator create(String method) {
      String verifier = generateCodeVerifier();
      String challenge = generateCodeChallenge(verifier, method);
      return new PkceGenerator(verifier, challenge, method);
   }

   private static String generateCodeVerifier() {
      SecureRandom random = new SecureRandom();
      byte[] bytes = new byte[32];
      random.nextBytes(bytes);
      return base64UrlEncode(bytes);
   }

   private static String generateCodeChallenge(String verifier, String method) {
      if ("plain".equals(method)) {
         return verifier;
      } else if ("S256".equals(method)) {
         try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return base64UrlEncode(hash);
         } catch (Exception var4) {
            throw new OAuth2Exception("Failed to generate code challenge", var4);
         }
      } else {
         throw new IllegalArgumentException("Unsupported PKCE method: " + method);
      }
   }

   private static String base64UrlEncode(byte[] data) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
   }

   public String getVerifier() {
      return this.verifier;
   }

   public String getChallenge() {
      return this.challenge;
   }

   public String getMethod() {
      return this.method;
   }
}
