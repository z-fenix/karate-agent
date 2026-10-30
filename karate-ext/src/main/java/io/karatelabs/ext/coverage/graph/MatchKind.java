package io.karatelabs.ext.coverage.graph;

import java.util.Locale;

public enum MatchKind {
   EXACT,
   TEMPLATED,
   AMBIGUOUS;

   public String wire() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}
