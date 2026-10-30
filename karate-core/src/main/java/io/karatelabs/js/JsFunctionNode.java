package io.karatelabs.js;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsFunctionNode extends JsFunction {
   static final Logger logger = LoggerFactory.getLogger(JsFunctionNode.class);
   final boolean arrow;
   final boolean async;
   final boolean generator;
   final Node node;
   final Node body;
   final List<Node> argNodes;
   final int argCount;
   final CoreContext declaredContext;
   final PrivateEnv privateEnv;
   final BindingsStore capturedBindings;
   final boolean strict;
   boolean isClassConstructor;
   boolean isDerivedConstructor;
   boolean isDefaultDerivedConstructor;
   ObjectLike homeObject;
   JsCallable baseBuiltinCtor;
   List<JsFunctionNode.FieldInit> instanceFields;
   List<PrivateName> privateBrands;
   SlotTable slotTable;
   private int callCount;

   public JsFunctionNode(boolean arrow, Node node, List<Node> argNodes, Node body, CoreContext declaredContext) {
      this(arrow, node, argNodes, body, declaredContext, false);
   }

   JsFunctionNode(boolean arrow, Node node, List<Node> argNodes, Node body, CoreContext declaredContext, boolean forceStrict) {
      this.arrow = arrow;
      this.async = node.async;
      this.generator = node.generator;
      this.node = node;
      this.argNodes = argNodes;
      this.argCount = argNodes.size();
      this.body = body;
      this.declaredContext = declaredContext;
      this.privateEnv = declaredContext == null ? null : declaredContext.privateEnv;
      this.strict = forceStrict || declaredContext != null && declaredContext.strict || body.type == NodeType.BLOCK && Interpreter.hasUseStrictDirective(body);
      this.length = expectedArgCount(argNodes);
      this.capturedBindings = captureBindings(declaredContext);
      this.slotTable = SlotTable.ENABLED ? SlotTable.forNodeEager(node, argNodes, body) : null;
   }

   private static int expectedArgCount(List<Node> argNodes) {
      int count = 0;

      for (Node argNode : argNodes) {
         if (argNode.getFirst().getFirstToken().type == TokenType.DOT_DOT_DOT
            || argNode.type == NodeType.FN_DECL_ARG && argNode.size() >= 3 && argNode.get(1).isToken() && argNode.get(1).token.type == TokenType.EQ) {
            break;
         }

         count++;
      }

      return count;
   }

   private static BindingsStore captureBindings(CoreContext context) {
      if (context.bindings == null) {
         return null;
      } else {
         Map<String, BindingSlot> snapshot = null;

         for (String key : context.bindings.keys()) {
            BindingSlot s = context.bindings.getSlot(key);
            if (s != null && s.scope != null) {
               if (snapshot == null) {
                  snapshot = new HashMap<>(4);
               }

               snapshot.put(key, s);
            }
         }

         return snapshot == null ? null : BindingsStore.captured(snapshot);
      }
   }

   @Override
   public Object call(Context callerContext, Object[] args) {
      CoreContext parentContext;
      boolean hostCall;
      if (callerContext instanceof CoreContext cc) {
         parentContext = cc;
         hostCall = false;
      } else {
         parentContext = this.declaredContext;
         hostCall = true;
      }

      CoreContext functionContext = new CoreContext(parentContext, this.node, args, this.declaredContext, this.capturedBindings);
      functionContext.strict = this.strict;
      functionContext.activeFunction = this.arrow ? (this.declaredContext != null ? this.declaredContext.activeFunction : null) : this;
      if (this.arrow) {
         Interpreter.bindArrowThis(functionContext, this);
      }

      Engine declaringEngine = this.declaredContext == null ? null : this.declaredContext.getEngine();
      if (declaringEngine != null && declaringEngine != Engine.current()) {
         Engine prevEngine = Engine.enter(declaringEngine);

         Object var9;
         try {
            Object result = this.bindArgsAndExecute(functionContext, parentContext, args);
            var9 = hostCall ? this.hostResult(functionContext, parentContext, result) : result;
         } finally {
            Engine.exit(prevEngine);
         }

         return var9;
      } else {
         Object result = this.bindArgsAndExecute(functionContext, parentContext, args);
         return hostCall ? this.hostResult(functionContext, parentContext, result) : result;
      }
   }

   private Object hostResult(CoreContext functionContext, CoreContext parentContext, Object result) {
      if (this.async) {
         return result;
      } else if (functionContext.isError()) {
         EngineException ex = Interpreter.errorAsException(functionContext, this.node);
         if (parentContext != null) {
            parentContext.reset();
         }

         throw ex;
      } else {
         return result;
      }
   }

   Object bindArgsAndExecute(CoreContext functionContext, CoreContext parentContext, Object[] args) {
      functionContext.privateEnv = this.privateEnv;
      if (this.generator) {
         Engine engine = functionContext.getEngine();
         return new JsGenerator(engine, this, functionContext, args);
      } else if (this.async) {
         return AsyncSupport.callAsync(this, functionContext, args);
      } else {
         try {
            return this.executeBody(functionContext, parentContext, args);
         } catch (StackOverflowError var5) {
            throw JsErrorException.rangeError("Maximum call stack size exceeded");
         }
      }
   }

   Object executeBody(CoreContext functionContext, CoreContext parentContext, Object[] args) {
      SlotTable table = this.slotTable;
      if (table == null && SlotTable.ENABLED && ++this.callCount == 2) {
         table = SlotTable.forNodeForced(this.node, this.argNodes, this.body);
         this.slotTable = table;
      }

      if (table != null) {
         functionContext.frame = table.newFrame();
         functionContext.frameTable = table;
      }

      for (int i = 0; i < this.argCount; i++) {
         Node argNode = this.argNodes.get(i);
         Node first = argNode.getFirst();
         if (first.getFirstToken().type == TokenType.DOT_DOT_DOT) {
            List<Object> remainingArgs = new ArrayList<>();

            for (int j = i; j < args.length; j++) {
               remainingArgs.add(args[j]);
            }

            Node restTarget = argNode.get(1);
            if (restTarget.type != NodeType.LIT_ARRAY && restTarget.type != NodeType.LIT_OBJECT) {
               functionContext.put(restTarget.getText(), remainingArgs);
            } else {
               Interpreter.evalAssign(restTarget, functionContext, BindScope.VAR, new JsArray(remainingArgs), true);
            }
         } else {
            Object argValue = i < args.length ? args[i] : Terms.UNDEFINED;
            if (argValue == Terms.UNDEFINED
               && argNode.type == NodeType.FN_DECL_ARG
               && argNode.size() >= 3
               && argNode.get(1).isToken()
               && argNode.get(1).token.type == TokenType.EQ
               && argNode.get(2).type == NodeType.EXPR) {
               argValue = Interpreter.eval(argNode.get(2), functionContext);
            }

            if (first.type != NodeType.LIT_ARRAY && first.type != NodeType.LIT_OBJECT) {
               String argName = first.getText();
               int slot = table == null ? -1 : table.paramSlots[i];
               if (slot < 0) {
                  functionContext.put(argName, argValue);
               } else {
                  if (argValue instanceof JsFunction fn && (fn.name == null || fn.name.isEmpty())) {
                     fn.name = argName;
                  }

                  functionContext.frame[slot] = argValue;
               }
            } else {
               Interpreter.evalAssign(first, functionContext, BindScope.VAR, argValue, true);
            }
         }
      }

      Object result = Interpreter.eval(this.body, functionContext);
      if (functionContext.isError()) {
         parentContext.updateFrom(functionContext);
      }

      if (this.body.type != NodeType.BLOCK) {
         return result;
      } else {
         return functionContext.getExitType() == null ? Terms.UNDEFINED : functionContext.getReturnValue();
      }
   }

   @Override
   public String getSource() {
      return this.node.getTextIncludingWhitespace();
   }

   @Override
   public boolean isConstructable() {
      return !this.arrow && !this.async && !this.generator;
   }

   static {
      JsErrorException.rangeError("");
   }

   static final class FieldInit {
      final String key;
      final PrivateName privateName;
      final Node initializer;

      FieldInit(String key, PrivateName privateName, Node initializer) {
         this.key = key;
         this.privateName = privateName;
         this.initializer = initializer;
      }
   }
}
