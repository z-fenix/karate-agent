package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.List;

public enum GovernanceDimension {
   DESIGN("design", "design & style consistency"),
   SECURITY("security", "security hygiene (bundled rules only — not a posture grade)"),
   DOCUMENTATION("documentation", "docs & error-model completeness"),
   PROVENNESS("provenness", "proven-ness — claimed by a requirement, exercised, not mock-only"),
   CONTRACT("contract", "contract-test depth (mock vs provider)");

   private final String wire;
   private final String label;

   private GovernanceDimension(String var3, String var4) {
      this.wire = var3;
      this.label = var4;
   }

   public String wire() {
      return this.wire;
   }

   public String label() {
      return this.label;
   }

   public static List<String> names() {
      ArrayList<java.lang.String> var0 = new ArrayList();

      for (GovernanceDimension var4 : values()) {
         var0.add(var4.wire);
      }

      return var0;
   }

   public static GovernanceDimension of(String var0) {
      for (GovernanceDimension var4 : values()) {
         if (var4.wire.equals(var0)) {
            return var4;
         }
      }

      var var5 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var0, names());
      throw new LintException(
         "unknown dimension: '"
            + var0
            + "'"
            + (var5.isEmpty() ? "" : " — did you mean " + String.join(" / ", var5) + "?")
            + " (known: "
            + String.join(", ", names())
            + ")"
      );
   }
}
