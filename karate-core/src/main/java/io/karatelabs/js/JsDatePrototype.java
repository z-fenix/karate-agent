package io.karatelabs.js;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

class JsDatePrototype extends Prototype {
   static final JsDatePrototype INSTANCE = new JsDatePrototype();

   private static String formatIso(JsDate d) {
      ZonedDateTime z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneOffset.UTC);
      return String.format(
         Locale.ROOT,
         "%s-%02d-%02dT%02d:%02d:%02d.%03dZ",
         JsDate.formatYearIso(z.getYear()),
         z.getMonthValue(),
         z.getDayOfMonth(),
         z.getHour(),
         z.getMinute(),
         z.getSecond(),
         (int)Math.floorMod(d.getTime(), 1000L)
      );
   }

   private static String formatUtc(JsDate d) {
      ZonedDateTime z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneOffset.UTC);
      String dayName = z.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      String monthName = z.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      return String.format(
         Locale.ROOT,
         "%s, %02d %s %s %02d:%02d:%02d GMT",
         dayName,
         z.getDayOfMonth(),
         monthName,
         JsDate.formatYear4(z.getYear()),
         z.getHour(),
         z.getMinute(),
         z.getSecond()
      );
   }

   private JsDatePrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.installConstructor("Date");
      this.install("getTime", 0, this::getTime);
      this.install("valueOf", 0, this::getTime);
      this.install("toString", 0, this::toStringMethod);
      this.install("@@toPrimitive", new JsBuiltinMethod("[Symbol.toPrimitive]", 1, this::toPrimitive));
      this.install("toISOString", 0, this::toISOString);
      this.install("toUTCString", 0, this::toUTCString);
      this.install("toGMTString", 0, this::toUTCString);
      this.install("getFullYear", 0, this::getFullYear);
      this.install("getMonth", 0, this::getMonth);
      this.install("getDate", 0, this::getDate);
      this.install("getDay", 0, this::getDay);
      this.install("getHours", 0, this::getHours);
      this.install("getMinutes", 0, this::getMinutes);
      this.install("getSeconds", 0, this::getSeconds);
      this.install("getMilliseconds", 0, this::getMilliseconds);
      this.install("getYear", 0, this::getYear);
      this.install("setDate", 1, this::setDate);
      this.install("setMonth", 2, this::setMonth);
      this.install("setFullYear", 3, this::setFullYear);
      this.install("setHours", 4, this::setHours);
      this.install("setMinutes", 3, this::setMinutes);
      this.install("setSeconds", 2, this::setSeconds);
      this.install("setMilliseconds", 1, this::setMilliseconds);
      this.install("setTime", 1, this::setTime);
      this.install("setYear", 1, this::setYear);
      this.install("setUTCDate", 1, this::setUTCDate);
      this.install("setUTCMonth", 2, this::setUTCMonth);
      this.install("setUTCFullYear", 3, this::setUTCFullYear);
      this.install("setUTCHours", 4, this::setUTCHours);
      this.install("setUTCMinutes", 3, this::setUTCMinutes);
      this.install("setUTCSeconds", 2, this::setUTCSeconds);
      this.install("setUTCMilliseconds", 1, this::setUTCMilliseconds);
      this.install("toLocaleDateString", 0, this::toLocaleDateString);
      this.install("toLocaleTimeString", 0, this::toLocaleTimeString);
      this.install("toLocaleString", 0, this::toLocaleString);
      this.install("toDateString", 0, this::toDateString);
      this.install("toTimeString", 0, this::toTimeString);
      this.install("toJSON", 1, this::toJSON);
      this.install("getTimezoneOffset", 0, this::getTimezoneOffset);
      this.install("getUTCFullYear", 0, this::getUTCFullYear);
      this.install("getUTCMonth", 0, this::getUTCMonth);
      this.install("getUTCDate", 0, this::getUTCDate);
      this.install("getUTCDay", 0, this::getUTCDay);
      this.install("getUTCHours", 0, this::getUTCHours);
      this.install("getUTCMinutes", 0, this::getUTCMinutes);
      this.install("getUTCSeconds", 0, this::getUTCSeconds);
      this.install("getUTCMilliseconds", 0, this::getUTCMilliseconds);
   }

   private static JsDate requireDate(Context context) {
      if (context.getThisObject() instanceof JsDate d) {
         return d;
      } else {
         throw JsErrorException.typeError("this is not a Date object");
      }
   }

   private static Object boxTime(double v) {
      return Double.isNaN(v) ? Double.NaN : (long)v;
   }

   private static ZonedDateTime localZdt(JsDate d) {
      return ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneId.systemDefault());
   }

   private static ZonedDateTime utcZdt(JsDate d) {
      return ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneOffset.UTC);
   }

   private Object getTime(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : d.getTime();
   }

   private Object toStringMethod(Context context, Object[] args) {
      return requireDate(context).toString();
   }

   private Object toPrimitive(Context context, Object[] args) {
      Object hint = args.length > 0 ? args[0] : Terms.UNDEFINED;
      String h;
      if (!"string".equals(hint) && !"default".equals(hint)) {
         if (!"number".equals(hint)) {
            throw JsErrorException.typeError("invalid hint to Symbol.toPrimitive: " + hint);
         }

         h = "number";
      } else {
         h = "string";
      }

      if (context.getThisObject() instanceof ObjectLike ol) {
         return Terms.ordinaryToPrimitive(ol, h, (CoreContext)context);
      } else {
         throw JsErrorException.typeError("Date.prototype[@@toPrimitive] called on non-object");
      }
   }

   private Object toISOString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         throw JsErrorException.rangeError("Invalid time value");
      } else {
         return formatIso(d);
      }
   }

   private Object toUTCString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? "Invalid Date" : formatUtc(d);
   }

   private Object toJSON(Context context, Object[] args) {
      Object o = context.getThisObject();
      if (o != null && o != Terms.UNDEFINED) {
         CoreContext cc = cc(context);
         Object tv = o;
         if (o instanceof ObjectLike && cc != null) {
            tv = Terms.toPrimitive(o, "number", cc);
            if (cc.isError()) {
               return null;
            }
         } else if (o instanceof JsValue jv) {
            tv = jv.getJsValue();
         }

         if (tv instanceof Number n && !Double.isFinite(n.doubleValue())) {
            return null;
         } else if (o instanceof JsDate d) {
            return formatIso(d);
         } else if (o instanceof ObjectLike ol && ol.getMember("toISOString") instanceof JsCallable jc) {
            CoreContext sub = cc == null ? null : new CoreContext(cc, null, null);
            if (sub != null) {
               sub.thisObject = o;
            }

            Object r = jc.call((Context)(sub == null ? context : sub), new Object[0]);
            if (sub != null && sub.isError() && cc != null) {
               cc.updateFrom(sub);
               return null;
            } else {
               return r;
            }
         } else {
            throw JsErrorException.typeError("toISOString is not a function");
         }
      } else {
         throw JsErrorException.typeError("Cannot convert null or undefined to object");
      }
   }

   private Object getFullYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getYear();
   }

   private Object getYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getYear() - 1900;
   }

   private Object getMonth(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getMonthValue() - 1;
   }

   private Object getDate(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getDayOfMonth();
   }

   private Object getDay(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getDayOfWeek().getValue() % 7;
   }

   private Object getHours(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getHour();
   }

   private Object getMinutes(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getMinute();
   }

   private Object getSeconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : localZdt(d).getSecond();
   }

   private Object getMilliseconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : (int)Math.floorMod(d.getTime(), 1000L);
   }

   private Object getTimezoneOffset(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : -((int)(JsDate.localTzaMs(d.getTime()) / 60000L));
   }

   private Object getUTCFullYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getYear();
   }

   private Object getUTCMonth(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getMonthValue() - 1;
   }

   private Object getUTCDate(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getDayOfMonth();
   }

   private Object getUTCDay(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getDayOfWeek().getValue() % 7;
   }

   private Object getUTCHours(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getHour();
   }

   private Object getUTCMinutes(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getMinute();
   }

   private Object getUTCSeconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : utcZdt(d).getSecond();
   }

   private Object getUTCMilliseconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      return d.isInvalid() ? Double.NaN : (int)Math.floorMod(d.getTime(), 1000L);
   }

   private static double[] coerceArgs(Object[] args, int n, CoreContext cc) {
      double[] out = new double[n];

      for (int i = 0; i < n; i++) {
         if (i >= args.length) {
            out[i] = Double.NaN;
         } else {
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
      }

      return out;
   }

   private static CoreContext cc(Context context) {
      return context instanceof CoreContext c ? c : null;
   }

   private Object setTime(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else {
         d.setTimeValue(c[0]);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setMilliseconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         double local = JsDate.utcToLocal(t);
         ZonedDateTime z = localZdt(d);
         double time = JsDate.makeTime(z.getHour(), z.getMinute(), z.getSecond(), c[0]);
         double newLocal = JsDate.makeDate(Math.floor(local / 8.64E7), time);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setSeconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 2);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = localZdt(d);
         double local = JsDate.utcToLocal(t);
         double useMs = n > 1 ? c[1] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(z.getHour(), z.getMinute(), c[0], useMs);
         double newLocal = JsDate.makeDate(Math.floor(local / 8.64E7), time);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setMinutes(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 3);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = localZdt(d);
         double local = JsDate.utcToLocal(t);
         double useS = n > 1 ? c[1] : z.getSecond();
         double useMs = n > 2 ? c[2] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(z.getHour(), c[0], useS, useMs);
         double newLocal = JsDate.makeDate(Math.floor(local / 8.64E7), time);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setHours(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 4);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = localZdt(d);
         double local = JsDate.utcToLocal(t);
         double useM = n > 1 ? c[1] : z.getMinute();
         double useS = n > 2 ? c[2] : z.getSecond();
         double useMs = n > 3 ? c[3] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(c[0], useM, useS, useMs);
         double newLocal = JsDate.makeDate(Math.floor(local / 8.64E7), time);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setDate(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = localZdt(d);
         double local = JsDate.utcToLocal(t);
         double timeOfDay = local - Math.floor(local / 8.64E7) * 8.64E7;
         double day = JsDate.makeDay(z.getYear(), z.getMonthValue() - 1, c[0]);
         double newLocal = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setMonth(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 2);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = localZdt(d);
         double local = JsDate.utcToLocal(t);
         double timeOfDay = local - Math.floor(local / 8.64E7) * 8.64E7;
         double useDt = n > 1 ? c[1] : z.getDayOfMonth();
         double day = JsDate.makeDay(z.getYear(), c[0], useDt);
         double newLocal = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setFullYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 3);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else {
         ZonedDateTime z;
         double timeOfDay;
         if (Double.isNaN(t)) {
            z = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
            timeOfDay = 0.0;
         } else {
            z = localZdt(d);
            double local = JsDate.utcToLocal(t);
            timeOfDay = local - Math.floor(local / 8.64E7) * 8.64E7;
         }

         double useMo = n > 1 ? c[1] : z.getMonthValue() - 1;
         double useDt = n > 2 ? c[2] : z.getDayOfMonth();
         double day = JsDate.makeDay(c[0], useMo, useDt);
         double newLocal = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(JsDate.localToUtc(newLocal));
         return boxTime(d.getTimeValue());
      }
   }

   private Object setYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else {
         double y = c[0];
         if (Double.isNaN(y)) {
            d.setTimeValue(Double.NaN);
            return Double.NaN;
         } else {
            double iy = y < 0.0 ? Math.ceil(y) : Math.floor(y);
            if (iy >= 0.0 && iy <= 99.0) {
               iy += 1900.0;
            }

            ZonedDateTime z;
            double timeOfDay;
            if (Double.isNaN(t)) {
               z = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
               timeOfDay = 0.0;
            } else {
               z = localZdt(d);
               double local = JsDate.utcToLocal(t);
               timeOfDay = local - Math.floor(local / 8.64E7) * 8.64E7;
            }

            double day = JsDate.makeDay(iy, z.getMonthValue() - 1, z.getDayOfMonth());
            double newLocal = JsDate.makeDate(day, timeOfDay);
            d.setTimeValue(JsDate.localToUtc(newLocal));
            return boxTime(d.getTimeValue());
         }
      }
   }

   private Object setUTCMilliseconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double time = JsDate.makeTime(z.getHour(), z.getMinute(), z.getSecond(), c[0]);
         double v = JsDate.makeDate(Math.floor(t / 8.64E7), time);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCSeconds(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 2);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double useMs = n > 1 ? c[1] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(z.getHour(), z.getMinute(), c[0], useMs);
         double v = JsDate.makeDate(Math.floor(t / 8.64E7), time);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCMinutes(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 3);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double useS = n > 1 ? c[1] : z.getSecond();
         double useMs = n > 2 ? c[2] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(z.getHour(), c[0], useS, useMs);
         double v = JsDate.makeDate(Math.floor(t / 8.64E7), time);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCHours(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 4);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double useM = n > 1 ? c[1] : z.getMinute();
         double useS = n > 2 ? c[2] : z.getSecond();
         double useMs = n > 3 ? c[3] : Math.floorMod((long)t, 1000L);
         double time = JsDate.makeTime(c[0], useM, useS, useMs);
         double v = JsDate.makeDate(Math.floor(t / 8.64E7), time);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCDate(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      double[] c = coerceArgs(args, 1, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double timeOfDay = t - Math.floor(t / 8.64E7) * 8.64E7;
         double day = JsDate.makeDay(z.getYear(), z.getMonthValue() - 1, c[0]);
         double v = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCMonth(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 2);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else if (Double.isNaN(t)) {
         return Double.NaN;
      } else {
         ZonedDateTime z = utcZdt(d);
         double timeOfDay = t - Math.floor(t / 8.64E7) * 8.64E7;
         double useDt = n > 1 ? c[1] : z.getDayOfMonth();
         double day = JsDate.makeDay(z.getYear(), c[0], useDt);
         double v = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private Object setUTCFullYear(Context context, Object[] args) {
      JsDate d = requireDate(context);
      double t = d.getTimeValue();
      int n = Math.min(args.length, 3);
      if (n == 0) {
         n = 1;
      }

      double[] c = coerceArgs(args, n, cc(context));
      if (c == null) {
         return Double.NaN;
      } else {
         ZonedDateTime z;
         double timeOfDay;
         if (Double.isNaN(t)) {
            z = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
            timeOfDay = 0.0;
         } else {
            z = utcZdt(d);
            timeOfDay = t - Math.floor(t / 8.64E7) * 8.64E7;
         }

         double useMo = n > 1 ? c[1] : z.getMonthValue() - 1;
         double useDt = n > 2 ? c[2] : z.getDayOfMonth();
         double day = JsDate.makeDay(c[0], useMo, useDt);
         double v = JsDate.makeDate(day, timeOfDay);
         d.setTimeValue(v);
         return boxTime(d.getTimeValue());
      }
   }

   private static Locale parseLocale(Object[] args) {
      return args.length > 0 && args[0] instanceof String tag ? Locale.forLanguageTag(tag) : Locale.getDefault();
   }

   private Object toLocaleDateString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         return "Invalid Date";
      } else {
         DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(parseLocale(args));
         return localZdt(d).format(formatter);
      }
   }

   private Object toLocaleTimeString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         return "Invalid Date";
      } else {
         DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(parseLocale(args));
         return localZdt(d).format(formatter);
      }
   }

   private Object toLocaleString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         return "Invalid Date";
      } else {
         DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).withLocale(parseLocale(args));
         return localZdt(d).format(formatter);
      }
   }

   private Object toDateString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         return "Invalid Date";
      } else {
         ZonedDateTime z = localZdt(d);
         String dayName = z.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
         String monthName = z.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
         return String.format(Locale.ROOT, "%s %s %02d %s", dayName, monthName, z.getDayOfMonth(), JsDate.formatYear4(z.getYear()));
      }
   }

   private Object toTimeString(Context context, Object[] args) {
      JsDate d = requireDate(context);
      if (d.isInvalid()) {
         return "Invalid Date";
      } else {
         ZonedDateTime z = localZdt(d);
         return String.format(
            Locale.ROOT, "%02d:%02d:%02d %s", z.getHour(), z.getMinute(), z.getSecond(), JsDate.formatGmtOffset((int)(JsDate.localTzaMs(d.getTime()) / 1000L))
         );
      }
   }

   private static LocalDate utcLocalDate(JsDate d) {
      return utcZdt(d).toLocalDate();
   }
}
