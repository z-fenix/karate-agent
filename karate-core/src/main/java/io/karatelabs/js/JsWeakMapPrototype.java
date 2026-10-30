package io.karatelabs.js;

class JsWeakMapPrototype extends Prototype {
   static final JsWeakMapPrototype INSTANCE = new JsWeakMapPrototype();

   private JsWeakMapPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("get", 1, this::get);
      this.install("set", 2, this::set);
      this.install("has", 1, this::has);
      this.install("delete", 1, this::delete);
      this.install("@@toStringTag", "WeakMap");
      this.installConstructor("WeakMap");
   }

   private static JsWeakMap asWeakMap(Context context) {
      if (context.getThisObject() instanceof JsWeakMap m) {
         return m;
      } else {
         throw JsErrorException.typeError("Method WeakMap.prototype called on incompatible receiver");
      }
   }

   private Object get(Context context, Object[] args) {
      return asWeakMap(context).getValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object set(Context context, Object[] args) {
      JsWeakMap m = asWeakMap(context);
      m.setValue(args.length > 0 ? args[0] : Terms.UNDEFINED, args.length > 1 ? args[1] : Terms.UNDEFINED);
      return m;
   }

   private Object has(Context context, Object[] args) {
      return asWeakMap(context).hasKey(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object delete(Context context, Object[] args) {
      return asWeakMap(context).deleteKey(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }
}
