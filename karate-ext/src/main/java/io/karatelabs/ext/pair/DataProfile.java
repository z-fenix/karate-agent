package io.karatelabs.ext.pair;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum DataProfile {
   SEEDED(
      "seeded",
      "the suite creates everything it needs through the API; the mock starts empty by construction. THE GUARANTEE IS ONE-SIDED: only the mock leg is empty by construction — nothing here empties the PROVIDER, and state it carried in (an earlier run's rows, a long-lived environment) is compared as though the suite had created it. A list endpoint returning more elements on the provider than on the mock is that, not drift. Start the provider clean — `mock.restart()` if it is a stand-in you started here, same port and the handle stays valid — or declare `fixture`/`ambient` and take their ceiling. The only profile from which `proven substitutable` is reachable."
   ),
   FIXTURE(
      "fixture",
      "both legs are pre-loaded from one declared fixture, whose digest becomes a freshness binding. Provisioning the provider side is the project's step, not the harness's."
   ),
   AMBIENT(
      "ambient",
      "the provider holds state we did not create and cannot enumerate, so equivalence CANNOT be established: reads over ambient data land in `notmeasured` and this suite's ceiling sits BELOW `proven substitutable`. Choosing it is choosing a ceiling."
   );

   private final String wire;
   private final String means;

   private DataProfile(String var3, String var4) {
      this.wire = var3;
      this.means = var4;
   }

   public String wire() {
      return this.wire;
   }

   public String means() {
      return this.means;
   }

   public static DataProfile of(Object var0) {
      if (var0 == null) {
         return SEEDED;
      } else {
         String var1 = String.valueOf(var0).trim().toLowerCase(Locale.ROOT);

         for (DataProfile var5 : values()) {
            if (var5.wire.equals(var1)) {
               return var5;
            }
         }

         throw new PairException(
            "unknown data profile '"
               + var0
               + "' — one of "
               + Arrays.stream(values()).map(DataProfile::wire).collect(Collectors.joining(", "))
               + ". The profile is a claim about how the two legs came to be in the same state, so there is no default that could be inferred from the run."
         );
      }
   }
}
