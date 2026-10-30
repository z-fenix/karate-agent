package io.karatelabs.ext.coverage.collect;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class CoveragePathFilterRegistry {
   private static final List<Predicate<String>> a = new CopyOnWriteArrayList<>();

   private CoveragePathFilterRegistry() {
   }

   public static void a(Predicate<String> var0) {
      if (var0 != null) {
         a.add(var0);
      }
   }

   public static boolean a(String var0) {
      if (var0 == null) {
         return false;
      } else {
         for (Predicate var2 : a) {
            try {
               if (var2.test(var0)) {
                  return true;
               }
            } catch (RuntimeException var4) {
            }
         }

         return false;
      }
   }
}
