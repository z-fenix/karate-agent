package io.karatelabs.js;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class SlotTable {
   static volatile boolean ENABLED = !"false".equals(System.getProperty("karate.js.slotFrames"));
   static final Object UNDECLARED = new Object() {
      @Override
      public String toString() {
         return "<undeclared>";
      }
   };
   static final Object TDZ = new Object() {
      @Override
      public String toString() {
         return "<tdz>";
      }
   };
   private static final Object BAIL = new Object();
   private static final Object DEFERRED = new Object();
   static final byte KIND_PARAM = 0;
   static final byte KIND_VAR = 1;
   static final byte KIND_FN_DECL = 2;
   static final byte KIND_LET = 3;
   static final byte KIND_CONST = 4;
   final String[] names;
   final byte[] kinds;
   final int[] paramSlots;
   private final Map<String, Integer> byName;

   private SlotTable(String[] names, byte[] kinds, int[] paramSlots, Map<String, Integer> byName) {
      this.names = names;
      this.kinds = kinds;
      this.paramSlots = paramSlots;
      this.byName = byName;
   }

   int size() {
      return this.names.length;
   }

   int indexOf(String name) {
      Integer i = this.byName.get(name);
      return i == null ? -1 : i;
   }

   boolean isFunctionScoped(int slot) {
      Integer i = this.byName.get(this.names[slot]);
      return i != null && i == slot;
   }

   Object[] newFrame() {
      Object[] frame = new Object[this.names.length];
      Arrays.fill(frame, UNDECLARED);
      return frame;
   }

   int[] lexicalSlots(String[] scopeNames) {
      int[] slots = null;

      for (int i = 0; i < scopeNames.length; i++) {
         for (int s = 0; s < this.names.length; s++) {
            if ((this.kinds[s] == 3 || this.kinds[s] == 4) && this.names[s].equals(scopeNames[i])) {
               if (slots == null) {
                  slots = new int[scopeNames.length];
                  Arrays.fill(slots, -1);
               }

               slots[i] = s;
               break;
            }
         }
      }

      return slots;
   }

   static JsErrorException tdzError(String name) {
      return JsErrorException.uninitialized(name);
   }

   static SlotTable forNodeEager(Node fnNode, List<Node> argNodes, Node body) {
      Object cached = fnNode.meta;
      if (cached == null) {
         if (!hasLoop(body)) {
            fnNode.meta = DEFERRED;
            return null;
         } else {
            SlotTable table = analyze(argNodes, body);
            fnNode.meta = table == null ? BAIL : table;
            return table;
         }
      } else {
         return cached != BAIL && cached != DEFERRED ? (SlotTable)cached : null;
      }
   }

   static SlotTable forNodeForced(Node fnNode, List<Node> argNodes, Node body) {
      Object cached = fnNode.meta;
      if (cached != null && cached != DEFERRED) {
         return cached == BAIL ? null : (SlotTable)cached;
      } else {
         SlotTable table = analyze(argNodes, body);
         fnNode.meta = table == null ? BAIL : table;
         return table;
      }
   }

   static SlotTable forProgram(Node program) {
      Object cached = program.meta;
      if (cached != null) {
         return cached == BAIL ? null : (SlotTable)cached;
      } else if (!hasLoop(program)) {
         program.meta = BAIL;
         return null;
      } else {
         SlotTable table = analyze(List.of(), program, true);
         program.meta = table == null ? BAIL : table;
         return table;
      }
   }

   private static boolean hasLoop(Node node) {
      if (node.isToken()) {
         return false;
      } else {
         switch (node.type) {
            case FOR_STMT:
            case WHILE_STMT:
            case DO_WHILE_STMT:
               return true;
            case FN_EXPR:
            case FN_ARROW_EXPR:
            case CLASS_EXPR:
               return false;
            default:
               int i = 0;

               for (int n = node.size(); i < n; i++) {
                  if (hasLoop(node.get(i))) {
                     return true;
                  }
               }

               return false;
         }
      }
   }

   private static SlotTable analyze(List<Node> argNodes, Node body) {
      return analyze(argNodes, body, false);
   }

   private static SlotTable analyze(List<Node> argNodes, Node body, boolean programScope) {
      int paramCount = argNodes.size();
      String[] paramNames = new String[paramCount];

      for (int i = 0; i < paramCount; i++) {
         Node argNode = argNodes.get(i);
         Node first = argNode.getFirst();
         if (first.getFirstToken().type == TokenType.DOT_DOT_DOT) {
            return null;
         }

         if (first.type == NodeType.LIT_ARRAY || first.type == NodeType.LIT_OBJECT) {
            return null;
         }

         String name = first.getText();

         for (int j = 0; j < i; j++) {
            if (name.equals(paramNames[j])) {
               return null;
            }
         }

         paramNames[i] = name;
      }

      SlotTable.Walker w = new SlotTable.Walker();
      w.walk(body, body.type == NodeType.BLOCK ? body : null);

      for (Node argNodex : argNodes) {
         Node defaultExpr = defaultExpr(argNodex);
         if (defaultExpr != null) {
            w.walk(defaultExpr, null);
         }
      }

      Set<String> paramSet = new LinkedHashSet<>(List.of(paramNames));

      for (Entry<String, SlotTable.LetDecl> e : w.letConsts.entrySet()) {
         String name = e.getKey();
         if (w.declCount.getOrDefault(name, 0) != 1 || paramSet.contains(name) || w.vars.contains(name) || w.fnDecls.contains(name)) {
            w.storeForced.add(name);
         }
      }

      w.letConsts.keySet().removeIf(w.storeForced::contains);
      Set<String> functionScoped = new LinkedHashSet<>();
      if (!programScope) {
         for (String p : paramNames) {
            functionScoped.add(p);
         }

         functionScoped.addAll(w.vars);
         functionScoped.addAll(w.fnDecls);
         functionScoped.removeAll(w.storeForced);
      }

      List<String> scopedNames = new ArrayList<>();

      for (Entry<String, SlotTable.LetDecl> ex : w.letConsts.entrySet()) {
         if (!w.storeForced.contains(ex.getKey()) && !w.captured.contains(ex.getKey())) {
            if (!programScope && ex.getValue().scopeNode == body) {
               functionScoped.add(ex.getKey());
            } else {
               scopedNames.add(ex.getKey());
            }
         }
      }

      functionScoped.removeAll(w.captured);
      functionScoped.remove("arguments");
      functionScoped.remove("this");
      if (functionScoped.isEmpty() && scopedNames.isEmpty()) {
         return null;
      } else if (functionScoped.size() + scopedNames.size() > 32767) {
         return null;
      } else {
         int total = functionScoped.size() + scopedNames.size();
         String[] names = new String[total];
         byte[] kinds = new byte[total];
         Map<String, Integer> byName = new HashMap<>(functionScoped.size() * 2);
         Map<String, Integer> allSlots = new HashMap<>(total * 2);
         int idx = 0;

         for (String name : functionScoped) {
            names[idx] = name;
            SlotTable.LetDecl let = w.letConsts.get(name);
            kinds[idx] = (byte)(let != null ? let.kind : (w.fnDecls.contains(name) && !w.vars.contains(name) ? 2 : (w.vars.contains(name) ? 1 : 0)));
            byName.put(name, idx);
            allSlots.put(name, idx);
            idx++;
         }

         for (String name : scopedNames) {
            SlotTable.LetDecl let = w.letConsts.get(name);
            names[idx] = name;
            kinds[idx] = let.kind;
            allSlots.put(name, idx);
            idx++;
         }

         int[] paramSlots = new int[paramCount];

         for (int i = 0; i < paramCount; i++) {
            Integer s = byName.get(paramNames[i]);
            paramSlots[i] = s == null ? -1 : s;
         }

         SlotTable table = new SlotTable(names, kinds, paramSlots, byName);
         if (!byName.isEmpty()) {
            annotate(body, byName);

            for (Node argNodexx : argNodes) {
               Node defaultExpr = defaultExpr(argNodexx);
               if (defaultExpr != null) {
                  annotate(defaultExpr, byName);
               }
            }
         }

         for (String name : scopedNames) {
            SlotTable.LetDecl let = w.letConsts.get(name);
            Integer slot = allSlots.get(name);
            if (let.scopeNode.type == NodeType.SWITCH_STMT) {
               int i = 0;

               for (int n = let.scopeNode.size(); i < n; i++) {
                  Node child = let.scopeNode.get(i);
                  if (child.type == NodeType.CASE_BLOCK || child.type == NodeType.DEFAULT_BLOCK) {
                     annotate(child, Map.of(name, slot));
                  }
               }
            } else {
               annotate(let.scopeNode, Map.of(name, slot));
            }
         }

         for (Entry<String, SlotTable.LetDecl> exx : w.letConsts.entrySet()) {
            Integer slot = allSlots.get(exx.getKey());
            if (slot != null) {
               exx.getValue().bindingNode.slot = slot.shortValue();
            }
         }

         return table;
      }
   }

   private static Node defaultExpr(Node argNode) {
      return argNode.type == NodeType.FN_DECL_ARG
            && argNode.size() >= 3
            && argNode.get(1).isToken()
            && argNode.get(1).token.type == TokenType.EQ
            && argNode.get(2).type == NodeType.EXPR
         ? argNode.get(2)
         : null;
   }

   private static void annotate(Node node, Map<String, Integer> names) {
      if (!node.isToken()) {
         switch (node.type) {
            case FN_EXPR:
            case FN_ARROW_EXPR:
            case CLASS_EXPR:
               break;
            case STATEMENT:
            case BLOCK:
            case VAR_STMT:
            case SWITCH_STMT:
            case TRY_STMT:
            default:
               int i = 0;

               for (int n = node.size(); i < n; i++) {
                  annotate(node.get(i), names);
               }
               break;
            case REF_EXPR:
               Node first = node.getFirst();
               if (first.isToken() && first.token.type == TokenType.IDENT) {
                  Integer idx = names.get(node.getText());
                  if (idx != null) {
                     node.slot = idx.shortValue();
                  }
               } else {
                  annotate(first, names);
               }
         }
      }
   }

   private static final class LetDecl {
      final byte kind;
      final Node scopeNode;
      final Node bindingNode;

      LetDecl(byte kind, Node scopeNode, Node bindingNode) {
         this.kind = kind;
         this.scopeNode = scopeNode;
         this.bindingNode = bindingNode;
      }
   }

   private static final class Walker {
      final Set<String> vars = new LinkedHashSet<>();
      final Set<String> fnDecls = new LinkedHashSet<>();
      final Set<String> storeForced = new LinkedHashSet<>();
      final Set<String> captured = new LinkedHashSet<>();
      final Map<String, SlotTable.LetDecl> letConsts = new LinkedHashMap<>();
      final Map<String, Integer> declCount = new HashMap<>();

      private void count(String name) {
         this.declCount.merge(name, 1, Integer::sum);
      }

      void walk(Node node, Node letScope) {
         if (!node.isToken()) {
            switch (node.type) {
               case FOR_STMT:
                  boolean cStyle = node.size() > 3
                     && node.get(2).type == NodeType.VAR_STMT
                     && node.get(3).isToken()
                     && node.get(3).token.type == TokenType.SEMI;
                  int iF = 0;

                  for (int nxx = node.size(); iF < nxx; iF++) {
                     Node child = node.get(iF);
                     this.walk(child, cStyle && iF == 2 ? node : null);
                  }
                  break;
               case WHILE_STMT:
               case DO_WHILE_STMT:
               default:
                  int iW = 0;

                  for (int n = node.size(); iW < n; iW++) {
                     this.walk(node.get(iW), null);
                  }
                  break;
               case FN_EXPR:
                  if (node.size() > 1 && node.get(1).isToken() && node.get(1).token.type == TokenType.IDENT) {
                     this.fnDecls.add(node.get(1).getText());
                  }

                  this.collectCaptured(node);
                  break;
               case FN_ARROW_EXPR:
               case CLASS_EXPR:
                  this.collectCaptured(node);
                  break;
               case STATEMENT:
                  int iS = 0;

                  for (int nxx = node.size(); iS < nxx; iS++) {
                     this.walk(node.get(iS), letScope);
                  }
                  break;
               case BLOCK:
                  int iB = 0;

                  for (int nxx = node.size(); iB < nxx; iB++) {
                     this.walk(node.get(iB), node);
                  }
                  break;
               case VAR_STMT:
                  boolean isVar = node.getFirstToken().type == TokenType.VAR;
                  byte kind = (byte)(node.getFirstToken().type == TokenType.CONST ? 4 : 3);
                  int iV = 0;

                  for (int nx = node.size(); iV < nx; iV++) {
                     Node child = node.get(iV);
                     if (child.type == NodeType.VAR_DECL) {
                        Node binding = child.getFirst();
                        if (binding.isToken() && binding.token.type == TokenType.IDENT) {
                           String name = binding.getText();
                           this.count(name);
                           if (isVar) {
                              this.vars.add(name);
                           } else if (letScope == null) {
                              this.storeForced.add(name);
                           } else {
                              SlotTable.LetDecl prev = this.letConsts.put(name, new SlotTable.LetDecl(kind, letScope, binding));
                              if (prev != null) {
                                 this.storeForced.add(name);
                              }
                           }
                        } else {
                           for (Node ident : binding.findChildren(TokenType.IDENT)) {
                              this.storeForced.add(ident.getText());
                              this.count(ident.getText());
                           }
                        }

                        int j = 1;

                        for (int m = child.size(); j < m; j++) {
                           this.walk(child.get(j), null);
                        }
                     }
                  }
                  break;
               case SWITCH_STMT:
                  int iX = 0;

                  for (int n = node.size(); iX < n; iX++) {
                     Node child = node.get(iX);
                     if (child.type != NodeType.CASE_BLOCK && child.type != NodeType.DEFAULT_BLOCK) {
                        this.walk(child, null);
                     } else {
                        int j = 0;

                        for (int m = child.size(); j < m; j++) {
                           this.walk(child.get(j), node);
                        }
                     }
                  }
                  break;
               case TRY_STMT:
                  if (node.size() > 4
                     && node.get(2).isToken()
                     && node.get(2).token.type == TokenType.CATCH
                     && node.get(3).isToken()
                     && node.get(3).token.type == TokenType.L_PAREN) {
                     Node param = node.get(4);
                     if (param.isToken() && param.token.type == TokenType.IDENT) {
                        this.storeForced.add(param.getText());
                        this.count(param.getText());
                     } else {
                        for (Node ident : param.findChildren(TokenType.IDENT)) {
                           this.storeForced.add(ident.getText());
                           this.count(ident.getText());
                        }
                     }
                  }

                  int iT = 0;

                  for (int n = node.size(); iT < n; iT++) {
                     this.walk(node.get(iT), null);
                  }
            }
         }
      }

      private void collectCaptured(Node node) {
         if (node.isToken()) {
            if (node.token.type == TokenType.IDENT) {
               this.captured.add(node.getText());
            }
         } else {
            int i = 0;

            for (int n = node.size(); i < n; i++) {
               this.collectCaptured(node.get(i));
            }
         }
      }
   }
}
