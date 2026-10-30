package io.karatelabs.ext.requirements.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RequirementsBundle(String component1, Map<String, Object> c, List<io.karatelabs.ext.requirements.model.RequirementNode> d, List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> e) {
   public static final String a = "karate-requirements/v1";
   public static final List<String> b = List.of("req", "cov", "ref", "real");


   public static RequirementsBundle a(List<io.karatelabs.ext.requirements.model.RequirementNode> var0) {
      return a(var0, List.of());
   }

   public static RequirementsBundle a(List<io.karatelabs.ext.requirements.model.RequirementNode> var0, List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("link-tags", b);
      return new RequirementsBundle("karate-requirements/v1", var2, var0, var1 == null ? List.of() : var1);
   }

   public io.karatelabs.ext.requirements.model.RequirementNode a(String var1) {
      if (var1 == null) {
         return null;
      } else {
         for (io.karatelabs.ext.requirements.model.RequirementNode var3 : this.d) {
            if (var1.equals(var3.b())) {
               return var3;
            }
         }

         return null;
      }
   }

   public Map<String, Object> a() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("schema", this.component1);
      if (this.c != null && !this.c.isEmpty()) {
         var1.put("config", this.c);
      }

      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.requirements.model.RequirementNode var4 : this.d) {
         var2.add(var4.a());
      }

      var1.put("requirements", var2);
      return var1;
   }

   public String b() {
      return this.component1;
   }

   public Map<String, Object> c() {
      return this.c;
   }

   public List<io.karatelabs.ext.requirements.model.RequirementNode> d() {
      return this.d;
   }

   public List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> e() {
      return this.e;
   }

   public record RequirementsDiagnostic(String a, String b, String c) {

   }
}
