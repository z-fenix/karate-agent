package io.karatelabs.ext.rules;

import io.karatelabs.ext.agent.events.AgentEvent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AgentEventsPackageTest {
   @Test
   public void annotationEventsRetainTheirWireFields() {
      var event = AgentEvent.AnnotationEvent.a("review", 42L).toMap();
      assertEquals("annotation", event.get("type"));
      assertEquals("review", event.get("text"));
      assertEquals(42L, event.get("attachedTo"));
   }
}
