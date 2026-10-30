package io.karatelabs.ext.mcp;

public class McpException extends RuntimeException {
   public final String a;

   public McpException(String var1, String var2, Throwable var3) {
      super("mcp " + var1 + " failure: " + var2, var3);
      this.a = var1;
   }
}
