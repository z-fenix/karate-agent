package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MutationEvaluator {
   static final String a = "catalog";
   static final String b = "mockStart";
   static final String c = "control";
   static final String d = "deck";
   static final String e = "candidate";
   static final String f = "PASS";
   static final String g = "FAIL";
   static final String h = "UNRESOLVED";
   static final String i = "NOTCHECKED";
   static final String j = "REFUSED";
   static final String k = "INVALID";
   static final String l = "TRANSPORT";
   static final String m = "TIMEOUT";
   static final String n = "START_FAILED";
   static final String o = "PARSE_FAILED";
   static final String p = "LINE_SHIFT";
   static final Set<String> q = Set.of(
      "PASS", "FAIL", "UNRESOLVED", "NOTCHECKED", "REFUSED", "INVALID", "TRANSPORT", "TIMEOUT", "START_FAILED", "PARSE_FAILED", "LINE_SHIFT"
   );
   private final List<String> r;
   private final List<String> s;
   private final Map<String, Object> t;
   private final List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> u = new ArrayList<>();

   MutationEvaluator(List<String> var1, List<String> var2) {
      this.r = List.copyOf(var1);
      this.s = List.copyOf(var2);
      this.t = new LinkedHashMap<>();
      this.t.put("deck", this.r);
      this.t.put("candidates", this.s);
   }

   void a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var1) {
      this.u.add(var1);
   }

   List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> a() {
      return this.u;
   }

   io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict b() {
      return a(this.r, this.s, this.u);
   }

   Map<String, Object> c() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList(this.u.size());

      for (io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var3 : this.u) {
         var1.add(var3.a());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("manifest", this.t);
      var4.put("legs", var1);
      return var4;
   }

   static io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict a(List<String> var0, List<String> var1, List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var2) {
      io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var3 = a(var2, "catalog", null);
      if (var3 == null || !"PARSE_FAILED".equals(var3.d()) && !"LINE_SHIFT".equals(var3.d())) {
         io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var4 = a(var2, "control", null);
         boolean var5 = var4 != null && "PASS".equals(var4.d());
         io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var6 = a(var2, "mockStart", "START_FAILED");
         if (var6 != null && !var5) {
            return a("INVALID", c(var6));
         } else if (var4 != null && !var5) {
            return a("INVALID", "the empty control sequence read " + var4.d() + (var4.e() == null ? "" : " (" + var4.e() + ")"));
         } else {
            var var7 = b(var2, "deck");
            var var8 = b(var2, "candidate");
            io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var9 = a(var2, "deck");
            io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var10 = a(var2, "candidate");
            boolean var11 = var9 != null && "TIMEOUT".equals(var9.d());
            if (a(var7)) {
               return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict("KILLED", null, var11, false, null, false, false, false);
            } else if (var11) {
               return a("TIMEOUT", var9.f());
            } else {
               boolean var12 = !var1.isEmpty() || !var8.isEmpty() || var10 != null || var6 != null;
               boolean var13 = !var12 || a(var10, var8, var1);
               boolean var14 = a(var9, var7, var0) && var13;
               boolean var15 = !var14 || var9.g() == null || var12 && var10.g() == null;
               boolean var16 = a(var8);
               if (!var14 || var15 || Boolean.TRUE.equals(var9.g()) || var10 != null && Boolean.TRUE.equals(var10.g())) {
                  if (var14 && b(var7) && b(var8)) {
                     return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict(
                        "SCREENED",
                        "indistinguishable on the selected sequences (" + (var7.size() + var8.size()) + " replayed)",
                        false,
                        false,
                        null,
                        var15,
                        false,
                        var16
                     );
                  } else if (var16) {
                     return a("KILLED", null);
                  } else {
                     boolean var17 = var10 != null && "TIMEOUT".equals(var10.d());
                     String var18 = var10 != null && !"PASS".equals(var10.d()) && !var17
                        ? b(var10)
                        : (var6 != null ? b(var6) : (var12 && !var13 && !var17 ? "NOTCHECKED" : null));
                     return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict("SURVIVED", null, false, var17, var18, var15, false, false);
                  }
               } else {
                  return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict("NOTCOVERED", "no replayed sequence evaluated the mutated site", false, false, null, false, var16, false);
               }
            }
         }
      } else {
         return a("INVALID", var3.d());
      }
   }

   private static boolean a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var0, List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var1, List<String> var2) {
      if (var0 != null && "PASS".equals(var0.d())) {
         HashSet<java.lang.String> var3 = new HashSet();

         for (io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var5 : var1) {
            var3.add(var5.c());
         }

         return var3.containsAll(var2);
      } else {
         return false;
      }
   }

   private static io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict a(String var0, String var1) {
      return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict(var0, var1, false, false, null, false, false, false);
   }

   private static io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult a(List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var0, String var1, String var2) {
      for (io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var4 : var0) {
         if (var4.b().equals(var1) && (var2 == null || var2.equals(var4.d()))) {
            return var4;
         }
      }

      return null;
   }

   private static io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult a(List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var0, String var1) {
      for (io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var3 : var0) {
         if (var3.b().equals(var1) && var3.c() == null) {
            return var3;
         }
      }

      return null;
   }

   private static List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> b(List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var0, String var1) {
      ArrayList<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var2 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var4 : var0) {
         if (var4.b().equals(var1) && var4.c() != null) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static boolean a(List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var0) {
      return var0.stream().anyMatch(var0x -> "FAIL".equals(var0x.d()));
   }

   private static boolean b(List<io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult> var0) {
      return var0.stream().allMatch(var0x -> Boolean.TRUE.equals(var0x.h()));
   }

   private static String b(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var0) {
      return var0.e() == null ? var0.d() : var0.e();
   }

   private static String c(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult var0) {
      return var0.f() == null ? b(var0) : b(var0) + ": " + var0.f();
   }

   record MutationLegResult(String b, String c, String d, String e, String f, Boolean g, Boolean h, String i, String j) {

      MutationLegResult(String var1, String var2, String var3, String var4, String var5, Boolean var6, Boolean var7) {
         this(var1, var2, var3, var4, var5, var6, var7, null, null);
      }

      MutationLegResult(String b, String c, String d, String e, String f, Boolean g, Boolean h, String i, String j) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
      }

      static io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult a(String var0, String var1, String var2, String var3, String var4) {
         return new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult(var0, var1, var2, var3, var4, null, null, null, null);
      }

      Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("phase", this.b);
         a(var1, "sequenceId", this.c);
         var1.put("outcome", this.d);
         a(var1, "code", this.e);
         a(var1, "detail", this.f);
         a(var1, "reach", this.g);
         a(var1, "projectionEqual", this.h);
         a(var1, "trace", this.i);
         a(var1, "baselineSha", this.j);
         return var1;
      }

      private static void a(Map<String, Object> var0, String var1, Object var2) {
         if (var2 != null) {
            var0.put(var1, var2);
         }
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

      public Boolean g() {
         return this.g;
      }

      public Boolean h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }
   }

   record MutationVerdict(String a, String b, boolean c, boolean d, String e, boolean f, boolean g, boolean h) {

      MutationVerdict(String a, String b, boolean c, boolean d, String e, boolean f, boolean g, boolean h) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }
   }
}
