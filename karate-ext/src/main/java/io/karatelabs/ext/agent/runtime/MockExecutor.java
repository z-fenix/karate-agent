package io.karatelabs.ext.agent.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class MockExecutor implements ToolExecutor {
   private final Map<String, Object> a = new LinkedHashMap<>();
   private Function<String, Object> b;
   private Object c = Map.of("status", "mock", "message", "Mock execution");

   public MockExecutor a(Function<String, Object> var1) {
      this.b = var1;
      return this;
   }

   public MockExecutor a(Object var1) {
      this.c = var1;
      return this;
   }

   @Override
   public ToolResult a(String var1) throws io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException {
      return this.b != null ? new ToolResult(this.b.apply(var1)) : new ToolResult(this.c);
   }

   @Override
   public void a(String var1, Object var2) {
      this.a.put(var1, var2);
   }

   @Override
   public Map<String, Object> a() {
      return this.a;
   }
}
