package io.karatelabs.ext.diff;

import java.util.LinkedHashMap;
import java.util.Map;

public record UnevaluatedDiff(String b, String c, String d, String e, Object f) {

   public UnevaluatedDiff(String b, String c, String d, String e, Object f) {
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("construct", this.c);
      var1.put("pointer", this.e);
      var1.put("side", this.d);
      var1.put("reason", this.b);
      if (this.f != null) {
         var1.put("detail", this.f);
      }

      return var1;
   }

   public String b() {
      return this.b;
   }

   public String c() {
      return this.c;
   }

   public String d() {
      return this.d;
   }

   public String e() {
      return this.e;
   }

   public Object f() {
      return this.f;
   }
}
