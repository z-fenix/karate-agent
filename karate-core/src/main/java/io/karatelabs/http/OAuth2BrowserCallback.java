package io.karatelabs.http;

import java.util.concurrent.CompletableFuture;

public interface OAuth2BrowserCallback {
   CompletableFuture<String> openBrowserForAuthorization(String var1, String var2);

   default void cancel() {
   }
}
