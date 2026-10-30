package io.karatelabs.ext.postman.convert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PostmanConversionResult(String b, int c, int d, List<io.karatelabs.ext.postman.convert.PostmanConversionResult.NotConvertedRequest> e, List<String> f) {


   public List<Map<String, Object>> a() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      this.e.forEach(var1x -> var1.add(var1x.a()));
      return var1;
   }

   public String b() {
      return this.b;
   }

   public int c() {
      return this.c;
   }

   public int d() {
      return this.d;
   }

   public List<io.karatelabs.ext.postman.convert.PostmanConversionResult.NotConvertedRequest> e() {
      return this.e;
   }

   public List<String> f() {
      return this.f;
   }

   public record NotConvertedRequest(String b, String c) {


      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("request", this.b);
         var1.put("text", this.c);
         return var1;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }
   }
}
