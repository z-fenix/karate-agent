package io.karatelabs.js;

import java.util.ArrayList;
import java.util.List;

class JsErrorConstructor extends JsFunction {
   private static final byte MESSAGE_ATTRS = 5;
   private final JsErrorPrototype errorPrototype;

   JsErrorConstructor(JsErrorPrototype prototype, int length) {
      this.name = prototype.getTypeName();
      this.length = length;
      this.errorPrototype = prototype;
      this.defineOwn("prototype", this.errorPrototype, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      if (this.errorPrototype == JsErrorPrototype.AGGREGATE_ERROR) {
         return this.constructAggregate(context, args);
      } else {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         String message = null;
         if (args.length > 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
            message = toMessageString(args[0], cc);
            if (cc != null && cc.isError()) {
               return Terms.UNDEFINED;
            }
         }

         JsError instance = new JsError(this.errorPrototype);
         if (message != null) {
            instance.defineOwn("message", message, (byte)5);
         }

         instance.captureStack(cc);
         installCauseFromOptions(instance, args, 1, cc);
         return instance;
      }
   }

   private static String toMessageString(Object value, CoreContext cc) {
      if (value instanceof ObjectLike inner && cc != null) {
         value = Terms.toPrimitive(inner, "string", cc);
         if (cc.isError()) {
            return null;
         }
      }

      if (value == null) {
         return "null";
      } else {
         return value == Terms.UNDEFINED ? "undefined" : value.toString();
      }
   }

   private Object constructAggregate(Context context, Object[] args) {
      Object errorsArg = args.length > 0 ? args[0] : Terms.UNDEFINED;
      if (errorsArg != null && errorsArg != Terms.UNDEFINED) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         List<Object> collected = new ArrayList<>();
         JsIterator iter = IterUtils.getIterator(errorsArg, context);

         while (iter.hasNext()) {
            collected.add(iter.next());
         }

         String message = null;
         if (args.length > 1 && args[1] != Terms.UNDEFINED) {
            message = toMessageString(args[1], cc);
            if (cc != null && cc.isError()) {
               return Terms.UNDEFINED;
            }
         }

         JsError instance = new JsError(this.errorPrototype);
         if (message != null) {
            instance.defineOwn("message", message, (byte)5);
         }

         instance.captureStack(cc);
         installCauseFromOptions(instance, args, 2, cc);
         instance.defineOwn("errors", new JsArray(collected), (byte)5);
         return instance;
      } else {
         throw JsErrorException.typeError("AggregateError requires an iterable of errors");
      }
   }

   private static void installCauseFromOptions(JsError instance, Object[] args, int optionsIndex, CoreContext cc) {
      if (args.length > optionsIndex) {
         if (args[optionsIndex] instanceof ObjectLike opts) {
            if (hasProperty(opts, "cause")) {
               Object causeVal = cc != null ? opts.getMember("cause", opts, cc) : opts.getMember("cause");
               if (cc == null || !cc.isError()) {
                  instance.defineOwn("cause", causeVal, (byte)5);
               }
            }
         }
      }
   }

   private static boolean hasProperty(ObjectLike obj, String name) {
      for (ObjectLike o = obj; o != null; o = o.getPrototype()) {
         if (o.isOwnProperty(name)) {
            return true;
         }
      }

      return false;
   }
}
