package io.karatelabs.ext.rules.analysis;

import io.karatelabs.ext.rules.NumericBoundsParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class AbstractBox {
   private static final double d = 9.007199E15F;
   public final List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> a;
   final long[] b;
   final long[] c;

   public AbstractBox(List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1) {
      this.a = List.copyOf(var1);
      this.b = new long[var1.size()];
      this.c = new long[var1.size()];

      for (int var2 = 0; var2 < var1.size(); var2++) {
         this.b[var2] = ((io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension)var1.get(var2)).e();
         this.c[var2] = ((io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension)var1.get(var2)).f();
      }
   }

   private AbstractBox(List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, long[] var2, long[] var3) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }

   public static List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> a(Map<String, Object> var0) {
      ArrayList<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1 = new ArrayList();

      for (Entry var3 : var0.entrySet()) {
         String var4 = (String)var3.getKey();
         Object var5 = var3.getValue();
         if (!(var5 instanceof List var7)) {
            if (var5 instanceof String var6) {
               String var8 = var6.startsWith("##") ? var6.substring(1) : var6;
               if (var8.equals("#boolean")) {
                  var1.add(new io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension(var4, io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.BOOL, 0L, 1L, List.of(Boolean.FALSE, Boolean.TRUE)));
                  continue;
               }

               if (var8.startsWith("#int[")) {
                  io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var9;
                  try {
                     var9 = io.karatelabs.ext.rules.NumericBoundsParser.a(var8);
                  } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var12) {
                     if (!var12.a.equals("notExact")) {
                        throw var12;
                     }

                     throw new RuleAnalysisException("INEXACT_BOUND", -1, var4 + " " + var6);
                  }

                  if (var9.a() != null && var9.b() != null) {
                     if (!(Math.abs(var9.a()) > 9.007199E15F) && !(Math.abs(var9.b()) > 9.007199E15F)) {
                        var1.add(new io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension(var4, io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.INT, var9.a().longValue(), var9.b().longValue(), null));
                        continue;
                     }

                     throw new RuleAnalysisException("INEXACT_BOUND", -1, var4 + " " + var6);
                  }

                  throw new RuleAnalysisException("UNBOUNDED_AXIS", -1, var4);
               }

               throw new RuleAnalysisException("OPEN_AXIS", -1, var4 + " " + var6);
            }

            throw new RuleAnalysisException("OPEN_AXIS", -1, var4);
         } else {
            if (var7.isEmpty()) {
               throw new RuleAnalysisException("EMPTY_ENUM", -1, var4);
            }

            a(var4, var7);
            var1.add(new io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension(var4, io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.ENUM, 0L, var7.size() - 1, new ArrayList<>(var7)));
         }
      }

      if (var1.size() > 64) {
         throw new RuleAnalysisException("TOO_MANY_AXES", -1, var1.size() + " axes; the dependency set is a 64-bit mask");
      } else {
         long var13 = 1L;

         for (io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var15 : (Iterable<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension>) (Iterable<?>) (var1)) {
            try {
               var13 = Math.multiplyExact(var13, var15.a());
            } catch (ArithmeticException var11) {
               throw new RuleAnalysisException("DOMAIN_TOO_LARGE", -1, "the declared domain has more rows than a long counts");
            }
         }

         return var1;
      }
   }

   private static void a(String var0, List<?> var1) {
      String var2 = null;

      for (Object var4 : var1) {
         String var5 = var4 instanceof String ? "string" : (var4 instanceof Boolean ? "boolean" : (var4 instanceof Number ? "number" : null));
         if (var5 == null) {
            throw new RuleAnalysisException("ENUM_MEMBER", -1, var0 + ": a member the abstraction cannot state");
         }

         if (var2 == null) {
            var2 = var5;
         } else if (!var2.equals(var5)) {
            throw new RuleAnalysisException("ENUM_MIXED", -1, var0 + ": " + var2 + " and " + var5 + " members");
         }
      }
   }

   public int a() {
      return this.a.size();
   }

   public long b() {
      long var1 = 1L;

      for (int var3 = 0; var3 < this.b.length; var3++) {
         var1 = Math.multiplyExact(var1, this.c[var3] - this.b[var3] + 1L);
      }

      return var1;
   }

   public boolean c() {
      for (int var1 = 0; var1 < this.b.length; var1++) {
         if (this.b[var1] != this.c[var1]) {
            return false;
         }
      }

      return true;
   }

   public long a(int var1) {
      return this.c[var1] - this.b[var1] + 1L;
   }

   public int a(long var1) {
      int var3 = -1;
      long var4 = 1L;

      for (int var6 = 0; var6 < this.b.length; var6++) {
         if ((var1 & 1L << var6) != 0L) {
            long var7 = this.a(var6);
            if (var7 > var4) {
               var4 = var7;
               var3 = var6;
            }
         }
      }

      return var3;
   }

   public AbstractBox[] b(int var1) {
      return this.a(var1, this.c(var1));
   }

   long c(int var1) {
      return this.b[var1] + (this.a(var1) - 1L) / 2L + 1L;
   }

   public AbstractBox[] a(int var1, long var2) {
      if (var1 < 0 || var1 >= this.b.length) {
         throw new IllegalArgumentException("axis " + var1 + " outside [0," + this.b.length + ")");
      } else if (var2 > this.b[var1] && var2 <= this.c[var1]) {
         long[] var4 = (long[])this.c.clone();
         var4[var1] = var2 - 1L;
         long[] var5 = (long[])this.b.clone();
         var5[var1] = var2;
         return new AbstractBox[]{new AbstractBox(this.a, (long[])this.b.clone(), var4), new AbstractBox(this.a, var5, (long[])this.c.clone())};
      } else {
         throw new IllegalArgumentException("cut " + var2 + " outside (" + this.b[var1] + "," + this.c[var1] + "]");
      }
   }

   boolean b(int var1, long var2) {
      return var1 >= 0 && var1 < this.b.length && var2 > this.b[var1] && var2 <= this.c[var1];
   }

   public io.karatelabs.ext.rules.analysis.AbstractValue d(int var1) {
      io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var2 = this.a.get(var1);
      return a(var2.d(), var2.g(), var1, this.b[var1], this.c[var1]);
   }

   public static io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind var0, List<Object> var1, int var2, long var3, long var5) {
      long var7 = 1L << var2;
      if (var0 == io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.INT) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(io.karatelabs.ext.rules.analysis.NumericInterval.a(var3, var5, true), var7);
      } else {
         return (io.karatelabs.ext.rules.analysis.AbstractValue)(var1.get(0) instanceof Number
            ? new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(io.karatelabs.ext.rules.analysis.NumericInterval.a(((Number)var1.get((int)var3)).doubleValue()), var7)
            : new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(var1.subList((int)var3, (int)var5 + 1), var7));
      }
   }

   public boolean e(int var1) {
      return this.b[var1] != this.c[var1] && this.a.get(var1).b();
   }

   public Map<String, Object> d() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (int var2 = 0; var2 < this.b.length; var2++) {
         io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var3 = this.a.get(var2);
         var1.put(var3.c(), var3.d() == io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.INT ? b(this.b[var2]) : var3.g().get((int)this.b[var2]));
      }

      return var1;
   }

   private static Object b(long var0) {
      return var0 >= -2147483648L && var0 <= 2147483647L ? (int)var0 : var0;
   }

   public List<Map<String, Object>> e() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      long[] var2 = (long[])this.b.clone();

      while (true) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();

         for (int var4 = 0; var4 < var2.length; var4++) {
            io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var5 = this.a.get(var4);
            var3.put(var5.c(), var5.d() == io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.INT ? b(var2[var4]) : var5.g().get((int)var2[var4]));
         }

         var1.add(var3);

         int var6;
         for (var6 = var2.length - 1; var6 >= 0 && var2[var6] == this.c[var6]; var6--) {
            var2[var6] = this.b[var6];
         }

         if (var6 < 0) {
            return var1;
         }

         var2[var6]++;
      }
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder("{");

      for (int var2 = 0; var2 < this.b.length; var2++) {
         io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var3 = this.a.get(var2);
         if (var2 > 0) {
            var1.append(", ");
         }

         var1.append(var3.c()).append(':');
         if (var3.d() == io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind.INT) {
            var1.append(this.b[var2] == this.c[var2] ? String.valueOf(this.b[var2]) : "[" + this.b[var2] + "," + this.c[var2] + "]");
         } else {
            var1.append(var3.g().subList((int)this.b[var2], (int)this.c[var2] + 1));
         }
      }

      return var1.append('}').toString();
   }

   public static enum DimensionKind {
      INT,
      ENUM,
      BOOL;
   }

   public record BoxDimension(String c, io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind d, long e, long f, List<Object> g) {

      public BoxDimension(String c, io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind d, long e, long f, List<Object> g) {
         g = g == null ? null : List.copyOf(g);
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }

      public long a() {
         return this.f - this.e + 1L;
      }

      boolean b() {
         return this.g != null && this.g.get(0) instanceof Number;
      }

      public String c() {
         return this.c;
      }

      public io.karatelabs.ext.rules.analysis.AbstractBox.DimensionKind d() {
         return this.d;
      }

      public long e() {
         return this.e;
      }

      public long f() {
         return this.f;
      }

      public List<Object> g() {
         return this.g;
      }
   }
}
