package io.karatelabs.ext.openapi.changes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SchemaDiffGraph {
   private final Map<String, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode> a = new HashMap<>();
   private final Map<String, Integer> b = new HashMap<>();
   private io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState c;

   io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode a(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var3) {
      String var4 = a(var1) + "\u0000" + a(var2) + "\u0000" + var3;
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var5 = this.a.get(var4);
      if (var5 == null) {
         String var6 = !var1.a() && !var2.a() ? var1.d + "\u0000" + var2.d : null;
         var5 = new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode(var6, var6 == null ? -1 : this.a(var6), var1, var2);
         this.a.put(var4, var5);
      }

      return var5;
   }

   private static String a(ComparableSchema var0) {
      StringBuilder var1 = new StringBuilder();

      for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var3 : var0.e) {
         var1.append(var3.b()).append('\u0001');
      }

      return var1.toString();
   }

   private int a(String var1) {
      return this.b.computeIfAbsent(var1, var1x -> this.b.size());
   }

   boolean a(SchemaDiffWalker var1, ComparableSchema var2, ComparableSchema var3, int var4) {
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var5 = this.a(var2, var3, var1.c);
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var6 = new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState(this.c);
      Boolean var7 = this.a(var5, var1.d, var4, var6);
      if (var7 == null) {
         this.c = var6;
         int var8 = var1.b.f;

         try {
            var7 = this.a(var1, var5, var4, var6);
         } finally {
            this.c = var6.a;
            var1.b.f = var8;
         }

         a(var5, var6, var7, var4);
      }

      b(var6);
      return var7;
   }

   private boolean a(SchemaDiffWalker var1, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var2, int var3, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var4) {
      if (var3 > 256) {
         var4.i = true;
         return true;
      } else {
         if (var2.a != null) {
            var4.e.set(var2.b);
            if (var1.d.contains(var2.a)) {
               if (!var4.c.get(var2.b)) {
                  var4.d.set(var2.b);
               }

               return false;
            }
         }

         Integer var5 = var4.b.get(var2);
         if (var5 != null && var5 >= var3) {
            return false;
         } else {
            var4.b.put(var2, var3);
            if (var3 > var4.h) {
               var4.h = var3;
            }

            var4.f.add(var2);
            if (var2.a != null) {
               var1.d.add(var2.a);
               var4.c.set(var2.b);
            }

            try {
               if (var2.f == null || var2.g) {
                  var1.a(var2);
               }

               if (var2.e) {
                  a(var4);
                  return true;
               } else {
                  for (io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var9 : var2.f) {
                     if (this.a(var1, var9, var3 + 1, var4)) {
                        return true;
                     }
                  }

                  return false;
               }
            } finally {
               if (var2.a != null) {
                  var1.d.remove(var2.a);
                  var4.c.clear(var2.b);
               }

               var4.f.remove(var4.f.size() - 1);
            }
         }
      }
   }

   private static void a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var0) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var2 : var0.f) {
         if (var2.g) {
            return;
         }
      }

      var0.g = var0.f.toArray(new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[0]);
   }

   private Boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var1, Set<String> var2, int var3, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var4) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffMemo var6 : var1.h) {
         if (var6.b() != null) {
            if (a(var6.b(), var2)) {
               for (io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var10 : var6.b()) {
                  if (var10.a != null) {
                     var4.e.set(var10.b);
                  }
               }

               return true;
            }
         } else if ((var6.a() || var3 + var6.f() <= 256) && this.a(var6, var2)) {
            var4.e.or(var6.e());
            var4.d.or(var6.c());
            var4.h = var3 + var6.f();
            return var6.a();
         }
      }

      return null;
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[] var0, Set<String> var1) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var5 : var0) {
         if (var5.a != null && var1.contains(var5.a)) {
            return false;
         }
      }

      return true;
   }

   private boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffMemo var1, Set<String> var2) {
      int var3 = 0;

      for (String var5 : var2) {
         Integer var6 = this.b.get(var5);
         if (var6 != null && var1.e().get(var6)) {
            if (!var1.c().get(var6)) {
               return false;
            }

            var3++;
         }
      }

      return var3 == var1.d();
   }

   private static void a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var0, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var1, boolean var2, int var3) {
      if (!var2 || !var1.i) {
         if (var2 && var1.g != null) {
            var0.h.add(new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffMemo(true, var1.g, null, 0, null, 0));
         } else {
            var0.h.add(new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffMemo(var2, null, var1.d, var1.d.cardinality(), var1.e, var1.h - var3));
         }
      }
   }

   private static void b(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var0) {
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var1 = var0.a;
      if (var1 != null) {
         var1.e.or(var0.e);

         for (int var2 = var0.d.nextSetBit(0); var2 >= 0; var2 = var0.d.nextSetBit(var2 + 1)) {
            if (!var1.c.get(var2)) {
               var1.d.set(var2);
            }
         }

         if (var0.h > var1.h) {
            var1.h = var0.h;
         }

         var1.i = var1.i | var0.i;
      }
   }

   private record SchemaDiffMemo(boolean a, io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[] b, BitSet c, int d, BitSet e, int f) {
   }

   static final class SchemaDiffNode {
      final String a;
      final int b;
      ComparableSchema c;
      ComparableSchema d;
      boolean e;
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[] f;
      boolean g;
      final List<io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffMemo> h = new ArrayList<>(1);

      SchemaDiffNode(String var1, int var2, ComparableSchema var3, ComparableSchema var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
      }
   }

   private static final class SchemaTraversalState {
      final io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState a;
      final Map<io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode, Integer> b = new IdentityHashMap<>();
      final BitSet c = new BitSet();
      final BitSet d = new BitSet();
      final BitSet e = new BitSet();
      final List<io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode> f = new ArrayList<>();
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[] g;
      int h;
      boolean i;

      SchemaTraversalState(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaTraversalState var1) {
         this.a = var1;
      }
   }
}
