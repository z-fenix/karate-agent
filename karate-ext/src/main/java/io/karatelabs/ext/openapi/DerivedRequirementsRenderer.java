package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class DerivedRequirementsRenderer {
   private final OpenApiOperationCatalog a;
   private final String b;

   private DerivedRequirementsRenderer(OpenApiOperationCatalog var1, String var2) {
      this.a = var1;
      this.b = var2 != null && !var2.isBlank() ? var2.trim() : "API requirements";
   }

   public static DerivedRequirementsRenderer a(OpenApiOperationCatalog var0) {
      return new DerivedRequirementsRenderer(var0, var0.document().title);
   }

   public static DerivedRequirementsRenderer a(String var0) {
      return a(io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromPath(var0));
   }

   public String a() {
      StringBuilder var1 = new StringBuilder();
      var1.append("# ").append(this.b).append(" — derived requirements\n\n");
      var1.append("> Reverse-engineered from the OpenAPI spec by `scaffold-from-openapi`. These stubs are\n");
      var1.append("> **hollow** — they restate the API's behavior, not the business intent. Review each:\n");
      var1.append("> assign `@criticality`, rewrite the title/prose as a real requirement, refine the\n");
      var1.append("> acceptance criteria, then remove the `@source=derived` marker once grounded.\n");
      LinkedHashMap<java.lang.String, java.util.ArrayList> var2 = new LinkedHashMap();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 : this.a.operations()) {
         var2.computeIfAbsent(this.a(var4), var0 -> new ArrayList<>()).add(var4);
      }

      for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         String var5 = (String)var13.getKey();
         String var6 = c(var5);
         var1.append("\n## ").append(var6).append(": ").append(d(var5)).append('\n');
         var1.append("@type=feature\n");
         int var7 = 0;

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var9 : (Iterable<io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo>) (Iterable<?>) ((List)var13.getValue())) {
            String var10 = String.format("%s-%03d", var6, ++var7);
            var1.append("\n### ").append(var10).append(": ").append(this.b(var9)).append('\n');
            StringBuilder var11 = new StringBuilder("@source=derived");
            if (var9.a() != null && !var9.a().isBlank()) {
               var11.append(" @real=openapi:").append(var9.a().trim());
            }

            var1.append((CharSequence)var11).append('\n');
            var1.append("_Derived from `").append(var9.b()).append(' ').append(var9.c()).append("` → ").append(var9.d()).append("._\n");
            var1.append("\n**Acceptance:**\n");
            var1.append("- 1: ").append(c(var9)).append('\n');
         }
      }

      return var1.toString();
   }

   private String a(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var2 = this.a.resourceOperationFor(var1);
      if (var2 != null) {
         return var2.a().a();
      } else if (var1.i() != null && !var1.i().isEmpty() && var1.i().get(0) != null && !var1.i().get(0).isBlank()) {
         return var1.i().get(0).trim();
      } else {
         String var3 = b(var1.c());
         return var3 == null ? "api" : var3;
      }
   }

   private static String b(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for (String var4 : var0.split("/")) {
            String var5 = var4.trim();
            if (!var5.isEmpty() && (!var5.startsWith("{") || !var5.endsWith("}"))) {
               return var5;
            }
         }

         return null;
      }
   }

   private String b(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      if (var1.h() != null && !var1.h().isBlank()) {
         return var1.h().trim();
      } else {
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var2 = this.a.resourceOperationFor(var1);
         if (var2 != null) {
            String var3 = e(var2.a().a());

            return switch (var2.b()) {
               case LIST -> "List " + var2.a().a();
               case CREATE -> "Create a " + var3;
               case READ -> "Get a " + var3;
               case UPDATE -> "Update a " + var3;
               case PATCH -> "Patch a " + var3;
               case DELETE -> "Delete a " + var3;
            };
         } else {
            return var1.b() + " " + var1.c();
         }
      }
   }

   private static String c(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      String var1 = var0.f() != null ? " with a valid request body" : "";
      return "WHEN a client sends `" + var0.b() + " " + var0.c() + "`" + var1 + " THE SYSTEM SHALL respond " + var0.d();
   }

   private static String c(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (char var5 : var0.toUpperCase().toCharArray()) {
         if (Character.isLetterOrDigit(var5)) {
            var1.append(var5);
         }
      }

      return var1.isEmpty() ? "REQ" : var1.toString();
   }

   private static String d(String var0) {
      String var1 = var0.replace('-', ' ').replace('_', ' ').trim();
      return var1.isEmpty() ? "API" : Character.toUpperCase(var1.charAt(0)) + var1.substring(1);
   }

   private static String e(String var0) {
      return var0.length() > 1 && var0.endsWith("s") && !var0.endsWith("ss") ? var0.substring(0, var0.length() - 1) : var0;
   }
}
