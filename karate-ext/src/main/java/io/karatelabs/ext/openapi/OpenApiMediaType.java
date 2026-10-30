package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiMediaType implements io.karatelabs.ext.common.MapConvertible {
   static final Logger a = LoggerFactory.getLogger(OpenApiMediaType.class);
   public final OpenApiRequestBody b;
   public final String c;
   public final SchemaDefinition d;
   public final List<RequestBodyExample> e = new ArrayList<>();
   public final Map<String, Object> f;
   public final Object g;

   public OpenApiMediaType(OpenApiRequestBody var1, String var2, Map<String, Object> var3) {
      this.b = var1;
      this.c = var2;
      if (var3 == null) {
         var3 = Collections.emptyMap();
      }

      Map var4 = (Map)var3.get("schema");
      if (var4 == null) {
         this.d = null;
      } else {
         this.d = var1.a.a.b.resolveSchemaReference(null, var4);
      }

      Map var5 = (Map)var3.get("encoding");
      if (var5 != null && !var5.isEmpty()) {
         this.f = var5;
      } else {
         this.f = Collections.emptyMap();
      }

      Object var6 = var3.get("example");
      this.a(null, var6);
      Map var7 = (Map)var3.get("examples");
      this.g = var6 != null ? var6 : this.a(var7);
      if (var7 != null) {
         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            if (var9.getValue() instanceof Map) {
               this.a((String)var9.getKey(), var9.getValue());
            } else if ("$ref".equals(var9.getKey())) {
               HashMap<java.lang.String, java.lang.Object> var10 = new HashMap();
               var10.put("$ref", var9.getValue());
               this.a(null, var10);
            }
         }
      }
   }

   private Object a(Map<String, Object> var1) {
      if (var1 != null && !var1.isEmpty()) {
         Object var2 = var1.values().iterator().next();
         if (var2 instanceof Map var3 && var3.containsKey("$ref")) {
            SchemaDefinition var4 = this.b.a.a.b.resolveSchemaReference(null, var3);
            var2 = var4 == null ? null : var4.rawSchema;
         }

         return var2 instanceof Map var5 ? var5.get("value") : null;
      } else {
         return null;
      }
   }

   private void a(String var1, Object var2) {
      if (var2 != null) {
         Map var3 = var2 instanceof Map ? (Map)var2 : Collections.singletonMap("value", var2);

         Map var4 = io.karatelabs.ext.openapi.SchemaDefinition.normalizeSchemaDescriptor(this.b.a.a.b, var3);
         this.b.e.add(new RequestBodyExample(this.b, this.c, var1, new SchemaDefinition(this.b.a.a.b, var4)));
      }
   }

   @Override
   public Map<String, Object> toMap() {
      HashMap<java.lang.String, java.lang.Object> var1 = new HashMap();
      var1.put("contentType", this.c);
      var1.put("schema", this.d.getRootNode().toMap());
      return var1;
   }
}
