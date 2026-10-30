package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ArgBuilder extends JsDispatch {
   private final String command;
   private final List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> axes = new ArrayList<>();

   ArgBuilder(String var1) {
      this.command = var1;
      this.fn("enum", var1x -> this.enumAxis(this.name(arg(var1x, 0)), arg(var1x, 1)));
      this.fn("int", var1x -> this.intAxis(this.name(arg(var1x, 0)), arg(var1x, 1), arg(var1x, 2), arg(var1x, 3)));
      this.fn("bool", var1x -> this.boolAxis(this.name(arg(var1x, 0))));
      this.fn("const", var1x -> this.constAxis(this.name(arg(var1x, 0)), arg(var1x, 1)));
   }

   List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> axes() {
      return List.copyOf(this.axes);
   }

   private Object enumAxis(String var1, Object var2) {
      if (!(var2 instanceof List var3)) {
         throw this.refuse(var1, "must declare an array of values");
      } else {
         List var4;
         try {
            var4 = io.karatelabs.ext.rules.NumericBoundsParser.a(new ArrayList<>(var3));
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var6) {
            throw this.refuse(var1, "needs a non-empty list of values — an empty enum would erase the axis");
         }

         for (int var5 = 0; var5 < var4.size(); var5++) {
            var4.set(var5, this.canonical(var1, "value " + var5, var4.get(var5)));
         }

         List var7 = frozen(var4);
         return this.axis(new io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis(var1, "enum", var7, null, null, var7));
      }
   }

   private Object intAxis(String var1, Object var2, Object var3, Object var4) {
      long var5 = this.bound(var1, "lo", var2);
      long var7 = this.bound(var1, "hi", var3);
      if (var5 > var7) {
         throw this.refuse(var1, "lo is above hi, so the declared domain is empty");
      } else {
         ArrayList<java.lang.Object> var9 = new ArrayList();
         if (var4 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var4)) {
            if (!(var4 instanceof List var10)) {
               throw this.refuse(var1, "the levels must be an array of integers");
            }

            if (var10.isEmpty()) {
               throw this.refuse(var1, "levels: required — name the integers the walk enumerates inside [" + var5 + ", " + var7 + "]");
            }

            for (Object var12 : var10) {
               var9.add(number(this.level(var1, var12, var5, var7)));
            }
         } else {
            if (var5 != var7) {
               throw this.refuse(var1, "levels: required — name the integers the walk enumerates inside [" + var5 + ", " + var7 + "]");
            }

            var9.add(number(var5));
         }

         return this.axis(new io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis(var1, "int", null, var5, var7, frozen(var9)));
      }
   }

   private Object boolAxis(String var1) {
      List<Object> var2 = List.of(false, true);
      return this.axis(new io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis(var1, "bool", var2, null, null, var2));
   }

   private Object constAxis(String var1, Object var2) {
      var var3 = frozen(Collections.singletonList(this.canonical(var1, "value", var2)));
      return this.axis(new io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis(var1, "const", var3, null, null, var3));
   }

   private Object axis(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var1) {
      for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var3 : this.axes) {
         if (var3.a().equals(var1.a())) {
            throw this.refuse(var1.a(), "is declared twice — one argument, one declared domain");
         }
      }

      this.axes.add(var1);
      return this;
   }

   private long bound(String var1, String var2, Object var3) {
      if (var3 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var3)) {
         try {
            return io.karatelabs.ext.rules.NumericBoundsParser.a(var3, true).longValue();
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var5) {
            throw this.refuse(var1, "the " + var2 + " bound must be a whole number, got " + var5.b);
         }
      } else {
         throw this.refuse(var1, "the " + var2 + " bound is required");
      }
   }

   private long level(String var1, Object var2, long var3, long var5) {
      if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         long var7;
         try {
            var7 = io.karatelabs.ext.rules.NumericBoundsParser.a(var2, true).longValue();
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var10) {
            throw this.refuse(var1, "a level must be a whole number, got " + var10.b);
         }

         if (var7 >= var3 && var7 <= var5) {
            return var7;
         } else {
            throw this.refuse(var1, "the level " + var7 + " lies outside the declared domain " + var3 + ".." + var5);
         }
      } else {
         throw this.refuse(var1, "a level must be a whole number, got " + var2);
      }
   }

   private Object canonical(String var1, String var2, Object var3) {
      try {
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var3);
      } catch (SnapshotException var5) {
         throw this.refuse(var1, var2 + ": " + var5.getMessage());
      }

      return io.karatelabs.ext.rules.twin.CanonicalJson.d(var3);
   }

   private String name(Object var1) {
      if (var1 instanceof String var2 && !var2.isEmpty()) {
         return var2;
      } else {
         throw TwinException.a(this.command, "args", "every declared argument needs a name");
      }
   }

   private TwinException refuse(String var1, String var2) {
      return TwinException.a(this.command, "args." + var1, var2);
   }

   private static List<Object> frozen(List<Object> var0) {
      return Collections.unmodifiableList(new ArrayList<>(var0));
   }

   private static Object number(long var0) {
      return var0 >= -2147483648L && var0 <= 2147483647L ? (int)var0 : var0;
   }

   private static Object arg(Object[] var0, int var1) {
      return var0 != null && var0.length > var1 ? var0[var1] : null;
   }
}
