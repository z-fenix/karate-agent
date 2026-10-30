package io.karatelabs.ext.postman;

import io.karatelabs.js.SimpleObject;
import io.karatelabs.match.Expect;

public class PmExpect implements SimpleObject {
   private final Expect delegate;

   public PmExpect(PmRuntime var1, Object var2) {
      this.delegate = new Expect(var2, false, var1.onMatch, false, () -> var1.karate.engine.getRootContext());
   }

   @Override
   public Object jsGet(String var1) {
      return this.delegate.jsGet(var1);
   }
}
