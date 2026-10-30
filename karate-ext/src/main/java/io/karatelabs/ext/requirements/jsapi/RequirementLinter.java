package io.karatelabs.ext.requirements.jsapi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class RequirementLinter {
   private static final Pattern a = Pattern.compile(".*\\b(shall|must)\\b.*", 2);
   private static final Pattern b = Pattern.compile("\\b(and|or)\\b", 2);
   private static final Set<String> c = Set.of(
      "redis",
      "kafka",
      "grpc",
      "postgres",
      "postgresql",
      "mysql",
      "mongodb",
      "sqlite",
      "microservice",
      "microservices",
      "docker",
      "kubernetes",
      "k8s",
      "nginx",
      "rabbitmq",
      "websocket",
      "memcached",
      "elasticsearch",
      "lambda",
      "dynamodb",
      "s3"
   );
   private static final int d = 4;
   private static final Set<String> e = Set.of("openapi", "grpc", "kafka", "asyncapi", "rules", "java");

   private RequirementLinter() {
   }

   static Map<String, Object> a(io.karatelabs.ext.requirements.model.RequirementsBundle var0) {
      var var1 = var0.d();
      var var2 = a(var1);
      LinkedHashMap<java.lang.String, java.lang.Integer> var3 = new LinkedHashMap();

      for (io.karatelabs.ext.requirements.model.RequirementNode var5 : (Iterable<io.karatelabs.ext.requirements.model.RequirementNode>) (Iterable<?>) (var1)) {
         var3.merge(var5.b(), 1, Integer::sum);
      }

      var var10 = var3.keySet();
      ArrayList var11 = new ArrayList();

      for (io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic var7 : var0.e()) {
         a(var11, var7.b(), "structural", var7.a(), "warn", var7.c());
      }

      HashSet var12 = new HashSet();

      for (io.karatelabs.ext.requirements.model.RequirementNode var8 : (Iterable<io.karatelabs.ext.requirements.model.RequirementNode>) (Iterable<?>) (var1)) {
         boolean var9 = !var2.contains(var8.b());
         a(var8, var9, var3, var10, var1, var12, var11);
         a(var8, var9, var11);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("findings", var11);
      var14.put("summary", a(var1.size(), var11));
      return var14;
   }

   private static void a(
      io.karatelabs.ext.requirements.model.RequirementNode var0,
      boolean var1,
      Map<String, Integer> var2,
      Set<String> var3,
      List<io.karatelabs.ext.requirements.model.RequirementNode> var4,
      Set<String> var5,
      List<Map<String, Object>> var6
   ) {
      if (d(var0.d())) {
         a(var6, var0.b(), "structural", "missing-title", "error", "requirement has no title");
      }

      if (var2.getOrDefault(var0.b(), 0) > 1 && var5.add(var0.b())) {
         a(var6, var0.b(), "structural", "duplicate-id", "error", "id is declared " + var2.get(var0.b()) + " times — ids must be unique");
      }

      if (e(var0.b()) && var0.b().contains("/")) {
         a(
            var6,
            var0.b(),
            "structural",
            "id-contains-slash",
            "error",
            "requirement id contains '/' — that is the reserved requirement/criterion separator (a tag @req="
               + var0.b()
               + " could not be told apart from a criterion ref); rename the id"
         );
      }

      if (e(var0.e())) {
         if (!var3.contains(var0.e())) {
            a(var6, var0.b(), "structural", "unresolved-parent", "error", "@parent= '" + var0.e() + "' does not resolve to a known requirement");
         } else if (b(var0, var4)) {
            a(var6, var0.b(), "structural", "parent-cycle", "error", "parent chain from '" + var0.b() + "' forms a cycle");
         }
      }

      if (var1) {
         if (var0.m() != null && !var0.m().isEmpty()) {
            for (String var8 : var0.m()) {
               if (!a.matcher(var8).matches()) {
                  a(
                     var6,
                     var0.b(),
                     "structural",
                     "acceptance-not-ears",
                     "warn",
                     "acceptance line is not an EARS obligation (no shall/must): \"" + c(var8) + "\""
                  );
               }
            }
         } else {
            a(var6, var0.b(), "structural", "leaf-missing-acceptance", "warn", "a leaf requirement has no acceptance criteria — nothing to verify against");
         }
      }

      if (var0.i() != null && "derived".equals(var0.i().get("source"))) {
         a(
            var6,
            var0.b(),
            "structural",
            "derived-stub",
            "warn",
            "a spec-derived stub restates the API, not the intent — give it a real title,"
               + (d(var0.h()) ? " @criticality," : "")
               + " acceptance; then drop @source=derived"
         );
      }

      a(var0, var6);
      if (e(var0.p())) {
         a(
            var6,
            var0.b(),
            "structural",
            "malformed-tag-line",
            "warn",
            "the tag line dropped '"
               + var0.p()
               + "' — a tag value cannot contain spaces (it truncates at the first space); check for a spaced value like @real=openapi:POST /x or @criticality=very high"
         );
      }
   }

   private static void a(io.karatelabs.ext.requirements.model.RequirementNode var0, List<Map<String, Object>> var1) {
      if (var0.k() != null) {
         for (String var3 : var0.k()) {
            if (!d(var3)) {
               int var4 = var3.indexOf(58);
               if (var4 > 0 && var4 != var3.length() - 1) {
                  String var5 = var3.substring(0, var4).toLowerCase();
                  if (!e.contains(var5)) {
                     a(
                        var1,
                        var0.b(),
                        "structural",
                        "real-target-malformed",
                        "warn",
                        "@real='"
                           + var3
                           + "' names an unrecognized namespace '"
                           + var5
                           + "' — expected one of openapi/grpc/kafka/asyncapi/rules/java; a value containing a space truncates at the space (e.g. 'openapi:POST /pets' → 'openapi:POST'), which then reads as unverified drift"
                     );
                  }
               } else {
                  a(
                     var1,
                     var0.b(),
                     "structural",
                     "real-target-malformed",
                     "warn",
                     "@real='"
                        + var3
                        + "' is not a namespaced artifact id — expected <scheme>:<id> (e.g. openapi:<opId>, grpc:<Service/Method>); list the real ones with Coverage.items('openapi') rather than guessing them from the spec"
                  );
               }
            }
         }
      }
   }

   private static boolean b(io.karatelabs.ext.requirements.model.RequirementNode var0, List<io.karatelabs.ext.requirements.model.RequirementNode> var1) {
      HashSet<java.lang.String> var2 = new HashSet();
      io.karatelabs.ext.requirements.model.RequirementNode var3 = var0;

      while (var3 != null && e(var3.e())) {
         if (!var2.add(var3.b())) {
            return true;
         }

         var3 = a(var1, var3.e());
      }

      return false;
   }

   private static void a(io.karatelabs.ext.requirements.model.RequirementNode var0, boolean var1, List<Map<String, Object>> var2) {
      String var3 = var0.l();
      if (e(var3)) {
         if (b.matcher(var3).find()) {
            a(var2, var0.b(), "lexical", "conjunction-in-goal", "info", "the goal joins clauses with and/or — consider splitting into separate requirements");
         }

         String var4 = a(var3);
         if (var4 != null) {
            a(
               var2,
               var0.b(),
               "lexical",
               "implementation-leak",
               "warn",
               "the goal names an implementation detail ('" + var4 + "') — state the outcome, not the how"
            );
         }
      }

      if (var1 && var0.n() != null && var0.n().size() > 4) {
         a(var2, var0.b(), "lexical", "constraint-count", "info", var0.n().size() + " constraints on one requirement — consider splitting it");
      }
   }

   private static String a(String var0) {
      for (String var4 : var0.toLowerCase().split("[^a-z0-9]+")) {
         if (c.contains(var4)) {
            return var4;
         }
      }

      return null;
   }

   private static Map<String, Object> a(int var0, List<Map<String, Object>> var1) {
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;
      LinkedHashMap<java.lang.String, java.lang.Integer> var5 = new LinkedHashMap();

      for (Map var7 : var1) {
         String var8 = String.valueOf(var7.get("severity"));
         switch (var8) {
            case "error":
               var2++;
               break;
            case "warn":
               var3++;
               break;
            default:
               var4++;
         }

         var5.merge(String.valueOf(var7.get("rule")), 1, Integer::sum);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
      var10.put("requirements", var0);
      var10.put("errors", var2);
      var10.put("warnings", var3);
      var10.put("infos", var4);
      var10.put("total", var1.size());
      var10.put("clean", var2 == 0 && var3 == 0);
      var10.put("byRule", var5);
      return var10;
   }

   static String a(Map<String, Object> var0) {
      Map var1 = (Map)var0.get("summary");
      List var2 = (List)var0.get("findings");
      StringBuilder var3 = new StringBuilder("# Requirements health\n\n");
      int var4 = a(var1, "errors");
      int var5 = a(var1, "warnings");
      String var6 = var4 > 0 ? "❌ FAIL" : (var5 > 0 ? "⚠️ ISSUES" : "✅ CLEAN");
      var3.append("**")
         .append(var6)
         .append("** — ")
         .append(a(var1, "requirements"))
         .append(" requirements · ")
         .append(var4)
         .append(" errors · ")
         .append(var5)
         .append(" warnings · ")
         .append(a(var1, "infos"))
         .append(" info\n\n");
      if (var2.isEmpty()) {
         var3.append("_No issues — requirements are structurally and lexically clean._\n");
         return var3.toString();
      } else {
         var3.append("| Requirement | Tier | Rule | Severity | Message |\n");
         var3.append("|---|---|---|---|---|\n");

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var2)) {
            var3.append("| ")
               .append(a(var8.get("id")))
               .append(" | ")
               .append(a(var8.get("tier")))
               .append(" | ")
               .append(a(var8.get("rule")))
               .append(" | ")
               .append(b(a(var8.get("severity"))))
               .append(" | ")
               .append(a(var8.get("message")))
               .append(" |\n");
         }

         return var3.toString();
      }
   }

   private static String b(String var0) {
      return switch (var0) {
         case "error" -> "❌ error";
         case "warn" -> "⚠️ warn";
         default -> "ℹ️ info";
      };
   }

   private static Set<String> a(List<io.karatelabs.ext.requirements.model.RequirementNode> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (io.karatelabs.ext.requirements.model.RequirementNode var3 : var0) {
         if (e(var3.e())) {
            var1.add(var3.e());
         }
      }

      return var1;
   }

   private static io.karatelabs.ext.requirements.model.RequirementNode a(List<io.karatelabs.ext.requirements.model.RequirementNode> var0, String var1) {
      for (io.karatelabs.ext.requirements.model.RequirementNode var3 : var0) {
         if (var3.b().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   private static void a(List<Map<String, Object>> var0, String var1, String var2, String var3, String var4, String var5) {
      LinkedHashMap<String, Object> var6 = new LinkedHashMap();
      var6.put("id", var1);
      var6.put("tier", var2);
      var6.put("rule", var3);
      var6.put("severity", var4);
      var6.put("message", var5);
      var0.add(var6);
   }

   private static String c(String var0) {
      return var0.length() <= 60 ? var0 : var0.substring(0, 57) + "...";
   }

   private static int a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var3 ? var3.intValue() : 0;
   }

   private static boolean d(String var0) {
      return var0 == null || var0.isBlank();
   }

   private static boolean e(String var0) {
      return var0 != null && !var0.isBlank();
   }

   private static String a(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }
}
