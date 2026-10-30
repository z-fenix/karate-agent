package io.karatelabs.core;

import java.util.Map;

public interface ChannelFactory {
   Channel create(ScenarioRuntime var1, Map<String, Object> var2);
}
