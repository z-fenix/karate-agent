package io.karatelabs.js;

class JsSetConstructor extends JsFunction {
   JsSetConstructor() {
      this.name = "Set";
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("prototype", JsSetPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      CallInfo callInfo = context.getCallInfo();
      boolean isNew = callInfo != null && callInfo.constructor;
      if (!isNew) {
         throw JsErrorException.typeError("Constructor Set requires 'new'");
      } else {
         JsSet set = new JsSet();
         if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
            if (!(set.getMember("add") instanceof JsCallable adder)) {
               throw JsErrorException.typeError("Set.prototype.add is not callable");
            } else {
               JsIterator iter = IterUtils.getIterator(args[0], context);
               CoreContext cc = context instanceof CoreContext c ? c : null;
               Object savedThis = cc != null ? cc.thisObject : null;

               try {
                  while (iter.hasNext()) {
                     Object v = iter.next();
                     if (cc != null) {
                        cc.thisObject = set;
                     }

                     adder.call(context, new Object[]{v});
                     if (cc != null && cc.isError()) {
                        return set;
                     }
                  }
               } finally {
                  if (cc != null) {
                     cc.thisObject = savedThis;
                  }
               }

               return set;
            }
         } else {
            return set;
         }
      }
   }
}
