package io.karatelabs.js;

import java.util.function.Function;

class JsRegexPrototype extends Prototype {
   static final JsRegexPrototype INSTANCE = new JsRegexPrototype();
   private static final byte ACCESSOR_ATTRS = 12;

   private JsRegexPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("test", 1, this::test);
      this.install("exec", 1, this::exec);
      this.install("toString", 0, this::toStringMethod);
      this.installFlagAccessor("source", "(?:)", r -> r.pattern);
      this.installFlagAccessor("flags", "", r -> r.flags);
      this.installFlagAccessor("global", Terms.UNDEFINED, r -> r.flags.contains("g"));
      this.installFlagAccessor("ignoreCase", Terms.UNDEFINED, r -> r.flags.contains("i"));
      this.installFlagAccessor("multiline", Terms.UNDEFINED, r -> r.flags.contains("m"));
      this.installFlagAccessor("dotAll", Terms.UNDEFINED, r -> r.flags.contains("s"));
      this.installFlagAccessor("sticky", Terms.UNDEFINED, r -> r.flags.contains("y"));
      this.installFlagAccessor("unicode", Terms.UNDEFINED, r -> r.flags.contains("u"));
      this.installConstructor("RegExp");
   }

   private void installFlagAccessor(String name, Object protoSentinel, Function<JsRegex, Object> extractor) {
      JsCallable lambda = (ctx, args) -> {
         Object thisObj = ctx.getThisObject();
         if (thisObj == this) {
            return protoSentinel;
         } else if (thisObj instanceof JsRegex r) {
            return extractor.apply(r);
         } else {
            throw JsErrorException.typeError("get RegExp.prototype." + name + " called on incompatible receiver");
         }
      };
      JsBuiltinMethod getter = new JsBuiltinMethod("get " + name, 0, lambda);
      this.installAccessor(name, getter, null, (byte)12);
   }

   private static JsRegex requireRegex(Context context, String methodName) {
      if (context.getThisObject() instanceof JsRegex r) {
         return r;
      } else {
         throw JsErrorException.typeError("RegExp.prototype." + methodName + " called on incompatible receiver");
      }
   }

   private static String argString(Object[] args, Context context) {
      Object arg = args.length > 0 ? args[0] : Terms.UNDEFINED;
      if (arg instanceof String s) {
         return s;
      } else {
         return arg instanceof JsString js ? js.text : Terms.toStringCoerce(arg, context instanceof CoreContext cc ? cc : null);
      }
   }

   private Object test(Context context, Object[] args) {
      return requireRegex(context, "test").test(argString(args, context));
   }

   private Object exec(Context context, Object[] args) {
      return requireRegex(context, "exec").exec(argString(args, context));
   }

   private Object toStringMethod(Context context, Object[] args) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsRegex r) {
         return r.toString();
      } else if (thisObj instanceof ObjectLike ol) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         return "/" + Terms.toStringCoerce(ol.getMember("source"), cc) + "/" + Terms.toStringCoerce(ol.getMember("flags"), cc);
      } else {
         throw JsErrorException.typeError("RegExp.prototype.toString called on incompatible receiver");
      }
   }
}
