package io.karatelabs.js;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyAccess {
   static final Logger logger = LoggerFactory.getLogger(PropertyAccess.class);
   private static final JsObject PROTOTYPE_PROBE = new JsObject();
   static final Object SHORT_CIRCUITED = new Object() {
      @Override
      public String toString() {
         return "<<short-circuited>>";
      }
   };
   private static final PropertyAccess.AccessSite SHORT_CIRCUIT_SITE = new PropertyAccess.AccessSite(null, null, false);

   private PropertyAccess() {
   }

   static boolean isChainRoot(Node node) {
      Node parent = node.getParent();
      if (parent == null) {
         return true;
      } else {
         return parent.type != NodeType.REF_DOT_EXPR
               && parent.type != NodeType.REF_BRACKET_EXPR
               && parent.type != NodeType.FN_CALL_EXPR
               && parent.type != NodeType.FN_TAGGED_TEMPLATE_EXPR
            ? true
            : parent.size() == 0 || parent.getFirst() != node;
      }
   }

   private static PropertyAccess.AccessSite resolveWriteSite(Node node, CoreContext context) {
      Object superReceiver = node.getFirst().type == NodeType.SUPER_EXPR ? context.thisObject : null;
      if (node.type == NodeType.REF_DOT_EXPR) {
         Node second = node.get(1);
         if (second.isToken()) {
            Node key = node.get(2);
            Object target = Interpreter.eval(node.getFirst(), context);
            if (target == SHORT_CIRCUITED) {
               return SHORT_CIRCUIT_SITE;
            } else if (context.isStopped()) {
               return null;
            } else {
               return key.token.type == TokenType.PRIVATE_NAME
                  ? new PropertyAccess.AccessSite(target, null, false, PrivateAccess.resolve(key.getText(), context))
                  : new PropertyAccess.AccessSite(target, key.getText(), false, null, superReceiver);
            }
         } else if (second.type == NodeType.REF_BRACKET_EXPR) {
            Object target = Interpreter.eval(node.getFirst(), context);
            if (target == SHORT_CIRCUITED) {
               return SHORT_CIRCUIT_SITE;
            } else if (context.isStopped()) {
               return null;
            } else {
               Object index = Interpreter.eval(second.get(2), context);
               return context.isStopped() ? null : new PropertyAccess.AccessSite(target, index, true, null, superReceiver);
            }
         } else {
            throw JsErrorException.typeError("cannot write to optional call expression");
         }
      } else {
         Object target = Interpreter.eval(node.getFirst(), context);
         if (target == SHORT_CIRCUITED) {
            return SHORT_CIRCUIT_SITE;
         } else if (context.isStopped()) {
            return null;
         } else {
            Object index = Interpreter.eval(node.get(2), context);
            return context.isStopped() ? null : new PropertyAccess.AccessSite(target, index, true, null, superReceiver);
         }
      }
   }

   private static Object siteRead(PropertyAccess.AccessSite site, CoreContext context) {
      if (site.receiver == null) {
         return site.isIndex ? getByIndex(site.target, site.key, false, context, false) : getByName(site.target, (String)site.key, false, context, false);
      } else {
         return site.isIndex && JsSymbol.keyedBy(site.key) != null
            ? getByIndex(site.receiver, site.key, false, context, false)
            : getByName(site.target, site.isIndex ? Terms.toPropertyKey(site.key) : (String)site.key, false, context, false, site.receiver);
      }
   }

   private static void siteWrite(PropertyAccess.AccessSite site, Object value, CoreContext context, Node trackingNode) {
      if (site.receiver == null) {
         if (site.isIndex) {
            setByIndex(site.target, site.key, value, context, trackingNode);
         } else {
            setByName(site.target, (String)site.key, value, context, trackingNode);
         }
      } else if (site.isIndex && JsSymbol.keyedBy(site.key) != null) {
         setByIndex(site.receiver, site.key, value, context, trackingNode);
      } else {
         setByName(site.target, site.isIndex ? Terms.toPropertyKey(site.key) : (String)site.key, value, context, trackingNode, site.receiver);
      }
   }

   static Object get(Node node, CoreContext context) {
      return switch (node.type) {
         case REF_EXPR -> getRefExpr(node, context, false);
         case REF_DOT_EXPR -> getRefDotExpr(node, context, false);
         case REF_BRACKET_EXPR -> getRefBracketExpr(node, context, false);
         case LIT_EXPR -> Interpreter.eval(node, context);
         case PAREN_EXPR -> Interpreter.eval(node.get(1), context);
         case FN_CALL_EXPR -> Interpreter.eval(node, context);
         default -> throw JsErrorException.typeError("cannot get from: " + node);
      };
   }

   static Object getCallable(Node node, CoreContext context) {
      return switch (node.type) {
         case REF_EXPR -> {
            Object c = getRefExpr(node, context, true);
            context.callReceiver = null;
            yield c;
         }
         case REF_DOT_EXPR -> getCallableRefDotExpr(node, context);
         case REF_BRACKET_EXPR -> getCallableRefBracketExpr(node, context);
         default -> throw JsErrorException.notAFunction(node);
         case PAREN_EXPR -> getCallableParenInner(node, context);
         case FN_CALL_EXPR, FN_TAGGED_TEMPLATE_EXPR, FN_EXPR, FN_ARROW_EXPR -> {
            Object c = Interpreter.eval(node, context);
            context.callReceiver = null;
            yield c;
         }
      };
   }

   static void set(Node node, CoreContext context, Object value) {
      set(node, context, value, null);
   }

   static void set(Node node, CoreContext context, Object value, Node trackingNode) {
      switch (node.type) {
         case REF_EXPR:
            int slot = node.slot;
            if (slot >= 0) {
               Object[] frame = context.frame;
               if (context.frame != null && frame[slot] != SlotTable.UNDECLARED) {
                  context.updateSlot(slot, value, trackingNode);
                  break;
               }
            }

            context.update(node.getText(), value, trackingNode);
            break;
         case REF_DOT_EXPR:
         case REF_BRACKET_EXPR:
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            if (site == null || site == SHORT_CIRCUIT_SITE) {
               return;
            }

            if (site.privateName != null) {
               PrivateAccess.set(site.target, site.privateName, value, context);
            } else {
               siteWrite(site, value, context, trackingNode);
            }
            break;
         default:
            throw JsErrorException.typeError("cannot set on: " + node);
      }
   }

   static Object assign(Node node, CoreContext context, Node rhsNode, Node trackingNode) {
      return switch (node.type) {
         case REF_EXPR -> {
            Object value = Interpreter.eval(rhsNode, context);
            if (context.isStopped()) {
               yield value;
            } else {
               set(node, context, value, trackingNode);
               yield value;
            }
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            if (context.isStopped()) {
               yield Terms.UNDEFINED;
            } else {
               Object value = Interpreter.eval(rhsNode, context);
               if (site != null && site != SHORT_CIRCUIT_SITE && !context.isStopped()) {
                  if (site.target == null && node.getFirst().type == NodeType.SUPER_EXPR) {
                     throw JsErrorException.typeError("cannot set property on null 'super' base");
                  }

                  if (site.privateName != null) {
                     PrivateAccess.set(site.target, site.privateName, value, context);
                  } else {
                     siteWrite(site, value, context, trackingNode);
                  }

                  yield value;
               } else {
                  yield value;
               }
            }
         }
         default -> throw JsErrorException.typeError("cannot set on: " + node);
      };
   }

   static Object compound(Node node, CoreContext context, TokenType operator, Node rhsNode, Node trackingNode) {
      return switch (node.type) {
         case REF_EXPR -> {
            int slot = node.slot;
            if (slot >= 0) {
               Object[] frame = context.frame;
               if (context.frame != null && frame[slot] != SlotTable.UNDECLARED) {
                  Object oldValue = frame[slot];
                  if (oldValue == SlotTable.TDZ) {
                     throw SlotTable.tdzError(node.getText());
                  }

                  Object operand = Interpreter.eval(rhsNode, context);
                  if (context.isStopped()) {
                     yield Terms.UNDEFINED;
                  } else {
                     Object newValue = applyOperator(oldValue, operator, operand, context);
                     context.updateSlot(slot, newValue, trackingNode);
                     yield newValue;
                  }
               }
            }

            yield compoundRefByName(node, context, operator, rhsNode, trackingNode);
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            if (site != null && site != SHORT_CIRCUIT_SITE && !context.isStopped()) {
               Object oldValue = site.privateName != null ? PrivateAccess.get(site.target, site.privateName, context) : siteRead(site, context);
               if (context.isStopped()) {
                  yield Terms.UNDEFINED;
               } else {
                  Object operand = Interpreter.eval(rhsNode, context);
                  if (context.isStopped()) {
                     yield Terms.UNDEFINED;
                  } else {
                     Object newValue = applyOperator(oldValue, operator, operand, context);
                     if (context.isStopped()) {
                        yield Terms.UNDEFINED;
                     } else {
                        if (site.privateName != null) {
                           PrivateAccess.set(site.target, site.privateName, newValue, context);
                        } else {
                           siteWrite(site, newValue, context, trackingNode);
                        }

                        yield newValue;
                     }
                  }
               }
            } else {
               yield Terms.UNDEFINED;
            }
         }
         default -> throw JsErrorException.typeError("cannot apply compound assignment to: " + node);
      };
   }

   static Object logicalCompound(Node node, CoreContext context, TokenType operator, Node rhsNode, Node trackingNode) {
      return switch (node.type) {
         case REF_EXPR -> {
            int slot = node.slot;
            if (slot >= 0) {
               Object[] frame = context.frame;
               if (context.frame != null && frame[slot] != SlotTable.UNDECLARED) {
                  Object oldValue = frame[slot];
                  if (oldValue == SlotTable.TDZ) {
                     throw SlotTable.tdzError(node.getText());
                  }

                  if (!shouldLogicalAssign(operator, oldValue)) {
                     yield oldValue;
                  } else {
                     Object newValue = Interpreter.eval(rhsNode, context);
                     if (context.isStopped()) {
                        yield null;
                     } else {
                        context.updateSlot(slot, newValue, trackingNode);
                        yield newValue;
                     }
                  }
               }
            }

            yield logicalCompoundRefByName(node, context, operator, rhsNode, trackingNode);
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            if (site != null && site != SHORT_CIRCUIT_SITE) {
               if (context.isStopped()) {
                  yield null;
               }

               if (site.target != null && site.target != Terms.UNDEFINED) {
                  if (site.privateName != null) {
                     Object oldValue = PrivateAccess.get(site.target, site.privateName, context);
                     if (!shouldLogicalAssign(operator, oldValue)) {
                        yield oldValue;
                     } else {
                        Object newValue = Interpreter.eval(rhsNode, context);
                        if (context.isStopped()) {
                           yield null;
                        } else {
                           PrivateAccess.set(site.target, site.privateName, newValue, context);
                           yield newValue;
                        }
                     }
                  } else if (site.receiver != null) {
                     Object oldValue = siteRead(site, context);
                     if (context.isStopped()) {
                        yield null;
                     } else if (!shouldLogicalAssign(operator, oldValue)) {
                        yield oldValue;
                     } else {
                        Object newValue = Interpreter.eval(rhsNode, context);
                        if (context.isStopped()) {
                           yield null;
                        } else {
                           siteWrite(site, newValue, context, trackingNode);
                           yield newValue;
                        }
                     }
                  } else {
                     yield site.isIndex
                        ? logicalCompoundByIndex(site.target, site.key, operator, rhsNode, context, trackingNode)
                        : logicalCompoundByName(site.target, (String)site.key, operator, rhsNode, context, trackingNode);
                  }
               }

               throw JsErrorException.typeError("cannot read properties of " + (site.target == null ? "null" : "undefined"));
            }

            yield Terms.UNDEFINED;
         }
         default -> throw JsErrorException.typeError("cannot apply logical-assignment to: " + node);
      };
   }

   private static Object compoundRefByName(Node node, CoreContext context, TokenType operator, Node rhsNode, Node trackingNode) {
      String name = node.getText();
      Object oldValue = context.get(name);
      if (context.isStopped()) {
         return Terms.UNDEFINED;
      } else {
         Object operand = Interpreter.eval(rhsNode, context);
         if (context.isStopped()) {
            return Terms.UNDEFINED;
         } else {
            Object newValue = applyOperator(oldValue, operator, operand, context);
            context.update(name, newValue, trackingNode);
            return newValue;
         }
      }
   }

   private static Object logicalCompoundRefByName(Node node, CoreContext context, TokenType operator, Node rhsNode, Node trackingNode) {
      String name = node.getText();
      Object oldValue = context.get(name);
      if (!shouldLogicalAssign(operator, oldValue)) {
         return oldValue;
      } else {
         Object newValue = Interpreter.eval(rhsNode, context);
         if (context.isStopped()) {
            return null;
         } else {
            context.update(name, newValue, trackingNode);
            return newValue;
         }
      }
   }

   private static Object incDecRefByName(Node node, CoreContext context, boolean isIncrement, boolean returnNew) {
      String name = node.getText();
      Object oldValue = context.get(name);
      Object step = Terms.incDecStep(oldValue);
      Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
      context.update(name, newValue);
      return returnNew ? newValue : oldValue;
   }

   private static boolean shouldLogicalAssign(TokenType operator, Object lhsValue) {
      return switch (operator) {
         case PIPE_PIPE_EQ -> !Terms.isTruthy(lhsValue);
         case AMP_AMP_EQ -> Terms.isTruthy(lhsValue);
         case QUES_QUES_EQ -> lhsValue == null || lhsValue == Terms.UNDEFINED;
         default -> throw new RuntimeException("unexpected logical-assignment operator: " + operator);
      };
   }

   private static Object logicalCompoundByIndex(Object object, Object index, TokenType operator, Node rhsNode, CoreContext context, Node trackingNode) {
      if (index instanceof Number n) {
         int i = denseIndex(n);
         if (object instanceof List && i >= 0) {
            List<Object> list = (List<Object>)object;
            Object oldValue = i < list.size() ? list.get(i) : Terms.UNDEFINED;
            if (!shouldLogicalAssign(operator, oldValue)) {
               return oldValue;
            }

            Object newValue = Interpreter.eval(rhsNode, context);
            if (context.isStopped()) {
               return null;
            }

            if (object instanceof JsArray ja) {
               ja.checkDensePad(i);
            }

            while (list.size() <= i) {
               list.add(Terms.UNDEFINED);
            }

            list.set(i, newValue);
            firePropertySet(context, String.valueOf(i), newValue, oldValue, object, trackingNode);
            return newValue;
         }
      }

      return logicalCompoundByName(object, Terms.toPropertyKey(index), operator, rhsNode, context, trackingNode);
   }

   private static Object logicalCompoundByName(Object object, String name, TokenType operator, Node rhsNode, CoreContext context, Node trackingNode) {
      Object oldValue = getByName(object, name, false, context, false);
      if (!shouldLogicalAssign(operator, oldValue)) {
         return oldValue;
      } else {
         Object newValue = Interpreter.eval(rhsNode, context);
         if (context.isStopped()) {
            return null;
         } else {
            setByName(object, name, newValue, context, trackingNode);
            return newValue;
         }
      }
   }

   static Object postIncDec(Node node, CoreContext context, boolean isIncrement) {
      return switch (node.type) {
         case REF_EXPR -> {
            int slot = node.slot;
            if (slot >= 0) {
               Object[] frame = context.frame;
               if (context.frame != null && frame[slot] != SlotTable.UNDECLARED) {
                  Object oldValue = frame[slot];
                  if (oldValue == SlotTable.TDZ) {
                     throw SlotTable.tdzError(node.getText());
                  }

                  Object step = Terms.incDecStep(oldValue);
                  Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
                  context.updateSlot(slot, newValue, null);
                  yield oldValue;
               }
            }

            yield incDecRefByName(node, context, isIncrement, false);
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            yield site == null || site == SHORT_CIRCUIT_SITE
               ? Terms.UNDEFINED
               : (
                  site.privateName != null
                     ? privateIncDec(site, isIncrement, false, context)
                     : (
                        site.receiver != null
                           ? superIncDec(site, isIncrement, false, context)
                           : (
                              site.isIndex
                                 ? postIncDecByIndex(site.target, site.key, isIncrement, context)
                                 : postIncDecByName(site.target, (String)site.key, isIncrement, context)
                           )
                     )
               );
         }
         case LIT_EXPR -> {
            Object oldValue = Interpreter.eval(node, context);
            yield oldValue;
         }
         default -> throw JsErrorException.typeError("cannot apply post inc/dec to: " + node);
      };
   }

   static Object preIncDec(Node node, CoreContext context, boolean isIncrement) {
      return switch (node.type) {
         case REF_EXPR -> {
            int slot = node.slot;
            if (slot >= 0) {
               Object[] frame = context.frame;
               if (context.frame != null && frame[slot] != SlotTable.UNDECLARED) {
                  Object oldValue = frame[slot];
                  if (oldValue == SlotTable.TDZ) {
                     throw SlotTable.tdzError(node.getText());
                  }

                  Object step = Terms.incDecStep(oldValue);
                  Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
                  context.updateSlot(slot, newValue, null);
                  yield newValue;
               }
            }

            yield incDecRefByName(node, context, isIncrement, true);
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site = resolveWriteSite(node, context);
            yield site == null || site == SHORT_CIRCUIT_SITE
               ? Terms.UNDEFINED
               : (
                  site.privateName != null
                     ? privateIncDec(site, isIncrement, true, context)
                     : (
                        site.receiver != null
                           ? superIncDec(site, isIncrement, true, context)
                           : (
                              site.isIndex
                                 ? preIncDecByIndex(site.target, site.key, isIncrement, context)
                                 : preIncDecByName(site.target, (String)site.key, isIncrement, context)
                           )
                     )
               );
         }
         default -> throw JsErrorException.typeError("cannot apply pre inc/dec to: " + node);
      };
   }

   static boolean delete(Node node, CoreContext context) {
      return switch (node.type) {
         case REF_EXPR -> {
            boolean var13 = false;
            yield var13;
         }
         case REF_DOT_EXPR, REF_BRACKET_EXPR -> {
            PropertyAccess.AccessSite site;
            try {
               site = resolveWriteSite(node, context);
            } catch (JsErrorException var7) {
               boolean var8 = false;
               yield var8;
            }

            if (site == SHORT_CIRCUIT_SITE) {
               boolean var9 = true;
               yield var9;
            } else if (site == null) {
               boolean var10 = false;
               yield var10;
            } else {
               JsSymbol sym = JsSymbol.keyedBy(site.key);
               if (sym != null) {
                  boolean var11 = !(site.target instanceof JsObject jo && !jo.removeSymbol(sym, context.strict));
                  yield var11;
               } else {
                  boolean var12 = deleteByKey(site.target, Terms.toPropertyKey(site.key), context, node);
                  yield var12;
               }
            }
         }
         default -> {
            boolean var2 = false;
            yield var2;
         }
      };
   }

   private static Object getRefExpr(Node node, CoreContext context, boolean functionCall) {
      int slot = node.slot;
      if (slot >= 0) {
         Object[] frame = context.frame;
         if (frame != null) {
            Object v = frame[slot];
            if (v != SlotTable.UNDECLARED) {
               if (v == SlotTable.TDZ) {
                  throw SlotTable.tdzError(node.getText());
               }

               if (functionCall && context.root.bridge != null && v instanceof ExternalAccess ea) {
                  return externalConstructor(ea);
               }

               return v;
            }
         }
      }

      return getRefExprByName(node, context, functionCall);
   }

   private static Object getRefExprByName(Node node, CoreContext context, boolean functionCall) {
      String name = node.getText();
      Object result = context.getOrNotFound(name);
      if (result == CoreContext.NOT_FOUND) {
         throw JsErrorException.referenceError(name + " is not defined");
      } else {
         return functionCall && context.root.bridge != null && result instanceof ExternalAccess ea ? externalConstructor(ea) : result;
      }
   }

   static JsConstructor externalConstructor(ExternalAccess ea) {
      return (c, args) -> ea.construct(args);
   }

   private static Object resolveRefDot(Node node, CoreContext context, boolean functionCall, boolean wantReceiver) {
      if (node.getFirst().type == NodeType.SUPER_EXPR && node.get(1).type == NodeType.TOKEN) {
         ObjectLike superProto = Interpreter.evalSuperBase(context);
         Object v = getByName(superProto, node.get(2).getText(), false, context, functionCall, context.thisObject);
         if (wantReceiver) {
            context.callReceiver = context.thisObject;
         }

         return v;
      } else {
         String name;
         Object object;
         boolean optional;
         if (node.get(1).type == NodeType.TOKEN) {
            optional = node.get(1).token.type == TokenType.QUES_DOT;
            Node keyNode = node.get(2);
            if (keyNode.token.type == TokenType.PRIVATE_NAME) {
               PrivateName pn = PrivateAccess.resolve(keyNode.getText(), context);
               Object receiver = Interpreter.eval(node.getFirst(), context);
               if (receiver == SHORT_CIRCUITED) {
                  return SHORT_CIRCUITED;
               }

               if (!optional || receiver != null && receiver != Terms.UNDEFINED) {
                  Object v = PrivateAccess.get(receiver, pn, context);
                  if (wantReceiver) {
                     context.callReceiver = receiver;
                  }

                  return v;
               }

               return SHORT_CIRCUITED;
            }

            name = keyNode.getText();

            try {
               object = Interpreter.eval(node.getFirst(), context);
            } catch (Exception var12) {
               if (context.root.bridge != null) {
                  String base = node.getFirst().getText();
                  String path = base + "." + name;
                  ExternalAccess ja = context.root.bridge.forType(path);
                  if (ja != null) {
                     if (wantReceiver) {
                        context.callReceiver = null;
                     }

                     if (functionCall) {
                        return (JsConstructor)(c, args) -> ja.construct(args);
                     }

                     return ja;
                  }

                  object = context.root.bridge.forType(base);
               } else {
                  object = null;
               }

               if (object == null) {
                  throw new RuntimeException("expression: " + node.getFirst().getText() + " - " + var12.getMessage(), var12);
               }
            }

            if (object == SHORT_CIRCUITED) {
               return SHORT_CIRCUITED;
            }

            if (optional && (object == null || object == Terms.UNDEFINED)) {
               return SHORT_CIRCUITED;
            }
         } else {
            optional = true;
            if (node.get(1).type == NodeType.REF_BRACKET_EXPR) {
               object = Interpreter.eval(node.getFirst(), context);
               if (object == SHORT_CIRCUITED) {
                  return SHORT_CIRCUITED;
               }

               if (object != null && object != Terms.UNDEFINED) {
                  Object index = Interpreter.eval(node.get(1).get(2), context);
                  Object v = getByIndex(object, index, false, context, functionCall);
                  if (wantReceiver) {
                     context.callReceiver = object;
                  }

                  return v;
               }

               return SHORT_CIRCUITED;
            }

            object = Interpreter.eval(node.getFirst(), context);
            if (object == SHORT_CIRCUITED) {
               return SHORT_CIRCUITED;
            }

            name = null;
         }

         if (functionCall) {
            Object jsValue = Terms.toJsValue(object);
            if (jsValue != null) {
               object = jsValue;
            }
         }

         if (name == null) {
            if (wantReceiver) {
               context.callReceiver = null;
            }

            return functionCall && context.root.bridge != null && object instanceof ExternalAccess ea ? (JavaCallable) (c, args) -> ea.construct(args) : object;
         } else {
            Object v2 = getByName(object, name, optional, context, functionCall);
            if (wantReceiver) {
               context.callReceiver = object;
            }

            return v2;
         }
      }
   }

   private static Object getRefDotExpr(Node node, CoreContext context, boolean functionCall) {
      return resolveRefDot(node, context, functionCall, false);
   }

   private static Object getCallableRefDotExpr(Node node, CoreContext context) {
      return resolveRefDot(node, context, true, true);
   }

   private static Object getRefBracketExpr(Node node, CoreContext context, boolean functionCall) {
      if (node.getFirst().type == NodeType.SUPER_EXPR) {
         ObjectLike superProto = Interpreter.evalSuperBase(context);
         Object index = Interpreter.eval(node.get(2), context);
         JsSymbol sym = JsSymbol.keyedBy(index);
         if (sym != null) {
            return superProto instanceof JsObject jo ? jo.getSymbolMember(sym, context.thisObject, context) : Terms.UNDEFINED;
         } else {
            return getByName(superProto, Terms.toPropertyKey(index), false, context, functionCall, context.thisObject);
         }
      } else {
         Object object = Interpreter.eval(node.getFirst(), context);
         if (object == SHORT_CIRCUITED) {
            return SHORT_CIRCUITED;
         } else {
            Object index = Interpreter.eval(node.get(2), context);
            Object result = getByIndex(object, index, false, context, functionCall);
            if (!functionCall && context.root.listener != null) {
               context.event(EventType.PROPERTY_GET, node, new Object[]{object, index, result});
            }

            return result;
         }
      }
   }

   private static Object getCallableParenInner(Node node, CoreContext context) {
      Node body = node.size() > 1 ? node.get(1) : null;
      Node inner = null;
      if (body != null && body.type == NodeType.EXPR_LIST && body.size() == 1) {
         Node onlyExpr = body.getFirst();
         if (onlyExpr.type == NodeType.EXPR && onlyExpr.size() == 1) {
            inner = onlyExpr.getFirst();
         }
      }

      if (inner != null && (inner.type == NodeType.REF_DOT_EXPR || inner.type == NodeType.REF_BRACKET_EXPR || inner.type == NodeType.PAREN_EXPR)) {
         Object result = getCallable(inner, context);
         if (result == SHORT_CIRCUITED) {
            context.callReceiver = null;
            return Terms.UNDEFINED;
         } else {
            return result;
         }
      } else {
         Object result = Interpreter.eval(node.get(1), context);
         context.callReceiver = null;
         return result;
      }
   }

   private static Object getCallableRefBracketExpr(Node node, CoreContext context) {
      if (node.getFirst().type == NodeType.SUPER_EXPR) {
         ObjectLike superProto = Interpreter.evalSuperBase(context);
         Object index = Interpreter.eval(node.get(2), context);
         JsSymbol sym = JsSymbol.keyedBy(index);
         Object result = sym != null
            ? (superProto instanceof JsObject jo ? jo.getSymbolMember(sym, context.thisObject, context) : Terms.UNDEFINED)
            : getByName(superProto, Terms.toPropertyKey(index), false, context, true, context.thisObject);
         context.callReceiver = context.thisObject;
         return result;
      } else {
         Object object = Interpreter.eval(node.getFirst(), context);
         if (object == SHORT_CIRCUITED) {
            return SHORT_CIRCUITED;
         } else {
            Object index = Interpreter.eval(node.get(2), context);
            Object result = getByIndex(object, index, false, context, true);
            context.callReceiver = object;
            return result;
         }
      }
   }

   private static Object getByIndex(Object object, Object index, boolean optional, CoreContext context, boolean functionCall) {
      JsSymbol sym = JsSymbol.keyedBy(index);
      if (sym != null) {
         return object instanceof JsObject jo ? jo.getSymbolMember(sym, object, context) : Terms.UNDEFINED;
      } else if (!functionCall && index instanceof Number n) {
         int i = denseIndex(n);
         if (i < 0) {
            return getByName(object, Terms.toPropertyKey(index), optional, context, functionCall);
         } else if (object != null && object != Terms.UNDEFINED) {
            if (object instanceof JsArray array) {
               return array.getIndexedValue(i, array, context);
            } else if (object instanceof List<?> list) {
               return i >= 0 && i < list.size() ? JsArray.unwrapHole(list.get(i)) : Terms.UNDEFINED;
            } else if (object instanceof String s) {
               return i >= 0 && i < s.length() ? s.substring(i, i + 1) : Terms.UNDEFINED;
            } else if (object instanceof byte[] bytes) {
               return i >= 0 && i < bytes.length ? bytes[i] & 255 : Terms.UNDEFINED;
            } else if (Terms.toObjectLike(object) instanceof JsArray jsArray) {
               return jsArray.getIndexedValue(i, jsArray, context);
            } else if (!(object instanceof Map) && !(object instanceof ObjectLike)) {
               throw JsErrorException.typeError("get by index [" + i + "] for non-array: " + object);
            } else {
               return getByName(object, Terms.toPropertyKey(index), optional, context, functionCall);
            }
         } else if (optional) {
            return Terms.UNDEFINED;
         } else {
            throw JsErrorException.typeError("cannot read properties of " + object + " (reading '[" + i + "]')");
         }
      } else {
         return getByName(object, Terms.toPropertyKey(index), optional, context, functionCall);
      }
   }

   private static Object getByName(Object object, String name, boolean optional, CoreContext context, boolean functionCall) {
      return getByName(object, name, optional, context, functionCall, object);
   }

   private static Object getByName(Object object, String name, boolean optional, CoreContext context, boolean functionCall, Object receiver) {
      if (object != null && object != Terms.UNDEFINED) {
         if (object instanceof JsObject jsObj) {
            PropertySlot own = jsObj.getOwnSlot(name);
            if (own != null) {
               return own.read(receiver, context);
            }

            Object result = jsObj.getMember(name, receiver, context);
            if (isFound(result)) {
               return result;
            }

            if (object instanceof JsValue && context.root.bridge != null && chainHasSlot(jsObj, name)) {
               return result;
            }

            if (object instanceof JsPrimitive prim) {
               return accessViaBridge(prim.getJavaValue(), name, context, functionCall);
            }

            if (!(object instanceof JsValue jv) || jv.getOriginalJavaValue() == null) {
               return Terms.UNDEFINED;
            }
         } else {
            if (object instanceof JsArray jsArr) {
               PropertySlot ownSlot = jsArr.getOwnSlot(name);
               if (ownSlot != null) {
                  return ownSlot.read(receiver, context);
               }

               if ("__proto__".equals(name)) {
                  ObjectLike p = jsArr.getPrototype();
                  return p != null ? p : Terms.UNDEFINED;
               }

               Object intrinsic = jsArr.resolveOwnIntrinsic(name);
               if (intrinsic != null) {
                  return intrinsic;
               }

               ObjectLike proto = jsArr.getPrototype();
               Object resultx = proto == null ? null : proto.getMember(name, receiver, context);
               if (isFound(resultx)) {
                  return resultx;
               }

               if (!name.isEmpty()) {
                  char c0 = name.charAt(0);
                  if (c0 >= '0' && c0 <= '9' && jsArr.hasOwnIndexedSlot(JsArray.parseIndex(name))) {
                     return resultx;
                  }
               }

               return Terms.UNDEFINED;
            }

            if (object instanceof ObjectLike ol) {
               Object resultxx = ol.getMember(name, receiver, context);
               if (isFound(resultxx)) {
                  return resultxx;
               }
            } else if (object instanceof Map<?, ?> map) {
               if (map.containsKey(name)) {
                  return map.get(name);
               }

               Object resultxx = PROTOTYPE_PROBE.getMember(name, receiver, context);
               if (resultxx != null) {
                  return resultxx;
               }

               if (context.root.bridge != null && chainHasSlot(PROTOTYPE_PROBE, name)) {
                  return resultxx;
               }
            } else {
               ObjectLike olx = Terms.toObjectLike(object);
               if (olx != null) {
                  Object resultxxx = olx.getMember(name, receiver, context);
                  if (isFound(resultxxx)) {
                     return resultxxx;
                  }

                  if (context.root.bridge != null && chainHasSlot(olx, name)) {
                     return resultxxx;
                  }
               }
            }
         }

         if (object instanceof JsCallable callable) {
            return (new JsFunction() {
               @Override
               public Object call(Context context, Object[] args) {
                  return callable.call(context, args);
               }
            }).getMember(name);
         } else {
            Object bridgeTarget = object instanceof JsValue jv ? jv.getOriginalJavaValue() : object;
            return bridgeTarget == null ? Terms.UNDEFINED : accessViaBridge(bridgeTarget, name, context, functionCall);
         }
      } else if (optional) {
         return Terms.UNDEFINED;
      } else {
         throw JsErrorException.typeError("cannot read properties of " + object + " (reading '" + name + "')");
      }
   }

   private static int denseIndex(Number n) {
      int i = n.intValue();
      return i >= 0 && n.doubleValue() == i ? i : -1;
   }

   private static void setByIndex(Object object, Object index, Object value, CoreContext context, Node trackingNode) {
      JsSymbol sym = JsSymbol.keyedBy(index);
      if (sym != null) {
         if (object instanceof JsObject jo) {
            jo.setSymbol(sym, value, context, context.strict);
         }
      } else {
         if (index instanceof Number n) {
            int i = denseIndex(n);
            if (i < 0) {
               setByName(object, Terms.toPropertyKey(index), value, context, trackingNode);
               return;
            }

            if (object instanceof JsArray array && (array.hasIndexedDescriptor(i) || !array.isExtensible())) {
               setByName(object, String.valueOf(i), value, context, trackingNode);
               return;
            }

            if (object instanceof List) {
               List<Object> list = (List<Object>) object;
               Object oldValue = i < list.size() ? list.get(i) : Terms.UNDEFINED;
               if (i < list.size()) {
                  list.set(i, value);
               } else {
                  Object pad = object instanceof JsArray ? JsArray.HOLE : Terms.UNDEFINED;
                  if (object instanceof JsArray ja) {
                     ja.checkDensePad(i);
                  }

                  while (list.size() < i) {
                     list.add(pad);
                  }

                  list.add(value);
               }

               firePropertySet(context, String.valueOf(i), value, oldValue, object, trackingNode);
               return;
            }

            if (object instanceof byte[] bytes) {
               if (value instanceof Number v) {
                  Object oldValue = i < bytes.length ? bytes[i] & 255 : Terms.UNDEFINED;
                  bytes[i] = (byte)(v.intValue() & 0xFF);
                  firePropertySet(context, String.valueOf(i), v.intValue() & 0xFF, oldValue, object, trackingNode);
               }

               return;
            }
         }

         setByName(object, Terms.toPropertyKey(index), value, context, trackingNode);
      }
   }

   static void setByName(Object object, String name, Object value, CoreContext context, Node trackingNode) {
      setByName(object, name, value, context, trackingNode, object);
   }

   static void setByName(Object object, String name, Object value, CoreContext context, Node trackingNode, Object receiver) {
      if (name == null) {
         throw JsErrorException.typeError("unexpected set [null]:" + value + " on: " + object);
      } else {
         if (object == null) {
            context.update(name, value, trackingNode);
         } else if (object instanceof ObjectLike objectLike) {
            if (objectLike instanceof JsArray ja && "length".equals(name)) {
               Object oldLen = ja.size();
               ja.handleLengthAssign(value, context);
               firePropertySet(context, name, ja.size(), oldLen, object, trackingNode);
               return;
            }

            AccessorSlot accSlot = findAccessorInChain(objectLike, name);
            if (accSlot != null) {
               accSlot.write(receiver, value, context, context.strict);
               return;
            }

            if (receiver != object) {
               if (receiver instanceof ObjectLike receiverObj) {
                  Object oldValue = receiverObj.getMember(name);
                  receiverObj.putMember(name, value, context, context.strict);
                  firePropertySet(context, name, value, oldValue, receiver, trackingNode);
                  return;
               }

               if (context.strict) {
                  throw JsErrorException.typeError("cannot create property '" + name + "' on " + Terms.typeOf(receiver));
               }

               return;
            }

            Object oldValue = objectLike.getMember(name);
            objectLike.putMember(name, value, context, context.strict);
            firePropertySet(context, name, value, oldValue, object, trackingNode);
         } else if (object instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) object;
            Object oldValue = map.get(name);
            map.put(name, value);
            firePropertySet(context, name, value, oldValue, object, trackingNode);
         } else {
            if (context.root.bridge == null) {
               throw JsErrorException.typeError("cannot set '" + name + "'");
            }

            try {
               if (object instanceof ExternalAccess ja) {
                  ja.setProperty(name, value);
               } else {
                  ExternalAccess ja = context.root.bridge.forInstance(object);
                  ja.setProperty(name, value);
               }

               firePropertySet(context, name, value, null, object, trackingNode);
            } catch (Exception var10) {
               logger.error("external bridge error: {}", var10.getMessage());
               throw JsErrorException.typeError("cannot set '" + name + "'");
            }
         }
      }
   }

   private static void firePropertySet(CoreContext context, String name, Object value, Object oldValue, Object target, Node node) {
      if (context.root.listener != null) {
         context.root.listener.onBind(BindEvent.propertySet(name, value, oldValue, target, context, node));
      }
   }

   private static Object postIncDecByIndex(Object object, Object index, boolean isIncrement, CoreContext context) {
      if (index instanceof Number n) {
         int i = denseIndex(n);
         if (object instanceof List && i >= 0) {
            List<Object> list = (List<Object>)object;
            Object oldValue = i < list.size() ? list.get(i) : Terms.UNDEFINED;
            Object step = Terms.incDecStep(oldValue);
            Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
            if (i < list.size()) {
               list.set(i, newValue);
            } else {
               setByIndex(object, i, newValue, context, null);
            }

            firePropertySet(context, String.valueOf(i), newValue, oldValue, object, null);
            return oldValue;
         }
      }

      return postIncDecByName(object, Terms.toPropertyKey(index), isIncrement, context);
   }

   private static Object superIncDec(PropertyAccess.AccessSite site, boolean isIncrement, boolean pre, CoreContext context) {
      Object oldValue = siteRead(site, context);
      Object step = Terms.incDecStep(oldValue);
      Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
      siteWrite(site, newValue, context, null);
      return pre ? newValue : oldValue;
   }

   private static Object privateIncDec(PropertyAccess.AccessSite site, boolean isIncrement, boolean pre, CoreContext context) {
      Object oldValue = PrivateAccess.get(site.target, site.privateName, context);
      Object step = Terms.incDecStep(oldValue);
      Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
      PrivateAccess.set(site.target, site.privateName, newValue, context);
      return pre ? newValue : oldValue;
   }

   private static Object postIncDecByName(Object object, String name, boolean isIncrement, CoreContext context) {
      Object oldValue = getByName(object, name, false, context, false);
      Object step = Terms.incDecStep(oldValue);
      Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
      setByName(object, name, newValue, context, null);
      return oldValue;
   }

   private static Object preIncDecByIndex(Object object, Object index, boolean isIncrement, CoreContext context) {
      if (index instanceof Number n) {
         int i = denseIndex(n);
         if (object instanceof List && i >= 0) {
            List<Object> list = (List<Object>)object;
            Object oldValue = i < list.size() ? list.get(i) : Terms.UNDEFINED;
            Object step = Terms.incDecStep(oldValue);
            Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
            if (i < list.size()) {
               list.set(i, newValue);
            } else {
               setByIndex(object, i, newValue, context, null);
            }

            firePropertySet(context, String.valueOf(i), newValue, oldValue, object, null);
            return newValue;
         }
      }

      return preIncDecByName(object, Terms.toPropertyKey(index), isIncrement, context);
   }

   private static Object preIncDecByName(Object object, String name, boolean isIncrement, CoreContext context) {
      Object oldValue = getByName(object, name, false, context, false);
      Object step = Terms.incDecStep(oldValue);
      Object newValue = isIncrement ? Terms.add(oldValue, step, context) : Terms.min(oldValue, step, context);
      setByName(object, name, newValue, context, null);
      return newValue;
   }

   private static boolean deleteByKey(Object object, String key, CoreContext context, Node node) {
      Object oldValue = null;
      if (object instanceof ObjectLike ol) {
         if (!context.strict && ol instanceof JsObject jo && jo.isOwnProperty(key) && (jo.getOwnAttrs(key) & 4) == 0) {
            return false;
         } else {
            oldValue = ol.getMember(key);
            ol.removeMember(key, context, context.strict);
            firePropertyDelete(context, key, oldValue, object, node);
            return true;
         }
      } else if (object instanceof Map<?, ?> map) {
         oldValue = map.get(key);
         map.remove(key);
         firePropertyDelete(context, key, oldValue, object, node);
         return true;
      } else if (object instanceof String s) {
         if ("length".equals(key)) {
            return false;
         } else {
            int idx = JsArray.parseIndex(key);
            return idx < 0 || idx >= s.length();
         }
      } else {
         return object instanceof Number || object instanceof Boolean;
      }
   }

   private static void firePropertyDelete(CoreContext context, String name, Object oldValue, Object target, Node node) {
      if (context.root.listener != null) {
         context.root.listener.onBind(BindEvent.propertyDelete(name, oldValue, target, context, node));
      }
   }

   private static Object applyOperator(Object oldValue, TokenType operator, Object operand, CoreContext context) {
      return switch (operator) {
         case PLUS_EQ -> Terms.add(oldValue, operand, context);
         case MINUS_EQ -> Terms.min(oldValue, operand, context);
         case STAR_EQ -> Terms.mul(oldValue, operand, context);
         case SLASH_EQ -> Terms.div(oldValue, operand, context);
         case PERCENT_EQ -> Terms.mod(oldValue, operand, context);
         case STAR_STAR_EQ -> Terms.exp(oldValue, operand, context);
         case GT_GT_EQ -> Terms.bitShiftRight(oldValue, operand, context);
         case LT_LT_EQ -> Terms.bitShiftLeft(oldValue, operand, context);
         case GT_GT_GT_EQ -> Terms.bitShiftRightUnsigned(oldValue, operand, context);
         case AMP_EQ -> Terms.bitAnd(oldValue, operand, context);
         case PIPE_EQ -> Terms.bitOr(oldValue, operand, context);
         case CARET_EQ -> Terms.bitXor(oldValue, operand, context);
         default -> throw new RuntimeException("unexpected operator: " + operator);
      };
   }

   private static boolean isFound(Object result) {
      return result != null && result != Terms.UNDEFINED;
   }

   private static AccessorSlot findAccessorInChain(ObjectLike obj, String name) {
      for (ObjectLike current = obj; current != null; current = current.getPrototype()) {
         PropertySlot s = ownSlot(current, name);
         if (s instanceof AccessorSlot acc) {
            return acc;
         }

         if (s != null) {
            return null;
         }
      }

      return null;
   }

   static boolean chainHasSlot(ObjectLike start, String name) {
      for (ObjectLike o = start; o != null; o = o.getPrototype()) {
         if (ownSlot(o, name) != null) {
            return true;
         }
      }

      return false;
   }

   static PropertySlot ownSlot(Object obj, String key) {
      if (obj instanceof JsObject jo) {
         return jo.getOwnSlot(key);
      } else if (obj instanceof JsArray ja) {
         return ja.getOwnSlot(key);
      } else {
         return obj instanceof Prototype p ? p.getOwnSlot(key) : null;
      }
   }

   private static Object accessViaBridge(Object object, String name, CoreContext context, boolean functionCall) {
      if (context.root.bridge == null) {
         return Terms.UNDEFINED;
      } else {
         try {
            ExternalAccess ja = object instanceof ExternalAccess ea ? ea : context.root.bridge.forInstance(object);
            if (functionCall) {
               return ja.getMethod(name);
            } else {
               Object result = ja.getPropertyOrNotFound(name);
               return result == JavaUtils.NOT_FOUND ? Terms.UNDEFINED : result;
            }
         } catch (Exception var6) {
            return Terms.UNDEFINED;
         }
      }
   }

   private static final class AccessSite {
      final Object target;
      final Object key;
      final boolean isIndex;
      final PrivateName privateName;
      final Object receiver;

      AccessSite(Object target, Object key, boolean isIndex) {
         this(target, key, isIndex, null, null);
      }

      AccessSite(Object target, Object key, boolean isIndex, PrivateName privateName) {
         this(target, key, isIndex, privateName, null);
      }

      AccessSite(Object target, Object key, boolean isIndex, PrivateName privateName, Object receiver) {
         this.target = target;
         this.key = key;
         this.isIndex = isIndex;
         this.privateName = privateName;
         this.receiver = receiver;
      }
   }
}
