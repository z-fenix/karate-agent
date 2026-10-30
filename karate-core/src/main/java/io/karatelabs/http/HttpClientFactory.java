package io.karatelabs.http;

public interface HttpClientFactory {
   HttpClient create();

   default void release(HttpClient client) {
   }
}
