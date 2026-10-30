package io.karatelabs.driver;

import io.karatelabs.core.ScenarioRuntime;
import java.util.Map;

public interface DriverProvider {
   Driver acquire(ScenarioRuntime var1, Map<String, Object> var2);

   void release(ScenarioRuntime var1, Driver var2);

   void shutdown();
}
