package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.agent.events.*;
import io.karatelabs.ext.agent.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentSemanticNamesTest {
   @Test
   public void listenerCallbacksPublishExpectedEvents() {
      List<AgentEvent> received = new ArrayList<>();
      try (AgentEventBus bus = new AgentEventBus("callbacks")) {
         bus.subscribe(received::add, true);
         var listener = new AgentEventReporter(bus);
         listener.onIterationStart(3);
         listener.onToolRequest("call()");
         listener.onToolResult("call()", "ok");
         listener.onPaused();
      }
      assertEquals(List.of("iteration_start", "tool_request", "tool_result", "paused"),
         received.stream().map(event -> event.toMap().get("type")).toList());
      assertEquals("ok", received.get(2).toMap().get("result"));
   }
   @Test
   public void builderPreservesJsonHeadersAndDefaults() {
      AgentHttpRequest request = AgentHttpRequestBuilder.post("/api")
         .bearerToken("test-token").header("Accept", "application/json")
         .body(java.util.Map.of("enabled", true)).build();
      assertEquals("POST", request.method());
      assertEquals("/api", request.url());
      assertEquals(request.url(), request.requestUri());
      assertEquals("Bearer test-token", request.header("Authorization"));
      assertEquals("application/json", request.header("Content-Type"));
      assertEquals("{\"enabled\":true}", new String(request.body(), StandardCharsets.UTF_8));
      assertEquals("GET", new AgentHttpRequestBuilder().url("/health").build().method());
   }

   @Test
   public void httpExceptionRetainsResponseDetails() {
      AgentHttpException error = new AgentHttpException(429, "rate limited");
      assertEquals(429, error.statusCode());
      assertEquals("rate limited", error.responseBody());
      assertEquals("HTTP 429: rate limited", error.getMessage());
      try (AgentHttpClient client = new AgentHttpClient()) {
         client.cancel();
      }
   }

   @Test
   public void eventBusDrainsQueuedEventsAndClosesListeners() {
      List<AgentEvent> received = new ArrayList<>();
      boolean[] closed = {false};
      AgentEvent event = AgentEvent.AnnotationEvent.a("review", 42L);
      AgentEventListener listener = new AgentEventListener() {
         public void onEvent(AgentEvent value) { received.add(value); }
         public void close() { closed[0] = true; }
      };
      try (AgentEventBus bus = new AgentEventBus("run-test")) {
         assertEquals("run-test", bus.runId());
         bus.subscribe(listener);
         AgentEventEmitter emitter = bus::publish;
         emitter.emit(event);
      }
      assertEquals(List.of(event), received);
      assertTrue(closed[0]);
   }

   @Test
   public void transcriptPreservesContextAndEventWireFormat() throws Exception {
      var path = Files.createTempFile("agent-events-", ".jsonl");
      try {
         try (AgentEventFileWriter writer = new AgentEventFileWriter(path, "run-test", "task", "model",
               Set.of(), null, new AgentRunContext("chat-test", "turn-test", "previous-run"))) {
            assertEquals(path, writer.path());
            writer.onEvent(AgentEvent.AnnotationEvent.a("review", 42L));
         }
         var lines = Files.readAllLines(path);
         assertEquals(2, lines.size());
         var start = Json.of(lines.get(0)).asMap();
         assertEquals("run_start", start.get("type"));
         assertEquals("chat-test", start.get("chatId"));
         assertEquals("turn-test", start.get("turnId"));
         assertEquals("previous-run", start.get("prevRunId"));
         var event = Json.of(lines.get(1)).asMap();
         assertEquals("annotation", event.get("type"));
         assertEquals("review", event.get("text"));
         assertEquals(42, ((Number) event.get("attachedTo")).intValue());
      } finally {
         Files.deleteIfExists(path);
      }
   }
}
