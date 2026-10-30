package io.karatelabs.js;

import io.karatelabs.parser.JsLexer;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Interpreter {
   static final Logger logger = LoggerFactory.getLogger(Interpreter.class);
   private static final Object[] EMPTY_ARGS = new Object[0];

   static Object bindThisForCall(Object receiver, CoreContext context, boolean strict) {
      if (receiver != null && receiver != Terms.UNDEFINED) {
         return receiver;
      } else {
         return strict ? Terms.UNDEFINED : context.root.thisObject;
      }
   }

   static void bindArrowThis(CoreContext callContext, JsFunctionNode arrowFn) {
      callContext.arrowFrame = true;
      if (arrowFn.declaredContext != null) {
         callContext.thisObject = arrowFn.declaredContext.thisObject;
      }
   }

   static boolean hasUseStrictDirective(Node body) {
      int i = 0;

      for (int n = body.size(); i < n; i++) {
         Node child = body.get(i);
         if (!child.isToken()) {
            if (child.type != NodeType.STATEMENT) {
               return false;
            }

            Token strTok = directiveStringToken(child);
            if (strTok == null) {
               return false;
            }

            String raw = strTok.getText();
            if ("\"use strict\"".equals(raw) || "'use strict'".equals(raw)) {
               return true;
            }
         }
      }

      return false;
   }

   private static Token directiveStringToken(Node stmt) {
      if (stmt.size() == 0) {
         return null;
      } else {
         Node exprList = stmt.getFirst();
         if (exprList.type == NodeType.EXPR_LIST && exprList.size() == 1) {
            Node expr = exprList.getFirst();
            if (expr.type == NodeType.EXPR && expr.size() == 1) {
               Node lit = expr.getFirst();
               if (lit.type == NodeType.LIT_EXPR && lit.size() == 1) {
                  Node tok = lit.getFirst();
                  if (!tok.isToken()) {
                     return null;
                  } else {
                     TokenType tt = tok.token.type;
                     return tt != TokenType.S_STRING && tt != TokenType.D_STRING ? null : tok.token;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   static void checkInterrupted() {
      if (Thread.currentThread().isInterrupted()) {
         throw new EngineInterruptedException();
      } else {
         if (AsyncActivation.anyAsync) {
            AsyncActivation activation = AsyncActivation.current();
            if (activation != null && !activation.scope.isOpen()) {
               throw new EngineInterruptedException();
            }
         }
      }
   }

   private static List<Node> fnArgs(Node fnArgs) {
      List<Node> list = new ArrayList<>(Math.max(1, fnArgs.size() - 2));
      int i = 0;

      for (int n = fnArgs.size(); i < n; i++) {
         Node fnArg = fnArgs.get(i);
         if (fnArg.type == NodeType.FN_DECL_ARG) {
            list.add(fnArg);
         }
      }

      return list;
   }

   private static Object lhsOperand(Node node, CoreContext context) {
      return eval(node.get(0), context);
   }

   private static Object rhsOperand(Node node, CoreContext context) {
      return context.isError() ? Terms.UNDEFINED : eval(node.get(2), context);
   }

   static Object evalAssign(Node bindings, CoreContext context, BindScope bindScope, Object value, boolean initialized) {
      if (bindings.type != NodeType.LIT_ARRAY && bindings.type != NodeType.LIT_OBJECT) {
         if (bindScope != null || bindings.type != NodeType.REF_DOT_EXPR && bindings.type != NodeType.REF_BRACKET_EXPR) {
            if (bindings.isToken() && bindings.token.type == TokenType.IDENT) {
               String name = bindings.getText();
               declareBinding(bindings, name, context, bindScope, value, initialized);
               if (context.root.listener != null) {
                  context.root.listener.onBind(BindEvent.declare(name, value, bindScope, context, bindings));
               }
            } else {
               for (Node varName : bindings.findChildren(TokenType.IDENT)) {
                  String name = varName.getText();
                  context.declare(name, value, toScope(bindScope), initialized);
                  if (context.root.listener != null) {
                     context.root.listener.onBind(BindEvent.declare(name, value, bindScope, context, bindings));
                  }
               }
            }
         } else {
            PropertyAccess.set(bindings, context, value);
         }
      } else {
         destructurePattern(bindings, context, bindScope, value, initialized);
      }

      return value;
   }

   private static Object evalAssignExpr(Node node, CoreContext context) {
      Node lhs = node.get(0);
      TokenType operator = node.get(1).token.type;
      if (operator == TokenType.PIPE_PIPE_EQ || operator == TokenType.AMP_AMP_EQ || operator == TokenType.QUES_QUES_EQ) {
         return PropertyAccess.logicalCompound(lhs, context, operator, node.get(2), node);
      } else if (operator != TokenType.EQ) {
         return PropertyAccess.compound(lhs, context, operator, node.get(2), node);
      } else if (lhs.type == NodeType.LIT_EXPR) {
         Object value = eval(node.get(2), context);
         if (context.isStopped()) {
            return value;
         } else {
            Node pattern = lhs.getFirst();
            if (pattern.type != NodeType.LIT_ARRAY && pattern.type != NodeType.LIT_OBJECT) {
               evalAssign(pattern, context, BindScope.VAR, value, true);
            } else {
               destructurePattern(pattern, context, null, value, false);
            }

            return value;
         }
      } else {
         return PropertyAccess.assign(lhs, context, node.get(2), node);
      }
   }

   private static void destructurePattern(Node pattern, CoreContext context, BindScope bindScope, Object source, boolean initialized) {
      if (source != null && source != Terms.UNDEFINED) {
         if (pattern.type == NodeType.LIT_ARRAY) {
            JsIterator iter = IterUtils.getIterator(source, context);
            int last = pattern.size() - 1;
            boolean abrupt = false;

            try {
               for (int i = 1; i < last; i++) {
                  Node elem = pattern.get(i);
                  Node first = elem.get(0);
                  if (first.isToken() && first.token.type == TokenType.DOT_DOT_DOT) {
                     JsArray rest = new JsArray();

                     while (iter.hasNext()) {
                        rest.list.add(iter.next());
                     }

                     bindTarget(elem.get(1), context, bindScope, rest, initialized);
                     break;
                  }

                  if (!first.isToken() || first.token.type != TokenType.COMMA) {
                     Object v = Terms.UNDEFINED;
                     if (iter.hasNext()) {
                        Object temp = iter.next();
                        if (temp != Terms.UNDEFINED) {
                           v = temp;
                        }
                     }

                     bindTarget(first, context, bindScope, v, initialized);
                  } else if (iter.hasNext()) {
                     iter.next();
                  }

                  if (context.isError()) {
                     abrupt = true;
                     break;
                  }
               }
            } catch (RuntimeException var19) {
               iter.close(context, true);
               throw var19;
            }

            iter.close(context, abrupt);
         } else if (pattern.type == NodeType.LIT_OBJECT) {
            ObjectLike objSource = source instanceof ObjectLike ? (ObjectLike)source : null;
            Map<String, Object> map = source instanceof Map ? (Map)source : null;
            Set<String> consumed = new HashSet<>();
            int last = pattern.size() - 1;

            for (int i = 1; i < last; i++) {
               Node elemx = pattern.get(i);
               int sz = elemx.size();
               if (sz > 0 && elemx.get(sz - 1).isToken() && elemx.get(sz - 1).token.type == TokenType.COMMA) {
                  sz--;
               }

               Node keyNode = elemx.getFirst();
               TokenType keyType = keyNode.token.type;
               if (keyType == TokenType.DOT_DOT_DOT) {
                  JsObject rest = new JsObject();
                  Terms.copyDataProperties(rest, source, consumed, context);
                  if (context.isError()) {
                     return;
                  }

                  bindTarget(elemx.get(1), context, bindScope, rest, initialized);
                  break;
               }

               boolean computed = keyType == TokenType.L_BRACKET;
               int afterKeyPos = computed ? 3 : 1;
               String key;
               if (computed) {
                  Object keyValue = evalExpr(elemx.get(1), context);
                  if (context.isError()) {
                     return;
                  }

                  key = Terms.toStringCoerce(keyValue, context);
               } else if (keyType != TokenType.S_STRING && keyType != TokenType.D_STRING) {
                  key = keyNode.getText();
               } else {
                  key = (String)Terms.literalValue(keyNode.token);
               }

               consumed.add(key);
               Object v = Terms.UNDEFINED;
               if (objSource != null) {
                  Object temp = objSource.getMember(key, objSource, context);
                  if (context.isError()) {
                     return;
                  }

                  if (temp != Terms.UNDEFINED) {
                     if (temp == null) {
                        if (map != null && map.containsKey(key)) {
                           v = null;
                        }
                     } else {
                        v = temp;
                     }
                  }
               } else if (map != null && map.containsKey(key)) {
                  Object tempx = map.get(key);
                  if (tempx != Terms.UNDEFINED) {
                     v = tempx;
                  }
               }

               if (!computed && sz < 3) {
                  bindLeaf(keyNode, key, context, bindScope, v, initialized);
               } else if (!computed && sz == 3 && elemx.get(1).token.type == TokenType.EQ) {
                  if (v == Terms.UNDEFINED) {
                     v = evalExpr(elemx.get(2), context);
                  }

                  bindLeaf(keyNode, key, context, bindScope, v, initialized);
               } else {
                  bindTarget(elemx.get(afterKeyPos + 1), context, bindScope, v, initialized);
               }

               if (context.isError()) {
                  return;
               }
            }
         }
      } else {
         throw JsErrorException.typeError("cannot destructure " + source);
      }
   }

   private static void bindTarget(Node target, CoreContext context, BindScope bindScope, Object value, boolean initialized) {
      Node node = target;
      if (target.type == NodeType.EXPR) {
         node = target.getFirst();
      }

      if (node.type == NodeType.ASSIGN_EXPR) {
         Node defaultNode = node.getLast();
         node = node.getFirst();
         if (value == Terms.UNDEFINED) {
            value = evalExpr(defaultNode, context);
         }
      }

      Node inner = node;
      if (node.type == NodeType.LIT_EXPR) {
         inner = node.getFirst();
      }

      if (inner.type == NodeType.LIT_ARRAY || inner.type == NodeType.LIT_OBJECT) {
         destructurePattern(inner, context, bindScope, value, initialized);
      } else if (node.type == NodeType.REF_EXPR) {
         bindLeaf(node, node.getText(), context, bindScope, value, initialized);
      } else if (node.type == NodeType.REF_DOT_EXPR || node.type == NodeType.REF_BRACKET_EXPR) {
         PropertyAccess.set(node, context, value, null);
      } else if (node.isToken() && node.token.type == TokenType.IDENT) {
         bindLeaf(node, node.getText(), context, bindScope, value, initialized);
      }
   }

   private static void bindLeaf(Node node, String name, CoreContext context, BindScope bindScope, Object value, boolean initialized) {
      if (bindScope != null) {
         declareBinding(node, name, context, bindScope, value, initialized);
         if (context.root.listener != null) {
            context.root.listener.onBind(BindEvent.declare(name, value, bindScope, context, node));
         }
      } else {
         context.update(name, value, node);
      }
   }

   private static void declareBinding(Node node, String name, CoreContext context, BindScope bindScope, Object value, boolean initialized) {
      int slot = node.slot;
      if (slot >= 0 && context.frame != null && bindScope != null) {
         if (value instanceof JsFunction fn && (fn.name == null || fn.name.isEmpty())) {
            fn.name = name;
         }

         context.frame[slot] = initialized ? value : SlotTable.TDZ;
      } else {
         context.declare(name, value, toScope(bindScope), initialized);
      }
   }

   private static Object evalBlock(Node node, CoreContext context) {
      context.enterScope(ContextScope.BLOCK, node);
      context.event(EventType.CONTEXT_ENTER, node);
      instantiateLexicalScope(node, context);
      hoistFunctionDeclarations(node, context);
      Object blockResult = null;

      try {
         int i = 0;

         for (int n = node.size(); i < n; i++) {
            Node child = node.get(i);
            if (child.type == NodeType.STATEMENT && !isFunctionDeclarationStatement(child)) {
               blockResult = eval(child, context);
               if (context.isStopped()) {
                  break;
               }
            }
         }
      } finally {
         context.event(EventType.CONTEXT_EXIT, node);
         context.exitScope();
      }

      return context.isStopped() ? context.getReturnValue() : blockResult;
   }

   private static Object evalBreakStmt(Node node, CoreContext context) {
      return context.stopAndBreak(node.size() > 1 ? node.get(1).getText() : null);
   }

   private static Object evalContinueStmt(Node node, CoreContext context) {
      return context.stopAndContinue(node.size() > 1 ? node.get(1).getText() : null);
   }

   private static Object evalLabelledStmt(Node node, CoreContext context) {
      Object result = eval(node.get(2), context);
      if (context.isBreaking() && node.getFirst().getText().equals(context.getExitLabel())) {
         context.reset();
      }

      return result;
   }

   private static boolean targetsStatement(CoreContext context, Node node) {
      String label = context.getExitLabel();
      return label == null || hasLabel(node, label);
   }

   private static boolean hasLabel(Node node, String label) {
      Node parent = node.getParent();

      while (parent != null && parent.type == NodeType.STATEMENT) {
         parent = parent.getParent();
         if (parent == null || parent.type != NodeType.LABELLED_STMT) {
            return false;
         }

         if (label.equals(parent.getFirst().getText())) {
            return true;
         }

         parent = parent.getParent();
      }

      return false;
   }

   private static Object evalDeleteExpr(Node node, CoreContext context) {
      return PropertyAccess.delete(node.get(1).getFirst(), context);
   }

   private static Object evalExpr(Node node, CoreContext context) {
      node = node.getFirst();
      context.event(EventType.EXPRESSION_ENTER, node);

      try {
         Object result = eval(node, context);
         if (context.root.listener != null) {
            context.event(EventType.EXPRESSION_EXIT, node);
         }

         return result;
      } catch (Exception var5) {
         if (context.root.listener != null) {
            Event event = new Event(EventType.EXPRESSION_EXIT, context, node);
            ExitResult exitResult = context.root.listener.onError(event, var5);
            if (exitResult != null) {
               Objects.requireNonNull(exitResult);
               return exitResult.returnValue;
            }
         }

         throw var5;
      }
   }

   private static Object evalExprList(Node node, CoreContext context) {
      Object result = null;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (child.type == NodeType.EXPR) {
            result = eval(child, context);
         }
      }

      return result;
   }

   private static Object chainStepResult(Object result, Node node) {
      return result == PropertyAccess.SHORT_CIRCUITED && PropertyAccess.isChainRoot(node) ? Terms.UNDEFINED : result;
   }

   private static Object evalFnCall(Node node, CoreContext context, boolean newKeyword) {
      Node fnArgsNode;
      if (newKeyword) {
         node = node.getFirst();
         if (node.size() == 1) {
            fnArgsNode = null;
         } else {
            fnArgsNode = node.get(2);
            node = node.getFirst();
         }
      } else {
         fnArgsNode = node.get(2);
         node = node.getFirst();
      }

      if (node.type == NodeType.SUPER_EXPR) {
         return evalSuperCall(fnArgsNode, context);
      } else {
         Object o = PropertyAccess.getCallable(node, context);
         Object receiver = context.callReceiver;
         if (o == PropertyAccess.SHORT_CIRCUITED) {
            return PropertyAccess.SHORT_CIRCUITED;
         } else {
            if (newKeyword && context.root.bridge != null && o instanceof ExternalAccess ea && ea.getJavaValue() instanceof Class) {
               o = PropertyAccess.externalConstructor(ea);
            }

            return invokeCallable(o, receiver, fnArgsNode, newKeyword, node, context);
         }
      }
   }

   private static Object evalOptionalCall(Node node, CoreContext context) {
      Object o = PropertyAccess.getCallable(node.getFirst(), context);
      Object receiver = context.callReceiver;
      if (o == PropertyAccess.SHORT_CIRCUITED) {
         return PropertyAccess.SHORT_CIRCUITED;
      } else if (o != null && o != Terms.UNDEFINED) {
         Node callExpr = node.get(1);
         Node fnArgsNode = callExpr.get(2);
         return invokeCallable(o, receiver, fnArgsNode, false, node.getFirst(), context);
      } else {
         return PropertyAccess.SHORT_CIRCUITED;
      }
   }

   private static Object invokeCallable(Object o, Object receiver, Node fnArgsNode, boolean newKeyword, Node node, CoreContext context) {
      if (o instanceof JsCallable callable) {
         if (newKeyword && !callable.isConstructable()) {
            throw JsErrorException.typeError(node.getTextIncludingWhitespace() + " is not a constructor");
         } else if (!newKeyword && callable instanceof JsFunctionNode cf && cf.isClassConstructor) {
            String n = cf.name != null && !cf.name.isEmpty() ? cf.name + " " : "";
            throw JsErrorException.typeError("Class constructor " + n + "cannot be invoked without 'new'");
         } else {
            Object[] args = evalCallArgs(fnArgsNode, context);
            if (context.isError()) {
               return Terms.UNDEFINED;
            } else {
               if (callable.isExternal()) {
                  marshalExternalArgs(args);
               }

               ObjectLike newInstance = null;
               Object result;
               if (callable instanceof JsFunctionNode jsFunc) {
                  CoreContext callContext = new CoreContext(context, node, args, jsFunc.declaredContext, jsFunc.capturedBindings);
                  callContext.strict = jsFunc.strict;
                  callContext.privateEnv = jsFunc.privateEnv;
                  callContext.activeFunction = jsFunc.arrow ? (jsFunc.declaredContext != null ? jsFunc.declaredContext.activeFunction : null) : jsFunc;
                  if (newKeyword) {
                     callContext.callInfo = new CallInfo(true, callable);
                     newInstance = allocateInstance(jsFunc);
                     if (jsFunc.getMember("prototype") instanceof ObjectLike protoObj) {
                        setInstancePrototype(newInstance, protoObj);
                     }

                     callContext.thisObject = newInstance;
                     if (jsFunc.isDefaultDerivedConstructor) {
                        runSuperConstructor(jsFunc, newInstance, args, callContext);
                        runInstanceFieldInitializers(jsFunc, newInstance, callContext);
                     } else if (!jsFunc.isDerivedConstructor) {
                        runInstanceFieldInitializers(jsFunc, newInstance, callContext);
                     }
                  } else if (!jsFunc.arrow) {
                     callContext.thisObject = bindThisForCall(receiver, context, jsFunc.strict);
                  } else {
                     bindArrowThis(callContext, jsFunc);
                  }

                  callContext.event(EventType.CONTEXT_ENTER, node);
                  result = jsFunc.bindArgsAndExecute(callContext, context, args);
               } else {
                  CoreContext callContext = new CoreContext(context, node, args);
                  if (newKeyword) {
                     callContext.callInfo = new CallInfo(true, callable);
                     callContext.thisObject = callable;
                  } else {
                     callContext.thisObject = bindThisForCall(receiver, context, false);
                  }

                  callContext.event(EventType.CONTEXT_ENTER, node);
                  result = callable.call(callContext, args);
                  if (AsyncActivation.anyAsync) {
                     AsyncSupport.hostReturnFence();
                  }

                  context.updateFrom(callContext);
               }

               if (newKeyword && newInstance != null && !(result instanceof ObjectLike)) {
                  result = newInstance;
               }

               return result;
            }
         }
      } else {
         throw JsErrorException.notAFunction(node);
      }
   }

   private static Object evalNewExpr(Node node, CoreContext context) {
      Node exprWrap = node.get(1);
      Node operand = exprWrap.getFirst();
      if (operand.type != NodeType.FN_TAGGED_TEMPLATE_EXPR) {
         return evalFnCall(exprWrap, context, true);
      } else {
         Object callable = evalFnTaggedTemplate(operand, context);
         if (context.isError()) {
            return Terms.UNDEFINED;
         } else if (callable instanceof JsCallable c && c.isConstructable()) {
            return invokeAsConstructor(c, new Object[0], operand, context);
         } else {
            throw JsErrorException.typeError(operand.getTextIncludingWhitespace() + " is not a constructor");
         }
      }
   }

   static Object constructFromHost(JsCallable callable, Object[] args, CoreContext context) {
      if (!callable.isConstructable()) {
         throw JsErrorException.typeError("target is not a constructor");
      } else {
         return invokeAsConstructor(callable, args, new Node(NodeType.NEW_EXPR), context);
      }
   }

   static void marshalExternalArgs(Object[] args) {
      for (int i = 0; i < args.length; i++) {
         Object arg = args[i];
         if (arg == Terms.UNDEFINED) {
            args[i] = null;
         } else if (arg instanceof JsValue jv && !(arg instanceof JsPrimitive)) {
            args[i] = jv.getJavaValue();
         }
      }
   }

   private static Object invokeAsConstructor(JsCallable callable, Object[] args, Node node, CoreContext context) {
      if (callable.isExternal()) {
         marshalExternalArgs(args);
      }

      ObjectLike newInstance = null;
      Object result;
      if (callable instanceof JsFunctionNode jsFunc) {
         CoreContext callContext = new CoreContext(context, node, args, jsFunc.declaredContext, jsFunc.capturedBindings);
         callContext.strict = jsFunc.strict;
         callContext.callInfo = new CallInfo(true, callable);
         callContext.privateEnv = jsFunc.privateEnv;
         callContext.activeFunction = jsFunc;
         newInstance = allocateInstance(jsFunc);
         if (jsFunc.getMember("prototype") instanceof ObjectLike protoObj) {
            setInstancePrototype(newInstance, protoObj);
         }

         callContext.thisObject = newInstance;
         if (jsFunc.isDefaultDerivedConstructor) {
            runSuperConstructor(jsFunc, newInstance, args, callContext);
            runInstanceFieldInitializers(jsFunc, newInstance, callContext);
         } else if (!jsFunc.isDerivedConstructor) {
            runInstanceFieldInitializers(jsFunc, newInstance, callContext);
         }

         callContext.event(EventType.CONTEXT_ENTER, node);
         result = jsFunc.bindArgsAndExecute(callContext, context, args);
      } else {
         CoreContext callContextx = new CoreContext(context, node, args);
         callContextx.callInfo = new CallInfo(true, callable);
         callContextx.thisObject = callable;
         callContextx.event(EventType.CONTEXT_ENTER, node);
         result = callable.call(callContextx, args);
         if (AsyncActivation.anyAsync) {
            AsyncSupport.hostReturnFence();
         }

         context.updateFrom(callContextx);
      }

      if (newInstance != null && !(result instanceof ObjectLike)) {
         result = newInstance;
      }

      return result;
   }

   private static Object evalFnTaggedTemplate(Node node, CoreContext context) {
      Node callableNode = node.get(0);
      Node tpl = node.get(1);
      JsArray cooked = new JsArray();
      JsArray raw = new JsArray();
      List<Object> substitutions = new ArrayList<>();
      StringBuilder cookedAccum = new StringBuilder();
      StringBuilder rawAccum = new StringBuilder();
      int i = 0;

      for (int n = tpl.size(); i < n; i++) {
         Node child = tpl.get(i);
         if (child.isToken()) {
            TokenType tt = child.token.type;
            if (tt == TokenType.T_STRING) {
               String text = child.token.getText();
               rawAccum.append(text);
               cookedAccum.append(unescapeString(text));
            } else if (tt == TokenType.DOLLAR_L_CURLY) {
               cooked.add(cookedAccum.toString());
               raw.add(rawAccum.toString());
               cookedAccum.setLength(0);
               rawAccum.setLength(0);
            }
         } else if (child.type == NodeType.EXPR) {
            Object value = eval(child, context);
            if (context.isError()) {
               return Terms.UNDEFINED;
            }

            substitutions.add(value);
         }
      }

      cooked.add(cookedAccum.toString());
      raw.add(rawAccum.toString());
      cooked.putMember("raw", raw);
      Object o = PropertyAccess.getCallable(callableNode, context);
      Object receiver = context.callReceiver;
      if (o == Terms.UNDEFINED) {
         return o;
      } else if (!(o instanceof JsCallable callable)) {
         throw JsErrorException.notAFunction(callableNode);
      } else {
         Object[] args = new Object[substitutions.size() + 1];
         args[0] = cooked;

         for (int var21 = 0; var21 < substitutions.size(); var21++) {
            args[var21 + 1] = substitutions.get(var21);
         }

         if (callable.isExternal()) {
            marshalExternalArgs(args);
         }

         Object result;
         if (callable instanceof JsFunctionNode jsFunc) {
            CoreContext callContext = new CoreContext(context, node, args, jsFunc.declaredContext, jsFunc.capturedBindings);
            callContext.strict = jsFunc.strict;
            if (!jsFunc.arrow) {
               callContext.thisObject = bindThisForCall(receiver, context, jsFunc.strict);
            } else {
               bindArrowThis(callContext, jsFunc);
            }

            callContext.event(EventType.CONTEXT_ENTER, node);
            result = jsFunc.bindArgsAndExecute(callContext, context, args);
         } else {
            CoreContext callContext = new CoreContext(context, node, args);
            callContext.thisObject = bindThisForCall(receiver, context, false);
            callContext.event(EventType.CONTEXT_ENTER, node);
            result = callable.call(callContext, args);
            context.updateFrom(callContext);
         }

         return result;
      }
   }

   private static void instantiateLexicalScope(Node node, CoreContext context) {
      LexicalScope ls = node.meta instanceof LexicalScope l ? l : LexicalScope.of(node);
      if (ls != LexicalScope.NONE) {
         ls.instantiate(context);
      }
   }

   static void hoistFunctionDeclarations(Node parent, CoreContext context) {
      int i = 0;

      for (int n = parent.size(); i < n; i++) {
         Node child = parent.get(i);
         if (isFunctionDeclarationStatement(child)) {
            evalFnExpr(child.getFirst(), context);
         }
      }
   }

   private static boolean isFunctionDeclarationStatement(Node stmt) {
      if (stmt.type != NodeType.STATEMENT) {
         return false;
      } else {
         short memo = stmt.slot;
         if (memo >= 0) {
            return memo == 1;
         } else {
            boolean result = computeFunctionDeclarationStatement(stmt);
            stmt.slot = (short)(result ? 1 : 0);
            return result;
         }
      }
   }

   private static boolean computeFunctionDeclarationStatement(Node stmt) {
      if (stmt.size() == 0) {
         return false;
      } else {
         Node first = stmt.getFirst();
         if (first.type == NodeType.FN_EXPR && first.size() >= 2) {
            int nameIndex = fnNameOrArgsIndex(first);
            if (first.size() <= nameIndex) {
               return false;
            } else {
               Node second = first.get(nameIndex);
               return second.token != null && second.token.type == TokenType.IDENT;
            }
         } else {
            return false;
         }
      }
   }

   private static int fnKeywordIndex(Node fnExpr) {
      return fnExpr.getFirst().token.type == TokenType.FUNCTION ? 0 : 1;
   }

   private static int fnNameOrArgsIndex(Node fnExpr) {
      int i = fnKeywordIndex(fnExpr) + 1;
      Node child = fnExpr.get(i);
      return child.token != null && child.token.type == TokenType.STAR ? i + 1 : i;
   }

   private static Object evalFnExpr(Node node, CoreContext context) {
      if (node.getFirst().type == NodeType.FN_DECL_ARGS) {
         return new JsFunctionNode(false, node, fnArgs(node.getFirst()), node.getLast(), context);
      } else {
         int i = fnNameOrArgsIndex(node);
         if (node.get(i).token.type == TokenType.IDENT) {
            JsFunctionNode fn = new JsFunctionNode(false, node, fnArgs(node.get(i + 1)), node.getLast(), context);
            context.put(node.get(i).getText(), fn);
            return fn;
         } else {
            return new JsFunctionNode(false, node, fnArgs(node.get(i)), node.getLast(), context);
         }
      }
   }

   private static Object evalFnArrowExpr(Node node, CoreContext context) {
      Node first = node.async ? node.get(1) : node.getFirst();
      List<Node> argNodes;
      if (first.type == NodeType.FN_DECL_ARGS) {
         argNodes = fnArgs(first);
      } else {
         argNodes = Collections.singletonList(node);
      }

      return new JsFunctionNode(true, node, argNodes, node.getLast(), context);
   }

   private static Object evalClassExpr(Node node, CoreContext context) {
      String className = null;
      if (node.size() > 1 && node.get(1).token != null && node.get(1).token.type == TokenType.IDENT) {
         className = node.get(1).getText();
      }

      if (className == null) {
         return evalClassDefinition(node, context, null, null);
      } else {
         context.enterScope(ContextScope.BLOCK, node);

         Object result;
         try {
            context.declareUninitialized(className, BindScope.CONST);
            BindingSlot inner = context.bindings.getSlot(className);
            result = evalClassDefinition(node, context, className, inner);
         } finally {
            context.exitScope();
         }

         Node parent = node.getParent();
         if (parent != null && parent.type == NodeType.STATEMENT && result instanceof JsFunctionNode && !context.isError()) {
            context.declare(className, result, BindScope.LET, true);
         }

         return result;
      }
   }

   private static Object evalClassDefinition(Node node, CoreContext context, String className, BindingSlot innerBinding) {
      boolean hasExtends = false;
      Object parentValue = null;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (child.token != null && child.token.type == TokenType.EXTENDS) {
            hasExtends = true;
            parentValue = eval(node.get(i + 1), context);
            break;
         }
      }

      if (context.isError()) {
         return Terms.UNDEFINED;
      } else {
         ObjectLike parentCtor = null;
         if (hasExtends && parentValue != null && parentValue != Terms.UNDEFINED) {
            if (!(parentValue instanceof JsCallable pc) || !pc.isConstructable() || !(parentValue instanceof ObjectLike)) {
               throw JsErrorException.typeError("Class extends value is not a constructor or null");
            }

            parentCtor = (ObjectLike)parentValue;
         }

         Node ctorFnExpr = null;
         List<Interpreter.ClassMember> members = new ArrayList<>();
         int ix = 0;

         for (int nx = node.size(); ix < nx; ix++) {
            Node child = node.get(ix);
            if (child.type == NodeType.CLASS_STATIC_BLOCK) {
               Interpreter.ClassMember block = new Interpreter.ClassMember();
               block.isStatic = true;
               block.staticBlock = child;
               members.add(block);
            } else if (child.type == NodeType.CLASS_METHOD) {
               Interpreter.ClassMember cm = analyzeClassMember(child);
               if (!cm.isField && !cm.isStatic && !cm.isAccessor && "constructor".equals(cm.simpleKey)) {
                  ctorFnExpr = cm.fnExpr;
               } else {
                  members.add(cm);
               }
            }
         }

         PrivateEnv savedPrivateEnv = context.privateEnv;
         PrivateEnv classPrivateEnv = declarePrivateNames(members, savedPrivateEnv);
         if (classPrivateEnv != null) {
            context.privateEnv = classPrivateEnv;
         }

         try {
            JsFunctionNode ctor;
            if (ctorFnExpr != null) {
               ctor = new JsFunctionNode(false, ctorFnExpr, fnArgs(ctorFnExpr.getFirst()), ctorFnExpr.getLast(), context, true);
            } else {
               ctor = new JsFunctionNode(false, node, Collections.emptyList(), new Node(NodeType.BLOCK), context, true);
            }

            ctor.name = className == null ? "" : className;
            ctor.isClassConstructor = true;
            if (hasExtends) {
               ctor.isDerivedConstructor = true;
               ctor.isDefaultDerivedConstructor = ctorFnExpr == null;
            }

            JsObject proto = ctor.getFunctionPrototype();
            proto.defineOwn("constructor", ctor, (byte)5);
            if (hasExtends) {
               if (parentCtor != null) {
                  ctor.setPrototype(parentCtor);
                  ctor.baseBuiltinCtor = parentCtor instanceof JsFunctionNode parentFn ? parentFn.baseBuiltinCtor : exoticBaseBuiltin(parentCtor);
                  proto.setPrototype(parentCtor.getMember("prototype") instanceof ObjectLike pp ? pp : null);
               } else {
                  proto.setPrototype(null);
               }
            }

            ctor.homeObject = proto;
            if (innerBinding != null) {
               innerBinding.value = ctor;
               innerBinding.initialized = true;
            }

            for (Interpreter.ClassMember cm : members) {
               if (cm.staticBlock == null) {
                  PrivateName pn = cm.privateName;
                  Object keyValue = pn == null ? classMemberKeyValue(cm, context) : pn.name;
                  JsSymbol symKey = JsSymbol.keyedBy(keyValue);
                  String key = symKey != null ? symKey.toString() : (String)keyValue;
                  if (context.isError()) {
                     return Terms.UNDEFINED;
                  }

                  if (cm.isField) {
                     if (cm.isStatic) {
                        cm.resolvedKey = key;
                        cm.resolvedSymbol = symKey;
                     } else {
                        if (ctor.instanceFields == null) {
                           ctor.instanceFields = new ArrayList<>();
                        }

                        ctor.instanceFields.add(new JsFunctionNode.FieldInit(pn == null ? key : null, pn, cm.initExpr));
                     }
                  } else {
                     JsObject target = (JsObject)(cm.isStatic ? ctor : proto);
                     JsFunctionNode fn = new JsFunctionNode(false, cm.fnExpr, fnArgs(cm.fnExpr.getFirst()), cm.fnExpr.getLast(), context, true);
                     fn.name = key;
                     fn.homeObject = target;
                     if (pn != null) {
                        if (cm.isAccessor) {
                           if ("get".equals(cm.kind)) {
                              pn.getter = fn;
                           } else {
                              pn.setter = fn;
                           }
                        } else {
                           pn.method = fn;
                        }

                        if (cm.isStatic) {
                           ctor.putPrivate(pn, PrivateName.BRAND);
                        } else {
                           if (ctor.privateBrands == null) {
                              ctor.privateBrands = new ArrayList<>();
                           }

                           if (!ctor.privateBrands.contains(pn)) {
                              ctor.privateBrands.add(pn);
                           }
                        }
                     } else if (cm.isAccessor && symKey != null) {
                        PropertySlot prior = target.ownSymbolSlot(symKey);
                        JsCallable g = prior instanceof AccessorSlot pa ? pa.getter : null;
                        JsCallable st = prior instanceof AccessorSlot pax ? pax.setter : null;
                        if ("get".equals(cm.kind)) {
                           g = fn;
                        } else {
                           st = fn;
                        }

                        target.defineOwnSymbolAccessor(symKey, g, st, (byte)4);
                     } else if (cm.isAccessor) {
                        PropertySlot existing = target.getOwnSlot(key);
                        JsCallable getter = existing instanceof AccessorSlot ea ? ea.getter : null;
                        JsCallable setter = existing instanceof AccessorSlot eax ? eax.setter : null;
                        if ("get".equals(cm.kind)) {
                           getter = fn;
                        } else {
                           setter = fn;
                        }

                        target.defineOwnAccessor(key, getter, setter, (byte)4);
                     } else if (symKey != null) {
                        target.defineOwnSymbol(symKey, fn, (byte)5);
                     } else {
                        target.defineOwn(key, fn, (byte)5);
                     }
                  }
               }
            }

            Iterator var39 = members.iterator();

            while (true) {
               while (true) {
                  if (!var39.hasNext()) {
                     return ctor;
                  }

                  Interpreter.ClassMember cmx = (Interpreter.ClassMember)var39.next();
                  if (cmx.staticBlock != null) {
                     runStaticBlock(cmx.staticBlock, ctor, context);
                     break;
                  }

                  if (cmx.isField && cmx.isStatic) {
                     Object value = evalFieldInitializer(cmx.initExpr, ctor, context);
                     if (!context.isError()) {
                        if (cmx.resolvedSymbol != null) {
                           ctor.defineOwnSymbol(cmx.resolvedSymbol, value, (byte)7);
                        } else if (cmx.privateName == null) {
                           ctor.putMember(cmx.resolvedKey, value);
                        } else {
                           ctor.putPrivate(cmx.privateName, value);
                        }
                     }
                     break;
                  }
               }

               if (context.isError()) {
                  return Terms.UNDEFINED;
               }
            }
         } finally {
            context.privateEnv = savedPrivateEnv;
         }
      }
   }

   private static PrivateEnv declarePrivateNames(List<Interpreter.ClassMember> members, PrivateEnv parent) {
      PrivateEnv env = null;

      for (Interpreter.ClassMember cm : members) {
         if (cm.isPrivate) {
            if (env == null) {
               env = new PrivateEnv(parent);
            }

            cm.privateName = env.declare(
               cm.simpleKey, cm.isField ? PrivateName.Kind.FIELD : (cm.isAccessor ? PrivateName.Kind.ACCESSOR : PrivateName.Kind.METHOD)
            );
         }
      }

      return env;
   }

   private static Object[] evalCallArgs(Node fnArgsNode, CoreContext context) {
      int argsCount = fnArgsNode == null ? 0 : fnArgsNode.size();
      if (argsCount == 0) {
         return EMPTY_ARGS;
      } else {
         Object[] args = new Object[argsCount];
         List<Object> spill = null;
         int n = 0;

         for (int i = 0; i < argsCount; i++) {
            Node fnArgNode = fnArgsNode.get(i);
            Node argNode = fnArgNode.get(0);
            if (argNode.isToken()) {
               if (spill == null) {
                  spill = new ArrayList<>(argsCount + 8);

                  for (int k = 0; k < n; k++) {
                     spill.add(args[k]);
                  }
               }

               Object arg = eval(fnArgNode.get(1), context);
               JsIterator iter = IterUtils.getIterator(arg, context);

               while (iter.hasNext()) {
                  spill.add(iter.next());
               }
            } else {
               Object v = eval(argNode, context);
               if (spill != null) {
                  spill.add(v);
               } else {
                  args[n++] = v;
               }
            }

            if (context.isError()) {
               break;
            }
         }

         if (spill != null) {
            return spill.toArray();
         } else {
            return n == args.length ? args : Arrays.copyOf(args, n);
         }
      }
   }

   private static Object evalSuperCall(Node fnArgsNode, CoreContext context) {
      JsFunctionNode active = context.activeFunction;
      if (active == null) {
         throw JsErrorException.syntaxError("'super' keyword is only valid inside a class");
      } else {
         Object[] args = evalCallArgs(fnArgsNode, context);
         if (context.isError()) {
            return Terms.UNDEFINED;
         } else {
            runSuperConstructor(active, context.thisObject, args, context);
            runInstanceFieldInitializers(active, context.thisObject, context);
            return Terms.UNDEFINED;
         }
      }
   }

   private static JsCallable exoticBaseBuiltin(Object parentCtor) {
      return !(parentCtor instanceof JsArrayConstructor)
            && !(parentCtor instanceof JsMapConstructor)
            && !(parentCtor instanceof JsSetConstructor)
            && !(parentCtor instanceof JsDateConstructor)
         ? null
         : (JsCallable)parentCtor;
   }

   private static ObjectLike allocateInstance(JsFunctionNode ctor) {
      JsCallable base = ctor.baseBuiltinCtor;
      if (base == null) {
         return new JsObject();
      } else if (base instanceof JsArrayConstructor) {
         return new JsArray();
      } else if (base instanceof JsMapConstructor) {
         return new JsMap();
      } else if (base instanceof JsSetConstructor) {
         return new JsSet();
      } else {
         return (ObjectLike)(base instanceof JsDateConstructor ? new JsDate(Double.NaN) : new JsObject());
      }
   }

   private static void setInstancePrototype(ObjectLike instance, ObjectLike proto) {
      if (instance instanceof JsObject jo) {
         jo.setPrototype(proto);
      } else if (instance instanceof JsArray ja) {
         ja.setPrototype(proto);
      }
   }

   private static boolean initializeExoticReceiver(Object thisObj, Object built) {
      if (thisObj instanceof JsArray dst && built instanceof JsArray src) {
         dst.list.addAll(src.list);
         return true;
      } else if (thisObj instanceof JsMap dst && built instanceof JsMap src) {
         dst.entries.putAll(src.entries);
         return true;
      } else if (thisObj instanceof JsSet dst && built instanceof JsSet src) {
         dst.elements.putAll(src.elements);
         return true;
      } else if (thisObj instanceof JsDate dst && built instanceof JsDate src) {
         dst.setTimeValue(src.getTimeValue());
         return true;
      } else {
         return false;
      }
   }

   static void runSuperConstructor(JsFunctionNode derivedCtor, Object thisObj, Object[] args, CoreContext context) {
      Object parent = derivedCtor.getPrototype();
      if (parent instanceof JsCallable parentCallable && parentCallable.isConstructable()) {
         if (parent instanceof JsFunctionNode parentFn) {
            CoreContext sc = new CoreContext(context, parentFn.node, args, parentFn.declaredContext, parentFn.capturedBindings);
            sc.strict = parentFn.strict;
            sc.thisObject = thisObj;
            sc.activeFunction = parentFn;
            sc.callInfo = new CallInfo(true, parentFn);
            sc.privateEnv = parentFn.privateEnv;
            if (parentFn.isDefaultDerivedConstructor) {
               runSuperConstructor(parentFn, thisObj, args, sc);
               runInstanceFieldInitializers(parentFn, thisObj, sc);
            } else if (!parentFn.isDerivedConstructor) {
               runInstanceFieldInitializers(parentFn, thisObj, sc);
            }

            parentFn.bindArgsAndExecute(sc, context, args);
         } else {
            Object built = constructFromHost(parentCallable, args, context);
            if (initializeExoticReceiver(thisObj, built)) {
               return;
            }

            if (built instanceof ObjectLike src && thisObj instanceof JsObject dst) {
               for (Entry<String, Object> e : src.toMap().entrySet()) {
                  dst.putMember(e.getKey(), e.getValue());
               }

               if (built instanceof JsError err) {
                  dst.defineOwn("stack", err.getMember("stack"), (byte)5);
               }
            }
         }
      } else {
         throw JsErrorException.typeError("super: parent class is not a constructor");
      }
   }

   private static Object evalFieldInitializer(Node initExpr, Object thisObj, CoreContext context) {
      if (initExpr == null) {
         return Terms.UNDEFINED;
      } else {
         CoreContext fieldContext = new CoreContext(context, initExpr, null, context, null);
         fieldContext.strict = context.strict;
         fieldContext.thisObject = thisObj;
         Object result = eval(initExpr, fieldContext);
         if (fieldContext.isError()) {
            context.updateFrom(fieldContext);
         }

         return result;
      }
   }

   private static void runStaticBlock(Node blockNode, JsFunctionNode ctor, CoreContext context) {
      Node body = blockNode.getLast();
      JsFunctionNode blockFn = new JsFunctionNode(false, blockNode, Collections.emptyList(), body, context, true);
      blockFn.homeObject = ctor;
      CoreContext blockContext = new CoreContext(context, blockNode, EMPTY_ARGS, context, null);
      blockContext.strict = true;
      blockContext.thisObject = ctor;
      blockContext.activeFunction = blockFn;
      blockFn.bindArgsAndExecute(blockContext, context, EMPTY_ARGS);
   }

   static void runInstanceFieldInitializers(JsFunctionNode ctor, Object thisObj, CoreContext context) {
      if (thisObj instanceof JsObject obj) {
         if (ctor.privateBrands != null) {
            for (PrivateName pn : ctor.privateBrands) {
               obj.putPrivate(pn, PrivateName.BRAND);
            }
         }

         if (ctor.instanceFields != null) {
            for (JsFunctionNode.FieldInit f : ctor.instanceFields) {
               Object value = evalFieldInitializer(f.initializer, thisObj, context);
               if (context.isError()) {
                  return;
               }

               if (f.privateName == null) {
                  obj.putMember(f.key, value);
               } else {
                  obj.putPrivate(f.privateName, value);
               }
            }
         }
      } else {
         if (thisObj instanceof ObjectLike ol && ctor.instanceFields != null) {
            for (JsFunctionNode.FieldInit f : ctor.instanceFields) {
               Object valuex = evalFieldInitializer(f.initializer, thisObj, context);
               if (context.isError()) {
                  return;
               }

               if (f.privateName == null) {
                  ol.putMember(f.key, valuex);
               }
            }
         }
      }
   }

   static ObjectLike evalSuperBase(CoreContext context) {
      JsFunctionNode active = context.activeFunction;
      if (active != null && active.homeObject != null) {
         ObjectLike home = active.homeObject;
         return home.getPrototype() instanceof ObjectLike ol ? ol : null;
      } else {
         throw JsErrorException.syntaxError("'super' keyword is only valid inside a class method");
      }
   }

   private static Interpreter.ClassMember analyzeClassMember(Node m) {
      Interpreter.ClassMember cm = new Interpreter.ClassMember();
      int n = m.size();
      Node lastChild = m.get(n - 1);
      int keyEnd;
      if (lastChild.type == NodeType.FN_EXPR) {
         cm.fnExpr = lastChild;
         keyEnd = n - 2;
      } else {
         cm.isField = true;
         int eqIdx = -1;

         for (int i = 0; i < n; i++) {
            Node c = m.get(i);
            if (c.token != null && c.token.type == TokenType.EQ) {
               eqIdx = i;
               break;
            }
         }

         if (eqIdx >= 0) {
            cm.initExpr = m.get(eqIdx + 1);
            keyEnd = eqIdx - 1;
         } else {
            keyEnd = lastChild.token != null && lastChild.token.type == TokenType.SEMI ? n - 2 : n - 1;
         }
      }

      Node kc = m.get(keyEnd);
      int keyStart;
      if (kc.token != null && kc.token.type == TokenType.R_BRACKET) {
         cm.computed = true;
         cm.keyExprNode = m.get(keyEnd - 1);
         keyStart = keyEnd - 2;
      } else {
         cm.simpleKey = keyTokenString(kc.token);
         cm.isPrivate = kc.token.type == TokenType.PRIVATE_NAME;
         keyStart = keyEnd;
      }

      for (int ix = 0; ix < keyStart; ix++) {
         String t = m.get(ix).getText();
         if ("static".equals(t)) {
            cm.isStatic = true;
         } else if ("get".equals(t)) {
            cm.isAccessor = true;
            cm.kind = "get";
         } else if ("set".equals(t)) {
            cm.isAccessor = true;
            cm.kind = "set";
         }
      }

      return cm;
   }

   private static String keyTokenString(Token t) {
      return t.type != TokenType.S_STRING && t.type != TokenType.D_STRING ? t.getText() : (String)Terms.literalValue(t);
   }

   private static Object classMemberKeyValue(Interpreter.ClassMember cm, CoreContext context) {
      if (cm.computed) {
         Object keyValue = eval(cm.keyExprNode, context);
         JsSymbol sym = JsSymbol.keyedBy(keyValue);
         return sym != null ? sym : Terms.toPropertyKey(keyValue, context);
      } else {
         return cm.simpleKey;
      }
   }

   private static boolean loopVarsAllSlotted(Node varStmt) {
      boolean any = false;

      for (Node declarator : varStmt.findImmediateChildren(NodeType.VAR_DECL)) {
         Node binding = declarator.getFirst();
         if (!binding.isToken() || binding.token.type != TokenType.IDENT || binding.slot < 0) {
            return false;
         }

         any = true;
      }

      return any;
   }

   private static Object evalForStmt(Node node, CoreContext context) {
      context.enterScope(ContextScope.LOOP_INIT, node);
      context.event(EventType.CONTEXT_ENTER, node);
      instantiateLexicalScope(node, context);
      Node forBody = node.getLast();
      Object forResult = null;
      boolean enteredBodyScope = false;

      try {
         if (node.get(2).token.type != TokenType.SEMI && node.get(3).token.type != TokenType.SEMI) {
            boolean in = node.get(3).token.type == TokenType.IN;
            Object forObject = eval(node.get(4), context);
            BindScope bindScope;
            Node bindings;
            if (node.get(2).type == NodeType.VAR_STMT) {
               Node declarator = node.get(2).get(1);
               bindings = declarator.getFirst();

               bindScope = switch (node.get(2).getFirstToken().type) {
                  case LET -> BindScope.LET;
                  case CONST -> BindScope.CONST;
                  default -> BindScope.VAR;
               };
            } else {
               bindScope = BindScope.VAR;
               bindings = node.get(2);
               Node bare = bindings;

               while (
                  bare != null
                     && !bare.isToken()
                     && bare.size() > 0
                     && (bare.type == NodeType.EXPR_LIST || bare.type == NodeType.EXPR || bare.type == NodeType.LIT_EXPR)
               ) {
                  bare = bare.getFirst();
               }

               if (bare == null || bare.isToken() || bare.type != NodeType.LIT_ARRAY && bare.type != NodeType.LIT_OBJECT) {
                  if (bare != null && (bare.type == NodeType.REF_DOT_EXPR || bare.type == NodeType.REF_BRACKET_EXPR)) {
                     bindings = bare;
                     bindScope = null;
                  }
               } else {
                  bindings = bare;
                  bindScope = null;
               }
            }

            boolean isLetOrConst = bindScope == BindScope.LET || bindScope == BindScope.CONST;
            if (in) {
               int index = -1;

               for (KeyValue kv : Terms.forInIterable(forObject, context)) {
                  checkInterrupted();
                  context.iteration = ++index;
                  if (isLetOrConst) {
                     context.enterScope(ContextScope.LOOP_BODY, forBody);
                     enteredBodyScope = true;
                  }

                  evalAssign(bindings, context, bindScope, kv.key(), true);
                  if (!context.isStopped()) {
                     forResult = eval(forBody, context);
                  }

                  if (isLetOrConst && enteredBodyScope) {
                     context.exitScope();
                     enteredBodyScope = false;
                  }

                  if (context.isStopped()) {
                     if (!context.isContinuing() || !targetsStatement(context, node)) {
                        break;
                     }

                     context.reset();
                  }
               }
            } else {
               JsIterator iter = IterUtils.getIterator(forObject, context);
               int index = -1;

               while (iter.hasNext()) {
                  checkInterrupted();
                  context.iteration = ++index;
                  Object varValue = iter.next();
                  if (isLetOrConst) {
                     context.enterScope(ContextScope.LOOP_BODY, forBody);
                     enteredBodyScope = true;
                  }

                  evalAssign(bindings, context, bindScope, varValue, true);
                  if (!context.isStopped()) {
                     forResult = eval(forBody, context);
                  }

                  if (isLetOrConst && enteredBodyScope) {
                     context.exitScope();
                     enteredBodyScope = false;
                  }

                  if (context.isStopped()) {
                     if (!context.isContinuing() || !targetsStatement(context, node)) {
                        iter.close(context, context.isError());
                        break;
                     }

                     context.reset();
                  }
               }
            }
         } else {
            boolean noInit = node.get(2).token.type == TokenType.SEMI;
            boolean isLetOrConst = false;
            BindScope loopVarScope = null;
            List<String> loopVarNames = null;
            if (!noInit && node.get(2).type == NodeType.VAR_STMT) {
               evalVarStmt(node.get(2), context);
               isLetOrConst = node.get(2).getFirstToken().type != TokenType.VAR;
               if (isLetOrConst && context.frame != null && loopVarsAllSlotted(node.get(2))) {
                  isLetOrConst = false;
               }

               if (isLetOrConst) {
                  loopVarNames = new ArrayList<>();

                  for (Node declarator : node.get(2).findImmediateChildren(NodeType.VAR_DECL)) {
                     JsParser.boundNames(declarator, loopVarNames);
                  }

                  loopVarScope = node.get(2).getFirstToken().type == TokenType.LET ? BindScope.LET : BindScope.CONST;
               }
            } else if (!noInit) {
               eval(node.get(2), context);
            }

            if (context.isStopped()) {
               return null;
            }

            int semi1 = noInit ? 2 : 3;
            Node forTest = node.get(semi1 + 1).token.type == TokenType.SEMI ? null : node.get(semi1 + 1);
            int semi2 = forTest == null ? semi1 + 1 : semi1 + 2;
            Node forAfter = node.get(semi2 + 1).token.type == TokenType.R_PAREN ? null : node.get(semi2 + 1);
            if (isLetOrConst && loopVarNames != null && !loopVarNames.isEmpty()) {
               List<Object> carry = new ArrayList<>(loopVarNames.size());

               for (String name : loopVarNames) {
                  carry.add(context.get(name));
               }

               int index = -1;

               while (true) {
                  checkInterrupted();
                  context.iteration = ++index;
                  context.enterScope(ContextScope.LOOP_BODY, forBody);
                  enteredBodyScope = true;

                  for (int k = 0; k < loopVarNames.size(); k++) {
                     context.declare(loopVarNames.get(k), carry.get(k), loopVarScope, true);
                  }

                  Object forCondition = forTest == null ? Boolean.TRUE : eval(forTest, context);
                  if (context.isStopped() || !Terms.isTruthy(forCondition)) {
                     break;
                  }

                  forResult = eval(forBody, context);
                  if (context.isStopped()) {
                     if (!context.isContinuing() || !targetsStatement(context, node)) {
                        break;
                     }

                     context.reset();
                  }

                  List<Object> bodyVals = new ArrayList<>(loopVarNames.size());

                  for (String name : loopVarNames) {
                     bodyVals.add(context.get(name));
                  }

                  context.exitScope();
                  context.enterScope(ContextScope.LOOP_BODY, forBody);

                  for (int k = 0; k < loopVarNames.size(); k++) {
                     context.declare(loopVarNames.get(k), bodyVals.get(k), loopVarScope, true);
                  }

                  if (forAfter != null) {
                     eval(forAfter, context);
                  }

                  for (int k = 0; k < loopVarNames.size(); k++) {
                     carry.set(k, context.get(loopVarNames.get(k)));
                  }

                  context.exitScope();
                  enteredBodyScope = false;
               }
            } else {
               int index = -1;

               while (true) {
                  checkInterrupted();
                  context.iteration = ++index;
                  if (context.isStopped()) {
                     break;
                  }

                  if (forTest != null) {
                     Object forConditionx = eval(forTest, context);
                     if (context.isStopped() || !Terms.isTruthy(forConditionx)) {
                        break;
                     }
                  }

                  forResult = eval(forBody, context);
                  if (context.isStopped()) {
                     if (!context.isContinuing() || !targetsStatement(context, node)) {
                        break;
                     }

                     context.reset();
                  }

                  if (forAfter != null) {
                     eval(forAfter, context);
                  }
               }
            }
         }
      } finally {
         if (enteredBodyScope) {
            context.exitScope();
         }
      }

      if (context.isBreaking() && targetsStatement(context, node)) {
         context.reset();
      }

      context.event(EventType.CONTEXT_EXIT, node);
      context.exitScope();
      return forResult;
   }

   private static Object evalIfStmt(Node node, CoreContext context) {
      Object condition = eval(node.get(2), context);
      if (context.isStopped()) {
         return null;
      } else {
         boolean taken = Terms.isTruthy(condition);
         context.event(EventType.BRANCH, node, taken);
         if (taken) {
            return eval(node.get(4), context);
         } else {
            return node.size() > 5 ? eval(node.get(6), context) : null;
         }
      }
   }

   private static Object evalInstanceOfExpr(Node node, CoreContext context) {
      Object lhs = eval(node.get(0), context);
      return context.isError() ? Terms.UNDEFINED : Terms.instanceOf(lhs, eval(node.get(2), context));
   }

   private static Object evalInExpr(Node node, CoreContext context) {
      if (node.get(0).type == NodeType.PRIVATE_NAME_EXPR) {
         PrivateName pn = PrivateAccess.resolve(node.get(0).getText(), context);
         return PrivateAccess.has(eval(node.get(2), context), pn);
      } else {
         Object lhs = eval(node.get(0), context);
         return context.isError() ? Terms.UNDEFINED : Terms.in(lhs, eval(node.get(2), context));
      }
   }

   private static BindScope toScope(BindScope scope) {
      return scope == BindScope.VAR ? null : scope;
   }

   private static Object evalLitArray(Node node, CoreContext context) {
      int last = node.size() - 1;
      JsArray array = new JsArray();
      List<Object> list = array.list;

      for (int i = 1; i < last; i++) {
         Node elem = node.get(i);
         Node exprNode = elem.get(0);
         if (exprNode.token.type == TokenType.DOT_DOT_DOT) {
            Object value = eval(elem.get(1), context);
            JsIterator iter = IterUtils.getIterator(value, context);

            while (iter.hasNext()) {
               list.add(iter.next());
            }
         } else if (exprNode.token.type == TokenType.COMMA) {
            list.add(JsArray.HOLE);
         } else {
            list.add(evalExpr(exprNode, context));
         }

         if (context.isError()) {
            break;
         }
      }

      return array;
   }

   private static Object evalLitObject(Node node, CoreContext context) {
      int last = node.size() - 1;
      JsObject result = new JsObject(new LinkedHashMap<>(last - 1));

      for (int i = 1; i < last; i++) {
         Node elem = node.get(i);
         Node keyNode = elem.getFirst();
         TokenType token = keyNode.token.type;
         if (token == TokenType.IDENT && isAccessorKeyword(keyNode.getText()) && elem.size() > 2 && accessorFnExprPosition(elem) > 1) {
            evalAccessorElem(elem, keyNode.getText(), context, result);
         } else {
            int keyPos = 0;
            if (token == TokenType.IDENT && "async".equals(keyNode.getText()) && elem.size() > 2 && accessorFnExprPosition(elem) > 1) {
               keyPos = 1;
               keyNode = elem.get(keyPos);
               token = keyNode.token.type;
            } else if (token == TokenType.STAR) {
               keyPos = 1;
               keyNode = elem.get(keyPos);
               token = keyNode.token.type;
            }

            boolean computed = token == TokenType.L_BRACKET;
            int afterKeyPos = keyPos + (computed ? 3 : 1);
            JsSymbol symKey = null;
            String key;
            if (computed) {
               Object keyValue = evalExpr(elem.get(keyPos + 1), context);
               symKey = JsSymbol.keyedBy(keyValue);
               key = symKey != null ? null : Terms.toPropertyKey(keyValue, context);
            } else if (token == TokenType.DOT_DOT_DOT) {
               key = null;
            } else if (token != TokenType.S_STRING && token != TokenType.D_STRING) {
               key = keyNode.getText();
            } else {
               key = (String)Terms.literalValue(keyNode.token);
            }

            if (token == TokenType.DOT_DOT_DOT) {
               Terms.copyDataProperties(result, evalExpr(elem.get(1), context), null, context);
            } else if (elem.size() > afterKeyPos && elem.get(afterKeyPos).type == NodeType.FN_EXPR) {
               putLiteralValue(result, key, symKey, evalFnExpr(elem.get(afterKeyPos), context));
            } else if (!computed && elem.size() < 3) {
               result.put(key, context.get(key));
            } else if (JsParser.isProtoSetter(elem)) {
               setLiteralPrototype(result, evalExpr(elem.get(afterKeyPos + 1), context), context);
            } else {
               putLiteralValue(result, key, symKey, evalExpr(elem.get(afterKeyPos + 1), context));
            }

            if (context.isError()) {
               break;
            }
         }
      }

      return result;
   }

   private static void putLiteralValue(JsObject result, String key, JsSymbol symKey, Object value) {
      if (symKey != null) {
         result.defineOwnSymbol(symKey, value, (byte)7);
      } else {
         result.put(key, value);
      }
   }

   private static void setLiteralPrototype(JsObject result, Object value, CoreContext context) {
      if (!context.isError()) {
         if (value == null) {
            result.setPrototype(null);
         } else if (value instanceof ObjectLike proto) {
            result.setPrototype(proto);
         }
      }
   }

   private static boolean isAccessorKeyword(String text) {
      return "get".equals(text) || "set".equals(text);
   }

   static Object invokeGetter(JsCallable getter, Object thisObj, CoreContext context) {
      Object savedThis = context.thisObject;
      context.thisObject = thisObj;

      Object var4;
      try {
         var4 = getter.call(context, EMPTY_ARGS);
      } finally {
         context.thisObject = savedThis;
      }

      return var4;
   }

   static void invokeSetter(JsCallable setter, Object thisObj, Object value, CoreContext context) {
      Object savedThis = context.thisObject;
      context.thisObject = thisObj;

      try {
         setter.call(context, new Object[]{value});
      } finally {
         context.thisObject = savedThis;
      }
   }

   private static int accessorFnExprPosition(Node elem) {
      for (int j = elem.size() - 1; j >= 0; j--) {
         if (elem.get(j).type == NodeType.FN_EXPR) {
            return j;
         }
      }

      return -1;
   }

   private static void evalAccessorElem(Node elem, String kind, CoreContext context, JsObject target) {
      int fnPos = accessorFnExprPosition(elem);
      Node keyChild = elem.get(1);
      JsSymbol symKey = null;
      String key;
      if (keyChild.token != null && keyChild.token.type == TokenType.L_BRACKET) {
         Object keyValue = evalExpr(elem.get(2), context);
         symKey = JsSymbol.keyedBy(keyValue);
         key = symKey != null ? symKey.toString() : Terms.toPropertyKey(keyValue, context);
      } else if (keyChild.token == null || keyChild.token.type != TokenType.S_STRING && keyChild.token.type != TokenType.D_STRING) {
         key = keyChild.getText();
      } else {
         key = (String)Terms.literalValue(keyChild.token);
      }

      if (evalFnExpr(elem.get(fnPos), context) instanceof JsCallable callable) {
         PropertySlot existing = symKey != null ? target.ownSymbolSlot(symKey) : target.getOwnSlot(key);
         JsCallable getter = existing instanceof AccessorSlot ea ? ea.getter : null;
         JsCallable setter = existing instanceof AccessorSlot eax ? eax.setter : null;
         if ("get".equals(kind)) {
            getter = callable;
         } else {
            setter = callable;
         }

         if (symKey != null) {
            target.defineOwnSymbolAccessor(symKey, getter, setter, (byte)6);
         } else {
            target.defineOwnAccessor(key, getter, setter, (byte)6);
         }
      }
   }

   private static String evalLitTemplate(Node node, CoreContext context) {
      StringBuilder sb = new StringBuilder();
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         if (context.isError()) {
            return sb.toString();
         }

         Node child = node.get(i);
         if (child.token.type == TokenType.T_STRING) {
            sb.append(unescapeString(child.token.getText()));
         } else if (child.type == NodeType.EXPR) {
            Object value = eval(child, context);
            if (context.isError()) {
               return sb.toString();
            }

            sb.append(Terms.toStringCoerce(value, context));
         }
      }

      return sb.toString();
   }

   private static String unescapeString(String s) {
      return JsLexer.unescapeStringLiteral(s);
   }

   private static Object evalLitExpr(Node node, CoreContext context) {
      node = node.getFirst();
      if (!node.isToken()) {
         return switch (node.type) {
            case LIT_ARRAY -> evalLitArray(node, context);
            case LIT_OBJECT -> evalLitObject(node, context);
            case LIT_TEMPLATE -> evalLitTemplate(node, context);
            case LIT_REGEX -> new JsRegex(node.getFirstToken().getText());
            default -> throw new RuntimeException("unexpected lit expr: " + node);
         };
      } else {
         Object value = Terms.literalValue(node.token);
         return !(value instanceof String s && (node.token.type == TokenType.S_STRING || node.token.type == TokenType.D_STRING)) ? value : unescapeString(s);
      }
   }

   private static Object evalLogicBitExpr(Node node, CoreContext context) {
      return switch (node.get(1).token.type) {
         case AMP -> Terms.bitAnd(lhsOperand(node, context), rhsOperand(node, context), context);
         case PIPE -> Terms.bitOr(lhsOperand(node, context), rhsOperand(node, context), context);
         case CARET -> Terms.bitXor(lhsOperand(node, context), rhsOperand(node, context), context);
         case GT_GT -> Terms.bitShiftRight(lhsOperand(node, context), rhsOperand(node, context), context);
         case LT_LT -> Terms.bitShiftLeft(lhsOperand(node, context), rhsOperand(node, context), context);
         case GT_GT_GT -> Terms.bitShiftRightUnsigned(lhsOperand(node, context), rhsOperand(node, context), context);
         default -> throw new RuntimeException("unexpected operator: " + node.get(1));
      };
   }

   private static boolean evalLogicExpr(Node node, CoreContext context) {
      Object lhs = eval(node.get(0), context);
      if (context.isStopped()) {
         return false;
      } else {
         Object rhs = eval(node.get(2), context);
         if (context.isStopped()) {
            return false;
         } else {
            TokenType logicOp = node.get(1).token.type;
            if (context.root.listener != null) {
               context.event(EventType.COMPARE, node, new Object[]{lhs, node.get(1).getText(), rhs});
            }
            return switch (logicOp) {
               case EQ_EQ -> Terms.looseEq(lhs, rhs, context);
               case EQ_EQ_EQ -> Terms.strictEq(lhs, rhs);
               case NOT_EQ -> !Terms.looseEq(lhs, rhs, context);
               case NOT_EQ_EQ -> !Terms.strictEq(lhs, rhs);
               case LT -> Terms.isLessThan(lhs, rhs, true, context) == 1;
               case GT -> Terms.isLessThan(rhs, lhs, false, context) == 1;
               case LT_EQ -> Terms.isLessThan(rhs, lhs, false, context) == 0;
               case GT_EQ -> Terms.isLessThan(lhs, rhs, true, context) == 0;
               default -> throw new RuntimeException("unexpected operator: " + node.get(1));
            };
         }
      }
   }

   private static Object evalLogicAndExpr(Node node, CoreContext context) {
      Object lhsValue = eval(node.get(0), context);
      if (context.isStopped()) {
         return null;
      } else {
         boolean lhs = Terms.isTruthy(lhsValue);
         context.event(EventType.BRANCH, node, lhs);
         if (node.get(1).token.type == TokenType.AMP_AMP) {
            return lhs ? eval(node.get(2), context) : lhsValue;
         } else {
            return lhs ? lhsValue : eval(node.get(2), context);
         }
      }
   }

   private static Object evalLogicNullishExpr(Node node, CoreContext context) {
      Object lhsValue = eval(node.get(0), context);
      if (context.isStopped()) {
         return null;
      } else {
         boolean defined = lhsValue != null && lhsValue != Terms.UNDEFINED;
         context.event(EventType.BRANCH, node, defined);
         return !defined ? eval(node.get(2), context) : lhsValue;
      }
   }

   private static Object evalLogicTernExpr(Node node, CoreContext context) {
      Object testValue = eval(node.get(0), context);
      if (context.isStopped()) {
         return null;
      } else {
         boolean taken = Terms.isTruthy(testValue);
         context.event(EventType.BRANCH, node, taken);
         return taken ? eval(node.get(2), context) : eval(node.get(4), context);
      }
   }

   private static Object evalMathAddExpr(Node node, CoreContext context) {
      return switch (node.get(1).token.type) {
         case PLUS -> {
            Object lhs = eval(node.get(0), context);
            yield context.isError() ? Terms.UNDEFINED : Terms.add(lhs, eval(node.get(2), context), context);
         }
         case MINUS -> Terms.min(lhsOperand(node, context), rhsOperand(node, context), context);
         default -> throw new RuntimeException("unexpected operator: " + node.get(1));
      };
   }

   private static Object evalMathMulExpr(Node node, CoreContext context) {
      return switch (node.get(1).token.type) {
         case STAR -> Terms.mul(lhsOperand(node, context), rhsOperand(node, context), context);
         case SLASH -> Terms.div(lhsOperand(node, context), rhsOperand(node, context), context);
         case PERCENT -> Terms.mod(lhsOperand(node, context), rhsOperand(node, context), context);
         default -> throw new RuntimeException("unexpected operator: " + node.get(1));
      };
   }

   private static Object evalMathPostExpr(Node node, CoreContext context) {
      boolean isIncrement = node.get(1).token.type == TokenType.PLUS_PLUS;
      return PropertyAccess.postIncDec(node.get(0), context, isIncrement);
   }

   private static Object evalMathPreExpr(Node node, CoreContext context) {
      Node exprNode = node.get(1).getFirst();

      return switch (node.get(0).token.type) {
         case PLUS -> {
            Object v = eval(exprNode, context);
            if (v instanceof BigInteger) {
               throw JsErrorException.typeError("Cannot convert a BigInt to a number using unary +");
            }

            yield Terms.toNumberCoerce(v, context);
         }
         case MINUS -> {
            Object v = eval(exprNode, context);
            yield v instanceof BigInteger bi ? Terms.narrowBigInt(bi.negate()) : Terms.mul(v, -1, context);
         }
         default -> throw new RuntimeException("unexpected operator: " + node.getFirst());
         case PLUS_PLUS -> PropertyAccess.preIncDec(exprNode, context, true);
         case MINUS_MINUS -> PropertyAccess.preIncDec(exprNode, context, false);
      };
   }

   private static Object evalProgram(Node node, CoreContext context) {
      if (!context.strict && hasUseStrictDirective(node)) {
         context.strict = true;
      }

      if (SlotTable.ENABLED && context.frame == null) {
         SlotTable table = SlotTable.forProgram(node);
         if (table != null) {
            context.frameTable = table;
            context.frame = table.newFrame();
         }
      }

      LexicalScope lexical = LexicalScope.compute(node);
      if (lexical != LexicalScope.NONE) {
         lexical.instantiateGlobal(context);
      }

      hoistFunctionDeclarations(node, context);
      Object progResult = null;
      boolean anyNonDecl = false;
      Object lastFnDecl = null;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (child.isEof()) {
            break;
         }

         if (isFunctionDeclarationStatement(child)) {
            String fnName = child.getFirst().get(1).getText();
            lastFnDecl = context.get(fnName);
         } else {
            progResult = eval(child, context);
            anyNonDecl = true;
            if (context.isError()) {
               throw errorAsException(context, child);
            }
         }
      }

      return anyNonDecl ? progResult : lastFnDecl;
   }

   static EngineException errorAsException(CoreContext context, Node node) {
      Object errorThrown = context.getErrorThrown();
      String errorMessage = null;
      String errorName = null;
      if (errorThrown instanceof JsObject jsError) {
         Object message = jsError.getMember("message");
         if (message instanceof String) {
            errorMessage = (String)message;
         }

         if (jsError.getMember("name") instanceof String s && !s.isEmpty()) {
            errorName = s;
         } else if (jsError.getMember("constructor") instanceof JsFunction ctor && ctor.getMember("name") instanceof String ctorName && !ctorName.isEmpty()) {
            errorName = ctorName;
         }
      }

      String jsMessage = errorMessage == null ? String.valueOf(errorThrown) : errorMessage;
      String rawMessage = jsMessage;
      if (errorName != null && !jsMessage.startsWith(errorName + ":")) {
         rawMessage = errorName + ": " + jsMessage;
      }

      String messagex = node.toStringError(rawMessage);
      boolean authored = context.isErrorAuthored();
      return new EngineException(messagex, null, errorName, jsMessage, authored, authored ? errorThrown : null, authored ? context.getErrorAuthoredLine() : 0);
   }

   private static Object evalRefExpr(Node node, CoreContext context) {
      if (node.getFirst().type == NodeType.FN_ARROW_EXPR) {
         return evalFnArrowExpr(node.getFirst(), context);
      } else {
         int slot = node.slot;
         if (slot >= 0) {
            Object[] frame = context.frame;
            if (frame != null) {
               Object v = frame[slot];
               if (v != SlotTable.UNDECLARED) {
                  if (v == SlotTable.TDZ) {
                     throw SlotTable.tdzError(node.getText());
                  }

                  return v;
               }
            }
         }

         return evalRefExprByName(node, context);
      }
   }

   private static Object evalRefExprByName(Node node, CoreContext context) {
      String varName = node.getText();
      if ("this".equals(varName)) {
         return context.getThisObject();
      } else {
         if (context.callArgs != null && "arguments".equals(varName)) {
            JsArray a = context.argumentsForRead();
            if (a != null) {
               return a;
            }
         }

         BindingSlot s = context.resolve(varName);
         if (s == null) {
            throw JsErrorException.referenceError(varName + " is not defined");
         } else {
            return context.readSlot(s, varName);
         }
      }
   }

   private static Object evalReturnStmt(Node node, CoreContext context) {
      if (node.size() > 1) {
         Object value = eval(node.get(1), context);
         return context.isError() ? value : context.stopAndReturn(value);
      } else {
         return context.stopAndReturn(Terms.UNDEFINED);
      }
   }

   private static Object evalStatement(Node node, CoreContext context) {
      node = node.getFirst();
      if (node.token.type == TokenType.SEMI) {
         return null;
      } else {
         context.event(EventType.STATEMENT_ENTER, node);
         RunInterceptor<Object> interceptor = (RunInterceptor<Object>)context.root.interceptor;
         DebugPointFactory<Object> pointFactory = null;
         Object point = null;
         if (interceptor != null && context.root.pointFactory != null) {
            DebugPointFactory<Object> factory = (DebugPointFactory<Object>)context.root.pointFactory;
            pointFactory = factory;
            Token first = node.getFirstToken();
            String sourcePath = first.getResource() != null ? first.getResource().getRelativePath() : null;
            point = factory.create(0, first.line, sourcePath, node, context);
            RunInterceptor.Action action = interceptor.beforeExecute(point);
            if (action == RunInterceptor.Action.SKIP) {
               context.event(EventType.STATEMENT_EXIT, node);
               return Terms.UNDEFINED;
            }

            if (action == RunInterceptor.Action.WAIT) {
               action = interceptor.waitForResume();
               if (action == RunInterceptor.Action.SKIP) {
                  context.event(EventType.STATEMENT_EXIT, node);
                  return Terms.UNDEFINED;
               }
            }
         }

         try {
            Object statementResult = eval(node, context);
            if (logger.isTraceEnabled() || Engine.DEBUG) {
               NodeType nodeType = node.type;
               if (nodeType != NodeType.EXPR && nodeType != NodeType.BLOCK) {
                  Token firstx = node.getFirstToken();
                  logger.trace("{}{} {} | {}", new Object[]{firstx.getResource(), firstx.getPositionDisplay(), statementResult, node});
                  if (Engine.DEBUG) {
                     System.out.println(firstx.getResource() + firstx.getPositionDisplay() + " " + statementResult + " | " + node);
                  }
               }
            }

            if (interceptor != null && pointFactory != null) {
               if (point == null) {
                  Token firstx = node.getFirstToken();
                  String sourcePathx = firstx.getResource() != null ? firstx.getResource().getRelativePath() : null;
                  point = pointFactory.create(0, firstx.line, sourcePathx, node, context);
               }

               interceptor.afterExecute(point, statementResult, null);
            }

            context.event(EventType.STATEMENT_EXIT, node);
            return statementResult;
         } catch (Exception var12) {
            if (interceptor != null && pointFactory != null) {
               if (point == null) {
                  Token firstx = node.getFirstToken();
                  String sourcePathx = firstx.getResource() != null ? firstx.getResource().getRelativePath() : null;
                  point = pointFactory.create(0, firstx.line, sourcePathx, node, context);
               }

               interceptor.afterExecute(point, null, var12);
            }

            if (context.root.listener != null) {
               Event event = new Event(EventType.STATEMENT_EXIT, context, node);
               ExitResult exitResult = context.root.listener.onError(event, var12);
               if (exitResult != null) {
                  Objects.requireNonNull(exitResult);
                  return exitResult.returnValue;
               }
            }

            if (var12 instanceof EngineException) {
               throw var12;
            } else if (var12 instanceof FlowControlSignal) {
               throw (RuntimeException)var12;
            } else {
               Token firstx = node.getFirstToken();
               String jsErrorName = null;
               String jsMessage = null;
               JsErrorException jex = findJsErrorException(var12);
               if (jex != null) {
                  Object nameVal = jex.payload.getMember("name");
                  Object msgVal = jex.payload.getMember("message");
                  jsErrorName = nameVal == null ? null : nameVal.toString();
                  jsMessage = msgVal != null && msgVal != Terms.UNDEFINED ? msgVal.toString() : null;
               }

               String body = var12.getMessage();
               if (body == null || body.isEmpty()) {
                  body = var12.getClass().getSimpleName();
               }

               StringBuilder sb = new StringBuilder();
               sb.append("js failed:\n");
               sb.append("==========\n");
               if (firstx.getResource().isFile()) {
                  sb.append("  File: ").append(firstx.getResource()).append("\n");
               }

               if (firstx.line != 0) {
                  sb.append("  Line: ").append(firstx.line + 1).append(", Col: ").append(firstx.col).append("\n");
               }

               sb.append("  Code: ").append(firstx.getLineText().trim()).append("\n");
               sb.append("  Error: ").append(body).append("\n");
               sb.append("==========");
               if (firstx.getResource().isFile()) {
                  System.err.println("file://" + firstx.getResource().getUri().getPath() + ":" + firstx.getPositionDisplay() + " " + var12);
               }

               throw new EngineException(sb.toString(), var12, jsErrorName, jsMessage);
            }
         }
      }
   }

   private static Object evalSwitchStmt(Node node, CoreContext context) {
      Object switchValue = eval(node.get(2), context);
      if (context.isStopped()) {
         return null;
      } else {
         List<Node> clauses = new ArrayList<>();
         int defaultAt = -1;
         int i = 0;

         for (int n = node.size(); i < n; i++) {
            Node child = node.get(i);
            if (!child.isToken()) {
               if (child.type == NodeType.DEFAULT_BLOCK) {
                  defaultAt = clauses.size();
                  clauses.add(child);
               } else if (child.type == NodeType.CASE_BLOCK) {
                  clauses.add(child);
               }
            }
         }

         context.enterScope(ContextScope.BLOCK, node);
         context.event(EventType.CONTEXT_ENTER, node);
         instantiateLexicalScope(node, context);
         Object result = null;

         try {
            for (Node clause : clauses) {
               hoistFunctionDeclarations(clause, context);
            }

            int enterAt = -1;

            for (int ix = 0; ix < clauses.size(); ix++) {
               if (ix != defaultAt) {
                  Node caseNode = clauses.get(ix);
                  Object caseValue = eval(caseNode.get(1), context);
                  if (context.isStopped()) {
                     return null;
                  }

                  boolean matched = Terms.strictEq(switchValue, caseValue);
                  context.event(EventType.BRANCH, caseNode, matched);
                  if (matched) {
                     enterAt = ix;
                     break;
                  }
               }
            }

            if (enterAt < 0) {
               enterAt = defaultAt;
            }

            if (enterAt < 0) {
               return null;
            } else {
               for (int ixx = enterAt; ixx < clauses.size(); ixx++) {
                  Node clause = clauses.get(ixx);
                  int j = clause.type == NodeType.CASE_BLOCK ? 3 : 2;

                  for (int m = clause.size(); j < m; j++) {
                     result = eval(clause.get(j), context);
                     if (context.isStopped()) {
                        return result;
                     }
                  }
               }

               return result;
            }
         } finally {
            context.event(EventType.CONTEXT_EXIT, node);
            context.exitScope();
            if (context.isBreaking() && targetsStatement(context, node)) {
               context.reset();
            }
         }
      }
   }

   private static Object evalThrowStmt(Node node, CoreContext context) {
      Object result = eval(node.get(1), context);
      return context.isError() ? result : context.stopAndThrowAuthored(result, node.getFirstToken().line + 1);
   }

   private static Object evalAwaitExpr(Node node, CoreContext context) {
      Object value = eval(node.get(1), context);
      if (context.isStopped()) {
         return value;
      } else {
         try {
            return AsyncSupport.await(value, context);
         } catch (AsyncSupport.AwaitRejection var4) {
            context.stopAndThrow(var4.reason);
            return Terms.UNDEFINED;
         }
      }
   }

   private static Object evalYieldExpr(Node node, CoreContext context) {
      GeneratorActivation act = GeneratorActivation.current();
      if (act == null) {
         throw JsErrorException.syntaxError("yield is only valid inside a generator function");
      } else {
         boolean star = node.size() > 1 && node.get(1).isToken() && node.get(1).token.type == TokenType.STAR;
         Node operandNode = null;
         if (star) {
            operandNode = node.get(2);
         } else if (node.size() > 1) {
            operandNode = node.get(1);
         }

         Object operand = operandNode == null ? Terms.UNDEFINED : eval(operandNode, context);
         if (context.isStopped()) {
            return operand;
         } else {
            return star ? evalYieldStar(operand, context, act) : applyResume(act.yieldAndReceive(operand), context);
         }
      }
   }

   private static Object applyResume(GeneratorActivation.ResumeSignal sig, CoreContext context) {
      return switch (sig.kind) {
         case NEXT -> sig.value;
         case THROW -> {
            context.stopAndThrow(sig.value);
            yield Terms.UNDEFINED;
         }
         case RETURN -> {
            context.stopAndReturn(sig.value);
            yield Terms.UNDEFINED;
         }
      };
   }

   private static Object evalYieldStar(Object operand, CoreContext context, GeneratorActivation act) {
      JsIterator probe = null;
      ObjectLike iterObj;
      if ((operand instanceof ObjectLike ol ? ol.getMember("@@iterator", ol, context) : null) instanceof JsCallable callable
         && operand instanceof ObjectLike receiver) {
         Object iter = callWith(callable, receiver, context);
         if (context.isStopped()) {
            return Terms.UNDEFINED;
         }

         if (!(iter instanceof ObjectLike io)) {
            throw JsErrorException.typeError("Result of the Symbol.iterator method is not an object");
         }

         iterObj = io;
      } else {
         probe = IterUtils.getIterator(operand, context);
         iterObj = IterUtils.toIteratorObject(probe);
      }

      GeneratorActivation.ResumeKind kind = GeneratorActivation.ResumeKind.NEXT;
      Object sent = Terms.UNDEFINED;

      while (true) {
         Object step;
         switch (kind) {
            case NEXT:
               step = invokeIteratorMethod(iterObj, "next", sent, context, true);
               if (context.isStopped()) {
                  return Terms.UNDEFINED;
               }
               break;
            case THROW:
               Object throwFn = iterObj.getMember("throw", iterObj, context);
               if (context.isStopped()) {
                  return Terms.UNDEFINED;
               }

               if (throwFn == null || throwFn == Terms.UNDEFINED) {
                  Object closed = invokeIteratorMethod(iterObj, "return", null, context, false);
                  if (context.isStopped()) {
                     return Terms.UNDEFINED;
                  }

                  if (closed != null && !(closed instanceof ObjectLike)) {
                     throw JsErrorException.typeError("iterator result is not an object");
                  }

                  throw JsErrorException.typeError("The iterator does not provide a 'throw' method");
               }

               if (!(throwFn instanceof JsCallable throwCallable)) {
                  throw JsErrorException.typeError("iterator.throw is not a function");
               }

               step = invokeIteratorCallable(iterObj, throwCallable, sent, context);
               if (context.isStopped()) {
                  return Terms.UNDEFINED;
               }

               if (!(step instanceof ObjectLike)) {
                  throw JsErrorException.typeError("iterator result is not an object");
               }
               break;
            case RETURN:
               Object returned = invokeIteratorMethod(iterObj, "return", sent, context, false);
               if (context.isStopped()) {
                  return Terms.UNDEFINED;
               }

               if (returned == null) {
                  context.stopAndReturn(sent);
                  return Terms.UNDEFINED;
               }

               if (!(returned instanceof ObjectLike)) {
                  throw JsErrorException.typeError("iterator result is not an object");
               }

               step = returned;
               break;
            default:
               throw new IllegalStateException();
         }

         ObjectLike stepObj = (ObjectLike)step;
         boolean done = Terms.isTruthy(stepObj.getMember("done", stepObj, context));
         if (context.isStopped()) {
            return Terms.UNDEFINED;
         }

         Object value = stepObj.getMember("value", stepObj, context);
         if (context.isStopped()) {
            return Terms.UNDEFINED;
         }

         if (value == null) {
            value = Terms.UNDEFINED;
         }

         if (done) {
            if (kind == GeneratorActivation.ResumeKind.RETURN) {
               context.stopAndReturn(value);
               return Terms.UNDEFINED;
            }

            return value;
         }

         GeneratorActivation.ResumeSignal sig = act.yieldAndReceive(value);
         kind = sig.kind;
         sent = sig.value;
      }
   }

   private static Object invokeIteratorMethod(ObjectLike iterObj, String name, Object arg, CoreContext context, boolean required) {
      Object fn = iterObj.getMember(name, iterObj, context);
      if (context.isStopped()) {
         return Terms.UNDEFINED;
      } else if (fn != null && fn != Terms.UNDEFINED) {
         if (fn instanceof JsCallable callable) {
            Object result = invokeIteratorCallable(iterObj, callable, arg, context);
            if (!context.isStopped() && required && !(result instanceof ObjectLike)) {
               throw JsErrorException.typeError("iterator result is not an object");
            } else {
               return result;
            }
         } else {
            throw JsErrorException.typeError("iterator." + name + " is not a function");
         }
      } else if (required) {
         throw JsErrorException.typeError("iterator." + name + " is not a function");
      } else {
         return null;
      }
   }

   private static Object invokeIteratorCallable(ObjectLike iterObj, JsCallable callable, Object arg, CoreContext context) {
      Object savedThis = context.thisObject;
      context.thisObject = iterObj;

      Object var5;
      try {
         var5 = callable.call(context, arg == null ? new Object[0] : new Object[]{arg});
      } finally {
         context.thisObject = savedThis;
      }

      return var5;
   }

   private static Object callWith(JsCallable callable, ObjectLike receiver, CoreContext context) {
      Object savedThis = context.thisObject;
      context.thisObject = receiver;

      Object var4;
      try {
         var4 = callable.call(context, new Object[0]);
      } finally {
         context.thisObject = savedThis;
      }

      return var4;
   }

   private static JsErrorException findJsErrorException(Throwable t) {
      Throwable cur = t;

      while (cur != null) {
         if (cur instanceof JsErrorException jex) {
            return jex;
         }

         Throwable next = cur.getCause();
         if (next == null || next == cur) {
            return null;
         }

         cur = next;
      }

      return null;
   }

   static JsError buildCaughtError(Throwable e) {
      JsErrorException jex = findJsErrorException(e);
      if (jex != null) {
         return jex.payload;
      } else {
         Throwable cause = e;

         while (cause instanceof EngineException && cause.getCause() != null) {
            cause = cause.getCause();
         }

         return JsErrorException.wrap(cause).payload;
      }
   }

   private static Object evalTryStmt(Node node, CoreContext context) {
      Object tryValue;
      try {
         tryValue = eval(node.get(1), context);
      } catch (RuntimeException var23) {
         if (var23 instanceof FlowControlSignal) {
            throw var23;
         }

         if (var23 instanceof EngineInterruptedException) {
            throw var23;
         }

         JsError errObj = buildCaughtError(var23);
         errObj.captureStack(context);
         context.stopAndThrow(errObj);
         tryValue = null;
      }

      Node finallyBlock = null;
      RuntimeException pending = null;
      if (node.get(2).token.type == TokenType.CATCH) {
         boolean hasParam = node.get(3).token.type == TokenType.L_PAREN;
         int catchBlockAt = hasParam ? 6 : 3;
         if (node.size() > catchBlockAt + 1) {
            finallyBlock = node.get(catchBlockAt + 2);
         }

         if (context.isError()) {
            Object errorThrown = context.getErrorThrown();
            context.reset();
            context.enterScope(ContextScope.CATCH, node);
            context.event(EventType.CONTEXT_ENTER, node);

            try {
               if (hasParam) {
                  Node param = node.get(4);
                  if (param.type != NodeType.LIT_ARRAY && param.type != NodeType.LIT_OBJECT) {
                     context.put(param.getText(), errorThrown);
                  } else {
                     destructurePattern(param, context, BindScope.VAR, errorThrown, true);
                  }
               }

               try {
                  tryValue = eval(node.get(catchBlockAt), context);
               } catch (RuntimeException var21) {
                  if (finallyBlock == null || var21 instanceof FlowControlSignal || var21 instanceof EngineInterruptedException) {
                     throw var21;
                  }

                  pending = var21;
                  tryValue = null;
               }

               if (context.isError()) {
                  tryValue = null;
               }
            } finally {
               context.event(EventType.CONTEXT_EXIT, node);
               context.exitScope();
            }
         }
      } else if (node.get(2).token.type == TokenType.FINALLY) {
         finallyBlock = node.get(3);
      }

      if (finallyBlock != null) {
         ExitType savedExit = context.getExitType();
         String savedLabel = context.getExitLabel();
         Object savedReturn = context.getReturnValue();
         Object savedError = context.getErrorThrown();
         boolean savedAuthored = context.isErrorAuthored();
         int savedAuthoredLine = context.getErrorAuthoredLine();
         if (savedExit != null) {
            context.reset();
         }

         context.enterScope(ContextScope.BLOCK, node);
         context.event(EventType.CONTEXT_ENTER, node);

         try {
            eval(finallyBlock, context);
         } finally {
            context.event(EventType.CONTEXT_EXIT, node);
            context.exitScope();
         }

         if (context.getExitType() == null) {
            if (pending != null) {
               throw pending;
            }

            if (savedExit != null) {
               context.restoreCompletion(savedExit, savedLabel, savedReturn, savedError, savedAuthored, savedAuthoredLine);
            }
         }
      }

      return tryValue;
   }

   private static Object evalTypeofExpr(Node node, CoreContext context) {
      try {
         Object value = eval(node.get(1), context);
         return Terms.typeOf(value);
      } catch (JsErrorException.Uninitialized var3) {
         throw var3;
      } catch (Exception var4) {
         return Terms.UNDEFINED.toString();
      }
   }

   private static Object evalUnaryExpr(Node node, CoreContext context) {
      Object unaryValue = eval(node.get(1), context);

      return switch (node.getFirst().token.type) {
         case NOT -> !Terms.isTruthy(unaryValue);
         case TILDE -> Terms.bitNot(unaryValue, context);
         case VOID -> Terms.UNDEFINED;
         default -> throw new RuntimeException("unexpected operator: " + node.getFirst());
      };
   }

   private static Object evalVarStmt(Node node, CoreContext context) {
      BindScope bindScope = switch (node.getFirstToken().type) {
         case LET -> BindScope.LET;
         case CONST -> BindScope.CONST;
         default -> BindScope.VAR;
      };
      Object lastValue = Terms.UNDEFINED;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (child.type == NodeType.VAR_DECL) {
            Node binding = child.getFirst();
            Object value;
            boolean initialized;
            if (child.size() > 2) {
               value = eval(child.get(2), context);
               initialized = true;
            } else {
               value = Terms.UNDEFINED;
               initialized = bindScope != BindScope.CONST;
            }

            evalAssign(binding, context, bindScope, value, initialized);
            lastValue = value;
         }
      }

      return lastValue;
   }

   private static Object evalWhileStmt(Node node, CoreContext context) {
      context.enterScope(ContextScope.LOOP_INIT, node);
      context.event(EventType.CONTEXT_ENTER, node);
      Node whileBody = node.getLast();
      Node whileExpr = node.get(2);
      Object whileResult = null;

      try {
         while (true) {
            checkInterrupted();
            Object whileCondition = eval(whileExpr, context);
            if (context.isStopped() || !Terms.isTruthy(whileCondition)) {
               break;
            }

            whileResult = eval(whileBody, context);
            if (context.isStopped()) {
               if (!context.isContinuing() || !targetsStatement(context, node)) {
                  break;
               }

               context.reset();
            }
         }
      } finally {
         if (context.isBreaking() && targetsStatement(context, node)) {
            context.reset();
         }

         context.event(EventType.CONTEXT_EXIT, node);
         context.exitScope();
      }

      return whileResult;
   }

   private static Object evalDoWhileStmt(Node node, CoreContext context) {
      context.enterScope(ContextScope.LOOP_INIT, node);
      context.event(EventType.CONTEXT_ENTER, node);
      Node doBody = node.get(1);
      Node doExpr = node.get(4);
      Object doResult = null;

      Object doCondition;
      try {
         do {
            checkInterrupted();
            doResult = eval(doBody, context);
            if (context.isStopped()) {
               if (!context.isContinuing() || !targetsStatement(context, node)) {
                  break;
               }

               context.reset();
            }

            doCondition = eval(doExpr, context);
         } while (!context.isStopped() && Terms.isTruthy(doCondition));
      } finally {
         if (context.isBreaking() && targetsStatement(context, node)) {
            context.reset();
         }

         context.event(EventType.CONTEXT_EXIT, node);
         context.exitScope();
      }

      return doResult;
   }

   static Object eval(Node node, CoreContext context) {
      return switch (node.type) {
         case ASSIGN_EXPR -> evalAssignExpr(node, context);
         case BLOCK -> evalBlock(node, context);
         case BREAK_STMT -> evalBreakStmt(node, context);
         case CONTINUE_STMT -> evalContinueStmt(node, context);
         case LABELLED_STMT -> evalLabelledStmt(node, context);
         case DELETE_EXPR -> evalDeleteExpr(node, context);
         case EXPR -> evalExpr(node, context);
         case EXPR_LIST -> evalExprList(node, context);
         case LIT_EXPR -> evalLitExpr(node, context);
         case FN_EXPR -> evalFnExpr(node, context);
         case FN_ARROW_EXPR -> evalFnArrowExpr(node, context);
         case FN_CALL_EXPR -> chainStepResult(evalFnCall(node, context, false), node);
         case FN_TAGGED_TEMPLATE_EXPR -> chainStepResult(evalFnTaggedTemplate(node, context), node);
         case FOR_STMT -> evalForStmt(node, context);
         case IF_STMT -> evalIfStmt(node, context);
         case INSTANCEOF_EXPR -> evalInstanceOfExpr(node, context);
         case IN_EXPR -> evalInExpr(node, context);
         case PRIVATE_NAME_EXPR -> throw JsErrorException.syntaxError("unexpected private name " + node.getText());
         case LOGIC_EXPR -> evalLogicExpr(node, context);
         case LOGIC_AND_EXPR -> evalLogicAndExpr(node, context);
         case LOGIC_NULLISH_EXPR -> evalLogicNullishExpr(node, context);
         case LOGIC_BIT_EXPR -> evalLogicBitExpr(node, context);
         case LOGIC_TERN_EXPR -> evalLogicTernExpr(node, context);
         case MATH_ADD_EXPR -> evalMathAddExpr(node, context);
         case MATH_EXP_EXPR -> Terms.exp(lhsOperand(node, context), rhsOperand(node, context), context);
         case MATH_MUL_EXPR -> evalMathMulExpr(node, context);
         case MATH_POST_EXPR -> evalMathPostExpr(node, context);
         case MATH_PRE_EXPR -> evalMathPreExpr(node, context);
         case NEW_EXPR -> evalNewExpr(node, context);
         case PAREN_EXPR -> eval(node.get(1), context);
         case CLASS_EXPR -> evalClassExpr(node, context);
         case SUPER_EXPR -> evalSuperBase(context);
         case PROGRAM -> evalProgram(node, context);
         case REF_EXPR -> evalRefExpr(node, context);
         case REF_BRACKET_EXPR -> chainStepResult(PropertyAccess.get(node, context), node);
         case REF_DOT_EXPR -> {
            Node second = node.size() > 1 ? node.get(1) : null;
            Object result = second != null && second.type == NodeType.FN_CALL_EXPR ? evalOptionalCall(node, context) : PropertyAccess.get(node, context);
            yield chainStepResult(result, node);
         }
         case RETURN_STMT -> evalReturnStmt(node, context);
         case STATEMENT -> evalStatement(node, context);
         case SWITCH_STMT -> evalSwitchStmt(node, context);
         case THROW_STMT -> evalThrowStmt(node, context);
         case TRY_STMT -> evalTryStmt(node, context);
         case TYPEOF_EXPR -> evalTypeofExpr(node, context);
         case UNARY_EXPR -> evalUnaryExpr(node, context);
         case AWAIT_EXPR -> evalAwaitExpr(node, context);
         case YIELD_EXPR -> evalYieldExpr(node, context);
         case VAR_STMT -> evalVarStmt(node, context);
         case WHILE_STMT -> evalWhileStmt(node, context);
         case DO_WHILE_STMT -> evalDoWhileStmt(node, context);
         default -> throw new RuntimeException(node.toStringError("eval - unexpected node"));
      };
   }

   private static final class ClassMember {
      boolean isStatic;
      boolean isAccessor;
      boolean isField;
      boolean isPrivate;
      PrivateName privateName;
      String kind;
      boolean computed;
      Node keyExprNode;
      String simpleKey;
      Node fnExpr;
      Node initExpr;
      Node staticBlock;
      String resolvedKey;
      JsSymbol resolvedSymbol;
   }
}
