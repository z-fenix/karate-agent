package io.karatelabs.ext.common;

import io.karatelabs.js.Context;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.ObjectLike;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsCallback {
   private static final Logger a = LoggerFactory.getLogger(JsCallback.class);
   private final JavaCallable b;

   private JsCallback(JavaCallable var1) {
      this.b = var1;
   }

   public static JsCallback a(Object var0) {
      return var0 instanceof JavaCallable var1 ? new JsCallback(var1) : null;
   }

   public static Object a(Object var0, String var1) {
      if (var0 instanceof ObjectLike var3) {
         return var3.getMember(var1);
      } else {
         return var0 instanceof Map var2 ? var2.get(var1) : null;
      }
   }

   public static JsCallback b(Object var0, String var1) {
      return a(a(var0, var1));
   }

   public Object a(Object... var1) {
      return this.a((Context)null, var1);
   }

   public Object a(Context var1, Object... var2) {
      try {
         return this.b.call(var1, var2);
      } catch (RuntimeException var4) {
         a.warn("js callback failed: {}", var4.toString());
         return null;
      }
   }

   public String b(Object... var1) {
      Object var2 = this.a(var1);
      return var2 == null ? null : String.valueOf(var2);
   }

   public static List<String> b(Object var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         if (var0 instanceof String var2) {
            for (String var8 : var2.split(",")) {
               a((List<String>)var1, (Object)var8);
            }
         } else if (var0 instanceof Object[] var3) {
            for (Object var14 : var3) {
               a(var1, var14);
            }
         } else if (var0 instanceof Iterable) {
            for (Object var12 : (Iterable)var0) {
               a(var1, var12);
            }
         } else {
            a(var1, var0);
         }

         return var1;
      }
   }

   private static void a(List<String> var0, Object var1) {
      if (var1 != null) {
         String var2 = String.valueOf(var1).trim();
         if (!var2.isEmpty()) {
            var0.add(var2);
         }
      }
   }
}
