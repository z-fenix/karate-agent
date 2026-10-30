package io.karatelabs.ext.rules.twin;

import java.util.LinkedHashMap;
import java.util.Map;

public class TwinException extends RuntimeException implements io.karatelabs.ext.common.jsapi.JsWireException {
   public static final String a = "TWIN_INVALID";
   public static final String b = "PARSE_FAILED";
   public static final String c = "SEQUENCE_INVALID";
   public static final String d = "PLAN_INVALID";
   public static final String e = "REQUIRED_INVALID";
   public static final String f = "CALC_OUTSIDE_APPLY";
   public static final String g = "CALC_USAGE";
   public static final String h = "LIVE_USAGE";
   public static final String i = "LIVE_UNDECLARED";
   public static final String j = "RESET_REQUIRED";
   public static final String k = "ADJUDICATION_UNKNOWN";
   public static final String l = "ADJUDICATION_USAGE";
   public static final String m = "ROUTE_UNRESOLVED";
   public static final String n = "ROUTE_DUPLICATE";
   public static final String o = "OPERATION_DUPLICATE";
   public static final String p = "WIRE_AMBIGUOUS";
   public static final String q = "OBSERVE_DISAGREES";
   public static final String r = "SCHEMA_UNSUPPORTED";
   public static final String s = "BINDING_MISSING";
   public static final String t = "IDENTITY_REASSIGNED";
   private final int u;
   private final String v;
   private Map<String, Object> w;

   public TwinException(int var1, String var2, String var3) {
      super(var3);
      this.u = var1;
      this.v = var2;
   }

   static TwinException a(String var0, String var1, String var2) {
      TwinException var3 = new TwinException(400, "TWIN_INVALID", (var0 == null ? "twin.js " : "command '" + var0 + "' ") + var1 + ": " + var2);
      LinkedHashMap var4 = new LinkedHashMap();
      if (var0 != null) {
         var4.put("command", var0);
      }

      var4.put("field", var1);
      var4.put("reason", var2);
      var3.w = var4;
      return var3;
   }

   static TwinException a(String var0, String var1, String var2, String var3) {
      TwinException var4 = new TwinException(400, var0, (var1 == null ? "twin.js " : "command '" + var1 + "' ") + var2 + ": " + var3);
      LinkedHashMap var5 = new LinkedHashMap();
      if (var1 != null) {
         var5.put("command", var1);
      }

      var5.put("field", var2);
      var5.put("reason", var3);
      var4.w = var5;
      return var4;
   }

   static TwinException a(String var0, String var1) {
      TwinException var2 = new TwinException(400, "PLAN_INVALID", "plan " + var0 + ": " + var1);
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("field", var0);
      var2.w = var3;
      return var2;
   }

   static TwinException b(String var0, String var1, String var2) {
      TwinException var3 = new TwinException(400, "REQUIRED_INVALID", (var0 == null ? "required " : "required row '" + var0 + "' ") + var1 + ": " + var2);
      LinkedHashMap var4 = new LinkedHashMap();
      if (var0 != null) {
         var4.put("row", var0);
      }

      var4.put("field", var1);
      var3.w = var4;
      return var3;
   }

   static TwinException a(int var0, String var1, String var2, Map<String, Object> var3) {
      TwinException var4 = new TwinException(var0, var1, var2);
      var4.w = var3;
      return var4;
   }

   static TwinException a(String var0, Map<String, Object> var1) {
      TwinException var2 = new TwinException(400, "SEQUENCE_INVALID", var0);
      var2.w = var1;
      return var2;
   }

   @Override
   public String a() {
      return this.getMessage();
   }

   @Override
   public String b() {
      return this.v;
   }

   @Override
   public int c() {
      return this.u;
   }

   @Override
   public Map<String, Object> d() {
      return this.w;
   }
}
