package io.karatelabs.ext.openapi.changes;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class DiffDocument {
   private static final Pattern g = Pattern.compile("3\\.[012](\\..*)?");
   final String a;
   final String b;
   final io.karatelabs.ext.sourcedoc.SourceDocument c;
   final Map<String, Object> d;
   final boolean e;
   final String f;

   private DiffDocument(String var1, String var2, io.karatelabs.ext.sourcedoc.SourceDocument var3, byte[] var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      if (var3.value() instanceof Map var5) {
         this.d = var5;
         if (this.d.containsKey("swagger")) {
            throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var1, var2, this.d("/swagger"));
         } else {
            String var7 = this.d("/openapi");
            if (var7 != null && g.matcher(var7).matches()) {
               this.e = var7.startsWith("3.0");
               this.f = "sha256:" + io.karatelabs.ext.common.Sha256.a(var4);
            } else {
               throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var1, var2, (Object)var7);
            }
         }
      } else {
         throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var1, var2, "the document is not an object");
      }
   }

   static DiffDocument a(String var0, String var1, String var2, byte[] var3) {
      io.karatelabs.ext.sourcedoc.SourceDocument var4;
      try {
         var4 = io.karatelabs.ext.sourcedoc.SourceDocument.parse(var1, var2);
      } catch (RuntimeException var6) {
         throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var0, var1, var6.getMessage());
      }

      return new DiffDocument(var0, var1, var4, var3);
   }

   private String d(String var1) {
      String var2 = this.c.scalarTextAt(var1);
      if (var2 != null) {
         return var2;
      } else {
         Object var3 = this.d.get(var1.substring(1));
         return var3 == null ? null : String.valueOf(var3);
      }
   }

   io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution a(Object var1, String var2) {
      String var3 = null;
      HashSet<java.lang.String> var4 = new HashSet();
      Object var5 = var1;

      String var6;
      for (var6 = var2; var5 instanceof Map; var6 = var3) {
         Map<java.lang.String, Object> var7 = (Map)var5;
         if (!(var7.get("$ref") instanceof String var8)) {
            break;
         }

         if (!var8.startsWith("#")) {
            return new io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution(var5, var6, var3, var8, false);
         }

         if (!var4.add(var8)) {
            return new io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution(var5, var6, var3, null, true);
         }

         Object var10 = this.a(var8);
         if (var10 == null) {
            return new io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution(var5, var6, var3, null, true);
         }

         var3 = var8.substring(1);
         var5 = var10;
      }

      return new io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution(var5, var6, var3, null, false);
   }

   Object a(String var1) {
      String var2 = var1.startsWith("#") ? var1.substring(1) : var1;
      if (var2.isEmpty()) {
         return this.d;
      } else if (!var2.startsWith("/")) {
         return null;
      } else {
         Object var3 = this.d;

         for (String var7 : var2.substring(1).split("/", -1)) {
            String var8 = var7.replace("~1", "/").replace("~0", "~");
            if (var3 instanceof Map var10) {
               var3 = var10.get(var8);
            } else {
               if (!(var3 instanceof List var9)) {
                  return null;
               }

               try {
                  int var11 = Integer.parseInt(var8);
                  var3 = var11 >= 0 && var11 < var9.size() ? var9.get(var11) : null;
               } catch (NumberFormatException var12) {
                  return null;
               }
            }

            if (var3 == null) {
               return null;
            }
         }

         return var3;
      }
   }

   Map<String, Object> b(String var1) {
      io.karatelabs.ext.sourcedoc.SourceDocument.ResolvedLocation var2 = this.c.resolveLocation(var1, io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.KEY);
      if (var2 == null) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("file", this.b);
         var3.put("line", var2.span().line());
         var3.put("col", var2.span().column());
         var3.put("exact", var2.exact());
         return var3;
      }
   }

   static String c(String var0) {
      return io.karatelabs.ext.sourcedoc.SourceDocument.escapePointerSegment(var0);
   }

   record ReferenceResolution(Object b, String c, String d, String e, boolean f) {


      boolean a() {
         return this.e == null && !this.f;
      }

      public Object b() {
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

      public boolean f() {
         return this.f;
      }
   }
}
