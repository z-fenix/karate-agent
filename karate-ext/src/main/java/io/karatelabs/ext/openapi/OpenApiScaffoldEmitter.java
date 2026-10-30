package io.karatelabs.ext.openapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;
import io.karatelabs.js.Engine;
import io.karatelabs.match.Match;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenApiScaffoldEmitter implements ScaffoldEmitter {
   private final OpenApiOperationCatalog a;
   private final String b;
   private final String c;

   public OpenApiScaffoldEmitter(OpenApiOperationCatalog var1, String var2, String var3) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }

   @Override
   public Map<String, String> emit(Contract var1, ScaffoldContext var2) {
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
      var3.put("karate-boot.js", a(this.a.document(), this.b));
      var3.put("karate-config.js", a(this.a.document().basePaths()));
      var3.put("start-mock.js", b(this.b));
      var3.put(this.c, a(this.a, this.b));
      var3.put("requirements/derived.md", io.karatelabs.ext.openapi.DerivedRequirementsRenderer.a(this.a).a());
      return var3;
   }

   private static String a(OpenApiDocument var0, String var1) {
      String var2 = var0.title == null ? "the API" : var0.title;
      return "// Scaffolded by Openapi.scaffold from "
         + var2
         + " ("
         + var1
         + ") — a runnable\n// 3-source coverage project. The spec backs both the stateful mock and the openapi source.\nboot.ext('http');                   // the in-run Http global — backs the stateful mock (auto-stopped)\nconst cov = boot.ext('coverage');\ncov.openapi = '"
         + var1
         + "';"
         + a(var1)
         + "// (1) openapi source — operations exercised by the run\ncov.requirements = 'requirements';  // (2) req source — derived stubs, @real-wired to operations\n// (3) add cov.rules = 'rulebooks' for a third source where domain logic needs a Rulebook.\n";
   }

   private static String a(String var0) {
      int var1 = Math.max(1, 19 - var0.length());
      return " ".repeat(var1);
   }

   private static String a(List<String> var0) {
      String var1 = var0.isEmpty() ? "" : (String)var0.getFirst();
      return "function fn() {\n    var started = karate.callSingle('/start-mock.js');\n    return { baseUrl: started.url"
         + (var1.isEmpty() ? "" : " + '" + var1 + "'")
         + " };\n}\n";
   }

   private static String b(String var0) {
      return "// The stateful Http.mock backend, from the spec alone (CRUD by convention); a request body\n// that breaks the spec's schema gets a 400.\nfunction() {\n    var mock = Http.mock('"
         + var0
         + "', { validate: true });\n    return { url: mock.url };\n}\n";
   }

   private static String a(OpenApiOperationCatalog var0, String var1) {
      String var2 = var0.document().title == null ? "API" : var0.document().title;
      StringBuilder var3 = new StringBuilder();
      var3.append("@scaffold\n");
      var3.append("Feature: ").append(var2).append(" — starter (scaffolded)\n\n");
      var3.append("  # Scaffolded by Openapi.scaffold from ").append(var1).append(". These calls become observed\n");
      var3.append("  # HTTP traffic the openapi coverage source scores; operations not called here stay the honest\n");
      var3.append("  # gaps, and the spec-derived requirement stubs stay NOTCOVERED until grounded (§2f).\n");
      var3.append("  #\n");
      var3.append("  # PROVISIONAL — bootstrap smoke. Its calls count as NORMAL openapi coverage (operations\n");
      var3.append("  # exercised), but because it is untagged it can only ever yield 'incidental' provenance on a\n");
      var3.append("  # requirement (never 'exercised' — that needs a @req= anchor), so it cannot make anything look\n");
      var3.append("  # ready. Replace it with intent-bearing @req=ID/criterion tests, then delete this file once they\n");
      var3.append("  # exercise the same operations. The @scaffold tag marks it disposable (unlike @ai, which a human\n");
      var3.append("  # must approve before removal).\n\n");
      var3.append("  Background:\n");
      var3.append("    * url baseUrl\n");
      int var4 = var3.length();
      int var5 = 0;
      byte var6 = 4;
      int var7 = 0;

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var9 : var0.resources()) {
         if (var7 >= var6) {
            break;
         }

         if (!var9.b().contains("{")) {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var10 = a(var0, var9, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.LIST);
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var11 = a(var0, var9, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.CREATE);
            if (var10 != null || var11 != null) {
               var7++;
               if (var10 != null) {
                  var3.append("\n  Scenario: list ").append(var9.a()).append('\n');
                  var3.append(c(var9.b()));
                  var3.append("    When method get\n");
                  var3.append("    Then status ").append(var10.d()).append('\n');
                  var3.append(a(var10));
                  var5++;
               }

               if (var11 != null) {
                  var3.append("\n  Scenario: create ").append(d(var9.a())).append('\n');
                  var3.append(c(var9.b()));
                  String var12 = b(var11);
                  if (var12 != null) {
                     var3.append("    And request ").append(var12).append('\n');
                  }

                  var3.append("    When method post\n");
                  var3.append("    Then status ").append(var11.d()).append('\n');
                  var3.append(a(var11));
                  var5++;
               }
            }
         }
      }

      if (var5 == 0) {
         int var13 = 3;

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var15 : var0.operations()) {
            if (var13 <= 0) {
               break;
            }

            if ("GET".equalsIgnoreCase(var15.b()) && !var15.c().contains("{")) {
               var3.append("\n  Scenario: ").append(c(var15)).append('\n');
               var3.append(c(var15.c()));
               var3.append("    When method get\n");
               var3.append("    Then status ").append(var15.d()).append('\n');
               var3.append(a(var15));
               var13--;
               var5++;
            }
         }
      }

      if (var3.indexOf("#[]", var4) >= 0 || var3.indexOf("match each", var4) >= 0) {
         var3.insert(var4, "    * configure matchEachEmptyAllowed = true\n");
      }

      if (var5 == 0) {
         var3.append("\n  # No parameter-free operations were detected to seed a starter scenario.\n");
         var3.append("  # Add your own calls below, then `karate run` to light up the openapi coverage source.\n");
      }

      return var3.toString();
   }

   private static String a(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      OpenApiSchemaNode var1 = var0.k();
      if (var1 == null) {
         return "";
      } else {
         try {
            io.karatelabs.ext.openapi.SchemaMatchConverter.MatchBuildResult var2 = "array".equals(var1.type()) && !var1.children.isEmpty() ? io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplateWithCompleteness(var1.children.getFirst()) : null;
            boolean var3 = var2 != null && var2.a() instanceof Map;
            io.karatelabs.ext.openapi.SchemaMatchConverter.MatchBuildResult var4 = var3 ? var2 : io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplateWithCompleteness(var1);
            Object var5 = var4.a();
            boolean var6 = var5 instanceof Map;
            Match.Type var7 = !var6
               ? Match.Type.EQUALS
               : (var3 ? (var4.b() ? Match.Type.EACH_EQUALS : Match.Type.EACH_CONTAINS_DEEP) : (var4.b() ? Match.Type.EQUALS : Match.Type.CONTAINS_DEEP));
            Object var8 = var0.e();
            if (var8 != null && var1.validateWithDirection(var8, io.karatelabs.ext.jsonschema.validation.ValidationDirection.RESPONSE).valid && !Match.execute(new Engine(), var7, var8, var5, true).pass) {
               return "";
            } else {
               String var9 = var6 && !var4.b() ? " contains deep " : " == ";
               return "    And match " + (var3 ? "each " : "") + "response" + var9 + Json.stringifyStrict(var5) + "\n";
            }
         } catch (RuntimeException var10) {
            return "";
         }
      }
   }

   private static io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo a(OpenApiOperationCatalog var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var1, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction var2) {
      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 : var0.operations()) {
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var5 = var0.resourceOperationFor(var4);
         if (var5 != null && var5.a() == var1 && var5.b() == var2) {
            return var4;
         }
      }

      return null;
   }

   private static String c(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var5 : var0.split("/")) {
         if (!var5.isEmpty()) {
            var1.add("'" + var5 + "'");
         }
      }

      return "    Given path " + String.join(", ", var1) + "\n";
   }

   private static String b(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      Object var1 = var0.g();
      return !(var1 instanceof Map) && !(var1 instanceof List) ? a(var0.f()) : Json.of(var1).toString();
   }

   private static String a(OpenApiSchemaNode var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            var0.markTreePresent();
            Object var1 = var0.generateSample();
            return var1 == null ? null : Json.of(var1).toString();
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private static String c(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      if (var0.h() != null && !var0.h().isBlank()) {
         return var0.h().trim();
      } else {
         return var0.a() != null && !var0.a().isBlank() ? var0.a() : var0.b().toLowerCase() + " " + var0.c();
      }
   }

   private static String d(String var0) {
      if (var0.endsWith("ies") && var0.length() > 3) {
         return var0.substring(0, var0.length() - 3) + "y";
      } else {
         return var0.endsWith("s") && var0.length() > 1 ? var0.substring(0, var0.length() - 1) : var0;
      }
   }
}
