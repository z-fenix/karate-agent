package io.karatelabs.js;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

abstract class Prototype implements ObjectLike {
   private static volatile boolean anyUserProps = false;
   private Map<String, PropertySlot> orphanUserProps;
   private static volatile boolean orphanNumericPropPolluted = false;
   private final Prototype __proto__;
   private final Map<String, Object> builtins = new LinkedHashMap<>();

   static boolean isNumericPropPolluted() {
      if (orphanNumericPropPolluted) {
         return true;
      } else {
         Engine engine = Engine.current();
         return engine != null && engine.numericPropPolluted;
      }
   }

   private static void markNumericPolluted() {
      Engine engine = Engine.current();
      if (engine != null) {
         engine.numericPropPolluted = true;
      } else {
         orphanNumericPropPolluted = true;
      }
   }

   private Map<String, PropertySlot> userProps() {
      if (!anyUserProps) {
         return null;
      } else {
         Engine engine = Engine.current();
         return engine != null ? engine.protoUserProps(this, false) : this.orphanUserProps;
      }
   }

   private Map<String, PropertySlot> userPropsForWrite() {
      anyUserProps = true;
      Engine engine = Engine.current();
      if (engine != null) {
         return engine.protoUserProps(this, true);
      } else {
         if (this.orphanUserProps == null) {
            this.orphanUserProps = new LinkedHashMap<>();
         }

         return this.orphanUserProps;
      }
   }

   private static boolean isCanonicalNumericKey(String name) {
      int n = name.length();
      if (n != 0 && n <= 10) {
         for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9') {
               return false;
            }
         }

         return n == 1 || name.charAt(0) != '0';
      } else {
         return false;
      }
   }

   Prototype(Prototype __proto__) {
      this.__proto__ = __proto__;
   }

   @Override
   public ObjectLike getPrototype() {
      return this.__proto__;
   }

   @Override
   public void putMember(String name, Object value) {
      Map<String, PropertySlot> userProps = this.userPropsForWrite();
      if (isCanonicalNumericKey(name)) {
         markNumericPolluted();
      }

      PropertySlot existing = userProps.get(name);
      if (existing instanceof DataSlot ds) {
         ds.value = value;
         ds.tombstoned = false;
      } else {
         userProps.put(name, new DataSlot(name, value));
      }
   }

   @Override
   public void removeMember(String name) {
      Map<String, PropertySlot> userProps = this.userProps();
      PropertySlot existing = userProps == null ? null : userProps.get(name);
      if (existing == null || !existing.tombstoned) {
         boolean inUser = existing != null;
         boolean shadowsBuiltin = this.builtins.containsKey(name);
         if (inUser || shadowsBuiltin) {
            if (!shadowsBuiltin || (this.getOwnAttrs(name) & 4) != 0) {
               if (shadowsBuiltin) {
                  if (existing instanceof DataSlot ds) {
                     ds.value = null;
                     ds.tombstoned = true;
                  } else {
                     DataSlot ts = new DataSlot(name);
                     ts.tombstoned = true;
                     this.userPropsForWrite().put(name, ts);
                  }
               } else {
                  userProps.remove(name);
               }
            }
         }
      }
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, PropertySlot> userProps = this.userProps();
      if (userProps != null && !userProps.isEmpty()) {
         Map<String, Object> view = new LinkedHashMap<>(userProps.size());

         for (PropertySlot s : userProps.values()) {
            if (!s.tombstoned) {
               view.put(s.name, s instanceof DataSlot ds ? ds.value : null);
            }
         }

         return view;
      } else {
         return Collections.emptyMap();
      }
   }

   @Override
   public final Object getMember(String name) {
      Map<String, PropertySlot> userProps = this.userProps();
      PropertySlot s = userProps == null ? null : userProps.get(name);
      if (s != null) {
         if (s.tombstoned) {
            return this.walkProto(name);
         } else {
            return s instanceof DataSlot ds ? ds.value : null;
         }
      } else {
         Object builtin = this.resolveBuiltin(name, null);
         return builtin != null ? builtin : this.walkProto(name);
      }
   }

   @Override
   public Object getMember(String name, Object receiver, CoreContext ctx) {
      Map<String, PropertySlot> userProps = this.userProps();
      PropertySlot s = userProps == null ? null : userProps.get(name);
      if (s != null) {
         if (s.tombstoned) {
            return this.__proto__ == null ? null : this.__proto__.getMember(name, receiver, ctx);
         } else {
            return s.read(receiver, ctx);
         }
      } else {
         Object builtin = this.resolveBuiltin(name, ctx);
         if (builtin instanceof AccessorSlot acc) {
            return acc.read(receiver, ctx);
         } else if (builtin != null) {
            return builtin;
         } else {
            return this.__proto__ == null ? null : this.__proto__.getMember(name, receiver, ctx);
         }
      }
   }

   private Object walkProto(String name) {
      return this.__proto__ == null ? null : this.__proto__.getMember(name);
   }

   private Object resolveBuiltin(String name, CoreContext ctx) {
      Object result = this.builtins.get(name);
      if (result instanceof Prototype.LazyRef lr) {
         return lr.resolve();
      } else if (result instanceof Prototype.ConstructorRef cr) {
         Engine engine = ctx != null ? ctx.getEngine() : Engine.current();
         return engine == null ? null : engine.builtinConstructor(cr.globalName);
      } else {
         return result;
      }
   }

   final void defineOwn(String name, Object value, byte attrs) {
      Map<String, PropertySlot> userProps = this.userPropsForWrite();
      if (isCanonicalNumericKey(name)) {
         markNumericPolluted();
      }

      PropertySlot existing = userProps.get(name);
      DataSlot s;
      if (existing instanceof DataSlot ds) {
         s = ds;
      } else {
         s = new DataSlot(name);
         userProps.put(name, s);
      }

      s.value = value;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   final void defineOwnAccessor(String name, JsCallable getter, JsCallable setter, byte attrs) {
      Map<String, PropertySlot> userProps = this.userPropsForWrite();
      if (isCanonicalNumericKey(name)) {
         markNumericPolluted();
      }

      PropertySlot existing = userProps.get(name);
      AccessorSlot s;
      if (existing instanceof AccessorSlot as) {
         s = as;
      } else {
         s = new AccessorSlot(name);
         userProps.put(name, s);
      }

      s.getter = getter;
      s.setter = setter;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   final PropertySlot getOwnSlot(String name) {
      Map<String, PropertySlot> userProps = this.userProps();
      if (userProps != null) {
         PropertySlot s = userProps.get(name);
         if (s != null) {
            return s.tombstoned ? null : s;
         }
      }

      return this.builtins.get(name) instanceof AccessorSlot acc ? acc : null;
   }

   protected final void install(String name, int length, JsCallable delegate) {
      this.builtins.put(name, new Prototype.LazyRef(() -> new JsBuiltinMethod(name, length, delegate)));
   }

   protected final void install(String name, Object value) {
      this.builtins.put(name, value);
   }

   protected final void installAccessor(String name, JsCallable getter, JsCallable setter, byte attrs) {
      AccessorSlot s = new AccessorSlot(name);
      s.getter = getter;
      s.setter = setter;
      s.attrs = attrs;
      this.builtins.put(name, s);
   }

   protected final void installConstructor(String globalName) {
      this.builtins.put("constructor", new Prototype.ConstructorRef(globalName));
   }

   public final byte getOwnAttrs(String name) {
      Map<String, PropertySlot> userProps = this.userProps();
      PropertySlot s = userProps == null ? null : userProps.get(name);
      if (s != null && !s.tombstoned) {
         return s.attrs;
      } else {
         return this.builtins.get(name) instanceof AccessorSlot acc ? acc.attrs : this.defaultAttrs(name);
      }
   }

   protected byte defaultAttrs(String name) {
      return 5;
   }

   @Override
   public boolean isOwnProperty(String name) {
      Map<String, PropertySlot> userProps = this.userProps();
      PropertySlot s = userProps == null ? null : userProps.get(name);
      return s != null ? !s.tombstoned : this.builtins.containsKey(name);
   }

   private record ConstructorRef(String globalName) {
   }

   private static final class LazyRef {
      private final Supplier<Object> supplier;
      private volatile Object resolved;

      LazyRef(Supplier<Object> supplier) {
         this.supplier = supplier;
      }

      Object resolve() {
         Object result = this.resolved;
         if (result == null) {
            synchronized (this) {
               result = this.resolved;
               if (result == null) {
                  result = this.supplier.get();
                  this.resolved = result;
               }
            }
         }

         return result;
      }
   }
}
