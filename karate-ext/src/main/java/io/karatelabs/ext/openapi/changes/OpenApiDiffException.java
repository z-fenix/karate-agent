package io.karatelabs.ext.openapi.changes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenApiDiffException extends RuntimeException implements io.karatelabs.ext.common.jsapi.JsWireException {
   private final String a;
   private final int b;
   private final Map<String, Object> c;

   OpenApiDiffException(String var1, int var2, String var3, Map<String, Object> var4) {
      super(var3);
      this.a = var1;
      this.b = var2;
      this.c = var4;
   }

   static OpenApiDiffException a(String var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("side", var0);
      var3.put("source", var1);
      var3.put("version", var2 == null ? null : String.valueOf(var2));
      var3.put("remedy", "convert the document to OpenAPI 3.0 or 3.1 first");
      return new OpenApiDiffException(
         "UNSUPPORTED_VERSION",
         422,
         var0
            + " '"
            + var1
            + "' is "
            + (var2 == null ? "not an OpenAPI 3.x document" : "version " + var2)
            + "; Openapi.diff reads OpenAPI 3.0, 3.1 and 3.2 — convert it first",
         var3
      );
   }

   static OpenApiDiffException a(String var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("side", var0);
      var3.put("source", var1);
      return new OpenApiDiffException("UNREADABLE", 400, var0 + " '" + var1 + "' could not be read: " + var2, var3);
   }

   static OpenApiDiffException a(String var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("source", var0);
      var2.put("remedy", "each waiver is {rule, scope, reason, owner, expires?, successor?, sunset?}");
      return new OpenApiDiffException("WAIVERS_MALFORMED", 400, "waivers (" + var0 + ") could not be read: " + var1, var2);
   }

   @Override
   public String a() {
      return this.getMessage();
   }

   @Override
   public String b() {
      return this.a;
   }

   @Override
   public int c() {
      return this.b;
   }

   @Override
   public Map<String, Object> d() {
      return this.c;
   }
}
