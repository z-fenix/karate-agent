package io.karatelabs.http;

import io.karatelabs.common.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthHandler implements AuthHandler {
   private final String username;
   private final String password;

   public BasicAuthHandler(String username, String password) {
      this.username = username;
      this.password = password;
   }

   @Override
   public void apply(HttpRequestBuilder builder) {
      String auth = this.username + ":" + this.password;
      String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
      builder.header("Authorization", "Basic " + encodedAuth);
   }

   @Override
   public String getType() {
      return "basic";
   }

   @Override
   public String toCurlArgument(String platform) {
      String userPass = this.username + ":" + this.password;
      return "-u " + StringUtils.shellEscapeForPlatform(userPass, platform);
   }

   public String getUsername() {
      return this.username;
   }

   public String getPassword() {
      return this.password;
   }
}
