package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleFlowGraph {
   private static final int b = 60;
   static final int a = 12;
   private final List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> c = new ArrayList<>();
   private final List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> d = new ArrayList<>();
   private final List<io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata> e = new ArrayList<>();
   private final Map<Integer, Integer> f = new LinkedHashMap<>();

   private RuleFlowGraph(String var1) {
      Node var2 = Engine.parse(var1);
      Node var3 = a(var2);
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var4 = new io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor("start", "start", "start", 0, 0);
      this.d.add(var4);
      this.a(var3, this.c);
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var5 = new io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor("end", "end", "end", 0, 0);
      String var6 = this.a(this.c, "start", "");
      this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var6, "end", ""));
      this.d.add(var5);
   }

   public static RuleFlowGraph a(String var0) {
      return new RuleFlowGraph(var0);
   }

   List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> a() {
      return this.c;
   }

   List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> b() {
      return a(this.c);
   }

   private static List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var0) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();

      for (Object var4 : var0) {
         if (d((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var4)) {
            var2.add(((RuleFlowGraph.LeafNode)var4).a());
         } else {
            a(var2, var1);
            switch (var4) {
               case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var7:
                  var1.add(new io.karatelabs.ext.rules.RuleFlowGraph.LoopNode(var7.a(), a(var7.b())));
                  break;
               case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var8:
                  ArrayList var9 = new ArrayList();

                  for (io.karatelabs.ext.rules.RuleFlowGraph.BranchArm var11 : var8.b()) {
                     var9.add(new io.karatelabs.ext.rules.RuleFlowGraph.BranchArm(var11.a(), a(var11.b())));
                  }

                  var1.add(new io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode(var8.a(), var9));
                  break;
               default:
                  var1.add(var4);
            }
         }
      }

      a(var2, var1);
      return var1;
   }

   private static boolean d(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var0) {
      if (var0 instanceof RuleFlowGraph.LeafNode var1) {
         String var2 = var1.a().b();
         return "process".equals(var2) ? true : "log".equals(var2) && !var1.a().c().startsWith("#");
      } else {
         return false;
      }
   }

   private static void a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> var0, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1) {
      if (!var0.isEmpty()) {
         if (var0.size() == 1) {
            var1.add(new RuleFlowGraph.LeafNode((io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor)var0.getFirst()));
         } else {
            var1.add(new io.karatelabs.ext.rules.RuleFlowGraph.GroupNode(b(var0), List.copyOf(var0)));
         }

         var0.clear();
      }
   }

   private static io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor b(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> var0) {
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var1 = (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor)var0.getFirst();
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var2 = (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor)var0.getLast();
      StringBuilder var3 = new StringBuilder();

      for (io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var5 : var0) {
         if ("log".equals(var5.b())) {
            if (var3.length() > 0) {
               var3.append(" · ");
            }

            var3.append(var5.c());
         }
      }

      return new io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor(var1.a(), "group", e(var3.toString()), var1.d(), var2.e());
   }

   public List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> c() {
      return this.d;
   }

   public List<io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata> d() {
      return this.e;
   }

   public io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor b(String var1) {
      return this.d.stream().filter(var1x -> var1x.a.equals(var1)).findFirst().orElse(null);
   }

   public List<io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata> c(String var1) {
      return this.e.stream().filter(var1x -> var1x.a.equals(var1)).toList();
   }

   static int a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var0) {
      return switch (var0) {
         case RuleFlowGraph.LeafNode var3 -> 1;
         case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var4 -> var4.b().size();
         case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var5 -> 1 + c(var5.b());
         case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var6 -> 1 + var6.b().stream().mapToInt(var0x -> c(var0x.b())).sum();
         default -> throw new MatchException(null, null);
      };
   }

   private static int c(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var0) {
      return var0.stream().mapToInt(RuleFlowGraph::a).sum();
   }

   static boolean b(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var0) {
      return (var0 instanceof io.karatelabs.ext.rules.RuleFlowGraph.LoopNode || var0 instanceof io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode) && a(var0) > 12;
   }

   public List<String> e() {
      ArrayList var1 = new ArrayList();
      b(this.c, var1);
      return var1;
   }

   private static void b(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var0, List<String> var1) {
      for (Object var3 : var0) {
         if (b((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var3)) {
            var1.add(c((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var3).a);
         }

         switch (var3) {
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var6:
               b(var6.b(), var1);
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var7:
               var7.b().forEach(var1x -> b(var1x.b(), var1));
               break;
            case RuleFlowGraph.LeafNode var8:
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var9:
               break;
            default:
               throw new MatchException(null, null);
         }
      }
   }

   static io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor c(io.karatelabs.ext.rules.RuleFlowGraph.FlowNode var0) {
      return switch (var0) {
         case RuleFlowGraph.LeafNode var3 -> var3.a();
         case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var4 -> var4.a();
         case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var5 -> var5.a();
         case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var6 -> var6.a();
         default -> throw new MatchException(null, null);
      };
   }

   public List<Map<String, Object>> f() {
      ArrayList var1 = new ArrayList();
      a(this.c, null, var1);
      return var1;
   }

   private static void a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var0, String var1, List<Map<String, Object>> var2) {
      for (Object var4 : var0) {
         io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var5 = c((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var4);
         boolean var6 = var4 instanceof RuleFlowGraph.LeafNode && "log".equals(var5.b) && var5.c.startsWith("#");
         boolean var7 = var4 instanceof io.karatelabs.ext.rules.RuleFlowGraph.LoopNode || var4 instanceof io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode && b((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var4);
         if (var6 || var7) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("id", var5.a);
            var8.put("kind", var6 ? "section" : var5.b);
            var8.put("label", var6 ? var5.c.replaceFirst("^#+\\s*", "") : var5.c);
            var8.put("line", var5.d);
            if (var7) {
               var8.put("size", a((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var4));
               var8.put("collapsible", b((io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var4));
            }

            var8.put("parent", var1);
            var2.add(var8);
         }

         String var15 = !(var4 instanceof io.karatelabs.ext.rules.RuleFlowGraph.LoopNode) && !(var4 instanceof io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode) ? var1 : var5.a;
         switch (var4) {
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var11:
               a(var11.b(), var15, var2);
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var12:
               var12.b().forEach(var2x -> a(var2x.b(), var15, var2));
               break;
            case RuleFlowGraph.LeafNode var13:
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var14:
               break;
            default:
               throw new MatchException(null, null);
         }
      }
   }

   private static Node a(Node var0) {
      for (Node var2 : var0.findAll(NodeType.VAR_DECL)) {
         if ("execute".equals(var2.getFirstToken().getText())) {
            Node var3 = var2.findFirstChild(NodeType.FN_EXPR);
            if (var3 == null) {
               var3 = var2.findFirstChild(NodeType.FN_ARROW_EXPR);
            }

            Node var4 = b(var3);
            if (var4 != null) {
               return var4;
            }
         }
      }

      for (Node var6 : var0.findAll(NodeType.FN_EXPR)) {
         if ("execute".equals(c(var6))) {
            Node var7 = b(var6);
            if (var7 != null) {
               return var7;
            }
         }
      }

      throw new RuleApiException(
         400, "NO_EXECUTE", "calc.js does not declare `const execute = function (calc) {...}` or `function execute(calc) {...}` — nothing to diagram"
      );
   }

   private static Node b(Node var0) {
      if (var0 == null) {
         return null;
      } else {
         var var1 = var0.findImmediateChildren(NodeType.BLOCK);
         return var1.isEmpty() ? null : (Node)var1.getFirst();
      }
   }

   private static String c(Node var0) {
      for (int var1 = 0; var1 < var0.size(); var1++) {
         Node var2 = var0.get(var1);
         if (!var2.isToken()) {
            return null;
         }

         if (var2.token.type == TokenType.IDENT) {
            return var2.getText();
         }
      }

      return null;
   }

   private void a(Node var1, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var2) {
      for (Node var4 : var1.findImmediateChildren(NodeType.STATEMENT)) {
         this.b(var4, var2);
      }
   }

   private void b(Node var1, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var2) {
      Node var3 = var1.getFirst();
      switch (var3.type) {
         case BLOCK:
            this.a(var3, var2);
            break;
         case IF_STMT:
            var2.add(this.d(var3));
            break;
         case SWITCH_STMT:
            var2.add(this.e(var3));
         case EOS:
            break;
         default:
            var2.add(this.f(var3));
      }
   }

   private io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode d(Node var1) {
      String var2 = i(var1.get(2));
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var3 = this.a("decision", var2, var1);
      ArrayList var4 = new ArrayList();
      this.b(var1.get(4), var4);
      ArrayList var5 = new ArrayList();
      if (var1.size() > 5) {
         this.b(var1.get(6), var5);
      }

      return new io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode(var3, List.of(new io.karatelabs.ext.rules.RuleFlowGraph.BranchArm("true", var4), new io.karatelabs.ext.rules.RuleFlowGraph.BranchArm("false", var5)));
   }

   private io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode e(Node var1) {
      String var2 = i(var1.get(2));
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var3 = this.a("decision", "switch (" + var2 + ")", var1);
      ArrayList var4 = new ArrayList();

      for (Node var6 : var1.findImmediateChildren(NodeType.CASE_BLOCK)) {
         ArrayList var7 = new ArrayList();

         for (int var8 = 3; var8 < var6.size(); var8++) {
            Node var9 = var6.get(var8);
            if (var9.type == NodeType.STATEMENT) {
               this.b(var9, var7);
            }
         }

         var4.add(new io.karatelabs.ext.rules.RuleFlowGraph.BranchArm(i(var6.get(1)), var7));
      }

      for (Node var11 : var1.findImmediateChildren(NodeType.DEFAULT_BLOCK)) {
         ArrayList var12 = new ArrayList();
         this.a(var11, var12);
         var4.add(new io.karatelabs.ext.rules.RuleFlowGraph.BranchArm("default", var12));
      }

      return new io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode(var3, var4);
   }

   private io.karatelabs.ext.rules.RuleFlowGraph.FlowNode f(Node var1) {
      Node var2 = g(var1);
      if (var2.type == NodeType.FN_CALL_EXPR) {
         String var3 = var2.getFirst().getText();
         if ("calc.log".equals(var3)) {
            return new RuleFlowGraph.LeafNode(this.a("log", this.h(var2), var1));
         }

         if (var3.endsWith(".forEach") || var3.endsWith(".map")) {
            Node var4 = var2.findFirstChild(NodeType.FN_ARROW_EXPR);
            if (var4 == null) {
               var4 = var2.findFirstChild(NodeType.FN_EXPR);
            }

            if (var4 != null) {
               String var5 = d(var2.getFirst().getFirst().getTextIncludingWhitespace());
               io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var6 = this.a("loop", "for each " + var5, var1);
               ArrayList var7 = new ArrayList();
               var var8 = var4.findImmediateChildren(NodeType.BLOCK);
               if (!var8.isEmpty()) {
                  this.a((Node)var8.getFirst(), var7);
               } else {
                  var7.add(new RuleFlowGraph.LeafNode(this.a("process", i(var4.getLast()), var4.getLast())));
               }

               return new io.karatelabs.ext.rules.RuleFlowGraph.LoopNode(var6, var7);
            }
         }
      }

      if (var2.type == NodeType.ASSIGN_EXPR && var2.getFirst().getText().startsWith("calc.output")) {
         return new RuleFlowGraph.LeafNode(this.a("output", i(var1), var1));
      } else {
         String var9 = var1.getText();
         return !var9.contains("lookup.") && !var9.contains("lookup[")
            ? new RuleFlowGraph.LeafNode(this.a("process", i(var1), var1))
            : new RuleFlowGraph.LeafNode(this.a("lookup", i(var1), var1));
      }
   }

   private static Node g(Node var0) {
      while ((var0.type == NodeType.EXPR_LIST || var0.type == NodeType.EXPR) && var0.size() > 0) {
         var0 = var0.getFirst();
      }

      return var0;
   }

   private String h(Node var1) {
      Node var2 = var1.findFirstChild(NodeType.FN_CALL_ARG);
      if (var2 != null) {
         String var3 = d(var2.getTextIncludingWhitespace());
         char var4 = var3.isEmpty() ? 0 : var3.charAt(0);
         if (var4 == '\'' || var4 == '"' || var4 == '`') {
            int var5 = var3.indexOf(var4, 1);
            if (var5 > 0) {
               String var6 = var3.substring(1, var5);
               return e(var5 < var3.length() - 1 ? var6 + "…" : var6);
            }
         }

         return e(var3);
      } else {
         return "log";
      }
   }

   private io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a(String var1, String var2, Node var3) {
      int var4 = var3.getFirstToken().line + 1;
      int var5 = var3.getLastToken().line + 1;
      int var6 = this.f.merge(var4, 1, Integer::sum) - 1;
      io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var7 = new io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor("L" + var4 + "#" + var6, var1, var2, var4, var5);
      this.d.add(var7);
      return var7;
   }

   private static String i(Node var0) {
      return e(d(var0.getTextIncludingWhitespace()));
   }

   private static String d(String var0) {
      return var0.replaceAll("\\s+", " ").trim();
   }

   private static String e(String var0) {
      return var0.length() > 60 ? var0.substring(0, 60) + "…" : var0;
   }

   private String a(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1, String var2, String var3) {
      for (Object var5 : var1) {
         var2 = switch (var5) {
            case RuleFlowGraph.LeafNode var8 -> {
               this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var2, var8.a().a, var3));
               yield var8.a().a;
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var9 -> {
               this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var2, var9.a().a, var3));
               yield var9.a().a;
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var10 -> {
               this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var2, var10.a().a, var3));
               this.d(var10.b());
               yield var10.a().a;
            }
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var11 -> {
               this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var2, var11.a().a, var3));
               yield this.a(var11);
            }
            default -> throw new MatchException(null, null);
         };
         var3 = "";
      }

      return var2;
   }

   private void d(List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var1) {
      if (!var1.isEmpty()) {
         Object var2 = (io.karatelabs.ext.rules.RuleFlowGraph.FlowNode)var1.getFirst();

         String var3 = switch (var2) {
            case RuleFlowGraph.LeafNode var6 -> var6.a().a;
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var7 -> var7.a().a;
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var8 -> this.a(var8);
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var9 -> {
               this.d(var9.b());
               yield var9.a().a;
            }
            default -> throw new MatchException(null, null);
         };
         this.a(var1.subList(1, var1.size()), var3, "");
      }
   }

   private String a(io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var1) {
      String var2 = var1.a().a + ".m";

      for (io.karatelabs.ext.rules.RuleFlowGraph.BranchArm var4 : var1.b()) {
         if (var4.b().isEmpty()) {
            this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var1.a().a, var2, var4.a()));
         } else {
            String var5 = this.a(var4.b(), var1.a().a, var4.a());
            this.e.add(new io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata(var5, var2, ""));
         }
      }

      this.d.add(new io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor(var2, "merge", "", var1.a().d, var1.a().d));
      return var2;
   }

   public String g() {
      StringBuilder var1 = new StringBuilder("flowchart TD\n");
      var1.append("  start([start])\n");
      this.a(var1, this.c, "  ");
      var1.append("  end_([end])\n");

      for (io.karatelabs.ext.rules.RuleFlowGraph.RuleFlowMetadata var3 : this.e) {
         var1.append("  ").append(f(var3.a));
         if (!var3.c.isEmpty()) {
            var1.append(" -->|").append(var3.c).append("| ");
         } else {
            var1.append(" --> ");
         }

         var1.append(f(var3.b)).append('\n');
      }

      return var1.toString();
   }

   private void a(StringBuilder var1, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> var2, String var3) {
      for (Object var5 : var2) {
         switch (var5) {
            case RuleFlowGraph.LeafNode var8:
               var1.append(var3).append(a(var8.a())).append('\n');
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.GroupNode var9:
               var1.append(var3).append(a(var9.a())).append('\n');
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.LoopNode var10:
               var1.append(var3).append("subgraph ").append(f(var10.a().a)).append("[\"").append(g(var10.a().c)).append("\"]\n");
               this.a(var1, var10.b(), var3 + "  ");
               var1.append(var3).append("end\n");
               break;
            case io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode var11:
               var1.append(var3).append(f(var11.a().a)).append("{\"").append(g(var11.a().c)).append("\"}\n");

               for (io.karatelabs.ext.rules.RuleFlowGraph.BranchArm var13 : var11.b()) {
                  this.a(var1, var13.b(), var3);
               }

               var1.append(var3).append(f(var11.a().a)).append("_m((\" \"))\n");
               break;
            default:
               throw new MatchException(null, null);
         }
      }
   }

   private static String a(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor var0) {
      String var1 = g(var0.c);
      String var2 = var0.b;

      return switch (var2) {
         case "log" -> f(var0.a) + ">\"" + var1 + "\"]";
         case "lookup" -> f(var0.a) + "[(\"" + var1 + "\")]";
         case "output" -> f(var0.a) + "[/\"" + var1 + "\"/]";
         default -> f(var0.a) + "[\"" + var1 + "\"]";
      };
   }

   private static String f(String var0) {
      return "end".equals(var0) ? "end_" : var0.replace('#', '_').replace('.', '_');
   }

   private static String g(String var0) {
      return var0.replace("\"", "#quot;");
   }

   record LeafNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a) implements io.karatelabs.ext.rules.RuleFlowGraph.FlowNode {

      LeafNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a) {
         this.a = a;
      }
   }

   record DecisionNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.BranchArm> b) implements io.karatelabs.ext.rules.RuleFlowGraph.FlowNode {

      DecisionNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.BranchArm> b) {
         this.a = a;
         this.b = b;
      }
   }

   public record RuleFlowMetadata(String a, String b, String c) {

      public RuleFlowMetadata(String a, String b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   public record FlowNodeDescriptor(String a, String b, String c, int d, int e) {

      public FlowNodeDescriptor(String a, String b, String c, int d, int e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   record GroupNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> b) implements io.karatelabs.ext.rules.RuleFlowGraph.FlowNode {

      GroupNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor> b) {
         this.a = a;
         this.b = b;
      }
   }

   sealed interface FlowNode permits RuleFlowGraph.LeafNode, io.karatelabs.ext.rules.RuleFlowGraph.DecisionNode, io.karatelabs.ext.rules.RuleFlowGraph.LoopNode, io.karatelabs.ext.rules.RuleFlowGraph.GroupNode {
   }

   record BranchArm(String a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> b) {

      BranchArm(String a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> b) {
         this.a = a;
         this.b = b;
      }
   }

   record LoopNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> b) implements io.karatelabs.ext.rules.RuleFlowGraph.FlowNode {

      LoopNode(io.karatelabs.ext.rules.RuleFlowGraph.FlowNodeDescriptor a, List<io.karatelabs.ext.rules.RuleFlowGraph.FlowNode> b) {
         this.a = a;
         this.b = b;
      }
   }
}
