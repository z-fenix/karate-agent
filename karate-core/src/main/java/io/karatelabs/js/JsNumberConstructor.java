package io.karatelabs.js;

class JsNumberConstructor extends JsFunction {
   private static final long MAX_SAFE_INTEGER = 9007199254740991L;
   private static final long MIN_SAFE_INTEGER = -9007199254740991L;
   private static final byte CONSTANT_ATTRS = 8;
   private static final byte METHOD_ATTRS = 13;

   JsNumberConstructor() {
      this.name = "Number";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("EPSILON", Math.ulp(1.0), (byte)8);
      this.defineOwn("MAX_VALUE", Double.MAX_VALUE, (byte)8);
      this.defineOwn("MIN_VALUE", Double.MIN_VALUE, (byte)8);
      this.defineOwn("MAX_SAFE_INTEGER", 9007199254740991L, (byte)8);
      this.defineOwn("MIN_SAFE_INTEGER", -9007199254740991L, (byte)8);
      this.defineOwn("POSITIVE_INFINITY", Double.POSITIVE_INFINITY, (byte)8);
      this.defineOwn("NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, (byte)8);
      this.defineOwn("NaN", Double.NaN, (byte)8);
      this.defineOwn("isFinite", new JsBuiltinMethod("isFinite", 1, (ctx, args) -> this.isFinite(args)), (byte)13);
      this.defineOwn("isInteger", new JsBuiltinMethod("isInteger", 1, (ctx, args) -> this.isInteger(args)), (byte)13);
      this.defineOwn("isNaN", new JsBuiltinMethod("isNaN", 1, (ctx, args) -> this.isNaN(args)), (byte)13);
      this.defineOwn("isSafeInteger", new JsBuiltinMethod("isSafeInteger", 1, (ctx, args) -> this.isSafeInteger(args)), (byte)13);
      this.defineOwn("parseInt", ContextRoot.PARSE_INT, (byte)13);
      this.defineOwn("parseFloat", ContextRoot.PARSE_FLOAT, (byte)13);
      this.defineOwn("prototype", JsNumberPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      return JsNumber.getObject(context, args);
   }

   private Object isFinite(Object[] args) {
      return args.length > 0 && args[0] instanceof Number n ? Double.isFinite(n.doubleValue()) : false;
   }

   private Object isInteger(Object[] args) {
      if (args.length > 0 && args[0] instanceof Number n) {
         double d = n.doubleValue();
         return Double.isFinite(d) && Math.floor(d) == d;
      } else {
         return false;
      }
   }

   private Object isNaN(Object[] args) {
      return args.length > 0 && args[0] instanceof Number n ? Double.isNaN(n.doubleValue()) : false;
   }

   private Object isSafeInteger(Object[] args) {
      if (args.length > 0 && args[0] instanceof Number n) {
         double d = n.doubleValue();
         if (!Double.isFinite(d)) {
            return false;
         } else {
            long l = (long)d;
            return d == l && l >= -9007199254740991L && l <= 9007199254740991L;
         }
      } else {
         return false;
      }
   }
}
