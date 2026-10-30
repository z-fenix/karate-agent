package io.karatelabs.ext.rules;

import io.karatelabs.js.ContextListener;
import io.karatelabs.js.ContextScope;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.NodeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Calc implements ContextListener, SimpleObject {
   static final Logger logger = LoggerFactory.getLogger(Calc.class);
   private final Object input;
   Object output;
   private final ArrayDeque<ExecutionTraceNode> stack = new ArrayDeque<>();
   ExecutionTraceNode root;
   private final List<Object[]> declaredLevels = new ArrayList<>();
   private final Map<Integer, String> labels = new LinkedHashMap<>();
   private Object outcome;
   private boolean outcomeSet;
   private final List<Object[]> properties = new ArrayList<>();
   private final Set<String> reqs = new LinkedHashSet<>();

   public Calc(Object var1) {
      this.input = var1;
   }

   public Object getInput() {
      return this.input;
   }

   public ExecutionTraceNode getRoot() {
      return this.root;
   }

   public Object getOutput() {
      return this.output;
   }

   public List<Object[]> getDeclaredLevels() {
      return this.declaredLevels;
   }

   public Map<Integer, String> getLabels() {
      return this.labels;
   }

   public Object getOutcome() {
      return this.outcome;
   }

   public boolean isOutcomeSet() {
      return this.outcomeSet;
   }

   public List<Object[]> getProperties() {
      return this.properties;
   }

   public Set<String> getReqs() {
      return this.reqs;
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "log" -> this.log();
         case "input" -> this.input;
         case "output" -> this.output;
         case "levels" -> this.levels();
         case "label" -> this.label();
         case "outcome" -> this.outcomeFn();
         case "always" -> this.property("always");
         case "sometimes" -> this.property("sometimes");
         case "req" -> this.req();
         case "round" -> this.round();
         default -> null;
      };
   }

   private JavaCallable round() {
      return (var0, var1) -> {
         if (var1.length != 0 && var1[0] instanceof Number var2) {
            double var8 = var2.doubleValue();
            if (!Double.isNaN(var8) && !Double.isInfinite(var8)) {
               int var5 = var1.length >= 2 && var1[1] instanceof Number var6 ? var6.intValue() : 2;
               if (var5 < 0 || var5 > 12) {
                  var5 = 2;
               }

               return RuleInputSpaceExplorer.a(BigDecimal.valueOf(var8).setScale(var5, RoundingMode.HALF_UP).doubleValue());
            } else {
               return var8;
            }
         } else {
            return var1.length == 0 ? null : var1[0];
         }
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      if (var1.equals("output")) {
         this.output = var2;
      } else {
         logger.warn("put not supported for: {}", var1);
      }
   }

   private JavaCallable log() {
      return (var1, var2) -> {
         ExecutionTraceNode var3 = this.stack.peek();
         if (var3 == null) {
            return null;
         } else {
            short var4 = var1.getNode().getFirstToken().line;
            String var5 = var1.getPath() + ":" + var4;
            String var6 = var2.length == 0 ? "" : var2[0] + "";
            logger.debug("{}", var6);
            ExecutionTraceNode var7 = new ExecutionTraceNode(var5, var4, var3, false, var6);
            if (var2.length >= 2 && var2[1] instanceof Map var8 && var8.get("step") != null) {
               var7.h = String.valueOf(var8.get("step"));
               var7.i = var8.get("amount") instanceof Number var11 ? var11.doubleValue() : null;
            }

            return null;
         }
      };
   }

   private JavaCallable levels() {
      return (var1, var2) -> {
         if (var2.length >= 2 && var2[0] != null) {
            ArrayList<java.lang.Object> var3 = new ArrayList();
            if (var2[1] instanceof List var4) {
               var3.addAll(var4);
            } else if (var2[1] != null) {
               var3.add(var2[1]);
            }

            this.declaredLevels.add(new Object[]{String.valueOf(var2[0]), var3});
         }

         return null;
      };
   }

   private JavaCallable label() {
      return (var1, var2) -> {
         if (var2.length >= 1 && var2[0] != null && var1 != null) {
            int var3 = var1.getNode().getFirstToken().line + 1;
            this.labels.put(var3, String.valueOf(var2[0]));
         }

         return null;
      };
   }

   private JavaCallable outcomeFn() {
      return (var1, var2) -> {
         this.outcome = var2.length == 0 ? null : var2[0];
         this.outcomeSet = true;
         return this.outcome;
      };
   }

   private JavaCallable property(String var1) {
      return (var2, var3) -> {
         String var4 = var3.length >= 1 && var3[0] != null ? String.valueOf(var3[0]) : var1;
         boolean var5 = var3.length >= 2 && Terms.isTruthy(var3[1]);
         int var6 = var2 == null ? 0 : var2.getNode().getFirstToken().line + 1;
         String var7 = var3.length >= 3 && var3[2] instanceof Map var8 && var8.get("req") != null ? String.valueOf(var8.get("req")) : null;
         this.properties.add(new Object[]{var4, var6, var5, var1, var7});
         return var5;
      };
   }

   private JavaCallable req() {
      return (var1, var2) -> {
         if (var2.length >= 1 && var2[0] != null) {
            this.reqs.add(String.valueOf(var2[0]));
         }

         return null;
      };
   }

   @Override
   public void onEvent(Event var1) {
      if (var1.context.getScope() == ContextScope.FUNCTION) {
         if (var1.node.type == NodeType.BLOCK) {
            if (var1.type == EventType.CONTEXT_ENTER) {
               int var2 = var1.context.getParent() == null ? -1 : var1.context.getParent().getIteration();
               short var3 = var1.node.getFirstToken().line;
               String var4 = var1.context.getPath() + ":" + var3 + ":context";
               String var5 = var2 == -1 ? "" : "### Iteration: " + (var2 + 1);
               ExecutionTraceNode var6 = new ExecutionTraceNode(var4, var3, this.stack.peek(), true, var5);
               if (this.root == null) {
                  this.root = var6;
               }

               this.stack.push(var6);
            } else if (var1.type == EventType.CONTEXT_EXIT) {
               this.stack.pop();
            }
         }
      }
   }
}
