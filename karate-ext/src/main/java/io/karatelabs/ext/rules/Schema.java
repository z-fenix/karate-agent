package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.js.Engine;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import io.karatelabs.match.Match.Type;
import io.karatelabs.match.Result.Failure;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Schema extends JsDispatch {
   private final RulebookRepository manager;
   private static final String DATE_MARKER = "#date";
   private static final String DATE_PATTERN = "#regex \\d{4}-\\d{2}-\\d{2}([T ].*)?";
   private static final String OPTIONAL = "#";
   private static final String OPTIONAL_DATE_MARKER = "##date";
   static final String OPTIONAL_PREFIX = "##";
   private static final String PREDICATE_FAILED = "evaluated to";
   private static final int MAX_REASONS = 3;

   public Schema(Path var1) {
      this.manager = new RulebookRepository(var1);
      this.fn("list", var1x -> this.list());
      this.fn("load", var1x -> this.load(JsApi.str(var1x, 0)));
      this.fn("validate", var1x -> this.validate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public static Schema atRulesHome(Path var0) {
      return new Schema(var0);
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Schema — validate data against a Rulebook's shape\n\nOne Schema per Rulebook — the rule folder's `schema.js` shape — keyed by rule name. Call\n`Schema.verb(args)` and end with the value.\n\nThe shape is a karate match-schema (`'#string'`, `'#number'`, `'#boolean'`, nested\nobjects) **plus three conventions of our own** — the same three `Rule.ranges` reads,\nso one declaration buys enforcement and analysis at once:\n\n- **a list of literals is an ENUM** — its members are the whole domain, so\n  `state: ['CA','NY']` accepts `'CA'` and refuses `'ZZ'`. It is NOT a literal array the\n  row must equal, and membership is strict (`'1'` is not `1`). An empty list is refused\n  at load: it would declare a field no value can satisfy.\n- **`[ {...} ]` is an element TEMPLATE** — \"every element looks like this\", not \"exactly\n  one element\". Cardinality is not the shape's business; declare it with `g.array` in\n  `generator.js`.\n- **`'#date'` is ours**, not karate's — an ISO `YYYY-MM-DD` (an optional time part is\n  tolerated); `'##date'` is its optional form.\n- **a numeric RANGE is ours too** — `'#int[0,6]'`, `'#number[0,25]'`, one side open\n  (`'#int[18,]'`, `'#number[,100]'`); `#int` demands a whole number. The range is the\n  DOMAIN: it is enforced here and it is the universe every verdict is read over.\n  `generator.js` may narrow it (`g.int('age',18,70)` under `'#int[0,120]'`) and is\n  refused if it widens it.\n\nA field is REQUIRED by default and no undeclared key is allowed — the match is strict\nboth ways. **Make one optional with karate's `##` prefix** — `notes: '##string'` accepts a\nrow that omits it (or carries null) and still type-checks the value when present; `##date`\nworks too — as does `##int[0,6]`. The ENUM form has no optional spelling yet: a list of\nliterals is always required.\n\n- `Schema.list()` — schemas available (one per rule) → `[{name}]`.\n- `Schema.load(name)` — `{name, schema}` (the shape).\n- `Schema.validate(name, row)` — match `row` against the shape via the core `Match` engine\n  → `{pass, message?, failures}`. **Top-level** engine keys (anything starting with `_`)\n  are ignored on both sides, so a saved scenario row validates as-is. A `_`-prefixed key\n  nested inside an object or array element is NOT ignored — it is matched like any field.\n\nTo claim that the schema must REFUSE a row, save it as a scenario with\n`_expect: 'schema-reject'` and read the verdict in `Rule.check(name).rejects`.\n\nA failure returns `{error, code, status}` (data, not a throw). Related: `Rule` (the oracle\n+ its scenarios — row-tables + the oracle-vs-live diff)."
      );
   }

   Object list() {
      try {
         ArrayList<java.util.Map<java.lang.String,java.lang.String>> var1 = new ArrayList();

         for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var3 : this.manager.b()) {
            var1.add(Map.of("name", var3.a()));
         }

         return var1;
      } catch (RuntimeException var4) {
         return JsApi.error(var4);
      }
   }

   Map<String, Object> load(String var1) {
      if (var1 == null) {
         return JsApi.missing("Schema.load(name) requires a schema (rule) name");
      } else {
         try {
            LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
            var2.put("name", var1);
            var2.put("schema", this.manager.b(var1).e());
            return var2;
         } catch (RuntimeException var3) {
            return JsApi.error(var3);
         }
      }
   }

   Map<String, Object> validate(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Schema.validate(name, row) requires a schema (rule) name");
      } else if (var2 == null) {
         return JsApi.missing("Schema.validate(name, row) requires a row object to validate");
      } else {
         try {
            Object var3 = this.manager.b(var1).e();
            return matchResult(validateAgainst(var3, var2));
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   static Result validateAgainst(Object var0, Object var1) {
      Object var2 = var0 instanceof Map var3 ? RulebookRepository.d(var3) : var0;
      Object var5 = var1 instanceof Map var4 ? RulebookRepository.d(var4) : var1;
      Engine var6 = new Engine();
      return Match.execute(var6, Type.EQUALS, var5, rewrite(var2, var6, new int[1]));
   }

   private static Object rewrite(Object var0, Engine var1, int[] var2) {
      if (var0 instanceof Map var8) {
         LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();

         for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
            var9.put(String.valueOf(var11.getKey()), rewrite(var11.getValue(), var1, var2));
         }

         return var9;
      } else if (!(var0 instanceof List var3)) {
         return translate(var0);
      } else if (var3.size() == 1 && var3.get(0) instanceof Map) {
         return elementMacro(rewrite(var3.get(0), var1, var2), var1, var2);
      } else {
         var var4 = enumMembers(var3);
         if (var4 != null) {
            return enumMacro(var4);
         } else {
            ArrayList<java.lang.Object> var5 = new ArrayList();

            for (Object var7 : var3) {
               var5.add(rewrite(var7, var1, var2));
            }

            return var5;
         }
      }
   }

   static Object translate(Object var0) {
      if (var0 instanceof String var1) {
         if ("#date".equals(var1)) {
            return "#regex \\d{4}-\\d{2}-\\d{2}([T ].*)?";
         } else if ("##date".equals(var1)) {
            return "##regex \\d{4}-\\d{2}-\\d{2}([T ].*)?";
         } else {
            io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var2;
            try {
               var2 = io.karatelabs.ext.rules.NumericBoundsParser.a(var1);
            } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var4) {
               throw new RuleApiException(
                  400,
                  "SCHEMA_INVALID",
                  "schema.js: '"
                     + var1
                     + "' "
                     + (
                        var4.a.equals("notExact")
                           ? "declares an integer bound beyond 2^53, which no double counts one by one"
                           : "is not a readable range — write #int[lo,hi] or #number[lo,hi]; either side may be left empty"
                     )
               );
            }

            return var2 == null ? var0 : (var1.startsWith("##") ? "#" : "") + rangeMacro(var2);
         }
      } else {
         return var0;
      }
   }

   private static String rangeMacro(io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var0) {
      StringBuilder var1 = new StringBuilder("#? typeof _ === \"number\"");
      if (var0.c()) {
         var1.append(" && Number.isInteger(_)");
      }

      if (var0.a() != null) {
         var1.append(" && _ >= ").append(number(var0.a()));
      }

      if (var0.b() != null) {
         var1.append(" && _ <= ").append(number(var0.b()));
      }

      return var1.toString();
   }

   private static String number(double var0) {
      return var0 == Math.rint(var0) ? String.valueOf((long)var0) : String.valueOf(var0);
   }

   static List<?> enumMembers(Object var0) {
      return var0 instanceof List var1 && !var1.isEmpty() && var1.stream().noneMatch(var0x -> var0x instanceof Map || var0x instanceof List) ? var1 : null;
   }

   private static String enumMacro(List<?> var0) {
      StringBuilder var1 = new StringBuilder("#? ");

      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (var2 > 0) {
            var1.append(" || ");
         }

         var1.append("_ === ").append(io.karatelabs.ext.rules.JsLiteralWriter.a(var0.get(var2)));
      }

      return var1.toString();
   }

   private static String elementMacro(Object var0, Engine var1, int[] var2) {
      String var3 = "__karateElementShape" + var2[0]++;
      var1.put(var3, var0);
      return "#[] " + var3;
   }

   static Map<String, Object> rejectVerdict(Object var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("_id", var1.get("_id"));
      if (var1.get("_label") != null) {
         var2.put("_label", var1.get("_label"));
      }

      var var3 = RulebookRepository.d(var1);
      if (var3.isEmpty()) {
         var2.put("status", "NOTCHECKED");
         var2.put("pass", false);
         var2.put(
            "why",
            "this reject row carries no business input, so a mismatch against the shape is arithmetic, not evidence — add the malformed field(s) whose refusal you want proved"
         );
         return var2;
      } else {
         Result var4 = validateAgainst(var0, var1);
         var2.put("status", var4.pass ? "ACCEPTED" : "REFUSED");
         var2.put("pass", !var4.pass);
         var2.put(
            "why",
            var4.pass
               ? "the schema ACCEPTED this row — either the shape is too loose to refuse it, or this row is not actually malformed"
               : "the schema refused it: " + refusalReason(var0, var4)
         );
         return var2;
      }
   }

   private static String refusalReason(Object var0, Result var1) {
      if (var1.failures != null && !var1.failures.isEmpty()) {
         var var2 = new ArrayList<>(var1.failures);
         var2.sort(Comparator.comparingInt(Failure::depth).reversed());
         StringBuilder var3 = new StringBuilder();
         int var4 = 0;

         for (Failure var6 : (Iterable<Failure>) (Iterable<?>) (var2)) {
            if (var4 == 3) {
               var3.append(", + ").append(var2.size() - var4).append(" more");
               break;
            }

            if (var4++ > 0) {
               var3.append("; ");
            }

            var3.append(var6.path()).append(' ').append(reasonFor(var0, var6));
         }

         return var3.toString();
      } else {
         return "shape mismatch";
      }
   }

   private static String reasonFor(Object var0, Failure var1) {
      if (var1.reason() != null && var1.reason().contains("evaluated to")) {
         var var2 = declaredEnumAt(var0, var1.path());
         if (var2 != null) {
            StringBuilder var3 = new StringBuilder("is ").append(io.karatelabs.ext.rules.JsLiteralWriter.a(var1.actualValue())).append(" — not one of [");

            for (int var6 = 0; var6 < var2.size(); var6++) {
               var3.append(var6 > 0 ? ", " : "").append(io.karatelabs.ext.rules.JsLiteralWriter.a(var2.get(var6)));
            }

            return var3.append(']').toString();
         } else {
            if (declaredAt(var0, var1.path()) instanceof String var4) {
               io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var7 = io.karatelabs.ext.rules.NumericBoundsParser.a(var4);
               if (var7 instanceof io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds) {
                  return rangeReason(var7, var1.actualValue());
               }
            }

            return var1.reason();
         }
      } else {
         return var1.reason();
      }
   }

   private static List<?> declaredEnumAt(Object var0, String var1) {
      return enumMembers(declaredAt(var0, var1));
   }

   private static Object declaredAt(Object var0, String var1) {
      if (var1 != null && var1.startsWith("$")) {
         Object var2 = var0;

         for (String var6 : var1.substring(1).replace("[", ".").replace("]", "").split("\\.")) {
            if (!var6.isBlank()) {
               if (var2 instanceof Map var8) {
                  var2 = var8.get(var6);
               } else {
                  if (!(var2 instanceof List var7)) {
                     return null;
                  }

                  var2 = var7.isEmpty() ? null : var7.get(0);
               }
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   private static String rangeReason(io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var0, Object var1) {
      if (var1 instanceof Number var2) {
         double var3 = var2.doubleValue();
         return var0.c() && var3 != Math.rint(var3)
            ? "not a whole number"
            : "is " + io.karatelabs.ext.rules.JsLiteralWriter.a(var1) + " — outside [" + (var0.a() == null ? "" : number(var0.a())) + ", " + (var0.b() == null ? "" : number(var0.b())) + "]";
      } else {
         return "not a number";
      }
   }

   static List<String> closedDomainViolations(Object var0, Object var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      collectOutsideDomain(var0, var1, "", var2, new Engine());
      return var2;
   }

   private static void collectOutsideDomain(Object var0, Object var1, String var2, List<String> var3, Engine var4) {
      var var5 = enumMembers(var0);
      if (var5 != null) {
         if (!Match.execute(var4, Type.EQUALS, var1, enumMacro(var5)).pass) {
            StringBuilder var13 = new StringBuilder(var2).append(' ').append(io.karatelabs.ext.rules.JsLiteralWriter.a(var1)).append(" is outside its declared domain [");

            for (int var14 = 0; var14 < var5.size(); var14++) {
               var13.append(var14 > 0 ? ", " : "").append(io.karatelabs.ext.rules.JsLiteralWriter.a(var5.get(var14)));
            }

            var3.add(var13.append(']').toString());
         }
      } else {
         if (var0 instanceof Map var6 && var1 instanceof Map var7) {
            for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
               String var12 = String.valueOf(var11.getKey());
               if (!var12.startsWith("_") && var7.containsKey(var12)) {
                  collectOutsideDomain(var11.getValue(), var7.get(var12), var2.isEmpty() ? var12 : var2 + "." + var12, var3, var4);
               }
            }
         } else if (var0 instanceof List var8 && var8.size() == 1 && var8.get(0) instanceof Map && var1 instanceof List var9) {
            for (int var10 = 0; var10 < var9.size(); var10++) {
               collectOutsideDomain(var8.get(0), var9.get(var10), var2 + "[" + var10 + "]", var3, var4);
            }
         }
      }
   }

   static List<Map<String, Object>> rejectVerdicts(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : var0.c()) {
         var1.add(rejectVerdict(var0.e(), var3));
      }

      return var1;
   }

   static Map<String, Object> acceptVerdict(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      Object var1 = var0.e();
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      int var3 = 0;
      int var4 = 0;

      for (Map var6 : var0.b()) {
         if (!missingDeclaredKeys(var1, RulebookRepository.d(var6)).isEmpty()) {
            var4++;
         } else {
            var3++;
            Result var7 = validateAgainst(var1, var6);
            if (!var7.pass) {
               LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
               var8.put("_id", var6.get("_id"));
               if (var6.get("_label") != null) {
                  var8.put("_label", var6.get("_label"));
               }

               var8.put("why", "the schema refused a row saved as valid: " + refusalReason(var1, var7) + " — either this row or schema.js is wrong");
               var2.add(var8);
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("status", var3 == 0 ? "NOTCHECKED" : (var2.isEmpty() ? "ACCEPTED" : "REFUSED"));
      var9.put("checked", var3);
      var9.put("incomplete", var4);
      var9.put("refused", var2);
      return var9;
   }

   static List<String> missingDeclaredKeys(Object var0, Object var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      collectMissing(var0, var1, "", var2);
      return var2;
   }

   private static void collectMissing(Object var0, Object var1, String var2, List<String> var3) {
      if (var0 instanceof Map var4) {
         if (!(var1 instanceof Map var7)) {
            return;
         }

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var10 = String.valueOf(var9.getKey());
            if (!var10.startsWith("_")) {
               String var11 = var2.isEmpty() ? var10 : var2 + "." + var10;
               if (!var7.containsKey(var10)) {
                  if (!(var9.getValue() instanceof String var12 && var12.startsWith("##"))) {
                     var3.add(var11);
                  }
               } else {
                  collectMissing(var9.getValue(), var7.get(var10), var11, var3);
               }
            }
         }
      } else if (var0 instanceof List var5 && var5.size() == 1 && var5.get(0) instanceof Map && var1 instanceof List var6) {
         for (int var14 = 0; var14 < var6.size(); var14++) {
            collectMissing(var5.get(0), var6.get(var14), var2 + "[" + var14 + "]", var3);
         }
      }
   }

   static Map<String, Object> matchResult(Result var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("pass", var0.pass);
      if (!var0.pass) {
         var1.put("message", var0.message);
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var0.failures != null) {
         for (Failure var4 : var0.failures) {
            var2.add(var4.toMap());
         }
      }

      var1.put("failures", var2);
      return var1;
   }
}
