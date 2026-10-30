package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CalcFunctionHost {
   private RulebookRuntime a;
   private boolean b;
   private int c;
   private int d;

   void a(RulebookRuntime var1) {
      this.a = var1;
      this.b = true;
      this.c = 0;
      this.d = 0;
   }

   void a() {
      this.b = false;
   }

   int b() {
      return this.c;
   }

   int c() {
      return this.d;
   }

   Object a(Object var1, Object var2) {
      if (!this.b) {
         throw new TwinException(400, "CALC_OUTSIDE_APPLY", "t.calc is callable inside apply only");
      } else {
         a(var1);
         String var3 = b(var2);
         if (this.a == null) {
            throw a("no rulebook is bound to this walk");
         } else {
            this.c++;

            io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookInvocation var4;
            try {
               var4 = this.a.a(var3, var1);
            } catch (RuntimeException var6) {
               this.d++;
               throw var6;
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("outcome", var4.c());
            var5.put("output", var4.d());
            return var5;
         }
      }
   }

   private static void a(Object var0) {
      try {
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var0);
      } catch (SnapshotException var2) {
         throw a("input " + var2.getMessage());
      }

      if (var0 instanceof JavaCallable || var0 instanceof List || !(var0 instanceof Map)) {
         throw a("input must be an object");
      }
   }

   private static String b(Object var0) {
      if (var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         if (!(var0 instanceof JavaCallable) && !(var0 instanceof List) && var0 instanceof Map var1) {
            for (Object var3 : var1.keySet()) {
               if (!"rule".equals(var3)) {
                  throw a("opts carries only rule, not '" + var3 + "'");
               }
            }

            if (var1.get("rule") instanceof String var4) {
               return var4;
            } else {
               throw a("opts.rule must be a rulebook name");
            }
         } else {
            throw a("opts must be an object");
         }
      } else {
         return null;
      }
   }

   private static TwinException a(String var0) {
      return new TwinException(400, "CALC_USAGE", "t.calc(input, opts?) — " + var0);
   }
}
