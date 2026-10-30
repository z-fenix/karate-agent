package io.karatelabs.ext.agent.runtime;

import java.util.Collections;
import java.util.Map;

public interface ToolExecutor {
   ToolResult a(String var1) throws io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException;

   void a(String var1, Object var2);

   default Map<String, Object> a() {
      return Collections.emptyMap();
   }

   public static class ToolExecutionException extends Exception {
      private final String a;

      public ToolExecutionException(String var1, String var2) {
         super(var1);
         this.a = var2;
      }

      public ToolExecutionException(String var1, Throwable var2, String var3) {
         super(var1, var2);
         this.a = var3;
      }

      public String a() {
         return this.a;
      }
   }
}
