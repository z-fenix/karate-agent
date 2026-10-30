package io.karatelabs.ext.max.run;

import io.karatelabs.ext.agent.events.AgentEvent;
import io.karatelabs.ext.agent.events.AgentEventListener;
import java.util.function.BiConsumer;

public final class HarnessSseBridge implements AgentEventListener {
   private final BiConsumer<String, String> sink;

   public HarnessSseBridge(BiConsumer<String, String> var1) {
      this.sink = var1;
   }

   @Override
   public void onEvent(AgentEvent var1) {
      if (!(var1 instanceof io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent)) {
         this.sink.accept(var1.a(), var1.e());
      }
   }
}
