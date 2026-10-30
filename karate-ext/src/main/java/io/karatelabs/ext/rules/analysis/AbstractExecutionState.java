package io.karatelabs.ext.rules.analysis;

import java.util.List;

public record AbstractExecutionState(List<io.karatelabs.ext.rules.analysis.AbstractExecutionState.ExecutionDimension> b, int c, String d, io.karatelabs.ext.rules.analysis.AbstractValue e, io.karatelabs.ext.rules.analysis.AbstractValue f, long g, boolean h) {

   public AbstractExecutionState(List<io.karatelabs.ext.rules.analysis.AbstractExecutionState.ExecutionDimension> b, int c, String d, io.karatelabs.ext.rules.analysis.AbstractValue e, io.karatelabs.ext.rules.analysis.AbstractValue f, long g, boolean h) {
      b = List.copyOf(b);
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
   }

   public long a(int var1) {
      return this.b.get(var1).d();
   }

   public long b(int var1) {
      return this.b.get(var1).e();
   }

   public long c(int var1) {
      return this.b.get(var1).a();
   }

   public int a() {
      return this.b.size();
   }

   public List<io.karatelabs.ext.rules.analysis.AbstractExecutionState.ExecutionDimension> b() {
      return this.b;
   }

   public int c() {
      return this.c;
   }

   public String d() {
      return this.d;
   }

   public io.karatelabs.ext.rules.analysis.AbstractValue e() {
      return this.e;
   }

   public io.karatelabs.ext.rules.analysis.AbstractValue f() {
      return this.f;
   }

   public long g() {
      return this.g;
   }

   public boolean h() {
      return this.h;
   }

   public record ExecutionDimension(String b, AbstractBox.DimensionKind c, long d, long e, List<Object> f) {

      public ExecutionDimension(String b, AbstractBox.DimensionKind c, long d, long e, List<Object> f) {
         f = f == null ? null : List.copyOf(f);
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      public long a() {
         return this.e - this.d + 1L;
      }

      public String b() {
         return this.b;
      }

      public AbstractBox.DimensionKind c() {
         return this.c;
      }

      public long d() {
         return this.d;
      }

      public long e() {
         return this.e;
      }

      public List<Object> f() {
         return this.f;
      }
   }
}
