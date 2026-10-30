package io.karatelabs.ext.coverage.dim;

import java.util.Map;

public interface DimensionOracle {
   Map<String, Object> ranges(String var1, Object var2);

   boolean feasible(String var1, Map<String, Object> var2);
}
