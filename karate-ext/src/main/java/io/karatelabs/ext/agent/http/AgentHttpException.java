package io.karatelabs.ext.agent.http;

public class AgentHttpException extends RuntimeException {
   private final int a;
   private final String b;

   public AgentHttpException(int var1, String var2) {
      super("HTTP " + var1 + (var2 != null && !var2.isBlank() ? ": " + var2 : ""));
      this.a = var1;
      this.b = var2;
   }

   public int statusCode() {
      return this.a;
   }

   public String responseBody() {
      return this.b;
   }
}
