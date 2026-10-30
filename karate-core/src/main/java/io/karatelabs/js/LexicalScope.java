package io.karatelabs.js;

import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.List;

final class LexicalScope {
   static final LexicalScope NONE = new LexicalScope(new String[0], new BindScope[0]);
   final String[] names;
   final BindScope[] scopes;
   private volatile LexicalScope.FrameSlots frameSlots;

   private LexicalScope(String[] names, BindScope[] scopes) {
      this.names = names;
      this.scopes = scopes;
   }

   static LexicalScope of(Node node) {
      if (node.meta instanceof LexicalScope ls) {
         return ls;
      } else {
         LexicalScope ls = compute(node);
         node.meta = ls;
         return ls;
      }
   }

   static LexicalScope compute(Node node) {
      List<String> names = new ArrayList<>(4);
      List<Boolean> consts = new ArrayList<>(4);
      switch (node.type) {
         case BLOCK:
         case PROGRAM:
            JsParser.lexicalDeclarations(node, names, consts);
            break;
         case SWITCH_STMT:
            int i = 0;

            for (int nx = node.size(); i < nx; i++) {
               Node child = node.get(i);
               if (child.type == NodeType.CASE_BLOCK || child.type == NodeType.DEFAULT_BLOCK) {
                  JsParser.lexicalDeclarations(child, names, consts);
               }
            }
            break;
         case FOR_STMT:
            Node head = node.size() > 2 ? node.get(2) : null;
            if (head != null && head.type == NodeType.VAR_STMT && head.getFirstToken().type != TokenType.VAR) {
               boolean isConst = head.getFirstToken().type == TokenType.CONST;
               int iF = 0;

               for (int n = head.size(); iF < n; iF++) {
                  Node declarator = head.get(iF);
                  if (declarator.type == NodeType.VAR_DECL) {
                     int before = names.size();
                     JsParser.boundNames(declarator, names);

                     for (int j = before; j < names.size(); j++) {
                        consts.add(isConst);
                     }
                  }
               }
            }
      }

      if (names.isEmpty()) {
         return NONE;
      } else {
         BindScope[] scopes = new BindScope[names.size()];

         for (int i = 0; i < scopes.length; i++) {
            scopes[i] = consts.get(i) ? BindScope.CONST : BindScope.LET;
         }

         return new LexicalScope(names.toArray(new String[0]), scopes);
      }
   }

   void instantiate(CoreContext context) {
      Object[] frame = context.frame;
      int[] slots = frame == null ? null : this.frameSlots(context.frameTable);

      for (int i = 0; i < this.names.length; i++) {
         int s = slots == null ? -1 : slots[i];
         if (s >= 0) {
            frame[s] = SlotTable.TDZ;
         } else {
            context.declareUninitialized(this.names[i], this.scopes[i]);
         }
      }
   }

   int[] frameSlots(SlotTable table) {
      LexicalScope.FrameSlots fs = this.frameSlots;
      if (fs == null || fs.table != table) {
         fs = new LexicalScope.FrameSlots(table, table.lexicalSlots(this.names));
         this.frameSlots = fs;
      }

      return fs.slots;
   }

   void instantiateGlobal(CoreContext context) {
      for (int i = 0; i < this.names.length; i++) {
         BindingSlot existing = context.bindings == null ? null : context.bindings.getSlot(this.names[i]);
         if (existing == null || existing.scope == null || existing.level != context.currentLevel) {
            context.declareUninitialized(this.names[i], this.scopes[i]);
         }
      }
   }

   private record FrameSlots(SlotTable table, int[] slots) {
   }
}
