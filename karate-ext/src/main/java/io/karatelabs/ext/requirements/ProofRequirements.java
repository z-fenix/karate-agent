package io.karatelabs.ext.requirements;

import io.karatelabs.js.ObjectLike;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ProofRequirements {
   private ProofRequirements() {
   }

   public static Map<String, List<String>> a(Object var0) {
      if (var0 == null) {
         return Map.of();
      } else {
         Map var1;
         if (var0 instanceof ObjectLike var3) {
            var1 = var3.toMap();
         } else {
            if (!(var0 instanceof Map var2)) {
               throw new IllegalArgumentException(
                  "cov.requireProofs must be an object of { '<criterion id>': '<proof definition id>' | [ids] }, not " + var0.getClass().getSimpleName()
               );
            }

            var1 = var2;
         }

         LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var7 = new LinkedHashMap();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            var var6 = io.karatelabs.ext.common.JsCallback.b(var5.getValue());
            if (var6.isEmpty()) {
               throw new IllegalArgumentException("cov.requireProofs['" + (String)var5.getKey() + "'] names no proof definition id");
            }

            var7.put((String)var5.getKey(), List.copyOf(var6));
         }

         return var7;
      }
   }
}
