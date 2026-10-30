package io.karatelabs.ext.mock;

import io.karatelabs.ext.common.MockRuntime;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import io.karatelabs.js.JavaInvokable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class MockInstance extends JsDispatch implements MockRuntime {
   private final MockServer server;
   private final CallLog callLog;
   private final Runnable resetState;
   private final Function<String, Object> varReader;
   private final List<Map<String, Object>> warnings;
   private final List<String> clears = new ArrayList<>(List.of("calls"));
   private final List<String> survives = new ArrayList<>();
   private Runnable restartState;
   private Runnable stopState;
   private volatile boolean draft;
   private volatile boolean stopped;
   private Object brain;

   public MockInstance(MockServer var1, CallLog var2, Runnable var3, Function<String, Object> var4, List<Map<String, Object>> var5) {
      this.server = var1;
      this.callLog = var2;
      this.resetState = var3;
      this.varReader = var4;
      this.warnings = var5 == null ? List.of() : var5;
      this.val("url", var1.url());
      this.val("port", var1.port());
      this.val("ssl", var1.ssl());
      this.val("warnings", this.warnings);
      this.lazy("calls", this::getCalls);
      this.lazy("draft", () -> this.draft);
      this.fn("stop", var1x -> {
         this.stop();
         return null;
      });
      this.fn("reset", var1x -> this.reset());
      this.fn(
         "restart",
         var1x -> var1x.length > 0
            ? JsApi.missing(
               "mock.restart() takes NO arguments — it rebuilds from the ORIGINAL boot source. To change the program in place, use mock.reprogram({js|file, state:'keep'|'reset'}) (raw-JS flavor)."
            )
            : this.restart()
      );
      this.fn("var", var1x -> var1x.length > 0 ? this.getVariable(String.valueOf(var1x[0])) : null);
      this.fn("reprogram", var0 -> refusal("reprogram", "swap this mock's program in place"));
      this.fn("freeze", var0 -> refusal("freeze", "promote a session-authored program to a committed file"));
   }

   private static Map<String, Object> refusal(String var0, String var1) {
      return JsApi.missing(
         "this mock flavor cannot "
            + var0
            + " ("
            + var1
            + ") — only the raw-JS flavor (Http.mock({js|file})) supports it; for other flavors use mock.stop() + a fresh Http.mock(...)"
      );
   }

   public MockInstance withState(List<String> var1, List<String> var2, Runnable var3) {
      if (var1 != null) {
         this.clears.addAll(var1);
      }

      if (var2 != null) {
         this.survives.addAll(var2);
      }

      this.restartState = var3;
      return this;
   }

   public static MockInstance start(Function<HttpRequest, HttpResponse> var0, int var1, Runnable var2, Function<String, Object> var3) {
      return start(var0, var1, var2, var3, List.of());
   }

   public static MockInstance start(
      Function<HttpRequest, HttpResponse> var0, int var1, Runnable var2, Function<String, Object> var3, List<Map<String, Object>> var4
   ) {
      return start(var0, var1, var2, var3, var4, null);
   }

   public static MockInstance start(
      Function<HttpRequest, HttpResponse> var0,
      int var1,
      Runnable var2,
      Function<String, Object> var3,
      List<Map<String, Object>> var4,
      UnaryOperator<Function<HttpRequest, HttpResponse>> var5
   ) {
      return start(var0, var1, var2, var3, var4, var5, null);
   }

   public static MockInstance start(
      Function<HttpRequest, HttpResponse> var0,
      int var1,
      Runnable var2,
      Function<String, Object> var3,
      List<Map<String, Object>> var4,
      UnaryOperator<Function<HttpRequest, HttpResponse>> var5,
      String var6
   ) {
      RecordingHandler var7 = new RecordingHandler(var0);
      Object var8 = var5 == null ? var7 : var5.apply(var7);
      HttpServer var9 = HttpServer.start(var6, var1, (Function<HttpRequest, HttpResponse>)var8, null, null);
      return new MockInstance(new io.karatelabs.ext.mock.MockInstance.HttpMockServer(var9), var7, var2, var3, var4);
   }

   public MockInstance withVerb(String var1, JavaInvokable var2) {
      this.fn(var1, var2);
      return this;
   }

   @Override
   public String getUrl() {
      return this.server.url();
   }

   public int getPort() {
      return this.server.port();
   }

   public List<Map<String, Object>> getCalls() {
      return this.callLog == null ? List.of() : this.callLog.getCalls();
   }

   public List<Map<String, Object>> getWarnings() {
      return this.warnings;
   }

   public void swapHandler(Function<HttpRequest, HttpResponse> var1) {
      if (this.callLog instanceof RecordingHandler var2) {
         var2.swap(var1);
      }
   }

   @Override
   public Object brain() {
      return this.brain;
   }

   public MockInstance withBrain(Object var1) {
      this.brain = var1;
      return this;
   }

   @Override
   public Object getVariable(String var1) {
      return this.varReader == null ? null : this.varReader.apply(var1);
   }

   public MockInstance onStop(Runnable var1) {
      this.stopState = var1;
      return this;
   }

   @Override
   public void stop() {
      this.stopped = true;
      this.server.stop();
      if (this.stopState != null) {
         this.stopState.run();
      }
   }

   public boolean isDraft() {
      return this.draft;
   }

   public void setDraft(boolean var1) {
      this.draft = var1;
   }

   public boolean isDraftLive() {
      return this.draft && !this.stopped;
   }

   @Override
   public boolean isStopped() {
      return this.stopped;
   }

   public Map<String, Object> reset() {
      int var1 = this.callLog == null ? 0 : this.callLog.clearAndCount();
      if (this.resetState != null) {
         this.resetState.run();
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("calls", var1);
      var2.put("cleared", List.copyOf(this.clears));
      boolean var3 = this.survives.isEmpty();
      if (!var3) {
         var2.put("survives", List.copyOf(this.survives));
      }

      var2.put("clean", var3);
      if (!var3) {
         var2.put(
            "restart",
            this.restartState != null
               ? "mock.restart() rebuilds this mock in the state it booted in, on the same port"
               : "only mock.stop() + a fresh Http.mock(...) returns this mock to its booted state"
         );
      }

      return var2;
   }

   public Map<String, Object> restart() {
      if (this.restartState != null) {
         this.restartState.run();
      }

      int var1 = this.callLog == null ? 0 : this.callLog.clearAndCount();
      if (this.resetState != null) {
         this.resetState.run();
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("restarted", true);
      var2.put("calls", var1);
      var2.put("url", this.getUrl());
      var2.put("port", this.getPort());
      return var2;
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("url", "port", "ssl", "calls", "warnings", "draft");
   }

   @Override
   public String jsKind() {
      return "mock";
   }

   private static final class HttpMockServer implements MockServer {
      private final HttpServer a;

      HttpMockServer(HttpServer var1) {
         this.a = var1;
      }

      @Override
      public String url() {
         return "http://localhost:" + this.a.getPort();
      }

      @Override
      public int port() {
         return this.a.getPort();
      }

      @Override
      public boolean ssl() {
         return false;
      }

      @Override
      public void stop() {
         this.a.stopAndWait();
      }
   }
}
