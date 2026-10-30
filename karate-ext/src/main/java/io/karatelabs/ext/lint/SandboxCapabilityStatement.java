package io.karatelabs.ext.lint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SandboxCapabilityStatement {
   public static final int CAPABILITY_VERSION = 2;

   private SandboxCapabilityStatement() {
   }

   public static Map<String, Object> statement() {
      LinkedHashMap<java.lang.String, java.lang.Object> var0 = new LinkedHashMap();
      var0.put("version", 2);
      var0.put(
         "grants",
         List.of(
            capability(
               "$",
               "the artifact under lint, bound as a private copy",
               "A rule reads the document — the raw one, or the $ref-resolved view when the rule declares `resolved`. Either way it is a copy, so a rule that writes through it mutates nothing the caller can observe."
            ),
            capability(
               "Coverage",
               "the read-only project coverage graph",
               "Enumerated verb-by-verb: "
                  + String.join(", ", ReadOnlyCoverage.GRANTED)
                  + ". Excluded by name: "
                  + String.join(", ", ReadOnlyCoverage.EXCLUDED)
                  + " — it hands back a filesystem path, and a sandbox that denies the filesystem must not leak one through a read verb."
            ),
            capability(
               "console",
               "write-only to the host log",
               "A rule may write to the process log; it can read nothing back, and this is the only binding that reaches outside the engine at all."
            ),
            capability(
               "fn",
               "the lint helper library",
               "Pure predicates over their arguments and the bound document: "
                  + String.join(", ", LintFns.NAMES)
                  + ". `fn.schema` is the shipped JSON Schema validator's non-throwing form (D218), not a second implementation."
            )
         )
      );
      var0.put(
         "denies",
         List.of(
            deniedCapability("filesystem", "no read/write/list binding is placed in the engine, and no reachable object exposes one"),
            deniedCapability("network", "no fetch/http/socket binding is placed in the engine"),
            deniedCapability("process", "no exec/spawn binding is placed in the engine"),
            deniedCapability("java-interop", "no ExternalBridge is installed on the engine, so `Java.type(...)` throws rather than resolving a class")
         )
      );
      var0.put(
         "bounds",
         List.of(
            boundCapability("wall-clock", "each rule evaluation is bounded and cancelled with an interrupt (override KARATE_LINT_TIMEOUT_MS; default 5000ms)"),
            boundCapability("single-thread", "rule evaluations serialize on one daemon thread"),
            boundCapability(
               "no ambient globals",
               "beyond the ECMAScript built-ins the engine provides (JSON, Math, RegExp, eval, Function, …), which confer no authority of their own — there is no host object for them to reach except the `console` grant above"
            )
         )
      );
      var0.put(
         "scope",
         "project-local, git-resident, human-reviewed rules (P25). A remote or unreviewed ruleset is out of scope and this statement is not an argument for extending one."
      );
      return var0;
   }

   public static String markdown() {
      var var0 = statement();
      StringBuilder var1 = new StringBuilder();
      var1.append("### Rule sandbox — capability statement v").append(2).append("\n\n");
      var1.append("Enumeration, not zero: a governance rule *does* have authority (a graph-reaching rule reads ")
         .append("the coverage graph), so the grants are listed and everything else is denied.\n\n");
      var1.append("**Granted**\n\n");

      for (Object var3 : listField(var0, "grants")) {
         appendBullet(var1, var3);
      }

      var1.append("\n**Denied**\n\n");

      for (Object var6 : listField(var0, "denies")) {
         appendBullet(var1, var6);
      }

      var1.append("\n**Bounded**\n\n");

      for (Object var7 : listField(var0, "bounds")) {
         appendBullet(var1, var7);
      }

      var1.append("\n").append(var0.get("scope")).append("\n");
      return var1.toString();
   }

   private static void appendBullet(StringBuilder var0, Object var1) {
      Map<java.lang.String, java.lang.Object> var2 = (Map)var1;
      var0.append("- `").append(var2.get("name")).append("` — ").append(var2.get("what"));
      Object var3 = var2.get("how");
      if (var3 != null) {
         var0.append(". ").append(var3);
      }

      var0.append("\n");
   }

   private static List<?> listField(Map<String, Object> var0, String var1) {
      return (List<?>)var0.get(var1);
   }

   private static Map<String, Object> capability(String var0, String var1, String var2) {
      return capabilityEntry(var0, var1, var2);
   }

   private static Map<String, Object> deniedCapability(String var0, String var1) {
      return capabilityEntry(var0, "denied", var1);
   }

   private static Map<String, Object> boundCapability(String var0, String var1) {
      return capabilityEntry(var0, "bounded", var1);
   }

   private static Map<String, Object> capabilityEntry(String var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("name", var0);
      var3.put("what", var1);
      var3.put("how", var2);
      return var3;
   }
}
