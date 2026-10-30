package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TwinModel(JavaCallable component1, List<io.karatelabs.ext.rules.twin.TwinModel.StateModel> b, List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> c, List<io.karatelabs.ext.rules.twin.TwinModel.InvariantModel> d, String e, Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> f, JavaCallable g, io.karatelabs.ext.rules.twin.CalcFunctionHost h, io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel i, JavaCallable j, io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel k, Map<String, Object> l) {
   public static final String a = "(unnamed)";

   public TwinModel(JavaCallable component1, List<io.karatelabs.ext.rules.twin.TwinModel.StateModel> b, List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> c, List<io.karatelabs.ext.rules.twin.TwinModel.InvariantModel> d, String e, Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> f, JavaCallable g, io.karatelabs.ext.rules.twin.CalcFunctionHost h, io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel i, JavaCallable j, io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel k, Map<String, Object> l) {
      this.component1 = component1;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
      this.k = k;
      this.l = l;
   }

   public static List<Map<String, Object>> a(List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> var0, Map<String, List<Object>> var1) {
      ArrayList var2 = new ArrayList();
      var2.add(new LinkedHashMap());

      for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var4 : var0) {
         List var5 = a(var4, var1 == null ? null : (List)var1.get(var4.a()));
         ArrayList var6 = new ArrayList(var2.size() * var5.size());

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var2)) {
            for (Object var10 : var5) {
               LinkedHashMap var11 = new LinkedHashMap(var8);
               var11.put(var4.a(), var10);
               var6.add(var11);
            }
         }

         var2 = var6;
      }

      return var2;
   }

   private static List<Object> a(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var0, List<Object> var1) {
      if (var1 == null) {
         return var0.f();
      } else {
         String var2 = "levels." + var0.a();
         if (var1.isEmpty()) {
            throw TwinException.a(null, var2, "must name at least one level");
         } else {
            for (Object var4 : var1) {
               String var5;
               try {
                  io.karatelabs.ext.rules.twin.CanonicalJson.b(var4);
                  var5 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var4);
               } catch (SnapshotException var7) {
                  throw TwinException.a(null, var2, var7.getMessage());
               }

               if (!a(var0, var4, var5)) {
                  throw TwinException.a(null, var2, var5 + " is not in the declared domain of '" + var0.a() + "'");
               }
            }

            return Collections.unmodifiableList(new ArrayList<>(var1));
         }
      }
   }

   static boolean a(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var0, Object var1) {
      return a(var0, var1, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
   }

   private static boolean a(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var0, Object var1, String var2) {
      if (var0.c() != null) {
         for (Object var4 : var0.c()) {
            if (io.karatelabs.ext.rules.twin.CanonicalJson.c(var4).equals(var2)) {
               return true;
            }
         }

         return false;
      } else {
         return var1 instanceof Number var3 && var3.doubleValue() == Math.rint(var3.doubleValue())
            ? var3.longValue() >= var0.d() && var3.longValue() <= var0.e()
            : false;
      }
   }

   public JavaCallable a() {
      return this.component1;
   }

   public List<io.karatelabs.ext.rules.twin.TwinModel.StateModel> b() {
      return this.b;
   }

   public List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> c() {
      return this.c;
   }

   public List<io.karatelabs.ext.rules.twin.TwinModel.InvariantModel> d() {
      return this.d;
   }

   public String e() {
      return this.e;
   }

   public Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> f() {
      return this.f;
   }

   public JavaCallable g() {
      return this.g;
   }

   public io.karatelabs.ext.rules.twin.CalcFunctionHost h() {
      return this.h;
   }

   public io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel i() {
      return this.i;
   }

   public JavaCallable j() {
      return this.j;
   }

   public io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel k() {
      return this.k;
   }

   public Map<String, Object> l() {
      return this.l;
   }

   public record ArgumentAxis(String a, String b, List<Object> c, Long d, Long e, List<Object> f) {

      public ArgumentAxis(String a, String b, List<Object> c, Long d, Long e, List<Object> f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   public record OperationBinding(String a, String b, String c, boolean d, io.karatelabs.ext.rules.twin.TwinModel.WireModel e) {

      public OperationBinding(String a, String b, String c, boolean d, io.karatelabs.ext.rules.twin.TwinModel.WireModel e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   public record CommandModel(String a, JavaCallable b, boolean c, JavaCallable d, JavaCallable e, List<Map<String, Object>> f, List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> g, boolean h, boolean i, String j, JavaCallable k, JavaCallable l, JavaCallable m, List<String> n, Map<String, String> o, List<String> p, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding q, Map<String, Object> r) {

      public CommandModel(String a, JavaCallable b, boolean c, JavaCallable d, JavaCallable e, List<Map<String, Object>> f, List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> g, boolean h, boolean i, String j, JavaCallable k, JavaCallable l, JavaCallable m, List<String> n, Map<String, String> o, List<String> p, io.karatelabs.ext.rules.twin.TwinModel.OperationBinding q, Map<String, Object> r) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
         this.l = l;
         this.m = m;
         this.n = n;
         this.o = o;
         this.p = p;
         this.q = q;
         this.r = r;
      }
   }

   public record InvariantModel(String a, JavaCallable b) {

      public InvariantModel(String a, JavaCallable b) {
         this.a = a;
         this.b = b;
      }
   }

   public record ReadBackModel(JavaCallable a, JavaCallable b, List<String> c, List<String> d, String e, List<String> f, Map<String, String> g) {

      public ReadBackModel(JavaCallable a, JavaCallable b, List<String> c, List<String> d, String e, List<String> f, Map<String, String> g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   public record ResourceIdentityModel(String b, String c, JavaCallable d, Object e, String f, boolean g, String h) {

      public ResourceIdentityModel(String b, String c, JavaCallable d, Object e, String f, boolean g, String h) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }

      public String a() {
         return this.b + "/{" + this.c + "}";
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public JavaCallable d() {
         return this.d;
      }

      public Object e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record WireRefusal(int a, String b) {

      public WireRefusal(int a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   public record StateModel(String a, JavaCallable b) {

      public StateModel(String a, JavaCallable b) {
         this.a = a;
         this.b = b;
      }
   }

   public record WireModel(Integer a, Map<String, List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>> b) {

      public WireModel(Integer a, Map<String, List<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>> b) {
         this.a = a;
         this.b = b;
      }
   }
}
