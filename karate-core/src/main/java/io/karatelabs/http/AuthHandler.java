package io.karatelabs.http;

public interface AuthHandler {
   void apply(HttpRequestBuilder var1);

   String getType();

   default String toCurlArgument(String platform) {
      return null;
   }

   default String toCurlPreview(String platform) {
      return this.toCurlArgument(platform);
   }
}
