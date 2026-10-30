package io.karatelabs.ext.rules.analysis;

import java.util.List;

public interface SplitTreeListener {
   SplitTreeListener b = new SplitTreeListener() {};

   default void a(List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, long[] var2, long[] var3) {
   }

   default void a(int var1, long var2) {
   }

   default void a(long[] var1, long[] var2, io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome var3, String var4, int var5) {
   }

   public static enum SplitOutcome {
      ACCEPTED,
      REFUSED,
      UNRESOLVED;
   }
}
