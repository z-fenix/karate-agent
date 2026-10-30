package io.karatelabs.js;

import java.math.BigInteger;

non-sealed class JsNumber extends JsObject implements JsPrimitive, JsCallable {
   final Number value;

   JsNumber() {
      this(0);
   }

   JsNumber(Number value) {
      super(null, JsNumberPrototype.INSTANCE);
      this.value = value;
   }

   @Override
   public String toString() {
      return Terms.numberToString(this.value);
   }

   @Override
   public Object getJavaValue() {
      return this.value;
   }

   @Override
   public Object call(Context context, Object[] args) {
      return getObject(context, args);
   }

   static Object getObject(Context context, Object[] args) {
      Number temp = 0;
      if (args.length > 0) {
         Object a = args[0];
         if (a instanceof ObjectLike) {
            a = Terms.toPrimitive(a, "number", (CoreContext)context);
            if (context instanceof CoreContext cc && cc.isError()) {
               return Terms.UNDEFINED;
            }
         }

         if (a instanceof BigInteger bi) {
            temp = Terms.narrow(bi.doubleValue());
         } else {
            temp = Terms.objectToNumber(a);
         }
      }

      CallInfo callInfo = context.getCallInfo();
      return callInfo != null && callInfo.constructor ? new JsNumber(temp) : temp;
   }
}
