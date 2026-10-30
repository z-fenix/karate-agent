package io.karatelabs.ext.agent.runtime;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AgentRunState {
   private io.karatelabs.ext.agent.runtime.AgentRunState.Status status = io.karatelabs.ext.agent.runtime.AgentRunState.Status.RUNNING;
   private final List<io.karatelabs.ext.agent.llm.LlmMessage> messages = new ArrayList<>();
   private String systemPrompt;
   private int iterations;
   private int inputTokens;
   private int outputTokens;
   private int cacheReadTokens;
   private int cacheCreationTokens;
   private String lastError;
   private String j;
   private String k;
   private String l;
   private Object finalResult;
   private String statement;
   private String task;
   private int historyWindow = 1;
   private Boolean success;

   public io.karatelabs.ext.agent.runtime.AgentRunState.Status status() {
      return this.status;
   }

   public void setStatus(io.karatelabs.ext.agent.runtime.AgentRunState.Status var1) {
      this.status = var1;
   }

   public List<io.karatelabs.ext.agent.llm.LlmMessage> messages() {
      return this.messages;
   }

   public void addMessage(io.karatelabs.ext.agent.llm.LlmMessage var1) {
      this.messages.add(var1);
   }

   public String systemPrompt() {
      return this.systemPrompt;
   }

   public void setSystemPrompt(String var1) {
      this.systemPrompt = var1;
   }

   public int iterations() {
      return this.iterations;
   }

   public void incrementIterations() {
      this.iterations++;
   }

   public int inputTokens() {
      return this.inputTokens;
   }

   public int outputTokens() {
      return this.outputTokens;
   }

   public int totalTokens() {
      return this.inputTokens + this.outputTokens + this.cacheReadTokens + this.cacheCreationTokens;
   }

   public void addUsage(io.karatelabs.ext.agent.llm.LlmResponse var1) {
      this.inputTokens = this.inputTokens + var1.inputTokens();
      this.outputTokens = this.outputTokens + var1.outputTokens();
      this.cacheReadTokens = this.cacheReadTokens + var1.cacheReadTokens();
      this.cacheCreationTokens = this.cacheCreationTokens + var1.cacheCreationTokens();
   }

   public int cacheReadTokens() {
      return this.cacheReadTokens;
   }

   public int cacheCreationTokens() {
      return this.cacheCreationTokens;
   }

   public String k() {
      return this.j;
   }

   public void b(String var1) {
      this.j = var1;
   }

   public String lastError() {
      return this.lastError;
   }

   public void setLastError(String var1) {
      this.lastError = var1;
   }

   public String m() {
      return this.k;
   }

   public String n() {
      return this.l;
   }

   public void a(String var1, String var2) {
      this.k = var1;
      this.l = var2;
   }

   public Object finalResult() {
      return this.finalResult;
   }

   public void setFinalResult(Object var1) {
      this.finalResult = var1;
   }

   public String statement() {
      return this.statement;
   }

   public void setStatement(String var1) {
      this.statement = var1;
   }

   public Boolean success() {
      return this.success;
   }

   public void setSuccess(Boolean var1) {
      this.success = var1;
   }

   public String task() {
      return this.task;
   }

   public void setTask(String var1) {
      this.task = var1;
   }

   public int historyWindow() {
      return this.historyWindow;
   }

   public void setHistoryWindow(int var1) {
      this.historyWindow = var1;
   }

   public String toJson() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("status", this.status.name());
      var1.put("iterations", this.iterations);
      var1.put("totalInputTokens", this.inputTokens);
      var1.put("totalOutputTokens", this.outputTokens);
      if (this.cacheReadTokens > 0) {
         var1.put("totalCacheReadTokens", this.cacheReadTokens);
      }

      if (this.cacheCreationTokens > 0) {
         var1.put("totalCacheCreationTokens", this.cacheCreationTokens);
      }

      var1.put("systemPrompt", this.systemPrompt);
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();

      for (io.karatelabs.ext.agent.llm.LlmMessage var4 : this.messages) {
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
         var5.put("role", var4.role().name());
         var5.put("text", var4.text());
         var2.add(var5);
      }

      var1.put("messages", var2);
      if (this.lastError != null) {
         var1.put("lastError", this.lastError);
      }

      if (this.finalResult != null) {
         var1.put("finalResult", this.finalResult);
      }

      if (this.statement != null) {
         var1.put("statement", this.statement);
      }

      return Json.stringifyStrict(var1);
   }

   public String summary() {
      StringBuilder var1 = new StringBuilder();
      var1.append("Run Summary:\n");
      var1.append("- Iterations: ").append(this.iterations).append("\n");
      var1.append("- Total tokens: ").append(this.totalTokens()).append("\n");
      var1.append("- Status: ").append(this.status).append("\n");
      if (!this.messages.isEmpty()) {
         var1.append("- Messages: ").append(this.messages.size()).append("\n");
      }

      return var1.toString();
   }

   public static enum Status {
      RUNNING,
      COMPLETED,
      FAILED,
      STUCK,
      TERMINATED,
      BLOCKED,
      WAITING;
   }
}
