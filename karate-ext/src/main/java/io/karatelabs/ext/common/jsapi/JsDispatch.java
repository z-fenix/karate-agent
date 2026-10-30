package io.karatelabs.ext.common.jsapi;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsLazy;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class JsDispatch implements SimpleObject {
   private final Map<String, Object> members = new LinkedHashMap<>();

   protected final void fn(String var1, JavaInvokable var2) {
      this.members.put(var1, var2);
   }

   protected final void fn(String var1, JavaCallable var2) {
      this.members.put(var1, var2);
   }

   protected final void val(String var1, Object var2) {
      this.members.put(var1, var2);
   }

   protected final void lazy(String var1, JsLazy var2) {
      this.members.put(var1, var2);
   }

   protected final boolean has(String var1) {
      return this.members.containsKey(var1);
   }

   @Override
   public Object jsGet(String var1) {
      Object var2 = this.members.get(var1);
      return var2 instanceof JsLazy var3 ? var3.get() : var2;
   }

   @Override
   public Collection<String> jsKeys() {
      return this.members.keySet();
   }

   public Collection<String> jsVerbs() {
      var var1 = new HashSet<>(this.jsKeys());
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : this.members.keySet()) {
         if (!var1.contains(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public Collection<String> jsNamespaces() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Entry var3 : this.members.entrySet()) {
         if (var3.getValue() instanceof JsDispatch) {
            var1.add((String)var3.getKey());
         }
      }

      return var1;
   }

   public String jsKind() {
      return null;
   }
}
