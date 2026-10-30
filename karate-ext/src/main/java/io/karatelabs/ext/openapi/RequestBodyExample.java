package io.karatelabs.ext.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestBodyExample {
   private static final Logger e = LoggerFactory.getLogger(RequestBodyExample.class);
   public OpenApiRequestBody a;
   public final String b;
   public final String c;
   public final SchemaDefinition d;

   public RequestBodyExample(OpenApiRequestBody var1, String var2, String var3, SchemaDefinition var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }
}
