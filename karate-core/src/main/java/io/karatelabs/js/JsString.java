package io.karatelabs.js;

import java.util.Iterator;
import java.util.List;

non-sealed class JsString extends JsObject implements JsPrimitive, JsCallable {
   final String text;
   private static final List<String> INTRINSIC_NAMES = List.of("length");

   JsString() {
      this("");
   }

   JsString(String text) {
      super(null, JsStringPrototype.INSTANCE);
      this.text = text;
   }

   @Override
   public String toString() {
      return this.text;
   }

   @Override
   public Object getJavaValue() {
      return this.text;
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      if ("length".equals(name)) {
         return this.text.length();
      } else {
         int i = parseIndex(name);
         return i >= 0 && i < this.text.length() ? String.valueOf(this.text.charAt(i)) : null;
      }
   }

   private static int parseIndex(String s) {
      int n = s.length();
      if (n != 0 && n <= 10) {
         int v = 0;

         for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
               return -1;
            }

            v = v * 10 + (c - '0');
         }

         return n > 1 && s.charAt(0) == 48 ? -1 : v;
      } else {
         return -1;
      }
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   @Override
   public Iterable<KeyValue> jsEntries() {
      return () -> new Iterator<KeyValue>() {
         int index = 0;

         @Override
         public boolean hasNext() {
            return this.index < JsString.this.text.length();
         }

         public KeyValue next() {
            int i = this.index++;
            String c = String.valueOf(JsString.this.text.charAt(i));
            return new KeyValue(JsString.this, i, i + "", c);
         }
      };
   }

   @Override
   public Iterable<KeyValue> jsEntries(CoreContext ctx) {
      return this.jsEntries();
   }

   @Override
   public Object call(Context context, Object[] args) {
      return getObject(context, args);
   }

   static Object getObject(Context context, Object[] args) {
      String temp;
      if (args.length == 0) {
         temp = "";
      } else if (args[0] instanceof JsSymbol sym) {
         temp = sym.toString();
      } else {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         temp = Terms.toStringCoerce(args[0], cc);
      }

      CallInfo callInfo = context.getCallInfo();
      return callInfo != null && callInfo.constructor ? new JsString(temp) : temp;
   }
}
