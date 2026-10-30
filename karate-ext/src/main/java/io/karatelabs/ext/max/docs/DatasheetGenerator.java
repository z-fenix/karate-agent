package io.karatelabs.ext.max.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatasheetGenerator {
   private static final String DEFAULT_CAPABILITIES = "../docs/CAPABILITIES.md";
   private static final String DEFAULT_DATASHEET = "src/main/resources/META-INF/karate/help/pages/datasheet.md";
   private static final String ASSETS_DIR = "../docs/assets";
   private static final String BRAND_RED = "#9F1C1C";
   private static final Pattern TRAILING_REF = Pattern.compile("\\s*\\([DPS]\\d+[a-z]?(?:[\\s,/\\u2013-]+[DPS]?\\d+[a-z]?)*\\)\\.?\\s*$");
   private static final Pattern BULLET = Pattern.compile("^- \\*\\*(.+?)\\*\\*(.*)$");
   private static final Pattern LEAD_SEP = Pattern.compile("^\\s*[\\u2013\\u2014-]\\s*");
   private static final Pattern INLINE_ROADMAP = Pattern.compile("\\(roadmap:\\s*");
   private static final Map<String, io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung> AREA_RUNG = Map.of(
      "Coverage & provenance",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST,
      "AI server & agentic harness",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST,
      "API testing & protocol reach",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST,
      "Contract testing & mock fidelity",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST,
      "The report & the deterministic floor",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST,
      "Rules & oracles",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.LOGIC,
      "Acceptance substrate & the UI↔rules bridge",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.LOGIC,
      "Requirements & traceability",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.GOVERN,
      "API governance",
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.GOVERN
   );
   private static final Map<String, String> AREA_ANCHOR = mapOf(
      "Coverage & provenance",
      "datasheet.area.coverage",
      "Requirements & traceability",
      "datasheet.area.requirements",
      "Rules & oracles",
      "datasheet.area.rules",
      "Acceptance substrate & the UI↔rules bridge",
      "datasheet.area.acceptance",
      "AI server & agentic harness",
      "datasheet.area.ai",
      "API testing & protocol reach",
      "datasheet.area.api",
      "Contract testing & mock fidelity",
      "datasheet.area.contract",
      "API governance",
      "datasheet.area.governance",
      "The report & the deterministic floor",
      "datasheet.area.report",
      "Deployment, adoption & licensing",
      "datasheet.deploy"
   );
   private static final Map<String, String> AREA_ICON = mapOf(
      "Coverage & provenance",
      "shield-check",
      "Requirements & traceability",
      "file-text",
      "Rules & oracles",
      "scale",
      "Acceptance substrate & the UI↔rules bridge",
      "circle-check",
      "AI server & agentic harness",
      "cpu",
      "API testing & protocol reach",
      "code-xml",
      "Contract testing & mock fidelity",
      "layers",
      "API governance",
      "gauge",
      "The report & the deterministic floor",
      "file-check",
      "Deployment, adoption & licensing",
      "package"
   );
   private static final Map<String, String> AREA_DIAGRAM = mapOf(
      "Coverage & provenance",
      "provenance-grade",
      "Requirements & traceability",
      "hub-hero",
      "Rules & oracles",
      "rules-oracle",
      "API testing & protocol reach",
      "protocol-reach",
      "Contract testing & mock fidelity",
      "contract-pair",
      "The report & the deterministic floor",
      "api-ui-one-report",
      "AI server & agentic harness",
      "connectivity",
      "Deployment, adoption & licensing",
      "instance-anatomy"
   );
   static final List<String> DIAGRAM_PAGES = List.of("getting-started.md", "install.md");
   private static final Pattern DIAGRAM_MARK = Pattern.compile("<!--diagram:([\\w-]+)-->.*?<!--/diagram-->", 32);
   private static final Map<String, String> ICONS = mapOf(
      "shield-check",
      "<path d=\"M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z\"/><path d=\"m9 12 2 2 4-4\"/>",
      "file-text",
      "<path d=\"M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z\"/><path d=\"M14 2v5a1 1 0 0 0 1 1h5\"/><path d=\"M10 9H8\"/><path d=\"M16 13H8\"/><path d=\"M16 17H8\"/>",
      "scale",
      "<path d=\"M12 3v18\"/><path d=\"m19 8 3 8a5 5 0 0 1-6 0zV7\"/><path d=\"M3 7h1a17 17 0 0 0 8-2 17 17 0 0 0 8 2h1\"/><path d=\"m5 8 3 8a5 5 0 0 1-6 0zV7\"/><path d=\"M7 21h10\"/>",
      "circle-check",
      "<circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"m9 12 2 2 4-4\"/>",
      "cpu",
      "<path d=\"M12 20v2\"/><path d=\"M12 2v2\"/><path d=\"M17 20v2\"/><path d=\"M17 2v2\"/><path d=\"M2 12h2\"/><path d=\"M2 17h2\"/><path d=\"M2 7h2\"/><path d=\"M20 12h2\"/><path d=\"M20 17h2\"/><path d=\"M20 7h2\"/><path d=\"M7 20v2\"/><path d=\"M7 2v2\"/><rect x=\"4\" y=\"4\" width=\"16\" height=\"16\" rx=\"2\"/><rect x=\"8\" y=\"8\" width=\"8\" height=\"8\" rx=\"1\"/>",
      "layers",
      "<path d=\"M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z\"/><path d=\"M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12\"/><path d=\"M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17\"/>",
      "code-xml",
      "<path d=\"m18 16 4-4-4-4\"/><path d=\"m6 8-4 4 4 4\"/><path d=\"m14.5 4-5 16\"/>",
      "file-check",
      "<path d=\"M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z\"/><path d=\"M14 2v5a1 1 0 0 0 1 1h5\"/><path d=\"m9 15 2 2 4-4\"/>",
      "gauge",
      "<path d=\"m12 14 4-4\"/><path d=\"M3.34 19a10 10 0 1 1 17.32 0\"/>",
      "package",
      "<path d=\"M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z\"/><path d=\"M12 22V12\"/><polyline points=\"3.29 7 12 12 20.71 7\"/><path d=\"m7.5 4.27 9 5.15\"/>"
   );

   private DatasheetGenerator() {
   }

   private static Map<String, String> mapOf(String... var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();

      for (byte var2 = 0; var2 < var0.length; var2 += 2) {
         var1.put(var0[var2], var0[var2 + 1]);
      }

      return var1;
   }

   public static void main(String[] var0) throws IOException {
      Path var1 = Path.of(var0.length > 0 ? var0[0] : "../docs/CAPABILITIES.md");
      Path var2 = Path.of(var0.length > 1 ? var0[1] : "src/main/resources/META-INF/karate/help/pages/datasheet.md");
      String var3 = generate(Files.readString(var1));
      Files.writeString(var2, var3);
      System.out.println("wrote " + var2 + " (" + var3.length() + " bytes) from " + var1);
      inlineHelpDiagrams(var2.getParent());
   }

   static void inlineHelpDiagrams(Path var0) throws IOException {
      for (String var2 : DIAGRAM_PAGES) {
         Path var3 = var0.resolve(var2);
         if (Files.exists(var3)) {
            String var4 = Files.readString(var3);
            String var5 = bakeDiagrams(var4);
            if (!var5.equals(var4)) {
               Files.writeString(var3, var5);
               System.out.println("inlined diagrams into " + var3);
            }
         }
      }
   }

   static String bakeDiagrams(String var0) {
      Matcher var1 = DIAGRAM_MARK.matcher(var0);
      StringBuilder var2 = new StringBuilder();

      while (var1.find()) {
         String var3 = var1.group(1);
         String var4 = "<!--diagram:" + var3 + "-->\n" + diagram(var3).strip() + "\n<!--/diagram-->";
         var1.appendReplacement(var2, Matcher.quoteReplacement(var4));
      }

      var1.appendTail(var2);
      return var2.toString();
   }

   public static String generate(String var0) {
      ArrayList<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea> var1 = new ArrayList();
      ArrayList<java.lang.String> var2 = new ArrayList();
      ArrayList<java.lang.String> var3 = new ArrayList();
      io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var4 = null;
      boolean var5 = false;
      boolean var6 = false;

      for (String var10 : var0.split("\n", -1)) {
         String var11 = var10.stripTrailing();
         if (var11.startsWith("## ")) {
            var5 = true;
            String var12 = var11.substring(3).trim();
            if (var12.startsWith("Horizon")) {
               var6 = true;
               var4 = null;
            } else {
               var6 = false;
               var4 = new io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea(var12);
               var1.add(var4);
            }
         } else if (var5 && !var11.startsWith("*Where:")) {
            if (var6) {
               if (var11.startsWith("- ") || var11.startsWith("  - ")) {
                  var3.add(cleanRefs(var11));
               }
            } else if (var4 != null && var11.startsWith("- ")) {
               Matcher var31 = BULLET.matcher(var11);
               if (var31.matches()) {
                  String var13 = var31.group(1);
                  String var14 = cleanRefs(var31.group(2));
                  if (var13.contains("(roadmap)")) {
                     String var15 = var13.replace("(roadmap)", "").replaceAll("\\s+$", "");
                     var2.add("- **" + var15 + "**" + var14);
                  } else {
                     String var38 = INLINE_ROADMAP.matcher(LEAD_SEP.matcher(var14).replaceFirst("").strip()).replaceAll("(planned: ");
                     var4.c.add(new io.karatelabs.ext.max.docs.DatasheetGenerator.Capability(var13, var38));
                  }
               }
            }
         }
      }

      StringBuilder var17 = new StringBuilder();
      var17.append("<!-- GENERATED from docs/CAPABILITIES.md by etc/build-datasheet.sh - DO NOT HAND-EDIT.\n");
      var17.append("     Edit the capability roster in docs/CAPABILITIES.md, then run etc/build-datasheet.sh.\n");
      var17.append("     The in-app /help datasheet page + the etc/datasheet.sh PDF/HTML both render this file,\n");
      var17.append("     so capabilities stay DRY across every surface. Gate: DatasheetGeneratedTest. -->\n\n");
      var17.append("# Karate Agent — Product Datasheet {#datasheet}\n\n");
      var17.append(diagram("verification-harness")).append("\n");
      var17.append("**Karate Agent** helps you author and maintain tests with AI - from a contract, a recording, or\n");
      var17.append("your requirements. Every run then becomes governed evidence: which requirements are really\n");
      var17.append("covered, by tests that really ran - one git-native graph the AI drives and a human audits. Adopt\n");
      var17.append("it one rung at a time: just test, then encode the logic, then govern it.\n\n");
      var17.append("- **Runs on your machines** - one Docker image or one Java jar; nothing is hosted by us.\n");
      var17.append(
         "- **Your AI, or none** - bring your own model (Anthropic, any OpenAI-compatible endpoint, OpenRouter, or a local Ollama); the deterministic product runs with no model at all.\n"
      );
      var17.append("- **Licensing** - a license file unlocks the paid features; open-source karate-core stays free.\n");
      var17.append(
         "- **Reading this sheet** - every table row ships today. Planned capabilities appear only under [Roadmap](#datasheet.roadmap), marked planned.\n\n"
      );
      var17.append(diagram("adoption-ladder")).append("\n");
      long var18 = var1.stream().filter(var0x -> !var0x.c.isEmpty() && !isDeployment(var0x)).count();
      var17.append("## Capabilities at a glance {#datasheet.map}\n\n");
      var17.append(numberWord(var18)).append(" capability areas across three adoption rungs, plus a deployment band - adopt any rung, stop at any rung.\n\n");
      var17.append("| Capability area | Rung | At a glance |\n");
      var17.append("|---|---|---|\n");

      for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung var34 : io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.values()) {
         for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var39 : (Iterable<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea>) (Iterable<?>) (var1)) {
            if (!var39.c.isEmpty() && !isDeployment(var39) && rungOf(var39) == var34) {
               var17.append(mapRow(var39, var34.level));
            }
         }
      }

      for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var26 : (Iterable<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea>) (Iterable<?>) (var1)) {
         if (!var26.c.isEmpty() && isDeployment(var26)) {
            var17.append(mapRow(var26, "any"));
         }
      }

      var17.append("\n");

      for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung var35 : io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.values()) {
         ArrayList<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea> var37 = new ArrayList();

         for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var16 : (Iterable<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea>) (Iterable<?>) (var1)) {
            if (!var16.c.isEmpty() && !isDeployment(var16) && rungOf(var16) == var35) {
               var37.add(var16);
            }
         }

         if (!var37.isEmpty()) {
            var17.append("## ").append(var35.title).append(" · ").append(var35.level).append(" {#").append(var35.anchor).append("}\n\n");
            var17.append(var35.blurb).append("\n");

            for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var42 : (Iterable<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea>) (Iterable<?>) (var37)) {
               appendArea(var17, var42);
            }
         }
      }

      for (io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var28 : (Iterable<io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea>) (Iterable<?>) (var1)) {
         if (!var28.c.isEmpty() && isDeployment(var28)) {
            var17.append("## Run it anywhere {#datasheet.deploy}\n\n");
            var17.append(
               "The same engine on a laptop, a shared server or a CI runner - as a Docker image with the browser stack built in, or a single Java jar - adopted feature by feature over open-source karate-core.\n"
            );
            var17.append("\n").append(diagram("deployment-modes"));
            appendArea(var17, var28, false);
         }
      }

      var17.append("\n## Roadmap — what's coming {#datasheet.roadmap}\n");
      var17.append("\nPlanned, not shipped today. Ask us about timing.\n\n");

      for (String var29 : (Iterable<String>) (Iterable<?>) (var2)) {
         var17.append(var29).append("\n");
      }

      if (!var3.isEmpty()) {
         var17.append("\nFurther out:\n\n");

         for (String var30 : (Iterable<String>) (Iterable<?>) (var3)) {
            var17.append(var30).append("\n");
         }
      }

      return var17.toString();
   }

   private static String mapRow(io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var0, String var1) {
      String var2 = !var0.b.isEmpty() ? var0.b : (isDeployment(var0) ? "Docker image or Java jar · laptop, server or CI" : "");
      return "| " + icon(var0) + " [" + var0.a + "](#" + AREA_ANCHOR.get(var0.a) + ") | " + var1 + " | " + var2 + " |\n";
   }

   private static void appendArea(StringBuilder var0, io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var1) {
      appendArea(var0, var1, true);
   }

   private static void appendArea(StringBuilder var0, io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var1, boolean var2) {
      if (var2) {
         var0.append("\n### ").append(icon(var1)).append(" ").append(var1.a).append(" {#").append(AREA_ANCHOR.get(var1.a)).append("}\n");
         if (!var1.b.isEmpty()) {
            var0.append("*").append(var1.b).append("*\n");
         }
      }

      var0.append("\n| Capability | What it does |\n");
      var0.append("|---|---|\n");

      for (io.karatelabs.ext.max.docs.DatasheetGenerator.Capability var4 : var1.c) {
         var0.append("| **").append(cell(var4.a)).append("** | ").append(cell(var4.b)).append(" |\n");
      }

      String var5 = AREA_DIAGRAM.get(var1.a);
      if (var5 != null) {
         var0.append("\n").append(diagram(var5));
      }

      var0.append("\n");
   }

   private static io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung rungOf(io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var0) {
      return AREA_RUNG.getOrDefault(var0.a, io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityRung.TEST);
   }

   private static boolean isDeployment(io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var0) {
      return var0.a.startsWith("Deployment");
   }

   private static String numberWord(long var0) {
      String[] var2 = new String[]{"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"};
      return var0 >= 0L && var0 < var2.length ? var2[(int)var0] : Long.toString(var0);
   }

   static Set<String> mappedAreaNames() {
      var var0 = new LinkedHashSet<>(AREA_RUNG.keySet());
      var0.addAll(AREA_ANCHOR.keySet());
      var0.addAll(AREA_ICON.keySet());
      var0.addAll(AREA_DIAGRAM.keySet());
      return var0;
   }

   static Collection<String> mappedIconNames() {
      return AREA_ICON.values();
   }

   static String iconSvg(String var0) {
      return ICONS.getOrDefault(var0, "");
   }

   private static String icon(io.karatelabs.ext.max.docs.DatasheetGenerator.CapabilityArea var0) {
      String var1 = ICONS.get(AREA_ICON.get(var0.a));
      return var1 == null
         ? ""
         : "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#9F1C1C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" style=\"vertical-align:-2px;margin-right:2px\">"
            + var1
            + "</svg>";
   }

   private static String diagram(String var0) {
      try {
         String var1 = Files.readString(Path.of("../docs/assets", var0 + ".svg")).strip();
         return var1.replaceFirst("<svg ", "<svg class=\"k-diagram\" ") + "\n";
      } catch (IOException var2) {
         throw new UncheckedIOException("missing brand diagram: " + var0 + ".svg", var2);
      }
   }

   private static String cell(String var0) {
      return var0.replace("|", "\\|");
   }

   static String cleanRefs(String var0) {
      return TRAILING_REF.matcher(var0).replaceAll("");
   }

   private static final class CapabilityArea {
      final String a;
      final String b;
      final List<io.karatelabs.ext.max.docs.DatasheetGenerator.Capability> c = new ArrayList<>();

      CapabilityArea(String var1) {
         int var2 = var1.indexOf(8212);
         if (var2 < 0) {
            this.a = var1.strip();
            this.b = "";
         } else {
            this.a = var1.substring(0, var2).strip();
            this.b = var1.substring(var2 + 1).strip().replaceAll("^\\*|\\*$", "");
         }
      }
   }

   private record Capability(String a, String b) {
   }

   private static enum CapabilityRung {
      TEST(
         "L1",
         "Just test",
         "datasheet.rung.test",
         "Run the tests you already have, get an honest coverage grade, and ship a report. No rules, no requirements - just evidence."
      ),
      LOGIC(
         "L2",
         "Encode the logic",
         "datasheet.rung.logic",
         "Put the business rules into an executable oracle, so the same rulebook drives the mock and judges the test."
      ),
      GOVERN(
         "L3",
         "Govern it",
         "datasheet.rung.govern",
         "Tie evidence to requirements in git - bidirectional traceability, readiness, and a confidence-to-ship verdict."
      );

      final String level;
      final String title;
      final String anchor;
      final String blurb;

      private CapabilityRung(String var3, String var4, String var5, String var6) {
         this.level = var3;
         this.title = var4;
         this.anchor = var5;
         this.blurb = var6;
      }
   }
}
