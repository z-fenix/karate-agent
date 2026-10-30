package io.karatelabs.ext.openapi;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiParameter implements io.karatelabs.ext.common.MapConvertible {
   static final Logger a = LoggerFactory.getLogger(OpenApiParameter.class);
   public final OpenApiPathItem b;
   public final OpenApiRequestBody c;
   public final String d;
   public final String e;
   public final boolean f;
   public final boolean g;
   public final SchemaDefinition h;
   public final String i;
   public final Boolean j;

   public OpenApiParameter(OpenApiPathItem var1, OpenApiRequestBody var2, Map<String, Object> var3) {
      this.b = var1;
      this.c = var2;
      this.d = (String)var3.get("name");
      this.e = (String)var3.get("in");
      this.i = (String)var3.get("style");
      this.j = (Boolean)var3.get("explode");
      Boolean var4 = (Boolean)var3.get("required");
      this.f = var4 != null && var4;
      Boolean var5 = (Boolean)var3.get("deprecated");
      this.g = var5 != null && var5;
      OpenApiDocument var6 = var1 == null ? var2.a.a.b : var1.b;
      var var7 = io.karatelabs.ext.openapi.SchemaDefinition.normalizeSchemaDescriptor(var6, var3);
      this.h = new SchemaDefinition(var6, var7);
   }

   @Override
   public Map<String, Object> toMap() {
      HashMap<java.lang.String, java.lang.Object> var1 = new HashMap();
      var1.put("name", this.d);
      var1.put("in", this.e);
      if (this.i != null) {
         var1.put("style", this.i);
      }

      if (this.j != null) {
         var1.put("explode", this.j);
      }

      var1.put("required", this.f);
      if (this.g) {
         var1.put("deprecated", this.g);
      }

      var1.put("schema", this.h.getRootNode().toMap());
      return var1;
   }
}
