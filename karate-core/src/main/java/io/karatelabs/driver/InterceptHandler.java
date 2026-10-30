package io.karatelabs.driver;

@FunctionalInterface
public interface InterceptHandler {
   InterceptResponse handle(InterceptRequest var1);
}
