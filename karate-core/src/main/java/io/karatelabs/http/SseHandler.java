package io.karatelabs.http;

@FunctionalInterface
public interface SseHandler {
   void accept(HttpRequest var1, SseConnection var2);
}
