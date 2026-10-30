package io.karatelabs.ext.robot;

import java.util.Map;

final class RobotException extends RuntimeException implements io.karatelabs.ext.common.jsapi.JsWireException {
   private static final int a = 400;
   private final String b;
   private final String c;
   private final String d;

   RobotException(String var1, String var2, String var3) {
      super(var3 != null && !var3.isEmpty() ? var1 + " | hint: " + var3 : var1);
      this.b = var1;
      this.c = var2;
      this.d = var3;
   }

   @Override
   public String a() {
      return this.b;
   }

   @Override
   public String b() {
      return this.c;
   }

   @Override
   public int c() {
      return 400;
   }

   @Override
   public Map<String, Object> d() {
      return this.d != null && !this.d.isEmpty() ? Map.of("hint", this.d) : null;
   }
}
