package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OpenApiOperation implements io.karatelabs.ext.common.MapConvertible {
   public final OpenApiPathItem a;
   public final String b;
   public final OpenApiRequestBody c;
   public final List<OpenApiResponse> d = new ArrayList<>();
   public final String e;
   public final String f;
   public final String g;
   public final List<String> h;
   public final boolean i;

   public OpenApiOperation(OpenApiPathItem var1, String var2, Map<String, Object> var3) {
      this.a = var1;
      this.b = var2.toUpperCase();
      if (var3 == null) {
         var3 = Collections.emptyMap();
      }

      this.e = (String)var3.get("summary");
      this.f = (String)var3.get("description");
      this.g = (String)var3.get("operationId");
      List var4 = (List)var3.get("tags");
      this.h = var4 != null ? Collections.unmodifiableList(var4) : Collections.emptyList();
      this.i = Boolean.TRUE.equals(var3.get("deprecated"));
      List var5 = (List)var3.get("parameters");
      Map var6 = (Map)var3.get("requestBody");
      if (var5 == null && var6 == null) {
         this.c = null;
      } else {
         this.c = new OpenApiRequestBody(this, var5, var6);
      }

      Map var7 = (Map)var3.get("responses");
      if (var7 != null) {
         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            Object var10 = var9.getKey();
            Map var11 = (Map)var9.getValue();
            this.d.add(new OpenApiResponse(this, var10 + "", var11));
         }
      }
   }

   @Override
   public Map<String, Object> toMap() {
      HashMap<java.lang.String, java.lang.Object> var1 = new HashMap();
      var1.put("path", this.a.c);
      var1.put("method", this.b);
      if (this.g != null) {
         var1.put("operationId", this.g);
      }

      if (this.e != null) {
         var1.put("summary", this.e);
      }

      if (this.f != null) {
         var1.put("description", this.f);
      }

      if (this.c != null) {
         var1.put("request", this.c.toMap());
      }

      if (!this.h.isEmpty()) {
         var1.put("tags", this.h);
      }

      if (this.i) {
         var1.put("deprecated", true);
      }

      return var1;
   }

   @Override
   public String toString() {
      return this.b + " " + this.a;
   }
}
