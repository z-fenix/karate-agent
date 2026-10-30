package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class JsFunctionPrototype extends Prototype {
   static final JsFunctionPrototype INSTANCE = new JsFunctionPrototype();

   private JsFunctionPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("call", 1, this::callMethod);
      this.install("apply", 2, this::applyMethod);
      this.install("bind", 1, this::bindMethod);
      this.install("toString", 0, this::toStringMethod);
      this.installConstructor("Function");
   }

   private static JsCallable asCallable(Context context) {
      return context.getThisObject() instanceof JsCallable callable ? callable : null;
   }

   private Object callMethod(Context context, Object[] args) {
      JsCallable callable = asCallable(context);
      if (callable == null) {
         throw JsErrorException.typeError("Function.prototype.call called on non-callable");
      } else {
         JsFunctionPrototype.ThisArgs thisArgs = new JsFunctionPrototype.ThisArgs(args, false);
         if (context instanceof CoreContext cc) {
            cc.thisObject = bindForCall(callable, thisArgs.thisObject, cc);
         }

         return callable.call(context, forwardArgs(callable, thisArgs.argArray()));
      }
   }

   private static Object[] forwardArgs(JsCallable callable, Object[] args) {
      if (!callable.isExternal()) {
         return args;
      } else {
         Object[] copy = Arrays.copyOf(args, args.length, Object[].class);
         Interpreter.marshalExternalArgs(copy);
         return copy;
      }
   }

   private static Object bindForCall(JsCallable callable, Object thisObject, CoreContext cc) {
      if (callable instanceof JsBuiltinMethod) {
         return thisObject;
      } else {
         boolean strict = callable instanceof JsFunctionNode jfn && jfn.strict;
         return Interpreter.bindThisForCall(thisObject, cc, strict);
      }
   }

   private Object applyMethod(Context context, Object[] args) {
      JsCallable callable = asCallable(context);
      if (callable == null) {
         throw JsErrorException.typeError("Function.prototype.apply called on non-callable");
      } else {
         JsFunctionPrototype.ThisArgs thisArgs = new JsFunctionPrototype.ThisArgs(args, true);
         if (context instanceof CoreContext cc) {
            cc.thisObject = bindForCall(callable, thisArgs.thisObject, cc);
         }

         return callable.call(context, forwardArgs(callable, thisArgs.argArray()));
      }
   }

   private Object bindMethod(Context context, Object[] args) {
      if (context.getThisObject() instanceof JsCallable target) {
         final Object boundThis = args.length > 0 ? args[0] : Terms.UNDEFINED;
         final Object[] preBound = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new Object[0];
         JsFunction bound = new JsFunction() {
            @Override
            public Object call(Context ctx, Object[] callArgs) {
               Object[] all;
               if (preBound.length == 0) {
                  all = callArgs;
               } else {
                  all = new Object[preBound.length + callArgs.length];
                  System.arraycopy(preBound, 0, all, 0, preBound.length);
                  System.arraycopy(callArgs, 0, all, preBound.length, callArgs.length);
               }

               if (ctx instanceof CoreContext cc) {
                  cc.thisObject = boundThis;
               }

               return target.call(ctx, JsFunctionPrototype.forwardArgs(target, all));
            }
         };
         if (target instanceof JsFunction tf) {
            bound.name = tf.name != null ? "bound " + tf.name : "bound";
            bound.length = Math.max(0, tf.length - preBound.length);
         } else {
            bound.name = "bound";
         }

         return bound;
      } else {
         throw JsErrorException.typeError("Function.prototype.bind called on non-callable");
      }
   }

   private Object toStringMethod(Context context, Object[] args) {
      if (context.getThisObject() instanceof JsFunction fn) {
         String source = fn.getSource();
         if (source != null) {
            return source;
         } else {
            String fnName = fn.name != null ? fn.name : "";
            return "function " + fnName + "() { [native code] }";
         }
      } else {
         return "[object Function]";
      }
   }

   private static class ThisArgs {
      final Object thisObject;
      final List<Object> args;

      ThisArgs(Object[] args, boolean apply) {
         List<Object> list = new ArrayList<>(Arrays.asList(args));
         if (!list.isEmpty()) {
            this.thisObject = list.removeFirst();
         } else {
            this.thisObject = Terms.UNDEFINED;
         }

         if (apply) {
            if (list.isEmpty()) {
               this.args = list;
            } else {
               Object first = list.getFirst();
               if (first instanceof List) {
                  this.args = (List<Object>)first;
               } else {
                  this.args = new ArrayList<>();
               }
            }
         } else {
            this.args = list;
         }
      }

      Object[] argArray() {
         Object[] out = this.args.toArray(new Object[0]);

         for (int i = 0; i < out.length; i++) {
            out[i] = JsArray.unwrapHole(out[i]);
         }

         return out;
      }
   }
}
