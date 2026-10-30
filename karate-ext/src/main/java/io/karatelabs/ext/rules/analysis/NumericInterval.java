package io.karatelabs.ext.rules.analysis;

import io.karatelabs.js.JsScalars;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record NumericInterval(double g, double h, boolean i) {
   private static final double d = 4.5035996E15F;

   public NumericInterval(double g, double h, boolean i) {
      if (Double.isNaN(g) || Double.isNaN(h) || Double.isInfinite(g) || Double.isInfinite(h)) {
         throw new RuleAnalysisException("NON_FINITE", -1, "an interval bound is not finite");
      } else if (g > h) {
         throw new IllegalArgumentException("lo > hi: " + g + " > " + h);
      } else {
         this.g = g;
         this.h = h;
         this.i = i;
      }
   }

   public static NumericInterval a(double var0) {
      return new NumericInterval(var0, var0, var0 == Math.rint(var0) && Math.abs(var0) < 4.5035996E15F);
   }

   public static NumericInterval a(double var0, double var2, boolean var4) {
      return new NumericInterval(var0, var2, var4);
   }

   public boolean a() {
      return this.g == this.h;
   }

   public boolean b(double var1) {
      return var1 >= this.g && var1 <= this.h;
   }

   public NumericInterval a(NumericInterval var1) {
      return new NumericInterval(Math.min(this.g, var1.g), Math.max(this.h, var1.h), this.i && var1.i);
   }

   private static NumericInterval a(double var0, double var2) {
      return new NumericInterval(var0 == 0.0 ? 0.0 : Math.nextDown(var0), var2 == 0.0 ? 0.0 : Math.nextUp(var2), false);
   }

   private static NumericInterval b(double var0, double var2, boolean var4) {
      return var4 && Math.abs(var0) < 4.5035996E15F && Math.abs(var2) < 4.5035996E15F ? new NumericInterval(var0, var2, true) : a(var0, var2);
   }

   private boolean c(double var1) {
      return this.a() && this.g == var1;
   }

   public NumericInterval b(NumericInterval var1) {
      if (this.c(0.0)) {
         return var1;
      } else {
         return var1.c(0.0) ? this : b(this.g + var1.g, this.h + var1.h, this.i && var1.i);
      }
   }

   public NumericInterval c(NumericInterval var1) {
      if (var1.c(0.0)) {
         return this;
      } else {
         return this.c(0.0) ? var1.b() : b(this.g - var1.h, this.h - var1.g, this.i && var1.i);
      }
   }

   public NumericInterval b() {
      return new NumericInterval(-this.h, -this.g, this.i);
   }

   public NumericInterval d(NumericInterval var1) {
      if (this.c(1.0)) {
         return var1;
      } else if (var1.c(1.0)) {
         return this;
      } else if (!this.c(0.0) && !var1.c(0.0)) {
         double var2 = this.g * var1.g;
         double var4 = this.g * var1.h;
         double var6 = this.h * var1.g;
         double var8 = this.h * var1.h;
         double var10 = Math.min(Math.min(var2, var4), Math.min(var6, var8));
         double var12 = Math.max(Math.max(var2, var4), Math.max(var6, var8));
         return b(var10, var12, this.i && var1.i);
      } else {
         return a(0.0);
      }
   }

   public NumericInterval a(NumericInterval var1, int var2) {
      if (var1.b(0.0)) {
         throw new RuleAnalysisException("RANGE_DIVISION", var2, "a divisor may be zero on this box");
      } else if (var1.c(1.0)) {
         return this;
      } else {
         double var3 = this.g / var1.g;
         double var5 = this.g / var1.h;
         double var7 = this.h / var1.g;
         double var9 = this.h / var1.h;
         double var11 = Math.min(Math.min(var3, var5), Math.min(var7, var9));
         double var13 = Math.max(Math.max(var3, var5), Math.max(var7, var9));
         boolean var15 = this.i && var1.i && this.a() && var1.a() && this.g % var1.g == 0.0;
         return b(var11, var13, var15);
      }
   }

   public NumericInterval b(NumericInterval var1, int var2) {
      if (!this.i || !var1.i) {
         throw new RuleAnalysisException("COERCION", var2, "'%' over non-integer values");
      } else if (var1.b(0.0)) {
         throw new RuleAnalysisException("RANGE_DIVISION", var2, "a modulus may be zero on this box");
      } else if (this.a() && var1.a()) {
         return a(this.g % var1.g);
      } else {
         double var3 = Math.max(Math.abs(var1.g), Math.abs(var1.h)) - 1.0;
         double var5 = this.g >= 0.0 ? 0.0 : Math.max(this.g, -var3);
         double var7 = this.h <= 0.0 ? 0.0 : Math.min(this.h, var3);
         return new NumericInterval(var5, var7, true);
      }
   }

   public NumericInterval e(NumericInterval var1) {
      return new NumericInterval(Math.min(this.g, var1.g), Math.min(this.h, var1.h), this.i && var1.i);
   }

   public NumericInterval f(NumericInterval var1) {
      return new NumericInterval(Math.max(this.g, var1.g), Math.max(this.h, var1.h), this.i && var1.i);
   }

   public NumericInterval c() {
      if (this.g >= 0.0) {
         return this;
      } else {
         return this.h <= 0.0 ? this.b() : new NumericInterval(0.0, Math.max(-this.g, this.h), this.i);
      }
   }

   public NumericInterval d() {
      return new NumericInterval(Math.floor(this.g), Math.floor(this.h), true);
   }

   public NumericInterval e() {
      double var1 = JsScalars.round(this.g);
      double var3 = JsScalars.round(this.h);
      return new NumericInterval(var1, var3, Math.abs(var1) < 4.5035996E15F && Math.abs(var3) < 4.5035996E15F);
   }

   public NumericInterval a(int var1) {
      double var2 = BigDecimal.valueOf(this.g).setScale(var1, RoundingMode.HALF_UP).doubleValue();
      double var4 = BigDecimal.valueOf(this.h).setScale(var1, RoundingMode.HALF_UP).doubleValue();
      return var1 == 0 ? new NumericInterval(var2, var4, true) : a(var2, var4);
   }

   public TernaryBoolean g(NumericInterval var1) {
      if (this.h < var1.g) {
         return io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE;
      } else {
         return this.g >= var1.h ? io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE;
      }
   }

   public TernaryBoolean h(NumericInterval var1) {
      if (this.h <= var1.g) {
         return io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE;
      } else {
         return this.g > var1.h ? io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE;
      }
   }

   public TernaryBoolean i(NumericInterval var1) {
      if (this.a() && var1.a()) {
         return io.karatelabs.ext.rules.analysis.TernaryBoolean.of(this.g == var1.g);
      } else {
         return !(this.h < var1.g) && !(var1.h < this.g) ? io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE : io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE;
      }
   }

   public TernaryBoolean f() {
      if (!this.b(0.0)) {
         return io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE;
      } else {
         return this.a() ? io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE;
      }
   }

   @Override
   public String toString() {
      return this.a() ? Double.toString(this.g) : "[" + this.g + "," + this.h + "]";
   }

   public double g() {
      return this.g;
   }

   public double h() {
      return this.h;
   }

   public boolean i() {
      return this.i;
   }
}
