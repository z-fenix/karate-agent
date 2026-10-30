package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.List;

public enum SelectorDialect {
   JS("js"),
   JSONPATH_PLUS("jsonpath-plus");

   private final String wire;

   private SelectorDialect(String var3) {
      this.wire = var3;
   }

   public String wire() {
      return this.wire;
   }

   public static SelectorDialect of(String var0) {
      for (SelectorDialect var4 : values()) {
         if (var4.wire.equals(var0)) {
            return var4;
         }
      }

      throw new LintException("unknown givenType: '" + var0 + "' (known: " + String.join(", ", names()) + ")");
   }

   public static List<String> names() {
      ArrayList<java.lang.String> var0 = new ArrayList();

      for (SelectorDialect var4 : values()) {
         var0.add(var4.wire);
      }

      return var0;
   }
}
