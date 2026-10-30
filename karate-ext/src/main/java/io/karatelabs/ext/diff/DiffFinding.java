package io.karatelabs.ext.diff;

import io.karatelabs.ext.common.Sha256;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DiffFinding {
   private final String a;
   private final String b;
   private DiffSeverity c;
   private String d;
   private String e;
   private String f;
   private String g;
   private String h;
   private String i;
   private String j;
   private String k = "none";
   private String l;
   private String m;
   private Map<String, Object> n;
   private Map<String, Object> o;
   private Map<String, Object> p;
   private boolean q;
   private Map<String, Object> r;
   private final Map<String, Object> s = new LinkedHashMap<>();

   public DiffFinding(String var1, String var2, DiffSeverity var3) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }

   public DiffSeverity severity() {
      return this.c;
   }

   public DiffFinding a(String var1) {
      this.d = var1;
      return this;
   }

   public DiffFinding b(String var1) {
      this.e = var1;
      return this;
   }

   public DiffFinding a(String var1, String var2) {
      this.f = var1;
      this.g = var2;
      return this;
   }

   public DiffFinding a(String var1, String var2, String var3) {
      this.h = var1;
      this.i = var2;
      this.j = var3;
      return this;
   }

   public DiffFinding c(String var1) {
      this.k = var1;
      return this;
   }

   public DiffFinding b(String var1, String var2) {
      this.l = var1;
      this.m = var2;
      return this;
   }

   public DiffFinding a(Map<String, Object> var1, Map<String, Object> var2) {
      this.n = var1;
      this.o = var2;
      return this;
   }

   public DiffFinding a(Map<String, Object> var1) {
      this.p = var1;
      return this;
   }

   public Map<String, Object> b() {
      return this.p;
   }

   public DiffFinding a(boolean var1) {
      this.q = var1;
      return this;
   }

   public DiffFinding b(Map<String, Object> var1) {
      this.r = var1;
      this.q = var1 != null;
      return this;
   }

   public DiffFinding a(String var1, Object var2) {
      if (var2 != null) {
         this.s.put(var1, var2);
      }

      return this;
   }

   public String identifier() {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();
      var1.put("taxonomy", this.a);
      var1.put("kind", this.b);
      var1.put("subject", this.g);
      var1.put("direction", this.k);
      var1.put("construct", this.i);
      var1.put("facet", this.j);
      return "sha256:" + io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.common.CanonicalJson.a(var1)).substring(0, 16);
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("kind", this.b);
      var1.put("level", this.c.name());
      var1.put("path", this.d);
      var1.put("detail", this.e);
      var1.put("id", this.identifier());
      if (this.f != null) {
         LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
         var2.put("kind", this.f);
         var2.put("key", this.g);
         var1.put("subject", var2);
      }

      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
      var3.put("kind", this.h);
      var3.put("key", this.i);
      if (this.j != null) {
         var3.put("facet", this.j);
      }

      var1.put("construct", var3);
      var1.put("direction", this.k);
      var1.put("pointer", this.l);
      var1.put("authored", this.m);
      if (this.n != null) {
         var1.put("baselineLocation", this.n);
      }

      if (this.o != null) {
         var1.put("currentLocation", this.o);
      }

      if (this.p != null) {
         var1.put("join", this.p);
      }

      var1.put("waived", this.q);
      if (this.r != null) {
         var1.put("waiver", this.r);
      }

      var1.putAll(this.s);
      return var1;
   }
}
