package io.karatelabs.ext.rules;

import java.util.ArrayList;
import java.util.List;

public class ExecutionTraceNode implements Comparable<ExecutionTraceNode> {
   public final String a;
   public final int b;
   public final ExecutionTraceNode c;
   public final boolean d;
   public final String e;
   public final List<ExecutionTraceNode> f = new ArrayList<>();
   public final int g;
   public String h;
   public Double i;

   public ExecutionTraceNode(String var1, int var2, ExecutionTraceNode var3, boolean var4, String var5) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      if (var3 != null) {
         var3.f.add(this);
      }

      if (var5 == null) {
         this.e = null;
         this.g = 0;
      } else {
         var5 = var5.trim();
         int var6 = 0;

         for (int var7 = 0; var7 < var5.length() && var5.charAt(var7) == '#'; var7++) {
            var6++;
         }

         if (var6 > 0) {
            this.e = var5.substring(var6).trim();
         } else {
            this.e = var5;
         }

         this.g = var6;
      }
   }

   public boolean a() {
      for (ExecutionTraceNode var2 : this.f) {
         if (!var2.a()) {
            return false;
         }
      }

      return this.d;
   }

   @Override
   public int compareTo(ExecutionTraceNode other) {
      return this.a(other);
   }

   public int a(ExecutionTraceNode var1) {
      return this.b - var1.b;
   }

   @Override
   public String toString() {
      return this.a + " " + this.e;
   }
}
