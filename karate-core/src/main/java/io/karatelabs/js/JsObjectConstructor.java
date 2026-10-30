package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

class JsObjectConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsObjectConstructor() {
      this.name = "Object";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("keys", new JsBuiltinMethod("keys", 1, this::keys), (byte)13);
      this.defineOwn("values", new JsBuiltinMethod("values", 1, this::values), (byte)13);
      this.defineOwn("entries", new JsBuiltinMethod("entries", 1, this::entries), (byte)13);
      this.defineOwn("assign", new JsBuiltinMethod("assign", 2, this::assign), (byte)13);
      this.defineOwn("fromEntries", new JsBuiltinMethod("fromEntries", 1, this::fromEntries), (byte)13);
      this.defineOwn("is", new JsBuiltinMethod("is", 2, (ctx, args) -> this.is(args)), (byte)13);
      this.defineOwn("create", new JsBuiltinMethod("create", 2, this::create), (byte)13);
      this.defineOwn("getPrototypeOf", new JsBuiltinMethod("getPrototypeOf", 1, (ctx, args) -> this.getPrototypeOf(args)), (byte)13);
      this.defineOwn("setPrototypeOf", new JsBuiltinMethod("setPrototypeOf", 2, (ctx, args) -> this.setPrototypeOf(args)), (byte)13);
      this.defineOwn("hasOwn", new JsBuiltinMethod("hasOwn", 2, (ctx, args) -> this.hasOwn(args)), (byte)13);
      this.defineOwn("getOwnPropertyNames", new JsBuiltinMethod("getOwnPropertyNames", 1, (ctx, args) -> this.getOwnPropertyNames(args)), (byte)13);
      this.defineOwn("getOwnPropertySymbols", new JsBuiltinMethod("getOwnPropertySymbols", 1, (ctx, args) -> this.getOwnPropertySymbols(args)), (byte)13);
      this.defineOwn("getOwnPropertyDescriptor", new JsBuiltinMethod("getOwnPropertyDescriptor", 2, (ctx, args) -> this.getOwnPropertyDescriptor(args)), (byte)13);
      this.defineOwn("getOwnPropertyDescriptors", new JsBuiltinMethod("getOwnPropertyDescriptors", 1, (ctx, args) -> this.getOwnPropertyDescriptors(args)), (byte)13);
      this.defineOwn("defineProperty", new JsBuiltinMethod("defineProperty", 3, this::defineProperty), (byte)13);
      this.defineOwn("defineProperties", new JsBuiltinMethod("defineProperties", 2, this::defineProperties), (byte)13);
      this.defineOwn("isExtensible", new JsBuiltinMethod("isExtensible", 1, (ctx, args) -> this.isExtensible(args)), (byte)13);
      this.defineOwn("preventExtensions", new JsBuiltinMethod("preventExtensions", 1, (ctx, args) -> this.preventExtensions(args)), (byte)13);
      this.defineOwn("isSealed", new JsBuiltinMethod("isSealed", 1, (ctx, args) -> this.isSealed(args)), (byte)13);
      this.defineOwn("seal", new JsBuiltinMethod("seal", 1, (ctx, args) -> this.seal(args)), (byte)13);
      this.defineOwn("isFrozen", new JsBuiltinMethod("isFrozen", 1, (ctx, args) -> this.isFrozen(args)), (byte)13);
      this.defineOwn("freeze", new JsBuiltinMethod("freeze", 1, (ctx, args) -> this.freeze(args)), (byte)13);
      this.defineOwn("groupBy", new JsBuiltinMethod("groupBy", 2, this::groupBy), (byte)13);
      this.defineOwn("prototype", JsObjectPrototype.INSTANCE, (byte)8);
   }

   private Object groupBy(Context context, Object[] args) {
      Object items = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Object callback = args.length > 1 ? args[1] : Terms.UNDEFINED;
      return GroupByImpl.toNullProtoObject(GroupByImpl.run(items, callback, true, context));
   }

   @Override
   public Object call(Context context, Object... args) {
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         Object v = args[0];
         if (v instanceof ObjectLike) {
            return v;
         } else if (v instanceof String s) {
            return new JsString(s);
         } else if (v instanceof Boolean b) {
            return new JsBoolean(b);
         } else {
            return v instanceof Number n ? new JsNumber(n) : new JsObject();
         }
      } else {
         return new JsObject();
      }
   }

   @Override
   public boolean isConstructable() {
      return true;
   }

   private Object isExtensible(Object[] args) {
      if (args.length == 0) {
         return false;
      } else {
         return args[0] instanceof ObjectLike ol ? ol.isExtensible() : false;
      }
   }

   private Object preventExtensions(Object[] args) {
      if (args.length == 0) {
         return Terms.UNDEFINED;
      } else {
         Object v = args[0];
         if (v instanceof ObjectLike ol) {
            ol.setExtensible(false);
         }

         return v;
      }
   }

   private Object isSealed(Object[] args) {
      if (args.length == 0) {
         return true;
      } else {
         return args[0] instanceof ObjectLike ol ? ol.isSealed() : true;
      }
   }

   private Object seal(Object[] args) {
      if (args.length == 0) {
         return Terms.UNDEFINED;
      } else {
         Object v = args[0];
         if (v instanceof ObjectLike ol) {
            ol.setSealed(true);
         }

         return v;
      }
   }

   private Object isFrozen(Object[] args) {
      if (args.length == 0) {
         return true;
      } else {
         return args[0] instanceof ObjectLike ol ? ol.isFrozen() : true;
      }
   }

   private Object freeze(Object[] args) {
      if (args.length == 0) {
         return Terms.UNDEFINED;
      } else {
         Object v = args[0];
         if (v instanceof ObjectLike ol) {
            ol.setFrozen(true);
         }

         return v;
      }
   }

   private Object keys(Context context, Object[] args) {
      requireObjectCoercible(args, "Object.keys");
      CoreContext cc = context instanceof CoreContext c ? c : null;
      List<Object> result = new ArrayList<>();

      for (KeyValue kv : Terms.toIterable(args[0], cc)) {
         result.add(kv.key());
      }

      return new JsArray(result);
   }

   private Object values(Context context, Object[] args) {
      requireObjectCoercible(args, "Object.values");
      CoreContext cc = context instanceof CoreContext c ? c : null;
      List<Object> result = new ArrayList<>();

      for (KeyValue kv : Terms.toIterable(args[0], cc)) {
         result.add(kv.value());
      }

      return new JsArray(result);
   }

   private Object entries(Context context, Object[] args) {
      requireObjectCoercible(args, "Object.entries");
      CoreContext cc = context instanceof CoreContext c ? c : null;
      List<Object> result = new ArrayList<>();

      for (KeyValue kv : Terms.toIterable(args[0], cc)) {
         List<Object> entry = new ArrayList<>();
         entry.add(kv.key());
         entry.add(kv.value());
         result.add(new JsArray(entry));
      }

      return new JsArray(result);
   }

   private static void requireObjectCoercible(Object[] args, String op) {
      if (args.length < 1 || args[0] == null || args[0] == Terms.UNDEFINED) {
         throw JsErrorException.typeError(op + " called on null or undefined");
      }
   }

   private Object assign(Context context, Object[] args) {
      if (args.length == 0) {
         return new LinkedHashMap();
      } else if (args[0] != null && args[0] != Terms.UNDEFINED) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         Object target = !(args[0] instanceof ObjectLike) && !(args[0] instanceof Map) ? new LinkedHashMap() : args[0];

         for (int i = 1; i < args.length && (cc == null || !cc.isError()); i++) {
            if (args[i] instanceof JsObject from && target instanceof JsObject to) {
               for (JsSymbol sym : from.ownEnumerableSymbols()) {
                  if (cc != null && cc.isError()) {
                     break;
                  }

                  Object v = from.getSymbol(sym, cc);
                  if (cc != null && cc.isError()) {
                     break;
                  }

                  to.setSymbol(sym, v, cc, true);
               }
            }

            for (KeyValue kv : Terms.toIterable(args[i], cc)) {
               if (cc == null || !cc.isError()) {
                  if (target instanceof ObjectLike ol) {
                     ol.putMember(kv.key(), kv.value(), cc, true);
                  } else if (target instanceof Map<?, ?> map) {
                     ((Map<String, Object>)map).put((String) kv.key(), kv.value());
                  }
                  continue;
               }
            }
         }

         return target;
      } else {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      }
   }

   private Object fromEntries(Context context, Object[] args) {
      Object source = args.length > 0 ? args[0] : Terms.UNDEFINED;
      if (source != null && source != Terms.UNDEFINED) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         JsObject result = new JsObject();
         JsIterator iter = IterUtils.getIterator(source, context);

         while (iter.hasNext()) {
            Object entry = iter.next();
            Object key;
            Object value;
            if (entry instanceof List<?> pair) {
               key = pair.isEmpty() ? Terms.UNDEFINED : pair.get(0);
               value = pair.size() > 1 ? pair.get(1) : Terms.UNDEFINED;
            } else {
               if (!(entry instanceof ObjectLike obj)) {
                  throw JsErrorException.typeError("Iterator value " + entry + " is not an entry object");
               }

               key = obj.getMember("0", obj, cc);
               value = obj.getMember("1", obj, cc);
            }

            JsSymbol sym = JsSymbol.keyedBy(key);
            if (sym != null) {
               result.defineOwnSymbol(sym, value, (byte)7);
            } else {
               result.putMember(Terms.toPropertyKey(key, cc), value);
            }
         }

         return result;
      } else {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      }
   }

   private Object is(Object[] args) {
      return args.length < 2 ? false : Terms.sameValue(args[0], args[1]);
   }

   private Object create(Context context, Object[] args) {
      JsObject newObj = new JsObject();
      if (args.length > 0 && args[0] instanceof ObjectLike proto) {
         newObj.setPrototype(proto);
      }

      if (args.length > 1 && args[1] != null && args[1] != Terms.UNDEFINED) {
         this.defineProperties(context, new Object[]{newObj, args[1]});
      }

      return newObj;
   }

   private Object getPrototypeOf(Object[] args) {
      return args.length > 0 && args[0] instanceof ObjectLike ol ? ol.getPrototype() : null;
   }

   private Object setPrototypeOf(Object[] args) {
      if (args.length >= 2) {
         ObjectLike proto = null;
         if (args[1] instanceof ObjectLike p) {
            proto = p;
         }

         if (args[0] instanceof JsObject obj) {
            obj.setPrototype(proto);
            return args[0];
         }

         if (args[0] instanceof JsArray arr) {
            arr.setPrototype(proto);
            return args[0];
         }
      }

      return args.length > 0 ? args[0] : null;
   }

   private Object hasOwn(Object[] args) {
      if (args.length < 1 || args[0] == null || args[0] == Terms.UNDEFINED) {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      } else if (args.length < 2) {
         return false;
      } else {
         JsSymbol sym = JsSymbol.keyedBy(args[1]);
         if (sym == null) {
            String prop = Terms.toPropertyKey(args[1]);
            return isOwnKey(args[0], prop);
         } else {
            return args[0] instanceof JsObject jo && jo.hasSymbol(sym);
         }
      }
   }

   private Object getOwnPropertyNames(Object[] args) {
      if (args.length >= 1 && args[0] != null && args[0] != Terms.UNDEFINED) {
         return new JsArray(new ArrayList<>(ownKeys(args[0])));
      } else {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      }
   }

   private Object getOwnPropertySymbols(Object[] args) {
      if (args.length >= 1 && args[0] != null && args[0] != Terms.UNDEFINED) {
         return new JsArray(args[0] instanceof JsObject jo ? new ArrayList<>(jo.ownSymbols()) : new ArrayList<>());
      } else {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      }
   }

   private Object getOwnPropertyDescriptor(Object[] args) {
      if (args.length < 1 || args[0] == null || args[0] == Terms.UNDEFINED) {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      } else if (args.length < 2) {
         return Terms.UNDEFINED;
      } else {
         JsSymbol sym = JsSymbol.keyedBy(args[1]);
         if (sym != null) {
            return args[0] instanceof JsObject jo && jo.hasSymbol(sym) ? buildSymbolDescriptor(jo, sym) : Terms.UNDEFINED;
         } else {
            String prop = Terms.toPropertyKey(args[1]);
            return !isOwnKey(args[0], prop) ? Terms.UNDEFINED : buildDescriptor(args[0], prop, ownAttrs(args[0], prop));
         }
      }
   }

   private Object getOwnPropertyDescriptors(Object[] args) {
      if (args.length >= 1 && args[0] != null && args[0] != Terms.UNDEFINED) {
         Map<String, Object> result = new LinkedHashMap<>();
         LinkedHashSet<String> keys = new LinkedHashSet<>(ownKeys(args[0]));
         if (args[0] instanceof JsObject jo) {
            for (String name : jo.ownIntrinsicNames()) {
               if (!keys.contains(name) && jo.hasOwnIntrinsic(name)) {
                  keys.add(name);
               }
            }
         }

         for (String key : keys) {
            result.put(key, buildDescriptor(args[0], key, ownAttrs(args[0], key)));
         }

         return new JsObject(result);
      } else {
         throw JsErrorException.typeError("Cannot convert undefined or null to object");
      }
   }

   private static boolean isOwnKey(Object obj, String key) {
      return obj instanceof ObjectLike ol ? ol.isOwnProperty(key) : ownKeys(obj).contains(key);
   }

   private static JsObject buildDescriptor(Object obj, String key, byte attrs) {
      Map<String, Object> desc = new LinkedHashMap<>();
      AccessorSlot acc = ownAccessorSlot(obj, key);
      if (acc != null) {
         desc.put("get", acc.getter == null ? Terms.UNDEFINED : acc.getter);
         desc.put("set", acc.setter == null ? Terms.UNDEFINED : acc.setter);
      } else {
         desc.put("value", ownGet(obj, key));
         desc.put("writable", (attrs & 1) != 0);
      }

      desc.put("enumerable", (attrs & 2) != 0);
      desc.put("configurable", (attrs & 4) != 0);
      return new JsObject(desc);
   }

   private static void validateNonConfigurableRedefine(
      Object label,
      boolean existingIsAccessor,
      AccessorSlot existingAcc,
      Object existingValue,
      byte oldAttrs,
      byte newAttrs,
      boolean isAccessor,
      boolean isData,
      boolean hasConfigurable,
      boolean hasEnumerable,
      boolean hasGet,
      boolean hasSet,
      boolean hasValue,
      JsCallable newGetter,
      JsCallable newSetter,
      Supplier<Object> newValue
   ) {
      if (hasConfigurable && (newAttrs & 4) != 0) {
         throw JsErrorException.typeError("Cannot redefine property: " + label);
      } else if (hasEnumerable && ((oldAttrs ^ newAttrs) & 2) != 0) {
         throw JsErrorException.typeError("Cannot redefine property: " + label);
      } else if (existingIsAccessor == isAccessor || !isAccessor && !isData) {
         if (existingIsAccessor && isAccessor) {
            JsCallable mergedGet = hasGet ? newGetter : existingAcc.getter;
            JsCallable mergedSet = hasSet ? newSetter : existingAcc.setter;
            if (mergedGet != existingAcc.getter || mergedSet != existingAcc.setter) {
               throw JsErrorException.typeError("Cannot redefine property: " + label);
            }
         } else if (!existingIsAccessor && isData) {
            boolean oldWritable = (oldAttrs & 1) != 0;
            boolean newWritable = (newAttrs & 1) != 0;
            if (!oldWritable && newWritable) {
               throw JsErrorException.typeError("Cannot redefine property: " + label);
            }

            if (!oldWritable && hasValue && !Terms.sameValue(existingValue, newValue.get())) {
               throw JsErrorException.typeError("Cannot redefine property: " + label);
            }
         }
      } else {
         throw JsErrorException.typeError("Cannot redefine property: " + label);
      }
   }

   private static JsObject buildSymbolDescriptor(JsObject obj, JsSymbol sym) {
      PropertySlot slot = obj.ownSymbolSlot(sym);
      Map<String, Object> desc = new LinkedHashMap<>();
      if (slot instanceof AccessorSlot acc) {
         desc.put("get", acc.getter == null ? Terms.UNDEFINED : acc.getter);
         desc.put("set", acc.setter == null ? Terms.UNDEFINED : acc.setter);
      } else {
         desc.put("value", slot.read(obj, null));
         desc.put("writable", slot.isWritable());
      }

      desc.put("enumerable", slot.isEnumerable());
      desc.put("configurable", slot.isConfigurable());
      return new JsObject(desc);
   }

   private static AccessorSlot ownAccessorSlot(Object obj, String key) {
      return PropertyAccess.ownSlot(obj, key) instanceof AccessorSlot acc ? acc : null;
   }

   Object defineProperty(Context context, Object[] args) {
      if (args.length >= 1 && (args[0] instanceof ObjectLike || args[0] instanceof Map)) {
         if (args.length < 2) {
            throw JsErrorException.typeError("property key is null");
         } else if (args.length >= 3 && args[2] != null && args[2] != Terms.UNDEFINED) {
            CoreContext keyCtx = context instanceof CoreContext c ? c : null;
            String prop = Terms.toPropertyKey(args[1], keyCtx);
            Object desc = args[2];
            ObjectLike descObj;
            Map<String, Object> descMap;
            if (desc instanceof ObjectLike ol) {
               descObj = ol;
               descMap = ol.toMap();
            } else {
               if (!(desc instanceof Map)) {
                  throw JsErrorException.typeError("Property descriptor must be an object");
               }

               descObj = null;
               descMap = (Map<String, Object>)desc;
            }

            CoreContext cc;
            CoreContext var48 = cc = context instanceof CoreContext cx ? cx : null;
            boolean hasGet = descHas(descObj, descMap, "get");
            boolean hasSet = descHas(descObj, descMap, "set");
            boolean hasValue = descHas(descObj, descMap, "value");
            boolean hasWritable = descHas(descObj, descMap, "writable");
            boolean hasEnumerable = descHas(descObj, descMap, "enumerable");
            boolean hasConfigurable = descHas(descObj, descMap, "configurable");
            boolean isAccessor = hasGet || hasSet;
            boolean isData = hasValue || hasWritable;
            boolean isGeneric = !isAccessor && !isData;
            if (isAccessor && isData) {
               throw JsErrorException.typeError("Invalid property descriptor. Cannot both specify accessors and a value or writable attribute");
            } else {
               Object target = args[0];
               JsSymbol symKey = JsSymbol.keyedBy(args[1]);
               boolean keyExists = symKey == null && ownKeys(target).contains(prop);
               if (symKey == null && !keyExists && target instanceof ObjectLike ol && !ol.isExtensible()) {
                  throw JsErrorException.typeError("Cannot define property " + prop + ", object is not extensible");
               } else {
                  JsCallable newGetter = null;
                  JsCallable newSetter = null;
                  if (isAccessor) {
                     if (hasGet) {
                        Object g = descRead(descObj, descMap, "get", cc);
                        if (g != Terms.UNDEFINED) {
                           if (!(g instanceof JsCallable cxx)) {
                              throw JsErrorException.typeError("Getter must be a function");
                           }

                           newGetter = cxx;
                        }
                     }

                     if (hasSet) {
                        Object s = descRead(descObj, descMap, "set", cc);
                        if (s != Terms.UNDEFINED) {
                           if (!(s instanceof JsCallable cxx)) {
                              throw JsErrorException.typeError("Setter must be a function");
                           }

                           newSetter = cxx;
                        }
                     }
                  }

                  if (symKey == null) {
                     boolean isArrayLength = target instanceof JsArray && "length".equals(prop) && hasValue;
                     Long coercedLength = null;
                     if (isArrayLength) {
                        coercedLength = JsArray.coerceToUint32(descRead(descObj, descMap, "value", cc), cc);
                     }

                     Object existing = keyExists ? ownGet(target, prop) : null;
                     AccessorSlot existingAcc = keyExists ? ownAccessorSlot(target, prop) : null;
                     boolean existingIsAccessor = existingAcc != null;
                     byte oldAttrs = ownAttrs(target, prop);
                     byte newAttrs = keyExists ? oldAttrs : 0;
                     if (hasWritable) {
                        newAttrs = setBit(newAttrs, (byte)1, Terms.isTruthy(descRead(descObj, descMap, "writable", cc)));
                     }

                     if (hasEnumerable) {
                        newAttrs = setBit(newAttrs, (byte)2, Terms.isTruthy(descRead(descObj, descMap, "enumerable", cc)));
                     }

                     if (hasConfigurable) {
                        newAttrs = setBit(newAttrs, (byte)4, Terms.isTruthy(descRead(descObj, descMap, "configurable", cc)));
                     }

                     if (isAccessor) {
                        newAttrs &= -2;
                     }

                     final Long coercedLengthF2 = coercedLength;

                     if (keyExists && (oldAttrs & 4) == 0) {
                        validateNonConfigurableRedefine(
                           prop,
                           existingIsAccessor,
                           existingAcc,
                           existing,
                           oldAttrs,
                           newAttrs,
                           isAccessor,
                           isData,
                           hasConfigurable,
                           hasEnumerable,
                           hasGet,
                           hasSet,
                           hasValue,
                           newGetter,
                           newSetter,
                           () -> isArrayLength ? coercedLengthF2 : descRead(descObj, descMap, "value", cc)
                        );
                     }

                     if (isAccessor) {
                        JsCallable getter = newGetter;
                        JsCallable setter = newSetter;
                        if (existingIsAccessor) {
                           if (!hasGet) {
                              getter = existingAcc.getter;
                           }

                           if (!hasSet) {
                              setter = existingAcc.setter;
                           }
                        }

                        applyDefineAccessor(target, prop, getter, setter, newAttrs);
                     } else if (isArrayLength) {
                        boolean ok = ((JsArray)target).defineLength(coercedLength.intValue(), newAttrs);
                        if (!ok) {
                           throw JsErrorException.typeError("Cannot redefine property: length");
                        }
                     } else if (isGeneric && existingIsAccessor) {
                        applyAttrsOnly(target, prop, newAttrs);
                     } else if (hasValue) {
                        applyDefine(target, prop, descRead(descObj, descMap, "value", cc), newAttrs);
                     } else if (!keyExists) {
                        applyDefine(target, prop, Terms.UNDEFINED, newAttrs);
                     } else if (existingIsAccessor) {
                        applyDefine(target, prop, Terms.UNDEFINED, newAttrs);
                     } else {
                        applyDefine(target, prop, existing, newAttrs);
                     }

                     return target;
                  } else if (!(target instanceof JsObject jo)) {
                     return target;
                  } else {
                     PropertySlot prior = jo.ownSymbolSlot(symKey);
                     if (prior == null && !jo.isExtensible()) {
                        throw JsErrorException.typeError("Cannot define property " + symKey + ", object is not extensible");
                     } else {
                        byte symAttrs = prior == null ? 0 : prior.attrs;
                        if (hasWritable) {
                           symAttrs = setBit(symAttrs, (byte)1, Terms.isTruthy(descRead(descObj, descMap, "writable", cc)));
                        }

                        if (hasEnumerable) {
                           symAttrs = setBit(symAttrs, (byte)2, Terms.isTruthy(descRead(descObj, descMap, "enumerable", cc)));
                        }

                        if (hasConfigurable) {
                           symAttrs = setBit(symAttrs, (byte)4, Terms.isTruthy(descRead(descObj, descMap, "configurable", cc)));
                        }

                        if (isAccessor) {
                           symAttrs &= -2;
                        }

                        AccessorSlot priorAcc = prior instanceof AccessorSlot pa ? pa : null;
                        if (prior != null && !prior.isConfigurable()) {
                           validateNonConfigurableRedefine(
                              symKey,
                              priorAcc != null,
                              priorAcc,
                              priorAcc != null ? null : prior.read(jo, cc),
                              prior.attrs,
                              symAttrs,
                              isAccessor,
                              isData,
                              hasConfigurable,
                              hasEnumerable,
                              hasGet,
                              hasSet,
                              hasValue,
                              newGetter,
                              newSetter,
                              () -> descRead(descObj, descMap, "value", cc)
                           );
                        }

                        if (isAccessor) {
                           JsCallable g = !hasGet && priorAcc != null ? priorAcc.getter : newGetter;
                           JsCallable st = !hasSet && priorAcc != null ? priorAcc.setter : newSetter;
                           jo.defineOwnSymbolAccessor(symKey, g, st, symAttrs);
                        } else if (isGeneric && priorAcc != null) {
                           jo.defineOwnSymbolAccessor(symKey, priorAcc.getter, priorAcc.setter, symAttrs);
                        } else {
                           jo.defineOwnSymbol(
                              symKey,
                              hasValue ? descRead(descObj, descMap, "value", cc) : (prior != null && priorAcc == null ? prior.read(jo, cc) : Terms.UNDEFINED),
                              symAttrs
                           );
                        }

                        return target;
                     }
                  }
               }
            }
         } else {
            throw JsErrorException.typeError("Property descriptor must be an object");
         }
      } else {
         throw JsErrorException.typeError("Object.defineProperty called on non-object");
      }
   }

   private static void applyAttrsOnly(Object target, String key, byte attrs) {
      PropertySlot s = PropertyAccess.ownSlot(target, key);
      if (s != null) {
         s.attrs = attrs;
      }
   }

   private static byte setBit(byte attrs, byte mask, boolean on) {
      return on ? (byte)(attrs | mask) : (byte)(attrs & ~mask);
   }

   private static Object descRead(ObjectLike descObj, Map<String, Object> descMap, String name, CoreContext ctx) {
      return descObj != null ? descObj.getMember(name, descObj, ctx) : descMap.get(name);
   }

   private static boolean descHas(ObjectLike descObj, Map<String, Object> descMap, String name) {
      if (descObj != null) {
         for (ObjectLike o = descObj; o != null; o = o.getPrototype()) {
            if (o.isOwnProperty(name)) {
               return true;
            }
         }

         return false;
      } else {
         return descMap.containsKey(name);
      }
   }

   private static void applyDefine(Object target, String key, Object value, byte attrs) {
      if (target instanceof JsObject jo) {
         jo.defineOwn(key, value, attrs);
      } else if (target instanceof JsArray ja) {
         ja.defineOwn(key, value, attrs);
      } else if (target instanceof Prototype p) {
         p.defineOwn(key, value, attrs);
      } else {
         ownPut(target, key, value);
      }
   }

   private static void applyDefineAccessor(Object target, String key, JsCallable getter, JsCallable setter, byte attrs) {
      if (target instanceof JsObject jo) {
         jo.defineOwnAccessor(key, getter, setter, attrs);
      } else if (target instanceof JsArray ja) {
         ja.defineOwnAccessor(key, getter, setter, attrs);
      } else if (target instanceof Prototype p) {
         p.defineOwnAccessor(key, getter, setter, attrs);
      }
   }

   private static byte ownAttrs(Object obj, String key) {
      if (obj instanceof JsObject jo) {
         return jo.getOwnAttrs(key);
      } else if (obj instanceof JsArray ja) {
         return ja.getOwnAttrs(key);
      } else {
         return obj instanceof Prototype p ? p.getOwnAttrs(key) : 7;
      }
   }

   private Object defineProperties(Context context, Object[] args) {
      if (args.length >= 1 && (args[0] instanceof ObjectLike || args[0] instanceof Map)) {
         Object source = args.length < 2 ? null : args[1];
         if (source != null && source != Terms.UNDEFINED) {
            if (source instanceof String s && !s.isEmpty()) {
               throw JsErrorException.typeError("Property descriptor must be an object");
            } else {
               CoreContext cc = context instanceof CoreContext c ? c : null;

               for (String key : ownKeys(source)) {
                  if (isEnumerableOwn(source, key)) {
                     Object value = source instanceof ObjectLike ol ? ol.getMember(key, ol, cc) : ((Map)source).get(key);
                     this.defineProperty(context, new Object[]{args[0], key, value});
                  }
               }

               return args[0];
            }
         } else {
            throw JsErrorException.typeError("Cannot convert undefined or null to object");
         }
      } else {
         throw JsErrorException.typeError("Object.defineProperties called on non-object");
      }
   }

   private static boolean isEnumerableOwn(Object obj, String key) {
      byte attrs;
      if (obj instanceof JsObject jo) {
         attrs = jo.getOwnAttrs(key);
      } else if (obj instanceof JsArray ja) {
         attrs = ja.getOwnAttrs(key);
      } else {
         if (!(obj instanceof Prototype p)) {
            return true;
         }

         attrs = p.getOwnAttrs(key);
      }

      return (attrs & 2) != 0;
   }

   private static Set<String> ownKeys(Object obj) {
      if (!(obj instanceof JsArray ja)) {
         if (obj instanceof ObjectLike ol) {
            return JsObject.orderedOwnKeys(ol.toMap().keySet());
         } else {
            return obj instanceof Map<?, ?> m ? JsObject.orderedOwnKeys((Set<String>)m.keySet()) : Collections.emptySet();
         }
      } else {
         LinkedHashSet<String> keys = new LinkedHashSet<>();
         int i = 0;

         for (int n = ja.size(); i < n; i++) {
            if (ja.isOwnProperty(Integer.toString(i))) {
               keys.add(Integer.toString(i));
            }
         }

         for (String k : ja.namedPropsView().keySet()) {
            keys.add(k);
         }

         keys.add("length");
         return keys;
      }
   }

   private static Object ownGet(Object obj, String key) {
      if (obj instanceof ObjectLike ol) {
         Map<String, Object> m = ol instanceof JsArray ja ? ja.namedPropsView() : ol.toMap();
         if (m.containsKey(key)) {
            return m.get(key);
         } else {
            return ol.isOwnProperty(key) ? ol.getMember(key) : Terms.UNDEFINED;
         }
      } else {
         return obj instanceof Map<?, ?> m ? m.get(key) : Terms.UNDEFINED;
      }
   }

   private static void ownPut(Object obj, String key, Object value) {
      if (obj instanceof ObjectLike ol) {
         ol.putMember(key, value);
      } else if (obj instanceof Map<?, ?> m) {
         ((Map<String, Object>)m).put(key, value);
      }
   }
}
