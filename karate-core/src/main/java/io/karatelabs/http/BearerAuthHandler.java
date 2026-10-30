package io.karatelabs.http;

public class BearerAuthHandler implements AuthHandler {
   private final String token;

   public BearerAuthHandler(String token) {
      this.token = token;
   }

   @Override
   public void apply(HttpRequestBuilder builder) {
      builder.header("Authorization", "Bearer " + this.token);
   }

   @Override
   public String getType() {
      return "bearer";
   }
}
