package io.karatelabs.js;

import io.karatelabs.common.Resource;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.ParserException;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Engine {
   public static boolean DEBUG = false;
   private static final ThreadLocal<Engine> CURRENT = new ThreadLocal<>();
   final BindingsStore bindings = new BindingsStore();
   private final Bindings bindingsView = new Bindings(this.bindings);
   private final ContextRoot root = new ContextRoot(this);
   private Map<Prototype, Map<String, PropertySlot>> protoUserProps;
   boolean numericPropPolluted;
   final ReentrantLock jsLock = new ReentrantLock(true);
   private final Object scopeGate = new Object();
   private Thread scopeOwner;
   private int evalDepth;
   private AsyncScope asyncScope;
   private volatile String poisoned;
   private Duration asyncDrainTimeout;
   private boolean asyncRejectionWarnOnly;
   long asyncTeardownMillis = 5000L;
   private boolean leadingFunctionExpression;

   static Engine current() {
      return CURRENT.get();
   }

   static Engine enter(Engine engine) {
      Engine prev = CURRENT.get();
      CURRENT.set(engine);
      return prev;
   }

   static void exit(Engine prev) {
      if (prev == null) {
         CURRENT.remove();
      } else {
         CURRENT.set(prev);
      }
   }

   Map<String, PropertySlot> protoUserProps(Prototype proto, boolean create) {
      if (this.protoUserProps == null) {
         if (!create) {
            return null;
         }

         this.protoUserProps = new HashMap<>();
      }

      Map<String, PropertySlot> map = this.protoUserProps.get(proto);
      if (map == null && create) {
         map = new LinkedHashMap<>();
         this.protoUserProps.put(proto, map);
      }

      return map;
   }

   JsFunction builtinConstructor(String name) {
      return this.root.builtinConstructor(name);
   }

   ContextRoot root() {
      return this.root;
   }

   AsyncScope currentScope() {
      synchronized (this.scopeGate) {
         return this.asyncScope;
      }
   }

   Duration asyncDrainTimeout() {
      return this.asyncDrainTimeout;
   }

   boolean asyncRejectionWarnOnly() {
      return this.asyncRejectionWarnOnly;
   }

   public void setAsyncDrainTimeout(Duration timeout) {
      this.asyncDrainTimeout = timeout;
   }

   public void setAsyncRejectionWarnOnly(boolean warnOnly) {
      this.asyncRejectionWarnOnly = warnOnly;
   }

   public void setLeadingFunctionExpression(boolean leadingFunctionExpression) {
      this.leadingFunctionExpression = leadingFunctionExpression;
   }

   private JsParser parser(Resource resource) {
      JsParser parser = new JsParser(resource);
      return this.leadingFunctionExpression ? parser.leadingFunctionExpression() : parser;
   }

   public boolean isPoisoned() {
      return this.poisoned != null;
   }

   void poison(String detail) {
      if (this.poisoned == null) {
         this.poisoned = detail;
      }
   }

   void checkPoisoned() {
      String detail = this.poisoned;
      if (detail != null) {
         throw new EngineException("engine is poisoned: " + detail, null);
      }
   }

   int releaseJsLock() {
      int holds = this.jsLock.getHoldCount();

      for (int i = 0; i < holds; i++) {
         this.jsLock.unlock();
      }

      return holds;
   }

   void reacquireJsLock(int holds) {
      for (int i = 0; i < holds; i++) {
         this.jsLock.lock();
      }
   }

   boolean enterEvalScope() {
      synchronized (this.scopeGate) {
         if (this.scopeOwner == Thread.currentThread()) {
            this.evalDepth++;
            return false;
         } else {
            AsyncScope current = this.asyncScope;
            if (current != null && runningInsideScope(current)) {
               this.evalDepth++;
               return false;
            } else {
               while (this.scopeOwner != null) {
                  try {
                     this.scopeGate.wait();
                  } catch (InterruptedException var5) {
                     Thread.currentThread().interrupt();
                     throw new EngineInterruptedException();
                  }
               }

               this.scopeOwner = Thread.currentThread();
               this.evalDepth = 1;
               this.asyncScope = new AsyncScope(this);
               return true;
            }
         }
      }
   }

   private static boolean runningInsideScope(AsyncScope scope) {
      AsyncActivation act = AsyncActivation.current();
      if (act != null && act.scope == scope) {
         return true;
      } else {
         GeneratorActivation gen = GeneratorActivation.current();
         return gen != null && gen.currentStepScope() == scope;
      }
   }

   void exitEvalScope(boolean outermost) {
      synchronized (this.scopeGate) {
         this.evalDepth--;
         if (outermost) {
            this.scopeOwner = null;
            this.asyncScope = null;
            this.scopeGate.notifyAll();
         }
      }
   }

   void closeScope(AsyncScope scope, String reason) {
      List<Object> stuck = scope.close(reason, this.asyncTeardownMillis);
      if (!stuck.isEmpty()) {
         this.poison("activation did not terminate at teardown: " + stuck.get(0));
      }
   }

   public Object eval(Node program) {
      return this.evalInternal(program, null);
   }

   public Object eval(Resource resource) {
      return this.evalInternal(resource, null);
   }

   public Object eval(File file) {
      return this.evalInternal(Resource.from(file.toPath()), null);
   }

   public Object eval(String text) {
      return this.evalInternal(Resource.text(text), null);
   }

   public static Node parse(String text) {
      return new JsParser(Resource.text(text)).parse();
   }

   public Object evalWith(Node program, Map<String, Object> vars) {
      return this.evalInternal(program, vars);
   }

   public Object evalWith(String text, Map<String, Object> vars) {
      return this.evalWith(Resource.text(text), vars);
   }

   public Object evalWith(Resource resource, Map<String, Object> vars) {
      return this.evalInternal(resource, vars);
   }

   public Object get(String name) {
      return toJava(this.bindings.getMember(name));
   }

   public void put(String name, Object value) {
      this.bindings.putMember(name, value);
   }

   public void putRootBinding(String name, Object value) {
      this.bindings.putHidden(name, value);
   }

   public void remove(String name) {
      this.bindings.remove(name);
   }

   public void setOnConsoleLog(Consumer<String> onConsoleLog) {
      this.root.setOnConsoleLog(onConsoleLog);
   }

   public Map<String, Object> getBindings() {
      return this.bindingsView;
   }

   public Map<String, Object> getRawBindings() {
      return this.bindings.getRawMap(false);
   }

   public Context getRootContext() {
      return this.root;
   }

   public Map<String, Object> getRootBindings() {
      return this.bindings.getRawMap(true);
   }

   public void setListener(ContextListener listener) {
      this.root.listener = listener;
   }

   public void setExternalBridge(ExternalBridge bridge) {
      this.root.bridge = bridge;
   }

   public <T> void setDebugSupport(RunInterceptor<T> interceptor, DebugPointFactory<T> factory) {
      this.root.interceptor = interceptor;
      this.root.pointFactory = factory;
   }

   public RunInterceptor<?> getInterceptor() {
      return this.root.interceptor;
   }

   public DebugPointFactory<?> getPointFactory() {
      return this.root.pointFactory;
   }

   static Object toJava(Object value) {
      if (value instanceof JsValue jv) {
         return jv.getJavaValue();
      } else if (value instanceof JsFunctionWrapper) {
         return value;
      } else {
         return value instanceof JsFunction fn ? new JsFunctionWrapper(fn) : value;
      }
   }

   protected Object evalRaw(String text) {
      Node program = this.parser(Resource.text(text)).parse();
      this.checkPoisoned();
      boolean outermost = this.enterEvalScope();
      AsyncScope scope = this.currentScope();

      Object var7;
      try {
         CoreContext context = new CoreContext(this.root, null, 0, program, ContextScope.GLOBAL, this.bindings);
         Object result = this.runLocked(program, context);
         var7 = outermost ? AsyncSupport.finishScope(this, scope, result) : result;
      } finally {
         if (outermost) {
            this.closeScope(scope, "eval complete");
         }

         this.exitEvalScope(outermost);
      }

      return var7;
   }

   private Object runLocked(Node program, CoreContext context) {
      this.jsLock.lock();
      Engine prevEngine = enter(this);

      Object var5;
      try {
         context.event(EventType.CONTEXT_ENTER, program);
         Object result = Interpreter.eval(program, context);
         context.event(EventType.CONTEXT_EXIT, program);
         var5 = result;
      } finally {
         exit(prevEngine);
         if (this.jsLock.isHeldByCurrentThread()) {
            this.jsLock.unlock();
         }
      }

      return var5;
   }

   private Object evalInternal(Resource resource, Map<String, Object> localVars) {
      return this.evalInternal(this.parser(resource).parse(), localVars);
   }

   private Object evalInternal(Node program, Map<String, Object> localVars) {
      this.checkPoisoned();
      boolean outermost = this.enterEvalScope();
      AsyncScope scope = this.currentScope();

      Object var5;
      try {
         var5 = this.evalScoped(program, localVars, scope, outermost);
      } finally {
         if (outermost) {
            this.closeScope(scope, "eval complete");
         }

         this.exitEvalScope(outermost);
      }

      return var5;
   }

   private Object evalScoped(Node program, Map<String, Object> localVars, AsyncScope scope, boolean outermost) {
      try {
         this.root.evalId++;
         CoreContext context;
         if (localVars == null) {
            context = new CoreContext(this.root, null, 0, program, ContextScope.GLOBAL, this.bindings);
         } else {
            CoreContext parent = new CoreContext(this.root, null, -1, new Node(NodeType.ROOT), ContextScope.GLOBAL, this.bindings);
            context = new CoreContext(this.root, parent, 0, program, ContextScope.GLOBAL, new BindingsStore(localVars));
         }

         Object result = this.runLocked(program, context);
         if (outermost) {
            result = AsyncSupport.finishScope(this, scope, result);
         }

         return toJava(result);
      } catch (Throwable var14) {
         if (var14 instanceof FlowControlSignal) {
            throw (RuntimeException)var14;
         } else if (var14 instanceof EngineInterruptedException eie) {
            throw eie;
         } else if (var14 instanceof ParserException pe) {
            throw pe;
         } else {
            String message = var14.getMessage();
            if (message == null) {
               message = var14 + "";
            }

            Resource resource = program.getFirstToken().getResource();
            String relPath = resource.isFile() ? resource.getRelativePath() : null;
            if (relPath != null && !message.endsWith(relPath)) {
               message = message + "\n" + relPath;
            }

            String jsErrorName = var14 instanceof EngineException ee ? ee.getJsErrorName() : null;
            String jsMessage = var14 instanceof EngineException eex ? eex.getJsMessage() : null;
            boolean authored = var14 instanceof EngineException eexx && eexx.isAuthoredThrow();
            Object thrownValue = authored ? ((EngineException)var14).getThrownValue() : null;
            int throwLine = authored ? ((EngineException)var14).getThrowLine() : 0;
            throw new EngineException(message, var14, jsErrorName, jsMessage, authored, thrownValue, throwLine);
         }
      }
   }
}
