package io.karatelabs.ext.rules;

import java.util.List;
import java.util.Set;

final class RuleFlowSvgRenderer {
   private static final int a = 250;
   private static final int b = 34;
   private static final int c = 280;
   private static final int d = 58;
   private static final int e = 100;
   private static final int f = 30;
   private static final int g = 28;
   private static final int h = 36;
   private static final int i = 16;
   private static final int j = 26;
   private static final int k = 5;
   private static final int l = 70;
   private static final int m = 24;
   private static final int n = 48;
   private static final int o = 48;
   private static final double p = 6.3;
   private final StringBuilder q = new StringBuilder();
   private final Set<String> r;
   private final boolean s;

   private RuleFlowSvgRenderer(Set<String> var1) {
      this.r = var1;
      this.s = var1.contains("all");
   }

   static String a(RuleFlowGraph var0) {
      return a(var0, Set.of());
   }

   static String a(RuleFlowGraph var0, Set<String> var1) {
      return a(var0, var1, false);
   }

   static String a(RuleFlowGraph var0, Set<String> var1, boolean var2) {
      return new RuleFlowSvgRenderer(var1).a(var2 ? var0.b() : var0.a());
   }

   private boolean a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var1) {
      return io.karatelabs.ext.rules.RuleFlowGraph.b(var1) && !this.s && !this.r.contains(io.karatelabs.ext.rules.RuleFlowGraph.c(var1).a());
   }

   private String a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1) {
      io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var2 = this.b(var1);
      int var3 = Math.max(var2.a, 100) + 48;
      int var4 = 82 + var2.b + 28 + 30 + 24;
      int var5 = var3 / 2;
      this.q
         .append("<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"kd-svg\" viewBox=\"0 0 ")
         .append(var3)
         .append(' ')
         .append(var4)
         .append("\" width=\"")
         .append(var3)
         .append("\" height=\"")
         .append(var4)
         .append("\">\n");
      this.q
         .append(
            "<defs><marker id=\"kd-arr\" viewBox=\"0 0 8 8\" refX=\"7\" refY=\"4\" markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\"><path d=\"M 0 0 L 8 4 L 0 8 z\" class=\"kd-arrow\"/></marker></defs>\n"
         );
      this.a("start", "start", var5, 24);
      byte var6 = 82;
      this.a(new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var5, 54), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var5, var6), null);
      io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var7 = this.a(var1, var5, var6);
      int var8 = var4 - 24 - 30;
      this.a(var7, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var5, var8), null);
      this.a("end", "end", var5, var8);
      this.q.append("</svg>\n");
      return this.q.toString();
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size b(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1) {
      int var2 = 0;
      int var3 = 0;

      for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var5 : var1) {
         io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var6 = this.b(var5);
         var2 = Math.max(var2, var6.a);
         var3 += (var3 > 0 ? 28 : 0) + var6.b;
      }

      return new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(var2, var3);
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size b(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var1) {
      if (this.a((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var1)) {
         return var1 instanceof io.karatelabs.ext.rules.RuleFlowGraph.LoopNode ? new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(282, 48) : new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(Math.max(280, 250), 134);
      } else {
         return switch (var1) {
            case RuleFlowGraph.LeafNode var4 -> new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(250, 34);
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var5 -> new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(250, 48);
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var6 -> {
               io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var13 = this.b(var6.b());
               yield new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(Math.max(var13.a, 250) + 32, 26 + var13.b + 32);
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var7 -> {
               int var8 = 0;
               int var9 = 0;

               for (io.karatelabs.ext.rules.RuleFlowGraph.BranchArm var11 : var7.b()) {
                  io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var12 = this.b(var11.b());
                  var8 += (var8 > 0 ? 36 : 0) + Math.max(var12.a, 70);
                  var9 = Math.max(var9, var12.b);
               }

               int var14 = Math.max(280, var8);
               int var15 = 86 + var9 + 28 + 10;
               yield new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size(var14, var15);
            }
            default -> throw new MatchException(null, null);
         };
      }
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1, int var2, int var3) {
      io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var4 = null;

      for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var6 : var1) {
         io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var7 = this.b(var6);
         if (var4 != null) {
            this.a(var4, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var3), null);
         }

         var4 = this.a(var6, var2, var3, var7);
         var3 += var7.b + 28;
      }

      return var4;
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var1, int var2, int var3, io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var4) {
      if (this.a((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var1)) {
         return this.b((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var1, var2, var3, var4);
      } else {
         return switch (var1) {
            case RuleFlowGraph.LeafNode var7 -> {
               this.a(var7.a(), var2, var3);
               yield new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var3 + 34);
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var8 -> {
               this.a(var8, var2, var3);
               yield new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var3 + 48);
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var9 -> {
               io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var12 = var9.a();
               int var11 = var2 - var4.a / 2;
               this.q.append("<g id=\"kd-").append(b(var12.a())).append("\" class=\"kd-node kd-loop").append("\" data-line=\"").append(var12.d()).append("\">");
               this.q
                  .append("<rect class=\"kd-loop-box\" x=\"")
                  .append(var11)
                  .append("\" y=\"")
                  .append(var3)
                  .append("\" width=\"")
                  .append(var4.a)
                  .append("\" height=\"")
                  .append(var4.b)
                  .append("\" rx=\"10\"/>");
               this.a("kd-text kd-text-quiet", var2, var3 + 17, var12.c(), var4.a - 16);
               if (io.karatelabs.ext.rules.RuleFlowGraph.b(var9)) {
                  this.a(var11 + 4, var3 + 4, var12.a(), true);
               }

               this.a(var12.c());
               this.q.append("</g>\n");
               this.a(var9.b(), var2, var3 + 26 + 16);
               yield new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var3 + var4.b);
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var10 -> this.a(var10, var2, var3, var4);
            default -> throw new MatchException(null, null);
         };
      }
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point b(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var1, int var2, int var3, io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var4) {
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var5 = io.karatelabs.ext.rules.RuleFlowGraph.c(var1);
      int var6 = io.karatelabs.ext.rules.RuleFlowGraph.a(var1) - 1;
      if (var1 instanceof io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var7) {
         int var8 = var3 + 58;
         int var9 = var3 + 29;
         this.q.append("<g id=\"kd-").append(b(var5.a())).append("\" class=\"kd-node kd-decision").append("\" data-line=\"").append(var5.d()).append("\">");
         this.q
            .append("<polygon class=\"kd-shape\" points=\"")
            .append(var2)
            .append(',')
            .append(var3)
            .append(' ')
            .append(var2 + 140)
            .append(',')
            .append(var9)
            .append(' ')
            .append(var2)
            .append(',')
            .append(var8)
            .append(' ')
            .append(var2 - 140)
            .append(',')
            .append(var9)
            .append("\"/>");
         this.a("kd-text", var2, var9 + 4, var5.c(), 240);
         this.a(var5.c());
         this.q.append("</g>\n");
         int var10 = var8 + 28;
         this.a(new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var8), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var10), null);
         this.a(var5, var2, var10, 250, null, var6 + " steps in " + var7.b().size() + " lanes");
         return new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var10 + 48);
      } else {
         this.a(var5, var2, var3, var4.a, var5.c(), var6 + " steps hidden");
         return new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var3 + 48);
      }
   }

   private void a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var1, int var2, int var3, int var4, String var5, String var6) {
      this.q.append("<g id=\"kd-").append(b(var1.a())).append(".c\" class=\"kd-node kd-collapsed").append("\" data-line=\"").append(var1.d()).append("\">");
      this.q
         .append("<rect class=\"kd-collapsed-box\" x=\"")
         .append(var2 - var4 / 2)
         .append("\" y=\"")
         .append(var3)
         .append("\" width=\"")
         .append(var4)
         .append("\" height=\"")
         .append(48)
         .append("\" rx=\"10\"/>");
      if (var5 != null) {
         this.a("kd-text", var2, var3 + 19, var5, var4 - 16);
         this.a("kd-text kd-text-quiet", var2, var3 + 36, var6, var4 - 16);
      } else {
         this.a("kd-text kd-text-quiet", var2, var3 + 24 + 4, var6, var4 - 16);
      }

      this.a(var2 - var4 / 2 + 4, var3 + 4, var1.a(), false);
      this.a(var5 != null ? var5 : var6);
      this.q.append("</g>\n");
   }

   private io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point a(io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var1, int var2, int var3, io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var4) {
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var5 = var1.a();
      int var6 = var3 + 58;
      int var7 = var3 + 29;
      this.q.append("<g id=\"kd-").append(b(var5.a())).append("\" class=\"kd-node kd-decision").append("\" data-line=\"").append(var5.d()).append("\">");
      this.q
         .append("<polygon class=\"kd-shape\" points=\"")
         .append(var2)
         .append(',')
         .append(var3)
         .append(' ')
         .append(var2 + 140)
         .append(',')
         .append(var7)
         .append(' ')
         .append(var2)
         .append(',')
         .append(var6)
         .append(' ')
         .append(var2 - 140)
         .append(',')
         .append(var7)
         .append("\"/>");
      this.a("kd-text", var2, var7 + 4, var5.c(), 240);
      if (io.karatelabs.ext.rules.RuleFlowGraph.b(var1)) {
         this.a(var2 - 140, var3, var5.a(), true);
      }

      this.a(var5.c());
      this.q.append("</g>\n");
      var var8 = var1.b();
      int var9 = var6 + 28;
      int var10 = 0;
      int[] var11 = new int[var8.size()];
      int var12 = 0;

      for (int var13 = 0; var13 < var8.size(); var13++) {
         io.karatelabs.ext.rules.RuleFlowSvgRenderer.Size var14 = this.b(((io.karatelabs.ext.rules.RuleFlowGraph.BranchArm)var8.get(var13)).b());
         var11[var13] = Math.max(var14.a, 70);
         var10 += (var13 > 0 ? 36 : 0) + var11[var13];
         var12 = Math.max(var12, var14.b);
      }

      int var20 = var9 + var12 + 28 + 5;
      int var21 = var2 - var10 / 2;

      for (int var15 = 0; var15 < var8.size(); var15++) {
         io.karatelabs.ext.rules.RuleFlowGraph.BranchArm var16 = (io.karatelabs.ext.rules.RuleFlowGraph.BranchArm)var8.get(var15);
         int var17 = var21 + var11[var15] / 2;
         var21 += var11[var15] + 36;
         io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var18 = var15 == 0 ? new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var6) : new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2 + 140, var7);
         if (var16.b().isEmpty()) {
            this.a(var18, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var17, var18.b), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var17, var20), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2 + (var17 < var2 ? -5 : 5), var20));
            this.a(var17 + 6, var18.b - 5, var16.a());
         } else {
            this.a(var18, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var17, var18.b), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var17, var9));
            this.a(var17 + 6, var9 - 8, var16.a());
            io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var19 = this.a(var16.b(), var17, var9);
            if (var19.a == var2) {
               this.a(var19, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var20 - 5));
            } else {
               this.a(var19, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var19.a, var20), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2 + (var19.a < var2 ? -5 : 5), var20));
            }
         }
      }

      this.q
         .append("<circle id=\"kd-")
         .append(b(var5.a()))
         .append(".m\" class=\"kd-merge\" cx=\"")
         .append(var2)
         .append("\" cy=\"")
         .append(var20)
         .append("\" r=\"")
         .append(5)
         .append("\" data-line=\"")
         .append(var5.d())
         .append("\"/>\n");
      return new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2, var20 + 5);
   }

   private void a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var1, int var2, int var3) {
      String var5 = var1.b();

      String var4 = switch (var5) {
         case "log" -> "kd-log";
         case "lookup" -> "kd-lookup";
         case "output" -> "kd-output";
         default -> "kd-process";
      };
      this.q.append("<g id=\"kd-").append(b(var1.a())).append("\" class=\"kd-node ").append(var4).append("\" data-line=\"").append(var1.d()).append("\">");
      this.q
         .append("<rect class=\"kd-shape\" x=\"")
         .append(var2 - 125)
         .append("\" y=\"")
         .append(var3)
         .append("\" width=\"")
         .append(250)
         .append("\" height=\"")
         .append(34)
         .append("\" rx=\"")
         .append("log".equals(var1.b()) ? 12 : 4)
         .append("\"/>");
      this.a("log".equals(var1.b()) ? "kd-text kd-text-quiet" : "kd-text", var2, var3 + 17 + 4, var1.c(), 234);
      this.a(var1.c());
      this.q.append("</g>\n");
   }

   private void a(io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var1, int var2, int var3) {
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var4 = var1.a();
      int var5 = var1.b().size();
      this.q.append("<g id=\"kd-").append(b(var4.a())).append("\" class=\"kd-node kd-group").append("\" data-line=\"").append(var4.d()).append("\">");
      this.q
         .append("<rect class=\"kd-group-box\" x=\"")
         .append(var2 - 125)
         .append("\" y=\"")
         .append(var3)
         .append("\" width=\"")
         .append(250)
         .append("\" height=\"")
         .append(48)
         .append("\" rx=\"8\"/>");
      String var6 = "· " + var5 + " steps";
      if (var4.c().isEmpty()) {
         this.a("kd-text kd-text-quiet", var2, var3 + 24 + 4, var6, 234);
      } else {
         this.a("kd-text", var2, var3 + 19, var4.c(), 234);
         this.a("kd-text kd-text-quiet", var2, var3 + 36, var6, 234);
      }

      StringBuilder var7 = new StringBuilder();

      for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var9 : var1.b()) {
         if (var7.length() > 0) {
            var7.append('\n');
         }

         var7.append('L').append(var9.d()).append("  ").append(var9.c());
      }

      this.a(var7.toString());
      this.q.append("</g>\n");
   }

   private void a(int var1, int var2, String var3, boolean var4) {
      this.q.append("<g class=\"kd-toggle\" data-").append(var4 ? "collapse" : "expand").append("=\"").append(b(var3)).append("\">");
      this.q.append("<rect class=\"kd-toggle-box\" x=\"").append(var1).append("\" y=\"").append(var2).append("\" width=\"16\" height=\"16\" rx=\"3\"/>");
      this.q
         .append("<text class=\"kd-toggle-sign\" x=\"")
         .append(var1 + 8)
         .append("\" y=\"")
         .append(var2 + 12)
         .append("\" text-anchor=\"middle\">")
         .append(var4 ? "−" : "+")
         .append("</text>");
      this.q.append("</g>");
   }

   private void a(String var1, String var2, int var3, int var4) {
      this.q.append("<g id=\"kd-").append(var1).append("\" class=\"kd-node kd-terminal\">");
      this.q
         .append("<rect class=\"kd-shape\" x=\"")
         .append(var3 - 50)
         .append("\" y=\"")
         .append(var4)
         .append("\" width=\"")
         .append(100)
         .append("\" height=\"")
         .append(30)
         .append("\" rx=\"")
         .append(15)
         .append("\"/>");
      this.a("kd-text", var3, var4 + 15 + 4, var2, 88);
      this.q.append("</g>\n");
   }

   private void a(io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var1, io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point var2, String var3) {
      if (var1.a != var2.a && var1.b != var2.b) {
         int var4 = (var1.b + var2.b) / 2;
         this.a(var1, new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var1.a, var4), new io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point(var2.a, var4), var2);
      } else {
         this.a(var1, var2);
      }

      if (var3 != null && !var3.isEmpty()) {
         this.a(Math.max(var1.a, var2.a) + 6, (var1.b + var2.b) / 2 - 4, var3);
      }
   }

   private void a(io.karatelabs.ext.rules.RuleFlowSvgRenderer.Point... var1) {
      this.q.append("<polyline class=\"kd-edge\" marker-end=\"url(#kd-arr)\" points=\"");

      for (int var2 = 0; var2 < var1.length; var2++) {
         if (var2 > 0) {
            this.q.append(' ');
         }

         this.q.append(var1[var2].a).append(',').append(var1[var2].b);
      }

      this.q.append("\"/>\n");
   }

   private void a(int var1, int var2, String var3) {
      this.q.append("<text class=\"kd-edge-label\" x=\"").append(var1).append("\" y=\"").append(var2).append("\">").append(b(var3)).append("</text>\n");
   }

   private void a(String var1, int var2, int var3, String var4, int var5) {
      int var6 = (int)(var5 / 6.3);
      String var7 = var4.length() > var6 && var6 > 1 ? var4.substring(0, var6 - 1) + "…" : var4;
      this.q
         .append("<text class=\"")
         .append(var1)
         .append("\" x=\"")
         .append(var2)
         .append("\" y=\"")
         .append(var3)
         .append("\" text-anchor=\"middle\">")
         .append(b(var7))
         .append("</text>");
   }

   private void a(String var1) {
      this.q.append("<title>").append(b(var1)).append("</title>");
   }

   private static String b(String var0) {
      return var0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
   }

   private record Point(int a, int b) {

      private Point(int a, int b) {
         this.a = a;
         this.b = b;
      }
   }

   private record Size(int a, int b) {

      private Size(int a, int b) {
         this.a = a;
         this.b = b;
      }
   }
}
