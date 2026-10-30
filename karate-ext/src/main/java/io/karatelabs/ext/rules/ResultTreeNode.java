package io.karatelabs.ext.rules;

import java.util.List;

public record ResultTreeNode(String component1, int component2, int component3, boolean component4, String e, String f, Double g, String h, String i, List<RuleDiffLine> j, List<ResultTreeNode> k) {
   public static final String a = "unchanged";
   public static final String b = "added";
   public static final String c = "removed";
   public static final String d = "changed";

   public ResultTreeNode(String var1, int var2, int var3, boolean var4, String var5, List<ResultTreeNode> var6) {
      this(var1, var2, var3, var4, var5, null, null, null, null, null, var6);
   }

   public ResultTreeNode(String var1, int var2, int var3, boolean var4, String var5, String var6, Double var7, List<ResultTreeNode> var8) {
      this(var1, var2, var3, var4, var5, var6, var7, null, null, null, var8);
   }

   public ResultTreeNode(String component1, int component2, int component3, boolean component4, String e, String f, Double g, String h, String i, List<RuleDiffLine> j, List<ResultTreeNode> k) {
      this.component1 = component1;
      this.component2 = component2;
      this.component3 = component3;
      this.component4 = component4;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
      this.k = k;
   }

   public String a() {
      return this.component1;
   }

   public int b() {
      return this.component2;
   }

   public int c() {
      return this.component3;
   }

   public boolean d() {
      return this.component4;
   }

   public String e() {
      return this.e;
   }

   public String f() {
      return this.f;
   }

   public Double g() {
      return this.g;
   }

   public String h() {
      return this.h;
   }

   public String i() {
      return this.i;
   }

   public List<RuleDiffLine> j() {
      return this.j;
   }

   public List<ResultTreeNode> k() {
      return this.k;
   }
}
