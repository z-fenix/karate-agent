package io.karatelabs.ext.mcp;

import io.karatelabs.core.Channel;
import io.karatelabs.core.ScenarioRuntime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpChannel implements Channel {
   static final Logger a = LoggerFactory.getLogger("karate.mcp");
   private final McpExt c;
   final List<McpSession> b = new ArrayList<>();

   public McpChannel(McpExt var1) {
      this.c = var1;
   }

   public McpSession a(ScenarioRuntime var1) {
      McpSession var2 = new McpSession();
      if (this.c != null) {
         this.c.seedDefaults(var2);
      }

      this.b.add(var2);
      return var2;
   }

   @Override
   public Object init(ScenarioRuntime runtime) {
      return this.a(runtime);
   }

   @Override
   public void afterScenario() {
      for (McpSession var2 : this.b) {
         try {
            var2.stop();
         } catch (Exception var4) {
            a.warn("{}", var4.getMessage());
         }
      }

      this.b.clear();
   }
}
