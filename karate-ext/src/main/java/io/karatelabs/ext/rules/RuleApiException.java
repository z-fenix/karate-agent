package io.karatelabs.ext.rules;

import java.util.Map;

public class RuleApiException extends RuntimeException implements io.karatelabs.ext.common.jsapi.JsWireException {
   public final int a;
   public final String b;
   private Map<String, Object> c;

   public RuleApiException(int var1, String var2, String var3) {
      super(var3);
      this.a = var1;
      this.b = var2;
   }

   public RuleApiException(int var1, String var2, String var3, Throwable var4) {
      super(var3, var4);
      this.a = var1;
      this.b = var2;
   }

   public RuleApiException a(Map<String, Object> var1) {
      this.c = var1;
      return this;
   }

   public Map<String, Object> e() {
      return this.c;
   }

   @Override
   public String a() {
      return this.getMessage();
   }

   @Override
   public String b() {
      return this.b;
   }

   @Override
   public int c() {
      return this.a;
   }

   @Override
   public Map<String, Object> d() {
      return this.c;
   }
}
