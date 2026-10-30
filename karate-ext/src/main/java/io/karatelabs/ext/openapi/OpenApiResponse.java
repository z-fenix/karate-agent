package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiResponse {
   private static final Logger h = LoggerFactory.getLogger(OpenApiResponse.class);
   public final OpenApiOperation a;
   public final String b;
   public final String c;
   public final String d;
   public final List<OpenApiResponseContent> e = new ArrayList<>();
   public final List<ResponseExample> f = new ArrayList<>();
   public final List<OpenApiResponseHeader> g = new ArrayList<>();

   public OpenApiResponse(OpenApiOperation var1, String var2, Map<String, Object> var3) {
      this.a = var1;
      this.b = var2;
      SchemaDefinition var4 = var1.a.b.resolveSchemaReference(null, var3);
      if (var4 == null) {
         var3 = Collections.emptyMap();
      } else {
         var3 = var4.rawSchema;
      }

      this.c = (String)var3.get("summary");
      this.d = (String)var3.get("description");
      Object var5 = (Map)var3.get("content");
      if (var5 == null) {
         Map var6 = (Map)var3.get("schema");
         if (var6 != null) {
            var5 = new HashMap();
            HashMap<java.lang.String, java.util.Map> var7 = new HashMap();
            ((java.util.Map)var5).put("*", var7);
            var7.put("schema", var6);
         }
      }

      if (var5 != null) {
         for (Entry var19 : (Iterable<Entry>) (Iterable<?>) (((java.util.Map)var5).entrySet())) {
            String var8 = (String)var19.getKey();
            Map var9 = (Map)var19.getValue();
            this.e.add(new OpenApiResponseContent(this, var8, var9));
         }
      }

      if (var3.get("examples") instanceof Map var20) {
         for (Entry var24 : (Iterable<Entry>) (Iterable<?>) (var20.entrySet())) {
            String var10 = (String)var24.getKey();
            Object var11 = var24.getValue();
            if (var11 instanceof Map var28) {
               for (Entry var31 : (Iterable<Entry>) (Iterable<?>) (var28.entrySet())) {
                  SchemaDefinition var32 = new SchemaDefinition(var1.a.b, Collections.singletonMap("value", var31.getValue()));
                  this.f.add(new ResponseExample(this, var10, (String)var31.getKey(), var32));
               }
            } else if (var11 instanceof List) {
               for (Object var14 : (List)var11) {
                  SchemaDefinition var15 = new SchemaDefinition(var1.a.b, Collections.singletonMap("value", var14));
                  this.f.add(new ResponseExample(this, var10, null, var15));
               }
            }
         }
      }

      Map var21 = (Map)var3.get("headers");
      if (var21 != null) {
         for (Entry var25 : (Iterable<Entry>) (Iterable<?>) (var21.entrySet())) {
            String var26 = (String)var25.getKey();
            Map var27 = (Map)var25.getValue();
            Map var29 = io.karatelabs.ext.openapi.SchemaDefinition.normalizeSchemaDescriptor(var1.a.b, var27);
            this.g.add(new OpenApiResponseHeader(this, var26, new SchemaDefinition(var1.a.b, var29)));
         }
      }
   }
}
