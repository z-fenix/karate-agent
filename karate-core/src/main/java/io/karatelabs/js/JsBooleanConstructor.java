package io.karatelabs.js;

class JsBooleanConstructor extends JsFunction {
   JsBooleanConstructor() {
      this.name = "Boolean";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("prototype", JsBooleanPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object... args) {
      boolean coerced = args.length > 0 && Terms.isTruthy(args[0]);
      CallInfo callInfo = context.getCallInfo();
      return callInfo != null && callInfo.constructor ? new JsBoolean(coerced) : coerced;
   }
}
