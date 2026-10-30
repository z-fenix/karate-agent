package io.karatelabs.js;

import java.util.HashMap;
import java.util.Map;

final class PrivateEnv {
   private final PrivateEnv parent;
   private final Map<String, PrivateName> names = new HashMap<>(4);

   PrivateEnv(PrivateEnv parent) {
      this.parent = parent;
   }

   PrivateName declare(String name, PrivateName.Kind kind) {
      PrivateName existing = this.names.get(name);
      if (existing != null) {
         return existing;
      } else {
         PrivateName pn = new PrivateName(name, kind);
         this.names.put(name, pn);
         return pn;
      }
   }

   PrivateName resolve(String name) {
      for (PrivateEnv e = this; e != null; e = e.parent) {
         PrivateName pn = e.names.get(name);
         if (pn != null) {
            return pn;
         }
      }

      return null;
   }
}
