package io.karatelabs.js;

class JsStringConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsStringConstructor() {
      this.name = "String";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("fromCharCode", new JsBuiltinMethod("fromCharCode", 1, (ctx, args) -> this.fromCharCode(args)), (byte)13);
      this.defineOwn("fromCodePoint", new JsBuiltinMethod("fromCodePoint", 1, (ctx, args) -> this.fromCodePoint(args)), (byte)13);
      this.defineOwn("raw", new JsBuiltinMethod("raw", 1, this::raw), (byte)13);
      this.defineOwn("prototype", JsStringPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      return JsString.getObject(context, args);
   }

   private Object fromCharCode(Object[] args) {
      StringBuilder sb = new StringBuilder();

      for (Object arg : args) {
         if (arg instanceof Number num) {
            sb.append((char)num.intValue());
         }
      }

      return sb.toString();
   }

   private Object fromCodePoint(Object[] args) {
      StringBuilder sb = new StringBuilder();
      Object[] var3 = args;
      int var4 = args.length;

      for (int var5 = 0; var5 < var4; var5++) {
         if (var3[var5] instanceof Number num) {
            int n = num.intValue();
            if (n < 0 || n > 1114111) {
               throw JsErrorException.rangeError("Invalid code point " + num);
            }

            sb.appendCodePoint(n);
         }
      }

      return sb.toString();
   }

   private Object raw(Context context, Object[] args) {
      Object template = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Terms.requireObjectCoercible(template, "String.raw");
      if (!(template instanceof ObjectLike templateObj)) {
         throw JsErrorException.typeError("String.raw template must be an object");
      } else {
         Object rawObj = templateObj.getMember("raw");
         Terms.requireObjectCoercible(rawObj, "String.raw .raw");
         if (rawObj instanceof ObjectLike raw) {
            CoreContext cc = context instanceof CoreContext c ? c : null;
            double rawLen = Terms.objectToNumber(raw.getMember("length")).doubleValue();
            if (!Double.isNaN(rawLen) && !(rawLen <= 0.0)) {
               long length = rawLen >= 2.147483647E9 ? 2147483647L : (long)rawLen;
               StringBuilder sb = new StringBuilder();

               for (long i = 0L; i < length; i++) {
                  sb.append(Terms.toStringCoerce(raw.getMember(Long.toString(i)), cc));
                  if (i + 1L == length) {
                     break;
                  }

                  int subIdx = (int)(i + 1L);
                  if (subIdx < args.length) {
                     sb.append(Terms.toStringCoerce(args[subIdx], cc));
                  }
               }

               return sb.toString();
            } else {
               return "";
            }
         } else {
            throw JsErrorException.typeError("String.raw .raw must be an object");
         }
      }
   }
}
