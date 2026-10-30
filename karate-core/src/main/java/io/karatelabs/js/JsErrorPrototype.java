package io.karatelabs.js;

class JsErrorPrototype extends Prototype {
   static final JsErrorPrototype ERROR = new JsErrorPrototype("Error", JsObjectPrototype.INSTANCE, true);
   static final JsErrorPrototype TYPE_ERROR = new JsErrorPrototype("TypeError", ERROR, false);
   static final JsErrorPrototype RANGE_ERROR = new JsErrorPrototype("RangeError", ERROR, false);
   static final JsErrorPrototype SYNTAX_ERROR = new JsErrorPrototype("SyntaxError", ERROR, false);
   static final JsErrorPrototype REFERENCE_ERROR = new JsErrorPrototype("ReferenceError", ERROR, false);
   static final JsErrorPrototype URI_ERROR = new JsErrorPrototype("URIError", ERROR, false);
   static final JsErrorPrototype EVAL_ERROR = new JsErrorPrototype("EvalError", ERROR, false);
   static final JsErrorPrototype AGGREGATE_ERROR = new JsErrorPrototype("AggregateError", ERROR, false);
   private final String typeName;

   private JsErrorPrototype(String name, Prototype parent, boolean isRoot) {
      super(parent);
      this.typeName = name;
      this.install("name", name);
      this.installConstructor(name);
      if (isRoot) {
         this.install("message", "");
         this.install("toString", 0, JsErrorPrototype::toStringMethod);
      }
   }

   String getTypeName() {
      return this.typeName;
   }

   private static Object toStringMethod(Context context, Object[] args) {
      if (context.getThisObject() instanceof ObjectLike obj) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         String name = readToString(obj, "name", "Error", cc);
         if (name == null) {
            return Terms.UNDEFINED;
         } else {
            String msg = readToString(obj, "message", "", cc);
            if (msg == null) {
               return Terms.UNDEFINED;
            } else if (name.isEmpty()) {
               return msg;
            } else {
               return msg.isEmpty() ? name : name + ": " + msg;
            }
         }
      } else {
         throw JsErrorException.typeError("Error.prototype.toString called on non-object");
      }
   }

   private static String readToString(ObjectLike obj, String key, String defaultIfUndefined, CoreContext cc) {
      Object val = cc != null ? obj.getMember(key, obj, cc) : obj.getMember(key);
      if (cc != null && cc.isError()) {
         return null;
      } else if (val != null && val != Terms.UNDEFINED) {
         if (val instanceof ObjectLike inner && cc != null) {
            val = Terms.toPrimitive(inner, "string", cc);
            if (cc.isError()) {
               return null;
            }
         }

         if (val == null) {
            return "null";
         } else {
            return val == Terms.UNDEFINED ? "undefined" : val.toString();
         }
      } else {
         return defaultIfUndefined;
      }
   }
}
