package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class JsArrayPrototype extends Prototype {
   static final JsArrayPrototype INSTANCE = new JsArrayPrototype();
   static final JsCallable DEFAULT_TO_STRING = (context, args) -> INSTANCE.join(context, new Object[0]);
   private static final String LENGTH_NON_WRITABLE = "Cannot assign to read only property 'length' of object '[object Array]'";
   private static final Object[] EMPTY_ARGS = new Object[0];

   private JsArrayPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("toString", DEFAULT_TO_STRING);
      this.install("map", 1, this::map);
      this.install("filter", 1, this::filter);
      this.install("join", 1, this::join);
      this.install("find", 1, this::find);
      this.install("findIndex", 1, this::findIndex);
      this.install("push", 1, this::push);
      this.install("reverse", 0, this::reverse);
      this.install("includes", 1, this::includes);
      this.install("indexOf", 1, this::indexOf);
      this.install("slice", 2, this::slice);
      this.install("forEach", 1, this::forEach);
      this.install("concat", 1, this::concat);
      this.install("every", 1, this::every);
      this.install("some", 1, this::some);
      this.install("reduce", 1, this::reduce);
      this.install("reduceRight", 1, this::reduceRight);
      this.install("flat", 0, this::flat);
      this.install("flatMap", 1, this::flatMap);
      this.install("sort", 1, this::sort);
      this.install("fill", 1, this::fill);
      this.install("splice", 2, this::splice);
      this.install("shift", 0, this::shift);
      this.install("unshift", 1, this::unshift);
      this.install("lastIndexOf", 1, this::lastIndexOf);
      this.install("pop", 0, this::pop);
      this.install("at", 1, this::at);
      this.install("copyWithin", 2, this::copyWithin);
      this.install("keys", 0, this::keys);
      this.install("values", 0, this::values);
      this.install("entries", 0, this::entries);
      this.install("findLast", 1, this::findLast);
      this.install("findLastIndex", 1, this::findLastIndex);
      this.install("with", 2, this::withMethod);
      this.install("toReversed", 0, this::toReversed);
      this.install("toSorted", 1, this::toSorted);
      this.install("toSpliced", 2, this::toSpliced);
      this.install("group", 1, this::group);
      this.installConstructor("Array");
      this.install("@@iterator", 0, IterUtils.SYMBOL_ITERATOR_METHOD);
   }

   private static List<Object> rawList(Context context) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsArray arr) {
         if (!arr.hasAnyDescriptor()) {
            return arr.toList();
         } else {
            CoreContext cc = context instanceof CoreContext cx ? cx : null;
            int len = arr.size();
            List<Object> snapshot = new ArrayList<>(len);

            for (int i = 0; i < len; i++) {
               Object v = arr.getIndexedValue(i, arr, cc);
               snapshot.add(v == null ? Terms.UNDEFINED : v);
            }

            return snapshot;
         }
      } else if (thisObj instanceof List<?> list) {
         return (List<Object>)list;
      } else {
         ObjectLike ol = Terms.toObjectLike(thisObj);
         if (ol instanceof JsArray arrx) {
            return arrx.toList();
         } else if (ol != null && ol.getMember("length") instanceof Number n) {
            int len = Math.max(n.intValue(), 0);
            List<Object> snapshot = new ArrayList<>(len);
            CoreContext cc = context instanceof CoreContext cx ? cx : null;

            for (int i = 0; i < len; i++) {
               Object v = ol.getMember(String.valueOf(i), ol, cc);
               snapshot.add(v == null ? Terms.UNDEFINED : v);
            }

            return snapshot;
         } else {
            return new ArrayList<>();
         }
      }
   }

   private static Iterable<KeyValue> jsEntries(Context context) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsArray arr && !arr.hasAnyDescriptor()) {
         return arr.jsEntries();
      } else if (Terms.toObjectLike(thisObj) instanceof JsArray arr && !arr.hasAnyDescriptor()) {
         return arr.jsEntries();
      } else {
         List<Object> list = rawList(context);
         return () -> new Iterator<KeyValue>() {
            int index = 0;

            @Override
            public boolean hasNext() {
               return this.index < list.size();
            }

            public KeyValue next() {
               int i = this.index++;
               return new KeyValue(thisObj, i, i + "", list.get(i));
            }
         };
      }
   }

   private static void setLength(ObjectLike target, int newLen, CoreContext ctx) {
      if (target instanceof JsArray arr) {
         if (!arr.handleLengthAssign(newLen, ctx)) {
            throw JsErrorException.typeError("Cannot assign to read only property 'length' of object '[object Array]'");
         }
      } else {
         PropertyAccess.setByName(target, "length", newLen, ctx, null);
      }
   }

   private static Object specGet(ObjectLike target, String name, CoreContext ctx) {
      Object v = target.getMember(name, target, ctx);
      return v == null ? Terms.UNDEFINED : v;
   }

   private static void specSet(ObjectLike target, String name, Object value, CoreContext ctx) {
      PropertyAccess.setByName(target, name, value, ctx, null);
   }

   private static void specDelete(ObjectLike target, String name) {
      target.removeMember(name);
   }

   private static boolean hasPropertyChain(ObjectLike obj, String name) {
      for (ObjectLike o = obj; o != null; o = o.getPrototype()) {
         if (o.isOwnProperty(name)) {
            return true;
         }
      }

      return false;
   }

   private static void specIterate(Context context, boolean ascending, boolean skipAbsent, JsArrayPrototype.IndexVisitor visitor) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      int len = lengthOf(target, cc);
      boolean clean = target.getClass() == JsArray.class
         && !((JsArray)target).hasAnyDescriptor()
         && ((JsArray)target).getPrototype() == INSTANCE
         && !Prototype.isNumericPropPolluted();
      if (clean) {
         JsArray arr = (JsArray)target;
         List<Object> list = arr.list;
         if (ascending) {
            for (int k = 0; k < len; k++) {
               Object v = k < list.size() ? list.get(k) : JsArray.HOLE;
               if (v == JsArray.HOLE) {
                  if (skipAbsent) {
                     continue;
                  }

                  v = Terms.UNDEFINED;
               }

               if (!visitor.visit(k, v)) {
                  return;
               }
            }
         } else {
            for (int k = len - 1; k >= 0; k--) {
               Object vx = k < list.size() ? list.get(k) : JsArray.HOLE;
               if (vx == JsArray.HOLE) {
                  if (skipAbsent) {
                     continue;
                  }

                  vx = Terms.UNDEFINED;
               }

               if (!visitor.visit(k, vx)) {
                  return;
               }
            }
         }
      } else {
         if (ascending) {
            for (int k = 0; k < len; k++) {
               if (!visitOneSlow(target, k, skipAbsent, cc, visitor)) {
                  return;
               }
            }
         } else {
            for (int kx = len - 1; kx >= 0; kx--) {
               if (!visitOneSlow(target, kx, skipAbsent, cc, visitor)) {
                  return;
               }
            }
         }
      }
   }

   private static boolean visitOneSlow(ObjectLike target, int k, boolean skipAbsent, CoreContext cc, JsArrayPrototype.IndexVisitor visitor) {
      String key = String.valueOf(k);
      boolean present = hasPropertyChain(target, key);
      if (!present) {
         return skipAbsent ? true : visitor.visit(k, Terms.UNDEFINED);
      } else {
         Object v = target.getMember(key, target, cc);
         if (v == null) {
            v = Terms.UNDEFINED;
         }

         return visitor.visit(k, v);
      }
   }

   private static int lengthOf(ObjectLike target, CoreContext ctx) {
      if (target instanceof JsArray arr) {
         return arr.size();
      } else {
         Object lenObj = target.getMember("length", target, ctx);
         if (lenObj != null && lenObj != Terms.UNDEFINED) {
            Number n = lenObj instanceof ObjectLike ? Terms.toNumberCoerce(lenObj, ctx) : Terms.objectToNumber(lenObj);
            if (ctx != null && ctx.isError()) {
               return 0;
            } else {
               double d = n == null ? Double.NaN : n.doubleValue();
               if (Double.isNaN(d) || d <= 0.0) {
                  return 0;
               } else {
                  return d >= 2.147483647E9 ? Integer.MAX_VALUE : (int)d;
               }
            }
         } else {
            return 0;
         }
      }
   }

   private static int checkResultLength(int len) {
      if (len == Integer.MAX_VALUE) {
         throw JsErrorException.rangeError("Invalid array length");
      } else {
         return len;
      }
   }

   private static void checkLengthLimit(ObjectLike target, CoreContext ctx, int addCount) {
      Object lenObj = target.getMember("length", target, ctx);
      if (lenObj != null && lenObj != Terms.UNDEFINED) {
         Number n = lenObj instanceof ObjectLike ? Terms.toNumberCoerce(lenObj, ctx) : Terms.objectToNumber(lenObj);
         if (ctx == null || !ctx.isError()) {
            double d = n == null ? Double.NaN : n.doubleValue();
            if (d + addCount > 9.007199254740991E15) {
               throw JsErrorException.typeError("Invalid array length");
            }
         }
      }
   }

   private static ObjectLike toReceiver(Object thisObj) {
      if (thisObj != null && thisObj != Terms.UNDEFINED) {
         ObjectLike o = thisObj instanceof ObjectLike ol ? ol : Terms.toObjectLike(thisObj);
         if (o == null) {
            throw JsErrorException.typeError("Array.prototype.* called on null or undefined");
         } else {
            return o;
         }
      } else {
         throw JsErrorException.typeError("Array.prototype.* called on null or undefined");
      }
   }

   private static JsCallable requireCallable(Object[] args, String methodName) {
      if (args.length > 0 && args[0] instanceof JsCallable callable) {
         return callable;
      } else {
         throw JsErrorException.typeError(methodName + " callback is not a function");
      }
   }

   private static JsCallable bindThis(JsCallable inner, Object[] methodArgs, int thisArgIdx) {
      if (methodArgs.length <= thisArgIdx) {
         return inner;
      } else {
         Object boundThis = methodArgs[thisArgIdx] == null ? Terms.UNDEFINED : methodArgs[thisArgIdx];
         return (ctx, args) -> {
            if (ctx instanceof CoreContext core) {
               Object saved = core.thisObject;
               core.thisObject = boundThis;

               Object var6;
               try {
                  var6 = inner.call(ctx, args);
               } finally {
                  core.thisObject = saved;
               }

               return var6;
            } else {
               return inner.call(ctx, args);
            }
         };
      }
   }

   private static void flatten(List<Object> source, List<Object> result, int depth) {
      for (Object item : source) {
         if (item != JsArray.HOLE) {
            if (depth > 0 && item instanceof List<?> list) {
               flatten((List<Object>)list, result, depth - 1);
            } else {
               result.add(item);
            }
         }
      }
   }

   private Object map(Context context, Object[] args) {
      Object thisObj = context.getThisObject();
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = thisObj instanceof ObjectLike o ? o : Terms.toObjectLike(thisObj);
      int len = target == null ? 0 : checkResultLength(lengthOf(target, cc));
      List<Object> results = new ArrayList<>(len);

      for (int i = 0; i < len; i++) {
         results.add(JsArray.HOLE);
      }

      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.map"), args, 1);
      specIterate(context, true, true, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         results.set(k, r);
         return true;
      });
      return new JsArray(results);
   }

   private Object filter(Context context, Object[] args) {
      List<Object> results = new ArrayList<>();
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.filter"), args, 1);
      Object thisObj = context.getThisObject();
      specIterate(context, true, true, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            results.add(v);
         }

         return true;
      });
      return new JsArray(results);
   }

   private Object join(Context context, Object[] args) {
      StringBuilder sb = new StringBuilder();
      CoreContext cc = context instanceof CoreContext core ? core : null;
      String delimiter = ",";
      if (args.length > 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         delimiter = Terms.toStringCoerce(args[0], cc);
      }

      List<Object> snapshot = rawList(context);
      int i = 0;

      for (int n = snapshot.size(); i < n; i++) {
         if (i > 0) {
            sb.append(delimiter);
         }

         Object v = snapshot.get(i);
         if (v != null && v != Terms.UNDEFINED && v != JsArray.HOLE) {
            sb.append(Terms.toStringCoerce(v, cc));
         }
      }

      return sb.toString();
   }

   private Object find(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.find"), args, 1);
      Object thisObj = context.getThisObject();
      Object[] result = new Object[]{Terms.UNDEFINED};
      specIterate(context, true, false, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            result[0] = v;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object findIndex(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.findIndex"), args, 1);
      Object thisObj = context.getThisObject();
      int[] result = new int[]{-1};
      specIterate(context, true, false, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            result[0] = k;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object push(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return 0;
      } else {
         if (target.getClass() == JsArray.class) {
            JsArray arr = (JsArray)target;
            if (!arr.hasAnyDescriptor() && arr.getPrototype() == INSTANCE && !Prototype.isNumericPropPolluted() && arr.denseAppendable()) {
               List<Object> list = arr.list;

               for (Object arg : args) {
                  list.add(arg);
               }

               return list.size();
            }
         }

         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            for (int i = 0; i < args.length; i++) {
               specSet(target, String.valueOf(len + i), args[i], cc);
            }

            int newLen = len + args.length;
            setLength(target, newLen, cc);
            return newLen;
         }
      }
   }

   private Object reverse(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            int middle = len >>> 1;

            for (int lower = 0; lower < middle; lower++) {
               int upper = len - lower - 1;
               String lowerKey = String.valueOf(lower);
               String upperKey = String.valueOf(upper);
               boolean lowerHas = hasPropertyChain(target, lowerKey);
               boolean upperHas = hasPropertyChain(target, upperKey);
               Object lowerVal = lowerHas ? specGet(target, lowerKey, cc) : null;
               Object upperVal = upperHas ? specGet(target, upperKey, cc) : null;
               if (lowerHas && upperHas) {
                  specSet(target, lowerKey, upperVal, cc);
                  specSet(target, upperKey, lowerVal, cc);
               } else if (upperHas) {
                  specSet(target, lowerKey, upperVal, cc);
                  specDelete(target, upperKey);
               } else if (lowerHas) {
                  specDelete(target, lowerKey);
                  specSet(target, upperKey, lowerVal, cc);
               }
            }

            return target;
         }
      }
   }

   private Object includes(Context context, Object[] args) {
      Object searchElement = args.length > 0 ? args[0] : Terms.UNDEFINED;
      boolean[] found = new boolean[]{false};
      specIterate(context, true, false, (k, v) -> {
         if (Terms.eq(v, searchElement, true)) {
            found[0] = true;
            return false;
         } else {
            return true;
         }
      });
      return found[0];
   }

   private Object indexOf(Context context, Object[] args) {
      if (args.length == 0) {
         return -1;
      } else {
         Object searchElement = args[0];
         Object thisObj = context.getThisObject();
         CoreContext cc = context instanceof CoreContext cx ? cx : null;
         ObjectLike target = thisObj instanceof ObjectLike o ? o : Terms.toObjectLike(thisObj);
         int len = target == null ? 0 : lengthOf(target, cc);
         if (len == 0) {
            return -1;
         } else {
            int fromIndex = 0;
            if (args.length > 1 && args[1] != null) {
               fromIndex = Terms.objectToNumber(args[1]).intValue();
               if (fromIndex < 0) {
                  fromIndex = Math.max(len + fromIndex, 0);
               }
            }

            if (fromIndex >= len) {
               return -1;
            } else {
               int[] result = new int[]{-1};
               int from = fromIndex;
               specIterate(context, true, true, (k, v) -> {
                  if (k < from) {
                     return true;
                  } else if (Terms.strictEq(v, searchElement)) {
                     result[0] = k;
                     return false;
                  } else {
                     return true;
                  }
               });
               return result[0];
            }
         }
      }
   }

   private Object slice(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      int size = thisArray.size();
      int start = 0;
      int end = size;
      if (args.length > 0 && args[0] != null) {
         start = Terms.objectToNumber(args[0]).intValue();
         if (start < 0) {
            start = Math.max(size + start, 0);
         }
      }

      if (args.length > 1 && args[1] != null) {
         end = Terms.objectToNumber(args[1]).intValue();
         if (end < 0) {
            end = Math.max(size + end, 0);
         }
      }

      start = Math.min(start, size);
      end = Math.min(end, size);
      List<Object> result = new ArrayList<>();

      for (int i = start; i < end; i++) {
         result.add(thisArray.get(i));
      }

      return new JsArray(result);
   }

   private Object forEach(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.forEach"), args, 1);
      Object thisObj = context.getThisObject();
      specIterate(context, true, true, (k, v) -> {
         if (context instanceof CoreContext cc) {
            cc.iteration = k;
         }

         callable.call(context, new Object[]{v, k, thisObj});
         return true;
      });
      return Terms.UNDEFINED;
   }

   private Object concat(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      List<Object> result = new ArrayList<>(thisArray);

      for (Object arg : args) {
         if (arg instanceof List<?> list) {
            result.addAll((Collection<? extends Object>)list);
         } else {
            result.add(arg);
         }
      }

      return new JsArray(result);
   }

   private Object every(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.every"), args, 1);
      Object thisObj = context.getThisObject();
      boolean[] result = new boolean[]{true};
      specIterate(context, true, true, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (!Terms.isTruthy(r)) {
            result[0] = false;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object some(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.some"), args, 1);
      Object thisObj = context.getThisObject();
      boolean[] result = new boolean[]{false};
      specIterate(context, true, true, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            result[0] = true;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object reduce(Context context, Object[] args) {
      JsCallable callable = requireCallable(args, "Array.prototype.reduce");
      Object thisObj = context.getThisObject();
      Object[] acc = new Object[1];
      boolean[] hasAcc = new boolean[]{args.length >= 2};
      if (hasAcc[0]) {
         acc[0] = args[1];
      }

      specIterate(context, true, true, (k, v) -> {
         if (!hasAcc[0]) {
            acc[0] = v;
            hasAcc[0] = true;
            return true;
         } else {
            acc[0] = callable.call(context, new Object[]{acc[0], v, k, thisObj});
            return true;
         }
      });
      if (!hasAcc[0]) {
         throw JsErrorException.typeError("Reduce of empty array with no initial value");
      } else {
         return acc[0];
      }
   }

   private Object reduceRight(Context context, Object[] args) {
      JsCallable callable = requireCallable(args, "Array.prototype.reduceRight");
      Object thisObj = context.getThisObject();
      Object[] acc = new Object[1];
      boolean[] hasAcc = new boolean[]{args.length >= 2};
      if (hasAcc[0]) {
         acc[0] = args[1];
      }

      specIterate(context, false, true, (k, v) -> {
         if (!hasAcc[0]) {
            acc[0] = v;
            hasAcc[0] = true;
            return true;
         } else {
            acc[0] = callable.call(context, new Object[]{acc[0], v, k, thisObj});
            return true;
         }
      });
      if (!hasAcc[0]) {
         throw JsErrorException.typeError("Reduce of empty array with no initial value");
      } else {
         return acc[0];
      }
   }

   private Object flat(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      int depth = 1;
      if (args.length > 0 && args[0] != null) {
         Number depthNum = Terms.objectToNumber(args[0]);
         if (!Double.isNaN(depthNum.doubleValue()) && !Double.isInfinite(depthNum.doubleValue())) {
            depth = depthNum.intValue();
         }
      }

      List<Object> result = new ArrayList<>();
      flatten(thisArray, result, depth);
      return new JsArray(result);
   }

   private Object flatMap(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.flatMap"), args, 1);
      Object thisObj = context.getThisObject();
      List<Object> mappedResult = new ArrayList<>();
      specIterate(context, true, true, (k, v) -> {
         Object mapped = callable.call(context, new Object[]{v, k, thisObj});
         if (mapped instanceof List) {
            for (Object item : (List)mapped) {
               if (item != JsArray.HOLE) {
                  mappedResult.add(item);
               }
            }
         } else {
            mappedResult.add(mapped);
         }

         return true;
      });
      return new JsArray(mappedResult);
   }

   private Object sort(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            List<Object> items = new ArrayList<>(len);

            for (int k = 0; k < len; k++) {
               String key = String.valueOf(k);
               if (hasPropertyChain(target, key)) {
                  items.add(specGet(target, key, cc));
               }
            }

            JsCallable comparator;
            JsCallable var15 = comparator = args.length > 0 && args[0] instanceof JsCallable jc ? jc : null;
            items.sort((a, b) -> {
               boolean aUndef = a == null || a == Terms.UNDEFINED;
               boolean bUndef = b == null || b == Terms.UNDEFINED;
               if (aUndef && bUndef) {
                  return 0;
               } else if (aUndef) {
                  return 1;
               } else if (bUndef) {
                  return -1;
               } else if (comparator != null) {
                  if (comparator.call(context, new Object[]{a, b}) instanceof Number n) {
                     double d = n.doubleValue();
                     if (Double.isNaN(d)) {
                        return 0;
                     } else {
                        return d < 0.0 ? -1 : (d > 0.0 ? 1 : 0);
                     }
                  } else {
                     return 0;
                  }
               } else {
                  String sa = Terms.toStringCoerce(a, cc);
                  String sb = Terms.toStringCoerce(b, cc);
                  return sa.compareTo(sb);
               }
            });

            for (int kx = 0; kx < items.size(); kx++) {
               specSet(target, String.valueOf(kx), items.get(kx), cc);
            }

            for (int kx = items.size(); kx < len; kx++) {
               specDelete(target, String.valueOf(kx));
            }

            return target;
         }
      }
   }

   private Object fill(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            Object value = args.length > 0 ? args[0] : Terms.UNDEFINED;
            int start = 0;
            int end = len;
            if (args.length > 1 && args[1] != null) {
               int rel = Terms.objectToNumber(args[1]).intValue();
               start = rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
            }

            if (args.length > 2 && args[2] != null) {
               int rel = Terms.objectToNumber(args[2]).intValue();
               end = rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
            }

            for (int k = start; k < end; k++) {
               specSet(target, String.valueOf(k), value, cc);
            }

            return target;
         }
      }
   }

   private Object splice(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      int len = lengthOf(target, cc);
      if (cc != null && cc.isError()) {
         return Terms.UNDEFINED;
      } else {
         int actualStart;
         if (args.length != 0 && args[0] != null) {
            int relativeStart = Terms.objectToNumber(args[0]).intValue();
            actualStart = relativeStart < 0 ? Math.max(len + relativeStart, 0) : Math.min(relativeStart, len);
         } else {
            actualStart = 0;
         }

         int itemCount;
         int actualDeleteCount;
         if (args.length == 0) {
            actualDeleteCount = 0;
            itemCount = 0;
         } else if (args.length == 1) {
            actualDeleteCount = len - actualStart;
            itemCount = 0;
         } else {
            int dc = args[1] == null ? 0 : Terms.objectToNumber(args[1]).intValue();
            actualDeleteCount = Math.min(Math.max(dc, 0), len - actualStart);
            itemCount = Math.max(args.length - 2, 0);
         }

         if (len == Integer.MAX_VALUE) {
            checkLengthLimit(target, cc, itemCount - actualDeleteCount);
         }

         List<Object> removed = new ArrayList<>(checkResultLength(actualDeleteCount));

         for (int k = 0; k < actualDeleteCount; k++) {
            String from = String.valueOf(actualStart + k);
            removed.add(hasPropertyChain(target, from) ? specGet(target, from, cc) : Terms.UNDEFINED);
         }

         if (itemCount < actualDeleteCount) {
            for (int k = actualStart; k < len - actualDeleteCount; k++) {
               String from = String.valueOf(k + actualDeleteCount);
               String to = String.valueOf(k + itemCount);
               if (hasPropertyChain(target, from)) {
                  specSet(target, to, specGet(target, from, cc), cc);
               } else {
                  specDelete(target, to);
               }
            }

            for (int kx = len; kx > len - actualDeleteCount + itemCount; kx--) {
               specDelete(target, String.valueOf(kx - 1));
            }
         } else if (itemCount > actualDeleteCount) {
            for (int kx = len - actualDeleteCount; kx > actualStart; kx--) {
               String from = String.valueOf(kx + actualDeleteCount - 1);
               String to = String.valueOf(kx + itemCount - 1);
               if (hasPropertyChain(target, from)) {
                  specSet(target, to, specGet(target, from, cc), cc);
               } else {
                  specDelete(target, to);
               }
            }
         }

         for (int kxx = 0; kxx < itemCount; kxx++) {
            specSet(target, String.valueOf(actualStart + kxx), args[kxx + 2], cc);
         }

         setLength(target, len - actualDeleteCount + itemCount, cc);
         return new JsArray(removed);
      }
   }

   private Object toReversed(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            List<Object> result = new ArrayList<>(checkResultLength(len));

            for (int k = 0; k < len; k++) {
               result.add(specGet(target, String.valueOf(len - k - 1), cc));
            }

            return new JsArray(result);
         }
      }
   }

   private Object toSorted(Context context, Object[] args) {
      if (args.length > 0 && args[0] != Terms.UNDEFINED && args[0] != null && !(args[0] instanceof JsCallable)) {
         throw JsErrorException.typeError("toSorted comparator must be a function");
      } else {
         CoreContext cc = context instanceof CoreContext cx ? cx : null;
         ObjectLike target = toReceiver(context.getThisObject());
         if (target == null) {
            return Terms.UNDEFINED;
         } else {
            int len = lengthOf(target, cc);
            if (cc != null && cc.isError()) {
               return Terms.UNDEFINED;
            } else {
               List<Object> items = new ArrayList<>(checkResultLength(len));

               for (int k = 0; k < len; k++) {
                  items.add(specGet(target, String.valueOf(k), cc));
               }

               JsCallable comparator;
               JsCallable var12 = comparator = args.length > 0 && args[0] instanceof JsCallable jc ? jc : null;
               items.sort((a, b) -> {
                  boolean aUndef = a == null || a == Terms.UNDEFINED;
                  boolean bUndef = b == null || b == Terms.UNDEFINED;
                  if (aUndef && bUndef) {
                     return 0;
                  } else if (aUndef) {
                     return 1;
                  } else if (bUndef) {
                     return -1;
                  } else if (comparator != null) {
                     if (comparator.call(context, new Object[]{a, b}) instanceof Number n) {
                        double d = n.doubleValue();
                        if (Double.isNaN(d)) {
                           return 0;
                        } else {
                           return d < 0.0 ? -1 : (d > 0.0 ? 1 : 0);
                        }
                     } else {
                        return 0;
                     }
                  } else {
                     String sa = Terms.toStringCoerce(a, cc);
                     String sb = Terms.toStringCoerce(b, cc);
                     return sa.compareTo(sb);
                  }
               });
               return new JsArray(items);
            }
         }
      }
   }

   private Object toSpliced(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            int actualStart;
            if (args.length != 0 && args[0] != null) {
               int relativeStart = Terms.objectToNumber(args[0]).intValue();
               actualStart = relativeStart < 0 ? Math.max(len + relativeStart, 0) : Math.min(relativeStart, len);
            } else {
               actualStart = 0;
            }

            int itemCount;
            int actualSkipCount;
            if (args.length == 0) {
               actualSkipCount = 0;
               itemCount = 0;
            } else if (args.length == 1) {
               actualSkipCount = len - actualStart;
               itemCount = 0;
            } else {
               int sc = args[1] == null ? 0 : Terms.objectToNumber(args[1]).intValue();
               actualSkipCount = Math.min(Math.max(sc, 0), len - actualStart);
               itemCount = Math.max(args.length - 2, 0);
            }

            int newLen = checkResultLength(len - actualSkipCount + itemCount);
            List<Object> result = new ArrayList<>(newLen);

            for (int i = 0; i < actualStart; i++) {
               result.add(specGet(target, String.valueOf(i), cc));
            }

            for (int i = 0; i < itemCount; i++) {
               result.add(args[i + 2]);
            }

            for (int i = actualStart + actualSkipCount; i < len; i++) {
               result.add(specGet(target, String.valueOf(i), cc));
            }

            return new JsArray(result);
         }
      }
   }

   private Object shift(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else if (len == 0) {
            setLength(target, 0, cc);
            return Terms.UNDEFINED;
         } else {
            Object first = specGet(target, "0", cc);

            for (int k = 1; k < len; k++) {
               String fromKey = String.valueOf(k);
               String toKey = String.valueOf(k - 1);
               if (hasPropertyChain(target, fromKey)) {
                  specSet(target, toKey, specGet(target, fromKey, cc), cc);
               } else {
                  specDelete(target, toKey);
               }
            }

            specDelete(target, String.valueOf(len - 1));
            setLength(target, len - 1, cc);
            return first;
         }
      }
   }

   private Object unshift(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return 0;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            int argCount = args.length;
            if (argCount > 0) {
               if (len == Integer.MAX_VALUE) {
                  checkLengthLimit(target, cc, argCount);
               }

               for (int k = len - 1; k >= 0; k--) {
                  String fromKey = String.valueOf(k);
                  String toKey = String.valueOf(k + argCount);
                  if (hasPropertyChain(target, fromKey)) {
                     specSet(target, toKey, specGet(target, fromKey, cc), cc);
                  } else {
                     specDelete(target, toKey);
                  }
               }

               for (int j = 0; j < argCount; j++) {
                  specSet(target, String.valueOf(j), args[j], cc);
               }
            }

            int newLen = len + argCount;
            setLength(target, newLen, cc);
            return newLen;
         }
      }
   }

   private Object lastIndexOf(Context context, Object[] args) {
      if (args.length == 0) {
         return -1;
      } else {
         Object searchElement = args[0];
         Object thisObj = context.getThisObject();
         CoreContext cc = context instanceof CoreContext cx ? cx : null;
         ObjectLike target = thisObj instanceof ObjectLike o ? o : Terms.toObjectLike(thisObj);
         int len = target == null ? 0 : lengthOf(target, cc);
         if (len == 0) {
            return -1;
         } else {
            int fromIndex = len - 1;
            if (args.length > 1 && args[1] != null) {
               Number n = Terms.objectToNumber(args[1]);
               if (!Double.isNaN(n.doubleValue())) {
                  fromIndex = n.intValue();
                  if (fromIndex < 0) {
                     fromIndex += len;
                  } else if (fromIndex >= len) {
                     fromIndex = len - 1;
                  }
               }
            }

            if (fromIndex < 0) {
               return -1;
            } else {
               int[] result = new int[]{-1};
               int from = fromIndex;
               specIterate(context, false, true, (k, v) -> {
                  if (k > from) {
                     return true;
                  } else if (Terms.strictEq(v, searchElement)) {
                     result[0] = k;
                     return false;
                  } else {
                     return true;
                  }
               });
               return result[0];
            }
         }
      }
   }

   private Object pop(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike target = toReceiver(context.getThisObject());
      if (target == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(target, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else if (len == 0) {
            setLength(target, 0, cc);
            return Terms.UNDEFINED;
         } else {
            int newLen = len - 1;
            String index = String.valueOf(newLen);
            Object element = specGet(target, index, cc);
            specDelete(target, index);
            setLength(target, newLen, cc);
            return element;
         }
      }
   }

   private Object at(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      int size = thisArray.size();
      if (size != 0 && args.length != 0 && args[0] != null) {
         int index = Terms.objectToNumber(args[0]).intValue();
         if (index < 0) {
            index += size;
         }

         return index >= 0 && index < size ? thisArray.get(index) : Terms.UNDEFINED;
      } else {
         return Terms.UNDEFINED;
      }
   }

   private Object copyWithin(Context context, Object[] args) {
      CoreContext cc = context instanceof CoreContext cx ? cx : null;
      ObjectLike receiver = toReceiver(context.getThisObject());
      if (receiver == null) {
         return Terms.UNDEFINED;
      } else {
         int len = lengthOf(receiver, cc);
         if (cc != null && cc.isError()) {
            return Terms.UNDEFINED;
         } else {
            int to = 0;
            if (args.length > 0 && args[0] != null) {
               int rel = Terms.objectToNumber(args[0]).intValue();
               to = rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
            }

            int from = 0;
            if (args.length > 1 && args[1] != null) {
               int rel = Terms.objectToNumber(args[1]).intValue();
               from = rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
            }

            int finalEnd = len;
            if (args.length > 2 && args[2] != null) {
               int rel = Terms.objectToNumber(args[2]).intValue();
               finalEnd = rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
            }

            int count = Math.min(finalEnd - from, len - to);
            int direction;
            if (from < to && to < from + count) {
               direction = -1;
               from += count - 1;
               to += count - 1;
            } else {
               direction = 1;
            }

            while (count > 0) {
               String fromKey = String.valueOf(from);
               String toKey = String.valueOf(to);
               if (hasPropertyChain(receiver, fromKey)) {
                  specSet(receiver, toKey, specGet(receiver, fromKey, cc), cc);
               } else {
                  specDelete(receiver, toKey);
               }

               from += direction;
               to += direction;
               count--;
            }

            return receiver;
         }
      }
   }

   private Object keys(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      List<Object> result = new ArrayList<>();
      int size = thisArray.size();

      for (int i = 0; i < size; i++) {
         result.add(i);
      }

      return result;
   }

   private Object values(Context context, Object[] args) {
      return IterUtils.toIteratorObject(IterUtils.listIterator(rawList(context)));
   }

   private Object entries(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      List<Object> result = new ArrayList<>();
      int size = thisArray.size();

      for (int i = 0; i < size; i++) {
         List<Object> entry = new ArrayList<>();
         entry.add(i);
         entry.add(thisArray.get(i));
         result.add(entry);
      }

      return result;
   }

   private Object findLast(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.findLast"), args, 1);
      Object thisObj = context.getThisObject();
      Object[] result = new Object[]{Terms.UNDEFINED};
      specIterate(context, false, false, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            result[0] = v;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object findLastIndex(Context context, Object[] args) {
      JsCallable callable = bindThis(requireCallable(args, "Array.prototype.findLastIndex"), args, 1);
      Object thisObj = context.getThisObject();
      int[] result = new int[]{-1};
      specIterate(context, false, false, (k, v) -> {
         Object r = callable.call(context, new Object[]{v, k, thisObj});
         if (Terms.isTruthy(r)) {
            result[0] = k;
            return false;
         } else {
            return true;
         }
      });
      return result[0];
   }

   private Object withMethod(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      int size = thisArray.size();
      if (size != 0 && args.length >= 2) {
         int index = Terms.objectToNumber(args[0]).intValue();
         if (index < 0) {
            index += size;
         }

         if (index >= 0 && index < size) {
            Object value = args[1];
            List<Object> result = new ArrayList<>(thisArray);
            result.set(index, value);
            return new JsArray(result);
         } else {
            return new JsArray(new ArrayList<>(thisArray));
         }
      } else {
         return new JsArray(new ArrayList<>(thisArray));
      }
   }

   private Object group(Context context, Object[] args) {
      List<Object> thisArray = rawList(context);
      JsCallable callable = requireCallable(args, "Array.prototype.group");
      Map<String, List<Object>> groups = new HashMap<>();

      for (KeyValue kv : jsEntries(context)) {
         Object key = callable.call(context, new Object[]{kv.value(), kv.index(), thisArray});
         String keyStr = key == null ? "null" : key.toString();
         if (!groups.containsKey(keyStr)) {
            groups.put(keyStr, new ArrayList<>());
         }

         groups.get(keyStr).add(kv.value());
      }

      JsObject result = new JsObject();

      for (Entry<String, List<Object>> entry : groups.entrySet()) {
         result.putMember(entry.getKey(), entry.getValue());
      }

      return result;
   }

   static {
      INSTANCE.install("toString", DEFAULT_TO_STRING);
   }

   @FunctionalInterface
   private interface IndexVisitor {
      boolean visit(int var1, Object var2);
   }
}
