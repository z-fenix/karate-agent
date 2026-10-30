package io.karatelabs.http;

public enum SameSite {
   STRICT("Strict"),
   LAX("Lax"),
   NONE("None");

   private final String headerValue;

   private SameSite(String headerValue) {
      this.headerValue = headerValue;
   }

   public String headerValue() {
      return this.headerValue;
   }
}
