package io.karatelabs.ext.robot.recording;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class TrafficRecorderTest {
   @Test
   public void capturesResponseForRequestWithBodyAndNoResourceType() {
      AtomicInteger calls = new AtomicInteger();
      CdpPageApi page = new CdpPageApi() {
         public CompletableFuture<ScriptEvaluationResult> a(String id) {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(new ScriptEvaluationResult("response", false));
         }
         public CompletableFuture<String> b(String id) { return CompletableFuture.completedFuture(null); }
      };
      TrafficRecorder recorder = new TrafficRecorder(Path.of("."), TrafficRecorder.TrafficRecorderLimits.a, page, null);
      recorder.onEvent("Network.requestWillBeSent", Map.of("requestId", "1", "request", Map.of("url", "https://example.com", "method", "POST", "postData", "body")));
      recorder.onEvent("Network.responseReceived", Map.of("requestId", "1", "response", Map.of("mimeType", "text/plain")));
      recorder.onEvent("Network.loadingFinished", Map.of("requestId", "1", "encodedDataLength", 8));
      recorder.c("1"); // Drain the event queue before checking the body fetch.
      assertEquals(1, calls.get());
   }
}
