package io.karatelabs.core;

import io.karatelabs.common.Resource;

public interface KarateJsContext {
   ScenarioRuntime getRuntime();

   KarateConfig getConfig();

   Resource getWorkingDir();
}
