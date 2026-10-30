package io.karatelabs.ext.robot.recording;

import java.util.concurrent.CompletableFuture;

public interface CdpPageApi {
   CompletableFuture<io.karatelabs.ext.robot.recording.CdpPageApi.ScriptEvaluationResult> a(String var1);

   CompletableFuture<String> b(String var1);

   public static final class CdpPageException extends RuntimeException {
      public CdpPageException(String var1) {
         super(var1);
      }
   }

   public record ScriptEvaluationResult(String a, boolean b) {
   }
}
