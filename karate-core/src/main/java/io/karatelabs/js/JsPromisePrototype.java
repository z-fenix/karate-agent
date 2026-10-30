package io.karatelabs.js;

class JsPromisePrototype extends Prototype {
   static final JsPromisePrototype INSTANCE = new JsPromisePrototype();
   private final JsCallable thenDelegate = this::then;

   private JsPromisePrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("then", 2, this.thenDelegate);
      this.install("catch", 1, this::catchMethod);
      this.install("finally", 1, this::finallyMethod);
      this.install("@@toStringTag", "Promise");
      this.installConstructor("Promise");
   }

   static boolean isIntrinsicThen(Object value) {
      return value instanceof JsBuiltinMethod method && method.delegate() == INSTANCE.thenDelegate;
   }

   private static JsPromise asPromise(Context context) {
      if (context.getThisObject() instanceof JsPromise promise) {
         return promise;
      } else {
         throw JsErrorException.typeError("Method Promise.prototype called on incompatible receiver");
      }
   }

   private static JsCallable callableOrNull(Object value) {
      return value instanceof JsCallable callable ? callable : null;
   }

   private Object then(Context context, Object[] args) {
      JsPromise self = asPromise(context);
      return then(self, callableOrNull(AsyncSupport.arg(args, 0)), callableOrNull(AsyncSupport.arg(args, 1)));
   }

   static JsPromise then(JsPromise self, JsCallable onFulfilled, JsCallable onRejected) {
      JsPromise derived = new JsPromise(self.engine, self.scope);
      self.addReaction((rejected, value) -> {
         JsCallable handler = rejected ? onRejected : onFulfilled;
         if (handler == null) {
            if (rejected) {
               derived.settle(true, value, null);
            } else {
               AsyncSupport.resolveValue(derived, value, null);
            }
         } else {
            AsyncSupport.Completion outcome = AsyncSupport.invoke(self.engine, handler, new Object[]{value}, null);
            if (outcome.threw()) {
               derived.settle(true, outcome.reason(), null);
            } else {
               AsyncSupport.resolveValue(derived, outcome.value(), null);
            }
         }
      });
      return derived;
   }

   private Object catchMethod(Context context, Object[] args) {
      JsPromise self = asPromise(context);
      return then(self, null, callableOrNull(AsyncSupport.arg(args, 0)));
   }

   private Object finallyMethod(Context context, Object[] args) {
      JsPromise self = asPromise(context);
      JsCallable onFinally = callableOrNull(AsyncSupport.arg(args, 0));
      if (onFinally == null) {
         return then(self, null, null);
      } else {
         JsPromise derived = new JsPromise(self.engine, self.scope);
         self.addReaction((rejected, value) -> {
            AsyncSupport.Completion outcome = AsyncSupport.invoke(self.engine, onFinally, new Object[0], null);
            if (outcome.threw()) {
               derived.settle(true, outcome.reason(), null);
            } else {
               JsPromise awaited = new JsPromise(self.engine, self.scope);
               AsyncSupport.resolveValue(awaited, outcome.value(), null);
               awaited.addReaction((finallyRejected, finallyValue) -> {
                  if (finallyRejected) {
                     derived.settle(true, finallyValue, null);
                  } else if (rejected) {
                     derived.settle(true, value, null);
                  } else {
                     AsyncSupport.resolveValue(derived, value, null);
                  }
               });
            }
         });
         return derived;
      }
   }
}
