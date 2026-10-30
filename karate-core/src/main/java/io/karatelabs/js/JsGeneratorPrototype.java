package io.karatelabs.js;

class JsGeneratorPrototype extends Prototype {
   static final JsGeneratorPrototype INSTANCE = new JsGeneratorPrototype();

   private JsGeneratorPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("next", 1, this::next);
      this.install("return", 1, this::returnMethod);
      this.install("throw", 1, this::throwMethod);
      this.install("@@iterator", 0, (context, args) -> asGenerator(context));
   }

   private static JsGenerator asGenerator(Context context) {
      if (context.getThisObject() instanceof JsGenerator g) {
         return g;
      } else {
         throw JsErrorException.typeError("Method Generator.prototype called on incompatible receiver");
      }
   }

   private static Object arg(Object[] args) {
      return args.length > 0 ? args[0] : Terms.UNDEFINED;
   }

   private Object next(Context context, Object[] args) {
      return asGenerator(context).next((CoreContext)context, arg(args));
   }

   private Object returnMethod(Context context, Object[] args) {
      return asGenerator(context).returnValue((CoreContext)context, arg(args));
   }

   private Object throwMethod(Context context, Object[] args) {
      return asGenerator(context).throwValue((CoreContext)context, arg(args));
   }
}
