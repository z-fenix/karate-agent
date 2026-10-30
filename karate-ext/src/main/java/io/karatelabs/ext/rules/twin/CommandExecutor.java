package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandExecutor {
   public static final String a = "applied";
   public static final String b = "refused";
   public static final String c = "error";
   public static final String d = "held";
   public static final String e = "failed";
   public static final String f = "notEvaluated";

   private CommandExecutor() {
   }

   static io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult a(TwinModel var0, Object var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3) {
      return a(var0, var1, var2, var3, null);
   }

   static io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult a(TwinModel var0, Object var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3, RulebookRuntime var4) {
      Object var5 = a(var0, var1, var3);
      Object var6 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var3 == null ? Map.of() : var3);
      if (var2.b() != null) {
         Object var7;
         try {
            var7 = var2.b().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var5));
         } catch (RuntimeException var20) {
            return a("guard", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var20), a(var20));
         }

         if (!Boolean.TRUE.equals(var7)) {
            return a("guard", "guard " + var2.a() + " is false in this state", null, null, null);
         }
      }

      var0.h().a(var4);

      Object var23;
      label169: {
         io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var10;
         try {
            var23 = var0.g() == null ? var2.d().call(null, var5, var6) : var0.g().call(null, var2.d(), var5, var6);
            break label169;
         } catch (RuntimeException var21) {
            TwinRejectionException var9 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var21, TwinRejectionException.class);
            if (var9 == null) {
               SnapshotException var28 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var21, SnapshotException.class);
               if (var28 == null) {
                  return a("apply", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var21), a(var21));
               }

               return a("snapshot", var28.getMessage(), var28.a, var28.b, var28.c);
            }

            var10 = a("apply", var9.a, null, null, null);
         } finally {
            var0.h().a();
         }

         return var10;
      }

      if (var23 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var23)) {
         String var25 = var0.g() == null ? b(var23) : String.valueOf(var23);
         return a("returnValue", "apply returned a value (" + var25 + "); it must mutate the draft and return undefined", null, null, null);
      } else {
         try {
            io.karatelabs.ext.rules.twin.CanonicalJson.b(var5);
         } catch (SnapshotException var19) {
            return a("snapshot", var19.getMessage(), var19.a, var19.b, var19.c);
         }

         if (var0.k() != null) {
            String var8 = var0.k().c();
            String var26 = a(var1, var8);
            String var29 = a(var5, var8);
            if (!var26.equals(var29)) {
               return a("identity", "apply moved the identity '" + var8 + "' from " + var26 + " to " + var29 + "; the mock allocates it", "IDENTITY_REASSIGNED");
            }
         }

         String var24 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var5);
         boolean var27 = var24.equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1)) && !var2.i();
         String var30 = "notEvaluated";
         String var32 = null;
         if (var2.e() != null) {
            try {
               var30 = Boolean.TRUE.equals(var2.e().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1), io.karatelabs.ext.rules.twin.CanonicalJson.d(var5), var6))
                  ? "held"
                  : "failed";
            } catch (RuntimeException var18) {
               var30 = "error";
               var32 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var18);
            }
         }

         boolean var12 = var0.h().b() > 0 && var0.h().c() == 0;
         return new io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult("applied", null, null, null, null, null, null, null, var5, var24, var27, var12, var30, var32, a(var0, var5));
      }
   }

   private static Object a(TwinModel var0, Object var1, Map<String, Object> var2) {
      Object var3 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var1);
      String var4 = var0.e();
      if (var4 != null && var2 != null && var2.containsKey(var4)) {
         a(var3).put(var4, var2.get(var4));
      }

      return var3;
   }

   static boolean a(io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var0) {
      return "error".equals(var0.b()) && "TIMEOUT".equals(var0.f());
   }

   static boolean b(TwinModel var0, Object var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3) {
      if (var2.b() == null) {
         return true;
      } else {
         try {
            return Boolean.TRUE.equals(var2.b().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(a(var0, var1, var3))));
         } catch (RuntimeException var5) {
            return false;
         }
      }
   }

   static List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> a(TwinModel var0, Object var1) {
      ArrayList<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> var2 = new ArrayList(var0.d().size());

      for (io.karatelabs.ext.rules.twin.TwinModel.InvariantModel var4 : var0.d()) {
         var2.add(new io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation(var4.a(), a(var4.b(), var1)));
      }

      return var2;
   }

   static String a(JavaCallable var0, Object var1) {
      try {
         return Boolean.TRUE.equals(var0.call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1))) ? "held" : "failed";
      } catch (RuntimeException var3) {
         return "error";
      }
   }

   private static String a(RuntimeException var0) {
      io.karatelabs.ext.rules.RuleApiException var1 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var0, io.karatelabs.ext.rules.RuleApiException.class);
      if (var1 != null) {
         return var1.b;
      } else {
         TwinException var2 = io.karatelabs.ext.rules.twin.ThrowableUtils.a(var0, TwinException.class);
         return var2 == null ? null : var2.b();
      }
   }

   private static io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult a(String var0, String var1, String var2, String var3, String var4) {
      return new io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult("refused", var0, var1, null, null, var2, var3, var4, null, null, false, false, null, null, null);
   }

   private static io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult a(String var0, String var1) {
      return a(var0, var1, null);
   }

   private static io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult a(String var0, String var1, String var2) {
      return new io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult("error", var0, null, var1, var2, null, null, null, null, null, false, false, null, null, null);
   }

   private static Map<String, Object> a(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static String a(Object var0, String var1) {
      var var2 = a(var0);
      return var2.containsKey(var1) ? io.karatelabs.ext.rules.twin.CanonicalJson.c(var2.get(var1)) : "(absent)";
   }

   private static String b(Object var0) {
      if (var0 instanceof String) {
         return "string";
      } else if (var0 instanceof Boolean) {
         return "boolean";
      } else if (var0 instanceof Number) {
         return "number";
      } else {
         return var0 instanceof JavaCallable ? "function" : "object";
      }
   }

   public record InvariantEvaluation(String a, String b) {

      public InvariantEvaluation(String a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   public record CommandResult(String b, String c, String d, String e, String f, String g, String h, String i, Object j, String k, boolean l, boolean m, String n, String o, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> p) {

      public CommandResult(String b, String c, String d, String e, String f, String g, String h, String i, Object j, String k, boolean l, boolean m, String n, String o, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> p) {
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
      }

      public boolean a() {
         return "applied".equals(this.b);
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public Object j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public boolean l() {
         return this.l;
      }

      public boolean m() {
         return this.m;
      }

      public String n() {
         return this.n;
      }

      public String o() {
         return this.o;
      }

      public List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> p() {
         return this.p;
      }
   }
}
