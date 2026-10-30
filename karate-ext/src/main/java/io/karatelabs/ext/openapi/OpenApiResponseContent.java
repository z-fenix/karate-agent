package io.karatelabs.ext.openapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiResponseContent {
   static final Logger a = LoggerFactory.getLogger(OpenApiResponseContent.class);
   public final OpenApiResponse b;
   public final String c;
   public final SchemaDefinition d;

   public OpenApiResponseContent(OpenApiResponse var1, String var2, Map<String, Object> var3) {
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

      Map var5 = (Map)var3.get("example");
      this.a(null, var5);
      Map var6 = (Map)var3.get("examples");
      if (var6 != null) {
         for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
            if (var8.getValue() instanceof Map) {
               this.a((String)var8.getKey(), var8.getValue());
            } else if ("$ref".equals(var8.getKey())) {
               HashMap<java.lang.String, java.lang.Object> var9 = new HashMap();
               var9.put("$ref", var8.getValue());
               this.a(null, var9);
            }
         }
      }
   }

   private void a(String var1, Object var2) {
      if (var2 != null) {
         Map var3 = var2 instanceof Map ? (Map)var2 : Collections.singletonMap("value", var2);

         Map var4 = io.karatelabs.ext.openapi.SchemaDefinition.normalizeSchemaDescriptor(this.b.a.a.b, var3);
         this.b.f.add(new ResponseExample(this.b, this.c, var1, new SchemaDefinition(this.b.a.a.b, var4)));
      }
   }
}
