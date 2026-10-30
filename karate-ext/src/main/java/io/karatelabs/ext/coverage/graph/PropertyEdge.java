package io.karatelabs.ext.coverage.graph;

import java.util.Map;

public record PropertyEdge(String component1, String component2, String component3, Map<String, Object> component4) {
   public static final String a = "hit";
   public static final String b = "parent";
   public static final String c = "ref";
   public static final String d = "real";
   public static final String e = "decomposes";

   public PropertyEdge(String component1, String component2, String component3, Map<String, Object> component4) {
      component4 = component4 == null ? Map.of() : component4;
      this.component1 = component1;
      this.component2 = component2;
      this.component3 = component3;
      this.component4 = component4;
   }

   public static PropertyEdge a(String var0, String var1, String var2) {
      return new PropertyEdge(var0, var1, var2, Map.of());
   }

   public String a() {
      return this.component1;
   }

   public String b() {
      return this.component2;
   }

   public String c() {
      return this.component3;
   }

   public Map<String, Object> d() {
      return this.component4;
   }
}
