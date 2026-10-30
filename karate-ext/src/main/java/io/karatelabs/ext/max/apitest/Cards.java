package io.karatelabs.ext.max.apitest;

import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.SourceSpan;
import io.karatelabs.gherkin.Step;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Cards {
   private static final Pattern NAMES_ADDRESS = Pattern.compile("(?i)\\b(address|url|base|host|server|endpoint)\\b");
   private static final Pattern PATH_PART = Pattern.compile("'[^']*'|\"[^\"]*\"|[^,]+");
   private static final Pattern CHANNEL = Pattern.compile("karate\\.channel\\(\\s*['\"]([A-Za-z0-9_-]+)['\"]");

   private Cards() {
   }

   public static Cards.FeatureView read(FeatureSource var0, String var1) {
      return read(var0, var1, null, -1);
   }

   public static Cards.FeatureView read(FeatureSource var0, String var1, Integer var2, int var3) {
      if (var0.feature() == null) {
         return new Cards.FeatureView(var1, var0.version(), false, var0.notEditable(), null, List.of(), null, List.of());
      } else {
         Feature var4 = var0.feature();
         FeatureSnapshot var5 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
         var var6 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var4.getTags());
         Cards.Flow var7 = null;
         if (var4.getBackground() != null && var4.getBackground().getSteps() != null) {
            var var8 = cards(var0, var5, var1, null, null, var4.getBackground().getSteps(), null);
            var7 = new Cards.Flow(null, "Background", 0, false, true, List.of(), List.of(), count(var8, List.of()), var8, null);
         }

         ArrayList var22 = new ArrayList();
         var var9 = var4.getSections();

         for (int var10 = 0; var10 < var9.size(); var10++) {
            FeatureSection var11 = (FeatureSection)var9.get(var10);
            List var12 = var11.isOutline() ? var11.getScenarioOutline().getSteps() : var11.getScenario().getSteps();
            List var13 = var11.isOutline() ? var11.getScenarioOutline().getTags() : var11.getScenario().getTags();
            String var14 = var11.isOutline() ? var11.getScenarioOutline().getName() : var11.getScenario().getName();
            OutlineRows.Bound var15 = var11.isOutline() && var2 != null && var2 == var10 ? OutlineRows.bind(var11.getScenarioOutline(), var3) : null;
            List var16 = cards(var0, var5, var1, var10, var14, var12, var15);
            if (var11.isOutline()) {
               ScenarioOutline var17 = var11.getScenarioOutline();
               boolean var18 = OutlineRows.addressable(var17);
               int var19 = 0;

               for (int var20 = 0; var20 < var17.getExamplesTables().size(); var20++) {
                  ExamplesTable var21 = var17.getExamplesTables().get(var20);
                  var16.add(dataTable(var0, var1, var10, var14, var20, var21, var18 ? var19 : null));
                  var19 += OutlineRows.rows(var21);
               }
            }

            List var23 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var13);
            var var24 = new ArrayList<>(var6);
            var24.addAll(var23);
            var22.add(
               new Cards.Flow(
                  var10,
                  var15 == null ? var14 : var15.scenario().getName(),
                  var11.getLine(),
                  var11.isOutline(),
                  false,
                  var23,
                  reqLinks(var24),
                  count(var16, var7 == null ? List.of() : var7.cards()),
                  var16,
                  var15 == null ? null : var3
               )
            );
         }

         return new Cards.FeatureView(var1, var0.version(), var0.editable(), var0.notEditable(), var4.getName(), var6, var7, var22);
      }
   }

   private static List<Cards.Card> cards(FeatureSource var0, FeatureSnapshot var1, String var2, Integer var3, String var4, List<Step> var5, OutlineRows.Bound var6) {
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();
      var var9 = var1.a(var3);
      Object var10 = var9;
      if (var6 != null) {
         var10 = new ArrayList();

         for (int var11 = 0; var11 < var9.size(); var11++) {
            Step var12 = var6.scenario().getSteps().get(var11);
            io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var13 = new io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot(
               var12.getPrefix(),
               var12.getKeyword(),
               var12.getText(),
               var12.getDocString(),
               var12.getTable() == null ? null : var12.getTable().getRows(),
               ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var9.get(var11)).h()
            );
            ((java.util.List)var10).add(var13);
         }
      }

      for (int var19 = 0; var19 < var5.size(); var19++) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var20 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)((java.util.List)var10).get(var19);
         io.karatelabs.ext.max.apitest.StepClassifier.StepKind var21 = io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var9.get(var19));
         if (var21 == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT) {
            var8.add(var19);
         } else if (var21 == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD) {
            var8.add(var19);
            var7.add(call(var0, var2, var3, var4, var5, var9, (List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>)var10, var6, var8));
            var8 = new ArrayList();
         } else {
            if (!var8.isEmpty()) {
               var7.add(call(var0, var2, var3, var4, var5, var9, (List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>)var10, var6, var8));
               var8 = new ArrayList();
            }

            Cards.Kind var14;

            var payload = switch (var21) {
               case CHECK_STATUS -> {
                  var14 = Cards.Kind.CHECK;
                  Integer var22 = null;

                  try {
                     var22 = Integer.valueOf(var20.e().strip());
                  } catch (RuntimeException var18) {
                  }

                  yield new Cards.Check(var22, null, null, null, null);
               }
               case CHECK_MATCH -> {
                  var14 = Cards.Kind.CHECK;
                  io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot var16 = io.karatelabs.ext.max.apitest.StepClassifier.d(var20) != null ? io.karatelabs.ext.max.apitest.StepClassifier.d(var20) : io.karatelabs.ext.max.apitest.StepClassifier.d((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var9.get(var19));
                  yield new Cards.Check(null, var16.a(), var16.b(), var16.c(), var16.d());
               }
               case REMEMBER -> {
                  var14 = Cards.Kind.REMEMBER;
                  yield new Cards.Remember(io.karatelabs.ext.max.apitest.StepClassifier.e(var20), io.karatelabs.ext.max.apitest.StepClassifier.f(var20));
               }
               default -> {
                  var14 = Cards.Kind.ADVANCED;
                  yield new Cards.Advanced(var20.d(), io.karatelabs.ext.max.apitest.StepClassifier.b(var20));
               }
            };
            var7.add(card(var0, var2, var3, var4, var5, var9, (List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>)var10, var6, List.of(var19), var14, payload));
         }
      }

      if (!var8.isEmpty()) {
         var7.add(call(var0, var2, var3, var4, var5, var9, (List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot>)var10, var6, var8));
      }

      return var7;
   }

   private static Cards.Card call(
      FeatureSource var0, String var1, Integer var2, String var3, List<Step> var4, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var5, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var6, OutlineRows.Bound var7, List<Integer> var8
   ) {
      String var9 = null;
      String var10 = null;
      String var11 = null;
      ArrayList var12 = new ArrayList();
      ArrayList var13 = new ArrayList();
      ArrayList var14 = new ArrayList();
      ArrayList var15 = new ArrayList();
      ArrayList var16 = new ArrayList();

      for (int var18 : var8) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var19 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var6.get(var18);
         String var20 = var19.f() != null ? var19.f() : (var19.e() == null ? null : var19.e().strip());
         String var21 = var19.d();
         switch (var21) {
            case "method":
               var9 = var20;
               break;
            case "url":
               var10 = var20;
               break;
            case "path":
               var12.add(var20);
               break;
            case "param":
            case "params":
               var13.add(var20);
               break;
            case "header":
            case "headers":
               var14.add(var20);
               break;
            case "cookie":
            case "cookies":
               var15.add(var20);
               break;
            case "form field":
            case "form fields":
               var16.add(var20);
               break;
            case "request":
               var11 = var20;
         }
      }

      Cards.Call var23 = new Cards.Call(var9, var10, var12, var13, var14, var15, var16, var11);
      return card(var0, var1, var2, var3, var4, var5, var6, var7, var8, Cards.Kind.CALL, var23);
   }

   private static Cards.Card card(
      FeatureSource var0,
      String var1,
      Integer var2,
      String var3,
      List<Step> var4,
      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var5,
      List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var6,
      OutlineRows.Bound var7,
      List<Integer> var8,
      Cards.Kind var9,
      Object var10
   ) {
      Step var11 = (Step)var4.get((Integer)var8.get(0));
      Step var12 = (Step)var4.get((Integer)var8.get(var8.size() - 1));
      Cards.Span var13 = Cards.Span.of(var0, var11.getSpan()).through(Cards.Span.of(var0, var12.getSpan()));
      ArrayList var14 = new ArrayList();
      ArrayList var15 = new ArrayList();
      boolean var16 = var0.editable() && var9 != Cards.Kind.ADVANCED;

      for (int var18 : var8) {
         Step var19 = (Step)var4.get(var18);
         var var20 = FeatureSource.ownedComments(var19);

         for (int var21 = 0; var21 < var20.size(); var21++) {
            var14.add(
               new Cards.Comment(((SourceSpan)var20.get(var21)).text(var0.text()).strip(), Cards.Span.of(var0, (SourceSpan)var20.get(var21)), var18, var21)
            );
         }

         String var26 = var19.getSpan().text(var0.text());
         var15.add(var7 == null ? var26 : var7.apply(var26));
         if (var16 && (!io.karatelabs.ext.max.apitest.FeatureSnapshot.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var5.get(var18), var26) || var19.getTable() != null && FeatureSource.tableProblem(var19.getTable()) != null)) {
            var16 = false;
         }
      }

      String var22 = null;

      for (int var23 = 0; var23 < var8.size(); var23++) {
         var22 = title(FeatureSource.ownedComments((Step)var4.get((Integer)var8.get(var23))), var0.text());
         boolean var25 = var9 == Cards.Kind.CALL && "url".equals(((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var5.get((Integer)var8.get(var23))).d());
         if (!var25 || var22 != null && !NAMES_ADDRESS.matcher(var22).find()) {
            break;
         }

         var22 = null;
      }

      if (var22 != null && var7 != null) {
         var22 = var7.apply(var22);
      }

      if (var22 == null) {
         var22 = words(var9, var10, (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var6.get((Integer)var8.get(0)));
      }

      Cards.SourceRef var24 = new Cards.SourceRef(var1, var2, var3, var2 == null, (Integer)var8.get(0), (Integer)var8.get(var8.size() - 1), null);
      return new Cards.Card(var9, var22, var13, var14, var16, var24, var15, var10);
   }

   private static Cards.Card dataTable(FeatureSource var0, String var1, int var2, String var3, int var4, ExamplesTable var5, Integer var6) {
      ArrayList var7 = new ArrayList();
      boolean var8 = var5.getTable() != null && var5.getTable().isDynamic();
      if (var5.getTable() != null) {
         var var9 = var5.getTable().getRows();

         for (int var10 = 0; var10 < var9.size(); var10++) {
            var7.add(
               new Cards.Row(
                  var10,
                  var6 != null && var10 != 0 ? var6 + var10 - 1 : null,
                  List.copyOf((Collection<? extends String>)var9.get(var10)),
                  Cards.Span.of(var0, var5.getTable().getRowSpan(var10))
               )
            );
         }
      }

      var var13 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var5.getTags());
      String var14 = var5.getName() != null && !var5.getName().isBlank() ? "Examples: " + var5.getName() : "Examples";
      Cards.SourceRef var11 = new Cards.SourceRef(var1, var2, var3, false, -1, -1, var4);
      boolean var12 = var0.editable() && var5.getTable() != null && FeatureSource.tableProblem(var5.getTable()) == null;
      return new Cards.Card(
         Cards.Kind.DATA_TABLE,
         var14,
         Cards.Span.of(var0, var5.getSpan()),
         List.of(),
         var12,
         var11,
         List.of(var5.getSpan().text(var0.text())),
         new Cards.DataTable(var4, var5.getName(), var13, var8, var7)
      );
   }

   private static String title(List<SourceSpan> var0, String var1) {
      if (var0.isEmpty()) {
         return null;
      } else {
         StringBuilder var2 = new StringBuilder();

         for (SourceSpan var4 : var0) {
            String var5 = var4.text(var1).strip();

            while (var5.startsWith("#")) {
               var5 = var5.substring(1);
            }

            if (!var5.isBlank()) {
               var2.append(var2.isEmpty() ? "" : " ").append(var5.strip());
            }
         }

         return var2.isEmpty() ? null : var2.toString();
      }
   }

   private static String words(Cards.Kind var0, Object var1, io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var2) {
      if (var1 instanceof Cards.Call var7) {
         String var4 = var7.method() == null ? "request setup" : var7.method().toUpperCase();
         String var5 = address(var7);
         return var5.isEmpty() ? var4 : var4 + " " + var5;
      } else if (var1 instanceof Cards.Check var6) {
         return var6.status() != null
            ? "status " + var6.status()
            : (Boolean.TRUE.equals(var6.each()) ? "each " : "") + var6.actual() + " " + var6.operator() + " " + var6.expected();
      } else {
         return var1 instanceof Cards.Remember var3 ? "remember " + var3.variable() : var2.b();
      }
   }

   private static String address(Cards.Call var0) {
      String var1 = var0.url() == null ? "" : var0.url().trim();
      String var2 = quoted(var1) ? unquote(var1).replaceAll("/+$", "") : "";
      ArrayList var3 = new ArrayList();

      for (String var5 : var0.paths()) {
         Matcher var6 = PATH_PART.matcher(var5);

         while (var6.find()) {
            String var7 = var6.group().trim();
            if (!var7.isEmpty()) {
               var3.add(quoted(var7) ? unquote(var7).replaceAll("^/+|/+$", "") : "{" + var7 + "}");
            }
         }
      }

      return var3.isEmpty() ? var2 : var2 + "/" + String.join("/", var3);
   }

   private static boolean quoted(String var0) {
      return var0.length() >= 2 && (var0.startsWith("'") && var0.endsWith("'") || var0.startsWith("\"") && var0.endsWith("\""));
   }

   private static String unquote(String var0) {
      return var0.substring(1, var0.length() - 1);
   }

   public static boolean callInput(String var0) {
      return io.karatelabs.ext.max.apitest.StepClassifier.a(var0);
   }

   private static Cards.CheckCount count(List<Cards.Card> var0, List<Cards.Card> var1) {
      int var2 = 0;
      boolean var3 = false;
      var var4 = new ArrayList<>(var1);
      var4.addAll(var0);

      for (Cards.Card var6 : (Iterable<Cards.Card>) (Iterable<?>) (var4)) {
         if (var6.kind() == Cards.Kind.CHECK) {
            var2++;
         } else if (var6.detail() instanceof Cards.Advanced var7) {
            if (var7.assertion() == io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.KNOWN) {
               var2++;
            } else if (var7.assertion() == io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.UNKNOWN) {
               var3 = true;
            }
         }
      }

      return new Cards.CheckCount(var2, var3);
   }

   private static List<String> reqLinks(List<String> var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0) {
         if (var3.startsWith("@req=")) {
            for (String var7 : var3.substring("@req=".length()).split(",")) {
               if (!var7.isBlank()) {
                  var1.add(var7.strip());
               }
            }
         }
      }

      return var1;
   }

   public static List<String> channels(Cards.Flow var0) {
      ArrayList var1 = new ArrayList();

      for (Cards.Card var3 : var0.cards()) {
         for (String var5 : (Iterable<String>) (Iterable<?>) (var3.steps() == null ? List.of() : var3.steps())) {
            Matcher var6 = CHANNEL.matcher(var5);

            while (var6.find()) {
               if (!var1.contains(var6.group(1))) {
                  var1.add(var6.group(1));
               }
            }
         }
      }

      return var1;
   }

   static Map<String, Object> flowJson(Cards.Flow var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.scenarioIndex() != null) {
         var1.put("index", var0.scenarioIndex());
         var1.put("line", var0.line());
      }

      var1.put("name", var0.name());
      if (var0.outline()) {
         var1.put("outline", true);
      }

      put(var1, "row", var0.row());
      if (var0.shared()) {
         var1.put("shared", true);
      }

      var1.put("tags", var0.tags());
      var1.put("req", var0.reqLinks());
      var1.put("channels", channels(var0));
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("count", var0.checks().count());
      if (var0.checks().atLeast()) {
         var2.put("atLeast", true);
      }

      if (var0.checks().notChecked()) {
         var2.put("notChecked", true);
      }

      var1.put("checks", var2);
      ArrayList var3 = new ArrayList();

      for (Cards.Card var5 : var0.cards()) {
         var3.add(cardJson(var5));
      }

      var1.put("cards", var3);
      return var1;
   }

   static Map<String, Object> cardJson(Cards.Card var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("kind", var0.kind().name().toLowerCase().replace('_', ' '));
      var1.put("title", var0.title());
      var1.put("span", var0.span().toJson());
      ArrayList var2 = new ArrayList();

      for (Cards.Comment var4 : var0.comments()) {
         var var5 = commentJson(var4);
         var5.put("span", var4.span().toJson());
         var2.add(var5);
      }

      var1.put("comments", var2);
      var1.put("editable", var0.editable());
      LinkedHashMap var14 = new LinkedHashMap();
      var14.put("feature", var0.source().feature());
      if (var0.source().background()) {
         var14.put("background", true);
      } else {
         var14.put("scenario", var0.source().scenarioIndex());
         var14.put("scenarioName", var0.source().scenarioName());
      }

      if (var0.source().examplesBlock() != null) {
         var14.put("examplesBlock", var0.source().examplesBlock());
      } else {
         var14.put("firstStep", var0.source().firstStep());
         var14.put("lastStep", var0.source().lastStep());
      }

      var1.put("source", var14);
      var1.put("steps", var0.steps());
      Object var15 = var0.detail();
      if (var15 instanceof Cards.Call var16) {
         put(var1, "method", var16.method());
         put(var1, "url", var16.url());
         var1.put("path", var16.paths());
         var1.put("params", var16.params());
         var1.put("headers", var16.headers());
         var1.put("cookies", var16.cookies());
         var1.put("formFields", var16.formFields());
         put(var1, "request", var16.request());
      } else if (var15 instanceof Cards.Check var6) {
         if (var6.status() != null) {
            var1.put("status", var6.status());
         } else {
            if (Boolean.TRUE.equals(var6.each())) {
               var1.put("each", true);
            }

            var1.put("actual", var6.actual());
            var1.put("operator", var6.operator());
            var1.put("expected", var6.expected());
         }
      } else if (var15 instanceof Cards.Remember var7) {
         var1.put("variable", var7.variable());
         var1.put("expression", var7.expression());
      } else if (var15 instanceof Cards.DataTable var8) {
         var1.put("block", var8.block());
         put(var1, "name", var8.name());
         var1.put("tags", var8.tags());
         if (var8.computed()) {
            var1.put("computed", true);
         }

         ArrayList var10 = new ArrayList();

         for (Cards.Row var12 : var8.rows()) {
            LinkedHashMap var13 = new LinkedHashMap();
            var13.put("index", var12.index());
            put(var13, "row", var12.row());
            var13.put("cells", var12.cells());
            var13.put("span", var12.span().toJson());
            var10.add(var13);
         }

         var1.put("rows", var10);
      } else if (var15 instanceof Cards.Advanced var9) {
         put(var1, "keyword", var9.keyword());
         var1.put("assertion", var9.assertion().name().toLowerCase());
      }

      return var1;
   }

   static Map<String, Object> compactFlow(Cards.Flow var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.scenarioIndex() != null) {
         var1.put("index", var0.scenarioIndex());
         var1.put("line", var0.line());
      }

      var1.put("name", var0.name());
      if (var0.outline()) {
         var1.put("outline", true);
      }

      put(var1, "row", var0.row());
      putList(var1, "req", var0.reqLinks());
      putList(var1, "tags", var0.tags().stream().filter(var0x -> !var0x.startsWith("@req=")).toList());
      putList(var1, "channels", channels(var0));
      if (!var0.shared()) {
         var1.put("checks", var0.checks().count());
         if (var0.checks().atLeast()) {
            var1.put("checksAtLeast", true);
         }
      }

      ArrayList var2 = new ArrayList();

      for (Cards.Card var4 : var0.cards()) {
         var2.add(compactCard(var4));
      }

      var1.put("cards", var2);
      return var1;
   }

   static Map<String, Object> compactCard(Cards.Card var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("kind", var0.kind().name().toLowerCase().replace('_', ' '));
      var1.put("title", var0.title());
      if (!var0.editable()) {
         var1.put("editable", false);
      }

      Object var2 = var0.detail();
      if (var2 instanceof Cards.DataTable var3) {
         var1.put("block", var3.block());
      } else {
         var1.put("step", var0.source().firstStep());
         var1.put("end", var0.source().lastStep());
      }

      ArrayList var14 = new ArrayList();

      for (Cards.Comment var5 : var0.comments()) {
         var14.add(commentJson(var5));
      }

      putList(var1, "comments", var14);
      if (var2 instanceof Cards.Call var15) {
         put(var1, "method", var15.method());
         put(var1, "url", var15.url());
         putList(var1, "path", var15.paths());
         putList(var1, "params", var15.params());
         putList(var1, "headers", var15.headers());
         putList(var1, "cookies", var15.cookies());
         putList(var1, "formFields", var15.formFields());
         put(var1, "request", var15.request());
      } else if (var2 instanceof Cards.Check var16) {
         if (var16.status() != null) {
            var1.put("status", var16.status());
         } else {
            if (Boolean.TRUE.equals(var16.each())) {
               var1.put("each", true);
            }

            var1.put("actual", var16.actual());
            var1.put("operator", var16.operator());
            var1.put("expected", var16.expected());
         }
      } else if (var2 instanceof Cards.Remember var6) {
         var1.put("variable", var6.variable());
         var1.put("expression", var6.expression());
      } else if (var2 instanceof Cards.DataTable var7) {
         put(var1, "name", var7.name());
         putList(var1, "tags", var7.tags());
         if (var7.computed()) {
            var1.put("computed", true);
         } else if (!var7.rows().isEmpty()) {
            var1.put("header", var7.rows().get(0).cells());
            ArrayList var9 = new ArrayList();

            for (Cards.Row var11 : var7.rows().subList(1, var7.rows().size())) {
               LinkedHashMap var12 = new LinkedHashMap();
               put(var12, "row", var11.row());
               LinkedHashMap var13 = new LinkedHashMap();
               var13.put("block", var7.block());
               var13.put("row", var11.index());
               var12.put("edit", var13);
               var12.put("cells", var11.cells());
               var9.add(var12);
            }

            var1.put("rows", var9);
         }
      } else if (var2 instanceof Cards.Advanced var8) {
         put(var1, "keyword", var8.keyword());
         var1.put("assertion", var8.assertion().name().toLowerCase());
      }

      if (var2 instanceof Cards.Advanced || !var0.editable()) {
         var1.put("steps", var0.steps());
      }

      return var1;
   }

   private static Map<String, Object> commentJson(Cards.Comment var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("step", var0.step());
      var1.put("comment", var0.index());
      var1.put("text", var0.text());
      return var1;
   }

   private static void putList(Map<String, Object> var0, String var1, List<?> var2) {
      if (var2 != null && !var2.isEmpty()) {
         var0.put(var1, var2);
      }
   }

   private static void put(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   public record Advanced(String keyword, io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification assertion) {

   }

   public record Call(String method, String url, List<String> paths, List<String> params, List<String> headers, List<String> cookies, List<String> formFields, String request) {

   }

   public record Card(Cards.Kind kind, String title, Cards.Span span, List<Cards.Comment> comments, boolean editable, Cards.SourceRef source, List<String> steps, Object detail) {

   }

   public record Check(Integer status, Boolean each, String actual, String operator, String expected) {

   }

   public record CheckCount(int count, boolean atLeast) {


      public boolean notChecked() {
         return this.count == 0 && !this.atLeast;
      }
   }

   public record Comment(String text, Cards.Span span, int step, int index) {

   }

   public record DataTable(int block, String name, List<String> tags, boolean computed, List<Cards.Row> rows) {

   }

   public record FeatureView(String path, String version, boolean editable, String notEditable, String name, List<String> tags, Cards.Flow background, List<Cards.Flow> flows) {


      public Map<String, Object> toJson() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("path", this.path);
         var1.put("version", this.version);
         var1.put("editable", this.editable);
         if (this.notEditable != null) {
            var1.put("notEditable", this.notEditable);
         }

         var1.put("name", this.name);
         var1.put("tags", this.tags);
         if (this.background != null) {
            var1.put("background", Cards.flowJson(this.background));
         }

         ArrayList var2 = new ArrayList();

         for (Cards.Flow var4 : this.flows) {
            var2.add(Cards.flowJson(var4));
         }

         var1.put("scenarios", var2);
         return var1;
      }

      public Map<String, Object> toCompactJson() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("path", this.path);
         var1.put("version", this.version);
         if (!this.editable) {
            var1.put("editable", false);
            var1.put("notEditable", this.notEditable);
         }

         var1.put("name", this.name);
         Cards.putList(var1, "tags", this.tags);
         if (this.background != null) {
            var1.put("background", Cards.compactFlow(this.background));
         }

         ArrayList var2 = new ArrayList();

         for (Cards.Flow var4 : this.flows) {
            var2.add(Cards.compactFlow(var4));
         }

         var1.put("scenarios", var2);
         return var1;
      }
   }

   public record Flow(Integer scenarioIndex, String name, int line, boolean outline, boolean shared, List<String> tags, List<String> reqLinks, Cards.CheckCount checks, List<Cards.Card> cards, Integer row) {

   }

   public static enum Kind {
      CALL,
      CHECK,
      REMEMBER,
      DATA_TABLE,
      ADVANCED;
   }

   public record Remember(String variable, String expression) {

   }

   public record Row(int index, Integer row, List<String> cells, Cards.Span span) {

   }

   public record SourceRef(String feature, Integer scenarioIndex, String scenarioName, boolean background, int firstStep, int lastStep, Integer examplesBlock) {

   }

   public record Span(int line, int endLine, int byteStart, int byteEnd, int charStart, int charEnd) {


      static Cards.Span of(FeatureSource var0, SourceSpan var1) {
         return new Cards.Span(var1.line(), var1.endLine(), var0.byteOffset(var1.start()), var0.byteOffset(var1.end()), var1.start(), var1.end());
      }

      Cards.Span through(Cards.Span var1) {
         return new Cards.Span(this.line, var1.endLine, this.byteStart, var1.byteEnd, this.charStart, var1.charEnd);
      }

      Map<String, Object> toJson() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("line", this.line);
         var1.put("endLine", this.endLine);
         var1.put("byteStart", this.byteStart);
         var1.put("byteEnd", this.byteEnd);
         return var1;
      }
   }
}
