package io.karatelabs.js;

import io.karatelabs.parser.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class CoreContext implements Context {
   ContextRoot root;
   Object thisObject = Terms.UNDEFINED;
   CallInfo callInfo;
   JsFunctionNode activeFunction;
   PrivateEnv privateEnv;
   boolean strict;
   BindingsStore bindings;
   final Object[] callArgs;
   final CoreContext outer;
   boolean arrowFrame;
   private JsArray argumentsObject;
   int currentLevel = 0;
   List<ScopeEntry> scopeStack;
   Object callReceiver;
   final BindingsStore capturedBindings;
   Object[] frame;
   SlotTable frameTable;
   final CoreContext parent;
   final ContextScope scope;
   final int depth;
   final Node node;
   private static final Object SHADOWED_UNAVAILABLE = new Object();
   static final Object NOT_FOUND = new Object();
   int iteration = -1;
   private ExitType exitType;
   private Object returnValue;
   private Object errorThrown;
   private String exitLabel;
   private boolean errorAuthored;
   private int errorAuthoredLine;

   CoreContext(ContextRoot root, CoreContext parent, int depth, Node node, ContextScope scope, BindingsStore bindings) {
      this.root = root;
      this.parent = parent;
      this.depth = depth;
      this.node = node;
      this.scope = scope;
      this.callArgs = null;
      this.outer = null;
      this.capturedBindings = null;
      this.bindings = bindings;
      if (parent != null) {
         this.thisObject = parent.thisObject;
         this.strict = parent.strict;
         this.activeFunction = parent.activeFunction;
         this.privateEnv = parent.privateEnv;
      } else if (root != null) {
         this.thisObject = root.thisObject;
      }
   }

   CoreContext(CoreContext parent, Node node, Object[] functionArgs, CoreContext outer, BindingsStore captured) {
      this.root = parent.root;
      this.parent = parent;
      this.depth = parent.depth + 1;
      this.node = node;
      this.scope = ContextScope.FUNCTION;
      this.callArgs = functionArgs;
      this.outer = outer;
      this.capturedBindings = captured;
      this.thisObject = parent.thisObject;
      this.activeFunction = parent.activeFunction;
   }

   CoreContext(CoreContext parent, Node node, Object[] functionArgs) {
      this(parent, node, functionArgs, null, null);
   }

   void event(EventType type, Node node) {
      if (this.root.listener != null) {
         Event event = new Event(type, this, node);
         this.root.listener.onEvent(event);
      }
   }

   void event(EventType type, Node node, Object value) {
      if (this.root.listener != null) {
         Event event = new Event(type, this, node, value);
         this.root.listener.onEvent(event);
      }
   }

   @Override
   public Engine getEngine() {
      return this.root.getEngine();
   }

   @Override
   public Context getParent() {
      return (Context)(this.parent != null ? this.parent : this.root);
   }

   @Override
   public ContextScope getScope() {
      return this.scope;
   }

   @Override
   public int getDepth() {
      return this.depth;
   }

   @Override
   public Node getNode() {
      return this.node;
   }

   @Override
   public int getIteration() {
      return this.iteration;
   }

   @Override
   public String getPath() {
      String parentPath = this.depth == 0 ? null : this.parent.getPath();
      String suffix = this.iteration == -1 ? "" : "[" + this.iteration + "]";
      return parentPath == null ? this.node.type + suffix : parentPath + "." + this.node.type + suffix;
   }

   @Override
   public Object getThisObject() {
      return this.thisObject;
   }

   @Override
   public CallInfo getCallInfo() {
      return this.callInfo;
   }

   @Override
   public Object[] getCallArgs() {
      return this.callArgs;
   }

   @Override
   public Map<String, Object> getLocalVariables() {
      Map<String, Object> locals = this.collectLocals();
      Map<String, Object> result = new LinkedHashMap<>(locals.size());
      locals.forEach((k, v) -> {
         if (v != SHADOWED_UNAVAILABLE) {
            result.put(k, Engine.toJava(v));
         }
      });
      return result;
   }

   @Override
   public Object evalWithLocals(String expression) {
      Map<String, Object> locals = this.collectLocals();
      locals.values().removeIf(v -> v == SHADOWED_UNAVAILABLE);
      return this.getEngine().evalWith(expression, locals);
   }

   private Map<String, Object> collectLocals() {
      Map<String, Object> map = new LinkedHashMap<>();
      if (this.frame != null && this.frameTable != null) {
         if (this.scopeStack != null) {
            for (int i = this.scopeStack.size() - 1; i >= 0; i--) {
               if (this.scopeStack.get(i).node != null && this.scopeStack.get(i).node.meta instanceof LexicalScope ls && ls != LexicalScope.NONE) {
                  int[] slots = ls.frameSlots(this.frameTable);
                  if (slots != null) {
                     for (int s : slots) {
                        this.collectSlot(s, map);
                     }
                  }
               }
            }
         }

         for (int s = 0; s < this.frameTable.size(); s++) {
            if (this.frameTable.isFunctionScoped(s)) {
               this.collectSlot(s, map);
            }
         }
      }

      BindingsStore globals = this.root.bindings;

      for (CoreContext c = this; c != null; c = c.outer != null ? c.outer : c.parent) {
         if (c.bindings == globals) {
            collectStore(globals, map, 1);
            break;
         }

         collectStore(c.bindings, map, 0);
         collectStore(c.capturedBindings, map, 0);
      }

      return map;
   }

   private void collectSlot(int s, Map<String, Object> map) {
      if (s >= 0 && s < this.frame.length) {
         Object v = this.frame[s];
         if (v != SlotTable.UNDECLARED) {
            map.putIfAbsent(this.frameTable.names[s], v == SlotTable.TDZ ? SHADOWED_UNAVAILABLE : v);
         }
      }
   }

   private static void collectStore(BindingsStore store, Map<String, Object> map, int minLevel) {
      if (store != null) {
         for (String key : store.keys()) {
            if (!map.containsKey(key)) {
               BindingSlot s = store.getSlot(key);
               if (s != null && !s.hidden && !s.tombstoned && s.level >= minLevel) {
                  if (s.scope != null && !s.initialized) {
                     map.put(key, SHADOWED_UNAVAILABLE);
                  } else {
                     Object v = s.value;
                     if (v instanceof JsLazy lazy) {
                        try {
                           v = lazy.get();
                        } catch (Exception var9) {
                           v = SHADOWED_UNAVAILABLE;
                        }
                     }

                     map.put(key, v);
                  }
               }
            }
         }
      }
   }

   @Override
   public String toString() {
      return this.getPath();
   }

   void enterScope(ContextScope scope, Node node) {
      this.currentLevel++;
      if (this.scopeStack == null) {
         this.scopeStack = new ArrayList<>(4);
      }

      this.scopeStack.add(new ScopeEntry(this.currentLevel, scope, node));
   }

   void exitScope() {
      if (this.scopeStack != null && !this.scopeStack.isEmpty()) {
         if (this.bindings != null) {
            this.bindings.popLevel(this.currentLevel);
         }

         this.scopeStack.remove(this.scopeStack.size() - 1);
         this.currentLevel--;
      }
   }

   int findFunctionLevel() {
      if (this.scopeStack != null) {
         for (int i = this.scopeStack.size() - 1; i >= 0; i--) {
            ScopeEntry entry = this.scopeStack.get(i);
            if (entry.scope == ContextScope.FUNCTION) {
               return entry.level;
            }
         }
      }

      return 0;
   }

   ContextScope getCurrentScope() {
      return this.scopeStack != null && !this.scopeStack.isEmpty() ? this.scopeStack.getLast().scope : this.scope;
   }

   BindingSlot resolve(String key) {
      if (this.bindings != null) {
         BindingSlot s = this.bindings.getSlot(key);
         if (s != null) {
            return s;
         }
      }

      if (this.capturedBindings != null) {
         BindingSlot s = this.capturedBindings.getSlot(key);
         if (s != null) {
            return s;
         }
      }

      if (this.outer != null) {
         BindingSlot s = this.outer.resolve(key);
         if (s != null) {
            return s;
         }
      } else if (this.parent != null) {
         BindingSlot s = this.parent.resolve(key);
         if (s != null) {
            return s;
         }
      }

      return this.root.resolveOrInit(key);
   }

   Object get(String key) {
      if ("this".equals(key)) {
         return this.thisObject;
      } else {
         if (this.callArgs != null && "arguments".equals(key)) {
            JsArray a = this.argumentsForRead();
            if (a != null) {
               return a;
            }
         }

         if (this.frameTable != null) {
            int idx = this.frameTable.indexOf(key);
            if (idx >= 0) {
               Object v = this.frame[idx];
               if (v != SlotTable.UNDECLARED) {
                  if (v == SlotTable.TDZ) {
                     throw SlotTable.tdzError(key);
                  }

                  return v;
               }
            }
         }

         BindingSlot s = this.resolve(key);
         return s == null ? Terms.UNDEFINED : this.readSlot(s, key);
      }
   }

   JsArray argumentsForRead() {
      if (!this.arrowFrame) {
         return this.getArgumentsObject();
      } else if (this.hasOwnArgumentsBinding()) {
         return null;
      } else {
         for (CoreContext c = this.outer != null ? this.outer : this.parent; c != null; c = c.outer != null ? c.outer : c.parent) {
            if (c.hasOwnArgumentsBinding()) {
               return null;
            }

            if (c.callArgs != null && !c.arrowFrame) {
               return c.getArgumentsObject();
            }
         }

         return null;
      }
   }

   private boolean hasOwnArgumentsBinding() {
      if (this.frameTable != null) {
         int idx = this.frameTable.indexOf("arguments");
         if (idx >= 0 && this.frame != null && this.frame[idx] != SlotTable.UNDECLARED) {
            return true;
         }
      }

      return this.bindings != null && this.bindings.hasMember("arguments");
   }

   JsArray getArgumentsObject() {
      if (this.argumentsObject == null) {
         List<Object> list = new ArrayList<>(this.callArgs.length);
         Collections.addAll(list, this.callArgs);
         this.argumentsObject = new JsArray(list);
      }

      return this.argumentsObject;
   }

   Object readSlot(BindingSlot s, String key) {
      if (s.scope != null && !s.initialized) {
         throw SlotTable.tdzError(key);
      } else {
         Object v = s.value;
         return v instanceof JsLazy lz ? lz.get() : v;
      }
   }

   boolean hasKey(String key) {
      if ("this".equals(key)) {
         return true;
      } else if (this.callArgs != null && "arguments".equals(key) && this.argumentsForRead() != null) {
         return true;
      } else {
         if (this.frameTable != null) {
            int idx = this.frameTable.indexOf(key);
            if (idx >= 0 && this.frame[idx] != SlotTable.UNDECLARED) {
               return true;
            }
         }

         return this.resolve(key) != null;
      }
   }

   Object getOrNotFound(String key) {
      if ("this".equals(key)) {
         return this.thisObject;
      } else {
         if (this.callArgs != null && "arguments".equals(key)) {
            JsArray a = this.argumentsForRead();
            if (a != null) {
               return a;
            }
         }

         if (this.frameTable != null) {
            int idx = this.frameTable.indexOf(key);
            if (idx >= 0) {
               Object v = this.frame[idx];
               if (v != SlotTable.UNDECLARED) {
                  if (v == SlotTable.TDZ) {
                     throw SlotTable.tdzError(key);
                  }

                  return v;
               }
            }
         }

         BindingSlot s = this.resolve(key);
         return s == null ? NOT_FOUND : this.readSlot(s, key);
      }
   }

   void put(String key, Object value) {
      this.declare(key, value, null, true);
   }

   void declare(String key, Object value, BindScope scope, boolean initialized) {
      if (value instanceof JsFunction fn && (fn.name == null || fn.name.isEmpty())) {
         fn.name = key;
      }

      if (this.frameTable != null) {
         int idx = this.frameTable.indexOf(key);
         if (idx >= 0) {
            this.frame[idx] = scope != null && !initialized ? SlotTable.TDZ : value;
            return;
         }
      }

      if (scope != null) {
         BindingSlot existing = this.bindings == null ? null : this.bindings.getSlot(key);
         if (existing != null && existing.scope != null && existing.level == this.currentLevel) {
            if (!existing.initialized) {
               existing.value = value;
               existing.scope = scope;
               existing.initialized = initialized;
               if (this.depth == 0) {
                  existing.evalId = this.root.evalId;
               }

               return;
            }

            ContextScope currentScope = this.getCurrentScope();
            if (currentScope != ContextScope.LOOP_INIT && currentScope != ContextScope.LOOP_BODY) {
               if (this.depth == 0 && existing.evalId != this.root.evalId) {
                  existing.value = value;
                  existing.scope = scope;
                  existing.initialized = initialized;
                  existing.evalId = this.root.evalId;
                  return;
               }

               throw JsErrorException.syntaxError("identifier '" + key + "' has already been declared");
            }
         }

         if (this.bindings == null) {
            this.bindings = new BindingsStore();
         }

         this.bindings.pushBinding(key, value, scope, this.currentLevel, initialized);
         if (this.depth == 0) {
            this.bindings.getSlot(key).evalId = this.root.evalId;
         }
      } else {
         int functionLevel = this.findFunctionLevel();
         if (this.bindings == null) {
            this.bindings = new BindingsStore();
         }

         BindingSlot existingx = this.bindings.getSlot(key);
         if (existingx != null && existingx.level <= functionLevel) {
            existingx.value = value;
         } else {
            this.bindings.pushBinding(key, value, null, functionLevel);
         }
      }
   }

   void declareUninitialized(String key, BindScope scope) {
      if (this.bindings == null) {
         this.bindings = new BindingsStore();
      }

      this.bindings.pushBinding(key, Terms.UNDEFINED, scope, this.currentLevel, false);
      if (this.depth == 0) {
         this.bindings.getSlot(key).evalId = this.root.evalId;
      }
   }

   void update(String key, Object value) {
      this.update(key, value, null);
   }

   void update(String key, Object value, Node node) {
      if (this.frameTable != null) {
         int idx = this.frameTable.indexOf(key);
         if (idx >= 0 && this.frame[idx] != SlotTable.UNDECLARED) {
            this.updateSlot(idx, value, node);
            return;
         }
      }

      BindingSlot s = this.resolve(key);
      if (s == null) {
         if (this.strict) {
            throw JsErrorException.referenceError(key + " is not defined");
         } else {
            this.assignImplicitGlobal(key, value, node);
         }
      } else if (s.scope != null && !s.initialized) {
         throw SlotTable.tdzError(key);
      } else if (s.scope == BindScope.CONST) {
         throw JsErrorException.typeError("assignment to constant: " + key);
      } else {
         if (value instanceof JsFunction fn && (fn.name == null || fn.name.isEmpty())) {
            fn.name = key;
         }

         Object oldValue = s.value;
         s.value = value;
         if (this.root.listener != null) {
            this.root.listener.onBind(BindEvent.assign(key, value, oldValue, this, node));
         }
      }
   }

   void updateSlot(int idx, Object value, Node node) {
      Object oldValue = this.frame[idx];
      if (oldValue == SlotTable.TDZ) {
         throw SlotTable.tdzError(this.frameTable.names[idx]);
      } else if (this.frameTable.kinds[idx] == 4) {
         throw JsErrorException.typeError("assignment to constant: " + this.frameTable.names[idx]);
      } else {
         if (value instanceof JsFunction fn && (fn.name == null || fn.name.isEmpty())) {
            fn.name = this.frameTable.names[idx];
         }

         this.frame[idx] = value;
         if (this.root.listener != null) {
            this.root.listener.onBind(BindEvent.assign(this.frameTable.names[idx], value, oldValue, this, node));
         }
      }
   }

   private void assignImplicitGlobal(String key, Object value, Node node) {
      this.root.bindings.putMember(key, value, null, true);
      if (this.root.listener != null) {
         this.root.listener.onBind(BindEvent.declare(key, value, BindScope.VAR, this, node));
      }
   }

   Object stopAndBreak(String label) {
      this.exitType = ExitType.BREAK;
      this.exitLabel = label;
      this.returnValue = null;
      this.errorThrown = null;
      this.errorAuthored = false;
      this.errorAuthoredLine = 0;
      return null;
   }

   Object stopAndThrow(Object error) {
      this.exitType = ExitType.THROW;
      this.exitLabel = null;
      this.returnValue = null;
      this.errorThrown = error;
      this.errorAuthored = false;
      this.errorAuthoredLine = 0;
      return error;
   }

   Object stopAndThrowAuthored(Object error, int line) {
      this.stopAndThrow(error);
      this.errorAuthored = true;
      this.errorAuthoredLine = line;
      return error;
   }

   Object stopAndReturn(Object value) {
      this.exitType = ExitType.RETURN;
      this.exitLabel = null;
      this.returnValue = value;
      this.errorThrown = null;
      this.errorAuthored = false;
      this.errorAuthoredLine = 0;
      return value;
   }

   Object stopAndContinue(String label) {
      this.exitType = ExitType.CONTINUE;
      this.exitLabel = label;
      this.returnValue = null;
      this.errorThrown = null;
      this.errorAuthored = false;
      this.errorAuthoredLine = 0;
      return null;
   }

   boolean isStopped() {
      return this.exitType != null;
   }

   boolean isContinuing() {
      return this.exitType == ExitType.CONTINUE;
   }

   boolean isBreaking() {
      return this.exitType == ExitType.BREAK;
   }

   void reset() {
      this.exitType = null;
      this.exitLabel = null;
      this.returnValue = null;
      this.errorThrown = null;
      this.errorAuthored = false;
      this.errorAuthoredLine = 0;
   }

   void restoreCompletion(ExitType savedExit, String savedLabel, Object savedReturn, Object savedError, boolean savedAuthored, int savedAuthoredLine) {
      this.exitType = savedExit;
      this.exitLabel = savedLabel;
      this.returnValue = savedReturn;
      this.errorThrown = savedError;
      this.errorAuthored = savedAuthored;
      this.errorAuthoredLine = savedAuthoredLine;
   }

   boolean isError() {
      return this.exitType == ExitType.THROW;
   }

   @Override
   public ExitType getExitType() {
      return this.exitType;
   }

   String getExitLabel() {
      return this.exitLabel;
   }

   @Override
   public Object getReturnValue() {
      return this.returnValue;
   }

   @Override
   public Object getErrorThrown() {
      return this.errorThrown;
   }

   boolean isErrorAuthored() {
      return this.errorAuthored;
   }

   int getErrorAuthoredLine() {
      return this.errorAuthoredLine;
   }

   void updateFrom(CoreContext childContext) {
      this.exitType = childContext.exitType;
      this.exitLabel = childContext.exitLabel;
      this.errorThrown = childContext.errorThrown;
      this.errorAuthored = childContext.errorAuthored;
      this.errorAuthoredLine = childContext.errorAuthoredLine;
      this.returnValue = childContext.returnValue;
   }
}
