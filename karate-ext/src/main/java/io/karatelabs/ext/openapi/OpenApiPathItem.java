package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiPathItem implements io.karatelabs.ext.common.MapConvertible {
   static final Logger a = LoggerFactory.getLogger(OpenApiPathItem.class);
   public final OpenApiDocument b;
   public final String c;
   public final List<OpenApiParameter> d = new ArrayList<>();
   public final List<OpenApiOperation> e = new ArrayList<>();
   public String f;
   public String g;

   public OpenApiOperation a(String var1) {
      for (OpenApiOperation var3 : this.e) {
         if (var3.b.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public OpenApiPathItem(OpenApiDocument var1, String var2, Map<String, Object> var3) {
      this.b = var1;
      this.c = var2;
      if (var3 != null) {
         for (Entry var5 : var3.entrySet()) {
            String var6 = (String)var5.getKey();
            switch (var6) {
               case "$ref":
                  a.warn("path level '$ref' not supported");
                  break;
               case "description":
                  this.f = (String)var5.getValue();
                  break;
               case "summary":
                  this.g = (String)var5.getValue();
                  break;
               case "servers":
                  a.warn("path level 'servers' not supported");
                  break;
               case "parameters":
                  List var9 = (List)var5.getValue();
                  if (var9 != null) {
                     for (Map var11 : (Iterable<Map>) (Iterable<?>) (var9)) {
                        SchemaDefinition var12 = var1.resolveSchemaReference(null, var11);
                        if (var12 != null) {
                           this.d.add(new OpenApiParameter(this, null, var12.rawSchema));
                        }
                     }
                  }
                  break;
               default:
                  Map var10 = (Map)var5.getValue();
                  this.e.add(new OpenApiOperation(this, var6, var10));
            }
         }
      }
   }

   @Override
   public Map<String, Object> toMap() {
      HashMap<java.lang.String, java.lang.Object> var1 = new HashMap();
      var1.put("path", this.c);
      if (this.g != null) {
         var1.put("summary", this.g);
      }

      if (this.f != null) {
         var1.put("description", this.f);
      }

      ArrayList<java.lang.String> var2 = new ArrayList(this.e.size());
      var1.put("methods", var2);

      for (OpenApiOperation var4 : this.e) {
         var2.add(var4.b);
      }

      return var1;
   }

   @Override
   public String toString() {
      return this.c;
   }
}
