package io.karatelabs.match;

@FunctionalInterface
public interface MarkerPolicy {
   boolean trusted(String var1);

   default RuntimeException untrustedFailure(RuntimeException e) {
      return e;
   }
}
