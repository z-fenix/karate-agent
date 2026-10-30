package io.karatelabs.js;

import java.util.Date;

class JsDateConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsDateConstructor() {
      this.name = "Date";
      this.length = 7;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("now", new JsBuiltinMethod("now", 0, (ctx, args) -> this.now(args)), (byte)13);
      this.defineOwn("parse", new JsBuiltinMethod("parse", 1, (ctx, args) -> this.parse(args)), (byte)13);
      this.defineOwn("UTC", new JsBuiltinMethod("UTC", 7, (ctx, args) -> this.utcWithContext(ctx instanceof CoreContext c ? c : null, args)), (byte)13);
      this.defineOwn("prototype", JsDatePrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      CallInfo callInfo = context.getCallInfo();
      boolean isNew = callInfo != null && callInfo.constructor;
      if (!isNew) {
         return new JsDate().toString();
      } else if (args.length == 0) {
         return new JsDate();
      } else {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof JsDate jd) {
               return new JsDate(jd.getTimeValue());
            } else if (arg instanceof Date d) {
               return new JsDate(d);
            } else {
               Object prim = arg;
               if (arg instanceof ObjectLike) {
                  prim = Terms.toPrimitive(arg, "default", cc);
                  if (cc != null && cc.isError()) {
                     return new JsDate(Double.NaN);
                  }
               } else if (arg instanceof JsValue jv) {
                  prim = jv.getJsValue();
               }

               if (prim instanceof String s) {
                  return new JsDate(JsDate.parseToTimeValue(s));
               } else {
                  double v = Terms.objectToNumber(prim).doubleValue();
                  return new JsDate(v);
               }
            }
         } else {
            double[] parts = coerceArgs(args, Math.min(args.length, 7), cc);
            if (parts == null) {
               return new JsDate(Double.NaN);
            } else {
               double year = parts[0];
               double month = parts[1];
               double date = parts.length > 2 ? parts[2] : 1.0;
               double hours = parts.length > 3 ? parts[3] : 0.0;
               double minutes = parts.length > 4 ? parts[4] : 0.0;
               double seconds = parts.length > 5 ? parts[5] : 0.0;
               double ms = parts.length > 6 ? parts[6] : 0.0;
               if (!Double.isNaN(year)) {
                  double iy = year < 0.0 ? Math.ceil(year) : Math.floor(year);
                  if (iy >= 0.0 && iy <= 99.0) {
                     year = 1900.0 + iy;
                  }
               }

               double day = JsDate.makeDay(year, month, date);
               double time = JsDate.makeTime(hours, minutes, seconds, ms);
               double local = JsDate.makeDate(day, time);
               return new JsDate(JsDate.localToUtc(local));
            }
         }
      }
   }

   private static double[] coerceArgs(Object[] args, int n, CoreContext cc) {
      double[] out = new double[n];

      for (int i = 0; i < n; i++) {
         Object v = args[i];
         if (v instanceof ObjectLike && cc != null) {
            v = Terms.toPrimitive(v, "number", cc);
            if (cc.isError()) {
               return null;
            }
         } else if (v instanceof JsValue jv) {
            v = jv.getJsValue();
         }

         out[i] = Terms.objectToNumber(v).doubleValue();
      }

      return out;
   }

   private Object now(Object[] args) {
      return System.currentTimeMillis();
   }

   private Object parse(Object[] args) {
      if (args.length == 0) {
         return Double.NaN;
      } else {
         Object arg = args[0];
         String s = arg == null ? "null" : String.valueOf(arg instanceof JsValue jv ? jv.getJsValue() : arg);
         double v = JsDate.parseToTimeValue(s);
         return Double.isNaN(v) ? Double.NaN : (long)v;
      }
   }

   private Object utc(Object[] args) {
      return this.utcWithContext(null, args);
   }

   private Object utcWithContext(CoreContext cc, Object[] args) {
      if (args.length == 0) {
         return Double.NaN;
      } else {
         double[] parts = coerceArgs(args, Math.min(args.length, 7), cc);
         if (parts == null) {
            return Double.NaN;
         } else {
            double year = parts[0];
            double month = parts.length > 1 ? parts[1] : 0.0;
            double date = parts.length > 2 ? parts[2] : 1.0;
            double hours = parts.length > 3 ? parts[3] : 0.0;
            double minutes = parts.length > 4 ? parts[4] : 0.0;
            double seconds = parts.length > 5 ? parts[5] : 0.0;
            double ms = parts.length > 6 ? parts[6] : 0.0;
            if (!Double.isNaN(year)) {
               double iy = year < 0.0 ? Math.ceil(year) : Math.floor(year);
               if (iy >= 0.0 && iy <= 99.0) {
                  year = 1900.0 + iy;
               }
            }

            double day = JsDate.makeDay(year, month, date);
            double time = JsDate.makeTime(hours, minutes, seconds, ms);
            double v = JsDate.timeClip(JsDate.makeDate(day, time));
            return Double.isNaN(v) ? Double.NaN : (long)v;
         }
      }
   }
}
