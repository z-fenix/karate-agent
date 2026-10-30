package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class JsPromiseConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;
   private final JsCallable resolveDelegate = this::resolveStatic;

   JsPromiseConstructor() {
      this.name = "Promise";
      this.length = 1;
      this.defineOwn("prototype", JsPromisePrototype.INSTANCE, (byte)8);
      this.defineOwn("resolve", new JsBuiltinMethod("resolve", 1, this.resolveDelegate), (byte)13);
      this.defineOwn("reject", new JsBuiltinMethod("reject", 1, this::rejectStatic), (byte)13);
      this.defineOwn("all", new JsBuiltinMethod("all", 1, this::all), (byte)13);
      this.defineOwn("allSettled", new JsBuiltinMethod("allSettled", 1, this::allSettled), (byte)13);
      this.defineOwn("race", new JsBuiltinMethod("race", 1, this::race), (byte)13);
      this.defineOwn("any", new JsBuiltinMethod("any", 1, this::any), (byte)13);
   }

   private static Engine engineOf(Context context) {
      Engine engine = context == null ? null : context.getEngine();
      if (engine == null) {
         engine = Engine.current();
      }

      if (engine == null) {
         throw JsErrorException.typeError("Promise requires an engine");
      } else {
         return engine;
      }
   }

   private static AsyncScope scopeOf(Engine engine) {
      AsyncScope scope = engine.currentScope();
      if (scope == null) {
         throw JsErrorException.typeError("Promise requires an active evaluation");
      } else {
         return scope;
      }
   }

   static JsPromise newPromise(Context context) {
      Engine engine = engineOf(context);
      AsyncActivation.anyAsync = true;
      return new JsPromise(engine, scopeOf(engine));
   }

   @Override
   public Object call(Context context, Object[] args) {
      CallInfo callInfo = context.getCallInfo();
      if (callInfo != null && callInfo.constructor) {
         if (AsyncSupport.arg(args, 0) instanceof JsCallable callable) {
            JsPromise promise = newPromise(context);
            AtomicBoolean claimed = new AtomicBoolean();
            JsCallable resolveFn = (ctx, a) -> {
               if (claimed.compareAndSet(false, true)) {
                  AsyncSupport.settleFromCallback(promise, AsyncSupport.arg(a, 0), false);
               }

               return Terms.UNDEFINED;
            };
            JsCallable rejectFn = (ctx, a) -> {
               if (claimed.compareAndSet(false, true)) {
                  AsyncSupport.settleFromCallback(promise, AsyncSupport.arg(a, 0), true);
               }

               return Terms.UNDEFINED;
            };
            AsyncSupport.Completion outcome = AsyncSupport.invoke(promise.engine, callable, new Object[]{resolveFn, rejectFn}, null);
            if (outcome.threw() && claimed.compareAndSet(false, true)) {
               promise.settle(true, outcome.reason(), null);
            }

            return promise;
         } else {
            throw JsErrorException.typeError("Promise resolver is not a function");
         }
      } else {
         throw JsErrorException.typeError("Promise constructor cannot be invoked without 'new'");
      }
   }

   private Object resolveStatic(Context context, Object[] args) {
      Object value = AsyncSupport.arg(args, 0);
      if (value instanceof JsPromise promise) {
         return promise;
      } else if (value instanceof CompletionStage<?> stage) {
         Engine engine = engineOf(context);
         return AsyncSupport.wrapStage(engine, scopeOf(engine), stage);
      } else {
         JsPromise promise = newPromise(context);
         AsyncSupport.resolveValue(promise, value, null);
         return promise;
      }
   }

   private Object rejectStatic(Context context, Object[] args) {
      JsPromise promise = newPromise(context);
      promise.settle(true, AsyncSupport.arg(args, 0), null);
      return promise;
   }

   private static void checkAbrupt(Context context) {
      if (context instanceof CoreContext cc && cc.isError()) {
         Object reason = cc.getErrorThrown();
         cc.reset();
         throw new JsPromiseConstructor.Abrupt(reason);
      }
   }

   private static void closeQuietly(JsIterator iter, Context context) {
      try {
         iter.close(context, true);
      } catch (RuntimeException var3) {
         if (var3 instanceof FlowControlSignal || AsyncSupport.isHostCancellation(var3)) {
            throw var3;
         }
      }

      if (context instanceof CoreContext cc && cc.isStopped()) {
         cc.reset();
      }
   }

   private static Object callWithThis(Context context, JsCallable fn, Object thisObj, Object[] args) {
      if (context instanceof CoreContext cc) {
         Object savedThis = cc.thisObject;
         cc.thisObject = thisObj;

         Object var6;
         try {
            var6 = fn.call(cc, args);
         } finally {
            cc.thisObject = savedThis;
         }

         return var6;
      } else {
         return fn.call(context, args);
      }
   }

   private static CoreContext coreOf(Context context) {
      return context instanceof CoreContext cc ? cc : null;
   }

   private ObjectLike constructorOf(Context context) {
      Object thisValue = context.getThisObject();
      if (thisValue != null && thisValue != Terms.UNDEFINED && thisValue != this) {
         if (thisValue instanceof ObjectLike obj && thisValue instanceof JsCallable callable && callable.isConstructable()) {
            return obj;
         } else {
            throw JsErrorException.typeError("Promise combinator called on a non-constructor");
         }
      } else {
         return this;
      }
   }

   private static JsPromise asPromise(Context context, Engine engine, AsyncScope scope, Object value, boolean intrinsic) {
      Object thenFn = null;
      if (value instanceof ObjectLike obj) {
         thenFn = obj.getMember("then", obj, coreOf(context));
         checkAbrupt(context);
      }

      if (value instanceof JsPromise promise && JsPromisePrototype.isIntrinsicThen(thenFn)) {
         return promise;
      } else {
         JsPromise wrapper = new JsPromise(engine, scope);
         if (thenFn instanceof JsCallable thenCallable) {
            AtomicBoolean claimed = new AtomicBoolean();
            JsCallable resolveFn = (ctx, a) -> {
               if (claimed.compareAndSet(false, true)) {
                  AsyncSupport.settleFromCallback(wrapper, AsyncSupport.arg(a, 0), false);
               }

               return Terms.UNDEFINED;
            };
            JsCallable rejectFn = (ctx, a) -> {
               if (claimed.compareAndSet(false, true)) {
                  AsyncSupport.settleFromCallback(wrapper, AsyncSupport.arg(a, 0), true);
               }

               return Terms.UNDEFINED;
            };
            callWithThis(context, thenCallable, value, new Object[]{resolveFn, rejectFn});
            checkAbrupt(context);
            return wrapper;
         } else if (!intrinsic) {
            throw JsErrorException.typeError("Promise resolve did not return a thenable");
         } else if (value instanceof ObjectLike) {
            wrapper.settle(false, value, null);
            return wrapper;
         } else {
            AsyncSupport.resolveValue(wrapper, value, null);
            return wrapper;
         }
      }
   }

   private List<JsPromise> collect(Context context, Object[] args, JsPromise result) {
      Engine engine = engineOf(context);
      AsyncScope scope = scopeOf(engine);
      AsyncActivation.anyAsync = true;
      ObjectLike receiver = this.constructorOf(context);
      Object iterable = AsyncSupport.arg(args, 0);
      List<JsPromise> promises = new ArrayList<>();
      JsIterator iter = null;

      try {
         Object resolveFn = receiver.getMember("resolve", receiver, coreOf(context));
         checkAbrupt(context);
         if (!(resolveFn instanceof JsCallable resolveCallable)) {
            throw JsErrorException.typeError("Promise resolve is not a function");
         } else {
            boolean intrinsic = receiver == this && resolveFn instanceof JsBuiltinMethod m && m.delegate() == this.resolveDelegate;
            iter = IterUtils.getIterator(iterable, context);
            checkAbrupt(context);

            while (iter.hasNext()) {
               Object item = iter.next();
               checkAbrupt(context);
               Object next = intrinsic ? this.resolveStatic(context, new Object[]{item}) : callWithThis(context, resolveCallable, receiver, new Object[]{item});
               checkAbrupt(context);
               promises.add(asPromise(context, engine, scope, next, intrinsic));
            }

            checkAbrupt(context);
            return promises;
         }
      } catch (RuntimeException var15) {
         if (!(var15 instanceof FlowControlSignal) && !AsyncSupport.isHostCancellation(var15)) {
            if (iter != null) {
               closeQuietly(iter, context);
            }

            result.settle(true, var15 instanceof JsPromiseConstructor.Abrupt abrupt ? abrupt.reason : AsyncSupport.reasonOf(var15), null);
            return null;
         } else {
            throw var15;
         }
      }
   }

   private Object all(Context context, Object[] args) {
      JsPromise result = newPromise(context);
      List<JsPromise> promises = this.collect(context, args, result);
      if (promises == null) {
         return result;
      } else {
         int total = promises.size();
         if (total == 0) {
            AsyncSupport.resolveValue(result, new JsArray(new ArrayList<>()), null);
            return result;
         } else {
            List<Object> values = new ArrayList<>(Collections.nCopies(total, Terms.UNDEFINED));
            AtomicInteger remaining = new AtomicInteger(total);

            for (int i = 0; i < total; i++) {
               int index = i;
               promises.get(i).addReaction((rejected, value) -> {
                  if (rejected) {
                     result.settle(true, value, null);
                  } else {
                     values.set(index, value);
                     if (remaining.decrementAndGet() == 0) {
                        result.settle(false, new JsArray(values), null);
                     }
                  }
               });
            }

            return result;
         }
      }
   }

   private Object allSettled(Context context, Object[] args) {
      JsPromise result = newPromise(context);
      List<JsPromise> promises = this.collect(context, args, result);
      if (promises == null) {
         return result;
      } else {
         int total = promises.size();
         if (total == 0) {
            result.settle(false, new JsArray(new ArrayList<>()), null);
            return result;
         } else {
            List<Object> values = new ArrayList<>(Collections.nCopies(total, Terms.UNDEFINED));
            AtomicInteger remaining = new AtomicInteger(total);

            for (int i = 0; i < total; i++) {
               int index = i;
               promises.get(i).addReaction((rejected, value) -> {
                  JsObject entry = new JsObject();
                  if (rejected) {
                     entry.putMember("status", "rejected");
                     entry.putMember("reason", value);
                  } else {
                     entry.putMember("status", "fulfilled");
                     entry.putMember("value", value);
                  }

                  values.set(index, entry);
                  if (remaining.decrementAndGet() == 0) {
                     result.settle(false, new JsArray(values), null);
                  }
               });
            }

            return result;
         }
      }
   }

   private Object race(Context context, Object[] args) {
      JsPromise result = newPromise(context);
      List<JsPromise> promises = this.collect(context, args, result);
      if (promises == null) {
         return result;
      } else {
         for (JsPromise promise : promises) {
            promise.addReaction((rejected, value) -> {
               if (rejected) {
                  result.settle(true, value, null);
               } else {
                  AsyncSupport.resolveValue(result, value, null);
               }
            });
         }

         return result;
      }
   }

   private Object any(Context context, Object[] args) {
      JsPromise result = newPromise(context);
      List<JsPromise> promises = this.collect(context, args, result);
      if (promises == null) {
         return result;
      } else {
         int total = promises.size();
         if (total == 0) {
            result.settle(true, aggregateError(new ArrayList<>()), null);
            return result;
         } else {
            List<Object> errors = new ArrayList<>(Collections.nCopies(total, Terms.UNDEFINED));
            AtomicInteger remaining = new AtomicInteger(total);

            for (int i = 0; i < total; i++) {
               int index = i;
               promises.get(i).addReaction((rejected, value) -> {
                  if (!rejected) {
                     result.settle(false, value, null);
                  } else {
                     errors.set(index, value);
                     if (remaining.decrementAndGet() == 0) {
                        result.settle(true, aggregateError(errors), null);
                     }
                  }
               });
            }

            return result;
         }
      }
   }

   private static JsError aggregateError(List<Object> errors) {
      JsError error = new JsError(JsErrorPrototype.AGGREGATE_ERROR);
      byte attrs = 5;
      error.defineOwn("message", "All promises were rejected", attrs);
      error.defineOwn("errors", new JsArray(errors), attrs);
      return error;
   }

   private static final class Abrupt extends RuntimeException {
      final transient Object reason;

      Abrupt(Object reason) {
         super(null, null, false, false);
         this.reason = reason;
      }
   }
}
