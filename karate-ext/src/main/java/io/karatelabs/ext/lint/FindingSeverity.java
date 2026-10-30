package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.List;

public enum FindingSeverity {
   ERROR("error"),
   WARN("warn"),
   INFO("info");

   private final String wire;

   private FindingSeverity(String var3) {
      this.wire = var3;
   }

   public String wire() {
      return this.wire;
   }

   public static FindingSeverity of(String var0) {
      for (FindingSeverity var4 : values()) {
         if (var4.wire.equals(var0)) {
            return var4;
         }
      }

      throw new LintException("unknown severity: '" + var0 + "' (known: " + String.join(", ", names()) + ")");
   }

   public static List<String> names() {
      ArrayList<java.lang.String> var0 = new ArrayList();

      for (FindingSeverity var4 : values()) {
         var0.add(var4.wire);
      }

      return var0;
   }
}
