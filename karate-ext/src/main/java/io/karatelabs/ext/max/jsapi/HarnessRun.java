package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentRunState;
import io.karatelabs.ext.robot.BotDriver;
import io.karatelabs.ext.robot.RobotAgentExtension;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HarnessRun {
   private String id;
   private String mode;
   private String task;
   private String check;
   private String status = "idle";
   private String error;
   private volatile boolean stopRequested;
   private Path transcript;
   private long startedMs;
   private io.karatelabs.ext.agent.harness.HarnessResult result;
   private volatile io.karatelabs.ext.agent.runtime.AgentLoop loop;

   public synchronized boolean tryStart(String var1, Path var2, String var3, String var4) {
      return this.tryStart(var1, var2, var3, var4, null);
   }

   public synchronized boolean tryStart(String var1, Path var2, String var3, String var4, String var5) {
      if ("running".equals(this.status)) {
         return false;
      } else {
         this.id = var1;
         this.check = var5;
         this.transcript = var2;
         this.mode = var3;
         this.task = var4;
         this.status = "running";
         this.error = null;
         this.result = null;
         this.stopRequested = false;
         this.startedMs = System.currentTimeMillis();
         return true;
      }
   }

   static String terminalStatus(io.karatelabs.ext.agent.harness.HarnessResult var0, boolean var1) {
      if (var0 == null) {
         return "failed";
      } else if (var0.isSuccess()) {
         return "completed";
      } else if (var0.isBlocked()) {
         return "blocked";
      } else {
         io.karatelabs.ext.agent.runtime.AgentRunState.Status var2 = var0.state().status();
         if (!var1 || var2 != io.karatelabs.ext.agent.runtime.AgentRunState.Status.TERMINATED && var2 != io.karatelabs.ext.agent.runtime.AgentRunState.Status.FAILED) {
            return var2 != io.karatelabs.ext.agent.runtime.AgentRunState.Status.TERMINATED && var2 != io.karatelabs.ext.agent.runtime.AgentRunState.Status.STUCK ? "failed" : "terminated";
         } else {
            return "stopped";
         }
      }
   }

   public synchronized String complete(io.karatelabs.ext.agent.harness.HarnessResult var1) {
      this.result = var1;
      this.status = terminalStatus(var1, this.stopRequested);
      if (var1 != null && !var1.isSuccess()) {
         this.error = var1.errorMessage();
      }

      this.loop = null;
      return this.status;
   }

   public synchronized void fail(String var1) {
      this.error = var1;
      this.status = "failed";
      this.loop = null;
   }

   public synchronized void settle(String var1) {
      if ("running".equals(this.status)) {
         this.status = var1;
      }

      this.loop = null;
   }

   public void attachLoop(io.karatelabs.ext.agent.runtime.AgentLoop var1) {
      this.loop = var1;
      if (this.stopRequested) {
         var1.stop();
      }
   }

   public void pause() {
      io.karatelabs.ext.agent.runtime.AgentLoop var1 = this.loop;
      if (var1 != null) {
         var1.pause();
      }
   }

   public void resume(String var1) {
      io.karatelabs.ext.agent.runtime.AgentLoop var2 = this.loop;
      if (var2 != null) {
         var2.resume(var1);
      }
   }

   public boolean awaitLoopPaused(long var1) {
      io.karatelabs.ext.agent.runtime.AgentLoop var3 = this.loop;
      return var3 != null && var3.awaitPaused(var1);
   }

   public BotDriver loopBotDriver() {
      io.karatelabs.ext.agent.runtime.AgentLoop var1 = this.loop;
      if (var1 == null) {
         return null;
      } else {
         RobotAgentExtension var2 = var1.extension(RobotAgentExtension.class);
         return var2 == null ? null : var2.a();
      }
   }

   public Path transcript() {
      return this.transcript;
   }

   public void stop() {
      this.stopRequested = true;
      io.karatelabs.ext.agent.runtime.AgentLoop var1 = this.loop;
      if (var1 != null) {
         var1.stop();
      }
   }

   public synchronized boolean isRunning() {
      return "running".equals(this.status);
   }

   public synchronized String id() {
      return this.id;
   }

   public synchronized String check() {
      return "running".equals(this.status) ? this.check : null;
   }

   public synchronized String status() {
      return this.status;
   }

   public synchronized Map<String, Object> toMap() {
      LinkedHashMap var1 = new LinkedHashMap();
      if (this.id == null) {
         var1.put("status", "idle");
         var1.put("hint", "no harness run yet — Harness.explore(task, {robot:true, async:true}) to start one");
         return var1;
      } else {
         var1.put("runId", this.id);
         var1.put("status", this.status);
         var1.put("mode", this.mode);
         var1.put("task", this.task);
         var1.put("startedMs", this.startedMs);
         if (this.transcript != null) {
            var1.put("transcript", this.transcript.toString());
         }

         if (this.result != null) {
            var1.put("iterations", this.result.state().iterations());
            var1.put("succeeded", this.result.isSuccess());
            var1.put(this.result.isSuccess() ? "result" : "error", this.result.isSuccess() ? this.result.result() : this.result.errorMessage());
            if (this.result.blockedResult() != null) {
               var1.put("blocker", this.result.blockedResult());
            }

            if (this.result.statement() != null) {
               var1.put("statement", this.result.statement());
            }

            var1.put("tokens", this.result.totalTokens());
            LinkedHashMap var2 = new LinkedHashMap();
            var2.put("input", this.result.state().inputTokens());
            var2.put("output", this.result.state().outputTokens());
            var2.put("cacheRead", this.result.state().cacheReadTokens());
            var2.put("cacheCreation", this.result.state().cacheCreationTokens());
            var1.put("tokenBreakdown", var2);
         } else if (this.error != null) {
            var1.put("error", this.error);
         }

         return var1;
      }
   }
}
