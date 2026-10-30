package io.karatelabs.ext.rules;

import io.karatelabs.ext.coverage.KafkaAction;
import io.karatelabs.ext.rules.analysis.NumericInterval;
import io.karatelabs.js.JavaCallable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompilationRegressionTest {
   @Test
   public void ruleDiffLinesPreserveTheirKindsAndText() {
      assertEquals(List.of(Map.of("kind", "unchanged", "text", "same"),
         Map.of("kind", "removed", "text", "old"),
         Map.of("kind", "added", "text", "new")),
         RuleTreeJsonMapper.a(RuleTreeDiffer.a("same\nold", "same\nnew")));
   }

   @Test
   public void requirementIdsAcceptScalarsAndDeduplicateLists() {
      assertEquals(List.of(), RulebookRepository.b((Map<String, Object>) null));
      assertEquals(List.of("req/one"), RulebookRepository.b(Map.of("_req", "req/one")));
      assertEquals(List.of("req/one", "req/two"),
         RulebookRepository.b(Map.of("_req", List.of("req/one", "req/two", "req/one"))));
   }

   @Test
   public void intervalOperationsUseTheCorrectRecordFields() {
      NumericInterval range = NumericInterval.a(2.0, 4.0, true);
      NumericInterval sum = range.b(NumericInterval.a(3.0));
      assertEquals(5.0, sum.g(), 0.0);
      assertEquals(7.0, sum.h(), 0.0);
      assertTrue(sum.i());
      assertFalse(range.a());
      assertTrue(NumericInterval.a(3.0).a());
      assertTrue(range.b(3.0));
      assertFalse(range.b(5.0));
   }

   @Test
   public void calcRoundsHalfUpAndPreservesMissingValues() {
      JavaCallable round = (JavaCallable) new Calc(Map.of()).jsGet("round");
      assertEquals(1.24, ((Number) round.call(null, 1.235, 2)).doubleValue(), 0.0);
      assertNull(round.call(null));
      assertTrue(Double.isNaN(((Number) round.call(null, Double.NaN)).doubleValue()));
   }

   @Test
   public void schemaStillChecksRangesAndRequiredKeys() {
      Map<String, Object> schema = Map.of("age", "#int[0,6]", "name", "#string");
      assertTrue(Schema.validateAgainst(schema, Map.of("age", 3, "name", "test")).pass);
      assertFalse(Schema.validateAgainst(schema, Map.of("age", 7, "name", "test")).pass);
      assertFalse(Schema.validateAgainst(schema, Map.of("age", 3)).pass);
   }

   @Test
   public void logNodesRetainTheirComparableOrdering() {
      ExecutionTraceNode earlier = new ExecutionTraceNode("file", 2, null, false, "first");
      ExecutionTraceNode later = new ExecutionTraceNode("file", 5, null, false, "second");
      assertTrue(earlier.compareTo(later) < 0);
      assertTrue(later.compareTo(earlier) > 0);
   }

   @Test
   public void dimensionCoverageGroupsObservedAxisValues() {
      Map<String, Object> item = Map.of("id", "GET /pets", "dimAxes",
         Map.of("region", Map.of("universe", List.of("eu", "us"))));
      Map<String, Object> hit = Map.of("item", "GET /pets", "attrs", Map.of("dim", Map.of("region", "eu")));
      Map<String, Object> graph = Map.of("items", List.of(item), "hits", List.of(hit));
      Map<?, ?> result = io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(graph, null);
      Map<?, ?> first = (Map<?, ?>)((List<?>)result.get("items")).get(0);
      Map<?, ?> region = (Map<?, ?>)((Map<?, ?>)first.get("axes")).get("region");
      assertEquals(List.of("eu"), region.get("seen"));
   }

   @Test
   public void harnessOutcomePreservesRecordEqualityAndStatus() {
      io.karatelabs.ext.agent.runtime.AgentOutcome result = io.karatelabs.ext.agent.runtime.AgentOutcome.success((Object)"ok");
      assertTrue(result.isSuccess());
      assertEquals(io.karatelabs.ext.agent.runtime.AgentOutcome.Kind.SUCCESS, result.kind());
      assertEquals(io.karatelabs.ext.agent.runtime.AgentRunState.Status.COMPLETED, result.status());
      assertEquals(result, io.karatelabs.ext.agent.runtime.AgentOutcome.success((Object)"ok"));
   }

   @Test
   public void postmanAuthInspectionUsesTheModelValidator() {
      assertNull(io.karatelabs.ext.postman.editor.PostmanCollectionEditor.c(Map.of("type", "bearer")));
      assertNotNull(io.karatelabs.ext.postman.editor.PostmanCollectionEditor.c(Map.of("type", "oauth2", "grant_type", "password")));
   }

   @Test
   public void mcpChannelInitializesAndCleansUpSessions() {
      io.karatelabs.core.Channel channel = new io.karatelabs.ext.mcp.McpChannel(null);
      assertTrue(channel.init(null) instanceof io.karatelabs.ext.mcp.McpSession);
      channel.afterScenario();
   }

   @Test
   public void unaryRedactorUsesTheSameSanitizationPath() {
      io.karatelabs.ext.agent.llm.SecretRedactor redactor = new io.karatelabs.ext.agent.llm.SecretRedactor(Map::of);
      assertEquals("hello world", redactor.apply("hello world"));
      String sanitized = redactor.apply("password=abc123");
      assertTrue(sanitized.startsWith("password="));
      assertFalse(sanitized.contains("abc123"));
   }

   @Test
   public void kafkaActionRetainsItsPayloadAccessorAndRecordComponent() {
      Object payload = Map.of("amount", 3);
      KafkaAction action = new KafkaAction("id", Set.of(), Instant.EPOCH, "test.feature",
         "scenario", 1, "slug", "topic", "schema", "publish", "passed", 10L, payload);
      assertSame(payload, action.payload());
      assertSame(payload, action.dimensionInput());
      assertEquals("topic#publish", action.itemId());
      assertTrue(Arrays.stream(KafkaAction.class.getRecordComponents())
         .anyMatch(component -> component.getName().equals("payload")));
   }
}
