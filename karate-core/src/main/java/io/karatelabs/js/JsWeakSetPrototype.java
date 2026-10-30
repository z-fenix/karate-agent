package io.karatelabs.js;

class JsWeakSetPrototype extends Prototype {
   static final JsWeakSetPrototype INSTANCE = new JsWeakSetPrototype();

   private JsWeakSetPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("add", 1, this::add);
      this.install("has", 1, this::has);
      this.install("delete", 1, this::delete);
      this.install("@@toStringTag", "WeakSet");
      this.installConstructor("WeakSet");
   }

   private static JsWeakSet asWeakSet(Context context) {
      if (context.getThisObject() instanceof JsWeakSet s) {
         return s;
      } else {
         throw JsErrorException.typeError("Method WeakSet.prototype called on incompatible receiver");
      }
   }

   private Object add(Context context, Object[] args) {
      JsWeakSet s = asWeakSet(context);
      s.addValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
      return s;
   }

   private Object has(Context context, Object[] args) {
      return asWeakSet(context).has(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object delete(Context context, Object[] args) {
      return asWeakSet(context).deleteValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }
}
