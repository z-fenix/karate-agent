package io.karatelabs.ext.agent.harness;

import io.karatelabs.ext.agent.runtime.AgentRunState;
import java.nio.file.Path;

public record HarnessResult(String runId, AgentRunState state, Path transcriptPath) {

   public HarnessResult(String runId, AgentRunState state, Path transcriptPath) {
      this.runId = runId;
      this.state = state;
      this.transcriptPath = transcriptPath;
   }

   public boolean isSuccess() {
      return Boolean.TRUE.equals(this.state.success());
   }

   public Object result() {
      return this.isSuccess() ? this.state.finalResult() : null;
   }

   public boolean isBlocked() {
      return this.state.status() == io.karatelabs.ext.agent.runtime.AgentRunState.Status.BLOCKED;
   }

   public Object blockedResult() {
      return this.isBlocked() ? this.state.finalResult() : null;
   }

   public String statement() {
      return this.state.statement();
   }

   public String errorMessage() {
      if (this.isSuccess()) {
         return null;
      } else {
         return this.state.lastError() != null ? this.state.lastError() : "agent did not signal success";
      }
   }

   public String g() {
      return this.isSuccess() ? null : this.state.m();
   }

   public String h() {
      return this.isSuccess() ? null : this.state.n();
   }

   public int totalTokens() {
      return this.state.totalTokens();
   }

   public String runId() {
      return this.runId;
   }

   public AgentRunState state() {
      return this.state;
   }

   public Path transcriptPath() {
      return this.transcriptPath;
   }
}
