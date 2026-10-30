package io.karatelabs.ext.rules;

import io.karatelabs.ext.agent.llm.LlmProvider;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentLlmPackageTest {
   @Test
   public void redactionAliasesAndLocationComponentsRemainUsable() {
      var registry = new SecretRedactor.SecretAliasRegistry();
      String alias = registry.aliasFor("private-value");
      assertEquals(alias, registry.aliasFor("private-value"));
      assertEquals(1, registry.size());
      var location = new SecretRedactor.RedactionLocation(2, "password=private-value");
      assertEquals(2, location.lineNumber());
      assertEquals("password=private-value", location.lineText());
      assertEquals("lineNumber", SecretRedactor.RedactionLocation.class.getRecordComponents()[0].getName());
      var redactor = new SecretRedactor(Map::of);
      assertFalse(redactor.redactWithAliasRegistry("password=private-value", registry).contains("private-value"));
   }
   @Test
   public void selectsProviderAndRedactsSecrets() {
      Object[] model = LlmProvider.parseModel("openai/model", LlmProvider.OLLAMA);
      assertEquals(LlmProvider.OPENAI, model[0]);
      assertEquals("model", model[1]);
      String result = new SecretRedactor(Map::of).apply("password=abc123");
      assertTrue(result.startsWith("password="));
      assertFalse(result.contains("abc123"));
   }
}
