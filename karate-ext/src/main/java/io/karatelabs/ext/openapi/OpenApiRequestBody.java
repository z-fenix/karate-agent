package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OpenApiRequestBody implements io.karatelabs.ext.common.MapConvertible {
   public final OpenApiOperation a;
   public final List<OpenApiParameter> b = new ArrayList<>();
   public final String c;
   public final List<OpenApiMediaType> d = new ArrayList<>();
   public final List<RequestBodyExample> e = new ArrayList<>();

   public OpenApiRequestBody(OpenApiOperation var1, List<Map<String, Object>> var2, Map<String, Object> var3) {
      this.a = var1;
      if (var2 == null) {
         var2 = Collections.emptyList();
      }

      Map var4 = null;

      for (Map var6 : var2) {
         if (var6.containsKey("$ref")) {
            SchemaDefinition var7 = var1.a.b.resolveSchemaReference(null, var6);
            if (var7 != null) {
               var6 = var7.rawSchema;
            }
         }

         String var12 = (String)var6.get("in");
         if (var12 != null) {
            byte var9 = -1;
            switch (var12.hashCode()) {
               case 3029410:
                  if (var12.equals("body")) {
                     var9 = 0;
                  }
               default:
                  switch (var9) {
                     case 0:
                        var4 = var6;
                        break;
                     default:
                        this.b.add(new OpenApiParameter(null, this, var6));
                  }
            }
         }
      }

      if (var3 != null) {
         this.c = (String)var3.get("description");
         Map var10 = (Map)var3.get("content");
         if (var10 != null) {
            for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
               String var8 = (String)var13.getKey();
               Map var14 = (Map)var13.getValue();
               this.d.add(new OpenApiMediaType(this, var8, var14));
            }
         }
      } else if (var4 != null) {
         this.c = (String)var4.get("description");
         this.d.add(new OpenApiMediaType(this, "*", var4));
      } else {
         this.c = null;
      }
   }

   @Override
   public Map<String, Object> toMap() {
      HashMap var1 = new HashMap();
      if (this.c != null) {
         var1.put("description", this.c);
      }

      ArrayList var2 = new ArrayList(this.b.size());
      var1.put("parameters", var2);

      for (OpenApiParameter var4 : this.b) {
         var2.add(var4.toMap());
      }

      if (!this.d.isEmpty()) {
         ArrayList var6 = new ArrayList(this.d.size());
         var1.put("contentTypes", var6);

         for (OpenApiMediaType var5 : this.d) {
            var6.add(var5.toMap());
         }
      }

      return var1;
   }
}
