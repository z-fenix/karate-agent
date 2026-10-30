package io.karatelabs.ext.agent.events;

import io.karatelabs.common.Json;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface AgentEvent
   permits io.karatelabs.ext.agent.events.AgentEvent.IterationStartEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ContextTrimEvent,
   io.karatelabs.ext.agent.events.AgentEvent.LlmRequestEvent,
   io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent,
   io.karatelabs.ext.agent.events.AgentEvent.LlmResponseStartEvent,
   io.karatelabs.ext.agent.events.AgentEvent.TextChunkEvent,
   io.karatelabs.ext.agent.events.AgentEvent.LlmResponseEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ToolRequestEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ToolResultEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ArtifactEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ConsoleEvent,
   io.karatelabs.ext.agent.events.AgentEvent.HintEvent,
   io.karatelabs.ext.agent.events.AgentEvent.StepEvent,
   io.karatelabs.ext.agent.events.AgentEvent.AssertionEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ScreenshotEvent,
   io.karatelabs.ext.agent.events.AgentEvent.HealedEvent,
   io.karatelabs.ext.agent.events.AgentEvent.PausedEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ResumedEvent,
   io.karatelabs.ext.agent.events.AgentEvent.CompleteEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent,
   io.karatelabs.ext.agent.events.AgentEvent.HumanActionEvent,
   io.karatelabs.ext.agent.events.AgentEvent.FocusEvent,
   io.karatelabs.ext.agent.events.AgentEvent.AnnotationEvent,
   io.karatelabs.ext.agent.events.AgentEvent.BrowserResetEvent,
   io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent {
   String a();

   long b();

   Map<String, Object> c();

   default Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("type", this.a());
      var1.put("ts", this.b());
      var1.putAll(this.c());
      return var1;
   }

   default String e() {
      return Json.stringifyStrict(this.toMap());
   }

   static Map<String, Object> a(Object... var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (byte var2 = 0; var2 + 1 < var0.length; var2 += 2) {
         Object var3 = var0[var2 + 1];
         if (var3 != null) {
            var1.put((String)var0[var2], var3);
         }
      }

      return var1;
   }

   static long f() {
      return System.currentTimeMillis();
   }

   public record AnnotationEvent(long b, String g, Long h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "annotation";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("text", this.g, "attachedTo", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.AnnotationEvent a(String var0, Long var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.AnnotationEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public String g() {
         return this.g;
      }

      public Long h() {
         return this.h;
      }
   }

   public record ArtifactEvent(long b, int g, String h, String i, String j, String k) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "artifact";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "kind", this.h, "contentType", this.i, "title", this.j, "reference", this.k);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ArtifactEvent a(int var0, String var1, String var2, String var3, String var4) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ArtifactEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2, var3, var4);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }
   }

   public record AssertionEvent(long ts, int iteration, Map<String, Object> data) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "assertion";
      }

      @Override
      public Map<String, Object> c() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("iteration", this.iteration);
         if (this.data != null) {
            var1.putAll(this.data);
         }

         return var1;
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.AssertionEvent a(int var0, Map<String, Object> var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.AssertionEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.ts;
      }

      public int g() {
         return this.iteration;
      }

      public Map<String, Object> h() {
         return this.data;
      }
   }

   public record BrowserResetEvent(long b) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "browser_reset";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a(new Object[0]);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.BrowserResetEvent g() {
         return new io.karatelabs.ext.agent.events.AgentEvent.BrowserResetEvent(io.karatelabs.ext.agent.events.AgentEvent.f());
      }

      @Override
      public long b() {
         return this.b;
      }
   }

   public record CompleteEvent(long b, String g, Object h, int i, int j, int k, int l, int m, String n) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "complete";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a(
            "status",
            this.g,
            "result",
            this.h,
            "iterations",
            this.i,
            "inputTokens",
            this.j,
            "outputTokens",
            this.k,
            "cacheReadTokens",
            this.l > 0 ? this.l : null,
            "cacheCreationTokens",
            this.m > 0 ? this.m : null,
            "lastError",
            this.n
         );
      }

      @Override
      public long b() {
         return this.b;
      }

      public String g() {
         return this.g;
      }

      public Object h() {
         return this.h;
      }

      public int i() {
         return this.i;
      }

      public int j() {
         return this.j;
      }

      public int k() {
         return this.k;
      }

      public int l() {
         return this.l;
      }

      public int m() {
         return this.m;
      }

      public String n() {
         return this.n;
      }
   }

   public record ConsoleEvent(long b, int g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "console";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "text", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ConsoleEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ConsoleEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ContextTrimEvent(long b, int g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "context_trim";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "summary", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ContextTrimEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ContextTrimEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ControlHandoffEvent(long b, String g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "control_handoff";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("to", this.g, "note", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent a(String var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ErrorEvent(long b, int g, String h, boolean i, String j) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "error";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "message", this.h, "retryable", this.i, "hint", this.j);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent a(int var0, String var1, boolean var2) {
         return a(var0, var1, var2, null);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent a(int var0, String var1, boolean var2, String var3) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2, var3);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public boolean i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }
   }

   public record FocusEvent(long b, String g, String h, String i) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "focus";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("phase", this.g, "locator", this.h, "frame", this.i);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.FocusEvent a(String var0, String var1, String var2) {
         return new io.karatelabs.ext.agent.events.AgentEvent.FocusEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2);
      }

      @Override
      public long b() {
         return this.b;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }
   }

   public record HealedEvent(long b, int g, String h, String i, String j, Double k) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "healed";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "from", this.h, "to", this.i, "action", this.j, "score", this.k);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.HealedEvent a(int var0, String var1, String var2, String var3, Double var4) {
         return new io.karatelabs.ext.agent.events.AgentEvent.HealedEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2, var3, var4);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public Double k() {
         return this.k;
      }
   }

   public record HintEvent(long b, int g, String h, String i) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "hint";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "source", this.h, "message", this.i);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.HintEvent a(int var0, String var1, String var2) {
         return new io.karatelabs.ext.agent.events.AgentEvent.HintEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }
   }

   public record HumanActionEvent(long b, Map<String, Object> g) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "human_action";
      }

      @Override
      public Map<String, Object> c() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         if (this.g != null) {
            var1.putAll(this.g);
         }

         var1.remove("type");
         var1.remove("ts");
         return var1;
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.HumanActionEvent a(Map<String, Object> var0) {
         return new io.karatelabs.ext.agent.events.AgentEvent.HumanActionEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0);
      }

      @Override
      public long b() {
         return this.b;
      }

      public Map<String, Object> g() {
         return this.g;
      }
   }

   public record IterationStartEvent(long b, int g) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "iteration_start";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.IterationStartEvent a(int var0) {
         return new io.karatelabs.ext.agent.events.AgentEvent.IterationStartEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }
   }

   public record LlmRequestEvent(long b, int g) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "llm_request";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.LlmRequestEvent a(int var0) {
         return new io.karatelabs.ext.agent.events.AgentEvent.LlmRequestEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }
   }

   public record LlmResponseEvent(long b, int g, String h, String i, int j, int k, int l, int m, long n, String o, String p, String q) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "llm_response";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a(
            "iteration",
            this.g,
            "text",
            this.h,
            "model",
            this.i,
            "finishReason",
            this.o,
            "refusal",
            this.p,
            "reasoning",
            this.q != null && !this.q.isEmpty() ? this.q : null,
            "inputTokens",
            this.j,
            "outputTokens",
            this.k,
            "cacheReadTokens",
            this.l > 0 ? this.l : null,
            "cacheCreationTokens",
            this.m > 0 ? this.m : null,
            "runningTimeMs",
            this.n
         );
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public int j() {
         return this.j;
      }

      public int k() {
         return this.k;
      }

      public int l() {
         return this.l;
      }

      public int m() {
         return this.m;
      }

      public long n() {
         return this.n;
      }

      public String o() {
         return this.o;
      }

      public String p() {
         return this.p;
      }

      public String q() {
         return this.q;
      }
   }

   public record LlmResponseStartEvent(long b, int g) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "llm_response_start";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.LlmResponseStartEvent a(int var0) {
         return new io.karatelabs.ext.agent.events.AgentEvent.LlmResponseStartEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }
   }

   public record PausedEvent(long b, int g) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "paused";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.PausedEvent a(int var0) {
         return new io.karatelabs.ext.agent.events.AgentEvent.PausedEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }
   }

   public record RequestFrameEvent(long b, String g, String h, List<Map<String, Object>> i) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "request_frame";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("kind", this.g, "system", this.h, "messages", this.i);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent a(String var0, String var1, List<Map<String, Object>> var2) {
         return new io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2);
      }

      @Override
      public long b() {
         return this.b;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public List<Map<String, Object>> i() {
         return this.i;
      }
   }

   public record ResumedEvent(long b, int g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "resumed";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "message", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ResumedEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ResumedEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ScreenshotEvent(long b, int g, String h, String i) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "screenshot";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "filename", this.h, "title", this.i);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ScreenshotEvent a(int var0, String var1, String var2) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ScreenshotEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }
   }

   public record StepEvent(long b, int g, String h, String i, String j) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "step";
      }

      @Override
      public Map<String, Object> c() {
         var var1 = io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "name", this.h);
         if (this.i != null) {
            var1.put("status", this.i);
         }

         if (this.j != null) {
            var1.put("note", this.j);
         }

         return var1;
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.StepEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.StepEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, null, null);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.StepEvent a(int var0, String var1, String var2, String var3) {
         return new io.karatelabs.ext.agent.events.AgentEvent.StepEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2, var3);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }
   }

   public record TextChunkEvent(long b, int g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "text_chunk";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "chunk", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.TextChunkEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.TextChunkEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ToolRequestEvent(long b, int g, String h) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "tool_request";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "js", this.h);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ToolRequestEvent a(int var0, String var1) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ToolRequestEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record ToolResultEvent(long b, int g, String h, String i) implements io.karatelabs.ext.agent.events.AgentEvent {

      @Override
      public String a() {
         return "tool_result";
      }

      @Override
      public Map<String, Object> c() {
         return io.karatelabs.ext.agent.events.AgentEvent.a("iteration", this.g, "js", this.h, "result", this.i);
      }

      public static io.karatelabs.ext.agent.events.AgentEvent.ToolResultEvent a(int var0, String var1, String var2) {
         return new io.karatelabs.ext.agent.events.AgentEvent.ToolResultEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var0, var1, var2);
      }

      @Override
      public long b() {
         return this.b;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }
   }
}
