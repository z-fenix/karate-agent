package io.karatelabs.js;

final class PrivateAccess {
   private PrivateAccess() {
   }

   static PrivateName resolve(String name, CoreContext context) {
      PrivateName pn = context.privateEnv == null ? null : context.privateEnv.resolve(name);
      if (pn == null) {
         throw JsErrorException.syntaxError("private name " + name + " is not declared in an enclosing class");
      } else {
         return pn;
      }
   }

   static boolean has(Object target, PrivateName pn) {
      return target instanceof JsObject obj && obj.hasPrivate(pn);
   }

   static Object get(Object target, PrivateName pn, CoreContext context) {
      JsObject obj = branded(target, pn, "read");

      return switch (pn.kind) {
         case FIELD -> obj.getPrivate(pn);
         case METHOD -> pn.method;
         case ACCESSOR -> {
            if (pn.getter == null) {
               throw JsErrorException.typeError("'" + pn.name + "' was defined without a getter");
            }

            yield Interpreter.invokeGetter(pn.getter, target, context);
         }
      };
   }

   static void set(Object target, PrivateName pn, Object value, CoreContext context) {
      JsObject obj = branded(target, pn, "write");
      switch (pn.kind) {
         case FIELD:
            obj.putPrivate(pn, value);
            break;
         case METHOD:
            throw JsErrorException.typeError("Cannot write to private method " + pn.name);
         case ACCESSOR:
            if (pn.setter == null) {
               throw JsErrorException.typeError("'" + pn.name + "' was defined without a setter");
            }

            Interpreter.invokeSetter(pn.setter, target, value, context);
      }
   }

   private static JsObject branded(Object target, PrivateName pn, String verb) {
      if (target instanceof JsObject obj && obj.hasPrivate(pn)) {
         return obj;
      } else {
         throw JsErrorException.typeError("Cannot " + verb + " private member " + pn.name + " from an object whose class did not declare it");
      }
   }
}
