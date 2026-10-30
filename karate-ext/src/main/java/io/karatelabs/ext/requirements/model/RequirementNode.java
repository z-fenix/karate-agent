package io.karatelabs.ext.requirements.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RequirementNode(String b, String c, String d, String e, String f, String g, String h, Map<String, String> i, List<String> j, List<String> k, String l, List<String> m, List<io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec> n, io.karatelabs.ext.requirements.model.RequirementNode.SourceReference o, String p, String q, int r) {


   public Map<String, Object> a() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", this.b);
      if (a(this.c)) {
         var1.put("type", this.c);
      }

      if (a(this.d)) {
         var1.put("title", this.d);
      }

      if (a(this.e)) {
         var1.put("parent", this.e);
      }

      if (a(this.f)) {
         var1.put("status", this.f);
      }

      if (a(this.g)) {
         var1.put("priority", this.g);
      }

      if (a(this.h)) {
         var1.put("criticality", this.h);
      }

      if (this.i != null && !this.i.isEmpty()) {
         var1.put("tags", new LinkedHashMap<>(this.i));
      }

      if (this.j != null && !this.j.isEmpty()) {
         var1.put("ref", List.copyOf(this.j));
      }

      if (this.k != null && !this.k.isEmpty()) {
         var1.put("real", List.copyOf(this.k));
      }

      if (a(this.l)) {
         var1.put("body", this.l);
      }

      if (this.m != null && !this.m.isEmpty()) {
         var1.put("acceptance", List.copyOf(this.m));
      }

      if (this.n != null && !this.n.isEmpty()) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

         for (io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec var4 : this.n) {
            var2.add(var4.a());
         }

         var1.put("constraints", var2);
      }

      if (this.o != null) {
         var var5 = this.o.a();
         if (!var5.isEmpty()) {
            var1.put("source", var5);
         }
      }

      return var1;
   }

   private static boolean a(String var0) {
      return var0 != null && !var0.isBlank();
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

   public String f() {
      return this.f;
   }

   public String g() {
      return this.g;
   }

   public String h() {
      return this.h;
   }

   public Map<String, String> i() {
      return this.i;
   }

   public List<String> j() {
      return this.j;
   }

   public List<String> k() {
      return this.k;
   }

   public String l() {
      return this.l;
   }

   public List<String> m() {
      return this.m;
   }

   public List<io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec> n() {
      return this.n;
   }

   public io.karatelabs.ext.requirements.model.RequirementNode.SourceReference o() {
      return this.o;
   }

   public String p() {
      return this.p;
   }

   public String q() {
      return this.q;
   }

   public int r() {
      return this.r;
   }

   public record ConstraintSpec(String b, String c, String d) {


      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("text", this.b);
         if (io.karatelabs.ext.requirements.model.RequirementNode.a(this.c)) {
            var1.put("kind", this.c);
         }

         if (io.karatelabs.ext.requirements.model.RequirementNode.a(this.d)) {
            var1.put("check", this.d);
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
   }

   public record SourceReference(String b, String c, Integer d) {


      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         if (io.karatelabs.ext.requirements.model.RequirementNode.a(this.b)) {
            var1.put("system", this.b);
         }

         if (io.karatelabs.ext.requirements.model.RequirementNode.a(this.c)) {
            var1.put("key", this.c);
         }

         if (this.d != null) {
            var1.put("rev", this.d);
         }

         return var1;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public Integer d() {
         return this.d;
      }
   }
}
