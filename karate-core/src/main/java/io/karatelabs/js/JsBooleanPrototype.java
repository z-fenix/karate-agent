package io.karatelabs.js;

class JsBooleanPrototype extends Prototype {
   static final JsBooleanPrototype INSTANCE = new JsBooleanPrototype();

   private JsBooleanPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("toString", 0, JsBooleanPrototype::toStringMethod);
      this.install("valueOf", 0, JsBooleanPrototype::valueOfMethod);
   }

   private static boolean asBoolean(Context context) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsBoolean jb) {
         return jb.value;
      } else {
         return thisObj instanceof Boolean b ? b : false;
      }
   }

   private static Object toStringMethod(Context context, Object[] args) {
      return Boolean.toString(asBoolean(context));
   }

   private static Object valueOfMethod(Context context, Object[] args) {
      return asBoolean(context);
   }
}
