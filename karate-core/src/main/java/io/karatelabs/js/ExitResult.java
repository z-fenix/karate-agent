package io.karatelabs.js;

public class ExitResult {
   public final boolean ignoreError = true;
   public final Object returnValue;

   public ExitResult(Object returnValue) {
      this.returnValue = returnValue;
   }
}
