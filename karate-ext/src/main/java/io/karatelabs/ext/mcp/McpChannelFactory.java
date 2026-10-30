package io.karatelabs.ext.mcp;

import io.karatelabs.core.Channel;
import io.karatelabs.core.ChannelFactory;
import io.karatelabs.core.ScenarioRuntime;
import java.util.Map;

public class McpChannelFactory implements ChannelFactory {
   private static volatile boolean a;
   private final McpExt b;

   public McpChannelFactory() {
      this(null);
   }

   public McpChannelFactory(McpExt var1) {
      this.b = var1;
   }

   @Override
   public Channel create(ScenarioRuntime var1, Map<String, Object> var2) {
      if (!a) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("mcp", "openapi");
         a = true;
      }

      return new McpChannel(this.b);
   }
}
