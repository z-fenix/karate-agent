package io.karatelabs.ext.rules;

import io.karatelabs.ext.agent.runtime.RetryPolicy;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.ToolResult;
import io.karatelabs.ext.agent.llm.LlmProvider;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentRuntimeNamesTest {
   @Test public void retriesUseExponentialBackoffAndCap() {
      RetryPolicy policy = new RetryPolicy(3, 100, 2.0, 250);
      assertEquals(100, policy.backoffMillis(0));
      assertEquals(200, policy.backoffMillis(1));
      assertEquals(250, policy.backoffMillis(3));
      try { new RetryPolicy(-1, 0, 1, 0); fail("negative retries"); }
      catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("maxRetries")); }
   }
   @Test public void outcomeAndToolResultPreserveData() {
      AgentOutcome outcome = AgentOutcome.success((Object) "done");
      assertTrue(outcome.isSuccess());
      assertEquals("done", outcome.value());
      ToolResult result = new ToolResult("value", List.of());
      assertEquals("value", result.value());
      assertFalse(result.hasArtifacts());
   }
   @Test public void renamedModelsRetainConstructorAndWireSemantics() {
      var blocked = AgentOutcome.blocked("approval", "waiting");
      assertEquals(AgentOutcome.Kind.BLOCKED, blocked.kind());
      assertEquals("approval", blocked.value());
      assertEquals("waiting", blocked.message());
      assertEquals(List.of(), new ToolResult("value", null).artifacts());
      assertEquals("maxRetries", RetryPolicy.class.getRecordComponents()[0].getName());
      var message = io.karatelabs.ext.agent.llm.LlmMessage.userBuilder()
         .text("hello").png(new byte[]{1, 2, 3}).build();
      assertEquals("hello", message.text());
      assertTrue(message.hasImages());
      assertEquals("AQID", message.parts().get(1).base64());
      assertEquals("image/png", message.parts().get(1).mediaType());
   }
   @Test public void providerModelPrefixOverridesDefault() {
      Object[] model = LlmProvider.parseModel("openai/model", LlmProvider.OLLAMA);
      assertEquals(LlmProvider.OPENAI, model[0]);
      assertEquals("model", model[1]);
   }
}
