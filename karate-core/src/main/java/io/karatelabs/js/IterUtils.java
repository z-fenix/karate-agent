package io.karatelabs.js;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class IterUtils {
   public static final String SYMBOL_ITERATOR = "@@iterator";
   public static final JsCallable SYMBOL_ITERATOR_METHOD = (ctx, args) -> {
      Object thisObj = ctx.getThisObject();
      JsIterator iter = getIterator(thisObj, ctx);
      return toIteratorObject(iter);
   };
   private static final Object[] EMPTY_ARGS = new Object[0];

   private IterUtils() {
   }

   public static JsIterator getIterator(Object source, Context context) {
      JsIterator iter = tryGetIterator(source, context);
      if (iter == null) {
         throw JsErrorException.typeError(describe(source) + " is not iterable");
      } else {
         return iter;
      }
   }

   public static JsIterator tryGetIterator(Object source, Context context) {
      if (source == null || source == Terms.UNDEFINED) {
         return null;
      } else if (source instanceof JsArray jsArray) {
         Object fn = readMember(jsArray, "@@iterator", context);
         if (fn instanceof JsBuiltinMethod m && m.delegate() == SYMBOL_ITERATOR_METHOD) {
            return listIterator(jsArray.list);
         } else if (isJsErrored(context)) {
            return exhaustedIterator();
         } else {
            return fn instanceof JsCallable callable ? userIterator(callable, jsArray, context) : null;
         }
      } else if (source instanceof JsString jsString) {
         return stringIterator(jsString.text);
      } else if (source instanceof String s) {
         return stringIterator(s);
      } else if (source instanceof List<?> list) {
         return listIterator((List<Object>)list);
      } else {
         JsArray asArray = Terms.toJsArray(source);
         if (asArray != null) {
            return listIterator(asArray.list);
         } else {
            if (source instanceof ObjectLike obj) {
               Object iteratorFn = readMember(obj, "@@iterator", context);
               if (isJsErrored(context)) {
                  return exhaustedIterator();
               }

               if (iteratorFn instanceof JsCallable callable) {
                  return userIterator(callable, obj, context);
               }
            }

            return null;
         }
      }
   }

   public static boolean isIterable(Object source, Context context) {
      return tryGetIterator(source, context) != null;
   }

   public static JsIterator iteratorFromCallable(JsCallable factory, ObjectLike receiver, Context context) {
      Object result;
      if (context instanceof CoreContext cc) {
         Object savedThis = cc.thisObject;
         cc.thisObject = receiver;

         try {
            result = factory.call(cc, EMPTY_ARGS);
         } finally {
            cc.thisObject = savedThis;
         }
      } else {
         result = factory.call(context, EMPTY_ARGS);
      }

      if (result != null && result != Terms.UNDEFINED) {
         if (result instanceof ObjectLike obj) {
            Object nextFn = obj.getMember("next");
            if (nextFn instanceof JsCallable) {
               return iteratorObjectWalker(obj, context);
            }
         }

         return getIterator(result, context);
      } else {
         throw JsErrorException.typeError("Result of iterator method is not an object");
      }
   }

   private static JsIterator iteratorObjectWalker(final ObjectLike iterObj, final Context context) {
      return new JsIterator() {
         Object pending;
         boolean fetched;
         boolean done;

         private void fetch() {
            if (!this.fetched && !this.done) {
               if (iterObj.getMember("next") instanceof JsCallable nextCallable) {
                  Object step;
                  if (context instanceof CoreContext cc) {
                     Object savedThis = cc.thisObject;
                     cc.thisObject = iterObj;

                     try {
                        step = nextCallable.call(cc, IterUtils.EMPTY_ARGS);
                     } finally {
                        cc.thisObject = savedThis;
                     }
                  } else {
                     step = nextCallable.call(context, IterUtils.EMPTY_ARGS);
                  }

                  if (IterUtils.isJsErrored(context)) {
                     this.done = true;
                  } else if (step instanceof ObjectLike stepObj) {
                     if (Terms.isTruthy(IterUtils.readMember(stepObj, "done", context))) {
                        this.done = true;
                     } else if (IterUtils.isJsErrored(context)) {
                        this.done = true;
                     } else {
                        this.pending = IterUtils.readMember(stepObj, "value", context);
                        if (this.pending == null && !IterUtils.hasProperty(stepObj, "value")) {
                           this.pending = Terms.UNDEFINED;
                        }

                        if (IterUtils.isJsErrored(context)) {
                           this.done = true;
                        } else {
                           this.fetched = true;
                        }
                     }
                  } else {
                     throw JsErrorException.typeError("iterator result is not an object");
                  }
               } else {
                  throw JsErrorException.typeError("iterator.next is not a function");
               }
            }
         }

         @Override
         public boolean hasNext() {
            this.fetch();
            return !this.done;
         }

         @Override
         public Object next() {
            this.fetch();
            if (this.done) {
               throw new NoSuchElementException();
            } else {
               this.fetched = false;
               Object v = this.pending;
               this.pending = null;
               return v;
            }
         }
      };
   }

   public static JsObject toIteratorObject(JsIterator iter) {
      JsObject result = new JsObject();
      JsCallable nextFn = (ctx, args) -> {
         JsObject step = new JsObject(new LinkedHashMap<>(2));
         if (iter.hasNext()) {
            step.putMember("value", iter.next());
            step.putMember("done", false);
         } else {
            step.putMember("value", Terms.UNDEFINED);
            step.putMember("done", true);
         }

         return step;
      };
      result.putMember("next", nextFn);
      result.putMember("@@iterator", (JsCallable)(ctx, args) -> result);
      return result;
   }

   static JsIterator listIterator(final List<Object> list) {
      return new JsIterator() {
         int i = 0;

         @Override
         public boolean hasNext() {
            return this.i < list.size();
         }

         @Override
         public Object next() {
            if (this.i >= list.size()) {
               throw new NoSuchElementException();
            } else {
               return JsArray.unwrapHole(list.get(this.i++));
            }
         }
      };
   }

   private static JsIterator stringIterator(final String text) {
      return new JsIterator() {
         int i = 0;

         @Override
         public boolean hasNext() {
            return this.i < text.length();
         }

         @Override
         public Object next() {
            if (this.i >= text.length()) {
               throw new NoSuchElementException();
            } else {
               int cp = text.codePointAt(this.i);
               int start = this.i;
               this.i = this.i + Character.charCount(cp);
               return text.substring(start, this.i);
            }
         }
      };
   }

   private static JsIterator userIterator(JsCallable iteratorFn, ObjectLike receiver, final Context context) {
      Object iter;
      if (context instanceof CoreContext cc) {
         Object savedThis = cc.thisObject;
         cc.thisObject = receiver;

         try {
            iter = iteratorFn.call(cc, EMPTY_ARGS);
         } finally {
            cc.thisObject = savedThis;
         }
      } else {
         iter = iteratorFn.call(context, EMPTY_ARGS);
      }

      if (isJsErrored(context)) {
         return exhaustedIterator();
      } else if (iter instanceof ObjectLike iterObj) {
         return new JsIterator() {
            Object pending;
            boolean fetched;
            boolean done;

            private void fetch() {
               if (!this.fetched && !this.done) {
                  if (iterObj.getMember("next") instanceof JsCallable nextCallable) {
                     Object step;
                     if (context instanceof CoreContext cc) {
                        Object savedThis = cc.thisObject;
                        cc.thisObject = iterObj;

                        try {
                           step = nextCallable.call(cc, IterUtils.EMPTY_ARGS);
                        } finally {
                           cc.thisObject = savedThis;
                        }
                     } else {
                        step = nextCallable.call(context, IterUtils.EMPTY_ARGS);
                     }

                     if (IterUtils.isJsErrored(context)) {
                        this.done = true;
                     } else if (step instanceof ObjectLike stepObj) {
                        if (Terms.isTruthy(IterUtils.readMember(stepObj, "done", context))) {
                           this.done = true;
                        } else if (IterUtils.isJsErrored(context)) {
                           this.done = true;
                        } else {
                           this.pending = IterUtils.readMember(stepObj, "value", context);
                           if (this.pending == null && !IterUtils.hasProperty(stepObj, "value")) {
                              this.pending = Terms.UNDEFINED;
                           }

                           if (IterUtils.isJsErrored(context)) {
                              this.done = true;
                           } else {
                              this.fetched = true;
                           }
                        }
                     } else {
                        throw JsErrorException.typeError("iterator result is not an object");
                     }
                  } else {
                     throw JsErrorException.typeError("iterator.next is not a function");
                  }
               }
            }

            @Override
            public boolean hasNext() {
               this.fetch();
               return !this.done;
            }

            @Override
            public Object next() {
               this.fetch();
               if (this.done) {
                  throw new NoSuchElementException();
               } else {
                  this.fetched = false;
                  Object v = this.pending;
                  this.pending = null;
                  return v;
               }
            }

            @Override
            public void close(Context ctx, boolean dueToError) {
               if (!this.done) {
                  this.done = true;
                  boolean hadError = IterUtils.isJsErrored(ctx);
                  Object savedError = hadError ? ((CoreContext)ctx).getErrorThrown() : null;
                  if (hadError) {
                     ((CoreContext)ctx).reset();
                  }

                  try {
                     Object retFn = IterUtils.readMember(iterObj, "return", ctx);
                     if (IterUtils.isJsErrored(ctx)) {
                        if (dueToError) {
                           IterUtils.clearError(ctx);
                        }

                        return;
                     }

                     if (retFn == null || retFn == Terms.UNDEFINED) {
                        return;
                     }

                     if (!(retFn instanceof JsCallable retCallable)) {
                        if (dueToError) {
                           return;
                        }

                        throw JsErrorException.typeError("iterator 'return' is not a function");
                     }

                     Object result;
                     if (ctx instanceof CoreContext cc) {
                        Object savedThis = cc.thisObject;
                        cc.thisObject = iterObj;

                        try {
                           result = retCallable.call(cc, IterUtils.EMPTY_ARGS);
                        } finally {
                           cc.thisObject = savedThis;
                        }
                     } else {
                        result = retCallable.call(ctx, IterUtils.EMPTY_ARGS);
                     }

                     if (!IterUtils.isJsErrored(ctx)) {
                        if (!dueToError && !(result instanceof ObjectLike)) {
                           throw JsErrorException.typeError("iterator 'return' result is not an object");
                        }

                        return;
                     }

                     if (dueToError) {
                        IterUtils.clearError(ctx);
                     }
                  } finally {
                     if (hadError) {
                        ((CoreContext)ctx).stopAndThrow(savedError);
                     }
                  }
               }
            }
         };
      } else {
         throw JsErrorException.typeError("Result of the Symbol.iterator method is not an object");
      }
   }

   private static void clearError(Context ctx) {
      if (ctx instanceof CoreContext cc && cc.isError()) {
         cc.reset();
      }
   }

   private static JsIterator exhaustedIterator() {
      return new JsIterator() {
         @Override
         public boolean hasNext() {
            return false;
         }

         @Override
         public Object next() {
            throw new NoSuchElementException();
         }
      };
   }

   private static boolean isJsErrored(Context context) {
      return context instanceof CoreContext cc && cc.isError();
   }

   private static boolean hasProperty(ObjectLike obj, String name) {
      for (ObjectLike o = obj; o != null; o = o.getPrototype()) {
         if (o.isOwnProperty(name)) {
            return true;
         }
      }

      return false;
   }

   private static Object readMember(ObjectLike obj, String name, Context context) {
      CoreContext cc = context instanceof CoreContext c ? c : null;
      return obj.getMember(name, obj, cc);
   }

   private static String describe(Object source) {
      if (source == null) {
         return "null";
      } else {
         return source == Terms.UNDEFINED ? "undefined" : Terms.toStringCoerce(source, null);
      }
   }
}
