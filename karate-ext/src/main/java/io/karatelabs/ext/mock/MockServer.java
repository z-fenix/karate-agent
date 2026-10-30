package io.karatelabs.ext.mock;

public interface MockServer {
   String url();

   int port();

   boolean ssl();

   void stop();
}
