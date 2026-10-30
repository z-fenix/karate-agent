package io.karatelabs.http;

@FunctionalInterface
public interface WsHandler {
   void accept(HttpRequest var1, WsConnection var2);
}
