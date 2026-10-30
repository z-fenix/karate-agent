package io.karatelabs.js;

import java.math.BigInteger;

class JsBigIntPrototype extends Prototype {
   static final JsBigIntPrototype INSTANCE = new JsBigIntPrototype();

   private JsBigIntPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("toString", 0, this::toStringMethod);
      this.install("valueOf", 0, this::valueOf);
      this.install("toLocaleString", 0, this::toStringMethod);
   }

   private Object toStringMethod(Context context, Object[] args) {
      BigInteger n = asBigInt(context);
      if (args.length > 0 && args[0] != Terms.UNDEFINED) {
         Object radixArg = args[0];
         if (radixArg instanceof ObjectLike && context instanceof CoreContext cc) {
            radixArg = Terms.toPrimitive(radixArg, "number", cc);
            if (cc.isError()) {
               return Terms.UNDEFINED;
            }
         }

         if (radixArg instanceof BigInteger) {
            throw JsErrorException.typeError("Cannot convert a BigInt to a number");
         } else {
            int radix = Terms.objectToNumber(radixArg).intValue();
            if (radix >= 2 && radix <= 36) {
               return n.toString(radix);
            } else {
               throw JsErrorException.rangeError("toString() radix must be between 2 and 36");
            }
         }
      } else {
         return n.toString();
      }
   }

   private Object valueOf(Context context, Object[] args) {
      return asBigInt(context);
   }

   private static BigInteger asBigInt(Context context) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsBigInt jb) {
         return jb.value;
      } else if (thisObj instanceof BigInteger bi) {
         return bi;
      } else {
         throw JsErrorException.typeError("BigInt.prototype method called on non-BigInt");
      }
   }
}
