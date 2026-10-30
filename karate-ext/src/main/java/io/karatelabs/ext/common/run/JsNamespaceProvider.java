package io.karatelabs.ext.common.run;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface JsNamespaceProvider {
   String name();

   Object namespace();

   default Object namespace(Path var1) {
      return this.namespace();
   }

   default String skill() {
      return null;
   }

   static List<JsNamespaceProvider> discover() {
      ArrayList<io.karatelabs.ext.common.run.JsNamespaceProvider> var0 = new ArrayList();

      for (JsNamespaceProvider var2 : ServiceLoader.load(JsNamespaceProvider.class)) {
         var0.add(var2);
      }

      return var0;
   }
}
